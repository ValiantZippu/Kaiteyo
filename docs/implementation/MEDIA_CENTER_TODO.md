# Media Center — TODO

> Organized per directive §44. Statuses: PLANNED / IN_PROGRESS / IMPLEMENTED / VERIFIED / BLOCKED / DEFERRED

## P0 — Critical foundation (B1 — DONE)

| # | Objective | Files/Modules | Completion criteria | Status |
|---|---|---|---|---|
| P0-1 | Remove prototype screen, replace with honest placeholder | `core/.../media/MediaCentreScreen.kt` | No `MediaTrack`/`ImmersionLine` fakes, no fake sliders, `DefaultMediaCentreContent` shows desktop-only empty state, `.bak` deleted | IMPLEMENTED |
| P0-2 | Verify Koin override still mounts real MediaView | `desktopApp/Main.kt`, `MediaCentreDesktopHost.kt` | `MainDestination.Media` → `DesktopMediaCentreContent` → `MediaView` on desktop; fallback honest on failure | IMPLEMENTED |
| P0-3 | Reference architecture + current-state docs | `reference/*`, `docs/architecture/MEDIA_CENTER_CURRENT_STATE.md`, `MEDIA_CENTER_ARCHITECTURE.md` | 4 reference analyses + mapping + audit committed | IMPLEMENTED |

## P1 — Core media

| # | Objective | Dependencies | Implementation notes | Testing | Status |
|---|---|---|---|---|---|
| P1-1 | Emit EventLog from MediaEngine transport (media_started/paused/completed/position) | `EventLog`, `MediaEngine.onBackendEvent` | Don't increment UI counters; events feed stats/home | Unit: event count per session | IMPLEMENTED (`MediaEngine.onBackendEvent`, `openLookup`, `addBookmark` emit `MediaStarted/MediaEnded/DictionaryLookup/SubtitleSelected/BookmarkAdded`) |
| P1-2 | Subtitle → node indexing (idempotent, versioned) | `SubtitleSearchIndex`, `MediaNodeGraph` | All lines → `appears_in_media` edges; re-index safe | Perf: 10k lines query budget | IMPLEMENTED (`MediaNodeGraph.indexSubtitleTrack`, `MediaEngine.indexCurrentSubtitlesToGraph`, live graph updates) |

## P2 — Mining (already implemented, verify E2E)

| # | Objective | Status |
|---|---|---|
| P2-1 | End-to-end: import video → subtitle sync → transcript select → dictionary → mine → Kaiteyo → Anki → Both → disable Kaiteyo → still mines to Anki → resume → history | IMPLEMENTED (automated `MediaMiningDestinationTest` + `MediaNodeGraphIndexTest`; manual 27-step pending runtime) |

## P3 — Integrations

| # | Objective | Status |
|---|---|---|
| P3-1 | Browser/Download abstractions (honest empty-states, no ghost buttons) | IMPLEMENTED (`BrowserEngine` tabs/reader + `MediaDownloadService` Queued/Downloading/Paused/Completed/Failed/Cancelled, `AppState.mediaDownloads`) |
| P3-2 | Yomitan/ASBPlayer boundary hardening (localhost auth) | IMPLEMENTED (TextHookServer, PlayerStateWebSocket) |

## P4 — UX polish

| # | Objective | Status |
|---|---|---|
| P4-1 | Heatmap calendar section in Stats (kanji-heatmap → Kaiteyo tokens) | IMPLEMENTED (`StatsView.MediaImmersionHeatmap` 52-week watchMs → level, `MediaStatisticsStore`) |
| P4-2 | Subtitle parser fuzz + corrupt-file tests | IMPLEMENTED (`SubtitleParserFuzzTest` 8 cases) |
| P4-3 | Large-library perf tests | IMPLEMENTED (`MediaLargeLibraryPerfTest`: 10k cue binary search 5k queries <2s, 20k parse <5s, 3k-item search 100 queries <2s) |

## P5 — Future capabilities (deferred)

| # | Objective | Status |
|---|---|---|
| P5-1 | Manga/news/novel reader extension points | DEFERRED (ReadingEngine stub) |
| P5-2 | Unified reading/media workflows | DEFERRED |

**Verification loop (directive §64):** after each batch — inspect → check imports → single-file syntax review (no bulk `gradlew` per current constraint) → update docs → next batch. Full `compileKotlinJvm`/`jvmTest` verified on last `--offline` run (2m36s SUCCESS) — re-verification deferred to CI.
