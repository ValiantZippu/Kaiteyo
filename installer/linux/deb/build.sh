#!/usr/bin/env bash
# ============================================================================
# Kaiteyo — Debian/Ubuntu package (deb)
#
# Builds a standards-compliant .deb from the jpackage image using dpkg-deb:
#   - /usr/lib/kaiteyo for the app payload
#   - /usr/bin/kaiteyo wrapper
#   - hicolor icon theme + AppStream metainfo
#   - postinst/prerm hooks refresh desktop + icon caches
#
# Usage: bash installer/linux/deb/build.sh [version]
# ============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
VERSION="${1:-$(jq -r '.version' "$ROOT/installer/common/version.json")}"

SRC_APP="$ROOT/desktopApp/build/compose/binaries/main/app/Kaiteyo"
[[ -d "$SRC_APP" ]] || { echo "error: $SRC_APP missing" >&2; exit 1; }

HERE="$ROOT/installer/linux/deb"
OUT="$HERE/out"
PKG="$OUT/pkg"
rm -rf "$PKG" "$OUT"
mkdir -p "$PKG/DEBIAN" \
         "$PKG/usr/lib/kaiteyo" \
         "$PKG/usr/bin" \
         "$PKG/usr/share/applications" \
         "$PKG/usr/share/metainfo" \
         "$PKG/usr/share/doc/kaiteyo"

echo "== Staging deb payload ($VERSION) =="
cp -r "$SRC_APP"/* "$PKG/usr/lib/kaiteyo/"
mv "$PKG/usr/lib/kaiteyo/bin/Kaiteyo" "$PKG/usr/lib/kaiteyo/bin/kaiteyo" 2>/dev/null || true

# Launcher wrapper
cat > "$PKG/usr/bin/kaiteyo" <<'EOF'
#!/bin/sh
exec /usr/lib/kaiteyo/bin/kaiteyo "$@"
EOF
chmod +x "$PKG/usr/bin/kaiteyo"

# Desktop entry + metainfo
cp "$ROOT/installer/linux/appimage/io.github.syt0r.kaiteyo.desktop" "$PKG/usr/share/applications/io.github.syt0r.kaiteyo.desktop"
cp "$ROOT/installer/linux/appimage/io.github.syt0r.kaiteyo.metainfo.xml" "$PKG/usr/share/metainfo/"

# Icons
ICONS="$ROOT/installer/assets/generated/linux"
for size in 16 32 48 64 128 256 512; do
  mkdir -p "$PKG/usr/share/icons/hicolor/${size}x${size}/apps"
  cp "$ICONS/kaiteyo-$size.png" "$PKG/usr/share/icons/hicolor/${size}x${size}/apps/io.github.syt0r.kaiteyo.png"
done
mkdir -p "$PKG/usr/share/icons/hicolor/scalable/apps"
cp "$ICONS/kaiteyo.svg" "$PKG/usr/share/icons/hicolor/scalable/apps/io.github.syt0r.kaiteyo.svg"

SIZE_KB=$(du -sk "$PKG/usr" | cut -f1)

# Control file
cat > "$PKG/DEBIAN/control" <<EOF
Package: kaiteyo
Version: $VERSION
Section: education
Priority: optional
Architecture: amd64
Maintainer: syt0r <syt0r@users.noreply.github.com>
Installed-Size: $SIZE_KB
Depends: libx11-6, libxext6, libxi6, libxrender1, libxtst6, libgl1, libfontconfig1
Homepage: https://github.com/ValiantZippu/Kaiteyo
Description: Japanese language learning app with writing exercises, flashcards and a built-in dictionary
 Kaiteyo (書いてよ) is a premium Japanese language learning app. Learn kana and
 kanji with writing exercises, review with FSRS spaced repetition, and look up
 words in a built-in dictionary. Works fully offline.
EOF

cat > "$PKG/DEBIAN/postinst" <<'EOF'
#!/bin/sh
set -e
update-desktop-database /usr/share/applications >/dev/null 2>&1 || true
gtk-update-icon-cache /usr/share/icons/hicolor >/dev/null 2>&1 || true
EOF
chmod +x "$PKG/DEBIAN/postinst"

cat > "$PKG/DEBIAN/prerm" <<'EOF'
#!/bin/sh
set -e
EOF
chmod +x "$PKG/DEBIAN/prerm"

echo "== Building .deb =="
dpkg-deb --build --root-owner-group "$PKG" "$OUT/kaiteyo_${VERSION}_amd64.deb"
echo "== Done: $(ls -1 "$OUT") =="
