package ua.syt0r.kanji.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import ua.syt0r.kanji.core.app_data.data.toFurigana

/**
 * WordSegmenter tests (spec §136–§137) over a canned [SegmentWordLookup]:
 * longest-match segmentation resolves real compounds to single word tokens
 * (日本語, 食べる), leaves particles and unmatched kana as kana segments, and
 * falls back to per-character kanji when no compound exists. The lookup is
 * the only DB touch point — the default adapter over KnowledgeRepository is
 * thin and covered by integration.
 */
class WordSegmenterTest {

    private fun word(
        id: Long,
        kanjiReading: String?,
        kanaReading: String,
        glossary: String = "meaning"
    ) = WordKnowledge(
        id = id,
        kanjiReading = kanjiReading,
        kanaReading = kanaReading,
        furigana = kanaReading.toFurigana(),
        glossary = listOf(glossary)
    )

    /** Canned dictionary: longest-form-wins per spelling. */
    private class FakeLookup(
        private val words: List<WordKnowledge>
    ) : SegmentWordLookup {

        private val byKanji = words.filter { it.kanjiReading != null }
        private val byKana = words.filter { it.kanjiReading == null }

        override suspend fun wordsWithText(text: String, limit: Int): List<WordKnowledge> {
            val matches = buildList {
                addAll(byKanji.filter { it.kanjiReading!!.startsWith(text) })
                addAll(byKana.filter { it.kanaReading.startsWith(text) })
            }
            return matches.distinctBy { it.id }.take(limit)
        }

        override suspend fun kanjiForCharacters(characters: Collection<String>): List<KanjiKnowledge> =
            characters.mapNotNull { c ->
                words.firstOrNull { it.kanjiReading == c }?.let {
                    KanjiKnowledge(character = c, meanings = listOf(it.glossary.firstOrNull() ?: ""))
                } ?: KanjiKnowledge(character = c, meanings = listOf("?"))
            }

        override suspend fun kanjiOfWord(word: WordKnowledge): List<KanjiKnowledge> =
            word.kanjiReading?.map { c -> kanjiForCharacters(listOf(c.toString())).first() }.orEmpty()
    }

    private val dict = FakeLookup(
        listOf(
            word(1, "日本語", "にほんご", "Japanese language"),
            word(2, "日本", "にほん", "Japan"),
            word(3, "食べる", "たべる", "to eat"),
            word(4, null, "です", "is (polite copula)"),
            word(5, null, "ます", "polite ending")
        )
    )

    private val segmenter = WordSegmenter(lookup = dict)

    @Test
    fun compoundKanjiResolvesToSingleWordToken() {
        val segments = segmenter.segment("日本語")
        assertEquals(1, segments.size)
        val token = segments[0]
        assertEquals("日本語", token.text)
        assertEquals(SegmentKind.Word, token.kind)
        assertNotNull(token.word)
        assertEquals("Japanese language", token.word!!.glossary.first())
        // The token carries the kanji of its spelling.
        assertEquals(listOf("日", "本", "語"), token.kanji.map { it.character })
    }

    @Test
    fun longestFormWins() {
        // 日本語 wins over 日本 at the same start position.
        val segments = segmenter.segment("日本語")
        assertEquals("日本語", segments[0].text)
        assertEquals(1L, segments[0].word?.id)
    }

    @Test
    fun mixedKanjiKanaWordStaysOneToken() {
        val segments = segmenter.segment("食べる")
        assertEquals(1, segments.size)
        assertEquals("食べる", segments[0].text)
        assertEquals(SegmentKind.Word, segments[0].kind)
        assertNotNull(segments[0].word)
        assertEquals(listOf("食", "べ", "る"), segments[0].kanji.map { it.character })
    }

    @Test
    fun particleStaysKanaSegmentWhenUnmatched() {
        val segments = segmenter.segment("を")
        assertEquals(1, segments.size)
        assertEquals(SegmentKind.Kana, segments[0].kind)
        assertNull(segments[0].word)
    }

    @Test
    fun unmatchedKanjiFallsBackToCharacterSegments() {
        // 漢 has no word in this canned dict — per-character kanji segment.
        val segments = segmenter.segment("漢字")
        assertEquals(2, segments.size)
        assertEquals(SegmentKind.Kanji, segments[0].kind)
        assertEquals("漢", segments[0].text)
        assertEquals(SegmentKind.Kanji, segments[1].kind)
        assertEquals("字", segments[1].text)
    }

    @Test
    fun sentenceSegmentsInOrderWithoutLoss() {
        val segments = segmenter.segment("日本語を食べる。")
        assertEquals("日本語を食べる。", segments.joinToString("") { it.text })
        assertEquals(SegmentKind.Word, segments[0].kind)        // 日本語
        assertEquals(SegmentKind.Kana, segments[1].kind)        // を (no word)
        assertEquals(SegmentKind.Word, segments[2].kind)        // 食べる
        assertEquals(SegmentKind.Punctuation, segments[3].kind) // 。
        assertEquals(4, segments.size)
    }

    @Test
    fun blankSentenceHasNoSegments() = runTestSuspend {
        assertTrue(segmenter.segment("   ").isEmpty())
        assertTrue(segmenter.segment("").isEmpty())
    }

    @Test
    fun inflectedFormHonestlyStaysUnsegmented() {
        // 食べます is not the exact spelling 食べる — mixed-word matching only
        // accepts exact dictionary spellings, so the kanji + kana stay split.
        val segments = segmenter.segment("食べます")
        assertEquals("食べます", segments.joinToString("") { it.text })
        assertEquals(SegmentKind.Kanji, segments[0].kind) // 食
        assertEquals("食", segments[0].text)
        assertEquals(SegmentKind.Kana, segments[1].kind)   // べます
        assertEquals("べます", segments[1].text)
    }

    @Test
    fun standaloneKanaWordMatches() {
        val segments = segmenter.segment("ます")
        assertEquals(1, segments.size)
        assertEquals(SegmentKind.Word, segments[0].kind)
        assertEquals(5L, segments[0].word?.id)
    }

    @Test
    fun unmatchedInflectedEndingStaysOneKanaSegment() {
        // します is not in the canned dict — the whole kana run is one segment.
        val segments = segmenter.segment("します")
        assertEquals(1, segments.size)
        assertEquals("します", segments[0].text)
        assertEquals(SegmentKind.Kana, segments[0].kind)
        assertNull(segments[0].word)
    }
}

/** Small helper so tests don't need kotlinx-coroutines-test everywhere. */
private suspend fun <T> runTestSuspend(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
