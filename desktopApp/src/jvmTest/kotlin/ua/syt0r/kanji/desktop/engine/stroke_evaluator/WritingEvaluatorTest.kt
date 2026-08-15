package ua.syt0r.kanji.desktop.engine.stroke_evaluator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WritingEvaluatorTest {

    /** No repository, no KanjiVG dir → built-in dataset is the active source. */
    private val evaluator = WritingEvaluator(repository = null, kanjiVgDirectory = null)

    @Test
    fun `builtin source is used when no KanjiVG directory exists`() {
        assertFalse(evaluator.kanjiVgPresent)
        assertEquals("builtin", evaluator.activeSource)
    }

    @Test
    fun `builtin evaluator scores a correct horizontal stroke`() {
        val drawn = listOf(
            listOf(StrokePoint(12.0, 50.0), StrokePoint(50.0, 50.0), StrokePoint(88.0, 50.0))
        )
        val result = evaluator.evaluate("一", drawn, 100.0, 100.0)

        assertTrue(result.supported)
        assertEquals("builtin", result.sourceLabel)
        assertEquals(1, result.referenceStrokeCount)
        assertTrue(result.strokes.first().correct)
        assertTrue(result.accuracy >= 0.8f)
    }

    @Test
    fun `builtin evaluator rejects a vertical stroke for a horizontal character`() {
        val drawn = listOf(
            listOf(StrokePoint(50.0, 12.0), StrokePoint(50.0, 88.0))
        )
        val result = evaluator.evaluate("一", drawn, 100.0, 100.0)

        assertTrue(result.supported)
        assertFalse(result.strokes.first().correct)
        assertTrue(result.accuracy < 0.5f)
    }

    @Test
    fun `unknown characters report unsupported honestly`() {
        val drawn = listOf(listOf(StrokePoint(10.0, 10.0), StrokePoint(90.0, 90.0)))
        val result = evaluator.evaluate("鬱", drawn, 100.0, 100.0)

        assertFalse(result.supported)
        assertTrue(result.strokes.isEmpty())
        assertEquals("builtin", result.sourceLabel)
    }

    @Test
    fun `empty drawing reports unsupported`() {
        val result = evaluator.evaluate("一", emptyList(), 100.0, 100.0)
        assertFalse(result.supported)
        assertEquals(0, result.drawnStrokeCount)
    }

    @Test
    fun `pointsToSvgPath produces a valid polyline path`() {
        val path = pointsToSvgPath(
            listOf(StrokePoint(0.0, 0.0), StrokePoint(50.0, 50.0), StrokePoint(100.0, 100.0)),
            grid = 100.0
        )
        // M0,0 L546,546 L1092,1092 — the 0..100 grid mapped to the 1092 box.
        assertTrue(path.startsWith("M0,0"), "path: $path")
        assertTrue(path.contains(" L546,546"), "path: $path")
        assertTrue(path.contains(" L1092,1092"), "path: $path")
    }

    @Test
    fun `supports reflects the builtin dataset`() {
        assertTrue(evaluator.supports("一"))
        assertTrue(evaluator.supports("日"))
        assertFalse(evaluator.supports("鬱"))
    }
}
