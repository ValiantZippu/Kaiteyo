package ua.syt0r.kanji.desktop.engine.dictionary

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock

// ============================================
// KAITEYO DICTIONARY ENGINE �?" MODELS
// A native, Yomitan-compatible dictionary
// engine. Everything here is pure, serializable
// and platform-neutral so it can persist and be
// shared across screens, the popup and the API.
// ============================================

/** A single sense/gloss of a dictionary entry. */
@Serializable
data class DictionarySense(
    val partOfSpeech: List<String> = emptyList(),
    val glosses: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val restrictions: List<String> = emptyList(),
    /** Cross-reference to related entries (kanji or vocabulary). */
    val crossReferences: List<String> = emptyList()
) {
    val primaryGloss: String get() = glosses.firstOrNull() ?: ""
}

/** A reading with optional pitch-accent data (Yomitan style). */
@Serializable
data class DictionaryReading(
    val reading: String,
    val elements: List<String> = emptyList(),
    val pitchAccents: List<PitchAccent> = emptyList(),
    val readingInformation: List<String> = emptyList(),
    val frequency: Double? = null,
    val valueTags: List<String> = emptyList()
)

/** A single pitch-accent position marker. */
@Serializable
data class PitchAccent(
    val position: Int,
    val downstep: Int? = null
)

/** Frequency info attached to a term. */
@Serializable
data class FrequencyInfo(
    val rank: Int? = null,
    val score: Double? = null
)

/** A single dictionary entry keyed by its spelling. */
@Serializable
data class DictionaryEntry(
    val headword: String,
    val spellings: List<String> = emptyList(),
    val readings: List<DictionaryReading> = emptyList(),
    val senses: List<DictionarySense> = emptyList(),
    val kanjiSpellings: List<KanjiSpelling> = emptyList(),
    val frequency: FrequencyInfo = FrequencyInfo(),
    val searchKeys: List<String> = emptyList(),
    val dictionaryId: String = "",
    val source: DictionaryEntryType = DictionaryEntryType.Vocabulary
)

/** Supplementary kanji info attached to a term (from KANJIDIC). */
@Serializable
data class KanjiSpelling(
    val character: String,
    val onReadings: List<String> = emptyList(),
    val kunReadings: List<String> = emptyList(),
    val meanings: List<String> = emptyList(),
    val strokeCounts: List<Int> = emptyList(),
    val jlpt: Int? = null,
    val grade: Int? = null,
    val frequency: Int? = null,
    val radicals: List<String> = emptyList()
)

/** What kind of entry a result represents. */
@Serializable
enum class DictionaryEntryType { Vocabulary, Kanji, Grammar, Name, Expression }

/** A dictionary that has been installed into the engine. */
@Serializable
data class InstalledDictionary(
    val id: String,
    val name: String,
    val revision: String = "",
    val authoredBy: String = "",
    val format: DictionaryFormat = DictionaryFormat.Yomitan,
    val sourceLanguage: String = "ja",
    val targetLanguage: String = "en",
    val enabled: Boolean = true,
    val priority: Int = 0,
    val createdAt: Instant = Instant.fromEpochMilliseconds(0),
    val entryCount: Long = 0,
    val tags: List<String> = emptyList()
) {
    val displayTitle: String get() = name
}

@Serializable
enum class DictionaryFormat { Yomitan, JmDict, KanjiDic, Frequency, PitchAccent, Grammar, Name, Custom }

/** One search hit across all enabled dictionaries. */
data class DictionaryMatch(
    val entry: DictionaryEntry,
    val dictionary: InstalledDictionary,
    val score: Int = 0
)

/** A completed lookup grouped by dictionary so the UI can render cleanly. */
data class DictionaryResultGroup(
    val dictionary: InstalledDictionary,
    val matches: List<DictionaryMatch>
)

/** Golden record that a mined card stores from a lookup. */
@Serializable
data class MinedDictionaryData(
    val headword: String,
    val reading: String = "",
    val definition: String = "",
    val dictionary: String = "",
    val pitchAccent: List<PitchAccent> = emptyList(),
    val frequency: FrequencyInfo = FrequencyInfo(),
    val example: String = ""
)