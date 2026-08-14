package ua.syt0r.kanji.desktop.engine.playback

import java.io.File

// ============================================
// KAITEYO PLAYBACK ABSTRACTION
// The media UI never talks to a concrete player
// engine. Everything goes through PlaybackBackend,
// which exposes capabilities, transport controls
// and a normalized live state. Implementations:
//   - AudioBackend (Java Sound, always available)
//   - VlcBackend   (VLCJ, when VLC is installed)
//   - MpvBackend   (mpv over IPC, when installed)
// ============================================

/** The concrete player engine behind a media session. */
enum class BackendKind { Audio, Vlc, Mpv, None }

/** What a backend can actually do — the UI reacts to this set. */
enum class PlaybackCapability {
    CanSeek,
    CanChangeSpeed,
    CanSelectSubtitle,
    CanSelectAudio,
    CanFrameStep,
    CanScreenshot,
    CanCaptureAudio,
    CanExternalSubtitles,
    CanHwAcceleration,
    CanChapters,
    CanFrameAccurateSeek,
    CanVolume,
    CanLoop,
    CanMute
}

/** Structured playback events feeding stats, history and the subtitle sync. */
enum class PlaybackEventType {
    MediaLoaded,
    MediaUnloaded,
    Started,
    Paused,
    Stopped,
    Completed,
    PositionChanged,
    Seeked,
    Buffering,
    BufferingEnded,
    SubtitleChanged,
    AudioTrackChanged,
    VideoTrackChanged,
    SpeedChanged,
    VolumeChanged,
    MutedChanged,
    ChapterChanged,
    Error
}

data class PlaybackEvent(
    val type: PlaybackEventType,
    val positionMs: Long = 0,
    val message: String = ""
)

/** A track exposed by a media file (audio, video or subtitle). */
enum class TrackKind { Video, Audio, Subtitle }

data class MediaTrackInfo(
    val id: String,
    val kind: TrackKind,
    val title: String = "",
    val language: String = ""
)

/** User-facing error categories — never raw backend exceptions. */
sealed interface PlaybackError {
    val userMessage: String

    data class FileMissing(val path: String) : PlaybackError {
        override val userMessage: String get() = "The media file could not be found:\n$path"
    }

    data class UnsupportedCodec(val detail: String) : PlaybackError {
        override val userMessage: String get() = "This file uses a codec the backend cannot decode.\n$detail"
    }

    data class BackendUnavailable(val detail: String) : PlaybackError {
        override val userMessage: String get() = "No playback backend is available.\n$detail"
    }

    data class SubtitleInvalid(val detail: String) : PlaybackError {
        override val userMessage: String get() = "The subtitle file could not be parsed.\n$detail"
    }

    data class AudioUnavailable(val detail: String) : PlaybackError {
        override val userMessage: String get() = "Audio output is unavailable.\n$detail"
    }

    data class PermissionDenied(val path: String) : PlaybackError {
        override val userMessage: String get() = "Permission denied when accessing:\n$path"
    }

    data class NetworkError(val detail: String) : PlaybackError {
        override val userMessage: String get() = "A network error occurred.\n$detail"
    }

    data class Other(val detail: String) : PlaybackError {
        override val userMessage: String get() = detail
    }
}

/**
 * Result of probing the system for an installed player engine.
 * The UI surfaces this in Settings → Media and in the player when
 * the preferred backend is missing.
 */
data class BackendProbe(
    val kind: BackendKind,
    val available: Boolean,
    val version: String = "",
    val path: String = "",
    val message: String = ""
) {
    val statusLabel: String get() = if (available) "Installed" else "Not installed"
}

/**
 * Contract every player engine implements. State is pulled via the
 * accessors (the MediaEngine owns the Compose-reactive state and polls
 * at ~10 Hz), events are pushed to [listener].
 */
interface PlaybackBackend {
    val kind: BackendKind
    val capabilities: Set<PlaybackCapability>

    /** Fired on the backend's own thread; forward to the main state. */
    var listener: ((PlaybackEvent) -> Unit)?

    /** Whether this backend is ready to open media right now. */
    val isAvailable: Boolean

    /** Optional extra detail (e.g. VLC version) for diagnostics. */
    val diagnosticName: String

    // ---- Transport -------------------------------------------------
    fun open(source: String): Result<Unit>
    fun play()
    fun pause()
    fun stop()
    fun seekTo(ms: Long)
    fun setSpeed(rate: Float)
    fun setVolume(percent: Int)
    fun setMuted(muted: Boolean)
    fun setLoop(loop: Boolean)

    // ---- Tracks ----------------------------------------------------
    fun availableTracks(): List<MediaTrackInfo>
    fun selectTrack(trackId: String?)
    fun setSubtitleDelay(delayMs: Long)

    // ---- Advanced --------------------------------------------------
    fun frameStepForward(): Boolean
    fun frameStepBackward(): Boolean
    fun snapshot(target: File): Result<String>
    fun chapters(): List<PlaybackChapter>

    // ---- Live state ------------------------------------------------
    fun currentPositionMs(): Long
    fun durationMs(): Long
    val isPlaying: Boolean
    val isBuffering: Boolean

    /**
     * Optional performance profile hint (battery / balanced / quality).
     * Backends that support hardware-acceleration / renderer tuning apply
     * it; the rest treat it as a no-op.
     */
    fun setPerformanceProfile(profile: String) = Unit

    fun close()
}

data class PlaybackChapter(val id: String, val title: String, val startMs: Long)

/** Render surface placeholder — backends that render their own window report this. */
enum class SurfaceMode { Embedded, ExternalWindow, None }

/**
 * Describes how a backend presents video so the UI knows whether to mount
 * an embedded canvas or show a hint that playback happens in its own window.
 */
interface SurfaceProvider {
    val surfaceMode: SurfaceMode
}
