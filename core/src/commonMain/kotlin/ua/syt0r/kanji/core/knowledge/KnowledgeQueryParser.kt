package ua.syt0r.kanji.core.knowledge

// ============================================================
// KNOWLEDGE QUERY PARSER — structured filters in plain text
// ------------------------------------------------------------
// KT-SEARCH-002 (spec §15): a query like "common verbs N3" must
// yield a meaningful filtered query without the user learning a
// query language. This parser is pure and deterministic:
//
//   "eat"                → text = "eat"
//   "N3 verb"            → text = "", jlpt = 3, pos = verb
//   "jlpt:n2 common"     → text = "", jlpt = 2, frequency = Common
//   "4 strokes 食べる"    → text = "食べる", strokeCount = 4
//   "い-adjective rare"   → text = "", pos = い-adjective,
//                          frequency = Rare
//
// Recognized tokens become chips the UI can show and clear. The
// remaining text is searched as before — nothing is lost, unknown
// tokens simply stay part of the text query.
// ============================================================

/** A parsed query: the remaining free text plus real filters + chips. */
data class ParsedKnowledgeQuery(
    val text: String,
    val filters: SearchFilters,
    /** Human labels of the recognized structured tokens, in input order. */
    val chips: List<String>
) {
    val hasFilters: Boolean get() = chips.isNotEmpty()
}

object KnowledgeQueryParser {

    /** POS keywords the engine can match against word part-of-speech tags. */
    private val posKeywords = listOf(
        "verb", "verbs", "noun", "nouns", "adjective", "adjectives",
        "い-adjective", "い-adjectives", "な-adjective", "な-adjectives", "adverb", "adverbs",
        "ichidan", "godan", "suru", "transitive", "intransitive",
        "particle", "particles", "pre-noun", "conjunction", "counter"
    )

    /** Frequency keywords → band. */
    private val frequencyKeywords = mapOf(
        "verycommon" to FrequencyBand.VeryCommon,
        "very-common" to FrequencyBand.VeryCommon,
        "common" to FrequencyBand.Common,
        "frequent" to FrequencyBand.Common,
        "moderate" to FrequencyBand.Moderate,
        "uncommon" to FrequencyBand.Uncommon,
        "rare" to FrequencyBand.Rare
    )

    /** Study-state keywords → StudyState (todo #109, spec §18). */
    private val studyStateKeywords = mapOf(
        "known" to StudyState.Known,
        "mastered" to StudyState.Mastered,
        "learning" to StudyState.Learning,
        "new" to StudyState.New,
        "due" to StudyState.Due,
        "review" to StudyState.Due,
        "relearning" to StudyState.Relearning,
        "suspended" to StudyState.Suspended
    )

    fun parse(raw: String): ParsedKnowledgeQuery {
        var jlpt: Int? = null
        var grade: Int? = null
        var frequency: FrequencyBand? = null
        var strokeCount: Int? = null
        var partOfSpeech: String? = null
        var studyState: StudyState? = null
        val chips = mutableListOf<String>()
        val freeText = mutableListOf<String>()

        raw.split(Regex("\\s+")).filter { it.isNotBlank() }.forEach { token ->
            when {
                // Explicit "jlpt:n3" / "grade:2" / "frequency:common" / "strokes:4"
                Regex("^jlpt:?(\\d)$", RegexOption.IGNORE_CASE).matchEntire(token)?.let {
                    jlpt = it.groupValues[1].toInt().coerceIn(1, 5)
                    chips += "JLPT:N$jlpt"
                } != null -> Unit

                Regex("^n([1-5])$", RegexOption.IGNORE_CASE).matchEntire(token)?.let {
                    jlpt = it.groupValues[1].toInt()
                    chips += "JLPT:N$jlpt"
                } != null -> Unit

                Regex("^grade:?(\\d{1,2})$", RegexOption.IGNORE_CASE).matchEntire(token)?.let {
                    grade = it.groupValues[1].toInt()
                    chips += "Grade $grade"
                } != null -> Unit

                Regex("^strokes?:?(\\d{1,2})$", RegexOption.IGNORE_CASE).matchEntire(token)?.let {
                    strokeCount = it.groupValues[1].toInt()
                    chips += "$strokeCount strokes"
                } != null -> Unit

                Regex("^(\\d{1,2})-?strokes?$", RegexOption.IGNORE_CASE).matchEntire(token)?.let {
                    strokeCount = it.groupValues[1].toInt()
                    chips += "$strokeCount strokes"
                } != null -> Unit

                // "frequency:common" / "frequency common"
                token.startsWith("frequency:", ignoreCase = true) -> {
                    val key = token.substringAfter(':').lowercase()
                    frequencyKeywords[key]?.let { band ->
                        frequency = band
                        chips += band.label
                    } ?: run { freeText += token }
                }

                else -> {
                    val lower = token.lowercase()
                    when {
                        lower in posKeywords -> {
                            // The filter stores the singular stem so it matches
                            // real POS tags ("verb" ⊂ "Ichidan verb"); the chip
                            // shows what the user typed.
                            partOfSpeech = token.trimEnd('s')
                            chips += token
                        }
                        lower in frequencyKeywords -> {
                            frequency = frequencyKeywords[lower]
                            chips += frequency!!.label
                        }
                        lower in studyStateKeywords -> {
                            studyState = studyStateKeywords[lower]
                            chips += studyState!!.label
                        }
                        else -> freeText += token
                    }
                }
            }
        }

        return ParsedKnowledgeQuery(
            text = freeText.joinToString(" "),
            filters = SearchFilters(
                jlpt = jlpt,
                grade = grade,
                frequency = frequency,
                strokeCount = strokeCount,
                partOfSpeech = partOfSpeech,
                studyState = studyState
            ),
            chips = chips
        )
    }
}
