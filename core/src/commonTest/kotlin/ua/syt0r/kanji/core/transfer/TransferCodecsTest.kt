package ua.syt0r.kanji.core.transfer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardStatus
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KaiteyoCard

class TransferCodecsTest {

    private fun card(
        id: String,
        character: String,
        meaning: String = "Water",
        reading: String = "みず / スイ",
        tags: List<String> = listOf("jlpt-n5", "水"),
        interval: Int = 21,
        status: CardStatus = CardStatus.Young
    ) = KaiteyoCard(
        id = id,
        character = character,
        meaning = meaning,
        reading = reading,
        tagNames = tags.toMutableList(),
        interval = interval,
        status = status
    )

    private val japaneseCards = listOf(
        card("k1", "水", "Water; Wednesday (water day)", "みず / スイ"),
        card("k2", "勉強", "Study; to study", "べんきょう"),
        card("k3", "猫", "Cat", "ねこ", tags = listOf("animal,pet", "jlpt-n5"))
    )

    // ------------------------------------------------------------
    // JSON
    // ------------------------------------------------------------

    @Test
    fun jsonRoundTripPreservesJapaneseText() {
        val json = TransferCodecs.toJson(japaneseCards)
        assertTrue(json.contains("水"), "JSON must contain the kanji, got: $json")
        assertTrue(json.contains("べんきょう"), "JSON must contain kana reading")

        val parsed = TransferCodecs.fromJson(json).getOrThrow()
        assertEquals(japaneseCards.size, parsed.size)
        assertEquals("水", parsed[0].character)
        assertEquals("みず / スイ", parsed[0].reading)
        assertEquals("べんきょう", parsed[1].reading)
        assertEquals(listOf("animal,pet", "jlpt-n5"), parsed[2].tagNames)
        assertEquals(21, parsed[0].interval)
        assertEquals(CardStatus.Young, parsed[0].status)
    }

    @Test
    fun jsonRejectsMalformedInput() {
        val result = TransferCodecs.fromJson("{ not valid json !")
        assertTrue(result.isFailure, "Malformed JSON must fail, got $result")
    }

    // ------------------------------------------------------------
    // CSV / TSV
    // ------------------------------------------------------------

    @Test
    fun csvRoundTripHandlesCommasQuotesAndNewlines() {
        val cards = listOf(
            card("k1", "水", meaning = "Water, sea", reading = "みず"),
            card("k2", "漢字", meaning = "Kanji \"characters\"\non two lines", reading = "かんじ")
        )
        val csv = TransferCodecs.toCsv(cards)
        val parsed = TransferCodecs.fromCsv(csv).getOrThrow()
        assertEquals(2, parsed.size)
        assertEquals("Water, sea", parsed[0].meaning)
        assertEquals("Kanji \"characters\"\non two lines", parsed[1].meaning)
        assertEquals("かんじ", parsed[1].reading)
    }

    @Test
    fun tsvRoundTripPreservesJapanese() {
        val tsv = TransferCodecs.toTsv(japaneseCards)
        val parsed = TransferCodecs.fromTsv(tsv).getOrThrow()
        assertEquals(japaneseCards.size, parsed.size)
        assertEquals("水", parsed[0].character)
        assertEquals("猫", parsed[2].character)
    }

    // ------------------------------------------------------------
    // TXT
    // ------------------------------------------------------------

    @Test
    fun txtRoundTripSplitsTabSeparatedColumns() {
        val text = "水\tWater\tみず\n猫\tCat\tねこ"
        val parsed = TransferCodecs.fromTxt(text).getOrThrow()
        assertEquals(2, parsed.size)
        assertEquals("水", parsed[0].character)
        assertEquals("Water", parsed[0].meaning)
        assertEquals("みず", parsed[0].reading)
        assertEquals(CardStatus.New, parsed[0].status)
    }

    @Test
    fun txtIgnoresBlankLinesAndInvalidRows() {
        val text = "水\tWater\n\n\t\n猫\tCat\tねこ"
        val parsed = TransferCodecs.fromTxt(text).getOrThrow()
        assertEquals(2, parsed.size)
    }

    @Test
    fun txtSurvivesUnicodeRoundTrip() {
        val text = TransferCodecs.toTxt(japaneseCards)
        val parsed = TransferCodecs.fromTxt(text).getOrThrow()
        assertEquals("べんきょう", parsed[1].reading)
    }
}
