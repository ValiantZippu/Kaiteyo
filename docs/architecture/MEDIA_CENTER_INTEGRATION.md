# Media Center — Integration

> Status: IMPLEMENTED  
> Covers: browser, downloads, external tools, TTS/audio, library/home, theming

## Browser

| Concern | Implementation | File |
|---|---|---|
| Engine | `BrowserEngine` (tabs, address bar, back/forward, bookmarks, history, reader mode, JavaFX WebView fallback) | `desktop/engine/browser/BrowserEngine.kt` |
| UI | `LearningBrowserView` (workspace) + `BrowserView` legacy | `desktop/ui/browser_web/*`, `desktop/ui/browser/*` |
| Fetch | `java.net.http` with 12–20s timeout, `User-Agent: Kaiteyo-LearningBrowser/1.0`, `ReaderMode.stripTags/extractReadable` | same |
| Media hook | `BrowserSelection → MiningPayload(source=browser)` via `MiningEngine` (shared pipeline) | `MiningEngine` |

**Architecture:** `BrowserService` is `BrowserEngine` itself; `MediaSourceResolver` is future extension (hard-coded site lists rejected). Downloads use `MediaDownloadService` (§20), not browser-embedded fetching.

## Download

| State | Handling |
|---|---|
| Queued → Downloading → Paused → Completed / Failed / Cancelled | `MediaDownloadService` state machine, daemon thread, 64KB streaming, progress coalesced 512KB, cooperative `cancelled` set, `runCatching` + `DownloadState.Failed` with `error` |
| Constraints | `http(s)` only, filename sanitized, no DRM bypass, target dir `~/.kaiteyo/downloads`, `User-Agent: Kaiteyo-MediaDownload/1.0` |
| UI | `MediaDownloadService.jobs` (`mutableStateListOf`) — shows progress, filename, source, size, status, retry/cancel (no background blocking) |

## External Tools

| Tool | Boundary | File |
|---|---|---|
| Text hook | `TextHookServer(port 8766)` TCP, `normalizeForLookup()` | `desktop/engine/media/TextHookServer.kt` |
| Player WebSocket | `PlayerStateWebSocket(port 8765)` `PlayerStateSnapshot` + `onSocketCommand` (play/pause/seek/lookup/mine) | `PlayerStateWebSocket.kt` |
| System media keys | `SystemMediaKeys` (Windows `WH_KEYBOARD_LL` hook) + `MediaTray` | `SystemMediaKeys.kt`, `MediaTray.kt` |
| ASBPlayer-style | `TextHookServer` + `PlayerStateWebSocket` as `ExternalMediaIntegration → KaiteyoMiningEndpoint → MiningService → DestinationResolver` | same |

All are localhost, `runCatching`, never crash. DRM/auth/copyright are respected — no unrestricted downloader.

## TTS / Audio

- **Core:** `KanaTtsManager` (core `core/tts`) is the shared `TtsService` — media never creates a second engine. Used for dictionary entries, subtitle text, sentences, cards, reading, mining.
- **Forvo/providers:** `AudioDictionaryService` → `Provider` abstraction (Forvo + TTS + local) — media consumes service, not direct Forvo calls.

## Library / Home / Statistics

- **Library:** `MediaLibrary` is the single source of truth; `LibraryView` is presentation/discovery, `Media` is media-specific ops — no duplicated DB.
- **Home:** `Continue Watching` = `library.updateProgress` + `PlaybackQueueDto`; `DashboardView` reads `MediaStatisticsStore` + `EventLog`, never queries DB directly.
- **Statistics:** `EventLog` (append-only, `event_log.json`) is source — `MediaStarted/MediaEnded/SubtitleSelected/BookmarkAdded/CardMined` → `MediaStatisticsStore` + `MiningStatisticsStore` → `HeatmapPanel`/`MediaImmersionHeatmap`.

## Theming / Responsive / Desktop

- Theming via `surfaceColors()` + `Ds*` tokens (`DsTokens.kt`, `themeManager`); no hard-coded colors; light/dark/sepia all affect media.
- Responsive: `WorkspaceShell` adaptive (dock/compact/floating), cinema/fullscreen, same domain powers desktop/phone (phone uses bottom sheets, compact controls — not scaled desktop).
- Desktop window: `WorkspaceShell` + `KaiteyoWindow` own window/nav/sidebar/floating; media respects `WindowPlacement`, no second window architecture.
