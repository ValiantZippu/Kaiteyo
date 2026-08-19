package ua.syt0r.kanji.presentation.screen.main.screen.kanji_entry

import org.koin.dsl.module
import ua.syt0r.kanji.presentation.multiplatformViewModel

val kanjiEntryScreenModule = module {

    multiplatformViewModel {
        KanjiEntryViewModel(
            viewModelScope = it.component1(),
            knowledge = get(),
            layoutStore = get(),
            profileStore = get(),
            homeStore = get()
        )
    }

}
