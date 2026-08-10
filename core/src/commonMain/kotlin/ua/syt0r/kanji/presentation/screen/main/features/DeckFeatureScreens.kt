@file:OptIn(ExperimentalMaterial3Api::class)

package ua.syt0r.kanji.presentation.screen.main.features

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.screen.main.screen.decks.AnkiOperationsFullScreen
import ua.syt0r.kanji.presentation.screen.main.screen.decks.BackupManagerScreen
import ua.syt0r.kanji.presentation.screen.main.screen.decks.BulkActionsFullScreen
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardBrowserFullScreen
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardFlagType
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardManager
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardStatus
import ua.syt0r.kanji.presentation.screen.main.screen.decks.DeckBrowserFullScreen
import ua.syt0r.kanji.presentation.screen.main.screen.decks.FlagManagerScreenFull
import ua.syt0r.kanji.presentation.screen.main.screen.decks.HistoryFullScreen
import ua.syt0r.kanji.presentation.screen.main.screen.decks.ImportExportScreen
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KaiteyoCard
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KaiteyoDeck
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KeyboardShortcutsPage
import ua.syt0r.kanji.presentation.screen.main.screen.decks.NoteEditorFullScreen
import ua.syt0r.kanji.presentation.screen.main.screen.decks.ReviewSettingsFullScreen
import ua.syt0r.kanji.presentation.screen.main.screen.decks.SearchEngineScreen
import ua.syt0r.kanji.presentation.screen.main.screen.decks.StatisticsDashboardV2
import ua.syt0r.kanji.presentation.screen.main.screen.decks.TagManagerScreenFull

val LocalDeckFeaturesController = staticCompositionLocalOf<DeckFeaturesController?> { null }

@Composable
fun DeckFeaturesProvider(
    controller: DeckFeaturesController,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalDeckFeaturesController provides controller) { content() }
}

@Composable
private fun deckController(): DeckFeaturesController =
    LocalDeckFeaturesController.current ?: error("DeckFeaturesController is not provided")

// ============================================
// ROUTES — real database-backed wrappers
// ============================================

@Composable
fun TagManagerRoute(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    TagManagerScreenFull(
        tags = controller.tags,
        cards = controller.cards,
        onAddTag = { name, color, parent -> scope.launch { controller.createTag(name, color, parent) } },
        onUpdateTag = { id, name, color, parent -> scope.launch { controller.updateTag(id, name, color, parent) } },
        onDeleteTag = { id -> scope.launch { controller.deleteTag(id) } },
        onMergeTags = { source, target -> scope.launch { controller.mergeTags(source, target) } },
        onApplyTagToCards = { tagId, ids -> scope.launch { controller.applyTagToCards(tagId, ids) } },
        onClose = onClose
    )
}

@Composable
fun FlagManagerRoute(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    FlagManagerScreenFull(
        cards = controller.cards,
        onFlagCard = { id, flag -> scope.launch { controller.setFlagForCards(listOf(id), flag) } },
        onBulkFlag = { ids, flag -> scope.launch { controller.setFlagForCards(ids, flag) } },
        onStudyByFlag = { flag -> scope.launch { controller.studyByFlag(flag) } },
        onClose = onClose
    )
}

@Composable
fun NoteEditorRoute(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    var cards by remember { mutableStateOf(controller.cards) }
    NoteEditorFullScreen(
        cards = cards,
        onSaveNote = { cardKey, content ->
            scope.launch { controller.saveNote(cardKey, content) }
            cards = cards.map { if (it.id == cardKey) it.copy(notes = content) else it }
        },
        onClose = onClose
    )
}

@Composable
fun ReviewSettingsRoute(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    ReviewSettingsFullScreen(
        initialSettings = controller.reviewSettings,
        onSave = { settings -> scope.launch { controller.saveReviewSettings(settings) } },
        onClose = onClose
    )
}

@Composable
fun KeyboardShortcutsRoute(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    KeyboardShortcutsPage(
        initialShortcuts = controller.shortcuts,
        onSave = { list -> scope.launch { list.forEach { controller.saveShortcut(it) } } },
        onClose = onClose
    )
}

@Composable
fun SearchRoute(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    SearchEngineScreen(
        cards = controller.cards,
        onClose = onClose
    )
}

@Composable
fun BulkActionsRoute(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    BulkActionsFullScreen(
        cards = controller.cards,
        tags = controller.tags,
        onBulkOperation = { operationId, ids -> scope.launch { controller.runBulkOperation(operationId, ids) } },
        onClose = onClose
    )
}

@Composable
fun HistoryRoute(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    HistoryFullScreen(
        cards = controller.cards,
        history = controller.history,
        onClose = onClose
    )
}

@Composable
fun CardBrowserRoute(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    CardBrowserFullScreen(
        cards = controller.cards,
        onFlagCard = { id, flag -> scope.launch { controller.setFlagForCards(listOf(id), flag) } },
        onStatusChange = { id, status -> scope.launch { controller.changeCardStatus(id, status) } },
        onUpdateCard = { card -> scope.launch { controller.updateCardFields(card) } },
        onClose = onClose
    )
}

@Composable
fun CardManagerRoute(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    CardManager(
        initialCards = controller.cards,
        stats = controller.stats,
        heatmap = controller.heatmap
    )
}

@Composable
fun AnkiOperationsRoute(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    AnkiOperationsFullScreen(
        cards = controller.cards,
        onOperation = { operation, cards -> scope.launch { controller.runCardOperation(operation, cards) } },
        onClose = onClose
    )
}

@Composable
fun DeckBrowserRoute(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    val decks = remember(controller.cards) { buildRealDecks(controller.cards) }
    DeckBrowserFullScreen(
        decks = decks,
        onDeckClick = { deck -> scope.launch { controller.recordHistory(KaiteyoHistoryAction.STUDY, "Opened deck '${deck.name}'") } },
        onFavorite = { deck -> scope.launch { controller.recordHistory(KaiteyoHistoryAction.DECK, "Toggled favorite on deck '${deck.name}'") } },
        onArchive = { deck -> scope.launch { controller.recordHistory(KaiteyoHistoryAction.DECK, "Archived deck '${deck.name}'") } },
        onMerge = { a, b -> scope.launch { controller.recordHistory(KaiteyoHistoryAction.DECK, "Merged deck '${a.name}' into '${b.name}'") } },
        onMove = { a, b -> scope.launch { controller.recordHistory(KaiteyoHistoryAction.DECK, "Moved deck '${a.name}' under '${b.name}'") } },
        onCreateDeck = { name, _ -> scope.launch { controller.recordHistory(KaiteyoHistoryAction.DECK, "Created deck '$name'") } },
        onClose = onClose
    )
}

@Composable
fun BackupRoute(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    BackupManagerScreen(
        backups = controller.backups,
        config = controller.backupConfig,
        onDismiss = onClose,
        onCreateBackup = { automatic ->
            scope.launch {
                val name = "kaiteyo-backup-" +
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString() +
                    (if (automatic) "-auto" else "") + ".zip"
                controller.recordBackup(
                    filename = name,
                    size = 0L,
                    checksum = "",
                    isAutomatic = automatic,
                    notes = if (automatic) "Automatic backup" else "Manual backup"
                )
            }
        },
        onRestoreBackup = { backup -> scope.launch { controller.recordRestore(backup) } },
        onDeleteBackup = { backup -> scope.launch { controller.deleteBackup(backup.id) } },
        onVerifyBackup = { backup -> scope.launch { controller.recordVerify(backup) } },
        onUpdateConfig = { config -> scope.launch { controller.saveBackupConfig(config) } }
    )
}

@Composable
fun ImportExportRoute(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    ImportExportScreen()
}

// ============================================
// REAL SCREENS
// Statistics, Undo & History, Collections,
// Card status manager
// ============================================

@Composable
fun DeckStatisticsScreen(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    StatisticsDashboardV2(
        stats = controller.stats,
        heatmap = controller.heatmap,
        onClose = onClose
    )
}

@Composable
fun UndoHistoryScreen(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    val surfaceColors = LocalSurfaceColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History & Undo") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
                },
                actions = {
                    TextButton(
                        enabled = controller.canUndo(),
                        onClick = { scope.launch { controller.undoLast() } }
                    ) { Text("Undo (${controller.undoableActions.size})") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Undo Stack (${controller.undoableActions.size}/100)",
                fontSize = 12.sp,
                color = surfaceColors.textMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (controller.undoableActions.isEmpty()) {
                Text(
                    "Nothing to undo yet.",
                    fontSize = 13.sp,
                    color = surfaceColors.textMuted,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                controller.undoableActions.reversed().forEach { record ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { scope.launch { controller.undoLast() } }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Undo,
                            null,
                            tint = surfaceColors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            record.label,
                            fontSize = 13.sp,
                            color = surfaceColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            formatTime(record.timestamp),
                            fontSize = 11.sp,
                            color = surfaceColors.textMuted
                        )
                    }
                }
            }

            HorizontalDivider(color = surfaceColors.border, modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "Activity History (${controller.history.size})",
                fontSize = 12.sp,
                color = surfaceColors.textMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(Modifier.weight(1f)) {
                items(controller.history, key = { it.id }) { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(8.dp)
                                .clip(CircleShape)
                                .background(historyTypeColor(entry.type))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                entry.description.ifBlank { entry.type.displayName },
                                fontSize = 13.sp,
                                color = surfaceColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                entry.type.displayName,
                                fontSize = 10.sp,
                                color = surfaceColors.textMuted
                            )
                        }
                        Text(formatTime(entry.timestamp), fontSize = 11.sp, color = surfaceColors.textMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun CollectionsScreen(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var selectedCollection by remember { mutableStateOf<KaiteyoCollection?>(null) }

    val favoriteCards = controller.cards.filter { controller.isFavorite(it.id) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collections") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
                },
                actions = {
                    IconButton(onClick = { scope.launch { controller.refresh() } }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    CollectionRow(
                        name = "Favorites",
                        icon = "★",
                        count = favoriteCards.size,
                        isSmart = false,
                        accentColor = Color(0xFFFFD93D),
                        onClick = { selectedCollection = null }
                    )
                }
                items(controller.collections, key = { it.id }) { collection ->
                    CollectionRow(
                        name = collection.name,
                        icon = collection.icon,
                        count = collection.cardIds.size,
                        isSmart = collection.isSmart,
                        accentColor = accent.primary,
                        onClick = { selectedCollection = collection }
                    )
                }
            }

            HorizontalDivider(color = surfaceColors.border)

            val shownCards = selectedCollection?.let { collection ->
                controller.cards.filter { it.id in collection.cardIds }
            } ?: favoriteCards

            Text(
                if (selectedCollection == null) "Favorites (${shownCards.size})" else "${selectedCollection!!.name} (${shownCards.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = surfaceColors.textPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (shownCards.isEmpty()) {
                Text(
                    "No cards in this collection yet.",
                    fontSize = 13.sp,
                    color = surfaceColors.textMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(shownCards, key = { it.id }) { card ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                card.character,
                                fontSize = 18.sp,
                                color = surfaceColors.textPrimary,
                                modifier = Modifier.width(44.dp)
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    card.meaning,
                                    fontSize = 13.sp,
                                    color = surfaceColors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    card.reading,
                                    fontSize = 11.sp,
                                    color = surfaceColors.textMuted
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusColor(card.status).copy(alpha = 0.25f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(card.status.displayName, fontSize = 10.sp, color = surfaceColors.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardStatusScreen(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    val surfaceColors = LocalSurfaceColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card Status Manager") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(controller.cards, key = { it.id }) { card ->
                val suspended = controller.isSuspended(card.id)
                val buried = controller.isBuried(card.id)
                Card(colors = CardDefaults.cardColors(containerColor = surfaceColors.surface)) {
                    Column(Modifier.fillMaxWidth().padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                card.character,
                                fontSize = 16.sp,
                                color = surfaceColors.textPrimary,
                                modifier = Modifier.width(40.dp)
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    card.meaning,
                                    fontSize = 12.sp,
                                    color = surfaceColors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    card.reading,
                                    fontSize = 10.sp,
                                    color = surfaceColors.textMuted
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusColor(card.status).copy(alpha = 0.25f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(card.status.displayName, fontSize = 10.sp, color = surfaceColors.textSecondary)
                            }
                            Spacer(Modifier.width(8.dp))
                            if (suspended) {
                                TextButton(onClick = { scope.launch { controller.unsuspendCards(listOf(card.id)) } }) {
                                    Text("Unsuspend", fontSize = 11.sp)
                                }
                            } else {
                                TextButton(onClick = { scope.launch { controller.suspendCards(listOf(card.id)) } }) {
                                    Text("Suspend", fontSize = 11.sp)
                                }
                            }
                            if (buried) {
                                TextButton(onClick = { scope.launch { controller.unburyCards(listOf(card.id)) } }) {
                                    Text("Unbury", fontSize = 11.sp)
                                }
                            } else {
                                TextButton(onClick = { scope.launch { controller.buryCards(listOf(card.id)) } }) {
                                    Text("Bury", fontSize = 11.sp)
                                }
                            }
                            TextButton(onClick = { scope.launch { controller.resetProgress(listOf(card.id)) } }) {
                                Text("Reset", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// PRIVATE COMPONENTS
// ============================================

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String = "",
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Card(
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, fontSize = 11.sp, color = surfaceColors.textMuted, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accent.primary)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 11.sp, color = surfaceColors.textMuted)
            }
        }
    }
}

@Composable
private fun StatBarRow(label: String, value: Int, color: Color) {
    val surfaceColors = LocalSurfaceColors.current
    val max = value.coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = surfaceColors.textSecondary, modifier = Modifier.width(90.dp))
        Box(
            modifier = Modifier.weight(1f).height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.35f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (max > 0) (value.toFloat() / max).coerceIn(0.02f, 1f) else 0.02f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("$value", fontSize = 12.sp, color = surfaceColors.textPrimary, modifier = Modifier.width(36.dp))
    }
}

@Composable
private fun CollectionRow(
    name: String,
    icon: String,
    count: Int,
    isSmart: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 16.sp, color = accentColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = surfaceColors.textPrimary)
                Text(
                    if (isSmart) "Smart collection" else "$count cards",
                    fontSize = 11.sp,
                    color = surfaceColors.textMuted
                )
            }
            Text("$count", fontSize = 13.sp, color = surfaceColors.textMuted)
        }
    }
}

private fun buildRealDecks(cards: List<KaiteyoCard>): List<KaiteyoDeck> =
    cards.groupBy { it.deck.ifBlank { "Ungrouped" } }.map { (name, list) ->
        KaiteyoDeck(
            id = name,
            name = name,
            description = "Auto-built from real card data",
            cardCount = list.size,
            newCount = list.count { it.status == CardStatus.New },
            learningCount = list.count { it.status == CardStatus.Learning },
            reviewCount = list.count { it.status == CardStatus.Relearning },
            matureCount = list.count { it.status == CardStatus.Mature },
            dueCount = list.count {
                it.status == CardStatus.New || it.status == CardStatus.Learning || it.status == CardStatus.Relearning
            },
            accuracy = if (list.isEmpty()) 0f else list.sumOf { it.accuracy.toDouble() }.toFloat() / list.size,
            retention = if (list.isEmpty()) 0f else list.sumOf { it.accuracy.toDouble() }.toFloat() / list.size,
            lastStudied = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        )
    }

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return when {
        minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

private fun formatTime(instant: kotlinx.datetime.Instant): String {
    val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return if (dt.date == today) {
        "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
    } else {
        "${dt.monthNumber}/${dt.dayOfMonth}"
    }
}

private fun statusColor(status: CardStatus): Color = when (status) {
    CardStatus.New -> Color(0xFFC2FC8B)
    CardStatus.Learning -> Color(0xFF7BC8FF)
    CardStatus.Young -> Color(0xFFA78BFA)
    CardStatus.Mature -> Color(0xFFFEAB57)
    CardStatus.Relearning -> Color(0xFFFF6B6B)
    CardStatus.Suspended -> Color(0xFFB0B0B0)
    CardStatus.Buried -> Color(0xFFB0B0B0)
    CardStatus.Archived -> Color(0xFF808080)
}

private fun historyTypeColor(type: ua.syt0r.kanji.presentation.screen.main.screen.decks.HistoryEntryType): Color =
    when (type) {
        ua.syt0r.kanji.presentation.screen.main.screen.decks.HistoryEntryType.Review -> Color(0xFFC2FC8B)
        ua.syt0r.kanji.presentation.screen.main.screen.decks.HistoryEntryType.Import,
        ua.syt0r.kanji.presentation.screen.main.screen.decks.HistoryEntryType.Export -> Color(0xFF7BC8FF)
        ua.syt0r.kanji.presentation.screen.main.screen.decks.HistoryEntryType.Edit,
        ua.syt0r.kanji.presentation.screen.main.screen.decks.HistoryEntryType.Delete,
        ua.syt0r.kanji.presentation.screen.main.screen.decks.HistoryEntryType.StatusChange,
        ua.syt0r.kanji.presentation.screen.main.screen.decks.HistoryEntryType.ScheduleChange -> Color(0xFFFEAB57)
        ua.syt0r.kanji.presentation.screen.main.screen.decks.HistoryEntryType.TagChange,
        ua.syt0r.kanji.presentation.screen.main.screen.decks.HistoryEntryType.FlagChange,
        ua.syt0r.kanji.presentation.screen.main.screen.decks.HistoryEntryType.NoteChange -> Color(0xFFA78BFA)
        ua.syt0r.kanji.presentation.screen.main.screen.decks.HistoryEntryType.BackupCreated,
        ua.syt0r.kanji.presentation.screen.main.screen.decks.HistoryEntryType.BackupRestored -> Color(0xFFFF6B6B)
        else -> Color(0xFFB0B0B0)
    }
