package ua.syt0r.kanji.core.knowledge

import ua.syt0r.kanji.core.japanese.isHiragana
import ua.syt0r.kanji.core.japanese.isKanji
import ua.syt0r.kanji.core.japanese.isKatakana

// ============================================================
// SENTENCE ANALYSIS — interactive tokens (spec §26–§27)
// ------------------------------------------------------------
// Splits a corpus sentence into interactive tokens so each
// segment can be linked to the dictionary. The bundled corpus
// has no morphological analyzer output, so tokenization is a
// character-class run splitter (kanji runs / kana runs /
// punctuation / latin), which is an honest, deterministic
// approximation — every token can still be looked up in the
// word/kanji tables. This is documented as approximate; a real
// MeCab/UniDic segmenter is a roadmap item.
//
// Nothing here fabricates linguistic information: tokens that
// resolve to a real word carry the word; kanji tokens carry the
// kanji; tokens that match nothing are left unlinked.
// ============================================================

enum class SentenceTokenKind { Kanji, Kana, Mixed, Punctuation, Latin, Other }

@kotlinx.serialization.Serializable
data class SentenceToken(
    val text: String,
    val kind: SentenceTokenKind,
    /** Character offset into the source sentence. */
    val startIndex: Int,
    val endIndex: Int
)

/**
 * Pure tokenizer. Splits on character-class runs:
 *  - a run of kanji is a [SentenceTokenKind.Kanji] token
 *  - a run of hiragana/katakana is [SentenceTokenKind.Kana]
 *  - a run mixing kanji + kana (e.g. 食べる) is [SentenceTokenKind.Mixed]
 *  - Japanese punctuation is [SentenceTokenKind.Punctuation]
 *  - latin letters/digits are [SentenceTokenKind.Latin]
 *  - anything else is [SentenceTokenKind.Other]
 */
object SentenceTokenizer {

    fun tokenize(sentence: String): List<SentenceToken> {
        if (sentence.isBlank()) return emptyList()
        val tokens = mutableListOf<SentenceToken>()
        var i = 0
        while (i < sentence.length) {
            val start = i
            var kind = classify(sentence[i])
            i++
            // Extend the run: same class continues; kanji+kana mixes into a
            // single Mixed token (食べる), and once Mixed, further kana/kanji
            // stay inside it (食べられる).
            while (i < sentence.length) {
                val nextKind = classify(sentence[i])
                val continues = when {
                    nextKind == kind -> true
                    kind == SentenceTokenKind.Kanji && nextKind == SentenceTokenKind.Kana -> true
                    kind == SentenceTokenKind.Kana && nextKind == SentenceTokenKind.Kanji -> true
                    kind == SentenceTokenKind.Mixed &&
                        (nextKind == SentenceTokenKind.Kanji || nextKind == SentenceTokenKind.Kana) -> true
                    else -> false
                }
                if (!continues) break
                if (kind != SentenceTokenKind.Mixed && nextKind != kind) {
                    kind = SentenceTokenKind.Mixed
                }
                i++
            }
            tokens.add(
                SentenceToken(
                    text = sentence.substring(start, i),
                    kind = kind,
                    startIndex = start,
                    endIndex = i
                )
            )
        }
        return tokens
    }

    private fun classify(char: Char): SentenceTokenKind = when {
        char.isKanji() -> SentenceTokenKind.Kanji
        char.isHiragana() || char.isKatakana() -> SentenceTokenKind.Kana
        char.isJapanesePunctuation() -> SentenceTokenKind.Punctuation
        char.isLetter() || char.isDigit() -> SentenceTokenKind.Latin
        else -> SentenceTokenKind.Other
    }

    private fun Char.isJapanesePunctuation(): Boolean =
        this in "。、「」『』・ー—！？…．．，　"

    /** Reassembles a sentence from tokens (for round-trip tests). */
    fun join(tokens: List<SentenceToken>): String =
        tokens.joinToString("") { it.text }
}

/**
 * A sentence where every token carries optional dictionary links.
 * Kanji/Mixed tokens resolve to kanji entries (whole-token word
 * first, then per-character fallback). Kana tokens resolve to the
 * first word whose kana reading matches.
 */
@kotlinx.serialization.Serializable
data class SentenceAnalysis(
    val sentence: String,
    val tokens: List<AnnotatedToken>,
    val grammarMatches: List<GrammarMatch>,
    val difficulty: SentenceDifficultyLevel
)

@kotlinx.serialization.Serializable
data class AnnotatedToken(
    val token: SentenceToken,
    /** Word resolved for this token (kana readings, kanji compounds). */
    val word: WordKnowledge? = null,
    /** Kanji resolved for a single-character token, or a kanji compound word. */
    val kanji: KanjiKnowledge? = null,
    /** Kanji characters inside a Mixed/Kanji token, per character. */
    val kanjiCharacters: List<KanjiKnowledge> = emptyList(),
    /** Grammar patterns whose matched text overlaps this token. */
    val grammar: List<GrammarMatch> = emptyList()
)

/**
 * Suspending analyzer. Resolves tokens against the knowledge
 * repository — all lookups are real database queries.
 *
 * Tokens come from the dictionary-driven [WordSegmenter] (longest
 * real-word match per position); the character-class
 * [SentenceTokenizer] remains as the fallback used by tests and
 * lightweight surfaces that don't need dictionary resolution.
 */
class SentenceAnalyzer(
    private val repository: KnowledgeRepository,
    private val segmenter: WordSegmenter = WordSegmenter(repository)
) {

    suspend fun analyze(sentence: String): SentenceAnalysis {
        val segments = segmenter.segment(sentence)
        val grammarMatches = GrammarCatalog.findIn(sentence)

        val annotated = segments.map { segment ->
            val grammar = grammarMatches.filter {
                it.startIndex in segment.startIndex until segment.endIndex ||
                    it.endIndex in segment.startIndex until segment.endIndex
            }
            AnnotatedToken(
                token = SentenceToken(
                    text = segment.text,
                    kind = when (segment.kind) {
                        SegmentKind.Word -> if (segment.word?.kanjiReading != null) SentenceTokenKind.Mixed else SentenceTokenKind.Kana
                        SegmentKind.Kanji -> SentenceTokenKind.Kanji
                        SegmentKind.Kana -> SentenceTokenKind.Kana
                        SegmentKind.Punctuation -> SentenceTokenKind.Punctuation
                        SegmentKind.Other -> SentenceTokenKind.Other
                    },
                    startIndex = segment.startIndex,
                    endIndex = segment.endIndex
                ),
                word = segment.word,
                kanji = segment.kanji.firstOrNull(),
                kanjiCharacters = segment.kanji,
                grammar = grammar
            )
        }

        val difficulty = SentenceDifficultyScorer.score(
            sentence = sentence,
            tokenCount = segments.size,
            kanjiTokens = segments.count { it.kind == SegmentKind.Kanji || it.kind == SegmentKind.Word },
            grammarMatchCount = grammarMatches.size
        )

        return SentenceAnalysis(sentence, annotated, grammarMatches, difficulty)
    }
}
