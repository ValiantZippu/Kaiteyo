package ua.syt0r.kanji.presentation.screen.main.screen.sentence_entry

import org.koin.dsl.module
import ua.syt0r.kanji.presentation.multiplatformViewModel

val sentenceEntryScreenModule = module {

    multiplatformViewModel {
        SentenceEntryViewModel(
            viewModelScope = it.component1(),
            analyzer = get(),
            profileStore = get()
        )
    }

}
