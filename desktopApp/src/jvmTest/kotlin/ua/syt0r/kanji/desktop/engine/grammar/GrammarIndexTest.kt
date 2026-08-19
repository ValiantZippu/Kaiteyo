package ua.syt0r.kanji.desktop.engine.grammar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GrammarIndexTest {

    private fun index(): GrammarIndex = GrammarIndex(CuratedGrammarFacts.all)

    @Test
    fun lookupFindsEntryByCanonicalPattern() {
        val entry = index().lookup("〜なければならない")
        assertTrue(entry?.meaning?.contains("must do") == true)
        assertEquals(4, entry?.jlpt)
    }

    @Test
    fun lookupFindsEntryByFormPattern() {
        // 〜なければいけない is declared as a form of 〜なければならない.
        val entry = index().lookup("〜なければいけない")
        assertEquals("〜なければならない", entry?.pattern)
    }

    @Test
    fun lookupReturnsNullForUnknown() {
        assertNull(index().lookup("〜全然ないパターン"))
    }

    @Test
    fun searchMatchesMeaningAndPattern() {
        val results = index().search("obligation")
        assertTrue(results.any { it.pattern == "〜なければならない" })
        val byPattern = index().search("〜たい")
        assertTrue(byPattern.any { it.pattern == "〜たい" })
    }

    @Test
    fun matchFindsPatternsPresentInSentenceLongestFirst() {
        val sentence = "宿題をしなければならないから、今やりましょう。"
        val matched = index().match(sentence)
        assertTrue(matched.any { it.pattern == "〜なければならない" })
        assertTrue(matched.any { it.pattern == "〜ましょう" })
        assertTrue(matched.any { it.pattern == "〜から" })
        // Longest pattern sorts first.
        assertEquals("〜なければならない", matched.first().pattern)
    }

    @Test
    fun everyCuratedEntryHasSourceProvenance() {
        CuratedGrammarFacts.all.forEach { entry ->
            assertTrue(entry.source.isNotBlank(), "entry ${entry.id} must declare a source")
        }
    }
}
