package ua.syt0r.kanji.presentation.common.ui.kanji

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.BrushSettings
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.BrushType
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.resolveAlpha
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.resolveStrokeCap
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.resolveStrokeJoin
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.resolveStrokeWidth
import kotlin.math.*

actual fun DrawScope.drawKanjiStroke(
    path: Path,
    color: Color,
    width: Float,
    drawProgress: Float?,
    brushSettings: BrushSettings
) {
    val scale = size.maxDimension / KanjiSize
    scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero) {
        when (brushSettings.brushType) {
            BrushType.Calligraphy -> drawCalligraphyStroke(path, color, width, drawProgress, brushSettings)
            BrushType.Pencil -> drawPencilStroke(path, color, width, drawProgress, brushSettings)
            BrushType.Side -> drawSideBrushStroke(path, color, width, drawProgress, brushSettings)
            BrushType.Pen -> drawPenStroke(path, color, width, drawProgress, brushSettings)
        }
    }
}

/**
 * Calligraphy brush: variable width based on stroke direction.
 * Thick on downstrokes, thin on horizontals, medium on diagonals.
 * Creates the authentic fude (筆) / brush-pen feel.
 */
private fun DrawScope.drawCalligraphyStroke(
    path: Path,
    color: Color,
    width: Float,
    drawProgress: Float?,
    brushSettings: BrushSettings
) {
    val alpha = color.alpha * brushSettings.resolveAlpha()
    val baseWidth = brushSettings.resolveStrokeWidth(width)
    val measure = PathMeasure().apply { setPath(path, false) }
    val totalLength = measure.length
    if (totalLength < 1f) return

    val points = samplePath(measure, totalLength, step = 1.5f)
    if (points.size < 2) return

    val progressLimit = drawProgress?.coerceIn(0f, 1f) ?: 1f
    val maxIndex = ((points.size - 1) * progressLimit).toInt().coerceIn(0, points.size - 1)

    // Calculate direction and velocity for each point
    val widths = FloatArray(points.size)
    for (i in points.indices) {
        val w = if (i == 0) {
            directionWidth(points[0], points[min(1, points.size - 1)])
        } else if (i >= points.size - 1) {
            directionWidth(points[i - 1], points[i])
        } else {
            val w1 = directionWidth(points[i - 1], points[i])
            val w2 = directionWidth(points[i], points[i + 1])
            (w1 + w2) * 0.5f
        }
        // Velocity modulation: fast strokes get slightly thinner
        val velocity = if (i > 0) {
            val dx = points[i].x - points[i - 1].x
            val dy = points[i].y - points[i - 1].y
            sqrt(dx * dx + dy * dy)
        } else 0f
        val velFactor = (1f - (velocity / 8f).coerceIn(0f, 0.4f))
        widths[i] = baseWidth * w * velFactor
    }

    // Draw variable-width stroke as filled polygon
    val strokeWidths = widths.map { it.coerceAtLeast(0.5f) }
    drawVariableWidthPath(points, strokeWidths, color, alpha, maxIndex)
}

private fun directionWidth(from: Offset, to: Offset): Float {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val angle = atan2(dy, dx)
    // Calligraphy: thick on vertical (down), thin on horizontal
    // Use abs(sin) for vertical emphasis
    val verticalFactor = abs(sin(angle.toDouble())).toFloat()
    // Range: 0.35 (horizontal) to 1.4 (vertical)
    return 0.35f + verticalFactor * 1.05f
}

/**
 * Draw a path with variable-width strokes by creating a filled outline polygon.
 */
private fun DrawScope.drawVariableWidthPath(
    points: List<Offset>,
    widths: List<Float>,
    color: Color,
    alpha: Float,
    maxIndex: Int
) {
    if (points.size < 2 || maxIndex < 1) return

    val leftPoints = mutableListOf<Offset>()
    val rightPoints = mutableListOf<Offset>()

    for (i in 0..maxIndex) {
        val p = points[i]
        val w = widths[i] * 0.5f

        // Normal vector (perpendicular to stroke direction)
        val normal = if (i == 0) {
            normal(points[0], points[min(1, points.size - 1)])
        } else if (i >= points.size - 1) {
            normal(points[max(0, i - 1)], points[i])
        } else {
            val n1 = normal(points[i - 1], points[i])
            val n2 = normal(points[i], points[i + 1])
            Offset((n1.x + n2.x) * 0.5f, (n1.y + n2.y) * 0.5f).normalized()
        }

        leftPoints.add(Offset(p.x + normal.x * w, p.y + normal.y * w))
        rightPoints.add(Offset(p.x - normal.x * w, p.y - normal.y * w))
    }

    // Build filled path from left side forward, right side backward
    val outlinePath = Path()
    if (leftPoints.isNotEmpty()) {
        outlinePath.moveTo(leftPoints[0].x, leftPoints[0].y)
        for (i in 1 until leftPoints.size) {
            outlinePath.lineTo(leftPoints[i].x, leftPoints[i].y)
        }
        // Connect to right side
        val lastRight = rightPoints.last()
        outlinePath.lineTo(lastRight.x, lastRight.y)
        // Go back along right side
        for (i in rightPoints.size - 2 downTo 0) {
            outlinePath.lineTo(rightPoints[i].x, rightPoints[i].y)
        }
        outlinePath.close()
    }

    // Draw the filled calligraphy path
    drawPath(outlinePath, color, alpha)

    // Add subtle ink-bleed effect at endpoints (rounded caps)
    if (points.size > 1) {
        drawCircle(color, alpha = alpha * 0.7f, radius = widths.first() * 0.5f, center = points.first())
        drawCircle(color, alpha = alpha * 0.7f, radius = widths[maxIndex.coerceAtMost(widths.size - 1)] * 0.5f, center = points[maxIndex])
    }
}

/**
 * Pen stroke: uniform width, smooth round cap.
 */
private fun DrawScope.drawPenStroke(
    path: Path,
    color: Color,
    width: Float,
    drawProgress: Float?,
    brushSettings: BrushSettings
) {
    val alpha = color.alpha * brushSettings.resolveAlpha()
    val strokeWidth = brushSettings.resolveStrokeWidth(width)

    drawPath(
        path = path,
        color = color,
        alpha = alpha,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = drawProgress?.let { progress ->
                val pathLength = PathMeasure().apply { setPath(path, false) }.length
                PathEffect.dashPathEffect(floatArrayOf(pathLength * progress, Float.MAX_VALUE))
            }
        )
    )
}

/**
 * Pencil stroke: slightly rough edge with lower opacity for a pencil feel.
 */
private fun DrawScope.drawPencilStroke(
    path: Path,
    color: Color,
    width: Float,
    drawProgress: Float?,
    brushSettings: BrushSettings
) {
    val alpha = color.alpha * brushSettings.resolveAlpha() * 0.75f
    val strokeWidth = brushSettings.resolveStrokeWidth(width) * 0.8f

    // Base stroke
    drawPath(
        path = path,
        color = color,
        alpha = alpha,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Butt,
            join = StrokeJoin.Bevel,
            pathEffect = drawProgress?.let { progress ->
                val pathLength = PathMeasure().apply { setPath(path, false) }.length
                PathEffect.dashPathEffect(floatArrayOf(pathLength * progress, Float.MAX_VALUE))
            }
        )
    )

    // Subtle secondary stroke for pencil texture (slightly offset)
    drawPath(
        path = path,
        color = color,
        alpha = alpha * 0.3f,
        style = Stroke(
            width = strokeWidth * 0.6f,
            cap = StrokeCap.Butt,
            join = StrokeJoin.Bevel,
            pathEffect = drawProgress?.let { progress ->
                val pathLength = PathMeasure().apply { setPath(path, false) }.length
                PathEffect.dashPathEffect(floatArrayOf(pathLength * progress, Float.MAX_VALUE))
            }
        )
    )
}

/**
 * Side brush: flat square tip, like a wide chisel marker.
 */
private fun DrawScope.drawSideBrushStroke(
    path: Path,
    color: Color,
    width: Float,
    drawProgress: Float?,
    brushSettings: BrushSettings
) {
    val alpha = color.alpha * brushSettings.resolveAlpha()
    val strokeWidth = brushSettings.resolveStrokeWidth(width) * 3f

    drawPath(
        path = path,
        color = color,
        alpha = alpha,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Square,
            join = StrokeJoin.Miter,
            pathEffect = drawProgress?.let { progress ->
                val pathLength = PathMeasure().apply { setPath(path, false) }.length
                PathEffect.dashPathEffect(floatArrayOf(pathLength * progress, Float.MAX_VALUE))
            }
        )
    )
}

// ============================================
// UTILITIES
// ============================================

/** Sample evenly-spaced points along a path. */
private fun samplePath(measure: PathMeasure, totalLength: Float, step: Float): List<Offset> {
    val result = mutableListOf<Offset>()
    var distance = 0f
    while (distance <= totalLength) {
        val pos = measure.getPosition(distance)
        result.add(pos)
        distance += step
    }
    // Ensure last point is included
    val lastPos = measure.getPosition(totalLength)
    if (result.isEmpty() || (result.last().let { abs(it.x - lastPos.x) + abs(it.y - lastPos.y) } > 0.5f)) {
        result.add(lastPos)
    }
    return result
}

/** Calculate perpendicular normal from point a to point b, normalized. */
private fun normal(a: Offset, b: Offset): Offset {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val len = sqrt(dx * dx + dy * dy)
    if (len < 0.001f) return Offset(0f, -1f)
    return Offset(-dy / len, dx / len)
}

/** Normalize this offset to unit length. */
private fun Offset.normalized(): Offset {
    val len = sqrt(x * x + y * y)
    return if (len < 0.001f) Offset(0f, -1f) else Offset(x / len, y / len)
}
