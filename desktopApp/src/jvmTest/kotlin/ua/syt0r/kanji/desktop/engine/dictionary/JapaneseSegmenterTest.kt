package ua.syt0r.kanji.desktop.engine.dictionary

import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.SrsStatus
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JapaneseSegmenterTest {

    private fun repository(): DictionaryRepository {
        val dir = File.createTempFile("kaiteyo-dict", "").let { it.delete(); File(it.parentFile, "kaiteyo-dict-test-${System.nanoTime()}") }
        val repo = DictionaryRepository(dir)
        val entries = listOf(
            DictionaryEntry(
                headword = "学校",
                spellings = listOf("学校"),
                readings = listOf(DictionaryReading("がっこう")),
                senses = listOf(DictionarySense(glosses = listOf("school"))),
                searchKeys = listOf("学校", "がっこう"),
                dictionaryId = "test"
            ),
            DictionaryEntry(
                headword = "行く",
                spellings = listOf("行く"),
                readings = listOf(DictionaryReading("いく")),
                senses = listOf(DictionarySense(glosses = listOf("to go"))),
                searchKeys = listOf("行く", "いく"),
                dictionaryId = "test"
            ),
            DictionaryEntry(
                headword = "食べる",
                spellings = listOf("食べる"),
                readings = listOf(DictionaryReading("たべる")),
                senses = listOf(DictionarySense(glosses = listOf("to eat"))),
                searchKeys = listOf("食べる", "たべる"),
                dictionaryId = "test"
            ),
            DictionaryEntry(
                headword = "走る",
                spellings = listOf("走る"),
                readings = listOf(DictionaryReading("はしる")),
                senses = listOf(DictionarySense(glosses = listOf("to run"))),
                searchKeys = listOf("走る", "はしる"),
                dictionaryId = "test"
            ),
            DictionaryEntry(
                headword = "に",
                spellings = listOf("に"),
                readings = listOf(DictionaryReading("に")),
                senses = listOf(DictionarySense(glosses = listOf("particle"))),
                searchKeys = listOf("に"),
                dictionaryId = "test"
            ),
            DictionaryEntry(
                headword = "ありがとう",
                spellings = listOf("ありがとう"),
                readings = listOf(DictionaryReading("ありがとう")),
                senses = listOf(DictionarySense(glosses = listOf("thanks"))),
                searchKeys = listOf("ありがとう"),
                dictionaryId = "test"
            )
        )
        repo.install(
            InstalledDictionary(id = "test", name = "Test", enabled = true, priority = 0),
            entries
        )
        return repo
    }

    private fun surfaces(tokens: List<SegmentToken>): List<String> = tokens.map { it.surface }

    @Test
    fun segmentsKanjiKanaMixedSentence() {
        val tokens = JapaneseSegmenter.segment("学校に行く", repository())
        assertEquals(listOf("学校", "に", "行く"), surfaces(tokens))
        assertEquals("がっこう", tokens[0].reading)
        assertEquals("いく", tokens[2].reading)
        assertNotNull(tokens[2].dictionaryMatch)
        assertEquals("行く", tokens[2].dictionaryMatch?.entry?.headword)
    }

    @Test
    fun pureKanaWordIsSingleToken() {
        val tokens = JapaneseSegmenter.segment("ありがとう", repository())
        assertEquals(listOf("ありがとう"), surfaces(tokens))
        assertEquals("thanks", tokens[0].dictionaryMatch?.entry?.senses?.firstOrNull()?.primaryGloss)
    }

    @Test
    fun inflectedPastTenseRecoversDictionaryForm() {
        val tokens = JapaneseSegmenter.segment("走った", repository())
        // 走った deinflects to 走る — the span stays one token with the match.
        assertTrue(surfaces(tokens).contains("走った"))
        val matched = tokens.first { it.surface == "走った" }
        assertEquals("走る", matched.dictionaryMatch?.entry?.headword)
    }

    @Test
    fun unknownWordsFallBackToKanjiAndKanaRuns() {
        val tokens = JapaneseSegmenter.segment("量子コンピュータ", repository())
        // 量子 is unknown → kanji chars as individual tokens, kana as a run.
        assertTrue(surfaces(tokens).contains("量"))
        assertTrue(surfaces(tokens).contains("子"))
        assertTrue(tokens.any { it.surface == "コンピュータ" })
    }

    @Test
    fun wordStatusReflectsCardPool() {
        val repo = repository()
        val cards = listOf(
            DesktopCard(
                id = "c1",
                character = "学校",
                meaning = "school",
                status = SrsStatus.Review,
                intervalDays = 5.0
            ),
            DesktopCard(
                id = "c2",
                character = "行く",
                meaning = "go",
                tags = listOf("mined"),
                status = SrsStatus.New
            )
        )
        val tokens = JapaneseSegmenter.segment("学校に行く", repo, cards)
        assertEquals(WordStatus.Known, tokens.first { it.surface == "学校" }.status)
        assertEquals(WordStatus.Mined, tokens.first { it.surface == "行く" }.status)
        assertEquals(WordStatus.Unknown, tokens.first { it.surface == "に" }.status)
    }

    @Test
    fun coverageEstimatesKnownFraction() {
        val repo = repository()
        val cards = listOf(
            DesktopCard(id = "c1", character = "学校", meaning = "school", status = SrsStatus.Review, intervalDays = 30.0)
        )
        val coverage = JapaneseSegmenter.coverage("学校に行く", repo, cards)
        assertTrue(coverage > 0f && coverage < 1f)
        assertEquals(1f, JapaneseSegmenter.coverage("学校", repo, cards))
    }
}
