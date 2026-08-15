# Kaiteyo (書いてよ) — UI System

> This is the **component catalog as implemented**. Every component listed here exists
> in code — `core/.../presentation/common/` for shared (multi-platform) components and
> `desktopApp/.../desktop/designsystem/` for the desktop `Ds*` system. Signatures are
> the real public APIs; where a spec below differs from the code, the code wins.

## Component philosophy

1. **Purposeful** — no decorative-only components (`DsEmptyState`, skeletons and
   tooltips exist because states need them).
2. **Consistent** — one `Ds*` component set across the whole desktop suite; shared
   Material3 components across all platforms.
3. **Responsive** — surfaces adapt to the window via `DsResponsive` width tiers and
   `adaptiveDialogWidth` (never fixed-size boxes floating mid-screen).
4. **Accessible** — keyboard navigable (`DsMenuPanel` grabs focus, ↑/↓ wrap, Esc
   dismisses), hover + focus states on every interactive element, reduced-motion
   honored (see `ANIMATION_SYSTEM.md`).
5. **Token-driven** — no hardcoded colors/spacing/radii in views; everything reads
   `DsSpacing`, `DsRadius`, `DsType`, `DsMotion`, `DsElevation`, `DsSemantic`
   (all scaled by the active theme config).

## Buttons (`DsButtons.kt`)

```kotlin
DsButton(text, onClick, modifier, kind = Primary, icon = null, enabled = true, compact = false)
```

`kind: DsButtonKind` — `Primary` (accent fill, onPrimary text), `Secondary`
(surfaceElevated fill), `Ghost` (transparent → surfaceInteractive on hover), `Danger`
(error fill), `AccentTint` (accent at 16% → 26% on hover).

Behavior: background `animateColorAsState` (160ms tween), scale spring
(pressed 0.97 / hovered 1.02, `spring(0.6, 500)`), disabled → muted text, `compact`
shrinks padding. Radius `DsRadius.Md`, padding `DsSpacing.Md` ×
`DsSpacing.Xs/Sm`.

```kotlin
DsIconButton(icon, onClick, modifier, contentDescription = null, tint = null, size = 34.dp, enabled = true)
```

36→34dp square icon button; hover background `surfaceInteractive`, hover tint = accent.
`DsTextButton(text, onClick, ...)` — text-only, accent-colored, 10% accent hover fill.
`DsButtonRow` — evenly spaced row (used by review grading).

## Cards & lists (`DsCards.kt`)

```kotlin
DsCard(modifier, elevated = false, onClick = null, content)
```

Surface or surfaceElevated fill, `DsRadius.Lg`, optional hover lift
(`DsElevation.Raised` shadow, 200ms tween) + 2dp accent top line when interactive
and hovered. `onClick != null` makes it interactive (hoverable, no ripple).

```kotlin
DsListItem(modifier, leading = {}, title, subtitle = null, trailing = {}, onClick = null)
```

40dp row (`padding(Lg, Md)`), hover → `surfaceInteractive`, title `DsType.Body` Medium
(1 line, ellipsis), subtitle `DsType.Caption` muted.

```kotlin
DsVirtualList(items, key, modifier, contentPadding, content)  // LazyColumn — 100k+ safe
DsFavoriteToggle(favorite, onToggle)                          // ★ star, yellow when set
DsChevron()                                                   // trailing chevron affordance
```

**Loading states** — `DsSkeleton(modifier, width, height, rounded)` pulses
0.45→1.0 alpha (900ms reverse tween, static under reduced motion); `DsSkeletonCard`
composes a card-shaped placeholder.

**Empty state** — `DsEmptyState(title, message, icon = null, action = {})`: accent
icon at 60%, title `DsType.BodyLarge`, message muted, optional action composable.

## Dialogs (`DsDialog.kt`)

```kotlin
DsDialog(title, onDismiss, modifier, compact = false, content)
```

Shared entrance: scale 0.94→1 spring + fade 180ms (`dialogEntranceLayer`). Width via
`adaptiveDialogWidth` — compact: 0.5 × window, 400–560dp; rich: 0.6 × window,
480–860dp. Panel `surfaceElevated`, `DsRadius.Xl`, padding `DsSpacing.Xl`, title
`DsType.Title` SemiBold. `usePlatformDefaultWidth = false` so width follows the window.

Specializations:
- `DsConfirmDialog(title, message, confirmText, onConfirm, onDismiss, danger = false)`
  — message + Cancel (text) + Confirm (primary or danger).
- `DsPromptDialog(title, placeholder, initialValue, onConfirm, onDismiss)` — single
  `DsTextField`, Save disabled while blank.
- `DsProgressDialog(title, message, progress, modifier)` — 6dp track + accent fill +
  percent label; used by transfers/imports.

## Inputs (`DsInputs.kt`)

```kotlin
DsTextField(value, onValueChange, modifier, placeholder = "", label = null, leadingIcon = null, singleLine = true)
```

Row on `surfaceElevated` (`DsRadius.Sm`), `BasicTextField` with accent cursor, 2dp
bottom hairline — accent when focused, border at 25% otherwise. `DsSearchField(...)` —
search icon + clear button (canonical search input, optional `autoFocus` via
`FocusRequester`). `DsNumericField(value, onValueChange, label = null)` — digit-only,
max 6 chars.

## Selects, tabs & chips (`DsSelect.kt`)

```kotlin
DsSelect(selected, options, onSelected, modifier, labelOf, icon = null)
```

Combo box: `surfaceElevated` row, chevron rotates 180° on open (spring 0.6/500),
border accent at 60% when expanded, `DropdownMenu` listing options (selected in
accent). `DsTabRow(tabs, selectedIndex, onSelect)` — segmented control, selected
segment accent 16% fill. `DsChip(text, selected, onClick, trailing = null)` — filter
pill, `DsRadius.Full`, selected accent 20% fill. `DsCategoryBadge(category)` — settings
category pill.

## Context menus (`DsMenu.kt`)

```kotlin
DsContextMenuHost(enabled, menuItems, content)   // right-click (or press) anchor
DsMenuItem(label, icon = null, shortcutLabel = null, checked = null, danger = false, enabled = true, onAction)
DsMenuPanel(menuItems, onDismiss)                // the panel itself
DsMenuItemRow(item, onClick, modifier)           // one row
DsMenuDivider()
```

Panel: 240dp, `surfaceInteractive`, keyboard access (grabs focus, ↑/↓ wrap skipping
disabled, Enter/Space activate, Esc dismiss), selected row highlighted accent 14%.
Danger rows tint #FF6B6B. `checked` shows an accent ✓; `shortcutLabel` right-aligned.

## Tags & flags (`DsTag.kt`)

```kotlin
DsTagChip(label, colorHex = "#808080", modifier, selected = false, onClick = null, removable = false, onRemove = null)
```

Colored pill: dot + label; background = color at 16% (25% hover, 35% selected);
text color auto-picks dark/light by `luminance()`. `DsFlagBadge(label, colorHex)` —
dot + label. `DsPriorityFlag(priority, colorHex)` — colored diamond + "P1". Hex parsing
via `parseHexColor` (accepts `#RRGGBB` / `#RRGGBBAA`).

## Toasts (`DsToast.kt`)

```kotlin
DsToastHost { show(text, kind = Info, durationMs = 3500); dismiss(msg) }
DsToastHostView(host, modifier, content)   // wraps content; host via LocalToastHost
```

Bottom-center stack; slide-up/fade entrance (240ms, speed-aware); kind colors:
Success (dark green bg, lime icon), Warning (dark amber), Error (dark red),
Info (surfaceInteractive). Icon does a small spring pop on appear (skipped under
reduced motion/instant speed).

## Toolbars & panels (`DsToolbar.kt`)

```kotlin
DsToolbar(title, modifier, subtitle = null, actions = {}, backIcon = null, onBack = null)
DsToolbarDivider()   // 1dp border@40%
DsSplitPane(modifier, vertical = true, initialFraction = 0.5, dividerWidth = 6.dp, onFractionChanged, first, second)
```

`DsSplitPane` is a resizable split with a draggable divider (accent on hover),
fraction clamped 0.1–0.9.

## Misc primitives (`DsMisc.kt`)

- `DsBadge(text, tint = accent)` — pill, accent 16% fill, `DsRadius.Full`.
- `DsStatTile(label, value, delta = null, deltaPositive = true)` — label
  (uppercase caption) + value (`DsType.Heading` Bold) + optional colored delta.
- `DsProgressBar(fraction, height = 6.dp, color = accent)` — rounded track/fill.
- `DsToggle(checked, onCheckedChange, label = null)` — Material Switch + optional
  label.
- `DsLink(text, onClick)` — accent text + arrow icon.
- `DsSectionHeader(title, subtitle = null, action = {})` — the standard view header.
- `DsNumberLabel(value)` — bold numeric label for grid density controls.

## Navigation (`NavShell.kt`, `WorkspaceShell.kt`)

- **Shared `NavShell`** — docked sidebar (any edge) or floating bubble; expanded/
  compact; auto-hide; item heights 40dp (desktop) / 48dp (phone); selected item =
  accent 14% fill + accent text; focus shows 2dp accent border; compact mode shows
  hover tooltips positioned per edge.
- **Desktop workspace** — `DsNavRail`/`DsNavBar` inside `DsDockIsland`; `DsTopBar`
  (title + subtitle + palette button "Search or jump to…" with Ctrl+K hint + settings
  + panel menu); `DsWorkspaceTabBar` for browser-style tabs; `DsDockColumn` for
  workspace panels; `DsFloatingLauncher`/launchpad (bubble mode).
- Below 720dp (`Breakpoints.CompactWindowWidth`): `CompactLayout` with `DsCompactNavBar`
  (bottom or top), hysteresis exit at 760dp.

## Interaction rules

| State | Visual change | Duration | Type |
|-------|---------------|----------|------|
| Hover | bg fill (surfaceInteractive / accent tint), scale 1.02 (buttons/cards), icon tint → accent | 160ms | tween / spring |
| Press | scale 0.97 (buttons), 0.97–0.96 (wizard cards) | — | spring |
| Focus | 2dp accent border | — | — |
| Disabled | muted text, no interactions | — | — |
| Loading | skeleton pulse 0.45↔1.0 | 900ms loop | tween reverse |

All durations honor `DsMotion` (Fast 120 / Normal 240 / Slow 380) × speed multiplier,
and reduced motion zeroes them.

## Keyboard navigation

- `Tab` / `Shift+Tab` move focus; Enter/Space activate; Esc closes menus/dialogs;
  ↑/↓ navigate menus and lists (wrapping).
- Review: `1–4` grade, `Space` reveal, `B` bury, `S` suspend, `R` retry,
  `Ctrl+Enter` skip, `Ctrl+Z` undo (see `ShortcutRegistry.kt` defaults).
- Palette: `Ctrl+K` open, `Esc` close, arrows + Enter to run.
- `Ctrl+B` toggles nav sidebar/floating; `Ctrl+Shift+N` cycles dock layouts
  (desktop).

## Related

- `docs/design/DESIGN_LANGUAGE.md` — the tokens behind these components
- `docs/design/THEME_SYSTEM.md` — where the tokens come from
- `docs/design/ANIMATION_SYSTEM.md` — the motion rules
- `docs/features/DESKTOP.md` — the desktop window shell
