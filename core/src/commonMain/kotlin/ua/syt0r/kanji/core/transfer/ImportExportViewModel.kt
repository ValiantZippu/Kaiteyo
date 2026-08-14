package ua.syt0r.kanji.core.transfer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.syt0r.kanji.core.transfer.ImportExportContract.ScreenState
import ua.syt0r.kanji.core.transfer.ImportExportContract.TransferFormat
import ua.syt0r.kanji.core.transfer.ImportExportContract.ExportFormat
import ua.syt0r.kanji.core.transfer.ImportExportContract.ExportConfig
import ua.syt0r.kanji.core.transfer.ImportPipeline
import ua.syt0r.kanji.core.transfer.ExportPipeline
import ua.syt0r.kanji.core.transfer.ExportBundle
import ua.syt0r.kanji.core.transfer.ImportPreview
import ua.syt0r.kanji.core.transfer.ImportResult
import ua.syt0r.kanji.core.transfer.AnkiPackage
import ua.syt0r.kanji.core.transfer.TransferCard
import ua.syt0r.kanji.presentation.screen.main.features.DeckFeaturesController
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KaiteyoCard
import kotlinx.datetime.Clock

class ImportExportViewModel(
    private val coroutineScope: CoroutineScope,
    private val deckFeaturesController: DeckFeaturesController,
    private val ankiPackage: AnkiPackage
) : ImportExportContract.ViewModel {

    private val _state = MutableStateFlow<ScreenState>(ScreenState.Loading("Loading cards..."))
    override val state: StateFlow<ScreenState> = _state.asStateFlow()

    private var currentPreview: ImportPreview? = null
    private var currentFormat: ImportExportContract.TransferFormat? = null

    init {
        loadCards()
    }

    override fun loadCards() {
        coroutineScope.launch {
            _state.value = ScreenState.Loading("Loading cards...")
            try {
                deckFeaturesController.ensureLoaded()
                val cards = deckFeaturesController.cards
                _state.value = ScreenState.Idle(totalCards = cards.size)
            } catch (e: Throwable) {
                _state.value = ScreenState.Error("Failed to load cards: ${e.message}", recoverable = true)
            }
        }
    }

    override fun previewImport(text: String, format: TransferFormat) {
        if (format == TransferFormat.Apkg) {
            _state.value = ScreenState.Error("Anki packages are binary files — use the file picker to import .apkg", recoverable = true)
            return
        }
        coroutineScope.launch {
            _state.value = ScreenState.Loading("Validating import...")
            val result = when (format) {
                TransferFormat.Json -> ImportPipeline().preview(text, ua.syt0r.kanji.core.transfer.TransferFormat.Json)
                TransferFormat.Csv -> ImportPipeline().preview(text, ua.syt0r.kanji.core.transfer.TransferFormat.Csv)
                TransferFormat.Tsv -> ImportPipeline().preview(text, ua.syt0r.kanji.core.transfer.TransferFormat.Tsv)
                TransferFormat.Txt -> ImportPipeline().preview(text, ua.syt0r.kanji.core.transfer.TransferFormat.Txt)
                TransferFormat.Apkg -> Result.failure(IllegalArgumentException("Use previewImportBytes for APKG"))
            }

            result.onSuccess { preview ->
                currentPreview = preview
                currentFormat = format
                _state.value = ScreenState.Preview(
                    preview = preview,
                    originalText = text,
                    format = format
                )
            }.onFailure { e ->
                _state.value = ScreenState.Error("Import preview failed: ${e.message}", recoverable = true)
            }
        }
    }

    override fun previewImportBytes(bytes: ByteArray, format: TransferFormat) {
        coroutineScope.launch {
            _state.value = ScreenState.Loading("Validating import...")
            val result = when (format) {
                TransferFormat.Apkg -> ankiPackage.read(bytes)
                    .map { cards -> ImportPipeline().previewCards(cards) }
                else -> Result.failure(IllegalArgumentException("Use previewImport for text formats"))
            }

            result.onSuccess { preview ->
                currentPreview = preview
                currentFormat = format
                _state.value = ScreenState.Preview(
                    preview = preview,
                    originalText = "",
                    format = format
                )
            }.onFailure { e ->
                _state.value = ScreenState.Error("Import preview failed: ${e.message}", recoverable = true)
            }
        }
    }

    override fun applyImport(policy: ConflictPolicy) {
        val preview = currentPreview ?: return
        coroutineScope.launch {
            _state.value = ScreenState.Importing(0.5f, "Applying import...")

            // Persist through the real data layer: imported cards merge into
            // the catalog (matching by character) instead of a throwaway
            // in-memory list that would vanish on the next refresh.
            val report = deckFeaturesController.mergeImportedCards(preview.cards, policy)

            _state.value = ScreenState.Success(
                message = "Import complete — merged ${report.merged}, updated ${report.updatedExisting}, skipped ${report.skipped} (${report.skippedReason})",
                result = ImportResult(
                    imported = report.merged,
                    skipped = report.skipped,
                    replaced = 0,
                    createdCopies = 0,
                    issues = emptyList()
                )
            )

            val formatLabel = currentFormat?.name ?: preview.format.name
            deckFeaturesController.recordImport("${preview.cards.size} cards from $formatLabel", report.merged)
            deckFeaturesController.refresh()
            loadCards()
        }
    }

    override fun export(config: ExportConfig): Result<String> = runCatching {
        _state.value = ScreenState.Exporting(0.2f, "Preparing export...")
        if (config.format == ExportFormat.Apkg) {
            // Binary output cannot be represented as a string; the UI must
            // use exportToFile for Anki packages.
            return@runCatching error("Anki packages must be saved to a file — use exportToFile")
        }

        val cards = selectCards(config)
        _state.value = ScreenState.Exporting(0.5f, "Serializing...")

        val transferCards = cards.map { TransferCard.fromKaiteyoCard(it) }
        val bundle = ExportBundle(
            cards = transferCards,
            metadata = mapOf(
                "exportedAt" to Clock.System.now().toString(),
                "cardCount" to cards.size.toString(),
                "format" to config.format.name
            )
        )

        val result = when (config.format) {
            ExportFormat.Json -> ExportPipeline.serialize(bundle, ua.syt0r.kanji.core.transfer.TransferFormat.Json)
            ExportFormat.Csv -> ExportPipeline.serialize(bundle, ua.syt0r.kanji.core.transfer.TransferFormat.Csv)
            ExportFormat.Tsv -> ExportPipeline.serialize(bundle, ua.syt0r.kanji.core.transfer.TransferFormat.Tsv)
            ExportFormat.Txt -> ExportPipeline.serialize(bundle, ua.syt0r.kanji.core.transfer.TransferFormat.Txt)
            ExportFormat.Apkg -> error("Unreachable — APKG is handled by exportToFile")
        }

        _state.value = ScreenState.Exporting(1f, "Export complete")
        coroutineScope.launch { deckFeaturesController.recordExport("exported as ${config.format}", cards.size) }

        result
    }

    override fun exportToFile(config: ExportConfig, fileName: String): Result<ByteArray> = runCatching {
        _state.value = ScreenState.Exporting(0.2f, "Preparing export...")
        val cards = selectCards(config)
        _state.value = ScreenState.Exporting(0.5f, "Serializing...")

        val bytes = when (config.format) {
            ExportFormat.Json -> {
                val bundle = ExportBundle(cards = cards.map { TransferCard.fromKaiteyoCard(it) })
                ExportPipeline.serialize(bundle, ua.syt0r.kanji.core.transfer.TransferFormat.Json).toByteArray(Charsets.UTF_8)
            }
            ExportFormat.Csv -> {
                val bundle = ExportBundle(cards = cards.map { TransferCard.fromKaiteyoCard(it) })
                ExportPipeline.serialize(bundle, ua.syt0r.kanji.core.transfer.TransferFormat.Csv).toByteArray(Charsets.UTF_8)
            }
            ExportFormat.Tsv -> {
                val bundle = ExportBundle(cards = cards.map { TransferCard.fromKaiteyoCard(it) })
                ExportPipeline.serialize(bundle, ua.syt0r.kanji.core.transfer.TransferFormat.Tsv).toByteArray(Charsets.UTF_8)
            }
            ExportFormat.Txt -> {
                val bundle = ExportBundle(cards = cards.map { TransferCard.fromKaiteyoCard(it) })
                ExportPipeline.serialize(bundle, ua.syt0r.kanji.core.transfer.TransferFormat.Txt).toByteArray(Charsets.UTF_8)
            }
            ExportFormat.Apkg -> ankiPackage.write(cards, "Kaiteyo").getOrThrow()
        }

        _state.value = ScreenState.Exporting(1f, "Export complete")
        coroutineScope.launch { deckFeaturesController.recordExport("exported ${config.format}", cards.size) }
        bytes
    }

    override fun dismissPreview() {
        currentPreview = null
        currentFormat = null
        loadCards()
    }

    override fun clearError() {
        loadCards()
    }

    private fun selectCards(config: ExportConfig): List<KaiteyoCard> {
        var cards = deckFeaturesController.cards

        if (config.filteredQuery.isNotBlank()) {
            val query = config.filteredQuery.lowercase()
            cards = cards.filter { card ->
                card.character.lowercase().contains(query) ||
                    card.meaning.lowercase().contains(query) ||
                    card.reading.lowercase().contains(query) ||
                    card.deck.lowercase().contains(query) ||
                    card.tagNames.any { it.lowercase().contains(query) } ||
                    card.notes.lowercase().contains(query)
            }
        }

        if (config.selectedDeckIds.isNotEmpty()) {
            cards = cards.filter { config.selectedDeckIds.contains(it.deckId) }
        }

        if (config.maxCards > 0) {
            cards = cards.take(config.maxCards)
        }

        return cards
    }
}
