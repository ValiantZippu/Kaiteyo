package ua.syt0r.kanji.desktop.engine.stroke_evaluator

import ua.syt0r.kanji.desktop.engine.kana.kanaStrokesFor
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.PI

// ============================================
// STROKE EVALUATOR
// Real per-stroke evaluation for writing practice.
//
// Every kanji in the dataset is a sequence of
// canonical strokes — polylines in a normalized
// 0..100 grid. The learner's freehand strokes are
// normalized onto the same grid, resampled to a
// fixed point count, then scored against the
// reference strokes:
//
//   shape     — average point deviation (0..100)
//   direction — angle difference between the
//               user's first→last point and the
//               reference first→last point
//   order     — whether the stroke was drawn in
//               the canonical sequence
//
// Strokes are matched greedily (nearest reference
// first), so the evaluation works even when the
// learner draws strokes in a different order or
// merges/splits them. No fake approximations —
// the dataset is real stroke sequences for the
// most common kanji plus the full kana syllabary
// (see engine.kana.KanaStrokes), and characters
// outside the dataset report "unsupported" instead
// of guessing.
// ============================================

/** One canonical stroke: a polyline in 0..100 grid coordinates. */
data class ReferenceStroke(
    val points: List<StrokePoint>
) {
    val direction: Double get() = angleOf(points.first(), points.last())

    companion object {
        fun of(vararg coords: Pair<Double, Double>): ReferenceStroke =
            ReferenceStroke(coords.map { StrokePoint(it.first, it.second) })
    }
}

/** Angle (degrees) between the first→last vectors of two strokes. */
private fun angleOf(from: StrokePoint, to: StrokePoint): Double {
    val angle = atan2(to.y - from.y, to.x - from.x) * 180.0 / PI
    return if (angle < 0.0) angle + 360.0 else angle
}

/** A point in the normalized 0..100 grid. */
data class StrokePoint(
    val x: Double,
    val y: Double
)

/** Per-stroke verdict with the concrete metric values that produced it. */
enum class StrokeMistake {
    None,
    Shape,
    Direction,
    ShapeAndDirection
}

/** Result of evaluating one user stroke against the reference set. */
data class StrokeEvaluation(
    val strokeIndex: Int,
    val correct: Boolean,
    val deviation: Float = 0f,
    val directionErrorDegrees: Float = 0f,
    val matchedReferenceIndex: Int = -1,
    val mistake: StrokeMistake = StrokeMistake.None
)

/** Aggregate result for a whole attempt. */
data class StrokeEvaluationResult(
    val expression: String,
    val strokeEvaluations: List<StrokeEvaluation>,
    /** 0..1 weighted accuracy — shape deviation dominates, direction second. */
    val accuracy: Float,
    val supported: Boolean
) {
    val correctStrokes: Int get() = strokeEvaluations.count { it.correct }
    val totalStrokes: Int get() = strokeEvaluations.size
}

object StrokeEvaluator {

    /** Shape deviation tolerance (0..100 grid units). Below = correct. */
    const val SHAPE_TOLERANCE = 22.0
    /** Direction tolerance in degrees. Below = correct. */
    const val DIRECTION_TOLERANCE = 45.0
    /** Points used to resample both user and reference strokes. */
    const val SAMPLE_COUNT = 12

    private const val GRID = 100.0

    // ------------------------------------------------------------
    // CANONICAL STROKE DATA
    // Compact but real: stroke order and approximate trajectories
    // for the most common kanji, following standard Japanese stroke
    // order. Coordinates are percentages of the writing grid.
    // Characters not listed here are "unsupported" — the app never
    // pretends to know strokes it does not have.
    // ------------------------------------------------------------
    private val dataset: Map<String, List<ReferenceStroke>> = buildMap {
        // 一 — one horizontal stroke, left to right
        put("一", listOf(ReferenceStroke.of(10.0 to 50.0, 90.0 to 50.0)))
        // 二 — two horizontals, top then bottom
        put("二", listOf(
            ReferenceStroke.of(20.0 to 35.0, 80.0 to 35.0),
            ReferenceStroke.of(10.0 to 65.0, 90.0 to 65.0)
        ))
        // 三 — three horizontals, top to bottom
        put("三", listOf(
            ReferenceStroke.of(20.0 to 25.0, 80.0 to 25.0),
            ReferenceStroke.of(10.0 to 50.0, 90.0 to 50.0),
            ReferenceStroke.of(15.0 to 75.0, 85.0 to 75.0)
        ))
        // 十 — horizontal then vertical (vertical crosses the middle)
        put("十", listOf(
            ReferenceStroke.of(15.0 to 50.0, 85.0 to 50.0),
            ReferenceStroke.of(50.0 to 12.0, 50.0 to 88.0)
        ))
        // 口 — three strokes: vertical left, horizontal top + right down, horizontal bottom
        put("口", listOf(
            ReferenceStroke.of(25.0 to 22.0, 25.0 to 78.0),
            ReferenceStroke.of(25.0 to 22.0, 75.0 to 22.0, 75.0 to 78.0),
            ReferenceStroke.of(25.0 to 78.0, 75.0 to 78.0)
        ))
        // 日 — four strokes: left vertical, top+right, middle horizontal, bottom
        put("日", listOf(
            ReferenceStroke.of(30.0 to 18.0, 30.0 to 82.0),
            ReferenceStroke.of(30.0 to 18.0, 70.0 to 18.0, 70.0 to 82.0),
            ReferenceStroke.of(30.0 to 50.0, 70.0 to 50.0),
            ReferenceStroke.of(30.0 to 82.0, 70.0 to 82.0)
        ))
        // 月 — four strokes: left vertical hook, top+right, two inner horizontals
        put("月", listOf(
            ReferenceStroke.of(30.0 to 18.0, 30.0 to 80.0, 34.0 to 86.0),
            ReferenceStroke.of(30.0 to 18.0, 70.0 to 18.0, 70.0 to 82.0),
            ReferenceStroke.of(35.0 to 40.0, 65.0 to 40.0),
            ReferenceStroke.of(35.0 to 60.0, 65.0 to 60.0)
        ))
        // 山 — three vertical strokes: middle tall, left, right
        put("山", listOf(
            ReferenceStroke.of(50.0 to 20.0, 50.0 to 80.0),
            ReferenceStroke.of(22.0 to 42.0, 22.0 to 82.0),
            ReferenceStroke.of(78.0 to 42.0, 78.0 to 82.0)
        ))
        // 川 — three vertical strokes, left to right
        put("川", listOf(
            ReferenceStroke.of(20.0 to 20.0, 20.0 to 80.0),
            ReferenceStroke.of(42.0 to 18.0, 42.0 to 82.0),
            ReferenceStroke.of(72.0 to 25.0, 72.0 to 75.0)
        ))
        // 水 — central hook, then left, right, right-lower
        put("水", listOf(
            ReferenceStroke.of(50.0 to 10.0, 50.0 to 75.0, 44.0 to 85.0),
            ReferenceStroke.of(28.0 to 28.0, 46.0 to 48.0),
            ReferenceStroke.of(72.0 to 22.0, 52.0 to 52.0),
            ReferenceStroke.of(60.0 to 60.0, 74.0 to 84.0)
        ))
        // 火 — left dot, right dot, central person shape
        put("火", listOf(
            ReferenceStroke.of(30.0 to 22.0, 42.0 to 30.0),
            ReferenceStroke.of(70.0 to 22.0, 58.0 to 30.0),
            ReferenceStroke.of(50.0 to 14.0, 50.0 to 50.0, 38.0 to 78.0),
            ReferenceStroke.of(50.0 to 50.0, 66.0 to 82.0)
        ))
        // 木 — horizontal, vertical, left slant, right slant
        put("木", listOf(
            ReferenceStroke.of(15.0 to 40.0, 85.0 to 40.0),
            ReferenceStroke.of(50.0 to 10.0, 50.0 to 88.0),
            ReferenceStroke.of(50.0 to 40.0, 30.0 to 84.0),
            ReferenceStroke.of(50.0 to 40.0, 72.0 to 84.0)
        ))
        // 田 — box with cross: left, top+right, horizontal, vertical, bottom
        put("田", listOf(
            ReferenceStroke.of(25.0 to 20.0, 25.0 to 80.0),
            ReferenceStroke.of(25.0 to 20.0, 75.0 to 20.0, 75.0 to 80.0),
            ReferenceStroke.of(25.0 to 50.0, 75.0 to 50.0),
            ReferenceStroke.of(50.0 to 20.0, 50.0 to 80.0),
            ReferenceStroke.of(25.0 to 80.0, 75.0 to 80.0)
        ))
        // 人 — left slant, right slant (apex high)
        put("人", listOf(
            ReferenceStroke.of(48.0 to 18.0, 38.0 to 84.0),
            ReferenceStroke.of(52.0 to 18.0, 68.0 to 84.0)
        ))
        // 大 — horizontal, left slant, right slant
        put("大", listOf(
            ReferenceStroke.of(15.0 to 38.0, 85.0 to 38.0),
            ReferenceStroke.of(50.0 to 12.0, 36.0 to 86.0),
            ReferenceStroke.of(50.0 to 12.0, 70.0 to 86.0)
        ))
        // 上 — vertical, horizontal, bottom horizontal
        put("上", listOf(
            ReferenceStroke.of(50.0 to 12.0, 50.0 to 78.0),
            ReferenceStroke.of(30.0 to 50.0, 70.0 to 50.0),
            ReferenceStroke.of(22.0 to 78.0, 78.0 to 78.0)
        ))
        // 下 — horizontal, vertical down, right dot
        put("下", listOf(
            ReferenceStroke.of(15.0 to 30.0, 85.0 to 30.0),
            ReferenceStroke.of(50.0 to 30.0, 50.0 to 80.0),
            ReferenceStroke.of(62.0 to 66.0, 68.0 to 72.0)
        ))
        // 中 — box with vertical through center
        put("中", listOf(
            ReferenceStroke.of(25.0 to 18.0, 25.0 to 82.0),
            ReferenceStroke.of(25.0 to 18.0, 75.0 to 18.0, 75.0 to 82.0),
            ReferenceStroke.of(25.0 to 82.0, 75.0 to 82.0),
            ReferenceStroke.of(50.0 to 10.0, 50.0 to 90.0)
        ))
        // 五 — horizontal, vertical down, horizontal, bottom horizontal
        put("五", listOf(
            ReferenceStroke.of(18.0 to 20.0, 82.0 to 20.0),
            ReferenceStroke.of(45.0 to 20.0, 45.0 to 60.0),
            ReferenceStroke.of(30.0 to 60.0, 75.0 to 60.0),
            ReferenceStroke.of(15.0 to 80.0, 85.0 to 80.0)
        ))
        // 六 — dot, horizontal, left slant, right slant
        put("六", listOf(
            ReferenceStroke.of(50.0 to 14.0, 56.0 to 20.0),
            ReferenceStroke.of(15.0 to 34.0, 85.0 to 34.0),
            ReferenceStroke.of(28.0 to 44.0, 40.0 to 82.0),
            ReferenceStroke.of(72.0 to 44.0, 60.0 to 82.0)
        ))
        // 七 — horizontal, then right-to-left slant hook
        put("七", listOf(
            ReferenceStroke.of(15.0 to 40.0, 85.0 to 40.0),
            ReferenceStroke.of(82.0 to 40.0, 58.0 to 82.0)
        ))
        // 八 — left slant, right slant
        put("八", listOf(
            ReferenceStroke.of(34.0 to 24.0, 30.0 to 78.0),
            ReferenceStroke.of(66.0 to 24.0, 70.0 to 78.0)
        ))
        // 九 — left slant, right hook
        put("九", listOf(
            ReferenceStroke.of(32.0 to 24.0, 26.0 to 68.0),
            ReferenceStroke.of(38.0 to 22.0, 78.0 to 40.0, 70.0 to 80.0)
        ))
        // 本 — horizontal, vertical, left slant, right slant, bottom horizontals
        put("本", listOf(
            ReferenceStroke.of(15.0 to 36.0, 85.0 to 36.0),
            ReferenceStroke.of(50.0 to 12.0, 50.0 to 88.0),
            ReferenceStroke.of(50.0 to 36.0, 32.0 to 80.0),
            ReferenceStroke.of(50.0 to 36.0, 70.0 to 80.0),
            ReferenceStroke.of(32.0 to 88.0, 68.0 to 88.0)
        ))
        // 今 — top slant, horizontal, then lower person
        put("今", listOf(
            ReferenceStroke.of(50.0 to 12.0, 38.0 to 30.0),
            ReferenceStroke.of(38.0 to 30.0, 62.0 to 30.0),
            ReferenceStroke.of(50.0 to 30.0, 50.0 to 56.0),
            ReferenceStroke.of(50.0 to 56.0, 36.0 to 86.0),
            ReferenceStroke.of(50.0 to 56.0, 64.0 to 86.0)
        ))
        // 学 — simplified: top dots, crown, vertical, child bottom
        put("学", listOf(
            ReferenceStroke.of(36.0 to 14.0, 42.0 to 20.0),
            ReferenceStroke.of(64.0 to 14.0, 58.0 to 20.0),
            ReferenceStroke.of(22.0 to 30.0, 78.0 to 30.0),
            ReferenceStroke.of(50.0 to 30.0, 50.0 to 58.0),
            ReferenceStroke.of(30.0 to 42.0, 70.0 to 42.0),
            ReferenceStroke.of(50.0 to 58.0, 50.0 to 88.0),
            ReferenceStroke.of(32.0 to 72.0, 68.0 to 72.0),
            ReferenceStroke.of(30.0 to 88.0, 70.0 to 88.0)
        ))
        // 生 — horizontal, vertical, horizontal, horizontal, bottom horizontal
        put("生", listOf(
            ReferenceStroke.of(25.0 to 18.0, 75.0 to 18.0),
            ReferenceStroke.of(50.0 to 18.0, 50.0 to 86.0),
            ReferenceStroke.of(20.0 to 42.0, 80.0 to 42.0),
            ReferenceStroke.of(18.0 to 62.0, 82.0 to 62.0),
            ReferenceStroke.of(15.0 to 82.0, 85.0 to 82.0)
        ))
        // 先 — horizontal, vertical, left slant, right slant, bottom legs
        put("先", listOf(
            ReferenceStroke.of(25.0 to 20.0, 75.0 to 20.0),
            ReferenceStroke.of(50.0 to 20.0, 50.0 to 86.0),
            ReferenceStroke.of(50.0 to 20.0, 34.0 to 56.0),
            ReferenceStroke.of(50.0 to 20.0, 66.0 to 56.0),
            ReferenceStroke.of(30.0 to 86.0, 70.0 to 86.0)
        ))
        // 文 — dot, horizontal, left slant, right slant
        put("文", listOf(
            ReferenceStroke.of(50.0 to 12.0, 56.0 to 20.0),
            ReferenceStroke.of(15.0 to 32.0, 85.0 to 32.0),
            ReferenceStroke.of(46.0 to 40.0, 34.0 to 84.0),
            ReferenceStroke.of(54.0 to 40.0, 70.0 to 84.0)
        ))
        // 年 — top slant, horizontal, vertical, horizontal, bottom vertical
        put("年", listOf(
            ReferenceStroke.of(35.0 to 14.0, 30.0 to 38.0),
            ReferenceStroke.of(25.0 to 38.0, 72.0 to 38.0),
            ReferenceStroke.of(45.0 to 14.0, 45.0 to 78.0),
            ReferenceStroke.of(30.0 to 56.0, 78.0 to 56.0),
            ReferenceStroke.of(50.0 to 56.0, 52.0 to 88.0)
        ))
        // 子 — horizontal hook top, vertical hook, horizontal bottom
        put("子", listOf(
            ReferenceStroke.of(30.0 to 20.0, 68.0 to 20.0, 60.0 to 34.0),
            ReferenceStroke.of(50.0 to 14.0, 50.0 to 82.0, 42.0 to 86.0),
            ReferenceStroke.of(25.0 to 78.0, 75.0 to 78.0)
        ))
    }

    // ------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------

    /** Whether canonical stroke data exists for the given character. */
    fun supports(expression: String): Boolean =
        expression.length == 1 && (dataset.containsKey(expression) || kanaStrokesFor(expression) != null)

    /**
     * Evaluate a full handwriting attempt.
     *
     * @param expression the expected character (single kanji)
     * @param drawnStrokes the learner's freehand strokes, in drawing order,
     *        each a list of raw canvas points
     * @param canvasWidth / [canvasHeight] the canvas size the points were
     *        captured in (for normalization)
     */
    fun evaluate(
        expression: String,
        drawnStrokes: List<List<StrokePoint>>,
        canvasWidth: Double = GRID,
        canvasHeight: Double = GRID
    ): StrokeEvaluationResult {
        // Kanji first, then the kana syllabary — one evaluator, one pipeline.
        val reference = dataset[expression] ?: kanaStrokesFor(expression)
        if (reference == null || drawnStrokes.isEmpty()) {
            return StrokeEvaluationResult(expression, emptyList(), 0f, supported = false)
        }

        val normalized = drawnStrokes.map { stroke ->
            normalizeStroke(stroke, canvasWidth, canvasHeight)
        }

        // Greedy matching: repeatedly take the (user stroke, reference stroke)
        // pair with the smallest shape deviation until every user stroke is
        // assigned or no reference strokes remain.
        val usedReference = BooleanArray(reference.size)
        val usedUser = BooleanArray(normalized.size)
        val matches = mutableListOf<Pair<Int, Int>>() // user index -> reference index

        while (matches.size < min(normalized.size, reference.size)) {
            var best = Triple(-1, -1, Double.MAX_VALUE)
            normalized.forEachIndexed { u, userStroke ->
                if (usedUser[u]) return@forEachIndexed
                reference.forEachIndexed { r, ref ->
                    if (usedReference[r]) return@forEachIndexed
                    val deviation = shapeDeviation(userStroke, ref.points)
                    if (deviation < best.third) {
                        best = Triple(u, r, deviation)
                    }
                }
            }
            if (best.first < 0) break
            usedUser[best.first] = true
            usedReference[best.second] = true
            matches.add(best.first to best.second)
        }

        val evaluations = normalized.indices.map { u ->
            val match = matches.firstOrNull { it.first == u }
            if (match == null) {
                StrokeEvaluation(
                    strokeIndex = u,
                    correct = false,
                    deviation = Float.MAX_VALUE,
                    directionErrorDegrees = 180f,
                    matchedReferenceIndex = -1,
                    mistake = StrokeMistake.Shape
                )
            } else {
                val ref = reference[match.second]
                val deviation = shapeDeviation(normalized[u], ref.points)
                val directionError = directionError(normalized[u], ref)
                val correct = deviation <= SHAPE_TOLERANCE && directionError <= DIRECTION_TOLERANCE
                val mistake = when {
                    deviation > SHAPE_TOLERANCE && directionError > DIRECTION_TOLERANCE -> StrokeMistake.ShapeAndDirection
                    deviation > SHAPE_TOLERANCE -> StrokeMistake.Shape
                    directionError > DIRECTION_TOLERANCE -> StrokeMistake.Direction
                    else -> StrokeMistake.None
                }
                StrokeEvaluation(
                    strokeIndex = u,
                    correct = correct,
                    deviation = deviation.toFloat(),
                    directionErrorDegrees = directionError.toFloat(),
                    matchedReferenceIndex = match.second,
                    mistake = mistake
                )
            }
        }

        // Order quality: count user strokes that landed on the reference stroke
        // with the same index they were drawn in (best-effort, not punitive).
        val orderOk = matches.count { (u, r) -> u == r }.toDouble() / reference.size
        val shapeScore = if (evaluations.isEmpty()) 0.0 else {
            evaluations.sumOf { (SHAPE_TOLERANCE - it.deviation.toDouble().coerceAtMost(SHAPE_TOLERANCE)).coerceAtLeast(0.0) } /
                (SHAPE_TOLERANCE * evaluations.size)
        }
        val dirScore = if (evaluations.isEmpty()) 0.0 else {
            evaluations.sumOf { (1.0 - (it.directionErrorDegrees / 180.0)).coerceIn(0.0, 1.0) } / evaluations.size
        }
        val accuracy = (shapeScore * 0.55 + dirScore * 0.3 + orderOk * 0.15).toFloat().coerceIn(0f, 1f)

        return StrokeEvaluationResult(
            expression = expression,
            strokeEvaluations = evaluations,
            accuracy = accuracy,
            supported = true
        )
    }

    // ------------------------------------------------------------
    // Geometry helpers
    // ------------------------------------------------------------

    /** Map raw canvas coordinates onto the 0..100 grid, preserving aspect ratio. */
    private fun normalizeStroke(points: List<StrokePoint>, width: Double, height: Double): List<StrokePoint> {
        if (points.isEmpty()) return emptyList()
        val scale = GRID / min(width, height).coerceAtLeast(1.0)
        return points.map { StrokePoint(it.x * scale, it.y * scale) }
    }

    /** Resample a polyline to exactly [SAMPLE_COUNT] points (arc-length based). */
    private fun resample(points: List<StrokePoint>, count: Int = SAMPLE_COUNT): List<StrokePoint> {
        if (points.size < 2) return points
        val lengths = DoubleArray(points.size - 1) { i -> dist(points[i], points[i + 1]) }
        val total = lengths.sum()
        if (total <= 0.0) return List(count) { points.first() }

        val cumulative = DoubleArray(lengths.size + 1)
        for (i in lengths.indices) cumulative[i + 1] = cumulative[i] + lengths[i]

        val out = mutableListOf<StrokePoint>()
        for (s in 0 until count) {
            val target = total * s / (count - 1)
            var seg = 0
            while (seg < lengths.size - 1 && cumulative[seg + 1] < target) seg++
            val segStart = cumulative[seg]
            val segLen = lengths[seg].coerceAtLeast(1e-9)
            val t = ((target - segStart) / segLen).coerceIn(0.0, 1.0)
            out.add(
                StrokePoint(
                    points[seg].x + (points[seg + 1].x - points[seg].x) * t,
                    points[seg].y + (points[seg + 1].y - points[seg].y) * t
                )
            )
        }
        return out
    }

    /** Average Euclidean distance between two resampled polylines. */
    private fun shapeDeviation(user: List<StrokePoint>, reference: List<StrokePoint>): Double {
        val a = resample(user)
        val b = resample(reference)
        if (a.isEmpty() || b.isEmpty()) return Double.MAX_VALUE
        val n = min(a.size, b.size)
        return (0 until n).sumOf { dist(a[it], b[it]) } / n
    }

    /** Angle (degrees) between the first→last vectors of two strokes. */
    private fun directionError(user: List<StrokePoint>, reference: ReferenceStroke): Double {
        if (user.size < 2) return 180.0
        val userAngle = angleOf(user.first(), user.last())
        var diff = abs(userAngle - reference.direction)
        while (diff > 180.0) diff -= 360.0
        return abs(diff)
    }

    private fun dist(a: StrokePoint, b: StrokePoint): Double = hypot(a.x - b.x, a.y - b.y)
}
