package ua.syt0r.kanji.presentation.screen.main.screen.radical_explorer

import kotlinx.coroutines.flow.StateFlow
import ua.syt0r.kanji.core.knowledge.KanjiKnowledge
import ua.syt0r.kanji.core.knowledge.RadicalStats

// ============================================================
// RADICAL EXPLORER — CONTRACT
// ------------------------------------------------------------
// Radicals are first-class entities here, not decorative tags.
// Selectable grid → stroke / JLPT / grade filtering → kanji
// previews → word drill-down. The flow is always:
//   RADICAL → COMPONENT → KANJI → WORDS → SENTENCES
// ============================================================

interface RadicalExplorerContract {

    interface ViewModel {
        val state: StateFlow<ScreenState>

        fun load()
        fun toggleRadical(radical: String)
        fun clearSelection()
        fun setMinStrokes(strokes: Int?)
        fun setJlpt(level: Int?)
        fun setGrade(grade: Int?)
        fun retry()
    }

    sealed interface ScreenState {

        data object Loading : ScreenState

        data class Loaded(
            val radicals: List<RadicalStats>,
            val selectedRadicals: List<String>,
            val minStrokes: Int?,
            val jlpt: Int?,
            val grade: Int?,
            val kanji: List<KanjiKnowledge>,
            val kanjiTotal: Int,
            val loadingKanji: Boolean
        ) : ScreenState

        data class Error(val message: String) : ScreenState
    }
}
