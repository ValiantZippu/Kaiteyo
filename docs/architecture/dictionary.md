# Kaiteyo Architecture — Dictionary Engine Specification

**Status**: Implemented in the desktop suite (engines are real and tested; suite-only views
pending the consolidation decision); bundled dictionary lookup implemented in the core app
**Owner**: suite `ua.syt0r.kanji.desktop.engine.dictionary` + core `app_data`
**Related**: `docs/architecture/language-model.md` · `docs/integrations/YOMITAN_DICTIONARIES.md` ·
`docs/user-guide/DESKTOP_SUITE.md` · `docs/architecture/database.md`

## 1. Purpose

A native, Yomitan-compatible glossary engine. It parses and indexes established dictionary
formats, searches across all installed dictionaries, and feeds the dictionary popup, the
lookup card, mining, the segmenter and the localhost API. Per §197 Kaiteyo implements an
**internal glossary engine** using compatible dictionary data — no browser-extension
dependency. All models are pure, `@Serializable` and platform-neutral so they persist and
cross the API boundary.

## 2. Architecture

```
UI (DictionaryLookupCard / DictionaryPopup / manager / API)
        │
        ▼
DictionaryService            ← app-facing controller, owned by AppState
        │                      (query state, recent searches, favorites, import orchestration)
        ▼
DictionaryRepository         ← owns installed dictionaries + entries + on-disk index
        │
        ├── DictionaryImporter  ← parses Yomitan/JMdict shapes into DictionaryEntry
        ├── JapaneseText        ← kana/romaji/kanji detection & conversion (pure)
        ├── Deinflect           ← BFS deinflection rule table (pure)
        ├── SourceManifest      ← optional source metadata
        └── index/  (per-dictionary JSON) + installed.json
```

Separation of concerns: `DictionaryService` is UI-facing state (Compose-reactive), the
repository is persistence + search, the importer/utilities are pure functions. The
repository deliberately does **not** use the app-wide settings engine (reserved for UI
prefs) — it owns its own `installed.json` + `index/*.json` under `~/.kaiteyo/dictionary/`.

## 3. Data model (`DictionaryModels.kt`)

### `DictionaryEntry`
| Field | Type | Notes |
|---|---|---|
| `headword` | String | primary spelling, the key |
| `spellings` | List<String> | alternate spellings |
| `readings` | List<DictionaryReading> | per-reading data incl. pitch |
| `senses` | List<DictionarySense> | glosses, POS, tags, restrictions, xrefs |
| `kanjiSpellings` | List<KanjiSpelling> | KANJIDIC-style supplementary kanji info |
| `frequency` | FrequencyInfo | rank + score |
| `searchKeys` | List<String> | precomputed probe keys (kanji/kana/romaji) |
| `dictionaryId` | String | owning dictionary (assigned at install) |
| `source` | DictionaryEntryType | Vocabulary/Kanji/Grammar/Name/Expression |

### `DictionaryReading`
`reading` · `elements` (spelling elements) · `pitchAccents: List<PitchAccent>` ·
`readingInformation` (tags like on:/kun:) · `frequency: Double?` · `valueTags`.

### `PitchAccent`
`position: Int` (downstep position) · `downstep: Int?`.

### `DictionarySense`
`partOfSpeech: List<String>` · `glosses: List<String>` · `tags` · `restrictions` ·
`crossReferences: List<String>` (related kanji/vocab entries). `primaryGloss` =
first gloss or `""`.

### `KanjiSpelling`
`character` · `onReadings` · `kunReadings` · `meanings` · `strokeCounts` ·
`jlpt: Int?` · `grade: Int?` · `frequency: Int?` · `radicals`.

### `FrequencyInfo`
`rank: Int?` · `score: Double?`.

### `InstalledDictionary`
`id` · `name` · `revision` · `authoredBy` · `format: DictionaryFormat` ·
`sourceLanguage` (default `"ja"`) · `targetLanguage` (default `"en"`) · `enabled` ·
`priority: Int` (lower = first) · `createdAt: Instant` · `entryCount: Long` · `tags`.
`displayTitle` = name.

### `DictionaryFormat`
`Yomitan | JmDict | KanjiDic | Frequency | PitchAccent | Grammar | Name | Custom`.

### Match types
- `DictionaryMatch(entry, dictionary, score)` — one hit.
- `DictionaryResultGroup(dictionary, matches)` — grouped for multi-dict rendering.
- `MinedDictionaryData(headword, reading, definition, dictionary, pitchAccent,
  frequency, example)` — the golden record a mined card persists from a lookup.

## 4. DictionaryService (controller)

Compose-reactive state (all `mutableStateOf`/`mutableStateListOf`):

- `query` — current search text.
- `recentSearches` (max 50, deduped, most-recent-first) — persisted to `history.json`
  (`HistoryDto`); `clearHistory()`.
- `favorites` — keys `"$dictId::$headword"` persisted to `favorites.json`
  (`FavoritesDto`); `toggleFavorite`, `isFavorite`, `favoriteEntries()` re-resolves keys
  against live lookup.

Operations:
- `lookup(query, mode)` — grouped; records the search (blank queries are not recorded).
- `lookupFlat(query, mode)` — flat match list.
- `importFile(file, appState): Result<InstalledDictionary>` — runs `DictionaryImporter`,
  errors with an actionable message when zero entries parse, installs via repository,
  records an activity-log entry (category Study: `Installed dictionary "X" with N
  entries`), returns `Result`.
- `install(meta, entries, appState)` / `remove(id, appState)` / `setEnabled(id, enabled)`
  / `reorder(ids)` — management, each activity-logged (except enabled/reorder).
- Persistence reads/writes are `runCatching` — a corrupt `history.json`/`favorites.json`
  silently resets to empty rather than crashing.

**Bundled seed**: `SEED_DICTIONARY_ID = "kaiteyo-core-kanji"` — `seedEntries()` builds a
kanji dictionary from `demoKanji` (character, on/kun, meanings, JLPT/grade, frequency,
radicals; search keys include kana conversions of readings + radicals) so lookups work
offline even before any import. `seedMeta()`: format `Custom`, tags `[builtin, kanji,
jlpt, grade]`, priority 0.

## 5. DictionaryRepository

**Persistence layout** (`~/.kaiteyo/dictionary/`):
- `installed.json` — `RepositoryDto(dictionaries: [InstalledDto])` (id, name, revision,
  author, format, enabled, priority, entryCount, tags).
- `index/<dictId>.json` — `List<EntryDto>` (headword, spellings, readings, senses,
  searchKeys) — the serialized entry index for that dictionary.
- Entry indexes are **lazily loaded** per dictionary on first access (`entriesFor`) and
  cached in memory; the search `keyIndex` is rebuilt on demand (`indexDirty` flag).

**Install**: `install(meta, entries)` removes any same-id dictionary first, normalizes
entries with the dict id, persists the index, saves metadata, marks the index dirty,
returns the installed record with live `entryCount`. `installImport` derives metadata from
`DictImportResult` (priority = current installed count).

**Search index**: `keyIndex: Map<String, List<EntryRef>>` maps every distinct search key →
`EntryRef(dictId, headword)`. Built once per mutation, not per query.

### SearchMode — bit-flag value class
```
ModeFlag { EXACT, PREFIX, KANA, DEINFLECT }
SearchMode.Exact        = EXACT
SearchMode.Kana         = EXACT + KANA
SearchMode.All          = EXACT + PREFIX + KANA + DEINFLECT   (default)
```

### Search algorithm (`lookup(query, mode)`)
1. Trim; empty → `[]`. `ensureIndex()` (rebuild if dirty).
2. Restrict to enabled dictionaries' ids.
3. `buildKeys(q, mode)` — probe keys:
   - Kanji input → the kanji string itself.
   - Kana input → the string + hiragana + katakana (when `KANA`).
   - Romaji → the string + `romajiToHiragana(q)` + its hiragana form.
   - Any kana-containing input also gets hiragana + katakana equivalents.
4. Candidate collection for each key:
   - Exact hits on the key.
   - `PREFIX`: every index key `startsWith(key)` (min key length 2, capped at 80 prefix
     keys) — scored −2.
   - `DEINFLECT` (non-kanji input): `Deinflect.deinflect(q)`; each recovered headword
     probes the index (flat score 4). Example: 食べた → 食べる.
5. **Scoring** (`score(q, matchedKey, entry)`):
   - 10 — headword == query or contains query
   - 9 — any spelling == or contains query
   - 8 — any reading (kana-normalized) == query
   - 6 — matched key == query
   - 5 — otherwise (prefix/deinflected fallthrough)
   Input and keys are normalized via `cleaned()` (hiragana + lowercase).
6. Dedupe by `dictId|headword`, keep **highest score**, sort descending, return.

`lookupGrouped` runs `lookup` then groups matches under each enabled dictionary (in
priority order), dropping empty groups.

## 6. DictionaryImporter

`import(path: File): DictImportBundle` routes by type:
- **Directory** — reads `index.json` for meta, all sibling `.json` files as terms.
- **ZIP** — streams every `*.json` entry (`ZipInputStream`); `index.json` provides
  name/revision/format; other JSON files are term documents.
- **Lone `index.json`** — meta from it, sibling JSONs as terms.
- **Single `.json`** — treated as a JMdict-style term document (`id = custom-<millis>`).
- Anything else → `IllegalArgumentException("Unsupported dictionary file: …")`.

`parseIndexMeta(bytes)` extracts title/name, revision, author (`author` + optional
`authoring`), and maps the `format` field: `kanjidic2`/`kanjidic` → KanjiDic,
`frequency` → Frequency, `pitch accent`/`pitch_accent` → PitchAccent, `grammar` →
Grammar, `names` → Name, `jmdict` → JmDict, else Yomitan.

`parseTerms(text)` accepts:
- **Yomitan term tuple arrays** — `["spelling", "reading", [defs], [tags], [freq]]`.
  Reading may be a string or object `{reading, tags}`; each def object yields senses from
  `pos`/`glossary` (fallback `gloss`) and pitch from a `pitch` array
  (`{position, downstep}`). Entry source is derived (`Name` if kanji spelling + `name`
  tag or blank reading, else `Vocabulary`); search keys = spelling + `kanaKeys(reading)` +
  lowercase spelling.
- **Object form** — `{headword|kanji|word, reading, readings:[{reading, readingInformation,
  pitchAccents}], senses:[{partOfSpeech, glosses|gloss, tags}]}`.
- Root can be a bare array, or an object with a `terms`/`entries` array (falls back to
  treating the object as one term).

`DictImportBundle(result: DictImportResult, entries)` — result carries id, name,
revision, detected format, entry count. JSON parsing is `runCatching` — a malformed file
yields zero entries, which `importFile` reports as an actionable error instead of a crash.

## 7. Deinflect (`Deinflect.kt`)

Compact conjugate-rule table (~90 rules) covering common verb/adjective inflections and
politeness forms. **BFS**: each rule that matches a suffix produces a shorter headword
(`dropLast(suffix.length) + replace`), queued until no rule matches; results sorted
shortest-first with `DeinflectionResult(word, ruleName, reason)`. Examples: 行って → 行く,
大きかった → 大きい, ませんでした → (base), 食べた → 食べる. `distinctBy { it.suffix }` keeps the
table canonical.

## 8. JapaneseText utilities

Dependency-free kana/romaji/kanji handling:
- `hasKana` / `isKana` / `isRomaji` / `isKanjiChar` (CJK 0x4E00–0x9FFF) / `isKanji`.
- `toHiragana` / `toKatakana` (character mapping ぁ–ゖ ↔ ァ–ヶ).
- `romajiToHiragana` — greedy longest-match table (yōon `ky`→きゃ, geminate `kka`→っか,
  `shi/chi/tsu`, `n/m`→ん); non-romaji input passes through; falls back to
  identity-copying for unmatched chars; `ー` preserved.
- `kanaKeys(text)` — {text, hiragana, katakana} deduped — the standard probe-key set.

## 9. UI surfaces

- **`DictionaryLookupCard`** — search across all enabled dictionaries; grouped results;
  hovering a result opens the popup at the mouse position.
- **`DictionaryPopup`** — native popup lookup (Reading/Browser/Media/OCR workflows):
  headword, readings, definitions, example, tags (JLPT, radicals), pronunciation (TTS),
  and actions: Create card (mining), Edit card, tags/flags, suspend/bookmark, copy
  (headword/reading/definition), open full dictionary (→ manager view).
- **Dictionary manager** (suite view) — install/enable/prioritize/remove dictionaries.
- **Bundled lookup (core app)** — kanji/vocab detail pages read `AppDataDatabase`
  directly (ships with the app; offline-first §182).
- **Segmenter feed** — `allEntries()` feeds the Japanese segmenter for text analysis.

## 10. Error model

- Unsupported path/format → `IllegalArgumentException` caught by `importFile`'s
  `runCatching` and surfaced as `Result.failure` with a user-readable message (§296).
- Zero parsed entries → explicit "No entries could be parsed… check it is a Yomitan-
  compatible export" (§219 — never a silent success).
- Corrupt `installed.json` / index files / history / favorites → `runCatching` reset to
  empty state; the app keeps working.
- No dictionaries / all disabled → honest empty state (§297).

## 11. Tests

- Suite JVM tests cover import parsing (tuple/object/ZIP), search modes
  (exact/prefix/kana/deinflect), scoring, dedupe, and repository install/remove/reorder
  (`desktopApp/src/jvmTest/.../dictionary/`).
- Gap: production-scale dataset tests (§279), malformed-input fuzzing (§280),
  romaji→kana table coverage, and deinflection regression fixtures.

## 12. Performance notes

- Index is **prebuilt per dictionary** and rebuilt only on mutation — queries hit the
  `keyIndex` map, not per-row scans.
- Prefix search caps expansion at 80 keys; dedupe uses a linked map so worst-case result
  size is bounded by candidate count.
- Future (§186–§187): FTS/trigram indexing, cached results, ranking beyond the 5-tier
  score.

## 13. Open items

- Dictionary manager, popup-in-shipped-app, and reading/browser surfaces are suite-only
  until the consolidation decision (audit §7-1).
- `demoKanji` seed is a bundled data file — confirm its provenance/license is covered in
  `docs/data/SOURCES.md`.
- Pitch-accent surfaces beyond dictionary entries (pitch diagrams on roadmap).

## 14. Node-layer integration (TARGET — ADR-0013, NODE §81–§83)

Dictionary entries become the **node anchors** of the whole product (§81).

### 14.1 Provenance & identity

- Every parsed entry maps to language nodes via `(source, source_id)`: JMdict entries →
  `source='jmdict'` + JMdict sequence; KANJIDIC → `source='kanjidic'`; imported
  Yomitan dictionaries → their own `source` names + entry ids. Idempotent import
  (NODE §78, NODE_DATA_MODEL §2.1).
- `DictionaryService.lookup` results resolve to node anchors; the same word looked up in
  two dictionaries converges on one canonical node (mapping policy in ADR-0013).

### 14.2 Traversal chips (§81)

The popup/lookup card gains related-node traversal backed by the knowledge graph:

- kanji → `contains_character` → words (ranked by `has_frequency`)
- word → `conjugates_to` → forms
- word/kanji → `appears_in_sentence` → example sentences (dataset-gated)
- word/kanji → `appears_in_media` → subtitle lines / scenes
- word/kanji → `represents` ← Journey objects
- word/kanji → `mastered_by` → user knowledge (dimension dials)

### 14.3 "Where have I seen this?" (§83)

`KnowledgeGraphService.whereHaveISeen` merges `appears_in_media`, `encountered_by`,
`mined_from`, and review history edges for the word, grouped by world (Media / Journey /
Study) with provenance + jump actions. This is the flagship personalization query and
must hit the §2 latency budget at full scale.

### 14.4 Lookup as evidence

`dictionary_lookup` events feed `ENCOUNTERED` transitions (KNOWLEDGE_STATE_MODEL §3) and
suggestion ranking — passive exposure counts, but is clearly weaker than review success
as evidence (§84 triggers).

### 14.5 Acceptance criteria

- Every entry reachable from every surface that mentions it (§82–§83).
- Traversal chips appear on popup + lookup card within latency budget.
- Import remains idempotent and provenance-complete (STANDARDS §184–§185).
