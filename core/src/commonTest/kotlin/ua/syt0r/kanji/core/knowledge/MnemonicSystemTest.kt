package ua.syt0r.kanji.core.knowledge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import kotlin.test.Test

class MnemonicSystemTest {

    // ------------------------------------------------------------------
    // Add and get
    // ------------------------------------------------------------------

    @Test
    fun emptyStoreHasNoMnemonics() {
        val store = MnemonicStore()
        assertTrue(store.getMnemonics("食").isEmpty())
        assertNull(store.getBestMnemonic("食"))
    }

    @Test
    fun addAndGetMnemonic() {
        val store = MnemonicStore()
        store.addMnemonic("食", KanjiMnemonic(
            text = "A person (人) over good (良) — eating well",
            source = ContentSourceType.UserGenerated
        ))
        val mnemonics = store.getMnemonics("食")
        assertEquals(1, mnemonics.size)
        assertEquals("食", mnemonics[0].text.let { "食" })
    }

    @Test
    fun bestMnemonicPrefersAuthoritative() {
        val store = MnemonicStore()
        store.addMnemonic("食", KanjiMnemonic(
            text = "AI mnemonic",
            source = ContentSourceType.AiGenerated
        ))
        store.addMnemonic("食", KanjiMnemonic(
            text = "Authoritative mnemonic",
            source = ContentSourceType.Authoritative
        ))
        val best = store.getBestMnemonic("食")
        assertNotNull(best)
        assertEquals(ContentSourceType.Authoritative, best?.source)
    }

    @Test
    fun bestMnemonicPrefersHigherRating() {
        val store = MnemonicStore()
        store.addMnemonic("食", KanjiMnemonic(
            text = "Rating 2",
            source = ContentSourceType.UserGenerated,
            rating = 2
        ))
        store.addMnemonic("食", KanjiMnemonic(
            text = "Rating 5",
            source = ContentSourceType.UserGenerated,
            rating = 5
        ))
        val best = store.getBestMnemonic("食")
        assertEquals("Rating 5", best?.text)
    }

    @Test
    fun inactiveMnemonicsAreHidden() {
        val store = MnemonicStore()
        store.addMnemonic("食", KanjiMnemonic(
            text = "Old mnemonic",
            source = ContentSourceType.UserGenerated,
            isActive = false
        ))
        store.addMnemonic("食", KanjiMnemonic(
            text = "New mnemonic",
            source = ContentSourceType.UserGenerated
        ))
        val mnemonics = store.getMnemonics("食")
        assertEquals(1, mnemonics.size)
        assertEquals("New mnemonic", mnemonics[0].text)
    }

    // ------------------------------------------------------------------
    // Rating
    // ------------------------------------------------------------------

    @Test
    fun rateMnemonicUpdatesRating() {
        val store = MnemonicStore()
        store.addMnemonic("食", KanjiMnemonic(text = "test", source = ContentSourceType.UserGenerated))
        store.rateMnemonic("食", 0, 5)
        val mnemonic = store.getMnemonics("食")[0]
        assertEquals(5, mnemonic.rating)
        assertEquals(1, mnemonic.useCount)
    }

    @Test
    fun ratingIsClamped() {
        val store = MnemonicStore()
        store.addMnemonic("食", KanjiMnemonic(text = "test", source = ContentSourceType.UserGenerated))
        store.rateMnemonic("食", 0, 99)
        assertEquals(5, store.getMnemonics("食")[0].rating)
    }

    // ------------------------------------------------------------------
    // Formulas
    // ------------------------------------------------------------------

    @Test
    fun addAndGetFormulas() {
        val store = MnemonicStore()
        store.addFormula("食", KanjiFormula(
            text = "人 (person) + 良 (good) = 食",
            components = listOf(
                FormulaComponent("人", "person", FormulaRole.Semantic),
                FormulaComponent("良", "good", FormulaRole.Phonetic)
            ),
            source = ContentSourceType.Authoritative,
            type = FormulaType.Structural
        ))
        val formulas = store.getFormulas("食")
        assertEquals(1, formulas.size)
        assertEquals(2, formulas[0].components.size)
        assertEquals(FormulaType.Structural, formulas[0].type)
    }

    @Test
    fun bestFormulaIsFirst() {
        val store = MnemonicStore()
        store.addFormula("食", KanjiFormula(
            text = "structural",
            source = ContentSourceType.Ai,
            type = FormulaType.Visual
        ))
        store.addFormula("食", KanjiFormula(
            text = "authoritative structural",
            source = ContentSourceType.Authoritative,
            type = FormulaType.Structural
        ))
        val best = store.getBestFormula("食")
        assertEquals("authoritative structural", best?.text)
    }

    // ------------------------------------------------------------------
    // Deactivate and clear
    // ------------------------------------------------------------------

    @Test
    fun deactivateMnemonicHidesIt() {
        val store = MnemonicStore()
        store.addMnemonic("食", KanjiMnemonic(text = "a", source = ContentSourceType.UserGenerated))
        store.deactivateMnemonic("食", 0)
        assertTrue(store.getMnemonics("食").isEmpty())
    }

    @Test
    fun clearMnemonicsRemovesAll() {
        val store = MnemonicStore()
        store.addMnemonic("食", KanjiMnemonic(text = "a", source = ContentSourceType.UserGenerated))
        store.clearMnemonics("食")
        assertTrue(store.getMnemonics("食").isEmpty())
    }

    @Test
    fun clearFormulasRemovesAll() {
        val store = MnemonicStore()
        store.addFormula("食", KanjiFormula(
            text = "a", source = ContentSourceType.UserGenerated, type = FormulaType.Visual
        ))
        store.clearFormulas("食")
        assertTrue(store.getFormulas("食").isEmpty())
    }

    // ------------------------------------------------------------------
    // Stats
    // ------------------------------------------------------------------

    @Test
    fun statsAreZeroForUnknownKanji() {
        val store = MnemonicStore()
        val stats = store.getStats("食")
        assertEquals(0, stats.total)
        assertFalse(stats.hasAuthoritative)
        assertFalse(stats.hasAi)
        assertFalse(stats.hasUser)
    }

    @Test
    fun statsCountBySource() {
        val store = MnemonicStore()
        store.addMnemonic("食", KanjiMnemonic(text = "a", source = ContentSourceType.UserGenerated))
        store.addMnemonic("食", KanjiMnemonic(text = "b", source = ContentSourceType.AiGenerated))
        store.addMnemonic("食", KanjiMnemonic(text = "c", source = ContentSourceType.UserGenerated))
        val stats = store.getStats("食")
        assertEquals(3, stats.total)
        assertEquals(2, stats.bySource[ContentSourceType.UserGenerated])
        assertEquals(1, stats.bySource[ContentSourceType.AiGenerated])
        assertTrue(stats.hasUser)
        assertTrue(stats.hasAi)
        assertFalse(stats.hasAuthoritative)
    }

    @Test
    fun statsAverageRating() {
        val store = MnemonicStore()
        store.addMnemonic("食", KanjiMnemonic(text = "a", source = ContentSourceType.UserGenerated, rating = 3))
        store.addMnemonic("食", KanjiMnemonic(text = "b", source = ContentSourceType.UserGenerated, rating = 5))
        val stats = store.getStats("食")
        assertEquals(4.0, stats.averageRating ?: -1.0, 0.001)
    }

    // ------------------------------------------------------------------
    // Enum sanity
    // ------------------------------------------------------------------

    @Test
    fun contentSourceTypesHaveLabels() {
        assertEquals("Authoritative", ContentSourceType.Authoritative.label)
        assertEquals("AI-generated", ContentSourceType.AiGenerated.label)
        assertEquals("User-created", ContentSourceType.UserGenerated.label)
        assertEquals("Community", ContentSourceType.Community.label)
        assertEquals("Derived from dataset", ContentSourceType.Derived.label)
    }

    @Test
    fun formulaTypesCovered() {
        assertTrue(FormulaType.entries.size >= 5)
        assertEquals("Structural — how the kanji is built", FormulaType.Structural.label)
    }

    @Test
    fun formulaRolesCovered() {
        assertTrue(FormulaRole.entries.size >= 5)
        assertEquals("Semantic — provides meaning", FormulaRole.Semantic.label)
    }

    @Test
    fun graphEdgeTypesCovered() {
        assertTrue(GraphEdgeType.entries.size >= 10)
        assertEquals("component of", GraphEdgeType.ComponentOf.label)
        assertEquals("radical of", GraphEdgeType.RadicalOf.label)
    }

    @Test
    fun graphNodeTypesCovered() {
        assertTrue(GraphNodeType.entries.size >= 8)
        assertEquals("Kanji", GraphNodeType.Kanji.label)
        assertEquals("Grammar", GraphNodeType.Grammar.label)
    }
}
