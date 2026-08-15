@file:OptIn(ExperimentalMaterial3Api::class)

package ua.syt0r.kanji.presentation.screen.main.screen.decks

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
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
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
                selectedTagIds = selectedTagIds,
                onMergeSelected = { ids ->
                    if (ids.size >= 2) {
                        // Merge every selected tag into the first selected one.
                        val target = ids.first()
                        ids.drop(1).forEach { onMergeTags(it, target) }
                        selectedTagIds = emptySet()
                        isSelectionMode = false
                    }
                },
                onDeleteSelected = { showBulkDeleteConfirm = true },
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
        KaiteyoAlertDialog(
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

    if (showBulkDeleteConfirm) {
        KaiteyoAlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text("Delete Tags") },
            text = { Text("Delete ${selectedTagIds.size} selected tag(s)? This will remove them from all cards.") },
            confirmButton = {
                TextButton(onClick = {
                    selectedTagIds.forEach { onDeleteTag(it) }
                    selectedTagIds = emptySet()
                    isSelectionMode = false
                    showBulkDeleteConfirm = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) { Text("Cancel") }
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
    selectedTagIds: Set<Long>,
    onMergeSelected: (Set<Long>) -> Unit,
    onDeleteSelected: (Set<Long>) -> Unit,
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
            // Bulk actions on the selected tags — real, not decorative.
            TextButton(
                onClick = { onMergeSelected(selectedTagIds) },
                enabled = selectedTagIds.size >= 2,
                modifier = Modifier.height(28.dp)
            ) {
                Text("Merge", fontSize = 11.sp)
            }
            TextButton(
                onClick = { onDeleteSelected(selectedTagIds) },
                enabled = selectedTagIds.isNotEmpty(),
                modifier = Modifier.height(28.dp)
            ) {
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
