package ua.syt0r.kanji.desktop.data

import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.SrsStatus
import kotlin.random.Random
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus

// ============================================
// KANJI REFERENCE DATASET
// Curated real kanji entries backing the bundled
// reference dictionary (offline lookup) and the
// stress dataset (manual perf tool). This is NOT
// seeded into the user library: first run starts
// empty and study content is earned, never
// pre-filled. Accuracy is best-effort for the
// bundled reference; the production pipeline
// replaces it with the app-data repository.
// ============================================

data class KanjiSeed(
    val character: String,
    val meaning: String,
    val on: List<String>,
    val kun: List<String>,
    val radicals: List<String>,
    val strokes: Int,
    val jlpt: Int,
    val grade: Int,
    val freq: Int
)

val demoKanji: List<KanjiSeed> = listOf(
    KanjiSeed("日", "sun; day", listOf("ニチ", "ジツ"), listOf("ひ", "-び", "-か"), listOf("日"), 4, 5, 1, 3),
    KanjiSeed("月", "moon; month", listOf("ゲツ", "ガツ"), listOf("つき"), listOf("月"), 4, 5, 1, 33),
    KanjiSeed("水", "water", listOf("スイ"), listOf("みず"), listOf("水"), 4, 5, 1, 208),
    KanjiSeed("火", "fire", listOf("カ"), listOf("ひ", "-び"), listOf("火"), 4, 5, 1, 168),
    KanjiSeed("木", "tree; wood", listOf("モク", "ボク"), listOf("き", "-ぎ"), listOf("木"), 4, 5, 1, 252),
    KanjiSeed("金", "gold; money", listOf("キン", "コン"), listOf("かね", "-がね"), listOf("金"), 8, 5, 1, 144),
    KanjiSeed("土", "soil; earth", listOf("ド", "ト"), listOf("つち"), listOf("土"), 3, 5, 1, 340),
    KanjiSeed("人", "person", listOf("ジン", "ニン"), listOf("ひと", "-り", "-と"), listOf("人"), 2, 5, 1, 4),
    KanjiSeed("山", "mountain", listOf("サン"), listOf("やま"), listOf("山"), 3, 5, 1, 108),
    KanjiSeed("川", "river; stream", listOf("セン"), listOf("かわ"), listOf("川"), 3, 5, 1, 220),
    KanjiSeed("田", "rice field", listOf("デン"), listOf("た"), listOf("田"), 5, 5, 1, 240),
    KanjiSeed("天", "heavens; sky", listOf("テン"), listOf("あま", "あめ"), listOf("大"), 4, 5, 1, 173),
    KanjiSeed("気", "spirit; energy", listOf("キ", "ケ"), listOf("いき"), listOf("气"), 6, 5, 2, 48),
    KanjiSeed("休", "rest", listOf("キュウ"), listOf("やす", "やすみ"), listOf("亻", "木"), 6, 5, 1, 306),
    KanjiSeed("行", "go; line", listOf("コウ", "ギョウ"), listOf("い", "ゆ", "おこな"), listOf("彳", "一"), 6, 5, 2, 12),
    KanjiSeed("電", "electricity", listOf("デン"), listOf("いなずま"), listOf("雨", "田"), 13, 5, 2, 127),
    KanjiSeed("車", "car; wheel", listOf("シャ"), listOf("くるま"), listOf("車"), 7, 5, 1, 85),
    KanjiSeed("間", "interval; between", listOf("カン", "ケン"), listOf("あいだ", "ま"), listOf("門", "日"), 12, 5, 2, 29),
    KanjiSeed("書", "write", listOf("ショ"), listOf("か", "がき"), listOf("聿", "日"), 10, 5, 2, 204),
    KanjiSeed("学", "study; learning", listOf("ガク"), listOf("まな"), listOf("子", "冖"), 8, 5, 1, 69),
    KanjiSeed("先", "previous; ahead", listOf("セン"), listOf("さき"), listOf("儿", "土"), 6, 5, 1, 93),
    KanjiSeed("生", "life; birth", listOf("セイ", "ショウ"), listOf("い", "う", "なま"), listOf("生"), 5, 5, 1, 10),
    KanjiSeed("大", "big", listOf("ダイ", "タイ"), listOf("おお"), listOf("大"), 3, 5, 1, 5),
    KanjiSeed("小", "small", listOf("ショウ"), listOf("ちい", "こ"), listOf("小"), 3, 5, 1, 63),
    KanjiSeed("中", "middle; inside", listOf("チュウ"), listOf("なか"), listOf("丨"), 4, 5, 1, 8),
    KanjiSeed("上", "above; up", listOf("ジョウ"), listOf("うえ", "あ", "のぼ"), listOf("一", "卜"), 3, 5, 1, 11),
    KanjiSeed("下", "below; down", listOf("カ", "ゲ"), listOf("した", "さ", "くだ", "お"), listOf("一", "卜"), 3, 5, 1, 14),
    KanjiSeed("右", "right", listOf("ウ", "ユウ"), listOf("みぎ"), listOf("口", "一"), 5, 5, 1, 380),
    KanjiSeed("左", "left", listOf("サ"), listOf("ひだり"), listOf("工", "一"), 5, 5, 1, 391),
    KanjiSeed("前", "before; front", listOf("ゼン"), listOf("まえ"), listOf("刂", "月"), 9, 5, 2, 28),
    KanjiSeed("後", "after; behind", listOf("ゴ", "コウ"), listOf("あと", "うしろ"), listOf("彳", "幺"), 9, 5, 2, 35),
    KanjiSeed("家", "house; family", listOf("カ", "ケ"), listOf("いえ", "や"), listOf("宀", "豕"), 10, 5, 2, 39),
    KanjiSeed("国", "country", listOf("コク"), listOf("くに"), listOf("囗", "玉"), 8, 5, 2, 20),
    KanjiSeed("校", "school", listOf("コウ"), listOf("せい"), listOf("木", "交"), 10, 5, 1, 122),
    KanjiSeed("駅", "station", listOf("エキ"), listOf("うまや"), listOf("馬", "阝"), 14, 4, 3, 194),
    KanjiSeed("空", "sky; empty", listOf("クウ"), listOf("そら", "あ", "から"), listOf("穴", "工"), 8, 4, 1, 113),
    KanjiSeed("海", "sea; ocean", listOf("カイ"), listOf("うみ"), listOf("氵", "毎"), 9, 4, 2, 76),
    KanjiSeed("道", "road; way", listOf("ドウ", "トウ"), listOf("みち"), listOf("辶", "首"), 12, 4, 2, 46),
    KanjiSeed("買", "buy", listOf("バイ"), listOf("か"), listOf("貝", "罒"), 12, 4, 2, 320),
    KanjiSeed("会", "meet; society", listOf("カイ", "エ"), listOf("あ"), listOf("人", "云"), 6, 4, 2, 40),
    KanjiSeed("話", "talk; story", listOf("ワ"), listOf("はな", "ばなし"), listOf("言", "舌"), 13, 4, 2, 71),
    KanjiSeed("聞", "hear; ask", listOf("ブン", "モン"), listOf("き"), listOf("門", "耳"), 14, 4, 2, 165),
    KanjiSeed("読", "read", listOf("ドク", "トク", "トウ"), listOf("よ"), listOf("言", "売"), 14, 4, 2, 121),
    KanjiSeed("見", "see; look", listOf("ケン"), listOf("み"), listOf("見"), 7, 4, 1, 66),
    KanjiSeed("食", "eat; food", listOf("ショク", "ジキ"), listOf("た", "く"), listOf("食"), 9, 4, 2, 162),
    KanjiSeed("飲", "drink", listOf("イン"), listOf("の"), listOf("食", "欠"), 12, 4, 3, 370),
    KanjiSeed("新", "new", listOf("シン"), listOf("あたら", "にい"), listOf("斤", "木"), 13, 4, 2, 86),
    KanjiSeed("古", "old", listOf("コ"), listOf("ふる", "いにしえ"), listOf("十", "口"), 5, 4, 1, 236),
    KanjiSeed("長", "long; leader", listOf("チョウ"), listOf("なが"), listOf("長"), 8, 4, 2, 30),
    KanjiSeed("思", "think", listOf("シ"), listOf("おも"), listOf("田", "心"), 9, 4, 2, 64),
    KanjiSeed("時", "time; hour", listOf("ジ"), listOf("とき", "どき"), listOf("日", "寺"), 10, 4, 2, 16),
    KanjiSeed("曜", "day of the week", listOf("ヨウ"), listOf("ひかり"), listOf("日", "翟"), 18, 4, 2, 410),
    KanjiSeed("手", "hand", listOf("シュ"), listOf("て", "た"), listOf("手"), 4, 4, 1, 74),
    KanjiSeed("足", "foot; sufficient", listOf("ソク"), listOf("あし", "た"), listOf("足"), 7, 4, 1, 149),
    KanjiSeed("雨", "rain", listOf("ウ"), listOf("あめ", "あま"), listOf("雨"), 8, 4, 1, 258),
    KanjiSeed("雪", "snow", listOf("セツ"), listOf("ゆき"), listOf("雨", "彐"), 11, 4, 2, 480),
    KanjiSeed("花", "flower", listOf("カ"), listOf("はな"), listOf("艹", "化"), 7, 4, 1, 274),
    KanjiSeed("言", "say; word", listOf("ゲン", "ゴン"), listOf("い", "こと"), listOf("言"), 7, 4, 2, 59),
    KanjiSeed("語", "language; word", listOf("ゴ"), listOf("かた"), listOf("言", "吾"), 14, 4, 2, 155),
    KanjiSeed("医", "medicine; doctor", listOf("イ"), listOf("いやす"), listOf("匚", "矢"), 7, 4, 3, 405),
    KanjiSeed("病", "sick; illness", listOf("ビョウ", "ヘイ"), listOf("やまい"), listOf("疒", "丙"), 10, 4, 3, 233),
    KanjiSeed("族", "family; tribe", listOf("ゾク"), listOf("やから"), listOf("方", "矢"), 11, 4, 3, 141),
    KanjiSeed("親", "parent; close", listOf("シン"), listOf("おや", "した"), listOf("立", "見", "木"), 16, 3, 2, 138),
    KanjiSeed("自", "self", listOf("ジ", "シ"), listOf("みずか"), listOf("自"), 6, 4, 2, 51),
    KanjiSeed("動", "move; motion", listOf("ドウ"), listOf("うご"), listOf("重", "力"), 11, 4, 3, 47),
    KanjiSeed("働", "work; labor", listOf("ドウ"), listOf("はたら"), listOf("亻", "動"), 13, 3, 4, 120),
    KanjiSeed("物", "thing; matter", listOf("ブツ", "モツ"), listOf("もの"), listOf("牛", "勿"), 8, 4, 3, 25),
    KanjiSeed("買", "buy", listOf("バイ"), listOf("か"), listOf("貝", "罒"), 12, 4, 2, 320),
    KanjiSeed("着", "wear; arrive", listOf("チャク", "ジャク"), listOf("き", "つ"), listOf("羊", "目"), 12, 3, 3, 96),
    KanjiSeed("走", "run", listOf("ソウ"), listOf("はし"), listOf("走"), 7, 4, 2, 275),
    KanjiSeed("乗", "ride; board", listOf("ジョウ"), listOf("の"), listOf("禾", "北"), 9, 3, 3, 222),
    KanjiSeed("降", "descend; alight", listOf("コウ"), listOf("お", "ふ"), listOf("阝", "夂"), 10, 3, 6, 365),
    KanjiSeed("東", "east", listOf("トウ"), listOf("ひがし"), listOf("木", "日"), 8, 4, 2, 60),
    KanjiSeed("南", "south", listOf("ナン", "ナ"), listOf("みなみ"), listOf("十", "冂"), 9, 4, 2, 150),
    KanjiSeed("西", "west", listOf("セイ", "サイ"), listOf("にし"), listOf("西"), 6, 4, 2, 130),
    KanjiSeed("北", "north", listOf("ホク"), listOf("きた"), listOf("匕"), 5, 4, 2, 97)
)

/** Generate a stress dataset of [count] synthetic cards (for perf demos). */
fun buildStressDataset(count: Int, random: Random = Random(42)): List<DesktopCard> {
    val base = demoKanji
    return (0 until count).map { i ->
        val seed = base[i % base.size]
        val deckId = "deck-${i % 8}"
        val reps = random.nextInt(0, 80)
        DesktopCard(
            id = "stress-$i",
            character = seed.character,
            meaning = "${seed.meaning} #$i",
            onReadings = seed.on,
            kunReadings = seed.kun,
            radicals = seed.radicals,
            strokeCount = seed.strokes,
            jlpt = seed.jlpt,
            grade = seed.grade,
            frequency = seed.freq + i % 500,
            tags = listOf("jlpt-n${seed.jlpt}", "deck-$deckId"),
            flags = if (i % 29 == 0) listOf("yellow") else emptyList(),
            favorite = i % 11 == 0,
            status = when {
                reps == 0 -> SrsStatus.New
                reps < 10 -> SrsStatus.Learning
                i % 37 == 0 -> SrsStatus.Relearning
                else -> SrsStatus.Review
            },
            intervalDays = (reps * 1.2).toDouble(),
            lapses = i % 7,
            reps = reps,
            accuracy = 0.5f + (i % 50) / 100f,
            deckId = deckId,
            createdAt = kotlinx.datetime.Clock.System.now().minus((count - i).toLong() / 10, kotlinx.datetime.DateTimeUnit.DAY, TimeZone.currentSystemDefault())
        )
    }
}

