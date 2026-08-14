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
- [x] **Sidebar looks attached** — Fixed in `WorkspaceShell.kt`/`WorkspaceNav.kt`: the dock (rail/bar/compact tab bar) now floats as an island — 8dp ring of window background on every side, `DsRadius.Lg` corners, `surfaceElevated` background, accent-tinted floating shadow and a soft inner border. *(Pending compile + runtime verification.)*
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

- [x] **Anki → Kaiteyo import** — New `AnkiImporter` (`engine/transfer/`) pulls decks, notes, cards and tags from AnkiConnect into Kaiteyo's library. The Anki deck path (`Japanese::N5::Kanji`) is preserved as nested Kaiteyo decks (reusable via the `anki:path:<full>` deck tag); notes map onto `DesktopCard`s with all fields preserved (unknown fields kept in the note); scheduling is carried over approximately (queue→status, learning seconds→days, review due→epoch-day approx, permille ease→decimal, reps/lapses). Duplicate detection is two-layered — the Anki note GUID (stored on the new `DesktopCard.externalId`) makes re-imports idempotent, with a content fingerprint fallback — and conflict handling is user-selectable (Skip / Update / Duplicate). Media referenced by fields (`[sound:x]` / `<img>`) is downloaded into `~/.kaiteyo/anki-media`. Import dialog (`ui/api/AnkiImportDialog.kt`) fetches a live deck preview (counts, tags, sample fronts), lets the user pick decks + policy + options, shows progress, and reports per-deck results with warnings. Launched from Integrations → AnkiConnect → "Import from Anki…". Tests in `AnkiImportMapperTest.kt`. *(Code complete — requires a live AnkiConnect to verify.)*
- [x] **System media keys (Windows)** — New `SystemMediaKeys.kt`: a global `WH_KEYBOARD_LL` hook via the existing JNA dependency captures the keyboard's Play/Pause · Next · Previous · Stop even without app focus. The hook is only installed while media is loaded (see `MediaEngine.tick()`), so Kaiteyo never swallows other apps' media keys while idle. Toggle: Settings → Media → Playback → "System media keys" (persisted). *(Code-complete; runtime verify on Windows.)*
- [x] **Media notifications** — Tray-balloon notifications for playback transitions and mined cards (Settings → Media → Playback → "Playback notifications"). `MediaTray.notify()` + `MediaEngine.notifyUser()`/`notifyMined()`.
- [x] **AnkiConnect integration** — New `AnkiConnectTransport.kt`: real AnkiConnect client (list/create decks, Basic notes, tags, base64 screenshot/audio media, `canAddNotes` duplicate detection). Mined cards forward to Anki when enabled (`media.anki.*` settings). Hub card with Test connection.
- [x] **Local API auth** — The local HTTP API now requires `Authorization: Bearer <token>` on every endpoint except `/api/health`; the token is generated once and persisted via settings; port is configurable (`media.api.port`); `selfTest()` verifies liveness + auth. New `media.api.enabled` auto-starts the server.
- [x] **Integration Hub** — `IntegrationsView` rebuilt with per-integration status cards (Local API, GameSentenceMiner, AnkiConnect, Text hook, Player WebSocket, System media keys), all driven by real engine state, with working Test-connection buttons on `Dispatchers.IO`.
- [x] **Settings persistence** — `SettingsEngine` now persists to `~/.kaiteyo/settings.json` (opt-in `persistFile`), so media key toggles, integration config and the API token survive restarts; reset-all/reset-category persist too.
- [x] **Floating dock island** — The desktop nav rail/bar and the compact tab bar are now floating islands: 8dp margin from the window edges, `DsRadius.Lg` (16dp) rounded corners, elevated surface background, accent-tinted `DsElevation.Floating` shadow, and a soft border (down from a harsh full-height 1dp line). Covered for all four dock positions + compact windows. See `DsDockIsland` in `WorkspaceShell.kt` and the dock styling in `WorkspaceNav.kt`. *(Pending compile + runtime verification.)*
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
- [x] **Desktop card pool was in-memory only** — `seedDemoData()` reseeded demo cards on every launch, wiping imports/edits. The pool now persists to `~/.kaiteyo/library/cards.json` (`LibraryStore.loadCards/saveCards`), is restored in `AppState.init`, and `seedDemoData()` gates on `cards.isNotEmpty() || library.hasPersistedCards()`. Imports, card edits, reviews, suspend/forget/reschedule and profile restore all persist the pool. *(Code complete — pending compile + runtime verification.)*
- [x] **Anki .apkg import/export was broken** — Desktop + core `AnkiPackage` rewritten: correct `sfld`/`csum`, per-card import with `ord` preserved, deck names from `col.decks`, `{{Field}}`/`{{cloze:N:..}}` template rendering, HTML sanitization, media extraction + reference repair, deterministic GUIDs, and clear error messages for non-ZIP / missing-database packages. *(Code complete — pending compile + runtime verification.)*
- [x] **Import/Export screen was a prototype** — `ImportExportSystem.kt` had no ViewModel wiring and every button was a no-op. Rewritten and wired to the real `ImportExportContract` pipeline: file pick/save (JVM `JFileChooser`, iOS document picker, Android SAF), paste import, preview with validation stats + issues, conflict policies, and export to file/clipboard.
- [x] **Core imports were ephemeral** — `ImportExportViewModel` now persists through `DeckFeaturesController.mergeImportedCards()`: scheduling → FSRS (ease/interval/lapses/reps), flags/tags/notes onto matching catalog cards, with history entries.
- [x] **Dead transfer code removed** — `BackupContract`/`BackupViewModel` (shadowed by the real `BackupScreenContract`) deleted; duplicate screen-local `ImportConflictStrategy`/`ImportFormat`/`ImportPreview`/`ExportConfig` types removed (single `core.transfer` source of truth); dangling `AnkiPackageAndroid`/`AnkiPackageIos` DI references fixed to the `AnkiPackage` actual.
- [x] **Screens could spin forever on load failure** — `refreshableDataFlow`/`refreshableDataProducerFlow` had no exception handling: any throw killed the collection coroutine and the screen stayed on the spinner. Both now emit `RefreshableData.Failed` instead of crashing the collector; Letters, Vocab, General dashboards and Deck Details gained a `ScreenState.Error` variant + shared `DashboardErrorState` (message + retry) in `dashboard_common`, and `retryLoad()` on their ViewModels. *(Code complete — pending compile + runtime verification.)*
- [x] **Dead/disabled controls** — `PracticeCommonUI` `ToolbarCountItem` was a `TextButton` wrapping a count badge with no action (now plain text); the General dashboard collection chips were decorative (now navigate to the real `MainDestination.Collections` route); removed stale WebDAV placeholder comment in `GitHubCloudProvider`.
- [x] **Android APKG was unimplemented** — `AnkiPackage.android.kt` now implements real import/export via Android's built-in `SQLiteDatabase` (schema v11, decks/models JSON, notes↔cards with `ord`, tags, status/interval/reps/lapses/ease mapping, HTML sanitization, useful errors). DI passes the app `Context`.
- [x] **iOS APKG was unimplemented** — `AnkiPackage.ios.kt` now implements real import/export via SQLDelight's `NativeSqliteDriver` (sqlite3 is linked via `linkSqlite = true`; a version-0 no-op schema opens existing collections without touching their schema). The ZIP container is handled by a new dependency-free codec — `IosZip.kt` (writer emits STORED entries, reader supports STORED + DEFLATE, exact CRC-32) and `IosInflate.kt` (pure-Kotlin RFC 1951 inflate: stored/fixed/dynamic blocks, 32 KiB back-references). Behaviour mirrors the JVM/Android actuals. *(Code complete — requires a macOS build to verify.)*
- [x] **Unsafe casts in Deck Details** — `navigateToDeckEdit` and the visibility-dialog block used `as ScreenState.Loaded`/`as ScreenState.Loaded.Letters`, which crash if the state isn't loaded at click time; both are now null-safe with graceful fallbacks.
- [x] **Review keyboard shortcuts advertised but not implemented** — `ReviewView` showed a footer "1 Again · 2 Hard · 3 Good · 4 Easy · Space reveal" plus B/S/R/Ctrl⏎/Ctrl Z hints with zero key handling. Added `onPreviewKeyEvent` in `SessionPanel`: Space reveals, 1-4 grade, B bury, S suspend, R retry, Ctrl+Enter skip, Ctrl+Z undo (guarded while the reschedule dialog is open so typing in its numeric field is never intercepted).
- [x] **iOS file picker/save was stubbed** — `TransferFileAccess.ios.kt` returned null/false, so APKG import/export was unreachable on iOS. `pickImportFile`/`saveExportFile` are now `suspend` (JVM semantics unchanged) and the iOS actuals present the real `UIDocumentPickerViewController` — open picker for import with extension validation, and a "Save to Files" export picker backed by a temporary file that is cleaned up on completion/cancel. Callers in `ImportExportSystem.kt` launch them from the composition scope. *(Code complete — requires a macOS build to verify.)*
- [x] **Android file picker/save was stubbed** — the Android actuals returned null/false with no way to reach a picker from a top-level suspend function. They now delegate to `AndroidTransferFileAccess`, a provider registry that the new `AndroidTransferFilePickerHost` composable (wrapping `setContent` in `KaiteyoActivity`) backs with real `ActivityResultContracts.OpenDocument`/`CreateDocument` launchers: import opens SAF for any file type and validates the extension via `OpenableColumns.DISPLAY_NAME`, export streams bytes to the user-chosen `ACTION_CREATE_DOCUMENT` URI. Until the host composes (previews/tests) the actuals degrade to null/false. *(Code complete — pending an Android build to verify.)*
- [x] **No picker-free re-import on Android** — a successful SAF pick now takes a persistable read grant (`takePersistableUriPermission`, best-effort) and remembers the URI + display name in SharedPreferences (`transfer_file_access`). New `getLastImportFileName`/`readLastImportFile` expect/actuals (JVM/iOS return null — iOS would need security-scoped bookmarks) feed a "Re-import {name}" button on the Import tab that re-reads the last file without opening the picker; a stale grant/file entry is cleared automatically so the shortcut disappears instead of failing forever. *(Code complete — pending an Android build to verify.)*
