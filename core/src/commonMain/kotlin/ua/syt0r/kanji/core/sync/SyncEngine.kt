package ua.syt0r.kanji.core.sync

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.account.AuthProvider
import ua.syt0r.kanji.core.account.AuthSession
import ua.syt0r.kanji.core.account.AuthenticationManager
import ua.syt0r.kanji.core.account.SyncSettings
import ua.syt0r.kanji.core.logger.Logger

// ============================================
// KAITEYO MODULAR SYNC ENGINE v1.2
// Layers: Auth → Cloud Provider → Sync Engine
// → Conflict Resolver → Encryption → Background
// → Local Database Adapter
// Every module is replaceable
// ============================================

// --- CLOUD PROVIDER INTERFACE ---
// Implementations: GitHub, WebDAV, SelfHosted, Dropbox, etc.

interface CloudProvider {
    val providerId: String
    val displayName: String
    suspend fun initialize(authSession: AuthSession): Result<Unit>
    suspend fun upload(operations: List<SyncOperation>): Result<List<SyncResult>>
    suspend fun download(sinceTimestamp: Long): Result<List<SyncOperation>>
    suspend fun getRemoteState(): Result<RemoteState>
    suspend fun deleteObject(objectId: String, objectType: SyncObjectType): Result<Unit>
    suspend fun getStorageInfo(): Result<StorageInfo>
    suspend fun validateConnection(): Result<Boolean>
}

data class RemoteState(
    val lastModified: Long = 0L,
    val objectCount: Int = 0,
    val checksum: String = ""
)

data class StorageInfo(
    val used: Long = 0L,
    val limit: Long = 0L,
    val available: Long = 0L
)

data class SyncResult(
    val objectId: String = "",
    val success: Boolean = false,
    val newVersion: Long = 0,
    val errorMessage: String = ""
)

// --- LOCAL DATABASE ADAPTER ---

interface LocalDatabaseAdapter {
    suspend fun getModifiedSince(timestamp: Long): List<SyncOperation>
    suspend fun getDeletedSince(timestamp: Long): List<SyncOperation>
    suspend fun applyRemoteOperations(operations: List<SyncOperation>): Result<List<SyncResult>>
    suspend fun getObjectVersion(objectId: String, objectType: SyncObjectType): Long
    suspend fun getObjectData(objectId: String, objectType: SyncObjectType): String?
    suspend fun getLocalState(): RemoteState
    suspend fun getObjectCount(): Int
    suspend fun getChecksum(): String
}

// --- ENCRYPTION LAYER ---

interface SyncEncryption {
    suspend fun encrypt(data: ByteArray): ByteArray
    suspend fun decrypt(data: ByteArray): ByteArray
    suspend fun generateChecksum(data: ByteArray): String
    suspend fun verifyIntegrity(data: ByteArray, checksum: String): Boolean
}

// --- BACKGROUND TASK MANAGER ---

interface BackgroundSyncManager {
    val isRunning: Boolean
    suspend fun start(interval: Long)
    suspend fun stop()
    suspend fun triggerNow()
    suspend fun getStatus(): SyncStateInfo
}

// --- SYNC ENGINE ---

enum class EngineState {
    Idle,
    Initializing,
    Syncing,
    Uploading,
    Downloading,
    Conflict,
    Error,
    Offline
}

class SyncEngine(
    private val cloudProvider: CloudProvider,
    private val localDatabase: LocalDatabaseAdapter,
    private val conflictResolver: ConflictResolver,
    private val encryption: SyncEncryption,
    private val authenticationManager: AuthenticationManager,
    private val settings: MutableStateFlow<SyncSettings>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(dispatcher)
    private var syncJob: Job? = null
    private var backgroundJob: Job? = null
    
    private val _engineState = MutableStateFlow(EngineState.Idle)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()
    
    private val _syncStateInfo = MutableStateFlow(SyncStateInfo())
    val syncStateInfo: StateFlow<SyncStateInfo> = _syncStateInfo.asStateFlow()
    
    private val _syncStatistics = MutableStateFlow(SyncStatistics())
    val syncStatistics: StateFlow<SyncStatistics> = _syncStatistics.asStateFlow()
    
    private val offlineQueue = mutableListOf<SyncOperation>()
    private val changeTracker = ChangeTracker()
    
    suspend fun initialize(): Result<Unit> = runCatching {
        Logger.d("SyncEngine: Initializing with provider ${cloudProvider.displayName}")
        _engineState.value = EngineState.Initializing
        
        val session = authenticationManager.getCurrentSession()
        cloudProvider.initialize(session).getOrThrow()
        
        _engineState.value = EngineState.Idle
        Logger.d("SyncEngine: Initialized successfully")
    }
    
    suspend fun sync(): Result<SyncStateInfo> = runCatching {
        Logger.d("SyncEngine: Starting sync")
        _engineState.value = EngineState.Syncing
        _syncStateInfo.value = _syncStateInfo.value.copy(status = SyncStatus.Syncing)
        
        val startTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        
        // 1. Check authentication
        if (!authenticationManager.isTokenValid()) {
            Logger.d("SyncEngine: Token invalid, attempting refresh")
            authenticationManager.refreshCurrentToken()
        }
        
        // 2. Get local changes
        val localChanges = changeTracker.getPendingChanges()
        Logger.d("SyncEngine: ${localChanges.size} local changes pending")
        
        // 3. Upload local changes
        if (localChanges.isNotEmpty()) {
            _engineState.value = EngineState.Uploading
            _syncStateInfo.value = _syncStateInfo.value.copy(
                status = SyncStatus.Uploading,
                currentOperation = "Uploading ${localChanges.size} changes"
            )
            
            val uploadResult = cloudProvider.upload(localChanges).getOrThrow()
            val failed = uploadResult.count { !it.success }
            if (failed > 0) Logger.w("SyncEngine: $failed uploads failed")
            
            changeTracker.markAsSynced(localChanges)
        }
        
        // 4. Get remote state
        val remoteState = cloudProvider.getRemoteState().getOrThrow()
        
        // 5. Download remote changes
        val lastSyncTimestamp = _syncStateInfo.value.lastSuccessfulSync
            .toLongOrNull() ?: 0L
        
        _engineState.value = EngineState.Downloading
        _syncStateInfo.value = _syncStateInfo.value.copy(
            status = SyncStatus.Downloading,
            currentOperation = "Downloading remote changes"
        )
        
        val remoteOperations = cloudProvider.download(lastSyncTimestamp).getOrThrow()
        Logger.d("SyncEngine: ${remoteOperations.size} remote changes")
        
        // 6. Resolve conflicts
        val resolvedOperations = conflictResolver.resolve(remoteOperations, localDatabase)
        
        // 7. Apply remote changes locally
        val applyResult = localDatabase.applyRemoteOperations(resolvedOperations).getOrThrow()
        
        // 8. Update state
        val duration = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - startTime
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString()
        
        _syncStateInfo.value = SyncStateInfo(
            status = SyncStatus.UpToDate,
            lastSuccessfulSync = now,
            currentOperation = "Idle",
            pendingChanges = 0,
            totalChanges = localChanges.size + remoteOperations.size,
            progress = 1f
        )
        
        _syncStatistics.value = _syncStatistics.value.copy(
            totalSyncs = _syncStatistics.value.totalSyncs + 1,
            totalUploaded = _syncStatistics.value.totalUploaded + localChanges.size,
            totalDownloaded = _syncStatistics.value.totalDownloaded + remoteOperations.size,
            averageSyncDuration = (_syncStatistics.value.averageSyncDuration + duration) / 2
        )
        
        _engineState.value = EngineState.Idle
        Logger.d("SyncEngine: Sync completed in ${duration}ms")
        
        _syncStateInfo.value
    }
    
    fun startBackgroundSync(intervalMinutes: Int = 15) {
        backgroundJob?.cancel()
        backgroundJob = scope.launch {
            while (true) {
                delay(intervalMinutes * 60 * 1000L)
                if (settings.value.autoSync) {
                    sync()
                }
            }
        }
        Logger.d("SyncEngine: Background sync started (interval=${intervalMinutes}min)")
    }
    
    fun stopBackgroundSync() {
        backgroundJob?.cancel()
        backgroundJob = null
        Logger.d("SyncEngine: Background sync stopped")
    }
    
    fun queueOfflineOperation(operation: SyncOperation) {
        offlineQueue.add(operation)
        changeTracker.trackChange(operation)
        _syncStateInfo.value = _syncStateInfo.value.copy(
            pendingChanges = offlineQueue.size
        )
        Logger.d("SyncEngine: Queued offline operation: ${operation.operation.displayName} ${operation.objectId}")
    }
    
    suspend fun processOfflineQueue(): Result<Int> = runCatching {
        if (offlineQueue.isEmpty()) return@runCatching 0
        
        val operations = offlineQueue.toList()
        offlineQueue.clear()
        
        val result = cloudProvider.upload(operations).getOrThrow()
        val successCount = result.count { it.success }
        
        changeTracker.markAsSynced(operations)
        _syncStateInfo.value = _syncStateInfo.value.copy(pendingChanges = offlineQueue.size)
        
        Logger.d("SyncEngine: Processed $successCount/${operations.size} offline operations")
        successCount
    }
    
    fun cancelSync() {
        syncJob?.cancel()
        _engineState.value = EngineState.Idle
        _syncStateInfo.value = _syncStateInfo.value.copy(status = SyncStatus.UpToDate)
        Logger.d("SyncEngine: Sync cancelled")
    }
    
    fun getOfflineQueueSize(): Int = offlineQueue.size
    
    fun getChangeCount(): Int = changeTracker.getPendingChanges().size
}

// --- CHANGE TRACKER ---

class ChangeTracker {
    private val pendingChanges = mutableListOf<SyncOperation>()
    private val syncedChanges = mutableListOf<SyncOperation>()
    
    fun trackChange(operation: SyncOperation) {
        pendingChanges.removeAll { it.objectId == operation.objectId && it.objectType == operation.objectType }
        pendingChanges.add(operation)
    }
    
    fun getPendingChanges(): List<SyncOperation> = pendingChanges.toList()
    
    fun markAsSynced(operations: List<SyncOperation>) {
        operations.forEach { op ->
            pendingChanges.removeAll { it.objectId == op.objectId && it.objectType == op.objectType }
            syncedChanges.add(op)
        }
        if (syncedChanges.size > 1000) {
            syncedChanges.removeAt(0)
        }
    }
    
    fun clear() {
        pendingChanges.clear()
        syncedChanges.clear()
    }
}

// --- CONFLICT RESOLVER ---

interface ConflictResolver {
    suspend fun resolve(
        remoteOperations: List<SyncOperation>,
        localDatabase: LocalDatabaseAdapter
    ): List<SyncOperation>
    
    suspend fun resolveConflict(
        conflict: SyncConflict,
        strategy: ConflictResolutionStrategy
    ): SyncOperation
}

class DefaultConflictResolver(
    private val defaultStrategy: ConflictResolutionStrategy = ConflictResolutionStrategy.KeepNewest
) : ConflictResolver {
    
    override suspend fun resolve(
        remoteOperations: List<SyncOperation>,
        localDatabase: LocalDatabaseAdapter
    ): List<SyncOperation> {
        val resolved = mutableListOf<SyncOperation>()
        
        for (operation in remoteOperations) {
            val localVersion = localDatabase.getObjectVersion(
                operation.objectId, operation.objectType
            )
            
            if (localVersion == 0L) {
                // Object doesn't exist locally, apply remote
                resolved.add(operation)
            } else if (localVersion < operation.timestamp) {
                // Remote is newer, apply remote
                resolved.add(operation)
            } else if (localVersion > operation.timestamp) {
                // Local is newer, keep local (skip remote)
                Logger.d("ConflictResolver: Local newer for ${operation.objectId}, keeping local")
            } else {
                // Same version, check for conflicts
                val localData = localDatabase.getObjectData(operation.objectId, operation.objectType)
                if (localData != operation.data) {
                    Logger.d("ConflictResolver: Conflict detected for ${operation.objectId}")
                    // Apply default strategy
                    when (defaultStrategy) {
                        ConflictResolutionStrategy.KeepRemote -> resolved.add(operation)
                        ConflictResolutionStrategy.KeepLocal -> { /* skip */ }
                        ConflictResolutionStrategy.KeepNewest -> resolved.add(operation)
                        else -> resolved.add(operation) // Default to remote
                    }
                }
            }
        }
        
        return resolved
    }
    
    override suspend fun resolveConflict(
        conflict: SyncConflict,
        strategy: ConflictResolutionStrategy
    ): SyncOperation {
        return when (strategy) {
            ConflictResolutionStrategy.KeepLocal -> SyncOperation(
                objectId = conflict.objectId,
                objectType = conflict.objectType,
                operation = SyncOperationType.Modify,
                data = conflict.localData,
                timestamp = conflict.localModifiedAt
            )
            ConflictResolutionStrategy.KeepRemote -> SyncOperation(
                objectId = conflict.objectId,
                objectType = conflict.objectType,
                operation = SyncOperationType.Modify,
                data = conflict.remoteData,
                timestamp = conflict.remoteModifiedAt
            )
            ConflictResolutionStrategy.KeepNewest -> {
                if (conflict.localModifiedAt >= conflict.remoteModifiedAt) {
                    SyncOperation(
                        objectId = conflict.objectId,
                        objectType = conflict.objectType,
                        operation = SyncOperationType.Modify,
                        data = conflict.localData,
                        timestamp = conflict.localModifiedAt
                    )
                } else {
                    SyncOperation(
                        objectId = conflict.objectId,
                        objectType = conflict.objectType,
                        operation = SyncOperationType.Modify,
                        data = conflict.remoteData,
                        timestamp = conflict.remoteModifiedAt
                    )
                }
            }
            ConflictResolutionStrategy.Merge -> {
                // Merge fields: prefer non-empty values
                val mergedData = mergeFieldByField(conflict)
                SyncOperation(
                    objectId = conflict.objectId,
                    objectType = conflict.objectType,
                    operation = SyncOperationType.Merge,
                    data = mergedData,
                    timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                )
            }
            else -> SyncOperation(
                objectId = conflict.objectId,
                objectType = conflict.objectType,
                operation = SyncOperationType.Modify,
                data = conflict.remoteData,
                timestamp = conflict.remoteModifiedAt
            )
        }
    }
    
    private fun mergeFieldByField(conflict: SyncConflict): String {
        // Simple merge: prefer non-empty values from either side
        val localFields = parseFields(conflict.localData)
        val remoteFields = parseFields(conflict.remoteData)
        val merged = localFields.toMutableMap()
        
        remoteFields.forEach { (key, value) ->
            if (value.isNotEmpty() && (merged[key]?.isEmpty() != false)) {
                merged[key] = value
            }
        }
        
        return buildString {
            merged.forEach { (key, value) ->
                append("$key=$value\n")
            }
        }
    }
    
    private fun parseFields(data: String): Map<String, String> {
        return data.lines()
            .filter { it.contains("=") }
            .associate { line ->
                val parts = line.split("=", limit = 2)
                parts[0] to (parts.getOrElse(1) { "" })
            }
    }
}

// --- ERROR RECOVERY ---

class SyncErrorRecovery(
    private val syncEngine: SyncEngine,
    private val maxRetries: Int = 3
) {
    private val errorHistory = mutableListOf<SyncErrorRecord>()
    
    data class SyncErrorRecord(
        val timestamp: Long = 0L,
        val errorType: ErrorType = ErrorType.Unknown,
        val message: String = "",
        val retryCount: Int = 0,
        val resolved: Boolean = false
    )
    
    enum class ErrorType {
        NetworkInterruption,
        TokenExpiration,
        ServerError,
        RateLimit,
        PartialUpload,
        PartialDownload,
        Conflict,
        CorruptedData,
        InterruptedSync,
        Unknown
    }
    
    suspend fun handleError(error: Exception, operation: String): RecoveryAction {
        val errorType = classifyError(error)
        val record = SyncErrorRecord(
            timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
            errorType = errorType,
            message = error.message ?: "Unknown error",
            retryCount = getRetryCount(errorType)
        )
        errorHistory.add(record)
        
        Logger.w("SyncErrorRecovery: Handling $errorType during $operation")
        
        return when (errorType) {
            ErrorType.NetworkInterruption -> RecoveryAction.Retry(delay = 5000)
            ErrorType.TokenExpiration -> RecoveryAction.RefreshToken
            ErrorType.ServerError -> RecoveryAction.Retry(delay = 10000)
            ErrorType.RateLimit -> RecoveryAction.Retry(delay = 60000)
            ErrorType.PartialUpload -> RecoveryAction.ResumeUpload
            ErrorType.PartialDownload -> RecoveryAction.ResumeDownload
            ErrorType.Conflict -> RecoveryAction.ResolveConflict
            ErrorType.CorruptedData -> RecoveryAction.Skip
            ErrorType.InterruptedSync -> RecoveryAction.Restart
            ErrorType.Unknown -> RecoveryAction.Fail
        }
    }
    
    private fun classifyError(error: Exception): ErrorType {
        val message = error.message ?: ""
        return when {
            message.contains("timeout") || message.contains("connect") -> 
                ErrorType.NetworkInterruption
            message.contains("token") || message.contains("unauthorized") -> 
                ErrorType.TokenExpiration
            message.contains("500") || message.contains("503") -> 
                ErrorType.ServerError
            message.contains("rate") || message.contains("limit") -> 
                ErrorType.RateLimit
            message.contains("corrupt") || message.contains("checksum") -> 
                ErrorType.CorruptedData
            message.contains("conflict") -> ErrorType.Conflict
            message.contains("interrupt") -> ErrorType.InterruptedSync
            else -> ErrorType.Unknown
        }
    }
    
    private fun getRetryCount(errorType: ErrorType): Int {
        return errorHistory.count { it.errorType == errorType && !it.resolved }
    }
    
    fun canRetry(errorType: ErrorType): Boolean {
        return getRetryCount(errorType) < maxRetries
    }
    
    fun getErrorHistory(): List<SyncErrorRecord> = errorHistory.toList()
    
    fun clearHistory() { errorHistory.clear() }
}

sealed interface RecoveryAction {
    data class Retry(val delay: Long = 5000) : RecoveryAction
    object RefreshToken : RecoveryAction
    object ResumeUpload : RecoveryAction
    object ResumeDownload : RecoveryAction
    object ResolveConflict : RecoveryAction
    object Skip : RecoveryAction
    object Restart : RecoveryAction
    object Fail : RecoveryAction
}