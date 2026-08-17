package ua.syt0r.kanji.desktop.game.learning

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import ua.syt0r.kanji.desktop.game.bridge.BridgeLookup
import ua.syt0r.kanji.desktop.game.bridge.BridgeMinePayload
import ua.syt0r.kanji.desktop.game.bridge.GameActivityKind
import ua.syt0r.kanji.desktop.game.bridge.GameBridge
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2

/**
 * Owns the player's learning state: which knowledge nodes have been
 * discovered in the world, how often they were encountered (adaptive — a
 * word you already know is never treated as new, spec §32), and which have
 * been mined into Kaiteyo. Every discovery flows through the [GameBridge].
 */
class LearningManager(
    private val graph: KnowledgeGraph,
    private val bridge: GameBridge
) {

    /** Serializable snapshot persisted in the save file. */
    @Serializable
    data class LearningState(
        val discovered: List<String> = emptyList(),
        val encounters: Map<String, Int> = emptyMap(),
        val mined: List<String> = emptyList(),
        val wordsLearned: Int = 0,
        val kanjiDiscovered: Int = 0
    )

    private val discoveredIds = mutableSetOf<String>()
    private val encounterCounts = mutableMapOf<String, Int>()
    private val minedIds = mutableSetOf<String>()

    var wordsLearned by mutableStateOf(0)
        private set
    var kanjiDiscovered by mutableStateOf(0)
        private set

    /** Bumped on every discovery so the UI can show the popup. */
    var pendingDiscovery by mutableStateOf<DiscoveryEvent?>(null)
        private set

    /** Consume the pending discovery once the UI has queued it. */
    fun clearPendingDiscovery() {
        pendingDiscovery = null
    }

    val discovered: Set<String> get() = discoveredIds.toSet()

    val mined: Set<String> get() = minedIds.toSet()

    fun isDiscovered(id: String): Boolean = id in discoveredIds

    fun encounterCount(id: String): Int = encounterCounts[id] ?: 0

    /**
     * The player encounters [nodeId] in the world (object inspection, NPC
     * line, sign, photo tag). Returns null when the node is already known.
     *
     * Adaptive learning (spec §73): a word the player already studies in
     * Kaiteyo is *recognized* in the world, never re-taught — no discovery
     * popup interrupts, nothing is double-counted, but the encounter still
     * registers (knowledge map, quests, encounters) because the world
     * noticing it is the reinforcement (spec §74).
     */
    fun discover(
        nodeId: String,
        assistance: AssistanceLevel,
        source: DiscoverySource,
        position: Vec2? = null
    ): DiscoveryEvent? {
        val node = graph.node(nodeId) ?: return null
        encounterCounts[nodeId] = encounterCount(nodeId) + 1
        val wasNew = discoveredIds.add(nodeId)
        val recognized = bridge.hasStudyMaterialFor(node.lookupKey())
        if (wasNew) {
            val lookup = bridge.lookup(node.lookupKey())
            val chain = graph.chainFor(nodeId)
            if (recognized) {
                // Already in the player's deck — recognize quietly (spec §74):
                // the encounter still counts for quests (LearnWord objectives
                // fire via the non-null return) and the knowledge map, but no
                // "New discovery!" popup and no learned-word counter bump.
                bridge.recordActivity(
                    GameActivityKind.WordDiscovered,
                    "Recognized ${node.headword} (already studied in Kaiteyo)"
                )
                return DiscoveryEvent(
                    node = node,
                    chain = chain,
                    wasNew = true,
                    supportLevel = assistance,
                    bridgeLookup = lookup,
                    source = source,
                    at = position
                )
            }
            when (node.kind) {
                KnowledgeKind.KANJI -> kanjiDiscovered++
                else -> wordsLearned++
            }
            // Meaningful activity: discovery is real learning, idling is not.
            bridge.recordActivity(
                if (node.kind == KnowledgeKind.KANJI) GameActivityKind.KanjiDiscovered
                else GameActivityKind.WordDiscovered,
                "Discovered ${node.headword} (${node.meaning}) in the world"
            )
            pendingDiscovery = DiscoveryEvent(
                node = node,
                chain = chain,
                wasNew = true,
                supportLevel = assistance,
                bridgeLookup = lookup,
                source = source,
                at = position
            )
        }
        return pendingDiscovery
    }

    /**
     * Mark a word as known directly (e.g. user already studies it in Kaiteyo).
     * Adaptive: already-known words never re-trigger discovery popups.
     */
    fun markKnown(id: String) {
        discoveredIds.add(id)
    }

    /** Mine a discovered node into Kaiteyo through the shared pipeline. */
    fun mine(nodeId: String, sourceDetail: String = "Game world"): Boolean {
        val node = graph.node(nodeId) ?: return false
        if (!minedIds.add(nodeId)) return false
        val lookup = bridge.lookup(node.lookupKey())
        val ok = bridge.mine(
            BridgeMinePayload(
                headword = node.headword,
                reading = node.reading.ifBlank { lookup?.reading.orEmpty() },
                definition = lookup?.meaning.orEmpty().ifBlank { node.meaning },
                sentence = node.sentence?.jp.orEmpty(),
                source = "game",
                sourceDetail = sourceDetail,
                tags = listOf("game", "discovery"),
                notes = "Discovered in the Kaiteyo game world"
            )
        )
        if (ok) bridge.toast("Mined \"${node.headword}\" → study it in Review")
        return ok
    }

    fun snapshot(): LearningState = LearningState(
        discovered = discoveredIds.sorted(),
        encounters = encounterCounts.toSortedMap(),
        mined = minedIds.sorted(),
        wordsLearned = wordsLearned,
        kanjiDiscovered = kanjiDiscovered
    )

    fun restore(state: LearningState) {
        discoveredIds.clear()
        discoveredIds.addAll(state.discovered)
        encounterCounts.clear()
        encounterCounts.putAll(state.encounters)
        minedIds.clear()
        minedIds.addAll(state.mined)
        wordsLearned = state.wordsLearned
        kanjiDiscovered = state.kanjiDiscovered
    }
}

/** Where a discovery came from — feeds quest events and stats. */
enum class DiscoverySource {
    Object, Npc, Sign, Photo, Quest, Environment, Writing
}

/** A discovery event the UI can present (spec §108). */
data class DiscoveryEvent(
    val node: KnowledgeNode,
    val chain: List<KnowledgeNode>,
    val wasNew: Boolean,
    val supportLevel: AssistanceLevel,
    val bridgeLookup: BridgeLookup?,
    val source: DiscoverySource,
    val at: Vec2?
)
