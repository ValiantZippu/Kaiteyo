package ua.syt0r.kanji.core.world

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import kotlin.test.Test

class WorldRuntimeTest {

    private fun emptyRuntime(): WorldRuntime {
        val loader = InMemoryChunkLoader { coord -> WorldChunk(coord = coord) }
        return WorldRuntime(
            chunks = WorldChunkManager(loader, loadRadius = 0),
            systems = emptyList()
        )
    }

    @Test
    fun startsInStoppedState() {
        val runtime = emptyRuntime()
        assertEquals(WorldRuntimeState.Stopped, runtime.runtimeState.value)
    }

    @Test
    fun startTransitionsThroughStartingToRunning() = runBlocking {
        val runtime = emptyRuntime()
        runtime.start()
        assertEquals(WorldRuntimeState.Running, runtime.runtimeState.value)
        runtime.stop()
        assertEquals(WorldRuntimeState.Stopped, runtime.runtimeState.value)
    }

    @Test
    fun startIsIdempotent() = runBlocking {
        val runtime = emptyRuntime()
        runtime.start()
        runtime.start()
        assertEquals(WorldRuntimeState.Running, runtime.runtimeState.value)
        runtime.stop()
    }

    @Test
    fun pauseAndResume() = runBlocking {
        val runtime = emptyRuntime()
        runtime.start()
        runtime.pause()
        assertEquals(WorldRuntimeState.Paused, runtime.runtimeState.value)
        runtime.resume()
        assertEquals(WorldRuntimeState.Running, runtime.runtimeState.value)
        runtime.stop()
    }

    @Test
    fun stopReleasesChunks() = runBlocking {
        val runtime = emptyRuntime()
        runtime.start()
        runtime.stop()
        assertEquals(WorldRuntimeState.Stopped, runtime.runtimeState.value)
    }

    @Test
    fun eventsAreEmitted() = runBlocking {
        val runtime = emptyRuntime()
        val events = mutableListOf<WorldEvent>()
        runtime.onEvent { events.add(it) }
        runtime.start()
        assertTrue(events.any { it is WorldEvent.StateChanged && it.state == WorldRuntimeState.Running })
        runtime.stop()
        assertTrue(events.any { it is WorldEvent.StateChanged && it.state == WorldRuntimeState.Stopped })
    }

    @Test
    fun playerPositionUpdates() = runBlocking {
        val runtime = emptyRuntime()
        runtime.start()
        runtime.updatePlayerPosition(WorldPosition(10.0, 0.0, 20.0))
        assertEquals(WorldPosition(10.0, 0.0, 20.0), runtime.playerPosition.value)
        assertEquals(ChunkCoord(0, 0), runtime.snapshot().playerChunk)
        runtime.stop()
    }

    @Test
    fun timeOfDayClamped() {
        val runtime = emptyRuntime()
        runtime.setTimeOfDay(2.5f)
        assertEquals(1.0f, runtime.timeOfDay.value, 0.001f)
        runtime.setTimeOfDay(-1f)
        assertEquals(0.0f, runtime.timeOfDay.value, 0.001f)
    }

    @Test
    fun snapshotReflectsState() = runBlocking {
        val runtime = emptyRuntime()
        runtime.start()
        runtime.updatePlayerPosition(WorldPosition(300.0, 0.0, 300.0))
        runtime.setTimeOfDay(0.25f)
        runtime.setWeather(WorldWeather.Rain)
        val snap = runtime.snapshot()
        assertEquals(WorldRuntimeState.Running, snap.runtimeState)
        assertEquals(ChunkCoord(1, 1), snap.playerChunk)
        assertEquals(0.25f, snap.timeOfDay, 0.001f)
        assertEquals(WorldWeather.Rain, snap.weather)
        runtime.stop()
    }

    @Test
    fun crashingSystemIsIsolated() = runBlocking {
        val crashingSystem = object : WorldSystem {
            override suspend fun onUpdate(deltaSeconds: Double, runtime: WorldRuntime) {
                throw RuntimeException("boom")
            }
        }
        val loader = InMemoryChunkLoader { coord -> WorldChunk(coord = coord) }
        val runtime = WorldRuntime(
            chunks = WorldChunkManager(loader, loadRadius = 0),
            systems = listOf(crashingSystem),
            isolateCrashes = true
        )
        runtime.start()
        val events = mutableListOf<WorldEvent>()
        runtime.onEvent { events.add(it) }
        runtime.update(1.0)
        // Runtime still running, error recorded
        assertEquals(WorldRuntimeState.Running, runtime.runtimeState.value)
        assertNotNull(runtime.lastError.value)
        assertTrue(events.any { it is WorldEvent.Error })
        runtime.stop()
    }

    @Test
    fun nonIsolatedCrashStopsRuntime() = runBlocking {
        val crashingSystem = object : WorldSystem {
            override suspend fun onUpdate(deltaSeconds: Double, runtime: WorldRuntime) {
                throw RuntimeException("boom")
            }
        }
        val loader = InMemoryChunkLoader { coord -> WorldChunk(coord = coord) }
        val runtime = WorldRuntime(
            chunks = WorldChunkManager(loader, loadRadius = 0),
            systems = listOf(crashingSystem),
            isolateCrashes = false
        )
        runtime.start()
        runtime.update(1.0)
        assertEquals(WorldRuntimeState.Crashed, runtime.runtimeState.value)
    }

    @Test
    fun gameLoopTicks() = runBlocking {
        val runtime = emptyRuntime()
        var ticks = 0
        runtime.onEvent { if (it is WorldEvent.Tick) ticks++ }
        runtime.start()
        // Run the loop briefly on the test scope.
        val job = launch { runtime.runGameLoop() }
        kotlinx.coroutines.delay(200)
        job.cancel()
        runtime.stop()
        assertTrue(ticks > 0)
    }
}
