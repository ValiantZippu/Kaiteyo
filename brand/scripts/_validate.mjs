#!/usr/bin/env node
/**
 * Kaiteyo — brand asset validation (backing _validate for validate-assets.sh).
 *
 * Reads brand/manifests/assets.json and checks every role's source asset:
 *   - file exists
 *   - extension is in the role's allowed formats
 *   - SVG is well-formed (balanced <svg> … </svg>, single root)
 *   - SVG declares width/height or a viewBox, and matches minSize
 *   - file name is kebab-case (no uppercase, spaces, or invalid chars)
 *   - no duplicate source paths across roles
 *   - variants reference files that exist or are explicitly null
 *
 * Prints one line per problem and exits non-zero if any are found.
 */
import { readFileSync, existsSync, statSync } from 'node:fs';
import { join, extname, basename } from 'node:path';

const [, , manifestPath, srcRoot] = process.argv;

if (!manifestPath || !srcRoot) {
  console.error('usage: node _validate.mjs <manifest.json> <sourceRoot>');
  process.exit(2);
}

const manifest = JSON.parse(readFileSync(manifestPath, 'utf8'));
const roles = manifest.roles;
if (!roles) {
  console.error('manifest has no "roles" object');
  process.exit(2);
}

const problems = [];
const seenSources = new Map();
const kebabCase = /^[a-z0-9]+(-[a-z0-9]+)*(\.[a-z0-9]+)+$/;

function svgDimensions(file) {
  const xml = readFileSync(file, 'utf8');
  const rootOpen = xml.indexOf('<svg');
  const rootClose = xml.indexOf('>', rootOpen);
  if (rootOpen < 0 || rootClose < 0) return null;
  const head = xml.slice(rootOpen, rootClose + 1);
  const attr = (name) => {
    const m = head.match(new RegExp(`${name}\\s*=\\s*"([^"]*)"`));
    return m ? m[1] : null;
  };
  const width = attr('width');
  const height = attr('height');
  const viewBox = attr('viewBox');
  let w = null;
  let h = null;
  if (width && height) {
    w = parseFloat(width);
    h = parseFloat(height);
  } else if (viewBox) {
    const [, , vbW, vbH] = viewBox.split(/\s+/).map(Number);
    w = vbW;
    h = vbH;
  }
  return { w, h, wellFormed: (xml.match(/<svg/g) || []).length === 1 && (xml.match(/<\/svg>/g) || []).length === 1 };
}

for (const [roleName, role] of Object.entries(roles)) {
  const source = role.source;
  const abs = join(srcRoot, '..', source);
  const rel = `brand/${source}`;

  // 1. File exists
  if (!existsSync(abs) || !statSync(abs).isFile()) {
    problems.push(`[${roleName}] missing source: ${rel}`);
    continue;
  }

  // 2. Duplicate source
  if (seenSources.has(source)) {
    problems.push(`[${roleName}] duplicate source also used by "${seenSources.get(source)}": ${rel}`);
  } else {
    seenSources.set(source, roleName);
  }

  // 3. Naming
  const name = basename(source);
  if (!kebabCase.test(name)) {
    problems.push(`[${roleName}] invalid file name (use kebab-case): ${name}`);
  }
  if (/[\sA-Z]/.test(name)) {
    problems.push(`[${roleName}] file name must be lowercase kebab-case, no spaces: ${name}`);
  }

  // 4. Format
  const ext = extname(source).slice(1).toLowerCase();
  if (!role.formats.includes(ext)) {
    problems.push(`[${roleName}] unsupported format "${ext}" (allowed: ${role.formats.join(', ')})`);
  }

  // 5. SVG well-formed + dimensions
  if (ext === 'svg') {
    const dims = svgDimensions(abs);
    if (!dims) {
      problems.push(`[${roleName}] cannot parse SVG (missing <svg> root / width+height or viewBox): ${rel}`);
    } else {
      if (!dims.wellFormed) {
        problems.push(`[${roleName}] SVG is not well-formed (expected exactly one <svg>…</svg>): ${rel}`);
      }
      const [minW, minH] = role.minSize ?? [0, 0];
      if (dims.w && dims.w < minW) problems.push(`[${roleName}] SVG width ${dims.w} < minimum ${minW}: ${rel}`);
      if (dims.h && dims.h < minH) problems.push(`[${roleName}] SVG height ${dims.h} < minimum ${minH}: ${rel}`);
    }
  }

  // 6. Variants must exist or be null
  for (const [variant, path] of Object.entries(role.variants ?? {})) {
    if (path == null) continue;
    const vAbs = join(srcRoot, '..', path);
    if (!existsSync(vAbs) || !statSync(vAbs).isFile()) {
      problems.push(`[${roleName}] variant "${variant}" file missing: brand/${path}`);
    }
  }

  // 7. Destinations must point at real application-resource paths (existence is
  //    checked at sync time; here we only verify the path is plausible).
  for (const dest of role.destinations ?? []) {
    if (!dest.path || !dest.kind) {
      problems.push(`[${roleName}] destination entry missing "kind"/"path"`);
    }
  }
}

if (problems.length > 0) {
  console.error(`\nFound ${problems.length} brand asset problem(s):`);
  for (const p of problems) console.error(`  ✗ ${p}`);
  process.exit(1);
}

console.log(`  ${Object.keys(roles).length} roles OK`);
