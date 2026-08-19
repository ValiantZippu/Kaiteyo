package ua.syt0r.kanji.core.knowledge

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ua.syt0r.kanji.core.srs.SrsCard
import ua.syt0r.kanji.core.srs.SrsItemStatus
import ua.syt0r.kanji.core.srs.fsrs.FsrsCard
import ua.syt0r.kanji.core.srs.fsrs.FsrsCardStatus

class StudyGateTest {

    private val now = Instant.parse("2026-01-01T00:00:00Z")

    private fun newCard(status: FsrsCardStatus) = FsrsCard(
        status = status,
        params = FsrsCardParams.New,
        interval = kotlin.time.Duration.ZERO,
        lapses = 0,
        repeats = 0
    )

    private fun existingParams(reviewTime: Instant) = FsrsCardParams.Existing(
        difficulty = 5.0,
        stability = 1.0,
        reviewTime = reviewTime
    )

    @Test
    fun noCardProjectsToNew() {
        assertEquals(StudyState.New, StudyGate.stateFor(null, now))
    }

    @Test
    fun learningCardProjectsToLearning() {
        val card = SrsCard(newCard(status = FsrsCardStatus.Learning))
        assertEquals(StudyState.Learning, StudyGate.stateFor(card, now))
    }

    @Test
    fun reviewCardWithElapsedIntervalIsDue() {
        val card = SrsCard(
            FsrsCard(
                status = FsrsCardStatus.Review,
                params = existingParams(now - kotlin.time.Duration.parse("1d")),
                interval = kotlin.time.Duration.parse("1d"),
                lapses = 0,
                repeats = 0
            )
        )
        assertEquals(StudyState.Due, StudyGate.stateFor(card, now))
    }

    @Test
    fun reviewCardWithStableIntervalIsMastered() {
        val card = SrsCard(
            FsrsCard(
                status = FsrsCardStatus.Review,
                params = existingParams(now - kotlin.time.Duration.parse("30d")),
                interval = kotlin.time.Duration.parse("30d"),
                lapses = 0,
                repeats = 0
            )
        )
        assertEquals(StudyState.Mastered, StudyGate.stateFor(card, now))
    }

    @Test
    fun bridgesCoarseStatus() {
        assertEquals(StudyState.New, StudyGate.fromSrsItemStatus(SrsItemStatus.New))
        assertEquals(StudyState.Known, StudyGate.fromSrsItemStatus(SrsItemStatus.Done))
        assertEquals(StudyState.Due, StudyGate.fromSrsItemStatus(SrsItemStatus.Review))
    }

    @Test
    fun knownStateIsOnlyKnownOrMastered() {
        assertTrue(StudyGate.isKnown(StudyState.Known))
        assertTrue(StudyGate.isKnown(StudyState.Mastered))
        assertFalse(StudyGate.isKnown(StudyState.New))
        assertFalse(StudyGate.isKnown(StudyState.Learning))
        assertFalse(StudyGate.isKnown(StudyState.Due))
    }

    @Test
    fun kanjiHasWritingAndReadingKeys() {
        val keys = StudyGate.cardKeysForKanji("漢")
        assertEquals(2, keys.size)
        assertEquals(0L, keys[0].practiceType) // writing
        assertEquals(1L, keys[1].practiceType) // reading
        assertEquals("漢", keys[0].itemKey)
    }
}
