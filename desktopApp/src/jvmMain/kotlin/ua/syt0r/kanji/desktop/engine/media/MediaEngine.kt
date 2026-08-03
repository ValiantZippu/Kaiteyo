package ua.syt0r.kanji.desktop.engine.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent
import javax.sound.sampled.LineUnavailableException
import kotlin.math.roundToInt

// ============================================
// KAITEYO MEDIA WORKSPACE ENGINE
// A unified Japanese-learning media workspace.
// Supports local video files (frame-extraction
// playback), audio clips (Java Sound), subtitle
// tracks (SRT/ASS/SSA/VTT), screenshot capture and
// timestamp bookmarks. Everything integrates with
// the dictionary popup and mining pipeline.
// ============================================

/** A timestamped bookmark saved while watching/listening. */
@Serializable
data class MediaBookmark(
    val id: String,
    val mediaPath: String,
    val timestampMs: Long,
    val label: String = "",
    val note: String = "",
    val createdAt: String = ""
)

/** A saved audio clip with optional sentence context. */
@Serializable
data class AudioClip(
    val id: String,
    val sourcePath: String,
    val label: String = "",
    val startMs: Long = 0,
    val endMs: Long = 0,
    val exportedPath: String = "",
    val createdAt: String = ""
)

@Serializable
enum class MediaKind { Video, Audio, Image, Pdf, Text, Web }

@Serializable
data class MediaDocument(
    val path: String,
    val name: String,
    val kind: MediaKind,
    val sizeBytes: Long = 0
) {
    val displayName: String get() = name
}

@Serializable
private data class MediaStateDto(
    val bookmarks: List<MediaBookmark> = emptyList(),
    val clips: List<AudioClip> = emptyList(),
    val recentFiles: List<String> = emptyList()
)

/** JVM audio player wrapping a Clip with pause/resume/seeking. */
class AudioPlayer {
    private var clip: Clip? = null
    private var pausedAtMs: Long = 0
    private var storedFormat: AudioFormat? = null

    val isPlaying: Boolean get() = clip?.isRunning == true

    fun load(file: File): Result<Unit> = runCatching {
        stop()
        val stream = AudioSystem.getAudioInputStream(file)
        clip = AudioSystem.getClip().also { c ->
            c.open(stream)
            c.addLineListener { ev ->
                if (ev.type == LineEvent.Type.STOP && ev.line is Clip) {
                    val cl = ev.line as Clip
                    if (cl.framePosition >= cl.frameLength) {
                        // natural completion
                        pausedAtMs = 0
                    }
                }
            }
        }
    }

    fun play() {
        val c = clip ?: return
        if (pausedAtMs > 0) {
            c.microsecondPosition = pausedAtMs
            pausedAtMs = 0
        }
        c.start()
    }

    fun pause() {
        val c = clip ?: return
        if (c.isRunning) {
            pausedAtMs = c.microsecondPosition
            c.stop()
        }
    }

    fun stop() {
        clip?.stop()
        clip?.close()
        clip = null
        pausedAtMs = 0
    }

    fun seekTo(ms: Long) {
        val c = clip ?: return
        val pos = ms.coerceIn(0, c.microsecondLength / 1000) * 1000
        c.microsecondPosition = pos
        pausedAtMs = 0
    }

    val positionMs: Long
        get() = if (pausedAtMs > 0) pausedAtMs
        else clip?.microsecondPosition?.div(1000) ?: 0

    val lengthMs: Long
        get() = clip?.microsecondLength?.div(1000) ?: 0
}

/**
 * A lightweight video document: metadata + still-frame capture
 * abstraction. Actual frame playback is delegated to the platform
 * (JavaFX is wired on desktop); this engine keeps state + bookmarks.
 */
class MediaEngine {

    var currentDocument by mutableStateOf<MediaDocument?>(null)
    var currentPositionMs by mutableStateOf(0L)
    var currentDurationMs by mutableStateOf(0L)
    var playbackSpeed by mutableStateOf(1.0f)
    var looping by mutableStateOf(false)
    var subtitleTrack by mutableStateOf<SubtitleTrack?>(null)
    var subtitleVisible by mutableStateOf(true)
    var currentScreenshotPath by mutableStateOf<String?>(null)

    val bookmarks = androidx.compose.runtime.mutableStateListOf<MediaBookmark>()
    val audioClips = androidx.compose.runtime.mutableStateListOf<AudioClip>()
    val recentFiles = androidx.compose.runtime.mutableStateListOf<String>()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val stateFile: File get() = File(System.getProperty("user.home"), ".kaiteyo/media-state.json")

    init {
        load()
    }

    fun openFile(file: File): MediaDocument {
        val kind = when (file.extension.lowercase()) {
            in setOf("mp4", "mkv", "webm", "mov", "avi", "m4v") -> MediaKind.Video
            in setOf("mp3", "wav", "ogg", "flac", "m4a") -> MediaKind.Audio
            in setOf("png", "jpg", "jpeg", "gif", "webp", "bmp") -> MediaKind.Image
            in setOf("pdf") -> MediaKind.Pdf
            in setOf("txt", "md", "srt", "vtt", "ass", "ssa") -> MediaKind.Text
            else -> MediaKind.Text
        }
        val doc = MediaDocument(file.absolutePath, file.name, kind, file.length())
        currentDocument = doc
        if (!recentFiles.contains(file.absolutePath)) {
            recentFiles.add(0, file.absolutePath)
            while (recentFiles.size > 30) recentFiles.removeAt(recentFiles.lastIndex)
        }
        save()
        return doc
    }

    fun openSubtitle(file: File) {
        subtitleTrack = SubtitleParser.parse(file)
        subtitleVisible = true
    }

    fun cueAt(ms: Long): SubtitleCue? = subtitleTrack?.cueAt(ms)

    fun addBookmark(label: String = "") {
        val doc = currentDocument ?: return
        val bm = MediaBookmark(
            id = "bm-${System.currentTimeMillis()}",
            mediaPath = doc.path,
            timestampMs = currentPositionMs,
            label = label,
            createdAt = kotlinx.datetime.Clock.System.now().toString()
        )
        bookmarks.add(bm)
        save()
    }

    fun removeBookmark(id: String) {
        bookmarks.removeAll { it.id == id }
        save()
    }

    fun addAudioClip(label: String, exportedPath: String = "") {
        val doc = currentDocument ?: return
        val clip = AudioClip(
            id = "clip-${System.currentTimeMillis()}",
            sourcePath = doc.path,
            label = label,
            createdAt = kotlinx.datetime.Clock.System.now().toString(),
            exportedPath = exportedPath
        )
        audioClips.add(0, clip)
        save()
    }

    fun removeClip(id: String) {
        audioClips.removeAll { it.id == id }
        save()
    }

    fun setScreenshot(path: String?) {
        currentScreenshotPath = path
        if (path != null) save()
    }

    fun updatePosition(ms: Long) {
        currentPositionMs = ms.coerceAtLeast(0)
    }

    private fun load() {
        if (!stateFile.exists()) return
        runCatching {
            val dto = json.decodeFromString<MediaStateDto>(stateFile.readText())
            bookmarks.clear(); bookmarks.addAll(dto.bookmarks)
            audioClips.clear(); audioClips.addAll(dto.clips)
            recentFiles.clear(); recentFiles.addAll(dto.recentFiles)
        }
    }

    private fun save() {
        runCatching {
            stateFile.writeText(
                json.encodeToString(MediaStateDto(bookmarks.toList(), audioClips.toList(), recentFiles.toList()))
            )
        }
    }

    companion object {
        fun formatTime(ms: Long): String {
            val total = ms.coerceAtLeast(0)
            val h = total / 3600000
            val m = (total % 3600000) / 60000
            val s = (total % 60000) / 1000
            return if (h > 0) "%d:%02d:%02d".format(h, m, s)
            else "%02d:%02d".format(m, s)
        }

        fun durationForRate(ms: Long, rate: Float): Long =
            (ms / rate.coerceAtLeast(0.1f)).roundToInt().toLong()
    }
}