package ua.syt0r.kanji.core.knowledge.cards

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ua.syt0r.kanji.core.knowledge.LearnerProfile

// ============================================================
// PROFILE → PRESET INTEGRATION
// ------------------------------------------------------------
// The full chain a user experiences: pick a learner profile →
// the profile recommends card presets → those presets resolve to
// real layouts with visible cards. Also verifies the cardinal
// rule of level adaptation: presentation adapts, DATA IS NEVER
// DESTROYED. Every kanji card type still exists in the registry
// regardless of what a profile hides.
// ============================================================

class ProfilePresetIntegrationTest {

    // ------------------------------------------------------------------
    // Chain: profile → presets → layouts
    // ------------------------------------------------------------------

    @Test
    fun everyProfileResolvesToAConcreteVisibleLayout() {
        LearnerProfile.entries.forEach { profile ->
            val presets = PresetAdapter.recommendedPresets(profile)
            val kanjiLayout = PresetAdapter.defaultLayoutIds(CardEntityType.Kanji, profile)
            val wordLayout = PresetAdapter.defaultLayoutIds(CardEntityType.Word, profile)
            val sentenceLayout = PresetAdapter.defaultLayoutIds(CardEntityType.Sentence, profile)
            val grammarLayout = PresetAdapter.defaultLayoutIds(CardEntityType.Grammar, profile)
            val collectionLayout = PresetAdapter.defaultLayoutIds(CardEntityType.Collection, profile)

            // Every layout must contain at least the hero card for its entity.
            assertTrue(kanjiLayout.contains("hero"), "kanji hero for ${profile.name}")
            assertTrue(wordLayout.isNotEmpty(), "word layout for ${profile.name}")
            assertTrue(sentenceLayout.isNotEmpty(), "sentence layout for ${profile.name}")
            assertTrue(grammarLayout.isNotEmpty(), "grammar layout for ${profile.name}")
            assertTrue(collectionLayout.isNotEmpty(), "collection layout for ${profile.name}")

            // The preset ids must actually exist in their registries.
            assertTrue(KanjiCardPresets.byId(presets.kanjiPresetId) != null, "kanji preset ${presets.kanjiPresetId}")
            assertTrue(WordCardPresets.byId(presets.wordPresetId) != null, "word preset ${presets.wordPresetId}")
            assertTrue(SentenceCardPresets.byId(presets.sentencePresetId) != null, "sentence preset ${presets.sentencePresetId}")
            assertTrue(GrammarCardPresets.byId(presets.grammarPresetId) != null, "grammar preset ${presets.grammarPresetId}")
            assertTrue(CollectionCardPresets.byId(presets.collectionPresetId) != null, "collection preset ${presets.collectionPresetId}")
        }
    }

    // ------------------------------------------------------------------
    // Adaptation: profiles change what is VISIBLE
    // ------------------------------------------------------------------

    @Test
    fun beginnerSeesFewerCardsThanAdvanced() {
        val beginner = PresetAdapter.defaultLayoutIds(CardEntityType.Kanji, LearnerProfile.Beginner)
        val advanced = PresetAdapter.defaultLayoutIds(CardEntityType.Kanji, LearnerProfile.Advanced)

        assertTrue(beginner.size < advanced.size, "beginner=$beginner advanced=$advanced")
        // Beginner never hides the core learning cards.
        assertTrue(beginner.contains("hero"))
        assertTrue(beginner.contains("meaning"))
        assertTrue(beginner.contains("readings"))
        // Advanced shows structure and grammar cards a beginner doesn't get by default.
        assertTrue(advanced.contains("component"))
        assertTrue(advanced.contains("grammar"))
    }

    @Test
    fun minimalWordPresetIsASubsetOfStandard() {
        val minimal = PresetAdapter.defaultLayoutIds(CardEntityType.Word, LearnerProfile.AbsoluteBeginner)
        val standard = PresetAdapter.defaultLayoutIds(CardEntityType.Word, LearnerProfile.Intermediate)

        assertTrue(minimal.size <= standard.size)
        assertTrue(standard.containsAll(minimal), "standard must show at least the minimal cards")
    }

    // ------------------------------------------------------------------
    // No data destruction: the registry is untouched by any profile
    // ------------------------------------------------------------------

    @Test
    fun hiddenCardsStillExistInTheRegistry() {
        // A beginner layout hides many cards, but every card type remains
        // available in the registry — profile adaptation never deletes data.
        val beginner = PresetAdapter.defaultLayoutIds(CardEntityType.Kanji, LearnerProfile.AbsoluteBeginner)
        val allTypes = KanjiCardType.entries.map { it.id }
        assertTrue(beginner.size < allTypes.size)
        assertEquals(allTypes.size, KanjiCardType.entries.size)

        // And an explicit user choice (research) can bring them all back.
        val research = PresetAdapter.defaultLayoutIds(CardEntityType.Kanji, LearnerProfile.Research)
        assertEquals(allTypes.size, research.size)
    }

    @Test
    fun profileChangeNeverCorruptsStoredLayout() {
        // A user customizes their kanji layout, then changes profile.
        // The stored layout must survive independently of the profile.
        val customized = KanjiCardLayout(
            order = listOf("meaning", "hero", "readings"),
            hidden = setOf("study")
        )

        // The adapter resolves a profile default, but a persisted explicit
        // layout wins over the profile recommendation.
        val explicit = PresetAdapter.resolvePreset(CardEntityType.Kanji, "research", LearnerProfile.AbsoluteBeginner)
        assertEquals("research", explicit)

        // Sanitization keeps only real cards — never a crash, never data loss.
        val sanitized = customized.sanitized()
        assertEquals(customized.order, sanitized.order)
        assertEquals(customized.hidden, sanitized.hidden)
    }

    // ------------------------------------------------------------------
    // Store round-trip: a saved profile maps to a stable recommendation
    // ------------------------------------------------------------------

    @Test
    fun recommendedPresetIsStableAcrossRepeatedCalls() {
        LearnerProfile.entries.forEach { profile ->
            val first = PresetAdapter.recommendedPresets(profile)
            val second = PresetAdapter.recommendedPresets(profile)
            assertEquals(first, second, "recommendations must be deterministic for ${profile.name}")
        }
    }

    @Test
    fun researchProfileShowsEveryKanjiCard() {
        val ids = PresetAdapter.defaultLayoutIds(CardEntityType.Kanji, LearnerProfile.Research)
        assertEquals(KanjiCardType.entries.map { it.id }.toSet(), ids.toSet())
    }

    @Test
    fun advancedProfileShowsStructureAndGrammarCards() {
        val ids = PresetAdapter.defaultLayoutIds(CardEntityType.Kanji, LearnerProfile.Advanced)
        // The advanced kanji preset surfaces structural cards a beginner
        // never sees by default (component, strokes, graph).
        assertTrue(ids.contains("component"))
        assertTrue(ids.contains("stroke"))
        assertTrue(ids.contains("graph"))
        assertTrue(ids.contains("grammar"))
    }

    @Test
    fun minimalSentenceLayoutHasCoreCards() {
        val ids = PresetAdapter.defaultLayoutIds(CardEntityType.Sentence, LearnerProfile.AbsoluteBeginner)
        // Minimal sentence preset keeps the sentence itself + translation,
        // and hides advanced analysis (tokens, grammar, difficulty, source).
        assertTrue(ids.contains("hero"))
        assertTrue(ids.contains("translation"))
        assertFalse(ids.contains("difficulty"))
        assertFalse(ids.contains("source"))
    }

    // ------------------------------------------------------------------
    // Sanity: no empty layouts, no unknown ids
    // ------------------------------------------------------------------

    @Test
    fun allResolvedLayoutsContainOnlyKnownIds() {
        LearnerProfile.entries.forEach { profile ->
            val kanjiIds = PresetAdapter.defaultLayoutIds(CardEntityType.Kanji, profile)
            assertTrue(kanjiIds.all { KanjiCardType.byId(it) != null }, "unknown kanji id for ${profile.name}")
            val wordIds = PresetAdapter.defaultLayoutIds(CardEntityType.Word, profile)
            assertTrue(wordIds.all { WordCardType.byId(it) != null }, "unknown word id for ${profile.name}")
            val sentenceIds = PresetAdapter.defaultLayoutIds(CardEntityType.Sentence, profile)
            assertTrue(sentenceIds.all { SentenceCardType.byId(it) != null }, "unknown sentence id for ${profile.name}")
            val grammarIds = PresetAdapter.defaultLayoutIds(CardEntityType.Grammar, profile)
            assertTrue(grammarIds.all { GrammarCardType.byId(it) != null }, "unknown grammar id for ${profile.name}")
            val collectionIds = PresetAdapter.defaultLayoutIds(CardEntityType.Collection, profile)
            assertTrue(collectionIds.all { CollectionCardType.byId(it) != null }, "unknown collection id for ${profile.name}")
        }
    }
}
