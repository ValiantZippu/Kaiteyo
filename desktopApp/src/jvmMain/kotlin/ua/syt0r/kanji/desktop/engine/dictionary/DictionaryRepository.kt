package ua.syt0r.kanji.desktop.engine.dictionary

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import kotlinx.datetime.Clock

// ============================================
// KAITEYO DICTIONARY REPOSITORY + ENGINE
// Owns installed dictionaries, their entries and
// the search index. Persists installed state and
// per-dictionary entry indexes to disk so they
// survive restarts without touching the app-wide
// settings engine (which is reserved for UI prefs).
// ============================================

/** Controls which matching strategies the engine applies. */
enum class ModeFlag { EXACT, PREFIX, KANA, DEINFLECT }

@JvmInline
value class SearchMode(private val bits: Int) {
    fun uses(flag: ModeFlag): Boolean = (bits and (1 shl flag.ordinal)) != 0
    fun plus(flag: ModeFlag): SearchMode = SearchMode(bits or (1 shl flag.ordinal))
    companion object {
        val Exact = SearchMode(1 shl ModeFlag.EXACT.ordinal)
        val Kana = Exact.plus(ModeFlag.KANA)
        val All: SearchMode =
            Exact.plus(ModeFlag.PREFIX).plus(ModeFlag.KANA).plus(ModeFlag.DEINFLECT)
    }
}

@Serializable
private data class InstalledDto(
    val id: String,
    val name: String,
    val revision: String = "",
    val author: String = "",
    val format: String = "Yomitan",
    val enabled: Boolean = true,
    val priority: Int = 0,
    val entryCount: Long = 0,
    val tags: List<String> = emptyList()
)

@Serializable
private data class RepositoryDto(val dictionaries: List<InstalledDto> = emptyList())

@Serializable
private data class EntryDto(
    val headword: String,
    val spellings: List<String> = emptyList(),
    val readings: List<ReadingDto> = emptyList(),
    val senses: List<SenseDto> = emptyList(),
    val searchKeys: List<String> = emptyList()
)

@Serializable
private data class ReadingDto(
    val reading: String,
    val pitchAccents: List<PitchDto> = emptyList(),
    val elements: List<String> = emptyList()
)

@Serializable
private data class PitchDto(val position: Int, val downstep: Int? = null)

@Serializable
private data class SenseDto(val partOfSpeech: List<String> = emptyList(), val glosses: List<String> = emptyList())

/** A pointer into the index: which dictionary and which headword. */
@Serializable
private data class EntryRef(val dictId: String, val headword: String)

class DictionaryRepository(private val dataDirectory: File) {

    val rootDirectory: File get() = dataDirectory

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val dictionaryFile: File get() = File(dataDirectory, "installed.json")
    private val indexDir: File get() = File(dataDirectory, "index")

    private val installed = mutableListOf<InstalledDictionary>()
    private val entriesByDict = mutableMapOf<String, MutableList<DictionaryEntry>>()
    private val keyIndex = mutableMapOf<String, MutableList<EntryRef>>()
    private var indexDirty = true

    init {
        indexDir.mkdirs()
        load()
    }

    fun installedDictionaries(): List<InstalledDictionary> = installed.sortedBy { it.priority }

    fun enabledDictionaries(): List<InstalledDictionary> = installed.filter { it.enabled }.sortedBy { it.priority }

    fun getDictionary(id: String): InstalledDictionary? = installed.firstOrNull { it.id == id }

    fun isInstalled(id: String): Boolean = installed.any { it.id == id }

    fun totalEntries(): Long = entriesByDict.values.sumOf { it.size.toLong() }

    /** Every entry across installed dictionaries (used by the segmenter). */
    fun allEntries(): List<DictionaryEntry> =
        installed.flatMap { entriesFor(it.id) }

    // ------------------------------------------------------------
    // Install / manage
    // ------------------------------------------------------------

    fun install(meta: InstalledDictionary, entries: List<DictionaryEntry>): InstalledDictionary {
        installed.removeAll { it.id == meta.id }
        entriesByDict.remove(meta.id)
        val normalized = entries.mapIndexed { i, e -> e.copy(dictionaryId = meta.id) }
        installed.add(meta.copy(entryCount = normalized.size.toLong(), createdAt = Clock.System.now()))
        entriesByDict[meta.id] = normalized.toMutableList()
        persistIndex(meta.id, normalized)
        save()
        markDirty()
        return installed.first { it.id == meta.id }
    }

    fun installImport(result: DictImportResult, entries: List<DictionaryEntry>): InstalledDictionary =
        install(
            InstalledDictionary(
                id = result.dictionaryId,
                name = result.name,
                revision = result.revision,
                format = result.detectedFormat,
                enabled = true,
                priority = installed.size
            ),
            entries
        )

    fun remove(id: String) {
        installed.removeAll { it.id == id }
        entriesByDict.remove(id)
        File(indexDir, "$id.json").delete()
        markDirty()
        save()
    }

    fun update(dictionary: InstalledDictionary) {
        val idx = installed.indexOfFirst { it.id == dictionary.id }
        if (idx == -1) return
        installed[idx] = dictionary
        markDirty()
        save()
    }

    fun reorder(ids: List<String>) {
        ids.forEachIndexed { i, id ->
            val idx = installed.indexOfFirst { it.id == id }
            if (idx != -1) installed[idx] = installed[idx].copy(priority = i)
        }
        markDirty()
        save()
    }

    // ------------------------------------------------------------
    // Search
    // ------------------------------------------------------------

    fun lookup(query: String, mode: SearchMode = SearchMode.All): List<DictionaryMatch> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        ensureIndex()
        val enabledIds = enabledDictionaries().map { it.id }.toSet()

        val keys = buildKeys(q, mode)
        val candidates = mutableListOf<DictionaryMatch>()

        for (key in keys) {
            keyIndex[key].orEmpty().forEach { ref ->
                if (ref.dictId !in enabledIds) return@forEach
                val entry = entryByRef(ref) ?: return@forEach
                candidates.add(DictionaryMatch(entry, dictionaryOf(ref.dictId), score(q, key, entry)))
            }
            if (mode.uses(ModeFlag.PREFIX) && key.length >= 2) {
                keyIndex.keys.filter { it.startsWith(key) }.take(80).forEach { k ->
                    keyIndex[k].orEmpty().forEach { ref ->
                        if (ref.dictId !in enabledIds) return@forEach
                        val entry = entryByRef(ref) ?: return@forEach
                        candidates.add(DictionaryMatch(entry, dictionaryOf(ref.dictId), score(q, k, entry) - 2))
                    }
                }
            }
        }

        if (mode.uses(ModeFlag.DEINFLECT) && !JapaneseText.isKanji(q)) {
            Deinflect.deinflect(q).forEach { cand ->
                keyIndex[cand.word].orEmpty().forEach { ref ->
                    if (ref.dictId !in enabledIds) return@forEach
                    val entry = entryByRef(ref) ?: return@forEach
                    candidates.add(DictionaryMatch(entry, dictionaryOf(ref.dictId), score = 4))
                }
            }
        }

        val unique = linkedMapOf<String, DictionaryMatch>()
        candidates.sortedByDescending { it.score }
            .forEach { m -> unique.putIfAbsent("${m.dictionary.id}|${m.entry.headword}", m) }
        return unique.values.toList()
    }

    /** Grouped lookup for rich multi-dictionary display. */
    fun lookupGrouped(query: String, mode: SearchMode = SearchMode.All): List<DictionaryResultGroup> {
        val matches = lookup(query, mode)
        return enabledDictionaries().mapNotNull { dict ->
            val own = matches.filter { it.dictionary.id == dict.id }
            if (own.isEmpty()) null else DictionaryResultGroup(dict, own)
        }
    }

    private fun score(input: String, matchedKey: String, entry: DictionaryEntry): Int {
        val key = cleaned(matchedKey)
        val q = cleaned(input)
        return when {
            entry.headword == q || entry.headword.contains(q) -> 10
            entry.spellings.any { it == q || it.contains(q) } -> 9
            entry.readings.any { cleaned(it.reading) == q } -> 8
            key == q -> 6
            else -> 5
        }
    }

    private fun cleaned(s: String): String = JapaneseText.toHiragana(s).let { it }.lowercase()

    /** Build the kana/romaji/kanji keys to probe. */
    private fun buildKeys(q: String, mode: SearchMode): List<String> {
        val set = linkedSetOf<String>()
        when {
            JapaneseText.isKanji(q) -> set.add(q)
            JapaneseText.isKana(q) -> {
                set.add(q)
                if (mode.uses(ModeFlag.KANA)) {
                    set.add(JapaneseText.toHiragana(q))
                    set.add(JapaneseText.toKatakana(q))
                }
            }
            else -> {
                set.add(q)
                val kana = JapaneseText.romajiToHiragana(q)
                if (kana.isNotBlank()) {
                    set.add(kana)
                    set.add(JapaneseText.toHiragana(kana))
                }
            }
        }
        // always also probe the katakana/hiragana equivalents of kana input
        if (JapaneseText.hasKana(q)) {
            set.add(JapaneseText.toHiragana(q))
            set.add(JapaneseText.toKatakana(q))
        }
        return set.distinct()
    }

    private fun entryByRef(ref: EntryRef): DictionaryEntry? {
        val list = entriesFor(ref.dictId)
        return list.firstOrNull { it.headword == ref.headword }
    }

    private fun dictionaryOf(id: String): InstalledDictionary =
        installed.firstOrNull { it.id == id } ?: InstalledDictionary(id, "orphan")

    private fun entriesFor(dictId: String): List<DictionaryEntry> {
        if (entriesByDict[dictId] == null) {
            entriesByDict[dictId] = loadIndex(dictId).toMutableList()
        }
        return entriesByDict[dictId] ?: emptyList()
    }

    private fun loadIndex(dictId: String): List<DictionaryEntry> {
        val file = File(indexDir, "$dictId.json")
        if (!file.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<List<EntryDto>>(file.readText()).map { dtoToEntry(it, dictId) }
        }.getOrDefault(emptyList())
    }

    private fun persistIndex(dictId: String, entries: List<DictionaryEntry>) {
        val dto = entries.map(::entryToDto)
        File(indexDir, "$dictId.json").writeText(json.encodeToString(dto))
    }

    private fun entryToDto(e: DictionaryEntry): EntryDto = EntryDto(
        headword = e.headword,
        spellings = e.spellings,
        readings = e.readings.map { r -> ReadingDto(r.reading, r.pitchAccents.map { PitchDto(it.position, it.downstep) }, r.elements) },
        senses = e.senses.map { SenseDto(it.partOfSpeech, it.glosses) },
        searchKeys = e.searchKeys
    )

    private fun dtoToEntry(d: EntryDto, dictId: String): DictionaryEntry = DictionaryEntry(
        headword = d.headword,
        spellings = d.spellings,
        readings = d.readings.map { DictionaryReading(it.reading, it.elements, it.pitchAccents.map { p -> PitchAccent(p.position, p.downstep) }) },
        senses = d.senses.map { DictionarySense(it.partOfSpeech, it.glosses) },
        searchKeys = d.searchKeys,
        dictionaryId = dictId
    )

    private fun markDirty() {
        indexDirty = true
    }

    private fun ensureIndex() {
        if (!indexDirty) return
        keyIndex.clear()
        entriesByDict.forEach { (dictId, entries) ->
            entries.forEach { e ->
                e.searchKeys.distinct().forEach { k ->
                    if (k.isNotBlank()) keyIndex.getOrPut(k) { mutableListOf() }.add(EntryRef(dictId, e.headword))
                }
            }
        }
        indexDirty = false
    }

    private fun load() {
        if (!dictionaryFile.exists()) return
        runCatching {
            val dto = json.decodeFromString<RepositoryDto>(dictionaryFile.readText())
            installed.clear()
            dto.dictionaries.forEach {
                installed.add(
                    InstalledDictionary(
                        id = it.id,
                        name = it.name,
                        revision = it.revision,
                        authoredBy = it.author,
                        format = DictionaryFormat.entries.firstOrNull { f -> f.name == it.format } ?: DictionaryFormat.Yomitan,
                        enabled = it.enabled,
                        priority = it.priority,
                        entryCount = it.entryCount,
                        tags = it.tags
                    )
                )
            }
        }
    }

    private fun save() {
        val dto = RepositoryDto(installed.map {
            InstalledDto(it.id, it.name, it.revision, it.authoredBy, it.format.name, it.enabled, it.priority, it.entryCount, it.tags)
        })
        runCatching { dictionaryFile.writeText(json.encodeToString(dto)) }
    }
}