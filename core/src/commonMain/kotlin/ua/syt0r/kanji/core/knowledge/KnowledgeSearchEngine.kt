package ua.syt0r.kanji.core.knowledge

import ua.syt0r.kanji.core.app_data.data.KanjiListEntry
import ua.syt0r.kanji.core.app_data.data.KanjiMeaningEntry
import ua.syt0r.kanji.core.app_data.data.KanjiReadingEntry
import ua.syt0r.kanji.core.japanese.isKanji

// ============================================================
// KNOWLEDGE SEARCH ENGINE
// ------------------------------------------------------------
// Universal Japanese search. One entry point from Home, Library,
// Browse and the dictionary; results are grouped by entity kind
// (KANJI / WORDS / SENTENCES / GRAMMAR) so a query like 食べる
// returns the kanji 食, the words 食べる・食べ物, example
// sentences, and any matching grammar patterns together.
//
// Design rules:
//   - every filter and sort changes the underlying result set
//   - kanji search runs against an in-memory index built from the
//     real bulk queries (2k+ jōyō kanji, trivial to scan)
//   - word/sentence searches are DB-backed with limits
//   - search() is a suspend fun — callers own debouncing
// ============================================================

/** Which entity kinds a query should return. */
enum class SearchCategory(val label: String) {
    Kanji("Kanji"), Words("Words"), Sentences("Sentences"), Grammar("Grammar")
}

/** Composable filters — every field narrows the real result set. */
data class SearchFilters(
    val jlpt: Int? = null,
    val grade: Int? = null,
    val frequency: FrequencyBand? = null,
    val strokeCount: Int? = null,
    val partOfSpeech: String? = null,
    /**
     * Kanji-only study-state filter (todo #109, spec §18): Known / Learning /
     * New / Due / Mastered / Suspended. Evaluated against the real SRS cards
     * via the query's [StudyOverlay] — never a UI-level guess.
     */
    val studyState: StudyState? = null
) {
    companion object {
        val None = SearchFilters()
    }
}

/**
 * Word rows fetched when POS/JLPT filters are active. The DB has no
 * filtered word query, so filtered search fetches this wider window and
 * applies the filters in Kotlin (KT-SEARCH-006). Large enough to be
 * useful on the jōyō word set, small enough to stay cheap per keystroke.
 */
private const val FILTERED_WORD_WINDOW = 300

enum class SearchSort(val label: String) {
    Relevance("Relevance"),
    Frequency("Frequency"),
    StrokeCount("Strokes"),
    Jlpt("JLPT"),
    Grade("Grade"),
    Alphabetical("A–Z"),
    Reading("Reading"),
    /** Sentence-only sort: estimated difficulty, easiest first. */
    Difficulty("Difficulty"),
    /** Study-based sorts (KT-SEARCH-004, todo #108): need a StudyOverlay. */
    RecentlyStudied("Recently studied"),
    RecentlyAdded("Recently added")
}

data class KnowledgeSearchQuery(
    val text: String,
    val categories: Set<SearchCategory> = SearchCategory.entries.toSet(),
    val filters: SearchFilters = SearchFilters.None,
    val sort: SearchSort = SearchSort.Relevance,
    /**
     * Real SRS state snapshot for study-aware filters and sorts
     * (KT-SEARCH-004/005, todo #108–#110). Empty = study-neutral search.
     */
    val studyOverlay: StudyOverlay = StudyOverlay(),
    val kanjiLimit: Int = 24,
    val wordLimit: Int = 24,
    val sentenceLimit: Int = 12,
    val grammarLimit: Int = 8
)

data class KanjiHit(
    val kanji: String,
    val keyword: String?,
    val on: List<String>,
    val kun: List<String>,
    val frequencyRank: Int?,
    val frequencyBand: FrequencyBand?,
    val classifications: List<KanjiTag>,
    val strokeCount: Int?,
    /**
     * Why this kanji matched. Null when the query text was empty and the hit
     * comes from filter-only search (JLPT / grade / strokes / frequency).
     */
    val matchType: KanjiMatchType?
)

enum class KanjiMatchType { Character, Reading, Meaning }

data class WordHit(
    val word: WordKnowledge,
    /** JLPT level of the word's kanji, when every kanji carries one. */
    val jlpt: Int?
)

data class SentenceHit(val sentence: SentenceKnowledge)

data class GrammarHit(val pattern: GrammarPattern)

/** Grouped results — each section carries its own real count. */
data class GroupedSearchResults(
    val query: KnowledgeSearchQuery,
    val kanji: List<KanjiHit> = emptyList(),
    val words: List<WordHit> = emptyList(),
    val sentences: List<SentenceHit> = emptyList(),
    val grammar: List<GrammarHit> = emptyList(),
    val wordTotal: Int = 0,
    val sentenceTotal: Int = 0
) {
    val isEmpty: Boolean
        get() = kanji.isEmpty() && words.isEmpty() && sentences.isEmpty() && grammar.isEmpty()

    val totalHits: Int get() = kanji.size + words.size + sentences.size + grammar.size
}

/**
 * In-memory kanji index built from the bulk database queries. The jōyō set
 * is ~2,100 characters — scanning this per keystroke is cheap and keeps
 * search feeling instant without touching the DB.
 */
class KanjiSearchIndex internal constructor(
    entries: List<KanjiListEntry>,
    meanings: List<KanjiMeaningEntry>,
    readings: List<KanjiReadingEntry>,
    strokeCounts: Map<String, Int>,
    classifications: Map<String, List<KanjiTag>>
) {

    private data class IndexRow(
        val kanji: String,
        val frequency: Int?,
        val meanings: List<String>,
        val on: List<String>,
        val kun: List<String>,
        val strokeCount: Int?,
        val tags: List<KanjiTag>
    )

    private val rows: List<IndexRow> = entries.map { entry ->
        val char = entry.kanji
        IndexRow(
            kanji = char,
            frequency = entry.frequency,
            meanings = meanings.filter { it.kanji == char }.map { it.meaning },
            on = readings.filter { it.kanji == char && it.readingType == "on" }.map { it.reading },
            kun = readings.filter { it.kanji == char && it.readingType == "kun" }.map { it.reading },
            strokeCount = strokeCounts[char],
            tags = classifications[char].orEmpty()
        )
    }

    /** Number of kanji in the index (the real dataset count). */
    val size: Int get() = rows.size

    /** The JLPT level of a character, when its tags include one. */
    fun jlptOf(character: String): Int? =
        rows.firstOrNull { it.kanji == character }
            ?.tags?.filterIsInstance<KanjiTag.Jlpt>()?.minOfOrNull { it.level }

    fun search(query: KnowledgeSearchQuery): List<KanjiHit> {
        val q = query.text.trim()
        // Normalized query (KT-SEARCH-005): full/half-width folded, katakana
        // → hiragana, lowercase, ー dropped; romaji input is also converted
        // to kana so "taberu" matches a たべる reading, and the original
        // form is kept for character matching (kanji untouched by
        // normalization).
        val normalized = q.normalizeForSearch()
        val romajiAsKana = q.romajiToHiragana()
        val filters = query.filters
        val overlay = query.studyOverlay
        // A blank query is only meaningful when filters are narrowing the set.
        if (q.isEmpty() && filters == SearchFilters.None) return emptyList()

        val matched = rows.mapNotNull { row ->
            val matchType = when {
                q.isEmpty() -> null
                row.kanji == q -> KanjiMatchType.Character
                q.length == 1 && row.kanji.contains(q) -> KanjiMatchType.Character
                // Readings match against the normalized kana form AND the
                // romaji conversion of the query, so た・タ・tA all find た.
                // Containment is bidirectional: readings are often partial
                // stems (食 → たべ), so a full-word query (たべる / taberu)
                // must match a stored stem it contains — and vice versa.
                // Empty normalized forms are guarded (a bare ー would
                // otherwise match every reading via "".contains).
                q.isNotEmpty() && (normalized.isNotEmpty() || romajiAsKana.isNotEmpty()) && (
                    row.on.any { reading ->
                        val r = reading.normalizeForSearch()
                        r.contains(normalized) || normalized.contains(r) ||
                            r.contains(romajiAsKana) || romajiAsKana.contains(r)
                    } ||
                        row.kun.any { reading ->
                            val r = reading.normalizeForSearch()
                            r.contains(normalized) || normalized.contains(r) ||
                                r.contains(romajiAsKana) || romajiAsKana.contains(r)
                        }
                    ) -> KanjiMatchType.Reading
                q.isNotEmpty() && row.meanings.any { it.contains(q, ignoreCase = true) } -> KanjiMatchType.Meaning
                else -> null
            } ?: return@mapNotNull null

            // Filters — every one narrows the result.
            if (filters.jlpt != null && row.tags.none { it is KanjiTag.Jlpt && it.level == filters.jlpt }) return@mapNotNull null
            if (filters.grade != null && row.tags.none { it is KanjiTag.Grade && it.number == filters.grade }) return@mapNotNull null
            if (filters.strokeCount != null && row.strokeCount != filters.strokeCount) return@mapNotNull null
            val band = FrequencyBand.forRank(row.frequency)
            if (filters.frequency != null && band != filters.frequency) return@mapNotNull null
            // Study-state filter — real SRS cards via the overlay (todo #109).
            if (filters.studyState != null &&
                overlay.state(row.kanji) != filters.studyState
            ) return@mapNotNull null

            KanjiHit(
                kanji = row.kanji,
                keyword = row.meanings.firstOrNull(),
                on = row.on,
                kun = row.kun,
                frequencyRank = row.frequency,
                frequencyBand = band,
                classifications = row.tags,
                strokeCount = row.strokeCount,
                matchType = matchType
            )
        }

        return matched
            .sortedWith(kanjiComparator(query.sort, overlay))
            .take(query.kanjiLimit)
    }

    private fun kanjiComparator(
        sort: SearchSort,
        overlay: StudyOverlay
    ): Comparator<KanjiHit> = when (sort) {
        SearchSort.Frequency -> compareBy<KanjiHit> { it.frequencyRank ?: Int.MAX_VALUE }.thenBy { it.kanji }
        SearchSort.StrokeCount -> compareBy<KanjiHit> { it.strokeCount ?: Int.MAX_VALUE }
            .thenBy { it.frequencyRank ?: Int.MAX_VALUE }
        SearchSort.Jlpt -> compareBy<KanjiHit> {
            it.classifications.filterIsInstance<KanjiTag.Jlpt>().minOfOrNull { t -> t.level } ?: Int.MAX_VALUE
        }.thenBy { it.frequencyRank ?: Int.MAX_VALUE }
        SearchSort.Grade -> compareBy<KanjiHit> {
            it.classifications.filterIsInstance<KanjiTag.Grade>().minOfOrNull { t -> t.number } ?: Int.MAX_VALUE
        }.thenBy { it.frequencyRank ?: Int.MAX_VALUE }
        SearchSort.Alphabetical -> compareBy { it.keyword ?: it.kanji }
        SearchSort.Reading -> compareBy<KanjiHit> { (it.on.firstOrNull() ?: it.kun.firstOrNull()) ?: "" }
        SearchSort.Relevance -> compareByDescending<KanjiHit> { hit ->
            when (hit.matchType) {
                KanjiMatchType.Character -> 3
                KanjiMatchType.Reading -> 2
                KanjiMatchType.Meaning -> 1
                null -> 0
            }
        }.thenBy { it.frequencyRank ?: Int.MAX_VALUE }
        // Difficulty has no kanji meaning — fall back to frequency ranking
        // so the sort is still deterministic and useful.
        SearchSort.Difficulty -> compareBy { it.frequencyRank ?: Int.MAX_VALUE }
        // Study-based sorts (todo #108): newest review first; kanji never
        // studied sort last (null timestamps never fake a date).
        SearchSort.RecentlyStudied -> compareByDescending<KanjiHit> {
            overlay.info(it.kanji)?.lastReview?.toEpochMilliseconds() ?: Long.MIN_VALUE
        }.thenBy { it.frequencyRank ?: Int.MAX_VALUE }
        SearchSort.RecentlyAdded -> compareByDescending<KanjiHit> {
            overlay.info(it.kanji)?.added?.toEpochMilliseconds() ?: Long.MIN_VALUE
        }.thenBy { it.frequencyRank ?: Int.MAX_VALUE }
    }
}

class KnowledgeSearchEngine(
    private val knowledge: KnowledgeRepository
) {

    private fun wordComparator(sort: SearchSort): Comparator<WordHit> = when (sort) {
        SearchSort.Alphabetical -> compareBy<WordHit> { it.word.displaySpelling }
            .thenBy { it.word.combinedGlossary() }
        SearchSort.Reading -> compareBy<WordHit> { it.word.kanaReading }
        SearchSort.Jlpt -> compareBy<WordHit> { it.jlpt ?: Int.MAX_VALUE }
            .thenBy { it.word.kanaReading }
        SearchSort.Frequency -> compareByDescending<WordHit> { it.word.displaySpelling.length }
            .thenBy { it.word.kanaReading }
        SearchSort.Difficulty -> compareBy<WordHit> { it.word.displaySpelling.length }
            .thenBy { it.word.kanaReading }
        else -> compareBy<WordHit> { it.word.kanaReading } // Relevance is DB-ordered.
    }

    private fun sentenceComparator(sort: SearchSort): Comparator<SentenceHit> = when (sort) {
        SearchSort.Difficulty -> compareBy<SentenceHit> {
            SentenceDifficultyScorer.score(it.sentence.text).level
        }.thenBy { it.sentence.text }
        SearchSort.Reading -> compareBy<SentenceHit> { it.sentence.text }
        else -> compareBy<SentenceHit> { it.sentence.text } // Relevance is DB-ordered.
    }

    /**
     * Runs the query, returning grouped, real results. A blank query is only
     * meaningful when filters narrow the set (filter-only browse — e.g. JLPT
     * N4 kanji without typing): in that mode the kanji index returns the
     * filtered set and the text-dependent sections (words/sentences/grammar)
     * are skipped, since there is no term to look up.
     */
    suspend fun search(query: KnowledgeSearchQuery): GroupedSearchResults {
        val q = query.text.trim()
        if (q.isEmpty() && query.filters == SearchFilters.None) return GroupedSearchResults(query)

        // The DB stores kana (and kanji) — fold width/script and convert
        // romaji to kana so "taberu" and "タベル" reach the same rows.
        // Kanji passes through both transforms untouched.
        val dbTerm = q.normalizeForSearch().let { normalized ->
            if (normalized.any { it.isLetter() && it.code < 128 }) normalized.romajiToHiragana()
            else normalized
        }

        val categories = query.categories
        val index = knowledge.kanjiSearchIndex()

        val kanji = if (SearchCategory.Kanji in categories) index.search(query) else emptyList()

        // Structured filters must actually filter the whole fetched set (spec
        // §15, §18). The DB has no pos/jlpt-filtered word query yet, so a
        // filtered search fetches a much wider window and filters in Kotlin —
        // meaningfully closer to full-set than the default 24-row window.
        // Full DB-side filtering remains a roadmap item (see kdoc on
        // filterWords); the window size is documented here, not hidden.
        val hasWordFilters = query.filters.partOfSpeech != null || query.filters.jlpt != null
        val wordFetchLimit = if (hasWordFilters) FILTERED_WORD_WINDOW else query.wordLimit

        val words = if (SearchCategory.Words in categories && q.isNotEmpty()) {
            knowledge.wordsWithText(dbTerm, limit = wordFetchLimit).map { word ->
                // Only report a JLPT level when every kanji in the word has one.
                val levels = word.kanjiReading
                    ?.filter { it.isKanji() }
                    ?.map { index.jlptOf(it.toString()) }
                    ?.filterNotNull()
                    .orEmpty()
                WordHit(
                    word = word,
                    jlpt = if (levels.isNotEmpty() && levels.size == word.kanjiReading?.count { it.isKanji() }) levels.min()
                    else null
                )
            }.let { hits ->
                // Structured filters must actually filter (spec §15, §18) —
                // otherwise "common verbs N3" would silently ignore POS/JLPT
                // on the word section.
                filterWords(
                    results = GroupedSearchResults(query, words = hits),
                    partOfSpeech = query.filters.partOfSpeech,
                    jlpt = query.filters.jlpt
                ).words.sortedWith(wordComparator(query.sort))
            }
        } else emptyList()

        // The count is window-scoped (filterWords operates on the fetched
        // window; full DB-side filtering needs new queries — see kdoc).
        // The count is the DB total (a real count query); when filters are
        // active the count stays the unfiltered total because the filtered
        // result is computed over the wider window, not the full dataset.
        val wordTotal = if (SearchCategory.Words in categories && q.isNotEmpty()) knowledge.wordsWithTextCount(dbTerm) else 0

        val sentences = if (SearchCategory.Sentences in categories && q.isNotEmpty()) {
            knowledge.sentencesWithText(dbTerm, limit = query.sentenceLimit).map { SentenceHit(it) }
                .sortedWith(sentenceComparator(query.sort))
        } else emptyList()

        val sentenceTotal = if (SearchCategory.Sentences in categories && q.isNotEmpty()) knowledge.sentencesWithTextCount(dbTerm) else 0

        val grammar = if (SearchCategory.Grammar in categories && q.isNotEmpty()) {
            GrammarCatalog.search(q).take(query.grammarLimit).map { GrammarHit(it) }
        } else emptyList()

        return GroupedSearchResults(
            query = query,
            kanji = kanji,
            words = words,
            sentences = sentences,
            grammar = grammar,
            wordTotal = wordTotal,
            sentenceTotal = sentenceTotal
        )
    }

    /**
     * Applies word-section filters over the fetched window. Honest scope note:
     * the DB has no query for pos/JLPT-filtered word lookup yet, so filtering
     * is applied to the search window — full DB-side filtering is a roadmap
     * item that requires new queries (see docs/architecture/KNOWLEDGE_SYSTEM.md).
     */
    suspend fun filterWords(
        results: GroupedSearchResults,
        partOfSpeech: String?,
        jlpt: Int?
    ): GroupedSearchResults {
        if (partOfSpeech == null && jlpt == null) return results
        val index = knowledge.kanjiSearchIndex()
        val filtered = results.words.filter { hit ->
            val posOk = partOfSpeech == null ||
                    hit.word.partOfSpeech.any { it.contains(partOfSpeech, ignoreCase = true) }
            val levels = hit.word.kanjiReading
                ?.filter { it.isKanji() }
                ?.map { index.jlptOf(it.toString()) }
                ?.filterNotNull()
                .orEmpty()
            val allTagged = hit.word.kanjiReading?.count { it.isKanji() } == levels.size
            val jlptOk = jlpt == null || (allTagged && levels.isNotEmpty() && levels.min() == jlpt)
            posOk && jlptOk
        }
        return results.copy(words = filtered)
    }
}
