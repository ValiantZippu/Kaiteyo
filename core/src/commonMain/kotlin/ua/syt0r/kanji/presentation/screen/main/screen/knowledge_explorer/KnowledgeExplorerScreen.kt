@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ua.syt0r.kanji.presentation.screen.main.screen.knowledge_explorer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.core.knowledge.ComponentKnowledge
import ua.syt0r.kanji.core.knowledge.FrequencyBand
import ua.syt0r.kanji.core.knowledge.GroupedSearchResults
import ua.syt0r.kanji.core.knowledge.KanjiHit
import ua.syt0r.kanji.core.knowledge.KnowledgeEdgeType
import ua.syt0r.kanji.core.knowledge.KnowledgeGraph
import ua.syt0r.kanji.core.knowledge.KnowledgeNode
import ua.syt0r.kanji.core.knowledge.SearchFilters
import ua.syt0r.kanji.core.knowledge.SearchSort
import ua.syt0r.kanji.core.knowledge.SentenceKnowledge
import ua.syt0r.kanji.core.knowledge.WordHit
import ua.syt0r.kanji.core.knowledge.frequencyRankLabel
import ua.syt0r.kanji.presentation.common.ui.KaiteyoEmptyState
import ua.syt0r.kanji.presentation.common.ui.KaiteyoPill
import ua.syt0r.kanji.presentation.common.ui.KaiteyoSectionCard
import ua.syt0r.kanji.presentation.common.ui.KaiteyoTag
import ua.syt0r.kanji.presentation.common.ui.PageIdentity
import ua.syt0r.kanji.presentation.common.ui.ProvidePageIdentity
import ua.syt0r.kanji.presentation.common.theme.AnimationTokens
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.screen.knowledge_explorer.KnowledgeExplorerContract.ScreenState

// ============================================================
// KNOWLEDGE EXPLORER — SCREEN
// ------------------------------------------------------------
// Dictionary explorer: universal grouped search → kanji / word
// entries → progressively expanding knowledge graph. Every
// section is driven by the knowledge core (real dictionary data).
// ============================================================

/** A keyboard-selectable result entry (kanji and words, in display order). */
private sealed interface ResultEntry {
    data class Kanji(val hit: KanjiHit) : ResultEntry
    data class Word(val hit: WordHit) : ResultEntry
}

@Composable
fun KnowledgeExplorerScreen(
    initialQuery: String = "",
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = getMultiplatformViewModel<KnowledgeExplorerContract.ViewModel>()
    val state by viewModel.state.collectAsState()

    var query by remember { mutableStateOf(initialQuery) }

    LaunchedEffect(Unit) {
        if (initialQuery.isNotBlank()) viewModel.onQueryChange(initialQuery)
    }

    // Flat, ordered list of selectable result entries (kanji then words, in
    // display order) — the single source of truth for arrow-key navigation
    // and Enter-to-open, mirroring the Library's unified search pattern.
    val currentResults = (state as? ScreenState.Results)?.results
    val selectableEntries = remember(currentResults) {
        buildList {
            currentResults?.kanji?.forEach { add(ResultEntry.Kanji(it)) }
            currentResults?.words?.forEach { add(ResultEntry.Word(it)) }
        }
    }
    var selectedIndex by remember { mutableStateOf(0) }
    LaunchedEffect(selectableEntries.size) {
        if (selectableEntries.isNotEmpty()) selectedIndex = selectedIndex % selectableEntries.size
        else selectedIndex = 0
    }
    val onKey = { event: androidx.compose.ui.input.key.KeyEvent ->
        if (event.type != androidx.compose.ui.input.key.KeyEventType.KeyDown) false
        else if (selectableEntries.isEmpty()) false
        else when (event.key) {
            androidx.compose.ui.input.key.Key.DirectionDown -> {
                selectedIndex = (selectedIndex + 1) % selectableEntries.size
                true
            }
            androidx.compose.ui.input.key.Key.DirectionUp -> {
                selectedIndex = (selectedIndex - 1 + selectableEntries.size) % selectableEntries.size
                true
            }
            androidx.compose.ui.input.key.Key.Enter -> {
                when (val entry = selectableEntries[selectedIndex]) {
                    is ResultEntry.Kanji -> viewModel.openKanji(entry.hit.kanji)
                    is ResultEntry.Word -> viewModel.openWord(entry.hit.word.id)
                }
                true
            }
            else -> false
        }
    }

    val panel = when (state) {
        is ScreenState.Initial -> "Landing"
        is ScreenState.Searching -> "Searching"
        is ScreenState.Results -> "Results"
        is ScreenState.KanjiDetail -> "KanjiDetail"
        is ScreenState.WordDetail -> "WordDetail"
        is ScreenState.Graph -> "Graph"
        is ScreenState.Error -> "Error"
    }

    ProvidePageIdentity(
        PageIdentity(id = "knowledge_explorer", name = "Dictionary explorer", route = "/knowledge_explorer", panel = panel)
    ) {
        // Arrow keys navigate the results, Enter opens the selection — attached
        // to an ancestor of the search field so it works once the field has
        // focus (same pattern as the Library's unified search).
        Column(
            modifier
                .fillMaxSize()
                .onPreviewKeyEvent(onKey)
        ) {
            ExplorerSearchBar(
                query = query,
                onQueryChange = {
                    query = it
                    viewModel.onQueryChange(it)
                },
                onClear = {
                    query = ""
                    viewModel.clearSearch()
                },
                onBack = onClose
            )

            when (val current = state) {
                is ScreenState.Initial -> LandingContent(
                    onBrowse = viewModel::browse,
                    onExplore = { viewModel.openGraph("食") }
                )
                is ScreenState.Searching -> CenteredLoader()
                is ScreenState.Results -> Column(Modifier.fillMaxSize()) {
                    // Real filters + sorts: every chip changes the query.
                    SearchFilterRow(
                        filters = current.filters,
                        sort = current.sort,
                        onSetFilter = viewModel::setFilter,
                        onSetSort = viewModel::setSort,
                        onClearFilters = viewModel::clearFilters
                    )
                    Box(Modifier.weight(1f)) {
                        ResultsContent(
                            results = current.results,
                            selectedIndex = selectedIndex,
                            onKanjiClick = viewModel::openKanji,
                            onWordClick = viewModel::openWord
                        )
                    }
                }
                is ScreenState.KanjiDetail -> KanjiDetailContent(
                    state = current,
                    onWordClick = viewModel::openWord,
                    onExploreGraph = { viewModel.openGraph(current.character) }
                )
                is ScreenState.WordDetail -> WordDetailContent(
                    state = current,
                    onKanjiClick = viewModel::openKanji
                )
                is ScreenState.Graph -> GraphContent(
                    graph = current.graph,
                    selected = current.selected,
                    loading = current.loading,
                    onSelectNode = viewModel::focusNode,
                    onExpand = viewModel::expandNode,
                    onBackToSearch = { viewModel.back() }
                )
                is ScreenState.Error -> ErrorContent(current.message, onRetry = viewModel::retry)
            }
        }
    }
}

// ============================================================
// SEARCH BAR
// ============================================================

@Composable
private fun ExplorerSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = surfaceColors.textPrimary
            )
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search kanji, words, sentences, grammar…", color = surfaceColors.textMuted) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = surfaceColors.textMuted)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = surfaceColors.textMuted)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { /* debounce in the VM handles it */ })
        )
    }
}

// ============================================================
// LANDING
// ============================================================

@Composable
private fun LandingContent(
    onBrowse: (SearchFilters) -> Unit,
    onExplore: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = Dimens.Space3, end = Dimens.Space3, bottom = Dimens.Space8, top = Dimens.Space3
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.RadiusLg))
                    .background(surfaceColors.surface)
                    .padding(Dimens.Space5),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "知",
                    fontSize = 56.sp,
                    color = accent.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(Dimens.Space2))
                Text(
                    text = "Dictionary explorer",
                    style = MaterialTheme.typography.titleMedium,
                    color = surfaceColors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(Dimens.Space1))
                Text(
                    text = "Search kanji, words, sentences and grammar — or browse by filter below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = surfaceColors.textMuted
                )
            }
        }

        // Filter-only browsing (spec §30): every tile runs a real query with
        // empty text + that filter — the kanji index returns the actual set.
        item { SectionHeader("BROWSE KANJI", 7) }
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
            ) {
                BrowseTile(label = "JLPT N5", detail = "Beginner kanji", onClick = { onBrowse(SearchFilters(jlpt = 5)) })
                BrowseTile(label = "JLPT N4", detail = "Core everyday kanji", onClick = { onBrowse(SearchFilters(jlpt = 4)) })
                BrowseTile(label = "Grade 1", detail = "First-year school kanji", onClick = { onBrowse(SearchFilters(grade = 1)) })
                BrowseTile(label = "Very common", detail = "Top frequency band", onClick = { onBrowse(SearchFilters(frequency = FrequencyBand.VeryCommon)) })
                BrowseTile(label = "4 strokes", detail = "Simplest shapes", onClick = { onBrowse(SearchFilters(strokeCount = 4)) })
                BrowseTile(label = "Verbs", detail = "Words tagged as verbs", onClick = { onBrowse(SearchFilters(partOfSpeech = "verb")) })
                BrowseTile(label = "Nouns", detail = "Words tagged as nouns", onClick = { onBrowse(SearchFilters(partOfSpeech = "noun")) })
            }
        }

        item { SectionHeader("KNOWLEDGE GRAPH", 1) }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.RadiusMd))
                    .background(surfaceColors.surface)
                    .clickable(onClick = onExplore)
                    .padding(Dimens.Space3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
            ) {
                Text("🗺️", fontSize = 22.sp)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Explore the graph from 食",
                        style = MaterialTheme.typography.bodyMedium,
                        color = surfaceColors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Pan, zoom and expand kanji → words → sentences → grammar",
                        style = MaterialTheme.typography.bodySmall,
                        color = surfaceColors.textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowseTile(
    label: String,
    detail: String,
    onClick: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Column(
        modifier = Modifier
            .widthIn(min = 130.dp)
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(surfaceColors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = accent.primary
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.labelSmall,
            color = surfaceColors.textMuted,
            maxLines = 1
        )
    }
}

// ============================================================
// RESULTS (grouped sections)
// ============================================================

@Composable
private fun ResultsContent(
    results: GroupedSearchResults,
    selectedIndex: Int = -1,
    onKanjiClick: (String) -> Unit,
    onWordClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = Dimens.Space3, end = Dimens.Space3, bottom = Dimens.Space8
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        if (results.isEmpty) {
            item {
                KaiteyoEmptyState(
                    icon = "🔍",
                    title = "No results",
                    message = "Nothing in the dictionary matched \"${results.query.text}\". Try kana, kanji, or an English meaning."
                )
            }
            return@LazyColumn
        }

        if (results.kanji.isNotEmpty() || results.words.isNotEmpty()) {
            item {
                Text(
                    text = "↑/↓ navigate · Enter open",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalSurfaceColors.current.textMuted
                )
            }
        }

        if (results.kanji.isNotEmpty()) {
            item { SectionHeader("KANJI", results.kanji.size) }
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
                ) {
                    results.kanji.forEachIndexed { index, hit ->
                        KanjiResultTile(
                            hit = hit,
                            selected = index == selectedIndex,
                            onClick = { onKanjiClick(hit.kanji) }
                        )
                    }
                }
            }
        }

        if (results.words.isNotEmpty()) {
            item { SectionHeader("WORDS", results.wordTotal) }
            itemsIndexed(results.words, key = { _, hit -> hit.word.id }) { index, hit ->
                WordResultRow(
                    hit = hit,
                    selected = (results.kanji.size + index) == selectedIndex,
                    onClick = { onWordClick(hit.word.id) }
                )
            }
        }

        if (results.sentences.isNotEmpty()) {
            item { SectionHeader("SENTENCES", results.sentenceTotal) }
            items(results.sentences, key = { it.sentence.text }) { hit ->
                SentenceRow(sentence = hit.sentence)
            }
        }

        if (results.grammar.isNotEmpty()) {
            item { SectionHeader("GRAMMAR", results.grammar.size) }
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
                ) {
                    results.grammar.forEach { hit ->
                        KaiteyoTag(text = hit.pattern.pattern, tint = LocalKaiteyoAccent.current.secondary)
                    }
                }
            }
        }
    }
}

// ============================================================
// FILTER / SORT ROW — every chip changes the real query
// ============================================================

@Composable
private fun SearchFilterRow(
    filters: SearchFilters,
    sort: SearchSort,
    onSetFilter: (SearchFilters) -> Unit,
    onSetSort: (SearchSort) -> Unit,
    onClearFilters: () -> Unit
) {
    val hasFilters = filters != SearchFilters.None
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space1)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            Text(
                text = "SORT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = LocalSurfaceColors.current.textMuted,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            SearchSort.entries.forEach { option ->
                KaiteyoPill(
                    text = option.label,
                    selected = sort == option,
                    onClick = { onSetSort(option) }
                )
            }
        }
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(AnimationTokens.DurationContent)),
            exit = fadeOut(tween(AnimationTokens.DurationFeedbackFast))
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.Space1),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
            ) {
                Text(
                    text = "FILTER",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = LocalSurfaceColors.current.textMuted,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                (1..5).forEach { level ->
                    KaiteyoPill(
                        text = "N$level",
                        selected = filters.jlpt == level,
                        onClick = { onSetFilter(filters.copy(jlpt = if (filters.jlpt == level) null else level)) }
                    )
                }
                (1..6).forEach { grade ->
                    KaiteyoPill(
                        text = "G$grade",
                        selected = filters.grade == grade,
                        onClick = { onSetFilter(filters.copy(grade = if (filters.grade == grade) null else grade)) }
                    )
                }
                FrequencyBand.entries.forEach { band ->
                    KaiteyoPill(
                        text = band.label,
                        selected = filters.frequency == band,
                        onClick = { onSetFilter(filters.copy(frequency = if (filters.frequency == band) null else band)) }
                    )
                }
                listOf("verb", "noun", "adjective").forEach { pos ->
                    KaiteyoPill(
                        text = pos,
                        selected = filters.partOfSpeech == pos,
                        onClick = { onSetFilter(filters.copy(partOfSpeech = if (filters.partOfSpeech == pos) null else pos)) }
                    )
                }
                if (hasFilters) {
                    KaiteyoPill(
                        text = "Clear filters",
                        selected = false,
                        onClick = onClearFilters
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.Space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(accent.primary)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = surfaceColors.textSecondary
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            color = surfaceColors.textMuted
        )
    }
}

@Composable
private fun KanjiResultTile(
    hit: KanjiHit,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Column(
        modifier = Modifier
            .widthIn(min = 88.dp)
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(if (selected) accent.primary.copy(alpha = 0.14f) else surfaceColors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(hit.kanji, fontSize = 30.sp, color = surfaceColors.textPrimary, fontWeight = FontWeight.Bold)
        hit.keyword?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = surfaceColors.textSecondary,
                maxLines = 1
            )
        }
        Text(
            text = hit.on.firstOrNull() ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = surfaceColors.textMuted,
            maxLines = 1
        )
        hit.frequencyRank?.let { rank ->
            Text(
                text = frequencyRankLabel(rank),
                style = MaterialTheme.typography.labelSmall,
                color = accent.primary
            )
        }
    }
}

@Composable
private fun WordResultRow(
    hit: WordHit,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(if (selected) accent.primary.copy(alpha = 0.14f) else surfaceColors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        Text(
            text = hit.word.displaySpelling,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = surfaceColors.textPrimary
        )
        Text(
            text = hit.word.kanaReading,
            style = MaterialTheme.typography.bodySmall,
            color = surfaceColors.textMuted
        )
        if (hit.jlpt != null) {
            KaiteyoTag(text = "N${hit.jlpt}", tint = LocalKaiteyoAccent.current.secondary)
        }
        Text(
            text = hit.word.combinedGlossary(),
            style = MaterialTheme.typography.bodySmall,
            color = surfaceColors.textSecondary,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
    }
}

@Composable
private fun SentenceRow(
    sentence: SentenceKnowledge,
    showTranslation: Boolean = true
) {
    val surfaceColors = LocalSurfaceColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(surfaceColors.surface)
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2)
    ) {
        Text(sentence.text, style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textPrimary)
        if (showTranslation) {
            Spacer(Modifier.height(2.dp))
            Text(sentence.translation, style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
        }
    }
}

// ============================================================
// KANJI DETAIL
// ============================================================

@Composable
private fun KanjiDetailContent(
    state: ScreenState.KanjiDetail,
    onWordClick: (Long) -> Unit,
    onExploreGraph: () -> Unit
) {
    if (state.loading) {
        CenteredLoader()
        return
    }
    val kanji = state.kanji
    if (kanji == null) {
        ErrorContent("${state.character} is not in the bundled dictionary data.")
        return
    }

    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = Dimens.Space3, end = Dimens.Space3, bottom = Dimens.Space8
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        // Hero
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.RadiusLg))
                    .background(surfaceColors.surface)
                    .padding(Dimens.Space6),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(kanji.character, fontSize = 64.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                kanji.keyword?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium, color = surfaceColors.textSecondary)
                }
                Spacer(Modifier.height(Dimens.Space2))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)) {
                    kanji.classifications.forEach { tag -> KaiteyoTag(text = tag.label) }
                }
                Spacer(Modifier.height(Dimens.Space2))
                KaiteyoPill(text = "Explore graph", selected = false, onClick = onExploreGraph)
            }
        }

        // Readings
        item {
            KaiteyoSectionCard(title = "Readings") {
                ReadingLine("ON", kanji.onReadings)
                ReadingLine("KUN", kanji.kunReadings)
            }
        }

        // Meanings
        item {
            KaiteyoSectionCard(title = "Meanings") {
                kanji.meanings.forEach { meaning ->
                    Text(
                        text = "·  $meaning",
                        style = MaterialTheme.typography.bodyMedium,
                        color = surfaceColors.textPrimary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        // Frequency + metadata
        item {
            KaiteyoSectionCard(title = "Frequency & metadata") {
                MetaRow("Frequency", frequencyRankLabel(kanji.frequencyRank))
                kanji.strokeCount?.let { MetaRow("Strokes", "$it") }
                MetaRow("Jōyō set", if (kanji.isJoyo) "Jōyō" else "Not jōyō")
            }
        }

        // Components
        if (state.radicals.isNotEmpty()) {
            item { SectionHeader("COMPONENTS", state.radicals.size) }
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
                ) {
                    state.radicals.forEach { component ->
                        ComponentTile(component)
                    }
                }
            }
        }

        // Words using it
        if (state.words.isNotEmpty()) {
            item { SectionHeader("WORDS USING ${kanji.character}", state.words.size) }
            items(state.words, key = { it.id }) { word ->
                WordResultRow(hit = WordHit(word = word, jlpt = null), onClick = { onWordClick(word.id) })
            }
        }

        // Sentences (already profile-adapted by the ViewModel)
        if (state.sentences.isNotEmpty()) {
            item { SectionHeader("EXAMPLES", state.sentences.size) }
            items(state.sentences, key = { it.text }) { sentence ->
                SentenceRow(sentence)
            }
        }

        // Grammar found in the examples
        if (state.grammar.isNotEmpty()) {
            item { SectionHeader("GRAMMAR IN EXAMPLES", state.grammar.size) }
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
                ) {
                    state.grammar.forEach { match -> KaiteyoTag(text = match.matchedText, tint = accent.secondary) }
                }
            }
        }
    }
}

@Composable
private fun ReadingLine(type: String, readings: List<String>) {
    if (readings.isEmpty()) return
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        Text(type, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = surfaceColors.textMuted)
        Text(readings.joinToString("・"), style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textPrimary)
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
        Text(value, style = MaterialTheme.typography.bodySmall, color = surfaceColors.textPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ComponentTile(component: ComponentKnowledge) {
    val surfaceColors = LocalSurfaceColors.current
    Column(
        modifier = Modifier
            .widthIn(min = 64.dp)
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(surfaceColors.surface)
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(component.component, fontSize = 24.sp, color = surfaceColors.textPrimary)
        Text("${component.strokesCount} strokes", style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted)
    }
}

// ============================================================
// WORD DETAIL
// ============================================================

@Composable
private fun WordDetailContent(
    state: ScreenState.WordDetail,
    onKanjiClick: (String) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val word = state.word
    val glossary = if (state.glossary.isNotEmpty()) state.glossary else word.glossary
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = Dimens.Space3, end = Dimens.Space3, bottom = Dimens.Space8
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.RadiusLg))
                    .background(surfaceColors.surface)
                    .padding(Dimens.Space6),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(word.displaySpelling, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                Text(word.kanaReading, style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textMuted)
                Spacer(Modifier.height(Dimens.Space2))
                Text(glossary.joinToString("; "), style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textSecondary)
                if (glossary.size < word.glossary.size) {
                    Text(
                        text = "+${word.glossary.size - glossary.size} more senses hidden by your learner profile",
                        style = MaterialTheme.typography.labelSmall,
                        color = surfaceColors.textMuted,
                        modifier = Modifier.padding(top = Dimens.Space1)
                    )
                }
            }
        }

        if (state.kanji.isNotEmpty()) {
            item { SectionHeader("KANJI", state.kanji.size) }
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
                ) {
                    state.kanji.forEach { kanji ->
                        Column(
                            modifier = Modifier
                                .widthIn(min = 56.dp)
                                .clip(RoundedCornerShape(Dimens.RadiusMd))
                                .background(surfaceColors.surface)
                                .clickable { onKanjiClick(kanji.character) }
                                .padding(Dimens.Space2),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(kanji.character, fontSize = 22.sp, color = surfaceColors.textPrimary)
                            kanji.keyword?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        if (state.sentences.isNotEmpty()) {
            item { SectionHeader("SENTENCES", state.sentences.size) }
            items(state.sentences, key = { it.text }) { sentence ->
                SentenceRow(sentence = sentence, showTranslation = state.showTranslations)
            }
        }
    }
}

// ============================================================
// GRAPH — progressive expansion
// ============================================================

@Composable
private fun GraphContent(
    graph: KnowledgeGraph,
    selected: String?,
    loading: Boolean,
    onSelectNode: (String) -> Unit,
    onExpand: (String, Set<KnowledgeEdgeType>?) -> Unit,
    onBackToSearch: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    var typeFilter by remember { mutableStateOf<Set<KnowledgeEdgeType>?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = Dimens.Space3)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.Space2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            Text("KNOWLEDGE GRAPH", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = surfaceColors.textSecondary)
            Spacer(Modifier.weight(1f))
            KaiteyoPill(text = "All relations", selected = typeFilter == null, onClick = { typeFilter = null })
            KaiteyoPill(
                text = "Structure",
                selected = typeFilter?.contains(KnowledgeEdgeType.ComponentOf) == true,
                onClick = { typeFilter = setOf(KnowledgeEdgeType.ComponentOf, KnowledgeEdgeType.RadicalOf) }
            )
            KaiteyoPill(
                text = "Usage",
                selected = typeFilter?.contains(KnowledgeEdgeType.UsedIn) == true,
                onClick = { typeFilter = setOf(KnowledgeEdgeType.UsedIn, KnowledgeEdgeType.AppearsIn) }
            )
            KaiteyoPill(text = "Back to search", selected = false, onClick = onBackToSearch)
        }

        if (loading) {
            CenteredLoader()
            return@Column
        }

        if (graph.isEmpty) {
            KaiteyoEmptyState(icon = "🗺️", title = "Empty graph", message = "Nothing to show for this entry.")
            return@Column
        }

        val selectedNode = selected?.let { graph.node(it) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = Dimens.Space8),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space3)
        ) {
            if (selectedNode != null) {
                item {
                    GraphNodeCard(
                        node = selectedNode,
                        selected = true,
                        accent = accent.primary,
                        onClick = { onExpand(selectedNode.id, typeFilter) }
                    )
                }
                item {
                    Text(
                        text = "Neighbors — tap to focus (first tap expands)",
                        style = MaterialTheme.typography.labelSmall,
                        color = surfaceColors.textMuted
                    )
                }
            }
            val neighbors = selected?.let { graph.neighbors(it, typeFilter) }.orEmpty()
            if (neighbors.isNotEmpty()) {
                items(neighbors, key = { it.id }) { node ->
                    GraphNodeCard(
                        node = node,
                        selected = false,
                        accent = accent.secondary,
                        onClick = { onSelectNode(node.id) }
                    )
                }
            }
            if (neighbors.isEmpty() && selectedNode != null) {
                item {
                    KaiteyoEmptyState(
                        icon = "🧩",
                        title = "No neighbors for this filter",
                        message = "Clear the relationship filter or expand this node.",
                        actionLabel = "Expand",
                        onAction = { onExpand(selectedNode.id, null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GraphNodeCard(
    node: KnowledgeNode,
    selected: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(if (selected) accent.copy(alpha = 0.14f) else surfaceColors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        Text(
            text = node.label,
            fontSize = if (node.kind == ua.syt0r.kanji.core.knowledge.KnowledgeNodeKind.Kanji) 20.sp else 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = surfaceColors.textPrimary
        )
        Text(
            text = node.kind.label,
            style = MaterialTheme.typography.labelSmall,
            color = surfaceColors.textMuted
        )
        node.subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = surfaceColors.textSecondary,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
        }
    }
}

// ============================================================
// SHARED
// ============================================================

@Composable
private fun CenteredLoader() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: (() -> Unit)? = null
) {
    Box(Modifier.fillMaxSize().padding(Dimens.Space4), contentAlignment = Alignment.Center) {
        KaiteyoEmptyState(
            icon = "⚠️",
            title = "Something went wrong",
            message = message,
            actionLabel = if (onRetry != null) "Retry" else null,
            onAction = onRetry
        )
    }
}
