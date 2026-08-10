package ua.syt0r.kanji.desktop.ui.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.WorkspaceView
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsChip
import ua.syt0r.kanji.desktop.designsystem.DsConfirmDialog
import ua.syt0r.kanji.desktop.designsystem.DsDialog
import ua.syt0r.kanji.desktop.designsystem.DsEmptyState
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsMenuDivider
import ua.syt0r.kanji.desktop.designsystem.DsMenuItem
import ua.syt0r.kanji.desktop.designsystem.DsMenuItemRow
import ua.syt0r.kanji.desktop.designsystem.DsNumericField
import ua.syt0r.kanji.desktop.designsystem.DsProgressBar
import ua.syt0r.kanji.desktop.designsystem.DsPromptDialog
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSearchField
import ua.syt0r.kanji.desktop.designsystem.DsSelect
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTagChip
import ua.syt0r.kanji.desktop.designsystem.DsTextField
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.dueColor
import ua.syt0r.kanji.desktop.designsystem.favoriteColor
import ua.syt0r.kanji.desktop.designsystem.infoColor
import ua.syt0r.kanji.desktop.designsystem.successColor
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.designsystem.warningColor
import ua.syt0r.kanji.desktop.designsystem.DsToggle
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.search.SearchEngine
import ua.syt0r.kanji.desktop.engine.transfer.TransferFilePicker
import ua.syt0r.kanji.desktop.model.ContentKind
import ua.syt0r.kanji.desktop.model.DeckDef
import ua.syt0r.kanji.desktop.model.DeckExportDto
import ua.syt0r.kanji.desktop.model.DeckModeStats
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.LibrarySuggestion
import ua.syt0r.kanji.desktop.model.StudyMode
import ua.syt0r.kanji.desktop.model.StudyModeProgress
import ua.syt0r.kanji.desktop.model.ToastKind
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ============================================
// LIBRARY
// The study workflow hub. Decks are typed by
// content kind (kanji, vocabulary, grammar,
// radicals, sentences) and every kind exposes its
// own set of study modes — flashcards, writing,
// recognition, recall, cloze and pattern drills.
// ============================================

private sealed interface LibraryScope {
    val label: String
    val count: (AppState) -> Int

    data object All : LibraryScope {
        override val label = "All decks"
        override val count = { state: AppState -> state.library.rootDecks().size }
    }

    data class Kind(val kind: ContentKind) : LibraryScope {
        override val label = kind.label
        override val count = { state: AppState -> state.library.decksForKind(kind).size }
    }

    data object DueToday : LibraryScope {
        override val label = "Due today"
        override val count = { state: AppState -> state.library.dueToday(state.cards.toList()).size }
    }

    data object New : LibraryScope {
        override val label = "New"
        override val count = { state: AppState -> state.library.newCards(state.cards.toList()).size }
    }

    data object Favorites : LibraryScope {
        override val label = "Favorites"
        override val count = { state: AppState -> state.library.favorites(state.cards.toList()).size }
    }

    data object Recent : LibraryScope {
        override val label = "Recently studied"
        override val count = { state: AppState -> state.library.studiedCards(state.cards.toList()).size }
    }

    data object Archived : LibraryScope {
        override val label = "Archived"
        override val count = { state: AppState -> state.library.archived().size }
    }
}

private fun scopeToName(scope: LibraryScope): String = when (scope) {
    LibraryScope.All -> "all"
    is LibraryScope.Kind -> "kind:${scope.kind.name}"
    LibraryScope.DueToday -> "due"
    LibraryScope.New -> "new"
    LibraryScope.Favorites -> "favorites"
    LibraryScope.Recent -> "recent"
    LibraryScope.Archived -> "archived"
}

private fun restoreLibraryScope(name: String): LibraryScope = when {
    name == "all" -> LibraryScope.All
    name == "due" -> LibraryScope.DueToday
    name == "new" -> LibraryScope.New
    name == "favorites" -> LibraryScope.Favorites
    name == "recent" -> LibraryScope.Recent
    name == "archived" -> LibraryScope.Archived
    name.startsWith("kind:") ->
        ContentKind.entries.firstOrNull { it.name == name.removePrefix("kind:") }?.let { LibraryScope.Kind(it) } ?: LibraryScope.All
    else -> LibraryScope.All
}

private fun restoreDeckSort(name: String): DeckSort =
    DeckSort.entries.firstOrNull { it.name == name } ?: DeckSort.Name

@Composable
fun LibraryView(state: AppState) {
    val sc = surfaceColors()
    var scope by remember { mutableStateOf(restoreLibraryScope(state.settings.getString("browser.library-scope", "all"))) }
    var query by remember { mutableStateOf("") }
    var selectedDeckId by remember { mutableStateOf<String?>(null) }
    var openEntryId by remember { mutableStateOf<String?>(null) }
    var browseDeckId by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    // When arriving from another view with a card selected (dashboard,
    // collections, tags…), surface that card's detail page right away —
    // the Library is the browser now. The selection is consumed here so
    // returning to the Library later starts fresh.
    LaunchedEffect(Unit) {
        state.selectedCard?.let {
            openEntryId = it.id
            state.selectedCard = null
        }
    }

    val selectScope: (LibraryScope) -> Unit = {
        scope = it
        state.settings.set("browser.library-scope", scopeToName(it))
        selectedDeckId = null
        openEntryId = null
        browseDeckId = null
    }
    val selectedDeck = selectedDeckId?.let { state.library.deck(it) }
    val openEntry = openEntryId?.let { id -> state.cards.firstOrNull { it.id == id } }
    val cards = state.cards.toList()

    Row(Modifier.fillMaxSize()) {
        // Scope rail
        Column(
            modifier = Modifier
                .width(212.dp)
                .fillMaxHeight()
                .background(sc.background)
                .padding(vertical = DsSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "LIBRARY",
                color = sc.textMuted,
                fontSize = DsType.Caption,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Xs)
            )
            LibraryScopeItem(LibraryScope.All, scope == LibraryScope.All, LibraryScope.All.count(state)) { selectScope(LibraryScope.All) }
            Text(
                text = "CONTENT",
                color = sc.textMuted,
                fontSize = DsType.Caption,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Sm)
            )
            ContentKind.entries.forEach { kind ->
                val target = LibraryScope.Kind(kind)
                LibraryScopeItem(target, scope == target, state.library.decksForKind(kind).size) { selectScope(target) }
            }
            Text(
                text = "SMART SCOPES",
                color = sc.textMuted,
                fontSize = DsType.Caption,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Sm)
            )
            listOf(LibraryScope.DueToday, LibraryScope.New, LibraryScope.Favorites, LibraryScope.Recent).forEach { smart ->
                LibraryScopeItem(smart, scope == smart, smart.count(state)) { selectScope(smart) }
            }
            Text(
                text = "MANAGE",
                color = sc.textMuted,
                fontSize = DsType.Caption,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Sm)
            )
            LibraryScopeItem(LibraryScope.Archived, scope == LibraryScope.Archived, LibraryScope.Archived.count(state)) { selectScope(LibraryScope.Archived) }
            Spacer(Modifier.weight(1f))
            DsButton(
                text = "New deck",
                icon = Icons.Default.Add,
                onClick = { showCreate = true },
                modifier = Modifier.padding(horizontal = DsSpacing.Lg)
            )
        }

        // Main content — the universal search bar sits above every
        // browsing surface, so searching, filtering and opening entries
        // all happen without leaving the Library.
        Column(Modifier.weight(1f).fillMaxHeight()) {
            if (openEntry == null && selectedDeck == null) {
                LibrarySearchBar(
                    state = state,
                    query = query,
                    onQueryChange = { query = it },
                    onOpenDeck = { selectedDeckId = it.id; query = "" },
                    onOpenEntry = { openEntryId = it.id; query = "" }
                )
            }
            when {
                openEntry != null -> EntryDetail(
                    state = state,
                    card = openEntry,
                    onBack = { openEntryId = null },
                    onOpenEntry = { openEntryId = it.id }
                )
                selectedDeck != null && browseDeckId == selectedDeck.id -> DeckEntriesView(
                    state = state,
                    deck = selectedDeck,
                    onBack = { browseDeckId = null },
                    onOpenEntry = { openEntryId = it.id }
                )
                selectedDeck != null -> DeckDetail(
                    state = state,
                    deck = selectedDeck,
                    onBack = {
                        selectedDeckId = null
                        browseDeckId = null
                    },
                    onBrowse = { browseDeckId = selectedDeck.id },
                    onOpenEntry = { openEntryId = it.id }
                )
                query.isNotBlank() -> LibrarySearchResults(
                    state = state,
                    query = query,
                    onQueryChange = { query = it },
                    onOpenDeck = { selectedDeckId = it.id; query = "" },
                    onOpenEntry = { openEntryId = it.id; query = "" }
                )
                else -> DeckCatalog(
                    state = state,
                    scope = scope,
                    onOpen = { selectedDeckId = it.id },
                    onOpenEntry = { openEntryId = it.id },
                    onCreate = { showCreate = true }
                )
            }
        }
    }

    if (showCreate) {
        CreateDeckDialog(state, onDismiss = { showCreate = false }, onCreated = { deck ->
            showCreate = false
            selectedDeckId = deck.id
        })
    }
}

@Composable
private fun LibraryScopeItem(scope: LibraryScope, selected: Boolean, count: Int, onClick: () -> Unit) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DsSpacing.Sm)
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(
                when {
                    selected -> ac.primary.copy(alpha = 0.16f)
                    hovered -> sc.surfaceInteractive.copy(alpha = 0.6f)
                    else -> Color.Transparent
                }
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = scope.label,
            color = if (selected) ac.primary else sc.textSecondary,
            fontSize = DsType.Body,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            color = if (selected) ac.primary.copy(alpha = 0.8f) else sc.textMuted,
            fontSize = DsType.Caption
        )
    }
}

// ============================================
// DECK CATALOG
// ============================================

private enum class DeckSort(val label: String) {
    Name("Name"),
    Newest("Newest"),
    Due("Most due"),
    New("Most new"),
    Favorite("Favorites first")
}

@Composable
private fun DeckCatalog(
    state: AppState,
    scope: LibraryScope,
    onOpen: (DeckDef) -> Unit,
    onOpenEntry: (DesktopCard) -> Unit,
    onCreate: () -> Unit
) {
    val sc = surfaceColors()
    val cards = state.cards.toList()
    val now = Clock.System.now()

    var folderPath by remember(scope) { mutableStateOf<List<String>>(emptyList()) }
    var sortMode by remember(scope) { mutableStateOf(restoreDeckSort(state.settings.getString("browser.library-sort", "name"))) }
    var selectionMode by remember { mutableStateOf(false) }
    var jlptFilter by remember(scope) { mutableStateOf(state.settings.getInt("browser.library-filter-jlpt", 0).takeIf { it > 0 }) }
    var difficultyFilter by remember(scope) { mutableStateOf(state.settings.getInt("browser.library-filter-difficulty", 0).takeIf { it > 0 }) }
    var favoritesOnly by remember(scope) { mutableStateOf(state.settings.getBool("browser.library-filter-favorites")) }
    val selectedIds = remember { mutableStateListOf<String>() }
    var bulkDeleteConfirm by remember { mutableStateOf(false) }
    var createFolder by remember { mutableStateOf(false) }

    val currentFolderId = folderPath.lastOrNull()
    val folderDeck = currentFolderId?.let { state.library.deck(it) }

    val decks = remember(
        state.library.revision, scope, sortMode, folderPath, jlptFilter, difficultyFilter, favoritesOnly
    ) {
        val base = when (scope) {
            is LibraryScope.Kind -> state.library.decksForKind(scope.kind)
            LibraryScope.All -> state.library.childrenOf(currentFolderId)
            else -> emptyList()
        }
        val filtered = base.filter { deck ->
            (jlptFilter == null ||
                deck.filterQuery.contains("jlpt:$jlptFilter", ignoreCase = true) ||
                deck.tags.any { it.contains("jlpt-$jlptFilter", ignoreCase = true) }) &&
                (difficultyFilter == null || deck.difficulty == difficultyFilter) &&
                (!favoritesOnly || deck.favorite)
        }
        when (sortMode) {
            DeckSort.Name -> filtered.sortedBy { it.name.lowercase() }
            DeckSort.Newest -> filtered.sortedByDescending { it.createdAt }
            DeckSort.Favorite -> filtered.sortedWith(
                compareByDescending<DeckDef> { it.favorite }.thenBy { it.name.lowercase() }
            )
            DeckSort.Due -> filtered.sortedByDescending { state.library.deckStats(it, cards, now).anyDue }
            DeckSort.New -> filtered.sortedByDescending { state.library.deckStats(it, cards, now).anyNew }
        }
    }
    val folders = decks.filter { state.library.childrenOf(it.id).isNotEmpty() }
    val leafDecks = decks.filterNot { it in folders }

    Column(Modifier.fillMaxSize()) {
        // Header: title + sort + management controls (search lives in the universal bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DsSpacing.Xl, vertical = DsSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            Text(
                text = when (scope) {
                    is LibraryScope.Kind -> "${scope.kind.label} decks"
                    LibraryScope.All -> "All decks"
                    else -> scope.label
                },
                color = sc.textPrimary,
                fontSize = DsType.Heading,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            DsSelect(
                selected = sortMode,
                options = DeckSort.entries.toList(),
                onSelected = { sortMode = it; state.settings.set("browser.library-sort", it.name) },
                labelOf = { it.label },
                modifier = Modifier.width(140.dp)
            )
            DsButton(
                text = "New folder",
                icon = Icons.Default.Folder,
                onClick = { createFolder = true },
                kind = DsButtonKind.Secondary
            )
            DsButton(
                text = "New deck",
                icon = Icons.Default.Add,
                onClick = onCreate,
                kind = DsButtonKind.Secondary
            )
            DsButton(
                text = if (selectionMode) "Exit select" else "Select",
                icon = Icons.Default.CheckBoxOutlineBlank,
                kind = DsButtonKind.Ghost,
                onClick = {
                    selectionMode = !selectionMode
                    if (!selectionMode) selectedIds.clear()
                }
            )
        }

        // Instant filters: JLPT, difficulty, favorites
        if (scope is LibraryScope.All || scope is LibraryScope.Kind) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DsSpacing.Xl),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
            ) {
                DsSelect(
                    selected = jlptFilter,
                    options = listOf<Int?>(null, 5, 4, 3, 2, 1),
                    onSelected = { jlptFilter = it; state.settings.setInt("browser.library-filter-jlpt", it ?: 0) },
                    labelOf = { it?.let { l -> "JLPT N$l" } ?: "JLPT: All" },
                    modifier = Modifier.width(140.dp)
                )
                DsSelect(
                    selected = difficultyFilter,
                    options = listOf<Int?>(null, 1, 2, 3, 4, 5),
                    onSelected = { difficultyFilter = it; state.settings.setInt("browser.library-filter-difficulty", it ?: 0) },
                    labelOf = { it?.let { "★".repeat(it) } ?: "Difficulty: All" },
                    modifier = Modifier.width(150.dp)
                )
                DsToggle(
                    checked = favoritesOnly,
                    onCheckedChange = { favoritesOnly = it; state.settings.setBool("browser.library-filter-favorites", it) },
                    label = "Favorites only",
                    modifier = Modifier.width(160.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${decks.size} deck${if (decks.size == 1) "" else "s"}",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
            Spacer(Modifier.height(DsSpacing.Sm))
        }

        // Breadcrumb when inside a folder (All scope only)
        if (scope is LibraryScope.All && folderPath.isNotEmpty() && folderDeck != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DsSpacing.Xl),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "All decks",
                    color = sc.textMuted,
                    fontSize = DsType.Body,
                    modifier = Modifier
                        .clip(RoundedCornerShape(DsRadius.Sm))
                        .clickable { folderPath = emptyList() }
                        .padding(horizontal = DsSpacing.Sm, vertical = 2.dp)
                )
                Text("/", color = sc.textMuted, fontSize = DsType.Caption)
                folderPath.forEachIndexed { index, id ->
                    val def = state.library.deck(id)
                    if (def != null) {
                        val isLast = index == folderPath.lastIndex
                        Text(
                            text = def.name,
                            color = if (isLast) sc.textPrimary else sc.textMuted,
                            fontSize = DsType.Body,
                            fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(DsRadius.Sm))
                                .clickable { folderPath = folderPath.take(index + 1) }
                                .padding(horizontal = DsSpacing.Sm, vertical = 2.dp)
                        )
                        if (!isLast) Text("/", color = sc.textMuted, fontSize = DsType.Caption)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${decks.size} items",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
            Spacer(Modifier.height(DsSpacing.Sm))
        }

        when (scope) {
            LibraryScope.DueToday -> EntryScopeGrid(state, "Due today", state.library.dueToday(cards), onOpenEntry)
            LibraryScope.New -> EntryScopeGrid(state, "New cards", state.library.newCards(cards), onOpenEntry)
            LibraryScope.Favorites -> EntryScopeGrid(state, "Favorites", state.library.favorites(cards), onOpenEntry)
            LibraryScope.Recent -> EntryScopeGrid(state, "Recently studied", state.library.studiedCards(cards), onOpenEntry)
            LibraryScope.Archived -> ArchivedCatalog(state, onRestore = { onOpen(it) })
            else -> {
                if (decks.isEmpty()) {
                    DsEmptyState(
                        title = if (folderDeck != null) "This folder is empty" else "No decks found",
                        message = when {
                            folderDeck != null -> "Move a deck into this folder, or create a new one here."
                            else -> "Create a deck or import content to get started."
                        },
                        icon = Icons.Default.Folder,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(Modifier.weight(1f).fillMaxWidth()) {
                        if (selectionMode) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = DsSpacing.Xl, vertical = DsSpacing.Sm)
                                    .clip(RoundedCornerShape(DsRadius.Md))
                                    .background(sc.surfaceElevated)
                                    .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Xs),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                            ) {
                                Text(
                                    text = "${selectedIds.size} selected",
                                    color = sc.textPrimary,
                                    fontSize = DsType.Label,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                DsButton(
                                    text = "Select all",
                                    kind = DsButtonKind.Ghost,
                                    compact = true,
                                    onClick = {
                                        selectedIds.clear()
                                        decks.forEach { selectedIds.add(it.id) }
                                    }
                                )
                                DsButton(
                                    text = "Archive",
                                    icon = Icons.Default.Archive,
                                    kind = DsButtonKind.Secondary,
                                    compact = true,
                                    enabled = selectedIds.isNotEmpty(),
                                    onClick = {
                                        selectedIds.toList().forEach { id -> state.library.toggleArchived(id) }
                                        state.activityLog.record(ActivityCategory.Deck, "Archived ${selectedIds.size} decks")
                                        state.toastHost.show("Archived ${selectedIds.size} decks", kind = ToastKind.Success)
                                        selectedIds.clear()
                                    }
                                )
                                DsButton(
                                    text = "Export",
                                    icon = Icons.Default.FileDownload,
                                    kind = DsButtonKind.Secondary,
                                    compact = true,
                                    enabled = selectedIds.isNotEmpty(),
                                    onClick = {
                                        exportDecks(state, selectedIds.mapNotNull { state.library.deck(it) })
                                    }
                                )
                                DsButton(
                                    text = "Delete",
                                    icon = Icons.Default.Delete,
                                    kind = DsButtonKind.Danger,
                                    compact = true,
                                    enabled = selectedIds.isNotEmpty(),
                                    onClick = { bulkDeleteConfirm = true }
                                )
                                DsButton(
                                    text = "Done",
                                    kind = DsButtonKind.Ghost,
                                    compact = true,
                                    onClick = { selectionMode = false; selectedIds.clear() }
                                )
                            }
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(300.dp),
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(
                                start = DsSpacing.Xl, end = DsSpacing.Xl, bottom = DsSpacing.Xl
                            ),
                            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md),
                            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                        ) {
                            items(folders, key = { it.id }) { folder ->
                                DeckCard(
                                    state = state,
                                    deck = folder,
                                    now = now,
                                    isFolder = true,
                                    selectionMode = selectionMode,
                                    selected = folder.id in selectedIds,
                                    onToggleSelect = {
                                        if (folder.id in selectedIds) selectedIds.remove(folder.id)
                                        else selectedIds.add(folder.id)
                                    },
                                    onOpen = {
                                        if (scope is LibraryScope.All) folderPath = folderPath + folder.id
                                        else onOpen(folder)
                                    }
                                )
                            }
                            items(leafDecks, key = { it.id }) { deck ->
                                DeckCard(
                                    state = state,
                                    deck = deck,
                                    now = now,
                                    isFolder = false,
                                    selectionMode = selectionMode,
                                    selected = deck.id in selectedIds,
                                    onToggleSelect = {
                                        if (deck.id in selectedIds) selectedIds.remove(deck.id)
                                        else selectedIds.add(deck.id)
                                    },
                                    onOpen = { onOpen(deck) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (createFolder) {
        DsPromptDialog(
            title = "New folder",
            placeholder = "Folder name",
            onConfirm = { name ->
                if (name.isNotBlank()) {
                    val folder = state.library.create(
                        name = name,
                        description = if (folderDeck != null) "Folder inside ${folderDeck.name}" else "Folder",
                        kind = ContentKind.Kanji
                    )
                    state.activityLog.record(ActivityCategory.Deck, "Created folder \"${folder.name}\"")
                    folderPath = folderPath + folder.id
                }
            },
            onDismiss = { createFolder = false }
        )
    }
    if (bulkDeleteConfirm) {
        DsConfirmDialog(
            title = "Delete ${selectedIds.size} decks?",
            message = "This permanently deletes the selected decks and everything inside them, including any sub-folders. This cannot be undone.",
            confirmText = "Delete",
            danger = true,
            onConfirm = {
                selectedIds.toList().forEach { id -> state.library.delete(id) }
                state.activityLog.record(ActivityCategory.Deck, "Deleted ${selectedIds.size} decks")
                state.toastHost.show("Deleted ${selectedIds.size} decks", kind = ToastKind.Info)
                selectedIds.clear()
            },
            onDismiss = { bulkDeleteConfirm = false }
        )
    }
}

/** Export one or more decks to a single JSON file via the native save dialog. */
private fun exportDecks(state: AppState, decks: List<DeckDef>) {
    if (decks.isEmpty()) return
    val cards = state.cards.toList()
    val dtos = decks.map { deck ->
        val ids = state.library.cardsIn(deck, cards).map { it.id }
        DeckExportDto(deck = deck.copy(cardIds = ids), cardIds = ids)
    }
    val json = Json { prettyPrint = true; encodeDefaults = true }
    val bytes = json.encodeToString<List<DeckExportDto>>(dtos).toByteArray(Charsets.UTF_8)
    val fileName = if (decks.size == 1) "${sanitizeFileName(decks.first().name)}.kaiteyo.json" else "kaiteyo-decks.json"
    val saved = TransferFilePicker.save(
        bytes = bytes,
        fileName = fileName,
        description = "Kaiteyo deck",
        "json"
    )
    if (saved) {
        val count = decks.sumOf { state.library.cardsIn(it, cards).size }
        state.toastHost.show(
            "Exported ${decks.size} deck${if (decks.size == 1) "" else "s"} ($count cards) to file",
            kind = ToastKind.Success
        )
        state.activityLog.record(ActivityCategory.Export, "Exported ${decks.size} deck(s), $count cards")
    }
}

private fun sanitizeFileName(name: String): String =
    name.replace(Regex("[\\\\/:*?\"<>|]"), "-").trim().ifBlank { "deck" }

/** Grid of individual cards for smart scopes (due / new / favorites / recent). */
@Composable
private fun EntryScopeGrid(
    state: AppState,
    title: String,
    entries: List<DesktopCard>,
    onOpenEntry: (DesktopCard) -> Unit
) {
    val sc = surfaceColors()
    Column(Modifier.fillMaxSize().padding(horizontal = DsSpacing.Xl)) {
        Text(
            text = title,
            color = sc.textPrimary,
            fontSize = DsType.Heading,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = DsSpacing.Md)
        )
        if (entries.isEmpty()) {
            DsEmptyState(
                title = "Nothing here yet",
                message = "Study a deck to build this list.",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(220.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
            ) {
                items(entries, key = { it.id }) { card ->
                    EntryCard(state, card, onOpenEntry)
                }
            }
        }
    }
}

@Composable
private fun DeckCard(
    state: AppState,
    deck: DeckDef,
    now: Instant,
    isFolder: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onOpen: () -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()
    val stats = state.library.deckStats(deck, state.cards.toList(), now)
    val modes = StudyMode.forKind(deck.kind)
    val childCount = state.library.childrenOf(deck.id).size
    var manageOpen by remember { mutableStateOf(false) }
    var manageAnchor by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var deckAction by remember { mutableStateOf<DeckAction?>(null) }

    DsCard(onClick = {
        if (selectionMode) onToggleSelect() else onOpen()
    }) {
        Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(DsRadius.Md))
                        .background(ac.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isFolder) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = ac.primary, modifier = Modifier.size(20.dp))
                    } else {
                        Text(
                            text = deck.icon.ifBlank { deck.kind.glyph },
                            color = ac.primary,
                            fontSize = DsType.Title,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(DsSpacing.Sm))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = deck.name,
                        color = sc.textPrimary,
                        fontSize = DsType.BodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isFolder && childCount > 0) "Folder · $childCount item${if (childCount == 1) "" else "s"}"
                        else deck.description,
                        color = sc.textMuted,
                        fontSize = DsType.Caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (selectionMode) {
                    Icon(
                        if (selected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = if (selected) ac.primary else sc.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (deck.favorite) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = favoriteColor(),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs), verticalAlignment = Alignment.CenterVertically) {
                DsBadge(text = deck.kind.label, tint = ac.primary)
                if (stats.anyDue > 0) DsBadge(text = "${stats.anyDue} due", tint = dueColor())
                if (stats.anyNew > 0) DsBadge(text = "${stats.anyNew} new", tint = infoColor())
                DsBadge(text = "${stats.total} cards", tint = sc.textMuted)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                modes.forEach { mode ->
                    val ms = stats.byMode[mode]
                    if (ms != null) {
                        DsModeChip(state, deck, mode, ms, compact = true)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsButton(
                    text = if (isFolder) "Open" else "Study",
                    icon = if (isFolder) Icons.Default.Folder else Icons.Default.PlayArrow,
                    onClick = onOpen,
                    modifier = Modifier.weight(1f),
                    kind = DsButtonKind.Primary,
                    compact = true
                )
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { if (manageAnchor != it) manageAnchor = it }
                        .padding(2.dp)
                ) {
                    DsIconButton(
                        icon = Icons.Default.MoreVert,
                        onClick = { manageOpen = true },
                        contentDescription = "Deck actions",
                        size = 30.dp
                    )
                }
            }
        }
    }

    if (manageOpen && manageAnchor != null) {
        val pos = manageAnchor!!.positionInWindow()
        Popup(
            onDismissRequest = { manageOpen = false },
            offset = IntOffset(pos.x.roundToInt(), pos.y.roundToInt() + manageAnchor!!.size.height),
            properties = PopupProperties(focusable = true)
        ) {
            DeckActionsMenu(
                state = state,
                deck = deck,
                onAction = { deckAction = it },
                onDismiss = { manageOpen = false }
            )
        }
    }

    if (deckAction != null) {
        DeckActionDialogs(
            state = state,
            deck = deck,
            action = deckAction,
            onClose = { deckAction = null },
            onBackToCatalog = {}
        )
    }
}

@Composable
private fun EntryCard(
    state: AppState,
    card: DesktopCard,
    onOpenEntry: (DesktopCard) -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onToggleSelect: (() -> Unit)? = null
) {
    val sc = surfaceColors()
    val ac = accent()
    val deckId = state.library.deckIdFor(card, state.cards.toList())
    val deck = deckId?.let { state.library.deck(it) }

    DsCard(onClick = {
        if (selectionMode) onToggleSelect?.invoke() else onOpenEntry(card)
    }) {
        Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
            ) {
                Text(
                    text = card.character,
                    color = sc.textPrimary,
                    fontSize = DsType.Heading,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (selectionMode) {
                    Icon(
                        if (selected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = if (selected) ac.primary else sc.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = card.meaning,
                color = sc.textSecondary,
                fontSize = DsType.Body,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                DsBadge(text = card.contentKind.label, tint = ac.primary)
                if (deck != null) DsBadge(text = deck.name, tint = sc.textMuted)
                DsBadge(text = card.status.name, tint = sc.textMuted)
            }
        }
    }
}

// ============================================
// ARCHIVED CATALOG
// ============================================

@Composable
private fun ArchivedCatalog(
    state: AppState,
    onRestore: (DeckDef) -> Unit
) {
    val sc = surfaceColors()
    var query by remember { mutableStateOf("") }
    val archived = remember(state.library.revision, query) {
        val q = query.trim()
        state.library.archived().filter { deck ->
            q.isBlank() || deck.name.contains(q, ignoreCase = true) ||
                deck.description.contains(q, ignoreCase = true)
        }.sortedBy { it.name.lowercase() }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DsSpacing.Xl, vertical = DsSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search archived decks…",
                modifier = Modifier.weight(1f)
            )
        }
        if (archived.isEmpty()) {
            DsEmptyState(
                title = "Nothing archived",
                message = "Archived decks hide from your active library until you restore them.",
                icon = Icons.Default.Archive,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = DsSpacing.Xl),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
            ) {
                archived.forEach { deck ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(DsRadius.Md))
                            .background(sc.surface)
                            .padding(DsSpacing.Md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(DsRadius.Md))
                                .background(sc.surfaceInteractive),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(deck.icon.ifBlank { deck.kind.glyph }, color = sc.textMuted, fontSize = DsType.Title, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(DsSpacing.Sm))
                        Column(Modifier.weight(1f)) {
                            Text(deck.name, color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${deck.kind.label} · ${state.library.cardsIn(deck, state.cards.toList()).size} cards",
                                color = sc.textMuted,
                                fontSize = DsType.Caption
                            )
                        }
                        DsButton(
                            text = "Restore",
                            icon = Icons.Default.Restore,
                            kind = DsButtonKind.Secondary,
                            compact = true,
                            onClick = {
                                state.library.toggleArchived(deck.id)
                                state.activityLog.record(ActivityCategory.Deck, "Restored deck \"${deck.name}\"")
                                onRestore(deck)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// DECK DETAIL
// ============================================

@Composable
private fun DeckDetail(
    state: AppState,
    deck: DeckDef,
    onBack: () -> Unit,
    onBrowse: () -> Unit,
    onOpenEntry: (DesktopCard) -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()
    val now = Clock.System.now()
    val cards = state.cards.toList()
    val stats = state.library.deckStats(deck, cards, now)
    val modes = StudyMode.forKind(deck.kind)
    var manageOpen by remember { mutableStateOf(false) }
    var tagsOpen by remember { mutableStateOf(false) }
    var editOpen by remember { mutableStateOf(false) }
    var deckAction by remember { mutableStateOf<DeckAction?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DsSpacing.Xl, vertical = DsSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsIconButton(icon = Icons.Default.ArrowBack, onClick = onBack, contentDescription = "Back to library")
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(DsRadius.Md))
                    .background(ac.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = deck.icon.ifBlank { deck.kind.glyph },
                    color = ac.primary,
                    fontSize = DsType.Heading,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text(
                        text = deck.name,
                        color = sc.textPrimary,
                        fontSize = DsType.Heading,
                        fontWeight = FontWeight.Bold
                    )
                    DsBadge(text = deck.kind.label, tint = ac.primary)
                    if (deck.builtIn) DsBadge(text = "Built-in", tint = sc.textMuted)
                    if (deck.archived) DsBadge(text = "Archived", tint = warningColor())
                }
                Text(
                    text = deck.description.ifBlank { "No description" },
                    color = sc.textMuted,
                    fontSize = DsType.Body
                )
                if (deck.tags.isNotEmpty()) {
                    Spacer(Modifier.height(DsSpacing.Xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        deck.tags.take(8).forEach { tag ->
                            DsTagChip(label = tag)
                        }
                    }
                }
            }
            DsIconButton(
                icon = if (deck.favorite) Icons.Default.Star else Icons.Default.StarBorder,
                onClick = { state.library.toggleFavorite(deck.id) },
                contentDescription = "Favorite",
                tint = if (deck.favorite) favoriteColor() else null
            )
            var manageAnchor by remember { mutableStateOf<LayoutCoordinates?>(null) }
            Box(
                modifier = Modifier
                    .onGloballyPositioned { if (manageAnchor != it) manageAnchor = it }
                    .padding(2.dp)
            ) {
                DsIconButton(
                    icon = Icons.Default.MoreVert,
                    onClick = { manageOpen = true },
                    contentDescription = "Deck actions"
                )
            }
            if (manageOpen && manageAnchor != null) {
                val pos = manageAnchor!!.positionInWindow()
                Popup(
                    onDismissRequest = { manageOpen = false },
                    offset = IntOffset(pos.x.roundToInt(), pos.y.roundToInt() + manageAnchor!!.size.height),
                    properties = PopupProperties(focusable = true)
                ) {
                    DeckActionsMenu(
                        state = state,
                        deck = deck,
                        onAction = { deckAction = it },
                        onDismiss = { manageOpen = false }
                    )
                }
            }
        }

        if (deck.archived) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DsSpacing.Xl)
                    .clip(RoundedCornerShape(DsRadius.Md))
                    .background(warningColor().copy(alpha = 0.12f))
                    .padding(DsSpacing.Md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "This deck is archived and hidden from your active library.",
                    color = sc.textSecondary,
                    fontSize = DsType.Body,
                    modifier = Modifier.weight(1f)
                )
                DsButton(
                    text = "Restore deck",
                    icon = Icons.Default.Restore,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    onClick = {
                        state.library.toggleArchived(deck.id)
                        state.activityLog.record(ActivityCategory.Deck, "Restored deck \"${deck.name}\"")
                    }
                )
            }
            Spacer(Modifier.height(DsSpacing.Lg))
        }

        // Summary strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DsSpacing.Xl),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DeckSummaryTile("Cards", stats.total.toString(), Modifier.weight(1f))
            DeckSummaryTile("New", stats.anyNew.toString(), Modifier.weight(1f))
            DeckSummaryTile("Due", stats.anyDue.toString(), Modifier.weight(1f))
            DeckSummaryTile("Completed", stats.anyCompleted.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(DsSpacing.Sm))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DsSpacing.Xl),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DeckSummaryTile("Suspended", stats.byMode.values.sumOf { it.suspendedCount }.toString(), Modifier.weight(1f))
            DeckSummaryTile(
                "Buried",
                stats.byMode.values.sumOf { it.buriedCount }.toString(),
                Modifier.weight(1f)
            )
            DeckSummaryTile(
                "Accuracy",
                if (stats.byMode.values.any { it.totalReviews > 0 })
                    "${(stats.byMode.values.filter { it.totalReviews > 0 }.map { it.accuracy }.average() * 100).toInt()}%"
                else "—",
                Modifier.weight(1f)
            )
            DeckSummaryTile(
                "Avg interval",
                stats.byMode.values.filter { it.avgInterval > 0 }
                    .let { r -> if (r.isEmpty()) "—" else "${r.map { it.avgInterval }.average().toInt()}d" },
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(DsSpacing.Lg))

        // Quick actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DsSpacing.Xl),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
        ) {
            val readyModes = modes.filter { mode ->
                stats.byMode[mode]?.let { it.newCount + it.dueCount > 0 } == true
            }
            val primaryMode = readyModes.firstOrNull() ?: modes.firstOrNull()
            DsButton(
                text = "Study",
                icon = Icons.Default.PlayArrow,
                onClick = {
                    if (primaryMode != null) {
                        if (primaryMode == StudyMode.Writing) state.startLibraryWriting(deck.id)
                        else state.startLibraryStudy(deck.id, primaryMode)
                    }
                },
                enabled = primaryMode != null
            )
            DsButton(
                text = "Browse cards",
                icon = Icons.Default.GridView,
                kind = DsButtonKind.Secondary,
                onClick = onBrowse
            )
            DsButton(
                text = "Edit deck",
                icon = Icons.Default.Create,
                kind = DsButtonKind.Secondary,
                onClick = { editOpen = true }
            )
            DsButton(
                text = "Tags",
                icon = Icons.Default.Label,
                kind = DsButtonKind.Secondary,
                onClick = { tagsOpen = true }
            )
            DsButton(
                text = "Statistics",
                icon = Icons.Default.BarChart,
                kind = DsButtonKind.Secondary,
                onClick = { state.currentView = WorkspaceView.Statistics }
            )
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(DsSpacing.Lg))

        // Study modes
        Text(
            text = "Study modes",
            color = sc.textPrimary,
            fontSize = DsType.Title,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = DsSpacing.Xl, vertical = DsSpacing.Sm)
        )
        modes.forEach { mode ->
            val ms = stats.byMode[mode]
            if (ms != null) {
                ModeCard(state, deck, mode, ms, now)
            }
        }

        // Membership preview
        Spacer(Modifier.height(DsSpacing.Lg))
        Text(
            text = "Deck contents",
            color = sc.textPrimary,
            fontSize = DsType.Title,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = DsSpacing.Xl, vertical = DsSpacing.Sm)
        )
        val members = state.library.cardsIn(deck, cards)
        if (members.isEmpty()) {
            DsEmptyState(
                title = "This deck has no cards",
                message = if (deck.filterQuery.isBlank()) "Add cards from the browser or import content." else "No content matches this deck's filter yet.",
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            members.take(24).forEach { card ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenEntry(card) }
                        .padding(horizontal = DsSpacing.Xl, vertical = DsSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.character,
                        color = sc.textPrimary,
                        fontSize = DsType.BodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(56.dp)
                    )
                    Text(
                        text = card.meaning,
                        color = sc.textSecondary,
                        fontSize = DsType.Body,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = card.readings.firstOrNull()?.let { "・$it" } ?: "",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }
            if (members.size > 24) {
                Text(
                    text = "+ ${members.size - 24} more — browse all ${members.size} cards",
                    color = sc.textMuted,
                    fontSize = DsType.Caption,
                    modifier = Modifier.padding(horizontal = DsSpacing.Xl)
                )
            }
        }
        Spacer(Modifier.height(DsSpacing.Xl))
    }

    if (tagsOpen) {
        DeckTagsDialog(state, deck, onDismiss = { tagsOpen = false })
    }
    if (editOpen) {
        DeckEditDialog(state, deck, onDismiss = { editOpen = false })
    }
    if (deckAction != null) {
        DeckActionDialogs(
            state = state,
            deck = deck,
            action = deckAction,
            onClose = { deckAction = null },
            onBackToCatalog = onBack
        )
    }
}

@Composable
private fun DeckSummaryTile(label: String, value: String, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(DsRadius.Lg))
            .background(sc.surface)
            .padding(DsSpacing.Md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = sc.textPrimary, fontSize = DsType.Title, fontWeight = FontWeight.Bold)
        Text(label, color = sc.textMuted, fontSize = DsType.Caption)
    }
}

@Composable
private fun ModeCard(state: AppState, deck: DeckDef, mode: StudyMode, stats: DeckModeStats, now: Instant) {
    val sc = surfaceColors()
    val ac = accent()
    val ready = stats.newCount + stats.dueCount

    DsCard(modifier = Modifier.padding(horizontal = DsSpacing.Xl).padding(bottom = DsSpacing.Sm)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DsSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(DsRadius.Md))
                    .background(ac.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(mode.glyph, color = ac.primary, fontSize = DsType.Title, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(DsSpacing.Md))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text(mode.label, color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                    if (ready > 0) DsBadge(text = "$ready ready", tint = if (stats.dueCount > 0) dueColor() else infoColor())
                }
                Text(mode.hint, color = sc.textMuted, fontSize = DsType.Caption)
                Spacer(Modifier.height(DsSpacing.Sm))
                DsProgressBar(fraction = stats.progressFraction, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(DsSpacing.Xs))
                Text(
                    text = "${stats.newCount} new · ${stats.learningCount} learning · ${stats.reviewCount} review · ${stats.masteredCount} mastered",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
            Spacer(Modifier.width(DsSpacing.Md))
            DsButton(
                text = if (ready > 0) "Study" else "Start",
                icon = Icons.Default.PlayArrow,
                onClick = {
                    if (mode == StudyMode.Writing) state.startLibraryWriting(deck.id)
                    else state.startLibraryStudy(deck.id, mode)
                },
                enabled = ready > 0
            )
        }
    }
}

/** Small per-mode pill: glyph + due count, clickable to start that mode directly. */
@Composable
private fun DsModeChip(
    state: AppState,
    deck: DeckDef,
    mode: StudyMode,
    stats: DeckModeStats,
    compact: Boolean
) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val active = stats.dueCount + stats.newCount

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(DsRadius.Full))
            .background(
                when {
                    hovered -> sc.surfaceInteractive
                    active > 0 -> ac.primary.copy(alpha = 0.14f)
                    else -> sc.surfaceElevated
                }
            )
            .clickable(interactionSource = interaction, indication = null) {
                if (mode == StudyMode.Writing) state.startLibraryWriting(deck.id)
                else state.startLibraryStudy(deck.id, mode)
            }
            .hoverable(interaction)
            .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = mode.glyph,
            color = if (active > 0) ac.primary else sc.textMuted,
            fontSize = if (compact) DsType.Caption else DsType.Label
        )
        if (active > 0) {
            Text(
                text = active.toString(),
                color = ac.primary,
                fontSize = DsType.Caption,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ============================================
// DECK ACTIONS MENU + DIALOGS
// The menu is a pure action emitter (it lives inside a Popup and
// would be disposed the moment it closes), while the dialogs render
// at the caller level via [DeckActionDialogs] so they survive.
// ============================================

private enum class DeckAction { Rename, Edit, Move, Merge, Tags, Export, Archive, Delete }

@Composable
private fun DeckActionsMenu(
    state: AppState,
    deck: DeckDef,
    onAction: (DeckAction) -> Unit,
    onDismiss: () -> Unit
) {
    val sc = surfaceColors()
    Column(
        modifier = Modifier
            .width(240.dp)
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(sc.surfaceInteractive)
            .padding(DsSpacing.Xs)
    ) {
        DsMenuItemRow(
            item = DsMenuItem(label = "Rename", icon = Icons.Default.Edit, onAction = {}),
            onClick = { onAction(DeckAction.Rename); onDismiss() }
        )
        DsMenuItemRow(
            item = DsMenuItem(label = "Edit deck…", icon = Icons.Default.Create, onAction = {}),
            onClick = { onAction(DeckAction.Edit); onDismiss() }
        )
        DsMenuItemRow(
            item = DsMenuItem(
                label = if (deck.favorite) "Remove favorite" else "Add to favorites",
                icon = Icons.Default.Favorite,
                onAction = {}
            ),
            onClick = {
                state.library.toggleFavorite(deck.id)
                state.activityLog.record(ActivityCategory.Deck, "${if (deck.favorite) "Unfavorited" else "Favorited"} deck \"${deck.name}\"")
                onDismiss()
            }
        )
        DsMenuItemRow(
            item = DsMenuItem(
                label = if (deck.pinned) "Unpin deck" else "Pin deck",
                icon = Icons.Default.PushPin,
                onAction = {}
            ),
            onClick = { state.library.togglePinned(deck.id); onDismiss() }
        )
        DsMenuDivider()
        DsMenuItemRow(
            item = DsMenuItem(label = "Duplicate deck", icon = Icons.Default.ContentCopy, onAction = {}),
            onClick = {
                val copy = state.library.duplicate(deck)
                state.activityLog.record(ActivityCategory.Deck, "Duplicated deck \"${deck.name}\" → \"${copy.name}\"")
                state.toastHost.show("Duplicated as \"${copy.name}\"", kind = ToastKind.Success)
                onDismiss()
            }
        )
        DsMenuItemRow(
            item = DsMenuItem(label = "Move to folder…", icon = Icons.Default.Folder, onAction = {}),
            onClick = { onAction(DeckAction.Move); onDismiss() }
        )
        DsMenuItemRow(
            item = DsMenuItem(label = "Merge into…", icon = Icons.Default.Add, onAction = {}),
            onClick = { onAction(DeckAction.Merge); onDismiss() }
        )
        DsMenuItemRow(
            item = DsMenuItem(label = "Edit tags…", icon = Icons.Default.Label, onAction = {}),
            onClick = { onAction(DeckAction.Tags); onDismiss() }
        )
        DsMenuItemRow(
            item = DsMenuItem(label = "Export deck…", icon = Icons.Default.FileDownload, onAction = {}),
            onClick = { onAction(DeckAction.Export); onDismiss() }
        )
        DsMenuDivider()
        DsMenuItemRow(
            item = DsMenuItem(
                label = if (deck.archived) "Restore deck" else "Archive deck",
                icon = Icons.Default.Archive,
                onAction = {}
            ),
            onClick = { onAction(DeckAction.Archive); onDismiss() }
        )
        DsMenuItemRow(
            item = DsMenuItem(label = "Delete deck", icon = Icons.Default.Delete, danger = true, onAction = {}),
            onClick = { onAction(DeckAction.Delete); onDismiss() }
        )
    }
}

/** Renders the dialog for whichever [DeckAction] was chosen from the menu. */
@Composable
private fun DeckActionDialogs(
    state: AppState,
    deck: DeckDef,
    action: DeckAction?,
    onClose: () -> Unit,
    onBackToCatalog: () -> Unit
) {
    if (action == null) return
    var mergeTarget by remember { mutableStateOf<DeckDef?>(null) }

    when (action) {
        DeckAction.Rename -> DsPromptDialog(
            title = "Rename deck",
            placeholder = "Deck name",
            initialValue = deck.name,
            onConfirm = { name ->
                state.library.rename(deck.id, name)
                state.activityLog.record(ActivityCategory.Deck, "Renamed deck to \"$name\"")
                state.toastHost.show("Deck renamed", kind = ToastKind.Success)
            },
            onDismiss = onClose
        )

        DeckAction.Edit -> DeckEditDialog(state, deck, onDismiss = onClose)

        DeckAction.Move -> DeckPickerDialog(
            state = state,
            title = "Move \"${deck.name}\"",
            subtitle = "Choose where to move this deck. Decks with children act as folders.",
            decks = state.library.validTargetsFor(deck.id),
            onPick = { destination ->
                state.library.move(deck.id, destination.id)
                state.activityLog.record(ActivityCategory.Deck, "Moved deck \"${deck.name}\" into \"${destination.name}\"")
                state.toastHost.show("Moved into \"${destination.name}\"", kind = ToastKind.Success)
                onClose()
            },
            onDismiss = onClose
        )

        DeckAction.Merge -> {
            val target = mergeTarget
            if (target == null) {
                DeckPickerDialog(
                    state = state,
                    title = "Merge \"${deck.name}\" into…",
                    subtitle = "Cards from \"${deck.name}\" will be combined into the target deck, then this deck is removed.",
                    decks = state.library.validTargetsFor(deck.id),
                    onPick = { destination -> mergeTarget = destination },
                    onDismiss = onClose
                )
            } else {
                DsConfirmDialog(
                    title = "Merge decks?",
                    message = "Combine all cards from \"${deck.name}\" into \"${target.name}\"? \"${deck.name}\" (and any sub-folders) will be deleted.",
                    confirmText = "Merge",
                    onConfirm = {
                        if (state.library.merge(target.id, deck.id)) {
                            state.activityLog.record(ActivityCategory.Deck, "Merged deck \"${deck.name}\" into \"${target.name}\"")
                            state.toastHost.show("Merged into \"${target.name}\"", kind = ToastKind.Success)
                            onBackToCatalog()
                        }
                        onClose()
                    },
                    onDismiss = onClose
                )
            }
        }

        DeckAction.Tags -> DeckTagsDialog(state, deck, onDismiss = onClose)

        DeckAction.Export -> {
            exportDecks(state, listOf(deck))
            onClose()
        }

        DeckAction.Archive -> {
            state.library.toggleArchived(deck.id)
            state.activityLog.record(ActivityCategory.Deck, "${if (deck.archived) "Restored" else "Archived"} deck \"${deck.name}\"")
            state.toastHost.show(
                if (deck.archived) "Deck restored" else "Deck archived",
                kind = ToastKind.Success
            )
            onClose()
            onBackToCatalog()
        }

        DeckAction.Delete -> DsConfirmDialog(
            title = "Delete \"${deck.name}\"?",
            message = "This permanently deletes the deck${if (state.library.childrenOf(deck.id).isNotEmpty()) " and its ${state.library.childrenOf(deck.id).size} sub-folder(s)" else ""}. Cards themselves are kept in your library. This cannot be undone.",
            confirmText = "Delete",
            danger = true,
            onConfirm = {
                state.library.delete(deck.id)
                state.activityLog.record(ActivityCategory.Deck, "Deleted deck \"${deck.name}\"")
                state.toastHost.show("Deck \"${deck.name}\" deleted", kind = ToastKind.Info)
                onClose()
                onBackToCatalog()
            },
            onDismiss = onClose
        )
    }
}

@Composable
private fun DeckEditDialog(state: AppState, deck: DeckDef, onDismiss: () -> Unit) {
    var name by remember(deck.id) { mutableStateOf(deck.name) }
    var description by remember(deck.id) { mutableStateOf(deck.description) }
    var icon by remember(deck.id) { mutableStateOf(deck.icon) }
    var difficulty by remember(deck.id) { mutableStateOf(deck.difficulty) }
    var filterQuery by remember(deck.id) { mutableStateOf(deck.filterQuery) }

    DsDialog(title = "Edit deck", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            DsTextField(value = name, onValueChange = { name = it }, label = "Name")
            DsTextField(value = description, onValueChange = { description = it }, label = "Description")
            DsTextField(value = icon, onValueChange = { icon = it }, label = "Icon glyph", placeholder = "Optional leading character (e.g. 字)")
            DsSelect(
                selected = difficulty,
                options = (1..5).toList(),
                onSelected = { difficulty = it },
                labelOf = { "Difficulty: ${\"★\".repeat(it)}" },
                modifier = Modifier.fillMaxWidth()
            )
            DsTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                label = "Filter query (optional)",
                placeholder = "e.g. jlpt:5 kind:kanji — dynamic membership"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
            ) {
                DsButton(text = "Cancel", kind = DsButtonKind.Ghost, onClick = onDismiss)
                DsButton(
                    text = "Save",
                    enabled = name.isNotBlank(),
                    onClick = {
                        state.library.update(
                            deck.copy(
                                name = name.trim(),
                                description = description.trim(),
                                icon = icon.trim(),
                                difficulty = difficulty,
                                filterQuery = filterQuery.trim()
                            )
                        )
                        state.activityLog.record(ActivityCategory.Deck, "Updated deck \"${deck.name}\"")
                        state.toastHost.show("Deck saved", kind = ToastKind.Success)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun DeckTagsDialog(state: AppState, deck: DeckDef, onDismiss: () -> Unit) {
    val sc = surfaceColors()
    var text by remember(deck.id) { mutableStateOf(deck.tags.joinToString(", ")) }
    val allTags = remember(state.library.revision) {
        state.library.allDecks().flatMap { it.tags }.distinct().sorted()
    }

    DsDialog(title = "Edit tags — ${deck.name}", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            DsTextField(
                value = text,
                onValueChange = { text = it },
                label = "Tags",
                placeholder = "comma separated, e.g. jlpt-n5, review-heavy"
            )
            if (allTags.isNotEmpty()) {
                Text(
                    text = "EXISTING TAGS",
                    color = sc.textMuted,
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
                ) {
                    allTags.forEach { tag ->
                        val active = deck.tags.contains(tag)
                        DsChip(
                            text = tag,
                            selected = active,
                            onClick = {
                                val current = text.split(',').map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
                                if (!current.add(tag)) current.remove(tag)
                                text = current.joinToString(", ")
                            }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
            ) {
                DsButton(text = "Cancel", kind = DsButtonKind.Ghost, onClick = onDismiss)
                DsButton(
                    text = "Save tags",
                    onClick = {
                        val tags = text.split(',').map { it.trim() }.filter { it.isNotBlank() }.distinct()
                        state.library.setTags(deck.id, tags)
                        state.activityLog.record(ActivityCategory.Deck, "Updated tags on \"${deck.name}\" (${tags.size})")
                        state.toastHost.show("Tags updated", kind = ToastKind.Success)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun CreateDeckDialog(state: AppState, onDismiss: () -> Unit, onCreated: (DeckDef) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(ContentKind.Kanji) }
    var difficulty by remember { mutableStateOf(2) }

    DsDialog(title = "New deck", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            Text(
                text = "Create a custom deck. Cards can be added from the browser or via search filters.",
                color = surfaceColors().textSecondary,
                fontSize = DsType.Body
            )
            DsTextField(value = name, onValueChange = { name = it }, label = "Name", placeholder = "My first deck")
            DsTextField(value = description, onValueChange = { description = it }, label = "Description", placeholder = "What are you studying?")
            DsSelect(
                selected = kind,
                options = ContentKind.entries,
                onSelected = { kind = it },
                labelOf = { it.label },
                modifier = Modifier.fillMaxWidth()
            )
            DsSelect(
                selected = difficulty,
                options = (1..5).toList(),
                onSelected = { difficulty = it },
                labelOf = { "Difficulty: ${\"★\".repeat(it)}" },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
            ) {
                DsButton(text = "Cancel", kind = DsButtonKind.Ghost, onClick = onDismiss)
                DsButton(
                    text = "Create deck",
                    enabled = name.isNotBlank(),
                    onClick = {
                        val deck = state.library.create(
                            name = name,
                            description = description,
                            kind = kind,
                            difficulty = difficulty
                        )
                        state.activityLog.record(ActivityCategory.Study, "Created deck \"${deck.name}\"")
                        onCreated(deck)
                    }
                )
            }
        }
    }
}

// ============================================
// UNIVERSAL SEARCH — the Library is the browser
// ============================================

@Composable
private fun LibrarySearchBar(
    state: AppState,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenDeck: (DeckDef) -> Unit,
    onOpenEntry: (DesktopCard) -> Unit
) {
    val sc = surfaceColors()
    var anchor by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var selectedIndex by remember(query) { mutableStateOf(0) }
    val suggestions = remember(query) { state.library.suggestions(state.cards.toList(), query, limit = 14) }

    val applySuggestion: (LibrarySuggestion) -> Unit = { suggestion ->
        when {
            suggestion.action.startsWith("open-deck:") ->
                state.library.deck(suggestion.action.removePrefix("open-deck:"))?.let(onOpenDeck)
            suggestion.action.startsWith("open-entry:") ->
                state.cards.firstOrNull { it.id == suggestion.action.removePrefix("open-entry:") }?.let(onOpenEntry)
            else -> onQueryChange(suggestion.payload.ifBlank { suggestion.title })
        }
        state.library.recordSearch(suggestion.payload.ifBlank { suggestion.title })
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = DsSpacing.Xl, end = DsSpacing.Xl, top = DsSpacing.Lg)
            .onGloballyPositioned { if (anchor != it) anchor = it }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> {
                        if (suggestions.isNotEmpty()) selectedIndex = (selectedIndex + 1) % suggestions.size
                        true
                    }
                    Key.DirectionUp -> {
                        if (suggestions.isNotEmpty()) selectedIndex = (selectedIndex - 1 + suggestions.size) % suggestions.size
                        true
                    }
                    Key.Enter -> {
                        if (query.isNotBlank() && suggestions.isNotEmpty()) {
                            applySuggestion(suggestions[selectedIndex.coerceIn(0, suggestions.lastIndex)])
                        }
                        query.isNotBlank() && suggestions.isNotEmpty()
                    }
                    Key.Escape -> {
                        if (query.isNotBlank()) onQueryChange("")
                        query.isNotBlank()
                    }
                    else -> false
                }
            }
    ) {
        DsSearchField(
            value = query,
            onValueChange = { onQueryChange(it); selectedIndex = 0 },
            placeholder = "Search kanji, vocabulary, grammar, decks, tags…  e.g. 猫 · jlpt:5 · tag:anime"
        )
    }

    if (query.isNotBlank() && anchor != null) {
        val pos = anchor!!.positionInWindow()
        Popup(
            onDismissRequest = {},
            offset = IntOffset(pos.x.roundToInt(), pos.y.roundToInt() + anchor!!.size.height + 4),
            properties = PopupProperties(focusable = false)
        ) {
            Column(
                modifier = Modifier
                    .width(anchor!!.size.width.dp)
                    .clip(RoundedCornerShape(DsRadius.Md))
                    .background(sc.surfaceElevated)
                    .border(1.dp, sc.border, RoundedCornerShape(DsRadius.Md))
                    .padding(DsSpacing.Xs)
            ) {
                if (suggestions.isEmpty()) {
                    Text(
                        text = "No suggestions — keep typing or press Enter",
                        color = sc.textMuted,
                        fontSize = DsType.Caption,
                        modifier = Modifier.padding(DsSpacing.Md)
                    )
                } else {
                    suggestions.forEachIndexed { index, suggestion ->
                        SuggestionRow(
                            suggestion = suggestion,
                            selected = index == selectedIndex,
                            onClick = { applySuggestion(suggestion) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(suggestion: LibrarySuggestion, selected: Boolean, onClick: () -> Unit) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val icon = when (suggestion.kind) {
        "deck" -> Icons.Default.Folder
        "jlpt", "grade" -> Icons.Default.School
        "frequency" -> Icons.Default.BarChart
        "tag" -> Icons.Default.Label
        "recent" -> Icons.Default.History
        "recent-entry" -> Icons.Default.Star
        else -> Icons.Default.GridView
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Sm))
            .background(
                when {
                    selected -> ac.primary.copy(alpha = 0.14f)
                    hovered -> sc.surfaceInteractive
                    else -> Color.Transparent
                }
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) ac.primary else sc.textSecondary, modifier = Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = suggestion.title,
                color = sc.textPrimary,
                fontSize = DsType.Body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (suggestion.subtitle.isNotBlank()) {
                Text(
                    text = suggestion.subtitle,
                    color = sc.textMuted,
                    fontSize = DsType.Caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LibrarySearchResults(
    state: AppState,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenDeck: (DeckDef) -> Unit,
    onOpenEntry: (DesktopCard) -> Unit
) {
    val sc = surfaceColors()
    val cards = state.cards.toList()
    val q = query.trim()
    LaunchedEffect(q) { if (q.isNotBlank()) state.library.recordSearch(q) }

    val deckMatches = remember(state.library.revision, q) {
        state.library.allDecks().filter { deck ->
            deck.name.contains(q, ignoreCase = true) || deck.description.contains(q, ignoreCase = true) ||
                deck.tags.any { it.contains(q, ignoreCase = true) }
        }.take(6)
    }
    val entries = remember(q) { state.library.search(cards, q, limit = 200) }

    Column(Modifier.fillMaxSize().padding(horizontal = DsSpacing.Xl)) {
        Text(
            text = "Results for \"$q\"",
            color = sc.textPrimary,
            fontSize = DsType.Heading,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = DsSpacing.Md)
        )
        if (deckMatches.isNotEmpty()) {
            SectionLabel("DECKS")
            Spacer(Modifier.height(DsSpacing.Xs))
            deckMatches.forEach { deck ->
                val count = state.library.cardsIn(deck, cards).size
                DsCard(onClick = { onOpenDeck(deck) }, modifier = Modifier.fillMaxWidth().padding(bottom = DsSpacing.Sm)) {
                    Row(
                        Modifier.padding(DsSpacing.Md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(DsRadius.Md))
                                .background(accent().primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(deck.icon.ifBlank { deck.kind.glyph }, color = accent().primary, fontSize = DsType.Title, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(deck.name, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
                            Text("${deck.kind.label} · $count cards", color = sc.textMuted, fontSize = DsType.Caption)
                        }
                        if (deck.favorite) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = favoriteColor(), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(DsSpacing.Lg))
        }
        if (entries.isEmpty() && deckMatches.isEmpty()) {
            DsEmptyState(
                title = "Nothing found",
                message = "Try a different term, or a filter like jlpt:3, grade:2, freq:<=500, tag:anime or kind:grammar.",
                modifier = Modifier.fillMaxSize()
            )
        } else if (entries.isNotEmpty()) {
            SectionLabel("ENTRIES (${entries.size})")
            Spacer(Modifier.height(DsSpacing.Xs))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(240.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = DsSpacing.Xl),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
            ) {
                items(entries, key = { it.entry.id }) { result ->
                    EntryCard(state, result.entry, onOpenEntry)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = surfaceColors().textMuted,
        fontSize = DsType.Caption,
        fontWeight = FontWeight.SemiBold
    )
}

// ============================================
// ENTRY DETAIL — the content page for any entry
// ============================================

@Composable
private fun EntryDetail(
    state: AppState,
    card: DesktopCard,
    onBack: () -> Unit,
    onOpenEntry: (DesktopCard) -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()
    val deckId = state.library.deckIdFor(card, state.cards.toList())
    val deck = deckId?.let { state.library.deck(it) }
    val modes = StudyMode.forKind(card.contentKind)
    val related = remember(card.id) {
        val pool = (deck?.let { state.library.cardsIn(it, state.cards.toList()) } ?: state.cards.toList())
            .filter { it.id != card.id }
        val sameJlpt = if (card.jlpt != null) pool.filter { it.jlpt == card.jlpt } else emptyList()
        (sameJlpt + pool).distinctBy { it.id }.take(8)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DsSpacing.Xl, vertical = DsSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsIconButton(icon = Icons.Default.ArrowBack, onClick = onBack, contentDescription = "Back")
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(DsRadius.Md))
                    .background(ac.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(card.character, color = sc.textPrimary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text(card.character, color = sc.textPrimary, fontSize = DsType.Heading, fontWeight = FontWeight.Bold)
                    DsBadge(text = card.contentKind.label, tint = ac.primary)
                    if (card.jlpt != null) DsBadge(text = "JLPT N${card.jlpt}", tint = infoColor())
                    if (card.grade != null) DsBadge(text = "Grade ${card.grade}", tint = sc.textMuted)
                    if (card.frequency != null) DsBadge(text = "#${card.frequency}", tint = warningColor())
                }
                Text(card.meaning, color = sc.textSecondary, fontSize = DsType.BodyLarge)
                if (deck != null) {
                    Text("In deck: ${deck.name}", color = sc.textMuted, fontSize = DsType.Caption)
                }
            }
            DsIconButton(
                icon = if (card.favorite) Icons.Default.Star else Icons.Default.StarBorder,
                onClick = {
                    val idx = state.cards.indexOfFirst { it.id == card.id }
                    if (idx >= 0) {
                        state.cards[idx] = state.cards[idx].copy(favorite = !state.cards[idx].favorite)
                        state.activityLog.record(ActivityCategory.Study, "${if (card.favorite) "Unfavorited" else "Favorited"} ${card.character}")
                    }
                },
                contentDescription = "Favorite",
                tint = if (card.favorite) favoriteColor() else null
            )
            DsButton(
                text = "Dictionary",
                icon = Icons.Default.MenuBook,
                kind = DsButtonKind.Secondary,
                onClick = {
                    state.dictionary.query = card.character
                    state.currentView = WorkspaceView.Dictionary
                }
            )
            DsButton(
                text = "Edit",
                icon = Icons.Default.Edit,
                kind = DsButtonKind.Secondary,
                onClick = { state.openEditor(card) }
            )
        }

        DsCard(modifier = Modifier.padding(horizontal = DsSpacing.Xl, vertical = DsSpacing.Sm)) {
            Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DetailRow("Readings", card.readings.joinToString("、").ifBlank { "—" })
                if (card.onReadings.isNotEmpty() || card.kunReadings.isNotEmpty()) {
                    DetailRow("On", card.onReadings.joinToString("、").ifBlank { "—" })
                    DetailRow("Kun", card.kunReadings.joinToString("、").ifBlank { "—" })
                }
                DetailRow("Radicals", card.radicals.joinToString("、").ifBlank { "—" })
                DetailRow("Components", card.components.joinToString("、").ifBlank { "—" })
                if (card.strokeCount > 0) DetailRow("Strokes", card.strokeCount.toString())
                if (card.note.isNotBlank()) DetailRow("Note", card.note)
                if (card.tags.isNotEmpty()) {
                    Text("TAGS", color = sc.textMuted, fontSize = DsType.Caption, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        card.tags.take(12).forEach { tag -> DsTagChip(label = tag) }
                    }
                }
            }
        }

        // Animated stroke-order playback for characters in the stroke dataset;
        // everything else falls back to a static handwriting reference grid.
        if (StrokeOrderData.sequences.containsKey(card.character)) {
            StrokeOrderPanel(
                character = card.character,
                strokeCount = card.strokeCount,
                modifier = Modifier.padding(horizontal = DsSpacing.Xl, vertical = DsSpacing.Sm),
                onPractice = {
                    if (deck != null) state.startLibraryWriting(deck.id)
                    else state.startWritingPractice(limit = 12, includeNew = true)
                }
            )
        } else {
            DsCard(modifier = Modifier.padding(horizontal = DsSpacing.Xl, vertical = DsSpacing.Sm)) {
                Row(
                    Modifier.padding(DsSpacing.Lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(DsRadius.Md))
                            .background(sc.surfaceElevated)
                            .border(1.dp, sc.border, RoundedCornerShape(DsRadius.Md)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val step = size.width / 5f
                            val gridColor = sc.border.copy(alpha = 0.6f)
                            for (i in 1 until 5) {
                                drawLine(gridColor, Offset(step * i, 0f), Offset(step * i, size.height), strokeWidth = 1f)
                                drawLine(gridColor, Offset(0f, step * i), Offset(size.width, step * i), strokeWidth = 1f)
                            }
                        }
                        Text(card.character, color = sc.textPrimary, fontSize = 64.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                        Text(
                            text = "Stroke order",
                            color = sc.textPrimary,
                            fontSize = DsType.Title,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (card.strokeCount > 0) "${card.strokeCount} strokes" else "Stroke count not available for this entry",
                            color = sc.textSecondary,
                            fontSize = DsType.Body
                        )
                        Text(
                            text = "Practice the character inside the 5×5 reference grid, following the standard stroke direction (top-to-bottom, left-to-right).",
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                        DsButton(
                            text = "Practice writing",
                            icon = Icons.Default.Create,
                            onClick = {
                                if (deck != null) state.startLibraryWriting(deck.id)
                                else state.startWritingPractice(limit = 12, includeNew = true)
                            }
                        )
                    }
                }
            }
        }

        Text(
            text = "Study progress",
            color = sc.textPrimary,
            fontSize = DsType.Title,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = DsSpacing.Xl, vertical = DsSpacing.Sm)
        )
        modes.forEach { mode ->
            EntryModeRow(state, card, deck, mode, state.library.modeProgress(card.id, mode))
        }

        if (related.isNotEmpty()) {
            Text(
                text = "Related entries",
                color = sc.textPrimary,
                fontSize = DsType.Title,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = DsSpacing.Xl, vertical = DsSpacing.Sm)
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(220.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
                    .padding(horizontal = DsSpacing.Xl),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
            ) {
                items(related, key = { it.id }) { relatedCard ->
                    DsCard(onClick = { onOpenEntry(relatedCard) }) {
                        Column(Modifier.padding(DsSpacing.Md)) {
                            Text(relatedCard.character, color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.Bold)
                            Text(relatedCard.meaning, color = sc.textMuted, fontSize = DsType.Caption, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(DsSpacing.Xl))
    }
}

@Composable
private fun EntryModeRow(
    state: AppState,
    card: DesktopCard,
    deck: DeckDef?,
    mode: StudyMode,
    p: StudyModeProgress
) {
    val sc = surfaceColors()
    val ac = accent()
    val fraction = (p.reps / 10f).coerceIn(0f, 1f)
    val statusText = when {
        p.isSuspended -> "Suspended"
        p.isCompleted -> "Mastered"
        p.isDue -> "Due"
        p.totalReviews > 0 -> "Learning"
        else -> "New"
    }
    DsCard(modifier = Modifier.padding(horizontal = DsSpacing.Xl).padding(bottom = DsSpacing.Sm)) {
        Row(Modifier.padding(DsSpacing.Lg), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(DsRadius.Md))
                    .background(ac.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(mode.glyph, color = ac.primary, fontSize = DsType.Title, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(DsSpacing.Md))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text(mode.label, color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                    DsBadge(
                        text = statusText,
                        tint = when {
                            p.isSuspended -> warningColor()
                            p.isCompleted -> successColor()
                            p.isDue -> dueColor()
                            else -> sc.textMuted
                        }
                    )
                }
                Text(mode.hint, color = sc.textMuted, fontSize = DsType.Caption)
                Spacer(Modifier.height(DsSpacing.Sm))
                DsProgressBar(fraction = fraction, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(DsSpacing.Xs))
                Text(
                    text = "${p.reps} reps · ${(p.accuracy * 100).toInt()}% accuracy · streak ${p.streak} · interval ${p.intervalDays.toInt()}d",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
            Spacer(Modifier.width(DsSpacing.Md))
            DsButton(
                text = if (p.isDue) "Study" else "Start",
                icon = Icons.Default.PlayArrow,
                enabled = deck != null && !p.isSuspended,
                onClick = {
                    if (deck != null) {
                        if (mode == StudyMode.Writing) state.startLibraryWriting(deck.id)
                        else state.startLibraryStudy(deck.id, mode)
                    }
                }
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val sc = surfaceColors()
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        Text(
            text = label,
            color = sc.textMuted,
            fontSize = DsType.Caption,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(96.dp)
        )
        Text(
            text = value,
            color = sc.textSecondary,
            fontSize = DsType.Body,
            modifier = Modifier.weight(1f)
        )
    }
}

// ============================================
// DECK ENTRIES — browse every card in a deck
// ============================================

@Composable
private fun DeckEntriesView(
    state: AppState,
    deck: DeckDef,
    onBack: () -> Unit,
    onOpenEntry: (DesktopCard) -> Unit
) {
    val sc = surfaceColors()
    var q by remember { mutableStateOf("") }
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }
    var moveOpen by remember { mutableStateOf(false) }
    var tagOpen by remember { mutableStateOf(false) }
    var rescheduleOpen by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf(false) }

    val members = remember(state.library.revision, q) {
        state.library.cardsIn(deck, state.cards.toList())
            .filter { q.isBlank() || SearchEngine.matches(it, q) }
            .sortedBy { it.character }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DsSpacing.Xl, vertical = DsSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            DsIconButton(icon = Icons.Default.ArrowBack, onClick = onBack, contentDescription = "Back to deck")
            Text(deck.icon.ifBlank { deck.kind.glyph }, color = accent().primary, fontSize = DsType.Heading, fontWeight = FontWeight.Bold)
            Text(
                text = deck.name,
                color = sc.textPrimary,
                fontSize = DsType.Heading,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            DsButton(
                text = if (selectionMode) "Done" else "Select",
                icon = if (selectionMode) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                kind = DsButtonKind.Ghost,
                onClick = {
                    selectionMode = !selectionMode
                    if (!selectionMode) selectedIds.clear()
                }
            )
            DsSearchField(
                value = q,
                onValueChange = { q = it },
                placeholder = "Filter ${members.size} cards…",
                modifier = Modifier.width(300.dp)
            )
        }

        if (selectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DsSpacing.Xl, vertical = DsSpacing.Sm)
                    .clip(RoundedCornerShape(DsRadius.Md))
                    .background(sc.surfaceElevated)
                    .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
            ) {
                Text(
                    text = "${selectedIds.size} selected",
                    color = sc.textPrimary,
                    fontSize = DsType.Label,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                DsButton(
                    text = "Select all",
                    kind = DsButtonKind.Ghost,
                    compact = true,
                    onClick = {
                        selectedIds.clear()
                        members.forEach { selectedIds.add(it.id) }
                    }
                )
                DsButton(
                    text = "Move to deck",
                    icon = Icons.Default.Folder,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    enabled = selectedIds.isNotEmpty(),
                    onClick = { moveOpen = true }
                )
                DsButton(
                    text = "Tag",
                    icon = Icons.Default.Label,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    enabled = selectedIds.isNotEmpty(),
                    onClick = { tagOpen = true }
                )
                DsButton(
                    text = "Reschedule",
                    icon = Icons.Default.Schedule,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    enabled = selectedIds.isNotEmpty(),
                    onClick = { rescheduleOpen = true }
                )
                DsButton(
                    text = "Favorite",
                    icon = Icons.Default.Star,
                    kind = DsButtonKind.Secondary,
                    compact = true,
                    enabled = selectedIds.isNotEmpty(),
                    onClick = {
                        selectedIds.toList().forEach { id ->
                            val idx = state.cards.indexOfFirst { it.id == id }
                            if (idx >= 0) state.cards[idx] = state.cards[idx].copy(favorite = true)
                        }
                        state.activityLog.record(ActivityCategory.Study, "Favorited ${selectedIds.size} cards")
                        state.toastHost.show("Favorited ${selectedIds.size} cards", kind = ToastKind.Success)
                        selectedIds.clear()
                    }
                )
                DsButton(
                    text = "Remove from deck",
                    kind = DsButtonKind.Ghost,
                    compact = true,
                    enabled = selectedIds.isNotEmpty(),
                    onClick = {
                        state.library.removeCards(deck.id, selectedIds.toList())
                        state.activityLog.record(ActivityCategory.Deck, "Removed ${selectedIds.size} cards from \"${deck.name}\"")
                        state.toastHost.show("Removed ${selectedIds.size} cards from \"${deck.name}\"", kind = ToastKind.Success)
                        selectedIds.clear()
                    }
                )
                DsButton(
                    text = "Delete",
                    icon = Icons.Default.Delete,
                    kind = DsButtonKind.Danger,
                    compact = true,
                    enabled = selectedIds.isNotEmpty(),
                    onClick = { deleteConfirm = true }
                )
            }
        }

        if (members.isEmpty()) {
            DsEmptyState(
                title = "No cards in this deck",
                message = if (q.isNotBlank()) "Nothing matches \"$q\" in this deck." else "Import content or add cards to get started.",
                icon = Icons.Default.GridView,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(240.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = DsSpacing.Xl, end = DsSpacing.Xl, bottom = DsSpacing.Xl),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
            ) {
                items(members, key = { it.id }) { card ->
                    EntryCard(
                        state = state,
                        card = card,
                        onOpenEntry = onOpenEntry,
                        selectionMode = selectionMode,
                        selected = card.id in selectedIds,
                        onToggleSelect = {
                            if (card.id in selectedIds) selectedIds.remove(card.id) else selectedIds.add(card.id)
                        }
                    )
                }
            }
        }
    }

    if (moveOpen) {
        DeckPickerDialog(
            state = state,
            title = "Move ${selectedIds.size} cards to…",
            subtitle = "The selected cards will leave \"${deck.name}\" and join the destination deck.",
            decks = state.library.allDecks().filter { it.id != deck.id },
            onPick = { target ->
                state.library.moveCards(deck.id, target.id, selectedIds.toList())
                state.activityLog.record(ActivityCategory.Deck, "Moved ${selectedIds.size} cards from \"${deck.name}\" to \"${target.name}\"")
                state.toastHost.show("Moved ${selectedIds.size} cards to \"${target.name}\"", kind = ToastKind.Success)
                selectedIds.clear()
            },
            onDismiss = { moveOpen = false }
        )
    }
    if (tagOpen) {
        DsPromptDialog(
            title = "Add tag to ${selectedIds.size} cards",
            placeholder = "tag name",
            onConfirm = { tagName ->
                val tag = tagName.trim()
                if (tag.isNotBlank()) {
                    selectedIds.toList().forEach { id ->
                        val idx = state.cards.indexOfFirst { it.id == id }
                        if (idx >= 0 && !state.cards[idx].tags.contains(tag)) {
                            state.cards[idx] = state.cards[idx].copy(tags = state.cards[idx].tags + tag)
                        }
                    }
                    state.activityLog.record(ActivityCategory.Study, "Tagged ${selectedIds.size} cards with #$tag")
                    state.toastHost.show("Tagged ${selectedIds.size} cards with #$tag", kind = ToastKind.Success)
                }
            },
            onDismiss = { tagOpen = false }
        )
    }
    if (rescheduleOpen) {
        var mode by remember { mutableStateOf(StudyMode.Flashcards) }
        var days by remember { mutableStateOf(1) }
        DsDialog(title = "Reschedule ${selectedIds.size} cards", onDismiss = { rescheduleOpen = false }) {
            Text(
                text = "Push the due date of the selected cards' \"${mode.label}\" track. Every other study mode keeps its own schedule untouched.",
                color = sc.textSecondary,
                fontSize = DsType.Body
            )
            Spacer(Modifier.height(DsSpacing.Lg))
            DsSelect(
                selected = mode,
                options = StudyMode.forKind(deck.kind),
                onSelected = { mode = it },
                labelOf = { it.label },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(DsSpacing.Md))
            DsNumericField(
                value = days,
                onValueChange = { days = it.coerceIn(0, 3650) },
                label = "Days from now (0 = due immediately)"
            )
            Spacer(Modifier.height(DsSpacing.Xl))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)) {
                DsButton(text = "Cancel", kind = DsButtonKind.Ghost, onClick = { rescheduleOpen = false })
                DsButton(text = "Reschedule", onClick = {
                    selectedIds.toList().forEach { id -> state.library.reschedule(id, mode, days) }
                    state.activityLog.record(
                        ActivityCategory.Study,
                        "Rescheduled ${selectedIds.size} cards (${mode.label}, +$days days)"
                    )
                    state.toastHost.show("Rescheduled ${selectedIds.size} cards", kind = ToastKind.Success)
                    selectedIds.clear()
                    rescheduleOpen = false
                })
            }
        }
    }
    if (deleteConfirm) {
        DsConfirmDialog(
            title = "Delete ${selectedIds.size} cards?",
            message = "These cards are removed from your entire library, including every deck. This cannot be undone.",
            confirmText = "Delete",
            danger = true,
            onConfirm = {
                selectedIds.toList().forEach { id -> state.deleteCard(id) }
                state.activityLog.record(ActivityCategory.Study, "Deleted ${selectedIds.size} cards")
                state.toastHost.show("Deleted ${selectedIds.size} cards", kind = ToastKind.Info)
                selectedIds.clear()
            },
            onDismiss = { deleteConfirm = false }
        )
    }
}
