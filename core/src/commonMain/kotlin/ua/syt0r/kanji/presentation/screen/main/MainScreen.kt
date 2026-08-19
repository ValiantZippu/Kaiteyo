package ua.syt0r.kanji.presentation.screen.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.knowledge.home.HomeCommandCenterStore
import ua.syt0r.kanji.core.analytics.AnalyticsManager
import ua.syt0r.kanji.core.user_data.database.DatabaseMigrationState
import ua.syt0r.kanji.presentation.common.nav.LocalNavBarBottomSpace
import ua.syt0r.kanji.presentation.common.nav.NavShell
import ua.syt0r.kanji.presentation.dialog.VersionChangeDialog
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.features.CommandPaletteOverlay
import ua.syt0r.kanji.presentation.screen.main.features.DeepLinkHandler
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoDataCenter
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoPalette
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoSearch
import ua.syt0r.kanji.presentation.screen.main.features.MigrationDialog
import ua.syt0r.kanji.presentation.screen.main.features.PaletteAction
import ua.syt0r.kanji.presentation.screen.main.features.SyncDialog
import ua.syt0r.kanji.presentation.screen.main.features.UniversalSearchOverlay

@Composable
fun MainScreen(
    deepLinkHandler: DeepLinkHandler
) {

    val viewModel = getMultiplatformViewModel<MainContract.ViewModel>()
    val navigationState = rememberMainNavigationState()

    // Universal search: route results through the real navigation state and
    // persist committed queries into the Home command-center store (the
    // store is Koin-injected here so the overlay itself stays storage-free).
    val homeCommandCenterStore = koinInject<HomeCommandCenterStore>()
    val searchScope = rememberCoroutineScope()
    LaunchedEffect(navigationState) {
        KaiteyoSearch.controller.onNavigate = { destination ->
            navigationState.navigate(destination)
        }
        KaiteyoSearch.controller.onSearchRecorded = { query ->
            searchScope.launch {
                homeCommandCenterStore.recordSearch(query, Clock.System.now().toEpochMilliseconds())
            }
        }
    }
    val migrationState = viewModel.migrationState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val dataCenter = koinInject<KaiteyoDataCenter>()

    // Space the docked bottom navigation bar occupies. NavShell writes it via
    // LocalNavBarBottomSpace; the SnackbarHost pads itself so snackbars always
    // render above the bar instead of behind it (see NavShell.kt).
    val navBarBottomSpace = remember { mutableStateOf(0.dp) }

    LaunchedEffect(Unit) { dataCenter.ensureLoaded() }

    LaunchedEffect(Unit) {
        KaiteyoPalette.controller.setActions(
            buildList {
                add(
                    PaletteAction(
                        title = "Kanji Browser",
                        subtitle = "Search, filter, browse all kanji",
                        keywords = "kanji browse search jlpt radical",
                        category = "Navigate"
                    ) { navigationState.navigate(MainDestination.KanjiBrowser()) }
                )
                add(
                    PaletteAction(
                        title = "Collections",
                        subtitle = "Smart collections, tags and flags",
                        keywords = "collections tags flags favorites",
                        category = "Navigate"
                    ) { navigationState.navigate(MainDestination.Collections) }
                )
                add(
                    PaletteAction(
                        title = "Favorites",
                        subtitle = "Only favorite kanji",
                        keywords = "favorites star starred",
                        category = "Filter"
                    ) {
                        navigationState.navigate(
                            MainDestination.KanjiBrowser(
                                ua.syt0r.kanji.presentation.screen.main.screen.kanji_browser.KanjiBrowserCriteria(
                                    favoritesOnly = true
                                )
                            )
                        )
                    }
                )
                add(
                    PaletteAction(
                        title = "Flagged kanji",
                        subtitle = "Kanji with any flag set",
                        keywords = "flagged flags color",
                        category = "Filter"
                    ) {
                        navigationState.navigate(
                            MainDestination.KanjiBrowser(
                                ua.syt0r.kanji.presentation.screen.main.screen.kanji_browser.KanjiBrowserCriteria(
                                    showFlagged = true
                                )
                            )
                        )
                    }
                )
                add(
                    PaletteAction(
                        title = "Difficult kanji",
                        subtitle = "Kanji above difficulty threshold",
                        keywords = "difficult hard problems",
                        category = "Filter"
                    ) {
                        navigationState.navigate(
                            MainDestination.KanjiBrowser(
                                ua.syt0r.kanji.presentation.screen.main.screen.kanji_browser.KanjiBrowserCriteria(
                                    showDifficult = true
                                )
                            )
                        )
                    }
                )
                add(
                    PaletteAction(
                        title = "Frequently failed",
                        subtitle = "Kanji with 3+ lapses",
                        keywords = "failed lapses mistakes",
                        category = "Filter"
                    ) {
                        navigationState.navigate(
                            MainDestination.KanjiBrowser(
                                ua.syt0r.kanji.presentation.screen.main.screen.kanji_browser.KanjiBrowserCriteria(
                                    minLapses = 3
                                )
                            )
                        )
                    }
                )
                add(
                    PaletteAction(
                        title = "Card Manager",
                        subtitle = "Browse and manage deck cards",
                        keywords = "decks cards manager anki",
                        category = "Navigate"
                    ) { navigationState.navigate(MainDestination.CardBrowser()) }
                )
                add(
                    PaletteAction(
                        title = "Statistics",
                        subtitle = "Dashboard and review stats",
                        keywords = "stats statistics dashboard heatmap",
                        category = "Navigate"
                    ) { navigationState.navigate(MainDestination.StatisticsDashboard) }
                )
                add(
                    PaletteAction(
                        title = "Dictionary Explorer",
                        subtitle = "Search kanji, words, sentences and grammar — explore the knowledge graph",
                        keywords = "dictionary kanji word sentence grammar graph knowledge explore",
                        category = "Navigate"
                    ) { navigationState.navigate(MainDestination.KnowledgeExplorer()) }
                )
                add(
                    PaletteAction(
                        title = "Radical Explorer",
                        subtitle = "Find kanji by radical — stroke, JLPT and grade filters",
                        keywords = "radical explorer kangxi stroke jlpt grade find kanji",
                        category = "Navigate"
                    ) { navigationState.navigate(MainDestination.RadicalExplorer) }
                )
                add(
                    PaletteAction(
                        title = "Component Explorer",
                        subtitle = "Every component and the kanji built from it",
                        keywords = "component explorer decomposition parts kanji structure",
                        category = "Navigate"
                    ) { navigationState.navigate(MainDestination.ComponentExplorer) }
                )
                add(
                    PaletteAction(
                        title = "Browse",
                        subtitle = "Explore Japanese by JLPT, grade, radicals, grammar and collections",
                        keywords = "browse explore jlpt grade kanji radicals grammar collections",
                        category = "Navigate"
                    ) { navigationState.navigate(MainDestination.BrowseHub) }
                )
                add(
                    PaletteAction(
                        title = "JLPT / Grade collections",
                        subtitle = "Kanji by JLPT level or school grade",
                        keywords = "jlpt grade collections kanji list",
                        category = "Navigate"
                    ) { navigationState.navigate(MainDestination.BrowseHub) }
                )
                add(
                    PaletteAction(
                        title = "Universal Search",
                        subtitle = "Search kanji, words, sentences and grammar from anywhere",
                        keywords = "search universal dictionary kanji word sentence grammar",
                        category = "Navigate",
                        shortcut = "Ctrl+Shift+F"
                    ) { KaiteyoSearch.controller.toggle() }
                )
                add(
                    PaletteAction(
                        title = "Media Centre",
                        subtitle = "Immersion workspace: player, subtitles, dictionary, mining",
                        keywords = "media video anime player subtitles immersion dictionary mining",
                        category = "Navigate"
                    ) { navigationState.navigate(MainDestination.Media) }
                )
                add(
                    PaletteAction(
                        title = "Close palette",
                        subtitle = "Dismiss this menu",
                        keywords = "close exit dismiss esc",
                        category = "App",
                        shortcut = "Esc"
                    ) { KaiteyoPalette.controller.close() }
                )
            }
        )
    }

    CompositionLocalProvider(LocalNavBarBottomSpace provides navBarBottomSpace) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = navBarBottomSpace.value),
                    snackbar = { NotificationSnackbar(it) }
                )
            }
        ) {
            NavShell(navigationState = navigationState) {
                MainNavigation(navigationState)
            }
        }
    }

    CommandPaletteOverlay()
    UniversalSearchOverlay()

    deepLinkHandler.HandleDeepLinksLaunchedEffect(navigationState)

    HandleScreenReportsLaunchedEffect(
        navigationState = navigationState
    )

    val currentMigrationState = migrationState.value
    if (currentMigrationState is DatabaseMigrationState.Running) {
        MigrationDialog(
            currentState = currentMigrationState
        )
        return
    }

    if (viewModel.showVersionChangeDialog.value) {
        VersionChangeDialog { viewModel.showVersionChangeDialog.value = false }
        return
    }

    HandleSnackbarNotificationsLaunchedEffect(
        notifications = viewModel.notifications,
        snackbarHostState = snackbarHostState,
        navigationState = navigationState
    )

    SyncDialog(
        state = viewModel.syncDialogState.collectAsState(),
        cancelSync = viewModel::cancelSync,
        resolveConflict = viewModel::resolveSyncConflict,
        navigateToAccount = {
            viewModel.cancelSync()
            navigationState.navigate(MainDestination.Account())
        }
    )

}

@Composable
private fun HandleScreenReportsLaunchedEffect(navigationState: MainNavigationState) {
    val analyticsManager = koinInject<AnalyticsManager>()
    LaunchedEffect(Unit) {
        snapshotFlow { navigationState.currentDestination.value }
            .map { it?.analyticsName }
            .filterNotNull()
            .onEach { analyticsManager.setScreen(it) }
            .launchIn(this)
    }
}

@Composable
private fun HandleSnackbarNotificationsLaunchedEffect(
    notifications: SharedFlow<MainSnackbarNotification>,
    snackbarHostState: SnackbarHostState,
    navigationState: MainNavigationState
) {
    LaunchedEffect(Unit) {
        notifications.collectLatest { notification ->
            val result = snackbarHostState.showSnackbar(notification)
            if (result == SnackbarResult.ActionPerformed) {
                val destination = notification.handleAction()
                if (destination != null) {
                    navigationState.navigate(destination)
                }
            }
        }
    }
}

@Composable
private fun NotificationSnackbar(snackbarData: SnackbarData) {
    val notification = snackbarData.visuals as MainSnackbarNotification
    when {
        notification.isError -> {
            Snackbar(
                snackbarData = snackbarData,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                actionColor = MaterialTheme.colorScheme.onErrorContainer,
                actionContentColor = MaterialTheme.colorScheme.onErrorContainer,
                dismissActionContentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }

        else -> {
            Snackbar(
                snackbarData = snackbarData,
                containerColor = MaterialTheme.colorScheme.surfaceDim,
                contentColor = MaterialTheme.colorScheme.onSurface,
                actionColor = MaterialTheme.colorScheme.primary,
                actionContentColor = MaterialTheme.colorScheme.primary,
                dismissActionContentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
