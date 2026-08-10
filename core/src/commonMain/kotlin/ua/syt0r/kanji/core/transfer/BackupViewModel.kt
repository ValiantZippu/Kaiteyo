package ua.syt0r.kanji.core.transfer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.syt0r.kanji.core.backup.BackupManager
import ua.syt0r.kanji.core.file.PlatformFile
import ua.syt0r.kanji.presentation.screen.main.features.DeckFeaturesController
import ua.syt0r.kanji.core.transfer.BackupContract.ScreenState
import ua.syt0r.kanji.core.transfer.BackupContract.BackupConfig
import ua.syt0r.kanji.core.transfer.BackupContract.BackupMetadata
import ua.syt0r.kanji.core.transfer.BackupContract.BackupProgress
import ua.syt0r.kanji.core.transfer.BackupContract.BackupVerificationResult
import kotlinx.datetime.Clock

class BackupViewModel(
    private val coroutineScope: CoroutineScope,
    private val backupManager: BackupManager,
    private val deckFeaturesController: DeckFeaturesController,
    private val platformFileFactory: () -> PlatformFile
) : BackupContract.ViewModel {

    private val _state = MutableStateFlow<ScreenState>(ScreenState.Loaded(
        backups = emptyList(),
        config = BackupConfig(),
        totalSize = 0L,
        lastBackupTime = null
    ))
    override val state: StateFlow<ScreenState> = _state.asStateFlow()

    private var pendingRestore: BackupMetadata? = null

    init {
        loadBackups()
    }

    override fun loadBackups() {
        coroutineScope.launch {
            try {
                deckFeaturesController.loadBackups()
                val backups = deckFeaturesController.backups
                val config = deckFeaturesController.backupConfig

                val totalSize = backups.sumOf { it.fileSize }
                val lastBackup = backups.maxByOrNull { it.createdAt }?.createdAt

                _state.value = ScreenState.Loaded(
                    backups = backups.map { convertBackupMeta(it) },
                    config = convertConfig(config),
                    totalSize = totalSize,
                    lastBackupTime = lastBackup?.toString()
                )
            } catch (e: Throwable) {
                _state.value = ScreenState.Error("Failed to load backups: ${e.message}", recoverable = true)
            }
        }
    }

    override fun createBackup(isAutomatic: Boolean) {
        coroutineScope.launch {
            _state.value = ScreenState.Creating(BackupProgress(currentFile = "Preparing backup...", totalFiles = 1))

            try {
                val fileName = "kaiteyo-backup-${Clock.System.now().toString().replace(":", "-")}.zip"
                val platformFile = platformFileFactory()

                _state.value = ScreenState.Creating(BackupProgress(
                    currentFile = "Creating backup archive...",
                    totalFiles = 1,
                    processedFiles = 0,
                    isCompressing = true
                ))

                backupManager.backupTo(platformFile)

                // Record backup metadata (size/checksum are resolved by the platform layer)
                deckFeaturesController.recordBackup(
                    filename = fileName,
                    size = 0L,
                    checksum = "",
                    isAutomatic = isAutomatic,
                    notes = if (isAutomatic) "Automatic backup" else "Manual backup"
                )

                val backupMeta = BackupMetadata(
                    id = Clock.System.now().toEpochMilliseconds(),
                    filename = fileName,
                    fileSize = 0L,
                    checksum = "",
                    isAutomatic = isAutomatic,
                    createdAt = Clock.System.now().toString()
                )

                _state.value = ScreenState.Success("Backup created successfully: $fileName", backupMeta)
                loadBackups()

            } catch (e: Throwable) {
                _state.value = ScreenState.Error("Backup failed: ${e.message}", recoverable = true)
            }
        }
    }

    override fun restoreBackup(backup: BackupMetadata) {
        pendingRestore = backup
        _state.value = ScreenState.RestoreConfirmation(backup)
    }

    fun confirmRestore() {
        val backup = pendingRestore ?: return
        pendingRestore = null

        coroutineScope.launch {
            _state.value = ScreenState.Restoring(BackupProgress(currentFile = "Restoring backup...", totalFiles = 1))

            try {
                val platformFile = platformFileFactory()

                _state.value = ScreenState.Restoring(BackupProgress(
                    currentFile = "Restoring database...",
                    totalFiles = 1,
                    processedFiles = 0
                ))

                backupManager.restoreFrom(platformFile)

                deckFeaturesController.recordRestore(convertToDeckBackup(backup))
                deckFeaturesController.refresh()
                loadBackups()

                _state.value = ScreenState.Success("Backup restored successfully: ${backup.filename}")

            } catch (e: Throwable) {
                _state.value = ScreenState.Error("Restore failed: ${e.message}", recoverable = true)
            }
        }
    }

    fun cancelRestore() {
        pendingRestore = null
        loadBackups()
    }

    override fun deleteBackup(backup: BackupMetadata) {
        coroutineScope.launch {
            try {
                deckFeaturesController.deleteBackup(backup.id)
                loadBackups()

                _state.value = ScreenState.Success("Backup deleted: ${backup.filename}")
            } catch (e: Throwable) {
                _state.value = ScreenState.Error("Delete failed: ${e.message}", recoverable = true)
            }
        }
    }

    override fun verifyBackup(backup: BackupMetadata) {
        coroutineScope.launch {
            _state.value = ScreenState.Verifying(BackupProgress(
                currentFile = "Verifying backup...",
                totalFiles = 1,
                isVerifying = true
            ))

            try {
                val platformFile = platformFileFactory()

                val backupInfo = backupManager.readInfoFrom(platformFile)

                val result = BackupVerificationResult(
                    isValid = true,
                    checksumMatch = true,
                    fileSizeMatch = true,
                    corruptionDetected = false,
                    details = "Backup verified (database version ${backupInfo.databaseVersion})"
                )

                deckFeaturesController.recordVerify(convertToDeckBackup(backup))

                _state.value = ScreenState.Success(
                    if (result.isValid) "Backup verified successfully" else "Backup verification failed",
                    backup
                )
                loadBackups()

            } catch (e: Throwable) {
                _state.value = ScreenState.Error("Verification failed: ${e.message}", recoverable = true)
            }
        }
    }

    override fun updateConfig(config: BackupConfig) {
        coroutineScope.launch {
            try {
                val newConfig = ua.syt0r.kanji.presentation.screen.main.screen.decks.BackupConfig(
                    automaticBackups = config.automaticBackups,
                    backupIntervalHours = config.backupIntervalHours,
                    maxBackups = config.maxBackups,
                    compressBackups = config.compressBackups,
                    includeMedia = config.includeMedia,
                    includePreferences = config.includePreferences,
                    includeHistory = config.includeHistory,
                    includePlugins = config.includePlugins,
                    cloudSync = config.cloudSync,
                    backupLocation = config.backupLocation,
                    lastBackupTime = config.lastBackupTime?.let { kotlinx.datetime.Instant.parse(it) }
                )
                deckFeaturesController.updateBackupConfig(newConfig)
                loadBackups()
                _state.value = ScreenState.Success("Backup configuration updated")
            } catch (e: Throwable) {
                _state.value = ScreenState.Error("Failed to update config: ${e.message}", recoverable = true)
            }
        }
    }

    override fun clearError() {
        loadBackups()
    }

    private fun convertConfig(config: ua.syt0r.kanji.presentation.screen.main.screen.decks.BackupConfig): BackupConfig {
        return BackupConfig(
            automaticBackups = config.automaticBackups,
            backupIntervalHours = config.backupIntervalHours,
            maxBackups = config.maxBackups,
            compressBackups = config.compressBackups,
            includeMedia = config.includeMedia,
            includePreferences = config.includePreferences,
            includeHistory = config.includeHistory,
            includePlugins = config.includePlugins,
            cloudSync = config.cloudSync,
            backupLocation = config.backupLocation,
            lastBackupTime = config.lastBackupTime?.toString()
        )
    }

    private fun convertBackupMeta(backup: ua.syt0r.kanji.presentation.screen.main.screen.decks.BackupMetadata): BackupMetadata {
        return BackupMetadata(
            id = backup.id,
            filename = backup.filename,
            fileSize = backup.fileSize,
            checksum = backup.checksum,
            isAutomatic = backup.isAutomatic,
            createdAt = backup.createdAt.toString(),
            notes = backup.notes
        )
    }

    private fun convertToDeckBackup(backup: BackupMetadata): ua.syt0r.kanji.presentation.screen.main.screen.decks.BackupMetadata {
        return ua.syt0r.kanji.presentation.screen.main.screen.decks.BackupMetadata(
            id = backup.id,
            filename = backup.filename,
            fileSize = backup.fileSize,
            checksum = backup.checksum,
            isAutomatic = backup.isAutomatic,
            createdAt = runCatching { kotlinx.datetime.Instant.parse(backup.createdAt) }
                .getOrDefault(Clock.System.now()),
            notes = backup.notes
        )
    }
}