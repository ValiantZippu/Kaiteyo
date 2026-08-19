package ua.syt0r.kanji.core.knowledge

import ua.syt0r.kanji.core.japanese.isKana
import ua.syt0r.kanji.core.japanese.isKatakana
import ua.syt0r.kanji.core.japanese.isKanji

// ============================================================
// JAPANESE TEXT NORMALIZATION (KT-SEARCH-005, spec §15, §136)
// ------------------------------------------------------------
// Search must treat 食べる, たべる, タベル, TABERU, tAbErU and
// ｔａｂｅｒｕ as the same intent. This module is pure and
// deterministic — the same input always yields the same output —
// so it can be unit-tested exhaustively and reused by any surface
// (search box, filters, wildcard matching, command palette).
//
// The normalizer does NOT guess. It folds what is unambiguous
// (width, case, kana script) and leaves kanji untouched. Romaji
// conversion is a documented kana-table mapping — good enough for
// query intent, never presented as authoritative pronunciation.
// ============================================================

/**
 * Folds a character to its search form:
 *  - full-width ASCII/latin/digits → half-width (ｓ → s, ４ → 4)
 *  - half-width katakana (ｶﾞ) → full-width katakana (ガ)
 *  - katakana → hiragana (search script never matters)
 *  - ASCII letters → lowercase
 *  - prolonged-sound mark ー dropped (長音 is a reading hint, not a
 *    different morpheme for matching purposes)
 * Kanji, punctuation and everything else pass through unchanged.
 */
fun Char.toSearchForm(): Char {
    val code = code
    // Full-width ASCII (FF01..FF5E) → half-width ASCII (21..7E).
    if (code in 0xFF01..0xFF5E) return (code - 0xFEE0).toChar()
    // Half-width katakana (FF66..FF9F) → full-width katakana.
    if (code in 0xFF66..0xFF9F) return (code + 0xCF).toChar()
    // Full-width katakana → hiragana (script-folding for matching).
    if (isKatakana()) return (code - 0x60).toChar()
    // ASCII case folding.
    if (code in 'A'.code..'Z'.code) return (code + 0x20).toChar()
    return this
}

/** Normalizes a whole string for matching (see [Char.toSearchForm]). */
fun String.normalizeForSearch(): String =
    buildString(length) {
        for (c in this@normalizeForSearch) {
            if (c == 'ー') continue
            append(c.toSearchForm())
        }
    }

/**
 * Kana → romaji using the bundled reading table (nihon-shiki base,
 * with the same alternatives the app already ships: し → "shi").
 * Used to make kana queries match romaji input (taberu → たべる).
 * Small kana fold to their base reading (っ → tsu) — honest limit:
 * geminate consonants aren't expanded (sakka → さっか, not さkか).
 */
fun String.kanaToRomaji(): String {
    val out = StringBuilder(length * 2)
    for (c in this) {
        if (c.isKanji()) {
            // Kanji has no reading here — keep the character so a romaji
            // search for a mixed string still has something to match.
            out.append(c)
            continue
        }
        // ー (prolonged mark) is kana-shaped but has no syllable reading —
        // keep it (or drop it like normalization does) rather than crashing
        // a map lookup that has no entry for it.
        if (c == 'ー') {
            out.append(c)
            continue
        }
        if (!c.isKana()) {
            out.append(c)
            continue
        }
        val reading = ua.syt0r.kanji.core.japanese.getKanaReading(c)
        out.append(reading.nihonShiki)
    }
    return out.toString()
}

/**
 * Romaji → hiragana. A best-effort kana-table conversion for query
 * normalization (ta → た, taberu → たべる, shi → し, kya → きゃ).
 *
 * Honest limits (documented, tested):
 *  - geminate consonants ("kka") collapse to っ + base kana ("か"),
 *    matching how さっか is written, not pronounced;
 *  - long vowels written as doubled letters ("too") are NOT inferred —
 *    input must use the actual kana to encode length;
 *  - unknown ASCII stays as-is so mixed queries degrade gracefully.
 */
fun String.romajiToHiragana(): String {
    val out = StringBuilder(length)
    var i = 0
    while (i < length) {
        // 3-letter digraphs first: y-columns (kya/kyu/kyo … ryo), the
        // sh-/ch-/ts- columns (shi/sha/shu/sho, chi/cha/chu/cho,
        // tsu/tsa/tse/tso) and xtu.
        var consumed = tryMatch(this, i, 3) { three ->
            when (three) {
                "kya" -> "きゃ"; "kyu" -> "きゅ"; "kyo" -> "きょ"
                "gya" -> "ぎゃ"; "gyu" -> "ぎゅ"; "gyo" -> "ぎょ"
                "nya" -> "にゃ"; "nyu" -> "にゅ"; "nyo" -> "にょ"
                "hya" -> "ひゃ"; "hyu" -> "ひゅ"; "hyo" -> "ひょ"
                "bya" -> "びゃ"; "byu" -> "びゅ"; "byo" -> "びょ"
                "pya" -> "ぴゃ"; "pyu" -> "ぴゅ"; "pyo" -> "ぴょ"
                "rya" -> "りゃ"; "ryu" -> "りゅ"; "ryo" -> "りょ"
                "mya" -> "みゃ"; "myu" -> "みゅ"; "myo" -> "みょ"
                "shi" -> "し"; "sha" -> "しゃ"; "shu" -> "しゅ"; "sho" -> "しょ"
                "chi" -> "ち"; "cha" -> "ちゃ"; "chu" -> "ちゅ"; "cho" -> "ちょ"
                "tsu" -> "つ"; "tsa" -> "つぁ"; "tse" -> "つぇ"; "tso" -> "つぉ"
                "xtu" -> "っ"
                else -> null
            }
        }
        if (consumed != null) {
            out.append(consumed)
            i += 3
            continue
        }

        // 2-letter vowel columns (ka/ki/ku …, si/zi/ti/tu/fu variants, the
        // small-vowel x-columns, wa/wo).
        consumed = tryMatch(this, i, 2) { two ->
            when (two) {
                "ka" -> "か"; "ki" -> "き"; "ku" -> "く"; "ke" -> "け"; "ko" -> "こ"
                "ga" -> "が"; "gi" -> "ぎ"; "gu" -> "ぐ"; "ge" -> "げ"; "go" -> "ご"
                "sa" -> "さ"; "si" -> "し"; "su" -> "す"; "se" -> "せ"; "so" -> "そ"
                "za" -> "ざ"; "zi" -> "じ"; "ji" -> "じ"; "zu" -> "ず"; "ze" -> "ぜ"; "zo" -> "ぞ"
                "ta" -> "た"; "ti" -> "ち"; "tu" -> "つ"; "te" -> "て"; "to" -> "と"
                "da" -> "だ"; "di" -> "ぢ"; "du" -> "づ"; "de" -> "で"; "do" -> "ど"
                "na" -> "な"; "ni" -> "に"; "nu" -> "ぬ"; "ne" -> "ね"; "no" -> "の"
                "ha" -> "は"; "hi" -> "ひ"; "fu" -> "ふ"; "hu" -> "ふ"; "he" -> "へ"; "ho" -> "ほ"
                "ba" -> "ば"; "bi" -> "び"; "bu" -> "ぶ"; "be" -> "べ"; "bo" -> "ぼ"
                "pa" -> "ぱ"; "pi" -> "ぴ"; "pu" -> "ぷ"; "pe" -> "ぺ"; "po" -> "ぽ"
                "ma" -> "ま"; "mi" -> "み"; "mu" -> "む"; "me" -> "め"; "mo" -> "も"
                "ya" -> "や"; "yu" -> "ゆ"; "yo" -> "よ"
                "ra" -> "ら"; "ri" -> "り"; "ru" -> "る"; "re" -> "れ"; "ro" -> "ろ"
                "wa" -> "わ"; "wo" -> "を"
                "xa" -> "ぁ"; "xi" -> "ぃ"; "xu" -> "ぅ"; "xe" -> "ぇ"; "xo" -> "ぉ"
                else -> null
            }
        }
        if (consumed != null) {
            out.append(consumed)
            i += 2
            continue
        }

        // Single vowels and syllabic n.
        consumed = tryMatch(this, i, 1) { one ->
            when (one) {
                "a" -> "あ"; "i" -> "い"; "u" -> "う"; "e" -> "え"; "o" -> "お"
                "n" -> "ん"
                else -> null
            }
        }
        if (consumed != null) {
            out.append(consumed)
            i += 1
            continue
        }

        // Sokuon: a doubled consonant means っ + the base consonant's kana
        // consumed on the next step (kka → っ + ka → っか).
        if (i + 1 < length && this[i] == this[i + 1] && this[i] in "kstchfmpgdb") {
            out.append('っ')
            i += 1
            continue
        }

        out.append(this[i])
        i += 1
    }
    return out.toString()
}

private inline fun tryMatch(text: String, index: Int, length: Int, map: (String) -> String?): String? {
    if (index + length > text.length) return null
    return map(text.substring(index, index + length))
}

/**
 * True when [text] contains a wildcard character (`*` or `?`).
 * Wildcards are opt-in — a literal `*` in a query is a wildcard,
 * which is the documented search convention (spec §15).
 */
fun String.containsWildcard(): Boolean = contains('*') || contains('?')

/**
 * A compiled wildcard matcher over normalized text. `*` matches any run
 * (including empty), `?` matches exactly one character, and literal text
 * matches after [String.normalizeForSearch] — case-, width- and
 * script-insensitive by construction (the pattern and the candidate are
 * both normalized before matching). Classic backtracking match, so `*`
 * groups are greedy but never fail to find a feasible split.
 */
class WildcardPattern private constructor(private val pattern: String) {

    /** True when [candidate] matches the pattern under normalization. */
    fun matches(candidate: String): Boolean {
        val text = candidate.normalizeForSearch()
        // Fast path: plain substring when the pattern has no wildcards.
        if (!pattern.contains('*') && !pattern.contains('?')) {
            return text.contains(pattern)
        }
        return matchAt(text, 0, 0)
    }

    /**
     * Backtracking wildcard match. pIndex walks the (normalized) pattern,
     * tIndex walks the (normalized) candidate.
     */
    private fun matchAt(text: String, pIndex: Int, tIndex: Int): Boolean {
        if (pIndex == pattern.length) return tIndex == text.length
        return when (val c = pattern[pIndex]) {
            '*' -> {
                // Greedy star: either consume one text char and retry the
                // star, or stop the star and match the rest of the pattern.
                if (matchAt(text, pIndex + 1, tIndex)) return true
                tIndex < text.length && matchAt(text, pIndex, tIndex + 1)
            }
            '?' -> tIndex < text.length && matchAt(text, pIndex + 1, tIndex + 1)
            else -> tIndex < text.length && text[tIndex] == c &&
                matchAt(text, pIndex + 1, tIndex + 1)
        }
    }

    companion object {
        fun compile(pattern: String): WildcardPattern =
            WildcardPattern(pattern.normalizeForSearch())
    }
}

/**
 * One-stop query matcher used by search surfaces:
 *  - empty [query] matches nothing (callers handle blank separately);
 *  - wildcard queries compile to [WildcardPattern];
 *  - otherwise the query is normalized and [candidate] must contain it.
 *
 * Romaji input is converted to kana before matching, mirroring the search
 * engine's DB-term normalization (taberu matches たべる). Kanji passes
 * through both transforms untouched.
 */
fun queryMatches(query: String, candidate: String): Boolean {
    if (query.isBlank()) return false
    return if (query.containsWildcard()) {
        WildcardPattern.compile(query).matches(candidate)
    } else {
        val normalizedQuery = query.normalizeForSearch()
        val queryForm = if (normalizedQuery.any { it.isLetter() && it.code < 128 }) {
            normalizedQuery.romajiToHiragana()
        } else {
            normalizedQuery
        }
        val text = candidate.normalizeForSearch()
        text.contains(queryForm)
    }
}
