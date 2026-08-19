package ua.syt0r.kanji.desktop.engine.graph

import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.SrsStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaExposureIndexTest {

    private fun minedCard(
        id: String,
        character: String,
        note: String,
        tags: List<String> = listOf("mined", "source:subtitle")
    ) = DesktopCard(
        id = id,
        character = character,
        meaning = "meaning",
        tags = tags,
        note = note,
        status = SrsStatus.New
    )

    private val subtitleNote = buildString {
        append("Sentence: 学校に行きます。\n")
        append("Source: 例のアニメ EP1\n")
        append("Timestamp: 42.5\n")
    }

    @Test
    fun wordCardAppearsInMedia() {
        val card = minedCard("c1", "学校", subtitleNote)
        val appearances = MediaExposureIndex.appearancesFor("学校", listOf(card))
        assertEquals(1, appearances.size)
        assertEquals("例のアニメ EP1", appearances[0].mediaTitle)
        assertEquals(42.5, appearances[0].timestamp)
        assertEquals("subtitle", appearances[0].source)
    }

    @Test
    fun kanjiMatchesWordsThatContainIt() {
        val card = minedCard("c1", "学校", subtitleNote)
        val appearances = MediaExposureIndex.appearancesFor("学", listOf(card))
        assertEquals(1, appearances.size)
        assertEquals("学校", card.character)
    }

    @Test
    fun nonMediaCardsAreExcluded() {
        val card = minedCard(
            id = "c1",
            character = "学校",
            note = "Source: reading doc",
            tags = listOf("mined", "source:reader")
        )
        // Reader mines are not media exposure.
        assertTrue(MediaExposureIndex.appearancesFor("学校", listOf(card)).isEmpty())
    }

    @Test
    fun cardsWithoutSourceLineAreExcluded() {
        val card = minedCard("c1", "学校", note = "Sentence: 学校。")
        assertTrue(MediaExposureIndex.appearancesFor("学校", listOf(card)).isEmpty())
    }

    @Test
    fun mediaTitlesAggregateCounts() {
        val cards = listOf(
            minedCard("c1", "学校", subtitleNote),
            minedCard(
                "c2", "大学",
                "Sentence: 大学です。\nSource: 例のアニメ EP1\nTimestamp: 100.0\n"
            ),
            minedCard(
                "c3", "食べる",
                "Sentence: 食べる。\nSource: 別のドラマ\nTimestamp: 5.0\n"
            )
        )
        val titles = MediaExposureIndex.mediaTitles(cards)
        assertEquals(2, titles.size)
        assertEquals("例のアニメ EP1", titles[0].first)
        assertEquals(2, titles[0].second)
        assertEquals("別のドラマ", titles[1].first)
    }
}
