package ua.syt0r.kanji.core.stroke_evaluator

// ============================================
// STROKE EVALUATION MODELS
// Structured, per-stroke scoring output used by
// the writing practice UI for real-time feedback
// and by the sequence evaluator for whole
// character analysis.
//
// All accuracy values are 0..1 (1 = perfect).
// ============================================

/** Per-stroke similarity breakdown. */
data class StrokeMetrics(
    /** How well the drawn direction matches the reference (0..1). */
    val directionAccuracy: Float,
    /** How well the drawn position matches after centering/scaling (0..1). */
    val positionAccuracy: Float,
    /** How close the drawn length is to the reference (0..1). */
    val lengthAccuracy: Float,
    /** How well turning/curvature matches the reference (0..1). */
    val curvatureAccuracy: Float
) {
    /** Weighted overall similarity (0..1). */
    val overallScore: Float
        get() = directionAccuracy * 0.4f + positionAccuracy * 0.35f +
            lengthAccuracy * 0.15f + curvatureAccuracy * 0.1f

    /** 0..100 integer, for display ("Stroke accuracy: 92%"). */
    fun accuracyPercent(): Int = (overallScore * 100).toInt().coerceIn(0, 100)

    companion object {
        val Perfect = StrokeMetrics(
            directionAccuracy = 1f,
            positionAccuracy = 1f,
            lengthAccuracy = 1f,
            curvatureAccuracy = 1f
        )
    }
}

enum class StrokeClassification {
    Correct,
    AlmostCorrect,
    Incorrect
}

/** Result of evaluating one drawn stroke against one reference stroke. */
data class StrokeEvaluation(
    val classification: StrokeClassification,
    val metrics: StrokeMetrics
) {
    val isAcceptable: Boolean get() = classification != StrokeClassification.Incorrect
}

// ------------------------------------------------------------
// Whole-character sequence analysis
// ------------------------------------------------------------

enum class StrokeSequenceIssueType {
    /** A stroke was drawn out of order (its match appears before a previous stroke's match). */
    WrongOrder,

    /** An expected stroke was never drawn (or never matched well enough). */
    MissingStroke,

    /** A stroke was drawn that does not correspond to any expected stroke. */
    ExtraStroke
}

data class StrokeSequenceIssue(
    val type: StrokeSequenceIssueType,
    /** Index of the expected stroke (null for ExtraStroke). */
    val expectedIndex: Int?,
    /** Index in the drawn sequence (null for MissingStroke). */
    val drawnIndex: Int?
)

/**
 * Full-character evaluation: every drawn stroke is matched against the
 * expected strokes, with order / missing / extra detection.
 */
data class StrokeSequenceEvaluation(
    val expectedCount: Int,
    val drawnCount: Int,
    /** Evaluation of each expected stroke in order (missing strokes get a 0-score evaluation). */
    val perStroke: List<StrokeEvaluation>,
    val issues: List<StrokeSequenceIssue>,
    /** Overall accuracy 0..1 — extra strokes reduce it proportionally. */
    val overallAccuracy: Float
) {
    /** Overall accuracy as an integer percentage (0..100). */
    fun accuracyPercent(): Int = (overallAccuracy * 100).toInt().coerceIn(0, 100)

    companion object {
        /** Evaluation used for an expected stroke that was never matched. */
        val MissingStrokeEvaluation = StrokeEvaluation(
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
