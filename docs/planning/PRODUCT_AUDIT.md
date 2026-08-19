# Kaiteyo — Product Audit

Date: 2026-08-14
Auditor: Codebuff (deep read of the repository at HEAD `760f64f7`)

This document maps the repository as it actually exists: what is real, what is
duplicated, what is dead, and what is fake. It is the foundation for the
"one coherent product" work. Everything here was verified by reading code, not
by assuming compilable code is correct.

---

## 1. THE HEADLINE FINDING: TWO PARALLEL APPLICATIONS

The repository contains **two complete, independent applications** that do not
talk to each other:

| | Core app | Desktop suite |
|---|---|---|
| Entry point | `desktopApp/.../Main.kt` `main()` → `KaiteyoApp` (also Android `KaiteyoActivity`, iOS) | **retired** — the standalone suite entry (`desktopSuiteMain()`/`SuiteMain.kt`) was removed; the suite is now a JVM-only feature library folded into shipped destinations (`DesktopMediaCentreContent`, `DesktopGameCentreContent`) |
| Reachable from a shipped `main()`? | **Yes** — this is the product | Only through shipped destinations (Media, Kaiteyo World) — no parallel shell |
| UI framework | Compose MPP, shared across desktop/Android/iOS | Compose Desktop, JVM-only |
| Navigation | `NavShell` (Sidebar/Floating, 4 edges, snap, form factors, persistence) | `WorkspaceNav` (dock: Sidebar/Floating, 12 snap points, persistence) |
| User data | SQLDelight `UserDataDatabase` (FSRS cards, decks, tags, flags, review history, study history, migrations) | JSON files under `~/.kaiteyo/` (`cards.json`, `settings.json`, statistics) |
| Reference data | SQLDelight `AppDataDatabase` (bundled asset, read-only) + `kjd/` generator | `DictionaryRepository` (imported Yomitan/JMdict dictionaries on disk) |
| SRS | FSRS in `core/.../srs/fsrs` | `ReviewSession` in `desktop/.../review` (its own scheduling) |
| Settings | `PreferencesContract` (DataStore) + `ThemeSettingsState` | `SettingsEngine` (`~/.kaiteyo/settings.json`) |
| Statistics | `StatisticsController` + `StatisticsScreen` (SQLDelight-driven) | `StatsView` + `AppState.summaries` (JSON-driven) |
| Library/decks | `LibraryScreen` + `LetterSrsManager`/`VocabSrsManager` + `DeckFeaturesController` | `LibraryStore` + `CollectionStore` |
| Dictionary/media/mining/OCR/AnkiConnect | — (absent) | `DictionaryService`, `MediaEngine`, `MiningEngine`, `OcrEngine`, `AnkiImporter`, `LocalApiServer` — **all unreachable in the shipped app** |
| Onboarding | — (none in core app) | `OnboardingWizard` (suite only) |

**Consequence:** every subsystem the brief cares about (dictionary, media,
mining, OCR, integrations, floating launcher with snap) exists *only* inside the
suite, which users can never launch. The app users actually get (core app) has
no dictionary popup, no media workspace, no mining. And the suite maintains a
second copy of decks, SRS, statistics, settings, themes, sync and account —
none of which share data with the core app. This is the single most important
architectural defect.

**Recommendation (decision needed):** either (a) make the suite the desktop
product and port its engines to share the core SQLDelight user database, or
(b) keep the core app as the product and integrate the suite's unique engines
(dictionary, media, mining, OCR) into the core `MainDestination` navigation
with the core data model. Both apps cannot ship. Any feature work should happen
in the chosen product; the other becomes a reference.

---

## 2. REFERENCE DATA LAYER (healthy)

- `kjd/` is a real, standalone Japanese data platform: ingest adapters for
  KanjiVG, KANJIDIC, JMdict, JmdictFurigana, Tanos JLPT, Leeds frequency,
  yomichan-jlpt-vocab; normalization; entity resolution; validation; SQLite
  export + Kotlin SDK + CLI. It is **architecturally independent** of the app —
  matches the brief's "reproducible ingestion pipeline".
- `core/.../app_data/` SQLDelight `AppDataDatabase` serves kanji/readings/
  meanings/radicals/classifications to the core app; `KaiteyoDataCenter` loads
  it once (`ensureLoaded`) and exposes it to Library/KanjiBrowser/Collections.
- **Gap:** the *suite* does not use `AppDataDatabase` or kjd output at all — its
  dictionary comes from user-imported Yomitan archives. Reference data and user
  data are cleanly separated in the core app (read-only asset vs mutable DB).
- **Gap:** grammar, pitch accent, and example sentences exist only as data
  *sources* in kjd (JMdict/JmdictFurigana), not yet as surfaced screens in the
  core app (Library's "Grammar" section card currently navigates to the vocab
  dashboard — see §5).

## 3. NAVIGATION — duplicated, both mature

Two full navigation models, both with sidebar/floating modes and persistence:

- Core `NavShell.kt` (shared, shipped): `NavigationMode.Sidebar/Floating`,
  edge selection (Left/Right/Top/Bottom; phone restricted to Top/Bottom),
  expanded/compact, tooltips, focus/hover states, `Ctrl+B` mode toggle,
  `LocalNavBarBottomSpace` so snackbars clear the dock, snap points for the
  bubble, persisted via `PreferencesContract`.
- Suite `WorkspaceNav.kt` (JVM-only, unshipped): same concepts re-implemented
  (dock island, compact tab bar, `NavLayout`/`NavExpansion`/`LauncherSnapPoint`
  with 12 snap targets).

Both are high quality; neither should be re-written. The decision in §1 decides
which survives. No third implementation should ever be created.

## 4. WHAT IS REAL (do not break)

Verified by reading call chains from entry point to database:

- **Home** — `HomeScreen` → `GeneralDashboardScreen`/`LibraryScreen`/
  `StatisticsScreen`/`SearchScreen`/`SettingsScreen`, all driven by
  `KaiteyoDataCenter` + SRS managers; counts come from SQLDelight.
- **Library hub** — real deck rows with real due/new counts
  (`LetterSrsDeck.totalDue()` etc.), real navigation to Deck Details/Deck
  Browser/Card Browser/Tags/Flags/Statistics/Import-Export.
- **Statistics** — one destination (`StatisticsScreen` via
  `StatisticsController`); old V2 consolidated (per CURRENT_ISSUES). Includes
  heatmap, day drill-down, real review-history aggregates. `stats/CardInspector`
  is a shared component, not a duplicate screen.
- **Deck features** — `DeckFeaturesController` + real routes (`DeckBrowserRoute`,
  `CardBrowserRoute`, `TagManagerRoute`, `FlagManagerRoute`, `NoteEditorRoute`,
  `ImportExportRoute`, `BackupRoute`, `StatisticsController`) all load from the
  database and surface loading/error/retry states.
- **Import/export** — real per-platform file access (JVM/iOS/Android), real
  Anki `.apkg` codec, real conflict policies, persisted card merge.
- **Sync/account** — `SyncManager`/`AccountManager` with real GitHub-cloud
  transport, conflict dialog, offline/retry states.
- **SRS** — FSRS in `core/.../srs/fsrs` with tests (`FsrsSchedulerTest.kt`).
- **Writing** — core letter practice with stroke evaluator
  (`core/.../stroke_evaluator`), persisted attempts, SRS integration.
- **Desktop window chrome** — `KaiteyoWindow.kt` (44dp drag region, window
  controls, bounds persistence); window shell is genuinely scoped now.
- **`kjd/`** data platform (see §2).

## 5. CONFIRMED DEFECTS (fixed in this pass)

### 5.1 Core app — shipped, user-visible

| Location | Defect | Fix |
|---|---|---|
| `screen/home/HomeScreenUI.kt` `CompactBottomNav` | "More" overflow button was a dead control (`// TODO: Show overflow menu with remaining tabs`) | Implemented a real `DropdownMenu` listing the remaining tabs (icon + label), selecting a tab navigates to it. Button now highlights when an overflow tab is active. |
| `screen/home/HomeScreenUI.kt` `VerticalTabButton` | Two `.clickable` modifiers on one Box — every tap fired the handler twice and the first clickable disabled the ripple | Single clickable with `LocalIndication`; fixed modifier order (weight → wrap → size → clip → background → clickable → testTag). |
| `screen/home/HomeScreenUI.kt` `CompactLauncher` | Dead private composable, never invoked | Removed (plus now-unused imports). |
| `screen/decks/TagManagerScreen.kt` | Selection-mode **Merge** and **Delete** toolbar buttons were `onClick = { }` — the screen's `onMergeTags`/`onDeleteTag` callbacks were already wired to the controller | Merge now merges every selected tag into the first selected (via `onMergeTags`); Delete shows a confirmation dialog then deletes all selected (via `onDeleteTag`). Buttons are disabled until a valid selection exists. |
| `screen/decks/PluginSystem.kt` | Refresh toolbar button was `onClick = { }`; plugin config dialog had dead inputs (fields never applied, `if (config.isNotEmpty() \|\| true)` hack) | Refresh re-registers built-ins and clears per-plugin error state; dialog now shows stored config, Apply writes key/value via `updateConfig` (enabled only when both fields are non-blank). |
| `screen/decks/BackupSystemExt.kt` `BackupVerifier` | `verifyChecksum()` returned hardcoded "Backup integrity verified"; `verifyDatabaseIntegrity()` returned "passed" without a database; `estimateCompressionRatio()` fabricated 0.4 | `verifyChecksum` now computes a real SHA-256 (pure-Kotlin, commonMain-safe — JVM `MessageDigest` is unavailable there) and compares honestly, reporting mismatch/read failures; `verifyDatabaseIntegrity` reports honestly that it needs the app DB handle; compression estimate documented as an expectation, not a measurement. The SHA-256 implementation was verified against FIPS test vectors and 200 random buffers vs node crypto. |

### 5.2 Dead code / shadow implementations — REMOVED (2026-08-18 pass)

All three dead shadows are gone from the tree (verified by `grep` across
`core/src` and `desktopApp/src` — only this document still names them).

- **`screen/decks/LearningPowerHub.kt` + friends** — the whole "Learning Power"
  hub (placeholder `BackupManagerScreen`, `SearchEngineScreen(onSearch = { })`,
  `BulkActionsFullScreen` with fake persistence, decorative stats, dead
  "Start Restore" button) was **removed**. The real destinations it faked
  (Backup, Search, Bulk actions, Card manager, Statistics) all ship as wired
  `MainDestination`s (`BackupRoute`, `SearchRoute`, `BulkActionsRoute`, …).
- **`screen/sync/SyncSettingsUI.kt`** — the 443-line parallel sync settings
  screen with the dead "Sync Now" button was **removed**. The real sync UI is
  `SyncScreen`.
- **`BackupSystemExt.kt` (BackupManagerScreen path)** — reachable only through
  the dead LearningPowerHub; the real backup UI is `BackupRoute`/`BackupScreen`.
- **`app/src/main/.../preview/screen/StatsScreenPreview.kt`** — preview only,
  uses fake data by design (previews are fine).

### 5.3 Intentional-but-questionable empty handlers (left as-is)

- `TutorialDialog.kt` SRS answer buttons (`onClick = { }`) — illustrative demo
  buttons inside a tutorial page that explains the answer buttons. Acceptable
  as-is; consider `enabled=false` styling if they read as interactive.
- `ReviewShortcutsSettings.kt` preview answer buttons — decorative preview.
- `DeckDetailsScreenUI.kt` `FilterChip(selected = true, enabled = false, ...)`
  — a disabled status chip; empty onClick is required by the API.
- `LearningPowerHub.kt` is dead (§5.2) so its inner dead buttons are moot.

### 5.4 Suite defects (unshipped, but real)

- `AppState.seedDemoData()` seeds ~a demo card pool into the user library on
  first run (`buildDemoCards` + `buildDemoContentCards` + seeded summaries/
  collections/activity/dictionary/library). Gated to first run, so it does not
  wipe user data — but demo cards presented as real study content violate the
  "no fake data" rule for a product that ships. The suite should start empty
  with a "first-run" empty state instead.
- `defaultSettings()` in `SettingsEngine` mixes legacy keys (`navigation.mode`
  with option `bubble`, `navigation.collapsed`) with the active
  `navigation.layout` enum — migration exists, but the settings catalog and
  the `NavLayout` enum disagree on vocabulary ("bubble" vs "Floating").
- The suite's `MediaEngine`/`OcrEngine`/`DictionaryService` are real but
  unreachable (§1).

## 6. DUPLICATION MAP (single-source-of-truth candidates)

| Concept | Core app (shipped) | Suite (unshipped) |
|---|---|---|
| Navigation | `NavShell.kt` | `WorkspaceNav.kt` |
| Settings | `PreferencesContract` (DataStore) | `SettingsEngine` (JSON) |
| Theme | `theme_manager` + `ThemeSettingsState` + `ThemeStudio` | `theming/ThemeManager` + `ThemePresets` + `ThemeStudioView` |
| Statistics | `StatisticsController`/`StatisticsScreen` | `StatsView` + `AppState` summaries |
| Library/decks | `LibraryScreen`, `LetterSrs`/`VocabSrs`, `DeckFeaturesController` | `LibraryStore`, `CollectionStore` |
| SRS/review | FSRS (SQLDelight) | `ReviewSession` (JSON) |
| Dictionary | `AppDataDatabase` (bundled) | `DictionaryService`/`DictionaryRepository` (imported) |
| Account/sync | `AccountManager`/`SyncManager` | `AccountEngine`/`CloudSyncCoordinator` |
| Import/export | `core/transfer` (APKG, JSON, conflict policy) | `desktop/transfer` (AnkiConnect, Anki importer) |
| Backup | `BackupScreen`/`BackupRoute` | `BackupSystemExt` (dead path) |
| Plugin system | `PluginSystem.kt` (in-memory) | `engine/plugin/PluginRegistry` |
| Onboarding | — | `OnboardingWizard` |

## 7. PLATFORM STATUS

- **Android** — real: `KaiteyoActivity`/`KaiteyoApplication`, flavors
  (googlePlay/fdroid), SAF file picker with persisted grants, APKG via
  `SQLiteDatabase`, previews. Not abandoned.
- **iOS** — real: renamed to `KaiteyoApp`, Swift host, document picker,
  dependency-free ZIP/inflate codec for APKG, SQLDelight `NativeSqliteDriver`.
  Build-verification requires macOS (expected).
- **Desktop** — two apps (see §1). Installer/package tasks exist per-OS;
  `local.properties` SDK remap is handled by `scratch/run-gradle.sh` in the
  WSL dev environment.
- **Build environment note:** in the current WSL container, the repo lives on a
  9p (drvfs) mount and Gradle's `CachingFileHasher` fails with a bare
  `java.io.IOException: I/O error` when the project `.gradle` cache holds files
  left by a live daemon (9p refuses to delete/open locked files). Recovery:
  kill stray `GradleDaemon` processes, then delete the project `.gradle`
  directory *while no daemon holds it*. A clean Linux-side `GRADLE_USER_HOME`
  (e.g. `/root/.gradle-kaiteyo`, as the repo's own `scratch/run-gradle.sh`
  documents) avoids the drvfs journal problems entirely.

## 8. PRIORITY ORDER FOR THE "ONE PRODUCT" WORK

1. **Decide the product** (core app vs suite) — gates everything else.
2. **Unify the user-data layer** — one SRS, one deck model, one statistics
   source of truth. Migrate suite engines onto the SQLDelight schema (or vice
   versa). Delete the losing navigation/settings/theme/statistics copies.
3. **Remove the dead shadows** (§5.2) — LearningPowerHub, SyncSettingsUI,
   unused BackupSystemExt path.
4. **Bring suite-only engines into the product** — dictionary popup, media,
   mining, OCR, AnkiConnect — over the unified data model.
5. **First-run UX** — empty state instead of seeded demo data; onboarding only
   in the product.
6. **Close data gaps** — grammar/pitch/example-sentence screens in Library from
   kjd; deck generation from reference data (JLPT/grade) instead of hardcoded
   lists.
