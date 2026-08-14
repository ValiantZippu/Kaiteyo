# Integrations

This section documents how Kaiteyo interoperates with external systems: Anki, Yomitan
dictionaries, media playback backends, its own local HTTP API, and the (planned) plugin
architecture.

| Document | Purpose |
|----------|---------|
| [`ANKI.md`](ANKI.md) | Anki `.apkg` import/export (all platforms) + AnkiConnect integration (desktop) |
| [`YOMITAN_DICTIONARIES.md`](YOMITAN_DICTIONARIES.md) | Yomitan-compatible dictionary import and the popup lookup workflow |
| [`MEDIA_BACKENDS.md`](MEDIA_BACKENDS.md) | VLC / mpv / Java Sound playback backends, subtitles, text hooks |
| [`LOCAL_API.md`](LOCAL_API.md) | The localhost HTTP API (bearer-token protected) |
| [`PLUGINS.md`](PLUGINS.md) | Plugin registry & marketplace (scaffold — no runtime loading yet) |

## Status summary

| Integration | Status | Where |
|---|---|---|
| Anki `.apkg` export | ✅ Implemented | core `AnkiPackage` + platform actuals (desktop/Android/iOS) |
| Anki `.apkg` import | ✅ Implemented | same pipeline |
| AnkiConnect (push mined cards / import decks) | ✅ Implemented | desktop `AnkiConnectTransport`, `AnkiImporter` |
| Yomitan dictionary import | ✅ Implemented | desktop `DictionaryImporter` (ZIP/JSON/JMdict) |
| Dictionary popup lookup | ✅ Implemented | desktop `DictionaryPopup` |
| VLC playback (VLCJ) | ✅ Implemented (needs VLC installed) | desktop `VlcBackend` |
| mpv playback (JSON-RPC) | ✅ Implemented (needs mpv installed) | desktop `MpvBackend` |
| Java Sound audio | ✅ Implemented | desktop `AudioBackend` |
| Subtitle formats (SRT/ASS/SSA/VTT) | ✅ Implemented | desktop `SubtitleEngine` |
| Local HTTP API | ✅ Implemented | desktop `LocalApiServer` (Ktor server) |
| Text hook server / player WebSocket | ✅ Implemented | desktop `TextHookServer`, `PlayerStateWebSocket` |
| Plugin runtime loading | 📋 Planned (registry + marketplace scaffold only) | desktop `engine/plugin/` |

## General principles

1. **Local-first.** Every integration either runs entirely on the device (VLC, mpv,
   AnkiConnect, local API) or is user-initiated (OAuth sync, dictionary import).
2. **Graceful degradation.** Missing optional runtimes (VLC, mpv, Tesseract) are detected
   and the UI falls back — the app never hard-fails on an optional dependency.
3. **Auth where it matters.** The local API requires a bearer token; AnkiConnect talks to
   the user's local Anki instance; sync uses GitHub OAuth device flow.
4. **Import content is sanitized.** HTML from imported decks is sanitized before rendering.
