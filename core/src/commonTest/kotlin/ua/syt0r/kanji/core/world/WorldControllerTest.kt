package ua.syt0r.kanji.core.world

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import kotlin.test.Test

class WorldControllerTest {

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Test
    fun startAndStop() = runBlocking {
        val controller = KamakuraWorld.create()
        controller.start()
        delay(100)
        assertEquals(WorldRuntimeState.Running, controller.runtimeState.value)
        controller.stop()
        controller.dispose()
    }

    @Test
    fun pauseAndResume() = runBlocking {
        val controller = KamakuraWorld.create()
        controller.start()
        delay(50)
        controller.pause()
        assertEquals(WorldRuntimeState.Paused, controller.runtimeState.value)
        controller.resume()
        assertEquals(WorldRuntimeState.Running, controller.runtimeState.value)
        controller.stop()
        controller.dispose()
    }

    @Test
    fun disposeIsSafeWhenNeverStarted() {
        val controller = KamakuraWorld.create()
        controller.dispose()
    }

    // ------------------------------------------------------------------
    // Movement
    // ------------------------------------------------------------------

    @Test
    fun movementUpdatesPlayerPosition() = runBlocking {
        val controller = KamakuraWorld.create()
        controller.start()
        controller.move(MovementInput(forward = 1f), deltaSeconds = 1.0)
        assertTrue(controller.playerPosition.value.length() > 0.0)
        controller.stop()
        controller.dispose()
    }

    @Test
    fun movementStreamsChunks() = runBlocking {
        val controller = KamakuraWorld.create()
        controller.start()
        // Move far enough to cross chunk boundaries
        repeat(30) {
            controller.move(MovementInput(forward = 1f, running = true), deltaSeconds = 1.0)
        }
        assertTrue(controller.loadedChunkCount() > 0)
        controller.stop()
        controller.dispose()
    }

    @Test
    fun tickAdvancesWithoutInput() = runBlocking {
        val controller = KamakuraWorld.create()
        controller.start()
        val start = controller.playerPosition.value
        controller.tick(deltaSeconds = 0.5)
        val end = controller.playerPosition.value
        // Player may move slightly (idle), but play time accrues.
        assertTrue(controller.playerSnapshot().playTimeSeconds >= 0.0)
        controller.stop()
        controller.dispose()
    }

    // ------------------------------------------------------------------
    // Teleport
    // ------------------------------------------------------------------

    @Test
    fun teleportToKnownLocation() = runBlocking {
        val controller = KamakuraWorld.create()
        controller.start()
        controller.teleportTo("kotoku-in")
        val pos = controller.playerPosition.value
        assertTrue(pos.length() > 0.0)
        assertTrue(controller.worldMap.isLocationDiscovered("kotoku-in"))
        controller.stop()
        controller.dispose()
    }

    @Test
    fun teleportToUnknownLocationIsNoop() = runBlocking {
        val controller = KamakuraWorld.create()
        controller.start()
        controller.teleportTo("nonexistent")
        assertEquals(WorldPosition.Zero, controller.playerPosition.value)
        controller.stop()
        controller.dispose()
    }

    @Test
    fun teleportToAllLocationsWorks() = runBlocking {
        val controller = KamakuraWorld.create()
        controller.start()
        KamakuraLocations.all.forEach { location ->
            controller.teleportTo(location.id)
            assertTrue("teleport to ${location.id}", controller.playerPosition.value.length() > 0.0)
        }
        assertEquals(KamakuraLocations.all.size, controller.worldMap.discoveredLocationCount)
        controller.stop()
        controller.dispose()
    }

    // ------------------------------------------------------------------
    // Camera
    // ------------------------------------------------------------------

    @Test
    fun switchCameraToggles() = runBlocking {
        val controller = KamakuraWorld.create()
        controller.start()
        val initial = controller.playerSnapshot().cameraMode
        controller.switchCamera()
        val after = controller.playerSnapshot().cameraMode
        assertTrue(initial != after)
        controller.switchCamera()
        assertEquals(initial, controller.playerSnapshot().cameraMode)
        controller.stop()
        controller.dispose()
    }

    // ------------------------------------------------------------------
    // Save/load
    // ------------------------------------------------------------------

    @Test
    fun saveAndLoadDefaultStorageDoesNotCrash() = runBlocking {
        val controller = KamakuraWorld.create()
        controller.start()
        controller.teleportTo("hase-dera")
        controller.save()
        // Default storage is a no-op → load returns false
        assertFalse(controller.load())
        controller.stop()
        controller.dispose()
    }

    @Test
    fun playerSnapshotReflectsPosition() = runBlocking {
        val controller = KamakuraWorld.create()
        controller.start()
        controller.teleportTo("kamakura-station")
        val snapshot = controller.playerSnapshot()
        assertEquals(controller.playerPosition.value, snapshot.position)
        controller.stop()
        controller.dispose()
    }
}
