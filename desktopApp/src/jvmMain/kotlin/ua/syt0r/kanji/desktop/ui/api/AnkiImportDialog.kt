package ua.syt0r.kanji.desktop.ui.api

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsDialog
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTextButton
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.transfer.AnkiConflictPolicy
import ua.syt0r.kanji.desktop.engine.transfer.AnkiDeckImportResult
import ua.syt0r.kanji.desktop.engine.transfer.AnkiDeckPreview
import ua.syt0r.kanji.desktop.engine.transfer.AnkiImportOptions

// ============================================
// ANKI IMPORT DIALOG
// Preview AnkiConnect decks (name, note/card
// counts, tags), pick which to import, choose a
// conflict policy, then run the import with live
// progress and a per-deck result summary. Every
// number is fetched from AnkiConnect in real
// time — nothing here is mocked.
// ============================================

@Composable
fun AnkiImportDialog(state: AppState, onDismiss: () -> Unit) {
    val sc = surfaceColors()
    val scope = rememberCoroutineScope()
    val importer = remember { state.ankiImporter }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var decks by remember { mutableStateOf<List<AnkiDeckPreview>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var policy by remember { mutableStateOf(AnkiConflictPolicy.Skip) }
    var includeScheduling by remember { mutableStateOf(true) }
    var includeMedia by remember { mutableStateOf(true) }
    var running by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var progressLabel by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<AnkiDeckImportResult>?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        withContext(Dispatchers.IO) { importer.fetchPreview() }
            .fold(
                onSuccess = { preview ->
                    decks = preview
                    selected = preview.map { it.name }.toSet()
                    loading = false
                },
                onFailure = { e ->
                    error = e.message ?: "Could not reach AnkiConnect"
                    loading = false
                }
            )
    }

    DsDialog(
        title = "Import from Anki",
        onDismiss = { if (!running) onDismiss() }
    ) {
        when {
            loading -> {
                Text("Contacting AnkiConnect…", color = sc.textSecondary, fontSize = DsType.Body)
                Spacer(Modifier.height(DsSpacing.Lg))
                Text(
                    "Make sure Anki is running with the AnkiConnect add-on enabled.",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }

            error != null -> {
                Text("Could not load decks", color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(DsSpacing.Sm))
                Text(error.orEmpty(), color = sc.textSecondary, fontSize = DsType.Body)
                Spacer(Modifier.height(DsSpacing.Lg))
                Text(
                    "Check that Anki is open and AnkiConnect is enabled, then try again.",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
                Spacer(Modifier.height(DsSpacing.Xl))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
                ) {
                    DsTextButton("Close", onClick = onDismiss)
                    DsButton(
                        text = "Retry",
                        onClick = {
                            loading = true
                            error = null
                            scope.launch {
                                withContext(Dispatchers.IO) { importer.fetchPreview() }
                                    .fold(
                                        onSuccess = {
                                            decks = it
                                            selected = it.map { d -> d.name }.toSet()
                                            loading = false
                                        },
                                        onFailure = { e -> error = e.message; loading = false }
                                    )
                            }
                        }
                    )
                }
            }

            results != null -> ResultSummary(results.orEmpty(), onDismiss)

            else -> ImportOptions(
                decks = decks,
                selected = selected,
                onToggle = { name ->
                    selected = if (name in selected) selected - name else selected + name
                },
                policy = policy,
                onPolicy = { policy = it },
                includeScheduling = includeScheduling,
                onIncludeScheduling = { includeScheduling = it },
                includeMedia = includeMedia,
                onIncludeMedia = { includeMedia = it },
                running = running,
                progress = progress,
                progressLabel = progressLabel,
                onImport = {
                    if (running || selected.isEmpty()) return@ImportOptions
                    running = true
                    progress = 0f
                    results = null
                    val chosen = decks.filter { it.name in selected }
                    val totalNotes = chosen.sumOf { it.noteCount }.coerceAtLeast(1)
                    scope.launch {
                        val out = mutableListOf<AnkiDeckImportResult>()
                        var done = 0
                        chosen.forEachIndexed { index, deck ->
                            withContext(Dispatchers.IO) {
                                importer.importDeck(
                                    deck,
                                    AnkiImportOptions(
                                        policy = policy,
                                        includeScheduling = includeScheduling,
                                        includeMedia = includeMedia
                                    ),
                                    onProgress = { processed, _ ->
                                        val noteShare = if (totalNotes > 0) (done + processed).toFloat() / totalNotes else 1f
                                        val label = "Importing \"${deck.name}\" — $processed notes…"
                                        val value = ((index / chosen.size.toFloat()) * 0.1f) + (noteShare * 0.9f)
                                        scope.launch(Dispatchers.Main) {
                                            progressLabel = label
                                            progress = value
                                        }
                                    }
                                )
                            }.onSuccess { out.add(it) }
                                .onFailure { e -> out.add(AnkiDeckImportResult(deck.name, warnings = listOf(e.message ?: "failed"))) }
                            done += deck.noteCount
                        }
                        running = false
                        progress = 1f
                        progressLabel = "Done"
                        results = out
                    }
                },
                onCancel = onDismiss
            )
        }
    }
}

@Composable
private fun ImportOptions(
    decks: List<AnkiDeckPreview>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    policy: AnkiConflictPolicy,
    onPolicy: (AnkiConflictPolicy) -> Unit,
    includeScheduling: Boolean,
    onIncludeScheduling: (Boolean) -> Unit,
    includeMedia: Boolean,
    onIncludeMedia: (Boolean) -> Unit,
    running: Boolean,
    progress: Float,
    progressLabel: String,
    onImport: () -> Unit,
    onCancel: () -> Unit
) {
    val sc = surfaceColors()

    Text(
        "Select decks to import. The Anki deck hierarchy (Japanese::N5::Kanji) is preserved as Kaiteyo deck nesting.",
        color = sc.textSecondary,
        fontSize = DsType.Body
    )
    Spacer(Modifier.height(DsSpacing.Lg))

    if (decks.isEmpty()) {
        Text("No decks found in Anki.", color = sc.textMuted, fontSize = DsType.Body)
    } else {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
        ) {
            decks.forEach { deck ->
                DeckRow(
                    deck = deck,
                    checked = deck.name in selected,
                    enabled = !running,
                    onToggle = { onToggle(deck.name) }
                )
            }
        }
    }

    Spacer(Modifier.height(DsSpacing.Lg))

    Text("If a card already exists", color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(DsSpacing.Sm))
    Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        AnkiConflictPolicy.entries.forEach { p ->
            DsButton(
                text = p.label,
                kind = if (p == policy) DsButtonKind.Primary else DsButtonKind.Secondary,
                compact = true,
                enabled = !running,
                onClick = { onPolicy(p) }
            )
        }
    }
    Spacer(Modifier.height(DsSpacing.Sm))
    Text(policy.description, color = sc.textMuted, fontSize = DsType.Caption)

    Spacer(Modifier.height(DsSpacing.Lg))

    ToggleRow("Import scheduling", "Carry over interval, due date, ease, reps and lapses from Anki", includeScheduling, !running, onIncludeScheduling)
    Spacer(Modifier.height(DsSpacing.Sm))
    ToggleRow("Import media", "Download audio and images referenced by note fields", includeMedia, !running, onIncludeMedia)

    Spacer(Modifier.height(DsSpacing.Lg))

    if (running) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(sc.surfaceInteractive)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(sc.textSecondary)
            )
        }
        Spacer(Modifier.height(DsSpacing.Sm))
        Text(progressLabel, color = sc.textMuted, fontSize = DsType.Caption)
    }

    Spacer(Modifier.height(DsSpacing.Xl))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
    ) {
        DsTextButton(if (running) "Importing…" else "Cancel", onClick = onCancel, enabled = !running)
        DsButton(
            text = "Import ${selected.size} deck(s)",
            onClick = onImport,
            enabled = !running && selected.isNotEmpty()
        )
    }
}

@Composable
private fun DeckRow(
    deck: AnkiDeckPreview,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val sc = surfaceColors()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (checked) sc.surfaceElevated else sc.surfaceInteractive.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Box(
            Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (checked) sc.textSecondary else sc.surfaceInteractive),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Text("✓", color = sc.surfaceElevated, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(deck.name, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium)
            deck.sampleFronts.firstOrNull()?.let { front ->
                Text(
                    "e.g. $front".take(44),
                    color = sc.textSecondary,
                    fontSize = DsType.Caption
                )
            }
            if (deck.tags.isNotEmpty()) {
                Text(
                    deck.tags.joinToString(", ").take(36),
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }
        Text(
            "${deck.noteCount} notes · ${deck.cardCount} cards",
            color = sc.textSecondary,
            fontSize = DsType.Caption
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    value: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val sc = surfaceColors()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onToggle(!value) }
            .padding(vertical = DsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium)
            Text(subtitle, color = sc.textMuted, fontSize = DsType.Caption)
        }
        DsBadge(text = if (value) "On" else "Off", tint = sc.textSecondary)
    }
}

@Composable
private fun ResultSummary(results: List<AnkiDeckImportResult>, onClose: () -> Unit) {
    val sc = surfaceColors()
    Text("Import complete", color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)

    Spacer(Modifier.height(DsSpacing.Lg))
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        results.forEach { r ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
            ) {
                Text(r.deckName, color = sc.textPrimary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
                DsBadge(text = "${r.imported} new", tint = sc.textSecondary)
                if (r.updated > 0) DsBadge(text = "${r.updated} updated", tint = sc.textSecondary)
                if (r.skipped > 0) DsBadge(text = "${r.skipped} skipped", tint = sc.textMuted)
            }
            if (r.mediaSaved > 0) {
                Text("${r.mediaSaved} media file(s) downloaded", color = sc.textMuted, fontSize = DsType.Caption)
            }
            r.warnings.take(2).forEach { w ->
                Text("⚠ $w", color = sc.textMuted, fontSize = DsType.Caption)
            }
        }
    }

    Spacer(Modifier.height(DsSpacing.Xl))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
    ) {
        DsButton(text = "Done", onClick = onClose)
    }
}
