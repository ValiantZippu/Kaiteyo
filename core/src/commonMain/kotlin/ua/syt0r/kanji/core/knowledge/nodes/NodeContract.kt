package ua.syt0r.kanji.core.knowledge.nodes

import kotlinx.serialization.Serializable

// ============================================================
// UNIVERSAL NODE CONTRACT (§78) + TYPED EDGES (§79–§80)
// ------------------------------------------------------------
// Every node in Kaiteyo — regardless of family — carries the
// same universal fields. Type-specific fields live on the
// concrete node implementations (or the typed payload slot),
// never in a nullable soup on the base contract.
//
// This is the code-side contract of ADR-0013: identity,
// provenance, and relationship validation are shared by every
// subsystem that consumes the node layer. The existing databases
// remain the storage of record — the node layer is additive.
// ============================================================

/** A stable, typed node identity (registry-backed, never a raw string). */
@Serializable
data class NodeId(
    val type: NodeType,
    /** Stable key within the type's namespace (e.g. the kanji character). */
    val ref: String
) {
    val asString: String get() = "${type.id}:$ref"

    companion object {
        /**
         * Parses "type:ref" back into a typed id. Returns null for an
         * unregistered type (the registry is the only source of truth).
         */
        fun parse(raw: String): NodeId? {
            val separator = raw.indexOf(':')
            if (separator <= 0) return null
            val type = NodeType.byId(raw.substring(0, separator)) ?: return null
            return NodeId(type, raw.substring(separator + 1))
        }
    }
}

/** Where a node came from — provenance is part of the contract (§78). */
@Serializable
enum class NodeSource {
    /** Bundled dictionary / corpus data (kjd, kanjidic, jmdict). */
    BundledData,
    /** The user (user-created content, cards, notes). */
    User,
    /** External integrations (Anki, Yomitan, AniList). */
    Integration,
    /** Derived aggregates (knowledge state, discovery records). */
    Derived,
    /** The future Journey world. */
    World
}

/**
 * The universal node contract (§78): every node has an id, a
 * typed identity, provenance, and optional hierarchy/tag hooks.
 * Family-specific fields are NOT here — they belong on concrete
 * node types. [schemaVersion] lets a node outlive its registry
 * row version; the registry's current version is the default.
 */
@Serializable
data class Node(
    val id: NodeId,
    val source: NodeSource,
    val sourceId: String = "",
    val schemaVersion: Int = 1,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L,
    val parentId: NodeId? = null,
    val ownerId: NodeId? = null,
    val worldId: String? = null,
    val tags: List<String> = emptyList()
)

/** A typed edge between two nodes (§79). */
@Serializable
data class NodeEdge(
    val type: RelationshipType,
    val from: NodeId,
    val to: NodeId,
    val weight: Float = 1f,
    val createdAtEpochMs: Long = 0L
) {
    val isValid: Boolean get() = validate().isEmpty()
}

/**
 * Validates an edge against the registry (STANDARDS §370: no
 * ad-hoc edges). Returns the list of violations (empty = valid).
 */
fun NodeEdge.validate(): List<String> {
    val violations = mutableListOf<String>()
    if (type.sourceTypes.isNotEmpty() && from.type !in type.sourceTypes) {
        violations += "source ${from.type.id} not allowed for '${type.id}' " +
            "(expected ${type.sourceTypes.joinToString { it.id }})"
    }
    if (type.targetTypes.isNotEmpty() && to.type !in type.targetTypes) {
        violations += "target ${to.type.id} not allowed for '${type.id}' " +
            "(expected ${type.targetTypes.joinToString { it.id }})"
    }
    if (from == to && !type.symmetric) {
        violations += "'${type.id}' cannot connect a node to itself"
    }
    return violations
}

/**
 * The two-graph bridge (§149): edges between the World graph and
 * the Language Knowledge graph are only legal through these edge
 * types. Used to police graph crossing at construction time.
 */
val BRIDGE_EDGE_TYPES: Set<RelationshipType> = setOf(
    RelationshipType.Represents,
    RelationshipType.EncounteredBy,
    RelationshipType.DiscoveredBy,
    RelationshipType.MinedFrom,
    RelationshipType.AppearsInMedia,
    RelationshipType.AppearsInScene,
    RelationshipType.Teaches
)

/**
 * True when the edge crosses the World ↔ Language boundary in a
 * way the bridge does not allow (a lint, not a hard rule — the
 * registry itself is the authority).
 */
fun NodeEdge.crossesBridgeIllegally(): Boolean =
    (from.type.family == NodeFamily.World && to.type.family == NodeFamily.Language ||
        from.type.family == NodeFamily.Language && to.type.family == NodeFamily.World) &&
        type !in BRIDGE_EDGE_TYPES
