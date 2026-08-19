@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package ua.syt0r.kanji.presentation.screen.main.screen.component_explorer

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.ui.KaiteyoEmptyState
import ua.syt0r.kanji.presentation.common.ui.KaiteyoSectionCard
import ua.syt0r.kanji.presentation.common.ui.KaiteyoTag
import ua.syt0r.kanji.presentation.common.ui.PageIdentity
import ua.syt0r.kanji.presentation.common.ui.ProvidePageIdentity
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.screen.component_explorer.ComponentExplorerContract.ScreenState

@Composable
fun ComponentExplorerScreen(
    onClose: () -> Unit,
    onOpenKanji: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = getMultiplatformViewModel<ComponentExplorerContract.ViewModel>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    ProvidePageIdentity(
        PageIdentity(id = "component_explorer", name = "Component explorer", route = "/browse/components", panel = null)
    ) {
        Column(modifier.fillMaxSize()) {
            Header(onClose = onClose)
            when (val current = state) {
                is ScreenState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is ScreenState.Error -> KaiteyoEmptyState(icon = "🧩", title = "Components unavailable", message = current.message)
                is ScreenState.Loaded -> LoadedContent(
                    state = current,
                    onSelectComponent = viewModel::selectComponent,
                    onClearSelection = viewModel::clearSelection,
                    onOpenKanji = onOpenKanji
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
        Text("Components", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
        Text("Radical-derived decomposition", style = MaterialTheme.typography.labelMedium, color = surfaceColors.textMuted)
    }
}

@Composable
private fun LoadedContent(
    state: ScreenState.Loaded,
    onSelectComponent: (String) -> Unit,
    onClearSelection: () -> Unit,
    onOpenKanji: (String) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    Column(Modifier.fillMaxSize()) {
        // Selection strip.
        if (state.selected != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
            ) {
                Text(state.selected, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                Text(
                    text = "${state.kanjiTotal} kanji contain this component",
                    style = MaterialTheme.typography.bodySmall,
                    color = surfaceColors.textMuted
                )
                IconButton(onClick = onClearSelection, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear selection", tint = surfaceColors.textMuted, modifier = Modifier.size(16.dp))
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = Dimens.Space3, end = Dimens.Space3, bottom = Dimens.Space8
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space3)
        ) {
            if (state.selected == null) {
                item {
                    Text(
                        text = "Select a component to see every kanji built from it — then drill into words and sentences.",
                        style = MaterialTheme.typography.bodySmall,
                        color = surfaceColors.textMuted
                    )
                }
                item {
                    ComponentGrid(
                        components = state.components,
                        onSelect = onSelectComponent
                    )
                }
            } else {
                when {
                    state.loadingKanji -> item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    state.kanji.isEmpty() -> item {
                        KaiteyoEmptyState(icon = "🔍", title = "No kanji", message = "No kanji use this component in the bundled dataset.")
                    }
                    else -> {
                        item {
                            Text(
                                text = "KANJI → COMPONENT → KANJI → WORDS → SENTENCES",
                                style = MaterialTheme.typography.labelSmall,
                                color = surfaceColors.textMuted
                            )
                        }
                        items(state.kanji.size) { index ->
                            KanjiRow(kanji = state.kanji[index], onClick = { onOpenKanji(state.kanji[index].character) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComponentGrid(
    components: List<ComponentExplorerContract.ComponentSummary>,
    onSelect: (String) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val filtered = components.filter { it.kanjiCount >= 1 }
    KaiteyoSectionCard(title = "All components", subtitle = "${filtered.size} components · sorted by usage") {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            filtered.forEach { summary ->
                ComponentTile(
                    text = summary.component.component,
                    count = summary.kanjiCount,
                    selected = false,
                    onClick = { onSelect(summary.component.component) }
                )
            }
        }
    }
}

@Composable
private fun ComponentTile(
    text: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Column(
        modifier = Modifier
            .widthIn(min = 68.dp)
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(if (selected) accent.primary.copy(alpha = 0.15f) else surfaceColors.surfaceInteractive)
            .border(
                width = 1.dp,
                color = when {
                    selected -> accent.primary
                    hovered -> surfaceColors.border
                    else -> androidx.compose.ui.graphics.Color.Transparent
                },
                shape = RoundedCornerShape(Dimens.RadiusMd)
            )
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Space2, vertical = Dimens.Space2),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text, fontSize = 20.sp, color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
        Text("$count", style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted)
    }
}

@Composable
private fun KanjiRow(
    kanji: ua.syt0r.kanji.core.knowledge.KanjiKnowledge,
    onClick: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        Text(kanji.character, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
        Column(Modifier.weight(1f)) {
            kanji.keyword?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = surfaceColors.textPrimary)
            }
            val readings = (kanji.onReadings + kanji.kunReadings).distinct()
            if (readings.isNotEmpty()) {
                Text(
                    text = readings.take(4).joinToString("・"),
                    style = MaterialTheme.typography.labelSmall,
                    color = surfaceColors.textMuted
                )
            }
        }
        kanji.jlpt?.let { KaiteyoTag(text = it.label, tint = LocalKaiteyoAccent.current.primary) }
    }
}
