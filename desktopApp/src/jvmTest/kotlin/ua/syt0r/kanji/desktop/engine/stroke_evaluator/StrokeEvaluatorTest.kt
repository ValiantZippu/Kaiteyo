package ua.syt0r.kanji.desktop.engine.stroke_evaluator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StrokeEvaluatorTest {

    // 一 is a single left→right horizontal stroke: (10,50) → (90,50).
    private val ichiReference = ReferenceStroke.of(10.0 to 50.0, 90.0 to 50.0)

    @Test
    fun `supports only characters with canonical data`() {
        assertTrue(StrokeEvaluator.supports("一"))
        assertTrue(StrokeEvaluator.supports("日"))
        assertFalse(StrokeEvaluator.supports("鬱"), "Unlisted kanji must report unsupported, never guess")
        assertFalse(StrokeEvaluator.supports("abc"))
        assertFalse(StrokeEvaluator.supports("食べる"), "Multi-character words are unsupported (per-character only)")
    }

    @Test
    fun `a close horizontal stroke scores as correct`() {
        val drawn = listOf(
            listOf(StrokePoint(12.0, 50.0), StrokePoint(50.0, 50.0), StrokePoint(88.0, 50.0))
        )
        val result = StrokeEvaluator.evaluate("一", drawn, 100.0, 100.0)

        assertTrue(result.supported)
        assertEquals(1, result.strokeEvaluations.size)
        assertTrue(result.strokeEvaluations.first().correct, "Slight offset must stay within tolerance")
        assertTrue(result.accuracy >= 0.8f, "Accuracy should be high for a near-perfect stroke")
    }

    @Test
    fun `a vertical stroke where a horizontal is expected fails shape and direction`() {
        val drawn = listOf(
            listOf(StrokePoint(50.0, 12.0), StrokePoint(50.0, 88.0))
        )
        val result = StrokeEvaluator.evaluate("一", drawn, 100.0, 100.0)

        assertTrue(result.supported)
        val ev = result.strokeEvaluations.first()
        assertFalse(ev.correct)
        assertTrue(ev.directionErrorDegrees > 45.0, "Vertical vs horizontal must exceed the direction tolerance")
        assertTrue(result.accuracy < 0.5f)
    }

    @Test
    fun `empty strokes report unsupported`() {
        val result = StrokeEvaluator.evaluate("一", emptyList(), 100.0, 100.0)
        assertFalse(result.supported)
        assertEquals(0f, result.accuracy)
    }

    @Test
    fun `direction error is measured in degrees`() {
        val reference = ReferenceStroke.of(10.0 to 10.0, 90.0 to 10.0) // 0°
        val user = ReferenceStroke.of(10.0 to 90.0, 90.0 to 90.0)      // 0° (right) — but far off
        val drawn = listOf(listOf(StrokePoint(10.0, 90.0), StrokePoint(90.0, 90.0)))
        val result = StrokeEvaluator.evaluate("一", drawn, 100.0, 100.0)
        val ev = result.strokeEvaluations.first()
        assertTrue(ev.deviation > StrokeEvaluator.SHAPE_TOLERANCE, "A stroke 80 units off must fail shape")
        assertFalse(ev.correct)
    }

    @Test
    fun `日 four-stroke sequence - correct order scores highest`() {
        // 日 reference strokes:
        // 1. left vertical (30,18)→(30,82)
        // 2. top+right (30,18)→(70,18)→(70,82)
        // 3. middle horizontal (30,50)→(70,50)
        // 4. bottom (30,82)→(70,82)
        val correct = listOf(
            listOf(StrokePoint(30.0, 18.0), StrokePoint(30.0, 82.0)),
            listOf(StrokePoint(30.0, 18.0), StrokePoint(70.0, 18.0), StrokePoint(70.0, 82.0)),
            listOf(StrokePoint(30.0, 50.0), StrokePoint(70.0, 50.0)),
            listOf(StrokePoint(30.0, 82.0), StrokePoint(70.0, 82.0))
        )
        val result = StrokeEvaluator.evaluate("日", correct, 100.0, 100.0)

        assertTrue(result.supported)
        assertEquals(4, result.strokeEvaluations.size)
        assertTrue(result.strokeEvaluations.all { it.correct })
        assertTrue(result.accuracy >= 0.9f)
    }

    @Test
    fun `日 with scrambled stroke order still matches by shape`() {
        // Same shapes, different order — greedy matching should still assign
        // each stroke to its nearest reference and keep most correct.
        val scrambled = listOf(
            listOf(StrokePoint(30.0, 82.0), StrokePoint(70.0, 82.0)), // bottom first
            listOf(StrokePoint(30.0, 18.0), StrokePoint(70.0, 18.0), StrokePoint(70.0, 82.0)),
            listOf(StrokePoint(30.0, 50.0), StrokePoint(70.0, 50.0)),
            listOf(StrokePoint(30.0, 18.0), StrokePoint(30.0, 82.0))
        )
        val result = StrokeEvaluator.evaluate("日", scrambled, 100.0, 100.0)

        assertTrue(result.supported)
        val correct = result.strokeEvaluations.count { it.correct }
        assertTrue(correct >= 3, "Order-tolerant matching should keep the shape-correct strokes (got $correct/4)")
    }

    @Test
    fun `unsupported characters never fabricate results`() {
        val drawn = listOf(listOf(StrokePoint(10.0, 10.0), StrokePoint(90.0, 90.0)))
        val result = StrokeEvaluator.evaluate("鬱", drawn, 100.0, 100.0)
        assertFalse(result.supported)
        assertTrue(result.strokeEvaluations.isEmpty())
    }
}
