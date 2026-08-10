package ua.syt0r.kanji.desktop.engine.dictionary

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

// ============================================
// KAITEYO DICTIONARY IMPORTER
// Parses Yomitan-compatible dictionary files
// (index.json + per-term JSON-LD documents) and
// JMdict-style JSON exports. Handles the common
// shapes produced by Wiktionary/Yomitan packs.
// ============================================

/** Result summarising a dictionary import. */
data class DictImportResult(
    val dictionaryId: String,
    val name: String,
    val revision: String,
    val detectedFormat: DictionaryFormat,
    val entries: Long
)

/** Import bundle: the summary plus the parsed entries ready for install. */
data class DictImportBundle(
    val result: DictImportResult,
    val entries: List<DictionaryEntry>
)

data class IndexMeta(
    val name: String,
    val revision: String,
    val author: String,
    val format: DictionaryFormat
)

object DictionaryImporter {

    private val json = Json { ignoreUnknownKeys = true }

    private fun str(el: JsonElement?): String = (el as? JsonPrimitive)?.content ?: ""

    private fun strList(el: JsonElement?): List<String> {
        val arr = el as? JsonArray ?: return emptyList()
        return arr.mapNotNull { (it as? JsonPrimitive)?.content }
    }

    fun parseIndexMeta(bytes: ByteArray): IndexMeta {
        val obj = runCatching {
            json.parseToJsonElement(String(bytes, Charsets.UTF_8)).jsonObject
        }.getOrNull() ?: return IndexMeta("", "", "", DictionaryFormat.Yomitan)
        return IndexMeta(
            name = str(obj["title"]).ifBlank { str(obj["name"]) },
            revision = str(obj["revision"]),
            author = str(obj["author"]) + str(obj["authoring"]).let { if (it.isNotBlank()) ":$it" else "" },
            format = when (str(obj["format"]).lowercase()) {
                "kanjidic2" -> DictionaryFormat.KanjiDic
                "kanjidic" -> DictionaryFormat.KanjiDic
                "frequency" -> DictionaryFormat.Frequency
                "pitch accent", "pitch_accent" -> DictionaryFormat.PitchAccent
                "grammar" -> DictionaryFormat.Grammar
                "names" -> DictionaryFormat.Name
                "jmdict" -> DictionaryFormat.JmDict
                else -> DictionaryFormat.Yomitan
            }
        )
    }

    /**
     * Import a dictionary from a file or directory.
     * Supports ZIP exports, a directory of term JSONs,
     * a lone index.json, or a single custom term JSON.
     */
    fun import(path: File): DictImportBundle {
        if (!path.exists()) error("Path does not exist: ${path.absolutePath}")
        if (path.isDirectory) return importDir(path)
        return when {
            path.extension.equals("zip", ignoreCase = true) -> importZip(path)
            path.name.equals("index.json", ignoreCase = true) -> importIndexFile(path)
            path.extension.equals("json", ignoreCase = true) -> importTermsFile(path)
            else -> throw IllegalArgumentException("Unsupported dictionary file: ${path.absolutePath}")
        }
    }

    private fun importTermsFile(file: File): DictImportBundle {
        val entries = parseTerms(file.readText())
        return DictImportBundle(
            result = DictImportResult(
                dictionaryId = "custom-${millis()}",
                name = file.nameWithoutExtension,
                revision = "",
                detectedFormat = DictionaryFormat.JmDict,
                entries = entries.size.toLong()
            ),
            entries = entries
        )
    }

    private fun importIndexFile(file: File): DictImportBundle {
        val meta = parseIndexMeta(file.readBytes())
        val parent = file.parentFile
        val siblings = parent?.listFiles()?.filter {
            it.extension.equals("json", true) && it.name != "index.json"
        } ?: emptyList()
        val entries = siblings.flatMap { parseTerms(it.readText()) }
        return DictImportBundle(
            result = DictImportResult(
                dictionaryId = "index-${millis()}",
                name = meta.name.ifBlank { parent?.name ?: "dictionary" },
                revision = meta.revision,
                detectedFormat = meta.format,
                entries = entries.size.toLong()
            ),
            entries = entries
        )
    }

    private fun importDir(dir: File): DictImportBundle {
        val indexFile = dir.listFiles()?.firstOrNull { it.name.equals("index.json", true) }
        val meta = indexFile?.let { parseIndexMeta(it.readBytes()) }
            ?: IndexMeta(dir.name, "", "", DictionaryFormat.Yomitan)
        val termFiles = dir.listFiles()?.filter {
            it.extension.equals("json", true) && it.name != "index.json"
        } ?: emptyList()
        val entries = termFiles.flatMap { parseTerms(it.readText()) }
        return DictImportBundle(
            result = DictImportResult(
                dictionaryId = "dir-${millis()}",
                name = meta.name.ifBlank { dir.name },
                revision = meta.revision,
                detectedFormat = meta.format,
                entries = entries.size.toLong()
            ),
            entries = entries
        )
    }

    private fun importZip(path: File): DictImportBundle {
        val entries = mutableListOf<DictionaryEntry>()
        var name = path.nameWithoutExtension
        var revision = ""
        var format = DictionaryFormat.Yomitan
        ZipInputStream(FileInputStream(path).buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".json")) {
                    val text = zis.readBytes().toString(Charsets.UTF_8)
                    if (entry.name == "index.json") {
                        val meta = parseIndexMeta(text.toByteArray())
                        name = meta.name.ifBlank { name }
                        revision = meta.revision
                        format = meta.format
                    } else {
                        entries.addAll(parseTerms(text))
                    }
                }
                entry = zis.nextEntry
            }
        }
        return DictImportBundle(
            result = DictImportResult("zip-${millis()}", name, revision, format, entries.size.toLong()),
            entries = entries
        )
    }

    /**
     * Parse Yomitan term documents. The index format is one JSON
     * array per line; each is ["spelling","reading",[{sense}],[tags],[freq]].
     */
    fun parseTerms(text: String): List<DictionaryEntry> {
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull() ?: return emptyList()
        val containers: List<JsonElement> = when (root) {
            is JsonArray -> root.toList()
            is JsonObject ->
                listOfNotNull(root["terms"], root["entries"])
                    .filterIsInstance<JsonArray>()
                    .flatMap { it }
                    .ifEmpty { listOf(root) }
            else -> return emptyList()
        }

        val out = mutableListOf<DictionaryEntry>()
        for (container in containers) {
            when (container) {
                is JsonArray -> out.addAll(parseTermTuple(container))
                is JsonObject -> out.addAll(parseTermObject(container))
                else -> Unit
            }
        }
        return out
    }

    /** Yomitan tuple form: ["spelling","reading",[...defs], [tags], [freq]]. */
    private fun parseTermTuple(arr: JsonArray): List<DictionaryEntry> {
        if (arr.size < 3) return emptyList()
        val spelling = str(arr[0])
        val readingEl = arr[1]
        val defArr = (arr[2] as? JsonArray) ?: JsonArray(emptyList())
        val tags = if (arr.size > 3) strList(arr[3]) else emptyList()

        val readingValue = (readingEl as? JsonObject)?.let { str(it["reading"]) } ?: str(readingEl)
        val senses = mutableListOf<DictionarySense>()
        val accents = mutableListOf<PitchAccent>()

        for (def in defArr) {
            val obj = def as? JsonObject ?: continue
            val pos = strList(obj["pos"])
            val glosses = strList(obj["glossary"]).ifEmpty { strList(obj["gloss"]) }
            if (glosses.isNotEmpty()) {
                senses.add(DictionarySense(partOfSpeech = pos, glosses = glosses, tags = tags))
            }
            (obj["pitch"] as? JsonArray)?.forEach { p ->
                val pObj = p as? JsonObject ?: return@forEach
                val position = (pObj["position"] as? JsonPrimitive)?.intOrNull ?: 0
                val downstep = (pObj["downstep"] as? JsonPrimitive)?.intOrNull
                accents.add(PitchAccent(position, downstep))
            }
        }

        val readingObj = readingEl as? JsonObject
        val readingInfo = strList(readingObj?.get("tags")).ifEmpty { tags }

        val entry = DictionaryEntry(
            headword = spelling,
            spellings = listOf(spelling),
            readings = listOf(
                DictionaryReading(
                    reading = readingValue,
                    readingInformation = readingInfo,
                    pitchAccents = accents
                )
            ),
            senses = senses,
            source = sourceFor(spelling, readingValue, tags),
            searchKeys = buildSearchKeys(spelling, readingValue) + listOf(spelling.lowercase())
        )
        return listOf(entry)
    }

    /** Object form: { headword, readings:[{reading,pitchAccents}], senses:[{glosses,partOfSpeech}] } */
    private fun parseTermObject(obj: JsonObject): List<DictionaryEntry> {
        val spelling = str(obj["headword"]).ifBlank { str(obj["kanji"]).ifBlank { str(obj["word"]) } }
        if (spelling.isBlank()) return emptyList()
        val reading = str(obj["reading"])
        val readingList = (obj["readings"] as? JsonArray)?.mapNotNull { r ->
            (r as? JsonObject)?.let {
                DictionaryReading(
                    reading = str(it["reading"]),
                    readingInformation = strList(it["readingInformation"]),
                    pitchAccents = (it["pitchAccents"] as? JsonArray)?.mapNotNull { p ->
                        (p as? JsonObject)?.let {
                            PitchAccent((it["position"] as? JsonPrimitive)?.intOrNull ?: 0, (it["downstep"] as? JsonPrimitive)?.intOrNull)
                        }
                    } ?: emptyList()
                )
            }
        } ?: emptyList()

        val senses = (obj["senses"] as? JsonArray)?.mapNotNull { el ->
            (el as? JsonObject)?.let {
                DictionarySense(
                    partOfSpeech = strList(it["partOfSpeech"]),
                    glosses = strList(it["glosses"]).ifEmpty { strList(it["gloss"]) },
                    tags = strList(it["tags"])
                )
            }
        } ?: emptyList()

        val readingValue = readingList.firstOrNull()?.reading ?: reading
        return listOf(
            DictionaryEntry(
                headword = spelling,
                spellings = listOf(spelling),
                readings = if (readingList.isNotEmpty()) readingList else listOf(DictionaryReading(readingValue)),
                senses = senses,
                source = sourceFor(spelling, readingValue, emptyList()),
                searchKeys = buildSearchKeys(spelling, readingValue) + listOf(spelling.lowercase())
            )
        )
    }

    private fun sourceFor(spelling: String, reading: String, tags: List<String>): DictionaryEntryType {
        if (JapaneseText.isKanji(spelling) && (tags.any { it.equals("name", true) } || reading.isBlank())) {
            return DictionaryEntryType.Name
        }
        return DictionaryEntryType.Vocabulary
    }

    private fun buildSearchKeys(spelling: String, reading: String): List<String> {
        val keys = linkedSetOf<String>()
        if (spelling.isBlank()) return emptyList()
        keys.add(spelling)
        if (reading.isNotBlank()) {
            keys.addAll(JapaneseText.kanaKeys(reading))
        }
        return keys.toList()
    }

    private fun millis(): Long = System.currentTimeMillis()
}