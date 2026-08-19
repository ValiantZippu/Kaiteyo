package ua.syt0r.kanji.desktop.engine.graph

import kotlinx.serialization.Serializable

// ============================================
// KAITEYO KNOWLEDGE GRAPH — MODELS
// The node layer (ADR-0013 read-model): kanji,
// words, radicals and components as nodes, with
// real relation edges built from the language
// database (jdata). User-knowledge states are
// derived from the live card pool — never
// fabricated.
// ============================================

enum class GraphNodeKind { Kanji, Word, Radical, Component, Kana }

@Serializable
data class GraphNode(
    val id: String,
    val kind: GraphNodeKind,
    val expression: String,
    val readings: List<String> = emptyList(),
    val meanings: List<String> = emptyList(),
    val jlpt: Int? = null,
    val frequencyRank: Int? = null,
    val strokeCount: Int? = null
)

enum class GraphEdgeKind { Contains, AppearsIn, Component, Radical }

@Serializable
data class GraphEdge(
    val from: String,
    val to: String,
    val kind: GraphEdgeKind
)

/** User knowledge state for a node, derived from the card pool. */
enum class KnowledgeState { Unknown, New, Learning, Known, Mature, Mined, Suspended }

/** Everything the explorer shows for one node. */
data class GraphNodeDetail(
    val node: GraphNode,
    val components: List<GraphNode> = emptyList(),
    val words: List<GraphNode> = emptyList(),
    val relatedKanji: List<GraphNode> = emptyList(),
    val radical: GraphNode? = null,
    val knowledge: KnowledgeState = KnowledgeState.Unknown,
    val mediaAppearances: List<MediaAppearance> = emptyList()
)

/** Where a word/kanji has been seen in media (mined cards carry provenance). */
@Serializable
data class MediaAppearance(
    val mediaTitle: String,
    val timestamp: Double? = null,
    val cardId: String,
    val source: String
)

/** A traversal hop during a path search. */
data class GraphPathHop(
    val fromExpression: String,
    val edge: GraphEdgeKind,
    val toExpression: String
)
