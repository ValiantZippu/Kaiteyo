# KJD — Kaiteyo Japanese Data Platform

A standalone, reusable Japanese language data platform. It ingests openly
licensed datasets, normalizes them into one canonical model, and produces a
portable SQLite database plus a typed Kotlin API and a developer CLI.

KJD is **architecturally independent of Kaiteyo**. Kaiteyo (and any other
application) consumes the generated database and the public API. There is no
runtime dependency on KanjiVG, KANJIDIC, JMdict, Yomitan, Anki or any other
upstream project — those are *source inputs* to the generator, not runtime
dependencies.

```
External datasets
    ↓ source adapters (this module)
Raw source store (sources/<id>/raw/)
    ↓ parsers
Normalization (Unicode, kana, punctuation)
    ↓ entity resolution + cross-source linking
Canonical Japanese knowledge model
    ↓ validation
SQLite database + indexes
    ↓
Public KJD API
    ├── Kotlin SDK (JapaneseDatabase / JapaneseDataApi)
    ├── CLI (kjd)
    ├── SQLite database distribution
    └── export artifacts (JSON / JSONL / CSV)
```

## Module layout

| Path | Purpose |
|---|---|
| `src/main/kotlin/io/kaiteyo/kjd/model/` | Canonical entities (kanji, kana, vocab, senses, strokes, radicals, relationships, provenance) |
| `.../source/` | First-class source metadata, licenses, attribution manifests |
| `.../parser/` | Source adapters: KanjiVG, KANJIDIC, JMdict, JmdictFurigana, Tanos JLPT, Leeds frequency, yomichan-jlpt-vocab |
| `.../normalize/` | Japanese-aware normalization (kana equivalence, NFC, punctuation) |
| `.../resolve/` | Entity resolution, deduplication, cross-source linking |
| `.../validate/` | Integrity validation + data-quality reporting |
| `.../db/` | SQLite schema + deterministic database writer |
| `.../search/` | Indexed search with Japanese-aware ranking |
| `.../api/` | **Public API** — the stable consumer boundary |
| `.../export/` | JSON / JSONL / CSV exporters |
| `.../pipeline/` | Full generation pipeline (build) |
| `.../cli/` | Developer CLI |

## Using the platform

### 1. Obtain or build a database

Prebuilt databases are distributed as `kjd-japanese-<version>.db`. To build
one from raw sources yourself:

```bash
# Place raw sources under sources/<id>/raw/ per source
kjd build --sources-dir ./data --out kjd-japanese.db --report-dir ./report
```

The layout is:

```
data/
  sources/
    kanjivg/raw/            # extracted KanjiVG SVG files (or a zip)
    kanjidic/raw/           # kanjidic2.xml
    jmdict/raw/             # jmdict.xml
    jmdict-furigana/raw/    # furigana XML/JSON
    tanos-jlpt/raw/         # jlpt-n5..n1 files
    leeds-frequency/raw/    # japanese.txt
    yomichan-jlpt-vocab/raw/# terms JSON
```

Sources that are absent are skipped (with a warning). Sources that are
present must be retrievable; the pipeline fails loudly on fatal corruption.

### 2. Query from Kotlin

```kotlin
import io.kaiteyo.kjd.api.JapaneseDatabase
import io.kaiteyo.kjd.api.JapaneseDataApi
import java.io.File

val db = JapaneseDatabase.open(File("kjd-japanese.db"))
val api = JapaneseDataApi(db)

val kanji = api.getKanji("食")                 // Kanji? — readings, meanings, JLPT, strokes
val vocab = api.getVocabulary("食べる")          // VocabularyEntry? — senses, furigana, frequency
val strokes = api.getStrokeData("食")           // List<Stroke> — paths + bounding boxes
val results = db.search("たべる")                // List<SearchResult> — kanji + vocab
val radical = api.getRadical("食")              // Radical?
api.close()
```

### 3. Use the CLI

```bash
kjd info
kjd search kanji 食
kjd search vocab 食べる
kjd lookup 食べる
kjd strokes 食
kjd radical 食
kjd stats
kjd validate
kjd sources
kjd export --format json --out export.json
```

## Provenance & licensing

Every fact in the database carries a `SourceRef` (source id, record id,
transformation, canonical flag). Every generated release embeds a
machine-readable and human-readable attribution manifest
(`third_party/THIRD_PARTY_DATA.json` / `.md`).

**License handling is first-class, not a README footnote.** Each source is
represented programmatically with its license, attribution requirement and
redistribution notes. The bundled datasets and their licenses are:

| Source | License |
|---|---|
| KanjiVG | CC BY-SA 3.0 |
| KANJIDIC | CC BY-SA 3.0 |
| JMdict | CC BY-SA 4.0 |
| JmdictFurigana | CC BY-SA 4.0 |
| Tanos JLPT lists | Free with attribution (verify current terms) |
| Leeds frequency data | Free for research/education with attribution (verify current terms) |
| yomichan-jlpt-vocab | CC BY-SA 4.0 |

**Verify the current redistribution terms before distributing a bundled
release.** The software license of KJD is separate from the data licenses;
externally sourced data is never claimed to be owned by Kaiteyo.

## Design principles

- **No runtime dependency on source projects.** Once the DB is generated, all
  lookups are offline and self-contained.
- **Source conflicts are never silently overwritten.** Canonical selection
  rules exist (e.g. Tanos is canonical JLPT; yomichan-jlpt-vocab is
  secondary), and every value remains traceable to its source.
- **Stable IDs.** Entities carry stable ids (`kanji:食`, `vocab:jmdict_1000990`)
  so applications (decks, mining, OCR, media) can reference them without
  copying dictionary data.
- **Deterministic builds.** Same source versions + same generator version ⇒
  same database (data rows carry no timestamps).
- **Validation is loud.** Fatal findings abort the build; warnings land in the
  quality report.
- **Separation from user data.** The canonical language database is immutable
  and read-only at runtime; user learning data lives elsewhere.

## Schema

See `src/main/kotlin/io/kaiteyo/kjd/db/Schema.kt`. The schema is versioned
(`meta.schema_version`); migrations are supported by keeping schema versions
explicit and validating on open.

## Developing

```bash
./gradlew :kjd:test        # unit + integration tests
./gradlew :kjd:run --args="info"
```

## Future direction

- Example-sentence dataset adapters (Tatoeba) behind the same pipeline
- Incremental patch updates (base DB + delta) instead of full rebuilds
- Additional export formats (compact binary)
- Pitch-accent and grammar extension datasets without schema breakage
