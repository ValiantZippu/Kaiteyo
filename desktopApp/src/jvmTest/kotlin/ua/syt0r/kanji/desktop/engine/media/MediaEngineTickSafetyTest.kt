package ua.syt0r.kanji.desktop.engine.media

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.engine.playback.BackendKind
import ua.syt0r.kanji.desktop.engine.playback.MediaTrackInfo
import ua.syt0r.kanji.desktop.engine.playback.PlaybackBackend
import ua.syt0r.kanji.desktop.engine.playback.PlaybackCapability
import ua.syt0r.kanji.desktop.engine.playback.PlaybackChapter
import ua.syt0r.kanji.desktop.engine.playback.PlaybackEvent

/**
 * The 10 Hz reconciliation loop runs on the composition's coroutine scope:
 * an uncaught exception there tears down the entire window. These tests pin
 * the guarantee that MediaEngine.tick() never propagates — no matter how
 * badly a backend (or anything tick touches) misbehaves. That is exactly the
 * "clicking Media closes the application" crash class, so a regression here
 * means the app dies again.
 */
class MediaEngineTickSafetyTest {

    /** Backend that dies on the very first state poll. */
    private class ExplodingBackend(
        override val isAvailable: Boolean = true
    ) : PlaybackBackend {
        override val kind = BackendKind.Mpv
        override val capabilities: Set<PlaybackCapability> = setOf(PlaybackCapability.CanSeek)
        override var listener: ((PlaybackEvent) -> Unit)? = null
        override val diagnosticName: String = "exploding test backend"
        override fun open(source: String) = Result.success(Unit)
        override fun play() = Unit
        override fun pause() = Unit
        override fun stop() = Unit
        override fun seekTo(ms: Long) = Unit
        override fun setSpeed(rate: Float) = Unit
        override fun setVolume(percent: Int) = Unit
        override fun setMuted(muted: Boolean) = Unit
        override fun setLoop(loop: Boolean) = Unit
        override fun availableTracks(): List<MediaTrackInfo> = emptyList()
        override fun selectTrack(trackId: String?) = Unit
        override fun setSubtitleDelay(delayMs: Long) = Unit
        override fun frameStepForward() = false
        override fun frameStepBackward() = false
        override fun snapshot(target: File): Result<String> =
            Result.failure(IllegalStateException("not supported"))
        override fun chapters(): List<PlaybackChapter> = emptyList()
        override fun currentPositionMs(): Long = throw IllegalStateException("backend exploded")
        override fun durationMs(): Long = throw IllegalStateException("backend exploded")
        override val isPlaying: Boolean get() = false
        override val isBuffering: Boolean get() = false
        override fun close() = Unit
    }

    /** Backend that reports stable state — proves the normal path still runs. */
    private class HealthyBackend : PlaybackBackend {
        override val kind = BackendKind.Audio
        override val capabilities: Set<PlaybackCapability> = emptySet()
        override var listener: ((PlaybackEvent) -> Unit)? = null
        override val isAvailable: Boolean get() = true
        override val diagnosticName: String = "healthy test backend"
        override fun open(source: String) = Result.success(Unit)
        override fun play() = Unit
        override fun pause() = Unit
        override fun stop() = Unit
        override fun seekTo(ms: Long) = Unit
        override fun setSpeed(rate: Float) = Unit
        override fun setVolume(percent: Int) = Unit
        override fun setMuted(muted: Boolean) = Unit
        override fun setLoop(loop: Boolean) = Unit
        override fun availableTracks(): List<MediaTrackInfo> = emptyList()
        override fun selectTrack(trackId: String?) = Unit
        override fun setSubtitleDelay(delayMs: Long) = Unit
        override fun frameStepForward() = false
        override fun frameStepBackward() = false
        override fun snapshot(target: File): Result<String> =
            Result.failure(IllegalStateException("not supported"))
        override fun chapters(): List<PlaybackChapter> = emptyList()
        override fun currentPositionMs(): Long = 12_000
        override fun durationMs(): Long = 1_000_000
        override val isPlaying: Boolean get() = true
        override val isBuffering: Boolean get() = false
        override fun close() = Unit
    }

    /**
     * Build an AppState + its MediaEngine with user.home redirected into a
     * throwaway directory, so the engines only ever touch the sandbox.
     */
    private fun withEngine(block: (AppState, MediaEngine) -> Unit) {
        val originalHome = System.getProperty("user.home")
        val sandbox = File.createTempFile("kaiteyo-tick-safety", "").let {
            it.delete()
            File(it.parentFile, "kaiteyo-tick-safety-${System.nanoTime()}")
        }
        sandbox.mkdirs()
        try {
            System.setProperty("user.home", sandbox.absolutePath)
            val state = AppState()
            try {
                block(state, state.media)
            } finally {
                runCatching { state.media.shutdown() }
            }
        } finally {
            System.setProperty("user.home", originalHome)
            sandbox.deleteRecursively()
        }
    }

    @Test
    fun tickWithExplodingBackendNeverPropagates() {
        withEngine { _, media ->
            val backend = ExplodingBackend(isAvailable = true)
            media.activeBackend = backend
            media.backendKind = BackendKind.Mpv

            // A backend that dies mid-poll must be swallowed, not thrown up
            // into the composition scope. The loop runs forever, so exercise
            // many consecutive ticks.
            repeat(50) { media.tick() }

            assertEquals(0L, media.positionMs)
            // The engine keeps the session intact — the user can still stop
            // or close the backend explicitly from the UI.
            assertEquals(backend, media.activeBackend)
        }
    }

    @Test
    fun tickWithUnavailableBackendIsCleanedUp() {
        withEngine { _, media ->
            media.activeBackend = ExplodingBackend(isAvailable = false)
            media.backendKind = BackendKind.Mpv

            media.tick()

            // A backend that both throws and reports unavailable is dropped so
            // it cannot poison every subsequent tick.
            assertNull(media.activeBackend)
            assertEquals(BackendKind.None, media.backendKind)
            assertNotNull(media.playbackError)

            // Subsequent ticks stay safe with the backend cleared.
            media.tick()
            assertNull(media.activeBackend)
        }
    }

    @Test
    fun tickWithoutBackendIsSafe() {
        withEngine { _, media ->
            media.activeBackend = null
            media.backendKind = BackendKind.None

            media.tick()

            assertEquals(BackendKind.None, media.backendKind)
            assertEquals(0L, media.positionMs)
        }
    }

    @Test
    fun tickWithHealthyBackendAdvancesPosition() {
        withEngine { _, media ->
            media.activeBackend = HealthyBackend()
            media.backendKind = BackendKind.Audio

            media.tick()

            assertEquals(12_000L, media.positionMs)
            assertEquals(1_000_000L, media.durationMs)
        }
    }

    @Test
    fun segmentationHelpersNeverThrow() {
        withEngine { _, media ->
            val cue = SubtitleCue(id = "c1", startMs = 0, endMs = 1000, text = "今日は学校に行きます")

            // These are read during composition on every frame — they must
            // degrade to empty/zero instead of taking the window down.
            assertTrue(media.tokensFor(cue).isNotEmpty())
            assertTrue(media.currentCoverage >= 0f)
            assertTrue(media.coverageFor(cue.text).totalTokens >= 0)
        }
    }
}
