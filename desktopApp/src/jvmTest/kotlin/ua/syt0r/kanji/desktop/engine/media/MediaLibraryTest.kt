package ua.syt0r.kanji.desktop.engine.media

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MediaLibraryTest {

    private fun tempDir(): File {
        val dir = File.createTempFile("kaiteyo-media", "").let { it.delete(); File(it.parentFile, "kaiteyo-media-test-${System.nanoTime()}") }
        dir.mkdirs()
        return dir
    }

    @Test
    fun addFileClassifiesByExtension() {
        val library = MediaLibrary(tempDir())
        val video = File.createTempFile("episode", ".mkv")
        val audio = File.createTempFile("track", ".mp3")
        val subtitle = File.createTempFile("episode", ".srt")
        try {
            val item = library.addFile(video)
            assertNotNull(item)
            assertEquals(MediaKind.Video, item.kind)
            assertEquals(MediaKind.Audio, library.addFile(audio)?.kind)
            // Subtitle files are not added as media items.
            assertEquals(null, library.addFile(subtitle))
        } finally {
            video.delete(); audio.delete(); subtitle.delete()
        }
    }

    @Test
    fun progressAndHistoryPersist() {
        val dir = tempDir()
        val library = MediaLibrary(dir)
        val video = File.createTempFile("episode", ".mp4")
        try {
            val item = library.addFile(video)!!
            library.updateProgress(item.id, 120_000, 600_000)
            library.recordHistory(library.item(item.id)!!, subtitleUsed = "episode.ja.srt", language = "ja")

            // Reload from disk — state must survive.
            val reloaded = MediaLibrary(dir)
            val restored = reloaded.item(item.id)
            assertNotNull(restored)
            assertEquals(120_000, restored.lastPositionMs)
            assertEquals(600_000, restored.durationMs)
            assertEquals(1, restored.watchCount)
            assertEquals(1, reloaded.history.size)
            assertEquals("ja", reloaded.history[0].language)
        } finally {
            video.delete()
        }
    }

    @Test
    fun favoritesTagsCollections() {
        val library = MediaLibrary(tempDir())
        val video = File.createTempFile("anime", ".mkv")
        try {
            val item = library.addFile(video)!!
            library.toggleFavorite(item.id)
            library.setTags(item.id, listOf("anime", "n3"))
            library.setCollection(item.id, "Anime")
            val updated = library.item(item.id)!!
            assertTrue(updated.favorite)
            assertEquals(listOf("anime", "n3"), updated.tags)
            assertEquals("Anime", updated.collection)
            assertEquals("Anime", library.byCollection("Anime").first().name)
            assertEquals(1, library.favorites().size)
        } finally {
            video.delete()
        }
    }

    @Test
    fun continueWatchingExcludesCompleted() {
        val library = MediaLibrary(tempDir())
        val a = File.createTempFile("a", ".mp4")
        val b = File.createTempFile("b", ".mp4")
        try {
            val ia = library.addFile(a)!!
            val ib = library.addFile(b)!!
            library.updateProgress(ia.id, 120_000, 600_000)   // 20% → continue
            library.updateProgress(ib.id, 550_000, 600_000)   // ~92% but not completed yet
            assertEquals(2, library.continueWatching().size)
            library.updateProgress(ib.id, 600_000, 600_000)   // completed
            assertEquals(1, library.continueWatching().size)
        } finally {
            a.delete(); b.delete()
        }
    }

    @Test
    fun searchMatchesNameAndTags() {
        val library = MediaLibrary(tempDir())
        val video = File.createTempFile("shingeki", ".mkv")
        try {
            val item = library.addFile(video)!!
            library.setTags(item.id, listOf("anime"))
            assertEquals(1, library.search("shingeki").size)
            assertEquals(1, library.search("anime").size)
            assertEquals(0, library.search("zzz").size)
        } finally {
            video.delete()
        }
    }
}
