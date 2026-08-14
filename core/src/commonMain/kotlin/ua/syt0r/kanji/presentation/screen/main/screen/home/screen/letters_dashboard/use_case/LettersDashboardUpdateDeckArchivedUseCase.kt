package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.letters_dashboard.use_case

import ua.syt0r.kanji.core.user_data.database.LetterPracticeRepository
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.letters_dashboard.LettersDashboardScreenContract

class LettersDashboardUpdateDeckArchivedUseCase(
    private val repository: LetterPracticeRepository
) : LettersDashboardScreenContract.UpdateDeckArchivedUseCase {

    override suspend operator fun invoke(deckId: Long, isArchived: Boolean) {
        repository.updateDeckArchived(deckId, isArchived)
    }

}
