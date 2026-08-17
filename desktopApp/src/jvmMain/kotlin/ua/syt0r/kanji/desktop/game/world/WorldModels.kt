package ua.syt0r.kanji.desktop.game.world

import kotlinx.serialization.Serializable
import ua.syt0r.kanji.desktop.game.engine.geom.Rect
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2

// ============================================================
// WORLD DATA STRUCTURES
// JAPAN → REGION → DISTRICT → CELL → LOCATION/OBJECT/ACTIVITY
//
// Everything below is @Serializable and loaded from JSON content
// files (see `content/`), so new regions, quests and vocabulary
// can be authored without touching the engine (spec §78).
// ============================================================

/** A point in world space (world units, zoom 1). */
@Serializable
data class WorldPoint(val x: Float, val y: Float) {
    fun toVec2(): Vec2 = Vec2(x, y)

    companion object {
        fun fromVec2(v: Vec2): WorldPoint = WorldPoint(v.x, v.y)
    }
}

@Serializable
data class WorldRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    fun toRect(): Rect = Rect(x, y, width, height)

    companion object {
        fun fromRect(r: Rect): WorldRect = WorldRect(r.x, r.y, r.width, r.height)
    }
}

/**
 * The reusable node model (spec §76). Both the world graph and the quest
 * graph are built from these: a node has prerequisites (ids of nodes that
 * must be complete/visited first), children, an optional location, learning
 * targets (knowledge node ids) and rewards (node ids granted on completion).
 */
@Serializable
data class GameNode(
    val id: String,
    val type: GameNodeType,
    val title: String,
    val description: String = "",
    val prerequisites: List<String> = emptyList(),
    val children: List<String> = emptyList(),
    val locationId: String? = null,
    val learningTargets: List<String> = emptyList(),
    val rewards: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
enum class GameNodeType {
    WORLD, LOCATION, QUEST, STORY, NPC, ACTIVITY, WORD, KANJI, GRAMMAR,
    SENTENCE, COLLECTION, REWARD, TRAVEL, CHECKPOINT
}

// ------------------------------------------------------------
// Region hierarchy
// ------------------------------------------------------------

@Serializable
data class Region(
    val id: String,
    val name: String,
    val nameJp: String,
    val description: String = "",
    val theme: String = "summer",
    val districts: List<District> = emptyList(),
    val locations: List<Location> = emptyList(),
    val spawn: SpawnPoint,
    val bounds: WorldRect
)

@Serializable
data class District(
    val id: String,
    val name: String,
    val nameJp: String = "",
    val cells: List<Cell> = emptyList()
)

/**
 * A cell is the streaming unit (spec §96): the world is divided into cells,
 * nearby cells are loaded, distant cells unload. The slice ships one cell
 * per district; the architecture supports many.
 */
@Serializable
data class Cell(
    val id: String,
    val name: String,
    val nameJp: String = "",
    /** Tile rows; each character resolves through [legend]. */
    val tiles: List<String> = emptyList(),
    /** Character → tile definition (colour + solidity + animation). */
    val legend: Map<String, TileDef> = emptyMap(),
    val objects: List<WorldObject> = emptyList(),
    val bounds: WorldRect
)

@Serializable
data class SpawnPoint(
    val regionId: String,
    val cellId: String,
    val position: WorldPoint,
    val facing: String = "Down"
)

// ------------------------------------------------------------
// World objects — the interactable stuff of the world
// ------------------------------------------------------------

@Serializable
enum class ObjectKind {
    Sign, VendingMachine, Bench, Door, Shop, Station, House, Tree, Fence,
    Mailbox, Boat, Lighthouse, Shrine, Well, Lantern, Bicycle, Cat, Bird,
    BeachTowel, CameraSpot, NoticeBoard, BusStop, PhoneBooth, Stalls
}

/**
 * A physical object in the world. Japanese appears *in* the object itself
 * ([label] is the raw Japanese text on the object — 看板/メニュー/入口…),
 * which is how the environment teaches before any quiz does (spec §9).
 */
@Serializable
data class WorldObject(
    val id: String,
    val kind: ObjectKind,
    val position: WorldPoint,
    val size: WorldPoint = WorldPoint(48f, 48f),
    val solid: Boolean = true,
    val label: String = "",
    val reading: String = "",
    val meaning: String = "",
    /** Id of the interactable behaviour (see interaction/). */
    val interactableId: String? = null,
    /** Knowledge node ids this object teaches when inspected. */
    val learningTargets: List<String> = emptyList(),
    /**
     * Kid-mode layer (spec §7, §68): simpler vocabulary for the same
     * object. When kid mode is on and this list is non-empty, it replaces
     * [learningTargets] — the same timetable teaches 電車 to a kid and
     * 時刻表 to everyone else. Empty = fall back to [learningTargets].
     * Validated against the knowledge graph at load time.
     */
    val kidTargets: List<String> = emptyList(),
    /** Optional visual accent (roof color, sign color) as 0xRRGGBB. */
    val accent: Int? = null,
    /**
     * World-clock phases this object is alive in (spec §40): "Morning",
     * "Day", "Evening", "Night". Empty = always open. A festival stall set
     * to ["Evening"] is closed at noon and comes alive at dusk.
     */
    val availablePhases: List<String> = emptyList(),
    /**
     * Seasons this object is alive in (spec §42): "Spring", "Summer",
     * "Autumn", "Winter". Empty = every season. A winter stall only appears
     * when the world turns cold.
     */
    val availableSeasons: List<String> = emptyList(),
    /**
     * Weather this object is open in (spec §41): "Sun", "Cloud", "Rain",
     * "Snow". Empty = any weather.
     */
    val availableWeather: List<String> = emptyList()
)

/** A named, discoverable place (station, beach, park, shop…). */
@Serializable
data class Location(
    val id: String,
    val name: String,
    val nameJp: String = "",
    val cellId: String,
    val anchor: WorldPoint,
    val radius: Float = 64f,
    val kind: LocationKind = LocationKind.Town,
    val description: String = "",
    val learningTargets: List<String> = emptyList(),
    /** Quest/story ids that unlock after discovering this location. */
    val unlocks: List<String> = emptyList()
)

@Serializable
enum class LocationKind {
    Town, Station, Beach, Park, Shop, Temple, School, Port, Mountain, Landmark
}

// ------------------------------------------------------------
// Tiles
// ------------------------------------------------------------

/** Visual + collision properties of one tile character. */
@Serializable
data class TileDef(
    /** 0xRRGGBB ground colour. */
    val color: Int,
    val solid: Boolean = false,
    /** Slight vertical bob so water/grass read as distinct materials. */
    val animated: Boolean = false,
    val depth: Int = 0
)

// ------------------------------------------------------------
// Travel network (spec §47-48)
// ------------------------------------------------------------

@Serializable
data class Station(
    val id: String,
    val name: String,
    val nameJp: String,
    val reading: String = "",
    val cellId: String,
    val position: WorldPoint,
    val lines: List<String> = emptyList(),
    /** Knowledge nodes (駅/切符/改札/ホーム…) taught by this station. */
    val learningTargets: List<String> = emptyList(),
    /** World-clock minutes the ride takes (arrival clock advance). */
    val arrivalDelayMinutes: Int = 0,
    /** Story auto-started on arrival (guided momentum, spec §54). */
    val arrivalStoryId: String? = null
)

@Serializable
data class TrainLine(
    val id: String,
    val name: String,
    val color: Int,
    val stations: List<String> = emptyList()
)

@Serializable
data class TravelEdge(
    val fromStationId: String,
    val toStationId: String,
    val lineId: String,
    /** Duration in world-time minutes (train schedules come later). */
    val minutes: Int = 5
)

@Serializable
data class TravelNetwork(
    val stations: List<Station> = emptyList(),
    val lines: List<TrainLine> = emptyList(),
    val edges: List<TravelEdge> = emptyList()
)
