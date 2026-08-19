package ua.syt0r.kanji.core.knowledge.cards

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract

// ============================================================
// COLLECTION CARD SYSTEM
// ------------------------------------------------------------
// A collection page (JLPT level, school grade, custom list) is
// a sequence of modular cards, just like the kanji, word,
// sentence, and grammar pages. Users can show / hide / reorder
// cards and apply presets.
//
//   CollectionCardType     the registry of every available card
//   CollectionCardLayout   the user's per-profile layout (persisted)
//   CollectionCardPresets  Beginner / Standard / Advanced / Research
//   CollectionCardLayoutStore  JSON persistence
// ============================================================

/** Every card a collection page can show. */
enum class CollectionCardType(
    val id: String,
    val title: String,
    val description: String
) {
    Hero("hero", "Hero", "Collection name, description, and stats"),
    KanjiGrid("kanji_grid", "Kanji grid", "Grid of all kanji in this collection"),
    KanjiList("kanji_list", "Kanji list", "Detailed list of kanji with readings"),
    FrequencyDistribution("frequency", "Frequency", "Frequency distribution of kanji in this collection"),
    JLPTBreakdown("jlpt", "JLPT", "JLPT level breakdown within this collection"),
    StudyState("study", "Study state", "Study progress overview for this collection"),
    Statistics("stats", "Statistics", "Stroke count distribution and other stats");

    companion object {
        fun byId(id: String): CollectionCardType? = entries.firstOrNull { it.id == id }
    }
}

/**
 * The user's collection-page layout: display order + hidden cards.
 * Serialized as JSON and persisted per profile.
 */
@Serializable
data class CollectionCardLayout(
    val order: List<String> = CollectionCardType.entries.map { it.id },
    val hidden: Set<String> = emptySet()
) {

    /** Cards in display order, minus hidden ones. */
    fun visibleCards(): List<CollectionCardType> =
        order.mapNotNull { id -> CollectionCardType.byId(id) }
            .filter { it.id !in hidden }

    fun isVisible(type: CollectionCardType): Boolean = type.id !in hidden

    fun setVisible(type: CollectionCardType, visible: Boolean): CollectionCardLayout =
        copy(hidden = if (visible) hidden - type.id else hidden + type.id)

    fun moveUp(type: CollectionCardType): CollectionCardLayout {
        val index = order.indexOf(type.id)
        if (index <= 0) return this
        val list = order.toMutableList()
        list.removeAt(index)
        list.add(index - 1, type.id)
        return copy(order = list)
    }

    fun moveDown(type: CollectionCardType): CollectionCardLayout {
        val index = order.indexOf(type.id)
        if (index < 0 || index >= order.lastIndex) return this
        val list = order.toMutableList()
        list.removeAt(index)
        list.add(index + 1, type.id)
        return copy(order = list)
    }

    /** Drops any unknown ids (stale layouts from older builds). */
    fun sanitized(): CollectionCardLayout = CollectionCardLayout(
        order = order.filter { CollectionCardType.byId(it) != null },
        hidden = hidden.filter { CollectionCardType.byId(it) != null }.toSet()
    )
}

/** Preset card layouts for different learner profiles. */
object CollectionCardPresets {

    val Minimal = CollectionCardLayout(
        order = CollectionCardType.entries.map { it.id },
        hidden = setOf("frequency", "jlpt", "study", "stats")
    )

    val Beginner = CollectionCardLayout(
        order = CollectionCardType.entries.map { it.id },
        hidden = setOf("frequency", "jlpt", "stats")
    )

    val Standard = CollectionCardLayout(
        order = CollectionCardType.entries.map { it.id },
        hidden = setOf("stats")
    )

    val Advanced = CollectionCardLayout(
        order = CollectionCardType.entries.map { it.id },
        hidden = emptySet()
    )

    val Research = CollectionCardLayout(
        order = CollectionCardType.entries.map { it.id },
        hidden = emptySet()
    )

    val all: List<CollectionCardPreset> = listOf(
        CollectionCardPreset("minimal", "Minimal", "Hero and kanji grid only", Minimal),
        CollectionCardPreset("beginner", "Beginner", "Core info with study state", Beginner),
        CollectionCardPreset("standard", "Standard", "Full analysis without stats", Standard),
        CollectionCardPreset("advanced", "Advanced", "Everything visible", Advanced),
        CollectionCardPreset("research", "Research", "Everything visible for deep analysis", Research)
    )

    fun byId(id: String): CollectionCardPreset? = all.firstOrNull { it.id == id }
}

data class CollectionCardPreset(
    val id: String,
    val name: String,
    val description: String,
    val layout: CollectionCardLayout
)

/**
 * JSON persistence for the collection-page layout. Falls back to defaults on
 * corrupt or stale blobs — a hand-edited preference can never crash layout.
 */
class CollectionCardLayoutStore(
    private val preferences: PreferencesContract.AppPreferences
) {

    suspend fun load(): CollectionCardLayout {
        val raw = preferences.collectionCardLayoutJson.get()
        if (raw.isBlank()) return CollectionCardLayout()
        return runCatching {
            Json.decodeFromString<CollectionCardLayout>(raw).sanitized()
        }.getOrDefault(CollectionCardLayout())
    }

    suspend fun save(layout: CollectionCardLayout) {
        preferences.collectionCardLayoutJson.set(
            Json.encodeToString(layout.sanitized())
        )
    }

    suspend fun reset() {
        preferences.collectionCardLayoutJson.set("")
    }
}
