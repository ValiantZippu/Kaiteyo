package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoSemanticColors
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.common.ui.KaiteyoAlertDialog
import ua.syt0r.kanji.presentation.screen.main.features.KANJI_BROWSER_DECK_NAME
import kotlin.math.roundToInt

// ============================================
// KAITEYO v1.2 — FULL CARD BROWSER
// 15+ columns, sortable headers, search,
// column visibility, multi-select, bulk ops
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardBrowserFullScreen(
    cards: List<KaiteyoCard>,
    presetCardIds: Set<String> = emptySet(),
    presetLabel: String? = null,
    initialDeckFilter: String? = null,
    decks: List<KaiteyoDeck> = emptyList(),
    embedded: Boolean = false,
    onFlagCard: (String, CardFlagType) -> Unit = { _, _ -> },
    onStatusChange: (String, CardStatus) -> Unit = { _, _ -> },
    onUpdateCard: (KaiteyoCard) -> Unit = {},
    onClose: () -> Unit = {}
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    // ── State ──
    var searchQuery by remember { mutableStateOf("") }
    var columns by remember { mutableStateOf(defaultBrowserColumns) }
    var sortColumn by remember { mutableStateOf("deck") }
    var sortAscending by remember { mutableStateOf(true) }
    var selectedCardIds by remember { mutableStateOf(setOf<String>()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var showColumnPicker by remember { mutableStateOf(false) }
    var showFilterPanel by remember { mutableStateOf(false) }
    var flagFilter by remember { mutableStateOf<CardFlagType?>(null) }
    var anyFlagFilter by remember { mutableStateOf(false) }
    // Multi-select card state filter (Anki's card-state sidebar is multi).
    var statusFilter by remember { mutableStateOf(setOf<CardStatus>()) }
    var deckFilter by remember { mutableStateOf(initialDeckFilter) }
    var tagFilter by remember { mutableStateOf<String?>(null) }
    // Right-click target for the desktop row context menu.
    var contextMenuCard by remember { mutableStateOf<KaiteyoCard?>(null) }
    // Filters the rail contents (saved searches, decks, tags) by text.
    var railQuery by remember { mutableStateOf("") }
    // User-created saved searches (session-scoped): captured from the current
    // filter combination with the "Save current search" rail action.
    var customSearches by remember { mutableStateOf(listOf<SavedSearch>()) }
    // Lets '/' or Ctrl+F jump straight into the search field.
    val searchFocusRequester = remember { FocusRequester() }
    // Optional day-filter preset: landing view for "cards practiced on <day>".
    var dayCardIds by remember { mutableStateOf(presetCardIds) }
    var showCardDetail by remember { mutableStateOf<KaiteyoCard?>(null) }
    var visibleColumns by remember { mutableStateOf(columns.filter { it.isVisible }.map { it.id }.toSet()) }
    // The note editor pane mirrors the Anki browser: clicking a row opens the
    // note on the right side instead of a modal dialog.
    var noteEditorCard by remember { mutableStateOf<KaiteyoCard?>(null) }
    // Which bulk-action dialog is open for the multi-selected cards.
    var bulkAction by remember { mutableStateOf<String?>(null) }

    // ── Filtered & Sorted Cards ──
    val processedCards = remember(cards, searchQuery, flagFilter, anyFlagFilter, statusFilter, deckFilter, tagFilter, dayCardIds, sortColumn, sortAscending) {
        var result = cards

        // Day preset filter (cards practiced on a specific day)
        if (dayCardIds.isNotEmpty()) {
            result = result.filter { it.id in dayCardIds }
        }

        // Query syntax — tag: flag: deck: status: due: plus free text across
        // every readable field (Anki-style search).
        if (searchQuery.isNotBlank()) {
            val parsed = parseBrowserQuery(searchQuery)
            result = result.filter { card ->
                val text = parsed.text
                val matchesText = text.isEmpty() ||
                    card.character.contains(text, ignoreCase = true) ||
                    card.meaning.contains(text, ignoreCase = true) ||
                    card.reading.contains(text, ignoreCase = true) ||
                    card.deck.contains(text, ignoreCase = true) ||
                    card.notes.contains(text, ignoreCase = true) ||
                    card.id.contains(text, ignoreCase = true) ||
                    card.tagNames.any { it.contains(text, ignoreCase = true) }
                val matchesTag = parsed.tag == null ||
                    card.tagNames.any { it.contains(parsed.tag, ignoreCase = true) }
                val matchesFlag = parsed.flag == null || card.flag == parsed.flag
                val matchesDeck = parsed.deck == null ||
                    card.deck.contains(parsed.deck, ignoreCase = true)
                val matchesStatus = parsed.status == null || card.status == parsed.status
                val matchesDue = parsed.due == null || when (parsed.due) {
                    "new" -> card.status == CardStatus.New
                    "learning" -> card.status == CardStatus.Learning || card.status == CardStatus.Relearning
                    "due", "today", "review" -> card.status == CardStatus.Young || card.status == CardStatus.Mature
                    "suspended" -> card.status == CardStatus.Suspended
                    "buried" -> card.status == CardStatus.Buried
                    "archived" -> card.status == CardStatus.Archived
                    else -> true
                }
                matchesText && matchesTag && matchesFlag && matchesDeck && matchesStatus && matchesDue
            }
        }

        // Filters
        flagFilter?.let { flag -> result = result.filter { it.flag == flag } }
        if (anyFlagFilter) result = result.filter { it.flag != CardFlagType.None }
        if (statusFilter.isNotEmpty()) result = result.filter { it.status in statusFilter }
        deckFilter?.let { deck -> result = result.filter { it.deck == deck } }
        tagFilter?.let { tag -> result = result.filter { it.tagNames.contains(tag) } }

        // The reference kanji catalog is a browseable REFERENCE, not a study
        // queue: status searches (New / Learning / Due / Card-State chips)
        // exclude it unless the user explicitly drilled into the Kanji
        // Browser deck. This is what keeps "New: 6410" nonsense from ever
        // appearing — the queue only ever shows real study decks.
        if (statusFilter.isNotEmpty() && deckFilter != KANJI_BROWSER_DECK_NAME) {
            result = result.filter { it.deck != KANJI_BROWSER_DECK_NAME }
        }

        // Sort
        result = when (sortColumn) {
            "kanji" -> result.sortedBy { it.character }
            "reading" -> result.sortedBy { it.reading }
            "meaning" -> result.sortedBy { it.meaning }
            "deck" -> result.sortedBy { it.deck }
            "tags" -> result.sortedBy { it.tagNames.firstOrNull() ?: "" }
            "flag" -> result.sortedBy { it.flag.ordinal }
            "status" -> result.sortedBy { it.status.ordinal }
            "interval" -> result.sortedBy { it.interval }
            "stability" -> result.sortedBy { it.interval }
            "difficulty" -> result.sortedBy { (1f - it.accuracy) + it.lapses * 0.05f }
            "due" -> result.sortedBy { it.status.ordinal }
            "ease" -> result.sortedBy { it.ease }
            "reviews" -> result.sortedBy { it.reviewCount }
            "lapses" -> result.sortedBy { it.lapses }
            "created" -> result.sortedBy { it.createdAt }
            "modified" -> result.sortedBy { it.modifiedAt }
            "lastReview" -> result.sortedBy { it.lastReviewed }
            "accuracy" -> result.sortedBy { it.accuracy }
            "timeStudied" -> result.sortedBy { it.totalTimeStudied }
            "select" -> result
            else -> result
        }
        if (!sortAscending) result = result.reversed()

        result
    }

    // ── Aggregate Data ──
    // Cards belonging to real study decks. The reference kanji catalog stays
    // fully browseable but never participates in queue counts.
    val studyCards = remember(cards) { cards.filter { it.deck != KANJI_BROWSER_DECK_NAME } }
    val uniqueDecks = remember(cards) { cards.map { it.deck }.distinct().sorted() }
    val uniqueTags = remember(cards) { cards.flatMap { it.tagNames }.distinct().sorted() }
    val totalCards = cards.size
    val filteredCount = processedCards.size
    val selectedCount = selectedCardIds.size

    // ── Bulk action dialogs — wired to the same single-card callbacks so
    // flag / status / tag apply to every selected card in one step. ──
    when (bulkAction) {
        "flag" -> BulkFlagDialog(
            onPick = { flag ->
                selectedCardIds.forEach { onFlagCard(it, flag) }
                selectedCardIds = emptySet()
                isSelectionMode = false
                bulkAction = null
            },
            onDismiss = { bulkAction = null }
        )
        "status" -> BulkStatusDialog(
            onPick = { status ->
                selectedCardIds.forEach { onStatusChange(it, status) }
                selectedCardIds = emptySet()
                isSelectionMode = false
                bulkAction = null
            },
            onDismiss = { bulkAction = null }
        )
        "tag" -> BulkTagDialog(
            onApply = { tags ->
                processedCards
                    .filter { it.id in selectedCardIds }
                    .forEach { card ->
                        onUpdateCard(
                            card.copy(tagNames = (card.tagNames + tags).distinct().toMutableList())
                        )
                    }
                selectedCardIds = emptySet()
                isSelectionMode = false
                bulkAction = null
            },
            onDismiss = { bulkAction = null }
        )
        else -> {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (embedded) "Browse" else "Card Browser") },
                navigationIcon = {
                    if (!embedded) {
                        IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
                    }
                },
                actions = {
                    IconButton(onClick = { showColumnPicker = true }) { Icon(Icons.Default.ViewColumn, "Columns") }
                    IconButton(onClick = { showFilterPanel = !showFilterPanel }) {
                        Icon(Icons.Default.FilterList, "Filters",
                            tint = if (flagFilter != null || anyFlagFilter || statusFilter.isNotEmpty() || deckFilter != null || tagFilter != null || dayCardIds.isNotEmpty())
                                accent.primary else surfaceColors.textMuted)
                    }
                    if (isSelectionMode) {
                        IconButton(onClick = { selectedCardIds = emptySet(); isSelectionMode = false }) {
                            Icon(Icons.Default.Close, "Cancel Selection")
                        }
                    } else {
                        IconButton(onClick = { isSelectionMode = true }) {
                            Icon(Icons.Default.CheckBox, "Select")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColors.surface,
                    titleContentColor = surfaceColors.textPrimary
                )
            )
        }
    ) { padding ->
        // The Anki-style panes need width to breathe: on narrow windows the
        // rail collapses and the note editor falls back to the detail dialog
        // so the table never gets squeezed out.
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
                // Browser-wide shortcuts: Ctrl+F jumps to search, Ctrl+A selects
                // every visible card, Escape clears the search + filters.
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when {
                            event.isCtrlPressed && event.key == Key.F -> {
                                searchFocusRequester.requestFocus()
                                true
                            }
                            event.isCtrlPressed && event.key == Key.A -> {
                                if (processedCards.isNotEmpty()) {
                                    selectedCardIds = processedCards.map { it.id }.toSet()
                                    isSelectionMode = true
                                }
                                true
                            }
                            event.key == Key.Escape -> {
                                val anythingActive = searchQuery.isNotBlank() || flagFilter != null ||
                                    anyFlagFilter || statusFilter.isNotEmpty() ||
                                    deckFilter != null || tagFilter != null || dayCardIds.isNotEmpty()
                                if (anythingActive) {
                                    flagFilter = null; anyFlagFilter = false; statusFilter = emptySet()
                                    deckFilter = null; tagFilter = null
                                    dayCardIds = emptySet()
                                    searchQuery = ""
                                    true
                                } else false
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            val wideLayout = maxWidth >= 680.dp
        Row(Modifier.fillMaxSize()) {
            // ── LEFT RAIL — Anki-style browser sidebar ──
            if (wideLayout) {
                BrowserFilterRail(
                    decks = decks,
                    uniqueDecks = uniqueDecks,
                    uniqueTags = uniqueTags,
                    cards = cards,
                    railQuery = railQuery,
                    onRailQueryChange = { railQuery = it },
                    customSearches = customSearches,
                    onSaveCustomSearch = { label ->
                        val newSearch = SavedSearch(
                            label = label,
                            flag = flagFilter,
                            anyFlag = anyFlagFilter,
                            statuses = statusFilter.toList(),
                            deck = deckFilter
                        )
                        customSearches = if (customSearches.any { it.label == label }) {
                            customSearches.map { if (it.label == label) newSearch else it }
                        } else {
                            customSearches + newSearch
                        }
                    },
                    onRemoveCustomSearch = { search -> customSearches = customSearches - search },
                    flagFilter = flagFilter,
                    anyFlagFilter = anyFlagFilter,
                    statusFilter = statusFilter,
                    deckFilter = deckFilter,
                    tagFilter = tagFilter,
                    onFlagFilterChange = { flagFilter = it },
                    onAnyFlagFilterChange = { anyFlagFilter = it },
                    onStatusFilterChange = { statusFilter = it },
                    onDeckFilterChange = { deckFilter = it },
                    onTagFilterChange = { tagFilter = it },
                    onClearFilters = {
                        flagFilter = null; anyFlagFilter = false; statusFilter = emptySet()
                        deckFilter = null; tagFilter = null
                        dayCardIds = emptySet()
                        searchQuery = ""
                    },
                    surfaceColors = surfaceColors,
                    accent = accent
                )

                VerticalDivider(color = surfaceColors.border.copy(alpha = 0.4f))
            }

            // ── CENTER — table ──
            Column(Modifier.weight(1f).fillMaxHeight()) {
            // ── Stats Bar ──
            BrowserStatsBar(
                totalCards = totalCards,
                filteredCount = filteredCount,
                selectedCount = selectedCount,
                isSelectionMode = isSelectionMode,
                surfaceColors = surfaceColors,
                accent = accent
            )

            // ── Search Bar ──
            BrowserSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                focusRequester = searchFocusRequester,
                flagFilter = flagFilter,
                statusFilter = statusFilter,
                deckFilter = deckFilter,
                tagFilter = tagFilter,
                dayFilterLabel = if (dayCardIds.isNotEmpty()) presetLabel else null,
                onClearDayFilter = { dayCardIds = emptySet() },
                onFlagFilterChange = { flagFilter = it },
                onStatusFilterChange = { statusFilter = it },
                onDeckFilterChange = { deckFilter = it },
                onTagFilterChange = { tagFilter = it },
                onClearFilters = {
                    flagFilter = null; anyFlagFilter = false; statusFilter = emptySet()
                    deckFilter = null; tagFilter = null
                    dayCardIds = emptySet()
                    searchQuery = ""
                },
                surfaceColors = surfaceColors,
                accent = accent
            )

            // ── Filter Panel ──
            AnimatedVisibility(visible = showFilterPanel) {
                FilterPanel(
                    flagFilter = flagFilter,
                    statusFilter = statusFilter,
                    deckFilter = deckFilter,
                    tagFilter = tagFilter,
                    onFlagFilterChange = { flagFilter = it },
                    onStatusFilterChange = { statusFilter = it },
                    onDeckFilterChange = { deckFilter = it },
                    onTagFilterChange = { tagFilter = it },
                    uniqueDecks = uniqueDecks,
                    uniqueTags = uniqueTags,
                    surfaceColors = surfaceColors,
                    accent = accent
                )
            }

            // ── Column Headers ──
            BrowserColumnHeaders(
                columns = columns.filter { it.id in visibleColumns },
                sortColumn = sortColumn,
                sortAscending = sortAscending,
                onSortChange = { col ->
                    if (sortColumn == col) sortAscending = !sortAscending
                    else { sortColumn = col; sortAscending = true }
                },
                isSelectionMode = isSelectionMode,
                allSelected = selectedCardIds.size == processedCards.size && processedCards.isNotEmpty(),
                onToggleSelectAll = {
                    if (selectedCardIds.size == processedCards.size) selectedCardIds = emptySet()
                    else selectedCardIds = processedCards.map { it.id }.toSet()
                },
                surfaceColors = surfaceColors,
                accent = accent
            )

            HorizontalDivider(color = surfaceColors.border.copy(alpha = 0.3f))

            // ── Card Rows ──
            if (processedCards.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, null, Modifier.size(48.dp), tint = surfaceColors.textMuted)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            when {
                                dayCardIds.isNotEmpty() && searchQuery.isNotBlank() ->
                                    "No cards from ${presetLabel ?: "the filtered set"} match your search"
                                dayCardIds.isNotEmpty() ->
                                    "No cards practiced on this day — the day filter is still applied"
                                searchQuery.isNotBlank() -> "No cards match your search"
                                else -> "No cards to display"
                            },
                            color = surfaceColors.textMuted
                        )
                        if (dayCardIds.isNotEmpty() && searchQuery.isBlank()) {
                            Spacer(Modifier.height(4.dp))
                            TextButton(onClick = { dayCardIds = emptySet() }) {
                                Text("Show all cards", fontSize = 12.sp, color = accent.primary)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = rememberLazyListState()
                ) {
                    itemsIndexed(processedCards, key = { _, it -> it.id }) { index, card ->
                        BrowserCardRow(
                            rowIndex = index,
                            card = card,
                            visibleColumns = visibleColumns,
                            columns = columns,
                            isSelected = card.id in selectedCardIds,
                            isSelectionMode = isSelectionMode,
                            contextMenuCard = contextMenuCard,
                            onToggleSelect = { id ->
                                selectedCardIds = if (id in selectedCardIds) selectedCardIds - id
                                else selectedCardIds + id
                            },
                            onClick = { if (wideLayout) noteEditorCard = card else showCardDetail = card },
                            onContextMenu = { contextMenuCard = if (contextMenuCard?.id == card.id) null else card },
                            onOpenDetails = { showCardDetail = card },
                            onFlagClick = { onFlagCard(card.id, it) },
                            onStatusClick = { onStatusChange(card.id, it) },
                            surfaceColors = surfaceColors,
                            accent = accent
                        )
                        HorizontalDivider(color = surfaceColors.border.copy(alpha = 0.15f))
                    }
                }
            }

            // ── Bottom Action Bar ──
            if (isSelectionMode && selectedCardIds.isNotEmpty()) {
                BrowserSelectionBar(
                    selectedCount = selectedCount,
                    onDeselectAll = { selectedCardIds = emptySet() },
                    onBulkTag = { bulkAction = "tag" },
                    onBulkFlag = { bulkAction = "flag" },
                    onBulkStatus = { bulkAction = "status" },
                    surfaceColors = surfaceColors,
                    accent = accent
                )
            }
            }

            // ── RIGHT PANE — Anki-style note editor for the selected note ──
            if (noteEditorCard != null && wideLayout) {
                VerticalDivider(color = surfaceColors.border.copy(alpha = 0.4f))
                BrowserNoteEditorPane(
                    card = noteEditorCard!!,
                    onUpdate = { updated ->
                        onUpdateCard(updated)
                        noteEditorCard = updated
                        // Keep the detail dialog in sync when it is open.
                        if (showCardDetail?.id == updated.id) showCardDetail = updated
                    },
                    onClose = { noteEditorCard = null },
                    onShowDetails = { showCardDetail = noteEditorCard },
                    surfaceColors = surfaceColors,
                    accent = accent
                )
            }
        }
        }
    }

    // ── Column Picker Dialog ──
    if (showColumnPicker) {
        ColumnPickerDialog(
            columns = columns,
            visibleColumns = visibleColumns,
            onToggleColumn = { colId ->
                visibleColumns = if (colId in visibleColumns) visibleColumns - colId
                else visibleColumns + colId
                columns = columns.map { if (it.id == colId) it.copy(isVisible = colId in visibleColumns) else it }
            },
            onDismiss = { showColumnPicker = false }
        )
    }

    // ── Card Detail Dialog ──
    showCardDetail?.let { card ->
        CardDetailDialog(
            card = card,
            onDismiss = { showCardDetail = null },
            onFlagChange = { onFlagCard(card.id, it) },
            onStatusChange = { onStatusChange(card.id, it) },
            onUpdate = { onUpdateCard(it) },
            surfaceColors = surfaceColors,
            accent = accent
        )
    }
}

// ════════════════════════════════════════════
// BROWSER STATS BAR
// ════════════════════════════════════════════

@Composable
private fun BrowserStatsBar(
    totalCards: Int,
    filteredCount: Int,
    selectedCount: Int,
    isSelectionMode: Boolean,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$filteredCount / $totalCards cards", fontSize = 12.sp, color = surfaceColors.textMuted)
        if (filteredCount < totalCards) {
            Text("filtered", fontSize = 11.sp, color = accent.primary)
        }
        Spacer(Modifier.weight(1f))
        if (isSelectionMode) {
            Text("$selectedCount selected", fontSize = 12.sp, color = accent.primary, fontWeight = FontWeight.Medium)
        }
    }
}

// ════════════════════════════════════════════
// BROWSER SEARCH BAR
// ════════════════════════════════════════════

@Composable
private fun BrowserSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    flagFilter: CardFlagType?,
    statusFilter: Set<CardStatus>,
    deckFilter: String?,
    tagFilter: String?,
    dayFilterLabel: String?,
    onClearDayFilter: () -> Unit,
    onFlagFilterChange: (CardFlagType?) -> Unit,
    onStatusFilterChange: (Set<CardStatus>) -> Unit,
    onDeckFilterChange: (String?) -> Unit,
    onTagFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    val hasFilters = flagFilter != null || statusFilter.isNotEmpty() || deckFilter != null || tagFilter != null || dayFilterLabel != null || query.isNotBlank()
    var showSearchTips by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f).height(44.dp).focusRequester(focusRequester),
                placeholder = { Text("Search cards... (e.g. tag:jlpt flag:red deck:N5)", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                trailingIcon = {
                    Row {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, "Clear", Modifier.size(16.dp))
                            }
                        }
                        IconButton(onClick = { showSearchTips = !showSearchTips }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Help, "Search Tips", Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = surfaceColors.textPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = surfaceColors.border.copy(alpha = 0.3f),
                    cursorColor = accent.primary
                ),
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* trigger search */ })
            )
        }

        // Active filter chips
        if (hasFilters) {
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                if (dayFilterLabel != null) {
                    AssistChip(onClick = onClearDayFilter, label = { Text("📅 $dayFilterLabel", fontSize = 10.sp, maxLines = 1) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(12.dp)) },
                        modifier = Modifier.height(24.dp), shape = RoundedCornerShape(12.dp))
                }
                if (query.isNotBlank()) {
                    AssistChip(onClick = { onQueryChange("") }, label = { Text("\"$query\"", fontSize = 10.sp, maxLines = 1) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(12.dp)) },
                        modifier = Modifier.height(24.dp), shape = RoundedCornerShape(12.dp))
                }
                flagFilter?.let { f ->
                    AssistChip(onClick = { onFlagFilterChange(null) }, label = { Text("Flag: ${f.displayName}", fontSize = 10.sp) },
                        leadingIcon = { Box(Modifier.size(8.dp).clip(CircleShape).background(f.colorFromHex())) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(12.dp)) },
                        modifier = Modifier.height(24.dp), shape = RoundedCornerShape(12.dp))
                }
                statusFilter.forEach { s ->
                    AssistChip(onClick = { onStatusFilterChange(statusFilter - s) }, label = { Text("Status: ${s.displayName}", fontSize = 10.sp) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(12.dp)) },
                        modifier = Modifier.height(24.dp), shape = RoundedCornerShape(12.dp))
                }
                deckFilter?.let { d ->
                    AssistChip(onClick = { onDeckFilterChange(null) }, label = { Text("Deck: $d", fontSize = 10.sp, maxLines = 1) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(12.dp)) },
                        modifier = Modifier.height(24.dp), shape = RoundedCornerShape(12.dp))
                }
                tagFilter?.let { t ->
                    AssistChip(onClick = { onTagFilterChange(null) }, label = { Text("Tag: $t", fontSize = 10.sp, maxLines = 1) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(12.dp)) },
                        modifier = Modifier.height(24.dp), shape = RoundedCornerShape(12.dp))
                }
            }
        }

        // Search tips
        AnimatedVisibility(visible = showSearchTips) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColors.surfaceInteractive),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.padding(8.dp)) {
                    Text("Search Tips", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = surfaceColors.textPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text("tag:jlpt-n5 — filter by tag", fontSize = 11.sp, color = surfaceColors.textMuted)
                    Text("flag:red — filter by flag", fontSize = 11.sp, color = surfaceColors.textMuted)
                    Text("deck:N5 — filter by deck", fontSize = 11.sp, color = surfaceColors.textMuted)
                    Text("status:learning — filter by status", fontSize = 11.sp, color = surfaceColors.textMuted)
                    Text("Combine: tag:jlpt flag:red", fontSize = 11.sp, color = surfaceColors.textMuted)
                }
            }
        }
    }
}

// ════════════════════════════════════════════
// FILTER PANEL
// ════════════════════════════════════════════

@Composable
private fun FilterPanel(
    flagFilter: CardFlagType?,
    statusFilter: Set<CardStatus>,
    deckFilter: String?,
    tagFilter: String?,
    onFlagFilterChange: (CardFlagType?) -> Unit,
    onStatusFilterChange: (Set<CardStatus>) -> Unit,
    onDeckFilterChange: (String?) -> Unit,
    onTagFilterChange: (String?) -> Unit,
    uniqueDecks: List<String>,
    uniqueTags: List<String>,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surfaceElevated),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Flag filter
            Text("Flag", style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(CardFlagType.entries) { flag ->
                    FilterChip(
                        selected = flagFilter == flag,
                        onClick = { onFlagFilterChange(if (flagFilter == flag) null else flag) },
                        label = { Text(flag.displayName, fontSize = 11.sp) },
                        leadingIcon = {
                            if (flag != CardFlagType.None) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(flag.colorFromHex()))
                            }
                        },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            // Status filter
            Text("Status", style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(CardStatus.entries) { status ->
                    FilterChip(
                        selected = status in statusFilter,
                        onClick = {
                            onStatusFilterChange(
                                if (status in statusFilter) statusFilter - status
                                else statusFilter + status
                            )
                        },
                        label = { Text(status.displayName, fontSize = 11.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            // Deck filter
            Text("Deck", style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(uniqueDecks) { deck ->
                    FilterChip(
                        selected = deckFilter == deck,
                        onClick = { onDeckFilterChange(if (deckFilter == deck) null else deck) },
                        label = { Text(deck, fontSize = 11.sp, maxLines = 1) },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            // Tag filter
            Text("Tag", style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(uniqueTags) { tag ->
                    FilterChip(
                        selected = tagFilter == tag,
                        onClick = { onTagFilterChange(if (tagFilter == tag) null else tag) },
                        label = { Text(tag, fontSize = 11.sp, maxLines = 1) },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════
// COLUMN HEADERS
// ════════════════════════════════════════════

@Composable
private fun BrowserColumnHeaders(
    columns: List<BrowserColumn>,
    sortColumn: String,
    sortAscending: Boolean,
    onSortChange: (String) -> Unit,
    isSelectionMode: Boolean,
    allSelected: Boolean,
    onToggleSelectAll: () -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColors.surfaceElevated)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        columns.forEach { col ->
            item {
                val isSorted = sortColumn == col.id
                Row(
                    modifier = Modifier
                        .width(col.width.dp)
                        .clickable { if (col.sortable) onSortChange(col.id) }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = when (col.alignment) {
                        ColumnAlignment.Left -> Arrangement.Start
                        ColumnAlignment.Center -> Arrangement.Center
                        ColumnAlignment.Right -> Arrangement.End
                    }
                ) {
                    if (col.id == "select") {
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = { onToggleSelectAll() },
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        if (isSorted) {
                            Icon(
                                if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                "Sort", Modifier.size(14.dp), tint = accent.primary
                            )
                            Spacer(Modifier.width(2.dp))
                        }
                        Text(
                            col.name,
                            fontSize = 11.sp,
                            fontWeight = if (isSorted) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSorted) accent.primary else surfaceColors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════
// BROWSER CARD ROW
// ════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserCardRow(
    rowIndex: Int,
    card: KaiteyoCard,
    visibleColumns: Set<String>,
    columns: List<BrowserColumn>,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    contextMenuCard: KaiteyoCard?,
    onToggleSelect: (String) -> Unit,
    onClick: () -> Unit,
    onContextMenu: () -> Unit,
    onOpenDetails: () -> Unit,
    onFlagClick: (CardFlagType) -> Unit,
    onStatusClick: (CardStatus) -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    // Zebra striping: alternate rows get a whisper of surface tint so the
    // table reads as a real grid instead of floating text (Anki parity).
    val zebra = if (rowIndex % 2 == 1) surfaceColors.surfaceInteractive.copy(alpha = 0.28f)
    else androidx.compose.ui.graphics.Color.Transparent
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) accent.primary.copy(alpha = 0.08f) else zebra,
        animationSpec = tween(150)
    )

    Box {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .pointerInput(card.id) {
                // Desktop right-click opens the same row actions as the
                // selection bar — mirroring the Anki browser's context menu.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pressEvent = awaitPointerEvent()
                    val isSecondary = down.type == PointerType.Mouse &&
                        pressEvent.buttons.isSecondaryPressed
                    if (isSecondary) {
                        // Consume the press so the plain click cannot also fire.
                        down.consume()
                        onContextMenu()
                    }
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Release) break
                    }
                }
            }
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelect(card.id)
                    else onClick()
                },
                onLongClick = { onToggleSelect(card.id) }
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        columns.filter { it.id in visibleColumns }.forEach { col ->
            Box(
                modifier = Modifier.width(col.width.dp).padding(horizontal = 6.dp),
                contentAlignment = when (col.alignment) {
                    ColumnAlignment.Left -> Alignment.CenterStart
                    ColumnAlignment.Center -> Alignment.Center
                    ColumnAlignment.Right -> Alignment.CenterEnd
                }
            ) {
                when (col.id) {
                    "select" -> Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect(card.id) },
                        modifier = Modifier.size(20.dp)
                    )
                    "kanji" -> Text(card.character, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = surfaceColors.textPrimary,
                        fontFamily = FontFamily.Default)
                    "reading" -> Text(card.reading, fontSize = 11.sp, color = surfaceColors.textMuted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    "meaning" -> Text(card.meaning, fontSize = 12.sp, color = surfaceColors.textPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    "deck" -> Text(card.deck, fontSize = 11.sp, color = surfaceColors.textMuted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    "tags" -> {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            items(card.tagNames.take(3)) { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(accent.primary.copy(alpha = 0.1f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(tag, fontSize = 9.sp, color = accent.primary, maxLines = 1)
                                }
                            }
                            if (card.tagNames.size > 3) {
                                item {
                                    Text("+${card.tagNames.size - 3}", fontSize = 9.sp, color = surfaceColors.textMuted)
                                }
                            }
                        }
                    }
                    "flag" -> {
                        if (card.flag != CardFlagType.None) {
                            Box(
                                modifier = Modifier.size(16.dp).clip(CircleShape)
                                    .background(card.flag.colorFromHex())
                            )
                        }
                    }
                    "status" -> Text(card.status.displayName, fontSize = 10.sp,
                        color = statusColor(card.status))
                    "stability" -> Text(formatStability(card), fontSize = 11.sp, color = surfaceColors.textPrimary)
                    "difficulty" -> Text(formatDifficulty(card), fontSize = 11.sp, color = difficultyColor(card, surfaceColors, accent))
                    "due" -> Text(
                        when (card.status) {
                            CardStatus.New -> "New"
                            CardStatus.Learning -> "Learning"
                            CardStatus.Relearning -> "Relearning"
                            CardStatus.Young, CardStatus.Mature -> if (card.lastReviewed.isBlank()) "New" else "Due"
                            CardStatus.Suspended -> "Suspended"
                            CardStatus.Buried -> "Buried"
                            CardStatus.Archived -> "Archived"
                        },
                        fontSize = 11.sp,
                        color = statusColor(card.status)
                    )
                    "interval" -> Text(formatInterval(card.interval), fontSize = 11.sp, color = surfaceColors.textPrimary)
                    "ease" -> Text(formatFloat(card.ease, 1), fontSize = 11.sp, color = surfaceColors.textPrimary)
                    "reviews" -> Text("${card.reviewCount}", fontSize = 11.sp, color = surfaceColors.textPrimary)
                    "lapses" -> Text("${card.lapses}", fontSize = 11.sp, color = if (card.lapses > 0) LocalKaiteyoSemanticColors.current.error else surfaceColors.textMuted)
                    "created" -> Text(card.createdAt, fontSize = 10.sp, color = surfaceColors.textMuted)
                    "modified" -> Text(card.modifiedAt, fontSize = 10.sp, color = surfaceColors.textMuted)
                    "lastReview" -> Text(card.lastReviewed, fontSize = 10.sp, color = surfaceColors.textMuted)
                    "nextReview" -> Text("", fontSize = 10.sp, color = surfaceColors.textMuted)
                    "accuracy" -> Text("${(card.accuracy * 100).roundToInt()}%", fontSize = 11.sp, color = surfaceColors.textPrimary)
                    "timeStudied" -> Text(formatTimeMs(card.totalTimeStudied), fontSize = 11.sp, color = surfaceColors.textMuted)
                    "jlpt" -> Text("", fontSize = 11.sp, color = surfaceColors.textMuted)
                    "strokeCount" -> Text("", fontSize = 11.sp, color = surfaceColors.textMuted)
                    "frequency" -> Text("", fontSize = 11.sp, color = surfaceColors.textMuted)
                    "srsStage" -> Text("", fontSize = 11.sp, color = surfaceColors.textMuted)
                    "note" -> Text(card.notes, fontSize = 10.sp, color = surfaceColors.textMuted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    // Desktop right-click context menu — the same row actions as the
    // selection bar, mirroring the Anki browser's context menu.
    DropdownMenu(
        expanded = contextMenuCard?.id == card.id,
        onDismissRequest = onContextMenu,
        modifier = Modifier.background(surfaceColors.surfaceElevated)
    ) {
        DropdownMenuItem(
            text = { Text("Preview / details", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Visibility, null, Modifier.size(16.dp)) },
            onClick = {
                onContextMenu()
                onOpenDetails()
            }
        )
        DropdownMenuItem(
            text = { Text("Suspend", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Pause, null, Modifier.size(16.dp)) },
            onClick = {
                onContextMenu()
                onStatusClick(if (card.status == CardStatus.Suspended) CardStatus.New else CardStatus.Suspended)
            }
        )
        DropdownMenuItem(
            text = { Text("Bury", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.HideImage, null, Modifier.size(16.dp)) },
            onClick = {
                onContextMenu()
                onStatusClick(if (card.status == CardStatus.Buried) CardStatus.New else CardStatus.Buried)
            }
        )
        HorizontalDivider(color = surfaceColors.border.copy(alpha = 0.3f))
        CardFlagType.entries.filter { it != CardFlagType.None }.forEach { flag ->
            DropdownMenuItem(
                text = { Text("Flag ${flag.displayName}", fontSize = 12.sp) },
                leadingIcon = {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(flag.colorFromHex()))
                },
                onClick = {
                    onContextMenu()
                    onFlagClick(flag)
                }
            )
        }
        if (card.flag != CardFlagType.None) {
            DropdownMenuItem(
                text = { Text("Clear flag", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) },
                onClick = {
                    onContextMenu()
                    onFlagClick(CardFlagType.None)
                }
            )
        }
    }
    }
}

// ════════════════════════════════════════════
// SELECTION ACTION BAR
// ════════════════════════════════════════════

@Composable
private fun BrowserSelectionBar(
    selectedCount: Int,
    onDeselectAll: () -> Unit,
    onBulkTag: () -> Unit,
    onBulkFlag: () -> Unit,
    onBulkStatus: () -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surfaceColors.surfaceElevated,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$selectedCount selected", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = surfaceColors.textPrimary)
            Spacer(Modifier.weight(1f))
            FilledTonalButton(onClick = onBulkTag, modifier = Modifier.height(32.dp)) {
                Icon(Icons.Default.Label, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Tag", fontSize = 12.sp)
            }
            FilledTonalButton(onClick = onBulkFlag, modifier = Modifier.height(32.dp)) {
                Icon(Icons.Default.Flag, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Flag", fontSize = 12.sp)
            }
            FilledTonalButton(onClick = onBulkStatus, modifier = Modifier.height(32.dp)) {
                Icon(Icons.Default.SwapVert, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Status", fontSize = 12.sp)
            }
            IconButton(onClick = onDeselectAll, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "Deselect", Modifier.size(18.dp))
            }
        }
    }
}

// ════════════════════════════════════════════
// COLUMN PICKER DIALOG
// ════════════════════════════════════════════

@Composable
private fun ColumnPickerDialog(
    columns: List<BrowserColumn>,
    visibleColumns: Set<String>,
    onToggleColumn: (String) -> Unit,
    onDismiss: () -> Unit
) {
    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Visible Columns") },
        text = {
            Column {
                columns.filter { it.id != "select" }.forEach { col ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onToggleColumn(col.id) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = col.id in visibleColumns, onCheckedChange = { onToggleColumn(col.id) })
                        Spacer(Modifier.width(4.dp))
                        Text(col.name, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

// ════════════════════════════════════════════
// CARD DETAIL DIALOG
// ════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardDetailDialog(
    card: KaiteyoCard,
    onDismiss: () -> Unit,
    onFlagChange: (CardFlagType) -> Unit,
    onStatusChange: (CardStatus) -> Unit,
    onUpdate: (KaiteyoCard) -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    var editedCard by remember { mutableStateOf(card) }
    var isEditing by remember { mutableStateOf(false) }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(card.character, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(card.meaning, fontSize = 14.sp, color = surfaceColors.textMuted)
            }
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reading
                DetailField("Reading", card.reading)

                // Deck
                DetailField("Deck", card.deck)

                // Status with change option
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status: ", fontSize = 12.sp, color = surfaceColors.textMuted)
                    var expandedStatus by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expandedStatus = true }) {
                            Text(card.status.displayName, fontSize = 13.sp)
                        }
                        DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                            CardStatus.entries.forEach { status ->
                                DropdownMenuItem(text = { Text(status.displayName) },
                                    onClick = { onStatusChange(status); expandedStatus = false })
                            }
                        }
                    }
                }

                // Flag with change option
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Flag: ", fontSize = 12.sp, color = surfaceColors.textMuted)
                    if (card.flag != CardFlagType.None) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(card.flag.colorFromHex()))
                        Spacer(Modifier.width(4.dp))
                    }
                    var expandedFlag by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expandedFlag = true }) {
                            Text(if (card.flag == CardFlagType.None) "None" else card.flag.displayName, fontSize = 13.sp)
                        }
                        DropdownMenu(expanded = expandedFlag, onDismissRequest = { expandedFlag = false }) {
                            CardFlagType.entries.forEach { flag ->
                                DropdownMenuItem(
                                    text = { Text(flag.displayName) },
                                    onClick = { onFlagChange(flag); expandedFlag = false },
                                    leadingIcon = {
                                        if (flag != CardFlagType.None) {
                                            Box(Modifier.size(12.dp).clip(CircleShape).background(flag.colorFromHex()))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Tags
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tags: ", fontSize = 12.sp, color = surfaceColors.textMuted)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(card.tagNames) { tag ->
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                    .background(accent.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) { Text(tag, fontSize = 10.sp, color = accent.primary) }
                        }
                    }
                }

                HorizontalDivider()

                // Stats
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column { Text("Interval", fontSize = 10.sp, color = surfaceColors.textMuted); Text(formatInterval(card.interval), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = surfaceColors.textPrimary) }
                    Column { Text("Ease", fontSize = 10.sp, color = surfaceColors.textMuted); Text(formatFloat(card.ease, 1), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = surfaceColors.textPrimary) }
                    Column { Text("Reviews", fontSize = 10.sp, color = surfaceColors.textMuted); Text("${card.reviewCount}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = surfaceColors.textPrimary) }
                    Column { Text("Lapses", fontSize = 10.sp, color = surfaceColors.textMuted); Text("${card.lapses}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = surfaceColors.textPrimary) }
                    Column { Text("Accuracy", fontSize = 10.sp, color = surfaceColors.textMuted); Text("${(card.accuracy * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = surfaceColors.textPrimary) }
                }

                HorizontalDivider()

                // Notes
                if (card.notes.isNotBlank()) {
                    Text("Notes", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = surfaceColors.textPrimary)
                    Text(card.notes, fontSize = 12.sp, color = surfaceColors.textMuted)
                }

                // Dates
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column { Text("Created", fontSize = 10.sp, color = surfaceColors.textMuted); Text(card.createdAt, fontSize = 11.sp, color = surfaceColors.textPrimary) }
                    Column { Text("Modified", fontSize = 10.sp, color = surfaceColors.textMuted); Text(card.modifiedAt, fontSize = 11.sp, color = surfaceColors.textPrimary) }
                    Column { Text("Last Review", fontSize = 10.sp, color = surfaceColors.textMuted); Text(card.lastReviewed, fontSize = 11.sp, color = surfaceColors.textPrimary) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun DetailField(label: String, value: String) {
    val surfaceColors = LocalSurfaceColors.current
    Row {
        Text("$label: ", fontSize = 12.sp, color = surfaceColors.textMuted)
        Text(value, fontSize = 13.sp, color = surfaceColors.textPrimary)
    }
}

// ════════════════════════════════════════════
// BROWSER FILTER RAIL — Anki-style sidebar
// Saved searches, flags, card state and the
// deck tree, each narrowing the table.
// ════════════════════════════════════════════

@Composable
private fun BrowserFilterRail(
    decks: List<KaiteyoDeck>,
    uniqueDecks: List<String>,
    uniqueTags: List<String>,
    cards: List<KaiteyoCard>,
    railQuery: String,
    onRailQueryChange: (String) -> Unit,
    customSearches: List<SavedSearch>,
    onSaveCustomSearch: (String) -> Unit,
    onRemoveCustomSearch: (SavedSearch) -> Unit,
    flagFilter: CardFlagType?,
    anyFlagFilter: Boolean,
    statusFilter: Set<CardStatus>,
    deckFilter: String?,
    tagFilter: String?,
    onFlagFilterChange: (CardFlagType?) -> Unit,
    onAnyFlagFilterChange: (Boolean) -> Unit,
    onStatusFilterChange: (Set<CardStatus>) -> Unit,
    onDeckFilterChange: (String?) -> Unit,
    onTagFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    val hasActiveFilter = flagFilter != null || anyFlagFilter || statusFilter.isNotEmpty() || deckFilter != null || tagFilter != null
    val q = railQuery.trim().lowercase()
    // Mirror the parent's derivation: the kanji-browser pseudo-deck is not a
    // study deck, so it never contributes to filter counts.
    val studyCards = remember(cards) { cards.filter { it.deck != KANJI_BROWSER_DECK_NAME } }

    Column(
        modifier = Modifier
            .width(224.dp)
            .fillMaxHeight()
            .background(surfaceColors.surface)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Browse", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
            Spacer(Modifier.weight(1f))
            if (hasActiveFilter) {
                TextButton(
                    onClick = onClearFilters,
                    modifier = Modifier.height(24.dp)
                ) { Text("Clear", fontSize = 11.sp, color = accent.primary) }
            }
        }

        // Sidebar filter — narrows everything below by text (Anki's sidebar
        // filter box).
        OutlinedTextField(
            value = railQuery,
            onValueChange = onRailQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .height(36.dp),
            placeholder = { Text("Filter sidebar…", fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(14.dp)) },
            trailingIcon = {
                if (railQuery.isNotBlank()) {
                    IconButton(onClick = { onRailQueryChange("") }, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Default.Close, "Clear sidebar filter", Modifier.size(12.dp))
                    }
                }
            },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 12.sp,
                color = surfaceColors.textPrimary
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = surfaceColors.border.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp)
        )

        // Today — Anki-style quick day filters with live counts.
        val todayChips = todaySearchChips().filter { it.label.contains(q, ignoreCase = true) }
        if (todayChips.isNotEmpty()) {
            FilterRailSection("Today", surfaceColors, accent) {
                todayChips.forEach { search ->
                    FilterRailChip(
                        label = search.label,
                        selected = search.isActive(flagFilter, anyFlagFilter, statusFilter, deckFilter, tagFilter),
                        onClick = { search.apply(
                            onFlagFilterChange, onAnyFlagFilterChange, onStatusFilterChange, onDeckFilterChange, onTagFilterChange
                        ) },
                        count = search.countOf(studyCards),
                        surfaceColors = surfaceColors,
                        accent = accent
                    )
                }
            }
        }

        // Saved searches — each entry shows its live card count, mirroring
        // Anki's browser sidebar. A "＋" captures the current filter combo as
        // a custom search the user can jump back to at any time.
        val savedSearches = savedSearchChips().filter { it.label.contains(q, ignoreCase = true) }
        val customMatches = customSearches.filter { it.label.contains(q, ignoreCase = true) }
        if (savedSearches.isNotEmpty() || customMatches.isNotEmpty()) {
            FilterRailSection(
                title = "Saved Searches",
                surfaceColors = surfaceColors,
                accent = accent,
                trailing = {
                    var showSaveDialog by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.Add, "Save current search", Modifier.size(14.dp), tint = accent.primary)
                    }
                    if (showSaveDialog) {
                        SaveSearchDialog(
                            suggestedLabel = buildString {
                                if (deckFilter != null) append("$deckFilter ")
                                if (statusFilter.isNotEmpty()) append(statusFilter.joinToString("+") { it.displayName })
                                if (flagFilter != null) append(" ${flagFilter.displayName}")
                            }.trim().ifBlank { "Custom search" },
                            onSave = { label ->
                                showSaveDialog = false
                                onSaveCustomSearch(label)
                            },
                            onDismiss = { showSaveDialog = false }
                        )
                    }
                }
            ) {
                savedSearches.forEach { search ->
                    FilterRailChip(
                        label = search.label,
                        selected = search.isActive(flagFilter, anyFlagFilter, statusFilter, deckFilter, tagFilter),
                        onClick = { search.apply(
                            onFlagFilterChange, onAnyFlagFilterChange, onStatusFilterChange, onDeckFilterChange, onTagFilterChange
                        ) },
                        count = search.countOf(studyCards),
                        surfaceColors = surfaceColors,
                        accent = accent
                    )
                }
                if (customMatches.isNotEmpty()) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = surfaceColors.border.copy(alpha = 0.3f))
                    customMatches.forEach { search ->
                        FilterRailChip(
                            label = search.label,
                            selected = search.isActive(flagFilter, anyFlagFilter, statusFilter, deckFilter, tagFilter),
                            onClick = { search.apply(
                                onFlagFilterChange, onAnyFlagFilterChange, onStatusFilterChange, onDeckFilterChange, onTagFilterChange
                            ) },
                            count = search.countOf(cards),
                            surfaceColors = surfaceColors,
                            accent = accent,
                            trailing = {
                                IconButton(
                                    onClick = { onRemoveCustomSearch(search) },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(Icons.Default.Close, "Remove search", Modifier.size(10.dp), tint = surfaceColors.textMuted)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Flags
        val flagOptions = CardFlagType.entries.filter { it.displayName.contains(q, ignoreCase = true) }
        if (flagOptions.isNotEmpty()) {
            FilterRailSection("Flags", surfaceColors, accent) {
                flagOptions.forEach { flag ->
                    FilterRailChip(
                        label = flag.displayName,
                        selected = flagFilter == flag,
                        onClick = { onFlagFilterChange(if (flagFilter == flag) null else flag) },
                        leadingDot = if (flag != CardFlagType.None) flag.colorFromHex() else null,
                        count = cards.count { it.flag == flag },
                        surfaceColors = surfaceColors,
                        accent = accent
                    )
                }
            }
        }

        // Card state — multi-select, each with a live count.
        val stateOptions = CardStatus.entries.filter { it.displayName.contains(q, ignoreCase = true) }
        if (stateOptions.isNotEmpty()) {
            FilterRailSection("Card State", surfaceColors, accent) {
                stateOptions.forEach { status ->
                    FilterRailChip(
                        label = status.displayName,
                        selected = status in statusFilter,
                        onClick = {
                            onStatusFilterChange(
                                if (status in statusFilter) statusFilter - status
                                else statusFilter + status
                            )
                        },
                        count = studyCards.count { it.status == status },
                        surfaceColors = surfaceColors,
                        accent = accent
                    )
                }
            }
        }

        // Decks — nested expandable tree with live card counts (Anki-style).
        // Falls back to a flat list of deck names when no deck objects are
        // supplied (e.g. day-practice presets that only pass cards).
        val matchingDecks = decks.filter { it.name.contains(q, ignoreCase = true) }
        val matchingUniqueDecks = uniqueDecks.filter { it.contains(q, ignoreCase = true) }
        if (matchingDecks.isNotEmpty() || (decks.isEmpty() && matchingUniqueDecks.isNotEmpty())) {
            val expandedDecks = remember { mutableStateOf(if (matchingDecks.size <= 8) matchingDecks.map { it.id }.toSet() else setOf<String>()) }
            FilterRailSection("Decks", surfaceColors, accent) {
                if (matchingDecks.isNotEmpty()) {
                    BrowserDeckTree(
                        decks = matchingDecks,
                        cards = cards,
                        deckFilter = deckFilter,
                        filterQuery = q,
                        expandedIds = expandedDecks.value,
                        onToggleExpanded = { id ->
                            expandedDecks.value = if (id in expandedDecks.value) expandedDecks.value - id
                            else expandedDecks.value + id
                        },
                        onSelect = { name -> onDeckFilterChange(if (deckFilter == name) null else name) },
                        surfaceColors = surfaceColors,
                        accent = accent
                    )
                } else {
                    matchingUniqueDecks.forEach { deck ->
                        FilterRailChip(
                            label = deck,
                            selected = deckFilter == deck,
                            onClick = { onDeckFilterChange(if (deckFilter == deck) null else deck) },
                            surfaceColors = surfaceColors,
                            accent = accent
                        )
                    }
                }
            }
        }

        // Tags
        val matchingTags = uniqueTags.filter { it.contains(q, ignoreCase = true) }
        if (matchingTags.isNotEmpty()) {
            FilterRailSection("Tags", surfaceColors, accent) {
                matchingTags.forEach { tag ->
                    FilterRailChip(
                        label = tag,
                        selected = tagFilter == tag,
                        onClick = { onTagFilterChange(if (tagFilter == tag) null else tag) },
                        surfaceColors = surfaceColors,
                        accent = accent
                    )
                }
            }
        }
    }
}

private data class SavedSearch(
    val label: String,
    val icon: String = "",
    val flag: CardFlagType? = null,
    val anyFlag: Boolean = false,
    val statuses: List<CardStatus> = emptyList(),
    val deck: String? = null
) {
    fun matchesCard(card: KaiteyoCard): Boolean = when {
        flag != null -> card.flag == flag
        anyFlag -> card.flag != CardFlagType.None
        statuses.isNotEmpty() -> card.status in statuses
        deck != null -> card.deck == deck
        else -> true
    }

    fun isActive(
        flagFilter: CardFlagType?,
        anyFlagFilter: Boolean,
        statusFilter: Set<CardStatus>,
        deckFilter: String?,
        tagFilter: String?
    ): Boolean {
        val statusMatch = if (statuses.isEmpty()) {
            statusFilter.isEmpty()
        } else {
            statusFilter.isNotEmpty() && statusFilter.all { it in statuses } && statuses.all { it in statusFilter }
        }
        return flagFilter == flag && anyFlagFilter == anyFlag &&
            statusMatch && deckFilter == deck && tagFilter == null
    }

    fun apply(
        onFlagFilterChange: (CardFlagType?) -> Unit,
        onAnyFlagFilterChange: (Boolean) -> Unit,
        onStatusFilterChange: (Set<CardStatus>) -> Unit,
        onDeckFilterChange: (String?) -> Unit,
        onTagFilterChange: (String?) -> Unit
    ) {
        onFlagFilterChange(flag)
        onAnyFlagFilterChange(anyFlag)
        onStatusFilterChange(statuses.toSet())
        onDeckFilterChange(deck)
        onTagFilterChange(null)
    }

    /** Live count of cards matching this saved search. */
    fun countOf(cards: List<KaiteyoCard>): Int = cards.count(::matchesCard)
}

/** Anki-style saved searches. */
private fun savedSearchChips(): List<SavedSearch> = listOf(
    SavedSearch("All cards"),
    SavedSearch("New", statuses = listOf(CardStatus.New)),
    SavedSearch("Learning", statuses = listOf(CardStatus.Learning)),
    SavedSearch("Review", statuses = listOf(CardStatus.Mature)),
    SavedSearch("Young", statuses = listOf(CardStatus.Young)),
    SavedSearch("Relearning", statuses = listOf(CardStatus.Relearning)),
    SavedSearch("Suspended", statuses = listOf(CardStatus.Suspended)),
    SavedSearch("Buried", statuses = listOf(CardStatus.Buried)),
    SavedSearch("Flagged", anyFlag = true)
)

/** Anki-style "Today" quick filters. */
private fun todaySearchChips(): List<SavedSearch> = listOf(
    SavedSearch("Due", statuses = listOf(CardStatus.Young, CardStatus.Mature)),
    SavedSearch("New", statuses = listOf(CardStatus.New)),
    SavedSearch("Learning", statuses = listOf(CardStatus.Learning, CardStatus.Relearning)),
    SavedSearch("Suspended", statuses = listOf(CardStatus.Suspended)),
    SavedSearch("Buried", statuses = listOf(CardStatus.Buried))
)

/**
 * Splits a browser query into structured filters plus free text:
 *   tag:jlpt  flag:red  deck:N5  status:learning  due:today
 * Unknown prefixes are treated as plain text.
 */
private data class BrowserQuery(
    val text: String = "",
    val tag: String? = null,
    val flag: CardFlagType? = null,
    val deck: String? = null,
    val status: CardStatus? = null,
    val due: String? = null
)

private fun parseBrowserQuery(raw: String): BrowserQuery {
    var textParts = mutableListOf<String>()
    var tag: String? = null
    var flag: CardFlagType? = null
    var deck: String? = null
    var status: CardStatus? = null
    var due: String? = null

    raw.split(Regex("\\s+")).forEach { token ->
        val colon = token.indexOf(':')
        if (colon > 0) {
            val key = token.substring(0, colon).lowercase()
            val value = token.substring(colon + 1).trim().lowercase()
            if (value.isEmpty()) {
                textParts += token
            } else when (key) {
                "tag", "tags" -> tag = value
                "flag" -> flag = CardFlagType.entries.firstOrNull { it.displayName.equals(value, ignoreCase = true) }
                "deck", "decks" -> deck = value
                "status", "state" -> status = CardStatus.entries.firstOrNull { it.displayName.equals(value, ignoreCase = true) }
                "due", "is" -> due = value
                else -> textParts += token
            }
        } else {
            textParts += token
        }
    }

    return BrowserQuery(
        text = textParts.joinToString(" "),
        tag = tag,
        flag = flag,
        deck = deck,
        status = status,
        due = due
    )
}

@Composable
private fun FilterRailSection(
    title: String,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = surfaceColors.textMuted,
                letterSpacing = 0.8.sp,
                modifier = Modifier.weight(1f)
            )
            if (trailing != null) trailing()
        }
        content()
    }
}

@Composable
private fun FilterRailChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingDot: Color? = null,
    count: Int? = null,
    trailing: (@Composable () -> Unit)? = null,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) accent.primary.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingDot?.let { dotColor ->
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(Modifier.width(7.dp))
        }
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) accent.primary else surfaceColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (count != null) {
            Text(
                text = count.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) accent.primary.copy(alpha = 0.9f) else surfaceColors.textMuted
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(2.dp))
            trailing()
        }
    }
}

// ════════════════════════════════════════════
// DECK TREE — nested expandable tree (Anki-style)
// ════════════════════════════════════════════

@Composable
private fun BrowserDeckTree(
    decks: List<KaiteyoDeck>,
    cards: List<KaiteyoCard>,
    deckFilter: String?,
    filterQuery: String = "",
    expandedIds: Set<String>,
    onToggleExpanded: (String) -> Unit,
    onSelect: (String) -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    val rootDecks = decks.filter { it.parentId == null || decks.none { d -> d.id == it.parentId } }
    // Cards may carry deck names not present in the deck objects; fold them in.
    val cardDeckNames = cards.map { it.deck }.distinct().sorted()
    val allRoots = (rootDecks.map { it.name } + cardDeckNames).distinct()
        .filter { it.contains(filterQuery, ignoreCase = true) }

    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        // "All decks" pseudo-root
        FilterRailChip(
            label = "All decks (${cards.size})",
            selected = deckFilter == null,
            onClick = { onSelect("") },
            surfaceColors = surfaceColors,
            accent = accent
        )

        allRoots.forEach { rootName ->
            val deck = decks.firstOrNull { it.name == rootName }
            BrowserDeckNode(
                name = rootName,
                deck = deck,
                cards = cards,
                deckFilter = deckFilter,
                filterQuery = filterQuery,
                depth = 0,
                expandedIds = expandedIds,
                onToggleExpanded = onToggleExpanded,
                onSelect = onSelect,
                surfaceColors = surfaceColors,
                accent = accent
            )
        }
    }
}

@Composable
private fun BrowserDeckNode(
    name: String,
    deck: KaiteyoDeck?,
    cards: List<KaiteyoCard>,
    deckFilter: String?,
    filterQuery: String = "",
    depth: Int,
    expandedIds: Set<String>,
    onToggleExpanded: (String) -> Unit,
    onSelect: (String) -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    val count = cards.count { it.deck == name }
    val hasChildren = deck?.children?.isNotEmpty() == true
    val expanded = deck == null || deck.id in expandedIds
    val selected = deckFilter == name

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) accent.primary.copy(alpha = 0.14f) else Color.Transparent)
            .clickable { if (hasChildren) onToggleExpanded(deck!!.id) else onSelect(name) }
            .padding(start = (8 + depth * 12).dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasChildren) {
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = surfaceColors.textMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(2.dp))
        } else {
            Spacer(Modifier.width(16.dp))
        }
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) accent.primary else surfaceColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (count > 0) {
            Text(
                text = count.toString(),
                fontSize = 10.sp,
                color = surfaceColors.textMuted
            )
        }
    }

    if (hasChildren && expanded) {
        deck!!.children
            .filter { it.name.contains(filterQuery, ignoreCase = true) }
            .forEach { child ->
                BrowserDeckNode(
                    name = child.name,
                    deck = child,
                    cards = cards,
                    deckFilter = deckFilter,
                    filterQuery = filterQuery,
                    depth = depth + 1,
                    expandedIds = expandedIds,
                    onToggleExpanded = onToggleExpanded,
                    onSelect = onSelect,
                    surfaceColors = surfaceColors,
                    accent = accent
                )
            }
    }
}

// ════════════════════════════════════════════
// BROWSER NOTE EDITOR PANE — Anki-style editor
// Fields from the note type, cloze support and
// a live preview, saving back through onUpdate.
// ════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserNoteEditorPane(
    card: KaiteyoCard,
    onUpdate: (KaiteyoCard) -> Unit,
    onClose: () -> Unit,
    onShowDetails: () -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    val allNoteTypes = remember { defaultKaiteyoNoteTypes }
    val noteType = remember(card.noteTypeId(), allNoteTypes) {
        allNoteTypes.firstOrNull { it.id == card.noteTypeId() } ?: allNoteTypes.first()
    }

    var expression by remember(card.id) { mutableStateOf(card.character) }
    var reading by remember(card.id) { mutableStateOf(card.reading) }
    var meaning by remember(card.id) { mutableStateOf(card.meaning) }
    var tags by remember(card.id) { mutableStateOf(card.tagNames.joinToString(", ")) }
    var notes by remember(card.id) { mutableStateOf(card.notes) }
    var customValues by remember(card.id) { mutableStateOf(card.customFields.toMap()) }
    var noteTypeExpanded by remember { mutableStateOf(false) }
    var showNoteTypeDialog by remember { mutableStateOf(false) }

    fun currentCard(): KaiteyoCard {
        val updatedTags = tags.split(',').map { it.trim() }.filter { it.isNotBlank() }
        return card.copy(
            character = expression,
            reading = reading,
            meaning = meaning,
            notes = notes,
            tagNames = updatedTags.toMutableList(),
            customFields = customValues.toMutableMap()
        )
    }

    Column(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(surfaceColors.surfaceElevated)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Pane header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Note", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                Text(card.deck, fontSize = 10.sp, color = surfaceColors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onShowDetails, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Info, "Card details", Modifier.size(16.dp), tint = surfaceColors.textMuted)
            }
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "Close editor", Modifier.size(16.dp), tint = surfaceColors.textMuted)
            }
        }

        HorizontalDivider(color = surfaceColors.border.copy(alpha = 0.4f))

        // Note type selector
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(surfaceColors.surfaceInteractive)
                    .clickable { noteTypeExpanded = true }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Description, null, Modifier.size(14.dp), tint = accent.primary)
                Spacer(Modifier.width(6.dp))
                Text("Note type: ${noteType.name}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = surfaceColors.textPrimary)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(16.dp), tint = surfaceColors.textMuted)
            }
            DropdownMenu(expanded = noteTypeExpanded, onDismissRequest = { noteTypeExpanded = false }) {
                allNoteTypes.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(type.name, fontSize = 13.sp)
                                if (type.description.isNotBlank()) {
                                    Text(type.description, fontSize = 10.sp, color = surfaceColors.textMuted, maxLines = 2)
                                }
                            }
                        },
                        trailingIcon = {
                            if (type.id == card.noteTypeId()) {
                                Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = accent.primary)
                            }
                        },
                        onClick = {
                            customValues = customValues + ("noteType" to type.id)
                            noteTypeExpanded = false
                            onUpdate(currentCard())
                        }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Note types…", fontSize = 13.sp, color = accent.primary) },
                    onClick = { noteTypeExpanded = false; showNoteTypeDialog = true }
                )
            }
        }

        // Editable fields — the core Anki-style editor body
        BrowserNoteField(
            label = "Expression",
            value = expression,
            onValueChange = { expression = it },
            surfaceColors = surfaceColors,
            accent = accent
        )
        BrowserNoteField(
            label = "Reading",
            value = reading,
            onValueChange = { reading = it },
            surfaceColors = surfaceColors,
            accent = accent
        )
        BrowserNoteField(
            label = "Meaning",
            value = meaning,
            onValueChange = { meaning = it },
            surfaceColors = surfaceColors,
            accent = accent
        )

        // Extra fields from the note type (stored in customFields)
        noteType.fields.filter { field ->
            field.id !in setOf("expression", "reading", "meaning", "notes")
        }.forEach { field ->
            val value = customValues["field:${field.id}"] ?: ""
            if (field.kind == NoteFieldKind.Cloze) {
                ClozeField(
                    label = field.label,
                    value = value,
                    onValueChange = { newValue ->
                        customValues = customValues + ("field:${field.id}" to newValue)
                    },
                    surfaceColors = surfaceColors,
                    accent = accent
                )
            } else {
                BrowserNoteField(
                    label = field.label,
                    value = value,
                    onValueChange = { newValue ->
                        customValues = customValues + ("field:${field.id}" to newValue)
                    },
                    surfaceColors = surfaceColors,
                    accent = accent
                )
            }
        }

        // Tags
        BrowserNoteField(
            label = "Tags (comma separated)",
            value = tags,
            onValueChange = { tags = it },
            surfaceColors = surfaceColors,
            accent = accent
        )

        // Notes (markdown)
        Text("Notes", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textMuted)
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
            placeholder = { Text("Markdown supported…", fontSize = 11.sp) },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = surfaceColors.textPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = surfaceColors.border.copy(alpha = 0.3f),
                cursorColor = accent.primary
            ),
            shape = RoundedCornerShape(8.dp)
        )

        // Save row — Anki keeps the action bar pinned below the fields.
        FilledTonalButton(
            onClick = { onUpdate(currentCard()) },
            modifier = Modifier.fillMaxWidth().height(34.dp)
        ) {
            Icon(Icons.Default.Save, null, Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Save note", fontSize = 12.sp)
        }

        // Live card preview — always visible below the fields (Anki-style).
        Text("Preview", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textMuted)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = surfaceColors.surface),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Front", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textMuted)
                Text(expression.ifBlank { "—" }, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                if (reading.isNotBlank()) {
                    Text(reading, fontSize = 13.sp, color = surfaceColors.textMuted)
                }
                if (notes.hasClozeDeletions()) {
                    Text(
                        "Cloze: " + Regex("""\{\{c\d+::""").findAll(notes).count() + " deletion(s)",
                        fontSize = 10.sp,
                        color = accent.primary
                    )
                }
                HorizontalDivider(color = surfaceColors.border.copy(alpha = 0.4f))
                Text("Back", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textMuted)
                Text(meaning.ifBlank { "—" }, fontSize = 13.sp, color = surfaceColors.textPrimary)
                if (notes.isNotBlank()) {
                    Text(notes, fontSize = 11.sp, color = surfaceColors.textMuted)
                }
                if (tagNamesDisplay(tags).isNotEmpty()) {
                    Text("Tags: ${tagNamesDisplay(tags).joinToString(", ")}", fontSize = 10.sp, color = surfaceColors.textMuted)
                }
            }
        }
    }

    if (showNoteTypeDialog) {
        NoteTypeInfoDialog(onDismiss = { showNoteTypeDialog = false })
    }
}

private fun tagNamesDisplay(tags: String): List<String> =
    tags.split(',').map { it.trim() }.filter { it.isNotBlank() }

@Composable
private fun BrowserNoteField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textMuted)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = surfaceColors.textPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent.primary.copy(alpha = 0.5f),
            unfocusedBorderColor = surfaceColors.border.copy(alpha = 0.3f),
            cursorColor = accent.primary
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun ClozeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    // A real TextFieldValue so selection is tracked: cloze wraps exactly the
    // selected part of the sentence (or the whole text when nothing is
    // selected), Anki-style.
    var fieldValue by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(value)) }

    fun commit(text: String) {
        fieldValue = fieldValue.copy(text = text, selection = androidx.compose.ui.text.TextRange(text.length))
        onValueChange(text)
    }

    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textMuted)
    OutlinedTextField(
        value = fieldValue,
        onValueChange = { fieldValue = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("あなたは{{c1::日本人}}です。", fontSize = 11.sp) },
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = surfaceColors.textPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent.primary.copy(alpha = 0.5f),
            unfocusedBorderColor = surfaceColors.border.copy(alpha = 0.3f),
            cursorColor = accent.primary
        ),
        shape = RoundedCornerShape(8.dp)
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            onClick = {
                val (updated, _) = insertCloze(
                    fieldValue.text,
                    fieldValue.selection.min,
                    fieldValue.selection.max
                )
                commit(updated)
            },
            modifier = Modifier.height(28.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null, Modifier.size(14.dp), tint = accent.primary)
            Spacer(Modifier.width(4.dp))
            Text("Cloze", fontSize = 11.sp, color = accent.primary)
        }
        Spacer(Modifier.weight(1f))
        Text(
            if (value.hasClozeDeletions()) "${Regex("""\{\{c\d+::""").findAll(value).count()} deletions" else "No deletions",
            fontSize = 10.sp,
            color = if (value.hasClozeDeletions()) accent.primary else surfaceColors.textMuted
        )
    }
}

@Composable
private fun NoteTypeInfoDialog(onDismiss: () -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Note Types") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "A note type defines the fields a flashcard carries. Kaiteyo ships with a default type (front expression, back reading + meaning + example + audio) with full cloze support — use {{c1::…}} around any part of a sentence to hide it.",
                    fontSize = 12.sp
                )
                defaultKaiteyoNoteTypes.forEach { type ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(type.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(type.description, fontSize = 11.sp)
                            Text(
                                type.fields.joinToString(" · ") { it.label },
                                fontSize = 10.sp,
                                color = surfaceColors.textMuted
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } }
    )
}

// ── Stability / difficulty helpers (Anki-style columns) ──

private fun formatStability(card: KaiteyoCard): String = when {
    card.reviewCount == 0 -> "—"
    card.interval <= 0 -> "(new)"
    card.interval < 30 -> "${card.interval}d"
    card.interval < 365 -> "${(card.interval / 30.4).roundToInt()}mo"
    else -> "${(card.interval / 365.25).roundToInt()}y"
}

private fun formatDifficulty(card: KaiteyoCard): String {
    if (card.reviewCount == 0) return "—"
    // Derive a 0–100 style difficulty from accuracy + lapses, matching Anki's
    // "difficulty %" column semantics (lower = easier).
    val lapsePenalty = card.lapses * 4f
    val difficulty = ((1f - card.accuracy) * 100f + lapsePenalty).coerceIn(0f, 100f)
    return "${difficulty.roundToInt()}%"
}

@Composable
private fun difficultyColor(
    card: KaiteyoCard,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
): Color {
    val sem = LocalKaiteyoSemanticColors.current
    return when {
        card.reviewCount == 0 -> surfaceColors.textMuted
        card.lapses > 0 -> sem.error
        card.accuracy >= 0.9f -> accent.primary
        card.accuracy >= 0.7f -> sem.warning
        else -> sem.info
    }
}

// ════════════════════════════════════════════
// BULK ACTION DIALOGS — one flag / status / tag
// applied to every selected card in a single step
// ════════════════════════════════════════════

@Composable
private fun BulkFlagDialog(
    onPick: (CardFlagType) -> Unit,
    onDismiss: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Flag on Selected") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Choose one flag color to apply to every selected card.",
                    fontSize = 12.sp,
                    color = surfaceColors.textMuted
                )
                // Flag palette — 4 per row, Anki-style
                CardFlagType.entries.chunked(4).forEach { rowFlags ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowFlags.forEach { flag ->
                            val color = flag.colorFromHex()
                            val isNone = flag == CardFlagType.None
                            Column(
                                modifier = Modifier.weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isNone) surfaceColors.surfaceElevated
                                        else color.copy(alpha = 0.18f)
                                    )
                                    .clickable { onPick(flag) }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (isNone) Color.Transparent else color)
                                        .then(
                                            if (isNone) Modifier.border(1.dp, surfaceColors.textMuted, CircleShape)
                                            else Modifier
                                        )
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    flag.displayName,
                                    fontSize = 10.sp,
                                    color = if (isNone) surfaceColors.textMuted else surfaceColors.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun BulkStatusDialog(
    onPick: (CardStatus) -> Unit,
    onDismiss: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Status of Selected") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Apply a card state to every selected card.",
                    fontSize = 12.sp,
                    color = surfaceColors.textMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                CardStatus.entries.forEach { status ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPick(status) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor(status))
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(status.displayName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.Default.ChevronRight,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = surfaceColors.textMuted
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun BulkTagDialog(
    onApply: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var tagInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Tags to Selected") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Tags are appended to every selected card. Separate multiple tags with commas or spaces.",
                    fontSize = 12.sp,
                    color = surfaceColors.textMuted
                )
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = {
                        tagInput = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. jlpt-n5, travel, food") },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        val tags = tagInput.split(",", " ", "、")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                        if (tags.isEmpty()) error = "Enter at least one tag."
                        else onApply(tags)
                    }),
                    isError = error != null,
                    supportingText = error?.let { { Text(it, fontSize = 11.sp) } }
                )
                if (tagInput.isNotBlank()) {
                    val preview = tagInput.split(",", " ", "、")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    if (preview.isNotEmpty()) {
                        Text(
                            "Will add: ${preview.joinToString(" · ") { it }}",
                            fontSize = 11.sp,
                            color = accent.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val tags = tagInput.split(",", " ", "、")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    if (tags.isEmpty()) error = "Enter at least one tag."
                    else onApply(tags)
                }
            ) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SaveSearchDialog(
    suggestedLabel: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var label by remember { mutableStateOf(suggestedLabel) }
    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save current search") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "This captures the active filters (deck, status, flag) so you can return to them with one click.",
                    fontSize = 12.sp,
                    color = surfaceColors.textMuted
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search name", fontSize = 13.sp) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (label.isNotBlank()) onSave(label.trim())
                    })
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (label.isNotBlank()) onSave(label.trim()) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun statusColor(status: CardStatus): Color {
    val sem = LocalKaiteyoSemanticColors.current
    return when (status) {
        CardStatus.New -> sem.cardNew
        CardStatus.Learning -> sem.cardLearning
        CardStatus.Young -> sem.cardYoung
        CardStatus.Mature -> sem.cardMature
        CardStatus.Relearning -> sem.cardRelearning
        CardStatus.Suspended -> sem.cardSuspended
        CardStatus.Buried -> sem.cardBuried
        CardStatus.Archived -> sem.cardArchived
    }
}
