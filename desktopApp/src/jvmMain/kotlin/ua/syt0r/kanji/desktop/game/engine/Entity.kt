package ua.syt0r.kanji.desktop.game.engine

import ua.syt0r.kanji.desktop.game.engine.geom.Vec2

/**
 * A lightweight entity. The game uses a pragmatic entity/component-lite model
 * rather than a full ECS: an [Entity] carries an id, a world transform and a
 * small map of typed components. Systems (player controller, npc director,
 * interaction) look entities up by role/component, which keeps the engine
 * simple while leaving room to move to a full ECS inside the [GameEngine]
 * without touching the rest of the architecture.
 */
class Entity(
    val id: String,
    initialPosition: Vec2 = Vec2.Zero
) {
    var position: Vec2 = initialPosition
    var velocity: Vec2 = Vec2.Zero
    var size: Vec2 = Vec2(24f, 32f)
    var facing: Direction = Direction.Down
    var solid: Boolean = false
    var active: Boolean = true

    private val components = mutableMapOf<Class<*>, Any>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(componentClass: Class<T>): T? = components[componentClass] as T?

    fun <T : Any> set(component: T) {
        components[component.javaClass] = component
    }

    fun remove(componentClass: Class<*>) {
        components.remove(componentClass)
    }

    fun has(componentClass: Class<*>): Boolean = components.containsKey(componentClass)

    val rect: ua.syt0r.kanji.desktop.game.engine.geom.Rect
        get() = ua.syt0r.kanji.desktop.game.engine.geom.Rect(
            position.x - size.x / 2f,
            position.y - size.y,
            size.x,
            size.y
        )

    /** Feet position (used for ground alignment and depth sorting). */
    val feet: Vec2 get() = position

    fun moveBy(delta: Vec2) {
        position += delta
    }

    fun teleport(to: Vec2) {
        position = to
        velocity = Vec2.Zero
    }
}

enum class Direction { Up, Down, Left, Right }

/** Tracks and iterates every entity in a scene. */
class EntityManager {
    private val entitiesById = mutableMapOf<String, Entity>()
    private val order = mutableListOf<Entity>()

    val entities: List<Entity> get() = order.toList()

    fun add(entity: Entity): Entity {
        entitiesById[entity.id] = entity
        order.add(entity)
        return entity
    }

    fun remove(id: String) {
        val entity = entitiesById.remove(id) ?: return
        order.remove(entity)
    }

    fun get(id: String): Entity? = entitiesById[id]

    fun clear() {
        entitiesById.clear()
        order.clear()
    }

    fun <T : Any> allWith(componentClass: Class<T>): List<Pair<Entity, T>> =
        order.mapNotNull { entity -> entity.get(componentClass)?.let { entity to it } }
}
