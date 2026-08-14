package ua.syt0r.kanji

import androidx.compose.ui.graphics.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ua.syt0r.kanji.core.stroke_evaluator.DefaultKanjiStrokeEvaluator
import ua.syt0r.kanji.core.stroke_evaluator.StrokeClassification
import ua.syt0r.kanji.core.stroke_evaluator.StrokeEvaluationConfig
import ua.syt0r.kanji.core.stroke_evaluator.StrokeSequenceEvaluator
import ua.syt0r.kanji.core.stroke_evaluator.StrokeSequenceIssueType

class StrokeEvaluatorTest {

    private val evaluator = DefaultKanjiStrokeEvaluator()
    private val sequenceEvaluator = StrokeSequenceEvaluator(evaluator)

    // ------------------------------------------------------------
    // Per-stroke scoring
    // ------------------------------------------------------------

    @Test
    fun identicalStroke_isCorrect() {
        val stroke = horizontalLine(0f, 0f, 100f)
        val result = evaluator.evaluate(stroke, stroke)
        assertEquals(StrokeClassification.Correct, result.classification)
        assertTrue(result.metrics.directionAccuracy > 0.95f, "direction $result")
        assertTrue(result.metrics.positionAccuracy > 0.9f, "position $result")
        assertTrue(result.metrics.lengthAccuracy > 0.95f, "length $result")
    }

    @Test
    fun slightlyOffsetStroke_withinDeadBand_isCorrect() {
        val expected = horizontalLine(0f, 0f, 100f)
        val drawn = horizontalLine(3f, 2f, 100f) // a few px off — still correct
        val result = evaluator.evaluate(expected, drawn)
        assertEquals(StrokeClassification.Correct, result.classification)
    }

    @Test
    fun wrongDirection_dropsDirectionAccuracy() {
        val expected = horizontalLine(0f, 0f, 100f)
        val drawn = verticalLine(0f, 0f, 100f)
        val result = evaluator.evaluate(expected, drawn)
        assertTrue(result.metrics.directionAccuracy < 0.3f, "direction should be low, was ${result.metrics.directionAccuracy}")
        assertEquals(StrokeClassification.Incorrect, result.classification)
    }

    @Test
    fun wrongPosition_dropsPositionAccuracy() {
        val expected = horizontalLine(0f, 0f, 100f)
        val drawn = horizontalLine(200f, 200f, 100f)
        val result = evaluator.evaluate(expected, drawn)
        assertTrue(result.metrics.positionAccuracy < 0.3f, "position should be low, was ${result.metrics.positionAccuracy}")
        assertTrue(result.metrics.directionAccuracy > 0.9f, "direction unaffected")
    }

    @Test
    fun tooShortStroke_dropsLengthAccuracy() {
        val expected = horizontalLine(0f, 0f, 100f)
        val drawn = horizontalLine(0f, 0f, 20f)
        val result = evaluator.evaluate(expected, drawn)
        assertTrue(result.metrics.lengthAccuracy < 0.3f, "length was ${result.metrics.lengthAccuracy}")
    }

    @Test
    fun scaledDownStroke_stillCorrect() {
        // Position is normalized to the reference bounding box, so drawing the
        // same shape much smaller (or larger) must not fail.
        val expected = horizontalLine(0f, 0f, 100f)
        val drawn = horizontalLine(0f, 0f, 25f)
        val result = evaluator.evaluate(expected, drawn)
        assertEquals(StrokeClassification.Correct, result.classification)
    }

    @Test
    fun scaledUpStroke_stillCorrect() {
        val expected = horizontalLine(0f, 0f, 50f)
        val drawn = horizontalLine(0f, 0f, 250f)
        val result = evaluator.evaluate(expected, drawn)
        assertEquals(StrokeClassification.Correct, result.classification)
    }

    @Test
    fun curvatureDifference_dropsCurvatureAccuracy() {
        val expected = arc(radius = 100f, sweep = 90f)
        val drawn = arc(radius = 100f, sweep = 270f) // much rounder
        val result = evaluator.evaluate(expected, drawn)
        assertTrue(
            result.metrics.curvatureAccuracy < 0.6f,
            "curvature was ${result.metrics.curvatureAccuracy}"
        )
    }

    @Test
    fun strictnessPresets_changeClassification() {
        // A stroke that is Correct under Normal tolerances should fail under Exam.
        val expected = horizontalLine(0f, 0f, 100f)
        val drawn = horizontalLine(6f, 4f, 96f) // small offset + shortened

        val normal = evaluator.evaluate(expected, drawn, StrokeEvaluationConfig.Normal)
        val exam = evaluator.evaluate(expected, drawn, StrokeEvaluationConfig.Exam)

        assertEquals(StrokeClassification.Correct, normal.classification)
        assertFalse(
            exam.classification == StrokeClassification.Correct,
            "exam should be stricter than normal (normal=$normal exam=$exam)"
        )
    }

    @Test
    fun areStrokesSimilar_delegatesToScoredEvaluation() {
        val identical = horizontalLine(0f, 0f, 100f)
        assertTrue(evaluator.areStrokesSimilar(identical, identical))

        val wrong = verticalLine(0f, 0f, 100f)
        assertFalse(evaluator.areStrokesSimilar(identical, wrong))
    }

    // ------------------------------------------------------------
    // Sequence analysis
    // ------------------------------------------------------------

    @Test
    fun correctSequence_hasNoIssues() {
        // "T" shape: horizontal stroke, then vertical stroke.
        val expected = listOf(
            horizontalLine(0f, 0f, 100f),
            verticalLine(50f, 0f, 100f)
        )
        val drawn = listOf(
            horizontalLine(0f, 0f, 100f),
            verticalLine(50f, 0f, 100f)
        )

        val result = sequenceEvaluator.evaluate(expected, drawn)
        assertTrue(result.issues.isEmpty(), "issues: ${result.issues}")
        assertTrue(result.overallAccuracy > 0.85f, "accuracy ${result.overallAccuracy}")
    }

    @Test
    fun swappedStrokes_detectsWrongOrder() {
        val expected = listOf(
            horizontalLine(0f, 0f, 100f),
            verticalLine(50f, 0f, 100f)
        )
        val drawn = listOf(
            verticalLine(50f, 0f, 100f), // vertical first — wrong order
            horizontalLine(0f, 0f, 100f)
        )

        val result = sequenceEvaluator.evaluate(expected, drawn)
        assertTrue(
            result.issues.any { it.type == StrokeSequenceIssueType.WrongOrder },
            "expected WrongOrder issue, got ${result.issues}"
        )
    }

    @Test
    fun missingStroke_isDetected() {
        val expected = listOf(
            horizontalLine(0f, 0f, 100f),
            verticalLine(50f, 0f, 100f)
        )
        val drawn = listOf(horizontalLine(0f, 0f, 100f))

        val result = sequenceEvaluator.evaluate(expected, drawn)
        assertTrue(
            result.issues.any { it.type == StrokeSequenceIssueType.MissingStroke },
            "expected MissingStroke issue, got ${result.issues}"
        )
    }

    @Test
    fun extraStroke_isDetected() {
        val expected = listOf(horizontalLine(0f, 0f, 100f))
        val drawn = listOf(
            horizontalLine(0f, 0f, 100f),
            diagonalLine(0f, 0f, 100f, 100f)
        )

        val result = sequenceEvaluator.evaluate(expected, drawn)
        assertTrue(
            result.issues.any { it.type == StrokeSequenceIssueType.ExtraStroke },
            "expected ExtraStroke issue, got ${result.issues}"
        )
        assertTrue(
            result.overallAccuracy < 0.99f,
            "extra strokes must reduce overall accuracy, was ${result.overallAccuracy}"
        )
    }

    @Test
    fun fastAndSlowWriting_areEquivalent() {
        // Timing is deliberately ignored — the same path must score identically
        // regardless of input speed.
        val expected = horizontalLine(0f, 0f, 100f)
        val drawn = horizontalLine(0f, 0f, 100f)
        assertEquals(
            evaluator.evaluate(expected, drawn).metrics.overallScore,
            evaluator.evaluate(expected, drawn).metrics.overallScore
        )
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private fun path(vararg points: Pair<Float, Float>): Path = Path().apply {
        moveTo(points[0].first, points[0].second)
        points.drop(1).forEach { (x, y) -> lineTo(x, y) }
    }

    private fun horizontalLine(x: Float, y: Float, length: Float): Path =
        path(x to y, (x + length) to y)

    private fun verticalLine(x: Float, y: Float, length: Float): Path =
        path(x to y, x to (y + length))

    private fun diagonalLine(x1: Float, y1: Float, x2: Float, y2: Float): Path =
        path(x1 to y1, x2 to y2)

    /** Arc approximated with short straight segments. */
    private fun arc(radius: Float, sweep: Float): Path {
        val segments = 12
        val points = (0..segments).map { i ->
            val angle = Math.toRadians(sweep * i / segments.toDouble())
            (radius * kotlin.math.cos(angle)).toFloat() to
                (radius * kotlin.math.sin(angle)).toFloat()
        }
        return path(*points.toTypedArray())
    }
}
