package ua.syt0r.kanji.core.knowledge.cards

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract

// ============================================================
// GRAMMAR CARD SYSTEM
// ------------------------------------------------------------
// A grammar page is a sequence of modular cards, just like
// the kanji, word, and sentence pages. Users can show / hide /
// reorder cards and apply presets — the page layout is data
// (GrammarCardLayout), never hardcoded per screen.
//
//   GrammarCardType     the registry of every available card
//   GrammarCardLayout   the user's per-profile layout (persisted)
//   GrammarCardPresets  Beginner / Standard / Advanced / Research
//   GrammarCardLayoutStore  JSON persistence
// ============================================================

/** Every card a grammar page can show. */
enum class GrammarCardType(
    val id: String,
    val title: String,
    val description: String
) {
    Hero("hero", "Hero", "The grammar pattern with reading and meaning"),
    Meaning("meaning", "Meaning", "Full explanation and nuance notes"),
    Structure("structure", "Structure", "Pattern breakdown and conjugation"),
    Examples("examples", "Examples", "Example sentences using this pattern"),
    JLPT("jlpt", "JLPT", "JLPT level and related grammar"),
    RelatedGrammar("related", "Related", "Similar and related grammar patterns"),
    Kanji("kanji", "Kanji", "Kanji used in pattern examples"),
    Study("study", "Study", "Study-state overview and actions");

    companion object {
        fun byId(id: String): GrammarCardType? = entries.firstOrNull { it.id == id }
    }
}

/**
 * The user's grammar-page layout: display order + hidden cards.
 * Serialized as JSON and persisted per profile.
 */
@Serializable
data class GrammarCardLayout(
    val order: List<String> = GrammarCardType.entries.map { it.id },
    val hidden: Set<String> = emptySet()
) {

    /** Cards in display order, minus hidden ones. */
    fun visibleCards(): List<GrammarCardType> =
        order.mapNotNull { id -> GrammarCardType.byId(id) }
            .filter { it.id !in hidden }

    fun isVisible(type: GrammarCardType): Boolean = type.id !in hidden

    fun setVisible(type: GrammarCardType, visible: Boolean): GrammarCardLayout =
        copy(hidden = if (visible) hidden - type.id else hidden + type.id)

    fun moveUp(type: GrammarCardType): GrammarCardLayout {
        val index = order.indexOf(type.id)
        if (index <= 0) return this
        val list = order.toMutableList()
        list.removeAt(index)
        list.add(index - 1, type.id)
        return copy(order = list)
    }

    fun moveDown(type: GrammarCardType): GrammarCardLayout {
        val index = order.indexOf(type.id)
        if (index < 0 || index >= order.lastIndex) return this
        val list = order.toMutableList()
        list.removeAt(index)
        list.add(index + 1, type.id)
        return copy(order = list)
    }

    /** Drops any unknown ids (stale layouts from older builds). */
    fun sanitized(): GrammarCardLayout = GrammarCardLayout(
        order = order.filter { GrammarCardType.byId(it) != null },
        hidden = hidden.filter { GrammarCardType.byId(it) != null }.toSet()
    )
}

/** Preset card layouts for different learner profiles. */
object GrammarCardPresets {

    val Minimal = GrammarCardLayout(
        order = GrammarCardType.entries.map { it.id },
        hidden = setOf("structure", "jlpt", "related", "kanji", "study")
    )

    val Beginner = GrammarCardLayout(
        order = GrammarCardType.entries.map { it.id },
        hidden = setOf("jlpt", "related", "study")
    )

    val Standard = GrammarCardLayout(
        order = GrammarCardType.entries.map { it.id },
        hidden = setOf("study")
    )

    val Advanced = GrammarCardLayout(
        order = GrammarCardType.entries.map { it.id },
        hidden = emptySet()
    )

    val Intermediate = GrammarCardLayout(
        order = GrammarCardType.entries.map { it.id },
        hidden = setOf("related", "study")
    )

    val Writing = GrammarCardLayout(
        order = GrammarCardType.entries.map { it.id },
        hidden = setOf("jlpt", "related", "kanji", "study")
    )

    val Reading = GrammarCardLayout(
        order = GrammarCardType.entries.map { it.id },
        hidden = setOf("structure", "jlpt", "related", "study")
    )

    val Dictionary = GrammarCardLayout(
        order = GrammarCardType.entries.map { it.id },
        hidden = setOf("study")
    )

    val Research = GrammarCardLayout(
        order = GrammarCardType.entries.map { it.id },
        hidden = emptySet()
    )

    val all: List<GrammarCardPreset> = listOf(
        GrammarCardPreset("minimal", "Minimal", "Pattern, meaning, and examples only", Minimal),
        GrammarCardPreset("beginner", "Beginner", "Core info with structure breakdown", Beginner),
        GrammarCardPreset("standard", "Standard", "Full analysis without study overlay", Standard),
        GrammarCardPreset("intermediate", "Intermediate", "Full analysis without related-pattern noise", Intermediate),
        GrammarCardPreset("advanced", "Advanced", "Everything including study actions", Advanced),
        GrammarCardPreset("writing", "Writing", "Pattern, meaning, structure, examples", Writing),
        GrammarCardPreset("reading", "Reading", "Pattern, meaning, examples, kanji", Reading),
        GrammarCardPreset("dictionary", "Dictionary", "Every grammar card, no study state", Dictionary),
        GrammarCardPreset("research", "Research", "Everything visible for deep analysis", Research)
    )

    fun byId(id: String): GrammarCardPreset? = all.firstOrNull { it.id == id }
}

data class GrammarCardPreset(
    val id: String,
    val name: String,
    val description: String,
    val layout: GrammarCardLayout
)

/**
 * JSON persistence for the grammar-page layout. Falls back to defaults on
 * corrupt or stale blobs — a hand-edited preference can never crash layout.
 */
class GrammarCardLayoutStore(
    private val preferences: PreferencesContract.AppPreferences
) {

    suspend fun load(): GrammarCardLayout {
        val raw = preferences.grammarCardLayoutJson.get()
        if (raw.isBlank()) return GrammarCardLayout()
        return runCatching {
            Json.decodeFromString<GrammarCardLayout>(raw).sanitized()
        }.getOrDefault(GrammarCardLayout())
    }

    suspend fun save(layout: GrammarCardLayout) {
        preferences.grammarCardLayoutJson.set(
            Json.encodeToString(layout.sanitized())
        )
    }

    suspend fun reset() {
        preferences.grammarCardLayoutJson.set("")
    }
}
