package ua.syt0r.kanji.core.knowledge.level

import ua.syt0r.kanji.core.knowledge.ExplanationDepth
import ua.syt0r.kanji.core.knowledge.LearnerProfile
import ua.syt0r.kanji.core.knowledge.LearnerProfileCatalog
import ua.syt0r.kanji.core.knowledge.ProfilePresentation
import ua.syt0r.kanji.core.knowledge.SentenceDifficultyLevel
import ua.syt0r.kanji.core.knowledge.SentenceDifficultyScorer
import ua.syt0r.kanji.core.knowledge.SentenceKnowledge
import ua.syt0r.kanji.core.knowledge.WordKnowledge

// ============================================================
// LEVEL ADAPTER — pure presentation adaptation (spec §23–§24)
// ------------------------------------------------------------
// Maps a knowledge entity + a learner profile to the *display*
// form: which senses are shown, whether furigana/translations
// are visible, how many example sentences appear and at what
// difficulty. All functions are pure — the source data is never
// mutated, only filtered/limited for presentation.
//
// Principles:
//   - a profile never deletes data, it hides it by default
//   - sentence difficulty follows SentenceDifficultyScorer
//   - the caller passes the *effective* presentation (Custom
//     overrides resolved by LearnerProfileStore); these helpers
//     also accept a bare profile and fall back to catalog defaults
// ============================================================

object LevelAdapter {

    /** Resolves the presentation for a profile (catalog defaults). */
    fun presentationFor(profile: LearnerProfile): ProfilePresentation =
        LearnerProfileCatalog.defaultsFor(profile)

    /** The presentation actually in effect (Custom overrides honored). */
    fun effectivePresentation(
        profile: LearnerProfile,
        overrides: ProfilePresentation? = null
    ): ProfilePresentation = when {
        profile == LearnerProfile.Custom && overrides != null -> overrides
        else -> presentationFor(profile)
    }

    /**
     * Applies a user-level presentation override on top of the effective
     * presentation (spec §24). A null override leaves the profile default in
     * place — romaji is never forced off unless the user says so.
     */
    fun applyRomajiOverride(
        presentation: ProfilePresentation,
        romajiOverride: Boolean?
    ): ProfilePresentation = if (romajiOverride == null) presentation
    else presentation.copy(showRomaji = romajiOverride)

    /**
     * The glossary entries (senses) a profile should see. Beginners get the
     * first sense(s) as an orientation; advanced/native see everything.
     */
    fun adaptedGlossary(
        word: WordKnowledge,
        presentation: ProfilePresentation
    ): List<String> {
        val max = when (presentation.explanationDepth) {
            ExplanationDepth.Simple -> 1
            ExplanationDepth.Clear -> 3
            ExplanationDepth.Technical,
            ExplanationDepth.JapaneseOnly -> Int.MAX_VALUE
        }
        return if (max == Int.MAX_VALUE) word.glossary
        else word.glossary.take(max)
    }

    /** True when the profile shows furigana on dictionary pages. */
    fun showFurigana(presentation: ProfilePresentation): Boolean =
        presentation.showFurigana

    /** True when the profile shows romaji. */
    fun showRomaji(presentation: ProfilePresentation): Boolean =
        presentation.showRomaji

    /** True when translations are visible (Native hides them). */
    fun showTranslations(presentation: ProfilePresentation): Boolean =
        presentation.showTranslations

    /**
     * Filters example sentences by the profile's difficulty bound, then caps
     * the count. Order is preserved; the bound comes from
     * [SentenceDifficultyScorer.acceptableFor].
     */
    fun adaptedSentences(
        sentences: List<SentenceKnowledge>,
        presentation: ProfilePresentation,
        limit: Int = 6
    ): List<SentenceKnowledge> = sentences
        .filter { sentence ->
            val level = SentenceDifficultyScorer.score(sentence.text)
            SentenceDifficultyScorer.acceptableForDifficulty(level, presentation.sentenceDifficulty)
        }
        .take(limit)

    /** Sentence difficulty level for display (surface-feature estimate). */
    fun sentenceLevel(sentence: SentenceKnowledge): SentenceDifficultyLevel =
        SentenceDifficultyScorer.score(sentence.text)

    /**
     * The JLPT band a profile should focus on by default (spec §23). Used by
     * Browse / KanjiBrowser "Recommended for your level" surfaces so a
     * beginner's default view is N5 rather than the whole jōyō set. Native /
     * Research / Custom have no restriction.
     */
    fun recommendedJlpt(profile: LearnerProfile): Set<Int> = when (profile) {
        LearnerProfile.ChildBeginner,
        LearnerProfile.AbsoluteBeginner,
        LearnerProfile.Beginner -> setOf(5)

        LearnerProfile.LowerIntermediate -> setOf(4, 5)
        LearnerProfile.Intermediate -> setOf(3, 4)
        LearnerProfile.UpperIntermediate -> setOf(2, 3)
        LearnerProfile.Advanced -> setOf(1, 2)
        LearnerProfile.Native,
        LearnerProfile.Research,
        LearnerProfile.Custom -> emptySet()
    }

    /** Short display label for the recommended band, e.g. "N5" or "N2–N3". */
    fun recommendedJlptLabel(levels: Set<Int>): String = when (levels.size) {
        0 -> "All levels"
        1 -> "N${levels.first()}"
        else -> levels.sorted().joinToString("–") { "N$it" }
    }
}
