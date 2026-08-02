# Kaiteyo (書いてよ) — Master TODO

## Priority Legend
- 🔴 **P0 (Critical)** — Blocks all other work
- 🟡 **P1 (High)** — Required for next release
- 🟢 **P2 (Medium)** — Planned for upcoming release
- 🔵 **P3 (Low)** — Nice to have

---

## 🔴 P0 — Critical (Must Fix Now)

### Window Experience
- [ ] Fix window drag region — only top 44dp should be draggable, NOT the entire application
- [ ] Ensure interactive UI components (buttons, lists, settings) are NEVER draggable
- [ ] Fix animation performance — target 60 FPS, no stuttering
- [ ] Fix theme switching smoothness
- [ ] Fix hover animation consistency
- [ ] Fix resize behavior — no panel jumping, no spacing changes, no broken animations

### Design Quality
- [ ] Fix inconsistent spacing throughout UI
- [ ] Ensure rounded panels feel intentional
- [ ] Fix component alignment
- [ ] Establish clear visual hierarchy
- [ ] Make sidebar feel like an elevated floating object, not attached to window

### Settings
- [ ] Reorganize Appearance settings into professional Appearance Studio
- [ ] Remove randomly placed appearance options

---

## 🟡 P1 — High (Next Release: v1.2)

### Floating Sidebar
- [ ] Snap to valid dock positions (Windows 11 Snap Layouts style)
- [ ] Mobile: Top, Bottom only with snap
- [ ] Sync indicator / sponsor button surfaced in shell chrome (currently only
      on portrait chrome)

### Theme System
- [ ] Implement all 8 built-in themes:
  - [ ] Signature (default) — Lime + Orange
  - [ ] OLED Black — True black
  - [ ] Dark Gray — Softer dark
  - [ ] Light — Clean light mode
  - [ ] Reading — Warm paper tones
  - [ ] Cotton Candy — Pastel
  - [ ] Ocean — Cool blue
  - [ ] Forest — Earthy green
- [ ] Signature theme MUST use BOTH lime (#C2FC8B) and orange (#FEAB57) distributed intelligently
- [ ] Reading theme: warm paper colors, cream backgrounds, low eye strain, ink-like text

---

## 🟢 P2 — Medium (v1.3)

### Theme Studio
- [ ] "+" button opens Theme Studio
- [ ] Start from any built-in theme as base
- [ ] Color editor with multiple color wheels (RGB, HSV, HSL, HEX)
- [ ] Opacity control
- [ ] Gradient editor with multiple stops
- [ ] Live preview showing: sidebar, cards, buttons, lists, dialogs, navigation
- [ ] Theme import/export as JSON
- [ ] Custom theme presets saved locally

### Motion Studio
- [ ] Animation presets: No Animation, Minimal, Standard, Smooth, Bouncy
- [ ] Each preset changes: duration, spring stiffness, bounce, fade timing, hover animations
- [ ] Preview animations before applying

### Layout Studio
- [ ] Panel spacing adjustment
- [ ] Corner radius control
- [ ] Density modes: Compact, Comfortable, Spacious
- [ ] Transparency/blur control
- [ ] Surface elevation control

---

## 🔵 P3 — Low (Future)

### Polish
- [ ] Page transitions with shared element animations
- [ ] Dialog/modal entrance/exit animations
- [ ] List item animations
- [ ] Native window shadows
- [ ] Glass morphism effects
- [ ] Parallax scrolling

### Accessibility
- [ ] Keyboard navigation (Tab, Shift+Tab, Enter, Escape, Arrow keys)
- [ ] Screen reader support
- [ ] Reduced motion option
- [ ] High contrast mode
- [ ] Font size adjustment

### Internationalization
- [ ] Japanese UI option
- [ ] Community-contributed translations

### Performance
- [ ] Lazy loading for large lists
- [ ] Image caching
- [ ] Memory optimization
- [ ] Startup time reduction
- [ ] Compose compiler metrics

---

## Completed ✅

- [x] Build system working (Gradle, Compose Multiplatform)
- [x] Desktop app compiles and runs
- [x] Basic window with undecorated chrome
- [x] Floating window controls (minimize, maximize, close)
- [x] Window drag region (basic implementation)
- [x] Rounded window corners (20dp)
- [x] Signature theme with lime (#C2FC8B) and orange (#FEAB57)
- [x] Theme system with BaseMode (Light/Dark/Oled)
- [x] Appearance settings with theme mode and accent selection
- [x] Documentation structure (/docs with 11+ files)
- [x] Hover animations (scale, glow, color)
- [x] Spring-based physics for animations
- [x] Unified NavShell (common) with Expanded / Compact / IconsOnly /
      FloatingIsland / Docked / AutoHide modes
- [x] Nav positions: Left, Right, Top, Bottom
- [x] Auto-hide with edge reveal strip and Ctrl+B toggle
- [x] Floating island: drag reposition (clamped to window), accent-tinted
      shadow, glass transparency
- [x] Dock mode: centered, spring scale on hover
- [x] Resizable docked strips (5dp drag strip) and floating island
- [x] Layout persistence via DataStore (NavLayoutManager)
- [x] NavShell layout doc (docs/17_NAVIGATION_SYSTEM.md)

---

## Navigation & Workflow Redesign (delivered, compiled)

The desktop suite's navigation was reworked around desktop productivity. All of
this is wired into settings, the command palette, keyboard shortcuts and toasts,
and `:desktopApp:compileKotlinJvm` is green.

- **Adaptive edge navigation** — sidebar is a real edge layout, not a rotated
  bar. `navigation.position` (left/right/top/bottom) and `navigation.collapsed`
  persist in settings. A `DsPositionPicker` (icon popup) moves the rail between
  edges; a collapse button toggles 232dp ⇄ 64dp rail.
- **Compact pill nav** — below 720dp width the app switches to `DsNavBar`
  (dedicated top/bottom pill rows), keeping every group reachable.
- **Floating launcher** — `navigation.mode` (traditional / floating / both)
  controls whether the docked rail, the bottom-right `DsFloatingLauncher`
  (translucent glass FAB + menu), or both are shown.
- **Persistent workspace panels** — `PanelKind` (Dictionary, Kanji Browser,
  Statistics, Deck Browser, Theme Studio, Search) open as a 360dp right dock
  (`DsDockColumn`) or as draggable/resizable floating windows
  (`DsFloatingPanelWindow`). Layout (kind, placement, x/y/size) persists as JSON
  under the `workspace.panels` setting key and survives restarts. Opened via the
  panel menu button or palette commands ("Toggle … panel").
- **Deck action surface** — `CollectionsView` details gained a full action rail:
  Study, Browse (up to 200 cards), Edit (dialog), Statistics, Duplicate, Export
  (JSON to clipboard), Archive/Restore, and Delete (danger). Archived decks sink
  to an "ARCHIVED" section with one-click restore. Backed by new
  `CollectionStore` ops: `rename`, `duplicate`, `toggleArchived`, `archived`,
  `export`.
- **Browser upgrades** — selection mode (checkboxes in grid/list), select-all,
  bulk toolbar (Tag, Flag, Favorite, Suspend, Reset, Delete), sort menu
  (Default/Character/Meaning/Status/Interval/Due/Tags), and "Review these N"
  which starts a review from the exact query.

## Desktop Suite Prototype (self-contained, compiled)

A standalone desktop-first study suite lives in
`desktopApp/src/jvmMain/kotlin/ua/syt0r/kanji/desktop/` — own engines, design
system and full view layer, independent of the Koin app. Launch via
`desktopSuiteMain()` in `desktopApp/SuiteMain.kt`; it reuses the borderless
`KaiteyoWindow` shell around `KaiteyoDesktopSuite`.

- **Engines** (`engine/`): SRS scheduler (FSRS/SM-2 hybrid), review queue
  (bury/suspend/undo/retry/forget/reschedule), search, saved filters, smart
  collections, statistics (heatmap/goals/weak spots), activity log, sync
  (provider abstraction + memory transport), themes (JSON studio), transfer
  (JSON/CSV/TSV/TXT import-export), plugins, shortcuts, settings.
- **Design system** (`designsystem/`): `Ds*` components reading core theme
  locals (`LocalSurfaceColors`, `LocalKaiteyoAccent`, `DsSpacing/Radius/Type`).
- **Views** (`ui/`): Dashboard, Browser, Review, Collections, Tags/Flags,
  Statistics, Activity Log, Import/Export, Sync, Shortcuts, Plugins, Theme
  Studio, Settings — wired by `KaiteyoWorkspace` (sidebar + top bar + global
  shortcuts + command palette + toasts).
- **State**: single `AppState` facade; `DemoData` seeds ~74 curated kanji with
  realistic SRS spread + 180 days of summaries.

Status: compiles clean (`:desktopApp:compileKotlinJvm`). The navigation/workflow
redesign above is implemented and integrated. Remaining polish: deprecation
warnings for non-AutoMirrored icons (cosmetic), and any runtime behavior checks
via `desktopSuiteMain()`.
