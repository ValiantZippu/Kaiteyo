package ua.syt0r.kanji.core.knowledge

import kotlin.math.cos
import kotlin.math.sin

// ============================================================
// GRAPH LAYOUT
// ------------------------------------------------------------
// Deterministic radial layout for the graph canvas. Positions
// are stable across recompositions (they depend only on the
// graph), so pan/zoom never fights the layout and expansion
// grows the graph outward from the root without reflowing
// existing nodes. Pure math — no Compose dependency.
// ============================================================

data class GraphPoint(val x: Float, val y: Float) {
    operator fun plus(other: GraphPoint): GraphPoint = GraphPoint(x + other.x, y + other.y)
}

object GraphLayout {

    /** Approximate node footprint in world units (canvas drawing). */
    const val NodeWidth = 120f
    const val NodeHeight = 44f

    private const val LevelRadius = 200f
    private const val ChildRadius = 170f
    private const val MaxBfsDepth = 3

    /**
     * Positions every visible node. The root sits at the origin; the first
     * ring is spread on a circle; deeper levels branch from their parent's
     * angle so the graph fans outward like a tree.
     */
    fun layout(graph: KnowledgeGraph): Map<String, GraphPoint> {
        val rootId = graph.rootId ?: return emptyMap()
        val positions = mutableMapOf<String, GraphPoint>()
        positions[rootId] = GraphPoint(0f, 0f)

        val placed = mutableSetOf(rootId)
        var frontier = listOf(rootId)

        repeat(MaxBfsDepth) { depth ->
            val nextFrontier = mutableListOf<String>()
            val level = depth + 1
            frontier.forEach { parentId ->
                val parentPos = positions.getValue(parentId)
                val children = graph.neighbors(parentId)
                    .filter { it.id !in placed }
                    .take(8)
                if (children.isEmpty()) return@forEach
                val baseAngle = angleOf(parentId, positions)
                val spread = if (level == 1) 360f else 150f
                val start = baseAngle - spread / 2f
                children.forEachIndexed { index, child ->
                    val angle = start + spread * index / children.size.coerceAtLeast(1)
                    val radius = if (level == 1) LevelRadius else ChildRadius
                    positions[child.id] = parentPos + polar(radius, angle)
                    placed.add(child.id)
                    nextFrontier.add(child.id)
                }
            }
            frontier = nextFrontier
            if (frontier.isEmpty()) return@repeat
        }

        return positions
    }

    private fun angleOf(nodeId: String, positions: Map<String, GraphPoint>): Float {
        val pos = positions[nodeId] ?: return 0f
        if (pos.x == 0f && pos.y == 0f) return 0f
        val angle = kotlin.math.atan2(pos.y, pos.x)
        return (angle * 180f / kotlin.math.PI.toFloat())
    }

    private fun polar(radius: Float, angleDeg: Float): GraphPoint {
        val rad = angleDeg * kotlin.math.PI.toFloat() / 180f
        return GraphPoint(radius * cos(rad), radius * sin(rad))
    }

    /** Midpoints of every edge, for drawing edge labels on the canvas. */
    fun edgeMidpoints(
        edges: List<KnowledgeEdge>,
        positions: Map<String, GraphPoint>
    ): Map<KnowledgeEdge, GraphPoint> =
        edges.mapNotNull { edge ->
            val from = positions[edge.from] ?: return@mapNotNull null
            val to = positions[edge.to] ?: return@mapNotNull null
            edge to GraphPoint((from.x + to.x) / 2f, (from.y + to.y) / 2f)
        }.toMap()
}
