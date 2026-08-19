package ua.syt0r.kanji.desktop.ui.dictionary

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsConfirmDialog
import ua.syt0r.kanji.desktop.designsystem.DsEmptyState
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSearchField
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsToggle
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryFormat
import ua.syt0r.kanji.desktop.engine.dictionary.InstalledDictionary
import ua.syt0r.kanji.desktop.engine.l10n.resolveSuiteString
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

// ============================================
// KAITEYO DICTIONARY MANAGER
// Full dictionary workspace: install Yomitan /
// JMdict-compatible dictionaries from ZIP, folder
// or JSON, search across all enabled dictionaries
// with rich multi-dictionary results, manage
// enabled/priority, and favorite entries.
// ============================================

@Composable
fun DictionaryManagerView(state: AppState) {
    val sc = surfaceColors()
    var query by remember { mutableStateOf(state.dictionary.query) }
    var importError by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<InstalledDictionary?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<String?>(null) }
    var filterQuery by remember { mutableStateOf("") }

    // When navigated here from an entry detail page (dictionary lookup),
    // reveal the lookup panel pre-filled with the requested term.
    LaunchedEffect(Unit) {
        if (state.dictionary.query.isNotBlank()) showSearch = true
    }

    Column(Modifier.fillMaxSize().padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        DsSectionHeader(
            title = resolveSuiteString { dictionariesTitle },
            subtitle = "${state.dictionary.installed.size} · ${state.dictionary.enabled.size} · ${resolveSuiteString { dictionariesSubtitle }}",
            action = {
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DsButton(
                        text = "Install dictionary",
                        icon = Icons.Default.Add,
                        onClick = {
                            val file = chooseDictionaryFile()
                            if (file != null) {
                                val result = state.dictionary.importFile(file, state)
                                result.fold(
                                    onSuccess = { state.toastHost.show("Installed \"${it.name}\" (${it.entryCount} entries)") },
                                    onFailure = { e -> importError = e.message ?: "Import failed" }
                                )
                            }
                        }
                    )
                    DsButton(
                        text = if (showSearch) resolveSuiteString { hideLookup } else resolveSuiteString { lookUpButton },
                        icon = Icons.Default.Search,
                        kind = DsButtonKind.Secondary,
                        onClick = { showSearch = !showSearch },
                        compact = true
                    )
                }
            }
        )

        if (showSearch) {
            DictionaryLookupCard(state, query, onQueryChange = { q ->
                query = q
                state.dictionary.query = q
            })
        }

        if (state.dictionary.installed.isEmpty()) {
            DsCard {
                DsEmptyState(
                    title = resolveSuiteString { noDictionariesTitle },
                    message = resolveSuiteString { noDictionariesMessage },
                    icon = Icons.Default.MenuBook,
                    action = {
                        DsButton(
                            text = resolveSuiteString { installDictionary },
                            icon = Icons.Default.Add,
                            onClick = {
                                val file = chooseDictionaryFile()
                                if (file != null) {
                                    val result = state.dictionary.importFile(file, state)
                                    result.fold(
                                        onSuccess = { state.toastHost.show("Installed \"${it.name}\"") },
                                        onFailure = { e -> importError = e.message ?: "Import failed" }
                                    )
                                }
                            }
                        )
                    }
                )
            }
        } else {
            DictionaryList(state, onDelete = { deleteTarget = it })
        }

        importError?.let { err ->
            DsCard {
                Text(err, color = sc.textPrimary, fontSize = DsType.Body)
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md), modifier = Modifier.padding(top = DsSpacing.Sm)) {
                    DsButton(text = "OK", kind = DsButtonKind.Secondary, compact = true, onClick = { importError = null })
                }
            }
        }
    }

    deleteTarget?.let { target ->
        DsConfirmDialog(
            title = "Remove dictionary",
            message = "Remove '${target.name}'? Its index files are deleted; cards already mined from it stay in your deck.",
            confirmText = "Remove",
            danger = true,
            onConfirm = {
                state.dictionary.remove(target.id, state)
                state.toastHost.show("Removed \"${target.name}\"")
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DictionaryLookupCard(state: AppState, query: String, onQueryChange: (String) -> Unit) {
    val sc = surfaceColors()
    var expandedHeadword by remember { mutableStateOf<String?>(null) }
    val groups = remember(query) {
        if (query.isBlank()) emptyList()
        else state.dictionary.lookup(query)
    }
    // Index-backed suggestions (Phase 8): instant headword/reading candidates
    // from the trigram index while the full grouped search is being typed.
    val suggestions = remember(query) {
        if (query.isBlank()) emptyList()
        else state.dictionary.suggestions(query, limit = 6)
    }

    DsCard {
        Column(Modifier.fillMaxWidth().padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            DsSearchField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "水, mizu, water, たべる…",
                autoFocus = false
            )
            if (suggestions.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs),
                    verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
                ) {
                    suggestions.forEach { match ->
                        val headword = match.entry.headword
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(DsRadius.Md))
                                .background(sc.surfaceInteractive)
                                .clickable { onQueryChange(headword) }
                                .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Xs)
                        ) {
                            Text(
                                text = headword,
                                color = accent().primary,
                                fontSize = DsType.Caption,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            if (query.isBlank()) {
                Text("Type kanji, kana, romaji or English to search across enabled dictionaries.", color = sc.textMuted, fontSize = DsType.Caption)
            } else if (groups.isEmpty()) {
                Text("No dictionary matches for \"$query\". Try another spelling.", color = sc.textMuted, fontSize = DsType.Body)
            } else {
                groups.forEach { group ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = DsSpacing.Sm)) {
                        Text(group.dictionary.name, color = sc.textSecondary, fontSize = DsType.Label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("${group.matches.size} match(es)", color = sc.textMuted, fontSize = DsType.Caption)
                    }
                    group.matches.forEach { match ->
                        val key = "${match.dictionary.id}|${match.entry.headword}"
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedHeadword = if (expandedHeadword == key) null else key }
                                .padding(vertical = DsSpacing.Sm)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(match.entry.headword, color = sc.textPrimary, fontSize = DsType.Heading, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(DsSpacing.Sm))
                                match.entry.readings.firstOrNull()?.let { r ->
                                    Text(r.reading, color = sc.textSecondary, fontSize = DsType.Body)
                                }
                            }
                            Text(
                                match.entry.senses.firstOrNull()?.glosses?.joinToString("; ").orEmpty(),
                                color = sc.textSecondary,
                                fontSize = DsType.Body,
                                maxLines = 2
                            )
                            if (expandedHeadword == key) {
                                Spacer(Modifier.height(DsSpacing.Sm))
                                Text("Part of speech: ${match.entry.senses.firstOrNull()?.partOfSpeech?.joinToString(", ").orEmpty()}", color = sc.textMuted, fontSize = DsType.Caption)
                                match.entry.kanjiSpellings.firstOrNull()?.let { k ->
                                    Text(
                                        "Kanji: ${k.character}  ·  ${k.strokeCounts.firstOrNull()?.let { "$it strokes" } ?: ""}  ·  grade ${k.grade ?: "-"}  ·  JLPT N${k.jlpt ?: "-"}",
                                        color = sc.textMuted,
                                        fontSize = DsType.Caption
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md), modifier = Modifier.padding(top = DsSpacing.Sm)) {
                                    DsButton(
                                        text = "Mine",
                                        icon = Icons.Default.Add,
                                        kind = DsButtonKind.Primary,
                                        compact = true,
                                        onClick = { state.mining.openMining(state.mining.payloadForEntry(match.entry, group.dictionary.name)) }
                                    )
                                    val fav = state.dictionary.isFavorite(match.dictionary.id, match.entry.headword)
                                    DsButton(
                                        text = if (fav) "Unfavorite" else "Favorite",
                                        kind = DsButtonKind.Secondary,
                                        compact = true,
                                        onClick = { state.dictionary.toggleFavorite(match.dictionary.id, match.entry.headword) }
                                    )
                                    DsButton(
                                        text = "Graph",
                                        icon = Icons.Default.Route,
                                        kind = DsButtonKind.Secondary,
                                        compact = true,
                                        onClick = {
                                            state.pendingGraphNode = match.entry.headword
                                            state.currentView = ua.syt0r.kanji.desktop.appstate.WorkspaceView.Graph
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DictionaryList(state: AppState, onDelete: (InstalledDictionary) -> Unit) {
    val sc = surfaceColors()
    val dictionaries = state.dictionary.installed

    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
        Text(
            "Installed (drag order = priority)",
            color = sc.textMuted,
            fontSize = DsType.Caption,
            fontWeight = FontWeight.SemiBold
        )
        LazyColumn(
            Modifier.fillMaxWidth().height(360.dp),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
        ) {
            items(dictionaries, key = { it.id }) { dict ->
                DictionaryRow(state, dict, onDelete)
            }
        }
    }
}

@Composable
private fun DictionaryRow(state: AppState, dict: InstalledDictionary, onDelete: (InstalledDictionary) -> Unit) {
    val sc = surfaceColors()
    DsCard(elevated = true) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DsSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text(dict.name, color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                    DsBadge(text = formatLabel(dict.format), tint = sc.textMuted)
                }
                Text(
                    buildString {
                        append(dict.entryCount).append(" entries")
                        if (dict.revision.isNotBlank()) append("  ·  rev ").append(dict.revision)
                        if (dict.authoredBy.isNotBlank()) append("  ·  by ").append(dict.authoredBy)
                    },
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
            DsToggle(
                checked = dict.enabled,
                onCheckedChange = { state.dictionary.setEnabled(dict.id, it) },
                label = null
            )
            DsIconButton(
                icon = Icons.Default.Delete,
                onClick = { onDelete(dict) },
                contentDescription = "Remove ${dict.name}",
                size = 30.dp
            )
        }
    }
}

private fun formatLabel(format: DictionaryFormat): String = when (format) {
    DictionaryFormat.Yomitan -> "Yomitan"
    DictionaryFormat.JmDict -> "JMdict"
    DictionaryFormat.KanjiDic -> "KANJIDIC"
    DictionaryFormat.Frequency -> "Frequency"
    DictionaryFormat.PitchAccent -> "Pitch accent"
    DictionaryFormat.Grammar -> "Grammar"
    DictionaryFormat.Name -> "Names"
    DictionaryFormat.Custom -> "Kaiteyo"
}

private fun chooseDictionaryFile(): File? {
    val chooser = JFileChooser().apply {
        dialogTitle = "Install dictionary (Yomitan ZIP / folder / JSON)"
        fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
        isAcceptAllFileFilterUsed = true
        addChoosableFileFilter(FileNameExtensionFilter("Yomitan export (ZIP)", "zip"))
        addChoosableFileFilter(FileNameExtensionFilter("Term JSON", "json"))
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}
