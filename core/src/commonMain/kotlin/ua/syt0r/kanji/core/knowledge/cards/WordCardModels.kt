package ua.syt0r.kanji.core.knowledge.cards

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract

// ============================================================
// WORD CARD SYSTEM
// ------------------------------------------------------------
// A word page is a sequence of modular cards, just like the
// kanji page. Users can show / hide / reorder cards and apply
// presets — the page layout is data (WordCardLayout), never
// hardcoded per screen.
//
//   WordCardType      the registry of every available card
//   WordCardLayout    the user's per-profile layout (persisted)
//   WordCardPresets   Beginner / Standard / Advanced / Research
//   WordCardLayoutStore  JSON persistence
// ============================================================

/** Every card a word page can show. */
enum class WordCardType(
    val id: String,
    val title: String,
    val description: String
) {
    Hero("hero", "Hero", "The word itself with reading and glossary"),
    Readings("readings", "Readings", "Kanji and kana readings with furigana"),
    Meanings("meanings", "Meanings", "Full glossary with parts of speech"),
    PartOfSpeech("pos", "Part of speech", "Grammatical classification tags"),
    Kanji("kanji", "Kanji", "Characters used in this word"),
    Frequency("frequency", "Frequency", "Word frequency where available"),
    Media("media", "Media", "Where this word appears in your media library"),
    Sentences("sentences", "Sentences", "Example sentences from the corpus"),
    Grammar("grammar", "Grammar", "Patterns found in example sentences"),
    Study("study", "Study", "Study-state overview and actions");

    companion object {
        fun byId(id: String): WordCardType? = entries.firstOrNull { it.id == id }
    }
}

/**
 * The user's word-page layout: display order + hidden cards.
 * Serialized as JSON and persisted per profile.
 */
@Serializable
data class WordCardLayout(
    val order: List<String> = WordCardType.entries.map { it.id },
    val hidden: Set<String> = emptySet()
) {

    /**
     * Cards in display order, minus hidden ones. Registry card types missing
     * from the saved order are appended (so a card added in a new build shows
     * up for users with a saved layout instead of silently vanishing); the
     * stored [order] itself is never mutated.
     */
    fun visibleCards(): List<WordCardType> {
        val ordered = order.mapNotNull { id -> WordCardType.byId(id) }
        val orderedIds = ordered.mapTo(mutableSetOf()) { it.id }
        val appended = WordCardType.entries.filter { it.id !in orderedIds }
        return (ordered + appended).filter { it.id !in hidden }
    }

    fun isVisible(type: WordCardType): Boolean = type.id !in hidden

    fun setVisible(type: WordCardType, visible: Boolean): WordCardLayout =
        copy(hidden = if (visible) hidden - type.id else hidden + type.id)

    fun moveUp(type: WordCardType): WordCardLayout {
        val index = order.indexOf(type.id)
        if (index <= 0) return this
        val list = order.toMutableList()
        list.removeAt(index)
        list.add(index - 1, type.id)
        return copy(order = list)
    }

    fun moveDown(type: WordCardType): WordCardLayout {
        val index = order.indexOf(type.id)
        if (index < 0 || index >= order.lastIndex) return this
        val list = order.toMutableList()
        list.removeAt(index)
        list.add(index + 1, type.id)
        return copy(order = list)
    }

    /** Drops any unknown ids (stale layouts from older builds). */
    fun sanitized(): WordCardLayout = WordCardLayout(
        order = order.filter { WordCardType.byId(it) != null },
        hidden = hidden.filter { WordCardType.byId(it) != null }.toSet()
    )
}

/** Preset card layouts for different learner profiles. */
object WordCardPresets {

    val Minimal = WordCardLayout(
        order = WordCardType.entries.map { it.id },
        hidden = setOf("pos", "frequency", "grammar", "study")
    )

    val Beginner = WordCardLayout(
        order = WordCardType.entries.map { it.id },
        hidden = setOf("frequency", "grammar", "study")
    )

    val Standard = WordCardLayout(
        order = WordCardType.entries.map { it.id },
        hidden = setOf("grammar")
    )

    val Advanced = WordCardLayout(
        order = WordCardType.entries.map { it.id },
        hidden = emptySet()
    )

    val Intermediate = WordCardLayout(
        order = WordCardType.entries.map { it.id },
        hidden = setOf("study")
    )

    val Writing = WordCardLayout(
        order = WordCardType.entries.map { it.id },
        hidden = setOf("frequency", "media", "grammar", "study")
    )

    val Reading = WordCardLayout(
        order = WordCardType.entries.map { it.id },
        hidden = setOf("frequency", "media", "study")
    )

    val Dictionary = WordCardLayout(
        order = WordCardType.entries.map { it.id },
        hidden = setOf("media", "study")
    )

    val Research = WordCardLayout(
        order = WordCardType.entries.map { it.id },
        hidden = emptySet()
    )

    val all: List<WordCardPreset> = listOf(
        WordCardPreset("minimal", "Minimal", "Meaning and reading only", Minimal),
        WordCardPreset("beginner", "Beginner", "Core info without frequency depth", Beginner),
        WordCardPreset("standard", "Standard", "Meaning, readings, kanji, sentences", Standard),
        WordCardPreset("intermediate", "Intermediate", "Full lexical data without study overlay", Intermediate),
        WordCardPreset("advanced", "Advanced", "Everything including grammar and study", Advanced),
        WordCardPreset("writing", "Writing", "Spelling-focused: readings, kanji, sentences", Writing),
        WordCardPreset("reading", "Reading", "Reading-focused: grammar and sentences, no media", Reading),
        WordCardPreset("dictionary", "Dictionary", "Lexical data without personal media or study", Dictionary),
        WordCardPreset("research", "Research", "Everything visible", Research)
    )

    fun byId(id: String): WordCardPreset? = all.firstOrNull { it.id == id }
}

data class WordCardPreset(
    val id: String,
    val name: String,
    val description: String,
    val layout: WordCardLayout
)

/**
 * JSON persistence for the word-page layout. Falls back to defaults on
 * corrupt or stale blobs — a hand-edited preference can never crash layout.
 */
class WordCardLayoutStore(
    private val preferences: PreferencesContract.AppPreferences
) {

    suspend fun load(): WordCardLayout {
        val raw = preferences.wordCardLayoutJson.get()
        if (raw.isBlank()) return WordCardLayout()
        return runCatching {
            Json.decodeFromString<WordCardLayout>(raw).sanitized()
        }.getOrDefault(WordCardLayout())
    }

    suspend fun save(layout: WordCardLayout) {
        preferences.wordCardLayoutJson.set(
            Json.encodeToString(layout.sanitized())
        )
    }

    suspend fun reset() {
        preferences.wordCardLayoutJson.set("")
    }
}
