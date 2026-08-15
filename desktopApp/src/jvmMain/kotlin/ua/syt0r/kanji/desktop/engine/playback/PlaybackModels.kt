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
    CanMute,
    CanAspectRatio,
    CanDisplayMode,
    CanVideoAdjustments,
    CanDeinterlace,
    CanAudioDelay,
    CanAudioChannel,
    CanAudioOutput,
    CanEqualizer
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
 *
 * The video/audio enhancement methods all have no-op defaults so a
 * minimal backend (the built-in audio player) compiles unchanged — the
 * UI gates every control on the corresponding [PlaybackCapability] and
 * never pretends an unsupported feature works.
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
     * How far ahead of the playhead the backend has buffered, for the
     * timeline's buffered region. Defaults to the current position
     * (no distinct buffer) — overridden by backends that know better.
     */
    fun bufferedPositionMs(): Long = currentPositionMs()

    // ---- Video rendering (display mode / adjustments) --------------
    // Defaults are no-ops; see PlaybackCapability.CanDisplayMode /
    // CanVideoAdjustments / CanDeinterlace for the gate.
    fun setDisplayMode(mode: VideoDisplayMode) = Unit
    fun setAspectRatio(preset: AspectRatioPreset) = Unit
    fun setVideoAdjustments(adjustments: VideoAdjustments) = Unit
    fun setDeinterlace(enabled: Boolean) = Unit

    // ---- Audio extras ----------------------------------------------
    fun setAudioDelay(delayMs: Long) = Unit
    fun setAudioChannel(channel: AudioChannelPreset) = Unit
    fun setAudioOutput(deviceId: String?) = Unit
    fun setEqualizer(equalizer: EqualizerSettings?) = Unit

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

// ============================================
// VIDEO DISPLAY MODES
// How the video is scaled into its container.
// Applied through the backend when supported;
// the UI gates on PlaybackCapability.CanDisplayMode.
// ============================================

enum class VideoDisplayMode(val label: String, val description: String) {
    Fit("Fit", "Whole frame visible, letterboxed to match"),
    Fill("Fill", "Fills the surface, cropping overflow"),
    Crop("Crop", "Center-cropped to the surface ratio"),
    Original("Original", "1:1 pixels, unscaled"),
    Stretch("Stretch", "Fills by distorting the aspect ratio")
}

/** Common aspect ratios — "Auto" keeps the source ratio. */
enum class AspectRatioPreset(val label: String, val value: String?) {
    Auto("Auto", null),
    R4x3("4:3", "4:3"),
    R16x9("16:9", "16:9"),
    R16x10("16:10", "16:10"),
    R21x9("21:9", "21:9"),
    R3x2("3:2", "3:2"),
    R1x1("1:1", "1:1"),
    R5x4("5:4", "5:4"),
    Square("Square", "1:1");

    companion object {
        fun fromLabel(label: String): AspectRatioPreset =
            entries.firstOrNull { it.label == label } ?: Auto
    }
}

// ============================================
// VIDEO ADJUSTMENTS
// Brightness/contrast/saturation/hue/gamma in
// normalized ranges. The UI sliders are 0..200
// with 100 = neutral; backends map to their own
// native ranges (VLC uses 0..2, mpv -100..100).
// ============================================

data class VideoAdjustments(
    /** 0..200, 100 = neutral. */
    val brightness: Float = 100f,
    /** 0..200, 100 = neutral. */
    val contrast: Float = 100f,
    /** 0..200, 100 = neutral. */
    val saturation: Float = 100f,
    /** 0..200, 100 = neutral. */
    val gamma: Float = 100f,
    /** -180..180, 0 = neutral. */
    val hue: Float = 0f,
    val deinterlace: Boolean = false
) {
    fun neutral(): Boolean =
        brightness == 100f && contrast == 100f && saturation == 100f && gamma == 100f && hue == 0f && !deinterlace

    fun withBrightness(v: Float) = copy(brightness = v.coerceIn(0f, 200f))
    fun withContrast(v: Float) = copy(contrast = v.coerceIn(0f, 200f))
    fun withSaturation(v: Float) = copy(saturation = v.coerceIn(0f, 200f))
    fun withGamma(v: Float) = copy(gamma = v.coerceIn(0f, 200f))
    fun withHue(v: Float) = copy(hue = v.coerceIn(-180f, 180f))
    fun withDeinterlace(v: Boolean) = copy(deinterlace = v)

    /** VLC-style normalized values (brightness/contrast/saturation/gamma 0..2). */
    fun vlcNeutralScale(): Pair<Float, Float> = Pair(1f, 1f)
}

// ============================================
// AUDIO CHANNEL PRESETS + OUTPUT
// ============================================

enum class AudioChannelPreset(val label: String, val vlcValue: String) {
    Stereo("Stereo", "stereo"),
    ReverseStereo("Reverse stereo", "rsterero"),
    Left("Left only", "left"),
    Right("Right only", "right"),
    Mono("Mono", "mono"),
    Headphones("Headphones", "headphones")
}

/** An audio output device exposed by the backend (VLC audio outputs). */
data class AudioOutputDevice(val id: String, val description: String)

// ============================================
// EQUALIZER
// Preamp + 10 ISO-standard bands (60 Hz … 16 kHz),
// the same layout libVLC exposes. Presets mirror
// the classic VLC equalizer preset family with
// values that are honest, real band gains (dB).
// ============================================

/** The 10 ISO center frequencies libVLC's equalizer uses. */
val EQUALIZER_BAND_FREQUENCIES_HZ = listOf(60f, 170f, 310f, 600f, 1000f, 3000f, 6000f, 12000f, 14000f, 16000f)

enum class EqualizerPreset(
    val label: String,
    val preampDb: Float,
    /** 10 band gains in dB, matching EQUALIZER_BAND_FREQUENCIES_HZ. */
    val bandsDb: List<Float>
) {
    Flat("Flat", 0f, List(10) { 0f }),
    Classical("Classical", 0f, listOf(0f, 0f, 0f, 0f, 0f, 0f, -7.2f, -7.2f, -7.2f, -9f)),
    Club("Club", 0f, listOf(0f, 0f, 8f, 5f, 5f, 5f, 0f, 0f, 0f, 0f)),
    Dance("Dance", 0f, listOf(9f, 7f, 2f, 0f, 0f, -5f, -7f, -7f, 0f, 0f)),
    FullBass("Full Bass", 0f, listOf(-8f, 9f, 9f, 5f, 1f, -4f, -8f, -10f, -11f, -11f)),
    FullBassTreble("Full Bass & Treble", 0f, listOf(7f, 5f, 0f, -7f, -7f, 0f, 5f, 8f, 9f, 11f)),
    FullTreble("Full Treble", 0f, listOf(-9f, -9f, -9f, -4f, 2f, 11f, 16f, 16f, 16f, 16f)),
    Headphones("Headphones", 0f, listOf(4f, 11f, 5f, -3f, -2f, 1f, 4f, 9f, 12f, 14f)),
    LargeHall("Large Hall", 0f, listOf(10f, 10f, 5f, 5f, 0f, -4f, -4f, -4f, 0f, 0f)),
    Live("Live", 0f, listOf(-6f, 0f, 4f, 5f, 5f, 5f, 4f, 2f, 2f, 2f)),
    Party("Party", 0f, listOf(7f, 7f, 0f, 0f, 0f, 0f, 0f, 0f, 7f, 7f)),
    Pop("Pop", -1f, listOf(-1f, 4f, 7f, 8f, 5f, 0f, -1f, -1f, -1f, -1f)),
    Reggae("Reggae", 0f, listOf(0f, 0f, 0f, -5f, 0f, 6f, 6f, 0f, 0f, 0f)),
    Rock("Rock", 8f, listOf(8f, 4f, -5f, -8f, -3f, 4f, 8f, 11f, 11f, 11f)),
    Ska("Ska", -2f, listOf(-2f, -4f, -4f, 0f, 4f, 5f, 8f, 9f, 11f, 9f)),
    Soft("Soft", 4f, listOf(4f, 1f, 0f, -2f, 0f, 4f, 8f, 9f, 11f, 12f)),
    SoftRock("Soft Rock", 4f, listOf(4f, 4f, 2f, 0f, -4f, -5f, -3f, 0f, 2f, 8f)),
    Techno("Techno", 8f, listOf(8f, 5f, 0f, -8f, 0f, 8f, 8f, 0f, 0f, 0f));

    companion object {
        fun fromLabel(label: String): EqualizerPreset =
            entries.firstOrNull { it.label == label } ?: Flat
    }
}

/** A fully-specified equalizer state: preset or custom bands. */
data class EqualizerSettings(
    val preset: EqualizerPreset = EqualizerPreset.Flat,
    val preampDb: Float = preset.preampDb,
    val bandsDb: List<Float> = preset.bandsDb
) {
    val active: Boolean get() = preampDb != 0f || bandsDb.any { it != 0f }

    fun withPreset(p: EqualizerPreset) = EqualizerSettings(preset = p, preampDb = p.preampDb, bandsDb = p.bandsDb)
    fun withPreamp(db: Float) = copy(preampDb = db.coerceIn(-20f, 20f))
    fun withBand(index: Int, db: Float): EqualizerSettings {
        val bands = bandsDb.toMutableList()
        if (index in bands.indices) bands[index] = db.coerceIn(-20f, 20f)
        return copy(bandsDb = bands)
    }

    fun normalizedBands(): List<Float> =
        (bandsDb + List(10) { 0f }).take(10)
}
