package ua.syt0r.kanji.desktop.engine.grammar

// ============================================
// KAITEYO GRAMMAR — INDEX
// A pure index over GrammarEntries: exact
// pattern lookup, meaning search, and sentence
// matching (patterns present in a sentence).
// Dataset-agnostic — feed it any adopted
// dataset plus the curated reference facts.
// ============================================

class GrammarIndex(
    private val entries: List<GrammarEntry>,
    private val conjugationEdges: List<GrammarConjugationEdge> = emptyList()
) {

    /** Canonical pattern + every form pattern → its entry. */
    private val byPattern: Map<String, GrammarEntry> = buildMap {
        this@GrammarIndex.entries.forEach { entry ->
            put(entry.pattern, entry)
            entry.forms.forEach { form -> putIfAbsent(form.pattern, entry) }
        }
    }

    val size: Int get() = entries.size

    /** The entry for an exact pattern (canonical or one of its forms). */
    fun lookup(pattern: String): GrammarEntry? {
        if (pattern.isBlank()) return null
        return byPattern[pattern]
    }

    /** Entries whose meaning or tags contain the query (substring, case-insensitive). */
    fun search(query: String): List<GrammarEntry> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        val needle = q.lowercase()
        return entries
            .filter { entry ->
                entry.pattern.contains(needle) ||
                    entry.meaning.lowercase().contains(needle) ||
                    entry.tags.any { it.lowercase().contains(needle) }
            }
            .sortedWith(compareByDescending<GrammarEntry> { it.pattern.startsWith(q) }.thenBy { it.pattern })
    }

    /**
     * Grammar patterns present in a sentence. Longest patterns first so
     * 〜なければならない wins over 〜ない when both match.
     */
    fun match(sentence: String): List<GrammarEntry> {
        if (sentence.isBlank()) return emptyList()
        return entries
            .filter { entry ->
                entry.pattern.isNotBlank() && sentence.contains(entry.pattern)
            }
            .sortedByDescending { it.pattern.length }
    }

    /** Conjugation edges from a pattern, resolved to real entries when present. */
    fun conjugations(pattern: String): List<Pair<GrammarConjugationEdge, GrammarEntry?>> =
        conjugationEdges
            .filter { it.from == pattern }
            .map { edge -> edge to lookup(edge.to) }
}
