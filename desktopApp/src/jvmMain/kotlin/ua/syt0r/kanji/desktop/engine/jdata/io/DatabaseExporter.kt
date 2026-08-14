package ua.syt0r.kanji.desktop.engine.jdata.io

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.desktop.engine.jdata.model.ComponentEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.KanaEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.KanjiEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.PlatformData
import ua.syt0r.kanji.desktop.engine.jdata.model.RadicalEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.RelationEdge
import ua.syt0r.kanji.desktop.engine.jdata.model.StrokeSet
import ua.syt0r.kanji.desktop.engine.jdata.model.VocabEntry
import ua.syt0r.kanji.desktop.engine.jdata.source.SourceDefinition
import java.io.File

// ============================================================
// DATABASE EXPORTER
// Canonical data leaves the platform as JSON (full fidelity), CSV
// (kanji / vocab / radicals / relations) or Markdown (human summary).
// Internal database details are never exposed — consumers get the
// same stable-ID model the API speaks.
// ============================================================

@Serializable
data class ExportEnvelope(
    val schemaVersion: Int,
    val generatedAt: String,
    val kanji: List<KanjiEntry>,
    val kana: List<KanaEntry>,
    val vocab: List<VocabEntry>,
    val radicals: List<RadicalEntry>,
    val components: List<ComponentEntry>,
    val strokeSets: List<StrokeSet>,
    val relations: List<RelationEdge>,
    val sources: List<SourceDefinition>
)

object DatabaseExporter {

    private val json = Json { prettyPrint = true; encodeDefaults = true }

    fun toJson(data: PlatformData): String = json.encodeToString(
        ExportEnvelope(
            schemaVersion = data.schemaVersion,
            generatedAt = data.generatedAt,
            kanji = data.kanji.values.sortedBy { it.id },
            kana = data.kana.values.sortedBy { it.id },
            vocab = data.vocab.values.sortedBy { it.id },
            radicals = data.radicals.values.sortedBy { it.id },
            components = data.components.values.sortedBy { it.id },
            strokeSets = data.strokeSets.values.sortedBy { it.character },
            relations = data.relations,
            sources = data.sources.values.sortedBy { it.id }
        )
    )

    fun toCsvKanji(data: PlatformData): String = buildString {
        appendLine("id,character,meaning,on,kun,strokes,radical,jlpt,grade,freq,provenance")
        data.kanji.values.sortedBy { it.character }.forEach { k ->
            appendLine(
                csv(k.id, k.character, k.meanings.joinToString("; "), k.onReadings.joinToString("/"),
                    k.kunReadings.joinToString("/"), k.strokeCount?.toString() ?: "", k.radicalId ?: "",
                    k.jlpt?.let { "N$it" } ?: "", k.grade?.toString() ?: "", k.frequencyRank?.toString() ?: "",
                    k.sources.joinToString("|") { it.sourceId })
            )
        }
    }

    fun toCsvVocab(data: PlatformData): String = buildString {
        appendLine("id,expression,reading,gloss,pos,jlpt,frequency,provenance")
        data.vocab.values.sortedBy { it.id }.forEach { v ->
            appendLine(
                csv(v.id, v.expression, v.primaryReading ?: "", v.primaryGloss,
                    v.partOfSpeech.joinToString("/"),
                    v.jlpt?.let { "N$it" } ?: "",
                    v.frequencies.firstNotNullOfOrNull { it.rank }?.toString() ?: "",
                    v.sources.joinToString("|") { it.sourceId })
            )
        }
    }

    fun toCsvRadicals(data: PlatformData): String = buildString {
        appendLine("id,character,meaning,strokes,kanji_count")
        data.radicals.values.sortedBy { it.id }.forEach { r ->
            val count = data.relations.count { it.kind == "radical" && it.toId == r.id }
            appendLine(csv(r.id, r.character, r.meaning ?: "", r.strokeCount?.toString() ?: "", count.toString()))
        }
    }

    fun toCsvRelations(data: PlatformData): String = buildString {
        appendLine("id,from_type,from_id,to_type,to_id,kind")
        data.relations.forEach { e ->
            appendLine(csv(e.id, e.fromType.name, e.fromId, e.toType.name, e.toId, e.kind))
        }
    }

    fun toMarkdown(data: PlatformData): String = buildString {
        appendLine("# Kaiteyo Japanese language data")
        appendLine()
        appendLine("- Schema version: ${data.schemaVersion}")
        appendLine("- Generated: ${data.generatedAt}")
        appendLine("- Sources: ${data.sources.size}")
        data.sources.values.sortedBy { it.id }.forEach { source ->
            appendLine("  - `${source.id}` ${source.name} v${source.version} (${source.licenseName.ifBlank { "license not declared" }})")
        }
        appendLine()
        appendLine("## Record counts")
        data.recordCounts.forEach { (kind, count) -> appendLine("- $kind: $count") }
        appendLine()
        appendLine("## Kanji sample")
        data.kanji.values.sortedBy { it.character }.take(10).forEach { k ->
            appendLine("- ${k.character} ${k.meanings.take(2).joinToString("; ")} (${k.onReadings.joinToString("、")})")
        }
        appendLine()
        appendLine("## Vocabulary sample")
        data.vocab.values.sortedBy { it.id }.take(10).forEach { v ->
            appendLine("- ${v.expression} [${v.primaryReading ?: ""}] ${v.primaryGloss}")
        }
    }

    /** Writes all export formats into [dir]; returns the written files. */
    fun writeAll(data: PlatformData, dir: File): List<File> {
        dir.mkdirs()
        val files = listOf(
            File(dir, "kaiteyo-data.json") to toJson(data),
            File(dir, "kanji.csv") to toCsvKanji(data),
            File(dir, "vocabulary.csv") to toCsvVocab(data),
            File(dir, "radicals.csv") to toCsvRadicals(data),
            File(dir, "relations.csv") to toCsvRelations(data),
            File(dir, "README.md") to toMarkdown(data)
        )
        files.forEach { (file, content) -> file.writeText(content) }
        return files.map { it.first }
    }

    private fun csv(vararg fields: String): String =
        fields.joinToString(",") { field ->
            val escaped = field.replace("\"", "\"\"")
            "\"$escaped\""
        }
}
