package ua.syt0r.kanji.desktop.engine.jdata.engine

import ua.syt0r.kanji.desktop.engine.jdata.model.KanaEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.KanaScript
import ua.syt0r.kanji.desktop.engine.jdata.model.SourceRef
import ua.syt0r.kanji.desktop.engine.jdata.model.StableIds

// ============================================================
// KANA SUBSYSTEM
// Kana is first-class language data: hiragana AND katakana with
// romaji readings, script, stable IDs and — for the base gojūon —
// reference stroke counts (source: "kana-stroke-reference"). This
// is what writing practice and reading tools consume for kana, so
// the platform does not assume only kanji need writing data.
// ============================================================

object KanaSystem {

    private const val STROKE_SOURCE = "kana-stroke-reference"

    /** Base gojūon seeds with widely published stroke counts (46 + ん). */
    private val base = listOf(
        KanaSeed("あ", "ア", "a", 3), KanaSeed("い", "イ", "i", 2),
        KanaSeed("う", "ウ", "u", 3), KanaSeed("え", "エ", "e", 3),
        KanaSeed("お", "オ", "o", 3), KanaSeed("か", "カ", "ka", 3),
        KanaSeed("き", "キ", "ki", 4), KanaSeed("く", "ク", "ku", 1),
        KanaSeed("け", "ケ", "ke", 3), KanaSeed("こ", "コ", "ko", 2),
        KanaSeed("さ", "サ", "sa", 3), KanaSeed("し", "シ", "shi", 1),
        KanaSeed("す", "ス", "su", 2), KanaSeed("せ", "セ", "se", 3),
        KanaSeed("そ", "ソ", "so", 1), KanaSeed("た", "タ", "ta", 4),
        KanaSeed("ち", "チ", "chi", 2), KanaSeed("つ", "ツ", "tsu", 1),
        KanaSeed("て", "テ", "te", 1), KanaSeed("と", "ト", "to", 2),
        KanaSeed("な", "ナ", "na", 4), KanaSeed("に", "ニ", "ni", 3),
        KanaSeed("ぬ", "ヌ", "nu", 2), KanaSeed("ね", "ネ", "ne", 4),
        KanaSeed("の", "ノ", "no", 1), KanaSeed("は", "ハ", "ha", 3),
        KanaSeed("ひ", "ヒ", "hi", 1), KanaSeed("ふ", "フ", "fu", 4),
        KanaSeed("へ", "ヘ", "he", 1), KanaSeed("ほ", "ホ", "ho", 4),
        KanaSeed("ま", "マ", "ma", 3), KanaSeed("み", "ミ", "mi", 2),
        KanaSeed("む", "ム", "mu", 3), KanaSeed("め", "メ", "me", 2),
        KanaSeed("も", "モ", "mo", 3), KanaSeed("や", "ヤ", "ya", 3),
        KanaSeed("ゆ", "ユ", "yu", 2), KanaSeed("よ", "ヨ", "yo", 2),
        KanaSeed("ら", "ラ", "ra", 2), KanaSeed("り", "リ", "ri", 2),
        KanaSeed("る", "ル", "ru", 1), KanaSeed("れ", "レ", "re", 2),
        KanaSeed("ろ", "ロ", "ro", 1), KanaSeed("わ", "ワ", "wa", 2),
        KanaSeed("を", "ヲ", "wo", 3), KanaSeed("ん", "ン", "n", 1)
    )

    /** Dakuten (voiced) and handakuten (semi-voiced) kana — stroke counts not asserted. */
    private val voiced = listOf(
        KanaSeed("が", "ガ", "ga"), KanaSeed("ぎ", "ギ", "gi"), KanaSeed("ぐ", "グ", "gu"),
        KanaSeed("げ", "ゲ", "ge"), KanaSeed("ご", "ゴ", "go"),
        KanaSeed("ざ", "ザ", "za"), KanaSeed("じ", "ジ", "ji"), KanaSeed("ず", "ズ", "zu"),
        KanaSeed("ぜ", "ゼ", "ze"), KanaSeed("ぞ", "ゾ", "zo"),
        KanaSeed("だ", "ダ", "da"), KanaSeed("ぢ", "ヂ", "ji"), KanaSeed("づ", "ヅ", "zu"),
        KanaSeed("で", "デ", "de"), KanaSeed("ど", "ド", "do"),
        KanaSeed("ば", "バ", "ba"), KanaSeed("び", "ビ", "bi"), KanaSeed("ぶ", "ブ", "bu"),
        KanaSeed("べ", "ベ", "be"), KanaSeed("ぼ", "ボ", "bo"),
        KanaSeed("ぱ", "パ", "pa"), KanaSeed("ぴ", "ピ", "pi"), KanaSeed("ぷ", "プ", "pu"),
        KanaSeed("ぺ", "ペ", "pe"), KanaSeed("ぽ", "ポ", "po")
    )

    /** Yōon (contracted) kana. */
    private val yoon = listOf(
        KanaSeed("きゃ", "キャ", "kya"), KanaSeed("きゅ", "キュ", "kyu"), KanaSeed("きょ", "キョ", "kyo"),
        KanaSeed("しゃ", "シャ", "sha"), KanaSeed("しゅ", "シュ", "shu"), KanaSeed("しょ", "ショ", "sho"),
        KanaSeed("ちゃ", "チャ", "cha"), KanaSeed("ちゅ", "チュ", "chu"), KanaSeed("ちょ", "チョ", "cho"),
        KanaSeed("にゃ", "ニャ", "nya"), KanaSeed("にゅ", "ニュ", "nyu"), KanaSeed("にょ", "ニョ", "nyo"),
        KanaSeed("ひゃ", "ヒャ", "hya"), KanaSeed("ひゅ", "ヒュ", "hyu"), KanaSeed("ひょ", "ヒョ", "hyo"),
        KanaSeed("みゃ", "ミャ", "mya"), KanaSeed("みゅ", "ミュ", "myu"), KanaSeed("みょ", "ミョ", "myo"),
        KanaSeed("りゃ", "リャ", "rya"), KanaSeed("りゅ", "リュ", "ryu"), KanaSeed("りょ", "リョ", "ryo"),
        KanaSeed("ぎゃ", "ギャ", "gya"), KanaSeed("ぎゅ", "ギュ", "gyu"), KanaSeed("ぎょ", "ギョ", "gyo"),
        KanaSeed("じゃ", "ジャ", "ja"), KanaSeed("じゅ", "ジュ", "ju"), KanaSeed("じょ", "ジョ", "jo"),
        KanaSeed("びゃ", "ビャ", "bya"), KanaSeed("びゅ", "ビュ", "byu"), KanaSeed("びょ", "ビョ", "byo"),
        KanaSeed("ぴゃ", "ピャ", "pya"), KanaSeed("ぴゅ", "ピュ", "pyu"), KanaSeed("ぴょ", "ピョ", "pyo")
    )

    /** Small kana + prolonged sound mark. */
    private val special = listOf(
        KanaSeed("っ", "ッ", ""), KanaSeed("ゃ", "ャ", ""), KanaSeed("ゅ", "ュ", ""),
        KanaSeed("ょ", "ョ", ""), KanaSeed("ー", "ー", "")
    )

    private data class KanaSeed(
        val hiragana: String,
        val katakana: String,
        val romaji: String,
        val strokes: Int? = null
    )

    fun all(): Map<String, KanaEntry> = (base + voiced + yoon + special).map { seed ->
        listOf(
            KanaEntry(
                id = StableIds.kana(seed.hiragana),
                character = seed.hiragana,
                script = KanaScript.HIRAGANA,
                reading = seed.romaji,
                strokeCount = seed.strokes,
                sources = listOf(SourceRef(STROKE_SOURCE, seed.hiragana))
            ),
            KanaEntry(
                id = StableIds.kana(seed.katakana),
                character = seed.katakana,
                script = KanaScript.KATAKANA,
                reading = seed.romaji,
                strokeCount = seed.strokes,
                sources = listOf(SourceRef(STROKE_SOURCE, seed.katakana))
            )
        )
    }.flatten().associateBy { it.id }

    fun hiragana(): List<KanaEntry> = all().values.filter { it.script == KanaScript.HIRAGANA }
    fun katakana(): List<KanaEntry> = all().values.filter { it.script == KanaScript.KATAKANA }
}
