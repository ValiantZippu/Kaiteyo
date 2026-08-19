package ua.syt0r.kanji.core.knowledge.nodes

// ============================================================
// RELATIONSHIP REGISTRY — the edge vocabulary as code
// ------------------------------------------------------------
// Faithful to docs/architecture/nodes/RELATIONSHIP_REGISTRY.md:
// a controlled vocabulary of typed relationships with source and
// target constraints. `related_to` is the linted escape hatch,
// never the default. A relationship is only valid when:
//   (a) it is a registered type, AND
//   (b) the source node type is in [sourceTypes] (empty = any), AND
//   (c) the target node type is in [targetTypes] (empty = any).
//
// The registry is intentionally minimal — new edges are a
// registry change plus validation, never ad-hoc inline strings.
// ============================================================

/** A directed, typed edge between two nodes (§79–§80). */
enum class RelationshipType(
    val id: String,
    /** Registry "from" constraint — empty = any node type. */
    val sourceTypes: List<NodeType> = emptyList(),
    /** Registry "to" constraint — empty = any node type. */
    val targetTypes: List<NodeType> = emptyList(),
    /** Whether the edge also makes sense reversed (↔ in the doc). */
    val symmetric: Boolean = false,
    /** Escape hatch — see docs; use only when no better type fits. */
    val isEscapeHatch: Boolean = false,
    val status: NodeStatus = NodeStatus.Target
) {

    // ── Hierarchy / membership ──────────────────────────────
    Contains("contains", symmetric = true, status = NodeStatus.Current),
    ContainsCharacter("contains_character", listOf(NodeType.Vocabulary), listOf(NodeType.Kanji), status = NodeStatus.Current),
    ContainsComponent("contains_component", listOf(NodeType.Kanji), listOf(NodeType.Component)),
    UsesRadical("uses_radical", listOf(NodeType.Kanji), listOf(NodeType.Radical)),
    PartOf("part_of", symmetric = true, status = NodeStatus.Current),
    BelongsTo("belongs_to", status = NodeStatus.Current),
    ParentOf("parent_of"),
    ChildOf("child_of"),

    // ── Language attributes ─────────────────────────────────
    HasReading("has_reading", listOf(NodeType.Kanji, NodeType.Vocabulary), listOf(NodeType.Reading), status = NodeStatus.Current),
    HasMeaning("has_meaning", listOf(NodeType.Kanji, NodeType.Vocabulary), listOf(NodeType.Meaning), status = NodeStatus.Current),
    HasPitch("has_pitch", listOf(NodeType.Vocabulary), listOf(NodeType.PitchPattern)),
    HasFrequency("has_frequency", listOf(NodeType.Kanji, NodeType.Vocabulary), listOf(NodeType.FrequencyEntry)),
    HasJlpt("has_jlpt", listOf(NodeType.Kanji, NodeType.Vocabulary, NodeType.Grammar), status = NodeStatus.Current),
    HasGrade("has_grade", listOf(NodeType.Kanji), status = NodeStatus.Current),

    // ── Occurrence ──────────────────────────────────────────
    AppearsIn("appears_in", status = NodeStatus.Current),
    AppearsInSentence(
        "appears_in_sentence",
        listOf(NodeType.Kanji, NodeType.Vocabulary, NodeType.Grammar),
        listOf(NodeType.Sentence)
    ),
    AppearsInMedia(
        "appears_in_media",
        listOf(NodeType.Kanji, NodeType.Vocabulary),
        listOf(NodeType.SubtitleLine, NodeType.Scene)
    ),
    AppearsInScene(
        "appears_in_scene",
        listOf(NodeType.Kanji, NodeType.Vocabulary),
        listOf(NodeType.Scene)
    ),
    Demonstrates("demonstrates", listOf(NodeType.Sentence), listOf(NodeType.Grammar)),
    SynonymOf("synonym_of", symmetric = true),
    AntonymOf("antonym_of", symmetric = true),
    ConjugatesTo("conjugates_to", symmetric = true),

    // ── Derivation / provenance ─────────────────────────────
    DerivedFrom("derived_from"),
    RelatedTo("related_to", symmetric = true, isEscapeHatch = true, status = NodeStatus.Target),

    // ── World placement ─────────────────────────────────────
    LocatedAt("located_at"),
    ContainsLocation("contains_location"),
    Represents("represents"),
    Depicts("depicts"),
    References("references"),
    ImportedFrom("imported_from", status = NodeStatus.Current),
    GeneratedFrom("generated_from", status = NodeStatus.Current),
    MinedFrom("mined_from", status = NodeStatus.Current),
    MappedTo("mapped_to", symmetric = true),

    // ── Learning / knowledge ────────────────────────────────
    Teaches("teaches"),
    Reviews("reviews", status = NodeStatus.Current),
    MasteredBy("mastered_by"),
    EncounteredBy("encountered_by"),
    DiscoveredBy("discovered_by"),
    ScheduledAt("scheduled_at"),
    Unlocks("unlocks"),
    Rewards("rewards"),
    Requires("requires"),
    Precedes("precedes", symmetric = true),
    Follows("follows", symmetric = true),
    ParticipatesIn("participates_in"),
    DependsOn("depends_on");

    companion object {
        private val byIdMap = entries.associateBy { it.id }

        /** Registry lookup — the only way to resolve an edge type string. */
        fun byId(id: String): RelationshipType? = byIdMap[id]

        /** The reverse edge for symmetric types (identity), else null. */
        fun reverseOf(type: RelationshipType): RelationshipType? =
            if (type.symmetric) type else null
    }
}
