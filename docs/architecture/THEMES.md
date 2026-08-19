# Kaiteyo Theme Architecture

## Overview

Kaiteyo's theme system provides 4 base modes, 7 accent schemes, and deep customization through tokens. Every color in the UI flows through composition locals — no hardcoded colors.

## Base Modes

| Mode | Background | Surface | Surface Elevated | Text Primary |
|---|---|---|---|---|
| OLED Black | #050505 | #0D0D0D | #101010 | #F0F0F0 |
| Dark Gray | #121212 | #1A1A1A | #242424 | #F0F0F0 |
| Light | #F5F5F5 | #EEEEEE | #E8E8E8 | #1A1A1A |
| Sepia | #F5F0E8 | #EDE5D8 | #E5DCC8 | #3D3028 |

## Accent Schemes

1. **Signature Pineapple** (default): #C2FC8B / #FEAB57
2. **Cotton Candy**: #D4A5F0 / #FFB5C5
3. **Ocean**: #00D4AA / #00A8FF
4. **Forest**: #81C784 / #A5D6A7
5. **Sunset**: #FF6B6B / #FFB347
6. **Lavender**: #B39DDB / #CE93D8
7. **Monochrome**: #E0E0E0 / #9E9E9E

Each scheme defines: primary, primaryDark, secondary, secondaryDark, onPrimary, onSecondary, tertiary, gradientStart/End.

## Token System

### SurfaceColors

```kotlin
data class SurfaceColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceInteractive: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textInverse: Color
)
```

### Dimens (Spacing Scale)

Based on 4dp grid:
- Space1 = 4dp, Space2 = 8dp, Space3 = 12dp, Space4 = 16dp
- Space5 = 20dp, Space6 = 24dp, Space8 = 32dp, Space10 = 40dp
- Space12 = 48dp, Space16 = 64dp, Space20 = 80dp

### Corner Radius

| Token | Value | Use |
|---|---|---|
| RadiusXs | 4dp | Checkboxes, indicators |
| RadiusSm | 8dp | Buttons, inputs, small cards |
| RadiusMd | 12dp | Standard cards, list items |
| RadiusLg | 16dp | Large cards, modals |
| RadiusXl | 24dp | Sidebar, content panels |
| Radius2xl | 32dp | Large containers, dialogs |

Multiplied by `RadiusConfig.globalMultiplier` at runtime.

### AnimationConfig

- Speed: Slow (1.5x), Normal (1x), Fast (0.6x), Instant (0x)
- Spring: damping 0.6, stiffness 300
- Page transitions: Crossfade, Slide, FadeThrough, Scale
- Theme transition: 450ms crossfade

### GlowConfig

- Intensity, radius, opacity for glow effects
- Applied to accent elements

### TypeScale

- Font scale, title scale, line height, letter spacing

## Composition Locals

| Local | Provides |
|---|---|
| `LocalSurfaceColors` | Current mode's SurfaceColors |
| `LocalKaiteyoAccent` | Current accent scheme |
| `LocalKaiteyoThemeState` | Full theme state (mode + accent + config) |
| `LocalAnimationConfig` | Animation parameters |
| `LocalRadiusConfig` | Corner radius settings |
| `LocalGlowConfig` | Glow effect settings |
| `LocalTypeScale` | Typography adjustments |
| `LocalLayoutConfig` | Density, sidebar, transparency |

## Theme State

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

## Sepia Theme

Sepia is a first-class theme with a complete visual hierarchy:
- Background: warm paper (#F5F0E8)
- Surfaces: slightly darker paper tones
- Text: dark brown (#3D3028)
- Borders: warm gray (#D4C8B8)
- Designed for comfortable Japanese typography and reading

## Color Codecs

KMP-safe color serialization:
- `Color.toHexString()`: `#RRGGBB` (optionally `#RRGGBBAA`)
- `parseColorHex()`: Parses back to `Color`
- No `java.*` APIs — safe on all targets

## Persistence

Theme settings are serialized as JSON in `PreferencesContract.AppPreferences.themeSettingsJson`. Includes:
- Base mode index
- Accent scheme index
- Animation speed
- Corner radius style
- Glow intensity
- Layout density

## Design Principles

1. **No hardcoded colors**: Every color flows through composition locals
2. **Semantic colors**: `semanticSuccess`, `semanticWarning`, `semanticError`, `semanticInfo`
3. **Accessibility**: High contrast mode, reduced motion, focus indicators
4. **Consistency**: Same tokens across all screens
5. **Theme-aware components**: `KaiteyoSectionCard`, `KaiteyoPill`, `KaiteyoTag` all read from locals

## Usage in Composables

```kotlin
@Composable
fun MyComponent() {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Box(
        Modifier
            .background(surfaceColors.surface)
            .border(1.dp, surfaceColors.border, RoundedCornerShape(Dimens.RadiusMd))
    ) {
        Text("Hello", color = surfaceColors.textPrimary)
        Text("Secondary", color = surfaceColors.textSecondary)
        Box(Modifier.background(accent.primary)) { ... }
    }
}
```
