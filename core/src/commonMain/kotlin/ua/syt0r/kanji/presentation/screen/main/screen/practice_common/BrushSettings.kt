package ua.syt0r.kanji.presentation.screen.main.screen.practice_common

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Brush types for kanji drawing.
 */
enum class BrushType(
    val displayName: String,
    val displayNameJa: String
) {
    Pen("Pen", "ペン"),
    Calligraphy("Calligraphy", "筆"),
    Pencil("Pencil", "鉛筆"),
    Side("Side", "サイド");

    companion object {
        val default = Pen
    }
}

/**
 * 2D point with pressure and timestamp for velocity calculation.
 */
data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f,
    val timestampMs: Long = 0L
)

/**
 * Settings that control how strokes appear when drawing kanji.
 *
 * @param brushType The type of brush tip simulation
 * @param thickness Stroke width multiplier (1.0 = default)
 * @param softness Alpha/opacity of the stroke (0.0-1.0)
 * @param smoothingEnabled Enable stroke smoothing (low-pass filter + bezier)
 * @param smoothingFactor Smoothing strength 0.0-1.0 (higher = smoother, lower = responsive)
 * @param predictionEnabled Enable input prediction for lower latency
 * @param predictionPoints Number of points to extrapolate (1-3)
 * @param velocitySmoothingEnabled Enable velocity-based adaptive smoothing
 * @param pressureEnabled Enable pressure sensitivity
 * @param jitterReduction Enable jitter/low-pass filtering
 * @param jitterThreshold Minimum movement to register (pixels, eliminates tremor)
 * @param bezierSmoothing Enable bezier curve interpolation between stroke points
 * @param bezierSegments Number of interpolation segments per pair of points
 */
data class BrushSettings(
    val brushType: BrushType = BrushType.Pen,
    val thickness: Float = 1.0f,
    val softness: Float = 1.0f,
    val smoothingEnabled: Boolean = true,
    val smoothingFactor: Float = 0.5f,
    val predictionEnabled: Boolean = true,
    val predictionPoints: Int = 2,
    val velocitySmoothingEnabled: Boolean = true,
    val pressureEnabled: Boolean = false,
    val jitterReduction: Boolean = true,
    val jitterThreshold: Float = 1.5f,
    val bezierSmoothing: Boolean = true,
    val bezierSegments: Int = 4
) {
    companion object {
        val default = BrushSettings()
    }
}

// ============================================
// STROKE SMOOTHING ENGINE
// ============================================

/**
 * Smooths input points using a moving average low-pass filter.
 * Higher smoothingFactor = smoother but more lag.
 */
fun smoothPoints(
    points: List<StrokePoint>,
    settings: BrushSettings
): List<StrokePoint> {
    if (!settings.smoothingEnabled || points.size < 3) return points

    val factor = settings.smoothingFactor.coerceIn(0f, 0.95f)
    val smoothed = mutableListOf(points.first())

    for (i in 1 until points.size) {
        val prev = smoothed.last()
        val curr = points[i]
        val sx = prev.x + factor * (curr.x - prev.x)
        val sy = prev.y + factor * (curr.y - prev.y)
        smoothed.add(curr.copy(x = sx, y = sy))
    }
    return smoothed
}

/**
 * Applies bezier interpolation between consecutive stroke points.
 * Produces smooth curves instead of sharp line segments.
 */
fun bezierSmooth(
    points: List<StrokePoint>,
    settings: BrushSettings
): List<StrokePoint> {
    if (!settings.bezierSmoothing || points.size < 3) return points

    val segments = settings.bezierSegments.coerceIn(2, 8)
    val result = mutableListOf<StrokePoint>()

    for (i in 0 until points.size - 1) {
        val p0 = if (i == 0) points[i] else points[i - 1]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = if (i + 2 < points.size) points[i + 2] else points[i + 1]

        for (t in 0 until segments) {
            val tt = t.toFloat() / segments
            val x = catmullRom(p0.x, p1.x, p2.x, p3.x, tt)
            val y = catmullRom(p0.y, p1.y, p2.y, p3.y, tt)
            val pressure = lerp(p1.pressure, p2.pressure, tt)
            result.add(StrokePoint(x, y, pressure))
        }
    }
    result.add(points.last())
    return result
}

/**
 * Catmull-Rom spline interpolation for smooth curves.
 */
private fun catmullRom(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
    val t2 = t * t
    val t3 = t2 * t
    return 0.5f * (
        (2f * p1) +
        (-p0 + p2) * t +
        (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2 +
        (-p0 + 3f * p1 - 3f * p2 + p3) * t3
    )
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/**
 * Reduces jitter by filtering points below a distance threshold.
 * Eliminates micro-movements from hand tremor.
 */
fun reduceJitter(
    points: List<StrokePoint>,
    settings: BrushSettings
): List<StrokePoint> {
    if (!settings.jitterReduction || points.size < 2) return points
    val threshold = settings.jitterThreshold.coerceIn(0.5f, 10f)
    val result = mutableListOf(points.first())
    for (i in 1 until points.size) {
        val dx = points[i].x - result.last().x
        val dy = points[i].y - result.last().y
        val distance = sqrt(dx * dx + dy * dy)
        if (distance >= threshold) {
            result.add(points[i])
        }
    }
    if (result.size == 1 && points.size > 1) result.add(points.last())
    return result
}

/**
 * Predicts future input points based on recent velocity vectors.
 * Reduces perceived latency by extrapolating the stroke.
 */
fun predictPoints(
    points: List<StrokePoint>,
    settings: BrushSettings
): List<StrokePoint> {
    if (!settings.predictionEnabled || points.size < 3) return points
    val count = settings.predictionPoints.coerceIn(1, 3)
    val result = points.toMutableList()

    val last = points.last()
    val prev = points[points.size - 2]
    val dx = last.x - prev.x
    val dy = last.y - prev.y
    // Additional velocity from previous pair
    val prev2 = if (points.size >= 3) points[points.size - 3] else prev
    val ddx = prev.x - prev2.x
    val ddy = prev.y - prev2.y
    // Acceleration
    val ax = dx - ddx
    val ay = dy - ddy

    for (i in 1..count) {
        val t = i.toFloat() / count
        val px = last.x + dx * t + 0.5f * ax * t * t
        val py = last.y + dy * t + 0.5f * ay * t * t
        result.add(StrokePoint(px, py, last.pressure))
    }
    return result
}

/**
 * Applies velocity-based adaptive smoothing.
 * Slow strokes get more smoothing, fast strokes get less (more responsive).
 */
fun velocitySmooth(
    points: List<StrokePoint>,
    settings: BrushSettings
): List<StrokePoint> {
    if (!settings.velocitySmoothingEnabled || points.size < 4) return points

    val result = mutableListOf(points.first())
    for (i in 1 until points.size) {
        val curr = points[i]
        val prev = points[i - 1]
        val dx = curr.x - prev.x
        val dy = curr.y - prev.y
        val velocity = sqrt(dx * dx + dy * dy)

        // Map velocity to smoothing factor: fast = less smoothing
        val velFactor = (velocity / 20f).coerceIn(0f, 1f)
        val adaptiveSmooth = 0.3f + (1f - velFactor) * 0.4f // 0.3 to 0.7

        val sx = prev.x + adaptiveSmooth * (curr.x - prev.x)
        val sy = prev.y + adaptiveSmooth * (curr.y - prev.y)
        result.add(curr.copy(x = sx, y = sy))
    }
    return result
}

/**
 * Full stroke processing pipeline: jitter reduction → smoothing → velocity smoothing → prediction → bezier
 */
fun processStroke(
    points: List<StrokePoint>,
    settings: BrushSettings
): List<StrokePoint> {
    if (points.size < 2) return points

    var processed = points
    processed = reduceJitter(processed, settings)
    processed = smoothPoints(processed, settings)
    processed = velocitySmooth(processed, settings)
    processed = predictPoints(processed, settings)
    processed = bezierSmooth(processed, settings)
    return processed
}

/**
 * Resolves drawing attributes from brush settings.
 */
fun BrushSettings.resolveStrokeCap(): StrokeCap = when (brushType) {
    BrushType.Pen -> StrokeCap.Round
    BrushType.Calligraphy -> StrokeCap.Round
    BrushType.Pencil -> StrokeCap.Butt
    BrushType.Side -> StrokeCap.Square
}

fun BrushSettings.resolveStrokeJoin(): StrokeJoin = when (brushType) {
    BrushType.Pen -> StrokeJoin.Round
    BrushType.Calligraphy -> StrokeJoin.Round
    BrushType.Pencil -> StrokeJoin.Bevel
    BrushType.Side -> StrokeJoin.Round
}

fun BrushSettings.resolveStrokeWidth(baseWidth: Float): Float {
    return baseWidth * thickness * when (brushType) {
        BrushType.Pen -> 1.0f
        BrushType.Calligraphy -> 1.5f
        BrushType.Pencil -> 0.8f
        BrushType.Side -> 3.0f
    }
}

fun BrushSettings.resolveAlpha(): Float = softness.coerceIn(0.1f, 1.0f)

/**
 * Calculates stroke width at a point considering pressure (if enabled).
 */
fun BrushSettings.resolvePressureWidth(
    baseWidth: Float,
    pressure: Float
): Float {
    if (!pressureEnabled) return resolveStrokeWidth(baseWidth)
    val pressureFactor = pressure.coerceIn(0.1f, 1.0f)
    val minWidth = resolveStrokeWidth(baseWidth) * 0.3f
    val maxWidth = resolveStrokeWidth(baseWidth) * 1.5f
    return minWidth + (maxWidth - minWidth) * pressureFactor
}
