package ua.syt0r.kanji.presentation.screen.main.screen.browse_hub

import kotlinx.coroutines.flow.StateFlow
import ua.syt0r.kanji.core.knowledge.GrammarPattern
import ua.syt0r.kanji.core.knowledge.LibraryCollection
import ua.syt0r.kanji.core.knowledge.RadicalStats

// ============================================================
// BROWSE HUB — CONTRACT (spec §30)
// ------------------------------------------------------------
// \"Explore Japanese\" — a browsable surface over the knowledge
// core: kanji by JLPT/grade/frequency, radicals, components,
// words, grammar, sentences and library collections. Every
// count shown here is a REAL dataset count.
// ============================================================

interface BrowseHubContract {

    interface ViewModel {
        val state: StateFlow<ScreenState>

        fun load()
    }

    sealed interface ScreenState {

        data object Loading : ScreenState

        data class Loaded(
            val kanjiTotal: Int,
            val radicalCount: Int,
            val componentCount: Int,
            val jlptCollections: List<LibraryCollection>,
            val gradeCollections: List<LibraryCollection>,
            val radicals: List<RadicalStats>,
            val grammarPatterns: List<GrammarPattern>,
            val frequencyDistribution: List<Pair<ua.syt0r.kanji.core.knowledge.FrequencyBand, Int>> = emptyList(),
            val jlptDistribution: List<Pair<Int, Int>> = emptyList(),
            val gradeDistribution: List<Pair<Int, Int>> = emptyList()
        ) : ScreenState

        data class Error(val message: String) : ScreenState
    }
}
