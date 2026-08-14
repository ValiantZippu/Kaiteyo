package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ua.syt0r.kanji.core.RefreshableData
import ua.syt0r.kanji.presentation.LifecycleAwareViewModel
import ua.syt0r.kanji.presentation.LifecycleState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard.GeneralDashboardScreenContract.ScreenState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard.use_case.SubscribeOnGeneralDashboardScreenDataUseCase

class GeneralDashboardViewModel(
    private val viewModelScope: CoroutineScope,
    private val subscribeOnScreenDataUseCase: SubscribeOnGeneralDashboardScreenDataUseCase
) : GeneralDashboardScreenContract.ViewModel,
    LifecycleAwareViewModel {

    private val _state = MutableStateFlow<ScreenState>(ScreenState.Loading)

    override val state: StateFlow<ScreenState> = _state
    override val lifecycleState = MutableStateFlow(LifecycleState.Hidden)

    private var loadJob: kotlinx.coroutines.Job? = null

    init {
        subscribeToData()
    }

    private fun subscribeToData() {
        loadJob?.cancel()
        loadJob = subscribeOnScreenDataUseCase(viewModelScope, lifecycleState)
            .onEach { refreshableData ->
                when (refreshableData) {
                    is RefreshableData.Loading -> _state.value = ScreenState.Loading
                    is RefreshableData.Failed -> _state.value = ScreenState.Error(refreshableData.error?.message)
                    is RefreshableData.Loaded -> _state.value = refreshableData.value
                }
            }
            .launchIn(viewModelScope)
    }

    override fun retryLoad() = subscribeToData()

}