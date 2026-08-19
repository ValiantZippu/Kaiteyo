package ua.syt0r.kanji.di

import org.koin.core.module.Module
import ua.syt0r.kanji.core.coreModule
import ua.syt0r.kanji.core.transfer.importExportModule
import ua.syt0r.kanji.presentation.screen.main.mainScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.about.aboutScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.daily_limit.dailyLimitScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.deckDetailsScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.deckEditScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.deck_picker.deckPickerScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.feedback.feedbackScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.home.homeScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.dashboard_common.commonDashboardComponentModule
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.general_dashboard.generalDashboardScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.letters_dashboard.lettersDashboardScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.search.searchScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.settingsScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.vocab_dashboard.vocabDashboardScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.info.infoScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.kanji_entry.kanjiEntryScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.knowledge_explorer.knowledgeExplorerScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.knowledge_graph.knowledgeGraphScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.radical_explorer.radicalExplorerScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.learner_profile.learnerProfileScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.sentence.sentenceScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.sentence_entry.sentenceEntryScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.component_explorer.componentExplorerScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.browse_hub.browseHubScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.collection_detail.collectionDetailScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.letterPracticeScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.vocabPracticeScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.sync.syncScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.text_analysis.textAnalysisScreenModule
import ua.syt0r.kanji.presentation.screen.main.screen.vocab_card.vocabCardScreenModule
import ua.syt0r.kanji.core.game.gameModule
import ua.syt0r.kanji.presentation.screen.main.screen.game.gameCentreModule
import ua.syt0r.kanji.presentation.screen.main.screen.media.mediaCentreModule
import ua.syt0r.kanji.presentation.screen.main.screen.world.worldScreenModule

private val screenModules = listOf(
    mainScreenModule,
    homeScreenModule,
    commonDashboardComponentModule,
    generalDashboardScreenModule,
    lettersDashboardScreenModule,
    vocabDashboardScreenModule,
    searchScreenModule,
    settingsScreenModule,
    aboutScreenModule,
    deckPickerScreenModule,
    deckEditScreenModule,
    deckDetailsScreenModule,
    letterPracticeScreenModule,
    vocabPracticeScreenModule,
    infoScreenModule,
    syncScreenModule,
    feedbackScreenModule,
    dailyLimitScreenModule,
    textAnalysisScreenModule,
    knowledgeExplorerScreenModule,
    kanjiEntryScreenModule,
    knowledgeGraphScreenModule,
    radicalExplorerScreenModule,
    learnerProfileScreenModule,
    sentenceScreenModule,
    sentenceEntryScreenModule,
    componentExplorerScreenModule,
    browseHubScreenModule,
    collectionDetailScreenModule,
    vocabCardScreenModule,
    mediaCentreModule,
    gameCentreModule,
    gameModule,
    worldScreenModule
)

val appModules: List<Module> = screenModules + listOf(
    coreModule,
    importExportModule,
    platformComponentsModule
)