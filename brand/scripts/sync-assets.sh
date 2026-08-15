#!/usr/bin/env bash
# ============================================================
# Kaiteyo — sync brand assets into application resources
#
# Pipeline:  brand/source  →  validate  →  brand/processed  →
#            app resources (core drawables, window icon,
#            Android/iOS/linux icons, website, installer).
#
# The user's originals in brand/source/ are NEVER modified.
#
# Usage: bash brand/scripts/sync-assets.sh
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BRAND="$ROOT/brand"
SRC="$BRAND/source"
PROCESSED="$BRAND/processed"
GENERATED="$BRAND/generated"

echo "== Kaiteyo brand asset sync =="

# 1. Validate first — never sync broken assets.
bash "$BRAND/scripts/validate-assets.sh"

mkdir -p "$PROCESSED" "$GENERATED"

# 2. Copy validated sources into brand/processed/ (the working copy).
cp -f "$SRC"/logos/*.svg "$PROCESSED/" 2>/dev/null || true
cp -f "$SRC"/marks/*.svg "$PROCESSED/" 2>/dev/null || true
cp -f "$SRC"/app-icons/*.svg "$PROCESSED/" 2>/dev/null || true
cp -f "$SRC"/banners/*.svg "$PROCESSED/" 2>/dev/null || true
cp -f "$SRC"/favicons/*.svg "$PROCESSED/" 2>/dev/null || true
cp -f "$SRC"/promotional/*.svg "$PROCESSED/" 2>/dev/null || true
echo "  copied sources -> brand/processed/"

# 3. Core shared drawable: app mark as an XML vector drawable (works on
#    Android + iOS + desktop via Compose resources; SVG is Android-unsafe).
#    Generated from the source mark by svg-to-vector.mjs — never hand-edited.
#    The same call emits the Android adaptive-icon layers (background /
#    foreground / monochrome) derived from the mark.
if [ -f "$SRC/marks/app-mark.svg" ]; then
  node "$BRAND/scripts/svg-to-vector.mjs" \
    --in  "$SRC/marks/app-mark.svg" \
    --out "$ROOT/core/src/commonMain/composeResources/drawable/kaiteyo_mark.xml" \
    --adaptive-dir "$ROOT/app/src/main/res/drawable"
  echo "  wrote core drawable + Android adaptive icon layers"
fi

# 4. Generate binary icon set from the app-icon master.
if [ -f "$SRC/app-icons/app-icon.svg" ]; then
  node "$BRAND/scripts/render-icons.mjs" \
    --in  "$SRC/app-icons/app-icon.svg" \
    --out "$GENERATED/kaiteyo-app-icon" \
    --png 128,256,512,1024 \
    --ico 16,32,48,64,128,256 \
    --icns 128,256,512,1024

  # Desktop window icon (Compose desktop + Linux jpackage icon)
  cp -f "$GENERATED/kaiteyo-app-icon-256.png" \
        "$ROOT/desktopApp/src/jvmMain/composeResources/drawable/windowIcon.png"
  # Desktop OS package icons (Windows .ico, macOS .icns)
  cp -f "$GENERATED/kaiteyo-app-icon.ico"  "$ROOT/desktopApp/windows_icon.ico"
  cp -f "$GENERATED/kaiteyo-app-icon.icns" "$ROOT/desktopApp/mac_icon.icns"
  # iOS app icon (1024×1024, no transparency)
  cp -f "$GENERATED/kaiteyo-app-icon-1024.png" \
        "$ROOT/iosApp/KaiteyoApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png"
  # Linux flatpak + snap icons
  cp -f "$GENERATED/kaiteyo-app-icon-256.png" \
        "$ROOT/desktopApp/linux/flatpak/io.github.syt0r.kaiteyo.png"
  cp -f "$GENERATED/kaiteyo-app-icon-128.png" \
        "$ROOT/desktopApp/linux/snapcraft/gui/icon.png"
  echo "  generated + copied desktop/iOS/linux icons"
fi

# 5. Installer brand mark (single source → installer asset copy).
if [ -f "$SRC/marks/app-mark.svg" ]; then
  cp -f "$SRC/marks/app-mark.svg" "$ROOT/installer/assets/brand/kaiteyo-mark.svg"
  echo "  installer mark updated (installer/assets/brand/kaiteyo-mark.svg)"
fi

# 6. Website favicon + mark (build.py synthesizes og-cover from these).
if [ -f "$SRC/favicons/favicon.svg" ]; then
  cp -f "$SRC/favicons/favicon.svg" "$PROCESSED/favicon.svg"
  echo "  website favicon source ready (brand/processed/favicon.svg)"
fi

echo "== sync complete =="
