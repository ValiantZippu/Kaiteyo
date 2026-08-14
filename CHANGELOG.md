# Kaiteyo (書いてよ) Changelog

All notable changes to Kaiteyo are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.0.0/) conventions, grouped by:

- **Added** — new features
- **Changed** — changes to existing behavior
- **Fixed** — bug fixes
- **Removed** — removed functionality
- **Security** — security-relevant changes

## Unreleased

### Changed
- **Documentation restructured** — the repository documentation was reorganized into a
  professional, navigable structure (`docs/` with topic areas: architecture, data,
  integrations, user-guide, platform, security, legal, testing, releases, planning).
  Architecture Decision Records moved to `docs/architecture/decisions/`; the changelog
  moved to the repository root; the root README was redesigned with accurate per-feature
  status. Repository root was cleaned of dev-session scratch files (crash dumps, build
  logs, paste files, one-off scripts).

## v2.2.1 (Current) — Platform Polish & Rebranding Completion

### Added
- **Premium installer subsystem** (`installer/`) — new, fully decoupled from Gradle:
  - Windows: branded Inno Setup 6 installer (modern dynamic dark-mode wizard,
    install/upgrade/repair/modify, silent install, keep-or-remove uninstaller, file
    associations, launch-after-install, install-dir memory) + portable zip build
  - macOS: styled DMG with branded background artwork and drag-to-Applications;
    hardened-runtime signing + notarization + stapling pipeline (`entitlements.plist`)
  - Linux: AppImage with AppStream metadata + multi-size icon theme, deb builder, rpm
    spec, Flathub-ready Flatpak manifest, Snap wrapper
  - Shared: `common/version.json` single source of truth, update-feed + artifact-manifest
    JSON schemas, integrity verification gate, staging/bump/feed generation scripts,
    SVG→bmp/ico/icns/png brand asset generator
  - Docs: `installer/docs/{ARCHITECTURE,BUILD,SIGNING,RELEASE,UPDATES,FIRST_RUN}.md`
- **First-run onboarding** — `OnboardingWizard` (8 steps, live-applied: theme, accent,
  scaling, font, navigation, motion) wired into `KaiteyoDesktopSuite`, gated once by the
  settings key `onboarding.completed`, re-openable from Settings, every step skippable,
  crash-safe completion via `AppState.completeOnboarding()`
- **Auto-update architecture** (`desktop/engine/updates/`) — `UpdateChannel`
  (stable/beta/nightly), `UpdateManifest` (feed schema v1), `HttpUpdateChecker`,
  sha256-verified `HttpUpdateDownloader`, `UpdateInstaller` interface, `UpdateService`
  coordinator with `StateFlow<UpdateState>`, `UpdatePolicy` rollback window
- **CI extended** — `build-all.yml` now produces the Inno EXE, MSI, portable zip, styled +
  notarized DMGs (arm/intel), deb, rpm and AppImage; `build-release.yml` stages + verifies
  artifacts and generates the stable update feed
- **Native window shell** — `KaiteyoWindow.kt`, `NativeWindowDrag.kt`, `WindowActions.kt`,
  `WindowStateStore.kt`:
  - 44dp custom title bar: K-logo system menu, draggable wordmark, native-style window
    controls with hover states
  - Native OS dragging on Windows (`WM_NCLBUTTONDOWN`/`HTCAPTION`) and Linux (EWMH
    `_NET_WM_MOVERESIZE`) with a Compose fallback
  - 8-zone invisible resize handles for the undecorated window; rounded corners flatten
    when maximized
  - Custom system menu (title-bar right-click / Alt+Space / logo / dock button) with full
    keyboard navigation
  - Window size & position persisted to `~/.kaiteyo/window.json` (screen-validated on
    load, throttled saves) and included in profile backups
- **Screenshot capture pipeline** — dev-only `--capture-state` flag,
  `scripts/capture-window-shell.sh`, and the website's desktop screenshot gallery wired to
  `docs/screenshots/`
- **Unified statistics dashboard** — the card manager's stats/heatmap tabs now render the
  single analytics dashboard via `embedded` mode; legacy `StatisticsOverview` removed and
  its unique values folded into the dashboard's Library Distribution section

### Changed
- **iOS project fully renamed** — `iosApp/KanjiDojoApp` → `iosApp/KaiteyoApp` (folder,
  Swift entry point, `xcodeproj`, `pbxproj`, shared scheme, all build references)
- **Docs restructured by topic** — flat numbered docs moved into topic folders;
  `docs/README.md` is the new index; internal links, `AGENTS.md`, `README.md`, and the
  website `documentation.json` all updated
- **Desktop packaging rebranded** — snapcraft plug/paths, flatpak metainfo changelog +
  URLs, AppImage metadata, and the snap launcher all Kaiteyo
- **Play Store changelog fixed** — stale `kanji-dojo` macOS note rewritten in `fastlane`

### Removed
- **Legacy attribution comments** — `Kanji.Dojo` references removed from the stats
  dashboard, Home stats KDoc, and built-in deck catalog comments

### Verified
- **Rebranding audit across all platforms** — Android, iOS, desktop, and website are fully
  Kaiteyo; only legal attribution (fork history, original-author copyright, upstream repo)
  and functional references (`kanji-dojo-data-base-v15.sql` asset, App Store URL) remain —
  see `docs/branding/BRANDING.md`

## v2.0.0 — Premium Experience

### Added
- **Unified Library hub** — single Library tab replacing the Kanji/Vocabulary split: hub
  with Sections + stat summary rows, drill-down screens (Kanji Decks, Vocabulary, Word &
  Sentence Search); default-tab preference remapped
- **Persisted deck archive** — `is_archived` columns on `letter_deck`/`vocab_deck`
  (previously dead, added by migration 13) are now real: declared in the SQLDelight
  schema, backed by `updateDeckArchived` repository methods, and toggleable from the Deck
  Edit → Save dialog
- **Theme Studio v2.0** — complete rewrite with a functional color editor: interactive HSV
  color wheel, synchronized RGB/HSL/HSV/HEX editors, 11 color targets, palette, gradient
  editor (Linear/Radial/Angular, multiple stops, angle, intensity, opacity), live preview
- **Floating Island Sidebar v2.0** — drag to reposition, snap-to-edge detection with
  spring animation, 9 dock states, resizable in floating mode, borderless elevated
  appearance with soft glow
- **Brush Quality Engine** — stroke smoothing (moving-average low-pass), input prediction
  (velocity + acceleration), Bezier smoothing (Catmull-Rom spline), velocity-based adaptive
  smoothing, jitter reduction, pressure sensitivity; full `processStroke()` pipeline
- **Branded Installer** — 8-screen premium installation wizard (welcome, location,
  components, theme, accent, accessibility, progress, completion)
- **Onboarding Wizard** — 8-step first-launch setup with live previews and skip-all
- **Anki-like features** — flag manager, enhanced note editor, keyboard shortcuts page,
  suspend/bury/forget/reschedule operations, universal search, tree-based deck browser

### Changed
- Theme Studio custom-color tab now writes to theme state via `accentScheme`
- Corner radius selector and sidebar mode/position selectors extracted into reusable
  components
- `SliderWithLabel` component for consistent slider UX
- All `String.format()` calls replaced with KMP-safe `formatFloat()`
- All `Divider` → `HorizontalDivider` (Material3)

### Fixed
- Color picker now actually updates theme state (was using local `remember` only)
- No `String.format()` / `System.currentTimeMillis()` / `Key.Digit0`-style usages (KMP
  compilation errors)
- Missing `@OptIn(ExperimentalMaterial3Api::class)` annotations added
- No duplicate function definitions

## v1.1.0

### Added
- Undecorated window with floating window controls
- Spring-based hover animations on window controls
- Theme system with `BaseMode` (Light/Dark/Oled), accent schemes with gradient support,
  glow/radius/animation/density configuration
- Appearance Studio with live preview
- Signature theme (Lime + Orange)

### Changed
- Rebranded from Kanji Dojo to Kaiteyo (書いてよ)
- Complete documentation system in `/docs/`

### Fixed
- `animateColorAsState` import (now from `androidx.compose.animation`)
- `animateFloatAsState` import (now from `androidx.compose.animation.core`)
- `windowState.window!!.close()` → `window.close()` in FrameWindowScope
- Missing `@Composable` import
- Desktop app compilation (BUILD SUCCESSFUL)

## v1.0.0 (Initial Fork)

### Added
- Forked from Kanji Dojo
- Basic flashcard study system with spaced repetition algorithm
- JLPT decks (N5–N1), stroke-order diagrams, vocabulary lookup
- Cross-platform support (Desktop, Android, iOS)
- Koin dependency injection, SQLDelight database, Ktor HTTP client, DataStore preferences
