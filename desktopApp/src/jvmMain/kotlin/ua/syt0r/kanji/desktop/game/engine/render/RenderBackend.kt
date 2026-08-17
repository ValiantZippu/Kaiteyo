package ua.syt0r.kanji.desktop.game.engine.render

import ua.syt0r.kanji.desktop.game.engine.geom.Vec2

/**
 * The render boundary of the game engine.
 *
 * This is the documented swap point for a real 3D engine (Orx/KorGE, libGDX,
 * or a future native engine — see `docs/game/ENGINE_DECISION.md`): the game
 * core never touches pixels directly, it emits a declarative frame through
 * this interface. The vertical slice ships a Compose-Canvas implementation
 * ([CanvasRenderer]); a 3D integration replaces [RenderBackend] with an
 * engine-specific adapter (scene graph → mesh/sprite batches) while the rest
 * of the game core stays unchanged.
 *
 * All coordinates are screen-space; the [Camera] transform is applied before
 * calls arrive. Text uses raw Japanese strings — rendering support is the
 * backend's job (Compose Canvas handles Japanese glyphs natively).
 */
interface RenderBackend {

    fun drawRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: RenderColor,
        cornerRadius: Float = 0f
    )

    fun drawCircle(
        center: Vec2,
        radius: Float,
        color: RenderColor
    )

    fun drawEllipse(
        center: Vec2,
        radiusX: Float,
        radiusY: Float,
        color: RenderColor
    )

    fun drawLine(
        from: Vec2,
        to: Vec2,
        color: RenderColor,
        strokeWidth: Float = 2f
    )

    fun drawPolyline(
        points: List<Vec2>,
        color: RenderColor,
        strokeWidth: Float = 2f,
        close: Boolean = false
    )

    fun drawText(
        text: String,
        at: Vec2,
        size: Float,
        color: RenderColor,
        centered: Boolean = true,
        maxWidth: Float? = null
    )

    /** Push a temporary global tint applied to everything drawn after it. */
    fun withTint(tint: RenderColor, block: () -> Unit)

    fun measureText(text: String, size: Float): Vec2
}

/** A simple RGBA color — keeps the render core free of Compose/engine types. */
data class RenderColor(
    val r: Float,
    val g: Float,
    val b: Float,
    val a: Float = 1f
) {
    fun withAlpha(alpha: Float): RenderColor = copy(a = alpha.coerceIn(0f, 1f))

    fun blend(over: RenderColor, amount: Float): RenderColor {
        val t = amount.coerceIn(0f, 1f)
        return RenderColor(
            r + (over.r - r) * t,
            g + (over.g - g) * t,
            b + (over.b - b) * t,
            a
        )
    }

    companion object {
        val White = RenderColor(1f, 1f, 1f)
        val Black = RenderColor(0f, 0f, 0f)

        fun rgb(hex: Int): RenderColor = RenderColor(
            ((hex shr 16) and 0xFF) / 255f,
            ((hex shr 8) and 0xFF) / 255f,
            (hex and 0xFF) / 255f
        )

        fun rgba(hex: Int, alpha: Float = 1f): RenderColor =
            rgb(hex).withAlpha(alpha)
    }
}
