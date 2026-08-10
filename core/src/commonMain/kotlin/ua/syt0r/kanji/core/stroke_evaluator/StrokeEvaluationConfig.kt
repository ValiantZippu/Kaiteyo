package ua.syt0r.kanji.core.stroke_evaluator

// ============================================
// STROKE EVALUATION CONFIG
// Every tolerance and scoring weight lives here
// so the recognition model is tunable per mode
// instead of being hardcoded across the codebase.
//
// Presets:
//   Normal — default writing practice.
//   Hard   — stricter tolerances, used by the
//            "Hard" practice mode.
//   Exam   — strictest tolerances, used by the
//            "Exam" mode (final-scoring session).
// ============================================

data class StrokeEvaluationConfig(
    /** Degrees of angular deviation that incur no penalty. */
    val directionDeadBand: Float = 6f,
    /** Direction deviation (degrees) that scores ~0 accuracy. */
    val maxDirectionErrorDegrees: Float = 40f,

    /** Position dead band as a fraction of the reference stroke scale. */
    val positionDeadBandFraction: Float = 0.02f,
    /** Positional error in dead-band units that scores ~0 accuracy. */
    val maxPositionError: Float = 3f,

    /** Length deviation ratio that scores ~0 accuracy (0.25 = 25%). */
    val lengthTolerance: Float = 0.25f,

    /** Turning-angle deviation (degrees) that incurs no curvature penalty. */
    val curvatureDeadBand: Float = 8f,

    /** Scoring weights; must sum to 1.0. */
    val directionWeight: Float = 0.4f,
    val positionWeight: Float = 0.35f,
    val lengthWeight: Float = 0.15f,
    val curvatureWeight: Float = 0.1f,

    /** Overall score (0..1) at or above which a stroke is Correct. */
    val correctThreshold: Float = 0.78f,
    /** Overall score (0..1) at or above which a stroke is AlmostCorrect. */
    val almostThreshold: Float = 0.6f,

    /** Number of evenly-spaced samples taken from each stroke. */
    val interpolationPoints: Int = 24
) {
    init {
        require(directionWeight + positionWeight + lengthWeight + curvatureWeight > 0.99f) {
            "Stroke evaluation weights must sum to 1.0"
        }
        require(correctThreshold > almostThreshold) {
            "correctThreshold must be above almostThreshold"
        }
    }

    companion object {
        val Normal = StrokeEvaluationConfig()

        val Hard = StrokeEvaluationConfig(
            directionDeadBand = 4f,
            maxDirectionErrorDegrees = 30f,
            positionDeadBandFraction = 0.015f,
            maxPositionError = 2.5f,
            lengthTolerance = 0.18f,
            curvatureDeadBand = 6f,
            correctThreshold = 0.85f,
            almostThreshold = 0.68f
        )

        val Exam = StrokeEvaluationConfig(
            directionDeadBand = 3f,
            maxDirectionErrorDegrees = 22f,
            positionDeadBandFraction = 0.012f,
            maxPositionError = 2f,
            lengthTolerance = 0.12f,
            curvatureDeadBand = 5f,
            correctThreshold = 0.9f,
            almostThreshold = 0.75f
        )
    }
}
