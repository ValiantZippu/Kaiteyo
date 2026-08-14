package ua.syt0r.kanji.core.statistics

import kotlinx.datetime.LocalDate

// ============================================================
// KNOWLEDGE GROWTH
// Cumulative progress series built from the precomputed daily
// rollups, so long histories stay cheap to render. Used for the
// "knowledge over time" charts (kanji/vocab, reviews, writing).
// ============================================================

/** One point on the cumulative growth curve. */
data class GrowthPoint(
    val date: LocalDate,
    val cumulativeReviews: Long = 0,
    val cumulativeNew: Long = 0,
    val cumulativeCorrect: Long = 0,
    val cumulativeWritingAttempts: Long = 0
)

object GrowthCalculator {

    /**
     * Builds the cumulative series from daily rollups (oldest → newest).
     * Days without a parseable date are skipped.
     */
    fun build(daily: List<DailyActivity>): List<GrowthPoint> {
        var reviews = 0L
        var newCards = 0L
        var correct = 0L
        var writing = 0L
        return daily
            .sortedWith(compareBy<DailyActivity> { it.date != null }.thenBy { it.date })
            .mapNotNull { day ->
                val date = day.date ?: return@mapNotNull null
                reviews += day.reviews
                newCards += day.newCards
                correct += day.correct
                writing += day.writingAttempts
                GrowthPoint(
                    date = date,
                    cumulativeReviews = reviews,
                    cumulativeNew = newCards,
                    cumulativeCorrect = correct,
                    cumulativeWritingAttempts = writing
                )
            }
    }

    /** Down-samples a long series to at most [maxPoints] points for charts. */
    fun sample(points: List<GrowthPoint>, maxPoints: Int = 60): List<GrowthPoint> {
        if (points.size <= maxPoints) return points
        val step = points.size.toDouble() / maxPoints
        return (0 until maxPoints).map { i ->
            points[(i * step).toInt()]
        }.plus(points.last())
    }
}
