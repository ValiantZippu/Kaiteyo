package ua.syt0r.kanji.desktop.game.interaction

import ua.syt0r.kanji.desktop.game.engine.SpatialHash
import ua.syt0r.kanji.desktop.game.engine.geom.Vec2
import ua.syt0r.kanji.desktop.game.world.WorldObject

// ============================================================
// INTERACTION SYSTEM (spec §18-19, §10)
// A universal IInteractable: every object exposes a prompt and a
// behaviour; the system finds the nearest one and the HUD shows
// a minimal "[E] 話す" prompt only when relevant.
// ============================================================

/** What happens when an interactable is activated. */
sealed interface InteractionBehavior {
    /** Talk to an NPC (opens its dialogue). */
    data class Talk(val npcId: String) : InteractionBehavior

    /** Read an object's Japanese text (sign, menu, announcement). */
    data class ReadObject(
        val objectId: String,
        val label: String,
        val learningTargets: List<String> = emptyList()
    ) : InteractionBehavior

    /** Buy a drink from a vending machine. */
    data class BuyDrink(val objectId: String) : InteractionBehavior

    /** Buy food from a festival stall (takoyaki, taiyaki…). */
    data class BuyFood(val objectId: String) : InteractionBehavior

    /** Order from a stall's Japanese menu (spec §56 ordering minigame). */
    data class OrderFood(val objectId: String) : InteractionBehavior

    /** Sit on a bench (idle animation + rest). */
    data class SitBench(val objectId: String) : InteractionBehavior

    /** Open the travel overlay at a station. */
    data class OpenStation(val stationId: String) : InteractionBehavior

    /** Trigger a photo spot (composition hint). */
    data class PhotoSpot(val objectId: String) : InteractionBehavior

    /** Open the writing desk (kana tracing, spec §57-59). */
    data class WriteKana(val objectId: String, val targets: List<String>) : InteractionBehavior

    /** Generic discovery interaction (inspect → knowledge). */
    data class Inspect(val objectId: String, val learningTargets: List<String>) : InteractionBehavior
}

/**
 * An interactable object in the world: the object definition (with its
 * Japanese label), its prompt text and its behaviour. NPCs expose their own
 * interactables through the same model.
 */
data class Interactable(
    val id: String,
    val position: Vec2,
    val radius: Float,
    val promptJp: String,
    val promptEn: String,
    val behavior: InteractionBehavior,
    val worldObject: WorldObject? = null,
    /** Phases this interactable is open in; empty = always (spec §40). */
    val availablePhases: List<String> = emptyList(),
    /** Seasons this interactable is open in; empty = every season (spec §42). */
    val availableSeasons: List<String> = emptyList(),
    /** Weather this interactable is open in; empty = any weather (spec §41). */
    val availableWeather: List<String> = emptyList()
) {
    /** True when [phase] is a window this interactable is alive in. */
    fun isOpenAt(phase: String): Boolean =
        availablePhases.isEmpty() || availablePhases.any { it.equals(phase, ignoreCase = true) }

    /** True when [season] is a window this interactable is alive in. */
    fun isOpenInSeason(season: String): Boolean =
        availableSeasons.isEmpty() || availableSeasons.any { it.equals(season, ignoreCase = true) }

    /** True when [weather] is a window this interactable is alive in. */
    fun isOpenInWeather(weather: String): Boolean =
        availableWeather.isEmpty() || availableWeather.any { it.equals(weather, ignoreCase = true) }
}

/** The result of dispatching an interaction — handled by the game session. */
sealed interface InteractionResult {
    data class StartDialogue(val npcId: String) : InteractionResult
    data class ReadObject(val objectId: String, val label: String, val learningTargets: List<String>) : InteractionResult
    data class BuyDrink(val objectId: String) : InteractionResult
    data class BuyFood(val objectId: String) : InteractionResult
    data class OrderFood(val objectId: String) : InteractionResult
    data class SitBench(val objectId: String) : InteractionResult
    data class OpenTravel(val stationId: String) : InteractionResult
    data class PhotoSpot(val objectId: String) : InteractionResult
    data class WriteKana(val objectId: String, val targets: List<String>) : InteractionResult
    data object None : InteractionResult
}

/**
 * Finds the nearest interactable within reach and reports what the player
 * can act on. Registered from world objects + NPCs at load time.
 */
class InteractionSystem {

    private val spatial = SpatialHash(cellSize = 64f)
    private val byId = mutableMapOf<String, Interactable>()

    val interactRadius = 72f

    fun register(interactable: Interactable) {
        byId[interactable.id] = interactable
    }

    fun clear() {
        byId.clear()
        spatial.clear()
    }

    fun rebuild() {
        spatial.clear()
        for (interactable in byId.values) {
            spatial.insert(
                interactable.id,
                ua.syt0r.kanji.desktop.game.engine.geom.Rect.fromCenter(
                    interactable.position,
                    interactable.radius * 2f,
                    interactable.radius * 2f
                )
            )
        }
    }

    fun interactable(id: String): Interactable? = byId[id]

    /** Nearest interactable within [interactRadius] of [position]. */
    fun nearest(position: Vec2): Interactable? =
        spatial.nearest(position, interactRadius)?.let { byId[it.id] }

    /** Dispatch an interaction for the given position; returns the result. */
    fun interact(position: Vec2): InteractionResult {
        val target = nearest(position) ?: return InteractionResult.None
        return when (val behavior = target.behavior) {
            is InteractionBehavior.Talk -> InteractionResult.StartDialogue(behavior.npcId)
            is InteractionBehavior.ReadObject ->
                InteractionResult.ReadObject(behavior.objectId, behavior.label, behavior.learningTargets)
            is InteractionBehavior.BuyDrink -> InteractionResult.BuyDrink(behavior.objectId)
            is InteractionBehavior.BuyFood -> InteractionResult.BuyFood(behavior.objectId)
            is InteractionBehavior.OrderFood -> InteractionResult.OrderFood(behavior.objectId)
            is InteractionBehavior.SitBench -> InteractionResult.SitBench(behavior.objectId)
            is InteractionBehavior.OpenStation -> InteractionResult.OpenTravel(behavior.stationId)
            is InteractionBehavior.PhotoSpot -> InteractionResult.PhotoSpot(behavior.objectId)
            is InteractionBehavior.WriteKana -> InteractionResult.WriteKana(behavior.objectId, behavior.targets)
            is InteractionBehavior.Inspect ->
                InteractionResult.ReadObject(behavior.objectId, target.worldObject?.label.orEmpty(), behavior.learningTargets)
        }
    }
}
