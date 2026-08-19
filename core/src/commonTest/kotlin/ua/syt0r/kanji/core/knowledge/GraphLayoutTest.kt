package ua.syt0r.kanji.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GraphLayoutTest {

    private fun node(id: String) = KnowledgeNode(id, KnowledgeNodeKind.Kanji, id)

    private fun graphWith(root: String, edges: List<Pair<String, String>>): KnowledgeGraph {
        val ids = (edges.flatMap { listOf(it.first, it.second) } + root).distinct()
        return KnowledgeGraph(
            rootId = root,
            nodes = ids.associateWith { node(it) },
            edges = edges.map { (a, b) -> KnowledgeEdge(a, b, KnowledgeEdgeType.RelatedTo) }
        )
    }

    @Test
    fun emptyGraphHasNoPositions() {
        assertEquals(emptyMap(), GraphLayout.layout(KnowledgeGraph()))
    }

    @Test
    fun rootSitsAtOrigin() {
        val graph = graphWith("食", emptyList())
        val positions = GraphLayout.layout(graph)
        assertEquals(GraphPoint(0f, 0f), positions["食"])
    }

    @Test
    fun allNodesGetPositions() {
        val graph = graphWith("食", listOf("食" to "口", "食" to "食べる"))
        val positions = GraphLayout.layout(graph)
        assertEquals(3, positions.size)
        assertTrue("口" in positions)
        assertTrue("食べる" in positions)
    }

    @Test
    fun layoutIsDeterministic() {
        val graph = graphWith("食", listOf("食" to "口", "食" to "食べる", "口" to "喝"))
        val first = GraphLayout.layout(graph)
        val second = GraphLayout.layout(graph)
        assertEquals(first, second)
    }

    @Test
    fun levelOneIsSpreadOnCircle() {
        val graph = graphWith("食", listOf("食" to "A", "食" to "B", "食" to "C", "食" to "D"))
        val positions = GraphLayout.layout(graph)
        val radiuses = listOf("A", "B", "C", "D").map { id ->
            val p = positions.getValue(id)
            kotlin.math.sqrt(p.x * p.x + p.y * p.y)
        }
        // All first-ring nodes sit at the same radius (within tolerance).
        radiuses.zipWithNext().forEach { (a, b) -> assertTrue(kotlin.math.abs(a - b) < 1f) }
        // ... and roughly at LevelRadius.
        radiuses.forEach { assertTrue(kotlin.math.abs(it - 200f) < 2f) }
    }

    @Test
    fun edgeMidpointsOnlyForPlacedNodes() {
        // The chain is deeper than the layout depth (3), so the last node is
        // never placed — its edges must be skipped by edgeMidpoints.
        val graph = graphWith(
            "食",
            listOf("食" to "口", "口" to "A", "A" to "B", "B" to "deep")
        )
        val positions = GraphLayout.layout(graph)
        val midpoints = GraphLayout.edgeMidpoints(graph.edges, positions)
        // 3 placed edges; the edge to "deep" is skipped.
        assertEquals(3, midpoints.size)
        assertTrue("deep" !in positions)
    }
}
