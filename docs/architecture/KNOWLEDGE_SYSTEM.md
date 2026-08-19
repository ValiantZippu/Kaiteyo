# Kaiteyo Knowledge System

## Overview

The knowledge system is the heart of Kaiteyo. It provides a unified, deeply connected data model for kanji, words, sentences, grammar, radicals, and components. Every entity has relationships; the user can explore naturally through those relationships.

## Entity Model

### Kanji

The kanji entity answers all five product questions:

| Question | Answered by |
|---|---|
| WHAT IS THIS? | `meanings`, `onReadings`, `kunReadings`, `classifications` |
| HOW IS IT BUILT? | `radical`, `components`, `strokeCount`, `strokePaths` |
| HOW IS IT USED? | `wordsContaining()`, `sentencesWithText()` |
| WHAT IS IT CONNECTED TO? | `kanjiRelatedByRadical()`, `componentsIn()` |
| HOW SHOULD I LEARN IT? | `StudyState`, `LearnerProfile`, card presets |

Key model: `KanjiKnowledge`
```kotlin
data class KanjiKnowledge(
    val character: String,
    val meanings: List<String>,
    val onReadings: List<String>,
    val kunReadings: List<String>,
    val frequencyRank: Int?,
    val classifications: List<KanjiTag>,  // JLPT, Grade, WaniKani
    val strokeCount: Int?,
    val variantFamily: String?,
    val strokePaths: List<String>
)
```

### Radical

Radicals are first-class entities, not decorative tags.

```kotlin
data class RadicalKnowledge(val radical: String, val strokeCount: Int)
data class RadicalStats(val radical: String, val strokeCount: Int, val kanjiCount: Int)
data class RadicalInKanji(val radical: String, val startStroke: Int, val strokesCount: Int)
```

### Component

Components differ from Kangxi radicals. The bundled dataset uses radical-derived decomposition.

```kotlin
data class ComponentKnowledge(
    val component: String,
    val radicalOf: String?,
    val strokesCount: Int,
    val source: ComponentSource  // RadicalDecomposition
)
```

### Word

Words are first-class JMdict-derived entities.

```kotlin
data class WordKnowledge(
    val id: Long,
    val kanjiReading: String?,
    val kanaReading: String,
    val furigana: FuriganaString?,
    val glossary: List<String>,
    val partOfSpeech: List<String>
)
```

### Sentence

Corpus sentences with translation and furigana.

```kotlin
data class SentenceKnowledge(
    val text: String,
    val translation: String,
    val furigana: FuriganaString
)
```

### Grammar

Grammar patterns from the built-in reference catalog.

```kotlin
data class GrammarPattern(
    val id: String,
    val pattern: String,  // "〜てみる"
    val meaning: String,
    val formation: String?,
    val register: String?,
    val jlpt: Int?,
    val keywords: List<String>
)
```

## Relationships

The knowledge graph uses typed edges:

| Edge Type | From → To | Example |
|---|---|---|
| Contains | word → kanji | 食べる → 食 |
| ComponentOf | kanji → component | 食 → 人 |
| RadicalOf | radical → kanji | 食 → 食 |
| UsedIn | kanji → word | 食 → 食べる |
| AppearsIn | word → sentence | 食べる → 今日は何を食べますか？ |
| ExampleOf | sentence → grammar | 〜てみる pattern match |
| RelatedTo | kanji → kanji | shared radical relationship |

## Knowledge Graph

### Progressive Expansion

The graph never materializes the whole dictionary. It starts with one root node and expands one ring at a time:

1. **Root**: Open a kanji → only the kanji node exists
2. **First expansion**: Show radicals, top vocabulary, related kanji
3. **Second expansion**: Show sentences, grammar, deeper relationships
4. **Progressive**: Each expansion adds 12 nodes max

### Limits

- `MAX_NODES_PER_EXPANSION = 12`
- `MAX_TOTAL_NODES = 120`
- `MAX_DEPTH = 4`
- `MAX_EDGES = 200`

### Canvas

The `KnowledgeGraphCanvas` composable provides:
- **Pan**: drag to move the view
- **Zoom**: scroll wheel or pinch
- **Select**: tap to select a node
- **Expand**: tap selected node again to expand neighbors
- **Controls**: zoom in/out, reset, expand buttons
- **Legend**: color-coded edge types

### Node Colors

- Kanji → accent primary
- Radical → teal
- Word → blue
- Sentence → green
- Grammar → orange

### Edge Colors

- Contains → light blue
- ComponentOf → green
- RadicalOf → light green
- UsedIn → amber
- AppearsIn → purple
- ExampleOf → pink
- RelatedTo → accent primary

## Search

### Kanji Search

In-memory index built from bulk database queries (~2k jōyō characters). Scanning per keystroke is cheap.

Match types:
- **Character**: exact kanji match (highest priority)
- **Reading**: on/kun reading contains query
- **Meaning**: English meaning contains query (case-insensitive)

### Word Search

Database-backed with LIMIT. Returns `WordKnowledge` objects.

### Sentence Search

Database-backed with LIMIT. Returns `SentenceKnowledge` objects.

### Grammar Search

Built-in catalog substring search. Returns matching `GrammarPattern` objects.

## Sentence Analysis

### Tokenization

Two layers, one public API (`SentenceAnalyzer.analyze`):

1. **`WordSegmenter`** (primary, dictionary-driven) — longest real-word match
   per position against the bundled dictionary, so 日本語 resolves to one
   token backed by the real word, 食べる stays one token, and unmatched
   spans fall back to character-class runs. Lookups are DB-backed and
   memoized per call.
2. **`SentenceTokenizer`** (fallback, pure) — character-class runs:
   - Kanji run → Kanji token
   - Kana run → Kana token
   - Mixed (食べる) → Mixed token
   - Punctuation → Punctuation token
   - Latin → Latin token

The segmenter is an honest approximation of morphology — a real MeCab/UniDic
parse remains the roadmap item `KT-SENT-002`. Tokens that match nothing are
left unlinked; nothing is fabricated.

### Annotation

`SentenceAnalyzer` resolves tokens against the knowledge repository:
- Kanji tokens → kanji entry (single char) or word lookup (compound)
- Mixed tokens → word entry + per-character kanji
- Kana tokens → word lookup (2+ chars)

### Difficulty Scoring

`SentenceDifficultyScorer` computes 1..10 level from:
- Length (≤6 = easy, >30 = hard)
- Kanji density (0% = easy, >35% = hard)
- Grammar pattern count
- Unknown kanji count (optional overlay)

Profiles filter sentences by max acceptable difficulty.

## Study State

### State Machine

```
New → Learning → Known → Due → Mastered
                     ↓          ↓
                Relearning  Relearning
                     ↓
                  Suspended
```

### Projection

`StudyStateMachine.project()` maps real FSRS cards to UI states:
- `New`: never studied
- `Learning`: in learning steps
- `Known`: review card, not yet due
- `Due`: interval elapsed
- `Mastered`: 21+ day interval
- `Relearning`: lapsed card
- `Suspended`: user-suspended (persistent intent)

### Study Gate

`StudyGate` bridges SRS internals to the knowledge system:
- `stateFor(card, now)` → StudyState
- `cardKeysForKanji(character)` → SRS card keys
- `cardKeyForWord(wordId)` → SRS card key
- `isKnown(state)` → boolean (Known or Mastered)

## Level Profiles

9 profiles control presentation depth. `LevelAdapter` (`core/knowledge/level/LevelAdapter.kt`)
applies a profile to concrete entities without mutating source data:
- `adaptedGlossary(word, presentation)` — limits senses by explanation depth
  (Simple → 1 sense, Clear → 3, Technical/JapaneseOnly → all)
- `adaptedSentences(sentences, presentation, limit)` — filters by the
  profile's difficulty bound, then caps
- `showFurigana / showRomaji / showTranslations` — presentation flags
- `effectivePresentation(profile, overrides)` — Custom overrides honored
  only for Custom; every other profile uses catalog defaults

Word pages render profile-adapted glossaries and example sentences
(`WordEntryScreen`); kanji pages filter example sentences by the same bound.

| Profile | Furigana | Romaji | Translations | Rare | Depth | Difficulty | Cards |
|---|---|---|---|---|---|---|---|
| ChildBeginner | ✓ | ✗ | ✓ | ✗ | Simple | Easy | Beginner |
| AbsoluteBeginner | ✓ | ✓ | ✓ | ✗ | Simple | Easy | Beginner |
| Beginner | ✓ | ✓ | ✓ | ✗ | Clear | Easy | Beginner |
| LowerIntermediate | ✓ | ✗ | ✓ | ✗ | Clear | Easy | Standard |
| Intermediate | ✓ | ✗ | ✓ | ✗ | Clear | Mixed | Standard |
| UpperIntermediate | ✗ | ✗ | ✓ | ✗ | Technical | Mixed | Advanced |
| Advanced | ✗ | ✗ | ✓ | ✓ | Technical | Hard | Advanced |
| Native | ✗ | ✗ | ✗ | ✓ | JapaneseOnly | Hard | Research |
| Research | ✓ | ✗ | ✓ | ✓ | Technical | Hard | Research |

## Card System

### Kanji Cards (13 types)

Hero, Meaning, Readings, Frequency, Classification, Radical, Component, Stroke, Vocabulary, Sentence, Grammar, Graph, Study

### Word Cards (9 types)

Hero, Readings, Meanings, PartOfSpeech, Kanji, Frequency, Sentences, Grammar, Study

### Presets

- **Minimal**: Core info only
- **Beginner**: Meaning, readings, vocabulary
- **Standard**: Full frequency, classification
- **Advanced**: Components, grammar, graph
- **Research**: Everything visible

### Persistence

Layouts are serialized as JSON and stored in `PreferencesContract.AppPreferences`. Corrupt or stale blobs fall back to defaults.

## Performance

### In-Memory Index

Kanji search uses an in-memory index (~2k characters). Building it on first use; cached thereafter.

### Debouncing

Search queries are debounced (280ms default). Old queries are cancelled.

### Lazy Pagination

`LazyPager<T>` provides infinite-scroll pagination with configurable page size.

### Caching

- `SearchResultCache<T>` with 30s TTL
- `LruCache<K, V>` for expensive computations (256 entries)

### Graph Limits

Progressive expansion prevents rendering thousands of nodes:
- 12 nodes per expansion

## Home Command Center (spec §31)

Home answers "what should I do now?" — `core/knowledge/home/HomeCommandCenter.kt`
persists real usage as JSON in app preferences (`HomeCommandCenterStore`):
- **Recent searches** — recorded by the universal-search controller when a
  result is opened (`commitSearch`), deduped, newest first, capped at 8
- **Recent entries** — kanji/word pages record their own visits, deduped by
  kind+ref, capped at 10
- Corrupt/stale blobs fall back to empty state (mirrors `KanjiCardLayoutStore`)

The General dashboard renders `HomeCommandCenterSection` (Quick search,
Recent searches, Recent entries, Discover) — every row navigates or searches;
nothing is decorative. Tests: `HomeCommandCenterStoreTest`.

## Browse Collections (spec §30)

`LibraryCatalog` builds JLPT/grade collections from real classification
queries. `MainDestination.CollectionDetail` is the drill-down: a named
collection with real counts, filters and paginated entries that deep-link
into kanji/word entries — collections are navigation surfaces, not static
lists. `BrowseHub` collection rows open this screen with real filters.

## Level-Adaptive Browse (spec §23)

Browse surfaces adapt to the learner profile without hiding data:
- `LevelAdapter.recommendedJlpt(profile)` returns the JLPT band a profile
  should focus on (ChildBeginner/Beginner → N5, … Advanced → N1–N2,
  Native/Research/Custom → unrestricted)
- **KanjiBrowser** shows a "For your level (N5)" chip that applies the band
  as a real JLPT filter; **BrowseHub** has a "For your level" section that
  deep-links into the pre-filtered browser
- **Universal search** orders sentence results by estimated difficulty for
  non-Hard profiles (easy first) — results are never deleted, only presented
  in profile order

## Search Input Modes (spec §16)

- **Clipboard** — "Paste clipboard" reads the system clipboard through the
  Compose `ClipboardManager` (cross-platform, real)
- **Image OCR** — `SearchOcrProvider` is the platform seam: desktop
  registers `DesktopSearchOcrProvider` (backed by the suite's `OcrEngine`,
  Tesseract) in `desktopAppModule`; on Android/iOS no provider is registered
  and the control is hidden — never a dead button
- **Voice / handwriting** — no STT/recognizer exists yet; documented as
  roadmap (`KT-SEARCH-003`) rather than faked

## Media Connections (spec §28)

`core/knowledge/media/MediaReference.kt` — a `MediaReference` (kind, media
title, Japanese text, timestamp) recorded when the desktop Media Centre
bookmarks a Japanese subtitle cue (`MediaEngine.onBookmarkCreated` →
`MediaReferenceStore`, wired in `MediaCentreDesktopHost`). Word entries show
"Found in your media" rows (text + title + mm:ss timestamp). Nothing is
fabricated — the card is absent until media has been touched.

## Word Cards (spec §20–§21)

`WordCardModels.kt` mirrors the kanji card system: `WordCardType` registry
(now 10 types incl. `Media`), `WordCardLayout` (order + hidden, sanitized),
`WordCardPresets` (Minimal/Beginner/Standard/Advanced/Research) and
`WordCardLayoutStore` (JSON persistence, corrupt blobs → defaults).
`WordEntryScreen` renders in the saved order with a Customize dialog
(show/hide + presets). The Frequency card stays in the registry but renders
nothing — there is no word-level data source yet, so it is never a fake card.
New registry types are appended at render time (`visibleCards()`) so a card
added in a new build shows for users with saved layouts.

## Study Cards (spec §15)

`StudyStatusProvider` (`core/knowledge/StudyStatusProvider.kt`) bridges
knowledge pages to the real SRS cards: given a kanji it loads the writing +
reading cards, given a word the flashcard card, and projects each through
`StudyGate` onto `StudyState`. A missing card is `New` + "not started" —
never fabricated. The kanji Study card (previously a static legend of all
possible states — decoration) now shows the actual per-practice state; the
word Study card does the same for flashcard state. Tests:
`StudyStatusProviderTest`.

## Query Interpretation (KT-SEARCH-002, spec §15)

`KnowledgeQueryParser` (pure, tested) turns plain text into a real
`SearchFilters` intersection without a query language:
- `N3` / `jlpt:n2` → JLPT level
- `grade:2` → school grade
- `common` / `frequency:rare` / `verycommon` → frequency band
- `strokes:4` / `7 strokes` → stroke count
- `verb` / `nouns` / `い-adjective` → part of speech (plural stem
  normalized so it matches real POS tags)
- unknown tokens stay in the free text — nothing is dropped

Recognized tokens surface as read-only chips in the universal search and
the parsed filters feed `engine.search()` directly.

## Media Graph Nodes (spec §28)

`KnowledgeNodeKind.Media` is a first-class graph node: expanding a word
adds `AppearsIn` edges to media-reference nodes (stable ids from
title+timestamp+text) whose recorded Japanese text matches the word. The
graph repository takes an optional `MediaReferenceStore` (registered); on
platforms without media the contribution is simply empty. Kanji pages also
gain a Media card matching the kanji's readings against the reference store.

## Text Normalization (KT-SEARCH-005, spec §15, §136–§140)

`JapaneseTextNormalizer` is a pure, deterministic module so search treats
食べる / たべる / タベル / TABERU / ｔａｂｅｒｕ as the same intent:

- **Width + case folding** — full-width ASCII/latin/digits → half-width,
  half-width katakana (ｶﾞ) → full-width, ASCII → lowercase; the
  prolonged-sound mark ー is dropped (a reading hint, not a morpheme).
- **Script folding** — katakana → hiragana for matching. Kanji is never
  touched by normalization.
- **Kana → romaji** — via the bundled reading table (し → shi); kanji is
  preserved, not transliterated. Honest limit: っ maps to its base reading
  "tsu" (sakka → satsuka), geminates are not collapsed in this direction.
- **Romaji → kana** — best-effort kana-table conversion for query intent
  (ta → た, shi → し, kya → きゃ, sokuon kka → っか). Honest limits:
  doubled-vowel length (too) is not inferred; unknown ASCII passes through.
- **Wildcards** — `*` matches any run, `?` exactly one character, both
  sides normalized before matching (`WildcardPattern`, classic backtracking
  match). Wildcards are opt-in: a literal `*` is a wildcard.
- **One-stop matcher** — `queryMatches(query, candidate)` routes to the
  wildcard path when the query contains a wildcard, else normalized
  containment.

Wired into search: the kanji index matches readings against the normalized
query AND its romaji→kana conversion (so "taberu" finds たべ), and the
DB word/sentence lookup folds width/script and converts romaji to kana so
"タベル" and "taberu" reach the same rows. Tests:
`JapaneseTextNormalizerTest` (width/script/romaji/wildcard/matcher).

## Word & Sentence Sorting (KT-SEARCH-004, spec §19)

`SearchSort` gained word/sentence-relevant options and every option changes
the real result order:

- **Kanji** — relevance / frequency / strokes / JLPT / grade / A–Z /
  reading (index-side, existing); `Difficulty` falls back to frequency
  rank (no kanji meaning).
- **Words** — A–Z (spelling), Reading (kana), JLPT, and stable fallbacks;
  applied after POS/JLPT filtering on the fetched window.
- **Sentences** — `Difficulty` sorts by estimated sentence difficulty
  (`SentenceDifficultyScorer`, easiest first); Reading sorts by text.

Universal search now shows a compact **Sort row** above the results; each
selection re-runs the real query with the new sort (a replay-1 shared flow,
so re-selecting the same text re-executes — StateFlow would have deduped it).

## Graph Branch Collapse (KT-GRAPH-002, spec §9–§10)

`KnowledgeGraph.collapsed(collapsedIds, pinnedId)` is a pure view transform:
collapsing a node hides its neighbors (branch collapse / clustering), keeps
the collapsed node itself, and records a real `+N hidden` count in its
`extra` map. The root and the pinned (selected) node are never hidden —
collapsing never strands the node being inspected. `KnowledgeGraphCanvas`
renders the collapsed graph, shows the `+N` badge on cluster chips, and
offers a Collapse control (canvas controls + per-chip) wired from the
KnowledgeGraph screen with local `collapsedIds` state; expanding a node
clears its collapse. Tests: `KnowledgeGraphTest` (hides neighbors, records
count, pins selected/root, identity for empty set).

## Keyword System (KT-DATA-002, spec §13)

`KanjiKeywordSet` models keywords as orientation tools, never definitions:
primary (first meaning), alternates, learner meaning, literal meaning and
component keyword — every field nullable (unknown = unavailable, never
guessed). `KeywordRegistry` is a central store the data layer populates;
when only a dictionary meaning exists it falls back to a single-keyword
set built from that real meaning. Tests: `KnowledgeMetadataTest`.

## Dataset Provenance (KT-DATA-003, spec §46)

`DatasetProvenance` records honest metadata per dataset: id, name, upstream
version, SPDX license, source URL, record counts per entity kind, import
date and checksum — a dataset without a recorded license says `null`, it
never inherits one. `DatasetProvenanceRegistry` answers "where does this
data come from?" from one place. Both registries are registered in Koin.
Tests: `KnowledgeMetadataTest`.

## Global Shortcuts (KT-SEARCH-008, spec §58)

`NavShell`'s global key handler now binds the shortcuts the UI advertised
but never wired:

- **Ctrl+K** — command palette (previously shown as "Ctrl+K to open" with
  no binding)
- **Ctrl+Shift+F** — universal search
- **Ctrl+B** — floating ↔ sidebar mode (existing)

No advertised shortcut is left dead.

## Graph Navigation Trail (KT-GRAPH-004, spec §75)

`GraphTrail` is a pure history model for the knowledge graph: the graph is
navigation, so the path the user walked is visible as **breadcrumbs** and
reversible with **back/forward**. Standard history semantics:

- `push(node)` appends and moves focus, discarding any stale forward
  entries (navigating to a new node invalidates the redo path);
- `back()` / `forward()` move the position and return the node to focus;
- `breadcrumbs()` is the trail from the root through the current position.

The standalone `KnowledgeGraphScreen` now shows a breadcrumb row (root →
… → current, each step clickable to jump back) plus back/forward buttons;
`KnowledgeGraphViewModel` records the trail on every expansion and exposes
`goBack`/`goForward`. Tests: `GraphTrailTest`.

## Study-State Colors (KT-THEME-003)

`studyStateColor` (`common/ui/knowledge/KnowledgeStudyColors.kt`) is the
single shared mapping from `StudyState` → color, consumed by both the kanji
and word entry pages (they previously duplicated the identical map). The
palette is semantic and stable across themes by design — like edge colors in
the graph — but it lives in exactly one place so it can never drift.

## Node Layer as Code (ADR-0013, spec §76–§83, §149)

ADR-0013's node architecture exists in two forms: the **docs** (master spec +
registries in `docs/architecture/nodes/`) and the **code** — a new
`core/knowledge/nodes/` package that makes the vocabulary executable:

- `NodeTypeRegistry.kt` — the full `NodeType` enum (every nodeType from
  `NODE_TYPE_REGISTRY.md`: family, parent, CURRENT/TARGET status). Adding a
  type is a registry change, never an inline string.
- `RelationshipRegistry.kt` — the `RelationshipType` enum (edge vocabulary
  with source/target node-type constraints; `related_to` is marked
  `isEscapeHatch = true` and is the only unbounded edge).
- `NodeContract.kt` — the universal node contract (§78): `NodeId` (typed,
  registry-validated, `"type:ref"` grammar), `Node` (id, source/sourceId,
  schemaVersion, timestamps, parent/owner/world, tags), `NodeEdge` (typed
  edge) with `validate()` against the registry, plus the **two-graph bridge
  lint** (§149): a World↔Language edge is illegal unless its type is in
  `BRIDGE_EDGE_TYPES` (`represents`, `encountered_by`, `discovered_by`,
  `mined_from`, `appears_in_media`, `appears_in_scene`, `teaches`).
- `NodeRegistryFacade.kt` — the single Koin-injectable handle.

Storage stays with the existing databases (read-model decision, ADR-0013
Accepted); the code layer is vocabulary + validation only. Tests:
`NodeRegistryTest` (14 cases).

## Per-Card Settings (KT-CARD-005, spec §21)

Card customization goes beyond show/hide/reorder: content cards have
**per-card settings**. `KanjiCardLayout.cardSettings` is a persisted
`Map<String, Int>`; `exampleLimit(type, default)` resolves a missing key to
the card's default (values are clamped to ≥1 — a hidden card is how you
disable a section). In the kanji page's edit mode each content card
(Vocabulary / Related / Sentences) shows a compact item-limit stepper; the
ViewModel loads up to the maximum so raising the limit shows more items
immediately, and the cards slice by the configured value. Tests:
`KanjiCardModelsTest`.

## Sentences in the Library (KT-UI-004, spec §29)

Sentences are first-class Library content: a **Sentences** mode
(`SentencesBrowse.kt`) searches the real bundled corpus (debounced, with a
difficulty estimate per hit and tap-through to the interactive
`SentenceEntry`), and the unified Library search now returns SENTENCES
alongside decks / kanji / vocabulary with the same keyboard navigation.

## Heatmap Keyboard Parity (KT-UI-006, spec §17)

A focused (keyboard-navigated) heatmap day now drives the same live tooltip
as a hovered one — focus loss clears it — so keyboard users get the same
day summary as pointer users.

## Romaji Override in Settings (KT-LEVEL-004, spec §24)

The per-user romaji override existed in data (`DisplayOverridesStore`) but
nothing in Settings wrote it. Settings → Appearance now has a real
**"Show romaji on dictionary pages"** toggle: on persists `override=true`,
off clears the override (following the learner profile again). The
underlying data is never changed — only the display.

## Study-Aware Search (KT-SEARCH-004/006, KT-SRS-001, spec §15, §18–§19)

Search now consumes the user's REAL SRS state, not guesses:

- **`StudyOverlay`** (`core/knowledge/StudyOverlay.kt`) — a per-query
  snapshot of every kanji's study state, built once from
  `SrsCardRepository.getAll()` by `StudyOverlayBuilder`. A kanji with no
  card at all is New (never fabricated as studied); a kanji studied in
  several practices reports its most-advanced practice; "added" honestly
  means the earliest real review (the FsrsCard model records no separate
  creation date).
- **Study-based sorts** — `SearchSort.RecentlyStudied` / `RecentlyAdded`
  order kanji by the real card timestamps; never-studied kanji sort last
  (null timestamps never fake a date).
- **Study-state filter** — `SearchFilters.studyState` + plain-text keywords
  (`known`, `mastered`, `learning`, `new`, `due`, `review`, `relearning`,
  `suspended`): "N3 due" is a real intersection query, evaluated against
  the overlay in the kanji index.

Tests: `StudyOverlayTest`, `KnowledgeQueryParserTest`.

## Explainable Recommendations (KT-LEVEL-003, spec §176–§177)

`StudyRecommendationEngine` answers "why should I study this kanji next?"
with REAL data and an EXPLAINED reason: due/relearning kanji rank first
("Review due · top-500 frequency"), learning kanji next ("In progress —
keep it fresh"), then frequency-ranked new kanji. The engine is pure and
tested (`StudyRecommendationEngineTest`); it never invents study state.

## Courses as First-Class Library Content (KT-UI-004, spec §29, §53)

The `LibraryCatalog` course model existed but was never surfaced. The
Library now has a **Courses** mode (`CoursesBrowse.kt`): the real courses
(Kyōiku Grades 1–6, JLPT N5→N1 — every collection a real dataset query)
list with their actual lesson/item counts, drill into lessons, and open
`CollectionDetail`. Nothing is fabricated — item counts come from the
catalog.

## Media Node Family (ADR-0013, todo #118/#119, spec §28, §149)

`MediaNodeBridge` maps REAL media references onto the typed node layer:
one `Series` node per media title, one `SubtitleLine` node per reference,
`contains` + `appears_in_media` edges, and a `mined` provenance tag on
lines mined from. Honest limits: a `mined_from` edge needs a card id that
`MediaReference` does not carry, so it is omitted rather than fabricated;
multi-character text is never guessed into a word node id. Tested in
`MediaNodeBridgeTest`.

## Subtitle Search (todo #117, spec §28)

The universal search shows a real **MEDIA** section from
`MediaReferenceStore.matching()` — references whose recorded Japanese text
contains the query, with title/kind/timestamp, tap → Media. Queried in
parallel with the knowledge engine; failures never kill search.

## Session-Level Statistics Export (todo #140)

`StatisticsExporter` (`core/statistics/StatisticsExport.kt`) serializes the
REAL session log + daily aggregates to CSV or JSON (comma-safe quoting,
date-ordered). Wired into the Statistics Data tab as **Sessions CSV /
Sessions JSON**, complementing the existing aggregate export. Tested in
`StatisticsExporterTest`.

## Home Recommendations (spec §31, §176–§177)

The General Dashboard's **Recommended next** section is fed by
`StudyRecommendationEngine` over REAL data: the letter-deck kanji (the
actual study pool), each projected to its FSRS study state via
`StudyOverlayBuilder` (`SrsCardRepository.getAll()`), with corpus frequency
from `KnowledgeRepository.kanjiFrequencyRanks()`. Priority: due/relearning
first, learning next, then frequency-ranked new kanji — every pick carries
a human-readable reason. A kanji with no card projects to New; nothing is
invented. Tapping a pick opens the kanji entry.

## Mined-from Edge (ADR-0013, §149)

`MediaReference.cardId` now carries the REAL desktop card id when a subtitle
cue is mined (`MediaEngine.onMined` → `MediaCentreDesktopHost` records a
`Mined` reference with it). `MediaNodeBridge` emits a
`Card → mined_from → SubtitleLine` edge for such references; a Mined
reference without a card id stays tag-only (`mined` provenance on the
line) — the id is never fabricated.

## Lesson → SRS Study Path (spec §29)

A library collection (course lesson) is one tap from becoming a real study
surface: **Study as deck** on the Collection detail finds-or-creates a
letter deck from the collection's kanji (`Lesson: {title}`, idempotent —
re-studying never duplicates) and navigates to its deck details, where the
normal SRS practice flow takes over.

## Library Media Mode (spec §28–§29)

Library now has a **Media** mode: every recorded media reference (bookmark
or mined subtitle cue) is real library content — the Japanese text, source
title, and kind (Bookmark / Mined / Cue). Tapping a single-kanji reference
opens the kanji entry; multi-character text opens the interactive sentence
view with no invented translation.

## Filtered Word Search Window (KT-SEARCH-010)

When POS/JLPT filters are active the engine fetches a 300-row word window
(up from 24) and filters in Kotlin — meaningfully closer to full-set than
before. Full DB-side filtering requires new SQLDelight queries and is a
roadmap item; the window size is documented in the engine kdoc, not hidden.
