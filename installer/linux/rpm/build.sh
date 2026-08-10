#!/usr/bin/env bash
# ============================================================================
# Kaiteyo — RPM build wrapper
#
# Requires: rpmbuild (rpmdevtools). Fedora/RHEL/CI only — Ubuntu builders
# should rely on the deb instead.
#
# Usage: bash installer/linux/rpm/build.sh [version]
# ============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
VERSION="${1:-$(jq -r '.version' "$ROOT/installer/common/version.json")}"

SRC_APP="$ROOT/desktopApp/build/compose/binaries/main/app/Kaiteyo"
[[ -d "$SRC_APP" ]] || { echo "error: $SRC_APP missing" >&2; exit 1; }

HERE="$ROOT/installer/linux/rpm"
OUT="$HERE/out"
mkdir -p "$OUT"

command -v rpmbuild >/dev/null 2>&1 || { echo "error: rpmbuild not found (install rpmdevtools)" >&2; exit 1; }

# --- Prepare sources: app image tarball + metadata ---------------------------
SOURCES="$HERE/rpmbuild/SOURCES"
rm -rf "$HERE/rpmbuild"
mkdir -p "$SOURCES"

tar -C "$ROOT/desktopApp/build/compose/binaries/main" \
  -cJf "$SOURCES/kaiteyo-$VERSION-image.tar.xz" app

cp "$ROOT/installer/linux/appimage/io.github.syt0r.kaiteyo.desktop" "$SOURCES/io.github.syt0r.kaiteyo.desktop"
cp "$ROOT/installer/linux/appimage/io.github.syt0r.kaiteyo.metainfo.xml" "$SOURCES/io.github.syt0r.kaiteyo.metainfo.xml"
ICONS="$ROOT/installer/assets/generated/linux"
for size in 16 32 48 64 128 256 512; do
  cp "$ICONS/kaiteyo-$size.png" "$SOURCES/kaiteyo-$size.png"
done
cp "$ICONS/kaiteyo.svg" "$SOURCES/kaiteyo.svg"

# --- Build -------------------------------------------------------------------
echo "== Building RPM ($VERSION) =="
rpmbuild -bb \
  --define "_topdir $HERE/rpmbuild" \
  --define "_version $VERSION" \
  --define "_sourcedir $SOURCES" \
  --define "_rpmdir $OUT" \
  "$HERE/build.spec"

echo "== Done: $(ls -1 "$OUT") =="
