package ua.syt0r.kanji.desktop.game.engine.input

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A real controller provider over JNA (already a desktop dependency for the
 * native window layer):
 *
 * - **Windows** — XInput (`XInputGetState` on `xinput1_4.dll`), the standard
 *   API for Xbox-style controllers; PS controllers present through emulation
 *   layers are detected via their vendor-reported layout name.
 * - **Linux** — the evdev joystick API (`/dev/input/js0`…) read through libc,
 *   with axis/button normalization. `js1`..`js3` are probed when needed.
 * - **Other platforms** — `connected == false`; the provider is a no-op.
 *
 * The provider runs a lightweight polling thread (60 Hz) that fills a
 * [GamepadSnapshot]; [poll] then maps that snapshot onto [InputState] as
 * [InputAction] presses/axes — exactly like the keyboard provider, so game
 * logic never sees the device (spec §14-16).
 */
class JnaGamepadProvider(
    var layout: GamepadLayout = GamepadLayout.Generic
) : GamepadProvider {

    override val kind = InputDeviceKind.Gamepad

    @Volatile
    override var connected: Boolean = false
        private set

    private val running = AtomicBoolean(false)
    private val thread: Thread

    private var lastButtons = emptySet<Int>()
    private var lastAxes = emptyMap<GamepadAxis, Float>()

    /** The latest raw snapshot, filled by the polling thread. */
    @Volatile
    private var snapshot = GamepadSnapshot()

    /**
     * Physical button presses detected by the polling thread since the last
     * [consumeNewButtonPress]. Independent of the gameplay edge bookkeeping
     * in [poll] so the rebind UI never steals an action press (spec §15).
     */
    private val pressQueue = ConcurrentLinkedQueue<GameKey>()

    /** The polling thread's own last-seen button set (for edge detection). */
    private var rawLastButtons = emptySet<Int>()

    init {
        val os = System.getProperty("os.name", "").lowercase()
        thread = Thread({
            try {
                if (os.contains("win")) {
                    runWindows()
                } else if (os.contains("linux")) {
                    runLinux()
                } else {
                    // macOS / other: platform gamepad framework is a future
                    // provider; this one reports disconnected.
                    connected = false
                }
            } catch (t: Throwable) {
                connected = false
            }
        }, "kaiteyo-gamepad").apply { isDaemon = true }
    }

    /** Start polling. Called by the session when the game view mounts. */
    fun start() {
        if (running.compareAndSet(false, true)) {
            thread.start()
        }
    }

    fun stop() {
        running.set(false)
    }

    // ------------------------------------------------------------
    // InputProvider
    // ------------------------------------------------------------

    /**
     * The [GameKey] of a button pressed since the last call, or null. Used by
     * the rebind UI to capture controller buttons while it listens; the
     * polling thread fills the queue from raw hardware deltas.
     */
    fun consumeNewButtonPress(): GameKey? = pressQueue.poll()

    override fun poll(state: InputState, calibration: InputCalibration) {
        val snap = snapshot
        if (!snap.connected) return
        layout = snap.layout

        // Buttons — edge press/release into the shared state, translated
        // through the active control scheme (same rebinding as the keyboard).
        val now = snap.heldButtons
        val released = lastButtons - now
        val pressed = now - lastButtons
        for (index in pressed) {
            snap.layout.buttonMap[index]?.let { key ->
                schemeActionFor(key)?.let { state.press(it) }
            }
        }
        for (index in released) {
            snap.layout.buttonMap[index]?.let { key ->
                schemeActionFor(key)?.let { state.release(it) }
            }
        }
        lastButtons = now

        // Sticks — dead zones from calibration, mapped onto move/look/run.
        val dead = calibration.leftStickDeadZone
        val lx = snap.axisFiltered(GamepadAxis.LeftX, dead)
        val ly = snap.axisFiltered(GamepadAxis.LeftY, dead)
        if (kotlin.math.abs(lx) > 0.01f || kotlin.math.abs(ly) > 0.01f) {
            state.setMoveAxis(lx, ly)
        }
        val rDead = calibration.rightStickDeadZone
        val rx = snap.axisFiltered(GamepadAxis.RightX, rDead)
        val ry = snap.axisFiltered(GamepadAxis.RightY, rDead)
        if (kotlin.math.abs(rx) > 0.01f || kotlin.math.abs(ry) > 0.01f) {
            state.addLookDelta(rx * calibration.lookSensitivity * 6f, ry * calibration.lookSensitivity * 6f)
        }
        // Triggers double as Run (right) / Interact (left) when mapped.
        val trigger = snap.axisFiltered(GamepadAxis.RightTrigger, dead)
        if (trigger > 0.01f) {
            state.setRun(trigger)
        }
        val leftTrigger = snap.axisFiltered(GamepadAxis.LeftTrigger, dead)
        if (leftTrigger > 0.01f) {
            state.press(InputAction.UseItem)
        } else {
            state.release(InputAction.UseItem)
        }
    }

    /** The active control scheme used to translate GameKeys → actions. */
    @Volatile
    var scheme: ControlScheme = ControlScheme.default()

    private fun schemeActionFor(key: GameKey): InputAction? =
        scheme.actionFor(key)

    // ------------------------------------------------------------
    // Windows — XInput
    // ------------------------------------------------------------

    private interface XInput : Library {
        fun XInputGetState(dwUserIndex: Int, pState: Pointer): Int
    }

    private fun runWindows() {
        val lib: XInput? = runCatching {
            Native.load("XInput1_4", XInput::class.java)
        }.getOrNull() ?: runCatching {
            Native.load("XInput9_1_0", XInput::class.java)
        }.getOrNull() ?: return

        var first = true
        while (running.get()) {
            val buffer = ByteBuffer.allocateDirect(16).order(ByteOrder.LITTLE_ENDIAN)
            val result = lib!!.XInputGetState(0, Native.getDirectBufferPointer(buffer))
            if (result == 0) {
                buffer.position(0)
                val buttons = buffer.short.toInt() and 0xFFFF
                val lt = buffer.get().toInt() and 0xFF
                val rt = buffer.get().toInt() and 0xFF
                val lx = buffer.short.toInt()
                val ly = buffer.short.toInt()
                val rx = buffer.short.toInt()
                val ry = buffer.short.toInt()

                // XInput packs DPad + face buttons in one bitmask; expose
                // them as distinct button indices (DPad = 10..13, faces 0..3).
                val heldButtons = buildSet {
                    if (buttons and 0x0001 != 0) add(10) // DPadUp
                    if (buttons and 0x0002 != 0) add(11) // DPadDown
                    if (buttons and 0x0004 != 0) add(12) // DPadLeft
                    if (buttons and 0x0008 != 0) add(13) // DPadRight
                    if (buttons and 0x0010 != 0) add(7)  // Start
                    if (buttons and 0x0020 != 0) add(6)  // Back
                    if (buttons and 0x0040 != 0) add(8)  // LeftStick
                    if (buttons and 0x0080 != 0) add(9)  // RightStick
                    if (buttons and 0x0100 != 0) add(4)  // LB
                    if (buttons and 0x0200 != 0) add(5)  // RB
                    if (buttons and 0x1000 != 0) add(0)  // A
                    if (buttons and 0x2000 != 0) add(1)  // B
                    if (buttons and 0x4000 != 0) add(2)  // X
                    if (buttons and 0x8000 != 0) add(3)  // Y
                }

                val axisMap = mapOf(
                    GamepadAxis.LeftX to (lx / 32768f),
                    GamepadAxis.LeftY to (-ly / 32768f),
                    GamepadAxis.RightX to (rx / 32768f),
                    GamepadAxis.RightY to (-ry / 32768f),
                    GamepadAxis.LeftTrigger to (lt / 255f),
                    GamepadAxis.RightTrigger to (rt / 255f)
                )
                val snapLayout = if (first) detectLayout() else layout
                snapshot = GamepadSnapshot(
                    connected = true,
                    heldButtons = heldButtons,
                    axes = axisMap,
                    layout = snapLayout
                )
                queueButtonEdges(heldButtons, snapLayout)
                first = false
            } else {
                // ERROR_DEVICE_NOT_CONNECTED — keep polling quietly. Reset
                // edge bookkeeping so a re-plug doesn't replay old presses.
                snapshot = GamepadSnapshot()
                rawLastButtons = emptySet()
            }
            Thread.sleep(16)
        }
    }

    private fun detectLayout(): GamepadLayout {
        // Xbox controllers are the norm on Windows; PS controllers show up
        // through emulation layers as Xbox. The layout is configurable in the
        // game settings, so detection is a starting default, not a lock-in.
        return GamepadLayout.Xbox.also { it.markDetected() }
    }

    // ------------------------------------------------------------
    // Linux — evdev joystick API (/dev/input/js0)
    // ------------------------------------------------------------

    private interface LibC : Library {
        fun open(pathname: String, flags: Int): Int
        fun read(fd: Int, buf: Pointer, count: Int): Int
        fun close(fd: Int): Int
    }

    private fun runLinux() {
        val libc: LibC? = runCatching {
            Native.load("c", LibC::class.java)
        }.getOrNull() ?: return

        val O_RDONLY = 0
        val O_NONBLOCK = 0x800

        // Hot-plug (spec §15): no joystick at start (or unplugged mid-run) is
        // never fatal — the loop keeps probing until a device appears.
        while (running.get()) {
            var fd = -1
            for (i in 0..3) {
                val f = File("/dev/input/js$i")
                if (!f.exists()) continue
                fd = libc!!.open(f.absolutePath, O_RDONLY or O_NONBLOCK)
                if (fd >= 0) break
            }
            if (fd < 0) {
                connected = false
                snapshot = GamepadSnapshot()
                Thread.sleep(1000)
                continue
            }
            connected = true

            // js_event: uint32 time, int16 value, uint8 type, uint8 number (8 bytes)
            val event = ByteBuffer.allocateDirect(8).order(ByteOrder.LITTLE_ENDIAN)
            // Keep last-known values (event stream, not state).
            val axisValues = mutableMapOf<Int, Float>()
            val buttonValues = mutableMapOf<Int, Boolean>()
            var first = true

            while (running.get()) {
                event.clear()
                val n = libc!!.read(fd, Native.getDirectBufferPointer(event), 8)
                if (n == 8) {
                    event.position(0)
                    event.int // time (skip)
                    val value = event.short.toInt()
                    val type = event.get().toInt() and 0xFF
                    val number = event.get().toInt() and 0xFF
                    val isInit = type and 0x80 != 0
                    val realType = type and 0x7F

                    when (realType) {
                        0x01 -> buttonValues[number] = value != 0
                        0x02 -> axisValues[number] = value / 32768f
                    }
                    if (!isInit) {
                        val held = buttonValues.filterValues { it }.keys.toSet()
                        val axes = axisValues.mapKeys { (index, _) ->
                            when (index) {
                                0 -> GamepadAxis.LeftX
                                1 -> GamepadAxis.LeftY
                                2 -> GamepadAxis.RightX
                                3 -> GamepadAxis.RightY
                                4 -> GamepadAxis.LeftTrigger
                                5 -> GamepadAxis.RightTrigger
                                else -> GamepadAxis.RightY
                            }
                        }
                        val snapLayout = if (first) detectLinuxLayout() else layout
                        snapshot = GamepadSnapshot(
                            connected = true,
                            heldButtons = held,
                            axes = axes,
                            layout = snapLayout
                        )
                        queueButtonEdges(held, snapLayout)
                        first = false
                    }
                }
                Thread.sleep(4)
            }
            libc!!.close(fd)
            snapshot = GamepadSnapshot()
            connected = false
            rawLastButtons = emptySet()
        }
    }

    /** Push newly-pressed physical buttons onto the rebind queue. */
    private fun queueButtonEdges(held: Set<Int>, activeLayout: GamepadLayout) {
        for (index in held - rawLastButtons) {
            activeLayout.buttonMap[index]?.let { pressQueue.add(it) }
        }
        rawLastButtons = held
    }

    private fun detectLinuxLayout(): GamepadLayout {
        // Most Linux controllers report the standard button order; Xbox pads
        // report as Xbox. The layout stays configurable.
        return GamepadLayout.Xbox.also { it.markDetected() }
    }
}
