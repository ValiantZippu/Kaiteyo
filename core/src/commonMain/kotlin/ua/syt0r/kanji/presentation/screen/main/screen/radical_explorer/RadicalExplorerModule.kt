package ua.syt0r.kanji.presentation.screen.main.screen.radical_explorer

import org.koin.dsl.module
import ua.syt0r.kanji.presentation.multiplatformViewModel

val radicalExplorerScreenModule = module {

    multiplatformViewModel {
        RadicalExplorerViewModel(
            viewModelScope = it.component1(),
            knowledge = get()
        )
    }

}
