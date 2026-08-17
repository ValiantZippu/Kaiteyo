package ua.syt0r.kanji.desktop.game.engine.input

import kotlinx.serialization.Serializable

/**
 * Maps [InputAction]s to physical [GameKey]s. Defaults are the classic
 * keyboard layout (WASD + arrows both work) plus an Xbox-style gamepad map;
 * players can rebind every action. Persisted through [GameSettings].
 */
@Serializable
data class ControlScheme(
    val bindings: Map<String, List<String>> = DEFAULT_BINDINGS
) {

    fun keysFor(action: InputAction): List<GameKey> =
        bindings[action.name].orEmpty().mapNotNull { name ->
            GameKey.entries.firstOrNull { it.name == name }
        }

    fun actionFor(key: GameKey): InputAction? {
        for ((actionName, keys) in bindings) {
            if (keys.any { it == key.name }) {
                return InputAction.entries.firstOrNull { it.name == actionName }
            }
        }
        return null
    }

    fun bind(action: InputAction, key: GameKey): ControlScheme {
        val current = bindings[action.name].orEmpty().toMutableList()
        if (key.name !in current) current.add(key.name)
        val updated = bindings.toMutableMap()
        updated[action.name] = current
        return ControlScheme(updated)
    }

    fun unbind(action: InputAction, key: GameKey): ControlScheme {
        val current = bindings[action.name].orEmpty().toMutableList()
        current.remove(key.name)
        val updated = bindings.toMutableMap()
        updated[action.name] = current
        return ControlScheme(updated)
    }

    fun reset(): ControlScheme = ControlScheme(DEFAULT_BINDINGS)

    companion object {
        fun default(): ControlScheme = ControlScheme(DEFAULT_BINDINGS)

        val DEFAULT_BINDINGS: Map<String, List<String>> = buildMap {
            fun bind(action: InputAction, vararg keys: GameKey) {
                put(action.name, keys.map { it.name })
            }
            bind(InputAction.MoveUp, GameKey.W, GameKey.ArrowUp, GameKey.GamepadDPadUp)
            bind(InputAction.MoveDown, GameKey.S, GameKey.ArrowDown, GameKey.GamepadDPadDown)
            bind(InputAction.MoveLeft, GameKey.A, GameKey.ArrowLeft, GameKey.GamepadDPadLeft)
            bind(InputAction.MoveRight, GameKey.D, GameKey.ArrowRight, GameKey.GamepadDPadRight)
            bind(InputAction.Run, GameKey.Shift)
            bind(InputAction.Interact, GameKey.E, GameKey.GamepadA)
            bind(InputAction.Jump, GameKey.Space)
            bind(InputAction.SwitchCamera, GameKey.V, GameKey.GamepadBack)
            bind(InputAction.PhotoMode, GameKey.C, GameKey.GamepadX)
            bind(InputAction.OpenMap, GameKey.M, GameKey.GamepadY)
            bind(InputAction.OpenQuests, GameKey.Q, GameKey.GamepadLeftBumper)
            bind(InputAction.OpenCollection, GameKey.I)
            bind(InputAction.OpenMenu, GameKey.Escape, GameKey.GamepadStart)
            bind(InputAction.Back, GameKey.Escape, GameKey.GamepadB)
            bind(InputAction.PhotoCapture, GameKey.MouseLeft, GameKey.GamepadRightBumper)
            bind(InputAction.ZoomIn, GameKey.Digit9)
            bind(InputAction.ZoomOut, GameKey.Digit0)
            bind(InputAction.ToggleDebug, GameKey.F3)
            bind(InputAction.UseItem, GameKey.F)
        }
    }
}

/**
 * Per-source input calibration: stick dead zones, look sensitivity, touch
 * sensitivity. Exposed in the game settings screen (spec §15).
 */
@Serializable
data class InputCalibration(
    val lookSensitivity: Float = 1f,
    val movementSensitivity: Float = 1f,
    val touchSensitivity: Float = 1f,
    val leftStickDeadZone: Float = 0.18f,
    val rightStickDeadZone: Float = 0.18f,
    val invertY: Boolean = false
)
