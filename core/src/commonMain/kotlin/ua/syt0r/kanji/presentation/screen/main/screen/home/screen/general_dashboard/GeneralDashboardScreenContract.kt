package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard

import androidx.compose.runtime.MutableState
import kotlinx.coroutines.flow.StateFlow
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoActivity
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoCollection

interface GeneralDashboardScreenContract {

    interface ViewModel {
        val state: StateFlow<ScreenState>
        fun retryLoad()
    }

    sealed interface ScreenState {

        data object Loading : ScreenState

        data class Error(
            val message: String? = null
        ) : ScreenState

        data class Loaded(
            val studyTargets: MutableState<List<StudyTargetState>>,
            val stats: GeneralDashboardStats,
            val recentDecks: List<DashboardDeckSummary>,
            val allDecks: List<DashboardDeckSummary>,
            val recentActivity: List<KaiteyoActivity>,
            val collections: List<KaiteyoCollection>,
            /**
             * Explained "study this next" picks, computed from real SRS state
             * + frequency (spec §31). Empty when nothing is recommended yet.
             */
            val recommendations: List<DashboardRecommendation> = emptyList()
        ) : ScreenState

    }

}
