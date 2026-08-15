#!/usr/bin/env node
/**
 * Kaiteyo — convert the simple brand-mark SVG into an Android VectorDrawable
 * XML (the format Compose Multiplatform resources can load on *every*
 * platform, including Android, via painterResource).
 *
 * SVG support in Compose resources is desktop/iOS-only, so the shared mark is
 * shipped as XML vector. This converter is the "processed" step for that one
 * file: it emits an equivalent <path> stream (rounded rect, round-capped
 * strokes, filled circle) and writes it to core's drawable folder.
 *
 * Supports the same subset as render-icons.mjs: rect (fill, rx), stroked
 * paths with round caps, filled circles. Fails loudly otherwise.
 *
 * Usage: node brand/scripts/svg-to-vector.mjs \
 *   --in brand/source/marks/app-mark.svg \
 *   --out core/src/commonMain/composeResources/drawable/kaiteyo_mark.xml
 */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname } from 'node:path';

const args = process.argv.slice(2);
const opt = (name) => {
  const i = args.indexOf(`--${name}`);
  return i >= 0 ? args[i + 1] : null;
};
const input = opt('in');
const output = opt('out');
// Optional: --adaptive-dir <dir> emits the three Android adaptive-icon layers
// (background / foreground / monochrome) for the app launcher icon, derived
// from the same mark with the content scaled into the safe zone.
const adaptiveDir = opt('adaptive-dir');
if (!input || (!output && !adaptiveDir)) {
  console.error('usage: node svg-to-vector.mjs --in <mark.svg> [--out <drawable.xml>] [--adaptive-dir <res-dir>]');
  process.exit(2);
}

function parseColor(raw) {
  const hex = raw.trim().replace(/^#/, '');
  if (hex.length === 3) return '#' + hex.split('').map((c) => c + c).join('');
  if (hex.length === 6) return '#' + hex;
  throw new Error(`unsupported color: ${raw}`);
}

function parseAttr(raw, name) {
  const m = raw.match(new RegExp(`(?:^|\\s)${name}\\s*=\\s*"([^"]*)"`));
  return m ? m[1] : null;
}

/** SVG pathData → Android VectorDrawable pathData (same syntax for M/L/Q/C/Z). */
function toPathData(d) {
  return d.replace(/\s+/g, ' ').trim();
}

/** Rounded rect → path with arcs. */
function roundedRectPath(x, y, w, h, rx) {
  const r = Math.min(rx, w / 2, h / 2);
  return [
    `M${x + r},${y}`,
    `L${x + w - r},${y}`,
    `A${r},${r} 0 0 1 ${x + w},${y + r}`,
    `L${x + w},${y + h - r}`,
    `A${r},${r} 0 0 1 ${x + w - r},${y + h}`,
    `L${x + r},${y + h}`,
    `A${r},${r} 0 0 1 ${x},${y + h - r}`,
    `L${x},${y + r}`,
    `A${r},${r} 0 0 1 ${x + r},${y}`,
    'Z',
  ].join(' ');
}

/** Circle → two-arc path centered at (cx,cy) with radius r. */
function circlePath(cx, cy, r) {
  return [
    `M${cx - r},${cy}`,
    `A${r},${r} 0 1 0 ${cx + r},${cy}`,
    `A${r},${r} 0 1 0 ${cx - r},${cy}`,
    'Z',
  ].join(' ');
}

const svg = readFileSync(input, 'utf8');

const root = svg.match(/<svg[^>]*>/);
if (!root) throw new Error('no <svg> root');
const viewBox = parseAttr(root[0], 'viewBox');
const vb = viewBox ? viewBox.split(/[\s,]+/).map(Number) : [0, 0, 1024, 1024];
const [, , vbW, vbH] = vb;

if (/<defs|<linearGradient|<radialGradient|<filter|<text|<g\b|transform=/i.test(svg)) {
  throw new Error('unsupported SVG feature: gradients/filters/text/groups/transforms');
}

const paths = [];

const rectRe = /<rect\b[^>]*>/g;
let m;
while ((m = rectRe.exec(svg))) {
  const raw = m[0];
  const x = parseFloat(parseAttr(raw, 'x') ?? '0');
  const y = parseFloat(parseAttr(raw, 'y') ?? '0');
  const w = parseFloat(parseAttr(raw, 'width'));
  const h = parseFloat(parseAttr(raw, 'height'));
  const rx = parseFloat(parseAttr(raw, 'rx') ?? '0');
  const fill = parseAttr(raw, 'fill');
  if (w == null || h == null) throw new Error('rect without width/height');
  if (!fill) throw new Error('unsupported: stroked rect');
  paths.push({ pathData: roundedRectPath(x, y, w, h, rx), fill: parseColor(fill) });
}

const pathRe = /<path\b[^>]*>/g;
while ((m = pathRe.exec(svg))) {
  const raw = m[0];
  const d = parseAttr(raw, 'd');
  if (!d) throw new Error('path without d');
  const stroke = parseAttr(raw, 'stroke');
  const strokeWidth = parseFloat(parseAttr(raw, 'stroke-width') ?? '0');
  if (!stroke || strokeWidth <= 0) throw new Error('unsupported: fill-only path');
  const lineCap = parseAttr(raw, 'stroke-linecap');
  if (lineCap && lineCap !== 'round') throw new Error(`unsupported stroke-linecap "${lineCap}"`);
  paths.push({
    pathData: toPathData(d),
    stroke: parseColor(stroke),
    strokeWidth,
    lineCap: 'round',
    opacity: parseFloat(parseAttr(raw, 'opacity') ?? '1'),
  });
}

const circleRe = /<circle\b[^>]*>/g;
while ((m = circleRe.exec(svg))) {
  const raw = m[0];
  const cx = parseFloat(parseAttr(raw, 'cx'));
  const cy = parseFloat(parseAttr(raw, 'cy'));
  const r = parseFloat(parseAttr(raw, 'r'));
  const fill = parseAttr(raw, 'fill');
  if (cx == null || cy == null || r == null) throw new Error('circle without cx/cy/r');
  if (!fill) throw new Error('unsupported: stroked circle');
  paths.push({ pathData: circlePath(cx, cy, r), fill: parseColor(fill) });
}

if (paths.length === 0) throw new Error('no renderable shapes');

const attrs = [`android:viewportWidth="${vbW}"`, `android:viewportHeight="${vbH}"`];

const elements = paths.map((p) => {
  const parts = ['<path'];
  if (p.fill) parts.push(`android:fillColor="${p.fill}"`);
  if (p.stroke) {
    parts.push(`android:strokeColor="${p.stroke}"`);
    parts.push(`android:strokeWidth="${p.strokeWidth}"`);
    if (p.lineCap) parts.push(`android:strokeLineCap="round"`);
    if (p.opacity != null && p.opacity !== 1) parts.push(`android:alpha="${p.opacity}"`);
  }
  parts.push(`android:pathData="${p.pathData}"`);
  parts.push('/>');
  return parts.join(' ');
});

const xml =
  '<?xml version="1.0" encoding="utf-8"?>\n' +
  '<!-- Generated by brand/scripts/svg-to-vector.mjs from the Kaiteyo brand mark.\n' +
  '     Do not edit by hand — re-run the sync pipeline when the mark changes. -->\n' +
  `<vector xmlns:android="http://schemas.android.com/apk/res/android"\n` +
  `    android:width="${vbW}dp"\n` +
  `    android:height="${vbH}dp"\n` +
  `    ${attrs.join('\n    ')}>\n` +
  elements.map((e) => `    ${e}`).join('\n') +
  '\n</vector>\n';

if (output) {
  mkdirSync(dirname(output), { recursive: true });
  writeFileSync(output, xml);
  console.log(`  wrote ${output} (${paths.length} shapes, ${vbW}×${vbH})`);
}

// ---------------------------------------------------------------------------
// Android adaptive icon layers
// ---------------------------------------------------------------------------
// The launcher icon is a 108×108 adaptive icon: the mark content (strokes +
// dot) is scaled into the 66/108 safe zone, the tile color becomes the
// background layer, and a monochrome layer reuses the mark for themed icons.
// Group transforms (scale/translate) are applied at render time by Android,
// so no path coordinates need re-baking.

if (adaptiveDir) {
  const SAFE = 66; // safe-zone diameter on the 108dp canvas
  // Content bbox of the mark (strokes + dot) in viewBox units.
  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
  for (const p of paths) {
    if (p.stroke) {
      // strokes are stroked paths with round caps
      const nums = p.pathData.match(/-?\d*\.?\d+/g)?.map(Number) ?? [];
      const half = p.strokeWidth / 2;
      for (let i = 0; i < nums.length; i += 2) {
        const x = nums[i], y = nums[i + 1];
        minX = Math.min(minX, x - half); maxX = Math.max(maxX, x + half);
        minY = Math.min(minY, y - half); maxY = Math.max(maxY, y + half);
      }
    } else {
      const nums = p.pathData.match(/-?\d*\.?\d+/g)?.map(Number) ?? [];
      for (let i = 0; i < nums.length; i += 2) {
        minX = Math.min(minX, nums[i]); maxX = Math.max(maxX, nums[i]);
        minY = Math.min(minY, nums[i + 1]); maxY = Math.max(maxY, nums[i + 1]);
      }
    }
  }
  if (!Number.isFinite(minX)) throw new Error('cannot compute mark bbox for adaptive icon');
  const cw = maxX - minX, ch = maxY - minY;
  const cx = (minX + maxX) / 2, cy = (minY + maxY) / 2;
  const scale = (SAFE / 108) * 108 / Math.max(cw, ch);
  // Keep a little breathing room inside the safe zone.
  const fit = (SAFE * 0.86) / Math.max(cw, ch);
  const tx = 54 - cx * fit;
  const ty = 54 - cy * fit;

  const vectorHeader = (extra) =>
    '<?xml version="1.0" encoding="utf-8"?>\n' +
    '<!-- Generated by brand/scripts/svg-to-vector.mjs from the Kaiteyo brand mark.\n' +
    '     Do not edit by hand — re-run the sync pipeline when the mark changes. -->\n' +
    `<vector xmlns:android="http://schemas.android.com/apk/res/android"\n` +
    `    android:width="108dp"\n` +
    `    android:height="108dp"\n` +
    `    android:viewportWidth="108"\n` +
    `    android:viewportHeight="108">\n` +
    extra;

  const pathEl = (p, colorOverride) => {
    const parts = ['<path'];
    if (p.fill) parts.push(`android:fillColor="${colorOverride ?? p.fill}"`);
    if (p.stroke) {
      parts.push(`android:strokeColor="${colorOverride ?? p.stroke}"`);
      parts.push(`android:strokeWidth="${(p.strokeWidth * fit)}"`);
      parts.push(`android:strokeLineCap="round"`);
      if (p.opacity != null && p.opacity !== 1) parts.push(`android:alpha="${p.opacity}"`);
    }
    parts.push(`android:pathData="${p.pathData}"`);
    parts.push('/>');
    return parts.join(' ');
  };

  // Background: the brand tile color fills the full canvas (masked by the OS).
  const tileFill = paths.find((p) => p.fill)?.fill ?? '#050505';
  const background =
    vectorHeader('') +
    `    <path android:fillColor="${tileFill}" android:pathData="M0,0h108v108h-108z" />\n` +
    '</vector>\n';

  const foregroundInner = paths
    .filter((p) => !p.fill || p.stroke) // strokes + dot only, no tile rect
    .map((p) => pathEl(p))
    .join('\n');
  const foreground =
    vectorHeader('') +
    `    <group android:scaleX="${fit}" android:scaleY="${fit}" android:translateX="${tx}" android:translateY="${ty}">\n` +
    foregroundInner.split('\n').map((l) => `        ${l}`).join('\n') +
    '\n    </group>\n' +
    '</vector>\n';

  // Monochrome: same content, single-color (#000 is what the OS tints).
  const monochrome =
    vectorHeader('') +
    `    <group android:scaleX="${fit}" android:scaleY="${fit}" android:translateX="${tx}" android:translateY="${ty}">\n` +
    paths
      .filter((p) => !p.fill || p.stroke)
      .map((p) => pathEl(p, '#000000'))
      .join('\n')
      .split('\n').map((l) => `        ${l}`).join('\n') +
    '\n    </group>\n' +
    '</vector>\n';

  mkdirSync(adaptiveDir, { recursive: true });
  writeFileSync(join(adaptiveDir, 'drawable_icon_background.xml'), background);
  writeFileSync(join(adaptiveDir, 'drawable_icon_foreground.xml'), foreground);
  writeFileSync(join(adaptiveDir, 'drawable_icon_monochrome.xml'), monochrome);
  console.log(`  wrote Android adaptive icon layers (${adaptiveDir})`);
}
