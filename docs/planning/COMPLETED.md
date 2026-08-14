# Kaiteyo — Completed Features

This document tracks completed features and milestones. When a task from TODO.md or CURRENT_ISSUES.md is completed, add it here.

## v2.3 — Anki Interoperability & Persistent Data (Latest)

### Data Persistence (Desktop Suite)
- [x] Card pool persisted to `~/.kaiteyo/library/cards.json`; restored on launch before any UI is built
- [x] Demo seeding is first-run only — imports/edits/reviews survive every restart
- [x] Every mutation path persists: reviews, writing, suspend/forget/reschedule, editor, mining, imports, profile restore, stress dataset

### Anki Compatibility (`.apkg`)
- [x] Export: real SQLite `collection.anki2` + media manifest, schema v11, correct `sfld`/`csum`, deterministic GUIDs, decks from Kaiteyo decks
- [x] Import: deck hierarchy preserved (`Japanese::N5::Kanji`), notes→cards with `ord`, tags, scheduling (type/queue→SRS, interval, ease, reps, lapses, due), media extracted + references repaired
- [x] Templates: `{{Field}}`, `{{text:}}`, `{{cloze:N:}}`, `{{FrontSide}}` rendered; unknown directives dropped safely
- [x] HTML sanitized (scripts/styles/event-handlers/javascript: URIs stripped) — imported content is never executed
- [x] Corrupt packages fail with useful messages (not a ZIP / no `collection.anki2`); rollback-safe (temp files only)
- [x] **Honest compatibility limits:** exact template styling, typing-mode cards, and cram scheduling are not reproduced — imported content is rendered to safe plain text with the sanitized HTML kept in the note field

### Core Import/Export Pipeline
- [x] Single `ConflictPolicy` (`KeepExisting`/`OverwriteExisting`/`Skip`/`KeepNewest`) + `ImportPreview`/`ImportResult` in `core.transfer`
- [x] JSON/CSV/TSV/TXT codecs with Japanese-safe UTF-8 handling, CSV quoting/escaping, duplicate detection, validation with severity
- [x] Imports merge into the kanji catalog (FSRS scheduling, flags, tags, notes) and are recorded in the study-history audit log
- [x] Import/Export screen fully wired (file pick/save, paste import, preview, conflict policy, export to file/clipboard)
- [x] Tests: `TransferCodecsTest` (JSON/CSV/TSV/TXT round trips), `ImportPipelineTest` (preview/duplicates/policies), `AnkiPackageJvmTest` (round trip, GUID stability, malformed input, status/checksum mapping)

### Cleanup
- [x] Dead `BackupContract`/`BackupViewModel` removed; screen-local duplicate transfer types removed
- [x] `TransferFileAccess` expect/actual added (JVM `JFileChooser`; Android/iOS degrade gracefully with clear UI messaging)

## v2.2.1 — Installer & First-Run Subsystem (Latest)

### Windows Installer (Inno Setup 6)
- [x] Branded modern wizard (dynamic light/dark, custom banner + small image, DPI-aware)
- [x] Install / Upgrade / Repair / Modify / Uninstall with data preservation
- [x] Silent install (`/VERYSILENT`), task-based components (shortcuts, file assoc, auto-update, dictionary pack)
- [x] Install-location memory, existing-install detection, launch-after-install
- [x] Polished uninstaller: keep-or-remove study data page, cache/temp cleanup, double confirmation
- [x] Portable zip build with self-contained data folder
- [x] `build.ps1`/`build.bat` wrappers with optional signtool signing

### macOS
- [x] Styled DMG (branded background, drag-to-Applications, volume icon)
- [x] Hardened-runtime signing + JVM entitlements, notarization + stapling pipeline
- [x] Per-arch DMGs (arm64 / x64)

### Linux
- [x] AppImage with AppStream metainfo + hicolor icon theme + AppRun
- [x] deb builder (control, postinst icon/desktop caches)
- [x] rpm spec + wrapper
- [x] Flathub-ready Flatpak manifest + build script
- [x] Snap wrapper (optional format)

### Shared Tooling
- [x] `common/version.json` single source of truth + `bump-version.sh`
- [x] Update-feed + artifact-manifest JSON schemas, `make-update-manifest.sh`, `verify-artifacts.sh`
- [x] `stage-artifacts.sh` canonical release dir + sha256 manifest
- [x] `generate-assets.sh` SVG → bmp/ico/icns/png pipeline
- [x] Release notes template, docs: ARCHITECTURE/BUILD/SIGNING/RELEASE/UPDATES/FIRST_RUN

### First-Run Experience
- [x] `OnboardingWizard` wired into `KaiteyoDesktopSuite` (shows once, crash-safe flag via settings key `onboarding.completed`)
- [x] 8-step flow: Welcome, Theme, Accent, Scaling, Font, Navigation, Motion, Finish — every step skippable
- [x] Live previews: selections write straight into `AppState.themeManager` / navigation state
- [x] Re-openable from Settings → “Show onboarding again” (`AppState.requestOnboarding()`)
- [x] `FIRST_RUN.md` documents the gating mechanism and integration points

### Auto-Update Architecture (designed, not enabled)
- [x] `UpdateChannel` stable/beta/nightly, `UpdateManifest` schema v1
- [x] `HttpUpdateChecker`, sha256-verified `HttpUpdateDownloader`
- [x] `UpdateInstaller` interface (per-platform apply), `UpdateService` StateFlow coordinator
- [x] `UpdatePolicy`: version compare, min-version guard, rollback window

### CI/CD
- [x] `build-all.yml` produces EXE/MSI/portable, styled+notarized DMGs, deb/rpm/AppImage
- [x] `build-release.yml` stages, verifies and feeds updates on tag push

## v2.0.0 — Premium Experience (Latest)

### Theme Studio v2.0
- [x] Interactive HSV color wheel with drag-to-pick
- [x] RGB/HSL/HSV/HEX synchronized editors
- [x] 11 color targets (Primary, Secondary, Tertiary, Background, Surface, Text, SurfaceVar, Outline, Success, Warning, Error)
- [x] Recent & saved color palette (save/clear, up to 20 colors)
- [x] Apply button writes to themeState.accentScheme
- [x] Gradient Editor (Linear/Radial/Angular, 2-8 stops, angle, intensity, opacity)
- [x] Motion Studio (5 presets, spring physics, page transitions, duration, reduced motion)
- [x] Layout Studio (density, corner radius, sidebar mode/position, glow, transparency)
- [x] Theme Management (Export/Import/Reset buttons)
- [x] Live Preview Panel (sidebar + dashboard + stats + progress cards)
- [x] KMP-safe formatting (no String.format!)
- [x] Custom Color hue/saturation/lightness computed properties

### Floating Island Sidebar v2.0
- [x] Drag to reposition (detectDragGestures)
- [x] 9 dock states (Left, Right, Top, Bottom + TL/TR/BL/BR + Floating)
- [x] Resizable via bottom-right handle
- [x] Drag handle indicator for floating mode
- [x] Close/dock button for floating mode
- [x] Borderless elevated glass appearance
- [x] Spring animations on all transitions
- [x] Auto-hide mode with slide transitions
- [x] Collapse toggle with rotation animation
- [x] SidebarNavItem, SidebarDivider, SidebarSectionHeader, SidebarProgress

### Brush Quality Engine
- [x] Stroke smoothing (moving average low-pass filter)
- [x] Input prediction (velocity + acceleration extrapolation)
- [x] Bezier smoothing (Catmull-Rom spline, 2-8 segments)
- [x] Velocity-based adaptive smoothing
- [x] Jitter reduction (tremor elimination)
- [x] Pressure sensitivity support
- [x] Full processStroke() pipeline
- [x] StrokePoint data class
- [x] resolvePressureWidth() function

### Branded Installer
- [x] 8-step installation wizard
- [x] Welcome screen with 6 feature showcases
- [x] Installation location picker
- [x] Component selection (shortcuts, startup, file assoc, auto-update)
- [x] Theme preview with 4 base modes
- [x] Accent theme selector
- [x] Accessibility settings (scale, font size, contrast, animations)
- [x] Animated progress screen
- [x] Completion screen with post-install options
- [x] Animated transitions between steps
- [x] Step progress indicator

### Onboarding Wizard
- [x] 8-step first-launch setup
- [x] Theme, accent, scaling, font size, sidebar, animations, finish
- [x] Live preview for each setting
- [x] Animated transitions
- [x] Skip All button
- [x] Step progress bar

### Anki-like Features (All Compiling)
- [x] FlagManagerScreen.kt
- [x] NoteEditorEnhanced.kt
- [x] KeyboardShortcutsPage.kt
- [x] AnkiCardOperations.kt
- [x] SearchEngineImpl.kt
- [x] DeckBrowserEnhanced.kt

### Build Fixes
- [x] Desktop app compiles successfully
- [x] Fixed String.format() → KMP-safe formatFloat()
- [x] Fixed Key.Digit0/Quote/Backtick → alternative constants
- [x] Fixed MutableList from .map() → .toMutableList()
- [x] Added @OptIn(ExperimentalMaterial3Api::class) annotations

## v1.1.0

### Build System
- [x] Desktop app compiles successfully
- [x] Fixed `animateColorAsState` import (now from `androidx.compose.animation`)
- [x] Fixed `animateFloatAsState` import (now from `androidx.compose.animation.core`)
- [x] Fixed `windowState.window!!.close()` → `window.close()` in FrameWindowScope
- [x] Fixed missing `@Composable` import

### Window Experience
- [x] Undecorated window (no title bar)
- [x] Floating window controls (minimize, maximize, close)
- [x] Spring-based hover animations on controls
- [x] Window drag region using WindowDraggableArea
- [x] Rounded corners (20dp)

### Theme System
- [x] Base mode support (Light, Dark, Oled)
- [x] KaiteyoThemeState with composition locals
- [x] Accent scheme selection
- [x] Gradient support per accent scheme
- [x] Glow configuration
- [x] Radius configuration
- [x] Animation configuration
- [x] Density configuration
- [x] SurfaceColors composition local

### Documentation
- [x] All 15+ documentation files in `/docs/`
- [x] Architecture Decision Records
- [x] AI Context for assistant onboarding
- [ ] Fix animation performance (stuttering)

### Theme System
- [ ] Implement remaining 7 built-in themes
- [ ] Signature theme: distribute lime + orange intelligently

## Planned for v1.2

### Floating Sidebar
- [ ] Floating island design
- [ ] Multiple dock positions
- [ ] Auto-hide
- [ ] Spring animations

### Appearance Studio
- [ ] Full Theme Studio with color editor
- [ ] Gradient editor
- [ ] Live preview
- [ ] Theme import/export
