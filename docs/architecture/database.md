# Kaiteyo Architecture — Database Specification & Migration Plan

**Status**: Implemented (shipped in the product) · schema frozen by ADR-0005
**Owner**: core data layer (`ua.syt0r.kanji.core.*`)
**Related**: `docs/architecture/OVERVIEW.md` · `docs/architecture/DATA_PLATFORM.md` ·
`docs/architecture/decisions/0005-sqldelight-two-databases.md` · `docs/data/ARCHITECTURE.md`

## 1. Purpose

Offline-first relational storage for all persistent application state. Per ADR-0005 the
app uses **two SQLDelight databases** plus DataStore for lightweight preferences and JSON
files for the desktop suite's local state. Every schema change is versioned; users are
never told to "delete the database" (§179–§181).

## 2. Storage owners (source of truth)

| Store | Owner | Contents |
|---|---|---|
| `AppDataDatabase` | `core/.../app_data/` | Bundled read-only dictionary/reference data (asset) |
| `UserDataDatabase` | `core/.../user_data/` | Mutable user data: decks, cards, SRS state, review history, tags, flags, notes, study history, shortcuts, backups, exams, stats rollups |
| DataStore preferences | `core/.../preferences` | `PreferencesContract` — settings/theme/onboarding state |
| `~/.kaiteyo/` JSON | desktop suite (`AppState`, engines) | Suite-local state: `cards.json`, `settings.json`, `library.json`, `learning/learning.json`, dictionary indexes/imports, `history.json`, `favorites.json`, OCR output, backups |
| KJD SQLite export | `kjd/` build output | Generated `kanji-dojo-data-base-v15.sql` asset (ingested into `AppDataDatabase`) |

## 3. AppDataDatabase (bundled, read-only)

Schema source: `core/src/commonMain/sqldelight_app_data/.../Letters.sq` + `Vocab.sq`.
Loaded once at startup (`KaiteyoDataCenter.ensureLoaded`). Bundled as the
`kanji-dojo-data-base-v15.sql` asset (AppDataDatabaseVersion = 15, declared in
`buildSrc/AppAssets.kt`).

**Kanji domain** — `character_stroke` (character, stroke_number, stroke_path),
`radical`, `kanji_data` (frequency, variant family), `kanji_reading` (on/kun, etc.),
`kanji_meaning` (prioritized), `kanji_classification` (JLPT/grade classes),
`kanji_radical` (positional radical decomposition with start stroke + stroke count),
`letter_vocab_example` (letter → example vocabulary).

**Vocabulary domain** (normalized JMdict-style, with foreign keys + `ON DELETE CASCADE`)
— `vocab_entry`, `vocab_kanji_element` (+ information/priority),
`vocab_kana_element` (+ restriction/information/priority), `vocab_sense` (+
kanji/kana restriction, part_of_speech, cross-reference, antonym, field, miscellaneous,
dialect, gloss, information, example), `sentence` (Tatoeba: sentence, translation, score,
furigana), `vocab_entity` (part-of-speech explanations), `vocab_furigana`,
`vocab_deck_card` (pre-baked JLPT/grade vocab deck cards).

**Search**: reading-based queries use `LIKE '%'||:text||'%'` with `GROUP BY entry_id`
over kanji/kana elements plus prioritized ordering — functional for the bundled scale but
brute-force; the FTS/trigram indexing work (§186–§187) is a future performance item.

## 4. UserDataDatabase (mutable, versioned)

Schema source: `core/src/commonMain/sqldelight_user_data/.../UserData.sq`,
`UserData_enhancements.sq`, `UserData_statistics.sq`. Current `PRAGMA user_version = 16`.
Migrations live in `core/src/commonMain/sqldelight_user_data/migrations/`.

**Decks & entries** — `letter_deck` / `letter_deck_entry` (kanji decks, `is_archived`
supported), `vocab_deck` / `vocab_deck_entry` (word decks with kanji/kana/meaning/word_id).
Position-ordered; archive flag persisted (the archive *filter/restore UI* is open — see
`planning/TODO.md` P1).

**SRS** — `fsrs_card` (`key` + `practice_type` composite PK; status, stability,
difficulty, lapses, repeats, last_review, interval). FSRS-5 algorithm (ADR-0006).

**Review history** — `review_history` (`key`, `practice_type`, timestamp, duration,
grade, mistakes, interval, deck_id). Append-only per review; queries aggregate by day,
grade, interval bucket, streak, per-item stats, first/last review.

**Enhancements** — `tag` (+ `card_tag`), `card_flag`, `card_note`, `study_history`
(audit log), `keyboard_shortcut` (rebindable, profile-scoped, conflict detection),
`backup_metadata` (filename, size, checksum, automatic, notes), `filtered_deck` (custom
study), `plugin_registry` (future-proof plugin metadata).

**Statistics & exams** — `study_session`, `writing_attempt` (per-stroke accuracy,
mistakes, wrong-order), `exam` / `exam_question` (question text, answer, options JSON,
user answer, timing, skill, JLPT level, mistake category), `learning_mistake`
(structured mistake log), and **`daily_stats`** — precomputed per-day rollups
(reviews/new/review/correct/incorrect/lapses/study_time/writing/exams/sessions) updated
incrementally so heatmaps and the today panel never scan the full history.

## 5. Migration policy

1. Every schema change is a **versioned migration** in
   `commonMain/sqldelight_user_data/migrations/` (`.sqm` files applied by SQLDelight,
   `user_version` bumped in `UserData.sq`). Never change tables in place.
2. New tables/queries are declared in `.sq` files **with `IF NOT EXISTS`** so the same
   DDL is safe from both a fresh schema and the migration path (`UserData_statistics.sq`
   documents this pattern).
3. `ALTER TABLE` goes in migration code, never in `.sq` files.
4. Regenerate interfaces after changing schemas:
   `gradlew :core:generateCommonMainAppDataDatabaseInterface` /
   `:core:generateCommonMainUserDataDatabaseInterface`.
5. Imported/parsed data is validated before insert (§181); decks and cards merged with
   conflict policies via `mergeImportedCards()`.
6. Desktop suite JSON files have their own light migration story (e.g. settings
   key-vocabulary migration in `SettingsEngine`); the data-layer consolidation decision
   (audit §7-1) determines their long-term fate.

## 6. Integrity & performance

- Foreign keys + `ON DELETE CASCADE` throughout the vocab schema; composite primary keys
  enforce uniqueness (e.g. `fsrs_card(key, practice_type)`, `review_history` append
  PKs, `daily_stats(date)`).
- Indexes exist on hot paths (`tag_parent_idx`, `tag_name_idx`, `card_tag_tag_idx`,
  `study_history_*`, `writing_attempt_*`, `exam_*`, `learning_mistake_*`).
- Slow-query investigation uses `EXPLAIN QUERY PLAN` (§192); indexes follow real access
  patterns, not blind coverage.
- `getTotalReviewsDuration` and the day aggregations clamp pathological durations
  (e.g. > 120 s per review) to keep study-time totals sane.

## 7. Tests

- Core tests: `:core:allTests` (SRS, statistics math, transfer/import).
- Desktop suite: `desktopApp/src/jvmTest/...` — e.g. `StatisticsRepositoryTest`,
  `GoalsEngineTest`, `ActivityTrackerTest`, `MediaEngineTickSafetyTest`.
- Migration/constraint/import/export DB tests are a stated gap (`planning/TODO.md`,
  §215–§217) — add as the migration surface grows.

## 8. Open items

- FTS/trigram search indexing for dictionary and vocab lookup (§186).
- Archived-deck filter + restore UI (data exists; UI missing).
- `daily_stats` correctness across timezone changes (day boundaries are local-time).
- Consolidation of suite JSON storage onto `UserDataDatabase` (audit §7-1).

## 9. Node-layer integration (TARGET — ADR-0013, NODE §78–§80, §84)

The node layer is **additive** to the two databases above — it never alters existing
schemas (AGENTS.md never-change list). Target contract: `docs/architecture/nodes/NODE_DATA_MODEL.md`.

### 9.1 What gets added

| Store | New tables (target) | Purpose |
|---|---|---|
| `UserDataDatabase` (or companion per ADR-0013) | `node`, `edge` | knowledge graph: identities + typed relationships |
| `UserDataDatabase` | `user_knowledge`, `knowledge_transition` | per-dimension knowledge state + evidence ledger (§84) |
| `UserDataDatabase` | `knowledge_score` | derived score cache (rebuildable, never truth) |
| `UserDataDatabase` | `event_log` | append-only evidence ledger (STANDARDS §210–§211) |
| Files (world packages) | versioned SQLite content DBs | immutable world content (§145, ADR-0015) |
| Files (save) | versioned JSON/SQLite per world | sparse player/world state (§144) |

### 9.2 Constraints that must hold

- **Provenance**: every `node`/`edge` row carries `source`/`source_id`; `UNIQUE
  (source, source_id)` makes imports idempotent (NODE §78).
- **Separation**: knowledge state lives in `user_knowledge`, never on `node.status`
  (lifecycle only) and never on FSRS card rows (§84).
- **Append-only**: `knowledge_transition` and `event_log` are never updated in place;
  derived caches (`knowledge_score`, `daily_stats`) are rebuildable.
- **Migration discipline**: new tables start at the next free `.sqm` migration number;
  `schema_version` on rows handles payload evolution without table churn (STANDARDS
  §180).

### 9.3 Backup & sync scope (STANDARDS §205–§206, §271)

- Backups include: node/edge/knowledge/event data + world saves + photos (user-selected)
  — in addition to the existing decks/cards/history.
- Sync is semantic-object based: nodes, edges, knowledge, saves, collections — never
  transient UI state.
- Restore must be transactional: a partial restore (e.g. node layer restored but events
  truncated) must be detected and rejected, or repaired, never left silently inconsistent.

### 9.4 Acceptance criteria (see TEST_PLAN §3–§4)

- No existing table is modified by the node layer.
- All §NODE_DATA_MODEL 8 query patterns resolve within budget at full scale.
- Deleting a user removes user-owned nodes/edges/knowledge/events; app-data and world
  content are untouched (STANDARDS §205).
