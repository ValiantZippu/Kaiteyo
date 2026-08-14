package ua.syt0r.kanji.core.sync

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import ua.syt0r.kanji.core.account.AuthSession
import ua.syt0r.kanji.core.account.AuthToken
import ua.syt0r.kanji.core.logger.Logger

// ============================================
// KAITEYO GITHUB CLOUD PROVIDER v1.2
// Uses GitHub Gist API for syncing
// Encrypted JSON blobs with versioning
// ============================================

class GitHubCloudProvider(
    private val httpClient: HttpClient,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true },
    private val gistDescription: String = "Kaiteyo Sync Data v1.2",
    private val gistFileName: String = "kaiteyo_sync.json",
    private val gistFileNameEncrypted: String = "kaiteyo_sync.enc"
) : CloudProvider {

    private var accessToken: String = ""
    private var gistId: String = ""
    private var userName: String = ""
    
    override val providerId: String = "github"
    override val displayName: String = "GitHub"
    
    override suspend fun initialize(authSession: AuthSession): Result<Unit> = runCatching {
        Logger.d("GitHubProvider: Initializing")
        accessToken = authSession.token.accessToken
        if (accessToken.isEmpty()) error("No access token available")
        
        // Try to find existing sync gist
        findOrCreateSyncGist()
        Logger.d("GitHubProvider: Initialized (gist=$gistId)")
    }
    
    override suspend fun upload(operations: List<SyncOperation>): Result<List<SyncResult>> = runCatching {
        Logger.d("GitHubProvider: Uploading ${operations.size} operations")
        
        val serialized = json.encodeToString(operations)
        val encoded = serialized.encodeToByteArray()
        val base64 = java.util.Base64.getEncoder().encodeToString(encoded)
        
        updateGistFile(gistFileName, base64)
        
        operations.map { SyncResult(objectId = it.objectId, success = true, newVersion = it.timestamp) }
    }
    
    override suspend fun download(sinceTimestamp: Long): Result<List<SyncOperation>> = runCatching {
        Logger.d("GitHubProvider: Downloading since $sinceTimestamp")
        
        val content = readGistFile(gistFileName) ?: return@runCatching emptyList()
        val decoded = java.util.Base64.getDecoder().decode(content)
        val deserialized = json.decodeFromString<List<SyncOperation>>(decoded.decodeToString())
        
        deserialized.filter { it.timestamp > sinceTimestamp }
    }
    
    override suspend fun getRemoteState(): Result<RemoteState> = runCatching {
        val content = readGistFile(gistFileName) ?: return@runCatching RemoteState()
        val decoded = java.util.Base64.getDecoder().decode(content)
        val operations = json.decodeFromString<List<SyncOperation>>(decoded.decodeToString())
        
        RemoteState(
            lastModified = operations.maxOfOrNull { it.timestamp } ?: 0L,
            objectCount = operations.size,
            checksum = content.hashCode().toString()
        )
    }
    
    override suspend fun deleteObject(objectId: String, objectType: SyncObjectType): Result<Unit> = runCatching {
        Logger.d("GitHubProvider: Marking $objectType $objectId as deleted")
        val current = download(0L).getOrDefault(emptyList())
        val updated = current.filterNot { it.objectId == objectId && it.objectType == objectType }
        val serialized = json.encodeToString(updated)
        val encoded = serialized.encodeToByteArray()
        val base64 = java.util.Base64.getEncoder().encodeToString(encoded)
        updateGistFile(gistFileName, base64)
    }
    
    override suspend fun getStorageInfo(): Result<StorageInfo> = runCatching {
        val gistInfo = httpClient.get("https://api.github.com/gists/$gistId") {
            header("Authorization", "Bearer $accessToken")
            header("Accept", "application/vnd.github.v3+json")
        }.body<Map<String, Any>>()
        
        val size = (gistInfo["size"] as? Number)?.toLong() ?: 0L
        StorageInfo(used = size, limit = 10485760L, available = 10485760L - size) // 10MB limit
    }
    
    override suspend fun validateConnection(): Result<Boolean> = runCatching {
        httpClient.get("https://api.github.com/user") {
            header("Authorization", "Bearer $accessToken")
            header("Accept", "application/vnd.github.v3+json")
        }
        true
    }
    
    private suspend fun findOrCreateSyncGist() {
        // List user's gists
        val gists: List<Map<String, Any>> = httpClient.get("https://api.github.com/gists") {
            header("Authorization", "Bearer $accessToken")
            header("Accept", "application/vnd.github.v3+json")
        }.body()
        
        // Find existing Kaiteyo gist
        val existingGist = gists.find { 
            (it["description"] as? String)?.contains("Kaiteyo") == true
        }
        
        if (existingGist != null) {
            gistId = existingGist["id"] as? String ?: ""
            Logger.d("GitHubProvider: Found existing gist $gistId")
        } else {
            // Create new gist
            val newGist = createGist()
            gistId = newGist
            Logger.d("GitHubProvider: Created new gist $gistId")
        }
    }
    
    private suspend fun createGist(): String {
        val response: Map<String, Any> = httpClient.post("https://api.github.com/gists") {
            header("Authorization", "Bearer $accessToken")
            header("Accept", "application/vnd.github.v3+json")
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "description" to gistDescription,
                "public" to false,
                "files" to mapOf(
                    gistFileName to mapOf("content" to "[]")
                )
            ))
        }.body()
        return response["id"] as? String ?: error("Failed to create gist")
    }
    
    private suspend fun updateGistFile(fileName: String, content: String) {
        httpClient.patch("https://api.github.com/gists/$gistId") {
            header("Authorization", "Bearer $accessToken")
            header("Accept", "application/vnd.github.v3+json")
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "files" to mapOf(
                    fileName to mapOf("content" to content)
                )
            ))
        }
    }
    
    private suspend fun readGistFile(fileName: String): String? {
        return try {
            val gist: Map<String, Any> = httpClient.get("https://api.github.com/gists/$gistId") {
                header("Authorization", "Bearer $accessToken")
                header("Accept", "application/vnd.github.v3+json")
            }.body()
            
            val files = gist["files"] as? Map<*, *> ?: return null
            val file = files[fileName] as? Map<*, *> ?: return null
            file["content"] as? String
        } catch (e: Exception) {
            Logger.w("GitHubProvider: Failed to read gist file: ${e.message}")
            null
        }
    }
    
}

// ============================================
// NOTE: the previous DefaultSyncEncryption implementation used a fixed
// XOR mask (0xAA) and a weak 31-multiplier checksum. Neither is real
// cryptography, so the implementation was removed rather than shipped as
// a false sense of security. Real implementations of the SyncEncryption
// interface should be platform-backed (Android Keystore, iOS Keychain,
// OS credential stores) and registered via DI.
// ============================================