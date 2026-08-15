package ua.syt0r.kanji.desktop.ui.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsConfirmDialog
import ua.syt0r.kanji.desktop.designsystem.DsSelect
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsStatTile
import ua.syt0r.kanji.desktop.designsystem.DsTabRow
import ua.syt0r.kanji.desktop.designsystem.DsTextArea
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.transfer.AnkiPackage
import ua.syt0r.kanji.desktop.engine.transfer.ConflictPolicy
import ua.syt0r.kanji.desktop.engine.transfer.ExportBundle
import ua.syt0r.kanji.desktop.engine.transfer.ExportPipeline
import ua.syt0r.kanji.desktop.engine.transfer.ImportPipeline
import ua.syt0r.kanji.desktop.engine.transfer.ImportPreview
import ua.syt0r.kanji.desktop.engine.transfer.ProfileArchive
import ua.syt0r.kanji.desktop.engine.transfer.ProfileData
import ua.syt0r.kanji.desktop.engine.transfer.TransferFilePicker
import ua.syt0r.kanji.desktop.engine.transfer.TransferFormat
import ua.syt0r.kanji.desktop.engine.transfer.capture
import ua.syt0r.kanji.desktop.engine.transfer.restore
import ua.syt0r.kanji.desktop.model.ToastKind

// ============================================
// IMPORT / EXPORT
// Lossless JSON/CSV/TSV/TXT export of cards, stats
// and full profiles; validated import with duplicate
// detection and conflict policies; native profile
// backups; and Anki .apkg interchange.
// ============================================

@Composable
fun TransferView(state: AppState) {
    var tab by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        DsTabRow(tabs = listOf("Export", "Import", "Backup", "Learning data"), selectedIndex = tab, onSelect = { tab = it })
        when (tab) {
            0 -> ExportPanel(state)
            1 -> ImportPanel(state)
            2 -> BackupPanel(state)
            3 -> LearningDataPanel(state)
        }
    }
}

// ============================================
// LEARNING DATA (unified learning store)
// Full-fidelity export / import of notes, cards,
// deck configs, review events, writing attempts,
// exam results and study sessions — via the
// ImportExportEngine. Never destroys data: import
// merges, and the legacy card pool is re-synced
// so every view sees the same content.
// ============================================

private enum class LearningExportFormat { Json, Csv, Tsv }
private enum class LearningImportFormat { Json, Csv, Tsv }

@Composable
private fun LearningDataPanel(state: AppState) {
    val sc = surfaceColors()
    val clipboard = LocalClipboardManager.current
    var exportFormat by remember { mutableStateOf(LearningExportFormat.Json) }
    var exportOutput by remember { mutableStateOf("") }
    var importFormat by remember { mutableStateOf(LearningImportFormat.Json) }
    var importText by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
        DsCard {
            Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                Text("Export learning data", color = sc.textPrimary, fontSize = DsType.Heading, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Full-fidelity export of the unified learning store: notes, cards, per-deck study config, review events, writing attempts, exam results and study sessions. JSON is lossless; CSV/TSV are spreadsheet-friendly.",
                    color = sc.textMuted,
                    fontSize = DsType.Body
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                ) {
                    DsSelect(
                        selected = exportFormat,
                        options = LearningExportFormat.entries.toList(),
                        onSelected = { exportFormat = it },
                        labelOf = { it.name },
                        modifier = Modifier.width(160.dp)
                    )
                    DsButton(
                        text = "Generate",
                        icon = Icons.Default.FileDownload,
                        onClick = {
                            exportOutput = when (exportFormat) {
                                LearningExportFormat.Json -> state.learning.exportSnapshotJson()
                                LearningExportFormat.Csv -> state.learning.exportCsv()
                                LearningExportFormat.Tsv -> state.learning.exportTsv()
                            }
                            state.toastHost.show("Export generated (${exportOutput.length} chars)", kind = ToastKind.Success)
                        }
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${state.learning.notes.size} notes · ${state.learning.cards.size} cards · ${state.learning.reviewEvents.size} reviews · ${state.learning.examResults.size} exams",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
                if (exportOutput.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                        Text("Preview", color = sc.textMuted, fontSize = DsType.Caption, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        DsButton(
                            text = "Save to file…",
                            icon = Icons.Default.Save,
                            compact = true,
                            onClick = {
                                val ext = exportFormat.name.lowercase()
                                val saved = TransferFilePicker.save(
                                    bytes = exportOutput.toByteArray(Charsets.UTF_8),
                                    fileName = "kaiteyo-learning.$ext",
                                    description = "Kaiteyo learning data ($ext)",
                                    ext
                                )
                                if (saved) {
                                    state.toastHost.show("Learning data saved ($exportFormat)", kind = ToastKind.Success)
                                    state.activityLog.record(ActivityCategory.Export, "Exported learning data (${exportFormat.name})")
                                }
                            }
                        )
                        DsButton(
                            text = "Copy to clipboard",
                            icon = Icons.Default.ContentCopy,
                            compact = true,
                            onClick = {
                                clipboard.setText(AnnotatedString(exportOutput))
                                state.toastHost.show("Copied ${exportOutput.length} characters", kind = ToastKind.Success)
                            }
                        )
                    }
                    DsTextArea(value = exportOutput, onValueChange = {}, height = 220.dp, readOnly = true)
                }
            }
        }

        DsCard {
            Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                Text("Import learning data", color = sc.textPrimary, fontSize = DsType.Heading, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Import a Kaiteyo learning-data file. JSON restores full fidelity (including history); CSV/TSV add notes and generate default cards. Imported content merges with your current data — nothing is deleted.",
                    color = sc.textMuted,
                    fontSize = DsType.Body
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                ) {
                    DsSelect(
                        selected = importFormat,
                        options = LearningImportFormat.entries.toList(),
                        onSelected = { importFormat = it },
                        labelOf = { it.name },
                        modifier = Modifier.width(160.dp)
                    )
                    DsButton(
                        text = "Load from file…",
                        icon = Icons.Default.FolderOpen,
                        onClick = {
                            val content = TransferFilePicker.open("Kaiteyo learning data", "json", "csv", "tsv")
                                ?.toString(Charsets.UTF_8)
                            if (content != null) importText = content
                        }
                    )
                    Spacer(Modifier.weight(1f))
                    DsButton(
                        text = "Import",
                        icon = Icons.Default.FileUpload,
                        enabled = importText.isNotBlank(),
                        onClick = {
                            applyLearningImport(state, importText, importFormat)
                            importText = ""
                        }
                    )
                }
                DsTextArea(value = importText, onValueChange = { importText = it }, height = 220.dp, readOnly = false)
            }
        }
    }
}

private fun applyLearningImport(state: AppState, text: String, format: LearningImportFormat) {
    val result = when (format) {
        LearningImportFormat.Json -> state.learning.importJson(text)
        LearningImportFormat.Csv -> state.learning.importCsv(text)
        LearningImportFormat.Tsv -> state.learning.importTsv(text)
    }
    if (result.errors.isNotEmpty()) {
        state.toastHost.show("Import failed: ${result.errors.first()}", kind = ToastKind.Error)
        return
    }
    // CSV/TSV imports only carry notes — materialize their default cards.
    if (format != LearningImportFormat.Json) state.learning.ensureCards()
    // Re-sync the legacy card pool so every view sees the imported content.
    val legacy = state.learning.allLegacyCards()
    state.cards.clear()
    state.cards.addAll(legacy)
    state.library.saveCards(state.cards.toList())
    state.activityLog.record(
        ActivityCategory.Import,
        "Imported learning data ($format): ${result.notesAdded} notes, ${result.notesUpdated} updated, ${result.cardsAdded} cards, ${result.eventsImported} events"
    )
    state.toastHost.show(
        "Import complete — ${result.notesAdded} notes added, ${result.notesUpdated} updated, ${result.cardsAdded} cards, ${result.eventsImported} events",
        kind = ToastKind.Success
    )
}

// ============================================
// EXPORT
// ============================================

@Composable
private fun ExportPanel(state: AppState) {
    val sc = surfaceColors()
    val clipboard = LocalClipboardManager.current
    var format by remember { mutableStateOf(TransferFormat.Json) }
    var includeStats by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
        DsCard {
            Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                Text("Export your data", color = sc.textPrimary, fontSize = DsType.Heading, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Export ${state.cards.size} cards. Choose a format — JSON is lossless, CSV/TSV are spreadsheet-friendly, TXT is human-readable.",
                    color = sc.textMuted,
                    fontSize = DsType.Body
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                ) {
                    DsSelect(
                        selected = format,
                        options = TransferFormat.entries.toList(),
                        onSelected = { format = it },
                        labelOf = { it.name },
                        modifier = Modifier.width(160.dp)
                    )
                    Switch(
                        checked = includeStats,
                        onCheckedChange = { includeStats = it }
                    )
                    Text("Include review log & stats", color = sc.textSecondary, fontSize = DsType.Body)
                    Spacer(Modifier.weight(1f))
                    DsButton(
                        text = "Generate",
                        icon = Icons.Default.FileDownload,
                        onClick = {
                            output = if (includeStats) {
                                ExportPipeline.exportProfile(
                                    cards = state.cards.toList(),
                                    reviewLog = state.reviewLog.toList(),
                                    summaries = state.summaries.toList()
                                )
                            } else {
                                ExportPipeline.serialize(ExportBundle(cards = state.cards.toList()), format)
                            }
                            state.toastHost.show("Export generated (${output.length} chars)", kind = ToastKind.Success)
                        }
                    )
                }
            }
        }

        if (output.isNotEmpty()) {
            DsCard {
                Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                        Text("Preview", color = sc.textMuted, fontSize = DsType.Caption, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        DsButton(
                            text = "Save to file…",
                            icon = Icons.Default.Save,
                            onClick = {
                                val saved = TransferFilePicker.save(
                                    bytes = output.toByteArray(Charsets.UTF_8),
                                    fileName = "kaiteyo-cards.${format.extension}",
                                    description = "Kaiteyo ${format.name} export",
                                    format.extension
                                )
                                if (saved) {
                                    state.toastHost.show("Saved ${state.cards.size} cards to file ($format)", kind = ToastKind.Success)
                                    state.activityLog.record(ActivityCategory.Export, "Exported ${state.cards.size} cards to file ($format)")
                                }
                            },
                            compact = true
                        )
                        DsButton(
                            text = "Copy to clipboard",
                            icon = Icons.Default.ContentCopy,
                            onClick = {
                                clipboard.setText(AnnotatedString(output))
                                state.toastHost.show("Copied ${output.length} characters", kind = ToastKind.Success)
                                state.activityLog.record(ActivityCategory.Export, "Exported ${state.cards.size} cards ($format)")
                            },
                            compact = true
                        )
                    }
                    DsTextArea(value = output, onValueChange = {}, height = 360.dp, readOnly = true)
                }
            }
        }
    }
}

// ============================================
// IMPORT
// ============================================

@Composable
private fun ImportPanel(state: AppState) {
    val sc = surfaceColors()
    var format by remember { mutableStateOf(TransferFormat.Json) }
    var text by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<ImportPreview?>(null) }
    var policy by remember { mutableStateOf(ConflictPolicy.KeepExisting) }
    val pipeline = remember { ImportPipeline() }

    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
        DsCard {
            Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                Text("Import cards", color = sc.textPrimary, fontSize = DsType.Heading, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                ) {
                    DsSelect(
                        selected = format,
                        options = TransferFormat.entries.toList(),
                        onSelected = { format = it; preview = null },
                        labelOf = { it.name },
                        modifier = Modifier.width(160.dp)
                    )
                    DsButton(
                        text = "Load from file…",
                        icon = Icons.Default.FolderOpen,
                        onClick = {
                            val content = TransferFilePicker.open("Kaiteyo import (JSON / CSV / TSV / TXT)", "json", "csv", "tsv", "txt")
                                ?.toString(Charsets.UTF_8)
                            if (content == null) return@DsButton
                            text = content
                            pipeline.preview(content, format)
                                .onSuccess { preview = it }
                                .onFailure { e ->
                                    state.toastHost.show("Import failed: ${e.message}", kind = ToastKind.Error)
                                    preview = null
                                }
                        }
                    )
                    DsButton(
                        text = "Validate & preview",
                        icon = Icons.Default.FileUpload,
                        onClick = {
                            pipeline.preview(text, format)
                                .onSuccess { preview = it }
                                .onFailure { e ->
                                    state.toastHost.show("Import failed: ${e.message}", kind = ToastKind.Error)
                                    preview = null
                                }
                        }
                    )
                }
                DsTextArea(value = text, onValueChange = { text = it }, height = 240.dp, readOnly = false)
            }
        }

        preview?.let { p ->
            PreviewSection(
                state = state,
                preview = p,
                policy = policy,
                onPolicyChange = { policy = it },
                formatLabel = format.name,
                onImport = {
                    applyImport(state, p, policy, format.name)
                    preview = null
                    text = ""
                }
            )
        }
    }
}

// ============================================
// BACKUP (native profile + Anki interchange)
// ============================================

@Composable
private fun BackupPanel(state: AppState) {
    val sc = surfaceColors()
    var confirmRestore by remember { mutableStateOf(false) }
    var pendingProfile by remember { mutableStateOf<ProfileData?>(null) }
    var ankiPreview by remember { mutableStateOf<ImportPreview?>(null) }
    var ankiPolicy by remember { mutableStateOf(ConflictPolicy.KeepExisting) }
    val ankiPipeline = remember { ImportPipeline() }

    val current = state.capture()

    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            DsStatTile(label = "Cards", value = current.cards.size.toString(), modifier = Modifier.weight(1f))
            DsStatTile(label = "Reviews", value = current.reviewLog.size.toString(), modifier = Modifier.weight(1f))
            DsStatTile(label = "Study days", value = current.studyDays.toString(), modifier = Modifier.weight(1f))
            DsStatTile(label = "Collections", value = current.collectionCount.toString(), modifier = Modifier.weight(1f))
        }

        DsCard {
            Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                Text("Native backup", color = sc.textPrimary, fontSize = DsType.Heading, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "A fully lossless snapshot of everything: cards, review history, daily statistics, collections, saved filters, settings, active theme and the activity ledger. Saving writes a .kaiteyozip archive; importing replaces current data.",
                    color = sc.textMuted,
                    fontSize = DsType.Body
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                ) {
                    DsButton(
                        text = "Export profile…",
                        icon = Icons.Default.FileDownload,
                        onClick = {
                            val data = state.capture()
                            val bytes = ProfileArchive.toZip(data)
                            val saved = TransferFilePicker.save(
                                bytes = bytes,
                                fileName = ProfileArchive.timestampedName(),
                                description = "Kaiteyo profile backup",
                                ProfileArchive.DEFAULT_EXTENSION
                            )
                            if (saved) {
                                state.toastHost.show("Profile backed up — ${data.cards.size} cards", kind = ToastKind.Success)
                                state.activityLog.record(ActivityCategory.Export, "Exported full profile backup (${data.cards.size} cards)")
                            }
                        }
                    )
                    Spacer(Modifier.weight(1f))
                    DsButton(
                        text = "Import profile…",
                        icon = Icons.Default.FileUpload,
                        onClick = {
                            val bytes = TransferFilePicker.open("Kaiteyo profile backup", ProfileArchive.DEFAULT_EXTENSION)
                                ?: return@DsButton
                            ProfileArchive.fromZip(bytes)
                                .onSuccess { data ->
                                    pendingProfile = data
                                    confirmRestore = true
                                }
                                .onFailure { e ->
                                    state.toastHost.show("Invalid backup file: ${e.message}", kind = ToastKind.Error)
                                }
                        }
                    )
                }
            }
        }

        DsCard {
            Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                Text("Anki interchange", color = sc.textPrimary, fontSize = DsType.Heading, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Exchange cards with Anki. Export writes a standard .apkg package (front = character, back = meaning); import reads .apkg files and previews them like any other import.",
                    color = sc.textMuted,
                    fontSize = DsType.Body
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                ) {
                    DsButton(
                        text = "Export as .apkg…",
                        icon = Icons.Default.FileDownload,
                        onClick = {
                            AnkiPackage.write(state.cards.toList())
                                .onSuccess { bytes ->
                                    val saved = TransferFilePicker.save(
                                        bytes = bytes,
                                        fileName = "kaiteyo-deck.${AnkiPackage.EXTENSION}",
                                        description = "Anki package",
                                        AnkiPackage.EXTENSION
                                    )
                                    if (saved) {
                                        state.toastHost.show("Exported ${state.cards.size} cards as Anki package", kind = ToastKind.Success)
                                        state.activityLog.record(ActivityCategory.Export, "Exported ${state.cards.size} cards as .apkg")
                                    }
                                }
                                .onFailure { e ->
                                    state.toastHost.show("APKG export failed: ${e.message}", kind = ToastKind.Error)
                                }
                        }
                    )
                    Spacer(Modifier.weight(1f))
                    DsButton(
                        text = "Import .apkg…",
                        icon = Icons.Default.FolderOpen,
                        onClick = {
                            val bytes = TransferFilePicker.open("Anki package", AnkiPackage.EXTENSION)
                                ?: return@DsButton
                            AnkiPackage.read(bytes)
                                .onSuccess { cards ->
                                    ankiPreview = ankiPipeline.previewCards(cards)
                                }
                                .onFailure { e ->
                                    state.toastHost.show("APKG import failed: ${e.message}", kind = ToastKind.Error)
                                }
                        }
                    )
                }
            }
        }

        ankiPreview?.let { p ->
            PreviewSection(
                state = state,
                preview = p,
                policy = ankiPolicy,
                onPolicyChange = { ankiPolicy = it },
                formatLabel = "Anki",
                onImport = {
                    applyImport(state, p, ankiPolicy, "APKG")
                    ankiPreview = null
                }
            )
        }
    }

    if (confirmRestore && pendingProfile != null) {
        val data = pendingProfile!!
        DsConfirmDialog(
            title = "Restore profile?",
            message = "This replaces ALL current data with the backup — ${data.cards.size} cards, ${data.reviewLog.size} review entries, ${data.studyDays} study days and ${data.collectionCount} collections. This cannot be undone.",
            confirmText = "Restore",
            danger = true,
            onConfirm = {
                state.restore(data)
                confirmRestore = false
                pendingProfile = null
            },
            onDismiss = {
                confirmRestore = false
                pendingProfile = null
            }
        )
    }
}

// ============================================
// Shared import-preview section
// ============================================

@Composable
private fun PreviewSection(
    state: AppState,
    preview: ImportPreview,
    policy: ConflictPolicy,
    onPolicyChange: (ConflictPolicy) -> Unit,
    formatLabel: String,
    onImport: () -> Unit
) {
    val sc = surfaceColors()
    DsCard {
        Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            Text("Import preview", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsBadge(text = "${preview.total} total", tint = Color(0xFF7BC8FF))
                DsBadge(text = "${preview.valid} valid", tint = Color(0xFFC2FC8B))
                DsBadge(text = "${preview.invalid} invalid", tint = Color(0xFFFF6B6B))
                DsBadge(text = "${preview.duplicates} duplicates", tint = Color(0xFFFEAB57))
            }
            if (preview.issues.isNotEmpty()) {
                Column {
                    preview.issues.take(5).forEach { issue ->
                        Text(
                            text = "[${issue.severity}] ${issue.message}",
                            color = if (issue.severity.name == "Error") Color(0xFFFF6B6B) else sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                Text("Conflict policy", color = sc.textSecondary, fontSize = DsType.Body)
                DsSelect(
                    selected = policy,
                    options = ConflictPolicy.entries.toList(),
                    onSelected = onPolicyChange,
                    labelOf = { it.name },
                    modifier = Modifier.width(180.dp)
                )
                Spacer(Modifier.weight(1f))
                DsButton(
                    text = "Import ${preview.valid} cards ($formatLabel)",
                    onClick = onImport
                )
            }
        }
    }
}

// ============================================
// Helpers
// ============================================

private val TransferFormat.extension: String
    get() = when (this) {
        TransferFormat.Json -> "json"
        TransferFormat.Csv -> "csv"
        TransferFormat.Tsv -> "tsv"
        TransferFormat.Txt -> "txt"
    }

private fun applyImport(state: AppState, preview: ImportPreview, policy: ConflictPolicy, label: String) {
    val result = ImportPipeline().apply(state.cards.toList(), preview.cards, policy)
    state.cards.clear()
    state.cards.addAll(result.combined)
    state.library.saveCards(state.cards.toList())
    val added = result.imported + result.createdCopies
    state.activityLog.record(ActivityCategory.Import, "Imported $added cards ($label)", affectedCount = added)
    state.toastHost.show(
        "Import complete — imported $added, replaced ${result.replaced}, skipped ${result.skipped} (${state.cards.size} total)",
        kind = ToastKind.Success
    )
}
