package ua.syt0r.kanji.desktop.game.player

import kotlinx.serialization.Serializable
import ua.syt0r.kanji.desktop.game.engine.Direction
import ua.syt0r.kanji.desktop.game.engine.Entity
import ua.syt0r.kanji.desktop.game.engine.geom.Rect
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2
import ua.syt0r.kanji.desktop.game.engine.input.InputManager
import ua.syt0r.kanji.desktop.game.engine.input.InputAction
import ua.syt0r.kanji.desktop.game.world.TileGrid
import ua.syt0r.kanji.desktop.game.world.WorldPoint
import kotlin.math.abs

// ============================================================
// PLAYER (spec §11, §17)
// A real character with walk/run/sit/interact states, smooth
// acceleration/deceleration, and an inventory/cosmetics layer.
// ============================================================

/** Persisted character state (saved with the game). */
@Serializable
data class PlayerState(
    val level: Int = 1,
    val xp: Int = 0,
    val regionId: String = "hamanaka",
    val cellId: String = "hamanaka-town",
    val position: WorldPoint = WorldPoint(0f, 0f),
    val facing: String = "Down",
    val cosmetics: List<String> = emptyList(),
    val inventory: List<ItemStack> = emptyList()
) {
    fun addItem(itemId: String, count: Int = 1): PlayerState {
        val existing = inventory.firstOrNull { it.itemId == itemId }
        val updated = if (existing != null) {
            inventory.map {
                if (it.itemId == itemId) it.copy(count = it.count + count) else it
            }
        } else {
            inventory + ItemStack(itemId, count)
        }
        return copy(inventory = updated)
    }

    fun addXp(amount: Int): PlayerState {
        var newLevel = level
        var newXp = xp + amount
        // Simple 100xp * level curve — light RPG, no grind (spec §4, §87).
        while (newXp >= xpForNext(newLevel)) {
            newXp -= xpForNext(newLevel)
            newLevel++
        }
        return copy(level = newLevel, xp = newXp)
    }

    companion object {
        fun xpForNext(level: Int): Int = 100 + (level - 1) * 60
    }
}

@Serializable
data class ItemStack(
    val itemId: String,
    val count: Int = 1
)

/** Item catalogue — all content-driven (JSON additions need no code). */
@Serializable
enum class ItemId(val label: String, val labelJp: String) {
    DrinkWater("Water", "水"),
    DrinkTea("Green tea", "お茶"),
    DrinkJuice("Juice", "ジュース"),
    Postcard("Postcard", "絵葉書"),
    Stamp("Town stamp", "スタンプ"),
    Photo("Photo", "写真")
}

/**
 * Movement controller: smooth acceleration and deceleration (no sliding,
 * no robotic direction snaps — spec §17), tile collision, running speed and
 * facing. Input arrives as [InputManager] actions, never raw keys.
 */
class PlayerController(
    private val walkSpeed: Float = 150f,
    private val runSpeed: Float = 230f,
    private val acceleration: Float = 900f
) {

    var canMove: Boolean = true

    var isRunning: Boolean = false
        private set

    /** Called every fixed tick. [solids] are world-object collision rects. */
    fun update(player: PlayerEntity, grid: TileGrid, input: InputManager, dt: Float, solids: List<Rect> = emptyList()) {
        if (!canMove) {
            player.entity.velocity = player.entity.velocity.lerp(Vec2.Zero, 1f - kotlin.math.exp(-10f * dt))
            return
        }
        val axis = input.state.moveAxis
        val running = input.state.runAmount > 0.5f
        isRunning = running
        val speed = if (running) runSpeed else walkSpeed
        val target = Vec2(axis.x * speed, axis.y * speed)

        // Smooth accel/decel toward the target velocity.
        val current = player.entity.velocity
        val rate = if (target.lengthSquared() > 0f) acceleration / speed else 8f
        val t = 1f - kotlin.math.exp(-rate * dt)
        val desired = current.lerp(target, t)
        player.entity.velocity = desired

        // Facing from the dominant movement axis (only when actually moving).
        if (target.lengthSquared() > 1f) {
            player.entity.facing = if (abs(target.x) > abs(target.y)) {
                if (target.x > 0f) Direction.Right else Direction.Left
            } else {
                if (target.y > 0f) Direction.Down else Direction.Up
            }
        }

        // Collide against solid tiles (axis-separated slide), then against
        // world objects (trees, buildings, fences) the same way.
        val tileResolved = grid.resolve(
            player.entity.position,
            player.entity.size,
            player.entity.velocity,
            dt
        )
        player.entity.position = resolveAgainstSolids(tileResolved, player.entity.size, player.entity.velocity, dt, solids)
        player.animation.update(player.entity, dt)
    }

    /** Slide along object rects, axis-separated like the tile pass. */
    private fun resolveAgainstSolids(
        position: Vec2,
        size: Vec2,
        velocity: Vec2,
        dt: Float,
        solids: List<Rect>
    ): Vec2 {
        if (solids.isEmpty()) return position
        var result = position
        // Horizontal.
        val nx = result.x + velocity.x * dt
        val xRect = Rect(nx - size.x / 2f, result.y - size.y, size.x, size.y)
        val xHit = solids.firstOrNull { it.overlaps(xRect) }
        result = result.withX(if (xHit != null) {
            if (velocity.x > 0f) xHit.x - size.x / 2f else xHit.right + size.x / 2f
        } else {
            nx
        })
        // Vertical.
        val ny = result.y + velocity.y * dt
        val yRect = Rect(result.x - size.x / 2f, ny - size.y, size.x, size.y)
        val yHit = solids.firstOrNull { it.overlaps(yRect) }
        result = result.withY(if (yHit != null) {
            if (velocity.y > 0f) yHit.y - size.y else yHit.bottom + size.y
        } else {
            ny
        })
        return result
    }
}

/** Idle/walk animation frame selection driven by velocity. */
class PlayerAnimation {
    private var bobPhase = 0f

    /** 0..1 walk cycle phase, used by the renderer to bob limbs. */
    var phase: Float = 0f
        private set

    fun update(entity: Entity, dt: Float) {
        val speed = entity.velocity.length()
        if (speed > 8f) {
            bobPhase += dt * speed / 24f
        }
        phase = bobPhase % 1f
    }
}

/** Runtime player: persisted [PlayerState] + live [Entity]. */
class PlayerEntity(
    var state: PlayerState,
    val entity: Entity
) {
    val animation = PlayerAnimation()

    fun syncPositionToState() {
        state = state.copy(
            position = WorldPoint(entity.position.x, entity.position.y),
            facing = entity.facing.name
        )
    }

    fun applyStateToEntity() {
        entity.teleport(state.position.toVec2())
        entity.facing = Direction.entries.firstOrNull { it.name == state.facing } ?: Direction.Down
    }

    /** Convenience for the direction the player faces. */
    fun facingVec(): Vec2 = when (entity.facing) {
        Direction.Up -> Vec2.Up
        Direction.Down -> Vec2.Down
        Direction.Left -> Vec2.Left
        Direction.Right -> Vec2.Right
    }
}
