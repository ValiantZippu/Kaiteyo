# Kaiteyo (書いてよ) — Branding

## Brand Identity

Kaiteyo is a premium, minimalist Japanese language learning application. The brand evokes modern Japanese design aesthetics — clean, precise, purposeful.

## Logo

### Primary Logo
- **Type**: Wordmark with Japanese characters
- **Colors**: Lime (#C2FC8B) and Orange (#FEAB57)
- **Usage**: Application icon, about page, README, splash screen
- **File**: `preview_assets/kaiteyo_logo.svg`

### Icon
- **Type**: Simplified icon for small displays
- **Usage**: Window icon, taskbar, dock, notification area
- **File**: `preview_assets/kaiteyo_icon_simple.svg`

### Wordmark
- **Type**: Text-only logo
- **Usage**: Splash screen, loading states, compact displays
- **File**: `preview_assets/kaiteyo_wordmark.svg`

## Color Palette

### Brand Colors
| Name | Hex | Usage |
|------|-----|-------|
| Kaiteyo Lime | `#C2FC8B` | Primary brand color, primary actions, selected states |
| Kaiteyo Orange | `#FEAB57` | Secondary brand color, highlights, hover states |
| Dark Background | `#1A1A1A` | Dark mode main background |
| Light Background | `#FAFAFA` | Light mode main background |

### Extended Palette
| Name | Hex | Usage |
|------|-----|-------|
| Error Red | `#FF6B6B` | Error states, close button hover |
| Success Green | `#4CAF50` | Success states, completion indicators |
| Warning Orange | `#FFB74D` | Warning states |
| Info Blue | `#64B5F6` | Information states |

## Typography

Kaiteyo uses system fonts exclusively:
- **macOS**: SF Pro, SF Mono
- **Windows**: Segoe UI, Cascadia Code
- **Linux**: Roboto, JetBrains Mono

No custom font files are bundled.

## Voice & Tone

| Context | Tone |
|---------|------|
| General UI | Neutral, professional |
| Error messages | Informative, not alarming |
| Success states | Brief acknowledgment |
| Onboarding | Warm, clear, minimal |
| Learning feedback | Constructive, precise |

## Rebranding Checklist

Replace every visible user-facing occurrence of "Kanji Dojo" with "Kaiteyo":

- [x] Application name in desktop title bar — `resolveString { appName }` → "Kaiteyo" (JP: "Kaiteyo (書いてよ)")
- [x] Installer name (MSI, DMG, Deb) — `packageName = "Kaiteyo"` in `desktopApp/build.gradle.kts`
- [x] Window title — custom title bar renders "Kaiteyo"
- [x] About page / credits — credits preserve upstream attribution, present as Kaiteyo
- [x] README.md — fully Kaiteyo with original-author attribution block
- [x] GitHub repository description — repo is `ValiantZippu/Kaiteyo` (set on GitHub)
- [x] Splash screen — Android splash + iOS launch background (OLED `#0A0A0A`)
- [x] Taskbar/dock icon tooltip — Kaiteyo icons on all platforms
- [x] Start menu / Applications menu entry — all `.desktop` files `Name=Kaiteyo`
- [x] Package description (if published) — flatpak metainfo, snapcraft, AppImage all Kaiteyo

> Verified complete by a full repo-wide sweep (code, resources, packaging, workflows,
> website source, docs). The only remaining "Kanji Dojo" references are legal
> attribution (fork history, original-author copyright, upstream repository) and
> functional references (`kanji-dojo-data-base-v15.sql` asset, App Store listing URL).

**Do NOT change:**
- Internal package namespaces (`ua.syt0r.kanji`)
- Database table names
- File names that reference "kanji" (unless they are user-facing)
- The `kanji-dojo-data-base-v15.sql` data asset filename and its download URLs
  (hosted on the upstream `syt0r/Kanji-Dojo-Data` release — renaming breaks the build)
- Attribution to the original Kanji Dojo project in credits and licenses

## Logo Usage Rules

1. Always maintain aspect ratio
2. Minimum clear space: 16dp on all sides
3. Do not stretch, distort, or rotate
4. Do not apply effects (drop shadows, gradients) beyond the original design
5. On dark backgrounds: use full-color version
6. On light backgrounds: use full-color version with adjusted contrast
7. Never replace the logo with text
