@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ua.syt0r.kanji.presentation.screen.main.screen.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.app_data.AppDataRepository
import ua.syt0r.kanji.core.app_data.WordClassification
import ua.syt0r.kanji.core.app_data.data.JapaneseWord
import ua.syt0r.kanji.core.knowledge.KnowledgeRepository
import ua.syt0r.kanji.core.knowledge.SentenceKnowledge
import ua.syt0r.kanji.core.knowledge.media.MediaReference
import ua.syt0r.kanji.core.knowledge.media.MediaReferenceKind
import ua.syt0r.kanji.core.knowledge.media.MediaReferenceStore
import ua.syt0r.kanji.core.srs.LetterPracticeType
import ua.syt0r.kanji.core.srs.LetterSrsDeck
import ua.syt0r.kanji.core.srs.LetterSrsDecksData
import ua.syt0r.kanji.core.srs.LetterSrsManager
import ua.syt0r.kanji.core.srs.VocabPracticeType
import ua.syt0r.kanji.core.srs.VocabSrsDeck
import ua.syt0r.kanji.core.srs.VocabSrsDecksData
import ua.syt0r.kanji.core.srs.VocabSrsManager
import ua.syt0r.kanji.presentation.common.ScreenLetterPracticeType
import ua.syt0r.kanji.presentation.common.ScreenVocabPracticeType
import ua.syt0r.kanji.presentation.common.ui.KaiteyoCountBadge
import ua.syt0r.kanji.presentation.common.ui.KaiteyoEmptyState
import ua.syt0r.kanji.presentation.common.ui.KaiteyoProgressRing
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoSemanticColors
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.features.ExamWorkspace
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoCollection
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoDataCenter
import ua.syt0r.kanji.presentation.screen.main.features.StatisticsController
import ua.syt0r.kanji.presentation.screen.main.features.StudyDecksSnapshot
import ua.syt0r.kanji.presentation.screen.main.features.resumeStudy
import ua.syt0r.kanji.presentation.screen.main.features.totalDue
import ua.syt0r.kanji.presentation.screen.main.features.totalNew
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.deck_picker.data.DeckPickerScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardFlagType
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KaiteyoCard
import ua.syt0r.kanji.presentation.screen.main.screen.library.KaiteyoLibraryMode
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenData
import ua.syt0r.kanji.presentation.screen.main.screen.info.toInfoScreenData
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.data.VocabPracticeScreenConfiguration

// ============================================
// LIBRARY — the single discovery surface
// One place for everything the learner owns
// and studies:
//   · unified search  → kanji, vocabulary, decks
//   · mode chips      → All · Decks · Kanji ·
//                       Vocabulary · Due · Favorites
//   · Manage menu     → deck/card/tag/flag/stats
//                       tools (one menu, not a
//                       wall of cards)
// ============================================

@Composable
fun LibraryScreen(navigationState: MainNavigationState) {
    val dataCenter = koinInject<KaiteyoDataCenter>()
    val letterSrsManager = koinInject<LetterSrsManager>()
    val vocabSrsManager = koinInject<VocabSrsManager>()
    val appDataRepository = koinInject<AppDataRepository>()
    LaunchedEffect(Unit) { dataCenter.ensureLoaded() }

    LibraryHub(
        navigationState = navigationState,
        dataCenter = dataCenter,
        letterSrsManager = letterSrsManager,
        vocabSrsManager = vocabSrsManager,
        appDataRepository = appDataRepository
    )
}

private enum class DeckCategory { Letters, Vocabulary }

private data class UnifiedDeck(
    val deckId: Long,
    val title: String,
    val category: DeckCategory,
    val lastReview: Instant?,
    val newCount: Int,
    val dueCount: Int,
    val totalCount: Int
) {
    /** Fraction of cards already introduced (not new) — a real progress signal. */
    val studiedFraction: Float
        get() = if (totalCount == 0) 0f else (totalCount - newCount).coerceAtLeast(0).toFloat() / totalCount
}

private enum class LibraryMode(val label: String) {
    All("All"),
    Decks("Decks"),
    Kanji("Kanji"),
    Vocabulary("Vocabulary"),
    Sentences("Sentences"),
    Grammar("Grammar"),
    Courses("Courses"),
    Kaiteyo("Kaiteyo"),
    Exams("Exams"),
    Due("Due"),
    Favorites("Favorites"),
    Media("Media")
}

// A single flat, keyboard-navigable view of every unified-search hit, in
// display order (decks → kanji → vocabulary) so arrow keys walk the same
// list the user sees.
private sealed interface SearchEntry {
    data class Deck(val deck: UnifiedDeck) : SearchEntry
    data class Kanji(val card: KaiteyoCard) : SearchEntry
    data class Vocab(val word: JapaneseWord) : SearchEntry
    data class Sentence(val sentence: SentenceKnowledge) : SearchEntry

    fun key(): String = when (this) {
        is Deck -> "deck-${deck.category}-${deck.deckId}"
        is Kanji -> "kanji-${card.id}"
        is Vocab -> "vocab-${word.id}"
        is Sentence -> "sentence-${sentence.text.hashCode()}"
    }
}

private fun SearchEntry.sectionTitle(): String = when (this) {
    is SearchEntry.Deck -> "DECKS"
    is SearchEntry.Kanji -> "KANJI"
    is SearchEntry.Vocab -> "VOCABULARY"
    is SearchEntry.Sentence -> "SENTENCES"
}

// LazyColumn index of the row for `selectedIndex` in the unified results list,
// accounting for the leading "Results for" item and per-section headers.
private fun searchListIndex(entries: List<SearchEntry>, selectedIndex: Int): Int {
    var index = 1 // item 0 is the "Results for" header
    var lastSection: String? = null
    entries.forEachIndexed { i, entry ->
        val section = entry.sectionTitle()
        if (section != lastSection) {
            index++
            lastSection = section
        }
        if (i == selectedIndex) return index
        index++
    }
    return index
}

@Composable
private fun LibraryHub(
    navigationState: MainNavigationState,
    dataCenter: KaiteyoDataCenter,
    letterSrsManager: LetterSrsManager,
    vocabSrsManager: VocabSrsManager,
    appDataRepository: AppDataRepository
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val statisticsController = koinInject<StatisticsController>()
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(LibraryMode.All) }

    // Unified search — while anything is typed, the Library body becomes
    // live results across kanji, vocabulary and decks (no separate screens).
    val knowledgeRepository = koinInject<KnowledgeRepository>()

    var query by remember { mutableStateOf("") }
    var vocabMatches by remember { mutableStateOf<List<JapaneseWord>>(emptyList()) }
    var vocabSearching by remember { mutableStateOf(false) }
    var sentenceMatches by remember { mutableStateOf<List<SentenceKnowledge>>(emptyList()) }
    var sentenceSearching by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.isBlank()) {
            vocabMatches = emptyList()
            vocabSearching = false
            sentenceMatches = emptyList()
            sentenceSearching = false
            return@LaunchedEffect
        }
        vocabSearching = true
        sentenceSearching = true
        delay(200) // debounce the dictionary query
        vocabMatches = appDataRepository.getWordsWithText(q, limit = 8)
        vocabSearching = false
        sentenceMatches = knowledgeRepository.sentencesWithText(q, limit = 5)
        sentenceSearching = false
    }

    val kanjiMatches by remember(dataCenter.cards, query) {
        derivedStateOf {
            val q = query.trim()
            if (q.isBlank()) emptyList()
            else dataCenter.cards.filter { card ->
                card.character.contains(q) ||
                    card.reading.contains(q, ignoreCase = true) ||
                    card.meaning.contains(q, ignoreCase = true)
            }.take(8)
        }
    }

    if (dataCenter.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading library…", color = surfaceColors.textMuted)
        }
        return
    }
    if (dataCenter.loadError) {
        val retryScope = rememberCoroutineScope()
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Could not load the library", color = surfaceColors.textMuted)
            TextButton(
                onClick = { retryScope.launch { dataCenter.retryLoad() } }
            ) {
                Text("Retry")
            }
        }
        return
    }

    val decksState by produceState<StudyDecksSnapshot?>(null, letterSrsManager, vocabSrsManager) {
        suspend fun reload() {
            value = StudyDecksSnapshot(
                letters = letterSrsManager.getDecks(),
                vocab = vocabSrsManager.getDecks()
            )
        }
        reload()
        launch {
            merge(letterSrsManager.dataChangeFlow, vocabSrsManager.dataChangeFlow)
                .collect { reload() }
        }
    }

    val letterDecks = decksState?.letters?.decks ?: emptyList()
    val vocabDecks = decksState?.vocab?.decks ?: emptyList()

    val totalNew = letterDecks.sumOf { it.totalNew() } + vocabDecks.sumOf { it.totalNew() }
    val totalDue = letterDecks.sumOf { it.totalDue() } + vocabDecks.sumOf { it.totalDue() }

    val unifiedDecks = buildList {
        letterDecks.forEach { add(it.toUnified(DeckCategory.Letters)) }
        vocabDecks.forEach { add(it.toUnified(DeckCategory.Vocabulary)) }
    }.sortedWith(
        compareByDescending<UnifiedDeck> { it.lastReview }
            .thenBy { it.title }
    )

    val deckMatches = remember(unifiedDecks, query) {
        val q = query.trim()
        if (q.isBlank()) emptyList()
        else unifiedDecks.filter { it.title.contains(q, ignoreCase = true) }.take(5)
    }

    // Flat, ordered list of every visible result — the single source of truth
    // for both the rendered sections and arrow-key navigation.
    val searchEntries = remember(deckMatches, kanjiMatches, vocabMatches, sentenceMatches) {
        buildList {
            deckMatches.forEach { add(SearchEntry.Deck(it)) }
            kanjiMatches.forEach { add(SearchEntry.Kanji(it)) }
            vocabMatches.forEach { add(SearchEntry.Vocab(it)) }
            sentenceMatches.forEach { add(SearchEntry.Sentence(it)) }
        }
    }
    var selectedIndex by remember { mutableStateOf(0) }

    val openKanji: (String) -> Unit = {
        navigationState.navigate(MainDestination.Info(InfoScreenData.Letter(it)))
    }
    val openVocab: (JapaneseWord) -> Unit = {
        navigationState.navigate(MainDestination.Info(it.toInfoScreenData()))
    }
    val openSentence: (SentenceKnowledge) -> Unit = {
        navigationState.navigate(MainDestination.SentenceEntry(it.text, it.translation))
    }
    val openDeck: (UnifiedDeck) -> Unit = { deck ->
        val configuration = when (deck.category) {
            DeckCategory.Letters -> DeckDetailsScreenConfiguration.LetterDeck(deck.deckId)
            DeckCategory.Vocabulary -> DeckDetailsScreenConfiguration.VocabDeck(deck.deckId)
        }
        navigationState.navigate(MainDestination.DeckDetails(configuration))
    }

    Column(Modifier.fillMaxSize()) {
        // ---- Header: title + Manage menu --------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Library",
                    color = surfaceColors.textPrimary,
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Search, browse and study — everything in one place",
                    color = surfaceColors.textMuted,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
            }
            ManageMenu(navigationState = navigationState, accent = accent, surfaceColors = surfaceColors)
        }

        // ---- Unified search field (always visible) --------------------
        // Arrow keys move the selection through the live results; Enter opens
        // the selected one; Escape clears the query. Attached to an ancestor
        // of the text field so it works the moment the field has focus.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> {
                            if (searchEntries.isNotEmpty()) {
                                selectedIndex = (selectedIndex + 1) % searchEntries.size
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            if (searchEntries.isNotEmpty()) {
                                selectedIndex = (selectedIndex - 1 + searchEntries.size) % searchEntries.size
                            }
                            true
                        }
                        Key.Enter -> {
                            val entry = searchEntries.getOrNull(selectedIndex)
                            if (entry != null) {
                                when (entry) {
                                    is SearchEntry.Deck -> openDeck(entry.deck)
                                    is SearchEntry.Kanji -> openKanji(entry.card.id)
                                    is SearchEntry.Vocab -> openVocab(entry.word)
                                    is SearchEntry.Sentence -> openSentence(entry.sentence)
                                }
                                true
                            } else {
                                false
                            }
                        }
                        Key.Escape -> {
                            if (query.isNotBlank()) {
                                query = ""
                                selectedIndex = 0
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                }
        ) {
            LibrarySearchField(
                value = query,
                onValueChange = { query = it; selectedIndex = 0 },
                placeholder = "Search kanji, vocabulary, decks…",
                accent = accent,
                surfaceColors = surfaceColors
            )
        }

        if (query.isNotBlank()) {
            // ---- Unified search results --------------------------------
            UnifiedSearchResults(
                dataCenter = dataCenter,
                query = query.trim(),
                entries = searchEntries,
                vocabSearching = vocabSearching || sentenceSearching,
                selectedIndex = selectedIndex,
                onOpenKanji = openKanji,
                onOpenVocab = openVocab,
                onOpenDeck = openDeck,
                onOpenSentence = openSentence,
                onOpenFullSearch = {
                    navigationState.navigate(MainDestination.SearchEngine)
                },
                onSuggestionClick = { query = it; selectedIndex = 0 },
                onClear = { query = ""; selectedIndex = 0 },
                accent = accent,
                surfaceColors = surfaceColors
            )
            return@Column
        }

        // ---- Mode chips ------------------------------------------------
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibraryMode.entries.forEach { candidate ->
                ModeChip(
                    label = candidate.label,
                    selected = mode == candidate,
                    onClick = { mode = candidate },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
            }
        }

        // ---- Mode content ----------------------------------------------
        when (mode) {
            LibraryMode.All -> LibraryList(
                navigationState = navigationState,
                dataCenter = dataCenter,
                unifiedDecks = unifiedDecks,
                totalNew = totalNew,
                totalDue = totalDue,
                decksState = decksState,
                accent = accent,
                surfaceColors = surfaceColors
            )

            LibraryMode.Decks -> DecksList(
                navigationState = navigationState,
                unifiedDecks = unifiedDecks,
                totalNew = totalNew,
                totalDue = totalDue,
                accent = accent,
                surfaceColors = surfaceColors
            )

            LibraryMode.Kanji -> KanjiBrowse(
                navigationState = navigationState,
                dataCenter = dataCenter,
                accent = accent,
                surfaceColors = surfaceColors
            )

            LibraryMode.Vocabulary -> VocabularyBrowse(
                navigationState = navigationState,
                appDataRepository = appDataRepository,
                accent = accent,
                surfaceColors = surfaceColors
            )

            LibraryMode.Due -> DueList(
                navigationState = navigationState,
                unifiedDecks = unifiedDecks,
                totalNew = totalNew,
                totalDue = totalDue,
                decksState = decksState,
                accent = accent,
                surfaceColors = surfaceColors
            )

            LibraryMode.Exams -> ExamWorkspace(
                controller = statisticsController,
                scope = scope
            )

            LibraryMode.Sentences -> SentencesBrowse(
                navigationState = navigationState,
                accent = accent,
                surfaceColors = surfaceColors
            )

            LibraryMode.Grammar -> GrammarBrowse(
                navigationState = navigationState,
                accent = accent,
                surfaceColors = surfaceColors
            )

            LibraryMode.Courses -> CoursesBrowse(
                navigationState = navigationState,
                accent = accent,
                surfaceColors = surfaceColors
            )

            LibraryMode.Kaiteyo -> KaiteyoLibraryMode(
                navigationState = navigationState,
                dataCenter = dataCenter,
                appDataRepository = appDataRepository,
                accent = accent,
                surfaceColors = surfaceColors
            )

            LibraryMode.Favorites -> FavoritesList(
                navigationState = navigationState,
                dataCenter = dataCenter,
                accent = accent,
                surfaceColors = surfaceColors
            )

            LibraryMode.Media -> MediaReferencesList(
                navigationState = navigationState,
                accent = accent,
                surfaceColors = surfaceColors
            )
        }
    }
}

// ============================================
// Unified search results
// ============================================

@Composable
private fun UnifiedSearchResults(
    dataCenter: KaiteyoDataCenter,
    query: String,
    entries: List<SearchEntry>,
    vocabSearching: Boolean,
    selectedIndex: Int,
    onOpenKanji: (String) -> Unit,
    onOpenVocab: (JapaneseWord) -> Unit,
    onOpenDeck: (UnifiedDeck) -> Unit,
    onOpenSentence: (SentenceKnowledge) -> Unit,
    onOpenFullSearch: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onClear: () -> Unit,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    val isEmpty = entries.isEmpty() && !vocabSearching

    // Keep the arrow-selected row visible as the selection moves.
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex, entries) {
        if (entries.isNotEmpty() && selectedIndex in entries.indices) {
            listState.animateScrollToItem(searchListIndex(entries, selectedIndex))
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "search-query") {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Results for \"$query\"",
                        color = surfaceColors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClear) {
                        Text("Clear", color = accent.primary, fontSize = 12.sp)
                    }
                }
                if (entries.isNotEmpty()) {
                    Text(
                        text = "↑/↓ navigate · Enter open · Esc clear",
                        color = surfaceColors.textMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        if (isEmpty) {
            item(key = "search-empty") {
                EmptySearchState(query = query, onSuggestionClick = onSuggestionClick, accent = accent, surfaceColors = surfaceColors)
            }
        } else {
            var lastSection: String? = null
            entries.forEachIndexed { index, entry ->
                val section = entry.sectionTitle()
                if (section != lastSection) {
                    item(key = "${entry.key()}-header") { ResultSectionTitle(section, accent, surfaceColors) }
                    lastSection = section
                }
                val selected = index == selectedIndex
                item(key = entry.key()) {
                    when (entry) {
                        is SearchEntry.Deck -> SearchDeckRow(
                            deck = entry.deck,
                            selected = selected,
                            onClick = { onOpenDeck(entry.deck) },
                            accent = accent,
                            surfaceColors = surfaceColors
                        )
                        is SearchEntry.Kanji -> KanjiRow(
                            card = entry.card,
                            dataCenter = dataCenter,
                            selected = selected,
                            onClick = { onOpenKanji(entry.card.id) },
                            accent = accent,
                            surfaceColors = surfaceColors
                        )
                        is SearchEntry.Vocab -> VocabRow(
                            word = entry.word,
                            selected = selected,
                            onClick = { onOpenVocab(entry.word) },
                            accent = accent,
                            surfaceColors = surfaceColors
                        )
                        is SearchEntry.Sentence -> SentenceRow(
                            sentence = entry.sentence,
                            selected = selected,
                            onClick = { onOpenSentence(entry.sentence) },
                            accent = accent,
                            surfaceColors = surfaceColors
                        )
                    }
                }
            }
            if (vocabSearching) {
                item(key = "vocab-loading") {
                    Text("Searching vocabulary…", color = surfaceColors.textMuted, fontSize = 12.sp)
                }
            }
            item(key = "open-full") {
                TextButton(
                    onClick = onOpenFullSearch,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open full search (words, sentences, radicals)", color = accent.primary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptySearchState(
    query: String,
    onSuggestionClick: (String) -> Unit,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColors.surface)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Nothing found for \"$query\"",
            color = surfaceColors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Try a kanji, a reading or an English meaning. Here are a few common searches:",
            color = surfaceColors.textMuted,
            fontSize = 12.sp
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("日", "食べる", "学校", "water", "JLPT").forEach { suggestion ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.primary.copy(alpha = 0.10f))
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(suggestion, color = accent.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun ResultSectionTitle(title: String, accent: KaiteyoAccentScheme, surfaceColors: SurfaceColors) {
    Text(
        text = title,
        color = accent.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 4.dp)
    )
}

// ============================================
// All / Decks modes
// ============================================

@Composable
private fun LibraryList(
    navigationState: MainNavigationState,
    dataCenter: KaiteyoDataCenter,
    unifiedDecks: List<UnifiedDeck>,
    totalNew: Int,
    totalDue: Int,
    decksState: StudyDecksSnapshot?,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "continue") {
            ContinueStudyingCard(
                totalNew = totalNew,
                totalDue = totalDue,
                onStudy = { resumeStudy(navigationState, decksState) },
                onCreateLetterDeck = {
                    navigationState.navigate(MainDestination.DeckPicker(DeckPickerScreenConfiguration.Letters))
                },
                onCreateVocabDeck = {
                    navigationState.navigate(MainDestination.DeckPicker(DeckPickerScreenConfiguration.Vocab))
                },
                accent = accent,
                surfaceColors = surfaceColors
            )
        }

        item(key = "stats") {
            val sem = LocalKaiteyoSemanticColors.current
            StatRow(
                items = listOf(
                    StatData("Decks", unifiedDecks.size, sem.info),
                    StatData("Kanji", dataCenter.cards.size, sem.success),
                    StatData("Favorites", dataCenter.favorites.value.size, sem.favoriteStar),
                    StatData("Reviews", dataCenter.totalReviews.value.toInt(), sem.new)
                ),
                accent = accent,
                surfaceColors = surfaceColors
            )
        }

        item(key = "decks-title") { SectionTitle("YOUR DECKS", accent, surfaceColors) }

        if (unifiedDecks.isEmpty()) {
            item(key = "decks-empty") {
                EmptyDecksCard(
                    onCreateLetterDeck = {
                        navigationState.navigate(MainDestination.DeckPicker(DeckPickerScreenConfiguration.Letters))
                    },
                    onCreateVocabDeck = {
                        navigationState.navigate(MainDestination.DeckPicker(DeckPickerScreenConfiguration.Vocab))
                    },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
            }
        } else {
            item(key = "decks-list") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    unifiedDecks.forEach { deck ->
                        DeckRow(
                            deck = deck,
                            onClick = {
                                val configuration = when (deck.category) {
                                    DeckCategory.Letters -> DeckDetailsScreenConfiguration.LetterDeck(deck.deckId)
                                    DeckCategory.Vocabulary -> DeckDetailsScreenConfiguration.VocabDeck(deck.deckId)
                                }
                                navigationState.navigate(MainDestination.DeckDetails(configuration))
                            },
                            accent = accent,
                            surfaceColors = surfaceColors
                        )
                    }
                }
            }
        }

        if (dataCenter.collections.isNotEmpty()) {
            item(key = "collections-title") { SectionTitle("COLLECTIONS", accent, surfaceColors) }
            item(key = "collections") {
                CollectionsSection(
                    collections = dataCenter.collections.toList(),
                    onOpenCollections = { navigationState.navigate(MainDestination.Collections) },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
            }
        }

        item(key = "spacer") { Spacer(Modifier.height(8.dp)) }
    }
}

private enum class DeckSort(val label: String) {
    Recent("Recent"),
    Name("Name"),
    MostDue("Most due"),
    MostNew("Most new")
}

private fun List<UnifiedDeck>.sortedByDeckSort(sort: DeckSort): List<UnifiedDeck> = when (sort) {
    DeckSort.Recent -> sortedWith(compareByDescending<UnifiedDeck> { it.lastReview }.thenBy { it.title })
    DeckSort.Name -> sortedBy { it.title.lowercase() }
    DeckSort.MostDue -> sortedWith(compareByDescending<UnifiedDeck> { it.dueCount }.thenBy { it.title })
    DeckSort.MostNew -> sortedWith(compareByDescending<UnifiedDeck> { it.newCount }.thenBy { it.title })
}

@Composable
private fun DecksList(
    navigationState: MainNavigationState,
    unifiedDecks: List<UnifiedDeck>,
    totalNew: Int,
    totalDue: Int,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    var deckSort by remember { mutableStateOf(DeckSort.Recent) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    val sortedDecks = remember(unifiedDecks, deckSort) { unifiedDecks.sortedByDeckSort(deckSort) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "decks-header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Decks",
                        color = surfaceColors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (totalNew + totalDue == 0) "All caught up"
                        else "$totalNew new · $totalDue due across ${unifiedDecks.size} decks",
                        color = surfaceColors.textMuted,
                        fontSize = 12.sp
                    )
                }
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(accent.primary.copy(alpha = 0.1f))
                            .clickable { sortMenuOpen = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Sort, null, tint = accent.primary, modifier = Modifier.size(15.dp))
                        Text(deckSort.label, color = accent.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                        DeckSort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label, fontSize = 13.sp) },
                                onClick = { deckSort = option; sortMenuOpen = false },
                                leadingIcon = {
                                    if (option == deckSort) {
                                        Icon(Icons.Outlined.Check, null, tint = accent.primary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        }
                    }
                }
                TextButton(onClick = {
                    navigationState.navigate(MainDestination.DeckPicker(DeckPickerScreenConfiguration.Letters))
                }) {
                    Text("+ Kanji", color = accent.primary, fontSize = 12.sp)
                }
                TextButton(onClick = {
                    navigationState.navigate(MainDestination.DeckPicker(DeckPickerScreenConfiguration.Vocab))
                }) {
                    Text("+ Vocab", color = accent.primary, fontSize = 12.sp)
                }
            }
        }

        if (sortedDecks.isEmpty()) {
            item(key = "decks-empty") {
                EmptyDecksCard(
                    onCreateLetterDeck = {
                        navigationState.navigate(MainDestination.DeckPicker(DeckPickerScreenConfiguration.Letters))
                    },
                    onCreateVocabDeck = {
                        navigationState.navigate(MainDestination.DeckPicker(DeckPickerScreenConfiguration.Vocab))
                    },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
            }
        } else {
            item(key = "decks-grid") {
                DeckCardGrid(
                    decks = sortedDecks,
                    onClick = { deck ->
                        val configuration = when (deck.category) {
                            DeckCategory.Letters -> DeckDetailsScreenConfiguration.LetterDeck(deck.deckId)
                            DeckCategory.Vocabulary -> DeckDetailsScreenConfiguration.VocabDeck(deck.deckId)
                        }
                        navigationState.navigate(MainDestination.DeckDetails(configuration))
                    },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
            }
        }
    }
}

// ============================================
// Deck card grid — the Library's deck presentation
// ============================================

@Composable
private fun DeckCardGrid(
    decks: List<UnifiedDeck>,
    onClick: (UnifiedDeck) -> Unit,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        decks.chunked(2).forEach { rowDecks ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowDecks.forEach { deck ->
                    DeckCard(
                        deck = deck,
                        onClick = { onClick(deck) },
                        accent = accent,
                        surfaceColors = surfaceColors,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowDecks.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DeckCard(
    deck: UnifiedDeck,
    onClick: () -> Unit,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val background by animateColorAsState(
        if (hovered) surfaceColors.surfaceInteractive else surfaceColors.surface
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (deck.category == DeckCategory.Letters) "字" else "語",
                    fontSize = 18.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = deck.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = surfaceColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = deck.categoryName(),
                    fontSize = 11.sp,
                    color = surfaceColors.textMuted
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            KaiteyoProgressRing(
                progress = deck.studiedFraction,
                size = 46.dp,
                strokeWidth = 5.dp,
                color = accent.primary
            ) {
                Text(
                    text = "${(deck.studiedFraction * 100).roundToInt()}%",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = surfaceColors.textPrimary
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (deck.newCount > 0 || deck.dueCount > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (deck.newCount > 0) {
                            KaiteyoCountBadge(deck.newCount, accent.primary)
                        }
                        if (deck.dueCount > 0) {
                            KaiteyoCountBadge(deck.dueCount, LocalKaiteyoSemanticColors.current.error)
                        }
                    }
                } else {
                    Text(
                        text = "Up to date",
                        fontSize = 11.sp,
                        color = surfaceColors.textMuted
                    )
                }
                Text(
                    text = if (deck.newCount + deck.dueCount > 0)
                        "${deck.newCount + deck.dueCount} cards ready today"
                    else "All caught up today",
                    fontSize = 10.sp,
                    color = surfaceColors.textMuted
                )
            }
        }
    }
}

// ============================================
// Kanji / Vocabulary browsing modes
// ============================================

@Composable
private fun KanjiBrowse(
    navigationState: MainNavigationState,
    dataCenter: KaiteyoDataCenter,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    var query by remember { mutableStateOf("") }
    var jlptLevels by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var grades by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var frequencyBand by remember { mutableStateOf<Int?>(null) }
    var selectedIndex by remember { mutableStateOf(0) }

    val hasFilters = jlptLevels.isNotEmpty() || grades.isNotEmpty() || frequencyBand != null

    val filtered by remember(dataCenter.cards, query, jlptLevels, grades, frequencyBand) {
        derivedStateOf {
            val q = query.trim()
            var list: List<KaiteyoCard> = if (q.isBlank()) dataCenter.cards
            else dataCenter.cards.filter { card ->
                card.character.contains(q) ||
                    card.reading.contains(q, ignoreCase = true) ||
                    card.meaning.contains(q, ignoreCase = true)
            }
            if (jlptLevels.isNotEmpty()) {
                list = list.filter { card ->
                    val classes = dataCenter.classifications[card.id].orEmpty()
                    jlptLevels.any { level -> classes.contains("n$level") }
                }
            }
            if (grades.isNotEmpty()) {
                list = list.filter { card ->
                    val classes = dataCenter.classifications[card.id].orEmpty()
                    grades.any { grade -> classes.contains("o$grade") }
                }
            }
            frequencyBand?.let { maxRank ->
                list = list.filter { (dataCenter.frequencies[it.id] ?: Int.MAX_VALUE) <= maxRank }
            }
            list.sortedBy { dataCenter.frequencies[it.id] ?: Int.MAX_VALUE }
        }
    }

    // Keep the arrow-selected row visible as the selection moves.
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex, filtered.size) {
        if (filtered.isNotEmpty()) {
            listState.animateScrollToItem(selectedIndex.coerceIn(0, filtered.lastIndex))
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Kanji",
                    color = surfaceColors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (hasFilters) "${filtered.size} kanji · ${jlptLevels.size + grades.size + (if (frequencyBand != null) 1 else 0)} filter${if (jlptLevels.size + grades.size + (if (frequencyBand != null) 1 else 0) == 1) "" else "s"} active"
                    else "${filtered.size} kanji · ordered by frequency",
                    color = surfaceColors.textMuted,
                    fontSize = 12.sp
                )
            }
            TextButton(onClick = { navigationState.navigate(MainDestination.KanjiBrowser()) }) {
                Text("Full browser", color = accent.primary, fontSize = 12.sp)
            }
        }

        // Arrow keys move the selection through the results; Enter opens the
        // selected one; Escape clears the query. Attached to an ancestor of the
        // text field so it works the moment the field has focus.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> {
                            if (filtered.isNotEmpty()) {
                                selectedIndex = (selectedIndex + 1) % filtered.size
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            if (filtered.isNotEmpty()) {
                                selectedIndex = (selectedIndex - 1 + filtered.size) % filtered.size
                            }
                            true
                        }
                        Key.Enter -> {
                            filtered.getOrNull(selectedIndex)?.let { card ->
                                navigationState.navigate(MainDestination.Info(InfoScreenData.Letter(card.id)))
                            }
                            filtered.isNotEmpty()
                        }
                        Key.Escape -> {
                            if (query.isNotBlank()) {
                                query = ""
                                selectedIndex = 0
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                }
        ) {
            LibrarySearchField(
                value = query,
                onValueChange = { query = it; selectedIndex = 0 },
                placeholder = "Filter kanji…",
                accent = accent,
                surfaceColors = surfaceColors
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterBarHeader(
                hasFilters = hasFilters,
                onClear = { jlptLevels = emptySet(); grades = emptySet(); frequencyBand = null; selectedIndex = 0 },
                accent = accent,
                surfaceColors = surfaceColors
            )
            LibraryFilterGroup("JLPT") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items((5 downTo 1).toList()) { level ->
                        LibraryFilterChip(
                            label = "N$level",
                            selected = level in jlptLevels,
                            onClick = {
                                jlptLevels = if (level in jlptLevels) jlptLevels - level else jlptLevels + level
                            },
                            accent = accent,
                            surfaceColors = surfaceColors
                        )
                    }
                }
            }
            LibraryFilterGroup("Grade") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(kanjiGradeOptions) { grade ->
                        LibraryFilterChip(
                            label = grade.label,
                            selected = grade.value in grades,
                            onClick = {
                                grades = if (grade.value in grades) grades - grade.value else grades + grade.value
                            },
                            accent = accent,
                            surfaceColors = surfaceColors
                        )
                    }
                }
            }
            LibraryFilterGroup("Frequency") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(frequencyBandOptions) { band ->
                        LibraryFilterChip(
                            label = band.label,
                            selected = frequencyBand == band.maxRank,
                            onClick = {
                                frequencyBand = if (frequencyBand == band.maxRank) null else band.maxRank
                            },
                            accent = accent,
                            surfaceColors = surfaceColors
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = when {
                        query.isNotBlank() -> "No kanji match \"$query\""
                        hasFilters -> "No kanji match these filters"
                        else -> "No kanji found"
                    },
                    color = surfaceColors.textMuted,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val selectedId = filtered.getOrNull(selectedIndex)?.id
                items(filtered, key = { it.id }) { card ->
                    KanjiRow(
                        card = card,
                        dataCenter = dataCenter,
                        selected = card.id == selectedId,
                        onClick = {
                            navigationState.navigate(MainDestination.Info(InfoScreenData.Letter(card.id)))
                        },
                        accent = accent,
                        surfaceColors = surfaceColors
                    )
                }
            }
        }
    }
}

@Composable
private fun VocabularyBrowse(
    navigationState: MainNavigationState,
    appDataRepository: AppDataRepository,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    var query by remember { mutableStateOf("") }
    var words by remember { mutableStateOf<List<JapaneseWord>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var jlptLevels by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var topics by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var loadingDeck by remember { mutableStateOf<String?>(null) }
    var selectedIndex by remember { mutableStateOf(0) }

    // Ids for each classification, loaded lazily from the bundled data — the
    // same source that powers the JLPT / topic import decks. A word belongs to
    // a filter if its id appears in any selected classification's card list.
    val deckIdSets = remember { mutableStateMapOf<String, Set<Long>>() }
    val scope = rememberCoroutineScope()

    fun loadDeck(classification: WordClassification) {
        if (classification.dbValue in deckIdSets) return
        scope.launch {
            loadingDeck = classification.dbValue
            deckIdSets[classification.dbValue] = appDataRepository
                .getImportDeckWords(classification.dbValue)
                .map { it.id }
                .toSet()
            loadingDeck = null
        }
    }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.isBlank()) {
            words = emptyList()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        delay(200)
        words = appDataRepository.getWordsWithText(q, limit = 50)
        searching = false
    }

    val hasFilters = jlptLevels.isNotEmpty() || topics.isNotEmpty()

    val activeDeckIds = remember(jlptLevels, topics, deckIdSets) {
        buildSet {
            jlptLevels.forEach { level -> deckIdSets["n$level"]?.let { addAll(it) } }
            topics.forEach { index -> deckIdSets["o$index"]?.let { addAll(it) } }
        }
    }

    val visibleWords = remember(words, activeDeckIds) {
        if (activeDeckIds.isEmpty()) words
        else words.filter { it.id in activeDeckIds }
    }

    // Keep the arrow-selected row visible as the selection moves.
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex, visibleWords.size) {
        if (visibleWords.isNotEmpty()) {
            listState.animateScrollToItem(selectedIndex.coerceIn(0, visibleWords.lastIndex))
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Vocabulary",
                    color = surfaceColors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (hasFilters) "${visibleWords.size} words · ${jlptLevels.size + topics.size} filter${if (jlptLevels.size + topics.size == 1) "" else "s"} active"
                    else "Search words, terms and readings",
                    color = surfaceColors.textMuted,
                    fontSize = 12.sp
                )
            }
            TextButton(onClick = { navigationState.navigate(MainDestination.SearchEngine) }) {
                Text("Full search", color = accent.primary, fontSize = 12.sp)
            }
        }

        // Arrow keys move the selection through the results; Enter opens the
        // selected one; Escape clears the query. Attached to an ancestor of the
        // text field so it works the moment the field has focus.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> {
                            if (visibleWords.isNotEmpty()) {
                                selectedIndex = (selectedIndex + 1) % visibleWords.size
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            if (visibleWords.isNotEmpty()) {
                                selectedIndex = (selectedIndex - 1 + visibleWords.size) % visibleWords.size
                            }
                            true
                        }
                        Key.Enter -> {
                            visibleWords.getOrNull(selectedIndex)?.let { word ->
                                navigationState.navigate(MainDestination.Info(word.toInfoScreenData()))
                            }
                            visibleWords.isNotEmpty()
                        }
                        Key.Escape -> {
                            if (query.isNotBlank()) {
                                query = ""
                                selectedIndex = 0
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                }
        ) {
            LibrarySearchField(
                value = query,
                onValueChange = { query = it; selectedIndex = 0 },
                placeholder = "Search vocabulary…",
                accent = accent,
                surfaceColors = surfaceColors
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterBarHeader(
                hasFilters = hasFilters,
                onClear = { jlptLevels = emptySet(); topics = emptySet(); selectedIndex = 0 },
                accent = accent,
                surfaceColors = surfaceColors
            )
            LibraryFilterGroup("JLPT") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items((5 downTo 1).toList()) { level ->
                        LibraryFilterChip(
                            label = "N$level",
                            selected = level in jlptLevels,
                            onClick = {
                                if (level in jlptLevels) {
                                    jlptLevels = jlptLevels - level
                                } else {
                                    jlptLevels = jlptLevels + level
                                    loadDeck(WordClassification.JLPT(level))
                                }
                            },
                            accent = accent,
                            surfaceColors = surfaceColors
                        )
                    }
                }
            }
            LibraryFilterGroup("Topic") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(vocabTopicOptions) { topic ->
                        LibraryFilterChip(
                            label = topic.label,
                            selected = topic.index in topics,
                            onClick = {
                                if (topic.index in topics) {
                                    topics = topics - topic.index
                                } else {
                                    topics = topics + topic.index
                                    loadDeck(WordClassification.Other(topic.index))
                                }
                            },
                            accent = accent,
                            surfaceColors = surfaceColors
                        )
                    }
                }
            }
            if (loadingDeck != null) {
                Text(
                    text = "Loading filter…",
                    color = surfaceColors.textMuted,
                    fontSize = 11.sp
                )
            }
        }

        when {
            searching -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Searching…", color = surfaceColors.textMuted, fontSize = 13.sp)
            }
            query.isBlank() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Type a word, reading or meaning — for example 食べる, たべる or \"to eat\".",
                    color = surfaceColors.textMuted,
                    fontSize = 13.sp
                )
            }
            visibleWords.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = if (hasFilters) "No words match these filters" else "No words match \"$query\"",
                    color = surfaceColors.textMuted,
                    fontSize = 13.sp
                )
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val selectedWordId = visibleWords.getOrNull(selectedIndex)?.id
                items(visibleWords, key = { it.id }) { word ->
                    VocabRow(
                        word = word,
                        selected = word.id == selectedWordId,
                        onClick = {
                            navigationState.navigate(MainDestination.Info(word.toInfoScreenData()))
                        },
                        accent = accent,
                        surfaceColors = surfaceColors
                    )
                }
            }
        }
    }
}

// ============================================
// Filter chips (in-place browsers)
// ============================================

private data class GradeOption(val value: Int, val label: String)

private val kanjiGradeOptions = listOf(
    GradeOption(1, "1"), GradeOption(2, "2"), GradeOption(3, "3"),
    GradeOption(4, "4"), GradeOption(5, "5"), GradeOption(6, "6"),
    GradeOption(8, "Secondary"), GradeOption(9, "Names"), GradeOption(10, "Names Var.")
)

private data class FrequencyBandOption(val maxRank: Int, val label: String)

private val frequencyBandOptions = listOf(
    FrequencyBandOption(500, "Top 500"),
    FrequencyBandOption(1000, "Top 1000"),
    FrequencyBandOption(2000, "Top 2000"),
    FrequencyBandOption(5000, "Top 5000")
)

private data class VocabTopicOption(val index: Int, val label: String)

private val vocabTopicOptions = listOf(
    VocabTopicOption(1, "Time"), VocabTopicOption(2, "Week"), VocabTopicOption(3, "Verbs"),
    VocabTopicOption(4, "Colors"), VocabTopicOption(5, "Food"), VocabTopicOption(6, "Japanese Food"),
    VocabTopicOption(7, "Grammar"), VocabTopicOption(8, "Animals"), VocabTopicOption(9, "Body"),
    VocabTopicOption(10, "Places"), VocabTopicOption(11, "Cities"), VocabTopicOption(12, "Transport")
)

@Composable
private fun FilterBarHeader(
    hasFilters: Boolean,
    onClear: () -> Unit,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "FILTERS",
            color = accent.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.weight(1f))
        if (hasFilters) {
            TextButton(
                onClick = onClear,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text("Clear", color = accent.primary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun LibraryFilterGroup(
    title: String,
    content: @Composable () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            color = surfaceColors.textMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        content()
    }
}

@Composable
private fun LibraryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    val bg by animateColorAsState(
        targetValue = if (selected) accent.primary.copy(alpha = 0.14f) else surfaceColors.surfaceInteractive,
        label = "filterChipBg"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, if (selected) accent.primary.copy(alpha = 0.4f) else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (selected) accent.primary else surfaceColors.textSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

// ============================================
// Due / Favorites modes
// ============================================

@Composable
private fun DueList(
    navigationState: MainNavigationState,
    unifiedDecks: List<UnifiedDeck>,
    totalNew: Int,
    totalDue: Int,
    decksState: StudyDecksSnapshot?,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    val dueDecks = unifiedDecks
        .filter { it.newCount > 0 || it.dueCount > 0 }
        .sortedWith(compareByDescending<UnifiedDeck> { it.dueCount }.thenByDescending { it.newCount })

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "due-header") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Due",
                    color = surfaceColors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                ContinueStudyingCard(
                    totalNew = totalNew,
                    totalDue = totalDue,
                    onStudy = { resumeStudy(navigationState, decksState) },
                    onCreateLetterDeck = {
                        navigationState.navigate(MainDestination.DeckPicker(DeckPickerScreenConfiguration.Letters))
                    },
                    onCreateVocabDeck = {
                        navigationState.navigate(MainDestination.DeckPicker(DeckPickerScreenConfiguration.Vocab))
                    },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
            }
        }

        if (dueDecks.isEmpty()) {
            item(key = "due-empty") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(surfaceColors.surface)
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "All caught up 🎉",
                        color = surfaceColors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Nothing is due today. Study some new cards or browse content to keep building your knowledge.",
                        color = surfaceColors.textMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            item(key = "due-title") { SectionTitle("WITH DUE OR NEW CARDS", accent, surfaceColors) }
            items(dueDecks, key = { "due-${it.category}-${it.deckId}" }) { deck ->
                DeckRow(
                    deck = deck,
                    onClick = {
                        val configuration = when (deck.category) {
                            DeckCategory.Letters -> DeckDetailsScreenConfiguration.LetterDeck(deck.deckId)
                            DeckCategory.Vocabulary -> DeckDetailsScreenConfiguration.VocabDeck(deck.deckId)
                        }
                        navigationState.navigate(MainDestination.DeckDetails(configuration))
                    },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
            }
        }
    }
}

@Composable
private fun FavoritesList(
    navigationState: MainNavigationState,
    dataCenter: KaiteyoDataCenter,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    val favoriteCards = remember(dataCenter.favorites.value) {
        dataCenter.favorites.value.mapNotNull { dataCenter.cardById(it) }
            .sortedBy { it.id }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "fav-header") {
            Text(
                text = "Favorites",
                color = surfaceColors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (favoriteCards.isEmpty()) {
            item(key = "fav-empty") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(surfaceColors.surface)
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No favorites yet",
                        color = surfaceColors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Tap the ★ on any kanji or word detail page to keep it here.",
                        color = surfaceColors.textMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(favoriteCards, key = { it.id }) { card ->
                KanjiRow(
                    card = card,
                    dataCenter = dataCenter,
                    onClick = {
                        navigationState.navigate(MainDestination.Info(InfoScreenData.Letter(card.id)))
                    },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
            }
        }
    }
}

// ============================================
// Shared pieces
// ============================================

@Composable
private fun LibrarySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(surfaceColors.surfaceInteractive)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = surfaceColors.textMuted,
            modifier = Modifier.size(19.dp)
        )
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = surfaceColors.textPrimary,
                fontSize = 14.sp
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(accent.primary),
            singleLine = true,
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = surfaceColors.textMuted, fontSize = 14.sp)
                }
                inner()
            }
        )
        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(22.dp)) {
                Icon(Icons.Default.Close, "Clear", tint = surfaceColors.textMuted, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val background by animateColorAsState(
        targetValue = when {
            selected -> accent.primary.copy(alpha = 0.16f)
            hovered -> surfaceColors.surfaceInteractive
            else -> surfaceColors.surface
        },
        label = "modeChipBg"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (selected) accent.primary else surfaceColors.textSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ManageMenu(
    navigationState: MainNavigationState,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    var open by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.MoreVert, "Manage", tint = surfaceColors.textSecondary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ManageItem("Deck Browser", "Create, rename, merge & delete decks", { open = false; navigationState.navigate(MainDestination.DeckBrowser) })
            ManageItem("Card Browser", "Search, filter & bulk-edit every card", { open = false; navigationState.navigate(MainDestination.CardBrowser()) })
            ManageItem("Tags", "Organize cards with nested tags", { open = false; navigationState.navigate(MainDestination.TagManager) })
            ManageItem("Flags", "Mark and filter flagged cards", { open = false; navigationState.navigate(MainDestination.FlagManager) })
            ManageItem("Statistics", "Study history, streaks & progress charts", { open = false; navigationState.navigate(MainDestination.StatisticsDashboard) })
            ManageItem("Import / Export", "Move decks and cards in or out of Kaiteyo", { open = false; navigationState.navigate(MainDestination.ImportExport) })
            ManageItem("Collections", "Smart collections & filtered decks", { open = false; navigationState.navigate(MainDestination.Collections) })
        }
    }
}

@Composable
private fun ManageItem(
    label: String,
    description: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Column {
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(description, fontSize = 11.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        onClick = onClick
    )
}

// ============================================
// Row composables
// ============================================

@Composable
private fun SearchDeckRow(
    deck: UnifiedDeck,
    selected: Boolean = false,
    onClick: () -> Unit,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val background by animateColorAsState(
        when {
            selected -> accent.primary.copy(alpha = 0.16f)
            hovered -> surfaceColors.surfaceInteractive
            else -> surfaceColors.surface
        }
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(if (deck.category == DeckCategory.Letters) "字" else "語", fontSize = 15.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = deck.title,
                color = surfaceColors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = deck.categoryName(),
                color = surfaceColors.textMuted,
                fontSize = 11.sp
            )
        }
        if (deck.newCount > 0 || deck.dueCount > 0) {
            MiniCountBadge(deck.newCount + deck.dueCount, accent.primary)
        }
    }
}

@Composable
private fun KanjiRow(
    card: KaiteyoCard,
    dataCenter: KaiteyoDataCenter,
    selected: Boolean = false,
    onClick: () -> Unit,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val background by animateColorAsState(
        when {
            selected -> accent.primary.copy(alpha = 0.16f)
            hovered -> surfaceColors.surfaceInteractive
            else -> surfaceColors.surface
        }
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = card.character,
            fontSize = 22.sp,
            color = surfaceColors.textPrimary,
            modifier = Modifier.width(40.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = card.reading.ifBlank { "—" },
                color = surfaceColors.textSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = card.meaning.take(60).ifBlank { "No meaning" },
                color = surfaceColors.textMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            dataCenter.classifications[card.id].orEmpty()
                .firstOrNull { it.startsWith("n") }
                ?.let { level ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accent.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(level.uppercase(), color = accent.primary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            card.flag.takeIf { it != CardFlagType.None }?.let { flag ->
                Box(Modifier.size(8.dp).clip(CircleShape).background(flag.colorFromHex()))
            }
            if (card.isFavorite) {
                Text("★", color = LocalKaiteyoSemanticColors.current.favoriteStar, fontSize = 14.sp)
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = surfaceColors.textMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SentenceRow(
    sentence: SentenceKnowledge,
    selected: Boolean = false,
    onClick: () -> Unit,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val background by animateColorAsState(
        when {
            selected -> accent.primary.copy(alpha = 0.16f)
            hovered -> surfaceColors.surfaceInteractive
            else -> surfaceColors.surface
        }
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = sentence.text,
                color = surfaceColors.textPrimary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sentence.translation.ifBlank { "No translation in the bundled corpus" },
                color = surfaceColors.textMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = surfaceColors.textMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun VocabRow(
    word: JapaneseWord,
    selected: Boolean = false,
    onClick: () -> Unit,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val background by animateColorAsState(
        when {
            selected -> accent.primary.copy(alpha = 0.16f)
            hovered -> surfaceColors.surfaceInteractive
            else -> surfaceColors.surface
        }
    )
    val expression = word.reading.kanjiReading ?: word.reading.kanaReading

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = expression,
                color = surfaceColors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${word.reading.kanaReading} · ${word.combinedGlossary().take(50)}",
                color = surfaceColors.textMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = surfaceColors.textMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ============================================
// Legacy pieces preserved from the original hub
// ============================================

private fun LetterSrsDeck.toUnified(category: DeckCategory) = UnifiedDeck(
    deckId = id,
    title = title,
    category = category,
    lastReview = lastReview,
    newCount = totalNew(),
    dueCount = totalDue(),
    totalCount = items.size
)

private fun VocabSrsDeck.toUnified(category: DeckCategory) = UnifiedDeck(
    deckId = id,
    title = title,
    category = category,
    lastReview = lastReview,
    newCount = totalNew(),
    dueCount = totalDue(),
    totalCount = items.size
)

@Composable
private fun ContinueStudyingCard(
    totalNew: Int,
    totalDue: Int,
    onStudy: () -> Unit,
    onCreateLetterDeck: () -> Unit,
    onCreateVocabDeck: () -> Unit,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val totalReady = totalNew + totalDue

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        if (hovered) accent.primary.copy(alpha = 0.2f) else accent.primary.copy(alpha = 0.15f),
                        accent.primary.copy(alpha = 0.05f)
                    )
                )
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onStudy)
            .hoverable(interactionSource)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = accent.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Continue Studying",
                    color = surfaceColors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (totalReady == 0) "All caught up — nothing due today"
                    else "$totalReady cards ready for today",
                    color = surfaceColors.textMuted,
                    fontSize = 12.sp
                )
            }
        }

        if (totalReady == 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCreateLetterDeck) {
                    Text("New Kanji deck", color = accent.primary, fontSize = 13.sp)
                }
                TextButton(onClick = onCreateVocabDeck) {
                    Text("New Vocab deck", color = accent.primary, fontSize = 13.sp)
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CountChip(
                    label = "New",
                    count = totalNew,
                    color = accent.primary,
                    surfaceColors = surfaceColors
                )
                CountChip(
                    label = "Due",
                    count = totalDue,
                    color = LocalKaiteyoSemanticColors.current.error,
                    surfaceColors = surfaceColors
                )
                Spacer(Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Study now",
                        color = accent.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = accent.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CountChip(
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color,
    surfaceColors: SurfaceColors
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Text(
            text = "$count $label",
            color = surfaceColors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyDecksCard(
    onCreateLetterDeck: () -> Unit,
    onCreateVocabDeck: () -> Unit,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColors.surface)
            .padding(horizontal = 18.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No decks yet",
            color = surfaceColors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Create a kanji or vocabulary deck to start studying.",
            color = surfaceColors.textMuted,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCreateLetterDeck) {
                Icon(Icons.Default.Add, null, Modifier.size(16.dp), tint = accent.primary)
                Spacer(Modifier.size(4.dp))
                Text("Kanji deck", color = accent.primary, fontSize = 13.sp)
            }
            TextButton(onClick = onCreateVocabDeck) {
                Icon(Icons.Default.Add, null, Modifier.size(16.dp), tint = accent.primary)
                Spacer(Modifier.size(4.dp))
                Text("Vocab deck", color = accent.primary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun DeckRow(
    deck: UnifiedDeck,
    onClick: () -> Unit,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val background by animateColorAsState(
        if (hovered) surfaceColors.surfaceInteractive else surfaceColors.surface
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (deck.category == DeckCategory.Letters) "字" else "語",
                fontSize = 18.sp
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = deck.title,
                color = surfaceColors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = deck.categoryName(),
                color = surfaceColors.textMuted,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(6.dp))
            // Real progress: share of cards no longer "new" (introduced).
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(surfaceColors.surfaceInteractive)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(deck.studiedFraction.coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent.primary)
                )
            }
        }
        if (deck.newCount > 0 || deck.dueCount > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (deck.newCount > 0) {
                    MiniCountBadge(deck.newCount, accent.primary)
                }
                if (deck.dueCount > 0) {
                    MiniCountBadge(deck.dueCount, LocalKaiteyoSemanticColors.current.error)
                }
            }
        } else {
            Text(
                text = "Up to date",
                color = surfaceColors.textMuted,
                fontSize = 11.sp
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = surfaceColors.textMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun UnifiedDeck.categoryName(): String = when (category) {
    DeckCategory.Letters -> "Kanji deck"
    DeckCategory.Vocabulary -> "Vocab deck"
}

@Composable
private fun MiniCountBadge(count: Int, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = count.toString(),
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CollectionsSection(
    collections: List<KaiteyoCollection>,
    onOpenCollections: () -> Unit,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    if (collections.isEmpty()) {
        Text(
            text = "No collections yet — flag or favorite kanji to build them.",
            color = surfaceColors.textMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        return
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        collections.take(8).forEach { collection ->
            val interactionSource = remember { MutableInteractionSource() }
            val hovered by interactionSource.collectIsHoveredAsState()
            val background by animateColorAsState(
                if (hovered) surfaceColors.surfaceInteractive else surfaceColors.surface
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(background)
                    .clickable(interactionSource = interactionSource, indication = null, onClick = onOpenCollections)
                    .hoverable(interactionSource)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(collection.icon, fontSize = 15.sp)
                Text(
                    text = collection.name,
                    color = surfaceColors.textPrimary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = collection.cardIds.size.toString(),
                    color = accent.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private data class StatData(
    val label: String,
    val value: Int,
    val color: androidx.compose.ui.graphics.Color = ua.syt0r.kanji.presentation.common.theme.semanticSuccess
)

@Composable
private fun StatRow(
    items: List<StatData>,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(surfaceColors.surface)
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(item.color)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = item.value.toString(),
                    color = item.color,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.label,
                    color = surfaceColors.textMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ============================================
// Media references — real "found in your media" content (spec §28, §29)
// ============================================

/**
 * Every Japanese text occurrence the user recorded in media (bookmarks /
 * mined subtitle cues), newest first. Tapping a single-kanji reference opens
 * the kanji entry; longer text opens the interactive sentence view. Only
 * real recorded references are shown — nothing is synthesized.
 */
@Composable
private fun MediaReferencesList(
    navigationState: MainNavigationState,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    val store = koinInject<MediaReferenceStore>()
    val references by produceState(initialValue = emptyList<MediaReference>()) {
        value = store.load().references
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "media-header") {
            Text(
                text = "Media",
                color = surfaceColors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (references.isEmpty()) {
            item(key = "media-empty") {
                KaiteyoEmptyState(
                    icon = "🎬",
                    title = "No media references yet",
                    message = "Bookmark or mine Japanese text in the Media Centre and it will appear here."
                )
            }
            return@LazyColumn
        }

        item(key = "media-count") {
            Text(
                text = "${references.size} reference${if (references.size == 1) "" else "s"} from your media",
                color = surfaceColors.textMuted,
                fontSize = 12.sp
            )
        }

        items(references.size, key = { "media-${it}-${references[it].timestampMs}" }) { index ->
            val reference = references[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(surfaceColors.surface)
                    .clickable {
                        if (reference.text.length == 1) {
                            navigationState.navigate(MainDestination.KanjiEntry(reference.text))
                        } else {
                            // Honest entry: the recorded text with no invented
                            // translation — the sentence view renders tokens.
                            navigationState.navigate(MainDestination.SentenceEntry(reference.text, ""))
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (reference.text.length == 1) reference.text else "文",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent.primary
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = reference.text,
                        color = surfaceColors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = reference.title,
                        color = surfaceColors.textMuted,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = mediaReferenceKindLabel(reference),
                    color = surfaceColors.textMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun mediaReferenceKindLabel(reference: MediaReference): String = when (reference.kind) {
    MediaReferenceKind.Mined -> "Mined"
    MediaReferenceKind.Bookmark -> "Bookmark"
    MediaReferenceKind.Subtitle -> "Cue"
}

@Composable
private fun SectionTitle(title: String, accent: KaiteyoAccentScheme, surfaceColors: SurfaceColors) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(accent.primary))
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            color = accent.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
