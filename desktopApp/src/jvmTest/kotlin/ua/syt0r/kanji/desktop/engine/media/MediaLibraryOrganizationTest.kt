package ua.syt0r.kanji.desktop.engine.media

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MediaLibraryOrganizationTest {

    private lateinit var dir: File

    @BeforeTest
    fun setup() {
        dir = Files.createTempDirectory("kaiteyo-mlib-test").toFile()
    }

    @AfterTest
    fun teardown() {
        dir.deleteRecursively()
    }

    // ------------------------------------------------------------
    // Series detection
    // ------------------------------------------------------------

    @Test
    fun detectsSeriesFromFilenames() {
        val lib = MediaLibrary(dir)
        assertEquals("Boku no Hero Academia", lib.detectSeries("Boku no Hero Academia - S01E05 [1080p].mkv"))
        assertEquals("ShingekiNoKyojin", lib.detectSeries("ShingekiNoKyojin_EP12.mkv"))
        assertEquals("鬼滅の刃", lib.detectSeries("第5話 鬼滅の刃.mkv"))
        assertEquals("SeriesX", lib.detectSeries("SeriesX 1x03.mkv"))
        assertEquals("", lib.detectSeries("BD 1080p web-dl.mkv"))
    }

    @Test
    fun groupsItemsBySeriesAndCollection() {
        val lib = MediaLibrary(dir)
        val a = File(dir, "ShowA S01E01.mkv").apply { writeBytes(byteArrayOf(1)) }
        val b = File(dir, "ShowA S01E02.mkv").apply { writeBytes(byteArrayOf(1)) }
        val m = File(dir, "Movie.mkv").apply { writeBytes(byteArrayOf(1)) }
        lib.addFile(a)
        lib.addFile(b)
        lib.addFile(m)

        val groups = lib.seriesGroups()
        assertEquals(2, groups.size)
        assertEquals(2, groups["ShowA"]?.size)
        assertEquals(1, groups["Movie"]?.size)
        assertEquals("EP01", groups["ShowA"]?.first()?.episode)
        assertEquals("EP02", groups["ShowA"]?.last()?.episode)
    }

    @Test
    fun collectionOverridesDetectedSeries() {
        val lib = MediaLibrary(dir)
        val f = File(dir, "ShowA S01E01.mkv").apply { writeBytes(byteArrayOf(1)) }
        val item = lib.addFile(f)!!
        lib.setCollection(item.id, "Anime")
        assertEquals("Anime", lib.seriesName(item))
        assertEquals("Anime", lib.seriesGroups().keys.first())
    }

    // ------------------------------------------------------------
    // Folder management
    // ------------------------------------------------------------

    @Test
    fun foldersCanBeAddedAndRemoved() {
        val lib = MediaLibrary(dir)
        lib.addFolder(dir.absolutePath)
        assertEquals(1, lib.folders.size)
        // Idempotent
        lib.addFolder(dir.absolutePath)
        assertEquals(1, lib.folders.size)
        lib.removeFolder(dir.absolutePath)
        assertEquals(0, lib.folders.size)
    }

    // ------------------------------------------------------------
    // Companion subtitles + episode detection on add
    // ------------------------------------------------------------

    @Test
    fun addFileAttachesCompanionSubtitleAndEpisode() {
        val lib = MediaLibrary(dir)
        val video = File(dir, "episode01.mkv").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        File(dir, "episode01.jpn.srt").writeText("1\n00:00:01,000 --> 00:00:02,000\nテスト")
        val item = lib.addFile(video)
        assertNotNull(item)
        assertEquals("EP01", item.episode)
        assertTrue(item.subtitlePath.endsWith("episode01.jpn.srt"))
    }

    @Test
    fun addFileIsIdempotent() {
        val lib = MediaLibrary(dir)
        val f = File(dir, "movie.mp4").apply { writeBytes(byteArrayOf(1)) }
        val first = lib.addFile(f)
        val second = lib.addFile(f)
        assertEquals(first?.id, second?.id)
        assertEquals(1, lib.items.size)
    }

    // ------------------------------------------------------------
    // Search + views
    // ------------------------------------------------------------

    @Test
    fun searchMatchesNameTagsAndCollection() {
        val lib = MediaLibrary(dir)
        val f = File(dir, "Kimetsu EP01.mkv").apply { writeBytes(byteArrayOf(1)) }
        val item = lib.addFile(f)!!
        lib.setTags(item.id, listOf("anime", "action"))
        assertEquals(1, lib.search("kimetsu").size)
        assertEquals(1, lib.search("action").size)
        assertEquals(0, lib.search("nothing-matches").size)
    }
}
