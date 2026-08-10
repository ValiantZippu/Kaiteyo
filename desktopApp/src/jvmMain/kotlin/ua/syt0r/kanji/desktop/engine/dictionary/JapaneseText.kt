package ua.syt0r.kanji.desktop.engine.dictionary

// ============================================
// KAITEYO DICTIONARY �?" JAPANESE TEXT UTILITIES
// Lightweight, dependency-free kana/romaji/kanji
// detection and conversion used to power lookups.
// No external libraries are required; everything
// is hand-rolled so the engine stays portable.
// ============================================

object JapaneseText {

    private val hiraganaToKatakana = ('ぁ'..'ゖ').associate { it to (it.code + 0x60).toChar() }
    private val katakanaToHiragana = hiraganaToKatakana.entries.associate { (k, v) -> v to k }

    private val kanaRanges = listOf(
        '぀'..'ゟ',
        '゠'..'ヿ'
    )

    /** Ordered by length descending so greedy matching prefers longer romaji. */
    private val romajiTable: List<Pair<String, String>> = listOf(
        "ky" to "きゃ","sh" to "しゃ","ch" to "ちゃ","ny" to "にゃ","hy" to "ひゃ","ry" to "りゃ",
        "gy" to "ぎゃ","jy" to "じゃ","by" to "びゃ","py" to "ぴゃ",
        "kka" to "っか","kki" to "っき","kku" to "っく","kke" to "っけ","kko" to "っこ",
        "sshi" to "っし","ttsu" to "っつ","tchi" to "っち",
        "shi" to "し","chi" to "ち","tsu" to "つ","ka" to "か","ga" to "が","sa" to "さ","za" to "ざ",
        "ta" to "た","da" to "だ","na" to "な","ha" to "は","ba" to "ば","pa" to "ぱ","ma" to "ま",
        "ya" to "や","ra" to "ら","wa" to "わ","nn" to "ん","ki" to "き","gi" to "ぎ","ku" to "く","gu" to "ぐ",
        "ke" to "け","ge" to "げ","ko" to "こ","go" to "ご","si" to "し","zi" to "じ","ji" to "じ","su" to "す",
        "zu" to "ず","se" to "せ","ze" to "ぜ","so" to "そ","zo" to "ぞ","ti" to "ち","di" to "ぢ","tu" to "つ",
        "du" to "づ","te" to "て","de" to "で","to" to "と","do" to "ど","ni" to "に","nu" to "ぬ","ne" to "ね",
        "no" to "の","hi" to "ひ","bi" to "び","pi" to "ぴ","hu" to "ふ","fu" to "ふ","bu" to "ぶ","pu" to "ぷ",
        "he" to "へ","be" to "べ","pe" to "ぺ","ho" to "ほ","bo" to "ぼ","po" to "ぽ","mi" to "み","mu" to "む",
        "me" to "め","mo" to "も","yu" to "ゆ","yo" to "よ","ri" to "り","ru" to "る","re" to "れ","ro" to "ろ",
        "wo" to "を","a" to "あ","i" to "い","u" to "う","e" to "え","o" to "お","k" to "き","k'u" to "く",
        "n" to "ん","m" to "ん",
        "ja" to "じゃ","ju" to "じゅ","jo" to "じょ","sha" to "しゃ","shu" to "しゅ","sho" to "しょ",
        "cha" to "ちゃ","chu" to "ちゅ","cho" to "ちょ","kya" to "きゃ","kyu" to "きゅ","kyo" to "きょ",
        "gya" to "ぎゃ","gyu" to "ぎゅ","gyo" to "ぎょ","bya" to "びゃ","byu" to "びゅ","byo" to "びょ",
        "pya" to "ぴゃ","pyu" to "ぴゅ","pyo" to "ぴょ","nya" to "にゃ","nyu" to "にゅ","nyo" to "にょ",
        "rya" to "りゃ","ryu" to "りゅ","ryo" to "りょ","hya" to "ひゃ","hyu" to "ひゅ","hyo" to "ひょ"
    ).distinctBy { it.first }

    /** True if a string contains any kana character. */
    fun hasKana(text: String): Boolean = text.any { c -> kanaRanges.any { r -> c in r } }

    /** True if the string is pure kana (digits/punct allowed). */
    fun isKana(text: String): Boolean = text.isNotEmpty() && text.all { c -> !c.isLetter() || kanaRanges.any { r -> c in r } }

    /** True if a string is entirely latin (romaji) letters. */
    fun isRomaji(text: String): Boolean = text.isNotEmpty() && text.all { it.isLetter() && it.code < 128 }

    /** True if the character is a kanji (CJK Unified Ideographs). */
    fun isKanjiChar(c: Char): Boolean = c.code in 0x4E00..0x9FFF
    fun isKanji(text: String): Boolean = text.isNotEmpty() && text.all(::isKanjiChar)

    fun toKatakana(text: String): String = text.map { katakanaToHiragana[it] ?: it }.joinToString("")
    fun toHiragana(text: String): String = text.map { hiraganaToKatakana[it] ?: it }.joinToString("")

    /**
     * Best-effort romaji -> kana conversion. Accepts already-kana
     * (identity passthrough). Non-romaji runs are copied through.
     */
    fun romajiToHiragana(text: String): String {
        val input = text.trim().lowercase()
        if (hasKana(input)) return input
        if (!isRomaji(input)) return ""

        val result = StringBuilder()
        var i = 0
        while (i < input.length) {
            var matched = false
            for (len in 4 downTo 1) {
                if (i + len <= input.length) {
                    val chunk = input.substring(i, i + len)
                    val hit = romajiTable.firstOrNull { it.first == chunk }
                    if (hit != null) {
                        result.append(hit.second)
                        i += len
                        matched = true
                        break
                    }
                }
            }
            if (!matched) {
                if (input[i] == 'ー') result.append('ー') else result.append(input[i])
                i += 1
            }
        }
        return result.toString()
    }

    /** Kana keys generated for matching: kana itself + romaji + katakana + hiragana. */
    fun kanaKeys(text: String): List<String> {
        val bases = listOf(text, toHiragana(text), toKatakana(text))
        val set = linkedSetOf<String>()
        bases.forEach { if (it.isNotEmpty()) set.add(it) }
        return set.toList()
    }
}