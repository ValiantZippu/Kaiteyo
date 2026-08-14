# Japanese Language Data Platform

A reusable, source-agnostic Japanese language data layer inside the desktop
module: `desktopApp/src/jvmMain/kotlin/ua/syt0r/kanji/desktop/engine/jdata/`.
It is **not** a Kaiteyo-internal database: the API is Compose-free, AppState-free
and usable by any JVM application.

## Layering

```
DictionaryRepository / DictionaryImporter   (Yomitan/JMdict/KANJIDIC parsing)
        │  (source layer — raw data never mutated)
        ▼
PlatformBuilder  ──►  PlatformData  (canonical, immutable, stable IDs)
        │
        ├── LanguageDatabase  (public API: lookup/search/strokes/provenance…)
        ├── SearchIndex       (inverted index + replaceable ranking + autocomplete)
        ├── DataValidator     (categorized issues; fatal fails the build)
        └── GenerationPipeline (deterministic build → quality report + release manifest)
```

| Package | Role |
|---|---|
| `jdata.model` | `PlatformData`, canonical entities, `StableIds` |
| `jdata.normalize` | Non-destructive Unicode/kana normalization |
| `jdata.engine` | Kanji/radical/component, kana, vocab/sense/reading, furigana, frequency/JLPT, strokes, relationship graph |
| `jdata.search` | Search index, `RankingStrategy`, `SearchFilters`, autocomplete |
| `jdata.validate` | `DataValidator`, `DedupResolver`, `SourceConflictPolicy` |
| `jdata.source` | Source adapter framework + reference adapters |
| `jdata.schema` | Canonical SQLite DDL emission + migrations |
| `jdata.pipeline` | `GenerationPipeline`, `QualityReport`, `ReleaseManifest` |
| `jdata.profiles` | Minimal / Standard / Full dataset profiles |
| `jdata.extensions` | Extension namespaces (pitch, grammar, examples, frequency) |
| `jdata.io` | JSON / CSV / Markdown exporters |
| `jdata.api` | `LanguageDatabase` — the public SDK surface |
| `jdata.integration` | `PlatformBuilder` (dictionary → canonical) |

## Stable IDs

IDs are deterministic, never random:

- kanji / kana / radical / component / stroke-set IDs embed the character
  (`k:日`, `r:日`, `s:食`).
- vocabulary IDs hash the `(expression, reading)` pair, so a regenerated
  database yields the same ID for the same word while different readings stay
  distinct.
- reading/sense/frequency/relation IDs derive from their owner + content.

This is what lets Kaiteyo cards, mining records and third-party apps reference
language entries that survive database updates.

## Provenance & licenses

Every entity carries `SourceRef`s (source ID + record key). Source definitions
carry version, homepage, license name/URL and retrieval date. `kjd attribution`
writes `THIRD_PARTY_DATA.md` / `THIRD_PARTY_DATA.json`; licenses are **not
invented** — an undeclared license is reported as "Not declared by source".

## Search

- Exact → prefix → reading → kana-fold → meaning match, scored by a replaceable
  `RankingStrategy` (default boosts frequency and source priority).
- No full scans: the `SearchIndex` probes posting lists; meaning search uses a
  gloss token index.
- `SearchFilters`: entity types, JLPT band, max frequency rank, reading
  substring, source IDs, part of speech.
- `autocomplete(prefix)` ranks suggestions from the same index.

## Strokes & writing

`StrokeSystem` provides `StrokeSet` (index, SVG path, computed bounds),
`SvgPathBounds` (numeric parser for M/L/H/V/C/S/Q/A/Z), and
`StrokeSequenceValidator` with explicit `WritingEvaluationConfig`
(relaxed/normal/exam, order sensitivity, tolerances).

### KanjiVG geometry (`KanjiVgGeometryProvider`)

A real, working `StrokeGeometryProvider` that reads a KanjiVG dataset
directory (`https://kanjivg.tagaini.net/`, CC BY-SA 3.0 — credited as a source,
never claimed):

- Resolves characters from hex codepoint (`kanji/4e00.svg`), literal
  (`kanji/食.svg`) or kana layouts, plus the aggregate `kanjivg.xml` document
  (parsed exactly once).
- Extracts per-stroke SVG `d` paths in stroke order (via the `number`
  attribute or `-sN` path-id suffix), skipping the stroke-number glyphs.
- Parsing is hardened: DOCTYPE disallowed, external entities off, secure
  processing on. Results are cached per character.

The dataset is not bundled; `kjd build --vg <dir>` / `kjd strokes --vg <dir>`
attach it at build time, and stroke sets are then attributed to the
`kanjivg` source in provenance.

### Writing-evaluation bridge (`StrokeEvaluationBridge`)

Connects canonical stroke data to the app's existing evaluators
(`ua.syt0r.kanji.core.stroke_evaluator.*`):

- `SvgPathConverter` converts SVG path data (M/L/H/V/C/S/Q/T/A/Z, absolute
  and relative, with proper S/T control-point reflection) into Compose
  `Path` objects — the only place the platform touches Compose.
- `StrokeEvaluationBridge.evaluate(set, drawnPaths, strictness)` runs the
  structural checks first (count/order via `StrokeSequenceValidator`), then
  the greedy `StrokeSequenceEvaluator` for per-stroke scores, wrong-order /
  missing / extra detection and overall accuracy.
- Platform strictness maps to the core scoring presets: Relaxed → Normal,
  Normal → Hard, Exam → Exam; acceptance thresholds are explicit
  (0.5 / 0.65 / 0.8). The evaluator implementation is injectable, so the
  `AltKanjiStrokeEvaluator` preference stays usable through the bridge.
- At Exam strictness the platform itself rejects stroke-order deviations: the
  core sequence evaluator does not penalize correct strokes drawn in the wrong
  order, so the bridge enforces that rule on top of the geometric score.

### Writing sessions (`jdata.writing.KanjiWritingSession`)

A real consumer of the platform demonstrating the full integration path for
any writing UI (Kaiteyo desktop, a standalone trainer, a third-party app):
`LanguageDatabase.getStrokeData(character)` → `WritingSession` (stable-ID
lookup of the canonical `StrokeSet`) → `submit(drawnPaths, strictness)` →
`WritingAttempt` with typed per-stroke scores, sequence issues and a
verdict. `liveStroke(...)` gives per-stroke feedback while drawing, and
`summaryLines()` renders everything for a CLI or log. UI-free by design.

### Desktop tests (`desktopApp/src/jvmTest/.../jdata/`)

The platform has a real desktop test suite (JVM, `kotlin.test`):

- `SvgPathConverterTest` — every SVG command, relative cursor math, S/T
  reflection, multi-subpath `Z` (regression for an infinite-loop bug),
  malformed input.
- `KanjiVgGeometryProviderTest` — hex/literal/kana layouts, aggregate
  document, ordering by `number` attribute and `-sN` suffix, number-glyph
  skip, malformed XML, negative caching, invalid root.
- `StrokeEvaluationBridgeTest` — identical / reversed / count-mismatch /
  empty attempts, strictness→config and threshold mapping, single-stroke
  scoring, validator count/order/index rules.
- `KanjiWritingSessionTest` — integration: PlatformData → LanguageDatabase →
  session → attempt, including the Exam wrong-order rejection.

## Generation pipeline

`kjd build` runs: validate sources → parse → normalize → resolve
(deduplicate by canonical identity, preserve source conflicts) → construct
relationships → build index → validate generated data → provenance/attribution
→ `quality-report.md` → `release-manifest.json`. Generation is deterministic
(sorted iteration, no randomness).

## CLI

`kjd` (engine/cli/KjdCli.kt) — every command reads real on-disk data:

```
info  sources  search  lookup  strokes  radical  validate  stats  attribution
import <file>  build [--profile]  export <json|csv|md>
kanji <char>   vocab <word>   autocomplete <prefix>
writing-check <kanji> <svg-path>... [--strictness relaxed|normal|exam] [--vg <dir>] [--strokes <file>]
```

`kjd writing-check` feeds drawn strokes (SVG path data as arguments or one
per line in `--strokes <file>`) through `KanjiWritingSession` and prints
per-stroke scores, sequence issues and a PASS/FAIL verdict — 0 exit code on
acceptance. Without `--vg` it runs the structural (count) check and says so.

## Extension model

`ExtensionRegistry` namespaces optional datasets (pitchAccent, grammar,
examples, frequencySources). The core schema stays stable; extensions attach
under namespaces and declare the `DataPart`s they provide, so consumers can
detect what an installation actually contains.
