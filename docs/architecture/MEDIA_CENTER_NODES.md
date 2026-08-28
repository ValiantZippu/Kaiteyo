# Media Center — Nodes

> Status: STUB (graph materialized, full lyric indexing P1-2 implemented)  
> Files: `desktop/engine/media/MediaNodeFamily.kt`, `desktop/engine/events/EventLog.kt`, `AppState.kt:mediaNodeGraph`

## Target family (NODE §130, §83)

| Media concept | Node type(s) | Edges |
|---|---|---|
| `MediaItem` | `media_source` → `series`/`anime`/`movie`/`episode`/`video`/`audio` | `belongs_to`, `contains` |
| `SubtitleTrack` + cues | `subtitle_track` → `subtitle_line` | `contains`; `word→line` `appears_in_media` |
| scenes | `scene` | `belongs_to` → episode; `appears_in_scene` |
| screenshots/clips | `screenshot`, `clip` | `belongs_to`, `depicts`, `mined_from` |
| playlists | `playlist` | `belongs_to` → members |

## Current graph

`MediaNodeGraph` (`MediaNodeFamily.kt`) builds `Series → Episode → Scene → SubtitleLine(index, startMs, endMs, text, exposureCount)`. Series inferred from filename (` - EP\d+`), episode numbered by insertion, `watchTimeMs` max of mining/bookmark timestamps.

- **Seed:** `AppState.mediaNodeGraph` lazy (`miningEvents.forEach(addMiningEvent)`, `bookmarks.forEach(addBookmark)`).
- **Live:** `MediaEngine.indexSubtitleTrack()` on `openSubtitleFile`/companion load (idempotent, preserves `exposureCount=1` via ±500ms), `recordMiningEvent → addMiningEvent`, `addBookmark → addBookmark`.
- **Idempotent:** `indexSubtitleTrack` replaces previous `idx-*` scenes for same `mediaPath`; re-index after subtitle edits is safe.
- **Query:** `allLines()` flattened for “Where did I learn this?” media exposure with episode + timestamp jump (via `MediaNodeBridge` / `MediaReferenceStore`).

## Cards ↔ Media

`MediaReference.cardId` carries the **real** `DesktopCard.id` (`MediaEngine.onMined → MediaCentreDesktopHost → MediaReferenceStore.record` `Mined`), so `MediaNodeBridge` emits `Card -mined_from-> SubtitleLine`. Unmined line stays `exposureCount=0`; mined reference without card id stays tag-only (never invented).

## Acceptance (§130)

- Subtitle → popup → mine → card ≤4 actions, zero app switching — via `MediaView` + `DictionaryPopup` + `MiningEngine`.
- `appears_in_media` at 10k+ lines: `SubtitleSearchIndex` (library-wide) + graph `allLines()` are `runCatching`/capped; perf tests are P4.
- Re-indexing after edits is safe and idempotent — verified by `MediaNodeGraphIndexTest` (idempotent, preserves mined, multi-media isolated).
