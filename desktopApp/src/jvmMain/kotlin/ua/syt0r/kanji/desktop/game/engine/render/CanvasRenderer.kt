package ua.syt0r.kanji.desktop.game.engine.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2
import kotlin.math.min

/**
 * [RenderBackend] implementation that draws into a Compose [DrawScope].
 * This is the vertical-slice renderer: a real, working 2D game renderer on
 * top of the Compose Canvas surface. Japanese glyphs are drawn through
 * Compose's text stack, so all world signage renders correctly.
 *
 * The [TextMeasurer] is created by the host composable (composition-scoped)
 * and handed in each frame — the renderer itself stays a plain object.
 */
class CanvasRenderer(
    private val scope: DrawScope,
    private val textMeasurer: TextMeasurer
) : RenderBackend {

    private val tintStack = ArrayDeque<RenderColor>()

    private fun effective(color: RenderColor): Color {
        var c = color
        for (tint in tintStack) {
            // Tint is applied as a screen-like overlay toward the tint color.
            c = c.blend(tint, min(tint.a, 1f) * 0.85f)
        }
        return Color(c.r, c.g, c.b, c.a)
    }

    override fun drawRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: RenderColor,
        cornerRadius: Float
    ) {
        val c = effective(color)
        if (cornerRadius <= 0f) {
            scope.drawRect(c, topLeft = Offset(x, y), size = Size(width, height))
        } else {
            scope.drawRoundRect(
                color = c,
                topLeft = Offset(x, y),
                size = Size(width, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
            )
        }
    }

    override fun drawCircle(center: Vec2, radius: Float, color: RenderColor) {
        scope.drawCircle(effective(color), radius = radius, center = Offset(center.x, center.y))
    }

    override fun drawEllipse(center: Vec2, radiusX: Float, radiusY: Float, color: RenderColor) {
        scope.drawOval(
            effective(color),
            topLeft = Offset(center.x - radiusX, center.y - radiusY),
            size = Size(radiusX * 2f, radiusY * 2f)
        )
    }

    override fun drawLine(from: Vec2, to: Vec2, color: RenderColor, strokeWidth: Float) {
        scope.drawLine(
            effective(color),
            start = Offset(from.x, from.y),
            end = Offset(to.x, to.y),
            strokeWidth = strokeWidth
        )
    }

    override fun drawPolyline(
        points: List<Vec2>,
        color: RenderColor,
        strokeWidth: Float,
        close: Boolean
    ) {
        if (points.size < 2) return
        val path = Path()
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        if (close) path.close()
        scope.drawPath(path, effective(color), style = Stroke(width = strokeWidth))
    }

    override fun drawText(
        text: String,
        at: Vec2,
        size: Float,
        color: RenderColor,
        centered: Boolean,
        maxWidth: Float?
    ) {
        if (text.isBlank()) return
        val layout = textMeasurer.measure(
            text = text,
            style = TextStyle(
                color = effective(color),
                fontSize = size.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            softWrap = false,
            constraints = Constraints(maxWidth = (maxWidth ?: Float.MAX_VALUE).toInt().coerceAtLeast(1))
        )
        val dx = if (centered) -layout.size.width / 2f else 0f
        val dy = if (centered) -layout.size.height / 2f else 0f
        scope.drawText(
            textLayoutResult = layout,
            topLeft = Offset(at.x + dx, at.y + dy)
        )
    }

    override fun withTint(tint: RenderColor, block: () -> Unit) {
        tintStack.addLast(tint)
        try {
            block()
        } finally {
            tintStack.removeLastOrNull()
        }
    }

    override fun measureText(text: String, size: Float): Vec2 {
        val layout = textMeasurer.measure(
            text = text,
            style = TextStyle(fontSize = size.sp, fontWeight = FontWeight.Medium)
        )
        return Vec2(layout.size.width.toFloat(), layout.size.height.toFloat())
    }
}
