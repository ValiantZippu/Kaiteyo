package io.kaiteyo.kjd.normalize

/**
 * Japanese-aware text normalization.
 *
 * Normalization is used for search keys, deduplication identity and lookup.
 * It must never destroy linguistic distinctions in stored data — the
 * canonical display values are always preserved; normalization is applied
 * only where identity or search matching happens.
 */
object JapaneseNormalizer {

    /** Unicode NFC normalization (Compose adds kanji/kana composed forms). */
    fun toNfc(input: String): String = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFC)

    /** Unicode NFD normalization. */
    fun toNfd(input: String): String = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)

    /** Collapse whitespace runs to a single space and trim. */
    fun collapseWhitespace(input: String): String =
        input.trim().replace(Regex("\\s+"), " ")

    /** Lowercase Latin letters (used for romaji/meaning search). */
    fun toSearchCase(input: String): String = input.lowercase()

    /**
     * Convert katakana to hiragana (long-vowel mark ー handled per common
     * practice by leaving it in place — it is not a kana that maps 1:1).
     */
    fun katakanaToHiragana(input: String): String {
        val sb = StringBuilder(input.length)
        for (char in input) {
            val code = char.code
            sb.append(
                if (code in 0x30A1..0x30F6) (code - 0x60).toChar()
                else char
            )
        }
        return sb.toString()
    }

    /** Convert hiragana to katakana. */
    fun hiraganaToKatakana(input: String): String {
        val sb = StringBuilder(input.length)
        for (char in input) {
            val code = char.code
            sb.append(
                if (code in 0x3041..0x3096) (code + 0x60).toChar()
                else char
            )
        }
        return sb.toString()
    }

    /** Remove common punctuation for search keys. */
    fun stripPunctuation(input: String): String =
        input.replace(Regex("[、。．.,!！?？·・:：;；'\"“”‘’()（）\\[\\]【】<>《》«»…—–-]"), "")

    /** Whether a string contains any kanji. */
    fun hasKanji(input: String): Boolean = input.any { it.code in 0x4E00..0x9FFF || it.code in 0x3400..0x4DBF }

    /** Whether a string is entirely kana. */
    fun isKanaOnly(input: String): Boolean =
        input.isNotEmpty() && input.all {
            val code = it.code
            (code in 0x3040..0x309F) || (code in 0x30A0..0x30FF) || it == 'ー' || it == '・'
        }

    /** Whether a string contains only kana and kanji (no Latin/digits/other). */
    fun isJapaneseScript(input: String): Boolean =
        input.isNotEmpty() && input.all {
            val code = it.code
            (code in 0x3040..0x309F) ||
                (code in 0x30A0..0x30FF) ||
                (code in 0x4E00..0x9FFF) ||
                (code in 0x3400..0x4DBF) ||
                it == 'ー' || it == '・'
        }

    /**
     * Full search key: NFC, lowercase, punctuation-stripped. Used for
     * substring matching on the search index.
     */
    fun searchKey(input: String): String =
        toSearchCase(collapseWhitespace(stripPunctuation(toNfc(input))))

    /**
     * Identity key: NFC + katakana→hiragana so that キャベツ and きゃべつ resolve
     * to the same identity during cross-source deduplication.
     */
    fun identityKey(input: String): String =
        katakanaToHiragana(toNfc(collapseWhitespace(input)))

    /** Whether [a] and [b] are equivalent under kana normalization. */
    fun kanaEquivalent(a: String, b: String): Boolean =
        identityKey(a) == identityKey(b)
}
