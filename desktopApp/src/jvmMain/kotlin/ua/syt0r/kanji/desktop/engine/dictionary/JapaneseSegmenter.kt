package ua.syt0r.kanji.desktop.engine.dictionary

import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.SrsStatus

// ============================================
// KAITEYO JAPANESE SEGMENTER
// Dictionary-backed tokenization for Japanese
// subtitle text. Japanese has no spaces, so a
// naive split is useless. This segmenter walks
// the text left to right and greedily matches the
// longest dictionary headword (kanji+kana spans),
// falling back to single kanji or kana runs.
// Inflected forms still resolve because the
// dictionary lookup applies deinflection.
// Results are cached per text; word statuses are
// recomputed cheaply against the card pool.
// ============================================

enum class WordStatus { Unknown, Known, Learning, Mature, New, Mined, Suspended }

data class SegmentToken(
    val surface: String,
    val offset: Int,
    val isJapanese: Boolean,
    val isKanji: Boolean,
    val dictionaryMatch: DictionaryMatch? = null,
    val reading: String = "",
    val status: WordStatus = WordStatus.Unknown
)

/** Internal cache record: everything except the volatile word status. */
private data class CachedSegmentation(
    val surfaces: List<String>,
    val offsets: List<Int>,
    val japanese: List<Boolean>,
    val kanji: List<Boolean>,
    val matches: List<DictionaryMatch?>,
    val readings: List<String>
)

object JapaneseSegmenter {

    private const val MAX_MATCH_LEN = 8
    private const val CACHE_SIZE = 200

    private val cache = object : LinkedHashMap<String, CachedSegmentation>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedSegmentation>): Boolean =
            size > CACHE_SIZE
    }

    // ------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------

    /** Segment Japanese text into tokens annotated with statuses. */
    fun segment(
        text: String,
        repository: DictionaryRepository,
        cards: List<DesktopCard> = emptyList()
    ): List<SegmentToken> {
        val cached = cachedSegment(text, repository)
        return cached.surfaces.mapIndexed { i, surface ->
            SegmentToken(
                surface = surface,
                offset = cached.offsets[i],
                isJapanese = cached.japanese[i],
                isKanji = cached.kanji[i],
                dictionaryMatch = cached.matches[i],
                reading = cached.readings[i],
                status = if (cached.japanese[i]) statusOf(surface, cards) else WordStatus.Unknown
            )
        }
    }

    /** Best dictionary match for a single surface (used by token clicks). */
    fun bestMatch(surface: String, repository: DictionaryRepository): DictionaryMatch? {
        if (surface.isBlank()) return null
        return repository.lookup(surface, SearchMode.All).firstOrNull()
    }

    /** Live status of a word given the current card pool. */
    fun statusOf(surface: String, cards: List<DesktopCard>): WordStatus {
        val card = cards.firstOrNull { it.character == surface } ?: return WordStatus.Unknown
        return when {
            card.status == SrsStatus.Suspended || card.status == SrsStatus.Buried -> WordStatus.Suspended
            card.tags.contains("mined") -> WordStatus.Mined
            card.status == SrsStatus.Review && card.intervalDays >= 21 -> WordStatus.Mature
            card.status == SrsStatus.Review -> WordStatus.Known
            card.status == SrsStatus.Learning || card.status == SrsStatus.Relearning -> WordStatus.Learning
            else -> WordStatus.New
        }
    }

    /** Estimate known-word coverage (0..1) for a piece of text. */
    fun coverage(text: String, repository: DictionaryRepository, cards: List<DesktopCard>): Float {
        val tokens = segment(text, repository, cards).filter { it.isJapanese && it.surface.any(::isKanjiCharOrKana) }
        if (tokens.isEmpty()) return 0f
        val known = tokens.count { it.status != WordStatus.Unknown }
        return known.toFloat() / tokens.size
    }

    // ------------------------------------------------------------
    // Core algorithm
    // ------------------------------------------------------------

    private fun cachedSegment(text: String, repository: DictionaryRepository): CachedSegmentation {
        synchronized(cache) {
            cache[text]?.let { return it }
        }
        val result = buildSegmentation(text, repository)
        synchronized(cache) {
            cache[text] = result
        }
        return result
    }

    private fun buildSegmentation(text: String, repository: DictionaryRepository): CachedSegmentation {
        val headwords = headwordSet(repository)
        val surfaces = mutableListOf<String>()
        val offsets = mutableListOf<Int>()
        val japanese = mutableListOf<Boolean>()
        val kanji = mutableListOf<Boolean>()
        val matches = mutableListOf<DictionaryMatch?>()
        val readings = mutableListOf<String>()

        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                isKanjiChar(c) -> {
                    val start = i
                    // A kanji headword may continue into following kana (食べる).
                    var end = i
                    while (end < text.length && (isKanjiChar(text[end]) || isKanaChar(text[end]))) end++
                    val window = text.substring(start, end)
                    val limit = minOf(window.length, MAX_MATCH_LEN)
                    var matchedLen = 0
                    for (len in limit downTo 1) {
                        val candidate = window.substring(0, len)
                        if (headwords.contains(candidate)) {
                            matchedLen = len
                            break
                        }
                    }
                    var surface = window.substring(0, matchedLen.coerceAtLeast(1))
                    var match = if (matchedLen > 0) bestMatch(surface, repository) else null
                    if (matchedLen == 0 && window.length > 1) {
                        // Inflected verb/adjective: deinflect the whole span
                        // (e.g. 走った → 走る) and use the span as one token.
                        val recovered = Deinflect.deinflect(window)
                            .firstOrNull { headwords.contains(it.word) }
                        if (recovered != null) {
                            surface = window
                            match = bestMatch(recovered.word, repository)
                        }
                    }
                    surfaces.add(surface)
                    offsets.add(start)
                    japanese.add(true)
                    kanji.add(true)
                    matches.add(match)
                    readings.add(match?.entry?.readings?.firstOrNull()?.reading ?: "")
                    i = start + surface.length
                }

                isKanaChar(c) -> {
                    val start = i
                    while (i < text.length && isKanaChar(text[i])) i++
                    val run = text.substring(start, i)
                    val runMatch = bestMatch(run, repository)
                    if (runMatch != null || run.length <= 2) {
                        surfaces.add(run)
                        offsets.add(start)
                        japanese.add(true)
                        kanji.add(false)
                        matches.add(runMatch)
                        readings.add(runMatch?.entry?.readings?.firstOrNull()?.reading ?: run)
                    } else {
                        // Inflected run: split off the longest deinflectable
                        // suffix (e.g. に + 行かなかった → 行く), keep the
                        // particle/prefix as its own token.
                        var splitAt = -1
                        var suffixMatch: DictionaryMatch? = null
                        for (s in run.length - 1 downTo 2) {
                            val suffix = run.substring(s)
                            val m = bestMatch(suffix, repository)
                            if (m != null) {
                                splitAt = s
                                suffixMatch = m
                                break
                            }
                        }
                        if (splitAt > 0) {
                            val prefix = run.substring(0, splitAt)
                            surfaces.add(prefix)
                            offsets.add(start)
                            japanese.add(true)
                            kanji.add(false)
                            matches.add(null)
                            readings.add(prefix)
                            val suffix = run.substring(splitAt)
                            surfaces.add(suffix)
                            offsets.add(start + splitAt)
                            japanese.add(true)
                            kanji.add(false)
                            matches.add(suffixMatch)
                            readings.add(suffixMatch?.entry?.readings?.firstOrNull()?.reading ?: suffix)
                        } else {
                            surfaces.add(run)
                            offsets.add(start)
                            japanese.add(true)
                            kanji.add(false)
                            matches.add(null)
                            readings.add(run)
                        }
                    }
                }

                else -> {
                    val start = i
                    while (i < text.length && !isKanjiChar(text[i]) && !isKanaChar(text[i])) i++
                    val surface = text.substring(start, i).trim()
                    if (surface.isNotEmpty()) {
                        surfaces.add(surface)
                        offsets.add(start)
                        japanese.add(false)
                        kanji.add(false)
                        matches.add(null)
                        readings.add("")
                    }
                }
            }
        }
        return CachedSegmentation(surfaces, offsets, japanese, kanji, matches, readings)
    }

    /** Precomputed set of all headwords across enabled dictionaries. */
    private fun headwordSet(repository: DictionaryRepository): Set<String> {
        val enabled = repository.enabledDictionaries().map { it.id }.toSet()
        return repository.allEntries().asSequence()
            .filter { it.dictionaryId in enabled }
            .flatMap { listOf(it.headword) + it.spellings }
            .filter { it.isNotEmpty() && it.length <= MAX_MATCH_LEN }
            .toHashSet()
    }

    private fun isKanjiChar(c: Char): Boolean =
        c.code in 0x4E00..0x9FFF || c.code in 0x3400..0x4DBF || c.code in 0xF900..0xFAFF

    private fun isKanaChar(c: Char): Boolean =
        c in '぀'..'ゟ' || c in '゠'..'ヿ'
}

private fun isKanjiCharOrKana(c: Char): Boolean =
    c.code in 0x4E00..0x9FFF || c.code in 0x3400..0x4DBF || c in '぀'..'ゟ' || c in '゠'..'ヿ'
