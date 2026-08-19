package ua.syt0r.kanji.presentation.screen.main.screen.world

import org.koin.dsl.module
import ua.syt0r.kanji.core.world.KamakuraWorld
import ua.syt0r.kanji.core.world.WorldSession
import ua.syt0r.kanji.presentation.multiplatformViewModel

val worldScreenModule = module {
    // One world session per app run — controller + player stay in sync.
    single {
        KamakuraWorld.createSession()
    }

    multiplatformViewModel<WorldScreenContract.ViewModel> {
        val session: WorldSession = get()
        WorldScreenViewModel(
            viewModelScope = it.component1(),
            controller = WorldScreenControllerBridge(session)
        )
    }
}

/**
 * Narrow bridge between the screen and the world. Exposes only the
 * [WorldScreenContract.WorldScreenControllerPort] surface — never
 * runtime internals.
 */
class WorldScreenControllerBridge(
    private val session: WorldSession
) : WorldScreenContract.WorldScreenControllerPort {

    private val controller = session.controller
    private val player = session.player

    override val runtimeState get() = controller.runtimeState
    override val playerPosition get() = controller.playerPosition
    override val timeOfDay get() = controller.timeOfDay
    override val weather get() = controller.weather
    override val lastError get() = controller.lastError
    override val worldMap get() = controller.worldMap

    override fun start() = controller.start()
    override fun stop() = controller.stop()
    override fun pause() = controller.pause()
    override fun resume() = controller.resume()
    override fun save() = controller.save()
    override fun load(): Boolean = controller.load("slot1")
    override fun dispose() = controller.dispose()

    override fun playerSnapshot() = player.player
    override fun loadedChunkCount(): Int = controller.loadedChunkCount()
    override fun queuedChunkCount(): Int = controller.queuedChunkCount()
    override fun move(input: ua.syt0r.kanji.core.world.MovementInput) = controller.move(input)
    override fun switchCamera() = controller.switchCamera()
    override fun teleportTo(locationId: String) = controller.teleportTo(locationId)
    override fun tick() = controller.tick()
}
