package ua.syt0r.kanji.desktop.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import ua.syt0r.kanji.desktop.game.engine.input.ControlScheme
import ua.syt0r.kanji.desktop.game.engine.input.GameKey
import ua.syt0r.kanji.desktop.game.engine.input.InputAction
import ua.syt0r.kanji.desktop.game.engine.input.InputManager
import ua.syt0r.kanji.desktop.game.engine.input.JnaGamepadProvider

/**
 * Control rebinding (spec §15): scheme changes flow through [InputManager]
 * into the keyboard provider immediately, reset restores defaults, and the
 * gamepad press queue stays quiet without hardware (hot-plug safe).
 */
class RebindAndHotplugTest {

    @Test
    fun `binding a key makes the keyboard provider use it next frame`() {
        val manager = InputManager()
        manager.bind(InputAction.Jump, GameKey.F)

        manager.beginFrame()
        manager.onKey(GameKey.F, down = true)
        manager.poll()
        assertTrue(manager.state.isPressed(InputAction.Jump))

        manager.onKey(GameKey.F, down = false)
        manager.poll()
        assertFalse(manager.state.isPressed(InputAction.Jump))
    }

    @Test
    fun `binding appends without clobbering existing keys`() {
        val manager = InputManager()
        manager.bind(InputAction.Jump, GameKey.F)
        val keys = manager.scheme.keysFor(InputAction.Jump)
        assertTrue(GameKey.Space in keys)
        assertTrue(GameKey.F in keys)
    }

    @Test
    fun `unbinding removes only the requested key`() {
        val manager = InputManager()
        manager.unbind(InputAction.Jump, GameKey.Space)
        val keys = manager.scheme.keysFor(InputAction.Jump)
        assertFalse(GameKey.Space in keys)
        assertTrue(keys.isNotEmpty())
    }

    @Test
    fun `reset restores the default scheme`() {
        val manager = InputManager()
        manager.bind(InputAction.Interact, GameKey.G)
        manager.resetBindings()
        assertEquals(ControlScheme.default(), manager.scheme)
    }

    @Test
    fun `rebound key no longer triggers its old action`() {
        val manager = InputManager()
        // Rebind E away from Interact by unbinding it.
        manager.unbind(InputAction.Interact, GameKey.E)

        manager.beginFrame()
        manager.onKey(GameKey.E, down = true)
        manager.poll()
        assertFalse(manager.state.isPressed(InputAction.Interact))
    }

    @Test
    fun `disconnected gamepad reports no pending button press`() {
        val provider = JnaGamepadProvider()
        provider.stop() // never actually connects on a headless test host
        assertFalse(provider.connected)
        assertNull(provider.consumeNewButtonPress())
    }
}
