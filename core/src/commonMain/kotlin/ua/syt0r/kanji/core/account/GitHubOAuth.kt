package ua.syt0r.kanji.core.account

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import ua.syt0r.kanji.core.logger.Logger

// ============================================
// KAITEYO GITHUB OAUTH MODULE v1.2
// Device Flow for desktop, Standard OAuth for web
// Never stores credentials, only secure tokens
// ============================================

interface GitHubOAuthProvider {
    suspend fun requestDeviceCode(): Result<OAuthDeviceCodeResponse>
    suspend fun pollForToken(deviceCode: String, interval: Int): Result<OAuthTokenResponse>
    suspend fun refreshToken(refreshToken: String): Result<OAuthTokenResponse>
    suspend fun revokeToken(accessToken: String): Result<Unit>
    suspend fun getUserInfo(accessToken: String): Result<GitHubUserInfo>
}

data class GitHubUserInfo(
    val id: Long = 0,
    val login: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val email: String = ""
)

class DefaultGitHubOAuthProvider(
    private val httpClient: HttpClient,
    private val clientId: String = "KaiteyoDesktop",
    private val deviceFlowUrl: String = "https://github.com/login/device/code",
    private val tokenUrl: String = "https://github.com/login/oauth/access_token",
    private val apiUrl: String = "https://api.github.com",
    private val redirectUri: String = "kaiteyo://oauth/callback"
) : GitHubOAuthProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun requestDeviceCode(): Result<OAuthDeviceCodeResponse> = runCatching {
        Logger.d("GitHubOAuth: Requesting device code")
        val response: Map<String, String> = httpClient.submitForm(
            url = deviceFlowUrl,
            formParameters = parameters {
                append("client_id", clientId)
                append("scope", "read:user user:email")
            }
        ).body()
        
        OAuthDeviceCodeResponse(
            deviceCode = response["device_code"] ?: error("Missing device_code"),
            userCode = response["user_code"] ?: error("Missing user_code"),
            verificationUri = response["verification_uri"] ?: "https://github.com/login/device",
            interval = response["interval"]?.toIntOrNull() ?: 5,
            expiresIn = response["expires_in"]?.toIntOrNull() ?: 900
        ).also { Logger.d("GitHubOAuth: Device code received, user_code=${it.userCode}") }
    }

    override suspend fun pollForToken(deviceCode: String, interval: Int): Result<OAuthTokenResponse> = runCatching {
        Logger.d("GitHubOAuth: Polling for token")
        while (true) {
            kotlinx.coroutines.delay((interval * 1000).toLong())
            val response: Map<String, String> = httpClient.submitForm(
                url = tokenUrl,
                formParameters = parameters {
                    append("client_id", clientId)
                    append("device_code", deviceCode)
                    append("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                    append("accept", "application/json")
                }
            ).body()
            
            when {
                response.containsKey("access_token") -> {
                    Logger.d("GitHubOAuth: Token received")
                    return@runCatching OAuthTokenResponse(
                        accessToken = response["access_token"] ?: "",
                        refreshToken = response["refresh_token"] ?: "",
                        expiresIn = response["expires_in"]?.toIntOrNull() ?: 3600,
                        scope = response["scope"] ?: ""
                    )
                }
                response["error"] == "authorization_pending" -> continue
                response["error"] == "slow_down" -> {
                    kotlinx.coroutines.delay(5000)
                    continue
                }
                response["error"] == "expired_token" -> error("Device code expired")
                response["error"] == "access_denied" -> error("User denied authorization")
                else -> error("Unknown error: ${response["error"]}")
            }
        }
        @Suppress("UNREACHABLE_CODE")
        error("Polling loop should never complete")
    }

    override suspend fun refreshToken(refreshToken: String): Result<OAuthTokenResponse> = runCatching {
        Logger.d("GitHubOAuth: Refreshing token")
        val response: Map<String, String> = httpClient.submitForm(
            url = tokenUrl,
            formParameters = parameters {
                append("client_id", clientId)
                append("refresh_token", refreshToken)
                append("grant_type", "refresh_token")
            }
        ).body()
        
        OAuthTokenResponse(
            accessToken = response["access_token"] ?: error("Missing access_token"),
            refreshToken = response["refresh_token"] ?: refreshToken,
            expiresIn = response["expires_in"]?.toIntOrNull() ?: 3600
        ).also { Logger.d("GitHubOAuth: Token refreshed") }
    }

    override suspend fun revokeToken(accessToken: String): Result<Unit> = runCatching {
        Logger.d("GitHubOAuth: Revoking token")
        httpClient.post("https://api.github.com/applications/$clientId/token") {
            header("Authorization", "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(mapOf("access_token" to accessToken))
        }
        Logger.d("GitHubOAuth: Token revoked")
    }

    override suspend fun getUserInfo(accessToken: String): Result<GitHubUserInfo> = runCatching {
        Logger.d("GitHubOAuth: Fetching user info")
        val userResponse: JsonObject = httpClient.get("$apiUrl/user") {
            header("Authorization", "Bearer $accessToken")
            header("Accept", "application/vnd.github.v3+json")
        }.body()
        
        val emailsResponse: List<JsonObject> = httpClient.get("$apiUrl/user/emails") {
            header("Authorization", "Bearer $accessToken")
            header("Accept", "application/vnd.github.v3+json")
        }.body()
        
        val primaryEmail = emailsResponse.firstOrNull { 
            it["primary"]?.jsonPrimitive?.content == "true" 
        }?.get("email")?.jsonPrimitive?.content ?: ""
        
        GitHubUserInfo(
            id = userResponse["id"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0,
            login = userResponse["login"]?.jsonPrimitive?.content ?: "",
            name = userResponse["name"]?.jsonPrimitive?.content ?: "",
            avatarUrl = userResponse["avatar_url"]?.jsonPrimitive?.content ?: "",
            email = primaryEmail
        ).also { Logger.d("GitHubOAuth: User info received: ${it.login}") }
    }
}

// ============================================
// TOKEN ENCRYPTION & SECURE STORAGE
//
// The SecureTokenStorage interface is the seam where platform keychains
// (Android Keystore, iOS Keychain, Windows Credential Manager, macOS
// Keychain) plug in. Implementations must never log token material and
// should prefer OS-backed secure storage over plain files.
//
// NOTE: a hardcoded XOR "encryption" implementation used to live here.
// It was removed because it is not real encryption and only created a
// false sense of security. The desktop suite ships a proper vault
// (ua.syt0r.kanji.desktop.engine.account.TokenVault) that persists
// tokens with a machine-bound key and restricted file permissions;
// the same interface should be implemented per platform here.
// ============================================

interface SecureTokenStorage {
    suspend fun saveToken(providerId: String, token: AuthToken)
    suspend fun getToken(providerId: String): AuthToken?
    suspend fun deleteToken(providerId: String)
    suspend fun hasToken(providerId: String): Boolean
    suspend fun listProviders(): List<String>
    suspend fun clearAllTokens()
}

// ============================================
// AUTHENTICATION MANAGER
// ============================================

enum class AuthState {
    Unauthenticated,
    Authenticating,
    Authenticated,
    TokenExpired,
    Error
}

data class AuthSession(
    val provider: AuthProvider = AuthProvider.Local(""),
    val token: AuthToken = AuthToken(),
    val userInfo: GitHubUserInfo? = null,
    val state: AuthState = AuthState.Unauthenticated,
    val lastAuthenticated: Long = 0L,
    val errorMessage: String = ""
)

class AuthenticationManager(
    private val gitHubOAuth: GitHubOAuthProvider,
    private val tokenStorage: SecureTokenStorage
) {
    private var currentSession: AuthSession = AuthSession()
    
    suspend fun authenticateWithGitHub(): Result<AuthSession> = runCatching {
        Logger.d("AuthManager: Starting GitHub OAuth device flow")
        currentSession = currentSession.copy(state = AuthState.Authenticating)
        
        val deviceCode = gitHubOAuth.requestDeviceCode().getOrThrow()
        Logger.d("AuthManager: Open $deviceCode.verificationUri and enter code ${deviceCode.userCode}")
        
        val token = gitHubOAuth.pollForToken(deviceCode.deviceCode, deviceCode.interval).getOrThrow()
        val userInfo = gitHubOAuth.getUserInfo(token.accessToken).getOrThrow()
        
        val authToken = AuthToken(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            expiresAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() + (token.expiresIn * 1000L),
            scope = token.scope
        )
        
        tokenStorage.saveToken("github", authToken)
        
        currentSession = AuthSession(
            provider = AuthProvider.GitHub(userInfo.id.toString()),
            token = authToken,
            userInfo = userInfo,
            state = AuthState.Authenticated,
            lastAuthenticated = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        )
        
        Logger.d("AuthManager: GitHub authentication successful for ${userInfo.login}")
        currentSession
    }
    
    suspend fun refreshCurrentToken(): Result<AuthSession> = runCatching {
        val refreshToken = currentSession.token.refreshToken
        if (refreshToken.isEmpty()) error("No refresh token available")
        
        val newToken = gitHubOAuth.refreshToken(refreshToken).getOrThrow()
        val authToken = AuthToken(
            accessToken = newToken.accessToken,
            refreshToken = newToken.refreshToken,
            expiresAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() + (newToken.expiresIn * 1000L)
        )
        
        tokenStorage.saveToken("github", authToken)
        currentSession = currentSession.copy(token = authToken, state = AuthState.Authenticated)
        currentSession
    }
    
    suspend fun signOut() {
        Logger.d("AuthManager: Signing out")
        try {
            gitHubOAuth.revokeToken(currentSession.token.accessToken)
        } catch (e: Exception) {
            Logger.w("AuthManager: Token revocation failed: ${e.message}")
        }
        tokenStorage.deleteToken("github")
        currentSession = AuthSession()
    }
    
    suspend fun signOutFromAllDevices() {
        Logger.d("AuthManager: Signing out from all devices")
        tokenStorage.clearAllTokens()
        try {
            gitHubOAuth.revokeToken(currentSession.token.accessToken)
        } catch (e: Exception) {
            Logger.w("AuthManager: Token revocation failed: ${e.message}")
        }
        currentSession = AuthSession()
    }
    
    fun getCurrentSession(): AuthSession = currentSession
    
    suspend fun isTokenValid(): Boolean {
        if (currentSession.state != AuthState.Authenticated) return false
        if (currentSession.token.accessToken.isEmpty()) return false
        if (currentSession.token.expiresAt <= kotlinx.datetime.Clock.System.now().toEpochMilliseconds()) {
            return try {
                refreshCurrentToken()
                true
            } catch (e: Exception) {
                currentSession = currentSession.copy(state = AuthState.TokenExpired)
                false
            }
        }
        return true
    }
    
    suspend fun restoreSession(): Boolean {
        val stored = tokenStorage.getToken("github") ?: return false
        currentSession = AuthSession(
            provider = AuthProvider.GitHub(""),
            token = stored,
            state = AuthState.Authenticated
        )
        return isTokenValid()
    }
}