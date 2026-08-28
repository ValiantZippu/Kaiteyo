package ua.syt0r.kanji.desktop.engine.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

// ============================================
// MEDIA DOWNLOAD SERVICE
// Manages queued/downloading/paused/completed/
// failed/cancelled jobs with progress. Uses
// java.net.http streaming so large media can
// be fetched without OOM. Respects platform
// restrictions: only http(s), no DRM bypass,
// filename sanitized, target dir is user-chosen.
// ============================================

enum class DownloadState(val label: String) { Queued("Queued"), Downloading("Downloading"), Paused("Paused"), Completed("Completed"), Failed("Failed"), Cancelled("Cancelled") }

@Serializable
data class DownloadJob(
    val id: String,
    val url: String,
    val fileName: String,
    val targetPath: String,
    val sizeBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val state: DownloadState = DownloadState.Queued,
    val error: String = "",
    val createdAt: String = ""
) {
    val progress: Float get() = if (sizeBytes > 0) (downloadedBytes.toFloat() / sizeBytes).coerceIn(0f, 1f) else 0f
}

class MediaDownloadService(
    private val downloadDir: File = File(System.getProperty("user.home"), ".kaiteyo/downloads")
) {
    val jobs = mutableStateListOf<DownloadJob>()
    var lastError by mutableStateOf<String?>(null)
        private set

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private val cancelled = mutableSetOf<String>()

    init { downloadDir.mkdirs() }

    /** Enqueue and start downloading [url] into [downloadDir]. Validates http(s) and sanitizes filename. */
    fun enqueue(url: String, fileName: String? = null): DownloadJob? {
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            lastError = "Only http(s) URLs are supported"
            return null
        }
        val safeName = sanitizeFileName(fileName ?: trimmed.substringAfterLast('/').substringBefore('?').ifBlank { "download" })
        val id = "dl-${System.currentTimeMillis()}"
        val target = File(downloadDir, safeName)
        val job = DownloadJob(id, trimmed, safeName, target.absolutePath, state = DownloadState.Queued, createdAt = java.time.Instant.now().toString())
        jobs.add(0, job)
        downloadAsync(job)
        return job
    }

    fun pause(id: String) {
        // Cooperative: mark cancelled and let thread exit; job becomes Paused (retryable).
        if (jobs.any { it.id == id && it.state == DownloadState.Downloading }) {
            cancelled.add(id)
            update(id) { it.copy(state = DownloadState.Paused) }
        }
    }

    fun cancel(id: String) {
        cancelled.add(id)
        update(id) { it.copy(state = DownloadState.Cancelled) }
    }

    fun retry(id: String) {
        val job = jobs.firstOrNull { it.id == id } ?: return
        if (job.state != DownloadState.Failed && job.state != DownloadState.Paused && job.state != DownloadState.Cancelled) return
        update(id) { it.copy(state = DownloadState.Queued, error = "", downloadedBytes = 0) }
        downloadAsync(jobs.first { it.id == id })
    }

    fun clearCompleted() { jobs.removeAll { it.state == DownloadState.Completed || it.state == DownloadState.Cancelled } }

    private fun downloadAsync(job: DownloadJob) {
        update(job.id) { it.copy(state = DownloadState.Downloading) }
        Thread {
            try {
                val request = HttpRequest.newBuilder(URI(job.url))
                    .timeout(Duration.ofSeconds(90))
                    .header("User-Agent", "Kaiteyo-MediaDownload/1.0")
                    .GET().build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
                if (response.statusCode() !in 200..399) {
                    error("HTTP ${response.statusCode()}")
                }
                val total = response.headers().firstValue("Content-Length").map { it.toLongOrNull() ?: 0L }.orElse(0L)
                if (total > 0) update(job.id) { it.copy(sizeBytes = total) }
                val target = File(job.targetPath)
                target.parentFile?.mkdirs()
                response.body().use { input ->
                    target.outputStream().use { output ->
                        val buf = ByteArray(64 * 1024)
                        var downloaded = 0L
                        while (true) {
                            if (job.id in cancelled) break
                            val n = input.read(buf)
                            if (n == -1) break
                            output.write(buf, 0, n)
                            downloaded += n
                            if (downloaded % (512 * 1024) == 0L) {
                                update(job.id) { it.copy(downloadedBytes = downloaded, sizeBytes = if (total > 0) total else downloaded) }
                            }
                        }
                        if (job.id in cancelled) {
                            // Paused or cancelled — keep partial file for resume (future: Range header).
                            return@use
                        }
                        update(job.id) { it.copy(downloadedBytes = downloaded, sizeBytes = if (total > 0) total else downloaded, state = DownloadState.Completed) }
                    }
                }
            } catch (e: Exception) {
                val wasCancelled = job.id in cancelled
                cancelled.remove(job.id)
                if (!wasCancelled) {
                    update(job.id) { it.copy(state = DownloadState.Failed, error = e.message ?: e.javaClass.simpleName) }
                    lastError = e.message
                }
            } finally {
                cancelled.remove(job.id)
            }
        }.also { it.isDaemon = true; it.name = "kaiteyo-download-${job.id}" }.start()
    }

    private fun update(id: String, transform: (DownloadJob) -> DownloadJob) {
        val idx = jobs.indexOfFirst { it.id == id }
        if (idx >= 0) jobs[idx] = transform(jobs[idx])
    }

    private fun sanitizeFileName(name: String): String {
        val safe = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(120).ifBlank { "download" }
        // Prevent path traversal or hidden files.
        return if (safe.startsWith(".")) "_$safe" else safe
    }
}
