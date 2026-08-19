package ua.syt0r.kanji.core.world

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ============================================================
// WORLD — SAVE/LOAD & WORLD MAP
// ------------------------------------------------------------
// A save file captures the player's state, discovered locations,
// unlocked quests, and world settings. Saves are JSON — the same
// format on every platform. The world map tracks which regions
// and locations the player has discovered.
// ============================================================

/**
 * A save slot's metadata.
 */
@Serializable
data class WorldSaveMetadata(
    val slotId: String,
    val saveTime: Long,
    val playTimeSeconds: Double,
    val playerRegionId: String?,
    val playerName: String = "Learner",
    val discoveredLocations: Int = 0,
    val level: Int = 1
)

/**
 * The full save data.
 */
@Serializable
data class WorldSaveData(
    val version: Int = 1,
    val metadata: WorldSaveMetadata,
    val player: SavedPlayerState = SavedPlayerState(),
    val discoveredRegions: Set<String> = emptySet(),
    val discoveredLocations: Set<String> = emptySet(),
    val completedQuests: Set<String> = emptySet(),
    val activeQuests: Set<String> = emptySet(),
    val worldSettings: WorldSettings = WorldSettings(),
    val stats: WorldStats = WorldStats()
)

/**
 * Saved player state.
 */
@Serializable
data class SavedPlayerState(
    val position: WorldPosition = WorldPosition.Zero,
    val facingYawDegrees: Float = 0f,
    val cameraMode: String = "ThirdPerson",
    val currentRegionId: String? = null,
    val currentLocationId: String? = null,
    val playTimeSeconds: Double = 0.0,
    val distanceWalkedMeters: Double = 0.0
)

/**
 * World settings persisted across sessions.
 */
@Serializable
data class WorldSettings(
    val timeScale: Float = 1.0f,
    val weatherEnabled: Boolean = true,
    val npcEnabled: Boolean = true,
    val vehicleDensity: Int = 3,
    val showNameplates: Boolean = true,
    val showCompass: Boolean = true,
    val dayNightCycle: Boolean = true
)

/**
 * Aggregate world stats.
 */
@Serializable
data class WorldStats(
    val npcsMet: Int = 0,
    val dialoguesHeard: Int = 0,
    val questsCompleted: Int = 0,
    val locationsVisited: Int = 0,
    val kmWalked: Double = 0.0,
    val trainsRidden: Int = 0,
    val itemsCollected: Int = 0
)

/**
 * Save/load service — persists and restores world saves.
 * The [storage] function abstracts the platform file system.
 */
class WorldSaveService(
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val storage: (String) -> String? = { null },
    private val write: (String, String) -> Unit = { _, _ -> }
) {

    /**
     * Saves world state. Returns the slot id.
     */
    fun save(
        slotId: String,
        player: SavedPlayerState,
        discoveredRegions: Set<String>,
        discoveredLocations: Set<String>,
        completedQuests: Set<String>,
        activeQuests: Set<String>,
        settings: WorldSettings,
        stats: WorldStats
    ): WorldSaveMetadata {
        val data = WorldSaveData(
            metadata = WorldSaveMetadata(
                slotId = slotId,
                saveTime = System.currentTimeMillis(),
                playTimeSeconds = player.playTimeSeconds,
                playerRegionId = player.currentRegionId,
                discoveredLocations = discoveredLocations.size
            ),
            player = player,
            discoveredRegions = discoveredRegions,
            discoveredLocations = discoveredLocations,
            completedQuests = completedQuests,
            activeQuests = activeQuests,
            worldSettings = settings,
            stats = stats
        )
        write("world_save_$slotId.json", json.encodeToString(WorldSaveData.serializer(), data))
        return data.metadata
    }

    /**
     * Loads a save slot. Returns null if the slot doesn't exist.
     */
    fun load(slotId: String): WorldSaveData? {
        val raw = storage("world_save_$slotId.json") ?: return null
        return runCatching {
            json.decodeFromString(WorldSaveData.serializer(), raw)
        }.getOrNull()
    }

    /** Lists all save slots. */
    fun listSlots(): List<WorldSaveMetadata> {
        // The storage function is keyed per slot; enumerate a fixed range.
        return (1..6).mapNotNull { load("slot$it")?.metadata }
    }
}

// ============================================================
// WORLD MAP
// ============================================================

/**
 * The world map — tracks regions and locations.
 */
class WorldMap {

    private val regions = mutableMapOf<String, WorldRegion>()
    private val locations = mutableMapOf<String, WorldLocation>()
    private val discoveredRegions = mutableSetOf<String>()
    private val discoveredLocations = mutableSetOf<String>()

    fun registerRegion(region: WorldRegion) {
        regions[region.id] = region
        region.children.forEach { registerRegion(it) }
    }

    fun registerLocation(location: WorldLocation) {
        locations[location.id] = location
    }

    fun region(id: String): WorldRegion? = regions[id]

    fun location(id: String): WorldLocation? = locations[id]

    fun allRegions(): List<WorldRegion> = regions.values.toList()

    fun allLocations(): List<WorldLocation> = locations.values.toList()

    fun locationsInRegion(regionId: String): List<WorldLocation> =
        locations.values.filter { it.regionId == regionId }

    /** Marks a region as discovered. */
    fun discoverRegion(id: String) {
        discoveredRegions.add(id)
    }

    /** Marks a location as discovered. */
    fun discoverLocation(id: String) {
        discoveredLocations.add(id)
    }

    fun isRegionDiscovered(id: String): Boolean = id in discoveredRegions

    fun isLocationDiscovered(id: String): Boolean = id in discoveredLocations

    val discoveredRegionCount: Int get() = discoveredRegions.size

    val discoveredLocationCount: Int get() = discoveredLocations.size

    /** Restores discovery state from a save. */
    fun restore(regions: Set<String>, locations: Set<String>) {
        discoveredRegions.clear()
        discoveredRegions.addAll(regions)
        discoveredLocations.clear()
        discoveredLocations.addAll(locations)
    }

    /** Locations sorted by distance from a world position. */
    fun nearestLocations(position: WorldPosition, limit: Int = 5): List<WorldLocation> =
        locations.values
            .sortedBy { it.position.horizontalDistanceTo(position) }
            .take(limit)
}
