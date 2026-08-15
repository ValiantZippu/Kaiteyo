# Kaiteyo — Theme System & Theme Studio

> This spec describes the theme feature **as implemented** (v2.2.1). The desktop
> Theme Studio lives in `desktopApp/.../desktop/ui/themes/ThemeStudioView.kt` and is
> backed by `desktop/.../engine/theming/ThemeManager.kt`; the shared engine theme is
> `core/.../presentation/common/theme/` (see `docs/design/THEME_SYSTEM.md` for the
> full token/architecture detail).

## Purpose

Users can customize every visual aspect of Kaiteyo: base appearance, accent colors,
typography, scaling, animation, effects and accessibility. Everything is live —
edits apply to the whole app (including the window chrome) immediately and persist
across restarts.

## User experience

### Base appearance (shared, all platforms)

- **Base modes** — OLED Black (default), Dark Gray, Light, Sepia (reading mode).
- **Accent schemes** — 7 presets: Signature Pineapple (default), Cotton Candy,
  Ocean, Forest, Sunset, Lavender, Monochrome.
- Available on mobile through **Settings → Appearance** and the **Appearance Studio**
  (`core/.../screen/main/screen/settings/AppearanceStudio.kt`), and on desktop through
  the Theme Studio.

### Desktop Theme Studio

The Theme Studio is a workspace view (`WorkspaceView.ThemeStudio` → `ThemeStudioView`)
with a theme library on the left (or a strip below 760dp) and a 7-tab editor:

| Tab | What it edits | Backing |
|-----|---------------|---------|
| **Colors** | primary/secondary/tertiary, on-colors, background/surface/text/border | `updateActiveColors` |
| **Typography** | base font size | `updateActiveTypography` |
| **Scaling** | display scale (UI zoom) | `updateActiveScaling` |
| **Animation** | speed multiplier, reduced motion, duration | `updateActiveAnimation` |
| **Effects** | glow intensity/radius/opacity, blur, transparency, glass opacity | `updateActiveEffects` |
| **Accessibility** | high-contrast and related a11y toggles | `ThemeAccessibilityEditor` |
| **Preview** | full live preview of the current theme | `ThemePreviewTab` |

Additional features:

- **Theme library** — presets + custom + imported themes; duplicate, rename, set
  description/author, favorite, delete, reset to preset values, reset all.
- **Live editing** — editing a preset auto-promotes it to a custom copy so the
  pristine preset stays intact (`ThemeManager.updateActive`).
- **JSON** — toggle a JSON card showing the active theme; **export** the current
  theme as JSON and **import** JSON themes (`ThemeManager.exportJson`/`importJson`;
  imported preset ids get an `imported-` prefix).
- **Instant application** — every mutation bumps `ThemeManager.revision` and the
  suite re-derives `AppTheme` through `ThemeMapper`, so edits reach all `Ds*`
  components and the window shell live (with the 450ms color crossfade).
- **Onboarding** — first-run wizard steps choose theme, accent, scaling, font size,
  navigation and motion (desktop only, re-openable from Settings).

## Technical design

### Architecture

```
ThemeManager (desktop, ~/.kaiteyo/themes/)
  ├── presets (ThemePresets.all)  +  customThemes (editable/imported)
  ├── activeThemeId / revision (mutableState)
  └── updateActive*(transform) → persist <id>.json + active.json
        │
        ▼
ThemeMapper (desktop/engine/theming)
  └── KaiteyoTheme → baseMode, accentScheme, surfaceColors, layoutConfig,
                     radiusConfig, animationConfig, typeScale, typography
        │
        ▼
AppTheme / KaiteyoThemeRoot (core/.../theme/Theme.kt)
  └── Material ColorScheme + Shapes + TypeScale + CompositionLocals
        │
        ▼
All shared UI + desktop Ds* components
```

Mobile uses the same `AppTheme`/`KaiteyoThemeState` but persistence flows through
`ThemeSettingsState` (DataStore) instead of `ThemeManager`.

### Key files

- `desktopApp/.../desktop/ui/themes/ThemeStudioView.kt` — the Studio UI
  (`ThemeLibraryRail`, `ThemeEditor`, tab editors, `ThemeJsonCard`)
- `desktopApp/.../desktop/engine/theming/ThemeManager.kt` — library + persistence +
  live edits (promote-to-custom, gradient stop syncing)
- `desktopApp/.../desktop/engine/theming/ThemePresets.kt` — built-in `KaiteyoTheme`
  presets
- `desktopApp/.../desktop/engine/theming/ThemeStudio.kt` — `KaiteyoTheme` model +
  `ThemeSerializer` (JSON validate/export/import)
- `desktopApp/.../desktop/engine/theming/ThemeMapper.kt` — theme → shared AppTheme
  inputs
- `core/.../presentation/common/theme/Color.kt`, `Theme.kt`, `Typography.kt`,
  `Dimens.kt` — shared engine theme
- `core/.../presentation/common/theme/ThemeSettingsState.kt` — persisted shared
  settings (accent, radius, density, motion, typography)
- `core/.../screen/main/screen/settings/AppearanceStudio.kt` — mobile/desktop
  Appearance Studio (color wheel, HSV/RGB/HSL/HEX, gradients, presets)

### Persistence

| Surface | Location |
|---------|----------|
| Desktop Theme Studio | `~/.kaiteyo/themes/` — `active.json` + `custom/<id>.json` |
| Shared engine settings (mobile + desktop) | DataStore via `ThemeSettingsState` |

### Theme JSON format

See `docs/design/THEME_SYSTEM.md` → "Theme JSON format" for the full schema
(`colors`, `gradient`, `typography`, `scaling`, `animation`, `spacing`, `corners`,
`effects`, metadata).

## Dependencies

- Compose Multiplatform (Material3 `ColorScheme`/`Shapes`)
- Koin (`ThemeManager` injection on desktop; `ThemeSettingsState` shared)
- DataStore (shared settings persistence)
- kotlinx.serialization (theme JSON, `ThemeSerializer`)

## Future improvements

- Community theme sharing / marketplace
- AI-generated themes
- Time-based theme switching (auto dark/light by local time)
- Per-window theming (multiple windows, different themes)

## Open questions

- Should themes sync across devices (e.g. via the GitHub gist sync)?
- Should a theme rating/review system exist?
- Should animated gradient backgrounds be supported?
