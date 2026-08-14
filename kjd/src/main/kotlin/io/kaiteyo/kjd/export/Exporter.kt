package io.kaiteyo.kjd.export

import io.kaiteyo.kjd.model.CanonicalDatabase
import io.kaiteyo.kjd.model.EntityType
import io.kaiteyo.kjd.model.Kanji
import io.kaiteyo.kjd.model.VocabularyEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Export subsystem. The canonical database can be exported to:
 *
 *   - JSON     (one document, grouped)
 *   - JSONL    (one record per line — streaming-friendly for huge datasets)
 *   - CSV      (flat tables for kanji / vocabulary)
 *
 * JSON is never the only distribution format; the SQLite database is the
 * primary artifact.
 */
class Exporter(private val json: Json = Json { prettyPrint = true; encodeDefaults = true }) {

    fun exportJson(database: CanonicalDatabase, target: File) {
        val payload = mapOf(
            "kanji" to database.kanji,
            "kana" to database.kana,
            "vocabulary" to database.vocabulary,
            "radicals" to database.radicals
        )
        target.parentFile?.mkdirs()
        target.writeText(json.encodeToString(payload))
    }

    fun exportJsonl(database: CanonicalDatabase, target: File) {
        target.parentFile?.mkdirs()
        val lineJson = Json { encodeDefaults = true }
        target.bufferedWriter().use { writer ->
            database.kanji.forEach { kanji ->
                writer.write(lineJson.encodeToString(JsonlRecord(EntityType.Kanji, kanji)))
                writer.newLine()
            }
            database.vocabulary.forEach { vocab ->
                writer.write(lineJson.encodeToString(JsonlRecord(EntityType.Vocabulary, vocab)))
                writer.newLine()
            }
        }
    }

    fun exportCsv(database: CanonicalDatabase, target: File) {
        target.parentFile?.mkdirs()
        target.bufferedWriter().use { writer ->
            // Kanji table.
            writer.write("literal,on_readings,kun_readings,meanings,grade,jlpt,frequency,stroke_count,radical")
            writer.newLine()
            database.kanji.forEach { kanji ->
                writer.write(
                    listOf(
                        csv(kanji.character.literal),
                        csv(kanji.onReadings.joinToString(" ") { it.value }),
                        csv(kanji.kunReadings.joinToString(" ") { it.value }),
                        csv(kanji.meanings.joinToString("; ") { it.value }),
                        kanji.grade?.toString().orEmpty(),
                        kanji.jlpt.joinToString(",") { "N${it.level}" },
                        kanji.frequency.joinToString(",") { it.value.toString() },
                        kanji.strokeCount?.toString().orEmpty(),
                        csv(kanji.radical.orEmpty())
                    ).joinToString(",")
                )
                writer.newLine()
            }

            writer.newLine()

            // Vocabulary table.
            writer.write("expression,reading,glosses,pos,jlpt")
            writer.newLine()
            database.vocabulary.forEach { vocab ->
                writer.write(
                    listOf(
                        csv(vocab.expression),
                        csv(vocab.readings.joinToString("/") { it.value }),
                        csv(vocab.senses.flatMap { it.glosses }.joinToString("; ") { it.value }),
                        csv(vocab.partsOfSpeech.joinToString("/") { it.value }),
                        vocab.jlpt.joinToString(",") { "N${it.level}" }
                    ).joinToString(",")
                )
                writer.newLine()
            }
        }
    }

    private fun csv(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value
}

@kotlinx.serialization.Serializable
private data class JsonlRecord(
    val type: EntityType,
    val data: kotlinx.serialization.json.JsonElement
) {
    constructor(type: EntityType, kanji: Kanji) : this(
        type,
        jsonElementOf(kanji)
    )

    constructor(type: EntityType, vocab: VocabularyEntry) : this(
        type,
        jsonElementOf(vocab)
    )

    companion object {
        private val elementJson = Json { encodeDefaults = true }
        private fun jsonElementOf(value: Any): kotlinx.serialization.json.JsonElement =
            when (value) {
                is Kanji -> elementJson.parseToJsonElement(elementJson.encodeToString(Kanji.serializer(), value))
                is VocabularyEntry -> elementJson.parseToJsonElement(elementJson.encodeToString(VocabularyEntry.serializer(), value))
                else -> throw IllegalArgumentException("Unsupported record type: ${value::class}")
            }
    }
}
