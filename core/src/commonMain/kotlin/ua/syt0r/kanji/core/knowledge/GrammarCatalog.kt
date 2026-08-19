package ua.syt0r.kanji.core.knowledge

// ============================================================
// GRAMMAR CATALOG
// ------------------------------------------------------------
// A curated starter set of common Japanese grammar patterns.
//
// HONESTY NOTE: this catalog ships with the app as reference data
// for sentence highlighting and grammar search. It is NOT an
// authoritative corpus and never claims to be. Matching is
// deterministic substring matching against the kana forms, so a
// "match" means "this pattern's kana form appears in the text" —
// it is a highlight hint, not a morphological parse. Extending
// with a licensed grammar dataset is a documented roadmap item
// (see docs/architecture/KNOWLEDGE_SYSTEM.md).
// ============================================================

/** The built-in starter grammar patterns. */
object GrammarCatalog {

    val patterns: List<GrammarPattern> = listOf(
        // ── Particles (deterministic kana forms) ────────────────────────
        particle("wa", "は", "topic marker", "marks the sentence topic", jlpt = 5, keywords = listOf("topic", "as for")),
        particle("ga", "が", "subject marker", "marks the grammatical subject", jlpt = 5, keywords = listOf("subject")),
        particle("wo", "を", "object marker", "marks the direct object", jlpt = 5, keywords = listOf("object")),
        particle("ni", "に", "location / time / indirect object", "marks destination, time, or recipient", jlpt = 5, keywords = listOf("in", "at", "to")),
        particle("de", "で", "means / location of action", "marks the place or tool of an action", jlpt = 5, keywords = listOf("by", "with", "at")),
        particle("to", "と", "with / and / quotation", "companion, listing, or quoted speech", jlpt = 5, keywords = listOf("with", "and")),
        particle("mo", "も", "also / too", "adds an item to a set", jlpt = 5, keywords = listOf("also", "too")),
        particle("he", "へ", "direction", "marks direction toward a place", jlpt = 5, keywords = listOf("to", "toward")),
        particle("kara", "から", "from / because", "origin, starting point, or reason", jlpt = 5, keywords = listOf("from", "because")),
        particle("made", "まで", "until / up to", "endpoint or limit", jlpt = 5, keywords = listOf("until", "up to")),
        particle("no", "の", "possessive / nominalizer", "possession or noun-linking", jlpt = 5, keywords = listOf("of", "possessive")),

        // ── Common patterns ─────────────────────────────────────────────
        pattern("te-miru", "〜てみる", "try doing", "attempt an action", jlpt = 4, keywords = listOf("try")),
        pattern("te-shimau", "〜てしまう", "end up doing", "completion or regret", jlpt = 4, keywords = listOf("end up", "regrettably")),
        pattern("te-iku", "〜ていく", "go on doing / gradually", "action continuing away or over time", jlpt = 4, keywords = listOf("go on", "gradually")),
        pattern("te-kuru", "〜てくる", "come to do / become", "action toward speaker or change over time", jlpt = 4, keywords = listOf("come to", "become")),
        pattern("tai", "〜たい", "want to do", "desire", jlpt = 5, keywords = listOf("want")),
        pattern("tagaru", "〜たがる", "want to do (3rd person)", "observed desire of others", jlpt = 4, keywords = listOf("want", "seems to want")),
        pattern("nagara", "〜ながら", "while doing", "simultaneous action", jlpt = 4, keywords = listOf("while")),
        pattern("node", "〜ので", "because (neutral)", "reason, softer than から", jlpt = 4, keywords = listOf("because", "since")),
        pattern("noni", "〜のに", "even though", "contrast / regret", jlpt = 3, keywords = listOf("even though", "although")),
        pattern("temo", "〜ても", "even if / even though", "concession", jlpt = 4, keywords = listOf("even if")),
        pattern("ba", "〜ば", "if (conditional)", "conditional form", jlpt = 4, keywords = listOf("if", "conditional")),
        pattern("nara", "〜なら", "if / given that", "condition on stated premise", jlpt = 4, keywords = listOf("if")),
        pattern("souda", "〜そう", "seems / looks like", "appearance or hearsay", jlpt = 4, keywords = listOf("seems", "looks like")),
        pattern("youda", "〜よう", "seems like / like", "resemblance or hearsay", jlpt = 4, keywords = listOf("seems", "like")),
        pattern("sugiru", "〜すぎる", "too much", "excess", jlpt = 4, keywords = listOf("too much", "excessive")),
        pattern("yasui", "〜やすい", "easy to do", "inclination", jlpt = 4, keywords = listOf("easy to")),
        pattern("nikui", "〜にくい", "hard to do", "difficulty", jlpt = 4, keywords = listOf("hard to", "difficult to")),
        pattern("koto-ga-aru", "〜ことがある", "have done before", "past experience or occasional occurrence", jlpt = 4, keywords = listOf("have done", "occasionally")),
        pattern("koto-ga-dekiru", "〜ことができる", "can do", "ability or possibility", jlpt = 4, keywords = listOf("can", "able to")),
        pattern("tsumori", "〜つもり", "intend to", "intention", jlpt = 4, keywords = listOf("intend", "plan to")),
        pattern("nakereba-naranai", "〜なければならない", "must do", "obligation", jlpt = 4, keywords = listOf("must", "have to")),
        pattern("to-ii", "〜といい", "it would be good if", "hope / suggestion", jlpt = 4, keywords = listOf("hope", "should"))
    )

    private fun particle(
        id: String, form: String, meaning: String, formation: String, jlpt: Int, keywords: List<String>
    ) = GrammarPattern(
        id = "particle-$id", pattern = form, meaning = meaning, formation = formation, jlpt = jlpt, keywords = keywords
    )

    private fun pattern(
        id: String, form: String, meaning: String, formation: String, jlpt: Int, keywords: List<String>
    ) = GrammarPattern(
        id = "grammar-$id", pattern = form, meaning = meaning, formation = formation, jlpt = jlpt, keywords = keywords
    )

    fun byId(id: String): GrammarPattern? = patterns.firstOrNull { it.id == id }

    /** All patterns (the browse surface's real catalog). */
    fun all(): List<GrammarPattern> = patterns
}

/**
 * Finds grammar patterns inside [text]. Matching is deterministic substring
 * matching over the pattern's kana forms — a hint for highlighting, never a
 * morphological claim. Matches are non-overlapping: once a span is claimed by
 * the longest matching pattern, shorter patterns inside it are skipped.
 */
fun GrammarCatalog.findIn(text: String): List<GrammarMatch> {
    if (text.isBlank()) return emptyList()

    // Longest form first so 〜てしまう wins over 〜て.
    val candidates = patterns
        .flatMap { pattern ->
            pattern.kanaForms.map { form -> pattern to form }
        }
        .sortedByDescending { (_, form) -> form.length }

    val matches = mutableListOf<GrammarMatch>()
    val claimed = mutableListOf<IntRange>()

    for ((pattern, form) in candidates) {
        if (form.isEmpty()) continue
        var searchFrom = 0
        while (true) {
            val index = text.indexOf(form, searchFrom)
            if (index < 0) break
            val range = index until (index + form.length)
            val overlaps = claimed.any { it.first < range.last && range.first < it.last }
            if (!overlaps) {
                matches.add(
                    GrammarMatch(
                        patternId = pattern.id,
                        matchedText = form,
                        startIndex = index,
                        endIndex = index + form.length
                    )
                )
                claimed.add(range)
            }
            searchFrom = index + 1
        }
    }

    // Stable ordering: left-to-right by position, then longest match first.
    return matches.sortedWith(
        compareBy({ it.startIndex }, { -(it.endIndex - it.startIndex) })
    )
}

/**
 * Grammar search: a pattern matches [query] when its kana form or one of its
 * keywords contains the query (case-insensitive), or the query is a JLPT
 * level string like "N4" / "n4" matching the pattern's level.
 */
fun GrammarCatalog.search(query: String): List<GrammarPattern> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()

    val jlptFromQuery = JLPT_QUERY.find(q)?.groupValues?.get(1)?.toIntOrNull()

    return patterns.filter { pattern ->
        val jlptMatch = jlptFromQuery != null && pattern.jlpt == jlptFromQuery
        val formMatch = pattern.kanaForms.any { it.lowercase().contains(q) }
        val keywordMatch = pattern.keywords.any { it.lowercase().contains(q) }
        val meaningMatch = pattern.meaning.lowercase().contains(q)
        jlptMatch || formMatch || keywordMatch || meaningMatch
    }
}

private val JLPT_QUERY = Regex("""n([1-5])""", RegexOption.IGNORE_CASE)
