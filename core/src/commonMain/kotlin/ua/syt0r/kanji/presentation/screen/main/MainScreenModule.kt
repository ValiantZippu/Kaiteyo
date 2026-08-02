package ua.syt0r.kanji.presentation.screen.main

import org.koin.dsl.module
import ua.syt0r.kanji.presentation.multiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.features.DeepLinkHandler
import ua.syt0r.kanji.presentation.screen.main.features.DeckFeaturesController
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoDataCenter

val mainScreenModule = module {

    multiplatformViewModel<MainContract.ViewModel> {
        MainScreenViewModel(
            viewModelScope = it.component1(),
            appPreferences = get(),
            accountManager = get(),
            migrationObservable = get(),
            syncManager = get()
        )
    }

    single { DeepLinkHandler() }

    single {
        KaiteyoDataCenter(
            appDataRepository = get(),
            fsrsCardRepository = get(),
            cardDatabaseManager = get(),
            reviewHistoryRepository = get(),
            timeUtils = get()
        )
    }

    single {
        DeckFeaturesController(
            dataCenter = get(),
            cardDatabaseManager = get(),
            reviewHistoryRepository = get(),
            fsrsCardRepository = get(),
            appPreferences = get(),
            timeUtils = get()
        )
    }

}