package ua.syt0r.kanji.core.transfer

import kotlinx.coroutines.flow.StateFlow
import ua.syt0r.kanji.presentation.screen.main.screen.decks.BackupConfig
import ua.syt0r.kanji.presentation.screen.main.screen.decks.BackupMetadata
import ua.syt0r.kanji.presentation.screen.main.screen.decks.BackupProgress
import ua.syt0r.kanji.presentation.screen.main.screen.decks.BackupVerificationResult

interface BackupContract {

    interface ViewModel {
        val state: StateFlow<ScreenState>

        fun loadBackups()
        fun createBackup(isAutomatic: Boolean)
        fun restoreBackup(backup: BackupMetadata)
        fun deleteBackup(backup: BackupMetadata)
        fun verifyBackup(backup: BackupMetadata)
        fun updateConfig(config: BackupConfig)
        fun clearError()
    }

    sealed interface ScreenState {
        data class Loaded(
            val backups: List<BackupMetadata>,
            val config: BackupConfig,
            val totalSize: Long,
            val lastBackupTime: String?
        ) : ScreenState

        data class Creating(
            val progress: BackupProgress
        ) : ScreenState

        data class Restoring(
            val progress: BackupProgress
        ) : ScreenState

        data class Verifying(
            val progress: BackupProgress
        ) : ScreenState

        data class Success(
            val message: String,
            val backup: BackupMetadata? = null
        ) : ScreenState

        data class Error(
            val message: String,
            val recoverable: Boolean = true
        ) : ScreenState

        data class RestoreConfirmation(
            val backup: BackupMetadata
        ) : ScreenState
    }

    data class BackupConfig(
        val automaticBackups: Boolean = true,
        val backupIntervalHours: Int = 24,
        val maxBackups: Int = 30,
        val compressBackups: Boolean = true,
        val includeMedia: Boolean = false,
        val includePreferences: Boolean = true,
        val includeHistory: Boolean = true,
        val includePlugins: Boolean = false,
        val cloudSync: Boolean = false,
        val backupLocation: String = "",
        val lastBackupTime: String? = null
    )

    data class BackupMetadata(
        val id: Long,
        val filename: String,
        val fileSize: Long,
        val checksum: String,
        val isAutomatic: Boolean,
        val createdAt: String,
        val notes: String = ""
    )

    data class BackupProgress(
        val currentFile: String = "",
        val totalFiles: Int = 0,
        val processedFiles: Int = 0,
        val bytesProcessed: Long = 0L,
        val totalBytes: Long = 0L,
        val isCompressing: Boolean = false,
        val isVerifying: Boolean = false,
        val isUploading: Boolean = false
    )

    data class BackupVerificationResult(
        val isValid: Boolean = false,
        val checksumMatch: Boolean = false,
        val fileSizeMatch: Boolean = false,
        val corruptionDetected: Boolean = false,
        val details: String = ""
    )
}