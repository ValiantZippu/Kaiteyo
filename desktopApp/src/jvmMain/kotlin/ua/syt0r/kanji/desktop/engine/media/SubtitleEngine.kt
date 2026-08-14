package ua.syt0r.kanji.desktop.engine.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

// ============================================
// KAITEYO SUBTITLE ENGINE
// Multi-track subtitle management on top of the
// format parsers: global offset, dual subtitles,
// binary-search cue lookup, cue navigation,
// transcript search/filters and timeline markers.
// ============================================

data class SubtitleTrackEntry(
    val id: String,
    val name: String,
    val track: SubtitleTrack,
    val path: String = "",
    val offsetMs: Long = 0,
    val language: String = ""
) {
    val format: SubtitleFormat get() = track.format
}

/** Filter for the transcript panel. */
enum class TranscriptFilter { All, Unknown, Known, Mined, New }

class SubtitleEngine {

    val tracks = mutableStateListOf<SubtitleTrackEntry>()
    var activeTrackId by mutableStateOf<String?>(null)
    var secondaryTrackId by mutableStateOf<String?>(null)
    var showSecondary by mutableStateOf(false)

    /** Global offset applied to every cue (positive = subtitles appear later). */
    var globalOffsetMs by mutableStateOf(0L)

    /** Independent offset for the secondary (dual-language) track. */
    var secondaryOffsetMs by mutableStateOf(0L)

    val activeTrack: SubtitleTrackEntry? get() = activeTrackId?.let { id -> tracks.firstOrNull { it.id == id } }
    val secondaryTrack: SubtitleTrackEntry? get() = secondaryTrackId?.let { id -> tracks.firstOrNull { it.id == id } }

    // ------------------------------------------------------------
    // Track management
    // ------------------------------------------------------------

    fun loadFile(file: File): Result<SubtitleTrackEntry> = runCatching {
        val parsed = SubtitleParser.parse(file, file.nameWithoutExtension)
        if (parsed.cues.isEmpty()) error("No cues found in ${file.name}")
        val entry = SubtitleTrackEntry(
            id = "sub-${System.currentTimeMillis()}",
            name = file.nameWithoutExtension,
            track = parsed,
            path = file.absolutePath,
            language = detectLanguage(file.name)
        )
        tracks.add(entry)
        activeTrackId = entry.id
        entry
    }

    /**
     * Load a second subtitle file as the dual-language track without touching
     * the active track. Ideal for 日本語 + English pairs.
     */
    fun loadSecondaryFile(file: File): Result<SubtitleTrackEntry> = runCatching {
        val parsed = SubtitleParser.parse(file, file.nameWithoutExtension)
        if (parsed.cues.isEmpty()) error("No cues found in ${file.name}")
        val entry = SubtitleTrackEntry(
            id = "sub-${System.currentTimeMillis()}",
            name = file.nameWithoutExtension,
            track = parsed,
            path = file.absolutePath,
            language = detectLanguage(file.name)
        )
        tracks.add(entry)
        secondaryTrackId = entry.id
        entry
    }

    fun clearSecondary() {
        secondaryTrackId = null
        showSecondary = false
    }

    fun addTrack(entry: SubtitleTrackEntry) {
        tracks.removeAll { it.id == entry.id }
        tracks.add(entry)
        activeTrackId = entry.id
    }

    fun removeTrack(id: String) {
        tracks.removeAll { it.id == id }
        if (activeTrackId == id) activeTrackId = tracks.firstOrNull()?.id
        if (secondaryTrackId == id) secondaryTrackId = null
    }

    fun clear() {
        tracks.clear()
        activeTrackId = null
        secondaryTrackId = null
        globalOffsetMs = 0
        secondaryOffsetMs = 0
    }

    /** Language heuristics from a subtitle filename (jpn/eng/ja/en/…). */
    private fun detectLanguage(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            Regex("(\\b|\\.)(jpn|ja|jp)(\\b|\\.)").containsMatchIn(lower) -> "ja"
            Regex("(\\b|\\.)(eng|en)(\\b|\\.)").containsMatchIn(lower) -> "en"
            Regex("(\\b|\\.)(chs|zh)(\\b|\\.)").containsMatchIn(lower) -> "zh"
            else -> ""
        }
    }

    fun setOffset(ms: Long) {
        globalOffsetMs = ms
    }

    fun adjustOffset(deltaMs: Long) {
        globalOffsetMs += deltaMs
    }

    fun adjustSecondaryOffset(deltaMs: Long) {
        secondaryOffsetMs += deltaMs
    }

    // ------------------------------------------------------------
    // Cue lookup
    // ------------------------------------------------------------

    fun sortedCues(): List<SubtitleCue> {
        val track = activeTrack ?: return emptyList()
        return track.track.cues.sortedBy { it.startMs }
    }

    /** Index of the cue active at [positionMs], or -1. */
    fun cueIndexAt(positionMs: Long): Int {
        val cues = sortedCues()
        if (cues.isEmpty()) return -1
        val offset = globalOffsetMs
        var lo = 0
        var hi = cues.size - 1
        var best = -1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            val start = cues[mid].startMs + offset
            val end = cues[mid].endMs + offset
            when {
                positionMs < start -> hi = mid - 1
                positionMs > end -> {
                    best = mid
                    lo = mid + 1
                }
                else -> return mid
            }
        }
        return best
    }

    fun activeCueAt(positionMs: Long): SubtitleCue? {
        val idx = cueIndexAt(positionMs)
        return if (idx >= 0) sortedCues()[idx] else null
    }

    fun activeCueIndex(positionMs: Long): Int = cueIndexAt(positionMs)

    fun cueAt(index: Int): SubtitleCue? = sortedCues().getOrNull(index)

    fun nextCue(afterMs: Long): SubtitleCue? =
        sortedCues().firstOrNull { it.startMs + globalOffsetMs > afterMs + 1 }

    fun prevCue(beforeMs: Long): SubtitleCue? =
        sortedCues().lastOrNull { it.startMs + globalOffsetMs < beforeMs - 1 }

    fun nextIndex(index: Int): Int = (index + 1).coerceAtMost(sortedCues().lastIndex)
    fun prevIndex(index: Int): Int = (index - 1).coerceAtLeast(0)

    // ------------------------------------------------------------
    // Transcript search / filter
    // ------------------------------------------------------------

    fun searchCues(query: String, filter: TranscriptFilter = TranscriptFilter.All): List<SubtitleCue> {
        val cues = sortedCues()
        val q = query.trim().lowercase()
        val filtered = if (q.isBlank()) cues else cues.filter { cue ->
            cue.text.lowercase().contains(q)
        }
        return when (filter) {
            TranscriptFilter.All -> filtered
            else -> filtered
        }
    }

    /** Timeline markers (start times) for the seek bar — aggregated when dense. */
    fun cueMarkers(maxMarkers: Int = 240): List<Long> {
        val cues = sortedCues()
        if (cues.isEmpty()) return emptyList()
        if (cues.size <= maxMarkers) return cues.map { it.startMs + globalOffsetMs }
        val step = cues.size.toDouble() / maxMarkers
        return (0 until maxMarkers).map { i -> cues[(i * step).toInt()].startMs + globalOffsetMs }
    }
}
