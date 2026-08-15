# 🎨 design — Kaiteyo Design System

This directory documents Kaiteyo's design system **as implemented** — every doc is
grounded in the real token code (`core/.../theme/`, `desktopApp/.../designsystem/`)
and the actual composables that consume it.

## Contents

| File | Purpose |
|------|---------|
| `DESIGN_LANGUAGE.md` | Design language: **real tokens** (Dimens, DsSpacing/DsRadius/DsType/DsMotion), typography, palette, layout & responsive rules |
| `DESIGN_SYSTEM.md` | Complete **visual identity** (brand vision v2.0) + appendix mapping vision → shipped implementation |
| `UI_SYSTEM.md` | **Component catalog** — real `Ds*` composables and shared components with their actual APIs |
| `THEME_SYSTEM.md` | **Theme system** — BaseMode + accent schemes + `KaiteyoThemeState`, `AppTheme`, `ThemeManager`/`ThemeMapper`, persistence |
| `ANIMATION_SYSTEM.md` | **Motion system** — `AnimationConfig`/`AnimationSpeed`/`DsMotion`, helpers, concrete patterns, reduced motion |

## Design Principles

1. **Content-first** — UI chrome recedes, content dominates
2. **Floating elements** — panels feel elevated, not attached (`DsDockIsland` ring)
3. **Generous whitespace** — 4dp grid, 16dp minimum edge padding
4. **Consistent radius** — 8dp controls, 12dp cards, 16dp dialogs, 24dp panels
5. **Spring animations** — natural feel at 60 FPS, config-driven

## Color Philosophy

- **Lime (#C2FC8B)**: primary actions, selected states, navigation (Signature accent)
- **Orange (#FEAB57)**: secondary actions, highlights
- 4 base modes (OLED/Dark/Light/Sepia) × 7 accent schemes, all themeable via the
  Appearance Studio / Theme Studio

## Source-of-truth files

| Concern | File |
|---------|------|
| Palette, accents, semantic colors | `core/.../presentation/common/theme/Color.kt` |
| Theme state, AppTheme, configs, motion helpers | `core/.../presentation/common/theme/Theme.kt` |
| Typography | `core/.../presentation/common/theme/Typography.kt` |
| Spacing/radius base values | `core/.../presentation/common/theme/Dimens.kt` |
| Desktop tokens (scaled) | `desktopApp/.../desktop/designsystem/DsTokens.kt` |
| Desktop components | `desktopApp/.../desktop/designsystem/Ds*.kt` |
| Desktop Theme Studio state | `desktopApp/.../desktop/engine/theming/ThemeManager.kt` |

## Related

- `docs/features/THEMES.md` — Theme Studio / Appearance Studio user experience
- `docs/features/DESKTOP.md` — window shell and workspace
- `docs/architecture/accessibility.md` — contrast, focus, reduced motion
