package ua.syt0r.kanji.core.transfer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardStatus
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KaiteyoCard

class AnkiPackageJvmTest {

    private val anki = AnkiPackage()

    private fun card(
        id: String,
        character: String,
        meaning: String = "Water",
        reading: String = "みず / スイ",
        tags: List<String> = listOf("jlpt-n5", "weather"),
        interval: Int = 21,
        ease: Float = 2.5f,
        status: CardStatus = CardStatus.Young,
        lapses: Int = 2,
        reviewCount: Int = 9
    ) = KaiteyoCard(
        id = id,
        character = character,
        meaning = meaning,
        reading = reading,
        deck = "Kaiteyo",
        tagNames = tags.toMutableList(),
        status = status,
        interval = interval,
        ease = ease,
        lapses = lapses,
        reviewCount = reviewCount
    )

    // ------------------------------------------------------------
    // Round trip
    // ------------------------------------------------------------

    @Test
    fun exportImportRoundTripPreservesCardData() {
        val cards = listOf(
            card("k1", "水", "Water", "みず / スイ"),
            card("k2", "猫", "Cat", "ねこ", tags = listOf("animal", "jlpt-n4"))
        )

        val bytes = anki.write(cards).getOrThrow()
        val imported = anki.read(bytes).getOrThrow()

        assertEquals(2, imported.size, "All exported cards must come back")
        val byChar = imported.associateBy { it.character }
        assertEquals("水", byChar["水"]?.character)
        assertNotNull(byChar["水"])
        assertEquals("Water", byChar["水"]?.meaning)
        assertEquals("みず / スイ", byChar["水"]?.reading)
        assertEquals(listOf("jlpt-n5", "weather"), byChar["水"]?.tagNames?.toList())
        assertEquals("jlpt-n4", byChar["猫"]?.tagNames?.getOrNull(1))

        // Scheduling state must survive the conversion.
        assertEquals(21, byChar["水"]?.interval)
        assertEquals(2.5f, byChar["水"]?.ease ?: 0f, absoluteTolerance = 0.01f)
        assertEquals(2, byChar["水"]?.lapses)
        assertEquals(9, byChar["水"]?.reviewCount)
        assertTrue(byChar["水"]?.status != CardStatus.New, "Mature card must not import as new")
    }

    @Test
    fun exportIsDeterministicPerCardGuid() {
        val a = anki.write(listOf(card("k1", "水"))).getOrThrow()
        val b = anki.write(listOf(card("k1", "水"))).getOrThrow()
        val readA = anki.read(a).getOrThrow()
        val readB = anki.read(b).getOrThrow()
        assertEquals(
            readA.first().id,
            readB.first().id,
            "Re-exporting the same card must produce the same stable id (duplicate detection)"
        )
    }

    @Test
    fun exportWithoutContentFailsWithMessage() {
        val result = anki.write(emptyList())
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Nothing to export") == true)
    }

    // ------------------------------------------------------------
    // Malformed input
    // ------------------------------------------------------------

    @Test
    fun nonZipBytesFailWithUsefulMessage() {
        val result = anki.read("this is definitely not an apkg".toByteArray())
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("ZIP") == true)
    }

    @Test
    fun zipWithoutAnkiDatabaseFailsWithUsefulMessage() {
        // A valid ZIP that simply lacks collection.anki2.
        val baos = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("readme.txt"))
            zip.write("hello".toByteArray())
            zip.closeEntry()
        }
        val result = anki.read(baos.toByteArray())
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("collection.anki2") == true)
    }

    // ------------------------------------------------------------
    // Anki mapping helpers
    // ------------------------------------------------------------

    @Test
    fun statusMappingIsConsistent() {
        assertEquals(0, anki.ankiType(CardStatus.New))
        assertEquals(2, anki.ankiType(CardStatus.Mature))
        assertEquals(-1, anki.ankiQueue(CardStatus.Suspended))
        assertEquals(CardStatus.Suspended, anki.statusFromAnki(2, -1))
        assertEquals(CardStatus.New, anki.statusFromAnki(0, 0))
        assertEquals(CardStatus.Learning, anki.statusFromAnki(1, 1))
    }

    @Test
    fun checksumMatchesAnkiSemantics() {
        // Anki's checksum: sum of UTF-8 bytes, masked to 32 bits.
        // 水 = U+6C34 → UTF-8 E6 B0 B4 → 230 + 176 + 180 = 586.
        assertEquals(586L, anki.checksum("水"))
        assertTrue(anki.checksum("水") != anki.checksum("火"))
    }
}
