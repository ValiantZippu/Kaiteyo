package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.ui.KaiteyoAlertDialog

// ============================================
// TAG MANAGER — Full UI
// Create, rename, merge, delete, color, nest
// ============================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TagManagerScreen(
    tags: List<CardTag>,
    onAddTag: (String, String, Long?) -> Unit,
    onUpdateTag: (Long, String, String, Long?) -> Unit,
    onDeleteTag: (Long) -> Unit,
    onMergeTags: (Long, Long) -> Unit,
    onClose: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf<CardTag?>(null) }
    var showMergeDialog by remember { mutableStateOf(false) }
    var mergeSourceTag by remember { mutableStateOf<CardTag?>(null) }

    val filteredTags = remember(tags, searchQuery) {
        if (searchQuery.isBlank()) tags
        else tags.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tag Manager") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "Close")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add Tag")
                    }
                    if (tags.size >= 2) {
                        IconButton(onClick = { showMergeDialog = true }) {
                            Icon(Icons.Default.MergeType, "Merge Tags")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search tags...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(Modifier.height(12.dp))

            // Tag list
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(filteredTags, key = { it.id }) { tag ->
                    TagListItem(
                        tag = tag,
                        nestedLevel = 0,
                        onEdit = { editingTag = it },
                        onDelete = { onDeleteTag(it.id) },
                        onMerge = { mergeSourceTag = it; showMergeDialog = true }
                    )
                }
                if (filteredTags.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No tags found", color = surfaceColors.textMuted)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        TagEditDialog(
            title = "Add Tag",
            initialName = "",
            initialColor = "#808080",
            initialParentId = null,
            tags = tags,
            onConfirm = { name, color, parentId ->
                onAddTag(name, color, parentId)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingTag?.let { tag ->
        TagEditDialog(
            title = "Edit Tag",
            initialName = tag.name,
            initialColor = tag.color,
            initialParentId = tag.parentId,
            tags = tags.filter { it.id != tag.id },
            onConfirm = { name, color, parentId ->
                onUpdateTag(tag.id, name, color, parentId)
                editingTag = null
            },
            onDismiss = { editingTag = null }
        )
    }

    if (showMergeDialog) {
        MergeTagsDialog(
            tags = tags,
            onMerge = { sourceId, targetId ->
                onMergeTags(sourceId, targetId)
                showMergeDialog = false
            },
            onDismiss = { showMergeDialog = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagListItem(
    tag: CardTag,
    nestedLevel: Int,
    onEdit: (CardTag) -> Unit,
    onDelete: (CardTag) -> Unit,
    onMerge: (CardTag) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val tagColor = tag.getDisplayColor()
    val bgColor by animateColorAsState(
        targetValue = tagColor.copy(alpha = 0.12f),
        animationSpec = tween(200), label = "tagBg"
    )
    val surfaceColors = LocalSurfaceColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (nestedLevel * 20).dp)
            .clip(RoundedCornerShape(10.dp))
            .background(surfaceColors.surface)
            .combinedClickable(
                onClick = { onEdit(tag) },
                onLongClick = { showMenu = true }
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(tagColor)
        )
        Spacer(Modifier.width(10.dp))
        // Tag name with hierarchy indicator
        Column(Modifier.weight(1f)) {
            Text(
                text = tag.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (tag.parentId != null) {
                Text(
                    text = "Nested tag",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Actions
        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.MoreVert, "Options", modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = { showMenu = false; onEdit(tag) },
                    leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)) }
                )
                DropdownMenuItem(
                    text = { Text("Merge...") },
                    onClick = { showMenu = false; onMerge(tag) },
                    leadingIcon = { Icon(Icons.Default.MergeType, null, Modifier.size(18.dp)) }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; onDelete(tag) },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, null, Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error)
                    }
                )
            }
        }
    }
}

@Composable
private fun TagEditDialog(
    title: String,
    initialName: String,
    initialColor: String,
    initialParentId: Long?,
    tags: List<CardTag>,
    onConfirm: (String, String, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    var parentId by remember { mutableStateOf(initialParentId) }
    var showParentSelector by remember { mutableStateOf(false) }

    val presetColors = listOf(
        "#FF6B6B", "#FEAB57", "#FFD93D", "#C2FC8B",
        "#7BC8FF", "#A78BFA", "#B0B0B0", "#808080"
    )

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tag name") },
                    placeholder = { Text("e.g., jlpt-n5, my-tag::subtag") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Color picker
                Text("Color", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetColors.forEach { hex ->
                        val isSelected = hex == selectedColor
                        val color = try {
                            val h = hex.removePrefix("#")
                            Color(
                                h.substring(0..1).toInt(16),
                                h.substring(2..3).toInt(16),
                                h.substring(4..5).toInt(16),
                                h.substring(6..7).toInt(16)
                            )
                        } catch (_: Exception) { Color.Gray }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                // Parent tag selector
                Text("Parent tag (optional)", style = MaterialTheme.typography.labelMedium)
                if (parentId != null) {
                    val parentTag = tags.find { it.id == parentId }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SubdirectoryArrowRight, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(parentTag?.name ?: "Unknown", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { parentId = null }) {
                            Text("Clear")
                        }
                    }
                } else {
                    TextButton(onClick = { showParentSelector = true }) {
                        Text("Set parent tag...")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), selectedColor, parentId) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showParentSelector && tags.isNotEmpty()) {
        KaiteyoAlertDialog(
            onDismissRequest = { showParentSelector = false },
            title = { Text("Select Parent Tag") },
            text = {
                LazyColumn(Modifier.height(300.dp)) {
                    items(tags) { tag ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { parentId = tag.id; showParentSelector = false }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(tag.getDisplayColor()))
                            Spacer(Modifier.width(8.dp))
                            Text(tag.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showParentSelector = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MergeTagsDialog(
    tags: List<CardTag>,
    onMerge: (Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var sourceTagId by remember { mutableStateOf<Long?>(null) }
    var targetTagId by remember { mutableStateOf<Long?>(null) }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge Tags") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Merge all cards from one tag into another, then delete the source tag.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                Text("Source tag (will be deleted)", style = MaterialTheme.typography.labelMedium)
                tags.forEach { tag ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sourceTagId == tag.id) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { sourceTagId = tag.id }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = sourceTagId == tag.id, onClick = { sourceTagId = tag.id })
                        Spacer(Modifier.width(4.dp))
                        Text(tag.name)
                    }
                }

                HorizontalDivider()

                Text("Target tag (will receive cards)", style = MaterialTheme.typography.labelMedium)
                tags.forEach { tag ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (targetTagId == tag.id) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { targetTagId = tag.id }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = targetTagId == tag.id, onClick = { targetTagId = tag.id })
                        Spacer(Modifier.width(4.dp))
                        Text(tag.name)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { sourceTagId?.let { s -> targetTagId?.let { t -> onMerge(s, t) } } },
                enabled = sourceTagId != null && targetTagId != null && sourceTagId != targetTagId
            ) { Text("Merge") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// clickable is imported from androidx.compose.foundation.clickable
