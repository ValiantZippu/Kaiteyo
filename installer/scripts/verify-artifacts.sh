#!/usr/bin/env bash
# ============================================================
# Kaiteyo — verify staged release artifacts
#
# Sanity-checks a staged release directory:
#   - every manifest entry has its file present
#   - sha256 matches the manifest
#   - no leftover build temp files
#
# Exit code 0 = release is safe to publish.
#
# Usage: bash installer/scripts/verify-artifacts.sh <version>
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
VERSION="${1:?usage: verify-artifacts.sh <version>}"
RELEASE_DIR="$ROOT/release/kaiteyo-$VERSION"
MANIFEST="$RELEASE_DIR/artifact-manifest.json"

[[ -f "$MANIFEST" ]] || { echo "error: $MANIFEST not found" >&2; exit 1; }

FAIL=0

while IFS= read -r file; do
  if [[ -f "$RELEASE_DIR/$file" ]]; then
    echo "  ok   $file"
  else
    echo "  FAIL missing: $file" >&2
    FAIL=1
  fi
done < <(jq -r '.artifacts[].file' "$MANIFEST")

while IFS=$'\t' read -r file expected; do
  actual=$(sha256sum "$RELEASE_DIR/$file" | cut -d' ' -f1)
  if [[ "$actual" == "$expected" ]]; then
    echo "  ok   sha256 $file"
  else
    echo "  FAIL sha256 $file (expected $expected, got $actual)" >&2
    FAIL=1
  fi
done < <(jq -r '.artifacts[] | [.file, .sha256] | @tsv' "$MANIFEST")

# Forbidden files in a release dir
for banned in "*.tmp" "*.log" "*.bak" "*.pdb"; do
  if compgen -G "$RELEASE_DIR/$banned" >/dev/null; then
    echo "  FAIL stray file matching $banned" >&2
    FAIL=1
  fi
done

if [[ $FAIL -eq 0 ]]; then
  echo "== $RELEASE_DIR is verified and publishable =="
else
  echo "== $RELEASE_DIR FAILED verification ==" >&2
  exit 1
fi
