# Media Center Mapping — Reference Concept → Kaiteyo

> Columns: Reference → Kaiteyo Equivalent → Module → Data Model → Service → UI → Node Connection → Statistics → Status

| # | Reference Concept | Kaiteyo Equivalent | Module | Data Model | Service | UI | Node | Stats | Status |
|---|---|---|---|---|---|---|---|---|---|
| 1 | Local video/audio | `MediaItem` + `MediaKind` | `desktop/engine/media` | `MediaItem(path, name, kind, isRemote)` | `MediaLibrary.addFile/addRemote` | `MediaView` + file/folder pickers | `media_source` node | `EventLog: media_started` | IMPLEMENTED |
| 2 | External subtitles (SRT/ASS/VTT) | `SubtitleTrack` + `SubtitleCue` | `desktop/engine/media` | `SubtitleCue(startMs, endMs, text)` | `SubtitleParser`, `SubtitleEngine` | `MediaTranscript` | `subtitle_line` node | `coverageFor()` | IMPLEMENTED |
| 3 | Subtitle track selection | `activeTrack` + `secondaryCue` | `desktop/engine/media` | `SubtitleTrack` id | `MediaEngine.selectSubtitleTrack` | Settings → Media | `subtitle_track` | — | IMPLEMENTED |
| 4 | Transcript interaction (cue→seek, follow) | `activeCueIndex` + 10Hz tick | `desktop/engine/media` | `activeCue` | `MediaEngine.tick()` | `MediaTranscript` | `appears_in_media` (via index) | — | IMPLEMENTED |
| 5 | Word selection | `selectedTokens` + anchor shift/drag | `desktop/engine/media` | `SegmentToken` | `DictionaryService` segmenter | `DictionaryPopup` | `vocab`/`kanji` node | `dictionaryLookupCount` | IMPLEMENTED |
| 6 | Dictionary lookup (Yomitan) | `DictionaryService` (Yomitan ZIP) | `desktop/engine/dictionary` | `DictionaryEntry`, `DictionaryMatch` | `DictionaryRepository` | `DictionaryPopup`, `MiningDialog` | `vocab` edge | `EventLog: word_looked_up` (planned) | IMPLEMENTED |
| 7 | ASBPlayer workflow | `TextHookServer` + `PlayerStateWebSocket` | `desktop/engine/media` | `PlayerStateSnapshot` | `TextHookServer`, `PlayerStateWebSocket` | — (external boundary) | `mined_from` | — | IMPLEMENTED (stub, localhost) |
| 8 | Sentence mining | `MiningPayload` → `DesktopCard` | `desktop/engine/mining` | `MiningPayload(headword, sentence, mediaRef…)` | `MiningEngine.mine()` | `MiningDialog`, subtitle context menu | `mined_from` | `MiningStatisticsStore.recordMine` | IMPLEMENTED |
| 9 | Audio extraction | `AudioClip` + ffmpeg range export | `desktop/engine/media` | `AudioClip(startMs,endMs)` | `MediaCapture.extractAudioClip` | Player → clip button | `clip` node | — | IMPLEMENTED |
| 10 | Screenshot/context | `lastScreenshotPath` + `MediaCapture.snapshot` | `desktop/engine/media` | file path in `MiningPayload` | `MediaCapture` | Toolbar camera button | `screenshot` node | — | IMPLEMENTED |
| 11 | Anki export | `AnkiConnectTransport` | `desktop/engine/mining` | `AnkiConfig(host,port,key)` | `AnkiConnectTransport.send` | Settings → Media → Anki | — | `MinedRecord.ankiStatus` | IMPLEMENTED |
| 12 | Kaiteyo deck export | `DesktopCard` in `AppState.cards` | `desktop/appstate` | `DesktopCard(character, meaning, note)` | `AppState.addCard` | Review queue | `card` node | `ReviewLog` | IMPLEMENTED |
| 13 | Kaiteyo+Anki dual export | `CardDestination.Both` | `desktop/engine/mining` | `CardDestination` enum | `MiningIntegrationManager.forward` | Mining dialog destination selector | Both | Both | IMPLEMENTED |
| 14 | Configurable destinations | `media.mine-destination` setting | `desktop/engine/settings` | `media.anki.enabled`, `media.mine-destination` | `MiningEngine.resolveDestination` | Settings → Media | — | — | IMPLEMENTED |
| 15 | Media libraries / albums / folders / series/seasons | `MediaLibrary` + `Playlist` + `PlaylistFolder` + `MediaFolder` | `desktop/engine/media` | `MediaPlaylist(folderId)`, `MediaFolder` | `MediaLibrary.*` + `MediaScanner` | `MediaLibraryPanel`, `MediaHome` (Continue Watching, Pinned, Collections) | `series`/`season`/`episode` (via `MediaNodeFamily`) | — | IMPLEMENTED |
| 16 | Movies/Episodes | `MediaItem.episode` + `seriesKey`/`nextEpisode` | `desktop/engine/media` | `episode`, `collection` | `MediaLibrary.nextEpisode` | Library detail | `episode`/`movie` | — | IMPLEMENTED |
| 17 | Watch progress / resume | `lastPositionMs` + `PlaybackQueueDto` | `desktop/engine/media` | `lastPositionMs`, `queue.json` | `MediaEngine.openItem` resume prompt + `tick` save | Resume dialog, Continue Watching | `media_position_updated` | `EventLog` | IMPLEMENTED |
| 18 | Playback history | `WatchHistoryEntry` | `desktop/engine/media` | `history.json` | `MediaLibrary.recordHistory` | MediaHome → Watched | — | — | IMPLEMENTED |
| 19 | Search | `MediaLibrary.search` + `SubtitleSearchIndex` | `desktop/engine/media` | `SubtitleSearchHit` | `MediaEngine.subtitleSearch` | Toolbar search → Browse view | — | — | IMPLEMENTED |
| 20 | Filtering/Sorting | `MediaHomePanel` browse filters | `desktop/ui/media` | — | `MediaLibrary` ops | `MediaLibraryPanel` | node queries (planned) | — | PARTIAL |
| 21 | Metadata (source/user/derived) | `MediaItem` (source size/duration, user tags/favorite, derived progress) | `desktop/engine/media` | `MediaItem` fields | — | Detail panel → Manage | — | — | IMPLEMENTED |
| 22 | Media organization | Folders + playlists + favorites + tags | `desktop/engine/media` | `MediaFolder`, `MediaPlaylist` | `MediaLibrary` CRUD | Library, Playlists, Folders | `playlist` node | — | IMPLEMENTED |
| 23 | Built-in browser | `BrowserEngine` + `LearningBrowserView` | `desktop/engine/browser`, `desktop/ui/browser_web` | — | `BrowserEngine` | Browser workspace | — | — | STUB (not media-integrated) |
| 24 | Downloads | — (system download via OS) | — | — | — | — | — | — | PLANNED |
| 25 | Online media (URLs) | `MediaItem.isRemote` + `openUrl` | `desktop/engine/media` | `isRemote=true` | `MediaLibrary.addRemote` | Open URL dialog | — | — | IMPLEMENTED |
| 26 | Manga / news / novel readers | `ReadingEngine` + `ReadingLibrary` + `EpubReader` | `desktop/engine/reading` | `ReadingDocument` | `ReadingEngine` | `ReadingView` | `document` node | — | STUB (separate workspace) |
| 27 | Unified statistics | `EventLog` + `MiningStatisticsStore` + `StatisticsRepository` | `desktop/engine/events, mining, learning` | `EventType` | `EventLog.record` | `StatsView`, `DashboardView` | — | Aggregated | PARTIAL (heatmap integration pending) |
| 28 | Study activity heatmap | `StudyCalendar` (core) + `HeatmapPanel` (desktop) | `core/.../kaiteyo/StudyCalendar.kt`, `desktop/ui/stats/HeatmapPanel.kt` | `StudyDaySummary` | `StatisticsRepository` | Stats/Dashboard calendar | — | `reviewLog` + `summaries` | PARTIAL (kanji-heatmap patterns → Kaiteyo tokens) |
| 29 | Content / mining history | `MinedRecord` + `MediaMiningEvent` | `desktop/engine/mining, media` | `MinedRecord(cardId, destination)` | `MiningEngine.recordMine` | Mining panel, Media → Recently Mined | `mined_from` | `MiningStatisticsStore` | IMPLEMENTED |
| 30 | SRS/study integration | `ReviewSession` + `LearningEngine` + `SrsScheduler` | `desktop/engine/learning, srs` | `ReviewLogEntry` | `AppState.startReview` | `ReviewView` | `card`→`review` | `EventLog: CardMined/Reviewed` | IMPLEMENTED |
| 31 | Node/knowledge connections | `MediaNodeFamily` + `KnowledgeGraph` + `MediaNodeGraph` | `desktop/engine/media, graph` | `media_source`, `subtitle_line`, `screenshot` | `KnowledgeGraph`, `MediaNodeGraph` | `GraphExplorerView` | edges `belongs_to`, `appears_in_media`, `mined_from` | — | STUB (graph materialized from mined provenance) |
| 32 | TTS / Forvo | `KanaTtsManager` (core) + Audio provider abstraction | `core/core/tts`, `desktop/engine/media` | — | `TtsService` | Player + dictionary surfaces | — | — | IMPLEMENTED (core TTS) |
| 33 | Theming | `ThemeManager` + `surfaceColors()` + `Ds*` tokens | `desktop/engine/theming`, `desktop/designsystem` | — | `ThemeManager` | All media UI via `Ds*` | — | — | IMPLEMENTED |
| 34 | Responsive | `WorkspaceShell` adaptive (dock/compact/floating) | `desktop/ui/workspace` | — | — | `MediaView` cinema/fullscreen | — | — | IMPLEMENTED |
| 35 | Error handling | `PlaybackError` + `runCatching` + throttled toasts | `desktop/engine/playback` | `PlaybackError` sealed | `MediaEngine.tickInternal` guard | Toast + activity log | — | `EventLog` | IMPLEMENTED |
| 36 | Critical E2E (27-step chain) | `MediaEngineTickSafetyTest` + `MediaPlaylistTest` | `desktopApp/src/jvmTest` | — | — | — | — | — | PARTIAL (parser/unit tests exist; full E2E not automated) |

**Legend:** IMPLEMENTED = code exists and is exercised; PARTIAL = core exists, polish/tests pending; STUB = abstraction exists, not wired to Media Center; PLANNED = documented, not yet implemented.
