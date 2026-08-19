# Kaiteyo Navigation Architecture

## Overview

Kaiteyo has exactly two navigation modes — **Sidebar** and **Floating** — both using the same `NavigationController`. Switching modes never recreates the application.

## Modes

### Sidebar

A structured dock on one of four screen edges (Left/Right/Top/Bottom). Two layouts:
- **Expanded**: icons + labels (220–340dp width, adaptive to window)
- **Compact**: icons only (64dp width)

### Floating

A movable launcher bubble with a full launchpad. The bubble:
- Drags freely with pointer delta
- Snaps to 12 anchor points on release
- Has hysteresis to prevent snap oscillation
- Supports left click, right click, touch, keyboard

## Architecture

```
NavShell
├── CompositionLocalProvider
│   ├── LocalNavigationSettings
│   ├── LocalDebugSettings
│   └── LocalHomeNavigationState
├── AdaptiveNavigation
│   ├── Row/Column layout (depends on edge)
│   │   ├── DockedSidebar (animated width/height)
│   │   └── Content (weighted sibling)
│   └── AnimatedVisibility
│       └── BubbleLauncher (floating mode)
├── PageNameIndicator (debug overlay)
└── DebugPanel (debug overlay)
```

## Layout Safety

The dock occupies REAL layout space — never a padding hack:
- Content is a weighted sibling that always gets remaining space
- Sidebar physically cannot swallow the window
- No negative padding possible
- Mode transitions animate between valid geometries

### NavGeometry

All measurements derive from one model:
- `adaptiveSidebarWidth()`: 16–22% of window, clamped 208–384dp
- `dockedBarSize()`: width (vertical) or height (horizontal)
- `contentReserve()`: docked region + system inset, always non-negative

Hard safety: docked region can never exceed 50% of window.

## Snap Points (Floating)

12 anchor points (3 per edge):
- Top: TopLeft, TopCenter, TopRight
- Bottom: BottomLeft, BottomCenter, BottomRight
- Left: LeftTop, LeftCenter, LeftBottom
- Right: RightTop, RightCenter, RightBottom

Corners are deduplicated across adjacent edges.

## Settings

### NavigationSettings

```kotlin
data class NavigationSettings(
    val mode: NavigationMode,           // Sidebar | Floating
    val rememberPreviousMode: Boolean,
    val defaultMode: NavigationMode,
    val lastMode: NavigationMode?,
    val animationsEnabled: Boolean,
    val animationDurationMs: Int,       // 260ms default
    val desktopEdge: SidebarPosition,   // Left | Right | Top | Bottom
    val sidebarExpansion: SidebarExpansion, // Expanded | Compact
    val snapPoint: BubbleSnapPoint,
    val snapOffsetX: Int,
    val snapOffsetY: Int,
    val bubble: BubbleSettings,
    val launchpad: LaunchpadSettings,
    val phone: PhoneNavigationSettings,
    val sidebar: SidebarSettings,
    val accessibility: AccessibilitySettings,
    val showPageName: Boolean
)
```

### BubbleSettings

- Size (56dp default)
- Icon size (26dp)
- Snap sensitivity (80)
- Safe margin (12dp)
- Hold duration (480ms)
- Auto-hide (20s default)
- Hover reveal (true)

### SidebarSettings

- Expanded width index (260dp default)
- Icon size (22dp)
- Compact spacing (8dp)
- Label visibility (Always | Hover | Never)

## Keyboard Shortcuts

- **Ctrl+B**: Toggle Sidebar ↔ Floating
- **Ctrl+Shift+F**: Open universal search
- **Escape**: Close overlays, go back
- **/**: Focus search
- **↑/↓**: Navigate lists
- **Enter**: Open selected item

## Transitions

- Sidebar width animates with spring physics
- Mode switching fades in/out
- Resize uses snap (no animation during drag)
- Page transitions: Crossfade, Slide, FadeThrough, Scale (configurable)

## Phone Adaptation

Phone only supports Top/Bottom bar placement:
- Bar height: 52dp (touch-friendly)
- Compact item size: 48dp (Material minimum)
- Mode/compact controls in settings, not in bar

## Debug Overlay

When `showPageName` is enabled:
- Top-right pill: Page name + analytics code
- Bottom-left panel: Page, Route, Panel, Theme, Navigation mode
- Copy debug info action
- Optional: FPS, viewport, window state

## State Persistence

Navigation settings are persisted as JSON in `PreferencesContract.AppPreferences.navSettingsJson`. Legacy mode names are migrated to the two-mode model.

## Screen Registry

Every major screen declares a `PageIdentity`:
- `id`: stable identifier
- `name`: human-readable name
- `route`: URL-like path
- `panel`: optional sub-surface

`PageRegistry` maps analytics codes to human-readable names for the debug overlay.

## Home Navigation

Home has 5 tabs:
1. GeneralDashboard (default)
2. Library
3. Stats
4. Search
5. Settings

Tab state is persisted via `PreferencesDefaultHomeTab`.

## Back Navigation

Back behavior preserves the navigation stack:
- Home → Search → 食べる → 食
- Back: 食 → 食べる → Search → Home

Main destinations maintain a stack; `navigateBack()` pops the top.

## Deep Links (Future)

Planned URL scheme:
- `kaiteyo://kanji/食`
- `kaiteyo://word/食べる`
- `kaiteyo://sentence/...`
- `kaiteyo://lesson/...`

Enables Anki integration, browser links, external apps.
