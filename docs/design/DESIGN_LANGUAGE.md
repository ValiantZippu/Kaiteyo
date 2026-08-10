# Kaiteyo (書いてよ) — Design Language

## Spacing System

Kaiteyo uses a 4dp base grid. All spacing values are multiples of 4.

| Token | Value | Usage |
|-------|-------|-------|
| `spacing.xxs` | 4dp | Tight icon padding, inline gaps |
| `spacing.xs` | 8dp | Between related elements |
| `spacing.sm` | 12dp | Between grouped elements |
| `spacing.md` | 16dp | Standard padding for cards, panels |
| `spacing.lg` | 24dp | Section spacing |
| `spacing.xl` | 32dp | Major section breaks |
| `spacing.xxl` | 48dp | Page-level margins |

## Typography Scale

| Level | Size | Weight | Usage |
|-------|------|--------|-------|
| Display | 32sp | Bold | Page titles, hero text |
| Title Large | 22sp | SemiBold | Section headers |
| Title Medium | 18sp | SemiBold | Card titles, dialog headers |
| Title Small | 14sp | Medium | Subsection headers |
| Body Large | 16sp | Regular | Primary content |
| Body Medium | 14sp | Regular | Secondary content |
| Body Small | 12sp | Regular | Captions, metadata |
| Label | 11sp | Medium | Badges, small annotations |

## Corner Radius

| Token | Value | Usage |
|-------|-------|-------|
| `radius.xs` | 4dp | Small buttons, tags |
| `radius.sm` | 8dp | Control buttons, inputs |
| `radius.md` | 12dp | Cards, panels |
| `radius.lg` | 16dp | Dialogs, modals |
| `radius.xl` | 20dp | Window corners, large surfaces |
| `radius.full` | 50% | Circular elements, avatars |

## Shadows & Elevation

| Level | Elevation | Usage |
|-------|-----------|-------|
| 0 | 0dp | Flat surfaces (background) |
| 1 | 1dp | Subtle separation (cards on surface) |
| 2 | 4dp | Floating elements (dropdowns, tooltips) |
| 3 | 8dp | Elevated panels (sidebar, dialogs) |
| 4 | 16dp | Modal overlays |
| 5 | 24dp | Top-level elements (notifications) |

Shadows should use the accent color tinted at low opacity (e.g., `accent.primary.copy(alpha = 0.15f)`).

## Glow System

Glows are used for interactive feedback:
- **Hover glow**: Accent color at 15-20% opacity behind the element
- **Focus glow**: Accent color at 10% opacity with 2dp spread
- **Active glow**: Accent color at 25% opacity with subtle scale

## Glass & Transparency

Glass effects (backdrop blur) are used sparingly:
- Floating panels in the sidebar
- Modal overlays
- Tooltip backgrounds

Standard glass parameters:
- Background: `surface.copy(alpha = 0.85f)`
- Blur radius: 20px
- Border: `accent.primary.copy(alpha = 0.08f)`

## Surface Hierarchy

```
Background (darkest)
  └── Surface (cards, panels)
       └── Surface Elevated (dialogs, modals)
            └── Surface Floating (tooltips, popovers)
                 └── Surface Overlay (modal backdrops)
```

Each level is slightly lighter (dark mode) or slightly darker (light mode) than the previous.

## Layout Philosophy

- **Content-first**: Navigation and chrome should recede, letting content dominate
- **Floating elements**: Sidebars, panels, and controls should feel elevated, not attached
- **Generous whitespace**: Don't crowd elements. Let them breathe.
- **Responsive**: Layout adapts to window size, not device type

## Visual Rhythm

- Consistent 8dp vertical rhythm between unrelated elements
- 4dp between related elements
- 16dp minimum padding from window edges
- 24dp between major sections

## Responsive Behavior

- Window width < 800dp: Compact layout (sidebar collapses, single column)
- Window width 800-1200dp: Comfortable layout (sidebar visible, two columns)
- Window width > 1200dp: Spacious layout (full sidebar, multi-column)
