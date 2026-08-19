package ua.syt0r.kanji.core.knowledge.cards

import ua.syt0r.kanji.core.knowledge.LearnerProfile

// ============================================================
// PRESET ADAPTER
// ------------------------------------------------------------
// Bridges the learner-profile system (§23, §16) and the card
// system (§12, §20-21). Each profile recommends a default card
// preset per entity type, so a beginner's kanji page shows fewer
// cards than an advanced learner's — without destroying any data.
// Users can always override via the card settings screen.
// ============================================================

/**
 * Recommended card presets for a learner profile.
 */
data class ProfileCardPresets(
    val kanjiPresetId: String,
    val wordPresetId: String,
    val sentencePresetId: String,
    val grammarPresetId: String,
    val collectionPresetId: String
)

/**
 * Maps learner profiles to recommended card presets.
 */
object PresetAdapter {

    /**
     * Returns the recommended card preset ids for a profile.
     */
    fun recommendedPresets(profile: LearnerProfile): ProfileCardPresets = when (profile) {
        // Absolute beginners: minimal information, no advanced cards.
        LearnerProfile.AbsoluteBeginner,
        LearnerProfile.ChildBeginner -> ProfileCardPresets(
            kanjiPresetId = "beginner",
            wordPresetId = "minimal",
            sentencePresetId = "minimal",
            grammarPresetId = "minimal",
            collectionPresetId = "minimal"
        )

        // Beginners: core info, simple examples.
        LearnerProfile.Beginner -> ProfileCardPresets(
            kanjiPresetId = "beginner",
            wordPresetId = "beginner",
            sentencePresetId = "beginner",
            grammarPresetId = "beginner",
            collectionPresetId = "beginner"
        )

        // Intermediate: everything useful, no study overlay by default.
        LearnerProfile.LowerIntermediate,
        LearnerProfile.Intermediate -> ProfileCardPresets(
            kanjiPresetId = "standard",
            wordPresetId = "standard",
            sentencePresetId = "standard",
            grammarPresetId = "standard",
            collectionPresetId = "standard"
        )

        // Advanced: everything including grammar and study actions.
        LearnerProfile.UpperIntermediate,
        LearnerProfile.Advanced -> ProfileCardPresets(
            kanjiPresetId = "advanced",
            wordPresetId = "advanced",
            sentencePresetId = "advanced",
            grammarPresetId = "advanced",
            collectionPresetId = "advanced"
        )

        // Native/Research: everything visible.
        LearnerProfile.Native,
        LearnerProfile.Research -> ProfileCardPresets(
            kanjiPresetId = "research",
            wordPresetId = "research",
            sentencePresetId = "research",
            grammarPresetId = "research",
            collectionPresetId = "research"
        )

        // Custom: default to the standard layout.
        LearnerProfile.Custom -> ProfileCardPresets(
            kanjiPresetId = "standard",
            wordPresetId = "standard",
            sentencePresetId = "standard",
            grammarPresetId = "standard",
            collectionPresetId = "standard"
        )
    }

    /**
     * Resolves a preset by id and entity type, falling back to the
     * profile-recommended preset when the id is unknown.
     */
    fun resolvePreset(
        entity: CardEntityType,
        presetId: String?,
        profile: LearnerProfile
    ): String {
        if (presetId != null) {
            val exists = when (entity) {
                CardEntityType.Kanji -> KanjiCardPresets.byId(presetId) != null
                CardEntityType.Word -> WordCardPresets.byId(presetId) != null
                CardEntityType.Sentence -> SentenceCardPresets.byId(presetId) != null
                CardEntityType.Grammar -> GrammarCardPresets.byId(presetId) != null
                CardEntityType.Collection -> CollectionCardPresets.byId(presetId) != null
            }
            if (exists) return presetId
        }
        return presetIdFor(entity, recommendedPresets(profile))
    }

    /**
     * The default layout for an entity type given a profile.
     * Returns the card ids in display order, minus hidden ones.
     */
    fun defaultLayoutIds(entity: CardEntityType, profile: LearnerProfile): List<String> {
        val presetId = presetIdFor(entity, recommendedPresets(profile))
        return layoutFor(entity, presetId)
    }

    /** The kanji preset recommended for a profile (used by KanjiEntry). */
    fun kanjiPresetFor(profile: LearnerProfile): String =
        recommendedPresets(profile).kanjiPresetId

    /** The word preset recommended for a profile (used by WordEntry). */
    fun wordPresetFor(profile: LearnerProfile): String =
        recommendedPresets(profile).wordPresetId

    private fun presetIdFor(entity: CardEntityType, presets: ProfileCardPresets): String =
        when (entity) {
            CardEntityType.Kanji -> presets.kanjiPresetId
            CardEntityType.Word -> presets.wordPresetId
            CardEntityType.Sentence -> presets.sentencePresetId
            CardEntityType.Grammar -> presets.grammarPresetId
            CardEntityType.Collection -> presets.collectionPresetId
        }

    /** Visible card ids for a preset id. */
    private fun layoutFor(entity: CardEntityType, presetId: String): List<String> = when (entity) {
        CardEntityType.Kanji -> KanjiCardPresets.byId(presetId)?.layout?.visibleCards()?.map { it.id }
            ?: KanjiCardLayout().visibleCards().map { it.id }
        CardEntityType.Word -> WordCardPresets.byId(presetId)?.layout?.visibleCards()?.map { it.id }
            ?: WordCardLayout().visibleCards().map { it.id }
        CardEntityType.Sentence -> SentenceCardPresets.byId(presetId)?.layout?.visibleCards()?.map { it.id }
            ?: SentenceCardLayout().visibleCards().map { it.id }
        CardEntityType.Grammar -> GrammarCardPresets.byId(presetId)?.layout?.visibleCards()?.map { it.id }
            ?: GrammarCardLayout().visibleCards().map { it.id }
        CardEntityType.Collection -> CollectionCardPresets.byId(presetId)?.layout?.visibleCards()?.map { it.id }
            ?: CollectionCardLayout().visibleCards().map { it.id }
    }
}
