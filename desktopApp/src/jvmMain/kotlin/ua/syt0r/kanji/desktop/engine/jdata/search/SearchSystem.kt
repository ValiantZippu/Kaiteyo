package ua.syt0r.kanji.desktop.engine.jdata.search

import ua.syt0r.kanji.desktop.engine.jdata.model.EntityType
import ua.syt0r.kanji.desktop.engine.jdata.model.PlatformData
import ua.syt0r.kanji.desktop.engine.jdata.normalize.Normalizer

// ============================================================
// SEARCH SYSTEM
// An in-memory inverted index over the canonical dataset:
//   - expression keys (kanji, kana, vocab)
//   - reading keys (hiragana + katakana + kanaKeys)
//   - meaning/gloss tokens (for gloss search)
// Query normalization is non-destructive; ranking is a replaceable
// strategy; autocomplete works off the same index with frequency
// ordering. No full-table scans — every lookup is index-driven.
// ============================================================

/** Filters applied to search results. Nulls mean "no filter". */
data class SearchFilters(
    val entityTypes: Set<EntityType>? = null,
    val jlpt: Int? = null,
    val maxFrequencyRank: Int? = null,
    val readingContains: String? = null,
    val sourceIds: Set<String>? = null,
    val partOfSpeech: Set<String>? = null
)

data class SearchHit(
    val entityType: EntityType,
    val id: String,
    val text: String,
    val reading: String = "",
    val gloss: String = "",
    val score: Float = 0f,
    val sourceIds: List<String> = emptyList(),
    val jlpt: Int? = null,
    val frequencyRank: Int? = null,
    val partOfSpeech: List<String> = emptyList()
)

data class Suggestion(
    val text: String,
    val reading: String = "",
    val entityType: EntityType,
    val id: String,
    val score: Float = 0f
)

/** How a candidate matched the query — the backbone of ranking. */
enum class Exactness { EXACT, PREFIX, READING, KANA, MEANING }

/** Context handed to a ranking strategy. */
data class RankingContext(
    val exactness: Exactness,
    val sourcePriority: Int = 0,
    val frequencyRank: Int? = null,
    val entityType: EntityType = EntityType.VOCAB
)

/** Replaceable ranking strategy — swap without touching the search API. */
interface RankingStrategy {
    fun rank(query: String, hit: SearchHit, context: RankingContext): Float
}

/**
 * Default strategy: exact > prefix > reading > kana-fold > meaning,
 * boosted by frequency (lower rank = better) and source priority.
 */
class DefaultRankingStrategy : RankingStrategy {

    override fun rank(query: String, hit: SearchHit, context: RankingContext): Float {
        var score = when (context.exactness) {
            Exactness.EXACT -> 100f
            Exactness.PREFIX -> 70f
            Exactness.READING -> 55f
            Exactness.KANA -> 45f
            Exactness.MEANING -> 30f
        }
        context.frequencyRank?.let { rank ->
            if (rank > 0) score += (5_000f / rank).coerceAtMost(40f)
        }
        score += context.sourcePriority * 2f
        when (context.entityType) {
            EntityType.KANJI -> score += 6f
            EntityType.KANA -> score += 4f
            EntityType.VOCAB -> score += 2f
            else -> Unit
        }
        return score
    }
}

/** Frequency-first ranking (used by frequency decks / lookups). */
class FrequencyRankingStrategy : RankingStrategy {
    override fun rank(query: String, hit: SearchHit, context: RankingContext): Float {
        val base = DefaultRankingStrategy().rank(query, hit, context)
        val rankBoost = context.frequencyRank?.let { rank ->
            if (rank > 0) (10_000f / rank).coerceAtMost(60f) else 0f
        } ?: 0f
        return base + rankBoost
    }
}

class SearchIndex(
    private val data: PlatformData,
    private val ranking: RankingStrategy = DefaultRankingStrategy()
) {

    private class HitRef(
        val type: EntityType,
        val id: String,
        val text: String,
        val reading: String,
        val gloss: String,
        val jlpt: Int?,
        val frequencyRank: Int?,
        val sourceIds: List<String>,
        val partOfSpeech: List<String>,
        val priority: Int
    )

    private val keysToHits = mutableMapOf<String, MutableList<HitRef>>()
    private val meaningTokens = mutableMapOf<String, MutableList<HitRef>>()
    private val hitById = mutableMapOf<String, HitRef>()

    init {
        build()
    }

    private fun build() {
        data.kanji.values.forEach { entry ->
            val ref = HitRef(
                type = EntityType.KANJI, id = entry.id, text = entry.character,
                reading = (entry.onReadings + entry.kunReadings).firstOrNull() ?: "",
                gloss = entry.meanings.joinToString("; "),
                jlpt = entry.jlpt, frequencyRank = entry.frequencyRank,
                sourceIds = entry.sources.map { it.sourceId },
                partOfSpeech = emptyList(), priority = 0
            )
            addKey(Normalizer.searchKey(entry.character), ref)
            (entry.onReadings + entry.kunReadings).forEach { r ->
                Normalizer.kanaForms(r).forEach { addKey(Normalizer.searchKey(it), ref) }
            }
            entry.meanings.forEach { addMeaningToken(it, ref) }
            hitById[ref.id] = ref
        }
        data.kana.values.forEach { entry ->
            val ref = HitRef(
                type = EntityType.KANA, id = entry.id, text = entry.character,
                reading = entry.reading, gloss = entry.reading,
                jlpt = null, frequencyRank = null,
                sourceIds = entry.sources.map { it.sourceId },
                partOfSpeech = emptyList(), priority = 0
            )
            addKey(Normalizer.searchKey(entry.character), ref)
            if (entry.reading.isNotBlank()) {
                addKey(Normalizer.searchKey(entry.reading), ref)
                Normalizer.kanaForms(entry.reading).forEach { addKey(Normalizer.searchKey(it), ref) }
            }
            hitById[ref.id] = ref
        }
        data.vocab.values.forEach { entry ->
            val priority = data.sources[entry.sources.firstOrNull()?.sourceId]?.priority ?: 0
            val ref = HitRef(
                type = EntityType.VOCAB, id = entry.id, text = entry.expression,
                reading = entry.primaryReading ?: "",
                gloss = entry.primaryGloss,
                jlpt = entry.jlpt,
                frequencyRank = entry.frequencies.firstNotNullOfOrNull { it.rank },
                sourceIds = entry.sources.map { it.sourceId },
                partOfSpeech = entry.partOfSpeech,
                priority = priority
            )
            addKey(Normalizer.searchKey(entry.expression), ref)
            Normalizer.kanaForms(entry.expression).forEach { addKey(Normalizer.searchKey(it), ref) }
            entry.readings.forEach { r ->
                Normalizer.kanaForms(r.kana).forEach { addKey(Normalizer.searchKey(it), ref) }
            }
            entry.allGlosses.forEach { addMeaningToken(it, ref) }
            hitById[ref.id] = ref
        }
    }

    private fun addKey(key: String, ref: HitRef) {
        if (key.isBlank()) return
        keysToHits.getOrPut(key) { mutableListOf() }.add(ref)
    }

    private fun addMeaningToken(gloss: String, ref: HitRef) {
        Normalizer.searchKey(gloss).split(" ")
            .filter { it.length >= 2 }
            .forEach { token ->
                if (token.isNotBlank()) meaningTokens.getOrPut(token) { mutableListOf() }.add(ref)
            }
    }

    fun search(query: String, filters: SearchFilters = SearchFilters()): List<SearchHit> {
        val raw = query.trim()
        if (raw.isEmpty()) return emptyList()
        val q = Normalizer.searchKey(raw)
        val candidates = linkedMapOf<String, SearchHit>()

        fun add(ref: HitRef, exactness: Exactness) {
            if (!passesFilters(ref, filters)) return
            val context = RankingContext(
                exactness = exactness,
                sourcePriority = ref.priority,
                frequencyRank = ref.frequencyRank,
                entityType = ref.type
            )
            val score = ranking.rank(raw, ref.toHit(), context)
            val hit = ref.toHit(score)
            val existing = candidates[hit.id]
            if (existing == null || existing.score < hit.score) {
                candidates[hit.id] = hit
            }
        }

        // Exact + prefix on keys.
        keysToHits[q]?.forEach { add(it, Exactness.EXACT) }
        if (q.length >= 1) {
            keysToHits.entries
                .filter { it.key.startsWith(q) }
                .take(120)
                .forEach { (_, refs) -> refs.forEach { add(it, Exactness.PREFIX) } }
        }
        // Reading match: probe the katakana form of the query too.
        val katakanaQ = Normalizer.searchKey(Normalizer.toKatakana(raw))
        if (katakanaQ != q) {
            keysToHits[katakanaQ]?.forEach { add(it, Exactness.READING) }
        }
        // Meaning search: token containment.
        if (q.length >= 2) {
            meaningTokens.entries
                .filter { it.key.contains(q) || q.contains(it.key) }
                .take(200)
                .forEach { (_, refs) -> refs.forEach { add(it, Exactness.MEANING) } }
        }

        return candidates.values.sortedByDescending { it.score }.take(200)
    }

    fun autocomplete(prefix: String, limit: Int = 10): List<Suggestion> {
        val q = Normalizer.searchKey(prefix)
        if (q.isEmpty()) return emptyList()
        val seen = linkedMapOf<String, Suggestion>()
        keysToHits.entries
            .filter { it.key.startsWith(q) }
            .sortedBy { it.key }
            .forEach { (key, refs) ->
                refs.forEach { ref ->
                    if (seen.size >= limit * 3) return@forEach
                    if (seen.containsKey(ref.id)) return@forEach
                    val context = RankingContext(
                        exactness = Exactness.PREFIX,
                        sourcePriority = ref.priority,
                        frequencyRank = ref.frequencyRank,
                        entityType = ref.type
                    )
                    val score = ranking.rank(prefix, ref.toHit(), context)
                    seen[ref.id] = Suggestion(ref.text, ref.reading, ref.type, ref.id, score)
                }
            }
        return seen.values.sortedByDescending { it.score }.take(limit)
    }

    fun hit(id: String): SearchHit? = hitById[id]?.toHit()

    private fun passesFilters(ref: HitRef, filters: SearchFilters): Boolean {
        filters.entityTypes?.let { if (ref.type !in it) return false }
        filters.jlpt?.let { if (ref.jlpt != it) return false }
        filters.maxFrequencyRank?.let { max ->
            val rank = ref.frequencyRank ?: return false
            if (rank > max) return false
        }
        filters.readingContains?.let { if (!ref.reading.contains(it)) return false }
        filters.sourceIds?.let { sources -> if (ref.sourceIds.none { it in sources }) return false }
        filters.partOfSpeech?.let { pos -> if (ref.partOfSpeech.none { it in pos }) return false }
        return true
    }

    private fun HitRef.toHit(score: Float = 0f): SearchHit = SearchHit(
        entityType = type,
        id = id,
        text = text,
        reading = reading,
        gloss = gloss,
        score = score,
        sourceIds = sourceIds,
        jlpt = jlpt,
        frequencyRank = frequencyRank,
        partOfSpeech = partOfSpeech
    )
}
