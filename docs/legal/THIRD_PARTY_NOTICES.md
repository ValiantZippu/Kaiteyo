# Third-Party Notices

This page summarizes the third-party data and libraries bundled with (or integrated into)
Kaiteyo. The authoritative, machine-readable list is `core/credits/libraries/*.json`,
rendered in-app on the Credits/About screen via AboutLibraries.

## Bundled data sets

| Dataset | Purpose | License | Attribution |
|---|---|---|---|
| [KanjiVG](https://kanjivg.tagaini.net/) | Stroke-order paths, radicals, components | CC BY-SA 3.0 | © Ulrich Apel — attribute and ShareAlike |
| [KANJIDIC](https://www.edrdg.org/kanjidic/kanjdicindex.html) | Kanji info: meanings, readings, classifications | CC BY-SA 3.0 | EDICT/KANJIDIC files © The Electronic Dictionary Research and Development Group (EDRDG) |
| [JMdict](https://www.edrdg.org/jmdict/j_jmdict.html) | Vocabulary entries and senses | CC BY-SA 4.0 | EDRDG |
| [JmdictFurigana](https://github.com/Doublevil/JmdictFurigana) | Furigana segmentation | CC BY-SA 4.0 | © JmdictFurigana contributors |
| [Tanos JLPT lists](http://www.tanos.co.uk/jlpt/) | JLPT kanji classification | CC BY 3.0 (per in-app credits) | © Jonathan Waller — verify current terms |
| [Leeds frequency data](https://corpus.leeds.ac.uk/list.html) | Word frequency ranking | CC BY 2.5 (per in-app credits) | University of Leeds — verify current terms |
| [yomichan-jlpt-vocab](https://github.com/stephenmk/yomichan-jlpt-vocab) | JLPT tags for vocabulary | CC BY-SA 4.0 | © stephenmk |
| [Netflix Japanese Frequency List](https://github.com/pciavolici/Netflix-Japanese-Subtitle-Frequency-List) | Subtitle word frequency | Custom (free with attribution) | © OhTalkWho オタク (Dave Doebrick) |
| [Chris Kempson Subtitle Frequency](https://github.com/chriskempson/japanese-subtitles-word-frequency-list) | Word + kanji frequency from subtitles | MIT | © Chris Kempson |
| [Patrick Kandrac 2242](https://forum.koohii.com/viewtopic.php?id=16394) | 2242 kanji frequency | Public | © Patrick Kandrac (sources: Google/KUF/MCD/文化庁) |
| [Nukemarine RTK Frequency](https://www.reddit.com/r/LearnJapanese/) | RTK frequency groups | Public | © Nukemarine |
| [Alex Yatskov Wikipedia Frequency](https://github.com/yatskov) | Wikipedia kanji frequency | Public | © Alex Yatskov |
| [Alexandre Girardi Word Frequency](http://ftp.monash.edu.au/pub/nihongo/) | Word frequency list | Public Domain | © Alexandre Girardi (Monash FTP Archive) |
| [Kanji Keys / TopoKanji](https://github.com/dshpika) | Kanji metadata + decomposition | CC BY 4.0 | © Dmitry Shpika |
| [CJK Decompositions Data](https://github.com/nieldlr/CJK-Decompositions) | Component decomposition | Public Domain | Public domain |
| [文化庁 (Agency for Cultural Affairs)](https://www.bunka.go.jp/) | Official kanji classifications | Government (public) | © Japanese Agency for Cultural Affairs |
| [kanjidatabase.com](https://kanjidatabase.com/) | Supplementary kanji metadata | Free (attribution) | kanjidatabase.com |
| [David Gouveia Kanji Data](https://github.com/davidgouveia) | Supplementary kanji data | Public | © David Gouveia |
| [Usagi Chan Phonetics Deck](https://ankiweb.net/shared/info/1218648935) | Phonetic component groups | CC BY-SA 4.0 | © shoui520 |
| [Shirabe Jisho](https://www.shirabejisho.com/) | JLPT lists + common words | Public | Shirabe Jisho |
| [kanjiapi.dev](https://kanjiapi.dev/) | REST API kanji data | Public | kanjiapi.dev (uses EDICT, KANJIDIC) |
| [Kanji School](https://github.com/drewdrawsws/kanji-school) | Kanji data from Jisho.org | Public | © Drew Edwards |

> Full provenance, update process, and transformations: [`../data/SOURCES.md`](../data/SOURCES.md).

## Key third-party libraries

| Library | Purpose | License |
|---|---|---|
| Kotlin / Kotlin Multiplatform | Language & toolchain | Apache-2.0 |
| Compose Multiplatform / Jetpack Compose | UI toolkit | Apache-2.0 |
| Koin | Dependency injection | Apache-2.0 |
| Ktor | HTTP client/server | Apache-2.0 |
| SQLDelight | SQLite access & typed SQL | Apache-2.0 |
| DataStore | Preferences | Apache-2.0 |
| kotlinx.serialization / datetime | Serialization, time | Apache-2.0 |
| Wanakana (core) | Japanese text conversion | MIT |
| AboutLibraries | Credits rendering | Apache-2.0 |
| Coil 3 | Image loading | Apache-2.0 |
| reorderable | List reordering | Apache-2.0 |
| VLCJ | VLC playback binding | GPL-3.0 |
| mpv (external process) | Optional media playback backend (IPC) | GPL-2.0-or-later, LGPL-2.1-or-later components |
| sqlite-jdbc | Desktop SQLite | Apache-2.0 |
| JNA | Native OS calls (window drag, media keys) | Apache-2.0 / LGPL |
| Firebase (googlePlay flavor only) | Analytics / crash reporting | proprietary ToS |
| ExoPlayer (media3) | Android media | Apache-2.0 |

The complete list with exact versions is in `gradle/libs.versions.toml` and the generated
AboutLibraries output (`desktopApp/src/jvmMain/composeResources/files/aboutlibraries.json`).

## Notices required by licenses

- **CC BY-SA 3.0/4.0 and CC BY datasets** require attribution and share-alike on
  derivatives. Kaiteyo's generated database includes per-entity provenance; the app
  credits screen and this page satisfy attribution for the bundled distribution.
- **GPL components (VLCJ / libVLC)** — VLCJ is GPL-3.0, compatible with Kaiteyo's GPL-3.0.
  Media playback is powered by libVLC/VLC when installed; VLC itself is LGPL-2.1-or-later
  with GPL-2.0-or-later plugin components. Source availability applies per the GPL.
- **mpv backend** — when selected, Kaiteyo drives an installed mpv process over IPC;
  mpv is GPL-2.0-or-later with LGPL-2.1-or-later components. Kaiteyo does not bundle mpv;
  the user's system installation is used and its license governs.
- **OFL fonts** (media generator promo assets) — redistribution permitted with the
  license retained.

## Reporting

If you believe a notice is missing or incorrect, open an issue at
<https://github.com/ValiantZippu/Kaiteyo/issues>.
