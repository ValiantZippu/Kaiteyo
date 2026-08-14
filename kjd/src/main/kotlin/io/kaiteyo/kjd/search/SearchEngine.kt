package io.kaiteyo.kjd.search

import io.kaiteyo.kjd.model.CanonicalDatabase
import io.kaiteyo.kjd.model.EntityId
import io.kaiteyo.kjd.model.EntityType
import io.kaiteyo.kjd.normalize.JapaneseNormalizer

/**
 * Stable search result abstraction. Consumers (UI, CLI, API) receive these,
 * never raw database rows.
 */
data class SearchResult(
    val entityType: EntityType,
    val entityId: EntityId,
    val displayText: String,
    val reading: String? = null,
    val gloss: String? = null,
    val relevance: Float,
    /** Searchable kana key used for ranking tie-breaks. */
    val kanaKey: String = ""
)

/**
 * A fast in-memory search index built once from the canonical database.
 *
 * Japanese search is normalized before indexing: kanji/kana identity, kana
 * equivalence (hiragana↔katakana), Unicode NFC and punctuation stripping.
 * All lookups scan index structures (prefix maps), never the whole table, so
 * autocomplete stays fast on large datasets.
 */
class SearchIndex private constructor(
    private val kanjiByKey: Map<String, List<SearchEntry>>,
    private val vocabByKey: Map<String, List<SearchEntry>>,
    private val entries: List<SearchEntry>
) {

    private data class SearchEntry(
        val result: SearchResult,
        val keys: List<String>,
        val isExact: Boolean
    )

    /**
     * Search both kanji and vocabulary. [query] may be kanji, kana, reading
     * or Latin (meaning) text.
     */
    fun search(query: String, limit: Int = 50): List<SearchResult> {
        val normalized = JapaneseNormalizer.searchKey(query)
        if (normalized.isBlank()) return emptyList()

        val kanaNormalized = JapaneseNormalizer.katakanaToHiragana(normalized)
        val hasKanji = JapaneseNormalizer.hasKanji(query)

        val scored = mutableListOf<SearchResult>()

        // Kanji index.
        kanjiByKey[kanaNormalized]?.forEach { entry -> scored.add(entry.result) }
        if (hasKanji) {
            kanjiByKey[normalized]?.forEach { entry -> scored.add(entry.result) }
        }

        // Vocabulary index.
        vocabByKey[kanaNormalized]?.forEach { entry -> scored.add(entry.result) }
        if (hasKanji) {
            vocabByKey[normalized]?.forEach { entry -> scored.add(entry.result) }
        }

        // Prefix fallback for autocomplete: match any index key starting with
        // the query (only when the exact map miss is empty, to stay cheap).
        if (scored.isEmpty()) {
            val prefix = kanaNormalized
            for ((key, list) in vocabByKey) {
                if (key.startsWith(prefix)) {
                    list.take(10).forEach { scored.add(it.result) }
                    if (scored.size >= limit) break
                }
            }
            for ((key, list) in kanjiByKey) {
                if (key.startsWith(prefix)) {
                    list.take(10).forEach { scored.add(it.result) }
                    if (scored.size >= limit) break
                }
            }
        }

        return scored.distinctBy { it.entityId.value }
            .sortedByDescending { it.relevance }
            .take(limit)
    }

    /** Exact-match lookup (single entity) for kanji or vocabulary. */
    fun lookupExact(query: String): SearchResult? {
        val key = JapaneseNormalizer.searchKey(query)
        if (key.isEmpty()) return null
        return (kanjiByKey[key] ?: vocabByKey[key])?.firstOrNull()?.result
    }

    companion object {

        /** Build the index from a canonical database snapshot. */
        fun build(database: CanonicalDatabase): SearchIndex {
            val kanjiMap = HashMap<String, MutableList<SearchEntry>>()
            val vocabMap = HashMap<String, MutableList<SearchEntry>>()
            val all = mutableListOf<SearchEntry>()

            for (kanji in database.kanji) {
                val literal = kanji.character.literal
                val readingText = (kanji.onReadings + kanji.kunReadings)
                    .joinToString(" ") { it.value }
                val meaning = kanji.meanings.firstOrNull()?.value ?: ""
                val result = SearchResult(
                    entityType = EntityType.Kanji,
                    entityId = kanji.id,
                    displayText = literal,
                    reading = readingText.ifBlank { null },
                    gloss = meaning.ifBlank { null },
                    relevance = 1f,
                    kanaKey = JapaneseNormalizer.katakanaToHiragana(literal)
                )
                val keys = buildList {
                    add(JapaneseNormalizer.searchKey(literal))
                    add(JapaneseNormalizer.katakanaToHiragana(literal))
                    kanji.onReadings.forEach { add(JapaneseNormalizer.searchKey(it.value)) }
                    kanji.kunReadings.forEach { add(JapaneseNormalizer.searchKey(it.value)) }
                    if (meaning.isNotBlank()) add(JapaneseNormalizer.searchKey(meaning))
                }.distinct()
                val entry = SearchEntry(result, keys, isExact = true)
                keys.forEach { key -> kanjiMap.getOrPut(key) { mutableListOf() }.add(entry) }
                all.add(entry)
            }

            for (vocab in database.vocabulary) {
                val reading = vocab.readings.firstOrNull()?.value ?: ""
                val gloss = vocab.senses.firstOrNull()?.glosses?.firstOrNull()?.value
                val result = SearchResult(
                    entityType = EntityType.Vocabulary,
                    entityId = vocab.id,
                    displayText = vocab.expression,
                    reading = reading.ifBlank { null },
                    gloss = gloss,
                    relevance = if (vocab.frequency.isNotEmpty()) {
                        (1f / (vocab.frequency.minOf { it.value } + 1)).coerceIn(0.2f, 1f)
                    } else 0.5f,
                    kanaKey = JapaneseNormalizer.katakanaToHiragana(vocab.expression)
                )
                val keys = buildList {
                    add(JapaneseNormalizer.searchKey(vocab.expression))
                    add(JapaneseNormalizer.katakanaToHiragana(vocab.expression))
                    vocab.readings.forEach { add(JapaneseNormalizer.searchKey(it.value)) }
                    gloss?.let { add(JapaneseNormalizer.searchKey(it)) }
                }.distinct()
                val entry = SearchEntry(result, keys, isExact = true)
                keys.forEach { key -> vocabMap.getOrPut(key) { mutableListOf() }.add(entry) }
                all.add(entry)
            }

            return SearchIndex(kanjiMap, vocabMap, all)
        }
    }
}

/** Convenience: normalize a query for display in result lists. */
fun SearchResult.preview(): String = buildString {
    append(displayText)
    if (reading != null) append(" ($reading)")
    if (gloss != null) append(" — $gloss")
}
