package ua.syt0r.kanji.desktop.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2
import ua.syt0r.kanji.desktop.game.engine.input.InputAction
import ua.syt0r.kanji.desktop.game.engine.input.InputCalibration
import ua.syt0r.kanji.desktop.game.engine.input.InputState
import ua.syt0r.kanji.desktop.game.engine.input.VirtualTouchProvider

/**
 * Touch controls (spec §16): the virtual provider translates joystick, look
 * drag and contextual buttons into the same [InputState] as every device.
 */
class TouchControlsTest {

    private val touch = VirtualTouchProvider()
    private val state = InputState()
    private val calibration = InputCalibration()

    @Test
    fun `move pad drives the movement axis`() {
        touch.setMovePad(Vec2(0.6f, -0.4f), run = false, state)
        assertEquals(0.6f, state.moveAxis.x)
        assertEquals(-0.4f, state.moveAxis.y)
        assertEquals(0f, state.runAmount)
    }

    @Test
    fun `pushing the stick to the rim runs`() {
        touch.setMovePad(Vec2(0.9f, 0f), run = true, state)
        assertEquals(1f, state.runAmount)
    }

    @Test
    fun `releasing the pad zeroes movement`() {
        touch.setMovePad(Vec2(0.5f, 0.5f), run = false, state)
        touch.setMovePad(Vec2.Zero, run = false, state)
        assertEquals(Vec2.Zero, state.moveAxis)
        assertEquals(0f, state.runAmount)
    }

    @Test
    fun `look drag accumulates like mouse delta`() {
        touch.addLook(3f, 2f, state)
        assertEquals(Vec2(3f, 2f), state.lookDelta)
        state.beginFrame()
        assertEquals(Vec2.Zero, state.lookDelta)
    }

    @Test
    fun `contextual button press and release map to actions`() {
        touch.pressAction(InputAction.Jump, down = true, state)
        assertTrue(state.isPressed(InputAction.Jump))

        touch.pressAction(InputAction.Jump, down = false, state)
        assertFalse(state.isPressed(InputAction.Jump))
    }

    @Test
    fun `quick tap acts as a single interact edge`() {
        touch.tapInteract(state)
        state.beginFrame()
        assertTrue(state.wasPressedThisFrame(InputAction.Interact))
        assertFalse(state.isPressed(InputAction.Interact))
    }

    @Test
    fun `touch provider is always connected and polls nothing`() {
        assertTrue(touch.connected)
        touch.poll(state, calibration) // must not throw or mutate
        assertEquals(Vec2.Zero, state.moveAxis)
    }
}
