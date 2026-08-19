package ua.syt0r.kanji.core.world

import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ============================================================
// WORLD — GEOGRAPHIC COORDINATE SYSTEM
// ------------------------------------------------------------
// Kaiteyo World models real Japanese geography. Locations are
// stored in two coordinate spaces:
//
//   WorldPosition  — planar world-space coordinates (x, z in meters)
//   GeoCoordinate  — WGS84 latitude/longitude
//
// A local projection maps real Japan lat/lon onto the planar
// world space near the origin. The projection is deliberately
// simple (equirectangular around the loaded region) — good enough
// for a chunked game world, not for satellite navigation.
// ============================================================

/**
 * Planar world-space position in meters. +x = east, +z = south
 * (matching typical 3D engine convention where the camera looks
 * along -z). [y] is altitude above sea level.
 */
@Serializable
data class WorldPosition(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0
) {
    operator fun plus(other: WorldPosition) = WorldPosition(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: WorldPosition) = WorldPosition(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Double) = WorldPosition(x * scale, y * scale, z * scale)

    fun distanceTo(other: WorldPosition): Double {
        val dx = other.x - x
        val dy = other.y - y
        val dz = other.z - z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun horizontalDistanceTo(other: WorldPosition): Double {
        val dx = other.x - x
        val dz = other.z - z
        return sqrt(dx * dx + dz * dz)
    }

    fun length(): Double = sqrt(x * x + y * y + z * z)

    companion object {
        val Zero = WorldPosition(0.0, 0.0, 0.0)
    }
}

/**
 * WGS84 geographic coordinate (degrees).
 */
@Serializable
data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double
) {
    /** Distance in meters using the haversine formula. */
    fun distanceTo(other: GeoCoordinate): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLon = Math.toRadians(other.longitude - longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(latitude)) * cos(Math.toRadians(other.latitude)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * Math.atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}

/**
 * A region of the world — a named geographic area (e.g. "Kamakura").
 */
@Serializable
data class WorldRegion(
    val id: String,
    val name: String,
    /** Japanese name. */
    val japaneseName: String,
    val center: GeoCoordinate,
    /** Region radius in meters. */
    val radiusMeters: Double,
    /** Region type. */
    val type: WorldRegionType,
    /** Child regions (cities within a prefecture, districts within a city). */
    val children: List<WorldRegion> = emptyList(),
    /** Description for the region page. */
    val description: String? = null
)

/**
 * Region hierarchy types.
 */
enum class WorldRegionType(val label: String) {
    Country("Country"),
    Prefecture("Prefecture"),
    City("City"),
    District("District"),
    Location("Location"),
    Building("Building"),
    Interior("Interior")
}

/**
 * A named location inside a region (e.g. "Tsuruoka Hachimangu", "Kamakura Station").
 */
@Serializable
data class WorldLocation(
    val id: String,
    val name: String,
    val japaneseName: String,
    val regionId: String,
    val coordinate: GeoCoordinate,
    val position: WorldPosition,
    val type: WorldLocationType,
    val description: String? = null,
    /** Opening hours (for activities). */
    val openingHours: String? = null
)

/**
 * Location types.
 */
enum class WorldLocationType(val label: String) {
    Temple("Temple"),
    Shrine("Shrine"),
    Station("Station"),
    Shop("Shop"),
    Restaurant("Restaurant"),
    Museum("Museum"),
    Park("Park"),
    Beach("Beach"),
    Coast("Coast"),
    School("School"),
    Landmark("Landmark"),
    Cafe("Cafe"),
    Street("Street"),
    Bridge("Bridge"),
    Nature("Nature"),
    Interior("Interior"),
    Other("Other")
}

/**
 * Local projection — maps WGS84 coordinates onto planar world space
 * near an origin. Equirectangular: 1 degree of latitude ≈ 111,320 m,
 * 1 degree of longitude scaled by cos(latitude).
 */
class WorldProjection(
    val originLatitude: Double,
    val originLongitude: Double
) {

    companion object {
        const val METERS_PER_DEGREE_LAT = 111_320.0

        /** The world's default origin — central Kamakura. */
        val KAMAKURA_ORIGIN = GeoCoordinate(35.3192, 139.5467)
    }

    fun toWorld(geo: GeoCoordinate): WorldPosition {
        val dLat = geo.latitude - originLatitude
        val dLon = geo.longitude - originLongitude
        val latScale = METERS_PER_DEGREE_LAT
        val lonScale = METERS_PER_DEGREE_LAT * cos(Math.toRadians(originLatitude))
        return WorldPosition(
            x = dLon * lonScale,
            y = 0.0,
            z = dLat * latScale
        )
    }

    fun toGeo(world: WorldPosition): GeoCoordinate {
        val latScale = METERS_PER_DEGREE_LAT
        val lonScale = METERS_PER_DEGREE_LAT * cos(Math.toRadians(originLatitude))
        return GeoCoordinate(
            latitude = originLatitude + world.z / latScale,
            longitude = originLongitude + world.x / lonScale
        )
    }
}

/**
 * Well-known real locations in Kamakura for the vertical slice.
 * Coordinates are approximate public-domain facts (not proprietary data).
 */
object KamakuraLocations {
    val PROJECTION = WorldProjection(
        WorldProjection.KAMAKURA_ORIGIN.latitude,
        WorldProjection.KAMAKURA_ORIGIN.longitude
    )

    val KAMAKURA = WorldRegion(
        id = "kamakura",
        name = "Kamakura",
        japaneseName = "鎌倉",
        center = GeoCoordinate(35.3192, 139.5467),
        radiusMeters = 6000.0,
        type = WorldRegionType.City,
        description = "A coastal city in Kanagawa Prefecture, former capital of Japan (1185–1333)."
    )

    val TSUROKA_HACHIMANGU = WorldLocation(
        id = "tsurugaoka-hachimangu",
        name = "Tsurugaoka Hachimangu",
        japaneseName = "鶴岡八幡宮",
        regionId = "kamakura",
        coordinate = GeoCoordinate(35.3264, 139.5564),
        position = PROJECTION.toWorld(GeoCoordinate(35.3264, 139.5564)),
        type = WorldLocationType.Shrine,
        description = "The most important Shinto shrine in Kamakura, founded in 1063."
    )

    val KOTOKU_IN = WorldLocation(
        id = "kotoku-in",
        name = "Kotoku-in",
        japaneseName = "高徳院",
        regionId = "kamakura",
        coordinate = GeoCoordinate(35.3167, 139.5356),
        position = PROJECTION.toWorld(GeoCoordinate(35.3167, 139.5356)),
        type = WorldLocationType.Temple,
        description = "Home of the Great Buddha (Daibutsu), a 13.35-meter bronze statue."
    )

    val KAMAKURA_STATION = WorldLocation(
        id = "kamakura-station",
        name = "Kamakura Station",
        japaneseName = "鎌倉駅",
        regionId = "kamakura",
        coordinate = GeoCoordinate(35.3192, 139.5502),
        position = PROJECTION.toWorld(GeoCoordinate(35.3192, 139.5502)),
        type = WorldLocationType.Station,
        description = "JR East station on the Yokosuka Line and the Enoden Line."
    )

    val YUIGAHAMA_BEACH = WorldLocation(
        id = "yuigahama-beach",
        name = "Yuigahama Beach",
        japaneseName = "由比ヶ浜",
        regionId = "kamakura",
        coordinate = GeoCoordinate(35.3127, 139.5413),
        position = PROJECTION.toWorld(GeoCoordinate(35.3127, 139.5413)),
        type = WorldLocationType.Beach,
        description = "A popular beach along Sagami Bay."
    )

    val HASE_DERA = WorldLocation(
        id = "hase-dera",
        name = "Hase-dera",
        japaneseName = "長谷寺",
        regionId = "kamakura",
        coordinate = GeoCoordinate(35.3122, 139.5341),
        position = PROJECTION.toWorld(GeoCoordinate(35.3122, 139.5341)),
        type = WorldLocationType.Temple,
        description = "A temple of the Jodo sect known for its Kannon statue and hydrangeas."
    )

    val ENODEN_STATION = WorldLocation(
        id = "enoden-kamakura",
        name = "Enoden Kamakura Station",
        japaneseName = "江ノ電鎌倉駅",
        regionId = "kamakura",
        coordinate = GeoCoordinate(35.3192, 139.5499),
        position = PROJECTION.toWorld(GeoCoordinate(35.3192, 139.5499)),
        type = WorldLocationType.Station,
        description = "Terminal station of the Enoshima Electric Railway (Enoden)."
    )

    val all: List<WorldLocation> = listOf(
        TSUROKA_HACHIMANGU,
        KOTOKU_IN,
        KAMAKURA_STATION,
        YUIGAHAMA_BEACH,
        HASE_DERA,
        ENODEN_STATION
    )
}
