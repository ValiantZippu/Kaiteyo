# Kaiteyo Architecture — Study Engine & Card Model

**Status**: Implemented — core SRS (FSRS-5) shipped; unified learning model in the suite
**Owner**: core `ua.syt0r.kanji.core.srs` + suite `ua.syt0r.kanji.desktop.engine.learning`
**Related**: `docs/architecture/database.md` · `docs/architecture/decisions/0006-fsrs-srs.md` ·
`docs/user-guide/STUDYING.md` · `docs/architecture/statistics.md`

## 1. Purpose

Kaiteyo owns its study system end-to-end (§199): decks, cards, notes, reviews and
scheduling are first-party; Anki stays an integration. The card model is
**note → card type → card**, each card carrying its own FSRS state, and every review is an
immutable event so statistics never depend on transient UI state. The suite learning layer
bridges the legacy `DesktopCard` pool so existing views keep working while new systems use
the richer model.

## 2. Architecture

```
UI (Review/Study, Library, Exams, Writing, Stats)
   │
   ▼
LearningEngine            ← facade over the unified learning model (one entry point)
   ├── LearningStore      ← single persisted source of truth (~/.kaiteyo/learning/learning.json)
   ├── StudyEngine        ← queue building + grading through the shared SRS scheduler
   ├── CardGenerator      ← deterministic note → card generation
   ├── MistakeEngine      ← real-mistake queue (Again, failed strokes, wrong exam answers)
   ├── ImportExportEngine ← JSON/CSV/TSV snapshot import/export
   ├── StatisticsRepository ← events → metrics (see statistics.md)
   └── ExamEngine         ← exams (see exams.md)
        │
        ▼
engine/srs (SrsScheduler)  ← shared scheduler (FSRS-5 core, also used by the core app)
```

## 3. Card model (`LearningModels.kt`)

### `LearningNote` — one fact (content)
`id` · `kind: LearningItemKind` · `expression` (surface form) · `meanings` · `reading`
(primary) · `onReadings`/`kunReadings` · `radicals` · `components` · `strokeCount` ·
`jlpt: Int?` · `grade: Int?` · `frequency: Int?` · `pitchAccent` · `tags` · `examples` ·
`audioPath` · `source: NoteSource` · `createdAt`/`updatedAt`.

- `allReadings` = on + kun + primary, distinct, non-blank.
- `dedupeKey()` = `"kind:expression:reading-prefix-32"` — stable dedupe.

`LearningItemKind`: Kanji · Vocabulary · Kana · Radical · Grammar · Custom.
`NoteSourceType`: Builtin · Import · Dictionary · MediaSubtitle · MediaAudio · MediaImage ·
MediaVideo · Anki · Custom. `NoteSource(type, sourceId, sourceDetail, timestampMs,
sourceText)` — provenance so source data never overwrites user data.

### `CardType` — the study direction
Recognition · Meaning · Reading · Writing · Listening · Production · Cloze · Pattern.
`toStudyMode()` bridges to legacy study modes; `defaultsFor(kind)`:
- Kanji → Recognition, Meaning, Reading, Writing
- Vocabulary → Recognition, Meaning, Reading, Listening, Production
- Kana → Recognition, Meaning, Reading, Writing, Listening
- Radical → Recognition, Meaning
- Grammar → Meaning, Pattern, Cloze
- Custom → Recognition, Meaning

### `NoteCard` — note × card type × deck
`id` (stable: `noteId::CardType::deckIdHash`) · `noteId` · `cardType` · `deckId` ·
`status: SrsStatus` (New/Learning/Review/Relearning/Suspended/Buried) · `intervalDays` ·
`dueAt` · `lapses` · `reps` · `ease` · `accuracy` (running 0..1) · `streak`/`bestStreak` ·
`lastReviewedAt` · `buried` (persisted; session-scoped bury keeps it out of queues).

Derived: `isNew`, `isSuspended`, `stage` (`LearningStage.of(status, intervalDays)` —
explicit criteria: New/Suspended/Buried → Introduced; Learning/Relearning → Learning;
Review with interval ≥ 21 d → Mature else Established), `isDue` (Learning/Review/Relearning
with `dueAt <= now`).

### `DeckStudyConfig` — per-deck settings
`deckId` · `dailyNewLimit` (20) · `dailyReviewLimit` (200) · `learningStepsMinutes`
([1,10]) · `graduatingIntervalDays` (1) · `easyIntervalDays` (4) · `maximumIntervalDays`
(3650) · `buryRelatedNew` (true) / `buryRelatedReviews` (false) · `suspendOnLapse` ·
`enabledCardTypes` (empty = per-kind defaults) · `interleaveNewAndReviews` (true).
`cardTypesFor(kind)` resolves enabled or defaults.

## 4. LearningStore — persistence

Single source of truth: `~/.kaiteyo/learning/learning.json` (`LearningSnapshot`:
notes, cards, deckConfigs, reviewEvents, writingAttempts, examResults, sessions,
`revision`). Pretty-printed JSON with `ignoreUnknownKeys` + `encodeDefaults` (forward
compatible reads). All collections are Compose-reactive; `revision` bumps on structural
mutation (view cache invalidation).

- **Notes**: `upsertNote` dedupes by id then `kind+expression`; **custom-note
  protection** — an imported note never overwrites a `Custom` note (returns the existing
  one); otherwise merges (keeps original createdAt + id, unions tags, keeps existing
  meanings when the import has none). `upsertNotes` batches then saves once. `deleteNote`
  cascades its cards.
- **Cards**: `upsertCard`/`upsertCards`, `removeCard`, `updateCardState` (single-write
  SRS update: status/interval/due/lapses/reps/ease/accuracy/streak/bestStreak/
  lastReviewedAt), `setBuried`.
- **Deck config**: `deckConfig(id)` lazily materializes defaults (and persists);
  `setDeckConfig`.
- **Events (append-only, immutable)**: `recordReview`, `recordWriting` (index 0),
  `recordExam` (index 0), `recordSession`/`updateSession` (resumable sessions).
- **Bridging**: `toDesktopCard(s)` maps the unified model back to the legacy pool so
  existing views keep working during migration.

## 5. StudyEngine — queue & grading

### Queue (`buildQueue(deckId, mode, now, includeNew, newLimit, reviewLimit)`)
1. Deck cards filtered by mode: `Flashcards` is inclusive; specific modes restrict to
   `cardType.toStudyMode() == mode`.
2. Active = not suspended, not buried.
3. New cards (if included), up to `dailyNewLimit` (or override).
4. Due cards (`status != New && isDue`), sorted by `dueAt`, up to `dailyReviewLimit`.
5. Ordering: interleave (new/due alternating) when configured, else new-then-due.
6. Maps to `StudyQueueItem(card, note)` — always real state, never mocks.

`dueCount`/`newCount` — deck badges/forecast.

### Grading (`grade(item, rating, activityType, responseTimeMs, mistakes, examId, sessionId, mode, now, params)`)
1. `SrsScheduler.schedule(currentStatus.toLike(), currentInterval, currentEase, lapses,
   learningSteps=0, rating.toLike(), now, params=Default)` → status (Learning/Review/
   Relearning), intervalDays, dueAt, ease.
2. Card state: `reps+1`; running `accuracy = (reps·acc + correct)/newReps` clamped 0..1;
   `streak` +1 on pass else 0 (`bestStreak` tracked); `lapses+1` on Again;
   `lastReviewedAt = now`.
3. `store.updateCardState(...)` — one write.
4. Appends `LearningReviewEvent` (full fidelity: statusBefore/After, intervalBefore/
   After, wasNew, lapsesAfter, response time, mistakes, writing accuracy, exam/session id).
5. Returns `GradeResult(card, event)`.

The shared `SrsScheduler` (core FSRS-5, `SrsParameters.Default`) is the single scheduler —
suite and core app cannot drift.

## 6. CardGenerator — deterministic regeneration

`generateForDeck(note, deckId, config, existing, now)`:
- Card types from `config.cardTypesFor(note.kind)`.
- Stable id per note+type+deck → **existing cards keep their SRS state in place**;
  newly-enabled types start as `New`.
- Returns the full set so callers replace atomically — regeneration never duplicates
  cards and never resets SRS.
`generateBatch` (bulk deck population), `previewCardTypes` (deck editor preview).

## 7. MistakeEngine

`MistakeCategory` (labels: e.g. Again, wrong reading, wrong kanji, …) · `MistakeItem` ·
`queue(limit=200)` — real mistakes from Again grades, failed writing attempts, wrong exam
answers and lapsed cards; `forCategory`, `asStudyQueue` (feeds a mistake-focused study
mode), `breakdown()` (category histogram). Never fabricated (PRODUCT_AUDIT: seeding is
first-run-only and SRS-neutral).

## 8. Import/export (learning layer)

- `exportSnapshot` — full-fidelity JSON (`LearningSnapshot`), the canonical backup.
- `exportCsv` / `exportTsv` — notes/cards flattened with proper escaping; imports
  dedupe + validate (`ImportResult` with counts; `importJson` reports progress).
- Anki/APKG + unified pipeline live in core `transfer` (see `docs/architecture/backup.md`
  and `docs/architecture/integrations.md`).

## 9. LearningEngine — facade

Public surface (all backed by the store, never mock data):
- Bridge: `syncFromLegacy(cards)` (once per launch), `recordLegacyReview`,
  `recordWritingAttempt`, `recordEvaluatedWriting` (per-stroke evaluation),
  `legacyCardsForDeck`, `allLegacyCards`, `ensureCards(deckId)`.
- Deck config: `deckStudyConfig`/`saveDeckStudyConfig`.
- Mistakes: `mistakeQueue`, `mistakeBreakdown`, `mistakeCards`.
- Import/export: `exportSnapshotJson/Csv/Tsv`, `importJson/Csv/Tsv`.
- Deck totals: `deckTotals(deckId): DeckLearningTotals`.
- Stats (delegates to `StatisticsRepository`): `jlptCoverage`, `characterProgress`,
  `writingStats`, `weakestKanji(≥2 attempts, lowest accuracy)`, `recentWritingAttempts`,
  `writingAccuracyTrend`, `cardHistory`, `examHistory`/`examAggregates`/`accuracyByType/
  Jlpt/Section/ExamType`, `examTrend`, `studyVsExamGap`, `forecast(days)`, `dueToday`,
  `mistakeSnapshot`, `periodStats`, `goalProgress`, `streaks`, `totalStudyTimeMs`.
- Unified search: `search(query)` over notes/cards with `scoreNote` ranking →
  `UnifiedSearchResult`.
- Kind mapping `ContentKind.toLearningItemKind()` bridges the legacy content model.

## 10. Writing engine

- Stroke model with real stroke data (§284–§285): `StrokeAttempt(strokeIndex, correct,
  deviation, mistake)`; `WritingAttemptEvent` (attempted vs expected, per-stroke
  evaluation, accuracy, mistakeCount, completed, duration) persisted; per-character
  summaries and trends derived from events.
- Stroke evaluation: core `stroke_evaluator` handles kanji **and** kana (voiced kana
  evaluate against their base shape; kana decks start writing practice like kanji decks).
- Recorded through `recordEvaluatedWriting` so stats, weakest-kanji and the knowledge
  profile all read the same event stream.

## 11. Error model

- No cards → honest empty state (§297); corrupt snapshot → `runCatching` reset to
  defaults (§219); queue is derived from DB state — never rebuilt per frame.
- Anki unavailable → "Anki unavailable", Kaiteyo cards still created (§201).
- Import of malformed data → `ImportResult` with failure counts; custom notes protected.

## 12. Tests

- Core: `FsrsSchedulerTest` (`:core:allTests`).
- Suite: `GoalsEngineTest`, `StatisticsRepositoryTest` (weakest-kanji math), 
  `ActivityTrackerTest`; engine tests for queue building, grading fidelity, and
  regeneration idempotence.
- Gaps: schedule-fidelity at scale (§279), `learning.json` migration tests, end-to-end
  deck→review→stats flows (§218).

## 13. Open items

- Consolidate the two SRS/deck/statistics implementations (audit §7-1).
- Archived-deck filter + restore UI (data exists, `is_archived`).
- AI-assisted scheduling is RESEARCH only (FSRS stays the scheduler).
- Node-layer knowledge state (ADR-0013, `nodes/KNOWLEDGE_STATE_MODEL.md`) will build  on this event stream — events are the input, never UI state (§210–§213).

## 14. Node-layer integration (TARGET — ADR-0013, NODE §84–§85)

The study engine is the primary **evidence source** for user knowledge.

### 14.1 Card → node teaching edges

- Every card declares the language nodes it teaches (`teaches` edges: card → kanji /
  vocab / grammar). Generated from the card's note/content deterministically
  (CardGenerator already deterministically maps notes → cards).
- `ReviewService.submitReview` emits `card_reviewed` with `nodeIds[]`; the knowledge
  service consumes it (KNOWLEDGE_STATE_MODEL §4) — **the scheduler never writes
  knowledge tables and vice versa** (STANDARDS §6 never-change).

### 14.2 Knowledge-informed selection (candidate order only)

- Review *candidate selection* may prioritize cards whose taught nodes are
  `WEAK`/`FORGOTTEN`/`LEARNING` (NODE_DATA_MODEL §8.5); FSRS arithmetic is untouched.
- Selection is a hint, not a schedule override — the scheduler owns intervals.

### 14.3 Mastery & production

- `WRITING_CAPABLE`/`PRODUCED` transitions come from writing practice + production
  reviews (§84); the writing stroke evaluator already produces scored attempts.
- `mastery_reached` events surface as achievements and (in Journey) rewards.

### 14.4 Acceptance criteria

- Every card resolves to ≥1 taught node; `card_reviewed` events carry complete
  `nodeIds[]`.
- Knowledge state changes are derived solely from the event stream (replayable).
- FSRS scheduling behavior is bit-identical with/without the knowledge layer (guard
  test, TEST_PLAN §4).
