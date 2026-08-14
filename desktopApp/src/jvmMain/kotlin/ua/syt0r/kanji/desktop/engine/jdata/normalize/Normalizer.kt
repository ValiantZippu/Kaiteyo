package ua.syt0r.kanji.desktop.engine.jdata.normalize

import ua.syt0r.kanji.desktop.engine.dictionary.JapaneseText
import java.text.Normalizer as JvmNormalizer

// ============================================================
// SEARCH / IDENTITY NORMALIZATION
// Non-destructive normalization for indexing and matching:
// Unicode NFC/NFKC, hiragana↔katakana, whitespace collapsing,
// punctuation stripping and Latin case folding. The canonical
// entries themselves are never rewritten — only the derived
// search keys use these forms.
// ============================================================

object Normalizer {

    private const val SEPARATOR = '\u0000'

    fun nfc(text: String): String = JvmNormalizer.normalize(text, JvmNormalizer.Form.NFC)
    fun nfkc(text: String): String = JvmNormalizer.normalize(text, JvmNormalizer.Form.NFKC)

    fun toHiragana(text: String): String = nfc(JapaneseText.toHiragana(text))
    fun toKatakana(text: String): String = nfc(JapaneseText.toKatakana(text))

    fun foldLatin(text: String): String = text.lowercase()

    fun collapseWhitespace(text: String): String = text.trim().replace(Regex("\\s+"), " ")

    /** Characters removed from search keys (not from canonical data). */
    private val punctuation = setOf(
        '、', '。', '，', '．', '！', '？', '!', '?', '.', ',', ':', ';',
        '(', ')', '[', ']', '{', '}', '「', '」', '『', '』', '【', '】', '…', '—', '・', '-', '_', '~', '〜'
    )

    fun stripPunctuation(text: String): String = text.filter { it !in punctuation }

    /** Canonical key for a term: NFKC → kana-folded → punctuation/space-stripped → lowercased. */
    fun searchKey(text: String): String =
        foldLatin(stripPunctuation(collapseWhitespace(toHiragana(nfkc(text)))))

    /** Canonical key for a reading: kana-folded, punctuation stripped. */
    fun readingKey(text: String): String =
        stripPunctuation(toHiragana(nfkc(text)))

    /** Kana forms (hiragana + katakana) of a string, for kana-insensitive matching. */
    fun kanaForms(text: String): List<String> {
        val hiragana = toHiragana(text)
        val katakana = toKatakana(text)
        return (listOf(hiragana, katakana) + JapaneseText.kanaKeys(text)).distinct()
    }

    /** Identity key used by the deduplication resolver. */
    fun identityKey(expression: String, reading: String): String =
        readingKey("$expression$SEPARATOR$reading")

    /** True if the string contains any kanji character. */
    fun hasKanji(text: String): Boolean = text.any { JapaneseText.isKanjiChar(it) }

    /** Kanji characters inside a term, in order, deduplicated. */
    fun kanjiCharacters(text: String): List<String> =
        text.filter { JapaneseText.isKanjiChar(it) }.map { it.toString() }.distinct()
}
