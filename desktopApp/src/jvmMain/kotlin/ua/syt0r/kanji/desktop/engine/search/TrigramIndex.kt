package ua.syt0r.kanji.desktop.engine.search

// ============================================
// KAITEYO TRIGRAM INDEX
// Fast substring search over an arbitrary
// string corpus. Every document is indexed by
// its trigrams; a query finds candidate docs
// that share at least one trigram, then
// verifies with a real substring check (a
// trigram hit is necessary, not sufficient).
// Used by document/subtitle search where a
// full scan is too slow.
// ============================================

class TrigramIndex {

    private val trigrams = mutableMapOf<String, MutableSet<Int>>()
    private val documents = mutableListOf<String>()
    private var version = 0

    val size: Int get() = documents.size

    /** Add a document and return its id (stable index). */
    fun add(text: String): Int {
        val id = documents.size
        documents.add(text)
        trigramsOf(text).forEach { gram ->
            trigrams.getOrPut(gram) { mutableSetOf() }.add(id)
        }
        version++
        return id
    }

    fun addAll(texts: List<String>): IntRange {
        val start = documents.size
        texts.forEach { add(it) }
        return start until documents.size
    }

    fun clear() {
        trigrams.clear()
        documents.clear()
        version++
    }

    /** Documents containing the query (substring), with match counts sorted desc. */
    fun search(query: String, limit: Int = 100): List<Pair<String, Int>> {
        if (query.isBlank()) return emptyList()
        val grams = trigramsOf(query)
        if (grams.isEmpty()) {
            // Query shorter than 3 chars (or non-indexable): fall back to scan.
            return documents.mapIndexedNotNull { id, doc ->
                val count = doc.countOccurrences(query)
                if (count > 0) id to count else null
            }.sortedByDescending { it.second }.take(limit)
                .map { (id, count) -> documents[id] to count }
        }

        val candidates = mutableMapOf<Int, Int>()
        grams.forEach { gram ->
            trigrams[gram]?.forEach { id ->
                candidates[id] = (candidates[id] ?: 0) + 1
            }
        }
        return candidates.entries
            .mapNotNull { (id, hits) ->
                val count = documents[id].countOccurrences(query)
                if (count > 0) (id to count) to hits else null
            }
            .sortedWith(compareByDescending<Pair<Pair<Int, Int>, Int>> { it.first.second }.thenByDescending { it.second })
            .take(limit)
            .map { (entry, _) -> documents[entry.first] to entry.second }
    }

    private fun String.countOccurrences(needle: String): Int {
        var count = 0
        var from = 0
        while (true) {
            val idx = indexOf(needle, from)
            if (idx < 0) break
            count++
            from = idx + 1
        }
        return count
    }

    companion object {
        private const val GRAM_SIZE = 3

        /** All trigrams of a text, deduplicated (non-letter chars are separators). */
        fun trigramsOf(text: String): Set<String> {
            val result = linkedSetOf<String>()
            var run = StringBuilder()
            fun flush() {
                val s = run.toString()
                if (s.length >= GRAM_SIZE) {
                    for (i in 0..s.length - GRAM_SIZE) {
                        result.add(s.substring(i, i + GRAM_SIZE))
                    }
                }
                run = StringBuilder()
            }
            text.forEach { c ->
                if (c.isLetterOrDigit() || SearchPipeline.isKanjiChar(c) || SearchPipeline.isKanaChar(c)) {
                    run.append(c)
                } else {
                    flush()
                }
            }
            flush()
            return result
        }
    }
}
