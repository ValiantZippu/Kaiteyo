# Kaiteyo Design System — Complete Reference

> The single source of truth for Kaiteyo's visual language.  
> Every screen, component, and interaction must derive from this system.

---

## Design Philosophy

Kaiteyo is a **modern, premium, desktop-first** Japanese learning workspace.

**Core principles:**
- **Connected** — every screen feels like part of one application
- **Calm** — interface supports long sessions without visual fatigue
- **Intentional** — every element has a purpose and hierarchy
- **Responsive** — adapts to window size, density, and theme
- **Accessible** — keyboard-first, screen-reader-friendly, reduced-motion support

**Visual identity:** Soft, premium, spatial, Japanese-learning oriented.

---

## Architecture

```
FOUNDATION (DsTokens.kt)
├── DsSpacing    — spacing scale (Xs→Section)
├── DsRadius     — corner radius scale (Xs→Full)
├── DsType       — typography scale (Caption→Display)
├── DsMotion     — animation durations + spring presets
├── DsElevation  — shadow/depth levels
├── DsIconSize   — standardized icon sizes
├── DsBorder     — border width tokens
└── DsSemantic   — status colors (success/warning/error/info)

THEME (SurfaceColors + KaiteyoAccentScheme)
├── Dark    — default, premium feel
├── Light   — clean, accessible
└── Sepia   — reading-optimized warm tones

COMPONENTS (Ds*.kt)
├── DsButtons     — Primary/Secondary/Ghost/Danger/AccentTint + IconButton + TextButton
├── DsCards       — Card, ListItem, VirtualList, Skeleton, EmptyState
├── DsDialogs     — Dialog, Confirm, Prompt, Progress
├── DsInputs      — TextField, SearchField, NumericField, TextArea
├── DsMenu        — ContextMenu, MenuPanel, MenuItem, MenuDivider
├── DsSelect      — Select, TabRow, Chip, CategoryBadge
├── DsTags        — TagChip, FlagBadge, PriorityFlag
├── DsToast       — ToastHost, ToastItem (Success/Warning/Error/Info)
├── DsToolbar     — Toolbar, ToolbarDivider, SplitPane
├── DsSwitch      — Native toggle (no Material3)
├── DsMisc        — Badge, StatTile, ProgressBar, Toggle, Link, SectionHeader
├── DsResponsive  — WidthTiers, adaptiveWidth, gridColumnCount
└── DsPageShell   — PageShell, PageHeader, Section, SectionCard, PageEmpty/PageLoading/PageError
```

---

## Token Reference

### Spacing (`DsSpacing`)

| Token | Base | Density ×0.7 | ×1.0 | ×1.3 |
|-------|------|-------------|------|------|
| `Xs` | 4dp | 2.8dp | 4dp | 5.2dp |
| `Sm` | 8dp | 5.6dp | 8dp | 10.4dp |
| `Md` | 12dp | 8.4dp | 12dp | 15.6dp |
| `Lg` | 16dp | 11.2dp | 16dp | 20.8dp |
| `Xl` | 24dp | 16.8dp | 24dp | 31.2dp |
| `Xxl` | 32dp | 22.4dp | 32dp | 41.6dp |
| `Section` | 40dp | 28dp | 40dp | 52dp |

**Usage:** Padding, margins, gaps, layout spacing. Never hardcode dp values.

### Radius (`DsRadius`)

| Token | Base | Use |
|-------|------|-----|
| `Xs` | 4dp | Badges, small chips |
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

### Motion (`DsMotion`)

| Token | Duration | Use |
|-------|----------|-----|
| `Instant` | 0ms | Immediate response |
| `Fast` | 120ms | Micro-interactions |
| `Normal` | 240ms | Standard transitions |
| `Slow` | 380ms | Major transitions |

**Spring presets:**
| Preset | Damping | Stiffness | Use |
|--------|---------|-----------|-----|
| `SpringMicro` | 0.6 | 500 | Hover, press, toggle |
| `SpringSnappy` | 0.55 | 380 | Panels, menus |
| `SpringSoft` | 0.7 | 280 | Floating elements |
| `SpringPanel` | 0.65 | 420 | Panel entrance |
| `SpringDialog` | 0.6 | 360 | Dialog entrance |

### Elevation (`DsElevation`)

| Token | Value | Use |
|-------|-------|-----|
| `Flat` | 0dp | Default surface |
| `Raised` | 2dp | Hovered cards |
| `Floating` | 8dp | Popovers, tooltips |
| `Overlay` | 16dp | Dialogs |

### Icon Sizes (`DsIconSize`)

| Token | Size | Use |
|-------|------|-----|
| `Xs` | 12dp | Inline indicators |
| `Sm` | 16dp | Button icons, list icons |
| `Md` | 20dp | Navigation icons |
| `Lg` | 24dp | Primary action icons |
| `Xl` | 32dp | Feature icons |
| `Xxl` | 48dp | Hero/decorative icons |

### Border Widths (`DsBorder`)

| Token | Width | Use |
|-------|-------|-----|
| `Hairline` | 0.5dp | Subtle dividers |
| `Thin` | 1dp | Default borders |
| `Medium` | 1.5dp | Emphasized borders |
| `Thick` | 2dp | Focus rings, active states |

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

**Tinted backgrounds:**
| Token | Opacity | Use |
|-------|---------|-----|
| `accentSoft()` | 8% | Card highlights, selected states |
| `accentMedium()` | 16% | Chip fills, button tints |
| `accentStrong()` | 26% | Hover states, active chips |
| `errorSoft()` | 12% | Destructive card backgrounds |
| `successSoft()` | 12% | Positive status backgrounds |
| `warningSoft()` | 12% | Caution backgrounds |
| `infoSoft()` | 12% | Informational backgrounds |

---

## Component Reference

### Buttons (`DsButtons.kt`)

```kotlin
DsButton(text = "Save", onClick = { save() }, kind = DsButtonKind.Primary)
DsButton(text = "Cancel", onClick = { cancel() }, kind = DsButtonKind.Secondary)
DsButton(text = "Delete", onClick = { delete() }, kind = DsButtonKind.Danger)
DsButton(text = "More", onClick = { more() }, kind = DsButtonKind.Ghost)
DsButton(text = "Tag", onClick = { tag() }, kind = DsButtonKind.AccentTint)

DsIconButton(icon = Icons.Default.Settings, onClick = { openSettings() })
DsTextButton(text = "View all →", onClick = { viewAll() })
```

**States:** Normal → Hover (color shift) → Pressed (scale 0.97) → Disabled (muted)

### Cards (`DsCards.kt`)

```kotlin
DsCard { /* flat card */ }
DsCard(elevated = true) { /* elevated card */ }
DsCard(onClick = { navigate() }) { /* interactive card with hover lift */ }

DsListItem(
    leading = { Icon(Icons.Default.Star, ...) },
    title = "Item name",
    subtitle = "Description",
    trailing = { DsChevron() },
    onClick = { open() }
)
```

### Dialogs (`DsDialog.kt`)

```kotlin
DsDialog(title = "Settings", onDismiss = { close() }) { /* content */ }

DsConfirmDialog(
    title = "Delete deck?",
    message = "This cannot be undone.",
    confirmText = "Delete",
    onConfirm = { delete() },
    onDismiss = { close() },
    danger = true
)

DsPromptDialog(
    title = "New deck",
    placeholder = "Deck name",
    onConfirm = { name -> create(name) },
    onDismiss = { close() }
)
```

### Inputs (`DsInputs.kt`)

```kotlin
DsTextField(value = text, onValueChange = { text = it }, placeholder = "Enter name")
DsSearchField(value = query, onValueChange = { query = it }, placeholder = "Search…")
DsTextArea(value = json, onValueChange = { json = it }, height = 200.dp)
```

### Tabs & Selection (`DsSelect.kt`)

```kotlin
DsTabRow(tabs = listOf("All", "Due", "New"), selectedIndex = tab, onSelect = { tab = it })
DsChip(text = "Kanji", selected = filter == "kanji", onClick = { filter = "kanji" })
DsSelect(selected = theme, options = themes, onSelected = { theme = it }, labelOf = { it.name })
```

### Tags (`DsTag.kt`)

```kotlin
DsTagChip(label = "JLPT N3", colorHex = "#4CAF50", selected = true)
DsFlagBadge(label = "Important", colorHex = "#FF5722")
DsPriorityFlag(priority = 1, colorHex = "#F44336")
```

### Toasts (`DsToast.kt`)

```kotlin
toastHost.show("Card saved", kind = ToastKind.Success)
toastHost.show("Import failed", kind = ToastKind.Error)
toastHost.show("Sync in progress…", kind = ToastKind.Info)
```

### Page Layout (`DsPageShell.kt`)

```kotlin
DsPageShell(title = "Settings", subtitle = "Configure your workspace") {
    DsSection(title = "General") {
        DsSectionCard {
            DsToggle(checked = darkMode, onCheckedChange = { darkMode = it }, label = "Dark mode")
        }
    }
    DsSection(title = "Appearance") {
        DsSectionCard {
            DsSelect(selected = theme, options = themes, onSelected = { theme = it }, labelOf = { it.name })
        }
    }
}
```

### Loading & Error States

```kotlin
DsPageLoading()  // skeleton layout
DsPageEmpty(title = "No cards yet", message = "Import from Library", icon = Icons.Default.Inbox)
DsPageError(message = "Failed to load", onRetry = { retry() })
```

---

## Usage Rules

1. **Never hardcode colors** — use `surfaceColors()`, `accent()`, `DsSemantic`
2. **Never hardcode spacing** — use `DsSpacing` tokens
3. **Never hardcode radii** — use `DsRadius` tokens
4. **Never hardcode font sizes** — use `DsType` tokens
5. **Use `DsButton` variants** — don't create custom button styles
6. **Use `DsCard`** — don't create custom card containers
7. **Use `DsSearchField`** — don't create custom search inputs
8. **Use `DsPageShell`** — don't manually construct page layouts
9. **Use `DsEmptyState`** — every screen needs an empty state
10. **Use `DsSkeleton`** — loading states must show layout
11. **Honor reduced motion** — `DsMotion.duration()` respects config
12. **Use `DsIconSize`** — don't hardcode icon dimensions
13. **Use `DsBorder`** — don't hardcode border widths

---

## Theme Integration

Every component automatically adapts to the active theme:

- `surfaceColors()` returns theme-appropriate colors
- `accent()` returns theme-appropriate accent
- `DsSemantic` returns theme-appropriate status colors
- `DsMotion.duration()` respects reduced-motion settings
- `DsSpacing` scales with density setting

**No screen should contain:**
```kotlin
if (darkMode) Color.Black else Color.White
```

Theme values belong in the theme system.

---

## Adding New Components

1. Add to the appropriate `Ds*.kt` file
2. Use existing tokens (`DsSpacing`, `DsRadius`, `DsType`, `surfaceColors()`, `accent()`)
3. Support all interaction states (hover, press, disabled)
4. Support all themes (dark, light, sepia)
5. Add keyboard support where applicable
6. Use `DsMotion` spring presets for animations
7. Document in `DS_COMPONENTS.md`

---

## Adding New Screens

1. Use `DsPageShell` for page layout
2. Use `DsSection` for content grouping
3. Use `DsSectionCard` for visual grouping
4. Use `DsButton` variants for actions
5. Use `DsSearchField` for search
6. Use `DsEmptyState` for empty states
7. Use `DsPageLoading` for loading states
8. Use `DsPageError` for error states
9. Register in `AppModule.kt`
10. Add strings to `EnglishStrings` + `JapaneseStrings`
