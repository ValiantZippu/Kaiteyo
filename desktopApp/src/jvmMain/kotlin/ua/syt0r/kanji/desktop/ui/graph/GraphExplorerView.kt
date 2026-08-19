package ua.syt0r.kanji.desktop.ui.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsSearchField
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTextField
import ua.syt0r.kanji.desktop.designsystem.DsToolbar
import ua.syt0r.kanji.desktop.designsystem.DsToolbarDivider
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.graph.GraphNode
import ua.syt0r.kanji.desktop.engine.graph.GraphNodeDetail
import ua.syt0r.kanji.desktop.engine.graph.GraphNodeKind
import ua.syt0r.kanji.desktop.engine.graph.GraphPathHop
import ua.syt0r.kanji.desktop.engine.graph.KnowledgeState
import ua.syt0r.kanji.desktop.engine.l10n.resolveSuiteString

/**
 * The Knowledge Graph explorer: search kanji/vocabulary, open a node and
 * traverse real relations (components, radical, words containing a kanji,
 * kanji inside a word). The breadcrumb stack keeps the traversal path
 * visible and one click away.
 */
@Composable
fun GraphExplorerView(state: AppState, modifier: Modifier = Modifier) {
    val graph = state.knowledgeGraph
    val sc = surfaceColors()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<GraphNode>>(emptyList()) }
    var current by remember { mutableStateOf<String?>(null) }
    val visited = remember { mutableStateListOf<String>() }

    // Deep links from dictionary surfaces ("Explore in graph") land here with
    // the expression staged on AppState — consume it exactly once.
    LaunchedEffect(state.pendingGraphNode) {
        state.pendingGraphNode?.let { expression ->
            state.pendingGraphNode = null
            query = expression
            results = graph.search(expression)
            current = expression
            visited.clear()
        }
    }

    fun openNode(expression: String) {
        if (current != null) visited.add(current!!)
        current = expression
    }

    Column(modifier.fillMaxSize()) {
        DsToolbar(
            title = resolveSuiteString { graphTitle },
            subtitle = resolveSuiteString { graphSubtitle },
            backIcon = if (current != null) Icons.AutoMirrored.Filled.ArrowBack else null,
            onBack = {
                if (visited.isNotEmpty()) {
                    current = visited.removeAt(visited.lastIndex)
                } else {
                    current = null
                }
            }
        )
        DsToolbarDivider()

        DsSearchField(
            value = query,
            onValueChange = { raw ->
                query = raw
                current = null
                visited.clear()
                results = if (raw.isBlank()) emptyList() else graph.search(raw)
            },
            placeholder = resolveSuiteString { graphSearchPlaceholder },
            modifier = Modifier.padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Md)
        )

        val expression = current
        if (expression == null) {
            SearchResults(
                results = results,
                hasQuery = query.isNotBlank(),
                graphAvailable = graph.available,
                onOpen = ::openNode,
                modifier = Modifier.weight(1f)
            )
        } else {
            NodeDetail(
                state = state,
                expression = expression,
                onNavigate = ::openNode,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ------------------------------------------------------------
// Search results
// ------------------------------------------------------------

@Composable
private fun SearchResults(
    results: List<GraphNode>,
    hasQuery: Boolean,
    graphAvailable: Boolean,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = DsSpacing.Lg,
            vertical = DsSpacing.Sm
        ),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        if (!graphAvailable) {
            item {
                Text(
                    text = "Install a dictionary to explore the knowledge graph.",
                    color = sc.textSecondary,
                    fontSize = DsType.Body
                )
            }
        } else if (hasQuery && results.isEmpty()) {
            item {
                Text(
                    text = resolveSuiteString { graphNoResults },
                    color = sc.textMuted,
                    fontSize = DsType.Body
                )
            }
        } else {
            items(results, key = { it.id }) { node ->
                SearchResultRow(node, onClick = { onOpen(node.expression) })
            }
        }
    }
}

@Composable
private fun SearchResultRow(node: GraphNode, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    val ac = accent()

    DsCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DsSpacing.Md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = node.expression,
                color = ac.primary,
                fontSize = if (node.kind == GraphNodeKind.Kanji) 26.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(64.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = node.meanings.joinToString("; ").ifBlank { node.kind.name },
                    color = sc.textPrimary,
                    fontSize = DsType.Body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = node.readings.joinToString(" · ").ifBlank { node.kind.name },
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
            NodeKindBadge(node.kind)
        }
    }
}

@Composable
private fun NodeKindBadge(kind: GraphNodeKind, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    val ac = accent()
    Text(
        text = kind.name,
        color = ac.primary,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ac.primary.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

// ------------------------------------------------------------
// Node detail
// ------------------------------------------------------------

@Composable
private fun NodeDetail(
    state: AppState,
    expression: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val graph = state.knowledgeGraph
    var detail by remember { mutableStateOf<GraphNodeDetail?>(null) }
    var pathOpen by remember { mutableStateOf(false) }
    var pathTarget by remember { mutableStateOf("") }
    var pathResult by remember { mutableStateOf<List<GraphPathHop>?>(null) }
    var pathError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(expression) {
        detail = graph.detail(expression)
        pathOpen = false
        pathTarget = ""
        pathResult = null
        pathError = null
    }

    fun findPath(from: String, to: String) {
        if (to.isBlank()) {
            pathResult = null
            pathError = resolveSuiteString { pathBlankError }
            return
        }
        pathError = null
        pathResult = graph.pathBetween(from, to)
        if (pathResult == null) {
            pathError = resolveSuiteString { pathNotfound }
        }
    }

    val current = detail
    if (current == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading…", color = androidx.compose.ui.graphics.Color.Gray)
        }
        return
    }

    val sc = surfaceColors()
    val ac = accent()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = DsSpacing.Lg,
            vertical = DsSpacing.Md
        ),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        // ---- Node hero -------------------------------------------
        item {
            DsCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = current.node.expression,
                            color = sc.textPrimary,
                            fontSize = if (current.node.kind == GraphNodeKind.Kanji) 56.sp else 34.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            NodeKindBadge(current.node.kind)
                            KnowledgeBadge(current.knowledge)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                        DsButton(
                            text = resolveSuiteString { practiceLabel },
                            icon = Icons.Default.PlayArrow,
                            kind = DsButtonKind.Secondary,
                            compact = true,
                            onClick = { state.startReview(query = "character:$expression") }
                        )
                        DsButton(
                            text = resolveSuiteString { findPathLabel },
                            icon = Icons.Default.Route,
                            kind = DsButtonKind.Secondary,
                            compact = true,
                            onClick = { pathTarget = ""; pathOpen = true }
                        )
                    }
                    if (current.node.readings.isNotEmpty()) {
                        Text(
                            text = current.node.readings.joinToString(" · "),
                            color = ac.primary,
                            fontSize = DsType.BodyLarge
                        )
                    }
                    if (current.node.meanings.isNotEmpty()) {
                        Text(
                            text = current.node.meanings.joinToString("; "),
                            color = sc.textSecondary,
                            fontSize = DsType.Body
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                        current.node.jlpt?.let {
                            Text("${resolveSuiteString { jlptLabel }} N$it", color = sc.textMuted, fontSize = DsType.Caption)
                        }
                        current.node.frequencyRank?.let {
                            Text("${resolveSuiteString { frequencyLabel }} #$it", color = sc.textMuted, fontSize = DsType.Caption)
                        }
                        current.node.strokeCount?.let {
                            Text("$it strokes", color = sc.textMuted, fontSize = DsType.Caption)
                        }
                    }
                }
            }
        }

        // ---- Path finder (Phase 4: 食べる → 食 → 食事 traversal) ----
        if (pathOpen) {
            item {
                DsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(DsSpacing.Lg),
                        verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                    ) {
                        Text(
                            text = "${resolveSuiteString { pathFromLabel }} ${current.node.expression}",
                            color = sc.textPrimary,
                            fontSize = DsType.BodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                        ) {
                            DsTextField(
                                value = pathTarget,
                                onValueChange = { pathTarget = it },
                                placeholder = resolveSuiteString { pathTargetPlaceholder },
                                modifier = Modifier.weight(1f)
                            )
                            DsButton(
                                text = resolveSuiteString { pathSearchLabel },
                                icon = Icons.Default.Search,
                                compact = true,
                                onClick = { findPath(current.node.expression, pathTarget) }
                            )
                        }
                        pathError?.let { message ->
                            Text(message, color = sc.textSecondary, fontSize = DsType.Caption)
                        }
                        val hops = pathResult
                        if (hops != null) {
                            if (hops.isEmpty()) {
                                Text(resolveSuiteString { pathSameNode }, color = sc.textMuted, fontSize = DsType.Caption)
                            } else {
                                Text(
                                    text = hops.joinToString("  →  ") { hop -> hop.toExpression },
                                    color = sc.textPrimary,
                                    fontSize = DsType.Body,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = hops.joinToString("  ·  ") { hop -> "${hop.edge.name} → ${hop.toExpression}" },
                                    color = sc.textMuted,
                                    fontSize = DsType.Caption
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---- Traversal chips --------------------------------------
        if (current.components.isNotEmpty()) {
            item {
                ChipSection(
                    label = resolveSuiteString { componentsLabel },
                    nodes = current.components,
                    onNavigate = onNavigate
                )
            }
        }
        current.radical?.let { radical ->
            item {
                ChipSection(
                    label = "Radical",
                    nodes = listOf(radical),
                    onNavigate = onNavigate
                )
            }
        }
        if (current.words.isNotEmpty()) {
            item {
                ChipSection(
                    label = resolveSuiteString { relatedWordsLabel },
                    nodes = current.words,
                    onNavigate = onNavigate
                )
            }
        }
        if (current.relatedKanji.isNotEmpty()) {
            item {
                ChipSection(
                    label = "Kanji inside",
                    nodes = current.relatedKanji,
                    onNavigate = onNavigate
                )
            }
        }

        // ---- Media exposure --------------------------------------
        if (current.mediaAppearances.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    Text(
                        text = resolveSuiteString { seenInMediaLabel },
                        color = sc.textPrimary,
                        fontSize = DsType.BodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    current.mediaAppearances.take(6).forEach { appearance ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "▶ ${appearance.mediaTitle}",
                                color = sc.textSecondary,
                                fontSize = DsType.Body,
                                modifier = Modifier.weight(1f)
                            )
                            appearance.timestamp?.let {
                                Text(
                                    text = formatTimestamp(it),
                                    color = sc.textMuted,
                                    fontSize = DsType.Caption
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KnowledgeBadge(knowledge: KnowledgeState, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    val ac = accent()
    val (label, color) = when (knowledge) {
        KnowledgeState.Unknown -> "New to you" to sc.textMuted
        KnowledgeState.New -> "New card" to sc.textSecondary
        KnowledgeState.Learning -> "Learning" to ac.secondary
        KnowledgeState.Known -> "Known" to ac.primary
        KnowledgeState.Mature -> "Mature" to ac.primary
        KnowledgeState.Mined -> "Mined" to ac.primary
        KnowledgeState.Suspended -> "Suspended" to sc.textMuted
    }
    Text(
        text = label,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipSection(
    label: String,
    nodes: List<GraphNode>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val ac = accent()

    Column(modifier, verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
        Text(
            text = label,
            color = sc.textPrimary,
            fontSize = DsType.BodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            nodes.take(24).forEach { node ->
                NodeChip(node, onClick = { onNavigate(node.expression) })
            }
        }
    }
}

@Composable
private fun NodeChip(node: GraphNode, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (hovered) ac.primary.copy(alpha = 0.18f) else sc.surfaceInteractive)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = node.expression,
            color = sc.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (node.meanings.isNotEmpty()) {
            Text(
                text = node.meanings.first(),
                color = sc.textMuted,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

private fun formatTimestamp(seconds: Double): String {
    val total = seconds.toInt().coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
