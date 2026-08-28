package ua.syt0r.kanji.desktop.engine.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaNodeGraphIndexTest {

    private fun cues(n: Int): List<SubtitleCue> = (0 until n).map { i ->
        SubtitleCue(id = "c$i", startMs = i * 2000L, endMs = i * 2000L + 1500, text = "テスト$i")
    }

    @Test
    fun indexIsIdempotent() {
        val g = MediaNodeGraph()
        val c = cues(5)
        g.indexSubtitleTrack("/a/ep.mkv", "My Anime - EP01", c)
        assertEquals(5, g.allLines().size)
        g.indexSubtitleTrack("/a/ep.mkv", "My Anime - EP01", c)
        // Still 5, not 10 — reindex replaces.
        assertEquals(5, g.allLines().size)
    }

    @Test
    fun minedLinesPreservedOnReindex() {
        val g = MediaNodeGraph()
        val c = cues(3)
        g.indexSubtitleTrack("/a/ep.mkv", "My Anime - EP01", c)
        // Mine the second cue
        g.addMiningEvent(MediaMiningEvent(cardId = "k1", mediaPath = "/a/ep.mkv", mediaName = "My Anime - EP01", timestampMs = 2000L, cueText = "テスト1"))
        val before = g.allLines().count { it.exposureCount == 1 }
        assertEquals(1, before)
        // Reindex same cues — mined exposure must survive
        g.indexSubtitleTrack("/a/ep.mkv", "My Anime - EP01", c)
        assertTrue(g.allLines().any { it.exposureCount == 1 })
    }

    @Test
    fun emptyCuesNoOp() {
        val g = MediaNodeGraph()
        g.indexSubtitleTrack("/a/ep.mkv", "My Anime", emptyList())
        assertTrue(g.allLines().isEmpty())
    }

    @Test
    fun differentMediaIsolated() {
        val g = MediaNodeGraph()
        g.indexSubtitleTrack("/a/ep1.mkv", "Anime - EP01", cues(2))
        g.indexSubtitleTrack("/a/ep2.mkv", "Anime - EP02", cues(3))
        assertEquals(5, g.allLines().size)
        assertEquals(2, g.series().size)
    }
}
