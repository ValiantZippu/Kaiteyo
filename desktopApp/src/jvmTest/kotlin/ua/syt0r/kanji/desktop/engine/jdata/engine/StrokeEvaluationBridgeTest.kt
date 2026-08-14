package ua.syt0r.kanji.desktop.engine.jdata.engine

import ua.syt0r.kanji.core.stroke_evaluator.StrokeClassification
import ua.syt0r.kanji.core.stroke_evaluator.StrokeEvaluationConfig
import ua.syt0r.kanji.core.stroke_evaluator.StrokeSequenceIssueType
import ua.syt0r.kanji.desktop.engine.jdata.model.SourceRef
import ua.syt0r.kanji.desktop.engine.jdata.model.StrokeEntry
import ua.syt0r.kanji.desktop.engine.jdata.model.StrokeSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [StrokeEvaluationBridge] (platform stroke sets -> core evaluators)
 * and the platform's own [StrokeSequenceValidator]. Uses two geometrically
 * distinct strokes (horizontal vs vertical) so sequence matching is
 * unambiguous.
 */
class StrokeEvaluationBridgeTest {

    private val horizontal = "M0,0 L100,0"
    private val vertical = "M0,0 L0,100"

    private val bridge = StrokeEvaluationBridge()

    private fun setOf(vararg paths: String): StrokeSet =
        StrokeSet(
            character = "食",
            strokeCount = paths.size,
            strokes = paths.mapIndexed { index, path -> StrokeEntry(index = index, path = path) },
            source = SourceRef("kanjivg", "98df")
        )

    @Test
    fun identicalStrokesPassAtEveryStrictness() {
        val set = setOf(horizontal, vertical)
        listOf(WritingStrictness.Relaxed, WritingStrictness.Normal, WritingStrictness.Exam).forEach { strictness ->
            val result = bridge.evaluate(set, listOf(horizontal, vertical), strictness)
            assertTrue(result.structuralIssues.isEmpty(), "no structural issues at $strictness")
            assertTrue(result.geometryAvailable, "geometry available at $strictness")
            assertTrue(result.accuracyPercent() >= 99, "accuracy at $strictness: ${result.accuracyPercent()}")
            assertTrue(result.issues.isEmpty(), "no sequence issues at $strictness")
            assertTrue(result.accepted, "accepted at $strictness")
        }
    }

    @Test
    fun reversedOrderIsDetectedAndFailsExamStrictness() {
        val set = setOf(horizontal, vertical)
        val drawn = listOf(vertical, horizontal)

        val relaxed = bridge.evaluate(set, drawn, WritingStrictness.Relaxed)
        assertTrue(relaxed.issues.any { it.type == StrokeSequenceIssueType.WrongOrder })
        assertTrue(relaxed.accepted, "relaxed strictness tolerates an order slip")

        val exam = bridge.evaluate(set, drawn, WritingStrictness.Exam)
        assertFalse(exam.accepted, "exam strictness must reject wrong stroke order")
    }

    @Test
    fun countMismatchIsStructuralAndProducesExtraStroke() {
        val set = setOf(horizontal, vertical)
        val result = bridge.evaluate(set, listOf(horizontal, vertical, horizontal), WritingStrictness.Normal)
        assertTrue(result.structuralIssues.any { it.contains("count") }, "count issue: ${result.structuralIssues}")
        assertFalse(result.accepted)
        assertNotNull(result.sequence)
        assertTrue(result.issues.any { it.type == StrokeSequenceIssueType.ExtraStroke })
    }

    @Test
    fun emptyDrawingIsRejectedWithoutGeometry() {
        val set = setOf(horizontal, vertical)
        val result = bridge.evaluate(set, emptyList(), WritingStrictness.Normal)
        assertTrue(result.structuralIssues.isNotEmpty())
        assertFalse(result.accepted)
        assertNull(result.sequence)
        assertFalse(result.geometryAvailable)
    }

    @Test
    fun strictnessMapsToCoreConfigsAndThresholds() {
        assertEquals(StrokeEvaluationConfig.Normal, StrokeEvaluationBridge.configFor(WritingStrictness.Relaxed))
        assertEquals(StrokeEvaluationConfig.Hard, StrokeEvaluationBridge.configFor(WritingStrictness.Normal))
        assertEquals(StrokeEvaluationConfig.Exam, StrokeEvaluationBridge.configFor(WritingStrictness.Exam))
        assertEquals(0.5f, StrokeEvaluationBridge.acceptanceThreshold(WritingStrictness.Relaxed))
        assertEquals(0.65f, StrokeEvaluationBridge.acceptanceThreshold(WritingStrictness.Normal))
        assertEquals(0.8f, StrokeEvaluationBridge.acceptanceThreshold(WritingStrictness.Exam))
    }

    @Test
    fun singleStrokeEvaluationScoresCorrectly() {
        val perfect = bridge.evaluateStroke(horizontal, horizontal, WritingStrictness.Normal)
        assertNotNull(perfect)
        assertEquals(StrokeClassification.Correct, perfect.classification)

        val mismatched = bridge.evaluateStroke(horizontal, vertical, WritingStrictness.Normal)
        assertNotNull(mismatched)
        assertTrue(mismatched.metrics.overallScore < 0.6f, "horizontal vs vertical must score low: ${mismatched.metrics.overallScore}")
    }

    @Test
    fun sequenceEvaluationScoresIdenticalInputPerfectly() {
        val seq = bridge.evaluateSequence(
            listOf(horizontal, vertical),
            listOf(horizontal, vertical),
            WritingStrictness.Normal
        )
        assertNotNull(seq)
        assertTrue(seq.overallAccuracy >= 0.99f)
        assertTrue(seq.issues.isEmpty())
    }

    @Test
    fun sequenceEvaluationReturnsNullOnUnparseablePaths() {
        assertNull(bridge.evaluateSequence(listOf("M0,0 L100,0"), listOf("X 10 10"), WritingStrictness.Normal))
    }

    // ------------------------------------------------------------
    // StrokeSequenceValidator — platform-level writing rules
    // ------------------------------------------------------------

    @Test
    fun countValidationHonorsRelaxedTolerance() {
        assertTrue(StrokeSequenceValidator.validateCount(3, 3).valid)
        assertFalse(StrokeSequenceValidator.validateCount(3, 5).valid)
        assertTrue(StrokeSequenceValidator.validateCount(3, 5, WritingStrictness.Relaxed).valid)
        assertFalse(StrokeSequenceValidator.validateCount(3, 6, WritingStrictness.Relaxed).valid)
    }

    @Test
    fun orderValidationDetectsPermutationsAndDeviation() {
        assertTrue(StrokeSequenceValidator.validateOrder(listOf(0, 1, 2, 3), listOf(0, 1, 2, 3), WritingStrictness.Normal).valid)
        // Single adjacent swap tolerated outside exam mode.
        assertTrue(StrokeSequenceValidator.validateOrder(listOf(0, 1, 2, 3), listOf(0, 2, 1, 3), WritingStrictness.Normal).valid)
        assertFalse(StrokeSequenceValidator.validateOrder(listOf(0, 1, 2, 3), listOf(0, 2, 1, 3), WritingStrictness.Exam).valid)
        // More than two positions off fails everywhere.
        assertFalse(StrokeSequenceValidator.validateOrder(listOf(0, 1, 2, 3), listOf(0, 3, 2, 1), WritingStrictness.Normal).valid)
        // Length mismatch.
        assertFalse(StrokeSequenceValidator.validateOrder(listOf(0, 1, 2, 3), listOf(0, 1, 2), WritingStrictness.Normal).valid)
    }

    @Test
    fun sequenceValidationFlagsOutOfRangeStrokeIndices() {
        val set = StrokeSet(
            character = "食",
            strokeCount = 2,
            strokes = listOf(StrokeEntry(index = 0), StrokeEntry(index = 5)),
            source = SourceRef("test")
        )
        val result = StrokeSequenceValidator.validateSequence(set, listOf(0, 1), WritingEvaluationConfig())
        assertFalse(result.valid)
        assertTrue(result.issues.any { it.contains("out of range") })
    }
}
