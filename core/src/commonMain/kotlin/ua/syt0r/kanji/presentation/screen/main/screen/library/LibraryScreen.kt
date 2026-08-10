@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ua.syt0r.kanji.presentation.screen.main.screen.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.koin.compose.koinInject
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
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoCollection
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoDataCenter
import ua.syt0r.kanji.presentation.screen.main.screen.deck_details.data.DeckDetailsScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.deck_picker.data.DeckPickerScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.letters_dashboard.LettersDashboardScreen
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.search.SearchScreen
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.vocab_dashboard.VocabDashboardScreen
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_vocab.data.VocabPracticeScreenConfiguration

// ============================================
// LIBRARY — the central hub
// Users immediately understand:
//   · what they own      → decks, collections, stats
//   · what they study    → due/new counts everywhere
//   · where to continue  → Continue Studying + deck rows
// ============================================

@Composable
fun LibraryScreen(navigationState: MainNavigationState) {
    val dataCenter = koinInject<KaiteyoDataCenter>()
    val letterSrsManager = koinInject<LetterSrsManager>()
    val vocabSrsManager = koinInject<VocabSrsManager>()
    LaunchedEffect(Unit) { dataCenter.ensureLoaded() }

    var view by remember { mutableStateOf<LibraryView>(LibraryView.Hub) }

    when (view) {
        LibraryView.Hub -> LibraryHub(
            navigationState = navigationState,
            dataCenter = dataCenter,
            letterSrsManager = letterSrsManager,
            vocabSrsManager = vocabSrsManager,
            onOpenKanjiDecks = { view = LibraryView.KanjiDecks },
            onOpenVocab = { view = LibraryView.Vocabulary },
            onOpenWordSearch = { view = LibraryView.WordSearch }
        )
        LibraryView.KanjiDecks -> DrillDownScaffold(title = "字  Kanji Decks", onBack = { view = LibraryView.Hub }) {
            LettersDashboardScreen(mainNavigationState = navigationState)
        }
        LibraryView.Vocabulary -> DrillDownScaffold(title = "語  Vocabulary", onBack = { view = LibraryView.Hub }) {
            VocabDashboardScreen(mainNavigationState = navigationState)
        }
        LibraryView.WordSearch -> DrillDownScaffold(title = "🔎  Word & Sentence Search", onBack = { view = LibraryView.Hub }) {
            SearchScreen(mainNavigationState = navigationState)
        }
    }
}

private sealed interface LibraryView {
    data object Hub : LibraryView
    data object KanjiDecks : LibraryView
    data object Vocabulary : LibraryView
    data object WordSearch : LibraryView
}

private enum class DeckCategory { Letters, Vocabulary }

private data class UnifiedDeck(
    val deckId: Long,
    val title: String,
    val category: DeckCategory,
    val lastReview: Instant?,
    val newCount: Int,
    val dueCount: Int
)

private data class LibraryDecksState(
    val letters: LetterSrsDecksData,
    val vocab: VocabSrsDecksData
)

@Composable
private fun DrillDownScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = surfaceColors.textSecondary)
            }
            Text(
                text = title,
                color = surfaceColors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(Modifier.fillMaxSize()) { content() }
    }
}

@Composable
private fun LibraryHub(
    navigationState: MainNavigationState,
    dataCenter: KaiteyoDataCenter,
    letterSrsManager: LetterSrsManager,
    vocabSrsManager: VocabSrsManager,
    onOpenKanjiDecks: () -> Unit,
    onOpenVocab: () -> Unit,
    onOpenWordSearch: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    var radicalCount by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        radicalCount = runCatching { dataCenter.loadRadicals().size }.getOrNull()
    }

    val decksState by produceState<LibraryDecksState?>(null, letterSrsManager, vocabSrsManager) {
        suspend fun reload() {
            value = LibraryDecksState(
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

    if (dataCenter.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading library…", color = surfaceColors.textMuted)
        }
        return
    }
    if (dataCenter.loadError) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Could not load the library", color = surfaceColors.textMuted)
        }
        return
    }

    val letterDecks = decksState?.letters?.decks ?: emptyList()
    val vocabDecks = decksState?.vocab?.decks ?: emptyList()

    val totalNew = letterDecks.sumOf { it.totalNew() } + vocabDecks.sumOf { it.totalNew() }
    val totalDue = letterDecks.sumOf { it.totalDue() } + vocabDecks.sumOf { it.totalDue() }
    val totalReady = totalNew + totalDue

    val unifiedDecks = buildList {
        letterDecks.forEach { add(it.toUnified(DeckCategory.Letters)) }
        vocabDecks.forEach { add(it.toUnified(DeckCategory.Vocabulary)) }
    }.sortedWith(
        compareByDescending<UnifiedDeck> { it.lastReview }
            .thenBy { it.title }
    )

    val favorites = dataCenter.favorites.value.size
    val collections = dataCenter.collections.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "header") {
            Column(Modifier.padding(top = 8.dp, bottom = 2.dp)) {
                Text(
                    text = "Library",
                    color = surfaceColors.textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Everything you own and study — in one place",
                    color = surfaceColors.textMuted,
                    fontSize = 13.sp
                )
            }
        }

        item(key = "search") {
            SearchBarCard(
                onClick = { navigationState.navigate(MainDestination.SearchEngine) },
                accent = accent,
                surfaceColors = surfaceColors
            )
        }

        item(key = "continue") {
            ContinueStudyingCard(
                totalNew = totalNew,
                totalDue = totalDue,
                onStudy = {
                    startStudy(
                        navigationState = navigationState,
                        decksState = decksState
                    )
                },
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
            StatRow(
                items = listOf(
                    StatData("Decks", unifiedDecks.size),
                    StatData("Kanji", dataCenter.cards.size),
                    StatData("Favorites", favorites),
                    StatData("Reviews", dataCenter.totalReviews.value.toInt())
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
                    unifiedDecks.take(6).forEach { deck ->
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
                    if (unifiedDecks.size > 6) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(
                                onClick = { navigationState.navigate(MainDestination.DeckBrowser) }
                            ) {
                                Text(
                                    text = "Browse all ${unifiedDecks.size} decks",
                                    color = accent.primary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "manage-title") { SectionTitle("MANAGE", accent, surfaceColors) }

        item(key = "manage-grid") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionCard(
                    glyph = "🗂",
                    title = "Deck Browser",
                    subtitle = "Create, rename, merge, archive & delete decks",
                    onClick = { navigationState.navigate(MainDestination.DeckBrowser) },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
                SectionCard(
                    glyph = "🃏",
                    title = "Card Browser",
                    subtitle = "Search, filter & bulk-edit every card",
                    onClick = { navigationState.navigate(MainDestination.CardBrowser) },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
                SectionCard(
                    glyph = "#",
                    title = "Tags",
                    subtitle = "Organize cards with nested tags",
                    count = dataCenter.tags.size,
                    onClick = { navigationState.navigate(MainDestination.TagManager) },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
                SectionCard(
                    glyph = "🚩",
                    title = "Flags",
                    subtitle = "Mark and filter flagged cards",
                    count = dataCenter.flags.size,
                    onClick = { navigationState.navigate(MainDestination.FlagManager) },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
                SectionCard(
                    glyph = "📊",
                    title = "Statistics",
                    subtitle = "Study history, streaks & progress charts",
                    onClick = { navigationState.navigate(MainDestination.StatisticsDashboard) },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
                SectionCard(
                    glyph = "⇄",
                    title = "Import / Export",
                    subtitle = "Move decks and cards in or out of Kaiteyo",
                    onClick = { navigationState.navigate(MainDestination.ImportExport) },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
            }
        }

        item(key = "study-title") { SectionTitle("STUDY", accent, surfaceColors) }

        item(key = "study-items") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionCard(
                    glyph = "字",
                    title = "Kanji",
                    subtitle = "Browse, filter & review all kanji",
                    count = dataCenter.cards.size,
                    onClick = { navigationState.navigate(MainDestination.KanjiBrowser()) },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
                SectionCard(
                    glyph = "📚",
                    title = "Kanji Decks",
                    subtitle = "Letter decks & spaced repetition",
                    onClick = onOpenKanjiDecks,
                    accent = accent,
                    surfaceColors = surfaceColors
                )
                SectionCard(
                    glyph = "語",
                    title = "Vocabulary",
                    subtitle = "Words, terms & vocab decks",
                    onClick = onOpenVocab,
                    accent = accent,
                    surfaceColors = surfaceColors
                )
                SectionCard(
                    glyph = "文",
                    title = "Grammar",
                    subtitle = "Particles & grammar terms",
                    onClick = onOpenVocab,
                    accent = accent,
                    surfaceColors = surfaceColors
                )
                SectionCard(
                    glyph = "例",
                    title = "Sentences",
                    subtitle = "Example sentences (Tatoeba)",
                    onClick = onOpenWordSearch,
                    accent = accent,
                    surfaceColors = surfaceColors
                )
                SectionCard(
                    glyph = "部",
                    title = "Radicals",
                    subtitle = radicalCount?.let { "$it radicals — search by parts" }
                        ?: "Search by radical parts",
                    onClick = { navigationState.navigate(MainDestination.KanjiBrowser()) },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
            }
        }

        item(key = "collections-title") { SectionTitle("COLLECTIONS", accent, surfaceColors) }

        item(key = "collections") {
            CollectionsSection(
                collections = dataCenter.collections.toList(),
                onOpenCollections = { navigationState.navigate(MainDestination.Collections) },
                accent = accent,
                surfaceColors = surfaceColors
            )
        }

        item(key = "spacer") { Spacer(Modifier.height(8.dp)) }
    }
}

// ============================================
// Pieces
// ============================================

private fun LetterSrsDeck.totalNew(): Int = progressMap.values.sumOf { it.dailyNew.size }
private fun LetterSrsDeck.totalDue(): Int = progressMap.values.sumOf { it.dailyDue.size }
private fun VocabSrsDeck.totalNew(): Int = progressMap.values.sumOf { it.dailyNew.size }
private fun VocabSrsDeck.totalDue(): Int = progressMap.values.sumOf { it.dailyDue.size }

private fun LetterSrsDeck.toUnified(category: DeckCategory) = UnifiedDeck(
    deckId = id,
    title = title,
    category = category,
    lastReview = lastReview,
    newCount = totalNew(),
    dueCount = totalDue()
)

private fun VocabSrsDeck.toUnified(category: DeckCategory) = UnifiedDeck(
    deckId = id,
    title = title,
    category = category,
    lastReview = lastReview,
    newCount = totalNew(),
    dueCount = totalDue()
)

private fun startStudy(
    navigationState: MainNavigationState,
    decksState: LibraryDecksState?
) {
    if (decksState == null) return

    val letters = decksState.letters
    val vocab = decksState.vocab

    if (letters.decks.isNotEmpty()) {
        val cards = buildLetterCards(letters, LetterPracticeType.Writing)
        if (cards.isNotEmpty()) {
            navigationState.navigate(
                MainDestination.LetterPractice(
                    LetterPracticeScreenConfiguration(
                        cards = cards,
                        practiceType = ScreenLetterPracticeType.Writing
                    )
                )
            )
            return
        }
        val readingCards = buildLetterCards(letters, LetterPracticeType.Reading)
        if (readingCards.isNotEmpty()) {
            navigationState.navigate(
                MainDestination.LetterPractice(
                    LetterPracticeScreenConfiguration(
                        cards = readingCards,
                        practiceType = ScreenLetterPracticeType.Reading
                    )
                )
            )
            return
        }
    }

    if (vocab.decks.isNotEmpty()) {
        val cards = buildVocabCards(vocab, VocabPracticeType.Flashcard)
        if (cards.isNotEmpty()) {
            navigationState.navigate(
                MainDestination.VocabPractice(
                    VocabPracticeScreenConfiguration(
                        cards = cards,
                        practiceType = ScreenVocabPracticeType.Flashcard
                    )
                )
            )
        }
    }
}

private fun buildLetterCards(
    decksData: LetterSrsDecksData,
    practiceType: LetterPracticeType
): List<LetterPracticeScreenConfiguration.Card> {
    if (decksData.decks.isEmpty()) return emptyList()
    val dailyNew = mutableMapOf<String, Long>()
    val dailyDue = mutableMapOf<String, Long>()
    decksData.decks.forEach { deck ->
        val progress = deck.progressMap.getValue(practiceType)
        progress.dailyNew.forEach { dailyNew[it] = deck.id }
        progress.dailyDue.forEach { dailyDue[it] = deck.id }
    }
    val leftover = decksData.dailyProgress.leftoversByPracticeTypeMap.getValue(practiceType)
    val newCards = dailyNew.toList().take(leftover.new).map { (letter, deckId) ->
        LetterPracticeScreenConfiguration.Card(letter, deckId)
    }
    val dueCards = dailyDue.toList().take(leftover.due).map { (letter, deckId) ->
        LetterPracticeScreenConfiguration.Card(letter, deckId)
    }
    return newCards + dueCards
}

private fun buildVocabCards(
    decksData: VocabSrsDecksData,
    practiceType: VocabPracticeType
): List<VocabPracticeScreenConfiguration.Card> {
    if (decksData.decks.isEmpty()) return emptyList()
    val dailyNew = mutableMapOf<Long, Long>()
    val dailyDue = mutableMapOf<Long, Long>()
    decksData.decks.forEach { deck ->
        val progress = deck.progressMap.getValue(practiceType)
        progress.dailyNew.forEach { dailyNew[it] = deck.id }
        progress.dailyDue.forEach { dailyDue[it] = deck.id }
    }
    val leftover = decksData.dailyProgress.leftoversByPracticeTypeMap.getValue(practiceType)
    val newCards = dailyNew.toList().take(leftover.new).map { (cardId, deckId) ->
        VocabPracticeScreenConfiguration.Card(cardId, deckId)
    }
    val dueCards = dailyDue.toList().take(leftover.due).map { (cardId, deckId) ->
        VocabPracticeScreenConfiguration.Card(cardId, deckId)
    }
    return newCards + dueCards
}

@Composable
private fun SearchBarCard(
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
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = surfaceColors.textMuted,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "Search cards, kanji, words, sentences…",
            color = surfaceColors.textMuted,
            fontSize = 14.sp
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "Search",
            color = accent.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

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
                if (hovered) accent.primary.copy(alpha = 0.14f)
                else accent.primary.copy(alpha = 0.1f)
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
                    color = androidx.compose.ui.graphics.Color(0xFFE53935),
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
        }
        if (deck.newCount > 0 || deck.dueCount > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (deck.newCount > 0) {
                    MiniCountBadge(deck.newCount, accent.primary)
                }
                if (deck.dueCount > 0) {
                    MiniCountBadge(deck.dueCount, androidx.compose.ui.graphics.Color(0xFFE53935))
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

private data class StatData(val label: String, val value: Int)

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
                Text(
                    text = item.value.toString(),
                    color = accent.primary,
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

@Composable
private fun SectionTitle(title: String, accent: KaiteyoAccentScheme, surfaceColors: SurfaceColors) {
    Text(
        text = title,
        color = accent.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SectionCard(
    glyph: String,
    title: String,
    subtitle: String,
    count: Int? = null,
    onClick: (() -> Unit)?,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val background by animateColorAsState(
        if (hovered) surfaceColors.surfaceInteractive else surfaceColors.surface
    )

    val base = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(background)

    val clickable = if (onClick != null) {
        base
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
    } else {
        base
    }

    Row(
        modifier = clickable.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(accent.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(glyph, fontSize = 20.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = surfaceColors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = surfaceColors.textMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (count != null) {
            Text(
                text = count.toString(),
                color = accent.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
