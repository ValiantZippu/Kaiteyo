package ua.syt0r.kanji.presentation.common.ui.knowledge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.focusable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import ua.syt0r.kanji.core.knowledge.GraphLayout
import ua.syt0r.kanji.core.knowledge.GraphPoint
import ua.syt0r.kanji.core.knowledge.KnowledgeEdgeType
import ua.syt0r.kanji.core.knowledge.KnowledgeGraph
import ua.syt0r.kanji.core.knowledge.KnowledgeNode
import ua.syt0r.kanji.core.knowledge.KnowledgeNodeKind
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors

// ============================================================
// KNOWLEDGE GRAPH CANVAS
// ------------------------------------------------------------
// A real graph navigation surface: pan (drag / touch), zoom
// (pinch / scroll wheel / buttons), tap to select (tap again to
// expand), relationship-colored edges, and a legend. Layout is
// deterministic radial (GraphLayout) — nodes never jitter and
// the graph is only ever as large as what has been expanded.
// ============================================================

private class GraphCameraState {
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)
    var scale by mutableFloatStateOf(1f)

    fun pan(dx: Float, dy: Float) {
        offsetX += dx
        offsetY += dy
    }

    fun zoomAt(factor: Float, pivot: Offset) {
        val newScale = (scale * factor).coerceIn(0.25f, 3.5f)
        val world = screenToWorld(pivot)
        scale = newScale
        offsetX = pivot.x - world.x * scale
        offsetY = pivot.y - world.y * scale
    }

    fun reset() {
        offsetX = 0f
        offsetY = 0f
        scale = 1f
    }

    fun screenToWorld(point: Offset): GraphPoint =
        GraphPoint((point.x - offsetX) / scale, (point.y - offsetY) / scale)
}

@Composable
fun KnowledgeGraphCanvas(
    graph: KnowledgeGraph,
    selectedId: String?,
    onSelectNode: (String) -> Unit,
    onExpandNode: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 480.dp,
    /** Node ids collapsed into clusters (their neighbors hidden, spec §9–§10). */
    collapsedIds: Set<String> = emptySet(),
    /** Toggle a node's collapse state; null hides the collapse control. */
    onToggleCollapse: ((String) -> Unit)? = null
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val camera = remember { GraphCameraState() }

    // Branch collapse is a pure view transform (KnowledgeGraph.collapsed):
    // the underlying graph is never mutated, the selected node is pinned so
    // the user can't lose the node they are inspecting.
    val visibleGraph = remember(graph, collapsedIds, selectedId) {
        graph.collapsed(collapsedIds, pinnedId = selectedId)
    }

    val positions = remember(visibleGraph) { GraphLayout.layout(visibleGraph) }
    val edgeMidpoints = remember(visibleGraph, positions) { GraphLayout.edgeMidpoints(visibleGraph.edges, positions) }

    val edgeColor: (KnowledgeEdgeType) -> Color = edgeColorMapping(accent.primary)

    Column(modifier.widthIn(max = 900.dp)) {
        Box(
            modifier
                .fillMaxSize()
                .height(height)
                .clip(RoundedCornerShape(16.dp))
                .background(surfaceColors.surface)
                .border(1.dp, surfaceColors.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .pointerInput(graph, selectedId) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        camera.pan(pan.x, pan.y)
                        if (zoom != 1f) camera.zoomAt(zoom, centroid)
                    }
                }
                .pointerInput(Unit) {
                    // Scroll-wheel zoom around the pointer.
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            val scroll = change.scrollDelta
                            if (scroll != Offset.Zero) {
                                camera.zoomAt(1f + scroll.y * 0.02f, change.position)
                            }
                        }
                    }
                }
                .pointerInput(graph, selectedId) {
                    detectTapGestures { position ->
                        val world = camera.screenToWorld(position)
                        val hit = positions.minByOrNull { (_, p) ->
                            (p.x - world.x) * (p.x - world.x) + (p.y - world.y) * (p.y - world.y)
                        }?.let { (id, p) -> id to p }
                        val nodeId = hit?.takeIf { (_, p) ->
                            kotlin.math.abs(p.x - world.x) < GraphLayout.NodeWidth / 2f + 16f &&
                                kotlin.math.abs(p.y - world.y) < GraphLayout.NodeHeight / 2f + 16f
                        }?.first
                        if (nodeId != null) {
                            if (nodeId == selectedId) onExpandNode(nodeId) else onSelectNode(nodeId)
                        }
                    }
                }
                // Keyboard navigation (spec §20): arrows pan, +/- zoom around
                // the view center, R resets. The canvas is focusable so the
                // graph stays fully usable without a mouse.
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionUp -> {
                            camera.pan(0f, 40f); true
                        }
                        Key.DirectionDown -> {
                            camera.pan(0f, -40f); true
                        }
                        Key.DirectionLeft -> {
                            camera.pan(40f, 0f); true
                        }
                        Key.DirectionRight -> {
                            camera.pan(-40f, 0f); true
                        }
                        Key.Equals -> {
                            camera.zoomAt(1.25f, Offset(camera.offsetX + 300f, camera.offsetY + 240f)); true
                        }
                        Key.Minus -> {
                            camera.zoomAt(0.8f, Offset(camera.offsetX + 300f, camera.offsetY + 240f)); true
                        }
                        Key.R -> {
                            camera.reset(); true
                        }
                        else -> false
                    }
                }
        ) {
            // Camera-transformed world layer: edges + nodes.
            Box(Modifier.fillMaxSize().graphicsLayer {
                translationX = camera.offsetX
                translationY = camera.offsetY
                scaleX = camera.scale
                scaleY = camera.scale
                transformOrigin = TransformOrigin(0f, 0f)
            }) {
                // Edges.
                Canvas(Modifier.fillMaxSize()) {
                    visibleGraph.edges.forEach { edge ->
                        val from = positions[edge.from] ?: return@forEach
                        val to = positions[edge.to] ?: return@forEach
                        drawLine(
                            color = edgeColor(edge.type).copy(alpha = 0.7f),
                            start = Offset(from.x, from.y),
                            end = Offset(to.x, to.y),
                            strokeWidth = 2f
                        )
                    }
                    edgeMidpoints.forEach { (edge, point) ->
                        drawCircle(
                            color = edgeColor(edge.type),
                            radius = 3.5f,
                            center = Offset(point.x, point.y)
                        )
                    }
                }

                // Nodes.
                positions.forEach { (nodeId, point) ->
                    val node = visibleGraph.node(nodeId)
                    if (node == null) return@forEach
                    val hiddenCount = node.extra[KnowledgeGraph.HIDDEN_COUNT_KEY]
                        ?.toIntOrNull()
                    GraphNodeChip(
                        node = node,
                        color = kindColor(node.kind, accent.primary),
                        selected = nodeId == selectedId,
                        clusterBadge = hiddenCount,
                        onTap = {
                            if (nodeId == selectedId) onExpandNode(nodeId) else onSelectNode(nodeId)
                        },
                        onCollapse = onToggleCollapse?.let { toggle -> { toggle(nodeId) } },
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (point.x - GraphLayout.NodeWidth / 2f).roundToInt(),
                                    (point.y - GraphLayout.NodeHeight / 2f).roundToInt()
                                )
                            }
                            .size(GraphLayout.NodeWidth.dp, GraphLayout.NodeHeight.dp)
                    )
                }
            }

            // Overlay controls.
            GraphCanvasControls(
                camera = camera,
                onExpandSelected = { selectedId?.let(onExpandNode) },
                onCollapseSelected = onToggleCollapse?.let { toggle ->
                    { selectedId?.let(toggle) }
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            )
        }

        // Edge legend + keyboard hints.
        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KnowledgeEdgeType.entries.forEach { type ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(edgeColor(type)))
                    Text(
                        text = type.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = surfaceColors.textMuted
                    )
                }
            }
        }
        Text(
            text = "Click canvas to focus · arrows pan · +/- zoom · R reset",
            style = MaterialTheme.typography.labelSmall,
            color = surfaceColors.textMuted,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun GraphNodeChip(
    node: KnowledgeNode,
    color: Color,
    selected: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    /** When set, the chip renders as a cluster with a "+N" badge. */
    clusterBadge: Int? = null,
    /** When non-null, a small collapse control appears on the chip. */
    onCollapse: (() -> Unit)? = null
) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = if (selected) 0.22f else 0.14f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) color else color.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onTap)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Column(Modifier.weight(1f, fill = false).widthIn(max = 84.dp)) {
            Text(
                text = node.label,
                color = surfaceColors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (node.kind == KnowledgeNodeKind.Kanji) 18.sp else 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = node.kind.label,
                color = surfaceColors.textMuted,
                fontSize = 9.sp,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
        if (clusterBadge != null && clusterBadge > 0) {
            Text(
                text = "+$clusterBadge",
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (onCollapse != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.18f))
                    .clickable(onClick = onCollapse)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "−",
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GraphCanvasControls(
    camera: GraphCameraState,
    onExpandSelected: () -> Unit,
    modifier: Modifier = Modifier,
    /** Collapse control; null hides it (no collapse support wired). */
    onCollapseSelected: (() -> Unit)? = null
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColors.surfaceElevated.copy(alpha = 0.95f))
            .border(1.dp, surfaceColors.border.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = { camera.zoomAt(1.25f, Offset.Zero) }, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "Zoom in", tint = surfaceColors.textPrimary, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = { camera.zoomAt(0.8f, Offset.Zero) }, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Filled.Remove, contentDescription = "Zoom out", tint = surfaceColors.textPrimary, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = { camera.reset() }, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Filled.RestartAlt, contentDescription = "Reset view", tint = surfaceColors.textPrimary, modifier = Modifier.size(16.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onExpandSelected)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Expand",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = accent.primary,
                textAlign = TextAlign.Center
            )
        }
        if (onCollapseSelected != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onCollapseSelected)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Collapse",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = surfaceColors.textMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Stable, relationship-type edge colors (semantic, not theme-dependent). */
internal fun edgeColorMapping(accent: Color): (KnowledgeEdgeType) -> Color = { type ->
    when (type) {
        KnowledgeEdgeType.Contains -> Color(0xFF4FC3F7)
        KnowledgeEdgeType.ComponentOf -> Color(0xFF81C784)
        KnowledgeEdgeType.RadicalOf -> Color(0xFFA5D6A7)
        KnowledgeEdgeType.UsedIn -> Color(0xFFFFB74D)
        KnowledgeEdgeType.AppearsIn -> Color(0xFFBA68C8)
        KnowledgeEdgeType.ExampleOf -> Color(0xFFF06292)
        KnowledgeEdgeType.RelatedTo -> accent
    }
}

/** Stable node-kind colors. */
internal fun kindColor(kind: KnowledgeNodeKind, accent: Color): Color = when (kind) {
    KnowledgeNodeKind.Kanji -> accent
    KnowledgeNodeKind.Radical -> Color(0xFF26A69A)
    KnowledgeNodeKind.Word -> Color(0xFF42A5F5)
    KnowledgeNodeKind.Sentence -> Color(0xFF66BB6A)
    KnowledgeNodeKind.Grammar -> Color(0xFFFFA726)
    KnowledgeNodeKind.Media -> Color(0xFFEC407A)
}
