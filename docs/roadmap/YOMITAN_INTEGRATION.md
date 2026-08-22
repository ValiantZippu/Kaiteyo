# Kaiteyo — Yomitan & Dictionary Integration Roadmap

> Kaiteyo's native dictionary system replaces Yomitan/ASBPlayer with an integrated
> learning ecosystem. This document covers the full dictionary architecture, Yomitan
> format compatibility, and the roadmap to a complete native reading experience.

---

## 1. Current State

### 1.1 What's Implemented

| Component | Status | Location |
|-----------|--------|----------|
| DictionaryService | ✅ Complete | `desktop/engine/dictionary/DictionaryService.kt` |
| DictionaryRepository | ✅ Complete | `desktop/engine/dictionary/DictionaryRepository.kt` |
| DictionaryImporter | ✅ Complete | `desktop/engine/dictionary/DictionaryImporter.kt` |
| DictionaryPopup | ✅ Complete | `desktop/ui/dictionary/DictionaryPopup.kt` |
| DictionaryLookupCard | ✅ Complete | `desktop/ui/dictionary/DictionaryLookupCard.kt` |
| MiningEngine | ✅ Complete | `desktop/engine/mining/MiningEngine.kt` |
| DictionaryModels | ✅ Complete | `desktop/engine/dictionary/DictionaryModels.kt` |
| Bundled JMdict | ✅ Complete | `core/app_data/` (AppDataDatabase) |
| Yomitan import (ZIP) | ✅ Complete | `DictionaryImporter.parseZip()` + `YomitanImporter` v2 (structured content, frequency bands, pitch accent, kanji metadata, tag banks) |
| Yomitan import (folder) | ✅ Complete | `DictionaryImporter.parseFolder()` + `YomitanImporter` v2 |
| Yomitan import (JSON) | ✅ Complete | `DictionaryImporter.parseSingleJson()` + `YomitanImporter` v2 |
| Dictionary search | ✅ Complete | Fuzzy + exact + prefix + kana + deinflect |
| Search history | ✅ Complete | `history.json` persistence |
| Favorites | ✅ Complete | `favorites.json` persistence |

### 1.2 What's Missing

| Component | Status | Priority |
|-----------|--------|----------|
| Pitch accent display | ✅ Data imported | P1 — data flows through YomitanImporter, display pending |
| Frequency data display | ✅ Data imported | P1 — FrequencyBand enum classifies ranks, display pending |
| JLPT/grade badges on entries | ❌ Not implemented | P1 |
| Radical/component tags | ❌ Not implemented | P1 |
| Sentence examples from corpus | ❌ Not implemented | P1 |
| Dictionary entry HTML rendering | ❌ Not implemented | P2 |
| Handlebars template support | ❌ Not implemented | P2 |
| Multiple dictionary priority | ✅ Complete | DictionaryRepository uses priority-based ordering |
| Dictionary update mechanism | ❌ Not implemented | P2 |
| AnkiConnect live sync | ⚠️ Partial | P1 |

---

## 2. Yomitan Format Specification

### 2.1 Supported Formats

Kaiteyo's `DictionaryImporter` supports these Yomitan-compatible formats:

#### ZIP Archive
```
dictionary.zip
├── index.json          # Required: name, revision, format, version
├── term_bank_1.json    # Term entries (array of arrays)
├── term_bank_2.json    # More terms
├── term_meta_bank_1.json  # Frequency/pitch data
├── kanji_bank_1.json   # Kanji entries
├── kanji_meta_bank_1.json  # Kanji frequency/radical data
└── tag_bank_1.json     # Tag definitions
```

#### Folder Structure
Same as ZIP, extracted to a directory.

#### Single JSON
A single `index.json` or term bank file.

### 2.2 Index Format

```json
{
  "title": "Custom Dictionary",
  "revision": "1.0",
  "format": "yomitan",
  "version": 3,
  "url": "https://example.com",
  "sourceLanguage": "ja",
  "targetLanguage": "en"
}
```

### 2.3 Term Entry Format (Yomitan v3)

Each term bank entry is an array:
```json
[
  "食べる",           // [0] expression
  "たべる",           // [1] reading
  "to eat",           // [2] definition (glossary)
  null,               // [3] definition (alternative)
  null,               // [4] definition tags
  null,               // [5] score/popularity
  1,                  // [6] sequence number
  null,               // [7] term tags
  null,               // [8]Deinflection rule
  null,               // [9] Structured content (Yomitan v2+)
  null,               // [10] Sh义 content
  null,               // [11] Sh义 content
  null                // [12] Data
]
```

### 2.4 Frequency Data

```json
["食べる", 1234, "freq"]        // frequency rank
["食べる", "SR", 1234]          // frequency with source
```

### 2.5 Pitch Accent Data

```json
["食べる", "食べる", 2, "東京", [[3, "LHH"]]]
// [expression, reading, accent_position, dialect, [[position, pattern]]]
```

---

## 3. Native Dictionary Architecture

### 3.1 Service Layer

```
DictionaryService (app-facing controller)
├── DictionaryRepository (data + index)
│   ├── Installed dictionaries
│   ├── Per-dictionary entries
│   └── Search index (per-dictionary JSON files)
├── DictionaryImporter (parse + install)
│   ├── ZIP parser
│   ├── Folder parser
│   └── JSON parser
└── DictionaryPopup (UI)
    ├── Result card
    ├── Action buttons
    └── Mining integration
```

### 3.2 Search Flow

```
User query
  → QueryProcessor (normalize, detect kana/romaji/kanji)
  → SearchMode flags (EXACT, PREFIX, KANA, DEINFLECT)
  → Per-dictionary search (parallel)
  → Score + rank results
  → Group by dictionary name
  → Display in DictionaryLookupCard
  → Hover → DictionaryPopup
```

### 3.3 Mining Flow

```
DictionaryPopup "Create card"
  → MiningEngine.mineFromDictionary()
  → MiningPayload (headword, reading, definition, sentence, source)
  → DesktopCard created
  → Added to AppState card pool
  → SRS scheduling via FSRS-5
  → Card appears in Review
```

---

## 4. Roadmap

### Phase 1: Enhanced Display (P1)

**Goal**: Make dictionary entries as informative as Yomitan

| Task | Description | Effort |
|------|-------------|--------|
| Pitch accent display | Show pitch accent patterns (LHH, LHL, etc.) on popup and detail view | 2 days |
| Frequency bands | Show frequency rank with band color (VeryCommon → Rare) | 1 day |
| JLPT badges | Show JLPT level on kanji/word entries from bundled data | 1 day |
| Grade badges | Show school grade on kanji entries | 0.5 day |
| Radical tags | Show radicals used in kanji decomposition | 1 day |
| Structured content | Parse Yomitan v2+ structured content (tables, links, images) | 3 days |
| Dictionary priority | User-configurable priority order for multi-dictionary results | 1 day |

### Phase 2: Rich Entry View (P1)

**Goal**: Full-featured dictionary detail page

| Task | Description | Effort |
|------|-------------|--------|
| Entry detail screen | Full-screen dictionary entry with all metadata | 3 days |
| Sentence examples | Show example sentences from Tatoeba/corpus for looked-up words | 2 days |
| Related words | Show words sharing the same reading or meaning | 1 day |
| Collocations | Show common word combinations | 2 days |
| Conjugation table | Show all conjugation forms for verbs/adjectives | 2 days |
| Audio pronunciation | TTS + optional bundled audio clips | 1 day |

### Phase 3: Import Enhancements (P2)

**Goal**: Support all Yomitan dictionary features

| Task | Description | Effort |
|------|-------------|--------|
| Handlebars templates | Render custom dictionary card layouts | 5 days |
| Dict CC format | Support Dict.cc format import | 2 days |
| EPWING format | Support EPWING dictionary format | 3 days |
| Custom field mapping | User-configurable field mapping for import | 2 days |
| Dictionary update | Check for dictionary updates, incremental re-import | 3 days |
| Dictionary sharing | Export/import Kaiteyo-formatted dictionaries | 1 day |

### Phase 4: Integration (P2)

**Goal**: Connect dictionary to the full learning ecosystem

| Task | Description | Effort |
|------|-------------|--------|
| AnkiConnect bridge | Live sync with desktop Anki (create/update cards) | 3 days |
| Reading mode integration | Dictionary popup in reading/EPUB view | 2 days |
| Browser integration | Dictionary popup in learning browser | 2 days |
| OCR → Dictionary | OCR results auto-lookup in dictionary | 1 day |
| Clipboard monitoring | Auto-lookup copied Japanese text | 1 day |
| Sentence mining | Mine full sentences with word breakdown | 3 days |

### Phase 5: Advanced (P3)

**Goal**: Best-in-class dictionary experience

| Task | Description | Effort |
|------|-------------|--------|
| Custom dictionaries | User-created dictionaries from CSV/JSON | 3 days |
| Dictionary statistics | Usage stats per dictionary, most-searched words | 2 days |
| Smart suggestions | "Did you mean..." for misspelled queries | 2 days |
| Cross-dictionary linking | Link entries across dictionaries (same word) | 2 days |
| AI-powered definitions | Optional AI-generated definitions for unknown words | 5 days |
| Dictionary plugins | Extensible dictionary format for custom data sources | 5 days |

---

## 5. Yomitan Compatibility Matrix

| Feature | Yomitan | Kaiteyo Current | Kaiteyo Target |
|---------|---------|----------------|----------------|
| Term entries | ✅ | ✅ | ✅ |
| Kanji entries | ✅ | ⚠️ Partial | ✅ |
| Frequency data | ✅ | ❌ | ✅ |
| Pitch accent | ✅ | ❌ | ✅ |
| Structured content | ✅ v2+ | ❌ | ✅ |
| Handlebars templates | ✅ | ❌ | ✅ |
| Custom dictionaries | ✅ | ✅ | ✅ |
| Dictionary priority | ✅ | ⚠️ Partial | ✅ |
| Popup on hover | ✅ | ✅ | ✅ |
| Mining to Anki | ✅ (via AnkiConnect) | ✅ (native) | ✅ |
| Audio playback | ✅ | ✅ (TTS) | ✅ (TTS + bundled) |
| Example sentences | ✅ | ❌ | ✅ |
| Conjugation | ✅ | ❌ | ✅ |
| EPWING support | ❌ | ❌ | ✅ |
| Multiple languages | ✅ | ⚠️ Partial | ✅ |

---

## 6. Design Guidelines

### 6.1 Dictionary Popup

The popup follows the heatmap card pattern:

```
┌─────────────────────────────────────┐
│ 食べる  たべる                ★ ⋮  │  ← Headword + reading + actions
│─────────────────────────────────────│
│ to eat, to have a meal              │  ← Definitions
│ N5 · 動詞 · JLPT                   │  ← Tags
│─────────────────────────────────────│
│ 🎵 頻度: 1,234位 (よく使う)         │  ← Frequency
│ 📊 声調: LHH (東京)                 │  ← Pitch accent
│─────────────────────────────────────│
│ [Create Card] [Copy] [Full Dict]   │  ← Actions
└─────────────────────────────────────┘
```

### 6.2 Tag Display

Tags use the semantic color system:

| Tag Type | Color | Alpha |
|----------|-------|-------|
| JLPT N5 | `sem.reviewGood` | 15% |
| JLPT N4 | `sem.info` | 15% |
| JLPT N3 | `sem.new` | 15% |
| JLPT N2 | `sem.warning` | 15% |
| JLPT N1 | `sem.error` | 15% |
| Part of speech | `accent.primary` | 12% |
| Frequency | `sem.info` | 12% |
| Radical | `sem.new` | 12% |

### 6.3 Search Modes

| Mode | Trigger | Behavior |
|------|---------|----------|
| Exact | Exact match | Only exact headword/reading matches |
| Prefix | Default | Matches start of headword/reading |
| Kana | Kana input | Converts to hiragana/katakana before search |
| Deinflect | Verb/adjective input | Attempts conjugation reversal |
| Wildcard | `*` or `?` | Pattern matching |

---

## 7. Related Documents

- `docs/architecture/dictionary.md` — current dictionary architecture
- `docs/integrations/yomitan_dictionaries/` — Yomitan format details
- `docs/integrations/anki/` — Anki integration
- `docs/features/DESKTOP.md` — desktop suite features
