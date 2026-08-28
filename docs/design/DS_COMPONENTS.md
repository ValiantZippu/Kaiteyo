# Kaiteyo Desktop Design System — Components

> The complete component library for the Kaiteyo desktop suite.  
> Every UI element must consume these tokens and components — never hardcode values.

---

## Design Philosophy

Kaiteyo is a **modern, premium, desktop-first** Japanese learning workspace.  
The interface should feel calm, intentional, and connected across every screen.

**Keywords:** Soft · Premium · Dense-when-appropriate · Spacious-when-appropriate · Responsive · Animated · Keyboard-friendly

---

## Token System (`DsTokens.kt`)

All values scale with density (Compact/Comfortable/Spacious) and display zoom.

### Spacing (`DsSpacing`)
| Token | Base | Use |
|-------|------|-----|
| `Xs` | 4dp | Tight padding, inline gaps |
| `Sm` | 8dp | Default inner padding |
| `Md` | 12dp | Card padding, row gaps |
| `Lg` | 16dp | Section padding, form spacing |
| `Xl` | 24dp | Page margins |
| `Xxl` | 32dp | Major section gaps |
| `Section` | 40dp | Page section dividers |

### Corner Radius (`DsRadius`)
| Token | Base | Use |
|-------|------|-----|
| `Xs` | 4dp | Small chips, badges |
| `Sm` | 8dp | Inputs, buttons, small cards |
| `Md` | 12dp | Cards, panels, menus |
| `Lg` | 16dp | Major cards, dialogs |
| `Xl` | 24dp | Floating panels, large dialogs |
| `Full` | 999dp | Pills, toggle tracks |

### Typography (`DsType`)
| Token | Base | Weight | Use |
|-------|------|--------|-----|
| `Caption` | 11sp | Medium | Metadata, timestamps |
| `Label` | 12sp | Medium | Button labels, chips |
| `Body` | 14sp | Normal | Primary text |
| `BodyLarge` | 16sp | Normal | Emphasized body |
| `Title` | 18sp | SemiBold | Screen titles |
| `Heading` | 22sp | Bold | Section headings |
| `Display` | 28sp | Bold | Hero values, kanji |

### Elevation (`DsElevation`)
| Token | Value | Use |
|-------|-------|-----|
| `Flat` | 0dp | Default surface |
| `Raised` | 2dp | Hovered cards |
| `Floating` | 8dp | Popovers, tooltips |
| `Overlay` | 16dp | Dialogs |

### Motion (`DsMotion`)
| Token | Duration | Use |
|-------|----------|-----|
| `Fast` | 120ms | Micro-interactions |
| `Normal` | 240ms | Standard transitions |
| `Slow` | 380ms | Major transitions |

All durations honor `reducedMotion` and `speed` config.

### Semantic Colors (`DsSemantic`)
| Token | Description |
|-------|-------------|
| `Success` | Green — positive feedback |
| `Warning` | Orange — caution |
| `Error` | Red — destructive/danger |
| `Info` | Blue — informational |
| `New` | Purple — new items |
| `Due` | Orange — items due for review |
| `Favorite` | Yellow — favorited items |

---

## Components

### Buttons (`DsButtons.kt`)

| Component | Use |
|-----------|-----|
| `DsButton` | Primary/Secondary/Ghost/Danger/AccentTint actions |
| `DsIconButton` | Icon-only actions (34dp default hit area) |
| `DsTextButton` | Inline text links (e.g. "View all →") |
| `DsButtonRow` | Evenly spaced button groups |

**States:** Normal → Hover (subtle lift + color shift) → Pressed (scale 0.97) → Disabled (muted)

**Button Kinds:**
- `Primary` — main action (accent background)
- `Secondary` — visible but quieter (elevated surface)
- `Ghost` — minimal, text-like
- `Danger` — destructive (red)
- `AccentTint` — accent-tinted background (tags, badges)

### Cards & Lists (`DsCards.kt`)

| Component | Use |
|-----------|-----|
| `DsCard` | Base card surface (flat or elevated) |
| `DsListItem` | List row with leading/trailing slots |
| `DsVirtualList` | Lazy column for 100k+ items |
| `DsFavoriteToggle` | Star toggle |
| `DsChevron` | Navigation affordance |
| `DsSkeleton` | Loading placeholder |
| `DsSkeletonCard` | Full card skeleton |
| `DsEmptyState` | Empty/error state with icon + action |

**Card design:** Top accent line on hover, gentle elevation lift, `DsRadius.Lg`.

### Inputs (`DsInputs.kt`)

| Component | Use |
|-----------|-----|
| `DsTextField` | Standard text input with bottom border |
| `DsSearchField` | Search with icon + clear button |
| `DsNumericField` | Digit-only field |

**Focus:** Bottom hairline glows accent color.

### Dialogs (`DsDialog.kt`)

| Component | Use |
|-----------|-----|
| `DsDialog` | Base dialog with title |
| `DsConfirmDialog` | Confirm/cancel with danger variant |
| `DsPromptDialog` | Text input dialog |
| `DsProgressDialog` | Progress bar dialog |

**Entrance:** Spring scale 0.94→1.0 + fade 180ms.

### Menus (`DsMenu.kt`)

| Component | Use |
|-----------|-----|
| `DsContextMenuHost` | Wraps content with right-click menu |
| `DsMenuPanel` | Keyboard-navigable menu panel |
| `DsMenuItem` | Menu item data class |
| `DsMenuDivider` | Section divider |

**Keyboard:** ↑/↓ navigate (wrapping), Enter activates, Esc dismisses.

### Tabs & Selection (`DsSelect.kt`)

| Component | Use |
|-----------|-----|
| `DsSelect` | Dropdown selector with animated chevron |
| `DsTabRow` | Segmented tab bar |
| `DsChip` | Filter/toggle pill |
| `DsCategoryBadge` | Settings category badge |

### Tags (`DsTag.kt`)

| Component | Use |
|-----------|-----|
| `DsTagChip` | Colored tag with dot + optional remove |
| `DsFlagBadge` | Flag indicator |
| `DsPriorityFlag` | Priority marker (P1-P5) |

### Toasts (`DsToast.kt`)

| Component | Use |
|-----------|-----|
| `DsToastHost` | State holder for toast messages |
| `DsToastHostView` | Renders toasts over content |
| `DsToastItem` | Individual toast (Success/Warning/Error/Info) |

### Toolbar (`DsToolbar.kt`)

| Component | Use |
|-----------|-----|
| `DsToolbar` | Page header with title + actions |
| `DsToolbarDivider` | Thin horizontal divider |
| `DsSplitPane` | Resizable horizontal/vertical split |

### Misc (`DsMisc.kt`)

| Component | Use |
|-----------|-----|
| `DsBadge` | Numeric badge (review counts) |
| `DsStatTile` | Label + big value + delta |
| `DsProgressBar` | Inline progress bar |
| `DsSwitch` | Kaiteyo-native toggle switch |
| `DsToggle` | Labeled switch |
| `DsLink` | Link-style action row |
| `DsSectionHeader` | Section title with optional action |
| `DsNumberLabel` | Grid density number |

### Responsive (`DsResponsive.kt`)

| Helper | Description |
|--------|-------------|
| `DsWidthTiers` | Compact/Standard/Wide/ExtraWide breakpoints |
| `rememberWidthTier()` | Current width tier ordinal |
| `adaptiveWidth()` | Window-responsive sizing |
| `adaptiveDialogWidth()` | Dialog width that grows with window |
| `gridColumnCount()` | Responsive column count |

---

## Theme System

### SurfaceColors (via `surfaceColors()`)
Every component reads semantic surface colors from the active theme.

| Token | Description |
|-------|-------------|
| `background` | Page background |
| `surface` | Card/panel background |
| `surfaceElevated` | Elevated card/dialog background |
| `surfaceInteractive` | Hover/select background |
| `textPrimary` | Primary text |
| `textSecondary` | Secondary text |
| `textMuted` | Muted/caption text |
| `border` | Subtle borders |

### AccentColors (via `accent()`)
| Token | Description |
|-------|-------------|
| `primary` | Brand accent |
| `onPrimary` | Text on accent |

### Themes
- **Dark** — default, premium feel
- **Light** — clean, accessible
- **Sepia** — reading-optimized warm tones

---

## Usage Rules

1. **Never hardcode colors** — use `surfaceColors()` and `accent()`
2. **Never hardcode spacing** — use `DsSpacing` tokens
3. **Never hardcode radii** — use `DsRadius` tokens
4. **Never hardcode font sizes** — use `DsType` tokens
5. **Use `DsButton` variants** — don't create custom button styles
6. **Use `DsCard`** — don't create custom card containers
7. **Use `DsSearchField`** — don't create custom search inputs
8. **Use `DsEmptyState`** — every screen needs an empty state
9. **Use `DsSkeleton`** — loading states must show layout
10. **Honor reduced motion** — `DsMotion.duration()` respects config

---

## Adding New Components

1. Add to the appropriate `Ds*.kt` file
2. Use existing tokens (`DsSpacing`, `DsRadius`, `DsType`, `surfaceColors()`, `accent()`)
3. Support all interaction states (hover, press, disabled)
4. Support all themes (dark, light, sepia)
5. Add keyboard support where applicable
6. Document in this file
