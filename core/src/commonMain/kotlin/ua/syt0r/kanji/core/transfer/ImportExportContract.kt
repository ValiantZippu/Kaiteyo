package ua.syt0r.kanji.core.transfer

import kotlinx.coroutines.flow.StateFlow
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KaiteyoCard

// ConflictPolicy, ImportPreview, ImportResult and TransferFormat resolve to
// their top-level declarations in this package (single source of truth).

interface ImportExportContract {

    interface ViewModel {
        val state: StateFlow<ScreenState>

        fun loadCards()

        /** Preview a text import (JSON / CSV / TSV / TXT). */
        fun previewImport(text: String, format: TransferFormat)

        /** Preview a binary import (APKG). */
        fun previewImportBytes(bytes: ByteArray, format: TransferFormat)

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