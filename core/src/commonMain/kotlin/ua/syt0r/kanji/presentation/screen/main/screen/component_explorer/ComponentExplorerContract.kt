package ua.syt0r.kanji.presentation.screen.main.screen.component_explorer

import kotlinx.coroutines.flow.StateFlow
import ua.syt0r.kanji.core.knowledge.ComponentKnowledge
import ua.syt0r.kanji.core.knowledge.KanjiKnowledge

// ============================================================
// COMPONENT EXPLORER — CONTRACT (spec §8)
// ------------------------------------------------------------
// Components are first-class entities. The explorer shows every
// component in the dataset, the kanji it appears in, and lets
// the user drill KANJI → COMPONENT → KANJI → WORDS → SENTENCES.
// ============================================================

interface ComponentExplorerContract {

    interface ViewModel {
        val state: StateFlow<ScreenState>

        /** Loads the component catalog (idempotent). */
        fun load()

        fun selectComponent(component: String)
        fun clearSelection()
    }

    sealed interface ScreenState {

        data object Loading : ScreenState

        data class Loaded(
            /** All components with their kanji counts, sorted by count. */
            val components: List<ComponentSummary>,
            /** The selected component, when one is chosen. */
            val selected: String? = null,
            /** Kanji containing the selected component. */
            val kanji: List<KanjiKnowledge> = emptyList(),
            /** Total kanji with the selected component. */
            val kanjiTotal: Int = 0,
            val loadingKanji: Boolean = false
        ) : ScreenState

        data class Error(val message: String) : ScreenState
    }

    data class ComponentSummary(
        val component: ComponentKnowledge,
        val kanjiCount: Int
    )
}
