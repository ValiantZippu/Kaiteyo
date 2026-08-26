package ua.syt0r.kanji.desktopApp

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import java.awt.Component
import java.awt.Frame

// ============================================
// WINDOW MESSAGE HANDLER — WM_NCHITTEST
// Intercepts the Windows hit-test message so
// returning HTMAXBUTTON (9) over the maximize
// button triggers the Windows 11 snap layout
// overlay.  Without this, undecorated windows
// never show the snap popup.
//
// Also ensures HTCAPTION is returned for the
// entire title bar drag zone so native snap
// (drag to screen edges) works reliably.
// ============================================

internal object WindowMessageHandler {

    // HT values from winuser.h.
    private const val HTCLIENT = 1
    private const val HTCAPTION = 2
    private const val HTMAXBUTTON = 9
    private const val HTSYSMENU = 3

    // Window messages.
    private const val WM_NCHITTEST = 0x0084

    private interface User32Ext : Library {
        companion object {
            val INSTANCE: User32Ext = Native.load("user32", User32Ext::class.java)
        }
        fun SetWindowSubclass(
            hWnd: WinDef.HWND?,
            pfnSubclassProc: SubclassProc?,
            uIdSubclass: WinDef.UINT_PTR?,
            dwRefData: Pointer?
        ): Boolean

        fun RemoveWindowSubclass(
            hWnd: WinDef.HWND?,
            pfnSubclassProc: SubclassProc?,
            uIdSubclass: WinDef.UINT_PTR?
        ): Boolean

        fun DefSubclassProc(hWnd: WinDef.HWND?, uMsg: Int, wParam: WinDef.WPARAM, lParam: WinDef.LPARAM): Long
    }

    fun interface SubclassProc : com.sun.jna.Callback {
        fun callback(
            hWnd: WinDef.HWND?,
            uMsg: Int,
            wParam: WinDef.WPARAM,
            lParam: WinDef.LPARAM,
            uIdSubclass: WinDef.UINT_PTR?,
            dwRefData: Pointer?
        ): Long
    }

    private var installedSubclass: SubclassProc? = null
    private var installedHwnd: WinDef.HWND? = null
    private const val SUBCLASS_ID = 1

    // Bounds of the maximize button in screen pixels (left, top, right, bottom).
    // Updated by the Compose title bar via onGloballyPositioned.
    @Volatile
    var maximizeButtonBounds: MaximizeButtonBounds? = null

    // Bounds of the title bar drag zone in screen pixels.
    // When the cursor is here (and not over the maximize button),
    // HTCAPTION is returned so Windows snap-to-edge works.
    @Volatile
    var titleBarBounds: MaximizeButtonBounds? = null

    data class MaximizeButtonBounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )

    /**
     * Installs the WM_NCHITTEST subclass on the given AWT Frame.
     * Safe to call multiple times — removes any previous subclass first.
     */
    fun install(frame: Frame) {
        if (!isWindows) return
        uninstall()
        try {
            val hwnd = hwndOf(frame) ?: return
            val subclass = SubclassProc { hWnd, uMsg, wParam, lParam, uIdSubclass, _ ->
                if (uMsg == WM_NCHITTEST) {
                    val result = handleNcHitTest(hWnd, wParam, lParam)
                    if (result != null) return@SubclassProc result
                }
                // Fall through to the default handler for everything else.
                User32Ext.INSTANCE.DefSubclassProc(hWnd, uMsg, wParam, lParam)
            }
            User32Ext.INSTANCE.SetWindowSubclass(
                hwnd,
                subclass,
                WinDef.UINT_PTR(SUBCLASS_ID.toLong()),
                null
            )
            installedSubclass = subclass
            installedHwnd = hwnd
        } catch (_: Throwable) {
            // Best effort — never break the window.
        }
    }

    /** Removes the subclass if installed. */
    fun uninstall() {
        if (!isWindows) return
        val subclass = installedSubclass ?: return
        val hwnd = installedHwnd ?: return
        try {
            User32Ext.INSTANCE.RemoveWindowSubclass(
                hwnd,
                subclass,
                WinDef.UINT_PTR(SUBCLASS_ID.toLong())
            )
        } catch (_: Throwable) {
        }
        installedSubclass = null
        installedHwnd = null
    }

    /**
     * Returns HTMAXBUTTON when the pointer is over the maximize button
     * area (triggers Windows 11 snap layout overlay), or HTCAPTION when
     * the pointer is over the title bar drag zone (enables OS snap-to-edge
     * when dragging the window to screen edges).
     * Returns null to let DefSubclassProc handle everything else.
     */
    private fun handleNcHitTest(
        hWnd: WinDef.HWND?,
        wParam: WinDef.WPARAM,
        lParam: WinDef.LPARAM
    ): Long? {
        // Only override when lParam gives screen coordinates (lParam ≠ 0
        // means the cursor position is encoded as (x, y) in the low/high word).
        if (lParam.toLong() == 0L) return null

        // Extract screen coordinates from lParam.
        val screenX = (lParam.toLong() and 0xFFFF).toInt().toShort().toInt() // low word, signed
        val screenY = ((lParam.toLong() shr 16) and 0xFFFF).toInt().toShort().toInt() // high word, signed

        // Check maximize button first — HTMAXBUTTON triggers the snap layout popup.
        val maxBounds = maximizeButtonBounds
        if (maxBounds != null &&
            screenX in maxBounds.left..maxBounds.right &&
            screenY in maxBounds.top..maxBounds.bottom
        ) {
            return HTMAXBUTTON.toLong()
        }

        // Check title bar drag zone — HTCAPTION enables OS snap-to-edge
        // (drag to screen edges) and native double-click maximize.
        val tbBounds = titleBarBounds
        if (tbBounds != null &&
            screenX in tbBounds.left..tbBounds.right &&
            screenY in tbBounds.top..tbBounds.bottom
        ) {
            return HTCAPTION.toLong()
        }

        return null // Let DefSubclassProc handle everything else.
    }

    /**
     * Resolves the native HWND from the AWT Frame via the internal peer.
     * Same approach as NativeWindowChrome — reflective access to the
     * deprecated `getPeer()` API; any failure returns null.
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
