# Kaiteyo (書いてよ) — Assets

## Logo & Branding Assets

### Primary Logo
- **File**: `preview_assets/kaiteyo_logo.svg`
- **Type**: Wordmark with Japanese characters
- **Colors**: Lime (#C2FC8B) and Orange (#FEAB57)
- **Usage**: Application icon, about page, README

### Icon
- **File**: `preview_assets/kaiteyo_icon_simple.svg`
- **Type**: Simplified icon for small displays
- **Usage**: Window icon, taskbar, dock

### Banner
- **File**: `preview_assets/kaiteyo_banner.svg`
- **Type**: Full-color banner with logo and tagline
- **Usage**: GitHub repository, social media

### Wordmark
- **File**: `preview_assets/kaiteyo_wordmark.svg`
- **Type**: Text-only logo
- **Usage**: Splash screen, loading states

### Inkscape Source
- **File**: `preview_assets/inkscape_icon.svg`
- **Type**: Editable source file
- **Usage**: Modifying and exporting variations

## Application Icons

### Desktop
| Platform | File | Format |
|----------|------|--------|
| Windows | `desktopApp/windows_icon.ico` | ICO |
| macOS | `desktopApp/mac_icon.icns` | ICNS |
| Linux | `desktopApp/src/jvmMain/composeResources/drawable/windowIcon.png` | PNG |

### Android
- Location: `app/src/main/res/`
- Formats: Adaptive icon (foreground + background)
- Sizes: mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi

### iOS
- Location: `iosApp/KaiteyoApp/Assets.xcassets/`
- Format: AppIcon.appiconset

## Color Palette

### Brand Colors
| Name | Hex | Usage |
|------|-----|-------|
| Kaiteyo Lime | `#C2FC8B` | Primary brand color |
| Kaiteyo Orange | `#FEAB57` | Secondary brand color |
| Dark Background | `#1A1A1A` | Dark mode background |
| Light Background | `#FAFAFA` | Light mode background |

### Extended Palette
| Name | Hex | Usage |
|------|-----|-------|
| Error Red | `#FF6B6B` | Error states, close button |
| Success Green | `#4CAF50` | Success states |
| Warning Orange | `#FFB74D` | Warning states |
| Info Blue | `#64B5F6` | Information states |

## Typography

Kaiteyo uses system fonts exclusively:
- **macOS**: SF Pro, SF Mono
- **Windows**: Segoe UI, Cascadia Code
- **Linux**: Roboto, JetBrains Mono

No custom font files are bundled with the application.

## Resource Files

### Compose Resources
- Location: `core/src/commonMain/composeResources/`
- Managed by: Compose Resources plugin
- Types: Drawables, strings, fonts

### Desktop Resources
- Location: `desktopApp/src/jvmMain/composeResources/`
- Window icon: `drawable/windowIcon.png`

### Android Resources
- Location: `app/src/main/res/`
- Standard Android resource directories

## Screenshots & Previews

### GitHub/Store Screenshots
- Location: `preview_assets/`
- Format: PNG
- Resolution: 1920x1080 (desktop), 1080x1920 (mobile)

### Feature Previews
- Location: `preview_assets/`
- Used in: README, documentation, store listings

## Asset Generation

The `mediaGenerator` module handles automated asset generation:
- Location: `mediaGenerator/`
- Purpose: Generate icons, screenshots, and promotional materials
- Run: `./gradlew :mediaGenerator:run`
