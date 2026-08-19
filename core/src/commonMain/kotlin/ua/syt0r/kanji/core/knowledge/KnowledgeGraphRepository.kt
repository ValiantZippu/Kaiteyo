package ua.syt0r.kanji.core.knowledge

import ua.syt0r.kanji.core.japanese.isKanji

// ============================================================
// KNOWLEDGE GRAPH — REPOSITORY
// ------------------------------------------------------------
// Builds and progressively expands knowledge graphs from real
// dictionary data. Every edge maps to a real relationship query
// in KnowledgeRepository; nothing is fabricated.
//
// Expansion rules (all capped so the graph never explodes):
//   kanji    → radicals (ComponentOf) · words (UsedIn)
//              · related kanji by shared radical (RelatedTo)
//              · sentences containing its readings (AppearsIn)
//   radical  → kanji that use it (RadicalOf)
//   word     → kanji in its spelling (Contains)
//              · sentences containing its reading (AppearsIn)
//   sentence → grammar patterns found in the text (ExampleOf)
//   grammar  → nothing (leaf)
// ============================================================

class KnowledgeGraphRepository(
    private val knowledge: KnowledgeRepository,
    private val mediaReferences: ua.syt0r.kanji.core.knowledge.media.MediaReferenceStore? = null
) {

    /** Default per-hop node cap — keeps graphs dense but bounded. */
    var defaultHopLimit: Int = 10

    /**
     * Opens a graph for [character]: the root kanji plus its first ring
     * (components/radicals and the words that use it). Sentences are only
     * expanded on demand to keep the opening cheap.
     */
    suspend fun initialGraph(character: String): KnowledgeGraph {
        val root = knowledge.kanji(character) ?: return KnowledgeGraph(rootId = null)
        val rootId = KnowledgeNode.kanji(character)

        val nodes = mutableListOf(root.toNode())
        val edges = mutableListOf<KnowledgeEdge>()

        // First ring: components / radicals + words using the kanji.
        knowledge.componentsIn(character).forEach { component ->
            val nodeId = KnowledgeNode.radical(component.component)
            nodes.addIfAbsent(
                KnowledgeNode(
                    id = nodeId,
                    kind = KnowledgeNodeKind.Radical,
                    label = component.component,
                    subtitle = "${component.strokesCount} strokes"
                )
            )
            edges.add(KnowledgeEdge(rootId, nodeId, KnowledgeEdgeType.ComponentOf))
        }

        knowledge.wordsContaining(character, limit = defaultHopLimit).forEach { word ->
            val nodeId = KnowledgeNode.word(word.id)
            nodes.addIfAbsent(word.toNode())
            edges.add(KnowledgeEdge(rootId, nodeId, KnowledgeEdgeType.UsedIn))
        }

        return KnowledgeGraph(rootId = rootId, nodes = nodes.associateBy { it.id }, edges = edges)
    }

    /**
     * Expands [nodeId] by one hop. [wantedTypes] filters which relationship
     * kinds to pull; [hopLimit] caps how many neighbors each kind contributes.
     */
    suspend fun expand(
        graph: KnowledgeGraph,
        nodeId: String,
        wantedTypes: Set<KnowledgeEdgeType>? = null,
        hopLimit: Int = defaultHopLimit
    ): GraphExpansion {
        val node = graph.node(nodeId) ?: return GraphExpansion(graph = graph, addedNodes = emptyList(), addedEdges = emptyList(), exhausted = listOf(nodeId))

        val allow = wantedTypes
        val nodes = mutableListOf<KnowledgeNode>()
        val edges = mutableListOf<KnowledgeEdge>()

        fun wants(type: KnowledgeEdgeType) = allow == null || type in allow

        when (node.kind) {
            KnowledgeNodeKind.Kanji -> {
                val character = node.label

                if (wants(KnowledgeEdgeType.ComponentOf)) {
                    knowledge.componentsIn(character).forEach { component ->
                        val target = KnowledgeNode.radical(component.component)
                        if (graph.node(target) == null) {
                            nodes.add(
                                KnowledgeNode(
                                    id = target,
                                    kind = KnowledgeNodeKind.Radical,
                                    label = component.component,
                                    subtitle = "${component.strokesCount} strokes"
                                )
                            )
                        }
                        edges.add(KnowledgeEdge(nodeId, target, KnowledgeEdgeType.ComponentOf))
                    }
                }

                if (wants(KnowledgeEdgeType.UsedIn)) {
                    knowledge.wordsContaining(character, limit = hopLimit).forEach { word ->
                        val target = KnowledgeNode.word(word.id)
                        if (graph.node(target) == null) nodes.add(word.toNode())
                        edges.add(KnowledgeEdge(nodeId, target, KnowledgeEdgeType.UsedIn))
                    }
                }

                if (wants(KnowledgeEdgeType.RelatedTo)) {
                    knowledge.kanjiRelatedByRadical(character, limit = hopLimit).forEach { related ->
                        val target = KnowledgeNode.kanji(related)
                        val relatedKanji = knowledge.kanji(related)
                        if (graph.node(target) == null) {
                            nodes.add(relatedKanji?.toNode() ?: KnowledgeNode(target, KnowledgeNodeKind.Kanji, related))
                        }
                        edges.add(KnowledgeEdge(nodeId, target, KnowledgeEdgeType.RelatedTo))
                    }
                }

                if (wants(KnowledgeEdgeType.AppearsIn)) {
                    val readings = (node.extra["on"].orEmpty() + node.extra["kun"].orEmpty())
                        .split(',')
                        .filter { it.isNotBlank() }
                    readings.take(3).forEach { reading ->
                        knowledge.sentencesWithText(reading, limit = hopLimit).forEach { sentence ->
                            val target = KnowledgeNode.sentence(sentence.index())
                            if (graph.node(target) == null) nodes.add(sentence.toNode())
                            edges.add(KnowledgeEdge(nodeId, target, KnowledgeEdgeType.AppearsIn))
                        }
                    }
                }
            }

            KnowledgeNodeKind.Radical -> {
                if (wants(KnowledgeEdgeType.RadicalOf)) {
                    knowledge.kanjiWithRadicals(listOf(node.label)).take(hopLimit).forEach { character ->
                        val target = KnowledgeNode.kanji(character)
                        val kanji = knowledge.kanji(character)
                        if (graph.node(target) == null) {
                            nodes.add(kanji?.toNode() ?: KnowledgeNode(target, KnowledgeNodeKind.Kanji, character))
                        }
                        edges.add(KnowledgeEdge(nodeId, target, KnowledgeEdgeType.RadicalOf))
                    }
                }
            }

            KnowledgeNodeKind.Word -> {
                if (wants(KnowledgeEdgeType.Contains)) {
                    val spelling = node.extra["spelling"] ?: node.label
                    spelling.forEach { char ->
                        if (char.isKanji()) {
                            val target = KnowledgeNode.kanji(char.toString())
                            val kanji = knowledge.kanji(char.toString())
                            if (kanji != null && graph.node(target) == null) {
                                nodes.add(kanji.toNode())
                            }
                            if (kanji != null) edges.add(KnowledgeEdge(nodeId, target, KnowledgeEdgeType.Contains))
                        }
                    }
                }

                if (wants(KnowledgeEdgeType.AppearsIn)) {
                    val reading = node.extra["kana"]?.ifBlank { node.label } ?: node.label
                    knowledge.sentencesWithText(reading, limit = hopLimit).forEach { sentence ->
                        val target = KnowledgeNode.sentence(sentence.index())
                        if (graph.node(target) == null) nodes.add(sentence.toNode())
                        edges.add(KnowledgeEdge(nodeId, target, KnowledgeEdgeType.AppearsIn))
                    }
                    // Media occurrences (spec §28): real bookmarks whose
                    // Japanese text matches this word.
                    expandMedia(node, nodeId, graph, nodes, edges, reading, hopLimit)
                }
            }

            KnowledgeNodeKind.Sentence -> {
                if (wants(KnowledgeEdgeType.ExampleOf)) {
                    knowledge.grammarIn(node.label).distinctBy { it.patternId }.take(hopLimit).forEach { match ->
                        val pattern = GrammarCatalog.byId(match.patternId)
                        val target = KnowledgeNode.grammar(match.patternId)
                        if (graph.node(target) == null) {
                            nodes.add(
                                KnowledgeNode(
                                    id = target,
                                    kind = KnowledgeNodeKind.Grammar,
                                    label = pattern?.pattern ?: match.matchedText,
                                    subtitle = pattern?.meaning,
                                    extra = mapOf("jlpt" to (pattern?.jlpt?.toString() ?: ""))
                                )
                            )
                        }
                        edges.add(KnowledgeEdge(nodeId, target, KnowledgeEdgeType.ExampleOf))
                    }
                }
            }

            KnowledgeNodeKind.Grammar -> {
                // Grammar nodes are leaves — nothing to expand.
                return GraphExpansion(graph = graph, addedNodes = emptyList(), addedEdges = emptyList(), exhausted = listOf(nodeId))
            }

            KnowledgeNodeKind.Media -> {
                // Media nodes are leaves — the reference carries the context.
                return GraphExpansion(graph = graph, addedNodes = emptyList(), addedEdges = emptyList(), exhausted = listOf(nodeId))
            }
        }

        val expanded = graph.merged(nodes, edges)
        return GraphExpansion(
            graph = expanded,
            addedNodes = nodes,
            addedEdges = edges,
            exhausted = if (nodes.isEmpty() && edges.isEmpty()) listOf(nodeId) else emptyList()
        )
    }

    /**
     * Adds media-reference nodes for [queryText] with AppearsIn edges.
     * The store is optional (no media backend on non-desktop), so this
     * contributes nothing when no references have been recorded.
     */
    private suspend fun expandMedia(
        node: KnowledgeNode,
        nodeId: String,
        graph: KnowledgeGraph,
        nodes: MutableList<KnowledgeNode>,
        edges: MutableList<KnowledgeEdge>,
        queryText: String,
        hopLimit: Int
    ) {
        val store = mediaReferences ?: return
        store.matching(queryText, limit = hopLimit.coerceAtMost(4)).forEach { ref ->
            val target = KnowledgeNode.media(ref.title, ref.timestampMs, ref.text)
            if (graph.node(target) == null) {
                nodes.add(
                    KnowledgeNode(
                        id = target,
                        kind = KnowledgeNodeKind.Media,
                        label = ref.text,
                        subtitle = ref.title,
                        extra = mapOf(
                            "media" to ref.title,
                            "timestamp" to ref.timestampMs.toString()
                        )
                    )
                )
            }
            edges.add(KnowledgeEdge(nodeId, target, KnowledgeEdgeType.AppearsIn))
        }
    }

    private fun MutableList<KnowledgeNode>.addIfAbsent(node: KnowledgeNode) {
        if (none { it.id == node.id }) add(node)
    }
}

// ---------------------------------------------------------------
// Node builders — file-scope extensions so NodeTraversal can seed
// graphs from ids without duplicating the build rules (KT-DICT-003
// traversal chips). Each maps a real knowledge entry onto a node.
// ---------------------------------------------------------------

fun KanjiKnowledge.toNode(): KnowledgeNode = KnowledgeNode(
    id = KnowledgeNode.kanji(character),
    kind = KnowledgeNodeKind.Kanji,
    label = character,
    subtitle = keyword,
    extra = mapOf(
        "on" to onReadings.joinToString(","),
        "kun" to kunReadings.joinToString(","),
        "frequency" to (frequencyRank?.toString() ?: "")
    )
)

fun WordKnowledge.toNode(): KnowledgeNode = KnowledgeNode(
    id = KnowledgeNode.word(id),
    kind = KnowledgeNodeKind.Word,
    label = displaySpelling,
    subtitle = combinedGlossary().take(80),
    extra = mapOf(
        "spelling" to (kanjiReading ?: ""),
        "kana" to kanaReading,
        "pos" to partOfSpeech.joinToString(",")
    )
)

fun SentenceKnowledge.toNode(): KnowledgeNode = KnowledgeNode(
    id = KnowledgeNode.sentence(index()),
    kind = KnowledgeNodeKind.Sentence,
    label = text,
    subtitle = translation.take(80),
    extra = emptyMap()
)

private fun SentenceKnowledge.index(): Long = (text.hashCode().toLong() and 0x7fffffffL)
