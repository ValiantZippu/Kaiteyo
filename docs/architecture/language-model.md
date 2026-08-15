# Kaiteyo Architecture — Language Data Model & Knowledge Graph

**Status**: Reference data implemented (two data platforms — see §7 debt) · knowledge
graph partial, target designed (ADR-0013)
**Owner**: `kjd/` standalone platform (ADR-0007) + suite `engine/jdata` + `engine/kana` +
core `app_data`
**Related**: `docs/architecture/DATA_PLATFORM.md` · `docs/data/SOURCES.md` ·
`docs/architecture/decisions/0007-kjd-data-platform.md` · `docs/architecture/database.md` ·
`docs/architecture/decisions/0013-node-architecture.md`

## 1. Purpose

Kaiteyo's language content is **data**, not source code (§326): open-dataset ingestion
pipelines produce the bundled read-only database, and the app builds its learning
experience (kanji, kana, vocabulary, radicals, grammar, example sentences) on top of it.
This document defines the canonical entity model, both data platforms, the kana domain,
search, and the current state of the "knowledge graph".

## 2. Canonical entity model

The suite's `engine/jdata` defines a typed, immutable, provenance-first model
(`CanonicalModels.kt`). `EntityType`: **KANJI, KANA, VOCAB, RADICAL, COMPONENT,
STROKE_SET, READING, SENSE, FREQUENCY, JLPT, RELATION, SOURCE** — every entity is a
typed schema, never generic "thing" with nullable soup (§76–§77).

### `SourceRef(sourceId, recordKey, retrievedAt)`
A reference into a source dataset — never mutated; **provenance is first-class** on
every entity (§185, §347).

### `KanjiEntry`
`id` · `character` · `meanings` · `onReadings`/`kunReadings` · `strokeCount` ·
`radicalId` · `jlpt` · `grade` · `frequencyRank` · `sources`. `hasMeaningfulData`.

### `KanaEntry`
`id` · `character` · `script` (HIRAGANA/KATAKANA) · `reading` · `strokeCount` ·
`sources`.

### `VocabEntry`
`id` · `expression` · `readings: List<ReadingInfo>` (kana, restrictions,
`pitchAccents: List<PitchMarker>`) · `senses: List<VocabSense>` (glosses + language +
POS + field + misc + restrictions + sourceRefs — a **structured sense, never a giant
meaning string**) · `furigana: List<FuriganaSegment>` (structural: `reading == null` for
kana runs) · `frequencies: List<FrequencyValue>` (source-scoped rank/value — never
compared blindly across sources) · `jlpt` · `sources`. Derived: `primaryReading`,
`primaryGloss`, `allGlosses`, `partOfSpeech`.

### `RadicalEntry` / `ComponentEntry`
Radical (character, meaning, stroke count) · component (character, `ComponentKind`:
RADICAL/SEMANTIC/PHONETIC/DECOMPOSITION).

### `StrokeSet` / `StrokeEntry` / `Bounds`
One character's full stroke set: index + SVG path + computed axis-aligned `Bounds`
(minX/minY/maxX/maxY + width/height/center); `strokeOrder` defaults to 0..n when paths
are absent. `StrokeGeometryProvider` interface (kanji from KanjiVG geometry via
`KanjiVgGeometryProvider`; kana from `KanaStrokes`; `NoStrokeGeometryProvider` fallback).

### `RelationEdge(id, fromType, fromId, toType, toId, kind)`
A **directed** typed relationship with a stable ID. `RelationshipGraph.build(data)`
derives edges: kanji↔radical (decomposition), kanji↔vocab (usage),
vocab↔reading/sense, kanji↔component (semantic/phonetic), kana↔base (voiced/yōon
relations). Helpers: `edgesFrom`, `edgesTo`, `neighbours`.

### `PlatformData(schemaVersion, generatedAt, ...)`
The immutable, fully-resolved dataset opened by `LanguageDatabase` — immutable maps keyed
by stable ID; **consumers never see SQLite internals** (§209). `SchemaSql` renders the
SQLite schema; `DatabaseExporter` exports it.

## 3. The two data platforms

| | `kjd/` (standalone, ADR-0007) | suite `engine/jdata` |
|---|---|---|
| Scope | Full ingest platform: adapters (KanjiVG, KANJIDIC, JMdict, JmdictFurigana, Tanos JLPT, Leeds frequency, yomichan-jlpt-vocab), normalization, entity resolution, validation, SQLite export, Kotlin SDK + CLI | Desktop-side pipeline: builds the same canonical model from imported dictionaries + bundled kana/kanji sources (`GenerationPipeline`), serves `LanguageDatabase`, incremental patch apply |
| Output | Bundled `AppDataDatabase` asset (`kanji-dojo-data-base-v15.sql`) | In-memory `PlatformData` + on-disk index |
| Consumers | Core app (all platforms) | Desktop suite engines (search, furigana, strokes, segmenter) |

**Debt**: two jdata implementations evolved separately — consolidate into one pipeline
(ADR-0007 note, `planning/TODO.md` TECHNICAL DEBT, audit §7-1). Both keep provenance.

### Generation pipeline (suite, `GenerationPipeline`)
`run(repository, config)` → `PipelineResult`: ingest → normalize (`Normalizer`) →
entity resolution (dedupe via `DedupResolver.canonicalKey` + `resolve`; source conflicts
via `SourceConflictPolicy` with priority + provenance labels) → validate (`DataValidator`
with `IssueSeverity { FATAL, RECOVERABLE, WARNING, UNSUPPORTED, MISSING_OPTIONAL,
UNRESOLVED }` — surrogate checks, entity index integrity, `ValidationReport.isFatal`)
→ graph build → export. Produces `QualityReport` (markdown) + `ReleaseManifest` (JSON).

### Search (`engine/jdata/search/SearchSystem.kt`)
- `SearchIndex` built once (kanji/kana/reading/meaning-token keys → `HitRef`s).
- `search(query, filters)` with `Exactness { EXACT, PREFIX, READING, KANA, MEANING }`;
  `autocomplete(prefix, limit)`; `SearchFilters` (kind, JLPT, grade, tag).
- `RankingStrategy` interface: `DefaultRankingStrategy` (exactness tiers) and
  `FrequencyRankingStrategy` (frequency-boosted) — pluggable, `RankingContext` carries
  the data for scoring.

### Furigana (`FuriganaEngine.parse`)
Best-effort structural furigana for arbitrary text: kana-run matching with a readings
lookup, feasibility pruning, kana-range detection — powers text analysis and card
rendering without external services.

### Stroke validation (`StrokeSystem` + `StrokeSequenceValidator`)
`validateCount` (declared vs actual, per `WritingStrictness { Relaxed, Normal, Exam }`),
`validateOrder`, `validateSequence` (against a `StrokeSet` + `WritingEvaluationConfig`) —
the evaluator's ground truth for writing practice and exams.

## 4. Kana as a first-class domain (`engine/kana`)

- `KanaData.kt` — full syllabary as data: `KanaChar(character, romanization, script,
  category, base, small, strokeCount, meaning, tags)` with **explicit relationships**
  (が → base か + dakuten; きゃ → base き + small ゃ; `voicedBase()` maps voiced→base).
  `KanaCategory`: Base (46 hiragana / 46 katakana), Dakuten, Handakuten, YoOn, Extended.
  Unicode codepoint strings (`U+3042`, `U+304D U+3083`); `deckTags` drive premade-deck
  membership; `isWritable` (single char) gates writing support.
- `KanaStrokes.kt` — canonical stroke polylines for the writable syllabary on the same
  0..100 grid as the built-in evaluator; `kanaStrokesFor`, `kanaWritingSupported`,
  `kanaReferenceStrokeCount`.
- `KanaCatalog.kt` — canonical kana notes + premade decks (Hiragana, Katakana, Dakuten,
  Extended Katakana, Full Kana) seeded idempotently (`kanaCardId = "kana-$character"`,
  `primaryKanaDeckId` by category, `buildKanaCards`, `seedKanaInto`).
- Kana flows through the unified learning model as `LearningItemKind.Kana`; voiced kana
  evaluate against their base shape in the writing engine (same stroke infrastructure as
  kanji, §286).

## 5. Knowledge graph — current state

| Capability | Status | Where |
|---|---|---|
| Kanji ↔ radical decomposition | ✅ real | `kanji_radical` + `RelationshipGraph` + radical browser |
| Kanji ↔ vocabulary examples | ✅ real | `letter_vocab_example` |
| Vocab sense cross-references | ✅ real | `vocab_sense_cross_reference` + jdata `RelationEdge` |
| Kanji ↔ readings/meanings/frequency/JLPT/grade | ✅ real | `kanji_*` tables + `KanjiEntry` |
| Vocab ↔ sentence (Tatoeba) | ✅ real | `vocab_sense_example` + `sentence` |
| Text → furigana | ✅ real | `vocab_furigana` + `FuriganaEngine` |
| Kana ↔ base/voiced/yōon | ✅ real | `KanaChar.base/small` + jdata kana relations |
| **User knowledge graph** | 🟡 partial | `KnowledgeProfileEngine` — per-dimension coverage + measured accuracy + approximate JLPT + frequency bands from real events (a *profile*, not a full entity graph) |
| Grammar/pitch edges | ⬜ planned | licensing RESEARCH |
| Journey ↔ knowledge bridge | 🟡 specified | target: ADR-0013/0014 — typed edges (`represents`, `encountered_by`, `discovered_by`, `mined_from`, `appears_in_media`, `teaches`) bridge World and Language graphs; not implemented |

The full graph is **future work with a written target design**: ADR-0013 +
`docs/architecture/NODE_ARCHITECTURE.md` + `nodes/NODE_TYPE_REGISTRY.md` /
`RELATIONSHIP_REGISTRY.md` / `KNOWLEDGE_STATE_MODEL.md`, gated on the §157 build order
(`docs/planning/TODO.md`). It must be domain logic (§177) fed by events (§210–§213),
never derived from UI state. The jdata `RelationEdge` model is the existing precedent for
typed edges.

## 6. Rules that apply here

- External data ≠ official user mastery (§291); estimates labeled approximate (§290).
- Never hardcode language data in UI source (§326).
- Normalization policy (§282): keep exact representations where language data depends on
  them; document NFKC/NFD decisions before applying.
- Japanese text is first-class (§281): hiragana/katakana/kanji, iteration marks,
  full/half-width, combining marks, surrogate pairs.
- Dataset licensing verified before ingestion (§183–§185): `docs/data/SOURCES.md` +
  `docs/legal/`.

## 7. Open items

- **Consolidate the two jdata implementations** (`kjd/` vs `engine/jdata`) into one
  pipeline (TECHNICAL DEBT, audit §7-1).
- Grammar, pitch-accent, example-sentence datasets behind the pipeline (licensing
  RESEARCH).
- FTS/trigram indexing at production scale (§186–§187).
- Node/knowledge-graph implementation per §157 (ADR-0013).
- `demoKanji` seed provenance confirmation in `docs/data/SOURCES.md`.

## 8. Canonical entities → node registry mapping (TARGET — ADR-0013)

The canonical entity model maps directly onto the node registry (so no new data model is
invented for the graph layer):

| `engine/jdata` entity | Node type (`NODE_TYPE_REGISTRY.md` §1) | Notes |
|---|---|---|
| KANJI | `kanji` | + `contains_component`, `uses_radical`, `has_reading`, `has_meaning`, `has_frequency`, `has_jlpt`, `has_grade` |
| KANA | `kana` | + `has_reading`, `appears_in` |
| VOCAB | `vocabulary` | + `contains_character`, `conjugates_to`, `appears_in_sentence` |
| RADICAL / COMPONENT | `radical`, `component` | dataset-gated (RESEARCH) |
| STROKE_SET | stroke data on `kanji`/`kana` | STANDARDS §284–§285 (not a node; payload) |
| READING / SENSE | `reading`, `meaning` | children of kanji/vocab |
| FREQUENCY / JLPT | `frequency_entry`, `has_frequency`/`has_jlpt` edges | derived from datasets |
| RELATION | `edge` (typed per registry) | the `RelationEdge` precedent generalizes |
| SOURCE | `source` / `dataset` (SYSTEM family) | provenance + attribution (STANDARDS §185) |

### 8.1 Knowledge graph target (recap for implementers)

The graph layer adds: `node`/`edge` stores (NODE_DATA_MODEL §2), `user_knowledge`
(KNOWLEDGE_STATE_MODEL), the event ledger (EVENT_CATALOG), and the §149 world bridge
(`represents`, `encountered_by`, `discovered_by`, `mined_from`, `appears_in_media`). It
is domain logic (§177) fed by events (§210–§213), gated on the §157 build order
(`docs/planning/TODO.md`). The jdata `RelationEdge` model remains the precedent for
typed edges; the registries are the vocabulary contract.

### 8.2 Acceptance criteria

- Every canonical entity type maps to exactly one registry node type (table above).
- No new entity type may be added to jdata without a matching registry row (§77).
- Grammar/pitch/sentence nodes stay TARGET until their datasets clear licensing
  RESEARCH (NODE_TYPE_REGISTRY §9 status).
