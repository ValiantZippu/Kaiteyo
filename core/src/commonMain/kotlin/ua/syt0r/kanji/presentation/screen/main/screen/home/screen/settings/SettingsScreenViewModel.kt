package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingsScreenContract.ScreenState

class SettingsScreenViewModel(
    coroutineScope: CoroutineScope,
    defaultCategories: List<SettingsScreenContract.Category>,
    customCategories: List<SettingsScreenContract.Category>,
) : SettingsScreenContract.ViewModel {

    private val _state = MutableStateFlow<ScreenState>(ScreenState.Loading)
    override val state: StateFlow<ScreenState> = _state

    init {
        coroutineScope.launch {
            // Platform-specific categories (Android reminder, Google Play
            // analytics) come first so they surface next to the common ones.
            _state.value = ScreenState.Loaded(customCategories + defaultCategories)
        }
    }

}
