package ua.syt0r.kanji.desktop.engine.kana

import kotlinx.serialization.Serializable

// ============================================
// KAITEYO KANA SYSTEM
// Kana is a first-class learning domain, not a
// set of text labels. Every character is a
// structured record carrying its script, class,
// romanization, Unicode code point, stroke data
// and membership information. The dataset covers:
//
//   * the complete base syllabary (46 + 46)
//   * dakuten (が ぎ ぐ …) and handakuten (ぱ ぴ …)
//     — relationships stored, not string-munged
//   * yōon combinations (きゃ きゅ …) with base +
//     small kana encoded explicitly
//   * extended katakana used in loanword
//     transcription (ティ ディ ファ …) — only
//     combinations that actually occur in Japanese
//
// The stroke-order / geometry side lives in
// KanaStrokes.kt and feeds the same writing
// engine used for kanji. Provenance: romanizations
// follow Hepburn; combinations follow standard
// school syllabary conventions. No invented
// readings or invalid katakana pairs.
// ============================================

@Serializable
enum class KanaScript(val label: String) {
    Hiragana("Hiragana"),
    Katakana("Katakana")
}

@Serializable
enum class KanaCategory(val label: String) {
    Base("Basic"),
    Dakuten("Dakuten"),
    Handakuten("Handakuten"),
    YoOn("Combination"),
    Extended("Extended")
}

/**
 * One kana record. [base] / [small] make the relationships between
 * characters explicit: が → base か + dakuten; きゃ → base き + small ゃ.
 * Single characters have empty [base]/[small].
 */
@Serializable
data class KanaChar(
    val character: String,
    val romanization: String,
    val script: KanaScript,
    val category: KanaCategory,
    val base: String = "",
    val small: String = "",
    val strokeCount: Int,
    val meaning: String,
    val tags: List<String> = emptyList()
) {
    /** Unicode code point(s) of the character, e.g. "U+3042" or "U+304D U+3083". */
    val unicode: String
        get() = character.codePoints().toArray().joinToString(" ") { "U+%04X".format(it) }

    /** Deck-membership tags: which premade decks this kana belongs to. */
    val deckTags: List<String>
        get() = buildList {
            add("kana")
            add(if (script == KanaScript.Hiragana) "hiragana" else "katakana")
            add(category.name.lowercase())
        }

    companion object {
        /** Whether a kana character is a single unit the writing engine can grade. */
        fun isWritable(character: String): Boolean = character.length == 1
    }
}

// ------------------------------------------------------------
// BASE SYLLABARY — hiragana (46)
// (character to romanization / stroke count / sample meaning)
// ------------------------------------------------------------

private val HIRAGANA_BASE: List<Triple<String, String, Int>> = listOf(
    Triple("あ", "a", 3), Triple("い", "i", 2), Triple("う", "u", 2), Triple("え", "e", 2), Triple("お", "o", 3),
    Triple("か", "ka", 3), Triple("き", "ki", 4), Triple("く", "ku", 1), Triple("け", "ke", 3), Triple("こ", "ko", 2),
    Triple("さ", "sa", 3), Triple("し", "shi", 1), Triple("す", "su", 2), Triple("せ", "se", 3), Triple("そ", "so", 3),
    Triple("た", "ta", 4), Triple("ち", "chi", 3), Triple("つ", "tsu", 1), Triple("て", "te", 1), Triple("と", "to", 2),
    Triple("な", "na", 4), Triple("に", "ni", 3), Triple("ぬ", "nu", 2), Triple("ね", "ne", 4), Triple("の", "no", 1),
    Triple("は", "ha", 3), Triple("ひ", "hi", 1), Triple("ふ", "fu", 4), Triple("へ", "he", 1), Triple("ほ", "ho", 4),
    Triple("ま", "ma", 3), Triple("み", "mi", 3), Triple("む", "mu", 3), Triple("め", "me", 2), Triple("も", "mo", 3),
    Triple("や", "ya", 3), Triple("ゆ", "yu", 2), Triple("よ", "yo", 2),
    Triple("ら", "ra", 2), Triple("り", "ri", 2), Triple("る", "ru", 3), Triple("れ", "re", 4), Triple("ろ", "ro", 1),
    Triple("わ", "wa", 2), Triple("を", "wo", 3), Triple("ん", "n", 2)
)

// ------------------------------------------------------------
// BASE SYLLABARY — katakana (46)
// ------------------------------------------------------------

private val KATAKANA_BASE: List<Triple<String, String, Int>> = listOf(
    Triple("ア", "a", 2), Triple("イ", "i", 2), Triple("ウ", "u", 3), Triple("エ", "e", 3), Triple("オ", "o", 3),
    Triple("カ", "ka", 2), Triple("キ", "ki", 3), Triple("ク", "ku", 2), Triple("ケ", "ke", 3), Triple("コ", "ko", 2),
    Triple("サ", "sa", 3), Triple("シ", "shi", 3), Triple("ス", "su", 2), Triple("セ", "se", 3), Triple("ソ", "so", 2),
    Triple("タ", "ta", 3), Triple("チ", "chi", 3), Triple("ツ", "tsu", 3), Triple("テ", "te", 3), Triple("ト", "to", 2),
    Triple("ナ", "na", 2), Triple("ニ", "ni", 2), Triple("ヌ", "nu", 2), Triple("ネ", "ne", 4), Triple("ノ", "no", 1),
    Triple("ハ", "ha", 2), Triple("ヒ", "hi", 2), Triple("フ", "fu", 2), Triple("ヘ", "he", 1), Triple("ホ", "ho", 4),
    Triple("マ", "ma", 3), Triple("ミ", "mi", 3), Triple("ム", "mu", 2), Triple("メ", "me", 3), Triple("モ", "mo", 3),
    Triple("ヤ", "ya", 2), Triple("ユ", "yu", 2), Triple("ヨ", "yo", 3),
    Triple("ラ", "ra", 2), Triple("リ", "ri", 2), Triple("ル", "ru", 2), Triple("レ", "re", 2), Triple("ロ", "ro", 3),
    Triple("ワ", "wa", 2), Triple("ヲ", "wo", 3), Triple("ン", "n", 2)
)

// ------------------------------------------------------------
// DAKUTEN / HANDAKUTEN — voiced (が行 …) and
// semi-voiced (ぱ行 …) pairs, both scripts.
// base → the unvoiced character the mark is added to.
// ------------------------------------------------------------

private val HIRAGANA_DAKUTEN: List<Triple<String, String, Int>> = listOf(
    Triple("が", "ga", 5), Triple("ぎ", "gi", 6), Triple("ぐ", "gu", 3), Triple("げ", "ge", 5), Triple("ご", "go", 4),
    Triple("ざ", "za", 5), Triple("じ", "ji", 3), Triple("ず", "zu", 4), Triple("ぜ", "ze", 5), Triple("ぞ", "zo", 5),
    Triple("だ", "da", 6), Triple("ぢ", "ji", 5), Triple("づ", "zu", 3), Triple("で", "de", 3), Triple("ど", "do", 4),
    Triple("ば", "ba", 5), Triple("び", "bi", 3), Triple("ぶ", "bu", 6), Triple("べ", "be", 3), Triple("ぼ", "bo", 6)
)

private val KATAKANA_DAKUTEN: List<Triple<String, String, Int>> = listOf(
    Triple("ガ", "ga", 4), Triple("ギ", "gi", 5), Triple("グ", "gu", 4), Triple("ゲ", "ge", 5), Triple("ゴ", "go", 4),
    Triple("ザ", "za", 5), Triple("ジ", "ji", 5), Triple("ズ", "zu", 4), Triple("ゼ", "ze", 5), Triple("ゾ", "zo", 4),
    Triple("ダ", "da", 5), Triple("ヂ", "ji", 5), Triple("ヅ", "zu", 5), Triple("デ", "de", 5), Triple("ド", "do", 4),
    Triple("バ", "ba", 4), Triple("ビ", "bi", 4), Triple("ブ", "bu", 4), Triple("ベ", "be", 3), Triple("ボ", "bo", 6)
)

private val HIRAGANA_HANDAKUTEN: List<Triple<String, String, Int>> = listOf(
    Triple("ぱ", "pa", 5), Triple("ぴ", "pi", 3), Triple("ぷ", "pu", 6), Triple("ぺ", "pe", 3), Triple("ぽ", "po", 6)
)

private val KATAKANA_HANDAKUTEN: List<Triple<String, String, Int>> = listOf(
    Triple("パ", "pa", 4), Triple("ピ", "pi", 4), Triple("プ", "pu", 4), Triple("ペ", "pe", 3), Triple("ポ", "po", 6)
)

// ------------------------------------------------------------
// BASE LOOKUPS — voiced / semi-voiced kana keep an
// explicit relationship to their unvoiced base (が → か,
// ぱ → は) so the connection lives in the data, not in
// string munging at render time.
// ------------------------------------------------------------

private val VOICED_TO_BASE: Map<String, String> = buildMap {
    listOf(
        "がぎぐげご" to "かきくけこ",
        "ざじずぜぞ" to "さしすせそ",
        "だぢづでど" to "たちつてと",
        "ばびぶべぼ" to "はひふへほ",
        "ガギグゲゴ" to "カキクケコ",
        "ザジズゼゾ" to "サシスセソ",
        "ダヂヅデド" to "タチツテト",
        "バビブベボ" to "ハヒフヘホ"
    ).forEach { (voiced, unvoiced) ->
        voiced.forEachIndexed { index, char -> put(char.toString(), unvoiced[index].toString()) }
    }
}

private val HANDAKUTEN_TO_BASE: Map<String, String> = buildMap {
    "ぱぴぷぺぽ".forEachIndexed { index, char -> put(char.toString(), "はひふへほ"[index].toString()) }
    "パピプペポ".forEachIndexed { index, char -> put(char.toString(), "ハヒフヘホ"[index].toString()) }
}

/** Unvoiced base character of a voiced / semi-voiced kana, or null. */
fun voicedBase(character: String): String? = VOICED_TO_BASE[character] ?: HANDAKUTEN_TO_BASE[character]

// ------------------------------------------------------------
// YŌON COMBINATIONS — base kana + small ゃ ゅ ょ,
// both scripts. base → the full-size kana, small → the
// small kana (both relationships are stored explicitly).
// ------------------------------------------------------------

private val HIRAGANA_YOON: List<Pair<String, String>> = listOf(
    "き" to "ゃ", "き" to "ゅ", "き" to "ょ",
    "し" to "ゃ", "し" to "ゅ", "し" to "ょ",
    "ち" to "ゃ", "ち" to "ゅ", "ち" to "ょ",
    "に" to "ゃ", "に" to "ゅ", "に" to "ょ",
    "ひ" to "ゃ", "ひ" to "ゅ", "ひ" to "ょ",
    "み" to "ゃ", "み" to "ゅ", "み" to "ょ",
    "り" to "ゃ", "り" to "ゅ", "り" to "ょ",
    "ぎ" to "ゃ", "ぎ" to "ゅ", "ぎ" to "ょ",
    "じ" to "ゃ", "じ" to "ゅ", "じ" to "ょ",
    "ぢ" to "ゃ", "ぢ" to "ゅ", "ぢ" to "ょ",
    "び" to "ゃ", "び" to "ゅ", "び" to "ょ",
    "ぴ" to "ゃ", "ぴ" to "ゅ", "ぴ" to "ょ"
)

private val KATAKANA_YOON: List<Pair<String, String>> = listOf(
    "キ" to "ャ", "キ" to "ュ", "キ" to "ョ",
    "シ" to "ャ", "シ" to "ュ", "シ" to "ョ",
    "チ" to "ャ", "チ" to "ュ", "チ" to "ョ",
    "ニ" to "ャ", "ニ" to "ュ", "ニ" to "ョ",
    "ヒ" to "ャ", "ヒ" to "ュ", "ヒ" to "ョ",
    "ミ" to "ャ", "ミ" to "ュ", "ミ" to "ョ",
    "リ" to "ャ", "リ" to "ュ", "リ" to "ョ",
    "ギ" to "ャ", "ギ" to "ュ", "ギ" to "ョ",
    "ジ" to "ャ", "ジ" to "ュ", "ジ" to "ョ",
    "ヂ" to "ャ", "ヂ" to "ュ", "ヂ" to "ョ",
    "ビ" to "ャ", "ビ" to "ュ", "ビ" to "ョ",
    "ピ" to "ャ", "ピ" to "ュ", "ピ" to "ョ"
)

// ------------------------------------------------------------
// EXTENDED KATAKANA — valid loanword transcription
// clusters (small-vowel and ヴ series). Only pairs that
// actually occur in Japanese are included.
// ------------------------------------------------------------

private val EXTENDED_KATAKANA: List<Triple<String, String, Int>> = listOf(
    // ウ + small vowels (foreign w- sounds)
    Triple("ウィ", "wi", 5), Triple("ウェ", "we", 5), Triple("ウォ", "wo", 5),
    // ヴ series (foreign v- sounds; ヴ is ウ + dakuten)
    Triple("ヴ", "vu", 5), Triple("ヴァ", "va", 7), Triple("ヴィ", "vi", 7),
    Triple("ヴェ", "ve", 7), Triple("ヴォ", "vo", 7), Triple("ヴュ", "vyu", 6),
    // palatalised s/z/t + ェ
    Triple("シェ", "she", 5), Triple("ジェ", "je", 5), Triple("チェ", "che", 6),
    // ツ + small vowels (foreign ts- clusters)
    Triple("ツァ", "tsa", 5), Triple("ツィ", "tsi", 5), Triple("ツェ", "tse", 5), Triple("ツォ", "tso", 5),
    // テ/デ + small vowels
    Triple("ティ", "ti", 6), Triple("ディ", "di", 6), Triple("テュ", "tyu", 6), Triple("デュ", "dyu", 6),
    Triple("トゥ", "tu", 5), Triple("ドゥ", "du", 5),
    // フ + small vowels (foreign f- sounds)
    Triple("ファ", "fa", 4), Triple("フィ", "fi", 4), Triple("フェ", "fe", 4), Triple("フォ", "fo", 4),
    Triple("フュ", "fyu", 4),
    // ク/グ + small vowels (foreign q-/g- clusters)
    Triple("クァ", "kwa", 4), Triple("クィ", "kwi", 4), Triple("クェ", "kwe", 4), Triple("クォ", "kwo", 4),
    Triple("グァ", "gwa", 4), Triple("グィ", "gwi", 4), Triple("グェ", "gwe", 4), Triple("グォ", "gwo", 4),
    // ス/ズ + small i (foreign si-/zi- sounds)
    Triple("スィ", "si", 4), Triple("ズィ", "zi", 4)
)

// ------------------------------------------------------------
// CATALOG — the complete, deduplicated kana set.
// ------------------------------------------------------------

/** Every kana Kaiteyo knows about, in a stable display order. */
val kanaCatalog: List<KanaChar> by lazy {
    buildList {
        fun addBase(script: KanaScript, rows: List<Triple<String, String, Int>>) {
            rows.forEach { (ch, rom, strokes) ->
                add(
                    KanaChar(
                        character = ch,
                        romanization = rom,
                        script = script,
                        category = KanaCategory.Base,
                        strokeCount = strokes,
                        meaning = "${script.label} $ch — \"$rom\""
                    )
                )
            }
        }

        addBase(KanaScript.Hiragana, HIRAGANA_BASE)
        addBase(KanaScript.Katakana, KATAKANA_BASE)

        fun addVoiced(script: KanaScript, category: KanaCategory, rows: List<Triple<String, String, Int>>) {
            rows.forEach { (ch, rom, strokes) ->
                val baseChar = voicedBase(ch) ?: ""
                add(
                    KanaChar(
                        character = ch,
                        romanization = rom,
                        script = script,
                        category = category,
                        base = baseChar,
                        strokeCount = strokes,
                        meaning = "${script.label} $ch — \"$rom\" (${baseChar} + ${category.label})"
                    )
                )
            }
        }

        addVoiced(KanaScript.Hiragana, KanaCategory.Dakuten, HIRAGANA_DAKUTEN)
        addVoiced(KanaScript.Katakana, KanaCategory.Dakuten, KATAKANA_DAKUTEN)
        addVoiced(KanaScript.Hiragana, KanaCategory.Handakuten, HIRAGANA_HANDAKUTEN)
        addVoiced(KanaScript.Katakana, KanaCategory.Handakuten, KATAKANA_HANDAKUTEN)

        fun addYoOn(script: KanaScript, rows: List<Pair<String, String>>) {
            val baseTable = if (script == KanaScript.Hiragana) HIRAGANA_BASE + HIRAGANA_DAKUTEN + HIRAGANA_HANDAKUTEN
            else KATAKANA_BASE + KATAKANA_DAKUTEN + KATAKANA_HANDAKUTEN
            rows.forEach { (base, small) ->
                val baseRow = baseTable.first { it.first == base }
                val smallRow = when (script) {
                    KanaScript.Hiragana -> HIRAGANA_BASE.first { it.first == small }
                    KanaScript.Katakana -> KATAKANA_BASE.first { it.first == small }
                }
                val combo = base + small
                val rom = yoonRomanization(baseRow.second, smallRow.second)
                add(
                    KanaChar(
                        character = combo,
                        romanization = rom,
                        script = script,
                        category = KanaCategory.YoOn,
                        base = base,
                        small = small,
                        strokeCount = baseRow.third + smallRow.third,
                        meaning = "${script.label} $combo — \"$rom\" ($base + $small)"
                    )
                )
            }
        }

        addYoOn(KanaScript.Hiragana, HIRAGANA_YOON)
        addYoOn(KanaScript.Katakana, KATAKANA_YOON)

        // Extended katakana — relationship to their base form is kept where a
        // single dominant base exists (ティ → テ + ィ); otherwise blank.
        EXTENDED_KATAKANA.forEach { (ch, rom, strokes) ->
            val baseChar = extendedBase(ch)
            add(
                KanaChar(
                    character = ch,
                    romanization = rom,
                    script = KanaScript.Katakana,
                    category = KanaCategory.Extended,
                    base = baseChar.first,
                    small = baseChar.second,
                    strokeCount = strokes,
                    meaning = "Extended katakana $ch — \"$rom\""
                )
            )
        }
    }
}

/** Common romaji for a yōon pair (base + small vowel). */
private fun yoonRomanization(baseRom: String, smallRom: String): String = when (baseRom to smallRom) {
    // Palatalised consonants drop the i and take the y- glide.
    "ki" to "ya" -> "kya"; "ki" to "yu" -> "kyu"; "ki" to "yo" -> "kyo"
    "shi" to "ya" -> "sha"; "shi" to "yu" -> "shu"; "shi" to "yo" -> "sho"
    "chi" to "ya" -> "cha"; "chi" to "yu" -> "chu"; "chi" to "yo" -> "cho"
    "ni" to "ya" -> "nya"; "ni" to "yu" -> "nyu"; "ni" to "yo" -> "nyo"
    "hi" to "ya" -> "hya"; "hi" to "yu" -> "hyu"; "hi" to "yo" -> "hyo"
    "mi" to "ya" -> "mya"; "mi" to "yu" -> "myu"; "mi" to "yo" -> "myo"
    "ri" to "ya" -> "rya"; "ri" to "yu" -> "ryu"; "ri" to "yo" -> "ryo"
    "gi" to "ya" -> "gya"; "gi" to "yu" -> "gyu"; "gi" to "yo" -> "gyo"
    "ji" to "ya" -> "ja"; "ji" to "yu" -> "ju"; "ji" to "yo" -> "jo"
    "bi" to "ya" -> "bya"; "bi" to "yu" -> "byu"; "bi" to "yo" -> "byo"
    "pi" to "ya" -> "pya"; "pi" to "yu" -> "pyu"; "pi" to "yo" -> "pyo"
    else -> baseRom.removeSuffix("i") + smallRom
}

/** Resolve the dominant base form of an extended katakana cluster. */
private fun extendedBase(cluster: String): Pair<String, String> {
    if (cluster.length < 2) return "" to ""
    val first = cluster.substring(0, 1)
    val rest = cluster.substring(1)
    // ヴ is a single character (ウ + dakuten).
    if (cluster == "ヴ") return "ウ" to ""
    // The rest is usually a small-vowel kana (ァ ィ ゥ ェ ォ ュ ョ).
    return first to rest
}

/** Look up a single kana record by its characters. */
fun kanaByCharacter(character: String): KanaChar? = kanaCatalog.firstOrNull { it.character == character }

/** All kana of one script (optionally filtered to a category). */
fun kanaFor(script: KanaScript, category: KanaCategory? = null): List<KanaChar> =
    kanaCatalog.filter { it.script == script && (category == null || it.category == category) }

/** Kana usable in the writing engine (single-character units). */
fun writableKana(): List<KanaChar> = kanaCatalog.filter { KanaChar.isWritable(it.character) }

/** Kana whose shape equals another (small ゃ uses や's geometry), for stroke lookup. */
fun kanaShapeAlias(character: String): String? {
    val smallToBase = mapOf(
        "ぁ" to "あ", "ぃ" to "い", "ぅ" to "う", "ぇ" to "え", "ぉ" to "お",
        "ゃ" to "や", "ゅ" to "ゆ", "ょ" to "よ", "っ" to "つ",
        "ァ" to "ア", "ィ" to "イ", "ゥ" to "ウ", "ェ" to "エ", "ォ" to "オ",
        "ャ" to "ヤ", "ュ" to "ユ", "ョ" to "ヨ", "ッ" to "ツ",
        "ヵ" to "カ", "ヶ" to "ケ"
    )
    return smallToBase[character]
}

/** Whether the character is any kind of kana (single unit or cluster). */
fun isKanaCharacter(character: String): Boolean =
    kanaCatalog.any { it.character == character }
