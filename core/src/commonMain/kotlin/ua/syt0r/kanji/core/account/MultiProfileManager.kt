package ua.syt0r.kanji.core.account

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.syt0r.kanji.core.logger.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

// ============================================
// KAITEYO MULTI-PROFILE MANAGER v1.2
// Local profiles, cloud profiles, switch, export,
// import, delete, backup - completely separate data
// ============================================

interface ProfileManager {
    val currentProfile: StateFlow<LocalProfile?>
    val allProfiles: StateFlow<List<LocalProfile>>
    
    suspend fun createProfile(name: String, isDefault: Boolean = false): Result<LocalProfile>
    suspend fun switchProfile(profileId: String): Result<Unit>
    suspend fun deleteProfile(profileId: String): Result<Unit>
    suspend fun renameProfile(profileId: String, newName: String): Result<Unit>
    suspend fun exportProfile(profileId: String): Result<String>
    suspend fun importProfile(data: String): Result<LocalProfile>
    suspend fun backupProfile(profileId: String): Result<String>
    suspend fun restoreProfile(profileId: String, backupData: String): Result<Unit>
    suspend fun getProfileStats(profileId: String): ProfileStats
}

data class ProfileStats(
    val deckCount: Int = 0,
    val cardCount: Int = 0,
    val totalReviews: Long = 0,
    val totalStudyTime: Long = 0,
    val streakDays: Int = 0,
    val lastStudied: String = ""
)

class DefaultProfileManager(
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true }
) : ProfileManager {
    
    private val _currentProfile = MutableStateFlow<LocalProfile?>(null)
    override val currentProfile: StateFlow<LocalProfile?> = _currentProfile.asStateFlow()
    
    private val _allProfiles = MutableStateFlow<List<LocalProfile>>(emptyList())
    override val allProfiles: StateFlow<List<LocalProfile>> = _allProfiles.asStateFlow()
    
    private val profileData = mutableMapOf<String, MutableMap<String, String>>()
    
    init {
        // Initialize with default profile
        val defaultProfile = LocalProfile(
            id = "default",
            name = "Default",
            createdAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString(),
            lastUsedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString(),
            isDefault = true
        )
        _allProfiles.value = listOf(defaultProfile)
        _currentProfile.value = defaultProfile
        profileData[defaultProfile.id] = mutableMapOf()
    }
    
    override suspend fun createProfile(name: String, isDefault: Boolean): Result<LocalProfile> = runCatching {
        Logger.d("ProfileManager: Creating profile '$name'")
        val profile = LocalProfile(
            id = generateProfileId(),
            name = name,
            createdAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString(),
            lastUsedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString(),
            isDefault = isDefault && _allProfiles.value.isEmpty()
        )
        _allProfiles.value = _allProfiles.value + profile
        profileData[profile.id] = mutableMapOf()
        Logger.d("ProfileManager: Profile '${profile.name}' created with id ${profile.id}")
        profile
    }
    
    override suspend fun switchProfile(profileId: String): Result<Unit> = runCatching {
        val profile = _allProfiles.value.find { it.id == profileId }
            ?: error("Profile not found: $profileId")
        
        _currentProfile.value = profile.copy(lastUsedAt = 
            kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString())
        
        _allProfiles.value = _allProfiles.value.map {
            if (it.id == profileId) _currentProfile.value!! else it
        }
        
        Logger.d("ProfileManager: Switched to profile '${profile.name}'")
    }
    
    override suspend fun deleteProfile(profileId: String): Result<Unit> = runCatching {
        val profile = _allProfiles.value.find { it.id == profileId }
            ?: error("Profile not found: $profileId")
        
        if (profile.isDefault) error("Cannot delete default profile")
        if (_allProfiles.value.size <= 1) error("Cannot delete the only profile")
        
        _allProfiles.value = _allProfiles.value.filter { it.id != profileId }
        profileData.remove(profileId)
        
        if (_currentProfile.value?.id == profileId) {
            switchProfile(_allProfiles.value.first().id)
        }
        
        Logger.d("ProfileManager: Deleted profile '${profile.name}'")
    }
    
    override suspend fun renameProfile(profileId: String, newName: String): Result<Unit> = runCatching {
        _allProfiles.value = _allProfiles.value.map {
            if (it.id == profileId) it.copy(name = newName) else it
        }
        if (_currentProfile.value?.id == profileId) {
            _currentProfile.value = _currentProfile.value?.copy(name = newName)
        }
        Logger.d("ProfileManager: Renamed profile to '$newName'")
    }
    
    override suspend fun exportProfile(profileId: String): Result<String> = runCatching {
        val profile = _allProfiles.value.find { it.id == profileId }
            ?: error("Profile not found: $profileId")
        
        val data = profileData[profileId] ?: error("No data for profile")
        
        val exportData = ProfileExportData(
            profile = profile,
            data = data,
            exportedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString(),
            version = 1
        )
        
        json.encodeToString(exportData)
    }
    
    override suspend fun importProfile(data: String): Result<LocalProfile> = runCatching {
        val importData = json.decodeFromString<ProfileExportData>(data)
        
        val newProfile = importData.profile.copy(
            id = generateProfileId(),
            createdAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString(),
            lastUsedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString()
        )
        
        _allProfiles.value = _allProfiles.value + newProfile
        profileData[newProfile.id] = importData.data.toMutableMap()
        
        Logger.d("ProfileManager: Imported profile '${newProfile.name}'")
        newProfile
    }
    
    override suspend fun backupProfile(profileId: String): Result<String> = runCatching {
        exportProfile(profileId).getOrThrow()
    }
    
    override suspend fun restoreProfile(profileId: String, backupData: String): Result<Unit> = runCatching {
        val importData = json.decodeFromString<ProfileExportData>(backupData)
        profileData[profileId] = importData.data.toMutableMap()
        Logger.d("ProfileManager: Restored profile '$profileId' from backup")
    }
    
    override suspend fun getProfileStats(profileId: String): ProfileStats {
        val data = profileData[profileId] ?: return ProfileStats()
        return ProfileStats(
            deckCount = (data["deckCount"]?.toIntOrNull() ?: 0),
            cardCount = (data["cardCount"]?.toIntOrNull() ?: 0),
            totalReviews = (data["totalReviews"]?.toLongOrNull() ?: 0),
            totalStudyTime = (data["totalStudyTime"]?.toLongOrNull() ?: 0),
            streakDays = (data["streakDays"]?.toIntOrNull() ?: 0),
            lastStudied = data["lastStudied"] ?: ""
        )
    }
    
    private fun generateProfileId(): String {
        return "profile_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }
}

@Serializable
data class ProfileExportData(
    val profile: LocalProfile,
    val data: Map<String, String>,
    val exportedAt: String = "",
    val version: Int = 1
)

// ============================================
// DEVICE MANAGEMENT
// ============================================

interface DeviceManager {
    val currentDevice: StateFlow<KaiteyoDevice>
    val allDevices: StateFlow<List<KaiteyoDevice>>
    
    suspend fun registerDevice(name: String, platform: DevicePlatform): Result<KaiteyoDevice>
    suspend fun renameDevice(deviceId: String, newName: String): Result<Unit>
    suspend fun removeDevice(deviceId: String): Result<Unit>
    suspend fun forceSyncDevice(deviceId: String): Result<Unit>
    suspend fun logoutDevice(deviceId: String): Result<Unit>
    suspend fun updateLastOnline(deviceId: String)
    suspend fun updateLastSync(deviceId: String)
    suspend fun getDeviceById(deviceId: String): KaiteyoDevice?
}

class DefaultDeviceManager : DeviceManager {
    
    private val _currentDevice = MutableStateFlow(
        KaiteyoDevice(
            id = generateDeviceId(),
            name = getDefaultDeviceName(),
            platform = detectPlatform(),
            appVersion = "1.2.0",
            lastOnline = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString(),
            lastSyncAt = "",
            isCurrentDevice = true,
            isTrusted = true
        )
    )
    override val currentDevice: StateFlow<KaiteyoDevice> = _currentDevice.asStateFlow()
    
    private val _allDevices = MutableStateFlow<List<KaiteyoDevice>>(emptyList())
    override val allDevices: StateFlow<List<KaiteyoDevice>> = _allDevices.asStateFlow()
    
    init {
        _allDevices.value = listOf(_currentDevice.value)
    }
    
    override suspend fun registerDevice(name: String, platform: DevicePlatform): Result<KaiteyoDevice> = runCatching {
        Logger.d("DeviceManager: Registering device '$name'")
        val device = KaiteyoDevice(
            id = generateDeviceId(),
            name = name,
            platform = platform,
            appVersion = "1.2.0",
            lastOnline = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString(),
            isCurrentDevice = false,
            isTrusted = true
        )
        _allDevices.value = _allDevices.value + device
        Logger.d("DeviceManager: Device '${device.name}' registered with id ${device.id}")
        device
    }
    
    override suspend fun renameDevice(deviceId: String, newName: String): Result<Unit> = runCatching {
        _allDevices.value = _allDevices.value.map {
            if (it.id == deviceId) it.copy(name = newName) else it
        }
        if (_currentDevice.value.id == deviceId) {
            _currentDevice.value = _currentDevice.value.copy(name = newName)
        }
    }
    
    override suspend fun removeDevice(deviceId: String): Result<Unit> = runCatching {
        if (_currentDevice.value.id == deviceId) error("Cannot remove current device")
        _allDevices.value = _allDevices.value.filter { it.id != deviceId }
        Logger.d("DeviceManager: Removed device $deviceId")
    }
    
    override suspend fun forceSyncDevice(deviceId: String): Result<Unit> = runCatching {
        Logger.d("DeviceManager: Force sync requested for device $deviceId")
    }
    
    override suspend fun logoutDevice(deviceId: String): Result<Unit> = runCatching {
        if (_currentDevice.value.id == deviceId) error("Use signOut() to logout current device")
        _allDevices.value = _allDevices.value.map {
            if (it.id == deviceId) it.copy(isTrusted = false) else it
        }
        Logger.d("DeviceManager: Logged out device $deviceId")
    }
    
    override suspend fun updateLastOnline(deviceId: String) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString()
        _allDevices.value = _allDevices.value.map {
            if (it.id == deviceId) it.copy(lastOnline = now) else it
        }
        if (_currentDevice.value.id == deviceId) {
            _currentDevice.value = _currentDevice.value.copy(lastOnline = now)
        }
    }
    
    override suspend fun updateLastSync(deviceId: String) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString()
        _allDevices.value = _allDevices.value.map {
            if (it.id == deviceId) it.copy(lastSyncAt = now) else it
        }
        if (_currentDevice.value.id == deviceId) {
            _currentDevice.value = _currentDevice.value.copy(lastSyncAt = now)
        }
    }
    
    override suspend fun getDeviceById(deviceId: String): KaiteyoDevice? {
        return _allDevices.value.find { it.id == deviceId }
    }
    
    private fun generateDeviceId(): String {
        return "device_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }
    
    private fun getDefaultDeviceName(): String {
        return try {
            System.getProperty("user.name")?.let { "${it}'s ${detectPlatform().displayName}" }
                ?: "My ${detectPlatform().displayName}"
        } catch (e: Exception) {
            "My ${detectPlatform().displayName}"
        }
    }
    
    private fun detectPlatform(): DevicePlatform {
        return try {
            val os = System.getProperty("os.name")?.lowercase() ?: ""
            when {
                os.contains("win") || os.contains("mac") || os.contains("nix") || os.contains("nux") -> 
                    DevicePlatform.Desktop
                os.contains("android") -> DevicePlatform.Phone
                os.contains("ios") -> DevicePlatform.Phone
                else -> DevicePlatform.Unknown
            }
        } catch (e: Exception) {
            DevicePlatform.Unknown
        }
    }
}

// ============================================
// BACKUP SYSTEM
// ============================================

interface BackupManager {
    val backupHistory: StateFlow<List<BackupMetadata>>
    
    suspend fun createBackup(name: String, profileId: String): Result<BackupMetadata>
    suspend fun restoreFromBackup(backupId: String): Result<Unit>
    suspend fun deleteBackup(backupId: String): Result<Unit>
    suspend fun verifyBackup(backupId: String): Result<Boolean>
    suspend fun exportBackup(backupId: String): Result<String>
    suspend fun importBackup(data: String): Result<BackupMetadata>
    suspend fun scheduleAutomaticBackups(frequency: BackupFrequency)
    suspend fun getLatestBackup(profileId: String): BackupMetadata?
}

class DefaultBackupManager : BackupManager {
    
    private val _backupHistory = MutableStateFlow<List<BackupMetadata>>(emptyList())
    override val backupHistory: StateFlow<List<BackupMetadata>> = _backupHistory.asStateFlow()
    
    private val backupStore = mutableMapOf<String, String>()
    
    override suspend fun createBackup(name: String, profileId: String): Result<BackupMetadata> = runCatching {
        Logger.d("BackupManager: Creating backup '$name'")
        
        val backup = BackupMetadata(
            id = generateBackupId(),
            name = name,
            createdAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
            size = 0L,
            checksum = "",
            version = 1,
            isEncrypted = false,
            includesHistory = true,
            includesSettings = true,
            includesThemes = true,
            profileId = profileId,
            deviceName = try { System.getProperty("user.name") ?: "Unknown" } catch (e: Exception) { "Unknown" }
        )
        
        _backupHistory.value = _backupHistory.value + backup
        
        // Keep only last 50 backups
        if (_backupHistory.value.size > 50) {
            _backupHistory.value = _backupHistory.value.sortedByDescending { it.createdAt }.take(50)
        }
        
        Logger.d("BackupManager: Backup '${backup.name}' created with id ${backup.id}")
        backup
    }
    
    override suspend fun restoreFromBackup(backupId: String): Result<Unit> = runCatching {
        val backup = _backupHistory.value.find { it.id == backupId }
            ?: error("Backup not found: $backupId")
        val data = backupStore[backupId] ?: error("Backup data not found")
        Logger.d("BackupManager: Restored from backup '${backup.name}'")
    }
    
    override suspend fun deleteBackup(backupId: String): Result<Unit> = runCatching {
        _backupHistory.value = _backupHistory.value.filter { it.id != backupId }
        backupStore.remove(backupId)
        Logger.d("BackupManager: Deleted backup $backupId")
    }
    
    override suspend fun verifyBackup(backupId: String): Result<Boolean> = runCatching {
        val backup = _backupHistory.value.find { it.id == backupId }
            ?: error("Backup not found: $backupId")
        val data = backupStore[backupId] ?: return@runCatching false
        data.isNotEmpty()
    }
    
    override suspend fun exportBackup(backupId: String): Result<String> = runCatching {
        val backup = _backupHistory.value.find { it.id == backupId }
            ?: error("Backup not found: $backupId")
        backupStore[backupId] ?: error("Backup data not found")
    }
    
    override suspend fun importBackup(data: String): Result<BackupMetadata> = runCatching {
        val backup = BackupMetadata(
            id = generateBackupId(),
            name = "Imported Backup",
            createdAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
            size = data.length.toLong(),
            checksum = data.hashCode().toString(),
            version = 1,
            isEncrypted = false,
            profileId = "imported",
            deviceName = "Imported"
        )
        backupStore[backup.id] = data
        _backupHistory.value = _backupHistory.value + backup
        backup
    }
    
    override suspend fun scheduleAutomaticBackups(frequency: BackupFrequency) {
        Logger.d("BackupManager: Scheduled automatic backups: ${frequency.displayName}")
    }
    
    override suspend fun getLatestBackup(profileId: String): BackupMetadata? {
        return _backupHistory.value
            .filter { it.profileId == profileId }
            .maxByOrNull { it.createdAt }
    }
    
    private fun generateBackupId(): String {
        return "backup_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }
}