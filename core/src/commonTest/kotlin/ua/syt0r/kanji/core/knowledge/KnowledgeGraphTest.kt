package ua.syt0r.kanji.core.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KnowledgeGraphTest {

    private val root = KnowledgeNode(
        id = KnowledgeNode.kanji("食"),
        kind = KnowledgeNodeKind.Kanji,
        label = "食",
        subtitle = "eat"
    )

    private val radicalNode = KnowledgeNode(
        id = KnowledgeNode.radical("口"),
        kind = KnowledgeNodeKind.Radical,
        label = "口"
    )

    private val wordNode = KnowledgeNode(
        id = KnowledgeNode.word(1234),
        kind = KnowledgeNodeKind.Word,
        label = "食べる",
        subtitle = "to eat"
    )

    @Test
    fun initialGraphContainsRootAndEdges() {
        val graph = KnowledgeGraph(
            rootId = root.id,
            nodes = mapOf(root.id to root, radicalNode.id to radicalNode),
            edges = listOf(
                KnowledgeEdge(root.id, radicalNode.id, KnowledgeEdgeType.ComponentOf),
                KnowledgeEdge(root.id, wordNode.id, KnowledgeEdgeType.UsedIn)
            )
        )
        assertEquals(2, graph.nodeCount)
        assertEquals(2, graph.edgeCount)
        assertEquals(root.id, graph.rootId)
    }

    @Test
    fun mergeDeduplicatesNodesAndEdges() {
        val graph = KnowledgeGraph(
            rootId = root.id,
            nodes = mapOf(root.id to root),
            edges = emptyList()
        )
        val merged = graph.merged(
            additionalNodes = listOf(radicalNode, radicalNode, wordNode),
            additionalEdges = listOf(
                KnowledgeEdge(root.id, radicalNode.id, KnowledgeEdgeType.ComponentOf),
                KnowledgeEdge(root.id, radicalNode.id, KnowledgeEdgeType.ComponentOf)
            )
        )
        assertEquals(3, merged.nodeCount)
        assertEquals(1, merged.edgeCount)
    }

    @Test
    fun neighborsReturnsConnectedNodesFilteredByType() {
        val graph = KnowledgeGraph(
            rootId = root.id,
            nodes = mapOf(
                root.id to root,
                radicalNode.id to radicalNode,
                wordNode.id to wordNode
            ),
            edges = listOf(
                KnowledgeEdge(root.id, radicalNode.id, KnowledgeEdgeType.ComponentOf),
                KnowledgeEdge(root.id, wordNode.id, KnowledgeEdgeType.UsedIn)
            )
        )
        assertEquals(setOf(radicalNode.id, wordNode.id), graph.neighbors(root.id).map { it.id }.toSet())
        assertEquals(
            listOf(wordNode.id),
            graph.neighbors(root.id, types = setOf(KnowledgeEdgeType.UsedIn)).map { it.id }
        )
    }

    @Test
    fun nodeIdConventionsAreStable() {
        assertEquals("kanji:食", KnowledgeNode.kanji("食"))
        assertEquals("radical:口", KnowledgeNode.radical("口"))
        assertEquals("word:42", KnowledgeNode.word(42))
        assertEquals("sentence:7", KnowledgeNode.sentence(7))
        assertEquals("grammar:te-miru", KnowledgeNode.grammar("te-miru"))
    }

    @Test
    fun expansionTracksAddedNodes() {
        val graph = KnowledgeGraph(rootId = root.id, nodes = mapOf(root.id to root))
        val (newNodes, newEdges) = listOf(radicalNode) to listOf(
            KnowledgeEdge(root.id, radicalNode.id, KnowledgeEdgeType.ComponentOf)
        )
        val expansion = GraphExpansion(
            graph = graph.merged(newNodes, newEdges),
            addedNodes = newNodes,
            addedEdges = newEdges
        )
        assertEquals(1, expansion.addedNodes.size)
        assertEquals(1, expansion.addedEdges.size)
        assertTrue(expansion.graph.node(radicalNode.id) != null)
    }

    @Test
    fun emptyGraphHasNoRoot() {
        val graph = KnowledgeGraph()
        assertEquals(null, graph.rootId)
        assertTrue(graph.isEmpty())
    }

    // ---------------------------------------------------------------
    // Branch collapse / clustering (spec §9–§10, KT-GRAPH-002)
    // ---------------------------------------------------------------

    @Test
    fun collapseHidesNeighborsAndRecordsHiddenCount() {
        val sentence = KnowledgeNode(KnowledgeNode.sentence(1), KnowledgeNodeKind.Sentence, "今日は食べる")
        val grammar = KnowledgeNode(KnowledgeNode.grammar("te-miru"), KnowledgeNodeKind.Grammar, "〜てみる")
        val graph = KnowledgeGraph(
            rootId = root.id,
            nodes = mapOf(
                root.id to root,
                wordNode.id to wordNode,
                sentence.id to sentence,
                grammar.id to grammar
            ),
            edges = listOf(
                KnowledgeEdge(root.id, wordNode.id, KnowledgeEdgeType.UsedIn),
                KnowledgeEdge(wordNode.id, sentence.id, KnowledgeEdgeType.AppearsIn),
                KnowledgeEdge(sentence.id, grammar.id, KnowledgeEdgeType.ExampleOf)
            )
        )

        // Collapse the word branch: the sentence + grammar nodes disappear,
        // the word node itself stays and records how many it hid.
        val collapsed = graph.collapsed(setOf(wordNode.id))
        assertEquals(setOf(root.id, wordNode.id), collapsed.nodes.keys)
        assertEquals(1, collapsed.edgeCount)
        assertEquals(
            "2",
            collapsed.node(wordNode.id)?.extra?.get(KnowledgeGraph.HIDDEN_COUNT_KEY)
        )
    }

    @Test
    fun collapsePinsSelectedAndRootNodes() {
        val sentence = KnowledgeNode(KnowledgeNode.sentence(1), KnowledgeNodeKind.Sentence, "今日は食べる")
        val graph = KnowledgeGraph(
            rootId = root.id,
            nodes = mapOf(root.id to root, wordNode.id to wordNode, sentence.id to sentence),
            edges = listOf(
                KnowledgeEdge(root.id, wordNode.id, KnowledgeEdgeType.UsedIn),
                KnowledgeEdge(wordNode.id, sentence.id, KnowledgeEdgeType.AppearsIn)
            )
        )
        // Collapsing the root keeps the root visible (it is the entry point);
        // collapsing the word keeps the selected word visible (never strand
        // the node the user is inspecting).
        val collapsedRoot = graph.collapsed(setOf(root.id))
        assertTrue(collapsedRoot.node(root.id) != null)

        val collapsedWord = graph.collapsed(setOf(wordNode.id), pinnedId = wordNode.id)
        assertTrue(collapsedWord.node(wordNode.id) != null)
        // The sentence is still a neighbor of the pinned node → kept.
        assertTrue(collapsedWord.node(sentence.id) != null)
    }

    // ---------------------------------------------------------------
    // Edge-type breakdown (spec §10: relationship filters / context)
    // ---------------------------------------------------------------

    @Test
    fun edgeTypeCountsGroupsIncidentEdgesByType() {
        val sentence = KnowledgeNode(KnowledgeNode.sentence(1), KnowledgeNodeKind.Sentence, "今日は食べる")
        val grammar = KnowledgeNode(KnowledgeNode.grammar("te-miru"), KnowledgeNodeKind.Grammar, "〜てみる")
        val graph = KnowledgeGraph(
            rootId = root.id,
            nodes = mapOf(
                root.id to root,
                radicalNode.id to radicalNode,
                wordNode.id to wordNode,
                sentence.id to sentence,
                grammar.id to grammar
            ),
            edges = listOf(
                KnowledgeEdge(root.id, radicalNode.id, KnowledgeEdgeType.ComponentOf),
                KnowledgeEdge(root.id, wordNode.id, KnowledgeEdgeType.UsedIn),
                KnowledgeEdge(wordNode.id, sentence.id, KnowledgeEdgeType.AppearsIn),
                KnowledgeEdge(sentence.id, grammar.id, KnowledgeEdgeType.ExampleOf)
            )
        )
        val rootCounts = graph.edgeTypeCounts(root.id)
        assertEquals(1, rootCounts[KnowledgeEdgeType.ComponentOf])
        assertEquals(1, rootCounts[KnowledgeEdgeType.UsedIn])
        assertEquals(2, rootCounts.size)

        // The word node is the hub: used-in (to the kanji) + appears-in (to the sentence).
        val wordCounts = graph.edgeTypeCounts(wordNode.id)
        assertEquals(1, wordCounts[KnowledgeEdgeType.UsedIn])
        assertEquals(1, wordCounts[KnowledgeEdgeType.AppearsIn])
    }

    @Test
    fun edgeTypeCountsIsEmptyForIsolatedNode() {
        val graph = KnowledgeGraph(
            rootId = root.id,
            nodes = mapOf(root.id to root)
        )
        assertTrue(graph.edgeTypeCounts(root.id).isEmpty())
        // Unknown ids are never an error.
        assertTrue(graph.edgeTypeCounts("does-not-exist").isEmpty())
    }

    @Test
    fun emptyCollapseSetIsIdentity() {
        val graph = KnowledgeGraph(
            rootId = root.id,
            nodes = mapOf(root.id to root, wordNode.id to wordNode),
            edges = listOf(KnowledgeEdge(root.id, wordNode.id, KnowledgeEdgeType.UsedIn))
        )
        val same = graph.collapsed(emptySet())
        assertEquals(graph, same)
    }
}
