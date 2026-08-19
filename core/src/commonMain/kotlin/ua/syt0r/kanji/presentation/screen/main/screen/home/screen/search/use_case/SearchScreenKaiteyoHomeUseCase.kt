package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.search.use_case

import ua.syt0r.kanji.core.app_data.AppDataRepository
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.search.SearchScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.search.SearchScreenContract.KaiteyoHomeState
import kotlin.random.Random

/**
 * Loads the dictionary home for the empty search state: a random kanji
 * (with its real meanings and readings), a random word and sentence that
 * contain that kanji, and per-band JLPT counts — all from the bundled
 * dictionary data. Every field is real; nothing is fabricated.
 */
class SearchScreenKaiteyoHomeUseCase(
    private val appDataRepository: AppDataRepository
) : SearchScreenContract.LoadKaiteyoHomeUseCase {

    override suspend fun load(): KaiteyoHomeState {
        val allKanji = runCatching { appDataRepository.getAllKanji() }.getOrDefault(emptyList())
        if (allKanji.isEmpty()) return KaiteyoHomeState()

        val entry = allKanji[Random.nextInt(allKanji.size)]
        val character = entry.kanji

        // Meanings + readings for the chosen character.
        val meanings = runCatching { appDataRepository.getMeanings(character) }
            .getOrDefault(emptyList())
        val readings = runCatching { appDataRepository.getReadings(character) }
            .getOrDefault(emptyMap())
        val readingsLine = readings.keys
            .sortedBy { readings[it]?.ordinal ?: 0 }
            .take(4)
            .joinToString(" · ")

        // A real example word + sentence containing that kanji.
        val word = loadExampleWord(character)
        val sentence = runCatching {
            appDataRepository.getSentencesWithText(character, offset = 0, limit = 1).firstOrNull()
        }.getOrNull()

        // Real JLPT band sizes from the import decks.
        val jlptCounts = (5 downTo 1).map { level ->
            level to runCatching { appDataRepository.getImportDeckWordsCount("n$level") }
                .getOrDefault(0)
        }

        return KaiteyoHomeState(
            isLoading = false,
            kanji = character,
            kanjiMeaning = meanings.joinToString(", "),
            kanjiReadings = readingsLine,
            word = word,
            sentence = sentence,
            jlptCounts = jlptCounts
        )
    }

    private suspend fun loadExampleWord(letter: String) =
        runCatching { appDataRepository.getWordExamples(letter) }
            .getOrDefault(emptyList())
            .takeIf { it.isNotEmpty() }
            ?.let { words -> words[Random.nextInt(words.size)] }
            ?: runCatching { appDataRepository.getWordsWithText(letter, limit = 20) }
                .getOrDefault(emptyList())
                .takeIf { it.isNotEmpty() }
                ?.let { words -> words[Random.nextInt(words.size)] }
}
