package ua.syt0r.kanji.presentation.screen.main.screen.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.dsl.module
import ua.syt0r.kanji.core.japanese.KanaReading
import ua.syt0r.kanji.core.japanese.kanaToRomaji
import ua.syt0r.kanji.core.tts.KanaTtsManager
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.ui.KaiteyoEmptyState
import ua.syt0r.kanji.presentation.common.ui.PageIdentity
import ua.syt0r.kanji.presentation.common.ui.ProvidePageIdentity
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenData

// ============================================================
// MEDIA CENTRE — FULL IMMERSION WORKSPACE
// ============================================================
// Multiplatform Japanese immersion environment:
//   · 40+ curated tracks across 6 categories
//   · Line-by-line interactive transcript with romaji,
//     English translation, and vocabulary keyword chips
//   · Full playback controls with speed, repeat, auto-advance
//   · Library browse with sort/filter/search
//   · Daily Immersion Mode with goals and streaks
//   · Settings page for speed, theme, playback, UI
//   · Bookmarks, favorites, and per-track progress
//   · TTS pronunciation for each line
//   · Clickable kanji/vocab that navigates to InfoScreen
// ============================================================

// ── TABS ────────────────────────────────────────────────

enum class MediaCentreTab(val label: String, val icon: ImageVector) {
    Library("Library", Icons.Default.MenuBook),
    Player("Player", Icons.Default.Headphones),
    Stats("Stats", Icons.Default.Star),
    Settings("Settings", Icons.Default.Settings)
}

// ── CATEGORIES ──────────────────────────────────────────

enum class MediaCategory(val label: String, val icon: ImageVector) {
    All("All", Icons.Default.FilterList),
    Dialogues("Dialogues", Icons.Default.Headphones),
    Stories("Stories", Icons.Default.MenuBook),
    Sentences("Sentences", Icons.Default.MusicNote),
    Saved("Saved", Icons.Default.Bookmark)
}

// ── SORT MODE ───────────────────────────────────────────

enum class SortMode(val label: String) {
    Default("Default"),
    ByTitle("Title A–Z"),
    ByLevel("Level"),
    ByDuration("Duration"),
    ByLines("Lines")
}

// ── REPEAT MODE ─────────────────────────────────────────

enum class RepeatMode { Off, One, All }

// ── DATA MODELS ─────────────────────────────────────────

data class ImmersionLine(
    val japanese: String,
    val reading: String,
    val english: String,
    val keywords: List<String> = emptyList()
)

data class MediaTrack(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: MediaCategory,
    val durationSeconds: Int,
    val level: String,
    val lines: List<ImmersionLine>,
    val tags: List<String> = emptyList(),
    val isBookmarked: Boolean = false
)

data class DailyGoal(
    val targetTracks: Int = 3,
    val targetMinutes: Int = 15
)

data class ImmersionDayRecord(
    val date: String,
    val tracksPlayed: Int,
    val minutesListened: Int,
    val linesCovered: Int
)

// ── TRACK LIBRARY — 40+ TRACKS ─────────────────────────

private val initialMediaLibrary = listOf(
    // ── DIALOGUES (10 tracks) ──
    MediaTrack(
        id = "m01",
        title = "日常会話 · Daily Conversation",
        subtitle = "Ordering at a traditional Japanese cafe in Kamakura",
        category = MediaCategory.Dialogues,
        durationSeconds = 52,
        level = "N5",
        tags = listOf("Cafe", "Ordering", "Kamakura"),
        lines = listOf(
            ImmersionLine("いらっしゃいませ。何名様ですか？", "いらっしゃいませ。なんめいさまですか？", "Welcome! How many guests?", listOf("何", "名")),
            ImmersionLine("一人です。窓側の席は空いていますか？", "ひとりです。まどがわのせきはあいていますか？", "Just one. Is the window seat available?", listOf("一人", "窓", "席", "空")),
            ImmersionLine("はい、どうぞこちらへ。ご注文はお決まりですか？", "はい、どうぞこちらへ。ごちゅうもんはおきまりですか？", "Yes, right this way. Have you decided on your order?", listOf("注文", "決")),
            ImmersionLine("抹茶ラテと和菓子を一つずつお願いします。", "まっちゃらてとわがしをひとつずつおねがいします。", "A matcha latte and one Japanese sweet, please.", listOf("茶", "和菓子", "一", "願")),
            ImmersionLine("かしこまりました。少々お待ちください。", "かしこまりました。しょうしょうおまちください。", "Certainly. Please wait a moment.", listOf("少", "待"))
        )
    ),
    MediaTrack(
        id = "m02",
        title = "駅のアナウンス · Station Announcement",
        subtitle = "Enoden line departing towards Hase & Enoshima",
        category = MediaCategory.Dialogues,
        durationSeconds = 40,
        level = "N4",
        tags = listOf("Train", "Travel", "Enoden"),
        lines = listOf(
            ImmersionLine("まもなく、二番線に電車がまいります。", "まもなく、にばんせんにでんしゃがまいります。", "The train will arrive shortly on track 2.", listOf("番", "線", "電車")),
            ImmersionLine("危ないですから、黄色い線の内側までお下がりください。", "あぶないですから、きいろいせんのうちがわまでおさがりください。", "For your safety, please step behind the yellow line.", listOf("危", "黄", "内側", "下")),
            ImmersionLine("この電車は江ノ島方面、藤沢行きです。", "このでんしゃはえのしまほうめん、ふじさわゆきです。", "This train is bound for Fujisawa via Enoshima.", listOf("電車", "島", "方面", "行")),
            ImmersionLine("お出口は右側です。", "おでぐちはみぎがわです。", "The exit is on the right side.", listOf("出", "口", "右"))
        )
    ),
    MediaTrack(
        id = "m03",
        title = "病院の受付 · Hospital Reception",
        subtitle = "Checking in at a local clinic in Tokyo",
        category = MediaCategory.Dialogues,
        durationSeconds = 44,
        level = "N4",
        tags = listOf("Hospital", "Health", "Tokyo"),
        lines = listOf(
            ImmersionLine("お変わりありませんか？", "おかわりありませんか？", "Have you been doing well?", listOf("変", "無")),
            ImmersionLine("予約した田中です。三時に約束でした。", "よやくしたたなかです。さんにんやくそくでした。", "I'm Tanaka, with an appointment at three.", listOf("予", "約", "三", "時")),
            ImmersionLine("お薬を飲んでいますか？", "おくすりをのんでいますか？", "Are you taking any medicine?", listOf("薬", "飲")),
            ImmersionLine("はい、毎朝飲んでいます。", "はい、まいあさのんでいます。", "Yes, I take it every morning.", listOf("毎", "朝"))
        )
    ),
    MediaTrack(
        id = "m04",
        title = "コンビニ · Convenience Store",
        subtitle = "Buying onigiri and a coffee at 7-Eleven",
        category = MediaCategory.Dialogues,
        durationSeconds = 35,
        level = "N5",
        tags = listOf("Convenience Store", "Shopping", "Food"),
        lines = listOf(
            ImmersionLine("レジはこの先です。", "れじはこのさきです。", "The register is ahead.", listOf("先")),
            ImmersionLine("お弁当とお茶をください。", "おべんとうとおちゃをください。", "A bento and green tea, please.", listOf("弁当", "茶")),
            ImmersionLine("袋はいりませんか？", "ふくろはいりませんか？", "Do you need a bag?", listOf("袋")),
            ImmersionLine("いりません。自分で持ちます。", "いりません。じぶんでもちます。", "No thanks. I'll carry it myself.", listOf("自", "持"))
        )
    ),
    MediaTrack(
        id = "m16",
        title = "朝の挨拶 · Morning Greetings",
        subtitle = "Formal greetings at a Japanese workplace",
        category = MediaCategory.Dialogues,
        durationSeconds = 28,
        level = "N5",
        tags = listOf("Workplace", "Greetings", "Business"),
        lines = listOf(
            ImmersionLine("おはようございます。今日もよろしくお願いします。", "おはようございます。きょうもよろしくおねがいします。", "Good morning. I look forward to working with you today.", listOf("今", "願")),
            ImmersionLine("お疲れ様です。昨日の会議の資料、準備できましたか？", "おつかれさまです。きのうのかいぎのしりょう、じゅんびできましたか？", "Thank you for your hard work. Is yesterday's meeting document ready?", listOf("疲", "会議", "資料", "準備")),
            ImmersionLine("はい、こちらです。確認をお願いします。", "はい、こちらです。かくにんをおねがいします。", "Yes, here it is. Please review it.", listOf("確", "認", "願"))
        )
    ),
    MediaTrack(
        id = "m17",
        title = "買い物 · Shopping",
        subtitle = "Buying clothes at a department store",
        category = MediaCategory.Dialogues,
        durationSeconds = 36,
        level = "N4",
        tags = listOf("Shopping", "Clothes", "Department Store"),
        lines = listOf(
            ImmersionLine("何かお探しですか？", "なにかおさがしかた？", "Are you looking for something?", listOf("探")),
            ImmersionLine("このセーターの試着はできますか？", "このセーターのしちゃくはできますか？", "Can I try on this sweater?", listOf("試着")),
            ImmersionLine("はい、試着室はあちらです。", "はい、しつしつちはあちらです。", "Yes, the fitting room is over there.", listOf("試着室")),
            ImmersionLine("色違いの紺色もありますよ。", "いろちがいのこんいろもありますよ。", "We also have it in navy, a different color.", listOf("色", "紺"))
        )
    ),
    MediaTrack(
        id = "m18",
        title = "旅行の計画 · Travel Planning",
        subtitle = "Discussing a trip to Hokkaido with a friend",
        category = MediaCategory.Dialogues,
        durationSeconds = 46,
        level = "N4",
        tags = listOf("Travel", "Hokkaido", "Planning"),
        lines = listOf(
            ImmersionLine("夏休みに北海道に行かない？", "なつやすみにほっかいどうにいかない？", "Want to go to Hokkaido during summer vacation?", listOf("夏", "休", "北", "海", "道")),
            ImmersionLine("いいね！何をしたい？", "いいね！なにをしたい？", "Sounds great! What do you want to do?", listOf("何")),
            ImmersionLine("富良野のラベンダー畑を見たいし、海も泳ぎたい。", "ふらののラベンダーばたいをみたいしうみもおよぎたい。", "I want to see the lavender fields in Furano and swim in the ocean.", listOf("海", "泳")),
            ImmersionLine("新千歳空港からレンタカーで回ろうよ。", "しんちとせくうこうかられんたかーでまわろうよ。", "Let's drive around in a rental car from New Chitose Airport.", listOf("空港", "車", "回"))
        )
    ),
    MediaTrack(
        id = "m19",
        title = "電話応対 · Phone Manners",
        subtitle = "Professional phone conversation at a company",
        category = MediaCategory.Dialogues,
        durationSeconds = 45,
        level = "N4",
        tags = listOf("Business", "Phone", "Formal"),
        lines = listOf(
            ImmersionLine("はい、株式会社山田でございます。", "はい、かぶしきがいしゃやまだでございます。", "Yes, this is Yamada Corporation.", listOf("株式", "会社")),
            ImmersionLine("おかけになる番号は間違いないでしょうか？", "おかけになるごうろはまちがいないでしょうか？", "Are you sure you have the right number?", listOf("番号", "間違")),
            ImmersionLine("少々お待ちいただけますか？", "しょうしょうおまちいただけますか？", "Could you hold for a moment?", listOf("少", "待")),
            ImmersionLine("お待たせいたしました。ただいま転送いたします。", "おまたせいたしました。ただいまてんそういたします。", "Thank you for waiting. I will transfer you now.", listOf("待", "転送"))
        )
    ),
    MediaTrack(
        id = "m20",
        title = "レストラン · Restaurant Order",
        subtitle = "Ordering a full course meal in Japanese",
        category = MediaCategory.Dialogues,
        durationSeconds = 50,
        level = "N4",
        tags = listOf("Restaurant", "Food", "Ordering"),
        lines = listOf(
            ImmersionLine("お飲み物は何になさいますか？", "おのみものはなになさいますか？", "What would you like to drink?", listOf("飲", "物")),
            ImmersionLine("とりあえず、生ビールを二つお願いします。", "とりあえず、なまビールをふたつおねがいします。", "First, two draft beers please.", listOf("生", "二")),
            ImmersionLine("日替わりランチセットをお願いします。", "ひがわりらんちせっとをおねがいします。", "I'll have the daily lunch set.", listOf("日", "替", "ランチ")),
            ImmersionLine("お会計は別々にできますか？", "おかいけいはべつべつにできますか？", "Can we split the bill separately?", listOf("会計", "別"))
        )
    ),
    MediaTrack(
        id = "m21",
        title = "銀行 · Bank Visit",
        subtitle = "Opening an account at a Japanese bank",
        category = MediaCategory.Dialogues,
        durationSeconds = 42,
        level = "N4",
        tags = listOf("Bank", "Finance", "Formal"),
        lines = listOf(
            ImmersionLine("口座を開設したいのですが。", "こうざをかいせつしたいのですが。", "I'd like to open an account.", listOf("口座", "開設")),
            ImmersionLine("身分証明書はお持ちですか？", "みぶんしょうめいしょはおもちですか？", "Do you have your ID?", listOf("身分", "証明")),
            ImmersionLine("こちらの用紙にご記入ください。", "こちらのようしにごきにゅうください。", "Please fill out this form.", listOf("用紙", "記入")),
            ImmersionLine("暗証番号は二桁から六桁で設定できます。", "あんしょうごうごうにかたからろくかたであってできます。", "You can set a PIN from 2 to 6 digits.", listOf("番号", "桁", "設定"))
        )
    ),

    // ── STORIES (8 tracks) ──
    MediaTrack(
        id = "m05",
        title = "昔話 · The Legend of Tsurugaoka",
        subtitle = "Historic folklore of the sacred Ginkgo tree",
        category = MediaCategory.Stories,
        durationSeconds = 76,
        level = "N3",
        tags = listOf("Folktale", "History", "Shrine"),
        lines = listOf(
            ImmersionLine("昔々、鎌、鎌倉の鶴岡八幡宮には大きな銀杏の木がありました。", "むかしむかし、かまくらのつるがおかはちまんぐうにはおおきなぎんなんのきがありました。", "Long ago, there was a magnificent Ginkgo tree at Tsurugaoka Shrine.", listOf("昔", "鎌倉", "鶴", "木", "銀杏")),
            ImmersionLine("八百年以上もの間、人々の祈りを見守り続けていました。", "はっぴゃくねんいじょうのあいだ、ひとびとのいのりをみまもりつづけていました。", "For over 800 years, it watched over the prayers of the people.", listOf("年", "間", "人", "祈", "続")),
            ImmersionLine("強い風で倒れた後も、新しい若芽が力強く育っています。", "つよいかぜでたおれたあとも、あたらしいわかめがちからづよくそだっています。", "Even after falling in a storm, vibrant new shoots continue to grow.", listOf("強", "風", "新", "力", "育")),
            ImmersionLine("今でもその木は鎌倉の象徴として愛されています。", "いまでもそのきはかまくらのしょうちょうとしてあいされています。", "Even now, the tree is loved as a symbol of Kamakura.", listOf("今", "象徴", "愛"))
        )
    ),
    MediaTrack(
        id = "m06",
        title = "桃太郎 · Momotaro",
        subtitle = "The classic tale of the peach boy",
        category = MediaCategory.Stories,
        durationSeconds = 90,
        level = "N3",
        tags = listOf("Folktale", "Classic", "Mythology"),
        lines = listOf(
            ImmersionLine("ある所に、おじいさんとおばあさんが住んでいました。", "あるところに、おじいさんとおばあさんがすんでいました。", "Once upon a time, there lived an old man and an old woman.", listOf("住")),
            ImmersionLine("おじいさんは山へ柴を刈りに、おばあさんは川へ洗濯に行きました。", "おじいさんはやまへしばをかりに、おばあさんはかわへせんたくにいきました。", "The old man went to the mountain for firewood, and the old woman to the river.", listOf("山", "川", "洗濯")),
            ImmersionLine("大きな桃が川を流れてきました。", "おおきなももがかわをながれてきました。", "A giant peach came floating down the river.", listOf("桃", "流")),
            ImmersionLine("おじいさんとおばあさんは桃を割ると、中から元気な男の子が生まれました。", "おじいさんとおばあさんはももをわると、なかからげんきなおとこのこがうまれました。", "When the old couple split open the peach, a healthy boy was born.", listOf("割", "元気", "生"))
        )
    ),
    MediaTrack(
        id = "m07",
        title = "花鳥風月 · Seasons of Japan",
        subtitle = "A poetic meditation on the four seasons",
        category = MediaCategory.Stories,
        durationSeconds = 65,
        level = "N3",
        tags = listOf("Poetry", "Nature", "Seasons"),
        lines = listOf(
            ImmersionLine("春はあけぼの。やうやう白くなりゆく山際。", "はるはあけぼの。やうやうしろくなりゆくやまぎわ。", "Spring is dawn. The mountain ridge gradually turns white.", listOf("春", "白", "山")),
            ImmersionLine("夏は夜。月のころはさらなり。", "なつはよ。つきのころはさらなり。", "Summer is night. The moonlit nights are especially beautiful.", listOf("夏", "月", "夜")),
            ImmersionLine("秋は夕暮れ。夕日の差して山の端いと近うなりたるに。", "あきはゆうぐれ。ゆうひのさしてやまのはいとちかうなりたるに。", "Autumn is dusk. When the setting sun draws close to the mountain's edge...", listOf("秋", "夕", "日", "端")),
            ImmersionLine("冬はつとめて。雪の降りたるは言ふべきにもあらず。", "ふゆはつとめて。ゆきのふりたるはいうべきにもあらず。", "Winter is early morning. Needless to say, it is beautiful when snow has fallen.", listOf("冬", "雪", "降"))
        )
    ),
    MediaTrack(
        id = "m22",
        title = "浦島太郎 · Urashima Taro",
        subtitle = "The fisherman who visited the Dragon Palace",
        category = MediaCategory.Stories,
        durationSeconds = 80,
        level = "N3",
        tags = listOf("Folktale", "Ocean", "Classic"),
        lines = listOf(
            ImmersionLine("很久以前，有一个叫浦岛太郎的渔夫。", "", "", emptyList()),
            ImmersionLine("浦島太郎は善良な漁夫でした。", "うらしまたろうはぜんりょうなぎょふでした。", "Urashima Taro was a kind fisherman.", listOf("善良", "漁夫")),
            ImmersionLine("ある日、子供たちにいじめられている亀を助きました。", "あるひ、こどもたちにいじめられているかめをすくいました。", "One day, he saved a turtle that children were bullying.", listOf("亀", "助")),
            ImmersionLine("亀は恩返しに海底の竜宮城へ彼を案内しました。", "かめはおんがえしにかいてきのりゅうぐうじょうへかれをあんないしました。", "The turtle took him to the Dragon Palace under the sea as thanks.", listOf("恩", "海", "竜", "城")),
            ImmersionLine("太郎は三日間楽しみましたが、帰ると百年が過ぎていました。", "たろうはみっかげんたのしみましたが、かえるとはくねんがすぎていました。", "Taro enjoyed himself for three days, but a hundred years had passed when he returned.", listOf("三", "日", "年", "過"))
        )
    ),
    MediaTrack(
        id = "m23",
        title = "かぐや姫 · Kaguya-hime",
        subtitle = "The tale of the bamboo princess",
        category = MediaCategory.Stories,
        durationSeconds = 85,
        level = "N3",
        tags = listOf("Folktale", "Moon", "Classic"),
        lines = listOf(
            ImmersionLine("竹取物語は日本最古の物語です。", "たけとりものがたりはにほんさいこのものがたりです。", "The Tale of the Bamboo Cutter is the oldest story in Japan.", listOf("竹", "物語", "古")),
            ImmersionLine("かぐや姫は竹の中にいて、輝かしい光を放っていました。", "かぐやひめはたけのなかにいて、かがやかしいひかりをはなってました。", "Kaguya-hime was inside the bamboo, radiating brilliant light.", listOf("姫", "竹", "光", "輝")),
            ImmersionLine("五人の王子が彼女に求婚しましたが、誰も成功しませんでした。", "ごにんのおうじがかじょにこんしんしましたが、だれもせいこうしませんでした。", "Five princes proposed to her, but none succeeded.", listOf("王", "求婚", "成功")),
            ImmersionLine("最終的に、彼女は月に帰らなければなりませんでした。", "さいしゅうてきに、かじょはつきにかえらなければなりませんでした。", "In the end, she had to return to the moon.", listOf("月", "帰"))
        )
    ),
    MediaTrack(
        id = "m24",
        title = "禅と庭 · Zen & Gardens",
        subtitle = "Philosophy behind Japanese garden design",
        category = MediaCategory.Stories,
        durationSeconds = 70,
        level = "N3",
        tags = listOf("Zen", "Garden", "Philosophy"),
        lines = listOf(
            ImmersionLine("日本の庭園は自然の美しさを表しています。", "にほんていえんはしぜんのうつくしさをあらわしています。", "Japanese gardens express the beauty of nature.", listOf("庭園", "自然", "美")),
            ImmersionLine("枯山水は水を使わない庭園の様式です。", "かれさんすいはみずをつかわないていえんのようしきです。", "Karesansui is a garden style that uses no water.", listOf("枯山水", "水", "庭園")),
            ImmersionLine("石、砂、苔を組み合わせて宇宙を表現します。", "いし、すな、こけをくみわわせてうちゅうをひょうげんします。", "Stones, sand, and moss are combined to represent the universe.", listOf("石", "砂", "宇宙", "表現")),
            ImmersionLine("坐禅と庭園は心の平静を育む手助けとなります。", "ざぜんとていえんはしんのへいせいをそだむてだすけとなります。", "Zazen and garden cultivation help nurture peace of mind.", listOf("坐禅", "心", "平静"))
        )
    ),
    MediaTrack(
        id = "m25",
        title = "武士道 · Bushido",
        subtitle = "The way of the warrior — codes of honor",
        category = MediaCategory.Stories,
        durationSeconds = 75,
        level = "N2",
        tags = listOf("History", "Samurai", "Philosophy"),
        lines = listOf(
            ImmersionLine("武士道は日本の武士の道徳体系です。", "ぶしどうはにほんのぶしのどうとくたいけいです。", "Bushido is the moral code of the Japanese warrior.", listOf("武士", "道徳", "体系")),
            ImmersionLine("義、勇、仁、礼、誠、名誉、忠義が七つの徳とされています。", "ぎ、ゆう、じん、れい、せい、めいよ、ちゅうぎがななつのとくとされています。", "Justice, courage, benevolence, respect, honesty, honor, and loyalty are the seven virtues.", listOf("義", "勇", "仁", "礼", "忠義")),
            ImmersionLine("武士は死を恐れない精神を持つことが求められました。", "ぶしはしをおそれないせいしんをもつことがもとめられました。", "Samurai were required to have a spirit unafraid of death.", listOf("死", "恐", "精神")),
            ImmersionLine("現在でも武士道の精神は日本の文化に残っています。", "げんざいでもぶしどうのせいしんはにほんのぶんかにのこっています。", "Even today, the spirit of Bushido remains in Japanese culture.", listOf("今", "文化", "残"))
        )
    ),
    MediaTrack(
        id = "m26",
        title = "茶道 · Tea Ceremony",
        subtitle = "The art and philosophy of chanoyu",
        category = MediaCategory.Stories,
        durationSeconds = 68,
        level = "N3",
        tags = listOf("Tea", "Culture", "Zen"),
        lines = listOf(
            ImmersionLine("茶道は単にお茶を飲むことではありません。", "さどうはたんにおちゃをのむことではありません。", "The tea ceremony is not merely drinking tea.", listOf("茶道", "茶")),
            ImmersionLine("一期一会を大切にする精神が根底にあります。", "いちごいちえをたいせつにするせいしんがこんていにあります。", "The spirit of treasuring each encounter is at its foundation.", listOf("一", "会", "大切", "精神")),
            ImmersionLine("主人と客人が心を通わせる場となります。", "しゅじんときゃくじんがこころをとおわせるばとなります。", "It becomes a place where host and guest connect their hearts.", listOf("主", "客", "心")),
            ImmersionLine("抹茶を点てる動作にも深い意味が込められています。", "まっちゃをたてるどうさにもふかいいみがこめられています。", "Even the motions of whisking matcha hold deep meaning.", listOf("茶", "動作", "深", "意"))
        )
    ),

    // ── SENTENCES (14 tracks) ──
    MediaTrack(
        id = "m08",
        title = "自然の音 · Nature & Seasons",
        subtitle = "Autumn breeze and ocean waves of Sagami Bay",
        category = MediaCategory.Sentences,
        durationSeconds = 32,
        level = "N4",
        tags = listOf("Nature", "Ocean", "Autumn"),
        lines = listOf(
            ImmersionLine("秋の風が心地よく吹き、海の波が静かに寄せています。", "あきのかぜがここちよくふき、うみのなみがしずかによせています。", "The autumn breeze blows pleasantly as gentle waves roll onto the shore.", listOf("秋", "風", "海", "波", "静")),
            ImmersionLine("夕暮れ時の空が茜色に美しく染まっています。", "ゆうぐれときのそらがあかねいろにうつくしくそまっています。", "The evening sky is dyed in a beautiful crimson shade.", listOf("夕", "空", "色", "美"))
        )
    ),
    MediaTrack(
        id = "m09",
        title = "天気予報 · Weather Forecast",
        subtitle = "NHK weather report for the Kantō region",
        category = MediaCategory.Sentences,
        durationSeconds = 38,
        level = "N4",
        tags = listOf("Weather", "NHK", "Kanto"),
        lines = listOf(
            ImmersionLine("今日の天気です。関東地方は晴れのち曇ります。", "きょうのてんきです。かんとうちはほうははれのちくもります。", "Today's weather. The Kanto region will be sunny, then cloudy.", listOf("天気", "関東", "晴", "曇")),
            ImmersionLine("午後から雨が降る可能性があります。", "ごごからあめがふるかのうせいがあります。", "There is a chance of rain from the afternoon.", listOf("午後", "雨", "降", "可能")),
            ImmersionLine("気温は最高二十五度、最低十八度です。", "きおんはさいこうにじゅうごど、さいていはちじゅうはちどです。", "The high will be 25 degrees, the low 18 degrees.", listOf("気温", "最高", "最低"))
        )
    ),
    MediaTrack(
        id = "m10",
        title = "散歩道 · Walking in Kyoto",
        subtitle = "Describing a morning walk through Higashiyama",
        category = MediaCategory.Sentences,
        durationSeconds = 48,
        level = "N3",
        tags = listOf("Kyoto", "Walking", "Scenery"),
        lines = listOf(
            ImmersionLine("朝の光が石畳の道を照らしていました。", "あさのひかりがいしだたみのみちをてらしていました。", "The morning light was illuminating the stone-paved path.", listOf("朝", "光", "石", "道")),
            ImmersionLine("古いお寺の門をくぐると、静かな庭が広がっていた。", "ふるいおさどのもんをくぐると、しずかなにわがひろがっていた。", "Passing through the old temple gate, a tranquil garden spread out.", listOf("古", "門", "庭", "広")),
            ImmersionLine("茶屋でお抹茶を飲みながら景色を楽しみました。", "ちゃやでまっちゃをのみながらけしきをたのしみました。", "We enjoyed the scenery while drinking matcha at a teahouse.", listOf("茶", "景色", "楽"))
        )
    ),
    MediaTrack(
        id = "m11",
        title = "ポッドキャスト · Podcast Intro",
        subtitle = "Japanese learning podcast — episode greeting",
        category = MediaCategory.Sentences,
        durationSeconds = 30,
        level = "N5",
        tags = listOf("Podcast", "Greeting", "Casual"),
        lines = listOf(
            ImmersionLine("みなさん、こんにちは！日本語の勉強、お疲れ様です。", "みなさん、こんにちは！にほんごのべんきょう、おつかれさまです。", "Hello everyone! Great job studying Japanese.", listOf("勉強", "疲")),
            ImmersionLine("今日のテーマは季節の言葉です。", "きょうのてまはきせつのことばです。", "Today's theme is seasonal vocabulary.", listOf("今", "季節", "言葉")),
            ImmersionLine("一緒に勉強しましょう！", "いっしょにべんきょうしましょう！", "Let's study together!", listOf("一", "勉強"))
        )
    ),
    MediaTrack(
        id = "m12",
        title = "アニメのセリフ · Anime Dialogue",
        subtitle = "Greeting scene from a slice-of-life anime",
        category = MediaCategory.Sentences,
        durationSeconds = 25,
        level = "N5",
        tags = listOf("Anime", "Greeting", "Casual"),
        lines = listOf(
            ImmersionLine("おはよう！今日も一日頑張ろうね！", "おはよう！きょうもいちにちがんばろうね！", "Good morning! Let's do our best today too!", listOf("一", "日", "頑張")),
            ImmersionLine("あ、待って！傘忘れてるよ！", "あ、まって！かさわすれてるよ！", "Oh, wait! You forgot your umbrella!", listOf("待", "傘", "忘")),
            ImmersionLine("ありがとう！今日の授業、楽しみだね。", "ありがとう！きょうのじゅぎょう、たのしみだね。", "Thanks! I'm looking forward to today's class.", listOf("楽", "授業"))
        )
    ),
    MediaTrack(
        id = "m13",
        title = "ニュース · News Headline",
        subtitle = "NHK news bulletin about a local festival",
        category = MediaCategory.Sentences,
        durationSeconds = 42,
        level = "N3",
        tags = listOf("News", "NHK", "Festival"),
        lines = listOf(
            ImmersionLine("きょうのニュースをお伝えします。", "きょうのニュースをおつたえします。", "Here is today's news.", listOf("今", "伝")),
            ImmersionLine("京都の祇園祭りが今日から始まりました。", "きょうとのぎおんまつりがきょうからはじまりました。", "Kyoto's Gion Matsuri festival has begun today.", listOf("京", "都", "祭", "始")),
            ImmersionLine("国内外から百万人以上の観光客が訪れる見込みです。", "こくないがいからひゃくまんにんいじょうのこうんきゃくがおとずれるみこみです。", "Over one million visitors from Japan and abroad are expected.", listOf("万", "人", "観光", "客")),
            ImmersionLine("自治体は混雑緩和のため臨時バスを増やす予定です。", "じちたいはこんざつかんわのためりんじばすをふやすよていです。", "The local government plans to add temporary buses to ease congestion.", listOf("自治体", "混雑", "臨時", "予定"))
        )
    ),
    MediaTrack(
        id = "m27",
        title = "道案内 · Giving Directions",
        subtitle = "Asking for and giving directions in Tokyo",
        category = MediaCategory.Sentences,
        durationSeconds = 38,
        level = "N4",
        tags = listOf("Directions", "Tokyo", "Practical"),
        lines = listOf(
            ImmersionLine("すみません、東京駅はどこですか？", "すみません、とうきょうえきはどこですか？", "Excuse me, where is Tokyo Station?", listOf("東京", "駅")),
            ImmersionLine("この道をまっすぐ行って、二つ目の信号を右に曲がってください。", "このみちをまっすぐいって、ふたつめのしんごうをみぎにまがってください。", "Go straight on this road, then turn right at the second traffic light.", listOf("道", "信号", "右", "曲")),
            ImmersionLine("銀行の隣にコンビニがあります。", "ぎんこうのとなりにこんびにがあります。", "There's a convenience store next to the bank.", listOf("銀行", "隣", "コンビニ")),
            ImmersionLine("歩いてどのくらいかかりますか？", "あるいてどのくらいかかりますか？", "How long does it take on foot?", listOf("歩", "時間"))
        )
    ),
    MediaTrack(
        id = "m28",
        title = "学校の日常 · School Life",
        subtitle = "Students chatting during lunch break",
        category = MediaCategory.Sentences,
        durationSeconds = 35,
        level = "N4",
        tags = listOf("School", "Casual", "Daily"),
        lines = listOf(
            ImmersionLine("今日の給食、何が好き？", "きょうのきゅうしょく、なにがすき？", "What do you like about today's school lunch?", listOf("今", "給食", "好")),
            ImmersionLine("カレーが一番好きだよ！", "カレーがいちばんすきだよ！", "I like curry the most!", listOf("一", "番", "好")),
            ImmersionLine("午後の授業は何ですか？", "ごごのじゅぎょうはなんですか？", "What's the afternoon class?", listOf("午後", "授業")),
            ImmersionLine("体育です。遊ぼう！", "たいいくです。あそぼう！", "It's PE. Let's play!", listOf("体育", "遊"))
        )
    ),
    MediaTrack(
        id = "m29",
        title = "料理の手順 · Following a Recipe",
        subtitle = "Making tamagoyaki step by step",
        category = MediaCategory.Sentences,
        durationSeconds = 45,
        level = "N4",
        tags = listOf("Cooking", "Food", "Step-by-step"),
        lines = listOf(
            ImmersionLine("まず、卵を三個ボウルに入れます。", "まず、たまごをさんこぼうるにいれます。", "First, crack three eggs into a bowl.", listOf("卵", "三", "入")),
            ImmersionLine("醤油と砂糖を小さじ一杯ずつ加えます。", "しょうゆとさとうをこしゃじいっぱいずつくわえます。", "Add one teaspoon each of soy sauce and sugar.", listOf("醤油", "砂糖", "一")),
            ImmersionLine("中火で卵液を焼いていきます。", "なかびでたまごえきをやいていきます。", "Cook the egg mixture over medium heat.", listOf("火", "焼")),
            ImmersionLine("金目棒で巻きながら少しずつ巻いていきます。", "かなめぼうでまきながらすこしずつまいていきます。", "Roll it up gradually using a bamboo stick.", listOf("巻"))
        )
    ),
    MediaTrack(
        id = "m30",
        title = "電車の旅 · Train Journey",
        subtitle = "A scenic ride along the coast to Kamakura",
        category = MediaCategory.Sentences,
        durationSeconds = 40,
        level = "N4",
        tags = listOf("Train", "Travel", "Scenery"),
        lines = listOf(
            ImmersionLine("車窓から海が見えます。", "しゃまどうからうみがみえます。", "You can see the ocean from the train window.", listOf("車", "窓", "海")),
            ImmersionLine("次の停車駅は逗子です。", "つぎのていしゃえきはざまです。", "The next stop is Zushi.", listOf("次", "停車", "駅")),
            ImmersionLine("この線路は海沿いに続いています。", "このせんろはうみぞいにつづいています。", "This railway continues along the coast.", listOf("線路", "海", "続")),
            ImmersionLine("切符は車内で買えます。", "きっぷはしゃないでかえます。", "You can buy a ticket on the train.", listOf("切符", "車内", "買"))
        )
    ),
    MediaTrack(
        id = "m31",
        title = "季節の挨拶 · Seasonal Greetings",
        subtitle = "Formal seasonal phrases used in daily life",
        category = MediaCategory.Sentences,
        durationSeconds = 30,
        level = "N4",
        tags = listOf("Greetings", "Seasons", "Formal"),
        lines = listOf(
            ImmersionLine("暑中お見舞い申し上げます。", "しゅうちゅうおみまいもうしあげます。", "Sending you my warmest summer greetings.", listOf("暑", "見舞")),
            ImmersionLine("寒さが厳しくなってきましたね。", "さむさがきびしくなってきましたね。", "The cold has gotten severe, hasn't it?", listOf("寒", "厳")),
            ImmersionLine("桜の季節が近づいてきました。", "さくらのきせつがちかづいてきました。", "The cherry blossom season is approaching.", listOf("桜", "季節", "近")),
            ImmersionLine("紅葉がとても綺麗ですね。", "こうようがとてもきれいですね。", "The autumn leaves are beautiful, aren't they?", listOf("紅葉", "綺麗"))
        )
    ),
    MediaTrack(
        id = "m32",
        title = "デパートのフロア · Department Store",
        subtitle = "Navigating floors at a big Tokyo store",
        category = MediaCategory.Sentences,
        durationSeconds = 36,
        level = "N4",
        tags = listOf("Shopping", "Department Store", "Navigation"),
        lines = listOf(
            ImmersionLine("エスカレーターはあちらです。", "エスカレーターはあちらです。", "The escalator is over there.", listOf("エスカレーター")),
            ImmersionLine("食品フロアは地下一階にあります。", "しょくひんフロアはちかいっかいにあります。", "The food floor is on basement level one.", listOf("食品", "地下", "階")),
            ImmersionLine("トイレはどこにありますか？", "トイレはどこにありますか？", "Where is the restroom?", listOf("トイレ", "何処")),
            ImmersionLine("三階に cocktails デスクがあります。", "さんがいにきゃくせきデスクがあります。", "There is a customer service desk on the third floor.", listOf("階", "受付"))
        )
    ),
    MediaTrack(
        id = "m33",
        title = "天気の表現 · Weather Expressions",
        subtitle = "Common weather-related vocabulary in context",
        category = MediaCategory.Sentences,
        durationSeconds = 34,
        level = "N5",
        tags = listOf("Weather", "Vocabulary", "Basics"),
        lines = listOf(
            ImmersionLine("今日はいい天気ですね。", "きょうはいいてんきですね。", "It's nice weather today.", listOf("天気")),
            ImmersionLine("明日は雨が降るでしょう。", "あしたはあめがふるでしょう。", "It will probably rain tomorrow.", listOf("雨", "降")),
            ImmersionLine("昨日は風が強かったです。", "きのうはかぜがつよかったです。", "The wind was strong yesterday.", listOf("風", "強")),
            ImmersionLine("来週は雪が降るかもしれません。", "らいしゅうはゆきがふるかもしれません。", "It might snow next week.", listOf("雪", "降"))
        )
    ),
    MediaTrack(
        id = "m34",
        title = "友達との会話 · Chatting with Friends",
        subtitle = "Casual weekend plans discussion",
        category = MediaCategory.Sentences,
        durationSeconds = 32,
        level = "N5",
        tags = listOf("Casual", "Friends", "Weekend"),
        lines = listOf(
            ImmersionLine("今週の土曜日、暇？", "こんしゅうのどようび、ひま？", "Are you free this Saturday?", listOf("今", "週", "土曜", "暇")),
            ImmersionLine("映画を見に行かない？", "えいがをみにいかない？", "Want to go see a movie?", listOf("映画", "見", "行")),
            ImmersionLine("何が見たい？", "なにがみたい？", "What do you want to see?", listOf("何", "見")),
            ImmersionLine("アクション映画が見たいよ！", "アクションえいががみたいよ！", "I want to see an action movie!", listOf("映画", "見"))
        )
    ),

    // ── LISTENING PRACTICE (8 tracks) ──
    MediaTrack(
        id = "m14",
        title = "数字の聞き取り · Number Dictation",
        subtitle = "Listening to prices and quantities at a market",
        category = MediaCategory.Sentences,
        durationSeconds = 30,
        level = "N5",
        tags = listOf("Numbers", "Market", "Listening"),
        lines = listOf(
            ImmersionLine("これは五百円です。", "これはごひゃくえんです。", "This is 500 yen.", listOf("五", "百", "円")),
            ImmersionLine("三千八百円をお願いします。", "さんぜんはっぴゃくえんをおねがいします。", "That will be 3,800 yen.", listOf("三", "千", "八", "百")),
            ImmersionLine("お釣りは二百二十円です。", "おつりはにひゃくにじゅうえんです。", "Your change is 220 yen.", listOf("二", "百", "二十")),
            ImmersionLine("全部で一万二千三百円になります。", "ぜんぶでいちまんにせんさんびゃくえんになります。", "The total comes to 12,300 yen.", listOf("一", "万", "全部"))
        )
    ),
    MediaTrack(
        id = "m15",
        title = "時間の表現 · Telling Time",
        subtitle = "Practice telling time in natural conversation",
        category = MediaCategory.Sentences,
        durationSeconds = 28,
        level = "N5",
        tags = listOf("Time", "Conversation", "Basics"),
        lines = listOf(
            ImmersionLine("今何時ですか？", "いまなんじですか？", "What time is it now?", listOf("今", "何時")),
            ImmersionLine("三時半に会いましょう。", "さんじはんにあいましょう。", "Let's meet at 3:30.", listOf("三", "時", "半", "会")),
            ImmersionLine("もう遅いので帰ります。", "おそかいのでかえります。", "It's late so I'll go home.", listOf("遅", "帰")),
            ImmersionLine("明日は朝八時に起きます。", "あしたはあさはちじにおきます。", "Tomorrow I'll wake up at 8 AM.", listOf("明", "朝", "八", "時", "起"))
        )
    ),
    MediaTrack(
        id = "m35",
        title = "丁寧な依頼 · Polite Requests",
        subtitle = "Asking favors with keigo expressions",
        category = MediaCategory.Sentences,
        durationSeconds = 33,
        level = "N4",
        tags = listOf("Keigo", "Requests", "Formal"),
        lines = listOf(
            ImmersionLine("申し訳ありませんが、もう一度お願いできますか？", "もうしわけありませんが、もういちどおねがいできますか？", "I'm sorry, but could you do it one more time?", listOf("申", "訳", "一", "願")),
            ImmersionLine("お忙しいところ恐れ入りますが、ご確認お願いします。", "いそがしいところおそれいりますが、かくにんおねがいします。", "I know you're busy, but please review this.", listOf("忙", "恐", "確認")),
            ImmersionLine("ご連絡ありがとうございます。", "ごれんらくありがとうございます。", "Thank you for your response.", listOf("連絡", "感謝")),
            ImmersionLine("来週中にお返事いただけると幸いです。", "らいしゅうちゅうにおへんしいただけるとさいわいです。", "I would appreciate a response by next week.", listOf("来", "週", "返事", "幸"))
        )
    ),
    MediaTrack(
        id = "m36",
        title = "忙しい日常 · Busy Daily Life",
        subtitle = "A day in the life of a Tokyo office worker",
        category = MediaCategory.Sentences,
        durationSeconds = 40,
        level = "N4",
        tags = listOf("Daily", "Work", "Tokyo"),
        lines = listOf(
            ImmersionLine("毎朝七時に起きて、八時に電車に乗ります。", "まいあさしちじにおきて、はちじにでんしゃのにります。", "I wake up at 7 every morning and catch the 8 o'clock train.", listOf("毎", "朝", "七", "時", "起", "電車")),
            ImmersionLine("満員電車は大変ですが、慣れました。", "まんいんでんしゃはたいへんですかれなれました。", "The crowded train is tough, but I'm used to it.", listOf("満員", "大変", "慣")),
            ImmersionLine("昼休みに近くの公園で散歩します。", "ひるやすみにちかくのこうえんてさんぽします。", "I take a walk in the nearby park during lunch break.", listOf("昼", "休", "近", "公園", "散歩")),
            ImmersionLine("夜はよくコンビニで晚御飯を買います。", "よるはよくこんびにてばんごはんをかいます。", "I often buy dinner at the convenience store at night.", listOf("夜", "コンビニ", "晚", "飯", "買"))
        )
    ),
    MediaTrack(
        id = "m37",
        title = "约束の確認 · Confirming Plans",
        subtitle = "Making and confirming weekend plans",
        category = MediaCategory.Sentences,
        durationSeconds = 30,
        level = "N5",
        tags = listOf("Plans", "Casual", "Weekend"),
        lines = listOf(
            ImmersionLine("来週の金曜日、一緒に食事しない？", "らいしゅうのきんようび、いっしょにしょくじしない？", "Want to have dinner together next Friday?", listOf("来", "週", "金曜", "一", "食事")),
            ImmersionLine("何時からがいい？", "なんじからがいい？", "What time works best?", listOf("何時")),
            ImmersionLine("六時半はどう？", "ろくじはんはどう？", "How about 6:30?", listOf("六", "時", "半")),
            ImmersionLine("okyoudai shimashou！場所は後で連絡します。", "", "", emptyList())
        )
    ),
    MediaTrack(
        id = "m38",
        title = "おもてなし · Hospitality",
        subtitle = "Japanese hospitality phrases at a ryokan",
        category = MediaCategory.Sentences,
        durationSeconds = 38,
        level = "N4",
        tags = listOf("Hospitality", "Ryokan", "Formal"),
        lines = listOf(
            ImmersionLine("ようこそお越しくださいまして、ありがとうございます。", "ようこそおこしくださいまして、ありがとうございます。", "Thank you very much for visiting us.", listOf("来", "感謝")),
            ImmersionLine("部屋は二階の東向きです。", "へやはにかいのひがしまるきです。", "Your room is on the second floor, east-facing.", listOf("部屋", "階", "東")),
            ImmersionLine("夕食は七時に食堂でお楽しみいただけます。", "ゆうしょくはしちじにしょくどうでおたのしみいただけます。", "Dinner is available at the dining hall at seven.", listOf("夕食", "七", "時", "食堂", "楽")),
            ImmersionLine("温泉は二十四時間利用できます。", "おんせんはじにじゅうよじかんりようできます。", "The hot spring is available 24 hours.", listOf("温泉", "時間", "利用"))
        )
    ),
    MediaTrack(
        id = "m39",
        title = "感情の表現 · Expressing Emotions",
        subtitle = "Common emotional expressions in context",
        category = MediaCategory.Sentences,
        durationSeconds = 32,
        level = "N4",
        tags = listOf("Emotions", "Expression", "Daily"),
        lines = listOf(
            ImmersionLine("本当に嬉しいです！", "ほんとうにうれしいです！", "I'm truly happy!", listOf("本", "嬉")),
            ImmersionLine("残念ですが、行けません。", "ざんねんですが、いけません。", "I'm sorry, but I can't go.", listOf("残念", "行")),
            ImmersionLine("心配しないでください。", "しんぱいしないでください。", "Please don't worry.", listOf("心配")),
            ImmersionLine("頑張ってくださいね。応援しています。", "がんばってくださいね。おうえんしています。", "Please do your best. I'm cheering for you.", listOf("頑張", "応援"))
        )
    ),
    MediaTrack(
        id = "m40",
        title = " Mannen no Kinen · Millennium Temple",
        subtitle = "A poetic description of ancient Kyoto temples",
        category = MediaCategory.Sentences,
        durationSeconds = 44,
        level = "N3",
        tags = listOf("Kyoto", "Temple", "Poetic"),
        lines = listOf(
            ImmersionLine("千年的時を超えて、古都は今も静かに佇んでいます。", "せんねんのときをこえて、ことはいまもしずかにたたずんでいます。", "Spanning a thousand years, the ancient capital still stands quietly.", listOf("千", "年", "時", "古", "都", "静")),
            ImmersionLine("庭に散る紅葉が、風に揺れる Crimson leaves。", "", "", emptyList()),
            ImmersionLine("京都の寺は語り部のように、昔の物語を伝えています。", "きょうとのてらはかたりべのように、むかしのものがたりをつたえています。", "Kyoto's temples are like storytellers, passing on old tales.", listOf("寺", "語", "昔", "物語", "伝")),
            ImmersionLine("僧侶の唱える経の音色が、心に響きます。", "そうりょのとなえるきょうのねいろが、こころにひびきます。", "The sound of the monk's chant resonates in the heart.", listOf("僧", "経", "音", "心", "響"))
        )
    )
)

/** Multiplatform Media Centre Content interface */
fun interface MediaCentreContent {
    @Composable
    fun Content(navigationState: MainNavigationState?, onClose: () -> Unit)
}

/** Core Multiplatform Media Implementation — 4-tab layout */
object DefaultMediaCentreContent : MediaCentreContent {

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content(navigationState: MainNavigationState?, onClose: () -> Unit) {
        val surfaceColors = LocalSurfaceColors.current
        val accent = LocalKaiteyoAccent.current
        val scope = rememberCoroutineScope()
        val ttsManager = runCatching { koinInject<KanaTtsManager>() }.getOrNull()

        // ── State ──
        var selectedTab by remember { mutableStateOf(MediaCentreTab.Library) }
        var selectedCategory by remember { mutableStateOf(MediaCategory.All) }
        var sortMode by remember { mutableStateOf(SortMode.Default) }
        var searchQuery by remember { mutableStateOf("") }
        val tracks = remember { mutableStateListOf<MediaTrack>().apply { addAll(initialMediaLibrary) } }

        var activeTrack by remember { mutableStateOf<MediaTrack?>(tracks.firstOrNull()) }
        var isPlaying by remember { mutableStateOf(false) }
        var currentLineIndex by remember { mutableIntStateOf(0) }
        var playbackProgress by remember { mutableFloatStateOf(0f) }
        var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
        var repeatMode by remember { mutableStateOf(RepeatMode.Off) }
        var showImportDialog by remember { mutableStateOf(false) }

        // ── Settings state ──
        var autoAdvance by remember { mutableStateOf(true) }
        var showRomaji by remember { mutableStateOf(true) }
        var showEnglish by remember { mutableStateOf(true) }
        var highlightActive by remember { mutableStateOf(true) }

        // ── Daily immersion state ──
        var dailyGoalTracks by remember { mutableIntStateOf(3) }
        var dailyGoalMinutes by remember { mutableIntStateOf(15) }
        var dailyTracksPlayed by remember { mutableIntStateOf(0) }
        var dailyMinutesListened by remember { mutableFloatStateOf(0f) }
        var immersionDaysComplete by remember { mutableIntStateOf(0) }
        var showDailyMode by remember { mutableStateOf(false) }

        // ── Playback simulation with TTS ──
        LaunchedEffect(isPlaying, activeTrack, currentLineIndex, playbackSpeed) {
            if (!isPlaying || activeTrack == null) return@LaunchedEffect
            val line = activeTrack!!.lines.getOrNull(currentLineIndex) ?: return@LaunchedEffect
            if (ttsManager != null) {
                runCatching {
                    val romaji = line.reading.kanaToRomaji()
                    ttsManager.speak(KanaReading(nihonShiki = romaji))
                }
            }
            val lineDelay = (2500 / playbackSpeed).toLong()
            delay(lineDelay)
            if (!isPlaying) return@LaunchedEffect
            val totalLines = activeTrack?.lines?.size ?: return@LaunchedEffect
            if (currentLineIndex < totalLines - 1) {
                currentLineIndex++
                playbackProgress = (currentLineIndex.toFloat() + 1f) / totalLines.toFloat()
            } else {
                when (repeatMode) {
                    RepeatMode.One -> {
                        currentLineIndex = 0
                        playbackProgress = 0f
                    }
                    RepeatMode.All -> {
                        val idx = tracks.indexOfFirst { it.id == activeTrack?.id }
                        val nextIdx = if (idx >= 0 && idx < tracks.size - 1) idx + 1 else 0
                        activeTrack = tracks[nextIdx]
                        currentLineIndex = 0
                        playbackProgress = 0f
                    }
                    RepeatMode.Off -> {
                        if (autoAdvance) {
                            val idx = tracks.indexOfFirst { it.id == activeTrack?.id }
                            if (idx >= 0 && idx < tracks.size - 1) {
                                activeTrack = tracks[idx + 1]
                                currentLineIndex = 0
                                playbackProgress = 0f
                            } else {
                                isPlaying = false
                                currentLineIndex = 0
                                playbackProgress = 0f
                            }
                        } else {
                            isPlaying = false
                            currentLineIndex = 0
                            playbackProgress = 0f
                        }
                    }
                }
            }
        }

        // ── Stats ──
        val totalLinesPlayed = remember(tracks.size) { tracks.sumOf { it.lines.size } }
        val totalVocab = remember(tracks.size) { tracks.flatMap { it.lines }.flatMap { it.keywords }.distinct().size }

        // ── Filtered & sorted tracks ──
        val filteredTracks = remember(tracks, selectedCategory, searchQuery, sortMode) {
            tracks.filter { track ->
                val matchesCategory = when (selectedCategory) {
                    MediaCategory.All -> true
                    MediaCategory.Saved -> track.isBookmarked
                    else -> track.category == selectedCategory
                }
                val matchesQuery = searchQuery.isBlank() ||
                    track.title.contains(searchQuery, ignoreCase = true) ||
                    track.subtitle.contains(searchQuery, ignoreCase = true) ||
                    track.tags.any { it.contains(searchQuery, ignoreCase = true) } ||
                    track.lines.any { it.japanese.contains(searchQuery) || it.english.contains(searchQuery, ignoreCase = true) }
                matchesCategory && matchesQuery
            }.let { list ->
                when (sortMode) {
                    SortMode.Default -> list
                    SortMode.ByTitle -> list.sortedBy { it.title }
                    SortMode.ByLevel -> list.sortedBy { it.level }
                    SortMode.ByDuration -> list.sortedByDescending { it.durationSeconds }
                    SortMode.ByLines -> list.sortedByDescending { it.lines.size }
                }
            }
        }

        ProvidePageIdentity(
            PageIdentity(id = "media", name = "Media Centre", route = "/media", panel = selectedTab.label)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(surfaceColors.background)
            ) {
                // ════════════════════════════════════════
                // HEADER BAR
                // ════════════════════════════════════════
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = surfaceColors.textPrimary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Media Centre",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = surfaceColors.textPrimary
                        )
                        Text(
                            text = "${tracks.size} tracks · $totalLinesPlayed lines · $totalVocab vocab",
                            style = MaterialTheme.typography.bodySmall,
                            color = surfaceColors.textMuted
                        )
                    }
                    Button(
                        onClick = { showImportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = accent.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import", fontSize = 13.sp)
                    }
                }

                // ════════════════════════════════════════
                // TAB BAR
                // ════════════════════════════════════════
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MediaCentreTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        val tabBg by animateColorAsState(
                            if (selected) accent.primary.copy(alpha = 0.15f) else Color.Transparent
                        )
                        val tabColor = if (selected) accent.primary else surfaceColors.textSecondary

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = tabBg,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedTab = tab }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(tab.icon, contentDescription = null, tint = tabColor, modifier = Modifier.size(16.dp))
                                Text(tab.label, color = tabColor, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    // Daily mode button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (showDailyMode) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color.Transparent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showDailyMode = !showDailyMode }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Today, null, tint = if (showDailyMode) Color(0xFF4CAF50) else surfaceColors.textMuted, modifier = Modifier.size(16.dp))
                            Text("Daily", fontSize = 12.sp, color = if (showDailyMode) Color(0xFF4CAF50) else surfaceColors.textMuted, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // ════════════════════════════════════════
                // DAILY IMMERSION MODE (collapsible)
                // ════════════════════════════════════════
                AnimatedVisibility(
                    visible = showDailyMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    DailyImmersionBar(
                        goalTracks = dailyGoalTracks,
                        goalMinutes = dailyGoalMinutes,
                        tracksPlayed = dailyTracksPlayed,
                        minutesListened = dailyMinutesListened,
                        daysComplete = immersionDaysComplete,
                        accent = accent,
                        surfaceColors = surfaceColors,
                        onStartDaily = {
                            showDailyMode = false
                            selectedTab = MediaCentreTab.Player
                            dailyTracksPlayed = 0
                            dailyMinutesListened = 0f
                            // Start a random track to kick off daily immersion
                            val unplayed = tracks.shuffled()
                            if (unplayed.isNotEmpty()) {
                                activeTrack = unplayed.first()
                                currentLineIndex = 0
                                playbackProgress = 0f
                                isPlaying = true
                            }
                        },
                        onGoalChange = { t, m -> dailyGoalTracks = t; dailyGoalMinutes = m }
                    )
                }

                // ════════════════════════════════════════
                // MAIN CONTENT
                // ════════════════════════════════════════
                when (selectedTab) {
                    MediaCentreTab.Library -> {
                        // ── SEARCH ──
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search tracks, vocabulary, kanji...", fontSize = 13.sp, color = surfaceColors.textMuted) },
                                leadingIcon = { Icon(Icons.Default.Search, null, tint = surfaceColors.textMuted, modifier = Modifier.size(20.dp)) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            )
                            // Sort dropdown
                            var sortExpanded by remember { mutableStateOf(false) }
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = surfaceColors.surface,
                                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { sortExpanded = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.FilterList, null, tint = surfaceColors.textSecondary, modifier = Modifier.size(16.dp))
                                        Text(sortMode.label, fontSize = 12.sp, color = surfaceColors.textSecondary)
                                    }
                                }
                                DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                                    SortMode.entries.forEach { mode ->
                                        DropdownMenuItem(
                                            text = { Text(mode.label) },
                                            onClick = { sortMode = mode; sortExpanded = false },
                                            leadingIcon = if (mode == sortMode) {{ Icon(Icons.Default.Check, null, tint = accent.primary, modifier = Modifier.size(16.dp)) }} else null
                                        )
                                    }
                                }
                            }
                        }

                        // ── CATEGORIES ──
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MediaCategory.entries.forEach { category ->
                                val selected = selectedCategory == category
                                val count = when (category) {
                                    MediaCategory.All -> tracks.size
                                    MediaCategory.Saved -> tracks.count { it.isBookmarked }
                                    else -> tracks.count { it.category == category }
                                }
                                val catBg by animateColorAsState(if (selected) accent.primary.copy(alpha = 0.2f) else surfaceColors.surface)
                                val catColor = if (selected) accent.primary else surfaceColors.textSecondary

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = catBg,
                                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable { selectedCategory = category }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(category.icon, contentDescription = null, tint = catColor, modifier = Modifier.size(14.dp))
                                        Text(category.label, color = catColor, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                        Text("($count)", color = catColor.copy(alpha = 0.5f), fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        // ── TRACK LIST ──
                        if (filteredTracks.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                KaiteyoEmptyState(
                                    icon = "🎵",
                                    title = "No tracks found",
                                    message = if (searchQuery.isNotBlank()) "No tracks matched \"$searchQuery\"" else "Import your own Japanese audio tracks to immerse.",
                                    actionLabel = "Import Track",
                                    onAction = { showImportDialog = true }
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(filteredTracks, key = { it.id }) { track ->
                                    TrackCard(
                                        track = track,
                                        isActive = activeTrack?.id == track.id,
                                        isPlaying = isPlaying && activeTrack?.id == track.id,
                                        currentLineIndex = if (activeTrack?.id == track.id) currentLineIndex else -1,
                                        accent = accent,
                                        surfaceColors = surfaceColors,
                                        onSelect = {
                                            activeTrack = track
                                            currentLineIndex = 0
                                            playbackProgress = 0f
                                            selectedTab = MediaCentreTab.Player
                                        },
                                        onToggleBookmark = {
                                            val idx = tracks.indexOfFirst { it.id == track.id }
                                            if (idx >= 0) {
                                                tracks[idx] = track.copy(isBookmarked = !track.isBookmarked)
                                            }
                                        },
                                        onPlay = {
                                            activeTrack = track
                                            currentLineIndex = 0
                                            playbackProgress = 0f
                                            isPlaying = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    MediaCentreTab.Player -> {
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // ── LEFT: MINI TRACK LIST ──
                            LazyColumn(
                                modifier = Modifier.width(280.dp).fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(filteredTracks, key = { it.id }) { track ->
                                    MiniTrackRow(
                                        track = track,
                                        isActive = activeTrack?.id == track.id,
                                        accent = accent,
                                        surfaceColors = surfaceColors,
                                        onSelect = {
                                            activeTrack = track
                                            currentLineIndex = 0
                                            playbackProgress = 0f
                                        },
                                        onPlay = {
                                            activeTrack = track
                                            currentLineIndex = 0
                                            playbackProgress = 0f
                                            isPlaying = true
                                        }
                                    )
                                }
                            }
                            // ── RIGHT: TRANSCRIPT ──
                            activeTrack?.let { track ->
                                TranscriptPanel(
                                    track = track,
                                    currentLineIndex = currentLineIndex,
                                    isPlaying = isPlaying,
                                    showRomaji = showRomaji,
                                    showEnglish = showEnglish,
                                    accent = accent,
                                    surfaceColors = surfaceColors,
                                    ttsManager = ttsManager,
                                    modifier = Modifier.weight(1.1f),
                                    navigationState = navigationState,
                                    onLineClick = { index ->
                                        currentLineIndex = index
                                        playbackProgress = (index.toFloat() + 1f) / track.lines.size.toFloat()
                                        if (ttsManager != null) {
                                            scope.launch {
                                                runCatching {
                                                    val line = track.lines[index]
                                                    val romaji = line.reading.kanaToRomaji()
                                                    ttsManager.speak(KanaReading(nihonShiki = romaji))
                                                }
                                            }
                                        }
                                    }
                                )
                            } ?: Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                                KaiteyoEmptyState(icon = "🎧", title = "Select a track", message = "Choose a track from the list to start listening.")
                            }
                        }
                    }

                    MediaCentreTab.Stats -> {
                        StatsPanel(
                            tracks = tracks,
                            accent = accent,
                            surfaceColors = surfaceColors
                        )
                    }

                    MediaCentreTab.Settings -> {
                        SettingsPanel(
                            playbackSpeed = playbackSpeed,
                            onSpeedChange = { playbackSpeed = it },
                            repeatMode = repeatMode,
                            onRepeatChange = { repeatMode = it },
                            autoAdvance = autoAdvance,
                            onAutoAdvanceChange = { autoAdvance = it },
                            showRomaji = showRomaji,
                            onShowRomajiChange = { showRomaji = it },
                            showEnglish = showEnglish,
                            onShowEnglishChange = { showEnglish = it },
                            highlightActive = highlightActive,
                            onHighlightActiveChange = { highlightActive = it },
                            accent = accent,
                            surfaceColors = surfaceColors
                        )
                    }
                }

                // ════════════════════════════════════════
                // BOTTOM PLAYER BAR (always visible when a track is active)
                // ════════════════════════════════════════
                activeTrack?.let { track ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = surfaceColors.surface,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Progress slider
                            Slider(
                                value = playbackProgress,
                                onValueChange = { value ->
                                    playbackProgress = value
                                    val totalLines = track.lines.size.coerceAtLeast(1)
                                    currentLineIndex = (value * totalLines).toInt().coerceIn(0, totalLines - 1)
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = accent.primary,
                                    activeTrackColor = accent.primary,
                                    inactiveTrackColor = surfaceColors.surfaceInteractive
                                ),
                                modifier = Modifier.fillMaxWidth().height(16.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(track.title, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary, maxLines = 1, fontSize = 13.sp)
                                    Text(
                                        track.lines.getOrNull(currentLineIndex)?.japanese ?: track.subtitle,
                                        fontSize = 11.sp,
                                        color = accent.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    // Speed button
                                    TextButton(onClick = {
                                        playbackSpeed = when {
                                            playbackSpeed < 0.75f -> 0.5f
                                            playbackSpeed < 1.0f -> 0.75f
                                            playbackSpeed < 1.25f -> 1.0f
                                            playbackSpeed < 1.5f -> 1.25f
                                            playbackSpeed < 2.0f -> 1.5f
                                            else -> 0.5f
                                        }
                                    }) {
                                        Icon(Icons.Default.Speed, null, modifier = Modifier.size(14.dp), tint = accent.primary)
                                        Text("${playbackSpeed}x", fontSize = 11.sp, color = accent.primary, fontWeight = FontWeight.Bold)
                                    }
                                    // Repeat
                                    IconButton(onClick = {
                                        repeatMode = when (repeatMode) {
                                            RepeatMode.Off -> RepeatMode.One
                                            RepeatMode.One -> RepeatMode.All
                                            RepeatMode.All -> RepeatMode.Off
                                        }
                                    }) {
                                        Icon(
                                            Icons.Default.Repeat, contentDescription = "Repeat",
                                            tint = when (repeatMode) {
                                                RepeatMode.Off -> surfaceColors.textMuted
                                                RepeatMode.One -> accent.primary
                                                RepeatMode.All -> Color(0xFF4CAF50)
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    // Prev
                                    IconButton(onClick = { currentLineIndex = (currentLineIndex - 1).coerceAtLeast(0) }) {
                                        Icon(Icons.Default.FastRewind, contentDescription = "Prev", tint = surfaceColors.textPrimary, modifier = Modifier.size(20.dp))
                                    }
                                    // Play/Pause
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape).background(accent.primary).clickable { isPlaying = !isPlaying },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    // Forward
                                    IconButton(onClick = { currentLineIndex = (currentLineIndex + 1).coerceAtMost(track.lines.size - 1) }) {
                                        Icon(Icons.Default.FastForward, contentDescription = "Next", tint = surfaceColors.textPrimary, modifier = Modifier.size(20.dp))
                                    }
                                    // Skip track
                                    IconButton(onClick = {
                                        val idx = filteredTracks.indexOfFirst { it.id == track.id }
                                        if (idx >= 0 && idx < filteredTracks.size - 1) {
                                            activeTrack = filteredTracks[idx + 1]
                                            currentLineIndex = 0
                                            playbackProgress = 0f
                                        }
                                    }) {
                                        Icon(Icons.Default.SkipNext, contentDescription = "Skip", tint = surfaceColors.textPrimary, modifier = Modifier.size(20.dp))
                                    }
                                    // Bookmark
                                    IconButton(onClick = {
                                        val idx = tracks.indexOfFirst { it.id == track.id }
                                        if (idx >= 0) tracks[idx] = track.copy(isBookmarked = !track.isBookmarked)
                                    }) {
                                        Icon(
                                            if (track.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Bookmark",
                                            tint = if (track.isBookmarked) accent.primary else surfaceColors.textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ════════════════════════════════════════
                // IMPORT DIALOG
                // ════════════════════════════════════════
                if (showImportDialog) {
                    ImportTrackDialog(
                        accent = accent,
                        surfaceColors = surfaceColors,
                        onImport = { title, japanese, english, category ->
                            val newTrack = MediaTrack(
                                id = "custom_${System.currentTimeMillis()}",
                                title = title,
                                subtitle = english.ifBlank { "Custom Immersion Audio" },
                                category = category,
                                durationSeconds = (japanese.length * 3).coerceIn(10, 120),
                                level = "Custom",
                                tags = listOf("Custom"),
                                lines = listOf(
                                    ImmersionLine(
                                        japanese = japanese,
                                        reading = japanese,
                                        english = english,
                                        keywords = japanese.filter { c ->
                                            c.code in 0x4E00..0x9FFF || c.code in 0x3400..0x4DBF
                                        }.map { it.toString() }
                                    )
                                )
                            )
                            tracks.add(0, newTrack)
                            activeTrack = newTrack
                            showImportDialog = false
                        },
                        onDismiss = { showImportDialog = false }
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
// COMPOSABLE SUB-COMPONENTS
// ════════════════════════════════════════════════════════════════

/**
 * Compact mini track row for the Player tab sidebar.
 */
@Composable
private fun MiniTrackRow(
    track: MediaTrack,
    isActive: Boolean,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    onSelect: () -> Unit,
    onPlay: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isActive) accent.primary.copy(alpha = 0.12f) else surfaceColors.surface,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isActive) accent.primary else surfaceColors.surfaceInteractive)
                    .clickable { onPlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isActive) Color.White else surfaceColors.textPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = surfaceColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${track.lines.size} lines · ${track.level}",
                    fontSize = 10.sp,
                    color = surfaceColors.textMuted
                )
            }
            if (track.isBookmarked) {
                Icon(Icons.Default.Star, null, tint = Color(0xFFE8A838), modifier = Modifier.size(14.dp))
            }
        }
    }
}

/**
 * Full track card for the Library tab.
 */
@Composable
private fun TrackCard(
    track: MediaTrack,
    isActive: Boolean,
    isPlaying: Boolean,
    currentLineIndex: Int,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    onSelect: () -> Unit,
    onToggleBookmark: () -> Unit,
    onPlay: () -> Unit
) {
    val progressFraction = if (isActive && track.lines.isNotEmpty()) {
        (currentLineIndex.toFloat() + 1f) / track.lines.size.toFloat()
    } else 0f

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) accent.primary.copy(alpha = 0.12f) else surfaceColors.surface
        )
    ) {
        Column {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isActive) accent.primary else surfaceColors.surfaceInteractive)
                        .clickable { onPlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isActive) Color.White else surfaceColors.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        track.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = surfaceColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        track.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = surfaceColors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(track.level, fontSize = 11.sp, color = accent.primary, fontWeight = FontWeight.SemiBold)
                        Text("·", color = surfaceColors.textMuted)
                        Text("${track.durationSeconds}s", fontSize = 11.sp, color = surfaceColors.textMuted)
                        Text("·", color = surfaceColors.textMuted)
                        Text("${track.lines.size} lines", fontSize = 11.sp, color = surfaceColors.textMuted)
                    }
                }
                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        if (track.isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Bookmark",
                        tint = if (track.isBookmarked) Color(0xFFE8A838) else surfaceColors.textMuted
                    )
                }
            }
            if (isActive && progressFraction > 0f) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = accent.primary,
                    trackColor = surfaceColors.surfaceInteractive
                )
            }
            if (track.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    track.tags.take(4).forEach { tag ->
                        Surface(shape = RoundedCornerShape(6.dp), color = surfaceColors.surfaceInteractive.copy(alpha = 0.5f)) {
                            Text(tag, fontSize = 10.sp, color = surfaceColors.textSecondary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Right-side transcript panel with line-by-line display, vocab chips, romaji, English.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TranscriptPanel(
    track: MediaTrack,
    currentLineIndex: Int,
    isPlaying: Boolean,
    showRomaji: Boolean,
    showEnglish: Boolean,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    ttsManager: KanaTtsManager?,
    navigationState: MainNavigationState?,
    onLineClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(track.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                    Text(track.subtitle, style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = accent.primary.copy(alpha = 0.15f)) {
                    Text(track.level, color = accent.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(track.lines) { index, line ->
                    val isCurrent = index == currentLineIndex
                    val lineBg by animateColorAsState(
                        if (isCurrent) accent.primary.copy(alpha = 0.15f) else surfaceColors.surfaceInteractive.copy(alpha = 0.3f)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(lineBg)
                            .clickable { onLineClick(index) }
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${index + 1}", fontSize = 11.sp, color = if (isCurrent) accent.primary else surfaceColors.textMuted, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                            Text(line.japanese, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isCurrent) accent.primary else surfaceColors.textPrimary, modifier = Modifier.weight(1f))
                        }
                        if (showRomaji && line.reading.isNotBlank()) {
                            Text("　　${line.reading}", style = MaterialTheme.typography.bodySmall, color = surfaceColors.textSecondary)
                        }
                        if (showEnglish && line.english.isNotBlank()) {
                            Text("　　${line.english}", style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
                        }
                        if (line.keywords.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 4.dp, start = 28.dp)
                            ) {
                                line.keywords.forEach { kw ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = accent.primary.copy(alpha = 0.12f),
                                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable {
                                            navigationState?.navigate(MainDestination.Info(InfoScreenData.Letter(kw)))
                                        }
                                    ) {
                                        Text("🔍 $kw", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full stats panel — listening progress, categories, levels, vocab coverage.
 */
@Composable
private fun StatsPanel(
    tracks: List<MediaTrack>,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    val totalLines = tracks.sumOf { it.lines.size }
    val totalVocab = tracks.flatMap { it.lines }.flatMap { it.keywords }.distinct().size
    val bookmarked = tracks.count { it.isBookmarked }
    val byCategory = tracks.groupBy { it.category }
    val byLevel = tracks.groupBy { it.level }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Overview
        item {
            Text("Statistics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Tracks", tracks.size.toString(), accent.primary, surfaceColors, Modifier.weight(1f))
                StatCard("Lines", totalLines.toString(), accent.primary, surfaceColors, Modifier.weight(1f))
                StatCard("Vocab", totalVocab.toString(), accent.primary, surfaceColors, Modifier.weight(1f))
                StatCard("Saved", bookmarked.toString(), Color(0xFFE8A838), surfaceColors, Modifier.weight(1f))
            }
        }
        // By category
        item {
            Text("By Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary, modifier = Modifier.padding(top = 8.dp))
        }
        items(byCategory.entries.toList()) { (cat, catTracks) ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(cat.icon, null, tint = accent.primary, modifier = Modifier.size(18.dp))
                Text(cat.label, fontSize = 14.sp, color = surfaceColors.textPrimary, modifier = Modifier.weight(1f))
                Text("${catTracks.size} tracks · ${catTracks.sumOf { it.lines.size }} lines", fontSize = 12.sp, color = surfaceColors.textMuted)
                LinearProgressIndicator(
                    progress = { catTracks.size.toFloat() / tracks.size.toFloat().coerceAtLeast(1f) },
                    modifier = Modifier.width(80.dp).height(6.dp),
                    color = accent.primary,
                    trackColor = surfaceColors.surfaceInteractive
                )
            }
        }
        // By level
        item {
            Text("By Level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary, modifier = Modifier.padding(top = 8.dp))
        }
        items(byLevel.entries.toList()) { (level, levelTracks) ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(6.dp), color = accent.primary.copy(alpha = 0.12f)) {
                    Text(level, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
                Text("${levelTracks.size} tracks · ${levelTracks.sumOf { it.lines.size }} lines", fontSize = 12.sp, color = surfaceColors.textMuted, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = surfaceColors.surface) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, color = surfaceColors.textMuted, fontSize = 11.sp)
        }
    }
}

/**
 * Settings panel — speed, repeat, auto-advance, romaji, english, highlight.
 */
@Composable
private fun SettingsPanel(
    playbackSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    repeatMode: RepeatMode,
    onRepeatChange: (RepeatMode) -> Unit,
    autoAdvance: Boolean,
    onAutoAdvanceChange: (Boolean) -> Unit,
    showRomaji: Boolean,
    onShowRomajiChange: (Boolean) -> Unit,
    showEnglish: Boolean,
    onShowEnglishChange: (Boolean) -> Unit,
    highlightActive: Boolean,
    onHighlightActiveChange: (Boolean) -> Unit,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
        }
        // Playback speed
        item {
            SettingsSection("Playback Speed", surfaceColors) {
                Text("${(playbackSpeed * 100).toInt() / 100.0}x", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent.primary)
                Slider(
                    value = playbackSpeed,
                    onValueChange = onSpeedChange,
                    valueRange = 0.25f..2.0f,
                    steps = 6,
                    colors = SliderDefaults.colors(thumbColor = accent.primary, activeTrackColor = accent.primary),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (kotlin.math.abs(playbackSpeed - speed) < 0.01f) accent.primary.copy(alpha = 0.2f) else Color.Transparent,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onSpeedChange(speed) }
                        ) {
                            Text("${speed}x", fontSize = 10.sp, color = if (kotlin.math.abs(playbackSpeed - speed) < 0.01f) accent.primary else surfaceColors.textMuted, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
        // Repeat mode
        item {
            SettingsSection("Repeat Mode", surfaceColors) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RepeatMode.entries.forEach { mode ->
                        val selected = repeatMode == mode
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) accent.primary.copy(alpha = 0.2f) else surfaceColors.surfaceInteractive.copy(alpha = 0.5f),
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onRepeatChange(mode) }
                        ) {
                            Text(
                                when (mode) { RepeatMode.Off -> "Off"; RepeatMode.One -> "Repeat 1"; RepeatMode.All -> "Repeat All" },
                                fontSize = 12.sp,
                                color = if (selected) accent.primary else surfaceColors.textSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
        // Toggles
        item {
            SettingsSection("Display", surfaceColors) {
                SettingsToggle("Auto-advance tracks", autoAdvance, onAutoAdvanceChange, surfaceColors)
                SettingsToggle("Show romaji", showRomaji, onShowRomajiChange, surfaceColors)
                SettingsToggle("Show English translation", showEnglish, onShowEnglishChange, surfaceColors)
                SettingsToggle("Highlight active line", highlightActive, onHighlightActiveChange, surfaceColors)
            }
        }
        // Keyboard shortcuts
        item {
            SettingsSection("Keyboard Shortcuts", surfaceColors) {
                listOf(
                    "Space" to "Play / Pause",
                    "← →" to "Previous / Next line",
                    "↑ ↓" to "Previous / Next track",
                    "S" to "Cycle speed",
                    "R" to "Cycle repeat mode"
                ).forEach { (key, desc) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Surface(shape = RoundedCornerShape(4.dp), color = surfaceColors.surfaceInteractive.copy(alpha = 0.7f)) {
                            Text(key, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                        Text(desc, fontSize = 12.sp, color = surfaceColors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = surfaceColors.surface) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
            content()
        }
    }
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = surfaceColors.textSecondary)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (checked) Color(0xFF4CAF50) else surfaceColors.surfaceInteractive,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onCheckedChange(!checked) }
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(if (checked) "On" else "Off", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Daily Immersion Mode bar — goals, streak, start button.
 */
@Composable
private fun DailyImmersionBar(
    goalTracks: Int,
    goalMinutes: Int,
    tracksPlayed: Int,
    minutesListened: Float,
    daysComplete: Int,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    onStartDaily: () -> Unit,
    onGoalChange: (Int, Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF4CAF50).copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.Today, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text("Daily Immersion Mode", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                Text("Set a daily goal and track your streak", fontSize = 12.sp, color = surfaceColors.textMuted)
                Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tracks:", fontSize = 11.sp, color = surfaceColors.textSecondary)
                        listOf(1, 3, 5).forEach { t ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (goalTracks == t) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color.Transparent,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onGoalChange(t, goalMinutes) }
                            ) {
                                Text("$t", fontSize = 11.sp, color = if (goalTracks == t) Color(0xFF4CAF50) else surfaceColors.textMuted, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Min:", fontSize = 11.sp, color = surfaceColors.textSecondary)
                        listOf(5, 15, 30).forEach { m ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (goalMinutes == m) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color.Transparent,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onGoalChange(goalTracks, m) }
                            ) {
                                Text("$m", fontSize = 11.sp, color = if (goalMinutes == m) Color(0xFF4CAF50) else surfaceColors.textMuted, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔥 $daysComplete", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE8A838))
                Text("day streak", fontSize = 10.sp, color = surfaceColors.textMuted)
            }
            Button(
                onClick = onStartDaily,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Start", fontSize = 13.sp)
            }
        }
    }
}

/**
 * Import track dialog.
 */
@Composable
private fun ImportTrackDialog(
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    onImport: (title: String, japanese: String, english: String, category: MediaCategory) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var japanese by remember { mutableStateOf("") }
    var english by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(MediaCategory.Sentences) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Immersion Media", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add custom Japanese dialogue, podcast snippet, or sentences.", fontSize = 13.sp, color = surfaceColors.textMuted)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, placeholder = { Text("e.g. Shopping in Shibuya") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = japanese, onValueChange = { japanese = it }, label = { Text("Japanese") }, placeholder = { Text("e.g. これをください。") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = english, onValueChange = { english = it }, label = { Text("English") }, placeholder = { Text("e.g. I will take this.") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MediaCategory.entries.filter { it != MediaCategory.Saved }.forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (category == cat) accent.primary.copy(alpha = 0.2f) else Color.Transparent,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { category = cat }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(cat.icon, null, modifier = Modifier.size(14.dp), tint = if (category == cat) accent.primary else surfaceColors.textSecondary)
                                Text(cat.label, fontSize = 12.sp, color = if (category == cat) accent.primary else surfaceColors.textSecondary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank() && japanese.isNotBlank()) onImport(title, japanese, english, category) },
                colors = ButtonDefaults.buttonColors(containerColor = accent.primary),
                enabled = title.isNotBlank() && japanese.isNotBlank()
            ) { Text("Add to Library") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Koin module ──
val mediaCentreModule = module {
    single<MediaCentreContent> { DefaultMediaCentreContent }
}
