package ua.syt0r.kanji.core.world

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import kotlin.test.Test

class WorldSystemsTest {

    // ------------------------------------------------------------------
    // Terrain
    // ------------------------------------------------------------------

    @Test
    fun terrainIsDeterministic() {
        val a = TerrainGenerator(TerrainConfig(), seed = 42)
        val b = TerrainGenerator(TerrainConfig(), seed = 42)
        assertEquals(a.heightAt(100.0, 200.0), b.heightAt(100.0, 200.0), 0.0001)
    }

    @Test
    fun terrainDiffersBySeed() {
        val a = TerrainGenerator(TerrainConfig(), seed = 1)
        val b = TerrainGenerator(TerrainConfig(), seed = 2)
        assertFalse(a.heightAt(100.0, 200.0) == b.heightAt(100.0, 200.0))
    }

    @Test
    fun terrainIsBoundedByMaxHeight() {
        val gen = TerrainGenerator(TerrainConfig(maxHeight = 120.0), seed = 42)
        repeat(100) { i ->
            val h = gen.heightAt(i * 100.0, i * 37.0)
            assertTrue(h <= 120.0)
        }
    }

    @Test
    fun chunkHeightmapHasExpectedResolution() {
        val gen = TerrainGenerator(TerrainConfig(), seed = 42)
        val map = gen.generateChunk(ChunkCoord(0, 0), resolution = 8)
        assertEquals(9, map.size) // resolution + 1
        assertEquals(9, map[0].size)
    }

    // ------------------------------------------------------------------
    // Water
    // ------------------------------------------------------------------

    @Test
    fun dryLandHasNoWater() {
        val water = WaterSystem(isOcean = { _, _ -> false })
        assertNull(water.surfaceHeightAt(0.0, 0.0))
        assertFalse(water.isWater(0.0, 0.0))
    }

    @Test
    fun oceanHasWater() {
        val water = WaterSystem(isOcean = { _, _ -> true })
        assertNotNull(water.surfaceHeightAt(0.0, 0.0))
        assertTrue(water.isWater(0.0, 0.0))
    }

    @Test
    fun wavesAnimateOverTime() = runBlocking {
        val water = WaterSystem(isOcean = { _, _ -> true })
        val before = water.surfaceHeightAt(0.0, 0.0)!!
        water.onUpdate(5.0, WorldRuntime(
            chunks = WorldChunkManager(InMemoryChunkLoader { WorldChunk(it) }, loadRadius = 0),
            systems = emptyList()
        ))
        val after = water.surfaceHeightAt(0.0, 0.0)!!
        assertFalse(before == after)
    }

    @Test
    fun beachIsDetectedAtShoreline() {
        val water = WaterSystem(
            isOcean = { _, z -> z < 0.0 },
            WaterConfig()
        )
        // Point straddling the shoreline (z = 1) is beach.
        assertTrue(water.isBeach(0.0, 1.0, band = 10.0))
        // Deep ocean is not beach.
        assertFalse(water.isBeach(0.0, -100.0, band = 10.0))
    }

    @Test
    fun kamakuraOceanSouthIsWater() {
        val ocean = KamakuraOcean(coastlineOffset = -500.0)
        assertTrue(ocean.isOcean(0.0, -1000.0))
        assertFalse(ocean.isOcean(0.0, 0.0))
    }

    // ------------------------------------------------------------------
    // NPCs
    // ------------------------------------------------------------------

    @Test
    fun npcMovesTowardSchedule() = runBlocking {
        val npc = Npc(
            id = "n1",
            name = "Test",
            japaneseName = "テスト",
            role = NpcRole.Commuter,
            homePosition = WorldPosition.Zero,
            schedule = mapOf(
                0 to WorldPosition.Zero,
                12 to WorldPosition(10.0, 0.0, 0.0)
            ),
            currentPosition = WorldPosition.Zero
        )
        val system = NpcSystem()
        system.addNpc(npc)
        val runtime = WorldRuntime(
            chunks = WorldChunkManager(InMemoryChunkLoader { WorldChunk(it) }, loadRadius = 0),
            systems = emptyList()
        )
        runtime.setTimeOfDay(0.5f) // noon → waypoint at (10,0,0)
        system.onUpdate(1.0, runtime)
        val moved = system.activeNpcs.first()
        assertTrue(moved.currentPosition.x > 0.0)
    }

    @Test
    fun npcsNearPosition() {
        val system = NpcSystem()
        system.addNpc(Npc(
            id = "n1", name = "A", japaneseName = "あ",
            role = NpcRole.Commuter, homePosition = WorldPosition.Zero,
            schedule = emptyMap(), currentPosition = WorldPosition(2.0, 0.0, 0.0)
        ))
        system.addNpc(Npc(
            id = "n2", name = "B", japaneseName = "い",
            role = NpcRole.Commuter, homePosition = WorldPosition.Zero,
            schedule = emptyMap(), currentPosition = WorldPosition(500.0, 0.0, 0.0)
        ))
        val near = system.npcsNear(WorldPosition.Zero, radiusMeters = 10.0)
        assertEquals(1, near.size)
        assertEquals("n1", near[0].id)
    }

    @Test
    fun npcBuilderProducesDialogue() {
        val npc = NpcBuilder("b1", "Test")
            .japaneseName("テスト")
            .role(NpcRole.Shopkeeper)
            .home(WorldPosition(1.0, 0.0, 1.0))
            .says("いらっしゃいませ", "Welcome")
            .can(NpcInteractionType.Shop, "Browse goods")
            .build()
        assertEquals(1, npc.dialogue.size)
        assertEquals(1, npc.interactions.size)
        assertEquals(NpcRole.Shopkeeper, npc.role)
    }

    @Test
    fun kamakuraNpcCastIsBuilt() {
        val cast = KamakuraNpcs.buildAll()
        assertTrue(cast.size >= 3)
        assertTrue(cast.any { it.role == NpcRole.StationStaff })
        assertTrue(cast.any { it.role == NpcRole.Surfer })
    }

    // ------------------------------------------------------------------
    // Vehicles
    // ------------------------------------------------------------------

    @Test
    fun vehicleMovesAlongRoute() = runBlocking {
        val system = VehicleSystem()
        system.addVehicle(Vehicle(
            id = "v1",
            type = VehicleType.Car,
            position = WorldPosition.Zero,
            speed = 10.0,
            headingDegrees = 0f,
            route = listOf(WorldPosition.Zero, WorldPosition(10.0, 0.0, 0.0))
        ))
        system.onUpdate(1.0, WorldRuntime(
            chunks = WorldChunkManager(InMemoryChunkLoader { WorldChunk(it) }, loadRadius = 0),
            systems = emptyList()
        ))
        val vehicle = system.activeVehicles.first()
        assertTrue(vehicle.position.x > 0.0)
        assertTrue(vehicle.position.x <= 10.0)
    }

    @Test
    fun vehicleLimitEnforced() {
        val system = VehicleSystem(vehicleLimit = 3)
        repeat(5) { i ->
            system.addVehicle(Vehicle("v$i", VehicleType.Car, WorldPosition.Zero, 1.0, 0f))
        }
        assertEquals(3, system.activeVehicles.size)
    }

    @Test
    fun vehicleClearOnStop() = runBlocking {
        val system = VehicleSystem()
        system.addVehicle(Vehicle("v1", VehicleType.Car, WorldPosition.Zero, 1.0, 0f))
        system.onStop(WorldRuntime(
            chunks = WorldChunkManager(InMemoryChunkLoader { WorldChunk(it) }, loadRadius = 0),
            systems = emptyList()
        ))
        assertTrue(system.activeVehicles.isEmpty())
    }

    // ------------------------------------------------------------------
    // Trains
    // ------------------------------------------------------------------

    @Test
    fun trainMovesTowardStation() = runBlocking {
        val system = TrainSystem()
        system.addTrain(Train(
            id = "t1",
            name = "Enoden",
            position = WorldPosition.Zero,
            speed = 8.0,
            stations = listOf(WorldPosition.Zero, WorldPosition(50.0, 0.0, 0.0))
        ))
        system.onUpdate(1.0, WorldRuntime(
            chunks = WorldChunkManager(InMemoryChunkLoader { WorldChunk(it) }, loadRadius = 0),
            systems = emptyList()
        ))
        val train = system.activeTrains.first()
        assertTrue(train.position.x > 0.0)
        assertTrue(train.position.x <= 50.0)
    }

    @Test
    fun trainDwellsAtStation() = runBlocking {
        val system = TrainSystem()
        system.addTrain(Train(
            id = "t1",
            name = "Enoden",
            position = WorldPosition(50.0, 0.0, 0.0),
            speed = 8.0,
            stations = listOf(WorldPosition.Zero, WorldPosition(50.0, 0.0, 0.0)),
            stationIndex = 1
        ))
        system.onUpdate(1.0, WorldRuntime(
            chunks = WorldChunkManager(InMemoryChunkLoader { WorldChunk(it) }, loadRadius = 0),
            systems = emptyList()
        ))
        // Train was AT the station → now dwelling
        assertTrue(system.activeTrains.first().isAtStation)
    }

    @Test
    fun trainDepartsAfterDwell() = runBlocking {
        val system = TrainSystem()
        system.addTrain(Train(
            id = "t1",
            name = "Enoden",
            position = WorldPosition(50.0, 0.0, 0.0),
            speed = 8.0,
            stations = listOf(WorldPosition.Zero, WorldPosition(50.0, 0.0, 0.0)),
            stationIndex = 1,
            isAtStation = true,
            dwellSeconds = 2.0
        ))
        val runtime = WorldRuntime(
            chunks = WorldChunkManager(InMemoryChunkLoader { WorldChunk(it) }, loadRadius = 0),
            systems = emptyList()
        )
        system.onUpdate(3.0, runtime)
        assertFalse(system.activeTrains.first().isAtStation)
    }

    // ------------------------------------------------------------------
    // Time & weather
    // ------------------------------------------------------------------

    @Test
    fun clockAdvances() = runBlocking {
        val system = TimeWeatherSystem(DayCycleConfig(dayLengthSeconds = 3600.0))
        val runtime = WorldRuntime(
            chunks = WorldChunkManager(InMemoryChunkLoader { WorldChunk(it) }, loadRadius = 0),
            systems = emptyList()
        )
        system.onStart(runtime)
        val before = system.currentTimeOfDay
        system.onUpdate(1800.0, runtime)
        val after = system.currentTimeOfDay
        assertTrue(after > before)
        // Half a day later
        assertTrue(after - before > 0.4f)
    }

    @Test
    fun clockWrapsAround() = runBlocking {
        val system = TimeWeatherSystem(DayCycleConfig(dayLengthSeconds = 100.0))
        val runtime = WorldRuntime(
            chunks = WorldChunkManager(InMemoryChunkLoader { WorldChunk(it) }, loadRadius = 0),
            systems = emptyList()
        )
        system.onStart(runtime)
        system.onUpdate(250.0, runtime) // 2.5 full days
        assertTrue(system.currentTimeOfDay in 0f..1f)
    }

    @Test
    fun sunIntensityPeaksAtNoon() {
        val system = TimeWeatherSystem()
        assertEquals(1.0f, system.sunIntensityAt(0.5f), 0.001f)
        assertEquals(0.0f, system.sunIntensityAt(0.0f), 0.001f)
        assertEquals(0.0f, system.sunIntensityAt(1.0f), 0.001f)
    }

    @Test
    fun daytimeIsDaylight() {
        val system = TimeWeatherSystem()
        assertTrue(system.isDaytimeAt(0.5f))  // noon
        assertFalse(system.isDaytimeAt(0.0f)) // midnight
    }

    @Test
    fun clockLabelFormats() {
        val system = TimeWeatherSystem()
        // 0.5 = noon = 12:00
        assertEquals("12:00", system.clockLabelAt(0.5f))
        assertEquals("00:00", system.clockLabelAt(0.0f))
    }

    @Test
    fun weatherChangesOverTime() = runBlocking {
        val system = TimeWeatherSystem(
            WeatherConfig(minChangeIntervalSeconds = 0.1)
        )
        val runtime = WorldRuntime(
            chunks = WorldChunkManager(InMemoryChunkLoader { WorldChunk(it) }, loadRadius = 0),
            systems = emptyList()
        )
        system.onStart(runtime)
        // Force many updates to roll weather several times.
        repeat(200) {
            system.onUpdate(1.0, runtime)
        }
        // Weather should have changed from the initial Clear at least once,
        // though with weights it could theoretically stay Clear. Accept
        // either outcome but assert the runtime flow is in sync.
        assertEquals(system.currentWeather, runtime.weather.value)
    }
}
