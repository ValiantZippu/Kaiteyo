package ua.syt0r.kanji.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GrammarCatalogTest {

    @Test
    fun particleIsFoundInsideSentence() {
        val matches = GrammarCatalog.findIn("私は毎日日本語を勉強します。")
        val wa = matches.firstOrNull { it.patternId == "particle-wa" }
        assertEquals("は", wa?.matchedText)
        assertEquals(1, wa?.startIndex)
    }

    @Test
    fun teFormPatternWinsOverBareParticle() {
        // 〜てみる contains て and みる — the longest form must claim the span.
        val matches = GrammarCatalog.findIn("一度食べてみるつもりです。")
        val teMiru = matches.firstOrNull { it.patternId == "grammar-te-miru" }
        assertEquals("てみる", teMiru?.matchedText)
        // No shorter て/み particle may overlap the claimed span.
        val overlapping = matches.any { match ->
            match.patternId != "grammar-te-miru" &&
                match.startIndex >= teMiru!!.startIndex &&
                match.endIndex <= teMiru.endIndex
        }
        assertEquals(false, overlapping)
    }

    @Test
    fun blankTextHasNoMatches() {
        assertEquals(emptyList(), GrammarCatalog.findIn(""))
        assertEquals(emptyList(), GrammarCatalog.findIn("   "))
    }

    @Test
    fun matchesAreOrderedLeftToRight() {
        val matches = GrammarCatalog.findIn("東京から大阪まで")
        assertTrue(matches.map { it.startIndex }.zipWithNext().all { (a, b) -> a <= b })
    }

    @Test
    fun searchByJLPTLevel() {
        val results = GrammarCatalog.search("N4")
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.jlpt == 4 })
    }

    @Test
    fun searchByKeywordAndKana() {
        assertTrue(GrammarCatalog.search("try").isNotEmpty())
        assertTrue(GrammarCatalog.search("てみる").isNotEmpty())
        assertTrue(GrammarCatalog.search("must").any { it.id == "grammar-nakereba-naranai" })
    }

    @Test
    fun searchWithBlankQueryIsEmpty() {
        assertEquals(emptyList(), GrammarCatalog.search("  "))
    }

    @Test
    fun patternKanaFormsStripTilde() {
        val pattern = GrammarCatalog.byId("grammar-te-miru")!!
        assertEquals(listOf("てみる"), pattern.kanaForms)
    }
}
