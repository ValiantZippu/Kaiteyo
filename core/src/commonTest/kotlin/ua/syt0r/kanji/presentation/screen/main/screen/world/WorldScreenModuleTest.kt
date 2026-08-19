package ua.syt0r.kanji.presentation.screen.main.screen.world

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import ua.syt0r.kanji.core.world.KamakuraWorld
import ua.syt0r.kanji.core.world.WorldRuntimeState
import ua.syt0r.kanji.core.world.WorldSession

// ============================================================
// WORLD — MODULE WIRING TEST
// ------------------------------------------------------------
// Verifies the World screen Koin module produces a working
// session, a bridge that exposes only the port surface, and a
// ViewModel that drives the runtime through the bridge.
// ============================================================

class WorldScreenModuleTest {

    @Test
    fun kamakuraWorld_createsUsableSession() {
        val session = KamakuraWorld.createSession()

        assertIs<WorldSession>(session)
        assertNotNull(session.controller)
        assertNotNull(session.player)
        // The map must know the Kamakura region and its landmarks.
        assertTrue(session.controller.worldMap.allRegions().isNotEmpty())
        assertTrue(session.controller.worldMap.allLocations().size >= 4)
    }

    @Test
    fun bridge_exposesPortSurface() {
        val session = KamakuraWorld.createSession()
        val bridge = WorldScreenControllerBridge(session)

        // Port surface exposes live readouts that map to the session.
        assertNotNull(bridge.runtimeState.value)
        assertNotNull(bridge.playerPosition.value)
        assertNotNull(bridge.weather.value)
        assertNotNull(bridge.timeOfDay.value)
        assertNotNull(bridge.worldMap)

        // The player behind the bridge is the session's player.
        assertEquals(session.player.player.position, bridge.playerSnapshot().position)
    }

    @Test
    fun bridge_controlsActuallyAffectTheWorld() = runBlocking {
        val session = KamakuraWorld.createSession()
        val bridge = WorldScreenControllerBridge(session)

        bridge.start()
        delay(100)
        try {
            // Starting must flip the runtime into Running.
            assertEquals(WorldRuntimeState.Running, bridge.runtimeState.value)

            // A teleport to a real location must move the player.
            val locationId = bridge.worldMap.allLocations().first().id
            bridge.teleportTo(locationId)
            val expected = bridge.worldMap.location(locationId)?.position
            assertNotNull(expected)
            assertEquals(expected, bridge.playerSnapshot().position)

            // Camera switching changes the camera mode.
            val before = bridge.playerSnapshot().cameraMode
            bridge.switchCamera()
            assertFalse(bridge.playerSnapshot().cameraMode == before)

            // Tick with no input keeps the world stable.
            bridge.tick()
            assertFalse(bridge.runtimeState.value == WorldRuntimeState.Crashed)
            assertEquals(null, bridge.lastError.value)
        } finally {
            bridge.stop()
            bridge.dispose()
        }
    }
}
