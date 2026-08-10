# Kaiteyo Changelog

## v2.2.1 (Current) — Platform Polish & Rebranding Completion

### Added
- **Premium installer subsystem** (`installer/`) — new, fully decoupled from Gradle:
  - Windows: branded Inno Setup 6 installer (modern dynamic dark-mode wizard, install/upgrade/repair/modify, silent install, keep-or-remove uninstaller, file associations, launch-after-install, install-dir memory) + portable zip build
  - macOS: styled DMG with branded background artwork and drag-to-Applications; hardened-runtime signing + notarization + stapling pipeline (`entitlements.plist`)
  - Linux: AppImage with AppStream metadata + multi-size icon theme, deb builder, rpm spec, Flathub-ready Flatpak manifest, Snap wrapper
  - Shared: `common/version.json` single source of truth, update-feed + artifact-manifest JSON schemas, integrity verification gate, staging/bump/feed generation scripts, SVG→bmp/ico/icns/png brand asset generator
  - Docs: `installer/docs/{ARCHITECTURE,BUILD,SIGNING,RELEASE,UPDATES,FIRST_RUN}.md`
- **First-run onboarding** — `OnboardingWizard` (8 steps, live-applied: theme, accent, scaling, font, navigation, motion) is wired into `KaiteyoDesktopSuite`, gated once by the settings key `onboarding.completed`, re-openable from Settings → “Show onboarding again”; every step skippable, crash-safe completion via `AppState.completeOnboarding()`
- **Auto-update architecture** (`desktop/engine/updates/`) — `UpdateChannel` (stable/beta/nightly), `UpdateManifest` (feed schema v1), `HttpUpdateChecker`, sha256-verified `HttpUpdateDownloader`, `UpdateInstaller` interface, `UpdateService` coordinator with `StateFlow<UpdateState>`, `UpdatePolicy` rollback window
- **CI extended** — `build-all.yml` now produces the Inno EXE, MSI, portable zip, styled + notarized DMGs (arm/intel), deb, rpm and AppImage; `build-release.yml` stages + verifies artifacts and generates the stable update feed

### Added
- **Native window shell** — `KaiteyoWindow.kt`, `NativeWindowDrag.kt`, `WindowActions.kt`, `WindowStateStore.kt`:
  - 44dp custom title bar: K-logo opens the system menu (double-click closes), draggable wordmark, native-style window controls with hover states
  - Native OS dragging on Windows (`WM_NCLBUTTONDOWN`/`HTCAPTION`) and Linux (EWMH `_NET_WM_MOVERESIZE`) with a Compose fallback — 1:1 tracking, no rubber-banding
  - 8-zone invisible resize handles for the undecorated window (5dp edge strips + 10dp corners, 860×600 minimum); rounded corners flatten when maximized
  - Custom system menu (title-bar right-click / Alt+Space / logo / dock button / launchpad strip) with full keyboard navigation (arrows, Enter, Esc) — one shared implementation
  - Window size & position persisted to `~/.kaiteyo/window.json` (screen-validated on load, throttled saves, maximized states skipped) and included in profile backups
  - Keyboard access across the dock menus, launchpad tile grid, window-control strip, and launcher bubble chips (focus rings, arrow navigation, focus returned on dismiss)
- **Screenshot capture pipeline** — dev-only `--capture-state=<shell|menu|launchpad|strip>` (fixed 1200×800 window, self-exit dwell, never touches saved bounds), `scripts/capture-window-shell.sh` (per-OS window capture: xdotool/ImageMagick, screencapture, Win32), and the website's desktop screenshot gallery wired to `docs/screenshots/`
- **Unified statistics dashboard** — the card manager's Kanji.Dojo-era Stats/Heatmap tabs now render the single analytics dashboard via `embedded` mode; the legacy `StatisticsOverview` and its helpers were removed, and its unique values (Flagged count, average interval, average ease) were folded into the dashboard's Library Distribution section, computed from real data in `StatsOverviewV2`

### Changed
- **iOS project fully renamed** — `iosApp/KanjiDojoApp` → `iosApp/KaiteyoApp` (folder, Swift entry point, `xcodeproj`, `pbxproj`, shared scheme, and all build references)
- **Docs restructured by topic** — flat numbered docs moved into `architecture/`, `design/`, `branding/`, `roadmap/`, `features/`, `development/`, `contributing/`, `assets/`, `releases/`, `planning/`, `screenshots/`; `docs/README.md` is the new index; all internal links, `AGENTS.md`, `README.md`, and the website `documentation.json` updated
- **Root cleanup** — AI-session build logs, crash dumps, and scratch databases moved to `scratch/` (gitignored)
- **Desktop packaging rebranded** — snapcraft plug/paths (`kaiteyo-data`, `~/.kaiteyo`), flatpak metainfo changelog + URLs, AppImage metadata, and the snap launcher (now globs the jar) all Kaiteyo
- **Play Store changelog fixed** — stale `kanji-dojo` macOS note rewritten in `fastlane`
- **Stale editor metadata cleaned** — Inkscape `export-filename` paths in icon SVGs no longer reference the old project path

### Removed
- **Legacy attribution comments** — `Kanji.Dojo` references removed from stats dashboard, Home stats KDoc, and built-in deck catalog comments

### Verified
- **Rebranding audit across all platforms** — Android (manifest, resources, icons, activities), iOS (bundle id, Info.plist, assets, scheme), desktop (window title, AppImage/flatpak/snap metadata), and website (pages, FAQ, wiki, config) are fully Kaiteyo. Only legal attribution (fork history, original-author copyright, upstream repo) and functional references (`kanji-dojo-data-base-v15.sql` asset, App Store URL) remain — see `docs/branding/BRANDING.md` checklist.

## v2.0.0 — Premium Experience

### Added
- **Unified Library hub** — Replaces the old Kanji/Vocabulary split in Home:
  - Single Library tab in the Home tab bar (`HomeScreenTab.Library`)
  - Library hub with Sections (Stats, Study, Library, Review) and stat summary rows
  - Drill-down screens: Kanji Decks, Vocabulary, Word & Sentence Search
  - Old `LettersDashboard`/`VocabDashboard` tabs removed; default-tab preference remapped to Library
- **Persisted deck archive** — The `letter_deck.is_archived` / `vocab_deck.is_archived` columns (previously added by migration 13 but never wired) are now real:
  - Columns declared in the SQLDelight schema so fresh databases get them too
  - `updateLetterDeckArchived` / `updateVocabDeckArchived` queries
  - `LetterDeck`/`VocabDeck` now carry `isArchived`
  - `updateDeckArchived(id, isArchived)` on both repositories
  - Archive toggle in the Deck Edit → Save dialog (Edit mode only)
  - Follow-up (not yet implemented): hide archived decks from main lists + an "Archived" section to restore them
- **Theme Studio v2.0** — Complete rewrite with functional color editor:
  - Interactive HSV color wheel with drag-to-pick
  - RGB/HSL/HSV/HEX editors that sync together
  - 11 color targets (Primary, Secondary, Tertiary, Background, Surface, Text, etc.)
  - Recent & saved color palette with save/clear
  - Apply button that actually updates theme state
  - Gradient Editor tab (Linear/Radial/Angular, multiple stops, angle, intensity, opacity)
  - All String.format() replaced with KMP-safe custom formatting
  - Custom Color hue/saturation/lightness computed properties (KMP-compatible)
- **Floating Island Sidebar v2.0** — Premium redesign:
  - Drag to reposition (pointerInput detectDragGestures)
  - Snap-to-edge detection with spring animation
  - 9 dock states (Left, Right, Top, Bottom + 4 corners + Floating)
  - Resizable (drag bottom-right handle in floating mode)
  - Drag handle indicator for floating mode
  - Close/dock button for floating mode
  - Borderless elevated appearance with soft glow
  - Disabled `animateIntOffsetAsState` import (unused)
- **Brush Quality Engine** — Professional stroke pipeline:
  - Stroke smoothing (moving average low-pass filter)
  - Input prediction (extrapolate next points from velocity + acceleration)
  - Bezier smoothing (Catmull-Rom spline interpolation, 2-8 segments)
  - Velocity-based adaptive smoothing (slow=more smooth, fast=responsive)
  - Jitter reduction (tremor elimination with configurable threshold)
  - Pressure sensitivity support with configurable width range
  - Full `processStroke()` pipeline: jitter → smooth → velocity → predict → bezier
  - StrokePoint data class with pressure and timestamp
- **Branded Installer** — 8-screen premium installation wizard:
  - Welcome screen with feature showcase
  - Installation location picker with disk space info
  - Component selection (shortcuts, startup, file associations, auto-update)
  - Theme preview (4 themes with mini live preview)
  - Accent theme selector (all accent schemes)
  - Accessibility settings (UI scale 80-200%, font size selection, high contrast toggle)
  - Animated progress screen with step list
  - Completion screen with launch/cleanup options
- **Onboarding Wizard** — 8-step first-launch setup:
  - Step 1: Welcome with feature highlights
  - Step 2: Theme selection with live mini preview
  - Step 3: Accent theme picker with all schemes
  - Step 4: UI scaling (80-200% with live preview)
  - Step 5: Font size selection (Small/Medium/Large/Extra Large with sample text)
  - Step 6: Sidebar layout (position + mode with visual preview)
  - Step 7: Animation presets (None to Cinematic with descriptions)
  - Step 8: Finish with quick action cards
  - Skip All button, step progress indicator
- **Anki-like Features** (all files compile successfully):
  - FlagManagerScreen.kt — Flag management with stats and bulk operations
  - NoteEditorEnhanced.kt — Full markdown editor with toolbar and preview
  - KeyboardShortcutsPage.kt — VS Code-style shortcut manager
  - AnkiCardOperations.kt — Suspend/bury/forget/reposition/filtered decks/cram
  - SearchEngineImpl.kt — Universal search across all fields
  - DeckBrowserEnhanced.kt — Tree-based deck browser

### Changed
- Theme Studio: Custom Color tab now properly writes to theme state via accentScheme
- Custom Color tab: Separated RGB/HSL/HSV/HEX into distinct editors with LaunchedEffect callbacks
- Corner radius selector extracted into reusable component
- Sidebar mode/position selectors extracted into reusable components
- SliderWithLabel component for consistent slider UX
- Export tab replaced by inline theme management in Layout tab
- All `String.format()` calls replaced with KMP-safe `formatFloat()`
- All `Divider` → `HorizontalDivider` (Material3)

### Fixed
- Color picker now actually updates theme state (was using local remember only)
- Color wheel uses proper HSV space with correct indicator positioning
- No `String.format()` calls — eliminates KMP compilation error
- No `System.currentTimeMillis()` — uses incremental counter fallback
- No `Key.Digit0/Quote/Backtick` — uses alternative key constants
- No `MutableList` assignment from `.map()` — adds `.toMutableList()`
- All `@OptIn(ExperimentalMaterial3Api::class)` annotations added
- No duplicate function definitions

## v1.1.0

### Added
- Undecorated window with floating window controls
- Spring-based hover animations on window controls
- Window drag region using WindowDraggableArea
- Rounded corners (20dp) on window
- Theme system with BaseMode (Light/Dark/Oled)
- KaiteyoThemeState with composition locals
- Accent scheme selection with gradient support
- Glow, radius, animation, and density configuration
- SurfaceColors composition local
- Appearance Studio with live preview
- Signature theme (Lime + Orange)

### Changed
- Rebranded from Kanji Dojo to Kaiteyo (書いてよ)
- Complete documentation system in `/docs/`
- Professional repository structure with folder READMEs
- `../development/AI_CONTEXT.md` for AI assistant onboarding

### Fixed
- `animateColorAsState` import (now from `androidx.compose.animation`)
- `animateFloatAsState` import (now from `androidx.compose.animation.core`)
- `windowState.window!!.close()` → `window.close()` in FrameWindowScope
- Missing `@Composable` import
- Desktop app compilation (BUILD SUCCESSFUL)

## v1.0.0 (Initial Fork)

### Added
- Forked from Kanji Dojo
- Basic flashcard study system
- Spaced repetition algorithm
- JLPT decks (N5-N1)
- Stroke order diagrams
- Vocabulary lookup
- Cross-platform support (Desktop, Android, iOS)
- Koin dependency injection
- SQLDelight database
- Ktor HTTP client
- DataStore preferences
