# Kaiteyo Architecture — Content System & Authoring

**Status**: Foundation exists (kjd content + premade decks + kana catalog are data);
authoring pipeline specified (ADR-0015), editor tool is FUTURE
**Owner**: kjd platform (content pipeline) · suite `engine/` (content consumers)
**Related**: `docs/architecture/language-model.md` · `docs/architecture/journey.md` ·
`docs/architecture/database.md` · `docs/architecture/nodes/CONTENT_AUTHORING.md`
(pipeline/validation spec, ADR-0015) · `docs/architecture/nodes/NODE_TYPE_REGISTRY.md`
(the node vocabulary content validates against) ·
`docs/architecture/decisions/0015-content-authoring.md`

## 1. Purpose

Content is data, not code (§326, §257): courses, lessons, quests, stories, dialogue,
dictionary supplements, Journey content and exams must be authorable through **structured
content formats with schemas and automatic validation** — never source-code edits per
lesson (§257). Content never executes code (§361). Every package identifies creator,
license, source, attribution, version (§260).

## 2. What exists today

| Content | Source | Validation |
|---|---|---|
| Dictionary/reference data | `kjd/` pipeline (KanjiVG, KANJIDIC, JMdict, JmdictFurigana, Tanos JLPT, Leeds frequency, yomichan-jlpt-vocab) → `AppDataDatabase` asset | `DataValidator` (jdata) + dataset provenance (§185); licenses in `docs/data/SOURCES.md` + `docs/legal/` |
| Premade JLPT/grade decks | reference data (`vocab_deck_card`); onboarding generates real JLPT kanji/vocab decks from the bundled DB | generated from data, never hardcoded (§326) |
| Kana catalog + decks | `KanaCatalog` (canonical kana notes + Hiragana/Katakana/Dakuten/Extended/Full decks), seeded idempotently on first run | stable dedupe keys (`kana-$character`); custom notes protected |
| User content | import/export (APKG, JSON/CSV/TSV/TXT) with conflict policies | `ImportPipeline` validation + preview (§358) |

## 3. Authoring pipeline (specified, ADR-0015)

`docs/architecture/nodes/CONTENT_AUTHORING.md` defines the target pipeline:

- **Content is data**: worlds, quests, dialogue, lessons, objects, language content are
  authored as structured, schema-validated files (JSON/YAML source with JSON Schema,
  compiled to SQLite packages for distribution — final syntax resolved during
  implementation).
- **Hard validation gates** (§148): schema, relationship, asset, localization, license,
  performance. Invalid content cannot publish; packages re-verify at install (manifest
  hash, dependency versions, min engine version).
- **Package format**: `packageId`, `version`, `minEngineVersion`, `dependencies`,
  `contentHash`, license/creator/attribution (STANDARDS §259–§260); no executable code
  (§361).
- **Node editor is FUTURE** (§147): a tool that authors and previews the same content the
  pipeline accepts. Until then, content is authored against the schemas in
  `CONTENT_AUTHORING.md` and the registries.
- **Dictionary/language data** continues through the kjd pipeline (ADR-0007); this ADR
  governs *authored* content (learning + world), sharing its validation philosophy.

## 4. Content packages (§259)

```
Kaiteyo Content Package
├── manifest        (packageId, version, minEngineVersion, dependencies, contentHash)
├── metadata        (title, creator, license, source, attribution)
├── nodes           (typed content per NODE_TYPE_REGISTRY / JOURNEY_WORLD_SCHEMA)
├── assets          (images, audio, fonts, 3D — referenced, not embedded)
└── localization    (per-locale strings for the content, distinct from app UI)
```

Packages are versioned, attributed, safe (no code execution — ADR-0015 + ADR-0011
security-first). Journey world content is the first big consumer
(`JOURNEY_WORLD_SCHEMA.md`).

## 5. Content vs app localization (§256)

Content localization is separate from app UI localization:
- App strings: `Strings` interface (EN/JP) — `docs/architecture/localization.md`.
- Content: learning content is Japanese *by design*; gloss language is data
  (`VocabSense.language`, `vocab_sense_gloss.language`). Content packages carry their own
  localization section.

## 6. Validation & security

- Content packages are untrusted input (§358): validate schemas, licenses, and asset
  references before load.
- Journey content never executes arbitrary code (§361).
- Duplicate/source data never overwrites user notes (custom-note protection in the
  learning store).
- Marketplace/plugin content follows the same untrusted-input discipline
  (`docs/architecture/integrations.md` §7).

## 7. Tests

- Existing: kjd dataset validation tests, import pipeline validation tests.
- Planned: content package schema validation tests, hard-gate enforcement tests, install
  re-verification (manifest hash, dependencies).

## 8. Open items

- Content package format spec + validator tooling (kjd-adjacent).
- Node editor tooling (§147) — FUTURE.
- Journey node/quest schema (with `docs/architecture/journey.md` §4) — under ADR-0013/0014.
- Grammar/pitch/example-sentence datasets (licensing RESEARCH) as additional content
  sources.
