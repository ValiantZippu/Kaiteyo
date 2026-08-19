package ua.syt0r.kanji.core.knowledge

// ============================================================
// KNOWLEDGE GRAPH — MODEL
// ------------------------------------------------------------
// The knowledge graph is a navigation surface, not decoration.
// A node is a kanji / radical / word / sentence / grammar entry;
// an edge is one labeled relationship. The graph is always
// expanded progressively — never thousands of nodes at once.
//
// Nodes are identified by stable, type-prefixed ids so a node
// can be resolved back to a dictionary entry:
//   "kanji:食"   "radical:口"   "word:12345"
//   "sentence:<index>"         "grammar:te-miru"
// ============================================================

enum class KnowledgeNodeKind(val label: String) {
    Kanji("Kanji"),
    Radical("Radical"),
    Word("Word"),
    Sentence("Sentence"),
    Grammar("Grammar"),
    /** A user-media occurrence (bookmark with Japanese text, spec §28). */
    Media("Media")
}

/** Edge semantics — what the relationship between two nodes means. */
enum class KnowledgeEdgeType(val label: String) {
    /** word → kanji: the word's spelling contains the character. */
    Contains("contains"),
    /** kanji → component/radical: the character is built from it. */
    ComponentOf("component of"),
    /** radical → kanji: the character uses this radical. */
    RadicalOf("radical of"),
    /** kanji → word: the word uses the character. */
    UsedIn("used in"),
    /** word → sentence: the sentence contains the word's reading. */
    AppearsIn("appears in"),
    /** sentence → grammar: the pattern is present in the text. */
    ExampleOf("example of"),
    /** kanji → kanji: share a radical (related by structure). */
    RelatedTo("related to")
}

data class KnowledgeNode(
    val id: String,
    val kind: KnowledgeNodeKind,
    val label: String,
    /** Secondary line (reading / meaning / gloss preview). */
    val subtitle: String? = null,
    /** Extra typed metadata surfaced in inspectors, never the source of truth. */
    val extra: Map<String, String> = emptyMap()
) {
    companion object {
        fun kanji(character: String): String = "kanji:$character"
        fun radical(radical: String): String = "radical:$radical"
        fun word(id: Long): String = "word:$id"
        fun sentence(index: Long): String = "sentence:$index"
        fun grammar(id: String): String = "grammar:$id"
        /** Stable media-reference node id (title + timestamp + text). */
        fun media(title: String, timestampMs: Long, text: String): String =
            "media:${title.hashCode().toLong() and 0x7fffffffL}:$timestampMs:${text.hashCode().toLong() and 0x7fffffffL}"
    }
}

data class KnowledgeEdge(
    val from: String,
    val to: String,
    val type: KnowledgeEdgeType
)

/**
 * An immutable graph snapshot. [rootId] is the entry point the graph was
 * opened from. [nodes] and [edges] only ever contain expanded nodes — the
 * graph never materializes the whole dictionary.
 */
data class KnowledgeGraph(
    val rootId: String? = null,
    val nodes: Map<String, KnowledgeNode> = emptyMap(),
    val edges: List<KnowledgeEdge> = emptyList()
) {
    val nodeCount: Int get() = nodes.size
    val edgeCount: Int get() = edges.size
    val isEmpty: Boolean get() = nodes.isEmpty() && edges.isEmpty()

    fun node(id: String): KnowledgeNode? = nodes[id]

    fun neighbors(id: String, types: Set<KnowledgeEdgeType>? = null): List<KnowledgeNode> {
        val wanted = types
        return edges
            .filter { (from, to, type) ->
                (from == id || to == id) && (wanted == null || type in wanted)
            }
            .mapNotNull { edge -> nodes[if (edge.from == id) edge.to else edge.from] }
            .distinct()
    }

    fun edgesBetween(from: String, to: String): List<KnowledgeEdge> =
        edges.filter { (a, b) -> (a == from && b == to) || (a == to && b == from) }

    /**
     * Counts incident edges of the node by relationship type — the shape of
     * the node's connections at a glance (spec §10: relationship filters and
     * contextual labels). Order follows [KnowledgeEdgeType.entries].
     */
    fun edgeTypeCounts(id: String): Map<KnowledgeEdgeType, Int> {
        val counts = KnowledgeEdgeType.entries.associateWith { 0 }.toMutableMap()
        edges.forEach { (from, to, type) ->
            if (from == id || to == id) counts[type] = (counts[type] ?: 0) + 1
        }
        return counts.filterValues { it > 0 }
    }

    /** Adds a batch of nodes/edges, deduplicating against what exists. */
    fun merged(additionalNodes: List<KnowledgeNode>, additionalEdges: List<KnowledgeEdge>): KnowledgeGraph {
        val mergedNodes = nodes.toMutableMap()
        additionalNodes.forEach { mergedNodes.putIfAbsent(it.id, it) }
        val existing = edges.toMutableSet()
        val mergedEdges = existing.toMutableList()
        additionalEdges.forEach { if (existing.add(it)) mergedEdges.add(it) }
        return copy(nodes = mergedNodes, edges = mergedEdges)
    }

    /**
     * Removes the neighbors of every [collapsedIds] node (branch collapse /
     * clustering, spec §9–§10): a collapsed node keeps its own label but its
     * incident edges disappear and a real "+N hidden" count is recorded in
     * its `extra` map, so the graph reads as one cluster instead of a tangle.
     *
     * The root node and [pinnedId] (usually the selected node) are never
     * hidden — collapsing is a view operation and must never strand the
     * node the user is inspecting. Pure and deterministic (unit-tested).
     */
    fun collapsed(collapsedIds: Set<String>, pinnedId: String? = null): KnowledgeGraph {
        if (collapsedIds.isEmpty()) return this
        val collapsedNodes = mutableMapOf<String, KnowledgeNode>()
        val removed = mutableSetOf<String>()

        collapsedIds.forEach { collapsedId ->
            val node = nodes[collapsedId] ?: return@forEach
            val neighbors = neighbors(collapsedId).filter { it.id != pinnedId && it.id != rootId }
            collapsedNodes[collapsedId] = node.copy(
                extra = node.extra + (HIDDEN_COUNT_KEY to neighbors.size.toString())
            )
            removed += neighbors.map { it.id }
        }

        if (removed.isEmpty() && collapsedNodes.isEmpty()) return this

        val surviving = nodes.filterKeys { it !in removed }
            .mapValues { (id, n) -> collapsedNodes[id] ?: n }
        val survivingEdges = edges.filter { edge ->
            edge.from !in removed && edge.to !in removed
        }
        return copy(nodes = surviving, edges = survivingEdges)
    }

    /**
     * Auto-clusters a dense expansion (spec §9–§10; MASTER item 149): greedily
     * collapses the non-protected node hiding the most neighbors until the
     * visible graph is at most [maxVisibleNodes] nodes. The root and [pinnedId]
     * are never hidden (the same protection rule as [collapsed]). Deterministic,
     * idempotent once under the limit, and safe to call repeatedly — a pure
     * view operation on the graph snapshot, never a change to the underlying data.
     */
    fun autoClustered(maxVisibleNodes: Int, pinnedId: String? = null): KnowledgeGraph {
        if (maxVisibleNodes < 1 || nodeCount <= maxVisibleNodes) return this
        val protected = setOfNotNull(rootId, pinnedId)
        var current = this
        while (current.nodeCount > maxVisibleNodes) {
            val best = current.nodes.keys
                .filter { it !in protected }
                .maxByOrNull { current.neighbors(it).size }
                ?: break
            val next = current.collapsed(setOf(best), pinnedId)
            if (next.nodeCount >= current.nodeCount) break // no-progress guard
            current = next
        }
        return current
    }

    companion object {
        /** `extra` key recording how many nodes a collapsed node hides. */
        const val HIDDEN_COUNT_KEY: String = "hiddenCount"
    }
}

/** Result of expanding one node. */
data class GraphExpansion(
    val graph: KnowledgeGraph,
    val addedNodes: List<KnowledgeNode>,
    val addedEdges: List<KnowledgeEdge>,
    /** Nodes that could not be expanded further (leaves of the visible graph). */
    val exhausted: List<String> = emptyList()
)
