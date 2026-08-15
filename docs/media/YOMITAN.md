# Yomitan-style Glossing in Kaiteyo

> **Status**: dictionary import + popup glossing are `IMPLEMENTED` in the desktop suite;
> product integration is `TARGET` (ADR-0017). Browser-extension delivery is `PLANNED`/
> `FUTURE`. This document is the research + native-design record (MASTER §16).

## 1. What Yomitan is (research summary)

Yomitan (formerly Yomichan) is a browser extension that scans Japanese text, looks up
terms/kanji in user-imported dictionaries, and shows definitions in a popup — with audio,
pitch, frequency, and Anki mining. Key properties:

| Property | Details |
|---|---|
| Dictionary format | ZIP/JSON term banks + kanji banks, `index.json` metadata (name, revision, language, format) |
| Text scanning | Segments selection into words (dictionary-driven), deinflects conjugated forms |
| Term lookup | Scoring over readings/spellings; frequency/pitch annotations when present |
| Kanji lookup | Radical, stroke, readings, meanings from kanji banks |
| Popup architecture | Browser-injected overlay at mouse/text selection |
| Anki integration | Sends mined notes via AnkiConnect |
| Licensing | GPL-3.0 (code); dictionaries are third-party data with their own licenses |

## 2. What Kaiteyo reuses

- **The dictionary format** (compatibility, not the extension): Kaiteyo imports
  Yomitan-compatible archives (ZIP/folder/JSON/JMdict) via `DictionaryImporter`
  (`docs/architecture/dictionary.md`).
- **The interaction pattern**: hover/click → popup with readings, definitions, example,
  tags, TTS, mining actions (`DictionaryPopup`).
- **The deinflection/segmentation model**: `Deinflect` + `JapaneseSegmenter`, with
  `SearchMode` EXACT/PREFIX/KANA/DEINFLECT (suite engine).

## 3. What Kaiteyo does NOT copy

- The browser extension itself, its UI, or its permission model.
- Any dependency on a browser being present (the product is a native app).
- Bundled third-party dictionaries (users import their own; Kaiteyo bundles only its
  own licensed data from kjd).

## 4. CAN Kaiteyo provide Yomitan-like functionality without a browser extension?

**Answer: yes — and the suite already proves it.** Evaluation of delivery mechanisms:

| Option | Verdict | Status |
|---|---|---|
| **A. Native application text selection** | ✅ Best for the desktop app — full control, offline, no permissions | `IMPLEMENTED` (suite popup) |
| **B. WebView injection** | 🟡 Good for in-app browser/reader contexts (learning browser uses JavaFX WebView) | `PARTIAL` (reader mode) — target: inject glossing into rendered pages |
| **C. Browser extension** | 🔵 Powerful for *other* browsers, but heavy maintenance + distribution (stores) | `FUTURE` (not decided; no ADR) |
| **D. Local browser integration** | 🟡 Companion tools via the local HTTP API + text-hook server (works today for media tools) | `IMPLEMENTED` (suite `LocalApiServer`, `TextHookServer`) |
| **E. Clipboard-based lookup** | ✅ Universal fallback on any platform | `PLANNED` (clipboard OCR/lookup exists partially in suite OCR pipeline) |
| **F. Accessibility APIs / OS selection APIs** | 🟡 Interesting for OS-wide lookup (Windows UI Automation, macOS AX) | `RESEARCH` (not evaluated in depth; record the decision here when done) |
| **G. OCR** | ✅ For images/video frames — camera/OCR capture feeds the same popup | `PARTIAL` (Tess4J when installed) |

**Decision (recorded)**: native in-app selection + popup over imported dictionaries is
the primary path; WebView injection extends it to rendered web content inside the app; a
full browser extension is explicitly deferred and will need its own ADR if ever pursued
(STANDARDS §197).

## 5. Popup feature contract (product target)

The popup (in any context — text editor, media subtitles, browser reader, Journey world)
provides: headword, reading(s), definition(s), example, tags (JLPT, radicals),
pronunciation (TTS), pitch/frequency annotations when data exists (KT-VOCAB-003), and
actions: create card, edit card, add tags/flags, suspend/bookmark, copy, open full
dictionary. Actions feed `MiningEngine` via `MiningPayload`
(`docs/architecture/mining.md`).

## 6. Glossary engine (target)

- One internal glossary engine (STANDARDS §197) over the bundled kjd data **and**
  user-imported dictionaries (enabled/priority order).
- Indexing per STANDARDS §186–§187 (FTS/trigram/prefix; no brute-force scans).
- The glossary is a **node interface** in the knowledge graph (NODE §81) — lookups and
  traversal chips share the same engine.

## Related

- Dictionary engine: `docs/architecture/dictionary.md`
- Dictionary import docs: `docs/integrations/YOMITAN_DICTIONARIES.md`
- Mining: `docs/architecture/mining.md`
- Media workflows: [`ASBPLAYER_WORKFLOW.md`](ASBPLAYER_WORKFLOW.md)
- Product spec: `docs/product/PRODUCT.md` (MASTER §16)
