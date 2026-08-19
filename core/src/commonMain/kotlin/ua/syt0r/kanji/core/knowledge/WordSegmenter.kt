package ua.syt0r.kanji.core.knowledge

import ua.syt0r.kanji.core.japanese.isHiragana
import ua.syt0r.kanji.core.japanese.isKanji
import ua.syt0r.kanji.core.japanese.isKatakana

// ============================================================
// WORD SEGMENTER — dictionary-driven longest match
// ------------------------------------------------------------
// A real morphological approximation (spec §136–§137): instead
// of splitting on character classes, the segmenter asks the
// bundled dictionary "what is the longest real word starting at
// this position?" — so 日本語 becomes one token backed by the
// real word, 食べる becomes one token, and を stays a particle.
//
// Honest limits (documented, not hidden):
//   - matching is longest-prefix over the dictionary; it is a
//     segmentation approximation, not a full MeCab/UniDic parse
//     (KT-SENT-002 remains the roadmap for lemma/POS analysis)
//   - every token that resolves to a real word carries the word;
//     unmatched spans fall back to character-class runs
//   - lookups go through [SegmentWordLookup], so the matching
//     logic is unit-testable without a database (the default
//     adapter hits the real bundled dictionary) and a sentence
//     never issues more than a handful of queries (memoized)
// ============================================================

enum class SegmentKind { Word, Kanji, Kana, Punctuation, Other }

@kotlinx.serialization.Serializable
data class WordSegment(
    val text: String,
    val kind: SegmentKind,
    val startIndex: Int,
    val endIndex: Int,
    /** The real dictionary word when this segment matched one. */
    val word: WordKnowledge? = null,
    /** Kanji entries for a kanji token (single char or compound spelling). */
    val kanji: List<KanjiKnowledge> = emptyList()
)

/**
 * The dictionary lookups the segmenter needs. The default adapter hits
 * [KnowledgeRepository]; tests substitute canned data so segmentation
 * behavior is verifiable without loading the bundled database.
 */
interface SegmentWordLookup {

    /** Words whose spelling/reading starts with [text] (limit caps results). */
    suspend fun wordsWithText(text: String, limit: Int): List<WordKnowledge>

    /** Kanji entries for the given characters (single-character lookups). */
    suspend fun kanjiForCharacters(characters: Collection<String>): List<KanjiKnowledge>

    /** The kanji used in a matched word's spelling. */
    suspend fun kanjiOfWord(word: WordKnowledge): List<KanjiKnowledge>

    /** Default DB-backed adapter. */
    class Database(
        private val knowledge: KnowledgeRepository
    ) : SegmentWordLookup {
        override suspend fun wordsWithText(text: String, limit: Int): List<WordKnowledge> =
            knowledge.wordsWithText(text, limit = limit)

        override suspend fun kanjiForCharacters(characters: Collection<String>): List<KanjiKnowledge> =
            knowledge.kanjiBatch(characters)

        override suspend fun kanjiOfWord(word: WordKnowledge): List<KanjiKnowledge> =
            knowledge.searchKanjiOfWord(word)
    }
}

/**
 * Longest-match segmenter over the real bundled dictionary.
 *
 * Algorithm per position:
 *   1. if the character is kanji, find the longest word whose kanji
 *      spelling starts here (query wordsWithText for decreasing lengths);
 *      single unmatched kanji fall back to a kanji segment
 *   2. if the character is kana, find the longest word whose kana
 *      reading starts here; unmatched kana coalesce into a kana run
 *   3. punctuation / latin / other become single segments
 *
 * Lookups are memoized in a per-call map so overlapping probes for
 * the same substring hit the database once.
 */
class WordSegmenter(
    knowledge: KnowledgeRepository? = null,
    lookup: SegmentWordLookup? = null
) {
    private val wordLookup: SegmentWordLookup = lookup
        ?: knowledge?.let { SegmentWordLookup.Database(it) }
        ?: throw IllegalArgumentException("WordSegmenter needs a KnowledgeRepository or a SegmentWordLookup")

    suspend fun segment(sentence: String): List<WordSegment> {
        if (sentence.isBlank()) return emptyList()
        val probeCache = mutableMapOf<String, List<WordKnowledge>>()
        val kanjiCache = mutableMapOf<String, List<KanjiKnowledge>>()
        val result = mutableListOf<WordSegment>()
        var i = 0
        while (i < sentence.length) {
            val char = sentence[i]
            val segment = when {
                char.isKanji() -> kanjiAt(sentence, i, probeCache, kanjiCache)
                char.isHiragana() || char.isKatakana() -> kanaAt(sentence, i, probeCache)
                char.isJapanesePunctuation() -> WordSegment(
                    text = char.toString(), kind = SegmentKind.Punctuation,
                    startIndex = i, endIndex = i + 1
                )
                else -> WordSegment(
                    text = char.toString(), kind = SegmentKind.Other,
                    startIndex = i, endIndex = i + 1
                )
            }
            result.add(segment)
            i = segment.endIndex
        }
        return result
    }

    private suspend fun kanjiAt(
        sentence: String,
        start: Int,
        probeCache: MutableMap<String, List<WordKnowledge>>,
        kanjiCache: MutableMap<String, List<KanjiKnowledge>>
    ): WordSegment {
        val end = run {
            var e = start
            while (e < sentence.length && sentence[e].isKanji()) e++
            e
        }
        val run = sentence.substring(start, end)

        // Longest word match over the whole kanji run, shrinking.
        for (len in run.length downTo 2) {
            val candidate = run.substring(0, len)
            val word = probe(candidate, probeCache).firstOrNull { it.kanjiReading == candidate }
            if (word != null) {
                val cached = kanjiCache[candidate]
                return WordSegment(
                    text = candidate, kind = SegmentKind.Word,
                    startIndex = start, endIndex = start + len,
                    word = word,
                    kanji = cached ?: wordLookup.kanjiOfWord(word).also { kanjiCache[candidate] = it }
                )
            }
        }

        // Mixed kanji + kana words (食べる, 見る): the kanji run alone never
        // matches, so extend the candidate into the following kana and probe
        // the full spelling against the dictionary. Only exact spellings
        // match — inflected forms (食べます) honestly stay unsegmented.
        val mixedEnd = run {
            var e = end
            while (e < sentence.length && (sentence[e].isHiragana() || sentence[e].isKatakana())) e++
            e
        }
        if (mixedEnd > end) {
            for (len in mixedEnd downTo end + 1) {
                val candidate = sentence.substring(start, len)
                val word = probe(candidate, probeCache).firstOrNull { it.kanjiReading == candidate }
                if (word != null) {
                    val cached = kanjiCache[candidate]
                    return WordSegment(
                        text = candidate, kind = SegmentKind.Word,
                        startIndex = start, endIndex = start + len,
                        word = word,
                        kanji = cached ?: wordLookup.kanjiOfWord(word).also { kanjiCache[candidate] = it }
                    )
                }
            }
        }

        // No compound matched: per-character kanji segments.
        val single = run.substring(0, 1)
        val cached = kanjiCache[single]
        return WordSegment(
            text = single, kind = SegmentKind.Kanji,
            startIndex = start, endIndex = start + 1,
            kanji = cached ?: wordLookup.kanjiForCharacters(listOf(single)).also { kanjiCache[single] = it }
        )
    }

    private suspend fun kanaAt(
        sentence: String,
        start: Int,
        probeCache: MutableMap<String, List<WordKnowledge>>
    ): WordSegment {
        val end = run {
            var e = start
            while (e < sentence.length && (sentence[e].isHiragana() || sentence[e].isKatakana())) e++
            e
        }
        val run = sentence.substring(start, end)

        // Longest kana word match (e.g. です, ました), shrinking.
        for (len in run.length downTo 2) {
            val candidate = run.substring(0, len)
            val word = probe(candidate, probeCache).firstOrNull {
                it.kanaReading == candidate || it.kanjiReading == null && it.kanaReading.startsWith(candidate)
            }
            if (word != null) {
                return WordSegment(
                    text = candidate, kind = SegmentKind.Word,
                    startIndex = start, endIndex = start + len,
                    word = word
                )
            }
        }

        // Unmatched kana coalesces into one kana segment.
        return WordSegment(
            text = run, kind = SegmentKind.Kana,
            startIndex = start, endIndex = end
        )
    }

    private suspend fun probe(
        text: String,
        cache: MutableMap<String, List<WordKnowledge>>
    ): List<WordKnowledge> =
        cache.getOrPut(text) { wordLookup.wordsWithText(text, limit = 8) }

    private fun Char.isJapanesePunctuation(): Boolean =
        this in "。、「」『』・ー—！？…．．，　"
}
