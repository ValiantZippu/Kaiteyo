package io.kaiteyo.kjd.normalize

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JapaneseNormalizerTest {

    @Test
    fun katakanaConvertsToHiragana() {
        assertEquals("たべる", JapaneseNormalizer.katakanaToHiragana("タベル"))
        assertEquals("あいうえお", JapaneseNormalizer.katakanaToHiragana("アイウエオ"))
    }

    @Test
    fun hiraganaConvertsToKatakana() {
        assertEquals("タベル", JapaneseNormalizer.hiraganaToKatakana("たべる"))
    }

    @Test
    fun identityKeyUnifiesKanaVariants() {
        assertEquals(
            JapaneseNormalizer.identityKey("タベル"),
            JapaneseNormalizer.identityKey("たべる")
        )
        assertTrue(JapaneseNormalizer.kanaEquivalent("キャベツ", "きゃべつ"))
    }

    @Test
    fun searchKeyNormalizesUnicodeAndPunctuation() {
        assertEquals("たべる", JapaneseNormalizer.searchKey("タベル。"))
        assertEquals("eat", JapaneseNormalizer.searchKey(" Eat! "))
    }

    @Test
    fun scriptClassification() {
        assertTrue(JapaneseNormalizer.hasKanji("食べる"))
        assertFalse(JapaneseNormalizer.hasKanji("たべる"))
        assertTrue(JapaneseNormalizer.isKanaOnly("たべる"))
        assertTrue(JapaneseNormalizer.isKanaOnly("タベル"))
        assertFalse(JapaneseNormalizer.isKanaOnly("食べる"))
        assertTrue(JapaneseNormalizer.isJapaneseScript("食べる"))
        assertFalse(JapaneseNormalizer.isJapaneseScript("eat"))
    }
}
