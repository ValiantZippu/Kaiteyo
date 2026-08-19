package ua.syt0r.kanji.presentation.screen.main.screen.component_explorer

import org.koin.dsl.module
import ua.syt0r.kanji.presentation.multiplatformViewModel

val componentExplorerScreenModule = module {

    multiplatformViewModel {
        ComponentExplorerViewModel(
            viewModelScope = it.component1(),
            knowledge = get()
        )
    }

}
