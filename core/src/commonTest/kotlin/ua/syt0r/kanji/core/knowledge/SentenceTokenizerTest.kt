package ua.syt0r.kanji.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SentenceTokenizerTest {

    @Test
    fun `kanji run is a single Kanji token`() {
        val tokens = SentenceTokenizer.tokenize("漢字")
        assertEquals(1, tokens.size)
        assertEquals("漢字", tokens[0].text)
        assertEquals(SentenceTokenKind.Kanji, tokens[0].kind)
    }

    @Test
    fun `mixed kanji plus kana run is a Mixed token`() {
        val tokens = SentenceTokenizer.tokenize("食べる")
        assertEquals(1, tokens.size)
        assertEquals("食べる", tokens[0].text)
        assertEquals(SentenceTokenKind.Mixed, tokens[0].kind)
    }

    @Test
    fun `kana run is a Kana token`() {
        val tokens = SentenceTokenizer.tokenize("ます")
        assertEquals(1, tokens.size)
        assertEquals("ます", tokens[0].text)
        assertEquals(SentenceTokenKind.Kana, tokens[0].kind)
    }

    @Test
    fun `punctuation is split out`() {
        val tokens = SentenceTokenizer.tokenize("勉強する。")
        val kinds = tokens.map { it.kind }
        assertTrue(SentenceTokenKind.Punctuation in kinds)
        assertEquals("。", tokens.last().text)
    }

    @Test
    fun `round trip reassembles the sentence`() {
        val sentence = "今日は日本語を勉強します。"
        val tokens = SentenceTokenizer.tokenize(sentence)
        assertEquals(sentence, SentenceTokenizer.join(tokens))
    }

    @Test
    fun `blank sentence yields no tokens`() {
        assertTrue(SentenceTokenizer.tokenize("   ").isEmpty())
    }
}

class SentenceDifficultyTest {

    @Test
    fun `blank sentence is very easy`() {
        val score = SentenceDifficultyScorer.score("")
        assertEquals(SentenceDifficultyTier.VeryEasy, score.tier)
    }

    @Test
    fun `short kana sentence scores low`() {
        val score = SentenceDifficultyScorer.score("はい。")
        assertTrue(score.level <= 3)
    }

    @Test
    fun `dense kanji sentence scores high`() {
        val score = SentenceDifficultyScorer.score("漢字情報処理技術者の資格試験に合格した")
        assertTrue(score.level >= 5)
    }

    @Test
    fun `known kanji overlay lowers difficulty`() {
        val hard = "漢字情報処理技術者の資格試験"
        val unknown = SentenceDifficultyScorer.score(hard)
        // A learner who knows all kanji in the sentence should never be scored harder
        // than the same sentence scored without the known-set overlay.
        val knownKanji = setOf("漢", "字", "情", "報", "処", "理", "技", "術", "者", "資", "格", "試", "験")
        val known = SentenceDifficultyScorer.score(hard, knownKanji = knownKanji)
        assertTrue(known.level <= unknown.level)
        assertTrue(known.factors.none { it.contains("not in your study set") })
    }

    @Test
    fun `acceptableFor respects profile bounds`() {
        val hard = SentenceDifficultyLevel(8, SentenceDifficultyTier.Hard, "Hard")
        assertTrue(!SentenceDifficultyScorer.acceptableFor(hard, LearnerProfile.AbsoluteBeginner))
        assertTrue(SentenceDifficultyScorer.acceptableFor(hard, LearnerProfile.Advanced))
    }
}
