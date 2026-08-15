---
title: Data & Attribution
description: The open datasets Kaiteyo uses, their licenses, sources, and how each one is used.
---

Kaiteyo bundles Japanese-language data from **openly licensed third-party datasets**. Original Kaiteyo code and data are kept distinct from third-party datasets: the data pipeline records a source reference on every entity and emits a machine-readable attribution manifest with each generated database.

## Bundled datasets

| Dataset | Purpose | License | Attribution |
|---|---|---|---|
| [KanjiVG](https://kanjivg.tagaini.net/) | Stroke-order paths, stroke order, radicals, components | CC BY-SA 3.0 | © Ulrich Apel — attribute and ShareAlike |
| [KANJIDIC](https://www.edrdg.org/kanjidic/kanjdicindex.html) | Kanji meanings, readings, grade, JLPT, frequency, radicals | CC BY-SA 3.0 | EDICT/KANJIDIC files © The Electronic Dictionary Research and Development Group (EDRDG) |
| [JMdict](https://www.edrdg.org/jmdict/j_jmdict.html) | Vocabulary entries and senses | CC BY-SA 4.0 | EDRDG |
| [JmdictFurigana](https://github.com/Doublevil/JmdictFurigana) | Furigana segmentation for vocabulary | CC BY-SA 4.0 | © JmdictFurigana contributors |
| [Tanos JLPT lists](http://www.tanos.co.uk/jlpt/) | JLPT kanji classification | CC BY 3.0 (per in-app credits) | © Jonathan Waller — verify current terms |
| [Leeds frequency data](https://corpus.leeds.ac.uk/list.html) | Word frequency ranking | CC BY 2.5 (per in-app credits) | University of Leeds — verify current terms |
| [yomichan-jlpt-vocab](https://github.com/stephenmk/yomichan-jlpt-vocab) | JLPT tags for vocabulary | CC BY-SA 4.0 | © stephenmk |

## How the data pipeline works

External datasets are ingested by the Kaiteyo Japanese Data Platform (KJD), a deterministic build pipeline:

```
External datasets
    ↓ source adapters
Raw source store (sources/<id>/raw/)
    ↓ parsers
Normalization → entity resolution → cross-source linking
    ↓ validation
SQLite database + indexes → packaged as the bundled app database
    ↓
Attribution manifest (THIRD_PARTY_DATA.json / .md)
```

- The pipeline is deterministic — same source versions plus the same generator produce the same database.
- Sources that are absent are skipped with a warning; the build fails loudly on fatal corruption.
- Prebuilt databases ship as `kjd-japanese-<version>.db`, with incremental patch updates applied in-app instead of full rebuilds.

## Third-party libraries

The app also bundles open-source libraries (Kotlin, Compose Multiplatform, Koin, Ktor, SQLDelight, DataStore and more). The complete, machine-readable list lives in `core/credits/libraries/*.json` and is rendered in-app on the Credits/About screen.

## Full documentation

- [Data & attribution documentation](/docs/data/sources/) — complete source list, licenses, provenance and update process
- [Third-party notices](/docs/legal/third_party_notices/) — summary of bundled data and libraries
- [Data platform architecture](/docs/data/architecture/) — how the pipeline works in detail
- [License](/license/) — Kaiteyo's own GPL-3.0 license
