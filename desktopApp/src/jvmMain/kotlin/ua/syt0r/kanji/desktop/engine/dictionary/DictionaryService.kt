package ua.syt0r.kanji.desktop.engine.dictionary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.desktop.data.demoKanji
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.nlp.JapaneseNlpEngine
import ua.syt0r.kanji.desktop.engine.search.SearchPipeline
import ua.syt0r.kanji.desktop.engine.search.TrigramIndex
import ua.syt0r.kanji.desktop.appstate.AppState
import java.io.File

// ============================================
// KAITEYO DICTIONARY SERVICE
// The app-facing controller for the dictionary
// engine. Owned by AppState, it keeps the search
// text, recent searches, favorites and exposes
// the importer + repository operations with
// activity logging and toasts.
// ============================================

@Serializable
private data class HistoryDto(val items: List<String> = emptyList())

@Serializable
private data class FavoritesDto(val items: List<String> = emptyList())

/** Shared path resolution so AppState can construct the service. */
fun AppState.dictionaryDataDirectory(): File =
    File(System.getProperty("user.home"), ".kaiteyo/dictionary")

class DictionaryService(val repository: DictionaryRepository) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val historyFile: File get() = File(repository.rootDirectory, "history.json")
    private val favoritesFile: File get() = File(repository.rootDirectory, "favorites.json")

    var query by mutableStateOf("")
    val recentSearches = mutableStateListOf<String>()
    val favorites = mutableStateListOf<String>()

    init {
        loadHistory()
        loadFavorites()
    }

    val installed: List<InstalledDictionary> get() = repository.installedDictionaries()
    val enabled: List<InstalledDictionary> get() = repository.enabledDictionaries()

    fun isInstalled(id: String): Boolean = repository.isInstalled(id)

    // ------------------------------------------------------------
    // Search
    // ------------------------------------------------------------

    fun lookup(query: String, mode: SearchMode = SearchMode.All): List<DictionaryResultGroup> {
        var groups = repository.lookupGrouped(query, mode)
        // NLP-enhanced: if the query is Japanese text with few results,
        // try morphological analysis to extract content words and search those too.
        if (groups.isEmpty() && JapaneseNlpEngine.isJapanese(query)) {
            val morphemes = JapaneseNlpEngine.contentWords(query)
            for (m in morphemes) {
                if (m.surface != query) {
                    val extra = repository.lookupGrouped(m.baseForm.ifBlank { m.surface }, mode)
                    groups = groups + extra.filter { eg ->
                        groups.none { it.dictionary.id == eg.dictionary.id }
                    }
                }
            }
        }
        if (query.isNotBlank()) recordSearch(query)
        return groups
    }

    // ------------------------------------------------------------
    // Index-backed suggestions (Phase 8 search pipeline)
    // ------------------------------------------------------------

    private var suggestionIndex: TrigramIndex? = null
    private var suggestionEntryCount = -1

    /**
     * Fast, index-backed headword/reading suggestions over the enabled
     * dictionaries, per STANDARDS §187 (normalize → tokenize → rank → filter):
     * a lazily rebuilt TrigramIndex supplies substring candidates and the
     * SearchPipeline ranks them Exact > Prefix > Contains > Kana. Used by the
     * lookup card to show instant candidates while typing — a cheap first
     * pass before the full grouped search resolves.
     */
    fun suggestions(query: String, limit: Int = 8): List<DictionaryMatch> {
        val q = SearchPipeline.normalize(query)
        if (q.isBlank()) return emptyList()
        val ranked = SearchPipeline.rankAndSort(q, ensureSuggestionIndex().search(q, limit = 40).map { it.first })
        return ranked
            .mapNotNull { (headword, _) -> repository.lookup(headword, SearchMode.Exact).firstOrNull() }
            .distinctBy { it.entry.headword }
            .take(limit)
    }

    private fun ensureSuggestionIndex(): TrigramIndex {
        val current = repository.allEntries()
        // Rebuild only when the corpus changes (installs/removes/reimports);
        // a count change is a cheap and reliable invalidation signal.
        if (suggestionIndex == null || current.size != suggestionEntryCount) {
            suggestionEntryCount = current.size
            suggestionIndex = TrigramIndex().apply {
                current.forEach { entry ->
                    add(entry.headword)
                    entry.spellings.forEach(::add)
                    entry.readings.forEach { add(it.reading) }
                }
            }
        }
        return suggestionIndex!!
    }

    fun lookupFlat(query: String, mode: SearchMode = SearchMode.All): List<DictionaryMatch> {
        var results = repository.lookup(query, mode)
        // NLP fallback: try base forms of content words if no results
        if (results.isEmpty() && JapaneseNlpEngine.isJapanese(query)) {
            val morphemes = JapaneseNlpEngine.contentWords(query)
            for (m in morphemes) {
                if (m.surface != query) {
                    val extra = repository.lookup(m.baseForm.ifBlank { m.surface }, mode)
                    results = (results + extra).distinctBy { it.entry.headword }
                }
            }
        }
        if (query.isNotBlank()) recordSearch(query)
        return results
    }

    fun recordSearch(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        recentSearches.remove(q)
        recentSearches.add(0, q)
        while (recentSearches.size > 50) recentSearches.removeAt(recentSearches.lastIndex)
        saveHistory()
    }

    fun clearHistory() {
        recentSearches.clear()
        saveHistory()
    }

    // ------------------------------------------------------------
    // Favorites
    // ------------------------------------------------------------

    fun isFavorite(dictId: String, headword: String): Boolean = favorites.contains(key(dictId, headword))

    fun toggleFavorite(dictId: String, headword: String) {
        val k = key(dictId, headword)
        if (favorites.contains(k)) favorites.remove(k) else favorites.add(0, k)
        saveFavorites()
    }

    fun favoriteEntries(): List<DictionaryMatch> =
        favorites.mapNotNull { k ->
            val parts = k.split("::", limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val entry = repository.lookup(parts[1]).firstOrNull { it.dictionary.id == parts[0] }
                ?: return@mapNotNull null
            entry
        }

    private fun key(dictId: String, headword: String) = "$dictId::$headword"

    // ------------------------------------------------------------
    // Install / manage / import
    // ------------------------------------------------------------

    fun importFile(file: File, state: AppState): Result<InstalledDictionary> = runCatching {
        // Try YomitanImporter v2 first for enhanced parsing (structured content,
        // frequency bands, pitch accent, kanji metadata, tag banks).
        val yomitanResult = runCatching { YomitanImporter.import(file) }.getOrNull()
        if (yomitanResult != null && yomitanResult.entries.isNotEmpty()) {
            val dict = repository.install(
                InstalledDictionary(
                    id = yomitanResult.dictionaryId,
                    name = yomitanResult.name,
                    revision = yomitanResult.revision,
                    format = yomitanResult.format,
                    enabled = true,
                    priority = repository.installedDictionaries().size,
                    tags = yomitanResult.tagDefinitions.map { it.name }
                ),
                yomitanResult.entries
            )
            val extras = buildString {
                if (yomitanResult.kanjiEntries.isNotEmpty()) append(", ${yomitanResult.kanjiEntries.size} kanji")
                if (yomitanResult.frequencyEntries.isNotEmpty()) append(", ${yomitanResult.frequencyEntries.size} frequency")
                if (yomitanResult.pitchAccentEntries.isNotEmpty()) append(", ${yomitanResult.pitchAccentEntries.size} pitch accent")
                if (yomitanResult.structuredContentCount > 0) append(", ${yomitanResult.structuredContentCount} structured content")
            }
            state.activityLog.record(
                ActivityCategory.Study,
                "Installed dictionary \"${dict.name}\" with ${dict.entryCount} entries$extras"
            )
            return@runCatching dict
        }

        // Fallback to the original DictionaryImporter for simpler formats
        val bundle = DictionaryImporter.import(file)
        if (bundle.entries.isEmpty()) {
            error("No entries could be parsed from ${file.name}. Check that the file is a Yomitan-compatible export.")
        }
        val dict = repository.installImport(bundle.result, bundle.entries)
        state.activityLog.record(
            ActivityCategory.Study,
            "Installed dictionary \"${dict.name}\" with ${dict.entryCount} entries"
        )
        dict
    }

    fun install(dict: InstalledDictionary, entries: List<DictionaryEntry>, state: AppState) {
        val installed = repository.install(dict, entries)
        state.activityLog.record(ActivityCategory.Study, "Installed dictionary \"${installed.name}\"")
    }

    fun remove(dictId: String, state: AppState) {
        val name = repository.getDictionary(dictId)?.name ?: dictId
        repository.remove(dictId)
        state.activityLog.record(ActivityCategory.Study, "Removed dictionary \"$name\"")
    }

    fun setEnabled(dictId: String, enabled: Boolean) {
        val dict = repository.getDictionary(dictId) ?: return
        repository.update(dict.copy(enabled = enabled))
    }

    fun reorder(ids: List<String>) = repository.reorder(ids)

    // ------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------

    private fun loadHistory() {
        if (!historyFile.exists()) return
        runCatching {
            val dto = json.decodeFromString<HistoryDto>(historyFile.readText())
            recentSearches.clear()
            recentSearches.addAll(dto.items.take(50))
        }
    }

    private fun saveHistory() = runCatching { historyFile.writeText(json.encodeToString(HistoryDto(recentSearches.toList()))) }

    private fun loadFavorites() {
        if (!favoritesFile.exists()) return
        runCatching {
            val dto = json.decodeFromString<FavoritesDto>(favoritesFile.readText())
            favorites.clear()
            favorites.addAll(dto.items)
        }
    }

    private fun saveFavorites() = runCatching { favoritesFile.writeText(json.encodeToString(FavoritesDto(favorites.toList()))) }

    // ------------------------------------------------------------
    // Seed: a bundled kanji dictionary so lookups work offline.
    // ------------------------------------------------------------

    companion object {
        const val SEED_DICTIONARY_ID = "kaiteyo-core-kanji"

        fun seedEntries(): List<DictionaryEntry> = demoKanji.map { seed ->
            DictionaryEntry(
                headword = seed.character,
                spellings = listOf(seed.character),
                readings = listOf(
                    DictionaryReading(
                        reading = (seed.on + seed.kun).firstOrNull().orEmpty(),
                        readingInformation = buildList {
                            if (seed.on.isNotEmpty()) add("on:${seed.on.joinToString("/")}")
                            if (seed.kun.isNotEmpty()) add("kun:${seed.kun.joinToString("/")}")
                        },
                        elements = seed.on + seed.kun
                    )
                ),
                senses = listOf(
                    DictionarySense(
                        partOfSpeech = listOf("kanji"),
                        glosses = seed.meaning.split("; ").map { it.trim() },
                        tags = buildList {
                            if (seed.jlpt > 0) add("jlpt-n${seed.jlpt}")
                            if (seed.grade > 0) add("grade-${seed.grade}")
                        }
                    )
                ),
                kanjiSpellings = listOf(
                    KanjiSpelling(
                        character = seed.character,
                        onReadings = seed.on,
                        kunReadings = seed.kun,
                        meanings = seed.meaning.split("; ").map { it.trim() },
                        strokeCounts = listOf(seed.strokes),
                        jlpt = seed.jlpt,
                        grade = seed.grade,
                        frequency = seed.freq,
                        radicals = seed.radicals
                    )
                ),
                frequency = FrequencyInfo(rank = seed.freq.takeIf { it > 0 }),
                source = DictionaryEntryType.Kanji,
                searchKeys = buildSet {
                    add(seed.character)
                    addAll(seed.on.flatMap { JapaneseText.kanaKeys(it) })
                    addAll(seed.kun.flatMap { JapaneseText.kanaKeys(it) })
                    addAll(seed.radicals)
                }.toList()
            )
        }

        fun seedMeta(): InstalledDictionary = InstalledDictionary(
            id = SEED_DICTIONARY_ID,
            name = "Kaiteyo Core Kanji",
            revision = "1.0",
            authoredBy = "Kaiteyo",
            format = DictionaryFormat.Custom,
            priority = 0,
            tags = listOf("builtin", "kanji", "jlpt", "grade")
        )
    }
}