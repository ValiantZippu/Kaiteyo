package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.transfer.ConflictPolicy
import ua.syt0r.kanji.core.transfer.ImportExportContract
import ua.syt0r.kanji.core.transfer.getLastImportFileName
import ua.syt0r.kanji.core.transfer.pickImportFile
import ua.syt0r.kanji.core.transfer.readLastImportFile
import ua.syt0r.kanji.core.transfer.saveExportFile
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoSemanticColors
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.getMultiplatformViewModel

// ============================================
// KAITEYO IMPORT/EXPORT SCREEN
// Wired to the real ImportExportContract
// pipeline: file picking, paste import, preview,
// conflict resolution and export to file or
// clipboard. Imports merge into the kanji
// catalog (scheduling, tags, flags, notes).
// ============================================

private val EXPORT_FORMATS = listOf(
    ImportExportContract.ExportFormat.Json,
    ImportExportContract.ExportFormat.Csv,
    ImportExportContract.ExportFormat.Tsv,
    ImportExportContract.ExportFormat.Txt,
    ImportExportContract.ExportFormat.Apkg
)

@Composable
fun ImportExportScreen() {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val scope = rememberCoroutineScope()
    val viewModel = getMultiplatformViewModel<ImportExportContract.ViewModel>(scope)
    val state by viewModel.state.collectAsState()

    var selectedTab by remember { mutableStateOf("Import") }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Import / Export",
            style = MaterialTheme.typography.titleLarge,
            color = surfaceColors.textPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Transfer study data between Kaiteyo and other applications. Imports merge scheduling, tags, flags and notes onto catalog cards.",
            color = surfaceColors.textMuted,
            fontSize = 13.sp
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Import", "Export").forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) accent.primary.copy(alpha = 0.15f) else surfaceColors.surface)
                        .clickable { selectedTab = tab }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        tab,
                        color = if (isSelected) accent.primary else surfaceColors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        val currentState = state
        when (currentState) {
            is ImportExportContract.ScreenState.Loading -> StatusLine(currentState)
            is ImportExportContract.ScreenState.Error -> ErrorLine(currentState, onDismiss = { viewModel.clearError() })
            is ImportExportContract.ScreenState.Success -> SuccessLine(currentState, onDismiss = { viewModel.loadCards() })
            else -> Unit
        }

        when (selectedTab) {
            "Import" -> ImportTab(viewModel, state)
            else -> ExportTab(viewModel, state)
        }
    }
}

// ============================================
// IMPORT
// ============================================

@Composable
private fun ImportTab(
    viewModel: ImportExportContract.ViewModel,
    state: ImportExportContract.ScreenState
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val scope = rememberCoroutineScope()

    var format by remember { mutableStateOf(ImportExportContract.TransferFormat.Json) }
    var text by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var lastImportName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { lastImportName = getLastImportFileName() }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Import Cards", style = MaterialTheme.typography.titleMedium, color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ImportExportContract.TransferFormat.entries.forEach { candidate ->
                val selected = candidate == format
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (selected) accent.primary.copy(alpha = 0.15f) else surfaceColors.surface)
                        .border(1.dp, if (selected) accent.primary else surfaceColors.border.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .clickable { format = candidate }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        candidate.name,
                        color = if (selected) accent.primary else surfaceColors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        val bytes = pickImportFile("Import file", extensionFor(format))
                        when {
                            bytes == null -> notice = "No file selected (or the file could not be read)."
                            format == ImportExportContract.TransferFormat.Apkg -> {
                                viewModel.previewImportBytes(bytes, format)
                                notice = ""
                            }
                            else -> {
                                val content = bytes.toString(Charsets.UTF_8)
                                text = content
                                viewModel.previewImport(content, format)
                                notice = ""
                            }
                        }
                        if (bytes != null) lastImportName = getLastImportFileName()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accent.primary, contentColor = accent.onPrimary)
            ) { Text("Load from file…", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }

            if (format != ImportExportContract.TransferFormat.Apkg) {
                Button(
                    onClick = {
                        viewModel.previewImport(text, format)
                        notice = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent.primary.copy(alpha = 0.15f), contentColor = accent.primary)
                ) { Text("Validate pasted text", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            }
        }

        // Offered on platforms that remember the last picked file (Android SAF
        // persistable grants); reads it back without opening the picker.
        lastImportName?.let { name ->
            Button(
                onClick = {
                    scope.launch {
                        val bytes = readLastImportFile()
                        when {
                            bytes == null -> {
                                notice = "Could not re-read the last file (it may have moved, or the permission expired)."
                                lastImportName = null
                            }
                            format == ImportExportContract.TransferFormat.Apkg -> {
                                viewModel.previewImportBytes(bytes, format)
                                notice = ""
                            }
                            else -> {
                                val content = bytes.toString(Charsets.UTF_8)
                                text = content
                                viewModel.previewImport(content, format)
                                notice = ""
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accent.primary.copy(alpha = 0.15f), contentColor = accent.primary),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Re-import $name", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
        }

        if (format != ImportExportContract.TransferFormat.Apkg) {
            Text("Paste ${format.name} data", color = surfaceColors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            PasteField(
                value = text,
                onValueChange = { text = it },
                placeholder = if (format == ImportExportContract.TransferFormat.Json) "[{ ... }] or { ... }" else "character, meaning, reading…"
            )
        } else {
            Text(
                "APKG import reads an Anki package and maps decks, cards, tags, scheduling and media into Kaiteyo.",
                color = surfaceColors.textMuted,
                fontSize = 12.sp
            )
        }

        if (notice.isNotBlank()) {
            Text(notice, color = accent.primary, fontSize = 12.sp)
        }

        val previewState = state as? ImportExportContract.ScreenState.Preview
        if (previewState != null) {
            val preview = previewState.preview
            var policy by remember { mutableStateOf(ConflictPolicy.KeepExisting) }

            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(surfaceColors.surfaceElevated.copy(alpha = 0.5f))
                    .border(1.dp, surfaceColors.border.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Import preview", color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PreviewStat("${preview.total} total", surfaceColors.textSecondary)
                    PreviewStat("${preview.valid} valid", accent.primary)
                    val sem = LocalKaiteyoSemanticColors.current
                    PreviewStat("${preview.invalid} invalid", sem.error)
                    PreviewStat("${preview.duplicates} duplicates", sem.warning)
                }
                if (preview.issues.isNotEmpty()) {
                    preview.issues.take(6).forEach { issue ->
                        Text(
                            "[${issue.severity}] ${issue.message}",
                            color = if (issue.severity.name == "Error") LocalKaiteyoSemanticColors.current.error else surfaceColors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }
                Text("Conflict policy", color = surfaceColors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ConflictPolicy.entries.forEach { candidate ->
                        val selected = candidate == policy
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(if (selected) accent.primary.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { policy = candidate }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                candidate.name,
                                color = if (selected) accent.primary else surfaceColors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Button(
                    onClick = { viewModel.applyImport(policy) },
                    colors = ButtonDefaults.buttonColors(containerColor = accent.primary, contentColor = accent.onPrimary),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) { Text("Import ${preview.valid} cards", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
            }
        }
    }
}

// ============================================
// EXPORT
// ============================================

@Composable
private fun ExportTab(
    viewModel: ImportExportContract.ViewModel,
    state: ImportExportContract.ScreenState
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val clipboard = LocalClipboardManager.current

    var format by remember { mutableStateOf(ImportExportContract.ExportFormat.Json) }
    var includeTags by remember { mutableStateOf(true) }
    var includeFlags by remember { mutableStateOf(true) }
    var includeNotes by remember { mutableStateOf(true) }
    var output by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val idleCards = (state as? ImportExportContract.ScreenState.Idle)?.totalCards

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Export Cards", style = MaterialTheme.typography.titleMedium, color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
        Text(
            "Exporting ${idleCards ?: "…"} catalog cards" + if (format == ImportExportContract.ExportFormat.Apkg) " as an Anki package" else ".",
            color = surfaceColors.textMuted,
            fontSize = 13.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EXPORT_FORMATS.forEach { candidate ->
                val selected = candidate == format
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (selected) accent.primary.copy(alpha = 0.15f) else surfaceColors.surface)
                        .border(1.dp, if (selected) accent.primary else surfaceColors.border.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .clickable { format = candidate; output = "" }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        candidate.name,
                        color = if (selected) accent.primary else surfaceColors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        if (format != ImportExportContract.ExportFormat.Apkg) {
            Text("Include:", color = surfaceColors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IncludeToggle("Tags", includeTags) { includeTags = it }
                IncludeToggle("Flags", includeFlags) { includeFlags = it }
                IncludeToggle("Notes", includeNotes) { includeNotes = it }
            }
        } else {
            Text(
                "APKG export writes an Anki package with decks, fields, tags and scheduling state. Review history is not represented in Anki's format.",
                color = surfaceColors.textMuted,
                fontSize = 12.sp
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        val config = ImportExportContract.ExportConfig(
                            format = format,
                            includeTags = includeTags,
                            includeFlags = includeFlags,
                            includeNotes = includeNotes
                        )
                        val fileName = when (format) {
                            ImportExportContract.ExportFormat.Json -> "kaiteyo-cards.json"
                            ImportExportContract.ExportFormat.Csv -> "kaiteyo-cards.csv"
                            ImportExportContract.ExportFormat.Tsv -> "kaiteyo-cards.tsv"
                            ImportExportContract.ExportFormat.Txt -> "kaiteyo-cards.txt"
                            ImportExportContract.ExportFormat.Apkg -> "kaiteyo-deck.apkg"
                        }
                        val result = viewModel.exportToFile(config, fileName)
                        result.onSuccess { bytes ->
                            val saved = saveExportFile(bytes, fileName, "Kaiteyo export", extensionFor(format))
                            notice = if (saved) "Saved $fileName" else "Save canceled (or unsupported on this platform)."
                            output = ""
                        }.onFailure { e ->
                            notice = "Export failed: ${e.message}"
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accent.primary, contentColor = accent.onPrimary)
            ) { Text("Save to file…", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }

            if (format != ImportExportContract.ExportFormat.Apkg) {
                Button(
                    onClick = {
                        val config = ImportExportContract.ExportConfig(
                            format = format,
                            includeTags = includeTags,
                            includeFlags = includeFlags,
                            includeNotes = includeNotes
                        )
                        viewModel.export(config)
                            .onSuccess { result ->
                                output = result
                                clipboard.setText(AnnotatedString(result))
                                notice = "Copied ${result.length} characters to the clipboard"
                            }
                            .onFailure { e -> notice = "Export failed: ${e.message}" }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent.primary.copy(alpha = 0.15f), contentColor = accent.primary)
                ) { Text("Generate & copy", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            }
        }

        if (notice.isNotBlank()) {
            Text(notice, color = accent.primary, fontSize = 12.sp)
        }

        if (output.isNotBlank()) {
            Text("Preview", color = surfaceColors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            PasteField(value = output, onValueChange = {}, placeholder = "", readOnly = true)
        }
    }
}

// ============================================
// Shared pieces
// ============================================

@Composable
private fun PasteField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    readOnly: Boolean = false,
    height: Int = 120
) {
    val surfaceColors = LocalSurfaceColors.current
    Box(
        modifier = Modifier.fillMaxWidth().height(height.dp).clip(RoundedCornerShape(12.dp))
            .background(surfaceColors.surfaceInteractive)
            .border(1.dp, surfaceColors.border.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(placeholder, color = surfaceColors.textMuted, fontSize = 13.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            textStyle = MaterialTheme.typography.bodySmall.copy(color = surfaceColors.textPrimary, fontSize = 13.sp),
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun PreviewStat(label: String, color: Color) {
    Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun IncludeToggle(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Row(
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onChanged(!checked) }.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp))
                .background(if (checked) accent.primary else surfaceColors.border)
        ) {
            if (checked) Text("✓", color = accent.onPrimary, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxSize())
        }
        Text(label, color = surfaceColors.textPrimary, fontSize = 13.sp)
    }
}

@Composable
private fun StatusLine(state: ImportExportContract.ScreenState.Loading) {
    val surfaceColors = LocalSurfaceColors.current
    Text(state.message, color = surfaceColors.textMuted, fontSize = 12.sp)
}

@Composable
private fun ErrorLine(state: ImportExportContract.ScreenState.Error, onDismiss: () -> Unit) {
    val accent = LocalKaiteyoAccent.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("⚠ ${state.message}", color = LocalKaiteyoSemanticColors.current.error, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier.clip(CircleShape).clickable(onClick = onDismiss).padding(horizontal = 8.dp, vertical = 2.dp)
        ) { Text("Dismiss", color = accent.primary, fontSize = 12.sp) }
    }
}

@Composable
private fun SuccessLine(state: ImportExportContract.ScreenState.Success, onDismiss: () -> Unit) {
    val accent = LocalKaiteyoAccent.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("✓ ${state.message}", color = LocalKaiteyoSemanticColors.current.success, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier.clip(CircleShape).clickable(onClick = onDismiss).padding(horizontal = 8.dp, vertical = 2.dp)
        ) { Text("OK", color = accent.primary, fontSize = 12.sp) }
    }
}

private fun extensionFor(format: ImportExportContract.TransferFormat): String = when (format) {
    ImportExportContract.TransferFormat.Json -> "json"
    ImportExportContract.TransferFormat.Csv -> "csv"
    ImportExportContract.TransferFormat.Tsv -> "tsv"
    ImportExportContract.TransferFormat.Txt -> "txt"
    ImportExportContract.TransferFormat.Apkg -> "apkg"
}

private fun extensionFor(format: ImportExportContract.ExportFormat): String = when (format) {
    ImportExportContract.ExportFormat.Json -> "json"
    ImportExportContract.ExportFormat.Csv -> "csv"
    ImportExportContract.ExportFormat.Tsv -> "tsv"
    ImportExportContract.ExportFormat.Txt -> "txt"
    ImportExportContract.ExportFormat.Apkg -> "apkg"
}
