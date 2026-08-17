package ua.syt0r.kanji.desktop.game.engine.input

import ua.syt0r.kanji.desktop.game.engine.geom.Vec2

/**
 * A device that can produce input. The engine hosts several providers at
 * once (keyboard/mouse is live on desktop; gamepad and touch plug in when a
 * controller is detected or the app runs on a touch device) and merges them
 * through the [InputManager] — no game code ever branches on device.
 */
interface InputProvider {
    val kind: InputDeviceKind

    /** True while this device is present and usable. */
    val connected: Boolean

    /** Called once per fixed tick to feed the shared [InputState]. */
    fun poll(state: InputState, calibration: InputCalibration)
}

enum class InputDeviceKind {
    KeyboardMouse,
    Gamepad,
    Touch
}

/**
 * Keyboard + mouse provider. The Compose host forwards raw key/mouse events
 * here ([handleKey], [handleMouseMove], [handleMouseWheel]); the provider
 * translates them into [InputAction] presses on the shared state. Its
 * bindings come from the live [ControlScheme] (rebinding updates it).
 */
class KeyboardMouseProvider(
    var scheme: ControlScheme
) : InputProvider {

    override val kind = InputDeviceKind.KeyboardMouse
    override val connected: Boolean = true

    fun handleKey(key: GameKey, down: Boolean, state: InputState) {
        val action = scheme.actionFor(key) ?: return
        if (down) state.press(action) else state.release(action)
    }

    fun handleMouseMove(dx: Float, dy: Float, state: InputState) {
        state.addLookDelta(dx, dy)
    }

    fun handleMouseWheel(delta: Float, state: InputState) {
        if (delta > 0f) state.press(InputAction.ZoomIn)
        if (delta < 0f) state.press(InputAction.ZoomOut)
    }

    override fun poll(state: InputState, calibration: InputCalibration) {
        val up = state.isPressed(InputAction.MoveUp)
        val down = state.isPressed(InputAction.MoveDown)
        val left = state.isPressed(InputAction.MoveLeft)
        val right = state.isPressed(InputAction.MoveRight)
        state.setMoveAxis(
            (if (right) 1f else 0f) - (if (left) 1f else 0f),
            (if (down) 1f else 0f) - (if (up) 1f else 0f)
        )
        state.setRun(if (state.isPressed(InputAction.Run)) 1f else 0f)
    }
}

/**
 * Controller provider (Xbox-style). Two integration styles are supported:
 *
 * - **Event-driven** — a host (Compose layer, OS gamepad framework) forwards
 *   button/axis events through [handleButton]/[handleAxis].
 * - **Poll-driven** — the provider reads hardware itself (see
 *   [JnaGamepadProvider]) and only implements [InputProvider.poll].
 *
 * The default no-op handlers make both styles implementable without stubs.
 */
interface GamepadProvider : InputProvider {
    fun handleButton(key: GameKey, down: Boolean, state: InputState) {}

    fun handleAxis(leftStick: Vec2, rightStick: Vec2, triggers: Float, state: InputState) {}
}

/**
 * Touch provider. Mobile never shrinks the desktop HUD: a virtual movement
 * pad, look-drag and contextual buttons map onto the same [InputAction]s.
 * Interface complete; wiring is a TODO (see `docs/game/TODO.md` under TOUCH).
 */
interface TouchProvider : InputProvider {
    fun handleGesture(gesture: TouchGesture, state: InputState) {}

    fun handleMovePad(axis: Vec2, state: InputState) {}
}

enum class TouchGesture {
    Tap,
    DoubleTap,
    Drag,
    PinchOpen,
    PinchClose,
    LongPress
}
