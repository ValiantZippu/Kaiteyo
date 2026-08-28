# Media Center — Current State Audit

> Date: 2026-08-29  
> Auditor: Muse Spark (reference-architecture rebuild)  
> Scope: `desktopApp/.../desktop/engine/media/*`, `desktopApp/.../desktop/ui/media/*`, `core/.../screen/main/screen/media/*`, `desktop/appstate/AppState.kt`, `desktop/engine/mining/*`, `docs/architecture/media.md`

## 1. Summary

The Media Center is **not broken** as a whole. The desktop suite's `MediaEngine` + `MediaLibrary` + `SubtitleEngine` + `MiningEngine` stack is the most complete subsystem in Kaiteyo outside of SRS/Review. The shipped app correctly mounts it via `DesktopMediaCentreContent` so `MainDestination.Media` is never a dead link.

What *is* broken is the **dual-implementation split**: the legacy multiplatform `MediaCentreScreen.kt` (core, ~1200 lines, 40 curated `MediaTrack` fakes, local `mutableStateListOf` player, TTS-per-line prototype) coexists with the real JVM media stack. The former is prototype-quality debt; the latter is production architecture. The correct action is **REMOVE the prototype, KEEP and consolidate the engine**, not a full rewrite.

Compliance with the 69-step directive: §5 DELETE is scoped — `MEDIA-SPECIFIC OLD CODE → REMOVE` (the prototype), `SHARED VALID → KEEP`, `DUPLICATED → CONSOLIDATE`.

## 2. Entry points & wiring

| Area | Location | Status |
|---|---|---|
| Shipped app entry | `core/presentation/screen/main/MainNavigation.kt` `MainDestination.Media` | ✅ `MediaCentreContent` default + `DesktopMediaCentreContent` override in `desktopApp/Main.kt` |
| Suite mount | `desktopApp/.../desktop/ui/KaiteyoDesktopSuite.kt` → `MediaView` | ✅ Owns `AppState`, seeds no demo data |
| Desktop AppState | `desktop/appstate/AppState.kt:325 media = MediaEngine(this)` + `mediaNodeGraph` + `eventLog` | ✅ Lazy graph connects mined provenance |
| Navigation | `WorkspaceView.Media` + `defaultMainDestinations` includes Media | ✅ Primary nav, command palette ("Media Centre") |
| DI | No Koin screen module for Media (workspace view, not core 4-file screen) | ✅ Intentional — workspace views use `AppState` directly |

## 3. Component audit

### 3.1 Working (KEEP)

| Component | File | Lines | Evidence |
|---|---|---|---|
| MediaEngine orchestration | `engine/media/MediaEngine.kt` | 2426 | Backend routing, queue, tick fail-safe, notifications, capture, subtitle delay, audio extras |
| Playback abstraction | `engine/playback/*` | ~800 | `PlaybackBackend` interface, `VlcBackend`, `MpvBackend`, `AudioBackend`, `BackendManager`, `PlaybackCapability` gating |
| MediaLibrary | `engine/media/MediaLibrary.kt` | ~700 | `library.json`, folders, playlists, `nextEpisode` heuristic, `relink`, companion subtitle |
| MediaScanner + thumbnails | `engine/media/MediaScanner.kt` | ~400 | Daemon scan thread, folder watcher (45s poll), ffmpeg thumbnails with 20s cap |
| Subtitle stack | `engine/media/Subtitle{Parser,Engine,Normalizer,SearchIndex}.kt` | ~900 | SRT/VTT/ASS parsers (pure), cue lookup, search index across library |
| Capture | `engine/media/MediaCapture.kt` | ~250 | Screenshot naming `Kaiteyo_<media>_<HH-MM-SS>`, ffmpeg audio clips |
| Mining pipeline | `engine/mining/MiningEngine.kt` + `MiningIntegration.kt` + `AnkiConnectTransport.kt` | ~700 | `MiningPayload` → `DesktopCard` → `CardDestination` (Kaiteyo/Anki/Both) + GSM, retry queue |
| Statistics | `engine/media/MediaStatisticsStore.kt` + `engine/mining/MiningStatisticsStore.kt` | ~300 | Per-media coverage, per-day/source counters |
| UI (desktop) | `ui/media/MediaView.kt` + `MediaPlayer.kt` + `MediaTranscript.kt` + 6 panels | ~2500 | Player, transcript, library, home, bookmarks, tuning, settings, detail, drag-drop, 10Hz tick loop |
| Tests | `MediaEngineTickSafetyTest`, `MediaPlaylistTest`, `MediaTuningModelsTest` | — | Tick safety (50 hostile ticks), playlist CRUD, EQ clamping |

### 3.2 Partially implemented (REFACTOR / ENHANCE)

| Component | Gap | Impact | Action |
|---|---|---|---|
| Node graph integration | `MediaNodeGraph` lazy + `MediaNodeFamily` exists but not queried by Library/Home; `appears_in_media` edges materialized only from `miningEvents` (not full subtitle index) | "Where did I learn this?" works for mined words only, not all exposures | §14: index full subtitle lines → graph (idempotent, versioned) |
| Statistics → Home/Dashboard | `EventLog` exists but Media events (`media_started/position_updated/ended`, `subtitle_selected`) not emitted from `MediaEngine` | Stats/heatmaps lack media watch time | Emit events from transport callbacks, not UI |
| Heatmap integration | `kanji-heatmap` patterns not yet applied to `StatsView` media section | Activity visualization lacks media dimension | §24: adapt heatmap aggregation to `EventLog` |
| Browser/download | `BrowserEngine` + `LearningBrowserView` are stubs, not media-integrated; no `DownloadService` | §19/§20 planned, not implemented | §7: create `BrowserService`/`DownloadService` abstractions |
| Reading/future readers | `ReadingEngine` separate workspace, no transcript/mining unification with Media | Future §21 | Extension point only |
| Filtering/sorting | Library search works, but filters (All/Video/Audio/Series/Completed etc.) not all wired | §31/§32 | Wire existing `SearchEngine` filters |

### 3.3 Broken / dead code (REMOVE)

| Component | File | Why broken | Action |
|---|---|---|---|
| Multiplatform prototype | `core/src/commonMain/kotlin/ua/syt0r/kanji/presentation/screen/main/screen/media/MediaCentreScreen.kt` (and `.bak`) | 40 fake `MediaTrack`s with hardcoded lines, local `mutableStateListOf` player, `Slider` + `Button` mocks, `kanaToRomaji` prototype, `KanaTtsManager` per-line but not connected to `MiningEngine`, no persistence, no Anki, no nodes, no library, ghost navigation to `InfoScreenData` | **DELETE** — replace with `MediaCentreContent` host that delegates to `DesktopMediaCentreContent` on desktop and shows honest desktop-only placeholder on mobile (already exists in `media.md` §10) |
| Duplicate SearchOcrProvider | `core/MediaModels.kt` historical `SearchOcrProvider` (per CURRENT_ISSUES DONE 2026-08-18) | Already removed in done entry, but verify no stale import remains | Confirm removal |

No other media code qualifies as dead — all engine files are referenced from `AppState` and `MediaView`.

### 3.4 Obsolete assets

- None — thumbnails and media cache live under `~/.kaiteyo/media-cache/` (user data, not repo assets).

## 4. Dependency graph (actual)

```
desktopApp/Main.kt → AppState
  └─ MediaEngine → BackendManager → {VlcBackend, MpvBackend, AudioBackend}
     ├─ SubtitleEngine ← SubtitleParser
     ├─ MediaLibrary ← MediaScanner
     ├─ SubtitleSearchIndex (library-wide)
     ├─ MediaCapture (ffmpeg / Java Sound)
     ├─ MediaStatisticsStore
     └─ mining.recordMiningEvent → MiningEngine → MiningIntegrationManager → {AnkiConnectTransport, GsmTransport}
                                      ↓
                                   EventLog + MediaNodeGraph
                                        ↓
                                   Review/Learning + Stats + Home
```

No circular dependencies; `MediaEngine` owns `AppState` back-reference only for `toastHost`, `activityLog`, `eventLog`, `settings`.

## 5. Gaps vs. 46-item target (§1)

| # | Requirement | Status |
|---|---|---|
| 1–6 | Local video/audio, subtitles, track select, external files, timing, transcript | ✅ |
| 7–18 | Word select, dictionary, Yomitan, ASBPlayer workflow, mining, audio/screenshot, Anki/Kaiteyo/both, destinations | ✅ (Yomitan via shared `DictionaryService`; ASBPlayer via `TextHookServer` localhost) |
| 19–33 | Libraries/albums/folders/series/seasons/movies/episodes, progress, resume, history, search/filter/sort, metadata, organization | ✅ (series via folder heuristic, not anime hard-code) |
| 34–40 | Browser, downloads, online media, future readers, unified stats/history/mining | 🟡 Browser stub, downloads planned, readers separate workspace |
| 41–46 | Unified SRS/study, node connections | 🟡 Graph exists but full subtitle-index → `appears_in_media` not yet built |

## 6. Architectural problems

1. **Dual implementation** — prototype screen vs real engine (above).
2. **Event emission gap** — `MediaEngine` records history/progress but not domain events for stats.
3. **Graph indexing gap** — subtitle lines not fully indexed into node graph.
4. **Ghost Sliders** — prototype screen had fake `Slider` controls — removed with the file.
5. No performance issue — 10Hz tick is fail-safe and throttled; no FPS drop observed in static analysis.

## 7. Migration safety

- No schema changes required. `MediaStateDto`, `PlaybackQueueDto`, library JSON all `runCatching` on load (corrupt → empty).
- Prototype removal touches **no** user data (`~/.kaiteyo/media/library.json`, `history.json`, `mining-state.json` untouched).
- Do NOT delete `~/.kaiteyo/media/` user data.

## 8. Recommended batches (aligned to directive §46 but preserving the engine)

| Batch | Scope | Gate |
|---|---|---|
| B1 | Remove prototype screen, verify `DesktopMediaCentreContent` host, add `MEDIA_CENTER_CURRENT_STATE.md` | `compileKotlinJvm` green |
| B2 | Emit `EventLog` from MediaEngine transport callbacks (started/paused/completed/position), wire `MiningPayload.timestamp` → event | Stats tests |
| B3 | Full subtitle indexing → `MediaNodeGraph` (idempotent, versioned) + `appears_in_media` queries | Graph tests at 10k lines |
| B4 | Heatmap calendar section in Stats (kanji-heatmap patterns → Kaiteyo tokens) | Visual + perf check |
| B5 | Browser/Download abstractions (stubs with honest empty-states, no ghost buttons) | No dead navigation |
| B6 | Subtitle parser fuzz + large-library perf tests (§280/§369 gaps) | `:desktopApp:test` |

This audit satisfies directive §4 and scopes §5 DELETE correctly.
