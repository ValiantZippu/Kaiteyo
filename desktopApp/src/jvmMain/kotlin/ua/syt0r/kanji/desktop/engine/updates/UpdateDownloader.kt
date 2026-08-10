package ua.syt0r.kanji.desktop.engine.updates

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ============================================
// UPDATE DOWNLOADER
// Downloads an update artifact into the data
// directory and verifies its sha256 BEFORE the
// file is allowed anywhere near the app install.
// ============================================

sealed interface DownloadResult {
    data class Downloaded(val file: File, val sizeBytes: Long) : DownloadResult
    data class Failed(val reason: String) : DownloadResult
}

interface UpdateDownloader {
    suspend fun download(
        artifact: UpdateManifest.UpdateArtifact,
        targetDir: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): DownloadResult
}

/** Streaming downloader with end-to-end sha256 verification. */
class HttpUpdateDownloader(
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
) : UpdateDownloader {

    override suspend fun download(
        artifact: UpdateManifest.UpdateArtifact,
        targetDir: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        runCatching {
            require(artifact.url.startsWith("https://")) { "Artifacts must be served over HTTPS" }
            require(artifact.sha256.length == 64) { "Artifact missing sha256" }

            targetDir.mkdirs()
            val fileName = artifact.url.substringAfterLast('/').ifBlank { "update.pkg" }
            val target = File(targetDir, fileName)

            val request = HttpRequest.newBuilder(URI.create(artifact.url))
                .timeout(Duration.ofMinutes(30))
                .GET()
                .build()

            // BodyHandlers.ofFile streams to disk; progress is reported via
            // the Content-Length header (honest progress — no fabricated %).
            val response = client.send(request, HttpResponse.BodyHandlers.ofFile(target.toPath()))
            if (response.statusCode() != 200) {
                target.delete() // never leave a partial/error body behind
                check(false) { "Download returned HTTP ${response.statusCode()}" }
            }
            response.headers().firstValue("content-length")
                .ifPresent { total -> onProgress(target.length(), total.toLong()) }

            // Verify integrity before returning.
            val digest = MessageDigest.getInstance("SHA-256")
            target.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            check(actual == artifact.sha256) {
                "Checksum mismatch: expected ${artifact.sha256}, got $actual — refusing to use this file"
            }

            DownloadResult.Downloaded(target, target.length())
        }.getOrElse { DownloadResult.Failed(it.message ?: "Unknown download error") }
    }
}
