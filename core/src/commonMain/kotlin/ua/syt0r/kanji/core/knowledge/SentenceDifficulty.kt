package ua.syt0r.kanji.core.knowledge

// ============================================================
// SENTENCE DIFFICULTY (spec §26–§27)
// ------------------------------------------------------------
// A deterministic, explainable difficulty score for corpus
// sentences, computed from surface features only:
//   - length (characters + tokens)
//   - kanji density (proportion of kanji characters)
//   - unique kanji count
//   - grammar pattern density
//   - optional known-kanji overlay (when the user supplies their
//     studied set, unknown kanji raise the score)
//
// The score is a 1..10 level with a human label. It is a
// heuristic — no ML, no fabricated provenance. The bundled
// corpus itself has no per-sentence rating, so this surface
// estimate is what the UI shows, labeled as an estimate.
// ============================================================

enum class SentenceDifficultyTier(val label: String) {
    VeryEasy("Very easy"),
    Easy("Easy"),
    Moderate("Moderate"),
    Hard("Hard"),
    VeryHard("Very hard")
}

@kotlinx.serialization.Serializable
data class SentenceDifficultyLevel(
    /** 1..10, higher = harder. */
    val level: Int,
    val tier: SentenceDifficultyTier,
    val label: String,
    /** Human-readable list of contributing factors. */
    val factors: List<String> = emptyList()
)

object SentenceDifficultyScorer {

    const val MIN_LEVEL = 1
    const val MAX_LEVEL = 10

    /**
     * Pure surface-feature score. [knownKanji] is the set of kanji the
     * user has studied (optional) — unknown kanji push the level up.
     */
    fun score(
        sentence: String,
        tokenCount: Int = 0,
        kanjiTokens: Int = 0,
        grammarMatchCount: Int = 0,
        knownKanji: Set<String> = emptySet()
    ): SentenceDifficultyLevel {
        if (sentence.isBlank()) {
            return SentenceDifficultyLevel(MIN_LEVEL, SentenceDifficultyTier.VeryEasy, "Very easy")
        }

        val factors = mutableListOf<String>()
        var score = 0.0

        // Length factor.
        val chars = sentence.length
        score += when {
            chars <= 6 -> 0.0
            chars <= 12 -> 1.0
            chars <= 20 -> 2.0
            chars <= 30 -> 3.0
            else -> 4.0
        }
        if (chars > 20) factors.add("long sentence")

        // Kanji density factor.
        val kanjiChars = sentence.count { it.isKanjiUnicode() }
        val density = if (chars > 0) kanjiChars.toDouble() / chars else 0.0
        score += when {
            density == 0.0 -> 0.0
            density < 0.2 -> 1.0
            density < 0.35 -> 2.0
            else -> 3.0
        }
        if (density >= 0.35) factors.add("high kanji density")
        if (kanjiChars >= 5) factors.add("$kanjiChars kanji characters")

        // Grammar density.
        score += when {
            grammarMatchCount == 0 -> 0.0
            grammarMatchCount <= 2 -> 1.0
            else -> 2.0
        }
        if (grammarMatchCount >= 3) factors.add("$grammarMatchCount grammar patterns")

        // Unknown kanji overlay.
        if (knownKanji.isNotEmpty()) {
            val uniqueKanji = sentence.filter { it.isKanjiUnicode() }.map { it.toString() }.toSet()
            val unknown = uniqueKanji - knownKanji
            if (unknown.isNotEmpty()) {
                score += when {
                    unknown.size <= 1 -> 0.5
                    unknown.size <= 3 -> 1.0
                    else -> 2.0
                }
                factors.add("${unknown.size} kanji not in your study set")
            }
        }

        val level = (score.toInt() + 1).coerceIn(MIN_LEVEL, MAX_LEVEL)
        val tier = when {
            level <= 2 -> SentenceDifficultyTier.VeryEasy
            level <= 4 -> SentenceDifficultyTier.Easy
            level <= 6 -> SentenceDifficultyTier.Moderate
            level <= 8 -> SentenceDifficultyTier.Hard
            else -> SentenceDifficultyTier.VeryHard
        }
        return SentenceDifficultyLevel(level, tier, tier.label, factors)
    }

    /** True when a sentence is acceptable for a profile's difficulty. */
    fun acceptableFor(
        level: SentenceDifficultyLevel,
        profile: LearnerProfile
    ): Boolean = acceptableForDifficulty(level, profile.sentenceDifficultyPreference())

    /** True when a sentence is acceptable for a difficulty preference. */
    fun acceptableForDifficulty(
        level: SentenceDifficultyLevel,
        preference: ua.syt0r.kanji.core.knowledge.SentenceDifficulty
    ): Boolean {
        val max = when (preference) {
            ua.syt0r.kanji.core.knowledge.SentenceDifficulty.Easy -> 4
            ua.syt0r.kanji.core.knowledge.SentenceDifficulty.Mixed -> 7
            ua.syt0r.kanji.core.knowledge.SentenceDifficulty.Hard -> 10
        }
        return level.level <= max
    }

    /** The difficulty preference a profile defaults to (mirrors the catalog). */
    fun LearnerProfile.sentenceDifficultyPreference(): ua.syt0r.kanji.core.knowledge.SentenceDifficulty =
        LearnerProfileCatalog.defaultsFor(this).sentenceDifficulty

    private fun Char.isKanjiUnicode(): Boolean =
        code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF
}
