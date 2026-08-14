package ua.syt0r.kanji.core.statistics

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class DeckRetentionTest {

    private fun session(
        deckId: Long,
        deckName: String,
        items: Int,
        correct: Int,
        minutes: Long
    ) = StudySessionRecord(
        startTime = Instant.fromEpochMilliseconds(0),
        deckId = deckId,
        deckName = deckName,
        itemsStudied = items,
        correct = correct,
        duration = minutes.minutes,
        isComplete = true
    )

    @Test
    fun sessionsAggregatePerDeck() {
        val retention = DeckRetentionCalculator.fromSessions(
            listOf(
                session(1, "JLPT N5", items = 10, correct = 9, minutes = 5),
                session(1, "JLPT N5", items = 10, correct = 7, minutes = 5),
                session(2, "Mining", items = 4, correct = 1, minutes = 3)
            )
        )
        assertEquals(2, retention.size)
        val n5 = retention.first { it.deckId == 1L }
        assertEquals(2, n5.sessions)
        assertEquals(20, n5.itemsStudied)
        assertEquals(16, n5.correct)
        assertEquals(0.8f, n5.accuracy)
        assertEquals(10 * 60_000L, n5.studyTimeMs)
    }

    @Test
    fun unnamedDecksFallBackToGeneral() {
        val retention = DeckRetentionCalculator.fromSessions(
            listOf(session(0, "", items = 5, correct = 3, minutes = 1))
        )
        assertEquals("General", retention.first().deckName)
    }

    @Test
    fun weakestDecksAreSortedByAccuracy() {
        val retention = DeckRetentionCalculator.weakest(
            listOf(
                session(1, "Good deck", items = 10, correct = 9, minutes = 1),
                session(2, "Bad deck", items = 10, correct = 2, minutes = 1)
            )
        )
        assertEquals("Bad deck", retention.first().deckName)
        assertEquals(0.2f, retention.first().accuracy)
    }

    @Test
    fun zeroItemsMeanNoData() {
        val retention = DeckRetentionCalculator.fromSessions(
            listOf(session(1, "Empty", items = 0, correct = 0, minutes = 0))
        )
        assertTrue(!retention.first().hasData)
    }
}
