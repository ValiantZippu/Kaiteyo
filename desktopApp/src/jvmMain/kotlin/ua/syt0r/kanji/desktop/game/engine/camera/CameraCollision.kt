package ua.syt0r.kanji.desktop.game.engine.camera

import ua.syt0r.kanji.desktop.game.engine.geom.Rect
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2

/**
 * Camera collision avoidance (spec §30): the follow camera must never sit
 * inside a solid building. Given the camera's desired focus point and the
 * cell's solid rects, this pushes the point out to the nearest edge — pure
 * and unit-tested, no rendering involved.
 */
object CameraCollision {

    /** Extra breathing room so the lens doesn't hug the wall (world units). */
    const val MARGIN = 12f

    /**
     * Returns [desired], or the nearest point just outside [solids] when it
     * would land inside one. Only the nearest edge matters — a camera being
     * pushed 2 px is indistinguishable; being pushed 200 px is not.
     */
    fun resolve(desired: Vec2, solids: List<Rect>, margin: Float = MARGIN): Vec2 {
        var pos = desired
        for (r in solids) {
            if (pos.x < r.x - margin || pos.x > r.x + r.width + margin ||
                pos.y < r.y - margin || pos.y > r.y + r.height + margin
            ) {
                continue
            }
            val left = pos.x - (r.x - margin)
            val right = (r.x + r.width + margin) - pos.x
            val top = pos.y - (r.y - margin)
            val bottom = (r.y + r.height + margin) - pos.y
            pos = when (minOf(left, right, top, bottom)) {
                left -> Vec2(r.x - margin, pos.y)
                right -> Vec2(r.x + r.width + margin, pos.y)
                top -> Vec2(pos.x, r.y - margin)
                else -> Vec2(pos.x, r.y + r.height + margin)
            }
        }
        return pos
    }
}
