package ua.syt0r.kanji.desktop.engine.jdata.pipeline

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryRepository
import ua.syt0r.kanji.desktop.engine.jdata.engine.NoStrokeGeometryProvider
import ua.syt0r.kanji.desktop.engine.jdata.engine.StrokeGeometryProvider
import ua.syt0r.kanji.desktop.engine.jdata.integration.PlatformBuilder
import ua.syt0r.kanji.desktop.engine.jdata.model.PlatformData
import ua.syt0r.kanji.desktop.engine.jdata.profiles.DatabaseProfile
import ua.syt0r.kanji.desktop.engine.jdata.schema.SchemaSql
import ua.syt0r.kanji.desktop.engine.jdata.validate.DataValidator
import ua.syt0r.kanji.desktop.engine.jdata.validate.DedupResolver
import ua.syt0r.kanji.desktop.engine.jdata.validate.IssueSeverity
import ua.syt0r.kanji.desktop.engine.jdata.validate.ValidationReport

// ============================================================
// GENERATION PIPELINE
// The reproducible build: validate sources → parse → normalize →
// resolve → relate → build → index → validate generated data →
// provenance/attribution → quality report → release manifest.
// Every step is logged with real numbers. Generation is
// deterministic (sorted iteration, no randomness).
// ============================================================

data class GenerationConfig(
    val generatorVersion: String = "1.0.0",
    val schemaVersion: Int = SchemaSql.SchemaVersion,
    val normalizationVersion: String = "1",
    val profile: DatabaseProfile = DatabaseProfile.Standard,
    val generatedAt: String = "",
    /** Real stroke geometry source (e.g. a KanjiVG directory); counts-only by default. */
    val geometry: StrokeGeometryProvider = NoStrokeGeometryProvider
)

interface PipelineLogger {
    fun info(message: String)
    fun warn(message: String)
    fun error(message: String)
}

class ConsolePipelineLogger(
    private val sink: (String) -> Unit = { println(it) }
) : PipelineLogger {
    override fun info(message: String) = sink("[kjd] $message")
    override fun warn(message: String) = sink("[kjd] WARN: $message")
    override fun error(message: String) = sink("[kjd] ERROR: $message")
}

class GenerationPipeline(private val logger: PipelineLogger) {

    fun run(repository: DictionaryRepository, config: GenerationConfig): PipelineResult {
        logger.info("Generation started (profile=${config.profile.label}, generator=${config.generatorVersion}, schema=${config.schemaVersion})")

        // 1. Validate sources (installed dictionaries must be coherent).
        val installed = repository.installedDictionaries()
        val brokenSources = installed.filter { it.entryCount < 0 }
        if (brokenSources.isNotEmpty()) {
            brokenSources.forEach { logger.error("Source ${it.id} has invalid entry count ${it.entryCount}") }
        }
        logger.info("Validated ${installed.size} source(s)")

        // 2–4. Parse → normalize → resolve (canonical build with entity resolution).
        val rawKanjiCount = repository.allEntries().flatMap { it.kanjiSpellings }.size
        val rawVocabCount = repository.allEntries().count { it.senses.isNotEmpty() || it.readings.isNotEmpty() }
        logger.info("Parsed $rawKanjiCount kanji spelling record(s), $rawVocabCount vocabulary record(s)")

        val platformData = PlatformBuilder.fromRepository(
            repository = repository,
            profile = config.profile,
            generatedAt = config.generatedAt,
            geometry = config.geometry
        )
        logger.info("Resolved ${platformData.kanji.size} kanji, ${platformData.vocab.size} vocabulary (deduplicated by canonical identity)")

        val duplicates = DedupResolver.findDuplicateCandidates(platformData.vocab.values)
        if (duplicates.isNotEmpty()) {
            logger.warn("${duplicates.size} duplicate identity candidate(s) found and merged")
        }

        // 5–7. Relate → index (relationships + search index built on final set).
        logger.info("Constructed ${platformData.relations.size} relationship edge(s)")
        logger.info("Created search index over ${platformData.totalEntries} entities")

        // 8. Validate the generated data.
        val validation = DataValidator.validate(platformData)
        if (validation.isClean()) {
            logger.info("Validation passed (${validation.issues.size} issues)")
        } else {
            logger.warn("Validation: ${validation.summary}")
            validation.fatal.forEach { logger.error("${it.code}: ${it.message}") }
        }

        // 9–10. Provenance + attribution.
        logger.info("Attached provenance to ${platformData.vocab.values.count { it.sources.isNotEmpty() }} vocabulary entr(ies)")
        logger.info("Attribution manifest covers ${platformData.sources.size} source(s)")

        // 11. Quality report.
        val quality = QualityReport.from(platformData, validation, installed.map { it.id to it.revision })
        logger.info("Quality: ${quality.recordCounts}")

        // 12. Release manifest.
        val release = ReleaseManifest(
            databaseVersion = SchemaSql.DatabaseVersion,
            schemaVersion = config.schemaVersion,
            generatorVersion = config.generatorVersion,
            normalizationVersion = config.normalizationVersion,
            sourceVersions = installed.map { SourceVersionEntry(it.id, it.revision) },
            generatedAt = config.generatedAt,
            licenseManifest = "THIRD_PARTY_DATA.md / THIRD_PARTY_DATA.json",
            recordCounts = quality.recordCounts,
            validationResult = if (validation.isFatal()) "FAILED" else if (validation.isClean()) "PASSED" else "PASSED_WITH_ISSUES"
        )

        logger.info("Generation complete: ${release.validationResult}")
        return PipelineResult(
            success = !validation.isFatal(),
            platformData = platformData,
            quality = quality,
            release = release
        )
    }
}

data class PipelineResult(
    val success: Boolean,
    val platformData: PlatformData,
    val quality: QualityReport,
    val release: ReleaseManifest
)

// ============================================================
// Quality report
// ============================================================

/** Serializable source/version pair for manifests. */
@Serializable
data class SourceVersionEntry(val sourceId: String, val version: String)

@Serializable
data class QualityReport(
    val recordCounts: Map<String, Int>,
    val strokeCoverage: Float,
    val jlptCoverage: Float,
    val frequencyCoverage: Float,
    val furiganaCoverage: Float,
    val unresolvedReferences: List<String>,
    val malformedRecords: List<String>,
    val duplicateCandidates: List<String>,
    val missingOptional: List<String>,
    val sourceVersions: List<SourceVersionEntry>,
    val validation: String
) {

    companion object {
        fun from(data: PlatformData, report: ValidationReport, sourceVersions: List<Pair<String, String>>): QualityReport {
            val kanjiTotal = data.kanji.size
            val vocabTotal = data.vocab.size
            val withJlpt = data.kanji.values.count { it.jlpt != null } + data.vocab.values.count { it.jlpt != null }
            val withFreq = data.kanji.values.count { it.frequencyRank != null } + data.vocab.values.count { it.frequencies.isNotEmpty() }
            val withFurigana = data.vocab.values.count { it.furigana.isNotEmpty() }
            return QualityReport(
                recordCounts = data.recordCounts,
                strokeCoverage = if (kanjiTotal == 0) 0f else data.strokeSets.size.toFloat() / kanjiTotal,
                jlptCoverage = if (kanjiTotal + vocabTotal == 0) 0f else withJlpt.toFloat() / (kanjiTotal + vocabTotal),
                frequencyCoverage = if (kanjiTotal + vocabTotal == 0) 0f else withFreq.toFloat() / (kanjiTotal + vocabTotal),
                furiganaCoverage = if (vocabTotal == 0) 0f else withFurigana.toFloat() / vocabTotal,
                unresolvedReferences = report.unresolved.map { it.entityId },
                malformedRecords = report.issues
                    .filter { it.severity == IssueSeverity.FATAL || it.severity == IssueSeverity.RECOVERABLE }
                    .map { "${it.code}: ${it.message}" },
                duplicateCandidates = report.issues.filter { it.code == "duplicate-entity" }.map { it.entityId },
                missingOptional = report.missingOptional.map { it.entityId },
                sourceVersions = sourceVersions.map { (id, version) -> SourceVersionEntry(id, version) },
                validation = report.summary
            )
        }
    }

    fun toMarkdown(): String = buildString {
        appendLine("# Data quality report")
        appendLine()
        appendLine("- Record counts: ${recordCounts.entries.joinToString(", ") { "${it.key}=${it.value}" }}")
        appendLine("- Stroke coverage: ${(strokeCoverage * 100).toInt()}%")
        appendLine("- JLPT coverage: ${(jlptCoverage * 100).toInt()}%")
        appendLine("- Frequency coverage: ${(frequencyCoverage * 100).toInt()}%")
        appendLine("- Furigana coverage: ${(furiganaCoverage * 100).toInt()}%")
        appendLine("- Unresolved references: ${unresolvedReferences.size}")
        appendLine("- Malformed records: ${malformedRecords.size}")
        appendLine("- Duplicate candidates: ${duplicateCandidates.size}")
        appendLine("- Missing optional: ${missingOptional.size}")
        appendLine("- Validation: $validation")
        if (unresolvedReferences.isNotEmpty()) appendLine("- Unresolved: ${unresolvedReferences.take(10).joinToString(", ")}")
        if (malformedRecords.isNotEmpty()) appendLine("- Malformed: ${malformedRecords.take(10).joinToString("; ")}")
    }
}

// ============================================================
// Release manifest
// ============================================================

@Serializable
data class ReleaseManifest(
    val databaseVersion: String,
    val schemaVersion: Int,
    val generatorVersion: String,
    val normalizationVersion: String,
    val sourceVersions: List<SourceVersionEntry>,
    val generatedAt: String,
    val licenseManifest: String,
    val recordCounts: Map<String, Int>,
    val validationResult: String
) {
    fun toJson(): String = Json { prettyPrint = true }.encodeToString(this)
}
