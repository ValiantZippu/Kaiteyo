package ua.syt0r.kanji.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Normalization tests (KT-SEARCH-005, spec §136–§140): width folding,
 * script folding, romaji round-trips, wildcards and the one-stop matcher.
 * The normalizer is pure and deterministic, so these tests are exhaustive
 * of the documented behavior.
 */
class JapaneseTextNormalizerTest {

    // ---------------------------------------------------------------
    // Width + case folding
    // ---------------------------------------------------------------

    @Test
    fun fullWidthAsciiFoldsToHalfWidth() {
        assertEquals("taberu4", "ｔａｂｅｒｕ４".normalizeForSearch())
        assertEquals("hello", "ＨＥＬＬＯ".normalizeForSearch())
    }

    @Test
    fun halfWidthKatakanaFoldsToFull() {
        assertEquals("カタカナ", "ｶﾀｶﾅ".normalizeForSearch())
    }

    @Test
    fun katakanaFoldsToHiraganaForMatching() {
        assertEquals("たべる", "タベル".normalizeForSearch())
        assertEquals("たべる", "たべる".normalizeForSearch())
    }

    @Test
    fun prolongedSoundMarkDropped() {
        // ー is a reading hint, dropped for matching (こーり → こり).
        assertEquals("こり", "こーり".normalizeForSearch())
        // Katakana folds to hiragana and ー is dropped (スーパー → すぱ).
        assertEquals("すぱ", "スーパー".normalizeForSearch())
    }

    @Test
    fun kanjiUntouchedByNormalization() {
        assertEquals("食べる", "食べる".normalizeForSearch())
        assertEquals("漢字", "漢字".normalizeForSearch())
    }

    @Test
    fun mixedStringNormalizesPerChar() {
        assertEquals("たべる4", "タベル４".normalizeForSearch())
    }

    // ---------------------------------------------------------------
    // Kana → romaji
    // ---------------------------------------------------------------

    @Test
    fun kanaToRomajiBasic() {
        assertEquals("taberu", "たべる".kanaToRomaji())
        assertEquals("nihon", "にほん".kanaToRomaji())
        assertEquals("shi", "し".kanaToRomaji())
        // Small っ maps to its base reading "tsu" (documented limit —
        // geminate consonants are not collapsed in this direction).
        assertEquals("satsuka", "さっか".kanaToRomaji())
    }

    @Test
    fun kanaToRomajiMixedKeepsKanji() {
        // Kanji is preserved, not transliterated (no reading here).
        assertEquals("食beru", "食べる".kanaToRomaji())
    }

    @Test
    fun kanaToRomajiLatinPassesThrough() {
        assertEquals("abc", "abc".kanaToRomaji())
    }

    @Test
    fun kanaToRomajiProlongedMarkDoesNotCrash() {
        // ー has no syllable reading — it must pass through, not throw.
        assertEquals("suーpaー", "スーパー".kanaToRomaji())
    }

    // ---------------------------------------------------------------
    // Romaji → hiragana
    // ---------------------------------------------------------------

    @Test
    fun romajiToHiraganaColumns() {
        assertEquals("たべる", "taberu".romajiToHiragana())
        assertEquals("にほんご", "nihongo".romajiToHiragana())
        assertEquals("きょうと", "kyouto".romajiToHiragana())
    }

    @Test
    fun romajiToHiraganaDigraphs() {
        assertEquals("しゃしん", "shashin".romajiToHiragana())
        assertEquals("ちゃ", "cha".romajiToHiragana())
        assertEquals("きゃ", "kya".romajiToHiragana())
    }

    @Test
    fun romajiToHiraganaSokuon() {
        assertEquals("さっか", "sakka".romajiToHiragana())
        assertEquals("がっこう", "gakkou".romajiToHiragana())
    }

    @Test
    fun romajiToHiraganaN() {
        assertEquals("ん", "n".romajiToHiragana())
        assertEquals("かんじ", "kanji".romajiToHiragana())
    }

    @Test
    fun romajiToHiraganaUnknownAsciiStays() {
        assertEquals("xたべる", "xtaberu".romajiToHiragana())
    }

    // ---------------------------------------------------------------
    // Wildcards
    // ---------------------------------------------------------------

    @Test
    fun wildcardStarMatchesAnyRun() {
        assertTrue(WildcardPattern.compile("食*").matches("食べ物"))
        assertTrue(WildcardPattern.compile("*食べ*").matches("私は食べ物が好き"))
        assertTrue(WildcardPattern.compile("た*る").matches("たべる"))
        assertFalse(WildcardPattern.compile("た*る").matches("たべ"))
    }

    @Test
    fun wildcardQuestionMarkMatchesExactlyOne() {
        assertTrue(WildcardPattern.compile("た?る").matches("たべる"))
        assertFalse(WildcardPattern.compile("た?る").matches("たる"))
        assertFalse(WildcardPattern.compile("た?る").matches("たべるよ"))
    }

    @Test
    fun wildcardAnchorsAtEdges() {
        assertTrue(WildcardPattern.compile("*る").matches("たべる"))
        assertFalse(WildcardPattern.compile("た*").matches("べる"))
        assertTrue(WildcardPattern.compile("*べる").matches("たべる"))
    }

    @Test
    fun wildcardIsNormalizationAware() {
        // Full-width pattern vs half-width candidate.
        assertTrue(WildcardPattern.compile("ｔａ*").matches("taberu"))
        // Case-insensitive.
        assertTrue(WildcardPattern.compile("TABE*").matches("taberu"))
        // Kana-script-insensitive.
        assertTrue(WildcardPattern.compile("*タベ*").matches("たべる"))
    }

    @Test
    fun plainPatternFastPathIsContains() {
        assertTrue(WildcardPattern.compile("べる").matches("たべる"))
        assertFalse(WildcardPattern.compile("食べ").matches("たべる"))
    }

    @Test
    fun containsWildcardDetectsOnlyWildcards() {
        assertTrue("食*".containsWildcard())
        assertTrue("た?る".containsWildcard())
        assertFalse("食べる".containsWildcard())
        assertFalse("".containsWildcard())
    }

    // ---------------------------------------------------------------
    // One-stop matcher
    // ---------------------------------------------------------------

    @Test
    fun queryMatchesNormalizesBothSides() {
        assertTrue(queryMatches("タベル", "たべる"))
        assertTrue(queryMatches("taberu", "タベル"))
        assertTrue(queryMatches("TABE", "たべる"))
        assertTrue(queryMatches("食べ", "食べる"))
    }

    @Test
    fun queryMatchesBlankIsFalse() {
        assertFalse(queryMatches("", "たべる"))
        assertFalse(queryMatches("   ", "たべる"))
    }

    @Test
    fun queryMatchesWildcardRoute() {
        assertTrue(queryMatches("た*る", "たべる"))
        assertTrue(queryMatches("*物", "食べ物"))
    }
}
