package ua.syt0r.kanji.core.knowledge.level

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ua.syt0r.kanji.core.app_data.data.toFurigana
import ua.syt0r.kanji.core.knowledge.ExplanationDepth
import ua.syt0r.kanji.core.knowledge.LearnerProfile
import ua.syt0r.kanji.core.knowledge.LearnerProfileCatalog
import ua.syt0r.kanji.core.knowledge.ProfilePresentation
import ua.syt0r.kanji.core.knowledge.SentenceDifficulty
import ua.syt0r.kanji.core.knowledge.SentenceKnowledge
import ua.syt0r.kanji.core.knowledge.WordKnowledge

/**
 * Level-adapter tests (spec §23–§24): presentation adaptation is pure and
 * never mutates source data — glossary senses are limited by depth,
 * sentence difficulty follows the profile bound, and Custom overrides are
 * honored only when the profile is Custom.
 */
class LevelAdapterTest {

    private val word = WordKnowledge(
        id = 1L,
        kanjiReading = "食べる",
        kanaReading = "たべる",
        furigana = "食べる".toFurigana(),
        glossary = listOf("to eat", "to live on", "to consume"),
        partOfSpeech = listOf("Ichidan verb")
    )

    private fun sentence(text: String, translation: String = "") =
        SentenceKnowledge(text = text, translation = translation, furigana = text.toFurigana())

    // ---------------------------------------------------------------
    // Glossary depth
    // ---------------------------------------------------------------

    @Test
    fun simpleDepthShowsOnlyTheFirstSense() {
        val presentation = ProfilePresentation(explanationDepth = ExplanationDepth.Simple)
        val adapted = LevelAdapter.adaptedGlossary(word, presentation)
        assertEquals(listOf("to eat"), adapted)
    }

    @Test
    fun clearDepthShowsThreeSenses() {
        val presentation = ProfilePresentation(explanationDepth = ExplanationDepth.Clear)
        assertEquals(3, LevelAdapter.adaptedGlossary(word, presentation).size)
    }

    @Test
    fun technicalDepthShowsEverything() {
        val presentation = ProfilePresentation(explanationDepth = ExplanationDepth.Technical)
        assertEquals(word.glossary, LevelAdapter.adaptedGlossary(word, presentation))
    }

    // ---------------------------------------------------------------
    // Visibility flags
    // ---------------------------------------------------------------

    @Test
    fun absoluteBeginnerShowsFuriganaRomajiAndTranslations() {
        val presentation = LearnerProfileCatalog.defaultsFor(LearnerProfile.AbsoluteBeginner)
        assertTrue(LevelAdapter.showFurigana(presentation))
        assertTrue(LevelAdapter.showRomaji(presentation))
        assertTrue(LevelAdapter.showTranslations(presentation))
    }

    @Test
    fun nativeHidesTranslationsAndRomaji() {
        val presentation = LearnerProfileCatalog.defaultsFor(LearnerProfile.Native)
        assertFalse(LevelAdapter.showTranslations(presentation))
        assertFalse(LevelAdapter.showRomaji(presentation))
    }

    // ---------------------------------------------------------------
    // Sentence difficulty filtering
    // ---------------------------------------------------------------

    @Test
    fun easyProfileKeepsOnlyEasySentences() {
        val presentation = ProfilePresentation(sentenceDifficulty = SentenceDifficulty.Easy)
        val easy = sentence("これは本です。")
        val hard = sentence("彼は複雑な経済理論を研究し続けている。")
        val adapted = LevelAdapter.adaptedSentences(listOf(easy, hard), presentation)
        assertEquals(listOf("これは本です。"), adapted.map { it.text })
    }

    @Test
    fun hardProfileKeepsEverything() {
        val presentation = ProfilePresentation(sentenceDifficulty = SentenceDifficulty.Hard)
        val hard = sentence("彼は複雑な経済理論を研究し続けている。")
        val adapted = LevelAdapter.adaptedSentences(listOf(hard), presentation)
        assertEquals(1, adapted.size)
    }

    @Test
    fun adaptedSentencesRespectsTheLimit() {
        val presentation = ProfilePresentation(sentenceDifficulty = SentenceDifficulty.Hard)
        val many = (1..10).map { sentence("これは本です。$it") }
        assertEquals(4, LevelAdapter.adaptedSentences(many, presentation, limit = 4).size)
    }

    // ---------------------------------------------------------------
    // Effective presentation resolution
    // ---------------------------------------------------------------

    @Test
    fun effectivePresentationHonorsCustomOverridesOnlyForCustom() {
        val overrides = ProfilePresentation(showRomaji = true, explanationDepth = ExplanationDepth.Technical)
        assertEquals(overrides, LevelAdapter.effectivePresentation(LearnerProfile.Custom, overrides))
        // Non-custom profiles ignore stored overrides.
        assertEquals(
            LearnerProfileCatalog.defaultsFor(LearnerProfile.Beginner),
            LevelAdapter.effectivePresentation(LearnerProfile.Beginner, overrides)
        )
    }

    @Test
    fun everyProfileResolvesToACatalogDefault() {
        for (profile in LearnerProfile.entries) {
            val presentation = LevelAdapter.presentationFor(profile)
            assertEquals(profile, LearnerProfileCatalog.byId(profile.id))
            assertTrue(presentation.sentenceDifficulty != null)
        }
    }
}
