package ua.syt0r.kanji.desktop.game.learning

import kotlinx.serialization.Serializable

// ============================================================
// KNOWLEDGE GRAPH (spec §73-75, §125)
//
// The game's learning content is a *graph*, not a list. A kanji node links
// to word nodes, words link to sentence nodes, and every node carries a
// `kaiteyoKey` that resolves to a real Kaiteyo dictionary entry — the game
// never duplicates the dictionary (spec §26, §138).
// ============================================================

@Serializable
enum class KnowledgeKind {
    WORD, KANJI, GRAMMAR, SENTENCE
}

@Serializable
data class KnowledgeNode(
    val id: String,
    val kind: KnowledgeKind,
    /** The Japanese surface form (headword / kanji / pattern / sentence). */
    val headword: String,
    val reading: String = "",
    val meaning: String = "",
    /** Kanji node ids that make up this node (駅 → えき → えき に行く). */
    val kanjiIds: List<String> = emptyList(),
    /** Related knowledge node ids (known before this one). */
    val dependsOn: List<String> = emptyList(),
    val sentence: Sentence? = null,
    /** 1..10 rough difficulty used by adaptive support. */
    val difficulty: Int = 1,
    /** Dictionary key used to look this up in Kaiteyo (usually the headword). */
    val kaiteyoKey: String = ""
) {
    fun lookupKey(): String = kaiteyoKey.ifBlank { headword }
}

@Serializable
data class Sentence(
    val jp: String,
    val reading: String = "",
    val translation: String = "",
    /** Grammar node ids referenced by this sentence. */
    val grammar: List<String> = emptyList(),
    /** Vocabulary node ids appearing in this sentence. */
    val vocabulary: List<String> = emptyList(),
    /** Speaker id when this is an NPC line (listening practice, spec §61). */
    val speaker: String? = null,
    val audio: Boolean = false
)

@Serializable
enum class KnowledgeRelation {
    COMPONENT,      // 木 → 林 (kanji component)
    COMPOUND,       // 学校 → 学生 (shared kanji)
    WORD_IN_SENTENCE,
    SENTENCE_USES_GRAMMAR,
    READING_OF,
    MEANING_OF
}

@Serializable
data class KnowledgeLink(
    val fromId: String,
    val toId: String,
    val relation: KnowledgeRelation
)

/** The game's knowledge graph, loaded from `knowledge.json`. */
class KnowledgeGraph(
    nodes: List<KnowledgeNode>,
    links: List<KnowledgeLink>
) {
    private val nodeIndex: Map<String, KnowledgeNode> = nodes.associateBy { it.id }
    private val outgoing: Map<String, List<KnowledgeLink>> = links.groupBy { it.fromId }

    val nodes: List<KnowledgeNode> get() = nodeIndex.values.toList()

    fun node(id: String): KnowledgeNode? = nodeIndex[id]

    fun nodeByHeadword(headword: String): KnowledgeNode? =
        nodeIndex.values.firstOrNull { it.headword == headword || it.kaiteyoKey == headword }

    /** Direct neighbours (outgoing links) of a node — the graph navigation surface. */
    fun neighbours(id: String): List<KnowledgeNode> =
        outgoing[id].orEmpty().mapNotNull { nodeIndex[it.toId] }

    /** Expand a discovered word into its connected world (駅 → えき → 駅に行く…). */
    fun expand(id: String): List<KnowledgeNode> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<KnowledgeNode>()
        fun walk(currentId: String, depth: Int) {
            if (depth > 4 || !seen.add(currentId)) return
            nodeIndex[currentId]?.let { result.add(it) }
            for (neighbour in neighbours(currentId)) {
                walk(neighbour.id, depth + 1)
            }
        }
        walk(id, 0)
        return result
    }

    /** Learning chain for one target: kanji → word → sentence (spec §25). */
    fun chainFor(targetId: String): List<KnowledgeNode> {
        val target = nodeIndex[targetId] ?: return emptyList()
        val chain = mutableListOf<KnowledgeNode>()
        for (kanjiId in target.kanjiIds) {
            nodeIndex[kanjiId]?.let { if (it !in chain) chain.add(it) }
        }
        if (target !in chain) chain.add(target)
        target.sentence?.let { sentence ->
            for (vocabId in sentence.vocabulary) {
                nodeIndex[vocabId]?.let { if (it !in chain) chain.add(it) }
            }
        }
        return chain
    }

    fun validateReferences(report: (String) -> Unit) {
        for (node in nodes) {
            for (dep in node.dependsOn) {
                if (nodeIndex[dep] == null) report("Knowledge '${node.id}' depends on missing '${dep}'")
            }
            for (kanji in node.kanjiIds) {
                if (nodeIndex[kanji] == null) report("Knowledge '${node.id}' references missing kanji '${kanji}'")
            }
            node.sentence?.let { s ->
                for (v in s.vocabulary) {
                    if (nodeIndex[v] == null) report("Sentence in '${node.id}' references missing vocab '${v}'")
                }
            }
        }
        for (link in outgoing.values.flatten()) {
            if (nodeIndex[link.fromId] == null || nodeIndex[link.toId] == null) {
                report("Knowledge link references missing nodes: ${link.fromId} -> ${link.toId}")
            }
        }
    }
}

/** Support level controls how much help the world gives (spec §109). */
@Serializable
enum class AssistanceLevel(val label: String) {
    Minimal("Japanese only"),
    Normal("Japanese + reading"),
    Guided("Japanese + reading + hints"),
    Kids("Visual + simplified support")
}
