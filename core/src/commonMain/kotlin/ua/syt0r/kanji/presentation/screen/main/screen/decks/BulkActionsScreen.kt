package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
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
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.datetime.Clock

// ============================================
// KAITEYO v1.2 — BULK ACTIONS SCREEN
// Multi-select card operations:
// Tag, Flag, Delete, Move, Suspend, Bury,
// Archive, Export, Reschedule, Change Deck
// ============================================

data class BulkActionItem(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val destructive: Boolean = false,
    val requiresConfirm: Boolean = true
)

private val bulkActionsList = listOf(
    BulkActionItem("tag", "Add Tags", "Apply tags to selected cards", Icons.Default.Label, Color(0xFFA78BFA)),
    BulkActionItem("flag", "Set Flag", "Assign a flag color to cards", Icons.Default.Flag, Color(0xFFFF6B6B)),
    BulkActionItem("move", "Move Deck", "Move cards to another deck", Icons.Default.DriveFileMove, Color(0xFFFEAB57)),
    BulkActionItem("suspend", "Suspend", "Suspend selected cards", Icons.Default.Block, Color(0xFFFFD93D)),
    BulkActionItem("bury", "Bury", "Bury cards until next day", Icons.Default.VisibilityOff, Color(0xFF7BC8FF)),
    BulkActionItem("archive", "Archive", "Archive cards for later", Icons.Default.Archive, Color(0xFFB0B0B0)),
    BulkActionItem("reschedule", "Reschedule", "Change due dates & intervals", Icons.Default.Schedule, Color(0xFFC2FC8B)),
    BulkActionItem("changeDeck", "Change Deck", "Move to a different deck", Icons.Default.Folder, Color(0xFFA78BFA)),
    BulkActionItem("export", "Export Selected", "Export cards to a file", Icons.Default.FileDownload, Color(0xFF7BC8FF)),
    BulkActionItem("delete", "Delete", "Permanently delete cards", Icons.Default.Delete, Color(0xFFFF6B6B), destructive = true),
    BulkActionItem("reset", "Reset Progress", "Reset card to new state", Icons.Default.Refresh, Color(0xFFFFD93D), requiresConfirm = true),
    BulkActionItem("duplicate", "Duplicate", "Create copies of cards", Icons.Default.ContentCopy, Color(0xFFFEAB57)),
    BulkActionItem("merge", "Merge Duplicates", "Merge duplicate cards", Icons.Default.MergeType, Color(0xFFC2FC8B), requiresConfirm = true),
    BulkActionItem("reposition", "Reposition", "Change card position/order", Icons.Default.SwapVert, Color(0xFFA78BFA))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkActionsFullScreen(
    cards: List<KaiteyoCard>,
    tags: List<CardTag>,
    onBulkOperation: (String, List<String>) -> Unit = { _, _ -> },
    onClose: () -> Unit = {}
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    // Selection state
    var searchQuery by remember { mutableStateOf("") }
    var selectedCardIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectAll by remember { mutableStateOf(false) }
    var showActionDialog by remember { mutableStateOf<String?>(null) }
    var showResultMessage by remember { mutableStateOf<String?>(null) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showFlagPicker by remember { mutableStateOf(false) }
    var showDeckPicker by remember { mutableStateOf(false) }
    var showConfirmDelete by remember { mutableStateOf(false) }

    // Filter state
    var statusFilter by remember { mutableStateOf<CardStatus?>(null) }
    var flagFilter by remember { mutableStateOf<CardFlagType?>(null) }
    var deckFilter by remember { mutableStateOf<String?>(null) }

    val filteredCards = remember(cards, searchQuery, statusFilter, flagFilter, deckFilter) {
        cards.filter { card ->
            (searchQuery.isBlank() ||
                    card.character.contains(searchQuery, ignoreCase = true) ||
                    card.reading.contains(searchQuery, ignoreCase = true) ||
                    card.meaning.contains(searchQuery, ignoreCase = true) ||
                    card.deck.contains(searchQuery, ignoreCase = true) ||
                    card.tagNames.any { it.contains(searchQuery, ignoreCase = true) }) &&
                    (statusFilter == null || card.status == statusFilter) &&
                    (flagFilter == null || card.flag == flagFilter) &&
                    (deckFilter == null || card.deck == deckFilter)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Bulk Actions")
                        if (selectedCardIds.isNotEmpty()) {
                            Text(
                                "${selectedCardIds.size} selected",
                                fontSize = 12.sp,
                                color = surfaceColors.textMuted
                            )
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    if (selectedCardIds.isNotEmpty()) {
                        TextButton(onClick = {
                            selectedCardIds = emptySet()
                            selectAll = false
                        }) {
                            Text("Deselect All", color = surfaceColors.textMuted)
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search cards to select...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = surfaceColors.textMuted) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent.primary,
                    unfocusedBorderColor = surfaceColors.border
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Filter chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                    label = { Text("All", fontSize = 11.sp) },
                    modifier = Modifier.height(28.dp)
                )
                CardStatus.entries.take(4).forEach { status ->
                    FilterChip(
                        selected = statusFilter == status,
                        onClick = { statusFilter = if (statusFilter == status) null else status },
                        label = { Text(status.displayName, fontSize = 11.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            // Select all bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(surfaceColors.surfaceElevated.copy(alpha = 0.5f))
                    .clickable {
                        if (selectedCardIds.size == filteredCards.size) {
                            selectedCardIds = emptySet()
                            selectAll = false
                        } else {
                            selectedCardIds = filteredCards.map { it.id }.toSet()
                            selectAll = true
                        }
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selectedCardIds.size == filteredCards.size && filteredCards.isNotEmpty(),
                    onCheckedChange = {
                        if (it) {
                            selectedCardIds = filteredCards.map { card -> card.id }.toSet()
                        } else {
                            selectedCardIds = emptySet()
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Select All (${filteredCards.size} cards)",
                    fontSize = 13.sp,
                    color = surfaceColors.textPrimary
                )
            }

            // Action buttons grid
            if (selectedCardIds.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(bulkActionsList) { action ->
                        ActionButton(
                            action = action,
                            surfaceColors = surfaceColors,
                            accent = accent,
                            onClick = {
                                when (action.id) {
                                    "tag" -> showTagPicker = true
                                    "flag" -> showFlagPicker = true
                                    "move", "changeDeck" -> showDeckPicker = true
                                    "delete" -> showConfirmDelete = true
                                    else -> {
                                        onBulkOperation(action.id, selectedCardIds.toList())
                                        showResultMessage = "${action.name} applied to ${selectedCardIds.size} cards"
                                        selectedCardIds = emptySet()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Cards list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredCards, key = { it.id }) { card ->
                    val isSelected = card.id in selectedCardIds
                    CardListItem(
                        card = card,
                        isSelected = isSelected,
                        surfaceColors = surfaceColors,
                        accent = accent,
                        onClick = {
                            selectedCardIds = if (isSelected) {
                                selectedCardIds - card.id
                            } else {
                                selectedCardIds + card.id
                            }
                        }
                    )
                }
            }
        }
    }

    // Tag picker dialog
    if (showTagPicker) {
        TagPickerDialog(
            tags = tags,
            surfaceColors = surfaceColors,
            accent = accent,
            onConfirm = { tagIds ->
                onBulkOperation("tag", selectedCardIds.toList())
                showResultMessage = "Tags applied to ${selectedCardIds.size} cards"
                selectedCardIds = emptySet()
                showTagPicker = false
            },
            onDismiss = { showTagPicker = false }
        )
    }

    // Flag picker dialog
    if (showFlagPicker) {
        FlagPickerDialog(
            surfaceColors = surfaceColors,
            accent = accent,
            onConfirm = { flag ->
                onBulkOperation("flag", selectedCardIds.toList())
                showResultMessage = "Flag applied to ${selectedCardIds.size} cards"
                selectedCardIds = emptySet()
                showFlagPicker = false
            },
            onDismiss = { showFlagPicker = false }
        )
    }

    // Deck picker dialog
    if (showDeckPicker) {
        val availableDecks = remember(cards) {
            cards.map { it.deck }.filter { it.isNotBlank() }.distinct().sorted()
        }
        DeckPickerDialog(
            decks = availableDecks,
            surfaceColors = surfaceColors,
            accent = accent,
            onConfirm = { deckId ->
                onBulkOperation("changeDeck", selectedCardIds.toList())
                showResultMessage = "Cards moved to new deck"
                selectedCardIds = emptySet()
                showDeckPicker = false
            },
            onDismiss = { showDeckPicker = false }
        )
    }

    // Delete confirmation dialog
    if (showConfirmDelete) {
        KaiteyoAlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Delete Cards?") },
            text = {
                Text("Are you sure you want to permanently delete ${selectedCardIds.size} cards? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBulkOperation("delete", selectedCardIds.toList())
                        showResultMessage = "${selectedCardIds.size} cards deleted"
                        selectedCardIds = emptySet()
                        showConfirmDelete = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Result snackbar
    showResultMessage?.let { message ->
        KaiteyoAlertDialog(
            onDismissRequest = { showResultMessage = null },
            confirmButton = {
                TextButton(onClick = { showResultMessage = null }) {
                    Text("OK")
                }
            },
            title = { Text("Done") },
            text = { Text(message) }
        )
    }
}

@Composable
private fun ActionButton(
    action: BulkActionItem,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (action.destructive) Color(0xFFFF6B6B).copy(alpha = 0.1f)
            else surfaceColors.surfaceElevated
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                action.icon,
                null,
                Modifier.size(28.dp),
                tint = if (action.destructive) Color(0xFFFF6B6B) else action.color
            )
            Spacer(Modifier.height(4.dp))
            Text(
                action.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (action.destructive) Color(0xFFFF6B6B) else surfaceColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CardListItem(
    card: KaiteyoCard,
    isSelected: Boolean,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) accent.primary.copy(alpha = 0.1f) else surfaceColors.surfaceElevated)
            .clickable(onClick = onClick)
            .then(if (isSelected) Modifier.border(1.dp, accent.primary.copy(alpha = 0.3f), RoundedCornerShape(10.dp)) else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onClick() }
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                card.character,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = surfaceColors.textPrimary
            )
            Text(
                "${card.reading} · ${card.meaning}",
                fontSize = 12.sp,
                color = surfaceColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                card.status.displayName,
                fontSize = 11.sp,
                color = statusColor(card.status)
            )
            if (card.flag != CardFlagType.None) {
                Text(
                    "● ${card.flag.displayName}",
                    fontSize = 10.sp,
                    color = card.flag.toColor()
                )
            }
        }
    }
}

@Composable
private fun TagPickerDialog(
    tags: List<CardTag>,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme,
    onConfirm: (List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTagIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var searchTagQuery by remember { mutableStateOf("") }

    val filteredTags = if (searchTagQuery.isBlank()) tags
    else tags.filter { it.name.contains(searchTagQuery, ignoreCase = true) }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Tags to Apply") },
        text = {
            Column(Modifier.heightIn(max = 400.dp)) {
                OutlinedTextField(
                    value = searchTagQuery,
                    onValueChange = { searchTagQuery = it },
                    placeholder = { Text("Search tags...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent.primary,
                        unfocusedBorderColor = surfaceColors.border
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(filteredTags) { tag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTagIds = if (tag.id in selectedTagIds) {
                                        selectedTagIds - tag.id
                                    } else {
                                        selectedTagIds + tag.id
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = tag.id in selectedTagIds,
                                onCheckedChange = {
                                    selectedTagIds = if (tag.id in selectedTagIds) {
                                        selectedTagIds - tag.id
                                    } else {
                                        selectedTagIds + tag.id
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(tag.color.toComposeColor())
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(tag.name, color = surfaceColors.textPrimary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedTagIds.toList()) },
                enabled = selectedTagIds.isNotEmpty()
            ) {
                Text("Apply Tags")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun FlagPickerDialog(
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme,
    onConfirm: (CardFlagType) -> Unit,
    onDismiss: () -> Unit
) {
    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Flag") },
        text = {
            Column {
                CardFlagType.entries.filter { it != CardFlagType.None }.forEach { flag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onConfirm(flag) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Flag,
                            null,
                            Modifier.size(24.dp),
                            tint = flag.toColor()
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(flag.displayName, color = surfaceColors.textPrimary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DeckPickerDialog(
    decks: List<String>,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("Select Destination Deck") },
        text = {
            if (decks.isEmpty()) {
                Text(
                    "No destination decks available.",
                    color = surfaceColors.textMuted,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyColumn(Modifier.heightIn(max = 350.dp)) {
                    items(decks) { deckName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onConfirm(deckName) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                null,
                                Modifier.size(20.dp),
                                tint = accent.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(deckName, color = surfaceColors.textPrimary)
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Helper to convert flag color
@Composable
private fun CardFlagType.toColor(): Color {
    val sem = LocalKaiteyoSemanticColors.current
    return when (this) {
        CardFlagType.None -> Color.Transparent
        CardFlagType.Red -> sem.flagRed
        CardFlagType.Orange -> sem.flagOrange
        CardFlagType.Yellow -> sem.flagYellow
        CardFlagType.Green -> sem.flagGreen
        CardFlagType.Blue -> sem.flagBlue
        CardFlagType.Purple -> sem.flagPurple
        CardFlagType.Gray -> sem.cardSuspended
    }
}

// Helper to convert tag color string to Compose Color
private fun String.toComposeColor(): Color {
    return try {
        val hex = removePrefix("#")
        Color(
            red = hex.substring(0, 2).toInt(16) / 255f,
            green = hex.substring(2, 4).toInt(16) / 255f,
            blue = hex.substring(4, 6).toInt(16) / 255f,
            alpha = if (hex.length >= 8) hex.substring(6, 8).toInt(16) / 255f else 1f
        )
    } catch (e: Exception) {
        Color.Gray
    }
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
