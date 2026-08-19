# Kaiteyo Data Flow

## Overview

Kaiteyo separates concerns into clear layers:

```
UI (Compose)
  ↓
Presentation (ViewModel / State)
  ↓
Domain (KnowledgeRepository / SearchEngine / GraphRepository)
  ↓
Data (AppDataRepository / SQLDelight)
```

## Data Sources

### AppDataDatabase (Bundled)

Read-only dictionary data shipped with the app:
- `kanji` — character, frequency, variant family
- `kanji_meaning` — English meanings per kanji
- `kanji_reading` — on/kun readings
- `kanji_classification` — JLPT, grade, WaniKani tags
- `kanji_stroke` — stroke count and paths
- `word` — JMdict entries
- `word_reading` — kanji/kana readings
- `word_gloss` — English glossary
- `word_classification` — part of speech, JLPT
- `sentence` — corpus sentences with translations
- `radical` — radical data
- `kanji_radical` — kanji ↔ radical mapping

Source: JMdict, KANJIDIC, Tatoeba, school grade datasets.

### UserDataDatabase (Mutable)

User state stored locally:
- `srs_card` — FSRS scheduling data
- `review_history` — review records with grades
- `deck` — user-created decks
- `card_flag` — flags and favorites
- `card_tag` — tags and tag assignments
- `study_history` — action history

### Preferences (Key-Value)

Settings and configuration:
- Navigation settings (JSON)
- Theme settings (JSON)
- Debug settings (JSON)
- Learner profile (JSON)
- Kanji/Word card layouts (JSON)
- Search history
- Shortcut bindings

## Knowledge Repository

Facade over `AppDataRepository`. The UI never touches raw SQLDelight rows.

### Kanji Flow

```
UI requests kanji("食")
  ↓
KnowledgeRepository.kanji("食")
  ↓
AppDataRepository.getData("食") → KanjiData
AppDataRepository.getMeanings("食") → List<String>
AppDataRepository.getReadings("食") → Map<String, ReadingType>
AppDataRepository.getClassificationsForKanji("食") → List<String>
AppDataRepository.getKanjiStrokeCounts() → Map<String, Int>
  ↓
KanjiKnowledge(character, meanings, readings, classifications, ...)
```

### Word Flow

```
UI requests wordsContaining("食")
  ↓
KnowledgeRepository.wordsContaining("食")
  ↓
AppDataRepository.getWordExamples("食") → List<JapaneseWord>
  ↓
List<WordKnowledge>(id, kanjiReading, kanaReading, glossary, pos)
```

### Sentence Flow

```
UI requests sentencesWithText("食べる")
  ↓
KnowledgeRepository.sentencesWithText("食べる")
  ↓
AppDataRepository.getSentencesWithText("食べる") → List<Sentence>
  ↓
List<SentenceKnowledge>(text, translation, furigana)
```

### Grammar Flow

```
UI requests grammarIn("今日は日本語を勉強します。")
  ↓
KnowledgeRepository.grammarIn(sentence)
  ↓
GrammarCatalog.findIn(sentence) → List<GrammarMatch>
  ↓
(matchedText, patternId, startIndex, endIndex)
```

## Search Flow

```
User types "食べる"
  ↓
UniversalSearch.updateQuery("食べる")
  ↓
Debouncer (280ms)
  ↓
KnowledgeSearchEngine.search(query)
  ↓
┌─ KanjiSearchIndex.search(query) → List<KanjiHit>
│  (in-memory scan of ~2k characters)
│
├─ KnowledgeRepository.wordsWithText("食べる") → List<WordHit>
│  (DB query with LIMIT)
│
├─ KnowledgeRepository.sentencesWithText("食べる") → List<SentenceHit>
│  (DB query with LIMIT)
│
└─ GrammarCatalog.search("食べる") → List<GrammarHit>
   (in-memory substring search)
  ↓
GroupedSearchResults(kanji, words, sentences, grammar)
```

## Knowledge Graph Flow

```
User opens graph for "食"
  ↓
KnowledgeGraphRepository.initialGraph("食")
  ↓
Build root node + first ring:
├─ Components/radicals → RadicalOf edges
├─ Words using 食 → UsedIn edges
└─ Related kanji by shared radical → RelatedTo edges
  ↓
KnowledgeGraph(rootId, nodes, edges)
  ↓
User taps a node to expand
  ↓
KnowledgeGraphRepository.expand(graph, nodeId, typeFilter)
  ↓
Pull one more ring of relationships:
├─ Kanji → components, words, related, sentences
├─ Radical → kanji that use it
├─ Word → kanji in spelling, sentences
└─ Sentence → grammar patterns
  ↓
GraphExpansion(graph, addedNodes, addedEdges, exhausted)
```

## Study State Flow

```
User reviews a card
  ↓
FsrsCardRepository updates FSRS card
  ↓
StudyStateMachine.project(card, now) → StudyState
  ↓
StudyGate.stateFor(card, now) → StudyState
  ↓
UI shows: New | Learning | Known | Due | Mastered | Relearning | Suspended
```

## Profile Adaptation Flow

```
User changes profile to "Beginner"
  ↓
LearnerProfileStore.save(profile)
  ↓
LearnerProfileCatalog.defaultsFor(Beginner) → ProfilePresentation
  ↓
UI reads:
├─ showFurigana = true
├─ showRomaji = true
├─ showTranslations = true
├─ explanationDepth = Clear
├─ sentenceDifficulty = Easy
├─ graphComplexity = Simple
└─ cardPresetId = "beginner"
  ↓
KanjiEntry loads → filters sentences by difficulty → shows beginner cards
WordEntry loads → shows all glossary senses
```

## Card Layout Flow

```
User opens kanji "食"
  ↓
KanjiCardLayoutStore.load() → KanjiCardLayout
  ↓
Layout.visibleCards() → List<KanjiCardType>
  ↓
For each card type:
├─ Hero → KanjiKnowledge.character + keyword
├─ Meaning → KanjiKnowledge.meanings
├─ Readings → on/kun readings
├─ Frequency → frequencyRank + FrequencyBand
├─ Classification → JLPT/grade tags
├─ Radical → first component
├─ Component → all components
├─ Stroke → strokeCount + strokePaths
├─ Vocabulary → wordsContaining()
├─ Sentence → sentencesWithText() filtered by profile
├─ Grammar → grammarIn(sentences)
├─ Graph → KnowledgeGraphCanvas
└─ Study → StudyState from SRS card
  ↓
LazyColumn renders cards in layout order
```

## User Data Isolation

```
DICTIONARY DATA (canonical)
├─ Kanji: 食
├─ Meanings: eat, food
├─ Readings: ショク, た(べる)
└─ NEVER MODIFIED by user actions

USER DATA (mutable)
├─ bookmarked = true
├─ knowledgeState = learning
├─ notes = "important kanji"
├─ reviews = [grade 3, grade 5, ...]
└─ flags = [favorite]
```

The dictionary stays canonical. User state is projected from real SRS cards.

## Offline Operation

All core dictionary functionality works offline:
- Kanji lookup
- Word search
- Sentence search
- Grammar matching
- Knowledge graph
- Study reviews

Cloud features (sync, backup) require network but degrade gracefully.
