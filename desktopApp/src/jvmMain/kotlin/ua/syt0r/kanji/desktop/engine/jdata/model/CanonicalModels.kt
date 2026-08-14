package ua.syt0r.kanji.desktop.engine.jdata.model

import kotlinx.serialization.Serializable
import ua.syt0r.kanji.desktop.engine.jdata.source.SourceDefinition

// ============================================================
// CANONICAL LANGUAGE MODELS
// The platform's normalized, source-agnostic representation of
// Japanese language data. These models are deliberately free of
// Compose, free of Kaiteyo study state and free of any single
// source format (KanjiVG / KANJIDIC / JMdict / Yomitan are all
// adapters into these shapes). Every entity carries a stable ID
// and explicit provenance where the source provides it.
// ============================================================

enum class EntityType { KANJI, KANA, VOCAB, RADICAL, COMPONENT, STROKE_SET, READING, SENSE, FREQUENCY, JLPT, RELATION, SOURCE }

enum class KanaScript { HIRAGANA, KATAKANA }

enum class ComponentKind { RADICAL, SEMANTIC, PHONETIC, DECOMPOSITION }

/** A reference into a source dataset (never mutated; provenance first-class). */
@Serializable
data class SourceRef(
    val sourceId: String,
    val recordKey: String = "",
    val retrievedAt: String = ""
)

/** One reading of a vocabulary entry. */
@Serializable
data class ReadingInfo(
    val kana: String,
    val restrictions: List<String> = emptyList(),
    val pitchAccents: List<PitchMarker> = emptyList()
)

/** A single pitch-accent position marker. */
@Serializable
data class PitchMarker(val position: Int, val downstep: Int? = null)

/** One structured sense — never a giant meaning string. */
@Serializable
data class VocabSense(
    val glosses: List<String>,
    val language: String = "en",
    val partOfSpeech: List<String> = emptyList(),
    val field: List<String> = emptyList(),
    val misc: List<String> = emptyList(),
    val restrictions: List<String> = emptyList(),
    val sourceRefs: List<SourceRef> = emptyList()
) {
    val primaryGloss: String get() = glosses.firstOrNull() ?: ""
}

/** Structural furigana: reading == null for kana runs (no annotation needed). */
@Serializable
data class FuriganaSegment(
    val text: String,
    val reading: String? = null
)

/** A frequency observation with its source context — never compared blindly. */
@Serializable
data class FrequencyValue(
    val source: String,
    val rank: Int? = null,
    val value: Double? = null,
    val scope: String = ""
)

@Serializable
data class KanjiEntry(
    val id: String,
    val character: String,
    val meanings: List<String> = emptyList(),
    val onReadings: List<String> = emptyList(),
    val kunReadings: List<String> = emptyList(),
    val strokeCount: Int? = null,
    val radicalId: String? = null,
    val jlpt: Int? = null,
    val grade: Int? = null,
    val frequencyRank: Int? = null,
    val sources: List<SourceRef> = emptyList()
) {
    val hasMeaningfulData: Boolean get() = meanings.isNotEmpty() || onReadings.isNotEmpty() || kunReadings.isNotEmpty()
}

@Serializable
data class KanaEntry(
    val id: String,
    val character: String,
    val script: KanaScript,
    val reading: String,
    val strokeCount: Int? = null,
    val sources: List<SourceRef> = emptyList()
)

@Serializable
data class VocabEntry(
    val id: String,
    val expression: String,
    val readings: List<ReadingInfo> = emptyList(),
    val senses: List<VocabSense> = emptyList(),
    val furigana: List<FuriganaSegment> = emptyList(),
    val frequencies: List<FrequencyValue> = emptyList(),
    val jlpt: Int? = null,
    val sources: List<SourceRef> = emptyList()
) {
    val primaryReading: String? get() = readings.firstOrNull()?.kana
    val primaryGloss: String get() = senses.firstOrNull()?.primaryGloss ?: ""
    val allGlosses: List<String> get() = senses.flatMap { it.glosses }
    val partOfSpeech: List<String> get() = senses.flatMap { it.partOfSpeech }.distinct()
}

@Serializable
data class RadicalEntry(
    val id: String,
    val character: String,
    val meaning: String? = null,
    val strokeCount: Int? = null,
    val sources: List<SourceRef> = emptyList()
)

@Serializable
data class ComponentEntry(
    val id: String,
    val character: String,
    val kind: ComponentKind,
    val sources: List<SourceRef> = emptyList()
)

/** Axis-aligned bounds of a stroke, derived from its path geometry. */
@Serializable
data class Bounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY
    val centerX: Float get() = (minX + maxX) / 2f
    val centerY: Float get() = (minY + maxY) / 2f
}

/** One stroke: index + SVG path + computed bounds (bounds may be null pre-render). */
@Serializable
data class StrokeEntry(
    val index: Int,
    val path: String? = null,
    val bounds: Bounds? = null
)

/** The full stroke set for one character. */
@Serializable
data class StrokeSet(
    val character: String,
    val strokeCount: Int,
    val strokes: List<StrokeEntry> = emptyList(),
    val source: SourceRef? = null
) {
    val strokeOrder: List<Int> get() = strokes.map { it.index }.ifEmpty { (0 until strokeCount).toList() }
}

/** A directed relationship between two entities, with a stable ID. */
@Serializable
data class RelationEdge(
    val id: String,
    val fromType: EntityType,
    val fromId: String,
    val toType: EntityType,
    val toId: String,
    val kind: String = "related"
)

/**
 * The immutable, fully-resolved dataset a [ua.syt0r.kanji.desktop.engine.jdata.api.LanguageDatabase]
 * is opened on. Immutable maps keyed by stable ID; consumers never see SQLite internals.
 */
class PlatformData(
    val schemaVersion: Int,
    val generatedAt: String,
    val kanji: Map<String, KanjiEntry>,
    val kana: Map<String, KanaEntry>,
    val vocab: Map<String, VocabEntry>,
    val radicals: Map<String, RadicalEntry>,
    val components: Map<String, ComponentEntry>,
    val strokeSets: Map<String, StrokeSet>,
    val relations: List<RelationEdge>,
    val sources: Map<String, SourceDefinition>
) {
    val totalEntries: Int get() = kanji.size + kana.size + vocab.size + radicals.size + components.size + strokeSets.size

    val recordCounts: Map<String, Int> = linkedMapOf(
        "kanji" to kanji.size,
        "kana" to kana.size,
        "vocabulary" to vocab.size,
        "radicals" to radicals.size,
        "components" to components.size,
        "strokeSets" to strokeSets.size,
        "relations" to relations.size,
        "sources" to sources.size
    )
}
