package ua.syt0r.kanji.core.stroke_evaluator

import androidx.compose.ui.graphics.Path
import ua.syt0r.kanji.core.PathApproximation
import ua.syt0r.kanji.core.PointF
import ua.syt0r.kanji.core.approximateEvenly
import ua.syt0r.kanji.core.center
import ua.syt0r.kanji.core.decreaseAll
import ua.syt0r.kanji.core.euclDistance
import ua.syt0r.kanji.core.logger.Logger
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Stroke evaluator with configurable scoring. Compares a drawn stroke against a
 * reference stroke on four independent axes — direction, position, length and
 * curvature — and combines them with the weights from [StrokeEvaluationConfig].
 *
 * Position is normalized against the reference stroke's own bounding box, so the
 * same tolerances work at any canvas size (small kanji, large kanji, tablet vs
 * phone) and for fast vs slow input (timing is deliberately ignored).
 */
class DefaultKanjiStrokeEvaluator : KanjiStrokeEvaluator {

    override fun evaluate(
        expected: Path,
        drawn: Path,
        config: StrokeEvaluationConfig
    ): StrokeEvaluation {
        val expectedApprox = expected.approximateEvenly(config.interpolationPoints)
            as? PathApproximation.Success
            ?: return failedEvaluation()
        val drawnApprox = drawn.approximateEvenly(config.interpolationPoints)
            as? PathApproximation.Success
            ?: return failedEvaluation()

        val metrics = StrokeMetrics(
            directionAccuracy = directionAccuracy(expectedApprox.points, drawnApprox.points, config),
            positionAccuracy = positionAccuracy(expectedApprox.points, drawnApprox.points, config),
            lengthAccuracy = lengthAccuracy(expectedApprox.length, drawnApprox.length, config),
            curvatureAccuracy = curvatureAccuracy(expectedApprox.points, drawnApprox.points, config)
        )

        val classification = when {
            metrics.overallScore >= config.correctThreshold -> StrokeClassification.Correct
            metrics.overallScore >= config.almostThreshold -> StrokeClassification.AlmostCorrect
            else -> StrokeClassification.Incorrect
        }

        Logger.d(
            "stroke score[${metrics.accuracyPercent()}] dir[${metrics.directionAccuracy}] " +
                "pos[${metrics.positionAccuracy}] len[${metrics.lengthAccuracy}] " +
                "curv[${metrics.curvatureAccuracy}] -> $classification"
        )

        return StrokeEvaluation(classification = classification, metrics = metrics)
    }

    private fun directionAccuracy(
        expected: List<PointF>,
        drawn: List<PointF>,
        config: StrokeEvaluationConfig
    ): Float {
        val segmentCount = expected.size - 1
        if (segmentCount < 1) return 0f

        var sum = 0f
        for (i in 0 until segmentCount) {
            val expectedAngle = segmentAngle(expected[i], expected[i + 1])
            val drawnAngle = segmentAngle(drawn[i], drawn[i + 1])
            sum += angularAccuracy(expectedAngle, drawnAngle, config)
        }
        return sum / segmentCount
    }

    private fun positionAccuracy(
        expected: List<PointF>,
        drawn: List<PointF>,
        config: StrokeEvaluationConfig
    ): Float {
        val expectedCenter = expected.center()
        val drawnCenter = drawn.center()

        val centeredExpected = expected.decreaseAll(expectedCenter)
        val centeredDrawn = drawn.decreaseAll(drawnCenter)

        val expectedSize = boundingBoxDiagonal(centeredExpected)
        if (expectedSize <= 0f) return 1f

        // Scale the drawn stroke to the reference bounding box so size
        // differences don't inflate the positional error.
        val drawnSize = boundingBoxDiagonal(centeredDrawn)
        val scale = if (drawnSize <= 0f) 1f else expectedSize / drawnSize
        val scaledDrawn = centeredDrawn.map { PointF(it.x * scale, it.y * scale) }

        val meanDistance = centeredExpected.zip(scaledDrawn)
            .map { (a, b) -> euclDistance(a, b) }
            .sum() / centeredExpected.size

        val deadBand = expectedSize * config.positionDeadBandFraction
        val normalizedError = meanDistance / deadBand
        val accuracy = 1f - min(1f, normalizedError / config.maxPositionError)
        return accuracy.coerceIn(0f, 1f)
    }

    private fun lengthAccuracy(
        expectedLength: Float,
        drawnLength: Float,
        config: StrokeEvaluationConfig
    ): Float {
        if (expectedLength <= 0f) return 1f
        if (drawnLength <= 0f) return 0f

        val deviation = kotlin.math.abs(drawnLength / expectedLength - 1f)
        val accuracy = 1f - min(1f, deviation / config.lengthTolerance)
        return accuracy.coerceIn(0f, 1f)
    }

    private fun curvatureAccuracy(
        expected: List<PointF>,
        drawn: List<PointF>,
        config: StrokeEvaluationConfig
    ): Float {
        val expectedTurns = turningAngles(expected)
        val drawnTurns = turningAngles(drawn)
        val count = min(expectedTurns.size, drawnTurns.size)
        if (count == 0) return 1f

        var sum = 0f
        for (i in 0 until count) {
            val diff = angularDifference(expectedTurns[i], drawnTurns[i])
            val error = (diff - config.curvatureDeadBand).coerceAtLeast(0f)
            val range = max(1f, config.maxDirectionErrorDegrees - config.curvatureDeadBand)
            val normalized = min(1f, error / range)
            sum += 1f - normalized
        }
        return (sum / count).coerceIn(0f, 1f)
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private fun segmentAngle(from: PointF, to: PointF): Float {
        return atan2(to.y - from.y, to.x - from.x) * 180f / PI
    }

    private fun angularAccuracy(expected: Float, drawn: Float, config: StrokeEvaluationConfig): Float {
        val diff = angularDifference(expected, drawn)
        val error = (diff - config.directionDeadBand).coerceAtLeast(0f)
        val range = max(1f, config.maxDirectionErrorDegrees - config.directionDeadBand)
        val normalized = min(1f, error / range)
        return 1f - normalized
    }

    private fun angularDifference(a: Float, b: Float): Float {
        var diff = kotlin.math.abs(a - b) % 360f
        if (diff > 180f) diff = 360f - diff
        return diff
    }

    private fun turningAngles(points: List<PointF>): List<Float> {
        if (points.size < 3) return emptyList()
        return (1 until points.size - 1).map { i ->
            val a = segmentAngle(points[i - 1], points[i])
            val b = segmentAngle(points[i], points[i + 1])
            angularDifference(a, b)
        }
    }

    private fun boundingBoxDiagonal(points: List<PointF>): Float {
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        return sqrt((maxX - minX) * (maxX - minX) + (maxY - minY) * (maxY - minY))
    }

    private fun failedEvaluation(): StrokeEvaluation {
        return StrokeEvaluation(
            classification = StrokeClassification.Incorrect,
            metrics = StrokeMetrics(
                directionAccuracy = 0f,
                positionAccuracy = 0f,
                lengthAccuracy = 0f,
                curvatureAccuracy = 0f
            )
        )
    }

    companion object {
        private const val PI = 3.14159265f
    }
}
