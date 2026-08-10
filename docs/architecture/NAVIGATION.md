# Kaiteyo (書いてよ) — Navigation System

## Overview

The navigation system is a unified, highly customizable desktop-OS-style shell
(`NavShell`) that replaces the legacy fixed sidebar/topbar/bottombar chrome.
It is implemented in common code and shared across platforms.

- **Entry point:** `NavShell` in
  `core/src/commonMain/kotlin/ua/syt0r/kanji/presentation/common/nav/NavShell.kt`
- **Wiring:** `MainScreen` wraps `MainNavigation` inside `NavShell`
  (`MainScreen.kt`). On portrait/phone layouts NavShell is a pass-through and
  the legacy `HomeScreenUI` chrome remains active.
- **State sharing:** NavShell creates the `HomeNavigationState` and provides it
  through `LocalHomeNavigationState`; `HomeScreen` consumes the provided state
  instead of creating its own, so shell tab buttons and page content stay in
  sync.

## Layout Model

`LayoutConfig` (in `Theme.kt`) is the single source of truth for layout. It is
exposed to the whole app via `LocalLayoutConfig` / `LocalKaiteyoThemeState`.

| Field | Type | Purpose |
|---|---|---|
| `sidebarMode` | `SidebarMode` | `Expanded`, `Compact`, `IconsOnly`, `FloatingIsland`, `Docked`, `AutoHide` |
| `sidebarPosition` | `SidebarPosition` | `Left`, `Right`, `Top`, `Bottom` |
| `autoHide` | `NavAutoHide` | `Never`, `Always`, `FullscreenOnly`, `Smart` |
| `collapsed` | `Boolean` | Collapse expanded mode to icon-only strip |
| `panelWidth` | `Dp` | Width of vertical panels / floating island (default 260dp) |
| `panelHeight` | `Dp` | Height of horizontal strips / floating island (default 56dp) |
| `floatingOffset` | `DpOffset` | Floating island position (clamped to window) |
| `accentIndex` | `Int` | Reserved for accent-tinted chrome |

## Modes

- **Expanded** — full-width/height strip docked to the edge with section
  headers and labels; resizable via a 5dp drag strip on the inner edge.
- **Compact** — icon-only strip (60dp) with hover tooltips.
- **IconsOnly** — same as Compact.
- **FloatingIsland** — detached panel, draggable anywhere in the window,
  resizable via the bottom-end handle; accent-tinted shadow; honors
  `transparencyEnabled` / `glassOpacity`.
- **Docked** — macOS-style centered dock with spring scale-on-hover (1.25x).
- **AutoHide** — strip slides in from the edge; a 8dp reveal strip is shown at
  the edge while hidden.

## Auto-Hide

- `Ctrl+B` toggles the strip anywhere.
- Hovering the strip keeps it revealed; leaving the panel hides it again.
- `FullscreenOnly` uses `LocalWindowPlacement` (provided by `KaiteyeWindow`
  from the real `WindowPlacement` state: `Floating` vs `Maximized`).

## Persistence

`NavLayoutManager` (`nav/NavLayoutManager.kt`) loads and saves the layout via
DataStore:

- Keys: `nav_sidebar_mode`, `nav_sidebar_position`, `nav_auto_hide`,
  `nav_collapsed`, `nav_width`, `nav_height`, `nav_floating_offset_x`,
  `nav_floating_offset_y`, `nav_accent_index`.
- NavShell loads the persisted layout into `themeState.layoutConfig` on start
  and mirrors every layout change back (`syncFrom`), so Appearance Studio,
  drags and resizes persist automatically.
- Settings added to `PreferencesContract.AppPreferences` /
  `AppPreferences.kt` (`enableBackup = false`).

## Navigation Model

- `NavEntry` — id, label (composable lambda), icon or `iconContent`, selected,
  enabled, onClick.
- `NavSection` — optional header + entries.
- `buildNavSections` builds three sections:
  - **Home** — all `HomeScreenTab.VisibleTabs` (icon + label from
    `titleResolver`), navigates to Home first, then switches tab.
  - **Features** — DeckBrowser, TextAnalysis, StatisticsDashboard.
  - **System** — AppearanceStudio, Backup, Sync, Sponsor (only when
    `PlatformFeature.supported`), About.

## Design Tokens

`NavTokens` centralizes all navigation sizing:

- Elevations: strip 6dp, dock 16dp, floating 24dp.
- Radii: from `Dimens` scale, multiplied by `RadiusConfig.globalMultiplier`
  at runtime (`scaledRadius`).
- Item heights, icon sizes, strip widths, edge margins.

## Known Limitations (next iterations)

- `Smart` auto-hide currently behaves like `Always`.
- Sync indicator / sponsor button from the legacy home sidebar are not yet
  surfaced in the shell chrome.
- Top/Bottom positions still overlap the 44dp custom titlebar drag region.
