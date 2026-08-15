# Kaiteyo Architecture — Exam System Specification

**Status**: Implemented (suite `ExamEngine` + workspace; core exam runner for letter
practice exams)
**Owner**: suite `ua.syt0r.kanji.desktop.engine.learning.ExamEngine` + `ui/exams/`
**Related**: `docs/architecture/study-engine.md` · `docs/architecture/statistics.md` ·
`docs/architecture/database.md`

## 1. Purpose

A modular, knowledge-aware exam system (§287–§289): questions are generated from what the
user actually studied — decks, JLPT bands, mistake history, weekly activity — never random
disconnected trivia. Answers are stored with question text, options, timing and exam
version so history stays interpretable if content changes. Exams also **feed the review
log**: answering exam questions updates SRS for the tested cards exactly like normal
reviews (activity type `Exam`), so exam behavior and study behavior never diverge.

## 2. Exam types (`ExamType`)

KanjiRecognition · KanjiReading · KanjiMeaning · VocabMeaning · VocabReading ·
VocabProduction · RadicalRecognition · GrammarStructure · GrammarUsage · MixedJlpt ·
**JlptSimulation** · Mistakes · Weekly.

## 3. Question types (`ExamQuestionType`)

- `MultipleChoiceMeaning` (`recognition:meaning`) · `MultipleChoiceReading`
  (`recognition:reading`) · `MultipleSelect` (`recognition:multiple`) · `Matching`
  (`recognition:matching`)
- `TypedReading` (`production:reading`) · `TypedExpression` (`production:writing`)
- `PatternSelection` (`production:grammar:pattern` — which pattern expresses this
  meaning) · `SentenceCompletion` (`recognition:reading:cloze` — fill the blank in a
  real example sentence).

## 4. Models

- `ExamQuestion(id, cardId, noteId, questionType, prompt, correctAnswer, options, jlpt,
  deckId)` — options only for choice/select types; answers are normalized for fair
  evaluation.
- `ExamSection(id, label, questions, timeLimitMs, intro)` — a timed unit; the JLPT
  simulation uses several.
- `ExamDraft(type, title, sections, deckId, jlpt, weekly, timeLimitMs)` — `questions`
  flattens sections; `sectionOf(questionId)` maps back for per-section analytics.
- `ExamAnswer(questionId, answer, confidence, skipped, responseTimeMs)`.
- `ExamQuestionResult` / `ExamResult` (event model) — persisted in `learning.json`; see
  `docs/architecture/study-engine.md`.

## 5. Generation

### Pool selection (`selectPool(type, deckId, jlpt, includeNew, includeMature, now)`)
- Deck id → real notes in that deck.
- JLPT level → real notes at that band (`jlptNotes`).
- `"mistakes"` → notes behind real `MistakeEngine` entries.
- `"week"` → notes with review events in the last 7 days.
- Grammar pools fall back to **notes** so grammar exams work before grammar cards exist.
- Distractors are **real same-band data**: `distractorMeanings`, `distractorReadings`,
  `distractorExpressions` draw from the actual pool — never invented options.

### Question builders
`cardTypesFor(type)` maps exam type → card types; `generateQuestionFor(note, card, qType)`
builds the concrete question; `generateQuestion(type, entry)` picks the builder by type.
Pool is shuffled with a seeded `Random(nanoTime)`; `questionCount` defaults to 20.

### JLPT simulation (`buildJlptSimulation(jlpt, deckId, now)`)
Three timed sections mirroring the real exam:
1. **文字・語彙 (Vocabulary)** — kanji/vocab recognition + production.
2. **文法 (Grammar)** — pattern selection + sentence completion from real grammar notes.
3. **読解 (Reading)** — cloze from real example sentences that contain the target
   expression.
Each section has its own clock (auto-advance + skip on timeout); per-section scoring;
JLPT-band scope. Sections carry intros.

### Weekly assessment (`generateWeekly`)
Built from what was actually studied this week (review events in the last 7 days, up to
40 notes, 30 questions, 30-minute limit) — a real "weekly assessment", never random.

### `generate(...): ExamDraft?`
Returns `null` when the pool is empty or no questions could be built — the UI shows an
honest "nothing to examine" state, never a broken exam.

## 6. Taking & evaluation

### Taking screen
Section header + per-section progress + intro, per-section countdown, keyboard shortcuts
(1–9 select, R reveal, S skip, Enter next), confidence capture.

### `evaluate(draft, answers, startedAt, now, skippedConfidence): ExamResult`
1. Per-question correctness:
   - Skipped / blank → false.
   - Choice/select → exact match on the selected option(s) (multi-select compares the
     full `|`-separated set).
   - Typed → `normalize()` comparison: lowercase, strips whitespace and Japanese/wide
     punctuation (`・。、，,!！?？()（）[]「」『』`).
2. Builds `ExamResult` (counts, skipped, timing, deck, jlpt, weekly, per-question
   results) and persists via `store.recordExam`.
3. **Feeds the review log**: every answered question grades its card through
   `StudyEngine.grade` with `activityType = Exam` — rating derived from correctness ×
   confidence (correct + confidence ≥ 3 → Easy; correct → Good; else Again); mistakes
   recorded as `"Exam: <question type>"`. Exam answers are real study activity and update
   SRS exactly like normal reviews.

## 7. Persistence

- Suite: `ExamResult`/`ExamQuestionResult` in `learning.json` (full fidelity: prompt,
  answer, options, timing, section, JLPT, confidence).
- Core: `exam` / `exam_question` tables (question index, type, prompt, answer, options
  JSON, user answer, is_correct, time_ms, entity_key, skill, jlpt_level, mistake_category;
  FK cascade) + `learning_mistake` rows for wrong answers + `daily_stats` exam rollups.
- Abandoned exams: `abandonExam` sets status = 2, distinct from completed (1) — abandoned
  exams never pollute score statistics.

## 8. Analytics & smart recommendations

From `StatisticsRepository`: `examHistory`, `examAggregates`, `accuracyByType/Jlpt/
Section/ExamType`, `examTrend`, `studyVsExamGap`. The config screen offers one-click
training: weakest question type, weakest JLPT band, recognition-vs-production gap.
History: score trend, accuracy by exam type, exam log with sections; "Take again"
regenerates the same config (fresh pool shuffle).

## 9. Error model

- Empty pool → `ExamDraft? = null` → honest empty state (§297).
- Corrupt `scope_json`/options → `runCatching` to defaults; the taking screen degrades to
  retry, never a crash (§219).
- Skipped/timeout paths are explicit (`skipped`, auto-advance) — never counted as
  failures nor successes in the wrong bucket.

## 10. Tests

- Suite engine tests cover pool selection, question generation, distractor integrity,
  normalization/evaluation math, and the review-log bridge
  (`desktopApp/src/jvmTest/.../learning/`).
- Gaps: section-timing edge cases (timeout auto-advance), seed reproducibility, JLPT-band
  pool integrity at scale, and migration tests for `exam_question` schema evolution.

## 11. Open items

- Listening question type needs a real audio question pipeline (§287).
- Exam versioning for historical results (§289) — partially covered by storing
  per-question text/options at answer time.
- Core (shipped) exam runner vs suite `ExamEngine` consolidation post-decision
  (audit §7-1).

## 12. Node-layer integration (TARGET — ADR-0013, STANDARDS §287–§289)

### 12.1 Exams as nodes

- `exam` and `question` node types (LEARNING family) with `assesses` edges to the
  language nodes each question tests — so an exam is a typed graph, not an opaque blob.
- `exam_history` (USER family) records results; answers stored with question version +
  exam version for historical interpretability (§289).

### 12.2 Generation from the knowledge layer (§288)

- Question pools derive from: user knowledge states (§84), study history, JLPT level,
  course/deck membership, media exposure, Journey content — via the same evidence
  stream stats use. Never random content disconnected from what the user learned.
- `Weakest-*` exam modes (existing weakest-kanji logic) extend naturally to the
  per-dimension knowledge model: weakest writing, weakest listening, recognition-vs-
  production gap.

### 12.3 Exam as evidence

- `exam_question_answered` events feed knowledge transitions (writing/listening
  dimensions) and stats — exams both *assess* and *train*, consistent with the current
  review-log bridge (§1).

### 12.4 Acceptance criteria

- Every question maps to ≥1 assessed node; pools rebuild deterministically from
  knowledge state + history.
- Historical results remain interpretable after content changes (versioned, §289).
- Exam stats appear under the Exams section of Stats (§131) and drill into per-node
  evidence.
