package ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.use_case

import ua.syt0r.kanji.core.srs.LetterPracticeType
import ua.syt0r.kanji.core.srs.SrsCardRepository
import ua.syt0r.kanji.core.srs.SrsScheduler
import ua.syt0r.kanji.presentation.screen.main.screen.deck_edit.LetterDeckEditListItem

interface ResetLetterSrsUseCase {
    suspend operator fun invoke(item: LetterDeckEditListItem)
}

class DefaultResetLetterSrsUseCase(
    private val srsCardRepository: SrsCardRepository,
    private val srsScheduler: SrsScheduler
) : ResetLetterSrsUseCase {

    override suspend fun invoke(item: LetterDeckEditListItem) {
        val freshCard = srsScheduler.newCard()
        LetterPracticeType.entries.forEach { practiceType ->
            srsCardRepository.update(practiceType.toSrsKey(item.character), freshCard)
        }
    }

}
