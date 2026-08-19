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

## Frequency sources (new)

### Netflix Japanese Frequency List

| Field | Value |
|---|---|
| Official source | <https://github.com/pciavolici/Netflix-Japanese-Subtitle-Frequency-List> |
| Purpose | Word frequency from Netflix Japanese subtitles |
| License | Custom (free with attribution) |
| Attribution | © OhTalkWho オタク (Dave Doebrick) |
| Bundled or downloaded | Downloaded at build time |
| Update process | Re-run KJD with a newer frequency list |

### Chris Kempson Subtitle Frequency

| Field | Value |
|---|---|
| Official source | <https://github.com/chriskempson/japanese-subtitles-word-frequency-list> |
| Purpose | Word + kanji frequency from Japanese subtitles |
| License | MIT |
| Attribution | © Chris Kempson |
| Bundled or downloaded | Downloaded at build time |
| Update process | Re-run KJD with a newer export |

### Patrick Kandrac 2242 Kanji Frequency

| Field | Value |
|---|---|
| Official source | <https://forum.koohii.com/viewtopic.php?id=16394> |
| Purpose | Frequency ranking for 2242 kanji from multiple sources |
| License | Public (free with attribution) |
| Attribution | © Patrick Kandrac (sources: Google Kanji Data, KUF, MCD, 文化庁) |
| Bundled or downloaded | Downloaded at build time |
| Update process | Re-run KJD with a newer list |

### Nukemarine RTK Frequency Groups

| Field | Value |
|---|---|
| Official source | <https://www.reddit.com/r/LearnJapanese/> |
| Purpose | Frequency groups organized by RTK order |
| License | Public (free with attribution) |
| Attribution | © Nukemarine |
| Bundled or downloaded | Downloaded at build time |
| Update process | Re-run KJD with a newer export |

### Alex Yatskov Wikipedia Frequency

| Field | Value |
|---|---|
| Official source | <https://github.com/yatskov> |
| Purpose | Kanji frequency extracted from Japanese Wikipedia |
| License | Public (free with attribution) |
| Attribution | © Alex Yatskov |
| Bundled or downloaded | Downloaded at build time |
| Update process | Re-run KJD with a newer export |

### Alexandre Girardi Word Frequency

| Field | Value |
|---|---|
| Official source | <http://ftp.monash.edu.au/pub/nihongo/> |
| Purpose | Word frequency from Japanese text corpus |
| License | Public Domain |
| Attribution | © Alexandre Girardi (public domain, Monash FTP Archive) |
| Bundled or downloaded | Downloaded at build time |
| Update process | Re-run KJD with a newer list |

### Kanji Keys / TopoKanji (Dmitry Shpika)

| Field | Value |
|---|---|
| Official source | <https://github.com/dshpika> |
| Purpose | Kanji metadata and frequency data |
| License | CC BY 4.0 |
| Attribution | © Dmitry Shpika |
| Bundled or downloaded | Downloaded at build time |
| Update process | Re-run KJD with a newer export |

## Structural data sources (new)

### CJK Decompositions Data

| Field | Value |
|---|---|
| Official source | <https://github.com/nieldlr/CJK-Decompositions> |
| Purpose | Kanji component decomposition data |
| License | Public Domain |
| Bundled or downloaded | Downloaded at build time |
| Update process | Re-run KJD with a newer export |

### 文化庁 (Agency for Cultural Affairs)

| Field | Value |
|---|---|
| Official source | <https://www.bunka.go.jp/> |
| Purpose | Official kanji classifications (jōyō, jinmeiyō, educational) |
| License | Government (public) |
| Attribution | © Japanese Agency for Cultural Affairs |
| Bundled or downloaded | Downloaded at build time |
| Update process | Re-run KJD with a newer export |

### kanjidatabase.com

| Field | Value |
|---|---|
| Official source | <https://kanjidatabase.com/> |
| Purpose | Supplementary kanji metadata |
| License | Free (with attribution) |
| Bundled or downloaded | Downloaded at build time |
| Update process | Re-run KJD with a newer export |

### David Gouveia Kanji Data

| Field | Value |
|---|---|
| Official source | <https://github.com/davidgouveia> |
| Purpose | Supplementary kanji metadata |
| License | Public (free with attribution) |
| Attribution | © David Gouveia |
| Bundled or downloaded | Downloaded at build time |
| Update process | Re-run KJD with a newer export |

## Learning metadata sources (new)

### Usagi Chan Kanji Phonetics Deck

| Field | Value |
|---|---|
| Official source | <https://ankiweb.net/shared/info/1218648935> |
| Purpose | Phonetic component groups for kanji study |
| License | CC BY-SA 4.0 |
| Attribution | © shoui520 |
| Bundled or downloaded | Downloaded at build time |
| Update process | Re-run KJD with a newer export |

### Shirabe Jisho JLPT Lists

| Field | Value |
|---|---|
| Official source | <https://www.shirabejisho.com/> |
| Purpose | JLPT kanji classification |
| License | Public (free with attribution) |
| Attribution | Shirabe Jisho |
| Bundled or downloaded | Downloaded at build time |
| Update process | Re-run KJD with a newer export |

### Shirabe Jisho Common Words

| Field | Value |
|---|---|
| Official source | <https://www.shirabejisho.com/> |
| Purpose | Common Japanese words list |
| License | Public (free with attribution) |
| Attribution | Shirabe Jisho |
| Bundled or downloaded | Downloaded at build time |
| Update process | Re-run KJD with a newer export |

### kanjiapi.dev

| Field | Value |
|---|---|
| Official source | <https://kanjiapi.dev/> |
| Purpose | REST API data for kanji readings, meanings, examples |
| License | Public (free with attribution) |
| Attribution | kanjiapi.dev (uses EDICT, KANJIDIC) |
| Bundled or downloaded | Downloaded at build time |
| Update process | Re-run KJD with a newer export |

### Kanji School (Drew Edwards)

| Field | Value |
|---|---|
| Official source | <https://github.com/drewdrawsws/kanji-school> |
| Purpose | Kanji data from Jisho.org (KANJIDIC-derived) |
| License | Public (free with attribution) |
| Attribution | © Drew Edwards |
| Bundled or downloaded | Downloaded at build time |
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
