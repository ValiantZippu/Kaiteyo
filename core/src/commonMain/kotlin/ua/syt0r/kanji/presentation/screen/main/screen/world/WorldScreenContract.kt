package ua.syt0r.kanji.presentation.screen.main.screen.world

import kotlinx.coroutines.flow.StateFlow
import ua.syt0r.kanji.core.world.CameraMode
import ua.syt0r.kanji.core.world.ChunkCoord
import ua.syt0r.kanji.core.world.MovementState
import ua.syt0r.kanji.core.world.WorldLocation
import ua.syt0r.kanji.core.world.WorldPosition
import ua.syt0r.kanji.core.world.WorldRegion
import ua.syt0r.kanji.core.world.WorldRuntimeState
import ua.syt0r.kanji.core.world.WorldWeather

// ============================================================
// WORLD — CONTRACT
// ------------------------------------------------------------
// The World screen hosts the Kamakura vertical slice. It is a
// real screen over the WorldController — no ghost UI. It shows
// the live runtime snapshot (state, position, chunk, time,
// weather, loaded chunks) and provides real controls: move,
// camera switch, teleport to known locations, save/load.
// ============================================================

interface WorldScreenContract {

    /**
     * The narrow app-facing port to the world. The screen only ever talks
     * to this — never to WorldRuntime internals. Implemented by the module
     * over the Kamakura world factory.
     */
    interface WorldScreenControllerPort {
        val runtimeState: kotlinx.coroutines.flow.StateFlow<WorldRuntimeState>
        val playerPosition: kotlinx.coroutines.flow.StateFlow<WorldPosition>
        val timeOfDay: kotlinx.coroutines.flow.StateFlow<Float>
        val weather: kotlinx.coroutines.flow.StateFlow<WorldWeather>
        val lastError: kotlinx.coroutines.flow.StateFlow<String?>
        val worldMap: ua.syt0r.kanji.core.world.WorldMap

        fun start()
        fun stop()
        fun pause()
        fun resume()
        fun save()
        fun load(): Boolean
        fun dispose()

        fun playerSnapshot(): ua.syt0r.kanji.core.world.WorldPlayer
        fun loadedChunkCount(): Int
        fun queuedChunkCount(): Int
        fun move(input: ua.syt0r.kanji.core.world.MovementInput)
        fun switchCamera()
        fun teleportTo(locationId: String)
        fun tick()
    }

    interface ViewModel {
        val state: StateFlow<ScreenState>

        fun start()
        fun stop()
        fun pause()
        fun resume()

        /** Applies a movement input frame. */
        fun move(forward: Float, strafe: Float, run: Boolean)

        /** Switches camera mode. */
        fun switchCamera()

        /** Teleports to a location by id. */
        fun teleportTo(locationId: String)

        /** Saves / loads a slot. */
        fun save()
        fun load()

        fun dispose()
    }

    data class ScreenState(
        val runtimeState: WorldRuntimeState = WorldRuntimeState.Stopped,
        val playerPosition: WorldPosition = WorldPosition.Zero,
        val playerChunk: ChunkCoord = ChunkCoord(0, 0),
        val playerMovement: MovementState = MovementState.Idle,
        val cameraMode: CameraMode = CameraMode.ThirdPerson,
        val loadedChunks: Int = 0,
        val queuedChunks: Int = 0,
        val timeOfDay: Float = 0.5f,
        val weather: WorldWeather = WorldWeather.Clear,
        val lastError: String? = null,
        val regions: List<WorldRegion> = emptyList(),
        val locations: List<WorldLocation> = emptyList(),
        val saveMessage: String? = null
    )
}
