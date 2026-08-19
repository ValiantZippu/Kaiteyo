package ua.syt0r.kanji.presentation.screen.main.screen.collection_detail

import kotlinx.coroutines.flow.StateFlow
import ua.syt0r.kanji.core.knowledge.KanjiKnowledge
import ua.syt0r.kanji.core.knowledge.LibraryCollection

// ============================================================
// COLLECTION DETAIL — CONTRACT (spec §29–§30)
// ------------------------------------------------------------
// A library collection's detail page: title, description, size,
// and the real kanji entries it contains (lazy-loaded in pages).
// Drill-down: COLLECTION → KANJI → WORDS → SENTENCES.
// ============================================================

interface CollectionDetailContract {

    interface ViewModel {
        val state: StateFlow<ScreenState>

        /** Loads the collection (idempotent per id). */
        fun load(collectionId: String)

        /** Loads the next page of kanji entries. */
        fun loadMore()

        /** Re-runs the last load after an error. */
        fun retry()

        /**
         * Creates (or finds) a letter deck from this collection's kanji and
         * publishes its id — the lesson-to-SRS study path (spec §29).
         */
        fun studyDeck()

        /** Clears the published study-deck id after navigation consumed it. */
        fun clearStudyDeckId()
    }

    sealed interface ScreenState {

        data object Loading : ScreenState

        data class Loaded(
            val collection: LibraryCollection,
            val kanji: List<KanjiKnowledge>,
            val totalKanji: Int,
            val hasMore: Boolean,
            val loadingMore: Boolean,
            /**
             * The letter deck created for this collection ("Study as deck"),
             * set once the deck exists; the screen navigates to it and calls
             * [clearStudyDeckId] so it only fires once.
             */
            val studyDeckId: Long? = null
        ) : ScreenState

        data class Error(val message: String) : ScreenState
    }
}
