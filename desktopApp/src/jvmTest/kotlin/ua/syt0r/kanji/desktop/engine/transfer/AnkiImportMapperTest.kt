package ua.syt0r.kanji.desktop.engine.transfer

import kotlinx.datetime.Instant
import ua.syt0r.kanji.desktop.model.ContentKind
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.SrsStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnkiImportMapperTest {

    // ------------------------------------------------------------
    // Deck hierarchy
    // ------------------------------------------------------------

    @Test
    fun `deck path splits into segments`() {
        assertEquals(listOf("Japanese", "N5", "Kanji"), AnkiImportMapper.splitDeckPath("Japanese::N5::Kanji"))
        assertEquals(listOf("Basic"), AnkiImportMapper.splitDeckPath("Basic"))
        assertEquals(emptyList(), AnkiImportMapper.splitDeckPath("::  ::"))
    }

    @Test
    fun `leaf name is the final segment`() {
        assertEquals("Kanji", AnkiImportMapper.leafDeckName("Japanese::N5::Kanji"))
        assertEquals("Basic", AnkiImportMapper.leafDeckName("Basic"))
    }

    @Test
    fun `parent paths build incrementally`() {
        assertEquals(listOf("Japanese", "Japanese::N5"), AnkiImportMapper.parentPaths("Japanese::N5::Kanji"))
        assertEquals(emptyList(), AnkiImportMapper.parentPaths("Basic"))
    }

    // ------------------------------------------------------------
    // Field extraction
    // ------------------------------------------------------------

    @Test
    fun `field lookup is case insensitive`() {
        val fields = mapOf("fRoNt" to "食べる", "back" to "to eat")
        assertEquals("食べる", AnkiImportMapper.pickField(fields, "Front", "Expression"))
        assertEquals("to eat", AnkiImportMapper.pickField(fields, "Back"))
        assertEquals("", AnkiImportMapper.pickField(fields, "Missing"))
    }

    @Test
    fun `html is stripped to plain text`() {
        assertEquals("食べる\nto eat", AnkiImportMapper.stripHtml("<b>食べる</b><br><i>to eat</i>"))
        assertEquals("A & B", AnkiImportMapper.stripHtml("A &amp; B"))
        assertEquals("answer", AnkiImportMapper.stripHtml("{{c1::answer::hint}}"))
        assertEquals("", AnkiImportMapper.stripHtml("[sound:foo.mp3]"))
    }

    @Test
    fun `media filenames are extracted from fields`() {
        val fields = mapOf(
            "Front" to "食べる [sound:taberu.mp3]",
            "Back" to "<img src=\"taberu.png\">",
            "Hint" to "<img src='other.png'>"
        )
        assertEquals(
            setOf("taberu.mp3", "taberu.png", "other.png"),
            AnkiImportMapper.extractMediaFilenames(fields).toSet()
        )
    }

    // ------------------------------------------------------------
    // Content kind
    // ------------------------------------------------------------

    @Test
    fun `single kanji is Kanji content`() {
        assertEquals(ContentKind.Kanji, AnkiImportMapper.inferContentKind("水"))
    }

    @Test
    fun `kana and kanji mixed content is Vocabulary`() {
        assertEquals(ContentKind.Vocabulary, AnkiImportMapper.inferContentKind("食べる"))
        assertEquals(ContentKind.Vocabulary, AnkiImportMapper.inferContentKind("学校"))
        assertEquals(ContentKind.Vocabulary, AnkiImportMapper.inferContentKind("ありがとう"))
    }

    // ------------------------------------------------------------
    // Scheduling mapping
    // ------------------------------------------------------------

    @Test
    fun `queue codes map to kaiteyo status`() {
        assertEquals(SrsStatus.New, AnkiImportMapper.queueToStatus(0))
        assertEquals(SrsStatus.Learning, AnkiImportMapper.queueToStatus(1))
        assertEquals(SrsStatus.Review, AnkiImportMapper.queueToStatus(2))
        assertEquals(SrsStatus.Learning, AnkiImportMapper.queueToStatus(3))
        assertEquals(SrsStatus.Suspended, AnkiImportMapper.queueToStatus(-1))
        assertEquals(SrsStatus.Suspended, AnkiImportMapper.queueToStatus(-2))
        assertEquals(SrsStatus.Buried, AnkiImportMapper.queueToStatus(-3))
        assertEquals(SrsStatus.Buried, AnkiImportMapper.queueToStatus(-4))
    }

    @Test
    fun `learning intervals are seconds, review intervals are days`() {
        assertEquals(3600.0 / 86400.0, AnkiImportMapper.intervalToDays(3600, 1))
        assertEquals(5.0, AnkiImportMapper.intervalToDays(5, 2))
        assertEquals(0.0, AnkiImportMapper.intervalToDays(0, 2))
    }

    @Test
    fun `due dates map for learning and review queues`() {
        val ms = 1_700_000_000_000L
        assertEquals(Instant.fromEpochMilliseconds(ms), AnkiImportMapper.dueToInstant(ms, 1))
        assertEquals(Instant.fromEpochMilliseconds(20_000L * 86_400_000L), AnkiImportMapper.dueToInstant(20_000, 2))
        assertNull(AnkiImportMapper.dueToInstant(0, 2))
        assertNull(AnkiImportMapper.dueToInstant(5, -1))
    }

    @Test
    fun `ease permille converts to decimal`() {
        assertEquals(2.5, AnkiImportMapper.easeToDouble(2500))
        assertEquals(1.0, AnkiImportMapper.easeToDouble(500))
        assertEquals(2.5, AnkiImportMapper.easeToDouble(null))
    }

    // ------------------------------------------------------------
    // Identity + dedupe
    // ------------------------------------------------------------

    @Test
    fun `external id and card id are stable`() {
        assertEquals("anki:abc-123", AnkiImportMapper.externalIdFor("abc-123"))
        assertEquals(
            AnkiImportMapper.cardIdFor("some-guid"),
            AnkiImportMapper.cardIdFor("some-guid")
        )
        assertTrue(AnkiImportMapper.cardIdFor("a") != AnkiImportMapper.cardIdFor("b"))
    }

    @Test
    fun `fingerprint normalises content`() {
        assertEquals(
            AnkiImportMapper.fingerprint("食べる", "<b>To Eat</b>"),
            AnkiImportMapper.fingerprint("食べる", "To eat")
        )
        assertTrue(
            AnkiImportMapper.fingerprint("食べる", "to eat") !=
                AnkiImportMapper.fingerprint("飲む", "to drink")
        )
    }

    // ------------------------------------------------------------
    // Note → card
    // ------------------------------------------------------------

    @Test
    fun `basic note maps onto a kaiteyo card`() {
        val card = AnkiImportMapper.noteToCard(
            guid = "guid-1",
            fields = mapOf(
                "Front" to "<b>学校</b>",
                "Back" to "school",
                "Reading" to "がっこう",
                "Example" to "学校に行く",
                "Extra" to "common word"
            ),
            tags = listOf("jlpt-n5"),
            scheduling = AnkiSchedulingData(queue = 2, interval = 7, due = 19_000, reps = 3, lapses = 1, ease = 2300),
            deckId = "deck-japanese-n5",
            includeScheduling = true,
            ankiDeckName = "Japanese::N5::Kanji"
        )

        assertEquals("学校", card.character)
        assertEquals("school", card.meaning)
        assertEquals(listOf("がっこう"), card.onReadings)
        assertEquals(SrsStatus.Review, card.status)
        assertEquals(7.0, card.intervalDays)
        assertEquals(3, card.reps)
        assertEquals(1, card.lapses)
        assertEquals(2.3, card.ease)
        assertEquals("anki:guid-1", card.externalId)
        assertEquals("deck-japanese-n5", card.deckId)
        assertTrue(card.tags.containsAll(listOf("anki", "jlpt-n5", "anki:Kanji")))
        // Unknown fields are preserved in the note.
        assertTrue(card.note.contains("Extra: common word"))
        assertTrue(card.note.contains("Sentence: 学校に行く"))
        assertTrue(card.note.contains("Anki deck: Japanese::N5::Kanji"))
    }

    @Test
    fun `scheduling can be skipped`() {
        val card = AnkiImportMapper.noteToCard(
            guid = "guid-2",
            fields = mapOf("Front" to "水", "Back" to "water"),
            tags = emptyList(),
            scheduling = AnkiSchedulingData(queue = 2, interval = 90, due = 100, reps = 20, lapses = 4, ease = 2000),
            deckId = "d",
            includeScheduling = false,
            ankiDeckName = "Basic"
        )
        assertEquals(SrsStatus.New, card.status)
        assertEquals(0.0, card.intervalDays)
        assertNull(card.dueAt)
        assertEquals(0, card.reps)
    }

    @Test
    fun `suspended cards stay suspended`() {
        val card = AnkiImportMapper.noteToCard(
            guid = "guid-3",
            fields = mapOf("Front" to "鳥", "Back" to "bird"),
            tags = emptyList(),
            scheduling = AnkiSchedulingData(queue = -1, interval = 10, due = 0, reps = 5, lapses = 3, ease = 2500),
            deckId = "d",
            includeScheduling = true,
            ankiDeckName = "Basic"
        )
        assertEquals(SrsStatus.Suspended, card.status)
    }

    @Test
    fun `missing front falls back to first non-empty field`() {
        val card = AnkiImportMapper.noteToCard(
            guid = "guid-4",
            fields = mapOf("Question" to "山", "Answer" to "mountain"),
            tags = emptyList(),
            scheduling = null,
            deckId = "d",
            includeScheduling = true,
            ankiDeckName = "Basic"
        )
        assertEquals("山", card.character)
        assertEquals("mountain", card.meaning)
    }

    // ------------------------------------------------------------
    // Dedupe lookup
    // ------------------------------------------------------------

    private fun card(guid: String, character: String, meaning: String): DesktopCard {
        val base = AnkiImportMapper.noteToCard(
            guid = guid,
            fields = mapOf("Front" to character, "Back" to meaning),
            tags = emptyList(),
            scheduling = null,
            deckId = "d",
            includeScheduling = true,
            ankiDeckName = "Basic"
        )
        return base.copy(id = "existing-$guid")
    }

    @Test
    fun `findExisting matches by external id first`() {
        val native = card("g-native", "山", "mountain")
        val imported = card("g-anki", "山", "mountain")
        val byExternal = mapOf(imported.externalId to imported)
        val byFingerprint = mapOf(AnkiImportMapper.fingerprint(native.character, native.meaning) to listOf(native))

        val incoming = AnkiImportMapper.noteToCard(
            guid = "g-anki",
            fields = mapOf("Front" to "山", "Back" to "mountain"),
            tags = emptyList(),
            scheduling = null,
            deckId = "d",
            includeScheduling = true,
            ankiDeckName = "Basic"
        )
        assertEquals(imported, AnkiImportMapper.findExisting(byExternal, byFingerprint, incoming))
    }

    @Test
    fun `findExisting falls back to fingerprint for native cards`() {
        val native = card("g-native", "水", "water")
        val byFingerprint = mapOf(AnkiImportMapper.fingerprint(native.character, native.meaning) to listOf(native))

        val incoming = AnkiImportMapper.noteToCard(
            guid = "g-other",
            fields = mapOf("Front" to "水", "Back" to "water"),
            tags = emptyList(),
            scheduling = null,
            deckId = "d",
            includeScheduling = true,
            ankiDeckName = "Basic"
        )
        assertEquals(native, AnkiImportMapper.findExisting(emptyMap(), byFingerprint, incoming))
    }

    @Test
    fun `findExisting returns null for genuinely new content`() {
        val incoming = AnkiImportMapper.noteToCard(
            guid = "g-new",
            fields = mapOf("Front" to "空", "Back" to "sky"),
            tags = emptyList(),
            scheduling = null,
            deckId = "d",
            includeScheduling = true,
            ankiDeckName = "Basic"
        )
        assertNull(AnkiImportMapper.findExisting(emptyMap(), emptyMap(), incoming))
    }
}
