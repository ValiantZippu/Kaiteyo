package ua.syt0r.kanji.core.knowledge

import kotlinx.serialization.Serializable

// ============================================================
// LEARNING CONTENT — formula / mnemonic / explanation (spec
// §11–§12)
// ------------------------------------------------------------
// Multiple learning representations for the same entity:
//   DIRECT   the entry itself
//   FORMULA  structural logic (components → kanji)
//   GRAPH    relationships (the knowledge graph)
//   MNEMONIC a memory story
//   STROKE   writing progression
//   EXAMPLES real corpus sentences
//   VOCABULARY real words
//   PRACTICE study actions
//
// Content is NEVER fabricated: every item carries a source type
// (authoritative / AI / user / community), a confidence, and an
// attribution. AI content is always labeled AI. The formula for a
// kanji is DERIVED from the real decomposition (components from
// KANJIDIC radical data) — that is factual, so it is labeled
// Authoritative/Derived. Mnemonics ship with the registry empty:
// nothing is invented.
// ============================================================

@Serializable
enum class ContentSourceType(val label: String) {
    Authoritative("Authoritative"),
    AiGenerated("AI-generated"),
    UserGenerated("User-created"),
    Community("Community"),
    Derived("Derived from dataset")
}

@Serializable
enum class ContentConfidence(val label: String) {
    High("High"),
    Medium("Medium"),
    Low("Low")
}

@Serializable
enum class LearningContentType(val label: String) {
    Formula("Formula"),
    Mnemonic("Mnemonic"),
    Explanation("Explanation"),
    SimplifiedExplanation("Simplified explanation"),
    Example("Example"),
    GraphNote("Graph note")
}

/** A piece of learning content attached to a knowledge entity. */
@Serializable
data class LearningContent(
    val id: String,
    /** Entity key: "kanji:漢", "word:123", "sentence:…". */
    val entityKey: String,
    val type: LearningContentType,
    val title: String,
    val body: String,
    val sourceType: ContentSourceType,
    val confidence: ContentConfidence,
    /** Who/what to credit; null when the dataset itself is the source. */
    val attribution: String? = null
)

/**
 * Registry pattern: future sources (a licensed mnemonic dataset, an
 * AI suggestion service, user submissions) register content here with
 * their real provenance. The bundled app ships only [FormulaBuilder]
 * output — real decomposition-derived formulas — and an empty mnemonic
 * store, so no fake content ever appears.
 */
class LearningContentRegistry {

    private val contents = LinkedHashMap<String, LearningContent>()

    fun register(content: LearningContent) {
        contents[content.id] = content
    }

    fun registerAll(list: List<LearningContent>) {
        list.forEach(::register)
    }

    fun forEntity(entityKey: String): List<LearningContent> =
        contents.values.filter { it.entityKey == entityKey }
            .sortedWith(compareBy<LearningContent> { it.type.ordinal }.thenByDescending { it.confidence.ordinal })

    fun all(): List<LearningContent> = contents.values.toList()

    fun clear() = contents.clear()
}

/**
 * Builds a REAL structural formula from the decomposition, e.g.
 *   氵 + 又 → 漢
 * The components come from KANJIDIC radical data via the knowledge
 * repository, so the formula is derived, not invented. Returns null
 * when the kanji has no component data (no fake formula is emitted).
 */
object FormulaBuilder {

    fun formula(
        character: String,
        components: List<ComponentKnowledge>,
        sourceType: ContentSourceType = ContentSourceType.Derived
    ): LearningContent? {
        if (components.isEmpty()) return null
        val parts = components.joinToString(" + ") { it.component }
        return LearningContent(
            id = "formula:$character",
            entityKey = "kanji:$character",
            type = LearningContentType.Formula,
            title = "Structure of $character",
            body = "$parts → $character",
            sourceType = sourceType,
            confidence = if (sourceType == ContentSourceType.AiGenerated) ContentConfidence.Medium else ContentConfidence.High,
            attribution = if (sourceType == ContentSourceType.Derived) "derived from KANJIDIC radical decomposition" else null
        )
    }
}
