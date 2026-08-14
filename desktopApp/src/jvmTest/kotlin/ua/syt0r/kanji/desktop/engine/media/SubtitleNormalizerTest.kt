package ua.syt0r.kanji.desktop.engine.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubtitleNormalizerTest {

    // ------------------------------------------------------------
    // ASS/SSA stripping
    // ------------------------------------------------------------

    @Test
    fun assOverrideBlocksAreStripped() {
        assertEquals("こんにちは", SubtitleNormalizer.stripAssTags("""{\k20}こんにちは{\r}"""))
        assertEquals("line one\nline two", SubtitleNormalizer.stripAssTags("""line one\Nline two"""))
        assertEquals("あ い", SubtitleNormalizer.stripAssTags("""あ\hい"""))
    }

    @Test
    fun htmlTagsAreStripped() {
        assertEquals("テスト", SubtitleNormalizer.stripHtml("<i>テスト</i>"))
        assertEquals("abc", SubtitleNormalizer.stripHtml("<b><u>abc</u></b>"))
    }

    @Test
    fun rubyAnnotationsCollapseToTheKanji() {
        assertEquals("漢字", SubtitleNormalizer.stripRuby("<ruby>漢字<rt>かんじ</rt></ruby>"))
        assertEquals("漢字と仮名", SubtitleNormalizer.stripRuby("<ruby>漢字<rt>かんじ</rt></ruby>と仮名"))
    }

    // ------------------------------------------------------------
    // Entity decoding
    // ------------------------------------------------------------

    @Test
    fun decimalEntitiesDecode() {
        assertEquals("こんにちは", SubtitleNormalizer.decodeEntities("&#12371;&#12393;&#12395;&#12385;&#12399;"))
    }

    @Test
    fun hexEntitiesDecode() {
        assertEquals("漢字", SubtitleNormalizer.decodeEntities("&#x6f22;&#x5b57;"))
    }

    @Test
    fun namedEntitiesDecode() {
        assertEquals("a & b", SubtitleNormalizer.decodeEntities("a &amp; b"))
        assertEquals("A B", SubtitleNormalizer.decodeEntities("A&nbsp;B"))
    }

    @Test
    fun unknownEntitiesAreLeftAlone() {
        assertEquals("&bogus;", SubtitleNormalizer.decodeEntities("&bogus;"))
    }

    // ------------------------------------------------------------
    // Speaker labels + furigana
    // ------------------------------------------------------------

    @Test
    fun speakerLabelsAreStripped() {
        assertEquals("おはよう", SubtitleNormalizer.stripSpeakerLabel("花子: おはよう"))
        assertEquals("おはよう", SubtitleNormalizer.stripSpeakerLabel("Hana:おはよう"))
    }

    @Test
    fun furiganaBracketsAfterKanjiAreCollapsed() {
        assertEquals("漢字", SubtitleNormalizer.stripFurigana("漢字[かんじ]"))
        assertEquals("漢字と仮名", SubtitleNormalizer.stripFurigana("漢字[かんじ]と仮名"))
    }

    @Test
    fun nonKanaBracketsSurvive() {
        // Stage directions / song annotations are NOT kana — keep them.
        assertEquals("[whispering]おはよう", SubtitleNormalizer.stripFurigana("[whispering]おはよう"))
    }

    // ------------------------------------------------------------
    // Full pipeline
    // ------------------------------------------------------------

    @Test
    fun normalizeProducesCleanLookupText() {
        val raw = """{\an8}{\k10}今日はいい天気ですね{\r}"""
        assertEquals("今日はいい天気ですね", SubtitleNormalizer.normalizeForLookup(raw))
    }

    @Test
    fun normalizeCollapsesWhitespace() {
        assertEquals("こんにちは 世界", SubtitleNormalizer.normalizeForLookup("こんにちは　 世界"))
    }

    // ------------------------------------------------------------
    // Japanese detection
    // ------------------------------------------------------------

    @Test
    fun japaneseDetection() {
        assertTrue(SubtitleNormalizer.isMostlyJapanese("こんにちは、元気ですか？"))
        assertTrue(SubtitleNormalizer.isMostlyJapanese("日本語の字幕です"))
        assertFalse(SubtitleNormalizer.isMostlyJapanese("Hello world, this is English"))
    }
}
