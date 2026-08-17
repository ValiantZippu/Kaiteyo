package ua.syt0r.kanji.desktop.game.engine.input

import ua.syt0r.kanji.desktop.game.engine.geom.Vec2

/**
 * Merges every connected [InputProvider] into one [InputState] and owns the
 * active [ControlScheme] + [InputCalibration]. Game code reads actions from
 * [InputManager.state] — this is the only input entry point.
 */
class InputManager(
    var scheme: ControlScheme = ControlScheme.default(),
    var calibration: InputCalibration = InputCalibration()
) {

    val state = InputState()

    private val providers = mutableListOf<InputProvider>()

    /** The keyboard/mouse provider — always present on desktop. */
    val keyboardMouse = KeyboardMouseProvider(scheme)

    /** Optional gamepad / touch providers (plugged in when detected). */
    var gamepad: GamepadProvider? = null
        internal set
    var touch: TouchProvider? = null
        internal set

    init {
        providers.add(keyboardMouse)
    }

    /**
     * Attach the JNA controller provider and start polling. The provider
     * shares the live [ControlScheme] so rebinding applies to the gamepad
     * too (spec §15). Returns it for lifecycle control (start/stop).
     */
    fun connectGamepad(): JnaGamepadProvider {
        val provider = JnaGamepadProvider().apply {
            scheme = this@InputManager.scheme
        }
        gamepad = provider
        provider.start()
        return provider
    }

    /** Detach the controller provider (game view teardown). */
    fun disconnectGamepad() {
        (gamepad as? JnaGamepadProvider)?.stop()
        gamepad = null
    }

    /**
     * Attach (or reuse) the virtual touch provider the overlay drives.
     * Passive — no thread, no lifecycle; it only forwards to [InputState].
     */
    fun connectTouch(): VirtualTouchProvider {
        val provider = touch as? VirtualTouchProvider ?: VirtualTouchProvider()
        this.touch = provider
        return provider
    }

    fun beginFrame() {
        state.beginFrame()
    }

    /** Poll every connected provider into the shared state. */
    fun poll() {
        for (provider in providers + listOfNotNull(gamepad, touch)) {
            if (provider.connected) provider.poll(state, calibration)
        }
    }

    // ---------------------------------------------------------------
    // Raw event entry points (called by the Compose host)
    // ---------------------------------------------------------------

    fun onKey(key: GameKey, down: Boolean) {
        keyboardMouse.handleKey(key, down, state)
    }

    fun onMouseMove(dx: Float, dy: Float) {
        keyboardMouse.handleMouseMove(dx, dy, state)
    }

    fun onMouseWheel(delta: Float) {
        keyboardMouse.handleMouseWheel(delta, state)
    }

    fun onMouseClick(position: Vec2, down: Boolean) {
        if (down) state.press(InputAction.Interact) else state.release(InputAction.Interact)
    }

    /** Photo-mode capture trigger (mouse click while the viewfinder is up). */
    fun onPhotoCapture(down: Boolean) {
        if (down) state.press(InputAction.PhotoCapture) else state.release(InputAction.PhotoCapture)
    }

    // ---------------------------------------------------------------
    // Rebinding API (spec §15 — control rebinding)
    // ---------------------------------------------------------------

    fun bind(action: InputAction, key: GameKey) {
        scheme = scheme.bind(action, key)
        // Re-create the keyboard provider mapping when the scheme changes.
        keyboardMouse.scheme = scheme
        (gamepad as? JnaGamepadProvider)?.scheme = scheme
    }

    fun unbind(action: InputAction, key: GameKey) {
        scheme = scheme.unbind(action, key)
        keyboardMouse.scheme = scheme
        (gamepad as? JnaGamepadProvider)?.scheme = scheme
    }

    fun resetBindings() {
        scheme = scheme.reset()
        keyboardMouse.scheme = scheme
        (gamepad as? JnaGamepadProvider)?.scheme = scheme
    }
}
