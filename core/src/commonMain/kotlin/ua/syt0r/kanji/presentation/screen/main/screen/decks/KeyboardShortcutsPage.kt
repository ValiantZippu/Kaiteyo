package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.common.ui.KaiteyoAlertDialog

// ============================================
// KEYBOARD SHORTCUTS PAGE
// Full keyboard shortcut management like VS Code
// Click to record, conflict detection, profiles
// ============================================

data class ShortcutEntry(
    val id: String,
    val category: String,
    val action: String,
    val defaultKey: String,
    val currentKey: String = defaultKey,
    val description: String = ""
) {
    val isModified get() = currentKey != defaultKey
}

data class ShortcutProfile(
    val name: String,
    val shortcuts: List<ShortcutEntry>
)

private val keyboardDefaultShortcuts = listOf(
    ShortcutEntry("again", "Review", "Again", "C", "Mark card as failed"),
    ShortcutEntry("hard", "Review", "Hard", "X", "Mark card as hard"),
    ShortcutEntry("good", "Review", "Good", "Z", "Mark card as good"),
    ShortcutEntry("easy", "Review", "Easy", "V", "Mark card as easy"),
    ShortcutEntry("undo", "Review", "Undo", "Ctrl+Z", "Undo last action"),
    ShortcutEntry("suspend", "Review", "Suspend Card", "S", "Suspend current card"),
    ShortcutEntry("bury", "Review", "Bury Card", "B", "Bury current card"),
    ShortcutEntry("flag", "Review", "Toggle Flag", "F", "Set/unset flag"),
    ShortcutEntry("tag", "Review", "Add Tag", "T", "Add tag to card"),
    ShortcutEntry("show-answer", "Review", "Show Answer", "Space", "Flip card to show answer"),
    ShortcutEntry("play-audio", "Review", "Play Audio", "A", "Play card audio"),
    ShortcutEntry("next", "Navigation", "Next Card", "Enter", "Go to next card"),
    ShortcutEntry("previous", "Navigation", "Previous Card", "Shift+Enter", "Go to previous card"),
    ShortcutEntry("search", "Browser", "Search", "/", "Focus search bar"),
    ShortcutEntry("select-all", "Browser", "Select All", "Ctrl+A", "Select all cards"),
    ShortcutEntry("deselect", "Browser", "Deselect All", "Escape", "Clear selection"),
    ShortcutEntry("delete", "Browser", "Delete Card", "Delete", "Delete selected cards"),
    ShortcutEntry("preview", "Browser", "Preview", "P", "Preview card"),
    ShortcutEntry("retry", "Review", "Retry", "R", "Retry pronunciation"),
    ShortcutEntry("skip", "Review", "Skip", "S,Alt", "Skip card (with Alt)"),
    ShortcutEntry("stats", "Navigation", "Statistics", "I", "Open statistics"),
    ShortcutEntry("history", "Navigation", "History", "Y", "Open review history"),
    ShortcutEntry("bulk-tag", "Browser", "Bulk Tag", "Shift+T", "Tag selected cards"),
    ShortcutEntry("bulk-flag", "Browser", "Bulk Flag", "Shift+F", "Flag selected cards"),
    ShortcutEntry("mark-again", "Review", "Mark Again", "1", "Quick answer: Again"),
    ShortcutEntry("mark-hard", "Review", "Mark Hard", "2", "Quick answer: Hard"),
    ShortcutEntry("mark-good", "Review", "Mark Good", "3", "Quick answer: Good"),
    ShortcutEntry("mark-easy", "Review", "Mark Easy", "4", "Quick answer: Easy"),
    ShortcutEntry("edit-note", "Browser", "Edit Note", "N", "Edit card note"),
    ShortcutEntry("card-info", "Browser", "Card Info", "I,Alt", "Show card details"),
)

private val shortcutCategories = listOf("Review", "Navigation", "Browser")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardShortcutsPage(
    initialShortcuts: List<ShortcutEntry> = keyboardDefaultShortcuts,
    onSave: (List<ShortcutEntry>) -> Unit = { },
    onClose: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var shortcuts by remember { mutableStateOf(initialShortcuts.toMutableList()) }
    var recordingId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showImportExportDialog by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf("default") }
    var conflictWarning by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    val filteredShortcuts = remember(shortcuts, searchQuery, selectedCategory) {
        shortcuts.filter { s ->
            (searchQuery.isBlank() || s.action.contains(searchQuery, ignoreCase = true) ||
                    s.currentKey.contains(searchQuery, ignoreCase = true)) &&
            (selectedCategory == null || s.category == selectedCategory)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keyboard Shortcuts") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    IconButton(onClick = { showImportExportDialog = true }) {
                        Icon(Icons.Default.FileUpload, "Import/Export")
                    }
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.RestartAlt, "Reset All")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Profile bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Profile:", fontSize = 13.sp, color = surfaceColors.textMuted)
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedProfile,
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier.menuAnchor().width(150.dp),
                        textStyle = TextStyle(fontSize = 13.sp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        singleLine = true
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("default", "vim", "emacs", "custom").forEach { profile ->
                            DropdownMenuItem(
                                text = { Text(profile) },
                                onClick = { selectedProfile = profile; expanded = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("${filteredShortcuts.size} shortcuts", fontSize = 12.sp, color = surfaceColors.textMuted)
            }

            // Category filter chips
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All", fontSize = 12.sp) }
                )
                shortcutCategories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Search shortcuts...") },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(Modifier.height(8.dp))

            // Conflict warning banner
            conflictWarning?.let { warning ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(warning, fontSize = 12.sp, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f))
                        IconButton(onClick = { conflictWarning = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, "Dismiss", Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Shortcut list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredShortcuts, key = { it.id }) { shortcut ->
                    ShortcutRow(
                        shortcut = shortcut,
                        isRecording = recordingId == shortcut.id,
                        onStartRecording = { recordingId = shortcut.id },
                        onKeyCaptured = { key ->
                            if (recordingId == shortcut.id) {
                                val conflict = shortcuts.find { it.currentKey == key && it.id != shortcut.id }
                                conflictWarning = if (conflict != null) {
                                    "'${key}' already assigned to '${conflict.action}'"
                                } else null
                                shortcuts = shortcuts.map {
                                    if (it.id == shortcut.id) it.copy(currentKey = key) else it
                                }.toMutableList()
                                recordingId = null
                            }
                        },
                        onCancelRecording = { recordingId = null },
                        onReset = {
                            shortcuts = shortcuts.map {
                                if (it.id == shortcut.id) it.copy(currentKey = it.defaultKey) else it
                            }.toMutableList()
                        },
                        surfaceColors = surfaceColors,
                        accent = accent,
                        focusRequester = focusRequester,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    // Reset dialog
    if (showResetDialog) {
        KaiteyoAlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Shortcuts") },
            text = { Text("Reset all keyboard shortcuts to their default values? This cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    shortcuts = keyboardDefaultShortcuts.map { it.copy() }.toMutableList()
                    showResetDialog = false
                    conflictWarning = null
                }) { Text("Reset All") }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancel") } }
        )
    }

    // Import/Export dialog
    if (showImportExportDialog) {
        ImportExportShortcutsDialog(
            onImport = { imported ->
                shortcuts = imported.toMutableList()
                showImportExportDialog = false
            },
            onDismiss = { showImportExportDialog = false },
            currentShortcuts = shortcuts
        )
    }
}

@Composable
private fun ShortcutRow(
    shortcut: ShortcutEntry,
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onKeyCaptured: (String) -> Unit,
    onCancelRecording: () -> Unit,
    onReset: () -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isRecording) accent.primary.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(200), label = "recordingBg"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Action info
            Column(Modifier.weight(1f)) {
                Text(shortcut.action, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(shortcut.description, fontSize = 11.sp, color = surfaceColors.textMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.width(8.dp))

            // Key display / recorder
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isRecording) accent.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(
                        1.dp,
                        if (isRecording) accent.primary else surfaceColors.textMuted.copy(alpha = 0.2f),
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { if (!isRecording) onStartRecording() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .then(if (isRecording) Modifier.focusRequester(focusRequester) else Modifier)
                    .onKeyEvent { event ->
                        if (isRecording && event.type == KeyEventType.KeyUp) {
                            val modifiers = mutableListOf<String>()
                            if (event.isCtrlPressed) modifiers.add("Ctrl")
                            if (event.isAltPressed) modifiers.add("Alt")
                            if (event.isShiftPressed) modifiers.add("Shift")
                            if (event.isMetaPressed) modifiers.add("Meta")
                            val keyName = when (event.key) {
                                Key.Spacebar -> "Space"
                                Key.Enter -> "Enter"
                                Key.Escape -> { onCancelRecording(); return@onKeyEvent true }
                                Key.Delete -> "Delete"
                                Key.Tab -> "Tab"
                                Key.Backspace -> "Backspace"
                                else -> event.key.keyName
                            }
                            val combo = if (modifiers.isNotEmpty()) {
                                (modifiers + keyName).joinToString("+")
                            } else keyName
                            onKeyCaptured(combo)
                            true
                        } else false
                    }
            ) {
                if (isRecording) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Keyboard, null, Modifier.size(16.dp), tint = accent.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("Press key...", fontSize = 12.sp, color = accent.primary)
                    }
                } else {
                    Text(
                        formatKeyForDisplay(shortcut.currentKey),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Reset button
            if (shortcut.isModified) {
                IconButton(onClick = onReset, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.RestartAlt, "Reset", Modifier.size(16.dp), tint = surfaceColors.textMuted)
                }
            }
        }
    }
}

private fun formatKeyForDisplay(key: String): String {
    // Convert Ctrl+Shift+A style to ⌃⇧A or ^+A style
    return key
        .replace("Ctrl", "⌃")
        .replace("Shift", "⇧")
        .replace("Alt", "⌥")
        .replace("Meta", "◆")
}

private val Key.keyName: String get() = when (this) {
    Key.A -> "A"; Key.B -> "B"; Key.C -> "C"; Key.D -> "D"
    Key.E -> "E"; Key.F -> "F"; Key.G -> "G"; Key.H -> "H"
    Key.I -> "I"; Key.J -> "J"; Key.K -> "K"; Key.L -> "L"
    Key.M -> "M"; Key.N -> "N"; Key.O -> "O"; Key.P -> "P"
    Key.Q -> "Q"; Key.R -> "R"; Key.S -> "S"; Key.T -> "T"
    Key.U -> "U"; Key.V -> "V"; Key.W -> "W"; Key.X -> "X"
    Key.Y -> "Y"; Key.Z -> "Z"
    Key.Zero -> "0"; Key.One -> "1"; Key.Two -> "2"
    Key.Three -> "3"; Key.Four -> "4"; Key.Five -> "5"
    Key.Six -> "6"; Key.Seven -> "7"; Key.Eight -> "8"
    Key.Nine -> "9"
    Key.F1 -> "F1"; Key.F2 -> "F2"; Key.F3 -> "F3"; Key.F4 -> "F4"
    Key.F5 -> "F5"; Key.F6 -> "F6"; Key.F7 -> "F7"; Key.F8 -> "F8"
    Key.F9 -> "F9"; Key.F10 -> "F10"; Key.F11 -> "F11"; Key.F12 -> "F12"
    Key.Minus -> "-"; Key.Equals -> "="; Key.Comma -> ","
    Key.Period -> "."; Key.Semicolon -> ";"; Key.Apostrophe -> "'"
    Key.Slash -> "/"; Key.Backslash -> "\\"; Key.LeftBracket -> "["
    Key.RightBracket -> "]"; Key.Grave -> "`"
    else -> this.keyCode.toString()
}

@Composable
private fun ImportExportShortcutsDialog(
    onImport: (List<ShortcutEntry>) -> Unit,
    onDismiss: () -> Unit,
    currentShortcuts: List<ShortcutEntry>
) {
    var text by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("export") }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(mode.replaceFirstChar { it.uppercase() } + " Shortcuts") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == "export", onClick = { mode = "export" }, label = { Text("Export") })
                    FilterChip(selected = mode == "import", onClick = { mode = "import" }, label = { Text("Import") })
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.heightIn(max = 200.dp).fillMaxWidth(),
                    placeholder = {
                        if (mode == "export") Text("Copy this JSON to share your shortcuts")
                        else Text("Paste shortcuts JSON here...")
                    },
                    textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    maxLines = 10
                )
                if (mode == "export") {
                    LaunchedEffect(currentShortcuts) {
                        val json = buildShortcutsJson(currentShortcuts)
                        text = json
                    }
                }
            }
        },
        confirmButton = {
            if (mode == "import") {
                Button(onClick = {
                    val parsed = parseShortcutsJson(text)
                    if (parsed != null) onImport(parsed)
                }) { Text("Import") }
            } else {
                Button(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun buildShortcutsJson(shortcuts: List<ShortcutEntry>): String {
    val entries = shortcuts.joinToString(",\n    ") {
        "\"${it.id}\": \"${it.currentKey}\""
    }
    return "{\n    $entries\n}"
}

private fun parseShortcutsJson(json: String): List<ShortcutEntry>? {
    return try {
        val map = mutableMapOf<String, String>()
        val regex = Regex("\"(\\w+(-\\w+)*)\"\\s*:\\s*\"([^\"]+)\"")
        regex.findAll(json).forEach { match ->
            map[match.groupValues[1]] = match.groupValues[3]
        }
        if (map.isEmpty()) return null
        keyboardDefaultShortcuts.map { default ->
            default.copy(currentKey = map[default.id] ?: default.currentKey)
        }
    } catch (_: Exception) { null }
}
