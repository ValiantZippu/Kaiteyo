package ua.syt0r.kanji.desktop.game.world

import ua.syt0r.kanji.desktop.game.engine.geom.Rect
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2

/**
 * The whole game world: regions, locations, the travel network and the world
 * graph. Content is loaded from JSON and validated before use — see
 * [ua.syt0r.kanji.desktop.game.validation.ContentValidator].
 */
class GameWorld(
    val regions: List<Region>,
    val travel: TravelNetwork,
    val worldGraph: WorldGraph,
    val defaultSpawn: SpawnPoint
) {

    private val regionById: Map<String, Region> = regions.associateBy { it.id }
    private val cellIndex: Map<String, Cell> = buildMap {
        for (region in regions) {
            for (district in region.districts) {
                for (cell in district.cells) {
                    put(cell.id, cell)
                }
            }
        }
    }
    private val locationIndex: Map<String, Location> = buildMap {
        for (region in regions) {
            for (location in region.locations) {
                put(location.id, location)
            }
        }
    }
    private val stationIndex: Map<String, Station> = travel.stations.associateBy { it.id }

    fun region(id: String): Region? = regionById[id]

    fun cell(id: String): Cell? = cellIndex[id]

    fun location(id: String): Location? = locationIndex[id]

    fun station(id: String): Station? = stationIndex[id]

    fun allLocations(): List<Location> = locationIndex.values.sortedBy { it.id }

    /** All objects across the world (used by validation and photo tagging). */
    fun allObjects(): List<WorldObject> =
        cellIndex.values.flatMap { it.objects }

    /** World bounds of a region (used by the camera clamp). */
    fun regionBounds(regionId: String): Rect? = regionById[regionId]?.bounds?.toRect()

    fun stationAt(cellId: String): Station? = travel.stations.firstOrNull { it.cellId == cellId }

    /** The region that owns [cellId], or null when unknown. */
    fun regionIdForCell(cellId: String): String? =
        regionById.entries.firstOrNull { (_, region) ->
            region.districts.any { district -> district.cells.any { it.id == cellId } }
        }?.key

    /** All cell ids in a region (used to scope NPCs and streaming). */
    fun cellIdsIn(regionId: String): Set<String> =
        regionById[regionId]
            ?.districts?.flatMap { it.cells.map { cell -> cell.id } }
            ?.toSet()
            ?: emptySet()

    companion object {
        const val UNKNOWN_REGION = "unknown"
    }
}

/**
 * The world graph — a node graph over the world's content (spec §75): the
 * WORLD graph (station → train → Kamakura) is separate from the KNOWLEDGE
 * graph (駅 → 電車 → 鎌倉) but they cross-reference through node ids.
 */
class WorldGraph(
    nodes: List<GameNode>
) {
    private val nodeIndex: Map<String, GameNode> = nodes.associateBy { it.id }

    val nodes: List<GameNode> get() = nodeIndex.values.toList()

    fun node(id: String): GameNode? = nodeIndex[id]

    fun childrenOf(id: String): List<GameNode> =
        nodeIndex[id]?.children?.mapNotNull { nodeIndex[it] } ?: emptyList()

    fun prerequisitesOf(id: String): List<GameNode> =
        nodeIndex[id]?.prerequisites?.mapNotNull { nodeIndex[it] } ?: emptyList()

    fun roots(): List<GameNode> = nodeIndex.values.filter { node -> node.prerequisites.isEmpty() }

    /** Whether every prerequisite of [id] is satisfied. */
    fun arePrerequisitesMet(id: String, completed: Set<String>): Boolean =
        nodeIndex[id]?.prerequisites?.all { it in completed } ?: false

    /** Topological order (used by the quest tracker and node editor). */
    fun topologicalOrder(): List<GameNode> {
        val result = mutableListOf<GameNode>()
        val visited = mutableSetOf<String>()
        fun visit(id: String) {
            if (!visited.add(id)) return
            nodeIndex[id]?.prerequisites?.forEach { visit(it) }
            nodeIndex[id]?.let { result.add(it) }
        }
        nodeIndex.keys.sorted().forEach { visit(it) }
        return result
    }
}

/**
 * World streaming (spec §96): cells are the load unit. The streamer keeps a
 * residency set — the current region's cells within a radius of the player —
 * and unloads everything outside it, so a future multi-cell world streams
 * instead of staying resident. The debug overlay reports the loaded count;
 * NPC population is scoped to the residency set (see GameSession.enterRegion).
 */
class WorldStreamer(
    private val world: GameWorld,
    private val loadRadiusCells: Int = 2
) {
    private val loadedCells = mutableSetOf<String>()
    private var lastCellId: String? = null
    private var lastPosition: Vec2 = Vec2.Zero

    val loaded: Set<String> get() = loadedCells.toSet()

    /**
     * Update streaming around [position] in [cellId]. Returns cells that
     * became newly loaded (content hooks would instantiate them). Cells in
     * the player's region that are farther than the load radius are unloaded.
     */
    fun update(cellId: String, position: Vec2): List<Cell> {
        lastCellId = cellId
        lastPosition = position
        val target = residencySet(cellId)
        if (target.isEmpty()) return emptyList()
        val newlyLoaded = mutableListOf<Cell>()
        for (candidate in target) {
            if (loadedCells.add(candidate)) {
                world.cell(candidate)?.let { newlyLoaded.add(it) }
            }
        }
        // Evict cells outside the residency set (LRU-free: radius-based).
        loadedCells.retainAll(target)
        return newlyLoaded
    }

    /** Evict cells beyond the load radius of the last known position. */
    fun unloadDistant() {
        val cellId = lastCellId ?: return
        loadedCells.retainAll(residencySet(cellId))
    }

    /**
     * The cells that should be resident: the player's cell plus every cell in
     * the same region whose centre is within [loadRadiusCells] cell-widths.
     * Regions are independent coordinate spaces, so cross-region cells never
     * co-reside — leaving a region unloads it, arriving loads it (spec §96).
     */
    private fun residencySet(cellId: String): Set<String> {
        val regionId = world.regionIdForCell(cellId) ?: return emptySet()
        val region = world.region(regionId) ?: return emptySet()
        val anchor = world.cell(cellId)?.bounds?.toRect()?.center ?: return emptySet()
        val radius = loadRadiusCells.coerceAtLeast(1) * CELL_WIDTH_UNITS
        val result = linkedSetOf<String>()
        for (district in region.districts) {
            for (cell in district.cells) {
                val centre = cell.bounds.toRect().center
                val distance = anchor.distanceTo(centre)
                if (distance <= radius || cell.id == cellId) {
                    result.add(cell.id)
                }
            }
        }
        return result
    }

    companion object {
        /** One cell-width in world units (cells are square-ish in practice). */
        const val CELL_WIDTH_UNITS = 2208f
    }
}
