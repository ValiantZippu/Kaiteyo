package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.StateFlow
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState


interface SettingsScreenContract {

    interface ViewModel {
        val state: StateFlow<ScreenState>
    }

    sealed interface ScreenState {
        object Loading : ScreenState
        data class Loaded(val categories: List<Category>) : ScreenState
    }

    /**
     * A settings category (General, Appearance, Navigation, …).
     *
     * [descriptors] power the instant search; the category page renders
     * [content]. [reset] restores every setting of the category (with a
     * confirmation dialog shown by the shell).
     */
    interface Category {

        val id: String
        val title: String
        val subtitle: String
        val keywords: List<String>
        val icon: ImageVector?

        val descriptors: List<SettingDescriptor>

        val reset: (suspend () -> Unit)?

        @Composable
        fun content(mainNavigationState: MainNavigationState)

    }

}
