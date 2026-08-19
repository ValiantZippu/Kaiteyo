package ua.syt0r.kanji.core.statistics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

class StatisticsExporterTest {

    private val now: Instant = Clock.System.now()

    private fun session(id: Long, items: Int, correct: Int) = StudySessionRecord(
        id = id,
        startTime = now,
        endTime = now + 15.minutes,
        duration = 15.minutes,
        mode = "flashcard",
        deckName = "N5, deck",
        itemsStudied = items,
        correct = correct
    )

    @Test
    fun jsonRoundTripsRealFields() {
        val export = StatisticsExporter.build(
            totalReviews = 120,
            currentStreak = 3,
            longestStreak = 9,
            sessions = listOf(session(1, 10, 8)),
            dailyActivity = emptyMap()
        )
        val json = StatisticsExporter.toJson(export)
        assertTrue(json.contains("\"totalReviews\": 120"))
        assertTrue(json.contains("\"currentStreak\": 3"))
        assertTrue(json.contains("\"longestStreak\": 9"))
        assertTrue(json.contains("\"sessions\""))
    }

    @Test
    fun csvHasHeaderAndSessionRows() {
        val export = StatisticsExporter.build(
            totalReviews = 120,
            currentStreak = 3,
            longestStreak = 9,
            sessions = listOf(session(1, 10, 8)),
            dailyActivity = emptyMap()
        )
        val csv = StatisticsExporter.toCsv(export)
        assertTrue(csv.startsWith("exported_at,total_reviews,current_streak,longest_streak"))
        assertTrue(csv.contains("session_start,session_end,duration_seconds"))
        assertTrue(csv.contains("flashcard"))
    }

    @Test
    fun csvEscapesDeckNamesWithCommas() {
        val export = StatisticsExporter.build(
            totalReviews = 1,
            currentStreak = 0,
            longestStreak = 0,
            sessions = listOf(session(1, 5, 4)),
            dailyActivity = emptyMap()
        )
        // deckName \"N5, deck\" contains a comma → must be quoted in CSV.
        val csv = StatisticsExporter.toCsv(export)
        assertTrue(csv.contains("\"N5, deck\""))
    }

    @Test
    fun dailyActivityFlattenedAndSorted() {
        val year = HeatmapYear(
            year = 2026,
            cells = linkedMapOf(
                kotlinx.datetime.LocalDate(2026, 2, 1) to DailyActivity(reviews = 5, correct = 4),
                kotlinx.datetime.LocalDate(2026, 1, 15) to DailyActivity(reviews = 3, correct = 3)
            )
        )
        val export = StatisticsExporter.build(
            totalReviews = 8,
            currentStreak = 1,
            longestStreak = 2,
            sessions = emptyList(),
            dailyActivity = mapOf(2026 to year)
        )
        assertEquals(2, export.dailyActivity.size)
        // Date-ordered: January before February.
        assertEquals("2026-01-15", export.dailyActivity.first().date)
        assertEquals(5L, export.dailyActivity.last().reviews)
    }

    @Test
    fun accuracyDerivedFromRealFields() {
        val export = StatisticsExporter.build(
            totalReviews = 2,
            currentStreak = 0,
            longestStreak = 0,
            sessions = listOf(session(1, 4, 3)),
            dailyActivity = emptyMap()
        )
        assertEquals(0.75f, export.sessions.first().accuracy)
    }
}
