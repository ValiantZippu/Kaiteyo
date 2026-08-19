package ua.syt0r.kanji.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Query-parser tests (KT-SEARCH-002, spec §15): \"common verbs N3\" becomes a
 * real SearchFilters intersection, unknown tokens stay in the free text, and
 * nothing is silently dropped.
 */
class KnowledgeQueryParserTest {

    @Test
    fun plainTextStaysUnchanged() {
        val parsed = KnowledgeQueryParser.parse("食べる")
        assertEquals("食べる", parsed.text)
        assertEquals(SearchFilters.None, parsed.filters)
        assertTrue(parsed.chips.isEmpty())
    }

    @Test
    fun structuredQueryProducesRealFilters() {
        val parsed = KnowledgeQueryParser.parse("common verbs N3")
        assertEquals("", parsed.text)
        assertEquals(3, parsed.filters.jlpt)
        // Plural stem is normalized so it matches real POS tags like
        // "Ichidan verb"; the chip keeps what the user typed.
        assertEquals("verb", parsed.filters.partOfSpeech)
        assertEquals(FrequencyBand.Common, parsed.filters.frequency)
        assertEquals(listOf("Common", "verbs", "JLPT:N3"), parsed.chips)
    }

    @Test
    fun explicitJpLtSyntax() {
        val parsed = KnowledgeQueryParser.parse("jlpt:n2")
        assertEquals(2, parsed.filters.jlpt)
        assertEquals(listOf("JLPT:N2"), parsed.chips)
    }

    @Test
    fun strokesForms() {
        assertEquals(4, KnowledgeQueryParser.parse("strokes:4").filters.strokeCount)
        assertEquals(7, KnowledgeQueryParser.parse("7 strokes").filters.strokeCount)
        assertEquals(9, KnowledgeQueryParser.parse("9-stroke").filters.strokeCount)
    }

    @Test
    fun gradeAndFrequencyForms() {
        val grade = KnowledgeQueryParser.parse("grade:2")
        assertEquals(2, grade.filters.grade)
        val frequency = KnowledgeQueryParser.parse("frequency:rare")
        assertEquals(FrequencyBand.Rare, frequency.filters.frequency)
        val bare = KnowledgeQueryParser.parse("rare")
        assertEquals(FrequencyBand.Rare, bare.filters.frequency)
    }

    @Test
    fun mixedTextAndFiltersIntersect() {
        val parsed = KnowledgeQueryParser.parse("食べる N5 verb")
        assertEquals("食べる", parsed.text)
        assertEquals(5, parsed.filters.jlpt)
        assertEquals("verb", parsed.filters.partOfSpeech)
    }

    @Test
    fun unknownTokensStayInText() {
        val parsed = KnowledgeQueryParser.parse("wavelength sushi N1")
        assertEquals("wavelength sushi", parsed.text)
        assertEquals(1, parsed.filters.jlpt)
    }

    @Test
    fun blankQueryHasNoFilters() {
        val parsed = KnowledgeQueryParser.parse("   ")
        assertEquals("", parsed.text)
        assertFalse(parsed.hasFilters)
        assertNull(parsed.filters.jlpt)
    }

    @Test
    fun caseInsensitiveKeywords() {
        val parsed = KnowledgeQueryParser.parse("COMMON NOUN n4")
        assertEquals(FrequencyBand.Common, parsed.filters.frequency)
        assertEquals("NOUN", parsed.filters.partOfSpeech)
        assertEquals(4, parsed.filters.jlpt)
    }

    @Test
    fun studyStateKeywordsProduceFilterAndChips() {
        val known = KnowledgeQueryParser.parse("known")
        assertEquals(StudyState.Known, known.filters.studyState)
        assertTrue(known.chips.contains("Known"))

        val due = KnowledgeQueryParser.parse("N3 due")
        assertEquals(3, due.filters.jlpt)
        assertEquals(StudyState.Due, due.filters.studyState)

        val learning = KnowledgeQueryParser.parse("learning 食")
        assertEquals("食", learning.text)
        assertEquals(StudyState.Learning, learning.filters.studyState)
    }

    @Test
    fun studyStateIsCaseInsensitive() {
        val parsed = KnowledgeQueryParser.parse("MASTERED")
        assertEquals(StudyState.Mastered, parsed.filters.studyState)
    }
}
