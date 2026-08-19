package ua.syt0r.kanji.core.knowledge.cards

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract

// ============================================================
// SENTENCE CARD SYSTEM
// ------------------------------------------------------------
// A sentence page is a sequence of modular cards, just like
// the kanji and word pages. Users can show / hide / reorder
// cards and apply presets — the page layout is data
// (SentenceCardLayout), never hardcoded per screen.
//
//   SentenceCardType     the registry of every available card
//   SentenceCardLayout   the user's per-profile layout (persisted)
//   SentenceCardPresets  Beginner / Standard / Advanced / Research
//   SentenceCardLayoutStore  JSON persistence
// ============================================================

/** Every card a sentence page can show. */
enum class SentenceCardType(
    val id: String,
    val title: String,
    val description: String
) {
    Hero("hero", "Hero", "The sentence itself with furigana"),
    Translation("translation", "Translation", "English translation and notes"),
    Tokens("tokens", "Tokens", "Interactive token breakdown with readings"),
    Grammar("grammar", "Grammar", "Grammar patterns found in this sentence"),
    Vocabulary("vocabulary", "Vocabulary", "Key words used in this sentence"),
    Difficulty("difficulty", "Difficulty", "Sentence difficulty analysis"),
    Source("source", "Source", "Corpus source and provenance"),
    Study("study", "Study", "Study-state overview and actions");

    companion object {
        fun byId(id: String): SentenceCardType? = entries.firstOrNull { it.id == id }
    }
}

/**
 * The user's sentence-page layout: display order + hidden cards.
 * Serialized as JSON and persisted per profile.
 */
@Serializable
data class SentenceCardLayout(
    val order: List<String> = SentenceCardType.entries.map { it.id },
    val hidden: Set<String> = emptySet()
) {

    /** Cards in display order, minus hidden ones. */
    fun visibleCards(): List<SentenceCardType> =
        order.mapNotNull { id -> SentenceCardType.byId(id) }
            .filter { it.id !in hidden }

    fun isVisible(type: SentenceCardType): Boolean = type.id !in hidden

    fun setVisible(type: SentenceCardType, visible: Boolean): SentenceCardLayout =
        copy(hidden = if (visible) hidden - type.id else hidden + type.id)

    fun moveUp(type: SentenceCardType): SentenceCardLayout {
        val index = order.indexOf(type.id)
        if (index <= 0) return this
        val list = order.toMutableList()
        list.removeAt(index)
        list.add(index - 1, type.id)
        return copy(order = list)
    }

    fun moveDown(type: SentenceCardType): SentenceCardLayout {
        val index = order.indexOf(type.id)
        if (index < 0 || index >= order.lastIndex) return this
        val list = order.toMutableList()
        list.removeAt(index)
        list.add(index + 1, type.id)
        return copy(order = list)
    }

    /** Drops any unknown ids (stale layouts from older builds). */
    fun sanitized(): SentenceCardLayout = SentenceCardLayout(
        order = order.filter { SentenceCardType.byId(it) != null },
        hidden = hidden.filter { SentenceCardType.byId(it) != null }.toSet()
    )
}

/** Preset card layouts for different learner profiles. */
object SentenceCardPresets {

    val Minimal = SentenceCardLayout(
        order = SentenceCardType.entries.map { it.id },
        hidden = setOf("tokens", "grammar", "difficulty", "source", "study")
    )

    val Beginner = SentenceCardLayout(
        order = SentenceCardType.entries.map { it.id },
        hidden = setOf("difficulty", "source", "study")
    )

    val Standard = SentenceCardLayout(
        order = SentenceCardType.entries.map { it.id },
        hidden = setOf("source")
    )

    val Advanced = SentenceCardLayout(
        order = SentenceCardType.entries.map { it.id },
        hidden = emptySet()
    )

    val Intermediate = SentenceCardLayout(
        order = SentenceCardType.entries.map { it.id },
        hidden = setOf("difficulty", "study")
    )

    val Writing = SentenceCardLayout(
        order = SentenceCardType.entries.map { it.id },
        hidden = setOf("difficulty", "source", "study")
    )

    val Reading = SentenceCardLayout(
        order = SentenceCardType.entries.map { it.id },
        hidden = setOf("source", "study")
    )

    val Dictionary = SentenceCardLayout(
        order = SentenceCardType.entries.map { it.id },
        hidden = setOf("study")
    )

    val Research = SentenceCardLayout(
        order = SentenceCardType.entries.map { it.id },
        hidden = emptySet()
    )

    val all: List<SentenceCardPreset> = listOf(
        SentenceCardPreset("minimal", "Minimal", "Sentence, translation, and vocabulary only", Minimal),
        SentenceCardPreset("beginner", "Beginner", "Token breakdown and grammar", Beginner),
        SentenceCardPreset("standard", "Standard", "Full analysis without source details", Standard),
        SentenceCardPreset("intermediate", "Intermediate", "Full analysis without difficulty or study", Intermediate),
        SentenceCardPreset("advanced", "Advanced", "Everything including source and study", Advanced),
        SentenceCardPreset("writing", "Writing", "Sentence, translation, tokens, vocabulary", Writing),
        SentenceCardPreset("reading", "Reading", "Analysis focus: tokens, grammar, difficulty", Reading),
        SentenceCardPreset("dictionary", "Dictionary", "All analysis cards, no study state", Dictionary),
        SentenceCardPreset("research", "Research", "Everything visible for deep analysis", Research)
    )

    fun byId(id: String): SentenceCardPreset? = all.firstOrNull { it.id == id }
}

data class SentenceCardPreset(
    val id: String,
    val name: String,
    val description: String,
    val layout: SentenceCardLayout
)

/**
 * JSON persistence for the sentence-page layout. Falls back to defaults on
 * corrupt or stale blobs — a hand-edited preference can never crash layout.
 */
class SentenceCardLayoutStore(
    private val preferences: PreferencesContract.AppPreferences
) {

    suspend fun load(): SentenceCardLayout {
        val raw = preferences.sentenceCardLayoutJson.get()
        if (raw.isBlank()) return SentenceCardLayout()
        return runCatching {
            Json.decodeFromString<SentenceCardLayout>(raw).sanitized()
        }.getOrDefault(SentenceCardLayout())
    }

    suspend fun save(layout: SentenceCardLayout) {
        preferences.sentenceCardLayoutJson.set(
            Json.encodeToString(layout.sanitized())
        )
    }

    suspend fun reset() {
        preferences.sentenceCardLayoutJson.set("")
    }
}
