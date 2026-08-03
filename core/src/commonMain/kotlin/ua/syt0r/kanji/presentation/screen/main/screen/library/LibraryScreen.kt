@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ua.syt0r.kanji.presentation.screen.main.screen.library

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoDataCenter
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.letters_dashboard.LettersDashboardScreen
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.search.SearchScreen
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.vocab_dashboard.VocabDashboardScreen
import ua.syt0r.kanji.presentation.screen.main.screen.kanji_browser.KanjiBrowserCriteria

// ============================================
// LIBRARY — the central hub
// Replaces the former Kanji/Vocabulary split
// with one consistent interface. Sections:
// Kanji · Vocabulary · Grammar · Sentences ·
// Radicals · Custom · Collections · Favorites ·
// Pinned · Recently Studied · Smart Collections
// ============================================

@Composable
fun LibraryScreen(navigationState: MainNavigationState) {
    val dataCenter = koinInject<KaiteyoDataCenter>()
    LaunchedEffect(Unit) { dataCenter.ensureLoaded() }

    var view by remember { mutableStateOf<LibraryView>(LibraryView.Hub) }

    when (view) {
        LibraryView.Hub -> LibraryHub(
            navigationState = navigationState,
            dataCenter = dataCenter,
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

    val favorites = dataCenter.favorites.value.size
    val collections = dataCenter.collections.size
    val customCount = dataCenter.collections.count { !it.isSmart }
    val smartCount = dataCenter.collections.count { it.isSmart }
    val recently = dataCenter.collections
        .firstOrNull { it.isSmart && it.name == "Recently learned" }
        ?.cardIds?.size ?: 0

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
                    text = "Your study hub — everything in one place",
                    color = surfaceColors.textMuted,
                    fontSize = 13.sp
                )
            }
        }

        item(key = "stats") {
            StatRow(
                items = listOf(
                    StatData("Kanji", dataCenter.cards.size),
                    StatData("Favorites", favorites),
                    StatData("Reviews", dataCenter.totalReviews.value.toInt()),
                    StatData("Tags", dataCenter.tags.size)
                ),
                accent = accent,
                surfaceColors = surfaceColors
            )
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

        item(key = "find-title") { SectionTitle("FIND & ORGANIZE", accent, surfaceColors) }

        item(key = "find-grid") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionCard(
                    glyph = "🗂",
                    title = "Collections",
                    subtitle = "All smart & saved collections",
                    count = collections,
                    onClick = { navigationState.navigate(MainDestination.Collections) },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
                SectionCard(
                    glyph = "★",
                    title = "Favorites",
                    subtitle = "Your starred kanji",
                    count = favorites,
                    onClick = {
                        navigationState.navigate(MainDestination.KanjiBrowser(KanjiBrowserCriteria(favoritesOnly = true)))
                    },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
                SectionCard(
                    glyph = "📌",
                    title = "Pinned",
                    subtitle = "Quick access pinned items",
                    count = 0,
                    onClick = { navigationState.navigate(MainDestination.Collections) },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
                SectionCard(
                    glyph = "🕐",
                    title = "Recently Studied",
                    subtitle = "Studied in the last 24 hours",
                    count = recently,
                    onClick = { navigationState.navigate(MainDestination.Collections) },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
                SectionCard(
                    glyph = "📁",
                    title = "Custom",
                    subtitle = "Saved filters & decks",
                    count = customCount,
                    onClick = { navigationState.navigate(MainDestination.Collections) },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
                SectionCard(
                    glyph = "✨",
                    title = "Smart Collections",
                    subtitle = "Auto-generated sets",
                    count = smartCount,
                    onClick = { navigationState.navigate(MainDestination.Collections) },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
                SectionCard(
                    glyph = "🚩",
                    title = "Flagged",
                    subtitle = "Kanji with any flag set",
                    count = dataCenter.flags.size,
                    onClick = {
                        navigationState.navigate(MainDestination.KanjiBrowser(KanjiBrowserCriteria(showFlagged = true)))
                    },
                    accent = accent,
                    surfaceColors = surfaceColors
                )
            }
        }

        item(key = "spacer") { Spacer(Modifier.height(8.dp)) }
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

    val base = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(if (hovered) surfaceColors.surfaceInteractive else surfaceColors.surface)

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