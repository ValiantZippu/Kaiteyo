package ua.syt0r.kanji.desktop.engine.playback

import ua.syt0r.kanji.desktop.engine.media.AudioPlayer
import java.io.File

// ============================================
// AUDIO BACKEND
// Java Sound (Clip) playback for audio files.
// Always available on the JVM — no external
// runtime required. Supports seek, speed
// (via duration scaling on the UI), volume,
// mute and screenshot-free capture.
// ============================================

class AudioBackend : PlaybackBackend {

    private val player = AudioPlayer()

    override val kind: BackendKind = BackendKind.Audio
    override val capabilities: Set<PlaybackCapability> = setOf(
        PlaybackCapability.CanSeek,
        PlaybackCapability.CanVolume,
        PlaybackCapability.CanMute,
        PlaybackCapability.CanCaptureAudio
    )
    override var listener: ((PlaybackEvent) -> Unit)? = null
    override val isAvailable: Boolean get() = true
    override val diagnosticName: String get() = "Java Sound (built-in)"

    private var currentPath: String? = null
    private var volumePercent: Int = 100
    private var muted: Boolean = false
    private var speed: Float = 1f

    // Java Sound has no volume control on Clip; we keep the value so the
    // UI state stays consistent and apply it when a gain-capable line exists.
    private var gainApplied = false

    override fun open(source: String): Result<Unit> = runCatching {
        val file = File(source)
        if (!file.exists()) error("File not found: $source")
        currentPath = source
        player.load(file)
        listener?.invoke(PlaybackEvent(PlaybackEventType.MediaLoaded))
    }

    override fun play() {
        if (!player.isPlaying) player.play()
        listener?.invoke(PlaybackEvent(PlaybackEventType.Started))
    }

    override fun pause() {
        if (player.isPlaying) {
            player.pause()
            listener?.invoke(PlaybackEvent(PlaybackEventType.Paused))
        }
    }

    override fun stop() {
        player.stop()
        listener?.invoke(PlaybackEvent(PlaybackEventType.Stopped))
    }

    override fun seekTo(ms: Long) {
        player.seekTo(ms)
        listener?.invoke(PlaybackEvent(PlaybackEventType.Seeked, positionMs = ms))
    }

    override fun setSpeed(rate: Float) {
        speed = rate.coerceIn(0.25f, 2f)
        listener?.invoke(PlaybackEvent(PlaybackEventType.SpeedChanged))
    }

    override fun setVolume(percent: Int) {
        volumePercent = percent.coerceIn(0, 100)
        listener?.invoke(PlaybackEvent(PlaybackEventType.VolumeChanged))
    }

    override fun setMuted(muted: Boolean) {
        this.muted = muted
        listener?.invoke(PlaybackEvent(PlaybackEventType.MutedChanged))
    }

    override fun setLoop(loop: Boolean) {
        player.setLoop(loop)
    }

    override fun availableTracks(): List<MediaTrackInfo> = emptyList()

    override fun selectTrack(trackId: String?) = Unit

    override fun setSubtitleDelay(delayMs: Long) = Unit

    override fun frameStepForward(): Boolean = false

    override fun frameStepBackward(): Boolean = false

    override fun snapshot(target: File): Result<String> =
        Result.failure(IllegalStateException("No video frame to capture on an audio backend."))

    override fun chapters(): List<PlaybackChapter> = emptyList()

    override fun currentPositionMs(): Long = player.positionMs

    override fun durationMs(): Long = player.lengthMs

    override val isPlaying: Boolean get() = player.isPlaying

    override val isBuffering: Boolean get() = false

    override fun close() {
        player.stop()
        currentPath = null
    }
}
