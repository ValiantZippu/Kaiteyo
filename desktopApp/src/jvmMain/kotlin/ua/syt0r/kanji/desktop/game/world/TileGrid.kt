package ua.syt0r.kanji.desktop.game.world

import ua.syt0r.kanji.desktop.game.engine.geom.Rect
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2
import kotlin.math.floor

/**
 * Resolves the character legend of a [Cell] into a collision grid. Solidity
 * comes from the tile definitions; trees/fences/buildings mark their tiles
 * solid in the content files.
 */
class TileGrid(
    val cell: Cell,
    val tileSize: Float = 48f
) {
    private val rows: List<String> = cell.tiles
    private val legend: Map<Char, TileDef> = cell.legend.entries
        .mapNotNull { (key, def) -> key.firstOrNull()?.let { it to def } }
        .toMap()
    private val solidCache = mutableMapOf<Char, Boolean>()

    val width: Int get() = rows.maxOfOrNull { it.length } ?: 0
    val height: Int get() = rows.size

    val worldWidth: Float get() = width * tileSize
    val worldHeight: Float get() = height * tileSize

    fun tileAt(tx: Int, ty: Int): Char {
        val row = rows.getOrNull(ty) ?: return '.'
        return row.getOrNull(tx) ?: '.'
    }

    fun isSolidAt(tx: Int, ty: Int): Boolean {
        if (tx < 0 || ty < 0 || tx >= width || ty >= height) return true
        val ch = tileAt(tx, ty)
        return solidCache.getOrPut(ch) { legend[ch]?.solid ?: false }
    }

    /** The tile's ground colour, resolved from the legend. */
    fun tileColor(tx: Int, ty: Int): Int? = legend[tileAt(tx, ty)]?.color

    fun tileDef(tx: Int, ty: Int): TileDef? = legend[tileAt(tx, ty)]

    fun isAnimatedAt(tx: Int, ty: Int): Boolean = legend[tileAt(tx, ty)]?.animated == true

    /** World rect of one tile. */
    fun tileRect(tx: Int, ty: Int): Rect =
        Rect(tx * tileSize, ty * tileSize, tileSize, tileSize)

    /**
     * Axis-separated collision resolution for a moving rect. Returns the
     * corrected position after sliding against solid tiles.
     */
    fun resolve(
        position: Vec2,
        size: Vec2,
        velocity: Vec2,
        dt: Float
    ): Vec2 {
        var result = position
        // Horizontal axis.
        val nx = position.x + velocity.x * dt
        result = result.withX(moveAxis(nx, size.x, size.y, result.y))
        // Vertical axis.
        val ny = position.y + velocity.y * dt
        result = result.withY(moveAxis(result.x, size.y, size.x, ny))
        return result
    }

    /** Slide one axis: check the tiles the rect would occupy and clamp. */
    private fun moveAxis(x: Float, halfW: Float, halfH: Float, y: Float): Float {
        val left = floor((x - halfW) / tileSize).toInt()
        val right = floor((x + halfW - 0.01f) / tileSize).toInt()
        val top = floor((y - halfH) / tileSize).toInt()
        val bottom = floor((y + halfH - 0.01f) / tileSize).toInt()
        for (ty in top..bottom) {
            for (tx in left..right) {
                if (isSolidAt(tx, ty)) {
                    // Push out horizontally.
                    val tileCenter = (tx + 0.5f) * tileSize
                    return if (x < tileCenter) {
                        (tx * tileSize) + halfW
                    } else {
                        (tx + 1) * tileSize - halfW
                    }
                }
            }
        }
        return x
    }

    /** Whether a rect overlaps solid ground (spawn/teleport safety). */
    fun isFree(rect: Rect): Boolean {
        val left = floor(rect.x / tileSize).toInt()
        val right = floor(rect.right / tileSize).toInt()
        val top = floor(rect.y / tileSize).toInt()
        val bottom = floor(rect.bottom / tileSize).toInt()
        for (ty in top..bottom) {
            for (tx in left..right) {
                if (isSolidAt(tx, ty)) return false
            }
        }
        return true
    }
}
