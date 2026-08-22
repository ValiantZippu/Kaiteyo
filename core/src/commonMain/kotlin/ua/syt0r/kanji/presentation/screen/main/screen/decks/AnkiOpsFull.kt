@file:OptIn(ExperimentalMaterial3Api::class)

package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoSemanticColors
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.common.ui.KaiteyoAlertDialog

// ============================================
// KAITEYO v1.2 — FULL ANKI OPERATIONS
// All SRS operations: suspend, bury, forget,
// reset, reposition, change due, set interval,
// filtered decks, cram mode, preview mode,
// study by tag/flag/deck/JLPT/frequency
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnkiOperationsFullScreen(
    cards: List<KaiteyoCard>,
    onOperation: (CardOperation, List<KaiteyoCard>) -> Unit = { _, _ -> },
    onClose: () -> Unit = {}
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    var selectedTab by remember { mutableStateOf("operations") } // operations | filtered | cram | preview | study
    var selectedCards by remember { mutableStateOf(cards.take(5)) }
    var showFilteredDeckDialog by remember { mutableStateOf(false) }
    var showCramDialog by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var showStudyByDialog by remember { mutableStateOf(false) }
    var showOperationResult by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedCategory by remember { mutableStateOf<Set<String>>(setOf("basic")) }

    // Group operations by category
    val operationsByCategory = remember {
        mapOf(
            "basic" to listOf(
                CardOperation.SuspendCard, CardOperation.SuspendNote,
                CardOperation.BuryCard, CardOperation.BuryNote, CardOperation.BurySiblings
            ),
            "progress" to listOf(
                CardOperation.ForgetCard, CardOperation.ResetProgress
            ),
            "schedule" to listOf(
                CardOperation.Reposition, CardOperation.ChangeDueDate, CardOperation.SetInterval
            ),
            "study" to listOf(
                CardOperation.PreviewMode, CardOperation.CramMode
            )
        )
    }

    val categoryLabels = mapOf(
        "basic" to "Hide / Show Cards",
        "progress" to "Reset Progress",
        "schedule" to "Change Schedule",
        "study" to "Study Modes"
    )

    val filteredCards = remember(cards, searchQuery) {
        if (searchQuery.isBlank()) cards
        else cards.filter { it.character.contains(searchQuery) || it.meaning.contains(searchQuery) || it.deck.contains(searchQuery) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Anki Operations") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    TextButton(onClick = { showStudyByDialog = true }) { Text("Study By", fontSize = 12.sp) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColors.surface,
                    titleContentColor = surfaceColors.textPrimary
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Tabs
            TabRow(
                selectedTabIndex = when(selectedTab) { "operations" -> 0; "filtered" -> 1; "cram" -> 2; "preview" -> 3; else -> 0 },
                containerColor = surfaceColors.surface,
                contentColor = accent.primary
            ) {
                Tab(selected = selectedTab == "operations", onClick = { selectedTab = "operations" }) { Text("Operations", fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp)) }
                Tab(selected = selectedTab == "filtered", onClick = { selectedTab = "filtered" }) { Text("Filtered", fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp)) }
                Tab(selected = selectedTab == "cram", onClick = { selectedTab = "cram" }) { Text("Cram", fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp)) }
                Tab(selected = selectedTab == "preview", onClick = { selectedTab = "preview" }) { Text("Preview", fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp)) }
            }

            when (selectedTab) {
                "operations" -> OperationsTab(
                    operationsByCategory = operationsByCategory,
                    categoryLabels = categoryLabels,
                    expandedCategory = expandedCategory,
                    onToggleCategory = { cat ->
                        expandedCategory = if (cat in expandedCategory) expandedCategory - cat
                        else expandedCategory + cat
                    },
                    selectedCards = selectedCards,
                    filteredCards = filteredCards,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onToggleCardSelection = { card ->
                        selectedCards = if (card in selectedCards) selectedCards - card
                        else selectedCards + card
                    },
                    onOperation = { op -> onOperation(op, selectedCards); showOperationResult = "${op.displayName} applied to ${selectedCards.size} cards" },
                    surfaceColors = surfaceColors,
                    accent = accent
                )
                "filtered" -> FilteredDecksTab(
                    onStart = { config -> showOperationResult = "Started filtered deck: ${config.name}" },
                    surfaceColors = surfaceColors,
                    accent = accent
                )
                "cram" -> CramTab(
                    onStart = { config -> showOperationResult = "Started cram session: ${config.name}" },
                    surfaceColors = surfaceColors,
                    accent = accent
                )
                "preview" -> PreviewTab(
                    cards = cards,
                    surfaceColors = surfaceColors,
                    accent = accent
                )
            }
        }
    }

    // Operation result toast
    showOperationResult?.let { msg ->
        KaiteyoAlertDialog(
            onDismissRequest = { showOperationResult = null },
            title = { Text("Operation Complete") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { showOperationResult = null }) { Text("OK") } }
        )
    }

    // Study By dialog
    if (showStudyByDialog) {
        StudyByDialog(
            cards = cards,
            onStudyByTag = { tag -> showOperationResult = "Studying tag: $tag"; showStudyByDialog = false },
            onStudyByFlag = { flag -> showOperationResult = "Studying flag: ${flag.displayName}"; showStudyByDialog = false },
            onStudyByDeck = { deck -> showOperationResult = "Studying deck: $deck"; showStudyByDialog = false },
            onStudyByJLPT = { level -> showOperationResult = "Studying JLPT N$level"; showStudyByDialog = false },
            onStudyByFrequency = { range -> showOperationResult = "Studying frequency: $range"; showStudyByDialog = false },
            onDismiss = { showStudyByDialog = false },
            surfaceColors = surfaceColors,
            accent = accent
        )
    }
}

// ════════════════════════════════════════════
// OPERATIONS TAB
// ════════════════════════════════════════════

@Composable
private fun OperationsTab(
    operationsByCategory: Map<String, List<CardOperation>>,
    categoryLabels: Map<String, String>,
    expandedCategory: Set<String>,
    onToggleCategory: (String) -> Unit,
    selectedCards: List<KaiteyoCard>,
    filteredCards: List<KaiteyoCard>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleCardSelection: (KaiteyoCard) -> Unit,
    onOperation: (CardOperation) -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    Column(Modifier.fillMaxSize()) {
        // Card selector
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColors.surfaceElevated),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("Selected Cards", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = surfaceColors.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text("${selectedCards.size} of ${filteredCards.size} cards selected", fontSize = 12.sp, color = surfaceColors.textMuted)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    placeholder = { Text("Filter cards...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 150.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(filteredCards, key = { it.id }) { card ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onToggleCardSelection(card) }.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = card in selectedCards, onCheckedChange = { onToggleCardSelection(card) }, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(card.character, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text(card.meaning, fontSize = 11.sp, color = surfaceColors.textMuted, maxLines = 1)
                            Spacer(Modifier.weight(1f))
                            Text(card.deck, fontSize = 10.sp, color = surfaceColors.textMuted)
                        }
                    }
                }
            }
        }

        // Operations grouped by category
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            operationsByCategory.forEach { (category, ops) ->
                item {
                    CategoryHeader(
                        label = categoryLabels[category] ?: category,
                        isExpanded = category in expandedCategory,
                        onToggle = { onToggleCategory(category) },
                        surfaceColors = surfaceColors,
                        accent = accent
                    )
                }
                if (category in expandedCategory) {
                    items(ops) { op ->
                        OperationItem(
                            operation = op,
                            enabled = selectedCards.isNotEmpty(),
                            onClick = { onOperation(op) },
                            surfaceColors = surfaceColors,
                            accent = accent
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════
// OPERATION ITEM
// ════════════════════════════════════════════

@Composable
private fun OperationItem(
    operation: CardOperation,
    enabled: Boolean,
    onClick: () -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) surfaceColors.surfaceElevated else surfaceColors.surfaceElevated.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (enabled) accent.primary.copy(alpha = 0.1f) else surfaceColors.border.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                operation.icon()
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(operation.displayName, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = if (enabled) surfaceColors.textPrimary else surfaceColors.textMuted)
                Text(operation.description, fontSize = 11.sp, color = surfaceColors.textMuted, maxLines = 1)
            }
            if (!enabled) {
                Text("Select cards", fontSize = 10.sp, color = surfaceColors.textMuted)
            } else {
                Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = surfaceColors.textMuted)
            }
        }
    }
}

// ════════════════════════════════════════════
// CATEGORY HEADER
// ════════════════════════════════════════════

@Composable
private fun CategoryHeader(
    label: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
            null, Modifier.size(18.dp), tint = surfaceColors.textMuted
        )
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = surfaceColors.textMuted)
    }
}

// ════════════════════════════════════════════
// FILTERED DECKS TAB
// ════════════════════════════════════════════

@Composable
private fun FilteredDecksTab(
    onStart: (FilteredDeckConfig) -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    var config by remember { mutableStateOf(FilteredDeckConfig()) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Create a filtered deck to study specific cards", fontSize = 13.sp, color = surfaceColors.textMuted)

        OutlinedTextField(
            value = config.name,
            onValueChange = { config = config.copy(name = it) },
            label = { Text("Deck Name") },
            placeholder = { Text("e.g., My Custom Study") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.searchQuery,
            onValueChange = { config = config.copy(searchQuery = it) },
            label = { Text("Search Query") },
            placeholder = { Text("e.g., tag:jlpt-n5 is:due flag:red") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Max cards slider
        Text("Max Cards: ${config.maxCards}", fontSize = 13.sp, color = surfaceColors.textPrimary)
        Slider(
            value = config.maxCards.toFloat(),
            onValueChange = { config = config.copy(maxCards = it.toInt()) },
            valueRange = 10f..9999f,
            modifier = Modifier.fillMaxWidth()
        )

        // Order
        Text("Order", style = MaterialTheme.typography.titleSmall, color = surfaceColors.textPrimary)
        var expandedOrder by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expandedOrder, onExpandedChange = { expandedOrder = it }) {
            OutlinedTextField(
                value = config.order.displayName,
                onValueChange = {},
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

        // Options
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = config.reschedule, onCheckedChange = { config = config.copy(reschedule = it) })
            Text("Reschedule cards after study", fontSize = 13.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = config.previewBeforeFilter, onCheckedChange = { config = config.copy(previewBeforeFilter = it) })
            Text("Preview before filtering", fontSize = 13.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = config.startNow, onCheckedChange = { config = config.copy(startNow = it) })
            Text("Start immediately", fontSize = 13.sp)
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onStart(config) },
            modifier = Modifier.fillMaxWidth(),
            enabled = config.name.isNotBlank()
        ) {
            Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Start Filtered Deck")
        }
    }
}

// ════════════════════════════════════════════
// CRAM TAB
// ════════════════════════════════════════════

@Composable
private fun CramTab(
    onStart: (CramConfigV2) -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    var config by remember { mutableStateOf(CramConfigV2()) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Cram mode for rapid review of specific cards", fontSize = 13.sp, color = surfaceColors.textMuted)

        OutlinedTextField(
            value = config.name,
            onValueChange = { config = config.copy(name = it) },
            label = { Text("Session Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.sourceDeck,
            onValueChange = { config = config.copy(sourceDeck = it) },
            label = { Text("Source Deck") },
            placeholder = { Text("Leave empty for all decks") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Card limit
        Text("Card Limit: ${config.cardLimit}", fontSize = 13.sp)
        Slider(
            value = config.cardLimit.toFloat(),
            onValueChange = { config = config.copy(cardLimit = it.toInt()) },
            valueRange = 5f..500f,
            modifier = Modifier.fillMaxWidth()
        )

        // Order
        Text("Order", style = MaterialTheme.typography.titleSmall)
        var expandedOrder by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expandedOrder, onExpandedChange = { expandedOrder = it }) {
            OutlinedTextField(
                value = config.orderType.displayName,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedOrder) },
                singleLine = true
            )
            ExposedDropdownMenu(expanded = expandedOrder, onDismissRequest = { expandedOrder = false }) {
                CramOrder.entries.forEach { order ->
                    DropdownMenuItem(
                        text = { Text(order.displayName) },
                        onClick = { config = config.copy(orderType = order); expandedOrder = false }
                    )
                }
            }
        }

        // Options
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = config.showBothSides, onCheckedChange = { config = config.copy(showBothSides = it) })
            Text("Show both sides", fontSize = 13.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = config.autoAdvance, onCheckedChange = { config = config.copy(autoAdvance = it) })
            Text("Auto-advance", fontSize = 13.sp)
        }
        if (config.autoAdvance) {
            Text("Delay: ${config.autoAdvanceDelayMs / 1000}s", fontSize = 12.sp, color = surfaceColors.textMuted)
            Slider(
                value = (config.autoAdvanceDelayMs / 1000).toFloat(),
                onValueChange = { config = config.copy(autoAdvanceDelayMs = (it * 1000).toLong()) },
                valueRange = 1f..10f
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = config.repeatIncorrect, onCheckedChange = { config = config.copy(repeatIncorrect = it) })
            Text("Repeat incorrect cards", fontSize = 13.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = config.shuffleCards, onCheckedChange = { config = config.copy(shuffleCards = it) })
            Text("Shuffle cards", fontSize = 13.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = config.includeSuspended, onCheckedChange = { config = config.copy(includeSuspended = it) })
            Text("Include suspended cards", fontSize = 13.sp)
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onStart(config) },
            modifier = Modifier.fillMaxWidth(),
            enabled = config.name.isNotBlank()
        ) {
            Icon(Icons.Default.Bolt, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Start Cram Session")
        }
    }
}

// ════════════════════════════════════════════
// PREVIEW TAB
// ════════════════════════════════════════════

@Composable
private fun PreviewTab(
    cards: List<KaiteyoCard>,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    var currentIndex by remember { mutableStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }
    var isReversed by remember { mutableStateOf(false) }
    var autoAdvance by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCards = remember(cards, searchQuery) {
        if (searchQuery.isBlank()) cards
        else cards.filter { it.character.contains(searchQuery) || it.meaning.contains(searchQuery) }
    }

    if (filteredCards.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No cards to preview", color = surfaceColors.textMuted)
        }
        return
    }

    val currentCard = filteredCards.getOrNull(currentIndex)

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Card ${currentIndex + 1} of ${filteredCards.size}", fontSize = 13.sp, color = surfaceColors.textMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(selected = isReversed, onClick = { isReversed = !isReversed }, label = { Text("Reversed", fontSize = 11.sp) }, modifier = Modifier.height(28.dp))
                FilterChip(selected = autoAdvance, onClick = { autoAdvance = !autoAdvance }, label = { Text("Auto", fontSize = 11.sp) }, modifier = Modifier.height(28.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        // Card preview
        if (currentCard != null) {
            Card(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = surfaceColors.surfaceElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (!showAnswer || isReversed) {
                            Text(currentCard.character, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                            Spacer(Modifier.height(8.dp))
                            Text(currentCard.reading, fontSize = 16.sp, color = surfaceColors.textMuted)
                        }
                        if (showAnswer || isReversed) {
                            if (!showAnswer) Spacer(Modifier.height(16.dp))
                            Text(currentCard.meaning, fontSize = 20.sp, color = surfaceColors.textPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text(currentCard.deck, fontSize = 12.sp, color = surfaceColors.textMuted)
                            if (currentCard.tagNames.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    currentCard.tagNames.forEach { tag ->
                                        Box(
                                            modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                                .background(accent.primary.copy(alpha = 0.1f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) { Text(tag, fontSize = 10.sp, color = accent.primary) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showAnswer = !showAnswer },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(if (showAnswer) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (showAnswer) "Hide Answer" else "Show Answer", fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { currentIndex = (currentIndex - 1).coerceAtLeast(0); showAnswer = false },
                    enabled = currentIndex > 0
                ) {
                    Icon(Icons.Default.ChevronLeft, null, Modifier.size(18.dp))
                    Text("Previous", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { currentIndex = (currentIndex + 1).coerceAtMost(filteredCards.size - 1); showAnswer = false },
                    enabled = currentIndex < filteredCards.size - 1
                ) {
                    Text("Next", fontSize = 12.sp)
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp))
                }
            }
        }
    }
}

// ════════════════════════════════════════════
// STUDY BY DIALOG
// ════════════════════════════════════════════

@Composable
private fun StudyByDialog(
    cards: List<KaiteyoCard>,
    onStudyByTag: (String) -> Unit,
    onStudyByFlag: (CardFlagType) -> Unit,
    onStudyByDeck: (String) -> Unit,
    onStudyByJLPT: (Int) -> Unit,
    onStudyByFrequency: (String) -> Unit,
    onDismiss: () -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    var studyType by remember { mutableStateOf("tag") }

    val uniqueTags = remember(cards) { cards.flatMap { it.tagNames }.distinct().sorted() }
    val uniqueDecks = remember(cards) { cards.map { it.deck }.distinct().sorted() }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Study By...") },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Study type selector
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("tag" to "Tag", "flag" to "Flag", "deck" to "Deck", "jlpt" to "JLPT", "freq" to "Frequency").forEach { (key, label) ->
                        FilterChip(
                            selected = studyType == key,
                            onClick = { studyType = key },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                HorizontalDivider()

                when (studyType) {
                    "tag" -> {
                        Text("Select a tag to study", fontSize = 13.sp, color = surfaceColors.textMuted)
                        LazyColumn {
                            items(uniqueTags) { tag ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { onStudyByTag(tag) }.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Label, null, Modifier.size(16.dp), tint = accent.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(tag, fontSize = 14.sp, color = surfaceColors.textPrimary)
                                    Spacer(Modifier.weight(1f))
                                    Text("${cards.count { it.tagNames.contains(tag) }} cards", fontSize = 12.sp, color = surfaceColors.textMuted)
                                    Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = surfaceColors.textMuted)
                                }
                            }
                        }
                    }
                    "flag" -> {
                        Column {
                            CardFlagType.entries.filter { it != CardFlagType.None }.forEach { flag ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { onStudyByFlag(flag) }.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(14.dp).clip(CircleShape).background(flag.colorFromHex()))
                                    Spacer(Modifier.width(8.dp))
                                    Text(flag.displayName, fontSize = 14.sp, color = surfaceColors.textPrimary)
                                    Spacer(Modifier.weight(1f))
                                    Text("${cards.count { it.flag == flag }} cards", fontSize = 12.sp, color = surfaceColors.textMuted)
                                    Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = surfaceColors.textMuted)
                                }
                            }
                        }
                    }
                    "deck" -> {
                        LazyColumn {
                            items(uniqueDecks) { deck ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { onStudyByDeck(deck) }.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Folder, null, Modifier.size(16.dp), tint = surfaceColors.textMuted)
                                    Spacer(Modifier.width(8.dp))
                                    Text(deck, fontSize = 14.sp, color = surfaceColors.textPrimary)
                                    Spacer(Modifier.weight(1f))
                                    Text("${cards.count { it.deck == deck }} cards", fontSize = 12.sp, color = surfaceColors.textMuted)
                                    Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = surfaceColors.textMuted)
                                }
                            }
                        }
                    }
                    "jlpt" -> {
                        Column {
                            (1..5).forEach { level ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { onStudyByJLPT(level) }.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val sem = LocalKaiteyoSemanticColors.current
                                    Text("N$level", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                        color = when (level) { 1 -> sem.error; 2 -> sem.warning; 3 -> sem.favorite; 4 -> sem.success; else -> sem.info })
                                    Spacer(Modifier.width(8.dp))
                                    Text("JLPT N$level", fontSize = 14.sp, color = surfaceColors.textPrimary)
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = surfaceColors.textMuted)
                                }
                            }
                        }
                    }
                    "frequency" -> {
                        Column {
                            listOf("common" to "Common (>=500)", "medium" to "Medium (100-500)", "rare" to "Rare (<100)").forEach { (key, label) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { onStudyByFrequency(key) }.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.TrendingUp, null, Modifier.size(16.dp), tint = surfaceColors.textMuted)
                                    Spacer(Modifier.width(8.dp))
                                    Text(label, fontSize = 14.sp, color = surfaceColors.textPrimary)
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = surfaceColors.textMuted)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
