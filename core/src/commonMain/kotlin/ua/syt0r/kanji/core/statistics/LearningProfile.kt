package ua.syt0r.kanji.core.statistics

import kotlin.math.roundToInt

// ============================================================
// PERSONAL LEARNING PROFILE
// A data-backed summary of strengths and weaknesses. Every field
// is derived from real study data (knowledge state, skill matrix,
// mistake records and study sessions). No invented motivational
// text: the conclusion is a plain statement of what the data says.
// ============================================================

/** A data-backed personal learning profile. Nulls mean "no data to support this conclusion". */
data class LearningProfile(
    /** Content type with the highest learned/studied ratio. */
    val strongestContentType: String? = null,
    /** Content type with the lowest learned/studied ratio (studied > 0). */
    val weakestContentType: String? = null,
    /** Skill with the highest measured accuracy. */
    val bestSkill: String? = null,
    /** Skill with the lowest measured accuracy. */
    val weakestSkill: String? = null,
    /** JLPT band with the lowest studied coverage (total > 0). */
    val weakestJlptBand: Int? = null,
    /** Study mode the user spends the most sessions in. */
    val dominantStudyMode: String? = null,
    /** Session counts per study mode, descending. */
    val studyModeBalance: List<Pair<String, Long>> = emptyList(),
    /** One-sentence, data-backed conclusion. Empty when there is too little data. */
    val conclusion: String = ""
) {
    val hasMeaningfulData: Boolean
        get() = strongestContentType != null || bestSkill != null || dominantStudyMode != null
}

object ProfileCalculator {

    private fun displayName(contentType: String): String = when (contentType) {
        ContentTypes.KANJI -> "Kanji"
        ContentTypes.VOCAB -> "Vocabulary"
        ContentTypes.RADICAL -> "Radicals"
        ContentTypes.GRAMMAR -> "Grammar"
        ContentTypes.KANA -> "Kana"
        else -> contentType
    }

    /**
     * Builds the profile from aggregated analytics. All inputs come from
     * [StatisticsController] which reads real database data.
     */
    fun build(
        contentKnowledge: List<ContentTypeKnowledge>,
        skillMatrix: List<SkillMatrixRow>,
        mistakeCategories: List<Pair<String, Long>>,
        studySessions: List<StudySessionRecord>
    ): LearningProfile {
        // 1. Content-type strength by learned/studied ratio.
        val studiedTypes = contentKnowledge.filter { it.studied > 0 }
        val byRatio = studiedTypes
            .map { it.contentType to (it.learned.toFloat() / it.studied) }
            .sortedByDescending { it.second }
        val strongestContentType = byRatio.firstOrNull()?.first
        val weakestContentType = byRatio.lastOrNull()?.first

        // 2. Skills with real measurements, ranked by accuracy.
        val skills = skillMatrix.flatMap { row ->
            listOf(
                row.recognition?.let { row.label to it },
                row.reading?.let { "${row.label} reading" to it },
                row.meaning?.let { "${row.label} meaning" to it },
                row.writing?.let { "${row.label} writing" to it }
            ).filterNotNull()
        }.sortedByDescending { it.second }
        val bestSkill = skills.firstOrNull()?.first
        val weakestSkill = skills.lastOrNull()?.first

        // 3. Weakest JLPT band from kanji coverage (only bands with a catalog).
        val weakestJlptBand = contentKnowledge
            .flatMap { it.jlptCoverage }
            .filter { it.total > 0 }
            .minByOrNull { it.studiedRatio }
            ?.level

        // 4. Dominant study mode from recorded sessions.
        val modeCounts = studySessions
            .groupingBy { it.mode.ifBlank { "study" } }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key to it.value.toLong() }
        val dominantStudyMode = modeCounts.firstOrNull()?.first

        // 5. Mistake pressure: the most common mistake category (if any).
        val topMistake = mistakeCategories.firstOrNull()?.let { (category, count) ->
            category.replace("_", " ") to count
        }

        // 6. One-sentence conclusion assembled strictly from the above.
        val conclusion = buildString {
            val parts = mutableListOf<String>()
            if (strongestContentType != null) {
                parts += "Strongest area: ${displayName(strongestContentType)}"
            }
            if (bestSkill != null) {
                parts += "best skill: $bestSkill"
            }
            if (weakestContentType != null && weakestContentType != strongestContentType) {
                parts += "weakest area: ${displayName(weakestContentType)}"
            }
            if (weakestSkill != null) {
                parts += "weakest skill: $weakestSkill"
            }
            if (weakestJlptBand != null) {
                parts += "lowest JLPT coverage: N$weakestJlptBand"
            }
            if (topMistake != null) {
                parts += "most common mistake: ${topMistake.first}"
            }
            if (parts.isEmpty()) {
                append("Study more to build a profile — no measurable area is available yet.")
            } else {
                append(parts.joinToString(". ").replaceFirstChar { it.uppercase() }.plus("."))
            }
        }

        return LearningProfile(
            strongestContentType = strongestContentType,
            weakestContentType = weakestContentType,
            bestSkill = bestSkill,
            weakestSkill = weakestSkill,
            weakestJlptBand = weakestJlptBand,
            dominantStudyMode = dominantStudyMode,
            studyModeBalance = modeCounts,
            conclusion = conclusion
        )
    }

    /** Formats a 0..1 accuracy as "NN%". */
    fun percent(value: Float): String = "${(value * 100).roundToInt()}%"
}
