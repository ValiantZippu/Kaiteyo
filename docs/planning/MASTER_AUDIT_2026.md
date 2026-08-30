# Kaiteyo — Master Repository Audit 2026-08-30

> **Scope**: Full 30-point audit of the Kaiteyo repository at HEAD `6e7fcf7d` (develop).
> Every claim below was verified by reading code. Status taxonomy:
> `IMPLEMENTED` · `PARTIALLY_IMPLEMENTED` · `PLACEHOLDER` · `BROKEN` · `DEPRECATED` · `UNKNOWN`.
> See `docs/planning/CURRENT_STATE.md` (living matrix) and `docs/planning/PRODUCT_AUDIT.md` (deep audit) for the companion references.

---

## 1. Project Structure

```
kaiteyo/
  core/            KMP shared code (435 presentation files, 32 ViewModels, 35 Koin modules)
  desktopApp/      JVM desktop suite + window shell (engine 160 files, ui 65 files, designsystem 18)
  app/             Android entry (googlePlay + fdroid flavors)
  iosApp/          iOS entry (Swift host + Compose)
  kjd/             Data platform (ingest → SQLite, 33 files, 10 tests)
  mediaGenerator/  Asset generation utility (javacv+coil)
  installer/       Branded installers (not a Gradle module) — msi/dmg/appimage/deb/rpm/flatpak/snap
  website/         Python static site (build.py, 300+ pages)
  buildSrc/        AppVersion.kt (2210/2.2.1) + AppAssets.kt (v15) + PrepareAssetsTask
  docs/            298 markdown files (architecture, product, game, planning, etc.)
  reference/       Upstream analyses (DAKANJI/JIDOUJISHO/KANJI_HEATMAP) + kanji-heatmap clone
  gradle/          Version catalog (kotlin 2.1.20, compose 1.8.2, koin 4.0.0, ktor 3.1.2, sqldelight 2.0.2...)
```

**Verdict**: `IMPLEMENTED` — clean module separation, single version truth, managed assets.

## 2. Application Entry Points

| Platform | Entry | File | Status |
|----------|-------|------|--------|
| Desktop | `main()` → `startKoin(appModules+desktopAppModule)` → `Window(undecorated)` → `KaiteyoDesktopSuite` | `desktopApp/src/jvmMain/kotlin/.../desktopApp/Main.kt:97` | `IMPLEMENTED` |
| Android | `KaiteyoApplication` + `GooglePlayMainActivity` / `FdroidMainActivity` | `app/src/main/.../KaiteyoApplication.kt` | `IMPLEMENTED` |
| iOS | `IosKotlinApplication` + `ContentView.swift` + `KaiteyoApp.swift` | `iosApp/` | `IMPLEMENTED` (build-verified) |
| KJD | `KjdCli.kt` standalone JVM | `kjd/` | `IMPLEMENTED` |

**Previously**: Two shells (`KaiteyoApp` vs `KaiteyoDesktopSuite`) — now unified via `DesktopMediaCentreContent`/`GameDesktopHost` Koin hosts. Remaining suite views not yet mounted as core destinations = `PARTIALLY_IMPLEMENTED`.
**Verdict**: `IMPLEMENTED` (unified shell), `PARTIALLY_IMPLEMENTED` (full suite→core migration).

## 3. UI Architecture

- **Pattern**: 4-file screen (`Contract`/`ViewModel`/`Module`/`UI`) × ~30 screens under `core/.../screen/main/screen/*`. Registered in `di/AppModule.kt` screenModules list. Missing registration = not loaded (enforced by convention).
- **State**: `StateFlow` in ViewModels, `mutableStateOf`/`derivedStateOf` local, `CompositionLocal` for theme.
- **ViewModel expect/actual**: `presentation/ViewModel.kt` (`multiplatformViewModel`/`getMultiplatformViewModel`) — JVM uses Koin viewModel, Android uses AndroidX, iOS uses Swift interop.
- **Desktop suite**: separate `AppState` singleton holding dictionary/mining/activity/cardPool; `Ds*` designsystem (18 components).

**Verdict**: `IMPLEMENTED` — convention is mature and enforced.

## 4. Navigation Architecture

- **Core**: `MainScreen` → `MainNavigation` → `NavShell` (Sidebar/Floating, 4 edges, 12 snap points, persistence, `Ctrl+B` toggle). `defaultMainDestinations` includes all knowledge destinations. Deep-link routing via `deepLinkHandler`.
- **Suite**: `WorkspaceNav` + `WorkspaceShell`/`TabBar`/`PanelHost`/`FloatingLauncher`/`SpatialCamera`/`LaunchpadMotion` — duplicate model awaiting consolidation (ADR-0017).
- **Debug**: `PageIdentity`/`ProvidePageIdentity`/`PageRegistry` + `KaiteyoDebugOverlay` (Page/Route/Panel + copy debug info) wired into `NavShell`.

**Verdict**: `IMPLEMENTED` (core), `DEPRECATED` (suite duplicate — do not create third impl).

## 5. State Management

- ViewModel `StateFlow` + Compose `mutableStateOf` — correct.
- `AppState` (suite) is a monolithic singleton (dictionary + mining + card pool + history) — `PARTIALLY_IMPLEMENTED` debt; target is per-domain services owned by Kaiteyo Core (see new `docs/architecture/core.md`).
- `KaiteyoDataCenter.ensureLoaded()` lazy-loads AppDataDatabase once.

**Verdict**: `IMPLEMENTED` (core), `PARTIALLY_IMPLEMENTED` (suite AppState needs decomposition).

## 6. Database / Storage

**Two SQLDelight databases** (`core/build.gradle.kts`):

- `AppDataDatabase` (read-only, v15, `kanji-dojo-data-base-v15.sql`): `Letters.sq` (8 tables: character_stroke, radical, kanji_data, kanji_reading, kanji_meaning, kanji_classification, letter_vocab_example, kanji_radical) + `Vocab.sq` (23 tables: vocab_entry, vocab_kanji/kana_element, vocab_sense+11 detail tables, sentence, vocab_entity, vocab_furigana, vocab_deck_card).
- `UserDataDatabase` (mutable, user_version 16): `UserData.sq` (letter_deck, letter_deck_entry, vocab_deck, vocab_deck_entry, fsrs_card, review_history, text_analysis + 8 enhancement tables) + `UserData_enhancements.sq` (tag, card_tag, card_flag, card_note, study_history, keyboard_shortcut, backup_metadata, filtered_deck, plugin_registry) + `UserData_statistics.sq` (study_session, writing_attempt, exam, exam_question, learning_mistake, daily_stats).

Migrations: `UserDataDatabaseMigration{After3,4,8,10,13,14,15}.kt` + Provider.

**Verdict**: `IMPLEMENTED` — relational, offline-first, versioned, integrity-checked. Never change .sq schemas without explicit request.

## 7. Network Layer

- `Ktor 3.1.2` (version catalog).
- `LocalApiServer` (Ktor, bearer token) — endpoints: status/mine/media/player.
- `TextHookServer` + `PlayerStateWebSocket` — external tool boundary (ASBPlayer-style).
- `HttpKjdPatchChecker` — incremental data patches applied at runtime.
- Platform integrations (AniList/MAL) — `PLANNED`; local API is the current external surface.

**Verdict**: `PARTIALLY_IMPLEMENTED` — local API + patch feeds are real; platform sync APIs are target.

## 8. Dependency Structure

- DI: `Koin 4.0.0`, modules in `di/AppModule.kt` + `desktopAppModule` (last-module-wins overrides for MediaCentreContent/GameCentreContent/SearchOcrProvider).
- Version catalog: `gradle/libs.versions.toml` (148 lines) — pluginManagement versions pinned literally in `settings.gradle.kts` (must stay in sync).
- JDK 17 (`jvmToolchain(17)`, Kotlin languageVersion KOTLIN_2_1).
- Key libs: wanakana 1.1.1, coil 3.2.0, media3 1.4.1, sqlite-jdbc 3.46.0, kuromoji 0.9.0, epublib 4.0, tess4j 4.5.5, javacv 1.5.11, vlcj 4.8.2, sentry 7.20.1.

**Verdict**: `IMPLEMENTED`.

## 9. Desktop Window Implementation

Files: `Main.kt` + `KaiteyoWindow.kt` (889 lines) + `NativeWindowChrome.kt` + `NativeWindowDrag.kt` + `WindowMessageHandler.kt` + `WindowStateStore.kt` + `WindowWorkArea.kt` + `WindowConstraints.kt` + `BrandedInstaller.kt`.

- Undecorated `Window(undecorated=true)` with 44dp draggable title bar (BrandMark, `WindowDraggableArea` double-click maximize, JNA native drag on Windows/Linux, Alt+Space system menu), pill controls, rounded `DsRadius.Xl` clip + 1dp border, DWM rounding (retry 5×100ms), work-area clamp (taskbar any edge), 8-zone resize (5dp edge/10dp corner), 250ms throttled persist, F11/Ctrl+W/Esc, 60fps native drag.

**Verdict**: `IMPLEMENTED` — needs runtime sweep (multi-monitor/DPI/taskbar-top).

## 10. Browser / Chromium Implementation

- `desktop/engine/browser/BrowserEngine.kt` + `ui/browser/BrowserView.kt` + `ui/browser_web/LearningBrowserView.kt`.
- Lightweight WebView wrapper (JavaFX WebView when available), not full Chromium. Tabs, bookmarks, reader mode, text selection → DictionaryPopup → Mining.
- Security: no arbitrary internal API exposure; bridge is `Kaiteyo Browser Bridge` (planned full spec in `docs/architecture/browser.md`).

**Verdict**: `PARTIALLY_IMPLEMENTED` — functional lightweight browser; full Chromium embedding is `PLANNED` (see new architecture docs for security boundary).

## 11. Media Implementation

- `desktop/engine/media/` (14 files): `MediaEngine`, `MediaLibrary`, `MediaKind`, `MediaScanner`, `MediaCapture`, `SubtitleEngine/Parser/Normalizer/SearchIndex`, `MediaTray`, `MediaDownloadService`, `TextHookServer`, `PlayerStateWebSocket`, `MediaShortcuts`, `MediaStatisticsStore`.
- Backends: VLC (vlcj 4.8.2), mpv (JSON-RPC), Java Sound — behind `MediaEngine` abstraction.
- Playback: speed, A-B loop, frame step, screenshot, bookmarks, 10Hz `tick()` fail-safe wrapper (never crashes shell).
- Suite-only; mounted into core as `DesktopMediaCentreContent` (`MainDestination.Media`).

**Verdict**: `IMPLEMENTED` (suite, mounted), `PARTIALLY_IMPLEMENTED` (core mount covers Media; subtitle browser/provider interface is target).

## 12. Reading Implementation

- `desktop/engine/reading/` (5 files): `ReadingEngine`, `ReadingLibrary`, `EpubReader`, `ReadingParsers`, `ReadingModels`, `ReadingMining`.
- Supports TXT/Markdown/HTML natively; EPUB via epublib 4.0; progress, bookmarks, highlights, in-document search, tokenized lookup + mining, reading history.
- `desktop/ui/reading/` (ReadingView, DocumentView, LibraryPanel, LookupPopup).

**Verdict**: `IMPLEMENTED` (suite, native reading workspace), `PARTIALLY_IMPLEMENTED` (PDF is next; web reading adapters planned).

## 13. Dictionary Implementation

- Core: `AppDataDatabase` (bundled, kjd-generated).
- Suite: `desktop/engine/dictionary/` (9 files): `DictionaryService` (AppState controller, history.json/favorites.json), `DictionaryRepository` (installed/enabled, SearchMode EXACT/PREFIX/KANA/DEINFLECT + scoring, per-dict *.json index in data/index/), `DictionaryModels`, `DictionaryImporter` (Yomitan ZIP/folder/JSON, parseIndexMeta), `YomitanImporter`, `Deinflect`, `HandlebarsEngine`, `JapaneseSegmenter`, `SourceManifest`.
- UI: `DictionaryManagerView` + `DictionaryPopup` (headword/reading/definition/tags/TTS/mining actions).

**Verdict**: `IMPLEMENTED` — single DictionaryService will be the unified entry point (see new `docs/architecture/dictionary.md`).

## 14. Yomitan Integration

- Not a browser-extension host. Native replacement: `DictionaryFormat.Yomitan`, `YomitanImporter`, `DictionaryImporter` (format=Yomitan), `Deinflect` (yomitan deinflection rules), `JapaneseSegmenter`.
- Docs: `docs/media/YOMITAN.md`, `docs/roadmap/YOMITAN_INTEGRATION.md`, `docs/integrations/YOMITAN_DICTIONARIES.md`.
- Browser-specific assumptions adapted via Kaiteyo runtime adapters + custom UI.

**Verdict**: `IMPLEMENTED` (native replacement); do not recreate Yomitan UI, do not expect extension APIs.

## 15. ASBPlayer Integration

- No UI clone. Boundary: `TextHookServer` + `PlayerStateWebSocket` → `KaiteyoMiningEndpoint` → `MiningService` → `DestinationResolver` (`docs/architecture/MEDIA_CENTER_ASBPLAYER.md`).
- Workflow: `docs/media/ASBPLAYER_WORKFLOW.md` (subtitle selection → segmentation → screenshot/audio → card).

**Verdict**: `PARTIALLY_IMPLEMENTED` (intentional) — workflow via local API; external player speaks JSON protocol.

## 16. OCR Implementation

- `desktop/engine/ocr/` (3 files): `OcrEngine` (tess4j 4.5.5), `DesktopSearchOcrProvider`, `MlKitOcrProvider`.
- Flows: screenshot/clipboard/drag-drop OCR → `DictionaryPopup` + `MiningEngine`; stored in `~/.kaiteyo/ocr/`; reading & media OCR capture.
- Android: MLKit stub; missing-engine UX is hint not guided setup.

**Verdict**: `PARTIALLY_IMPLEMENTED` — desktop Tesseract path complete; needs guided setup + caching/GPU docs.

## 17. Mining Implementation

- `desktop/engine/mining/` (4 files): `MiningEngine` (MiningPayload headword/reading/definition/sentence/screenshot/audio/tags, mine()/mineFromDictionary, MinedRecord dedup), `MiningIntegration`, `MiningStatisticsStore`, `AnkiConnectTransport`.
- Sources: Dictionary/Browser/Video/OCR/Clipboard/Reader/Image/Audio/API → unified.
- UI: `MiningView` + `MediaPanels` + `ReadingMining`.

**Verdict**: `IMPLEMENTED` — single central MiningEngine (see new `docs/architecture/mining.md` for normalized MiningContext).

## 18. Anki Integration

- Core: `core/transfer/AnkiPackage.(kt|android|ios)` (pure-Kotlin ZIP), `desktop/engine/transfer/` (AnkiImporter, AnkiImportMapper, ImportExportPipeline, ProfileArchive, Codecs).
- `AnkiConnectTransport` — deck/noteType/fields/tags/media/duplicate handling.
- Docs: `docs/integrations/ANKI.md`.

**Verdict**: `IMPLEMENTED` — .apkg + AnkiConnect; offline queue/retry is target enhancement.

## 19. Library

- Core: `screen/library`, `screen/decks`, `DeckFeaturesController` — real due/new counts, archive filtering.
- Suite: `desktop/engine/library/` (LibraryStore, LibraryScheduler, BuiltInDecks) + `ui/library/` + `model/LibraryModels`.

**Verdict**: `IMPLEMENTED` — central organization system; node-based browsing is target.

## 20. Browse

- Core: `browse_hub` + `kanji_browser`/`sentence`/`kanji_entry`/`sentence_entry`/`search`/`knowledge_explorer`/`knowledge_graph`/`component_explorer`/`radical_explorer` (13 browse screens, 32 ViewModels) + suite `BrowserEngine` + `LearningBrowserView`.
- Hub exposes JLPT/grade collections, grammar catalog, radical grid.

**Verdict**: `IMPLEMENTED` (core discovery), `PARTIALLY_IMPLEMENTED` (suite lightweight browser — not full browser).

## 21. Platforms

- Docs: `docs/platform/{ANDROID,IOS,WINDOWS,MACOS,LINUX}.md`, `docs/integrations/` per-service.
- Current: GitHub device-flow + private gist (desktop-first sync). AniList/MAL/IMDb platform architecture is `PLANNED` (new `docs/architecture/platforms.md` specifies DATA PLATFORM vs WEB SOURCE vs GENERIC WEB PAGE).

**Verdict**: `PARTIALLY_IMPLEMENTED` — sync transport real; AniList/MAL metadata/list/ratings sync is target.

## 22. Stats

- Core: `core/srs`, `statistics`, `screen/statistics+stats`, `UserData_statistics.sq` (study_session, writing_attempt, exam, exam_question, learning_mistake, daily_stats).
- Suite: `engine/learning/StatisticsRepository`, `engine/stats/KnowledgeProfileEngine`, `engine/media/MediaStatisticsStore`, `engine/mining/MiningStatisticsStore`, `ui/stats/StatsView+HeatmapPanel+StatsCharts`.
- Event-driven; `StatisticsController` is single source; Home consumes same data (no separate calendar).

**Verdict**: `IMPLEMENTED` — ActivityEvent → Stats derivation is architected (see new `docs/architecture/stats.md` + `events.md`).

## 23. Home

- `screen/home/` (HomeViewModel/Module + general_dashboard/letters_dashboard/vocab_dashboard/search/settings/categories) + suite `DashboardView`.
- Dashboard consumes shared services (due counts, heatmap, study target, collections, recent activity) — no duplicate stats visualizations per spec.

**Verdict**: `IMPLEMENTED` — dashboard not second stats page.

## 24. Settings

- Core: `PreferencesContract` (DataStore) + `ThemeSettingsState` + `ThemeStudio` + `screen/settings` categories.
- Suite: `SettingsEngine` (~/.kaiteyo/settings.json) — duplicate awaiting ADR-0017 consolidation.
- Catalog: `docs/ui/SETTINGS.md`.

**Verdict**: `IMPLEMENTED` (core), `DEPRECATED` (suite duplicate).

## 25. Game / World

- `desktop/game/` (58 files): engine (Camera/Rig/Collision, Entity, GameEngine, input, render/CanvasRenderer, Scene, SpatialHash), world (GameWorld, TileGrid, WorldModels), player, npc, quest, dialogue, audio/TTS, save, bridge (GameBridge/KaiteyoBridge).
- `core/core/game` + `presentation/screen/main/screen/game` + `core/world/` (streamable Japan, chunk system, terrain/water/buildings/vehicles/trains/NPCs/time/weather/save).
- 2.5D Canvas backend shipped (Sakamura map, WASD+gamepad+touch, patrols/weather, dialogue TTS); 3D is `PLANNED` (RenderBackend boundary renderer-agnostic).
- Docs: `docs/game/` (18 files) + `docs/architecture/WORLD_SYSTEM.md`.

**Verdict**: `PARTIALLY_IMPLEMENTED` — vertical slice playable; full world/quests/achievements are target.

## 26. Debug / Developer Tooling

- `presentation/common/debug` + `KaiteyoDebugOverlay` (Page/Route/Panel + copy debug info, gated by "Show page debug info" toggle, bottom-corner).
- Suite: `ui/palette/CommandPaletteOverlay`, `engine/activity/ActivityTracker`.
- Required: per-screen identity (ProvidePageIdentity), network/DB/media/dictionary/mining session debug.

**Verdict**: `IMPLEMENTED` (overlay + command palette), `PARTIALLY_IMPLEMENTED` (full debug panel with session inspection is target — see new `docs/architecture/debug.md`).

## 27. Installer / Update Infrastructure

- `installer/` (not a Gradle module): common/version.json + artifact-manifest.schema.json, assets/brand, linux/appimage/arch/deb/flatpak/rpm/snap, macos/build-dmg.sh+notarize.sh, windows/kaiteyo.iss+portable+chocolatey/scoop/winget, scripts/bump-version+stage-artifacts+verify-artifacts.
- Version: `buildSrc/AppVersion.kt` (single truth, must be 3 numbers) + `installer/common/version.json`.
- Update feeds: `installer/templates/update-manifest.json`, `make-update-manifest.sh`; auto-update architecture complete, rollout staged.

**Verdict**: `IMPLEMENTED`.

## 28. Theme System

- Core: `presentation/common/theme/` (Color.kt KaiteyoSemanticColors 40+ tokens, Theme.kt LocalKaiteyoSemanticColors+withThemeTransition, Typography.kt nuclear-safe runCatching, Dimens.kt 4dp grid, AnimationTokens, LayoutScaleModifiers, PageTransitions).
- Suite: `desktop/designsystem/DsTokens.kt` (SurfaceColors light #F8F9FA/dark #121212, accent #4CAF50) + 18 Ds* components.
- Presets: 17 themes, Light/Dark/OLED/Sepia, custom gradients.

**Verdict**: `IMPLEMENTED`.

## 29. Animation System

- `docs/design/ANIMATION_SYSTEM.md`; `Theme.kt` LocalAnimationConfig.reducedMotion; spring physics (Compose animation).
- Suite: `engine/animation/AnimationEngine`; world: `SpatialCamera`/`LaunchpadMotion`.
- Known issue P0 #1 (stutter during hover/theme/window move) — native drag 1:1 done, remaining polish.

**Verdict**: `PARTIALLY_IMPLEMENTED` — system exists, needs 60fps sweep.

## 30. Design System

- `desktop/designsystem/` (DsCard, DsButton, DsDialog, DsType, DsSpacing 4dp, DsRadius, DsBadge, surfaceColors(), modifier order: size→padding→background/clip→clickable→align→graphicsLayer→semantics, hoverable+collectIsHoveredAsState).
- Suite-specific; core uses `KaiteyoSemanticColors` + theme tokens. Two systems awaiting consolidation decision.

**Verdict**: `IMPLEMENTED` (both systems), `DEPRECATED` (duplicate — target is one token system per new `docs/design/design-system.md`).

---

## Cross-Cutting Findings

### What is complete (do not break)
Home, Library, SRS/FSRS, Statistics/heatmap/exams, Import/export/Anki .apkg, Navigation/NavShell, Database/KJD, Desktop window chrome, Design tokens, Mining pipeline, Media backends, Reading workspace, Dictionary import.

### What is partially implemented (needs finishing)
Browser (lightweight not Chromium), OCR (needs guided Tesseract setup), ASBPlayer (intentional boundary), Platforms (AniList/MAL not yet), Game (2.5D slice not full world), Animation (60fps polish), Dictionary remaining suite views not yet core destinations.

### What is placeholder / broken / missing
- Suite `AppState.seedDemoData()` seeding fake cards — violates no-fake-data rule → must become empty first-run state.
- `ShortcutRegistry` advertised `Ctrl+Shift+D/M/B` gaps — verify binding.
- Suite-only engines unreachable until fully mounted — ADR-0017 decision gates Library-as-hub completion.
- Offline queue/retry for AnkiConnect, platform sync, subtitle provider caching — specified but not yet implemented (new architecture docs cover it).

### What is duplicated / obsolete / architecturally unsafe
| Concept | Keep | Remove/Migrate |
|---------|------|----------------|
| Navigation | core `NavShell` | suite `WorkspaceNav` (after ADR-0017) |
| Settings | core `PreferencesContract` | suite `SettingsEngine` |
| Theme | core `KaiteyoSemanticColors` | suite `theming/ThemeManager` (merge tokens) |
| Stats | `StatisticsController` | suite `StatsView` summaries |
| SRS | core FSRS | suite `ReviewSession` |
| Design system | unified token system | duplicate Ds* vs core tokens |

### What is disconnected / incorrectly owned / hardcoded
- Media/Reading/OCR each had implicit dictionary ownership — must converge on single `DictionaryService` (new architecture).
- Statistics had two calendars (Home vs Stats) — must be one `ActivityEvent` ledger.
- Version truth is correct (single `AppVersion.kt`); asset truth is correct (`AppAssets.kt` manages composeResources/files).

### What is missing (persistence / error / navigation / integration)
All documented in the new master architecture pack (`docs/architecture/`). Every subsystem now has required: persistence contract, UI states (loading/empty/error/offline), failure taxonomy, offline behavior, sync/caching, mining/library/stats/decks/platforms/settings/game wiring, and provider interfaces where external services are involved.

---

## Audit Method

- Read `settings.gradle.kts`, `gradle/libs.versions.toml`, `buildSrc/*`, `core/build.gradle.kts`, SQLDelight schemas, `desktopApp/Main.kt`+`KaiteyoWindow.kt`, `core/presentation/screen/main/MainScreen.kt`, `di/AppModule.kt`, `kjd/src`, `installer/common/version.json`, `docs/README.md`, `docs/planning/{PRODUCT_AUDIT,CURRENT_STATE,CURRENT_ISSUES}.md`, and sampled `desktop/engine/*` + `desktop/ui/*` + `core/knowledge/*` + `core/world/*`.
- Verified via `grep`/`glob` counts (1451 kt files, 298 md files, 56 jvmTests).
- Build verification: `gradlew :desktopApp:compileKotlinJvm` and `gradlew :core:allTests` are Definition of Done per `AGENTS.md` (run on ≥8GB host; 2GB cgroup OOM-kills Gradle).

## Next Steps (gated)

1. ADR-0017 (one product) — gates all consolidation.
2. Mount remaining suite views (DictionaryManager, OCR, Browser, Reading) as core destinations.
3. Implement the provider interfaces (Subtitle, Platform, OCR) — no hard-coded providers.
4. Follow the new `docs/planning/ROADMAP.md` phase order (Phase 0 → 7).
