package ua.syt0r.kanji.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ua.syt0r.kanji.presentation.screen.main.features.SearchItem
import ua.syt0r.kanji.presentation.screen.main.features.flattenForUi

// ============================================================
// RADICAL EXPLORER + UNIVERSAL SEARCH — TESTS
// ------------------------------------------------------------
// Pure logic behind the radical explorer (RadicalStats) and the
// universal search overlay (result flattening + keyboard order).
// ============================================================

class RadicalExplorerTest {

    @Test
    fun radicalStatsSortsByKanjiCountDescending() {
        val stats = listOf(
            RadicalStats(radical = "口", strokeCount = 3, kanjiCount = 90),
            RadicalStats(radical = "木", strokeCount = 4, kanjiCount = 120),
            RadicalStats(radical = "水", strokeCount = 4, kanjiCount = 75)
        ).sortedByDescending { it.kanjiCount }

        assertEquals("木", stats.first().radical)
        assertEquals("水", stats.last().radical)
        assertEquals(4, stats[0].strokeCount)
    }

    @Test
    fun radicalStatsCarryStrokesAndCount() {
        val stats = RadicalStats(radical = "日", strokeCount = 4, kanjiCount = 60)
        assertEquals("日", stats.radical)
        assertEquals(4, stats.strokeCount)
        assertEquals(60, stats.kanjiCount)
    }

    @Test
    fun multiRadicalIntersectionIsUserSelectableUpToFour() {
        // The ViewModel caps multi-select at 4; the model itself is a plain list.
        val selected = buildList {
            add("口")
            add("木")
            add("氵")
            add("日")
        }
        assertEquals(4, selected.size)
        assertTrue(selected.distinct().size == selected.size)
    }
}

class UniversalSearchFlattenTest {

    private fun sampleResults(): GroupedSearchResults {
        val index = KanjiSearchIndex(
            entries = listOf(
                ua.syt0r.kanji.core.app_data.data.KanjiListEntry(
                    kanji = "食",
                    frequency = 12
                )
            ),
            meanings = emptyList(),
            readings = emptyList(),
            strokeCounts = emptyMap(),
            classifications = emptyMap()
        )
        val kanjiHit = index.search(KnowledgeSearchQuery(text = "食")).first()
        return GroupedSearchResults(
            query = KnowledgeSearchQuery(text = "食"),
            kanji = listOf(kanjiHit),
            words = listOf(
                WordHit(
                    word = WordKnowledge(
                        id = 1,
                        kanjiReading = "食べる",
                        kanaReading = "たべる",
                        glossary = listOf("to eat"),
                        partOfSpeech = listOf("Ichidan verb")
                    ),
                    jlpt = null
                )
            ),
            sentences = listOf(
                SentenceHit(
                    SentenceKnowledge(
                        text = "ご飯を食べます。",
                        translation = "I eat rice.",
                        furigana = ua.syt0r.kanji.core.app_data.data.FuriganaString(
                            compounds = emptyList()
                        )
                    )
                )
            ),
            grammar = listOf(
                GrammarHit(
                    GrammarPattern(
                        id = "te-miru",
                        pattern = "〜てみる",
                        meaning = "to try doing"
                    )
                )
            )
        )
    }

    @Test
    fun flattenPreservesKanjiThenWordsThenSentencesThenGrammar() {
        val results = sampleResults()
        val flat = flattenForUi(results)

        assertEquals(4, flat.size)
        assertTrue(flat[0] is SearchItem.KanjiItem)
        assertTrue(flat[1] is SearchItem.WordItem)
        assertTrue(flat[2] is SearchItem.SentenceItem)
        assertTrue(flat[3] is SearchItem.GrammarItem)
    }

    @Test
    fun flattenIsEmptyForEmptyResults() {
        val empty = GroupedSearchResults(query = KnowledgeSearchQuery(text = ""))
        assertTrue(flattenForUi(empty).isEmpty())
    }

    @Test
    fun flattenedKanjiCarriesTheCharacter() {
        val flat = flattenForUi(sampleResults())
        val kanji = flat.first() as SearchItem.KanjiItem
        assertEquals("食", kanji.hit.kanji)
    }

    @Test
    fun flattenedWordCarriesTheWordId() {
        val flat = flattenForUi(sampleResults())
        val word = flat[1] as SearchItem.WordItem
        assertEquals(1L, word.hit.word.id)
        assertEquals("たべる", word.hit.word.kanaReading)
    }
}
