package io.kaiteyo.kjd.source

import io.kaiteyo.kjd.model.SourceRef
import kotlinx.serialization.Serializable

/**
 * First-class source metadata. Every external dataset used by the generator is
 * described by a [SourceMetadata] record; generated releases embed the full
 * list so provenance is machine-readable and human-readable.
 */
@Serializable
data class SourceMetadata(
    /** Stable id, e.g. "kanjivg", "kanjidic", "jmdict". */
    val id: String,
    val name: String,
    val homepage: String,
    val license: License,
    /** Version/date of the snapshot that was ingested. */
    val version: String,
    /** ISO-8601 timestamp when the data was retrieved. */
    val retrievedAt: String,
    val attribution: String,
    val redistributionNotes: String = "",
    val modificationNotes: String = "",
    /** Canonical URL of the exact artifact consumed by the generator. */
    val sourceUrl: String = ""
)

/** License description for a data source. */
@Serializable
data class License(
    val id: String,
    val name: String,
    val url: String = "",
    /** Whether the license permits redistribution in derived works. */
    val allowsRedistribution: Boolean = false,
    val attributionRequired: Boolean = false,
    val shareAlike: Boolean = false
)

/**
 * One consumed artifact of a source (e.g. a single KanjiVG zip or a JMdict
 * XML file). Raw inputs are stored under `sources/<id>/raw/` and never
 * mutated; metadata lives under `sources/<id>/metadata/`.
 */
@Serializable
data class SourceArtifact(
    val sourceId: String,
    val fileName: String,
    val sha256: String = "",
    val byteSize: Long = 0,
    val recordCount: Long = 0,
    /** Records parsed / rejected counts for the quality report. */
    val parsedCount: Long = 0,
    val rejectedCount: Long = 0
)

/**
 * A machine + human readable third-party attribution manifest, generated for
 * every release under `third_party/THIRD_PARTY_DATA.json` / `.md`.
 */
@Serializable
data class AttributionManifest(
    val platform: String,
    val generatedBy: String,
    val generatedAt: String,
    val schemaVersion: Int,
    val sources: List<SourceMetadata>
)

fun SourceMetadata.toSourceRef(recordId: String? = null, transformation: String = "parsed", isCanonical: Boolean = false) =
    SourceRef(
        sourceId = id,
        recordId = recordId,
        transformation = transformation,
        isCanonical = isCanonical
    )

/** Well-known source ids used across the pipeline. */
object SourceIds {
    // Original bundled sources
    const val KANJIVG = "kanjivg"
    const val KANJIDIC = "kanjidic"
    const val JMDICT = "jmdict"
    const val JMDICT_FURIGANA = "jmdict-furigana"
    const val TANOS_JLPT = "tanos-jlpt"
    const val LEEDS_FREQUENCY = "leeds-frequency"
    const val YOMICHAN_JLPT_VOCAB = "yomichan-jlpt-vocab"
    const val TATOEBA = "tatoeba"
    // Frequency sources
    const val NETFLIX_FREQUENCY = "netflix-frequency"
    const val KEMPSON_FREQUENCY = "kempson-frequency"
    const val KANDRAC_2242 = "kandrac-2242"
    const val NUKEMARINE_RTK = "nukemarine-rtk"
    const val YATSKOV_WIKIPEDIA = "yatskov-wikipedia"
    const val GIRARDI_WORD_FREQ = "girardi-word-freq"
    const val SHPIKA_KANJI_KEYS = "shpika-kanji-keys"
    const val TOPOKANJI = "topokanji"
    // Structural data
    const val CJK_DECOMPOSITIONS = "cjk-decompositions"
    const val BUNKACHO = "bunkacho"
    const val KANJIDATABASE = "kanjidatabase"
    const val DAVID_GOUVEIA = "david-gouveia"
    // Learning metadata
    const val USAGI_CHAN_PHONETICS = "usagi-chan-phonetics"
    const val SHIRABE_JLPT = "shirabe-jlpt"
    const val SHIRABE_COMMON = "shirabe-common"
    const val KANJI_API = "kanji-api"
    const val KANJI_SCHOOL = "kanji-school"
}
