package ua.syt0r.kanji.presentation.screen.main.screen.knowledge_explorer

import org.koin.dsl.module
import ua.syt0r.kanji.presentation.multiplatformViewModel

val knowledgeExplorerScreenModule = module {

    multiplatformViewModel {
        KnowledgeExplorerViewModel(
            viewModelScope = it.component1(),
            knowledge = get(),
            searchEngine = get(),
            graphRepository = get(),
            profileStore = get()
        )
    }

}
