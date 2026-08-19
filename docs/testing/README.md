# Kaiteyo Test Strategy

## Overview

Kaiteyo uses `kotlin.test` in `commonTest` for cross-platform testing. Tests run on JVM via JUnit platform.

## Test Categories

### Unit Tests

Pure logic tests with no Android/UI dependencies:

| Test File | What it Tests |
|---|---|
| `KanjiCardModelsTest` | Kanji card layout, presets, persistence, sanitization |
| `WordCardModelsTest` | Word card layout, presets, persistence, sanitization |
| `LevelProfileTest` | Learner profile catalog, presentation defaults |
| `NavigationSettingsStateTest` | Navigation settings persistence, mode switching |
| `PerformanceUtilsTest` | LruCache, SearchResultCache, frequencyNormalized |
| `FsrsSchedulerTest` | FSRS scheduling algorithm |
| `SentenceDifficultyTest` | Difficulty scoring accuracy |
| `KnowledgeGraphTest` | Graph expansion, node/edge management |
| `SearchEngineTest` | Kanji search index, grouped results |

### Integration Tests

Tests that verify component interaction:

| Test File | What it Tests |
|---|---|
| `KnowledgeRepositoryTest` | Repository → AppDataRepository integration |
| `SearchIntegrationTest` | Full search pipeline (query → grouped results) |
| `GraphExpansionTest` | Progressive expansion with type filtering |

### UI Tests

Compose UI tests (when established):

| Test File | What it Tests |
|---|---|
| `NavigationTest` | Sidebar ↔ Floating switching |
| `ThemeSwitchingTest` | Theme persistence across restarts |
| `CardCustomizationTest` | Card show/hide/reorder persistence |

## Test Patterns

### Fake Preferences

All tests use `FakeAppPreferences` that implements `PreferencesContract.AppPreferences` with in-memory maps:

```kotlin
private class FakeAppPreferences : PreferencesContract.AppPreferences {
    private val values = mutableMapOf<String, Any?>()
    private fun <T> mem(key: String, default: T): SuspendedProperty<T> = ...
    
    override val kanjiCardLayoutJson get() = mem("kanjiCardLayoutJson", "")
    override val wordCardLayoutJson get() = mem("wordCardLayoutJson", "")
    override val learnerProfileJson get() = mem("learnerProfileJson", "")
    // ... all other properties
}
```

### Test Execution

```bash
# Run all tests
./gradlew :core:allTests

# Run specific test class
./gradlew :core:jvmTest --tests "ua.syt0r.kanji.core.knowledge.cards.KanjiCardModelsTest"

# Run desktop tests
./gradlew :desktopApp:test

# Run data platform tests
./gradlew :kjd:test
```

## What to Test

### Card System

- Default layout shows all cards
- Hide/show individual cards
- Move cards up/down
- Preset application
- Layout persistence round-trip
- Corrupt JSON fallback
- Unknown card ID sanitization

### Search

- Exact kanji match (highest priority)
- Reading match
- Meaning match (case-insensitive)
- Category filtering (Kanji/Words/Sentences/Grammar)
- JLPT filtering
- Grade filtering
- Frequency filtering
- Sort by frequency/strokes/JLPT/grade
- Empty query returns nothing
- Debouncing cancels old queries

### Knowledge Graph

- Initial graph has root + first ring
- Expand adds neighbors
- Type filter limits expansion
- Grammar nodes are leaves (no expansion)
- Exhausted nodes tracked
- Graph never exceeds limits

### Sentence Analysis

- Tokenization splits correctly
- Mixed tokens (食べる) recognized
- Punctuation excluded from dictionary lookup
- Grammar patterns matched
- Difficulty scoring accuracy
- Profile-based sentence filtering

### Study State

- FSRS card → StudyState projection
- State transitions (New → Learning → Known → Due → Mastered)
- Relearning from lapse
- Suspend/resume
- Forget resets to New

### Level Profiles

- Each profile has correct defaults
- Custom profile uses overrides
- Profile switching preserves custom config
- Invalid profile falls back to Intermediate

### Performance

- LruCache eviction order
- LruCache access refreshes
- SearchResultCache TTL
- Search cache invalidation
- Frequency normalization
- Graph expansion limits

## Coverage Goals

| Area | Target | Current |
|---|---|---|
| Card System | 100% | ✅ |
| Search Engine | 90% | 80% |
| Knowledge Graph | 85% | 75% |
| Sentence Analysis | 90% | 85% |
| Study State | 95% | 90% |
| Level Profiles | 100% | ✅ |
| Performance Utils | 90% | 85% |
| Navigation | 80% | 70% |
| Theme System | 75% | 65% |

## Test Anti-Patterns to Avoid

1. **Don't test implementation details** — test behavior
2. **Don't depend on test ordering** — each test is independent
3. **Don't use real databases** — use fakes
4. **Don't test UI rendering** — test state and logic
5. **Don't skip error cases** — test failure modes

## Adding New Tests

1. Create test file in `core/src/commonTest/kotlin/ua/syt0r/kanji/...`
2. Use `kotlin.test` assertions
3. Use `FakeAppPreferences` for persistence tests
4. Run `./gradlew :core:allTests` to verify
5. Update this document with new test file
