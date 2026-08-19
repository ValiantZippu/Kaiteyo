package ua.syt0r.kanji.presentation.screen.main.screen.collection_detail

import org.koin.dsl.module
import ua.syt0r.kanji.presentation.multiplatformViewModel

val collectionDetailScreenModule = module {

    multiplatformViewModel {
        CollectionDetailViewModel(
            viewModelScope = it.component1(),
            knowledge = get(),
            catalog = get(),
            letterPracticeRepository = get()
        )
    }

}
