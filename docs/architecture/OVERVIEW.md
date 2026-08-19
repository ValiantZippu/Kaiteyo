# Kaiteyo Architecture Overview

## Core Philosophy

Kaiteyo is a deeply connected Japanese dictionary, knowledge graph, learning environment, reading system, library, and eventually media/game ecosystem. The architecture answers five questions for every entity:

1. **WHAT IS THIS?** — meaning, readings, classification
2. **HOW IS IT BUILT?** — radical, components, decomposition
3. **HOW IS IT USED?** — vocabulary, compounds, sentences
4. **WHAT IS IT CONNECTED TO?** — related kanji, components, words
5. **HOW SHOULD I LEARN IT?** — stroke order, mnemonic, graph, examples, SRS

## Module Structure

```
core/                    Shared code (KMP)
├── commonMain/          All platforms
│   ├── kotlin/ua/syt0r/kanji/
│   │   ├── core/        Data layer (repositories, models, services)
│   │   ├── di/          Koin modules
│   │   └── presentation/UI (Compose MPP)
│   └── sqldelight_*/    Database schemas + migrations
├── jvmMain/             JVM-specific (desktop)
├── androidMain/         Android-specific
└── iosMain/             iOS-specific

desktopApp/              Thin JVM wrapper + desktop suite
app/                     Android entry point
iosApp/                  iOS entry point
kjd/                     Data platform (ingests datasets → DB)
```

## Knowledge System

### Domain Models (`core/knowledge/`)

```
KanjiKnowledge       → character, meanings, readings, classifications, frequency
RadicalKnowledge     → radical, stroke count
RadicalStats         → radical + kanji count (for explorer grid)
ComponentKnowledge   → component, radicalOf, strokes, source
WordKnowledge        → id, kanjiReading, kanaReading, furigana, glossary, pos
SentenceKnowledge    → text, translation, furigana
GrammarPattern       → id, pattern, meaning, formation, register, jlpt
GrammarMatch         → patternId, matchedText, startIndex, endIndex
```

### Knowledge Graph (`KnowledgeGraph.kt`)

The graph is a navigation surface, not decoration. Nodes are type-prefixed (`"kanji:食"`, `"radical:口"`, `"word:12345"`) and edges are labeled relationships.

**Node types**: Kanji, Radical, Word, Sentence, Grammar
**Edge types**: Contains, ComponentOf, RadicalOf, UsedIn, AppearsIn, ExampleOf, RelatedTo

The graph is always expanded progressively — never thousands of nodes at once. See `GraphExpansionLimits` for caps.

### Knowledge Repository (`KnowledgeRepository.kt`)

Facade over `AppDataRepository`. The UI never touches raw SQLDelight rows. All lookups return domain models.

Key methods:
- `kanji(character)` → full kanji entry
- `radicalsIn(character)` → components inside a kanji
- `kanjiWithRadicals(radicals)` → intersection search
- `wordsContaining(character)` → kanji → words
- `sentencesWithText(text)` → word → sentences
- `grammarIn(sentence)` → grammar matches
- `kanjiSearchIndex()` → in-memory search index (cached)

### Search Engine (`KnowledgeSearchEngine.kt`)

Universal grouped search. One entry point from Home, Library, Browse, and the dictionary. Results are grouped: KANJI → WORDS → SENTENCES → GRAMMAR.

**Categories** (filterable): Kanji, Words, Sentences, Grammar
**Filters**: JLPT, Grade, Frequency, StrokeCount, PartOfSpeech
**Sorts**: Relevance, Frequency, StrokeCount, Jlpt, Grade, Alphabetical, Reading

Kanji search uses an in-memory index (~2k jōyō characters). Word/sentence searches are DB-backed.

### Sentence Analysis (`SentenceAnalysis.kt`)

Interactive tokenization of corpus sentences. Each token is annotated with dictionary links:
- Kanji tokens → kanji entry
- Mixed tokens (食べる) → word entry + per-character kanji
- Kana tokens → word lookup (2+ chars)

Grammar patterns are highlighted via substring matching from the built-in catalog.

### Sentence Difficulty (`SentenceDifficulty.kt`)

Surface-feature scoring (1..10) based on:
- Sentence length
- Kanji density
- Unique kanji count
- Grammar pattern density
- Known-kanji overlay (optional)

### Study State (`StudyStateMachine.kt`)

One explicit state machine: New → Learning → Known → Due → Mastered → Relearning → Suspended. Derived from real FSRS cards. No scattered booleans.

### Level Profiles (`LevelProfile.kt`)

9 learner profiles (ChildBeginner → Research) + Custom. Controls:
- Furigana visibility
- Romaji visibility
- Translation visibility
- Rare reading visibility
- Explanation depth
- Sentence difficulty
- Graph complexity
- Card preset

## Card System

Every entity page (kanji, word, sentence, grammar, collection) is a sequence of modular cards. The layout is **data**, never hardcoded per screen — users can show/hide/reorder cards and apply presets. See `core/knowledge/cards/`.

### Kanji Cards (`KanjiCardModels.kt`)

16 card types: Hero, Meaning, Readings, Frequency, Classification, Radical, Component, Stroke, Vocabulary, Related, Variant, Sentence, Grammar, Graph, Media, Study.

### Word Cards (`WordCardModels.kt`)

9 card types: Hero, Readings, Meanings, PartOfSpeech, Kanji, Frequency, Sentences, Grammar, Study.

### Sentence Cards (`SentenceCardModels.kt`)

8 card types: Hero, Translation, Tokens, Grammar, Vocabulary, Difficulty, Source, Study.

### Grammar Cards (`GrammarCardModels.kt`)

8 card types: Hero, Meaning, Structure, Examples, JLPT, RelatedGrammar, Kanji, Study.

### Collection Cards (`CollectionCardModels.kt`)

7 card types: Hero, KanjiGrid, KanjiList, FrequencyDistribution, JLPTBreakdown, StudyState, Statistics.

**Presets** (all five systems): Minimal, Beginner, Standard, Advanced, Research. (Kanji also has Intermediate / Writing / Reading / Dictionary.)

### Registry & Adapter

- `CardRegistry` — one API over all five card systems (cards, counts, presets, titles, visible-card resolution from stored JSON).
- `PresetAdapter` — learner profile (§23) → recommended card preset per entity type, with fallback resolution.
- `CardSettingsScreen` — per-entity show/hide/reorder (drag), presets, reset, save.
- Layout is `CardLayout` (order + hidden), serialized as JSON, persisted per profile in app preferences, sanitized on load (corrupt blobs fall back to defaults). Stores registered in `CoreModule`.

## World System

Kaiteyo World is a streamable 3D Japan with its own isolated runtime (`core/world/`). See `docs/architecture/WORLD_SYSTEM.md`.

- Geographic coordinate system (world space + WGS84 projection)
- Chunk system (256 m chunks, streaming, load radius, hard cap)
- Terrain, water/beach, buildings, vehicles, Enoden train, NPCs
- Player controller + third/first-person camera
- Time & weather cycle
- Save/load (versioned JSON) + world map
- `WorldController` + `KamakuraWorld` factory; `WorldScreen` wired into navigation

## Navigation

### Two-Mode System

- **Sidebar**: Structured dock on screen edge (Left/Right/Top/Bottom)
- **Floating**: Draggable launcher bubble with 12 snap points

Both use the same `NavigationController` — switching modes never recreates the app.

### NavShell Architecture

```
NavShell
├── AdaptiveNavigation
│   ├── Row (vertical sidebar) or Column (horizontal bar)
│   │   ├── DockedSidebar (animated width/height)
│   │   └── Content (weighted sibling)
│   └── BubbleLauncher (floating mode overlay)
├── PageNameIndicator (debug, top-right)
└── DebugPanel (debug, bottom-left)
```

## Theme System

### Base Modes
- OLED Black (default)
- Dark Gray
- Light
- Sepia

### Accent Schemes (7)
Signature Pineapple, Cotton Candy, Ocean, Forest, Sunset, Lavender, Monochrome

### Token System
- `SurfaceColors`: background, surface, surfaceElevated, surfaceInteractive, border, textPrimary/Secondary/Muted/Inverse
- `KaiteyoAccentScheme`: primary, primaryDark, secondary, secondaryDark, onPrimary, onSecondary, tertiary
- `Dimens`: spacing scale (4dp grid), corner radius system
- `AnimationConfig`: speed, spring params, page transitions
- `RadiusConfig`: style presets (Square/Rounded/VeryRounded/Soft)
- `GlowConfig`: intensity, radius, opacity
- `TypeScale`: fontScale, titleScale, lineHeight

## Search

### Universal Search (`UniversalSearch.kt`)

Ctrl+Shift+F (desktop) or search icon. One overlay from any screen. Grouped results with category filter chips (Kanji/Words/Sentences/Grammar).

### Library Search

Inline unified search across kanji, vocabulary, and decks. Keyboard-navigable (↑/↓/Enter/Esc).

### Browse Hub

Exploratory surface: JLPT collections, grade collections, frequency distribution, radical grid, grammar catalog.

## Performance Patterns

- **Debounced search** (280ms default)
- **In-memory kanji index** (~2k chars, trivial to scan)
- **Lazy pagination** (`LazyPager<T>`)
- **Search result cache** (30s TTL)
- **Graph expansion limits** (12 nodes/expansion, 120 total, depth 4)
- **LRU cache** for expensive computations

## Accessibility

- Focus indicators (accent-colored 2dp border)
- Keyboard navigation (Escape, /, Ctrl+K)
- Minimum touch targets (48dp)
- Reduced motion support
- High contrast mode
- Screen reader labels
- Non-color indicators

## Data Flow

```
UI → ViewModel → KnowledgeRepository → AppDataRepository → SQLDelight
                                    ↓
                              KnowledgeSearchEngine (in-memory index)
                                    ↓
                              KnowledgeGraph (progressive expansion)
```

User data (bookmarks, progress, notes) is separate from dictionary data (canonical).

## Testing

- `commonTest/`: `kotlin.test` in commonMain
- Unit tests for: search, filters, sorting, card layout, sentence analysis, difficulty scoring, study state machine, level profiles
- Fake preferences for test isolation
- Graph layout tests
