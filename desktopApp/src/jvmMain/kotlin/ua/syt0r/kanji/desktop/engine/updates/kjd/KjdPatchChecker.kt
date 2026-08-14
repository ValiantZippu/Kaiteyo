package ua.syt0r.kanji.desktop.engine.updates.kjd

import ua.syt0r.kanji.desktop.engine.updates.UpdateChannel
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ============================================
// KJD PATCH CHECKER
// Fetches the KJD patch feed over HTTPS and
// picks the patch applicable to the bundled
// database's current fingerprint. The interface
// stays small so tests can substitute a local
// fake without touching the service.
// ============================================

sealed interface KjdPatchCheckResult {
    data class NoPatch(
        val channel: UpdateChannel,
        val checkedAt: String,
        val feedDatabaseVersion: String
    ) : KjdPatchCheckResult

    data class PatchAvailable(
        val channel: UpdateChannel,
        val entry: KjdPatchFeed.PatchEntry,
        val feed: KjdPatchFeed
    ) : KjdPatchCheckResult

    data class Failed(val reason: String) : KjdPatchCheckResult
}

interface KjdPatchChecker {
    /** Returns the patch that upgrades [state] to the feed's latest, or a
     *  no-patch/failure verdict. Suspends for network I/O. */
    suspend fun check(
        channel: UpdateChannel,
        state: KjdDatabaseState
    ): KjdPatchCheckResult
}

/** Production checker: fetches {baseUrl}/kjd-update-{channel}.json over HTTPS. */
class HttpKjdPatchChecker(
    private val baseUrl: String = KJD_PATCH_FEED_BASE_URL,
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
) : KjdPatchChecker {

    override suspend fun check(
        channel: UpdateChannel,
        state: KjdDatabaseState
    ): KjdPatchCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            require(baseUrl.startsWith("https://")) { "KJD feeds must be served over HTTPS" }
            val url = baseUrl.trimEnd('/') + "/" + channel.kjdFeedFileName()
            val request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            check(response.statusCode() == 200) { "Feed returned HTTP ${response.statusCode()}" }
            val feed = KjdPatchFeedParser.parse(response.body())

            // Pick the patch that takes THIS database to the next release.
            // fromFingerprint is the authoritative identity — version strings
            // are informational.
            val applicable = feed.patches.firstOrNull { it.fromFingerprint == state.fingerprint }
            if (applicable == null) {
                KjdPatchCheckResult.NoPatch(channel, feed.publishedAt, feed.databaseVersion)
            } else {
                KjdPatchCheckResult.PatchAvailable(channel, applicable, feed)
            }
        }.getOrElse { KjdPatchCheckResult.Failed(it.message ?: "Unknown KJD check error") }
    }
}

/** Feed filename per channel, mirroring the app update feed naming. */
fun UpdateChannel.kjdFeedFileName(): String = "kjd-update-${name.lowercase()}.json"
