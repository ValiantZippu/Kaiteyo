@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ua.syt0r.kanji.presentation.screen.main.screen.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.knowledge.KnowledgeRepository
import ua.syt0r.kanji.core.knowledge.SentenceDifficultyScorer
import ua.syt0r.kanji.core.knowledge.SentenceKnowledge
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.common.ui.KaiteyoPill
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState

// ============================================================
// SENTENCES BROWSE — sentences as first-class Library content
// ------------------------------------------------------------
// Searches the real bundled corpus (same DB query the kanji /
// word pages use), shows each hit with its translation and a
// difficulty estimate, and opens the interactive SentenceEntry
// (tokenized, grammar-highlighted) on tap. No fake sentences:
// an empty query shows an honest hint, never fabricated rows.
// ============================================================

@Composable
fun SentencesBrowse(
    navigationState: MainNavigationState,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    val repository = koinInject<KnowledgeRepository>()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SentenceKnowledge>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.isBlank()) {
            results = emptyList()
            searching = false
            searched = false
            return@LaunchedEffect
        }
        searching = true
        delay(250) // debounce the corpus query
        results = repository.sentencesWithText(q, limit = 50)
        searching = false
        searched = true
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Sentences",
                    color = surfaceColors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${results.size} matches · bundled corpus (Tatoeba-derived), difficulty is an estimate",
                    color = surfaceColors.textMuted,
                    fontSize = 12.sp
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search sentence text — e.g. 食べる, 学校, 私…", color = surfaceColors.textMuted) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = surfaceColors.textMuted)
            },
            singleLine = true
        )

        when {
            searching -> Text(
                text = "Searching the corpus…",
                color = surfaceColors.textMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp)
            )
            query.isBlank() -> Text(
                text = "Type any Japanese text — sentences containing it will appear here. Tap one to open its interactive page.",
                color = surfaceColors.textMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp)
            )
            searched && results.isEmpty() -> Text(
                text = "No corpus sentences contain \"${query.trim()}\". Try a shorter or more common term.",
                color = surfaceColors.textMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp)
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results, key = { it.text }) { sentence ->
                    val difficulty = SentenceDifficultyScorer.score(sentence.text)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(surfaceColors.surface)
                            .clickable {
                                navigationState.navigate(
                                    MainDestination.SentenceEntry(sentence.text, sentence.translation)
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = sentence.text,
                                color = surfaceColors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (sentence.translation.isNotBlank()) {
                                Text(
                                    text = sentence.translation,
                                    color = surfaceColors.textSecondary,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = "Difficulty: ${difficulty.label} (estimate)",
                                color = surfaceColors.textMuted,
                                fontSize = 11.sp
                            )
                        }
                        KaiteyoPill(
                            text = difficulty.tier.name,
                            selected = false,
                            onClick = {
                                navigationState.navigate(
                                    MainDestination.SentenceEntry(sentence.text, sentence.translation)
                                )
                            },
                            tint = LocalKaiteyoAccent.current.primary
                        )
                    }
                }
            }
        }
    }
}
