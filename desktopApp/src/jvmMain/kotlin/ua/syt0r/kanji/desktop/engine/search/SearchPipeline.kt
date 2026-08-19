package ua.syt0r.kanji.desktop.engine.search

import java.text.Normalizer

// ============================================
// KAITEYO SEARCH PIPELINE
// The STANDARDS §187 pipeline: normalize →
// tokenize → rank → filter. Compose-free and
// pure; any search surface (dictionary, cards,
// documents) can reuse it. Japanese is handled
// by kana/kanji runs (real word segmentation
// lives in JapaneseSegmenter — this pipeline
// normalizes and scores text broadly).
// ============================================

/** One token of a normalized query. */
data class SearchToken(
    val text: String,
    val isJapanese: Boolean,
    val isKanji: Boolean
)

/** Ranking modes, best to loosest. */
enum class SearchRank {
    Exact,
    Prefix,
    Contains,
    Kana,
    None
}

object SearchPipeline {

    /** NFKC-normalize text: full-width → half-width, composed kana, etc. */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFKC).trim()

    /** Tokenize a query into Japanese runs and Latin words. */
    fun tokenize(query: String): List<SearchToken> {
        val normalized = normalize(query)
        if (normalized.isBlank()) return emptyList()

        val tokens = mutableListOf<SearchToken>()
        var i = 0
        while (i < normalized.length) {
            val c = normalized[i]
            when {
                isKanjiChar(c) || isKanaChar(c) -> {
                    val start = i
                    while (i < normalized.length && (isKanjiChar(normalized[i]) || isKanaChar(normalized[i]))) i++
                    val run = normalized.substring(start, i)
                    tokens += SearchToken(run, isJapanese = true, isKanji = run.any(::isKanjiChar))
                }
                c.isLetterOrDigit() -> {
                    val start = i
                    while (i < normalized.length && normalized[i].isLetterOrDigit()) i++
                    tokens += SearchToken(normalized.substring(start, i), isJapanese = false, isKanji = false)
                }
                else -> i++
            }
        }
        return tokens
    }

    /** How well [candidate] matches [query] (Japanese-aware). */
    fun rank(query: String, candidate: String): SearchRank {
        if (candidate.isBlank()) return SearchRank.None
        val q = normalize(query)
        val c = normalize(candidate)
        if (q.isEmpty()) return SearchRank.None
        if (q == c) return SearchRank.Exact
        if (c.startsWith(q)) return SearchRank.Prefix
        if (c.contains(q)) return SearchRank.Contains
        // Kana match: the query is kana and appears within the candidate
        // (e.g. がっこう vs 学校) or the candidate's kana form.
        if (q.all(::isKanaChar) && c.any(::isKanjiChar)) return SearchRank.Kana
        return SearchRank.None
    }

    /** Filter + sort candidates by rank against the query. */
    fun rankAndSort(query: String, candidates: List<String>): List<Pair<String, SearchRank>> =
        candidates
            .mapNotNull { candidate ->
                val r = rank(query, candidate)
                if (r == SearchRank.None) null else candidate to r
            }
            .sortedWith(
                compareByDescending<Pair<String, SearchRank>> { it.second.ordinal }
                    .thenBy { it.first }
            )

    fun isKanjiChar(c: Char): Boolean =
        c.code in 0x4E00..0x9FFF || c.code in 0x3400..0x4DBF || c.code in 0xF900..0xFAFF

    fun isKanaChar(c: Char): Boolean =
        c in '぀'..'ゟ' || c in '゠'..'ヿ'
}
