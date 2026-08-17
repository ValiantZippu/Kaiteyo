package ua.syt0r.kanji.desktop.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ua.syt0r.kanji.desktop.game.activity.Stroke
import ua.syt0r.kanji.desktop.game.activity.StrokePoint
import ua.syt0r.kanji.desktop.game.activity.WritingEvaluator
import ua.syt0r.kanji.desktop.game.interaction.Interactable
import ua.syt0r.kanji.desktop.game.interaction.InteractionBehavior
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2
import ua.syt0r.kanji.desktop.game.ui.menus.FocusNav

/**
 * In-world writing (spec §57-59), menu focus (spec §14-15) and phase-gated
 * activities (spec §40) — the pure logic behind each system.
 */
class WritingAndGatingTest {

    // ------------------------------------------------------------
    // Writing evaluator (lenient coverage, spec §59)
    // ------------------------------------------------------------

    @Test
    fun `no strokes fails`() {
        assertFalse(WritingEvaluator.evaluate(emptyList()).pass)
        assertEquals(0f, WritingEvaluator.evaluate(emptyList()).coverage)
    }

    @Test
    fun `a full sweep across the canvas passes`() {
        val stroke: Stroke = listOf(
            StrokePoint(0f, 0.1f),
            StrokePoint(0.5f, 0.1f),
            StrokePoint(1f, 0.1f)
        )
        assertTrue(WritingEvaluator.evaluate(listOf(stroke)).pass)
    }

    @Test
    fun `a tiny scribble fails`() {
        val stroke: Stroke = listOf(StrokePoint(0.49f, 0.49f), StrokePoint(0.51f, 0.51f))
        assertFalse(WritingEvaluator.evaluate(listOf(stroke)).pass)
    }

    @Test
    fun `a rough trace of a kana-like shape passes`() {
        // A loose あ-ish loop: up-right curve, cross stroke, tail.
        val stroke: Stroke = listOf(
            StrokePoint(0.3f, 0.7f), StrokePoint(0.35f, 0.3f), StrokePoint(0.5f, 0.2f),
            StrokePoint(0.65f, 0.3f), StrokePoint(0.7f, 0.7f),
            StrokePoint(0.2f, 0.55f), StrokePoint(0.8f, 0.55f),
            StrokePoint(0.5f, 0.55f), StrokePoint(0.45f, 0.95f)
        )
        assertTrue(WritingEvaluator.evaluate(listOf(stroke)).pass)
    }

    @Test
    fun `score is coverage between zero and one`() {
        val stroke: Stroke = listOf(StrokePoint(0f, 0f), StrokePoint(0f, 0.5f))
        val result = WritingEvaluator.evaluate(listOf(stroke))
        assertTrue(result.coverage in 0f..1f)
    }

    // ------------------------------------------------------------
    // Phase gating (spec §40)
    // ------------------------------------------------------------

    private fun gate(vararg phases: String): Interactable = Interactable(
        id = "g",
        position = Vec2.Zero,
        radius = 64f,
        promptJp = "x",
        promptEn = "x",
        behavior = InteractionBehavior.Inspect("o", emptyList()),
        availablePhases = phases.toList()
    )

    @Test
    fun `ungated interactables are always open`() {
        assertTrue(gate().isOpenAt("Morning"))
        assertTrue(gate().isOpenAt("Night"))
    }

    @Test
    fun `evening stall is closed at noon and open at dusk`() {
        val stall = gate("Evening")
        assertFalse(stall.isOpenAt("Day"))
        assertTrue(stall.isOpenAt("Evening"))
    }

    @Test
    fun `phase matching is case-insensitive`() {
        assertTrue(gate("evening").isOpenAt("Evening"))
    }

    // ------------------------------------------------------------
    // Menu focus (spec §14-15)
    // ------------------------------------------------------------

    @Test
    fun `focus moves down and wraps`() {
        val nav = FocusNav(3)
        assertEquals(0, nav.focused)
        nav.down()
        assertEquals(1, nav.focused)
        nav.down()
        nav.down()
        assertEquals(1, nav.focused) // wrapped 2 → 0 → 1
    }

    @Test
    fun `focus moves up and wraps`() {
        val nav = FocusNav(2)
        nav.up()
        assertEquals(1, nav.focused)
        nav.up()
        assertEquals(0, nav.focused)
    }

    @Test
    fun `select returns the focused index`() {
        val nav = FocusNav(4)
        nav.down()
        nav.down()
        assertEquals(2, nav.select())
    }
}
