package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject
import ua.syt0r.kanji.presentation.common.nav.LocalHomeNavigationState
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.features.DeckFeaturesController
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.deck_picker.data.DeckPickerScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.home.HomeScreenTab

@Composable
fun GeneralDashboardScreen(
    mainNavigationState: MainNavigationState,
    viewModel: GeneralDashboardScreenContract.ViewModel = getMultiplatformViewModel()
) {

    val deckFeaturesController = koinInject<DeckFeaturesController>()

    // Read in the composable body — the shell provides this on desktop so
    // "Continue studying" can jump to the Library tab when nothing is queued.
    val homeNavigationState = LocalHomeNavigationState.current

    // Keep the deck summaries loaded so the browser and other surfaces stay
    // in lockstep whenever a deck or a study session changes.
    LaunchedEffect(Unit) {
        deckFeaturesController.ensureLoaded()
        deckFeaturesController.loadDecks()
        deckFeaturesController.deckChangesFlow.collect {
            deckFeaturesController.loadDecks()
        }
    }

    GeneralDashboardScreenUI(
        state = viewModel.state.collectAsState(),
        navigateToDailyLimitConfiguration = {
            mainNavigationState.navigate(MainDestination.DailyLimit)
        },
        navigateToCreateLetterDeck = {
            val destination = MainDestination.DeckPicker(DeckPickerScreenConfiguration.Letters)
            mainNavigationState.navigate(destination)
        },
        navigateToCreateVocabDeck = {
            val destination = MainDestination.DeckPicker(DeckPickerScreenConfiguration.Vocab)
            mainNavigationState.navigate(destination)
        },
        navigateToLetterPractice = { mainNavigationState.navigate(it) },
        navigateToVocabPractice = { mainNavigationState.navigate(it) },
        navigateToDeckDetails = { deck ->
            val configuration = when (deck.category) {
                DashboardDeckCategory.Letters -> DeckDetailsScreenConfiguration.LetterDeck(deck.deckId)
                DashboardDeckCategory.Vocabulary -> DeckDetailsScreenConfiguration.VocabDeck(deck.deckId)
            }
            mainNavigationState.navigate(MainDestination.DeckDetails(configuration))
        },
        navigateToSearch = { mainNavigationState.navigate(MainDestination.SearchEngine) },
        navigateToCardBrowser = { mainNavigationState.navigate(MainDestination.CardBrowser()) },
        navigateToStatistics = { mainNavigationState.navigate(MainDestination.StatisticsDashboard) },        navigateToImportExport = {
            mainNavigationState.navigate(MainDestination.ImportExport)
        },
        navigateToCollections = {
            mainNavigationState.navigate(MainDestination.Collections)
        },
        navigateToDictionary = {
            mainNavigationState.navigate(MainDestination.KnowledgeExplorer())
        },
        navigateToRadicals = {
            mainNavigationState.navigate(MainDestination.RadicalExplorer)
        },
        navigateToKanjiBrowser = {
            mainNavigationState.navigate(MainDestination.KanjiBrowser())
        },
        navigateToSentences = {
            mainNavigationState.navigate(MainDestination.SentenceExplorer())
        },
        navigateToLearnerProfile = {
            mainNavigationState.navigate(MainDestination.LearnerProfile)
        },
        onOpenKanji = { character ->
            mainNavigationState.navigate(MainDestination.KanjiEntry(character))
        },
        onOpenWord = { wordId ->
            mainNavigationState.navigate(MainDestination.WordEntry(wordId))
        },
        onOpenBrowse = {
            mainNavigationState.navigate(MainDestination.BrowseHub)
        },
        onOpenRadicals = {
            mainNavigationState.navigate(MainDestination.RadicalExplorer)
        },
        onOpenComponents = {
            mainNavigationState.navigate(MainDestination.ComponentExplorer)
        },
        // Continue studying with an empty queue jumps to the Library tab so
        // the user can pick a deck instead of dead-ending on Home.
        navigateToLibrary = {
            if (homeNavigationState != null) {
                homeNavigationState.navigate(HomeScreenTab.Library)
            } else {
                mainNavigationState.navigate(MainDestination.DailyLimit)
            }
        },
        onOpenDay = { day ->
            mainNavigationState.navigate(MainDestination.DayPractice(day.toString()))
        },
        retryLoad = { viewModel.retryLoad() },
        textAnalysisClick = { mainNavigationState.navigate(MainDestination.TextAnalysis) }
    )

}
