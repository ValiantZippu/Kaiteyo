package ua.syt0r.kanji.presentation.screen.main.screen.practice_common

import ua.syt0r.kanji.core.stroke_evaluator.StrokeEvaluationConfig
import ua.syt0r.kanji.core.user_data.preferences.PreferencesWritingStrictness
import ua.syt0r.kanji.presentation.common.resources.string.StringResolveScope

// ============================================
// WRITING STRICTNESS
// The recognition model used by the writing
// practice is tunable per session. Normal is
// the default daily-practice tolerance, Hard
// tightens it, and Exam is the strictest
// (used for the Exam-style session flow).
// ============================================

enum class WritingStrictness(
    override val titleResolver: StringResolveScope<String>,
    val evaluationConfig: StrokeEvaluationConfig
) : DisplayableEnum {

    Normal(
        titleResolver = { letterPractice.evaluationStrictnessNormal },
        evaluationConfig = StrokeEvaluationConfig.Normal
    ),
    Hard(
        titleResolver = { letterPractice.evaluationStrictnessHard },
        evaluationConfig = StrokeEvaluationConfig.Hard
    ),
    Exam(
        titleResolver = { letterPractice.evaluationStrictnessExam },
        evaluationConfig = StrokeEvaluationConfig.Exam
    )
}

fun PreferencesWritingStrictness.toScreenType(): WritingStrictness =
    WritingStrictness.entries.first { it.name == name }

fun WritingStrictness.toRepoType(): PreferencesWritingStrictness =
    PreferencesWritingStrictness.valueOf(name)

// ============================================
// WRITING ATTEMPT STATS
// Collected per written character during one
// practice attempt and merged across repeats so
// the session summary can report real stroke
// accuracy, wrong-order count and near-misses.
// ============================================

data class WritingAttemptStats(
    val strokeCount: Int = 0,
    val mistakes: Int = 0,
    val wrongOrderCount: Int = 0,
    val almostCount: Int = 0,
    /** Weighted overall stroke similarity (0..1), null when nothing was scored. */
    val strokeAccuracy: Float? = null
) {
    val accuracyPercent: Int?
        get() = strokeAccuracy?.let { (it * 100).toInt().coerceIn(0, 100) }

    val isEmpty: Boolean get() = strokeCount == 0

    /** Combines this attempt with another (e.g. per-character writers in a vocab item). */
    fun mergedWith(other: WritingAttemptStats): WritingAttemptStats {
        if (isEmpty) return other
        if (other.isEmpty) return this
        val totalStrokes = strokeCount + other.strokeCount
        val combinedAccuracy = (strokeAccuracy ?: 0f) * strokeCount +
            (other.strokeAccuracy ?: 0f) * other.strokeCount
        return WritingAttemptStats(
            strokeCount = totalStrokes,
            mistakes = mistakes + other.mistakes,
            wrongOrderCount = wrongOrderCount + other.wrongOrderCount,
            almostCount = almostCount + other.almostCount,
            strokeAccuracy = combinedAccuracy / totalStrokes
        )
    }
}
