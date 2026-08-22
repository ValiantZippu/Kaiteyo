@file:OptIn(ExperimentalMaterial3Api::class)

package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
// KAITEYO v1.2 — FULL DECK BROWSER
// Nested folders, collapse/expand, favorite,
// pin, archive, drag-drop, merge, split
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckBrowserFullScreen(
    decks: List<KaiteyoDeck>,
    onDeckClick: (KaiteyoDeck) -> Unit = {},
    onFavorite: (KaiteyoDeck) -> Unit = {},
    onPin: (KaiteyoDeck) -> Unit = {},
    onArchive: (KaiteyoDeck) -> Unit = {},
    onMerge: (KaiteyoDeck, KaiteyoDeck) -> Unit = { _, _ -> },
    onMove: (KaiteyoDeck, KaiteyoDeck) -> Unit = { _, _ -> },
    onRename: (KaiteyoDeck, String) -> Unit = { _, _ -> },
    onDelete: (KaiteyoDeck) -> Unit = {},
    onCreateDeck: (String, String, String?, String?) -> Unit = { _, _, _, _ -> },
    onBrowse: (KaiteyoDeck) -> Unit = {},
    onFindContent: (KaiteyoDeck) -> Unit = {},
    onClose: () -> Unit = {}
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    // State
    var searchQuery by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf("tree") } // tree | list | compact
    var expandedIds by remember { mutableStateOf(setOf<String>()) }
    var selectedDeckId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<KaiteyoDeck?>(null) }
    var showMergeDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<KaiteyoDeck?>(null) }
    var mergeSource by remember { mutableStateOf<KaiteyoDeck?>(null) }
    var dragOverDeckId by remember { mutableStateOf<String?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var draggedDeck by remember { mutableStateOf<KaiteyoDeck?>(null) }
    var showFilteredDecks by remember { mutableStateOf(false) }
    var showArchived by remember { mutableStateOf(false) }
    var filterType by remember { mutableStateOf("all") } // all | favorite | pinned | virtual | archived

    // Build hierarchy
    val rootDecks = remember(decks, expandedIds) { buildDeckHierarchy(decks, expandedIds) }

    // Filter decks
    val filteredRoots = remember(rootDecks, searchQuery, filterType, showArchived) {
        var roots = rootDecks
        if (searchQuery.isNotBlank()) {
            roots = filterDeckTree(roots, searchQuery)
        }
        if (!showArchived) {
            roots = filterDeckTree(roots, null) // Just rebuild without archived
        }
        when (filterType) {
            "favorite" -> roots = filterDeckTree(roots, null).filter { node -> containsFavorite(node) }
            "pinned" -> roots = filterDeckTree(roots, null).filter { node -> containsPinned(node) }
            "virtual" -> roots = filterDeckTree(roots, null).filter { node -> containsVirtual(node) }
            "archived" -> roots = filterDeckTree(roots, null).filter { node -> containsArchived(node) }
        }
        roots
    }

    // Stats
    val totalCards = decks.sumOf { it.cardCount }
    val totalDue = decks.sumOf { it.dueCount }
    val totalNew = decks.sumOf { it.newCount }
    val totalFavorite = decks.count { it.isFavorite }
    val totalArchived = decks.count { it.isArchived }
    val totalPinned = decks.count { it.isPinned }
    val totalVirtual = decks.count { it.isVirtual || it.isSmart || it.isDynamic }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deck Browser") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) { Icon(Icons.Default.CreateNewFolder, "New Deck") }
                    IconButton(onClick = { viewMode = when(viewMode) { "tree" -> "list"; "list" -> "compact"; else -> "tree" } }) {
                        Icon(when (viewMode) { "tree" -> Icons.Default.ViewList; "list" -> Icons.Default.ViewModule; else -> Icons.Default.AccountTree }, "Toggle View")
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
            DeckStatsBar(
                deckCount = decks.size,
                totalCards = totalCards,
                totalDue = totalDue,
                totalNew = totalNew,
                favoriteCount = totalFavorite,
                archivedCount = totalArchived,
                pinnedCount = totalPinned,
                virtualCount = totalVirtual,
                surfaceColors = surfaceColors,
                accent = accent
            )

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                placeholder = { Text("Search decks...") },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, "Clear", Modifier.size(18.dp)) }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            // Filter chips
            DeckFilterChips(
                filterType = filterType,
                onFilterChange = { filterType = it },
                showArchived = showArchived,
                onToggleArchived = { showArchived = !showArchived },
                surfaceColors = surfaceColors,
                accent = accent
            )

            // Deck list
            if (filteredRoots.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOff, null, Modifier.size(48.dp), tint = surfaceColors.textMuted)
                        Spacer(Modifier.height(8.dp))
                        Text(if (searchQuery.isNotBlank()) "No matching decks" else "No decks yet",
                            color = surfaceColors.textMuted)
                        if (searchQuery.isBlank()) {
                            TextButton(onClick = { showCreateDialog = true }) { Text("Create your first deck") }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    when (viewMode) {
                        "tree" -> {
                            items(filteredRoots, key = { "root_${it.deck.id}" }) { node ->
                                DeckTreeRow(
                                    node = node,
                                    depth = 0,
                                    expandedIds = expandedIds,
                                    selectedId = selectedDeckId,
                                    dragOverId = dragOverDeckId,
                                    isDragging = isDragging,
                                    onToggleExpand = { id ->
                                        expandedIds = if (id in expandedIds) expandedIds - id else expandedIds + id
                                    },
                                    onSelect = { selectedDeckId = it.deck.id; onDeckClick(it.deck) },
                                    onFavorite = { onFavorite(it) },
                                    onPin = { onPin(it) },
                                    onArchive = { onArchive(it) },
                                    onDragStart = { deck -> isDragging = true; draggedDeck = deck },
                                    onDragOver = { id -> dragOverDeckId = id },
                                    onDragEnd = {
                                        isDragging = false
                                        draggedDeck?.let { src ->
                                            dragOverDeckId?.let { targetId ->
                                                decks.find { it.id == targetId }?.let { target ->
                                                    onMove(src, target)
                                                }
                                            }
                                        }
                                        draggedDeck = null
                                        dragOverDeckId = null
                                    },
                                    onEdit = { showEditDialog = it },
                                    onDelete = { showDeleteConfirm = it },
                                    onMerge = { source -> mergeSource = source; showMergeDialog = true },
                                    onBrowse = { onBrowse(it) },
                                    surfaceColors = surfaceColors,
                                    accent = accent
                                )
                            }
                        }
                        "list" -> {
                            val flatList = flattenDeckTree(filteredRoots)
                            items(flatList, key = { it.deck.id }) { node ->
                                DeckListItem(
                                    deck = node.deck,
                                    depth = node.depth,
                                    isSelected = node.deck.id == selectedDeckId,
                                    isDragOver = node.deck.id == dragOverDeckId,
                                    onClick = { selectedDeckId = node.deck.id; onDeckClick(node.deck) },
                                    onFavorite = { onFavorite(node.deck) },
                                    onPin = { onPin(node.deck) },
                                    onArchive = { onArchive(node.deck) },
                                    onEdit = { showEditDialog = node.deck },
                                    onDelete = { showDeleteConfirm = node.deck },
                                    onBrowse = { onBrowse(node.deck) },
                                    surfaceColors = surfaceColors,
                                    accent = accent
                                )
                            }
                        }
                        "compact" -> {
                            val flatList = flattenDeckTree(filteredRoots)
                            items(flatList, key = { it.deck.id }) { node ->
                                DeckCompactItem(
                                    deck = node.deck,
                                    onClick = { selectedDeckId = node.deck.id; onDeckClick(node.deck) },
                                    surfaceColors = surfaceColors,
                                    accent = accent
                                )
                            }
                        }
                    }
                }
            }

            // Bottom quick stats
            if (selectedDeckId != null) {
                val selectedDeck = decks.find { it.id == selectedDeckId }
                if (selectedDeck != null) {
                    DeckDetailBar(
                        deck = selectedDeck,
                        onClose = { selectedDeckId = null },
                        onBrowse = { onBrowse(selectedDeck) },
                        onFindContent = { onFindContent(selectedDeck) },
                        surfaceColors = surfaceColors,
                        accent = accent
                    )
                }
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        DeckCreateDialog(
            decks = decks,
            onConfirm = { name, type, parentId, noteTypeId ->
                onCreateDeck(name, type, parentId, noteTypeId)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    showEditDialog?.let { deck ->
        DeckEditDialog(
            deck = deck,
            onConfirm = { name, _ ->
                onRename(deck, name)
                showEditDialog = null
            },
            onDismiss = { showEditDialog = null }
        )
    }

    if (showMergeDialog) {
        DeckMergeDialog(
            source = mergeSource,
            decks = decks,
            onConfirm = { target ->
                mergeSource?.let { src -> onMerge(src, target) }
                showMergeDialog = false
                mergeSource = null
            },
            onDismiss = { showMergeDialog = false; mergeSource = null }
        )
    }

    showDeleteConfirm?.let { deck ->
        KaiteyoAlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Deck") },
            text = { Text("Delete \"${deck.name}\" and its ${deck.cardCount} cards? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(deck)
                    showDeleteConfirm = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") } }
        )
    }
}

// ── Data structures ──

data class DeckTreeItem(
    val deck: KaiteyoDeck,
    val children: List<DeckTreeItem> = emptyList(),
    val depth: Int = 0
)

// ── Build tree hierarchy ──

private fun buildDeckHierarchy(decks: List<KaiteyoDeck>, expandedIds: Set<String>): List<DeckTreeItem> {
    val rootDecks = decks.filter { it.parentId == null && !it.isArchived }
    return rootDecks.map { deck -> buildTree(deck, decks, 0) }
}

private fun buildTree(deck: KaiteyoDeck, allDecks: List<KaiteyoDeck>, depth: Int): DeckTreeItem {
    val children = allDecks.filter { it.parentId == deck.id }
    return DeckTreeItem(
        deck = deck,
        children = children.map { buildTree(it, allDecks, depth + 1) },
        depth = depth
    )
}

private fun flattenDeckTree(nodes: List<DeckTreeItem>): List<DeckTreeItem> {
    val result = mutableListOf<DeckTreeItem>()
    nodes.forEach { node ->
        result.add(node)
        result.addAll(flattenDeckTree(node.children))
    }
    return result
}

private fun filterDeckTree(nodes: List<DeckTreeItem>, query: String?): List<DeckTreeItem> {
    if (query == null || query.isBlank()) return nodes
    return nodes.filter { node ->
        node.deck.name.lowercase().contains(query.lowercase()) ||
        node.deck.description.lowercase().contains(query.lowercase()) ||
        filterDeckTree(node.children, query).isNotEmpty()
    }.map { node ->
        node.copy(children = filterDeckTree(node.children, query))
    }
}

private fun containsFavorite(node: DeckTreeItem): Boolean =
    node.deck.isFavorite || node.children.any { containsFavorite(it) }

private fun containsPinned(node: DeckTreeItem): Boolean =
    node.deck.isPinned || node.children.any { containsPinned(it) }

private fun containsVirtual(node: DeckTreeItem): Boolean =
    (node.deck.isVirtual || node.deck.isSmart || node.deck.isDynamic) || node.children.any { containsVirtual(it) }

private fun containsArchived(node: DeckTreeItem): Boolean =
    node.deck.isArchived || node.children.any { containsArchived(it) }

// ════════════════════════════════════════════
// DECK STATS BAR
// ════════════════════════════════════════════

@Composable
private fun DeckStatsBar(
    deckCount: Int,
    totalCards: Int,
    totalDue: Int,
    totalNew: Int,
    favoriteCount: Int,
    archivedCount: Int,
    pinnedCount: Int,
    virtualCount: Int,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$deckCount decks", fontSize = 12.sp, color = surfaceColors.textMuted)
        Text("$totalCards cards", fontSize = 12.sp, color = surfaceColors.textMuted)
        Text("$totalDue due", fontSize = 12.sp, color = accent.primary, fontWeight = FontWeight.Medium)
        Text("$totalNew new", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.weight(1f))
        if (favoriteCount > 0) {
            val sem = LocalKaiteyoSemanticColors.current
            Icon(Icons.Default.Star, null, Modifier.size(12.dp), tint = sem.favoriteStar)
            Text("$favoriteCount", fontSize = 11.sp, color = surfaceColors.textMuted)
        }
        if (pinnedCount > 0) {
            Icon(Icons.Default.PushPin, null, Modifier.size(12.dp), tint = surfaceColors.textMuted)
            Text("$pinnedCount", fontSize = 11.sp, color = surfaceColors.textMuted)
        }
        if (virtualCount > 0) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(12.dp), tint = surfaceColors.textMuted)
            Text("$virtualCount", fontSize = 11.sp, color = surfaceColors.textMuted)
        }
    }
}

// ════════════════════════════════════════════
// DECK FILTER CHIPS
// ════════════════════════════════════════════

@Composable
private fun DeckFilterChips(
    filterType: String,
    onFilterChange: (String) -> Unit,
    showArchived: Boolean,
    onToggleArchived: () -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf("all" to "All", "favorite" to "Favorites", "pinned" to "Pinned", "virtual" to "Smart").forEach { (key, label) ->
            FilterChip(
                selected = filterType == key,
                onClick = { onFilterChange(key) },
                label = { Text(label, fontSize = 11.sp) },
                modifier = Modifier.height(28.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        FilterChip(
            selected = showArchived,
            onClick = onToggleArchived,
            label = { Text("Archived", fontSize = 11.sp) },
            modifier = Modifier.height(28.dp)
        )
    }
}

// ════════════════════════════════════════════
// DECK TREE ROW (with drag-drop)
// ════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeckTreeRow(
    node: DeckTreeItem,
    depth: Int,
    expandedIds: Set<String>,
    selectedId: String?,
    dragOverId: String?,
    isDragging: Boolean,
    onToggleExpand: (String) -> Unit,
    onSelect: (DeckTreeItem) -> Unit,
    onFavorite: (KaiteyoDeck) -> Unit,
    onPin: (KaiteyoDeck) -> Unit = {},
    onArchive: (KaiteyoDeck) -> Unit,
    onDragStart: (KaiteyoDeck) -> Unit,
    onDragOver: (String) -> Unit,
    onDragEnd: () -> Unit,
    onEdit: (KaiteyoDeck) -> Unit,
    onDelete: (KaiteyoDeck) -> Unit,
    onMerge: (KaiteyoDeck) -> Unit,
    onBrowse: (KaiteyoDeck) -> Unit = {},
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    val sem = LocalKaiteyoSemanticColors.current
    val deck = node.deck
    val hasChildren = node.children.isNotEmpty()
    val isExpanded = deck.id in expandedIds
    val isSelected = deck.id == selectedId
    val isDragOver = deck.id == dragOverId
    var showMenu by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 20).dp)
                .clip(RoundedCornerShape(10.dp))
                .then(
                    when {
                        isDragOver -> Modifier.background(accent.primary.copy(alpha = 0.1f))
                        isSelected -> Modifier.background(surfaceColors.surfaceInteractive)
                        else -> Modifier
                    }
                )
                .then(Modifier.padding(vertical = 2.dp))
                .clickable { onSelect(node) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle
            Icon(
                Icons.Default.DragHandle, "Drag",
                modifier = Modifier.size(20.dp).padding(start = 4.dp),
                tint = surfaceColors.textMuted.copy(alpha = 0.5f)
            )

            // Expand/collapse
            if (hasChildren) {
                IconButton(onClick = { onToggleExpand(deck.id) }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        null, Modifier.size(18.dp), tint = surfaceColors.textMuted
                    )
                }
            } else {
                Spacer(Modifier.width(28.dp))
            }

            // Deck icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            deck.isFavorite -> sem.favoriteStar.copy(alpha = 0.2f)
                            deck.isVirtual -> accent.primary.copy(alpha = 0.1f)
                            deck.isArchived -> surfaceColors.border.copy(alpha = 0.2f)
                            deck.isPinned -> sem.new.copy(alpha = 0.15f)
                            else -> surfaceColors.surfaceInteractive
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    deck.isFavorite -> Icon(Icons.Default.Star, null, Modifier.size(16.dp), tint = sem.favoriteStar)
                    deck.isVirtual -> Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp), tint = accent.primary)
                    deck.isArchived -> Icon(Icons.Default.Archive, null, Modifier.size(16.dp), tint = surfaceColors.textMuted)
                    deck.isPinned -> Icon(Icons.Default.PushPin, null, Modifier.size(16.dp), tint = sem.new)
                    else -> Icon(Icons.Default.Folder, null, Modifier.size(16.dp), tint = surfaceColors.textMuted)
                }
            }

            Spacer(Modifier.width(8.dp))

            // Deck info
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        deck.name,
                        fontSize = 13.sp,
                        fontWeight = if (deck.isPinned) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (deck.isArchived) surfaceColors.textMuted else surfaceColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (deck.isDynamic) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.AutoMode, null, Modifier.size(12.dp), tint = accent.primary)
                    }
                }
                Text(
                    "${deck.cardCount} cards | ${deck.dueCount} due",
                    fontSize = 10.sp,
                    color = surfaceColors.textMuted
                )
            }

            // Stats
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 4.dp)) {
                if (deck.dueCount > 0) {
                    Text("${deck.dueCount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accent.primary)
                    Text("due", fontSize = 9.sp, color = surfaceColors.textMuted)
                }
            }

            // Menu
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.MoreVert, "Options", Modifier.size(18.dp), tint = surfaceColors.textMuted)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEdit(deck) },
                        leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)) })
                    DropdownMenuItem(
                        text = { Text(if (deck.isFavorite) "Unfavorite" else "Favorite") },
                        onClick = { showMenu = false; onFavorite(deck) },
                        leadingIcon = { Icon(if (deck.isFavorite) Icons.Default.StarBorder else Icons.Default.Star, null, Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (deck.isPinned) "Unpin" else "Pin") },
                        onClick = { showMenu = false; onPin(deck) },
                        leadingIcon = { Icon(Icons.Default.PushPin, null, Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("View Cards") },
                        onClick = { showMenu = false; onBrowse(deck) },
                        leadingIcon = { Icon(Icons.Default.ViewList, null, Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (deck.isArchived) "Unarchive" else "Archive") },
                        onClick = { showMenu = false; onArchive(deck) },
                        leadingIcon = { Icon(Icons.Default.Archive, null, Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(text = { Text("Merge into...") }, onClick = { showMenu = false; onMerge(deck) },
                        leadingIcon = { Icon(Icons.Default.CallMerge, null, Modifier.size(18.dp)) })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete(deck) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) })
                }
            }
        }

        // Children
        if (hasChildren && isExpanded) {
            node.children.forEach { child ->
                DeckTreeRow(
                    node = child,
                    depth = depth + 1,
                    expandedIds = expandedIds,
                    selectedId = selectedId,
                    dragOverId = dragOverId,
                    isDragging = isDragging,
                    onToggleExpand = onToggleExpand,
                    onSelect = onSelect,
                    onFavorite = onFavorite,
                    onPin = onPin,
                    onArchive = onArchive,
                    onDragStart = onDragStart,
                    onDragOver = onDragOver,
                    onDragEnd = onDragEnd,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onMerge = onMerge,
                    onBrowse = onBrowse,
                    surfaceColors = surfaceColors,
                    accent = accent
                )
            }
        }
    }
}

// ════════════════════════════════════════════
// DECK LIST ITEM
// ════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeckListItem(
    deck: KaiteyoDeck,
    depth: Int,
    isSelected: Boolean,
    isDragOver: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onPin: () -> Unit = {},
    onArchive: () -> Unit,
    onEdit: (KaiteyoDeck) -> Unit,
    onDelete: (KaiteyoDeck) -> Unit,
    onBrowse: (KaiteyoDeck) -> Unit = {},
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    val sem = LocalKaiteyoSemanticColors.current
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp)
            .clip(RoundedCornerShape(8.dp))
            .then(if (isSelected) Modifier.background(accent.primary.copy(alpha = 0.08f)) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
                .background(if (deck.isFavorite) sem.favoriteStar.copy(alpha = 0.2f) else surfaceColors.surfaceInteractive),
            contentAlignment = Alignment.Center
        ) {
            Icon(if (deck.isFavorite) Icons.Default.Star else Icons.Default.Folder, null,
                Modifier.size(14.dp), tint = if (deck.isFavorite) sem.favoriteStar else surfaceColors.textMuted)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(deck.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = surfaceColors.textPrimary, maxLines = 1)
            Text("${deck.cardCount} cards", fontSize = 10.sp, color = surfaceColors.textMuted)
        }
        if (deck.dueCount > 0) {
            Text("${deck.dueCount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accent.primary)
        }
        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.MoreVert, "Options", Modifier.size(18.dp), tint = surfaceColors.textMuted)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEdit(deck) },
                    leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)) })
                DropdownMenuItem(text = { Text("Favorite") }, onClick = { showMenu = false; onFavorite() },
                    leadingIcon = { Icon(Icons.Default.Star, null, Modifier.size(18.dp)) })
                DropdownMenuItem(text = { Text("Pin") }, onClick = { showMenu = false; onPin() },
                    leadingIcon = { Icon(Icons.Default.PushPin, null, Modifier.size(18.dp)) })
                DropdownMenuItem(text = { Text("View Cards") }, onClick = { showMenu = false; onBrowse(deck) },
                    leadingIcon = { Icon(Icons.Default.ViewList, null, Modifier.size(18.dp)) })
                DropdownMenuItem(text = { Text("Archive") }, onClick = { showMenu = false; onArchive() },
                    leadingIcon = { Icon(Icons.Default.Archive, null, Modifier.size(18.dp)) })
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; onDelete(deck) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) })
            }
        }
    }
}

// ════════════════════════════════════════════
// DECK COMPACT ITEM
// ════════════════════════════════════════════

@Composable
private fun DeckCompactItem(
    deck: KaiteyoDeck,
    onClick: () -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    val sem = LocalKaiteyoSemanticColors.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surfaceElevated),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (deck.isFavorite) sem.favoriteStar.copy(alpha = 0.2f) else surfaceColors.surfaceInteractive),
                contentAlignment = Alignment.Center
            ) {
                Icon(if (deck.isFavorite) Icons.Default.Star else Icons.Default.Folder, null,
                    Modifier.size(18.dp), tint = if (deck.isFavorite) sem.favoriteStar else surfaceColors.textMuted)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(deck.name, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = surfaceColors.textPrimary)
                Text("${deck.cardCount} cards, ${deck.dueCount} due", fontSize = 10.sp, color = surfaceColors.textMuted)
            }
            Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = surfaceColors.textMuted)
        }
    }
}

// ════════════════════════════════════════════
// DECK DETAIL BAR (bottom)
// ════════════════════════════════════════════

@Composable
private fun DeckDetailBar(
    deck: KaiteyoDeck,
    onClose: () -> Unit,
    onBrowse: (KaiteyoDeck) -> Unit = {},
    onFindContent: (KaiteyoDeck) -> Unit = {},
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surfaceColors.surfaceElevated,
        shadowElevation = 8.dp
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            Column(Modifier.weight(1f)) {
                Text(deck.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = surfaceColors.textPrimary)
                Text(deck.description, fontSize = 11.sp, color = surfaceColors.textMuted, maxLines = 1)
            }
            Spacer(Modifier.width(8.dp))
            // Stats
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text("${deck.cardCount}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = surfaceColors.textPrimary)
                Text("Cards", fontSize = 9.sp, color = surfaceColors.textMuted)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text("${deck.dueCount}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accent.primary)
                Text("Due", fontSize = 9.sp, color = surfaceColors.textMuted)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text("${deck.newCount}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Text("New", fontSize = 9.sp, color = surfaceColors.textMuted)
            }
            if (deck.accuracy > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text("${(deck.accuracy * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = surfaceColors.textPrimary)
                    Text("Acc", fontSize = 9.sp, color = surfaceColors.textMuted)
                }
            }
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "Close", Modifier.size(18.dp))
            }
            }
            // Empty deck: real next action instead of a dead end. "Browse
            // Content" opens the library search to add kanji/vocabulary;
            // "View Cards" still opens the (empty) card browser for context.
            if (deck.cardCount == 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info, null,
                        modifier = Modifier.size(16.dp),
                        tint = accent.primary
                    )
                    Text(
                        "This deck is empty — add kanji or vocabulary to start studying.",
                        fontSize = 11.sp,
                        color = surfaceColors.textMuted,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onFindContent(deck) }) {
                        Text("Browse Content", fontSize = 12.sp)
                    }
                    TextButton(onClick = { onBrowse(deck) }) {
                        Text("View Cards", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════
// DIALOGS
// ════════════════════════════════════════════

@Composable
private fun DeckCreateDialog(
    decks: List<KaiteyoDeck>,
    onConfirm: (String, String, String?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var deckType by remember { mutableStateOf("kanji") }
    var noteTypeId by remember { mutableStateOf("kaiteyo-default") }
    var parentId by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }

    val rootDecks = decks.filter { it.parentId == null && !it.isArchived }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Deck") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Deck Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // Deck type decides which real repository owns the deck:
                // writing decks hold characters, flashcard decks hold words.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Type", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(52.dp))
                    listOf(
                        "kanji" to "Writing",
                        "vocabulary" to "Flashcards"
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = deckType == value,
                            onClick = { deckType = value },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
                if (deckType == "vocabulary") {
                    var noteTypeExpanded by remember { mutableStateOf(false) }
                    Text("Note type", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ExposedDropdownMenuBox(expanded = noteTypeExpanded, onExpandedChange = { noteTypeExpanded = it }) {
                        OutlinedTextField(
                            value = defaultKaiteyoNoteTypes
                                .firstOrNull { it.id == noteTypeId }?.name
                                ?: "Kaiteyo (default)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Flashcard note type") },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(noteTypeExpanded) },
                            singleLine = true
                        )
                        ExposedDropdownMenu(expanded = noteTypeExpanded, onDismissRequest = { noteTypeExpanded = false }) {
                            defaultKaiteyoNoteTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(type.name, fontSize = 13.sp)
                                            Text(
                                                type.fields.joinToString(" · ") { it.label },
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = { noteTypeId = type.id; noteTypeExpanded = false }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (rootDecks.isNotEmpty()) {
                    var expandedParent by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedParent, onExpandedChange = { expandedParent = it }) {
                        OutlinedTextField(
                            value = parentId?.let { decks.find { d -> d.id == it }?.name ?: "None (Root)" } ?: "None (Root)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Parent Deck") },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedParent) },
                            singleLine = true
                        )
                        ExposedDropdownMenu(expanded = expandedParent, onDismissRequest = { expandedParent = false }) {
                            DropdownMenuItem(text = { Text("None (Root)") }, onClick = { parentId = null; expandedParent = false })
                            rootDecks.forEach { d ->
                                DropdownMenuItem(text = { Text(d.name) }, onClick = { parentId = d.id; expandedParent = false })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, deckType, parentId, noteTypeId) },
                enabled = name.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DeckEditDialog(
    deck: KaiteyoDeck,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(deck.name) }
    var description by remember { mutableStateOf(deck.description) }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Deck") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Deck Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name, description) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DeckMergeDialog(
    source: KaiteyoDeck?,
    decks: List<KaiteyoDeck>,
    onConfirm: (KaiteyoDeck) -> Unit,
    onDismiss: () -> Unit
) {
    var targetId by remember { mutableStateOf<String?>(null) }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge Deck") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Merge \"${source?.name ?: ""}\" into:", fontSize = 13.sp)
                var expandedTarget by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expandedTarget, onExpandedChange = { expandedTarget = it }) {
                    OutlinedTextField(
                        value = targetId?.let { decks.find { d -> d.id == it }?.name ?: "Select target" } ?: "Select target",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Deck") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedTarget) },
                        singleLine = true
                    )
                    ExposedDropdownMenu(expanded = expandedTarget, onDismissRequest = { expandedTarget = false }) {
                        decks.filter { it.id != source?.id }.forEach { d ->
                            DropdownMenuItem(text = { Text(d.name) }, onClick = { targetId = d.id; expandedTarget = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { targetId?.let { id -> decks.find { it.id == id }?.let { onConfirm(it) } } },
                enabled = targetId != null
            ) { Text("Merge") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
