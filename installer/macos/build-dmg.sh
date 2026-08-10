#!/usr/bin/env bash
# ============================================================================
# Kaiteyo — styled macOS DMG builder
#
# Wraps the jpackage .app bundle in a premium drag-to-Applications DMG:
#   - branded background artwork (installer/assets/macos/dmg-background.svg)
#   - correct icon placement (app left, /Applications symlink right)
#   - volume icon + window chrome
#   - optional: re-sign + notarize (see notarize.sh)
#
# macOS host only. Requires: create-dmg (brew install create-dmg), or falls
# back to hdiutil + AppleScript when create-dmg is missing.
#
# Usage: bash installer/macos/build-dmg.sh <arch> [version]
#   arch    arm64 | x64     (matches the jpackage image to wrap)
#   version e.g. 2.2.1       (defaults to common/version.json)
# ============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ARCH="${1:?usage: build-dmg.sh <arm64|x64> [version]}"
VERSION="${2:-$(jq -r '.version' "$ROOT/installer/common/version.json")}"

APP_DIR="$ROOT/desktopApp/build/compose/binaries/main/app/Kaiteyo.app"
[[ -d "$APP_DIR" ]] || { echo "error: $APP_DIR missing — run :desktopApp:createDistributable" >&2; exit 1; }

OUT="$ROOT/desktopApp/build/compose/binaries/main/dmg"
mkdir -p "$OUT"
DMG="$OUT/Kaiteyo-$VERSION-macos-$ARCH.dmg"

# Background artwork (render 2x for retina, keep 1x fallback).
BG_DIR="$ROOT/installer/assets/generated/macos"
BG1="$BG_DIR/dmg-background.png"
BG2="$BG_DIR/dmg-background@2x.png"
[[ -f "$BG1" ]] || bash "$ROOT/installer/scripts/generate-assets.sh"

# Working copy — create-dmg may re-sign/stamp; never touch the source bundle.
STAGE="$(mktemp -d)/Kaiteyo.app"
cp -R "$APP_DIR" "$STAGE"
chmod -R u+w "$STAGE"

# Icon: reuse the app's own icns for the volume.
VOL_ICON="$APP_DIR/Contents/Resources/Kaiteyo.icns"
[[ -f "$VOL_ICON" ]] || VOL_ICON="$(find "$APP_DIR/Contents" -name '*.icns' | head -1)"

if command -v create-dmg >/dev/null 2>&1; then
  echo "== create-dmg: $DMG =="
  create-dmg \
    --volname "Kaiteyo $VERSION" \
    --volicon "$VOL_ICON" \
    --background "$BG2" \
    --window-pos 200 120 \
    --window-size 660 400 \
    --icon-size 128 \
    --icon "Kaiteyo.app" 180 190 \
    --app-drop-link 480 190 \
    --no-internet-enable \
    "$DMG" \
    "$(dirname "$STAGE")"
else
  echo "== create-dmg not found — using hdiutil fallback (plainer look) =="
  STAGE_DIR="$(dirname "$STAGE")"
  ln -s /Applications "$STAGE_DIR/Applications"
  hdiutil create -volname "Kaiteyo $VERSION" -srcfolder "$STAGE_DIR" -ov -format UDZO "$DMG"
fi

rm -rf "$STAGE"
echo "== Done: $DMG =="
echo "  Next: bash installer/macos/notarize.sh \"$DMG\" (if you have Apple credentials)"
