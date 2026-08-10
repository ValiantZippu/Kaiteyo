#!/usr/bin/env bash
# ============================================================================
# Kaiteyo — AppImage build
#
# Wraps the jpackage image in an AppDir with full desktop integration:
#   - multi-size icon theme (16..512 + svg)
#   - proper .desktop entry + AppStream metainfo
#   - AppRun launcher
#
# Usage: bash installer/linux/appimage/build.sh [version]
#   (AppImage is built for the host architecture; CI matrix builds x86_64 + aarch64)
# ============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
VERSION="${1:-$(jq -r '.version' "$ROOT/installer/common/version.json")}"
ARCH="$(uname -m)"
case "$ARCH" in
  x86_64) APPIMAGE_ARCH="x86_64" ;;
  aarch64|arm64) APPIMAGE_ARCH="aarch64" ;;
  *) echo "unsupported arch: $ARCH" >&2; exit 1 ;;
esac

SRC_APP="$ROOT/desktopApp/build/compose/binaries/main/app/Kaiteyo"
[[ -d "$SRC_APP" ]] || { echo "error: $SRC_APP missing — run :desktopApp:createDistributable" >&2; exit 1; }

HERE="$ROOT/installer/linux/appimage"
OUT="$HERE/out"
APPDIR="$HERE/AppDir"
rm -rf "$APPDIR" "$OUT"
mkdir -p "$APPDIR/usr" "$OUT"

echo "== Assembling AppDir ($APPIMAGE_ARCH) =="
cp -r "$SRC_APP"/* "$APPDIR/usr/"
# Keep the jpackage binary name so the desktop entry (Exec=Kaiteyo) stays correct.
cp "$HERE/io.github.syt0r.kaiteyo.desktop" "$APPDIR/io.github.syt0r.kaiteyo.desktop"
cp "$HERE/io.github.syt0r.kaiteyo.metainfo.xml" "$APPDIR/usr/share/metainfo/"

# Icon theme
ICONS="$ROOT/installer/assets/generated/linux"
for size in 16 32 48 64 128 256 512; do
  mkdir -p "$APPDIR/usr/share/icons/hicolor/${size}x${size}/apps"
  cp "$ICONS/kaiteyo-$size.png" "$APPDIR/usr/share/icons/hicolor/${size}x${size}/apps/io.github.syt0r.kaiteyo.png"
done
mkdir -p "$APPDIR/usr/share/icons/hicolor/scalable/apps"
cp "$ICONS/kaiteyo.svg" "$APPDIR/usr/share/icons/hicolor/scalable/apps/io.github.syt0r.kaiteyo.svg"
cp "$ICONS/kaiteyo-512.png" "$APPDIR/io.github.syt0r.kaiteyo.png"

# AppRun (AppImage runtime entry)
cat > "$APPDIR/AppRun" <<'EOF'
#!/bin/sh
HERE="$(dirname "$(readlink -f "$0")")"
export PATH="$HERE/usr/bin:$PATH"
exec "$HERE/usr/bin/Kaiteyo" "$@"
EOF
chmod +x "$APPDIR/AppRun"

# Runtime
if [ ! -f "$HERE/appimagetool-$APPIMAGE_ARCH.AppImage" ]; then
  echo "== Downloading appimagetool ($APPIMAGE_ARCH) =="
  wget -q "https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-$APPIMAGE_ARCH.AppImage" \
    -O "$HERE/appimagetool-$APPIMAGE_ARCH.AppImage"
  chmod +x "$HERE/appimagetool-$APPIMAGE_ARCH.AppImage"
fi

echo "== Building Kaiteyo-$VERSION-$APPIMAGE_ARCH.AppImage =="
"$HERE/appimagetool-$APPIMAGE_ARCH.AppImage" \
  --appimage-extract-and-run \
  "$APPDIR" \
  "$OUT/Kaiteyo-$VERSION-$APPIMAGE_ARCH.AppImage"

chmod +x "$OUT/Kaiteyo-$VERSION-$APPIMAGE_ARCH.AppImage"
echo "== Done: $(ls -1 "$OUT") =="
