package ua.syt0r.kanji.presentation.screen.main.screen.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.analytics.AnalyticsManager
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.presentation.common.nav.LocalHomeNavigationState
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState

@Composable
fun HomeScreen(
    mainNavigationState: MainNavigationState,
    viewModel: HomeScreenContract.ViewModel = getMultiplatformViewModel(),
) {

    // First-run onboarding: gate the home shell until the user has set up
    // their JLPT target / daily limits (or explicitly skipped). Existing
    // users are unaffected — the flag defaults to completed.
    val appPreferences = koinInject<PreferencesContract.AppPreferences>()
    var onboardingCompleted by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        onboardingCompleted = appPreferences.onboardingCompleted.get()
    }
    if (!onboardingCompleted) {
        OnboardingWizard(
            onComplete = { onboardingCompleted = true }
        )
        return
    }

    val shellHomeNavigationState = LocalHomeNavigationState.current
    val defaultTabState = viewModel.defaultTab.collectAsState()
    val homeNavigationState = shellHomeNavigationState
        ?: rememberHomeNavigationState(defaultTabState.value)

    // Apply the preference-loaded default tab once, only overriding the initial
    // default (GeneralDashboard) — rememberSaveable preserves any user-selected tab.
    var defaultTabApplied by remember { mutableStateOf(false) }
    if (!defaultTabApplied && defaultTabState.value != HomeScreenTab.GeneralDashboard) {
        LaunchedEffect(defaultTabState.value) {
            homeNavigationState.navigate(defaultTabState.value)
            defaultTabApplied = true
        }
    }

    val tabContent = remember {
        movableContentOf { HomeNavigationContent(homeNavigationState, mainNavigationState) }
    }

    if (shellHomeNavigationState != null) {
        // Desktop shell renders the navigation chrome in NavShell
        Surface(Modifier.fillMaxSize()) {
            tabContent()
        }
    } else {
        HomeScreenUI(
            availableTabs = HomeScreenTab.VisibleTabs,
            selectedTabState = homeNavigationState.selectedTab,
            syncIconState = viewModel.syncIconState.collectAsState(),
            onTabSelected = { homeNavigationState.navigate(it) },
            onSyncButtonClick = {
                val isSyncStarted = viewModel.trySync()
                if (!isSyncStarted) mainNavigationState.navigate(MainDestination.Sync)
            }
        ) {

            tabContent()

        }
    }

    val analyticsManager = koinInject<AnalyticsManager>()
    LaunchedEffect(Unit) {
        snapshotFlow { homeNavigationState.selectedTab.value }
            .distinctUntilChanged()
            .onEach { analyticsManager.setScreen(it.analyticsName) }
            .launchIn(this)
    }

}