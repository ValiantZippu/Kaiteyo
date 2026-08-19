package ua.syt0r.kanji.desktop.engine.graph

import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryRepository
import ua.syt0r.kanji.desktop.engine.jdata.api.LanguageDatabase
import ua.syt0r.kanji.desktop.engine.jdata.model.KanjiEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.VocabEntry
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.SrsStatus

// ============================================
// KAITEYO KNOWLEDGE GRAPH — ENGINE
// A read-model over the language database
// (ADR-0013): nodes are kanji/words/components/
// radicals with real relation edges, and each
// node's user knowledge state is derived from
// the live card pool. Pure and Compose-free —
// fully testable with a real LanguageDatabase
// built from a test dictionary repository.
// ============================================

class KnowledgeGraph(
    private val database: LanguageDatabase?,
    private val repository: DictionaryRepository?,
    private val cards: List<DesktopCard>
) {

    /** True when the jdata platform is available (dictionaries installed). */
    val available: Boolean get() = database != null

    // ------------------------------------------------------------
    // Search / resolve
    // ------------------------------------------------------------

    /** Find nodes matching a query: kanji exact, then vocabulary, then autocomplete. */
    fun search(query: String): List<GraphNode> {
        val db = database ?: return emptyList()
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        val results = mutableListOf<GraphNode>()
        // Exact kanji.
        db.getKanji(q)?.let { results += kanjiNode(it) }
        // Exact vocabulary.
        db.getVocabulary(q).forEach { results += wordNode(it) }
        // Autocomplete suggestions.
        if (results.size < 8) {
            db.autocomplete(q, limit = 8 - results.size).forEach { suggestion ->
                if (results.none { it.expression == suggestion.text }) {
                    db.getKanji(suggestion.text)?.let { results += kanjiNode(it) }
                        ?: db.getVocabulary(suggestion.text).firstOrNull()?.let { results += wordNode(it) }
                }
            }
        }
        return results.distinctBy { it.id }
    }

    fun node(expression: String): GraphNode? {
        val db = database ?: return null
        db.getKanji(expression)?.let { return kanjiNode(it) }
        db.getVocabulary(expression).firstOrNull()?.let { return wordNode(it) }
        return null
    }

    // ------------------------------------------------------------
    // Detail (one node + its neighborhood)
    // ------------------------------------------------------------

    fun detail(expression: String): GraphNodeDetail? {
        val db = database ?: return null
        val node = node(expression) ?: return null
        val kanji = db.getKanji(expression)

        val components = if (kanji != null) {
            db.getComponents(expression).mapNotNull { c ->
                c.character.takeIf { it.isNotBlank() }?.let { ch ->
                    GraphNode(
                        id = "component:$ch",
                        kind = GraphNodeKind.Component,
                        expression = ch
                    )
                }
            }
        } else emptyList()

        val radical = if (kanji != null) {
            db.getRadical(expression)?.let { r ->
                GraphNode(
                    id = "radical:${r.character}",
                    kind = GraphNodeKind.Radical,
                    expression = r.character,
                    meanings = listOfNotNull(r.meaning)
                )
            }
        } else null

        val words = if (kanji != null) {
            db.vocabForKanji(expression).map(::wordNode)
        } else emptyList()

        // For a word node: the kanji it contains (traverse the other way).
        val relatedKanji = if (kanji == null) {
            db.kanjiForVocab(node.id).map(::kanjiNode)
        } else emptyList()

        return GraphNodeDetail(
            node = node,
            components = components,
            words = words,
            relatedKanji = relatedKanji,
            radical = radical,
            knowledge = knowledgeOf(expression),
            mediaAppearances = MediaExposureIndex.appearancesFor(expression, cards)
        )
    }

    // ------------------------------------------------------------
    // Knowledge state (from the live card pool)
    // ------------------------------------------------------------

    fun knowledgeOf(expression: String): KnowledgeState {
        val card = cards.firstOrNull { it.character == expression } ?: return KnowledgeState.Unknown
        return when {
            card.status == SrsStatus.Suspended || card.status == SrsStatus.Buried -> KnowledgeState.Suspended
            card.tags.contains("mined") -> KnowledgeState.Mined
            card.status == SrsStatus.Review && card.intervalDays >= 21 -> KnowledgeState.Mature
            card.status == SrsStatus.Review -> KnowledgeState.Known
            card.status == SrsStatus.Learning || card.status == SrsStatus.Relearning -> KnowledgeState.Learning
            else -> KnowledgeState.New
        }
    }

    // ------------------------------------------------------------
    // Path search (BFS over relation edges)
    // ------------------------------------------------------------

    /**
     * Find a relation path between two expressions (e.g. 食べる → 食 → 食事).
     * Returns the hop list, or null when no path exists within [maxDepth].
     */
    fun pathBetween(from: String, to: String, maxDepth: Int = 4): List<GraphPathHop>? {
        val db = database ?: return null
        if (from == to) return emptyList()
        val fromNode = node(from) ?: return null
        val toNode = node(to) ?: return null

        data class Frontier(val expression: String, val hops: List<GraphPathHop>)
        val visited = mutableSetOf<String>()
        var frontier = listOf(Frontier(fromNode.expression, emptyList()))

        repeat(maxDepth) {
            val next = mutableListOf<Frontier>()
            for (f in frontier) {
                if (f.expression !in visited) {
                    visited += f.expression
                    neighbors(f.expression).forEach { (edge, neighbor) ->
                        val hops = f.hops + GraphPathHop(f.expression, edge, neighbor.expression)
                        if (neighbor.expression == toNode.expression) return hops
                        next += Frontier(neighbor.expression, hops)
                    }
                }
            }
            frontier = next
        }
        return null
    }

    /** Direct relation edges from a node, deduplicated by (kind, to). */
    fun neighbors(expression: String): List<Pair<GraphEdgeKind, GraphNode>> {
        val db = database ?: return emptyList()
        val node = node(expression) ?: return emptyList()
        val result = mutableListOf<Pair<GraphEdgeKind, GraphNode>>()

        when (node.kind) {
            GraphNodeKind.Kanji -> {
                db.vocabForKanji(expression).forEach { result += GraphEdgeKind.AppearsIn to wordNode(it) }
                db.getComponents(expression).forEach { c ->
                    c.character.takeIf { it.isNotBlank() }?.let { ch ->
                        result += GraphEdgeKind.Component to GraphNode(
                            id = "component:$ch",
                            kind = GraphNodeKind.Component,
                            expression = ch
                        )
                    }
                }
                db.getRadical(expression)?.let { r ->
                    result += GraphEdgeKind.Radical to GraphNode(
                        id = "radical:${r.character}",
                        kind = GraphNodeKind.Radical,
                        expression = r.character,
                        meanings = listOfNotNull(r.meaning)
                    )
                }
            }

            GraphNodeKind.Word -> {
                db.kanjiForVocab(node.id).forEach { result += GraphEdgeKind.Contains to kanjiNode(it) }
            }

            else -> {}
        }
        return result.distinctBy { it.first to it.second.id }
    }

    // ------------------------------------------------------------
    // Node mapping
    // ------------------------------------------------------------

    private fun kanjiNode(entry: KanjiEntry): GraphNode = GraphNode(
        id = entry.id,
        kind = GraphNodeKind.Kanji,
        expression = entry.character,
        readings = entry.onReadings + entry.kunReadings,
        meanings = entry.meanings,
        jlpt = entry.jlpt,
        frequencyRank = entry.frequencyRank,
        strokeCount = entry.strokeCount
    )

    private fun wordNode(entry: VocabEntry): GraphNode = GraphNode(
        id = entry.id,
        kind = GraphNodeKind.Word,
        expression = entry.expression,
        readings = entry.readings.mapNotNull { it.kana },
        meanings = entry.allGlosses,
        jlpt = entry.jlpt,
        frequencyRank = entry.frequencies.firstOrNull()?.rank
    )
}
