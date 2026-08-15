package ua.syt0r.kanji.desktopApp

import java.awt.GraphicsDevice
import java.awt.GraphicsEnvironment
import java.awt.Insets
import java.awt.Rectangle
import java.awt.Toolkit

// ============================================
// WINDOW WORK AREA
// The usable desktop region: the display's full
// bounds minus the OS-reserved areas (Windows
// taskbar on any edge, macOS menu bar + dock,
// Linux panels the toolkit reports). All values
// are in AWT device pixels — on a
// per-monitor-DPI-aware JVM (JDK 9+ default)
// that is the physical pixel space Compose
// converts to dp with the window's density.
// ============================================

data class WorkArea(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    val right: Int get() = x + width
    val bottom: Int get() = y + height

    fun asRectangle(): Rectangle = Rectangle(x, y, width, height)

    /** Intersection area with [rect], in px² (0 when they do not overlap). */
    fun intersectionArea(rect: Rectangle): Int {
        val a = asRectangle()
        val w = minOf(a.x + a.width, rect.x + rect.width) - maxOf(a.x, rect.x)
        val h = minOf(a.y + a.height, rect.y + rect.height) - maxOf(a.y, rect.y)
        return if (w > 0 && h > 0) w * h else 0
    }

    /**
     * Clamps [rect] so it stays fully inside this work area. The size is
     * capped to the work area (a window can never be larger than the usable
     * desktop), and the position is kept where it was when it already fits
     * (top-left anchored).
     */
    fun clampRect(rect: Rectangle): Rectangle {
        val w = minOf(rect.width, width)
        val h = minOf(rect.height, height)
        val cx = rect.x.coerceIn(x, right - w)
        val cy = rect.y.coerceIn(y, bottom - h)
        return Rectangle(cx, cy, w, h)
    }
}

/**
 * Platform work-area queries. AWT is the single source of truth here: it
 * reports every display in the same device-pixel coordinate space the window
 * shell works in, so multi-monitor, per-monitor DPI and taskbar-on-any-edge
 * cases all resolve through one path instead of per-platform hacks.
 */
object WindowWorkAreas {

    private fun intersectionArea(a: Rectangle, b: Rectangle): Int {
        val w = minOf(a.x + a.width, b.x + b.width) - maxOf(a.x, b.x)
        val h = minOf(a.y + a.height, b.y + b.height) - maxOf(a.y, b.y)
        return if (w > 0 && h > 0) w * h else 0
    }

    /** All displays' usable areas, in AWT device pixels. */
    fun enumerate(): List<WorkArea> = runCatching {
        GraphicsEnvironment.getLocalGraphicsEnvironment()
            .screenDevices
            .mapNotNull { device ->
                val bounds = device.defaultConfiguration.bounds
                if (bounds.width <= 0 || bounds.height <= 0) return@mapNotNull null
                val work = workAreaOf(device)
                if (work.width <= 0 || work.height <= 0) null else work
            }
    }.getOrDefault(emptyList())

    /** The primary display's usable area (falls back to its full bounds). */
    fun primary(): WorkArea {
        val areas = enumerate()
        if (areas.isNotEmpty()) return areas.first()
        val bounds = runCatching {
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                .defaultScreenDevice.defaultConfiguration.bounds
        }.getOrDefault(Rectangle(0, 0, 1920, 1080))
        return WorkArea(bounds.x, bounds.y, bounds.width, bounds.height)
    }

    /**
     * The work area of the display the window [bounds] lives on: the display
     * with the largest overlap wins; when the window overlaps nothing (a
     * monitor was disconnected), the display whose center is nearest wins so
     * the window recovers onto a valid screen instead of a dead one.
     */
    fun forBounds(bounds: Rectangle): WorkArea {
        val devices = runCatching {
            GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.toList()
        }.getOrDefault(emptyList())
        if (devices.isEmpty()) return primary()

        val best = devices.maxWithOrNull(
            Comparator { a, b ->
                val areaA = intersectionArea(a.defaultConfiguration.bounds, bounds)
                val areaB = intersectionArea(b.defaultConfiguration.bounds, bounds)
                if (areaA != areaB) areaA.compareTo(areaB)
                else centerDistanceSquared(b, bounds).compareTo(centerDistanceSquared(a, bounds))
            }
        ) ?: devices.first()

        return workAreaOf(best)
    }

    /**
     * Scale factor (dp per AWT device pixel) of the display containing
     * [bounds], for DPI-safe conversions outside a Compose window (e.g. the
     * startup store, which must convert stored pixels to dp before the window
     * exists to apply its own density).
     */
    fun densityFor(bounds: Rectangle): Float {
        val device = runCatching {
            GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
                .maxByOrNull { intersectionArea(it.defaultConfiguration.bounds, bounds) }
                ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
        }.getOrNull() ?: return 1f
        val scale = runCatching { device.defaultConfiguration.defaultTransform.scaleX }
            .getOrNull() ?: 1.0
        return scale.coerceAtLeast(1.0).toFloat()
    }

    private fun workAreaOf(device: GraphicsDevice): WorkArea {
        val config = device.defaultConfiguration
        val bounds = config.bounds
        val insets = runCatching { Toolkit.getDefaultToolkit().getScreenInsets(config) }
            .getOrElse { Insets(0, 0, 0, 0) }
        return WorkArea(
            x = bounds.x + insets.left,
            y = bounds.y + insets.top,
            width = (bounds.width - insets.left - insets.right).coerceAtLeast(0),
            height = (bounds.height - insets.top - insets.bottom).coerceAtLeast(0)
        )
    }

    private fun centerDistanceSquared(device: GraphicsDevice, bounds: Rectangle): Long {
        val db = device.defaultConfiguration.bounds
        val dcx = db.x + db.width / 2
        val dcy = db.y + db.height / 2
        val cx = bounds.x + bounds.width / 2
        val cy = bounds.y + bounds.height / 2
        val dx = (dcx - cx).toLong()
        val dy = (dcy - cy).toLong()
        return dx * dx + dy * dy
    }
}
