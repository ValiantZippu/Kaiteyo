package ua.syt0r.kanji.core.knowledge

import kotlinx.serialization.Serializable

// ============================================================
// LEVEL PROFILES — level-adaptive presentation
// ------------------------------------------------------------
// One knowledge model; profiles only adapt presentation and
// content. A profile never deletes information — it controls
// visibility, sentence difficulty, romaji/translation defaults,
// explanation depth and graph complexity (spec §23–§24).
// Profiles can be switched at any time and never destroy data.
//
// The presentation defaults are a catalog (like KanjiCardPresets
// / FrequencyBand): the profile enum is the identity, the catalog
// supplies what each profile shows by default, and a Custom
// profile carries user overrides.
// ============================================================

/** The learner profiles. [Custom] uses [ProfilePresentation] overrides. */
@Serializable
enum class LearnerProfile(val id: String, val displayName: String, val jpDisplayName: String) {
    ChildBeginner("child-beginner", "Child beginner", "こども"),
    AbsoluteBeginner("absolute-beginner", "Absolute beginner", "超初心者"),
    Beginner("beginner", "Beginner", "初心者"),
    LowerIntermediate("lower-intermediate", "Lower intermediate", "初中級"),
    Intermediate("intermediate", "Intermediate", "中級"),
    UpperIntermediate("upper-intermediate", "Upper intermediate", "中上級"),
    Advanced("advanced", "Advanced", "上級"),
    Native("native", "Native", "ネイティブ"),
    Research("research", "Research", "研究者"),
    Custom("custom", "Custom", "カスタム")
}

/** How deeply kanji/word explanations go. */
@Serializable
enum class ExplanationDepth {
    /** Very simple language (child / absolute beginner). */
    Simple,
    /** Clear, learner-friendly explanations. */
    Clear,
    /** Technical detail (advanced). */
    Technical,
    /** Japanese-only explanations (native). */
    JapaneseOnly
}

/** Difficulty of example sentences and generated study content. */
@Serializable
enum class SentenceDifficulty {
    Easy,
    Mixed,
    Hard
}

/** How much of the relationship graph is shown by default. */
@Serializable
enum class GraphComplexity {
    /** Direct neighbors only (radical → kanji → top words). */
    Simple,
    /** Neighbors plus one related-hop (related kanji). */
    Standard,
    /** Full expanded surface (everything the explorer can show). */
    Full
}

/**
 * What a profile shows. Every flag only hides/adapts presentation —
 * the underlying data is always available (rare readings, etymology,
 * advanced grammar are never deleted, only hidden by default).
 */
@Serializable
data class ProfilePresentation(
    val showFurigana: Boolean = true,
    val showRomaji: Boolean = false,
    val showTranslations: Boolean = true,
    val showRareReadings: Boolean = false,
    val showEtymology: Boolean = false,
    val explanationDepth: ExplanationDepth = ExplanationDepth.Clear,
    val sentenceDifficulty: SentenceDifficulty = SentenceDifficulty.Mixed,
    val graphComplexity: GraphComplexity = GraphComplexity.Standard,
    /** Id of the kanji-page card preset (see KanjiCardPresets). */
    val cardPresetId: String = "standard"
)

/**
 * The default presentation for each profile (spec §23: an absolute
 * beginner sees meaning/reading/furigana/simple examples; an advanced
 * user sees rare readings, corpus data, grammar and complex
 * relationships). The Custom profile starts from the Intermediate
 * defaults and is meant to be overridden by user settings.
 */
object LearnerProfileCatalog {

    fun defaultsFor(profile: LearnerProfile): ProfilePresentation = when (profile) {
        LearnerProfile.ChildBeginner -> ProfilePresentation(
            showFurigana = true,
            showRomaji = false,
            showTranslations = true,
            explanationDepth = ExplanationDepth.Simple,
            sentenceDifficulty = SentenceDifficulty.Easy,
            graphComplexity = GraphComplexity.Simple,
            cardPresetId = "beginner"
        )

        LearnerProfile.AbsoluteBeginner -> ProfilePresentation(
            showFurigana = true,
            showRomaji = true,
            showTranslations = true,
            explanationDepth = ExplanationDepth.Simple,
            sentenceDifficulty = SentenceDifficulty.Easy,
            graphComplexity = GraphComplexity.Simple,
            cardPresetId = "beginner"
        )

        LearnerProfile.Beginner -> ProfilePresentation(
            showFurigana = true,
            showRomaji = true,
            showTranslations = true,
            explanationDepth = ExplanationDepth.Clear,
            sentenceDifficulty = SentenceDifficulty.Easy,
            graphComplexity = GraphComplexity.Simple,
            cardPresetId = "beginner"
        )

        LearnerProfile.LowerIntermediate -> ProfilePresentation(
            showFurigana = true,
            showRomaji = false,
            showTranslations = true,
            explanationDepth = ExplanationDepth.Clear,
            sentenceDifficulty = SentenceDifficulty.Easy,
            graphComplexity = GraphComplexity.Standard,
            cardPresetId = "standard"
        )

        LearnerProfile.Intermediate -> ProfilePresentation(
            showFurigana = true,
            showRomaji = false,
            showTranslations = true,
            explanationDepth = ExplanationDepth.Clear,
            sentenceDifficulty = SentenceDifficulty.Mixed,
            graphComplexity = GraphComplexity.Standard,
            cardPresetId = "standard"
        )

        LearnerProfile.UpperIntermediate -> ProfilePresentation(
            showFurigana = false,
            showRomaji = false,
            showTranslations = true,
            showRareReadings = false,
            explanationDepth = ExplanationDepth.Technical,
            sentenceDifficulty = SentenceDifficulty.Mixed,
            graphComplexity = GraphComplexity.Standard,
            cardPresetId = "advanced"
        )

        LearnerProfile.Advanced -> ProfilePresentation(
            showFurigana = false,
            showRomaji = false,
            showTranslations = true,
            showRareReadings = true,
            explanationDepth = ExplanationDepth.Technical,
            sentenceDifficulty = SentenceDifficulty.Hard,
            graphComplexity = GraphComplexity.Full,
            cardPresetId = "advanced"
        )

        LearnerProfile.Native -> ProfilePresentation(
            showFurigana = false,
            showRomaji = false,
            showTranslations = false,
            showRareReadings = true,
            showEtymology = true,
            explanationDepth = ExplanationDepth.JapaneseOnly,
            sentenceDifficulty = SentenceDifficulty.Hard,
            graphComplexity = GraphComplexity.Full,
            cardPresetId = "research"
        )

        LearnerProfile.Research -> ProfilePresentation(
            showFurigana = true,
            showRomaji = false,
            showTranslations = true,
            showRareReadings = true,
            showEtymology = true,
            explanationDepth = ExplanationDepth.Technical,
            sentenceDifficulty = SentenceDifficulty.Hard,
            graphComplexity = GraphComplexity.Full,
            cardPresetId = "research"
        )

        LearnerProfile.Custom -> ProfilePresentation()
    }

    /** Human-readable profile list for pickers (id/name/description). */
    data class ProfilePreset(val id: String, val name: String, val description: String)

    val presets: List<ProfilePreset> = listOf(
        ProfilePreset("child-beginner", "Child beginner", "Visual, simple, story-first"),
        ProfilePreset("absolute-beginner", "Absolute beginner", "Meaning, reading, furigana, simple examples"),
        ProfilePreset("beginner", "Beginner", "Clear explanations, easy sentences, romaji on"),
        ProfilePreset("lower-intermediate", "Lower intermediate", "Romaji off, standard cards"),
        ProfilePreset("intermediate", "Intermediate", "Standard cards, mixed difficulty"),
        ProfilePreset("upper-intermediate", "Upper intermediate", "Technical depth, advanced cards"),
        ProfilePreset("advanced", "Advanced", "Rare readings, corpus data, full graph"),
        ProfilePreset("native", "Native", "Japanese-first, no translations"),
        ProfilePreset("research", "Research", "Everything visible"),
        ProfilePreset("custom", "Custom", "Your own combination")
    )

    fun byId(id: String): LearnerProfile? = LearnerProfile.entries.firstOrNull { it.id == id }
}
