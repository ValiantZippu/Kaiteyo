package ua.syt0r.kanji

import androidx.compose.ui.graphics.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import ua.syt0r.kanji.core.stroke_evaluator.DefaultKanjiStrokeEvaluator
import ua.syt0r.kanji.core.stroke_evaluator.StrokeEvaluationConfig
import ua.syt0r.kanji.core.stroke_evaluator.StrokeSequenceEvaluator
import ua.syt0r.kanji.core.stroke_evaluator.StrokeSequenceIssueType
import ua.syt0r.kanji.core.user_data.preferences.PreferencesWritingStrictness
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.WritingAttemptStats
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.WritingStrictness
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.toRepoType
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.toScreenType

/**
 * Tests for the writing session layer: multi-character (vocabulary) sequence
 * evaluation, writing-attempt statistics merging, and strictness mapping.
 */
class WritingSessionTest {

    private val evaluator = DefaultKanjiStrokeEvaluator()
    private val sequenceEvaluator = StrokeSequenceEvaluator(evaluator)

    // ------------------------------------------------------------
    // Multi-character vocabulary sequence evaluation
    // ------------------------------------------------------------

    @Test
    fun multiCharacterVocabulary_secondCharacterMissing_detectsMissingStrokes() {
        // Vocabulary "日本語" (two kanji with strokes in this simplified test):
        // character 1: "T" shape (2 strokes), character 2: "L" shape (2 strokes).
        val character1Strokes = listOf(
            horizontalLine(0f, 0f, 100f),
            verticalLine(50f, 0f, 100f)
        )
        val character2Strokes = listOf(
            horizontalLine(0f, 0f, 80f),
            verticalLine(80f, 0f, 60f)
        )

        // User drew only the first character correctly.
        val drawn = character1Strokes

        val result = sequenceEvaluator.evaluate(
            expectedStrokes = character1Strokes + character2Strokes,
            drawnStrokes = drawn
        )

        val missingCount = result.issues.count { it.type == StrokeSequenceIssueType.MissingStroke }
        assertEquals(2, missingCount, "both strokes of the second character are missing: ${result.issues}")
        assertTrue(
            result.overallAccuracy < 0.55f,
            "missing half the characters must cut accuracy, was ${result.overallAccuracy}"
        )
    }

    @Test
    fun multiCharacterVocabulary_wrongOrderAcrossCharacters_isDetected() {
        // Two characters, each a single stroke: horizontal then vertical.
        val expected = listOf(
            horizontalLine(0f, 0f, 100f),
            verticalLine(50f, 0f, 100f)
        )
        // Drawn in reverse order (as if the user wrote the second character first).
        val drawn = listOf(
            verticalLine(50f, 0f, 100f),
            horizontalLine(0f, 0f, 100f)
        )

        val result = sequenceEvaluator.evaluate(expected, drawn)

        assertTrue(
            result.issues.any { it.type == StrokeSequenceIssueType.WrongOrder },
            "expected WrongOrder, got ${result.issues}"
        )
    }

    @Test
    fun extraCharacter_whenNoneExpected_reducesAccuracy() {
        val expected = listOf(horizontalLine(0f, 0f, 100f))
        val drawn = listOf(
            horizontalLine(0f, 0f, 100f),
            horizontalLine(0f, 0f, 100f) // duplicated stroke = extra
        )

        val result = sequenceEvaluator.evaluate(expected, drawn)

        assertTrue(
            result.issues.any { it.type == StrokeSequenceIssueType.ExtraStroke },
            "expected ExtraStroke, got ${result.issues}"
        )
    }

    // ------------------------------------------------------------
    // Writing attempt statistics
    // ------------------------------------------------------------

    @Test
    fun attemptStats_merge_combinesCountsAndWeightsAccuracy() {
        val first = WritingAttemptStats(
            strokeCount = 2,
            mistakes = 1,
            wrongOrderCount = 1,
            almostCount = 0,
            strokeAccuracy = 0.8f
        )
        val second = WritingAttemptStats(
            strokeCount = 3,
            mistakes = 0,
            wrongOrderCount = 0,
            almostCount = 1,
            strokeAccuracy = 0.9f
        )

        val merged = first.mergedWith(second)

        assertEquals(5, merged.strokeCount)
        assertEquals(1, merged.mistakes)
        assertEquals(1, merged.wrongOrderCount)
        assertEquals(1, merged.almostCount)
        // Weighted: (0.8 * 2 + 0.9 * 3) / 5
        assertEquals(0.86f, merged.strokeAccuracy!!, 0.001f)
        assertEquals(86, merged.accuracyPercent)
    }

    @Test
    fun attemptStats_merge_withEmpty_isIdentity() {
        val stats = WritingAttemptStats(
            strokeCount = 4,
            mistakes = 2,
            wrongOrderCount = 1,
            strokeAccuracy = 0.75f
        )
        assertEquals(stats, stats.mergedWith(WritingAttemptStats()))
        assertEquals(stats, WritingAttemptStats().mergedWith(stats))
    }

    @Test
    fun attemptStats_withoutScores_hasNullAccuracy() {
        val stats = WritingAttemptStats(strokeCount = 3)
        assertEquals(null, stats.strokeAccuracy)
        assertEquals(null, stats.accuracyPercent)
    }

    // ------------------------------------------------------------
    // Strictness mapping
    // ------------------------------------------------------------

    @Test
    fun strictness_screenToRepoRoundTrip() {
        WritingStrictness.entries.forEach { screenType ->
            assertEquals(screenType, screenType.toRepoType().toScreenType())
        }
    }

    @Test
    fun strictness_presetsMapToTunedConfigs() {
        assertEquals(StrokeEvaluationConfig.Normal, WritingStrictness.Normal.evaluationConfig)
        assertNotNull(WritingStrictness.Hard.evaluationConfig)
        assertNotNull(WritingStrictness.Exam.evaluationConfig)
        assertEquals(PreferencesWritingStrictness.Exam, WritingStrictness.Exam.toRepoType())
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
}
