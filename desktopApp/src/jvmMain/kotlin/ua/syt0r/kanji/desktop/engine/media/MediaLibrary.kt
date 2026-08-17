package ua.syt0r.kanji.desktop.engine.media

import androidx.compose.runtime.mutableStateListOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

// ============================================
// KAITEYO MEDIA LIBRARY
// Persistent catalog of local media: files and
// watched folders, per-item progress with resume
// positions, watch history, favorites, tags and
// user collections. All state is Compose-reactive
// and persisted as JSON (never binaries in a DB).
// ============================================

@Serializable
data class MediaItem(
    val id: String,
    val path: String,
    val name: String,
    val kind: MediaKind,
    val sizeBytes: Long = 0,
    /** True for network sources (http/https) — never treated as a local file. */
    val isRemote: Boolean = false,
    val durationMs: Long = 0,
    val addedAt: String = "",
    val lastPositionMs: Long = 0,
    val lastWatchedAt: String = "",
    val watchCount: Int = 0,
    val completed: Boolean = false,
    val favorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val collection: String = "",
    val subtitlePath: String = "",
    val episode: String = "",
    /** User-entered comprehension estimate, 0 = unset, 1..5. Never computed. */
    val comprehension: Int = 0,
    /** Free-form user note about this media. */
    val note: String = ""
) {
    val displayName: String get() = name

    /** Fraction 0..1 of the media already watched (for resume/continue UI). */
    val progressFraction: Float
        get() = if (durationMs <= 0) 0f else (lastPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

@Serializable
data class MediaFolder(
    val path: String,
    val includeSubdirs: Boolean = true,
    val addedAt: String = ""
)

@Serializable
data class WatchHistoryEntry(
    val mediaId: String,
    val path: String,
    val title: String,
    val positionMs: Long,
    val durationMs: Long,
    val percentage: Float,
    val watchedAt: String,
    val subtitleUsed: String = "",
    val language: String = ""
)

/** A user-named, persistent playlist referencing library items by id. */
@Serializable
data class MediaPlaylist(
    val id: String,
    val name: String,
    val itemIds: List<String> = emptyList(),
    val createdAt: String = "",
    /** Id of the [PlaylistFolder] this playlist lives in ("" = library root). */
    val folderId: String = "",
    /** Pinned playlists sort above unpinned ones in lists. */
    val favorite: Boolean = false
) {
    fun renamed(newName: String) = copy(name = newName)
    fun withItem(itemId: String) = copy(itemIds = itemIds + itemId)
    fun withoutItem(itemId: String) = copy(itemIds = itemIds.filterNot { it == itemId })
    fun withOrder(newOrder: List<String>) = copy(itemIds = newOrder)
    fun inFolder(folderId: String) = copy(folderId = folderId)
    fun withFavorite(value: Boolean) = copy(favorite = value)
}

/**
 * A user-named folder that organizes playlists (nested via [parentId]).
 * Persisted in the library file; deleting a folder moves its playlists and
 * child folders back up one level — nothing is lost.
 */
@Serializable
data class PlaylistFolder(
    val id: String,
    val name: String,
    val parentId: String = "",
    val createdAt: String = ""
)

@Serializable
private data class LibraryDto(
    val items: List<MediaItem> = emptyList(),
    val folders: List<MediaFolder> = emptyList(),
    val playlists: List<MediaPlaylist> = emptyList(),
    val playlistFolders: List<PlaylistFolder> = emptyList()
)

@Serializable
private data class HistoryDto(val entries: List<WatchHistoryEntry> = emptyList())

class MediaLibrary(
    private val directory: File = File(System.getProperty("user.home"), ".kaiteyo/media")
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val libraryFile: File get() = File(directory, "library.json")
    private val historyFile: File get() = File(directory, "history.json")

    val items = mutableStateListOf<MediaItem>()
    val folders = mutableStateListOf<MediaFolder>()
    val history = mutableStateListOf<WatchHistoryEntry>()
    val playlists = mutableStateListOf<MediaPlaylist>()
    val playlistFolders = mutableStateListOf<PlaylistFolder>()

    /** Common user-facing collections, plus any custom ones found in items. */
    val defaultCollections = listOf("Anime", "Movies", "TV", "YouTube", "Music", "Audiobooks", "Other")
    val allCollections: List<String>
        get() = (defaultCollections + items.map { it.collection }.filter { it.isNotBlank() }).distinct()

    init {
        directory.mkdirs()
        load()
    }

    // ------------------------------------------------------------
    // Items
    // ------------------------------------------------------------

    fun addFile(file: File, durationMs: Long = 0): MediaItem? {
        val kind = MediaKind.of(file)
        if (kind == null) return null
        val existing = items.firstOrNull { it.path == file.absolutePath }
        if (existing != null) return existing
        val item = MediaItem(
            id = "media-${file.absolutePath.hashCode().toUInt().toString(16)}-${System.currentTimeMillis() % 100000}",
            path = file.absolutePath,
            name = file.name,
            kind = kind,
            sizeBytes = file.length(),
            durationMs = durationMs,
            addedAt = kotlinx.datetime.Clock.System.now().toString(),
            subtitlePath = findCompanionSubtitle(file).firstOrNull()?.absolutePath ?: "",
            episode = detectEpisode(file.name)
        )
        items.add(item)
        save()
        return item
    }

    fun upsert(item: MediaItem) {
        val idx = items.indexOfFirst { it.id == item.id }
        if (idx >= 0) items[idx] = item else items.add(item)
        save()
    }

    fun itemByPath(path: String): MediaItem? = items.firstOrNull { it.path == path }

    fun item(id: String): MediaItem? = items.firstOrNull { it.id == id }

    /** True when the item's file is still where the library thinks it is. */
    fun fileExists(item: MediaItem): Boolean = item.isRemote || File(item.path).exists()

    /**
     * Add a network media source (http/https stream) to the library.
     * The URL is the item's path; the remote flag keeps every file-based
     * code path (thumbnails, relink, companion subtitles) away from it.
     */
    fun addRemote(url: String, name: String, kind: MediaKind): MediaItem {
        val existing = items.firstOrNull { it.path == url }
        if (existing != null) return existing
        val item = MediaItem(
            id = "media-${url.hashCode().toUInt().toString(16)}-${System.currentTimeMillis() % 100000}",
            path = url,
            name = name,
            kind = kind,
            isRemote = true,
            addedAt = kotlinx.datetime.Clock.System.now().toString()
        )
        items.add(item)
        save()
        return item
    }

    /**
     * Point an item at a new file (moved drives, renamed folders, swapped
     * library root). The item keeps its identity, history, bookmarks and
     * mined-card links; only path-dependent fields are refreshed.
     */
    fun relink(id: String, newFile: File): MediaItem? {
        val idx = items.indexOfFirst { it.id == id }
        if (idx == -1) return null
        if (items[idx].isRemote) return null
        val updated = items[idx].copy(
            path = newFile.absolutePath,
            name = newFile.name,
            sizeBytes = newFile.length(),
            episode = detectEpisode(newFile.name),
            subtitlePath = findCompanionSubtitle(newFile).firstOrNull()?.absolutePath ?: ""
        )
        items[idx] = updated
        history.forEachIndexed { h, entry ->
            if (entry.mediaId == id) history[h] = entry.copy(path = newFile.absolutePath, title = newFile.name)
        }
        save()
        return updated
    }

    /** Series key: the parent folder plus the name with its episode marker removed. */
    fun seriesKey(item: MediaItem): String {
        val folder = File(item.path).parentFile?.absolutePath ?: ""
        val stripped = item.name
            .replace(Regex("(?i)\\bs(\\d{1,2})[\\s._-]*e(\\d{1,3})\\b"), "")
            .replace(Regex("(?i)\\b(\\d{1,2})x(\\d{1,3})\\b"), "")
            .replace(Regex("(?i)\\b(?:ep|episode|ep)\\s*\\.?\\s*(\\d{1,3})\\b"), "")
            .replace(Regex("第\\s*(\\d{1,3})\\s*話"), "")
            .replace(Regex("[\\[(]\\s*(\\d{1,3})\\s*[\\])]"), "")
            .trim()
        return "$folder|$stripped"
    }

    /** Numeric episode number from an episode tag (S01E05 → 5, 第3話 → 3, EP12 → 12). */
    fun episodeNumber(tag: String): Int {
        if (tag.isBlank()) return 0
        return Regex("(\\d+)").find(tag)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    /** The next item in the same series (same folder + base name), ordered by episode. */
    fun nextEpisode(item: MediaItem): MediaItem? {
        val key = seriesKey(item)
        val num = episodeNumber(item.episode)
        return items
            .filter { it.id != item.id && seriesKey(it) == key && episodeNumber(it.episode) > 0 }
            .sortedBy { episodeNumber(it.episode) }
            .firstOrNull { episodeNumber(it.episode) > num }
    }

    fun removeItem(id: String, forgetHistory: Boolean = false) {
        items.removeAll { it.id == id }
        if (forgetHistory) history.removeAll { it.mediaId == id }
        save()
    }

    /** Auto-discover subtitle files that belong to a media file. */
    fun findCompanionSubtitle(mediaFile: File): List<File> {
        val base = mediaFile.nameWithoutExtension
        val dir = mediaFile.parentFile ?: return emptyList()
        val extensions = setOf("srt", "ass", "ssa", "vtt")
        val direct = dir.listFiles()?.filter { f ->
            f.extension.lowercase() in extensions && f.nameWithoutExtension == base
        } ?: emptyList()
        // episode01.mkv  +  episode01.jpn.ass / episode01.en.srt
        val prefixed = dir.listFiles()?.filter { f ->
            f.extension.lowercase() in extensions &&
                f.nameWithoutExtension.startsWith(base + ".")
        } ?: emptyList()
        return (direct + prefixed).sortedBy { it.name }
    }

    // ------------------------------------------------------------
    // Progress / history
    // ------------------------------------------------------------

    fun updateProgress(itemId: String, positionMs: Long, durationMs: Long) {
        val idx = items.indexOfFirst { it.id == itemId }
        if (idx == -1) return
        val current = items[idx]
        val completed = durationMs > 0 && positionMs >= durationMs - 15000
        items[idx] = current.copy(
            lastPositionMs = positionMs.coerceAtLeast(0),
            durationMs = durationMs.coerceAtLeast(current.durationMs),
            lastWatchedAt = kotlinx.datetime.Clock.System.now().toString(),
            completed = current.completed || completed
        )
    }

    /** One more completed watch session for an item. */
    fun bumpWatchCount(itemId: String) {
        val idx = items.indexOfFirst { it.id == itemId }
        if (idx == -1) return
        items[idx] = items[idx].copy(watchCount = items[idx].watchCount + 1)
        save()
    }

    fun recordHistory(item: MediaItem, subtitleUsed: String = "", language: String = "") {
        val entry = WatchHistoryEntry(
            mediaId = item.id,
            path = item.path,
            title = item.name,
            positionMs = item.lastPositionMs,
            durationMs = item.durationMs,
            percentage = item.progressFraction,
            watchedAt = kotlinx.datetime.Clock.System.now().toString(),
            subtitleUsed = subtitleUsed,
            language = language
        )
        history.removeAll { it.mediaId == item.id && it.path == item.path }
        history.add(0, entry)
        while (history.size > 400) history.removeAt(history.lastIndex)
        save()
    }

    fun forgetHistory() {
        items.forEachIndexed { i, item ->
            items[i] = item.copy(lastPositionMs = 0, lastWatchedAt = "", watchCount = 0, completed = false)
        }
        history.clear()
        save()
    }

    // ------------------------------------------------------------
    // Folders
    // ------------------------------------------------------------

    fun addFolder(path: String, includeSubdirs: Boolean = true) {
        val p = File(path).absolutePath
        if (folders.none { it.path == p }) {
            folders.add(MediaFolder(p, includeSubdirs, kotlinx.datetime.Clock.System.now().toString()))
        }
        save()
    }

    fun removeFolder(path: String) {
        folders.removeAll { it.path == path }
        save()
    }

    // ------------------------------------------------------------
    // Playlists
    // ------------------------------------------------------------

    /** Create a named playlist; returns its id (or the existing one if the name is taken). */
    fun createPlaylist(name: String, folderId: String = ""): String {
        val clean = name.trim()
        if (clean.isBlank()) return ""
        playlists.firstOrNull { it.name.equals(clean, ignoreCase = true) }?.let { return it.id }
        val id = "playlist-${System.currentTimeMillis()}"
        playlists.add(
            MediaPlaylist(
                id = id,
                name = clean,
                createdAt = kotlinx.datetime.Clock.System.now().toString(),
                folderId = folderId
            )
        )
        save()
        return id
    }

    /** Copy a playlist (name + items, no folder); returns the new id or null. */
    fun duplicatePlaylist(id: String): String? {
        val source = playlists.firstOrNull { it.id == id } ?: return null
        val copy = MediaPlaylist(
            id = "playlist-${System.currentTimeMillis()}",
            name = "${source.name} (copy)",
            itemIds = source.itemIds.toList(),
            createdAt = kotlinx.datetime.Clock.System.now().toString(),
            folderId = source.folderId,
            favorite = false
        )
        playlists.add(copy)
        save()
        return copy.id
    }

    /** Pin/unpin a playlist (favorites sort first). */
    fun togglePlaylistFavorite(id: String) {
        val idx = playlists.indexOfFirst { it.id == id }
        if (idx == -1) return
        playlists[idx] = playlists[idx].withFavorite(!playlists[idx].favorite)
        save()
    }

    /** Move a playlist into a folder ("" moves it back to the library root). */
    fun movePlaylistToFolder(id: String, folderId: String) {
        val idx = playlists.indexOfFirst { it.id == id }
        if (idx == -1) return
        if (folderId.isNotBlank() && playlistFolder(folderId) == null) return
        playlists[idx] = playlists[idx].inFolder(folderId)
        save()
    }

    /** Playlists directly inside [folderId] ("" = root), favorites first. */
    fun playlistsInFolder(folderId: String): List<MediaPlaylist> =
        playlists.filter { it.folderId == folderId }
            .sortedBy { !it.favorite }
            .sortedBy { it.name.lowercase() }

    // ------------------------------------------------------------
    // Playlist folders
    // ------------------------------------------------------------

    /** Create a folder; returns its id (or the existing one if the name is taken). */
    fun createPlaylistFolder(name: String, parentId: String = ""): String {
        val clean = name.trim()
        if (clean.isBlank()) return ""
        playlistFolders.firstOrNull { it.name.equals(clean, ignoreCase = true) && it.parentId == parentId }
            ?.let { return it.id }
        val id = "plfolder-${System.currentTimeMillis()}"
        playlistFolders.add(
            PlaylistFolder(
                id = id,
                name = clean,
                parentId = parentId,
                createdAt = kotlinx.datetime.Clock.System.now().toString()
            )
        )
        save()
        return id
    }

    fun renamePlaylistFolder(id: String, newName: String): Boolean {
        val idx = playlistFolders.indexOfFirst { it.id == id }
        if (idx == -1) return false
        val clean = newName.trim()
        if (clean.isBlank()) return false
        playlistFolders[idx] = playlistFolders[idx].copy(name = clean)
        save()
        return true
    }

    /**
     * Delete a folder: playlists inside it return to the library root and
     * child folders are re-parented one level up — nothing is ever lost.
     */
    fun deletePlaylistFolder(id: String) {
        val target = playlistFolder(id) ?: return
        playlists.forEachIndexed { i, p ->
            if (p.folderId == id) playlists[i] = p.inFolder(target.parentId)
        }
        playlistFolders.forEachIndexed { i, f ->
            if (f.parentId == id) playlistFolders[i] = f.copy(parentId = target.parentId)
        }
        playlistFolders.removeAll { it.id == id }
        save()
    }

    fun playlistFolder(id: String): PlaylistFolder? = playlistFolders.firstOrNull { it.id == id }

    /** Immediate child folders of [parentId], sorted by name. */
    fun folderChildren(parentId: String): List<PlaylistFolder> =
        playlistFolders.filter { it.parentId == parentId }.sortedBy { it.name.lowercase() }

    /** Flat ancestor chain from root to [id] (used for breadcrumbs). */
    fun folderChain(id: String): List<PlaylistFolder> {
        val chain = mutableListOf<PlaylistFolder>()
        var current = playlistFolder(id)
        while (current != null) {
            chain.add(0, current)
            current = current.parentId.takeIf { it.isNotBlank() }?.let(::playlistFolder)
        }
        return chain
    }

    fun renamePlaylist(id: String, newName: String): Boolean {
        val idx = playlists.indexOfFirst { it.id == id }
        if (idx == -1) return false
        val clean = newName.trim()
        if (clean.isBlank()) return false
        if (playlists.any { it.id != id && it.name.equals(clean, ignoreCase = true) }) return false
        playlists[idx] = playlists[idx].renamed(clean)
        save()
        return true
    }

    fun deletePlaylist(id: String) {
        playlists.removeAll { it.id == id }
        save()
    }

    /** Add an item id to a playlist (deduplicated). */
    fun addToPlaylist(playlistId: String, itemId: String) {
        val idx = playlists.indexOfFirst { it.id == playlistId }
        if (idx == -1) return
        if (itemId in playlists[idx].itemIds) return
        playlists[idx] = playlists[idx].withItem(itemId)
        save()
    }

    fun removeFromPlaylist(playlistId: String, itemId: String) {
        val idx = playlists.indexOfFirst { it.id == playlistId }
        if (idx == -1) return
        playlists[idx] = playlists[idx].withoutItem(itemId)
        save()
    }

    /** Move an item within a playlist; returns the new order. */
    fun reorderPlaylist(playlistId: String, itemId: String, toIndex: Int): List<String> {
        val idx = playlists.indexOfFirst { it.id == playlistId }
        if (idx == -1) return emptyList()
        val current = playlists[idx].itemIds
        val from = current.indexOf(itemId)
        if (from == -1) return current
        val reordered = current.toMutableList().apply { removeAt(from) }
        val target = toIndex.coerceIn(0, reordered.size)
        reordered.add(target, itemId)
        playlists[idx] = playlists[idx].withOrder(reordered)
        save()
        return reordered
    }

    /** Resolve a playlist to its current items (missing library entries are skipped). */
    fun playlistItems(playlist: MediaPlaylist): List<MediaItem> =
        playlist.itemIds.mapNotNull(::item)

    fun playlist(id: String): MediaPlaylist? = playlists.firstOrNull { it.id == id }

    // ------------------------------------------------------------
    // Playlist import / export (M3U — paths + EXTINF titles)
    // ------------------------------------------------------------

    /**
     * Write a playlist to an M3U file. Media is referenced by absolute path —
     * the library never copies files, and neither does the export.
     */
    fun exportPlaylistM3u(playlist: MediaPlaylist, target: File): Boolean {
        val items = playlistItems(playlist)
        return runCatching {
            val lines = buildList {
                add("#EXTM3U")
                items.forEach { item ->
                    add("#EXTINF:${item.durationMs / 1000},${item.name}")
                    add(item.path)
                }
            }
            target.writeText(lines.joinToString("\n") + "\n")
            true
        }.getOrDefault(false)
    }

    /**
     * Import an M3U playlist: media paths become library items (added when
     * the file exists and is not indexed yet, with relative paths resolved
     * against the playlist file) and are joined into a playlist named after
     * the file. Returns the new playlist id, or "" when nothing was valid.
     */
    fun importPlaylistM3u(file: File, name: String = file.nameWithoutExtension): String {
        if (!file.isFile) return ""
        val id = createPlaylist(name)
        if (id.isBlank()) return ""
        val paths = runCatching { file.readLines() }
            .getOrDefault(emptyList())
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
        paths.forEach { raw ->
            val path = raw.substringBefore('#').trim()
            if (path.isBlank()) return@forEach
            val f = File(path)
            val resolved = when {
                f.exists() -> f
                !f.isAbsolute && file.parentFile != null -> File(file.parentFile, path)
                else -> f
            }
            val item = if (resolved.exists()) addFile(resolved) else itemByPath(resolved.absolutePath)
            item?.let { addToPlaylist(id, it.id) }
        }
        return id
    }

    // ------------------------------------------------------------
    // Tags / favorites / collections
    // ------------------------------------------------------------

    fun toggleFavorite(itemId: String) {
        val idx = items.indexOfFirst { it.id == itemId }
        if (idx == -1) return
        items[idx] = items[idx].copy(favorite = !items[idx].favorite)
        save()
    }

    fun setTags(itemId: String, tags: List<String>) {
        val idx = items.indexOfFirst { it.id == itemId }
        if (idx == -1) return
        items[idx] = items[idx].copy(tags = tags.distinct())
        save()
    }

    fun setCollection(itemId: String, collection: String) {
        val idx = items.indexOfFirst { it.id == itemId }
        if (idx == -1) return
        items[idx] = items[idx].copy(collection = collection)
        save()
    }

    /** All tag values currently used across the library (sorted, distinct). */
    fun allTags(): List<String> =
        items.flatMap { it.tags }.distinct().sorted()

    /** Toggle the "watch-later" convenience tag on an item. */
    fun toggleWatchLater(itemId: String) {
        val idx = items.indexOfFirst { it.id == itemId }
        if (idx == -1) return
        val tags = items[idx].tags.toMutableList()
        if ("watch-later" in tags) tags.remove("watch-later") else tags.add("watch-later")
        items[idx] = items[idx].copy(tags = tags.distinct())
        save()
    }

    fun watchLater(): List<MediaItem> = items.filter { "watch-later" in it.tags }

    /** Is the item tagged watch-later? */
    fun isWatchLater(itemId: String): Boolean =
        items.firstOrNull { it.id == itemId }?.tags?.contains("watch-later") == true

    fun setSubtitle(itemId: String, subtitlePath: String) {
        val idx = items.indexOfFirst { it.id == itemId }
        if (idx == -1) return
        items[idx] = items[idx].copy(subtitlePath = subtitlePath)
        save()
    }

    /** User-entered comprehension rating (0 clears, 1..5 sets). */
    fun setComprehension(itemId: String, rating: Int) {
        val idx = items.indexOfFirst { it.id == itemId }
        if (idx == -1) return
        items[idx] = items[idx].copy(comprehension = rating.coerceIn(0, 5))
        save()
    }

    fun setNote(itemId: String, note: String) {
        val idx = items.indexOfFirst { it.id == itemId }
        if (idx == -1) return
        items[idx] = items[idx].copy(note = note)
        save()
    }

    // ------------------------------------------------------------
    // Views
    // ------------------------------------------------------------

    fun continueWatching(limit: Int = 40): List<MediaItem> =
        items.filter { it.lastWatchedAt.isNotBlank() && !it.completed && it.progressFraction in 0.01f..0.99f }
            .sortedByDescending { it.lastWatchedAt }
            .take(limit)

    fun recent(limit: Int = 60): List<MediaItem> =
        items.sortedByDescending { it.lastWatchedAt.ifBlank { it.addedAt } }.take(limit)

    fun favorites(): List<MediaItem> = items.filter { it.favorite }

    fun recentlyAdded(limit: Int = 60): List<MediaItem> =
        items.sortedByDescending { it.addedAt }.take(limit)

    fun byCollection(name: String): List<MediaItem> =
        items.filter { it.collection == name }.sortedBy { it.name.lowercase() }

    fun search(query: String): List<MediaItem> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return items.sortedBy { it.name.lowercase() }
        return items.filter {
            it.name.lowercase().contains(q) || it.path.lowercase().contains(q) ||
                it.tags.any { t -> t.lowercase().contains(q) } || it.collection.lowercase().contains(q)
        }.sortedBy { it.name.lowercase() }
    }

    // ------------------------------------------------------------
    // Sorting / filtering
    // ------------------------------------------------------------

    /** Sort keys offered by the Media Centre browse view. */
    enum class MediaSortMode(val label: String) {
        Title("Title"),
        DateAdded("Date added"),
        RecentlyWatched("Recently watched"),
        Duration("Duration"),
        Progress("Progress"),
        FileSize("File size"),
        LastModified("Last modified")
    }

    /** Sort a list of items by [mode] (most useful orders descending). */
    fun sortItems(list: List<MediaItem>, mode: MediaSortMode): List<MediaItem> = when (mode) {
        MediaSortMode.Title -> list.sortedBy { it.name.lowercase() }
        MediaSortMode.DateAdded -> list.sortedByDescending { it.addedAt }
        MediaSortMode.RecentlyWatched -> list.sortedByDescending { it.lastWatchedAt.ifBlank { it.addedAt } }
        MediaSortMode.Duration -> list.sortedByDescending { it.durationMs }
        MediaSortMode.Progress -> list.sortedByDescending { it.progressFraction }
        MediaSortMode.FileSize -> list.sortedByDescending { it.sizeBytes }
        MediaSortMode.LastModified -> list.sortedByDescending {
            if (it.isRemote) 0L else java.io.File(it.path).lastModified()
        }
    }

    /** Watch-state filter presets for the browse view. */
    enum class MediaWatchFilter(val label: String) {
        Any("Any"),
        Unwatched("Unwatched"),
        InProgress("In progress"),
        Completed("Completed")
    }

    fun matchesWatchFilter(item: MediaItem, filter: MediaWatchFilter): Boolean = when (filter) {
        MediaWatchFilter.Any -> true
        MediaWatchFilter.Unwatched -> item.watchCount == 0 && item.lastWatchedAt.isBlank()
        MediaWatchFilter.InProgress -> item.lastWatchedAt.isNotBlank() && !item.completed && item.progressFraction in 0.01f..0.99f
        MediaWatchFilter.Completed -> item.completed
    }

    // ------------------------------------------------------------
    // Folder browsing (explorer)
    // ------------------------------------------------------------

    /** Items whose file lives directly inside [dirPath]. */
    fun itemsDirectlyUnder(dirPath: String): List<MediaItem> {
        val prefix = dirPath.trimEnd('/')
        return items.filter { item ->
            if (item.isRemote) return@filter false
            val parent = java.io.File(item.path).parentFile?.absolutePath?.trimEnd('/')
            parent == prefix
        }.sortedBy { it.name.lowercase() }
    }

    /**
     * Immediate subfolders of [dirPath] that contain media, derived from the
     * library (never a filesystem walk) — one level at a time.
     */
    fun subfoldersUnder(dirPath: String): List<String> {
        val prefix = dirPath.trimEnd('/')
        val found = linkedSetOf<String>()
        items.forEach { item ->
            if (item.isRemote) return@forEach
            val parent = java.io.File(item.path).parentFile ?: return@forEach
            if (parent.absolutePath.trimEnd('/') == prefix) return@forEach
            val grandParent = parent.parentFile
            if (grandParent?.absolutePath?.trimEnd('/') == prefix) found.add(parent.absolutePath)
        }
        return found.sortedBy { it.substringAfterLast('/') }
    }

    /** Leaf name of a folder path for breadcrumbs / tiles. */
    fun folderName(path: String): String =
        java.io.File(path).name.ifBlank { path }

    /** Watched folders whose directory still exists (browse roots). */
    fun existingWatchedFolders(): List<String> =
        folders.map { it.path }.filter { java.io.File(it).isDirectory }.sorted()

    fun totalWatchTimeMs(): Long = history.sumOf { (it.durationMs * it.percentage).toLong() }

    /**
     * Detect a series/season/episode marker from a filename without requiring
     * any metadata: S01E05, 1x05, EP12, episode 3, [05], 第5話. Returns "" when
     * nothing obvious is found (users can still organize manually).
     */
    fun detectEpisode(fileName: String): String {
        val name = fileName.replace('_', ' ')
        Regex("(?i)\\bs(\\d{1,2})[\\s._-]*e(\\d{1,3})\\b").find(name)?.let { m ->
            return "S${m.groupValues[1].padStart(2, '0')}E${m.groupValues[2].padStart(2, '0')}"
        }
        Regex("(?i)\\b(\\d{1,2})x(\\d{1,3})\\b").find(name)?.let { m ->
            return "S${m.groupValues[1].padStart(2, '0')}E${m.groupValues[2].padStart(2, '0')}"
        }
        Regex("(?i)\\b(?:ep|episode|ep)\\s*\\.?\\s*(\\d{1,3})\\b").find(name)?.let { m ->
            return "EP${m.groupValues[1].padStart(2, '0')}"
        }
        Regex("第\\s*(\\d{1,3})\\s*話").find(name)?.let { m ->
            return "第${m.groupValues[1]}話"
        }
        Regex("[\\[(]\\s*(\\d{1,3})\\s*[\\])]").find(name)?.let { m ->
            return "EP${m.groupValues[1].padStart(2, '0')}"
        }
        return ""
    }

    /**
     * Best-effort series name from a filename: strips episode markers
     * (S01E05, 1x05, EP12, 第5話, [05]) and release tags (1080p, web-dl,
     * BD, subs, …). Never requires metadata; returns "" when nothing
     * sensible remains (users can still assign a collection manually).
     */
    fun detectSeries(fileName: String): String {
        var name = fileName.replace('_', ' ').trim()
        name = name
            .replace(Regex("(?i)\\bs\\d{1,2}[\\s._-]*e\\d{1,3}\\b"), " ")
            .replace(Regex("(?i)\\b\\d{1,2}x\\d{1,3}\\b"), " ")
            .replace(Regex("(?i)\\b(?:ep|episode)\\s*\\.?\\s*\\d{1,3}\\b"), " ")
            .replace(Regex("第\\s*\\d{1,3}\\s*話"), " ")
            .replace(Regex("[\\[(]\\s*\\d{1,3}\\s*[\\])]"), " ")
        name = name
            .replace(Regex("\\.[A-Za-z0-9]{2,4}$"), " ")
            .replace(
                Regex("(?i)\\b(1080p|720p|2160p|4k|web[ -]?dl|bluray|\\bbd\\b|hdtv|dvdrip|webrip|hevc|x264|x265|aac|flac|subs?|jpn|jap|eng|softsub|hardsub)\\b"),
                " "
            )
            .replace(Regex("(?i)\\[[^\\]]*]\\s*"), " ")
        return name.replace(Regex("\\s+"), " ").trim(' ', '-', '.', '(', ')', '[', ']', '_')
    }

    /** Grouping key for an item: its collection, else a detected series. */
    fun seriesName(item: MediaItem): String =
        item.collection.ifBlank { detectSeries(item.name) }.ifBlank { "Other" }

    /** Items grouped by collection/series, each group sorted by episode. */
    fun seriesGroups(): Map<String, List<MediaItem>> {
        val groups = LinkedHashMap<String, MutableList<MediaItem>>()
        items.forEach { item -> groups.getOrPut(seriesName(item)) { mutableListOf() }.add(item) }
        return groups.mapValues { it.value.sortedBy { i -> i.episode } }
    }

    fun totalWatches(): Int = history.size

    fun watchedMediaCount(): Int = items.count { it.watchCount > 0 }

    // ------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------

    private fun load() {
        if (libraryFile.exists()) {
            runCatching {
                val dto = json.decodeFromString<LibraryDto>(libraryFile.readText())
                items.clear(); items.addAll(dto.items)
                folders.clear(); folders.addAll(dto.folders)
                playlists.clear(); playlists.addAll(dto.playlists)
                playlistFolders.clear(); playlistFolders.addAll(dto.playlistFolders)
            }
        }
        if (historyFile.exists()) {
            runCatching {
                val dto = json.decodeFromString<HistoryDto>(historyFile.readText())
                history.clear(); history.addAll(dto.entries)
            }
        }
    }

    private fun save() {
        runCatching {
            libraryFile.writeText(
                json.encodeToString(
                    LibraryDto(items.toList(), folders.toList(), playlists.toList(), playlistFolders.toList())
                )
            )
            historyFile.writeText(json.encodeToString(HistoryDto(history.toList())))
        }
    }
}
