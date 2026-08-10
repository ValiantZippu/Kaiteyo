package ua.syt0r.kanji.core.transfer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.syt0r.kanji.core.transfer.ImportExportContract.ScreenState
import ua.syt0r.kanji.core.transfer.ImportExportContract.TransferFormat
import ua.syt0r.kanji.core.transfer.ImportExportContract.ExportFormat
import ua.syt0r.kanji.core.transfer.ImportExportContract.ExportConfig
import ua.syt0r.kanji.core.transfer.ImportExportContract.ConflictPolicy
import ua.syt0r.kanji.core.transfer.ImportPipeline
import ua.syt0r.kanji.core.transfer.ExportPipeline
import ua.syt0r.kanji.core.transfer.ExportBundle
import ua.syt0r.kanji.core.transfer.ImportPreview
import ua.syt0r.kanji.core.transfer.ImportResult
import ua.syt0r.kanji.core.transfer.TransferCodecs
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
    private var currentPreviewText: String = ""
    private var currentPreviewFormat: TransferFormat = TransferFormat.Json

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
        coroutineScope.launch {
            _state.value = ScreenState.Loading("Validating import...")
            currentPreviewText = text
            currentPreviewFormat = format

            val result = when (format) {
                TransferFormat.Json -> ImportPipeline().preview(text, ua.syt0r.kanji.core.transfer.TransferFormat.Json)
                TransferFormat.Csv -> ImportPipeline().preview(text, ua.syt0r.kanji.core.transfer.TransferFormat.Csv)
                TransferFormat.Tsv -> ImportPipeline().preview(text, ua.syt0r.kanji.core.transfer.TransferFormat.Tsv)
                TransferFormat.Txt -> ImportPipeline().preview(text, ua.syt0r.kanji.core.transfer.TransferFormat.Txt)
                TransferFormat.Apkg -> {
                    // For APKG, we need to read bytes first
                    ankiPackage.read(text.toByteArray())
                        .map { cards -> ImportPipeline().previewCards(cards) }
                }
            }

            result.onSuccess { preview ->
                currentPreview = preview
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

    override fun applyImport(policy: ConflictPolicy) {
        val preview = currentPreview ?: return
        coroutineScope.launch {
            _state.value = ScreenState.Importing(0.5f, "Applying import...")

            val existingCards = deckFeaturesController.cards
            val result = ImportPipeline().apply(existingCards, preview.cards, mapConflictPolicy(policy))

            _state.value = ScreenState.Success(
                message = "Import complete — imported ${result.imported}, replaced ${result.replaced}, skipped ${result.skipped} (${result.combined.size} total)",
                result = result
            )

            // Record in history
            deckFeaturesController.recordImport("${preview.cards.size} cards from ${preview.format}", result.imported + result.createdCopies)

            // Refresh cards
            deckFeaturesController.refresh()
            loadCards()
        }
    }

    override fun export(config: ExportConfig): Result<String> = runCatching {
        _state.value = ScreenState.Exporting(0.2f, "Preparing export...")

        var cards = deckFeaturesController.cards

        // Apply filters
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
            ExportFormat.Apkg -> {
                val bytes = ankiPackage.write(cards).getOrThrow()
                bytes.toString(charset("UTF-8"))
            }
        }

        _state.value = ScreenState.Exporting(1f, "Export complete")
        coroutineScope.launch { deckFeaturesController.recordExport("exported as ${config.format}", cards.size) }

        result
    }

    override fun exportToFile(config: ExportConfig, fileName: String): Result<ByteArray> = runCatching {
        _state.value = ScreenState.Exporting(0.2f, "Preparing export...")

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

        _state.value = ScreenState.Exporting(0.5f, "Serializing...")

        return@runCatching when (config.format) {
            ExportFormat.Json -> {
                val transferCards = cards.map { TransferCard.fromKaiteyoCard(it) }
                val bundle = ExportBundle(cards = transferCards)
                ExportPipeline.serialize(bundle, ua.syt0r.kanji.core.transfer.TransferFormat.Json).toByteArray(charset("UTF-8"))
            }
            ExportFormat.Csv -> {
                val transferCards = cards.map { TransferCard.fromKaiteyoCard(it) }
                val bundle = ExportBundle(cards = transferCards)
                ExportPipeline.serialize(bundle, ua.syt0r.kanji.core.transfer.TransferFormat.Csv).toByteArray(charset("UTF-8"))
            }
            ExportFormat.Tsv -> {
                val transferCards = cards.map { TransferCard.fromKaiteyoCard(it) }
                val bundle = ExportBundle(cards = transferCards)
                ExportPipeline.serialize(bundle, ua.syt0r.kanji.core.transfer.TransferFormat.Tsv).toByteArray(charset("UTF-8"))
            }
            ExportFormat.Txt -> {
                val transferCards = cards.map { TransferCard.fromKaiteyoCard(it) }
                val bundle = ExportBundle(cards = transferCards)
                ExportPipeline.serialize(bundle, ua.syt0r.kanji.core.transfer.TransferFormat.Txt).toByteArray(charset("UTF-8"))
            }
            ExportFormat.Apkg -> {
                ankiPackage.write(cards, "Kaiteyo").getOrThrow()
            }
        }
    }

    override fun dismissPreview() {
        currentPreview = null
        currentPreviewText = ""
        loadCards()
    }

    override fun clearError() {
        loadCards()
    }

    private fun mapConflictPolicy(policy: ConflictPolicy): ua.syt0r.kanji.core.transfer.ConflictPolicy = when (policy) {
        ConflictPolicy.KeepExisting -> ua.syt0r.kanji.core.transfer.ConflictPolicy.KeepExisting
        ConflictPolicy.OverwriteExisting -> ua.syt0r.kanji.core.transfer.ConflictPolicy.OverwriteExisting
        ConflictPolicy.Skip -> ua.syt0r.kanji.core.transfer.ConflictPolicy.Skip
        ConflictPolicy.KeepNewest -> ua.syt0r.kanji.core.transfer.ConflictPolicy.KeepNewest
    }
}