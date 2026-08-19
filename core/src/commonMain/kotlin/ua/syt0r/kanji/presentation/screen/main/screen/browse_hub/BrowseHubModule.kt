package ua.syt0r.kanji.presentation.screen.main.screen.browse_hub

import org.koin.dsl.module
import ua.syt0r.kanji.presentation.multiplatformViewModel

val browseHubScreenModule = module {

    multiplatformViewModel<BrowseHubContract.ViewModel> {
        BrowseHubViewModel(
            viewModelScope = it.component1(),
            knowledge = get(),
            catalog = get()
        )
    }

}
