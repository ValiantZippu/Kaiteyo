package ua.syt0r.kanji.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for [KnowledgeGraph.autoClustered] (spec §9–§10; MASTER item 149):
 * automatic collapse of dense expansions. The root and a pinned/selected
 * node are never hidden; clustering is pure, deterministic and idempotent.
 */
class KnowledgeGraphAutoClusterTest {

    private fun node(id: String) = KnowledgeNode(id = id, kind = KnowledgeNodeKind.Kanji, label = id)

    /** root ⇄ A, and A — B, C, D, E (a dense hub hiding behind root). */
    private fun denseGraph(): KnowledgeGraph {
        val ids = listOf("kanji:R", "kanji:A", "kanji:B", "kanji:C", "kanji:D", "kanji:E")
        val nodes = ids.associateWith { node(it) }
        val edges = listOf(
            KnowledgeEdge("kanji:R", "kanji:A", KnowledgeEdgeType.RelatedTo),
            KnowledgeEdge("kanji:A", "kanji:B", KnowledgeEdgeType.RelatedTo),
            KnowledgeEdge("kanji:A", "kanji:C", KnowledgeEdgeType.RelatedTo),
            KnowledgeEdge("kanji:A", "kanji:D", KnowledgeEdgeType.RelatedTo),
            KnowledgeEdge("kanji:A", "kanji:E", KnowledgeEdgeType.RelatedTo)
        )
        return KnowledgeGraph(rootId = "kanji:R", nodes = nodes, edges = edges)
    }

    @Test
    fun underLimitIsReturnedUnchanged() {
        val graph = denseGraph() // 6 nodes
        val result = graph.autoClustered(maxVisibleNodes = 10)
        assertEquals(6, result.nodeCount)
        assertNotNull(result.node("kanji:B"))
    }

    @Test
    fun collapsesDenseHubAndKeepsRoot() {
        val result = denseGraph().autoClustered(maxVisibleNodes = 3)

        assertEquals(2, result.nodeCount)
        // Root is always protected.
        assertNotNull(result.node("kanji:R"))
        // The dense hub survives and records how many nodes it hides.
        val hub = result.node("kanji:A")
        assertNotNull(hub)
        assertEquals("4", hub.extra[KnowledgeGraph.HIDDEN_COUNT_KEY])
        // Hidden leaves are gone.
        assertNull(result.node("kanji:B"))
        assertNull(result.node("kanji:E"))
    }

    @Test
    fun pinnedNodeIsNeverHidden() {
        val result = denseGraph().autoClustered(maxVisibleNodes = 3, pinnedId = "kanji:B")

        assertEquals(3, result.nodeCount)
        assertNotNull(result.node("kanji:R"))
        assertNotNull(result.node("kanji:A"))
        // The pinned node survives because collapsing the hub keeps it.
        assertNotNull(result.node("kanji:B"))
    }

    @Test
    fun resultIsDeterministicAndIdempotent() {
        val a = denseGraph().autoClustered(3)
        val b = denseGraph().autoClustered(3)
        assertEquals(a.nodeCount, b.nodeCount)
        assertEquals(a.node("kanji:A")?.extra, b.node("kanji:A")?.extra)

        // Clustering an already-clustered graph at the same limit is a no-op.
        assertEquals(a.nodeCount, a.autoClustered(3).nodeCount)
    }

    @Test
    fun degenerateGraphWithProtectedRootOnlyCannotShrink() {
        // Root connected only to leaves: collapsing a leaf would have to hide
        // the root, which is forbidden — so no progress is made and it returns
        // the input unchanged rather than looping forever.
        val nodes = mapOf(
            "kanji:R" to node("kanji:R"),
            "kanji:L1" to node("kanji:L1"),
            "kanji:L2" to node("kanji:L2")
        )
        val graph = KnowledgeGraph(
            rootId = "kanji:R",
            nodes = nodes,
            edges = listOf(
                KnowledgeEdge("kanji:R", "kanji:L1", KnowledgeEdgeType.RelatedTo),
                KnowledgeEdge("kanji:R", "kanji:L2", KnowledgeEdgeType.RelatedTo)
            )
        )
        val result = graph.autoClustered(maxVisibleNodes = 2)
        assertEquals(3, result.nodeCount)
        assertNotNull(result.node("kanji:R"))
    }
}
