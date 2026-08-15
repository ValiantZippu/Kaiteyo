package ua.syt0r.kanji.desktopApp

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.Rectangle
import java.io.File
import kotlin.math.roundToInt

// ============================================
// WINDOW STATE STORE
// Remembers the window's size, position and
// maximize state across launches. Geometry is
// validated and clamped against the OS work
// area (taskbar on any edge, macOS menu bar +
// dock) of the display the window was on, so
// the app can never reopen under the taskbar,
// off-screen, or bigger than the usable desktop.
// ============================================

@Serializable
data class SavedWindowBounds(
    val width: Int = 0,
    val height: Int = 0,
    val x: Int? = null,
    val y: Int? = null,
    val maximized: Boolean = false
) {
    val isUsable: Boolean get() = width > 0 && height > 0
}

/**
 * Startup geometry expressed in dp for [androidx.compose.ui.window.rememberWindowState]
 * (position null = let the OS choose).
 */
data class WindowStartupBounds(
    val widthDp: Float,
    val heightDp: Float,
    val xDp: Float?,
    val yDp: Float?,
    val maximized: Boolean
)

object WindowStateStore {

    private val json = Json { ignoreUnknownKeys = true }

    private val file: File
        get() = File(System.getProperty("user.home"), ".kaiteyo/window.json")

    /**
     * Loads the saved geometry, corrects it against the current display
     * topology and returns it in dp, converted through the target display's
     * scale so DPI changes between sessions are handled.
     *
     * A saved position that no longer intersects any display (monitor
     * disconnected, layout changed) or that would sit under a taskbar /
     * menu bar is corrected into the nearest valid work area. The window is
     * never larger than the display's usable area. First run returns the
     * default size centered on the primary display's work area.
     */
    fun load(): WindowStartupBounds {
        val saved = read()
        if (!saved.isUsable) return defaultStartup()
        val x = saved.x ?: return defaultStartup()
        val y = saved.y ?: return defaultStartup()

        val savedRect = Rectangle(x, y, saved.width, saved.height)
        val workArea = WindowWorkAreas.forBounds(savedRect)
        val clamped = workArea.clampRect(savedRect)
        val density = WindowWorkAreas.densityFor(savedRect)
        return WindowStartupBounds(
            widthDp = clamped.width / density,
            heightDp = clamped.height / density,
            xDp = clamped.x / density,
            yDp = clamped.y / density,
            maximized = saved.maximized
        )
    }

    /**
     * First run (or unusable saved state): the default size centered on the
     * primary display's usable area, so the window always appears inside the
     * work area — never under a top taskbar.
     */
    private fun defaultStartup(): WindowStartupBounds {
        val workArea = WindowWorkAreas.primary()
        val density = WindowWorkAreas.densityFor(workArea.asRectangle())
        val width = (WindowConstraints.DefaultWidth.value * density).roundToInt()
            .coerceAtMost(workArea.width)
        val height = (WindowConstraints.DefaultHeight.value * density).roundToInt()
            .coerceAtMost(workArea.height)
        val x = workArea.x + (workArea.width - width) / 2
        val y = workArea.y + (workArea.height - height) / 2
        return WindowStartupBounds(
            widthDp = width / density,
            heightDp = height / density,
            xDp = x / density,
            yDp = y / density,
            maximized = false
        )
    }

    /** The raw saved geometry (unclamped, AWT pixels) — used by profile backups. */
    fun read(): SavedWindowBounds = runCatching {
        if (file.exists()) json.decodeFromString<SavedWindowBounds>(file.readText())
        else SavedWindowBounds()
    }.getOrDefault(SavedWindowBounds())

    fun save(bounds: SavedWindowBounds) = runCatching {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(bounds))
    }
}
