package ua.syt0r.kanji.desktop.engine.dictionary

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ============================================================
// SOURCE PROVENANCE / ATTRIBUTION
// Every installed dictionary is a third-party dataset. This
// manifest keeps its license, version, attribution and retrieval
// info structured so consumers can honor redistribution terms.
// We never claim ownership of third-party content — when a
// source does not declare a license we say so explicitly instead
// of inventing one.
// ============================================================

/** Structured license/attribution record for one installed source. */
@Serializable
data class LicenseRecord(
    val sourceId: String,
    val name: String,
    val homepage: String = "",
    val licenseName: String = "",
    val licenseUrl: String = "",
    val attribution: String = "",
    val version: String = "",
    val retrievedAt: String = "",
    val redistributionNotes: String = "",
    val transformationNotes: String = "",
    val entryCount: Long = 0
)

/** The full third-party data manifest for a repository. */
@Serializable
data class ThirdPartyDataManifest(
    val schemaVersion: Int = 1,
    val generator: String = "Kaiteyo dictionary engine (kjd)",
    val generatedAt: String = "",
    val sources: List<LicenseRecord> = emptyList(),
    val totalEntries: Long = 0
)

object ThirdPartyDataReport {

    private val json = Json { prettyPrint = true; encodeDefaults = true }

    /**
     * Builds the manifest from the currently installed dictionaries. Field
     * values come from the dictionaries' own metadata (id, name, revision,
     * author). License details are only filled when the source declares
     * them; otherwise the record explicitly says "Not declared".
     */
    fun generate(repository: DictionaryRepository): ThirdPartyDataManifest {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString()
        val records = repository.installedDictionaries().map { dict ->
            val license = if (dict.tags.any { it.startsWith("license:") }) {
                dict.tags.first { it.startsWith("license:") }.removePrefix("license:")
            } else "Not declared by source"
            LicenseRecord(
                sourceId = dict.id,
                name = dict.name,
                homepage = dict.tags.firstOrNull { it.startsWith("homepage:") }?.removePrefix("homepage:") ?: "",
                licenseName = license,
                licenseUrl = dict.tags.firstOrNull { it.startsWith("license-url:") }?.removePrefix("license-url:") ?: "",
                attribution = dict.authoredBy,
                version = dict.revision,
                retrievedAt = "",
                redistributionNotes = "See license; the Kaiteyo code license (GPL-3.0) does not apply to third-party data.",
                transformationNotes = "Normalized to the dictionary engine model; original source data is never mutated.",
                entryCount = dict.entryCount
            )
        }
        return ThirdPartyDataManifest(
            generatedAt = now,
            sources = records,
            totalEntries = repository.totalEntries()
        )
    }

    fun toJson(manifest: ThirdPartyDataManifest): String = json.encodeToString(manifest)

    fun toMarkdown(manifest: ThirdPartyDataManifest): String = buildString {
        appendLine("# Third-Party Data")
        appendLine()
        appendLine("Kaiteyo uses the following third-party Japanese language datasets.")
        appendLine("The Kaiteyo code is licensed under GPL-3.0; this license does **not** cover the data below.")
        appendLine()
        appendLine("Generated: ${manifest.generatedAt}")
        appendLine("Generator: ${manifest.generator}")
        appendLine("Total entries: ${manifest.totalEntries}")
        appendLine()
        for (source in manifest.sources) {
            appendLine("## ${source.name} (`${source.sourceId}`)")
            appendLine()
            appendLine("- Version: ${source.version.ifBlank { "—" }}")
            appendLine("- Authored by: ${source.attribution.ifBlank { "—" }}")
            appendLine("- Homepage: ${source.homepage.ifBlank { "—" }}")
            appendLine("- License: ${source.licenseName}")
            if (source.licenseUrl.isNotBlank()) appendLine("- License URL: ${source.licenseUrl}")
            appendLine("- Entries: ${source.entryCount}")
            appendLine("- Redistribution: ${source.redistributionNotes}")
            appendLine("- Transformations: ${source.transformationNotes}")
            appendLine()
        }
    }
}
