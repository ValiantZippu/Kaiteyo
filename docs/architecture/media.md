# Kaiteyo Architecture — Media, Playback & Subtitle Engine

**Status**: Implemented (suite engine, integrated into the shipped app as the Media Centre)
**Owner**: suite `ua.syt0r.kanji.desktop.engine.media` + `engine/playback`
**Related**: `docs/features/MEDIA.md` · `docs/integrations/MEDIA_BACKENDS.md` ·
`docs/user-guide/DESKTOP_SUITE.md` · `docs/architecture/mining.md`

## 1. Purpose

Media playback with a **player abstraction** (§194): the UI never talks to a concrete
player engine. Kaiteyo's value is the media-learning integration — subtitle lookup, mining,
bookmarks, coverage analytics — not a reimplementation of VLC/FFmpeg (§193, §363).
Media files are untrusted input (§357): malformed containers, malicious subtitles and
corrupt files must degrade to user-facing errors, never crashes.

## 2. Subsystem map

```
MediaView (workspace UI)
   │  MediaCentre host (DesktopMediaCentreContent in the shipped app)
   ▼
MediaEngine (2426-line controller, owned by AppState)
   ├── backends: BackendManager → AudioBackend | VlcBackend | MpvBackend   (PlaybackBackend)
   ├── subtitles: SubtitleEngine + SubtitleParser + SubtitleNormalizer
   ├── library: MediaLibrary (+ MediaScanner + MediaKind)
   ├── capture: MediaCapture (screenshots + audio clips, ffmpeg/Java Sound)
   ├── statistics: MediaStatisticsStore (coverage, immersion analytics)
   ├── hotkeys: MediaHotkeys (+ SystemMediaKeys for OS keys)
   ├── tray: MediaTray
   └── API surfaces: TextHookServer, PlayerStateWebSocket (external tooling)
```

## 3. Playback abstraction (`PlaybackModels.kt`)

### `PlaybackBackend` interface
Contract every player engine implements. State is pulled via accessors (the engine polls
at ~10 Hz); events are pushed to `listener`.

- Transport: `open(source): Result<Unit>`, `play`, `pause`, `stop`, `seekTo(ms)`,
  `setSpeed(rate)`, `setVolume(percent)`, `setMuted`, `setLoop`.
- Tracks: `availableTracks(): List<MediaTrackInfo>` (Video/Audio/Subtitle with id, title,
  language), `selectTrack(trackId?)`, `setSubtitleDelay(delayMs)`.
- Advanced: `frameStepForward/Backward(): Boolean`, `snapshot(target: File): Result<String>`,
  `chapters(): List<PlaybackChapter>`.
- Live state: `currentPositionMs()`, `durationMs()`, `isPlaying`, `isBuffering`,
  `bufferedPositionMs()` (defaults to position — timeline's buffered region).
- Video rendering: `setDisplayMode`, `setAspectRatio`, `setVideoAdjustments`,
  `setDeinterlace`.
- Audio extras: `setAudioDelay`, `setAudioChannel`, `setAudioOutput(deviceId?)`,
  `setEqualizer(EqualizerSettings?)`.
- `setPerformanceProfile(profile)` (battery/balanced/quality hint) and `close()`.
- **All advanced methods have no-op defaults** so the built-in audio player compiles
  unchanged; every control is gated on `PlaybackCapability`.

### Backends
| Backend | Kind | Implementation | Notes |
|---|---|---|---|
| `AudioBackend` | Audio | Java Sound `Clip` | always available; WAV/AIFF-native |
| `VlcBackend` | Vlc | VLCJ (libVLC) | GPL-3 compatible (see desktopApp build); full video |
| `MpvBackend` | Mpv | mpv over IPC | when mpv is installed |

`BackendManager` routes by `MediaKind` and probes availability. `BackendProbe(kind,
available, version, path, message)` is surfaced in Settings → Media and when the preferred
backend is missing — the app degrades gracefully (§201) instead of failing.

### Capabilities & honesty
`PlaybackCapability` — CanSeek, CanChangeSpeed, CanSelectSubtitle, CanSelectAudio,
CanFrameStep, CanScreenshot, CanCaptureAudio, CanExternalSubtitles, CanHwAcceleration,
CanChapters, CanFrameAccurateSeek, CanVolume, CanLoop, CanMute, CanAspectRatio,
CanDisplayMode, CanVideoAdjustments, CanDeinterlace, CanAudioDelay, CanAudioChannel,
CanAudioOutput, CanEqualizer.

**Rule**: the UI gates every control on the corresponding capability and never pretends an
unsupported feature works (§325).

### Events & errors
- `PlaybackEventType` — MediaLoaded/Unloaded, Started/Paused/Stopped/Completed,
  PositionChanged/Seeked, Buffering(Ended), Subtitle/Audio/Video track changed,
  Speed/Volume/Muted/Chapter changed, Error. `PlaybackEvent(type, positionMs, message)`.
- `PlaybackError` (sealed, all carry `userMessage`): FileMissing, UnsupportedCodec,
  BackendUnavailable, SubtitleInvalid, AudioUnavailable, PermissionDenied, NetworkError,
  Other. Never raw backend exceptions on screen (§296).
- `SurfaceProvider.surfaceMode` — Embedded/ExternalWindow/None so the UI knows whether to
  mount an embedded canvas or show a hint.

### Display modes & tuning models
- `VideoDisplayMode`: Fit / Fill / Crop / Original / Stretch.
- `AspectRatioPreset`: Auto + 4:3, 16:9, 16:10, 21:9, 3:2, 1:1, 5:4, Square.
- `VideoAdjustments`: brightness/contrast/saturation/gamma 0..200 (100 neutral), hue
  −180..180, deinterlace; `neutral()`, range-clamped `with*` copies.
- `AudioChannelPreset`: Stereo / Reverse / Left / Right / Mono / Headphones (VLC values).
- `EqualizerSettings`: preamp + 10 ISO bands (60 Hz–16 kHz, `EQUALIZER_BAND_FREQUENCIES_HZ`)
  with the 18-preset VLC family (Flat…Techno) as real dB gains; `withPreset/withPreamp/
  withBand` clamped to ±20 dB; `normalizedBands()` pads to 10.

## 4. MediaEngine — state & orchestration

All playback/UI state is Compose-reactive (`mutableStateOf`/`mutableStateListOf`).

### Sub-engines
`subtitles = SubtitleEngine()` · `library = MediaLibrary()` · `scanner = MediaScanner(library)`
· `backends = BackendManager()` · `statistics = MediaStatisticsStore()` · `hotkeys`.

### Playback state
`activeBackend`, `backendKind`, `currentItem`, `isPlaying`, `positionMs`, `durationMs`,
`speed`, `volume`, `muted`, `buffering`, `playbackError`, `bufferedPositionMs`; video
rendering (`displayMode`, `aspectRatio`, `videoAdjustments`); audio extras
(`subtitleDelayMs`, `audioDelayMs`, `audioChannel`, `audioOutputId`, `equalizer`,
`lastScreenshotPath`, `lastAudioClipPath`, `lastVideoClipPath`, `lastMinedPayload`);
`currentDocument: MediaDocument?`.

### Subtitle UI state
`subtitleVisible`, `annotationMode` (Status/…) with `activeCue`, `activeCueIndex`,
`secondaryCue`; lookup/selection (`selectedTokens`, anchor-based selection extension,
`tokenCycleIndex`, `lookupQuery`, `lookupPosition`).

### Workspace state
`transcriptOpen`, `dictionaryOpen`, `libraryOpen`, `settingsOpen`, `controlsVisible`,
`fullscreenActive`, `cinemaMode` (closes side panels), `librarySearchQuery`,
`textInputFocused` (immersion hotkeys stand down while typing).

### Playback modes
`condensedPlayback` + `condensedFastForward` (fast-forward through unsubtitled gaps),
`autoPauseMode`, `loopMode` + `loopStartMs/EndMs`, `replayRemaining`, `studyMode`,
`seekAmountMs`. **Mini player**: playback continues outside the Media workspace.
**Resume prompt**: `resumePromptEnabled/Pending`, `pendingResumeMs`.

### Play queue
`playQueue` + `queueIndex`, persisted to `~/.kaiteyo/media/queue.json` (ids + cursor,
resolved against the library on load). API: `addToQueue`, `playPlaylist` (replace +
start), `playShuffled`, `queuePlaylist` (append), `removeFromQueue`, `clearQueue`,
`resolveNextUp()` (next queued item, else `library.nextEpisode` for same-series
auto-continue), `playNext`. Every action surfaces a toast (empty playlist, queued,
cleared, playing).

### Safety (crash prevention)
The 10 Hz `tick()` reconciliation loop is a fail-safe wrapper around `tickInternal()`:
failures are swallowed, surfaced as a throttled toast + activity-log entry, and a backend
that both throws and reports unavailable is **closed and dropped** so it can't poison
subsequent ticks. Composition-read helpers (`tokensFor`, `coverageFor`, `currentCoverage`,
`mediaStatsFor`) degrade to empty/zero through `runCatching`. `MediaView` init is guarded
(settings restore, AWT host-window lookup, optional `DropTarget`). **Regression test**:
`MediaEngineTickSafetyTest` (50 ticks vs an exploding backend; unavailable+throwing
cleanup; healthy advance). Opening Media can never close the app (§219).

### Persistence
`MediaStateDto` (bookmarks, clips, recent files, mining events) + `PlaybackQueueDto` +
library JSON; all reads/writes `runCatching`.

## 5. Media library (`MediaLibrary.kt`)

Storage: `~/.kaiteyo/media/library.json` (`LibraryDto`: items, folders, playlists,
playlistFolders) + `history.json` (`WatchHistoryEntry`).

- `MediaItem` — id, path, name, kind, sizeBytes, `isRemote`, durationMs, addedAt,
  lastPositionMs, lastWatchedAt, watchCount, `completed`, `favorite`, tags, collection,
  subtitlePath, `episode`, `comprehension` (0–100), note; `progressFraction` derived.
- `MediaFolder` — path + includeSubdirs. `MediaPlaylist` — id, name, itemIds,
  createdAt, folderId, favorite; functional helpers (`renamed/withItem/withoutItem/
  withOrder/inFolder/withFavorite`). `PlaylistFolder` — nested folder tree.
- Default collections: Anime, Movies, TV, YouTube, Music, Audiobooks, Other.
- Key ops: `addFile` (dedupe by absolute path; associates companion subtitles),
  `upsert`, `itemByPath`, `fileExists` (remotes always "exist"), `addRemote(url)`,
  `relink(id, newFile)` (moved-file repair), `seriesKey`/`episodeNumber`/`nextEpisode`
  (same-folder + base-name series grouping), `removeItem(forgetHistory)`,
  `findCompanionSubtitle` (srt/ass/ssa/vtt, direct + `<name>.*` prefixed), playlist
  CRUD/ordering/folders.

### MediaScanner
- `scan(folder, recursive)` on a daemon thread; reactive `scanning/progress/
  scannedFiles/lastError/scanMessage`; cancellation via `AtomicBoolean`; incremental
  progress on large folders; collects media + subtitle files.
- **Folder watcher**: `startWatching(45s)` daemon thread polls configured folders and
  auto-registers new files (`lastWatchFound` + message) — dependency-free, never blocks UI.
- **Thumbnails**: `requestThumbnail(item)` — poster frame via ffmpeg
  (`-ss duration/3 or 3s`, `-frames:v 1`, `scale=320:-2`, `-q:v 5`, 20 s timeout),
  cached under `~/.kaiteyo/media-cache/thumbnails/<id>.jpg`; missing ffmpeg/failure →
  plain icon, never an error.

### MediaKind
Video/audio extension classification (`of(file)`, `fromUrl(url)`); drives backend routing,
icons and grouping.

## 6. Subtitle engine

### Model (`SubtitleParser.kt`)
- `SubtitleCue(id, startMs, endMs, text, style, speaker)`; `durationMs`;
  `tokens()` (segmenter tokens for lookup/coverage).
- `SubtitleTrack(name, cues, format, language)`; `cueAt(ms)`.
- `SubtitleFormat`: Srt / Ass / Ssa / Vtt; `detectFormat` by extension.

### Parsers
- **SRT** — BOM + CRLF normalization, timestamp regex (`,`/`.`/`:` millis), speaker
  detected via `^([A-Za-z0-9_\-一-龯]+)[:：]\s*`.
- **VTT** — header stripping, same cue engine.
- **ASS/SSA** — parses `[Script Info]/[V4+ Styles]/[Events]`, `Format:` field order,
  `Dialogue:` rows (Start/End/Text/Name/Style columns), ASS tag stripping for display
  text, `assTimeToMs`.
- Parsing is backend-independent (§195) and strictly bounded — malformed/corrupt files
  surface `SubtitleInvalid`, never a crash (§357).

### SubtitleEngine
Track management, active-cue resolution for the transcript overlay, delay offset
(`subtitleDelayMs`) independent of the player, synchronization with the 10 Hz tick, and
secondary-cue support for two-line display.

## 7. Capture (`MediaCapture.kt`)

- Cache: `~/.kaiteyo/media-cache` (kept separate from the library).
- `findFfmpeg()` — `KAITEYO_FFMPEG_PATH` env override, then PATH scan (`ffmpeg`,
  `ffmpeg.exe`); `ffmpegAvailable`.
- Screenshots: canonical `Kaiteyo_<media>_<HH-MM-SS>.<ext>` naming (pure, tested);
  configurable format/folder.
- Audio clips: `extractAudioClip(source, startMs, endMs, label)` → WAV. ffmpeg preferred
  (video + compressed audio); Java Sound range export fallback for WAV/AIFF. Duration
  coerced to ≥ 50 ms. Exported clips are attached to mined cards.

## 8. Immersion analytics

`MediaStatisticsStore` + `MediaCoverageStats(totalTokens, known, learning, unknown,
mined, suspended)` → `coverage` — per-media token coverage vs the user's card pool
(segmentation via the shared segmenter; known/learning/unknown derived from real deck
state). `MediaMiningEvent(cardId, mediaPath, mediaName, timestampMs, cueText)` records
every mine for "Recently Mined" and the mined-from-this-media list. `joinTokenSurfaces`
rebuilds display text from segment tokens.

## 9. External surfaces

- **`TextHookServer`** — local text hook for browser extensions (mining from the web).
- **`PlayerStateWebSocket`** — real-time player state (`PlayerStateSnapshot`) for
  companion tooling.
- **`SystemMediaKeys`** — Windows `WH_KEYBOARD_LL` hook for OS media keys + media
  notifications (runtime verification pending).
- **`MediaTray`** — tray integration.
- All authenticated/localhost; see `docs/architecture/integrations.md`.

## 10. Media Centre (shipped app)

`MainDestination.Media` (core) → `MediaCentreContent`; `desktopApp/Main.kt` overrides with
`DesktopMediaCentreContent`, mounting the suite's full `MediaView` (backends, subtitles,
dictionary popup, mining) with its own `AppState` inside the shipped app. The core default
is an honest desktop-only screen (EN/JP strings) so the entry is never a dead link on
mobile. Registered in `defaultMainDestinations`, primary nav, command palette ("Media
Centre"). Media home (Continue Watching, Pinned, Watch Later, Collections, Recently
Added, Playlists, Recently Mined, Watched), browse/explorer (search/sort/filters/grid),
detail page (progress, metadata, subtitles, playlists, watch history, mined-from list,
Manage), playlists + folders.

## 11. Tests

- `MediaEngineTickSafetyTest` — exploding backend × 50 ticks, unavailable+throwing
  cleanup (`activeBackend → null`), no-backend no-op, healthy advance, segmentation
  helpers never throw.
- `MediaPlaylistTest` — CRUD/reorder/persist/missing items.
- `MediaTuningModelsTest` — EQ preset integrity, adjustment clamping, display modes,
  screenshot naming.
- `MediaShortcutsTest` — hotkey chords.
- Gaps: real-file playback tests, subtitle parser fuzz (§280), corrupt-container tests
  (§357), large-library performance (§369).

## 12. Performance

- 10 Hz tick with fail-safe wrapper; state pulled via accessors — no per-frame allocation
  in hot paths; buffered-region from `bufferedPositionMs`.
- Thumbnails streamed on background threads with a 20 s cap; cache keyed by item id.
- Folder scanning/watching never touches the UI thread; incremental progress.
- Hardware acceleration where the backend supports it (capability-gated).

## 13. Open items

- Bundled backend packaging (VLC/mpv availability is external; graceful degradation is
  tested, bundling is future work).
- Anime/media metadata enrichment (adapter-based, §292) — RESEARCH.
- Browser workspace (watch in-browser) is planned, not core.
- iOS/Windows runtime verification of media paths (BLOCKED list).

## 14. Node-layer integration (TARGET — ADR-0013, NODE §130, §83)

The MEDIA family (`docs/architecture/nodes/NODE_TYPE_REGISTRY.md` §3) turns the media
engine's objects into first-class graph citizens.

### 14.1 Mapping

| Media engine concept | Node type(s) | Edges |
|---|---|---|
| library item | `media_source` → `series` / `anime` / `movie` / `episode` / `video` / `audio` | `belongs_to`, `contains` |
| subtitle track + lines | `subtitle_track` → `subtitle_line` | `contains`; word→line `appears_in_media` |
| scenes | `scene` | `belongs_to` → episode; `appears_in_scene` from words |
| screenshots / clips | `screenshot`, `clip` | `belongs_to`, `depicts`, `mined_from` |
| playlists | `playlist` (collection node) | `belongs_to` → members |

### 14.2 Subtitle indexing → the graph (§83 engine)

`SubtitleService.indexLines` materializes `appears_in_media` edges (word → subtitle line)
against the language graph. This single index powers:

- "Where have I seen this?" (§83) — media exposure with episode + timestamp jump.
- Coverage analytics (existing `MediaStatisticsStore` gains per-node depth).
- Mining provenance (card ← line, `mined_from`).

Indexing is idempotent (rebuild-safe) and versioned per media item; partial indexes
(re-index after subtitle edits) must be supported.

### 14.3 Continue watching & library views

- `Continue Watching` = `media_position_updated` events, not a bespoke store.
- Library sections (Anime/Movies/Videos/Audio/Mining/History) = node queries with
  filters, not hardcoded lists (§130 acceptance criteria).

### 14.4 Playback as evidence

- `media_started` / `media_position_updated` / `media_ended` feed stats + resume.
- `subtitle_selected` feeds `ENCOUNTERED`/`EXPOSED` knowledge transitions (passive
  exposure, §112) — never interrupts playback (§112 rule).

### 14.5 Acceptance criteria

- Subtitle → popup → mine → card is ≤4 actions with zero app switching (§130).
- `appears_in_media` queries hit §TEST_PLAN budgets at 10k+ lines.
- Re-indexing after subtitle edits is safe and idempotent.
