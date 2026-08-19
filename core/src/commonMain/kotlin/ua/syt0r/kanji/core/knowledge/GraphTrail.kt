package ua.syt0r.kanji.core.knowledge

// ============================================================
// GRAPH TRAIL — breadcrumbs + back/forward (KT-GRAPH-004, spec §75)
// ------------------------------------------------------------
// The knowledge graph is a navigation surface, so it needs a trail:
// the path of nodes the user has moved through, breadcrumb-able and
// reversible. This model is pure and deterministic (unit-tested):
// it records the sequence of focused/expanded nodes from the root,
// and supports back/forward without ever mutating the graph itself.
//
// Semantics (standard history model):
//   - [push] moves to a node, appending it to the history and
//     discarding any forward entries (navigating to a new node
//     invalidates the redo path).
//   - [back] / [forward] move the position through the history and
//     return the node id to focus; they are no-ops at the edges.
//   - [breadcrumbs] returns the trail from the root to the current
//     position (the focused node is always the last breadcrumb).
// ============================================================

/**
 * The navigation trail through a knowledge graph.
 *
 * [entries] is the full history of visited node ids (oldest first);
 * [position] indexes the currently focused entry. Forward entries
 * are the ids after [position] (kept only until the next [push]).
 */
data class GraphTrail(
    val entries: List<String> = emptyList(),
    val position: Int = entries.lastIndex
) {
    /** The currently focused node id (null when the trail is empty). */
    val current: String? get() = entries.getOrNull(position)

    val size: Int get() = entries.size
    val isEmpty: Boolean get() = entries.isEmpty()

    val canGoBack: Boolean get() = position > 0
    val canGoForward: Boolean get() = position < entries.lastIndex

    /** The root of the trail (oldest entry), when any. */
    val root: String? get() = entries.firstOrNull()

    /** Trail from the root through the current position, for breadcrumbs. */
    fun breadcrumbs(): List<String> = entries.take(position + 1)

    /**
     * Moves to [nodeId], appending it to the history. Any forward
     * entries beyond the current position are discarded (navigating
     * to a new node invalidates the redo path). A repeated consecutive
     * id is a no-op (expanding the focused node again).
     */
    fun push(nodeId: String): GraphTrail {
        if (current == nodeId && position == entries.lastIndex) return this
        val truncated = entries.take(position + 1)
        return GraphTrail(
            entries = truncated + nodeId,
            position = truncated.size
        )
    }

    /** Moves back one step. Returns the node id to focus, or null. */
    fun back(): Pair<GraphTrail, String?> {
        if (!canGoBack) return this to null
        return copy(position = position - 1) to entries[position - 1]
    }

    /** Moves forward one step. Returns the node id to focus, or null. */
    fun forward(): Pair<GraphTrail, String?> {
        if (!canGoForward) return this to null
        return copy(position = position + 1) to entries[position + 1]
    }

    companion object {
        val Empty = GraphTrail()
    }
}
