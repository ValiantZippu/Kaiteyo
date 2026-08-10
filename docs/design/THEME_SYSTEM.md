# Kaiteyo (書いてよ) — Theme System

## Overview

Kaiteyo's theme system is built on a token-based architecture. Every visual property is controlled by a theme token, enabling complete customization through the Appearance Studio.

## Theme Tokens

### Color Tokens

| Token | Description | Example (Signature Dark) |
|-------|-------------|--------------------------|
| `primary` | Primary accent color | `#C2FC8B` (Lime) |
| `secondary` | Secondary accent color | `#FEAB57` (Orange) |
| `onPrimary` | Text/icon on primary | `#1A1A1A` |
| `onSecondary` | Text/icon on secondary | `#1A1A1A` |
| `background` | Main background | `#1A1A1A` |
| `surface` | Card/panel background | `#242424` |
| `surfaceElevated` | Elevated surface | `#2E2E2E` |
| `surfaceFloating` | Floating elements | `#333333` |
| `textPrimary` | Primary text | `#FFFFFF` |
| `textSecondary` | Secondary text | `#A0A0A0` |
| `border` | Subtle borders | `#333333` |
| `error` | Error states | `#FF6B6B` |
| `success` | Success states | `#4CAF50` |
| `warning` | Warning states | `#FFB74D` |
| `info` | Information states | `#64B5F6` |

### Gradient Tokens

| Token | Description |
|-------|-------------|
| `gradientStart` | Start color for accent gradients |
| `gradientEnd` | End color for accent gradients |
| `surfaceGradient` | Subtle surface gradient |

### Glow Tokens

| Token | Description |
|-------|-------------|
| `glowIntensity` | 0.0 to 2.0 multiplier for glow effects |
| `glowColor` | Color used for glow (usually primary) |
| `glowSpread` | Radius of glow effect in dp |

### Radius Tokens

| Token | Description |
|-------|-------------|
| `cornerRadius` | Global corner radius multiplier |
| `customRadius` | Per-component radius override |

### Animation Tokens

| Token | Description |
|-------|-------------|
| `animationSpeed` | 0 (none) to 3 (bouncy) |
| `springStiffness` | Spring stiffness for animations |
| `springDamping` | Spring damping ratio |
| `transitionDuration` | Base transition duration in ms |

### Density Tokens

| Token | Description |
|-------|-------------|
| `density` | Compact, Comfortable, or Spacious |
| `spacingMultiplier` | 0.75x, 1.0x, or 1.25x |

---

## Built-in Themes

### Signature (Default)
The official Kaiteyo identity. Uses both lime and orange distributed intelligently.

| Token | Value |
|-------|-------|
| Primary | `#C2FC8B` |
| Secondary | `#FEAB57` |
| Background | `#1A1A1A` |
| Surface | `#242424` |
| Gradient | Lime → Orange |

### OLED Black
True black background for OLED displays. Maximum contrast.

| Token | Value |
|-------|-------|
| Primary | `#C2FC8B` |
| Background | `#000000` |
| Surface | `#0A0A0A` |

### Dark Gray
Softer dark mode. Less contrast than OLED, easier on eyes.

| Token | Value |
|-------|-------|
| Primary | `#C2FC8B` |
| Background | `#1E1E1E` |
| Surface | `#2D2D2D` |

### Light
Clean light mode for daytime use.

| Token | Value |
|-------|-------|
| Primary | `#4CAF50` |
| Background | `#FAFAFA` |
| Surface | `#FFFFFF` |
| Text Primary | `#1A1A1A` |

### Reading
Warm paper tones for extended reading sessions.

| Token | Value |
|-------|-------|
| Primary | `#8B7355` |
| Background | `#F5F0E8` |
| Surface | `#EDE5D8` |
| Text Primary | `#3D3028` |
| Text Secondary | `#7A6B5D` |

### Cotton Candy
Soft pastel theme. Playful but not childish.

| Token | Value |
|-------|-------|
| Primary | `#FF9EBB` |
| Secondary | `#B388FF` |
| Background | `#1A1A2E` |
| Surface | `#252540` |

### Ocean
Cool blue tones. Calm and focused.

| Token | Value |
|-------|-------|
| Primary | `#4FC3F7` |
| Secondary | `#81D4FA` |
| Background | `#0D1B2A` |
| Surface | `#1B2838` |

### Forest
Earthy green tones. Natural and grounding.

| Token | Value |
|-------|-------|
| Primary | `#81C784` |
| Secondary | `#A5D6A7` |
| Background | `#1A2E1A` |
| Surface | `#243824` |

---

## Theme Architecture

```
ThemeManager
  └── currentTheme (StateFlow<PreferencesTheme>)
       └── Maps to BaseMode (Light/Dark/Oled)
            └── Applies KaiteyoAccentScheme
                 └── Generates color palette
                      └── Provides via CompositionLocals
```

### CompositionLocals

| Local | Provides |
|-------|----------|
| `LocalKaiteyoThemeState` | Mutable theme state (base mode, accent, glow, radius, animation, density) |
| `LocalKaiteyoAccent` | Current accent scheme colors |
| `LocalSurfaceColors` | Surface hierarchy colors |
| `LocalKaiteyoAccentList` | All available accent schemes |

### Theme State

```kotlin
class KaiteyoThemeState {
    var baseMode: BaseMode
    var accentScheme: KaiteyoAccentScheme
    var glowConfig: GlowConfig
    var radiusConfig: RadiusConfig
    var animationConfig: AnimationConfig
    var densityConfig: DensityConfig
}
```

---

## Custom Themes

Users can create custom themes through the Appearance Studio:
1. Start from any built-in theme as a base
2. Modify individual color tokens
3. Create gradient stops
4. Adjust glow, radius, animation, and density
5. Save as a named preset
6. Export/import as JSON

### Theme JSON Format

```json
{
  "name": "My Custom Theme",
  "base": "signature",
  "colors": {
    "primary": "#C2FC8B",
    "secondary": "#FEAB57",
    "background": "#1A1A1A"
  },
  "gradient": {
    "start": "#C2FC8B",
    "end": "#FEAB57"
  },
  "glow": {
    "intensity": 1.0,
    "spread": 4
  },
  "radius": {
    "cornerRadius": 12,
    "customRadius": null
  },
  "animation": {
    "speed": 2,
    "springStiffness": 300,
    "springDamping": 0.5
  },
  "density": {
    "mode": "comfortable",
    "spacingMultiplier": 1.0
  }
}
