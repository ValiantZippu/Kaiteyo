package ua.syt0r.kanji.desktop.engine.playback

import java.awt.Component
import java.io.File
import javax.swing.SwingUtilities

// ============================================
// VLC BACKEND
// Real VLC playback through VLCJ, embedded
// directly in the Kaiteyo workspace. Requires a
// VLC installation with libvlc available — the
// BackendManager probes for it and the UI shows
// install guidance when it is missing. The AWT
// surface is hosted by a Compose SwingPanel.
// VLCJ is GPL-3.0; Kaiteyo is GPL-3.0.
// ============================================

class VlcBackend(
    private val libDir: String? = null,
    private val vlcPath: String? = null
) : PlaybackBackend, SurfaceProvider {

    override val kind: BackendKind = BackendKind.Vlc
    override val capabilities: Set<PlaybackCapability> = setOf(
        PlaybackCapability.CanSeek,
        PlaybackCapability.CanFrameAccurateSeek,
        PlaybackCapability.CanChangeSpeed,
        PlaybackCapability.CanSelectSubtitle,
        PlaybackCapability.CanSelectAudio,
        PlaybackCapability.CanVolume,
        PlaybackCapability.CanMute,
        PlaybackCapability.CanScreenshot,
        PlaybackCapability.CanExternalSubtitles,
        PlaybackCapability.CanHwAcceleration,
        PlaybackCapability.CanChapters,
        PlaybackCapability.CanLoop,
        PlaybackCapability.CanAspectRatio,
        PlaybackCapability.CanDisplayMode,
        PlaybackCapability.CanVideoAdjustments,
        PlaybackCapability.CanDeinterlace,
        PlaybackCapability.CanAudioDelay,
        PlaybackCapability.CanAudioChannel,
        PlaybackCapability.CanAudioOutput,
        PlaybackCapability.CanEqualizer
    )
    override var listener: ((PlaybackEvent) -> Unit)? = null
    override val isAvailable: Boolean get() = runCatching {
        Class.forName("uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent")
        true
    }.getOrDefault(false)

    override val surfaceMode: SurfaceMode = SurfaceMode.Embedded

    private var component: uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent? = null
    private var initialized = false
    private var loadedPath: String? = null

    @Volatile private var positionMs: Long = 0
    @Volatile private var durationMs: Long = 0
    @Volatile private var playing: Boolean = false
    @Volatile private var buffering: Boolean = false

    private val diagnostics: String by lazy {
        runCatching {
            val f = uk.co.caprica.vlcj.factory.MediaPlayerFactory()
            val version = f.application().version()
            f.release()
            version
        }.getOrDefault("VLC")
    }

    override val diagnosticName: String get() = "VLC $diagnostics${vlcPath?.let { " ($it)" } ?: ""}"

    /** The AWT surface for the Compose SwingPanel — created on the EDT. */
    fun ensureComponent(): Component? {
        if (component != null) return component
        return runCatching {
            libDir?.let { dir ->
                if (File(dir).isDirectory) {
                    System.setProperty("jna.library.path", dir)
                }
            }
            var created: uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent? = null
            if (SwingUtilities.isEventDispatchThread()) {
                created = uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent()
            } else {
                SwingUtilities.invokeAndWait {
                    created = uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent()
                }
            }
            component = created
            initialized = true
            bindEvents(created!!)
            created
        }.getOrNull()
    }

    private fun bindEvents(cmp: uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent) {
        val mp = cmp.mediaPlayer()
        mp.events().addMediaPlayerEventListener(object : uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer) {
                playing = true
                buffering = false
                listener?.invoke(PlaybackEvent(PlaybackEventType.Started, positionMs = positionMs))
            }

            override fun paused(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer) {
                playing = false
                listener?.invoke(PlaybackEvent(PlaybackEventType.Paused, positionMs = positionMs))
            }

            override fun stopped(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer) {
                playing = false
                listener?.invoke(PlaybackEvent(PlaybackEventType.Stopped, positionMs = positionMs))
            }

            override fun finished(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer) {
                playing = false
                listener?.invoke(PlaybackEvent(PlaybackEventType.Completed, positionMs = durationMs))
            }

            override fun error(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer) {
                listener?.invoke(PlaybackEvent(PlaybackEventType.Error, message = "VLC reported a playback error"))
            }

            override fun timeChanged(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer, newTime: Long) {
                positionMs = newTime
                listener?.invoke(PlaybackEvent(PlaybackEventType.PositionChanged, positionMs = newTime))
            }

            override fun lengthChanged(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer, newLength: Long) {
                durationMs = newLength
            }

            override fun buffering(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer, newCache: Float) {
                buffering = newCache < 100f
                listener?.invoke(
                    if (buffering) PlaybackEvent(PlaybackEventType.Buffering)
                    else PlaybackEvent(PlaybackEventType.BufferingEnded)
                )
            }

            override fun mediaPlayerReady(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer) {
                durationMs = mediaPlayer.status().length()
                listener?.invoke(PlaybackEvent(PlaybackEventType.MediaLoaded))
            }

            override fun elementaryStreamSelected(
                mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer,
                trackType: uk.co.caprica.vlcj.media.TrackType,
                newTrackId: Int
            ) {
                val type = when (trackType) {
                    uk.co.caprica.vlcj.media.TrackType.AUDIO -> PlaybackEventType.AudioTrackChanged
                    uk.co.caprica.vlcj.media.TrackType.TEXT -> PlaybackEventType.SubtitleChanged
                    else -> PlaybackEventType.VideoTrackChanged
                }
                listener?.invoke(PlaybackEvent(type, positionMs = positionMs))
            }

            override fun chapterChanged(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer, newChapter: Int) {
                listener?.invoke(PlaybackEvent(PlaybackEventType.ChapterChanged, positionMs = positionMs))
            }
        })
    }

    override fun open(source: String): Result<Unit> = runCatching {
        ensureComponent() ?: error("VLC native libraries not found — install VLC to use this backend")
        loadedPath = source
        val media = if (source.startsWith("http://") || source.startsWith("https://")) source else File(source).absolutePath
        component?.mediaPlayer()?.media()?.play(media) ?: error("VLC player not initialized")
        durationMs = 0
        positionMs = 0
    }

    override fun play() {
        val mp = component?.mediaPlayer() ?: return
        mp.controls().play()
    }

    override fun pause() {
        val mp = component?.mediaPlayer() ?: return
        mp.controls().pause()
    }

    override fun stop() {
        val mp = component?.mediaPlayer() ?: return
        mp.controls().stop()
    }

    override fun seekTo(ms: Long) {
        val mp = component?.mediaPlayer() ?: return
        mp.controls().setTime(ms.coerceAtLeast(0))
        listener?.invoke(PlaybackEvent(PlaybackEventType.Seeked, positionMs = ms))
    }

    override fun setSpeed(rate: Float) {
        val mp = component?.mediaPlayer() ?: return
        mp.controls().setRate(rate.coerceIn(0.25f, 4f))
        listener?.invoke(PlaybackEvent(PlaybackEventType.SpeedChanged))
    }

    override fun setVolume(percent: Int) {
        val mp = component?.mediaPlayer() ?: return
        mp.audio().setVolume(percent.coerceIn(0, 100))
        listener?.invoke(PlaybackEvent(PlaybackEventType.VolumeChanged))
    }

    override fun setMuted(muted: Boolean) {
        val mp = component?.mediaPlayer() ?: return
        mp.audio().setMute(muted)
        listener?.invoke(PlaybackEvent(PlaybackEventType.MutedChanged))
    }

    override fun setLoop(loop: Boolean) {
        val mp = component?.mediaPlayer() ?: return
        mp.controls().setRepeat(loop)
    }

    override fun availableTracks(): List<MediaTrackInfo> {
        val mp = component?.mediaPlayer() ?: return emptyList()
        return runCatching {
            buildList {
                mp.video().trackDescriptions().forEach { d ->
                    add(MediaTrackInfo(id = d.id().toString(), kind = TrackKind.Video, title = d.description()))
                }
                mp.audio().trackDescriptions().forEach { d ->
                    add(MediaTrackInfo(id = d.id().toString(), kind = TrackKind.Audio, title = d.description()))
                }
                mp.subpictures().trackDescriptions().forEach { d ->
                    add(MediaTrackInfo(id = d.id().toString(), kind = TrackKind.Subtitle, title = d.description()))
                }
            }
        }.getOrDefault(emptyList())
    }

    override fun selectTrack(trackId: String?) {
        val mp = component?.mediaPlayer() ?: return
        val id = trackId?.toIntOrNull() ?: return
        val track = availableTracks().firstOrNull { it.id == trackId }
        when (track?.kind) {
            TrackKind.Video -> mp.video().setTrack(id)
            TrackKind.Audio -> mp.audio().setTrack(id)
            TrackKind.Subtitle -> mp.subpictures().setTrack(id)
            else -> Unit
        }
    }

    override fun setSubtitleDelay(delayMs: Long) {
        val mp = component?.mediaPlayer() ?: return
        mp.subpictures().setDelay(delayMs)
    }

    // ------------------------------------------------------------
    // Video rendering (display mode / aspect / adjustments)
    // ------------------------------------------------------------

    override fun setDisplayMode(mode: VideoDisplayMode) {
        val mp = component?.mediaPlayer() ?: return
        runCatching {
            when (mode) {
                VideoDisplayMode.Fit -> {
                    mp.video().setScale(0f)
                    mp.video().setCropGeometry("")
                }
                VideoDisplayMode.Fill -> {
                    mp.video().setScale(0f)
                    mp.video().setCropGeometry("")
                    mp.video().setAspectRatio("16:9")
                }
                VideoDisplayMode.Crop -> {
                    mp.video().setScale(0f)
                    mp.video().setCropGeometry("16:9")
                }
                VideoDisplayMode.Original -> {
                    mp.video().setCropGeometry("")
                    mp.video().setScale(1f)
                }
                VideoDisplayMode.Stretch -> {
                    mp.video().setScale(0f)
                    mp.video().setCropGeometry("")
                    mp.video().setAspectRatio("")
                }
            }
        }
    }

    override fun setAspectRatio(preset: AspectRatioPreset) {
        val mp = component?.mediaPlayer() ?: return
        runCatching { mp.video().setAspectRatio(preset.value ?: "") }
    }

    override fun setVideoAdjustments(adjustments: VideoAdjustments) {
        val mp = component?.mediaPlayer() ?: return
        runCatching {
            if (adjustments.neutral() && !adjustments.deinterlace) {
                mp.video().setAdjustVideo(false)
            } else {
                mp.video().setAdjustVideo(true)
                // VLC expects 0..2 with 1 = neutral; our sliders are 0..200/100.
                mp.video().setBrightness(adjustments.brightness / 100f)
                mp.video().setContrast(adjustments.contrast / 100f)
                mp.video().setSaturation(adjustments.saturation / 100f)
                mp.video().setGamma(adjustments.gamma / 100f)
                mp.video().setHue(adjustments.hue)
            }
            setDeinterlace(adjustments.deinterlace)
        }
    }

    override fun setDeinterlace(enabled: Boolean) {
        val mp = component?.mediaPlayer() ?: return
        runCatching {
            mp.video().setDeinterlace(
                if (enabled) uk.co.caprica.vlcj.player.base.DeinterlaceMode.YADIF
                else uk.co.caprica.vlcj.player.base.DeinterlaceMode.DISCARD
            )
        }
    }

    // ------------------------------------------------------------
    // Audio extras (delay / channel / output / equalizer)
    // ------------------------------------------------------------

    override fun setAudioDelay(delayMs: Long) {
        val mp = component?.mediaPlayer() ?: return
        runCatching { mp.audio().setDelay(delayMs) }
    }

    override fun setAudioChannel(channel: AudioChannelPreset) {
        val mp = component?.mediaPlayer() ?: return
        runCatching {
            val mapped = when (channel) {
                AudioChannelPreset.Stereo -> uk.co.caprica.vlcj.player.base.AudioChannel.STEREO
                AudioChannelPreset.ReverseStereo -> uk.co.caprica.vlcj.player.base.AudioChannel.RSTEREO
                AudioChannelPreset.Left -> uk.co.caprica.vlcj.player.base.AudioChannel.LEFT
                AudioChannelPreset.Right -> uk.co.caprica.vlcj.player.base.AudioChannel.RIGHT
                AudioChannelPreset.Mono -> uk.co.caprica.vlcj.player.base.AudioChannel.MONO
                AudioChannelPreset.Headphones -> uk.co.caprica.vlcj.player.base.AudioChannel.HEADPHONES
            }
            mp.audio().setChannel(mapped)
        }
    }

    override fun setAudioOutput(deviceId: String?) {
        val mp = component?.mediaPlayer() ?: return
        runCatching {
            if (deviceId == null) mp.audio().setOutput("")
            else mp.audio().setOutput(deviceId)
        }
    }

    override fun setEqualizer(equalizer: EqualizerSettings?) {
        val mp = component?.mediaPlayer() ?: return
        runCatching {
            if (equalizer == null || !equalizer.active) {
                mp.audio().setEqualizer(null)
                return@runCatching
            }
            val eq = uk.co.caprica.vlcj.player.base.Equalizer(EQUALIZER_BAND_FREQUENCIES_HZ.size)
            eq.setPreamp(equalizer.preampDb)
            equalizer.normalizedBands().forEachIndexed { index, db -> eq.setAmp(index, db) }
            mp.audio().setEqualizer(eq)
        }
    }

    override fun frameStepForward(): Boolean {
        val mp = component?.mediaPlayer() ?: return false
        val wasPlaying = playing
        if (wasPlaying) mp.controls().pause()
        mp.controls().nextFrame()
        return true
    }

    override fun frameStepBackward(): Boolean = false

    override fun snapshot(target: File): Result<String> = runCatching {
        val mp = component?.mediaPlayer() ?: error("VLC not initialized")
        target.parentFile?.mkdirs()
        val ok = mp.snapshots().save(target)
        if (!ok) error("VLC could not capture a snapshot")
        target.absolutePath
    }

    override fun chapters(): List<PlaybackChapter> {
        val mp = component?.mediaPlayer() ?: return emptyList()
        return runCatching {
            mp.chapters().descriptions().mapIndexed { i, d ->
                PlaybackChapter("ch-$i", d.name(), d.offset())
            }
        }.getOrDefault(emptyList())
    }

    override fun currentPositionMs(): Long = positionMs

    override fun durationMs(): Long = durationMs

    override val isPlaying: Boolean get() = playing

    override val isBuffering: Boolean get() = buffering

    override fun close() {
        runCatching { component?.mediaPlayer()?.controls()?.stop() }
        runCatching { component?.release() }
        component = null
        initialized = false
        loadedPath = null
        playing = false
        buffering = false
        listener?.invoke(PlaybackEvent(PlaybackEventType.MediaUnloaded))
    }
}
