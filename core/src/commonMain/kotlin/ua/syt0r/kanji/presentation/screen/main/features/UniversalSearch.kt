package ua.syt0r.kanji.presentation.screen.main.features

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.knowledge.GroupedSearchResults
import ua.syt0r.kanji.core.knowledge.LearnerProfileStore
import ua.syt0r.kanji.core.knowledge.SearchCategory
import ua.syt0r.kanji.core.knowledge.SearchOcrProvider
import ua.syt0r.kanji.core.knowledge.SearchSort
import ua.syt0r.kanji.core.knowledge.SentenceDifficultyScorer
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.core.knowledge.KanjiHit
import ua.syt0r.kanji.core.knowledge.KnowledgeQueryParser
import ua.syt0r.kanji.core.knowledge.KnowledgeSearchEngine
import ua.syt0r.kanji.core.knowledge.KnowledgeSearchQuery
import ua.syt0r.kanji.core.knowledge.WordHit
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors

// ============================================
// UNIVERSAL SEARCH
// Dictionary search everywhere. One overlay,
// reachable from any screen: grouped kanji /
// word / sentence / grammar results, debounced
// against the real knowledge engine.
// ============================================

sealed interface UniversalSearchState {
    data object Idle : UniversalSearchState
    data object Searching : UniversalSearchState
    data class Results(val results: GroupedSearchResults) : UniversalSearchState
    data class Error(val message: String) : UniversalSearchState
}

class UniversalSearchController {

    var isOpen by mutableStateOf(false)
        private set

    var query by mutableStateOf("")
        private set

    var selectedIndex by mutableStateOf(0)
        private set

    /** Active category filters — when empty all categories are searched. */
    var activeCategories by mutableStateOf<Set<SearchCategory>>(emptySet())
        private set

    /**
     * Structured-filter tokens recognized from the plain-text query
     * (KT-SEARCH-002), e.g. ["JLPT:N3", "verb"]. Rendered as chips.
     */
    var activeChips by mutableStateOf<List<String>>(emptyList())
        private set

    /**
     * Active sort (KT-SEARCH-004) — applies to the underlying query.
     * Backing field named [sortState] so the property setter does not
     * clash with the public [setSort] mutator on the JVM.
     */
    var sortState by mutableStateOf(SearchSort.Relevance)
        private set

    fun setSort(value: SearchSort) {
        if (value == sortState) return
        sortState = value
        // Re-run the search so sorting is reflected immediately.
        scope?.let { s -> s.launch { queryFlow.emit(query) } }
    }

    var state by mutableStateOf<UniversalSearchState>(UniversalSearchState.Idle)
        private set

    /**
     * Invoked when the user picks a result. Set by the host (MainScreen)
     * so the overlay itself stays navigation-agnostic.
     */
    var onNavigate: ((MainDestination) -> Unit)? = null

    /**
     * Invoked when the user commits a search (opens a result). Set by the
     * host so the overlay stays storage-agnostic — MainScreen persists the
     * query into the Home command-center store.
     */
    var onSearchRecorded: ((String) -> Unit)? = null

    /** Records the committed query (the one that produced the opened result). */
    fun commitSearch() {
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) onSearchRecorded?.invoke(trimmed)
    }

    // Replay-1 shared flow (not StateFlow): re-emitting the same query text
    // must still re-run the search — that is how sort/category changes re-
    // execute without touching the text (StateFlow would dedupe the value).
    private val queryFlow = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    private var searchJob: Job? = null
    private var scope: kotlinx.coroutines.CoroutineScope? = null

    /**
     * Real SRS snapshot for study-aware filters/sorts (todo #108–#110).
     * Injected lazily by the host so tests can build the controller without
     * a Koin container.
     */
    var studyOverlayLoader: (suspend () -> ua.syt0r.kanji.core.knowledge.StudyOverlay)? = null

    /**
     * Media references for the MEDIA result section (todo #117, spec §28).
     * Queried alongside the knowledge engine so "subtitle text" search finds
     * where a term appeared in the user's own media. Null on platforms with
     * no media backend.
     */
    var mediaSearch: (suspend (String, Int) -> List<ua.syt0r.kanji.core.knowledge.media.MediaReference>)? = null

    /**
     * Media hits from the last search pass — surfaced under a MEDIA section
     * alongside the grouped knowledge results.
     */
    var mediaHits by mutableStateOf<List<ua.syt0r.kanji.core.knowledge.media.MediaReference>>(emptyList())
        private set

    fun attach(
        scope: kotlinx.coroutines.CoroutineScope,
        engine: KnowledgeSearchEngine,
        profileStore: LearnerProfileStore? = null,
        studyOverlayLoader: (suspend () -> ua.syt0r.kanji.core.knowledge.StudyOverlay)? = null,
        mediaSearch: (suspend (String, Int) -> List<ua.syt0r.kanji.core.knowledge.media.MediaReference>)? = null
    ) {
        if (this.scope != null) return
        this.scope = scope
        this.studyOverlayLoader = studyOverlayLoader
        this.mediaSearch = mediaSearch
        scope.launch {
            queryFlow.collect { text ->
                val trimmed = text.trim()
                if (trimmed.isEmpty()) {
                    state = UniversalSearchState.Idle
                    mediaHits = emptyList()
                    return@collect
                }
                state = UniversalSearchState.Searching
                // Honest debounce: 280ms, cancellable.
                delay(280)
                try {
                    // Media section (todo #117): real subtitle/bookmark/mined
                    // references matching the raw query. Queried in parallel
                    // with the knowledge engine; failures never kill search.
                    mediaHits = try {
                        mediaSearch?.invoke(trimmed, 5) ?: emptyList()
                    } catch (t: Throwable) {
                        emptyList()
                    }
                    val categories = if (activeCategories.isEmpty())
                        SearchCategory.entries.toSet()
                    else activeCategories
                    // Plain-text structured filters (spec §15): "common verbs N3"
                    // becomes free text + real SearchFilters. The recognized
                    // tokens are surfaced as chips, never swallowed silently.
                    val parsed = KnowledgeQueryParser.parse(trimmed)
                    activeChips = parsed.chips
                    // Study-aware search: a real SRS snapshot is loaded once
                    // per query so "Recently studied" sorts and the study-state
                    // filter operate on real cards (todo #108–#110).
                    val overlay = try {
                        studyOverlayLoader?.invoke() ?: ua.syt0r.kanji.core.knowledge.StudyOverlay()
                    } catch (t: Throwable) {
                        ua.syt0r.kanji.core.knowledge.StudyOverlay()
                    }
                    val results = engine.search(
                        KnowledgeSearchQuery(
                            text = parsed.text,
                            categories = categories,
                            filters = parsed.filters,
                            sort = sortState,
                            studyOverlay = overlay,
                            kanjiLimit = 12,
                            wordLimit = 10,
                            sentenceLimit = 6,
                            grammarLimit = 6
                        )
                    )
                    // Level-adaptive presentation (spec §23): search results are
                    // never hidden — a beginner profile just sees easy examples
                    // first, ordered by the estimated difficulty of each sentence.
                    val adapted = if (profileStore != null) {
                        val preference = profileStore.load()
                        val presentation = preference.effectivePresentation()
                        if (presentation.sentenceDifficulty == ua.syt0r.kanji.core.knowledge.SentenceDifficulty.Hard) {
                            results
                        } else {
                            results.copy(
                                sentences = results.sentences.sortedBy {
                                    SentenceDifficultyScorer.score(it.sentence.text).level
                                }
                            )
                        }
                    } else {
                        results
                    }
                    state = UniversalSearchState.Results(adapted)
                } catch (t: Throwable) {
                    state = UniversalSearchState.Error(t.message ?: "Search failed")
                }
            }
        }
    }

    fun open() {
        query = ""
        selectedIndex = 0
        activeChips = emptyList()
        mediaHits = emptyList()
        state = UniversalSearchState.Idle
        isOpen = true
    }

    fun close() {
        isOpen = false
    }

    fun toggle() {
        if (isOpen) close() else open()
    }

    fun updateQuery(newQuery: String) {
        query = newQuery
        selectedIndex = 0
        searchJob?.cancel()
        searchJob = scope?.launch { queryFlow.emit(newQuery) }
    }

    fun toggleCategory(category: SearchCategory) {
        activeCategories = if (category in activeCategories) {
            activeCategories - category
        } else {
            activeCategories + category
        }
        selectedIndex = 0
        searchJob?.cancel()
        searchJob = scope?.launch { queryFlow.emit(query) }
    }

    fun clearCategories() {
        activeCategories = emptySet()
        selectedIndex = 0
        searchJob?.cancel()
        searchJob = scope?.launch { queryFlow.emit(query) }
    }

    /** Keyboard navigation over the flattened result list. */
    fun moveSelection(delta: Int) {
        val count = flattenedResults().size
        if (count == 0) return
        selectedIndex = ((selectedIndex + delta) % count + count) % count
    }

    fun executeSelected() {
        val flat = flattenedResults()
        val item = flat.getOrNull(selectedIndex) ?: return
        commitSearch()
        when (item) {
            is SearchItem.KanjiItem -> onNavigate?.invoke(MainDestination.KanjiEntry(item.hit.kanji))
            is SearchItem.WordItem -> onNavigate?.invoke(MainDestination.WordEntry(item.hit.word.id))
            is SearchItem.SentenceItem -> onNavigate?.invoke(MainDestination.SentenceEntry(item.text, item.translation))
            is SearchItem.GrammarItem -> onNavigate?.invoke(MainDestination.KnowledgeExplorer(item.pattern))
            is SearchItem.MediaItem -> onNavigate?.invoke(MainDestination.Media)
        }
        close()
    }

    private fun flattenedResults(): List<SearchItem> {
        val knowledge = (state as? UniversalSearchState.Results)?.let { flattenForUi(it.results) }.orEmpty()
        val media = mediaHits.map { SearchItem.MediaItem(it) }
        return knowledge + media
    }
}

/** Flattened, keyboard-navigable result item. */
internal sealed interface SearchItem {
    data class KanjiItem(val hit: KanjiHit) : SearchItem
    data class WordItem(val hit: WordHit) : SearchItem
    data class SentenceItem(val text: String, val translation: String) : SearchItem
    data class GrammarItem(val pattern: String, val meaning: String) : SearchItem
    data class MediaItem(val reference: ua.syt0r.kanji.core.knowledge.media.MediaReference) : SearchItem
}

/** Global search controller, like the command palette. */
object KaiteyoSearch {
    val controller = UniversalSearchController()
}

@Composable
fun UniversalSearchOverlay(
    controller: UniversalSearchController = KaiteyoSearch.controller
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val koin = getKoin()

    // Attach once — the host (MainScreen) sets onNavigate before this renders.
    val engine = koinInject<KnowledgeSearchEngine>()
    val profileStore = koinInject<LearnerProfileStore>()
    // Study-aware search (todo #108–#110): the real SRS card map feeds the
    // overlay builder so study sorts/filters use actual FSRS state.
    val srsCards = remember { koin.getOrNull<ua.syt0r.kanji.core.srs.SrsCardRepository>() }
    // Media references (todo #117): the shared core store — desktop records
    // real subtitle/bookmark/mined references; other platforms may have none.
    val mediaStore = remember { koin.getOrNull<ua.syt0r.kanji.core.knowledge.media.MediaReferenceStore>() }
    val scope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob()) }
    LaunchedEffect(Unit) {
        controller.attach(
            scope = scope,
            engine = engine,
            profileStore = profileStore,
            studyOverlayLoader = if (srsCards != null) {
                { ua.syt0r.kanji.core.knowledge.StudyOverlayBuilder.build(srsCards.getAll()) }
            } else {
                null
            },
            mediaSearch = if (mediaStore != null) { query, limit ->
                mediaStore.matching(query, limit)
            } else {
                null
            }
        )
    }

    AnimatedVisibility(
        visible = controller.isOpen,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { controller.close() },
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 72.dp)
                    .width(720.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(initialScale = 0.97f, animationSpec = androidx.compose.animation.core.spring()) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(surfaceColors.surfaceElevated)
                    ) {
                        UniversalSearchField(controller, surfaceColors, accent)
                        UniversalSearchResults(controller, surfaceColors, accent)
                        UniversalSearchFooter(controller, surfaceColors)
                    }
                }
            }
        }
    }
}

@Composable
private fun UniversalSearchField(
    controller: UniversalSearchController,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val koin = getKoin()
    // OCR is a platform-gated input mode (spec §16): the desktop app registers
    // a SearchOcrProvider; on other platforms none exists and the control is
    // hidden — never a dead button.
    val ocrProvider = remember { koin.getOrNull<SearchOcrProvider>() }
    var ocrBusy by remember { mutableStateOf(false) }
    var ocrHint by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(controller.isOpen) {
        if (controller.isOpen) {
            text = controller.query
            focusRequester.requestFocus()
        }
    }

    val applyText: (String) -> Unit = { value ->
        text = value
        controller.updateQuery(value)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColors.surfaceInteractive)
            .focusRequester(focusRequester)
            .focusable()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("検索", color = accent.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            BasicTextField(
                value = text,
                onValueChange = applyText,
                textStyle = TextStyle(color = surfaceColors.textPrimary, fontSize = 17.sp),
                cursorBrush = SolidColor(accent.primary),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            if (controller.query.isNotEmpty()) {
                Text("esc", color = surfaceColors.textMuted, fontSize = 11.sp)
            }
        }

            // ── Input modes (spec §16): real sources, real results ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InputModeChip(
                    label = "Paste clipboard",
                    surfaceColors = surfaceColors,
                    accent = accent
                ) {
                    scope.launch {
                        val pasted = clipboard.getText()?.text?.trim()
                        if (!pasted.isNullOrEmpty()) {
                            ocrHint = null
                            applyText(pasted)
                        }
                    }
                }
                if (ocrProvider != null) {
                    InputModeChip(
                        label = if (ocrBusy) "Scanning…" else "Scan image (OCR)",
                        surfaceColors = surfaceColors,
                        accent = accent,
                        enabled = !ocrBusy
                    ) {
                        scope.launch {
                            ocrBusy = true
                            ocrHint = null
                            val recognized = ocrProvider.ocrClipboardImage()
                            ocrBusy = false
                            if (recognized != null) {
                                applyText(recognized)
                            } else {
                                ocrHint = "No Japanese text found in the clipboard image."
                            }
                        }
                    }
                }
            }
            if (ocrHint != null) {
                Text(
                    text = ocrHint!!,
                    color = surfaceColors.textMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (controller.activeChips.isNotEmpty()) {
                // Structured-filter chips (KT-SEARCH-002): read-only tags that
                // reflect the recognized filters — edit the query to change them.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    controller.activeChips.forEach { chip ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(accent.primary.copy(alpha = 0.10f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = chip,
                                color = accent.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Text(
                        text = "filtering results — edit the query to change",
                        color = surfaceColors.textMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun InputModeChip(
    label: String,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(if (enabled) accent.primary.copy(alpha = 0.12f) else surfaceColors.surfaceInteractive)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = if (enabled) accent.primary else surfaceColors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CategoryFilterChips(
    controller: UniversalSearchController,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
) {
    val categories = SearchCategory.entries
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColors.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val isActive = category in controller.activeCategories
            val allActive = controller.activeCategories.isEmpty()
            val selected = isActive || allActive
            val interactionSource = remember { MutableInteractionSource() }
            val hovered by interactionSource.collectIsHoveredAsState()

            val bgColor by animateColorAsState(
                if (selected) accent.primary.copy(alpha = if (hovered) 0.22f else 0.15f)
                else if (hovered) surfaceColors.surfaceInteractive
                else Color.Transparent
            )
            val textColor = if (selected) accent.primary else surfaceColors.textMuted

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .border(
                        width = if (selected) 1.dp else 0.dp,
                        color = if (selected) accent.primary.copy(alpha = 0.3f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .hoverable(interactionSource)
                    .clickable { controller.toggleCategory(category) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = category.label,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
        if (controller.activeCategories.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { controller.clearCategories() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text("Clear", color = surfaceColors.textMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun UniversalSearchResults(
    controller: UniversalSearchController,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
    ) {
        when (val current = controller.state) {
            is UniversalSearchState.Idle -> {
                EmptyHint(
                    surfaceColors = surfaceColors,
                    title = "Search everything",
                    message = "Kanji, words, sentences and grammar — try 食べる, taberu, eat, or JLPT:N3."
                )
            }
            is UniversalSearchState.Searching -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UniversalSearchState.Error -> {
                EmptyHint(
                    surfaceColors = surfaceColors,
                    title = "Search failed",
                    message = current.message
                )
            }
            is UniversalSearchState.Results -> {
                // Stack the chips, sort row and list vertically — Box children
                // would otherwise overlay each other.
                Column(Modifier.fillMaxSize()) {
                // Category filter chips above the results.
                CategoryFilterChips(controller, surfaceColors, accent)
                // Media hits are surfaced alongside knowledge results (todo
                // #117) — the empty check must consider both.
                val flat = flattenForUi(current.results) + controller.mediaHits.map { SearchItem.MediaItem(it) }
                if (flat.isEmpty()) {
                    EmptyHint(
                        surfaceColors = surfaceColors,
                        title = "No results",
                        message = "Nothing matched \"${controller.query.trim()}\"."
                    )
                } else {
                    // Sort row (KT-SEARCH-004): every option re-runs the real
                    // query with the new sort — kanji sorts server-side in the
                    // index, word/sentence sorts apply to their sections.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfaceColors.surface)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Sort:",
                            color = surfaceColors.textMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        SearchSort.entries.forEach { sortOption ->
                            val selected = controller.sortState == sortOption
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(
                                        if (selected) accent.primary.copy(alpha = 0.12f)
                                        else surfaceColors.surfaceInteractive
                                    )
                                    .clickable { controller.setSort(sortOption) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = sortOption.label,
                                    color = if (selected) accent.primary else surfaceColors.textMuted,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    LazyColumn(Modifier.weight(1f)) {
                        // Sectioned rendering: kanji, then words, then sentences/grammar,
                        // then media references (todo #117) from the user's own library.
                        if (controller.mediaHits.isNotEmpty()) {
                            item { SectionLabel("MEDIA · ${controller.mediaHits.size}", surfaceColors, accent) }
                            items(controller.mediaHits.size, key = { "m${it}_${controller.mediaHits[it].timestampMs}_${controller.mediaHits[it].title}" }) { index ->
                                val reference = controller.mediaHits[index]
                                SearchResultRow(
                                    primary = reference.text,
                                    secondary = reference.title,
                                    meta = "${reference.kind.name.lowercase()} · ${(reference.timestampMs / 1000)}s",
                                    selected = flat.indexOfFirst { it is SearchItem.MediaItem && it.reference.timestampMs == reference.timestampMs && it.reference.title == reference.title } == controller.selectedIndex,
                                    onClick = {
                                        controller.commitSearch()
                                        controller.onNavigate?.invoke(MainDestination.Media)
                                        controller.close()
                                    },
                                    surfaceColors = surfaceColors,
                                    accent = accent
                                )
                            }
                        }
                        if (current.results.kanji.isNotEmpty()) {
                            item { SectionLabel("KANJI · ${current.results.kanji.size}", surfaceColors, accent) }
                            items(current.results.kanji.size, key = { "k$it" }) { index ->
                                val hit = current.results.kanji[index]
                                SearchResultRow(
                                    primary = hit.kanji,
                                    secondary = hit.keyword ?: "",
                                    meta = hit.on.firstOrNull() ?: "",
                                    selected = flat.indexOfFirst { it is SearchItem.KanjiItem && it.hit.kanji == hit.kanji } == controller.selectedIndex,
                                    onClick = {
                                        controller.commitSearch()
                                        controller.onNavigate?.invoke(MainDestination.KanjiEntry(hit.kanji))
                                        controller.close()
                                    },
                                    surfaceColors = surfaceColors,
                                    accent = accent
                                )
                            }
                        }
                        if (current.results.words.isNotEmpty()) {
                            item { SectionLabel("WORDS · ${current.results.wordTotal}", surfaceColors, accent) }
                            items(current.results.words.size, key = { "w${it}_${current.results.words[it].word.id}" }) { index ->
                                val hit = current.results.words[index]
                                SearchResultRow(
                                    primary = hit.word.displaySpelling,
                                    secondary = hit.word.kanaReading,
                                    meta = hit.word.combinedGlossary(),
                                    selected = flat.indexOfFirst { it is SearchItem.WordItem && it.hit.word.id == hit.word.id } == controller.selectedIndex,
                                    onClick = {
                                        controller.commitSearch()
                                        controller.onNavigate?.invoke(MainDestination.WordEntry(hit.word.id))
                                        controller.close()
                                    },
                                    surfaceColors = surfaceColors,
                                    accent = accent
                                )
                            }
                        }
                        if (current.results.sentences.isNotEmpty()) {
                            item { SectionLabel("SENTENCES · ${current.results.sentenceTotal}", surfaceColors, accent) }
                            items(current.results.sentences.size, key = { "s${it}_${current.results.sentences[it].sentence.text}" }) { index ->
                                val sentence = current.results.sentences[index].sentence
                                SearchResultRow(
                                    primary = sentence.text,
                                    secondary = sentence.translation,
                                    meta = "",
                                    selected = flat.indexOfFirst { it is SearchItem.SentenceItem && it.text == sentence.text } == controller.selectedIndex,
                                    onClick = {
                                        controller.commitSearch()
                                        controller.onNavigate?.invoke(MainDestination.SentenceEntry(sentence.text, sentence.translation))
                                        controller.close()
                                    },
                                    surfaceColors = surfaceColors,
                                    accent = accent
                                )
                            }
                        }
                        if (current.results.grammar.isNotEmpty()) {
                            item { SectionLabel("GRAMMAR · ${current.results.grammar.size}", surfaceColors, accent) }
                            items(current.results.grammar.size, key = { "g$it" }) { index ->
                                val hit = current.results.grammar[index]
                                SearchResultRow(
                                    primary = hit.pattern.pattern,
                                    secondary = hit.pattern.meaning,
                                    meta = hit.pattern.jlpt?.let { "N$it" } ?: "",
                                    selected = flat.indexOfFirst { it is SearchItem.GrammarItem && it.pattern == hit.pattern.pattern } == controller.selectedIndex,
                                    onClick = {
                                        controller.commitSearch()
                                        controller.onNavigate?.invoke(MainDestination.KnowledgeExplorer(hit.pattern.pattern))
                                        controller.close()
                                    },
                                    surfaceColors = surfaceColors,
                                    accent = accent
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

internal fun flattenForUi(results: GroupedSearchResults): List<SearchItem> = buildList {
    results.kanji.forEach { add(SearchItem.KanjiItem(it)) }
    results.words.forEach { add(SearchItem.WordItem(it)) }
    results.sentences.forEach { add(SearchItem.SentenceItem(it.sentence.text, it.sentence.translation)) }
    results.grammar.forEach { add(SearchItem.GrammarItem(it.pattern.pattern, it.pattern.meaning)) }
}

@Composable
private fun SectionLabel(
    label: String,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
) {
    Text(
        text = label,
        color = accent.primary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SearchResultRow(
    primary: String,
    secondary: String,
    meta: String,
    selected: Boolean,
    onClick: () -> Unit,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) accent.primary.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = primary,
                color = if (selected) accent.primary else surfaceColors.textPrimary,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (secondary.isNotBlank()) {
                Text(
                    text = secondary,
                    color = surfaceColors.textMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                color = surfaceColors.textMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyHint(
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    title: String,
    message: String
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, color = surfaceColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(message, color = surfaceColors.textMuted, fontSize = 12.sp)
    }
}

@Composable
private fun UniversalSearchFooter(
    controller: UniversalSearchController,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColors.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("↑↓ navigate", color = surfaceColors.textMuted, fontSize = 11.sp)
        Text("↵ open", color = surfaceColors.textMuted, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        Text("Ctrl+Shift+F to open", color = surfaceColors.textMuted, fontSize = 11.sp)
    }
}
