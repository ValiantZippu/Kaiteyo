package ua.syt0r.kanji.core.transfer

import kotlinx.coroutines.flow.StateFlow
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KaiteyoCard
import ua.syt0r.kanji.core.transfer.TransferFormat
import ua.syt0r.kanji.core.transfer.ImportPreview
import ua.syt0r.kanji.core.transfer.ImportResult
import ua.syt0r.kanji.core.transfer.ConflictPolicy

// Forward declarations for types defined below
typealias ExportFormat = ImportExportContract.ExportFormat
typealias ExportConfig = ImportExportContract.ExportConfig

interface ImportExportContract {

    interface ViewModel {
        val state: StateFlow<ScreenState>

        fun loadCards()
        fun previewImport(text: String, format: TransferFormat)
        fun applyImport(policy: ConflictPolicy)
        fun export(config: ExportConfig): Result<String>
        fun exportToFile(config: ExportConfig, fileName: String): Result<ByteArray>
        fun dismissPreview()
        fun clearError()
    }

    sealed interface ScreenState {
        data class Idle(
            val totalCards: Int = 0
        ) : ScreenState

        data class Loading(
            val message: String = "Loading..."
        ) : ScreenState

        data class Preview(
            val preview: ImportPreview,
            val originalText: String,
            val format: TransferFormat
        ) : ScreenState

        data class Exporting(
            val progress: Float,
            val message: String
        ) : ScreenState

        data class Importing(
            val progress: Float,
            val message: String
        ) : ScreenState

        data class Success(
            val message: String,
            val result: ImportResult? = null
        ) : ScreenState

        data class Error(
            val message: String,
            val recoverable: Boolean = true
        ) : ScreenState
    }

    enum class TransferFormat { Json, Csv, Tsv, Txt, Apkg }

    enum class ExportFormat { Json, Csv, Tsv, Txt, Apkg }

    enum class ConflictPolicy { KeepExisting, OverwriteExisting, Skip, KeepNewest }

    data class ExportConfig(
        val format: ExportFormat = ExportFormat.Csv,
        val includeTags: Boolean = true,
        val includeFlags: Boolean = true,
        val includeNotes: Boolean = true,
        val includeHistory: Boolean = false,
        val includeStatistics: Boolean = false,
        val filteredQuery: String = "",
        val selectedDeckIds: List<Long> = emptyList(),
        val maxCards: Int = 0
    )
}