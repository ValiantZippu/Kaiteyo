package ua.syt0r.kanji.core.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.math.abs
import kotlin.test.Test

class WorldGeoTest {

    // ------------------------------------------------------------------
    // WorldPosition
    // ------------------------------------------------------------------

    @Test
    fun addition() {
        val a = WorldPosition(1.0, 2.0, 3.0)
        val b = WorldPosition(4.0, 5.0, 6.0)
        assertEquals(WorldPosition(5.0, 7.0, 9.0), a + b)
    }

    @Test
    fun subtraction() {
        val a = WorldPosition(4.0, 5.0, 6.0)
        val b = WorldPosition(1.0, 2.0, 3.0)
        assertEquals(WorldPosition(3.0, 3.0, 3.0), a - b)
    }

    @Test
    fun scaling() {
        val a = WorldPosition(2.0, 3.0, 4.0)
        assertEquals(WorldPosition(4.0, 6.0, 8.0), a * 2.0)
    }

    @Test
    fun distanceIsZeroForSamePoint() {
        assertEquals(0.0, WorldPosition(1.0, 2.0, 3.0).distanceTo(WorldPosition(1.0, 2.0, 3.0)), 0.001)
    }

    @Test
    fun distanceIsPythagorean() {
        // 3-4-5 triangle in the horizontal plane
        val a = WorldPosition(0.0, 0.0, 0.0)
        val b = WorldPosition(3.0, 0.0, 4.0)
        assertEquals(5.0, a.distanceTo(b), 0.001)
    }

    @Test
    fun horizontalDistanceIgnoresY() {
        val a = WorldPosition(0.0, 10.0, 0.0)
        val b = WorldPosition(3.0, 100.0, 4.0)
        assertEquals(5.0, a.horizontalDistanceTo(b), 0.001)
    }

    @Test
    fun length() {
        assertEquals(5.0, WorldPosition(3.0, 0.0, 4.0).length(), 0.001)
    }

    // ------------------------------------------------------------------
    // GeoCoordinate
    // ------------------------------------------------------------------

    @Test
    fun haversineDistanceIsApproximatelyCorrect() {
        // Roughly 100 km along a meridian ≈ 0.9 degrees of latitude.
        val a = GeoCoordinate(35.0, 139.0)
        val b = GeoCoordinate(35.9, 139.0)
        val dist = a.distanceTo(b)
        // ~100 km, allow generous tolerance
        assertTrue(dist in 95_000.0..105_000.0)
    }

    @Test
    fun sameCoordinateDistanceIsZero() {
        val a = GeoCoordinate(35.0, 139.0)
        assertEquals(0.0, a.distanceTo(GeoCoordinate(35.0, 139.0)), 0.001)
    }

    // ------------------------------------------------------------------
    // Projection round-trip
    // ------------------------------------------------------------------

    @Test
    fun projectionRoundTrips() {
        val projection = WorldProjection(35.0, 139.0)
        val geo = GeoCoordinate(35.5, 139.5)
        val world = projection.toWorld(geo)
        val back = projection.toGeo(world)
        assertEquals(geo.latitude, back.latitude, 0.00001)
        assertEquals(geo.longitude, back.longitude, 0.00001)
    }

    @Test
    fun originMapsToZero() {
        val projection = WorldProjection(35.0, 139.0)
        val world = projection.toWorld(GeoCoordinate(35.0, 139.0))
        assertEquals(0.0, world.x, 0.001)
        assertEquals(0.0, world.z, 0.001)
    }

    @Test
    fun latitudeIncreasesZ() {
        val projection = WorldProjection(35.0, 139.0)
        val north = projection.toWorld(GeoCoordinate(35.1, 139.0))
        // +0.1° lat ≈ 11,132 m south in +z
        assertTrue(north.z > 11_000.0)
        assertEquals(0.0, north.x, 0.001)
    }

    @Test
    fun longitudeIncreasesX() {
        val projection = WorldProjection(35.0, 139.0)
        val east = projection.toWorld(GeoCoordinate(35.0, 139.1))
        // +0.1° lon ≈ 9,100 m east in +x (scaled by cos 35°)
        assertTrue(east.x > 8_000.0)
        assertEquals(0.0, east.z, 0.001)
    }

    // ------------------------------------------------------------------
    // Kamakura locations
    // ------------------------------------------------------------------

    @Test
    fun kamakuraLocationsAreInWorldSpace() {
        val station = KamakuraLocations.KAMAKURA_STATION.position
        val shrine = KamakuraLocations.TSUROKA_HACHIMANGU.position
        // They should be within a few km of the origin.
        assertTrue(station.length() < 2000.0)
        assertTrue(shrine.length() < 2000.0)
    }

    @Test
    fun kamakuraBeachIsSouth() {
        val beach = KamakuraLocations.YUIGAHAMA_BEACH.position
        assertTrue(beach.z > 0.0) // south = positive z
    }

    @Test
    fun kamakuraRegionHasChildrenOrDescription() {
        assertTrue(KamakuraLocations.KAMAKURA.description != null)
    }

    @Test
    fun allKamakuraLocationsRegistered() {
        assertTrue(KamakuraLocations.all.size >= 5)
    }
}
