package io.kaiteyo.kjd.model

import kotlinx.serialization.Serializable

/**
 * Stable identifier for a record inside the canonical database.
 *
 * IDs are never derived from display strings alone (those can change across
 * source revisions); they are assigned during entity resolution so that the
 * same logical entity always maps to the same ID within one generated release.
 */
@Serializable
data class EntityId(
    /** Stable string id, e.g. "kanji:食", "vocab:jmdict_1000990", "radical:29". */
    val value: String
) {
    override fun toString(): String = value
}

/** Broad category of a canonical entity. Used by search results and links. */
@Serializable
enum class EntityType {
    Kanji,
    Kana,
    Radical,
    Component,
    Vocabulary,
    Sense,
    ExampleSentence,
    GrammarPoint
}

/**
 * A reference to the external source a field or entity originated from.
 *
 * Every imported fact carries a [SourceRef] so the platform can always answer
 * "where did this come from?" — including the exact record within the source
 * and the transformation that produced the canonical value.
 */
@Serializable
data class SourceRef(
    val sourceId: String,
    /** Original record id inside the source (e.g. KanjiVG "kvg:kanji_食"). */
    val recordId: String? = null,
    /** Human description of the transformation performed (e.g. "parsed", "merged", "transliterated"). */
    val transformation: String = "parsed",
    /** True when this value was chosen as canonical among conflicting sources. */
    val isCanonical: Boolean = false
)

/**
 * A reading attached to a character or vocabulary entry.
 *
 * [type] distinguishes on'yomi / kun'yomi for kanji ("on" / "kun"), and is
 * null for kana-only vocabulary readings where the distinction does not apply.
 */
@Serializable
data class Reading(
    val value: String,
    val type: String? = null,
    val source: List<SourceRef> = emptyList()
)

/** A meaning/gloss. [language] is an ISO 639-1 code ("en", "de", "es", ...). */
@Serializable
data class Meaning(
    val value: String,
    val language: String = "en",
    val source: List<SourceRef> = emptyList()
)

/**
 * One Unicode character (any script). This is the most general character
 * entity; [Kanji] and [KanaCharacter] specialize it.
 */
@Serializable
data class Character(
    val id: EntityId,
    /** The literal character, exactly one Unicode codepoint (possibly a surrogate pair). */
    val literal: String,
    val codepoint: Int,
    val normalized: String,
    val characterType: CharacterType,
    val readings: List<Reading> = emptyList(),
    val meanings: List<Meaning> = emptyList(),
    val strokeCount: Int? = null,
    val radical: String? = null,
    val jlpt: List<JlptClassification> = emptyList(),
    val grade: Int? = null,
    val frequency: List<FrequencyRecord> = emptyList(),
    val sources: List<SourceRef> = emptyList()
)

@Serializable
enum class CharacterType {
    Kanji,
    Kana,
    Hiragana,
    Katakana,
    Other
}

/** A kanji character with its full linguistic profile. */
@Serializable
data class Kanji(
    val id: EntityId,
    val character: Character,
    val onReadings: List<Reading> = emptyList(),
    val kunReadings: List<Reading> = emptyList(),
    val meanings: List<Meaning> = emptyList(),
    val grade: Int? = null,
    val jlpt: List<JlptClassification> = emptyList(),
    val frequency: List<FrequencyRecord> = emptyList(),
    val strokeCount: Int? = null,
    /** Radical reference (e.g. Kangxi radical number as string). */
    val radical: String? = null,
    val components: List<Component> = emptyList(),
    val strokes: List<Stroke> = emptyList(),
    /** Vocabulary entries containing this kanji (resolved during linking). */
    val vocabularyIds: List<EntityId> = emptyList(),
    val sources: List<SourceRef> = emptyList()
)

/** Kana character (hiragana or katakana). */
@Serializable
data class KanaCharacter(
    val id: EntityId,
    val character: Character,
    val syllabary: Syllabary,
    val romaji: String? = null,
    val strokeCount: Int? = null,
    val strokes: List<Stroke> = emptyList(),
    val sources: List<SourceRef> = emptyList()
)

@Serializable
enum class Syllabary {
    Hiragana,
    Katakana
}

/**
 * A radical. Radicals are represented independently from kanji so they can be
 * studied and linked on their own.
 */
@Serializable
data class Radical(
    val id: EntityId,
    /** Kangxi radical number (1..214) where applicable. */
    val number: Int? = null,
    val character: String? = null,
    val name: String? = null,
    val meanings: List<Meaning> = emptyList(),
    val strokeCount: Int? = null,
    val unicodeCodepoint: Int? = null,
    val kanjiIds: List<EntityId> = emptyList(),
    val sources: List<SourceRef> = emptyList()
)

/**
 * A graphical/phonetic/semantic component of a kanji. Distinct from the
 * radical: a character can have many components with different roles.
 */
@Serializable
data class Component(
    val id: EntityId,
    val character: String,
    /** Role: "radical", "graphical", "phonetic", "semantic" or a custom value. */
    val role: String = "graphical",
    val strokeCount: Int? = null,
    val sources: List<SourceRef> = emptyList()
)

/** One stroke of a character with structured geometry. */
@Serializable
data class Stroke(
    val id: EntityId,
    /** 1-based stroke index within the character. */
    val index: Int,
    val characterId: EntityId,
    /** Path geometry in SVG path `d` syntax, in a 1092x1092 coordinate space (KanjiVG convention). */
    val path: String,
    val pathType: StrokePathType = StrokePathType.KanjiVg,
    /** Approximate direction of the stroke, derived from the path if available. */
    val direction: StrokeDirection? = null,
    val boundingBox: BoundingBox? = null,
    val sources: List<SourceRef> = emptyList()
)

@Serializable
enum class StrokePathType {
    KanjiVg,
    Svg,
    Polyline
}

@Serializable
enum class StrokeDirection {
    TopToBottom,
    BottomToTop,
    LeftToRight,
    RightToLeft,
    DiagonalDownRight,
    DiagonalDownLeft,
    DiagonalUpRight,
    DiagonalUpLeft,
    Unknown
}

@Serializable
data class BoundingBox(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
)

/**
 * JLPT classification. Modeled as a classification sourced from a dataset,
 * NOT as an intrinsic property — see the platform philosophy: the source of a
 * classification must remain distinguishable.
 */
@Serializable
data class JlptClassification(
    val level: Int,
    val source: SourceRef
)

/**
 * Frequency record preserving source context. The raw meaning of the value
 * depends on the source (rank vs count vs corpus position); that methodology
 * context is kept in the source metadata and in [corpus]/[methodology].
 */
@Serializable
data class FrequencyRecord(
    val value: Int,
    val source: SourceRef,
    /** Corpus/methodology label, e.g. "Leeds Internet corpus 1-billion words". */
    val methodology: String? = null
)

/**
 * One vocabulary entry (a lexeme as represented in JMdict/other sources).
 * Never assume a single reading per expression: [readings] may contain many.
 */
@Serializable
data class VocabularyEntry(
    val id: EntityId,
    val expression: String,
    val readings: List<VocabularyReading> = emptyList(),
    val senses: List<Sense> = emptyList(),
    /** Kanji composing the expression, resolved to canonical ids. */
    val kanjiIds: List<EntityId> = emptyList(),
    val furigana: List<FuriganaSegment> = emptyList(),
    val jlpt: List<JlptClassification> = emptyList(),
    val frequency: List<FrequencyRecord> = emptyList(),
    val partsOfSpeech: List<PartOfSpeech> = emptyList(),
    val sources: List<SourceRef> = emptyList()
)

@Serializable
data class VocabularyReading(
    val value: String,
    /** True when the reading is kana-only (no kanji), useful for kana search. */
    val isKanaOnly: Boolean = false,
    val noKanji: Boolean = false,
    /** Set of source-refs per reading so multiple readings stay attributable. */
    val source: List<SourceRef> = emptyList()
)

/**
 * One sense of a vocabulary entry. Senses are modeled independently so a
 * single entry can carry multiple senses with per-sense glosses, POS, field
 * and restriction data.
 */
@Serializable
data class Sense(
    val id: EntityId,
    val vocabularyId: EntityId,
    val index: Int,
    val glosses: List<Meaning> = emptyList(),
    val partsOfSpeech: List<PartOfSpeech> = emptyList(),
    val fields: List<String> = emptyList(),
    val misc: List<String> = emptyList(),
    val restrictions: List<String> = emptyList(),
    val sources: List<SourceRef> = emptyList()
)

@Serializable
data class PartOfSpeech(
    val value: String,
    val source: List<SourceRef> = emptyList()
)

/**
 * A furigana annotation segment. Segments partition the expression; segments
 * with a non-null [reading] are the kanji that carry a reading.
 */
@Serializable
data class FuriganaSegment(
    val text: String,
    val reading: String? = null
)

/**
 * An example sentence. Modeled separately because the availability and
 * licensing of example data varies by dataset.
 */
@Serializable
data class ExampleSentence(
    val id: EntityId,
    val text: String,
    val translation: String? = null,
    val language: String = "ja",
    val sources: List<SourceRef> = emptyList()
)

/**
 * A directed relationship between two canonical entities.
 *
 * [relationType] uses values like "contains", "composes", "radical_of",
 * "component_of", "vocabulary_of", "example_for", ...
 */
@Serializable
data class Relationship(
    val id: EntityId,
    val from: EntityId,
    val to: EntityId,
    val relationType: String,
    val source: List<SourceRef> = emptyList()
)

/** A tag/classification attached to an entity (e.g. "archaic", "news"). */
@Serializable
data class Tag(
    val id: EntityId,
    val name: String,
    val entityId: EntityId? = null,
    val source: List<SourceRef> = emptyList()
)
