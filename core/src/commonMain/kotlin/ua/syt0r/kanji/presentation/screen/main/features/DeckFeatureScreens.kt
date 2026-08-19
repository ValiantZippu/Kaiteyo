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
import androidx.compose.material3.CircularProgressIndicator
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
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.statistics.DayItemPractice
import ua.syt0r.kanji.core.statistics.DayPracticeBreakdown
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.screen.main.screen.decks.AnkiOperationsFullScreen
import ua.syt0r.kanji.presentation.screen.main.screen.decks.BackupManagerScreen
import ua.syt0r.kanji.presentation.screen.main.screen.decks.BulkActionsFullScreen
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardBrowserFullScreen
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardDifficulty
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

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorView(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Could not load data",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "The data failed to load. Retry to try again.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text("Retry")
            }
        }
    }
}

// ============================================
// ROUTES — real database-backed wrappers
// ============================================

@Composable
fun TagManagerRoute(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    if (controller.isLoading) {
        LoadingView()
        return
    }
    if (controller.loadError) {
        ErrorView(onRetry = { scope.launch { controller.loadAll() } })
        return
    }
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
    if (controller.isLoading) {
        LoadingView()
        return
    }
    if (controller.loadError) {
        ErrorView(onRetry = { scope.launch { controller.loadAll() } })
        return
    }
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
    if (controller.isLoading) {
        LoadingView()
        return
    }
    if (controller.loadError) {
        ErrorView(onRetry = { scope.launch { controller.loadAll() } })
        return
    }
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
    if (controller.isLoading) {
        LoadingView()
        return
    }
    if (controller.loadError) {
        ErrorView(onRetry = { scope.launch { controller.loadAll() } })
        return
    }
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
    if (controller.isLoading) {
        LoadingView()
        return
    }
    if (controller.loadError) {
        ErrorView(onRetry = { scope.launch { controller.loadAll() } })
        return
    }
    KeyboardShortcutsPage(
        initialShortcuts = controller.shortcuts,
        onSave = { list -> scope.launch { list.forEach { controller.saveShortcut(it) } } },
        onClose = onClose
    )
}

@Composable
fun SearchRoute(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    if (controller.isLoading) {
        LoadingView()
        return
    }
    if (controller.loadError) {
        ErrorView(onRetry = { scope.launch { controller.loadAll() } })
        return
    }
    SearchEngineScreen(
        cards = controller.cards,
        onClose = onClose
    )
}

@Composable
fun BulkActionsRoute(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    if (controller.isLoading) {
        LoadingView()
        return
    }
    if (controller.loadError) {
        ErrorView(onRetry = { scope.launch { controller.loadAll() } })
        return
    }
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
    val scope = rememberCoroutineScope()
    if (controller.isLoading) {
        LoadingView()
        return
    }
    if (controller.loadError) {
        ErrorView(onRetry = { scope.launch { controller.loadAll() } })
        return
    }
    HistoryFullScreen(
        cards = controller.cards,
        history = controller.history,
        onClose = onClose
    )
}

@Composable
fun CardBrowserRoute(
    controller: DeckFeaturesController,
    onClose: () -> Unit = {},
    initialDeckId: String? = null
) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    if (controller.isLoading) {
        LoadingView()
        return
    }
    if (controller.loadError) {
        ErrorView(onRetry = { scope.launch { controller.loadAll() } })
        return
    }
    // Catalog = kanji rows (real deck names) + vocabulary rows, optionally
    // narrowed to a single deck ("letter:N" / "vocab:N") whose name becomes
    // the initial deck filter so the browser opens pre-filtered. The final
    // list is derived from controller.cards at composition time so edits made
    // inside the browser (flags, status, fields) reflect immediately.
    var parts by remember { mutableStateOf<DeckFeaturesController.BrowserCatalogParts?>(null) }
    LaunchedEffect(controller.isLoaded) {
        if (controller.isLoaded) {
            parts = controller.browserCatalogParts(initialDeckId)
        }
    }
    val catalog = remember(controller.cards, parts) {
        if (parts == null) return@remember emptyList()
        val kanjiRows = controller.cards.map { card ->
            val name = parts!!.deckNameByCharacter[card.id]
            if (name != null && name != card.deck) card.copy(deck = name) else card
        }
        val all = kanjiRows + parts!!.vocabRows
        parts!!.filterIds?.let { ids -> all.filter { it.id in ids } } ?: all
    }
    CardBrowserFullScreen(
        cards = catalog,
        initialDeckFilter = parts?.filterLabel,
        onFlagCard = { id, flag -> scope.launch { controller.setFlagForCards(listOf(id), flag) } },
        onStatusChange = { id, status -> scope.launch { controller.changeCardStatus(id, status) } },
        onUpdateCard = { card -> scope.launch { controller.updateCardFields(card) } },
        onClose = onClose
    )
}

/**
 * The Browse tab hosts the Anki-style browser directly: the deck tree rail,
 * the sortable table with Stability/Difficulty/Due columns, and the inline
 * note editor all render against the live library, exactly like Anki's
 * Browse window.
 */
@Composable
fun BrowseTabBrowser(
    onOpenDeck: (String) -> Unit = {},
    onOpenCard: (String) -> Unit = {}
) {
    val controller = koinInject<DeckFeaturesController>()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { controller.ensureLoaded() }
    LaunchedEffect(controller.isLoading) {
        if (!controller.isLoading && controller.isLoaded) controller.loadDecks()
    }
    LaunchedEffect(Unit) {
        controller.deckChangesFlow.collect { controller.loadDecks() }
    }

    if (controller.isLoading) {
        LoadingView()
        return
    }
    if (controller.loadError) {
        ErrorView(onRetry = { scope.launch { controller.loadAll() } })
        return
    }

    var parts by remember { mutableStateOf<DeckFeaturesController.BrowserCatalogParts?>(null) }
    LaunchedEffect(controller.isLoaded) {
        if (controller.isLoaded) {
            parts = controller.browserCatalogParts()
        }
    }

    val catalog = remember(controller.cards, parts) {
        if (parts == null) return@remember emptyList()
        val kanjiRows = controller.cards.map { card ->
            val name = parts!!.deckNameByCharacter[card.id]
            if (name != null && name != card.deck) card.copy(deck = name) else card
        }
        kanjiRows + parts!!.vocabRows
    }

    // Decks with the parent/child hierarchy attached so the rail tree nests.
    val deckObjects = remember(controller.deckSummaries) {
        val flat = buildRealDecks(controller.deckSummaries)
        val byId = flat.associateBy { it.id }
        val roots = flat.filter { deck ->
            deck.parentId == null || byId[deck.parentId] == null
        }
        roots.forEach { root ->
            root.children.clear()
            flat.filter { it.parentId == root.id }.forEach { root.children.add(it) }
        }
        roots
    }

    CardBrowserFullScreen(
        cards = catalog,
        decks = deckObjects,
        embedded = true,
        onFlagCard = { id, flag -> scope.launch { controller.setFlagForCards(listOf(id), flag) } },
        onStatusChange = { id, status -> scope.launch { controller.changeCardStatus(id, status) } },
        onUpdateCard = { card -> scope.launch { controller.updateCardFields(card) } }
    )
}

@Composable
fun DayPracticeCardsRoute(
    day: String,
    onClose: () -> Unit = {}
) {
    val controller = koinInject<DeckFeaturesController>()
    val statisticsController = koinInject<StatisticsController>()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        controller.ensureLoaded()
        statisticsController.ensureLoaded()
    }

    if (controller.isLoading || statisticsController.isLoading) {
        LoadingView()
        return
    }
    if (controller.loadError || statisticsController.loadError) {
        ErrorView(onRetry = {
            scope.launch {
                controller.loadAll()
                statisticsController.load()
            }
        })
        return
    }

    val date = remember(day) {
        runCatching { kotlinx.datetime.LocalDate.parse(day) }.getOrNull()
    }

    // Resolve the real set of cards practiced on that day from review history.
    var breakdown by remember { mutableStateOf<DayPracticeBreakdown?>(null) }
    LaunchedEffect(date) {
        breakdown = date?.let {
            runCatching { statisticsController.itemsPracticedOnDay(it) }.getOrNull()
        }
    }

    val practicedKeys = remember(breakdown) {
        buildSet {
            breakdown?.kanji?.forEach { add(it.key) }
            breakdown?.vocab?.forEach { add(it.key) }
        }
    }

    // Pass the full shared catalog (plus vocabulary rows synthesized from the
    // day's history) so the preset filter genuinely narrows it: clearing the
    // day chip reveals the whole library instead of a no-op.
    val libraryCards = remember(controller.cards, breakdown) {
        val catalogKeys = controller.cards.map { it.id }.toSet()
        val synthesizedVocab = breakdown?.vocab.orEmpty()
            .filter { it.key !in catalogKeys }
            .map { it.toLibraryCard(day) }
        controller.cards + synthesizedVocab
    }

    CardBrowserFullScreen(
        cards = libraryCards,
        presetCardIds = practicedKeys,
        presetLabel = "Practiced on $day",
        onFlagCard = { id, flag -> scope.launch { controller.setFlagForCards(listOf(id), flag) } },
        onStatusChange = { id, status -> scope.launch { controller.changeCardStatus(id, status) } },
        onUpdateCard = { card -> scope.launch { controller.updateCardFields(card) } },
        onClose = onClose
    )
}

/** Synthesizes a browser row for a vocabulary item practiced on a given day. */
private fun DayItemPractice.toLibraryCard(day: String): KaiteyoCard = KaiteyoCard(
    id = key,
    character = content.ifBlank { key },
    meaning = meaning.ifBlank { reading },
    reading = reading,
    deck = "Vocabulary",
    deckId = 0L,
    tags = mutableListOf(),
    tagNames = mutableListOf(practiceLabel.lowercase().replace(" ", "-")),
    flag = CardFlagType.None,
    notes = "",
    status = when {
        accuracy >= 0.9f -> CardStatus.Mature
        accuracy >= 0.7f -> CardStatus.Young
        else -> CardStatus.Learning
    },
    difficulty = CardDifficulty.Good,
    priority = 0,
    isSuspended = false,
    isBuried = false,
    isArchived = false,
    isFavorite = false,
    customFields = mutableMapOf(),
    aliases = mutableListOf(),
    relatedCards = mutableListOf(),
    createdAt = day,
    modifiedAt = day,
    lastReviewed = day,
    reviewCount = count,
    interval = 0,
    ease = 2.5f,
    lapses = mistakes,
    accuracy = accuracy,
    totalTimeStudied = 0L
)

@Composable
fun CardManagerRoute(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    if (controller.isLoading) {
        LoadingView()
        return
    }
    if (controller.loadError) {
        ErrorView(onRetry = { scope.launch { controller.loadAll() } })
        return
    }
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
    if (controller.isLoading) {
        LoadingView()
        return
    }
    if (controller.loadError) {
        ErrorView(onRetry = { scope.launch { controller.loadAll() } })
        return
    }
    AnkiOperationsFullScreen(
        cards = controller.cards,
        onOperation = { operation, cards -> scope.launch { controller.runCardOperation(operation, cards) } },
        onClose = onClose
    )
}

@Composable
fun DeckBrowserRoute(
    controller: DeckFeaturesController,
    onClose: () -> Unit = {},
    onOpenDeck: (String) -> Unit = {},
    onBrowse: (String) -> Unit = {},
    onFindContent: () -> Unit = {}
) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    if (controller.isLoading) {
        LoadingView()
        return
    }
    if (controller.loadError) {
        ErrorView(onRetry = { scope.launch { controller.loadAll() } })
        return
    }
    LaunchedEffect(controller.isLoading) {
        if (!controller.isLoading && controller.isLoaded) controller.loadDecks()
    }
    // Live counts: any deck/FSRS/review change reloads the summaries so the
    // browser's new/learning/review/due numbers always match the study screens.
    LaunchedEffect(Unit) {
        controller.deckChangesFlow.collect { controller.loadDecks() }
    }
    val decks = buildRealDecks(controller.deckSummaries)
    DeckBrowserFullScreen(
        decks = decks,
        onDeckClick = { deck -> onOpenDeck(deck.id) },
        onFavorite = { deck -> scope.launch { controller.toggleDeckFavorite(deck.id) } },
        onPin = { deck -> scope.launch { controller.toggleDeckPin(deck.id) } },
        onArchive = { deck -> scope.launch { controller.archiveDeck(deck.id, !deck.isArchived) } },
        onMerge = { a, b -> scope.launch { controller.mergeDeck(a.id, b.id) } },
        onMove = { a, b -> scope.launch { controller.moveDeck(a.id, b.id) } },
        onRename = { deck, name -> scope.launch { controller.renameDeck(deck.id, name) } },
        onDelete = { deck -> scope.launch { controller.deleteDeck(deck.id) } },
        onCreateDeck = { name, type, _, noteTypeId ->
            scope.launch { controller.createDeck(name, type, noteTypeId) }
        },
        onBrowse = { deck -> onBrowse(deck.id) },
        onFindContent = { onFindContent() },
        onClose = onClose
    )
}

@Composable
fun BackupRoute(
    controller: DeckFeaturesController,
    onOpenBackup: () -> Unit,
    onClose: () -> Unit = {}
) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    if (controller.isLoading) {
        LoadingView()
        return
    }
    if (controller.loadError) {
        ErrorView(onRetry = { scope.launch { controller.loadAll() } })
        return
    }
    BackupManagerScreen(
        backups = controller.backups,
        config = controller.backupConfig,
        onDismiss = onClose,
        // Creating and restoring both go through the real backup flow (platform
        // file picker + BackupManager) — the manager screen never fabricates
        // metadata-only backup records.
        onOpenBackup = onOpenBackup,
        onDeleteBackup = { backup -> scope.launch { controller.deleteBackup(backup.id) } },
        onUpdateConfig = { config -> scope.launch { controller.saveBackupConfig(config) } }
    )
}

@Composable
fun ImportExportRoute(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    if (controller.isLoading) {
        LoadingView()
        return
    }
    if (controller.loadError) {
        ErrorView(onRetry = { scope.launch { controller.loadAll() } })
        return
    }
    ImportExportScreen()
}

// ============================================
// REAL SCREENS
// Undo & History, Collections, Card status
// manager. (Statistics lives in the unified
// screen/statistics/StatisticsScreen.kt.)
// ============================================

@Composable
fun UndoHistoryScreen(controller: DeckFeaturesController, onClose: () -> Unit = {}) {
    LaunchedEffect(Unit) { controller.ensureLoaded() }
    val scope = rememberCoroutineScope()
    if (controller.isLoading) {
        LoadingView()
        return
    }
    if (controller.loadError) {
        ErrorView(onRetry = { scope.launch { controller.loadAll() } })
        return
    }
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
    if (controller.isLoading) {
        LoadingView()
        return
    }
    if (controller.loadError) {
        ErrorView(onRetry = { scope.launch { controller.loadAll() } })
        return
    }
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
    if (controller.isLoading) {
        LoadingView()
        return
    }
    if (controller.loadError) {
        ErrorView(onRetry = { scope.launch { controller.loadAll() } })
        return
    }
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

private fun buildRealDecks(summaries: List<DeckFeaturesController.DeckSummary>): List<KaiteyoDeck> =
    summaries.map { summary ->
        KaiteyoDeck(
            id = summary.id,
            name = summary.name,
            description = if (summary.cardCount == 1) "1 card" else "${summary.cardCount} cards",
            isArchived = summary.isArchived,
            isFavorite = summary.isFavorite,
            isPinned = summary.isPinned,
            cardCount = summary.cardCount,
            newCount = summary.newCount,
            learningCount = summary.learningCount,
            reviewCount = summary.reviewCount,
            matureCount = summary.reviewCount,
            dueCount = summary.dueCount,
            accuracy = 0f,
            retention = 0f,
            lastStudied = ""
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
