package ua.syt0r.kanji.desktopApp

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.Structure
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import java.awt.Frame
import java.util.Locale

// ============================================
// NATIVE WINDOW DRAG
// Compose's WindowDraggableArea moves the window
// manually on every pointer event, which feels
// laggy at speed (event coalescing + full
// repaint per move). Handing the drag to the OS
// instead gives 1:1 cursor tracking:
//
//   Windows — WM_NCLBUTTONDOWN / HTCAPTION, the
//             same path native title bars use
//             (also gives native double-click
//             maximize/restore and snap).
//   Linux   — EWMH _NET_WM_MOVERESIZE client
//             message; the window manager runs
//             the native move.
//
// On any failure the caller falls back to
// Compose's pointer-based dragging, so the title
// bar stays draggable everywhere.
// ============================================

internal val isWindows: Boolean = osName().startsWith("windows")
internal val isLinux: Boolean = osName().contains("linux")
internal val isMacOS: Boolean = osName().contains("mac")

/** True where the OS can take over the drag natively. */
internal val isNativeDragAvailable: Boolean = isWindows || isLinux

private fun osName(): String =
    System.getProperty("os.name").orEmpty().lowercase(Locale.ROOT)

/**
 * Starts an OS-native window drag from the current foreground window.
 *
 * The caller should invoke this on pointer-press inside the draggable title
 * region. When it returns `true` the OS owns the gesture. When it returns
 * `false` the caller falls back to Compose's pointer-based dragging.
 */
internal fun startNativeWindowDrag(window: Frame): Boolean {
    return when {
        isWindows -> dragWindows()
        isLinux -> dragLinux(window)
        else -> false
    }
}

/** Minimal WinUser surface for the capture release not exposed by JNA's User32. */
private interface NativeUser32 : Library {
    companion object {
        val INSTANCE: NativeUser32 = Native.load("user32", NativeUser32::class.java)
    }

    fun ReleaseCapture(): Boolean
}

// ------------------------------------------------------------
// Windows — WM_NCLBUTTONDOWN / HTCAPTION
// ------------------------------------------------------------

private fun dragWindows(): Boolean {
    return try {
        val user32 = User32.INSTANCE
        val hwnd: WinDef.HWND = user32.GetForegroundWindow()
        if (hwnd == null) return false
        // AWT holds the mouse capture while the button is down; release it so
        // the OS drag loop can take over.
        NativeUser32.INSTANCE.ReleaseCapture()
        // WM_NCLBUTTONDOWN = 0x00A1 with HTCAPTION = 0x0002.
        user32.SendMessage(hwnd, 0x00A1, WinDef.WPARAM(0x0002), WinDef.LPARAM(0))
        true
    } catch (_: Throwable) {
        false
    }
}

// ------------------------------------------------------------
// Linux — EWMH _NET_WM_MOVERESIZE via X11
// ------------------------------------------------------------

private interface X11 : Library {
    companion object {
        val INSTANCE: X11 = Native.load("X11", X11::class.java)

        const val CLIENT_MESSAGE = 33
        const val SUBSTRUCTURE_REDIRECT_MASK = 0x00100000L
        const val SUBSTRUCTURE_NOTIFY_MASK = 0x00080000L
        const val NET_WM_MOVERESIZE_MOVE = 8L
    }

    fun XOpenDisplay(display_name: String?): Pointer?
    fun XCloseDisplay(display: Pointer?): Int
    fun XFlush(display: Pointer?): Int
    fun XFree(data: Pointer?): Int
    fun XDefaultRootWindow(display: Pointer?): Long
    fun XInternAtom(display: Pointer?, atom_name: String?, only_if_exists: Int): Long
    fun XSetErrorHandler(handler: XErrorHandler?): XErrorHandler?

    fun XQueryTree(
        display: Pointer?,
        w: Long,
        root_return: LongArray,
        parent_return: LongArray,
        children_return: PointerByReference,
        nchildren_return: IntArray
    ): Int

    fun XFetchName(display: Pointer?, w: Long, window_name_return: PointerByReference): Int

    fun XGetInputFocus(display: Pointer?, focus_return: LongArray, revert_to_return: IntArray): Int

    fun XQueryPointer(
        display: Pointer?,
        w: Long,
        root_return: LongArray,
        child_return: LongArray,
        root_x_return: IntArray,
        root_y_return: IntArray,
        win_x_return: IntArray,
        win_y_return: IntArray,
        mask_return: IntArray
    ): Int

    fun XSendEvent(display: Pointer?, w: Long, propagate: Int, event_mask: Long, event_send: XClientMessageEvent): Int
}

/** No-op X error handler: suppresses BadWindow races during window scans. */
internal fun interface XErrorHandler : Callback {
    fun handle(display: Pointer?, event: Pointer?): Int
}

private val IGNORE_X_ERRORS = XErrorHandler { _, _ -> 0 }

@Structure.FieldOrder("type", "serial", "send_event", "display", "window", "message_type", "format", "data")
private class XClientMessageEvent : Structure() {
    @JvmField var type: Int = 0
    @JvmField var serial: Long = 0
    @JvmField var send_event: Int = 0
    @JvmField var display: Pointer? = null
    @JvmField var window: Long = 0
    @JvmField var message_type: Long = 0
    @JvmField var format: Int = 0
    @JvmField var data: LongArray = LongArray(5)
}

private fun dragLinux(window: Frame): Boolean {
    return try {
        val x11 = X11.INSTANCE
        val display = x11.XOpenDisplay(null) ?: return false
        try {
            // Suppress X errors while we scan the tree (windows can be
            // destroyed mid-scan by the WM); restore the toolkit's handler
            // right after, so nothing else is affected.
            val previousHandler = x11.XSetErrorHandler(IGNORE_X_ERRORS)
            try {
                val root = x11.XDefaultRootWindow(display)
                val moveAtom = x11.XInternAtom(display, "_NET_WM_MOVERESIZE", 0)
                if (moveAtom == 0L) return false

                val target = findWindowId(x11, display, root, window.title)
                    ?: return false

                val rootX = IntArray(1)
                val rootY = IntArray(1)
                val pointerOk = x11.XQueryPointer(
                    display, root,
                    LongArray(1), LongArray(1),
                    rootX, rootY, IntArray(1), IntArray(1), IntArray(1)
                )
                if (pointerOk == 0) return false

                val event = XClientMessageEvent()
                event.type = X11.CLIENT_MESSAGE
                event.window = target
                event.message_type = moveAtom
                event.format = 32
                event.data[0] = 1 // source: normal application
                event.data[1] = rootX[0].toLong()
                event.data[2] = rootY[0].toLong()
                event.data[3] = X11.NET_WM_MOVERESIZE_MOVE
                event.data[4] = 0 // button
                event.write()

                val sent = x11.XSendEvent(
                    display, root, 0,
                    X11.SUBSTRUCTURE_REDIRECT_MASK or X11.SUBSTRUCTURE_NOTIFY_MASK,
                    event
                )
                x11.XFlush(display)
                sent != 0
            } finally {
                x11.XSetErrorHandler(previousHandler)
            }
        } finally {
            x11.XCloseDisplay(display)
        }
    } catch (_: Throwable) {
        false
    }
}

/**
 * Locates the X window whose WM_NAME contains [title]. Checks the focused
 * window and its ancestors first (the common case), then falls back to a
 * breadth-first scan of the whole tree for reparenting window managers.
 */
private fun findWindowId(x11: X11, display: Pointer, root: Long, title: String): Long? {
    val want = title.lowercase(Locale.ROOT)
    if (want.isBlank()) return null

    val focus = LongArray(1)
    if (x11.XGetInputFocus(display, focus, IntArray(1)) != 0) {
        var w = focus[0]
        while (w != 0L && w != root) {
            if (windowTitle(x11, display, w)?.lowercase(Locale.ROOT)?.contains(want) == true) return w
            w = parentOf(x11, display, w) ?: break
        }
    }

    val queue = ArrayDeque<Long>()
    queue.add(root)
    while (queue.isNotEmpty()) {
        val w = queue.removeFirst()
        if (w != root) {
            val name = windowTitle(x11, display, w)
            if (name != null && name.lowercase(Locale.ROOT).contains(want)) return w
        }
        childrenOf(x11, display, w)?.forEach { queue.add(it) }
    }
    return null
}

private fun windowTitle(x11: X11, display: Pointer, w: Long): String? {
    val name = PointerByReference()
    return try {
        if (x11.XFetchName(display, w, name) != 0) {
            name.value?.getString(0)
        } else {
            null
        }
    } finally {
        if (name.value != null) x11.XFree(name.value)
    }
}

private fun parentOf(x11: X11, display: Pointer, w: Long): Long? {
    val root = LongArray(1)
    val parent = LongArray(1)
    val children = PointerByReference()
    val n = IntArray(1)
    return if (x11.XQueryTree(display, w, root, parent, children, n) != 0) {
        if (children.value != null) x11.XFree(children.value)
        parent[0].takeIf { it != 0L }
    } else {
        null
    }
}

private fun childrenOf(x11: X11, display: Pointer, w: Long): List<Long>? {
    val root = LongArray(1)
    val parent = LongArray(1)
    val children = PointerByReference()
    val n = IntArray(1)
    if (x11.XQueryTree(display, w, root, parent, children, n) == 0) return null
    val result = if (n[0] > 0 && children.value != null) {
        children.value.getLongArray(0, n[0]).toList()
    } else {
        emptyList()
    }
    if (children.value != null) x11.XFree(children.value)
    return result
}
