package ua.syt0r.kanji.core.knowledge.cards

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract

// ============================================================
// KANJI CARD SYSTEM
// ------------------------------------------------------------
// A kanji page is a sequence of modular cards. Users can show /
// hide / reorder cards and switch presets — the page layout is
// data (KanjiCardLayout), never hardcoded per screen. Adding a
// new card = one enum entry + one render branch in the screen;
// no other screen needs to change.
//
//   KanjiCardType      the registry of every available card
//   KanjiCardLayout    the user's per-profile layout (persisted)
//   KanjiCardPresets   Beginner / Standard / Advanced / Research
//   KanjiCardLayoutStore  JSON persistence
// ============================================================

/** Every card a kanji page can show. */
enum class KanjiCardType(
    val id: String,
    val title: String,
    val description: String
) {
    Hero("hero", "Hero", "The kanji itself with keyword"),
    Meaning("meaning", "Meaning", "Keyword and contextual meanings"),
    Readings("readings", "Readings", "On / kun readings"),
    Frequency("frequency", "Frequency", "Rank and band, with source"),
    Classification("classification", "Classification", "JLPT, grade, jōyō set"),
    Radical("radical", "Radical", "The kanji's radical"),
    Component("component", "Components", "Structural components"),
    Stroke("stroke", "Strokes", "Stroke count and stroke order"),
    Vocabulary("vocabulary", "Vocabulary", "Words that use this kanji"),
    Related("related", "Related kanji", "Kanji sharing a radical with this one"),
    Variant("variant", "Variant family", "Simplified / traditional / variant relationships"),
    Sentence("sentence", "Sentences", "Example sentences from the corpus"),
    Grammar("grammar", "Grammar", "Patterns found in the examples"),
    Graph("graph", "Graph", "Knowledge graph entry"),
    Media("media", "Media", "Where this kanji appears in your media library"),
    Study("study", "Study", "Study-state overview");

    companion object {
        fun byId(id: String): KanjiCardType? = entries.firstOrNull { it.id == id }
    }
}

/**
 * The user's kanji-page layout: display order + hidden cards.
 * Serialized as JSON and persisted per profile.
 */
@Serializable
data class KanjiCardLayout(
    val order: List<String> = KanjiCardType.entries.map { it.id },
    val hidden: Set<String> = emptySet(),
    /**
     * Per-card settings, keyed by card id (spec §21: per-card settings).
     * Today: the example/sentence limit for content cards. Empty = defaults
     * (a missing key always resolves to the card's default limit).
     */
    val cardSettings: Map<String, Int> = emptyMap()
) {

    /**
     * The configured example limit for a content card, or [defaultLimit]
     * when the user never touched it. Setting it to 0 is not allowed (a
     * hidden card is how you disable a section).
     */
    fun exampleLimit(type: KanjiCardType, defaultLimit: Int): Int =
        (cardSettings[type.id] ?: defaultLimit).coerceAtLeast(1)

    /** Sets the example limit for a card (persisted by the store). */
    fun setExampleLimit(type: KanjiCardType, limit: Int): KanjiCardLayout =
        copy(cardSettings = cardSettings + (type.id to limit.coerceAtLeast(1)))

    /**
     * Cards in display order, minus hidden ones. Registry card types missing
     * from the saved order are appended (so a card added in a new build shows
     * up for users with a saved layout instead of silently vanishing); the
     * stored [order] itself is never mutated.
     */
    fun visibleCards(): List<KanjiCardType> {
        val ordered = order.mapNotNull { id -> KanjiCardType.byId(id) }
        val orderedIds = ordered.mapTo(mutableSetOf()) { it.id }
        val appended = KanjiCardType.entries.filter { it.id !in orderedIds }
        return (ordered + appended).filter { it.id !in hidden }
    }

    fun isVisible(type: KanjiCardType): Boolean = type.id !in hidden

    fun setVisible(type: KanjiCardType, visible: Boolean): KanjiCardLayout =
        copy(hidden = if (visible) hidden - type.id else hidden + type.id)

    fun moveUp(type: KanjiCardType): KanjiCardLayout {
        val index = order.indexOf(type.id)
        if (index <= 0) return this
        val list = order.toMutableList()
        list.removeAt(index)
        list.add(index - 1, type.id)
        return copy(order = list)
    }

    fun moveDown(type: KanjiCardType): KanjiCardLayout {
        val index = order.indexOf(type.id)
        if (index < 0 || index >= order.lastIndex) return this
        val list = order.toMutableList()
        list.removeAt(index)
        list.add(index + 1, type.id)
        return copy(order = list)
    }

    /** Drops any unknown ids (stale layouts from older builds). */
    fun sanitized(): KanjiCardLayout = KanjiCardLayout(
        order = order.filter { KanjiCardType.byId(it) != null },
        hidden = hidden.filter { KanjiCardType.byId(it) != null }.toSet()
    )
}

/** Preset card layouts for different learner profiles. */
object KanjiCardPresets {

    val Minimal = KanjiCardLayout(
        order = KanjiCardType.entries.map { it.id },
        hidden = setOf(
            "frequency", "classification", "radical", "component",
            "related", "variant", "stroke", "grammar", "graph", "study", "sentence"
        )
    )

    val Beginner = KanjiCardLayout(
        order = KanjiCardType.entries.map { it.id },
        hidden = setOf(
            "frequency", "classification", "radical", "component",
            "related", "variant", "stroke", "grammar", "graph"
        )
    )

    val Standard = KanjiCardLayout(
        order = KanjiCardType.entries.map { it.id },
        hidden = setOf("component", "related", "variant", "stroke", "grammar", "graph")
    )

    val Advanced = KanjiCardLayout(
        order = KanjiCardType.entries.map { it.id },
        hidden = setOf("study")
    )

    val Intermediate = KanjiCardLayout(
        order = KanjiCardType.entries.map { it.id },
        hidden = setOf("related", "variant", "stroke", "grammar", "graph", "study")
    )

    val Writing = KanjiCardLayout(
        order = KanjiCardType.entries.map { it.id },
        hidden = setOf("frequency", "classification", "related", "variant", "grammar", "graph", "study")
    )

    val Reading = KanjiCardLayout(
        order = KanjiCardType.entries.map { it.id },
        hidden = setOf("classification", "radical", "component", "related", "variant", "stroke", "graph", "study")
    )

    val Dictionary = KanjiCardLayout(
        order = KanjiCardType.entries.map { it.id },
        hidden = setOf("graph", "study")
    )

    val Research = KanjiCardLayout(
        order = KanjiCardType.entries.map { it.id },
        hidden = emptySet()
    )

    val all: List<KanjiCardPreset> = listOf(
        KanjiCardPreset("minimal", "Minimal", "Meaning, readings, a few words", Minimal),
        KanjiCardPreset("beginner", "Beginner", "Core info without classification depth", Beginner),
        KanjiCardPreset("standard", "Standard", "Meaning, readings, frequency, vocabulary", Standard),
        KanjiCardPreset("intermediate", "Intermediate", "Everything up to advanced structure", Intermediate),
        KanjiCardPreset("advanced", "Advanced", "Adds components, strokes, grammar, graph", Advanced),
        KanjiCardPreset("writing", "Writing", "Writing-focused: meaning, readings, components, strokes", Writing),
        KanjiCardPreset("reading", "Reading", "Reading-focused: meaning, readings, vocabulary, sentences", Reading),
        KanjiCardPreset("dictionary", "Dictionary", "Every data card, no study state", Dictionary),
        KanjiCardPreset("research", "Research", "Everything", Research)
    )

    fun byId(id: String): KanjiCardPreset? = all.firstOrNull { it.id == id }
}

data class KanjiCardPreset(
    val id: String,
    val name: String,
    val description: String,
    val layout: KanjiCardLayout
)

/**
 * JSON persistence for the kanji-page layout. Falls back to defaults on
 * corrupt or stale blobs — a hand-edited preference can never crash layout.
 */
class KanjiCardLayoutStore(
    private val preferences: PreferencesContract.AppPreferences
) {

    suspend fun load(): KanjiCardLayout {
        val raw = preferences.kanjiCardLayoutJson.get()
        if (raw.isBlank()) return KanjiCardLayout()
        return runCatching {
            Json.decodeFromString<KanjiCardLayout>(raw).sanitized()
        }.getOrDefault(KanjiCardLayout())
    }

    suspend fun save(layout: KanjiCardLayout) {
        preferences.kanjiCardLayoutJson.set(
            Json.encodeToString(layout.sanitized())
        )
    }

    suspend fun reset() {
        preferences.kanjiCardLayoutJson.set("")
    }
}
