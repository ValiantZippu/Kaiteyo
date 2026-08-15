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
import ua.syt0r.kanji.presentation.common.ui.KaiteyoAlertDialog

// ── Tag Create Dialog ──

@Composable
fun TagCreateDialog(
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

    KaiteyoAlertDialog(
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
fun TagEditDialog(
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

    KaiteyoAlertDialog(
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
fun TagMergeDialog(
    tags: List<CardTag>,
    onConfirm: (Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var sourceId by remember { mutableStateOf<Long?>(null) }
    var targetId by remember { mutableStateOf<Long?>(null) }

    KaiteyoAlertDialog(
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
fun TagApplyDialog(
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

    KaiteyoAlertDialog(
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
