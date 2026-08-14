package ua.syt0r.kanji.core.statistics

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for the per-day "cards practiced" breakdown — the data behind the
 * heatmap day drill-down. The function is pure: identical review history in,
 * identical breakdown out.
 */
class DayPracticeBreakdownTest {

    private val kanjiTypes = setOf(0L, 1L)   // LetterWriting, LetterReading
    private val writingTypes = setOf(0L, 12L) // LetterWriting, VocabWriting

    private fun review(
        key: String,
        practiceType: Long,
        grade: Int,
        mistakes: Int = 0,
        time: Long = 1_700_000_000_000L
    ) = ua.syt0r.kanji.core.user_data.database.ReviewHistoryItem(
        key = key,
        practiceType = practiceType,
        timestamp = Instant.fromEpochMilliseconds(time),
        duration = 3.seconds,
        grade = grade,
        mistakes = mistakes,
        interval = 1L,
        deckId = 1L
    )

    private val today = LocalDate(2026, 8, 10)

    @Test
    fun emptyHistoryProducesEmptyBreakdown() {
        val breakdown = StatisticsCalculator.buildDayPracticeBreakdown(
            date = today,
            reviews = emptyList(),
            kanjiTypes = kanjiTypes,
            writingTypes = writingTypes
        )
        assertTrue(breakdown.isEmpty)
        assertEquals(0, breakdown.totalReviews)
        assertEquals(0, breakdown.correct)
        assertEquals(0, breakdown.totalMistakes)
        assertEquals(0f, breakdown.accuracy)
        assertEquals(today, breakdown.date)
    }

    @Test
    fun groupsByKeyAndPracticeType() {
        val reviews = listOf(
            review("食", 0L, grade = 3),           // kanji writing
            review("食", 0L, grade = 2),           // same key+type → merged
            review("食", 1L, grade = 1),           // kanji reading → separate item
            review("食べる", 10L, grade = 3)         // vocab
        )
        val breakdown = StatisticsCalculator.buildDayPracticeBreakdown(
            date = today,
            reviews = reviews,
            kanjiTypes = kanjiTypes,
            writingTypes = writingTypes
        )
        assertFalse(breakdown.isEmpty)
        assertEquals(4, breakdown.totalReviews)
        assertEquals(3, breakdown.correct)

        assertEquals(2, breakdown.kanji.size)
        val writing = breakdown.kanji.first { it.practiceType == 0L }
        assertEquals("食", writing.content)
        assertEquals(2, writing.count)
        assertEquals(2, writing.correct)
        assertEquals(3, writing.lastGrade)

        val reading = breakdown.kanji.first { it.practiceType == 1L }
        assertEquals(1, reading.count)
        assertEquals(0, reading.correct)
        assertEquals("Reading", reading.practiceLabel)

        assertEquals(1, breakdown.vocab.size)
        assertEquals("食べる", breakdown.vocab.first().content)
        assertEquals(ContentTypes.VOCAB, breakdown.vocab.first().contentType)
        assertEquals("Flashcard", breakdown.vocab.first().practiceLabel)
    }

    @Test
    fun writingIsASubsetOfKanjiAndVocab() {
        val reviews = listOf(
            review("行", 0L, grade = 2),      // kanji writing
            review("行", 1L, grade = 3),      // kanji reading
            review("行く", 12L, grade = 2)     // vocab writing
        )
        val breakdown = StatisticsCalculator.buildDayPracticeBreakdown(
            date = today,
            reviews = reviews,
            kanjiTypes = kanjiTypes,
            writingTypes = writingTypes
        )
        assertEquals(2, breakdown.writing.size)
        assertEquals(setOf("行", "行く"), breakdown.writing.map { it.content }.toSet())
        // 行/0 and 行/1 are distinct (key, practiceType) groups → two kanji items.
        assertEquals(2, breakdown.kanji.size)
        assertEquals(1, breakdown.vocab.size)
    }

    @Test
    fun correctCountsUseGradeGreaterThanOne() {
        val reviews = listOf(
            review("本", 1L, grade = 0),  // Again
            review("本", 1L, grade = 1),  // Hard
            review("本", 1L, grade = 2),  // Good
            review("本", 1L, grade = 3)   // Easy
        )
        val breakdown = StatisticsCalculator.buildDayPracticeBreakdown(
            date = today,
            reviews = reviews,
            kanjiTypes = kanjiTypes,
            writingTypes = writingTypes
        )
        val item = breakdown.kanji.single()
        assertEquals(4, item.count)
        assertEquals(2, item.correct)
        assertEquals(0.5f, item.accuracy)
        assertEquals(3, item.lastGrade)
        assertEquals(0.5f, breakdown.accuracy)
    }

    @Test
    fun mistakesAreSummed() {
        val reviews = listOf(
            review("人", 0L, grade = 1, mistakes = 2),
            review("人", 0L, grade = 2, mistakes = 1)
        )
        val breakdown = StatisticsCalculator.buildDayPracticeBreakdown(
            date = today,
            reviews = reviews,
            kanjiTypes = kanjiTypes,
            writingTypes = writingTypes
        )
        assertEquals(3, breakdown.totalMistakes)
        assertEquals(3, breakdown.kanji.single().mistakes)
    }

    @Test
    fun contentResolverPopulatesDisplayFields() {
        val reviews = listOf(review("食", 1L, grade = 3))
        val breakdown = StatisticsCalculator.buildDayPracticeBreakdown(
            date = today,
            reviews = reviews,
            kanjiTypes = kanjiTypes,
            writingTypes = writingTypes,
            contentResolver = { key, _ ->
                when (key) {
                    "食" -> Triple("食", "たべ", "to eat; food")
                    else -> Triple(key, "", "")
                }
            }
        )
        val item = breakdown.kanji.single()
        assertEquals("食", item.content)
        assertEquals("たべ", item.reading)
        assertEquals("to eat; food", item.meaning)
    }

    @Test
    fun defaultResolverUsesKeyAsContent() {
        val reviews = listOf(review("花", 1L, grade = 2))
        val breakdown = StatisticsCalculator.buildDayPracticeBreakdown(
            date = today,
            reviews = reviews,
            kanjiTypes = kanjiTypes,
            writingTypes = writingTypes
        )
        assertEquals("花", breakdown.kanji.single().content)
        assertEquals("", breakdown.kanji.single().reading)
    }

    @Test
    fun itemsSortByCountThenContent() {
        val reviews = listOf(
            review("一", 1L, grade = 2),
            review("一", 1L, grade = 2),
            review("九", 1L, grade = 2),
            review("十", 1L, grade = 3),
            review("十", 1L, grade = 2)
        )
        val breakdown = StatisticsCalculator.buildDayPracticeBreakdown(
            date = today,
            reviews = reviews,
            kanjiTypes = kanjiTypes,
            writingTypes = writingTypes
        )
        // Two counts of 2 (一, 十) come first, ordered by content; then 九.
        assertEquals(listOf("一", "十", "九"), breakdown.kanji.map { it.content })
    }

    @Test
    fun unknownPracticeTypeFallsBackToVocab() {
        val reviews = listOf(review("custom", 99L, grade = 3))
        val breakdown = StatisticsCalculator.buildDayPracticeBreakdown(
            date = today,
            reviews = reviews,
            kanjiTypes = kanjiTypes,
            writingTypes = writingTypes
        )
        assertTrue(breakdown.kanji.isEmpty())
        assertEquals(1, breakdown.vocab.size)
        assertEquals(ContentTypes.VOCAB, breakdown.vocab.single().contentType)
        assertEquals("Practice", breakdown.vocab.single().practiceLabel)
    }

    @Test
    fun practiceTypeLabels() {
        assertEquals("Writing", StatisticsCalculator.practiceTypeLabel(0L))
        assertEquals("Reading", StatisticsCalculator.practiceTypeLabel(1L))
        assertEquals("Flashcard", StatisticsCalculator.practiceTypeLabel(10L))
        assertEquals("Reading picker", StatisticsCalculator.practiceTypeLabel(11L))
        assertEquals("Writing", StatisticsCalculator.practiceTypeLabel(12L))
        assertEquals("Practice", StatisticsCalculator.practiceTypeLabel(77L))
    }
}
