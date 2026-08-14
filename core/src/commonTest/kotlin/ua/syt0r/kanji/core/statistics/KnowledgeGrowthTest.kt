package ua.syt0r.kanji.core.statistics

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

class KnowledgeGrowthTest {

    private fun day(date: LocalDate, reviews: Int, newCards: Int, correct: Int, writing: Int) =
        DailyActivity(
            date = date,
            reviews = reviews,
            newCards = newCards,
            correct = correct,
            studyTime = 1.hours,
            writingAttempts = writing
        )

    @Test
    fun cumulativeSeriesAccumulatesInDateOrder() {
        val daily = listOf(
            day(LocalDate(2024, 1, 3), reviews = 5, newCards = 2, correct = 4, writing = 1),
            day(LocalDate(2024, 1, 1), reviews = 10, newCards = 3, correct = 8, writing = 2),
            day(LocalDate(2024, 1, 2), reviews = 7, newCards = 1, correct = 6, writing = 0)
        )
        val growth = GrowthCalculator.build(daily)
        assertEquals(3, growth.size)
        assertEquals(LocalDate(2024, 1, 1), growth.first().date)
        assertEquals(10L, growth.first().cumulativeReviews)
        assertEquals(3L, growth.first().cumulativeNew)
        // After all three days: 10 + 7 + 5 = 22 reviews.
        assertEquals(22L, growth.last().cumulativeReviews)
        assertEquals(6L, growth.last().cumulativeNew)
        assertEquals(18L, growth.last().cumulativeCorrect)
        assertEquals(3L, growth.last().cumulativeWritingAttempts)
    }

    @Test
    fun emptyInputProducesEmptySeries() {
        assertEquals(emptyList(), GrowthCalculator.build(emptyList()))
    }

    @Test
    fun samplingKeepsSeriesWhenUnderMax() {
        val points = (1..10).map { GrowthPoint(LocalDate(2024, 1, it), cumulativeReviews = it.toLong()) }
        assertEquals(points, GrowthCalculator.sample(points, maxPoints = 60))
    }

    @Test
    fun samplingDownsizesLongSeries() {
        val points = (1..100).map { GrowthPoint(LocalDate(2024, 1, it), cumulativeReviews = it.toLong()) }
        val sampled = GrowthCalculator.sample(points, maxPoints = 10)
        assertEquals(11, sampled.size) // 10 samples + the final point
        assertEquals(points.last(), sampled.last())
    }
}
