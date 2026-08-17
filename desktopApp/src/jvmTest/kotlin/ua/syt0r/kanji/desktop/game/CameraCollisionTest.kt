package ua.syt0r.kanji.desktop.game

import kotlin.test.Test
import kotlin.test.assertEquals
import ua.syt0r.kanji.desktop.game.engine.camera.CameraCollision
import ua.syt0r.kanji.desktop.game.engine.geom.Rect
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2

/** Pure camera-collision avoidance (spec §30) — no rendering involved. */
class CameraCollisionTest {

    @Test
    fun `a point outside solids is never moved`() {
        val solids = listOf(Rect(100f, 100f, 50f, 50f))
        val desired = Vec2(20f, 30f)
        assertEquals(desired, CameraCollision.resolve(desired, solids))
    }

    @Test
    fun `a point inside a solid is pushed to the nearest edge`() {
        val solids = listOf(Rect(100f, 100f, 50f, 50f))
        // Desired (130, 140): distances to the left/right/top/bottom edges
        // (with margin) are 42 / 32 / 52 / 22 — the bottom edge is nearest.
        val resolved = CameraCollision.resolve(Vec2(130f, 140f), solids)
        assertEquals(Vec2(130f, 100f + 50f + CameraCollision.MARGIN), resolved)
    }

    @Test
    fun `a point pushed out of one solid is pushed out of an overlapping one too`() {
        val solids = listOf(
            Rect(100f, 100f, 50f, 50f),
            Rect(140f, 60f, 50f, 50f) // overlaps the first's top-right corner
        )
        // Nearest overall escape ends outside BOTH rects.
        val resolved = CameraCollision.resolve(Vec2(160f, 90f), solids)
        val insideAny = solids.any { r ->
            resolved.x > r.x - CameraCollision.MARGIN &&
                resolved.x < r.x + r.width + CameraCollision.MARGIN &&
                resolved.y > r.y - CameraCollision.MARGIN &&
                resolved.y < r.y + r.height + CameraCollision.MARGIN
        }
        assertEquals(false, insideAny, "resolved point $resolved still inside a solid")
    }
}
