package ua.syt0r.kanji.presentation.screen.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import ua.syt0r.kanji.core.analytics.AnalyticsManager
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.presentation.common.resources.string.getStrings
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingDescriptor
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingsScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.SettingGroup
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.ToggleSetting
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.rememberSetting

// ============================================
// GOOGLE PLAY ANALYTICS CATEGORY
// Privacy toggle wired to Firebase analytics.
// Injected on the googlePlay flavor only.
// ============================================

class GooglePlayAnalyticsSettingsCategory(
    private val appPreferences: PreferencesContract.AppPreferences,
    private val analyticsManager: AnalyticsManager
) : SettingsScreenContract.Category {

    private val strings = getStrings()

    override val id: String = "analytics"
    override val title: String = strings.settings.analyticsTitle
    override val subtitle: String = strings.settings.analyticsMessage
    override val keywords: List<String> =
        listOf("analytics", "privacy", "telemetry", "usage", "data", "anonymous")
    override val icon: ImageVector? = Icons.Default.Insights

    override val reset: (suspend () -> Unit)? = {
        appPreferences.analyticsEnabled.set(true)
        analyticsManager.setAnalyticsEnabled(true)
    }

    override val descriptors: List<SettingDescriptor> = listOf(
        SettingDescriptor(
            id = "analytics_enabled",
            title = strings.settings.analyticsTitle,
            description = strings.settings.analyticsMessage,
            keywords = listOf("analytics", "privacy", "anonymous", "usage"),
            render = { AnalyticsToggle() }
        )
    )

    @Composable
    override fun content(mainNavigationState: MainNavigationState) {
        SettingGroup(
            title = null,
            children = listOf(
                { AnalyticsToggle() }
            )
        )
    }

    @Composable
    private fun AnalyticsToggle() {
        val binding = rememberSetting(appPreferences.analyticsEnabled)
        ToggleSetting(
            title = strings.settings.analyticsTitle,
            description = strings.settings.analyticsMessage,
            checked = binding.value,
            onChanged = { enabled ->
                binding.set(enabled)
                analyticsManager.setAnalyticsEnabled(enabled)
                analyticsManager.sendEvent("analytics_toggled") {
                    put("analytics_enabled", enabled)
                }
            }
        )
    }

}
