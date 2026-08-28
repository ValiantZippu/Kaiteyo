# Media Center — Yomitan

> Status: IMPLEMENTED (shared provider)  
> Files: `desktop/engine/dictionary/*` (`DictionaryService`, `DictionaryRepository`, `DictionaryImporter`)

## Architecture

`SelectedText → DictionaryService → YomitanProvider → DictionaryEntry → MiningContext`

- Yomitan is a **dictionary provider**, not a UI component. The same `DictionaryService` serves Browser, Reading, Media, OCR, clipboard — no second dictionary for media.
- Media sends selected subtitle text (`MediaEngine.selectedTokens` → `joinTokenSurfaces` → `lookupQuery`) to `DictionaryService.lookup(query)`.

## Provider

- `DictionaryImporter` parses Yomitan-compatible archives (ZIP, folder, JSON array/object) via `parseIndexMeta` (`index.json` `format` field). Supported: index folder, ZIP, lone `index.json`, single term JSON, `.json` (JMdict).
- `DictionaryRepository` owns installed dictionaries (`installedDictionaries()` prioritized, `enabledDictionaries()` filtered `enabled=true`), builds on-demand index per-dictionary (`data/index/*.json`), search with `SearchMode` (EXACT, PREFIX, KANA, DEINFLECT) + scoring.
- `DictionaryService.lookup` returns grouped/flat results with `DictionaryMatch(entry + source dictionary + score)`, favorites `favorites.json`, history `history.json`.

## Media wiring

- `MediaEngine.selectToken(token)` → `openLookup(surface)` → `dictionaryOpen=true`, `dictionaryLookupCount++`, `statistics.recordLookup()`, `EventLog(DictionaryLookup)` + `SubtitleSelected`.
- `MediaEngine.payloadForCue(cue, token, phraseTokens)` prefers `token.dictionaryMatch` else phrase `lookup()` else segment fallback; `definition` = `entry.senses.glosses`; `tags` include `dict:<name>`, `phrase`, `media:<collection>`, `subtitle`.
- `DictionaryPopup` (hover/click on `ReadingView`/ `Media`) shows headword, readings, senses, example, JLPT/radical tags, TTS, actions (Create/Edit/Tag/Suspend/Copy/TTS/Open manager) → `MiningEngine.mine(payload)`.

## No duplication

Media never calls Forvo/TTS/provider directly; all through `DictionaryService` + `KanaTtsManager`/`AudioDictionaryService` abstractions.
