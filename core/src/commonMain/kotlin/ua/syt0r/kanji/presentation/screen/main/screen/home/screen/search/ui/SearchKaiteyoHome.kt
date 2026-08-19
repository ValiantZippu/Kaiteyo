package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.search.ui

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
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.search.SearchScreenContract.KaiteyoHomeState

/**
 * The dictionary home shown in Search while the query is empty.
 *
 * Everything here comes from real bundled data through
 * [ua.syt0r.kanji.presentation.screen.main.screen.home.screen.search.use_case.SearchScreenKaiteyoHomeUseCase]:
 * a random kanji with its actual meanings and readings, a real word and
 * sentence that contain that kanji, and real JLPT band sizes. The refresh
 * button re-rolls the trio from the live repository.
 */
@Composable
fun SearchKaiteyoHome(
    state: KaiteyoHomeState,
    onRefresh: () -> Unit,
    onCharacterClick: (String) -> Unit,
    onWordClick: (JapaneseWord) -> Unit
) {

    val accent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ---- Hero banner ------------------------------------------
        item(key = "kaiteyo-search-hero") {
            SearchKaiteyoHeroBanner(accent = accent)
        }

        // ---- Random picks header with refresh ---------------------
        item(key = "kaiteyo-search-picks-header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KaiteyoSectionLabel("Discover", Modifier.weight(1f))
                IconButton(onClick = onRefresh, enabled = !state.isLoading) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "New random picks",
                        tint = accent.primary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        // ---- Random kanji hero ------------------------------------
        item(key = "kaiteyo-search-kanji") {
            SearchKaiteyoKanjiCard(
                state = state,
                accent = accent,
                surfaceColors = surfaceColors,
                onCharacterClick = onCharacterClick
            )
        }

        // ---- Random word ------------------------------------------
        item(key = "kaiteyo-search-word") {
            KaiteyoCard(
                header = "Word",
                subtitle = state.word?.let { "containing ${state.kanji ?: ""}" } ?: "from your vocabulary",
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                val word = state.word
                if (word != null) {
                    KaiteyoVocabRow(
                        word = word,
                        onClick = { onWordClick(word) },
                        onBookmarkClick = { onWordClick(word) }
                    )
                } else {
                    SearchKaiteyoEmptyState(
                        text = if (state.isLoading) "Finding a word…" else "No example word for this kanji.",
                        surfaceColors = surfaceColors
                    )
                }
            }
        }

        // ---- Random sentence ---------------------------------------
        item(key = "kaiteyo-search-sentence") {
            KaiteyoCard(
                header = "Sentence",
                subtitle = state.sentence?.let { "containing ${state.kanji ?: ""}" } ?: "real example sentence",
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val sentence = state.sentence
                if (sentence != null) {
                    KaiteyoSentenceRow(
                        sentence = sentence,
                        onFuriganaClick = onCharacterClick
                    )
                } else {
                    SearchKaiteyoEmptyState(
                        text = if (state.isLoading) "Looking for a sentence…" else "No example sentence for this kanji.",
                        surfaceColors = surfaceColors
                    )
                }
            }
        }

        // ---- JLPT band sizes ---------------------------------------
        item(key = "kaiteyo-search-jlpt") {
            KaiteyoCard(
                header = "JLPT levels",
                subtitle = "Real kanji counts per band in your dictionary",
                contentPadding = PaddingValues(16.dp)
            ) {
                SearchKaiteyoJlptTiles(
                    counts = state.jlptCounts,
                    surfaceColors = surfaceColors,
                    accent = accent
                )
            }
        }

        // ---- Footer ------------------------------------------------
        item(key = "kaiteyo-search-footer") {
            Text(
                text = "Type anything to search — or tap a kanji below to open its page. Every pick here is real data from your dictionary.",
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

@Composable
private fun SearchKaiteyoHeroBanner(accent: KaiteyoAccentScheme) {
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
            text = "Kaiteyo in Kaiteyo",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFC9BFFF)
        )
        Text(
            text = "辞書を探検する",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Kanji, words and sentences are all connected — start anywhere and follow the graph.",
            fontSize = 12.sp,
            color = Color(0xFFB8ACEA),
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun SearchKaiteyoKanjiCard(
    state: KaiteyoHomeState,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors,
    onCharacterClick: (String) -> Unit
) {
    KaiteyoCard(
        header = "Kanji",
        subtitle = state.kanjiMeaning?.takeIf { it.isNotBlank() }
            ?: (if (state.isLoading) "picking a character…" else "from the bundled catalog"),
        contentPadding = PaddingValues(16.dp)
    ) {
        val character = state.kanji
        if (character != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(accent.primary.copy(alpha = 0.12f))
                        .clickable { onCharacterClick(character) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = character,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = surfaceColors.textPrimary
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = state.kanjiReadings?.takeIf { it.isNotBlank() } ?: "—",
                        fontSize = 13.sp,
                        color = surfaceColors.textSecondary
                    )
                    Text(
                        text = state.kanjiMeaning?.takeIf { it.isNotBlank() } ?: "—",
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
            SearchKaiteyoEmptyState(
                text = if (state.isLoading) "Picking a kanji…" else "No kanji in the catalog yet.",
                surfaceColors = surfaceColors
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchKaiteyoJlptTiles(
    counts: List<Pair<Int, Int>>,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    if (counts.isEmpty()) {
        SearchKaiteyoEmptyState(
            text = "No JLPT data available.",
            surfaceColors = surfaceColors
        )
        return
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        counts.forEach { (level, count) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.primary.copy(alpha = 0.08f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "N$level",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = surfaceColors.textPrimary
                    )
                    Text(
                        text = "$count kanji",
                        fontSize = 10.sp,
                        color = surfaceColors.textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchKaiteyoEmptyState(
    text: String,
    surfaceColors: SurfaceColors
) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = surfaceColors.textMuted,
        modifier = Modifier.padding(12.dp)
    )
}
