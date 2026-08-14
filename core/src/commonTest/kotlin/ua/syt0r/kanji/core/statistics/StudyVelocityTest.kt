package ua.syt0r.kanji.core.statistics

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class StudyVelocityTest {

    private fun day(date: LocalDate, reviews: Int, newCards: Int = 0, studyHours: Int = 0, writing: Int = 0) =
        DailyActivity(
            date = date,
            reviews = reviews,
            newCards = newCards,
            correct = reviews,
            studyTime = studyHours.hours,
            writingAttempts = writing
        )

    private fun exam(id: Long, accuracy: Float, startMs: Long) = ExamRecord(
        id = id,
        title = "Exam $id",
        startedAt = Instant.fromEpochMilliseconds(startMs),
        status = ExamStatus.Completed,
        accuracy = accuracy,
        score = (accuracy * 10).toInt()
    )

    @Test
    fun ratesAreComputedOverTheWindow() {
        val daily = (1..7).map { day(LocalDate(2024, 1, it), reviews = 10, newCards = 1, studyHours = 1) }
        val velocity = VelocityCalculator.build(daily, emptyList(), windowDays = 7)

        assertEquals(10f, velocity.reviewsPerDay)
        assertEquals(7f, velocity.newItemsPerWeek)
        assertEquals(7f, velocity.studyHoursPerWeek)
        assertTrue(velocity.hasData)
    }

    @Test
    fun zeroDataStaysZero() {
        val velocity = VelocityCalculator.build(emptyList(), emptyList(), windowDays = 30)
        assertEquals(0f, velocity.reviewsPerDay)
        assertEquals(0f, velocity.newItemsPerWeek)
        assertEquals(0f, velocity.studyHoursPerWeek)
        assertTrue(!velocity.hasData)
    }

    @Test
    fun examDeltaComparesFirstHalfToSecondHalf() {
        val exams = listOf(
            exam(1, 0.5f, 1_000), exam(2, 0.5f, 2_000),
            exam(3, 0.8f, 3_000), exam(4, 0.8f, 4_000)
        )
        val velocity = VelocityCalculator.build(emptyList(), exams, windowDays = 30)
        assertEquals(30f, velocity.examScoreDelta)
        assertEquals(4f, velocity.examsPerMonth)
    }

    @Test
    fun examDeltaNeedsAtLeastFourExams() {
        val exams = listOf(exam(1, 0.5f, 1_000), exam(2, 0.9f, 2_000))
        val velocity = VelocityCalculator.build(emptyList(), exams, windowDays = 30)
        assertNull(velocity.examScoreDelta)
    }

    @Test
    fun invalidWindowReturnsEmptyMetrics() {
        val velocity = VelocityCalculator.build(emptyList(), emptyList(), windowDays = 0)
        assertEquals(0, velocity.windowDays)
    }

    @Test
    fun oneDecimalFormatsCleanly() {
        assertEquals("10", VelocityCalculator.oneDecimal(10f))
        assertEquals("10.5", VelocityCalculator.oneDecimal(10.5f))
        assertEquals("0", VelocityCalculator.oneDecimal(0f))
    }
}
