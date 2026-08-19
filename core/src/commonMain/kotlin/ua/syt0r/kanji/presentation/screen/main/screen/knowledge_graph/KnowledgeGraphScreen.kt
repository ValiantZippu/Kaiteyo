@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package ua.syt0r.kanji.presentation.screen.main.screen.knowledge_graph

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import ua.syt0r.kanji.core.knowledge.KnowledgeEdgeType
import ua.syt0r.kanji.core.knowledge.KnowledgeNodeKind
import ua.syt0r.kanji.presentation.common.ui.KaiteyoEmptyState
import ua.syt0r.kanji.presentation.common.ui.KaiteyoPill
import ua.syt0r.kanji.presentation.common.ui.KaiteyoSectionCard
import ua.syt0r.kanji.presentation.common.ui.PageIdentity
import ua.syt0r.kanji.presentation.common.ui.ProvidePageIdentity
import ua.syt0r.kanji.presentation.common.ui.knowledge.KnowledgeGraphCanvas
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.screen.knowledge_graph.KnowledgeGraphContract.ScreenState


// ============================================================
// KNOWLEDGE GRAPH — SCREEN
// ------------------------------------------------------------
// Full-page graph explorer: a pan/zoom canvas on the left and a
// node inspector on the right. The graph only ever contains
// what has been expanded — real dictionary relationships, pulled
// one ring at a time.
// ============================================================

@Composable
fun KnowledgeGraphScreen(
    root: String,
    onClose: () -> Unit,
    onOpenKanji: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = getMultiplatformViewModel<KnowledgeGraphContract.ViewModel>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(root) { viewModel.load(root) }

    ProvidePageIdentity(
        PageIdentity(id = "knowledge_graph", name = "Knowledge graph", route = "/graph/$root", panel = "Inspector")
    ) {
        Column(modifier.fillMaxSize()) {
            GraphHeader(
                root = root,
                onClose = onClose,
                onReset = viewModel::reset
            )
            when (val current = state) {
                is ScreenState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is ScreenState.Error -> KaiteyoEmptyState(
                    icon = "🗺️",
                    title = "Graph unavailable",
                    message = current.message,
                    actionLabel = "Retry",
                    onAction = viewModel::retry
                )
                is ScreenState.Loaded -> Column(Modifier.weight(1f)) {
                    // Breadcrumb trail + back/forward (KT-GRAPH-004): the graph
                    // is a navigation surface — the path the user walked is
                    // always visible and reversible.
                    GraphTrailRow(
                        trail = current.trail,
                        onBack = viewModel::goBack,
                        onForward = viewModel::goForward,
                        onJumpTo = viewModel::selectNode
                    )
                    GraphBody(
                        state = current,
                        onSelectNode = viewModel::selectNode,
                        onExpandNode = viewModel::expandNode,
                        onTypeFilter = viewModel::setTypeFilter,
                        onOpenKanji = onOpenKanji
                    )
                }
            }
        }
    }
}

@Composable
private fun GraphHeader(root: String, onClose: () -> Unit, onReset: () -> Unit) {
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
        Text("Knowledge graph", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
        Text(root, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LocalKaiteyoAccent.current.primary)
        Spacer(Modifier.weight(1f))
        KaiteyoPill(text = "Reset view", selected = false, onClick = onReset)
    }
}

@Composable
private fun GraphTrailRow(
    trail: ua.syt0r.kanji.core.knowledge.GraphTrail,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onJumpTo: (String) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space1)
    ) {
        IconButton(onClick = onBack, enabled = trail.canGoBack, modifier = Modifier.size(30.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = if (trail.canGoBack) surfaceColors.textPrimary else surfaceColors.textMuted.copy(alpha = 0.4f)
            )
        }
        // Forward is a real state, but the icon needs one; reuse ArrowForward.
        IconButton(onClick = onForward, enabled = trail.canGoForward, modifier = Modifier.size(30.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Forward",
                tint = if (trail.canGoForward) surfaceColors.textPrimary else surfaceColors.textMuted.copy(alpha = 0.4f)
            )
        }
        // Breadcrumbs: root → … → current. Each step jumps back to it.
        trail.breadcrumbs().forEachIndexed { index, nodeId ->
            if (index > 0) {
                Text("›", color = surfaceColors.textMuted, fontSize = 12.sp)
            }
            val isCurrent = index == trail.breadcrumbs().lastIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isCurrent) accent.primary.copy(alpha = 0.12f) else surfaceColors.surfaceInteractive)
                    .clickable { onJumpTo(nodeId) }
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = nodeId.substringAfter(':')
                        .take(10),
                    color = if (isCurrent) accent.primary else surfaceColors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun GraphBody(
    state: ScreenState.Loaded,
    onSelectNode: (String) -> Unit,
    onExpandNode: (String) -> Unit,
    onTypeFilter: (Set<KnowledgeEdgeType>?) -> Unit,
    onOpenKanji: (String) -> Unit
) {
    Row(Modifier.fillMaxSize().padding(horizontal = Dimens.Space3)) {
        // ── Canvas (flexible) ───────────────────────────────
        Column(Modifier.weight(1f).fillMaxHeight()) {
            // Relationship filters.
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Space1),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
            ) {
                KaiteyoPill(text = "All relations", selected = state.typeFilter == null, onClick = { onTypeFilter(null) })
                KaiteyoPill(
                    text = "Structure",
                    selected = state.typeFilter?.contains(KnowledgeEdgeType.ComponentOf) == true,
                    onClick = {
                        onTypeFilter(setOf(KnowledgeEdgeType.ComponentOf, KnowledgeEdgeType.RadicalOf))
                    }
                )
                KaiteyoPill(
                    text = "Usage",
                    selected = state.typeFilter?.contains(KnowledgeEdgeType.UsedIn) == true,
                    onClick = { onTypeFilter(setOf(KnowledgeEdgeType.UsedIn, KnowledgeEdgeType.AppearsIn)) }
                )
                KaiteyoPill(
                    text = "Related",
                    selected = state.typeFilter?.contains(KnowledgeEdgeType.RelatedTo) == true,
                    onClick = { onTypeFilter(setOf(KnowledgeEdgeType.RelatedTo)) }
                )
            }

            if (state.graph.isEmpty) {
                KaiteyoEmptyState(icon = "🗺️", title = "Empty graph", message = "Nothing to show for this entry.")
            } else {
                // Collapse is a view-only transform (branch clustering):
                // toggling it never mutates the graph, and expanding a node
                // clears its collapse so new neighbors stay visible.
                var collapsedIds by remember { mutableStateOf(setOf<String>()) }
                KnowledgeGraphCanvas(
                    graph = state.graph,
                    selectedId = state.selectedId,
                    onSelectNode = onSelectNode,
                    onExpandNode = { nodeId ->
                        collapsedIds = collapsedIds - nodeId
                        onExpandNode(nodeId)
                    },
                    onToggleCollapse = { nodeId ->
                        collapsedIds = if (nodeId in collapsedIds) collapsedIds - nodeId
                        else collapsedIds + nodeId
                    },
                    collapsedIds = collapsedIds,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(Modifier.width(Dimens.Space3))

        // ── Inspector (fixed) ───────────────────────────────
        Column(
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            if (state.loading) {
                Box(Modifier.fillMaxWidth().padding(Dimens.Space6), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val node = state.selectedNode
                if (node == null) {
                    KaiteyoEmptyState(icon = "🖱️", title = "Select a node", message = "Tap a node on the canvas to inspect it. Tap again to expand.")
                } else {
                    NodeInspector(
                        node = node,
                        graph = state.graph,
                        exhausted = node.id in state.exhaustedIds,
                        onExpand = { onExpandNode(node.id) },
                        onSelectNode = onSelectNode,
                        onOpenKanji = onOpenKanji
                    )
                }
            }
        }
    }
}

@Composable
private fun NodeInspector(
    node: ua.syt0r.kanji.core.knowledge.KnowledgeNode,
    graph: ua.syt0r.kanji.core.knowledge.KnowledgeGraph,
    exhausted: Boolean,
    onExpand: () -> Unit,
    onSelectNode: (String) -> Unit,
    onOpenKanji: (String) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space2)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.RadiusMd))
                .background(surfaceColors.surface)
                .padding(Dimens.Space3),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = node.label,
                fontSize = if (node.kind == KnowledgeNodeKind.Kanji) 44.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                color = surfaceColors.textPrimary
            )
            Text(node.kind.label, style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted)
        }

        node.subtitle?.let {
            KaiteyoSectionCard(title = "Info") {
                Text(it, style = MaterialTheme.typography.bodySmall, color = surfaceColors.textSecondary)
            }
        }

        // Typed extras.
        val extraRows = node.extra.filter { (key, value) -> value.isNotBlank() }
        if (extraRows.isNotEmpty()) {
            KaiteyoSectionCard(title = "Metadata") {
                extraRows.forEach { (key, value) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(key, style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted)
                        Text(value, style = MaterialTheme.typography.labelSmall, color = surfaceColors.textPrimary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Relationship-type breakdown: how the selected node is connected,
        // with the same colors the canvas uses for edges (spec §10: node
        // emphasis + contextual labels). Empty types are omitted.
        val typeCounts = graph.edgeTypeCounts(node.id)
        if (typeCounts.isNotEmpty()) {
            KaiteyoSectionCard(title = "Relationships") {
                typeCounts.forEach { (type, count) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = Dimens.Space1),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
                    ) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    ua.syt0r.kanji.presentation.common.ui.knowledge.edgeColorMapping(
                                        LocalKaiteyoAccent.current.primary
                                    )(type)
                                )
                        )
                        Text(
                            text = type.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = surfaceColors.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = surfaceColors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Neighbors in the visible graph.
        val neighbors = graph.neighbors(node.id, null)
        if (neighbors.isNotEmpty()) {
            KaiteyoSectionCard(title = "Neighbors — ${neighbors.size}") {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space1),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space1)
                ) {
                    neighbors.forEach { neighbor ->
                        KaiteyoPill(
                            text = neighbor.label,
                            selected = false,
                            // Focus the neighbor in the graph (same as tapping it on the canvas).
                            onClick = { onSelectNode(neighbor.id) }
                        )
                    }
                }
            }
        }

        // Visible graph stats: loaded nodes/edges, so "how big is this
        // exploration" is always answerable (spec §9: progressive expansion).
        KaiteyoSectionCard(title = "Graph") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Loaded nodes", style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted)
                Text(graph.nodeCount.toString(), style = MaterialTheme.typography.labelMedium, color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = Dimens.Space1),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Loaded edges", style = MaterialTheme.typography.labelSmall, color = surfaceColors.textMuted)
                Text(graph.edgeCount.toString(), style = MaterialTheme.typography.labelMedium, color = surfaceColors.textPrimary, fontWeight = FontWeight.SemiBold)
            }
        }

        if (exhausted) {
            Text(
                text = "This node is a leaf — nothing else to expand.",
                style = MaterialTheme.typography.labelSmall,
                color = surfaceColors.textMuted
            )
        } else {
            KaiteyoPill(text = "Expand this node", selected = false, onClick = onExpand)
        }

        if (node.kind == KnowledgeNodeKind.Kanji) {
            KaiteyoPill(text = "Open kanji entry", selected = false, onClick = { onOpenKanji(node.label) })
        }
    }
}
