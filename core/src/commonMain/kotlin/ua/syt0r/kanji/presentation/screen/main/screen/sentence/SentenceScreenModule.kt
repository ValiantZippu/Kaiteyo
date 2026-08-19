package ua.syt0r.kanji.presentation.screen.main.screen.sentence

import org.koin.dsl.module
import ua.syt0r.kanji.presentation.multiplatformViewModel

val sentenceScreenModule = module {

    multiplatformViewModel {
        SentenceScreenViewModel(
            viewModelScope = it.component1(),
            knowledge = get(),
            analyzer = get()
        )
    }

}
