package ua.syt0r.kanji.presentation.common.ui.kaiteyo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ua.syt0r.kanji.presentation.common.theme.LocalAnimationConfig
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// ============================================================
// KAITEYO GLYPH GRAPH — Connected Interactive Knowledge System
//
// Features:
//   · Multi-tier concentric orbit hierarchy (Radicals/Components -> Phonetics -> Compounds)
//   · Spring physics motion system with stable resting points
//   · Node hover, selection, expansion, focus glow pulse
//   · Pan & Zoom with boundary constraints
//   · Desktop contextual right-click menu & keyboard navigation support
//   · Theme-aware: Light, Dark, OLED, Sepia, Cream, Paper, Midnight
//   · Reduced motion support via LocalAnimationConfig
// ============================================================

// ── Node & Edge Models ──────────────────────────────────────

enum class GlyphNodeType {
    RootKanji,
    Radical,
    Phonetic,
    SemanticComponent,
    Compound
}

data class GlyphNode(
    val character: String,
    val meaning: String,
    val type: GlyphNodeType = GlyphNodeType.SemanticComponent,
    val depth: Int = 1,
    val readingHint: String? = null,
    val strokes: Int? = null,
    val children: List<GlyphNode> = emptyList()
) {
    val isRadical: Boolean get() = type == GlyphNodeType.Radical
    val isPhonetic: Boolean get() = type == GlyphNodeType.Phonetic
}

data class GlyphEdge(
    val from: String,
    val to: String,
    val label: String,
    val type: GlyphNodeType
)

private data class NodePosition(
    val center: Offset,
    val radius: Float,
    val node: GlyphNode,
    val ringIndex: Int
)

// ── Full Interactive Graph Canvas ───────────────────────────

@Composable
fun AnimatedGlyphGraph(
    rootCharacter: String,
    rootMeaning: String,
    components: List<GlyphNode>,
    onCharacterClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onPlayAudio: ((String) -> Unit)? = null,
    enableControls: Boolean = true,
    showDebugInfo: Boolean = false
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val animationConfig = LocalAnimationConfig.current
    val clipboardManager = LocalClipboardManager.current
    val isReducedMotion = animationConfig.reducedMotion

    var expandedNodes by remember(rootCharacter) { mutableStateOf(setOf(rootCharacter)) }
    var selectedNode by remember(rootCharacter) { mutableStateOf<GlyphNode?>(null) }
    var contextMenuNode by remember { mutableStateOf<GlyphNode?>(null) }
    var contextMenuOffset by remember { mutableStateOf(DpOffset.Zero) }

    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Orbit drift for alive feeling (disabled when reduced motion is requested)
    val infiniteTransition = rememberInfiniteTransition(label = "orbitDrift")
    val driftAngle by if (!isReducedMotion) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 140_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "drift"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val graph = remember(rootCharacter, rootMeaning, components, expandedNodes) {
        buildInteractiveGraph(rootCharacter, rootMeaning, components, expandedNodes)
    }
    val nodes = remember(graph) { flattenNodes(graph) }
    val edges = remember(graph) { flattenEdges(graph) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColors.surface)
            .border(1.dp, surfaceColors.surfaceInteractive.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoomDelta, _ ->
                    zoom = (zoom * zoomDelta).coerceIn(0.6f, 2.5f)
                    panOffset = Offset(
                        (panOffset.x + pan.x).coerceIn(-400f, 400f),
                        (panOffset.y + pan.y).coerceIn(-400f, 400f)
                    )
                }
            }
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val centerCanvas = Offset(widthPx / 2f + panOffset.x, heightPx / 2f + panOffset.y)
        val minDim = min(widthPx, heightPx)

        val positions = remember(nodes, zoom, driftAngle, centerCanvas, minDim) {
            computeConcentricLayout(nodes, centerCanvas, minDim, zoom, driftAngle)
        }

        // Concentric Orbit Rings Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ringRadii = listOf(
                minDim * 0.28f * zoom,
                minDim * 0.44f * zoom,
                minDim * 0.60f * zoom
            )
            ringRadii.forEachIndexed { idx, ringRadius ->
                val alpha = (0.25f - idx * 0.06f).coerceAtLeast(0.08f)
                val ringColor = when (idx) {
                    0 -> accent.primary.copy(alpha = alpha)
                    1 -> accent.secondary.copy(alpha = alpha)
                    else -> surfaceColors.surfaceInteractive.copy(alpha = alpha * 1.5f)
                }
                drawCircle(
                    color = ringColor,
                    center = centerCanvas,
                    radius = ringRadius,
                    style = Stroke(
                        width = (1.5f * density.density),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), phase = driftAngle * (idx + 1))
                    )
                )
            }
        }

        // Relationship Lines with Progressive Reveal
        AnimatedEdgeCanvas(
            edges = edges,
            positions = positions,
            accentColor = accent.primary,
            secondaryAccent = accent.secondary,
            surfaceColors = surfaceColors,
            isReducedMotion = isReducedMotion
        )

        // Nodes Rendering
        positions.forEachIndexed { index, pos ->
            val isRoot = pos.node.depth == 0
            val isSelected = selectedNode?.character == pos.node.character
            val isExpanded = pos.node.character in expandedNodes

            InteractiveGlyphBubble(
                node = pos.node,
                center = pos.center,
                radius = pos.radius,
                index = index,
                isRoot = isRoot,
                isSelected = isSelected,
                isExpanded = isExpanded,
                accent = accent.primary,
                secondaryAccent = accent.secondary,
                surfaceColors = surfaceColors,
                isReducedMotion = isReducedMotion,
                onClick = {
                    selectedNode = pos.node
                    if (pos.node.children.isNotEmpty()) {
                        expandedNodes = if (pos.node.character in expandedNodes) {
                            expandedNodes - pos.node.character
                        } else {
                            expandedNodes + pos.node.character
                        }
                    }
                    if (pos.node.character != rootCharacter) {
                        onCharacterClick(pos.node.character)
                    }
                },
                onLongClick = { offset ->
                    contextMenuNode = pos.node
                    contextMenuOffset = DpOffset(
                        with(density) { offset.x.toDp() },
                        with(density) { offset.y.toDp() }
                    )
                }
            )
        }

        // Overlay Navigation / Controls
        if (enableControls) {
            GraphControlsOverlay(
                zoom = zoom,
                onZoomIn = { zoom = (zoom * 1.2f).coerceAtMost(2.5f) },
                onZoomOut = { zoom = (zoom * 0.8f).coerceAtLeast(0.6f) },
                onReset = {
                    zoom = 1f
                    panOffset = Offset.Zero
                    expandedNodes = setOf(rootCharacter)
                    selectedNode = null
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            )
        }

        // Legend at top-left
        GraphLegendOverlay(
            accent = accent.primary,
            secondaryAccent = accent.secondary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        )

        // Debug info badge if requested
        if (showDebugInfo) {
            Text(
                text = "Nodes: ${nodes.size} | Edges: ${edges.size} | Zoom: ${((zoom * 100).roundToInt())}%",
                fontSize = 9.sp,
                color = surfaceColors.textMuted,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .background(surfaceColors.surfaceInteractive.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        // Context Menu on Right-Click / Long-Press
        contextMenuNode?.let { targetNode ->
            DropdownMenu(
                expanded = true,
                onDismissRequest = { contextMenuNode = null },
                offset = contextMenuOffset,
                modifier = Modifier.background(surfaceColors.surface)
            ) {
                DropdownMenuItem(
                    text = { Text("Explore ${targetNode.character}", color = surfaceColors.textPrimary) },
                    onClick = {
                        contextMenuNode = null
                        onCharacterClick(targetNode.character)
                    }
                )
                if (targetNode.readingHint != null && onPlayAudio != null) {
                    DropdownMenuItem(
                        text = { Text("Play Pronunciation", color = surfaceColors.textPrimary) },
                        onClick = {
                            contextMenuNode = null
                            onPlayAudio(targetNode.readingHint)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Copy Character", color = surfaceColors.textPrimary) },
                    onClick = {
                        contextMenuNode = null
                        clipboardManager.setText(AnnotatedString(targetNode.character))
                    }
                )
            }
        }
    }
}

// ── Compact Graph for Cards & Summaries ──────────────────────

@Composable
fun CompactGlyphGraph(
    character: String,
    meaning: String,
    components: List<GlyphNode>,
    onCharacterClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val isReducedMotion = LocalAnimationConfig.current.reducedMotion

    val graph = remember(character, meaning, components) {
        buildInteractiveGraph(character, meaning, components, setOf(character))
    }
    val nodes = remember(graph) { flattenNodes(graph) }
    val edges = remember(graph) { flattenEdges(graph) }

    val orbitProgress = remember { Animatable(if (isReducedMotion) 360f else 0f) }
    LaunchedEffect(Unit) {
        if (!isReducedMotion) {
            delay(80)
            orbitProgress.animateTo(
                targetValue = 360f,
                animationSpec = tween(1000, easing = FastOutSlowInEasing)
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColors.surface)
            .border(1.dp, surfaceColors.surfaceInteractive.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Glyph Graph",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = surfaceColors.textPrimary
                )
                Text(
                    text = "Interactive visual decomposition of $character",
                    fontSize = 11.sp,
                    color = surfaceColors.textMuted
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendDot(accent.primary, "Radical")
                LegendDot(accent.secondary, "Phonetic")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Compact Canvas Area
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            val centerCanvas = Offset(widthPx / 2f, heightPx / 2f)
            val minDim = min(widthPx, heightPx)

            val positions = remember(nodes, centerCanvas, minDim) {
                computeConcentricLayout(nodes, centerCanvas, minDim, zoom = 0.88f, driftAngle = 0f)
            }

            // Orbit Ring Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val ringRadius = minDim * 0.36f
                drawCircle(
                    color = accent.primary.copy(alpha = 0.35f),
                    center = centerCanvas,
                    radius = ringRadius,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), phase = orbitProgress.value)
                    )
                )
            }

            // Edges
            AnimatedEdgeCanvas(
                edges = edges,
                positions = positions,
                accentColor = accent.primary,
                secondaryAccent = accent.secondary,
                surfaceColors = surfaceColors,
                isReducedMotion = isReducedMotion
            )

            // Nodes
            positions.forEachIndexed { index, pos ->
                InteractiveGlyphBubble(
                    node = pos.node,
                    center = pos.center,
                    radius = pos.radius,
                    index = index,
                    isRoot = pos.node.depth == 0,
                    isSelected = false,
                    isExpanded = true,
                    accent = accent.primary,
                    secondaryAccent = accent.secondary,
                    surfaceColors = surfaceColors,
                    isReducedMotion = isReducedMotion,
                    onClick = { onCharacterClick(pos.node.character) },
                    onLongClick = {}
                )
            }
        }
    }
}

// ── Graph Construction & Concentric Layout Algorithm ────────

private fun buildInteractiveGraph(
    rootChar: String,
    rootMeaning: String,
    components: List<GlyphNode>,
    expandedNodes: Set<String>
): GlyphNode {
    return GlyphNode(
        character = rootChar,
        meaning = rootMeaning,
        type = GlyphNodeType.RootKanji,
        depth = 0,
        children = components.map { child ->
            if (child.character in expandedNodes && child.children.isNotEmpty()) child
            else child.copy(children = emptyList())
        }
    )
}

private fun flattenNodes(node: GlyphNode, depth: Int = 0): List<GlyphNode> {
    val result = mutableListOf(node.copy(depth = depth))
    if (depth < 3) {
        node.children.forEach { child ->
            result.addAll(flattenNodes(child, depth + 1))
        }
    }
    return result.distinctBy { it.character }
}

private fun flattenEdges(node: GlyphNode): List<GlyphEdge> {
    val result = mutableListOf<GlyphEdge>()
    node.children.forEach { child ->
        val label = when (child.type) {
            GlyphNodeType.Radical -> "radical"
            GlyphNodeType.Phonetic -> "phonetic"
            GlyphNodeType.Compound -> "compound"
            else -> "component"
        }
        result.add(GlyphEdge(from = node.character, to = child.character, label = label, type = child.type))
        result.addAll(flattenEdges(child))
    }
    return result
}

private fun computeConcentricLayout(
    nodes: List<GlyphNode>,
    centerCanvas: Offset,
    minDim: Float,
    zoom: Float,
    driftAngle: Float
): List<NodePosition> {
    if (nodes.isEmpty()) return emptyList()
    val root = nodes.firstOrNull { it.depth == 0 } ?: nodes.first()
    val result = mutableListOf<NodePosition>()

    // Central root node sizing
    val rootRadius = (minDim * 0.14f).coerceIn(40f, 56f) * zoom
    result.add(NodePosition(center = centerCanvas, radius = rootRadius, node = root, ringIndex = 0))

    val driftRad = Math.toRadians(driftAngle.toDouble()).toFloat()
    val byDepth = nodes.filter { it.depth > 0 }.groupBy { it.depth }

    byDepth.toSortedMap().forEach { (depth, depthNodes) ->
        val ringBaseFraction = when (depth) {
            1 -> 0.34f
            2 -> 0.52f
            else -> 0.70f
        }
        val ringRadius = minDim * ringBaseFraction * zoom
        val nodeRadius = (minDim * (0.10f - depth * 0.012f)).coerceIn(22f, 40f) * zoom

        val count = depthNodes.size
        val angleStep = if (count > 0) 2.0 * Math.PI / count else 0.0
        val baseOffset = -Math.PI / 2.0 + (depth * 0.35) + driftRad * (0.03f / depth)

        depthNodes.forEachIndexed { index, node ->
            val angle = baseOffset + index * angleStep
            val cx = centerCanvas.x + ringRadius * cos(angle).toFloat()
            val cy = centerCanvas.y + ringRadius * sin(angle).toFloat()
            result.add(
                NodePosition(
                    center = Offset(cx, cy),
                    radius = nodeRadius,
                    node = node,
                    ringIndex = depth
                )
            )
        }
    }
    return result
}

// ── Interactive Spring Bubble Node ──────────────────────────

@Composable
private fun InteractiveGlyphBubble(
    node: GlyphNode,
    center: Offset,
    radius: Float,
    index: Int,
    isRoot: Boolean,
    isSelected: Boolean,
    isExpanded: Boolean,
    accent: Color,
    secondaryAccent: Color,
    surfaceColors: SurfaceColors,
    isReducedMotion: Boolean,
    onClick: () -> Unit,
    onLongClick: (Offset) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Spring entrance animation
    val animProgress = remember { Animatable(if (isReducedMotion) 1f else 0f) }
    LaunchedEffect(node.character) {
        if (!isReducedMotion) {
            delay((index * 45L).coerceAtMost(300L))
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    // Glow pulse on focus/selection
    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected || isHovered) 0.5f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "glowAlpha"
    )

    val scale = (animProgress.value * (if (isHovered) 1.08f else 1.0f)).coerceAtLeast(0.01f)
    val alpha = animProgress.value

    val nodeColor = when {
        isRoot -> accent
        node.isRadical -> accent.copy(alpha = 0.35f)
        node.isPhonetic -> secondaryAccent.copy(alpha = 0.35f)
        else -> surfaceColors.surfaceInteractive.copy(alpha = 0.8f)
    }

    val borderColor = when {
        isRoot -> accent
        isSelected -> accent
        node.isRadical -> accent.copy(alpha = 0.85f)
        node.isPhonetic -> secondaryAccent.copy(alpha = 0.85f)
        else -> surfaceColors.surfaceInteractive
    }

    val textColor = when {
        isRoot -> surfaceColors.textInverse
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (center.x - radius).roundToInt(),
                    (center.y - radius).roundToInt()
                )
            }
            .size((radius * 2).dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .drawBehind {
                if (glowAlpha > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                (if (node.isPhonetic) secondaryAccent else accent).copy(alpha = glowAlpha),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2, size.height / 2),
                            radius = size.width * 0.75f
                        ),
                        radius = size.width * 0.7f
                    )
                }
            }
            .clip(CircleShape)
            .background(nodeColor)
            .border(
                width = if (isSelected || isRoot) 2.5.dp else 1.5.dp,
                color = borderColor,
                shape = CircleShape
            )
            .pointerInput(node.character) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { offset -> onLongClick(offset) }
                )
            }
            .semantics {
                role = Role.Button
                contentDescription = "${node.character}, ${node.meaning}"
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = node.character,
                fontSize = if (isRoot) (radius * 0.72f).sp else (radius * 0.78f).sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
            if (radius >= 20f && node.meaning.isNotBlank()) {
                Text(
                    text = node.meaning.take(14),
                    fontSize = (radius * 0.30f).coerceIn(9f, 13f).sp,
                    color = textColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Animated Edge Lines Canvas ──────────────────────────────

@Composable
private fun AnimatedEdgeCanvas(
    edges: List<GlyphEdge>,
    positions: List<NodePosition>,
    accentColor: Color,
    secondaryAccent: Color,
    surfaceColors: SurfaceColors,
    isReducedMotion: Boolean
) {
    val progress = remember { Animatable(if (isReducedMotion) 1f else 0f) }
    LaunchedEffect(edges.size) {
        if (!isReducedMotion) {
            progress.snapTo(0f)
            progress.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val frac = progress.value
        edges.forEachIndexed { edgeIndex, edge ->
            val fromPos = positions.find { it.node.character == edge.from }
            val toPos = positions.find { it.node.character == edge.to }
            if (fromPos != null && toPos != null) {
                val edgeFrac = ((frac * edges.size - edgeIndex) / 1.5f).coerceIn(0f, 1f)
                if (edgeFrac <= 0f) return@forEachIndexed

                val edgeColor = when (edge.type) {
                    GlyphNodeType.Radical -> accentColor.copy(alpha = 0.65f)
                    GlyphNodeType.Phonetic -> secondaryAccent.copy(alpha = 0.65f)
                    else -> surfaceColors.textMuted.copy(alpha = 0.35f)
                }

                val endX = fromPos.center.x + (toPos.center.x - fromPos.center.x) * edgeFrac
                val endY = fromPos.center.y + (toPos.center.y - fromPos.center.y) * edgeFrac

                drawLine(
                    color = edgeColor,
                    start = fromPos.center,
                    end = Offset(endX, endY),
                    strokeWidth = 1.8.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                )

                // Join indicator node dot
                if (edgeFrac >= 1f) {
                    val midX = (fromPos.center.x + toPos.center.x) / 2f
                    val midY = (fromPos.center.y + toPos.center.y) / 2f
                    drawCircle(
                        color = edgeColor.copy(alpha = 0.4f),
                        radius = 2.5.dp.toPx(),
                        center = Offset(midX, midY)
                    )
                }
            }
        }
    }
}

// ── Overlays & Legends ──────────────────────────────────────

@Composable
private fun GraphControlsOverlay(
    zoom: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = modifier
            .background(surfaceColors.surfaceInteractive.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
            .border(1.dp, surfaceColors.surfaceInteractive, RoundedCornerShape(10.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onZoomIn, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = surfaceColors.textPrimary, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onZoomOut, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = surfaceColors.textPrimary, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Autorenew, contentDescription = "Reset View", tint = surfaceColors.textSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun GraphLegendOverlay(
    accent: Color,
    secondaryAccent: Color,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = modifier
            .background(surfaceColors.surface.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
            .border(1.dp, surfaceColors.surfaceInteractive.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendDot(accent, "Radical")
        LegendDot(secondaryAccent, "Phonetic")
        LegendDot(surfaceColors.surfaceInteractive.copy(alpha = 0.9f), "Component")
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = LocalSurfaceColors.current.textMuted)
    }
}
