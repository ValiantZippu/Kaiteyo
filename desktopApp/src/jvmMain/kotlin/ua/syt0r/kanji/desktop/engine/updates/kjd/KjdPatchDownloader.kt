package ua.syt0r.kanji.desktop.engine.updates.kjd

import io.kaiteyo.kjd.patch.DatabasePatch
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

// ============================================
// KJD PATCH DOWNLOADER
// Downloads a patch artifact into the updates
// directory and verifies its sha256 BEFORE the
// file is allowed near the bundled database.
// ============================================

sealed interface KjdPatchDownloadResult {
    data class Downloaded(val file: File, val patch: DatabasePatch, val sizeBytes: Long) : KjdPatchDownloadResult
    data class Failed(val reason: String) : KjdPatchDownloadResult
}

interface KjdPatchDownloader {
    suspend fun download(
        entry: KjdPatchFeed.PatchEntry,
        targetDir: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit
    ): KjdPatchDownloadResult
}

/** Streaming downloader with end-to-end sha256 verification and patch parsing. */
class HttpKjdPatchDownloader(
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
) : KjdPatchDownloader {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun download(
        entry: KjdPatchFeed.PatchEntry,
        targetDir: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit
    ): KjdPatchDownloadResult = withContext(Dispatchers.IO) {
        runCatching {
            require(entry.url.startsWith("https://")) { "KJD patches must be served over HTTPS" }
            require(entry.sha256.length == 64) { "KJD patch missing sha256" }

            targetDir.mkdirs()
            val target = File(targetDir, "kjd-patch-${entry.toDatabaseVersion}.json")

            val request = HttpRequest.newBuilder(URI.create(entry.url))
                .timeout(Duration.ofMinutes(30))
                .GET()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofFile(target.toPath()))
            if (response.statusCode() != 200) {
                target.delete()
                check(false) { "Download returned HTTP ${response.statusCode()}" }
            }
            response.headers().firstValue("content-length")
                .ifPresent { total -> onProgress(target.length(), total.toLong()) }

            // Verify integrity before parsing.
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
            check(actual == entry.sha256) {
                "Checksum mismatch: expected ${entry.sha256}, got $actual — refusing to use this patch"
            }

            // Parse into the platform's patch model.
            val patch = json.decodeFromString<DatabasePatch>(target.readText())

            KjdPatchDownloadResult.Downloaded(target, patch, target.length())
        }.getOrElse { KjdPatchDownloadResult.Failed(it.message ?: "Unknown KJD download error") }
    }
}
