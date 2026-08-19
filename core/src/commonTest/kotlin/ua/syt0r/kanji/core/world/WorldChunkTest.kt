package ua.syt0r.kanji.core.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class WorldChunkTest {

    // ------------------------------------------------------------------
    // ChunkCoord
    // ------------------------------------------------------------------

    @Test
    fun fromWorldPositive() {
        val coord = ChunkCoord.fromWorld(WorldPosition(300.0, 0.0, 512.0))
        assertEquals(ChunkCoord(1, 2), coord)
    }

    @Test
    fun fromWorldNegative() {
        val coord = ChunkCoord.fromWorld(WorldPosition(-100.0, 0.0, -300.0))
        assertEquals(ChunkCoord(-1, -2), coord)
    }

    @Test
    fun worldOrigin() {
        val coord = ChunkCoord(2, 3)
        assertEquals(WorldPosition(512.0, 0.0, 768.0), coord.worldOrigin)
    }

    @Test
    fun distanceBetweenChunks() {
        val a = ChunkCoord(0, 0)
        val b = ChunkCoord(2, 2)
        assertEquals(2, a.distanceTo(b))
        val c = ChunkCoord(-1, 3)
        assertEquals(3, c.distanceTo(a))
    }

    @Test
    fun chunkSizeConstant() {
        assertEquals(256.0, CHUNK_SIZE, 0.001)
    }

    // ------------------------------------------------------------------
    // WorldChunk
    // ------------------------------------------------------------------

    @Test
    fun emptyChunkDetected() {
        val chunk = WorldChunk(coord = ChunkCoord(0, 0))
        assertTrue(chunk.isEmpty)
    }

    @Test
    fun chunkWithContentIsNotEmpty() {
        val chunk = WorldChunk(
            coord = ChunkCoord(0, 0),
            buildings = listOf(
                ChunkBuilding("b1", "House", 10.0, 10.0, 8.0, 8.0, 6.0, BuildingType.House)
            )
        )
        assertFalse(chunk.isEmpty)
    }

    @Test
    fun chunkWithWaterIsNotEmpty() {
        val chunk = WorldChunk(coord = ChunkCoord(0, 0), waterLevel = 0.0)
        assertFalse(chunk.isEmpty)
    }

    // ------------------------------------------------------------------
    // ChunkManager streaming
    // ------------------------------------------------------------------

    @Test
    fun updateForPlayerQueuesChunks() {
        val loader = InMemoryChunkLoader { coord -> WorldChunk(coord = coord) }
        val manager = WorldChunkManager(loader, loadRadius = 1)
        manager.updateForPlayer(WorldPosition(0.0, 0.0, 0.0))
        // 3x3 = 9 chunks queued
        assertEquals(9, manager.queuedCount)
    }

    @Test
    fun loadNextLoadsQueuedChunk() = runBlocking {
        val loader = InMemoryChunkLoader { coord -> WorldChunk(coord = coord) }
        val manager = WorldChunkManager(loader, loadRadius = 1)
        manager.updateForPlayer(WorldPosition(0.0, 0.0, 0.0))
        val loaded = manager.loadNext()
        assertNotNull(loaded)
        assertEquals(1, manager.loadedCount)
    }

    @Test
    fun drainQueueLoadsAll() = runBlocking {
        val loader = InMemoryChunkLoader { coord -> WorldChunk(coord = coord) }
        val manager = WorldChunkManager(loader, loadRadius = 1)
        manager.updateForPlayer(WorldPosition(0.0, 0.0, 0.0))
        manager.drainQueue(maxSteps = 100)
        assertEquals(9, manager.loadedCount)
        assertEquals(0, manager.queuedCount)
    }

    @Test
    fun movingPlayerUnloadsDistantChunks() = runBlocking {
        val loader = InMemoryChunkLoader { coord -> WorldChunk(coord = coord) }
        val manager = WorldChunkManager(loader, loadRadius = 1, unloadRadius = 1)
        manager.updateForPlayer(WorldPosition(0.0, 0.0, 0.0))
        manager.drainQueue(maxSteps = 100)
        assertEquals(9, manager.loadedCount)

        // Teleport far away — old chunks unload, new ones queue.
        manager.updateForPlayer(WorldPosition(5000.0, 0.0, 5000.0))
        assertTrue(manager.loadedCount < 9)
        assertTrue(manager.queuedCount > 0)
    }

    @Test
    fun chunkAtReturnsLoadedChunk() = runBlocking {
        val loader = InMemoryChunkLoader { coord -> WorldChunk(coord = coord) }
        val manager = WorldChunkManager(loader, loadRadius = 1)
        manager.updateForPlayer(WorldPosition(0.0, 0.0, 0.0))
        manager.drainQueue(maxSteps = 100)
        val coord = ChunkCoord(0, 0)
        assertNotNull(manager.chunkAt(coord))
        assertTrue(manager.isLoaded(coord))
    }

    @Test
    fun unloadAllClearsEverything() = runBlocking {
        val loader = InMemoryChunkLoader { coord -> WorldChunk(coord = coord) }
        val manager = WorldChunkManager(loader, loadRadius = 1)
        manager.updateForPlayer(WorldPosition(0.0, 0.0, 0.0))
        manager.drainQueue(maxSteps = 100)
        manager.unloadAll()
        assertEquals(0, manager.loadedCount)
        assertEquals(0, manager.queuedCount)
    }

    @Test
    fun failedChunkIsRecorded() {
        val loader = InMemoryChunkLoader { coord ->
            throw RuntimeException("load failed")
        }
        val manager = WorldChunkManager(loader, loadRadius = 0)
        manager.updateForPlayer(WorldPosition(0.0, 0.0, 0.0))
        val chunk = manager.loadNext()
        assertNull(chunk)
        assertEquals(ChunkState.Failed, manager.stateOf(ChunkCoord(0, 0)))
        assertEquals(0, manager.loadedCount)
    }

    @Test
    fun stateOfUnloadedIsUnloaded() {
        val loader = InMemoryChunkLoader { coord -> WorldChunk(coord = coord) }
        val manager = WorldChunkManager(loader)
        assertEquals(ChunkState.Unloaded, manager.stateOf(ChunkCoord(0, 0)))
    }

    @Test
    fun hardCapLimitsLoadedChunks() = runBlocking {
        val loader = InMemoryChunkLoader { coord -> WorldChunk(coord = coord) }
        val manager = WorldChunkManager(loader, loadRadius = 2, maxLoadedChunks = 4)
        manager.updateForPlayer(WorldPosition(0.0, 0.0, 0.0))
        manager.drainQueue(maxSteps = 100)
        assertTrue(manager.loadedCount <= 4)
    }
}
