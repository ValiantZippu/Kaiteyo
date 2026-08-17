package ua.syt0r.kanji.desktop.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.BrowserViewMode
import ua.syt0r.kanji.desktop.appstate.WorkspaceView
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsChip
import ua.syt0r.kanji.desktop.designsystem.DsConfirmDialog
import ua.syt0r.kanji.desktop.designsystem.DsContextMenuHost
import ua.syt0r.kanji.desktop.designsystem.DsDialog
import ua.syt0r.kanji.desktop.designsystem.DsEmptyState
import ua.syt0r.kanji.desktop.designsystem.DsFavoriteToggle
import ua.syt0r.kanji.desktop.designsystem.DsFlagBadge
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsMenuItem
import ua.syt0r.kanji.desktop.designsystem.DsPromptDialog
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSearchField
import ua.syt0r.kanji.desktop.designsystem.DsSelect
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsSplitPane
import ua.syt0r.kanji.desktop.designsystem.DsTagChip
import ua.syt0r.kanji.desktop.designsystem.DsTextField
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.parseHexColor
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import kotlinx.datetime.toLocalDateTime
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.review.ReviewFilterPreset
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.SrsStatus
import ua.syt0r.kanji.desktop.model.ToastKind
import ua.syt0r.kanji.desktop.ui.library.DeckPickerDialog

// ============================================
// BROWSER
// Full card browser: search, filters, saved
// searches, grid/list/detail modes, preview
// panel, tagging, flags, favorites, deletion.
// ============================================

private val flagColors = mapOf(
    "red" to "#FF6B6B",
    "orange" to "#FEAB57",
    "yellow" to "#FFD93D",
    "green" to "#C2FC8B",
    "blue" to "#7BC8FF",
    "purple" to "#A78BFA"
)

@Composable
fun BrowserView(state: AppState) {
    var activePreset by remember { mutableStateOf<ReviewFilterPreset?>(null) }
    var activeTag by remember { mutableStateOf<String?>(null) }
    var activeFlag by remember { mutableStateOf<String?>(null) }
    var tagDialogCard by remember { mutableStateOf<DesktopCard?>(null) }
    var flagDialogCard by remember { mutableStateOf<DesktopCard?>(null) }
    var sortMode by remember { mutableStateOf(SortMode.Default) }
    var selectionMode by remember { mutableStateOf(false) }
    var bulkTagDialog by remember { mutableStateOf(false) }
    var bulkFlagDialog by remember { mutableStateOf(false) }
    var bulkDeleteConfirm by remember { mutableStateOf(false) }
    var activeStateFilter by remember { mutableStateOf<String?>(null) }
    var pendingAddToDeck by remember { mutableStateOf<List<String>?>(null) }
    var batchEditDialog by remember { mutableStateOf(false) }
    // Keyboard navigation over the result list: ↑/↓ move, Enter opens,
    // Esc clears. Arrows are ignored while the search field is focused so
    // caret movement inside the field keeps working.
    var focusedIndex by remember { mutableStateOf(-1) }
    var searchFocused by remember { mutableStateOf(false) }
    // Live suggestions under the search field (recent searches, tags, flags,
    // query operators). Keyboard: arrows move, Enter applies, Esc dismisses.
    var suggestionsVisible by remember { mutableStateOf(false) }
    var suggestionIndex by remember { mutableStateOf(0) }
    var searchFieldPos by remember { mutableStateOf<IntOffset?>(null) }
    var searchFieldHeight by remember { mutableStateOf(0) }
    // Query that was just applied from the dropdown — typing it back into the
    // field must not instantly re-open the dropdown.
    var lastAppliedQuery by remember { mutableStateOf<String?>(null) }

    val selectionActive = selectionMode || state.selectedCardIds.isNotEmpty()

    val query = buildString {
        append(state.browserQuery.trim())
        if (activePreset != null && activePreset!!.query.isNotBlank()) {
            if (isNotBlank()) append(" ")
            append(activePreset!!.query)
        }
        if (activeTag != null) {
            if (isNotBlank()) append(" ")
            append("tag:").append(activeTag)
        }
        if (activeFlag != null) {
            if (isNotBlank()) append(" ")
            append("flag:").append(activeFlag)
        }
        if (activeStateFilter != null) {
            if (isNotBlank()) append(" ")
            append("status:").append(activeStateFilter)
        }
    }
    val results = sortCards(state.searchCards(query), sortMode)

    // Typing or a filter/sort change resets keyboard focus so navigation
    // always starts from the top of the freshly computed list.
    LaunchedEffect(query, results.size) { focusedIndex = -1 }

    // Suggestions recompute from the raw query and the card pool; any query
    // change re-shows them (unless the user explicitly dismissed with Esc).
    val suggestions = remember(state.browserQuery, state.cards) { buildBrowseSuggestions(state, state.browserQuery) }
    LaunchedEffect(state.browserQuery) {
        suggestionIndex = 0
        if (state.browserQuery != lastAppliedQuery) suggestionsVisible = true
    }
    val showSuggestions = suggestionsVisible && searchFocused && suggestions.isNotEmpty()
    val applySuggestion: (BrowseSuggestion) -> Unit = { suggestion ->
        lastAppliedQuery = suggestion.query
        state.browserQuery = suggestion.query
        suggestionsVisible = false
    }

    // Mouse clicks sync the keyboard focus so arrows continue from the
    // row the user just clicked.
    val syncFocus: (DesktopCard) -> Unit = { card ->
        focusedIndex = results.indexOfFirst { it.id == card.id }
    }
    val openCard: (DesktopCard) -> Unit = { card ->
        syncFocus(card)
        handleCardClick(state, card, selectionMode)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> {
                        when {
                            results.isEmpty() -> false
                            searchFocused && suggestions.isNotEmpty() -> {
                                suggestionIndex = (suggestionIndex + 1) % suggestions.size
                                true
                            }
                            searchFocused -> false
                            else -> {
                                focusedIndex = if (focusedIndex < 0) 0 else (focusedIndex + 1).coerceAtMost(results.lastIndex)
                                true
                            }
                        }
                    }
                    Key.DirectionUp -> {
                        when {
                            results.isEmpty() -> false
                            searchFocused && suggestions.isNotEmpty() -> {
                                suggestionIndex = (suggestionIndex - 1 + suggestions.size) % suggestions.size
                                true
                            }
                            searchFocused -> false
                            else -> {
                                focusedIndex = if (focusedIndex <= 0) 0 else focusedIndex - 1
                                true
                            }
                        }
                    }
                    Key.Enter -> {
                        when {
                            searchFocused && suggestions.isNotEmpty() -> {
                                applySuggestion(suggestions[suggestionIndex])
                                true
                            }
                            searchFocused && results.isNotEmpty() -> {
                                openCard(results[0])
                                true
                            }
                            focusedIndex in results.indices -> {
                                openCard(results[focusedIndex])
                                true
                            }
                            else -> false
                        }
                    }
                    Key.Escape -> {
                        if (searchFocused) {
                            when {
                                showSuggestions -> { suggestionsVisible = false; true }
                                state.browserQuery.isNotBlank() -> { state.browserQuery = ""; true }
                                else -> false
                            }
                        } else when {
                            selectionActive -> {
                                state.selectedCardIds.clear()
                                selectionMode = false
                                true
                            }
                            activePreset != null || activeTag != null || activeFlag != null || activeStateFilter != null -> {
                                activePreset = null
                                activeTag = null
                                activeFlag = null
                                activeStateFilter = null
                                true
                            }
                            state.selectedCard != null -> { state.selectedCard = null; true }
                            else -> false
                        }.also { focusedIndex = -1 }
                    }
                    else -> false
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInWindow()
                        searchFieldPos = IntOffset(pos.x.roundToInt(), pos.y.roundToInt())
                        searchFieldHeight = coords.size.height
                    }
                    .onFocusChanged { searchFocused = it.isFocused }
            ) {
                DsSearchField(
                    value = state.browserQuery,
                    onValueChange = { state.browserQuery = it },
                    placeholder = "水 meaning:water on:スイ tag:jlpt-4 flag:red interval:>=21 …",
                    modifier = Modifier.fillMaxWidth()
                )
                if (showSuggestions) {
                    val pos = searchFieldPos
                    if (pos != null) {
                        Popup(
                            onDismissRequest = {},
                            offset = IntOffset(pos.x, pos.y + searchFieldHeight + 6),
                            properties = PopupProperties(focusable = false)
                        ) {
                            SuggestionDropdown(
                                suggestions = suggestions,
                                selectedIndex = suggestionIndex,
                                onSelect = applySuggestion,
                                onHover = { suggestionIndex = it }
                            )
                        }
                    }
                }
            }
            DsSelect(
                selected = activePreset ?: ReviewFilterPreset.All,
                options = ReviewFilterPreset.entries.toList(),
                onSelected = { activePreset = if (it == ReviewFilterPreset.All) null else it },
                labelOf = { it.label },
                modifier = Modifier.width(170.dp)
            )
            DsButton(
                text = "Save",
                icon = Icons.Default.Save,
                onClick = {
                    if (state.browserQuery.isNotBlank()) {
                        state.filterStore.save(state.browserQuery.trim(), state.browserQuery.trim())
                        state.toastHost.show("Search saved", kind = ToastKind.Success)
                    } else {
                        state.toastHost.show("Type a query to save it first", kind = ToastKind.Info)
                    }
                },
                kind = DsButtonKind.Secondary,
                compact = true
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DsSpacing.Lg),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DsChip(
                text = "All",
                selected = activePreset == null && activeTag == null && activeFlag == null && activeStateFilter == null,
                onClick = { activePreset = null; activeTag = null; activeFlag = null; activeStateFilter = null }
            )
            ReviewFilterPreset.entries.filter { it != ReviewFilterPreset.All }.forEach { preset ->
                DsChip(
                    text = preset.label,
                    selected = activePreset == preset,
                    onClick = { activePreset = if (activePreset == preset) null else preset }
                )
            }
            DsChip(
                text = "Suspended",
                selected = activeStateFilter == "suspended",
                onClick = { activeStateFilter = if (activeStateFilter == "suspended") null else "suspended" }
            )
            DsChip(
                text = "Buried",
                selected = activeStateFilter == "buried",
                onClick = { activeStateFilter = if (activeStateFilter == "buried") null else "buried" }
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${results.size} cards · ↑↓ navigate · Enter open · Esc clear",
                color = surfaceColors().textMuted,
                fontSize = DsType.Caption
            )
        }

        Spacer(Modifier.height(DsSpacing.Sm))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DsSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DsIconButton(
                    icon = Icons.Default.GridView,
                    onClick = { state.browserViewMode = BrowserViewMode.Grid },
                    contentDescription = "Grid",
                    tint = if (state.browserViewMode == BrowserViewMode.Grid) accent().primary else Color.Unspecified
                )
                DsIconButton(
                    icon = Icons.Default.ViewList,
                    onClick = { state.browserViewMode = BrowserViewMode.List },
                    contentDescription = "List",
                    tint = if (state.browserViewMode == BrowserViewMode.List) accent().primary else Color.Unspecified
                )
                DsIconButton(
                    icon = Icons.Default.SelectAll,
                    onClick = {
                        if (state.selectedCardIds.size == results.size) {
                            state.selectedCardIds.clear()
                        } else {
                            state.selectedCardIds.clear()
                            results.forEach { state.selectedCardIds.add(it.id) }
                        }
                    },
                    contentDescription = "Select all",
                    tint = if (state.selectedCardIds.size == results.size && results.isNotEmpty()) accent().primary else Color.Unspecified
                )
                DsIconButton(
                    icon = Icons.Default.CheckBoxOutlineBlank,
                    onClick = { selectionMode = !selectionMode },
                    contentDescription = "Selection mode",
                    tint = if (selectionMode) accent().primary else Color.Unspecified
                )
            }
            Spacer(Modifier.weight(1f))
            DsSelect(
                selected = sortMode,
                options = SortMode.entries.toList(),
                onSelected = { sortMode = it },
                labelOf = { it.label },
                modifier = Modifier.width(150.dp)
            )
            DsButton(
                text = "Review these ${results.size}",
                icon = Icons.Default.PlayArrow,
                onClick = { state.startReview(query = query) },
                compact = true
            )
            DsButton(
                text = "Edit",
                icon = Icons.Default.Edit,
                kind = DsButtonKind.Secondary,
                enabled = state.selectedCard != null,
                onClick = { state.selectedCard?.let { state.openEditor(it) } },
                compact = true
            )
        }

        if (selectionActive) {
            Spacer(Modifier.height(DsSpacing.Sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DsSpacing.Lg)
                    .clip(RoundedCornerShape(DsRadius.Md))
                    .background(surfaceColors().surfaceElevated)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
            ) {
                Text(
                    text = "${state.selectedCardIds.size} selected",
                    color = surfaceColors().textPrimary,
                    fontSize = DsType.Label,
                    fontWeight = FontWeight.SemiBold
                )
                DsButton(
                    text = "Tag",
                    icon = Icons.Default.Label,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = { bulkTagDialog = true }
                )
                DsButton(
                    text = "Flag",
                    icon = Icons.Default.Flag,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = { bulkFlagDialog = true }
                )
                DsButton(
                    text = "Favorite",
                    icon = Icons.Default.Star,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = { bulkSetFavorite(state, state.selectedCardIds.toList(), favorite = true) }
                )
                DsButton(
                    text = "Batch edit…",
                    icon = Icons.Default.Edit,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = { batchEditDialog = true }
                )
                DsButton(
                    text = "Edit",
                    icon = Icons.Default.Edit,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = {
                        state.selectedCardIds.firstOrNull()?.let { id ->
                            state.cards.firstOrNull { it.id == id }?.let { state.openEditor(it) }
                        }
                    }
                )
                DsButton(
                    text = "Suspend",
                    icon = Icons.Default.Refresh,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = { bulkSuspend(state, state.selectedCardIds.toList()) }
                )
                DsButton(
                    text = "Bury",
                    icon = Icons.Default.Block,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = { bulkBury(state, state.selectedCardIds.toList()) }
                )
                DsButton(
                    text = "Add to deck",
                    icon = Icons.Default.Folder,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = { pendingAddToDeck = state.selectedCardIds.toList() }
                )
                DsButton(
                    text = "Reset",
                    icon = Icons.Default.Refresh,
                    kind = DsButtonKind.Ghost,
                    compact = true,
                    onClick = { bulkReset(state, state.selectedCardIds.toList()) }
                )
                DsButton(
                    text = "Delete",
                    icon = Icons.Default.Delete,
                    kind = DsButtonKind.Danger,
                    compact = true,
                    onClick = { bulkDeleteConfirm = true }
                )
                DsButton(
                    text = "Clear",
                    kind = DsButtonKind.Ghost,
                    compact = true,
                    onClick = { state.selectedCardIds.clear() }
                )
            }
        }

        Spacer(Modifier.height(DsSpacing.Sm))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                results.isEmpty() -> {
                    DsEmptyState(
                        title = "No cards match",
                        message = "Try a broader search or clear the filters.",
                        modifier = Modifier.align(Alignment.Center),
                        action = {
                            val filtersActive = state.browserQuery.isNotBlank() ||
                                activePreset != null || activeTag != null || activeFlag != null || activeStateFilter != null
                            if (filtersActive) {
                                DsButton(
                                    text = "Clear search & filters",
                                    kind = DsButtonKind.Secondary,
                                    compact = true,
                                    onClick = {
                                        state.browserQuery = ""
                                        activePreset = null
                                        activeTag = null
                                        activeFlag = null
                                        activeStateFilter = null
                                    }
                                )
                            }
                        }
                    )
                }
                state.browserViewMode == BrowserViewMode.Details && state.browserShowPreview -> {
                    DsSplitPane(
                        vertical = false,
                        initialFraction = 0.58f,
                        modifier = Modifier.fillMaxSize(),
                        first = {
                            BrowserList(
                                state = state,
                                cards = results,
                                selectionMode = selectionMode,
                                focusedCardId = focusedIndex.takeIf { it in results.indices }?.let { results[it].id },
                                onSelect = openCard,
                                onRequestAddTag = { tagDialogCard = it },
                                onRequestAddFlag = { flagDialogCard = it },
                                onRequestAddToDeck = { pendingAddToDeck = listOf(it.id) }
                            )
                        },
                        second = {
                            CardDetailPanel(state, state.selectedCard, onRequestAddTag = { tagDialogCard = it })
                        }
                    )
                }
                else -> {
                    when (state.browserViewMode) {
                        BrowserViewMode.Grid -> BrowserGrid(
                            state = state,
                            cards = results,
                            selectionMode = selectionMode,
                            focusedCardId = focusedIndex.takeIf { it in results.indices }?.let { results[it].id },
                            onFocusCard = syncFocus,
                            onRequestAddTag = { tagDialogCard = it },
                            onRequestAddFlag = { flagDialogCard = it },
                            onRequestAddToDeck = { pendingAddToDeck = listOf(it.id) }
                        )
                        BrowserViewMode.List -> BrowserList(
                            state = state,
                            cards = results,
                            selectionMode = selectionMode,
                            focusedCardId = focusedIndex.takeIf { it in results.indices }?.let { results[it].id },
                            onSelect = openCard,
                            onRequestAddTag = { tagDialogCard = it },
                            onRequestAddFlag = { flagDialogCard = it },
                            onRequestAddToDeck = { pendingAddToDeck = listOf(it.id) }
                        )
                        BrowserViewMode.Details -> BrowserList(
                            state = state,
                            cards = results,
                            selectionMode = selectionMode,
                            focusedCardId = focusedIndex.takeIf { it in results.indices }?.let { results[it].id },
                            onSelect = openCard,
                            onRequestAddTag = { tagDialogCard = it },
                            onRequestAddFlag = { flagDialogCard = it },
                            onRequestAddToDeck = { pendingAddToDeck = listOf(it.id) }
                        )
                    }
                }
            }
        }
    }

    tagDialogCard?.let { card ->
        DsPromptDialog(
            title = "Add tag to ${card.character}",
            placeholder = "e.g. food, jlpt-n3, verbs",
            onConfirm = { value -> addTag(state, card, value) },
            onDismiss = { tagDialogCard = null }
        )
    }
    flagDialogCard?.let { card ->
        DsPromptDialog(
            title = "Add flag to ${card.character}",
            placeholder = "red, orange, yellow, green, blue, purple",
            onConfirm = { value -> addFlag(state, card, value.trim().lowercase()) },
            onDismiss = { flagDialogCard = null }
        )
    }
    if (bulkTagDialog) {
        DsPromptDialog(
            title = "Tag ${state.selectedCardIds.size} cards",
            placeholder = "e.g. food, jlpt-n3",
            onConfirm = { value -> bulkAddTag(state, state.selectedCardIds.toList(), value) },
            onDismiss = { bulkTagDialog = false }
        )
    }
    if (bulkFlagDialog) {
        DsPromptDialog(
            title = "Flag ${state.selectedCardIds.size} cards",
            placeholder = "red, orange, yellow, green, blue, purple",
            onConfirm = { value -> bulkAddFlag(state, state.selectedCardIds.toList(), value.trim().lowercase()) },
            onDismiss = { bulkFlagDialog = false }
        )
    }
    if (bulkDeleteConfirm) {
        DsConfirmDialog(
            title = "Delete ${state.selectedCardIds.size} cards?",
            message = "This cannot be undone.",
            confirmText = "Delete",
            danger = true,
            onConfirm = { bulkDelete(state, state.selectedCardIds.toList()) },
            onDismiss = { bulkDeleteConfirm = false }
        )
    }
    if (batchEditDialog) {
        BatchEditDialog(
            state = state,
            count = state.selectedCardIds.size,
            onApply = { status, noteAppend, tagsReplace ->
                bulkEditFields(state, state.selectedCardIds.toList(), status, noteAppend, tagsReplace)
            },
            onDismiss = { batchEditDialog = false }
        )
    }
    pendingAddToDeck?.let { ids ->
        if (ids.isEmpty()) {
            pendingAddToDeck = null
        } else {
            DeckPickerDialog(
                state = state,
                title = "Add ${ids.size} card${if (ids.size == 1) "" else "s"} to deck",
                subtitle = "Choose a deck. Cards keep their own progress — this only changes deck membership.",
                decks = state.library.allDecks(),
                onPick = { deck ->
                    state.library.addCards(deck.id, ids)
                    state.activityLog.record(ActivityCategory.Study, "Added ${ids.size} card(s) to \"${deck.name}\"")
                    state.toastHost.show(
                        "Added ${ids.size} card${if (ids.size == 1) "" else "s"} to \"${deck.name}\"",
                        kind = ToastKind.Success
                    )
                    pendingAddToDeck = null
                },
                onDismiss = { pendingAddToDeck = null }
            )
        }
    }
}

// ============================================
// GRID
// ============================================

@Composable
private fun BrowserGrid(
    state: AppState,
    cards: List<DesktopCard>,
    selectionMode: Boolean,
    onRequestAddTag: (DesktopCard) -> Unit,
    onRequestAddFlag: (DesktopCard) -> Unit,
    onRequestAddToDeck: (DesktopCard) -> Unit = {},
    focusedCardId: String? = null,
    onFocusCard: (DesktopCard) -> Unit = {}
) {
    val gridState = rememberLazyGridState()
    // Keyboard focus scrolls the focused cell into view.
    LaunchedEffect(focusedCardId) {
        val index = cards.indexOfFirst { it.id == focusedCardId }
        if (index >= 0) gridState.animateScrollToItem(index)
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(190.dp),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(DsSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm),
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        itemsIndexed(cards, key = { _, card -> card.id }) { _, card ->
            BrowserCell(
                state = state,
                card = card,
                selectionMode = selectionMode,
                focused = card.id == focusedCardId,
                onRequestAddTag = onRequestAddTag,
                onRequestAddFlag = onRequestAddFlag,
                onRequestAddToDeck = onRequestAddToDeck,
                onClick = {
                    onFocusCard(card)
                    handleCardClick(state, card, selectionMode)
                }
            )
        }
    }
}

@Composable
private fun BrowserCell(
    state: AppState,
    card: DesktopCard,
    selectionMode: Boolean,
    focused: Boolean = false,
    onRequestAddTag: (DesktopCard) -> Unit,
    onRequestAddFlag: (DesktopCard) -> Unit,
    onRequestAddToDeck: (DesktopCard) -> Unit,
    onClick: () -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()
    val selected = card.id in state.selectedCardIds
    val menuItems = cardContextMenu(state, card, onRequestAddTag, onRequestAddFlag, onRequestAddToDeck)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Lg))
            .background(if (selected) ac.primary.copy(alpha = 0.12f) else sc.surface)
            .then(if (focused) Modifier.border(1.5.dp, ac.primary, RoundedCornerShape(DsRadius.Lg)) else Modifier)
    ) {
        DsContextMenuHost(enabled = true, menuItems = menuItems) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .hoverable(remember { MutableInteractionSource() })
                    .clickable(onClick = onClick)
                    .padding(DsSpacing.Md)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = card.character,
                        color = sc.textPrimary,
                        fontSize = DsType.Display,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (selectionMode) {
                        Icon(
                            if (selected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            null,
                            tint = if (selected) ac.primary else sc.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    } else if (card.favorite) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFD93D), modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    text = card.meaning,
                    color = sc.textSecondary,
                    fontSize = DsType.Body,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DsBadge(text = card.status.name, tint = statusColor(card.status))
                    if (card.flags.isNotEmpty()) {
                        DsFlagBadge(label = card.flags.first(), colorHex = flagColors[card.flags.first()] ?: "#808080")
                    }
                }
            }
        }
    }
}

// ============================================
// LIST
// ============================================

@Composable
private fun BrowserList(
    state: AppState,
    cards: List<DesktopCard>,
    selectionMode: Boolean,
    onSelect: (DesktopCard) -> Unit,
    onRequestAddTag: (DesktopCard) -> Unit = {},
    onRequestAddFlag: (DesktopCard) -> Unit = {},
    onRequestAddToDeck: (DesktopCard) -> Unit = {},
    focusedCardId: String? = null
) {
    val sc = surfaceColors()
    val listState = rememberLazyListState()
    // Keyboard focus scrolls the focused row into view.
    LaunchedEffect(focusedCardId) {
        val index = cards.indexOfFirst { it.id == focusedCardId }
        if (index >= 0) listState.animateScrollToItem(index)
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(DsSpacing.Md)
    ) {
        itemsIndexed(cards, key = { _, card -> card.id }) { _, card ->
            DsContextMenuHost(menuItems = cardContextMenu(state, card, onRequestAddTag, onRequestAddFlag, onRequestAddToDeck)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(DsRadius.Md))
                        .background(if (card.id == state.selectedCard?.id || card.id in state.selectedCardIds || card.id == focusedCardId) sc.surfaceInteractive else Color.Transparent)
                        .hoverable(remember { MutableInteractionSource() })
                        .clickable { onSelect(card) }
                        .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectionMode) {
                        Icon(
                            if (card.id in state.selectedCardIds) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            null,
                            tint = if (card.id in state.selectedCardIds) accent().primary else sc.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(DsSpacing.Sm))
                    }
                    Text(
                        text = card.character,
                        color = sc.textPrimary,
                        fontSize = DsType.Heading,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(56.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = card.meaning,
                            color = sc.textPrimary,
                            fontSize = DsType.Body,
                            maxLines = 1
                        )
                        Text(
                            text = buildString {
                                card.onReadings.forEach { append(it).append(" ") }
                                card.kunReadings.take(2).forEach { append(it).append(" ") }
                            }.trim(),
                            color = sc.textMuted,
                            fontSize = DsType.Caption,
                            maxLines = 1
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        DsBadge(text = card.status.name, tint = statusColor(card.status))
                        if (card.flags.isNotEmpty()) {
                            DsFlagBadge(label = card.flags.first(), colorHex = flagColors[card.flags.first()] ?: "#808080")
                        }
                        DsFavoriteToggle(favorite = card.favorite, onToggle = { toggleFavorite(state, card) })
                    }
                }
            }
        }
    }
}

// ============================================
// DETAIL PANEL
// ============================================

@Composable
fun CardDetailPanel(
    state: AppState,
    card: DesktopCard?,
    onRequestAddTag: (DesktopCard) -> Unit = {}
) {
    val sc = surfaceColors()
    val ac = accent()

    if (card == null) {
        DsEmptyState(
            title = "Select a card",
            message = "Click any card in the browser to inspect it here.",
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    var noteText by remember(card.id) { mutableStateOf(card.note) }
    var editNote by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(sc.surfaceElevated)
            .verticalScroll(rememberScrollState())
            .padding(DsSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = card.character,
                    color = sc.textPrimary,
                    fontSize = DsType.Display,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = card.meaning,
                    color = sc.textSecondary,
                    fontSize = DsType.BodyLarge
                )
            }
            DsFavoriteToggle(favorite = card.favorite, onToggle = { toggleFavorite(state, card) })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            DsBadge(text = card.status.name, tint = statusColor(card.status))
            card.flags.forEach { flag ->
                DsBadge(text = "flag: $flag", tint = parseHexColor(flagColors[flag]))
            }
            if (card.favorite) DsBadge(text = "favorite", tint = Color(0xFFFFD93D))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
            Column {
                Text("Onyomi", color = sc.textMuted, fontSize = DsType.Caption)
                Text(card.onReadings.joinToString("、") { it.ifBlank { "—" } }, color = sc.textPrimary, fontSize = DsType.BodyLarge)
            }
            Column {
                Text("Kunyomi", color = sc.textMuted, fontSize = DsType.Caption)
                Text(card.kunReadings.joinToString("、") { it.ifBlank { "—" } }, color = sc.textPrimary, fontSize = DsType.BodyLarge)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
            DetailColumn("Radicals", card.radicals.joinToString(" ").ifBlank { "—" })
            DetailColumn("Strokes", card.strokeCount.toString())
            DetailColumn("JLPT", card.jlpt?.let { "N$it" } ?: "—")
            DetailColumn("Grade", card.grade?.toString() ?: "—")
        }

        DsCard {
            Column(Modifier.padding(DsSpacing.Lg)) {
                Text("SRS State", color = sc.textMuted, fontSize = DsType.Caption)
                Spacer(Modifier.height(DsSpacing.Sm))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat("Interval", "${card.intervalDays}d")
                    Stat("Reps", card.reps.toString())
                    Stat("Lapses", card.lapses.toString())
                    Stat("Ease", "%.2f".format(card.ease))
                    Stat("Accuracy", "${(card.accuracy * 100).toInt()}%")
                    Stat("Due", card.dueAt?.let { "${it.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date}" } ?: "—")
                }
            }
        }

        Text("Tags", color = sc.textMuted, fontSize = DsType.Caption)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            card.tags.forEach { tag ->
                DsTagChip(label = tag, colorHex = "#7BC8FF", removable = true, onRemove = { removeTag(state, card, tag) })
            }
            DsTagChip(label = "+ add tag", colorHex = "#606060", onClick = { onRequestAddTag(card) })
        }

        Spacer(Modifier.height(4.dp))

        Text("Note", color = sc.textMuted, fontSize = DsType.Caption)
        if (editNote) {
            DsTextField(
                value = noteText,
                onValueChange = { noteText = it },
                placeholder = "Your private note…"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsButton(text = "Save note", compact = true, onClick = {
                    val idx = state.cards.indexOfFirst { it.id == card.id }
                    if (idx >= 0) state.cards[idx] = state.cards[idx].copy(note = noteText)
                    editNote = false
                    state.toastHost.show("Note saved", kind = ToastKind.Success)
                })
                DsButton(text = "Cancel", compact = true, kind = DsButtonKind.Ghost, onClick = { editNote = false })
            }
        } else {
            Text(
                text = card.note.ifBlank { "No note yet." },
                color = if (card.note.isBlank()) sc.textMuted else sc.textSecondary,
                fontSize = DsType.Body
            )
            TextButton(onClick = { editNote = true; noteText = card.note }) {
                Text(if (card.note.isBlank()) "Add note" else "Edit note", color = ac.primary)
            }
        }

        Spacer(Modifier.height(DsSpacing.Sm))
        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            DsButton(
                text = "Review this card",
                icon = Icons.Default.PlayArrow,
                onClick = { state.startReview(query = "id:${card.id}") }
            )
            DsButton(
                text = "Look up in dictionary",
                icon = Icons.Default.MenuBook,
                kind = DsButtonKind.Secondary,
                enabled = card.character.isNotBlank(),
                onClick = {
                    state.dictionary.query = card.character
                    state.currentView = WorkspaceView.Dictionary
                }
            )
        }
    }
}

@Composable
private fun DetailColumn(label: String, value: String) {
    val sc = surfaceColors()
    Column {
        Text(label, color = sc.textMuted, fontSize = DsType.Caption)
        Text(value, color = sc.textPrimary, fontSize = DsType.Body)
    }
}

@Composable
private fun Stat(label: String, value: String) {
    val sc = surfaceColors()
    Column {
        Text(label, color = sc.textMuted, fontSize = DsType.Caption)
        Text(value, color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

// ============================================
// CARD ACTIONS
// ============================================

private fun toggleFavorite(state: AppState, card: DesktopCard) {
    val idx = state.cards.indexOfFirst { it.id == card.id }
    if (idx >= 0) {
        state.cards[idx] = state.cards[idx].copy(favorite = !state.cards[idx].favorite)
        state.activityLog.record(
            ActivityCategory.Favorite,
            "${if (state.cards[idx].favorite) "Favorited" else "Unfavorited"} ${card.character}"
        )
    }
}

private fun removeTag(state: AppState, card: DesktopCard, tag: String) {
    val idx = state.cards.indexOfFirst { it.id == card.id }
    if (idx >= 0) {
        state.cards[idx] = state.cards[idx].copy(tags = state.cards[idx].tags.filter { it != tag })
        state.activityLog.record(ActivityCategory.Tag, "Removed tag $tag from ${card.character}")
    }
}

private fun addTag(state: AppState, card: DesktopCard, tag: String) {
    if (tag.isBlank()) return
    val idx = state.cards.indexOfFirst { it.id == card.id }
    if (idx >= 0 && tag !in state.cards[idx].tags) {
        state.cards[idx] = state.cards[idx].copy(tags = state.cards[idx].tags + tag)
        state.activityLog.record(ActivityCategory.Tag, "Added tag $tag to ${card.character}")
    }
}

private fun addFlag(state: AppState, card: DesktopCard, flag: String) {
    if (flag.isBlank()) return
    val idx = state.cards.indexOfFirst { it.id == card.id }
    if (idx >= 0 && flag !in state.cards[idx].flags) {
        state.cards[idx] = state.cards[idx].copy(flags = state.cards[idx].flags + flag)
        state.activityLog.record(ActivityCategory.Flag, "Flagged ${card.character} with $flag")
    }
}

private fun cardContextMenu(
    state: AppState,
    card: DesktopCard,
    onRequestAddTag: (DesktopCard) -> Unit,
    onRequestAddFlag: (DesktopCard) -> Unit,
    onRequestAddToDeck: (DesktopCard) -> Unit = {}
): List<DsMenuItem> = buildList {
    add(DsMenuItem(
        if (card.id in state.selectedCardIds) "Deselect" else "Select",
        Icons.Default.CheckBoxOutlineBlank,
        onAction = {
            if (card.id in state.selectedCardIds) state.selectedCardIds.remove(card.id) else state.selectedCardIds.add(card.id)
        }
    ))
    add(DsMenuItem("Open in details", Icons.Default.Visibility, onAction = {
        state.selectedCard = card
        state.browserViewMode = BrowserViewMode.Details
    }))
    add(DsMenuItem("Review this card", Icons.Default.PlayArrow, onAction = {
        state.startReview(query = "id:${card.id}")
    }))
    add(DsMenuItem("Bury", Icons.Default.Block, onAction = {
        val idx = state.cards.indexOfFirst { it.id == card.id }
        if (idx >= 0) state.cards[idx] = state.cards[idx].copy(status = SrsStatus.Buried, dueAt = null)
        state.toastHost.show("Buried ${card.character}", kind = ToastKind.Info)
    }))
    add(DsMenuItem(if (card.favorite) "Unfavorite" else "Favorite", Icons.Default.Star, onAction = { toggleFavorite(state, card) }))
    add(DsMenuItem("Add tag", Icons.Default.Label, onAction = { onRequestAddTag(card) }))
    add(DsMenuItem("Add flag", Icons.Default.Flag, onAction = { onRequestAddFlag(card) }))
    add(DsMenuItem("Add to deck…", Icons.Default.Folder, onAction = { onRequestAddToDeck(card) }))
    add(DsMenuItem("Suspend", Icons.Default.Refresh, shortcutLabel = "S", onAction = {
        val idx = state.cards.indexOfFirst { it.id == card.id }
        if (idx >= 0) state.cards[idx] = state.cards[idx].copy(status = SrsStatus.Suspended, dueAt = null)
        state.toastHost.show("Suspended ${card.character}", kind = ToastKind.Success)
    }))
    add(DsMenuItem("Reset card", Icons.Default.Refresh, onAction = {
        val idx = state.cards.indexOfFirst { it.id == card.id }
        if (idx >= 0) state.cards[idx] = state.cards[idx].copy(status = SrsStatus.New, intervalDays = 0.0, reps = 0, lapses = 0, dueAt = null)
        state.toastHost.show("Reset ${card.character} to new", kind = ToastKind.Success)
    }))
    add(DsMenuItem("Delete card", Icons.Default.Delete, danger = true, onAction = {
        state.cards.removeAll { it.id == card.id }
        state.activityLog.record(ActivityCategory.Study, "Deleted ${card.character}")
        state.toastHost.show("Deleted ${card.character}", kind = ToastKind.Info)
    }))
}

@Composable
private fun statusColor(status: SrsStatus): Color = when (status) {
    SrsStatus.New -> Color(0xFFA78BFA)
    SrsStatus.Learning -> Color(0xFF7BC8FF)
    SrsStatus.Review -> Color(0xFFC2FC8B)
    SrsStatus.Relearning -> Color(0xFFFEAB57)
    SrsStatus.Suspended, SrsStatus.Buried -> Color(0xFF606060)
}

// ============================================
// SORTING & BULK ACTIONS
// ============================================

private enum class SortMode(val label: String) {
    Default("Sort: Default"),
    Character("Sort: Character"),
    Meaning("Sort: Meaning"),
    Status("Sort: Status"),
    Interval("Sort: Interval"),
    Due("Sort: Due"),
    Tags("Sort: Tags")
}

private fun sortCards(cards: List<DesktopCard>, mode: SortMode): List<DesktopCard> = when (mode) {
    SortMode.Default -> cards
    SortMode.Character -> cards.sortedBy { it.character }
    SortMode.Meaning -> cards.sortedBy { it.meaning.lowercase() }
    SortMode.Status -> cards.sortedBy { it.status.ordinal }
    SortMode.Interval -> cards.sortedBy { it.intervalDays }
    SortMode.Due -> cards.sortedWith(compareBy<DesktopCard> { it.dueAt == null }.thenBy { it.dueAt })
    SortMode.Tags -> cards.sortedBy { it.tags.size }
}

private fun handleCardClick(state: AppState, card: DesktopCard, selectionMode: Boolean) {
    if (selectionMode) {
        if (card.id in state.selectedCardIds) state.selectedCardIds.remove(card.id) else state.selectedCardIds.add(card.id)
    } else {
        state.selectedCard = card
        state.browserViewMode = BrowserViewMode.Details
    }
}

private fun bulkSetFavorite(state: AppState, ids: List<String>, favorite: Boolean) {
    ids.forEach { id ->
        val idx = state.cards.indexOfFirst { it.id == id }
        if (idx >= 0) state.cards[idx] = state.cards[idx].copy(favorite = favorite)
    }
    state.activityLog.record(ActivityCategory.Favorite, "${if (favorite) "Favorited" else "Unfavorited"} ${ids.size} cards")
    state.toastHost.show("${ids.size} cards ${if (favorite) "favorited" else "updated"}", kind = ToastKind.Success)
}

private fun bulkSuspend(state: AppState, ids: List<String>) {
    ids.forEach { id ->
        val idx = state.cards.indexOfFirst { it.id == id }
        if (idx >= 0) state.cards[idx] = state.cards[idx].copy(status = SrsStatus.Suspended, dueAt = null)
    }
    state.activityLog.record(ActivityCategory.Study, "Suspended ${ids.size} cards")
    state.toastHost.show("Suspended ${ids.size} cards", kind = ToastKind.Success)
}

private fun bulkBury(state: AppState, ids: List<String>) {
    ids.forEach { id ->
        val idx = state.cards.indexOfFirst { it.id == id }
        if (idx >= 0) state.cards[idx] = state.cards[idx].copy(status = SrsStatus.Buried, dueAt = null)
    }
    state.activityLog.record(ActivityCategory.Study, "Buried ${ids.size} cards")
    state.toastHost.show("Buried ${ids.size} cards", kind = ToastKind.Info)
}

@Composable
private fun BatchEditDialog(
    state: AppState,
    count: Int,
    onApply: (SrsStatus?, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val sc = surfaceColors()
    var statusText by remember { mutableStateOf("") }
    var noteAppend by remember { mutableStateOf("") }
    var tagsReplace by remember { mutableStateOf("") }

    DsDialog(title = "Batch edit $count cards", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            Text(
                text = "Apply shared changes to all $count selected cards. Leave a field empty to keep it unchanged.",
                color = sc.textSecondary,
                fontSize = DsType.Body
            )
            DsSelect(
                selected = statusText,
                options = listOf("") + SrsStatus.entries.map { it.name },
                onSelected = { statusText = it },
                labelOf = { if (it.isEmpty()) "Keep current status" else "Set status: $it" },
                modifier = Modifier.fillMaxWidth()
            )
            DsTextField(
                value = noteAppend,
                onValueChange = { noteAppend = it },
                label = "Append to note",
                placeholder = "e.g. Review this again",
                singleLine = false
            )
            DsTextField(
                value = tagsReplace,
                onValueChange = { tagsReplace = it },
                label = "Replace tags",
                placeholder = "comma separated — replaces existing tags"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
            ) {
                DsButton(text = "Cancel", kind = DsButtonKind.Ghost, onClick = onDismiss)
                DsButton(
                    text = "Apply",
                    enabled = statusText.isNotEmpty() || noteAppend.isNotBlank() || tagsReplace.isNotBlank(),
                    onClick = {
                        onApply(
                            if (statusText.isEmpty()) null else SrsStatus.fromName(statusText),
                            noteAppend.trim(),
                            tagsReplace.trim()
                        )
                        onDismiss()
                    }
                )
            }
        }
    }
}

private fun bulkEditFields(state: AppState, ids: List<String>, status: SrsStatus?, noteAppend: String, tagsReplace: String) {
    if (ids.isEmpty()) return
    ids.forEach { id ->
        val idx = state.cards.indexOfFirst { it.id == id }
        if (idx >= 0) {
            var updated = state.cards[idx]
            if (status != null) {
                updated = updated.copy(
                    status = status,
                    dueAt = if (status == SrsStatus.New) null else updated.dueAt
                )
            }
            if (noteAppend.isNotBlank()) {
                updated = updated.copy(
                    note = updated.note.let { if (it.isBlank()) noteAppend else "$it\n$noteAppend" }
                )
            }
            if (tagsReplace.isNotBlank()) {
                updated = updated.copy(
                    tags = tagsReplace.split(',').map { it.trim() }.filter { it.isNotBlank() }.distinct()
                )
            }
            state.cards[idx] = updated
        }
    }
    state.activityLog.record(ActivityCategory.Study, "Batch-edited ${ids.size} cards")
    state.toastHost.show("Batch edit applied to ${ids.size} cards", kind = ToastKind.Success)
}

private fun bulkReset(state: AppState, ids: List<String>) {
    ids.forEach { id ->
        val idx = state.cards.indexOfFirst { it.id == id }
        if (idx >= 0) state.cards[idx] = state.cards[idx].copy(status = SrsStatus.New, intervalDays = 0.0, reps = 0, lapses = 0, dueAt = null)
    }
    state.activityLog.record(ActivityCategory.Study, "Reset ${ids.size} cards to new")
    state.toastHost.show("Reset ${ids.size} cards to new", kind = ToastKind.Success)
}

private fun bulkAddTag(state: AppState, ids: List<String>, tag: String) {
    val t = tag.trim()
    if (t.isBlank()) return
    var count = 0
    ids.forEach { id ->
        val idx = state.cards.indexOfFirst { it.id == id }
        if (idx >= 0 && t !in state.cards[idx].tags) {
            state.cards[idx] = state.cards[idx].copy(tags = state.cards[idx].tags + t)
            count++
        }
    }
    state.activityLog.record(ActivityCategory.Tag, "Tagged $count cards with $t")
    state.toastHost.show("Tagged $count cards", kind = ToastKind.Success)
}

private fun bulkAddFlag(state: AppState, ids: List<String>, flag: String) {
    val f = flag.trim()
    if (f.isBlank()) return
    var count = 0
    ids.forEach { id ->
        val idx = state.cards.indexOfFirst { it.id == id }
        if (idx >= 0 && f !in state.cards[idx].flags) {
            state.cards[idx] = state.cards[idx].copy(flags = state.cards[idx].flags + f)
            count++
        }
    }
    state.activityLog.record(ActivityCategory.Flag, "Flagged $count cards with $f")
    state.toastHost.show("Flagged $count cards", kind = ToastKind.Success)
}

private fun bulkDelete(state: AppState, ids: List<String>) {
    state.cards.removeAll { it.id in ids }
    state.selectedCardIds.removeAll(ids.toSet())
    state.activityLog.record(ActivityCategory.Study, "Deleted ${ids.size} cards")
    state.toastHost.show("Deleted ${ids.size} cards", kind = ToastKind.Info)
}

// ============================================
// SEARCH SUGGESTIONS
// Live suggestions under the search field as the
// user types: recent searches, tags, flag colors
// and query operators. All in-memory and fast;
// applying a suggestion is just setting the query.
// ============================================

private data class BrowseSuggestion(
    val label: String,
    val subtitle: String,
    val query: String,
    val kind: String // recent | tag | flag | operator
)

private fun buildBrowseSuggestions(state: AppState, raw: String): List<BrowseSuggestion> {
    val q = raw.trim().lowercase()
    val out = LinkedHashMap<String, BrowseSuggestion>()

    fun matches(text: String) = q.isBlank() || text.lowercase().contains(q)
    fun add(suggestion: BrowseSuggestion) {
        if (out.size < 8) out.putIfAbsent(suggestion.query, suggestion)
    }

    // Recent searches (most recent first).
    state.filterStore.recent.forEach { filter ->
        if (matches(filter.name)) {
            add(BrowseSuggestion(filter.name, "recent search", filter.query, "recent"))
        }
    }

    // Saved filters (pinned / most-used first) — the same list Sync shows.
    state.filterStore.all().forEach { filter ->
        if (matches(filter.name) || matches(filter.query)) {
            add(BrowseSuggestion("Saved: ${filter.name}", filter.query, filter.query, "saved"))
        }
    }

    // Tags actually present in the card pool.
    state.cards.flatMap { it.tags }.distinct().sorted().forEach { tag ->
        if (matches(tag)) {
            add(BrowseSuggestion("tag:$tag", "filter by tag", "tag:$tag", "tag"))
        }
    }

    // Flag colors.
    flagColors.keys.sorted().forEach { color ->
        if (matches(color)) {
            add(BrowseSuggestion("flag:$color", "filter by flag color", "flag:$color", "flag"))
        }
    }

    // Query operators — only while the user is typing the operator itself
    // (no colon yet), so they never fight with tag/flag value matches.
    if (!q.contains(":")) {
        val text = raw.trim()
        listOf(
            "meaning:" to "search the meaning text",
            "on:" to "match an on-yomi reading",
            "kun:" to "match a kun-yomi reading",
            "tag:" to "filter by tag",
            "flag:" to "filter by flag color",
            "status:" to "filter by SRS state (new/learning/review/…)",
            "interval:" to "filter by interval (e.g. interval:>=21)",
            "id:" to "match a specific card id"
        ).forEach { (op, description) ->
            if (q.isBlank() || op.startsWith(q)) {
                val query = if (text.isBlank()) op else "$text $op"
                add(BrowseSuggestion(op, description, query, "operator"))
            }
        }
    }

    return out.values.toList()
}

@Composable
private fun SuggestionDropdown(
    suggestions: List<BrowseSuggestion>,
    selectedIndex: Int,
    onSelect: (BrowseSuggestion) -> Unit,
    onHover: (Int) -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()

    Column(
        modifier = Modifier
            .width(380.dp)
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(sc.surfaceElevated)
            .border(1.dp, sc.border.copy(alpha = 0.6f), RoundedCornerShape(DsRadius.Md))
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        suggestions.forEachIndexed { index, suggestion ->
            val interaction = remember { MutableInteractionSource() }
            val hovered by interaction.collectIsHoveredAsState()
            val selected = index == selectedIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DsRadius.Sm))
                    .background(if (selected) sc.surfaceInteractive else Color.Transparent)
                    .hoverable(interaction)
                    .clickable(interactionSource = interaction, indication = null) { onSelect(suggestion) }
                    .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (suggestion.kind) {
                        "recent" -> Icons.Default.History
                        "saved" -> Icons.Default.Bookmarks
                        "tag" -> Icons.Default.Label
                        "flag" -> Icons.Default.Flag
                        else -> Icons.Default.Search
                    },
                    contentDescription = null,
                    tint = if (selected) ac.primary else sc.textMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(DsSpacing.Sm))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = suggestion.label,
                        color = sc.textPrimary,
                        fontSize = DsType.Body,
                        maxLines = 1
                    )
                    Text(
                        text = suggestion.subtitle,
                        color = sc.textMuted,
                        fontSize = DsType.Caption,
                        maxLines = 1
                    )
                }
            }
            LaunchedEffect(hovered) {
                if (hovered) onHover(index)
            }
        }
    }
}
