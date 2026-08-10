#!/usr/bin/env bash
# ============================================================
# Kaiteyo — bump version everywhere at once
#
# Updates the single source of truth (installer/common/version.json)
# and the Gradle AppVersion.kt so they can never drift apart.
#
# Usage: bash installer/scripts/bump-version.sh <version> [version_code]
#   version       e.g. 2.3.0   (MAJOR.MINOR.PATCH)
#   version_code  e.g. 2310    (optional; defaults to existing+10)
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
VERSION="${1:?usage: bump-version.sh <version> [version_code]}"

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "error: version must be MAJOR.MINOR.PATCH (got '$VERSION')" >&2
  exit 1
fi

VERSION_JSON="$ROOT/installer/common/version.json"
APP_VERSION_KT="$ROOT/buildSrc/src/main/kotlin/AppVersion.kt"

# Current values
CUR_JSON_VC=$(jq -r '.version_code' "$VERSION_JSON" 2>/dev/null || echo 0)
NEW_CODE="${2:-$((CUR_JSON_VC + 10))}"

echo "Bumping Kaiteyo to $VERSION (version_code $NEW_CODE)"
echo "  - $VERSION_JSON"
jq --arg v "$VERSION" --argjson c "$NEW_CODE" \
   '.version = $v | .version_code = $c' "$VERSION_JSON" \
   > "$VERSION_JSON.tmp" && mv "$VERSION_JSON.tmp" "$VERSION_JSON"

echo "  - $APP_VERSION_KT"
sed -i \
  -e "s/const val versionCode = [0-9]*/const val versionCode = $NEW_CODE/" \
  -e "s/const val versionName = \"[^\"]*\"/const val versionName = \"$VERSION\"/" \
  -e "s/const val desktopAppVersion = \"[^\"]*\"/const val desktopAppVersion = \"$VERSION\"/" \
  "$APP_VERSION_KT"

echo "  - AppVersion.kt now:"
grep "const val" "$APP_VERSION_KT" | sed 's/^/    /'
echo "Done. Commit both files together."
