package ua.syt0r.kanji.core.knowledge

import ua.syt0r.kanji.core.app_data.AppDataRepository
import ua.syt0r.kanji.core.app_data.data.JapaneseWord
import ua.syt0r.kanji.core.app_data.data.KanjiListEntry
import ua.syt0r.kanji.core.app_data.data.KanjiMeaningEntry
import ua.syt0r.kanji.core.app_data.data.KanjiReadingEntry
import ua.syt0r.kanji.core.app_data.data.ReadingType
import ua.syt0r.kanji.core.app_data.Sentence
import ua.syt0r.kanji.core.japanese.isKanji

// ============================================================
// KNOWLEDGE REPOSITORY
// ------------------------------------------------------------
// Builds the Kaiteyo knowledge entities (kanji, radicals,
// components, words, sentences, grammar) from the bundled
// dictionary database. This is a pure facade over
// AppDataRepository — the UI never touches raw SQLDelight rows.
//
// Every relationship below maps to a real query:
//   kanji → radical / component   kanji_radical + radical
//   radical → kanji               getCharactersWithRadicals
//   kanji → words                 letter_vocab_example
//   word → sentences              sentence (LIKE over reading)
//   kanji → related kanji         shared-radical lookup
// ============================================================

class KnowledgeRepository(
    private val appData: AppDataRepository
) {

    // ---------------------------------------------------------------
    // KANJI
    // ---------------------------------------------------------------

    /** Loads a full kanji entry, or null when the character is not in the dataset. */
    suspend fun kanji(character: String): KanjiKnowledge? {
        if (character.length != 1) return null
        val data = appData.getData(character) ?: return null
        val meanings = appData.getMeanings(character)
        val readings = appData.getReadings(character)
        val classifications = KanjiTag.fromDbValues(appData.getClassificationsForKanji(character))
        return KanjiKnowledge(
            character = character,
            meanings = meanings,
            onReadings = readings.filterValues { it == ReadingType.ON }.keys.toList(),
            kunReadings = readings.filterValues { it == ReadingType.KUN }.keys.toList(),
            frequencyRank = data.frequency,
            classifications = classifications,
            strokeCount = appData.getKanjiStrokeCounts()[character],
            variantFamily = data.variantFamily,
            strokePaths = appData.getStrokes(character)
        )
    }

    /** Kanji entries for a list of characters (bulk). Missing characters are skipped. */
    suspend fun kanjiBatch(characters: Collection<String>): List<KanjiKnowledge> {
        if (characters.isEmpty()) return emptyList()
        return characters.distinct().mapNotNull { kanji(it) }
    }

    /**
     * Frequency rank per character (one bulk query). Used by the study
     * recommendation engine on Home — frequency is a real corpus rank,
     * never a synthesized score.
     */
    suspend fun kanjiFrequencyRanks(): Map<String, Int> =
        appData.getAllKanji().mapNotNull { entry ->
            entry.frequency?.let { entry.kanji to it }
        }.toMap()

    /** All kanji classifications as typed tags, keyed by character. */
    suspend fun kanjiTags(): Map<String, List<KanjiTag>> {
        val rows = appData.getAllClassifications()
        return rows
            .mapNotNull { entry -> KanjiTag.fromDbValue(entry.classification)?.let { entry.kanji to it } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, tags) -> tags.sortedBy { it.sortKey } }
    }

    // ---------------------------------------------------------------
    // RADICALS / COMPONENTS
    // ---------------------------------------------------------------

    /** All radicals in the dataset (the radical explorer's source grid). */
    suspend fun allRadicals(): List<RadicalKnowledge> =
        appData.getRadicals().map { RadicalKnowledge(it.radical, it.strokesCount) }

    /** The radicals inside [character], in stroke order. */
    suspend fun radicalsIn(character: String): List<RadicalInKanji> =
        appData.getRadicalsInCharacter(character).map {
            RadicalInKanji(it.radical, it.startPosition, it.strokesCount)
        }

    /** Kanji characters that contain ALL of [radicals] (intersection search). */
    suspend fun kanjiWithRadicals(radicals: List<String>): List<String> =
        appData.getCharactersWithRadicals(radicals)

    /**
     * The components of a kanji. In the bundled dataset the component graph
     * is radical-derived, so each component maps to the radical it came from.
     */
    suspend fun componentsIn(character: String): List<ComponentKnowledge> {
        val radicals = radicalsIn(character)
        val radicalStrokes = appData.getRadicals()
            .associate { it.radical to it.strokesCount }
        return radicals.map {
            ComponentKnowledge(
                component = it.radical,
                radicalOf = it.radical,
                strokesCount = radicalStrokes[it.radical] ?: it.strokesCount
            )
        }
    }

    /** Kanji that share at least one radical with [character] (related kanji). */
    suspend fun kanjiRelatedByRadical(character: String, limit: Int = 24): List<String> {
        val radicals = radicalsIn(character).map { it.radical }
        if (radicals.isEmpty()) return emptyList()
        val related = appData.getAllRadicalsInCharactersWithSelectedRadicals(radicals.toSet())
        return related.filter { it != character }.take(limit)
    }

    // ---------------------------------------------------------------
    // RADICAL EXPLORER
    // ---------------------------------------------------------------

    /**
     * A radical with its stroke count and the number of kanji that use it.
     * Used by the radical explorer's grid (counts drive the sort/filter).
     */
    suspend fun radicalStats(): List<RadicalStats> {
        val all = appData.getRadicals().map { RadicalStats(it.radical, it.strokesCount, 0) }
        val counts = all.map { stats ->
            val count = appData.getCharactersWithRadicals(listOf(stats.radical)).size
            stats.copy(kanjiCount = count)
        }
        return counts.sortedByDescending { it.kanjiCount }
    }

    /** Kanji entries using ALL of [radicals], with full knowledge data. */
    suspend fun kanjiForRadicals(
        radicals: List<String>,
        limit: Int = 60,
        offset: Int = 0
    ): List<KanjiKnowledge> {
        if (radicals.isEmpty()) return emptyList()
        val characters = appData.getCharactersWithRadicals(radicals)
            .drop(offset)
            .take(limit)
        return kanjiBatch(characters)
    }

    suspend fun kanjiForRadicalsCount(radicals: List<String>): Int {
        if (radicals.isEmpty()) return 0
        return appData.getCharactersWithRadicals(radicals).size
    }

    // ---------------------------------------------------------------
    // WORDS
    // ---------------------------------------------------------------

    /** Words that use [character] (kanji → vocabulary edge). */
    suspend fun wordsContaining(character: String, limit: Int = 30): List<WordKnowledge> =
        appData.getWordExamples(character)
            .take(limit)
            .map { it.toKnowledge() }

    /** Words whose reading contains [text] (kanji or kana). */
    suspend fun wordsWithText(
        text: String,
        offset: Int = 0,
        limit: Int = 30
    ): List<WordKnowledge> =
        appData.getWordsWithText(text, offset, limit).map { it.toKnowledge() }

    suspend fun wordsWithTextCount(text: String): Int = appData.getWordsWithTextCount(text)

    /** A single word by id. */
    suspend fun word(id: Long): WordKnowledge? =
        appData.findWords(id = id, kanjiReading = null, kanaReading = null)
            .firstOrNull()
            ?.toKnowledge()

    // ---------------------------------------------------------------
    // SENTENCES
    // ---------------------------------------------------------------

    /** Sentences containing [text] — used for word → sentence edges. */
    suspend fun sentencesWithText(
        text: String,
        offset: Int = 0,
        limit: Int = 20
    ): List<SentenceKnowledge> =
        appData.getSentencesWithText(text, offset, limit).map { it.toKnowledge() }

    suspend fun sentencesWithTextCount(text: String): Int =
        appData.getSentencesWithTextCount(text)

    // ---------------------------------------------------------------
    // WORD-CENTRIC HELPERS
    // ---------------------------------------------------------------

    /**
     * Sentences containing a word's reading (kanji or kana) — the real
     * word → sentence edge used by the explorer and the knowledge graph.
     */
    suspend fun sentencesForWordReading(word: WordKnowledge, limit: Int = 8): List<SentenceKnowledge> {
        val readings = listOfNotNull(word.kanaReading, word.kanjiReading)
            .filter { it.isNotBlank() }
            .distinct()
            .take(3)
        return readings
            .flatMap { sentencesWithText(it, limit = limit) }
            .distinctBy { it.text }
            .take(limit)
    }

    /** Kanji entries used in a word's spelling (the word → kanji edge). */
    suspend fun searchKanjiOfWord(word: WordKnowledge): List<KanjiKnowledge> {
        val characters = word.kanjiReading
            ?.filter { it.isKanji() }
            ?.map { it.toString() }
            .orEmpty()
        return kanjiBatch(characters)
    }

    // ---------------------------------------------------------------
    // GRAMMAR
    // ---------------------------------------------------------------

    /** Grammar matches inside a sentence (from the built-in catalog). */
    fun grammarIn(sentence: String): List<GrammarMatch> = GrammarCatalog.findIn(sentence)

    // ---------------------------------------------------------------
    // SEARCH INDEX
    // ---------------------------------------------------------------

    private var cachedKanjiIndex: KanjiSearchIndex? = null

    /**
     * The in-memory kanji search index, built once from real bulk queries
     * and cached. Invalidate after a data update.
     */
    suspend fun kanjiSearchIndex(): KanjiSearchIndex {
        cachedKanjiIndex?.let { return it }
        val index = KanjiSearchIndex(
            entries = appData.getAllKanji(),
            meanings = appData.getAllKanjiMeanings(),
            readings = appData.getAllKanjiReadings(),
            strokeCounts = appData.getKanjiStrokeCounts(),
            classifications = kanjiTags()
        )
        cachedKanjiIndex = index
        return index
    }

    fun invalidateKanjiSearchIndex() {
        cachedKanjiIndex = null
    }

    // ---------------------------------------------------------------
    // MAPPERS
    // ---------------------------------------------------------------

    private fun JapaneseWord.toKnowledge(): WordKnowledge = WordKnowledge(
        id = id,
        kanjiReading = reading.kanjiReading,
        kanaReading = reading.kanaReading,
        furigana = reading.furigana,
        glossary = glossary,
        partOfSpeech = partOfSpeechList
    )

    private fun Sentence.toKnowledge(): SentenceKnowledge = SentenceKnowledge(
        text = value,
        translation = translation,
        furigana = furigana,
        provenance = ContentProvenance(
            sourceType = ContentSourceType.Authoritative,
            sourceLabel = "Bundled corpus (Tatoeba)",
            confidence = ContentConfidence.High
        )
    )
}
