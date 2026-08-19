package ua.syt0r.kanji.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [SentenceDifficultyScorer] (spec §26–§27): the deterministic,
 * surface-feature difficulty estimate used to filter example sentences.
 * No ML, no fabricated provenance — pure length / kanji-density / grammar /
 * known-kanji scoring, labeled as an estimate.
 */
class SentenceDifficultyTest {

    @Test
    fun blankSentenceIsMinimalDifficulty() {
        val result = SentenceDifficultyScorer.score("")
        assertEquals(SentenceDifficultyScorer.MIN_LEVEL, result.level)
        assertEquals(SentenceDifficultyTier.VeryEasy, result.tier)
    }

    @Test
    fun shortKanaOnlySentenceScoresVeryEasy() {
        val result = SentenceDifficultyScorer.score("あ")
        assertEquals(1, result.level)
        assertEquals(SentenceDifficultyTier.VeryEasy, result.tier)
        assertTrue(result.factors.isEmpty())
    }

    @Test
    fun kanjiDensityRaisesLevel() {
        // Single kanji "食": density 1.0 -> high-density band, no length/grammar/overlay.
        val result = SentenceDifficultyScorer.score("食")
        assertEquals(4, result.level)
        assertEquals(SentenceDifficultyTier.Easy, result.tier)
        assertContains(result.factors, "high kanji density")
    }

    @Test
    fun unknownKanjiOverlayRaisesLevelAndExplains() {
        // 3 distinct kanji, no length/grammar influence (chars <= 6).
        val noOverlay = SentenceDifficultyScorer.score("食飲学")
        val withOverlay = SentenceDifficultyScorer.score(
            sentence = "食飲学",
            knownKanji = setOf("犬", "牛")
        )
        val allKnown = SentenceDifficultyScorer.score(
            sentence = "食飲学",
            knownKanji = setOf("食", "飲", "学")
        )

        // Unknown kanji push the level up by one band.
        assertTrue(withOverlay.level > noOverlay.level)
        assertContains(withOverlay.factors, "3 kanji not in your study set")
        // When every kanji is known the overlay adds nothing.
        assertEquals(noOverlay.level, allKnown.level)
    }

    @Test
    fun longSentenceAddsExplanatoryFactor() {
        // 21 hiragana, no kanji -> length factor only.
        val long = "あいうえおかきくけこさしすせそたちつてとと"
        val result = SentenceDifficultyScorer.score(long)
        assertContains(result.factors, "long sentence")
        assertEquals(4, result.level)
    }

    @Test
    fun grammarDensityRaisesLevelAndExplains() {
        val base = SentenceDifficultyScorer.score("食飲学")
        val withGrammar = SentenceDifficultyScorer.score(
            sentence = "食飲学",
            grammarMatchCount = 3
        )
        assertTrue(withGrammar.level > base.level)
        assertContains(withGrammar.factors, "3 grammar patterns")
    }

    @Test
    fun maximalInputClampsToVeryHard() {
        // Long (33 chars) + high kanji density + heavy grammar + many unknown
        // kanji together saturate and clamp to the 1..10 scale.
        val result = SentenceDifficultyScorer.score(
            sentence = "今日日本語学校勉強先生今日日本語学校勉強先生今日日本語学校勉強先生",
            grammarMatchCount = 5,
            knownKanji = setOf("犬")
        )
        assertEquals(SentenceDifficultyScorer.MAX_LEVEL, result.level)
        assertEquals(SentenceDifficultyTier.VeryHard, result.tier)
        assertContains(result.factors, "5 grammar patterns")
    }

    @Test
    fun acceptabilityRespectsDifficultyPreference() {
        fun level(value: Int) = SentenceDifficultyLevel(value, SentenceDifficultyTier.Hard, "Hard")

        // Easy profile accepts up to 4.
        assertTrue(SentenceDifficultyScorer.acceptableForDifficulty(level(4), SentenceDifficulty.Easy))
        assertFalse(SentenceDifficultyScorer.acceptableForDifficulty(level(5), SentenceDifficulty.Easy))
        // Mixed accepts up to 7.
        assertTrue(SentenceDifficultyScorer.acceptableForDifficulty(level(7), SentenceDifficulty.Mixed))
        assertFalse(SentenceDifficultyScorer.acceptableForDifficulty(level(8), SentenceDifficulty.Mixed))
        // Hard accepts everything.
        assertTrue(SentenceDifficultyScorer.acceptableForDifficulty(level(10), SentenceDifficulty.Hard))
    }
}
