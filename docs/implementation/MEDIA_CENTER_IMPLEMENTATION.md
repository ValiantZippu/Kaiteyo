# Media Center — Implementation

> Status: IMPLEMENTED (P0-P4), VERIFIED pending runtime  
> Batches: B1 clean boundary → B2 media import/library → B3 subtitles/transcript → B4 dictionary/Yomitan → B5 mining destinations → B6 nodes/stats → B7 browser/download → B8 polish → B9 testing → B10 cleanup

## Batches

| Batch | Scope | Gate |
|---|---|---|
| B1 | Remove `MediaCentreScreen.kt` prototype (40 fakes) → honest `DefaultMediaCentreContent`; keep `MediaEngine` | `compileKotlinJvm` |
| B2 | `MediaLibrary` + `MediaScanner` + thumbnails (ffmpeg `scale=320:-2`), playlists/folders, companion subtitle | `MediaLibraryTest`, `MediaPlaylistTest` |
| B3 | `SubtitleParser` (SRT/VTT/ASS), `SubtitleEngine` (cueIndexAt binary search, `globalOffsetMs`), transcript tap→seek | `SubtitleEngineTest`, `SubtitleParserFuzzTest` |
| B4 | `DictionaryService` (Yomitan ZIP) reused, `MediaEngine.selectToken` → `openLookup` | popup + mining |
| B5 | `MiningEngine.mine` + `MiningIntegrationManager` + `AnkiConnectTransport`/`GsmTransport` + `CardDestination` | `MediaMiningDestinationTest` |
| B6 | `MediaNodeGraph.indexSubtitleTrack` (idempotent) + `EventLog` (MediaStarted/MediaEnded/SubtitleSelected/BookmarkAdded) + `MediaStatisticsStore` + `MiningStatisticsStore` + `MediaImmersionHeatmap` | `MediaNodeGraphIndexTest` |
| B7 | `BrowserEngine` (already) + `MediaDownloadService` (Queued/Downloading/Paused/Completed/Failed/Cancelled) + `AppState.mediaDownloads` | honest empty-states, no site hard-coding |
| B8 | `Ds*` theming, `WorkspaceShell` responsive, cinema/fullscreen, `Battery/Shared` vs isolated window | visual audit |
| B9 | `MediaEngineTickSafetyTest`, `MediaEngineLifecycleTest`, `MediaPlaylistTest`, parser fuzz, destination E2E | `:desktopApp:jvmTest` |
| B10 | Remove `.bak`, `AtomicBoolean` unused import, consolidate duplicated `SearchOcrProvider` | clean compile |

## Actual files

- Core host: `core/.../media/MediaCentreScreen.kt` (120 lines, Koin `mediaCentreModule`)
- Desktop host: `desktopApp/MediaCentreDesktopHost.kt` (`DesktopMediaCentreContent` → `MediaView`)
- Engine: `MediaEngine.kt` (event emits + graph indexing), `MediaNodeFamily.kt` (full index), `MediaDownloadService.kt`, `MediaLibrary.kt`, `SubtitleParser.kt`, `SubtitleEngine.kt`
- Mining: `MiningEngine.kt`, `MiningIntegration.kt`, `AnkiConnectTransport.kt`
- Stats: `EventLog.kt`, `MediaStatisticsStore.kt`, `StatsView.kt` (+ `MediaImmersionHeatmap`), `HeatmapPanel.kt`
- Tests: `SubtitleParserFuzzTest.kt`, `MediaMiningDestinationTest.kt`, `MediaNodeGraphIndexTest.kt`, existing tick/lifecycle/playlist

## Platform

Desktop (Windows/Linux/macOS) is primary; mobile `DefaultMediaCentreContent` is honest desktop-only placeholder (no dead navigation). `PlaybackCapability` gates every control; `PlaybackError` (sealed, `userMessage`) never surfaces raw backend exceptions.

## Verification (pending host with ≥8GB)

`gradlew :desktopApp:compileKotlinJvm` + `:desktopApp:jvmTest --tests "*Media*"` + manual 27-step chain.
