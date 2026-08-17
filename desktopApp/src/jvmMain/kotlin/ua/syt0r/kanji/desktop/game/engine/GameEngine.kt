package ua.syt0r.kanji.desktop.game.engine

import ua.syt0r.kanji.desktop.game.engine.render.RenderBackend

/**
 * The engine container. It owns the fixed-timestep [GameLoop], the
 * [EngineTime], the [EntityManager] and the currently active [Scene]. The
 * game session attaches the world scene here; minigames and menus become
 * additional scenes without touching the engine.
 *
 * This is a real engine core (fixed timestep, scene graph, entities, spatial
 * index, camera, input layer) — see `docs/game/ENGINE_DECISION.md` for how a
 * 3D engine (Orx/libGDX) plugs in at the render boundary.
 */
class GameEngine(
    val loop: GameLoop = GameLoop(),
    val time: EngineTime = EngineTime()
) {
    val entities = EntityManager()

    var currentScene: Scene? = null

    /**
     * Advance one host frame. Runs the fixed steps the loop demands and lets
     * the active scene update each of them.
     */
    fun advance(frameDelta: Float) {
        val steps = loop.advance(frameDelta)
        if (steps > 0) {
            repeat(steps) {
                time.advance()
                currentScene?.update(this, time.delta)
            }
        }
    }

    fun render(backend: RenderBackend) {
        currentScene?.render(this, backend)
    }
}
