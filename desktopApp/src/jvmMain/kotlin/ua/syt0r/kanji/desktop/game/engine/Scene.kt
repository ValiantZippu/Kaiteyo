package ua.syt0r.kanji.desktop.game.engine

import ua.syt0r.kanji.desktop.game.engine.render.RenderBackend

/**
 * A [Scene] owns the entities, systems and render behaviour of one game mode:
 * the exploration world, the camera/photo viewfinder, menus, or future
 * minigames. Scenes are data — the [SceneManager] decides which one is live.
 */
interface Scene {

    /** Unique id of the scene (e.g. "world", "photo", "dialogue", "menu"). */
    val id: String

    /** Called once when the scene becomes active. */
    fun onEnter() {}

    /** Called once when the scene is replaced/paused. */
    fun onExit() {}

    /** Fixed-step update. */
    fun update(engine: GameEngine, dt: Float) {}

    /** Render this scene's world content (the host applies HUD on top). */
    fun render(engine: GameEngine, backend: RenderBackend) {}
}

/** The set of scene kinds the engine knows about. */
enum class SceneKind(val sceneId: String) {
    World("world"),
    Photo("photo"),
    Dialogue("dialogue"),
    Menu("menu"),
    Travel("travel"),
    Loading("loading")
}

/** Stacked scene switching — push/pop keeps the exploration scene alive under menus. */
class SceneManager(initial: Scene) {
    private val stack = ArrayDeque<Scene>()

    val current: Scene get() = stack.last()

    val size: Int get() = stack.size

    init {
        stack.addLast(initial)
    }

    /** Push a scene on top; the previous one is paused but stays alive. */
    fun push(scene: Scene) {
        stack.lastOrNull()?.onExit()
        stack.addLast(scene)
        scene.onEnter()
    }

    /** Pop the top scene; returns the scene that is now current. */
    fun pop(): Scene? {
        if (stack.size <= 1) return null
        val leaving = stack.removeLast()
        leaving.onExit()
        val now = stack.last()
        now.onEnter()
        return now
    }

    fun replace(scene: Scene) {
        stack.lastOrNull()?.onExit()
        stack.removeLastOrNull()
        stack.addLast(scene)
        scene.onEnter()
    }

    fun clearTo(scene: Scene) {
        stack.forEach { it.onExit() }
        stack.clear()
        stack.addLast(scene)
        scene.onEnter()
    }

    fun scenes(): List<Scene> = stack.toList()
}
