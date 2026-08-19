package ua.syt0r.kanji.core.knowledge

// ============================================================
// NODE TRAVERSAL (KT-DICT-003, NODE §81)
// ------------------------------------------------------------
// "Node-anchored dictionary traversal" — the typed walk from one
// dictionary entry to its neighbors and beyond:
//
//     食べる ──Contains──▶ 食 ──UsedIn──▶ 食事
//
// A traversal is a sequence of one-hop expansions, each returning
// typed hops (node + edge type). The engine is the same
// KnowledgeGraphRepository the graph canvas uses — traversal
// chips and the graph never disagree about what connects to what.
//
// Seed resolution is honest: kanji / word / radical / grammar ids
// resolve to real entries. Sentence ids are content hashes (the
// graph's stable id), which cannot be reversed to text, so a
// sentence can only be expanded when the caller holds the node
// itself (e.g. from the sentence screen) — [hops] accepts a
// pre-built seed node for that case.
// ============================================================

/**
 * One typed step in a traversal: the neighbor node reached from the
 * seed through [edgeType].
 */
data class TraversalHop(
    val node: KnowledgeNode,
    val edgeType: KnowledgeEdgeType
)

/**
 * Resolves a stable node id to a single-node graph seed, or null when
 * the id cannot be mapped back to a real entry.
 */
class NodeTraversal(
    private val knowledge: KnowledgeRepository,
    private val graphRepository: KnowledgeGraphRepository
) {

    /**
     * Resolves [nodeId] to a seed [KnowledgeNode] using real lookups.
     * Sentence ids are content hashes and are not resolvable (see
     * file header) — expand a sentence via [hops] with its node.
     */
    suspend fun resolve(nodeId: String): KnowledgeNode? {
        val (kind, payload) = nodeId.split(':', limit = 2).let {
            if (it.size != 2) return null
            it[0] to it[1]
        }
        return when (kind) {
            "kanji" -> knowledge.kanji(payload)?.toNode()
            "word" -> payload.toLongOrNull()?.let { knowledge.word(it)?.toNode() }
            "radical" -> {
                val radical = knowledge.allRadicals().firstOrNull { it.radical == payload }
                    ?: return null
                KnowledgeNode(
                    id = nodeId,
                    kind = KnowledgeNodeKind.Radical,
                    label = radical.radical,
                    subtitle = "${radical.strokeCount} strokes"
                )
            }
            "grammar" -> GrammarCatalog.byId(payload)?.let { pattern ->
                KnowledgeNode(
                    id = nodeId,
                    kind = KnowledgeNodeKind.Grammar,
                    label = pattern.pattern,
                    subtitle = pattern.meaning,
                    extra = mapOf(
                        "jlpt" to (pattern.jlpt?.toString() ?: ""),
                        "meaning" to pattern.meaning
                    )
                )
            }
            else -> null
        }
    }

    /**
     * One hop from [nodeId] (or a pre-built [seed]) — the traversal-chip
     * set: every direct neighbor with its edge type. [wantedTypes]
     * filters which relationship kinds to pull (relationship filters,
     * spec §10); [limit] caps each kind's contribution.
     */
    suspend fun hops(
        nodeId: String? = null,
        seed: KnowledgeNode? = null,
        wantedTypes: Set<KnowledgeEdgeType>? = null,
        limit: Int = 10
    ): List<TraversalHop> {
        val node = seed ?: nodeId?.let { resolve(it) } ?: return emptyList()
        val seedGraph = KnowledgeGraph(
            rootId = node.id,
            nodes = mapOf(node.id to node)
        )
        val expansion = graphRepository.expand(
            graph = seedGraph,
            nodeId = node.id,
            wantedTypes = wantedTypes,
            hopLimit = limit
        )
        return expansion.addedEdges.mapNotNull { edge ->
            val neighborId = if (edge.from == node.id) edge.to else edge.from
            expansion.graph.node(neighborId)?.let { TraversalHop(it, edge.type) }
        }.distinctBy { it.node.id to it.edgeType }
    }

    /**
     * A multi-hop walk: starting at [nodeId], follow each relationship
     * kind in [edgeSequence] in order. Returns the frontier after each
     * hop — the 食べる → 食 → 食事 chain is
     * `walk("word:…", listOf(Contains, UsedIn))`.
     */
    suspend fun walk(
        nodeId: String,
        edgeSequence: List<KnowledgeEdgeType>,
        limit: Int = 6
    ): List<List<TraversalHop>> {
        if (edgeSequence.isEmpty()) return emptyList()

        val result = mutableListOf<List<TraversalHop>>()
        var frontier = hops(nodeId = nodeId, wantedTypes = setOf(edgeSequence.first()), limit = limit)
        if (frontier.isEmpty()) return result
        result.add(frontier)

        edgeSequence.drop(1).forEach { edgeType ->
            val next = frontier.flatMap { hop ->
                hops(nodeId = hop.node.id, wantedTypes = setOf(edgeType), limit = limit)
            }.distinctBy { it.node.id to it.edgeType }
            if (next.isEmpty()) return result
            result.add(next)
            frontier = next
        }
        return result
    }
}
