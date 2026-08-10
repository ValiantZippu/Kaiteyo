package ua.syt0r.kanji.desktop.engine.account

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ua.syt0r.kanji.core.logger.Logger
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.Base64

// ============================================
// GITHUB OAUTH DEVICE FLOW (RFC 8628)
// Real, working device authorization flow:
//   1. requestDeviceCode  → user code + URL
//   2. pollForToken       → polls until the user
//      authorizes (handles pending / slow_down /
//      expired_token / access_denied)
//   3. refreshToken       → long-lived refresh
//   4. revokeToken        → best-effort revoke
//   5. fetchUserInfo      → profile for the UI
// Uses the JDK's java.net.http client (no extra
// dependencies); all calls are suspend + IO.
// ============================================

class GitHubDeviceFlowClient(
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    data class DeviceCodeInfo(
        val deviceCode: String,
        val userCode: String,
        val verificationUri: String,
        val intervalSeconds: Int,
        val expiresInSeconds: Int
    )

    data class TokenResult(
        val accessToken: String,
        val refreshToken: String,
        val expiresInSeconds: Int,
        val scope: String
    )

    data class GitHubUser(
        val id: Long,
        val login: String,
        val name: String,
        val avatarUrl: String,
        val email: String
    )

    suspend fun requestDeviceCode(clientId: String): Result<DeviceCodeInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val body = postForm(
                DEVICE_CODE_URL,
                mapOf("client_id" to clientId, "scope" to "read:user user:email gist")
            )
            val obj = json.parseToJsonElement(body).jsonObject
            DeviceCodeInfo(
                deviceCode = obj["device_code"]?.jsonPrimitive?.content
                    ?: error("GitHub response missing device_code"),
                userCode = obj["user_code"]?.jsonPrimitive?.content
                    ?: error("GitHub response missing user_code"),
                verificationUri = obj["verification_uri"]?.jsonPrimitive?.content
                    ?: "https://github.com/login/device",
                intervalSeconds = obj["interval"]?.jsonPrimitive?.content?.toIntOrNull() ?: 5,
                expiresInSeconds = obj["expires_in"]?.jsonPrimitive?.content?.toIntOrNull() ?: 900
            )
        }.onSuccess { Logger.d("GitHubDeviceFlow: device code issued for $clientId") }
    }

    /** Polls until the user authorizes or the flow fails. Suspends between polls. */
    suspend fun pollForToken(
        clientId: String,
        deviceCode: String,
        intervalSeconds: Int,
        expiresAtEpochMs: Long
    ): Result<TokenResult> = withContext(Dispatchers.IO) {
        runCatching {
            var interval = intervalSeconds.coerceAtLeast(5)
            while (true) {
                delay(interval * 1000L)
                val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                if (expiresAtEpochMs in 1..now) error("The device code expired — please try again")

                val response = postForm(
                    TOKEN_URL,
                    mapOf(
                        "client_id" to clientId,
                        "device_code" to deviceCode,
                        "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                        "accept" to "application/json"
                    )
                )
                val obj = json.parseToJsonElement(response).jsonObject
                when (val error = obj["error"]?.jsonPrimitive?.content) {
                    null -> return@runCatching TokenResult(
                        accessToken = obj["access_token"]?.jsonPrimitive?.content
                            ?: error("GitHub response missing access_token"),
                        refreshToken = obj["refresh_token"]?.jsonPrimitive?.content ?: "",
                        // GitHub device-flow access tokens are long-lived; the
                        // absence of expires_in means the token does not expire.
                        expiresInSeconds = obj["expires_in"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        scope = obj["scope"]?.jsonPrimitive?.content ?: ""
                    )

                    "authorization_pending" -> Unit // keep polling
                    "slow_down" -> interval += 5     // GitHub asks for a longer pause
                    "expired_token" -> error("The device code expired — please try again")
                    "access_denied" -> error("Authorization was denied")
                    else -> error("GitHub returned: $error")
                }
            }
            @Suppress("UNREACHABLE_CODE")
            error("GitHub polling loop should never complete without a result")
        }.onSuccess { Logger.d("GitHubDeviceFlow: token received") }
    }

    suspend fun refreshToken(clientId: String, refreshToken: String): Result<TokenResult> = withContext(Dispatchers.IO) {
        runCatching {
            val body = postForm(
                TOKEN_URL,
                mapOf(
                    "client_id" to clientId,
                    "refresh_token" to refreshToken,
                    "grant_type" to "refresh_token"
                )
            )
            val obj = json.parseToJsonElement(body).jsonObject
            TokenResult(
                accessToken = obj["access_token"]?.jsonPrimitive?.content
                    ?: error("GitHub response missing access_token"),
                refreshToken = obj["refresh_token"]?.jsonPrimitive?.content ?: refreshToken,
                expiresInSeconds = obj["expires_in"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                scope = obj["scope"]?.jsonPrimitive?.content ?: ""
            )
        }
    }

    /**
     * Best-effort token revocation. Device-flow (public client) tokens cannot
     * be revoked server-side without a client secret; we still attempt the
     * endpoint and always drop the local copy on disconnect.
     */
    suspend fun revokeToken(clientId: String, accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val basic = "Basic " + Base64.getEncoder().encodeToString("$clientId:".encodeToByteArray())
            client.send(
                HttpRequest.newBuilder(URI.create("https://api.github.com/applications/$clientId/token"))
                    .header("Authorization", basic)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("Content-Type", "application/json")
                    .DELETE()
                    .timeout(Duration.ofSeconds(15))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            )
            Unit
        }.onFailure { Logger.w("GitHubDeviceFlow: revocation failed (non-fatal): ${it.message}") }
    }

    suspend fun fetchUserInfo(accessToken: String): Result<GitHubUser> = withContext(Dispatchers.IO) {
        runCatching {
            val userObj = getJson("$API_URL/user", accessToken)
            val emails = runCatching {
                json.parseToJsonElement(getText("$API_URL/user/emails", accessToken)).jsonArray
            }.getOrNull() as? JsonArray

            val primaryEmail = emails?.firstOrNull { element ->
                element.jsonObject["primary"]?.jsonPrimitive?.content == "true"
            }?.jsonObject?.get("email")?.jsonPrimitive?.content ?: ""

            GitHubUser(
                id = userObj["id"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                login = userObj["login"]?.jsonPrimitive?.content ?: "",
                name = userObj["name"]?.jsonPrimitive?.content ?: "",
                avatarUrl = userObj["avatar_url"]?.jsonPrimitive?.content ?: "",
                email = primaryEmail
            )
        }
    }

    // ------------------------------------------------------------
    // HTTP helpers (blocking — callers already run on Dispatchers.IO)
    // ------------------------------------------------------------

    private fun postForm(url: String, params: Map<String, String>): String {
        val form = params.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, StandardCharsets.UTF_8)}=${URLEncoder.encode(v, StandardCharsets.UTF_8)}"
        }
        val request = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .timeout(Duration.ofSeconds(30))
            .build()
        return send(request)
    }

    private fun getJson(url: String, accessToken: String): kotlinx.serialization.json.JsonObject =
        json.parseToJsonElement(getText(url, accessToken)).jsonObject

    private fun getText(url: String, accessToken: String): String {
        val request = HttpRequest.newBuilder(URI.create(url))
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/vnd.github.v3+json")
            .GET()
            .timeout(Duration.ofSeconds(30))
            .build()
        return send(request)
    }

    private fun send(request: HttpRequest): String {
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            error("GitHub request failed (HTTP ${response.statusCode()})")
        }
        return response.body()
    }

    private companion object {
        const val DEVICE_CODE_URL = "https://github.com/login/device/code"
        const val TOKEN_URL = "https://github.com/login/oauth/access_token"
        const val API_URL = "https://api.github.com"
    }
}
