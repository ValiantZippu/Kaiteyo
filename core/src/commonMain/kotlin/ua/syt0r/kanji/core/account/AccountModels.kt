package ua.syt0r.kanji.core.account

import kotlinx.serialization.Serializable

// ============================================
// KAITEYO ACCOUNT & SYNC DATA MODELS v1.2
// Modular authentication, profiles, devices
// ============================================

// --- AUTHENTICATION ---

@Serializable
sealed interface AuthProvider {
    val providerId: String
    val displayName: String
    
    @Serializable
    data class GitHub(val userId: String) : AuthProvider {
        override val providerId: String = "github"
        override val displayName: String = "GitHub"
    }
    
    @Serializable
    data class WebDAV(val serverUrl: String) : AuthProvider {
        override val providerId: String = "webdav"
        override val displayName: String = "WebDAV"
    }
    
    @Serializable
    data class SelfHosted(val serverUrl: String) : AuthProvider {
        override val providerId: String = "selfhosted"
        override val displayName: String = "Self-Hosted"
    }
    
    @Serializable
    data class Dropbox(val userId: String) : AuthProvider {
        override val providerId: String = "dropbox"
        override val displayName: String = "Dropbox"
    }
    
    @Serializable
    data class GoogleDrive(val userId: String) : AuthProvider {
        override val providerId: String = "googledrive"
        override val displayName: String = "Google Drive"
    }
    
    @Serializable
    data class OneDrive(val userId: String) : AuthProvider {
        override val providerId: String = "onedrive"
        override val displayName: String = "OneDrive"
    }
    
    @Serializable
    data class Local(val profileId: String) : AuthProvider {
        override val providerId: String = "local"
        override val displayName: String = "Local Only"
    }
}

@Serializable
data class AuthToken(
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresAt: Long = 0L,
    val tokenType: String = "bearer",
    val scope: String = ""
)

@Serializable
data class OAuthDeviceCodeResponse(
    val deviceCode: String = "",
    val userCode: String = "",
    val verificationUri: String = "",
    val interval: Int = 5,
    val expiresIn: Int = 900
)

@Serializable
data class OAuthTokenResponse(
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresIn: Int = 3600,
    val scope: String = ""
)

// --- PROFILE ---

@Serializable
data class KaiteyoProfile(
    val id: String = "",
    val displayName: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val email: String = "",
    val provider: AuthProvider = AuthProvider.Local(""),
    val joinedAt: String = "",
    val lastSyncAt: String = "",
    val isCloudProfile: Boolean = false,
    val storageUsed: Long = 0L,
    val storageLimit: Long = 0L,
    val deviceCount: Int = 0
)

// --- DEVICE ---

@Serializable
data class KaiteyoDevice(
    val id: String = "",
    val name: String = "",
    val platform: DevicePlatform = DevicePlatform.Unknown,
    val appVersion: String = "",
    val lastOnline: String = "",
    val lastSyncAt: String = "",
    val isCurrentDevice: Boolean = false,
    val isTrusted: Boolean = true
)

enum class DevicePlatform(val displayName: String) {
    Desktop("Desktop"),
    Laptop("Laptop"),
    Tablet("Tablet"),
    Phone("Phone"),
    Unknown("Unknown")
}

// --- SYNCHRONIZATION VERSIONING ---

@Serializable
data class SyncableObject(
    val id: String = "",
    val version: Long = 1,
    val createdAt: Long = 0L,
    val modifiedAt: Long = 0L,
    val lastSyncedAt: Long = 0L,
    val deviceId: String = "",
    val conflictStatus: ConflictStatus = ConflictStatus.None,
    val isDeleted: Boolean = false,
    val checksum: String = ""
)

enum class ConflictStatus(val displayName: String) {
    None("None"),
    LocalChanged("Local Changed"),
    RemoteChanged("Remote Changed"),
    BothChanged("Both Changed"),
    Merged("Merged"),
    Resolved("Resolved")
}

// --- CONFLICT RESOLUTION ---

@Serializable
data class SyncConflict(
    val objectId: String = "",
    val objectType: SyncObjectType = SyncObjectType.Card,
    val localVersion: Long = 0,
    val remoteVersion: Long = 0,
    val localModifiedAt: Long = 0,
    val remoteModifiedAt: Long = 0,
    val localData: String = "",
    val remoteData: String = "",
    val resolvedData: String = "",
    val fieldDiffs: List<FieldDiff> = emptyList()
)

@Serializable
data class FieldDiff(
    val fieldName: String = "",
    val localValue: String = "",
    val remoteValue: String = "",
    val isSelected: Boolean = false
)

enum class SyncObjectType(val displayName: String) {
    Card("Card"),
    Deck("Deck"),
    Tag("Tag"),
    Setting("Setting"),
    Theme("Theme"),
    Layout("Layout"),
    Bookmark("Bookmark"),
    Dictionary("Dictionary"),
    Shortcut("Shortcut"),
    Profile("Profile")
}

enum class ConflictResolutionStrategy(val displayName: String) {
    KeepLocal("Keep Local"),
    KeepRemote("Keep Remote"),
    Merge("Merge"),
    KeepNewest("Keep Newest"),
    KeepLocalForFields("Choose Individual Fields"),
    AlwaysLocal("Always Keep Local"),
    AlwaysRemote("Always Keep Cloud"),
    AskEachTime("Ask Each Time")
}

// --- SYNC OPERATIONS ---

data class SyncOperation(
    val objectId: String = "",
    val objectType: SyncObjectType = SyncObjectType.Card,
    val operation: SyncOperationType = SyncOperationType.Add,
    val data: String = "",
    val timestamp: Long = 0L
)

enum class SyncOperationType(val displayName: String) {
    Add("Added"),
    Modify("Modified"),
    Delete("Deleted"),
    Move("Moved"),
    Merge("Merged"),
    Rename("Renamed")
}

// --- SYNC STATUS ---

enum class SyncStatus(val displayName: String) {
    Syncing("Syncing"),
    UpToDate("Up to Date"),
    Offline("Offline"),
    Conflict("Conflict"),
    Waiting("Waiting"),
    Uploading("Uploading"),
    Downloading("Downloading"),
    Failed("Failed")
}

data class SyncStateInfo(
    val status: SyncStatus = SyncStatus.UpToDate,
    val lastSuccessfulSync: String = "",
    val currentOperation: String = "",
    val pendingChanges: Int = 0,
    val totalChanges: Int = 0,
    val progress: Float = 0f,
    val errorMessage: String = ""
)

// --- SYNC STATISTICS ---

data class SyncStatistics(
    val totalSyncs: Long = 0,
    val totalUploaded: Long = 0,
    val totalDownloaded: Long = 0,
    val totalConflicts: Long = 0,
    val totalErrors: Long = 0,
    val averageSyncDuration: Long = 0,
    val lastSyncSize: Long = 0,
    val dataSize: Long = 0
)

// --- SETTINGS ---

data class SyncSettings(
    val provider: AuthProvider = AuthProvider.Local(""),
    val autoSync: Boolean = true,
    val syncFrequency: SyncFrequency = SyncFrequency.Every15Minutes,
    val meteredNetwork: Boolean = false,
    val wifiOnly: Boolean = true,
    val conflictStrategy: ConflictResolutionStrategy = ConflictResolutionStrategy.AskEachTime,
    val autoBackup: Boolean = true,
    val backupFrequency: BackupFrequency = BackupFrequency.Daily,
    val maxBackups: Int = 10,
    val encryptLocalData: Boolean = true,
    val syncOnAppStart: Boolean = true,
    val syncOnAppResume: Boolean = true,
    val showSyncNotifications: Boolean = true
)

enum class SyncFrequency(val displayName: String, val minutes: Int) {
    Every5Minutes("Every 5 min", 5),
    Every15Minutes("Every 15 min", 15),
    Every30Minutes("Every 30 min", 30),
    EveryHour("Every hour", 60),
    Every6Hours("Every 6 hours", 360),
    Manual("Manual only", -1)
}

enum class BackupFrequency(val displayName: String) {
    EveryHour("Every hour"),
    Daily("Daily"),
    Weekly("Weekly"),
    Monthly("Monthly"),
    Manual("Manual only")
}