package ua.syt0r.kanji.desktop.game.engine

import ua.syt0r.kanji.desktop.game.engine.geom.Rect
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2
import kotlin.math.floor

/**
 * Uniform-grid spatial hash used for broad-phase queries (interaction radius,
 * photo framing, nearby NPCs). Entities register their id + bounds each frame
 * (cheap for a small world) and queries return candidate ids to narrow-check.
 *
 * This is the "world streaming" seed: cells are bucketed the same way a
 * future streaming system buckets districts — see [world.WorldStreamer].
 */
class SpatialHash(cellSize: Float = 96f) {

    private val cellSize = cellSize
    private val buckets = mutableMapOf<Long, MutableList<SpatialEntry>>()

    data class SpatialEntry(val id: String, val rect: Rect)

    fun clear() {
        buckets.clear()
    }

    private fun key(cx: Int, cy: Int): Long =
        (cx.toLong() shl 32) xor (cy.toLong() and 0xFFFFFFFFL)

    private fun cellOf(value: Float): Int = floor(value / cellSize).toInt()

    fun insert(id: String, rect: Rect) {
        val minX = cellOf(rect.x)
        val maxX = cellOf(rect.right)
        val minY = cellOf(rect.y)
        val maxY = cellOf(rect.bottom)
        for (cx in minX..maxX) {
            for (cy in minY..maxY) {
                buckets.getOrPut(key(cx, cy)) { mutableListOf() }.add(SpatialEntry(id, rect))
            }
        }
    }

    /**
     * Return entries whose rect overlaps [region]. Duplicate ids (an entry
     * spanning cells appears in several buckets) are de-duplicated.
     */
    fun query(region: Rect): List<SpatialEntry> {
        val minX = cellOf(region.x)
        val maxX = cellOf(region.right)
        val minY = cellOf(region.y)
        val maxY = cellOf(region.bottom)
        val seen = mutableSetOf<String>()
        val result = mutableListOf<SpatialEntry>()
        for (cx in minX..maxX) {
            for (cy in minY..maxY) {
                val bucket = buckets[key(cx, cy)] ?: continue
                for (entry in bucket) {
                    if (seen.add(entry.id) && entry.rect.overlaps(region)) {
                        result.add(entry)
                    }
                }
            }
        }
        return result
    }

    /** Nearest entry to [point] whose rect overlaps a circle of [radius]. */
    fun nearest(point: Vec2, radius: Float): SpatialEntry? {
        val region = Rect(point.x - radius, point.y - radius, radius * 2f, radius * 2f)
        return query(region).minByOrNull { entry -> entry.rect.center.distanceTo(point) }
    }
}
