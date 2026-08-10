#!/usr/bin/env bash
# ============================================================
# Kaiteyo — generate installer brand assets
# Turns SVG sources in installer/assets/ into the binary
# formats each packaging tool needs (bmp/ico/icns/png).
#
# Requirements (all optional per-artifact):
#   rsvg-convert (librsvg)   — SVG → PNG (recommended)
#   ImageMagick `convert`    — PNG → ICO/BMP fallbacks
#   png2icns / icnsutils     — PNG → ICNS (macOS)
#   iconutil                 — macOS-native PNG→ICNS
#
# Usage: bash installer/scripts/generate-assets.sh
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ASSETS="$ROOT/installer/assets"
OUT="$ASSETS/generated"

mkdir -p "$OUT/windows" "$OUT/macos" "$OUT/linux"

have() { command -v "$1" >/dev/null 2>&1; }

# Resolve an ImageMagick binary once (IM7 = magick, IM6 = convert).
if have magick; then
  IMG="magick"
elif have convert; then
  IMG="convert"
else
  IMG=""
fi

svg_to_png() {
  local src="$1" out="$2" w="$3" h="$4"
  if have rsvg-convert; then
    rsvg-convert -w "$w" -h "$h" "$src" -o "$out"
  elif [[ -n "$IMG" ]]; then
    "$IMG" -background none -resize "${w}x${h}" "$src" "$out"
  else
    echo "  !! missing rsvg-convert/ImageMagick, cannot render $src — set-up continues"
    return 0
  fi
  echo "  generated ${out#$ROOT/}"
}

echo "== Kaiteyo installer assets =="

# --- Windows: banner (164x314), small image (150x57), icon ico ---------------
svg_to_png "$ASSETS/windows/banner.svg"  "$OUT/windows/banner.png"     164 314
svg_to_png "$ASSETS/windows/banner.svg"  "$OUT/windows/banner-dark.png" 164 314
svg_to_png "$ASSETS/windows/welcome.svg" "$OUT/windows/welcome.png"    150 57

if [[ -n "$IMG" ]]; then
  # BMP24 keeps Inno happy; wizard images must be exact size.
  "$IMG" "$OUT/windows/banner.png"  -depth 24 BMP3:"$OUT/windows/banner.bmp"
  "$IMG" "$OUT/windows/banner-dark.png" -negate -depth 24 BMP3:"$OUT/windows/banner-dark.bmp"
  # NOTE: for a true dark variant provide a dedicated dark SVG; -negate is a
  # placeholder so Inno's dynamic mode has something to load.
  "$IMG" "$OUT/windows/welcome.png" -depth 24 BMP3:"$OUT/windows/welcome.bmp"
  # Icon: multi-size ico for SetupIconFile / uninstaller.
  "$IMG" -background none "$ASSETS/brand/kaiteyo-mark.svg" \
    -define icon:auto-resize=256,128,64,48,32,16 \
    "$OUT/windows/kaiteyo.ico"
  echo "  generated windows/kaiteyo.ico (multi-size)"
else
  echo "  !! ImageMagick not found — skipping bmp/ico (Windows build needs them)"
fi

# --- macOS: DMG background (660x400 + 2x retina) ------------------------------
svg_to_png "$ASSETS/macos/dmg-background.svg" "$OUT/macos/dmg-background.png" 660 400
svg_to_png "$ASSETS/macos/dmg-background.svg" "$OUT/macos/dmg-background@2x.png" 1320 800

# --- Linux: icon theme (svg + multi-res png) ----------------------------------
svg_to_png "$ASSETS/brand/kaiteyo-mark.svg" "$OUT/linux/kaiteyo.svg" 512 512
for size in 16 32 48 64 128 256 512; do
  svg_to_png "$ASSETS/brand/kaiteyo-mark.svg" "$OUT/linux/kaiteyo-${size}.png" "$size" "$size"
done

echo "== done. Generated assets are in installer/assets/generated/ =="
