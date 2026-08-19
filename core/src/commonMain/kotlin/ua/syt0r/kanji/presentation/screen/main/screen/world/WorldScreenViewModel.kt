package ua.syt0r.kanji.presentation.screen.main.screen.world

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.world.ChunkCoord
import ua.syt0r.kanji.core.world.MovementInput
import ua.syt0r.kanji.presentation.screen.main.screen.world.WorldScreenContract.ScreenState
import ua.syt0r.kanji.presentation.screen.main.screen.world.WorldScreenContract.WorldScreenControllerPort

/**
 * World screen viewmodel — owns a [WorldScreenControllerPort] and mirrors
 * its observable state into the screen state. Start/stop are tied to the
 * screen lifecycle via [start] / [dispose].
 */
class WorldScreenViewModel(
    private val viewModelScope: CoroutineScope,
    private val controller: WorldScreenControllerPort
) : WorldScreenContract.ViewModel {

    private val _state = MutableStateFlow(ScreenState())
    override val state: StateFlow<ScreenState> = _state

    private var collectJob: Job? = null
    private var inputJob: Job? = null

    override fun start() {
        // Re-entrant: if previously stopped, cancel stale jobs and restart.
        inputJob?.cancel()
        collectJob?.cancel()
        controller.start()

        collectJob = viewModelScope.launch {
            combine(
                controller.runtimeState,
                controller.playerPosition,
                controller.timeOfDay,
                controller.weather,
                controller.lastError
            ) { runtime, position, tod, weather, error ->
                val snapshot = controller.playerSnapshot()
                ScreenState(
                    runtimeState = runtime,
                    playerPosition = position,
                    playerChunk = ChunkCoord.fromWorld(position),
                    playerMovement = snapshot.movement,
                    cameraMode = snapshot.cameraMode,
                    loadedChunks = controller.loadedChunkCount(),
                    queuedChunks = controller.queuedChunkCount(),
                    timeOfDay = tod,
                    weather = weather,
                    lastError = error,
                    regions = controller.worldMap.allRegions(),
                    locations = controller.worldMap.allLocations(),
                    saveMessage = null
                )
            }.collect { _state.value = it }
        }

        // Drive ticks at ~20 Hz so the world animates even with no input.
        inputJob = viewModelScope.launch {
            while (true) {
                controller.tick()
                kotlinx.coroutines.delay(50)
            }
        }
    }

    override fun stop() {
        controller.stop()
    }

    override fun pause() = controller.pause()

    override fun resume() = controller.resume()

    override fun move(forward: Float, strafe: Float, run: Boolean) {
        controller.move(MovementInput(forward = forward, strafe = strafe, running = run))
    }

    override fun switchCamera() = controller.switchCamera()

    override fun teleportTo(locationId: String) {
        controller.teleportTo(locationId)
    }

    override fun save() {
        controller.save()
        _state.value = _state.value.copy(saveMessage = "Saved")
    }

    override fun load() {
        val ok = controller.load()
        _state.value = _state.value.copy(saveMessage = if (ok) "Loaded" else "No save found")
    }

    override fun dispose() {
        inputJob?.cancel()
        collectJob?.cancel()
        controller.dispose()
    }
}
