package ua.syt0r.kanji.core.knowledge.nodes

// ============================================================
// NODE REGISTRY FACADE — single injectable entry point
// ------------------------------------------------------------
// The registries are enums (pure vocabulary); this facade is the
// one Koin-injectable handle consumers use instead of reaching
// into enum companions directly. It adds nothing but a stable
// API surface, so storage decisions (ADR-0013 implementation
// detail) can later hang off it without changing callers.
// ============================================================

class NodeRegistryFacade {

    /** All registered node types, registry order. */
    val allNodeTypes: List<NodeType> get() = NodeType.entries

    /** All registered relationship types, registry order. */
    val allRelationshipTypes: List<RelationshipType> get() = RelationshipType.entries

    fun nodeTypeById(id: String): NodeType? = NodeType.byId(id)

    fun relationshipById(id: String): RelationshipType? = RelationshipType.byId(id)

    fun nodeTypesOfFamily(family: NodeFamily): List<NodeType> = NodeType.ofFamily(family)

    /** The two-graph bridge edge types (§149). */
    val bridgeEdgeTypes: Set<RelationshipType> get() = BRIDGE_EDGE_TYPES
}
