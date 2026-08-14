package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.vocab_dashboard.use_case

import ua.syt0r.kanji.core.user_data.database.VocabPracticeRepository
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.vocab_dashboard.VocabDashboardScreenContract

class UpdateVocabDeckArchivedUseCase(
    private val repository: VocabPracticeRepository
) : VocabDashboardScreenContract.UpdateDeckArchivedUseCase {

    override suspend operator fun invoke(deckId: Long, isArchived: Boolean) {
        repository.updateDeckArchived(deckId, isArchived)
    }

}
