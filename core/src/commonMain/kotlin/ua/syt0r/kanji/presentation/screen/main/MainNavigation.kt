package ua.syt0r.kanji.presentation.screen.main

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import kotlinx.coroutines.launch
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
import ua.syt0r.kanji.presentation.screen.main.screen.game.GameScreen
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
                onClose = { state.navigateBack() },
                onOpenDeck = { deckId ->
                    deckDetailsFor(deckId)?.let { configuration ->
                        state.navigate(MainDestination.DeckDetails(configuration))
                    }
                },
                onBrowse = { deckId ->
                    state.navigate(MainDestination.CardBrowser(deckId = deckId))
                },
                onFindContent = {
                    state.navigate(MainDestination.SearchEngine)
                }
            )
        }

    }

    @Serializable
    data class CardBrowser(
        val deckId: String? = null
    ) : MainDestination {

        override val analyticsName: String = "card_browser"

        @Composable
        override fun Content(state: MainNavigationState) {
            CardBrowserRoute(
                controller = koinInject(),
                onClose = { state.navigateBack() },
                initialDeckId = deckId
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
                // Create/restore launch the real file-based backup screen — the
                // deck-features manager is a history + launcher, never a fake engine.
                onOpenBackup = { state.navigate(MainDestination.Backup) },
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

    // ==================== KNOWLEDGE EXPLORER ====================

    /**
     * The dictionary explorer — universal grouped search, kanji / word
     * entries and the progressively-expanding knowledge graph, all driven by
     * the knowledge core (real bundled dictionary data).
     */
    @Serializable
    data class KnowledgeExplorer(
        val query: String = ""
    ) : MainDestination {

        override val analyticsName: String = "knowledge_explorer"

        @Composable
        override fun Content(state: MainNavigationState) {
            ua.syt0r.kanji.presentation.screen.main.screen.knowledge_explorer.KnowledgeExplorerScreen(
                initialQuery = query,
                onClose = { state.navigateBack() }
            )
        }

    }

    // ==================== KANJI ENTRY ====================

    /**
     * The modular kanji page. Content is a persisted, user-configurable
     * sequence of cards (meaning, readings, components, words, sentences,
     * graph…) — see the KanjiEntry screen and the KanjiCardLayout store.
     */
    @Serializable
    data class KanjiEntry(
        val character: String
    ) : MainDestination {

        override val analyticsName: String = "kanji_entry"

        @Composable
        override fun Content(state: MainNavigationState) {
            ua.syt0r.kanji.presentation.screen.main.screen.kanji_entry.KanjiEntryScreen(
                character = character,
                onClose = { state.navigateBack() },
                onOpenWord = { wordId ->
                    state.navigate(
                        MainDestination.WordEntry(wordId = wordId)
                    )
                },
                onOpenGraph = {
                    state.navigate(MainDestination.KnowledgeGraph(character))
                },
                onOpenKanji = { relatedCharacter ->
                    state.navigate(MainDestination.KanjiEntry(relatedCharacter))
                },
                onOpenSentence = { sentence, translation ->
                    state.navigate(MainDestination.SentenceEntry(sentence, translation))
                }
            )
        }

    }

    // ==================== WORD ENTRY ====================

    /** The word page — readings, meanings, kanji connections, examples. */
    @Serializable
    data class WordEntry(
        val wordId: Long
    ) : MainDestination {

        override val analyticsName: String = "word_entry"

        @Composable
        override fun Content(state: MainNavigationState) {
            ua.syt0r.kanji.presentation.screen.main.screen.knowledge_explorer.WordEntryScreen(
                wordId = wordId,
                onClose = { state.navigateBack() },
                onOpenKanji = { character ->
                    state.navigate(MainDestination.KanjiEntry(character))
                },
                onOpenSentence = { sentence, translation ->
                    state.navigate(MainDestination.SentenceEntry(sentence, translation))
                }
            )
        }

    }

    // ==================== LEARNER PROFILE ====================

    /**
     * The level-adaptation picker (spec §23–§24). Profiles adapt
     * presentation defaults — furigana, romaji, translations, depth,
     * sentence difficulty, graph complexity, card preset. They never
     * delete data.
     */
    @Serializable
    object LearnerProfile : MainDestination {

        override val analyticsName: String = "learner_profile"

        @Composable
        override fun Content(state: MainNavigationState) {
            ua.syt0r.kanji.presentation.screen.main.screen.learner_profile.LearnerProfileScreen(
                onClose = { state.navigateBack() }
            )
        }

    }

    /** Sentence corpus explorer — search sentences, then open any result. */
    @Serializable
    data class SentenceExplorer(
        val query: String = ""
    ) : MainDestination {

        override val analyticsName: String = "sentence_explorer"

        @Composable
        override fun Content(state: MainNavigationState) {
            ua.syt0r.kanji.presentation.screen.main.screen.sentence.SentenceScreen(
                initialQuery = query,
                onClose = { state.navigateBack() },
                onOpenKanji = { character ->
                    state.navigate(MainDestination.KanjiEntry(character))
                },
                onOpenWord = { wordId ->
                    state.navigate(MainDestination.WordEntry(wordId))
                }
            )
        }

    }

    // ==================== CARD SETTINGS ====================

    /**
     * The card customization screen (spec §20–§21). Lets the user pick
     * which entity type to configure, then show/hide/reorder its cards
     * and apply presets. Every toggle persists through the real layout
     * store for that entity type.
     */
    @Serializable
    data class CardSettings(
        val entityType: String = "kanji"
    ) : MainDestination {

        override val analyticsName: String = "card_settings"

        @Composable
        override fun Content(state: MainNavigationState) {
            val entity = ua.syt0r.kanji.core.knowledge.cards.CardEntityType.entries
                .firstOrNull { it.name.equals(entityType, ignoreCase = true) }
                ?: ua.syt0r.kanji.core.knowledge.cards.CardEntityType.Kanji
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            ua.syt0r.kanji.presentation.common.ui.cards.CardSettingsScreen(
                entityType = entity,
                onBack = { state.navigateBack() },
                onPresetSelected = { presetId ->
                    // Persist the preset layout for this entity type immediately.
                    scope.launch { persistPreset(entity, presetId) }
                },
                onSave = { order, hidden ->
                    scope.launch { persistLayout(entity, order, hidden) }
                    state.navigateBack()
                }
            )
        }

    }

    // ==================== WORLD ====================

    /**
     * Kaiteyo World — the Kamakura vertical slice. A streamable 3D
     * Japan over the World runtime: live state readouts, movement,
     * camera switch, teleport to real landmarks, save/load.
     */
    @Serializable
    object World : MainDestination {

        override val analyticsName: String = "world"

        @Composable
        override fun Content(state: MainNavigationState) {
            ua.syt0r.kanji.presentation.screen.main.screen.world.WorldScreen(
                navigationState = state,
                onClose = { state.navigateBack() }
            )
        }

    }

    // ==================== KNOWLEDGE GRAPH ====================

    /**
     * The standalone knowledge-graph explorer. A pan/zoom canvas over the
     * progressively-expanding graph rooted at [root] (kanji / radical /
     * word / sentence / grammar node).
     */
    @Serializable
    data class KnowledgeGraph(
        val root: String
    ) : MainDestination {

        override val analyticsName: String = "knowledge_graph"

        @Composable
        override fun Content(state: MainNavigationState) {
            ua.syt0r.kanji.presentation.screen.main.screen.knowledge_graph.KnowledgeGraphScreen(
                root = root,
                onClose = { state.navigateBack() },
                onOpenKanji = { character ->
                    state.navigate(MainDestination.KanjiEntry(character))
                }
            )
        }

    }

    // ==================== RADICAL EXPLORER ====================

    /**
     * The radical explorer. A first-class radical browser: selectable grid,
     * stroke / JLPT / grade filtering, kanji previews and word drill-down —
     * RADICAL → KANJI → WORDS → SENTENCES.
     */
    @Serializable
    object RadicalExplorer : MainDestination {

        override val analyticsName: String = "radical_explorer"

        @Composable
        override fun Content(state: MainNavigationState) {
            ua.syt0r.kanji.presentation.screen.main.screen.radical_explorer.RadicalExplorerScreen(
                onClose = { state.navigateBack() },
                onOpenKanji = { character ->
                    state.navigate(MainDestination.KanjiEntry(character))
                }
            )
        }

    }

    // ==================== SENTENCE ENTRY ====================

    /**
     * A corpus sentence page with interactive tokens (tap a token → kanji /
     * word entry), grammar highlighting and a difficulty estimate.
     */
    @Serializable
    data class SentenceEntry(
        val sentence: String,
        val translation: String = ""
    ) : MainDestination {

        override val analyticsName: String = "sentence_entry"

        @Composable
        override fun Content(state: MainNavigationState) {
            ua.syt0r.kanji.presentation.screen.main.screen.sentence_entry.SentenceEntryScreen(
                sentence = sentence,
                translation = translation,
                onClose = { state.navigateBack() },
                onOpenKanji = { character ->
                    state.navigate(MainDestination.KanjiEntry(character))
                },
                onOpenWord = { wordId ->
                    state.navigate(MainDestination.WordEntry(wordId = wordId))
                }
            )
        }

    }

    // ==================== COMPONENT EXPLORER ====================

    /**
     * The component explorer — components are first-class entities. A
     * component grid with real kanji counts; select one to see every kanji
     * built from it, then drill into words and sentences.
     */
    @Serializable
    object ComponentExplorer : MainDestination {

        override val analyticsName: String = "component_explorer"

        @Composable
        override fun Content(state: MainNavigationState) {
            ua.syt0r.kanji.presentation.screen.main.screen.component_explorer.ComponentExplorerScreen(
                onClose = { state.navigateBack() },
                onOpenKanji = { character ->
                    state.navigate(MainDestination.KanjiEntry(character))
                }
            )
        }

    }

    // ==================== BROWSE HUB ====================

    /**
     * The browse hub — "Explore Japanese": kanji by JLPT/grade, radicals,
     * components, grammar patterns and library collections, all with real
     * dataset counts.
     */
    @Serializable
    object BrowseHub : MainDestination {

        override val analyticsName: String = "browse_hub"

        @Composable
        override fun Content(state: MainNavigationState) {
            ua.syt0r.kanji.presentation.screen.main.screen.browse_hub.BrowseHubScreen(
                onClose = { state.navigateBack() },
                onOpenRadicalExplorer = { state.navigate(MainDestination.RadicalExplorer) },
                onOpenComponentExplorer = { state.navigate(MainDestination.ComponentExplorer) },
                onOpenKanjiBrowser = { state.navigate(MainDestination.KanjiBrowser()) },
                onOpenRecommended = { levels ->
                    state.navigate(
                        MainDestination.KanjiBrowser(
                            ua.syt0r.kanji.presentation.screen.main.screen.kanji_browser.KanjiBrowserCriteria(
                                jlptLevels = levels
                            )
                        )
                    )
                },
                onOpenCollection = { collectionId ->
                    state.navigate(MainDestination.CollectionDetail(collectionId))
                },
                onOpenCollections = { state.navigate(MainDestination.Collections) }
            )
        }

    }

    // ==================== COLLECTION DETAIL ====================

    /**
     * A library collection's page (JLPT / grade / course lesson): header with
     * the real kanji count plus lazy-loaded kanji entries — COLLECTION →
     * KANJI → WORDS → SENTENCES.
     */
    @Serializable
    data class CollectionDetail(
        val collectionId: String
    ) : MainDestination {

        override val analyticsName: String = "collection_detail"

        @Composable
        override fun Content(state: MainNavigationState) {
            ua.syt0r.kanji.presentation.screen.main.screen.collection_detail.CollectionDetailScreen(
                collectionId = collectionId,
                onClose = { state.navigateBack() },
                onOpenKanji = { character ->
                    state.navigate(MainDestination.KanjiEntry(character))
                },
                onOpenDeck = { deckId ->
                    state.navigate(
                        MainDestination.DeckDetails(
                            ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsScreenConfiguration.LetterDeck(deckId)
                        )
                    )
                }
            )
        }

    }

    // ==================== MEDIA CENTRE ====================

    /**
     * The Media Centre — the immersion workspace (player, subtitles,
     * dictionary, mining). The desktop app provides the real implementation
     * via [ua.syt0r.kanji.presentation.screen.main.screen.media.MediaCentreContent];
     * other platforms show an honest "desktop only" screen so the navigation
     * entry is never a dead link.
     */
    @Serializable
    object Media : MainDestination {

        override val analyticsName: String = "media"

        @Composable
        override fun Content(state: MainNavigationState) {
            val content = koinInject<ua.syt0r.kanji.presentation.screen.main.screen.media.MediaCentreContent>()
            content.Content(
                navigationState = state,
                onClose = { state.navigateBack() }
            )
        }

    }

    // ==================== KAITEYO WORLD (GAME) ====================

    /**
     * Kaiteyo World destination alias. Routes directly to the unified [World] experience.
     */
    @Serializable
    object Game : MainDestination {

        override val analyticsName: String = "world"

        @Composable
        override fun Content(state: MainNavigationState) {
            World.Content(state)
        }

    }

}

/**
 * Maps a Deck Browser row id ("letter:123" / "vocab:123") to the real deck
 * details configuration so opening a deck opens the actual deck screen.
 */
private fun deckDetailsFor(deckId: String): DeckDetailsScreenConfiguration? {
    val rawId = deckId.removePrefix("letter:").removePrefix("vocab:").toLongOrNull() ?: return null
    return when {
        deckId.startsWith("letter:") -> DeckDetailsScreenConfiguration.LetterDeck(rawId)
        deckId.startsWith("vocab:") -> DeckDetailsScreenConfiguration.VocabDeck(rawId)
        else -> null
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
    MainDestination.CardBrowser::class.configuration(),
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
    MainDestination.KnowledgeExplorer::class.configuration(),
    MainDestination.KanjiEntry::class.configuration(),
    MainDestination.WordEntry::class.configuration(),
    MainDestination.LearnerProfile.configuration(),
    MainDestination.SentenceEntry::class.configuration(),
    MainDestination.SentenceExplorer::class.configuration(),
    MainDestination.KnowledgeGraph::class.configuration(),
    MainDestination.RadicalExplorer.configuration(),
    MainDestination.ComponentExplorer.configuration(),
    MainDestination.BrowseHub.configuration(),
    MainDestination.CollectionDetail::class.configuration(),
    MainDestination.Media.configuration(),
    MainDestination.Game.configuration(),
    MainDestination.World.configuration(),
    MainDestination.CardSettings::class.configuration(),
)

// ============================================================
// Card-settings persistence helpers
// ------------------------------------------------------------
// Persist a card layout or preset for an entity type through its
// real layout store (Koin-injected). Unknown stores fall back to
// the kanji store — layouts are per-type JSON blobs.
// ============================================================

private suspend fun persistPreset(
    entity: ua.syt0r.kanji.core.knowledge.cards.CardEntityType,
    presetId: String
) {
    val koin = org.koin.mp.KoinPlatform.getKoin()
    when (entity) {
        ua.syt0r.kanji.core.knowledge.cards.CardEntityType.Kanji -> {
            val preset = ua.syt0r.kanji.core.knowledge.cards.KanjiCardPresets.byId(presetId) ?: return
            koin.get<ua.syt0r.kanji.core.knowledge.cards.KanjiCardLayoutStore>().save(preset.layout)
        }
        ua.syt0r.kanji.core.knowledge.cards.CardEntityType.Word -> {
            val preset = ua.syt0r.kanji.core.knowledge.cards.WordCardPresets.byId(presetId) ?: return
            koin.get<ua.syt0r.kanji.core.knowledge.cards.WordCardLayoutStore>().save(preset.layout)
        }
        ua.syt0r.kanji.core.knowledge.cards.CardEntityType.Sentence -> {
            val preset = ua.syt0r.kanji.core.knowledge.cards.SentenceCardPresets.byId(presetId) ?: return
            koin.get<ua.syt0r.kanji.core.knowledge.cards.SentenceCardLayoutStore>().save(preset.layout)
        }
        ua.syt0r.kanji.core.knowledge.cards.CardEntityType.Grammar -> {
            val preset = ua.syt0r.kanji.core.knowledge.cards.GrammarCardPresets.byId(presetId) ?: return
            koin.get<ua.syt0r.kanji.core.knowledge.cards.GrammarCardLayoutStore>().save(preset.layout)
        }
        ua.syt0r.kanji.core.knowledge.cards.CardEntityType.Collection -> {
            val preset = ua.syt0r.kanji.core.knowledge.cards.CollectionCardPresets.byId(presetId) ?: return
            koin.get<ua.syt0r.kanji.core.knowledge.cards.CollectionCardLayoutStore>().save(preset.layout)
        }
    }
}

private suspend fun persistLayout(
    entity: ua.syt0r.kanji.core.knowledge.cards.CardEntityType,
    order: List<String>,
    hidden: Set<String>
) {
    val koin = org.koin.mp.KoinPlatform.getKoin()
    when (entity) {
        ua.syt0r.kanji.core.knowledge.cards.CardEntityType.Kanji -> {
            val store = koin.get<ua.syt0r.kanji.core.knowledge.cards.KanjiCardLayoutStore>()
            // Preserve per-card settings (KT-CARD-005): the CardSettings
            // screen only edits order/hidden — rebuilding the layout from
            // scratch here would silently wipe the item limits set in the
            // kanji page's edit mode.
            val settings = store.load().cardSettings
            store.save(
                ua.syt0r.kanji.core.knowledge.cards.KanjiCardLayout(
                    order = order, hidden = hidden, cardSettings = settings
                )
            )
        }
        ua.syt0r.kanji.core.knowledge.cards.CardEntityType.Word ->
            koin.get<ua.syt0r.kanji.core.knowledge.cards.WordCardLayoutStore>()
                .save(ua.syt0r.kanji.core.knowledge.cards.WordCardLayout(order = order, hidden = hidden))
        ua.syt0r.kanji.core.knowledge.cards.CardEntityType.Sentence ->
            koin.get<ua.syt0r.kanji.core.knowledge.cards.SentenceCardLayoutStore>()
                .save(ua.syt0r.kanji.core.knowledge.cards.SentenceCardLayout(order = order, hidden = hidden))
        ua.syt0r.kanji.core.knowledge.cards.CardEntityType.Grammar ->
            koin.get<ua.syt0r.kanji.core.knowledge.cards.GrammarCardLayoutStore>()
                .save(ua.syt0r.kanji.core.knowledge.cards.GrammarCardLayout(order = order, hidden = hidden))
        ua.syt0r.kanji.core.knowledge.cards.CardEntityType.Collection ->
            koin.get<ua.syt0r.kanji.core.knowledge.cards.CollectionCardLayoutStore>()
                .save(ua.syt0r.kanji.core.knowledge.cards.CollectionCardLayout(order = order, hidden = hidden))
    }
}
