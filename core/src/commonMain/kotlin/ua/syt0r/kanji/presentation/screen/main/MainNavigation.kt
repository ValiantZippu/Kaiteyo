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
import ua.syt0r.kanji.presentation.screen.main.screen.sponsor.SponsorScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.sync.SyncScreen
import ua.syt0r.kanji.presentation.screen.main.screen.text_analysis.TextAnalysisScreen
import ua.syt0r.kanji.presentation.screen.main.screen.vocab_card.SuggestedVocabCardData
import ua.syt0r.kanji.presentation.screen.main.screen.vocab_card.VocabCardScreen
import ua.syt0r.kanji.presentation.screen.main.screen.vocab_card.VocabCardScreenMode
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardManager
import ua.syt0r.kanji.presentation.screen.main.screen.decks.DeckFeaturesHub
import ua.syt0r.kanji.presentation.screen.main.screen.decks.PluginManagerScreen
import ua.syt0r.kanji.presentation.screen.main.screen.decks.StatisticsDashboard
import ua.syt0r.kanji.presentation.screen.main.screen.decks.BackupManagerScreen
import ua.syt0r.kanji.presentation.screen.main.screen.decks.ImportExportScreen
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
    object Sponsor : MainDestination {

        override val analyticsName: String = "sponsor"

        @Composable
        override fun Content(state: MainNavigationState) {
            val content = koinInject<SponsorScreenContract.Content>()
            content(state)
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
            DeckFeaturesHub(
                navigationState = state,
                onClose = { state.navigateBack() }
            )
        }

    }

    @Serializable
    object CardBrowser : MainDestination {

        override val analyticsName: String = "card_browser"

        @Composable
        override fun Content(state: MainNavigationState) {
            CardManager()
        }

    }

    @Serializable
    object StatisticsDashboard : MainDestination {

        override val analyticsName: String = "statistics_dashboard"

        @Composable
        override fun Content(state: MainNavigationState) {
            StatisticsDashboard(
                cards = emptyList(),
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
            BackupManagerScreen(
                onDismiss = { state.navigateBack() },
                onCreateBackup = { /* TODO */ },
                onRestoreBackup = { /* TODO */ },
                onDeleteBackup = { /* TODO */ },
                onVerifyBackup = { /* TODO */ },
                onUpdateConfig = { /* TODO */ }
            )
        }

    }

    @Serializable
    object ImportExport : MainDestination {

        override val analyticsName: String = "import_export"

        @Composable
        override fun Content(state: MainNavigationState) {
            ImportExportScreen()
        }

    }

    @Serializable
    object TagManager : MainDestination {

        override val analyticsName: String = "tag_manager"

        @Composable
        override fun Content(state: MainNavigationState) {
            ua.syt0r.kanji.presentation.screen.main.screen.decks.TagManagerScreen(
                tags = emptyList(),
                onAddTag = { _, _, _ -> },
                onUpdateTag = { _, _, _, _ -> },
                onDeleteTag = { },
                onMergeTags = { _, _ -> },
                onClose = { state.navigateBack() }
            )
        }

    }

    @Serializable
    object FlagManager : MainDestination {

        override val analyticsName: String = "flag_manager"

        @Composable
        override fun Content(state: MainNavigationState) {
            // Flag selector is handled as a dialog from CardManager
            Text("Flag Manager - Select flags for your cards")
        }

    }

    @Serializable
    object NoteEditor : MainDestination {

        override val analyticsName: String = "note_editor"

        @Composable
        override fun Content(state: MainNavigationState) {
            Text("Note Editor - Add markdown notes to cards")
        }

    }

    @Serializable
    object CardStatusManager : MainDestination {

        override val analyticsName: String = "card_status"

        @Composable
        override fun Content(state: MainNavigationState) {
            Text("Card Status - Change between New, Learning, Young, Mature")
        }

    }

    @Serializable
    object ReviewSettings : MainDestination {

        override val analyticsName: String = "review_settings"

        @Composable
        override fun Content(state: MainNavigationState) {
            Text("Review Settings - Customize answer buttons and behavior")
        }

    }

    @Serializable
    object KeyboardShortcuts : MainDestination {

        override val analyticsName: String = "keyboard_shortcuts"

        @Composable
        override fun Content(state: MainNavigationState) {
            Text("Keyboard Shortcuts - Configure key bindings")
        }

    }

    @Serializable
    object StudyHistory : MainDestination {

        override val analyticsName: String = "study_history"

        @Composable
        override fun Content(state: MainNavigationState) {
            Text("Study History - Audit log of all reviews")
        }

    }

    @Serializable
    object HeatmapView : MainDestination {

        override val analyticsName: String = "heatmap"

        @Composable
        override fun Content(state: MainNavigationState) {
            Text("Heatmap - GitHub-style contribution heatmap")
        }

    }

    @Serializable
    object SearchEngine : MainDestination {

        override val analyticsName: String = "search_engine"

        @Composable
        override fun Content(state: MainNavigationState) {
            Text("Search Engine - Universal search across all cards")
        }

    }

    @Serializable
    object BulkActions : MainDestination {

        override val analyticsName: String = "bulk_actions"

        @Composable
        override fun Content(state: MainNavigationState) {
            Text("Bulk Actions - Multi-select operations on cards")
        }

    }

    @Serializable
    object UndoHistory : MainDestination {

        override val analyticsName: String = "undo_history"

        @Composable
        override fun Content(state: MainNavigationState) {
            Text("Undo History - Undo and redo actions")
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
    MainDestination.Sponsor.configuration(),
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
    MainDestination.HeatmapView.configuration(),
    MainDestination.SearchEngine.configuration(),
    MainDestination.BulkActions.configuration(),
    MainDestination.UndoHistory.configuration(),
)
