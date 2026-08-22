@file:OptIn(ExperimentalMaterial3Api::class)

package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoSemanticColors
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.common.ui.KaiteyoAlertDialog

// ============================================
// KAITEYO v1.2 — REVIEW SETTINGS & KEYBOARD SHORTCUTS
// Full review configuration and shortcut management
// ============================================

// ════════════════════════════════════════════
// REVIEW SETTINGS FULL SCREEN
// ════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSettingsFullScreen(
    initialSettings: ReviewSettingsV2 = ReviewSettingsV2(),
    onSave: (ReviewSettingsV2) -> Unit = {},
    onClose: () -> Unit = {}
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var settings by remember { mutableStateOf(initialSettings) }
    var hasChanges by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("layout") }

    val categories = listOf(
        "layout" to "Layout",
        "buttons" to "Buttons",
        "display" to "Display",
        "behavior" to "Behavior",
        "advanced" to "Advanced"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Settings") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    TextButton(onClick = { onSave(settings); hasChanges = false }) { Text("Save") }
                    TextButton(onClick = { settings = ReviewSettingsV2(); hasChanges = true }) { Text("Reset", color = surfaceColors.textMuted) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColors.surface,
                    titleContentColor = surfaceColors.textPrimary
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Category tabs
            LazyRow(
                modifier = Modifier.fillMaxWidth().background(surfaceColors.surfaceElevated),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { (key, label) ->
                    FilterChip(
                        selected = selectedCategory == key,
                        onClick = { selectedCategory = key },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            HorizontalDivider(color = surfaceColors.border.copy(alpha = 0.2f))

            // Settings content
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (selectedCategory) {
                    "layout" -> {
                        item { SettingsHeader("Review Layout", "Choose how cards are displayed during review", surfaceColors) }
                        item {
                            SettingsDropdown(
                                label = "Layout",
                                value = settings.layout.displayName,
                                options = ReviewLayout.entries.map { it.displayName },
                                onSelect = { idx -> settings = settings.copy(layout = ReviewLayout.entries[idx]); hasChanges = true },
                                surfaceColors = surfaceColors
                            )
                        }
                        item {
                            SettingsDropdown(
                                label = "Button Size",
                                value = settings.buttonSize.displayName,
                                options = ReviewButtonSize.entries.map { it.displayName },
                                onSelect = { idx -> settings = settings.copy(buttonSize = ReviewButtonSize.entries[idx]); hasChanges = true },
                                surfaceColors = surfaceColors
                            )
                        }
                        item {
                            SettingsDropdown(
                                label = "Button Mode",
                                value = settings.buttonMode.displayName,
                                options = ReviewButtonMode.entries.map { it.displayName },
                                onSelect = { idx -> settings = settings.copy(buttonMode = ReviewButtonMode.entries[idx]); hasChanges = true },
                                surfaceColors = surfaceColors
                            )
                        }
                        item {
                            SettingsSlider(
                                label = "Card Padding",
                                value = settings.cardPadding.toFloat(),
                                onValueChange = { settings = settings.copy(cardPadding = it.toInt()); hasChanges = true },
                                valueRange = 0f..32f,
                                suffix = "dp",
                                surfaceColors = surfaceColors
                            )
                        }
                    }
                    "buttons" -> {
                        item { SettingsHeader("Button Visibility", "Show or hide individual answer buttons", surfaceColors) }
                        item { SettingsSwitch("Show Again Button", !settings.hideAgain, { settings = settings.copy(hideAgain = !it); hasChanges = true }, surfaceColors) }
                        item { SettingsSwitch("Show Hard Button", !settings.hideHard, { settings = settings.copy(hideHard = !it); hasChanges = true }, surfaceColors) }
                        item { SettingsSwitch("Show Good Button", !settings.hideGood, { settings = settings.copy(hideGood = !it); hasChanges = true }, surfaceColors) }
                        item { SettingsSwitch("Show Easy Button", !settings.hideEasy, { settings = settings.copy(hideEasy = !it); hasChanges = true }, surfaceColors) }
                        item { SettingsSwitch("Show Answer Button", settings.showAnswerButton, { settings = settings.copy(showAnswerButton = it); hasChanges = true }, surfaceColors) }
                        item { Spacer(Modifier.height(8.dp)) }
                        item { SettingsHeader("Confirmation", "Require confirmation for actions", surfaceColors) }
                        item { SettingsSwitch("Confirmation Dialogs", settings.confirmationDialogs, { settings = settings.copy(confirmationDialogs = it); hasChanges = true }, surfaceColors) }
                        item { SettingsSwitch("Skip Reveal Delay", settings.skipRevealDelay, { settings = settings.copy(skipRevealDelay = it); hasChanges = true }, surfaceColors) }
                    }
                    "display" -> {
                        item { SettingsHeader("Display Options", "Control what information is shown on cards", surfaceColors) }
                        item { SettingsSwitch("Show Timer", settings.showTimer, { settings = settings.copy(showTimer = it); hasChanges = true }, surfaceColors) }
                        item { SettingsSwitch("Show Card Count", settings.showCardCount, { settings = settings.copy(showCardCount = it); hasChanges = true }, surfaceColors) }
                        item { SettingsSwitch("Show Deck Name", settings.showDeckName, { settings = settings.copy(showDeckName = it); hasChanges = true }, surfaceColors) }
                        item { SettingsSwitch("Show Tags", settings.showTags, { settings = settings.copy(showTags = it); hasChanges = true }, surfaceColors) }
                        item { SettingsSwitch("Show All Tags", settings.showAllTags, { settings = settings.copy(showAllTags = it); hasChanges = true }, surfaceColors) }
                        item { SettingsSwitch("Show All Flags", settings.showAllFlags, { settings = settings.copy(showAllFlags = it); hasChanges = true }, surfaceColors) }
                        item { SettingsSwitch("Show Remaining", settings.showRemaining, { settings = settings.copy(showRemaining = it); hasChanges = true }, surfaceColors) }
                        item { SettingsSwitch("Show Estimated Time", settings.showEstimatedTime, { settings = settings.copy(showEstimatedTime = it); hasChanges = true }, surfaceColors) }
                        item { SettingsSwitch("Show Next Review Time", settings.showNextReviewTime, { settings = settings.copy(showNextReviewTime = it); hasChanges = true }, surfaceColors) }
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            SettingsSlider(
                                label = "Font Size Scale",
                                value = settings.fontSizeScale,
                                onValueChange = { settings = settings.copy(fontSizeScale = it); hasChanges = true },
                                valueRange = 0.5f..2.0f,
                                suffix = "x",
                                surfaceColors = surfaceColors
                            )
                        }
                    }
                    "behavior" -> {
                        item { SettingsHeader("Review Behavior", "Control how reviews work", surfaceColors) }
                        item { SettingsSwitch("Auto-Play Audio", settings.autoPlayAudio, { settings = settings.copy(autoPlayAudio = it); hasChanges = true }, surfaceColors) }
                        item { SettingsSwitch("Swipe Gestures", settings.swipeGestures, { settings = settings.copy(swipeGestures = it); hasChanges = true }, surfaceColors) }
                        item { SettingsSwitch("Tap to Reveal", settings.tapToReveal, { settings = settings.copy(tapToReveal = it); hasChanges = true }, surfaceColors) }
                        item { SettingsSwitch("Scroll to Reveal", settings.scrollToReveal, { settings = settings.copy(scrollToReveal = it); hasChanges = true }, surfaceColors) }
                        item { SettingsSwitch("Night Mode in Reviews", settings.nightModeInReviews, { settings = settings.copy(nightModeInReviews = it); hasChanges = true }, surfaceColors) }
                        item { SettingsSwitch("Bury Related on Answer", settings.buryRelatedOnAnswer, { settings = settings.copy(buryRelatedOnAnswer = it); hasChanges = true }, surfaceColors) }
                        item { SettingsSwitch("Auto-Advance", settings.autoAdvance, { settings = settings.copy(autoAdvance = it); hasChanges = true }, surfaceColors) }
                        if (settings.autoAdvance) {
                            item {
                                SettingsSlider(
                                    label = "Auto-Advance Delay (seconds)",
                                    value = settings.autoAdvanceSeconds.toFloat(),
                                    onValueChange = { settings = settings.copy(autoAdvanceSeconds = it.toInt()); hasChanges = true },
                                    valueRange = 1f..30f,
                                    suffix = "s",
                                    surfaceColors = surfaceColors
                                )
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item { SettingsHeader("Study Time", "Keep study time honest by ignoring idle gaps", surfaceColors) }
                        item {
                            SettingsSwitch(
                                "Smart Activity Detection",
                                settings.smartActivityDetection,
                                { settings = settings.copy(smartActivityDetection = it); hasChanges = true },
                                surfaceColors
                            )
                        }
                        if (settings.smartActivityDetection) {
                            item {
                                SettingsSlider(
                                    label = "Idle Threshold",
                                    value = settings.inactivityThresholdMinutes.toFloat(),
                                    onValueChange = { settings = settings.copy(inactivityThresholdMinutes = it.toInt()); hasChanges = true },
                                    valueRange = 1f..60f,
                                    suffix = "m",
                                    surfaceColors = surfaceColors
                                )
                            }
                        }
                    }
                    "advanced" -> {
                        item { SettingsHeader("Advanced Settings", "Additional review configuration", surfaceColors) }
                        item { SettingsSwitch("Skip Reveal Delay", settings.skipRevealDelay, { settings = settings.copy(skipRevealDelay = it); hasChanges = true }, surfaceColors) }
                        item { SettingsSwitch("Confirmation Dialogs", settings.confirmationDialogs, { settings = settings.copy(confirmationDialogs = it); hasChanges = true }, surfaceColors) }
                        item { SettingsSwitch("Bury Related New Cards", settings.buryRelatedOnAnswer, { settings = settings.copy(buryRelatedOnAnswer = it); hasChanges = true }, surfaceColors) }
                    }
                }

                // Preview
                item {
                    Spacer(Modifier.height(16.dp))
                    ReviewPreviewCard(
                        settings = settings,
                        surfaceColors = surfaceColors,
                        accent = accent
                    )
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

// ════════════════════════════════════════════
// SETTINGS COMPONENTS
// ════════════════════════════════════════════

@Composable
private fun SettingsHeader(title: String, description: String, surfaceColors: SurfaceColors) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, color = surfaceColors.textPrimary)
        Text(description, fontSize = 12.sp, color = surfaceColors.textMuted)
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SettingsSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    surfaceColors: SurfaceColors
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = surfaceColors.textPrimary, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
    surfaceColors: SurfaceColors
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, fontSize = 13.sp, color = surfaceColors.textMuted)
        Spacer(Modifier.height(4.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { onSelect(index); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    suffix: String,
    surfaceColors: SurfaceColors
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, color = surfaceColors.textPrimary)
            Text("${value.toInt()}$suffix", fontSize = 13.sp, color = surfaceColors.textMuted)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

// ════════════════════════════════════════════
// REVIEW PREVIEW CARD
// ════════════════════════════════════════════

@Composable
private fun ReviewPreviewCard(
    settings: ReviewSettingsV2,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surfaceElevated),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Preview", style = MaterialTheme.typography.titleSmall, color = surfaceColors.textPrimary)
            Spacer(Modifier.height(12.dp))

            // Simulated card
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceColors.surface)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("水", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                    Text("Water", fontSize = 14.sp, color = surfaceColors.textMuted)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Simulated buttons — a static preview of the review layout with
            // the current settings; the real buttons work during review.
            Text(
                text = "Simulated preview — the real buttons work during review",
                fontSize = 11.sp,
                color = surfaceColors.textMuted,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val buttons = mutableListOf<String>()
                if (!settings.hideAgain) buttons.add("Again")
                if (!settings.hideHard) buttons.add("Hard")
                if (!settings.hideGood) buttons.add("Good")
                if (!settings.hideEasy) buttons.add("Easy")

                buttons.forEach { label ->
                    val sem = LocalKaiteyoSemanticColors.current
                    val btnColor = when (label) {
                        "Again" -> sem.reviewAgain
                        "Hard" -> sem.reviewHard
                        "Good" -> sem.reviewGood
                        "Easy" -> sem.reviewEasy
                        else -> accent.primary
                    }
                    Button(
                        // Static preview — intentionally non-interactive.
                        onClick = { /* preview only */ },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = btnColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (settings.showTimer || settings.showCardCount) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (settings.showCardCount) {
                        Text("12 / 45", fontSize = 11.sp, color = surfaceColors.textMuted)
                    }
                    if (settings.showTimer) {
                        Text("00:32", fontSize = 11.sp, color = surfaceColors.textMuted)
                    }
                    if (settings.showRemaining) {
                        Text("33 remaining", fontSize = 11.sp, color = surfaceColors.textMuted)
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════
// KEYBOARD SHORTCUTS FULL SCREEN
// ════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun KeyboardShortcutsFullScreen(
    initialShortcuts: List<ShortcutEntryV2> = defaultShortcuts,
    initialProfiles: List<ShortcutProfileV2> = listOf(ShortcutProfileV2(id = "default", name = "Default", shortcuts = initialShortcuts, isBuiltIn = true)),
    onSave: (List<ShortcutEntryV2>) -> Unit = {},
    onClose: () -> Unit = {}
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    var shortcuts by remember { mutableStateOf(initialShortcuts) }
    var profiles by remember { mutableStateOf(initialProfiles) }
    var activeProfile by remember { mutableStateOf(profiles.firstOrNull()?.id ?: "default") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ShortcutCategory?>(null) }
    var showRecordingDialog by remember { mutableStateOf<ShortcutEntryV2?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showConflictDialog by remember { mutableStateOf<String?>(null) }
    var recordingKey by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }

    // Filtered shortcuts
    val filteredShortcuts = remember(shortcuts, searchQuery, selectedCategory) {
        shortcuts.filter { s ->
            (searchQuery.isBlank() || s.actionName.lowercase().contains(searchQuery.lowercase()) ||
             s.description.lowercase().contains(searchQuery.lowercase()) ||
             (s.primaryKey?.displayText?.lowercase()?.contains(searchQuery.lowercase()) ?: false)) &&
            (selectedCategory == null || s.category == selectedCategory)
        }.groupBy { it.category }
    }

    val currentProfile = profiles.find { it.id == activeProfile }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keyboard Shortcuts") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    IconButton(onClick = { showProfileDialog = true }) { Icon(Icons.Default.Person, "Profiles") }
                    IconButton(onClick = { showImportDialog = true }) { Icon(Icons.Default.FileDownload, "Import") }
                    IconButton(onClick = { showExportDialog = true }) { Icon(Icons.Default.FileUpload, "Export") }
                    TextButton(onClick = { onSave(shortcuts) }) { Text("Save") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColors.surface,
                    titleContentColor = surfaceColors.textPrimary
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Profile bar
            Row(
                modifier = Modifier.fillMaxWidth().background(surfaceColors.surfaceElevated).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Profile:", fontSize = 12.sp, color = surfaceColors.textMuted)
                Spacer(Modifier.width(8.dp))
                Text(currentProfile?.name ?: "Default", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = surfaceColors.textPrimary)
                Spacer(Modifier.weight(1f))
                Text("${shortcuts.size} shortcuts", fontSize = 12.sp, color = surfaceColors.textMuted)
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                placeholder = { Text("Search shortcuts...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, "Clear", Modifier.size(18.dp)) }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent.primary.copy(alpha = 0.5f))
            )

            // Category filter chips
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All", fontSize = 11.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }
                items(ShortcutCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat.displayName, fontSize = 11.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            // Shortcut list
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                filteredShortcuts.forEach { (category, catShortcuts) ->
                    item {
                        Text(
                            category.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = surfaceColors.textMuted,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                    items(catShortcuts, key = { it.id }) { shortcut ->
                        ShortcutRow(
                            shortcut = shortcut,
                            onRecord = { showRecordingDialog = shortcut },
                            onReset = {
                                val default = initialShortcuts.find { it.id == shortcut.id }
                                if (default != null) {
                                    shortcuts = shortcuts.map { if (it.id == shortcut.id) default else it }
                                }
                            },
                            onToggle = {
                                shortcuts = shortcuts.map { if (it.id == shortcut.id) it.copy(isEnabled = !it.isEnabled) else it }
                            },
                            hasConflict = shortcuts.any { s ->
                                s.id != shortcut.id && s.isEnabled &&
                                s.primaryKey?.isDefined == true &&
                                s.primaryKey == shortcut.primaryKey
                            },
                            surfaceColors = surfaceColors,
                            accent = accent
                        )
                    }
                }

                if (filteredShortcuts.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(if (searchQuery.isNotBlank()) "No matching shortcuts" else "No shortcuts available",
                                color = surfaceColors.textMuted)
                        }
                    }
                }
            }
        }
    }

    // Recording dialog
    showRecordingDialog?.let { shortcut ->
        KaiteyoAlertDialog(
            onDismissRequest = {
                showRecordingDialog = null
                isRecording = false
            },
            title = { Text("Record Shortcut") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Press a key combination for:", fontSize = 14.sp, color = surfaceColors.textMuted)
                    Spacer(Modifier.height(8.dp))
                    Text(shortcut.actionName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = surfaceColors.textPrimary)
                    Spacer(Modifier.height(16.dp))

                    if (isRecording) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = surfaceColors.surfaceElevated)
                        ) {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text(recordingKey.ifBlank { "Listening..." }, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                    color = if (recordingKey.isNotBlank()) accent.primary else surfaceColors.textMuted)
                            }
                        }
                    } else {
                        Button(
                            onClick = { isRecording = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Keyboard, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Click to Record")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Common combinations
                    if (!isRecording) {
                        Text("Or choose a preset:", fontSize = 12.sp, color = surfaceColors.textMuted)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                AssistChip(onClick = {
                                    recordingKey = "Ctrl+1"
                                }, label = { Text("Ctrl+1", fontSize = 11.sp) }, modifier = Modifier.height(28.dp))
                            }
                            item {
                                AssistChip(onClick = {
                                    recordingKey = "Ctrl+Shift+A"
                                }, label = { Text("Ctrl+Shift+A", fontSize = 11.sp) }, modifier = Modifier.height(28.dp))
                            }
                            item {
                                AssistChip(onClick = {
                                    recordingKey = "Alt+Shift+1"
                                }, label = { Text("Alt+Shift+1", fontSize = 11.sp) }, modifier = Modifier.height(28.dp))
                            }
                            item {
                                AssistChip(onClick = {
                                    recordingKey = "Space"
                                }, label = { Text("Space", fontSize = 11.sp) }, modifier = Modifier.height(28.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (recordingKey.isNotBlank()) {
                            // Check for conflicts
                            val conflicting = shortcuts.find { s ->
                                s.id != shortcut.id && s.isEnabled &&
                                s.primaryKey?.displayText == recordingKey
                            }
                            if (conflicting != null) {
                                showConflictDialog = "${conflicting.actionName} already uses $recordingKey"
                            } else {
                                val newCombination = KeyCombination(key = recordingKey)
                                shortcuts = shortcuts.map {
                                    if (it.id == shortcut.id) it.copy(primaryKey = newCombination) else it
                                }
                                showRecordingDialog = null
                                isRecording = false
                                recordingKey = ""
                            }
                        }
                    },
                    enabled = recordingKey.isNotBlank()
                ) { Text("Assign") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRecordingDialog = null
                    isRecording = false
                    recordingKey = ""
                }) { Text("Cancel") }
            }
        )
    }

    // Conflict dialog
    showConflictDialog?.let { msg ->
        KaiteyoAlertDialog(
            onDismissRequest = { showConflictDialog = null },
            title = { Text("Conflict Detected") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { showConflictDialog = null }) { Text("OK") }
            }
        )
    }

    // Profile dialog
    if (showProfileDialog) {
        ProfileDialog(
            profiles = profiles,
            activeProfile = activeProfile,
            onSelectProfile = { activeProfile = it },
            onCreateProfile = { name ->
                val newProfile = ShortcutProfileV2(
                    id = "profile_${profiles.size}",
                    name = name,
                    shortcuts = shortcuts.toList()
                )
                profiles = profiles + newProfile
                activeProfile = newProfile.id
            },
            onDeleteProfile = { id ->
                profiles = profiles.filter { it.id != id }
                if (activeProfile == id) activeProfile = profiles.firstOrNull()?.id ?: "default"
            },
            onDuplicate = { id ->
                val source = profiles.find { it.id == id }
                if (source != null) {
                    val dup = source.copy(id = "profile_${profiles.size}", name = "${source.name} (Copy)")
                    profiles = profiles + dup
                }
            },
            onDismiss = { showProfileDialog = false },
            surfaceColors = surfaceColors,
            accent = accent
        )
    }

    // Import dialog
    if (showImportDialog) {
        ImportShortcutsDialog(
            onImport = { imported ->
                shortcuts = imported
                showImportDialog = false
            },
            onDismiss = { showImportDialog = false },
            surfaceColors = surfaceColors
        )
    }

    // Export dialog
    if (showExportDialog) {
        ExportShortcutsDialog(
            shortcuts = shortcuts,
            onDismiss = { showExportDialog = false },
            surfaceColors = surfaceColors
        )
    }
}

// ════════════════════════════════════════════
// SHORTCUT ROW
// ════════════════════════════════════════════

@Composable
private fun ShortcutRow(
    shortcut: ShortcutEntryV2,
    onRecord: () -> Unit,
    onReset: () -> Unit,
    onToggle: () -> Unit,
    hasConflict: Boolean,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    val sem = LocalKaiteyoSemanticColors.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onRecord).padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(shortcut.actionName, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = if (shortcut.isEnabled) surfaceColors.textPrimary else surfaceColors.textMuted)
            if (shortcut.description.isNotBlank()) {
                Text(shortcut.description, fontSize = 10.sp, color = surfaceColors.textMuted, maxLines = 1)
            }
        }

        // Conflict warning
        if (hasConflict) {
            Icon(Icons.Default.Warning, "Conflict", Modifier.size(16.dp), tint = sem.favorite)
            Spacer(Modifier.width(4.dp))
        }

        // Key display
        if (shortcut.primaryKey?.isDefined == true && shortcut.isEnabled) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .background(if (hasConflict) sem.favorite.copy(alpha = 0.15f) else accent.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(shortcut.primaryKey!!.displayText, fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = if (hasConflict) sem.favorite else accent.primary)
            }
        } else {
            Text("—", fontSize = 12.sp, color = surfaceColors.textMuted, modifier = Modifier.padding(horizontal = 8.dp))
        }

        Spacer(Modifier.width(4.dp))

        // Record button
        IconButton(onClick = onRecord, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Edit, "Record", Modifier.size(16.dp), tint = surfaceColors.textMuted)
        }

        // Toggle
        Switch(
            checked = shortcut.isEnabled,
            onCheckedChange = { onToggle() },
            modifier = Modifier.height(24.dp)
        )
    }
}

// ════════════════════════════════════════════
// PROFILE DIALOG
// ════════════════════════════════════════════

@Composable
private fun ProfileDialog(
    profiles: List<ShortcutProfileV2>,
    activeProfile: String,
    onSelectProfile: (String) -> Unit,
    onCreateProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDismiss: () -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    var showCreateField by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Shortcut Profiles") },
        text = {
            Column(modifier = Modifier.heightIn(max = 350.dp)) {
                if (showCreateField) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text("Profile name", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                        )
                        IconButton(onClick = {
                            if (newName.isNotBlank()) {
                                onCreateProfile(newName)
                                newName = ""
                                showCreateField = false
                            }
                        }) { Icon(Icons.Default.Check, null) }
                        IconButton(onClick = { showCreateField = false }) { Icon(Icons.Default.Close, null) }
                    }
                }

                LazyColumn {
                    items(profiles) { profile ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onSelectProfile(profile.id) }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = activeProfile == profile.id,
                                onClick = { onSelectProfile(profile.id) }
                            )
                            Spacer(Modifier.width(4.dp))
                            Column(Modifier.weight(1f)) {
                                Text(profile.name, fontSize = 14.sp, color = surfaceColors.textPrimary)
                                if (profile.isBuiltIn) {
                                    Text("Built-in", fontSize = 11.sp, color = surfaceColors.textMuted)
                                }
                            }
                            if (!profile.isBuiltIn) {
                                IconButton(onClick = { onDuplicate(profile.id) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.ContentCopy, "Duplicate", Modifier.size(16.dp), tint = surfaceColors.textMuted)
                                }
                                IconButton(onClick = { onDeleteProfile(profile.id) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, "Delete", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                if (!showCreateField) {
                    TextButton(onClick = { showCreateField = true }) {
                        Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New Profile")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

// ════════════════════════════════════════════
// IMPORT / EXPORT SHORTCUTS DIALOGS
// ════════════════════════════════════════════

@Composable
private fun ImportShortcutsDialog(
    onImport: (List<ShortcutEntryV2>) -> Unit,
    onDismiss: () -> Unit,
    surfaceColors: SurfaceColors
) {
    var jsonText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Shortcuts") },
        text = {
            Column {
                Text("Paste JSON shortcut configuration:", fontSize = 13.sp, color = surfaceColors.textMuted)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it; error = null },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                    placeholder = { Text("[{\\n  \\\"id\\\": \\\"showAnswer\\\",\\n  \\\"actionName\\\": \\\"Show Answer\\\",\\n  ...\\n}]") },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                )
                if (error != null) {
                    Text(error!!, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    // Parse JSON would go here
                    onImport(emptyList())
                } catch (e: Exception) {
                    error = "Invalid JSON format"
                }
            }, enabled = jsonText.isNotBlank()) { Text("Import") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ExportShortcutsDialog(
    shortcuts: List<ShortcutEntryV2>,
    onDismiss: () -> Unit,
    surfaceColors: SurfaceColors
) {
    val jsonPreview = remember(shortcuts) {
        shortcuts.joinToString(",\n  ") { s ->
            """{"id":"${s.id}","action":"${s.actionName}","key":"${s.primaryKey?.displayText ?: ""}","enabled":${s.isEnabled}}"""
        }.let { "[\n  $it\n]" }
    }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Shortcuts") },
        text = {
            Column {
                Text("JSON configuration (${shortcuts.size} shortcuts):", fontSize = 13.sp, color = surfaceColors.textMuted)
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(surfaceColors.surfaceInteractive)
                        .padding(8.dp).verticalScroll(rememberScrollState())
                ) {
                    Text(jsonPreview, fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = surfaceColors.textPrimary)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}
