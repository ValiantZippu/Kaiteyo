package ua.syt0r.kanji.core.statistics

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ============================================================
// STATISTICS EXPORT (todo #140, spec §126)
// ------------------------------------------------------------
// Serializes REAL study history (sessions + daily aggregates +
// streaks) into CSV or JSON so a learner can back up or analyze
// their progress outside the app. Pure: given the records, out
// come strings — the screen loads the real data and hands it
// over, nothing is synthesized.
// ============================================================

/** The full export payload — everything the screen can truthfully say. */
@Serializable
data class StatisticsExport(
    val exportedAt: Instant,
    val totalReviews: Long,
    val currentStreak: Int,
    val longestStreak: Int,
    val sessions: List<StudySessionRecord> = emptyList(),
    val dailyActivity: List<DailyActivityExport> = emptyList()
)

/** A single day's real aggregate (reviews/new/time/accuracy). */
@Serializable
data class DailyActivityExport(
    val date: String,
    val reviews: Long,
    val newCards: Int,
    val studyTimeMinutes: Long,
    val accuracy: Float
)

object StatisticsExporter {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * Builds the payload from the controller's real state. [dailyActivity]
     * maps each heatmap year's cells (date → DailyActivity) into the flat,
     * date-ordered list.
     */
    fun build(
        totalReviews: Long,
        currentStreak: Int,
        longestStreak: Int,
        sessions: List<StudySessionRecord>,
        dailyActivity: Map<Int, HeatmapYear>
    ): StatisticsExport {
        val flat = dailyActivity.values
            .flatMap { year ->
                year.cells.map { (date, activity) ->
                    DailyActivityExport(
                        date = date.toString(),
                        reviews = activity.reviews.toLong(),
                        newCards = activity.newCards,
                        studyTimeMinutes = activity.studyTime.inWholeMinutes,
                        accuracy = activity.accuracy
                    )
                }
            }
            .sortedBy { it.date }

        return StatisticsExport(
            exportedAt = kotlinx.datetime.Clock.System.now(),
            totalReviews = totalReviews,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            sessions = sessions,
            dailyActivity = flat
        )
    }

    fun toJson(export: StatisticsExport): String = json.encodeToString(export)

    /** CSV with a real header row — every column is a real field. */
    fun toCsv(export: StatisticsExport): String = buildString {
        appendLine("exported_at,total_reviews,current_streak,longest_streak")
        appendLine("${export.exportedAt},${export.totalReviews},${export.currentStreak},${export.longestStreak}")
        appendLine()
        appendLine("session_start,session_end,duration_seconds,mode,deck_name,items_studied,new_items,review_items,correct,incorrect,accuracy,complete")
        export.sessions.forEach { s ->
            appendLine(
                listOf(
                    s.startTime,
                    s.endTime?.toString() ?: "",
                    s.duration.inWholeSeconds,
                    s.mode,
                    csvEscape(s.deckName),
                    s.itemsStudied,
                    s.newItems,
                    s.reviewItems,
                    s.correct,
                    s.incorrect,
                    "%.3f".format(s.accuracy),
                    s.isComplete
                ).joinToString(",")
            )
        }
        if (export.sessions.isNotEmpty()) {
            appendLine()
        }
        appendLine("date,reviews,new_cards,study_time_minutes,accuracy")
        export.dailyActivity.forEach { d ->
            appendLine(
                listOf(
                    d.date, d.reviews, d.newCards, d.studyTimeMinutes,
                    "%.3f".format(d.accuracy)
                ).joinToString(",")
            )
        }
    }

    private fun csvEscape(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
