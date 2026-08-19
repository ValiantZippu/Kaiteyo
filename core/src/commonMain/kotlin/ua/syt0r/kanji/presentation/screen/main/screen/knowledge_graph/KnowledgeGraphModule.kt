package ua.syt0r.kanji.presentation.screen.main.screen.knowledge_graph

import org.koin.dsl.module
import ua.syt0r.kanji.presentation.multiplatformViewModel

val knowledgeGraphScreenModule = module {

    multiplatformViewModel {
        KnowledgeGraphViewModel(
            viewModelScope = it.component1(),
            repository = get()
        )
    }

}
