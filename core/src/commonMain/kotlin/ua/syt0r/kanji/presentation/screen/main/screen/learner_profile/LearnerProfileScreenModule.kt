package ua.syt0r.kanji.presentation.screen.main.screen.learner_profile

import org.koin.dsl.module
import ua.syt0r.kanji.presentation.multiplatformViewModel

val learnerProfileScreenModule = module {

    multiplatformViewModel {
        LearnerProfileScreenViewModel(
            viewModelScope = it.component1(),
            store = get()
        )
    }

}
