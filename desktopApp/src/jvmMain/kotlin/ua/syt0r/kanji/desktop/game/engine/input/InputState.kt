package ua.syt0r.kanji.desktop.game.engine.input

import ua.syt0r.kanji.desktop.game.engine.geom.Vec2

/**
 * The current frame's input snapshot. Systems read this — never raw events.
 *
 * Raw presses/releases arrive from Compose callbacks *between* fixed ticks,
 * so edges are queued: [beginFrame] promotes the queue into the frame's edge
 * sets. An event is therefore delivered on the next tick at worst — it can
 * never be silently dropped by a beginFrame racing the callback.
 */
class InputState {

    private val pressed = mutableSetOf<InputAction>()

    /** Actions pressed right now (held). */
    val held: Set<InputAction> get() = pressed.toSet()

    /** Raw edges queued since the last [beginFrame]. */
    private val rawEdgePresses = mutableSetOf<InputAction>()
    private val rawEdgeReleases = mutableSetOf<InputAction>()

    /** Actions that became pressed this frame (edge trigger). */
    private val justPressed = mutableSetOf<InputAction>()

    /** Actions released this frame. */
    private val justReleased = mutableSetOf<InputAction>()

    /** Movement axis from keyboard/gamepad/touch, normalized -1..1. */
    var moveAxis: Vec2 = Vec2.Zero
        private set

    /** Look axis (mouse delta / right stick / touch drag). */
    var lookDelta: Vec2 = Vec2.Zero
        private set

    /** Analog run input (0..1). */
    var runAmount: Float = 0f
        private set

    fun isPressed(action: InputAction): Boolean = action in pressed

    fun wasPressedThisFrame(action: InputAction): Boolean = action in justPressed

    fun wasReleasedThisFrame(action: InputAction): Boolean = action in justReleased

    /** Begin a new frame: promote raw edges, reset per-frame values. */
    fun beginFrame() {
        justPressed.clear()
        justReleased.clear()
        justPressed.addAll(rawEdgePresses)
        justReleased.addAll(rawEdgeReleases)
        rawEdgePresses.clear()
        rawEdgeReleases.clear()
        lookDelta = Vec2.Zero
    }

    fun press(action: InputAction) {
        if (pressed.add(action)) {
            rawEdgePresses.add(action)
        }
    }

    fun release(action: InputAction) {
        if (pressed.remove(action)) {
            rawEdgeReleases.add(action)
        }
    }

    fun setMoveAxis(x: Float, y: Float) {
        moveAxis = Vec2(x.coerceIn(-1f, 1f), y.coerceIn(-1f, 1f))
    }

    fun addLookDelta(dx: Float, dy: Float) {
        lookDelta = Vec2(lookDelta.x + dx, lookDelta.y + dy)
    }

    fun setRun(amount: Float) {
        runAmount = amount.coerceIn(0f, 1f)
    }

    fun clearAll() {
        pressed.clear()
        rawEdgePresses.clear()
        rawEdgeReleases.clear()
        justPressed.clear()
        justReleased.clear()
        moveAxis = Vec2.Zero
        lookDelta = Vec2.Zero
        runAmount = 0f
    }
}
