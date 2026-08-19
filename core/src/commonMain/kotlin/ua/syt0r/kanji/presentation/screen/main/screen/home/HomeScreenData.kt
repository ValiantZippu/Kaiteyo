package ua.syt0r.kanji.presentation.screen.main.screen.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import org.koin.compose.koinInject
import ua.syt0r.kanji.presentation.common.resources.icon.ExtraIcons
import ua.syt0r.kanji.presentation.common.resources.icon.HomeOutline
import ua.syt0r.kanji.presentation.common.resources.string.StringResolveScope
import ua.syt0r.kanji.presentation.common.textDp
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.features.BrowseTabBrowser
import ua.syt0r.kanji.presentation.screen.main.features.StatisticsController
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard.GeneralDashboardScreen
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingsScreen
import ua.syt0r.kanji.presentation.screen.main.screen.library.LibraryScreen
import ua.syt0r.kanji.presentation.screen.main.screen.statistics.StatisticsScreen

enum class HomeScreenTab(
    val analyticsName: String,
    val iconContent: @Composable () -> Unit,
    val titleResolver: StringResolveScope<String>,
    val content: @Composable (MainNavigationState) -> Unit
) {

    GeneralDashboard(
        analyticsName = "general_dashboard",
        iconContent = { Icon(ExtraIcons.HomeOutline, null) },
        titleResolver = { home.generalDashboardTabLabel },
        content = { GeneralDashboardScreen(it) }
    ),
    Library(
        analyticsName = "library",
        iconContent = {
            Text(
                text = "書",
                fontSize = 18.textDp,
                fontWeight = FontWeight.Bold
            )
        },
        titleResolver = { home.libraryTabLabel },
        content = { LibraryScreen(navigationState = it) }
    ),
    Stats(
        analyticsName = "stats",
        iconContent = { Icon(Icons.Default.QueryStats, null) },
        titleResolver = { home.statsTabLabel },
        content = { HomeStatsTab(it) }
    ),
    Search(
        analyticsName = "search",
        iconContent = { Icon(Icons.Default.Search, null) },
        titleResolver = { home.searchTabLabel },
        content = { BrowseTabBrowser() }
    ),
    Settings(
        analyticsName = "settings",
        iconContent = { Icon(Icons.Outlined.Settings, null) },
        titleResolver = { home.settingsTabLabel },
        content = { SettingsScreen(it) }
    );

    val buttonTestTag = name

    companion object {
        val VisibleTabs: List<HomeScreenTab> = entries
    }

}

/**
 * The Stats tab renders the unified Kaiteyo analytics dashboard — the single
 * Statistics implementation in the app. Every number is computed by
 * [StatisticsController] from real database data.
 */
@Composable
private fun HomeStatsTab(navigationState: MainNavigationState) {
    val controller: StatisticsController = koinInject()
    StatisticsScreen(
        controller = controller,
        onClose = null,
        onOpenLibraryDay = { day ->
            navigationState.navigate(MainDestination.DayPractice(day.toString()))
        }
    )
}

data class SyncIconState(
    val loading: Boolean = false,
    val indicator: SyncIconIndicator = SyncIconIndicator.Disabled
)

enum class SyncIconIndicator {
    Disabled,
    PendingUpload,
    UpToDate,
    Canceled,
    Error,
    Conflict
}
