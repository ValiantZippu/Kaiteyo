package ua.syt0r.kanji.desktopApp

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.GraphicsEnvironment
import java.io.File

// ============================================
// WINDOW STATE STORE
// Remembers the floating window's size and
// position across launches so the app reopens
// where the user left it. Stored alongside the
// other app data in ~/.kaiteyo/window.json.
// ============================================

@Serializable
data class SavedWindowBounds(
    val width: Int = 0,
    val height: Int = 0,
    val x: Int? = null,
    val y: Int? = null
) {
    val isUsable: Boolean get() = width > 0 && height > 0
}

object WindowStateStore {

    private val json = Json { ignoreUnknownKeys = true }

    private val file: File
        get() = File(System.getProperty("user.home"), ".kaiteyo/window.json")

    /**
     * Loads the saved bounds. The position is dropped when it no longer
     * intersects the usable screen area (monitor changed, display rotated…),
     * so the window can never reopen off-screen; the size is kept.
     */
    fun load(): SavedWindowBounds {
        val saved = runCatching {
            if (file.exists()) json.decodeFromString<SavedWindowBounds>(file.readText())
            else SavedWindowBounds()
        }.getOrDefault(SavedWindowBounds())
        if (!saved.isUsable) return SavedWindowBounds()
        val x = saved.x ?: return SavedWindowBounds()
        val y = saved.y ?: return SavedWindowBounds()
        val screen = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
        val onScreen = x < screen.x + screen.width - 100 &&
            y < screen.y + screen.height - 100 &&
            x + saved.width > screen.x + 100 &&
            y + saved.height > screen.y + 100
        return if (onScreen) saved else saved.copy(x = null, y = null)
    }

    fun save(bounds: SavedWindowBounds) = runCatching {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(bounds))
    }
}
