package ua.syt0r.kanji.core

import kotlinx.serialization.json.Json
import org.koin.dsl.binds
import org.koin.dsl.module
import ua.syt0r.kanji.core.analytics.AnalyticsManager
import ua.syt0r.kanji.core.analytics.PrintAnalyticsManager
import ua.syt0r.kanji.core.app_data.AppDataDatabaseProvider
import ua.syt0r.kanji.core.app_data.AppDataRepository
import ua.syt0r.kanji.core.app_data.SqlDelightAppDataRepository
import ua.syt0r.kanji.core.backup.BackupManager
import ua.syt0r.kanji.core.backup.BackupRestoreCompletionNotifier
import ua.syt0r.kanji.core.backup.BackupRestoreEventsProvider
import ua.syt0r.kanji.core.backup.BackupRestoreObservable
import ua.syt0r.kanji.core.backup.DefaultBackupManager
import ua.syt0r.kanji.core.feedback.DefaultFeedbackManager
import ua.syt0r.kanji.core.feedback.DefaultFeedbackUserDataProvider
import ua.syt0r.kanji.core.feedback.FeedbackManager
import ua.syt0r.kanji.core.feedback.FeedbackUserDataProvider
import ua.syt0r.kanji.core.japanese.CharacterClassifier
import ua.syt0r.kanji.core.japanese.DefaultCharacterClassifier
import ua.syt0r.kanji.core.knowledge.DatasetProvenanceRegistry
import ua.syt0r.kanji.core.knowledge.KeywordRegistry
import ua.syt0r.kanji.core.knowledge.KnowledgeGraphRepository
import ua.syt0r.kanji.core.knowledge.KnowledgeRepository
import ua.syt0r.kanji.core.knowledge.KnowledgeSearchEngine
import ua.syt0r.kanji.core.knowledge.NodeTraversal
import ua.syt0r.kanji.core.knowledge.LearnerProfileStore
import ua.syt0r.kanji.core.knowledge.LibraryCatalog
import ua.syt0r.kanji.core.knowledge.SentenceAnalyzer
import ua.syt0r.kanji.core.knowledge.WordSegmenter
import ua.syt0r.kanji.core.knowledge.home.HomeCommandCenterStore
import ua.syt0r.kanji.core.knowledge.cards.KanjiCardLayoutStore
import ua.syt0r.kanji.core.srs.applySrsDefinitions
import ua.syt0r.kanji.core.sync.addSyncDefinitions
import ua.syt0r.kanji.core.theme_manager.ThemeManager
import ua.syt0r.kanji.core.time.DefaultTimeUtils
import ua.syt0r.kanji.core.time.TimeUtils
import ua.syt0r.kanji.core.user_data.database.addUserDataDatabaseDefinitions
import ua.syt0r.kanji.core.user_data.preferences.BackupPropertiesHolder
import ua.syt0r.kanji.core.user_data.preferences.DataStorePreferencesManager
import ua.syt0r.kanji.core.user_data.preferences.DefaultPreferencesBackupManager
import ua.syt0r.kanji.core.user_data.preferences.DefaultUserPreferencesMigrationManager
import ua.syt0r.kanji.core.user_data.preferences.PreferencesBackupManager
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.core.user_data.preferences.PreferencesManager
import ua.syt0r.kanji.core.user_data.preferences.UserPreferencesMigrationManager

/**
 * Koin module that provides core (non-platform-specific) dependencies:
 * analytics, app data, backup, preferences, theme, character classification,
 * network API, feedback, and vocab card resolution.
 */
val coreModule = module {

    applySrsDefinitions()
    addNetworkClientsDefinitions()
    addSyncDefinitions()
    addAccountDefinitions()
    addUserDataDatabaseDefinitions()

    single<AnalyticsManager> { PrintAnalyticsManager() }

    single<AppDataRepository> {
        val deferredDatabase = get<AppDataDatabaseProvider>().provideAsync()
        SqlDelightAppDataRepository(deferredDatabase)
    }

    factory<PreferencesBackupManager> {
        DefaultPreferencesBackupManager(
            preferencesManager = get(),
            backupPropertiesHolder = get(),
            timeUtils = get()
        )
    }

    single<UserPreferencesMigrationManager> {
        DefaultUserPreferencesMigrationManager(
            dataStore = get()
        )
    }

    single {
        DataStorePreferencesManager(
            dataStore = get(),
            migrationManager = get(),
            timeUtils = get()
        )
    } binds arrayOf(
        PreferencesManager::class,
        BackupPropertiesHolder::class
    )

    single<PreferencesContract.AppPreferences> { get<PreferencesManager>().appPreferences }

    single<PreferencesContract.PracticePreferences> { get<PreferencesManager>().practicePreferences }

    single { BackupRestoreObservable() } binds arrayOf(
        BackupRestoreCompletionNotifier::class,
        BackupRestoreEventsProvider::class
    )

    factory<BackupManager> {
        DefaultBackupManager(
            userDataDatabaseManager = get(),
            preferencesBackupManager = get(),
            archiveHandler = get(),
            restoreCompletionNotifier = get()
        )
    }

    factory<TimeUtils> { DefaultTimeUtils }

    single<ThemeManager> {
        ThemeManager(appPreferences = get())
    }

    single<CharacterClassifier> { DefaultCharacterClassifier(appDataRepository = get()) }

    // ---- Kaiteyo knowledge core (dictionary domain, graph, search) ----
    single<KnowledgeRepository> { KnowledgeRepository(appData = get()) }
    single { KnowledgeGraphRepository(knowledge = get(), mediaReferences = get()) }
    single { NodeTraversal(knowledge = get(), graphRepository = get()) }
    single { KnowledgeSearchEngine(knowledge = get()) }
    single { KanjiCardLayoutStore(preferences = get()) }
    single { ua.syt0r.kanji.core.knowledge.cards.WordCardLayoutStore(preferences = get()) }
    single { ua.syt0r.kanji.core.knowledge.cards.SentenceCardLayoutStore(preferences = get()) }
    single { ua.syt0r.kanji.core.knowledge.cards.GrammarCardLayoutStore(preferences = get()) }
    single { ua.syt0r.kanji.core.knowledge.cards.CollectionCardLayoutStore(preferences = get()) }
    single { LearnerProfileStore(preferences = get()) }
    single { ua.syt0r.kanji.core.knowledge.level.DisplayOverridesStore(preferences = get()) }
    single { SentenceAnalyzer(repository = get()) }
    single { WordSegmenter(knowledge = get()) }
    single { ua.syt0r.kanji.core.knowledge.StudyStatusProvider(cards = get()) }
    single { LibraryCatalog(knowledge = get()) }
    single { HomeCommandCenterStore(preferences = get()) }
    single { ua.syt0r.kanji.core.knowledge.media.MediaReferenceStore(preferences = get()) }
    // Keyword + dataset provenance registries (KT-DATA-002/003): the data
    // layer populates these on load; UIs read them to show "where does this
    // data come from?" without fabricating sources.
    single { KeywordRegistry() }
    single { DatasetProvenanceRegistry() }
    // Node layer (ADR-0013, code part): the typed registries are enums
    // (NodeType / RelationshipType) — pure vocabulary, no storage, no DI
    // needed. Consumers reference them directly; storage is deferred to the
    // ADR's incremental implementation.
    single { ua.syt0r.kanji.core.knowledge.nodes.NodeRegistryFacade() }

    single<Json> { Json.Default }

    single<NetworkApi> {
        DefaultNetworkApi(
            networkClients = get(),
            json = Json { ignoreUnknownKeys = true }
        )
    }

    factory<FeedbackManager> {
        DefaultFeedbackManager(
            networkApi = get(),
            userDataProvider = get()
        )
    }

    factory<FeedbackUserDataProvider> {
        DefaultFeedbackUserDataProvider()
    }

    single {
        VocabCardResolver(
            vocabPracticeRepository = get(),
            appDataRepository = get()
        )
    }

}