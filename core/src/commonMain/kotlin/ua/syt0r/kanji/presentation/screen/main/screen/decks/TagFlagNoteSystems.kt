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

// ============================================
// KAITEYO v1.2 — TAGS, FLAGS & NOTES SYSTEMS
// Complete management UIs for all three features
// ============================================

// ════════════════════════════════════════════
// TAGS SYSTEM — Full nested tag manager
// ════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TagManagerScreenFull(
    tags: List<CardTag>,
    cards: List<KaiteyoCard> = emptyList(),
    onAddTag: (String, String, Long?) -> Unit = { _, _, _ -> },
    onUpdateTag: (Long, String, String, Long?) -> Unit = { _, _, _, _ -> },
    onDeleteTag: (Long) -> Unit = {},
    onMergeTags: (Long, Long) -> Unit = { _, _ -> },
    onApplyTagToCards: (Long, List<String>) -> Unit = { _, _ -> },
    onClose: () -> Unit = {}
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<CardTag?>(null) }
    var showMergeDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<CardTag?>(null) }
    var showApplyDialog by remember { mutableStateOf<CardTag?>(null) }
    var expandedTagIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedTagIds by remember { mutableStateOf(setOf<Long>()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf("tree") } // tree | flat | cards
    var sortOrder by remember { mutableStateOf("name") } // name | count | color

    // Build tree hierarchy
    val rootTags = remember(tags) { tags.filter { it.parentId == null } }
    val childrenOf = remember(tags) { tags.groupBy { it.parentId } }

    // Filter tags
    val filteredTags = remember(tags, searchQuery) {
        if (searchQuery.isBlank()) tags
        else tags.filter { it.name.lowercase().contains(searchQuery.lowercase()) }
    }

    val filteredRootTags = remember(filteredTags) {
        filteredTags.filter { it.parentId == null }
    }

    // Compute tag usage counts
    val tagUsageCounts = remember(cards, tags) {
        val counts = mutableMapOf<Long, Int>()
        tags.forEach { tag ->
            counts[tag.id] = cards.count { card -> tag.name in card.tagNames }
        }
        counts
    }

    val totalTaggedCards = cards.count { it.tagNames.isNotEmpty() }
    val totalTags = tags.size
    val unusedTags = tags.filter { (tagUsageCounts[it.id] ?: 0) == 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tag Manager") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            selectedTagIds = emptySet()
                            isSelectionMode = false
                        }) { Icon(Icons.Default.Close, "Cancel Selection") }
                    } else {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, "Add Tag")
                        }
                        IconButton(onClick = { viewMode = when(viewMode) { "tree" -> "flat"; "flat" -> "cards"; else -> "tree" } }) {
                            Icon(
                                when (viewMode) {
                                    "tree" -> Icons.Default.AccountTree
                                    "flat" -> Icons.Default.ViewList
                                    else -> Icons.Default.GridView
                                }, "Toggle View"
                            )
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
            // Stats bar
            TagStatsBar(
                totalTags = totalTags,
                totalTaggedCards = totalTaggedCards,
                unusedCount = unusedTags.size,
                surfaceColors = surfaceColors,
                accent = accent
            )

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                placeholder = { Text("Search tags...") },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, "Clear", Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp),
                shape = RoundedCornerShape(10.dp)
            )

            // Quick filters
            TagQuickFilters(
                sortOrder = sortOrder,
                onSortOrderChange = { sortOrder = it },
                showUnusedOnly = false,
                onToggleUnused = { },
                isSelectionMode = isSelectionMode,
                onToggleSelectionMode = {
                    isSelectionMode = !isSelectionMode
                    if (!isSelectionMode) selectedTagIds = emptySet()
                },
                selectedCount = selectedTagIds.size,
                surfaceColors = surfaceColors,
                accent = accent
            )

            // Tag content
            if (filteredRootTags.isEmpty() && filteredTags.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Label, null, Modifier.size(48.dp), tint = surfaceColors.textMuted)
                        Spacer(Modifier.height(8.dp))
                        Text(if (searchQuery.isNotBlank()) "No matching tags" else "No tags yet",
                            color = surfaceColors.textMuted)
                        if (searchQuery.isBlank()) {
                            TextButton(onClick = { showCreateDialog = true }) { Text("Create your first tag") }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    when (viewMode) {
                        "tree" -> {
                            items(filteredRootTags, key = { it.id }) { tag ->
                                TagTreeItem(
                                    tag = tag,
                                    depth = 0,
                                    allTags = tags,
                                    childrenOf = childrenOf,
                                    expandedIds = expandedTagIds,
                                    onToggleExpand = { id ->
                                        expandedTagIds = if (id in expandedTagIds) expandedTagIds - id
                                        else expandedTagIds + id
                                    },
                                    usageCount = tagUsageCounts[tag.id] ?: 0,
                                    isSelected = tag.id in selectedTagIds,
                                    isSelectionMode = isSelectionMode,
                                    onToggleSelect = { id ->
                                        selectedTagIds = if (id in selectedTagIds) selectedTagIds - id
                                        else selectedTagIds + id
                                    },
                                    onEdit = { showEditDialog = it },
                                    onDelete = { showDeleteConfirm = it },
                                    onMerge = { showMergeDialog = true },
                                    onApply = { showApplyDialog = it },
                                    surfaceColors = surfaceColors,
                                    accent = accent
                                )
                            }
                        }
                        "flat" -> {
                            items(filteredTags.sortedBy { it.name }, key = { it.id }) { tag ->
                                TagFlatItem(
                                    tag = tag,
                                    usageCount = tagUsageCounts[tag.id] ?: 0,
                                    isSelected = tag.id in selectedTagIds,
                                    isSelectionMode = isSelectionMode,
                                    onToggleSelect = { id ->
                                        selectedTagIds = if (id in selectedTagIds) selectedTagIds - id
                                        else selectedTagIds + id
                                    },
                                    onEdit = { showEditDialog = it },
                                    onDelete = { showDeleteConfirm = it },
                                    onMerge = { showMergeDialog = true },
                                    onApply = { showApplyDialog = it },
                                    surfaceColors = surfaceColors,
                                    accent = accent
                                )
                            }
                        }
                        else -> {
                            // Card-based view
                            items(filteredTags.sortedBy { it.name }, key = { it.id }) { tag ->
                                TagCardItem(
                                    tag = tag,
                                    usageCount = tagUsageCounts[tag.id] ?: 0,
                                    isSelected = tag.id in selectedTagIds,
                                    isSelectionMode = isSelectionMode,
                                    onToggleSelect = { id ->
                                        selectedTagIds = if (id in selectedTagIds) selectedTagIds - id
                                        else selectedTagIds + id
                                    },
                                    onEdit = { showEditDialog = it },
                                    onDelete = { showDeleteConfirm = it },
                                    surfaceColors = surfaceColors,
                                    accent = accent
                                )
                            }
                        }
                    }

                    // Unused tags section
                    if (unusedTags.isNotEmpty() && viewMode != "cards") {
                        item {
                            Spacer(Modifier.height(12.dp))
                            Text("Unused Tags (${unusedTags.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = surfaceColors.textMuted,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
                        }
                        items(unusedTags, key = { "unused_${it.id}" }) { tag ->
                            TagFlatItem(
                                tag = tag,
                                usageCount = 0,
                                isSelected = tag.id in selectedTagIds,
                                isSelectionMode = isSelectionMode,
                                onToggleSelect = { id ->
                                    selectedTagIds = if (id in selectedTagIds) selectedTagIds - id
                                    else selectedTagIds + id
                                },
                                onEdit = { showEditDialog = it },
                                onDelete = { showDeleteConfirm = it },
                                onMerge = { showMergeDialog = true },
                                onApply = { showApplyDialog = it },
                                surfaceColors = surfaceColors,
                                accent = accent,
                                isUnused = true
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        TagCreateDialog(
            tags = tags,
            onConfirm = { name, color, parentId ->
                onAddTag(name, color, parentId)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    showEditDialog?.let { tag ->
        TagEditDialog(
            tag = tag,
            tags = tags,
            onConfirm = { name, color, parentId ->
                onUpdateTag(tag.id, name, color, parentId)
                showEditDialog = null
            },
            onDismiss = { showEditDialog = null }
        )
    }

    if (showMergeDialog) {
        TagMergeDialog(
            tags = tags,
            onConfirm = { sourceId, targetId ->
                onMergeTags(sourceId, targetId)
                showMergeDialog = false
            },
            onDismiss = { showMergeDialog = false }
        )
    }

    showDeleteConfirm?.let { tag ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Tag") },
            text = { Text("Delete \"${tag.name}\"? This will remove it from all cards.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteTag(tag.id)
                    showDeleteConfirm = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
            }
        )
    }

    showApplyDialog?.let { tag ->
        TagApplyDialog(
            tag = tag,
            cards = cards,
            onConfirm = { cardIds ->
                onApplyTagToCards(tag.id, cardIds)
                showApplyDialog = null
            },
            onDismiss = { showApplyDialog = null }
        )
    }
}

// ── Tag Stats Bar ──

@Composable
private fun TagStatsBar(
    totalTags: Int,
    totalTaggedCards: Int,
    unusedCount: Int,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("$totalTags tags", fontSize = 12.sp, color = surfaceColors.textMuted)
        Text("$totalTaggedCards cards", fontSize = 12.sp, color = surfaceColors.textMuted)
        if (unusedCount > 0) {
            Text("$unusedCount unused", fontSize = 12.sp, color = surfaceColors.textMuted)
        }
    }
}

// ── Tag Quick Filters ──

@Composable
private fun TagQuickFilters(
    sortOrder: String,
    onSortOrderChange: (String) -> Unit,
    showUnusedOnly: Boolean,
    onToggleUnused: () -> Unit,
    isSelectionMode: Boolean,
    onToggleSelectionMode: () -> Unit,
    selectedCount: Int,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sort
        var expandedSort by remember { mutableStateOf(false) }
        Box {
            FilterChip(
                selected = false,
                onClick = { expandedSort = true },
                label = { Text("Sort: ${when(sortOrder) { "name" -> "Name"; "count" -> "Count"; else -> "Color" }}", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Sort, null, Modifier.size(14.dp)) },
                modifier = Modifier.height(28.dp)
            )
            DropdownMenu(expanded = expandedSort, onDismissRequest = { expandedSort = false }) {
                DropdownMenuItem(text = { Text("Name") }, onClick = { onSortOrderChange("name"); expandedSort = false })
                DropdownMenuItem(text = { Text("Count") }, onClick = { onSortOrderChange("count"); expandedSort = false })
                DropdownMenuItem(text = { Text("Color") }, onClick = { onSortOrderChange("color"); expandedSort = false })
            }
        }

        // Selection mode toggle
        FilterChip(
            selected = isSelectionMode,
            onClick = onToggleSelectionMode,
            label = { Text(if (isSelectionMode) "$selectedCount selected" else "Select", fontSize = 11.sp) },
            leadingIcon = { Icon(if (isSelectionMode) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank, null, Modifier.size(14.dp)) },
            modifier = Modifier.height(28.dp)
        )

        Spacer(Modifier.weight(1f))

        if (isSelectionMode) {
            TextButton(onClick = { }, modifier = Modifier.height(28.dp)) {
                Text("Merge", fontSize = 11.sp)
            }
            TextButton(onClick = { }, modifier = Modifier.height(28.dp)) {
                Text("Delete", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ── Tag Tree Item ──

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagTreeItem(
    tag: CardTag,
    depth: Int,
    allTags: List<CardTag>,
    childrenOf: Map<Long?, List<CardTag>>,
    expandedIds: Set<Long>,
    onToggleExpand: (Long) -> Unit,
    usageCount: Int,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelect: (Long) -> Unit,
    onEdit: (CardTag) -> Unit,
    onDelete: (CardTag) -> Unit,
    onMerge: () -> Unit,
    onApply: (CardTag) -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme,
    isUnused: Boolean = false
) {
    val children = childrenOf[tag.id] ?: emptyList()
    val hasChildren = children.isNotEmpty()
    val isExpanded = tag.id in expandedIds
    val tagColor = tag.getDisplayColor()
    var showMenu by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode) onToggleSelect(tag.id)
                        else if (hasChildren) onToggleExpand(tag.id)
                    },
                    onLongClick = { showMenu = true }
                )
                .padding(start = (16 + depth * 20).dp, end = 8.dp)
                .then(
                    if (isSelected) Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.primary.copy(alpha = 0.08f))
                    else Modifier
                )
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection checkbox
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect(tag.id) },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(4.dp))
            }

            // Expand/collapse
            if (hasChildren) {
                IconButton(onClick = { onToggleExpand(tag.id) }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        null, Modifier.size(18.dp), tint = surfaceColors.textMuted
                    )
                }
            } else {
                Spacer(Modifier.width(24.dp))
            }

            // Color dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(tagColor)
            )

            Spacer(Modifier.width(8.dp))

            // Tag name with hierarchy
            Column(Modifier.weight(1f)) {
                Text(
                    tag.name,
                    fontSize = if (depth == 0) 14.sp else 13.sp,
                    fontWeight = if (depth == 0) FontWeight.Medium else FontWeight.Normal,
                    color = surfaceColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isUnused) {
                    Text("Unused", fontSize = 10.sp, color = surfaceColors.textMuted)
                }
            }

            // Usage count
            Text("$usageCount", fontSize = 12.sp, color = surfaceColors.textMuted, modifier = Modifier.padding(horizontal = 4.dp))

            // Context menu
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.MoreVert, "Options", Modifier.size(18.dp), tint = surfaceColors.textMuted)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEdit(tag) },
                        leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)) })
                    DropdownMenuItem(text = { Text("Apply to Cards") }, onClick = { showMenu = false; onApply(tag) },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, null, Modifier.size(18.dp)) })
                    DropdownMenuItem(text = { Text("Merge...") }, onClick = { showMenu = false; onMerge() },
                        leadingIcon = { Icon(Icons.Default.CallMerge, null, Modifier.size(18.dp)) })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete(tag) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) })
                }
            }
        }

        // Children
        if (hasChildren && isExpanded) {
            children.forEach { child ->
                TagTreeItem(
                    tag = child,
                    depth = depth + 1,
                    allTags = allTags,
                    childrenOf = childrenOf,
                    expandedIds = expandedIds,
                    onToggleExpand = onToggleExpand,
                    usageCount = usageCount,
                    isSelected = child.id in (setOf<Long>()),
                    isSelectionMode = isSelectionMode,
                    onToggleSelect = onToggleSelect,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onMerge = onMerge,
                    onApply = onApply,
                    surfaceColors = surfaceColors,
                    accent = accent
                )
            }
        }
    }
}

// ── Tag Flat Item ──

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagFlatItem(
    tag: CardTag,
    usageCount: Int,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelect: (Long) -> Unit,
    onEdit: (CardTag) -> Unit,
    onDelete: (CardTag) -> Unit,
    onMerge: () -> Unit,
    onApply: (CardTag) -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme,
    isUnused: Boolean = false
) {
    val tagColor = tag.getDisplayColor()
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelect(tag.id) },
                onLongClick = { showMenu = true }
            )
            .then(
                if (isSelected) Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.primary.copy(alpha = 0.08f))
                else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect(tag.id) }, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier.size(12.dp).clip(CircleShape).background(tagColor)
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(tag.name, fontSize = 14.sp, color = surfaceColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (isUnused) Text("Unused", fontSize = 10.sp, color = surfaceColors.textMuted)
        }
        Text("$usageCount", fontSize = 12.sp, color = surfaceColors.textMuted)
        if (tag.parentId != null) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.SubdirectoryArrowRight, null, Modifier.size(14.dp), tint = surfaceColors.textMuted)
        }
        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.MoreVert, "Options", Modifier.size(18.dp), tint = surfaceColors.textMuted)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEdit(tag) },
                    leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)) })
                DropdownMenuItem(text = { Text("Apply to Cards") }, onClick = { showMenu = false; onApply(tag) },
                    leadingIcon = { Icon(Icons.Default.PlaylistAdd, null, Modifier.size(18.dp)) })
                DropdownMenuItem(text = { Text("Merge...") }, onClick = { showMenu = false; onMerge() },
                    leadingIcon = { Icon(Icons.Default.CallMerge, null, Modifier.size(18.dp)) })
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; onDelete(tag) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) })
            }
        }
    }
}

// ── Tag Card Item ──

@Composable
private fun TagCardItem(
    tag: CardTag,
    usageCount: Int,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelect: (Long) -> Unit,
    onEdit: (CardTag) -> Unit,
    onDelete: (CardTag) -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    val tagColor = tag.getDisplayColor()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surfaceElevated),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(tagColor),
                contentAlignment = Alignment.Center
            ) {
                Text(tag.name.take(1), fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = Color.White)
            }
            Spacer(Modifier.height(6.dp))
            Text(tag.name, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                color = surfaceColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$usageCount cards", fontSize = 10.sp, color = surfaceColors.textMuted)
        }
    }
}

// ── Tag Create Dialog ──

@Composable
private fun TagCreateDialog(
    tags: List<CardTag>,
    onConfirm: (String, String, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#FFA78BFA") }
    var parentId by remember { mutableStateOf<Long?>(null) }
    var showColorPicker by remember { mutableStateOf(false) }

    val presetColors = listOf(
        "#FFFF6B6B", "#FFFEAB57", "#FFFFD93D", "#FFC2FC8B",
        "#FF7BC8FF", "#FFA78BFA", "#FFB0B0B0", "#FF000000",
        "#FFFFFFFF", "#FF4CAF50", "#FFFF9800", "#FF2196F3"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tag Name") },
                    placeholder = { Text("e.g., jlpt-n5, common, animal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Color picker
                Text("Color", style = MaterialTheme.typography.labelMedium)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(120.dp)
                ) {
                    items(presetColors) { hex ->
                        val c = try {
                            val h = hex.removePrefix("#")
                            Color(h.substring(2..3).toInt(16), h.substring(4..5).toInt(16),
                                h.substring(6..7).toInt(16), h.substring(0..1).toInt(16))
                        } catch (_: Exception) { Color.Gray }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (color == hex) Modifier.border(2.dp, Color.White, CircleShape)
                                    else Modifier
                                )
                                .clickable { color = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (color == hex) {
                                Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = Color.White)
                            }
                        }
                    }
                }

                // Parent selection
                val rootTags = tags.filter { it.parentId == null }
                if (rootTags.isNotEmpty()) {
                    var expandedParent by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedParent, onExpandedChange = { expandedParent = it }) {
                        OutlinedTextField(
                            value = parentId?.let { pid -> tags.find { it.id == pid }?.name ?: "None (Root)" } ?: "None (Root)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Parent Tag") },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedParent) },
                            singleLine = true
                        )
                        ExposedDropdownMenu(expanded = expandedParent, onDismissRequest = { expandedParent = false }) {
                            DropdownMenuItem(
                                text = { Text("None (Root)") },
                                onClick = { parentId = null; expandedParent = false }
                            )
                            rootTags.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.name) },
                                    onClick = { parentId = t.id; expandedParent = false }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, color, parentId) },
                enabled = name.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Tag Edit Dialog ──

@Composable
private fun TagEditDialog(
    tag: CardTag,
    tags: List<CardTag>,
    onConfirm: (String, String, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(tag.name) }
    var color by remember { mutableStateOf(tag.color) }
    var parentId by remember { mutableStateOf(tag.parentId) }

    val presetColors = listOf(
        "#FFFF6B6B", "#FFFEAB57", "#FFFFD93D", "#FFC2FC8B",
        "#FF7BC8FF", "#FFA78BFA", "#FFB0B0B0", "#FF000000",
        "#FFFFFFFF", "#FF4CAF50", "#FFFF9800", "#FF2196F3"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tag Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Color", style = MaterialTheme.typography.labelMedium)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(120.dp)
                ) {
                    items(presetColors) { hex ->
                        val c = try {
                            val h = hex.removePrefix("#")
                            Color(h.substring(2..3).toInt(16), h.substring(4..5).toInt(16),
                                h.substring(6..7).toInt(16), h.substring(0..1).toInt(16))
                        } catch (_: Exception) { Color.Gray }
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(c)
                                .then(if (color == hex) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                                .clickable { color = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (color == hex) Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = Color.White)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, color, parentId) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Tag Merge Dialog ──

@Composable
private fun TagMergeDialog(
    tags: List<CardTag>,
    onConfirm: (Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var sourceId by remember { mutableStateOf<Long?>(null) }
    var targetId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge Tags") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Merge one tag into another. Cards with the source tag will be retagged.", fontSize = 13.sp)

                var expandedSource by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expandedSource, onExpandedChange = { expandedSource = it }) {
                    OutlinedTextField(
                        value = sourceId?.let { id -> tags.find { tag -> tag.id == id }?.name ?: "Select source" } ?: "Select source",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Source Tag (to merge FROM)") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedSource) },
                        singleLine = true
                    )
                    ExposedDropdownMenu(expanded = expandedSource, onDismissRequest = { expandedSource = false }) {
                        tags.forEach { t ->
                            DropdownMenuItem(text = { Text(t.name) }, onClick = { sourceId = t.id; expandedSource = false })
                        }
                    }
                }

                var expandedTarget by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expandedTarget, onExpandedChange = { expandedTarget = it }) {
                    OutlinedTextField(
                        value = targetId?.let { id -> tags.find { tag -> tag.id == id }?.name ?: "Select target" } ?: "Select target",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Tag (to merge INTO)") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedTarget) },
                        singleLine = true
                    )
                    ExposedDropdownMenu(expanded = expandedTarget, onDismissRequest = { expandedTarget = false }) {
                        tags.filter { it.id != sourceId }.forEach { t ->
                            DropdownMenuItem(text = { Text(t.name) }, onClick = { targetId = t.id; expandedTarget = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { sourceId?.let { s -> targetId?.let { t -> onConfirm(s, t) } } },
                enabled = sourceId != null && targetId != null
            ) { Text("Merge") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Tag Apply Dialog ──

@Composable
private fun TagApplyDialog(
    tag: CardTag,
    cards: List<KaiteyoCard>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    val filteredCards = remember(cards, searchQuery) {
        if (searchQuery.isBlank()) cards
        else cards.filter { it.character.contains(searchQuery) || it.meaning.contains(searchQuery) || it.deck.contains(searchQuery) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply \"${tag.name}\" to Cards") },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter cards...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("${selectedIds.size} of ${filteredCards.size} selected", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                            Spacer(Modifier.width(4.dp))
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
            TextButton(onClick = { onConfirm(selectedIds.toList()) }, enabled = selectedIds.isNotEmpty()) {
                Text("Apply (${selectedIds.size})")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

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

    AlertDialog(
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

// ════════════════════════════════════════════
// NOTES SYSTEM — Full note editor
// ════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorFullScreen(
    cards: List<KaiteyoCard> = emptyList(),
    onSaveNote: (String, String) -> Unit = { _, _ -> },
    onClose: () -> Unit = {}
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var selectedCardId by remember { mutableStateOf<String?>(null) }
    var editContent by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showPreview by remember { mutableStateOf(false) }
    var showFormattingHelp by remember { mutableStateOf(false) }

    val cardsWithNotes = cards.filter { it.notes.isNotBlank() }
    val cardsWithoutNotes = cards.filter { it.notes.isBlank() }

    val filteredCards = remember(cards, searchQuery) {
        if (searchQuery.isBlank()) cards
        else cards.filter {
            it.character.contains(searchQuery) || it.meaning.contains(searchQuery) ||
            it.notes.contains(searchQuery) || it.deck.contains(searchQuery)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    if (selectedCardId != null) {
                        IconButton(onClick = { showPreview = !showPreview }) {
                            Icon(if (showPreview) Icons.Default.Edit else Icons.Default.Visibility, "Toggle Preview")
                        }
                        IconButton(onClick = { showFormattingHelp = true }) {
                            Icon(Icons.Default.Help, "Formatting Help")
                        }
                        TextButton(onClick = {
                            selectedCardId?.let { onSaveNote(it, editContent) }
                            selectedCardId = null
                            editContent = ""
                        }) { Text("Save") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColors.surface,
                    titleContentColor = surfaceColors.textPrimary
                )
            )
        }
    ) { padding ->
        if (selectedCardId == null) {
            // Card list with notes overview
            Column(Modifier.fillMaxSize().padding(padding)) {
                // Stats
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${cardsWithNotes.size} with notes", fontSize = 12.sp, color = surfaceColors.textMuted)
                    Text("${cardsWithoutNotes.size} without", fontSize = 12.sp, color = surfaceColors.textMuted)
                    Text("${cards.size} total", fontSize = 12.sp, color = surfaceColors.textMuted)
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search cards...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredCards, key = { it.id }) { card ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedCardId = card.id
                                editContent = card.notes
                            },
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
                                if (card.notes.isNotBlank()) {
                                    Icon(Icons.Default.Description, null, Modifier.size(16.dp), tint = accent.primary)
                                } else {
                                    Icon(Icons.Default.Description, null, Modifier.size(16.dp), tint = surfaceColors.textMuted.copy(alpha = 0.4f))
                                }
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = surfaceColors.textMuted)
                            }
                        }
                    }
                }
            }
        } else {
            // Note editor
            val selectedCard = cards.find { it.id == selectedCardId }
            Column(Modifier.fillMaxSize().padding(padding)) {
                // Card info header
                if (selectedCard != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(surfaceColors.surfaceElevated)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedCard.character, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(selectedCard.meaning, fontSize = 13.sp, color = surfaceColors.textPrimary)
                            Text(selectedCard.deck, fontSize = 11.sp, color = surfaceColors.textMuted)
                        }
                        Spacer(Modifier.weight(1f))
                        // Flag indicator
                        if (selectedCard.flag != CardFlagType.None) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(selectedCard.flag.colorFromHex()))
                        }
                    }
                }

                // Formatting toolbar
                NoteFormattingToolbar(
                    onInsertBold = { editContent += "**bold**" },
                    onInsertItalic = { editContent += "*italic*" },
                    onInsertUnderline = { editContent += "<u>underline</u>" },
                    onInsertStrikethrough = { editContent += "~~strikethrough~~" },
                    onInsertHeader = { editContent += "\n## Header\n" },
                    onInsertLink = { editContent += "[text](url)" },
                    onInsertImage = { editContent += "![alt](image.png)" },
                    onInsertTable = { editContent += "\n| Col1 | Col2 |\n|------|------|\n| Cell | Cell |\n" },
                    onInsertCode = { editContent += "\n```\ncode\n```\n" },
                    onInsertChecklist = { editContent += "\n- [ ] Task\n" },
                    onInsertList = { editContent += "\n- Item\n" },
                    onInsertNumberedList = { editContent += "\n1. Item\n" },
                    onInsertQuote = { editContent += "\n> Quote\n" },
                    onInsertDivider = { editContent += "\n---\n" },
                    surfaceColors = surfaceColors,
                    accent = accent
                )

                if (showPreview) {
                    // Markdown preview
                    NotePreview(
                        content = editContent,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        surfaceColors = surfaceColors
                    )
                } else {
                    // Editor
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
                        placeholder = { Text("Write your notes here...\n\nMarkdown supported:\n- **Bold**\n- *Italic*\n- [Links](url)\n- ![Images](file.png)\n- Tables, code blocks, checklists, etc.") },
                        textStyle = TextStyle(fontSize = 14.sp, color = surfaceColors.textPrimary, lineHeight = 20.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent.primary.copy(alpha = 0.5f),
                            unfocusedBorderColor = surfaceColors.border.copy(alpha = 0.3f),
                            cursorColor = accent.primary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }
    }

    if (showFormattingHelp) {
        NoteFormattingHelpDialog(onDismiss = { showFormattingHelp = false })
    }
}

// ── Note Formatting Toolbar ──

@Composable
private fun NoteFormattingToolbar(
    onInsertBold: () -> Unit,
    onInsertItalic: () -> Unit,
    onInsertUnderline: () -> Unit,
    onInsertStrikethrough: () -> Unit,
    onInsertHeader: () -> Unit,
    onInsertLink: () -> Unit,
    onInsertImage: () -> Unit,
    onInsertTable: () -> Unit,
    onInsertCode: () -> Unit,
    onInsertChecklist: () -> Unit,
    onInsertList: () -> Unit,
    onInsertNumberedList: () -> Unit,
    onInsertQuote: () -> Unit,
    onInsertDivider: () -> Unit,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    var showMore by remember { mutableStateOf(false) }

    Column {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceColors.surfaceElevated)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                FormatButton(Icons.Default.FormatBold, "Bold") { onInsertBold() }
                FormatButton(Icons.Default.FormatItalic, "Italic") { onInsertItalic() }
                FormatButton(Icons.Default.FormatUnderlined, "Underline") { onInsertUnderline() }
                FormatButton(Icons.Default.FormatStrikethrough, "Strikethrough") { onInsertStrikethrough() }
            }
            item { Spacer(Modifier.width(4.dp)) }
            item {
                FormatButton(Icons.Default.Title, "Header") { onInsertHeader() }
                FormatButton(Icons.Default.Link, "Link") { onInsertLink() }
                FormatButton(Icons.Default.Image, "Image") { onInsertImage() }
                FormatButton(Icons.Default.TableChart, "Table") { onInsertTable() }
            }
            item { Spacer(Modifier.width(4.dp)) }
            item {
                FormatButton(Icons.Default.Code, "Code") { onInsertCode() }
                FormatButton(Icons.Default.CheckBox, "Checklist") { onInsertChecklist() }
                FormatButton(Icons.Default.FormatListBulleted, "List") { onInsertList() }
                FormatButton(Icons.Default.FormatListNumbered, "Numbered") { onInsertNumberedList() }
            }
            item { Spacer(Modifier.width(4.dp)) }
            item {
                FormatButton(Icons.Default.FormatQuote, "Quote") { onInsertQuote() }
                FormatButton(Icons.Default.HorizontalRule, "Divider") { onInsertDivider() }
            }
        }
    }
}

@Composable
private fun FormatButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(icon, contentDescription, Modifier.size(18.dp), tint = surfaceColors.textPrimary)
    }
}

// ── Note Preview ──

@Composable
private fun NotePreview(
    content: String,
    modifier: Modifier = Modifier,
    surfaceColors: SurfaceColors
) {
    Column(modifier = modifier) {
        val lines = content.split("\n")
        var inCodeBlock = false
        var inTable = false

        lines.forEach { line ->
            when {
                line.startsWith("```") -> {
                    inCodeBlock = !inCodeBlock
                    if (inCodeBlock) {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(surfaceColors.surfaceInteractive)
                                .padding(8.dp)
                        ) {
                            Text(line.removePrefix("```"), fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = surfaceColors.textPrimary)
                        }
                    }
                }
                inCodeBlock -> {
                    Text(line, fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = surfaceColors.textPrimary,
                        modifier = Modifier.padding(start = 8.dp))
                }
                line.startsWith("# ") -> Text(line.removePrefix("# "), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                line.startsWith("## ") -> Text(line.removePrefix("## "), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                line.startsWith("### ") -> Text(line.removePrefix("### "), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = surfaceColors.textPrimary)
                line.startsWith("> ") -> Text(line.removePrefix("> "), fontSize = 13.sp, color = surfaceColors.textMuted,
                    modifier = Modifier.padding(start = 8.dp).then(Modifier.fillMaxWidth().background(surfaceColors.surfaceInteractive.copy(alpha = 0.3f)).padding(8.dp)))
                line.startsWith("- [ ] ") -> Text("☐ ${line.removePrefix("- [ ] ")}", fontSize = 13.sp, color = surfaceColors.textPrimary)
                line.startsWith("- [x] ") -> Text("☑ ${line.removePrefix("- [x] ")}", fontSize = 13.sp, color = surfaceColors.textPrimary)
                line.startsWith("- ") -> Text("• ${line.removePrefix("- ")}", fontSize = 13.sp, color = surfaceColors.textPrimary)
                line.startsWith("---") -> HorizontalDivider(color = surfaceColors.border, modifier = Modifier.padding(vertical = 4.dp))
                line.startsWith("|") -> {
                    if (!inTable) { inTable = true }
                    val cells = line.split("|").filter { it.isNotBlank() }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        cells.forEach { cell ->
                            Text(cell.trim(), fontSize = 12.sp, color = surfaceColors.textPrimary,
                                modifier = Modifier.weight(1f))
                        }
                    }
                }
                line.startsWith("![") -> {
                    val alt = line.substringAfter("![").substringBefore("]")
                    Text("[Image: $alt]", fontSize = 13.sp, color = surfaceColors.textMuted)
                }
                line.startsWith("[") -> {
                    val text = line.substringAfter("[").substringBefore("]")
                    val url = line.substringAfter("(").substringBefore(")")
                    Text(text, fontSize = 13.sp, color = androidx.compose.ui.graphics.Color(0xFF7BC8FF),
                        textDecoration = TextDecoration.Underline)
                }
                line.isBlank() -> Spacer(Modifier.height(4.dp))
                else -> Text(line, fontSize = 13.sp, color = surfaceColors.textPrimary)
            }
            if (line.isBlank() && inTable) inTable = false
        }
    }
}

// ── Note Formatting Help Dialog ──

@Composable
private fun NoteFormattingHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Markdown Formatting") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                item {
                    Text("**Bold**", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("*Italic*", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("~~Strikethrough~~", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("[Link](url)", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("![Image](file.png)", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                }
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Text("Headers:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("# H1", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("## H2", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("### H3", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                }
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Text("Lists:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("- Unordered", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("1. Ordered", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("- [ ] Checklist", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("- [x] Done", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                }
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Text("Code & Tables:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("`inline code`", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("```\\ncode block\\n```", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("| Col1 | Col2 |", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("|------|------|", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                }
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Text("Other:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("> Quote", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("--- Divider", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } }
    )
}

// ════════════════════════════════════════════
// SHARED COMPONENTS
// ════════════════════════════════════════════

@Composable
fun StatCard2(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surfaceElevated),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = color ?: accent.primary)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                Text(label, fontSize = 10.sp, color = surfaceColors.textMuted)
            }
        }
    }
}

// ════════════════════════════════════════════
// FLAG SELECTOR DIALOG (reusable)
// ════════════════════════════════════════════

@Composable
fun TagFlagSelectorDialog(
    currentFlag: CardFlagType,
    onSelect: (CardFlagType) -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Select Flag", style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CardFlagType.entries.forEach { flag ->
                val color = flag.colorFromHex()
                val isSelected = currentFlag == flag
                IconButton(
                    onClick = { onSelect(flag) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(
                        modifier = Modifier.size(24.dp)
                            .clip(CircleShape)
                            .background(if (flag == CardFlagType.None) Color.Transparent else color)
                            .then(
                                if (isSelected) Modifier.border(2.dp, Color.White, CircleShape)
                                else Modifier
                            )
                            .then(
                                if (flag == CardFlagType.None) Modifier.border(1.dp, Color.Gray, CircleShape)
                                else Modifier
                            )
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════
// CARD STATUS SELECTOR DIALOG (reusable)
// ════════════════════════════════════════════

@Composable
fun TagCardStatusSelectorDialog(
    currentStatus: CardStatus,
    onSelect: (CardStatus) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Card Status") },
        text = {
            Column {
                CardStatus.entries.forEach { status ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onSelect(status) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentStatus == status,
                            onClick = { onSelect(status) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(status.displayName, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ════════════════════════════════════════════
// NOTE EDITOR DIALOG (reusable inline)
// ════════════════════════════════════════════

@Composable
fun TagNoteEditorDialog(
    initialContent: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Note") },
        text = {
            Column {
                NoteFormattingToolbar(
                    onInsertBold = { content += "**bold**" },
                    onInsertItalic = { content += "*italic*" },
                    onInsertUnderline = { content += "<u>underline</u>" },
                    onInsertStrikethrough = { content += "~~strikethrough~~" },
                    onInsertHeader = { content += "\n## Header\n" },
                    onInsertLink = { content += "[text](url)" },
                    onInsertImage = { content += "![alt](image.png)" },
                    onInsertTable = { content += "\n| Col1 | Col2 |\n|------|------|\n| Cell | Cell |\n" },
                    onInsertCode = { content += "\n```\ncode\n```\n" },
                    onInsertChecklist = { content += "\n- [ ] Task\n" },
                    onInsertList = { content += "\n- Item\n" },
                    onInsertNumberedList = { content += "\n1. Item\n" },
                    onInsertQuote = { content += "\n> Quote\n" },
                    onInsertDivider = { content += "\n---\n" },
                    surfaceColors = LocalSurfaceColors.current,
                    accent = LocalKaiteyoAccent.current
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                    placeholder = { Text("Write your note...") },
                    textStyle = TextStyle(fontSize = 14.sp)
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(content) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
