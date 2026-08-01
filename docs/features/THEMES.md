# Kaiteyo — Theme System Specification

## Purpose

The theme system allows users to customize every visual aspect of Kaiteyo. It provides built-in themes, a full Appearance Studio for custom theme creation, and live preview of changes.

## User Experience

### Built-in Themes
Users can select from 8 built-in themes:
1. **Signature** (default) — Lime + Orange, the official Kaiteyo identity
2. **OLED Black** — True black for OLED displays
3. **Dark Gray** — Softer dark mode
4. **Light** — Clean light mode
5. **Reading** — Warm paper tones for extended reading
6. **Cotton Candy** — Soft pastels
7. **Ocean** — Cool blue tones
8. **Forest** — Earthy green tones

### Appearance Studio
A "+" button opens the full Appearance Studio with:

**Color Studio:**
- Color wheels (RGB, HSV, HSL, HEX)
- Opacity control
- Gradient editor with multiple stops
- Live preview of all changes

**Motion Studio:**
- Animation presets: None, Minimal, Standard, Smooth, Bouncy
- Speed control
- Spring stiffness tuning

**Layout Studio:**
- Sidebar position (Left, Right, Top, Bottom, Floating)
- Density modes (Compact, Comfortable, Spacious)
- Corner radius slider
- Transparency/blur controls
- Surface elevation controls

### Live Preview
Every change updates immediately in a preview panel showing:
- Sidebar
- Cards
- Buttons
- Lists
- Dialogs
- Navigation

### Theme Import/Export
- Export current theme as JSON
- Import theme from JSON file
- Shareable theme format

## Technical Design

### Architecture
```
ThemeManager (interface)
  └── KaiteyoThemeState (mutable state holder)
       ├── baseMode (Light/Dark/Oled)
       ├── accentScheme (KaiteyoAccentScheme)
       ├── glowConfig (GlowConfig)
       ├── radiusConfig (RadiusConfig)
       ├── animationConfig (AnimationConfig)
       └── densityConfig (DensityConfig)
```

### CompositionLocals
- `LocalKaiteyoThemeState` — Mutable theme state
- `LocalKaiteyoAccent` — Current accent scheme
- `LocalSurfaceColors` — Surface hierarchy colors
- `LocalKaiteyoAccentList` — All available accent schemes

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
  "glow": { "intensity": 1.0, "spread": 4 },
  "radius": { "cornerRadius": 12, "customRadius": null },
  "animation": { "speed": 2, "springStiffness": 300, "springDamping": 0.5 },
  "density": { "mode": "comfortable", "spacingMultiplier": 1.0 }
}
```

## Dependencies
- Compose Multiplatform
- Koin (for ThemeManager injection)
- DataStore (for theme persistence)

## Future Improvements
- Community theme sharing
- Theme marketplace
- AI-generated themes
- Dynamic theme based on wallpaper
- Time-based theme switching (auto dark/light)

## Open Questions
- Should themes sync across devices?
- Should there be a theme rating system?
- Should themes support animated backgrounds?
