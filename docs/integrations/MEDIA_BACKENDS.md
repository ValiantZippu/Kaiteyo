# Media Playback Backends

The desktop suite's Media center plays local video/audio files through pluggable backends
(`desktopApp/.../engine/playback/`). Playback is **offline** — files are local; the
backend is whatever the user has installed.

## Backends

| Backend | Implementation | Requirement | Fallback |
|---|---|---|---|
| VLC (via VLCJ) | `VlcBackend.kt` | VLC installed with `libvlc` available | Detected at startup by `BackendManager`; UI shows a hint |
| mpv (JSON-RPC) | `MpvBackend.kt` | mpv installed | Same |
| Java Sound (audio) | `AudioBackend.kt` | Built-in (JDK) | Always available for audio files |

- `BackendManager` probes for available backends and selects one; the UI communicates
  which backend is in use.
- VLCJ is GPL-compatible with Kaiteyo's GPL-3.0 (see `desktopApp/build.gradle.kts`).

## Player features

- Play/pause/stop, seek, volume, playback speed, A–B repeat, frame stepping
- Screenshot capture (`MediaCapture.kt`)
- Bookmarking and jump-to-timestamp (mined cards store timestamps)
- System media keys (Windows): global keyboard hook (`SystemMediaKeys.kt`) active only
  while media is loaded — opt-in via Settings → Media → Playback
- Tray notifications for playback transitions (opt-in)

## Subtitles

- Formats: **SRT, ASS, SSA, VTT** — parsed by `SubtitleParser.kt`, normalized by
  `SubtitleNormalizer.kt`, synchronized by `SubtitleEngine.kt`.
- Subtitle text is selectable → `DictionaryPopup` → mining ("Mine subtitle" creates a
  sentence card with screenshot + audio + timestamp).

## Text hooks & player state

- `TextHookServer.kt` — serves the current subtitle line to external tools
  (GameSentenceMiner-style workflows).
- `PlayerStateWebSocket.kt` — streams player state (position, media, playback) to
  connected clients.
- Both are surfaced in the Integrations hub (`IntegrationsView`) with test buttons and
  are auth/scope-controlled through the local API token where applicable.

## Security & failure handling

- Kaiteyo never executes media content; playback delegates to the installed backend.
- If a backend is missing, the UI degrades gracefully with actionable hints rather than
  failing.
- Media files are user-provided; metadata and bookmarks are stored as JSON under
  `~/.kaiteyo/`.

## Development notes

- Tests: `desktopApp/src/jvmTest/.../media/` — `SubtitleEngineTest`,
  `SubtitleNormalizerTest`, `SystemMediaKeysTest`, `MediaLibraryTest`,
  `MediaLibraryOrganizationTest`, `MediaSelectionTest`, `MediaShortcutsTest`,
  `MediaStatisticsStoreTest`.
