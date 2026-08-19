package ua.syt0r.kanji.core.knowledge.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import kotlin.test.Test

class CardRegistryTest {

    private val registry = CardRegistry()

    // ------------------------------------------------------------------
    // Card counts
    // ------------------------------------------------------------------

    @Test
    fun kanjiCardCount() {
        assertEquals(KanjiCardType.entries.size, registry.cardCount(CardEntityType.Kanji))
    }

    @Test
    fun wordCardCount() {
        assertEquals(WordCardType.entries.size, registry.cardCount(CardEntityType.Word))
    }

    @Test
    fun sentenceCardCount() {
        assertEquals(SentenceCardType.entries.size, registry.cardCount(CardEntityType.Sentence))
    }

    @Test
    fun grammarCardCount() {
        assertEquals(GrammarCardType.entries.size, registry.cardCount(CardEntityType.Grammar))
    }

    @Test
    fun collectionCardCount() {
        assertEquals(CollectionCardType.entries.size, registry.cardCount(CardEntityType.Collection))
    }

    // ------------------------------------------------------------------
    // All cards
    // ------------------------------------------------------------------

    @Test
    fun allKanjiCards() {
        val cards = registry.allCardsFor(CardEntityType.Kanji)
        assertEquals(KanjiCardType.entries.size, cards.size)
        assertTrue(cards.containsAll(KanjiCardType.entries.map { it.id }))
    }

    @Test
    fun allWordCards() {
        val cards = registry.allCardsFor(CardEntityType.Word)
        assertEquals(WordCardType.entries.size, cards.size)
        assertTrue(cards.containsAll(WordCardType.entries.map { it.id }))
    }

    @Test
    fun allSentenceCards() {
        val cards = registry.allCardsFor(CardEntityType.Sentence)
        assertEquals(SentenceCardType.entries.size, cards.size)
        assertTrue(cards.containsAll(SentenceCardType.entries.map { it.id }))
    }

    @Test
    fun allGrammarCards() {
        val cards = registry.allCardsFor(CardEntityType.Grammar)
        assertEquals(GrammarCardType.entries.size, cards.size)
        assertTrue(cards.containsAll(GrammarCardType.entries.map { it.id }))
    }

    @Test
    fun allCollectionCards() {
        val cards = registry.allCardsFor(CardEntityType.Collection)
        assertEquals(CollectionCardType.entries.size, cards.size)
        assertTrue(cards.containsAll(CollectionCardType.entries.map { it.id }))
    }

    // ------------------------------------------------------------------
    // Presets
    // ------------------------------------------------------------------

    @Test
    fun kanjiPresets() {
        val presets = registry.presetsFor(CardEntityType.Kanji)
        assertTrue(presets.contains("beginner"))
        assertTrue(presets.contains("advanced"))
        assertTrue(presets.contains("research"))
    }

    @Test
    fun wordPresets() {
        val presets = registry.presetsFor(CardEntityType.Word)
        assertTrue(presets.contains("minimal"))
        assertTrue(presets.contains("standard"))
    }

    @Test
    fun sentencePresets() {
        val presets = registry.presetsFor(CardEntityType.Sentence)
        assertTrue(presets.contains("beginner"))
        assertTrue(presets.contains("advanced"))
    }

    @Test
    fun grammarPresets() {
        val presets = registry.presetsFor(CardEntityType.Grammar)
        assertTrue(presets.contains("beginner"))
        assertTrue(presets.contains("standard"))
    }

    @Test
    fun collectionPresets() {
        val presets = registry.presetsFor(CardEntityType.Collection)
        assertTrue(presets.contains("beginner"))
        assertTrue(presets.contains("advanced"))
    }

    // ------------------------------------------------------------------
    // Title and description
    // ------------------------------------------------------------------

    @Test
    fun kanjiCardTitle() {
        assertEquals("Hero", registry.cardTitle(CardEntityType.Kanji, "hero"))
        assertEquals("Readings", registry.cardTitle(CardEntityType.Kanji, "readings"))
    }

    @Test
    fun wordCardTitle() {
        assertEquals("Hero", registry.cardTitle(CardEntityType.Word, "hero"))
        assertEquals("Sentences", registry.cardTitle(CardEntityType.Word, "sentences"))
    }

    @Test
    fun sentenceCardTitle() {
        assertEquals("Tokens", registry.cardTitle(CardEntityType.Sentence, "tokens"))
        assertEquals("Grammar", registry.cardTitle(CardEntityType.Sentence, "grammar"))
    }

    @Test
    fun grammarCardTitle() {
        assertEquals("Structure", registry.cardTitle(CardEntityType.Grammar, "structure"))
        assertEquals("JLPT", registry.cardTitle(CardEntityType.Grammar, "jlpt"))
    }

    @Test
    fun unknownCardTitleReturnsId() {
        assertEquals("nonexistent", registry.cardTitle(CardEntityType.Kanji, "nonexistent"))
    }

    @Test
    fun collectionCardTitle() {
        assertEquals("Hero", registry.cardTitle(CardEntityType.Collection, "hero"))
        assertEquals("Kanji grid", registry.cardTitle(CardEntityType.Collection, "kanji_grid"))
    }

    // ------------------------------------------------------------------
    // Visible card IDs from JSON
    // ------------------------------------------------------------------

    @Test
    fun kanjiVisibleCardsFromDefaultLayout() {
        val ids = registry.visibleCardIds(CardEntityType.Kanji, "")
        assertEquals(KanjiCardType.entries.size, ids.size)
    }

    @Test
    fun wordVisibleCardsFromDefaultLayout() {
        val ids = registry.visibleCardIds(CardEntityType.Word, "")
        assertEquals(WordCardType.entries.size, ids.size)
    }

    @Test
    fun sentenceVisibleCardsFromDefaultLayout() {
        val ids = registry.visibleCardIds(CardEntityType.Sentence, "")
        assertEquals(SentenceCardType.entries.size, ids.size)
    }

    @Test
    fun grammarVisibleCardsFromDefaultLayout() {
        val ids = registry.visibleCardIds(CardEntityType.Grammar, "")
        assertEquals(GrammarCardType.entries.size, ids.size)
    }

    @Test
    fun collectionVisibleCardsFromDefaultLayout() {
        val ids = registry.visibleCardIds(CardEntityType.Collection, "")
        assertEquals(CollectionCardType.entries.size, ids.size)
    }

    @Test
    fun visibleCardsFromCorruptJsonReturnsDefault() {
        val ids = registry.visibleCardIds(CardEntityType.Kanji, "NOT JSON {{{")
        assertEquals(KanjiCardType.entries.size, ids.size)
    }

    // ------------------------------------------------------------------
    // Every entity type has entries
    // ------------------------------------------------------------------

    @Test
    fun allEntityTypesHaveCards() {
        CardEntityType.entries.forEach { entity ->
            assertTrue("${entity.label} should have cards", registry.cardCount(entity) > 0)
        }
    }

    @Test
    fun allEntityTypesHavePresets() {
        CardEntityType.entries.forEach { entity ->
            assertTrue("${entity.label} should have presets", registry.presetsFor(entity).isNotEmpty())
        }
    }
}
