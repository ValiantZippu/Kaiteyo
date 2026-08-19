package ua.syt0r.kanji.desktop.engine.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchPipelineTest {

    // ------------------------------------------------------------
    // Normalization
    // ------------------------------------------------------------

    @Test
    fun normalizeNfkcFullWidthToHalfWidth() {
        // Full-width ＡＢＣ → half-width ABC.
        assertEquals("abc", SearchPipeline.normalize("ＡＢＣ"))
    }

    @Test
    fun normalizeTrimsWhitespace() {
        assertEquals("学校", SearchPipeline.normalize("  学校  "))
    }

    // ------------------------------------------------------------
    // Tokenization
    // ------------------------------------------------------------

    @Test
    fun tokenizeSplitsJapaneseAndLatin() {
        val tokens = SearchPipeline.tokenize("学校 JLPT")
        assertEquals(2, tokens.size)
        assertEquals("学校", tokens[0].text)
        assertTrue(tokens[0].isJapanese)
        assertEquals("JLPT", tokens[1].text)
        assertTrue(!tokens[1].isJapanese)
    }

    @Test
    fun tokenizeKeepsKanaRuns() {
        val tokens = SearchPipeline.tokenize("がっこう")
        assertEquals(listOf("がっこう"), tokens.map { it.text })
    }

    // ------------------------------------------------------------
    // Ranking
    // ------------------------------------------------------------

    @Test
    fun rankExactPrefixContainsKana() {
        assertEquals(SearchRank.Exact, SearchPipeline.rank("学校", "学校"))
        assertEquals(SearchRank.Prefix, SearchPipeline.rank("学", "学校"))
        assertEquals(SearchRank.Contains, SearchPipeline.rank("学校", "学校祭"))
        // がっこう is kana for 学校 (kanji) → kana match.
        assertEquals(SearchRank.Kana, SearchPipeline.rank("がっこう", "学校"))
    }

    @Test
    fun rankAndSortOrdersBestFirst() {
        val candidates = listOf("学校祭", "学校", "学園", "雑誌")
        val ranked = SearchPipeline.rankAndSort("学校", candidates)
        assertEquals("学校", ranked[0].first)
        assertEquals(SearchRank.Exact, ranked[0].second)
        assertTrue(ranked.none { it.first == "雑誌" })
    }

    @Test
    fun rankKanaQueryAgainstKanaCandidateIsExact() {
        assertEquals(SearchRank.Exact, SearchPipeline.rank("がっこう", "がっこう"))
    }

    // ------------------------------------------------------------
    // Trigram index
    // ------------------------------------------------------------

    @Test
    fun trigramSearchFindsSubstring() {
        val index = TrigramIndex()
        index.add("学校に行きます")
        index.add("大学で勉強します")
        index.add("これは本です")

        val results = index.search("学校")
        assertEquals(1, results.size)
        assertTrue(results[0].first.contains("学校"))
    }

    @Test
    fun trigramSearchShortQueryFallsBackToScan() {
        val index = TrigramIndex()
        index.add("あいうえお")
        index.add("かきくけこ")
        val results = index.search("あい")
        assertEquals(1, results.size)
        assertEquals("あいうえお", results[0].first)
    }

    @Test
    fun trigramSearchCountsOccurrences() {
        val index = TrigramIndex()
        index.add("テストテスト")
        val results = index.search("テスト")
        assertEquals(1, results.size)
        assertEquals(2, results[0].second)
    }

    @Test
    fun trigramSearchNoMatchesReturnsEmpty() {
        val index = TrigramIndex()
        index.add("日本語のテキスト")
        assertTrue(index.search("英語").isEmpty())
    }
}
