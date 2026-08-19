@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package ua.syt0r.kanji.presentation.screen.main.screen.radical_explorer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.core.knowledge.KanjiKnowledge
import ua.syt0r.kanji.core.knowledge.RadicalStats
import ua.syt0r.kanji.presentation.common.ui.KaiteyoEmptyState
import ua.syt0r.kanji.presentation.common.ui.KaiteyoPill
import ua.syt0r.kanji.presentation.common.ui.PageIdentity
import ua.syt0r.kanji.presentation.common.ui.ProvidePageIdentity
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.screen.radical_explorer.RadicalExplorerContract.ScreenState


// ============================================================
// RADICAL EXPLORER — SCREEN
// ------------------------------------------------------------
// A selectable radical grid (real radicals, real counts), stroke
// / JLPT / grade filters, and a kanji results panel. Selecting
// radicals intersects them; tapping a kanji opens its entry.
// ============================================================

@Composable
fun RadicalExplorerScreen(
    onClose: () -> Unit,
    onOpenKanji: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = getMultiplatformViewModel<RadicalExplorerContract.ViewModel>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    ProvidePageIdentity(
        PageIdentity(id = "radical_explorer", name = "Radical explorer", route = "/radicals", panel = "Results")
    ) {
        Column(modifier.fillMaxSize()) {
            ExplorerHeader(
                onClose = onClose,
                selectedCount = (state as? ScreenState.Loaded)?.selectedRadicals?.size ?: 0,
                onClear = viewModel::clearSelection
            )
            when (val current = state) {
                is ScreenState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is ScreenState.Error -> KaiteyoEmptyState(icon = "⚠️", title = "Radical data unavailable", message = current.message)
                is ScreenState.Loaded -> ExplorerBody(
                    state = current,
                    onToggleRadical = viewModel::toggleRadical,
                    onMinStrokes = viewModel::setMinStrokes,
                    onJlpt = viewModel::setJlpt,
                    onGrade = viewModel::setGrade,
                    onOpenKanji = onOpenKanji
                )
            }
        }
    }
}

@Composable
private fun ExplorerHeader(
    onClose: () -> Unit,
    selectedCount: Int,
    onClear: () -> Unit
) {
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
        Text("Radical explorer", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
        if (selectedCount > 0) {
            KaiteyoPill(text = "$selectedCount selected · clear", selected = false, onClick = onClear)
        }
    }
}

@Composable
private fun ExplorerBody(
    state: ScreenState.Loaded,
    onToggleRadical: (String) -> Unit,
    onMinStrokes: (Int?) -> Unit,
    onJlpt: (Int?) -> Unit,
    onGrade: (Int?) -> Unit,
    onOpenKanji: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = Dimens.Space3)) {
        FilterStrip(
            minStrokes = state.minStrokes,
            jlpt = state.jlpt,
            grade = state.grade,
            onMinStrokes = onMinStrokes,
            onJlpt = onJlpt,
            onGrade = onGrade
        )

        Spacer(Modifier.height(Dimens.Space2))

        // ── Radical grid ───────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = Dimens.Space4),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            items(state.radicals.chunked(10)) { rowRadicals ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
                ) {
                    rowRadicals.forEach { stats ->
                        RadicalCell(
                            stats = stats,
                            selected = stats.radical in state.selectedRadicals,
                            onClick = { onToggleRadical(stats.radical) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(10 - rowRadicals.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        Spacer(Modifier.height(Dimens.Space2))

        // ── Kanji results ──────────────────────────────────
        if (state.selectedRadicals.isNotEmpty()) {
            KanjiResultsPanel(
                state = state,
                onOpenKanji = onOpenKanji
            )
        } else {
            KaiteyoEmptyState(
                icon = "🔎",
                title = "Select radicals to find kanji",
                message = "Radicals are intersected — pick up to four to narrow the set."
            )
        }
    }
}

@Composable
private fun FilterStrip(
    minStrokes: Int?,
    jlpt: Int?,
    grade: Int?,
    onMinStrokes: (Int?) -> Unit,
    onJlpt: (Int?) -> Unit,
    onGrade: (Int?) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space1)) {
        Text("Stroke count", style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.Space1)) {
            items(listOf<Int?>(null, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17)) { strokes ->
                KaiteyoPill(
                    text = strokes?.let { "≥$it" } ?: "Any",
                    selected = minStrokes == strokes,
                    onClick = { onMinStrokes(strokes) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space1)) {
            Text("JLPT", style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted)
            listOf<Int?>(null, 5, 4, 3, 2, 1).forEach { level ->
                KaiteyoPill(
                    text = level?.let { "N$it" } ?: "Any",
                    selected = jlpt == level,
                    onClick = { onJlpt(level) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space1)) {
            Text("Grade", style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted)
            listOf<Int?>(null, 1, 2, 3, 4, 5, 6).forEach { g ->
                KaiteyoPill(
                    text = g?.let { "G$it" } ?: "Any",
                    selected = grade == g,
                    onClick = { onGrade(g) }
                )
            }
        }
        Text(
            text = "Filters apply over the visible results (DB-wide filtering is a roadmap item).",
            style = MaterialTheme.typography.labelSmall,
            color = surfaceColors.textMuted
        )
    }
}

@Composable
private fun RadicalCell(
    stats: RadicalStats,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(if (selected) accent.primary.copy(alpha = 0.14f) else surfaceColors.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent.primary else surfaceColors.border.copy(alpha = 0.4f),
                shape = RoundedCornerShape(Dimens.RadiusMd)
            )
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.Space2),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stats.radical,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) accent.primary else surfaceColors.textPrimary
        )
        Text(
            text = "${stats.strokeCount} strokes · ${stats.kanjiCount}",
            style = MaterialTheme.typography.labelSmall,
            color = surfaceColors.textMuted
        )
    }
}

@Composable
private fun KanjiResultsPanel(
    state: ScreenState.Loaded,
    onOpenKanji: (String) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusLg))
            .background(surfaceColors.surface)
            .padding(Dimens.Space3)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            Text(
                text = "KANJI — ${state.selectedRadicals.joinToString("")}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = surfaceColors.textSecondary
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${state.kanjiTotal} total",
                style = MaterialTheme.typography.labelSmall,
                color = surfaceColors.textMuted
            )
        }
        Spacer(Modifier.height(Dimens.Space2))

        if (state.loadingKanji) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.kanji.isEmpty()) {
            KaiteyoEmptyState(
                icon = "🔍",
                title = "No kanji match",
                message = "No kanji in the bundled dataset use all selected radicals with these filters."
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
            ) {
                state.kanji.forEach { kanji ->
                    KanjiResultCell(kanji = kanji, onClick = { onOpenKanji(kanji.character) })
                }
            }
        }
    }
}

@Composable
private fun KanjiResultCell(kanji: KanjiKnowledge, onClick: () -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Column(
        modifier = Modifier
            .widthIn(min = 76.dp)
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(surfaceColors.surfaceInteractive)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Space2, vertical = Dimens.Space2),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(kanji.character, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
        kanji.keyword?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = surfaceColors.textSecondary, maxLines = 1)
        }
        kanji.frequencyRank?.let {
            Text(
                text = "#$it",
                style = MaterialTheme.typography.labelSmall,
                color = accent.primary
            )
        }
    }
}
