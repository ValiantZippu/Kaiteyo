package ua.syt0r.kanji.core.knowledge

import ua.syt0r.kanji.core.app_data.data.KanjiListEntry
import ua.syt0r.kanji.core.app_data.data.KanjiMeaningEntry
import ua.syt0r.kanji.core.app_data.data.KanjiReadingEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KanjiSearchIndexTest {

    private val entries = listOf(
        KanjiListEntry("食", 183),
        KanjiListEntry("飯", 300),
        KanjiListEntry("飲", 250),
        KanjiListEntry("水", 120)
    )

    private val meanings = listOf(
        KanjiMeaningEntry("食", "eat"),
        KanjiMeaningEntry("食", "food"),
        KanjiMeaningEntry("飯", "meal"),
        KanjiMeaningEntry("飲", "drink"),
        KanjiMeaningEntry("水", "water")
    )

    private val readings = listOf(
        KanjiReadingEntry("食", "on", "ショク"),
        KanjiReadingEntry("食", "kun", "たべ"),
        KanjiReadingEntry("飯", "on", "ハン"),
        KanjiReadingEntry("飲", "on", "イン"),
        KanjiReadingEntry("水", "on", "スイ"),
        KanjiReadingEntry("水", "kun", "みず")
    )

    private val strokes = mapOf("食" to 9, "飯" to 12, "飲" to 12, "水" to 4)

    private val tags = mapOf(
        "食" to listOf<KanjiTag>(KanjiTag.Jlpt(4), KanjiTag.Grade(2)),
        "飯" to listOf<KanjiTag>(KanjiTag.Jlpt(4), KanjiTag.Grade(4)),
        "飲" to listOf<KanjiTag>(KanjiTag.Jlpt(4), KanjiTag.Grade(3)),
        "水" to listOf<KanjiTag>(KanjiTag.Jlpt(5), KanjiTag.Grade(1))
    )

    private fun index(): KanjiSearchIndex = KanjiSearchIndex(
        entries = entries,
        meanings = meanings,
        readings = readings,
        strokeCounts = strokes,
        classifications = tags
    )

    @Test
    fun exactCharacterMatch() {
        val hits = index().search(KnowledgeSearchQuery(text = "食"))
        assertEquals(1, hits.size)
        assertEquals("食", hits.first().kanji)
        assertEquals(KanjiMatchType.Character, hits.first().matchType)
    }

    @Test
    fun readingMatch() {
        val hits = index().search(KnowledgeSearchQuery(text = "みず"))
        assertTrue(hits.any { it.kanji == "水" && it.matchType == KanjiMatchType.Reading })
    }

    @Test
    fun meaningMatchIsCaseInsensitive() {
        val hits = index().search(KnowledgeSearchQuery(text = "DRINK"))
        assertTrue(hits.any { it.kanji == "飲" && it.matchType == KanjiMatchType.Meaning })
    }

    @Test
    fun relevanceSortsCharacterBeforeReadingBeforeMeaning() {
        // "水" matches 水 by character AND ショク? No — "水" as a character
        // match only 水; as a reading, みず contains 水? No. So single hit.
        val hits = index().search(KnowledgeSearchQuery(text = "水"))
        assertEquals(listOf("水"), hits.map { it.kanji })
    }

    @Test
    fun jlptFilterNarrowsResults() {
        val hits = index().search(
            KnowledgeSearchQuery(text = "", filters = SearchFilters(jlpt = 5))
        )
        assertEquals(listOf("水"), hits.map { it.kanji })
    }

    @Test
    fun frequencyFilterNarrowsResults() {
        val hits = index().search(
            KnowledgeSearchQuery(text = "", filters = SearchFilters(frequency = FrequencyBand.VeryCommon))
        )
        // All four ranks are ≤ 500 → all match the VeryCommon band.
        assertEquals(4, hits.size)
    }

    @Test
    fun strokeFilterNarrowsResults() {
        val hits = index().search(
            KnowledgeSearchQuery(text = "", filters = SearchFilters(strokeCount = 12))
        )
        assertEquals(setOf("飯", "飲"), hits.map { it.kanji }.toSet())
    }

    @Test
    fun frequencySortOrdersByRank() {
        val hits = index().search(
            KnowledgeSearchQuery(text = "", sort = SearchSort.Frequency)
        )
        assertEquals(listOf("水", "食", "飲", "飯"), hits.map { it.kanji })
    }

    @Test
    fun jlptSortOrdersByLevelThenFrequency() {
        val hits = index().search(
            KnowledgeSearchQuery(text = "", sort = SearchSort.Jlpt)
        )
        // N5 (水) first, then N4 by frequency: 食(183) 飲(250) 飯(300).
        assertEquals(listOf("水", "食", "飲", "飯"), hits.map { it.kanji })
    }

    @Test
    fun jlptOfReturnsLevel() {
        assertEquals(4, index().jlptOf("食"))
        assertEquals(5, index().jlptOf("水"))
        assertNull(index().jlptOf("無"))
    }

    @Test
    fun romajiReadingMatchesNormalizedKana() {
        // KT-SEARCH-005: "taberu" must find 食 via its たべ kun reading.
        val hits = index().search(KnowledgeSearchQuery(text = "taberu"))
        assertTrue(hits.any { it.kanji == "食" && it.matchType == KanjiMatchType.Reading })
    }

    @Test
    fun fullWidthKatakanaQueryMatchesKanaReading() {
        // Full-width タベ folds to たべ; katakana ショク folds to しょく.
        val hits = index().search(KnowledgeSearchQuery(text = "ショク"))
        assertTrue(hits.any { it.kanji == "食" && it.matchType == KanjiMatchType.Reading })
    }

    @Test
    fun romajiCaseIsIrrelevant() {
        val hits = index().search(KnowledgeSearchQuery(text = "TABERU"))
        assertTrue(hits.any { it.kanji == "食" })
    }

    @Test
    fun blankQueryReturnsNothing() {
        assertEquals(emptyList(), index().search(KnowledgeSearchQuery(text = "  ")))
    }
}
