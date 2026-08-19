package ua.syt0r.kanji.presentation.screen.main.screen.sentence_entry

import kotlinx.coroutines.flow.StateFlow
import ua.syt0r.kanji.core.knowledge.SentenceAnalysis

// ============================================================
// SENTENCE ENTRY — CONTRACT (spec §26–§27)
// ------------------------------------------------------------
// A corpus sentence page: every token is interactive (tap a
// kanji → kanji entry, tap a word → word entry), grammar
// patterns are highlighted, and a difficulty estimate is shown.
// ============================================================

interface SentenceEntryContract {

    interface ViewModel {
        val state: StateFlow<ScreenState>

        /** Loads and analyzes [sentence] (idempotent per sentence). */
        fun load(sentence: String, translation: String)
        fun retry()
    }

    sealed interface ScreenState {

        data object Loading : ScreenState

        data class Loaded(
            val sentence: String,
            val translation: String,
            val analysis: SentenceAnalysis,
            /** Profile-driven presentation (KT-LEVEL-003, spec §23–§24). */
            val showTranslation: Boolean = true,
            val showFurigana: Boolean = true,
            val showRomaji: Boolean = false
        ) : ScreenState

        data class Error(val message: String) : ScreenState
    }
}
