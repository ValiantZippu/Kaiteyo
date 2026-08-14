package io.kaiteyo.kjd.parser

import io.kaiteyo.kjd.source.SourceMetadata
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.File

/**
 * Parses the yomichan-jlpt-vocab dataset (a JSON array of Yomitan-style term
 * dictionaries). Each term is `[expression, reading, tags[], ...]`; the tags
 * contain `"jlpt-n5"` .. `"jlpt-n1"`.
 *
 * This is a *source adapter* only — the platform never copies Yomitan's
 * runtime implementation; it ingests the published data under its license.
 */
class YomichanJlptVocabParser : SourceParser<RawYomichanJlptVocab> {

    override val sourceId: String = "yomichan-jlpt-vocab"

    private val json = Json { ignoreUnknownKeys = true }

    override fun parse(file: File, metadata: SourceMetadata): ParseResult<RawYomichanJlptVocab> {
        val parsed = mutableListOf<RawYomichanJlptVocab>()
        val rejected = mutableListOf<ParseFailure>()

        try {
            val root: JsonElement = json.parseToJsonElement(file.readText())
            val array: JsonArray = when (root) {
                is JsonArray -> root
                is JsonObject -> root["terms"]?.jsonArray ?: root["data"]?.jsonArray ?: JsonArray(emptyList())
                else -> JsonArray(emptyList())
            }

            for ((index, element) in array.withIndex()) {
                try {
                    val termArray = element.jsonArray
                    if (termArray.size < 3) {
                        rejected.add(ParseFailure(recordId = index.toString(), reason = "Term array too short"))
                        continue
                    }
                    val expression = termArray[0].jsonPrimitive.contentOrNull ?: continue
                    val reading = termArray[1].jsonPrimitive.contentOrNull
                    val tags = termArray[2].jsonArray
                        .mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                    val level = tags.firstNotNullOfOrNull { tag ->
                        Regex("jlpt[-_]?([1-5])", RegexOption.IGNORE_CASE)
                            .find(tag)?.groupValues?.get(1)?.toIntOrNull()
                    }
                    if (level == null) continue
                    parsed.add(RawYomichanJlptVocab(expression, reading, level))
                } catch (t: Throwable) {
                    rejected.add(ParseFailure(recordId = index.toString(), reason = "Term parse failed: ${t.summary()}", exception = t))
                }
            }
        } catch (t: Throwable) {
            rejected.add(ParseFailure(recordId = file.name, reason = "JSON parse failed: ${t.summary()}", exception = t))
        }

        return ParseResult(metadata, parsed, rejected)
    }
}
