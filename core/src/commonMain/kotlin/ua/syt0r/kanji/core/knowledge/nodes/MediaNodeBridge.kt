package ua.syt0r.kanji.core.knowledge.nodes

import ua.syt0r.kanji.core.japanese.isKanji
import ua.syt0r.kanji.core.knowledge.media.MediaReference
import ua.syt0r.kanji.core.knowledge.media.MediaReferenceKind

// ============================================================
// MEDIA NODE BRIDGE (ADR-0013 code part, todo #118/#119)
// ------------------------------------------------------------
// Maps REAL media references (recorded by the desktop media
// engine when the user bookmarks / mines / captures subtitle
// text) onto the typed node layer: one series node per media
// title, one subtitle-line node per reference, and typed edges —
// series contains subtitle_line, vocabulary appears_in_media,
// and mined cards mine_from the line. The registries are the
// only vocabulary used; nothing is fabricated.
//
// Honest limits: the reference text is recorded Japanese, so a
// language node id is derived only when the text is exactly one
// kanji (safe) — longer text is left as a line-level node, never
// guessed into a word id.
// ============================================================

/** The typed graph produced from a user's media references. */
data class MediaNodeGraph(
    val nodes: List<Node>,
    val edges: List<NodeEdge>,
    /** References that could not be mapped (blank text etc.). */
    val skipped: Int = 0
) {
    val isEmpty: Boolean get() = nodes.isEmpty() && edges.isEmpty()
}

object MediaNodeBridge {

    /**
     * Builds the typed node graph from the real references. A reference
     * with blank text is skipped (counted honestly). Series nodes are
     * deduplicated by title; the node id is a stable hash of the title
     * (never a fabricated external id).
     */
    fun build(references: List<MediaReference>, nowEpochMs: Long = 0L): MediaNodeGraph {
        val nodes = mutableListOf<Node>()
        val edges = mutableListOf<NodeEdge>()
        val seriesByTitle = mutableMapOf<String, NodeId>()
        var skipped = 0

        references.forEach { ref ->
            if (ref.text.isBlank()) {
                skipped++
                return@forEach
            }
            val title = ref.title.ifBlank { "Untitled media" }
            val seriesId = seriesByTitle.getOrPut(title) {
                NodeId(NodeType.Series, stableId(title))
            }
            if (nodes.none { it.id == seriesId }) {
                nodes += Node(
                    id = seriesId,
                    source = NodeSource.Integration,
                    sourceId = title,
                    createdAtEpochMs = nowEpochMs,
                    updatedAtEpochMs = nowEpochMs,
                    tags = listOf("media", "user")
                )
            }

            // One subtitle-line node per reference.
            val lineId = NodeId(NodeType.SubtitleLine, stableId(ref.title + ref.text + ref.timestampMs))
            if (nodes.none { it.id == lineId }) {
                nodes += Node(
                    id = lineId,
                    source = NodeSource.Integration,
                    sourceId = ref.title,
                    createdAtEpochMs = ref.recordedAt,
                    updatedAtEpochMs = ref.recordedAt,
                    parentId = seriesId,
                    tags = listOf("subtitle", "text")
                )
            }
            // series contains subtitle_line.
            edges += NodeEdge(
                type = RelationshipType.Contains,
                from = seriesId,
                to = lineId,
                createdAtEpochMs = ref.recordedAt
            )

            // Language bridge: when the reference text is exactly one kanji,
            // it is safe to link the line to that kanji via appears_in_media
            // (spec §28, §149). Longer text is never guessed into a word id.
            if (ref.text.length == 1 && ref.text[0].isKanji()) {
                val kanjiId = NodeId(NodeType.Kanji, ref.text)
                if (nodes.none { it.id == kanjiId }) {
                    nodes += Node(
                        id = kanjiId,
                        source = NodeSource.BundledData,
                        createdAtEpochMs = nowEpochMs,
                        updatedAtEpochMs = nowEpochMs,
                        tags = listOf("language")
                    )
                }
                edges += NodeEdge(
                    type = RelationshipType.AppearsInMedia,
                    from = kanjiId,
                    to = lineId,
                    createdAtEpochMs = ref.recordedAt
                )
            }

            // Mined references are tagged on the line node so the provenance
            // is preserved (kind = Mined), and — when the desktop mining
            // engine reported the real card id — a mined_from edge is built
            // from the Card node to the subtitle line (ADR-0013, §149). The
            // card id is never invented: no cardId, no edge, just the tag.
            if (ref.kind == MediaReferenceKind.Mined) {
                val nodeIndex = nodes.indexOfFirst { it.id == lineId }
                if (nodeIndex >= 0) {
                    val node = nodes[nodeIndex]
                    nodes[nodeIndex] = node.copy(tags = node.tags + "mined")
                }
                if (ref.cardId != null) {
                    val cardId = NodeId(NodeType.Card, ref.cardId)
                    if (nodes.none { it.id == cardId }) {
                        nodes += Node(
                            id = cardId,
                            source = NodeSource.User,
                            sourceId = ref.cardId,
                            createdAtEpochMs = ref.recordedAt,
                            updatedAtEpochMs = ref.recordedAt,
                            tags = listOf("mined", "card")
                        )
                    }
                    edges += NodeEdge(
                        type = RelationshipType.MinedFrom,
                        from = cardId,
                        to = lineId,
                        createdAtEpochMs = ref.recordedAt
                    )
                }
            }
        }

        return MediaNodeGraph(nodes = nodes, edges = edges, skipped = skipped)
    }

    /** Stable 64-bit id from a string (FNV-1a) — deterministic across runs. */
    private fun stableId(value: String): String {
        var hash = 0x811C9DC5L
        value.forEach { char ->
            hash = hash xor char.code.toLong()
            hash *= 0x01000193L
        }
        return hash.toString(16)
    }
}
