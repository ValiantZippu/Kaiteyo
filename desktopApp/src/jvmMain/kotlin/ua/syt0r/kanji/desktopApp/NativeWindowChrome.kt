package ua.syt0r.kanji.desktopApp

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef
import java.awt.Component
import java.awt.Frame

// ============================================
// NATIVE WINDOW CHROME — Windows 11 rounding
// Hands DWM the window's corner preference and
// border color so the OS renders the actual
// rounded window with a theme-colored hairline,
// instead of a fake in-app rounding. Best
// effort: any failure leaves the app's own
// rounded-surface fallback untouched.
// ============================================

internal object NativeWindowChrome {

    // DWMWINDOWATTRIBUTE values (dwmapi.h).
    private const val DWMWA_WINDOW_CORNER_PREFERENCE = 33
    private const val DWMWA_BORDER_COLOR = 34
    // DWMWCP corner-preference values.
    private const val DWMWCP_DONOTROUND = 1
    private const val DWMWCP_ROUND = 2

    private interface Dwmapi : Library {
        fun DwmSetWindowAttribute(hwnd: WinDef.HWND, dwAttribute: Int, pvAttribute: Pointer, cbAttribute: Int): Int

        companion object {
            val INSTANCE: Dwmapi = Native.load("dwmapi", Dwmapi::class.java)
        }
    }

    /**
     * Applies the native window presentation for the current window state and
     * theme: DWM-rounded corners (square while maximized, matching the OS) and
     * a theme-colored hairline border on Windows 11.
     *
     * @return `false` when the HWND is not available yet (the frame is not
     *   realized) and the caller should retry shortly; `true` when applied,
     *   not applicable (non-Windows / older builds) or refused by DWM.
     */
    fun update(frame: Frame, isMaximized: Boolean, borderColorArgb: Int): Boolean {
        if (!isWindows) return true
        if (!isWindows11) return true
        return try {
            val hwnd = hwndOf(frame) ?: return false
            val preference = if (isMaximized) DWMWCP_DONOTROUND else DWMWCP_ROUND
            Dwmapi.INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, intPointer(preference), 4)
            if (!isMaximized) {
                Dwmapi.INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_BORDER_COLOR, intPointer(argbToColorRef(borderColorArgb)), 4)
            }
            true
        } catch (_: Throwable) {
            // DWM is optional chrome; never let it break the window, and do
            // not retry a refused call.
            true
        }
    }

    private fun intPointer(value: Int): Pointer =
        Memory(4).apply { setInt(0, value) }

    /** ARGB → COLORREF (0x00BBGGRR), the byte order DWM expects. */
    private fun argbToColorRef(argb: Int): Int {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (b shl 16) or (g shl 8) or r
    }

    /**
     * Windows 11 detection without JNA version-gated helpers: Windows 11 is
     * build 22000+ (older releases were also branded "Windows 10" up to
     * build 19045, so the build number is the reliable discriminator).
     */
    private val isWindows11: Boolean by lazy {
        val name = System.getProperty("os.name") ?: ""
        val build = System.getProperty("os.version")
            ?.substringBefore('.')
            ?.toIntOrNull() ?: 0
        name.startsWith("Windows 11", ignoreCase = true) ||
            (name.startsWith("Windows", ignoreCase = true) && build >= 22000)
    }

    /**
     * HWND of the AWT [Frame], resolved through the internal WComponentPeer.
     * `Component.getPeer()` is deprecated and module-restricted on JDK 9+,
     * so it is accessed reflectively; any failure (modules, removed internals)
     * degrades to null and the caller simply skips the native chrome — this
     * is best-effort presentation, never a hard dependency.
     */
    private fun hwndOf(frame: Frame): WinDef.HWND? = try {
        val getPeer = Component::class.java.getDeclaredMethod("getPeer")
        getPeer.isAccessible = true
        val peer = getPeer.invoke(frame) ?: return null
        val value = Class.forName("sun.awt.windows.WComponentPeer")
            .getMethod("getHWnd")
            .invoke(peer)
        val hwnd = when (value) {
            is Long -> value
            is Int -> value.toLong()
            else -> return null
        }
        if (hwnd == 0L) null else WinDef.HWND(Pointer.createConstant(hwnd))
    } catch (_: Throwable) {
        null
    }
}
