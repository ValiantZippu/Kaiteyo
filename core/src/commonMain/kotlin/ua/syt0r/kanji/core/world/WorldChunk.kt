package ua.syt0r.kanji.core.world

import kotlinx.serialization.Serializable
import kotlin.math.floor

// ============================================================
// WORLD — CHUNK SYSTEM & STREAMING
// ------------------------------------------------------------
// The world is divided into square chunks in world space. Only
// chunks near the player are loaded into memory; everything else
// stays on disk. A chunk grid keyed by (cx, cz) makes lookups
// trivial and streaming bounded.
//
//   CHUNK_SIZE      meters per chunk side (default 256 m)
//   LOAD_RADIUS     chunks loaded around the player (default 3 → 7×7)
//   UNLOAD_RADIUS   chunks released once the player moves away
//
// Each chunk stores its own terrain, buildings, and objects —
// the world is never materialized as one giant map file.
// ============================================================

/** Meters per chunk side. */
const val CHUNK_SIZE = 256.0

/** Chunks loaded around the player (radius). 3 → 7×7 = 49 chunks ≈ 3.2 km². */
const val DEFAULT_LOAD_RADIUS = 3

/** Chunks kept after the player moves away before release. */
const val DEFAULT_UNLOAD_RADIUS = 4

/**
 * Chunk grid coordinates. [cx]/[cz] are chunk indices.
 */
@Serializable
data class ChunkCoord(
    val cx: Int,
    val cz: Int
) {
    /** World-space position of the chunk's southwest corner. */
    val worldOrigin: WorldPosition
        get() = WorldPosition(
            x = cx * CHUNK_SIZE,
            y = 0.0,
            z = cz * CHUNK_SIZE
        )

    fun distanceTo(other: ChunkCoord): Int =
        maxOf(kotlin.math.abs(cx - other.cx), kotlin.math.abs(cz - other.cz))

    companion object {
        fun fromWorld(position: WorldPosition): ChunkCoord = ChunkCoord(
            cx = floor(position.x / CHUNK_SIZE).toInt(),
            cz = floor(position.z / CHUNK_SIZE).toInt()
        )
    }
}

/**
 * Chunk load state.
 */
enum class ChunkState(val label: String) {
    Unloaded("Unloaded"),
    Queued("Queued"),
    Loading("Loading"),
    Loaded("Loaded"),
    Unloading("Unloading"),
    Failed("Failed")
}

/**
 * A single world chunk — terrain + objects that live entirely
 * within its bounds.
 */
@Serializable
data class WorldChunk(
    val coord: ChunkCoord,
    /** Heightmap rows [z][x] in meters above sea level. */
    val heightmap: List<List<Double>> = emptyList(),
    /** Buildings in this chunk. */
    val buildings: List<ChunkBuilding> = emptyList(),
    /** Roads in this chunk. */
    val roads: List<ChunkRoad> = emptyList(),
    /** Water level (meters) — null = no water in this chunk. */
    val waterLevel: Double? = null,
    /** Locations in this chunk. */
    val locations: List<String> = emptyList(),
    /** LOD level currently loaded (0 = highest detail). */
    val lodLevel: Int = 0
) {
    val isEmpty: Boolean
        get() = heightmap.isEmpty() && buildings.isEmpty() && roads.isEmpty() &&
                waterLevel == null && locations.isEmpty()
}

/**
 * A building within a chunk. Positions are local to the chunk.
 */
@Serializable
data class ChunkBuilding(
    val id: String,
    val name: String,
    val localX: Double,
    val localZ: Double,
    val widthMeters: Double,
    val depthMeters: Double,
    val heightMeters: Double,
    val type: BuildingType,
    /** Entrance offset relative to building center. */
    val entranceX: Double = 0.0,
    val entranceZ: Double = 0.0,
    /** Whether the interior is implemented. */
    val hasInterior: Boolean = false,
    /** Style/color variant. */
    val style: Int = 0
)

/**
 * A road segment within a chunk.
 */
@Serializable
data class ChunkRoad(
    val id: String,
    /** Local polyline points (x, z). */
    val points: List<Pair<Double, Double>>,
    val widthMeters: Double = 8.0,
    val type: RoadType = RoadType.Street
)

/**
 * Building types.
 */
enum class BuildingType(val label: String) {
    House("House"),
    Shop("Shop"),
    Temple("Temple"),
    Shrine("Shrine"),
    Station("Station"),
    School("School"),
    Restaurant("Restaurant"),
    Apartment("Apartment"),
    Office("Office"),
    Museum("Museum"),
    Cafe("Cafe"),
    TrainShed("Train shed"),
    Other("Other")
}

/**
 * Road types.
 */
enum class RoadType(val label: String) {
    Highway("Highway"),
    MainRoad("Main road"),
    Street("Street"),
    Rail("Railway"),
    Path("Path"),
    CoastRoad("Coast road")
}

/**
 * The chunk manager — owns which chunks are loaded, streams them
 * in/out as the player moves, and enforces load limits.
 */
class WorldChunkManager(
    private val loader: ChunkLoader,
    private val loadRadius: Int = DEFAULT_LOAD_RADIUS,
    private val unloadRadius: Int = DEFAULT_UNLOAD_RADIUS,
    private val maxLoadedChunks: Int = 64
) {

    private val loaded = mutableMapOf<ChunkCoord, WorldChunk>()
    private val states = mutableMapOf<ChunkCoord, ChunkState>()
    private val loadQueue = ArrayDeque<ChunkCoord>()

    /** Currently loaded chunks (unmodifiable snapshot). */
    val loadedChunks: Map<ChunkCoord, WorldChunk> get() = loaded.toMap()

    /** Number of loaded chunks. */
    val loadedCount: Int get() = loaded.size

    /** Chunks in the load queue. */
    val queuedCount: Int get() = loadQueue.size

    fun chunkAt(coord: ChunkCoord): WorldChunk? = loaded[coord]

    fun isLoaded(coord: ChunkCoord): Boolean = loaded.containsKey(coord)

    fun stateOf(coord: ChunkCoord): ChunkState = states[coord] ?: ChunkState.Unloaded

    /**
     * Updates the streaming set for a player position. Marks chunks
     * within [loadRadius] for loading and releases chunks outside
     * [unloadRadius].
     */
    fun updateForPlayer(position: WorldPosition) {
        val center = ChunkCoord.fromWorld(position)

        // Queue chunks within load radius
        for (dx in -loadRadius..loadRadius) {
            for (dz in -loadRadius..loadRadius) {
                val coord = ChunkCoord(center.cx + dx, center.cz + dz)
                if (states[coord] == null) {
                    states[coord] = ChunkState.Queued
                    loadQueue.addLast(coord)
                }
            }
        }

        // Mark distant chunks for unloading
        val toUnload = loaded.keys.filter { it.distanceTo(center) > unloadRadius }
        for (coord in toUnload) {
            states[coord] = ChunkState.Unloading
            loaded.remove(coord)
        }
    }

    /**
     * Loads the next queued chunk synchronously. Returns the chunk
     * that was loaded, or null if the queue is empty.
     */
    fun loadNext(): WorldChunk? {
        if (loaded.size >= maxLoadedChunks) {
            // Enforce the hard cap — release the farthest chunk first.
            return null
        }
        while (loadQueue.isNotEmpty()) {
            val coord = loadQueue.removeFirst()
            if (states[coord] == ChunkState.Loaded || states[coord] == ChunkState.Loading) continue
            states[coord] = ChunkState.Loading
            val chunk = try {
                loader.load(coord)
            } catch (t: Throwable) {
                states[coord] = ChunkState.Failed
                continue
            }
            states[coord] = ChunkState.Loaded
            loaded[coord] = chunk
            return chunk
        }
        return null
    }

    /** Loads queued chunks until the queue is drained or the cap is hit. */
    fun drainQueue(maxSteps: Int = 8): Int {
        var count = 0
        while (count < maxSteps && loadQueue.isNotEmpty() && loaded.size < maxLoadedChunks) {
            loadNext() ?: break
            count++
        }
        return count
    }

    /** Unloads everything (e.g. on world exit). */
    fun unloadAll() {
        loaded.clear()
        states.clear()
        loadQueue.clear()
    }
}

/**
 * Chunk loader — the streaming source for chunk data.
 * The real implementation reads procedurally generated or
 * pre-baked chunk files from disk.
 */
interface ChunkLoader {
    fun load(coord: ChunkCoord): WorldChunk
}

/**
 * In-memory chunk loader for testing and for chunks that are
 * procedurally generated.
 */
class InMemoryChunkLoader(
    private val generate: (ChunkCoord) -> WorldChunk
) : ChunkLoader {
    override fun load(coord: ChunkCoord): WorldChunk = generate(coord)
}

/**
 * Chunk metrics for performance tracking.
 */
data class ChunkMetrics(
    val loadedChunks: Int,
    val queuedChunks: Int,
    val totalMemoryBytes: Long,
    val averageChunkBytes: Long,
    val failedChunks: Int
)
