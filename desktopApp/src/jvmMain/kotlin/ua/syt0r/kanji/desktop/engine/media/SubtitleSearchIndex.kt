package ua.syt0r.kanji.desktop.engine.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

// ============================================
// KAITEYO SUBTITLE SEARCH INDEX
// A library-wide, in-memory index of every
// subtitle track associated with library media
// (the stored track plus companion files). Lets
// the user search a Japanese word across ALL
// media instead of only the loaded episode:
//
//   食べる → Anime A · EP03 · 12:43
//            Anime A · EP07 · 04:12
//            Anime B · EP02 · 18:20
//
// Indexing is incremental and runs off the UI
// thread: files are parsed once per session and
// cached by path + mtime, so a refresh only
// re-parses new or changed files. Search is a
// plain substring pass over the cached cues with
// katakana→hiragana folding, so たべる matches a
// タベル line without any morphological analysis.
// ============================================

/** One search hit: a cue inside one media item's subtitle track. */
@Serializable
data class SubtitleSearchHit(
    val mediaId: String,
    val mediaName: String,
    val mediaPath: String,
    val trackName: String,
    val trackPath: String,
    val cueIndex: Int,
    val cueText: String,
    val startMs: Long,
    val endMs: Long
)

/** Parsed cues of one subtitle file, cached by path + mtime. */
private class CachedSubtitleTrack(
    val path: String,
    val mtimeMs: Long,
    val trackName: String,
    val cues: List<SubtitleCue>
)

class SubtitleSearchIndex(private val library: MediaLibrary) {

    /** Subtitle files currently indexed, keyed by absolute path. */
    private val trackCache = mutableStateMapOf<String, CachedSubtitleTrack>()

    var indexing by mutableStateOf(false)
        private set
    var indexedFiles by mutableStateOf(0)
        private set
    var indexedMediaCount by mutableStateOf(0)
        private set
    var lastMessage by mutableStateOf("")
        private set

    private val cancelled = AtomicBoolean(false)
    private var worker: Thread? = null

    /** Number of cues currently indexed (for the UI status line). */
    val cueCount: Int get() = trackCache.values.sumOf { it.cues.size }

    val isIdle: Boolean get() = !indexing

    // ------------------------------------------------------------
    // Indexing
    // ------------------------------------------------------------

    /**
     * Parse every subtitle file referenced by the library (stored track +
     * companion files). Incremental: files already cached with an unchanged
     * mtime are kept untouched; only new or changed files are re-parsed.
     * Runs on a background thread and never blocks the UI. Safe to call
     * repeatedly — it is cheap when nothing changed.
     */
    fun refreshAsync() {
        if (indexing) return
        cancelled.set(false)
        indexing = true
        worker = thread(name = "kaiteyo-subtitle-index", isDaemon = true) {
            try {
                val wanted = LinkedHashSet<String>()
                var mediaWithTracks = 0
                library.items.toList().forEach { item ->
                    if (item.isRemote) return@forEach
                    val candidates = buildList {
                        if (item.subtitlePath.isNotBlank()) add(item.subtitlePath)
                        addAll(library.findCompanionSubtitle(File(item.path)).map { it.absolutePath })
                    }.distinct()
                    if (candidates.isNotEmpty()) mediaWithTracks++
                    candidates.forEach { path ->
                        if (cancelled.get()) return@forEach
                        wanted.add(path)
                        val file = File(path)
                        if (!file.isFile) return@forEach
                        val cached = trackCache[path]
                        if (cached != null && cached.mtimeMs == file.lastModified()) return@forEach
                        runCatching {
                            val parsed = SubtitleParser.parse(file, file.nameWithoutExtension)
                            if (parsed.cues.isNotEmpty()) {
                                trackCache[path] = CachedSubtitleTrack(path, file.lastModified(), parsed.name, parsed.cues)
                            }
                        }
                    }
                }
                // Drop tracks whose files are no longer referenced by the library.
                trackCache.keys.toList().forEach { path ->
                    if (path !in wanted) trackCache.remove(path)
                }
                indexedFiles = trackCache.size
                indexedMediaCount = mediaWithTracks
                lastMessage = if (indexedFiles == 0) {
                    "No subtitle tracks found in the library — add media with subtitles or drop .srt/.ass files next to your videos"
                } else {
                    "$indexedFiles subtitle file(s) indexed across $indexedMediaCount media item(s) · $cueCount cues"
                }
            } catch (e: Exception) {
                lastMessage = "Indexing failed: ${e.message?.take(80) ?: e.javaClass.simpleName}"
            } finally {
                indexing = false
            }
        }
    }

    fun cancel() {
        cancelled.set(true)
    }

    fun clear() {
        cancelled.set(true)
        trackCache.clear()
        indexedFiles = 0
        indexedMediaCount = 0
        lastMessage = ""
    }

    // ------------------------------------------------------------
    // Search
    // ------------------------------------------------------------

    /**
     * Find every cue containing [query] across the indexed tracks.
     * Matching is case-insensitive and folds katakana→hiragana (so たべる
     * finds a タベル line); no morphological analysis is implied. Each hit
     * carries the exact media, track and timestamp to jump to.
     */
    fun search(query: String, limit: Int = 500): List<SubtitleSearchHit> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        val qLower = q.lowercase()
        val qKana = kanaFold(qLower)
        val hits = ArrayList<SubtitleSearchHit>()
        library.items.toList().forEach { item ->
            if (item.isRemote) return@forEach
            subtitleTracksFor(item).forEach { (trackPath, track) ->
                track.cues.forEachIndexed { index, cue ->
                    val text = cue.text
                    val tLower = text.lowercase()
                    if (tLower.contains(qLower) || kanaFold(tLower).contains(qKana)) {
                        hits.add(
                            SubtitleSearchHit(
                                mediaId = item.id,
                                mediaName = item.name,
                                mediaPath = item.path,
                                trackName = track.trackName,
                                trackPath = trackPath,
                                cueIndex = index,
                                cueText = text,
                                startMs = cue.startMs,
                                endMs = cue.endMs
                            )
                        )
                    }
                }
            }
        }
        // Deterministic order: media name, then timestamp.
        return hits.sortedWith(compareBy({ it.mediaName.lowercase() }, { it.startMs })).take(limit)
    }

    /** Tracks available for an item, stable order (stored track first, then companions). */
    private fun subtitleTracksFor(item: MediaItem): List<Pair<String, CachedSubtitleTrack>> {
        val paths = buildList {
            if (item.subtitlePath.isNotBlank()) add(item.subtitlePath)
            library.findCompanionSubtitle(File(item.path)).forEach { add(it.absolutePath) }
        }.distinct()
        return paths.mapNotNull { path -> trackCache[path]?.let { path to it } }
    }

    companion object {
        /** Fold katakana to hiragana (search たべる matches タベル); other chars pass through. */
        fun kanaFold(text: String): String = buildString(text.length) {
            text.forEach { c ->
                val code = c.code
                append(if (code in 0x30A1..0x30F6) (code - 0x60).toChar() else c)
            }
        }
    }
}
