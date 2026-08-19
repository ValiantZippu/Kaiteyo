package ua.syt0r.kanji.presentation.screen.main.screen.kanji_entry

import kotlinx.coroutines.flow.StateFlow
import ua.syt0r.kanji.core.knowledge.ComponentKnowledge
import ua.syt0r.kanji.core.knowledge.GrammarMatch
import ua.syt0r.kanji.core.knowledge.KanjiKnowledge
import ua.syt0r.kanji.core.knowledge.SentenceKnowledge
import ua.syt0r.kanji.core.knowledge.WordKnowledge
import ua.syt0r.kanji.core.knowledge.cards.KanjiCardLayout
import ua.syt0r.kanji.core.knowledge.cards.KanjiCardType

// ============================================================
// KANJI ENTRY — CONTRACT
// ------------------------------------------------------------
// The modular kanji page. Content is a sequence of cards driven
// by a persisted KanjiCardLayout: show / hide / reorder / apply
// presets — all real, all persisted.
// ============================================================

interface KanjiEntryContract {

    interface ViewModel {
        val state: StateFlow<ScreenState>

        /** Loads the entry for [character] (idempotent per character). */
        fun load(character: String)

        // ── Card customization (persisted) ────────────────────────────
        fun toggleCard(type: KanjiCardType)
        fun moveCardUp(type: KanjiCardType)
        fun moveCardDown(type: KanjiCardType)

        /** Per-card settings (spec §21): the example/word limit for content cards. */
        fun setCardLimit(type: KanjiCardType, limit: Int)
        fun applyPreset(presetId: String)
        fun setEditMode(enabled: Boolean)

        fun retry()
    }

    sealed interface ScreenState {

        data object Loading : ScreenState

        data class Loaded(
            val character: String,
            val kanji: KanjiKnowledge,
            val radicals: List<ComponentKnowledge>,
            val words: List<WordKnowledge>,
            val related: List<KanjiKnowledge>,
            val sentences: List<SentenceKnowledge>,
            val grammar: List<GrammarMatch>,
            val layout: KanjiCardLayout,
            val editMode: Boolean
        ) : ScreenState

        data class Error(val message: String) : ScreenState
    }
}
