package ua.syt0r.kanji.desktop.engine.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Fuzz / robustness tests for SubtitleParser (§40, §357).
 * Every case asserts: no throw, bounded output, malformed → empty or degraded, never crash.
 */
class SubtitleParserFuzzTest {

    @Test
    fun emptyInputReturnsEmptyTrack() {
        assertEquals(0, SubtitleParser.parseSrt("", "empty").cues.size)
        assertEquals(0, SubtitleParser.parseVtt("", "empty").cues.size)
        assertEquals(0, SubtitleParser.parseAss("", "empty").cues.size)
    }

    @Test
    fun bomAndCrlfNormalized() {
        val srt = "\uFEFF1\r\n00:00:01,000 --> 00:00:02,000\r\nこんにちは\r\n\r\n"
        val track = SubtitleParser.parseSrt(srt, "bom")
        assertEquals(1, track.cues.size)
        assertEquals("こんにちは", track.cues[0].text)
    }

    @Test
    fun malformedTimestampIsSkipped() {
        val srt = """
            1
            99:99:99,999 --> XX:YY:ZZ,000
            bad time

            2
            00:00:01,000 --> 00:00:02,000
            good line
        """.trimIndent()
        val track = SubtitleParser.parseSrt(srt, "malformed")
        assertEquals(1, track.cues.size)
        assertEquals("good line", track.cues[0].text)
    }

    @Test
    fun hugeFileDoesNotAllocateUnbounded() {
        val cue = "00:00:01,000 --> 00:00:02,000\nhello\n\n"
        val huge = buildString { repeat(10_000) { append("${it + 1}\n"); append(cue) } }
        val track = SubtitleParser.parseSrt(huge, "huge")
        assertEquals(10_000, track.cues.size)
        assertEquals("hello", track.cues[0].text)
    }

    @Test
    fun assWithoutFormatProducesEmpty() {
        val ass = """
            [Events]
            Dialogue: 0,0:00:01.00,0:00:02.00,Default,,0,0,0,,hello
        """.trimIndent()
        val track = SubtitleParser.parseAss(ass, "no-format")
        assertEquals(0, track.cues.size)
    }

    @Test
    fun assStripsTagsAndPreservesCommas() {
        val ass = """
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.00,0:00:03.00,Default,Speaker,0,0,0,,Hello, world, {\i1}こんにちは\Nnext
        """.trimIndent()
        val track = SubtitleParser.parseAss(ass, "tags")
        assertEquals(1, track.cues.size)
        assertEquals("Hello, world, こんにちは\nnext", track.cues[0].text)
    }

    @Test
    fun vttHeaderAndNoteIgnored() {
        val vtt = """
            WEBVTT

            NOTE this is a comment

            00:00:01.000 --> 00:00:02.000
            hello

            00:00:03.000 --> 00:00:04.000
            world
        """.trimIndent()
        val track = SubtitleParser.parseVtt(vtt, "vtt")
        assertEquals(2, track.cues.size)
        assertEquals("hello", track.cues[0].text)
    }

    @Test
    fun overlappingCuesBothKept() {
        val srt = """
            1
            00:00:01,000 --> 00:00:05,000
            first

            2
            00:00:03,000 --> 00:00:04,000
            second overlapping
        """.trimIndent()
        val track = SubtitleParser.parseSrt(srt, "overlap")
        assertEquals(2, track.cues.size)
        assertTrue(track.cues[0].durationMs > 0)
    }

    @Test
    fun garbageBytesNotCrash() {
        val garbage = ByteArray(1024) { (it % 256).toByte() }.toString(Charsets.ISO_8859_1)
        // Must not throw; may return empty.
        val tracks = listOf(
            SubtitleParser.parseSrt(garbage, "garbage-srt"),
            SubtitleParser.parseVtt(garbage, "garbage-vtt"),
            SubtitleParser.parseAss(garbage, "garbage-ass")
        )
        assertTrue(tracks.all { it.cues.size >= 0 })
    }
}
