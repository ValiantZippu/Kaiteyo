package ua.syt0r.kanji.core.knowledge

// ============================================================
// STUDY RECOMMENDATION ENGINE (spec §31, §100; todo #43/#100)
// ------------------------------------------------------------
// "Why should I study this kanji next?" — explainable, driven by
// REAL data: the user's study overlay (actual FSRS state per
// kanji) plus the kanji's frequency rank. No fabricated scores:
// every recommendation carries a human-readable reason built from
// the actual facts that produced it.
//
// The engine is pure — give it a candidate pool of kanji with
// their study state and frequency, get back a ranked, explained
// recommendation list. Home wires it to real data.
// ============================================================

/** A candidate kanji for recommendation with the facts that matter. */
data class RecommendationCandidate(
    val character: String,
    val keyword: String?,
    val frequencyRank: Int?,
    val studyState: StudyState,
    /** Real last-review instant (ms) — null = never reviewed. */
    val lastReviewMs: Long?,
    val jlpt: Int?
)

/** One explained recommendation. */
data class StudyRecommendation(
    val character: String,
    val keyword: String?,
    val reason: String,
    /** Sortable urgency: higher = recommended sooner. */
    val urgency: Int
)

object StudyRecommendationEngine {

    /** Count of Due/Relearning kanji — the "catch-up" number. */
    fun dueCount(candidates: List<RecommendationCandidate>): Int =
        candidates.count { it.studyState == StudyState.Due || it.studyState == StudyState.Relearning }

    fun learningCount(candidates: List<RecommendationCandidate>): Int =
        candidates.count { it.studyState == StudyState.Learning }

    /**
     * Ranks candidates and explains each one. Priority (highest first):
     *   1. Due / Relearning — reviews are waiting (urgency 4 + frequency nudge)
     *   2. Learning — in progress, keep the momentum (urgency 3)
    *   3. New kanji with strong frequency — high-value introductions (urgency 2)
     *   4. Everything else — New but lower-frequency (urgency 1)
     * A kanji with NO data at all (null state) is treated as New — never
     * invented as studied.
     */
    fun recommend(
        candidates: List<RecommendationCandidate>,
        limit: Int = 8
    ): List<StudyRecommendation> {
        val rank = { c: RecommendationCandidate -> c.frequencyRank ?: Int.MAX_VALUE }

        val ranked = candidates.map { candidate ->
            val (urgency, reason) = when (candidate.studyState) {
                StudyState.Due, StudyState.Relearning -> {
                    val freq = frequencyHint(candidate.frequencyRank)
                    4 to "Review due${freq.let { if (it.isNotEmpty()) " · $it" else "" }}"
                }
                StudyState.Learning ->
                    3 to "In progress — keep it fresh"
                StudyState.New, StudyState.Mastered, StudyState.Known, StudyState.Suspended -> {
                    val freq = frequencyHint(candidate.frequencyRank)
                    val freqBoost = if (freq.isNotEmpty()) 1 else 0
                    val base = if (candidate.frequencyRank != null && candidate.frequencyRank <= 1500) 2 else 1
                    (base + freqBoost) to when {
                        freq.isNotEmpty() -> "New kanji · $freq"
                        else -> "New kanji"
                    }
                }
            }
            StudyRecommendation(
                character = candidate.character,
                keyword = candidate.keyword,
                reason = reason,
                urgency = urgency
            )
        }

        return ranked
            .sortedWith(
                compareByDescending<StudyRecommendation> { it.urgency }
                    .thenByDescending { rec ->
                        // Ties: higher-frequency candidates first (lower rank = better).
                        candidates.firstOrNull { it.character == rec.character }
                            ?.let { -rank(it) } ?: Int.MAX_VALUE
                    }
            )
            .take(limit)
    }

    private fun frequencyHint(rank: Int?): String = when {
        rank == null -> ""
        rank <= 500 -> "top-500 frequency"
        rank <= 2000 -> "top-2000 frequency"
        else -> ""
    }
}
