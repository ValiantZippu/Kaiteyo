package ua.syt0r.kanji.desktop.engine.account

import kotlinx.serialization.Serializable
import ua.syt0r.kanji.desktop.engine.sync.ConflictResolution

// ============================================
// KAITEYO DESKTOP ACCOUNT MODELS
// Identity, providers, devices, sessions and
// account settings. Everything persisted by the
// AccountEngine is serializable so the account
// can be exported/imported and later synced.
//
// Authentication tokens are NEVER stored in
// these models — they live in the TokenVault,
// which is never serialized into plain files.
// ============================================

@Serializable
enum class ProviderKind(val id: String, val displayName: String, val tagline: String) {
    Local("local", "Local Account", "Offline-first profile — no cloud account required"),
    GitHub("github", "GitHub", "Sign in with GitHub for cloud synchronization"),
    Google("google", "Google", "Sign in with Google"),
    Apple("apple", "Apple", "Sign in with Apple"),
    Microsoft("microsoft", "Microsoft", "Sign in with Microsoft");

    companion object {
        fun fromId(id: String): ProviderKind = entries.firstOrNull { it.id == id } ?: Local
    }
}

@Serializable
enum class ConnectionStatus { Connected, Available, NotConfigured, Error }

@Serializable
data class ProviderConnection(
    val kind: ProviderKind = ProviderKind.Local,
    val userId: String = "",
    val displayName: String = "",
    val connectedAtEpochMs: Long = 0L,
    val lastUsedAtEpochMs: Long = 0L,
    val status: ConnectionStatus = ConnectionStatus.Available,
    val errorMessage: String = ""
) {
    val isConnected: Boolean get() = status == ConnectionStatus.Connected
}

@Serializable
data class AccountIdentity(
    val id: String = "",
    val displayName: String = "Learner",
    val username: String = "",
    val email: String = "",
    val joinedAtEpochMs: Long = 0L,
    val avatarSeed: String = "",
    val learnerLevel: String = "beginner",
    val isLocalOnly: Boolean = true
)

@Serializable
data class AccountDevice(
    val id: String = "",
    val name: String = "This device",
    val platform: String = "Desktop",
    val appVersion: String = "",
    val databaseVersion: Int = 0,
    val lastOnlineEpochMs: Long = 0L,
    val lastSyncEpochMs: Long = 0L,
    val isCurrent: Boolean = false,
    val isTrusted: Boolean = true
)

@Serializable
data class AccountSession(
    val id: String = "",
    val deviceId: String = "",
    val providerKind: ProviderKind = ProviderKind.Local,
    val createdAtEpochMs: Long = 0L,
    val lastActiveAtEpochMs: Long = 0L,
    val isCurrent: Boolean = false
)

@Serializable
data class AccountSettingsData(
    val autoSync: Boolean = false,
    val syncIntervalMinutes: Int = 30,
    val syncOnStart: Boolean = false,
    /** How to resolve blobs changed on both sides during a sync. */
    val conflictResolution: ConflictResolution = ConflictResolution.Skip,
    val encryptLocalData: Boolean = true,
    val notifySyncCompleted: Boolean = true,
    val notifySyncFailed: Boolean = true,
    val notifyReviewReminders: Boolean = false,
    val allowAnonymousTelemetry: Boolean = false,
    val debugLogging: Boolean = false,
    /** GitHub OAuth App client id used for the device flow (public value, not a secret). */
    val githubClientId: String = ""
)

/** OAuth token material — kept inside the [TokenVault], never in plain JSON state files. */
@Serializable
data class AuthToken(
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresAtEpochMs: Long = 0L,
    val tokenType: String = "bearer",
    val scope: String = ""
) {
    val isExpired: Boolean
        get() = expiresAtEpochMs in 1 until kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
}

/** One slice of on-disk storage, e.g. dictionaries, backups or media. */
data class StorageCategory(
    val key: String,
    val label: String,
    val bytes: Long,
    val fileCount: Int
)

data class StorageBreakdown(
    val categories: List<StorageCategory> = emptyList(),
    val totalBytes: Long = 0L
) {
    val sorted: List<StorageCategory> get() = categories.sortedByDescending { it.bytes }
}

/** State machine for the provider connect (OAuth device flow). */
sealed interface AuthFlowState {
    data object Idle : AuthFlowState

    /** The device-flow request is in flight — shown while contacting the provider. */
    data object Polling : AuthFlowState

    data class AwaitingAuthorization(
        val provider: ProviderKind,
        val verificationUri: String,
        val userCode: String,
        val expiresAtEpochMs: Long
    ) : AuthFlowState

    data class Completed(
        val provider: ProviderKind,
        val accountName: String
    ) : AuthFlowState

    data class Failed(
        val provider: ProviderKind,
        val message: String,
        val retryable: Boolean
    ) : AuthFlowState
}
