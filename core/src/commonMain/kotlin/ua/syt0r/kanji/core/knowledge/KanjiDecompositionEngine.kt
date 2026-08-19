package ua.syt0r.kanji.core.knowledge

import kotlinx.serialization.Serializable

// ============================================================
// KANJI DECOMPOSITION ENGINE
// ------------------------------------------------------------
// Decomposes kanji into components, radicals, and structural
// parts. Provides structural analysis (left-right, top-bottom,
// enclosure, etc.) and component-level lookups.
//
// The engine ships with a CURATED starter decomposition dataset
// (well-established structural facts for common jōyō kanji) plus
// a Kangxi radical table. It NEVER fabricates data — a kanji not
// in the dataset returns null/empty, so "no data" is always
// honest. The dataset is data, not code: extending it never
// requires touching the engine logic.
// ============================================================

/** Structural decomposition of a kanji. */
@Serializable
data class KanjiDecomposition(
    /** The kanji being decomposed. */
    val kanji: String,
    /** Top-level components. */
    val components: List<DecompositionComponent>,
    /** The Kangxi radical (if known). */
    val radical: DecompositionComponent? = null,
    /** Structural layout type. */
    val layout: DecompositionLayout,
    /** Total stroke count. */
    val strokeCount: Int? = null,
    /** Stroke order data (list of stroke descriptions). */
    val strokeOrder: List<StrokeData>? = null,
    /** Confidence in this decomposition. */
    val confidence: ContentConfidence = ContentConfidence.High
)

/** A component in a decomposition. */
@Serializable
data class DecompositionComponent(
    /** The component character/shape. */
    val character: String,
    /** Position within the kanji. */
    val position: ComponentPosition,
    /** Meaning of the component (if known). */
    val meaning: String? = null,
    /** Is this the Kangxi radical? */
    val isRadical: Boolean = false,
    /** Is this a semantic component? */
    val isSemantic: Boolean = false,
    /** Is this a phonetic component? */
    val isPhonetic: Boolean = false,
    /** Reading hint (if phonetic). */
    val phoneticHint: String? = null,
    /** Sub-components (recursive decomposition). */
    val subComponents: List<DecompositionComponent> = emptyList()
)

/** Position of a component within the kanji. */
enum class ComponentPosition(val label: String) {
    Left("Left"),
    Right("Right"),
    Top("Top"),
    Bottom("Bottom"),
    TopLeft("Top-left"),
    TopRight("Top-right"),
    BottomLeft("Bottom-left"),
    BottomRight("Bottom-right"),
    Center("Center"),
    Enclosure("Enclosure"),
    Enclosed("Enclosed"),
    Overlapping("Overlapping"),
    Surrounding("Surrounding"),
    Full("Full character")
}

/** Structural layout type of a kanji. */
enum class DecompositionLayout(val label: String) {
    LeftRight("Left-right (左右)"),
    TopBottom("Top-bottom (上下)"),
    Enclosure("Enclosure (囲み)"),
    Surround("Surround (かこみ)"),
    Overlap("Overlap (重なり)"),
    Single("Single component (独体)"),
    Diagonal("Diagonal (はね/れつ)"),
    Complex("Complex (複合)"),
    Unknown("Unknown")
}

/** Stroke data for a single stroke. */
@Serializable
data class StrokeData(
    /** Stroke index (1-based). */
    val index: Int,
    /** Stroke type (horizontal, vertical, diagonal, dot, hook, etc.). */
    val type: StrokeType,
    /** Starting position (x, y) normalized 0-1. */
    val startX: Float = 0f,
    val startY: Float = 0f,
    /** Ending position (x, y) normalized 0-1. */
    val endX: Float = 0f,
    val endY: Float = 0f,
    /** Direction of the stroke. */
    val direction: StrokeDirection = StrokeDirection.Unknown
)

/** Stroke types. */
enum class StrokeType(val label: String) {
    Horizontal("Horizontal (よこ)"),
    Vertical("Vertical (たて)"),
    DiagonalRight("Diagonal right (はね)"),
    DiagonalLeft("Diagonal left (れつ)"),
    Dot("Dot (てん)"),
    Hook("Hook (はね)"),
    Bend("Bend (われ)"),
    Curve("Curve (curve)"),
    Press("Press (おとし)"),
    Unknown("Unknown")
}

/** Stroke directions. */
enum class StrokeDirection(val label: String) {
    LeftToRight("Left → Right"),
    RightToLeft("Right → Left"),
    TopToBottom("Top → Bottom"),
    BottomToTop("Bottom → Top"),
    DiagonalUpRight("Diagonal ↗"),
    DiagonalUpLeft("Diagonal ↖"),
    DiagonalDownRight("Diagonal ↘"),
    DiagonalDownLeft("Diagonal ↙"),
    Unknown("Unknown")
}

/**
 * One curated decomposition entry. `components` are the direct
 * top-level parts; the engine builds the recursive view on demand.
 */
data class DecompositionEntry(
    val kanji: String,
    val components: List<DecompositionComponent>,
    val radical: DecompositionComponent?,
    val layout: DecompositionLayout,
    val strokeCount: Int? = null
)

/**
 * A data source for decompositions. The bundled app ships
 * [CuratedDecompositionDataset]; a licensed dataset can be
 * swapped in later without touching engine logic.
 */
interface DecompositionDataset {
    fun entry(kanji: String): DecompositionEntry?
    fun allKanji(): List<String>
}

/**
 * The bundled curated starter dataset. Contains well-established
 * structural decompositions for common jōyō kanji (standard CJK
 * decomposition facts, e.g. 明 = 日 + 月). Kanji not listed here
 * simply have no data — the engine never invents a decomposition.
 */
object CuratedDecompositionDataset : DecompositionDataset {

    private val entries: Map<String, DecompositionEntry> = buildList {
        // ---- Single-component (独体) kanji ----
        add(single("木", 4, "tree; wood"))
        add(single("火", 4, "fire"))
        add(single("水", 4, "water"))
        add(single("土", 3, "earth; ground"))
        add(single("山", 3, "mountain"))
        add(single("石", 5, "stone"))
        add(single("口", 3, "mouth"))
        add(single("日", 4, "sun; day"))
        add(single("月", 4, "moon; month"))
        add(single("田", 5, "rice field"))
        add(single("力", 2, "power; strength"))
        add(single("女", 3, "woman"))
        add(single("子", 3, "child"))
        add(single("人", 2, "person"))
        add(single("門", 8, "gate"))
        add(single("立", 5, "stand"))
        add(single("米", 6, "rice"))

        // ---- Left-right (左右) ----
        add(
            DecompositionEntry(
                kanji = "明", layout = DecompositionLayout.LeftRight, strokeCount = 8,
                radical = radical("日"),
                components = listOf(
                    comp("日", ComponentPosition.Left, "sun", isRadical = true, isSemantic = true),
                    comp("月", ComponentPosition.Right, "moon", isSemantic = true)
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "休", layout = DecompositionLayout.LeftRight, strokeCount = 6,
                radical = radical("亻"),
                components = listOf(
                    comp("亻", ComponentPosition.Left, "person", isRadical = true, isSemantic = true),
                    comp("木", ComponentPosition.Right, "tree", isSemantic = true)
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "好", layout = DecompositionLayout.LeftRight, strokeCount = 6,
                radical = radical("女"),
                components = listOf(
                    comp("女", ComponentPosition.Left, "woman", isRadical = true, isSemantic = true),
                    comp("子", ComponentPosition.Right, "child", isSemantic = true)
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "林", layout = DecompositionLayout.LeftRight, strokeCount = 8,
                radical = radical("木"),
                components = listOf(
                    comp("木", ComponentPosition.Left, "tree", isRadical = true, isSemantic = true),
                    comp("木", ComponentPosition.Right, "tree", isSemantic = true)
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "海", layout = DecompositionLayout.LeftRight, strokeCount = 9,
                radical = radical("氵"),
                components = listOf(
                    comp("氵", ComponentPosition.Left, "water", isRadical = true, isSemantic = true),
                    comp("毎", ComponentPosition.Right, "every", isPhonetic = true, phoneticHint = "kai")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "河", layout = DecompositionLayout.LeftRight, strokeCount = 8,
                radical = radical("氵"),
                components = listOf(
                    comp("氵", ComponentPosition.Left, "water", isRadical = true, isSemantic = true),
                    comp("可", ComponentPosition.Right, "possible", isPhonetic = true, phoneticHint = "ka")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "池", layout = DecompositionLayout.LeftRight, strokeCount = 6,
                radical = radical("氵"),
                components = listOf(
                    comp("氵", ComponentPosition.Left, "water", isRadical = true, isSemantic = true),
                    comp("也", ComponentPosition.Right, "also", isPhonetic = true, phoneticHint = "chi")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "油", layout = DecompositionLayout.LeftRight, strokeCount = 8,
                radical = radical("氵"),
                components = listOf(
                    comp("氵", ComponentPosition.Left, "water", isRadical = true, isSemantic = true),
                    comp("由", ComponentPosition.Right, "reason; cause", isPhonetic = true, phoneticHint = "yu")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "湖", layout = DecompositionLayout.LeftRight, strokeCount = 12,
                radical = radical("氵"),
                components = listOf(
                    comp("氵", ComponentPosition.Left, "water", isRadical = true, isSemantic = true),
                    comp("胡", ComponentPosition.Right, "barbarian (phonetic)", isPhonetic = true, phoneticHint = "ko")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "語", layout = DecompositionLayout.LeftRight, strokeCount = 14,
                radical = radical("言"),
                components = listOf(
                    comp("言", ComponentPosition.Left, "word; speech", isRadical = true, isSemantic = true),
                    comp("吾", ComponentPosition.Right, "I; me (phonetic)", isPhonetic = true, phoneticHint = "go")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "話", layout = DecompositionLayout.LeftRight, strokeCount = 13,
                radical = radical("言"),
                components = listOf(
                    comp("言", ComponentPosition.Left, "word; speech", isRadical = true, isSemantic = true),
                    comp("舌", ComponentPosition.Right, "tongue", isPhonetic = true, phoneticHint = "wa")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "読", layout = DecompositionLayout.LeftRight, strokeCount = 14,
                radical = radical("言"),
                components = listOf(
                    comp("言", ComponentPosition.Left, "word; speech", isRadical = true, isSemantic = true),
                    comp("売", ComponentPosition.Right, "sell (phonetic)", isPhonetic = true, phoneticHint = "do")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "校", layout = DecompositionLayout.LeftRight, strokeCount = 10,
                radical = radical("木"),
                components = listOf(
                    comp("木", ComponentPosition.Left, "tree", isRadical = true, isSemantic = true),
                    comp("交", ComponentPosition.Right, "mix; exchange", isPhonetic = true, phoneticHint = "ko")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "板", layout = DecompositionLayout.LeftRight, strokeCount = 8,
                radical = radical("木"),
                components = listOf(
                    comp("木", ComponentPosition.Left, "tree", isRadical = true, isSemantic = true),
                    comp("反", ComponentPosition.Right, "reverse", isPhonetic = true, phoneticHint = "ban")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "杯", layout = DecompositionLayout.LeftRight, strokeCount = 8,
                radical = radical("木"),
                components = listOf(
                    comp("木", ComponentPosition.Left, "tree", isRadical = true, isSemantic = true),
                    comp("不", ComponentPosition.Right, "not (phonetic)", isPhonetic = true, phoneticHint = "hai")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "材", layout = DecompositionLayout.LeftRight, strokeCount = 7,
                radical = radical("木"),
                components = listOf(
                    comp("木", ComponentPosition.Left, "tree", isRadical = true, isSemantic = true),
                    comp("才", ComponentPosition.Right, "talent (phonetic)", isPhonetic = true, phoneticHint = "zai")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "時", layout = DecompositionLayout.LeftRight, strokeCount = 10,
                radical = radical("日"),
                components = listOf(
                    comp("日", ComponentPosition.Left, "sun; time", isRadical = true, isSemantic = true),
                    comp("寺", ComponentPosition.Right, "temple (phonetic)", isPhonetic = true, phoneticHint = "ji")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "晴", layout = DecompositionLayout.LeftRight, strokeCount = 12,
                radical = radical("日"),
                components = listOf(
                    comp("日", ComponentPosition.Left, "sun", isRadical = true, isSemantic = true),
                    comp("青", ComponentPosition.Right, "blue (phonetic)", isPhonetic = true, phoneticHint = "sei")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "清", layout = DecompositionLayout.LeftRight, strokeCount = 11,
                radical = radical("氵"),
                components = listOf(
                    comp("氵", ComponentPosition.Left, "water", isRadical = true, isSemantic = true),
                    comp("青", ComponentPosition.Right, "blue (phonetic)", isPhonetic = true, phoneticHint = "sei")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "情", layout = DecompositionLayout.LeftRight, strokeCount = 11,
                radical = radical("忄"),
                components = listOf(
                    comp("忄", ComponentPosition.Left, "heart", isRadical = true, isSemantic = true),
                    comp("青", ComponentPosition.Right, "blue (phonetic)", isPhonetic = true, phoneticHint = "jō")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "精", layout = DecompositionLayout.LeftRight, strokeCount = 14,
                radical = radical("米"),
                components = listOf(
                    comp("米", ComponentPosition.Left, "rice", isRadical = true, isSemantic = true),
                    comp("青", ComponentPosition.Right, "blue (phonetic)", isPhonetic = true, phoneticHint = "sei")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "星", layout = DecompositionLayout.TopBottom, strokeCount = 9,
                radical = radical("日"),
                components = listOf(
                    comp("日", ComponentPosition.Top, "sun", isRadical = true, isSemantic = true),
                    comp("生", ComponentPosition.Bottom, "life; birth", isPhonetic = true, phoneticHint = "sei")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "秋", layout = DecompositionLayout.LeftRight, strokeCount = 9,
                radical = radical("禾"),
                components = listOf(
                    comp("禾", ComponentPosition.Left, "grain", isRadical = true, isSemantic = true),
                    comp("火", ComponentPosition.Right, "fire", isSemantic = true)
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "私", layout = DecompositionLayout.LeftRight, strokeCount = 7,
                radical = radical("禾"),
                components = listOf(
                    comp("禾", ComponentPosition.Left, "grain", isRadical = true, isSemantic = true),
                    comp("厶", ComponentPosition.Right, "private (phonetic)", isPhonetic = true, phoneticHint = "shi")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "科", layout = DecompositionLayout.LeftRight, strokeCount = 9,
                radical = radical("禾"),
                components = listOf(
                    comp("禾", ComponentPosition.Left, "grain", isRadical = true, isSemantic = true),
                    comp("斗", ComponentPosition.Right, "dipper; measure", isPhonetic = true, phoneticHint = "ka")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "秒", layout = DecompositionLayout.LeftRight, strokeCount = 9,
                radical = radical("禾"),
                components = listOf(
                    comp("禾", ComponentPosition.Left, "grain", isRadical = true, isSemantic = true),
                    comp("少", ComponentPosition.Right, "few; little", isPhonetic = true, phoneticHint = "byō")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "飲", layout = DecompositionLayout.LeftRight, strokeCount = 12,
                radical = radical("飠"),
                components = listOf(
                    comp("飠", ComponentPosition.Left, "food", isRadical = true, isSemantic = true),
                    comp("欠", ComponentPosition.Right, "lack; yawn", isSemantic = true)
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "飯", layout = DecompositionLayout.LeftRight, strokeCount = 12,
                radical = radical("飠"),
                components = listOf(
                    comp("飠", ComponentPosition.Left, "food", isRadical = true, isSemantic = true),
                    comp("反", ComponentPosition.Right, "reverse (phonetic)", isPhonetic = true, phoneticHint = "han")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "洗", layout = DecompositionLayout.LeftRight, strokeCount = 9,
                radical = radical("氵"),
                components = listOf(
                    comp("氵", ComponentPosition.Left, "water", isRadical = true, isSemantic = true),
                    comp("先", ComponentPosition.Right, "before (phonetic)", isPhonetic = true, phoneticHint = "sen")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "泳", layout = DecompositionLayout.LeftRight, strokeCount = 8,
                radical = radical("氵"),
                components = listOf(
                    comp("氵", ComponentPosition.Left, "water", isRadical = true, isSemantic = true),
                    comp("永", ComponentPosition.Right, "eternal (phonetic)", isPhonetic = true, phoneticHint = "ei")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "決", layout = DecompositionLayout.LeftRight, strokeCount = 7,
                radical = radical("氵"),
                components = listOf(
                    comp("氵", ComponentPosition.Left, "water", isRadical = true, isSemantic = true),
                    comp("夬", ComponentPosition.Right, "decisive (phonetic)", isPhonetic = true, phoneticHint = "ketsu")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "汽", layout = DecompositionLayout.LeftRight, strokeCount = 7,
                radical = radical("氵"),
                components = listOf(
                    comp("氵", ComponentPosition.Left, "water", isRadical = true, isSemantic = true),
                    comp("气", ComponentPosition.Right, "steam; gas", isPhonetic = true, phoneticHint = "ki")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "洋", layout = DecompositionLayout.LeftRight, strokeCount = 9,
                radical = radical("氵"),
                components = listOf(
                    comp("氵", ComponentPosition.Left, "water", isRadical = true, isSemantic = true),
                    comp("羊", ComponentPosition.Right, "sheep (phonetic)", isPhonetic = true, phoneticHint = "yō")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "美", layout = DecompositionLayout.TopBottom, strokeCount = 9,
                radical = radical("羊"),
                components = listOf(
                    comp("羊", ComponentPosition.Top, "sheep", isRadical = true, isSemantic = true),
                    comp("大", ComponentPosition.Bottom, "big", isSemantic = true)
                )
            )
        )

        // ---- Top-bottom (上下) ----
        add(
            DecompositionEntry(
                kanji = "森", layout = DecompositionLayout.TopBottom, strokeCount = 12,
                radical = radical("木"),
                components = listOf(
                    comp("木", ComponentPosition.Top, "tree", isRadical = true, isSemantic = true),
                    comp("林", ComponentPosition.Bottom, "grove", isSemantic = true)
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "岩", layout = DecompositionLayout.TopBottom, strokeCount = 8,
                radical = radical("山"),
                components = listOf(
                    comp("山", ComponentPosition.Top, "mountain", isRadical = true, isSemantic = true),
                    comp("石", ComponentPosition.Bottom, "stone", isSemantic = true)
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "男", layout = DecompositionLayout.TopBottom, strokeCount = 7,
                radical = radical("田"),
                components = listOf(
                    comp("田", ComponentPosition.Top, "field", isRadical = true, isSemantic = true),
                    comp("力", ComponentPosition.Bottom, "power", isSemantic = true)
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "早", layout = DecompositionLayout.TopBottom, strokeCount = 6,
                radical = radical("日"),
                components = listOf(
                    comp("日", ComponentPosition.Top, "sun", isRadical = true, isSemantic = true),
                    comp("十", ComponentPosition.Bottom, "ten", isSemantic = true)
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "花", layout = DecompositionLayout.TopBottom, strokeCount = 7,
                radical = radical("艹"),
                components = listOf(
                    comp("艹", ComponentPosition.Top, "grass", isRadical = true, isSemantic = true),
                    comp("化", ComponentPosition.Bottom, "change (phonetic)", isPhonetic = true, phoneticHint = "ka")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "草", layout = DecompositionLayout.TopBottom, strokeCount = 9,
                radical = radical("艹"),
                components = listOf(
                    comp("艹", ComponentPosition.Top, "grass", isRadical = true, isSemantic = true),
                    comp("早", ComponentPosition.Bottom, "early", isPhonetic = true, phoneticHint = "sō")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "空", layout = DecompositionLayout.TopBottom, strokeCount = 8,
                radical = radical("穴"),
                components = listOf(
                    comp("穴", ComponentPosition.Top, "hole; cave", isRadical = true, isSemantic = true),
                    comp("工", ComponentPosition.Bottom, "work (phonetic)", isPhonetic = true, phoneticHint = "kū")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "究", layout = DecompositionLayout.TopBottom, strokeCount = 7,
                radical = radical("穴"),
                components = listOf(
                    comp("穴", ComponentPosition.Top, "hole; cave", isRadical = true, isSemantic = true),
                    comp("九", ComponentPosition.Bottom, "nine (phonetic)", isPhonetic = true, phoneticHint = "kyū")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "家", layout = DecompositionLayout.TopBottom, strokeCount = 10,
                radical = radical("宀"),
                components = listOf(
                    comp("宀", ComponentPosition.Top, "roof", isRadical = true, isSemantic = true),
                    comp("豕", ComponentPosition.Bottom, "pig", isSemantic = true)
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "安", layout = DecompositionLayout.TopBottom, strokeCount = 6,
                radical = radical("宀"),
                components = listOf(
                    comp("宀", ComponentPosition.Top, "roof", isRadical = true, isSemantic = true),
                    comp("女", ComponentPosition.Bottom, "woman", isSemantic = true)
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "字", layout = DecompositionLayout.TopBottom, strokeCount = 6,
                radical = radical("宀"),
                components = listOf(
                    comp("宀", ComponentPosition.Top, "roof", isRadical = true, isSemantic = true),
                    comp("子", ComponentPosition.Bottom, "child", isSemantic = true)
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "室", layout = DecompositionLayout.TopBottom, strokeCount = 9,
                radical = radical("宀"),
                components = listOf(
                    comp("宀", ComponentPosition.Top, "roof", isRadical = true, isSemantic = true),
                    comp("至", ComponentPosition.Bottom, "arrive (phonetic)", isPhonetic = true, phoneticHint = "shitsu")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "音", layout = DecompositionLayout.TopBottom, strokeCount = 9,
                radical = radical("音"),
                components = listOf(
                    comp("立", ComponentPosition.Top, "stand", isSemantic = true),
                    comp("日", ComponentPosition.Bottom, "sun; day", isSemantic = true)
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "章", layout = DecompositionLayout.TopBottom, strokeCount = 11,
                radical = radical("立"),
                components = listOf(
                    comp("立", ComponentPosition.Top, "stand", isRadical = true, isSemantic = true),
                    comp("早", ComponentPosition.Bottom, "early", isPhonetic = true, phoneticHint = "shō")
                )
            )
        )

        // ---- Enclosure (囲み) ----
        add(
            DecompositionEntry(
                kanji = "間", layout = DecompositionLayout.Enclosure, strokeCount = 12,
                radical = radical("門"),
                components = listOf(
                    comp("門", ComponentPosition.Enclosure, "gate", isRadical = true, isSemantic = true),
                    comp("日", ComponentPosition.Enclosed, "sun; day", isSemantic = true)
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "問", layout = DecompositionLayout.Enclosure, strokeCount = 11,
                radical = radical("門"),
                components = listOf(
                    comp("門", ComponentPosition.Enclosure, "gate", isRadical = true, isSemantic = true),
                    comp("口", ComponentPosition.Enclosed, "mouth", isSemantic = true)
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "聞", layout = DecompositionLayout.Enclosure, strokeCount = 14,
                radical = radical("門"),
                components = listOf(
                    comp("門", ComponentPosition.Enclosure, "gate", isRadical = true, isSemantic = true),
                    comp("耳", ComponentPosition.Enclosed, "ear", isSemantic = true)
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "国", layout = DecompositionLayout.Enclosure, strokeCount = 8,
                radical = radical("囗"),
                components = listOf(
                    comp("囗", ComponentPosition.Enclosure, "enclosure", isRadical = true, isSemantic = true),
                    comp("玉", ComponentPosition.Enclosed, "jewel", isSemantic = true)
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "回", layout = DecompositionLayout.Enclosure, strokeCount = 6,
                radical = radical("囗"),
                components = listOf(
                    comp("囗", ComponentPosition.Enclosure, "enclosure", isRadical = true, isSemantic = true),
                    comp("口", ComponentPosition.Enclosed, "mouth", isSemantic = true)
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "病", layout = DecompositionLayout.Enclosure, strokeCount = 10,
                radical = radical("疒"),
                components = listOf(
                    comp("疒", ComponentPosition.Enclosure, "sickness", isRadical = true, isSemantic = true),
                    comp("丙", ComponentPosition.Enclosed, "third (phonetic)", isPhonetic = true, phoneticHint = "byō")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "痛", layout = DecompositionLayout.Enclosure, strokeCount = 12,
                radical = radical("疒"),
                components = listOf(
                    comp("疒", ComponentPosition.Enclosure, "sickness", isRadical = true, isSemantic = true),
                    comp("甬", ComponentPosition.Enclosed, "path (phonetic)", isPhonetic = true, phoneticHint = "tsū")
                )
            )
        )
        add(
            DecompositionEntry(
                kanji = "食", layout = DecompositionLayout.TopBottom, strokeCount = 9,
                radical = radical("食"),
                components = listOf(
                    comp("人", ComponentPosition.Top, "person", isSemantic = true),
                    comp("良", ComponentPosition.Bottom, "good", isSemantic = true)
                )
            )
        )
    }.associateBy { it.kanji }

    override fun entry(kanji: String): DecompositionEntry? = entries[kanji]

    override fun allKanji(): List<String> = entries.keys.sorted()

    /** Helper: a single-component (独体) kanji whose only part is itself. */
    private fun single(kanji: String, strokes: Int, meaning: String): DecompositionEntry =
        DecompositionEntry(
            kanji = kanji,
            layout = DecompositionLayout.Single,
            strokeCount = strokes,
            radical = DecompositionComponent(
                character = kanji, position = ComponentPosition.Full,
                meaning = meaning, isRadical = true, isSemantic = true
            ),
            components = listOf(
                DecompositionComponent(
                    character = kanji, position = ComponentPosition.Full,
                    meaning = meaning, isRadical = true, isSemantic = true
                )
            )
        )

    private fun comp(
        character: String,
        position: ComponentPosition,
        meaning: String,
        isRadical: Boolean = false,
        isSemantic: Boolean = false,
        isPhonetic: Boolean = false,
        phoneticHint: String? = null
    ) = DecompositionComponent(
        character = character, position = position, meaning = meaning,
        isRadical = isRadical, isSemantic = isSemantic,
        isPhonetic = isPhonetic, phoneticHint = phoneticHint
    )

    private fun radical(character: String) = DecompositionComponent(
        character = character, position = ComponentPosition.Left,
        meaning = "radical", isRadical = true
    )
}

/** Kangxi radical table (subset used by the curated dataset). */
data class KangxiRadical(
    val character: String,
    val number: Int,
    val japaneseName: String,
    val meaning: String,
    val strokeCount: Int,
    val variants: List<String> = emptyList()
)

/** Well-known Kangxi radicals referenced by the curated dataset. */
object KangxiRadicalTable {

    val radicals: Map<String, KangxiRadical> = listOf(
        KangxiRadical("亻", 9, "にんべん", "person", 2, variants = listOf("人")),
        KangxiRadical("口", 30, "くち", "mouth", 3),
        KangxiRadical("囗", 31, "くにがまえ", "enclosure", 3),
        KangxiRadical("土", 32, "つち", "earth", 3),
        KangxiRadical("女", 38, "おんな", "woman", 3),
        KangxiRadical("子", 39, "こ", "child", 3),
        KangxiRadical("宀", 40, "うかんむり", "roof", 3),
        KangxiRadical("山", 46, "やま", "mountain", 3),
        KangxiRadical("工", 48, "こう", "work", 3),
        KangxiRadical("廾", 55, "にじゅうあし", "two hands", 3),
        KangxiRadical("心", 61, "こころ", "heart", 4, variants = listOf("忄")),
        KangxiRadical("日", 72, "ひ", "sun; day", 4),
        KangxiRadical("月", 74, "つき", "moon; month", 4),
        KangxiRadical("木", 75, "き", "tree; wood", 4),
        KangxiRadical("欠", 76, "あくび", "lack; yawn", 4),
        KangxiRadical("水", 85, "みず", "water", 4, variants = listOf("氵")),
        KangxiRadical("火", 86, "ひ", "fire", 4),
        KangxiRadical("牛", 93, "うし", "cow", 4),
        KangxiRadical("田", 102, "た", "field", 5),
        KangxiRadical("疒", 104, "やまいだれ", "sickness", 5),
        KangxiRadical("石", 112, "いし", "stone", 5),
        KangxiRadical("禾", 115, "のぎへん", "grain", 5),
        KangxiRadical("穴", 116, "あな", "cave; hole", 5),
        KangxiRadical("立", 117, "たつ", "stand", 5),
        KangxiRadical("米", 119, "こめ", "rice", 6),
        KangxiRadical("耳", 128, "みみ", "ear", 6),
        KangxiRadical("艹", 140, "くさかんむり", "grass", 6, variants = listOf("艸")),
        KangxiRadical("言", 149, "げん", "word; speech", 7),
        KangxiRadical("食", 184, "しょく", "food; eat", 8, variants = listOf("飠")),
        KangxiRadical("門", 169, "もん", "gate", 8)
    ).associateBy { it.character }

    fun byCharacter(character: String): KangxiRadical? = radicals[character]

    fun byVariant(variant: String): KangxiRadical? =
        radicals.values.firstOrNull { variant in it.variants }
}

/**
 * Kanji decomposition engine. Uses the curated dataset; a licensed
 * dataset can be injected via [DecompositionDataset] without changing
 * engine logic. Missing data is honest: null / empty / 0.
 */
class KanjiDecompositionEngine(
    private val dataset: DecompositionDataset = CuratedDecompositionDataset
) {

    /** True when the string is exactly one CJK unified ideograph. */
    private fun isSingleKanji(kanji: String): Boolean {
        if (kanji.length != 1) return false
        val code = kanji[0].code
        return code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF
    }

    /**
     * Decompose a kanji into its components.
     * Returns null when the input isn't a kanji or the dataset has no entry.
     */
    fun decompose(kanji: String): KanjiDecomposition? {
        if (!isSingleKanji(kanji)) return null
        val entry = dataset.entry(kanji) ?: return null
        return KanjiDecomposition(
            kanji = kanji,
            components = entry.components,
            radical = entry.radical,
            layout = entry.layout,
            strokeCount = entry.strokeCount,
            confidence = ContentConfidence.High
        )
    }

    /**
     * Determine the structural layout of a kanji. Dataset-backed when
     * known; otherwise Unknown (never a guessed structure).
     */
    fun estimateLayout(kanji: String): DecompositionLayout {
        if (!isSingleKanji(kanji)) return DecompositionLayout.Unknown
        return dataset.entry(kanji)?.layout ?: DecompositionLayout.Unknown
    }

    /**
     * Get the Kangxi radical for a kanji (radical character + number).
     * Returns null when the kanji or its radical is not in the dataset.
     */
    fun getRadical(kanji: String): RadicalInfo? {
        if (!isSingleKanji(kanji)) return null
        val entry = dataset.entry(kanji) ?: return null
        val radicalChar = entry.radical?.character ?: return null
        val table = KangxiRadicalTable.byCharacter(radicalChar)
            ?: KangxiRadicalTable.byVariant(radicalChar)
            ?: return null
        return RadicalInfo(
            character = table.character,
            number = table.number,
            japaneseName = table.japaneseName,
            meaning = table.meaning,
            strokeCount = table.strokeCount,
            variants = table.variants
        )
    }

    /** Inverted index: component character → kanji that contain it. */
    private val componentIndex: Map<String, List<String>> by lazy {
        val index = mutableMapOf<String, MutableList<String>>()
        dataset.allKanji().forEach { kanji ->
            val entry = dataset.entry(kanji) ?: return@forEach
            entry.components.forEach { component ->
                index.getOrPut(component.character) { mutableListOf() }.add(kanji)
            }
        }
        index.mapValues { it.value.sorted() }
    }

    /**
     * Find all kanji that contain a given component.
     * Returns empty when the component is unknown — never fake results.
     */
    fun findKanjiContainingComponent(component: String): List<String> {
        if (component.isBlank()) return emptyList()
        return componentIndex[component] ?: emptyList()
    }

    /**
     * Find all components of a kanji (recursive decomposition).
     * Empty when the kanji has no dataset entry.
     */
    fun fullDecomposition(kanji: String, maxDepth: Int = 3): List<DecompositionComponent> {
        val direct = decompose(kanji)?.components ?: return emptyList()
        if (maxDepth <= 0) return direct

        return direct.map { component ->
            if (component.character != component.character.trim()) return@map component
            if (component.character.length == 1 && isSingleKanji(component.character)) {
                val sub = fullDecomposition(component.character, maxDepth - 1)
                if (sub.isNotEmpty() && sub != listOf(component)) {
                    component.copy(subComponents = sub)
                } else {
                    component
                }
            } else {
                component
            }
        }
    }

    /**
     * Structural similarity (Jaccard over shared components, 0..1).
     * 0 when either kanji has no data — never a guessed similarity.
     */
    fun structuralSimilarity(kanji1: String, kanji2: String): Float {
        val comp1 = decompose(kanji1)?.components?.map { it.character }?.toSet() ?: return 0f
        val comp2 = decompose(kanji2)?.components?.map { it.character }?.toSet() ?: return 0f
        if (comp1.isEmpty() || comp2.isEmpty()) return 0f
        val intersection = comp1.intersect(comp2)
        val union = comp1.union(comp2)
        return if (union.isEmpty()) 0f else intersection.size.toFloat() / union.size
    }
}

/** Radical information. */
@Serializable
data class RadicalInfo(
    /** The radical character. */
    val character: String,
    /** Kangxi radical number. */
    val number: Int,
    /** Japanese name. */
    val japaneseName: String? = null,
    /** English meaning. */
    val meaning: String? = null,
    /** Stroke count of the radical. */
    val strokeCount: Int,
    /** Alternative forms/variants. */
    val variants: List<String> = emptyList()
)
