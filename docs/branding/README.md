# 🎯 branding — Kaiteyo Brand Assets

This directory contains the brand guidelines and asset inventory. The actual asset files
live in the locations listed below (SVG sources are committed; generated icons are
produced by `installer/scripts/generate-assets.sh`).

## Asset inventory

| Asset | Location | Usage |
|-------|----------|-------|
| Logo mark | `preview_assets/kaiteyo_logo.svg` | README header, website, about page |
| Wordmark | `preview_assets/kaiteyo_wordmark.svg` | Wordmark usage |
| GitHub banner | `preview_assets/kaiteyo_banner.svg` | Repository social preview |
| Icon (simple) | `preview_assets/kaiteyo_icon_simple.svg` | Simplified icon |
| Editable source | `preview_assets/inkscape_icon.svg` | Inkscape source of the icon |
| Brand mark (installer) | `installer/assets/brand/kaiteyo-mark.svg` | Installer artwork source |
| Desktop window icon | `desktopApp/src/jvmMain/composeResources/drawable/windowIcon.png` | App window icon (Linux) |
| Windows icon | `desktopApp/windows_icon.ico` | Windows packaging |
| macOS icon | `desktopApp/mac_icon.icns` | macOS packaging |
| Android launcher | `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | Android launcher icon |
| iOS app icon | `iosApp/KaiteyoApp/Assets.xcassets/AppIcon.appiconset/` | iOS app icon |
| Phone screenshots | `fastlane/metadata/android/en-US/images/phoneScreenshots/` | Store listings |
| Generated icons (bmp/ico/icns/png) | `installer/assets/` (generated) | Installer surfaces |

## Color palette

| Name | Hex | Usage |
|------|-----|-------|
| Kaiteyo Lime | `#C2FC8B` | Primary brand color |
| Kaiteyo Orange | `#FEAB57` | Secondary brand color |
| Dark Background | `#1A1A1A` | Dark mode background |
| Light Background | `#FAFAFA` | Light mode background |

## Logo rules

1. Always maintain aspect ratio
2. Minimum clear space: 16dp on all sides
3. Do not stretch, distort, or rotate
4. Do not apply effects (drop shadows, gradients) beyond original design

## Guidelines

- [`BRAND_GUIDELINES.md`](BRAND_GUIDELINES.md) — full brand guidelines (colors, mark
  usage, typography)
- [`BRANDING.md`](BRANDING.md) — rebranding history and the Kaiteyo sweep checklist
- Brand artwork must follow these guidelines on every surface, including the installer
  (see `installer/README.md`).
