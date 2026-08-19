package ua.syt0r.kanji.presentation.screen.main.screen.sentence

import kotlinx.coroutines.flow.StateFlow
import ua.syt0r.kanji.core.knowledge.AnnotatedToken
import ua.syt0r.kanji.core.knowledge.GrammarMatch
import ua.syt0r.kanji.core.knowledge.SentenceAnalysis
import ua.syt0r.kanji.core.knowledge.SentenceKnowledge

// ============================================================
// SENTENCE — CONTRACT
// ------------------------------------------------------------
// The token-interactive sentence page (spec §26–§27). Two modes:
//  - Explorer: search the bundled corpus for sentences
//  - Detail:   an analyzed sentence where every token is linked
//              to the dictionary, grammar matches are highlighted,
//              and difficulty + provenance are shown honestly.
// Nothing here fabricates linguistic information — tokens that
// resolve to a real word/kanji carry the entry; the rest are
// left unlinked; the difficulty is a labelled surface estimate.
// ============================================================

interface SentenceScreenContract {

    interface ViewModel {
        val state: StateFlow<ScreenState>

        fun search(query: String)
        fun open(sentence: SentenceKnowledge)
        /** Opens the corpus sentence whose text matches [text] (nav entry). */
        fun openByText(text: String)
        fun back()
        fun retry()
    }

    sealed interface ScreenState {

        data object Idle : ScreenState

        data class Explorer(
            val query: String,
            val results: List<SentenceKnowledge>,
            val loading: Boolean
        ) : ScreenState

        data class Detail(
            val sentence: SentenceKnowledge,
            /** Null while [loading]; the analyzed, token-linked sentence. */
            val analysis: SentenceAnalysis?,
            /** Same-corpus sentences sharing a kanji with [sentence]. */
            val related: List<SentenceKnowledge>,
            val loading: Boolean
        ) : ScreenState

        data class Error(val message: String) : ScreenState
    }
}
