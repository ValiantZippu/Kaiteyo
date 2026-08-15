@file:OptIn(ExperimentalMaterial3Api::class)

package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.common.ui.KaiteyoAlertDialog

// ════════════════════════════════════════════
// FLAGS SYSTEM — Full flag manager
// ════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlagManagerScreenFull(
    cards: List<KaiteyoCard> = emptyList(),
    onFlagCard: (String, CardFlagType) -> Unit = { _, _ -> },
    onBulkFlag: (List<String>, CardFlagType) -> Unit = { _, _ -> },
    onStudyByFlag: (CardFlagType) -> Unit = {},
    onClose: () -> Unit = {}
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var selectedFlag by remember { mutableStateOf<CardFlagType?>(null) }
    var showBulkDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf("grid") } // grid | list | stats

    // Compute stats per flag
    val flagStats = remember(cards) {
        CardFlagType.entries.filter { it != CardFlagType.None }.map { flag ->
            val flagged = cards.filter { it.flag == flag }
            FlagStatsV2(
                flagType = flag,
                totalCards = flagged.size,
                dueCards = flagged.count { c -> c.status == CardStatus.New || c.status == CardStatus.Learning || c.status == CardStatus.Relearning },
                newCards = flagged.count { it.status == CardStatus.New },
                averageEase = if (flagged.isNotEmpty()) flagged.map { it.ease }.average().toFloat() else 2.5f,
                averageAccuracy = if (flagged.isNotEmpty()) flagged.map { it.accuracy }.average().toFloat() else 0f,
                totalReviews = flagged.sumOf { it.reviewCount },
                totalLapses = flagged.sumOf { it.lapses },
                retentionRate = if (flagged.isNotEmpty()) flagged.map { it.accuracy }.average().toFloat() else 0f
            )
        }
    }

    // Filtered cards for selected flag
    val flaggedCards = remember(cards, selectedFlag, searchQuery) {
        if (selectedFlag == null) return@remember emptyList()
        var result = cards.filter { it.flag == selectedFlag }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            result = result.filter { it.character.lowercase().contains(q) || it.meaning.lowercase().contains(q) || it.deck.lowercase().contains(q) }
        }
        result
    }

    val totalFlagged = cards.count { it.flag != CardFlagType.None }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedFlag != null) "Flag: ${selectedFlag!!.displayName}" else "Flag Manager") },
                navigationIcon = { IconButton(onClick = if (selectedFlag != null) { { selectedFlag = null } } else onClose) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    if (selectedFlag == null) {
                        IconButton(onClick = { showBulkDialog = true }) { Icon(Icons.Default.Build, "Bulk Flag") }
                        IconButton(onClick = { viewMode = when(viewMode) { "grid" -> "list"; "list" -> "stats"; else -> "grid" } }) {
                            Icon(when (viewMode) { "grid" -> Icons.Default.ViewList; "list" -> Icons.Default.BarChart; else -> Icons.Default.GridView }, "Toggle View")
                        }
                    } else {
                        IconButton(onClick = { onStudyByFlag(selectedFlag!!) }) { Icon(Icons.Default.PlayArrow, "Study") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColors.surface,
                    titleContentColor = surfaceColors.textPrimary
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Stats bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("$totalFlagged flagged", fontSize = 12.sp, color = surfaceColors.textMuted)
                Text("${CardFlagType.entries.size - 1} colors", fontSize = 12.sp, color = surfaceColors.textMuted)
            }

            if (selectedFlag == null) {
                // Flag overview
                when (viewMode) {
                    "grid" -> FlagGrid(
                        flagStats = flagStats,
                        totalCards = cards.size,
                        onFlagClick = { selectedFlag = it.flagType },
                        surfaceColors = surfaceColors,
                        accent = accent
                    )
                    "list" -> FlagList(
                        flagStats = flagStats,
                        onFlagClick = { selectedFlag = it.flagType },
                        surfaceColors = surfaceColors,
                        accent = accent
                    )
                    "stats" -> FlagStatsView(
                        flagStats = flagStats,
                        surfaceColors = surfaceColors,
                        accent = accent
                    )
                }
            } else {
                // Cards with this flag
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        placeholder = { Text("Search flagged cards...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(flaggedCards, key = { it.id }) { card ->
                            FlaggedCardRow(
                                card = card,
                                onRemoveFlag = { onFlagCard(card.id, CardFlagType.None) },
                                onChangeFlag = { flag -> onFlagCard(card.id, flag) },
                                surfaceColors = surfaceColors,
                                accent = accent
                            )
                        }
                        if (flaggedCards.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No cards with this flag", color = surfaceColors.textMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBulkDialog) {
        FlagBulkDialog(
            cards = cards,
            onConfirm = { cardIds, flag -> onBulkFlag(cardIds, flag); showBulkDialog = false },
            onDismiss = { showBulkDialog = false }
        )
    }
}

// ── Flag Grid ──

@Composable
private fun FlagGrid(
    flagStats: List<FlagStatsV2>,
    totalCards: Int,
    onFlagClick: (FlagStatsV2) -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(flagStats) { stat ->
            val color = stat.flagType.colorFromHex()
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onFlagClick(stat) },
                colors = CardDefaults.cardColors(containerColor = surfaceColors.surfaceElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(16.dp).clip(CircleShape).background(color)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stat.flagType.displayName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                            color = surfaceColors.textPrimary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("${stat.totalCards} cards", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        color = surfaceColors.textPrimary)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${stat.dueCards} due", fontSize = 12.sp, color = accent.primary)
                        Text("${stat.newCards} new", fontSize = 12.sp, color = surfaceColors.textMuted)
                    }
                    if (stat.totalCards > 0) {
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { stat.averageAccuracy },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = color,
                            trackColor = surfaceColors.border.copy(alpha = 0.3f)
                        )
                        Text("${(stat.averageAccuracy * 100).toInt()}% accuracy", fontSize = 10.sp, color = surfaceColors.textMuted)
                    }
                }
            }
        }
    }
}

// ── Flag List ──

@Composable
private fun FlagList(
    flagStats: List<FlagStatsV2>,
    onFlagClick: (FlagStatsV2) -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(flagStats) { stat ->
            val color = stat.flagType.colorFromHex()
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .clickable { onFlagClick(stat) }
                    .background(surfaceColors.surfaceElevated)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stat.flagType.displayName, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = surfaceColors.textPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${stat.totalCards} cards", fontSize = 12.sp, color = surfaceColors.textMuted)
                        Text("${stat.dueCards} due", fontSize = 12.sp, color = accent.primary)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${(stat.averageAccuracy * 100).toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = surfaceColors.textPrimary)
                    Text("accuracy", fontSize = 10.sp, color = surfaceColors.textMuted)
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = surfaceColors.textMuted)
            }
        }
    }
}

// ── Flag Stats View ──

@Composable
private fun FlagStatsView(
    flagStats: List<FlagStatsV2>,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary cards
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard2("Total Flagged", "${flagStats.sumOf { it.totalCards }}", Icons.Default.Flag, Modifier.weight(1f))
            StatCard2("Avg Accuracy", "${(flagStats.filter { it.totalCards > 0 }.map { it.averageAccuracy }.average().toFloat() * 100).toInt()}%", Icons.Default.CheckCircle, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard2("Total Reviews", "${flagStats.sumOf { it.totalReviews }}", Icons.Default.History, Modifier.weight(1f))
            StatCard2("Total Lapses", "${flagStats.sumOf { it.totalLapses }}", Icons.Default.Error, Modifier.weight(1f))
        }

        // Per-flag breakdown
        Text("Per-Flag Breakdown", style = MaterialTheme.typography.titleSmall, color = surfaceColors.textPrimary)
        flagStats.forEach { stat ->
            val color = stat.flagType.colorFromHex()
            Card(colors = CardDefaults.cardColors(containerColor = surfaceColors.surfaceElevated)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
                        Spacer(Modifier.width(8.dp))
                        Text(stat.flagType.displayName, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = surfaceColors.textPrimary)
                        Spacer(Modifier.weight(1f))
                        Text("${stat.totalCards} cards", fontSize = 12.sp, color = surfaceColors.textMuted)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column { Text("Due", fontSize = 10.sp, color = surfaceColors.textMuted); Text("${stat.dueCards}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary) }
                        Column { Text("New", fontSize = 10.sp, color = surfaceColors.textMuted); Text("${stat.newCards}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary) }
                        Column { Text("Ease", fontSize = 10.sp, color = surfaceColors.textMuted); Text(formatFloat(stat.averageEase, 1), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary) }
                        Column { Text("Reviews", fontSize = 10.sp, color = surfaceColors.textMuted); Text("${stat.totalReviews}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary) }
                        Column { Text("Lapses", fontSize = 10.sp, color = surfaceColors.textMuted); Text("${stat.totalLapses}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary) }
                    }
                    if (stat.totalCards > 0) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { stat.averageAccuracy }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = color, trackColor = surfaceColors.border.copy(alpha = 0.3f))
                        Text("${(stat.averageAccuracy * 100).toInt()}% retention", fontSize = 10.sp, color = surfaceColors.textMuted)
                    }
                }
            }
        }
    }
}

// ── Flagged Card Row ──

@Composable
private fun FlaggedCardRow(
    card: KaiteyoCard,
    onRemoveFlag: () -> Unit,
    onChangeFlag: (CardFlagType) -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    var showFlagMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surfaceElevated),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(card.character, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(card.meaning, fontSize = 13.sp, color = surfaceColors.textPrimary, maxLines = 1)
                Text(card.deck, fontSize = 11.sp, color = surfaceColors.textMuted)
            }
            Text(card.status.displayName, fontSize = 11.sp, color = surfaceColors.textMuted,
                modifier = Modifier.padding(horizontal = 4.dp))
            Box {
                IconButton(onClick = { showFlagMenu = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Flag, null, Modifier.size(18.dp), tint = card.flag.colorFromHex())
                }
                DropdownMenu(expanded = showFlagMenu, onDismissRequest = { showFlagMenu = false }) {
                    DropdownMenuItem(text = { Text("Remove Flag") }, onClick = { showFlagMenu = false; onRemoveFlag() },
                        leadingIcon = { Icon(Icons.Default.Close, null, Modifier.size(18.dp)) })
                    HorizontalDivider()
                    CardFlagType.entries.filter { it != CardFlagType.None }.forEach { flag ->
                        DropdownMenuItem(
                            text = { Text(flag.displayName) },
                            onClick = { showFlagMenu = false; onChangeFlag(flag) },
                            leadingIcon = {
                                Box(Modifier.size(14.dp).clip(CircleShape).background(flag.colorFromHex()))
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── Flag Bulk Dialog ──

@Composable
private fun FlagBulkDialog(
    cards: List<KaiteyoCard>,
    onConfirm: (List<String>, CardFlagType) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFlag by remember { mutableStateOf(CardFlagType.Red) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCards = remember(cards, searchQuery) {
        if (searchQuery.isBlank()) cards
        else cards.filter { it.character.contains(searchQuery) || it.meaning.contains(searchQuery) }
    }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Flag Cards") },
        text = {
            Column(modifier = Modifier.heightIn(max = 450.dp)) {
                // Flag selector
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    CardFlagType.entries.filter { it != CardFlagType.None }.forEach { flag ->
                        IconButton(
                            onClick = { selectedFlag = flag },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(20.dp).clip(CircleShape)
                                    .background(flag.colorFromHex())
                                    .then(if (selectedFlag == flag) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter cards...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("${selectedIds.size} of ${filteredCards.size} selected", fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredCards, key = { it.id }) { card ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedIds = if (card.id in selectedIds) selectedIds - card.id else selectedIds + card.id
                            }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = card.id in selectedIds, onCheckedChange = {
                                selectedIds = if (card.id in selectedIds) selectedIds - card.id else selectedIds + card.id
                            })
                            Text(card.character, fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(card.meaning, fontSize = 12.sp, maxLines = 1)
                                Text(card.deck, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedIds.toList(), selectedFlag) }, enabled = selectedIds.isNotEmpty()) {
                Text("Flag ${selectedIds.size} cards as ${selectedFlag.displayName}")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
