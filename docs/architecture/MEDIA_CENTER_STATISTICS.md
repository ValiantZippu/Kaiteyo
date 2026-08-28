# Media Center — Statistics

> Status: PARTIAL (events → stores → home, heatmap P4-1 done)  
> Files: `desktop/engine/events/EventLog.kt`, `desktop/engine/media/MediaStatisticsStore.kt`, `desktop/engine/mining/MiningStatisticsStore.kt`, `desktop/ui/stats/StatsView.kt`, `desktop/ui/stats/HeatmapPanel.kt`

## Source of truth

`EventLog` (`~/.kaiteyo/event_log.json`, append-only, `eventId/occurredAt/eventType/source/payload/sessionId/schemaVersion`) — derived metrics are re-runnable; corrections append, never edit. `MediaStarted`, `MediaEnded`, `SubtitleSelected`, `DictionaryLookup`, `CardMined`, `BookmarkAdded` are the media family.

## Flow

```
PlaybackService (MediaEngine.onBackendEvent / openLookup / addBookmark / recordMiningEvent)
        ↓
EventLog.record(eventType, source=media, payload={mediaId, mediaName, timestampMs, query})
        ↓
MediaStatisticsStore (per-day buckets MediaDayStat{watchMs, studyMs, lookups, mined, sessions}, 366 days, coalesced 1s persist `stats.json`)
MiningStatisticsStore (per-day/per-source)
        ↓
StatisticsView / Home / Node graph / Heatmap
```

Media watch time is **separate from study time** (`recordWatch(ms, study=studyMode)` + `ActivityTracker` engaged intervals, never `app open` time).

## Metrics

| Metric | Source |
|---|---|
| `watchTime`, `activeMediaDays`, `mediaSessions`, `episodesCompleted` | `MediaStatisticsStore.totalWatchMs`, `activeDays`, `totalSessions`, `library.recordHistory` |
| `sentencesMined`, `wordsLookedUp`, `cardsCreated`, `audioExtracted` | `MiningStatisticsStore.totalMined`, `MediaStatisticsStore.totalLookups`, `MediaMiningEvent` |
| `mediaCompletionRate` | `MediaItem.progressFraction` + `library.updateProgress` |

No `PlayerScreen → increment random integer`; all via `EventLog` + stores.

## Heatmap (kanji-heatmap → Kaiteyo)

- `HeatmapPanel` (study) + `MediaImmersionHeatmap` (media) are GitHub-style intensity grids (kanji-heatmap `STUDY EVENT → DATE → ACTIVITY → AGGREGATION → CALENDAR → HEATMAP` repurposed).
- Media heatmap: 52 weeks, watchMs → level (0/ <15m/ <30m/ <60m/ ≥60m), `heatColor(level)` with `Ds*` tokens, 11dp cells.
- Both are `DsCard` sections in `StatsView` with `HeatmapEngine.currentStreak` + `MediaStatisticsStore.activeDays`.

## Home / Library

- `Home` → `Continue Watching` = `media_position_updated` derived from `MediaStatisticsStore` + `PlaybackQueueDto`, not bespoke store.
- `Library` sections = node queries (`mediaNodeGraph.series()`) with filters, not hard-coded lists (acceptance §130).

## Tests

`MediaStatisticsStoreTest`, `GoalsEngineTest`, `StreakEngineTest` exist; large-library perf (§369) is P4.
