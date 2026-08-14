package ua.syt0r.kanji.presentation.screen.main

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import org.koin.compose.koinInject
import ua.syt0r.kanji.presentation.common.ScreenLetterPracticeType
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.screen.about.AboutScreen
import ua.syt0r.kanji.presentation.screen.main.screen.account.AccountScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.backup.BackupScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.credits.CreditsScreen
import ua.syt0r.kanji.presentation.screen.main.screen.daily_limit.DailyLimitScreen
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.DeckDetailsScreen
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.DeckEditScreen
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.DeckEditScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.deck_picker.DeckPickerScreen
import ua.syt0r.kanji.presentation.screen.main.screen.deck_picker.data.DeckPickerScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.feedback.FeedbackScreen
import ua.syt0r.kanji.presentation.screen.main.screen.feedback.FeedbackTopic
import ua.syt0r.kanji.presentation.screen.main.screen.home.HomeScreen
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreen
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenData
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.LetterPracticeScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.VocabPracticeScreen
import ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.data.VocabPracticeScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.statistics.StatisticsScreen
import ua.syt0r.kanji.presentation.screen.main.screen.sync.SyncScreen
import ua.syt0r.kanji.presentation.screen.main.screen.text_analysis.TextAnalysisScreen
import ua.syt0r.kanji.presentation.screen.main.screen.vocab_card.SuggestedVocabCardData
import ua.syt0r.kanji.presentation.screen.main.screen.vocab_card.VocabCardScreen
import ua.syt0r.kanji.presentation.screen.main.screen.vocab_card.VocabCardScreenMode
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardManager
import ua.syt0r.kanji.presentation.screen.main.screen.decks.DeckFeaturesHub
import ua.syt0r.kanji.presentation.screen.main.screen.decks.PluginManagerScreen
import ua.syt0r.kanji.presentation.screen.main.screen.decks.BackupManagerScreen
import ua.syt0r.kanji.presentation.screen.main.screen.decks.ImportExportScreen
import ua.syt0r.kanji.presentation.screen.main.features.BackupRoute
import ua.syt0r.kanji.presentation.screen.main.features.BulkActionsRoute
import ua.syt0r.kanji.presentation.screen.main.features.CardBrowserRoute
import ua.syt0r.kanji.presentation.screen.main.features.CardStatusScreen
import ua.syt0r.kanji.presentation.screen.main.features.DeckBrowserRoute
import ua.syt0r.kanji.presentation.screen.main.features.StatisticsController
import ua.syt0r.kanji.presentation.screen.main.features.DayPracticeCardsRoute
import ua.syt0r.kanji.presentation.screen.main.features.FlagManagerRoute
import ua.syt0r.kanji.presentation.screen.main.features.HistoryRoute
import ua.syt0r.kanji.presentation.screen.main.features.ImportExportRoute
import ua.syt0r.kanji.presentation.screen.main.features.KeyboardShortcutsRoute
import ua.syt0r.kanji.presentation.screen.main.features.NoteEditorRoute
import ua.syt0r.kanji.presentation.screen.main.features.ReviewSettingsRoute
import ua.syt0r.kanji.presentation.screen.main.features.SearchRoute
import ua.syt0r.kanji.presentation.screen.main.features.TagManagerRoute
import ua.syt0r.kanji.presentation.screen.main.features.UndoHistoryScreen
import kotlin.reflect.KClass

interface MainNavigationState {
    val currentDestination: State<MainDestination?>
    fun navigateBack()
    fun popUpToHome()
    fun navigate(destination: MainDestination)
    fun navigateToTop(destination: MainDestination)
}

@Composable
expect fun rememberMainNavigationState(): MainNavigationState

@Composable
expect fun MainNavigation(state: MainNavigationState)

interface MainDestination {

    val analyticsName: String?

    @Composable
    fun Content(state: MainNavigationState)


    @Serializable
    object Home : MainDestination {

        override val analyticsName: String? = null

        @Composable
        override fun Content(state: MainNavigationState) {
            HomeScreen(mainNavigationState = state)
        }

    }

    @Serializable
    object About : MainDestination {

        override val analyticsName: String = "about"

        @Composable
        override fun Content(state: MainNavigationState) {
            AboutScreen(
                mainNavigationState = state
            )
        }

    }

    @Serializable
    object Credits : MainDestination {

        override val analyticsName: String = "credits"

        @Composable
        override fun Content(state: MainNavigationState) {
            CreditsScreen(state)
        }

    }

    @Serializable
    data class DeckPicker(
        val configuration: DeckPickerScreenConfiguration
    ) : MainDestination {

        override val analyticsName: String = "deck_picker"

        @Composable
        override fun Content(state: MainNavigationState) {
            DeckPickerScreen(
                configuration = configuration,
                mainNavigationState = state
            )
        }

    }

    @Serializable
    data class DeckEdit(
        val configuration: DeckEditScreenConfiguration
    ) : MainDestination {

        override val analyticsName: String = "deck_edit"

        @Composable
        override fun Content(state: MainNavigationState) {
            DeckEditScreen(
                configuration = configuration,
                mainNavigationState = state
            )
        }

    }

    @Serializable
    data class DeckDetails(
        val configuration: DeckDetailsScreenConfiguration
    ) : MainDestination {

        override val analyticsName: String = "deck_details"

        @Composable
        override fun Content(state: MainNavigationState) {
            DeckDetailsScreen(
                configuration = configuration,
                mainNavigationState = state
            )
        }
    }

    @Serializable
    data class LetterPractice(
        val configuration: LetterPracticeScreenConfiguration
    ) : MainDestination {

        override val analyticsName: String = when (configuration.practiceType) {
            ScreenLetterPracticeType.Writing -> "writing_practice"
            ScreenLetterPracticeType.Reading -> "reading_practice"
        }

        @Composable
        override fun Content(state: MainNavigationState) {
            val content = koinInject<LetterPracticeScreenContract.Content>()
            content(
                configuration = configuration,
                mainNavigationState = state,
                viewModel = getMultiplatformViewModel()
            )
        }

    }

    @Serializable
    data class VocabPractice(
        val configuration: VocabPracticeScreenConfiguration
    ) : MainDestination {

        override val analyticsName: String = "vocab_practice"

        @Composable
        override fun Content(state: MainNavigationState) {
            VocabPracticeScreen(
                configuration = configuration,
                mainNavigationState = state
            )
        }

    }

    @Serializable
    data class Info(
        val data: InfoScreenData
    ) : MainDestination {

        override val analyticsName: String = "info"

        @Composable
        override fun Content(state: MainNavigationState) {
            InfoScreen(
                screenData = data,
                mainNavigationState = state
            )
        }

    }

    @Serializable
    object Backup : MainDestination {

        override val analyticsName: String = "backup"

        @Composable
        override fun Content(state: MainNavigationState) {
            val content = koinInject<BackupScreenContract.Content>()
            content(state)
        }

    }

    @Serializable
    data class Feedback(
        val topic: FeedbackTopic
    ) : MainDestination {

        override val analyticsName: String = "feedback"

        @Composable
        override fun Content(state: MainNavigationState) {
            FeedbackScreen(
                feedbackTopic = topic,
                mainNavigationState = state
            )
        }

    }

    @Serializable
    object DailyLimit : MainDestination {

        override val analyticsName: String = "daily_limit"

        @Composable
        override fun Content(state: MainNavigationState) {
            DailyLimitScreen(state)
        }

    }

    @Serializable
    data class Account(
        val screenData: AccountScreenContract.ScreenData? = null
    ) : MainDestination {

        override val analyticsName: String = "account"

        @Composable
        override fun Content(state: MainNavigationState) {
            val content = koinInject<AccountScreenContract.Content>()
            content(state, screenData)
        }

    }

    @Serializable
    object Sync : MainDestination {

        override val analyticsName: String = "sync"

        @Composable
        override fun Content(state: MainNavigationState) {
            SyncScreen(
                mainNavigationState = state
            )
        }

    }

    @Serializable
    object AppearanceStudio : MainDestination {

        override val analyticsName: String = "appearance_studio"

        @Composable
        override fun Content(state: MainNavigationState) {
            ua.syt0r.kanji.presentation.screen.main.screen.settings.AppearanceStudio()
        }

    }

    @Serializable
    object TextAnalysis : MainDestination {

        override val analyticsName: String = "text_analysis"

        @Composable
        override fun Content(state: MainNavigationState) {
            TextAnalysisScreen(
                navigationState = state
            )
        }

    }

    @Serializable
    data class VocabCard(
        val screenMode: VocabCardScreenMode,
        val cardData: SuggestedVocabCardData
    ) : MainDestination {

        override val analyticsName: String = "vocab_card"

        @Composable
        override fun Content(state: MainNavigationState) {
            VocabCardScreen(
                navigationState = state,
                screenMode = screenMode,
                cardData = cardData
            )
        }

    }

    // ==================== DECK FEATURES ====================

    @Serializable
    object DeckBrowser : MainDestination {

        override val analyticsName: String = "deck_browser"

        @Composable
        override fun Content(state: MainNavigationState) {
            DeckBrowserRoute(
                controller = koinInject(),
                onClose = { state.navigateBack() }
            )
        }

    }

    @Serializable
    object CardBrowser : MainDestination {

        override val analyticsName: String = "card_browser"

        @Composable
        override fun Content(state: MainNavigationState) {
            CardBrowserRoute(
                controller = koinInject(),
                onClose = { state.navigateBack() }
            )
        }

    }

    @Serializable
    object StatisticsDashboard : MainDestination {

        override val analyticsName: String = "statistics_dashboard"

        @Composable
        override fun Content(state: MainNavigationState) {
            // Single unified statistics destination — the old StatisticsDashboardV2
            // screen and its route wrappers have been consolidated into this one.
            StatisticsScreen(
                controller = koinInject<StatisticsController>(),
                onClose = { state.navigateBack() },
                onOpenLibraryDay = { day ->
                    state.navigate(MainDestination.DayPractice(day.toString()))
                }
            )
        }

    }

    @Serializable
    data class DayPractice(
        val day: String
    ) : MainDestination {

        override val analyticsName: String = "day_practice"

        @Composable
        override fun Content(state: MainNavigationState) {
            DayPracticeCardsRoute(
                day = day,
                onClose = { state.navigateBack() }
            )
        }

    }

    @Serializable
    object PluginManager : MainDestination {

        override val analyticsName: String = "plugin_manager"

        @Composable
        override fun Content(state: MainNavigationState) {
            PluginManagerScreen(
                onClose = { state.navigateBack() }
            )
        }

    }

    @Serializable
    object BackupManager : MainDestination {

        override val analyticsName: String = "backup_manager"

        @Composable
        override fun Content(state: MainNavigationState) {
            BackupRoute(
                controller = koinInject(),
                onClose = { state.navigateBack() }
            )
        }

    }

    @Serializable
    object ImportExport : MainDestination {

        override val analyticsName: String = "import_export"

        @Composable
        override fun Content(state: MainNavigationState) {
            ImportExportRoute(
                controller = koinInject(),
                onClose = { state.navigateBack() }
            )
        }

    }

    @Serializable
    object TagManager : MainDestination {

        override val analyticsName: String = "tag_manager"

        @Composable
        override fun Content(state: MainNavigationState) {
            TagManagerRoute(
                controller = koinInject(),
                onClose = { state.navigateBack() }
            )
        }

    }

    @Serializable
    object FlagManager : MainDestination {

        override val analyticsName: String = "flag_manager"

        @Composable
        override fun Content(state: MainNavigationState) {
            FlagManagerRoute(
                controller = koinInject(),
                onClose = { state.navigateBack() }
            )
        }

    }

    @Serializable
    object NoteEditor : MainDestination {

        override val analyticsName: String = "note_editor"

        @Composable
        override fun Content(state: MainNavigationState) {
            NoteEditorRoute(
                controller = koinInject(),
                onClose = { state.navigateBack() }
            )
        }

    }

    @Serializable
    object CardStatusManager : MainDestination {

        override val analyticsName: String = "card_status"

        @Composable
        override fun Content(state: MainNavigationState) {
            CardStatusScreen(
                controller = koinInject(),
                onClose = { state.navigateBack() }
            )
        }

    }

    @Serializable
    object ReviewSettings : MainDestination {

        override val analyticsName: String = "review_settings"

        @Composable
        override fun Content(state: MainNavigationState) {
            ReviewSettingsRoute(
                controller = koinInject(),
                onClose = { state.navigateBack() }
            )
        }

    }

    @Serializable
    object KeyboardShortcuts : MainDestination {

        override val analyticsName: String = "keyboard_shortcuts"

        @Composable
        override fun Content(state: MainNavigationState) {
            KeyboardShortcutsRoute(
                controller = koinInject(),
                onClose = { state.navigateBack() }
            )
        }

    }

    @Serializable
    object StudyHistory : MainDestination {

        override val analyticsName: String = "study_history"

        @Composable
        override fun Content(state: MainNavigationState) {
            HistoryRoute(
                controller = koinInject(),
                onClose = { state.navigateBack() }
            )
        }

    }

    @Serializable
    object SearchEngine : MainDestination {

        override val analyticsName: String = "search_engine"

        @Composable
        override fun Content(state: MainNavigationState) {
            SearchRoute(
                controller = koinInject(),
                onClose = { state.navigateBack() }
            )
        }

    }

    @Serializable
    object BulkActions : MainDestination {

        override val analyticsName: String = "bulk_actions"

        @Composable
        override fun Content(state: MainNavigationState) {
            BulkActionsRoute(
                controller = koinInject(),
                onClose = { state.navigateBack() }
            )
        }

    }

    @Serializable
    object UndoHistory : MainDestination {

        override val analyticsName: String = "undo_history"

        @Composable
        override fun Content(state: MainNavigationState) {
            UndoHistoryScreen(
                controller = koinInject(),
                onClose = { state.navigateBack() }
            )
        }

    }

    // ==================== KAITEYO REDESIGN ====================

    @Serializable
    data class KanjiBrowser(
        val criteria: ua.syt0r.kanji.presentation.screen.main.screen.kanji_browser.KanjiBrowserCriteria =
            ua.syt0r.kanji.presentation.screen.main.screen.kanji_browser.KanjiBrowserCriteria()
    ) : MainDestination {

        override val analyticsName: String = "kanji_browser"

        @Composable
        override fun Content(state: MainNavigationState) {
            val dataCenter = koinInject<ua.syt0r.kanji.presentation.screen.main.features.KaiteyoDataCenter>()
            androidx.compose.runtime.LaunchedEffect(Unit) { dataCenter.ensureLoaded() }
            ua.syt0r.kanji.presentation.screen.main.screen.kanji_browser.KanjiBrowserScreen(
                navigationState = state,
                dataCenter = dataCenter,
                initialCriteria = criteria
            )
        }

    }

    @Serializable
    object Collections : MainDestination {

        override val analyticsName: String = "collections"

        @Composable
        override fun Content(state: MainNavigationState) {
            val dataCenter = koinInject<ua.syt0r.kanji.presentation.screen.main.features.KaiteyoDataCenter>()
            androidx.compose.runtime.LaunchedEffect(Unit) { dataCenter.ensureLoaded() }
            ua.syt0r.kanji.presentation.screen.main.screen.kanji_browser.CollectionsScreen(
                navigationState = state,
                dataCenter = dataCenter
            )
        }

    }

}

sealed interface MainDestinationConfiguration<T : MainDestination> {

    val clazz: KClass<T>
    val subclassRegisterer: (PolymorphicModuleBuilder<MainDestination>) -> Unit

    data class NoParams<T : MainDestination>(
        val instance: T,
        override val clazz: KClass<T>,
        override val subclassRegisterer: (PolymorphicModuleBuilder<MainDestination>) -> Unit
    ) : MainDestinationConfiguration<T>

    data class WithArguments<T : MainDestination>(
        override val clazz: KClass<T>,
        override val subclassRegisterer: (PolymorphicModuleBuilder<MainDestination>) -> Unit
    ) : MainDestinationConfiguration<T>

}

inline fun <reified T : MainDestination> T.configuration(): MainDestinationConfiguration.NoParams<T> {
    return MainDestinationConfiguration.NoParams(
        instance = this,
        clazz = T::class,
        subclassRegisterer = {
            it.subclass(
                subclass = T::class,
                serializer = kotlinx.serialization.serializer()
            )
        }
    )
}

inline fun <reified T : MainDestination> KClass<T>.configuration(): MainDestinationConfiguration.WithArguments<T> {
    return MainDestinationConfiguration.WithArguments(
        clazz = this,
        subclassRegisterer = {
            it.subclass(
                subclass = this@configuration,
                serializer = kotlinx.serialization.serializer()
            )
        }
    )
}

val defaultMainDestinations: List<MainDestinationConfiguration<*>> = listOf(
    MainDestination.Home.configuration(),
    MainDestination.Backup.configuration(),
    MainDestination.About.configuration(),
    MainDestination.Credits.configuration(),
    MainDestination.DailyLimit.configuration(),
    MainDestination.Sync.configuration(),
    MainDestination.TextAnalysis.configuration(),
    MainDestination.AppearanceStudio.configuration(),
    MainDestination.VocabCard::class.configuration(),
    MainDestination.DeckPicker::class.configuration(),
    MainDestination.DeckDetails::class.configuration(),
    MainDestination.DeckEdit::class.configuration(),
    MainDestination.Feedback::class.configuration(),
    MainDestination.Info::class.configuration(),
    MainDestination.LetterPractice::class.configuration(),
    MainDestination.VocabPractice::class.configuration(),
    MainDestination.Account::class.configuration(),
    MainDestination.DeckBrowser.configuration(),
    MainDestination.CardBrowser.configuration(),
    MainDestination.StatisticsDashboard.configuration(),
    MainDestination.DayPractice::class.configuration(),
    MainDestination.PluginManager.configuration(),
    MainDestination.BackupManager.configuration(),
    MainDestination.ImportExport.configuration(),
    MainDestination.TagManager.configuration(),
    MainDestination.FlagManager.configuration(),
    MainDestination.NoteEditor.configuration(),
    MainDestination.CardStatusManager.configuration(),
    MainDestination.ReviewSettings.configuration(),
    MainDestination.KeyboardShortcuts.configuration(),
    MainDestination.StudyHistory.configuration(),
    MainDestination.SearchEngine.configuration(),
    MainDestination.BulkActions.configuration(),
    MainDestination.UndoHistory.configuration(),
    MainDestination.KanjiBrowser::class.configuration(),
    MainDestination.Collections.configuration(),
)
