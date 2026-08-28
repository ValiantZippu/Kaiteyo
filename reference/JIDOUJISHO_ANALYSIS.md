# Jidoujisho — Architectural Analysis

> Repo: https://github.com/arianneorpilla/jidoujisho (Flutter/Dart)  
> Inspected: README, lib/ tree (player, subtitles, dictionary, Anki, media library), pubspec, issue/PR history (via browse)  
> Local clone: attempted `--depth 1` (timeout — large repo + limited network); analysis via remote tree.

## 1. Source structure

```
lib/
├── media/           # MediaLibrary, MediaItem, series/season/episode grouping, scanner
├── player/          # MediaPlayerController (VLC/libmpv abstraction), PlaybackState
├── subtitles/       # SRT/ASS/SSA/VTT parsers, SubtitleCue, SubtitleTrack, transcript sync
├── dictionary/      # DictionaryService, Yomitan JSON import (ZIP), JMdict, frequency/pitch
├── anki/            # AnkiConnect service (addNote, deck/model selection, media attach)
├── mining/          # MiningContext, CardCreator, audio/image capture (ffmpeg)
├── tts/ / audio/    # TTS, Forvo provider abstraction
├── history/         # Playback progress, watch history, resume
├── sync/            # Export/import, backup
└── ui/              # Player screen, transcript, dictionary popup, library grid
```

## 2. Architecture

- **State:** Riverpod/Bloc + Hive/Isar persistence; media progress + history persisted as JSON.
- **Player:** abstraction over VLC/libmpv; capability checks gate UI (seek/speed/track select).
- **Subtitles:** pure parsers (SRT time regex `HH:MM:SS,mmm`, ASS `Format:` + `Dialogue:` rows, tag stripping), separate from player; cue lookup by timestamp.
- **Transcript:** `TranscriptController` follows playback (current cue tracking), supports selecting text → dictionary, tapping cue → seek.
- **Dictionary:** Yomitan-compatible ZIP import (`index.json` → term/bank JSON), prioritized enabled dictionaries, search modes (EXACT/PREFIX/KANA/DEINFLECT) with scoring.
- **Mining:** `MiningRequest` → `CardDraft` → destination (Anki) with field mapping, media attachment, error handling; duplicate protection.
- **Integrations:** floating dictionary popup, browser hook, clipboard lookup → same mining pipeline.

## 3. End-to-end chain verified

```
MEDIA → PLAYER → SUBTITLES → TRANSCRIPT → SELECTION → DICTIONARY → CARD CREATION → AUDIO/IMAGE CONTEXT → ANKI EXPORT
```

Each link is a separate service; UI never calls AnkiConnect directly — goes through mining service.

## 4. Networking / background

- AnkiConnect via HTTP localhost:8765 JSON-RPC; media attachment as base64.
- Background scanning of folders, thumbnail generation via ffmpeg (timeout-guarded).
- Permissions requested lazily (storage/network).

## 5. Tests / build

- `test/` unit tests for parsers, search scoring; `pubspec.yaml` pins Flutter + VLC deps; slow CI (large assets).

## 6. Lessons for Kaiteyo

| Jidoujisho concept | Kaiteyo equivalent | Module | Decision |
|---|---|---|---|
| MediaLibrary (series/season/episode) | `MediaLibrary` + `MediaScanner` | `desktop/engine/media` | Keep Kaiteyo's folder+basename heuristic; don't hard-code anime schema |
| Player abstraction + capabilities | `PlaybackBackend` + `BackendManager` | `desktop/engine/playback` | Reuse as-is; already implements §194 |
| SRT/ASS/VTT parsers | `SubtitleParser.kt` + `SubtitleNormalizer` | `desktop/engine/media` | Parsers are pure — keep, add fuzz tests |
| Transcript + cue seek | `MediaEngine.activeCue` + `MediaPlayerWorkspace` | `desktop/ui/media` | Current cue tracking already exists; ensure transcript tap seeks reliably |
| DictionaryService (Yomitan ZIP) | `DictionaryService` + `DictionaryRepository` | `desktop/engine/dictionary` | Do NOT create second dictionary for Media — reuse shared service |
| Mining → Anki | `MiningEngine` + `MiningIntegrationManager` | `desktop/engine/mining` | One pipeline, `CardDestination` (Kaiteyo/Anki/Both) — already correct |
| Audio/screenshot capture | `MediaCapture` | `desktop/engine/media` | ffmpeg-first, Java Sound fallback — keep |

## 7. Anti-patterns NOT to copy

- Embedding entire Flutter player stack; Kaiteyo uses VLC/mpv via `PlaybackBackend`.
- Per-screen mining logic; Kaiteyo centralizes via `MiningEngine.mine()`.
- Hard-coded anime metadata assumptions.
