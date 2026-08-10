# Kaiteyo (書いてよ) — UI System

## Component Philosophy

Every component in Kaiteyo follows these principles:
1. **Purposeful** — No decorative-only components
2. **Consistent** — Same component behaves the same everywhere
3. **Responsive** — Adapts to context and window size
4. **Accessible** — Keyboard navigable, screen-reader friendly
5. **Animated** — Smooth transitions for state changes

---

## Buttons

### Primary Button
- Solid background using accent primary color
- White text
- 8dp corner radius
- Hover: slight scale-up (1.02x) with glow
- Press: scale-down (0.98x)
- Min height: 40dp, horizontal padding: 16dp

### Secondary Button
- Outline style with accent color border
- Transparent background (filled on hover)
- Same dimensions as primary

### Ghost Button
- No border until hover
- Subtle background on hover
- Used for toolbar actions

### Icon Button
- 36dp x 36dp size
- 8dp corner radius
- No text, only icon
- Hover: background fill at 10% accent
- Used for window controls, toolbars

---

## Cards

### Standard Card
- Background: surface color
- Corner radius: 12dp
- Inner padding: 16dp
- Subtle border: `accent.copy(alpha = 0.05f)`
- Hover: elevation increase + subtle glow
- May contain: title, description, metadata, actions

### Interactive Card
- Same as standard card
- Clickable with ripple/hover effect
- Cursor changes to pointer
- Used for: study items, selection grids

### Preview Card
- Used in Appearance Studio live preview
- Slightly elevated shadow
- Shows a miniature representation of UI
- 120dp x 80dp minimum size

---

## Lists

### Standard List
- 48dp minimum row height
- 16dp horizontal padding
- 4dp vertical gap between items
- Optional leading icon/image
- Optional trailing action

### Compact List
- 36dp row height
- Dense layout for settings
- Used in: settings panels

---

## Dialogs & Modals

### Dialog
- Corner radius: 16dp
- Background: surface elevated color
- Shadow: elevation level 4 (16dp)
- Width: 480dp maximum
- Padding: 24dp
- Closes on: Escape key, clicking backdrop, explicit close button

### Modal
- Full-screen overlay for focused tasks
- Backdrop: 60% opacity black
- Content centered vertically and horizontally
- Used for: Theme Studio, detailed editing

---

## Sidebar

### Floating Island
- Not attached to window edge
- 8dp gap from edge
- Corner radius: 16dp
- Shadow: elevation level 3 (8dp)
- Width: 280dp expanded, 56dp collapsed
- Can dock to: left, right, top, bottom
- Collapse/expand with spring animation

### Navigation Sidebar
- Contains: app logo, navigation items, user menu
- Each item: 44dp height, icon + label
- Active item: accent primary background
- Collapsed: only icons visible

---

## Inputs

### Text Field
- 44dp height
- 12dp corner radius
- Border: 1dp, `textSecondary.copy(alpha = 0.3f)`
- Focus: accent primary border + glow
- Label above field, helper text below

### Slider
- Track height: 4dp
- Thumb size: 16dp
- Accent primary color
- Smooth animation on value change

### Toggle/Switch
- Width: 44dp, height: 24dp
- Thumb: 20dp circle
- Active: accent primary
- Inactive: `textSecondary.copy(alpha = 0.3f)`

---

## Navigation

### Tab Bar
- 48dp height
- Active indicator: 2dp underline with accent primary
- Hover: subtle background change
- Optional icon + label layout

### Breadcrumbs
- 20dp height
- Separator: ">" in textSecondary
- Current page: textPrimary, not clickable

---

## Progress Indicators

### Linear Progress
- 4dp height
- Accent primary color
- Indeterminate: animated gradient sweep
- Determinate: smooth fill animation

### Circular Progress
- 36dp diameter
- 3dp stroke width
- Used for: loading states, sync indicators

---

## Tooltips

- Appear on hover after 300ms delay
- Position: above the element
- Background: surface elevated at 95% opacity
- Text: 12sp, primary text color
- Corner radius: 6dp
- Padding: 8dp horizontal, 4dp vertical

---

## Interaction Rules

| State | Visual Change | Duration | Easing |
|-------|--------------|----------|--------|
| Hover | Scale 1.02x, background glow | 150ms | ease-out |
| Press | Scale 0.98x | 80ms | ease-in |
| Focus | Outer glow ring | 200ms | ease-out |
| Disabled | 40% opacity | 200ms | linear |
| Loading | Skeleton shimmer | 1.5s loop | linear |

## Keyboard Navigation

- Tab: Move focus forward
- Shift+Tab: Move focus backward
- Enter/Space: Activate focused element
- Escape: Close dialog/modal
- Arrow keys: Navigate within lists, sliders
- Ctrl+K: Command palette (future)
