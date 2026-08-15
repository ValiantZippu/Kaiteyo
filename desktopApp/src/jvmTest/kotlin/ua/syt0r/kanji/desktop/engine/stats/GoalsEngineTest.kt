package ua.syt0r.kanji.desktop.engine.stats

import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.LocalDate
import ua.syt0r.kanji.desktop.model.StudyDaySummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Study Target math: today's reviews vs the configured daily review target.
 *
 * The Dashboard Study Target card and Goals card both derive from
 * [GoalsEngine.progress] over persisted `StudyDaySummary` rows — the target
 * comes from Settings → Statistics → Daily review target, and the achieved
 * count is the sum of today's real review totals. These tests lock down that
 * derivation: windowing, clamping, completion, and the configurable target.
 */
class GoalsEngineTest {

    private fun summary(
        day: String,
        reviewCount: Int = 0,
        newCount: Int = 0,
        timeMinutes: Long = 0
    ) = StudyDaySummary(
        day = day,
        reviewCount = reviewCount,
        newCount = newCount,
        timeSpent = timeMinutes.minutes
    )

    private fun dailyReviews(target: Int) =
        GoalsEngine.defaultGoals(dailyReviewTarget = target).first { it.id == "goal-daily-reviews" }

    // ------------------------------------------------------------
    // Daily reviews vs configured target
    // ------------------------------------------------------------

    @Test
    fun `daily reviews goal counts only today's reviews`() {
        val today = LocalDate(2026, 1, 10)
        val progress = GoalsEngine.progress(
            dailyReviews(20),
            listOf(
                summary("2026-01-10", reviewCount = 12), // today
                summary("2026-01-09", reviewCount = 40)  // yesterday — must not count
            ),
            today
        )

        assertEquals(12, progress.achieved)
        assertEquals(20, progress.target)
        assertEquals(0.6f, progress.fraction, 0.001f)
        assertFalse(progress.complete)
    }

    @Test
    fun `over target clamps fraction at one and marks complete`() {
        val today = LocalDate(2026, 1, 10)
        val progress = GoalsEngine.progress(
            dailyReviews(20),
            listOf(summary("2026-01-10", reviewCount = 30)),
            today
        )

        assertEquals(30, progress.achieved)
        assertEquals(1f, progress.fraction, 0.001f, "Fraction must clamp, not exceed 1")
        assertTrue(progress.complete)
    }

    @Test
    fun `empty summaries report zero achieved`() {
        val today = LocalDate(2026, 1, 10)
        val progress = GoalsEngine.progress(dailyReviews(20), emptyList(), today)

        assertEquals(0, progress.achieved)
        assertEquals(0f, progress.fraction, 0.001f)
        assertFalse(progress.complete)
    }

    @Test
    fun `configured daily target flows into the daily reviews goal`() {
        val goals = GoalsEngine.defaultGoals(dailyReviewTarget = 30)

        val reviewsGoal = goals.first { it.metric == GoalMetric.Reviews && it.period == GoalPeriod.Daily }
        assertEquals(30, reviewsGoal.target, "Settings target must drive the daily reviews goal")
        assertEquals(5, goals.first { it.metric == GoalMetric.NewCards }.target, "Other goals keep their defaults")
        assertEquals(15, goals.first { it.metric == GoalMetric.Minutes }.target)
    }

    @Test
    fun `non positive target yields zero fraction without division problems`() {
        val today = LocalDate(2026, 1, 10)
        val zeroGoal = GoalDef("zero-target", "Zero", GoalPeriod.Daily, 0, GoalMetric.Reviews)
        val progress = GoalsEngine.progress(zeroGoal, listOf(summary("2026-01-10", reviewCount = 5)), today)

        assertEquals(0f, progress.fraction, 0.001f, "No division by zero / NaN")
        assertTrue(progress.complete, "Any activity beats a zero target")
    }

    // ------------------------------------------------------------
    // Period windows
    // ------------------------------------------------------------

    @Test
    fun `weekly goal spans monday through sunday of the current week`() {
        // 2026-01-09 is a Friday; the week runs Mon 05 .. Sun 11.
        val today = LocalDate(2026, 1, 9)
        val weekly = GoalsEngine.defaultGoals().first { it.id == "goal-weekly-reviews" }
        val progress = GoalsEngine.progress(
            weekly,
            listOf(
                summary("2026-01-05", reviewCount = 10), // Monday — in week
                summary("2026-01-11", reviewCount = 5),  // Sunday — in week
                summary("2026-01-04", reviewCount = 99), // previous Sunday — excluded
                summary("2026-01-12", reviewCount = 99)  // next Monday — excluded
            ),
            today
        )

        assertEquals(15, progress.achieved)
    }

    @Test
    fun `malformed summary days are skipped not crashed`() {
        val today = LocalDate(2026, 1, 10)
        val progress = GoalsEngine.progress(
            dailyReviews(20),
            listOf(
                summary("2026-01-10", reviewCount = 7),
                StudyDaySummary(day = "not-a-date", reviewCount = 999),
                StudyDaySummary(day = "", reviewCount = 999)
            ),
            today
        )

        assertEquals(7, progress.achieved, "Unparseable rows must be ignored, not crash or count")
    }

    // ------------------------------------------------------------
    // Other metrics the study target panel shares
    // ------------------------------------------------------------

    @Test
    fun `new cards goal counts today's new cards`() {
        val today = LocalDate(2026, 1, 10)
        val goal = GoalsEngine.defaultGoals().first { it.metric == GoalMetric.NewCards }
        val progress = GoalsEngine.progress(goal, listOf(summary("2026-01-10", newCount = 3)), today)

        assertEquals(3, progress.achieved)
        assertEquals(5, progress.target)
        assertEquals(0.6f, progress.fraction, 0.001f)
    }

    @Test
    fun `minutes goal derives from persisted study time`() {
        val today = LocalDate(2026, 1, 10)
        val goal = GoalDef("minutes", "Minutes", GoalPeriod.Daily, 15, GoalMetric.Minutes)
        val progress = GoalsEngine.progress(
            goal,
            listOf(summary("2026-01-10", reviewCount = 0, timeMinutes = 47)),
            today
        )

        assertEquals(47, progress.achieved)
        assertEquals(1f, progress.fraction, 0.001f)
        assertTrue(progress.complete)
    }
}
