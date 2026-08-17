package ua.syt0r.kanji.desktop.game.npc

import kotlinx.serialization.Serializable
import ua.syt0r.kanji.desktop.game.engine.Entity
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2
import ua.syt0r.kanji.desktop.game.world.WorldPoint

// ============================================================
// NPC SYSTEM (spec §52-53)
// NPCs have id, appearance, schedule, dialogue, language level,
// quests and relationships. Schedules make the world feel alive.
// ============================================================

@Serializable
data class NpcDefinition(
    val id: String,
    val name: String,
    val nameJp: String = "",
    val role: String = "",
    val appearance: NpcAppearance = NpcAppearance(),
    val cellId: String,
    val anchor: WorldPoint,
    /** Where the NPC goes through the day (minutes from midnight). */
    val schedule: List<NpcScheduleEntry> = emptyList(),
    /**
     * Optional patrol: the NPC loops through these points, pausing at each
     * (spec §39). Patrols drive NPCs that wander without a hard schedule.
     */
    val patrolPoints: List<WorldPoint> = emptyList(),
    /** Seconds to pause at each patrol point before moving on. */
    val patrolPauseSeconds: Float = 2f,
    /**
     * Chained routes (spec §39, §52): the NPC walks a different patrol route
     * per time window. The active route is chosen by the world clock, so an
     * NPC can wander the beach in the morning and the market at dusk without
     * teleporting. Empty = plain patrol or schedule only.
     */
    val routes: List<NpcRoute> = emptyList(),
    /** Weather kinds this NPC is present in; empty = any weather (spec §41). */
    val weatherPhases: List<String> = emptyList(),
    /** Seasons this NPC is present in; empty = every season (spec §42). */
    val seasons: List<String> = emptyList(),
    val dialogueId: String? = null,
    /** Rough learner-facing language level of this NPC's lines. */
    val languageLevel: Int = 1,
    val knowledgeTargets: List<String> = emptyList(),
    val questIds: List<String> = emptyList(),
    /**
     * Knowledge-node ids this NPC lights up about (spec §53): talking about
     * a favorite topic deepens the relationship (affinity) — the same words
     * the player is trying to learn.
     */
    val favoriteTopics: List<String> = emptyList(),
    /** Greeting flavour; shown above the head when idle. */
    val idleLine: String = "",
    val idleLineJp: String = ""
)

@Serializable
data class NpcAppearance(
    /** Simple vector-art identity: hair/shirt/pants colours + accessory. */
    val hairColor: Int = 0x4A3B2A,
    val shirtColor: Int = 0x7FA8C9,
    val pantsColor: Int = 0x3E4A5A,
    val accessory: String = "",
    val bodyType: String = "standard"
)

@Serializable
data class NpcScheduleEntry(
    /** World-clock minute of day when this leg starts (0..1439). */
    val fromMinutes: Int,
    /** ...and when it ends. */
    val toMinutes: Int,
    val cellId: String,
    val position: WorldPoint
)

/**
 * One chained patrol leg (spec §39, §52): during [fromMinutes]..[toMinutes]
 * the NPC loops through [points], pausing [pauseSeconds] at each. Routes are
 * tried in order and the first whose window covers the clock wins.
 */
@Serializable
data class NpcRoute(
    val fromMinutes: Int,
    val toMinutes: Int,
    val points: List<WorldPoint>,
    val pauseSeconds: Float = 2f
)

/** Light relationship state (spec §53) — met/talked/helped/quest history. */
@Serializable
data class NpcRelationship(
    val npcId: String,
    var met: Boolean = false,
    var talkedCount: Int = 0,
    var helpedCount: Int = 0,
    val questHistory: List<String> = emptyList(),
    val favoriteTopics: List<String> = emptyList(),
    /**
     * Deepened by talking about the NPC's favorite topics (spec §53).
     * Purely additive — there is no failure state, only familiarity.
     */
    var affinity: Int = 0
)

/** Runtime NPC: definition + live entity + relationship. */
class NpcRuntime(
    val definition: NpcDefinition,
    val entity: Entity,
    var relationship: NpcRelationship = NpcRelationship(
        definition.id,
        favoriteTopics = definition.favoriteTopics
    )
) {
    val id: String get() = definition.id

    /** Whether the NPC is currently walking to a schedule waypoint. */
    var isMoving: Boolean = false
        private set

    /** Patrol bookkeeping (spec §39) — which point, and the pause countdown. */
    private var patrolIndex = 0
    private var patrolPauseLeft = 0f

    /** Position from the schedule at a world-clock minute. */
    fun scheduledPosition(minuteOfDay: Int): WorldPoint? {
        val entry = definition.schedule.firstOrNull { minuteOfDay in it.fromMinutes..it.toMinutes }
        return entry?.position
    }

    /** The chained route covering [minuteOfDay], or null (spec §39, §52). */
    fun activeRoute(minuteOfDay: Int): NpcRoute? =
        definition.routes.firstOrNull { minuteOfDay in it.fromMinutes..it.toMinutes }

    /**
     * Advance the patrol loop by one tick, walking [points] (the active
     * route's leg, or the plain patrol). Returns true while walking,
     * false while pausing at a point (or no patrol configured).
     */
    fun patrolTick(speed: Float, dt: Float, points: List<WorldPoint> = definition.patrolPoints): Boolean {
        if (points.isEmpty()) return false
        if (patrolPauseLeft > 0f) {
            patrolPauseLeft -= dt
            isMoving = false
            return false
        }
        val target = points[patrolIndex % points.size].toVec2()
        val arrived = !walkTowards(target, speed, dt)
        if (arrived) {
            patrolIndex = (patrolIndex + 1) % points.size
            patrolPauseLeft = definition.patrolPauseSeconds
        }
        return isMoving
    }

    /**
     * Walk toward a target at [speed]; returns true while still en route.
     * The NPC stops when close enough (arrived → idle), so a schedule leg
     * becomes a real little walk, not a teleport (spec §39, §52).
     */
    fun walkTowards(target: Vec2, speed: Float, dt: Float, arriveDistance: Float = 3f): Boolean {
        val toTarget = target - entity.position
        val distance = toTarget.length()
        if (distance <= arriveDistance) {
            isMoving = false
            entity.velocity = Vec2.Zero
            return false
        }
        val step = (speed * dt).coerceAtMost(distance)
        entity.position += toTarget.normalized() * step
        entity.facing = when {
            kotlin.math.abs(toTarget.x) > kotlin.math.abs(toTarget.y) ->
                if (toTarget.x > 0) ua.syt0r.kanji.desktop.game.engine.Direction.Right else ua.syt0r.kanji.desktop.game.engine.Direction.Left
            else -> if (toTarget.y > 0) ua.syt0r.kanji.desktop.game.engine.Direction.Down else ua.syt0r.kanji.desktop.game.engine.Direction.Up
        }
        isMoving = true
        return true
    }
}

/**
 * Spawns NPCs into the entity manager and moves them along their schedules
 * as the world clock advances. The slice's NPCs stand near their anchors;
 * the schedule machinery already supports full daily routines.
 */
class NpcDirector(
    val definitions: List<NpcDefinition>
) {
    val runtimes = mutableListOf<NpcRuntime>()
    private val byId = mutableMapOf<String, NpcRuntime>()

    fun clear() {
        for (runtime in runtimes) {
            runtime.entity.active = false
        }
        runtimes.clear()
        byId.clear()
    }

    /**
     * Spawn the NPCs that live in [allowedCellIds] (the active region's
     * cells). NPCs of other regions are not instantiated until the player
     * travels there — one world, region-scoped population.
     */
    fun spawn(entities: ua.syt0r.kanji.desktop.game.engine.EntityManager, defaultMinute: Int = 9 * 60, allowedCellIds: Set<String>? = null) {
        if (runtimes.isNotEmpty()) return
        for (definition in definitions) {
            if (allowedCellIds != null && definition.cellId !in allowedCellIds) continue
            // Scheduled NPCs appear only during their time window (spec §40,
            // §52): a festival stall-holder with an evening-only schedule is
            // not standing there at noon. Always-present NPCs (static or
            // patrol, no schedule) spawn at their anchor.
            val scheduled = definition.schedule
                .firstOrNull { defaultMinute in it.fromMinutes..it.toMinutes }
                ?.position
            if (definition.schedule.isNotEmpty() && scheduled == null) continue
            spawnDefinition(definition, entities, scheduled ?: definition.anchor)
        }
    }

    private fun spawnDefinition(
        definition: NpcDefinition,
        entities: ua.syt0r.kanji.desktop.game.engine.EntityManager,
        position: WorldPoint
    ) {
        val entity = entities.add(
            Entity(
                id = "npc-${definition.id}",
                initialPosition = position.toVec2()
            ).apply {
                size = Vec2(26f, 34f)
                solid = true
            }
        )
        val runtime = NpcRuntime(definition, entity)
        runtimes.add(runtime)
        byId[definition.id] = runtime
    }

    fun npc(id: String): NpcRuntime? = byId[id]

    fun allNpcs(): List<NpcRuntime> = runtimes.toList()

    /**
     * Advance schedules AND the time-window population: NPCs whose window
     * opens spawn in, whose window closes despawn. Returns true when the
     * population changed (the session rebuilds interactables).
     *
     * [weatherKind] gates weather-conditional NPCs (spec §41): an NPC that
     * only appears in the rain is absent when the sun is out.
     */
    fun tick(entities: ua.syt0r.kanji.desktop.game.engine.EntityManager, minuteOfDay: Int, dt: Float, weatherKind: String = "Sun", seasonKind: String = "Summer"): Boolean {
        var changed = false
        // Spawn NPCs whose time window just opened (evening festival folk).
        for (definition in definitions) {
            if (byId.containsKey(definition.id)) continue
            if (!weatherAllows(definition, weatherKind)) continue
            if (!seasonAllows(definition, seasonKind)) continue
            val covering = definition.schedule.firstOrNull { minuteOfDay in it.fromMinutes..it.toMinutes }
            val route = definition.routes.firstOrNull { minuteOfDay in it.fromMinutes..it.toMinutes }
            when {
                definition.schedule.isNotEmpty() && covering != null -> {
                    spawnDefinition(definition, entities, covering.position)
                    changed = true
                }
                definition.routes.isNotEmpty() && route != null -> {
                    // Chained route NPC: appears at its window's first point.
                    spawnDefinition(definition, entities, route.points.firstOrNull() ?: definition.anchor)
                    changed = true
                }
                definition.schedule.isEmpty() && definition.routes.isEmpty() -> {
                    // Patrol NPCs are always present; instantiate at their anchor.
                    spawnDefinition(definition, entities, definition.anchor)
                    changed = true
                }
            }
        }
        // Despawn NPCs whose time window has closed (or weather changed).
        for (runtime in runtimes.toList()) {
            val definition = runtime.definition
            if (!weatherAllows(definition, weatherKind) || !seasonAllows(definition, seasonKind)) {
                runtime.entity.active = false
                runtimes.remove(runtime)
                byId.remove(definition.id)
                changed = true
                continue
            }
            if (definition.schedule.isEmpty() && definition.routes.isEmpty()) continue
            val covering = definition.schedule.firstOrNull { minuteOfDay in it.fromMinutes..it.toMinutes }
            val route = definition.routes.firstOrNull { minuteOfDay in it.fromMinutes..it.toMinutes }
            if (covering == null && route == null) {
                runtime.entity.active = false
                runtimes.remove(runtime)
                byId.remove(definition.id)
                changed = true
            }
        }
        // Movement: scheduled NPCs walk to their waypoint; patrol and chained-
        // route NPCs loop their active leg with pauses (velocity-based, §39).
        for (runtime in runtimes) {
            if (!runtime.entity.active) continue
            val scheduled = runtime.scheduledPosition(minuteOfDay)?.toVec2()
            if (scheduled != null) {
                runtime.walkTowards(scheduled, npcWalkSpeed, dt)
            } else {
                val leg = runtime.activeRoute(minuteOfDay)?.points ?: emptyList()
                runtime.patrolTick(npcWalkSpeed, dt, if (leg.isEmpty()) runtime.definition.patrolPoints else leg)
            }
        }
        return changed
    }

    private fun weatherAllows(definition: NpcDefinition, weatherKind: String): Boolean =
        definition.weatherPhases.isEmpty() ||
            definition.weatherPhases.any { it.equals(weatherKind, ignoreCase = true) }

    private fun seasonAllows(definition: NpcDefinition, seasonKind: String): Boolean =
        definition.seasons.isEmpty() ||
            definition.seasons.any { it.equals(seasonKind, ignoreCase = true) }

    companion object {
        /** Walk speed in world units/second — stroll, not sprint. */
        const val npcWalkSpeed = 26f
    }
}
