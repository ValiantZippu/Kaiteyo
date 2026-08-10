#!/usr/bin/env bash
# ============================================================================
# Kaiteyo — Snap build (optional format)
#
# The canonical Snap manifest lives at desktopApp/linux/snapcraft/snapcraft.yaml
# (kept in the Gradle tree for historical reasons). This wrapper modernizes the
# build inputs: it swaps the stale jar-based launcher for the jpackage image.
#
# NOTE: Snap is currently OPTIONAL. The default release set is
# AppImage + deb + rpm + Flatpak. Enable this only after snapcraft review passes.
#
# Requires: snapcraft (classic confinement)
# Usage: bash installer/linux/snap/build.sh [version]
# ============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
VERSION="${1:-$(jq -r '.version' "$ROOT/installer/common/version.json")}"

SRC_APP="$ROOT/desktopApp/build/compose/binaries/main/app/Kaiteyo"
[[ -d "$SRC_APP" ]] || { echo "error: $SRC_APP missing" >&2; exit 1; }

command -v snapcraft >/dev/null 2>&1 || { echo "error: snapcraft not found" >&2; exit 1; }

OUT="$ROOT/installer/linux/snap/out"
mkdir -p "$OUT"

# Stage the jpackage image where the snapcraft part expects it.
SNAP_SRC="$ROOT/desktopApp/linux/snapcraft/local/app"
rm -rf "$SNAP_SRC"
mkdir -p "$SNAP_SRC"
cp -r "$SRC_APP"/* "$SNAP_SRC/"

# Update the launcher to use the bundled image instead of a bare jar.
cat > "$ROOT/desktopApp/linux/snapcraft/local/launch-kaiteyo.sh" <<'EOF'
#!/bin/bash
export SNAP_DATA_DIR="$SNAP_USER_DATA"
export JAVA_HOME="$SNAP/local/app/runtime"
exec "$SNAP/local/app/bin/Kaiteyo" "$@"
EOF
chmod +x "$ROOT/desktopApp/linux/snapcraft/local/launch-kaiteyo.sh"

echo "== Building Snap ($VERSION) =="
(
  cd "$ROOT/desktopApp/linux/snapcraft"
  snapcraft --output "$OUT/kaiteyo_${VERSION}_amd64.snap"
)

echo "== Done: $(ls -1 "$OUT") =="
echo "  (Optional format — not part of the default release set.)"
