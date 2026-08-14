package io.kaiteyo.kjd.pipeline

import io.kaiteyo.kjd.KjdVersion
import io.kaiteyo.kjd.db.Schema
import io.kaiteyo.kjd.model.CanonicalDatabase
import io.kaiteyo.kjd.source.SourceMetadata
import io.kaiteyo.kjd.validate.ValidationFinding
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

/**
 * Machine-readable release manifest written next to every generated database
 * (`release-manifest.json`). It records exactly what a release contains so
 * consumers and update tooling can verify compatibility before use:
 *
 *   - database / schema / generator versions
 *   - source versions and license count
 *   - record counts per entity type
 *   - validation outcome
 *   - generation timestamp
 */
@Serializable
data class ReleaseManifest(
    val platform: String,
    val databaseVersion: String,
    val schemaVersion: Int,
    val generatorVersion: String,
    val generatedAt: String,
    val sourceVersions: Map<String, String>,
    val licenseCount: Int,
    val recordCounts: RecordCounts,
    val validation: ValidationSummary,
    /** Deterministic content-state hash — used by the incremental updater. */
    val stateFingerprint: String
)

@Serializable
data class RecordCounts(
    val kanji: Int,
    val kana: Int,
    val vocabulary: Int,
    val senses: Int,
    val radicals: Int,
    val components: Int,
    val relationships: Int
)

@Serializable
data class ValidationSummary(
    val passed: Boolean,
    val fatalCount: Int,
    val warningCount: Int
)

object ReleaseManifestWriter {

    private val json = Json { prettyPrint = true; encodeDefaults = true }

    fun build(
        database: CanonicalDatabase,
        sources: List<SourceMetadata>,
        findings: List<ValidationFinding>,
        stateFingerprint: String
    ): ReleaseManifest = ReleaseManifest(
        platform = KjdVersion.PLATFORM_NAME,
        databaseVersion = KjdVersion.SDK_VERSION,
        schemaVersion = Schema.SCHEMA_VERSION,
        generatorVersion = KjdVersion.GENERATOR_VERSION,
        generatedAt = Instant.now().toString(),
        sourceVersions = sources.associate { it.id to it.version },
        licenseCount = sources.size,
        recordCounts = RecordCounts(
            kanji = database.kanji.size,
            kana = database.kana.size,
            vocabulary = database.vocabulary.size,
            senses = database.senses.size,
            radicals = database.radicals.size,
            components = database.components.size,
            relationships = database.relationships.size
        ),
        validation = ValidationSummary(
            passed = findings.none { it.severity == io.kaiteyo.kjd.validate.Severity.Fatal },
            fatalCount = findings.count { it.severity == io.kaiteyo.kjd.validate.Severity.Fatal },
            warningCount = findings.count { it.severity == io.kaiteyo.kjd.validate.Severity.Warning }
        ),
        stateFingerprint = stateFingerprint
    )

    fun write(manifest: ReleaseManifest, target: File) {
        target.parentFile?.mkdirs()
        target.writeText(json.encodeToString(ReleaseManifest.serializer(), manifest))
    }
}
