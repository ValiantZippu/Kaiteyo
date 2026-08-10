package ua.syt0r.kanji.desktop.data

import ua.syt0r.kanji.desktop.model.ContentKind
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.SrsStatus
import kotlin.random.Random
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus

// ============================================
// DEMO KANJI DATASET
// Curated real kanji entries used to seed the
// desktop suite so every view is alive on first
// run. Accuracy is best-effort for demo purposes;
// the production pipeline replaces these with the
// app-data repository.
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

/** Generate the demo card pool with realistic SRS spread. */
fun buildDemoCards(random: Random = Random(11)): List<DesktopCard> {
    val now = kotlinx.datetime.Clock.System.now()
    val cards = mutableListOf<DesktopCard>()
    demoKanji.forEachIndexed { index, seed ->
        val repBase = (index * 7) % 40
        val reps = repBase
        val lapses = (index * 3) % 6
        val interval = if (reps > 0) (reps * 1.3).coerceAtMost(180.0) else 0.0
        val status = when {
            reps == 0 -> SrsStatus.New
            lapses >= 3 -> SrsStatus.Relearning
            reps < 12 -> SrsStatus.Learning
            else -> SrsStatus.Review
        }
        val accuracy = when {
            reps == 0 -> 0.5f
            lapses >= 3 -> (0.35f + (index % 20) / 100f).coerceAtMost(0.7f)
            else -> (0.72f + (index % 25) / 100f).coerceAtMost(0.99f)
        }
        val dueAt = when (status) {
            SrsStatus.New -> null
            SrsStatus.Relearning -> now.minus((index % 3).toLong(), kotlinx.datetime.DateTimeUnit.MINUTE)
            else -> {
                val offsetDays = ((index * 5) % 30) - 12
                now.minus(-offsetDays.toLong(), kotlinx.datetime.DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            }
        }
        val lastReviewed = if (reps > 0) {
            now.minus(((index % 12) + 1).toLong(), kotlinx.datetime.DateTimeUnit.DAY, TimeZone.currentSystemDefault())
        } else null

        cards.add(
            DesktopCard(
                id = "kanji-${seed.character.hashCode().toString(16)}",
                character = seed.character,
                meaning = seed.meaning,
                onReadings = seed.on,
                kunReadings = seed.kun,
                radicals = seed.radicals,
                strokeCount = seed.strokes,
                jlpt = seed.jlpt,
                grade = seed.grade,
                frequency = seed.freq,
                tags = buildList {
                    add("jlpt-n${seed.jlpt}")
                    add("grade-${seed.grade}")
                    if (seed.radicals.isNotEmpty()) add("radical-${seed.radicals.first()}")
                },
                flags = if (index % 17 == 0) listOf("red") else if (index % 23 == 0) listOf("blue") else emptyList(),
                favorite = index % 13 == 0,
                status = status,
                intervalDays = interval,
                dueAt = dueAt,
                lapses = lapses,
                reps = reps,
                ease = 2.5,
                accuracy = accuracy,
                lastReviewedAt = lastReviewed,
                createdAt = now.minus((180 - index * 3).toLong(), kotlinx.datetime.DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            )
        )
    }
    return cards
}

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

// ============================================
// DEMO CONTENT DATASET
// Vocabulary, grammar, radicals and sentences that
// bring the built-in deck catalog to life on first
// run. Real entries (best-effort accuracy), tagged
// so the dynamic built-in decks resolve them.
// Replaced by imported dictionary content in
// production, without any schema change.
// ============================================

private data class VocabSeed(
    val word: String,
    val reading: String,
    val meaning: String,
    val jlpt: Int,
    val freq: Int,
    val tags: List<String>
)

private data class GrammarSeed(
    val pattern: String,
    val meaning: String,
    val example: String,
    val jlpt: Int,
    val level: String
)

private data class RadicalSeed(
    val glyph: String,
    val meaning: String,
    val variants: List<String>,
    val tag: String
)

private data class SentenceSeed(
    val japanese: String,
    val english: String,
    val jlpt: Int
)

private val demoVocab: List<VocabSeed> = listOf(
    VocabSeed("水", "みず", "water", 5, 208, listOf("core-1000")),
    VocabSeed("食べる", "たべる", "to eat", 5, 120, listOf("core-1000", "conversation")),
    VocabSeed("飲む", "のむ", "to drink", 5, 310, listOf("core-1000", "conversation")),
    VocabSeed("見る", "みる", "to see, to look", 5, 66, listOf("core-1000")),
    VocabSeed("聞く", "きく", "to listen, to ask", 5, 165, listOf("core-1000")),
    VocabSeed("行く", "いく", "to go", 5, 12, listOf("core-1000", "conversation")),
    VocabSeed("来る", "くる", "to come", 5, 44, listOf("core-1000")),
    VocabSeed("学校", "がっこう", "school", 5, 122, listOf("core-1000")),
    VocabSeed("先生", "せんせい", "teacher", 5, 93, listOf("core-1000", "conversation")),
    VocabSeed("友達", "ともだち", "friend", 5, 210, listOf("core-1000", "conversation")),
    VocabSeed("家族", "かぞく", "family", 5, 180, listOf("core-1000")),
    VocabSeed("時間", "じかん", "time", 5, 16, listOf("core-1000")),
    VocabSeed("今日", "きょう", "today", 5, 23, listOf("core-1000", "conversation")),
    VocabSeed("明日", "あした", "tomorrow", 5, 55, listOf("core-1000", "conversation")),
    VocabSeed("電車", "でんしゃ", "train", 4, 127, listOf("core-1000", "news")),
    VocabSeed("駅員", "えきいん", "station attendant", 4, 194, listOf("core-2000", "news")),
    VocabSeed("空港", "くうこう", "airport", 4, 113, listOf("core-2000", "news")),
    VocabSeed("地震", "じしん", "earthquake", 4, 260, listOf("core-2000", "news")),
    VocabSeed("台風", "たいふう", "typhoon", 4, 290, listOf("core-2000", "news")),
    VocabSeed("増える", "ふえる", "to increase", 4, 305, listOf("core-2000", "news")),
    VocabSeed("減る", "へる", "to decrease", 4, 330, listOf("core-2000", "news")),
    VocabSeed("世界", "せかい", "world", 4, 20, listOf("core-1000", "news")),
    VocabSeed("経済", "けいざい", "economy", 3, 27, listOf("core-1000", "news")),
    VocabSeed("政治", "せいじ", "politics", 3, 58, listOf("core-2000", "news")),
    VocabSeed("社会", "しゃかい", "society", 3, 41, listOf("core-1000", "news")),
    VocabSeed("環境", "かんきょう", "environment", 3, 95, listOf("core-2000", "news")),
    VocabSeed("増加", "ぞうか", "increase (noun)", 3, 350, listOf("core-2000", "news")),
    VocabSeed("減少", "げんしょう", "decrease (noun)", 3, 380, listOf("core-2000", "news")),
    VocabSeed("解決", "かいけつ", "resolution, solution", 2, 240, listOf("core-2000", "news")),
    VocabSeed("影響", "えいきょう", "influence, effect", 2, 150, listOf("core-1000", "news")),
    VocabSeed("拡大", "かくだい", "expansion", 2, 320, listOf("core-2000", "news")),
    VocabSeed("強化", "きょうか", "strengthening", 2, 340, listOf("core-2000", "news")),
    VocabSeed("衰退", "すいたい", "decline", 1, 480, listOf("core-3000", "news")),
    VocabSeed("履行", "りこう", "implementation, performance", 1, 520, listOf("core-3000", "news")),
    VocabSeed("逸脱", "いつだつ", "deviation", 1, 610, listOf("core-3000", "news")),
    VocabSeed("頑張る", "がんばる", "to do one's best", 4, 130, listOf("core-1000", "anime", "conversation")),
    VocabSeed("憧れ", "あこがれ", "longing, admiration", 4, 420, listOf("core-2000", "anime")),
    VocabSeed("仲間", "なかま", "comrade, fellow", 3, 280, listOf("core-1000", "anime")),
    VocabSeed("勇者", "ゆうしゃ", "hero, brave warrior", 3, 450, listOf("core-2000", "anime")),
    VocabSeed("魔王", "まおう", "demon king", 3, 500, listOf("core-3000", "anime")),
    VocabSeed("魔法", "まほう", "magic", 4, 300, listOf("core-1000", "anime")),
    VocabSeed("冒険", "ぼうけん", "adventure", 3, 310, listOf("core-2000", "anime")),
    VocabSeed("宿命", "しゅくめい", "destiny, fate", 2, 460, listOf("core-3000", "anime")),
    VocabSeed("絆", "きずな", "bond, ties", 2, 380, listOf("core-2000", "anime")),
    VocabSeed("瞬き", "まばたき", "blink", 2, 520, listOf("core-3000", "anime"))
)

private val demoGrammar: List<GrammarSeed> = listOf(
    GrammarSeed("〜ながら", "while doing; both … and …", "音楽を聞きながら勉強します。", 5, "basic-grammar"),
    GrammarSeed("〜てもいい", "may do; it's okay to do", "ここに座ってもいいですか。", 5, "basic-grammar"),
    GrammarSeed("〜なくてはいけない", "must do (informal: 〜なくちゃ)", "薬を飲まなくてはいけません。", 5, "basic-grammar"),
    GrammarSeed("〜たばかり", "just did (recently)", "さっき食べたばかりです。", 4, "basic-grammar"),
    GrammarSeed("〜そうだ", "looks like; seems (appearance)", "雨が降りそうだ。", 4, "basic-grammar"),
    GrammarSeed("〜てしまう", "to finish doing; regrettably does", "ケーキを全部食べてしまった。", 4, "basic-grammar"),
    GrammarSeed("〜ばかりでなく", "not only … but also", "彼は日本語ばかりでなく英語も話せる。", 3, "intermediate"),
    GrammarSeed("〜わりに", "considering that; although", "あのレストランは値段のわりに美味しい。", 3, "intermediate"),
    GrammarSeed("〜ものの", "although; even though", "高いものの、品質は良い。", 2, "intermediate"),
    GrammarSeed("〜つつある", "is in the process of doing", "経済は回復しつつある。", 2, "advanced"),
    GrammarSeed("〜がたい", "hard to do; difficult to", "信じがたい事実だ。", 2, "advanced"),
    GrammarSeed("〜までもなく", "needless to say; without even doing", "言うまでもなく、安全が最優先だ。", 1, "advanced")
)

private val demoRadicals: List<RadicalSeed> = listOf(
    RadicalSeed("氵", "water", listOf("水", "氵"), "radical-basic"),
    RadicalSeed("火", "fire", listOf("灬"), "radical-basic"),
    RadicalSeed("木", "tree, wood", listOf("木"), "radical-basic"),
    RadicalSeed("人", "person", listOf("亻", "𠆢"), "radical-basic"),
    RadicalSeed("口", "mouth, opening", listOf("口"), "radical-basic"),
    RadicalSeed("日", "sun, day", listOf("日"), "radical-basic"),
    RadicalSeed("月", "moon, month", listOf("月"), "radical-basic"),
    RadicalSeed("心", "heart, mind", listOf("忄", "㣺"), "radical-basic"),
    RadicalSeed("言", "speech, say", listOf("訁"), "radical-basic"),
    RadicalSeed("辶", "walking, movement", listOf("辶"), "radical-extended"),
    RadicalSeed("阝", "village/urban (left/right ear)", listOf("阜", "邑"), "radical-extended"),
    RadicalSeed("艹", "grass, plants", listOf("艸"), "radical-extended"),
    RadicalSeed("宀", "roof, house", listOf("宀"), "radical-extended"),
    RadicalSeed("疒", "sickness, illness", listOf("疒"), "radical-extended"),
    RadicalSeed("彳", "step, going slowly", listOf("彳"), "component"),
    RadicalSeed("幺", "short thread, tiny", listOf("幺"), "component"),
    RadicalSeed("豕", "pig, hog", listOf("豕"), "component"),
    RadicalSeed("臼", "mortar, grinding", listOf("臼"), "component")
)

private val demoSentences: List<SentenceSeed> = listOf(
    SentenceSeed("私は毎朝コーヒーを飲みます。", "I drink coffee every morning.", 5),
    SentenceSeed("電車で学校へ行きます。", "I go to school by train.", 5),
    SentenceSeed("昨日、友達と映画を見ました。", "Yesterday I watched a movie with a friend.", 5),
    SentenceSeed("新しい本を買って、すぐに読み始めた。", "I bought a new book and started reading it right away.", 4),
    SentenceSeed("台風のため、飛行機が欠航した。", "The flight was cancelled because of the typhoon.", 4),
    SentenceSeed("経済は少しずつ回復しつつある。", "The economy is gradually recovering.", 3),
    SentenceSeed("環境問題の解決には国際的な協力が必要だ。", "International cooperation is needed to solve environmental problems.", 3),
    SentenceSeed("彼は言うまでもなく、チームの中心的存在だ。", "Needless to say, he is the center of the team.", 2)
)

/** Generate the non-kanji demo content (vocabulary, grammar, radicals, sentences). */
fun buildDemoContentCards(random: Random = Random(31)): List<DesktopCard> {
    val now = kotlinx.datetime.Clock.System.now()
    val cards = mutableListOf<DesktopCard>()

    demoVocab.forEachIndexed { index, seed ->
        val reps = (index * 5) % 18
        val status = when {
            reps == 0 -> SrsStatus.New
            reps < 8 -> SrsStatus.Learning
            else -> SrsStatus.Review
        }
        cards.add(
            DesktopCard(
                id = "vocab-${seed.word.hashCode().toString(16)}",
                character = seed.word,
                meaning = seed.meaning,
                onReadings = listOf(seed.reading),
                jlpt = seed.jlpt,
                frequency = seed.freq,
                tags = buildList {
                    add("jlpt-n${seed.jlpt}")
                    addAll(seed.tags)
                },
                favorite = index % 19 == 0,
                status = status,
                intervalDays = (reps * 1.3).toDouble(),
                reps = reps,
                accuracy = (0.6f + (index % 30) / 100f).coerceAtMost(0.98f),
                contentKind = ContentKind.Vocabulary,
                createdAt = now.minus((120 - index).toLong(), kotlinx.datetime.DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            )
        )
    }

    demoGrammar.forEachIndexed { index, seed ->
        val reps = (index * 6) % 14
        cards.add(
            DesktopCard(
                id = "grammar-${seed.pattern.hashCode().toString(16)}",
                character = seed.pattern,
                meaning = seed.meaning,
                note = seed.example,
                jlpt = seed.jlpt,
                tags = listOf("jlpt-n${seed.jlpt}", seed.level),
                status = if (reps == 0) SrsStatus.New else if (index % 3 == 0) SrsStatus.Relearning else SrsStatus.Review,
                intervalDays = (reps * 1.6).toDouble(),
                reps = reps,
                accuracy = (0.55f + (index % 25) / 100f).coerceAtMost(0.95f),
                contentKind = ContentKind.Grammar,
                createdAt = now.minus((110 - index).toLong(), kotlinx.datetime.DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            )
        )
    }

    demoRadicals.forEachIndexed { index, seed ->
        cards.add(
            DesktopCard(
                id = "radical-${seed.glyph.hashCode().toString(16)}",
                character = seed.glyph,
                meaning = seed.meaning,
                radicals = listOf(seed.glyph),
                components = seed.variants,
                tags = listOf(seed.tag),
                favorite = index % 7 == 0,
                status = if (index % 2 == 0) SrsStatus.New else SrsStatus.Learning,
                reps = if (index % 2 == 0) 0 else 3,
                intervalDays = 0.5,
                accuracy = 0.75f,
                contentKind = ContentKind.Radical,
                createdAt = now.minus((100 - index).toLong(), kotlinx.datetime.DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            )
        )
    }

    demoSentences.forEachIndexed { index, seed ->
        cards.add(
            DesktopCard(
                id = "sentence-${seed.japanese.hashCode().toString(16)}",
                character = seed.japanese,
                meaning = seed.english,
                jlpt = seed.jlpt,
                tags = listOf("jlpt-n${seed.jlpt}", "example"),
                status = SrsStatus.New,
                reps = 0,
                accuracy = 0.5f,
                contentKind = ContentKind.Sentence,
                createdAt = now.minus((90 - index).toLong(), kotlinx.datetime.DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            )
        )
    }

    return cards
}
