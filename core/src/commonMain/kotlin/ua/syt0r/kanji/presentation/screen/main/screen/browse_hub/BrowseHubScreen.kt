@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package ua.syt0r.kanji.presentation.screen.main.screen.browse_hub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.knowledge.GrammarPattern
import ua.syt0r.kanji.core.knowledge.LearnerProfileStore
import ua.syt0r.kanji.core.knowledge.LibraryCollection
import ua.syt0r.kanji.core.knowledge.level.LevelAdapter
import ua.syt0r.kanji.presentation.common.ui.KaiteyoEmptyState
import ua.syt0r.kanji.presentation.common.ui.KaiteyoPill
import ua.syt0r.kanji.presentation.common.ui.KaiteyoSectionCard
import ua.syt0r.kanji.presentation.common.ui.PageIdentity
import ua.syt0r.kanji.presentation.common.ui.ProvidePageIdentity
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.screen.browse_hub.BrowseHubContract.ScreenState

// ============================================================
// BROWSE HUB — \"Explore Japanese\" (spec §30)
// ------------------------------------------------------------
// An exploratory browse surface, not a database table: kanji by
// JLPT / grade, radicals, components, grammar patterns and
// library collections, all with REAL dataset counts. Every
// section drills into a real destination.
// ============================================================

@Composable
fun BrowseHubScreen(
    onClose: () -> Unit,
    onOpenRadicalExplorer: () -> Unit,
    onOpenComponentExplorer: () -> Unit,
    onOpenKanjiBrowser: () -> Unit,
    onOpenRecommended: (Set<Int>) -> Unit = {},
    onOpenCollection: (String) -> Unit,
    onOpenCollections: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = getMultiplatformViewModel<BrowseHubContract.ViewModel>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    ProvidePageIdentity(
        PageIdentity(id = "browse_hub", name = "Browse", route = "/browse", panel = null)
    ) {
        Column(modifier.fillMaxSize()) {
            Header(onClose = onClose)
            when (val current = state) {
                is ScreenState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is ScreenState.Error -> KaiteyoEmptyState(icon = "🧭", title = "Browse unavailable", message = current.message)
                is ScreenState.Loaded -> LoadedContent(
                    state = current,
                    onOpenRadicalExplorer = onOpenRadicalExplorer,
                    onOpenComponentExplorer = onOpenComponentExplorer,
                    onOpenKanjiBrowser = onOpenKanjiBrowser,
                    onOpenRecommended = onOpenRecommended,
                    onOpenCollection = onOpenCollection,
                    onOpenCollections = onOpenCollections
                )
            }
        }
    }
}

@Composable
private fun Header(onClose: () -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = surfaceColors.textPrimary)
        }
        Text("Browse", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
        Text("Explore Japanese", style = MaterialTheme.typography.labelMedium, color = surfaceColors.textMuted)
    }
}

@Composable
private fun LoadedContent(
    state: ScreenState.Loaded,
    onOpenRadicalExplorer: () -> Unit,
    onOpenComponentExplorer: () -> Unit,
    onOpenKanjiBrowser: () -> Unit,
    onOpenRecommended: (Set<Int>) -> Unit,
    onOpenCollection: (String) -> Unit,
    onOpenCollections: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = Dimens.Space3, end = Dimens.Space3, bottom = Dimens.Space8
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        item {
            StatsBanner(
                kanjiTotal = state.kanjiTotal,
                radicalCount = state.radicalCount,
                componentCount = state.componentCount
            )
        }
        item {
            KaiteyoSectionCard(title = "Explore", subtitle = "Browse by entry point") {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
                ) {
                    KaiteyoPill(text = "Kanji browser", selected = false, onClick = onOpenKanjiBrowser)
                    KaiteyoPill(text = "Radicals", selected = false, onClick = onOpenRadicalExplorer)
                    KaiteyoPill(text = "Components", selected = false, onClick = onOpenComponentExplorer)
                    KaiteyoPill(text = "Collections", selected = false, onClick = onOpenCollections)
                }
            }
        }
        item {
            RecommendedForYouSection(onOpenRecommended = onOpenRecommended)
        }
        item {
            CollectionSection(title = "By JLPT", collections = state.jlptCollections, onOpen = onOpenCollection)
        }
        item {
            CollectionSection(title = "By school grade", collections = state.gradeCollections, onOpen = onOpenCollection)
        }
        item {
            FrequencyDistributionSection(state.frequencyDistribution, state.kanjiTotal)
        }
        item {
            JlptBreakdownSection(state.jlptDistribution)
        }
        item {
            GrammarSection(patterns = state.grammarPatterns)
        }
    }
}

@Composable
private fun StatsBanner(kanjiTotal: Int, radicalCount: Int, componentCount: Int) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusXl))
            .background(surfaceColors.surface)
            .padding(Dimens.Space4),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Stat("$kanjiTotal", "kanji", accent.primary)
        Stat("$radicalCount", "radicals", accent.secondary)
        Stat("$componentCount", "components", accent.primary)
    }
}

@Composable
private fun Stat(value: String, label: String, tint: androidx.compose.ui.graphics.Color) {
    val surfaceColors = LocalSurfaceColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = tint)
        Text(label, style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted)
    }
}

@Composable
private fun CollectionSection(
    title: String,
    collections: List<LibraryCollection>,
    onOpen: (String) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    if (collections.isEmpty()) return
    KaiteyoSectionCard(title = title, subtitle = "Real kanji counts from the bundled dataset") {
        collections.forEach { collection ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.RadiusSm))
                    .clickable { onOpen(collection.id) }
                    .padding(horizontal = Dimens.Space2, vertical = Dimens.Space2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(collection.title, style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text(collection.description, style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted, maxLines = 2)
                }
                Text("${collection.size}", style = MaterialTheme.typography.labelMedium, color = surfaceColors.textMuted)
            }
        }
    }
}

@Composable
private fun FrequencyDistributionSection(
    distribution: List<Pair<ua.syt0r.kanji.core.knowledge.FrequencyBand, Int>>,
    kanjiTotal: Int
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    if (distribution.isEmpty()) return
    val maxCount = distribution.maxOfOrNull { it.second } ?: 1
    KaiteyoSectionCard(title = "Frequency distribution", subtitle = "Kanji count by usage frequency") {
        distribution.forEach { (band, count) ->
            val fraction = if (maxCount > 0) count.toFloat() / maxCount else 0f
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
            ) {
                Text(
                    text = band.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = surfaceColors.textSecondary,
                    modifier = Modifier.width(80.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(surfaceColors.surfaceInteractive)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                            .background(accent.primary.copy(alpha = 0.7f))
                    )
                }
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = surfaceColors.textMuted,
                    modifier = Modifier.width(36.dp)
                )
            }
        }
        if (kanjiTotal > 0) {
            Text(
                text = "Total: $kanjiTotal kanji in the bundled dataset",
                style = MaterialTheme.typography.labelSmall,
                color = surfaceColors.textMuted,
                modifier = Modifier.padding(top = Dimens.Space2)
            )
        }
    }
}

@Composable
private fun JlptBreakdownSection(distribution: List<Pair<Int, Int>>) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    if (distribution.isEmpty()) return
    val maxCount = distribution.maxOfOrNull { it.second } ?: 1
    KaiteyoSectionCard(title = "JLPT breakdown", subtitle = "Kanji count per JLPT level") {
        distribution.forEach { (level, count) ->
            val fraction = if (maxCount > 0) count.toFloat() / maxCount else 0f
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
            ) {
                Text(
                    text = "N$level",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent.primary,
                    modifier = Modifier.width(30.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(surfaceColors.surfaceInteractive)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                            .background(accent.primary.copy(alpha = 0.5f))
                    )
                }
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = surfaceColors.textMuted,
                    modifier = Modifier.width(36.dp)
                )
            }
        }
    }
}

@Composable
private fun GrammarSection(patterns: List<GrammarPattern>) {
    val surfaceColors = LocalSurfaceColors.current
    if (patterns.isEmpty()) return
    KaiteyoSectionCard(title = "Grammar", subtitle = "${patterns.size} built-in reference patterns") {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            patterns.take(24).forEach { pattern ->
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Dimens.RadiusMd))
                        .padding(horizontal = Dimens.Space2, vertical = Dimens.Space2)
                ) {
                    Text(pattern.pattern, style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text(pattern.meaning, style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted, maxLines = 1)
                }
            }
        }
        Text(
            text = "Grammar highlight matching is substring-based — a hint, not a parse.",
            style = MaterialTheme.typography.labelSmall,
            color = surfaceColors.textMuted,
            modifier = Modifier.padding(top = Dimens.Space2)
        )
    }
}

/**
 * Level-adaptive entry point (spec §23): the JLPT band the learner profile
 * recommends, with a one-tap drill into the real KanjiBrowser filter. The
 * band comes from the profile catalog — Native/Research see "All levels".
 */
@Composable
private fun RecommendedForYouSection(
    onOpenRecommended: (Set<Int>) -> Unit
) {
    val profileStore = koinInject<LearnerProfileStore>()
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var band by remember { mutableStateOf<Set<Int>?>(null) }

    LaunchedEffect(Unit) {
        band = LevelAdapter.recommendedJlpt(profileStore.load().profile)
    }

    val current = band ?: return
    val label = LevelAdapter.recommendedJlptLabel(current)

    KaiteyoSectionCard(
        title = "For your level",
        subtitle = if (current.isEmpty()) "Your profile has no JLPT restriction" else "The JLPT band your learner profile recommends"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(Dimens.RadiusMd))
                    .background(accent.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = accent.primary)
            }
            Text(
                text = if (current.isEmpty()) "Browse the whole dataset — change it in Settings → Appearance → Learner profile."
                else "${current.size} kanji level${if (current.size == 1) "" else "s"} · opens the Kanji browser pre-filtered",
                style = MaterialTheme.typography.bodySmall,
                color = surfaceColors.textSecondary,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { onOpenRecommended(current) }) {
                Text("Browse", color = accent.primary)
            }
        }
    }
}
