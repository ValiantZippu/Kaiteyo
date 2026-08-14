# Open-Source Data Sources & Attribution

Kaiteyo bundles Japanese-language data that comes from **openly licensed third-party
datasets**. Original Kaiteyo code and data are always kept distinct from third-party
datasets: the KJD pipeline records a `SourceRef` on every entity and emits a
machine-readable attribution manifest with each generated database.

> **License accuracy:** the licenses below come from (a) the in-app credits
> (`core/credits/libraries/*.json`, displayed via AboutLibraries) and (b) the KJD source
> definitions (`kjd/.../source/`). Where a source's current redistribution terms should be
> re-verified before a distribution, that is stated explicitly. **Kaiteyo does not
> invent licenses.** An undeclared license is reported as "Not declared by source".

## The KJD pipeline

All datasets below are ingested by **KJD** (`kjd/` — the Kaiteyo Japanese Data Platform):

```
External datasets
    ↓ source adapters
Raw source store (sources/<id>/raw/)
    ↓ parsers
Normalization → entity resolution → cross-source linking
    ↓ validation
SQLite database + indexes  →  packaged as the bundled AppDataDatabase
    ↓
Attribution manifest (THIRD_PARTY_DATA.json / .md)
```

- The pipeline is **deterministic** (same source versions + same generator ⇒ same database).
- Sources that are absent are skipped with a warning; the build fails loudly on fatal
  corruption.
- Prebuilt databases are distributed as `kjd-japanese-<version>.db`; the desktop app can
  apply **incremental patch updates** (`DatabasePatcher`) instead of full rebuilds.
- Full developer documentation: `kjd/README.md` and `architecture/DATA_PLATFORM.md`.

## Bundled datasets

### KanjiVG — stroke-order data

| Field | Value |
|---|---|
| Official source | <https://kanjivg.tagaini.net/> |
| Purpose | Per-stroke SVG paths, stroke order, radical/component information |
| License | CC BY-SA 3.0 |
| Attribution | Required (ShareAlike). KanjiVG © Ulrich Apel |
| Bundled or downloaded | Attached at KJD build time (`kjd build --vg <dir>`); stroke sets are attributed to `kanjivg` in provenance. Not a runtime download |
| Update process | Re-run KJD with a newer KanjiVG dump |
| Transformation | SVG path extraction, stroke numbering, normalization, bounds computation (`KanjiVgGeometryProvider`) |
| Compatibility | CC BY-SA 3.0 is compatible with Kaiteyo's GPL-3.0 for data distribution; keep attribution |

### KANJIDIC — character information

| Field | Value |
|---|---|
| Official source | <https://www.edrdg.org/kanjidic/kanjdicindex.html> |
| Purpose | Kanji meanings, readings, classifications (grade, JLPT, frequency, radicals) |
| License | CC BY-SA 3.0 (per in-app credits) |
| Bundled or downloaded | Generated into the bundled app database by KJD |
| Update process | Re-run KJD with a newer KANJIDIC file |
| Transformation | XML parsing, normalization, canonical entity resolution |

### JMdict — Japanese–Multilingual dictionary

| Field | Value |
|---|---|
| Official source | <https://www.edrdg.org/jmdict/j_jmdict.html> |
| Purpose | Vocabulary entries: expressions, readings, senses, parts of speech |
| License | CC BY-SA 4.0 |
| Bundled or downloaded | Generated into the bundled app database by KJD |
| Update process | Re-run KJD with a newer JMdict XML |
| Transformation | XML parsing, sense/reading splitting, furigana attachment, canonical IDs |

### JmdictFurigana — furigana data

| Field | Value |
|---|---|
| Official source | <https://github.com/Doublevil/JmdictFurigana> |
| Purpose | Furigana (reading) segmentation for JMdict words |
| License | CC BY-SA 4.0 |
| Bundled or downloaded | Generated into the bundled app database by KJD |
| Update process | Re-run KJD with a newer furigana dataset |

### Tanos JLPT lists — JLPT classification

| Field | Value |
|---|---|
| Official source | <http://www.tanos.co.uk/jlpt/> |
| Purpose | JLPT level classification for kanji (N5–N1) |
| License | CC BY 3.0 (per in-app credits); Tanos states "free to use with attribution" — **verify current terms before redistribution** |
| Bundled or downloaded | Generated into the bundled app database by KJD; canonical JLPT source (yomichan-jlpt-vocab is secondary) |
| Update process | Re-run KJD with a newer list |

### Leeds frequency data — word frequency

| Field | Value |
|---|---|
| Official source | <https://corpus.leeds.ac.uk/list.html> |
| Purpose | Frequency ranking of Japanese words from internet corpus |
| License | CC BY 2.5 (per in-app credits); Leeds states free for research/education with attribution — **verify current terms before redistribution** |
| Bundled or downloaded | Generated into the bundled app database by KJD |
| Update process | Re-run KJD with a newer list |

### yomichan-jlpt-vocab — JLPT vocabulary tags

| Field | Value |
|---|---|
| Official source | <https://github.com/stephenmk/yomichan-jlpt-vocab> |
| Purpose | JLPT-level tags for vocabulary (associations between Tanos JLPT words and JMdict entries) |
| License | CC BY-SA 4.0 |
| Bundled or downloaded | Generated into the bundled app database by KJD (secondary JLPT source) |
| Update process | Re-run KJD with a newer export |

## Other data assets (not dictionary content)

| Asset | Source / license | Notes |
|---|---|---|
| TTS kana voice (Neural2B) | Google Text-to-Speech neural voice assets (see asset licensing in `buildSrc/.../AppAssets.kt`) | Downloaded on first build; wav for desktop/iOS, opus for Android |
| Fonts (media generator) | Quicksand (OFL) used by `mediaGenerator` for promo assets | Build-time only |
| App icons / brand | Original Kaiteyo artwork (see `docs/branding/`) | Kaiteyo-owned |

## User-imported dictionaries (desktop)

The desktop suite also imports **user-provided** Yomitan-compatible dictionaries
(ZIP/JSON, JMdict/KANJIDIC/KanjiVG formats). These are *not* bundled and *not* distributed
by Kaiteyo — they live in `~/.kaiteyo/dictionaries/` and stay on the user's device.
Users are responsible for the licensing of the dictionaries they import. See
`integrations/YOMITAN_DICTIONARIES.md`.

## Redistribution checklist (before shipping a release)

1. Generated databases embed the attribution manifest — verify `THIRD_PARTY_DATA.md` is
   included with the app build.
2. The in-app credits screen (AboutLibraries + `core/credits/libraries/`) lists every
   bundled dataset with its license — keep it in sync with `core/credits/`.
3. Re-verify the current redistribution terms of Tanos and Leeds before each release (they
   may change over time).
4. Never remove attribution headers from bundled data.
