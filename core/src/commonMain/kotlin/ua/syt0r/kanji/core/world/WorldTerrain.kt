package ua.syt0r.kanji.core.world

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.pow

// ============================================================
// WORLD — TERRAIN SYSTEM
// ------------------------------------------------------------
// Procedural terrain heightfield for the chunked world. The
// Kamakura slice blends real-world facts (coastline along the
// south, hills inland) with procedural noise for believable
// variation. All heights are meters above sea level.
// ============================================================

/**
 * Terrain height sampler — returns the ground height at (x, z).
 */
fun interface TerrainSampler {
    fun heightAt(x: Double, z: Double): Double
}

/**
 * A deterministic pseudo-noise function (value noise with
 * smooth interpolation). Deterministic = same seed always
 * produces the same world.
 */
class ValueNoise(private val seed: Long) {

    private fun hash(x: Int, z: Int): Double {
        var h = x * 374761393 + z * 668265263 + seed.toInt() * 1274126177
        h = (h xor (h shr 13)) * 1274126177
        h = h xor (h shr 16)
        return (h and 0x7fffffff).toDouble() / 0x7fffffff.toDouble()
    }

    private fun smooth(t: Double): Double = t * t * (3 - 2 * t)

    /** Sample noise at fractional (x, z) in 0..1. */
    fun noise(x: Double, z: Double): Double {
        val x0 = kotlin.math.floor(x).toInt()
        val z0 = kotlin.math.floor(z).toInt()
        val fx = smooth(x - x0)
        val fz = smooth(z - z0)
        val a = hash(x0, z0)
        val b = hash(x0 + 1, z0)
        val c = hash(x0, z0 + 1)
        val d = hash(x0 + 1, z0 + 1)
        return a * (1 - fx) * (1 - fz) + b * fx * (1 - fz) + c * (1 - fx) * fz + d * fx * fz
    }

    /** Fractal brownian motion — sum of octaves for natural-looking terrain. */
    fun fbm(x: Double, z: Double, octaves: Int = 5, lacunarity: Double = 2.0, gain: Double = 0.5): Double {
        var amplitude = 1.0
        var frequency = 1.0
        var sum = 0.0
        var norm = 0.0
        repeat(octaves) {
            sum += noise(x * frequency, z * frequency) * amplitude
            norm += amplitude
            amplitude *= gain
            frequency *= lacunarity
        }
        return sum / norm
    }
}

/**
 * Terrain generation configuration for a region.
 */
data class TerrainConfig(
    /** Base height in meters. */
    val baseHeight: Double = 0.0,
    /** Noise amplitude in meters. */
    val amplitude: Double = 60.0,
    /** Noise scale (smaller = larger features). */
    val scale: Double = 0.0008,
    /** Coastline distance from origin where ground meets sea level. */
    val coastlineOffset: Double = -500.0,
    /** Sea level in meters. */
    val seaLevel: Double = 0.0,
    /** Max hill height. */
    val maxHeight: Double = 120.0
)

/**
 * Terrain generator for a region. Produces a heightfield over
 * any world-space rectangle.
 */
class TerrainGenerator(
    private val config: TerrainConfig = TerrainConfig(),
    seed: Long = 42
) : TerrainSampler {

    private val noise = ValueNoise(seed)

    override fun heightAt(x: Double, z: Double): Double {
        // Distance from the coastline (south = negative z for Kamakura).
        // Sea level at coastline, rising inland.
        val coastDist = z - config.coastlineOffset

        // Coastal falloff: below sea level south of the beach.
        val coastal = if (coastDist < 0) {
            // Underwater — gentle slope down to the bay.
            config.seaLevel + coastDist * 0.02
        } else {
            // On land — rises from the beach.
            config.seaLevel + coastDist * 0.008
        }

        // Rolling hills inland.
        val hills = noise.fbm(x * config.scale, z * config.scale) * config.amplitude

        // Distance from center — the Kamakura basin is ringed by hills.
        val radial = kotlin.math.sqrt(x * x + z * z)
        val basin = (config.maxHeight * 0.5) * kotlin.math.exp(-(radial / 2500.0).pow(2))

        val height = coastal + hills * 0.4 + basin
        return height.coerceAtMost(config.maxHeight)
    }

    /**
     * Generates a heightfield for a chunk.
     */
    fun generateChunk(coord: ChunkCoord, resolution: Int = 8): List<List<Double>> {
        val origin = coord.worldOrigin
        return (0..resolution).map { zi ->
            (0..resolution).map { xi ->
                heightAt(
                    origin.x + xi * (CHUNK_SIZE / resolution),
                    origin.z + zi * (CHUNK_SIZE / resolution)
                )
            }
        }
    }
}

/**
 * Terrain system — owns the generator and provides lookup for
 * the player controller and chunk streaming.
 */
class TerrainSystem(
    private val generator: TerrainSampler
) : WorldSystem {

    /** Sample ground height at a world position. */
    fun heightAt(position: WorldPosition): Double = generator.heightAt(position.x, position.z)

    fun heightAt(x: Double, z: Double): Double = generator.heightAt(x, z)

    override suspend fun onStart(runtime: WorldRuntime) {}
    override suspend fun onStop(runtime: WorldRuntime) {}
    override suspend fun onUpdate(deltaSeconds: Double, runtime: WorldRuntime) {}
}
