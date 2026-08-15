# Kaiteyo (書いてよ) — Theme System

> This document describes the theme system **as it is implemented** — not the
> aspirational version. Source of truth: `core/.../presentation/common/theme/`
> (`Theme.kt`, `Color.kt`, `Typography.kt`, `Dimens.kt`) for the shared engine, and
> `desktopApp/.../desktop/engine/theming/` (`ThemeManager.kt`, `ThemePresets.kt`,
> `ThemeStudio.kt`) + `desktopApp/.../desktop/engine/theming/ThemeMapper.kt` for the
> desktop Theme Studio.

## Two theme layers

Kaiteyo has **two cooperating theme systems**:

1. **Shared engine theme** (`core/.../theme/`) — `BaseMode` + `KaiteyoAccentScheme`
   + `KaiteyoThemeState`. Used by **all platforms** (Android/iOS/desktop). Persisted
   via `ThemeSettingsState` (DataStore) on mobile; the desktop app maps its Theme
   Studio themes onto it through `ThemeMapper`.
2. **Desktop Theme Studio** (`desktopApp/.../engine/theming/`) — `ThemeManager`
   owns a full theme library (presets + custom + imported) as `KaiteyoTheme` objects
   persisted as JSON in `~/.kaiteyo/themes/`. The suite renders through
   `KaiteyoDesktopSuite` → `AppTheme(...)` fed by `ThemeMapper`.

On desktop the **Theme Studio is the source of truth**; `ThemeMapper` translates a
`KaiteyoTheme` into `baseMode`, `accentScheme`, `customSurface`, `layoutConfig`,
`radiusConfig`, `animationConfig`, `typeScale` and `typography` for the shared
`AppTheme`.

## Shared color model (`Color.kt`)

### Base modes

`enum class BaseMode(val displayName: String)`: `Oled` ("OLED Black", default),
`Dark` ("Dark Gray"), `Light`, `Sepia` (reading mode).

`surfaceForBaseMode(mode)` returns a `SurfaceColors`:

| Field | Role |
|-------|------|
| `background` | Window/screen background |
| `surface` | Cards, panels |
| `surfaceElevated` | Elevated surfaces, inputs |
| `surfaceInteractive` | Hover/press fills |
| `border` | Hairlines and outlines |
| `textPrimary` / `textSecondary` / `textMuted` / `textInverse` | Text ramp |

Oled values: background #050505, surface #0D0D0D, elevated #101010, interactive
#1A1A1A, border #2A2A2A, text #F0F0F0/#A0A0A0/#606060. Sepia swaps in warm paper
tones (#F5F0E8 background, #3D3028 text).

### Accent schemes

`KaiteyoAccentScheme(name, primary, primaryDark, secondary, secondaryDark, onPrimary,
onSecondary, tertiary?, previewColors, gradientStart?, gradientEnd?)`.

`AllAccentSchemes` (in order): **Signature Pineapple** (default; lime #C2FC8B +
orange #FEAB57), **Cotton Candy**, **Ocean**, **Forest**, **Sunset**, **Lavender**,
**Monochrome**. Each is a data object with dark variants for light-mode contrast
(`primaryDark`/`secondaryDark` are used by the light color scheme).

### Semantic colors

`semanticSuccess` (#C2FC8B), `semanticWarning` (#FEAB57), `semanticError` (#FF6B6B),
`semanticInfo` (#7BC8FF), `semanticNew` (#A78BFA), `favoriteYellow` (#FFD93D),
`dueOrange` (#FF9F43). Theme-aware accessors on `MaterialTheme`
(`successColor`, `warningColor`, `infoColor`, `newColor`, `dangerColor`,
`favoriteColor`) and desktop `DsSemantic`.

### Gradients & glow

`KaiteyoGradient(start, end, angle=45°, intensity)` derived per accent; `KaiteyoGlow`
(color, radius, opacity, intensity) with `primaryGlow()`/`secondaryGlow()` helpers.

## Theme state (`Theme.kt`)

`KaiteyoThemeState` holds the live editable state, each field a `mutableStateOf`:

```kotlin
class KaiteyoThemeState(
    var baseMode: BaseMode,
    var accentScheme: KaiteyoAccentScheme,
    var animationConfig: AnimationConfig,
    var radiusConfig: RadiusConfig,
    var glowConfig: GlowConfig,
    var layoutConfig: LayoutConfig,
    var typeScale: TypeScale
)
```

### Config data classes (all serializable)

- **`AnimationConfig`** — `speed` (`AnimationSpeed`: Slow 1.5× / Normal 1.0× /
  Fast 0.6× / Instant 0.0×), `reducedMotion`, `springDamping` (0.6), `springStiffness`
  (300), `defaultDuration` (300ms), `pageTransition`
  (`PageTransitionType`: Crossfade / Slide / FadeThrough / Scale), `themeTransitionEnabled`.
- **`RadiusConfig`** — `style` (`CornerRadiusStyle`: Square 0.5× / Rounded 1.0× /
  Very Rounded 1.5× / Soft 2.0×), `customRadius`, `buttonRadius`.
- **`GlowConfig`** — `intensity`, `radius`, `opacity` multipliers.
- **`LayoutConfig`** — `density` (`UIDensity`: Compact 0.7× / Comfortable 1.0× /
  Spacious 1.3×), `sidebarMode`, `sidebarPosition`, `autoHide`, `collapsed`,
  `panelWidth` (260dp), `panelHeight`, `floatingOffset`, `accentIndex`,
  `transparencyEnabled`, `blurEnabled`, `glassOpacity` (0.8), plus scale factors:
  `displayScale`, `buttonScale`, `iconScale`, `bubbleScale`, `toolbarHeightScale`,
  `windowPaddingScale`.
- **`TypeScale`** — `fontScale`, `titleScale`, `lineHeight`, `letterSpacing`.

### Composition locals

| Local | Provides |
|-------|----------|
| `LocalKaiteyoThemeState` | Mutable `KaiteyoThemeState` |
| `LocalKaiteyoAccent` | Current accent scheme |
| `LocalBaseMode` | Current base mode |
| `LocalSurfaceColors` | Current `SurfaceColors` |
| `LocalAnimationConfig` / `LocalRadiusConfig` / `LocalGlowConfig` / `LocalLayoutConfig` / `LocalTypeScale` | The five configs |
| `LocalExtraColors` | Light/dark semantic scheme |
| `LocalThemeSettingsState` | Persisted settings state (shared engine) |

Convenience extensions: `MaterialTheme.kaiteyoAccent`, `.baseMode`, `.surfaceColors`,
`.kaiteyoThemeState`, `.animationConfig`, `.glowConfig`, `.radiusConfig`,
`.layoutConfig`, plus `.extraColorScheme`.

## AppTheme (`Theme.kt`)

`AppTheme(...)` is the single entry point:

1. Resolves `SurfaceColors` from `baseMode` (or `customSurface`).
2. Builds the Material `ColorScheme` from the accent + surface (dark or light variant;
   `primaryContainer` etc. are accent at 15–20% alpha).
3. Builds `Shapes` from `Dimens` radii × radius multiplier — **every** Material
   component inherits the user's radius config.
4. **Theme transition**: when `themeTransitionEnabled` and duration > 0, every color
   in the scheme, extra scheme, surface colors and accent is wrapped in
   `animateColorAsState` (450ms base × speed) so a theme switch crossfades the whole
   UI in place — state preserved because the tree never leaves composition.
5. Applies live `TypeScale` (`Typography.scaledBy`), wraps MaterialTheme, and provides
   all locals.

`KaiteyoThemeRoot` (in `KaiteyoApp.kt`) wires the persisted settings into
`KaiteyoThemeState` on launch and persists every mutation back
(`ThemeSettings.from(themeState)` → `ThemeSettingsState.accept`), so Theme Studio and
Settings stay in sync and survive restarts.

## Desktop Theme Manager (`ThemeManager.kt`)

- Owns `presets` (from `ThemePresets.all`) + `customThemes` (mutable list) + the
  active theme id. `revision` bumps on every mutation so derived UI recomposes.
- **Live editing** — `updateActive(transform)`: if the active theme is a preset it is
  first **promoted** to a custom copy ("{name} (Custom)") so pristine presets stay
  intact; every mutation persists immediately to `~/.kaiteyo/themes/custom/<id>.json`
  and `active.json`.
- Typed updaters: `updateActiveColors`, `updateActiveGradient`,
  `updateActiveTypography`, `updateActiveScaling`, `updateActiveAnimation`,
  `updateActiveSpacing`, `updateActiveCorners`, `updateActiveEffects`,
  `updateActiveMeta`. `updateActiveColors` keeps the brand gradient stops in step with
  Primary/Secondary edits unless the user recolored them explicitly.
- **Library CRUD** — `duplicate`, `rename`, `setDescription`, `setAuthor`,
  `toggleFavorite`, `deleteTheme` (active falls back to default preset), `resetTheme`,
  `resetAll`.
- **Import/export** — `exportJson(id)`, `importJson(text)`, `exportAllJson()` via
  `ThemeSerializer` (validates on import; imported preset ids are prefixed
  `imported-`).

### Theme JSON format

A `KaiteyoTheme` serializes to JSON with `id`, `name`, `source` (preset/custom/
imported), `author`, `description`, `favorite`, `createdAt`, `updatedAt`, and the
groups:

```json
{
  "colors": { "primary": "#C2FC8B", "secondary": "#FEAB57", "background": "#050505", "surface": "#0D0D0D", "textPrimary": "#F0F0F0", "border": "#2A2A2A" },
  "gradient": { "enabled": true, "stops": [{ "color": "#C2FC8B", "position": 0.0 }, { "color": "#FEAB57", "position": 1.0 }], "angle": 45 },
  "typography": { "fontSize": 1.0 },
  "scaling": { "displayScale": 1.0, "buttonScale": 1.0, "iconScale": 1.0 },
  "animation": { "speed": 1.0, "reducedMotion": false, "durationMs": 300, "springDamping": 0.6, "springStiffness": 300 },
  "spacing": { "multiplier": 1.0 },
  "corners": { "multiplier": 1.0 },
  "effects": { "glowIntensity": 1.0, "glowRadius": 1.0, "opacity": 1.0, "blurEnabled": false, "transparencyEnabled": false, "glassOpacity": 0.8 }
}
```

## ThemeMapper (desktop)

`ThemeMapper` converts a `KaiteyoTheme` into shared `AppTheme` inputs:
`baseMode(theme)`, `accentScheme(theme)`, `surfaceColors(theme)`,
`layoutConfig(theme)`, `radiusConfig(theme)`, `animationConfig(theme)`,
`typeScale(theme)`, `typography(theme)`. It's what makes Theme Studio edits reach
every `Ds*` component and the window chrome live (through `KaiteyoDesktopSuite`'s
`shell` slot).

## Persistence summary

| Surface | Where |
|---------|-------|
| Shared engine theme settings | DataStore (`ThemeSettingsState`), mobile + desktop |
| Desktop Theme Studio library | `~/.kaiteyo/themes/` — `active.json` + `custom/<id>.json` |
| Window geometry | `~/.kaiteyo/window.json` (`WindowStateStore`) |
| Settings (general) | Desktop `settings.json` via `SettingsEngine`; mobile DataStore |

## Built-in themes (what the user actually sees)

- **Base modes** (4): OLED Black, Dark Gray, Light, Sepia.
- **Accent schemes** (7): Signature Pineapple, Cotton Candy, Ocean, Forest, Sunset,
  Lavender, Monochrome.
- **Desktop presets** (`ThemePresets.all`) — full `KaiteyoTheme` presets that bundle
  a base mode + colors + motion + layout (the default "Signature" theme is the app's
  first-run appearance).

Custom themes are created by editing a preset (which auto-promotes it to a custom
theme), duplicating, or importing JSON.

## Related

- `docs/design/DESIGN_LANGUAGE.md` — tokens and palette values
- `docs/design/UI_SYSTEM.md` — components consuming the tokens
- `docs/features/THEMES.md` — Theme Studio user experience
- `docs/architecture/performance.md` — theme transition performance notes
