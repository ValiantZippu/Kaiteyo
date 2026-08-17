package ua.syt0r.kanji.desktop.game.engine.input

import ua.syt0r.kanji.desktop.game.engine.geom.Vec2

/**
 * Touch input (spec §16): a dynamic-origin movement pad, look drag and
 * contextual buttons — PUBG/Genshin-style. The [overlay UI][ua.syt0r.kanji.desktop.game.ui.TouchControlsOverlay]
 * drives this provider live; it translates into the same [InputAction]s as
 * every other device, so game logic never sees the surface.
 *
 * Mobile never gets a shrunk desktop HUD: the pad appears where the thumb
 * lands, the right side drags the camera, and buttons appear only when
 * relevant (spec §16, §71).
 */
class VirtualTouchProvider : TouchProvider {

    override val kind = InputDeviceKind.Touch
    override val connected = true

    /** Move the virtual stick (normalized -1..1); [run] when pushed to the rim. */
    fun setMovePad(axis: Vec2, run: Boolean, state: InputState) {
        state.setMoveAxis(axis.x, axis.y)
        state.setRun(if (run) 1f else 0f)
    }

    /** Look drag (right side of the screen). */
    fun addLook(dx: Float, dy: Float, state: InputState) {
        state.addLookDelta(dx, dy)
    }

    /** Contextual button press/release. */
    fun pressAction(action: InputAction, down: Boolean, state: InputState) {
        if (down) state.press(action) else state.release(action)
    }

    /** A quick tap on the look side acts as Interact. */
    fun tapInteract(state: InputState) {
        state.press(InputAction.Interact)
        state.release(InputAction.Interact)
    }

    override fun poll(state: InputState, calibration: InputCalibration) {
        // The overlay drives everything live through the methods above; a
        // passive touch surface has nothing to poll.
    }
}
