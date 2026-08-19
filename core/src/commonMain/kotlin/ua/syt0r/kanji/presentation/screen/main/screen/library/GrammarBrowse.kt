@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ua.syt0r.kanji.presentation.screen.main.screen.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.core.knowledge.GrammarCatalog
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.common.ui.KaiteyoPill
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState

// ============================================================
// GRAMMAR BROWSE — grammar as first-class Library content
// ------------------------------------------------------------
// Lists the real bundled grammar catalog (GrammarCatalog.all():
// particles + common patterns with meaning, formation and JLPT
// level). Search + JLPT chips filter the actual catalog; tapping
// a pattern opens the dictionary explorer on that pattern, which
// surfaces real corpus sentences containing it + the grammar
// match — grammar → sentences, no fake content.
// ============================================================

@Composable
fun GrammarBrowse(
    navigationState: MainNavigationState,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    var query by remember { mutableStateOf("") }
    var jlptLevels by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val hasFilters = jlptLevels.isNotEmpty()

    val patterns = remember(query, jlptLevels) {
        val q = query.trim().lowercase()
        GrammarCatalog.all().filter { pattern ->
            val jlptOk = jlptLevels.isEmpty() || pattern.jlpt in jlptLevels
            val textOk = q.isEmpty() ||
                pattern.pattern.lowercase().contains(q) ||
                pattern.meaning.lowercase().contains(q) ||
                pattern.keywords.any { it.lowercase().contains(q) }
            jlptOk && textOk
        }
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
                    text = "Grammar",
                    color = surfaceColors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${patterns.size} patterns · ${if (hasFilters) "JLPT filtered" else "built-in reference catalog — not an authoritative corpus"}",
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
            placeholder = { Text("Search patterns, meanings, keywords…", color = surfaceColors.textMuted) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = surfaceColors.textMuted)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { /* live filtering above */ })
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (5 downTo 1).forEach { level ->
                KaiteyoPill(
                    text = "N$level",
                    selected = level in jlptLevels,
                    onClick = {
                        jlptLevels = if (level in jlptLevels) jlptLevels - level else jlptLevels + level
                    }
                )
            }
            if (hasFilters) {
                KaiteyoPill(
                    text = "Clear",
                    selected = false,
                    onClick = { jlptLevels = emptySet() }
                )
            }
        }

        if (patterns.isEmpty()) {
            Text(
                text = "No grammar patterns match. Try a different level or search term.",
                color = surfaceColors.textMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(patterns.size) { index ->
                    val pattern = patterns[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(surfaceColors.surface)
                            .clickable {
                                // Grammar → real sentences: open the explorer on
                                // this pattern's kana form.
                                navigationState.navigate(MainDestination.KnowledgeExplorer(pattern.pattern))
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = pattern.pattern,
                                color = surfaceColors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = pattern.meaning,
                                color = surfaceColors.textSecondary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = pattern.formation.orEmpty(),
                                color = surfaceColors.textMuted,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        KaiteyoPill(
                            text = "N${pattern.jlpt}",
                            selected = false,
                            onClick = {
                                navigationState.navigate(MainDestination.KnowledgeExplorer(pattern.pattern))
                            },
                            tint = accent.primary
                        )
                    }
                }
            }
        }
    }
}
