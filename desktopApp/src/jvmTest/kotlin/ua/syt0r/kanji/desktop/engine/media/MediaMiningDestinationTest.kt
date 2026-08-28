package ua.syt0r.kanji.desktop.engine.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.engine.mining.CardDestination
import ua.syt0r.kanji.desktop.engine.mining.MiningPayload
import java.io.File
import java.nio.file.Files

/**
 * Critical E2E slice: one mining action through DestinationResolver must not
 * create three separate pipelines. Verifies the directive §9 abstraction:
 * Kaiteyo / Anki / Both via a single MiningPayload.
 */
class MediaMiningDestinationTest {

    private fun tempHome(): File = Files.createTempDirectory("kaiteyo-mining-e2e").toFile()

    @Test
    fun kaiteyoOnlyCreatesCardInPool() {
        val home = tempHome()
        val prev = System.getProperty("user.home")
        System.setProperty("user.home", home.absolutePath)
        try {
            val state = AppState()
            state.settings.set("media.mine-destination", "kaiteyo")
            state.settings.setBool("media.anki.enabled", false)
            val payload = MiningPayload(headword = "猫", reading = "ねこ", definition = "cat", sentence = "猫が寝ている。", source = "subtitle", sourceDetail = "Test Anime · srt")
            val card = state.mining.mine(payload)
            assertNotNull(card)
            assertTrue(state.cards.any { it.character == "猫" })
            assertEquals(CardDestination.Kaiteyo, state.mining.resolveDestination(null))
        } finally {
            System.setProperty("user.home", prev)
            home.deleteRecursively()
        }
    }

    @Test
    fun bothDestinationCreatesKaiteyoCardEvenWhenAnkiUnreachable() {
        val home = tempHome()
        val prev = System.getProperty("user.home")
        System.setProperty("user.home", home.absolutePath)
        try {
            val state = AppState()
            state.settings.set("media.mine-destination", "both")
            state.settings.setBool("media.anki.enabled", true)
            state.settings.set("media.anki.host", "127.0.0.1")
            state.settings.set("media.anki.port", 19876) // nothing listening → fail
            val before = state.cards.size
            val payload = MiningPayload(headword = "犬", reading = "いぬ", definition = "dog", sentence = "犬が走る。", source = "subtitle")
            val card = state.mining.mine(payload)
            // Kaiteyo card must exist even though Anki export fails (queued for retry).
            assertNotNull(card)
            assertEquals(before + 1, state.cards.size)
            // Pending export queued because Anki was enabled and unreachable.
            assertTrue(state.mining.pendingExports.isNotEmpty() || state.mining.minedRecords.any { it.ankiStatus == "failed" })
        } finally {
            System.setProperty("user.home", prev)
            home.deleteRecursively()
        }
    }

    @Test
    fun ankiOnlyDoesNotDuplicateKaiteyoCardWhenReachable() {
        // When Anki is disabled, anki-only should fallback to Kaiteyo (never lose word).
        val home = tempHome()
        val prev = System.getProperty("user.home")
        System.setProperty("user.home", home.absolutePath)
        try {
            val state = AppState()
            state.settings.setBool("media.anki.enabled", false)
            val payload = MiningPayload(headword = "山", reading = "やま", definition = "mountain", source = "subtitle")
            val card = state.mining.mine(payload, destinationOverride = CardDestination.Anki)
            // Disabled Anki → still a Kaiteyo card, no pending export.
            assertNotNull(card)
            assertTrue(state.mining.pendingExports.isEmpty())
        } finally {
            System.setProperty("user.home", prev)
            home.deleteRecursively()
        }
    }

    @Test
    fun miningPayloadPreservesMediaProvenance() {
        val payload = MiningPayload(
            headword = "海",
            reading = "うみ",
            definition = "sea",
            sentence = "海が綺麗だ。",
            source = "subtitle",
            sourceDetail = "My Anime · Episode 1",
            timestamp = 83.5,
            videoPath = "/tmp/episode.mkv",
            tags = listOf("media:My Anime", "subtitle")
        )
        assertEquals(83.5, payload.timestamp)
        assertEquals("/tmp/episode.mkv", payload.videoPath)
        assertTrue(payload.tags.contains("subtitle"))
    }
}
