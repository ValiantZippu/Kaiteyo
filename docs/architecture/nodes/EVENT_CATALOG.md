# Event Catalog

**Status**: TARGET — the catalog for the node-layer `event_log` (ADR-0013,
[`NODE_DATA_MODEL.md`](NODE_DATA_MODEL.md) §5). Nothing here implies an event exists
until its row says so. **Today's real events** (persisted, verified in
[`docs/architecture/statistics.md`](../statistics.md)) are marked `CURRENT`; the rest are
the target catalog.

Every event carries: `event_id`, `user_id`, `occurred_at`, `event_type`, `source`,
`payload` (semantic facts only — never UI state or credentials), `schema_version`,
`session_id` (STANDARDS §211).

## Family: STUDY

| Event | Payload (key fields) | Status |
|---|---|---|
| `study_started` / `study_ended` | session id, deck, mode, interrupted | CURRENT (`StudySessionRecord`) |
| `card_reviewed` | card/note id, card type, rating, statusBefore/After, intervalBefore/After, lapsesAfter, wasNew, response time, mistakes | CURRENT (`LearningReviewEvent` → `review_history`) |
| `card_suspended` / `card_buried` / `card_reset` | card id, reason | CURRENT (state changes logged) |
| `writing_attempted` | character, per-stroke `StrokeAttempt`s, accuracy, mistake count, duration | CURRENT (`WritingAttemptEvent` → `writing_attempt`) |
| `mistake_recorded` | entity key, mistake category, context | CURRENT (`learning_mistake`) |

## Family: CONTENT / DICTIONARY

| Event | Payload | Status |
|---|---|---|
| `kanji_encountered` | node id, surface, source surface | TARGET |
| `vocabulary_encountered` | node id, headword, reading, source surface | TARGET |
| `dictionary_lookup` | headword, dictionary id, mode | TARGET (history exists in suite `history.json`) |
| `text_analyzed` | text, tokens, analyzed flags | TARGET |

## Family: MEDIA

| Event | Payload | Status |
|---|---|---|
| `media_started` / `media_ended` | media id, file, position, duration | CURRENT (watch history) |
| `subtitle_selected` | media id, subtitle line/offset, text | TARGET |
| `card_mined` | mining source, headword, media ref (file + timestamp), deck | CURRENT (`MiningEngine` activity log) |
| `bookmark_added` / `audio_clip_saved` | media id, timestamp, note | CURRENT (`MediaBookmark` / `AudioClip`) |

## Family: EXAM

| Event | Payload | Status |
|---|---|---|
| `exam_started` / `exam_completed` / `exam_abandoned` | exam id, type, scope, score, accuracy, total time, sections | CURRENT (`exam` + `ExamResult`; abandon distinct from complete) |
| `exam_question_answered` | exam id, question id, type, correct, timeMs, skill, JLPT band, mistake category | CURRENT (`exam_question`) |

## Family: JOURNEY (target world)

| Event | Payload | Status |
|---|---|---|
| `quest_started` / `quest_completed` | quest id, world id, cell | TARGET |
| `location_discovered` | location node id, world id, cell | TARGET |
| `photo_taken` | photo id, location, linked nodes | TARGET |
| `npc_dialogue` | npc id, dialogue node id, choice | TARGET |
| `world_entered` / `world_exited` | world id, save version | TARGET |

## Family: SYSTEM / SYNC

| Event | Payload | Status |
|---|---|---|
| `sync_started` / `sync_completed` / `sync_conflict` | provider, counts, conflict ids | CURRENT (sync log) |
| `backup_created` / `backup_restored` / `backup_verified` | backup id, checksum, size | CURRENT (`backup_metadata`) |
| `import_finished` | format, counts, conflicts | CURRENT (import log) |
| `settings_changed` | key, source | TARGET (policy-dependent; do not log sensitive values) |

## Rules

1. Payloads carry semantic facts only — never UI state, layout, or private credentials
   (STANDARDS §211, §220).
2. The log is append-only; corrections create new rows, never edits.
3. Derived metrics (heatmap, knowledge scores, exam stats) read from the log and are
   re-runnable — the log is the single source of truth (§213).
4. Adding a catalog entry is a registry + validation change (ADR-0013), gated by the
   content/authoring pipeline where relevant.
