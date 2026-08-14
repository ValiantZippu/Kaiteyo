package ua.syt0r.kanji.desktop.engine.jdata.engine

import androidx.compose.ui.graphics.Path
import ua.syt0r.kanji.core.stroke_evaluator.DefaultKanjiStrokeEvaluator
import ua.syt0r.kanji.core.stroke_evaluator.KanjiStrokeEvaluator
import ua.syt0r.kanji.core.stroke_evaluator.StrokeEvaluation
import ua.syt0r.kanji.core.stroke_evaluator.StrokeEvaluationConfig
import ua.syt0r.kanji.core.stroke_evaluator.StrokeSequenceEvaluation
import ua.syt0r.kanji.core.stroke_evaluator.StrokeSequenceEvaluator
import ua.syt0r.kanji.core.stroke_evaluator.StrokeSequenceIssue
import ua.syt0r.kanji.core.stroke_evaluator.StrokeSequenceIssueType
import ua.syt0r.kanji.desktop.engine.jdata.model.StrokeSet

// ============================================================
// WRITING-EVALUATION BRIDGE
// Connects the platform's canonical stroke data (StrokeSet with SVG
// path strings from KanjiVG) to the app's existing stroke evaluators
// (ua.syt0r.kanji.core.stroke_evaluator.*). The core evaluators speak
// androidx.compose.ui.graphics.Path; this bridge is the only place that
// conversion happens, so the rest of the platform stays Compose-free.
//
// Strictness mapping (platform → core scoring presets):
//   Relaxed → StrokeEvaluationConfig.Normal
//   Normal  → StrokeEvaluationConfig.Hard
//   Exam    → StrokeEvaluationConfig.Exam
//
// The structural checks (stroke count/order rules from
// [StrokeSequenceValidator]) run first; the geometric evaluation
// ([StrokeSequenceEvaluator]) runs when the reference paths exist.
// ============================================================

class StrokeEvaluationBridge(
    private val strokeEvaluator: KanjiStrokeEvaluator = DefaultKanjiStrokeEvaluator()
) {

    private val sequenceEvaluator by lazy { StrokeSequenceEvaluator(strokeEvaluator) }

    /** Everything the writing UI needs for one character attempt. */
    data class BridgeResult(
        val character: String,
        val strictness: WritingStrictness,
        val expectedCount: Int,
        val drawnCount: Int,
        /** False when the reference has no path geometry (count-only mode). */
        val geometryAvailable: Boolean,
        /** Structural problems (e.g. wrong stroke count). */
        val structuralIssues: List<String>,
        /** Geometric per-stroke + sequence result; null when geometry unavailable. */
        val sequence: StrokeSequenceEvaluation?
    ) {
        val perStroke: List<StrokeEvaluation> get() = sequence?.perStroke ?: emptyList()
        val issues: List<StrokeSequenceIssue> get() = sequence?.issues ?: emptyList()
        val overallAccuracy: Float get() = sequence?.overallAccuracy ?: 0f

        fun accuracyPercent(): Int = (overallAccuracy * 100).toInt().coerceIn(0, 100)

        /**
         * Accepted = structurally sound AND geometry (when available) above the
         * threshold AND — at Exam strictness — no stroke-order deviation. The
         * core sequence evaluator does not penalize correct strokes drawn in the
         * wrong order, so the platform enforces order at exam level itself.
         */
        val accepted: Boolean
            get() = structuralIssues.isEmpty() &&
                (sequence == null || sequence.overallAccuracy >= acceptanceThreshold(strictness)) &&
                (strictness != WritingStrictness.Exam ||
                    sequence?.issues?.none { it.type == StrokeSequenceIssueType.WrongOrder } != false)
    }

    /**
     * Evaluates a drawn character against a canonical [StrokeSet]. The drawn
     * strokes are SVG path strings in the order the user drew them.
     */
    fun evaluate(set: StrokeSet, drawnPaths: List<String>, strictness: WritingStrictness): BridgeResult {
        val config = configFor(strictness)
        val expectedPaths = set.strokes.filter { it.path != null }.map { it.path!! }

        val structuralIssues = mutableListOf<String>()
        if (drawnPaths.size != set.strokeCount) {
            structuralIssues += "Stroke count mismatch: drew ${drawnPaths.size}, expected ${set.strokeCount}"
        }
        val sequence = run {
            val geometryAvailable = expectedPaths.size == set.strokeCount && drawnPaths.isNotEmpty()
            if (!geometryAvailable) return@run null
            val expected = expectedPaths.mapNotNull { SvgPathConverter.toComposePath(it) }
            val drawn = drawnPaths.mapNotNull { SvgPathConverter.toComposePath(it) }
            if (expected.size != expectedPaths.size || drawn.size != drawnPaths.size) return@run null
            sequenceEvaluator.evaluate(expected, drawn, config)
        }

        return BridgeResult(
            character = set.character,
            strictness = strictness,
            expectedCount = set.strokeCount,
            drawnCount = drawnPaths.size,
            geometryAvailable = sequence != null,
            structuralIssues = structuralIssues,
            sequence = sequence
        )
    }

    /** Single-stroke comparison against a reference path (for live feedback). */
    fun evaluateStroke(
        expectedPath: String,
        drawnPath: String,
        strictness: WritingStrictness
    ): StrokeEvaluation? {
        val expected = SvgPathConverter.toComposePath(expectedPath) ?: return null
        val drawn = SvgPathConverter.toComposePath(drawnPath) ?: return null
        return strokeEvaluator.evaluate(expected, drawn, configFor(strictness))
    }

    /** Whole-sequence evaluation over raw path lists (no StrokeSet needed). */
    fun evaluateSequence(
        expectedPaths: List<String>,
        drawnPaths: List<String>,
        strictness: WritingStrictness
    ): StrokeSequenceEvaluation? {
        val expected = expectedPaths.mapNotNull { SvgPathConverter.toComposePath(it) }
        val drawn = drawnPaths.mapNotNull { SvgPathConverter.toComposePath(it) }
        if (expected.isEmpty() || drawn.isEmpty()) return null
        return sequenceEvaluator.evaluate(expected, drawn, configFor(strictness))
    }

    companion object {
        fun configFor(strictness: WritingStrictness): StrokeEvaluationConfig = when (strictness) {
            WritingStrictness.Relaxed -> StrokeEvaluationConfig.Normal
            WritingStrictness.Normal -> StrokeEvaluationConfig.Hard
            WritingStrictness.Exam -> StrokeEvaluationConfig.Exam
        }

        /** Minimum overall accuracy for a set to be accepted under each strictness. */
        fun acceptanceThreshold(strictness: WritingStrictness): Float = when (strictness) {
            WritingStrictness.Relaxed -> 0.5f
            WritingStrictness.Normal -> 0.65f
            WritingStrictness.Exam -> 0.8f
        }
    }
}

/**
 * Converts SVG path `d` data into a Compose [Path]. Supports the full command
 * set KanjiVG emits: M/m L/l H/h V/v C/c S/s Q/q T/t A/a Z/z (absolute and
 * relative), with proper S/T control-point reflection. Returns null on
 * malformed data or unknown commands — callers treat that as "no geometry".
 */
object SvgPathConverter {

    private val tokenRegex = Regex("-?\\d+\\.?\\d*(?:[eE][-+]?\\d+)?|[A-Za-z]")

    fun toComposePath(pathData: String): Path? {
        val tokens = tokenRegex.findAll(pathData).map { it.value }.toList()
        if (tokens.isEmpty()) return null
        val path = Path()
        var i = 0
        var command = 'M'
        var cursorX = 0f
        var cursorY = 0f
        var startX = 0f
        var startY = 0f
        var lastControl: Pair<Float, Float>? = null

        fun isNumber(token: String): Boolean = token.first().isDigit() || token.first() == '-'
        fun next(): Float = tokens[i++].toFloat()
        fun reflectedControl(): Pair<Float, Float> =
            lastControl?.let { (cx, cy) -> (2 * cursorX - cx) to (2 * cursorY - cy) }
                ?: (cursorX to cursorY)

        return try {
            while (i < tokens.size) {
                val token = tokens[i]
                if (!isNumber(token)) {
                    command = token[0]
                    i++
                    continue
                }
                when (command) {
                    'M' -> {
                        val x = next(); val y = next()
                        path.moveTo(x, y); cursorX = x; cursorY = y; startX = x; startY = y
                        lastControl = null
                        command = 'L' // extra coordinate pairs after M are implicit lineto (SVG spec)
                    }
                    'm' -> {
                        val x = next(); val y = next()
                        path.relativeMoveTo(x, y); cursorX += x; cursorY += y; startX = cursorX; startY = cursorY
                        lastControl = null
                        command = 'l'
                    }
                    'L' -> { val x = next(); val y = next(); path.lineTo(x, y); cursorX = x; cursorY = y }
                    'l' -> { val x = next(); val y = next(); path.relativeLineTo(x, y); cursorX += x; cursorY += y }
                    'H' -> { val x = next(); path.lineTo(x, cursorY); cursorX = x }
                    'h' -> { val x = next(); path.relativeLineTo(x, 0f); cursorX += x }
                    'V' -> { val y = next(); path.lineTo(cursorX, y); cursorY = y }
                    'v' -> { val y = next(); path.relativeLineTo(0f, y); cursorY += y }
                    'C' -> {
                        val x1 = next(); val y1 = next(); val x2 = next(); val y2 = next(); val x3 = next(); val y3 = next()
                        path.cubicTo(x1, y1, x2, y2, x3, y3)
                        lastControl = x2 to y2; cursorX = x3; cursorY = y3
                    }
                    'c' -> {
                        val x1 = next(); val y1 = next(); val x2 = next(); val y2 = next(); val x3 = next(); val y3 = next()
                        path.relativeCubicTo(x1, y1, x2, y2, x3, y3)
                        lastControl = (cursorX + x2) to (cursorY + y2)
                        cursorX += x3; cursorY += y3
                    }
                    'S' -> {
                        val x2 = next(); val y2 = next(); val x3 = next(); val y3 = next()
                        val control1 = reflectedControl()
                        path.cubicTo(control1.first, control1.second, x2, y2, x3, y3)
                        lastControl = x2 to y2; cursorX = x3; cursorY = y3
                    }
                    's' -> {
                        val x2 = next(); val y2 = next(); val x3 = next(); val y3 = next()
                        val control1 = reflectedControl()
                        val control2 = (cursorX + x2) to (cursorY + y2)
                        val end = (cursorX + x3) to (cursorY + y3)
                        path.cubicTo(control1.first, control1.second, control2.first, control2.second, end.first, end.second)
                        lastControl = control2; cursorX = end.first; cursorY = end.second
                    }
                    'Q' -> {
                        val x1 = next(); val y1 = next(); val x2 = next(); val y2 = next()
                        path.quadraticBezierTo(x1, y1, x2, y2)
                        lastControl = x1 to y1; cursorX = x2; cursorY = y2
                    }
                    'q' -> {
                        val x1 = next(); val y1 = next(); val x2 = next(); val y2 = next()
                        path.relativeQuadraticBezierTo(x1, y1, x2, y2)
                        lastControl = (cursorX + x1) to (cursorY + y1)
                        cursorX += x2; cursorY += y2
                    }
                    'T' -> {
                        val x2 = next(); val y2 = next()
                        val control = reflectedControl()
                        path.quadraticBezierTo(control.first, control.second, x2, y2)
                        lastControl = control; cursorX = x2; cursorY = y2
                    }
                    't' -> {
                        val x2 = next(); val y2 = next()
                        val control = reflectedControl()
                        val end = (cursorX + x2) to (cursorY + y2)
                        path.quadraticBezierTo(control.first, control.second, end.first, end.second)
                        lastControl = control; cursorX = end.first; cursorY = end.second
                    }
                    'A' -> {
                        // Arc: rx ry rot large-arc sweep x y — approximate the endpoint with a line.
                        next(); next(); next(); next(); next()
                        val x = next(); val y = next()
                        path.lineTo(x, y); cursorX = x; cursorY = y
                    }
                    'a' -> {
                        next(); next(); next(); next(); next()
                        val x = next(); val y = next()
                        path.relativeLineTo(x, y); cursorX += x; cursorY += y
                    }
                    'Z', 'z' -> {
                        path.close()
                        cursorX = startX; cursorY = startY; lastControl = null
                        // A coordinate pair after Z starts a new subpath (implicit
                        // moveto) — switching commands here also guarantees the loop
                        // advances instead of re-entering the Z branch forever.
                        command = 'M'
                    }
                    else -> return null
                }
            }
            path
        } catch (t: Throwable) {
            null
        }
    }
}
