package ua.syt0r.kanji.desktop.engine.jdata.api

import ua.syt0r.kanji.desktop.engine.jdata.engine.RelationshipGraph
import ua.syt0r.kanji.desktop.engine.jdata.model.Bounds
import ua.syt0r.kanji.desktop.engine.jdata.model.ComponentEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.EntityType
import ua.syt0r.kanji.desktop.engine.jdata.model.FrequencyValue
import ua.syt0r.kanji.desktop.engine.jdata.model.FuriganaSegment
import ua.syt0r.kanji.desktop.engine.jdata.model.KanjiEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.PlatformData
import ua.syt0r.kanji.desktop.engine.jdata.model.RadicalEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.ReadingInfo
import ua.syt0r.kanji.desktop.engine.jdata.model.RelationEdge
import ua.syt0r.kanji.desktop.engine.jdata.model.SourceRef
import ua.syt0r.kanji.desktop.engine.jdata.model.StableIds
import ua.syt0r.kanji.desktop.engine.jdata.model.StrokeEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.StrokeSet
import ua.syt0r.kanji.desktop.engine.jdata.model.VocabEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.VocabSense
import ua.syt0r.kanji.desktop.engine.jdata.search.SearchFilters
import ua.syt0r.kanji.desktop.engine.jdata.search.SearchHit
import ua.syt0r.kanji.desktop.engine.jdata.search.SearchIndex
import ua.syt0r.kanji.desktop.engine.jdata.search.Suggestion

// ============================================================
// LANGUAGE DATABASE — PUBLIC API
// `database.open(data)`, then `lookup() / search() / getKanji() /
// getStrokeData() / getProvenance() / …`. Compose-free, Kaiteyo-free,
// AppState-free: any JVM/Kotlin application can consume the platform.
// All accessors are pure reads over immutable data — thread-safe by
// construction (nothing mutates after construction).
// ============================================================

class LanguageDatabase private constructor(
    private val data: PlatformData,
    private val search: SearchIndex
) {

    val schemaVersion: Int get() = data.schemaVersion
    val generatedAt: String get() = data.generatedAt
    val sources: Map<String, ua.syt0r.kanji.desktop.engine.jdata.source.SourceDefinition> get() = data.sources

    companion object {
        fun open(data: PlatformData): LanguageDatabase =
            LanguageDatabase(data, SearchIndex(data))
    }

    // ------------------------------------------------------------
    // Search
    // ------------------------------------------------------------

    fun lookup(expression: String): List<SearchHit> = search.search(expression).take(10)

    fun search(query: String, filters: SearchFilters = SearchFilters()): List<SearchHit> =
        search.search(query, filters)

    fun autocomplete(prefix: String, limit: Int = 10): List<Suggestion> =
        search.autocomplete(prefix, limit)

    // ------------------------------------------------------------
    // Kanji
    // ------------------------------------------------------------

    fun getKanji(character: String): KanjiEntry? = data.kanji[StableIds.kanji(character)]

    fun getKanjiById(id: String): KanjiEntry? = data.kanji[id]

    fun kanjiForVocab(vocabId: String): List<KanjiEntry> =
        RelationshipGraph.neighbours(data.relations, vocabId, "contains")
            .mapNotNull { data.kanji[it] }

    // ------------------------------------------------------------
    // Vocabulary
    // ------------------------------------------------------------

    fun getVocabulary(expression: String): List<VocabEntry> =
        data.vocab.values.filter { it.expression == expression }.sortedBy { it.id }

    fun getVocabularyById(id: String): VocabEntry? = data.vocab[id]

    fun vocabForKanji(character: String): List<VocabEntry> =
        RelationshipGraph.neighbours(data.relations, StableIds.kanji(character), "appears-in")
            .mapNotNull { data.vocab[it] }
            .sortedBy { it.id }

    // ------------------------------------------------------------
    // Readings / meanings / senses
    // ------------------------------------------------------------

    fun getReadings(id: String): List<ReadingInfo> = data.vocab[id]?.readings ?: emptyList()

    fun getMeanings(id: String): List<String> {
        data.vocab[id]?.let { return it.allGlosses }
        data.kanji[id]?.let { return it.meanings }
        return emptyList()
    }

    fun getSenses(id: String): List<VocabSense> = data.vocab[id]?.senses ?: emptyList()

    fun getFurigana(id: String): List<FuriganaSegment> = data.vocab[id]?.furigana ?: emptyList()

    // ------------------------------------------------------------
    // Strokes — UI-independent geometry API
    // ------------------------------------------------------------

    fun getStrokeData(character: String): StrokeSet? = data.strokeSets[StableIds.strokeSet(character)]

    fun getStroke(character: String, index: Int): StrokeEntry? =
        data.strokeSets[StableIds.strokeSet(character)]?.strokes?.firstOrNull { it.index == index }

    fun getStrokeBounds(character: String): Bounds? =
        data.strokeSets[StableIds.strokeSet(character)]?.strokes?.mapNotNull { it.bounds }?.let { list ->
            if (list.isEmpty()) null
            else Bounds(
                minX = list.minOf { it.minX },
                minY = list.minOf { it.minY },
                maxX = list.maxOf { it.maxX },
                maxY = list.maxOf { it.maxY }
            )
        }

    fun getStrokePath(character: String, index: Int): String? =
        data.strokeSets[StableIds.strokeSet(character)]?.strokes?.firstOrNull { it.index == index }?.path

    fun getStrokeOrder(character: String): List<Int> =
        data.strokeSets[StableIds.strokeSet(character)]?.strokeOrder ?: emptyList()

    // ------------------------------------------------------------
    // Radicals / components
    // ------------------------------------------------------------

    fun getRadical(character: String): RadicalEntry? {
        val radicalId = getKanji(character)?.radicalId ?: return null
        return data.radicals[radicalId]
    }

    fun getComponents(character: String): List<ComponentEntry> =
        RelationshipGraph.neighbours(data.relations, StableIds.kanji(character), "component")
            .mapNotNull { data.components[it] }

    // ------------------------------------------------------------
    // JLPT / frequency / provenance / relations
    // ------------------------------------------------------------

    fun getJLPT(id: String): Int? {
        data.kanji[id]?.let { return it.jlpt }
        data.vocab[id]?.let { return it.jlpt }
        return null
    }

    fun getFrequency(id: String): List<FrequencyValue> {
        data.vocab[id]?.let { return it.frequencies }
        data.kanji[id]?.frequencyRank?.let { rank ->
            return listOf(FrequencyValue("kanji-frequency", rank = rank))
        }
        return emptyList()
    }

    fun getProvenance(id: String): List<SourceRef> {
        data.kanji[id]?.let { return it.sources }
        data.kana[id]?.let { return it.sources }
        data.vocab[id]?.let { return it.sources }
        data.radicals[id]?.let { return it.sources }
        return emptyList()
    }

    fun getRelations(id: String): List<RelationEdge> =
        RelationshipGraph.edgesFrom(data.relations, id) + RelationshipGraph.edgesTo(data.relations, id)

    // ------------------------------------------------------------
    // Stats
    // ------------------------------------------------------------

    fun stats(): DatabaseStats = DatabaseStats(
        schemaVersion = data.schemaVersion,
        recordCounts = data.recordCounts,
        totalEntries = data.totalEntries,
        kanjiWithStrokes = data.strokeSets.values.count { set ->
            data.kanji.containsKey(StableIds.kanji(set.character)) || data.kana.containsKey(StableIds.kana(set.character))
        }
    )
}

data class DatabaseStats(
    val schemaVersion: Int,
    val recordCounts: Map<String, Int>,
    val totalEntries: Int,
    val kanjiWithStrokes: Int
)
