package ua.syt0r.kanji.core.world

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// ============================================================
// WORLD — CONTROLLER
// ------------------------------------------------------------
// The app-facing handle to the World. The app shell holds one
// WorldController; it owns the runtime scope, starts/stops the
// runtime, and forwards player input. The app never touches the
// runtime internals directly.
// ============================================================

/**
 * The app-facing world handle. Safe to hold from the UI layer.
 */
class WorldController(
    private val runtime: WorldRuntime,
    private val player: PlayerController,
    private val map: WorldMap = WorldMap(),
    private val saves: WorldSaveService = WorldSaveService()
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null
    private var started = false

    /** Observable runtime state. */
    val runtimeState = runtime.runtimeState

    /** Observable player position. */
    val playerPosition = runtime.playerPosition

    /** Observable time of day. */
    val timeOfDay = runtime.timeOfDay

    /** Observable weather. */
    val weather = runtime.weather

    /** Observable last error. */
    val lastError = runtime.lastError

    val worldMap: WorldMap get() = map

    /** Starts the world and its game loop. Idempotent. */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            runtime.start()
            if (runtime.runtimeState.value == WorldRuntimeState.Running) {
                loopJob = scope.launch { runtime.runGameLoop() }
            }
        }
    }

    /** Stops the world and releases everything. */
    fun stop() {
        started = false
        loopJob?.cancel()
        loopJob = null
        scope.launch { runtime.stop() }
    }

    /** Pauses the game loop. */
    fun pause() = runtime.pause()

    /** Resumes the game loop. */
    fun resume() = runtime.resume()

    /** Applies one frame of player input. */
    fun move(input: MovementInput, deltaSeconds: Double) {
        val updated = player.update(input, deltaSeconds)
        runtime.updatePlayerPosition(updated.position)
        // Stream chunks around the new position.
        runtime.chunks.updateForPlayer(updated.position)
        runtime.chunks.drainQueue(maxSteps = 4)
    }

    /** Convenience: move with a fixed 1/20 s frame (screen loop drives this). */
    fun move(input: MovementInput) = move(input, deltaSeconds = 0.05)

    /** Advances one tick (used by the screen's fixed-rate input loop). */
    fun tick(deltaSeconds: Double = 0.05) {
        if (runtime.runtimeState.value != WorldRuntimeState.Running) return
        move(MovementInput(), deltaSeconds)
        runtime.chunks.drainQueue(maxSteps = 2)
    }

    /** Number of currently loaded chunks. */
    fun loadedChunkCount(): Int = runtime.chunks.loadedCount

    /** Number of queued chunks waiting to load. */
    fun queuedChunkCount(): Int = runtime.chunks.queuedCount

    /** Switches the player's camera mode. */
    fun switchCamera() = player.switchCamera()

    /** The current player entity (for UI readouts). */
    fun playerSnapshot() = player.player

    /** Teleports the player to a location. */
    fun teleportTo(locationId: String) {
        val location = map.location(locationId) ?: return
        player.teleport(location.position)
        runtime.updatePlayerPosition(location.position)
        runtime.chunks.updateForPlayer(location.position)
        map.discoverLocation(locationId)
    }

    /** Boards a vehicle by id. */
    fun board(vehicleId: String) {
        player.board(vehicleId)
    }

    /** Alights at the given position. */
    fun alight(position: WorldPosition) {
        player.alight(position)
        runtime.updatePlayerPosition(position)
    }

    /** Saves the current world state. */
    fun save(slotId: String = "slot1") {
        val current = player.player
        val stats = WorldStats(
            npcsMet = 0,
            kmWalked = current.distanceWalkedMeters / 1000.0,
            locationsVisited = map.discoveredLocationCount
        )
        saves.save(
            slotId = slotId,
            player = SavedPlayerState(
                position = current.position,
                facingYawDegrees = current.facing.yawDegrees,
                cameraMode = current.cameraMode.name,
                currentRegionId = map.allRegions().firstOrNull()?.id,
                currentLocationId = current.currentLocationId,
                playTimeSeconds = current.playTimeSeconds,
                distanceWalkedMeters = current.distanceWalkedMeters
            ),
            discoveredRegions = map.allRegions().filter { map.isRegionDiscovered(it.id) }.map { it.id }.toSet(),
            discoveredLocations = map.allLocations().filter { map.isLocationDiscovered(it.id) }.map { it.id }.toSet(),
            completedQuests = emptySet(),
            activeQuests = emptySet(),
            settings = WorldSettings(),
            stats = stats
        )
    }

    /** Loads a save slot and teleports the player. */
    fun load(slotId: String): Boolean {
        val data = saves.load(slotId) ?: return false
        player.teleport(
            data.player.position,
            Facing(yawDegrees = data.player.facingYawDegrees)
        )
        runtime.updatePlayerPosition(data.player.position)
        map.restore(data.discoveredRegions, data.discoveredLocations)
        return true
    }

    /** Releases the controller (call when the world screen closes). */
    fun dispose() {
        stop()
        scope.cancel()
    }
}

// ============================================================
// KAMAKURA WORLD FACTORY
// ------------------------------------------------------------
// Builds the Kamakura vertical slice: terrain, ocean, buildings,
// NPCs, trains, and the full runtime wired together.
// ============================================================

/**
 * A running world session — the controller plus the player it drives.
 * Bundling them keeps the screen bridge and the runtime on the same
 * player instance.
 */
class WorldSession(
    val controller: WorldController,
    val player: PlayerController
)

/**
 * Builds a ready-to-start Kamakura world.
 */
object KamakuraWorld {

    /**
     * Creates the world runtime and controller for the Kamakura slice.
     */
    fun create(): WorldController = createSession().controller

    /**
     * Creates a full session (controller + player) for the Kamakura slice.
     */
    fun createSession(): WorldSession {
        // Geographic data
        val projection = KamakuraLocations.PROJECTION
        val ocean = KamakuraOcean(coastlineOffset = -500.0)

        // Terrain
        val terrain = TerrainSystem(
            TerrainGenerator(
                TerrainConfig(
                    baseHeight = 0.0,
                    amplitude = 55.0,
                    coastlineOffset = -500.0,
                    seaLevel = 0.0,
                    maxHeight = 110.0
                ),
                seed = 42
            )
        )

        // Water
        val water = WaterSystem(
            WaterConfig(baseLevel = 0.0, waveAmplitude = 0.3),
            isOcean = { x, z -> ocean.isOcean(x, z) }
        )

        // Chunk loader: procedural generation per chunk.
        val chunkLoader = InMemoryChunkLoader { coord ->
            val gen = TerrainGenerator(
                TerrainConfig(amplitude = 55.0, coastlineOffset = -500.0),
                seed = 42
            )
            val buildings = BuildingGenerator(seed = 7)
            val placement = buildings.generateForChunk(coord)
            WorldChunk(
                coord = coord,
                heightmap = gen.generateChunk(coord),
                buildings = placement.buildings,
                roads = placement.roads,
                waterLevel = if (ocean.isOcean(coord.worldOrigin.x, coord.worldOrigin.z)) 0.0 else null
            )
        }
        val chunks = WorldChunkManager(chunkLoader)

        // Time & weather
        val timeWeather = TimeWeatherSystem()

        // NPCs
        val npcs = NpcSystem()
        KamakuraNpcs.buildAll().forEach { npcs.addNpc(it) }

        // Trains: Enoden from Kamakura station toward Hase.
        val trains = TrainSystem()
        val kamakuraStation = KamakuraLocations.KAMAKURA_STATION.position
        val hasePosition = KamakuraLocations.HASE_DERA.position
        trains.addTrain(
            Train(
                id = "enoden-1",
                name = "Enoden 300形",
                position = kamakuraStation,
                speed = 8.0,
                direction = 1,
                stations = listOf(kamakuraStation, hasePosition)
            )
        )

        // Vehicles
        val vehicles = VehicleSystem(vehicleLimit = 10)

        // Player controller wired to terrain/water
        val player = PlayerController(
            settings = PlayerSettings(),
            terrainHeight = { x, z -> terrain.heightAt(x, z) },
            waterLevel = { x, z -> water.surfaceHeightAt(x, z) },
            isBlocked = { _, _, _ -> false }
        )

        // Runtime
        val runtime = WorldRuntime(
            chunks = chunks,
            systems = listOf(
                timeWeather,
                terrain,
                water,
                npcs,
                trains,
                vehicles
            ),
            targetFps = 60,
            isolateCrashes = true
        )

        // World map with Kamakura data
        val map = WorldMap()
        map.registerRegion(KamakuraLocations.KAMAKURA)
        KamakuraLocations.all.forEach { map.registerLocation(it) }
        map.discoverRegion("kamakura")

        return WorldSession(WorldController(runtime, player, map), player)
    }
}
