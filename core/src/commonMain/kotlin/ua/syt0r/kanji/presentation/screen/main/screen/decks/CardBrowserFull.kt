package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
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
    var statusFilter by remember { mutableStateOf<CardStatus?>(null) }
    var deckFilter by remember { mutableStateOf<String?>(null) }
    var tagFilter by remember { mutableStateOf<String?>(null) }
    var showCardDetail by remember { mutableStateOf<KaiteyoCard?>(null) }
    var visibleColumns by remember { mutableStateOf(columns.filter { it.isVisible }.map { it.id }.toSet()) }

    // ── Filtered & Sorted Cards ──
    val processedCards = remember(cards, searchQuery, flagFilter, statusFilter, deckFilter, tagFilter, sortColumn, sortAscending) {
        var result = cards

        // Text search
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            result = result.filter { card ->
                card.character.lowercase().contains(q) ||
                card.meaning.lowercase().contains(q) ||
                card.reading.lowercase().contains(q) ||
                card.deck.lowercase().contains(q) ||
                card.tagNames.any { it.lowercase().contains(q) } ||
                card.notes.lowercase().contains(q) ||
                card.flag.displayName.lowercase().contains(q) ||
                card.status.displayName.lowercase().contains(q) ||
                card.id.lowercase().contains(q)
            }
        }

        // Filters
        flagFilter?.let { flag -> result = result.filter { it.flag == flag } }
        statusFilter?.let { status -> result = result.filter { it.status == status } }
        deckFilter?.let { deck -> result = result.filter { it.deck == deck } }
        tagFilter?.let { tag -> result = result.filter { it.tagNames.contains(tag) } }

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
    val uniqueDecks = remember(cards) { cards.map { it.deck }.distinct().sorted() }
    val uniqueTags = remember(cards) { cards.flatMap { it.tagNames }.distinct().sorted() }
    val totalCards = cards.size
    val filteredCount = processedCards.size
    val selectedCount = selectedCardIds.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card Browser") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    IconButton(onClick = { showColumnPicker = true }) { Icon(Icons.Default.ViewColumn, "Columns") }
                    IconButton(onClick = { showFilterPanel = !showFilterPanel }) {
                        Icon(Icons.Default.FilterList, "Filters",
                            tint = if (flagFilter != null || statusFilter != null || deckFilter != null || tagFilter != null)
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
        Column(Modifier.fillMaxSize().padding(padding)) {
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
                flagFilter = flagFilter,
                statusFilter = statusFilter,
                deckFilter = deckFilter,
                tagFilter = tagFilter,
                onFlagFilterChange = { flagFilter = it },
                onStatusFilterChange = { statusFilter = it },
                onDeckFilterChange = { deckFilter = it },
                onTagFilterChange = { tagFilter = it },
                onClearFilters = {
                    flagFilter = null; statusFilter = null
                    deckFilter = null; tagFilter = null
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
                        Text(if (searchQuery.isNotBlank()) "No cards match your search" else "No cards to display",
                            color = surfaceColors.textMuted)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = rememberLazyListState()
                ) {
                    items(processedCards, key = { it.id }) { card ->
                        BrowserCardRow(
                            card = card,
                            visibleColumns = visibleColumns,
                            columns = columns,
                            isSelected = card.id in selectedCardIds,
                            isSelectionMode = isSelectionMode,
                            onToggleSelect = { id ->
                                selectedCardIds = if (id in selectedCardIds) selectedCardIds - id
                                else selectedCardIds + id
                            },
                            onClick = { showCardDetail = card },
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
                    onDelete = {
                        // Delete selected
                    },
                    onExport = { /* Export selected */ },
                    onBulkTag = { /* Bulk tag */ },
                    onBulkFlag = { /* Bulk flag */ },
                    onBulkStatus = { /* Bulk status */ },
                    surfaceColors = surfaceColors,
                    accent = accent
                )
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
    flagFilter: CardFlagType?,
    statusFilter: CardStatus?,
    deckFilter: String?,
    tagFilter: String?,
    onFlagFilterChange: (CardFlagType?) -> Unit,
    onStatusFilterChange: (CardStatus?) -> Unit,
    onDeckFilterChange: (String?) -> Unit,
    onTagFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    val hasFilters = flagFilter != null || statusFilter != null || deckFilter != null || tagFilter != null || query.isNotBlank()
    var showSearchTips by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f).height(44.dp),
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
                statusFilter?.let { s ->
                    AssistChip(onClick = { onStatusFilterChange(null) }, label = { Text("Status: ${s.displayName}", fontSize = 10.sp) },
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
    statusFilter: CardStatus?,
    deckFilter: String?,
    tagFilter: String?,
    onFlagFilterChange: (CardFlagType?) -> Unit,
    onStatusFilterChange: (CardStatus?) -> Unit,
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
                        selected = statusFilter == status,
                        onClick = { onStatusFilterChange(if (statusFilter == status) null else status) },
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
    card: KaiteyoCard,
    visibleColumns: Set<String>,
    columns: List<BrowserColumn>,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelect: (String) -> Unit,
    onClick: () -> Unit,
    onFlagClick: (CardFlagType) -> Unit,
    onStatusClick: (CardStatus) -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) accent.primary.copy(alpha = 0.08f)
        else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(150)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
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
                        color = when (card.status) {
                            CardStatus.New -> Color(0xFF7BC8FF)
                            CardStatus.Learning -> Color(0xFFFEAB57)
                            CardStatus.Young -> Color(0xFFC2FC8B)
                            CardStatus.Mature -> Color(0xFF4CAF50)
                            CardStatus.Relearning -> Color(0xFFFF6B6B)
                            CardStatus.Suspended -> surfaceColors.textMuted
                            CardStatus.Buried -> surfaceColors.textMuted
                            CardStatus.Archived -> surfaceColors.textMuted
                        })
                    "interval" -> Text(formatInterval(card.interval), fontSize = 11.sp, color = surfaceColors.textPrimary)
                    "ease" -> Text(formatFloat(card.ease, 1), fontSize = 11.sp, color = surfaceColors.textPrimary)
                    "reviews" -> Text("${card.reviewCount}", fontSize = 11.sp, color = surfaceColors.textPrimary)
                    "lapses" -> Text("${card.lapses}", fontSize = 11.sp, color = if (card.lapses > 0) Color(0xFFFF6B6B) else surfaceColors.textMuted)
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
}

// ════════════════════════════════════════════
// SELECTION ACTION BAR
// ════════════════════════════════════════════

@Composable
private fun BrowserSelectionBar(
    selectedCount: Int,
    onDeselectAll: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
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
            FilledTonalButton(onClick = onExport, modifier = Modifier.height(32.dp)) {
                Icon(Icons.Default.FileUpload, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Export", fontSize = 12.sp)
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
    AlertDialog(
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

    AlertDialog(
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
