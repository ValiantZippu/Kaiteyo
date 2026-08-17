package ua.syt0r.kanji.desktop.game.activity

/**
 * In-world writing (spec §57-59): trace a kana on a chalkboard / writing desk
 * and the world rewards the attempt. Evaluation is deliberately lenient —
 * casual game interaction must never depend on perfect handwriting (spec
 * §59) — so the evaluator scores coverage of the trace, not penmanship.
 */
data class StrokePoint(val x: Float, val y: Float)

typealias Stroke = List<StrokePoint>

data class WritingResult(
    /** Fraction of the sampling grid the strokes covered (0..1). */
    val coverage: Float,
    val pass: Boolean
)

/** Pure stroke evaluation — unit-tested, no UI or audio involved. */
object WritingEvaluator {

    /** Coverage threshold for a pass (lenient on purpose). */
    const val PASS_COVERAGE = 0.22f

    fun evaluate(strokes: List<Stroke>, gridSize: Int = 24): WritingResult {
        if (strokes.isEmpty()) return WritingResult(0f, pass = false)
        val cells = gridSize * gridSize
        val touched = mutableSetOf<Int>()
        for (stroke in strokes) {
            if (stroke.size < 2) continue
            for (i in 0 until stroke.size - 1) {
                val a = stroke[i]
                val b = stroke[i + 1]
                val length = distance(a, b)
                if (length <= 0f) continue
                val steps = (length * gridSize).toInt().coerceAtLeast(1)
                for (s in 0..steps) {
                    val t = s.toFloat() / steps
                    val x = a.x + (b.x - a.x) * t
                    val y = a.y + (b.y - a.y) * t
                    val gx = (x * gridSize).toInt().coerceIn(0, gridSize - 1)
                    val gy = (y * gridSize).toInt().coerceIn(0, gridSize - 1)
                    touched.add(gy * gridSize + gx)
                }
            }
        }
        val coverage = touched.size.toFloat() / cells
        return WritingResult(coverage, pass = coverage >= PASS_COVERAGE)
    }

    private fun distance(a: StrokePoint, b: StrokePoint): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
