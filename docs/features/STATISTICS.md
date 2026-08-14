# Statistics & Examination System

Kaiteyo's learning analytics lives in one place: the **Statistics** screen
(`StatisticsScreen`) backed by the **`StatisticsController`** (Koin singleton)
and the pure-domain calculators under
`core/src/commonMain/kotlin/ua/syt0r/kanji/core/statistics/`.

Everything the screen renders is computed from real database data. There are no
hardcoded charts, no fake exam scores and no placeholder tabs.

## Architecture

```
UserDataDatabase (SQLDelight)  ──►  StatisticsRepository  ──►  StatisticsController
                                                                       │
                                              ┌────────────────────────┴───────────────┐
                                              ▼                                      ▼
                              StatisticsCalculator (aggregates)          Domain calculators
                                              │                        (pure functions, tested)
                                              ▼
                                    StatisticsScreen (one destination)
                                              │
                                              ├─ Overview (profile, velocity, heatmap)
                                              ├─ Knowledge (JLPT / frequency coverage)
                                              ├─ Retention (SRS grades, intervals)
                                              ├─ Sessions / mistakes / weak entities
                                              ├─ Goals (+ append-only goal history)
                                              └─ Exams (generator → runner → graded review)
```

### Navigation consolidation

There is exactly **one** statistics destination. The old `StatisticsDashboardV2`
(`ModernStatisticsDashboard.kt`) and its `DeckStatisticsScreen` wrapper were
removed; the unified `StatisticsScreen` is wired into:

- `MainNavigation` → `MainDestination.StatisticsDashboard`
- `HomeScreenData` → the Home **Stats** tab
- `LearningPowerHub` → the Statistics feature (previously rendered empty defaults)
- `CardManager` → the embedded **Stats** tab

## Data flow

1. **Recording** happens where study happens: `StatisticsRecorder` writes daily
   rollups, review-per-item totals, writing attempts, study sessions, exam
   records/questions and learning mistakes into the user database.
2. **Aggregation** (`StatisticsCalculator`) builds overviews, heatmaps, streaks,
   milestones and exam statistics from those tables — never from current deck
   state, so history stays accurate.
3. **Interpretation** happens in the pure calculators:

| Calculator | File | Answers |
|---|---|---|
| `ProfileCalculator` | `LearningProfile.kt` | "What am I strong/weak at?" |
| `VelocityCalculator` | `StudyVelocity.kt` | "How fast am I going?" |
| `DeckRetentionCalculator` | `DeckRetention.kt` | "Which decks are weak?" |
| `GrowthCalculator` | `KnowledgeGrowth.kt` | "How has knowledge changed over time?" |
| `WeeklyExam` | `WeeklyExam.kt` | "What should my weekly exam cover?" |
| `GoalHistory` | `GoalHistory.kt` | "Do I complete my goals?" |

All six are pure Kotlin (no Compose, no DB) and covered by unit tests in
`core/src/commonTest/.../statistics/`.

## Metric definitions (documented, shared)

- **Studied** — the item has at least one recorded review.
- **Learned** — reviewed with an FSRS interval ≥ 1 day.
- **Mature** — interval ≥ 21 days. **Mastered** — interval ≥ 180 days.
- **Weak** — ≥ 3 lapses on the FSRS card.
- **Retention** — `correct / total` over the window (per grade, per interval
  bucket, per age band, per deck).
- **Overall accuracy** — correct reviews ÷ total reviews, all time.
- **Velocity window** — trailing 30 local days; rates are *weekly* (reviews/day,
  new items/week, study hours/week, writing attempts/week, exams/month).
- **Exam score delta** — average accuracy of the second half of completed exams
  in the window minus the first half (needs ≥ 4 exams), in percentage points.

## Examination system

- `ExamGenerator` — builds questions from **actually studied** items
  (kanji from the studied catalog with radicals/stroke counts; vocabulary from
  user decks), with JLPT distribution, production (free-text) + recognition
  (multiple-choice) question types, valid distractors and a reproducible seed.
- `ExamRunnerScreen` — question flow with option selection / text input,
  per-question answer persistence, countdown timer and quit-with-confirmation.
- `ExamScorer` — normalizes answers and categorizes mistakes; wrong answers
  become `LearningMistake` records that feed the weakness analytics.
- **Weekly exam** — preset over the trailing 7 days (`startWeeklyExam`,
  `weeklyExamSummary` for the preview).
- `GradedExam` results review — every question, your answer, the correct answer
  and the mistake category; dismissed via `clearLastGradedExam()`.

## Goals

Goals are persisted locally (`statisticsGoalsJson`), progress is derived from
real counters (`goalProgress()`), and every goal-list change appends an
append-only snapshot to `statisticsGoalHistoryJson`. `GoalHistory` then answers
trend questions (completion ratio per day, longest all-completed streak, most
frequently failed goal types) without ever reconstructing the past from current
state.

## Export & privacy

- `exportReport()` — human-readable summary (fully data-backed).
- `exportCsv()` — daily rollups (date, reviews, new, correct, incorrect,
  lapses, study time, writing, exams).
- `exportJson()` — overview/knowledge/JLPT/exams/profile/velocity aggregates;
  no internal database details.
- Everything is computed **offline**. No statistics leave the device.

## Performance

- Daily rollups are precomputed per review (`StatisticsRecorder` updates
  counters incrementally — no full-history scans on every answer).
- Long histories are aggregated in SQL and down-sampled for charts
  (`GrowthCalculator.sample`, capped drill-down lists).
- The UI reads state from the controller; nothing loads whole history into
  Compose.
