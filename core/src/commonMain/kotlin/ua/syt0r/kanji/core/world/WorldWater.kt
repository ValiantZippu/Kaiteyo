package ua.syt0r.kanji.core.world

import kotlin.math.sin

// ============================================================
// WORLD — WATER SYSTEM
// ------------------------------------------------------------
// Ocean water along the coast with simple animated waves and
// beach/coast detection. Water is a height field above which
// the player swims; wave amplitude is time-animated.
// ============================================================

/**
 * Water configuration.
 */
data class WaterConfig(
    /** Base water level in meters. */
    val baseLevel: Double = 0.0,
    /** Wave amplitude in meters. */
    val waveAmplitude: Double = 0.35,
    /** Wave frequency. */
    val waveFrequency: Double = 0.02,
    /** Wave speed. */
    val waveSpeed: Double = 1.2,
    /** Water clarity 0..1. */
    val clarity: Float = 0.8f,
    /** Water color (ARGB). */
    val colorArgb: Long = 0xFF1E88E5
)

/**
 * Water system — animates waves and answers water queries.
 */
class WaterSystem(
    private val config: WaterConfig = WaterConfig(),
    /** Coastline function: returns true when (x, z) is ocean. */
    private val isOcean: (Double, Double) -> Boolean = { _, _ -> false }
) : WorldSystem {

    private var timeSeconds = 0.0

    /** Water surface height at (x, z) at the current time. */
    fun surfaceHeightAt(x: Double, z: Double): Double? {
        if (!isOcean(x, z)) return null
        return config.baseLevel + waveOffset(x, z)
    }

    private fun waveOffset(x: Double, z: Double): Double {
        val t = timeSeconds * config.waveSpeed
        return sin(x * config.waveFrequency + t) * config.waveAmplitude +
                sin(z * config.waveFrequency * 1.3 - t * 0.7) * config.waveAmplitude * 0.5
    }

    /** Whether (x, z) is water. */
    fun isWater(x: Double, z: Double): Boolean = isOcean(x, z)

    /** Whether (x, z) is a beach (within [band] meters of the shoreline). */
    fun isBeach(x: Double, z: Double, band: Double = 30.0): Boolean {
        // Probe the ocean function in a small ring around the point.
        val probeRadius = 3.0
        val oceanCount = listOf(
            isOcean(x + probeRadius, z),
            isOcean(x - probeRadius, z),
            isOcean(x, z + probeRadius),
            isOcean(x, z - probeRadius)
        ).count { it }
        // Beach = partially ocean, partially land
        return oceanCount in 1..3
    }

    override suspend fun onStart(runtime: WorldRuntime) {
        timeSeconds = 0.0
    }

    override suspend fun onStop(runtime: WorldRuntime) {}

    override suspend fun onUpdate(deltaSeconds: Double, runtime: WorldRuntime) {
        timeSeconds += deltaSeconds
    }
}

/**
 * Ocean function for Kamakura: the bay is south (negative z) of
 * the coastline offset. Anything south of the coast line is ocean.
 */
class KamakuraOcean(
    private val coastlineOffset: Double = -500.0
) {
    fun isOcean(x: Double, z: Double): Boolean = z < coastlineOffset
}
