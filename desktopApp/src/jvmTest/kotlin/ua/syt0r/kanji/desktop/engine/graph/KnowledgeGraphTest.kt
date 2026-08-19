package ua.syt0r.kanji.desktop.engine.graph

import ua.syt0r.kanji.desktop.engine.jdata.api.LanguageDatabase
import ua.syt0r.kanji.desktop.engine.jdata.model.ComponentEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.ComponentKind
import ua.syt0r.kanji.desktop.engine.jdata.model.EntityType
import ua.syt0r.kanji.desktop.engine.jdata.model.KanjiEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.PlatformData
import ua.syt0r.kanji.desktop.engine.jdata.model.RadicalEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.ReadingInfo
import ua.syt0r.kanji.desktop.engine.jdata.model.RelationEdge
import ua.syt0r.kanji.desktop.engine.jdata.model.VocabEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.VocabSense
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.SrsStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KnowledgeGraphTest {

    private fun testDatabase(): LanguageDatabase {
        val kanji = mapOf(
            "k:学" to KanjiEntry(
                id = "k:学", character = "学",
                meanings = listOf("study", "learning"),
                onReadings = listOf("ガク"), kunReadings = listOf("まな"),
                radicalId = "r:木", jlpt = 5, frequencyRank = 50, strokeCount = 8
            ),
            "k:校" to KanjiEntry(id = "k:校", character = "校", meanings = listOf("school"), onReadings = listOf("コウ"), jlpt = 5),
            "k:大" to KanjiEntry(id = "k:大", character = "大", meanings = listOf("big"), onReadings = listOf("ダイ", "タイ"), kunReadings = listOf("おお"), jlpt = 5),
            "k:食" to KanjiEntry(id = "k:食", character = "食", meanings = listOf("eat"), onReadings = listOf("ショク"), kunReadings = listOf("たべ"), jlpt = 4)
        )
        val vocab = mapOf(
            "v:gakkou" to VocabEntry(
                id = "v:gakkou", expression = "学校",
                readings = listOf(ReadingInfo("がっこう")),
                senses = listOf(VocabSense(glosses = listOf("school"))),
                jlpt = 5
            ),
            "v:daigaku" to VocabEntry(
                id = "v:daigaku", expression = "大学",
                readings = listOf(ReadingInfo("だいがく")),
                senses = listOf(VocabSense(glosses = listOf("university"))),
                jlpt = 5
            ),
            "v:taberu" to VocabEntry(
                id = "v:taberu", expression = "食べる",
                readings = listOf(ReadingInfo("たべる")),
                senses = listOf(VocabSense(glosses = listOf("to eat"))),
                jlpt = 4
            )
        )
        val components = mapOf("c:冖" to ComponentEntry(id = "c:冖", character = "冖", kind = ComponentKind.DECOMPOSITION))
        val radicals = mapOf("r:木" to RadicalEntry(id = "r:木", character = "木", meaning = "tree"))

        // Bidirectional edges exactly as RelationshipGraph.build produces.
        val relations = listOf(
            RelationEdge("e1", EntityType.KANJI, "k:学", EntityType.VOCAB, "v:gakkou", "appears-in"),
            RelationEdge("e2", EntityType.VOCAB, "v:gakkou", EntityType.KANJI, "k:学", "contains"),
            RelationEdge("e3", EntityType.KANJI, "k:校", EntityType.VOCAB, "v:gakkou", "appears-in"),
            RelationEdge("e4", EntityType.VOCAB, "v:gakkou", EntityType.KANJI, "k:校", "contains"),
            RelationEdge("e5", EntityType.KANJI, "k:大", EntityType.VOCAB, "v:daigaku", "appears-in"),
            RelationEdge("e6", EntityType.VOCAB, "v:daigaku", EntityType.KANJI, "k:大", "contains"),
            RelationEdge("e7", EntityType.KANJI, "k:学", EntityType.VOCAB, "v:daigaku", "appears-in"),
            RelationEdge("e8", EntityType.VOCAB, "v:daigaku", EntityType.KANJI, "k:学", "contains"),
            RelationEdge("e9", EntityType.KANJI, "k:食", EntityType.VOCAB, "v:taberu", "appears-in"),
            RelationEdge("e10", EntityType.VOCAB, "v:taberu", EntityType.KANJI, "k:食", "contains"),
            RelationEdge("e11", EntityType.KANJI, "k:学", EntityType.COMPONENT, "c:冖", "component"),
            RelationEdge("e12", EntityType.KANJI, "k:学", EntityType.RADICAL, "r:木", "radical")
        )

        val data = PlatformData(
            schemaVersion = 1,
            generatedAt = "test",
            kanji = kanji,
            kana = emptyMap(),
            vocab = vocab,
            radicals = radicals,
            components = components,
            strokeSets = emptyMap(),
            relations = relations,
            sources = emptyMap()
        )
        return LanguageDatabase.open(data)
    }

    private fun graph(cards: List<DesktopCard> = emptyList()): KnowledgeGraph =
        KnowledgeGraph(testDatabase(), repository = null, cards = cards)

    // ------------------------------------------------------------
    // Search / resolution
    // ------------------------------------------------------------

    @Test
    fun searchFindsKanjiAndWords() {
        val g = graph()
        val results = g.search("学")
        assertTrue(results.any { it.kind == GraphNodeKind.Kanji && it.expression == "学" })
        val wordResults = g.search("学校")
        assertTrue(wordResults.any { it.kind == GraphNodeKind.Word && it.expression == "学校" })
    }

    @Test
    fun nodeResolvesKanjiWithMetadata() {
        val g = graph()
        val node = g.node("学")
        assertNotNull(node)
        assertEquals(GraphNodeKind.Kanji, node.kind)
        assertEquals(listOf("ガク", "まな"), node.readings)
        assertEquals(5, node.jlpt)
        assertEquals(50, node.frequencyRank)
        assertEquals(8, node.strokeCount)
    }

    // ------------------------------------------------------------
    // Detail / neighborhood
    // ------------------------------------------------------------

    @Test
    fun kanjiDetailListsWordsComponentsAndRadical() {
        val g = graph()
        val detail = g.detail("学")
        assertNotNull(detail)
        assertEquals(2, detail.words.size) // 学校, 大学
        assertTrue(detail.words.all { it.kind == GraphNodeKind.Word })
        assertTrue(detail.components.any { it.expression == "冖" })
        assertEquals("木", detail.radical?.expression)
        assertEquals(KnowledgeState.Unknown, detail.knowledge)
    }

    @Test
    fun wordDetailListsContainedKanji() {
        val g = graph()
        val detail = g.detail("学校")
        assertNotNull(detail)
        assertTrue(detail.relatedKanji.any { it.expression == "学" })
        assertTrue(detail.relatedKanji.any { it.expression == "校" })
    }

    @Test
    fun neighborsTraversesBothKinds() {
        val g = graph()
        val kanjiNeighbors = g.neighbors("学").map { it.second.expression }.toSet()
        assertTrue("学校" in kanjiNeighbors)
        assertTrue("大学" in kanjiNeighbors)
        assertTrue("冖" in kanjiNeighbors)
    }

    // ------------------------------------------------------------
    // Knowledge state
    // ------------------------------------------------------------

    @Test
    fun knowledgeStateComesFromCards() {
        val cards = listOf(
            DesktopCard(
                id = "c1", character = "学校", meaning = "school",
                tags = listOf("mined"), status = SrsStatus.New
            ),
            DesktopCard(
                id = "c2", character = "学", meaning = "study",
                status = SrsStatus.Review, intervalDays = 30.0
            )
        )
        val g = graph(cards)
        assertEquals(KnowledgeState.Mined, g.knowledgeOf("学校"))
        assertEquals(KnowledgeState.Mature, g.knowledgeOf("学"))
        assertEquals(KnowledgeState.Unknown, g.knowledgeOf("大学"))
    }

    // ------------------------------------------------------------
    // Path search
    // ------------------------------------------------------------

    @Test
    fun pathBetweenWordsViaSharedKanji() {
        val g = graph()
        val path = g.pathBetween("学校", "大学")
        assertNotNull(path)
        // 学校 → (contains) 学 → (appears-in) 大学
        assertEquals(GraphEdgeKind.Contains, path.first().edge)
        assertEquals("学", path[1].toExpression)
        assertEquals("大学", path.last().toExpression)
    }

    @Test
    fun pathBetweenReturnsNullWhenUnreachable() {
        val g = graph()
        // 食べる shares no kanji with 大学 within depth 2.
        val path = g.pathBetween("食べる", "大学", maxDepth = 2)
        assertNull(path)
    }

    @Test
    fun pathBetweenSameNodeIsEmpty() {
        val g = graph()
        assertEquals(emptyList<GraphPathHop>(), g.pathBetween("学", "学"))
    }
}
