package ua.syt0r.kanji.desktop.game.engine.geom

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Immutable 2D vector used for all world-space positions, velocities and
 * sizes in the game engine. World units are "world pixels" at zoom 1.
 */
@kotlinx.serialization.Serializable
data class Vec2(val x: Float, val y: Float) {

    operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)
    operator fun times(scale: Float): Vec2 = Vec2(x * scale, y * scale)
    operator fun times(other: Vec2): Vec2 = Vec2(x * other.x, y * other.y)
    operator fun div(scale: Float): Vec2 = Vec2(x / scale, y / scale)
    operator fun unaryMinus(): Vec2 = Vec2(-x, -y)

    fun lengthSquared(): Float = x * x + y * y

    fun length(): Float = sqrt(lengthSquared())

    fun distanceTo(other: Vec2): Float = (this - other).length()

    fun normalized(): Vec2 {
        val len = length()
        return if (len == 0f) Vec2.Zero else Vec2(x / len, y / len)
    }

    fun dot(other: Vec2): Float = x * other.x + y * other.y

    fun lerp(other: Vec2, t: Float): Vec2 = Vec2(x + (other.x - x) * t, y + (other.y - y) * t)

    fun withX(newX: Float): Vec2 = Vec2(newX, y)
    fun withY(newY: Float): Vec2 = Vec2(x, newY)

    /** Screen-space convenience: a vector is "roughly axis aligned". */
    fun isNearlyAxisAligned(epsilon: Float = 0.01f): Boolean =
        abs(x) < epsilon || abs(y) < epsilon

    companion object {
        val Zero = Vec2(0f, 0f)
        val One = Vec2(1f, 1f)
        val Up = Vec2(0f, -1f)
        val Down = Vec2(0f, 1f)
        val Left = Vec2(-1f, 0f)
        val Right = Vec2(1f, 0f)

        fun lerp(from: Vec2, to: Vec2, t: Float): Vec2 = from.lerp(to, t)
    }
}

/** Axis-aligned rectangle in world space. */
data class Rect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    val right: Float get() = x + width
    val bottom: Float get() = y + height
    val center: Vec2 get() = Vec2(x + width / 2f, y + height / 2f)
    val topLeft: Vec2 get() = Vec2(x, y)

    fun contains(point: Vec2): Boolean =
        point.x >= x && point.x <= right && point.y >= y && point.y <= bottom

    fun overlaps(other: Rect): Boolean =
        x < other.right && right > other.x && y < other.bottom && bottom > other.y

    fun intersects(other: Rect): Boolean = overlaps(other)

    fun expanded(amount: Float): Rect =
        Rect(x - amount, y - amount, width + amount * 2f, height + amount * 2f)

    fun translated(offset: Vec2): Rect =
        Rect(x + offset.x, y + offset.y, width, height)

    fun clampedTo(bounds: Rect): Rect =
        Rect(
            min(max(x, bounds.x), bounds.right - width),
            min(max(y, bounds.y), bounds.bottom - height),
            width,
            height
        )

    companion object {
        fun fromCenter(center: Vec2, width: Float, height: Float): Rect =
            Rect(center.x - width / 2f, center.y - height / 2f, width, height)

        fun between(a: Vec2, b: Vec2): Rect {
            val x = min(a.x, b.x)
            val y = min(a.y, b.y)
            return Rect(x, y, abs(a.x - b.x), abs(a.y - b.y))
        }
    }
}

fun clamp(value: Float, min: Float, max: Float): Float = when {
    value < min -> min
    value > max -> max
    else -> value
}

fun clamp(value: Int, min: Int, max: Int): Int = when {
    value < min -> min
    value > max -> max
    else -> value
}

/** Frame-rate independent smoothing factor. */
fun smoothingFactor(rate: Float, dt: Float): Float = 1f - kotlin.math.exp(-rate * dt)
