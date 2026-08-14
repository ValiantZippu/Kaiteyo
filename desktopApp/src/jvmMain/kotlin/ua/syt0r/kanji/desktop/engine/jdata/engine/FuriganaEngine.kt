package ua.syt0r.kanji.desktop.engine.jdata.engine

import ua.syt0r.kanji.desktop.engine.dictionary.JapaneseText
import ua.syt0r.kanji.desktop.engine.jdata.model.FuriganaSegment
import ua.syt0r.kanji.desktop.engine.jdata.normalize.Normalizer

// ============================================================
// FURIGANA ENGINE
// Produces structural furigana annotations (text + reading pairs)
// from an expression and its reading, so consumers can render
// 食[た]べる, 食べる or 食（た）べる as they wish. The reading text
// itself is never altered — only annotated.
//
// Strategy (two-stage, both honest):
//  1. When a kanji→reading lookup is supplied (from the kanji
//     subsystem's on/kun readings), match the reading against the
//     concatenated candidate readings of each kanji run, choosing
//     the candidate that leaves a feasible remainder.
//  2. Fallback: mechanical shortest-feasible segmentation. This is
//     best-effort and documented as such — perfect segmentation
//     needs a full per-word reading dictionary.
// ============================================================

object FuriganaEngine {

    /** [readingLookup] returns known readings for one kanji character (e.g. on/kun, dot-split). */
    fun parse(
        expression: String,
        reading: String,
        readingLookup: (String) -> List<String> = { emptyList() }
    ): List<FuriganaSegment> {
        val base = Normalizer.readingKey(reading)
        if (base.isEmpty() || expression.isEmpty()) return emptyList()

        val result = mutableListOf<FuriganaSegment>()
        var cursor = 0
        var i = 0
        while (i < expression.length) {
            val c = expression[i]
            if (JapaneseText.isKanjiChar(c)) {
                val start = i
                while (i < expression.length && JapaneseText.isKanjiChar(expression[i])) i++
                val run = expression.substring(start, i)
                val restExpr = expression.substring(i)
                val chosen = matchRun(run, restExpr, base, cursor, readingLookup)
                if (chosen != null) {
                    result.add(FuriganaSegment(run, chosen))
                    cursor += chosen.length
                } else {
                    result.add(FuriganaSegment(run, null))
                }
            } else {
                result.add(FuriganaSegment(c.toString(), null))
                if (cursor < base.length && base[cursor].toString() == c.toString()) cursor++
                i++
            }
        }
        return result
    }

    /**
     * Picks the reading for a kanji run. Prefers a dictionary-backed
     * candidate; otherwise falls back to the shortest reading prefix
     * that still leaves a feasible remainder (okurigana-safe).
     */
    private fun matchRun(
        run: String,
        restExpr: String,
        base: String,
        from: Int,
        lookup: (String) -> List<String>
    ): String? {
        if (from >= base.length) return null
        val maxLen = minOf(base.length - from, 24)

        // Stage 1: dictionary candidates (bounded cross product).
        val candidates = runReadings(run, lookup)
        for (candidate in candidates) {
            val len = candidate.length
            if (len == 0 || len > maxLen) continue
            if (base.regionMatches(from, candidate, 0, len) &&
                isFeasible(base, from + len, restExpr)
            ) {
                return candidate
            }
        }

        // Stage 2: mechanical shortest-feasible fallback.
        for (len in 1..maxLen) {
            if (isFeasible(base, from + len, restExpr)) {
                return base.substring(from, from + len)
            }
        }
        return null
    }

    /** Cross product of per-kanji readings, bounded to keep this linear-ish. */
    private fun runReadings(run: String, lookup: (String) -> List<String>): List<String> {
        val perChar = run.map { ch -> lookup(ch.toString()).distinct().take(3) }
        if (perChar.any { it.isEmpty() }) return emptyList()
        var combos = listOf("")
        for (readings in perChar) {
            val next = mutableListOf<String>()
            for (prefix in combos) {
                for (r in readings) {
                    if (next.size >= 12) break
                    next.add(prefix + r)
                }
                if (next.size >= 12) break
            }
            combos = next
        }
        return combos
    }

    /**
     * True when the kana characters remaining in [restExpr] can still be
     * matched in order by the remaining reading starting at [from].
     */
    private fun isFeasible(base: String, from: Int, restExpr: String): Boolean {
        val restKana = Normalizer.readingKey(restExpr).filter { isKanaChar(it) }
        if (restKana.isEmpty()) return true
        var r = from
        var k = 0
        while (r < base.length && k < restKana.length) {
            if (base[r] == restKana[k]) k++
            r++
        }
        return k == restKana.length
    }

    private fun isKanaChar(c: Char): Boolean = c in '぀'..'ゟ' || c in '゠'..'ヿ'
}
