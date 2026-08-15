#!/usr/bin/env node
/**
 * Kaiteyo — dependency-free SVG → PNG/ICO/ICNS rasterizer.
 *
 * Renders the *simple* Kaiteyo brand SVGs (rounded-rect tile + stroked brush
 * paths + accent dot) into the binary formats the OS/installer pipeline needs:
 *
 *   PNG   — lossless RGBA at any size (window icon, iOS icon, flatpak/snap)
 *   ICO   — multi-size, PNG-compressed entries (Vista+ format, Windows)
 *   ICNS  — PNG chunks (ic07–ic14, macOS)
 *
 * No external dependencies: node's built-in `zlib` writes the PNG stream and a
 * tiny rasterizer handles the SVG subset used by the brand mark. If an SVG
 * uses features this rasterizer does not support (gradients, filters, <text>,
 * <g transform>, …) it fails loudly instead of rendering something wrong.
 *
 * Usage:
 *   node brand/scripts/render-icons.mjs \
 *     --in  brand/source/app-icons/app-icon.svg \
 *     --out brand/generated/kaiteyo \
 *     --png 256,512,1024 --ico 16,32,48,64,128,256 --icns 128,256,512,1024
 */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { deflateSync } from 'node:zlib';

// ---------------------------------------------------------------------------
// Minimal SVG parsing (subset used by the Kaiteyo mark)
// ---------------------------------------------------------------------------

function parseColor(raw) {
  const hex = raw.trim().replace(/^#/, '');
  if (hex.length === 3) {
    return [
      parseInt(hex[0] + hex[0], 16),
      parseInt(hex[1] + hex[1], 16),
      parseInt(hex[2] + hex[2], 16),
    ];
  }
  if (hex.length === 6) {
    return [
      parseInt(hex.slice(0, 2), 16),
      parseInt(hex.slice(2, 4), 16),
      parseInt(hex.slice(4, 6), 16),
    ];
  }
  throw new Error(`unsupported color: ${raw}`);
}

function parseAttr(raw, name) {
  const m = raw.match(new RegExp(`(?:^|\\s)${name}\\s*=\\s*"([^"]*)"`));
  return m ? m[1] : null;
}

/** Flatten an SVG pathData string into line segments in user space. */
function flattenPath(d, step = 1 / 24) {
  const pts = [];
  let i = 0;
  let cx = 0;
  let cy = 0;
  let startX = 0;
  let startY = 0;
  let cur = null;
  let sub = null;

  const num = () => {
    while (i < d.length && /[\s,]/.test(d[i])) i++;
    const m = d.slice(i).match(/^[+-]?(?:\d+\.?\d*|\.\d+)(?:[eE][+-]?\d+)?/);
    if (!m) throw new Error(`cannot parse number at "${d.slice(i)}"`);
    i += m[0].length;
    return parseFloat(m[0]);
  };
  const cmd = () => {
    while (i < d.length && /[\s,]/.test(d[i])) i++;
    return d[i];
  };

  while (i < d.length) {
    let c = cmd();
    if (/[A-Za-z]/.test(c)) {
      cur = c;
      i++;
    } else if (!cur) {
      throw new Error(`pathData must start with a command, got "${d.slice(i)}"`);
    } else {
      c = cur; // implicit repetition
    }
    const rel = c === c.toLowerCase();
    const ref = (x, y) => (rel ? [cx + x, cy + y] : [x, y]);

    switch (c.toUpperCase()) {
      case 'M': {
        const [x, y] = ref(num(), num());
        cx = x; cy = y; startX = x; startY = y; sub = null;
        break;
      }
      case 'L': {
        const [x, y] = ref(num(), num());
        if (sub) pts.push([sub.x, sub.y, x, y]);
        else pts.push([cx, cy, x, y]);
        sub = { x, y };
        cx = x; cy = y;
        break;
      }
      case 'Q': {
        const [qx, qy] = ref(num(), num());
        const [x, y] = ref(num(), num());
        const sx = sub ? sub.x : cx;
        const sy = sub ? sub.y : cy;
        for (let t = step; t <= 1.0001; t += step) {
          const u = 1 - t;
          const px = u * u * sx + 2 * u * t * qx + t * t * x;
          const py = u * u * sy + 2 * u * t * qy + t * t * y;
          const last = pts.length ? pts[pts.length - 1] : null;
          const lx = last && t === step && sub ? last[2] : last ? last[2] : sx;
          const ly = last && t === step && sub ? last[3] : last ? last[3] : sy;
          pts.push([lx, ly, px, py]);
        }
        sub = { x, y };
        cx = x; cy = y;
        break;
      }
      case 'Z': {
        if (sub && (sub.x !== startX || sub.y !== startY)) {
          pts.push([sub.x, sub.y, startX, startY]);
        }
        sub = null;
        cx = startX; cy = startY;
        break;
      }
      default:
        throw new Error(`unsupported path command "${c}"`);
    }
  }
  return pts;
}

// ---------------------------------------------------------------------------
// Rasterizer
// ---------------------------------------------------------------------------

/**
 * Rasterize one SVG document into an RGBA buffer.
 * Supports: <svg viewBox>, <rect> (fill, rx), <path> (stroked, round caps),
 * <circle> (fill). Opacity honored. 4× supersampled AA.
 */
function rasterizeSvg(svg, sizePx) {
  const root = svg.match(/<svg[^>]*>/);
  if (!root) throw new Error('no <svg> root');
  const viewBox = parseAttr(root[0], 'viewBox');
  const vb = viewBox ? viewBox.split(/[\s,]+/).map(Number) : [0, 0, 1024, 1024];
  const [vbX, vbY, vbW, vbH] = vb;
  const scale = sizePx / Math.max(vbW, vbH);

  const shapes = [];
  const unsupported = (what) => {
    throw new Error(`unsupported SVG feature in ${what}: the Kaiteyo rasterizer only handles rect/path/circle with solid fills and round-capped strokes`);
  };

  // Parse shapes in DOCUMENT ORDER (painter's algorithm) — collect every
  // <rect>/<circle>/<path> with its position, then parse them sorted by index.
  const tagged = [];
  const tagRe = /<(rect|circle|path)\b[^>]*>/g;
  let m;
  while ((m = tagRe.exec(svg))) tagged.push({ tag: m[1], raw: m[0], index: m.index });
  tagged.sort((a, b) => a.index - b.index);

  for (const { tag, raw } of tagged) {
    if (tag === 'rect') {
      const x = parseFloat(parseAttr(raw, 'x') ?? '0');
      const y = parseFloat(parseAttr(raw, 'y') ?? '0');
      const w = parseFloat(parseAttr(raw, 'width'));
      const h = parseFloat(parseAttr(raw, 'height'));
      const rx = parseFloat(parseAttr(raw, 'rx') ?? '0');
      const fill = parseAttr(raw, 'fill') ?? '#000000';
      if (w == null || h == null) unsupported('rect (width/height required)');
      shapes.push({ type: 'rect', x, y, w, h, rx, color: parseColor(fill), opacity: parseFloat(parseAttr(raw, 'opacity') ?? '1') });
    } else if (tag === 'circle') {
      const cx = parseFloat(parseAttr(raw, 'cx'));
      const cy = parseFloat(parseAttr(raw, 'cy'));
      const r = parseFloat(parseAttr(raw, 'r'));
      const fill = parseAttr(raw, 'fill') ?? '#000000';
      if (cx == null || cy == null || r == null) unsupported('circle');
      shapes.push({ type: 'circle', cx, cy, r, color: parseColor(fill), opacity: parseFloat(parseAttr(raw, 'opacity') ?? '1') });
    } else {
      const d = parseAttr(raw, 'd');
      if (!d) unsupported('path (no d)');
      const stroke = parseAttr(raw, 'stroke');
      const strokeWidth = parseFloat(parseAttr(raw, 'stroke-width') ?? '0');
      const opacity = parseFloat(parseAttr(raw, 'opacity') ?? '1');
      if (!stroke || strokeWidth <= 0) unsupported('path (fill-only paths)');
      const lineCap = parseAttr(raw, 'stroke-linecap');
      if (lineCap && lineCap !== 'round') unsupported(`path stroke-linecap "${lineCap}"`);
      shapes.push({ type: 'path', segments: flattenPath(d), width: strokeWidth, color: parseColor(stroke), opacity });
    }
  }

  if (shapes.length === 0) throw new Error('no renderable shapes in SVG');

  // Reject anything we cannot render faithfully.
  if (/<defs|<linearGradient|<radialGradient|<filter|<text|<g\b|transform=/i.test(svg)) {
    unsupported('gradients/filters/text/groups/transforms');
  }

  const SS = sizePx >= 512 ? 2 : 4; // supersampling (halved for large rasters)
  const buf = Buffer.alloc(sizePx * sizePx * 4); // RGBA
  const px = (i, j) => buf.subarray((i * sizePx + j) * 4, (i * sizePx + j) * 4 + 4);

  // Per-shape pixel-space bounding box so strokes/dots are only tested on the
  // pixels near them instead of the whole canvas.
  const box = (s) => {
    if (s.type === 'rect') {
      return { x0: (s.x) * scale, y0: (s.y) * scale, x1: (s.x + s.w) * scale, y1: (s.y + s.h) * scale };
    }
    if (s.type === 'circle') {
      return { x0: (s.cx - s.r) * scale, y0: (s.cy - s.r) * scale, x1: (s.cx + s.r) * scale, y1: (s.cy + s.r) * scale };
    }
    let x0 = Infinity, y0 = Infinity, x1 = -Infinity, y1 = -Infinity;
    for (const [a, b, c, d] of s.segments) {
      for (const [X, Y] of [[a, b], [c, d]]) {
        if (X < x0) x0 = X; if (X > x1) x1 = X;
        if (Y < y0) y0 = Y; if (Y > y1) y1 = Y;
      }
    }
    const m = s.width / 2;
    return { x0: (x0 - m) * scale, y0: (y0 - m) * scale, x1: (x1 + m) * scale, y1: (y1 + m) * scale };
  };
  const boxes = shapes.map(box);

  const distToSeg = (px0, py0, x1, y1, x2, y2) => {
    const dx = x2 - x1;
    const dy = y2 - y1;
    const len2 = dx * dx + dy * dy;
    let t = len2 > 0 ? ((px0 - x1) * dx + (py0 - y1) * dy) / len2 : 0;
    t = Math.max(0, Math.min(1, t));
    const cx0 = x1 + t * dx;
    const cy0 = y1 + t * dy;
    return Math.hypot(px0 - cx0, py0 - cy0);
  };

  const inRoundedRect = (x, y, r) => {
    const { x: rx, y: ry, w, h } = r;
    const rad = Math.min(r.rx, w / 2, h / 2);
    const cx = x - rx - w / 2;
    const cy = y - ry - h / 2;
    const hx = w / 2 - rad;
    const hy = h / 2 - rad;
    if (cx >= -hx && cx <= hx) return Math.abs(cy) <= hy + rad;
    if (cy >= -hy && cy <= hy) return Math.abs(cx) <= hx + rad;
    return Math.hypot(cx - Math.sign(cx) * hx, cy - Math.sign(cy) * hy) <= rad;
  };

  for (let y = 0; y < sizePx; y++) {
    for (let x = 0; x < sizePx; x++) {
      let r = 0, g = 0, b = 0, a = 0;
      for (let sy = 0; sy < SS; sy++) {
        for (let sx = 0; sx < SS; sx++) {
          const fx = (x + (sx + 0.5) / SS) / scale + vbX;
          const fy = (y + (sy + 0.5) / SS) / scale + vbY;
          // Painter's algorithm: later shapes paint over earlier ones, so the
          // LAST shape containing the pixel wins (the tile rect, then strokes,
          // then the accent dot). Bounding boxes skip far-away shapes.
          let hit = null;
          for (let si = 0; si < shapes.length; si++) {
            const bx = boxes[si];
            if (fx * scale < bx.x0 || fx * scale > bx.x1 || fy * scale < bx.y0 || fy * scale > bx.y1) continue;
            const s = shapes[si];
            let inside = false;
            if (s.type === 'rect') {
              inside = inRoundedRect(fx, fy, s);
            } else if (s.type === 'circle') {
              inside = Math.hypot(fx - s.cx, fy - s.cy) <= s.r;
            } else {
              const half = s.width / 2;
              let minD = Infinity;
              for (const [x1, y1, x2, y2] of s.segments) {
                const d = distToSeg(fx, fy, x1, y1, x2, y2);
                if (d < minD) minD = d;
              }
              // round caps: endpoints act as dots
              const last = s.segments[s.segments.length - 1];
              if (last) minD = Math.min(minD, Math.hypot(fx - last[2], fy - last[3]));
              inside = minD <= half;
            }
            if (inside) hit = s;
          }
          if (hit) {
            r += hit.color[0]; g += hit.color[1]; b += hit.color[2]; a += 255 * hit.opacity;
          }
        }
      }
      const n = SS * SS;
      const dst = px(y, x);
      const dstA = dst[3];
      const srcA = a / n;
      const outA = srcA + dstA * (1 - srcA / 255);
      if (outA <= 0) continue;
      const outR = Math.round((r / n * srcA + dst[0] * dstA * (1 - srcA / 255)) / outA);
      const outG = Math.round((g / n * srcA + dst[1] * dstA * (1 - srcA / 255)) / outA);
      const outB = Math.round((b / n * srcA + dst[2] * dstA * (1 - srcA / 255)) / outA);
      dst[0] = outR; dst[1] = outG; dst[2] = outB; dst[3] = Math.round(outA);
    }
  }
  return buf;
}

// ---------------------------------------------------------------------------
// PNG encoder (RGBA, zlib deflate)
// ---------------------------------------------------------------------------

const CRC_TABLE = (() => {
  const t = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c >>> 0;
  }
  return t;
})();

function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length);
  const typeBuf = Buffer.from(type, 'ascii');
  const crcBuf = Buffer.alloc(4);
  crcBuf.writeUInt32BE(crc32(Buffer.concat([typeBuf, data])));
  return Buffer.concat([len, typeBuf, data, crcBuf]);
}

function encodePng(rgba, size) {
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(size, 0);
  ihdr.writeUInt32BE(size, 4);
  ihdr[8] = 8; // bit depth
  ihdr[9] = 6; // color type RGBA
  const stride = size * 4;
  const raw = Buffer.alloc((stride + 1) * size);
  for (let y = 0; y < size; y++) {
    raw[y * (stride + 1)] = 0; // filter: none
    rgba.copy(raw, y * (stride + 1) + 1, y * stride, (y + 1) * stride);
  }
  const idat = deflateSync(raw, { level: 9 });
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', ihdr),
    chunk('IDAT', idat),
    chunk('IEND', Buffer.alloc(0)),
  ]);
}

// ---------------------------------------------------------------------------
// ICO / ICNS writers (both embed PNG blobs)
// ---------------------------------------------------------------------------

function encodeIco(sizes, pngFor) {
  const blobs = sizes.map((s) => pngFor(s));
  const header = Buffer.alloc(6);
  header.writeUInt16LE(0, 0);
  header.writeUInt16LE(1, 2);
  header.writeUInt16LE(blobs.length, 4);
  const entries = Buffer.alloc(16 * blobs.length);
  let offset = 6 + entries.length;
  blobs.forEach((blob, i) => {
    const e = entries.subarray(i * 16, (i + 1) * 16);
    e[0] = sizes[i] >= 256 ? 0 : sizes[i];
    e[1] = sizes[i] >= 256 ? 0 : sizes[i];
    e[4] = 1; // planes
    e[6] = 32; // bpp
    e.writeUInt32LE(blob.length, 8);
    e.writeUInt32LE(offset, 12);
    offset += blob.length;
  });
  return Buffer.concat([header, entries, ...blobs]);
}

function encodeIcns(sizes, pngFor) {
  const typeFor = {
    16: 'icp4', 32: 'icp5', 64: 'icp6', 128: 'ic07',
    256: 'ic08', 512: 'ic09', 1024: 'ic10',
    32: 'ic11', 64: 'ic12', 256: 'ic13', 512: 'ic14',
  };
  const chunks = [];
  for (const s of sizes) {
    const type = typeFor[s];
    if (!type) continue;
    const png = pngFor(s);
    const len = Buffer.alloc(4);
    len.writeUInt32BE(png.length + 8);
    chunks.push(Buffer.concat([Buffer.from(type, 'ascii'), len, png]));
  }
  const body = Buffer.concat(chunks);
  const header = Buffer.alloc(8);
  header.write('icns', 0, 'ascii');
  header.writeUInt32BE(body.length + 8, 4);
  return Buffer.concat([header, body]);
}

// ---------------------------------------------------------------------------
// CLI
// ---------------------------------------------------------------------------

const args = process.argv.slice(2);
const opt = (name) => {
  const i = args.indexOf(`--${name}`);
  return i >= 0 ? args[i + 1] : null;
};
const optList = (name, def) => (opt(name) ?? def).split(',').map(Number).filter(Boolean);

const input = opt('in');
const outBase = opt('out');
if (!input || !outBase) {
  console.error('usage: node render-icons.mjs --in <svg> --out <base> [--png 256] [--ico 16,32,48,64,128,256] [--icns 128,256,512,1024]');
  process.exit(2);
}

const pngSizes = optList('png', '');
const icoSizes = optList('ico', '');
const icnsSizes = optList('icns', '');

try {
  const svg = readFileSync(input, 'utf8');
  mkdirSync(dirname(outBase), { recursive: true });

  const pngFor = (size) => encodePng(rasterizeSvg(svg, size), size);

  for (const s of pngSizes) {
    const out = `${outBase}-${s}.png`;
    writeFileSync(out, pngFor(s));
    console.log(`  wrote ${out}`);
  }
  if (icoSizes.length) {
    const out = `${outBase}.ico`;
    writeFileSync(out, encodeIco(icoSizes, pngFor));
    console.log(`  wrote ${out} (${icoSizes.join(',')}px)`);
  }
  if (icnsSizes.length) {
    const out = `${outBase}.icns`;
    writeFileSync(out, encodeIcns(icnsSizes, pngFor));
    console.log(`  wrote ${out} (${icnsSizes.join(',')}px)`);
  }
} catch (err) {
  console.error(`  !! render-icons failed: ${err.message}`);
  process.exit(1);
}
