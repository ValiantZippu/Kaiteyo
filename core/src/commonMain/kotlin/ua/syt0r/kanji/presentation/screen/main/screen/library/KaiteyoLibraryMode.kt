package ua.syt0r.kanji.presentation.screen.main.screen.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import ua.syt0r.kanji.core.app_data.AppDataRepository
import ua.syt0r.kanji.core.app_data.Sentence
import ua.syt0r.kanji.core.app_data.data.JapaneseWord
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.KaiteyoBadge
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.KaiteyoCard
import ua.syt0r.kanji.presentation.common.ui.KaiteyoSectionLabel
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.KaiteyoSentenceRow
import ua.syt0r.kanji.presentation.common.ui.kaiteyo.KaiteyoVocabRow
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoDataCenter
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KaiteyoCard
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenData
import ua.syt0r.kanji.presentation.screen.main.screen.info.toInfoScreenData

// ============================================================
// KANJIVERSE LIBRARY MODE — the dictionary's front door.
//
// A Kaiteyo-style home inside the Library: a welcome banner,
// a random kanji + word + sentence trio (all drawn from real
// bundled data via the data center and the app data repository),
// and explore lists that dive into JLPT bands, school grades and
// the user's own collections. Re-rolling pulls fresh picks.
// ============================================================

@Composable
fun KaiteyoLibraryMode(
    navigationState: MainNavigationState,
    dataCenter: KaiteyoDataCenter,
    appDataRepository: AppDataRepository,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {

    // ----- Random-pick state (re-rolled together) -----------------
    var rerollKey by remember { mutableStateOf(0) }
    var loadingPicks by remember { mutableStateOf(false) }
    var randomKanji by remember { mutableStateOf<KaiteyoCard?>(null) }
    var randomWord by remember { mutableStateOf<JapaneseWord?>(null) }
    var randomSentence by remember { mutableStateOf<Sentence?>(null) }

    val loader = remember(appDataRepository) { KaiteyoPickLoader(appDataRepository) }

    // Each reroll picks a random kanji from the catalog, then real
    // words and sentences that actually contain that kanji.
    LaunchedEffect(rerollKey) {
        loadingPicks = true
        randomWord = null
        randomSentence = null
        val size = dataCenter.cards.size
        val pick = if (size > 0) dataCenter.cards[Random.nextInt(size)] else null
        randomKanji = pick
        if (pick != null) {
            val (word, sentence) = loader.load(pick.character)
            randomWord = word
            randomSentence = sentence
        }
        loadingPicks = false
    }

    // ----- Explore lists (real counts from the loaded catalog) ----
    val jlptCounts = remember(dataCenter.classifications) {
        (5 downTo 1).map { level ->
            level to dataCenter.classifications.values.count { classes -> classes.contains("n$level") }
        }
    }
    val gradeCounts = remember(dataCenter.classifications) {
        (1..6).map { grade ->
            grade to dataCenter.classifications.values.count { classes -> classes.contains("o$grade") }
        }
    }

    val openKanji: (String) -> Unit = {
        navigationState.navigate(MainDestination.Info(InfoScreenData.Letter(it)))
    }
    val openWord: (JapaneseWord) -> Unit = {
        navigationState.navigate(MainDestination.Info(it.toInfoScreenData()))
    }
    val openKanjiBrowser: () -> Unit = {
        navigationState.navigate(MainDestination.KanjiBrowser())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ---- Welcome banner -------------------------------------
        item(key = "kaiteyo-welcome") {
            KaiteyoWelcomeBanner(
                onExploreKanji = openKanjiBrowser,
                accent = accent
            )
        }

        // ---- Random picks ---------------------------------------
        item(key = "kaiteyo-picks-header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KaiteyoSectionLabel("Daily picks", Modifier.weight(1f))
                IconButton(onClick = { rerollKey++ }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "New random picks",
                        tint = accent.primary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        item(key = "kaiteyo-random-kanji") {
            KaiteyoCard(
                header = "Random kanji",
                subtitle = randomKanji?.let { "from the bundled ${dataCenter.cards.size}-character catalog" }
                    ?: "from the bundled catalog",
                contentPadding = PaddingValues(16.dp)
            ) {
                val kanji = randomKanji
                if (kanji != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(accent.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = kanji.character,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = surfaceColors.textPrimary
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = kanji.reading,
                                fontSize = 13.sp,
                                color = surfaceColors.textSecondary
                            )
                            Text(
                                text = kanji.meaning,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = surfaceColors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        KaiteyoBadge(
                            text = "漢字",
                            containerColor = accent.primary.copy(alpha = 0.14f),
                            contentColor = accent.primary
                        )
                    }
                } else {
                    Text(
                        text = if (loadingPicks) "Picking a kanji…" else "No kanji in the catalog yet.",
                        fontSize = 12.sp,
                        color = surfaceColors.textMuted
                    )
                }
            }
        }

        item(key = "kaiteyo-random-word") {
            KaiteyoCard(
                header = "Random word",
                subtitle = randomWord?.let { "containing ${randomKanji?.character ?: ""}" } ?: "from your vocabulary",
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                val word = randomWord
                if (word != null) {
                    KaiteyoVocabRow(
                        word = word,
                        onClick = { openWord(word) },
                        onBookmarkClick = { openWord(word) }
                    )
                } else {
                    Text(
                        text = if (loadingPicks) "Finding a word…" else "No example words for this kanji.",
                        fontSize = 12.sp,
                        color = surfaceColors.textMuted,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        item(key = "kaiteyo-random-sentence") {
            KaiteyoCard(
                header = "Random sentence",
                subtitle = randomSentence?.let { "containing ${randomKanji?.character ?: ""}" } ?: "real example sentence",
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val sentence = randomSentence
                if (sentence != null) {
                    KaiteyoSentenceRow(
                        sentence = sentence,
                        onFuriganaClick = openKanji
                    )
                } else {
                    Text(
                        text = if (loadingPicks) "Looking for a sentence…" else "No example sentence for this kanji.",
                        fontSize = 12.sp,
                        color = surfaceColors.textMuted,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        // ---- Explore lists --------------------------------------
        item(key = "kaiteyo-explore-header") {
            KaiteyoSectionLabel("Explore lists")
        }

        item(key = "kaiteyo-jlpt") {
            KaiteyoListSection(
                title = "JLPT levels",
                subtitle = "Kanji in each band of the official exam levels",
                items = jlptCounts.map { (level, count) ->
                    KaiteyoListTile(
                        title = "N$level",
                        subtitle = "$count kanji",
                        onClick = openKanjiBrowser
                    )
                }
            )
        }

        item(key = "kaiteyo-grades") {
            KaiteyoListSection(
                title = "School grades",
                subtitle = "Kanji by Japanese school year (1–6)",
                items = gradeCounts.map { (grade, count) ->
                    KaiteyoListTile(
                        title = "Grade $grade",
                        subtitle = "$count kanji",
                        onClick = openKanjiBrowser
                    )
                }
            )
        }

        if (dataCenter.collections.isNotEmpty()) {
            item(key = "kaiteyo-collections") {
                KaiteyoListSection(
                    title = "Your collections",
                    subtitle = "Hand-picked and smart decks",
                    items = dataCenter.collections.map { collection ->
                        KaiteyoListTile(
                            title = "${collection.icon} ${collection.name}",
                            subtitle = "${collection.cardIds.size} cards",
                            onClick = {
                                navigationState.navigate(MainDestination.Collections)
                            }
                        )
                    }
                )
            }
        }

        item(key = "kaiteyo-footer") {
            Text(
                text = "Every card on this page is real data from your catalog — roll again to meet another corner of the language.",
                fontSize = 10.sp,
                color = surfaceColors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )
        }
    }
}

/**
 * Loads a random word + sentence pair for a kanji from real data.
 * Prefers example words from the dictionary, falls back to a text
 * search, and grabs the first sentence containing the character.
 */
private class KaiteyoPickLoader(private val repository: AppDataRepository) {

    suspend fun load(letter: String): Pair<JapaneseWord?, Sentence?> {
        val words = runCatching { repository.getWordExamples(letter) }.getOrDefault(emptyList())
        val fallbackWords = if (words.isEmpty()) {
            runCatching { repository.getWordsWithText(letter, limit = 20) }.getOrDefault(emptyList())
        } else words
        val word = fallbackWords.getOrNull(Random.nextInt(fallbackWords.size.coerceAtLeast(1)))
        val sentence = runCatching {
            repository.getSentencesWithText(letter, offset = 0, limit = 1).firstOrNull()
        }.getOrNull()
        return word to sentence
    }
}

@Composable
private fun KaiteyoWelcomeBanner(
    onExploreKanji: () -> Unit,
    accent: KaiteyoAccentScheme
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF2A2152),
                        Color(0xFF3B2E6B),
                        accent.primary.copy(alpha = 0.35f)
                    )
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Welcome to Kaiteyo!",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFC9BFFF)
        )
        Text(
            text = "漢字宇宙へようこそ！",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Every kanji, word and sentence in Kaiteyo is connected — explore the graph from any starting point.",
            fontSize = 12.sp,
            color = Color(0xFFB8ACEA),
            lineHeight = 17.sp
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .align(Alignment.End)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF6C5CE7))
                .clickable(onClick = onExploreKanji)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Explore kanji",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KaiteyoListSection(
    title: String,
    subtitle: String,
    items: List<KaiteyoListTile>
) {
    val surfaceColors = LocalSurfaceColors.current
    KaiteyoCard(
        header = title,
        subtitle = subtitle,
        contentPadding = PaddingValues(16.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { tile ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(surfaceColors.surfaceInteractive.copy(alpha = 0.5f))
                        .clickable(onClick = tile.onClick)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = tile.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = surfaceColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = tile.subtitle,
                            fontSize = 10.sp,
                            color = surfaceColors.textMuted
                        )
                    }
                }
            }
        }
    }
}

private data class KaiteyoListTile(
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)
