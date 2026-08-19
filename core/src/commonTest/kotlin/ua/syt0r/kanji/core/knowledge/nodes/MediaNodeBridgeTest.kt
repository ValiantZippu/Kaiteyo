package ua.syt0r.kanji.core.knowledge.nodes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ua.syt0r.kanji.core.knowledge.media.MediaReference
import ua.syt0r.kanji.core.knowledge.media.MediaReferenceKind

class MediaNodeBridgeTest {

    private fun ref(
        text: String,
        title: String = "S1E1.mkv",
        kind: MediaReferenceKind = MediaReferenceKind.Subtitle,
        timestampMs: Long = 1000,
        cardId: String? = null
    ) = MediaReference(
        kind = kind,
        title = title,
        text = text,
        timestampMs = timestampMs,
        recordedAt = timestampMs,
        cardId = cardId
    )

    @Test
    fun singleReferenceBuildsSeriesAndLine() {
        val graph = MediaNodeBridge.build(listOf(ref("駅")))
        assertEquals(2, graph.nodes.size) // series + subtitle line
        assertTrue(graph.nodes.any { it.id.type == NodeType.Series })
        assertTrue(graph.nodes.any { it.id.type == NodeType.SubtitleLine })
        assertEquals(1, graph.edges.size)
        assertEquals(RelationshipType.Contains, graph.edges.first().type)
        assertTrue(graph.edges.first().isValid)
    }

    @Test
    fun kanjiTextLinksLineToKanji() {
        val graph = MediaNodeBridge.build(listOf(ref("食")))
        assertTrue(graph.nodes.any { it.id.type == NodeType.Kanji && it.id.ref == "食" })
        val appears = graph.edges.filter { it.type == RelationshipType.AppearsInMedia }
        assertEquals(1, appears.size)
        assertEquals(NodeType.Kanji, appears.first().from.type)
        assertEquals(NodeType.SubtitleLine, appears.first().to.type)
        // The bridge lint must accept appears_in_media (spec §149).
        assertFalse(appears.first().crossesBridgeIllegally())
    }

    @Test
    fun longTextNeverGuessedIntoWordNode() {
        val graph = MediaNodeBridge.build(listOf(ref("駅前で待っています。")))
        // No language node is fabricated for multi-char text.
        assertFalse(graph.nodes.any { it.id.type == NodeType.Vocabulary })
        assertFalse(graph.nodes.any { it.id.type == NodeType.Kanji })
    }

    @Test
    fun sameSeriesDeduplicatesSeriesNode() {
        val graph = MediaNodeBridge.build(
            listOf(ref("駅"), ref("電車", timestampMs = 2000))
        )
        assertEquals(1, graph.nodes.count { it.id.type == NodeType.Series })
        assertEquals(2, graph.nodes.count { it.id.type == NodeType.SubtitleLine })
        assertEquals(2, graph.edges.count { it.type == RelationshipType.Contains })
    }

    @Test
    fun minedReferenceTaggedOnLine() {
        val graph = MediaNodeBridge.build(listOf(ref("食", kind = MediaReferenceKind.Mined)))
        val line = graph.nodes.first { it.id.type == NodeType.SubtitleLine }
        assertTrue("mined" in line.tags)
        // No fabricated mined_from edge without a real card id.
        assertFalse(graph.edges.any { it.type == RelationshipType.MinedFrom })
    }

    @Test
    fun minedReferenceWithRealCardIdBuildsMinedFromEdge() {
        val graph = MediaNodeBridge.build(
            listOf(ref("食", kind = MediaReferenceKind.Mined, cardId = "card_123"))
        )
        val card = graph.nodes.first { it.id.type == NodeType.Card && it.id.ref == "card_123" }
        assertEquals(NodeSource.User, card.source)
        val edge = graph.edges.first { it.type == RelationshipType.MinedFrom }
        assertEquals(NodeType.Card, edge.from.type)
        assertEquals(NodeType.SubtitleLine, edge.to.type)
        assertTrue(edge.isValid)
        // The bridge lint accepts mined_from (spec §149).
        assertFalse(edge.crossesBridgeIllegally())
    }

    @Test
    fun minedCardIdIsNeverInvented() {
        // A Mined reference WITHOUT a card id must not fabricate a Card node.
        val graph = MediaNodeBridge.build(listOf(ref("食", kind = MediaReferenceKind.Mined)))
        assertFalse(graph.nodes.any { it.id.type == NodeType.Card })
        assertFalse(graph.edges.any { it.type == RelationshipType.MinedFrom })
    }

    @Test
    fun blankTextSkippedHonestly() {
        val graph = MediaNodeBridge.build(listOf(ref("   ")))
        assertEquals(1, graph.skipped)
        assertTrue(graph.isEmpty)
    }

    @Test
    fun nodeIdsAreDeterministic() {
        val first = MediaNodeBridge.build(listOf(ref("駅")))
        val second = MediaNodeBridge.build(listOf(ref("駅")))
        assertEquals(first.nodes, second.nodes)
        assertEquals(first.edges, second.edges)
    }
}
