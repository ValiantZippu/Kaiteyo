package ua.syt0r.kanji.desktop.ui.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.WorkspaceView
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsConfirmDialog
import ua.syt0r.kanji.desktop.designsystem.DsDialog
import ua.syt0r.kanji.desktop.designsystem.DsEmptyState
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsPromptDialog
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.adaptiveWidth
import ua.syt0r.kanji.desktop.designsystem.DsSearchField
import ua.syt0r.kanji.desktop.designsystem.DsSelect
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTagChip
import ua.syt0r.kanji.desktop.designsystem.DsTextField
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.model.CollectionDef
import ua.syt0r.kanji.desktop.model.CollectionKind
import ua.syt0r.kanji.desktop.model.SmartCollectionPresets
import ua.syt0r.kanji.desktop.model.ToastKind

// ============================================
// COLLECTIONS
// The library: browse collections (manual + smart,
// nested folders), search + sort the list, run
// reviews, pin/favorite/archive, duplicate, merge,
// move into folders, bulk-select, and create new
// collections from smart presets.
// ============================================

private enum class CollectionSortMode(val label: String) {
    Name("Name"),
    Size("Card count"),
    Recent("Recently created"),
    PinnedFirst("Pinned first")
}

@Composable
fun CollectionsView(state: AppState) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var createDialog by remember { mutableStateOf(false) }
    var smartDialog by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(CollectionSortMode.Name) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var mergeDialog by remember { mutableStateOf(false) }
    var moveDialog by remember { mutableStateOf(false) }
    var bulkDeleteConfirm by remember { mutableStateOf(false) }

    val selected = state.collections.collections.firstOrNull { it.id == selectedId }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Left: list — a width that follows the window (≈26%, clamped so it
        // stays usable on small windows and never swallows the detail pane).
        val listWidth = adaptiveWidth(maxWidth, 0.26f, 260.dp, 400.dp)
        Row(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(listWidth)
                .fillMaxSize()
                .padding(DsSpacing.Md)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = DsSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Collections",
                    color = surfaceColors().textPrimary,
                    fontSize = DsType.Title,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                DsIconButton(icon = Icons.Default.Add, onClick = { createDialog = true }, contentDescription = "New collection")
                DsIconButton(icon = Icons.Default.Folder, onClick = { smartDialog = true }, contentDescription = "Smart preset")
            }
            DsSearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search collections…",
                modifier = Modifier.padding(bottom = DsSpacing.Sm)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
            ) {
                DsSelect(
                    selected = sortMode,
                    options = CollectionSortMode.entries.toList(),
                    onSelected = { sortMode = it },
                    labelOf = { it.label },
                    modifier = Modifier.weight(1f)
                )
                DsIconButton(
                    icon = if (selectionMode) Icons.Default.SelectAll else Icons.Default.CheckBoxOutlineBlank,
                    onClick = {
                        selectionMode = !selectionMode
                        selectedIds = emptySet()
                    },
                    contentDescription = "Toggle selection mode",
                    tint = if (selectionMode) accent().primary else Color.Unspecified
                )
            }
            Spacer(Modifier.height(DsSpacing.Sm))
            CollectionList(
                state = state,
                selectedId = selectedId,
                onSelect = { selectedId = it },
                searchQuery = searchQuery,
                sortMode = sortMode,
                selectionMode = selectionMode,
                selectedIds = selectedIds,
                onToggleSelect = { id ->
                    selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                }
            )
            if (selectionMode && selectedIds.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = DsSpacing.Sm)
                        .clip(RoundedCornerShape(DsRadius.Md))
                        .background(surfaceColors().surfaceElevated)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                ) {
                    Text(
                        text = "${selectedIds.size}",
                        color = surfaceColors().textPrimary,
                        fontSize = DsType.Label,
                        fontWeight = FontWeight.SemiBold
                    )
                    DsButton(
                        text = "Merge into…",
                        icon = Icons.Default.MergeType,
                        kind = DsButtonKind.Secondary,
                        compact = true,
                        onClick = { mergeDialog = true }
                    )
                    DsButton(
                        text = "Move to…",
                        icon = Icons.Default.DriveFileMove,
                        kind = DsButtonKind.Secondary,
                        compact = true,
                        onClick = { moveDialog = true }
                    )
                    DsButton(
                        text = "Archive",
                        icon = Icons.Default.Archive,
                        kind = DsButtonKind.Ghost,
                        compact = true,
                        onClick = {
                            selectedIds.forEach { state.collections.toggleArchived(it) }
                            state.toastHost.show("Archived ${selectedIds.size} collections", kind = ToastKind.Info)
                            selectedIds = emptySet()
                        }
                    )
                    DsButton(
                        text = "Delete",
                        icon = Icons.Default.Delete,
                        kind = DsButtonKind.Danger,
                        compact = true,
                        onClick = { bulkDeleteConfirm = true }
                    )
                    DsButton(
                        text = "Done",
                        kind = DsButtonKind.Ghost,
                        compact = true,
                        onClick = { selectionMode = false; selectedIds = emptySet() }
                    )
                }
            }
        }

        // Right: detail
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(DsSpacing.Md)
        ) {
            if (selected == null) {
                DsEmptyState(
                    title = "Select a collection",
                    message = "Collections group cards for targeted study.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                CollectionDetail(
                    state = state,
                    def = selected,
                    onDelete = { deleteId = selected.id },
                    onMerge = { mergeDialog = true; selectedIds = setOf(selected.id) },
                    onMove = { moveDialog = true; selectedIds = setOf(selected.id) }
                )
            }
        }
        }
    }

    if (createDialog) {
        DsDialog(title = "New collection", onDismiss = { createDialog = false }) {
            var name by remember { mutableStateOf("") }
            var description by remember { mutableStateOf("") }
            DsTextField(value = name, onValueChange = { name = it }, placeholder = "Name", label = "Name")
            Spacer(Modifier.height(DsSpacing.Md))
            DsTextField(value = description, onValueChange = { description = it }, placeholder = "Optional description", label = "Description")
            Spacer(Modifier.height(DsSpacing.Xl))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)) {
                DsButton(text = "Cancel", kind = DsButtonKind.Ghost, onClick = { createDialog = false })
                DsButton(text = "Create", enabled = name.isNotBlank(), onClick = {
                    state.collections.create(name.trim(), description.trim(), CollectionKind.Manual)
                    state.activityLog.record(ActivityCategory.Deck, "Created collection $name")
                    createDialog = false
                })
            }
        }
    }

    if (smartDialog) {
        DsPromptDialog(
            title = "Add smart collection preset",
            placeholder = "e.g. Recently learned, Failed today, Low accuracy…",
            onConfirm = { value ->
                val rule = smartPresetRule(value)
                if (rule != null) {
                    val def = state.collections.create(value, "Smart collection", CollectionKind.Smart)
                    state.collections.update(def.copy(smartRule = rule))
                    state.toastHost.show("Smart collection '$value' created", kind = ToastKind.Success)
                } else {
                    state.toastHost.show("Unknown preset name", kind = ToastKind.Warning)
                }
            },
            onDismiss = { smartDialog = false }
        )
    }

    if (mergeDialog) {
        MergeDialog(
            state = state,
            sources = selectedIds.toList(),
            onDismiss = { mergeDialog = false }
        )
    }

    if (moveDialog) {
        MoveDialog(
            state = state,
            targets = selectedIds.toList(),
            onDismiss = { moveDialog = false }
        )
    }

    deleteId?.let { id ->
        val def = state.collections.collections.firstOrNull { it.id == id }
        DsConfirmDialog(
            title = "Delete collection",
            message = "Delete '${def?.name ?: id}'? Cards themselves are not deleted.",
            confirmText = "Delete",
            danger = true,
            onConfirm = {
                state.collections.delete(id)
                if (selectedId == id) selectedId = null
                state.activityLog.record(ActivityCategory.Deck, "Deleted collection ${def?.name}")
            },
            onDismiss = { deleteId = null }
        )
    }

    if (bulkDeleteConfirm) {
        DsConfirmDialog(
            title = "Delete ${selectedIds.size} collections?",
            message = "Their cards are kept; only the collections are removed.",
            confirmText = "Delete",
            danger = true,
            onConfirm = {
                selectedIds.forEach { state.collections.delete(it) }
                if (selectedId in selectedIds) selectedId = null
                selectedIds = emptySet()
                selectionMode = false
                state.toastHost.show("Collections deleted", kind = ToastKind.Info)
            },
            onDismiss = { bulkDeleteConfirm = false }
        )
    }
}

// ============================================
// MERGE DIALOG — fold one or more collections into a target
// ============================================

@Composable
private fun MergeDialog(state: AppState, sources: List<String>, onDismiss: () -> Unit) {
    val sc = surfaceColors()
    val targets = state.collections.collections.filter { it.id !in sources }
    DsDialog(title = "Merge into…", onDismiss = onDismiss) {
        Text(
            text = "Merge ${sources.size} collection(s) into a single target. The sources are removed after the merge.",
            color = sc.textSecondary,
            fontSize = DsType.Body
        )
        Spacer(Modifier.height(DsSpacing.Md))
        if (targets.isEmpty()) {
            Text("No other collections available.", color = sc.textMuted, fontSize = DsType.Body)
        }
        targets.forEach { target ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DsRadius.Md))
                    .background(sc.surfaceInteractive.copy(alpha = 0.4f))
                    .clickable {
                        val added = state.collections.merge(target.id, sources)
                        state.activityLog.record(ActivityCategory.Deck, "Merged ${sources.size} collections into ${target.name}")
                        state.toastHost.show("Merged into '${target.name}' (+$added cards)", kind = ToastKind.Success)
                        onDismiss()
                    }
                    .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(target.name, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                DsBadge(text = target.kind.name, tint = if (target.kind == CollectionKind.Smart) Color(0xFFA78BFA) else sc.textMuted)
            }
        }
    }
}

// ============================================
// MOVE DIALOG — reparent one or more collections into a folder
// ============================================

@Composable
private fun MoveDialog(state: AppState, targets: List<String>, onDismiss: () -> Unit) {
    val sc = surfaceColors()
    val ac = accent()
    val parents = state.collections.collections.filter { it.id !in targets }
    DsDialog(title = "Move to folder…", onDismiss = onDismiss) {
        Text(
            text = "Move ${targets.size} collection(s) under a folder (or the root).",
            color = sc.textSecondary,
            fontSize = DsType.Body
        )
        Spacer(Modifier.height(DsSpacing.Md))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DsRadius.Md))
                .background(sc.surfaceInteractive.copy(alpha = 0.4f))
                .clickable {
                    targets.forEach { state.collections.move(it, null) }
                    state.toastHost.show("Moved to root", kind = ToastKind.Success)
                    onDismiss()
                }
                .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Folder, null, tint = sc.textSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(DsSpacing.Sm))
            Text("Root (no folder)", color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium)
        }
        parents.forEach { parent ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DsRadius.Md))
                    .background(sc.surfaceInteractive.copy(alpha = 0.4f))
                    .clickable {
                        targets.forEach { state.collections.move(it, parent.id) }
                        state.toastHost.show("Moved under '${parent.name}'", kind = ToastKind.Success)
                        onDismiss()
                    }
                    .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DriveFileMove, null, tint = ac.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(DsSpacing.Sm))
                Text(parent.name, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                DsBadge(text = parent.kind.name.take(5), tint = sc.textMuted)
            }
        }
    }
}

// ============================================
// COLLECTION LIST
// ============================================

@Composable
private fun CollectionList(
    state: AppState,
    selectedId: String?,
    onSelect: (String) -> Unit,
    searchQuery: String,
    sortMode: CollectionSortMode,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    onToggleSelect: (String) -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()

    fun matches(def: CollectionDef): Boolean =
        searchQuery.isBlank() ||
            def.name.contains(searchQuery, ignoreCase = true) ||
            def.description.contains(searchQuery, ignoreCase = true)

    fun sorted(defs: List<CollectionDef>): List<CollectionDef> {
        val bySize = { def: CollectionDef -> state.collections.resolveCards(def, state.cards.toList(), state.library).size }
        return when (sortMode) {
            CollectionSortMode.Name -> defs.sortedBy { it.name.lowercase() }
            CollectionSortMode.Size -> defs.sortedByDescending { bySize(it) }
            CollectionSortMode.Recent -> defs.sortedByDescending { it.createdAt }
            CollectionSortMode.PinnedFirst -> defs.sortedWith(
                compareByDescending<CollectionDef> { it.pinned }
                    .thenByDescending { it.favorite }
                    .thenBy { it.name.lowercase() }
            )
        }
    }

    val roots = sorted(state.collections.childrenOf(null).filter { !it.archived && matches(it) })
    val archived = state.collections.archived()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(roots.size, key = { roots[it].id }) { index ->
            val def = roots[index]
            val children = state.collections.childrenOf(def.id).filter { !it.archived && matches(it) }
            val cardsIn = state.collections.resolveCards(def, state.cards.toList(), state.library)
            val interaction = remember { MutableInteractionSource() }
            val hovered by interaction.collectIsHoveredAsState()
            val selected = def.id == selectedId

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(DsRadius.Md))
                        .background(
                            when {
                                selected -> ac.primary.copy(alpha = 0.16f)
                                hovered -> sc.surfaceInteractive.copy(alpha = 0.6f)
                                else -> Color.Transparent
                            }
                        )
                        .clickable(interactionSource = interaction, indication = null) { onSelect(def.id) }
                        .hoverable(interaction)
                        .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectionMode) {
                        CollectionCheckbox(def.id in selectedIds) { onToggleSelect(def.id) }
                        Spacer(Modifier.width(DsSpacing.Xs))
                    }
                    if (def.pinned) {
                        Icon(Icons.Default.PushPin, null, tint = ac.primary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = def.name,
                            color = if (selected) ac.primary else sc.textPrimary,
                            fontSize = DsType.Body,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${cardsIn.size} cards",
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    }
                    DsBadge(text = def.kind.name.take(5), tint = if (def.kind == CollectionKind.Smart) Color(0xFFA78BFA) else sc.textMuted)
                }
                children.forEach { child ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(DsRadius.Md))
                            .background(if (child.id == selectedId) ac.primary.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { onSelect(child.id) }
                            .padding(start = DsSpacing.Xl, end = DsSpacing.Md, top = DsSpacing.Sm, bottom = DsSpacing.Sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectionMode) {
                            CollectionCheckbox(child.id in selectedIds) { onToggleSelect(child.id) }
                            Spacer(Modifier.width(DsSpacing.Xs))
                        }
                        Text(
                            text = "└  ${child.name}",
                            color = if (child.id == selectedId) ac.primary else sc.textSecondary,
                            fontSize = DsType.Body,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = state.collections.resolveCards(child, state.cards.toList()).size.toString(),
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    }
                }
            }
        }

        if (archived.isNotEmpty()) {
            item(key = "archived-header") {
                Text(
                    text = "ARCHIVED",
                    color = sc.textMuted,
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm)
                )
            }
            items(archived.size, key = { archived[it].id }) { index ->
                val def = archived[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(DsRadius.Md))
                        .background(if (def.id == selectedId) ac.primary.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onSelect(def.id) }
                        .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Archive, null, tint = sc.textMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = def.name,
                        color = sc.textMuted,
                        fontSize = DsType.Body,
                        modifier = Modifier.weight(1f)
                    )
                    DsIconButton(
                        icon = Icons.Default.Unarchive,
                        onClick = { state.collections.toggleArchived(def.id) },
                        contentDescription = "Restore",
                        size = 26.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionCheckbox(checked: Boolean, onClick: () -> Unit) {
    val sc = surfaceColors()
    val ac = accent()
    Icon(
        imageVector = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
        contentDescription = if (checked) "Selected" else "Not selected",
        tint = if (checked) ac.primary else sc.textMuted,
        modifier = Modifier
            .size(18.dp)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun CollectionDetail(
    state: AppState,
    def: CollectionDef,
    onDelete: () -> Unit,
    onMerge: () -> Unit,
    onMove: () -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()
    // Deck-owned cards count toward the collection — the Library is the hub.
    val cards = state.collections.resolveCards(def, state.cards.toList(), state.library)
    val decks = state.collections.resolveDecks(def, state.library)

    var editOpen by remember(def.id) { mutableStateOf(false) }
    var editName by remember(def.id) { mutableStateOf(def.name) }
    var editDescription by remember(def.id) { mutableStateOf(def.description) }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
        DsCard {
            Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                            Text(
                                text = def.name,
                                color = sc.textPrimary,
                                fontSize = DsType.Heading,
                                fontWeight = FontWeight.Bold
                            )
                            DsBadge(text = def.kind.name, tint = if (def.kind == CollectionKind.Smart) Color(0xFFA78BFA) else Color(0xFF7BC8FF))
                        }
                        if (def.description.isNotBlank()) {
                            Text(def.description, color = sc.textMuted, fontSize = DsType.Body)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DsIconButton(
                            icon = if (def.pinned) Icons.Default.PushPin else Icons.Default.PushPin,
                            onClick = { state.collections.togglePinned(def.id) },
                            contentDescription = "Pin",
                            tint = if (def.pinned) ac.primary else Color.Unspecified
                        )
                        DsIconButton(
                            icon = if (def.favorite) Icons.Default.Star else Icons.Default.StarBorder,
                            onClick = { state.collections.toggleFavorite(def.id) },
                            contentDescription = "Favorite",
                            tint = if (def.favorite) Color(0xFFFFD93D) else Color.Unspecified
                        )
                        DsIconButton(
                            icon = Icons.Default.Delete,
                            onClick = onDelete,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF6B6B)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                ) {
                    DsButton(
                        text = "Study",
                        icon = Icons.Default.PlayArrow,
                        onClick = { state.startReview(collection = def) },
                        compact = true
                    )
                    DsButton(
                        text = "Browse",
                        icon = Icons.Default.GridView,
                        kind = DsButtonKind.Secondary,
                        onClick = {
                            state.selectedCardIds.clear()
                            cards.take(200).forEach { state.selectedCardIds.add(it.id) }
                            state.currentView = WorkspaceView.Browser
                        },
                        compact = true
                    )
                    DsButton(
                        text = "Edit",
                        icon = Icons.Default.Edit,
                        kind = DsButtonKind.Secondary,
                        onClick = {
                            editName = def.name
                            editDescription = def.description
                            editOpen = true
                        },
                        compact = true
                    )
                    DsButton(
                        text = "Statistics",
                        icon = Icons.Default.BarChart,
                        kind = DsButtonKind.Secondary,
                        onClick = { state.currentView = WorkspaceView.Statistics },
                        compact = true
                    )
                    DsButton(
                        text = "Duplicate",
                        icon = Icons.Default.ContentCopy,
                        kind = DsButtonKind.Secondary,
                        onClick = {
                            val copy = state.collections.duplicate(def)
                            state.toastHost.show("Duplicated as '${copy.name}'", kind = ToastKind.Success)
                        },
                        compact = true
                    )
                    DsButton(
                        text = "Merge into…",
                        icon = Icons.Default.MergeType,
                        kind = DsButtonKind.Secondary,
                        onClick = onMerge,
                        compact = true
                    )
                    DsButton(
                        text = "Move to…",
                        icon = Icons.Default.DriveFileMove,
                        kind = DsButtonKind.Secondary,
                        onClick = onMove,
                        compact = true
                    )
                    DsButton(
                        text = "Export",
                        icon = Icons.Default.FileDownload,
                        kind = DsButtonKind.Secondary,
                        onClick = {
                            val json = state.collections.export(def, cards)
                            copyToClipboard(json)
                            state.toastHost.show("Exported ${cards.size} cards to clipboard", kind = ToastKind.Success)
                        },
                        compact = true
                    )
                    DsButton(
                        text = if (def.archived) "Restore" else "Archive",
                        icon = if (def.archived) Icons.Default.Unarchive else Icons.Default.Archive,
                        kind = DsButtonKind.Ghost,
                        onClick = {
                            state.collections.toggleArchived(def.id)
                            state.toastHost.show(if (def.archived) "Restored '${def.name}'" else "Archived '${def.name}'", kind = ToastKind.Info)
                        },
                        compact = true
                    )
                    DsButton(
                        text = "Delete",
                        icon = Icons.Default.Delete,
                        kind = DsButtonKind.Danger,
                        onClick = onDelete,
                        compact = true
                    )
                }
            }
        }

        if (def.smartRule != null) {
            DsCard {
                Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text("Smart rule", color = sc.textMuted, fontSize = DsType.Caption, fontWeight = FontWeight.Medium)
                    Text(
                        text = def.smartRule.conditions.joinToString(" AND ") { c -> "${c.field} ${c.operator} ${c.value}" }.ifBlank { "Match all cards" },
                        color = sc.textSecondary,
                        fontSize = DsType.Body
                    )
                }
            }
        }

        if (decks.isNotEmpty()) {
            Text("Decks", color = sc.textMuted, fontSize = DsType.Caption, fontWeight = FontWeight.Medium)
            DsCard {
                Column(
                    Modifier.padding(DsSpacing.Sm),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    decks.forEach { deck ->
                        val deckCards = state.library.cardsIn(deck, state.cards.toList())
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(DsRadius.Md))
                                .background(sc.surfaceInteractive.copy(alpha = 0.4f))
                                .clickable { state.currentView = WorkspaceView.Library }
                                .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = deck.icon.ifBlank { deck.kind.glyph },
                                color = ac.primary,
                                fontSize = DsType.Title,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(36.dp)
                            )
                            Column(Modifier.weight(1f)) {
                                Text(deck.name, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium)
                                Text("${deck.kind.label} · ${deckCards.size} cards", color = sc.textMuted, fontSize = DsType.Caption)
                            }
                            DsBadge(text = deck.kind.label, tint = ac.primary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(DsSpacing.Sm))
        }

        Text("Contents", color = sc.textMuted, fontSize = DsType.Caption, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            cards.take(48).forEach { card ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(DsRadius.Sm))
                        .background(sc.surfaceInteractive.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = card.character,
                        color = sc.textPrimary,
                        fontSize = DsType.BodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Text(
            text = if (cards.isEmpty()) "This collection is empty." else "Showing ${cards.size} cards.",
            color = sc.textMuted,
            fontSize = DsType.Caption
        )
    }

    if (editOpen) {
        DsDialog(title = "Edit collection", onDismiss = { editOpen = false }) {
            DsTextField(value = editName, onValueChange = { editName = it }, placeholder = "Name", label = "Name")
            Spacer(Modifier.height(DsSpacing.Md))
            DsTextField(value = editDescription, onValueChange = { editDescription = it }, placeholder = "Optional description", label = "Description")
            Spacer(Modifier.height(DsSpacing.Xl))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)) {
                DsButton(text = "Cancel", kind = DsButtonKind.Ghost, onClick = { editOpen = false })
                DsButton(text = "Save", enabled = editName.isNotBlank(), onClick = {
                    state.collections.update(def.copy(name = editName.trim(), description = editDescription.trim()))
                    state.activityLog.record(ActivityCategory.Deck, "Edited collection ${def.name}")
                    editOpen = false
                })
            }
        }
    }
}

private fun copyToClipboard(text: String) {
    runCatching {
        java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(java.awt.datatransfer.StringSelection(text), null)
    }
}

private fun smartPresetRule(label: String): ua.syt0r.kanji.desktop.model.SmartCollectionRule? = when (label.lowercase()) {
    "recently learned", "recent" -> SmartCollectionPresets.recentlyLearned(1)
    "failed today" -> SmartCollectionPresets.failedToday()
    "failed this week" -> SmartCollectionPresets.failedThisWeek()
    "low accuracy" -> SmartCollectionPresets.lowAccuracy(0.6f)
    "not reviewed" -> SmartCollectionPresets.notReviewed()
    "flagged" -> SmartCollectionPresets.flagged()
    "favorite" -> SmartCollectionPresets.favorite()
    else -> null
}
