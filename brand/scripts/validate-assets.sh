#!/usr/bin/env bash
# ============================================================
# Kaiteyo — validate brand assets against the manifest
# Fails loudly on: missing files, unsupported formats,
# undersized assets, duplicate names, invalid characters.
# Never silently substitutes anything.
#
# Usage: bash brand/scripts/validate-assets.sh
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BRAND="$ROOT/brand"
MANIFEST="$BRAND/manifests/assets.json"
SRC="$BRAND/source"

# Node is required for JSON parsing (available in CI/dev; no other deps).
if ! command -v node >/dev/null 2>&1; then
  echo "  !! node not found — required to parse $MANIFEST" >&2
  exit 1
fi

echo "== Kaiteyo brand asset validation =="
echo "  manifest: $MANIFEST"

node "$BRAND/scripts/_validate.mjs" "$MANIFEST" "$SRC"
code=$?
if [ "$code" -ne 0 ]; then
  echo "  !! validation FAILED (exit $code)" >&2
  exit "$code"
fi
echo "  validation OK"
