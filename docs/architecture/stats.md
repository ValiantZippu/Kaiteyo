# Kaiteyo — Stats

> **Status**: `IMPLEMENTED` (event-driven stats, heatmap, AFK) + `ARCHITECTED` (unified Activity ledger derivation).
> Companion: `events.md`, `core.md`, `data-model.md`, `../architecture/statistics.md`, `nodes/EVENT_CATALOG.md`.

## 1. What it is

**Derived** analytics — Stats never owns a separate calendar. Every number derives from the single `ActivityEvent` ledger (events.md). Home and Stats consume the same data; there is exactly one heatmap, one streak, one study-time number.

## 2. Data model

```kotlin
data class ActivityEvent(
    val id: String, // ULID
    val type: EventType, // enum: Studied, Watched, Read, Mined, Played, Reviewed, Completed, Added, Searched, ...
    val timestamp: Instant,
    val contentId: ContentId?,
    val deckId: String?,
    val sessionId: String?,
    val details: JsonObject, // typed per event (see events.md)
)

// Derived aggregates (materialized in daily_stats + computed on demand)
data class DailyAggregate(
    val date: LocalDate,
    val reviews: Int, val newCards: Int, val correct: Int, val incorrect: Int, val lapses: Int,
    val studyTimeMs: Long, // engagement-based (see §5)
    val mediaTimeMs: Long,
    val readingTimeMs: Long,
    val minedCount: Int,
    val kanjiLearned: Int,
    val vocabMined: Int,
)

data class StatsSnapshot(
    val daily: List<DailyAggregate>, // for heatmap / calendar
    val weekly: WeeklyAggregate,
    val monthly: MonthlyAggregate,
    val yearly: YearlyAggregate,
    val streak: Streak, // current, longest, last date
    val kanjiCoverage: KanjiCoverage, // frequency/JLPT bands
    val vocabCoverage: VocabCoverage,
    val cardsCreated: Int,
    val platformActivity: Map<String,Int>, // per platform
)
```

## 3. Derivation

```
ActivityEvent (append-only, offline-first, indexed)
  →daily_stats (materialized, updated on each Study/Watch/Read/Mine event)
  →weekly/monthly/yearly rollups (sum of daily)
  →streaks (consecutive days where reviews>0 or studied>0)
  →studyTime (engaged time, not wall time — see §5)
  →coverage (unique kanji/vocab encountered/mined/mastered)
```

Never fabricate. If no event, count is 0 / blank — not a fake estimate.

## 4. Dimensions

Daily / weekly / monthly / yearly / lifetime; studied / watched / read / mined / played / reviewed / completed / added / searched; study time / media time / reading time; kanji learned / vocab mined / cards created; deck progress; JLPT estimates (derived, never a fake score); frequency coverage; retention/accuracy; mature/new/reviewed/lapses; active/idle/media/mining/game time.

All surfaced per blueprint §46.

## 5. Active-time detection (AFK model)

Spec: `../architecture/statistics.md` + `docs/desktop/engine/activity/ActivityTracker.kt` (suite, smart AFK).

- **Never count app-open time as study time.** Activity signals: keyboard, mouse, touch, controller, window focus, media playback, study interaction, scrolling, drawing/typing.
- **Modes**: SMART (context-aware timeouts: General 2m / Study 5m / Writing 6m / Media 10m) and CUSTOM (fixed 1–120m).
- **Engaged time** = `engagedSince(sessionStartedAt)` — pure overlap sum, always ≤ wall time; falls back to wall time when tracking disabled.
- Stats distinguish active study / passive media / idle / background (inactive never counted as study).

## 6. Heatmap

Anki-style yearly heatmap with year navigation (52-weeks ↔ calendar-year chips, animated slide). Intensity = meaningful activity (not clicks). Day drill-down tooltip: date, study duration, active duration, decks, cards, kanji/vocab/grammar, media, mining, exams, game activity. Blank days stay blank. Surfaced in both Home (glance) and Stats (full).

## 7. Home vs Stats separation

| Surface | Shows |
|---------|-------|
| Home (dashboard) | Continue (resume), Recent, Study progress (due/new), Media/Reading progress, Daily activity (today), Time remaining, Goals, Quick actions — glanceable, derived |
| Stats (full) | Heatmap, curves, retention, goals, velocity, drill-down, year nav, exam/achievement detail — analytical |

Home does not duplicate Stats visualizations per blueprint §41.

## 8. APIs

```kotlin
interface StatisticsService {
    fun daily(date: LocalDate): DailyAggregate?
    fun range(from: LocalDate, to: LocalDate): List<DailyAggregate>
    fun snapshot(): Flow<StatsSnapshot>
    fun streak(): Streak
    suspend fun exportSessionsCsv(): File
    suspend fun exportSessionsJson(): File
}
```

## 9. Caching / sync / offline

- `daily_stats` materialized rollup (one row per date, updated on event append).
- Range queries are DB queries, not full ledger scans; indexed by date.
- Sync: ActivityEvents replicate via SyncService; stats recompute locally (no cross-device stat merge conflict — events are the truth).
- Offline: fully available (ledger is local).

## 10. Failure states

No events yet (empty state: "No activity yet — start studying"), partial ledger (show what exists), corrupt rollup (recompute from events, log, Debug).

## 11. Evolution

New EventType → new derivation (add DailyAggregate field with migration if materialized, else computed). No schema break for computed metrics.
