package ua.syt0r.kanji.desktop.engine.media

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTR
import com.sun.jna.win32.W32APIOptions

// ============================================
// KAITEYO SYSTEM MEDIA KEYS
// A global low-level keyboard hook (WH_KEYBOARD_LL)
// that captures the keyboard's multimedia keys —
// play/pause, previous, next, stop — even while
// Kaiteyo has no focus. Implemented with the JNA
// already on the desktop classpath (the same
// library used for native window dragging), so no
// new dependency is introduced.
//
// Platform reality: only Windows exposes these
// keys through a low-level hook without extra
// installs. On macOS/Linux [supported] is false and
// [start] reports that honestly instead of faking
// success — the in-app media hotkeys and the tray
// controller still work there. The hook thread is a
// daemon with its own message pump and is stopped
// cleanly via PostThreadMessageW(WM_QUIT).
// ============================================

/** The actions the multimedia keys map onto. */
enum class SystemMediaKeyAction {
    Toggle,
    Next,
    Previous,
    Stop
}

// VK codes for the standard Windows multimedia keys.
private const val VK_MEDIA_NEXT_TRACK = 0xB0
private const val VK_MEDIA_PREV_TRACK = 0xB1
private const val VK_MEDIA_STOP = 0xB2
private const val VK_MEDIA_PLAY_PAUSE = 0xB3

/**
 * Map a Windows VK code to a media action (null for unrelated keys).
 * Kept as a pure function so the mapping is unit-testable.
 */
internal fun systemMediaKeyActionForVk(vk: Int): SystemMediaKeyAction? = when (vk) {
    VK_MEDIA_PLAY_PAUSE -> SystemMediaKeyAction.Toggle
    VK_MEDIA_NEXT_TRACK -> SystemMediaKeyAction.Next
    VK_MEDIA_PREV_TRACK -> SystemMediaKeyAction.Previous
    VK_MEDIA_STOP -> SystemMediaKeyAction.Stop
    else -> null
}

private const val WH_KEYBOARD_LL = 13
private const val WM_KEYDOWN = 0x0100
private const val WM_QUIT = 0x0012

/** Minimal user32 surface used by the media-key hook. */
private interface MediaKeyUser32 : Library {
    fun SetWindowsHookExW(idHook: Int, lpfn: MediaKeyProc, hMod: Pointer?, dwThreadId: Int): Long
    fun UnhookWindowsHookEx(hhk: Long): Boolean
    fun CallNextHookEx(hhk: Long, nCode: Int, wParam: Long, lParam: Long): Long
    fun GetModuleHandleW(lpModuleName: String?): Pointer?
    fun GetMessageW(lpMsg: MediaKeyMsg, hWnd: Pointer?, wMsgFilterMin: Int, wMsgFilterMax: Int): Int
    fun TranslateMessage(lpMsg: MediaKeyMsg): Boolean
    fun DispatchMessageW(lpMsg: MediaKeyMsg): Long

    companion object {
        val INSTANCE: MediaKeyUser32 =
            Native.load("user32", MediaKeyUser32::class.java, W32APIOptions.UNICODE_OPTIONS)
    }
}

/** Minimal kernel32 surface for a clean hook-thread shutdown. */
private interface MediaKeyKernel32 : Library {
    fun GetCurrentThreadId(): Int
    fun PostThreadMessageW(idThread: Int, Msg: Int, wParam: Long, lParam: Long): Boolean

    companion object {
        val INSTANCE: MediaKeyKernel32 = Native.load("kernel32", MediaKeyKernel32::class.java)
    }
}

/** WH_KEYBOARD_LL hook procedure. */
private interface MediaKeyProc : Callback {
    fun callback(nCode: Int, wParam: Long, lParam: Pointer?): Long
}

/** MSG — flattened POINT so JNA never needs a nested structure. */
private class MediaKeyMsg : Structure() {
    @JvmField var hwnd: Pointer? = null
    @JvmField var message: Int = 0
    @JvmField var wParam: Long = 0
    @JvmField var lParam: Long = 0
    @JvmField var time: Int = 0
    @JvmField var ptX: Int = 0
    @JvmField var ptY: Int = 0

    override fun getFieldOrder(): List<String> =
        listOf("hwnd", "message", "wParam", "lParam", "time", "ptX", "ptY")
}

/** KBDLLHOOKSTRUCT — the payload behind every media-key event. */
private class KbdllHookStruct : Structure() {
    @JvmField var vkCode: Int = 0
    @JvmField var scanCode: Int = 0
    @JvmField var flags: Int = 0
    @JvmField var time: Int = 0
    @JvmField var dwExtraInfo: ULONG_PTR = ULONG_PTR()

    override fun getFieldOrder(): List<String> =
        listOf("vkCode", "scanCode", "flags", "time", "dwExtraInfo")

    companion object {
        /** Reads the struct from the hook's lParam pointer (useMemory is protected). */
        fun from(pointer: Pointer): KbdllHookStruct {
            val s = KbdllHookStruct()
            s.useMemory(pointer)
            s.read()
            return s
        }
    }
}

/**
 * Global media-key listener. [onAction] receives the mapped [SystemMediaKeyAction]
 * whenever the keyboard's multimedia keys are pressed while Kaiteyo is running.
 */
class SystemMediaKeys(
    private val onAction: (SystemMediaKeyAction) -> Unit
) {

    private val lock = Any()
    private var hookThread: Thread? = null
    private var hookHandle: Long = 0
    private var callbackRef: MediaKeyProc? = null

    // Real OS thread id of the hook thread — Thread.id is the JVM's own
    // counter and cannot be used with PostThreadMessageW.
    @Volatile
    private var hookOsThreadId: Int = 0

    private val activeState = androidx.compose.runtime.mutableStateOf(false)
    private val lastErrorState = androidx.compose.runtime.mutableStateOf<String?>(null)

    /** Whether the hook is currently installed. */
    val active: Boolean get() = activeState.value

    /** Why the last [start] failed (null when it succeeded or hasn't run). */
    val lastError: String? get() = lastErrorState.value

    /** Only Windows can capture the multimedia keys without extra installs. */
    val supported: Boolean
        get() = System.getProperty("os.name").lowercase().contains("windows")

    /** Install the hook. Returns true when the keys are actually captured. */
    fun start(): Boolean {
        if (!supported) {
            lastErrorState.value = "System media keys need Windows (macOS/Linux use the tray or in-app hotkeys)"
            return false
        }
        synchronized(lock) {
            if (active) return true
            callbackRef = object : MediaKeyProc {
                override fun callback(nCode: Int, wParam: Long, lParam: Pointer?): Long =
                    onHookEvent(nCode, wParam, lParam)
            }
            activeState.value = true
            lastErrorState.value = null
            hookThread = Thread(::hookLoop, "kaiteyo-media-keys").also {
                it.isDaemon = true
                it.start()
            }
        }
        return true
    }

    fun stop() {
        synchronized(lock) {
            if (!active) return
            activeState.value = false
            // Remove the hook immediately (safe cross-thread — it's a handle
            // call), then wake the pump thread with WM_QUIT so it exits.
            val handle = hookHandle
            if (handle != 0L) {
                runCatching { MediaKeyUser32.INSTANCE.UnhookWindowsHookEx(handle) }
                hookHandle = 0
            }
            val osThreadId = hookOsThreadId
            if (osThreadId != 0) {
                runCatching { MediaKeyKernel32.INSTANCE.PostThreadMessageW(osThreadId, WM_QUIT, 0, 0) }
            }
            hookThread = null
            callbackRef = null
        }
    }

    private fun hookLoop() {
        hookOsThreadId = MediaKeyKernel32.INSTANCE.GetCurrentThreadId()
        val user32 = MediaKeyUser32.INSTANCE
        val proc = synchronized(lock) { callbackRef } ?: return
        val hMod = runCatching { user32.GetModuleHandleW(null) }.getOrNull()
        val handle = user32.SetWindowsHookExW(WH_KEYBOARD_LL, proc, hMod, 0)
        synchronized(lock) {
            hookHandle = if (handle != 0L) handle else 0
            if (handle == 0L) activeState.value = false
        }
        if (handle == 0L) {
            lastErrorState.value = "SetWindowsHookExW failed (code ${Native.getLastError()})"
            return
        }

        // Message pump — GetMessageW returns 0 when WM_QUIT arrives.
        val msg = MediaKeyMsg()
        while (true) {
            val result = user32.GetMessageW(msg, null, 0, 0)
            if (result <= 0) break
            user32.TranslateMessage(msg)
            user32.DispatchMessageW(msg)
        }
        runCatching { user32.UnhookWindowsHookEx(handle) }
        synchronized(lock) {
            hookHandle = 0
            hookOsThreadId = 0
            activeState.value = false
        }
    }

    private fun onHookEvent(nCode: Int, wParam: Long, lParam: Pointer?): Long {
        if (nCode >= 0 && wParam == WM_KEYDOWN.toLong()) {
            val ptr = lParam
            if (ptr != null) {
                val vk = runCatching { KbdllHookStruct.from(ptr).vkCode }.getOrDefault(0)
                val action = systemMediaKeyActionForVk(vk)
                if (action != null) {
                    // The hook is only installed while Kaiteyo has media loaded,
                    // so consuming the key is always correct here.
                    onAction(action)
                    return 1
                }
            }
        }
        val handle = synchronized(lock) { hookHandle }
        return if (handle == 0L) 0
        else MediaKeyUser32.INSTANCE.CallNextHookEx(handle, nCode, wParam, lParam?.let { Pointer.nativeValue(it) } ?: 0L)
    }
}
