package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.categories

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.intl.Locale
import kotlinx.datetime.LocalTime
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.core.user_data.preferences.PreferencesDefaultHomeTab
import ua.syt0r.kanji.presentation.common.resources.string.getStrings
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.LocalSettingsNavigation
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingDescriptor
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingsScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.DropdownSetting
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.InfoSetting
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.LinkSetting
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.SettingGroup
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.TimeSetting
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.rememberSetting

class GeneralSettingsCategory(
    private val appPreferences: PreferencesContract.AppPreferences
) : SettingsScreenContract.Category {

    private val strings = getStrings()
    private val s = strings.center
    private val homeStrings = strings.home

    override val id: String = "general"
    override val title: String = s.categoryGeneral
    override val subtitle: String = s.categoryGeneralSubtitle
    override val keywords: List<String> =
        listOf("startup", "start", "home", "tab", "reset", "time", "limit", "language", "daily")
    override val icon: ImageVector? = Icons.Default.Settings

    override val reset: (suspend () -> Unit)? = {
        appPreferences.defaultHomeTab.set(PreferencesDefaultHomeTab.GeneralDashboard)
        appPreferences.dailyResetTime.set(LocalTime(0, 0))
    }

    private fun defaultTabLabel(tab: PreferencesDefaultHomeTab): String = when (tab) {
        PreferencesDefaultHomeTab.GeneralDashboard -> homeStrings.generalDashboardTabLabel
        PreferencesDefaultHomeTab.Letters -> homeStrings.lettersDashboardTabLabel
        PreferencesDefaultHomeTab.Vocab -> homeStrings.vocabDashboardTabLabel
    }

    override val descriptors: List<SettingDescriptor> = listOf(
        SettingDescriptor(
            id = "default_tab",
            title = s.defaultTab,
            description = s.defaultTabDescription,
            keywords = listOf("home", "start", "initial", "screen", "dashboard"),
            render = { DefaultTabSetting() }
        ),
        SettingDescriptor(
            id = "daily_reset_time",
            title = s.dailyResetTime,
            description = s.dailyResetTimeDescription,
            keywords = listOf("reset", "schedule", "midnight", "day", "limit"),
            render = { DailyResetTimeSetting() }
        ),
        SettingDescriptor(
            id = "daily_limit",
            title = s.dailyLimit,
            description = s.dailyLimitDescription,
            keywords = listOf("new", "review", "cards", "limit", "cap"),
            render = { DailyLimitLink() }
        ),
        SettingDescriptor(
            id = "language",
            title = s.language,
            description = s.languageDescription,
            keywords = listOf("locale", "日本語", "english", "translation"),
            render = { LanguageSetting() }
        )
    )

    @Composable
    override fun content(mainNavigationState: MainNavigationState) {
        SettingGroup(
            title = s.groupStartup,
            children = listOf(
                { DefaultTabSetting() },
                { DailyResetTimeSetting() }
            )
        )
        SettingGroup(
            title = s.groupStudy,
            children = listOf(
                { DailyLimitLink() }
            )
        )
        SettingGroup(
            title = s.groupApplication,
            children = listOf(
                { LanguageSetting() }
            )
        )
    }

    // ============================================
    // SETTINGS
    // ============================================

    @Composable
    private fun DefaultTabSetting() {
        val binding = rememberSetting(appPreferences.defaultHomeTab)
        DropdownSetting(
            title = s.defaultTab,
            description = s.defaultTabDescription,
            options = PreferencesDefaultHomeTab.entries,
            labelOf = ::defaultTabLabel,
            selected = binding.value,
            onSelected = { binding.set(it) }
        )
    }

    @Composable
    private fun DailyResetTimeSetting() {
        val binding = rememberSetting(appPreferences.dailyResetTime)
        TimeSetting(
            title = s.dailyResetTime,
            description = s.dailyResetTimeDescription,
            value = binding.value,
            onChanged = { binding.set(it) }
        )
    }

    @Composable
    private fun DailyLimitLink() {
        val navigationState = LocalSettingsNavigation.current ?: return
        LinkSetting(
            title = s.dailyLimit,
            description = s.dailyLimitDescription,
            onClick = { navigationState.navigate(MainDestination.DailyLimit) }
        )
    }

    @Composable
    private fun LanguageSetting() {
        val languageLabel = if (Locale.current.language == "ja") "日本語" else "English"
        InfoSetting(
            title = s.language,
            description = s.languageDescription,
            value = languageLabel
        )
    }

}
