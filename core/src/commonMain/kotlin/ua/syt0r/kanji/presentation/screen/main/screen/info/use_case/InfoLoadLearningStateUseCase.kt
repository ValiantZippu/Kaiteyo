package ua.syt0r.kanji.presentation.screen.main.screen.info.use_case

import kotlinx.datetime.Instant
import ua.syt0r.kanji.core.srs.LetterPracticeType
import ua.syt0r.kanji.core.srs.VocabPracticeType
import ua.syt0r.kanji.core.srs.fsrs.FsrsCard
import ua.syt0r.kanji.core.statistics.StatisticsRepository
import ua.syt0r.kanji.core.user_data.database.CardDatabaseManager
import ua.syt0r.kanji.core.user_data.database.FsrsCardRepository
import ua.syt0r.kanji.core.user_data.database.LetterPracticeRepository
import ua.syt0r.kanji.core.user_data.database.ReviewHistoryRepository
import ua.syt0r.kanji.core.user_data.database.VocabPracticeRepository
import ua.syt0r.kanji.presentation.screen.main.features.LETTER_WRITING_PRACTICE_TYPE
import ua.syt0r.kanji.presentation.screen.main.features.SUSPENDED_FLAG_TYPE

// ============================================
// LEARNING STATE FOR THE DETAIL PAGE
// Every value is read from real persisted state:
//   · FSRS cards (per practice type)
//   · review history counts
//   · writing attempt statistics
//   · letter / vocab deck membership
//   · tags, notes, suspend flag
// ============================================

/** Learning state for one practice type of an item. */
data class CardLearningState(
    val practiceTypeName: String,
    val status: String,
    val reviews: Int,
    val lapses: Int,
    val intervalDays: Int,
    val lastReview: Instant?,
    val nextDue: Instant?
)

/** Aggregated writing practice statistics for a character. */
data class WritingLearningStats(
    val attempts: Int,
    val accuracy: Float
)

/** Deck membership entry used by the "add to deck" dialog. */
data class DeckMembership(
    val deckId: Long,
    val title: String,
    val contains: Boolean
)

/** Full learning state shown on a kanji / vocabulary detail page. */
data class ItemLearningState(
    val practiceTypes: List<CardLearningState> = emptyList(),
    val writing: WritingLearningStats? = null,
    val decks: List<DeckMembership> = emptyList(),
    val tags: List<String> = emptyList(),
    val note: String = "",
    val isSuspended: Boolean = false
) {
    val isEmpty: Boolean
        get() = practiceTypes.all { it.reviews == 0 && it.status == "New" } &&
            writing == null && decks.none { it.contains } && tags.isEmpty() && note.isBlank()

    val hasProgress: Boolean get() = !isEmpty
}

class InfoLoadLearningStateUseCase(
    private val fsrsCardRepository: FsrsCardRepository,
    private val reviewHistoryRepository: ReviewHistoryRepository,
    private val statisticsRepository: StatisticsRepository,
    private val cardDatabaseManager: CardDatabaseManager,
    private val letterPracticeRepository: LetterPracticeRepository,
    private val vocabPracticeRepository: VocabPracticeRepository
) {

    suspend fun loadLetter(character: String): ItemLearningState {
        val practiceTypes = LetterPracticeType.entries.map { type ->
            val card = fsrsCardRepository.get(type.toSrsKey(character))
            val reviews = reviewHistoryRepository.getTotalReviewCount(
                character,
                type.srsPracticeType.value
            )
            card.toCardLearningState(type.name, reviews.toInt())
        }

        val writing = runCatching {
            statisticsRepository.getWritingSummaryForCharacter(character)
        }.getOrNull()
            ?.takeIf { it.attempts > 0 }
            ?.let { WritingLearningStats(it.attempts, it.accuracy) }

        val decks = letterPracticeRepository.getDecks()
            .map { deck ->
                val characters = letterPracticeRepository.getDeckCharacters(deck.id)
                DeckMembership(
                    deckId = deck.id,
                    title = deck.name,
                    contains = character in characters
                )
            }
            .filter { !it.title.isBlank() }

        val tags = cardDatabaseManager.getTagsForCard(character, LETTER_WRITING_PRACTICE_TYPE)
            .map { it.name }

        val note = cardDatabaseManager.getNote(character, LETTER_WRITING_PRACTICE_TYPE).orEmpty()
        val isSuspended = cardDatabaseManager.getFlag(character, LETTER_WRITING_PRACTICE_TYPE) ==
            SUSPENDED_FLAG_TYPE

        return ItemLearningState(
            practiceTypes = practiceTypes,
            writing = writing,
            decks = decks,
            tags = tags,
            note = note,
            isSuspended = isSuspended
        )
    }

    suspend fun loadVocab(
        wordId: Long,
        kanjiReading: String?,
        kanaReading: String
    ): ItemLearningState {
        val key = wordId.toString()

        val practiceTypes = VocabPracticeType.entries.map { type ->
            val card = fsrsCardRepository.get(type.toSrsKey(wordId))
            val reviews = reviewHistoryRepository.getTotalReviewCount(
                key,
                type.srsPracticeType.value
            )
            card.toCardLearningState(type.name, reviews.toInt())
        }

        val decks = vocabPracticeRepository.getDecksContainingWord(kanjiReading, kanaReading)
            .toSet()
            .let { containingIds ->
                vocabPracticeRepository.getDecks().map { deck ->
                    DeckMembership(
                        deckId = deck.id,
                        title = deck.title,
                        contains = deck.id in containingIds
                    )
                }
            }
            .filter { !it.title.isBlank() }

        val tags = cardDatabaseManager
            .getTagsForCard(key, VocabPracticeType.Flashcard.srsPracticeType.value)
            .map { it.name }

        val note = cardDatabaseManager
            .getNote(key, VocabPracticeType.Flashcard.srsPracticeType.value)
            .orEmpty()
        val isSuspended = cardDatabaseManager.getFlag(
            key,
            VocabPracticeType.Flashcard.srsPracticeType.value
        ) == SUSPENDED_FLAG_TYPE

        return ItemLearningState(
            practiceTypes = practiceTypes,
            writing = null,
            decks = decks,
            tags = tags,
            note = note,
            isSuspended = isSuspended
        )
    }

    private fun FsrsCard?.toCardLearningState(
        practiceTypeName: String,
        reviews: Int
    ): CardLearningState {
        if (this == null) {
            return CardLearningState(
                practiceTypeName = practiceTypeName,
                status = "New",
                reviews = reviews,
                lapses = 0,
                intervalDays = 0,
                lastReview = null,
                nextDue = null
            )
        }

        val expectedReview = lastReview?.plus(interval)
        val status = when {
            status == ua.syt0r.kanji.core.srs.fsrs.FsrsCardStatus.New -> "New"
            status == ua.syt0r.kanji.core.srs.fsrs.FsrsCardStatus.Learning -> "Learning"
            status == ua.syt0r.kanji.core.srs.fsrs.FsrsCardStatus.Relearning -> "Relearning"
            expectedReview != null && expectedReview <= kotlinx.datetime.Clock.System.now() -> "Review"
            else -> "Scheduled"
        }

        return CardLearningState(
            practiceTypeName = practiceTypeName,
            status = status,
            reviews = reviews,
            lapses = lapses,
            intervalDays = interval.inWholeDays.toInt(),
            lastReview = lastReview,
            nextDue = expectedReview
        )
    }

}
