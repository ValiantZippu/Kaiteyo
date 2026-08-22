package ua.syt0r.kanji.desktop.engine.nlp

import java.io.BufferedReader
import java.io.InputStreamReader

// ============================================
// KAITEYO JAPANESE NLP ENGINE
// Morphological analysis via Kuromoji (IPAdic)
// with a built-in fallback tokenizer for when
// Kuromoji isn't on the classpath. Provides word
// segmentation, reading assignment, POS tagging,
// and deinflection hints for the reading engine.
// ============================================

/** A single morpheme/token from analysis. */
data class Morpheme(
    val surface: String,
    val reading: String = "",
    val baseForm: String = "",
    val partOfSpeech: String = "",
    val partOfSpeechSub1: String = "",
    val partOfSpeechSub2: String = "",
    val inflectionType: String = "",
    val inflectionForm: String = "",
    val confidence: Double = 1.0
)

/** Result of morphological analysis on a text fragment. */
data class NlpResult(
    val text: String,
    val morphemes: List<Morpheme>,
    val isJapanese: Boolean = true,
    val engine: String = "kuromoji"
) {
    val segments: List<String> get() = morphemes.map { it.surface }
    val readings: List<String> get() = morphemes.map { it.reading }.filter { it.isNotBlank() }
    val fullReading: String get() = readings.joinToString("")
}

/** Interface for morphological analyzers. */
interface MorphologicalAnalyzer {
    val name: String
    val available: Boolean
    fun analyze(text: String): NlpResult
}

// ----------------------------------------------------------
// Kuromoji-based analyzer (requires kuromoji-ipadic jar)
// ----------------------------------------------------------

class KuromojiAnalyzer : MorphologicalAnalyzer {

    override val name = "Kuromoji (IPAdic)"
    override val available: Boolean = runCatching {
        Class.forName("com.atilika.kuromoji.Tokenizer")
        true
    }.getOrDefault(false)

    override fun analyze(text: String): NlpResult {
        if (!available) return FallbackTokenizer.analyze(text)
        return runCatching {
            val tokenizerClass = Class.forName("com.atilika.kuromoji.Tokenizer")
            val tokenizer = tokenizerClass.getDeclaredConstructor().newInstance()
            val tokenizeMethod = tokenizerClass.getMethod("tokenize", String::class.java)
            @Suppress("UNCHECKED_CAST")
            val tokens = tokenizeMethod.invoke(tokenizer, text) as Iterable<Any>

            val morphemes = tokens.map { token ->
                val tokenClass = token.javaClass
                val getSurface = tokenClass.getMethod("getSurface")
                val getReading = runCatching { tokenClass.getMethod("getReading") }.getOrNull()
                val getBaseForm = runCatching { tokenClass.getMethod("getBaseForm") }.getOrNull()
                val getAllFeatures = runCatching { tokenClass.getMethod("getAllFeatures") }.getOrNull()

                val surface = getSurface.invoke(token)?.toString() ?: ""
                val reading = runCatching { getReading?.invoke(token)?.toString() }.getOrNull() ?: ""
                val baseForm = runCatching { getBaseForm?.invoke(token)?.toString() }.getOrNull() ?: ""
                val features = runCatching { getAllFeatures?.invoke(token)?.toString() }.getOrNull() ?: ""

                val parts = features.split(",").map { it.trim() }

                Morpheme(
                    surface = surface,
                    reading = reading,
                    baseForm = baseForm.ifBlank { surface },
                    partOfSpeech = parts.getOrElse(0) { "" },
                    partOfSpeechSub1 = parts.getOrElse(1) { "" },
                    partOfSpeechSub2 = parts.getOrElse(2) { "" },
                    inflectionType = parts.getOrElse(4) { "" },
                    inflectionForm = parts.getOrElse(5) { "" },
                    confidence = 1.0
                )
            }

            NlpResult(text, morphemes, true, "kuromoji")
        }.getOrElse { FallbackTokenizer.analyze(text) }
    }
}

// ----------------------------------------------------------
// Fallback: simple kana/kanji boundary tokenizer
// ----------------------------------------------------------

object FallbackTokenizer : MorphologicalAnalyzer {

    override val name = "Fallback (regex-based)"
    override val available = true

    /** Character class ranges for Japanese scripts. */
    private val kanaRegex = Regex("[\\u3040-\\u309F\\u30A0-\\u30FF]+")    // Hiragana + Katakana
    private val kanjiRegex = Regex("[\\u4E00-\\u9FFF\\u3400-\\u4DBF]+")  // CJK Unified
    private val mixedRegex = Regex("[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FFF\\u3400-\\u4DBF]+")
    private val latinRegex = Regex("[A-Za-z0-9]+")
    private val punctuationRegex = Regex("[。、！？「」『』（）・…—～]+")

    override fun analyze(text: String): NlpResult {
        val morphemes = mutableListOf<Morpheme>()
        var pos = 0

        while (pos < text.length) {
            val remaining = text.substring(pos)
            val match = listOf(
                punctuationRegex.find(remaining),
                kanaRegex.find(remaining),
                kanjiRegex.find(remaining),
                latinRegex.find(remaining)
            ).filterNotNull()
                .minByOrNull { it.range.first }
                ?.takeIf { it.range.first == 0 }

            if (match != null) {
                val surface = match.value
                val isKana = surface.all { it in '\u3040'..'\u30FF' }
                val isKanji = surface.all { it in '\u4E00'..'\u9FFF' || it in '\u3400'..'\u4DBF' }
                morphemes.add(
                    Morpheme(
                        surface = surface,
                        reading = if (isKanji) surface else surface, // No reading resolution without Kuromoji
                        baseForm = surface,
                        partOfSpeech = when {
                            isKana -> "助詞" // particle-like
                            isKanji -> "名詞" // noun-like
                            else -> "記号"
                        },
                        confidence = 0.7
                    )
                )
                pos += surface.length
            } else {
                // Unknown character — skip one
                val ch = text[pos].toString()
                morphemes.add(Morpheme(surface = ch, reading = ch, confidence = 0.3))
                pos++
            }
        }

        return NlpResult(text, morphemes, isJapanese = true, engine = "fallback")
    }
}

// ----------------------------------------------------------
// Composite analyzer: tries Kuromoji, falls back to regex
// ----------------------------------------------------------

object JapaneseNlpEngine {

    private val analyzers: List<MorphologicalAnalyzer> by lazy {
        val kuromoji = runCatching { KuromojiAnalyzer() }.getOrNull()
        val list = mutableListOf<MorphologicalAnalyzer>()
        if (kuromoji?.available == true) list.add(kuromoji)
        list.add(FallbackTokenizer)
        list
    }

    /** The currently active analyzer. */
    val analyzer: MorphologicalAnalyzer get() = analyzers.first { it.available }

    /** Analyze text into morphemes. */
    fun analyze(text: String): NlpResult = analyzer.analyze(text)

    /** Simple: return just the segmented surface forms. */
    fun segment(text: String): List<String> = analyze(text).segments

    /** Tokenize and return only content words (nouns, verbs, adjectives). */
    fun contentWords(text: String): List<Morpheme> {
        return analyze(text).morphemes.filter { m ->
            m.partOfSpeech.startsWith("名詞") ||
            m.partOfSpeech.startsWith("動詞") ||
            m.partOfSpeech.startsWith("形容") ||
            m.partOfSpeech.startsWith("副詞")
        }
    }

    /** Detect if a string looks Japanese (contains kana or kanji). */
    fun isJapanese(text: String): Boolean {
        return text.any { ch ->
            ch in '\u3040'..'\u309F' || // Hiragana
            ch in '\u30A0'..'\u30FF' || // Katakana
            ch in '\u4E00'..'\u9FFF' || // CJK
            ch in '\u3400'..'\u4DBF'    // CJK Extension A
        }
    }

    /** Convenience: get the reading (kana) of a kanji string. */
    fun getReading(text: String): String {
        return analyze(text).fullReading.ifBlank { text }
    }
}
