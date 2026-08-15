# Kaiteyo (書いてよ) — Design Language

> This document is grounded in the **actual tokens and code**. Every value below maps
> to a real constant: `core/.../presentation/common/theme/Dimens.kt` (shared tokens),
> `desktopApp/.../desktop/designsystem/DsTokens.kt` (desktop tokens) and
> `core/.../presentation/common/theme/Color.kt` (palette). When the docs and the code
> disagree, the code wins.

## Design principles

1. **Content-first** — UI chrome recedes; content dominates (see `DsTopBar` in
   `WorkspaceShell.kt`: a slim title + subtitle, not a heavy header).
2. **Floating elements** — Panels feel elevated, not attached. The desktop dock is
   wrapped in `DsDockIsland` — an 8dp ring of window background around the rail so it
   reads as an island, not chrome glued to the edge.
3. **Generous whitespace** — 4dp base grid, 16dp minimum edge padding
   (`Dimens.ContentPaddingSmall`), 20dp standard (`Dimens.ContentPadding`).
4. **Consistent radius** — every rounded corner comes from the shared radius tokens
   (below) and follows the user's radius configuration.
5. **Spring animations** — natural feel at 60 FPS; durations honor the user's
   animation speed and reduced-motion preference (see `ANIMATION_SYSTEM.md`).

## Token architecture

Two token layers, both derived from the same runtime configuration:

- **Shared tokens** (`core/.../theme/Dimens.kt`) — the base 4dp grid, radius base
  values, icon/panel sizes. Read by both mobile and desktop UI.
- **Desktop design-system tokens** (`desktopApp/.../designsystem/DsTokens.kt`) —
  `DsSpacing`, `DsRadius`, `DsMotion`, `DsType`, `DsElevation`, `DsSemantic`. These
  are `@Composable` getters that **scale with the active theme config** (density
  multiplier, display zoom, radius multiplier, animation speed, type scale), so the
  whole suite reacts to Theme Studio / Appearance Studio edits instantly.

### Density multiplier

`DsSpacing.densityMultiplier()` maps `UIDensity` (from `Theme.kt`) to a spacing factor:

| Density | Multiplier |
|---------|------------|
| Compact | 0.7× |
| Comfortable (default) | 1.0× |
| Spacious | 1.3× |

Every `DsSpacing` value is `base × densityMultiplier × displayScale`
(`LayoutConfig.displayScale`, 0.8–1.6). The shared `Dimens` values are the unscaled
base.

## Spacing System

Base values (`Dimens.kt`), all multiples of 4dp:

| Token | Value | Usage |
|-------|-------|-------|
| `Dimens.Space1` | 4dp | Tight icon padding, inline gaps |
| `Dimens.Space2` | 8dp | Between related elements |
| `Dimens.Space3` | 12dp | Between grouped elements |
| `Dimens.Space4` | 16dp | Standard padding for cards, panels |
| `Dimens.Space5` | 20dp | Content padding (`Dimens.ContentPadding`) |
| `Dimens.Space6` | 24dp | Section spacing, window padding |
| `Dimens.Space8` | 32dp | Major section breaks |
| `Dimens.Space10` | 40dp | `DsSpacing.Section` base |
| `Dimens.Space12` | 48dp | Page-level margins |
| `Dimens.Space16` | 64dp | Large separations |
| `Dimens.Space20` | 80dp | Generous hero spacing |

Desktop token equivalents (`DsSpacing`): `Xs=4`, `Sm=8`, `Md=12`, `Lg=16`, `Xl=24`,
`Xxl=32`, `Section=40` — all scaled by density × zoom.

**Visual rhythm rules:** 8dp vertical rhythm between unrelated elements, 4dp between
related ones, 16dp minimum edge padding, 24dp between major sections.

## Corner Radius System

Base values (`Dimens.kt`), scaled at runtime by `RadiusConfig` (style multiplier ×
custom radius) — see `Theme.kt` `AppTheme`, where Material's `Shapes` are built from
these tokens so every Material component inherits the same radius system:

| Token | Value | Usage |
|-------|-------|-------|
| `Dimens.RadiusXs` | 4dp | Checkboxes, small indicators |
| `Dimens.RadiusSm` | 8dp | Buttons, inputs, small cards |
| `Dimens.RadiusMd` | 12dp | Standard cards, list items |
| `Dimens.RadiusLg` | 16dp | Large cards, modals, context panels |
| `Dimens.RadiusXl` | 24dp | Sidebar panel, main content panel |
| `Dimens.Radius2xl` | 32dp | Large containers, dialogs |

Desktop tokens (`DsRadius`): `Xs=4`, `Sm=8`, `Md=12`, `Lg=16`, `Xl=24`, `Full=999`
(pills). Note `DsDialog` uses `DsRadius.Xl` (24dp) for its panel and `DsRadius.Md` for
controls.

### Radius styles (`CornerRadiusStyle`)

| Style | Global multiplier |
|-------|-------------------|
| Square | 0.5× |
| Rounded (default) | 1.0× |
| Very Rounded | 1.5× |
| Soft | 2.0× |

A `customRadius` override multiplies on top. This is what the Appearance Studio
"corner radius" and Theme Studio "Corners" tabs edit.

## Typography Scale

`core/.../theme/Typography.kt` defines `AppTypography` — the Material 3 type scale
with the **Japanese locale list** baked into every style (`LocaleList("ja")`) so
Japanese text shaping is correct everywhere. Font family is the platform default
(`FontFamily.Default`), not a bundled font; platform bundles (e.g. Android `res/font`)
are resolved by the system.

| Material style | Size | Weight | Typical usage |
|----------------|------|--------|---------------|
| `displayLarge` | 57sp | 400 | Large display numbers |
| `displayMedium` | 45sp | 400 | Hero metrics |
| `displaySmall` | 36sp | 400 | Page-level numbers |
| `headlineLarge` | 32sp | **700** | Page titles |
| `headlineMedium` | 28sp | 400 | Section headers |
| `headlineSmall` | 24sp | 400 | Panel titles |
| `titleLarge` | 22sp | 400 | Subsection headers |
| `titleMedium` | 16sp | 500 | Card titles, dialog headers |
| `titleSmall` | 14sp | 500 | Subsection labels |
| `bodyLarge` | 16sp | 400 | Primary content |
| `bodyMedium` | 14sp | 400 | Secondary content |
| `bodySmall` | 12sp | 400 | Captions, metadata |
| `labelLarge` | 14sp | 500 | Buttons, nav items |
| `labelMedium` | 12sp | 500 | Badges, small annotations |
| `labelSmall` | 11sp | **300** | Micro-labels |

The desktop design system exposes a trimmed token set (`DsType`): `Caption=11`,
`Label=12`, `Body=14`, `BodyLarge=16`, `Title=18`, `Heading=22`, `Display=28` —
scaled by `fontScale × displayScale × titleScale`.

**Live scaling:** `TypeScale` (`Theme.kt`) multiplies sizes at runtime — titles use
`titleScale`, body uses `fontScale`, line height uses `lineHeight`, plus an additive
`letterSpacing`. This powers the font-size and scaling settings (and onboarding steps
4–5). Japanese text renders with 1.6 line height (`lineHeight` in `StepFontSize`).

## Color System

`core/.../theme/Color.kt` is the single source of truth. Structure:

- **Base modes** (`BaseMode`): `Oled` (default), `Dark`, `Light`, `Sepia` — each maps
  to a `SurfaceColors` (background, surface, surfaceElevated, surfaceInteractive,
  border, textPrimary/Secondary/Muted/Inverse) via `surfaceForBaseMode()`.
- **Accent schemes** (`KaiteyoAccentScheme`): 7 presets in `AllAccentSchemes` —
  Signature Pineapple (default), Cotton Candy, Ocean, Forest, Sunset, Lavender,
  Monochrome. Each has primary/primaryDark/secondary/secondaryDark/onPrimary/
  onSecondary/tertiary/previewColors/gradientStart/gradientEnd.
- **Semantic colors**: `semanticSuccess` (#C2FC8B), `semanticWarning` (#FEAB57),
  `semanticError` (#FF6B6B), `semanticInfo` (#7BC8FF), `semanticNew` (#A78BFA),
  `favoriteYellow` (#FFD93D), `dueOrange` (#FF9F43). Exposed theme-aware via
  `MaterialTheme.successColor / warningColor / infoColor / newColor / dangerColor /
  favoriteColor` and on desktop via `DsSemantic`.
- **Gradients** (`KaiteyoGradient`): start/end colors derived per accent.
- **Glow** (`KaiteyoGlow`): color + radius + opacity; `primaryGlow()` /
  `secondaryGlow()` build from the accent.

Default surface values (Oled mode):

| Role | Value |
|------|-------|
| Background | #050505 |
| Surface | #0D0D0D |
| Surface elevated | #101010 |
| Surface interactive | #1A1A1A |
| Border | #2A2A2A |
| Text primary | #F0F0F0 |
| Text secondary | #A0A0A0 |
| Text muted | #606060 |

**Color usage guidance:** ~85% dark surfaces, ~10% text, ~5% accent. The signature
accent gradient is lime (#C2FC8B) → orange (#FEAB57).

## Elevation & Shadows

Desktop tokens (`DsElevation`): `Flat=0`, `Raised=2`, `Floating=8`, `Overlay=16`.
`DsCard` lifts from `Flat` to `Raised` on hover for interactive cards; the nav sidebar
uses `NavTokens.SidebarElevation = 10dp`; dialogs cast `Overlay`-scale shadows.

Shared Material elevation (`Elevation.kt` / Material defaults) follows the standard
level-0..level-5 scale for modal surfaces.

## Glow System

Glows come from the accent (`KaiteyoGlow` / `GlowConfig`):
- **Hover glow** — accent at 15–20% opacity behind the element (e.g. buttons use
  `accent.primary.copy(alpha = 0.1f)` backgrounds on hover).
- **Focus glow** — accent-tinted border (nav items draw a 2dp accent border when
  focused).
- **Active glow** — accent-tinted fill at ~14–18% (selected nav item, selected chips:
  `accent.primary.copy(alpha = 0.14f..0.2f)`).

## Glass & Transparency

`LayoutConfig.transparencyEnabled` / `blurEnabled` / `glassOpacity` (Theme Studio
Layout tab) control translucent surfaces. Default glass: `surface` at `glassOpacity`
(0.8), blur where the platform supports it.

## Surface Hierarchy

```
Background (darkest)
  └── Surface (cards, panels)
       └── Surface Elevated (dialogs, modals, inputs)
            └── Surface Interactive (hover/focus fills)
                 └── Surface Floating (popups, tooltips)
```

Each level is slightly lighter (dark mode) or slightly darker (light mode) than the
previous. The desktop `DsColors.surface` exposes the current `SurfaceColors` directly.

## Layout & Responsive Behavior

### Shared nav shell (`NavShell.kt`)

`NavShell` / `AdaptiveNavigation` unify navigation across phone, tablet and desktop:

- **Two modes** — `Sidebar` (docked on any edge: left/right/top/bottom) and
  `Floating` (draggable launcher bubble with snap points). `Ctrl+B` toggles.
- **Expansion** — expanded vs compact (icon rail). Expanded items are 40dp high
  (`NavTokens.ItemHeight`); compact hitboxes are 40dp desktop / 48dp phone.
- **Tokens** (`NavTokens`): `SidebarRadius = Dimens.RadiusXl`, `SidebarMargin = 8dp`,
  `SidebarElevation = 10dp`, `CompactRailWidth = 64dp`, `HorizontalBarHeight = 56dp`,
  `PhoneBarHeight = 52dp`, `ItemIconSize = 20dp`.
- **Auto-hide** — `NavAutoHide`: Never / Always / FullscreenOnly / Smart.

### Desktop workspace (`WorkspaceShell.kt`)

- **Breakpoints** (with hysteresis so resizing near the boundary doesn't flip-flop):
  - `< 720dp` (`Breakpoints.CompactWindowWidth`): compact bottom/top tab bar
    (`CompactLayout`).
  - `720–760dp`: hysteresis exit — once compact, stays compact until 760dp.
  - `≥ 760dp`: desktop dock (`DesktopLayout`) with `DsNavRail` (left/right) or
    `DsNavBar` (top/bottom), wrapped in `DsDockIsland` (8dp float ring).
- **Width tiers** (`DsResponsive.DsWidthTiers`): Compact 720, Standard 1024,
  Wide 1440, ExtraWide 1920. `gridColumnCount()` derives column counts; `adaptiveWidth`
  and `adaptiveDialogWidth` size dialogs/panels against the window (compact dialogs
  400–560dp, rich dialogs 480–860dp).

### Mobile

Phone/tablet layouts are handled by the shared `NavShell` (bottom/top bars on phone,
edge sidebar on tablet/desktop). `HomeScreenTab` bar on phones is 52dp tall with 48dp
touch targets.

## Window (Desktop)

`desktopApp/.../KaiteyoWindow.kt` + `Main.kt`:
- Default 1200×800, minimum 860×600, custom 44dp title bar (`DESKTOP.md`).
- Rounded corners (20dp) while floating; square when maximized.
- Window state persisted to `~/.kaiteyo/window.json` (`WindowStateStore`).
- Work-area-aware: saved positions are validated against the usable screen area.

## Related

- `docs/design/UI_SYSTEM.md` — the component catalog (real `Ds*` composables)
- `docs/design/THEME_SYSTEM.md` — how tokens flow from theme state to UI
- `docs/design/ANIMATION_SYSTEM.md` — motion tokens and patterns
- `docs/architecture/OVERVIEW.md` — module map and architecture
