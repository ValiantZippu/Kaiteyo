package ua.syt0r.kanji.core.knowledge

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ua.syt0r.kanji.core.srs.SrsCard
import ua.syt0r.kanji.core.srs.SrsCardKey
import ua.syt0r.kanji.core.srs.SrsCardRepository
import ua.syt0r.kanji.core.srs.fsrs.FsrsCard
import ua.syt0r.kanji.core.srs.fsrs.FsrsCardStatus
import ua.syt0r.kanji.core.srs.fsrs.FsrsCardParams
import kotlin.time.Duration

/**
 * Study-status-provider tests (spec §15): the provider projects REAL FSRS
 * cards onto the shared StudyState vocabulary per practice (kanji → writing
 * + reading, word → flashcard). A missing card is New + isNew — never
 * fabricated as studied.
 */
class StudyStatusProviderTest {

    private val now = Instant.parse("2026-01-01T00:00:00Z")

    private fun reviewCard(intervalDays: Long, reviewedDaysAgo: Long = intervalDays): SrsCard = SrsCard(
        FsrsCard(
            status = FsrsCardStatus.Review,
            params = FsrsCardParams.Existing(
                difficulty = 5.0,
                stability = 1.0,
                reviewTime = now - Duration.parse("${reviewedDaysAgo}d")
            ),
            interval = Duration.parse("${intervalDays}d"),
            lapses = 0,
            repeats = 0
        )
    )

    private class FakeCardRepo(
        private val cards: Map<SrsCardKey, SrsCard>
    ) : SrsCardRepository {
        override val changesFlow: SharedFlow<Unit> = MutableStateFlow(Unit)
        override suspend fun get(key: SrsCardKey): SrsCard? = cards[key]
        override suspend fun getAll(): Map<SrsCardKey, SrsCard> = cards
        override suspend fun update(key: SrsCardKey, card: SrsCard) = Unit
    }

    @Test
    fun noCardsProjectToNewAndUnstarted() = runBlocking {
        val provider = StudyStatusProvider(FakeCardRepo(emptyMap()))
        val entries = provider.kanjiStates("漢", now)
        assertEquals(2, entries.size)
        assertEquals(listOf("Writing", "Reading"), entries.map { it.practiceLabel })
        assertTrue(entries.all { it.state == StudyState.New && it.isNew })
    }

    @Test
    fun writingCardReviewedShowsKnownReadingStaysNew() = runBlocking {
        val writingKey = StudyGate.cardKeysForKanji("漢")[0]
        val provider = StudyStatusProvider(
            FakeCardRepo(mapOf(writingKey to reviewCard(intervalDays = 3)))
        )
        val entries = provider.kanjiStates("漢", now)
        val writing = entries.first { it.practiceLabel == "Writing" }
        val reading = entries.first { it.practiceLabel == "Reading" }
        assertEquals(StudyState.Known, writing.state)
        assertFalse(writing.isNew)
        assertEquals(StudyState.New, reading.state)
        assertTrue(reading.isNew)
    }

    @Test
    fun stableReviewCardIsMastered() = runBlocking {
        val writingKey = StudyGate.cardKeysForKanji("漢")[0]
        val provider = StudyStatusProvider(
            FakeCardRepo(mapOf(writingKey to reviewCard(intervalDays = 30)))
        )
        val writing = provider.kanjiStates("漢", now).first { it.practiceLabel == "Writing" }
        assertEquals(StudyState.Mastered, writing.state)
    }

    @Test
    fun wordStateUsesFlashcardKey() = runBlocking {
        val key = StudyGate.cardKeyForWord(42L)
        assertEquals("42", key.itemKey)
        assertEquals(10L, key.practiceType) // VocabFlashcard

        val provider = StudyStatusProvider(FakeCardRepo(emptyMap()))
        val fresh = provider.wordState(42L, now)
        assertEquals(StudyState.New, fresh?.state)
        assertTrue(fresh?.isNew == true)
        assertEquals("Flashcard", fresh?.practiceLabel)

        val studied = StudyStatusProvider(FakeCardRepo(mapOf(key to reviewCard(intervalDays = 5))))
        assertEquals(StudyState.Known, studied.wordState(42L, now)?.state)
        assertFalse(studied.wordState(42L, now)?.isNew == true)
    }
}
