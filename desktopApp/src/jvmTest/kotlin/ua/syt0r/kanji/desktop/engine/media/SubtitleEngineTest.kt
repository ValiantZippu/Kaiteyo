package ua.syt0r.kanji.desktop.engine.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubtitleEngineTest {

    private val srtSample = """
        1
        00:00:01,000 --> 00:00:03,500
        今日は学校に行く。

        2
        00:00:04,000 --> 00:00:06,000
        そして、勉強する。

        3
        00:00:07,250 --> 00:00:09,000
        {malformed timestamp line}
        食べました。
    """.trimIndent()

    private fun engine(): SubtitleEngine {
        val engine = SubtitleEngine()
        val track = SubtitleParser.parseSrt(srtSample)
        engine.addTrack(SubtitleTrackEntry("t1", "sample", track))
        return engine
    }

    @Test
    fun parsesCuesInOrder() {
        val engine = engine()
        assertEquals(3, engine.sortedCues().size)
        assertEquals("今日は学校に行く。", engine.sortedCues()[0].text)
    }

    @Test
    fun findsActiveCueByBinarySearch() {
        val engine = engine()
        assertEquals(0, engine.cueIndexAt(2000))
        assertEquals(1, engine.cueIndexAt(5000))
        assertEquals(2, engine.cueIndexAt(8000))
        // Between cues → index of the previous cue.
        assertEquals(0, engine.cueIndexAt(3900))
        // Before everything → -1, after everything → last index.
        assertEquals(-1, engine.cueIndexAt(0))
        assertEquals(2, engine.cueIndexAt(60000))
    }

    @Test
    fun globalOffsetShiftsActiveCue() {
        val engine = engine()
        // +2s offset: cue 0 now spans 3s..5.5s.
        engine.setOffset(2000)
        assertEquals(0, engine.cueIndexAt(4000))
        assertEquals(-1, engine.cueIndexAt(1000))
        assertEquals(1, engine.cueIndexAt(5500))
    }

    @Test
    fun nextAndPreviousCue() {
        val engine = engine()
        val next = engine.nextCue(3500)
        assertNotNull(next)
        assertEquals("そして、勉強する。", next.text)
        val prev = engine.prevCue(6000)
        assertNotNull(prev)
        assertEquals("今日は学校に行く。", prev.text)
    }

    @Test
    fun cueMarkersAggregateWhenDense() {
        val engine = engine()
        val markers = engine.cueMarkers(maxMarkers = 2)
        assertEquals(2, markers.size)
        assertEquals(1000, markers[0])
    }

    @Test
    fun assParsingStripsTagsAndHandlesCommasInText() {
        val ass = """
            [Script Info]
            Title: test

            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.00,0:00:02.50,Default,Speaker1,0,0,0,,{\fad(100,100)}こんにちは、世界！\N二行目
        """.trimIndent()
        val track = SubtitleParser.parseAss(ass, "test", SubtitleFormat.Ass)
        assertEquals(1, track.cues.size)
        val cue = track.cues[0]
        assertEquals(1000, cue.startMs)
        assertEquals(2500, cue.endMs)
        assertEquals("こんにちは、世界！\n二行目", cue.text)
        assertEquals("Speaker1", cue.speaker)
    }

    @Test
    fun vttWithHeaderParses() {
        val vtt = """
            WEBVTT

            NOTE test file

            00:00:00.500 --> 00:00:02.000
            こんにちは

            00:00:03.000 --> 00:00:05.000
            さようなら
        """.trimIndent()
        val track = SubtitleParser.parseVtt(vtt, "test")
        assertEquals(2, track.cues.size)
        assertEquals("こんにちは", track.cues[0].text)
        assertEquals(500, track.cues[0].startMs)
    }

    @Test
    fun emptyTrackIsSafe() {
        val engine = SubtitleEngine()
        assertNull(engine.activeCueAt(1000))
        assertEquals(-1, engine.cueIndexAt(1000))
        assertTrue(engine.sortedCues().isEmpty())
    }
}
