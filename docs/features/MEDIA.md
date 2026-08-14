# Media — Immersion Workspace

The Media workspace turns Kaiteyo into a full immersion environment:

```
MEDIA → SUBTITLES → TEXT → DICTIONARY → UNDERSTANDING → MINING → CARD → SRS → STATISTICS
```

Everything is real: playback goes through an actual engine (VLC, mpv, or the
built-in Java Sound audio engine), subtitles are parsed from real files, dictionary
lookups hit Kaiteyo's dictionary repository, and mined sentences become standard
study cards in the card pool — never a disconnected toy system.

## Playback backends (`desktop/engine/playback/`)

The UI only talks to the `PlaybackBackend` interface (`PlaybackModels.kt`). Each
backend exposes a capability set (`PlaybackCapability`) and the UI enables/disables
controls honestly:

- **AudioBackend** — Java Sound `Clip`. Always available; audio files play with
  seek/volume/mute. No speed control (Java Sound cannot change rate) — the chips
  correctly hide.
- **VlcBackend** — VLCJ 4.x, embedded canvas hosted in a Compose `SwingPanel`.
  Requires a VLC install with libvlc; detected by `BackendManager`.
- **MpvBackend** — launches mpv as a child process with `--input-ipc-server` and
  drives it over JSON-RPC (Unix socket / Windows named pipe). No stdout scraping.
- **BackendManager** — probes PATH + platform install locations, honours
  `KAITEYO_VLC_PATH` / `KAITEYO_MPV_PATH`, owns singletons and shutdown.

No backend installed for video → clear `PlaybackError.BackendUnavailable` message
and install guidance, never a silent dead end.

## Subtitle engine (`desktop/engine/media/SubtitleEngine.kt`)

- Parsers for SRT / ASS / SSA / VTT (robust: malformed timestamps, overlaps,
  empty cues, ASS styling/karaoke stripped for lookup).
- Multi-track with a secondary (dual-language) track.
- Binary-search active-cue lookup at ~10 Hz tick, global offset, cue navigation,
  transcript search/filters, aggregated timeline markers.
- In-memory cue text corrections (`displayTextFor`) flow through lookup, mining,
  transcript and export — the source file is never touched.

## Dictionary + segmentation

- `JapaneseSegmenter` — dictionary-backed Japanese tokenizer (longest headword
  match, deinflection fallback via `Deinflect`, kana-run splitting), cached.
- Token statuses (unknown/known/learning/mature/mined/suspended) from the live
  card pool; coverage estimates are clearly labelled as estimates.

## Subtitle text selection (#10)

The subtitle overlay supports **multi-word selection** — not just click-a-word:

- **Click a token** — selects it and opens the dictionary popup (as before).
- **Shift-click** — extends the selection from the anchor token to the clicked
  token (click another token first to re-anchor).
- **Drag across tokens** — selects the whole range live as you drag, either
  direction; release anywhere.
- **Select all** — one click selects the entire subtitle (sentence selection).
- **Click empty space** — clears the selection.

Selection is resolved by hit-testing the rendered token bounds (smallest token
wins at overlaps), so it works in every annotation mode (plain/reading/status/
frequency). The selection always preserves the **exact original text and timing**:
`selectedPhrase()` rejoins token surfaces in offset order, and multi-word
selections keep their cue's start/end for mining.

When two or more tokens are selected a small action bar appears above the
subtitle: the joined phrase plus **Lookup** (phrase search), **Mine** (creates a
phrase card), **Copy** and **Clear**. Phrase mining prefers a dictionary entry
for the whole phrase (reading + definition); when none exists it falls back to
the component tokens' readings/definitions and tags the card `phrase`. The pure
selection helpers (`tokenIndexAt`, `expandRange`, `joinTokenSurfaces`) are unit-
tested in `MediaSelectionTest`.


## Mining

- `mineCue` / `bulkMine` (multi-select transcript) create real `DesktopCard`s in
  the card pool with sentence, target, reading, definition, screenshot, audio,
  timestamp, source and tags.
- Every subtitle-mined card records a `MediaMiningEvent` so **Media → Card → Media**
  round-trips: review the card, click the source, land at the exact timestamp.
- `MiningIntegration` + `GsmTransport` forward to GameSentenceMiner when enabled —
  Kaiteyo mining never depends on it.
- Audio clips extracted with ffmpeg (or Java Sound for WAV/AIFF), screenshots via
  backend snapshot, both stored in `~/.kaiteyo/media-cache`.

## Library + history

- `MediaLibrary` persists items, watched folders, watch history, resume positions,
  favorites, tags, collections and auto-detected episode markers (`S01E05`, `EP12`,
  `第3話`…) as JSON in `~/.kaiteyo/media`.
- **Network media** (`#99–100`): the toolbar's *Open URL* plays http(s) streams
  directly (both VLC and mpv accept URLs natively). Remote items carry
  `isRemote` so thumbnails, companion-subtitle detection, relink and
  file-missing checks never touch them as local files.
- **Drag & drop** (`#264–265`): while the Media workspace is open, dropping
  video/audio opens it, a subtitle file attaches to the current media, and a
  folder is added to the library and scanned — all routed through the same
  engine calls as the file pickers.
- `MediaScanner` scans folders on a background thread with progress/cancel.
- Watch sessions separate **watch time** from **study time**; study mode records
  practice time into the standard statistics pipeline.

## UI

- `MediaView` (Player / Library / Stats / Bookmarks / Settings tabs), `MediaPlayer`
  (video surface, annotated subtitle overlay, auto-hiding controls, A–B loop,
  condensed playback, auto-pause, OCR frame, clipboard lookup, cue editor, timeline
  markers), `MediaTranscript` (search, status filters, multi-select bulk mining,
  export, right-click context menu), `MediaPanels` (library, settings, bookmarks,
  coverage stats, GSM config).
- `MediaMiniPlayer` keeps playback alive while browsing other workspaces.
- Media hotkeys are **configurable** — every player action lives in the
  `MediaHotkeys` catalog (`desktop/engine/media/MediaShortcuts.kt`), is
  rebindable from Media → Settings → Keyboard shortcuts, and persists to
  `~/.kaiteyo/media-hotkeys.json`. Defaults: `Space` play/pause, arrows seek,
  `Ctrl+←/→` ±30 s, `Alt+←/→` word cycling, `Shift+←/→` previous/next cue,
  `R` replay, `A` mine, `D` dictionary, `B` bookmark, `G` study mode,
  `S` screenshot, `C` audio clip, `E` condensed, `F` transcript, `T` subtitles,
  `L` loop, `N`/`V` next/previous item. They dispatch only while the Media
  workspace is active and stand down while typing; the command palette hints
  read the live bindings. This is the single media shortcut system — the global
  registry only keeps navigation (`Alt+V`) and app/review/browser chords.

## Local API

Media endpoints were added to the existing `LocalApiServer` for external tools
(current media, subtitle, position, player controls, mining).

## External integrations (opt-in, local-only)

- **Player WebSocket** (`PlayerStateWebSocket`, `ws://127.0.0.1:8765`, zero-dep
  RFC6455) broadcasts live player state JSON every 500 ms and accepts control
  frames (`play`, `pause`, `toggle`, `stop`, `screenshot`, `mine`, `replay`,
  `seek`, `lookup`). No filesystem access is exposed.
- **Text hook** (`TextHookServer`, TCP port 8766) receives raw Japanese text
  lines from texthookers / scripts and pushes them into the dictionary workflow;
  `CLEAR` resets the lookup.
- Both start from Settings when enabled; live status (port, client count) is
  shown in the Media settings panel.

## Playback niceties

- **Resume prompt** — reopening media with a saved position offers
  Resume / Start over (per `media.resume-prompt`).
- **Chapter markers** on the seek bar from the active backend (`chapters()`).
- **Frame stepping** buttons when the backend supports `CanFrameStep`.
- **Dual-subtitle independent timing** — the secondary track has its own
  ±0.5 s offset controls.
- **Performance profile** (`battery` / `balanced` / `quality`) maps onto mpv
  hardware-decoding / vsync settings.
- **Dictionary-form transcript search** — the "Dict form" chip matches surface
  text, token readings, and resolved dictionary headwords (searching 行く also
  finds 行かなかった).

## Per-media study data

`MediaItem` now stores a user-entered **comprehension rating** (1–5, never
computed), free-form **notes** and **tags**, alongside collections, favorites and
episode markers. The Stats panel also exposes a live **debug snapshot**
(backend, position, cue timing, token, query, mining counts, integration
status) for issue reporting.

## Settings

`SettingsEngine` gains a Media category (default speed, seek amount, auto-pause,
condensed, annotation mode, subtitle font/position/outline, dual subtitles,
mini player, mining defaults, duplicate policy, GSM host/port/mode) plus backend
probe/test actions in the workspace settings panel.

## Deep-cut immersion features

- **Video clip capture** — `MediaCapture.extractVideoClip` encodes the cue's
  range to MP4 (H.264/AAC via ffmpeg). `MiningPayload.videoPath` is written to
  the card note as `Video:`; the `media.mine-video` setting attaches clips
  automatically when mining. Only offered when ffmpeg is actually present.
- **Subtitle themes** — `SubtitleTheme` presets (Classic / Minimal / Cinema /
  High contrast / Custom) control backdrop opacity, text color, outline and
  weight; Custom reads `media.subtitle-opacity` + `media.subtitle-weight`.
  Applied live by `SubtitleOverlay`.
- **Contextual dictionary** — the docked panel now shows the *current sentence*
  it came from, with previous/next sentence navigation, replay, and a
  `辞書形` badge when the selected token is an inflected form
  (食べました → 食べる).
- **Word cycling** — Alt+←/Alt+→ cycle the selected word within the current
  subtitle (`cycleToken`), wrapping through Japanese tokens only.
- **Chapter navigation** — ◀/▶ chapter chips appear when the backend reports
  chapters; the current chapter title is shown beside them.
- **Cinema mode** — one click (or the toolbar toggle) removes the toolbar/tabs
  so the player owns the full workspace; the player's own "Cinema" chip flips
  it back.
- **In-player library search** — the toolbar search box shares one query state
  (`librarySearchQuery`) with the library panel, so searching from the player
  jumps straight to filtered results.
- **Replay count** — `media.replay-count` (default 1) controls how many times R
  replays the current subtitle; `media.mini-player` is now a real persisted
  setting (it was referenced but previously undefined).

## Media analytics (`MediaStatisticsStore`)

Watch time, study time, dictionary lookups, mined sentences and watch sessions
are recorded incrementally into a persistent per-local-day store
(`~/.kaiteyo/media/stats.json`) — no history scans, append-only aggregates.

- `recordWatch(ms, study)` — called on every session flush; study-mode watch
  time is tracked separately from leisure time.
- `recordLookup()` — every token selection / manual query.
- `recordMined()` — every card that lands in the pool from media.
- `recordSession()` — every media open.

Queries (`watchMsBetween`, `studyMsBetween`, `lookupsBetween`, `minedBetween`,
`activeDays`, `summary()`) answer "how much media have I consumed / studied" for
any local date range and feed the `/api/media/stats` endpoint. Days are capped
at 366 buckets. Time is recorded against the user's local day
(`Clock.System.todayIn(systemDefault)`), never UTC.

## Queue + end-of-episode flow

- The **play queue** (`playQueue` / `queueIndex`) is now persisted to
  `~/.kaiteyo/media/queue.json` (ids + cursor) so a binge session survives a
  restart; `N` / `V` jump next / previous.
- `resolveNextUp()` offers the next queued item, or the next episode of the same
  series when the queue is exhausted.
- On playback completion the **end-of-episode overlay** offers Next / Replay /
  Library; `media.auto-advance` skips the overlay and plays the next item
  automatically.

## Condensed fast-forward

With condensed playback on, `media.condensed-fast-forward` advances through
unsubtitled gaps in ~1 s chunks instead of jumping straight to the next
subtitle — a genuine fast-forward through dead air rather than an instant cut.
`toggleCondensed()` reads the setting when condensed turns on, and
`toggleCondensedFastForward()` flips + persists it live.

## Subtitle normalizer (`SubtitleNormalizer`)

A pure, test-covered text pipeline used before dictionary lookup / mining /
search when text arrives raw (hooks, OCR, clipboard, edited cues):

- Strips ASS/SSA override blocks + escape sequences, HTML tags and `<ruby>`
  annotations; decodes numeric and named HTML entities; collapses kanji+furigana
  brackets (`漢字[かんじ]` → `漢字`); removes leading speaker labels;
  `isMostlyJapanese()` gates popups for romaji/lyric lines.

The subtitle parsers already strip styling at parse time; this normalizer is the
on-demand pass for every other text source — it is wired into the text-hook and
clipboard lookup paths so external lines arrive at the dictionary already clean.
`isMostlyJapanese()` also gates popups so romaji/lyric lines don't trigger
pointless dictionary opens.

## Local API additions

`LocalApiServer` gained read-only media endpoints (all local, no filesystem
access):

- `GET /api/player/state` — full live player snapshot (position, duration,
  playing, buffering, speed, volume, backend, active subtitle, selected token,
  mined count).
- `GET /api/media/library?query=` — searchable library listing.
- `GET /api/media/history` — watch-history entries.
- `GET /api/media/queue` — queue size, cursor and next item.
- `GET /api/media/stats` — the analytics overview from `MediaStatisticsStore`.
- `GET /api/mining/history` — recent mined records.

## Play queue, auto-advance and end-of-episode

- **Play queue** (spec #203): add any library item to the queue (row button), clear it, or jump to it; the queue is persisted as ids + cursor in `~/.kaiteyo/media/queue.json` so a binge session survives a restart.
- **Queue-aware next/previous**: `playNext()`/`playPrevious()` walk the queue first, then fall back to the next episode of the same series (folder + base name + episode number, `S01E05` / `EP12` / `第5話` styles).
- **End-of-episode overlay** (#204–205): when playback completes, Kaiteyo offers **Play next** (suggested next item), **Replay** and **Back to library**. With `media.auto-advance` on, the next item plays automatically.
- **Next-episode resolution**: `MediaLibrary.nextEpisode()` groups items by series key and orders by episode number — no metadata required, manual collection override always wins.

## Thumbnails, folder watcher and relink

- **Poster thumbnails** (#200): ffmpeg extracts one frame ~1/3 into each video on a background thread, cached in `~/.kaiteyo/media-cache/thumbnails/`. Rows show the poster instead of the icon; no ffmpeg means the plain icon (never a dead image).
- **Background folder watcher** (#197): with `media.watch-folders` on, configured library folders are swept every ~45 s and new media is auto-added. Watcher status is visible in the library panel.
- **Relink moved files** (#192–194): a missing file shows a *Relink* action; the item keeps its identity, history, bookmarks and mined-card links — only the path-dependent fields (name, size, episode, companion subtitle) are refreshed.

## System tray media controls (#128, #262–263)

A java.awt `SystemTray` icon (書 mark) appears while media is loaded: current item tooltip, Play/Pause, Previous, Next, Stop, Open Kaiteyo media, and Quit media. Pure AWT — no new dependencies; unsupported/headless desktops simply never show it.

## System media keys & notifications (#128, #262)

Dedicated keyboard media buttons (Play/Pause, Next, Previous, Stop) control the player globally, even while Kaiteyo is in the background:

- **Windows** — a global low-level keyboard hook (`WH_KEYBOARD_LL`, via the JNA already used for native window dragging — no new dependency) running on a daemon thread with its own `GetMessage` pump. `VK_MEDIA_*` presses map to Play/Pause, Next, Previous and Stop; the hook is removed and the pump woken cleanly on stop.
- **macOS / Linux** — no global hook is installed; the tray menu and in-app media hotkeys cover background control (documented limitation).

Implementation: `engine/media/SystemMediaKeys.kt`. The `systemMediaKeyActionForVk` mapping is a pure function covered by unit tests. Managed from **Media → Settings → System media keys & notifications**, with a live Listening / Registration failed / Unsupported badge; `media.system-media-keys` (default on).

Playback notifications are opt-in tray balloons (`media.notifications`, default on): **Playing** on start, **Paused** on pause, **Finished** on completion (plus the auto-advance notice). The tray shows at most one balloon per ~2.5 s so pause/resume chatter stays quiet. Both are persisted in the normal settings store and stop cleanly on shutdown.

## Video quality (#105–106)

`media.mpv-shader` lets advanced users pass an optional GLSL shader file (e.g. an Anime4K pipeline) to mpv as `--glsl-shaders` at open time. The performance profile (battery/balanced/quality) already maps onto mpv hwdec/video-sync.
