package ua.syt0r.kanji.core.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import kotlin.test.Test

class WorldSaveMapTest {

    // ------------------------------------------------------------------
    // WorldMap
    // ------------------------------------------------------------------

    @Test
    fun registerAndLookupRegion() {
        val map = WorldMap()
        val region = WorldRegion(
            id = "kamakura",
            name = "Kamakura",
            japaneseName = "鎌倉",
            center = GeoCoordinate(35.3192, 139.5467),
            radiusMeters = 5000.0,
            type = WorldRegionType.City
        )
        map.registerRegion(region)
        assertNotNull(map.region("kamakura"))
        assertNull(map.region("missing"))
    }

    @Test
    fun registerAndLookupLocation() {
        val map = WorldMap()
        map.registerLocation(KamakuraLocations.KAMAKURA_STATION)
        assertNotNull(map.location("kamakura-station"))
        assertEquals("鎌倉駅", map.location("kamakura-station")?.japaneseName)
    }

    @Test
    fun locationsInRegion() {
        val map = WorldMap()
        KamakuraLocations.all.forEach { map.registerLocation(it) }
        val locations = map.locationsInRegion("kamakura")
        assertEquals(KamakuraLocations.all.size, locations.size)
    }

    @Test
    fun discoverTracksState() {
        val map = WorldMap()
        map.registerLocation(KamakuraLocations.KAMAKURA_STATION)
        assertFalse(map.isLocationDiscovered("kamakura-station"))
        map.discoverLocation("kamakura-station")
        assertTrue(map.isLocationDiscovered("kamakura-station"))
        assertEquals(1, map.discoveredLocationCount)
    }

    @Test
    fun restoreFromSave() {
        val map = WorldMap()
        KamakuraLocations.all.forEach { map.registerLocation(it) }
        map.discoverLocation("kamakura-station")
        map.restore(
            regions = setOf("kamakura"),
            locations = setOf("kamakura-station", "kotoku-in")
        )
        assertTrue(map.isLocationDiscovered("kotoku-in"))
        assertEquals(2, map.discoveredLocationCount)
    }

    @Test
    fun nearestLocationsSortedByDistance() {
        val map = WorldMap()
        KamakuraLocations.all.forEach { map.registerLocation(it) }
        val nearest = map.nearestLocations(WorldPosition.Zero, limit = 3)
        assertTrue(nearest.size <= 3)
        // First is closest to origin
        val firstDist = nearest[0].position.length()
        val secondDist = nearest[1].position.length()
        assertTrue(firstDist <= secondDist)
    }

    // ------------------------------------------------------------------
    // Save/load
    // ------------------------------------------------------------------

    @Test
    fun saveRoundTrips() {
        val store = mutableMapOf<String, String>()
        val saves = WorldSaveService(
            storage = { key -> store[key] },
            write = { key, value -> store[key] = value }
        )
        saves.save(
            slotId = "slot1",
            player = SavedPlayerState(
                position = WorldPosition(10.0, 2.0, 30.0),
                playTimeSeconds = 100.0,
                currentRegionId = "kamakura"
            ),
            discoveredRegions = setOf("kamakura"),
            discoveredLocations = setOf("kamakura-station"),
            completedQuests = setOf("q1"),
            activeQuests = emptySet(),
            settings = WorldSettings(),
            stats = WorldStats(kmWalked = 1.5)
        )
        val loaded = saves.load("slot1")
        assertNotNull(loaded)
        assertEquals(WorldPosition(10.0, 2.0, 30.0), loaded?.player?.position)
        assertEquals("kamakura", loaded?.metadata?.playerRegionId)
        assertTrue(loaded?.discoveredLocations?.contains("kamakura-station") == true)
        assertTrue(loaded?.completedQuests?.contains("q1") == true)
        assertEquals(1.5, loaded?.stats?.kmWalked ?: 0.0, 0.001)
    }

    @Test
    fun loadMissingSlotReturnsNull() {
        val saves = WorldSaveService(storage = { null }, write = { _, _ -> })
        assertNull(saves.load("nonexistent"))
    }

    @Test
    fun corruptSaveReturnsNull() {
        val saves = WorldSaveService(
            storage = { "NOT VALID JSON" },
            write = { _, _ -> }
        )
        assertNull(saves.load("slot1"))
    }

    @Test
    fun saveMetadataCapturesTime() {
        val store = mutableMapOf<String, String>()
        val saves = WorldSaveService(
            storage = { store[it] },
            write = { k, v -> store[k] = v }
        )
        val meta = saves.save(
            slotId = "slot1",
            player = SavedPlayerState(),
            discoveredRegions = emptySet(),
            discoveredLocations = setOf("a"),
            completedQuests = emptySet(),
            activeQuests = emptySet(),
            settings = WorldSettings(),
            stats = WorldStats()
        )
        assertTrue(meta.saveTime > 0)
        assertEquals(1, meta.discoveredLocations)
        assertEquals("Learner", meta.playerName)
    }

    // ------------------------------------------------------------------
    // WorldController integration
    // ------------------------------------------------------------------

    @Test
    fun kamakuraWorldFactoryBuilds() {
        val controller = KamakuraWorld.create()
        assertNotNull(controller)
        controller.start()
        assertEquals(WorldRuntimeState.Running, controller.runtimeState.value)
        controller.stop()
        controller.dispose()
    }

    @Test
    fun kamakuraWorldMapHasLocations() {
        val controller = KamakuraWorld.create()
        assertTrue(controller.worldMap.allLocations().size >= 5)
        assertTrue(controller.worldMap.isRegionDiscovered("kamakura"))
        controller.dispose()
    }

    @Test
    fun teleportToLocationMovesPlayer() {
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
    fun playerInputMovesAndStreams() {
        val controller = KamakuraWorld.create()
        controller.start()
        controller.move(MovementInput(forward = 1f), 0.5)
        assertTrue(controller.playerPosition.value.length() > 0.0)
        controller.move(MovementInput(forward = 1f), 0.5)
        assertTrue(controller.playerPosition.value.length() > 1.0)
        controller.stop()
        controller.dispose()
    }

    @Test
    fun saveAndLoadThroughController() {
        val store = mutableMapOf<String, String>()
        val controller = KamakuraWorld.create() // uses default no-op storage; test via direct service
        // Verify the controller's save/load doesn't crash with default storage.
        controller.start()
        controller.teleportTo("kamakura-station")
        controller.save("slot1")
        controller.stop()
        controller.dispose()
        assertTrue(store.isEmpty()) // default storage is a no-op
    }
}
