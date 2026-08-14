# Yomitan-Style Dictionaries & Popup Lookup

Kaiteyo's desktop suite implements a **Yomitan-style reading workflow**: import
Yomitan-compatible dictionaries, hover any Japanese text, and get an instant popup with
readings, definitions, mining actions, and more — without leaving the app.

> Yomitan (https://github.com/themoeway/yomitan) is a browser extension; Kaiteyo is not a
> fork of it and does not depend on it. "Yomitan-style" refers to the *workflow* and the
> *dictionary file format* (which Kaiteyo reads directly).

## Purpose

The dictionary layer is the foundation of the whole immersion loop: look up → mine →
review. It also powers lookup from the Media center (subtitles), the Learning Browser,
OCR results, and the reading workspace.

## Dictionary import

| Detail | Value |
|---|---|
| Formats | Yomitan-compatible archives: ZIP, index folder, lone `index.json`, single term JSON; `.json` term documents (treated as JMdict); JMDict / KANJIDIC / KanjiVG style data |
| Parser | `desktopApp/.../engine/dictionary/DictionaryImporter.kt` — `parseIndexMeta` extracts name/revision/format from `index.json`; returns a `DictImportBundle` |
| Storage | `~/.kaiteyo/dictionaries/` with per-dictionary JSON index files under `~/.kaiteyo/index/` |
| Duplicate handling | Reinstalling a dictionary with the same id replaces it |
| Security | Archives are parsed locally with strict/extracting parsers; corrupt archives fail with clear errors (no code execution) |

## Lookup & search

- `DictionaryService` (app-facing controller owned by `AppState`) → `DictionaryRepository`
  (installed/enabled dictionaries, on-disk index).
- Search modes: EXACT, PREFIX, KANA, DEINFLECT (`SearchMode` flags) with scoring and
  deinflection (`Deinflect.kt`, `JapaneseSegmenter.kt`).
- Results are grouped by dictionary and rendered as a flat list
  (`DictionaryLookupCard`).

## The popup (DictionaryPopup)

Hovering or clicking Japanese text (in media subtitles, the browser, OCR results, or
plain text) opens `DictionaryPopup` at the pointer:

- Headword, reading(s), definition(s), example sentence, tags (JLPT, radicals)
- **Pronunciation** (text-to-speech)
- **Mining**: "Create card" feeds `MiningEngine` with a `MiningPayload` (headword, reading,
  definition, sentence, screenshot/audio paths, timestamp, source, tags)
- **Edit card** (opens the card editor), **add tags/flags**, **suspend/bookmark**
- **Copy** (headword / reading / definition)
- **Open full dictionary** (jumps to `DictionaryManagerView`)

## The workflow

1. Play a video with subtitles / open a page in the browser / OCR an image.
2. Hover a word → popup appears.
3. Click "Create card" → sentence card lands in the SRS queue with media context.
4. Review later; jump back to the exact scene (timestamp is stored on the card).

## Licensing note

Bundled dictionary data comes from the open datasets in `docs/data/SOURCES.md`.
**User-imported** dictionaries are the user's responsibility — Kaiteyo never redistributes
them. See `data/SOURCES.md` → "User-imported dictionaries".
