package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.ui.KaiteyoAlertDialog
import kotlin.math.max
import kotlin.math.min

// ============================================
// FULL ANKI CARD OPERATIONS
// Suspend, Bury, Forget, Reposition, Reschedule,
// Filtered Decks, Custom Study, Preview, Cram
// ============================================

/** All card operations that can be performed */
enum class CardOperation(val displayName: String, val description: String, val icon: @Composable () -> Unit) {
    SuspendCard("Suspend Card", "Hide card until manually unsuspended",
        { Icon(Icons.Default.Block, null) }),
    SuspendNote("Suspend Note", "Hide all cards for this note",
        { Icon(Icons.Default.Block, null) }),
    BuryCard("Bury Card", "Hide card until next day",
        { Icon(Icons.Default.VisibilityOff, null) }),
    BuryNote("Bury Note", "Hide all cards for this note until next day",
        { Icon(Icons.Default.VisibilityOff, null) }),
    BurySiblings("Bury Siblings", "Hide other cards for this note",
        { Icon(Icons.Default.VisibilityOff, null) }),
    ForgetCard("Forget Card", "Reset card to new state",
        { Icon(Icons.Default.RestartAlt, null) }),
    ResetProgress("Reset Progress", "Clear all review data for this card",
        { Icon(Icons.Default.DeleteSweep, null) }),
    Reposition("Reposition", "Change card position in deck",
        { Icon(Icons.Default.SwapVert, null) }),
    ChangeDueDate("Change Due Date", "Set a custom due date",
        { Icon(Icons.Default.DateRange, null) }),
    SetInterval("Set Interval", "Manually set review interval",
        { Icon(Icons.Default.Timer, null) }),
    PreviewMode("Preview Mode", "Browse cards without affecting schedule",
        { Icon(Icons.Default.Visibility, null) }),
    CramMode("Cram Mode", "Study cards rapidly for exam prep",
        { Icon(Icons.Default.Bolt, null) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardOperationsScreen(
    selectedCards: List<KaiteyoCard> = emptyList(),
    onOperation: (CardOperation, List<KaiteyoCard>) -> Unit = { _, _ -> },
    onClose: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card Operations") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("Operations (${selectedCards.size} selected)",
                    style = MaterialTheme.typography.titleSmall,
                    color = surfaceColors.textMuted,
                    modifier = Modifier.padding(bottom = 4.dp))
            }

            items(CardOperation.entries) { op ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = surfaceColors.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onOperation(op, selectedCards) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accent.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) { op.icon() }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(op.displayName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(op.description, fontSize = 12.sp, color = surfaceColors.textMuted)
                        }
                        Icon(Icons.Default.KeyboardArrowRight, null, Modifier.size(20.dp),
                            tint = surfaceColors.textMuted)
                    }
                }
            }
        }
    }
}

// ============================================
// FILTERED DECKS / CUSTOM STUDY
// ============================================

data class FilteredDeckConfig(
    val name: String = "",
    val searchQuery: String = "",
    val maxCards: Int = 100,
    val order: FilteredDeckOrder = FilteredDeckOrder.DueFirst,
    val reschedule: Boolean = true,
    val previewBeforeFilter: Boolean = false,
    val startNow: Boolean = true
)

enum class FilteredDeckOrder(val displayName: String) {
    DueFirst("Due First"),
    NewFirst("New First"),
    ReviewFirst("Review First"),
    LowestEase("Lowest Ease First"),
    HighestEase("Highest Ease First"),
    Random("Random"),
    MostLapses("Most Lapses First"),
    ByDeck("Group by Deck")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilteredDeckDialog(
    onStart: (FilteredDeckConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var config by remember { mutableStateOf(FilteredDeckConfig()) }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Study / Filtered Deck") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = config.name,
                    onValueChange = { config = config.copy(name = it) },
                    label = { Text("Deck Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = config.searchQuery,
                    onValueChange = { config = config.copy(searchQuery = it) },
                    label = { Text("Search Query (optional)") },
                    placeholder = { Text("deck:current is:due tag:jlpt-n5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Max Cards:", Modifier.weight(1f), fontSize = 14.sp)
                    Slider(
                        value = config.maxCards.toFloat(),
                        onValueChange = { config = config.copy(maxCards = it.toInt()) },
                        valueRange = 10f..9999f,
                        modifier = Modifier.weight(2f)
                    )
                    Text("${config.maxCards}", fontSize = 12.sp, modifier = Modifier.width(40.dp))
                }

                // Order
                Text("Order", style = MaterialTheme.typography.titleSmall)
                var expandedOrder by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expandedOrder, onExpandedChange = { expandedOrder = it }) {
                    OutlinedTextField(
                        value = config.order.displayName,
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedOrder) },
                        singleLine = true
                    )
                    ExposedDropdownMenu(expanded = expandedOrder, onDismissRequest = { expandedOrder = false }) {
                        FilteredDeckOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.displayName) },
                                onClick = { config = config.copy(order = order); expandedOrder = false }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = config.reschedule, onCheckedChange = { config = config.copy(reschedule = it) })
                    Spacer(Modifier.width(8.dp))
                    Text("Reschedule cards based on review", fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = config.previewBeforeFilter, onCheckedChange = { config = config.copy(previewBeforeFilter = it) })
                    Spacer(Modifier.width(8.dp))
                    Text("Preview before filtering", fontSize = 13.sp)
                }
            }
        },
        confirmButton = { Button(onClick = { onStart(config); onDismiss() }) { Text("Start Studying") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ============================================
// STUDY MODES
// ============================================

enum class StudyMode(val displayName: String, val description: String) {
    Normal("Normal", "Standard SRS review"),
    Preview("Preview", "Browse without affecting schedule"),
    Cram("Cram", "Rapid-fire review for exam prep"),
    ReviewAhead("Review Ahead", "Study cards due in the future"),
    ReviewForgotten("Review Forgotten", "Re-study cards you've forgotten"),
    StudyTagged("Study Tagged", "Review cards with specific tags"),
    StudyFlagged("Study Flagged", "Review flagged cards only"),
    StudyByDeck("Study by Deck", "Focus on a specific deck"),
    StudyByJLPT("Study by JLPT", "Review cards by JLPT level"),
    StudyByFrequency("Study by Frequency", "Study based on kanji frequency"),
    Custom("Custom Study", "User-defined study session")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyModeSelector(
    currentMode: StudyMode = StudyMode.Normal,
    onSelectMode: (StudyMode) -> Unit,
    onDismiss: () -> Unit
) {
    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Study Mode") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StudyMode.entries.forEach { mode ->
                    val isSelected = mode == currentMode
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                            .clickable { onSelectMode(mode) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(mode.displayName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(mode.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ============================================
// CRAM MODE CONFIGURATION
// ============================================

data class CramConfig(
    val cardCount: Int = 50,
    val showBothSides: Boolean = true,
    val autoAdvance: Boolean = false,
    val autoAdvanceDelay: Int = 3,
    val randomOrder: Boolean = true,
    val repeatMistakes: Boolean = true,
    val maxMistakesBeforeSkip: Int = 3
)

@Composable
fun CramModeDialog(
    onStart: (CramConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var config by remember { mutableStateOf(CramConfig()) }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cram Mode Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Cards:", Modifier.weight(1f), fontSize = 14.sp)
                    Slider(
                        value = config.cardCount.toFloat(),
                        onValueChange = { config = config.copy(cardCount = it.toInt()) },
                        valueRange = 10f..500f,
                        modifier = Modifier.weight(2f)
                    )
                    Text("${config.cardCount}", fontSize = 12.sp, modifier = Modifier.width(40.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = config.showBothSides, onCheckedChange = { config = config.copy(showBothSides = it) })
                    Spacer(Modifier.width(8.dp))
                    Text("Show both sides", fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = config.autoAdvance, onCheckedChange = { config = config.copy(autoAdvance = it) })
                    Spacer(Modifier.width(8.dp))
                    Text("Auto-advance", fontSize = 13.sp)
                }
                if (config.autoAdvance) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Delay:", Modifier.weight(1f), fontSize = 13.sp)
                        Slider(
                            value = config.autoAdvanceDelay.toFloat(),
                            onValueChange = { config = config.copy(autoAdvanceDelay = it.toInt()) },
                            valueRange = 1f..10f,
                            modifier = Modifier.weight(2f)
                        )
                        Text("${config.autoAdvanceDelay}s", fontSize = 12.sp, modifier = Modifier.width(40.dp))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = config.randomOrder, onCheckedChange = { config = config.copy(randomOrder = it) })
                    Spacer(Modifier.width(8.dp))
                    Text("Random order", fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = config.repeatMistakes, onCheckedChange = { config = config.copy(repeatMistakes = it) })
                    Spacer(Modifier.width(8.dp))
                    Text("Repeat mistakes", fontSize = 13.sp)
                }
            }
        },
        confirmButton = { Button(onClick = { onStart(config); onDismiss() }) { Text("Start Cram") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ============================================
// PREVIEW MODE
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewModeScreen(
    cards: List<KaiteyoCard> = emptyList(),
    onClose: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    var currentIndex by remember { mutableStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    Text("${currentIndex + 1}/${cards.size}", fontSize = 13.sp,
                        color = surfaceColors.textMuted)
                }
            )
        }
    ) { padding ->
        if (cards.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No cards to preview", color = surfaceColors.textMuted)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                val card = cards.getOrNull(currentIndex)

                if (card != null) {
                    Card(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = surfaceColors.surface)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Front
                            Text(card.character, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Text(card.reading, fontSize = 20.sp, color = surfaceColors.textMuted)

                            if (showAnswer) {
                                Spacer(Modifier.height(24.dp))
                                HorizontalDivider()
                                Spacer(Modifier.height(24.dp))
                                Text(card.meaning, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(8.dp))
                                // Additional card info
                                Column {
                                    Row {
                                        Text("Deck: ", fontSize = 13.sp, color = surfaceColors.textMuted)
                                        Text(card.deck, fontSize = 13.sp)
                                    }
                                    Row {
                                        Text("Status: ", fontSize = 13.sp, color = surfaceColors.textMuted)
                                        StatusBadge(status = card.status)
                                    }
                                    Row {
                                        Text("Flag: ", fontSize = 13.sp, color = surfaceColors.textMuted)
                                        Text(card.flag.displayName, fontSize = 13.sp)
                                    }
                                    card.tags.forEach { tag ->
                                        Row {
                                            Text("Tag: ", fontSize = 13.sp, color = surfaceColors.textMuted)
                                            Text(tag.name, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = { currentIndex = max(0, currentIndex - 1); showAnswer = false },
                            enabled = currentIndex > 0
                        ) { Text("Previous") }

                        Button(
                            onClick = { showAnswer = !showAnswer }
                        ) { Text(if (showAnswer) "Hide Answer" else "Show Answer") }

                        OutlinedButton(
                            onClick = { currentIndex = min(cards.size - 1, currentIndex + 1); showAnswer = false },
                            enabled = currentIndex < cards.size - 1
                        ) { Text("Next") }
                    }

                    // Keyboard hints
                    Text("Use ← → arrow keys to navigate, Space to flip",
                        fontSize = 11.sp, color = surfaceColors.textMuted,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
                }
            }
        }
    }
}

// ============================================
// REPOSITION / RESCHEDULE DIALOG
// ============================================

@Composable
fun RepositionDialog(
    onConfirm: (newPosition: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var position by remember { mutableStateOf("0") }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reposition Cards") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Set the starting position for the selected cards.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = position,
                    onValueChange = { if (it.all { c -> c.isDigit() }) position = it },
                    label = { Text("New Position") },
                    singleLine = true
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(position.toIntOrNull() ?: 0) }) { Text("Reposition") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun RescheduleDialog(
    onConfirm: (newInterval: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var interval by remember { mutableStateOf("21") }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Interval") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Set a new review interval for the selected cards (in days).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = interval,
                    onValueChange = { if (it.all { c -> c.isDigit() }) interval = it },
                    label = { Text("Interval (days)") },
                    singleLine = true
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(interval.toIntOrNull() ?: 21) }) { Text("Set Interval") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ChangeDueDateDialog(
    onConfirm: (daysFromNow: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var days by remember { mutableStateOf("0") }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Due Date") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Set the due date relative to today. Positive = future, negative = overdue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = days,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("-?\\d+"))) days = it },
                    label = { Text("Days from now") },
                    singleLine = true
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(days.toIntOrNull() ?: 0) }) { Text("Set Due Date") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
