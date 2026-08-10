# Installer Brand Assets

All artwork in this directory is **vector source (SVG)**. Binary outputs
(`.bmp`, `.png`, `.icns`, `.ico`) are generated — never hand-edited — by
`../scripts/generate-assets.sh`.

## What lives where

```
assets/
├── brand/
│   └── kaiteyo-mark.svg          # The "K" mark on a rounded square (primary logo mark)
├── windows/
│   ├── banner.svg                # 164×314 wizard banner (Inno Setup WizardImageFile)
│   ├── welcome.svg               # 150×57 wizard small image (WizardSmallImageFile)
│   └── uninstaller.svg           # Uninstaller icon source (drop-shadow "K")
└── macos/
    └── dmg-background.svg        # 660×400 DMG background artwork
```

## Brand tokens (from docs/branding/BRAND_GUIDELINES.md)

| Token | Value |
|-------|-------|
| Primary (light) | `#e1e4e6` |
| Primary (dark) | `#201a1a` |
| Accent (default) | indigo family — the app's default accent; see `core/.../theme/Color.kt` |
| Type | System UI; the mark itself is the "K" glyph in a rounded square |

The installer reuses the *same* accent the user picks in-app. Inno Setup is
compiled with the default accent baked in (mark background); the onboarding
wizard uses the live theme.

## Regenerating

```bash
bash installer/scripts/generate-assets.sh
```

Requires `rsvg-convert` (librsvg) + ImageMagick `convert` + `png2icns`/`icotool`
(or `ImageMagick` `convert` fallbacks). See the script for exact requirements.

Generated files land in `assets/generated/`:

| File | Used by |
|------|---------|
| `windows/banner.bmp` | `kaiteyo.iss` → `WizardImageFile` |
| `windows/banner-dark.bmp` | `WizardImageFileDynamicDark` (dark mode) |
| `windows/welcome.bmp` | `WizardSmallImageFile` |
| `windows/installer-icon.ico` | `SetupIconFile` + uninstaller icon |
| `macos/dmg-background.png` | `macos/build-dmg.sh` |
| `macos/dmg-background@2x.png` | Retina DMG background |
| `linux/appicon-{16,32,48,64,128,256,512}.png` | AppImage/deb/rpm icon theme |
| `linux/appicon.svg` | AppStream icon + Flatpak |
