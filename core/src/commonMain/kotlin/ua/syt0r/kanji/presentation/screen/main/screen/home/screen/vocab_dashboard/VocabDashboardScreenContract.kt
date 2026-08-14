package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.vocab_dashboard

import androidx.compose.runtime.MutableState
import kotlinx.coroutines.flow.StateFlow
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DeckDashboardListState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DecksMergeRequestData
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DecksSortRequestData
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.VocabDeckDashboardItem
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.VocabDeckDashboardPracticeTypeItem

interface VocabDashboardScreenContract {

    interface ViewModel {

        val screenState: StateFlow<ScreenState>

        fun mergeDecks(data: DecksMergeRequestData)
        fun sortDecks(data: DecksSortRequestData)
        fun setDeckArchived(deckId: Long, isArchived: Boolean)
        fun retryLoad()

    }

    sealed interface ScreenState {

        object Loading : ScreenState

        data class Error(
            val message: String? = null
        ) : ScreenState

        data class Loaded(
            val listState: DeckDashboardListState,
            val archivedItems: List<VocabDeckDashboardItem>,
            val practiceTypeItems: List<VocabDeckDashboardPracticeTypeItem>,
            val selectedPracticeTypeItem: MutableState<VocabDeckDashboardPracticeTypeItem>,
        ) : ScreenState

    }

    interface UpdateDeckArchivedUseCase {
        suspend operator fun invoke(deckId: Long, isArchived: Boolean)
    }

}