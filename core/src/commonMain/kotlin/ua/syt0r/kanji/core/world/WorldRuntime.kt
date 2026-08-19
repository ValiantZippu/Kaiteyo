package ua.syt0r.kanji.core.world

// ============================================================
// WORLD — RUNTIME ISOLATION & LIFECYCLE
// ------------------------------------------------------------
// The World runs inside its own runtime, isolated from the app's
// UI lifecycle. It owns the game loop, the chunk manager, and
// all world systems (weather, NPCs, vehicles, etc.). The app
// talks to it through a narrow controller interface — a World
// crash can never take down the app shell.
//
//   WorldRuntime        the container + game loop
//   WorldController     the app-facing handle
//   WorldState          observable snapshot
//   WorldEvent          events emitted to listeners
// ============================================================

/**
 * Lifecycle states of the world runtime.
 */
enum class WorldRuntimeState(val label: String) {
    Stopped("Stopped"),
    Starting("Starting"),
    Running("Running"),
    Paused("Paused"),
    Stopping("Stopping"),
    Crashed("Crashed")
}

/**
 * Observable snapshot of the world.
 */
data class WorldState(
    val runtimeState: WorldRuntimeState = WorldRuntimeState.Stopped,
    val playerPosition: WorldPosition = WorldPosition.Zero,
    val playerChunk: ChunkCoord = ChunkCoord(0, 0),
    val loadedChunks: Int = 0,
    val activeNpcs: Int = 0,
    val activeVehicles: Int = 0,
    val timeOfDay: Float = 0.5f, // 0..1 (0 = midnight, 0.5 = noon)
    val weather: WorldWeather = WorldWeather.Clear,
    val fps: Float = 0f,
    val memoryBytes: Long = 0,
    val lastError: String? = null
)

/**
 * Events emitted by the world runtime.
 */
sealed interface WorldEvent {
    data class StateChanged(val state: WorldRuntimeState) : WorldEvent
    data class ChunkLoaded(val coord: ChunkCoord) : WorldEvent
    data class ChunkUnloaded(val coord: ChunkCoord) : WorldEvent
    data class PlayerMoved(val position: WorldPosition) : WorldEvent
    data class Error(val message: String, val cause: Throwable? = null) : WorldEvent
    data object Tick : WorldEvent
}

/**
 * A world system — a self-contained subsystem updated once per frame.
 */
interface WorldSystem {
    /** Called once when the system starts. */
    suspend fun onStart(runtime: WorldRuntime) {}

    /** Called once when the system stops. */
    suspend fun onStop(runtime: WorldRuntime) {}

    /** Called every frame with the delta time in seconds. */
    suspend fun onUpdate(deltaSeconds: Double, runtime: WorldRuntime) {}
}

/**
 * The world runtime. Owns the game loop and all systems.
 * Lifecycle is strict: start() → running → stop(). Any system
 * failure is caught and recorded instead of propagating.
 */
class WorldRuntime(
    /** Chunk manager used to stream the world. */
    val chunks: WorldChunkManager,
    /** World systems, updated in order every frame. */
    val systems: List<WorldSystem> = emptyList(),
    /** Target updates per second. */
    val targetFps: Int = 60,
    /** Whether a crash should pause (true) or kill the loop (false). */
    val isolateCrashes: Boolean = true
) {

    private val _runtimeState = kotlinx.coroutines.flow.MutableStateFlow(WorldRuntimeState.Stopped)
    private val _playerPosition = kotlinx.coroutines.flow.MutableStateFlow(WorldPosition.Zero)
    private val _timeOfDay = kotlinx.coroutines.flow.MutableStateFlow(0.5f)
    private val _weather = kotlinx.coroutines.flow.MutableStateFlow(WorldWeather.Clear)
    private val _lastError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    /** The runtime lifecycle state. */
    val runtimeState: kotlinx.coroutines.flow.StateFlow<WorldRuntimeState> = _runtimeState
    /** Current player position (updated by the player system). */
    val playerPosition: kotlinx.coroutines.flow.StateFlow<WorldPosition> = _playerPosition
    /** Current time of day 0..1. */
    val timeOfDay: kotlinx.coroutines.flow.StateFlow<Float> = _timeOfDay
    /** Current weather. */
    val weather: kotlinx.coroutines.flow.StateFlow<WorldWeather> = _weather
    /** Last isolated error (null when healthy). */
    val lastError: kotlinx.coroutines.flow.StateFlow<String?> = _lastError

    private val listeners = mutableListOf<(WorldEvent) -> Unit>()
    private var running = false
    private var paused = false
    private var loopJob: kotlinx.coroutines.Job? = null
    private var lastFrameTime = 0L

    /** Live snapshot for non-flow consumers. */
    fun snapshot(): WorldState = WorldState(
        runtimeState = _runtimeState.value,
        playerPosition = _playerPosition.value,
        playerChunk = ChunkCoord.fromWorld(_playerPosition.value),
        loadedChunks = chunks.loadedCount,
        timeOfDay = _timeOfDay.value,
        weather = _weather.value,
        lastError = _lastError.value
    )

    /** Emits an event to all listeners. */
    fun onEvent(listener: (WorldEvent) -> Unit) {
        listeners.add(listener)
    }

    private fun emit(event: WorldEvent) {
        listeners.forEach { it(event) }
    }

    /** Sets the runtime lifecycle state. */
    fun setRuntimeState(state: WorldRuntimeState) {
        _runtimeState.value = state
        emit(WorldEvent.StateChanged(state))
    }

    /** Updates the player position (called by the player system). */
    fun updatePlayerPosition(position: WorldPosition) {
        _playerPosition.value = position
        emit(WorldEvent.PlayerMoved(position))
    }

    /** Sets the time of day. */
    fun setTimeOfDay(value: Float) {
        _timeOfDay.value = value.coerceIn(0f, 1f)
    }

    /** Sets the weather. */
    fun setWeather(value: WorldWeather) {
        _weather.value = value
    }

    /** Starts the runtime and the game loop. Idempotent. */
    suspend fun start() {
        if (running) return
        running = true
        setRuntimeState(WorldRuntimeState.Starting)

        try {
            for (system in systems) {
                runIsolated("system.onStart(${system::class.simpleName})") {
                    system.onStart(this)
                }
            }
            setRuntimeState(WorldRuntimeState.Running)
        } catch (t: Throwable) {
            crash("Failed to start world", t)
        }
    }

    /** Pauses the game loop (world stays loaded). */
    fun pause() {
        if (!running || paused) return
        paused = true
        setRuntimeState(WorldRuntimeState.Paused)
    }

    /** Resumes a paused loop. */
    fun resume() {
        if (!running || !paused) return
        paused = false
        setRuntimeState(WorldRuntimeState.Running)
    }

    /** Stops the runtime and releases resources. Idempotent. */
    suspend fun stop() {
        if (!running && _runtimeState.value == WorldRuntimeState.Stopped) return
        setRuntimeState(WorldRuntimeState.Stopping)
        loopJob?.cancel()
        loopJob = null
        try {
            for (system in systems.reversed()) {
                runIsolated("system.onStop(${system::class.simpleName})") {
                    system.onStop(this)
                }
            }
        } catch (t: Throwable) {
            // Best-effort cleanup — never throws out of stop().
        }
        chunks.unloadAll()
        running = false
        setRuntimeState(WorldRuntimeState.Stopped)
    }

    /**
     * Runs the game loop. Should be launched on a background scope.
     * Never returns an exception — crashes are isolated.
     */
    suspend fun runGameLoop() {
        if (!running) return
        val frameTimeMs = 1000L / targetFps
        lastFrameTime = System.currentTimeMillis()

        while (running) {
            if (paused) {
                kotlinx.coroutines.delay(frameTimeMs)
                continue
            }

            val now = System.currentTimeMillis()
            val delta = (now - lastFrameTime).coerceAtLeast(1L) / 1000.0
            lastFrameTime = now

            update(delta)

            val sleepMs = frameTimeMs - (System.currentTimeMillis() - now)
            if (sleepMs > 0) kotlinx.coroutines.delay(sleepMs)
        }
    }

    /** One frame: update every system inside the isolation boundary. */
    internal suspend fun update(deltaSeconds: Double) {
        for (system in systems) {
            runIsolated("system.onUpdate(${system::class.simpleName})") {
                system.onUpdate(deltaSeconds, this)
            }
        }
        emit(WorldEvent.Tick)
    }

    /** Runs [block], catching and recording failures instead of propagating. */
    private suspend fun runIsolated(label: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            if (isolateCrashes) {
                _lastError.value = "$label failed: ${t.message}"
                emit(WorldEvent.Error("$label failed: ${t.message}", t))
            } else {
                crash("$label failed", t)
            }
        }
    }

    private fun crash(message: String, cause: Throwable?) {
        running = false
        _lastError.value = message
        setRuntimeState(WorldRuntimeState.Crashed)
        emit(WorldEvent.Error(message, cause))
    }
}

/**
 * World weather states.
 */
enum class WorldWeather(val label: String, val emoji: String) {
    Clear("Clear", "☀️"),
    PartlyCloudy("Partly cloudy", "⛅"),
    Cloudy("Cloudy", "☁️"),
    Rain("Rain", "🌧️"),
    HeavyRain("Heavy rain", "⛈️"),
    Snow("Snow", "❄️"),
    Fog("Fog", "🌫️"),
    Wind("Wind", "💨")
}
