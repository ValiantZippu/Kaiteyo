package ua.syt0r.kanji.desktop.engine.updates

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ============================================
// UPDATE CHECKER
// Fetches the channel feed over HTTPS and
// parses it. The interface stays small so a
// real backend (GitHub API, self-hosted feed)
// can be swapped in without touching callers.
// ============================================

sealed interface UpdateCheckResult {
    data class UpToDate(val channel: UpdateChannel, val checkedAt: String) : UpdateCheckResult
    data class Available(
        val channel: UpdateChannel,
        val manifest: UpdateManifest
    ) : UpdateCheckResult
    data class Failed(val reason: String) : UpdateCheckResult
}

interface UpdateChecker {
    /** Returns the latest manifest for [channel], or null when up to date. */
    suspend fun check(
        channel: UpdateChannel,
        current: AppVersionInfo,
        policy: UpdatePolicy
    ): UpdateCheckResult
}

/** Production checker: fetches {baseUrl}/{channel.feedFileName} over HTTPS.
 *  Refuses non-HTTPS feeds. */
class HttpUpdateChecker(
    private val baseUrl: String,
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
) : UpdateChecker {

    override suspend fun check(
        channel: UpdateChannel,
        current: AppVersionInfo,
        policy: UpdatePolicy
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            require(baseUrl.startsWith("https://")) { "Update feeds must be served over HTTPS" }
            val url = baseUrl.trimEnd('/') + "/" + channel.feedFileName
            val request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            check(response.statusCode() == 200) { "Feed returned HTTP ${response.statusCode()}" }
            val manifest = UpdateManifestParser.parse(response.body())
            check(UpdateChannel.fromName(manifest.channel) == channel) { "Feed channel mismatch" }

            if (!policy.isUpdateAvailable(current, manifest.latest)) {
                UpdateCheckResult.UpToDate(channel, manifest.publishedAt)
            } else {
                UpdateCheckResult.Available(channel, manifest)
            }
        }.getOrElse { UpdateCheckResult.Failed(it.message ?: "Unknown check error") }
    }
}
