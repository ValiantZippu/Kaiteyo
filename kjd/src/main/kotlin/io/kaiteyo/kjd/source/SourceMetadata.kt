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
    const val KANJIVG = "kanjivg"
    const val KANJIDIC = "kanjidic"
    const val JMDICT = "jmdict"
    const val JMDICT_FURIGANA = "jmdict-furigana"
    const val TANOS_JLPT = "tanos-jlpt"
    const val LEEDS_FREQUENCY = "leeds-frequency"
    const val YOMICHAN_JLPT_VOCAB = "yomichan-jlpt-vocab"
    const val TATOEBA = "tatoeba"
}
