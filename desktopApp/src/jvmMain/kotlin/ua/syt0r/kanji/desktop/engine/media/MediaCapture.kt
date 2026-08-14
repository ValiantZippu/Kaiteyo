package ua.syt0r.kanji.desktop.engine.media

import java.io.File
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.AudioInputStream

// ============================================
// KAITEYO MEDIA CAPTURE
// Screenshot + audio-clip capture for mining.
// Audio clips are extracted with ffmpeg when it
// is installed; Java Sound covers WAV/AIFF range
// export. Every artifact lands in the managed
// media cache (~/.kaiteyo/media-cache) so the
// main library stays light.
// ============================================

object MediaCapture {

    val cacheDir: File get() = File(System.getProperty("user.home"), ".kaiteyo/media-cache")

    fun ensureCache(): File {
        cacheDir.mkdirs()
        return cacheDir
    }

    // ------------------------------------------------------------
    // ffmpeg detection
    // ------------------------------------------------------------

    fun findFfmpeg(): String? {
        System.getenv("KAITEYO_FFMPEG_PATH")?.takeIf { it.isNotBlank() }?.let { return it }
        val path = System.getenv("PATH") ?: return null
        val names = listOf("ffmpeg", "ffmpeg.exe")
        path.split(File.pathSeparator).forEach { dir ->
            names.forEach { name ->
                val f = File(dir, name)
                if (f.canExecute()) return f.absolutePath
            }
        }
        return null
    }

    val ffmpegAvailable: Boolean get() = findFfmpeg() != null

    // ------------------------------------------------------------
    // Audio clip extraction
    // ------------------------------------------------------------

    /**
     * Extract the [startMs, endMs] range of a media file into a WAV clip.
     * Prefers ffmpeg (handles video files and compressed audio); falls back
     * to a Java Sound range export for WAV/AIFF sources.
     */
    fun extractAudioClip(
        sourcePath: String,
        startMs: Long,
        endMs: Long,
        label: String = "clip"
    ): Result<File> {
        ensureCache()
        val target = File(cacheDir, "clip-${System.currentTimeMillis()}-$label.wav")
        val ffmpeg = findFfmpeg()
        return if (ffmpeg != null) {
            extractWithFfmpeg(ffmpeg, sourcePath, startMs, endMs, target)
        } else {
            extractWithJavaSound(File(sourcePath), startMs, endMs, target)
        }
    }

    private fun extractWithFfmpeg(ffmpeg: String, source: String, startMs: Long, endMs: Long, target: File): Result<File> {
        return runCatching {
            val startSec = startMs / 1000.0
            val durationSec = (endMs - startMs).coerceAtLeast(50) / 1000.0
            val pb = ProcessBuilder(
                ffmpeg,
                "-ss", formatSeconds(startSec),
                "-i", source,
                "-t", formatSeconds(durationSec),
                "-vn", "-acodec", "pcm_s16le", "-ar", "44100", "-ac", "1",
                "-y", target.absolutePath
            ).redirectErrorStream(true)
            val p = pb.start()
            p.inputStream.readBytes()
            val ok = p.waitFor(60, TimeUnit.SECONDS)
            p.destroy()
            if (!ok || !target.exists() || target.length() == 0L) error("ffmpeg could not extract the audio range")
            target
        }
    }

    private fun extractWithJavaSound(source: File, startMs: Long, endMs: Long, target: File): Result<File> {
        return runCatching {
            val stream = AudioSystem.getAudioInputStream(source)
            val format = stream.format
            if (format.encoding != javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED) {
                error("Audio clip capture for this format needs ffmpeg (install ffmpeg and restart)")
            }
            val bytesPerMs = (format.frameRate * format.frameSize / 1000.0).toInt().coerceAtLeast(1)
            val skip = startMs * bytesPerMs
            var skipped = 0L
            while (skipped < skip) {
                val n = stream.skip((skip - skipped).coerceAtMost(1_048_576))
                if (n <= 0) break
                skipped += n
            }
            val frameLength = ((endMs - startMs) * format.frameRate / 1000).toInt()
            val frames = stream.readNBytes(frameLength * format.frameSize)
            val outStream = AudioInputStream(
                java.io.ByteArrayInputStream(frames),
                format,
                (frames.size / format.frameSize).toLong()
            )
            AudioSystem.write(outStream, AudioFileFormat.Type.WAVE, target)
            stream.close()
            outStream.close()
            if (target.length() == 0L) error("Nothing captured")
            target
        }
    }

    /**
     * Extract the [startMs, endMs] range of a video into a compact MP4 clip
     * (H.264 + AAC via ffmpeg). Requires ffmpeg — video encoding is far
     * outside Java Sound's scope.
     */
    fun extractVideoClip(
        sourcePath: String,
        startMs: Long,
        endMs: Long,
        label: String = "clip"
    ): Result<File> {
        val ffmpeg = findFfmpeg()
            ?: return Result.failure(IllegalStateException("Video clip capture needs ffmpeg (install it and restart Kaiteyo)"))
        ensureCache()
        val target = File(cacheDir, "vclip-${System.currentTimeMillis()}-$label.mp4")
        return runCatching {
            val startSec = startMs / 1000.0
            val durationSec = (endMs - startMs).coerceAtLeast(50) / 1000.0
            val pb = ProcessBuilder(
                ffmpeg,
                "-ss", formatSeconds(startSec),
                "-i", sourcePath,
                "-t", formatSeconds(durationSec),
                "-c:v", "libx264", "-preset", "veryfast", "-crf", "20",
                "-c:a", "aac", "-b:a", "128k",
                "-movflags", "+faststart",
                "-y", target.absolutePath
            ).redirectErrorStream(true)
            val p = pb.start()
            p.inputStream.readBytes()
            val ok = p.waitFor(120, TimeUnit.SECONDS)
            p.destroy()
            if (!ok || !target.exists() || target.length() == 0L) error("ffmpeg could not extract the video range")
            target
        }
    }

    private fun formatSeconds(sec: Double): String = String.format("%.3f", sec)

    // ------------------------------------------------------------
    // Cache management
    // ------------------------------------------------------------

    fun cacheSizeBytes(): Long = ensureCache().listFiles()?.sumOf { it.length() } ?: 0L

    fun cacheFileCount(): Int = ensureCache().listFiles()?.size ?: 0

    fun clearCache(): Int {
        val files = ensureCache().listFiles() ?: return 0
        var removed = 0
        files.forEach { if (it.delete()) removed++ }
        return removed
    }
}
