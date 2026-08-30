# Kaiteyo — Yomitan Integration

> **Status**: `IMPLEMENTED` (native replacement) — see `docs/architecture/dictionary.md`.
> Reference: `reference/yomitan/` (upstream clone for research; do not modify as part of Kaiteyo impl).

## 1. Upstream

- Repository: https://github.com/themoeway/yomitan
- License: **GPL-3.0** — verify before copying any file; Kaiteyo is GPL-3.0 so compatible, but attribution + share-alike apply. Never copy without license header + attribution entry in `docs/legal/THIRD_PARTY_NOTICES.md`.
- Format: Yomitan dictionaries are ZIP archives with `index.json` (`format: 3`, title, revision, sequenced, frequencyMode, etc.) + `term_bank_*.json` / `kanji_bank_*.json` / `tag_bank_*.json`.

## 2. Architecture (Yomitan)

Yomitan is a **browser extension** (content script → background → popup) with IndexedDB dictionary storage, Japanese deinflection, tokenization, and Handlebars-based glossary rendering. Assumptions: DOM available, extension messaging, IndexedDB, browser storage sync.

## 3. What Kaiteyo reuses

| Yomitan module | Kaiteyo reuse |
|----------------|---------------|
| Dictionary format / `index.json` meta / term banks | ✅ — `DictionaryImporter.parseIndexMeta` + `DictImportBundle` |
| Deinflection rules (ichidan/godan/adjective/noun conjugation) | ✅ — `desktop/engine/dictionary/Deinflect.kt` |
| Tokenization helpers | partial — `JapaneseSegmenter.kt` adapts for non-browser |
| `tag_bank.json` → tags (JLPT/grade/frequency) | ✅ |
| Glossary Handlebars templates | ✅ — `HandlebarsEngine.kt` (ported rendering) |

## 4. What Kaiteyo adapts

The browser surface: no content script / extension messaging / IndexedDB / browser.storage. Replaced by `DictionaryRepository` (on-disk per-dict `*.json` index in `data/index/`), `DictionaryService` (priority + SearchMode), and `DictionaryPopup` (Compose, not HTML popup).

## 5. What Kaiteyo does NOT copy

Extension UI, manifest, background page, IndexedDB schema, browser action — not reused. Popup styling is Kaiteyo's `Ds*` system.

## 6. Integration difficulty / risk

Low — format is documented and stable; deinflection rules are pure data. Risk is license compliance (GPL) and future format changes (major `format` bump in `index.json`).

## 7. Maintenance

Track upstream releases; test new `format` versions in `YomitanImporterTest` before accepting.
