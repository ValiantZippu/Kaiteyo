package ua.syt0r.kanji.desktop.engine.media

// ============================================
// KAITEYO SUBTITLE NORMALIZER
// Raw subtitle lines contain ASS/SSA override
// blocks, HTML tags, karaoke timing, speaker
// labels and furigana annotations. The dictionary
// lookup path needs clean Japanese, so every
// consumer (popup, mining, transcript search,
// text hook) routes text through this pipeline
// before segmentation. Nothing is destructively
// lost — display rendering keeps structure and
// only lookup/text paths get the cleaned form.
// ============================================

object SubtitleNormalizer {

    /** Strip ASS/SSA override blocks like {\\k20} and escape sequences. */
    fun stripAssTags(text: String): String =
        text.replace(Regex("\\{[^}]*}"), "")
            .replace("\\N", "\n")
            .replace("\\n", "\n")
            .replace("\\h", " ")

    /** Strip HTML/XML tags (VTT <i>, <ruby>, <rt>, …). */
    fun stripHtml(text: String): String = text.replace(Regex("<[^>]*>"), "")

    /** Collapse <ruby>漢字<rt>かんじ</rt></ruby> to just 漢字. */
    fun stripRuby(text: String): String =
        text.replace(Regex("<rt>.*?</rt>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<rt/>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("</?ruby>", RegexOption.IGNORE_CASE), "")

    /**
     * Decode numeric + common named HTML entities. Handles the full range of
     * decimal/hex code points so subtitle files can carry kana/kanji entities.
     */
    fun decodeEntities(text: String): String {
        val named = mapOf(
            "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'",
            "nbsp" to " ", "middot" to "·", "hellip" to "…", "mdash" to "—",
            "ndash" to "–", "larr" to "←", "rarr" to "→", "darr" to "↓", "uarr" to "↑"
        )
        return Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z]+);").replace(text) { m ->
            val body = m.groupValues[1]
            when {
                body.startsWith("#x", ignoreCase = true) ->
                    body.drop(2).toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: m.value
                body.startsWith("#") ->
                    body.drop(1).toIntOrNull()?.let { String(Character.toChars(it)) } ?: m.value
                else -> named[body.lowercase()] ?: m.value
            }
        }
    }

    /** A leading "NAME:" / "NAME：" speaker marker (ja or latin names). */
    fun stripSpeakerLabel(text: String): String =
        text.replaceFirst(Regex("^\\s*[A-Za-z0-9_\\-\\u3040-\\u30ff\\u4e00-\\u9fff]+[:：]\\s*"), "")

    /**
     * Remove kanji + kana in square brackets immediately following a kanji
     * run (JMdict-style furigana annotations like 漢字[かんじ] and 食べる[たべる]).
     * Only strips when the bracket content is pure kana so legit brackets
     * (stage directions, song lyrics) survive.
     */
    fun stripFurigana(text: String): String {
        val kana = Regex("^[\\u3040-\\u30ff\\u30fc\\u30fb]+$")
        return Regex("([\\u4e00-\\u9fff\\u3400-\\u4dbf]+)\\[([^\\[\\]]{1,16})]").replace(text) { m ->
            val reading = m.groupValues[2]
            if (kana.matches(reading)) m.groupValues[1] else m.value
        }
    }

    /** Strip karaoke/lyric markers: ♪ ♫ and bracketed romaji/kanji gloss lines. */
    fun stripDecoration(text: String): String =
        text.replace(Regex("[♪♫♬]"), "")
            .replace(Regex("^\\s*[\\-\\u2022•]\\s*"), "")
            .replace(Regex("\\s*\\\\\\s*$"), "")
            .trim()

    /**
     * Full pipeline for dictionary lookup / mining / transcript search:
     * returns clean, normalized Japanese (tags gone, entities decoded,
     * furigana collapsed, speaker markers removed, whitespace collapsed).
     */
    fun normalizeForLookup(text: String): String =
        cleanForDisplay(text).replace(Regex("\\s+"), " ").trim()

    /**
     * Cleaning for display: tags + entities handled, but structure (line
     * breaks, punctuation) is preserved.
     */
    fun cleanForDisplay(text: String): String {
        var out = text
        out = stripAssTags(out)
        out = stripHtml(out)
        out = stripRuby(out)
        out = decodeEntities(out)
        out = stripFurigana(out)
        out = stripSpeakerLabel(out)
        out = stripDecoration(out)
        return out.trim()
    }

    /**
     * True when a normalized cue is mostly Japanese script — used by the
     * popup to avoid opening the dictionary for song lyrics in romaji.
     */
    fun isMostlyJapanese(text: String): Boolean {
        val cleaned = normalizeForLookup(text).filterNot { it.isWhitespace() }
        if (cleaned.isEmpty()) return false
        val japanese = cleaned.count {
            it.code in 0x3040..0x30ff || it.code in 0x4E00..0x9FFF ||
                it.code in 0x3400..0x4DBF || it.code in 0x30FC..0x30FC || it == '々'
        }
        return japanese * 2 >= cleaned.length
    }
}
