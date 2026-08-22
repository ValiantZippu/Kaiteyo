package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoSemanticColors
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.ui.KaiteyoAlertDialog
import ua.syt0r.kanji.presentation.screen.main.features.StatisticsController
import ua.syt0r.kanji.presentation.screen.main.screen.statistics.StatisticsScreen
import kotlinx.datetime.Clock

// ============================================
// KAITEYO CARD MANAGER v2.0
// Tags, Flags, Notes, Card Status, Review
// Settings, Keyboard Shortcuts, History, Backup,
// Bulk Operations, Filtered Decks, Statistics,
// Heatmap, Search, Plugins
// ============================================

// Re-export from CardEnhancements for backward compat
typealias KaiteyoCardFlag = CardFlagType

enum class CardDifficulty(val displayName: String, val multiplier: Float) {
    Again("Again", 0.0f),
    Hard("Hard", 0.8f),
    Good("Good", 1.0f),
    Easy("Easy", 1.3f)
}

enum class StudyAction(val displayName: String) {
    ShowAnswer("Show Answer"),
    Again("Again"),
    Hard("Hard"),
    Good("Good"),
    Easy("Easy"),
    Suspend("Suspend"),
    Bury("Bury"),
    Skip("Skip"),
    Preview("Preview"),
    Undo("Undo"),
    Retry("Retry"),
    Flag("Flag"),
    Note("Note"),
    Tag("Tag"),
    Delete("Delete"),
    Edit("Edit"),
    More("More Options")
}

// ============================================
// ENHANCED CARD DATA MODEL
// ============================================

data class KaiteyoCard(
    val id: String = "card_001",
    val character: String = "水",
    val meaning: String = "Water",
    val reading: String = "みず / スイ",
    val deck: String = "N5 Kanji",
    val deckId: Long = 0L,
    val tags: MutableList<CardTag> = mutableListOf(),
    val tagNames: MutableList<String> = mutableListOf("jlpt-n5", "water", "weather"),
    val flag: CardFlagType = CardFlagType.None,
    val notes: String = "",
    val status: CardStatus = CardStatus.New,
    val difficulty: CardDifficulty = CardDifficulty.Good,
    val priority: Int = 0,
    val isSuspended: Boolean = false,
    val isBuried: Boolean = false,
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    val customFields: MutableMap<String, String> = mutableMapOf(),
    val aliases: MutableList<String> = mutableListOf(),
    val relatedCards: MutableList<String> = mutableListOf(),
    val createdAt: String = "2026-01-15",
    val modifiedAt: String = "2026-07-28",
    val lastReviewed: String = "2026-07-28",
    val reviewCount: Int = 47,
    val interval: Int = 21,
    val ease: Float = 2.5f,
    val lapses: Int = 0,
    val accuracy: Float = 0.85f,
    val totalTimeStudied: Long = 0L
)

// ============================================
// SORT STATE
// ============================================

private enum class SortColumn {
    Deck, Card, Due, Ease, Interval, Tags, Flag, Status, Accuracy
}

private data class SortState(
    val column: SortColumn = SortColumn.Deck,
    val ascending: Boolean = true
)

// ============================================
// MAIN CARD MANAGER COMPOSABLE
// ============================================

@Composable
fun CardManager(
    initialCards: List<KaiteyoCard> = listOf(
        KaiteyoCard(),
        KaiteyoCard(id = "card_002", character = "火", meaning = "Fire",
            reading = "ひ / カ", tagNames = mutableListOf("jlpt-n5", "fire"),
            flag = CardFlagType.Red, status = CardStatus.Learning)
    ),
    stats: StatsOverviewV2 = StatsOverviewV2(),
    heatmap: HeatmapDataV2 = HeatmapDataV2()
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var cards by remember { mutableStateOf(initialCards) }
    var selectedTab by remember { mutableStateOf("Browse") }
    var showTagManager by remember { mutableStateOf(false) }
    var showFlagSelector by remember { mutableStateOf<KaiteyoCard?>(null) }
    var showNoteEditor by remember { mutableStateOf<KaiteyoCard?>(null) }
    var showStatusSelector by remember { mutableStateOf<KaiteyoCard?>(null) }
    var showReviewSettings by remember { mutableStateOf(false) }
    var showShortcutSettings by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showHeatmap by remember { mutableStateOf(false) }
    var showBulkActions by remember { mutableStateOf(false) }
    var selectedCards by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sortState by remember { mutableStateOf(SortState(SortColumn.Due, false)) }
    var flagFilter by remember { mutableStateOf<CardFlagType?>(null) }
    var statusFilter by remember { mutableStateOf<CardStatus?>(null) }

    // Filter + sort cards
    val filteredCards = remember(cards, searchQuery, flagFilter, statusFilter, sortState) {
        var result = cards

        // Text filter
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            result = result.filter { card ->
                card.character.lowercase().contains(q) ||
                card.meaning.lowercase().contains(q) ||
                card.reading.lowercase().contains(q) ||
                card.deck.lowercase().contains(q) ||
                card.tagNames.any { it.lowercase().contains(q) } ||
                card.notes.lowercase().contains(q)
            }
        }

        // Flag filter
        flagFilter?.let { flag ->
            result = result.filter { it.flag == flag }
        }

        // Status filter
        statusFilter?.let { status ->
            result = result.filter { it.status == status }
        }

        // Sort
        result = when (sortState.column) {
            SortColumn.Deck -> result.sortedBy { it.deck }
            SortColumn.Card -> result.sortedBy { it.character }
            SortColumn.Due -> result.sortedBy { it.interval }
            SortColumn.Ease -> result.sortedBy { it.ease }
            SortColumn.Interval -> result.sortedBy { it.interval }
            SortColumn.Tags -> result.sortedBy { it.tagNames.firstOrNull() ?: "" }
            SortColumn.Flag -> result.sortedBy { it.flag.ordinal }
            SortColumn.Status -> result.sortedBy { it.status.ordinal }
            SortColumn.Accuracy -> result.sortedBy { it.accuracy }
        }
        if (!sortState.ascending) result = result.reversed()
        result
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        AnkiSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            flagFilter = flagFilter,
            statusFilter = statusFilter,
            onFlagFilterChange = { flagFilter = it },
            onStatusFilterChange = { statusFilter = it },
            onClearFilters = {
                flagFilter = null
                statusFilter = null
                searchQuery = ""
            },
            surfaceColors = surfaceColors,
            accent = accent
        )

        // Top Action Bar
        CardManagerActionBar(
            isSelectionMode = isSelectionMode,
            selectedCount = selectedCards.size,
            onToggleSelectionMode = { isSelectionMode = !isSelectionMode },
            onTagManager = { showTagManager = true },
            onReviewSettings = { showReviewSettings = true },
            onShortcutSettings = { showShortcutSettings = true },
            onHistory = { showHistory = true },
            onHeatmap = { showHeatmap = true },
            onBulkActions = { showBulkActions = true },
            onSelectAll = { selectedCards = filteredCards.map { it.id }.toSet() },
            onDeselectAll = { selectedCards = emptySet() },
            surfaceColors = surfaceColors,
            accent = accent
        )

        // Tab Row: Browse | Stats | Heatmap
        CardManagerTabs(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        Spacer(modifier = Modifier.height(4.dp))

        // Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                "Browse" -> CardBrowserContent(
                    cards = filteredCards,
                    selectedCards = selectedCards,
                    isSelectionMode = isSelectionMode,
                    sortState = sortState,
                    onSortChange = { column ->
                        sortState = if (sortState.column == column) {
                            sortState.copy(ascending = !sortState.ascending)
                        } else {
                            SortState(column, true)
                        }
                    },
                    onToggleSelect = { id ->
                        selectedCards = if (id in selectedCards) selectedCards - id else selectedCards + id
                    },
                    onCardClick = { card ->
                        if (isSelectionMode) {
                            selectedCards = if (card.id in selectedCards) selectedCards - card.id else selectedCards + card.id
                        }
                    },
                    onFlagClick = { showFlagSelector = it },
                    onNoteClick = { showNoteEditor = it },
                    onStatusClick = { showStatusSelector = it },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
                // One unified stats menu everywhere: the analytics dashboard
                // (same screen as the Home Stats tab and the Statistics
                // destination), which includes the study heatmap and the exam
                // system. All numbers come from the real StatisticsController.
                "Stats" -> StatisticsScreen(
                    controller = koinInject<StatisticsController>(),
                    onClose = null
                )
            }
        }

        // Bottom status bar with card count
        AnkiStatusBar(
            visibleCount = filteredCards.size,
            totalCount = cards.size,
            selectedCount = selectedCards.size,
            isSelectionMode = isSelectionMode,
            surfaceColors = surfaceColors
        )
    }

    // Dialogs
    if (showTagManager) {
        TagManagerScreen(
            tags = emptyList(),
            onAddTag = { _, _, _ -> },
            onUpdateTag = { _, _, _, _ -> },
            onDeleteTag = { },
            onMergeTags = { _, _ -> },
            onClose = { showTagManager = false }
        )
    }

    showFlagSelector?.let { card ->
        FlagSelectorDialog(
            currentFlag = card.flag,
            onSelect = { flag ->
                cards = cards.map { if (it.id == card.id) it.copy(flag = flag) else it }
                showFlagSelector = null
            },
            onDismiss = { showFlagSelector = null }
        )
    }

    showNoteEditor?.let { card ->
        NoteEditorDialog(
            initialContent = card.notes,
            onSave = { content ->
                cards = cards.map { if (it.id == card.id) it.copy(notes = content) else it }
                showNoteEditor = null
            },
            onDismiss = { showNoteEditor = null }
        )
    }

    showStatusSelector?.let { card ->
        CardStatusSelectorDialog(
            currentStatus = card.status,
            onSelect = { status ->
                cards = cards.map { if (it.id == card.id) it.copy(status = status, isSuspended = status == CardStatus.Suspended, isBuried = status == CardStatus.Buried, isArchived = status == CardStatus.Archived) else it }
                showStatusSelector = null
            },
            onDismiss = { showStatusSelector = null }
        )
    }

    if (showReviewSettings) {
        ReviewSettingsDialog(
            onDismiss = { showReviewSettings = false },
            onSave = { }
        )
    }

    if (showShortcutSettings) {
        KeyboardShortcutSettingsDialog(
            shortcuts = emptyList(),
            onSave = { },
            onDismiss = { showShortcutSettings = false }
        )
    }

    if (showHistory) {
        StudyHistoryDialog(
            history = emptyList(),
            onDismiss = { showHistory = false }
        )
    }

    if (showHeatmap) {
        HeatmapFullDialog(
            data = cards.groupBy { it.lastReviewed.take(10) }.mapValues { it.value.size },
            onDismiss = { showHeatmap = false }
        )
    }

    if (showBulkActions) {
        BulkActionsDialog(
            selectedCount = selectedCards.size,
            onAction = { action ->
                showBulkActions = false
                isSelectionMode = false
                selectedCards = emptySet()
            },
            onDismiss = {
                showBulkActions = false
                isSelectionMode = false
                selectedCards = emptySet()
            }
        )
    }
}

// ============================================
// ANKI-STYLE SEARCH BAR
// ============================================

@Composable
private fun AnkiSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    flagFilter: CardFlagType?,
    statusFilter: CardStatus?,
    onFlagFilterChange: (CardFlagType?) -> Unit,
    onStatusFilterChange: (CardStatus?) -> Unit,
    onClearFilters: () -> Unit,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
) {
    var showFlagMenu by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }
    val hasFilters = flagFilter != null || statusFilter != null || query.isNotBlank()
    var showFilterPopover by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
            .background(surfaceColors.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Main search row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search icon
            Icon(
                Icons.Default.Search,
                "Search",
                modifier = Modifier.size(20.dp),
                tint = surfaceColors.textMuted
            )

            // Search input
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f).height(48.dp),
                placeholder = {
                    Text(
                        "Search cards... (e.g. tag:jlpt, flag:red, deck:N5)",
                        color = surfaceColors.textMuted.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = surfaceColors.textPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = surfaceColors.border.copy(alpha = 0.3f),
                    cursorColor = accent.primary
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Filter button
            IconButton(
                onClick = { showFilterPopover = !showFilterPopover },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.FilterList,
                    "Filters",
                    tint = if (hasFilters) accent.primary else surfaceColors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Clear button
            if (hasFilters) {
                IconButton(
                    onClick = onClearFilters,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        "Clear",
                        tint = surfaceColors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Active filter chips
        if (hasFilters) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (query.isNotBlank()) {
                    AssistChip(
                        onClick = { onQueryChange("") },
                        label = { Text("Search: \"$query\"", fontSize = 11.sp, maxLines = 1) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, "Remove", Modifier.size(14.dp))
                        },
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
                flagFilter?.let { flag ->
                    AssistChip(
                        onClick = { onFlagFilterChange(null) },
                        label = { Text("Flag: ${flag.displayName}", fontSize = 11.sp) },
                        trailingIcon = {
                            Box(
                                Modifier.size(10.dp)
                                    .clip(CircleShape)
                                    .background(flag.colorFromHex())
                            )
                        },
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
                statusFilter?.let { status ->
                    AssistChip(
                        onClick = { onStatusFilterChange(null) },
                        label = { Text("Status: ${status.displayName}", fontSize = 11.sp) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, "Remove", Modifier.size(14.dp))
                        },
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        }

        // Filter popover
        DropdownMenu(
            expanded = showFilterPopover,
            onDismissRequest = { showFilterPopover = false }
        ) {
            Text(
                "Filter by Flag",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = surfaceColors.textMuted
            )
            CardFlagType.entries.drop(1).forEach { flag ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(12.dp).clip(CircleShape)
                                    .background(flag.colorFromHex())
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(flag.displayName, fontSize = 13.sp)
                        }
                    },
                    onClick = {
                        onFlagFilterChange(if (flagFilter == flag) null else flag)
                        showFilterPopover = false
                    },
                    leadingIcon = {
                        if (flagFilter == flag) {
                            Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        }
                    }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                "Filter by Status",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = surfaceColors.textMuted
            )
            CardStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.displayName, fontSize = 13.sp) },
                    onClick = {
                        onStatusFilterChange(if (statusFilter == status) null else status)
                        showFilterPopover = false
                    },
                    leadingIcon = {
                        if (statusFilter == status) {
                            Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        }
                    }
                )
            }
        }
    }
}

// ============================================
// ANKI STATUS BAR
// ============================================

@Composable
private fun AnkiStatusBar(
    visibleCount: Int,
    totalCount: Int,
    selectedCount: Int,
    isSelectionMode: Boolean,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(surfaceColors.surfaceElevated)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "$visibleCount of $totalCount cards",
            style = MaterialTheme.typography.labelSmall,
            color = surfaceColors.textMuted
        )
        if (isSelectionMode) {
            Text(
                "$selectedCount selected",
                style = MaterialTheme.typography.labelSmall,
                color = surfaceColors.textPrimary
            )
        }
    }
}

// ============================================
// ACTION BAR
// ============================================

@Composable
private fun CardManagerActionBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    onToggleSelectionMode: () -> Unit,
    onTagManager: () -> Unit,
    onReviewSettings: () -> Unit,
    onShortcutSettings: () -> Unit,
    onHistory: () -> Unit,
    onHeatmap: () -> Unit,
    onBulkActions: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Text(
                "$selectedCount selected",
                style = MaterialTheme.typography.titleSmall,
                color = surfaceColors.textPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                SmallActionButton(onClick = onSelectAll, Icons.Default.SelectAll, "Select All", surfaceColors)
                SmallActionButton(onClick = onDeselectAll, Icons.Default.Deselect, "Deselect", surfaceColors)
                SmallActionButton(onClick = onBulkActions, Icons.Default.Build, "Bulk", surfaceColors)
                SmallActionButton(onClick = onToggleSelectionMode, Icons.Default.Close, "Cancel", surfaceColors)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                SmallActionButton(onClick = onTagManager, Icons.Default.Label, "Tags", surfaceColors)
                SmallActionButton(onClick = onHeatmap, Icons.Default.GridOn, "Heatmap", surfaceColors)
                SmallActionButton(onClick = onHistory, Icons.Default.History, "History", surfaceColors)
                SmallActionButton(onClick = onReviewSettings, Icons.Default.Settings, "Settings", surfaceColors)
                SmallActionButton(onClick = onShortcutSettings, Icons.Default.Keyboard, "Shortcuts", surfaceColors)
                SmallActionButton(onClick = onToggleSelectionMode, Icons.Default.Checklist, "Select", surfaceColors)
            }
        }
    }
}

@Composable
private fun SmallActionButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, label, Modifier.size(16.dp), tint = surfaceColors.textSecondary)
            Text(label, color = surfaceColors.textSecondary, fontSize = 11.sp)
        }
    }
}

// ============================================
// STUDY ACTIONS BAR
// ============================================

@Composable
private fun StudyActionsBar(onAction: (StudyAction) -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColors.surfaceElevated)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val sem = LocalKaiteyoSemanticColors.current
        val primaryActions = listOf(
            StudyAction.Again to sem.reviewAgain,
            StudyAction.Hard to sem.reviewHard,
            StudyAction.Good to accent.primary,
            StudyAction.Easy to sem.reviewEasy
        )
        primaryActions.forEach { (action, color) ->
            Button(
                onClick = { onAction(action) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = color.copy(alpha = 0.15f),
                    contentColor = color
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(action.displayName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
        // More actions dropdown
        Box {
            var expanded by remember { mutableStateOf(false) }
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreHoriz, "More", tint = surfaceColors.textSecondary)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf(
                    StudyAction.Suspend, StudyAction.Bury, StudyAction.Skip,
                    StudyAction.Flag, StudyAction.Note, StudyAction.Tag,
                    StudyAction.Preview, StudyAction.Undo, StudyAction.Retry
                ).forEach { action ->
                    DropdownMenuItem(
                        text = { Text(action.displayName) },
                        onClick = { expanded = false; onAction(action) },
                        leadingIcon = {
                            Icon(
                                when (action) {
                                    StudyAction.Suspend -> Icons.Default.Block
                                    StudyAction.Bury -> Icons.Default.VisibilityOff
                                    StudyAction.Skip -> Icons.Default.SkipNext
                                    StudyAction.Flag -> Icons.Default.Flag
                                    StudyAction.Note -> Icons.Default.Note
                                    StudyAction.Tag -> Icons.Default.Label
                                    StudyAction.Preview -> Icons.Default.Preview
                                    StudyAction.Undo -> Icons.Default.Undo
                                    StudyAction.Retry -> Icons.Default.Refresh
                                    else -> Icons.Default.MoreHoriz
                                },
                                null, Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

// ============================================
// TABS
// ============================================

@Composable
private fun CardManagerTabs(selectedTab: String, onTabSelected: (String) -> Unit) {
    val accent = LocalKaiteyoAccent.current
    val tabs = listOf(
        "Browse" to Icons.Default.List,
        "Stats" to Icons.Default.BarChart
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tabs.forEach { (tab, icon) ->
            FilterChip(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                label = { Text(tab, fontSize = 12.sp) },
                leadingIcon = { Icon(icon, null, Modifier.size(16.dp)) },
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

// ============================================
// CARD BROWSER CONTENT (Anki-style columns)
// ============================================

@Composable
private fun CardBrowserContent(
    cards: List<KaiteyoCard>,
    selectedCards: Set<String>,
    isSelectionMode: Boolean,
    sortState: SortState,
    onSortChange: (SortColumn) -> Unit,
    onToggleSelect: (String) -> Unit,
    onCardClick: (KaiteyoCard) -> Unit,
    onFlagClick: (KaiteyoCard) -> Unit,
    onNoteClick: (KaiteyoCard) -> Unit,
    onStatusClick: (KaiteyoCard) -> Unit,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    if (cards.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.SearchOff,
                    null,
                    Modifier.size(48.dp),
                    tint = surfaceColors.textMuted.copy(alpha = 0.4f)
                )
                Spacer(Modifier.height(8.dp))
                Text("No cards found", color = surfaceColors.textMuted, fontSize = 14.sp)
                Text("Try adjusting your search or filters", color = surfaceColors.textMuted.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Column headers
        item {
            AnkiColumnHeaders(
                sortState = sortState,
                onSortChange = onSortChange,
                isSelectionMode = isSelectionMode,
                surfaceColors = surfaceColors,
                accent = accent
            )
        }

        // Card rows
        items(cards, key = { it.id }) { card ->
            AnkiCardRow(
                card = card,
                isSelected = card.id in selectedCards,
                isSelectionMode = isSelectionMode,
                onToggleSelect = { onToggleSelect(card.id) },
                onClick = { onCardClick(card) },
                onFlagClick = { onFlagClick(card) },
                onNoteClick = { onNoteClick(card) },
                onStatusClick = { onStatusClick(card) },
                accent = accent,
                surfaceColors = surfaceColors
            )
        }

        // Bottom spacing
        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ============================================
// ANKI COLUMN HEADERS
// ============================================

@Composable
private fun AnkiColumnHeaders(
    sortState: SortState,
    onSortChange: (SortColumn) -> Unit,
    isSelectionMode: Boolean,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
) {
    val columns = listOf(
        "Deck" to SortColumn.Deck,
        "Card" to SortColumn.Card,
        "Due" to SortColumn.Due,
        "Ease" to SortColumn.Ease,
        "Int" to SortColumn.Interval,
        "Tags" to SortColumn.Tags,
        "Flag" to SortColumn.Flag,
        "Status" to SortColumn.Status
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColors.surfaceElevated.copy(alpha = 0.5f))
            .padding(horizontal = if (isSelectionMode) 4.dp else 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        columns.forEachIndexed { index, (label, column) ->
            val isSorted = sortState.column == column
            val width = when (column) {
                SortColumn.Deck -> 0.18f
                SortColumn.Card -> 0.22f
                SortColumn.Due -> 0.08f
                SortColumn.Ease -> 0.08f
                SortColumn.Interval -> 0.06f
                SortColumn.Tags -> 0.18f
                SortColumn.Flag -> 0.08f
                SortColumn.Status -> 0.12f
                SortColumn.Accuracy -> 0.08f
                SortColumn.Status -> 0.12f
            }

            Row(
                modifier = Modifier
                    .weight(width)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onSortChange(column) }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    color = if (isSorted) accent.primary else surfaceColors.textMuted,
                    fontSize = 10.sp,
                    fontWeight = if (isSorted) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
                if (isSorted) {
                    Icon(
                        if (sortState.ascending) Icons.Default.ArrowUpward
                        else Icons.Default.ArrowDownward,
                        null,
                        Modifier.size(12.dp),
                        tint = accent.primary
                    )
                }
            }
        }
    }
    HorizontalDivider(
        color = surfaceColors.border.copy(alpha = 0.2f),
        thickness = 0.5.dp
    )
}

// ============================================
// ANKI-STYLE CARD ROW
// ============================================

@Composable
private fun AnkiCardRow(
    card: KaiteyoCard,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit,
    onFlagClick: () -> Unit,
    onNoteClick: () -> Unit,
    onStatusClick: () -> Unit,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    val bgColor = when {
        isSelected -> accent.primary.copy(alpha = 0.06f)
        card.isSuspended -> LocalKaiteyoSemanticColors.current.error.copy(alpha = 0.04f)
        card.isBuried -> LocalKaiteyoSemanticColors.current.cardSuspended.copy(alpha = 0.04f)
        else -> Color.Transparent
    }
    val flagColor = if (card.flag != CardFlagType.None) {
        card.flag.colorFromHex().copy(alpha = 0.6f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .then(
                if (flagColor != Color.Transparent) Modifier.drawBehind {
                    drawRect(
                        color = flagColor,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                    )
                } else Modifier
            )
            .clickable {
                if (isSelectionMode) onToggleSelect()
                else onClick()
            }
            .padding(horizontal = if (isSelectionMode) 4.dp else 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Deck
        Text(
            card.deck,
            modifier = Modifier.weight(0.18f),
            color = surfaceColors.textSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Card (character + meaning)
        Column(modifier = Modifier.weight(0.22f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    card.character,
                    color = surfaceColors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (card.notes.isNotBlank()) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Note,
                        "Has note",
                        Modifier.size(12.dp),
                        tint = surfaceColors.textMuted.copy(alpha = 0.5f)
                    )
                }
            }
            Text(
                card.meaning,
                color = surfaceColors.textSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Due (interval)
        Text(
            if (card.interval > 0) "${card.interval}d" else "New",
            modifier = Modifier.weight(0.08f),
            color = if (card.interval == 0) LocalKaiteyoSemanticColors.current.info else surfaceColors.textSecondary,
            fontSize = 12.sp,
            fontWeight = if (card.interval == 0) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1
        )

        // Ease
        Text(
            "${(card.ease * 100).toInt()}%",
            modifier = Modifier.weight(0.08f),
            color = when {
                card.ease >= 2.5f -> LocalKaiteyoSemanticColors.current.success
                card.ease >= 1.5f -> LocalKaiteyoSemanticColors.current.warning
                else -> LocalKaiteyoSemanticColors.current.error
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )

        // Interval (short)
        Text(
            "${card.interval}d",
            modifier = Modifier.weight(0.06f),
            color = surfaceColors.textMuted,
            fontSize = 11.sp,
            maxLines = 1
        )

        // Tags
        Row(
            modifier = Modifier.weight(0.18f),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            card.tagNames.take(2).forEach { tag ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(accent.primary.copy(alpha = 0.08f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        tag,
                        color = accent.primary.copy(alpha = 0.6f),
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (card.tagNames.size > 2) {
                Text(
                    "+${card.tagNames.size - 2}",
                    color = surfaceColors.textMuted,
                    fontSize = 8.sp
                )
            }
        }

        // Flag indicator
        Box(
            modifier = Modifier.weight(0.08f),
            contentAlignment = Alignment.Center
        ) {
            if (card.flag != CardFlagType.None) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(card.flag.colorFromHex())
                        .clickable { onFlagClick() }
                )
            }
        }

        // Status
        Box(modifier = Modifier.weight(0.12f), contentAlignment = Alignment.Center) {
            StatusBadge(status = card.status)
        }
    }
    HorizontalDivider(
        color = surfaceColors.border.copy(alpha = 0.08f),
        thickness = 0.5.dp
    )
}

// ============================================
// STATUS BADGE
// ============================================

@Composable
fun StatusBadge(status: CardStatus) {
    val sem = LocalKaiteyoSemanticColors.current
    val bgColor = when (status) {
        CardStatus.New -> sem.cardNew
        CardStatus.Learning -> sem.cardLearning
        CardStatus.Young -> sem.cardYoung
        CardStatus.Mature -> sem.cardMature
        CardStatus.Relearning -> sem.cardRelearning
        CardStatus.Suspended -> sem.cardSuspended
        CardStatus.Buried -> sem.cardBuried
        CardStatus.Archived -> sem.cardArchived
    }
    val textColor = Color(0xFF1A1A1A)
    Box(
        modifier = Modifier.clip(RoundedCornerShape(4.dp))
            .background(bgColor.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(status.displayName, color = textColor, fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}

// ============================================
// FLAG SELECTOR DIALOG
// ============================================

@Composable
fun FlagSelectorDialog(
    currentFlag: CardFlagType,
    onSelect: (CardFlagType) -> Unit,
    onDismiss: () -> Unit
) {
    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Flag") },
        text = {
            Column {
                CardFlagType.entries.filter { it != CardFlagType.None }.forEach { flag ->
                    val isSelected = flag == currentFlag
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                            .clickable { onSelect(flag) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(20.dp).clip(CircleShape)
                                .background(flag.colorFromHex())
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(flag.displayName, style = MaterialTheme.typography.bodyMedium)
                        if (isSelected) {
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { onSelect(CardFlagType.None) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Remove Flag", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

// ============================================
// NOTE EDITOR DIALOG
// ============================================

@Composable
fun NoteEditorDialog(
    initialContent: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }
    var useMarkdown by remember { mutableStateOf(false) }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Card Note") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Markdown", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = useMarkdown, onCheckedChange = { useMarkdown = it })
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    placeholder = { Text("Write your note here...\n\nMarkdown supported: **bold**, *italic*, `code`, [links](url)") },
                    maxLines = 20
                )
                if (useMarkdown && content.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Preview:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    MarkdownPreview(content = content)
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(content) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ============================================
// MARKDOWN PREVIEW (Simplified)
// ============================================

@Composable
fun MarkdownPreview(content: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        content.split("\n").forEach { line ->
            when {
                line.startsWith("### ") -> Text(line.removePrefix("### "),
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                line.startsWith("## ") -> Text(line.removePrefix("## "),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                line.startsWith("# ") -> Text(line.removePrefix("# "),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                line.startsWith("- ") -> Text("• ${line.removePrefix("- ")}",
                    style = MaterialTheme.typography.bodySmall)
                line.startsWith("> ") -> Text(line.removePrefix("> "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                line.startsWith("```") -> { /* code block start/end */ }
                line.isBlank() -> Spacer(Modifier.height(8.dp))
                else -> {
                    // Simple inline formatting
                    val processed = line
                        .replace("**", "*")
                        .replace("__", "*")
                    Text(processed, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// ============================================
// CARD STATUS SELECTOR
// ============================================

@Composable
fun CardStatusSelectorDialog(
    currentStatus: CardStatus,
    onSelect: (CardStatus) -> Unit,
    onDismiss: () -> Unit
) {
    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Card Status") },
        text = {
            LazyColumn(Modifier.height(400.dp)) {
                items(CardStatus.entries) { status ->
                    val isSelected = status == currentStatus
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                            .clickable { onSelect(status) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge(status = status)
                        Spacer(Modifier.width(12.dp))
                        Text(status.displayName, style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        if (isSelected) {
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

// ============================================
// REVIEW SETTINGS DIALOG
// ============================================

@Composable
fun ReviewSettingsDialog(
    onSave: (ReviewSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var settings by remember { mutableStateOf(ReviewSettings()) }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Settings") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Button visibility
                Text("Answer Buttons", style = MaterialTheme.typography.titleSmall)
                listOf("Again" to settings.showAgain, "Hard" to settings.showHard,
                    "Good" to settings.showGood, "Easy" to settings.showEasy).forEach { (label, value) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = value, onCheckedChange = {
                            settings = when (label) {
                                "Again" -> settings.copy(showAgain = it)
                                "Hard" -> settings.copy(showHard = it)
                                "Good" -> settings.copy(showGood = it)
                                "Easy" -> settings.copy(showEasy = it)
                                else -> settings
                            }
                        })
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }

                HorizontalDivider()

                // Layout
                Text("Button Layout", style = MaterialTheme.typography.titleSmall)
                ButtonLayout.entries.forEach { layout ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = settings.buttonLayout == layout,
                            onClick = { settings = settings.copy(buttonLayout = layout) })
                        Spacer(Modifier.width(8.dp))
                        Text(layout.displayName)
                    }
                }

                HorizontalDivider()

                // Other options
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = settings.autoNext, onCheckedChange = { settings = settings.copy(autoNext = it) })
                    Spacer(Modifier.width(8.dp))
                    Text("Auto-next after answer")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = settings.showTimer, onCheckedChange = { settings = settings.copy(showTimer = it) })
                    Spacer(Modifier.width(8.dp))
                    Text("Show answer timer")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = settings.showProgress, onCheckedChange = { settings = settings.copy(showProgress = it) })
                    Spacer(Modifier.width(8.dp))
                    Text("Show progress")
                }

                // Button size
                Text("Button Size", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ButtonSize.entries.forEach { size ->
                        FilterChip(
                            selected = settings.buttonSize == size,
                            onClick = { settings = settings.copy(buttonSize = size) },
                            label = { Text(size.displayName) }
                        )
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(settings); onDismiss() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ============================================
// KEYBOARD SHORTCUT SETTINGS
// ============================================

@Composable
fun KeyboardShortcutSettingsDialog(
    shortcuts: List<KeyboardShortcut>,
    onSave: (List<KeyboardShortcut>) -> Unit,
    onDismiss: () -> Unit
) {
    var localShortcuts by remember { mutableStateOf(shortcuts.toMutableList()) }
    var recordingAction by remember { mutableStateOf<String?>(null) }

    val defaultShortcuts = listOf(
        "show-answer" to "Space",
        "again" to "1",
        "hard" to "2",
        "good" to "3",
        "easy" to "4",
        "undo" to "Z",
        "suspend" to "@",
        "bury" to "-",
        "tag" to "T",
        "flag" to "F",
        "note" to "N",
        "search" to "/",
        "delete" to "Delete",
        "preview" to "P",
        "retry" to "R",
        "skip" to "S",
        "stats" to "I",
        "heatmap" to "H",
        "history" to "Y",
        "bulk-tag" to "Shift+T",
        "bulk-flag" to "Shift+F",
        "select-all" to "Ctrl+A",
        "deselect" to "Escape"
    )

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Keyboard Shortcuts") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Click a shortcut to record a new key combination.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))

                defaultShortcuts.forEach { (actionId, defaultKey) ->
                    val existing = localShortcuts.find { it.actionId == actionId }
                    val displayKey = existing?.getDisplayText() ?: defaultKey

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .clickable { recordingAction = actionId }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(actionId.replace("-", " ").replaceFirstChar { it.uppercase() },
                            modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        if (recordingAction == actionId) {
                            Text("Press key...", color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text(displayKey, color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(localShortcuts); onDismiss() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ============================================
// STUDY HISTORY DIALOG
// ============================================

@Composable
fun StudyHistoryDialog(
    history: List<StudyHistoryEntry>,
    onDismiss: () -> Unit
) {
    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Study History") },
        text = {
            if (history.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, null, Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(8.dp))
                        Text("No history yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(Modifier.height(400.dp)) {
                    items(history) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (entry.actionType) {
                                    StudyActionType.Review -> Icons.Default.CheckCircle
                                    StudyActionType.Suspend -> Icons.Default.Block
                                    StudyActionType.Bury -> Icons.Default.VisibilityOff
                                    StudyActionType.Flag -> Icons.Default.Flag
                                    StudyActionType.Tag -> Icons.Default.Label
                                    StudyActionType.Edit -> Icons.Default.Edit
                                    StudyActionType.Delete -> Icons.Default.Delete
                                    StudyActionType.Import -> Icons.Default.FileUpload
                                    StudyActionType.Export -> Icons.Default.FileDownload
                                    StudyActionType.Backup -> Icons.Default.Backup
                                    StudyActionType.BulkOperation -> Icons.Default.Build
                                    else -> Icons.Default.Info
                                },
                                null, Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry.actionType.displayName, style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium)
                                if (entry.details.isNotBlank()) {
                                    Text(entry.details, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Text(entry.timestamp.toString().take(19).replace("T", " "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

// ============================================
// HEATMAP VIEW
// ============================================

@Composable
fun HeatmapView(
    data: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val days = listOf("Mon", "", "Wed", "", "Fri", "", "Sun")
    val today = Clock.System.now().toString().take(10)
    val yearStart = today.take(5) + "01-01"

    Column(modifier = modifier) {
        Text("Contribution Heatmap", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))

        // Simplified heatmap grid
        val weeks = 26 // Show half year
        val cellSize = 12.dp
        val gap = 3.dp

        Box(modifier = Modifier.fillMaxWidth().height(cellSize * 7 + gap * 6)) {
            // Day labels
            Column(Modifier.align(Alignment.TopStart), verticalArrangement = Arrangement.spacedBy(gap)) {
                days.forEach { Text(it, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.height(cellSize)) }
            }

            // Grid
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(start = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                (0 until weeks).forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        (0 until 7).forEach { day ->
                            val date = calculateDate(week, day, yearStart)
                            val count = data[date] ?: 0
                            val intensity = when {
                                count >= 10 -> 0.9f
                                count >= 5 -> 0.6f
                                count >= 1 -> 0.3f
                                else -> 0.05f
                            }
                            val isToday = date == today
                            Box(
                                modifier = Modifier.size(cellSize)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = intensity))
                                    .then(if (isToday) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)) else Modifier)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Less", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(0.05f, 0.3f, 0.6f, 0.9f).forEach { alpha ->
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)))
            }
            Text("More", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun calculateDate(week: Int, day: Int, yearStart: String): String {
    // Simplified date calculation
    val (year, month, dayOfMonth) = yearStart.split("-").map { it.toInt() }
    val startDay = java.time.LocalDate.of(year, month, dayOfMonth)
    val result = startDay.plusDays((week * 7 + day).toLong())
    return result.toString()
}

@Composable
fun HeatmapFullDialog(
    data: Map<String, Int>,
    onDismiss: () -> Unit
) {
    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Study Heatmap") },
        text = {
            Column(Modifier.height(400.dp).verticalScroll(rememberScrollState())) {
                HeatmapView(data = data, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Text("Year Overview", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                // Full year heatmap
                HeatmapView(data = data, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

// ============================================
// SEARCH DIALOG
// ============================================

@Composable
fun SearchDialog(
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var searchMode by remember { mutableStateOf("All Fields") }

    val searchModes = listOf("All Fields", "Kanji", "Reading", "Meaning", "Tags", "Notes", "Deck")

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search Cards") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        if (it.length >= 2) onSearch(it)
                    },
                    placeholder = { Text("Search cards...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Search mode chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    searchModes.take(4).forEach { mode ->
                        FilterChip(
                            selected = searchMode == mode,
                            onClick = { searchMode = mode },
                            label = { Text(mode, fontSize = 11.sp) }
                        )
                    }
                }

                // Search tips
                Text("Tips: Use quotes for exact match, - to exclude, tag: to filter by tag",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Results placeholder
                if (query.length >= 2) {
                    Text("Results will appear here", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

// ============================================
// BULK ACTIONS DIALOG
// ============================================

@Composable
fun BulkActionsDialog(
    selectedCount: Int,
    onAction: (BulkActionType) -> Unit,
    onDismiss: () -> Unit
) {
    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Actions ($selectedCount cards)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BulkActionType.entries.forEach { action ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onAction(action) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            when (action) {
                                BulkActionType.Tag -> Icons.Default.Label
                                BulkActionType.Flag -> Icons.Default.Flag
                                BulkActionType.Delete -> Icons.Default.Delete
                                BulkActionType.Move -> Icons.Default.DriveFileMove
                                BulkActionType.Suspend -> Icons.Default.Block
                                BulkActionType.Bury -> Icons.Default.VisibilityOff
                                BulkActionType.Archive -> Icons.Default.Archive
                                BulkActionType.Export -> Icons.Default.FileDownload
                                BulkActionType.Reschedule -> Icons.Default.Schedule
                                BulkActionType.ChangeDeck -> Icons.Default.Folder
                            },
                            null, Modifier.size(20.dp),
                            tint = when (action) {
                                BulkActionType.Delete -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(action.displayName, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ============================================
// IMPORT / EXPORT DIALOG (placeholder)
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    onExport: (String) -> Unit
) {
    var selectedFormat by remember { mutableStateOf("APKG") }
    val formats = listOf("APKG", "CSV", "JSON", "TXT", "Markdown")

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import / Export") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Format", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    formats.forEach { format ->
                        FilterChip(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format },
                            label = { Text(format) }
                        )
                    }
                }
                HorizontalDivider()
                Button(onClick = { onImport(selectedFormat) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.FileUpload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Import $selectedFormat")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onExport(selectedFormat) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.FileDownload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export $selectedFormat")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}