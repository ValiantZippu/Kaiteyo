package ua.syt0r.kanji.desktop.game.engine.input

import kotlinx.serialization.Serializable
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2

/**
 * Controller layout presets (spec §15): Xbox-style, PlayStation-style and a
 * generic fallback. Physical buttons/sticks map onto the same [GameKey]s the
 * keyboard uses, so rebinding and game logic never know the device.
 */
@Serializable
enum class GamepadLayout(
    val label: String,
    /** Physical button index (vendor ordering) → [GameKey]. */
    val buttonMap: Map<Int, GameKey>,
    /** Physical axis index → semantic axis. */
    val axisMap: Map<Int, GamepadAxis>
) {
    Xbox(
        label = "Xbox",
        buttonMap = baseButtonMap(),
        axisMap = mapOf(
            0 to GamepadAxis.LeftX,
            1 to GamepadAxis.LeftY,
            2 to GamepadAxis.RightX,
            3 to GamepadAxis.RightY
        )
    ),
    PlayStation(
        label = "PlayStation",
        buttonMap = baseButtonMap().toMutableMap().apply {
            // PS physical face-button order: Cross/Circle/Square/Triangle.
            put(0, GameKey.GamepadB)  // Cross  ✕
            put(1, GameKey.GamepadA)  // Circle ◯
            put(2, GameKey.GamepadY)  // Square □
            put(3, GameKey.GamepadX)  // Triangle △
        },
        axisMap = mapOf(
            0 to GamepadAxis.LeftX,
            1 to GamepadAxis.LeftY,
            2 to GamepadAxis.RightX,
            3 to GamepadAxis.RightY
        )
    ),
    Generic(
        label = "Generic",
        buttonMap = baseButtonMap(),
        axisMap = mapOf(
            0 to GamepadAxis.LeftX,
            1 to GamepadAxis.LeftY,
            2 to GamepadAxis.RightX,
            3 to GamepadAxis.RightY
        )
    );

    /** True when this layout was detected on a connected device. */
    var detected: Boolean = false
        private set

    fun markDetected() {
        detected = true
    }

    companion object {
        fun fromLabel(label: String): GamepadLayout =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: Generic
    }
}

/** Physical button indices shared across layouts (0-9) plus DPad (10-13). */
private fun baseButtonMap(): Map<Int, GameKey> = mapOf(
    0 to GameKey.GamepadA,
    1 to GameKey.GamepadB,
    2 to GameKey.GamepadX,
    3 to GameKey.GamepadY,
    4 to GameKey.GamepadLeftBumper,
    5 to GameKey.GamepadRightBumper,
    6 to GameKey.GamepadBack,
    7 to GameKey.GamepadStart,
    8 to GameKey.GamepadLeftStick,
    9 to GameKey.GamepadRightStick,
    10 to GameKey.GamepadDPadUp,
    11 to GameKey.GamepadDPadDown,
    12 to GameKey.GamepadDPadLeft,
    13 to GameKey.GamepadDPadRight
)

/** Semantic axes a controller exposes (analog values). */
enum class GamepadAxis { LeftX, LeftY, RightX, RightY, LeftTrigger, RightTrigger }

/**
 * A controller snapshot from the platform layer. The JNA provider fills this
 * from XInput (Windows) or the evdev joystick API (Linux); a future mobile
 * provider fills it from the OS gamepad framework.
 */
data class GamepadSnapshot(
    val connected: Boolean = false,
    /** Button indices currently held. */
    val heldButtons: Set<Int> = emptySet(),
    /** Raw analog axes -1..1 (dead zone NOT applied). */
    val axes: Map<GamepadAxis, Float> = emptyMap(),
    val layout: GamepadLayout = GamepadLayout.Generic
) {
    fun axis(axis: GamepadAxis): Float = axes[axis] ?: 0f

    /** Apply dead zones + a small axial ramp so sticks feel natural. */
    fun axisFiltered(axis: GamepadAxis, deadZone: Float): Float {
        val raw = axis(axis)
        val magnitude = kotlin.math.abs(raw)
        if (magnitude <= deadZone) return 0f
        // Scale the leftover range back to 0..1 so tiny movements are smooth.
        return (magnitude - deadZone) / (1f - deadZone) * if (raw < 0f) -1f else 1f
    }
}
