package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.search.use_case

import androidx.compose.runtime.mutableStateOf
import ua.syt0r.kanji.core.app_data.AppDataRepository
import ua.syt0r.kanji.core.japanese.isKana
import ua.syt0r.kanji.core.knowledge.normalizeForSearch
import ua.syt0r.kanji.presentation.common.PaginatableJapaneseWordList
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.search.SearchScreenContract

class SearchScreenProcessInputUseCase(
    private val appDataRepository: AppDataRepository
) : SearchScreenContract.ProcessInputUseCase {

    override suspend fun process(
        input: String
    ): SearchScreenContract.ScreenState {

        // Unify IME width/case and fold katakana to hiragana so queries match
        // dictionary spellings regardless of how the text was typed
        // (JapaneseTextNormalizer, KT-SEARCH-005).
        val normalized = input.normalizeForSearch()

        val knownCharacters = normalized.mapNotNull {
            val charString = it.toString()
            val areStrokesAvailable = appDataRepository.getStrokes(charString).isNotEmpty()

            val isKnown = areStrokesAvailable && when {
                it.isKana() -> true
                else -> appDataRepository.getReadings(charString).isNotEmpty()
            }

            if (isKnown) charString else null
        }


        val (wordsCount, words) = normalized.takeIf { it.isNotEmpty() }
            ?.let {
                val wordsCount = appDataRepository.getWordsWithTextCount(normalized)
                val words = appDataRepository.getWordsWithText(
                    text = normalized,
                    limit = SearchScreenContract.InitialWordsCount
                )
                wordsCount to words
            }
            ?: (0 to emptyList())

        return SearchScreenContract.ScreenState(
            isLoading = false,
            characters = knownCharacters,
            words = mutableStateOf(PaginatableJapaneseWordList(wordsCount, words)),
            query = normalized
        )
    }

}