package ua.syt0r.kanji.core.statistics

import kotlin.math.roundToInt
import kotlin.time.Duration

// ============================================================
// LEARNING VELOCITY
// Rate-of-progress metrics over a configurable time window.
// "New items per week", "reviews per day", "study hours per
// week", etc. — all derived from the daily rollups and exam
// records, never estimated.
// ============================================================

/** Rate metrics over a trailing window of local days. */
data class VelocityMetrics(
    /** The trailing window (in days) these metrics were computed over. */
    val windowDays: Int = 0,
    val reviewsPerDay: Float = 0f,
    val newItemsPerWeek: Float = 0f,
    val studyHoursPerWeek: Float = 0f,
    val writingAttemptsPerWeek: Float = 0f,
    val examsPerMonth: Float = 0f,
    /**
     * Change in average exam score between the first half and the second
     * half of the completed exams in the window (percentage points).
     * Null when there are fewer than 4 completed exams.
     */
    val examScoreDelta: Float? = null
) {
    val hasData: Boolean get() = windowDays > 0 && (reviewsPerDay > 0f || writingAttemptsPerWeek > 0f)
}

object VelocityCalculator {

    /**
     * Computes rate metrics from the per-day rollups ([daily], already
     * restricted to the trailing window by the caller) and the completed
     * exams inside that window.
     */
    fun build(
        daily: List<DailyActivity>,
        completedExams: List<ExamRecord>,
        windowDays: Int
    ): VelocityMetrics {
        if (windowDays <= 0) return VelocityMetrics(windowDays = windowDays)

        val totalReviews = daily.sumOf { it.reviews.toLong() }
        val totalNew = daily.sumOf { it.newCards.toLong() }
        val totalStudyMs = daily.sumOf { it.studyTime.inWholeMilliseconds }
        val totalWriting = daily.sumOf { it.writingAttempts.toLong() }

        val days = windowDays.toFloat()
        val examsInWindow = completedExams.sortedBy { it.startedAt }

        val delta = if (examsInWindow.size >= 4) {
            val half = examsInWindow.size / 2
            val first = examsInWindow.take(half).map { it.accuracy }
            val second = examsInWindow.drop(half).map { it.accuracy }
            val avg = { list: List<Float> -> list.average().toFloat() }
            (avg(second) - avg(first)) * 100f
        } else null

        return VelocityMetrics(
            windowDays = windowDays,
            reviewsPerDay = totalReviews / days,
            newItemsPerWeek = totalNew / days * 7f,
            studyHoursPerWeek = ((totalStudyMs / 3_600_000.0) / days * 7f).toFloat(),
            writingAttemptsPerWeek = totalWriting / days * 7f,
            examsPerMonth = examsInWindow.size / days * 30f,
            examScoreDelta = delta
        )
    }

    /** Formats a float with one decimal. */
    fun oneDecimal(value: Float): String {
        val scaled = (value * 10f).roundToInt()
        val tenths = scaled % 10
        return if (tenths == 0) (scaled / 10).toString() else "${scaled / 10}.$tenths"
    }
}

// Extension to reuse the existing Duration formatting in reports.
fun Duration.asStudyHoursString(): String {
    val minutes = inWholeMinutes
    return when {
        minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m"
        else -> "${inWholeSeconds}s"
    }
}
