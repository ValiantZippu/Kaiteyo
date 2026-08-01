package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors

// ============================================
// DECK BROWSER ENHANCED
// Folders, Nested decks, Collapse, Expand,
// Favorite, Pinned, Archive, Duplicate detection,
// Merge, Split, Drag & Drop
// ============================================

data class DeckTreeNode(
    val deck: KaiteyoDeck,
    val children: List<DeckTreeNode> = emptyList(),
    val depth: Int = 0,
    val isExpanded: Boolean = true,
    val isDragging: Boolean = false,
    val isDropTarget: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckBrowserEnhancedScreen(
    decks: List<KaiteyoDeck> = emptyList(),
    onDeckClick: (KaiteyoDeck) -> Unit = { },
    onFavorite: (KaiteyoDeck) -> Unit = { },
    onArchive: (KaiteyoDeck) -> Unit = { },
    onMerge: (KaiteyoDeck, KaiteyoDeck) -> Unit = { _, _ -> },
    onMove: (KaiteyoDeck, KaiteyoDeck) -> Unit = { _, _ -> },
    onCreateDeck: (String, String?) -> Unit = { _, _ -> },
    onClose: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var searchQuery by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf("tree") }
    var selectedDeck by remember { mutableStateOf<KaiteyoDeck?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }
    var mergeSource by remember { mutableStateOf<KaiteyoDeck?>(null) }
    var expandedIds by remember { mutableStateOf(setOf<String>()) }

    // Build tree structure
    val rootNodes = remember(decks, expandedIds) {
        buildDeckTree(decks, expandedIds)
    }

    // Filtered view
    val filteredRoots = remember(rootNodes, searchQuery) {
        if (searchQuery.isBlank()) rootNodes
        else filterTree(rootNodes, searchQuery)
    }

    // Stats
    val totalCards = decks.sumOf { it.cardCount }
    val totalDue = decks.sumOf { it.dueCount }
    val totalNew = decks.sumOf { it.newCount }
    val archivedDecks = decks.count { it.isArchived }
    val favoriteDecks = decks.count { it.isFavorite }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deck Browser") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, "New Deck")
                    }
                    IconButton(onClick = { viewMode = if (viewMode == "tree") "list" else "tree" }) {
                        Icon(if (viewMode == "tree") Icons.Default.ViewList else Icons.Default.AccountTree, "Toggle View")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Stats bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("${decks.size} decks", fontSize = 12.sp, color = surfaceColors.textMuted)
                Text("$totalCards cards", fontSize = 12.sp, color = surfaceColors.textMuted)
                Text("$totalDue due", fontSize = 12.sp, color = accent.primary)
                Text("$totalNew new", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                if (archivedDecks > 0) {
                    Text("$archivedDecks archived", fontSize = 12.sp, color = surfaceColors.textMuted)
                }
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search decks...") },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp),
                shape = RoundedCornerShape(10.dp)
            )

            // Deck list
            if (filteredRoots.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filteredRoots, key = { it.deck.id }) { node ->
                        DeckTreeNodeRow(
                            node = node,
                            isSelected = selectedDeck?.id == node.deck.id,
                            searchQuery = searchQuery,
                            onToggleExpand = {
                                expandedIds = if (node.isExpanded) expandedIds - node.deck.id
                                else expandedIds + node.deck.id
                            },
                            onClick = { selectedDeck = node.deck; onDeckClick(node.deck) },
                            onFavorite = { onFavorite(node.deck) },
                            onArchive = { onArchive(node.deck) },
                            onMergeSource = { mergeSource = node.deck; showMergeDialog = true },
                            onMoveTo = { target -> onMove(node.deck, target) },
                            surfaceColors = surfaceColors,
                            accent = accent
                        )
                    }
                }
            }
        }
    }

    // Create deck dialog
    if (showCreateDialog) {
        CreateDeckDialog(
            onConfirm = { name, parentId ->
                onCreateDeck(name, parentId)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
            existingDecks = decks
        )
    }

    // Merge dialog
    if (showMergeDialog && mergeSource != null) {
        MergeDecksDialog(
            sourceDeck = mergeSource!!,
            targetDecks = decks.filter { it.id != mergeSource!!.id },
            onMerge = { target ->
                onMerge(mergeSource!!, target)
                showMergeDialog = false
                mergeSource = null
            },
            onDismiss = { showMergeDialog = false; mergeSource = null }
        )
    }
}

@Composable
private fun DeckTreeNodeRow(
    node: DeckTreeNode,
    isSelected: Boolean,
    searchQuery: String,
    onToggleExpand: () -> Unit,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onArchive: () -> Unit,
    onMergeSource: () -> Unit,
    onMoveTo: (KaiteyoDeck) -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    val deck = node.deck
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) accent.primary.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(200), label = "deckBg"
    )
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(start = (node.depth * 24).dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = surfaceColors.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expand/collapse
                if (node.children.isNotEmpty()) {
                    IconButton(onClick = onToggleExpand, modifier = Modifier.size(24.dp)) {
                        Icon(
                            if (node.isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                            "Toggle", Modifier.size(18.dp), tint = surfaceColors.textMuted
                        )
                    }
                } else {
                    Spacer(Modifier.width(24.dp))
                }

                // Deck icon
                Box(
                    modifier = Modifier.size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                deck.isArchived -> Color.Gray.copy(alpha = 0.2f)
                                deck.isFavorite -> Color(0xFFFFD700).copy(alpha = 0.2f)
                                else -> accent.primary.copy(alpha = 0.1f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when {
                            deck.isArchived -> Icons.Default.Archive
                            deck.isFavorite -> Icons.Default.Star
                            else -> Icons.Default.Folder
                        },
                        null, Modifier.size(20.dp),
                        tint = when {
                            deck.isArchived -> Color.Gray
                            deck.isFavorite -> Color(0xFFFFD700)
                            else -> accent.primary
                        }
                    )
                }
                Spacer(Modifier.width(12.dp))

                // Deck info
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(deck.name, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (deck.isPinned) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.PushPin, null,
                                modifier = Modifier.size(14.dp).padding(bottom = 2.dp),
                                tint = surfaceColors.textMuted)
                        }
                        if (deck.isVirtual) {
                            Spacer(Modifier.width(4.dp))
                            Text("FILTERED", fontSize = 8.sp, fontWeight = FontWeight.Bold,
                                color = accent.primary,
                                modifier = Modifier.clip(RoundedCornerShape(2.dp))
                                    .background(accent.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 3.dp, vertical = 1.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${deck.cardCount} cards", fontSize = 11.sp, color = surfaceColors.textMuted)
                        if (deck.dueCount > 0) {
                            Text("${deck.dueCount} due", fontSize = 11.sp, color = accent.primary)
                        }
                        if (deck.newCount > 0) {
                            Text("${deck.newCount} new", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Stats
                Column(horizontalAlignment = Alignment.End) {
                    Text("${(deck.accuracy * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("${(deck.retention * 100).toInt()}% retention", fontSize = 11.sp, color = surfaceColors.textMuted)
                }

                // Menu
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.MoreVert, "Options", Modifier.size(18.dp), tint = surfaceColors.textMuted)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (deck.isFavorite) "Unfavorite" else "Favorite") },
                            onClick = { onFavorite(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Star, null, Modifier.size(18.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (deck.isArchived) "Unarchive" else "Archive") },
                            onClick = { onArchive(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Archive, null, Modifier.size(18.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Merge...") },
                            onClick = { onMergeSource(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.MergeType, null, Modifier.size(18.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { showMenu = false },
                            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null, Modifier.size(18.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Delete, null, Modifier.size(18.dp)) },
                            colors = MenuDefaults.itemColors(leadingIconColor = MaterialTheme.colorScheme.error)
                        )
                    }
                }
            }
        }

        // Children
        AnimatedVisibility(visible = node.isExpanded && node.children.isNotEmpty()) {
            Column {
                node.children.forEach { child ->
                    DeckTreeNodeRow(
                        node = child,
                        isSelected = isSelected,
                        searchQuery = searchQuery,
                        onToggleExpand = onToggleExpand,
                        onClick = onClick,
                        onFavorite = onFavorite,
                        onArchive = onArchive,
                        onMergeSource = onMergeSource,
                        onMoveTo = onMoveTo,
                        surfaceColors = surfaceColors,
                        accent = accent
                    )
                }
            }
        }
    }
}

// ============================================
// DIALOGS
// ============================================

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CreateDeckDialog(
    onConfirm: (String, String?) -> Unit,
    onDismiss: () -> Unit,
    existingDecks: List<KaiteyoDeck>
) {
    var name by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf<String?>(null) }
    var createSubdeck by remember { mutableStateOf(false) }
    var expandedParent by remember { mutableStateOf(false) }

    AlertDialog(
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = createSubdeck, onCheckedChange = { createSubdeck = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Create as subdeck", fontSize = 13.sp)
                }
                if (createSubdeck) {
                    ExposedDropdownMenuBox(expanded = expandedParent, onExpandedChange = { expandedParent = it }) {
                        OutlinedTextField(
                            value = parentId?.let { id -> existingDecks.firstOrNull { d -> d.id == id }?.name ?: "Select parent" } ?: "Select parent",
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedParent) },
                            singleLine = true
                        )
                        ExposedDropdownMenu(expanded = expandedParent, onDismissRequest = { expandedParent = false }) {
                            existingDecks.forEach { deck ->
                                DropdownMenuItem(
                                    text = { Text(deck.name) },
                                    onClick = { parentId = deck.id; expandedParent = false }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, parentId) }, enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MergeDecksDialog(
    sourceDeck: KaiteyoDeck,
    targetDecks: List<KaiteyoDeck>,
    onMerge: (KaiteyoDeck) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTarget by remember { mutableStateOf<KaiteyoDeck?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge Decks") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Merge cards from \"${sourceDeck.name}\" into:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(targetDecks) { deck ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTarget = deck }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTarget == deck,
                                onClick = { selectedTarget = deck }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(deck.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text("${deck.cardCount} cards", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Text("This will move all cards from ${sourceDeck.name} to the selected deck. ${sourceDeck.name} will be deleted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = { selectedTarget?.let { onMerge(it) } }, enabled = selectedTarget != null) {
                Text("Merge")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ============================================
// TREE UTILITIES
// ============================================

private fun buildDeckTree(decks: List<KaiteyoDeck>, expandedIds: Set<String>): List<DeckTreeNode> {
    val rootDecks = decks.filter { it.parentId == null || it.parentId.isEmpty() }
    return rootDecks.map { buildNode(it, decks, expandedIds, 0) }
}

private fun buildNode(deck: KaiteyoDeck, allDecks: List<KaiteyoDeck>, expandedIds: Set<String>, depth: Int): DeckTreeNode {
    val children = allDecks.filter { it.parentId == deck.id }.map { child ->
        buildNode(child, allDecks, expandedIds, depth + 1)
    }
    return DeckTreeNode(
        deck = deck,
        children = children,
        depth = depth,
        isExpanded = deck.id in expandedIds || expandedIds.isEmpty()
    )
}

private fun filterTree(nodes: List<DeckTreeNode>, query: String): List<DeckTreeNode> {
    val q = query.lowercase()
    return nodes.filter { node ->
        node.deck.name.lowercase().contains(q) ||
        filterTree(node.children, q).isNotEmpty()
    }.map { node ->
        node.copy(children = filterTree(node.children, q))
    }
}

// Uses KaiteyoDeck from DeckManager.kt
