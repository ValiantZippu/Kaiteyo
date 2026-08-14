package io.kaiteyo.kjd.pipeline

import io.kaiteyo.kjd.db.DatabaseWriter
import io.kaiteyo.kjd.export.AttributionWriter
import io.kaiteyo.kjd.export.Exporter
import io.kaiteyo.kjd.model.CanonicalDatabase
import io.kaiteyo.kjd.parser.JmdictFuriganaParser
import io.kaiteyo.kjd.parser.JmdictParser
import io.kaiteyo.kjd.parser.KanjidicParser
import io.kaiteyo.kjd.parser.KanjiVgParser
import io.kaiteyo.kjd.parser.LeedsFrequencyParser
import io.kaiteyo.kjd.parser.TanosJlptParser
import io.kaiteyo.kjd.parser.YomichanJlptVocabParser
import io.kaiteyo.kjd.parser.ParseFailure
import io.kaiteyo.kjd.parser.SourceParser
import io.kaiteyo.kjd.resolve.EntityResolver
import io.kaiteyo.kjd.source.SafeArchiveExtractor
import io.kaiteyo.kjd.source.SourceIds
import io.kaiteyo.kjd.source.SourceMetadata
import io.kaiteyo.kjd.validate.DatabaseValidator
import io.kaiteyo.kjd.validate.QualityReport
import io.kaiteyo.kjd.validate.QualityReporter
import io.kaiteyo.kjd.validate.Severity
import java.io.File

/**
 * Configuration for one pipeline run. All paths are explicit — no
 * developer-specific defaults.
 */
data class PipelineConfig(
    /** Root directory that contains `sources/<id>/raw/...` per source. */
    val sourcesDir: File,
    val outputDatabase: File,
    val outputReportDir: File? = null,
    /** When true, the generated database + attribution are also exported as JSON/CSV. */
    val exportArtifacts: Boolean = false,
    val exportDirectory: File? = null
)

/**
 * The complete generation pipeline:
 *
 *   validate sources → parse → normalize → resolve → validate → generate →
 *   index → validate → metadata → attribution → package
 *
 * Raw inputs live under `sources/<id>/raw/`; the layout is discovered here.
 * Any enabled dataset present in the tree is ingested; missing optional
 * datasets are skipped with a logged warning.
 */
class KjdPipeline {

    private val logger = PipelineLogger()

    private val kanjiVgParser = KanjiVgParser()
    private val kanjidicParser = KanjidicParser()
    private val jmdictParser = JmdictParser()
    private val furiganaParser = JmdictFuriganaParser()
    private val tanosParser = TanosJlptParser()
    private val leedsParser = LeedsFrequencyParser()
    private val yomichanParser = YomichanJlptVocabParser()

    fun run(config: PipelineConfig): QualityReport {
        logger.info("KJD pipeline start — sources: ${config.sourcesDir.absolutePath}")

        if (!config.sourcesDir.exists()) {
            throw IllegalArgumentException("Sources directory does not exist: ${config.sourcesDir}")
        }

        // ---------------------------------------------------------------
        // 1. Discover + parse every enabled source.
        // ---------------------------------------------------------------
        val parsedKanjiVg = parseIfPresent(config, SourceIds.KANJIVG, kanjiVgParser)
        val parsedKanjidic = parseIfPresent(config, SourceIds.KANJIDIC, kanjidicParser)
        val parsedJmdict = parseIfPresent(config, SourceIds.JMDICT, jmdictParser)
        val parsedFurigana = parseIfPresent(config, SourceIds.JMDICT_FURIGANA, furiganaParser)
        val parsedTanos = parseIfPresent(config, SourceIds.TANOS_JLPT, tanosParser)
        val parsedLeeds = parseIfPresent(config, SourceIds.LEEDS_FREQUENCY, leedsParser)
        val parsedYomichan = parseIfPresent(config, SourceIds.YOMICHAN_JLPT_VOCAB, yomichanParser)

        // ---------------------------------------------------------------
        // 2. Normalize + resolve + cross-link into the canonical database.
        // ---------------------------------------------------------------
        val resolver = EntityResolver()
        resolver.resolve(
            kanjiVg = parsedKanjiVg.first,
            kanjidic = parsedKanjidic.first,
            jmdict = parsedJmdict.first,
            furigana = parsedFurigana.first,
            tanosJlpt = parsedTanos.first,
            leedsFrequency = parsedLeeds.first,
            yomichanJlptVocab = parsedYomichan.first,
            kanjiVgCharacters = parsedKanjiVg.second,
            kanjidicCharacters = parsedKanjidic.second,
            jmdictEntries = parsedJmdict.second,
            furiganaRecords = parsedFurigana.second,
            tanosJlptRecords = parsedTanos.second,
            leedsFrequencyRecords = parsedLeeds.second,
            yomichanJlptVocabRecords = parsedYomichan.second
        )
        val database = resolver.database().snapshot()

        // ---------------------------------------------------------------
        // 3. Validate the canonical database.
        // ---------------------------------------------------------------
        val findings = DatabaseValidator().validate(database)
        val fatal = findings.filter { it.severity == Severity.Fatal }
        if (fatal.isNotEmpty()) {
            fatal.forEach { logger.error("FATAL [${it.entityType}] ${it.entityId ?: "-"}: ${it.message}") }
            throw IllegalStateException("Validation failed with ${fatal.size} fatal finding(s)")
        }
        findings.filter { it.severity == Severity.Warning }
            .forEach { logger.warn("[${it.entityType}] ${it.entityId ?: "-"}: ${it.message}") }

        // ---------------------------------------------------------------
        // 4. Generate the SQLite database.
        // ---------------------------------------------------------------
        val sources = allMetadata()
        DatabaseWriter().write(database, config.outputDatabase, sources)
        logger.info("Generated ${config.outputDatabase.absolutePath}")

        // ---------------------------------------------------------------
        // 5. Quality report.
        // ---------------------------------------------------------------
        val report = QualityReporter().report(
            database,
            findings,
            sources.associate { it.id to it.version }
        )
        config.outputReportDir?.let { dir ->
            dir.mkdirs()
            File(dir, "quality-report.md").writeText(report.toMarkdown())
            File(dir, "quality-report.json").writeText(
                kotlinx.serialization.json.Json { prettyPrint = true }
                    .encodeToString(QualityReport.serializer(), report)
            )
        }

        // ---------------------------------------------------------------
        // 5b. Third-party attribution + release manifest (shipped with the DB).
        // ---------------------------------------------------------------
        val outputDir = config.outputDatabase.parentFile ?: File(".")
        outputDir.mkdirs()
        val manifest = io.kaiteyo.kjd.source.AttributionManifest(
            platform = io.kaiteyo.kjd.KjdVersion.PLATFORM_NAME,
            generatedBy = io.kaiteyo.kjd.KjdVersion.GENERATOR_VERSION,
            generatedAt = java.time.Instant.now().toString(),
            schemaVersion = io.kaiteyo.kjd.db.Schema.SCHEMA_VERSION,
            sources = sources
        )
        val (attributionJson, attributionMd) = AttributionWriter().write(manifest, outputDir)
        logger.info("Wrote ${attributionJson.name} and ${attributionMd.name}")

        val releaseManifest = ReleaseManifestWriter.build(
            database = database,
            sources = sources,
            findings = findings,
            stateFingerprint = io.kaiteyo.kjd.patch.DatabaseFingerprint.compute(config.outputDatabase)
        )
        val releaseFile = File(outputDir, "release-manifest.json")
        ReleaseManifestWriter.write(releaseManifest, releaseFile)
        logger.info("Wrote ${releaseFile.name}")

        // ---------------------------------------------------------------
        // 6. Optional export artifacts.
        // ---------------------------------------------------------------
        if (config.exportArtifacts) {
            val exportDir = config.exportDirectory ?: config.outputDatabase.parentFile ?: File(".")
            val exporter = Exporter()
            exporter.exportJson(database, File(exportDir, "kjd-export.json"))
            exporter.exportCsv(database, File(exportDir, "kjd-export.csv"))
            logger.info("Exported artifacts to ${exportDir.absolutePath}")
        }

        logger.info("Pipeline complete — kanji=${report.kanjiCount} vocab=${report.vocabularyCount} senses=${report.senseCount}")
        return report
    }

    // ---------------------------------------------------------------
    // Source discovery
    // ---------------------------------------------------------------

    private fun allMetadata(): List<SourceMetadata> =
        io.kaiteyo.kjd.source.BuiltinSources.all

    /**
     * Parse a source when its raw directory exists. Returns
     * (metadataBySourceId, records).
     */
    private fun <T> parseIfPresent(
        config: PipelineConfig,
        sourceId: String,
        parser: SourceParser<T>
    ): Pair<Map<String, SourceMetadata>, List<T>> {
        val sourceDir = File(config.sourcesDir, "sources/$sourceId/raw")
        if (!sourceDir.exists()) {
            logger.warn("Source $sourceId not present — skipping (${sourceDir.absolutePath})")
            return emptyMap<String, SourceMetadata>() to emptyList()
        }
        val metadata = io.kaiteyo.kjd.source.BuiltinSources.byId(sourceId)
        val files = sourceDir.listFiles { file -> file.isFile }?.toList()
            .orEmpty()
            .filter {
                it.name.endsWith(".xml", true) || it.name.endsWith(".json", true) ||
                    it.name.endsWith(".svg", true) || it.name.endsWith(".txt", true) ||
                    it.name.endsWith(".zip", true)
            }

        val records = mutableListOf<T>()
        var rejectedTotal = 0
        for (file in files) {
            val result = if (file.extension.equals("zip", ignoreCase = true)) {
                parseZip(config, sourceDir, parser, metadata, file)
            } else {
                parser.parse(file, metadata)
            }
            records.addAll(result.parsed)
            rejectedTotal += result.rejected.size
            reportRejections(sourceId, file, result.rejected)
        }
        logger.info("Source $sourceId: ${records.size} records parsed, $rejectedTotal rejected")
        return mapOf(sourceId to metadata) to records
    }

    private fun <T> parseZip(
        config: PipelineConfig,
        sourceDir: File,
        parser: SourceParser<T>,
        metadata: SourceMetadata,
        zipFile: File
    ): io.kaiteyo.kjd.parser.ParseResult<T> {
        // Extract the zip into a sibling extracted/ dir (safely — malicious
        // archives cannot escape the extraction dir) and parse the contents.
        val extractionDir = File(sourceDir, "extracted/${zipFile.nameWithoutExtension}")
        if (!extractionDir.exists()) {
            val result = SafeArchiveExtractor.extractZip(zipFile, extractionDir)
            result.rejected.forEach {
                logger.warn("${sourceDir.parentFile.name}/${zipFile.name}: rejected archive entry ${it.name}: ${it.reason}")
            }
        }
        val innerFiles = extractionDir.walkTopDown()
            .filter { it.isFile }
            .filter {
                it.name.endsWith(".svg", true) || it.name.endsWith(".xml", true) ||
                    it.name.endsWith(".json", true)
            }
            .toList()
        val parsed = mutableListOf<T>()
        val rejected = mutableListOf<ParseFailure>()
        for (inner in innerFiles) {
            val result = parser.parse(inner, metadata)
            parsed.addAll(result.parsed)
            rejected.addAll(result.rejected)
        }
        return io.kaiteyo.kjd.parser.ParseResult(metadata, parsed, rejected)
    }

    private fun reportRejections(sourceId: String, file: File, rejected: List<ParseFailure>) {
        if (rejected.isEmpty()) return
        rejected.take(3).forEach { failure ->
            logger.warn("$sourceId/${file.name}: rejected ${failure.recordId ?: "-"}: ${failure.reason}")
        }
    }
}

/** Minimal structured logger for pipeline runs. */
class PipelineLogger {
    fun info(message: String) = println("[KJD] $message")
    fun warn(message: String) = println("[KJD] WARN: $message")
    fun error(message: String) = System.err.println("[KJD] ERROR: $message")
}
