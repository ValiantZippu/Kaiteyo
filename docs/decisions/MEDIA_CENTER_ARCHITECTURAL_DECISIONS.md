# Media Center — Architectural Decisions

> ADR-style, one per major choice: REFERENCE → REQUIREMENT → DECISION → WHY → IMPLEMENTATION → TEST

## ADR-1: One mining pipeline, not three

- **Reference:** jidoujisho per-screen mining duplication.
- **Requirement:** Kaiteyo + Anki + Both configurable, future producers (Yomitan, ASBPlayer, Reader, OCR, Browser, Game) share same destination.
- **Decision:** `MiningPayload → MiningEngine.mine() → DestinationResolver → MiningIntegrationManager.forward()` with `CardDestination` enum.
- **Why:** Single provenance, single retry queue, no ghost buttons.
- **Implementation:** `MiningEngine.kt:148`, `MiningIntegration.kt`, `AnkiConnectTransport.kt`.
- **Test:** `MediaMiningDestinationTest` ( Both+unreachable → pending, Anki-only fallback).

## ADR-2: Keep MediaEngine, delete prototype

- **Reference:** directive §5 DELETE.
- **Requirement:** Native Kaiteyo subsystem, not copied app.
- **Decision:** MEDIA-SPECIFIC OLD CODE (`MediaCentreScreen.kt` 40 fakes) → REMOVE; SHARED VALID (`MediaEngine`, `MediaLibrary`, `PlaybackBackend`, `SubtitleEngine`, `MiningEngine`) → KEEP; BROKEN SHARED → REFACTOR (EventLog emits, graph full index).
- **Why:** Engine is the most complete subsystem (2426-line controller, fail-safe tick, capability gating) — rewrite would lose correctness.
- **Implementation:** `core/.../MediaCentreScreen.kt` honest placeholder, `desktopApp/MediaCentreDesktopHost.kt` still mounts `MediaView`.
- **Test:** `MediaEngineTickSafetyTest` (opening Media never closes app).

## ADR-3: Player abstraction via capabilities

- **Reference:** jidoujisho VLC/libmpv layer.
- **Requirement:** Desktop (VLC/mpv) + Android (future) + honest degradation when no backend.
- **Decision:** `PlaybackBackend` interface + `PlaybackCapability` enum + `BackendManager` probe; UI gates every control on capability.
- **Why:** No fake sliders when backend can't seek/screenshot.
- **Implementation:** `engine/playback/PlaybackModels.kt`, `VlcBackend`, `MpvBackend`, `AudioBackend`.
- **Test:** tick safety + probe UI.

## ADR-4: Pure subtitle parsers

- **Reference:** jidoujisho SRT/ASS.
- **Requirement:** SRT/ASS/SSA/VTT where practical, malformed → `SubtitleInvalid` never crash.
- **Decision:** `SubtitleParser` pure `String → Track`, bounded errors, `SubtitleEngine.cueIndexAt` binary search.
- **Why:** Backend-independent, testable, survivable.
- **Implementation:** `SubtitleParser.kt`, `SubtitleEngine.kt`.
- **Test:** `SubtitleEngineTest`, `SubtitleParserFuzzTest` (10k, garbage).

## ADR-5: EventLog as single source

- **Reference:** kanji-heatmap STUDY EVENT → HEATMAP.
- **Requirement:** Unified statistics/home/history/mining, no UI counters.
- **Decision:** `EventLog` append-only (`MediaStarted/MediaEnded/SubtitleSelected/DictionaryLookup/CardMined/BookmarkAdded`) + `MediaStatisticsStore`/`MiningStatisticsStore` per-day buckets.
- **Why:** Re-runnable derived metrics, heatmap-ready.
- **Implementation:** `EventLog.kt`, `MediaEngine.onBackendEvent`, `StatsView` + `MediaImmersionHeatmap`.
- **Test:** `EventLogTest`.

## ADR-6: Full subtitle index, idempotent graph

- **Reference:** kanji-heatmap aggregation but for immersion.
- **Requirement:** “Where did I learn this word?” → episode + timestamp, at 10k lines.
- **Decision:** `MediaNodeGraph.indexSubtitleTrack` (full cues, `idx-*` scene, preserves `exposureCount=1` via ±500ms, `clearMedia`).
- **Why:** Mined provenance alone is too sparse; user wants passive exposure.
- **Implementation:** `MediaNodeFamily.kt:115`, `MediaEngine.indexCurrentSubtitlesToGraph`.
- **Test:** `MediaNodeGraphIndexTest`.

## ADR-7: Browser is not a downloader

- **Reference:** directive §19 built-in browser + §20 download where legal.
- **Requirement:** Search + media discovery + download with DRM/legal constraints, no hard-coded sites.
- **Decision:** `BrowserEngine` (tabs, reader, WebView) + `MediaDownloadService` (http(s)-only, sanitized filename, daemon streaming, Queued→Completed/Failed).
- **Why:** Avoids unrestricted downloader that ignores restrictions.
- **Implementation:** `BrowserEngine.kt`, `MediaDownloadService.kt`, `AppState.mediaDownloads`.
- **Test:** manual http(s) enqueue, cancel/pause.
