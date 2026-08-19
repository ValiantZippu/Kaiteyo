package ua.syt0r.kanji.core.knowledge.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import ua.syt0r.kanji.core.knowledge.LearnerProfile
import kotlin.test.Test

class PresetAdapterTest {

    // ------------------------------------------------------------------
    // Recommended presets per profile
    // ------------------------------------------------------------------

    @Test
    fun beginnerUsesSimplePresets() {
        val presets = PresetAdapter.recommendedPresets(LearnerProfile.AbsoluteBeginner)
        assertEquals("beginner", presets.kanjiPresetId)
        assertEquals("minimal", presets.wordPresetId)
        assertEquals("minimal", presets.sentencePresetId)
    }

    @Test
    fun intermediateUsesStandardPresets() {
        val presets = PresetAdapter.recommendedPresets(LearnerProfile.Intermediate)
        assertEquals("standard", presets.kanjiPresetId)
        assertEquals("standard", presets.wordPresetId)
        assertEquals("standard", presets.sentencePresetId)
    }

    @Test
    fun advancedUsesAdvancedPresets() {
        val presets = PresetAdapter.recommendedPresets(LearnerProfile.Advanced)
        assertEquals("advanced", presets.kanjiPresetId)
        assertEquals("advanced", presets.grammarPresetId)
        assertEquals("advanced", presets.collectionPresetId)
    }

    @Test
    fun nativeUsesResearchPresets() {
        val presets = PresetAdapter.recommendedPresets(LearnerProfile.Native)
        assertEquals("research", presets.kanjiPresetId)
        assertEquals("research", presets.wordPresetId)
        assertEquals("research", presets.sentencePresetId)
    }

    @Test
    fun allProfilesResolveToExistingPresets() {
        LearnerProfile.entries.forEach { profile ->
            val presets = PresetAdapter.recommendedPresets(profile)
            assertTrue("kanji preset exists for ${profile.name}", KanjiCardPresets.byId(presets.kanjiPresetId) != null)
            assertTrue("word preset exists for ${profile.name}", WordCardPresets.byId(presets.wordPresetId) != null)
            assertTrue("sentence preset exists for ${profile.name}", SentenceCardPresets.byId(presets.sentencePresetId) != null)
            assertTrue("grammar preset exists for ${profile.name}", GrammarCardPresets.byId(presets.grammarPresetId) != null)
            assertTrue("collection preset exists for ${profile.name}", CollectionCardPresets.byId(presets.collectionPresetId) != null)
        }
    }

    // ------------------------------------------------------------------
    // Resolve preset
    // ------------------------------------------------------------------

    @Test
    fun resolveValidPresetReturnsIt() {
        val result = PresetAdapter.resolvePreset(CardEntityType.Kanji, "research", LearnerProfile.Beginner)
        assertEquals("research", result)
    }

    @Test
    fun resolveUnknownPresetFallsBackToProfile() {
        val result = PresetAdapter.resolvePreset(CardEntityType.Kanji, "nonexistent", LearnerProfile.Advanced)
        assertEquals("advanced", result)
    }

    @Test
    fun resolveNullPresetFallsBackToProfile() {
        val result = PresetAdapter.resolvePreset(CardEntityType.Word, null, LearnerProfile.AbsoluteBeginner)
        assertEquals("minimal", result)
    }

    @Test
    fun resolveChecksEntitySpecificPresets() {
        // "beginner" exists for kanji — resolved as-is even for advanced.
        val result = PresetAdapter.resolvePreset(CardEntityType.Kanji, "beginner", LearnerProfile.Advanced)
        assertEquals("beginner", result)
    }

    // ------------------------------------------------------------------
    // Default layout ids
    // ------------------------------------------------------------------

    @Test
    fun beginnerKanjiLayoutIsCompact() {
        val ids = PresetAdapter.defaultLayoutIds(CardEntityType.Kanji, LearnerProfile.Beginner)
        // Beginner kanji preset hides some cards — fewer than full.
        assertTrue(ids.size < KanjiCardType.entries.size)
    }

    @Test
    fun researchShowsEverything() {
        val ids = PresetAdapter.defaultLayoutIds(CardEntityType.Kanji, LearnerProfile.Research)
        assertEquals(KanjiCardType.entries.size, ids.size)
    }

    @Test
    fun intermediateWordLayoutIsStandard() {
        val ids = PresetAdapter.defaultLayoutIds(CardEntityType.Word, LearnerProfile.Intermediate)
        assertTrue(ids.contains("hero"))
        assertTrue(ids.contains("meanings"))
    }

    // ------------------------------------------------------------------
    // Convenience helpers
    // ------------------------------------------------------------------

    @Test
    fun kanjiPresetForProfile() {
        assertEquals("beginner", PresetAdapter.kanjiPresetFor(LearnerProfile.Beginner))
        assertEquals("advanced", PresetAdapter.kanjiPresetFor(LearnerProfile.Advanced))
    }

    @Test
    fun wordPresetForProfile() {
        assertEquals("minimal", PresetAdapter.wordPresetFor(LearnerProfile.AbsoluteBeginner))
        assertEquals("research", PresetAdapter.wordPresetFor(LearnerProfile.Research))
    }
}
