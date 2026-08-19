package ua.syt0r.kanji.desktop.engine.media

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.engine.playback.BackendKind
import ua.syt0r.kanji.desktop.engine.playback.MediaTrackInfo
import ua.syt0r.kanji.desktop.engine.playback.PlaybackBackend
import ua.syt0r.kanji.desktop.engine.playback.PlaybackCapability
import ua.syt0r.kanji.desktop.engine.playback.PlaybackChapter
import ua.syt0r.kanji.desktop.engine.playback.PlaybackEvent

/**
 * Media lifecycle tests (KT-TEST-012, spec §24–§26, §41). The tick-safety
 * suite pins the 10 Hz loop; this suite pins the *lifecycle* guarantees:
 * shutdown never throws (even with a hostile backend attached), is idempotent
 * (app close paths may race), and afterwards the engine stays safe — a tick
 * after shutdown must not resurrect a session or throw into the scope.
 */
class MediaEngineLifecycleTest {

    /** Backend that throws from every lifecycle + poll call. */
    private class HostileBackend(
        override val isAvailable: Boolean = true
    ) : PlaybackBackend {
        override val kind = BackendKind.Mpv
        override val capabilities: Set<PlaybackCapability> = setOf(PlaybackCapability.CanSeek)
        override var listener: ((PlaybackEvent) -> Unit)? = null
        override val diagnosticName: String = "hostile test backend"
        override fun open(source: String) = Result.failure(IllegalStateException("hostile open"))
        override fun play() = throw IllegalStateException("hostile play")
        override fun pause() = throw IllegalStateException("hostile pause")
        override fun stop() = throw IllegalStateException("hostile stop")
        override fun seekTo(ms: Long) = throw IllegalStateException("hostile seek")
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
        override fun currentPositionMs(): Long = throw IllegalStateException("hostile poll")
        override fun durationMs(): Long = throw IllegalStateException("hostile poll")
        override val isPlaying: Boolean get() = true
        override val isBuffering: Boolean get() = false
        override fun close() = throw IllegalStateException("hostile close")
    }

    /** Backend reporting stable state — used to prove a live session. */
    private class TrackingBackend : PlaybackBackend {
        override val kind = BackendKind.Audio
        override val capabilities: Set<PlaybackCapability> = emptySet()
        override var listener: ((PlaybackEvent) -> Unit)? = null
        override val isAvailable: Boolean get() = true
        override val diagnosticName: String = "tracking test backend"
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
        override fun currentPositionMs(): Long = 5_000
        override fun durationMs(): Long = 100_000
        override val isPlaying: Boolean get() = false
        override val isBuffering: Boolean get() = false
        override fun close() = Unit
    }

    private fun withEngine(block: (AppState, MediaEngine) -> Unit) {
        val originalHome = System.getProperty("user.home")
        val sandbox = File.createTempFile("kaiteyo-lifecycle", "").let {
            it.delete()
            File(it.parentFile, "kaiteyo-lifecycle-${System.nanoTime()}")
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
    fun shutdownWithHostileBackendNeverPropagates() {
        withEngine { _, media ->
            media.activeBackend = HostileBackend(isAvailable = true)
            media.backendKind = BackendKind.Mpv

            // shutdown() touches stopIntegrations + backends.shutdownAll +
            // tray.stop — any of which can break. It must never throw out of
            // the app-close path, and the session reference must be cleared.
            media.shutdown()
            assertNull(media.activeBackend)
            assertEquals(BackendKind.None, media.backendKind)
        }
    }

    @Test
    fun shutdownIsIdempotent() {
        withEngine { _, media ->
            val backend = TrackingBackend()
            media.activeBackend = backend
            media.backendKind = BackendKind.Audio

            // App-close paths can race (window close + system shutdown hook) —
            // repeated shutdown must never throw or leave a stale session.
            media.shutdown()
            media.shutdown()
            media.shutdown()

            assertNull(media.activeBackend)
            assertEquals(BackendKind.None, media.backendKind)
        }
    }

    @Test
    fun shutdownClearsActiveSession() {
        withEngine { _, media ->
            media.activeBackend = TrackingBackend()
            media.backendKind = BackendKind.Audio

            media.shutdown()

            // The session reference is dropped so nothing can tick it again;
            // the backend manager's own internal backends are closed by
            // shutdownAll (its job, not the assigned active backend's).
            assertNull(media.activeBackend)
            assertEquals(BackendKind.None, media.backendKind)
        }
    }

    @Test
    fun tickAfterShutdownIsSafe() {
        withEngine { _, media ->
            media.activeBackend = TrackingBackend()
            media.backendKind = BackendKind.Audio

            media.shutdown()

            // The 10 Hz loop may tick once more before the coroutine cancels;
            // a tick after shutdown must not throw or resurrect the backend.
            media.tick()
            assertNull(media.activeBackend)
            assertEquals(BackendKind.None, media.backendKind)
        }
    }

    @Test
    fun shutdownWithoutSessionIsSafe() {
        withEngine { _, media ->
            // No backend, no session — a bare shutdown (first app close before
            // anything played) must be a no-op, not a crash.
            media.shutdown()
            assertNull(media.activeBackend)
            assertEquals(BackendKind.None, media.backendKind)
        }
    }

    /**
     * Rapid navigation (todo #120): the user can flip between the media page
     * and other screens quickly, and app-close can race any in-flight open.
     * Simulate a burst of session swaps + ticks + shutdown — the engine must
     * never throw, and after the burst the session is cleanly gone.
     */
    @Test
    fun rapidOpenCloseTickBurstIsSafe() {
        withEngine { _, media ->
            repeat(25) { iteration ->
                // Each "navigation" may attach a fresh backend (a fresh open)
                // or just tick whatever is current, then drop the session.
                val backend = TrackingBackend()
                media.activeBackend = backend
                media.backendKind = BackendKind.Audio
                media.tick()
                media.tick()
                // A hostile backend occasionally appears mid-burst to prove the
                // loop survives a broken swap too.
                if (iteration % 7 == 0) {
                    media.activeBackend = HostileBackend(isAvailable = true)
                    media.tick()
                }
                media.activeBackend = null
                media.backendKind = BackendKind.None
            }
            media.shutdown()
            assertNull(media.activeBackend)
            assertEquals(BackendKind.None, media.backendKind)
        }
    }
}
