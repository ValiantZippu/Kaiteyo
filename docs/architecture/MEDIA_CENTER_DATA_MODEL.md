# Media Center — Data Model

> Status: IMPLEMENTED  
> Source of truth: `desktop/engine/media/MediaLibrary.kt`, `MediaNodeFamily.kt`, `MediaEngine.kt`, `SubtitleParser.kt`

## Entities

| Entity | File | Key fields | Persistence |
|---|---|---|---|
| `MediaItem` | `MediaLibrary.kt` | `id, path, name, kind:MediaKind, sizeBytes, isRemote, durationMs, addedAt, lastPositionMs, lastWatchedAt, watchCount, completed, favorite, tags, collection, subtitlePath, episode, comprehension, note` + `progressFraction` derived | `~/.kaiteyo/media/library.json` (`LibraryDto`) |
| `MediaFolder` | `MediaLibrary.kt` | `path, includeSubdirs` | same JSON |
| `MediaPlaylist` | `MediaLibrary.kt` | `id, name, itemIds, createdAt, folderId, favorite` + helpers `renamed/withItem/withoutItem/withOrder/inFolder` | same JSON |
| `PlaylistFolder` | `MediaLibrary.kt` | nested folder tree | same JSON |
| `MediaBookmark` | `MediaEngine.kt` | `id, mediaPath, timestampMs, label, note, createdAt` | `~/.kaiteyo/media-state.json` (`MediaStateDto`) |
| `AudioClip` | `MediaEngine.kt` | `id, sourcePath, label, startMs, endMs, exportedPath, createdAt` | same |
| `MediaMiningEvent` | `MediaEngine.kt` | `cardId, mediaPath, mediaName, timestampMs, cueText, createdAt` | same, also `mediaNodeGraph` |
| `SubtitleCue` | `SubtitleParser.kt` | `id, startMs, endMs, text, style, speaker` | transient (subtitle files) + indexed in graph |
| `SubtitleTrack` | `SubtitleParser.kt` | `name, cues, format:Srt/Ass/Ssa/Vtt, language` | `SubtitleEngine` active track |
| `DownloadJob` | `MediaDownloadService.kt` | `id, url, fileName, targetPath, sizeBytes, downloadedBytes, state:DownloadState, error, createdAt` | `BrowserStateDto` + `~/.kaiteyo/downloads` files |
| `MediaNode` family | `MediaNodeFamily.kt` | `MediaSeries → MediaEpisode → MediaScene → SubtitleLine(index, startMs, endMs, text, exposureCount)` | derived, lazy `AppState.mediaNodeGraph` |
| `PlaybackQueueDto` | `MediaEngine.kt` | `ids, index` | `~/.kaiteyo/media/queue.json` |

## Relationships

```
MediaCollection (concept) → MediaItem → MediaAsset (file) → SubtitleTrack → SubtitleCue
PlaybackProgress (library.updateProgress) references MediaItem
MiningContext (MiningPayload.mediaRef + cueId) references MediaItem + SubtitleCue
MiningCard (DesktopCard) references MiningContext via note field + MediaMiningEvent
MediaNodeGraph: Series -contains-> Episode -contains-> Scene -contains-> SubtitleLine
              Word -appears_in_media-> SubtitleLine (via full index, ±500ms merge preserves mined count)
              Card -mined_from-> SubtitleLine (exposureCount=1)
```

## Invariants

- `MediaItem.path` is the stable key; remotes always “exist” (`fileExists` returns true for `isRemote`).
- `seriesKey`/`episodeNumber`/`nextEpisode` are heuristics (same folder + basename), not hard-coded anime schema.
- `MediaStateDto` + `PlaybackQueueDto` + library JSON all `runCatching` on load (corrupt → empty, never crash).
- Subtitle parsers are pure (`File/String → Track`), backend-independent, bounded errors → `SubtitleInvalid`.
- Download filenames are sanitized (`[\\/:*?\"<>|] → _`, no leading `.`, 120 chars).

## Gaps vs §14 target

- `MediaSource`/`MediaProvider`/`MediaMetadata` split (source/user/derived) is collapsed into `MediaItem` fields — intentionally, to avoid premature table explosion. Future split is an extension point, not a current requirement.
