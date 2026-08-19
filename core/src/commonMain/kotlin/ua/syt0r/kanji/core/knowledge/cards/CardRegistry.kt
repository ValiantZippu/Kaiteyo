package ua.syt0r.kanji.core.knowledge.cards

// ============================================================
// UNIFIED CARD REGISTRY
// ------------------------------------------------------------
// A single entry point to the modular card system. Each entity
// type (kanji, word, sentence, grammar) has its own card layout
// system. This registry provides a unified API for querying
// which cards exist, which are visible, and which presets apply.
//
// Usage:
//   val registry = CardRegistry()
//   val kanjiCards = registry.visibleCards(CardEntityType.Kanji)
//   val presets = registry.presetsFor(CardEntityType.Word)
// ============================================================

/** The entity types that have modular card pages. */
enum class CardEntityType(
    val label: String,
    val description: String
) {
    Kanji("Kanji", "Kanji character entry page"),
    Word("Word", "Vocabulary entry page"),
    Sentence("Sentence", "Sentence entry page"),
    Grammar("Grammar", "Grammar pattern entry page"),
    Collection("Collection", "Collection/JLPT/grade list page")
}

/**
 * Unified card registry. Wraps the four individual card systems
 * (KanjiCardType, WordCardType, SentenceCardType, GrammarCardType)
 * behind a single API.
 */
class CardRegistry {

    /** All card types for a given entity type. */
    fun allCardsFor(entity: CardEntityType): List<String> = when (entity) {
        CardEntityType.Kanji -> KanjiCardType.entries.map { it.id }
        CardEntityType.Word -> WordCardType.entries.map { it.id }
        CardEntityType.Sentence -> SentenceCardType.entries.map { it.id }
        CardEntityType.Grammar -> GrammarCardType.entries.map { it.id }
        CardEntityType.Collection -> CollectionCardType.entries.map { it.id }
    }

    /** Count of all cards for a given entity type. */
    fun cardCount(entity: CardEntityType): Int = when (entity) {
        CardEntityType.Kanji -> KanjiCardType.entries.size
        CardEntityType.Word -> WordCardType.entries.size
        CardEntityType.Sentence -> SentenceCardType.entries.size
        CardEntityType.Grammar -> GrammarCardType.entries.size
        CardEntityType.Collection -> CollectionCardType.entries.size
    }

    /** Available presets for a given entity type. */
    fun presetsFor(entity: CardEntityType): List<String> = when (entity) {
        CardEntityType.Kanji -> KanjiCardPresets.all.map { it.id }
        CardEntityType.Word -> WordCardPresets.all.map { it.id }
        CardEntityType.Sentence -> SentenceCardPresets.all.map { it.id }
        CardEntityType.Grammar -> GrammarCardPresets.all.map { it.id }
        CardEntityType.Collection -> CollectionCardPresets.all.map { it.id }
    }

    /** Get a display-friendly title for a card id. */
    fun cardTitle(entity: CardEntityType, cardId: String): String = when (entity) {
        CardEntityType.Kanji -> KanjiCardType.byId(cardId)?.title ?: cardId
        CardEntityType.Word -> WordCardType.byId(cardId)?.title ?: cardId
        CardEntityType.Sentence -> SentenceCardType.byId(cardId)?.title ?: cardId
        CardEntityType.Grammar -> GrammarCardType.byId(cardId)?.title ?: cardId
        CardEntityType.Collection -> CollectionCardType.byId(cardId)?.title ?: cardId
    }

    /** Get a display-friendly description for a card id. */
    fun cardDescription(entity: CardEntityType, cardId: String): String = when (entity) {
        CardEntityType.Kanji -> KanjiCardType.byId(cardId)?.description ?: ""
        CardEntityType.Word -> WordCardType.byId(cardId)?.description ?: ""
        CardEntityType.Sentence -> SentenceCardType.byId(cardId)?.description ?: ""
        CardEntityType.Grammar -> GrammarCardType.byId(cardId)?.description ?: ""
        CardEntityType.Collection -> CollectionCardType.byId(cardId)?.description ?: ""
    }

    /**
     * Compute visible cards from a stored layout JSON.
     * This is entity-type-agnostic: pass the layout JSON and
     * the entity type, get back the ordered list of visible card ids.
     */
    fun visibleCardIds(entity: CardEntityType, layoutJson: String): List<String> {
        return when (entity) {
            CardEntityType.Kanji -> runCatching {
                kotlinx.serialization.json.Json.decodeFromString<KanjiCardLayout>(layoutJson)
            }.getOrDefault(KanjiCardLayout()).visibleCards().map { it.id }

            CardEntityType.Word -> runCatching {
                kotlinx.serialization.json.Json.decodeFromString<WordCardLayout>(layoutJson)
            }.getOrDefault(WordCardLayout()).visibleCards().map { it.id }

            CardEntityType.Sentence -> runCatching {
                kotlinx.serialization.json.Json.decodeFromString<SentenceCardLayout>(layoutJson)
            }.getOrDefault(SentenceCardLayout()).visibleCards().map { it.id }

            CardEntityType.Grammar -> runCatching {
                kotlinx.serialization.json.Json.decodeFromString<GrammarCardLayout>(layoutJson)
            }.getOrDefault(GrammarCardLayout()).visibleCards().map { it.id }

            CardEntityType.Collection -> runCatching {
                kotlinx.serialization.json.Json.decodeFromString<CollectionCardLayout>(layoutJson)
            }.getOrDefault(CollectionCardLayout()).visibleCards().map { it.id }
        }
    }
}
