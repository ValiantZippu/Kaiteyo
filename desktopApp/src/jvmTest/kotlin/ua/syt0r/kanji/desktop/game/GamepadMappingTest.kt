package ua.syt0r.kanji.desktop.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2
import ua.syt0r.kanji.desktop.game.engine.input.ControlScheme
import ua.syt0r.kanji.desktop.game.engine.input.GameKey
import ua.syt0r.kanji.desktop.game.engine.input.GamepadAxis
import ua.syt0r.kanji.desktop.game.engine.input.GamepadLayout
import ua.syt0r.kanji.desktop.game.engine.input.GamepadProvider
import ua.syt0r.kanji.desktop.game.engine.input.GamepadSnapshot
import ua.syt0r.kanji.desktop.game.engine.input.InputAction
import ua.syt0r.kanji.desktop.game.engine.input.InputCalibration
import ua.syt0r.kanji.desktop.game.engine.input.InputDeviceKind
import ua.syt0r.kanji.desktop.game.engine.input.InputManager
import ua.syt0r.kanji.desktop.game.engine.input.InputState

/**
 * Gamepad support (spec §14-16, §133 CONTROLLER): layout mapping (Xbox/PS/
 * generic), stick dead zones and the provider → InputState translation the
 * JNA provider performs per frame.
 */
class GamepadMappingTest {

    @Test
    fun `xbox face buttons map to the expected game keys`() {
        assertEquals(GameKey.GamepadA, GamepadLayout.Xbox.buttonMap[0])
        assertEquals(GameKey.GamepadB, GamepadLayout.Xbox.buttonMap[1])
        assertEquals(GameKey.GamepadX, GamepadLayout.Xbox.buttonMap[2])
        assertEquals(GameKey.GamepadY, GamepadLayout.Xbox.buttonMap[3])
        assertEquals(GameKey.GamepadDPadUp, GamepadLayout.Xbox.buttonMap[10])
        assertEquals(GameKey.GamepadDPadRight, GamepadLayout.Xbox.buttonMap[13])
    }

    @Test
    fun `playstation face buttons swap cross and circle`() {
        // PS: physical 0 = Cross (→ A), 1 = Circle (→ B).
        assertEquals(GameKey.GamepadB, GamepadLayout.PlayStation.buttonMap[0])
        assertEquals(GameKey.GamepadA, GamepadLayout.PlayStation.buttonMap[1])
        assertEquals(GameKey.GamepadY, GamepadLayout.PlayStation.buttonMap[2])
        assertEquals(GameKey.GamepadX, GamepadLayout.PlayStation.buttonMap[3])
    }

    @Test
    fun `dead zone filters small stick deflection and scales the rest`() {
        val snapshot = GamepadSnapshot(
            connected = true,
            axes = mapOf(GamepadAxis.LeftX to 0.05f, GamepadAxis.LeftY to 0.5f)
        )
        assertEquals(0f, snapshot.axisFiltered(GamepadAxis.LeftX, 0.18f))
        // (0.5 - 0.18) / (1 - 0.18) ≈ 0.39
        val filtered = snapshot.axisFiltered(GamepadAxis.LeftY, 0.18f)
        assertTrue(filtered in 0.38f..0.40f)
    }

    @Test
    fun `negative axes keep sign after dead zone`() {
        val snapshot = GamepadSnapshot(connected = true, axes = mapOf(GamepadAxis.LeftY to -0.6f))
        val filtered = snapshot.axisFiltered(GamepadAxis.LeftY, 0.18f)
        assertTrue(filtered < 0f)
    }

    @Test
    fun `default scheme binds gamepad keys to actions`() {
        val scheme = ControlScheme.default()
        assertEquals(InputAction.Interact, scheme.actionFor(GameKey.GamepadA))
        assertEquals(InputAction.OpenMenu, scheme.actionFor(GameKey.GamepadStart))
        assertEquals(InputAction.PhotoMode, scheme.actionFor(GameKey.GamepadX))
        assertEquals(InputAction.MoveUp, scheme.actionFor(GameKey.GamepadDPadUp))
    }

    /** A deterministic fake provider that feeds a scripted snapshot. */
    private class ScriptedGamepad : GamepadProvider {
        override val kind = InputDeviceKind.Gamepad
        override val connected = true
        var snapshot = GamepadSnapshot(connected = false)
        private var lastButtons = emptySet<Int>()

        override fun poll(state: InputState, calibration: InputCalibration) {
            val snap = snapshot
            if (!snap.connected) {
                lastButtons = emptySet()
                return
            }
            // Same edge translation as JnaGamepadProvider.poll.
            val scheme = ControlScheme.default()
            for (index in snap.heldButtons - lastButtons) {
                snap.layout.buttonMap[index]?.let { key ->
                    scheme.actionFor(key)?.let { state.press(it) }
                }
            }
            for (index in lastButtons - snap.heldButtons) {
                snap.layout.buttonMap[index]?.let { key ->
                    scheme.actionFor(key)?.let { state.release(it) }
                }
            }
            lastButtons = snap.heldButtons
            val lx = snap.axisFiltered(GamepadAxis.LeftX, calibration.leftStickDeadZone)
            val ly = snap.axisFiltered(GamepadAxis.LeftY, calibration.leftStickDeadZone)
            state.setMoveAxis(lx, ly)
        }
    }

    @Test
    fun `gamepad buttons and sticks drive the shared input state`() {
        val manager = InputManager()
        val pad = ScriptedGamepad()
        manager.gamepad = pad

        manager.beginFrame()
        pad.snapshot = GamepadSnapshot(
            connected = true,
            heldButtons = setOf(0), // A
            axes = mapOf(GamepadAxis.LeftX to 1f, GamepadAxis.LeftY to 0f),
            layout = GamepadLayout.Xbox
        )
        manager.poll()

        assertTrue(manager.state.isPressed(InputAction.Interact))
        assertEquals(1f, manager.state.moveAxis.x)
        assertEquals(0f, manager.state.moveAxis.y)
    }

    @Test
    fun `released gamepad button clears the action`() {
        val manager = InputManager()
        val pad = ScriptedGamepad()
        manager.gamepad = pad

        manager.beginFrame()
        pad.snapshot = GamepadSnapshot(connected = true, heldButtons = setOf(0), layout = GamepadLayout.Xbox)
        manager.poll()
        assertTrue(manager.state.isPressed(InputAction.Interact))

        manager.beginFrame()
        pad.snapshot = GamepadSnapshot(connected = true, heldButtons = emptySet(), layout = GamepadLayout.Xbox)
        manager.poll()
        assertTrue(!manager.state.isPressed(InputAction.Interact))
    }

    @Test
    fun `look axis accumulates as delta like mouse`() {
        val state = InputState()
        state.addLookDelta(2f, 3f)
        assertEquals(Vec2(2f, 3f), state.lookDelta)
        state.beginFrame()
        assertEquals(Vec2.Zero, state.lookDelta)
    }
}
