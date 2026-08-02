package ua.syt0r.kanji.desktop.ui.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
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
import ua.syt0r.kanji.desktop.designsystem.DsSelect
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTagChip
import ua.syt0r.kanji.desktop.designsystem.DsTextField
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.model.CollectionDef
import ua.syt0r.kanji.desktop.model.CollectionKind
import ua.syt0r.kanji.desktop.model.SmartCollectionPresets
import ua.syt0r.kanji.desktop.model.ToastKind

// ============================================
// COLLECTIONS
// Browse collections (manual + smart), inspect
// contents, run reviews, pin/favorite/delete, and
// create new collections from smart presets.
// ============================================

@Composable
fun CollectionsView(state: AppState) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var createDialog by remember { mutableStateOf(false) }
    var smartDialog by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<String?>(null) }

    val selected = state.collections.collections.firstOrNull { it.id == selectedId }

    Row(Modifier.fillMaxSize()) {
        // Left: list
        Column(
            modifier = Modifier
                .width(280.dp)
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
            CollectionList(state, selectedId) { selectedId = it }
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
                CollectionDetail(state, selected, onDelete = { deleteId = selected.id })
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
                    state.activityLog.record(ua.syt0r.kanji.desktop.engine.history.ActivityCategory.Deck, "Created collection $name")
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
                state.activityLog.record(ua.syt0r.kanji.desktop.engine.history.ActivityCategory.Deck, "Deleted collection ${def?.name}")
            },
            onDismiss = { deleteId = null }
        )
    }
}

@Composable
private fun CollectionList(state: AppState, selectedId: String?, onSelect: (String) -> Unit) {
    val sc = surfaceColors()
    val ac = accent()
    val roots = state.collections.childrenOf(null).filter { !it.archived }
    val archived = state.collections.archived()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(roots.size, key = { roots[it].id }) { index ->
            val def = roots[index]
            val children = state.collections.childrenOf(def.id).filter { !it.archived }
            val cardsIn = state.collections.resolveCards(def, state.cards.toList())
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
private fun CollectionDetail(state: AppState, def: CollectionDef, onDelete: () -> Unit) {
    val sc = surfaceColors()
    val ac = accent()
    val cards = state.collections.resolveCards(def, state.cards.toList())

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
                    state.activityLog.record(ua.syt0r.kanji.desktop.engine.history.ActivityCategory.Deck, "Edited collection ${def.name}")
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
