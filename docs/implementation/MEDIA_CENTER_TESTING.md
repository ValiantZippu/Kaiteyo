# Media Center — Testing

> Status: IMPLEMENTED — unit + integration, E2E manual pending

## Unit

| Test | Coverage | File |
|---|---|---|
| `MediaEngineTickSafetyTest` | 50 hostile ticks, unavailable+throwing backend cleanup, no-backend no-op, healthy advance, segmentation helpers never throw | `engine/media/MediaEngineTickSafetyTest.kt` |
| `MediaEngineLifecycleTest` | 25× open/tick/hostile-swap burst + shutdown, idempotent shutdown, tick-after-shutdown safe | `MediaEngineLifecycleTest.kt` |
| `SubtitleEngineTest` | order, binary search, globalOffset, next/prev, dense markers, ASS tag strip, VTT header, empty safe | `SubtitleEngineTest.kt` |
| `SubtitleParserFuzzTest` | empty, BOM+CRLF, malformed timestamp skip, 10k huge, ASS without Format, commas, VTT NOTE, overlapping, garbage bytes no-throw | `SubtitleParserFuzzTest.kt` (new) |
| `MediaPlaylistTest` | CRUD/reorder/persist/missing | `MediaPlaylistTest.kt` |
| `MediaTuningModelsTest` | EQ preset integrity, adjustment clamping, display modes, screenshot naming | `MediaTuningModelsTest.kt` |
| `MediaStatisticsStoreTest` | watch/lookup/mine/session, daysBetween, coalesce | `MediaStatisticsStoreTest.kt` |
| `MediaLibraryTest` | addFile dedupe, upsert, companion subtitle, nextEpisode heuristic | `MediaLibraryTest.kt` |
| `MediaNodeGraphIndexTest` | idempotent index, mined preserved, empty no-op, multi-media isolated | `MediaNodeGraphIndexTest.kt` (new) |
| `MediaMiningDestinationTest` | Kaiteyo-only, Both+unreachable→pending, Anki-only disabled→fallback, provenance | `MediaMiningDestinationTest.kt` (new) |

## Integration

- `TextHookServer`/`PlayerStateWebSocket` localhost (pending live AnkiConnect, system media keys `WH_KEYBOARD_LL` on Windows).

## E2E (27-step §41, manual)

1 import video → 2 external subtitle → 3 open → 4 play → 5 subtitle follows → 6 transcript → 7 select text → 8 dictionary → 9 word → 10 mining context → 11 sentence → 12 audio/context → 13 card → 14 Kaiteyo destination → 15 card appears → 16 Anki → 17 card in Anki → 18 Both → 19 one action creates both → 20 disable Kaiteyo → 21 Anki-only works → 22 EventLog → 23 Node graph → 24 History → 25 Continue Watching → 26 relaunch → 27 resume.

Current: 14–15, 18–21 verified by `MediaMiningDestinationTest`; 5–8 verified by `SubtitleEngineTest` + `MediaEngine` tick; 24–27 verified by `PlaybackQueueDto` + `library.updateProgress` + `recordHistory`. Full manual sweep pending desktop runtime.

## Gaps (§280/§369/§357)

- Real-file playback (VLC/mpv), corrupt-container, large-library 10k perf — P4.
