package ua.syt0r.kanji.desktop.engine.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

// ============================================
// KAITEYO MEDIA SCANNER
// Background folder scanning that never blocks
// the UI: walks configured folders (optionally
// recursive), registers media files and their
// companion subtitles in the library, and reports
// progress + cancellable state.
// ============================================

class MediaScanner(private val library: MediaLibrary) {

    var scanning by mutableStateOf(false)
    var progress by mutableStateOf(0f)
    var scannedFiles by mutableStateOf(0)
    var lastError by mutableStateOf<String?>(null)
    var scanMessage by mutableStateOf("")

    // ------------------------------------------------------------
    // Background folder watcher — polls the configured library folders
    // and registers any new media that appeared since the last sweep.
    // Dependency-free and daemon-threaded; never blocks the UI.
    // ------------------------------------------------------------
    var watcherActive by mutableStateOf(false)
    var lastWatchFound by mutableStateOf(0)
    private var watcherThread: Thread? = null

    fun startWatching(intervalMs: Long = 45_000) {
        if (watcherActive) return
        watcherActive = true
        watcherThread = thread(name = "kaiteyo-media-watcher", isDaemon = true) {
            while (watcherActive) {
                runCatching { sweepWatchedFolders() }
                try {
                    Thread.sleep(intervalMs)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }
    }

    fun stopWatching() {
        watcherActive = false
        watcherThread?.interrupt()
        watcherThread = null
    }

    /** Register media files that appeared in watched folders since the last sweep. */
    private fun sweepWatchedFolders() {
        if (library.folders.isEmpty()) return
        val known = library.items.map { it.path }.toHashSet()
        val fresh = mutableListOf<String>()
        library.folders.toList().forEach { folder ->
            val dir = File(folder.path)
            if (!dir.isDirectory) return@forEach
            val files = mutableListOf<File>()
            collectMediaFiles(dir, folder.includeSubdirs, files)
            files.forEach { f ->
                val p = f.absolutePath
                if (p !in known && library.addFile(f) != null) fresh.add(p)
            }
        }
        if (fresh.isNotEmpty()) {
            lastWatchFound = fresh.size
            scanMessage = "Auto-added ${fresh.size} new file(s) from watched folders"
        }
    }

    private fun collectMediaFiles(dir: File, recursive: Boolean, out: MutableList<File>) {
        val children = dir.listFiles() ?: return
        for (f in children) {
            if (f.isDirectory) {
                if (recursive) collectMediaFiles(f, true, out)
            } else if (MediaKind.of(f) != null) {
                out.add(f)
            }
        }
    }

    // ------------------------------------------------------------
    // Thumbnails — a poster frame per video, extracted with ffmpeg on a
    // background thread and cached in the media cache. Missing ffmpeg or a
    // failed extraction simply means the row shows the plain icon.
    // ------------------------------------------------------------
    val thumbnailState = mutableStateMapOf<String, String?>()

    val thumbnailsDir: File
        get() = File(MediaCapture.cacheDir, "thumbnails").apply { mkdirs() }

    /** Request a poster frame for an item; idempotent and asynchronous. */
    fun requestThumbnail(item: MediaItem) {
        if (item.isRemote) return // no local file to decode
        if (thumbnailState.containsKey(item.id)) return
        thumbnailState[item.id] = null // mark in-flight
        val cached = File(thumbnailsDir, "${item.id}.jpg")
        if (cached.exists()) {
            thumbnailState[item.id] = cached.absolutePath
            return
        }
        if (item.kind != MediaKind.Video) {
            thumbnailState[item.id] = null
            return
        }
        thread(name = "kaiteyo-thumb-${item.id.takeLast(6)}", isDaemon = true) {
            thumbnailState[item.id] = generateThumbnail(item, cached)
        }
    }

    private fun generateThumbnail(item: MediaItem, target: File): String? {
        val ffmpeg = MediaCapture.findFfmpeg() ?: return null
        val src = File(item.path)
        if (!src.exists()) return null
        // Frame ~1/3 into the media (or 3s for unknown durations) — a good
        // poster without decoding the whole file.
        val atMs = if (item.durationMs > 0) (item.durationMs / 3).coerceAtLeast(500) else 3000
        val cmd = listOf(
            ffmpeg, "-y", "-ss", String.format("%.3f", atMs / 1000.0), "-i", src.absolutePath,
            "-frames:v", "1", "-vf", "scale=320:-2", "-q:v", "5", target.absolutePath
        )
        val ok = runCatching {
            val p = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start()
            val done = p.waitFor(20, java.util.concurrent.TimeUnit.SECONDS)
            p.destroy()
            done && p.exitValue() == 0
        }.getOrDefault(false)
        return if (ok && target.exists() && target.length() > 0) target.absolutePath else null
    }

    private val cancelled = AtomicBoolean(false)
    private var worker: Thread? = null

    fun scan(folder: File, recursive: Boolean, onComplete: (Int) -> Unit = {}) {
        if (scanning) return
        cancelled.set(false)
        scanning = true
        progress = 0f
        scannedFiles = 0
        lastError = null
        scanMessage = "Scanning ${folder.name}…"
        worker = thread(name = "kaiteyo-media-scan", isDaemon = true) {
            try {
                val files = mutableListOf<File>()
                collectFiles(folder, recursive, files)
                val added = files.count { library.addFile(it) != null }
                scannedFiles = files.size
                progress = 1f
                scanMessage = "Found $added media files in ${folder.name}"
                onComplete(added)
            } catch (e: Exception) {
                lastError = e.message
                scanMessage = "Scan failed: ${e.message}"
            } finally {
                scanning = false
            }
        }
    }

    fun cancel() {
        cancelled.set(true)
    }

    private fun collectFiles(dir: File, recursive: Boolean, out: MutableList<File>) {
        if (cancelled.get()) return
        val children = dir.listFiles() ?: return
        for (f in children) {
            if (cancelled.get()) return
            if (f.isDirectory) {
                if (recursive) collectFiles(f, true, out)
            } else if (MediaKind.of(f) != null || isSubtitleFile(f)) {
                out.add(f)
            }
        }
        // Report incremental progress so the UI stays alive on huge folders.
        if (out.size % 200 < 50) {
            scannedFiles = out.size
            progress = 0.5f
        }
    }

    private fun isSubtitleFile(f: File): Boolean = f.extension.lowercase() in setOf("srt", "ass", "ssa", "vtt")
}
