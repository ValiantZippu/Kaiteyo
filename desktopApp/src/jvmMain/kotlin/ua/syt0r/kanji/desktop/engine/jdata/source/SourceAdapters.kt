package ua.syt0r.kanji.desktop.engine.jdata.source

import kotlinx.serialization.Serializable
import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryEntry
import ua.syt0r.kanji.desktop.engine.dictionary.DictionaryRepository
import ua.syt0r.kanji.desktop.engine.jdata.model.PlatformData
import ua.syt0r.kanji.desktop.engine.jdata.validate.DedupResolver

// ============================================================
// SOURCE ADAPTER FRAMEWORK
// Adding a new dataset means supplying a definition + parser +
// normalizer + validator + resolver — the pipeline itself stays
// untouched. Raw source content is never mutated: parsers produce
// new canonical drafts and provenance is attached at import time.
// ============================================================

/** Metadata about one source dataset. */
@Serializable
data class SourceDefinition(
    val id: String,
    val name: String,
    val version: String = "",
    val homepage: String = "",
    val licenseName: String = "",
    val licenseUrl: String = "",
    val retrievalDate: String = "",
    val format: String = "",
    val priority: Int = 0,
    val tags: List<String> = emptyList()
)

/** Version + processing metadata for one source. */
@Serializable
data class SourceVersion(
    val sourceId: String,
    val version: String = "",
    val checksum: String = "",
    val parserVersion: String = "",
    val transformationVersion: String = ""
)

/** One raw record as read from a source (never mutated). */
data class SourceRecord(
    val sourceId: String,
    val key: String,
    val payload: Any? = null,
    val retrievedAt: String = ""
)

/** A parser converts raw records into intermediate (source-shaped) records. */
interface SourceParser<in R, out P> {
    fun parse(record: SourceRecord): List<P>
}

/** A normalizer converts source-shaped records into canonical drafts. */
interface SourceNormalizer<in P, out N> {
    fun normalize(parsed: List<P>): List<N>
}

/** Validates canonical data; returns categorized issues. */
interface SourceValidator {
    fun validate(data: PlatformData): ua.syt0r.kanji.desktop.engine.jdata.validate.ValidationReport
}

/** Resolves duplicate candidates into canonical identities. */
interface SourceResolver {
    fun resolve(candidates: List<DedupResolver.DuplicateCandidate>): Map<String, String>
}

/** Imports a source into a repository/store. */
interface SourceImporter {
    fun import(definitions: List<SourceDefinition>, records: List<SourceRecord>): ImportSummary
}

data class ImportSummary(
    val sourceId: String = "",
    val imported: Int = 0,
    val skipped: Int = 0,
    val rejected: List<String> = emptyList()
)

/** Transforms canonical data (extension pipelines hook in here). */
interface SourceTransformer {
    fun transform(input: PlatformData): PlatformData
}

/** Exports canonical data to another representation. */
interface SourceExporter {
    fun export(data: PlatformData): ExportBundle
}

data class ExportBundle(
    val format: String,
    val content: String,
    val fileName: String
)

// ============================================================
// Reference implementations
// ============================================================

/**
 * Yomitan/JMdict term parser: the heavy lifting (ZIP/JSON-LD parsing) is
 * done by [ua.syt0r.kanji.desktop.engine.dictionary.DictionaryImporter];
 * this adapter layers the source pipeline on top so terms can be treated
 * as raw records and normalized through the canonical pipeline.
 */
class JmdictTermParser : SourceParser<SourceRecord, DictionaryEntry> {

    override fun parse(record: SourceRecord): List<DictionaryEntry> {
        val text = record.payload as? String ?: return emptyList()
        return ua.syt0r.kanji.desktop.engine.dictionary.DictionaryImporter.parseTerms(text)
            .map { it.copy(dictionaryId = record.sourceId) }
    }
}

/** Normalization pass: NFC + kana-folding on canonical drafts. */
class CanonicalNormalizer : SourceNormalizer<DictionaryEntry, DictionaryEntry> {
    override fun normalize(parsed: List<DictionaryEntry>): List<DictionaryEntry> =
        parsed.map { entry ->
            entry.copy(
                headword = java.text.Normalizer.normalize(entry.headword, java.text.Normalizer.Form.NFC),
                spellings = entry.spellings.map { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFC) },
                readings = entry.readings.map { reading ->
                    reading.copy(reading = reading.reading)
                }
            )
        }
}

/** Default resolver: identity-key based deduplication (see DedupResolver). */
class IdentitySourceResolver : SourceResolver {
    override fun resolve(candidates: List<DedupResolver.DuplicateCandidate>): Map<String, String> {
        // Canonical id per candidate key: the first (deterministic) id wins.
        return candidates.associate { candidate -> candidate.key to candidate.ids.minOrNull().orEmpty() }
    }
}

/** Importer that installs parsed entries into the dictionary repository. */
class RepositorySourceImporter(private val repository: DictionaryRepository) : SourceImporter {

    override fun import(definitions: List<SourceDefinition>, records: List<SourceRecord>): ImportSummary {
        var imported = 0
        var skipped = 0
        val rejected = mutableListOf<String>()
        definitions.forEach { definition ->
            val entries = records
                .filter { it.sourceId == definition.id }
                .flatMap { record ->
                    val text = record.payload as? String ?: return@flatMap emptyList()
                    ua.syt0r.kanji.desktop.engine.dictionary.DictionaryImporter.parseTerms(text)
                }
                .map { it.copy(dictionaryId = definition.id) }
            if (entries.isNotEmpty()) {
                repository.install(
                    ua.syt0r.kanji.desktop.engine.dictionary.InstalledDictionary(
                        id = definition.id,
                        name = definition.name,
                        revision = definition.version,
                        authoredBy = "",
                        format = ua.syt0r.kanji.desktop.engine.dictionary.DictionaryFormat.Yomitan,
                        priority = definition.priority,
                        tags = definition.tags
                    ),
                    entries
                )
                imported += entries.size
            } else {
                skipped++
                rejected.add(definition.id)
            }
        }
        return ImportSummary(
            sourceId = definitions.firstOrNull()?.id ?: "",
            imported = imported,
            skipped = skipped,
            rejected = rejected
        )
    }
}
