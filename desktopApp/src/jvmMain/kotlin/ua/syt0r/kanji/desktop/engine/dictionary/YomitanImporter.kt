package ua.syt0r.kanji.desktop.engine.dictionary

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.doubleOrNull
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

// ============================================
// KAITEYO YOMITAN IMPORTER v2
// Full-featured Yomitan dictionary importer
// with structured content, frequency bands,
// pitch accent, kanji metadata, tag banks,
// and multi-format support. Extends the original
// DictionaryImporter with Yomitan v2/v3 features.
// ============================================

/** Frequency band classification. */
enum class FrequencyBand(val label: String, val minRank: Int, val maxRank: Int) {
    VeryCommon("Very Common", 1, 5000),
    Common("Common", 5001, 15000),
    Uncommon("Uncommon", 15001, 50000),
    Rare("Rare", 50001, 150000),
    VeryRare("Very Rare", 150001, Int.MAX_VALUE);

    companion object {
        fun fromRank(rank: Int): FrequencyBand = when {
            rank <= 5000 -> VeryCommon
            rank <= 15000 -> Common
            rank <= 50000 -> Uncommon
            rank <= 150000 -> Rare
            else -> VeryRare
        }
    }
}

/** A tag definition from tag_bank. */
data class TagDefinition(
    val name: String,
    val category: String,
    val sortOrder: Int = 0,
    val notes: String = "",
    val isCommon: Boolean = false
)

/** Frequency entry with source and rank. */
data class FrequencyEntry(
    val expression: String,
    val reading: String,
    val rank: Int,
    val source: String = "",
    val band: FrequencyBand = FrequencyBand.VeryRare
)

/** Pitch accent entry with dictionary form and pattern. */
data class PitchAccentEntry(
    val expression: String,
    val reading: String,
    val position: Int,
    val dialect: String = "",
    val patterns: List<PitchAccentPattern> = emptyList()
)

data class PitchAccentPattern(
    val position: Int,
    val pattern: String // e.g., "LHH", "LHL"
)

/** Structured content node (Yomitan v2+). */
sealed class StructuredContent {
    data class Text(val text: String) : StructuredContent()
    data class Element(
        val tag: String,
        val attributes: Map<String, String> = emptyMap(),
        val children: List<StructuredContent> = emptyList()
    ) : StructuredContent()
    data class Link(
        val href: String,
        val title: String = "",
        val children: List<StructuredContent> = emptyList()
    ) : StructuredContent()
    data class Image(
        val path: String,
        val width: Int = 0,
        val height: Int = 0
    ) : StructuredContent()
}

/** Kanji dictionary entry from kanji_bank. */
data class KanjiDictionaryEntry(
    val character: String,
    val onReadings: List<String> = emptyList(),
    val kunReadings: List<String> = emptyList(),
    val meanings: List<String> = emptyList(),
    val strokeCounts: List<Int> = emptyList(),
    val jlpt: Int? = null,
    val grade: Int? = null,
    val frequency: Int? = null,
    val radicals: List<String> = emptyList(),
    val dictionaryId: String = ""
)

/** Extended import result with all Yomitan data. */
data class YomitanImportResult(
    val dictionaryId: String,
    val name: String,
    val revision: String,
    val format: DictionaryFormat,
    val entries: List<DictionaryEntry>,
    val kanjiEntries: List<KanjiDictionaryEntry>,
    val frequencyEntries: List<FrequencyEntry>,
    val pitchAccentEntries: List<PitchAccentEntry>,
    val tagDefinitions: List<TagDefinition>,
    val structuredContentCount: Int,
    val totalEntries: Long
)

// ----------------------------------------------------------
// Parser state
// ----------------------------------------------------------

private data class ParseState(
    var entries: MutableList<DictionaryEntry> = mutableListOf(),
    var kanjiEntries: MutableList<KanjiDictionaryEntry> = mutableListOf(),
    var frequencyEntries: MutableList<FrequencyEntry> = mutableListOf(),
    var pitchAccentEntries: MutableList<PitchAccentEntry> = mutableListOf(),
    var tagDefinitions: MutableList<TagDefinition> = mutableListOf(),
    var structuredContentCount: Int = 0
)

object YomitanImporter {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun str(el: JsonElement?): String = (el as? JsonPrimitive)?.content ?: ""
    private fun int(el: JsonElement?): Int? = (el as? JsonPrimitive)?.intOrNull
    private fun double(el: JsonElement?): Double? = (el as? JsonPrimitive)?.doubleOrNull
    private fun bool(el: JsonElement?): Boolean? = (el as? JsonPrimitive)?.booleanOrNull

    private fun strList(el: JsonElement?): List<String> {
        return when (el) {
            is JsonArray -> el.mapNotNull { (it as? JsonPrimitive)?.content }
            is JsonPrimitive -> listOf(el.content)
            else -> emptyList()
        }
    }

    // ----------------------------------------------------------
    // Public API
    // ----------------------------------------------------------

    /** Import a Yomitan dictionary from file or directory. */
    fun import(path: File): YomitanImportResult {
        if (!path.exists()) error("Path does not exist: ${path.absolutePath}")
        return if (path.isDirectory) importDirectory(path) else importFile(path)
    }

    // ----------------------------------------------------------
    // ZIP import
    // ----------------------------------------------------------

    private fun importFile(file: File): YomitanImportResult {
        return when {
            file.extension.equals("zip", true) -> importZip(file)
            file.name.equals("index.json", true) -> importIndexFile(file)
            file.extension.equals("json", true) -> importSingleJson(file)
            else -> error("Unsupported file: ${file.name}")
        }
    }

    private fun importZip(path: File): YomitanImportResult {
        val state = ParseState()
        var name = path.nameWithoutExtension
        var revision = ""
        var format = DictionaryFormat.Yomitan

        ZipInputStream(FileInputStream(path).buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".json")) {
                    val text = zis.readBytes().toString(Charsets.UTF_8)
                    when {
                        entry.name.equals("index.json", true) -> {
                            val meta = parseIndex(text)
                            name = meta.first.ifBlank { name }
                            revision = meta.second
                            format = meta.third
                        }
                        entry.name.contains("term_bank") -> {
                            parseTermBank(text, state)
                        }
                        entry.name.contains("term_meta_bank") || entry.name.contains("freq") -> {
                            parseFrequencyBank(text, state)
                        }
                        entry.name.contains("kanji_bank") -> {
                            parseKanjiBank(text, state)
                        }
                        entry.name.contains("kanji_meta_bank") -> {
                            parseKanjiMetaBank(text, state)
                        }
                        entry.name.contains("tag_bank") -> {
                            parseTagBank(text, state)
                        }
                    }
                }
                entry = zis.nextEntry
            }
        }

        return buildResult(path.nameWithoutExtension.hashCode().toString(), name, revision, format, state)
    }

    // ----------------------------------------------------------
    // Directory import
    // ----------------------------------------------------------

    private fun importDirectory(dir: File): YomitanImportResult {
        val state = ParseState()
        var name = dir.name
        var revision = ""
        var format = DictionaryFormat.Yomitan

        dir.listFiles()?.forEach { file ->
            if (!file.isFile || !file.name.endsWith(".json")) return@forEach
            val text = file.readText()
            when {
                file.name.equals("index.json", true) -> {
                    val meta = parseIndex(text)
                    name = meta.first.ifBlank { name }
                    revision = meta.second
                    format = meta.third
                }
                file.name.contains("term_bank") -> parseTermBank(text, state)
                file.name.contains("term_meta_bank") || file.name.contains("freq") -> parseFrequencyBank(text, state)
                file.name.contains("kanji_bank") -> parseKanjiBank(text, state)
                file.name.contains("kanji_meta_bank") -> parseKanjiMetaBank(text, state)
                file.name.contains("tag_bank") -> parseTagBank(text, state)
            }
        }

        return buildResult("dir-${dir.absolutePath.hashCode().toUInt().toString(16)}", name, revision, format, state)
    }

    // ----------------------------------------------------------
    // Index + single JSON import
    // ----------------------------------------------------------

    private fun importIndexFile(file: File): YomitanImportResult {
        val (name, revision, format) = parseIndex(file.readText())
        val state = ParseState()
        file.parentFile?.listFiles()?.filter {
            it.extension.equals("json", true) && it.name != "index.json"
        }?.forEach { f ->
            val text = f.readText()
            when {
                f.name.contains("term_bank") -> parseTermBank(text, state)
                f.name.contains("term_meta_bank") || f.name.contains("freq") -> parseFrequencyBank(text, state)
                f.name.contains("kanji_bank") -> parseKanjiBank(text, state)
                f.name.contains("tag_bank") -> parseTagBank(text, state)
            }
        }
        return buildResult("idx-${file.absolutePath.hashCode().toUInt().toString(16)}", name, revision, format, state)
    }

    private fun importSingleJson(file: File): YomitanImportResult {
        val state = ParseState()
        val text = file.readText()
        parseTermBank(text, state)
        return buildResult(
            "json-${file.absolutePath.hashCode().toUInt().toString(16)}",
            file.nameWithoutExtension, "", DictionaryFormat.Yomitan, state
        )
    }

    // ----------------------------------------------------------
    // Index parsing
    // ----------------------------------------------------------

    private fun parseIndex(text: String): Triple<String, String, DictionaryFormat> {
        val obj = runCatching {
            json.parseToJsonElement(text).jsonObject
        }.getOrNull() ?: return Triple("", "", DictionaryFormat.Yomitan)

        val format = when (str(obj["format"]).lowercase()) {
            "kanjidic2", "kanjidic" -> DictionaryFormat.KanjiDic
            "frequency" -> DictionaryFormat.Frequency
            "pitch accent", "pitch_accent" -> DictionaryFormat.PitchAccent
            "grammar" -> DictionaryFormat.Grammar
            "names" -> DictionaryFormat.Name
            "jmdict" -> DictionaryFormat.JmDict
            else -> DictionaryFormat.Yomitan
        }

        return Triple(
            str(obj["title"]).ifBlank { str(obj["name"]) },
            str(obj["revision"]),
            format
        )
    }

    // ----------------------------------------------------------
    // Term bank parsing (Yomitan v3 array format)
    // ----------------------------------------------------------

    private fun parseTermBank(text: String, state: ParseState) {
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull() ?: return
        val items = when (root) {
            is JsonArray -> root
            is JsonObject -> listOf(root)
            else -> return
        }

        for (item in items) {
            when (item) {
                is JsonArray -> parseTermTupleV3(item, state)
                is JsonObject -> parseTermObject(item, state)
                else -> Unit
            }
        }
    }

    /** Yomitan object format: {"expression":..., "reading":..., " senses":[...]} */
    private fun parseTermObject(obj: JsonObject, state: ParseState) {
        val expression = str(obj["expression"]).ifBlank { str(obj["word"]) }
        if (expression.isBlank()) return
        val reading = str(obj["reading"]).ifBlank { expression }
        val tags = strList(obj["tags"])
        val score = int(obj["score"])
        val seq = int(obj["seq"])

        val sensesArr = obj["senses"] as? JsonArray ?: obj["definitions"] as? JsonArray
        val senses = mutableListOf<DictionarySense>()
        if (sensesArr != null) {
            for (senseEl in sensesArr) {
                val senseObj = senseEl as? JsonObject ?: continue
                val pos = strList(senseObj["pos"])
                val glossary = strList(senseObj["glossary"]).ifEmpty { strList(senseObj["gloss"]) }
                val senseTags = strList(senseObj["tags"])
                if (glossary.isNotEmpty() || pos.isNotEmpty()) {
                    senses.add(
                        DictionarySense(
                            partOfSpeech = pos,
                            glosses = glossary,
                            tags = senseTags + tags
                        )
                    )
                }
            }
        }

        state.entries.add(
            DictionaryEntry(
                headword = expression,
                spellings = listOf(expression),
                readings = listOf(
                    DictionaryReading(
                        reading = reading,
                        readingInformation = tags
                    )
                ),
                senses = senses,
                frequency = FrequencyInfo(rank = score),
                searchKeys = buildSearchKeys(expression, reading),
                source = if (JapaneseText.isKanji(expression)) DictionaryEntryType.Name else DictionaryEntryType.Vocabulary
            )
        )
    }

    /**
     * Yomitan v3 tuple: [expression, reading, [senses], tags, score, seq, termTags,
     *                   deinfRules, structuredContent, ...]
     */
    private fun parseTermTupleV3(arr: JsonArray, state: ParseState) {
        if (arr.size < 3) return

        val expression = str(arr[0])
        val readingEl = arr[1]
        val sensesArr = arr[2] as? JsonArray ?: return
        val tags = if (arr.size > 3) strList(arr[3]) else emptyList()
        val score = if (arr.size > 4) double(arr[4])?.toInt() else null
        val seq = if (arr.size > 5) int(arr[5]) else null
        val termTags = if (arr.size > 6) strList(arr[6]) else emptyList()

        val reading = (readingEl as? JsonObject)?.let { str(it["reading"]) }
            ?: (readingEl as? JsonPrimitive)?.content
            ?: expression

        val senses = mutableListOf<DictionarySense>()
        val allAccents = mutableListOf<PitchAccent>()
        var structuredContent: StructuredContent? = null

        for (senseEl in sensesArr) {
            val senseObj = senseEl as? JsonObject ?: continue
            val pos = strList(senseObj["pos"])
            val glossary = strList(senseObj["glossary"]).ifEmpty { strList(senseObj["gloss"]) }
            val senseTags = strList(senseObj["tags"])
            val restrictions = strList(senseObj["restr"])
            val crossRefs = strList(senseObj["xref"]).ifEmpty { strList(senseObj["l"]) }

            if (glossary.isNotEmpty() || pos.isNotEmpty()) {
                senses.add(
                    DictionarySense(
                        partOfSpeech = pos,
                        glosses = glossary,
                        tags = senseTags + tags,
                        restrictions = restrictions,
                        crossReferences = crossRefs
                    )
                )
            }

            // Extract pitch accent from structured content if present
            (senseObj["pitch"] as? JsonArray)?.forEach { p ->
                val pObj = p as? JsonObject ?: return@forEach
                val position = int(pObj["position"]) ?: 0
                val downstep = int(pObj["downstep"])
                allAccents.add(PitchAccent(position, downstep))
            }
        }

        // Parse structured content (Yomitan v2+)
        if (arr.size > 8 && arr[8] != null) {
            structuredContent = parseStructuredContent(arr[8])
            if (structuredContent != null) state.structuredContentCount++
        }

        // Parse additional data fields
        val pitchAccents = if (arr.size > 9) {
            parsePitchFromData(arr[9])
        } else allAccents

        val readingInfo = strList((readingEl as? JsonObject)?.get("tags"))

        state.entries.add(
            DictionaryEntry(
                headword = expression,
                spellings = listOf(expression),
                readings = listOf(
                    DictionaryReading(
                        reading = reading,
                        readingInformation = readingInfo + termTags,
                        pitchAccents = pitchAccents
                    )
                ),
                senses = senses,
                frequency = FrequencyInfo(rank = score),
                searchKeys = buildSearchKeys(expression, reading),
                source = if (JapaneseText.isKanji(expression)) DictionaryEntryType.Name else DictionaryEntryType.Vocabulary
            )
        )
    }

    // ----------------------------------------------------------
    // Structured content parser
    // ----------------------------------------------------------

    private fun parseStructuredContent(el: JsonElement?): StructuredContent? {
        return when (el) {
            null -> null
            is JsonPrimitive -> StructuredContent.Text(el.content)
            is JsonArray -> {
                if (el.size == 2) {
                    // Link form: ["href", {content}]
                    val href = str(el[0])
                    val content = parseStructuredContent(el[1])
                    if (href.isNotBlank()) {
                        StructuredContent.Link(href, children = listOfNotNull(content))
                    } else content
                } else if (el.size == 3 && str(el[0]) == "img") {
                    // Image form: ["img", {attributes}]
                    val attrs = (el[1] as? JsonObject)?.mapValues { str(it.value) } ?: emptyMap()
                    StructuredContent.Image(
                        path = attrs["path"] ?: "",
                        width = int(el[2]) ?: 0
                    )
                } else {
                    // Array of children
                    StructuredContent.Element(
                        tag = "div",
                        children = el.mapNotNull { parseStructuredContent(it) }
                    )
                }
            }
            is JsonObject -> {
                val tag = str(el["tag"]).ifBlank { "span" }
                val attrs = mutableMapOf<String, String>()
                (el["data"] as? JsonObject)?.forEach { (k, v) -> attrs[k] = str(v) }
                val children = (el["content"] as? JsonArray)?.mapNotNull { parseStructuredContent(it) }
                    ?: listOfNotNull(el["content"]?.let { parseStructuredContent(it) })

                if (tag == "a" || el.containsKey("href")) {
                    StructuredContent.Link(
                        href = str(el["href"]),
                        title = str(el["title"]),
                        children = children
                    )
                } else {
                    StructuredContent.Element(tag, attrs, children)
                }
            }
            else -> null
        }
    }

    // ----------------------------------------------------------
    // Frequency bank parsing
    // ----------------------------------------------------------

    private fun parseFrequencyBank(text: String, state: ParseState) {
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull() ?: return
        val items = when (root) {
            is JsonArray -> root
            is JsonObject -> listOf(root)
            else -> return
        }

        for (item in items) {
            if (item !is JsonArray || item.size < 2) continue
            val expression = str(item[0])
            val rank = int(item[1]) ?: continue
            val source = if (item.size > 2) str(item[2]) else ""

            state.frequencyEntries.add(
                FrequencyEntry(
                    expression = expression,
                    reading = expression,
                    rank = rank,
                    source = source,
                    band = FrequencyBand.fromRank(rank)
                )
            )
        }
    }

    // ----------------------------------------------------------
    // Kanji bank parsing
    // ----------------------------------------------------------

    private fun parseKanjiBank(text: String, state: ParseState) {
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull() ?: return
        val items = when (root) {
            is JsonArray -> root
            else -> return
        }

        for (item in items) {
            if (item !is JsonArray || item.size < 2) continue
            val character = str(item[0])
            val onReadings = strList(item[1])
            val kunReadings = if (item.size > 2) strList(item[2]) else emptyList()
            val meanings = if (item.size > 3) strList(item[3]) else emptyList()

            state.kanjiEntries.add(
                KanjiDictionaryEntry(
                    character = character,
                    onReadings = onReadings,
                    kunReadings = kunReadings,
                    meanings = meanings
                )
            )
        }
    }

    private fun parseKanjiMetaBank(text: String, state: ParseState) {
        // Parse kanji frequency and radical metadata
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull() ?: return
        val items = when (root) {
            is JsonArray -> root
            else -> return
        }

        for (item in items) {
            if (item !is JsonArray || item.size < 3) continue
            val character = str(item[0])
            val type = str(item[1])
            val value = item[2]

            when (type) {
                "freq" -> {
                    val rank = int(value) ?: continue
                    // Update existing kanji entry with frequency
                    state.kanjiEntries.find { it.character == character }?.let {
                        val idx = state.kanjiEntries.indexOf(it)
                        state.kanjiEntries[idx] = it.copy(frequency = rank)
                    }
                }
                "rad" -> {
                    val radicals = strList(value)
                    state.kanjiEntries.find { it.character == character }?.let {
                        val idx = state.kanjiEntries.indexOf(it)
                        state.kanjiEntries[idx] = it.copy(radicals = radicals)
                    }
                }
                "jlpt" -> {
                    val jlpt = int(value) ?: continue
                    state.kanjiEntries.find { it.character == character }?.let {
                        val idx = state.kanjiEntries.indexOf(it)
                        state.kanjiEntries[idx] = it.copy(jlpt = jlpt)
                    }
                }
                "grade" -> {
                    val grade = int(value) ?: continue
                    state.kanjiEntries.find { it.character == character }?.let {
                        val idx = state.kanjiEntries.indexOf(it)
                        state.kanjiEntries[idx] = it.copy(grade = grade)
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------
    // Tag bank parsing
    // ----------------------------------------------------------

    private fun parseTagBank(text: String, state: ParseState) {
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull() ?: return
        val items = when (root) {
            is JsonArray -> root
            else -> return
        }

        for (item in items) {
            if (item !is JsonArray || item.size < 2) continue
            val name = str(item[0])
            val category = str(item[1])
            val sortOrder = if (item.size > 2) int(item[2]) ?: 0 else 0
            val notes = if (item.size > 3) str(item[3]) else ""
            val isCommon = if (item.size > 4) bool(item[4]) ?: false else false

            state.tagDefinitions.add(
                TagDefinition(name, category, sortOrder, notes, isCommon)
            )
        }
    }

    // ----------------------------------------------------------
    // Pitch accent data extraction
    // ----------------------------------------------------------

    private fun parsePitchFromData(el: JsonElement?): List<PitchAccent> {
        if (el == null) return emptyList()
        return when (el) {
            is JsonArray -> {
                el.mapNotNull { item ->
                    val obj = item as? JsonObject ?: return@mapNotNull null
                    PitchAccent(
                        position = int(obj["position"]) ?: 0,
                        downstep = int(obj["downstep"])
                    )
                }
            }
            else -> emptyList()
        }
    }

    // ----------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------

    private fun buildSearchKeys(spelling: String, reading: String): List<String> {
        val keys = linkedSetOf<String>()
        if (spelling.isBlank()) return emptyList()
        keys.add(spelling)
        if (reading.isNotBlank() && reading != spelling) {
            keys.addAll(JapaneseText.kanaKeys(reading))
        }
        return keys.toList()
    }

    private fun buildResult(
        id: String,
        name: String,
        revision: String,
        format: DictionaryFormat,
        state: ParseState
    ): YomitanImportResult {
        // Merge frequency data into entries
        val freqByExpression = state.frequencyEntries.groupBy { it.expression }
        val updatedEntries = state.entries.map { entry ->
            val freqs = freqByExpression[entry.headword]
            val bestFreq = freqs?.minByOrNull { it.rank }
            if (bestFreq != null) {
                entry.copy(frequency = FrequencyInfo(rank = bestFreq.rank))
            } else entry
        }

        return YomitanImportResult(
            dictionaryId = id,
            name = name,
            revision = revision,
            format = format,
            entries = updatedEntries,
            kanjiEntries = state.kanjiEntries,
            frequencyEntries = state.frequencyEntries,
            pitchAccentEntries = state.pitchAccentEntries,
            tagDefinitions = state.tagDefinitions,
            structuredContentCount = state.structuredContentCount,
            totalEntries = updatedEntries.size.toLong() + state.kanjiEntries.size
        )
    }
}
