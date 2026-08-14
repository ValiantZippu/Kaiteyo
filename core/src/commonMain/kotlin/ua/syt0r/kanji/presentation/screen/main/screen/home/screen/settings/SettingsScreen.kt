package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingsScreenContract.ScreenState

@Composable
fun SettingsScreen(
    mainNavigationState: MainNavigationState,
    viewModel: SettingsScreenContract.ViewModel = getMultiplatformViewModel()
) {

    val state = viewModel.state.collectAsState()

    when (val screenState = state.value) {
        ScreenState.Loading -> {
            CircularProgressIndicator(Modifier.fillMaxSize().wrapContentSize())
        }

        is ScreenState.Loaded -> {
            SettingsCenterShell(
                categories = screenState.categories,
                mainNavigationState = mainNavigationState
            )
        }
    }

}
