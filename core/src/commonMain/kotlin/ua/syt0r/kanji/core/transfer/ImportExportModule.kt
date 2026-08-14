package ua.syt0r.kanji.core.transfer

import org.koin.dsl.module
import ua.syt0r.kanji.presentation.multiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.features.DeckFeaturesController

val importExportModule = module {
    multiplatformViewModel<ImportExportContract.ViewModel> { parametersHolder ->
        ImportExportViewModel(
            coroutineScope = parametersHolder.component1(),
            deckFeaturesController = get(),
            ankiPackage = get()
        )
    }
}
