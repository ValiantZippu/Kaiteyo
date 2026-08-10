# 🎨 design — Kaiteyo Design System

This directory documents Kaiteyo's complete design system.

## Contents

| File | Purpose |
|------|---------|
| `DESIGN_SYSTEM.md` | Complete design system overview |
| `DESIGN_LANGUAGE.md` | UI philosophy, spacing, typography, shadows |
| `UI_SYSTEM.md` | Component specs, interaction rules |
| `THEME_SYSTEM.md` | Theme tokens, built-in themes, custom themes |
| `ANIMATION_SYSTEM.md` | Animation philosophy, presets, patterns |

Deeper per-topic references (color system, typography, motion, icons, layout,
accessibility, platform UX) are planned; until then the files above are the
canonical design documentation.

## Design Principles

1. **Content-first** — UI chrome recedes, content dominates
2. **Floating elements** — Panels feel elevated, not attached
3. **Generous whitespace** — 4dp grid, 16dp minimum edge padding
4. **Consistent radius** — 8dp controls, 12dp cards, 16dp dialogs, 20dp window
5. **Spring animations** — Natural feel at 60 FPS

## Color Philosophy

- **Lime (#C2FC8B)**: Primary actions, selected states, navigation
- **Orange (#FEAB57)**: Secondary actions, hover, focus, highlights
- Both colors appear in gradients for premium surfaces

## Related

- `docs/design/DESIGN_LANGUAGE.md` — Design language overview
- `docs/design/UI_SYSTEM.md` — Component specifications
- `docs/design/ANIMATION_SYSTEM.md` — Animation specifications
