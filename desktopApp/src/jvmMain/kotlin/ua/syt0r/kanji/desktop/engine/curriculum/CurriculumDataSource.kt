package ua.syt0r.kanji.desktop.engine.curriculum

import ua.syt0r.kanji.desktop.appstate.AppState

// ============================================
// KAITEYO CURRICULUM — DATA SOURCE
// The engine measures objectives only through
// this interface, so it stays testable with a
// fake source and the app supplies a real one
// backed by AppState (card pool + review log).
// ============================================

interface CurriculumDataSource {

    /** Human name of a deck, or null when it is not installed. */
    fun deckName(deckId: String): String?

    /** Deck ids currently installed (built-in + user decks). */
    fun availableDeckIds(): Set<String>

    /** Cards currently in a deck (its learned membership). */
    fun cardCountInDeck(deckId: String): Int

    /** Distinct cards in a deck that have been reviewed at least once. */
    fun reviewedCardCountInDeck(deckId: String): Int

    /** Total review events recorded. */
    fun totalReviewEvents(): Int
}

/** Real implementation over AppState's card pool + review log. */
class AppStateCurriculumDataSource(
    private val state: AppState
) : CurriculumDataSource {

    override fun deckName(deckId: String): String? =
        state.library.deck(deckId)?.name ?: deckId

    override fun availableDeckIds(): Set<String> = buildSet {
        state.library.allDecks().forEach { add(it.id) }
        state.cards.forEach { add(it.deckId) }
    }

    override fun cardCountInDeck(deckId: String): Int =
        state.cards.count { it.deckId == deckId }

    override fun reviewedCardCountInDeck(deckId: String): Int {
        val reviewedCardIds = state.reviewLog.map { it.cardId }.toSet()
        return state.cards.count { it.deckId == deckId && it.id in reviewedCardIds }
    }

    override fun totalReviewEvents(): Int = state.reviewLog.size
}
