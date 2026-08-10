package ua.syt0r.kanji.core.stroke_evaluator

import androidx.compose.ui.graphics.Path

interface KanjiStrokeEvaluator {

    /**
     * Boolean similarity check. Default implementation derives from [evaluate]:
     * anything that is not Incorrect counts as "similar", so existing callers
     * keep working while implementations migrate to the scored API.
     */
    fun areStrokesSimilar(first: Path, second: Path): Boolean {
        return evaluate(
            expected = first,
            drawn = second
        ).isAcceptable
    }

    /**
     * Scores a drawn stroke against a reference stroke.
     *
     * @param expected the reference stroke (from kanji stroke data)
     * @param drawn    the user's drawn stroke
     * @param config   tolerance/scoring model (strictness preset)
     */
    fun evaluate(
        expected: Path,
        drawn: Path,
        config: StrokeEvaluationConfig = StrokeEvaluationConfig.Normal
    ): StrokeEvaluation {
        // Default: binary classification without detailed metrics. Implementations
        // that want per-stroke scoring (direction/position/length) override this.
        return if (areStrokesSimilar(expected, drawn)) {
            StrokeEvaluation(
                classification = StrokeClassification.Correct,
                metrics = StrokeMetrics.Perfect
            )
        } else {
            StrokeEvaluation(
                classification = StrokeClassification.Incorrect,
                metrics = StrokeMetrics(
                    directionAccuracy = 0f,
                    positionAccuracy = 0f,
                    lengthAccuracy = 0f,
                    curvatureAccuracy = 0f
                )
            )
        }
    }

}
