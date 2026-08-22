package ua.syt0r.kanji.desktop.ui.library

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmarks
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
import androidx.compose.material.icons.filled.ViewList
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
import androidx.compose.ui.input.key.type
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
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
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSelect
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTagChip
import ua.syt0r.kanji.desktop.designsystem.DsTextField
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.dueColor
import ua.syt0r.kanji.desktop.designsystem.favoriteColor
import ua.syt0r.kanji.desktop.designsystem.infoColor
import ua.syt0r.kanji.desktop.designsystem.newColor
import ua.syt0r.kanji.desktop.designsystem.successColor
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.designsystem.warningColor
import ua.syt0r.kanji.desktop.designsystem.DsToggle
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.l10n.resolveSuiteString
import ua.syt0r.kanji.desktop.engine.learning.CardType
import ua.syt0r.kanji.desktop.engine.learning.DeckStudyConfig
import ua.syt0r.kanji.desktop.engine.learning.LearningEngine.UnifiedSearchResult
import ua.syt0r.kanji.desktop.engine.learning.toLearningItemKind
import ua.syt0r.kanji.desktop.engine.search.SearchEngine
import ua.syt0r.kanji.desktop.engine.transfer.TransferFilePicker
import ua.syt0r.kanji.desktop.model.ContentKind
import ua.syt0r.kanji.desktop.model.DeckDef
import ua.syt0r.kanji.desktop.model.DeckExportDto
import ua.syt0r.kanji.desktop.model.DeckModeStats
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.LibrarySearchResult
import ua.syt0r.kanji.desktop.model.LibrarySuggestion
import ua.syt0r.kanji.desktop.model.StudyMode
import ua.syt0r.kanji.desktop.model.StudyModeProgress
import ua.syt0r.kanji.desktop.model.ToastKind
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.HeatmapDayData
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.HeatmapDisplayMode
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.StudyHeatmap
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

    /** A collection — the Library's top-level container of decks. */
    data class Collection(val def: ua.syt0r.kanji.desktop.model.CollectionDef) : LibraryScope {
        override val label = def.name
        override val count = { state: AppState -> state.collections.resolveDecks(def, state.library).size }
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
    is LibraryScope.Collection -> "collection:${scope.def.id}"
    LibraryScope.DueToday -> "due"
    LibraryScope.New -> "new"
    LibraryScope.Favorites -> "favorites"
    LibraryScope.Recent -> "recent"
    LibraryScope.Archived -> "archived"
}

private fun restoreLibraryScope(name: String, state: AppState): LibraryScope = when {
    name == "all" -> LibraryScope.All
    name == "due" -> LibraryScope.DueToday
    name == "new" -> LibraryScope.New
    name == "favorites" -> LibraryScope.Favorites
    name == "recent" -> LibraryScope.Recent
    name == "archived" -> LibraryScope.Archived
    name.startsWith("kind:") ->
        ContentKind.entries.firstOrNull { it.name == name.removePrefix("kind:") }?.let { LibraryScope.Kind(it) } ?: LibraryScope.All
    name.startsWith("collection:") -> {
        // The collection may have been renamed or deleted since the scope was
        // remembered — resolve by id, falling back to the default scope.
        val id = name.removePrefix("collection:")
        state.collections.collections.firstOrNull { it.id == id }?.let { LibraryScope.Collection(it) } ?: LibraryScope.All
    }
    else -> LibraryScope.All
}

private fun restoreDeckSort(name: String): DeckSort =
    DeckSort.entries.firstOrNull { it.name == name } ?: DeckSort.Name

private fun restoreLibraryView(name: String): String = if (name == "list") "list" else "grid"

// ============================================
// Merged search — one model over decks, legacy
// entries and the unified learning store
// ============================================

private sealed interface MergedResultRow {
    data class Deck(val deck: DeckDef, val count: Int) : MergedResultRow
    data class Store(val result: UnifiedSearchResult) : MergedResultRow
    data class Entry(val card: DesktopCard) : MergedResultRow
}

private data class MergedSearchData(
    val deckMatches: List<DeckDef>,
    val storeResults: List<UnifiedSearchResult>,
    val extraEntries: List<LibrarySearchResult>,
    val rows: List<MergedResultRow>
)

private fun buildMergedSearch(
    state: AppState,
    cards: List<DesktopCard>,
    q: String,
    kind: ua.syt0r.kanji.desktop.engine.learning.LearningItemKind?,
    jlpt: Int?
): MergedSearchData {
    val deckMatches = state.library.allDecks().filter { deck ->
        deck.name.contains(q, ignoreCase = true) || deck.description.contains(q, ignoreCase = true) ||
            deck.tags.any { it.contains(q, ignoreCase = true) }
    }.take(6)

    // Legacy-card matches…
    val entries = state.library.search(cards, q, limit = 200)
    // …and the unified store (synced from the same cards) with real stage/due
    // state. The store covers most content; anything it doesn't (not yet
    // synced) still appears via `extraEntries` — nothing lost, nothing doubled.
    val storeResults = state.learning.search(
        query = q,
        kinds = kind?.let { setOf(it) } ?: emptySet(),
        jlpt = jlpt,
        maxResults = 40
    )
    val storeLegacyIds = storeResults.mapNotNull { result ->
        state.learning.cards.firstOrNull { it.noteId == result.noteId }?.let { storeCard ->
            state.learning.legacyCardsForDeck(storeCard.deckId).firstOrNull { it.id == storeCard.id }?.id
        }
    }.toSet()
    val extraEntries = entries.filter { it.entry.id !in storeLegacyIds }

    val rows = buildList {
        deckMatches.forEach { add(MergedResultRow.Deck(it, state.library.cardsIn(it, cards).size)) }
        storeResults.forEach { add(MergedResultRow.Store(it)) }
        extraEntries.forEach { add(MergedResultRow.Entry(it.entry)) }
    }
    return MergedSearchData(deckMatches, storeResults, extraEntries, rows)
}

/** Maps a unified-store result back to its legacy card, if any. */
private fun storeResultLegacyCard(state: AppState, result: UnifiedSearchResult): DesktopCard? {
    val card = state.learning.cards.firstOrNull { it.noteId == result.noteId && it.cardType == CardType.Recognition }
        ?: state.learning.cards.firstOrNull { it.noteId == result.noteId }
    return card?.let { storeCard ->
        state.learning.legacyCardsForDeck(storeCard.deckId).firstOrNull { it.id == storeCard.id }
    }
}

private fun openMergedRow(
    state: AppState,
    row: MergedResultRow,
    onOpenDeck: (DeckDef) -> Unit,
    onOpenEntry: (DesktopCard) -> Unit
) {
    when (row) {
        is MergedResultRow.Deck -> onOpenDeck(row.deck)
        is MergedResultRow.Store -> storeResultLegacyCard(state, row.result)?.let(onOpenEntry)
        is MergedResultRow.Entry -> onOpenEntry(row.card)
    }
}

@Composable
fun LibraryView(state: AppState) {
    var scope by remember { mutableStateOf(restoreLibraryScope(state.settings.getString("browser.library-scope", "all"), state)) }
    var query by remember { mutableStateOf("") }
    var selectedDeckId by remember { mutableStateOf<String?>(null) }
    var openEntryId by remember { mutableStateOf<String?>(null) }
    var browseDeckId by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    // Kind + JLPT filters for the merged search results (null = all).
    var kindFilter by remember { mutableStateOf<ua.syt0r.kanji.desktop.engine.learning.LearningItemKind?>(null) }
    var jlptFilter by remember { mutableStateOf<Int?>(null) }
    // Arrow-key selection over the merged results list.
    var resultIndex by remember { mutableStateOf(0) }

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

    // Deep link from Home: a collection card lands here scoped to that
    // collection — the Library is the hub. The pending id is consumed so
    // returning to the Library later starts at the last-used scope.
    LaunchedEffect(state.pendingCollectionId) {
        state.pendingCollectionId?.let { id ->
            state.collections.collections.firstOrNull { it.id == id }?.let { def ->
                selectScope(LibraryScope.Collection(def))
            }
            state.pendingCollectionId = null
        }
    }
    val selectedDeck = selectedDeckId?.let { state.library.deck(it) }
    val openEntry = openEntryId?.let { id -> state.cards.firstOrNull { it.id == id } }

    // Main content — one pinned search plus a row of mode chips. Typing
    // swaps the browse surface for merged results (decks + entries + learning
    // store in one list); clearing the query returns to the chip-driven
    // catalog. Searching, filtering and opening entries all happen without
    // leaving the Library.
    Column(Modifier.fillMaxSize()) {
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
            else -> {
                val q = query.trim()
                val merged = remember(state.library.revision, state.learning.revision, q, kindFilter, jlptFilter) {
                    if (q.isBlank()) null else buildMergedSearch(state, state.cards.toList(), q, kindFilter, jlptFilter)
                }
                LibrarySearchBar(
                    state = state,
                    query = query,
                    onQueryChange = { query = it; resultIndex = 0 },
                    resultRows = merged?.rows ?: emptyList(),
                    resultIndex = resultIndex,
                    onResultIndexChange = { resultIndex = it },
                    onOpenDeck = { selectedDeckId = it.id; query = "" },
                    onOpenEntry = { openEntryId = it.id; query = "" }
                )
                if (query.isNotBlank() && merged != null) {
                    // Merged results — one keyboard-navigable list across
                    // decks, legacy entries and the unified learning store.
                    LibrarySearchResults(
                        state = state,
                        query = query,
                        data = merged,
                        selectedIndex = resultIndex,
                        kind = kindFilter,
                        onKindChange = { kindFilter = it; resultIndex = 0 },
                        jlpt = jlptFilter,
                        onJlptChange = { jlptFilter = it; resultIndex = 0 },
                        onOpenDeck = { selectedDeckId = it.id; query = "" },
                        onOpenEntry = { openEntryId = it.id; query = "" }
                    )
                } else {
                    ModeChipBar(state = state, scope = scope, onSelect = selectScope)
                    // Collections are the Library's top-level organization —
                    // the learning hub starts with the containers, not a flat
                    // list of deck cards.
                    if (scope is LibraryScope.All) {
                        CollectionsStrip(
                            state = state,
                            onOpen = { selectScope(LibraryScope.Collection(it)) }
                        )
                    }
                    // Unified deck statistics — per-deck new/learning/review/due
                    // counts straight from the learning store's card state.
                    UnifiedDeckStatsSection(state)
                    DeckCatalog(
                        state = state,
                        scope = scope,
                        onOpen = { selectedDeckId = it.id },
                        onOpenEntry = { openEntryId = it.id },
                        onCreate = { showCreate = true }
                    )
                }
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

// Mode chips — the same All · Decks · Kanji · Vocabulary · … pattern as the
// core Library, replacing the old side rail. One tap switches the browse
// surface in place; every chip shows a live count.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModeChipBar(state: AppState, scope: LibraryScope, onSelect: (LibraryScope) -> Unit) {
    val allScopes = buildList {
        // When a collection is open it leads the chips (one tap back to All).
        if (scope is LibraryScope.Collection) add(scope)
        add(LibraryScope.All)
        addAll(ContentKind.entries.map { LibraryScope.Kind(it) })
        add(LibraryScope.DueToday)
        add(LibraryScope.New)
        add(LibraryScope.Favorites)
        add(LibraryScope.Recent)
        add(LibraryScope.Archived)
    }
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = DsSpacing.Xl, end = DsSpacing.Xl, top = DsSpacing.Md),
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        allScopes.forEach { candidate ->
            ScopeChip(
                label = candidate.label,
                count = candidate.count(state),
                selected = scope == candidate,
                onClick = { onSelect(candidate) }
            )
        }
    }
}

@Composable
private fun ScopeChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(DsRadius.Full))
            .background(
                when {
                    selected -> ac.primary.copy(alpha = 0.16f)
                    hovered -> sc.surfaceInteractive
                    else -> sc.surfaceElevated
                }
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = DsSpacing.Md, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) ac.primary else sc.textSecondary,
            fontSize = DsType.Body,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = count.toString(),
            color = if (selected) ac.primary.copy(alpha = 0.8f) else sc.textMuted,
            fontSize = DsType.Caption
        )
    }
}

// ============================================
// COLLECTIONS STRIP
// The Library's top-level organization: a row of
// collection cards (the containers) above the deck
// catalog. Each card shows its deck/card counts and
// live due workload; opening one scopes the catalog
// to that collection's decks.
// ============================================

@Composable
private fun CollectionsStrip(
    state: AppState,
    onOpen: (ua.syt0r.kanji.desktop.model.CollectionDef) -> Unit
) {
    val sc = surfaceColors()
    val collections = state.collections.childrenOf(null).filter { !it.archived }
    if (collections.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = DsSpacing.Xl, end = DsSpacing.Xl, top = DsSpacing.Md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = resolveSuiteString { collectionsButton },
                color = sc.textPrimary,
                fontSize = DsType.BodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "open one to see its decks",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
        Spacer(Modifier.height(DsSpacing.Sm))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
        ) {
            collections.forEach { def ->
                CollectionStripCard(state = state, def = def, onOpen = onOpen)
            }
        }
    }
}

@Composable
private fun CollectionStripCard(
    state: AppState,
    def: ua.syt0r.kanji.desktop.model.CollectionDef,
    onOpen: (ua.syt0r.kanji.desktop.model.CollectionDef) -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()
    val now = kotlinx.datetime.Clock.System.now()
    val decks = state.collections.resolveDecks(def, state.library)
    val cards = state.collections.resolveCards(def, state.cards.toList(), state.library)
    val due = decks.sumOf { state.library.deckStats(it, state.cards.toList(), now).anyDue } +
        cards.count {
            it.dueAt != null && it.dueAt <= now && it.status != ua.syt0r.kanji.desktop.model.SrsStatus.New
        }

    DsCard(
        modifier = Modifier.width(248.dp),
        onClick = { onOpen(def) }
    ) {
        Column(
            modifier = Modifier.padding(DsSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(DsRadius.Md))
                        .background(ac.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Bookmarks, null, tint = ac.primary, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(DsSpacing.Sm))
                Text(
                    text = def.name,
                    color = sc.textPrimary,
                    fontSize = DsType.Body,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = "${decks.size} decks · ${cards.size} cards",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                if (due > 0) DsBadge(text = "$due due", tint = dueColor())
                if (def.kind == ua.syt0r.kanji.desktop.model.CollectionKind.Smart) {
                    DsBadge(text = "Smart", tint = Color(0xFFA78BFA))
                }
            }
            if (cards.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                DsButton(
                    text = resolveSuiteString { studyAction },
                    icon = Icons.Default.PlayArrow,
                    compact = true,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { state.startReview(collection = def) }
                )
            }
        }
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
    var viewMode by remember(scope) { mutableStateOf(restoreLibraryView(state.settings.getString("browser.library-view", "grid"))) }
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
            is LibraryScope.Collection -> state.collections.resolveDecks(scope.def, state.library)
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
                    LibraryScope.All -> resolveSuiteString { allDecksLabel }
                    else -> scope.label
                },
                color = sc.textPrimary,
                fontSize = DsType.Heading,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            DsIconButton(
                icon = Icons.Default.GridView,
                onClick = { viewMode = "grid"; state.settings.set("browser.library-view", viewMode) },
                contentDescription = resolveSuiteString { gridViewDesc },
                tint = if (viewMode == "grid") accent().primary else sc.textMuted
            )
            DsIconButton(
                icon = Icons.Default.ViewList,
                onClick = { viewMode = "list"; state.settings.set("browser.library-view", viewMode) },
                contentDescription = resolveSuiteString { listViewDesc },
                tint = if (viewMode == "list") accent().primary else sc.textMuted
            )
            DsSelect(
                selected = sortMode,
                options = DeckSort.entries.toList(),
                onSelected = { sortMode = it; state.settings.set("browser.library-sort", it.name) },
                labelOf = { it.label },
                modifier = Modifier.width(140.dp)
            )
            DsButton(
                text = resolveSuiteString { newFolderButton },
                icon = Icons.Default.Folder,
                onClick = { createFolder = true },
                kind = DsButtonKind.Secondary
            )
            DsButton(
                text = resolveSuiteString { newDeckButton },
                icon = Icons.Default.Add,
                onClick = onCreate,
                kind = DsButtonKind.Secondary
            )
            DsButton(
                text = if (selectionMode) resolveSuiteString { exitSelectButton } else resolveSuiteString { selectButton },
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
                    label = resolveSuiteString { favoritesOnlyLabel },
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
                    text = resolveSuiteString { allDecksLabel },
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
                        title = if (folderDeck != null) resolveSuiteString { thisFolderEmpty } else resolveSuiteString { noDecksFound },
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
                                    text = resolveSuiteString { selectAllButton },
                                    kind = DsButtonKind.Ghost,
                                    compact = true,
                                    onClick = {
                                        selectedIds.clear()
                                        decks.forEach { selectedIds.add(it.id) }
                                    }
                                )
                                DsButton(
                                    text = resolveSuiteString { archiveButton },
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
                                    text = resolveSuiteString { exportButton },
                                    icon = Icons.Default.FileDownload,
                                    kind = DsButtonKind.Secondary,
                                    compact = true,
                                    enabled = selectedIds.isNotEmpty(),
                                    onClick = {
                                        exportDecks(state, selectedIds.mapNotNull { state.library.deck(it) })
                                    }
                                )
                                DsButton(
                                    text = resolveSuiteString { deleteButton },
                                    icon = Icons.Default.Delete,
                                    kind = DsButtonKind.Danger,
                                    compact = true,
                                    enabled = selectedIds.isNotEmpty(),
                                    onClick = { bulkDeleteConfirm = true }
                                )
                                DsButton(
                                    text = resolveSuiteString { doneButton },
                                    kind = DsButtonKind.Ghost,
                                    compact = true,
                                    onClick = { selectionMode = false; selectedIds.clear() }
                                )
                            }
                        }
                        val toggleSelect: (DeckDef) -> Unit = { deck ->
                            if (deck.id in selectedIds) selectedIds.remove(deck.id)
                            else selectedIds.add(deck.id)
                        }
                        if (viewMode == "list") {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentPadding = PaddingValues(
                                    start = DsSpacing.Xl, end = DsSpacing.Xl, bottom = DsSpacing.Xl
                                ),
                                verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
                            ) {
                                items(folders, key = { it.id }) { folder ->
                                    DeckListRow(
                                        state = state,
                                        deck = folder,
                                        now = now,
                                        isFolder = true,
                                        selectionMode = selectionMode,
                                        selected = folder.id in selectedIds,
                                        onToggleSelect = { toggleSelect(folder) },
                                        onOpen = {
                                            if (scope is LibraryScope.All) folderPath = folderPath + folder.id
                                            else onOpen(folder)
                                        }
                                    )
                                }
                                items(leafDecks, key = { it.id }) { deck ->
                                    DeckListRow(
                                        state = state,
                                        deck = deck,
                                        now = now,
                                        isFolder = false,
                                        selectionMode = selectionMode,
                                        selected = deck.id in selectedIds,
                                        onToggleSelect = { toggleSelect(deck) },
                                        onOpen = { onOpen(deck) }
                                    )
                                }
                            }
                        } else {
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
                                        onToggleSelect = { toggleSelect(folder) },
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
                                        onToggleSelect = { toggleSelect(deck) },
                                        onOpen = { onOpen(deck) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (createFolder) {
        DsPromptDialog(
            title = resolveSuiteString { newFolderTitle },
            placeholder = resolveSuiteString { folderNamePlaceholder },
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
            title = resolveSuiteString { deleteDecksTitle },
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = DsSpacing.Md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = sc.textPrimary,
                fontSize = DsType.Heading,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (entries.isNotEmpty()) {
                DsButton(
                    text = "${resolveSuiteString { studyTheseLabel }} ${entries.size}",
                    icon = Icons.Default.PlayArrow,
                    compact = true,
                    onClick = {
                        // Exact queue: the very cards shown in this grid, via
                        // the id: filter (id:a OR id:b OR …).
                        val query = entries.take(500).joinToString(" OR ") { "id:${it.id}" }
                        state.startReview(query = query)
                    }
                )
            }
        }
        if (entries.isEmpty()) {
            DsEmptyState(
                title = resolveSuiteString { nothingHereYet },
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
                Spacer(Modifier.weight(1f))
                // Mini heatmap showing last 4 weeks of study activity
                if (!isFolder) {
                    DeckMiniHeatmap(state = state, deckId = deck.id)
                }
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
                    text = if (isFolder) resolveSuiteString { openButton } else resolveSuiteString { studyAction },
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
                        contentDescription = resolveSuiteString { deckActionsDesc },
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

/** Dense single-row deck representation for the Library's list view. */
@Composable
private fun DeckListRow(
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
    val childCount = state.library.childrenOf(deck.id).size
    var manageOpen by remember { mutableStateOf(false) }
    var manageAnchor by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var deckAction by remember { mutableStateOf<DeckAction?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(if (selected) ac.primary.copy(alpha = 0.12f) else sc.surface)
            .hoverable(remember { MutableInteractionSource() })
            .clickable { if (selectionMode) onToggleSelect() else onOpen() }
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Icon(
                if (selected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = null,
                tint = if (selected) ac.primary else sc.textMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(DsSpacing.Sm))
        }
        if (isFolder) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = ac.primary, modifier = Modifier.size(18.dp))
        } else {
            Text(
                text = deck.icon.ifBlank { deck.kind.glyph },
                color = ac.primary,
                fontSize = DsType.Title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(28.dp)
            )
        }
        Spacer(Modifier.width(DsSpacing.Md))
        Column(Modifier.weight(1f)) {
            Text(
                text = deck.name,
                color = sc.textPrimary,
                fontSize = DsType.Body,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isFolder && childCount > 0) "Folder · $childCount item${if (childCount == 1) "" else "s"}"
                else deck.description.ifBlank { deck.kind.label },
                color = sc.textMuted,
                fontSize = DsType.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(DsSpacing.Sm))
        DsBadge(text = deck.kind.label, tint = ac.primary)
        if (stats.anyDue > 0) DsBadge(text = "${stats.anyDue} due", tint = dueColor())
        if (stats.anyNew > 0) DsBadge(text = "${stats.anyNew} new", tint = infoColor())
        DsBadge(text = "${stats.total} cards", tint = sc.textMuted)
        Spacer(Modifier.width(DsSpacing.Sm))
        DsButton(
            text = if (isFolder) resolveSuiteString { openButton } else resolveSuiteString { studyAction },
            icon = if (isFolder) Icons.Default.Folder else Icons.Default.PlayArrow,
            compact = true,
            onClick = onOpen
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
    var settingsOpen by remember { mutableStateOf(false) }
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
            DsButton(
                text = "Study settings",
                icon = Icons.Default.Schedule,
                kind = DsButtonKind.Secondary,
                onClick = { settingsOpen = true }
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

        // Study settings — the deck's real queue configuration (limits, steps,
        // intervals, card types) persisted in the unified LearningStore.
        Spacer(Modifier.height(DsSpacing.Lg))
        val studyConfig = state.learning.deckStudyConfig(deck.id)
        DsCard(modifier = Modifier.padding(horizontal = DsSpacing.Xl).fillMaxWidth()) {
            Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsSectionHeader(
                    title = "Study settings",
                    subtitle = "Queue limits, learning steps and intervals used by the study engine",
                    action = {
                        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                            DsButton(
                                text = "Study (unified)",
                                icon = Icons.Default.PlayArrow,
                                compact = true,
                                onClick = {
                                    val totals = state.learning.deckTotals(deck.id)
                                    if (totals.due + totals.new > 0) {
                                        state.startUnifiedDeckReview(deck.id, StudyMode.Flashcards)
                                    } else {
                                        state.toastHost.show("Nothing due in \"${deck.name}\"", kind = ToastKind.Info)
                                    }
                                }
                            )
                            DsButton(
                                text = "Edit settings",
                                icon = Icons.Default.Schedule,
                                kind = DsButtonKind.Secondary,
                                compact = true,
                                onClick = { settingsOpen = true }
                            )
                        }
                    }
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                    SettingsSummaryTile("New / day", studyConfig.dailyNewLimit.toString(), Modifier.weight(1f))
                    SettingsSummaryTile("Reviews / day", studyConfig.dailyReviewLimit.toString(), Modifier.weight(1f))
                    SettingsSummaryTile("Learning steps", studyConfig.learningStepsMinutes.joinToString(", ") { "${it}m" }, Modifier.weight(1f))
                    SettingsSummaryTile("Graduating", "${studyConfig.graduatingIntervalDays}d", Modifier.weight(1f))
                    SettingsSummaryTile("Easy", "${studyConfig.easyIntervalDays}d", Modifier.weight(1f))
                    SettingsSummaryTile("Max interval", "${studyConfig.maximumIntervalDays.toInt()}d", Modifier.weight(1f))
                }
                Text(
                    text = "Card types: " + studyConfig.cardTypesFor(deck.kind.toLearningItemKind()).joinToString(", ") { it.label } +
                        if (studyConfig.enabledCardTypes.isEmpty()) " (per-kind defaults)" else "",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
                val behaviors = listOfNotNull(
                    if (studyConfig.interleaveNewAndReviews) "interleaves new & reviews" else null,
                    if (studyConfig.buryRelatedNew) "buries related new" else null,
                    if (studyConfig.buryRelatedReviews) "buries related reviews" else null,
                    if (studyConfig.suspendOnLapse) "suspends on lapse" else null
                )
                Text(
                    text = if (behaviors.isEmpty()) "No extra bury/suspend behavior" else behaviors.joinToString(" · "),
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
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
    if (settingsOpen) {
        DeckStudySettingsDialog(state, deck, onDismiss = { settingsOpen = false })
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

private enum class DeckAction { Rename, Edit, Settings, AddToCollection, Move, Merge, Tags, Export, Archive, Delete }

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
            item = DsMenuItem(label = "Study settings…", icon = Icons.Default.Schedule, onAction = {}),
            onClick = { onAction(DeckAction.Settings); onDismiss() }
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
        DsMenuItemRow(
            item = DsMenuItem(label = "Add to collection…", icon = Icons.Default.Bookmarks, onAction = {}),
            onClick = { onAction(DeckAction.AddToCollection); onDismiss() }
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

        DeckAction.Settings -> DeckStudySettingsDialog(state, deck, onDismiss = onClose)

        DeckAction.AddToCollection -> AddToCollectionDialog(state, deck, onDismiss = onClose)

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

/** Choose which collections own this deck (membership, not copies). */
@Composable
private fun AddToCollectionDialog(state: AppState, deck: DeckDef, onDismiss: () -> Unit) {
    val sc = surfaceColors()
    val ac = accent()
    val collections = state.collections.collections.filter { !it.archived }

    DsDialog(title = "Add \"${deck.name}\" to collections", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Text(
                text = "Toggle membership — the deck stays canonical in the Library; each collection just references it.",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
            if (collections.isEmpty()) {
                Text(
                    text = "No collections yet — create one from the Collections workspace first.",
                    color = sc.textMuted,
                    fontSize = DsType.Body
                )
            }
            collections.forEach { def ->
                val owned = state.collections.ownsDeck(def.id, deck.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(DsRadius.Md))
                        .background(sc.surfaceInteractive.copy(alpha = 0.4f))
                        .clickable {
                            if (owned) {
                                state.collections.removeDeck(def.id, deck.id)
                                state.toastHost.show("Removed from '${def.name}'", kind = ToastKind.Info)
                            } else {
                                state.collections.addDeck(def.id, deck.id)
                                state.toastHost.show("Added to '${def.name}'", kind = ToastKind.Success)
                            }
                        }
                        .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = def.name,
                        color = sc.textPrimary,
                        fontSize = DsType.Body,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    DsBadge(
                        text = "${state.collections.resolveDecks(def, state.library).size} decks",
                        tint = sc.textMuted
                    )
                    Spacer(Modifier.width(DsSpacing.Sm))
                    Icon(
                        if (owned) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = if (owned) ac.primary else sc.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(DsSpacing.Sm))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)) {
                DsButton(text = "Done", kind = DsButtonKind.Ghost, onClick = onDismiss)
            }
        }
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
                labelOf = { "Difficulty: ${"★".repeat(it)}" },
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

/** Compact label/value pair used by the deck study-settings summary. */
@Composable
private fun SettingsSummaryTile(label: String, value: String, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    Column(modifier) {
        Text(value, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
        Text(label, color = sc.textMuted, fontSize = DsType.Caption)
    }
}

/**
 * Per-deck study settings editor. Every field maps to the persisted
 * DeckStudyConfig in the unified LearningStore — limits, learning steps,
 * intervals, bury/suspend behavior and the enabled card types. Empty card
 * types mean the per-kind defaults.
 */
@Composable
private fun DeckStudySettingsDialog(state: AppState, deck: DeckDef, onDismiss: () -> Unit) {
    val sc = surfaceColors()
    val existing = state.learning.deckStudyConfig(deck.id)
    var dailyNew by remember(deck.id) { mutableStateOf(existing.dailyNewLimit) }
    var dailyReview by remember(deck.id) { mutableStateOf(existing.dailyReviewLimit) }
    var stepsText by remember(deck.id) { mutableStateOf(existing.learningStepsMinutes.joinToString(", ")) }
    var graduatingText by remember(deck.id) { mutableStateOf(existing.graduatingIntervalDays.toString()) }
    var easyText by remember(deck.id) { mutableStateOf(existing.easyIntervalDays.toString()) }
    var maxIntervalText by remember(deck.id) { mutableStateOf(existing.maximumIntervalDays.toInt().toString()) }
    var interleave by remember(deck.id) { mutableStateOf(existing.interleaveNewAndReviews) }
    var buryNew by remember(deck.id) { mutableStateOf(existing.buryRelatedNew) }
    var buryReviews by remember(deck.id) { mutableStateOf(existing.buryRelatedReviews) }
    var suspendLapse by remember(deck.id) { mutableStateOf(existing.suspendOnLapse) }
    var enabledTypes by remember(deck.id) { mutableStateOf(existing.enabledCardTypes) }

    DsDialog(title = "Study settings — ${deck.name}", onDismiss = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            DsNumericField(value = dailyNew, onValueChange = { dailyNew = it }, label = "New cards / day")
            DsNumericField(value = dailyReview, onValueChange = { dailyReview = it }, label = "Reviews / day")
            DsTextField(
                value = stepsText,
                onValueChange = { stepsText = it },
                label = "Learning steps (minutes, comma separated)",
                placeholder = "e.g. 1, 10"
            )
            DsTextField(value = graduatingText, onValueChange = { graduatingText = it }, label = "Graduating interval (days)")
            DsTextField(value = easyText, onValueChange = { easyText = it }, label = "Easy interval (days)")
            DsTextField(value = maxIntervalText, onValueChange = { maxIntervalText = it }, label = "Maximum interval (days)")
            DsToggle(checked = interleave, onCheckedChange = { interleave = it }, label = "Interleave new cards and reviews")
            DsToggle(checked = buryNew, onCheckedChange = { buryNew = it }, label = "Bury related new cards")
            DsToggle(checked = buryReviews, onCheckedChange = { buryReviews = it }, label = "Bury related reviews")
            DsToggle(checked = suspendLapse, onCheckedChange = { suspendLapse = it }, label = "Suspend cards that lapse")
            Text(
                text = "Enabled card types",
                color = sc.textSecondary,
                fontSize = DsType.Label,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Empty = per-kind defaults (${CardType.defaultsFor(deck.kind.toLearningItemKind()).joinToString(", ") { it.label }})",
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
            ) {
                CardType.entries.forEach { type ->
                    val selected = type in enabledTypes
                    DsChip(
                        text = type.label,
                        selected = selected,
                        onClick = {
                            enabledTypes = if (selected) enabledTypes - type else enabledTypes + type
                        }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
            ) {
                DsButton(text = "Cancel", kind = DsButtonKind.Ghost, onClick = onDismiss)
                DsButton(
                    text = "Save settings",
                    onClick = {
                        val steps = stepsText.split(',').mapNotNull { it.trim().toLongOrNull() }.filter { it > 0 }
                        state.learning.saveDeckStudyConfig(
                            DeckStudyConfig(
                                deckId = deck.id,
                                dailyNewLimit = dailyNew.coerceAtLeast(0),
                                dailyReviewLimit = dailyReview.coerceAtLeast(0),
                                learningStepsMinutes = steps.ifEmpty { listOf(1L, 10L) },
                                graduatingIntervalDays = graduatingText.toDoubleOrNull()?.coerceAtLeast(0.1) ?: existing.graduatingIntervalDays,
                                easyIntervalDays = easyText.toDoubleOrNull()?.coerceAtLeast(0.1) ?: existing.easyIntervalDays,
                                maximumIntervalDays = maxIntervalText.toDoubleOrNull()?.coerceAtLeast(1.0) ?: existing.maximumIntervalDays,
                                buryRelatedNew = buryNew,
                                buryRelatedReviews = buryReviews,
                                suspendOnLapse = suspendLapse,
                                enabledCardTypes = enabledTypes,
                                interleaveNewAndReviews = interleave
                            )
                        )
                        state.activityLog.record(ActivityCategory.Deck, "Updated study settings for \"${deck.name}\"")
                        state.toastHost.show("Study settings saved", kind = ToastKind.Success)
                        onDismiss()
                    }
                )
            }
        }
    }
}

// ============================================
// UNIFIED LEARNING SEARCH
// One search over the unified learning store — the same notes exams,
// mistakes and statistics read from. Every result carries real stage/due
// state, and clicking an entry opens its card in the browser.
// ============================================

// ============================================
// UNIFIED DECK STATISTICS
// Per-deck SRS totals straight from the unified store — the same state
// exams, mistakes and statistics read from. Lists decks that actually
// have unified cards, with real stage counts.
// ============================================

@Composable
private fun UnifiedDeckStatsSection(state: AppState) {
    val sc = surfaceColors()
    val ac = accent()
    val now = Clock.System.now()
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val deckIds = remember(state.learning.revision) {
        state.learning.cards.map { it.deckId }.distinct().sorted()
    }

    // ── Heatmap data from study summaries ──
    val heatmapData = remember(state.summaries) {
        state.summaries.map { s ->
            val date = kotlinx.datetime.LocalDate.parse(s.day)
            HeatmapDayData(date = date, count = s.newCount + s.reviewCount)
        }
    }

    // ── Stats ──
    val totalCards = state.cards.size
    val dueToday = state.dueCount(now)
    val mastered = state.masteredCount()
    val totalReviews = state.totalReviews()
    val studyTime = state.totalStudyTime()
    val streakDays = remember(state.summaries) {
        var streak = 0
        var checkDate = today
        while (true) {
            val summary = state.summaries.firstOrNull { it.day == checkDate.toString() }
            if (summary != null && (summary.newCount + summary.reviewCount) > 0) {
                streak++
                checkDate = checkDate.minus(1, kotlinx.datetime.DateTimeUnit.DAY)
            } else break
        }
        streak
    }
    val masteryPct = if (totalCards == 0) 0f else mastered.toFloat() / totalCards

    // ── Recent activity ──
    val recentActivity = remember(state.activityLog.entries) {
        state.activityLog.entries.take(8)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        // ── Stats row ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            LibraryStatCard(
                icon = Icons.Default.School,
                label = "Total cards",
                value = "$totalCards",
                tint = ac.primary
            )
            LibraryStatCard(
                icon = Icons.Default.Schedule,
                label = "Due today",
                value = "$dueToday",
                tint = dueColor()
            )
            LibraryStatCard(
                icon = Icons.Default.Star,
                label = "Mastered",
                value = "$mastered",
                tint = successColor(),
                subtitle = "${(masteryPct * 100).toInt()}%"
            )
            LibraryStatCard(
                icon = Icons.Default.LocalFireDepartment,
                label = "Streak",
                value = "$streakDays days",
                tint = Color(0xFFFFB300)
            )
            LibraryStatCard(
                icon = Icons.Default.AccessTime,
                label = "Study time",
                value = state.formatDuration(studyTime),
                tint = infoColor()
            )
            LibraryStatCard(
                icon = Icons.Default.Refresh,
                label = "Reviews",
                value = "$totalReviews",
                tint = ac.secondary
            )
        }

        // ── Heatmap ──
        StudyHeatmap(
            activityData = heatmapData,
            displayMode = HeatmapDisplayMode.Expanded,
            modifier = Modifier.fillMaxWidth()
        )

        // ── Activity feed + deck summary side by side ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            // Activity feed
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, null, tint = ac.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(DsSpacing.Sm))
                        Text(
                            "Recent Activity",
                            color = sc.textPrimary,
                            fontSize = DsType.BodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (recentActivity.isEmpty()) {
                        Text(
                            "No activity yet — start studying to see your progress here.",
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    } else {
                        recentActivity.forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                            ) {
                                val categoryIcon = when (entry.category) {
                                    ua.syt0r.kanji.desktop.engine.history.ActivityCategory.Review -> Icons.Default.Refresh
                                    ua.syt0r.kanji.desktop.engine.history.ActivityCategory.Import -> Icons.Default.FileDownload
                                    ua.syt0r.kanji.desktop.engine.history.ActivityCategory.Deck -> Icons.Default.Folder
                                    ua.syt0r.kanji.desktop.engine.history.ActivityCategory.Study -> Icons.Default.School
                                    ua.syt0r.kanji.desktop.engine.history.ActivityCategory.Export -> Icons.Default.ContentCopy
                                    else -> Icons.Default.History
                                }
                                Icon(
                                    categoryIcon, null,
                                    tint = sc.textMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = entry.summary,
                                        color = sc.textSecondary,
                                        fontSize = DsType.Caption,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = formatRelativeTime(entry.timestamp),
                                    color = sc.textMuted,
                                    fontSize = DsType.Caption
                                )
                            }
                        }
                    }
                }
            }

            // Deck summary
            DsCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BarChart, null, tint = ac.secondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(DsSpacing.Sm))
                        Text(
                            "Deck Summary",
                            color = sc.textPrimary,
                            fontSize = DsType.BodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (deckIds.isEmpty()) {
                        Text(
                            "No decks yet — create one to start.",
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    } else {
                        deckIds.take(8).forEach { deckId ->
                            val totals = state.learning.deckTotals(deckId)
                            val deckName = state.library.deck(deckId)?.name ?: deckId
                            Column(Modifier.padding(vertical = DsSpacing.Xs)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(deckName, color = sc.textSecondary, fontSize = DsType.Caption, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    DeckMiniCount("new", totals.new, newColor())
                                    DeckMiniCount("due", totals.due, dueColor())
                                    DeckMiniCount("review", totals.review, successColor())
                                }
                                DsProgressBar(
                                    fraction = if (totals.total == 0) 0f else (totals.review.toFloat() / totals.total).coerceIn(0f, 1f),
                                    modifier = Modifier.padding(top = 2.dp),
                                    color = successColor()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color,
    subtitle: String? = null
) {
    val sc = surfaceColors()
    DsCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(DsSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            Text(
                text = value,
                color = sc.textPrimary,
                fontSize = DsType.Heading,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = tint,
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun formatRelativeTime(timestamp: kotlinx.datetime.Instant): String {
    val now = Clock.System.now()
    val diff = now - timestamp
    val minutes = diff.inWholeMinutes
    val hours = diff.inWholeHours
    val days = diff.inWholeDays
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}

/** Build a 7×4 mini heatmap (last 4 weeks) for a specific deck. */
@Composable
private fun DeckMiniHeatmap(
    state: AppState,
    deckId: String,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val ac = accent()
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    // Collect review counts per day for this deck's cards
    val deckCards = remember(state.library.revision, state.cards.size) {
        val deck = state.library.deck(deckId) ?: return@remember emptyList()
        state.library.cardsIn(deck, state.cards.toList()).map { it.id }.toSet()
    }

    val heatmapData = remember(state.reviewLog.size, deckCards) {
        if (deckCards.isEmpty()) return@remember emptyList()
        state.reviewLog
            .filter { it.cardId in deckCards }
            .groupBy { it.reviewedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date }
            .map { (date, entries) -> HeatmapDayData(date = date, count = entries.size) }
    }

    if (heatmapData.isEmpty()) return

    val countByDate = remember(heatmapData) { heatmapData.associate { it.date to it.count } }
    val maxCount = countByDate.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val cellSize = 6.dp
    val cellGap = 1.5.dp
    val shape = RoundedCornerShape(1.5.dp)

    // Last 28 days (4 weeks)
    val startDate = today.minus(27, DateTimeUnit.DAY)
    val startOffset = startDate.dayOfWeek.isoDayNumber - 1

    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(cellGap)) {
            for (week in 0 until 4) {
                Column(verticalArrangement = Arrangement.spacedBy(cellGap)) {
                    for (dow in 0 until 7) {
                        val dayNum = week * 7 + dow - startOffset
                        val date = if (dayNum in 0..27) {
                            startDate.plus(dayNum.toLong(), DateTimeUnit.DAY)
                        } else null
                        val count = date?.let { countByDate[it] } ?: 0
                        val alpha = if (count <= 0) 0.06f
                        else (0.15f + 0.85f * (count.toFloat() / maxCount)).coerceAtMost(1f)

                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .clip(shape)
                                .background(ac.primary.copy(alpha = if (date != null && date == today) 1f else alpha))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckMiniCount(label: String, count: Int, tint: Color) {
    val sc = surfaceColors()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(
            Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (count > 0) tint else sc.border.copy(alpha = 0.3f))
        )
        Text(
            text = if (count > 0) "$label $count" else label,
            color = if (count > 0) sc.textSecondary else sc.textMuted.copy(alpha = 0.6f),
            fontSize = DsType.Caption
        )
        Spacer(Modifier.width(DsSpacing.Sm))
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
                labelOf = { "Difficulty: ${"★".repeat(it)}" },
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
    resultRows: List<MergedResultRow>,
    resultIndex: Int,
    onResultIndexChange: (Int) -> Unit,
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

    // Once merged results are showing, arrow keys and Enter navigate the live
    // results list (the popup yields); suggestions only take over while there
    // are no full results yet.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = DsSpacing.Xl, end = DsSpacing.Xl, top = DsSpacing.Lg)
            .onGloballyPositioned { if (anchor != it) anchor = it }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> {
                        if (resultRows.isNotEmpty()) {
                            onResultIndexChange((resultIndex + 1) % resultRows.size)
                        } else if (suggestions.isNotEmpty()) {
                            selectedIndex = (selectedIndex + 1) % suggestions.size
                        }
                        true
                    }
                    Key.DirectionUp -> {
                        if (resultRows.isNotEmpty()) {
                            onResultIndexChange((resultIndex - 1 + resultRows.size) % resultRows.size)
                        } else if (suggestions.isNotEmpty()) {
                            selectedIndex = (selectedIndex - 1 + suggestions.size) % suggestions.size
                        }
                        true
                    }
                    Key.Enter -> {
                        if (resultRows.isNotEmpty()) {
                            resultRows.getOrNull(resultIndex)?.let {
                                openMergedRow(state, it, onOpenDeck, onOpenEntry)
                            }
                            true
                        } else if (query.isNotBlank() && suggestions.isNotEmpty()) {
                            applySuggestion(suggestions[selectedIndex.coerceIn(0, suggestions.lastIndex)])
                            true
                        } else {
                            false
                        }
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

    if (query.isNotBlank() && anchor != null && resultRows.isEmpty()) {
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
    data: MergedSearchData,
    selectedIndex: Int,
    kind: ua.syt0r.kanji.desktop.engine.learning.LearningItemKind?,
    onKindChange: (ua.syt0r.kanji.desktop.engine.learning.LearningItemKind?) -> Unit,
    jlpt: Int?,
    onJlptChange: (Int?) -> Unit,
    onOpenDeck: (DeckDef) -> Unit,
    onOpenEntry: (DesktopCard) -> Unit
) {
    val sc = surfaceColors()
    val q = query.trim()
    LaunchedEffect(q) { if (q.isNotBlank()) state.library.recordSearch(q) }

    val deckMatches = data.deckMatches
    val storeResults = data.storeResults
    val extraEntries = data.extraEntries
    val rows = data.rows
    val hasFilters = kind != null || jlpt != null
    val hasContent = deckMatches.isNotEmpty() || storeResults.isNotEmpty() || extraEntries.isNotEmpty()
    val deckCounts = data.rows.filterIsInstance<MergedResultRow.Deck>().associate { it.deck.id to it.count }

    // Keep the arrow-selected row visible as the selection moves.
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex, rows.size) {
        if (rows.isNotEmpty() && selectedIndex in rows.indices) {
            listState.animateScrollToItem(resultRowListIndex(data, selectedIndex))
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = DsSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm),
        contentPadding = PaddingValues(bottom = DsSpacing.Xl)
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text(
                    text = "Results for \"$q\"",
                    color = sc.textPrimary,
                    fontSize = DsType.Heading,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = DsSpacing.Md)
                )
                // Kind + JLPT filters — apply to the learning-store section.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DsChip(text = "All kinds", selected = kind == null, onClick = { onKindChange(null) })
                    ua.syt0r.kanji.desktop.engine.learning.LearningItemKind.entries.forEach { k ->
                        DsChip(text = k.label, selected = kind == k, onClick = { onKindChange(k) })
                    }
                    Spacer(Modifier.width(DsSpacing.Sm))
                    DsChip(text = "All JLPT", selected = jlpt == null, onClick = { onJlptChange(null) })
                    (1..5).forEach { level ->
                        DsChip(text = "N$level", selected = jlpt == level, onClick = { onJlptChange(level) })
                    }
                }
                Text(
                    text = "↑/↓ navigate · Enter open · Esc clear",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }

        if (!hasContent) {
            item(key = "empty") {
                DsEmptyState(
                    title = "Nothing found",
                    message = "Try a different term, or a filter like jlpt:3, grade:2, freq:<=500, tag:anime or kind:grammar.",
                    modifier = Modifier.fillMaxWidth().padding(vertical = DsSpacing.Xl)
                )
            }
        } else {
            if (deckMatches.isNotEmpty()) {
                item(key = "decks-label") { SectionLabel("DECKS") }
                items(deckMatches, key = { "deck-${it.id}" }) { deck ->
                    val rowIndex = rows.indexOfFirst { it is MergedResultRow.Deck && it.deck.id == deck.id }
                    DeckResultRow(
                        deck = deck,
                        count = deckCounts[deck.id] ?: 0,
                        selected = rowIndex == selectedIndex,
                        onClick = { onOpenDeck(deck) }
                    )
                }
            }
            if (storeResults.isNotEmpty() || extraEntries.isNotEmpty()) {
                item(key = "content-label") {
                    SectionLabel(if (hasFilters) "CONTENT — FILTERED" else "CONTENT")
                }
                items(storeResults, key = { "store-${it.noteId}" }) { result ->
                    val rowIndex = rows.indexOfFirst { it is MergedResultRow.Store && it.result.noteId == result.noteId }
                    StoreResultRow(
                        state = state,
                        result = result,
                        selected = rowIndex == selectedIndex,
                        onOpenEntry = onOpenEntry
                    )
                }
                items(extraEntries, key = { "entry-${it.entry.id}" }) { result ->
                    val rowIndex = rows.indexOfFirst { it is MergedResultRow.Entry && it.card.id == result.entry.id }
                    EntryResultRow(
                        state = state,
                        card = result.entry,
                        selected = rowIndex == selectedIndex,
                        onOpenEntry = onOpenEntry
                    )
                }
            }
        }
    }
}

// LazyColumn index of the row for `selectedIndex` in the merged results,
// accounting for the header and per-section labels.
private fun resultRowListIndex(data: MergedSearchData, selectedIndex: Int): Int {
    var index = 1 // item 0 is the header
    if (data.deckMatches.isNotEmpty()) {
        index += 1 // DECKS label
        index += data.deckMatches.size
    }
    if (data.storeResults.isNotEmpty() || data.extraEntries.isNotEmpty()) {
        index += 1 // CONTENT label
        val contentIndex = if (data.deckMatches.isNotEmpty()) selectedIndex - data.deckMatches.size else selectedIndex
        index += contentIndex
    }
    return index
}

@Composable
private fun DeckResultRow(deck: DeckDef, count: Int, selected: Boolean = false, onClick: () -> Unit) {
    val sc = surfaceColors()
    val ac = accent()
    DsCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) Modifier.border(1.5.dp, ac.primary.copy(alpha = 0.6f), RoundedCornerShape(DsRadius.Lg))
                else Modifier
            )
    ) {
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

@Composable
private fun StoreResultRow(state: AppState, result: UnifiedSearchResult, selected: Boolean = false, onOpenEntry: (DesktopCard) -> Unit) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Sm))
            .background(
                when {
                    selected -> ac.primary.copy(alpha = 0.14f)
                    hovered -> sc.surfaceInteractive
                    else -> sc.surfaceElevated
                }
            )
            .hoverable(interaction)
            .clickable {
                val card = state.learning.cards.firstOrNull { it.noteId == result.noteId && it.cardType == CardType.Recognition }
                    ?: state.learning.cards.firstOrNull { it.noteId == result.noteId }
                if (card != null) {
                    state.learning.legacyCardsForDeck(card.deckId).firstOrNull { it.id == card.id }
                        ?.let { onOpenEntry(it) }
                }
            }
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Text(
            text = result.expression,
            color = sc.textPrimary,
            fontSize = DsType.BodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = result.reading,
            color = sc.textSecondary,
            fontSize = DsType.Body,
            modifier = Modifier.width(140.dp)
        )
        Text(
            text = result.meanings.take(2).joinToString("; "),
            color = sc.textSecondary,
            fontSize = DsType.Body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (result.jlpt != null) DsBadge(text = "N${result.jlpt}", tint = accent().primary)
        DsBadge(
            text = result.stage.label,
            tint = when (result.stage) {
                ua.syt0r.kanji.desktop.engine.learning.LearningStage.Mature -> successColor()
                ua.syt0r.kanji.desktop.engine.learning.LearningStage.Established -> Color(0xFF7BC8FF)
                ua.syt0r.kanji.desktop.engine.learning.LearningStage.Learning -> warningColor()
                else -> sc.textMuted
            }
        )
        if (result.due > 0) DsBadge(text = "${result.due} due", tint = dueColor())
    }
}

@Composable
private fun EntryResultRow(state: AppState, card: DesktopCard, selected: Boolean = false, onOpenEntry: (DesktopCard) -> Unit) {
    val sc = surfaceColors()
    val ac = accent()
    val deckId = state.library.deckIdFor(card, state.cards.toList())
    val deck = deckId?.let { state.library.deck(it) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Sm))
            .background(
                when {
                    selected -> ac.primary.copy(alpha = 0.14f)
                    hovered -> sc.surfaceInteractive
                    else -> sc.surfaceElevated
                }
            )
            .hoverable(interaction)
            .clickable { onOpenEntry(card) }
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Text(
            text = card.character,
            color = sc.textPrimary,
            fontSize = DsType.BodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = card.meaning,
            color = sc.textSecondary,
            fontSize = DsType.Body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        DsBadge(text = card.contentKind.label, tint = ac.primary)
        if (deck != null) DsBadge(text = deck.name, tint = sc.textMuted)
        DsBadge(text = card.status.name, tint = sc.textMuted)
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
    var deckPickerOpen by remember { mutableStateOf(false) }
    val containingDecks = remember(state.library.revision, card.id) {
        state.library.decksContaining(card, state.cards.toList())
    }
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

        // Deck membership — every deck containing this entry, with real
        // add/remove actions. Filter decks (built-ins) match by search rule
        // and are shown as such; only explicit membership can be removed here.
        DsCard(modifier = Modifier.padding(horizontal = DsSpacing.Xl, vertical = DsSpacing.Sm)) {
            Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "DECKS",
                        color = sc.textMuted,
                        fontSize = DsType.Caption,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    DsButton(
                        text = "Add to deck",
                        icon = Icons.Default.Add,
                        kind = DsButtonKind.Secondary,
                        compact = true,
                        onClick = { deckPickerOpen = true }
                    )
                }
                if (containingDecks.isEmpty()) {
                    Text(
                        text = "Not in any deck yet — add it to study it alongside related content.",
                        color = sc.textMuted,
                        fontSize = DsType.Body
                    )
                } else {
                    containingDecks.forEach { containing ->
                        val explicit = containing.cardIds.contains(card.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(DsRadius.Md))
                                .background(sc.surfaceElevated)
                                .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                        ) {
                            Text(
                                text = containing.name,
                                color = sc.textPrimary,
                                fontSize = DsType.Body,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (!explicit) DsBadge(text = "auto", tint = sc.textMuted)
                            DsButton(
                                text = "Remove",
                                kind = DsButtonKind.Ghost,
                                compact = true,
                                enabled = explicit,
                                onClick = {
                                    state.library.removeCards(containing.id, listOf(card.id))
                                    state.toastHost.show("Removed ${card.character} from ${containing.name}", kind = ToastKind.Success)
                                    state.activityLog.record(ActivityCategory.Deck, "Removed ${card.character} from ${containing.name}")
                                }
                            )
                        }
                    }
                }
            }
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

    if (deckPickerOpen) {
        DeckPickerDialog(
            state = state,
            title = "Add to deck",
            subtitle = "Choose a deck for \"${card.character}\". Auto (filter) decks match by search rule; explicit membership can be removed from the list above.",
            decks = state.library.allDecks(),
            onPick = { target ->
                deckPickerOpen = false
                state.library.addCards(target.id, listOf(card.id))
                state.toastHost.show("Added ${card.character} to ${target.name}", kind = ToastKind.Success)
                state.activityLog.record(ActivityCategory.Deck, "Added ${card.character} to ${target.name}")
            },
            onDismiss = { deckPickerOpen = false }
        )
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
