package ua.syt0r.kanji.desktop.engine.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaNodeFamilyTest {

    private fun miningEvent(cardId: String, media: String, ts: Long, cue: String) = MediaMiningEvent(
        cardId = cardId,
        mediaPath = "/media/$media",
        mediaName = media,
        timestampMs = ts,
        cueText = cue,
        createdAt = ""
    )

    @Test
    fun groupsEpisodesIntoSeriesByTitle() {
        val graph = MediaNodeGraph()
        graph.addMiningEvent(miningEvent("c1", "Bocchi - EP01.mkv", 1000, "今日は"))
        graph.addMiningEvent(miningEvent("c2", "Bocchi - EP02.mkv", 2000, "ギター"))
        graph.addMiningEvent(miningEvent("c3", "Other.mkv", 500, "別"))

        val series = graph.series()
        assertEquals(2, series.size)
        val bocchi = series.first { it.title == "Bocchi" }
        assertEquals(2, bocchi.episodes.size)
        assertEquals(2, bocchi.exposureCount())
    }

    @Test
    fun scenesCoverMiningTimestamps() {
        val graph = MediaNodeGraph()
        graph.addMiningEvent(miningEvent("c1", "Show - EP01.mp4", 1000, "水"))
        graph.addMiningEvent(miningEvent("c2", "Show - EP01.mp4", 60_000, "食べる"))

        val episode = graph.series().single().episodes.single()
        assertEquals(2, episode.scenes.size)
        assertTrue(episode.scenes.any { it.text() == "水" })
        assertTrue(episode.scenes.any { it.text() == "食べる" })
    }

    @Test
    fun sameSceneCollectsMultipleMinedLines() {
        val graph = MediaNodeGraph()
        graph.addMiningEvent(miningEvent("c1", "Show - EP01.mp4", 1000, "水"))
        graph.addMiningEvent(miningEvent("c2", "Show - EP01.mp4", 1500, "綺麗"))

        val episode = graph.series().single().episodes.single()
        assertEquals(1, episode.scenes.size)
        assertEquals(2, episode.scenes.single().lines.size)
        assertEquals(2, episode.exposureCount())
    }

    @Test
    fun bookmarkAddsWatchTime() {
        val graph = MediaNodeGraph()
        graph.addMiningEvent(miningEvent("c1", "Show - EP01.mp4", 30_000, "水"))
        graph.addBookmark(MediaBookmark(id = "b1", mediaPath = "/media/Show - EP01.mp4", timestampMs = 1_200_000))

        val episode = graph.series().single().episodes.single()
        assertEquals(1_200_000, episode.watchTimeMs)
        assertEquals(1_200_000, graph.totalWatchTimeMs())
    }

    @Test
    fun allLinesFlattensForSentenceSearch() {
        val graph = MediaNodeGraph()
        graph.addMiningEvent(miningEvent("c1", "A - EP01.mp4", 1000, "日本語"))
        graph.addMiningEvent(miningEvent("c2", "B - EP01.mp4", 2000, "勉強"))

        val lines = graph.allLines()
        assertEquals(2, lines.size)
        assertTrue(lines.map { it.text }.containsAll(listOf("日本語", "勉強")))
    }
}
