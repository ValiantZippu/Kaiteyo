# Kaiteyo Architecture — Statistics & Event Model

**Status**: Implemented — event-driven statistics (suite) + SQLDelight statistics (core)
**Owner**: suite `ua.syt0r.kanji.desktop.engine.learning.StatisticsRepository` +
`engine.activity` · core `StatisticsController`
**Related**: `docs/architecture/study-engine.md` · `docs/architecture/database.md` ·
`docs/features/STATISTICS.md` · `docs/architecture/exams.md`

## 1. Purpose

Statistics are **derived from events**, never from live UI state (§213). Raw events →
aggregation → derived metrics → presentation. The suite keeps the full event log in
`learning.json`; the core app precomputes per-day rollups (`daily_stats`) so heatmaps and
the today panel are O(1). Historical data is never destroyed, so metrics can evolve
without losing history.

## 2. Event sources (suite)

All immutable, captured at the moment of the action (§211 — id, timestamp, type, source,
payload, schemaVersion):

| Event | Produced by | Stored in `LearningSnapshot` |
|---|---|---|
| `LearningReviewEvent` | `StudyEngine.grade` (review, exam, writing-adjacent paths) | `reviewEvents` |
| `WritingAttemptEvent` | `recordWritingAttempt` / `recordEvaluatedWriting` | `writingAttempts` |
| `ExamResult` / `ExamQuestionResult` | `ExamEngine.evaluate` | `examResults` |
| `StudySessionRecord` | study sessions (resumable) | `sessions` |
| `learning_mistake` (core) / mistake queue (suite) | Again grades, failed strokes, wrong exam answers | mistake engine |

The **core** app records the same shapes relationally: `review_history`,
`writing_attempt`, `exam`/`exam_question`, `study_session`, `daily_stats` rollups —
see `docs/architecture/database.md`.

## 3. StatisticsRepository (suite) — the single pipeline

Pure functions over the store; every metric below reads events/cards, never UI state.

### Period stats (`periodStats(store, period, today)` → `PeriodStats`)
`StatsPeriod`: Today / Week (Mon–Sun) / Month / Year / All.
- Reviews, `newCards` (from `event.wasNew`), `correct`, `again`, `studyTimeMs`
  (sum of review response times + writing durations), `activeDays` (distinct local days),
  writing attempts/correct, exams/question counts/correct.
- Derived: `answered`, `accuracy`, `writingAccuracy`, `examAccuracy`.
- `lifetime()` = `periodStats(All)`.

### Streaks (`streaks` → `StreakInfo(current, longest)`)
Active days = distinct local days with any review/writing/exam event. Current streak
walks backward from today; longest scans sorted days for consecutive runs. Malformed day
strings are skipped via `runCatching` (a corrupt summary can never crash the Dashboard).

### JLPT coverage (`jlptCoverage` → `List<JlptCoverage>`)
Per level N5–N1 from notes with real `jlpt` metadata: total, known (stage Established/
Mature), learning, unseen (Introduced), due. Derived fractions: `introducedFraction`,
`establishedFraction`. **Labeled approximate** (§290) — never an official JLPT result.

### Character progress (`characterProgress`)
Unique kanji studied/established, unique vocabulary established, writing attempt totals —
derived from events and card stages.

### Writing stats
- `writingStats(limit)` — per-character rows (expression, attempts, correct, accuracy,
  kind).
- `weakestKanji(limit=8)` — **≥ 2 attempts, lowest accuracy first** (`correct =
  accuracy ≥ 0.99`), kanji-kind only. Drives the Dashboard Writing Practice card.

### Exam analytics
`examHistory(limit=30)`, `examAggregates` (counts/accuracy/avg time), `accuracyByType`,
`accuracyByJlpt`, `accuracyBySection`, `accuracyByExamType`, `examTrend` — all from
`ExamResult.questions`.

### Study vs exam gap (`studyVsExamGap`)
`studyAccuracy`, `examRecognitionAccuracy`, `examProductionAccuracy`,
`examWritingAccuracy` — the recognition-vs-production insight (drives smart exam
recommendations).

### Forecast & due
`forecast(days=30)` → per-day due counts from real card due dates; `dueToday` (excludes
suspended/buried/new).

### Daily series, mistakes, totals
`dailySeries(days)` (heatmap/trend input), `mistakeSnapshot` (category counts),
`totalStudyTimeMs`.

## 4. Goals (`GoalsRepository`)

`LearningGoal(id, name, metric, target, period)` with `GoalMetric { Reviews, NewCards,
Minutes }`. Defaults: Daily reviews 20 · Daily new 5 · Daily minutes 15 · Weekly reviews
150 · Weekly minutes 90. `progress(goal, stats)` → `GoalProgress(achieved, fraction
clamped 0..1, complete)`. `allProgress` computes from real `periodStats`.

The configurable `stats.daily-target` setting (Settings → Statistics → Daily review
target, default 20) feeds **both** the Dashboard Study Target card and the Goals card's
daily-reviews goal so they can never disagree (`GoalsEngine.defaultGoals(dailyReviewTarget)`).

## 5. Engagement time (AFK model, §212) — `ActivityTracker`

Real activity is modeled as **timestamped intervals**: a signal (global pointer/keyboard
observation at the workspace shell root — clicks, drags, hover/scroll past a threshold,
never consumed; plus study/writing session-start and grading signals) opens or extends an
engagement; a lapse past the configurable timeout closes it (AFK); the next signal opens a
new interval. AFK state is derived per-second, O(1), no timers in the engine.

- `endReview`/`endWriting` credit `engagedSince(sessionStartedAt)` — a pure overlap sum
  always ≤ wall time, so walking away mid-session never inflates study time. Falls back
  to wall time when tracking is disabled.
- **Smart vs custom**: `activity.afk-mode` (Smart = context-aware: General 2 min / Study 5
  / Writing 6 / Media 10; Custom = fixed `activity.afk-timeout-minutes` 1–120).
- Stats distinguish active study, passive media, idle, background (§212).
- Tests: `ActivityTrackerTest` (overlap, AFK pause/resume, smart vs custom, disabled
  fallback, per-day buckets, reset).

## 6. Heatmap

From real review + new activity. Suite: `HeatmapEngine.buildAlignedYear` + `dailySeries`;
52-weeks ↔ calendar-year switching with animated push/slide (respects reduced motion);
blank days stay blank; intensity is real study, not clicks (§214). Core: `daily_stats`
rollups keep per-day queries O(1).

## 7. Knowledge profile

`KnowledgeProfileEngine` (suite) — a study-based estimate, never a fake score:
- Dimensions: kana / kanji / vocabulary / writing coverage.
- Measured accuracy where real attempts exist (writing accuracy from stroke events).
- Cumulative JLPT coverage N5→N1 — **labeled approximate** (§290).
- Frequency-band coverage (top 1k/2k/5k/10k) from real frequency metadata.
- Data-driven confidence label. Surface: Dashboard "Knowledge snapshot" card + Stats'
  Learning Overview + kana tiles. Target: the node-based `KNOWLEDGE_STATE_MODEL.md`
  (ADR-0013) builds on this same event stream.

## 8. Surfaces & duplication

- Core: `StatisticsScreen` (one destination, `StatisticsController`) — heatmap, day
  drill-down, review aggregates; `CardInspector` shared component.
- Suite: `StatsView` + Learning Overview + knowledge profile + study/exam gap.
- **Duplication**: the two statistics stacks (SQLDelight-driven core vs JSON-driven
  suite) are part of the consolidation decision (audit §7-1). Until then both are real;
  neither is derived from UI state.

## 9. Tests

- `StatisticsRepositoryTest` — weakest-kanji derivation (≥2 attempts, `accuracy ≥ 0.99`
  correct, kanji-kind filtering, weakest-first + limit, empty-store states), period
  windowing, goal math.
- `GoalsEngineTest` — today-only windowing, fraction clamping, completion, configurable
  daily target, weekly/monthly windows, malformed-day skipping, minutes/new-card metrics.
- `ActivityTrackerTest` — engagement overlap, AFK pause/resume, smart vs custom.
- Core DB tests: `:core:allTests` covers review-history aggregation math.
- Gaps: aggregation at scale (§369), timezone-boundary tests, event-schema versioning.

## 10. Rules

- Never fabricate precision (§290): estimates are labeled; JLPT coverage is approximate.
- Events carry schemaVersion discipline (§211); no arbitrary UI state in analytics.
- Privacy: local by default (§294); sync/telemetry only with disclosure (§295).

## 11. Node-layer integration (TARGET — ADR-0013, NODE §131, §85)

### 11.1 One event stream, all worlds

The suite and core event stacks consolidate onto the shared `event_log` contract
([EVENT_CATALOG](nodes/EVENT_CATALOG.md)): study, media, mining, Journey, discovery,
exam, sync, and system events all flow through it (STANDARDS §210–§213). Each
subsystem keeps emitting its current events; the catalog defines the unified payload
schemas and consumers.

### 11.2 Knowledge scores as derived metrics (§85)

- `knowledge_score` rows are derived caches rebuilt from `user_knowledge` + `event_log`
  — never written by UI, never treated as truth (KNOWLEDGE_STATE_MODEL §5).
- Stats Overview renders the simplified dials (kanji/vocab/listening/reading/writing)
  with basis + confidence labels (estimated where partial, §290).

### 11.3 Journey & discovery sections (§131)

- Journey stats (locations visited, discoveries, photos, quests, story progress) derive
  from Journey events; the section appears only when Journey data exists.
- Discovery stats (by kind, over time — NODE_DATA_MODEL §8.7) feed Browse and Home.

### 11.4 Drill-down per node

- Stats drill-down accepts a node id: "my history with 食" merges reviews, lookups,
  media exposures, Journey encounters, and exam answers for that node — the
  per-node evidence ledger (§84).

### 11.5 Acceptance criteria

- Replaying the log reproduces heatmap + knowledge scores (TEST_PLAN §5).
- Every displayed number has an explainable basis (hover/tap) or an explicit estimate
  label (§290).
- No fabricated precision anywhere (existing §10 rule, extended to Journey data).
