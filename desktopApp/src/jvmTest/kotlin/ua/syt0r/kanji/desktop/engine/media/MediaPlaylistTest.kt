package ua.syt0r.kanji.desktop.engine.media

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MediaPlaylistTest {

    private fun tempDir(): File {
        val dir = File.createTempFile("kaiteyo-playlist", "").let { it.delete(); File(it.parentFile, "kaiteyo-playlist-test-${System.nanoTime()}") }
        dir.mkdirs()
        return dir
    }

    private fun MediaLibrary.withItems(vararg names: String): List<MediaItem> =
        names.map { addFile(File.createTempFile(it, ".mp4"))!! }

    @Test
    fun createRenameDeletePlaylist() {
        val library = MediaLibrary(tempDir())
        val id = library.createPlaylist("Anime favorites")
        assertNotNull(id)
        assertEquals("Anime favorites", library.playlist(id)?.name)
        // Duplicate names collapse to the existing playlist.
        assertEquals(id, library.createPlaylist("anime favorites"))

        assertTrue(library.renamePlaylist(id, "Immersion queue"))
        assertEquals("Immersion queue", library.playlist(id)?.name)
        // Blank / colliding names are rejected.
        assertFalse(library.renamePlaylist(id, "  "))
        assertFalse(library.renamePlaylist(id, "Immersion queue"))

        library.deletePlaylist(id)
        assertEquals(null, library.playlist(id))
    }

    @Test
    fun addRemoveAndResolveItems() {
        val library = MediaLibrary(tempDir())
        val items = library.withItems("ep01", "ep02", "ep03")
        val id = library.createPlaylist("Series")
        items.forEach { library.addToPlaylist(id, it.id) }
        // Duplicate add is a no-op.
        library.addToPlaylist(id, items[0].id)
        assertEquals(3, library.playlist(id)?.itemIds?.size)
        assertEquals(3, library.playlistItems(library.playlist(id)!!).size)

        library.removeFromPlaylist(id, items[1].id)
        assertEquals(listOf(items[0].id, items[2].id), library.playlist(id)?.itemIds)
    }

    @Test
    fun reorderMovesItem() {
        val library = MediaLibrary(tempDir())
        val items = library.withItems("a", "b", "c")
        val id = library.createPlaylist("Order")
        items.forEach { library.addToPlaylist(id, it.id) }

        // Move "a" to the end.
        val reordered = library.reorderPlaylist(id, items[0].id, 2)
        assertEquals(listOf(items[1].id, items[2].id, items[0].id), reordered)
        // Unknown item leaves the order untouched.
        assertEquals(reordered.size, library.reorderPlaylist(id, "nope", 0).size)
    }

    @Test
    fun playlistsPersistAcrossReload() {
        val dir = tempDir()
        val library = MediaLibrary(dir)
        val items = library.withItems("one", "two")
        val id = library.createPlaylist("Persisted")
        items.forEach { library.addToPlaylist(id, it.id) }

        val reloaded = MediaLibrary(dir)
        val restored = reloaded.playlist(id)
        assertNotNull(restored)
        assertEquals("Persisted", restored.name)
        assertEquals(items.map { it.id }, restored.itemIds)
        assertEquals(2, reloaded.playlistItems(restored).size)
    }

    @Test
    fun playlistSkipsMissingLibraryItems() {
        val library = MediaLibrary(tempDir())
        val item = library.withItems("ghost")[0]
        val id = library.createPlaylist("Ghosts")
        library.addToPlaylist(id, item.id)
        library.addToPlaylist(id, "media-missing-id")

        assertEquals(2, library.playlist(id)?.itemIds?.size)
        // Only the item that still exists resolves.
        assertEquals(1, library.playlistItems(library.playlist(id)!!).size)
        assertEquals(item.id, library.playlistItems(library.playlist(id)!!)[0].id)
    }
}
