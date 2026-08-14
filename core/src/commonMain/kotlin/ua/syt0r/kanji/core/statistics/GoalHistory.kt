package ua.syt0r.kanji.core.statistics

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

// ============================================================
// GOAL HISTORY
// Append-only snapshots of the goal set, recorded whenever the
// goal list changes. Enables completion trends and streaks
// without reconstructing historical state from the current
// deck state (which would be wrong for past days). Pure
// domain logic — no UI, no database dependency.
// ============================================================

/** One goal's state at snapshot time. */
@Serializable
data class GoalSnapshot(
    val id: String = "",
    val type: String = "",
    val period: String = "",
    val target: Int = 1,
    val current: Int = 0,
    val completed: Boolean = false
) {
    val fraction: Float get() = if (target <= 0) 0f else (current.toFloat() / target).coerceIn(0f, 1f)
}

/** A full snapshot of the goal set taken on one local day. */
@Serializable
data class GoalHistoryEntry(
    val date: String = "",
    val snapshots: List<GoalSnapshot> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0
) {
    val completionRatio: Float get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount
    val allCompleted: Boolean get() = totalCount > 0 && completedCount == totalCount
}

object GoalHistory {

    /** Upper bound on stored snapshots; older entries are dropped. */
    const val MaxEntries = 180

    /** Appends a snapshot of [progress] for [date], keeping at most [MaxEntries]. */
    fun record(
        entries: List<GoalHistoryEntry>,
        date: LocalDate,
        progress: List<GoalProgress>
    ): List<GoalHistoryEntry> {
        val entry = GoalHistoryEntry(
            date = date.toString(),
            snapshots = progress.map { p ->
                GoalSnapshot(
                    id = p.goal.id,
                    type = p.goal.type.name,
                    period = p.goal.period.name,
                    target = p.target,
                    current = p.current,
                    completed = p.completed
                )
            },
            completedCount = progress.count { it.completed },
            totalCount = progress.size
        )
        return (entries + entry).takeLast(MaxEntries)
    }

    /** Completion ratio per snapshot day over the trailing [window] entries. */
    fun completionTrend(entries: List<GoalHistoryEntry>, window: Int = 14): List<Pair<String, Float>> =
        entries.takeLast(window).map { it.date to it.completionRatio }

    /** Longest run of consecutive snapshots where every goal was completed. */
    fun longestCompletionStreak(entries: List<GoalHistoryEntry>): Int {
        var best = 0
        var current = 0
        entries.forEach { entry ->
            if (entry.allCompleted) {
                current += 1
                if (current > best) best = current
            } else {
                current = 0
            }
        }
        return best
    }

    /** Goal types that most often failed to complete — weak spots over time. */
    fun weakestGoalTypes(entries: List<GoalHistoryEntry>, limit: Int = 3): List<Pair<String, Int>> =
        entries
            .flatMap { entry -> entry.snapshots.filter { !it.completed } }
            .groupingBy { it.type }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(limit)
}
