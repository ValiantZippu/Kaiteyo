package ua.syt0r.kanji.core.knowledge.nodes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NodeRegistryTest {

    // ---------------------------------------------------------
    // NodeType registry
    // ---------------------------------------------------------

    @Test
    fun everyTypeHasAUniqueIdAndFamily() {
        val ids = NodeType.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "nodeType ids must be unique")
        NodeType.entries.forEach { type ->
            assertTrue(type.family in NodeFamily.entries, "${type.id} family must be registered")
        }
    }

    @Test
    fun byIdResolvesRegisteredTypesAndRejectsUnknown() {
        assertEquals(NodeType.Kanji, NodeType.byId("kanji"))
        assertEquals(NodeType.Sentence, NodeType.byId("sentence"))
        assertNull(NodeType.byId("no-such-node"))
    }

    @Test
    fun familiesContainTheirDocumentedTypes() {
        val language = NodeType.ofFamily(NodeFamily.Language).map { it.id }
        assertTrue("kanji" in language)
        assertTrue("vocabulary" in language)
        assertTrue("sentence" in language)

        val world = NodeType.ofFamily(NodeFamily.World).map { it.id }
        assertTrue("city" in world)
        assertTrue("station" in world)
        assertTrue("restaurant" in world)
    }

    @Test
    fun bridgeRelevantTypesAreRegistered() {
        // The two-graph bridge (§149) depends on these existing.
        listOf("represents", "encountered_by", "discovered_by", "mined_from",
            "appears_in_media", "teaches").forEach { id ->
            assertNotNull(RelationshipType.byId(id), "bridge edge '$id' must be registered")
        }
    }

    // ---------------------------------------------------------
    // NodeId
    // ---------------------------------------------------------

    @Test
    fun nodeIdRoundTripsThroughAsString() {
        val id = NodeId(NodeType.Kanji, "食")
        assertEquals("kanji:食", id.asString)
        val parsed = NodeId.parse("kanji:食")
        assertEquals(id, parsed)
    }

    @Test
    fun nodeIdParseRejectsUnknownTypeAndMalformed() {
        assertNull(NodeId.parse("nonsense:食"))
        assertNull(NodeId.parse("kanji"))
        assertNull(NodeId.parse(":食"))
        assertNull(NodeId.parse("kanji:"))
    }

    // ---------------------------------------------------------
    // Edge validation (§79–§80)
    // ---------------------------------------------------------

    @Test
    fun typedEdgeValidWhenBothEndsAllowed() {
        val edge = NodeEdge(
            type = RelationshipType.ContainsCharacter,
            from = NodeId(NodeType.Vocabulary, "食事"),
            to = NodeId(NodeType.Kanji, "食")
        )
        assertTrue(edge.isValid)
        assertEquals(emptyList(), edge.validate())
    }

    @Test
    fun typedEdgeRejectsWrongEndTypes() {
        // contains_character is vocabulary → kanji only; kanji → vocabulary
        // is backwards and sentence is not a valid source.
        val backwards = NodeEdge(
            type = RelationshipType.ContainsCharacter,
            from = NodeId(NodeType.Kanji, "食"),
            to = NodeId(NodeType.Vocabulary, "食事")
        )
        assertFalse(backwards.isValid)
        assertTrue(backwards.validate().isNotEmpty())

        val wrongSource = NodeEdge(
            type = RelationshipType.ContainsCharacter,
            from = NodeId(NodeType.Sentence, "s"),
            to = NodeId(NodeType.Kanji, "食")
        )
        assertFalse(wrongSource.isValid)
    }

    @Test
    fun usesRadicalIsDirected() {
        val kanjiToRadical = NodeEdge(
            type = RelationshipType.UsesRadical,
            from = NodeId(NodeType.Kanji, "駅"),
            to = NodeId(NodeType.Radical, "馬")
        )
        assertTrue(kanjiToRadical.isValid)

        val reversed = NodeEdge(
            type = RelationshipType.UsesRadical,
            from = NodeId(NodeType.Radical, "馬"),
            to = NodeId(NodeType.Kanji, "駅")
        )
        assertFalse(reversed.isValid)
    }

    @Test
    fun escapeHatchIsRegisteredButMarked() {
        val related = RelationshipType.byId("related_to")
        assertNotNull(related)
        assertTrue(related.isEscapeHatch)
        // The escape hatch accepts anything.
        val anyEdge = NodeEdge(
            type = related,
            from = NodeId(NodeType.Kanji, "食"),
            to = NodeId(NodeType.Restaurant, "r1")
        )
        assertTrue(anyEdge.isValid)
    }

    @Test
    fun symmetricTypesRoundTrip() {
        assertTrue(RelationshipType.SynonymOf.symmetric)
        assertEquals(RelationshipType.SynonymOf, RelationshipType.reverseOf(RelationshipType.SynonymOf))
        // Directed edges have no reverse registered.
        assertNull(RelationshipType.reverseOf(RelationshipType.UsesRadical))
    }

    // ---------------------------------------------------------
    // Bridge lint (§149)
    // ---------------------------------------------------------

    @Test
    fun worldToLanguageEdgeAllowedOnlyThroughBridge() {
        // A station "represents" vocabulary — legal bridge edge.
        val legal = NodeEdge(
            type = RelationshipType.Represents,
            from = NodeId(NodeType.Station, "kamakura"),
            to = NodeId(NodeType.Vocabulary, "鎌倉")
        )
        assertFalse(legal.crossesBridgeIllegally())

        // A station "contains" a kanji is NOT a bridge edge — linted.
        val illegal = NodeEdge(
            type = RelationshipType.Contains,
            from = NodeId(NodeType.Station, "kamakura"),
            to = NodeId(NodeType.Kanji, "食")
        )
        assertTrue(illegal.crossesBridgeIllegally())
    }

    @Test
    fun sameFamilyEdgesNeverTriggerBridgeLint() {
        val sameFamily = NodeEdge(
            type = RelationshipType.Contains,
            from = NodeId(NodeType.Prefecture, "kanagawa"),
            to = NodeId(NodeType.City, "kamakura")
        )
        assertFalse(sameFamily.crossesBridgeIllegally())
    }

    @Test
    fun minedFromIsCurrentAndBridgeLegal() {
        val mining = NodeEdge(
            type = RelationshipType.MinedFrom,
            from = NodeId(NodeType.Card, "c1"),
            to = NodeId(NodeType.SubtitleLine, "l1")
        )
        assertTrue(mining.isValid)
        assertFalse(mining.crossesBridgeIllegally())
    }
}
