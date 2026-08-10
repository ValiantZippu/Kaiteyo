# Kaiteyo — Current Issues

This is a living document. Add issues as they are discovered, mark them as fixed when resolved.

## 🔴 P0 — Critical (Blocking Usability)

### Desktop Window

- [x] **Window dragging grabs the whole UI** — Addressed in `KaiteyoWindow.kt`: drag is scoped to a dedicated 44dp title-bar region only; the rest of the UI is no longer draggable. *(Pending compile + runtime verification.)*
- [x] **Interactive components are draggable** — Addressed — `WindowDraggableArea` is scoped to the title bar only, behind the window controls. *(Pending compile + runtime verification.)*
- [ ] **Animation stuttering** — Hover animations, theme switching, and window movement are not smooth. Target 60 FPS.
- [ ] **Resize glitches** — Panels jump, spacing changes unexpectedly, animations break during window resize.
- [ ] **Hover animations are inconsistent** — Some elements animate on hover, others don't. The behavior varies across components.

### Design

- [ ] **Inconsistent spacing** — Different components use different padding/margin values. No adherence to the 4dp grid.
- [ ] **Poor component alignment** — Elements in cards, lists, and settings panels don't align properly with each other.
- [ ] **No clear visual hierarchy** — It's hard to distinguish primary, secondary, and tertiary content at a glance.
- [ ] **Sidebar looks attached** — The sidebar feels like it's glued to the window edge. It should be a floating island with elevation and shadow.
- [ ] **Rounded panels don't feel intentional** — Some elements are rounded, some are square. No consistent corner radius strategy.

### Settings

- [ ] **Appearance options are disorganized** — Settings feel randomly placed with no logical grouping.
- [ ] **No Appearance Studio** — The current settings are basic. Need full Theme Studio with color editor, gradient editor, live preview.

## 🟡 P1 — High (v1.2)

### Floating Sidebar

- [ ] **Implement floating island design** — Sidebar should not attach to window edge. It should float with rounded corners, elevation, shadow, and glow.
- [ ] **Dock positions** — Support Left, Right, Top, Bottom, and Floating modes.
- [ ] **Auto-hide** — Sidebar hides when not in use, reveals on hover or click.
- [ ] **Collapse/expand animation** — Use spring animations for smooth expand/collapse.
- [ ] **Snap to valid positions** — Similar to Windows 11 Snap Layouts.

### Theme System

- [ ] **Missing 7 built-in themes** — Only Signature is partially implemented. Need: OLED, Dark Gray, Light, Reading, Cotton Candy, Ocean, Forest.
- [ ] **Signature theme needs color distribution** — Currently too lime-heavy. Need to distribute lime (#C2FC8B) and orange (#FEAB57) intelligently across buttons, navigation, cards, progress, highlights, glows, gradients.
- [ ] **Reading theme** — Warm paper colors, cream backgrounds, low eye strain, ink-like text.
- [ ] **Gradient distribution** — Lime→Orange gradients should appear on selected nav, progress bars, accent cards, hero sections, theme previews.
- [ ] **Glow effects need enhancement** — Increase default glow intensity. Add animated glow for buttons, cards, navigation, window controls.

### Appearance Studio

- [ ] **Color editor** — RGB, HSV, HSL, HEX color pickers.
- [ ] **Gradient editor** — Multiple stops, angle control, intensity slider.
- [ ] **Live preview** — Real-time preview of sidebar, cards, buttons, dialogs, lists, navigation.
- [ ] **Theme import/export** — Export theme as JSON, import from JSON file.
- [ ] **Animation controls** — Presets: None, Minimal, Standard, Smooth, Bouncy. Speed control. Spring stiffness tuning.
- [ ] **Layout controls** — Sidebar position, density modes (Compact/Comfortable/Spacious), corner radius, transparency, blur, elevation.

## 🟢 P2 — Medium (v1.3)

### Motion Studio

- [ ] Animation presets with preview
- [ ] Per-component animation control
- [ ] Reduced motion support

### Layout Studio

- [ ] Sidebar position (Left, Right, Top, Bottom)
- [ ] Density modes
- [ ] Corner radius slider
- [ ] Transparency/blur controls
- [ ] Surface elevation controls
- [ ] Compact/comfortable/spacious modes

### Branding

- [x] Replace remaining "Kanji Dojo" references in user-facing strings
- [x] Update desktop app title/installer name
- [x] Update GitHub metadata and README
- [x] Update about page
- [x] Update splash screen
- [x] Rename iOS project `KanjiDojoApp` → `KaiteyoApp` (folder, xcodeproj, pbxproj, scheme)
- [x] Rebrand desktop packaging (AppImage, flatpak metainfo, snapcraft plug/paths, launcher)
- [x] Fix stale Play Store changelog (`fastlane`) and editor metadata in icon SVGs
- [x] Repo-wide branding sweep — only legal attribution and functional refs remain
  (see `docs/branding/BRANDING.md` checklist)

## 🔵 P3 — Low (Future)

### Performance

- [ ] Profile and optimize recompositions
- [ ] Lazy loading for long lists
- [ ] Image caching
- [ ] Reduce APK/MSI size

### Accessibility

- [ ] Keyboard navigation
- [ ] Screen reader support
- [ ] High contrast mode
- [ ] Font size adjustment

## ✅ Recently Fixed

- [x] **Custom window chrome** — Custom 44dp title bar with drag region, window controls, double-click maximize/restore, and right-click system menu; rounded corners flatten when maximized. (See `KaiteyoWindow.kt`.) **Note:** code written but not yet build-verified — run `:desktopApp:compileKotlinJvm` before treating as resolved.
- [x] **iOS project fully renamed to Kaiteyo** — `KanjiDojoApp` folder, Swift entry point, `xcodeproj`, `project.pbxproj`, and shared scheme all renamed with no leftover references.
- [x] **Docs restructured by topic** — Numbered root docs moved into topical folders; `docs/README.md` is the new index; internal links, `AGENTS.md`, `README.md`, and website `documentation.json` all updated.
- [x] **Root directory cleanup** — Build logs, crash dumps, and scratch databases moved to gitignored `scratch/`.
- [x] **Persisted deck archive flag** — `is_archived` columns on `letter_deck`/`vocab_deck` (previously dead, added only by migration 13) are now in the SQLDelight schema, backed by `updateDeckArchived` repository methods and a toggle in the Deck Edit save dialog. **Follow-up:** filter archived decks from the main dashboard lists and add an "Archived" section to restore them (currently archived decks stay visible everywhere).
- [x] **Unified Library hub** — Home now has a single Library tab (replaces Kanji/Vocabulary split). Includes hub with Sections + stat summary rows and drill-down screens (Kanji Decks, Vocabulary, Word & Sentence Search). Old default-tab preference remapped.
- [x] **Import error: `animateColorAsState`** — Fixed by importing from `androidx.compose.animation`
- [x] **Import error: `animateFloatAsState`** — Fixed by importing from `androidx.compose.animation.core`
- [x] **`windowState.window!!.close()` error** — Fixed by using `window.close()` in FrameWindowScope
- [x] **Missing `@Composable` import** — Added `import androidx.compose.runtime.Composable`
