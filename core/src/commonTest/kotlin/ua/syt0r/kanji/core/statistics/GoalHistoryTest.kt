package ua.syt0r.kanji.core.statistics

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class GoalHistoryTest {

    private fun goal(id: String) = LearningGoal(id = id, type = GoalType.DailyReviews, target = 10)

    private fun progress(goal: LearningGoal, current: Int) =
        GoalProgress(goal = goal, current = current, target = goal.target, completed = current >= goal.target)

    @Test
    fun recordAppendsSnapshotsNewestLast() {
        val g = goal("a")
        val first = GoalHistory.record(emptyList(), LocalDate(2024, 1, 1), listOf(progress(g, 10)))
        val second = GoalHistory.record(first, LocalDate(2024, 1, 2), listOf(progress(g, 3)))
        assertEquals(2, second.size)
        assertEquals("2024-01-01", second.first().date)
        assertEquals("2024-01-02", second.last().date)
    }

    @Test
    fun completionRatioReflectsSnapshots() {
        val g1 = goal("a")
        val g2 = goal("b")
        val entry = GoalHistory.record(emptyList(), LocalDate(2024, 1, 1), listOf(progress(g1, 10), progress(g2, 2))).single()
        assertEquals(1, entry.completedCount)
        assertEquals(2, entry.totalCount)
        assertEquals(0.5f, entry.completionRatio)
        assertEquals(false, entry.allCompleted)
    }

    @Test
    fun historyIsBounded() {
        val g = goal("a")
        var entries = emptyList<GoalHistoryEntry>()
        for (day in 1..200) {
            entries = GoalHistory.record(entries, LocalDate(2024, 1, day), listOf(progress(g, 10)))
        }
        assertEquals(GoalHistory.MaxEntries, entries.size)
        assertEquals("2024-01-21", entries.first().date) // 200 - 180 + 1
    }

    @Test
    fun completionStreakCountsConsecutiveAllCompletedDays() {
        val g = goal("a")
        fun entry(day: Int, complete: Boolean) =
            GoalHistory.record(emptyList(), LocalDate(2024, 1, day), listOf(progress(g, if (complete) 10 else 0))).single()
        val history = listOf(entry(1, true), entry(2, true), entry(3, false), entry(4, true))
        assertEquals(2, GoalHistory.longestCompletionStreak(history))
    }

    @Test
    fun weakestGoalTypesTrackRecurringFailures() {
        val g = goal("a")
        val weak = LearningGoal(id = "w", type = GoalType.NewKanji, target = 5)
        val history = listOf(
            GoalHistory.record(emptyList(), LocalDate(2024, 1, 1), listOf(progress(g, 10), progress(weak, 0))).single(),
            GoalHistory.record(emptyList(), LocalDate(2024, 1, 2), listOf(progress(g, 10), progress(weak, 1))).single()
        )
        val weakest = GoalHistory.weakestGoalTypes(history)
        assertEquals("NewKanji", weakest.first().first)
        assertEquals(2, weakest.first().second)
    }

    @Test
    fun emptyHistoryHasNoTrends() {
        assertEquals(emptyList(), GoalHistory.completionTrend(emptyList()))
        assertEquals(0, GoalHistory.longestCompletionStreak(emptyList()))
        assertEquals(emptyList(), GoalHistory.weakestGoalTypes(emptyList()))
    }
}
