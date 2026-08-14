package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.vocab_dashboard

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.RefreshableData
import ua.syt0r.kanji.core.analytics.AnalyticsManager
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.presentation.LifecycleAwareViewModel
import ua.syt0r.kanji.presentation.LifecycleState
import ua.syt0r.kanji.presentation.common.ScreenVocabPracticeType
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DeckDashboardListMode
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DeckDashboardListState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DecksMergeRequestData
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.DecksSortRequestData
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.VocabDeckDashboardPracticeTypeItem
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.vocab_dashboard.VocabDashboardScreenContract.ScreenState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.vocab_dashboard.use_case.MergeVocabDecksUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.vocab_dashboard.use_case.SubscribeOnDashboardVocabDecksUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.vocab_dashboard.use_case.UpdateVocabDecksOrderUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.vocab_dashboard.use_case.VocabDashboardScreenData
import kotlin.time.Duration.Companion.seconds

class VocabDashboardViewModel(
    private val viewModelScope: CoroutineScope,
    private val subscribeOnDashboardVocabDecksUseCase: SubscribeOnDashboardVocabDecksUseCase,
    private val mergeVocabDecksUseCase: MergeVocabDecksUseCase,
    private val updateDecksOrderUseCase: UpdateVocabDecksOrderUseCase,
    private val updateDeckArchivedUseCase: VocabDashboardScreenContract.UpdateDeckArchivedUseCase,
    private val preferencesRepository: PreferencesContract.AppPreferences,
    private val analyticsManager: AnalyticsManager
) : VocabDashboardScreenContract.ViewModel, LifecycleAwareViewModel {

    private val sortRequestsChannel = Channel<DecksSortRequestData>()

    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    override val screenState: StateFlow<ScreenState> = _screenState

    override val lifecycleState: MutableStateFlow<LifecycleState> =
        MutableStateFlow(LifecycleState.Hidden)

    private var loadJob: kotlinx.coroutines.Job? = null

    init {
        subscribeToData()
        sortRequestsChannel.consumeAsFlow()
            // To avoid infinite loading when rapidly clicking on apply sort button
            .debounce(1.seconds)
            .onEach { updateDecksOrderUseCase.update(it) }
            .launchIn(viewModelScope)
    }

    private fun subscribeToData() {
        loadJob?.cancel()
        loadJob = subscribeOnDashboardVocabDecksUseCase(lifecycleState)
            .onEach { _screenState.value = toScreenState(it) }
            .launchIn(viewModelScope)
    }

    override fun retryLoad() = subscribeToData()

    private suspend fun toScreenState(data: RefreshableData<VocabDashboardScreenData>): ScreenState = when (data) {
        is RefreshableData.Loading ->
            ScreenState.Loading

        is RefreshableData.Failed -> ScreenState.Error(data.error?.message)

        is RefreshableData.Loaded -> {
            val screenData = data.value
                        val sortByTimeEnabled = preferencesRepository.vocabDashboardSortByTime.get()
                        val (activeItems, archivedItems) =
                            screenData.decks.partition { !it.isArchived }
                        val listState = DeckDashboardListState(
                            items = activeItems,
                            sortByReviewTime = sortByTimeEnabled,
                            showDailyNewIndicator = screenData.dailyLimitEnabled,
                            mode = mutableStateOf(DeckDashboardListMode.Browsing)
                        )

                        val practiceTypeItems = ScreenVocabPracticeType.values()
                            .map { practiceType ->
                                val hasPendingReviews = listState.items.any {
                                    it.studyProgress.getValue(practiceType).run {
                                        dailyNew.isNotEmpty() || dailyDue.isNotEmpty()
                                    }
                                }
                                VocabDeckDashboardPracticeTypeItem(
                                    practiceType = practiceType,
                                    hasPendingReviews = hasPendingReviews
                                )
                            }

                        val practiceType = ScreenVocabPracticeType
                            .from(preferencesRepository.vocabDashboardPracticeType.get())

                        val selectedItemState = mutableStateOf(
                            practiceTypeItems.first { it.practiceType == practiceType }
                        )

                        snapshotFlow { selectedItemState.value }
                            .map { it.practiceType.preferencesType }
                            .onEach { preferencesRepository.vocabDashboardPracticeType.set(it) }
                            .launchIn(viewModelScope)

                        ScreenState.Loaded(
                            listState = listState,
                            archivedItems = archivedItems,
                            practiceTypeItems = practiceTypeItems,
                            selectedPracticeTypeItem = selectedItemState
                        )
                    }
                }

    override fun mergeDecks(data: DecksMergeRequestData) {
        _screenState.value = ScreenState.Loading
        viewModelScope.launch { mergeVocabDecksUseCase(data) }
    }

    override fun sortDecks(data: DecksSortRequestData) {
        _screenState.value = ScreenState.Loading
        viewModelScope.launch { sortRequestsChannel.send(data) }
    }

    override fun setDeckArchived(deckId: Long, isArchived: Boolean) {
        _screenState.value = ScreenState.Loading
        viewModelScope.launch { updateDeckArchivedUseCase(deckId, isArchived) }
    }

}