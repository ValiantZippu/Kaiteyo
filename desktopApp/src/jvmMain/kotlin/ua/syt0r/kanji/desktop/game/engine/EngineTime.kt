package ua.syt0r.kanji.desktop.game.engine

/**
 * Fixed-timestep accumulator — the classic "fix your timestep" loop.
 *
 * The engine advances in discrete [tick] steps of [fixedDelta] (default 60 Hz)
 * regardless of the host frame rate, so movement, physics and NPC schedules
 * behave identically on a 30 FPS machine and a 240 FPS machine. Frames that
 * take longer than [maxFrameDelta] are clamped (a paused debugger must never
 * cause a physics "time bomb").
 */
class GameLoop(
    val fixedDelta: Float = 1f / 60f,
    val maxFrameDelta: Float = 1f / 4f
) {
    private var accumulator = 0f
    var tickCount: Long = 0L
        private set
    var alpha: Float = 0f
        private set

    /**
     * Feed one host frame's elapsed time. Returns the number of fixed steps
     * that must run this frame (usually 0 or 1).
     */
    fun advance(frameDelta: Float): Int {
        val clamped = frameDelta.coerceIn(0f, maxFrameDelta)
        accumulator += clamped
        var steps = 0
        while (accumulator >= fixedDelta && steps < 8) {
            accumulator -= fixedDelta
            tickCount++
            steps++
        }
        if (steps >= 8) accumulator = 0f
        alpha = (accumulator / fixedDelta).coerceIn(0f, 1f)
        return steps
    }

    fun reset() {
        accumulator = 0f
        alpha = 0f
    }
}

/** Engine-wide time bookkeeping handed to systems each tick. */
class EngineTime(val fixedDelta: Float = 1f / 60f) {
    /** Seconds since the engine started (fixed-step time). */
    var elapsedSeconds: Float = 0f
        private set

    /** The delta of the current tick (always the engine's fixed step). */
    val delta: Float get() = fixedDelta

    /** Total number of fixed steps run. */
    var tick: Long = 0L
        private set

    fun advance() {
        elapsedSeconds += fixedDelta
        tick++
    }
}
