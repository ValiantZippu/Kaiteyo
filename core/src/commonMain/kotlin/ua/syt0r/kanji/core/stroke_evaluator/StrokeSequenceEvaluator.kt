package ua.syt0r.kanji.core.stroke_evaluator

import androidx.compose.ui.graphics.Path
import kotlin.math.max

/**
 * Evaluates a full drawn character (or vocabulary item) against its expected
 * strokes. Unlike the per-stroke evaluator, this understands the *sequence*:
 *
 *  - wrong order   — a stroke was drawn, but earlier than a stroke it should
 *                    follow (e.g. the horizontal line before the vertical);
 *  - missing stroke — an expected stroke was never drawn;
 *  - extra stroke  — a drawn stroke matched none of the expected strokes.
 *
 * Matching is greedy: expected strokes are processed in order and each is
 * matched to the best-scoring unused drawn stroke. This keeps the algorithm
 * linear-ish and deterministic, which matters for live feedback.
 */
class StrokeSequenceEvaluator(
    private val strokeEvaluator: KanjiStrokeEvaluator
) {

    fun evaluate(
        expectedStrokes: List<Path>,
        drawnStrokes: List<Path>,
        config: StrokeEvaluationConfig = StrokeEvaluationConfig.Normal
    ): StrokeSequenceEvaluation {
        if (expectedStrokes.isEmpty()) {
            return StrokeSequenceEvaluation(
                expectedCount = 0,
                drawnCount = drawnStrokes.size,
                perStroke = emptyList(),
                issues = drawnStrokes.indices.map {
                    StrokeSequenceIssue(StrokeSequenceIssueType.ExtraStroke, null, it)
                },
                overallAccuracy = 0f
            )
        }

        val usedDrawn = BooleanArray(drawnStrokes.size)
        val matchedDrawnIndices = arrayOfNulls<Int>(expectedStrokes.size)

        val perStroke = expectedStrokes.mapIndexed { expectedIndex, expected ->
            val (drawnIndex, evaluation) = bestMatch(
                expected = expected,
                drawnStrokes = drawnStrokes,
                used = usedDrawn,
                config = config
            )
            if (drawnIndex != null) {
                usedDrawn[drawnIndex] = true
                matchedDrawnIndices[expectedIndex] = drawnIndex
            }
            evaluation
        }

        // Order check: the drawn index matched to expected[i] must increase as
        // i increases, otherwise the user drew strokes out of order.
        val issues = mutableListOf<StrokeSequenceIssue>()
        var lastDrawnIndex = -1
        matchedDrawnIndices.forEachIndexed { expectedIndex, drawnIndex ->
            when {
                drawnIndex == null -> {
                    issues += StrokeSequenceIssue(
                        StrokeSequenceIssueType.MissingStroke,
                        expectedIndex,
                        null
                    )
                }

                drawnIndex < lastDrawnIndex -> {
                    issues += StrokeSequenceIssue(
                        StrokeSequenceIssueType.WrongOrder,
                        expectedIndex,
                        drawnIndex
                    )
                    lastDrawnIndex = max(lastDrawnIndex, drawnIndex)
                }

                else -> lastDrawnIndex = max(lastDrawnIndex, drawnIndex)
            }
        }

        // Any drawn stroke that was never used is an extra stroke.
        drawnStrokes.indices.forEach { drawnIndex ->
            if (!usedDrawn[drawnIndex]) {
                issues += StrokeSequenceIssue(
                    StrokeSequenceIssueType.ExtraStroke,
                    null,
                    drawnIndex
                )
            }
        }

        // Overall accuracy: sum of per-stroke scores (missing = 0) scaled by the
        // drawn/expected ratio so extra strokes reduce the score.
        val scoreSum = perStroke.sumOf { it.metrics.overallScore.toDouble() }.toFloat()
        val base = scoreSum / expectedStrokes.size
        val overallAccuracy = base * (expectedStrokes.size.toFloat() /
            max(1, drawnStrokes.size))

        return StrokeSequenceEvaluation(
            expectedCount = expectedStrokes.size,
            drawnCount = drawnStrokes.size,
            perStroke = perStroke,
            issues = issues,
            overallAccuracy = overallAccuracy.coerceIn(0f, 1f)
        )
    }

    private fun bestMatch(
        expected: Path,
        drawnStrokes: List<Path>,
        used: BooleanArray,
        config: StrokeEvaluationConfig
    ): Pair<Int?, StrokeEvaluation> {
        var bestIndex: Int? = null
        var bestScore = 0f
        var bestEvaluation = StrokeSequenceEvaluation.MissingStrokeEvaluation

        drawnStrokes.forEachIndexed { index, drawn ->
            if (used[index]) return@forEachIndexed
            val evaluation = strokeEvaluator.evaluate(expected, drawn, config)
            if (evaluation.metrics.overallScore > bestScore) {
                bestIndex = index
                bestScore = evaluation.metrics.overallScore
                bestEvaluation = evaluation
            }
        }

        // Only accept the match if it is at least AlmostCorrect — otherwise the
        // expected stroke is considered missing (and the drawn stroke stays free
        // to be flagged as extra later).
        return if (bestIndex != null && bestEvaluation.isAcceptable) {
            bestIndex to bestEvaluation
        } else {
            null to StrokeSequenceEvaluation.MissingStrokeEvaluation
        }
    }
}
