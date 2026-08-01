package ua.syt0r.kanji.core.sync

import kotlinx.serialization.Serializable

// ════════════════════════════════════════════
// SYNC DATA MODELS
// Core types for synchronization engine
// ════════════════════════════════════════════

@Serializable
data class SyncOperation(
    val objectId: String = "",
    val objectType: SyncObjectType = SyncObjectType.Card,
    val operation: SyncOperationType = SyncOperationType.Create,
    val timestamp: Long = 0L,
    val data: String = "",
    val version: Int = 1
) {
    val displayName: String get() = operation.displayName
}

enum class SyncObjectType {
    Card,
    Deck,
    Tag,
    Flag,
    Note,
    Settings,
    Stats,
    Backup,
    Media,
    Plugin
}

enum class SyncOperationType(val displayName: String) {
    Create("Create"),
    Update("Update"),
    Delete("Delete"),
    Modify("Modify"),
    Merge("Merge"),
    Restore("Restore"),
    Resolve("Resolve")
}

data class SyncConflict(
    val objectId: String = "",
    val objectType: SyncObjectType = SyncObjectType.Card,
    val operationType: SyncOperationType = SyncOperationType.Update,
    val localData: String = "",
    val remoteData: String = "",
    val localModifiedAt: Long = 0L,
    val remoteModifiedAt: Long = 0L,
    val operation: SyncOperation = SyncOperation()
)

enum class ConflictResolutionStrategy {
    KeepNewest,
    KeepLocal,
    KeepRemote,
    Merge,
    Manual
}

// Sync engine state types

data class SyncStateInfo(
    val status: SyncStatus = SyncStatus.Disconnected,
    val lastSyncTimestamp: Long = 0L,
    val pendingOperations: Int = 0,
    val pendingChanges: Int = 0,
    val conflictsCount: Int = 0,
    val lastErrorMessage: String? = null,
    val lastSuccessfulSync: String = "",
    val currentOperation: String = "",
    val totalChanges: Int = 0,
    val progress: Float = 0f
)

data class SyncStatistics(
    val totalSyncs: Int = 0,
    val totalUploaded: Int = 0,
    val totalDownloaded: Int = 0,
    val totalConflicts: Int = 0,
    val totalErrors: Int = 0,
    val lastSyncDuration: Long = 0L,
    val bytesUploaded: Long = 0L,
    val bytesDownloaded: Long = 0L,
    val averageSyncDuration: Long = 0L
)

enum class SyncStatus {
    Disconnected,
    Connecting,
    Connected,
    Syncing,
    Uploading,
    Downloading,
    UpToDate,
    Success,
    Error,
    Conflict
}

// GitHub specific types

data class GitHubAuthState(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresAt: Long = 0L,
    val scope: String = "",
    val isAuthenticated: Boolean = false
)

data class GitHubRepoInfo(
    val owner: String = "",
    val name: String = "",
    val branch: String = "main",
    val path: String = ""
)
