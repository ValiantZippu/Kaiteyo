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
