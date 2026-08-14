package ua.syt0r.kanji.core.statistics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeeklyExamTest {

    private fun item(
        key: String,
        contentType: String,
        jlpt: Int? = null,
        content: String = key
    ) = ExamSourceItem(
        key = key,
        content = content,
        reading = "よみ",
        meaning = "meaning",
        jlptLevel = jlpt,
        contentType = contentType,
        radical = null,
        strokeCount = null,
        studied = true
    )

    @Test
    fun configTargetsLastSevenDays() {
        val config = WeeklyExam.config(questionCount = 15, seed = 42L)
        assertEquals("Weekly Exam", config.title)
        assertEquals(15, config.questionCount)
        assertEquals(7, config.studiedWithinDays)
        assertEquals(42L, config.seed)
    }

    @Test
    fun summaryCountsContentAndJlpt() {
        val summary = WeeklyExam.summarize(
            listOf(
                item("日", ContentTypes.KANJI, jlpt = 5),
                item("食", ContentTypes.KANJI, jlpt = 5),
                item("食べる", ContentTypes.VOCAB, jlpt = 5),
                item("簡単", ContentTypes.VOCAB, jlpt = 4)
            )
        )
        assertEquals(2, summary.kanjiStudied)
        assertEquals(2, summary.vocabStudied)
        assertEquals(4, summary.total)
        assertEquals(2, summary.byJlpt.first { it.first == 5 }.second)
        assertTrue(summary.hasContent)
    }

    @Test
    fun emptyItemsMeanNoContent() {
        val summary = WeeklyExam.summarize(emptyList())
        assertFalse(summary.hasContent)
        assertEquals(0, summary.total)
    }

    @Test
    fun jlptBandsWithoutItemsAreOmitted() {
        val summary = WeeklyExam.summarize(
            listOf(item("日", ContentTypes.KANJI, jlpt = 5))
        )
        assertEquals(listOf(5 to 1), summary.byJlpt)
    }
}
