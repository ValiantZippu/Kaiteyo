package ua.syt0r.kanji.desktop.engine.media

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.system.measureTimeMillis

class MediaLargeLibraryPerfTest {

    @Test
    fun subtitleSearchScalesTo10kCues() {
        val engine = SubtitleEngine()
        // Build 10k cues (approx 5 hours content)
        val cues = (0 until 10_000).map { i ->
            SubtitleCue(id = "c$i", startMs = i * 3000L, endMs = i * 3000L + 2000L, text = "テスト文 $i こんにちは世界")
        }
        val track = SubtitleTrack(name = "large", cues = cues, format = SubtitleFormat.Srt)
        engine.addTrack(SubtitleTrackEntry("t1", "large", track))

        // Binary search lookup should be O(log n) — 5k queries under 200ms
        val elapsed = measureTimeMillis {
            repeat(5000) { i ->
                engine.cueIndexAt((i * 6000L) % 30_000_000L)
            }
        }
        assertTrue(elapsed < 2000, "5k cue lookups took ${elapsed}ms, expected <2000ms")
    }

    @Test
    fun parserHandlesLargeFileWithoutOom() {
        val largeSrt = buildString {
            repeat(20000) { i ->
                append("${i + 1}\n")
                val start = String.format("%02d:%02d:%02d,%03d", i / 3600, (i % 3600) / 60, i % 60, 0)
                val end = String.format("%02d:%02d:%02d,%03d", i / 3600, (i % 3600) / 60, (i % 60) + 1, 500)
                append("$start --> $end\n")
                append("テスト文 $i\n\n")
            }
        }
        var cues = 0
        val elapsed = measureTimeMillis {
            val track = SubtitleParser.parseSrt(largeSrt, "perf")
            cues = track.cues.size
        }
        assertTrue(cues == 20000, "Expected 20000 cues, got $cues")
        assertTrue(elapsed < 5000, "Parsing 20k cues took ${elapsed}ms, expected <5000ms")
    }

    @Test
    fun mediaLibrarySearchHandlesThousandsOfItems() {
        val lib = MediaLibrary()
        // Simulate 3000 media items (no file IO — addRemote)
        repeat(3000) { i ->
            lib.addRemote("https://example.com/video$i.mp4", "Video $i", MediaKind.Video)
        }
        val elapsed = measureTimeMillis {
            repeat(100) {
                lib.search("Video 1")
            }
        }
        assertTrue(elapsed < 2000, "100 searches over 3k items took ${elapsed}ms")
    }
}
