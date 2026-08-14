package ua.syt0r.kanji.desktop.engine.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import ua.syt0r.kanji.core.logger.Logger
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

// ============================================
// GITHUB GIST SYNC TRANSPORT
// A real SyncTransport that persists the whole
// SyncManifest (all blobs) as a single private
// gist owned by the connected GitHub account.
//
//   • One gist per account, found by description
//     ("Kaiteyo Sync") or created on first push.
//   • Token is supplied by a provider lambda (the
//     AccountEngine handles refresh internally);
//     on HTTP 401 the token is re-fetched once.
//   • Blob versions are monotonic per name, so the
//     SyncEngine's diff/reconcile semantics hold.
//
// Gists are a pragmatic, dependency-free cloud
// endpoint. Swap this class for Google Drive /
// Dropbox / a custom server by implementing the
// same SyncTransport interface.
// ============================================

class GitHubGistSyncTransport(
    private val stateDir: File,
    private val tokenProvider: suspend () -> Result<String>,
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
) : SyncTransport {

    override val type: SyncProviderType = SyncProviderType.GitHub

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val stateFile: File get() = File(stateDir, "gist-state.json")
    private var cachedGistId: String? = null

    // ------------------------------------------------------------
    // SyncTransport contract
    // ------------------------------------------------------------

    override suspend fun list(): List<SyncBlob> = withContext(Dispatchers.IO) {
        val gistId = findOrCreateGist() ?: return@withContext emptyList()
        runCatching { fetchManifest(gistId)?.blobs ?: emptyList() }
            .onFailure { Logger.w("GitHubGist: list failed: ${it.message}") }
            .getOrDefault(emptyList())
    }

    override suspend fun download(name: String): SyncBlob = withContext(Dispatchers.IO) {
        val gistId = findOrCreateGist() ?: return@withContext SyncBlob(name)
        fetchManifest(gistId)?.blobs?.firstOrNull { it.name == name } ?: SyncBlob(name)
    }

    override suspend fun upload(blob: SyncBlob): Long = withContext(Dispatchers.IO) {
        val gistId = findOrCreateGist()
            ?: error("Could not create a GitHub gist for syncing")
        val current = fetchManifest(gistId)
        val existing = current?.blobs?.firstOrNull { it.name == blob.name }
        val stored = blob.copy(
            version = maxOf(existing?.version ?: 0L, blob.version) + 1L,
            modifiedAt = Clock.System.now()
        )
        val merged = SyncManifest(
            schema = current?.schema ?: 1,
            deviceId = current?.deviceId ?: blob.name,
            blobs = (current?.blobs.orEmpty().filterNot { it.name == blob.name }) + stored
        )
        writeManifest(gistId, merged)
        stored.version
    }

    override suspend fun delete(name: String) = withContext(Dispatchers.IO) {
        val gistId = findOrCreateGist() ?: return@withContext
        val current = fetchManifest(gistId) ?: return@withContext
        writeManifest(
            gistId,
            SyncManifest(
                schema = current.schema,
                deviceId = current.deviceId,
                blobs = current.blobs.filterNot { it.name == name }
            )
        )
    }

    override suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val token = obtainToken()
            val body = getText("$API_URL/user", token)
            val login = json.parseToJsonElement(body).jsonObject["login"]?.jsonPrimitive?.content ?: "GitHub user"
            "Connected as $login"
        }
    }

    // ------------------------------------------------------------
    // Gist lifecycle
    // ------------------------------------------------------------

    private suspend fun findOrCreateGist(): String? {
        cachedGistId?.let { return it }
        stateFile.readTextOrNull()?.let { raw ->
            val saved = runCatching { json.decodeFromString<GistState>(raw) }.getOrNull()
            if (!saved?.gistId.isNullOrBlank()) {
                // Verify it still exists before trusting it.
                if (runCatching { getText("$API_URL/gists/${saved!!.gistId}", obtainToken()) }.isSuccess) {
                    cachedGistId = saved.gistId
                    return saved.gistId
                }
            }
        }

        // Search the account's gists for the Kaiteyo marker.
        // (A single 100-gist page: plenty for the typical account; a heavier
        // gist user would simply create a second Kaiteyo gist on next sync.)
        val token = obtainToken()
        val listing = getText("$API_URL/gists?per_page=100", token)
        val foundId = runCatching {
            json.parseToJsonElement(listing).jsonArray.firstOrNull { gist ->
                gist.jsonObject["description"]?.jsonPrimitive?.content == GIST_DESCRIPTION
            }?.jsonObject?.get("id")?.jsonPrimitive?.content
        }.getOrNull()

        if (!foundId.isNullOrBlank()) {
            cachedGistId = foundId
            persistGistId(foundId)
            return foundId
        }

        // Create a new private gist seeded with an empty manifest.
        val createBody = buildJsonObject {
            put("description", GIST_DESCRIPTION)
            put("public", false)
            put("files", buildJsonObject {
                put(MANIFEST_FILE_NAME, buildJsonObject {
                    put("content", json.encodeToString(SyncManifest()))
                })
            })
        }.toString()

        val created = sendJson("POST", "$API_URL/gists", createBody, token)
        val id = json.parseToJsonElement(created).jsonObject["id"]?.jsonPrimitive?.content
            ?: error("GitHub gist creation returned no id")
        cachedGistId = id
        persistGistId(id)
        return id
    }

    private fun persistGistId(id: String) {
        runCatching {
            stateDir.mkdirs()
            stateFile.writeText(json.encodeToString(GistState(gistId = id)))
        }.onFailure { Logger.w("GitHubGist: failed to persist gist id: ${it.message}") }
    }

    // ------------------------------------------------------------
    // Manifest read/write
    // ------------------------------------------------------------

    private suspend fun fetchManifest(gistId: String): SyncManifest? {
        val token = obtainToken()
        val gist = getText("$API_URL/gists/$gistId", token)
        val file = json.parseToJsonElement(gist).jsonObject["files"]?.jsonObject
            ?.get(MANIFEST_FILE_NAME)?.jsonObject
            ?: return null
        val content = file["content"]?.jsonPrimitive?.content
        val truncated = file["truncated"]?.jsonPrimitive?.content == "true"
        val rawUrl = file["raw_url"]?.jsonPrimitive?.content

        val raw = when {
            !content.isNullOrBlank() && !truncated -> content
            !rawUrl.isNullOrBlank() -> getText(rawUrl, token)
            else -> return null
        }
        return runCatching { json.decodeFromString<SyncManifest>(raw) }.getOrNull()
    }

    private suspend fun writeManifest(gistId: String, manifest: SyncManifest) {
        val token = obtainToken()
        val body = buildJsonObject {
            put("files", buildJsonObject {
                put(MANIFEST_FILE_NAME, buildJsonObject {
                    put("content", json.encodeToString(manifest))
                })
            })
        }.toString()
        // Editing a gist requires PATCH, not POST.
        sendJson("PATCH", "$API_URL/gists/$gistId", body, token)
    }

    // ------------------------------------------------------------
    // Auth + HTTP
    // ------------------------------------------------------------

    private suspend fun obtainToken(): String = runCatching {
        tokenProvider().getOrThrow()
    }.getOrElse { error("GitHub sync requires a connected account: ${it.message}") }

    /** GET with one token-refresh retry on 401. */
    private suspend fun getText(url: String, token: String): String {
        var attempt = 0
        while (true) {
            val response = send("GET", url, token, body = null)
            if (response.statusCode() == 401 && attempt == 0) {
                attempt++
                val refreshed = tokenProvider().getOrNull() ?: continue
                return send("GET", url, refreshed, body = null).let { resp ->
                    if (resp.statusCode() in 200..299) resp.body()
                    else error("GitHub request failed (HTTP ${resp.statusCode()})")
                }
            }
            if (response.statusCode() !in 200..299) {
                error("GitHub request failed (HTTP ${response.statusCode()})")
            }
            return response.body()
        }
    }

    /** POST/PATCH JSON with one token-refresh retry on 401. */
    private suspend fun sendJson(method: String, url: String, body: String, token: String): String {
        var attempt = 0
        while (true) {
            val response = send(method, url, token, body)
            if (response.statusCode() == 401 && attempt == 0) {
                attempt++
                val refreshed = tokenProvider().getOrNull() ?: continue
                val retried = send(method, url, refreshed, body)
                if (retried.statusCode() !in 200..299) {
                    error("GitHub request failed (HTTP ${retried.statusCode()})")
                }
                return retried.body()
            }
            if (response.statusCode() !in 200..299) {
                error("GitHub request failed (HTTP ${response.statusCode()})")
            }
            return response.body()
        }
    }

    private fun send(
        method: String,
        url: String,
        token: String,
        body: String?
    ): HttpResponse<String> {
        val builder = HttpRequest.newBuilder(URI.create(url))
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github.v3+json")
            .timeout(Duration.ofSeconds(30))
        if (body != null) {
            builder.header("Content-Type", "application/json")
            builder.method(method, HttpRequest.BodyPublishers.ofString(body))
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody())
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    private fun File.readTextOrNull(): String? = runCatching { if (exists()) readText() else null }.getOrNull()

    private companion object {
        const val API_URL = "https://api.github.com"
        const val GIST_DESCRIPTION = "Kaiteyo Sync"
        const val MANIFEST_FILE_NAME = "kaiteyo-sync.json"
    }
}

@Serializable
private data class GistState(val gistId: String = "")
