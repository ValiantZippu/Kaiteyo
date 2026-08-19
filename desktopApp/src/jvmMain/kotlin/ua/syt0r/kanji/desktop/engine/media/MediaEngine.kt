package ua.syt0r.kanji.desktop.engine.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.engine.dictionary.JapaneseSegmenter
import ua.syt0r.kanji.desktop.engine.dictionary.SegmentToken
import ua.syt0r.kanji.desktop.engine.dictionary.WordStatus
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.mining.MiningPayload
import ua.syt0r.kanji.desktop.engine.playback.AspectRatioPreset
import ua.syt0r.kanji.desktop.engine.playback.AudioChannelPreset
import ua.syt0r.kanji.desktop.engine.playback.BackendKind
import ua.syt0r.kanji.desktop.engine.playback.BackendManager
import ua.syt0r.kanji.desktop.engine.playback.BackendProbe
import ua.syt0r.kanji.desktop.engine.playback.EqualizerPreset
import ua.syt0r.kanji.desktop.engine.playback.EqualizerSettings
import ua.syt0r.kanji.desktop.engine.playback.MediaTrackInfo
import ua.syt0r.kanji.desktop.engine.playback.PlaybackBackend
import ua.syt0r.kanji.desktop.engine.playback.PlaybackCapability
import ua.syt0r.kanji.desktop.engine.playback.PlaybackChapter
import ua.syt0r.kanji.desktop.engine.playback.PlaybackError
import ua.syt0r.kanji.desktop.engine.playback.PlaybackEvent
import ua.syt0r.kanji.desktop.engine.playback.PlaybackEventType
import ua.syt0r.kanji.desktop.engine.playback.TrackKind
import ua.syt0r.kanji.desktop.engine.playback.VideoAdjustments
import ua.syt0r.kanji.desktop.engine.playback.VideoDisplayMode
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.ToastKind
import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

// ============================================
// KAITEYO MEDIA WORKSPACE ENGINE
// The immersion hub. Owns the playback backend,
// subtitle engine, media library and capture
// utilities, and exposes a single reactive state
// the workspace UI binds to. Every transport
// action goes through a PlaybackBackend; every
// mined sentence flows into the standard card
// pipeline (cards → SRS → statistics).
//
//   MEDIA → SUBTITLES → TEXT → DICTIONARY
//        → UNDERSTANDING → MINING → CARD → SRS
// ============================================

/** A timestamped bookmark saved while watching/listening. */
@Serializable
data class MediaBookmark(
    val id: String,
    val mediaPath: String,
    val timestampMs: Long,
    val label: String = "",
    val note: String = "",
    val createdAt: String = ""
)

/** A saved audio clip with optional sentence context. */
@Serializable
data class AudioClip(
    val id: String,
    val sourcePath: String,
    val label: String = "",
    val startMs: Long = 0,
    val endMs: Long = 0,
    val exportedPath: String = "",
    val createdAt: String = ""
)

/** Backward-compatible lightweight document descriptor. */
@Serializable
data class MediaDocument(
    val path: String,
    val name: String,
    val kind: MediaKind,
    val sizeBytes: Long = 0
) {
    val displayName: String get() = name
}

@Serializable
private data class MediaStateDto(
    val bookmarks: List<MediaBookmark> = emptyList(),
    val clips: List<AudioClip> = emptyList(),
    val recentFiles: List<String> = emptyList(),
    val miningEvents: List<MediaMiningEvent> = emptyList()
)

/** Persisted play queue — ids + cursor, resolved against the library on load. */
@Serializable
private data class PlaybackQueueDto(
    val ids: List<String> = emptyList(),
    val index: Int = -1
)

/** A card mined from media — keeps the exact moment so it can be re-opened. */
@Serializable
data class MediaMiningEvent(
    val cardId: String,
    val mediaPath: String,
    val mediaName: String,
    val timestampMs: Long,
    val cueText: String = "",
    val createdAt: String = ""
)

/** Vocabulary coverage breakdown for a text or an entire subtitle track. */
data class MediaCoverageStats(
    val totalTokens: Int,
    val known: Int,
    val learning: Int,
    val unknown: Int,
    val mined: Int,
    val suspended: Int
) {
    /** Share of tokens that are known or in progress (0..1). */
    val coverage: Float
        get() = if (totalTokens == 0) 0f else (known + learning).toFloat() / totalTokens
}

/** Join selected tokens back into the exact original text (offset order). */
internal fun joinTokenSurfaces(tokens: List<SegmentToken>): String =
    tokens.sortedBy { it.offset }.joinToString("") { it.surface }

/** Live player state broadcast to WebSocket clients. */
@Serializable
data class PlayerStateSnapshot(
    val media: String = "",
    val path: String = "",
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val speed: Float = 1f,
    val backend: String = "",
    val subtitle: String = "",
    val subtitleStartMs: Long = 0,
    val subtitleEndMs: Long = 0,
    val selectedToken: String = "",
    val lookupQuery: String = "",
    val minedCount: Int = 0,
    val textHookRunning: Boolean = false,
    val wsClients: Int = 0
)

/** Subtitle text annotation styles. */
enum class AnnotationMode(val label: String) {
    Off("Plain"),
    Reading("Reading"),
    Status("Word status"),
    Frequency("Frequency")
}

/** What should happen automatically around subtitle cues. */
enum class AutoPauseMode(val label: String) {
    Off("Off"),
    AtCueStart("Pause at cue start"),
    AtCueEnd("Pause at cue end"),
    BeforeCue("Pause before cue")
}

/** Loop behaviour for subtitles / ranges. */
enum class LoopMode(val label: String) {
    Off("Off"),
    CurrentCue("Loop current cue"),
    Range("Loop A–B range")
}

/** JVM audio player wrapping a Clip with pause/resume/seeking/looping. */
class AudioPlayer {
    private var clip: Clip? = null
    private var pausedAtMs: Long = 0
    private var loopEnabled: Boolean = false

    val isPlaying: Boolean get() = clip?.isRunning == true

    /** Continuous replay (used by the media loop feature). */
    fun setLoop(enabled: Boolean) {
        loopEnabled = enabled
        clip?.loop(if (enabled) Clip.LOOP_CONTINUOUSLY else 0)
    }

    fun load(file: File): Result<Unit> = runCatching {
        stop()
        val stream = AudioSystem.getAudioInputStream(file)
        clip = AudioSystem.getClip().also { c ->
            c.open(stream)
            c.addLineListener { ev ->
                if (ev.type == LineEvent.Type.STOP && ev.line is Clip) {
                    val cl = ev.line as Clip
                    if (cl.framePosition >= cl.frameLength) {
                        pausedAtMs = 0
                    }
                }
            }
        }
    }

    fun play() {
        val c = clip ?: return
        if (pausedAtMs > 0) {
            c.microsecondPosition = pausedAtMs
            pausedAtMs = 0
        }
        c.start()
    }

    fun pause() {
        val c = clip ?: return
        if (c.isRunning) {
            pausedAtMs = c.microsecondPosition
            c.stop()
        }
    }

    fun stop() {
        loopEnabled = false
        clip?.stop()
        clip?.close()
        clip = null
        pausedAtMs = 0
    }

    fun seekTo(ms: Long) {
        val c = clip ?: return
        val pos = ms.coerceIn(0, c.microsecondLength / 1000) * 1000
        c.microsecondPosition = pos
        pausedAtMs = 0
    }

    val positionMs: Long
        get() = if (pausedAtMs > 0) pausedAtMs
        else clip?.microsecondPosition?.div(1000) ?: 0

    val lengthMs: Long
        get() = clip?.microsecondLength?.div(1000) ?: 0
}

/**
 * The central media engine. Constructed with the AppState so mining,
 * statistics and the activity log stay wired to the rest of Kaiteyo.
 */
class MediaEngine(private val state: AppState) {

    // ------------------------------------------------------------
    // Sub-engines
    // ------------------------------------------------------------
    val subtitles = SubtitleEngine()
    val library = MediaLibrary()
    val scanner = MediaScanner(library)
    val backends = BackendManager()

    /**
     * Library-wide subtitle index: every subtitle file associated with any
     * library item, so a Japanese word can be found across ALL media (not
     * just the loaded episode) and opened at its timestamp.
     */
    val subtitleSearchIndex = SubtitleSearchIndex(library)

    // ---- Immersion analytics -------------------------------------------
    val statistics = MediaStatisticsStore()

    // ---- Configurable media hotkeys (Media → Settings → Keyboard shortcuts)
    val hotkeys = MediaHotkeys()

    // ------------------------------------------------------------
    // Playback state
    // ------------------------------------------------------------
    var activeBackend by mutableStateOf<PlaybackBackend?>(null)
    var backendKind by mutableStateOf(BackendKind.None)
    var currentItem by mutableStateOf<MediaItem?>(null)
    var isPlaying by mutableStateOf(false)
    var positionMs by mutableStateOf(0L)
    var durationMs by mutableStateOf(0L)
    var speed by mutableStateOf(1f)
    var volume by mutableStateOf(100)
    var muted by mutableStateOf(false)
    var buffering by mutableStateOf(false)
    var playbackError by mutableStateOf<PlaybackError?>(null)

    // ---- Video rendering (display mode / aspect / adjustments) ------
    var displayMode by mutableStateOf(VideoDisplayMode.Fit)
    var aspectRatio by mutableStateOf(AspectRatioPreset.Auto)
    var videoAdjustments by mutableStateOf(VideoAdjustments())

    /** How far ahead of the playhead the backend has buffered (timeline region). */
    var bufferedPositionMs by mutableStateOf(0L)

    // ---- Audio extras ----------------------------------------------
    /** Subtitle timing offset in ms (+ later, − earlier). */
    var subtitleDelayMs by mutableStateOf(0L)
    var audioDelayMs by mutableStateOf(0L)
    var audioChannel by mutableStateOf(AudioChannelPreset.Stereo)
    var audioOutputId by mutableStateOf<String?>(null)
    var equalizer by mutableStateOf(EqualizerSettings())
    var lastScreenshotPath by mutableStateOf<String?>(null)
    var lastAudioClipPath by mutableStateOf<String?>(null)
    var lastVideoClipPath by mutableStateOf<String?>(null)
    var lastMinedPayload by mutableStateOf<MiningPayload?>(null)

    val currentDocument: MediaDocument?
        get() = currentItem?.let { MediaDocument(it.path, it.name, it.kind, it.sizeBytes) }

    // ------------------------------------------------------------
    // Subtitle UI state
    // ------------------------------------------------------------
    var subtitleVisible by mutableStateOf(true)
    var annotationMode by mutableStateOf(AnnotationMode.Status)
    var activeCue by mutableStateOf<SubtitleCue?>(null)
    var activeCueIndex by mutableStateOf(-1)
    var secondaryCue by mutableStateOf<SubtitleCue?>(null)

    // ------------------------------------------------------------
    // Lookup / selection state
    // ------------------------------------------------------------
    var selectedTokens by mutableStateOf<List<SegmentToken>>(emptyList())
    private var selectionAnchor = -1 // token index anchor for shift/drag extension
    var tokenCycleIndex by mutableStateOf(-1)
    var lookupQuery by mutableStateOf<String?>(null)
    var lookupPosition by mutableStateOf<androidx.compose.ui.geometry.Offset?>(null)

    // ------------------------------------------------------------
    // Workspace UI state
    // ------------------------------------------------------------
    var transcriptOpen by mutableStateOf(true)
    var dictionaryOpen by mutableStateOf(false)
    var libraryOpen by mutableStateOf(false)
    var settingsOpen by mutableStateOf(false)
    var controlsVisible by mutableStateOf(true)
    var fullscreenActive by mutableStateOf(false)

    /** Cinema mode hides the surrounding chrome for a distraction-free screen. */
    var cinemaMode by mutableStateOf(false)

    /** Search box bound to the library panel — usable straight from the player. */
    var librarySearchQuery by mutableStateOf("")

    /** Flip cinema mode; side panels close so the player owns the screen. */
    fun toggleCinemaMode() {
        cinemaMode = !cinemaMode
        if (cinemaMode) {
            transcriptOpen = false
            dictionaryOpen = false
            settingsOpen = false
        }
    }

    /** True while a text field is focused — immersion hotkeys stand down. */
    var textInputFocused by mutableStateOf(false)

    // ------------------------------------------------------------
    // Playback modes
    // ------------------------------------------------------------
    var condensedPlayback by mutableStateOf(false)

    /** Fast-forward (rather than jump) through unsubtitled gaps. */
    var condensedFastForward by mutableStateOf(false)
    var autoPauseMode by mutableStateOf(AutoPauseMode.Off)
    var loopMode by mutableStateOf(LoopMode.Off)
    var loopStartMs by mutableStateOf(0L)
    var loopEndMs by mutableStateOf(0L)
    var replayRemaining by mutableStateOf(0)
    var studyMode by mutableStateOf(false)
    var seekAmountMs by mutableStateOf(5000L)

    // ------------------------------------------------------------
    // Mini player — playback continues outside the Media workspace
    // ------------------------------------------------------------
    var miniPlayerEnabled by mutableStateOf(false)
    var miniPlayerOpen by mutableStateOf(false)

    // ------------------------------------------------------------
    // Resume prompt
    // ------------------------------------------------------------
    var resumePromptEnabled by mutableStateOf(true)
    var resumePromptPending by mutableStateOf(false)
    var pendingResumeMs by mutableStateOf(0L)

    // ------------------------------------------------------------
    // Play queue — the library's "up next" ordering, including
    // automatic next-episode resolution when the queue is empty.
    // ------------------------------------------------------------
    val playQueue = mutableStateListOf<MediaItem>()
    var queueIndex by mutableStateOf(-1)

    /** The item the queue is currently positioned on, if any. */
    val currentQueueItem: MediaItem?
        get() = playQueue.getOrNull(queueIndex)

    fun addToQueue(item: MediaItem) {
        if (playQueue.none { it.id == item.id }) playQueue.add(item)
        if (queueIndex == -1) queueIndex = 0
        persistQueue()
        state.toastHost.show("\"${item.name}\" added to queue", kind = ToastKind.Info)
    }

    /** Replace the queue with a playlist's items and start playing from [startId]. */
    fun playPlaylist(playlist: MediaPlaylist, startId: String? = null) {
        val items = library.playlistItems(playlist)
        if (items.isEmpty()) {
            state.toastHost.show("Playlist is empty", kind = ToastKind.Info)
            return
        }
        playQueue.clear()
        playQueue.addAll(items)
        queueIndex = items.indexOfFirst { it.id == startId }.coerceAtLeast(0)
        persistQueue()
        openItem(playQueue[queueIndex])
        state.toastHost.show("Playing playlist \"${playlist.name}\"", kind = ToastKind.Info)
    }

    /** Replace the queue with a shuffled playlist and start playing from the top. */
    fun playShuffled(playlist: MediaPlaylist) {
        val items = library.playlistItems(playlist).shuffled()
        if (items.isEmpty()) {
            state.toastHost.show("Playlist is empty", kind = ToastKind.Info)
            return
        }
        playQueue.clear()
        playQueue.addAll(items)
        queueIndex = 0
        persistQueue()
        openItem(playQueue[0])
        state.toastHost.show("Playing \"${playlist.name}\" shuffled (${items.size} items)", kind = ToastKind.Info)
    }

    /** Queue a whole playlist after the current position (no auto-play). */
    fun queuePlaylist(playlist: MediaPlaylist) {
        val items = library.playlistItems(playlist)
        if (items.isEmpty()) return
        val before = playQueue.size
        items.forEach { if (playQueue.none { q -> q.id == it.id }) playQueue.add(it) }
        if (queueIndex == -1) queueIndex = before
        persistQueue()
        state.toastHost.show("\"${playlist.name}\" queued (${items.size} items)", kind = ToastKind.Info)
    }

    fun removeFromQueue(index: Int) {
        if (index !in playQueue.indices) return
        playQueue.removeAt(index)
        if (queueIndex >= index) queueIndex = (queueIndex - 1).coerceAtLeast(-1)
        persistQueue()
    }

    fun clearQueue() {
        playQueue.clear()
        queueIndex = -1
        persistQueue()
        state.toastHost.show("Play queue cleared", kind = ToastKind.Info)
    }

    /** Persist the queue so a binge session survives a restart (ids + cursor). */
    private fun persistQueue() {
        runCatching {
            File(System.getProperty("user.home"), ".kaiteyo/media/queue.json").writeText(
                json.encodeToString(PlaybackQueueDto(playQueue.map { it.id }, queueIndex))
            )
        }
    }

    /** Restore the queue (ids only — items resolve against the library). */
    private fun loadQueue() {
        val f = File(System.getProperty("user.home"), ".kaiteyo/media/queue.json")
        if (!f.exists()) return
        runCatching {
            val dto = json.decodeFromString<PlaybackQueueDto>(f.readText())
            playQueue.clear()
            dto.ids.mapNotNull { library.item(it) }.forEach { playQueue.add(it) }
            queueIndex = dto.index.coerceIn(-1, playQueue.lastIndex)
        }
    }

    /**
     * What should play when this episode ends: the next queued item, or the
     * next episode of the same series (same folder + base name), or nothing.
     */
    fun resolveNextUp(): MediaItem? {
        if (queueIndex in playQueue.indices && queueIndex < playQueue.lastIndex) {
            return playQueue[queueIndex + 1]
        }
        return currentItem?.let { library.nextEpisode(it) }
    }

    /** Advance the queue to the next item and play it (queue-aware next). */
    fun playNext() {
        if (queueIndex in playQueue.indices && queueIndex < playQueue.lastIndex) {
            queueIndex += 1
            openItem(playQueue[queueIndex])
            persistQueue()
        } else {
            val next = currentItem?.let { library.nextEpisode(it) }
            if (next != null) {
                state.toastHost.show("Auto-advancing to ${next.episode.ifBlank { next.name }}", kind = ToastKind.Info)
                openItem(next)
            } else {
                state.toastHost.show("No next item in the queue or series", kind = ToastKind.Info)
            }
        }
    }

    /** Step back through the queue (queue-aware previous). */
    fun playPrevious() {
        if (queueIndex > 0 && queueIndex in playQueue.indices) {
            queueIndex -= 1
            openItem(playQueue[queueIndex])
            persistQueue()
        } else {
            state.toastHost.show("Already at the start of the queue", kind = ToastKind.Info)
        }
    }

    // ------------------------------------------------------------
    // End-of-episode overlay + auto-advance
    // ------------------------------------------------------------
    var endOfEpisodeVisible by mutableStateOf(false)

    /** The item offered as "next" on the end-of-episode overlay. */
    val suggestedNextUp: MediaItem?
        get() = resolveNextUp()

    fun dismissEndOfEpisode() {
        endOfEpisodeVisible = false
    }

    /** Advance to the suggested next item (auto-advance / overlay button). */
    fun playSuggestedNext() {
        dismissEndOfEpisode()
        playNext()
    }

    private fun onPlaybackCompleted() {
        isPlaying = false
        wasPlaying = false
        flushWatchTime()
        currentItem?.let { item ->
            library.updateProgress(item.id, durationMs, durationMs)
            library.recordHistory(item, subtitleUsed = subtitles.activeTrack?.name.orEmpty())
        }
        loopMode = LoopMode.Off
        replayRemaining = 0
        if (currentItem == null) {
            notifyPlayback("Finished", currentItem)
            state.toastHost.show("Playback finished", kind = ToastKind.Info)
            return
        }
        notifyPlayback("Finished", currentItem)
        val next = resolveNextUp()
        if (next != null && state.settings.getBool("media.auto-advance")) {
            state.toastHost.show("Episode finished — playing ${next.name}", kind = ToastKind.Info)
            notifyUser("Episode finished", "Playing next: ${next.name}")
            if (queueIndex >= 0 && queueIndex < playQueue.lastIndex) queueIndex += 1
            openItem(next)
        } else {
            endOfEpisodeVisible = true
            currentItem?.let { notifyUser("Episode finished", it.name) }
        }
    }

    /**
     * Relink the current item (or any item) to a new file location after the
     * original was moved — identity, history and mined-card links survive.
     */
    fun relinkItem(itemId: String, newFile: File): Boolean {
        val target = library.item(itemId)
        if (target?.isRemote == true) return false
        val updated = library.relink(itemId, newFile) ?: return false
        // The poster was extracted from the old file — drop it and regenerate.
        scanner.thumbnailState.remove(itemId)
        File(scanner.thumbnailsDir, "$itemId.jpg").delete()
        // If the relinked item is the one playing, reload it from the new path.
        if (currentItem?.id == itemId) {
            activeBackend?.stop()
            activeBackend?.close()
            openItem(updated)
        }
        state.activityLog.record(ActivityCategory.System, "Relinked media to ${newFile.absolutePath}", details = updated.name)
        state.toastHost.show("Relinked \"${updated.name}\"", kind = ToastKind.Success)
        return true
    }

    fun relinkCurrentItem(newFile: File): Boolean =
        currentItem?.let { relinkItem(it.id, newFile) } ?: false

    // ------------------------------------------------------------
    // External integrations (text hook + player WebSocket)
    // ------------------------------------------------------------
    val textHook = TextHookServer(::onHookText)
    val playerSocket = PlayerStateWebSocket(::stateJson, ::onSocketCommand)
    var textHookRunning by mutableStateOf(false)
    var lastHookText by mutableStateOf<String?>(null)

    // ------------------------------------------------------------
    // Subtitle editing (in-memory corrections, file untouched)
    // ------------------------------------------------------------
    var cueEditOpen by mutableStateOf(false)
    private val cueTextOverrides = mutableStateMapOf<String, String>()

    // ------------------------------------------------------------
    // Bookkeeping (persisted like before)
    // ------------------------------------------------------------
    val bookmarks = mutableStateListOf<MediaBookmark>()
    val audioClips = mutableStateListOf<AudioClip>()
    val recentFiles = mutableStateListOf<String>()
    val miningEvents = mutableStateListOf<MediaMiningEvent>()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val stateFile: File get() = File(System.getProperty("user.home"), ".kaiteyo/media-state.json")

    // Watch-session accounting: media time is separate from study time.
    private var sessionStartMs = 0L
    private var sessionWatchMs = 0L
    private var sessionCounted = true
    private var lastProgressSave = 0L
    private var lastAutoPauseIndex = -1
    private var trayShown = false
    private var trayUnsupported = false
    private var wasPlaying = false // last transport state, for notifications
    var watchTimeMs by mutableStateOf(0L)
        private set

    /** Estimated known-word coverage (0..1) of the current subtitle. */
    val currentCoverage: Float
        get() {
            val cue = activeCue ?: return 0f
            // Composition reads this every frame — a segmentation hiccup must
            // degrade to 0 rather than take the window down with it.
            return runCatching {
                ua.syt0r.kanji.desktop.engine.dictionary.JapaneseSegmenter.coverage(
                    cue.text, state.dictionary.repository, state.cards.toList()
                )
            }.getOrDefault(0f)
        }

    /** Number of cards mined from the current media item. */
    val currentMinedCount: Int
        get() = currentItem?.let { item -> miningEvents.count { it.mediaPath == item.path } } ?: 0

    /** Running count of dictionary lookups in this session. */
    var dictionaryLookupCount by mutableStateOf(0)
        private set

    /** Coverage breakdown for the current subtitle. */
    val currentStats: MediaCoverageStats
        get() = activeCue?.let { coverageFor(displayTextFor(it)) } ?: MediaCoverageStats(0, 0, 0, 0, 0, 0)

    /** Distinct kanji appearing in a piece of text. */
    fun kanjiIn(text: String): List<Char> =
        text.filter { it.code in 0x4E00..0x9FFF || it.code in 0x3400..0x4DBF || it.code in 0xF900..0xFAFF }.toList().distinct()

    /**
     * Coverage breakdown for one piece of text (the active cue, a search hit,
     * a selected range…). Statuses come from the live card pool.
     */
    fun coverageFor(text: String): MediaCoverageStats {
        val tokens = runCatching {
            JapaneseSegmenter.segment(text, state.dictionary.repository, state.cards.toList())
                .filter { it.isJapanese }
        }.getOrDefault(emptyList())
        var known = 0
        var learning = 0
        var unknown = 0
        var mined = 0
        var suspended = 0
        tokens.forEach { t ->
            when (t.status) {
                WordStatus.Known, WordStatus.Mature -> known++
                WordStatus.Learning, WordStatus.New -> learning++
                WordStatus.Unknown -> unknown++
                WordStatus.Mined -> mined++
                WordStatus.Suspended -> suspended++
            }
        }
        return MediaCoverageStats(tokens.size, known, learning, unknown, mined, suspended)
    }

    /**
     * Vocabulary coverage for a whole media item — the unique surfaces of the
     * loaded subtitle track, each classified against the card pool. This is
     * the honest "known-word %" number for an episode, clearly labelled as an
     * estimate in the UI.
     */
    fun mediaStatsFor(item: MediaItem): MediaCoverageStats {
        val cues = subtitles.activeTrack?.track?.cues ?: return MediaCoverageStats(0, 0, 0, 0, 0, 0)
        val seen = LinkedHashSet<String>()
        cues.forEach { cue ->
            runCatching {
                JapaneseSegmenter.segment(displayTextFor(cue), state.dictionary.repository)
                    .filter { it.isJapanese }
                    .forEach { seen.add(it.surface) }
            }
        }
        var known = 0
        var learning = 0
        var unknown = 0
        var mined = 0
        var suspended = 0
        seen.forEach { surface ->
            when (JapaneseSegmenter.statusOf(surface, state.cards.toList())) {
                WordStatus.Known, WordStatus.Mature -> known++
                WordStatus.Learning, WordStatus.New -> learning++
                WordStatus.Unknown -> unknown++
                WordStatus.Mined -> mined++
                WordStatus.Suspended -> suspended++
            }
        }
        return MediaCoverageStats(seen.size, known, learning, unknown, mined, suspended)
    }

    /** Bookmarks belonging to a media item, newest first. */
    fun bookmarksFor(item: MediaItem): List<MediaBookmark> =
        bookmarks.filter { it.mediaPath == item.path }.sortedByDescending { it.timestampMs }

    /** Audio clips extracted from a media item, newest first. */
    fun clipsFor(item: MediaItem): List<AudioClip> =
        audioClips.filter { it.sourcePath == item.path }.sortedByDescending { it.createdAt }

    init {
        load()
        loadQueue()
    }

    // ------------------------------------------------------------
    // Opening media
    // ------------------------------------------------------------

    /** Open a media file; optionally resume from where the user left off. */
    fun openFile(file: File, resume: Boolean = true) {
        val item = library.addFile(file) ?: run {
            state.toastHost.show("Unsupported media file: ${file.name}", kind = ToastKind.Warning)
            return
        }
        openItem(item, resume)
    }

    /**
     * Open a network media source (http/https stream or direct file URL).
     * Both video backends already play URLs natively; the item joins the
     * library with its remote flag so nothing pretends it is a local file.
     */
    fun openUrl(url: String, name: String? = null, resume: Boolean = false) {
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            state.toastHost.show("Enter an http(s) URL", kind = ToastKind.Warning)
            return
        }
        val kind = MediaKind.fromUrl(trimmed) ?: MediaKind.Video
        val displayName = name?.takeIf { it.isNotBlank() }
            ?: trimmed.substringAfterLast('/').substringBefore('?').ifBlank { trimmed }
        val item = library.addRemote(trimmed, displayName, kind)
        openItem(item, resume)
    }

    fun openItem(item: MediaItem, resume: Boolean = true) {
        val file = File(item.path)
        if (!item.isRemote && !file.exists()) {
            playbackError = PlaybackError.FileMissing(item.path)
            state.toastHost.show("Media file not found: ${file.name}", kind = ToastKind.Warning)
            return
        }
        playbackError = null
        currentItem = item
        if (!recentFiles.contains(item.path)) {
            recentFiles.add(0, item.path)
            while (recentFiles.size > 30) recentFiles.removeAt(recentFiles.lastIndex)
        }
        save()

        // Backend selection: VLC first for video, then mpv, then report clearly.
        val backend = backends.backendFor(item.kind)
        if (backend == null) {
            activeBackend = null
            backendKind = BackendKind.None
            playbackError = PlaybackError.BackendUnavailable("Install VLC or mpv (or point Kaiteyo to them in Settings → Media) to play video. Audio files always work.")
            state.toastHost.show("No video backend available — install VLC or mpv", kind = ToastKind.Warning)
            return
        }
        activeBackend = backend
        backendKind = backend.kind

        backend.listener = ::onBackendEvent
        // Optional mpv shader pipeline (e.g. an Anime4K config file) from settings.
        if (backend is ua.syt0r.kanji.desktop.engine.playback.MpvBackend) {
            backend.shaderPath = state.settings.getString("media.mpv-shader", "").ifBlank { null }
        }
        val openResult = backend.open(file.absolutePath)
        openResult.onFailure { e ->
            playbackError = if (e is PlaybackError) e else PlaybackError.Other(e.message ?: "Unknown error")
            state.toastHost.show("Could not play ${item.name}: ${playbackError?.userMessage}", kind = ToastKind.Error)
        }
        openResult.onSuccess {
            endOfEpisodeVisible = false
            statistics.recordSession()
            if (resume && item.lastPositionMs > 5000) {
                if (resumePromptEnabled) {
                    pendingResumeMs = item.lastPositionMs
                    resumePromptPending = true
                    backend.pause()
                } else {
                    backend.seekTo(item.lastPositionMs)
                    state.toastHost.show("Resumed at ${formatTime(item.lastPositionMs)}", kind = ToastKind.Info)
                }
            }
            // Auto-attach the remembered or companion subtitle (one track at a time).
            // Remote streams never have companion files next to them.
            val subPath = if (item.isRemote) item.subtitlePath
            else item.subtitlePath.ifBlank { library.findCompanionSubtitle(file).firstOrNull()?.absolutePath.orEmpty() }
            if (subPath.isNotBlank() && File(subPath).exists()) {
                subtitles.clear()
                subtitles.loadFile(File(subPath))
                library.setSubtitle(item.id, subPath)
            }
            speed.let { backend.setSpeed(it) }
            volume.let { backend.setVolume(it) }
            muted.let { backend.setMuted(it) }
            backend.setPerformanceProfile(state.settings.getString("media.perf-profile", "balanced"))
            // Restore the user's rendering / audio preferences onto the new media.
            applyVideoSettings()
            applyAudioSettings()
            if (subtitleDelayMs != 0L) backend.setSubtitleDelay(subtitleDelayMs)
            state.activityLog.record(ActivityCategory.Study, "Opened media \"${item.name}\"", details = item.kind.name)
        }
    }

    /**
     * Open an item and jump straight to [positionMs] (used by the detail view
     * "Open at …" actions and mined-card source links).
     */
    fun openItemAt(item: MediaItem, positionMs: Long = 0L, play: Boolean = true) {
        openItem(item, resume = false)
        if (positionMs > 0) {
            seekTo(positionMs)
            if (play) this.play()
        }
    }

    /** Toggle the watch-later tag on a library item. */
    fun toggleWatchLater(itemId: String) {
        library.toggleWatchLater(itemId)
        state.toastHost.show(
            if (library.isWatchLater(itemId)) "Added to Watch later" else "Removed from Watch later",
            kind = ToastKind.Info
        )
    }

    fun openSubtitleFile(file: File) {
        subtitles.loadFile(file).onSuccess { entry ->
            currentItem?.let { library.setSubtitle(it.id, file.absolutePath) }
            subtitleVisible = true
            state.toastHost.show("Loaded subtitles: ${entry.name} (${entry.track.cues.size} cues)")
        }.onFailure {
            playbackError = PlaybackError.SubtitleInvalid(it.message ?: "Parse failed")
            state.toastHost.show("Could not parse subtitles", kind = ToastKind.Warning)
        }
    }

    // ------------------------------------------------------------
    // Library-wide subtitle search
    // ------------------------------------------------------------

    /** Search every indexed subtitle track in the library for [query]. */
    fun subtitleSearch(query: String, limit: Int = 500): List<SubtitleSearchHit> =
        subtitleSearchIndex.search(query, limit)

    /**
     * Open the media a subtitle search hit belongs to, at the hit's
     * timestamp, with the hit's track active and the transcript open so
     * the matching line is visible and highlighted.
     */
    fun openSubtitleHit(hit: SubtitleSearchHit) {
        val item = library.item(hit.mediaId) ?: library.itemByPath(hit.mediaPath)
        if (item == null) {
            state.toastHost.show("This media is no longer in the library", kind = ToastKind.Warning)
            return
        }
        openItem(item, resume = false)
        // Make sure the hit's track is the active one (openItem auto-attaches
        // the item's stored track; a companion hit needs explicit loading).
        if (subtitles.activeTrack?.path != hit.trackPath) {
            val trackFile = File(hit.trackPath)
            if (trackFile.exists()) openSubtitleFile(trackFile)
        }
        seekTo(hit.startMs + subtitles.globalOffsetMs)
        play()
        transcriptOpen = true
        state.activityLog.record(
            ActivityCategory.Study,
            "Opened subtitle search hit",
            details = "${item.name} @ ${formatTime(hit.startMs)} · ${hit.cueText.take(60)}"
        )
        state.toastHost.show(
            "${item.name} · ${formatTime(hit.startMs)} · ${hit.cueText.take(40)}",
            kind = ToastKind.Info
        )
    }

    // ------------------------------------------------------------
    // Desktop notifications (tray balloons — opt-in via settings)
    // ------------------------------------------------------------

    /** Show a tray balloon, starting the tray lazily (opt-in via settings). */
    fun notifyUser(title: String, message: String) {
        if (!state.settings.getBool("media.notifications")) return
        if (trayShown) {
            tray.notify(title, message)
            return
        }
        if (trayUnsupported) return
        if (tray.supported) {
            tray.start()
            trayShown = true
            tray.notify(title, message)
        } else {
            trayUnsupported = true
        }
    }

    /** Convenience used by the mining pipeline after a card lands. */
    fun notifyMined(headword: String) {
        notifyUser("Card mined", "\"$headword\" added to your deck — study it in Review")
    }

    /** Tray balloon for a playback transition (title + media name). */
    private fun notifyPlayback(title: String, item: MediaItem?) {
        val media = item ?: return
        notifyUser(title, media.name)
    }

    // ------------------------------------------------------------
    // Backend events → reactive state
    // ------------------------------------------------------------

    private fun onBackendEvent(event: PlaybackEvent) {
        when (event.type) {
            PlaybackEventType.Started -> {
                isPlaying = true
                if (!wasPlaying) notifyPlayback("Playing", currentItem)
                wasPlaying = true
                if (sessionStartMs == 0L) sessionStartMs = System.currentTimeMillis()
                sessionCounted = false
            }
            PlaybackEventType.Paused -> {
                isPlaying = false
                wasPlaying = false
                notifyPlayback("Paused", currentItem)
                flushWatchTime()
            }
            PlaybackEventType.Stopped -> {
                isPlaying = false
                wasPlaying = false
                flushWatchTime()
            }
            PlaybackEventType.Completed -> onPlaybackCompleted()
            PlaybackEventType.Buffering -> buffering = true
            PlaybackEventType.BufferingEnded -> buffering = false
            PlaybackEventType.Error -> {
                isPlaying = false
                playbackError = PlaybackError.Other(event.message.ifBlank { "Playback engine stopped." })
            }
            PlaybackEventType.MediaUnloaded -> isPlaying = false
            else -> Unit
        }
    }

    // ------------------------------------------------------------
    // Transport
    // ------------------------------------------------------------

    fun play() {
        val backend = activeBackend ?: return
        if (playbackError != null) playbackError = null
        lastAutoPauseIndex = -1
        backend.play()
    }

    fun pause() {
        activeBackend?.pause()
    }

    fun togglePlay() {
        if (isPlaying) pause() else play()
    }

    fun stop() {
        activeBackend?.stop()
        positionMs = 0
    }

    fun seekTo(ms: Long) {
        val backend = activeBackend ?: return
        if (PlaybackCapability.CanSeek !in backend.capabilities) {
            state.toastHost.show("This backend cannot seek", kind = ToastKind.Info)
            return
        }
        lastAutoPauseIndex = -1
        backend.seekTo(ms.coerceAtLeast(0))
        positionMs = ms.coerceAtLeast(0)
    }

    fun seekBy(deltaMs: Long) {
        seekTo(positionMs + deltaMs)
    }

    fun updateSpeed(rate: Float) {
        speed = rate.coerceIn(0.25f, 2f)
        activeBackend?.setSpeed(speed)
    }

    fun updateVolume(value: Int) {
        volume = value.coerceIn(0, 100)
        activeBackend?.setVolume(volume)
    }

    fun toggleMute() {
        muted = !muted
        activeBackend?.setMuted(muted)
    }

    fun setLoop(loop: Boolean) {
        activeBackend?.setLoop(loop)
    }

    // ------------------------------------------------------------
    // Video rendering (display mode / aspect / adjustments)
    // ------------------------------------------------------------

    /** Apply the current rendering preferences to the active backend. */
    fun applyVideoSettings() {
        val backend = activeBackend ?: return
        if (PlaybackCapability.CanDisplayMode in backend.capabilities) backend.setDisplayMode(displayMode)
        if (PlaybackCapability.CanAspectRatio in backend.capabilities) backend.setAspectRatio(aspectRatio)
        if (PlaybackCapability.CanVideoAdjustments in backend.capabilities) backend.setVideoAdjustments(videoAdjustments)
        if (PlaybackCapability.CanDeinterlace in backend.capabilities) backend.setDeinterlace(videoAdjustments.deinterlace)
    }

    fun updateDisplayMode(mode: VideoDisplayMode) {
        displayMode = mode
        activeBackend?.let { if (PlaybackCapability.CanDisplayMode in it.capabilities) it.setDisplayMode(mode) }
        state.settings.set("media.display-mode", mode.name.lowercase())
    }

    fun cycleDisplayMode() {
        val next = VideoDisplayMode.entries[(VideoDisplayMode.entries.indexOf(displayMode) + 1) % VideoDisplayMode.entries.size]
        updateDisplayMode(next)
        state.toastHost.show("Display: ${next.label} — ${next.description}", kind = ToastKind.Info)
    }

    fun updateAspectRatio(preset: AspectRatioPreset) {
        aspectRatio = preset
        activeBackend?.let { if (PlaybackCapability.CanAspectRatio in it.capabilities) it.setAspectRatio(preset) }
        state.settings.set("media.aspect-ratio", preset.name.lowercase())
    }

    fun cycleAspectRatio() {
        val next = AspectRatioPreset.entries[(AspectRatioPreset.entries.indexOf(aspectRatio) + 1) % AspectRatioPreset.entries.size]
        updateAspectRatio(next)
        state.toastHost.show("Aspect ratio: ${next.label}", kind = ToastKind.Info)
    }

    /** Update one video adjustment and push it to the backend immediately. */
    fun updateVideoAdjustment(transform: (VideoAdjustments) -> VideoAdjustments) {
        videoAdjustments = transform(videoAdjustments)
        activeBackend?.let { if (PlaybackCapability.CanVideoAdjustments in it.capabilities) it.setVideoAdjustments(videoAdjustments) }
        persistVideoAdjustments()
    }

    fun resetVideoAdjustments() {
        videoAdjustments = VideoAdjustments()
        activeBackend?.let { if (PlaybackCapability.CanVideoAdjustments in it.capabilities) it.setVideoAdjustments(videoAdjustments) }
        state.settings.set("media.video-brightness", 100)
        state.settings.set("media.video-contrast", 100)
        state.settings.set("media.video-saturation", 100)
        state.settings.set("media.video-gamma", 100)
        state.settings.set("media.video-hue", 0)
        state.settings.setBool("media.video-deinterlace", false)
        state.toastHost.show("Video adjustments reset", kind = ToastKind.Info)
    }

    fun toggleDeinterlace() {
        updateVideoAdjustment { it.withDeinterlace(!it.deinterlace) }
    }

    private fun persistVideoAdjustments() {
        state.settings.set("media.video-brightness", videoAdjustments.brightness.toInt())
        state.settings.set("media.video-contrast", videoAdjustments.contrast.toInt())
        state.settings.set("media.video-saturation", videoAdjustments.saturation.toInt())
        state.settings.set("media.video-gamma", videoAdjustments.gamma.toInt())
        state.settings.set("media.video-hue", videoAdjustments.hue.toInt())
        state.settings.setBool("media.video-deinterlace", videoAdjustments.deinterlace)
    }

    // ------------------------------------------------------------
    // Audio extras (delay / channel / output / equalizer)
    // ------------------------------------------------------------

    /** Apply audio preferences to the active backend. */
    fun applyAudioSettings() {
        val backend = activeBackend ?: return
        if (PlaybackCapability.CanAudioDelay in backend.capabilities) backend.setAudioDelay(audioDelayMs)
        if (PlaybackCapability.CanAudioChannel in backend.capabilities) backend.setAudioChannel(audioChannel)
        if (PlaybackCapability.CanAudioOutput in backend.capabilities) backend.setAudioOutput(audioOutputId)
        if (PlaybackCapability.CanEqualizer in backend.capabilities) backend.setEqualizer(equalizer)
    }

    fun updateAudioDelay(ms: Long) {
        audioDelayMs = ms.coerceIn(-10000, 10000)
        activeBackend?.let { if (PlaybackCapability.CanAudioDelay in it.capabilities) it.setAudioDelay(audioDelayMs) }
        state.settings.set("media.audio-delay-ms", audioDelayMs)
        if (ms != 0L) {
            state.toastHost.show("Audio delay ${if (ms > 0) "+" else ""}${ms} ms", kind = ToastKind.Info)
        }
    }

    fun adjustAudioDelay(deltaMs: Long) {
        updateAudioDelay(audioDelayMs + deltaMs)
    }

    fun updateAudioChannel(channel: AudioChannelPreset) {
        audioChannel = channel
        activeBackend?.let { if (PlaybackCapability.CanAudioChannel in it.capabilities) it.setAudioChannel(channel) }
        state.settings.set("media.audio-channel", channel.name.lowercase())
    }

    fun updateAudioOutput(deviceId: String?) {
        audioOutputId = deviceId
        activeBackend?.let { if (PlaybackCapability.CanAudioOutput in it.capabilities) it.setAudioOutput(deviceId) }
        state.settings.set("media.audio-output", deviceId.orEmpty())
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        equalizer = equalizer.withPreset(preset)
        activeBackend?.let { if (PlaybackCapability.CanEqualizer in it.capabilities) it.setEqualizer(equalizer) }
        state.settings.set("media.eq-preset", preset.name.lowercase())
        state.toastHost.show("Equalizer: ${preset.label}", kind = ToastKind.Info)
    }

    fun updateEqualizerPreamp(db: Float) {
        equalizer = equalizer.withPreamp(db)
        activeBackend?.let { if (PlaybackCapability.CanEqualizer in it.capabilities) it.setEqualizer(equalizer) }
        state.settings.set("media.eq-preamp-db", db)
    }

    fun updateEqualizerBand(index: Int, db: Float) {
        equalizer = equalizer.withBand(index, db)
        activeBackend?.let { if (PlaybackCapability.CanEqualizer in it.capabilities) it.setEqualizer(equalizer) }
        state.settings.set("media.eq-bands-db", equalizer.bandsDb.joinToString(",") { it.toString() })
    }

    fun disableEqualizer() {
        equalizer = EqualizerSettings()
        activeBackend?.let { if (PlaybackCapability.CanEqualizer in it.capabilities) it.setEqualizer(null) }
        state.settings.set("media.eq-preset", "flat")
        state.toastHost.show("Equalizer off", kind = ToastKind.Info)
    }

    fun selectSubtitleTrack(trackId: String?) {
        activeBackend?.selectTrack(trackId)
    }

    fun selectAudioTrack(trackId: String?) {
        activeBackend?.selectTrack(trackId)
    }

    fun selectVideoTrack(trackId: String?) {
        activeBackend?.selectTrack(trackId)
    }

    fun setSubtitleDelay(delayMs: Long) {
        subtitleDelayMs = delayMs.coerceIn(-10000, 10000)
        activeBackend?.setSubtitleDelay(subtitleDelayMs)
        subtitles.setOffset(subtitleDelayMs)
        state.settings.set("media.subtitle-delay-ms", subtitleDelayMs)
    }

    fun adjustSubtitleDelay(deltaMs: Long) {
        setSubtitleDelay(subtitles.globalOffsetMs + deltaMs)
    }

    fun frameStepForward() {
        activeBackend?.frameStepForward()
    }

    fun frameStepBackward() {
        activeBackend?.frameStepBackward()
    }

    /**
     * Cycle the selected word within the current subtitle (Alt+← / Alt+→).
     * Only Japanese tokens participate; the selection wraps around.
     */
    fun cycleToken(direction: Int) {
        val cue = activeCue ?: return
        val tokens = tokensFor(cue).filter { it.isJapanese }
        if (tokens.isEmpty()) return
        val current = selectedTokens.firstOrNull()?.let { sel ->
            tokens.indexOfFirst { it.surface == sel.surface && it.offset == sel.offset }
        } ?: tokenCycleIndex
        val base = if (current >= 0) current else if (direction > 0) -1 else 0
        val next = ((base + direction) % tokens.size + tokens.size) % tokens.size
        tokenCycleIndex = next
        selectToken(tokens[next])
    }

    // ------------------------------------------------------------
    // Chapter navigation (backend-provided chapters)
    // ------------------------------------------------------------

    /** Index of the chapter containing the current position, or -1. */
    val currentChapterIndex: Int
        get() {
            val list = chapters
            if (list.isEmpty()) return -1
            return list.indexOfLast { it.startMs <= positionMs }
        }

    fun previousChapter() {
        val list = chapters
        if (list.isEmpty()) {
            state.toastHost.show("This file has no chapters", kind = ToastKind.Info)
            return
        }
        val idx = currentChapterIndex
        seekTo(if (idx > 0) list[idx - 1].startMs else 0L)
        play()
    }

    fun nextChapter() {
        val list = chapters
        if (list.isEmpty()) {
            state.toastHost.show("This file has no chapters", kind = ToastKind.Info)
            return
        }
        val idx = currentChapterIndex
        seekTo(if (idx >= 0 && idx < list.lastIndex) list[idx + 1].startMs else durationMs)
        play()
    }

    // ------------------------------------------------------------
    // Capture
    // ------------------------------------------------------------

    fun captureScreenshot(): String? {
        val backend = activeBackend ?: return null
        if (PlaybackCapability.CanScreenshot !in backend.capabilities) {
            state.toastHost.show("The ${backend.kind.name} backend cannot capture screenshots", kind = ToastKind.Warning)
            return null
        }
        // Configurable folder + format (Kaiteyo_AnimeName_00-18-42.png).
        val dir = File(
            state.settings.getString("media.screenshot-folder", "").ifBlank { MediaCapture.ensureCache().absolutePath }
        ).apply { mkdirs() }
        val format = state.settings.getString("media.screenshot-format", "png").lowercase().takeIf { it == "jpg" || it == "jpeg" || it == "png" } ?: "png"
        val target = File(dir, screenshotFileName(currentItem?.name.orEmpty(), positionMs, format))
        val result = backend.snapshot(target)
        result.onSuccess {
            lastScreenshotPath = it
            state.activityLog.record(ActivityCategory.Study, "Captured video screenshot", details = currentItem?.name.orEmpty())
            state.toastHost.show("Screenshot saved to ${target.name}", kind = ToastKind.Success)
        }.onFailure {
            state.toastHost.show("Screenshot failed: ${it.message}", kind = ToastKind.Warning)
        }
        return lastScreenshotPath
    }

    /**
     * Extract the cue's audio range, honoring the configurable padding
     * (before/after) and an optional hard cap on clip length so a long
     * subtitle never produces a massive file. Reads the settings when the
     * defaults are used; explicit values (e.g. from the mining dialog)
     * override the settings.
     */
    fun captureAudioClip(
        cue: SubtitleCue,
        paddingBeforeMs: Long = state.settings.getInt("media.audio-padding-before-ms", 200).toLong(),
        paddingAfterMs: Long = state.settings.getInt("media.audio-padding-after-ms", 200).toLong(),
        maxDurationMs: Long = state.settings.getInt("media.audio-max-duration-ms", 10000).toLong()
    ): String? {
        val item = currentItem ?: return null
        val start = (cue.startMs - paddingBeforeMs).coerceAtLeast(0)
        val rawEnd = cue.endMs + paddingAfterMs
        val end = if (maxDurationMs > 0) minOf(rawEnd, start + maxDurationMs) else rawEnd
        val label = cue.text.take(24).replace(Regex("[^\\p{L}\\p{N}]"), "")
        val result = MediaCapture.extractAudioClip(item.path, start, end, label)
        result.onSuccess { file ->
            val clip = AudioClip(
                id = "clip-${System.currentTimeMillis()}",
                sourcePath = item.path,
                label = cue.text.take(60),
                startMs = start,
                endMs = end,
                exportedPath = file.absolutePath,
                createdAt = Clock.System.now().toString()
            )
            audioClips.add(0, clip)
            save()
            lastAudioClipPath = file.absolutePath
            state.toastHost.show("Audio clip captured", kind = ToastKind.Success)
        }.onFailure {
            state.toastHost.show("Audio capture failed: ${it.message}", kind = ToastKind.Warning)
        }
        return lastAudioClipPath
    }

    /** Capture a short MP4 clip of the cue's video range (requires ffmpeg). */
    fun captureVideoClip(cue: SubtitleCue, paddingMs: Long = 200): String? {
        val item = currentItem ?: return null
        if (!MediaCapture.ffmpegAvailable) {
            state.toastHost.show("Video clip capture needs ffmpeg", kind = ToastKind.Warning)
            return null
        }
        val start = (cue.startMs - paddingMs).coerceAtLeast(0)
        val end = cue.endMs + paddingMs
        val label = cue.text.take(24).replace(Regex("[^\\p{L}\\p{N}]"), "")
        val result = MediaCapture.extractVideoClip(item.path, start, end, label)
        var captured: String? = null
        result.onSuccess { file ->
            captured = file.absolutePath
            lastVideoClipPath = file.absolutePath
            state.activityLog.record(ActivityCategory.Study, "Captured video clip", details = cue.text.take(60))
            state.toastHost.show("Video clip captured", kind = ToastKind.Success)
        }.onFailure {
            state.toastHost.show("Video clip failed: ${it.message}", kind = ToastKind.Warning)
        }
        // Never return a stale path from a previous capture on failure.
        return captured
    }

    // ------------------------------------------------------------
    // Subtitle-driven playback
    // ------------------------------------------------------------

    fun seekToCue(index: Int) {
        val cue = subtitles.cueAt(index) ?: return
        seekTo(cue.startMs + subtitles.globalOffsetMs)
    }

    fun replayCue(replayTimes: Int = state.settings.getInt("media.replay-count", 1)) {
        val cue = activeCue ?: return
        loopMode = LoopMode.Off
        replayRemaining = replayTimes
        seekTo(cue.startMs + subtitles.globalOffsetMs)
        play()
    }

    fun replayPreviousCue() {
        val idx = subtitles.prevIndex(activeCueIndex)
        seekToCue(idx)
        play()
    }

    fun replayNextCue() {
        val idx = subtitles.nextIndex(activeCueIndex)
        seekToCue(idx)
        play()
    }

    fun toggleLoopCue() {
        if (loopMode == LoopMode.CurrentCue) {
            loopMode = LoopMode.Off
        } else {
            val cue = activeCue ?: return
            loopMode = LoopMode.CurrentCue
            replayRemaining = 0
            loopStartMs = cue.startMs + subtitles.globalOffsetMs
            loopEndMs = cue.endMs + subtitles.globalOffsetMs
        }
    }

    fun setLoopRange(startMs: Long, endMs: Long) {
        loopStartMs = startMs
        loopEndMs = endMs
        loopMode = if (endMs > startMs) LoopMode.Range else LoopMode.Off
    }

    /**
     * Mark point A of an A–B repeat range at the current position. When a
     * range is already active and the playhead has moved past A, the click
     * completes the range by setting B instead (matches the on-screen chips).
     */
    fun setLoopPointA() {
        if (loopMode == LoopMode.Range && loopStartMs > 0 && positionMs > loopStartMs) {
            loopEndMs = positionMs
        } else {
            loopStartMs = positionMs
            loopMode = LoopMode.Range
        }
        state.toastHost.show("A–B loop: ${formatTime(loopStartMs)} → ${formatTime(loopEndMs)}", kind = ToastKind.Info)
    }

    /** Mark point B of an A–B repeat range at the current position. */
    fun setLoopPointB() {
        loopEndMs = positionMs
        loopMode = LoopMode.Range
        state.toastHost.show("A–B loop: ${formatTime(loopStartMs)} → ${formatTime(loopEndMs)}", kind = ToastKind.Info)
    }

    fun toggleCondensed() {
        condensedPlayback = !condensedPlayback
        if (condensedPlayback) {
            condensedFastForward = state.settings.getBool("media.condensed-fast-forward")
            state.toastHost.show("Condensed playback: skipping unsubtitled sections")
        }
    }

    /** Flip fast-forward mode for condensed playback (persisted in settings). */
    fun toggleCondensedFastForward() {
        condensedFastForward = !condensedFastForward
        state.settings.setBool("media.condensed-fast-forward", condensedFastForward)
        state.toastHost.show(
            if (condensedFastForward) "Condensed playback: fast-forwarding through gaps"
            else "Condensed playback: jumping straight to the next subtitle",
            kind = ToastKind.Info
        )
    }

    // ------------------------------------------------------------
    // Lookup / mining
    // ------------------------------------------------------------

    /**
     * Select a token. [extend] (shift-click / drag continuation) selects the
     * contiguous range between the selection anchor and this token; otherwise
     * the token becomes the new single selection. Multi-word selections keep
     * the exact original text (joined token surfaces) for lookup and mining.
     */
    fun selectToken(token: SegmentToken, extend: Boolean = false) {
        val cue = activeCue
        val tokens = cue?.let { tokensFor(it) } ?: emptyList()
        val index = tokens.indexOfFirst { it.surface == token.surface && it.offset == token.offset }
        if (extend && index >= 0 && selectionAnchor in tokens.indices) {
            selectTokenRange(selectionAnchor, index, tokens)
        } else {
            selectedTokens = listOf(token)
            selectionAnchor = index
            openLookup(token.surface)
        }
    }

    /** Select every token between [fromIndex] and [toIndex] of the active cue. */
    fun selectTokenRange(fromIndex: Int, toIndex: Int) {
        val cue = activeCue ?: return
        selectTokenRange(fromIndex, toIndex, tokensFor(cue))
    }

    private fun selectTokenRange(fromIndex: Int, toIndex: Int, tokens: List<SegmentToken>) {
        if (tokens.isEmpty()) return
        val lo = fromIndex.coerceIn(0, tokens.lastIndex)
        val hi = toIndex.coerceIn(0, tokens.lastIndex)
        selectedTokens = (minOf(lo, hi)..maxOf(lo, hi)).map { tokens[it] }
        selectionAnchor = minOf(lo, hi)
        openLookup(joinTokenSurfaces(selectedTokens))
    }

    /** Select every token of the current subtitle (whole-sentence selection). */
    fun selectAllTokens() {
        val cue = activeCue ?: return
        val tokens = tokensFor(cue)
        if (tokens.isNotEmpty()) selectTokenRange(0, tokens.lastIndex, tokens)
    }

    /** Drop the current token selection without closing the dictionary. */
    fun clearSelection() {
        selectedTokens = emptyList()
        selectionAnchor = -1
    }

    /** The exact original text of the current selection (token order preserved). */
    fun selectedPhrase(): String = joinTokenSurfaces(selectedTokens)

    private fun openLookup(text: String) {
        lookupQuery = text
        dictionaryOpen = true
        dictionaryLookupCount++
        statistics.recordLookup()
        state.activityLog.record(
            ActivityCategory.Study,
            "Dictionary lookup: ${text.take(40)}",
            details = currentItem?.name.orEmpty()
        )
    }

    fun lookupText(text: String) {
        openLookup(text)
    }

    fun clearLookup() {
        lookupQuery = null
        dictionaryOpen = false
        selectedTokens = emptyList()
        selectionAnchor = -1
    }

    // ------------------------------------------------------------
    // Mini player
    // ------------------------------------------------------------

    fun openMiniPlayer() {
        miniPlayerOpen = true
    }

    fun closeMiniPlayer() {
        miniPlayerOpen = false
    }

    fun toggleMiniPlayer() {
        miniPlayerOpen = !miniPlayerOpen
    }

    // ------------------------------------------------------------
    // External integrations (text hook + player WebSocket)
    // ------------------------------------------------------------

    // System tray controller — play/pause/next/prev/stop while the app is
    // in the background (java.awt only; absent on unsupported desktops).
    val tray = MediaTray(::onTrayAction)

    // Global OS media keys (dedicated Play/Pause · Next · Previous · Stop
    // buttons on the keyboard). Windows captures them via a low-level
    // keyboard hook; macOS/Linux fall back to the tray. Started lazily on
    // first media and by the settings toggle.
    val systemMediaKeys = SystemMediaKeys(::onSystemMediaKey)
    private var systemKeysStarted = false
    private var systemKeysUnsupported = false

    /** Start whatever integrations are enabled in settings. */
    fun startIntegrationsIfEnabled() {
        if (state.settings.getBool("media.text-hook.enabled")) startTextHook()
        if (state.settings.getBool("media.ws.enabled")) startPlayerSocket()
        if (state.settings.getBool("media.watch-folders")) startFolderWatcher()
        if (state.settings.getBool("media.api.enabled")) state.localApi.start()
        // System media keys are deliberately NOT started here: the hook only
        // captures keys while media is loaded (see tick()), so Kaiteyo never
        // swallows Play/Pause/Next/Prev from other apps while idle.
    }

    fun stopIntegrations() {
        stopTextHook()
        stopPlayerSocket()
        stopFolderWatcher()
        state.localApi.stop()
        stopSystemMediaKeys()
    }

    /** Global media keys, driven by the settings toggle (idempotent). */
    fun startSystemMediaKeys() {
        if (systemMediaKeys.active) return
        if (systemMediaKeys.start()) {
            systemKeysStarted = true
        } else {
            systemKeysUnsupported = true
            systemMediaKeys.lastError?.let {
                state.activityLog.record(ActivityCategory.System, "System media keys unavailable: $it")
            }
        }
    }

    fun stopSystemMediaKeys() {
        systemMediaKeys.stop()
        systemKeysStarted = false
    }

    /** Whether the bridge is currently capturing keys (drives the settings badge). */
    val systemMediaKeysActive: Boolean get() = systemMediaKeys.active
    val systemMediaKeysSupported: Boolean get() = systemMediaKeys.supported

    /** Media-key action dispatch (see SystemMediaKeyAction). */
    private fun onSystemMediaKey(action: SystemMediaKeyAction) {
        when (action) {
            SystemMediaKeyAction.Toggle -> togglePlay()
            SystemMediaKeyAction.Next -> playNext()
            SystemMediaKeyAction.Previous -> playPrevious()
            SystemMediaKeyAction.Stop -> stop()
        }
    }

    /** Background watcher: pick up new media dropped into watched folders. */
    fun startFolderWatcher() {
        if (scanner.watcherActive) return
        scanner.startWatching()
        state.toastHost.show("Watching library folders for new media", kind = ToastKind.Info)
    }

    fun stopFolderWatcher() {
        scanner.stopWatching()
    }

    /** Tray action dispatch (see MediaTray action names). */
    private fun onTrayAction(action: String) {
        when (action) {
            "toggle" -> togglePlay()
            "previous" -> playPrevious()
            "next" -> playNext()
            "stop" -> stop()
            "show" -> state.currentView = ua.syt0r.kanji.desktop.appstate.WorkspaceView.Media
            "quit-media" -> closeMedia()
        }
    }

    fun startTextHook(port: Int = state.settings.getInt("media.text-hook.port", 8766)) {
        if (textHook.isRunning) return
        textHook.port = port
        textHook.start()
        textHookRunning = textHook.isRunning
        state.toastHost.show(
            if (textHookRunning) "Text hook listening on port $port — send Japanese text via netcat or texthookers"
            else "Text hook failed: ${textHook.lastError}",
            kind = if (textHookRunning) ToastKind.Success else ToastKind.Warning
        )
    }

    fun stopTextHook() {
        textHook.stop()
        textHookRunning = false
    }

    fun startPlayerSocket(port: Int = state.settings.getInt("media.ws.port", 8765)) {
        if (playerSocket.isRunning) return
        playerSocket.port = port
        playerSocket.start()
        state.toastHost.show(
            if (playerSocket.isRunning) "Player WebSocket on ws://127.0.0.1:$port — live state + commands"
            else "WebSocket failed: ${playerSocket.lastError}",
            kind = if (playerSocket.isRunning) ToastKind.Success else ToastKind.Warning
        )
    }

    fun stopPlayerSocket() {
        playerSocket.stop()
    }

    val wsRunning: Boolean get() = playerSocket.isRunning
    val wsClients: Int get() = playerSocket.clientCount
    val textHookClients: Int get() = textHook.clientCount

    /** A text line arrived from an external texthooker. */
    private fun onHookText(text: String) {
        lastHookText = text
        if (text.equals("CLEAR", ignoreCase = true)) {
            clearLookup()
            return
        }
        val clean = SubtitleNormalizer.normalizeForLookup(text)
        if (clean.isBlank()) return
        lookupText(clean.take(120))
        state.activityLog.record(ActivityCategory.Study, "Text hook: ${clean.take(60)}", details = currentItem?.name.orEmpty())
    }

    /** A command frame arrived over the player WebSocket. */
    private fun onSocketCommand(text: String) {
        runCatching {
            val obj = json.parseToJsonElement(text).jsonObject
            when (obj["command"]?.jsonPrimitive?.content) {
                "play" -> play()
                "pause" -> pause()
                "toggle" -> togglePlay()
                "stop" -> stop()
                "screenshot" -> captureScreenshot()
                "mine" -> mineCurrentCue()
                "replay" -> replayCue()
                "seek" -> obj["positionMs"]?.jsonPrimitive?.content?.toLongOrNull()?.let { seekTo(it) }
                "lookup" -> obj["text"]?.jsonPrimitive?.content?.let { lookupText(it.take(120)) }
                else -> Unit
            }
        }
    }

    /** Live player state broadcast to WebSocket clients every 500 ms. */
    private fun stateJson(): String = runCatching {
        val cue = activeCue
        json.encodeToString(
            PlayerStateSnapshot(
                media = currentItem?.name.orEmpty(),
                path = currentItem?.path.orEmpty(),
                positionMs = positionMs,
                durationMs = durationMs,
                playing = isPlaying,
                buffering = buffering,
                speed = speed,
                backend = backendKind.name,
                subtitle = cue?.let { displayTextFor(it) }.orEmpty(),
                subtitleStartMs = cue?.startMs ?: 0,
                subtitleEndMs = cue?.endMs ?: 0,
                selectedToken = selectedTokens.firstOrNull()?.surface.orEmpty(),
                lookupQuery = lookupQuery.orEmpty(),
                minedCount = currentMinedCount,
                textHookRunning = textHookRunning,
                wsClients = playerSocket.clientCount
            )
        )
    }.getOrDefault("{}")

    /**
     * Answer the resume prompt: true continues from the saved position,
     * false starts over. The prompt only appears when media.resume-prompt
     * is enabled and a position was actually saved.
     */
    fun answerResumePrompt(resume: Boolean) {
        resumePromptPending = false
        if (resume && pendingResumeMs > 0) {
            seekTo(pendingResumeMs)
            play()
            state.toastHost.show("Resumed at ${formatTime(pendingResumeMs)}", kind = ToastKind.Info)
        } else {
            play()
        }
        pendingResumeMs = 0
    }

    // ------------------------------------------------------------
    // End-of-episode quick actions (used by the overlay)
    // ------------------------------------------------------------

    /** Start the current episode over from the beginning. */
    fun restartEpisode() {
        endOfEpisodeVisible = false
        seekTo(0L)
        play()
    }

    /** Close the overlay and show the library panel. */
    fun returnToLibrary() {
        endOfEpisodeVisible = false
        clearLookup()
        libraryOpen = true
    }

    /** Chapter list from the active backend (used for timeline markers). */
    val chapters: List<PlaybackChapter>
        get() = activeBackend?.chapters() ?: emptyList()

    /** Nudge the dual-language track independently of the main track. */
    fun adjustSecondaryOffset(deltaMs: Long) {
        subtitles.adjustSecondaryOffset(deltaMs)
    }

    /** Live diagnostic snapshot for the debug panel. */
    fun debugSnapshot(): String = buildString {
        appendLine("Backend: ${backendKind.name} (${activeBackend?.diagnosticName ?: "none"})")
        appendLine("Media: ${currentItem?.name ?: "—"}")
        appendLine("Position: ${formatTime(positionMs)} / ${formatTime(durationMs)} · ${speed}x · buffering=$buffering")
        val cue = activeCue
        append("Subtitle: ${cue?.id ?: "—"}")
        if (cue != null) {
            append(" ${formatTime(cue.startMs + subtitles.globalOffsetMs)}–${formatTime(cue.endMs + subtitles.globalOffsetMs)}")
        }
        appendLine()
        appendLine("Token: ${selectedTokens.firstOrNull()?.surface ?: "—"} · Query: ${lookupQuery ?: "—"}")
        appendLine("Mined (media): $currentMinedCount · Lookups: $dictionaryLookupCount")
        appendLine("Text hook: ${if (textHookRunning) "on (${textHookClients} clients)" else "off"} · last: ${lastHookText ?: "—"}")
        appendLine("WebSocket: ${if (wsRunning) "on (${wsClients} clients)" else "off"}")
        appendLine("GSM: ${if (state.miningIntegration.gsm.connected) "connected" else "offline"} · mode: ${state.miningIntegration.mode.label}")
        append("Loop: ${loopMode.label} · Condensed: $condensedPlayback")
        appendLine()
        appendLine("Queue: ${playQueue.size} item(s) · cursor $queueIndex · watcher: ${if (scanner.watcherActive) "on (${scanner.lastWatchFound} last found)" else "off"}")
        appendLine("Watch: ${formatDuration(statistics.totalWatchMs)} · Study: ${formatDuration(statistics.totalStudyMs)} · Lookups: ${statistics.totalLookups} · Mined: ${statistics.totalMined}")
    }

    // ------------------------------------------------------------
    // Dual subtitles (secondary track)
    // ------------------------------------------------------------

    fun openSecondarySubtitleFile(file: File) {
        subtitles.loadSecondaryFile(file).onSuccess { entry ->
            subtitles.showSecondary = true
            state.toastHost.show("Secondary subtitles: ${entry.name} (${entry.track.cues.size} cues)")
        }.onFailure {
            state.toastHost.show("Could not parse secondary subtitles", kind = ToastKind.Warning)
        }
    }

    fun clearSecondarySubtitles() {
        subtitles.clearSecondary()
        secondaryCue = null
    }

    // ------------------------------------------------------------
    // Subtitle text editing (in-memory corrections)
    // ------------------------------------------------------------

    /** The text a cue shows, honoring user corrections. */
    fun displayTextFor(cue: SubtitleCue): String = cueTextOverrides[cue.id] ?: cue.text

    fun openCueEditor() {
        if (activeCue != null) cueEditOpen = true
    }

    fun closeCueEditor() {
        cueEditOpen = false
    }

    fun editCueText(cueId: String, text: String) {
        if (text.isBlank()) cueTextOverrides.remove(cueId)
        else cueTextOverrides[cueId] = text
        state.toastHost.show("Subtitle text updated (in-memory only)", kind = ToastKind.Info)
    }

    fun hasCueEdits(): Boolean = cueTextOverrides.isNotEmpty()

    fun resetCueEdits() {
        cueTextOverrides.clear()
        state.toastHost.show("Subtitle text corrections cleared", kind = ToastKind.Info)
    }

    // ------------------------------------------------------------
    // Study mode
    // ------------------------------------------------------------

    /** Flip study mode — when on, watch time counts as study time. */
    fun toggleStudyMode() {
        studyMode = !studyMode
        state.toastHost.show(
            if (studyMode) "Study mode on — watch time counts as study time"
            else "Study mode off — watching is leisure",
            kind = ToastKind.Info
        )
    }

    /** Segmented tokens of the active cue, annotated with word status. */
    fun tokensFor(cue: SubtitleCue): List<SegmentToken> =
        runCatching {
            JapaneseSegmenter.segment(displayTextFor(cue), state.dictionary.repository, state.cards.toList())
        }.getOrDefault(emptyList())

    /** Build a fully-populated mining payload from a cue + target token. */
    fun payloadForCue(
        cue: SubtitleCue,
        token: SegmentToken?,
        phraseTokens: List<SegmentToken>? = null,
        screenshotPath: String? = null,
        audioPath: String? = null,
        videoPath: String? = null,
        includeScreenshot: Boolean = true,
        includeAudio: Boolean = true,
        includeVideo: Boolean = true,
        deckId: String = DesktopCard.DEFAULT_DECK_ID
    ): MiningPayload {
        val phraseSel = phraseTokens?.takeIf { it.size > 1 }?.sortedBy { it.offset }
        val phrase = phraseSel?.joinToString("") { it.surface }
        // Prefer a dictionary entry for the whole phrase; fall back to the
        // component token's match so a phrase without an exact entry still
        // mines with useful reading/definition data.
        val phraseMatch = phrase?.takeIf { it.isNotBlank() }
            ?.let { state.dictionary.lookup(it).firstOrNull()?.matches?.firstOrNull() }
        val match = token?.dictionaryMatch ?: phraseMatch
        val headword = token?.surface?.takeIf { it.isNotBlank() }
            ?: phrase?.takeIf { it.isNotBlank() }
            ?: match?.entry?.headword
            ?: JapaneseSegmenter.segment(cue.text, state.dictionary.repository)
                .firstOrNull { it.isJapanese }?.surface
            ?: cue.text.take(40)

        val item = currentItem
        val shot = if (includeScreenshot) (screenshotPath ?: lastScreenshotPath) else null
        val audio = if (includeAudio) (audioPath ?: lastAudioClipPath) else null
        val video = if (includeVideo) (videoPath ?: lastVideoClipPath) else null

        return MiningPayload(
            headword = headword,
            reading = phraseSel?.joinToString("") { it.reading }?.takeIf { it.isNotBlank() }
                ?: token?.reading
                ?: match?.entry?.readings?.firstOrNull()?.reading.orEmpty(),
            definition = match?.entry?.senses?.joinToString("\n") { s -> s.glosses.joinToString("; ") }.orEmpty(),
            sentence = displayTextFor(cue),
            screenshotPath = shot,
            audioPath = audio,
            videoPath = video,
            timestamp = cue.startMs / 1000.0,
            source = "subtitle",
            sourceDetail = buildString {
                item?.let { append(it.name) }
                val track = subtitles.activeTrack
                if (track != null) append(" · ").append(track.name)
            },
            tags = buildList {
                if (match != null) add("dict:${match.dictionary.name}")
                if (phrase != null) add("phrase")
                item?.let {
                    if (it.collection.isNotBlank()) add("media:${it.collection}")
                    add("media:${it.name}")
                }
                add("subtitle")
            }.distinct(),
            deckId = deckId
        )
    }

    /**
     * Record that a card was mined from the current media (round-trip link).
     * Called by MiningEngine after a card lands in the pool.
     */
    fun recordMiningEvent(card: DesktopCard, payload: MiningPayload) {
        val item = currentItem ?: return
        statistics.recordMined()
        val ts = (payload.timestamp ?: (positionMs / 1000.0)).toLong().coerceAtLeast(0) * 1000
        val event = MediaMiningEvent(
            cardId = card.id,
            mediaPath = item.path,
            mediaName = item.name,
            timestampMs = ts,
            cueText = payload.sentence.take(120),
            createdAt = Clock.System.now().toString()
        )
        miningEvents.removeAll { it.cardId == card.id }
        miningEvents.add(0, event)
        while (miningEvents.size > 500) miningEvents.removeAt(miningEvents.lastIndex)
        save()
        // Knowledge ⇄ media bridge (spec §28, ADR-0013): surface the real
        // card id so the core media-reference store can record a MINED
        // reference and the node layer can build the mined_from edge.
        onMined?.invoke(event)
    }

    /**
     * Re-open the media a mined card came from, at its timestamp.
     * Returns false when no source media is recorded / found.
     */
    fun openFromCard(card: DesktopCard): Boolean {
        val event = miningEvents.firstOrNull { it.cardId == card.id }
        val path = event?.mediaPath ?: card.note.lineSequence()
            .firstOrNull { it.startsWith("Source:") }?.removePrefix("Source:")?.trim()
        if (path.isNullOrBlank()) return false
        val item = library.itemByPath(path)
            ?: if (path.startsWith("http://") || path.startsWith("https://")) {
                library.addRemote(path, path.substringAfterLast('/').substringBefore('?').ifBlank { path }, MediaKind.Video)
            } else {
                library.addFile(File(path))
            }
            ?: return false
        openItem(item, resume = false)
        val tsMs = event?.timestampMs
            ?: card.note.lineSequence().firstOrNull { it.startsWith("Timestamp:") }
                ?.removePrefix("Timestamp:")?.trim()?.toDoubleOrNull()?.times(1000)?.toLong()
                ?: 0L
        if (tsMs > 0) {
            seekTo(tsMs)
            play()
        }
        state.currentView = ua.syt0r.kanji.desktop.appstate.WorkspaceView.Media
        return true
    }

    /** Look up whatever is on the system clipboard right now. */
    fun lookupClipboard() {
        val text = runCatching {
            java.awt.Toolkit.getDefaultToolkit().systemClipboard
                .getData(java.awt.datatransfer.DataFlavor.stringFlavor) as? String
        }.getOrNull()?.trim()
        if (text.isNullOrBlank()) {
            state.toastHost.show("Clipboard is empty or not text", kind = ToastKind.Info)
            return
        }
        val clean = SubtitleNormalizer.normalizeForLookup(text)
        if (clean.isBlank()) return
        lookupText(clean.take(120))
        state.toastHost.show("Clipboard lookup: ${clean.take(40)}", kind = ToastKind.Info)
    }

    /** Capture the current frame and run OCR on it (anime without subtitles). */
    fun ocrFrame() {
        val shot = captureScreenshot() ?: return
        val file = File(shot)
        if (!file.exists()) return
        val result = state.ocr.ocrImage(file, "jpn")
        val text = result.text.trim()
        if (text.isBlank() || text.startsWith("OCR backend unavailable")) {
            state.toastHost.show(
                if (text.isBlank()) "OCR returned no text" else text,
                kind = ToastKind.Warning
            )
            return
        }
        lookupText(text.lines().first().take(120))
        state.toastHost.show("OCR: ${text.lines().first().take(60)}", kind = ToastKind.Success)
    }

    /** Open the mining dialog pre-filled with the current cue + token. */
    fun mineCue(cue: SubtitleCue, token: SegmentToken? = null, phraseTokens: List<SegmentToken>? = null) {
        val captureShot = state.settings.getBool("media.mine-screenshot")
        val captureAudio = state.settings.getBool("media.mine-audio")
        val deckId = state.settings.getString("media.mine-deck", DesktopCard.DEFAULT_DECK_ID)

        // Capture assets silently when the user enabled them.
        val captureVideo = state.settings.getBool("media.mine-video")
        var shot: String? = null
        var audio: String? = null
        var video: String? = null
        if (captureShot && can(PlaybackCapability.CanScreenshot) && isPlaying) {
            shot = captureScreenshot()
        }
        if (captureAudio && MediaCapture.ffmpegAvailable) {
            audio = captureAudioClip(cue)
        }
        if (captureVideo && MediaCapture.ffmpegAvailable) {
            video = captureVideoClip(cue, state.settings.getInt("media.audio-padding-ms", 200).toLong())
        }

        val payload = payloadForCue(
            cue = cue,
            token = token,
            phraseTokens = phraseTokens,
            screenshotPath = shot,
            audioPath = audio,
            videoPath = video,
            includeScreenshot = captureShot,
            includeAudio = captureAudio,
            deckId = deckId.ifBlank { DesktopCard.DEFAULT_DECK_ID }
        )
        lastMinedPayload = payload
        state.mining.openMining(payload)
        state.activityLog.record(
            ActivityCategory.Study,
            "Mining \"${payload.headword}\" from ${currentItem?.name ?: "media"}",
            details = cue.text.take(80)
        )
    }

    /** Mine the current cue — the whole selected phrase when several tokens are selected. */
    fun mineCurrentCue() {
        val cue = activeCue ?: return
        if (selectedTokens.size > 1) mineCue(cue, null, selectedTokens)
        else mineCue(cue, selectedTokens.firstOrNull())
    }

    /**
     * Bulk-mine several subtitle lines straight into the card pool (no dialog
     * per line). Uses the configured default deck; duplicates are skipped per
     * the duplicate policy. Ideal for a whole re-watch session.
     */
    fun bulkMine(cues: List<SubtitleCue>) {
        if (cues.isEmpty()) return
        val deckId = state.settings.getString("media.mine-deck", DesktopCard.DEFAULT_DECK_ID)
            .ifBlank { DesktopCard.DEFAULT_DECK_ID }
        var created = 0
        var duplicates = 0
        cues.forEach { cue ->
            val existed = state.cards.any { it.note.contains("Sentence: ${displayTextFor(cue).take(60)}") }
            val payload = payloadForCue(cue, null, includeScreenshot = false, includeAudio = false, includeVideo = false, deckId = deckId)
            state.mining.mine(payload)
            if (existed) duplicates++ else created++
        }
        state.toastHost.show(
            "$created sentence${if (created == 1) "" else "s"} mined" +
                if (duplicates > 0) " · $duplicates duplicate${if (duplicates == 1) "" else "s"} skipped" else "",
            kind = if (created > 0) ToastKind.Success else ToastKind.Info
        )
        state.activityLog.record(ActivityCategory.Study, "Bulk-mined $created subtitles", details = currentItem?.name.orEmpty())
    }

    // ------------------------------------------------------------
    // Tick — called from the UI at ~10 Hz
    // ------------------------------------------------------------

    /**
     * Tick — called from the UI at ~10 Hz on the composition's coroutine
     * scope, so it MUST never throw: an uncaught exception here would tear
     * down the whole window. Every backend/subtitle/settings interaction
     * therefore funnels through [tickInternal]; a failure is surfaced as a
     * throttled toast + activity-log entry instead of propagating.
     */
    fun tick() {
        try {
            tickInternal()
        } catch (e: Throwable) {
            onTickFailure(e)
        }
    }

    private var lastTickFailureMs = 0L

    /** React to a tick failure without killing the application. */
    private fun onTickFailure(e: Throwable) {
        val now = System.currentTimeMillis()
        if (now - lastTickFailureMs > 10_000) {
            lastTickFailureMs = now
            state.activityLog.record(
                ActivityCategory.System,
                "Media tick recovered: ${e.message ?: e.javaClass.simpleName}"
            )
            state.toastHost.show(
                "Media engine hiccup: ${e.message?.take(80) ?: e.javaClass.simpleName}",
                kind = ToastKind.Warning
            )
        }
        // A backend that died mid-stream would poison every subsequent tick —
        // drop it so the next user action re-initializes from a clean slate.
        val backend = activeBackend
        if (backend != null && !backend.isAvailable) {
            runCatching { backend.close() }
            activeBackend = null
            backendKind = BackendKind.None
            if (playbackError == null) {
                playbackError = PlaybackError.Other(e.message ?: "Playback engine stopped unexpectedly")
            }
        }
    }

    private fun tickInternal() {
        // Keep the system tray in sync — started on the first loaded media
        // (guarded, so unsupported desktops never see it), then cheap updates.
        val item = currentItem
        if (item != null) {
            if (!trayShown && !trayUnsupported) {
                if (tray.supported) {
                    tray.start()
                    trayShown = true
                } else {
                    // Cache the negative so unsupported desktops aren't probed
                    // again on every tick.
                    trayUnsupported = true
                }
            }
            if (trayShown) {
                tray.label = item.name
                tray.setPlaying(isPlaying)
            }
            // Global media keys — start once on the first loaded media, then
            // track the setting. Unsupported OSes are cached so tick stays cheap.
            if (!systemKeysStarted && !systemKeysUnsupported &&
                state.settings.getBool("media.system-media-keys") &&
                systemMediaKeys.supported
            ) {
                startSystemMediaKeys()
            }
        }
        val backend = activeBackend ?: return
        val pos = backend.currentPositionMs()
        val dur = backend.durationMs()
        positionMs = pos
        if (dur > 0) durationMs = dur
        bufferedPositionMs = backend.bufferedPositionMs().coerceIn(pos, durationMs.coerceAtLeast(pos))

        // Current subtitle follows playback (binary search, no polling of the file).
        val cueIdx = subtitles.cueIndexAt(pos)
        if (cueIdx != activeCueIndex) {
            activeCueIndex = cueIdx
            activeCue = subtitles.cueAt(cueIdx)
            tokenCycleIndex = -1
            secondaryCue = if (subtitles.showSecondary) {
                subtitles.secondaryTrack?.track?.cueAt(pos - subtitles.globalOffsetMs - subtitles.secondaryOffsetMs)
            } else null
        }

        // Condensed playback: skip (or fast-forward through) unsubtitled gaps.
        if (condensedPlayback && isPlaying && activeCue == null) {
            val next = subtitles.nextCue(pos)
            val gapMs = state.settings.getInt("media.condensed-gap-ms", 4000)
            val gapStart = next?.startMs?.plus(subtitles.globalOffsetMs) ?: Long.MAX_VALUE
            if (next != null && gapStart - pos > gapMs) {
                if (condensedFastForward) {
                    // Advance through the gap in ~1 s chunks (~10× at the 10 Hz tick)
                    // — a true fast-forward, not an instant jump.
                    seekTo(pos + minOf(gapStart - pos, 1000L))
                } else {
                    seekTo(gapStart)
                }
                return
            }
        }

        // Auto-pause at cue boundaries.
        if (isPlaying && activeCue != null) {
            val cue = activeCue!!
            when (autoPauseMode) {
                AutoPauseMode.AtCueStart -> {
                    // Pause once when a NEW cue becomes active (asbplayer-style
                    // sentence-by-sentence processing).
                    if (cueIdx != lastAutoPauseIndex && cueIdx >= 0) {
                        lastAutoPauseIndex = cueIdx
                        pause()
                    }
                }
                AutoPauseMode.AtCueEnd -> if (pos >= cue.endMs + subtitles.globalOffsetMs - 40) pause()
                AutoPauseMode.BeforeCue -> {
                    val next = subtitles.nextCue(pos)
                    if (next != null && next.startMs + subtitles.globalOffsetMs - pos < 300) pause()
                }
                AutoPauseMode.Off -> Unit
            }
        }

        // Loop current cue / A-B range.
        if (loopMode == LoopMode.CurrentCue && activeCue != null) {
            if (pos >= loopEndMs) {
                seekTo(loopStartMs)
                if (replayRemaining > 0) {
                    replayRemaining -= 1
                    if (replayRemaining == 0) {
                        loopMode = LoopMode.Off
                        pause()
                    }
                }
            }
        } else if (loopMode == LoopMode.Range && loopEndMs > loopStartMs) {
            if (pos >= loopEndMs || pos < loopStartMs) seekTo(loopStartMs)
        }

        // Persist watch progress every ~5 seconds.
        if (pos - lastProgressSave > 5000 && currentItem != null) {
            lastProgressSave = pos
            library.updateProgress(currentItem!!.id, pos, durationMs)
        }
    }

    // ------------------------------------------------------------
    // Watch session accounting
    // ------------------------------------------------------------

    private fun flushWatchTime() {
        if (sessionStartMs == 0L) return
        val elapsed = System.currentTimeMillis() - sessionStartMs
        sessionStartMs = 0L
        if (elapsed <= 0) return
        sessionWatchMs += elapsed
        watchTimeMs += elapsed
        statistics.recordWatch(elapsed, study = studyMode)
        currentItem?.let { item ->
            library.updateProgress(item.id, positionMs, durationMs)
            library.recordHistory(item, subtitleUsed = subtitles.activeTrack?.name.orEmpty(), language = subtitles.activeTrack?.language.orEmpty())
            if (!sessionCounted) {
                sessionCounted = true
                library.bumpWatchCount(item.id)
            }
        }
        val label = currentItem?.name ?: "media"
        if (studyMode) {
            state.recordPracticeTime(elapsed.milliseconds)
            state.activityLog.record(ActivityCategory.Study, "Studied with \"$label\" for ${formatDuration(elapsed)}")
        } else {
            state.activityLog.record(ActivityCategory.System, "Watched \"$label\" for ${formatDuration(elapsed)}")
        }
    }

    fun closeMedia() {
        flushWatchTime()
        endOfEpisodeVisible = false
        if (trayShown) {
            tray.label = "—"
            tray.setPlaying(false)
        }
        activeBackend?.stop()
        activeBackend?.close()
        activeBackend = null
        backendKind = BackendKind.None
        currentItem = null
        subtitles.clear()
        activeCue = null
        activeCueIndex = -1
        positionMs = 0
        durationMs = 0
        isPlaying = false
        loopMode = LoopMode.Off
        playbackError = null
        clearLookup()
    }

    /** Backend probes for the settings screen. */
    fun probeBackends(): List<BackendProbe> = listOf(backends.probeVlc(), backends.probeMpv())

    // ------------------------------------------------------------
    // Bookmark / clip persistence (kept from the original workspace)
    // ------------------------------------------------------------

    /**
     * Hook fired when a bookmark is created. The Media Centre host wires it
     * to the core MediaReferenceStore so knowledge pages can show "Found in
     * your media" — the label (subtitle cue text when present) becomes the
     * Japanese text reference.
     */
    var onBookmarkCreated: ((MediaBookmark) -> Unit)? = null

    /**
     * Hook fired when a subtitle cue is mined into a card (the card id is
     * real — it is the desktop card that was just created). The Media
     * Centre host wires it to the core MediaReferenceStore so the node
     * layer can build a real mined_from edge (spec §28, ADR-0013).
     */
    var onMined: ((MediaMiningEvent) -> Unit)? = null

    fun addBookmark(label: String = "") {
        val item = currentItem ?: return
        val bm = MediaBookmark(
            id = "bm-${System.currentTimeMillis()}",
            mediaPath = item.path,
            timestampMs = positionMs,
            label = label.ifBlank { activeCue?.text?.take(40) ?: "Bookmark" },
            createdAt = Clock.System.now().toString()
        )
        bookmarks.add(bm)
        save()
        onBookmarkCreated?.invoke(bm)
        state.toastHost.show("Bookmarked at ${formatTime(positionMs)}", kind = ToastKind.Success)
    }

    fun removeBookmark(id: String) {
        bookmarks.removeAll { it.id == id }
        save()
    }

    fun removeClip(id: String) {
        audioClips.removeAll { it.id == id }
        save()
    }

    fun setScreenshot(path: String?) {
        lastScreenshotPath = path
        if (path != null) save()
    }

    // ------------------------------------------------------------
    // Keyboard control (works while focus is anywhere in Media)
    // ------------------------------------------------------------

    /**
     * Media-first hotkeys. Only consulted when the Media workspace is the
     * active view, so review/browser shortcuts are never shadowed.
     *
     * Keys are resolved through the configurable [hotkeys] catalog, so every
     * action is rebindable from Media → Settings → Keyboard shortcuts and
     * there is exactly one shortcut system for the media workspace.
     * Escape keeps its contextual behaviour (close popup → exit fullscreen
     * → collapse panels) and is deliberately not rebindable.
     */
    fun handleKey(key: String, ctrl: Boolean, shift: Boolean, alt: Boolean, meta: Boolean): Boolean {
        // Never steal keys while the user is typing (transcript search etc.).
        if (textInputFocused) return false
        if (key == "escape" && !ctrl && !shift && !alt && !meta) {
            handleEscape()
            return true
        }
        val action = hotkeys.actionForPressed(key, ctrl, shift, alt, meta) ?: return false
        dispatchAction(action.id)
        return true
    }

    /** Execute a media action by its catalog id. */
    private fun dispatchAction(actionId: String) {
        when (actionId) {
            "play-pause" -> togglePlay()
            "seek-back" -> seekBy(-seekAmountMs)
            "seek-forward" -> seekBy(seekAmountMs)
            "seek-back-30s" -> seekBy(-30000)
            "seek-forward-30s" -> seekBy(30000)
            "cycle-word-back" -> cycleToken(-1)
            "cycle-word-forward" -> cycleToken(1)
            "volume-up" -> updateVolume(volume + 5)
            "volume-down" -> updateVolume(volume - 5)
            "mine" -> mineCurrentCue()
            "dictionary" -> toggleDictionary()
            "transcript" -> transcriptOpen = !transcriptOpen
            "subtitles" -> subtitleVisible = !subtitleVisible
            "library" -> libraryOpen = !libraryOpen
            "replay" -> replayCue()
            "loop" -> toggleLoopCue()
            "set-loop-a" -> setLoopPointA()
            "set-loop-b" -> setLoopPointB()
            "screenshot" -> captureScreenshot()
            "condensed" -> toggleCondensed()
            "capture-audio" -> if (activeCue != null) captureAudioClip(activeCue!!)
            "bookmark" -> addBookmark()
            "study-mode" -> toggleStudyMode()
            "next" -> playNext()
            "previous" -> playPrevious()
            "next-cue" -> replayNextCue()
            "prev-cue" -> replayPreviousCue()
            // ---- Rendering / audio transport ---------------------------
            "mute" -> toggleMute()
            "fullscreen" -> toggleFullscreen()
            "subtitle-delay-back" -> adjustSubtitleDelay(-500)
            "subtitle-delay-reset" -> setSubtitleDelay(0)
            "subtitle-delay-forward" -> adjustSubtitleDelay(500)
            "speed-down" -> cycleSpeed(-1)
            "speed-up" -> cycleSpeed(1)
            "frame-step-back" -> frameStepBackward()
            "frame-step-forward" -> frameStepForward()
            "chapter-previous" -> previousChapter()
            "chapter-next" -> nextChapter()
            "cycle-display" -> cycleDisplayMode()
            "cycle-aspect" -> cycleAspectRatio()
        }
    }

    // ------------------------------------------------------------
    // Rendering / audio extras (shortcut targets)
    // ------------------------------------------------------------

    /** Step through the standard speed ladder; 1x is one key away. */
    private fun cycleSpeed(dir: Int) {
        val ladder = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        val idx = ladder.indexOfFirst { it >= speed - 0.001f }
        val next = when {
            idx < 0 -> if (dir > 0) 0 else ladder.lastIndex
            else -> (idx + dir).coerceIn(0, ladder.lastIndex)
        }
        updateSpeed(ladder[next])
        state.toastHost.show("Speed: ${ladder[next]}×", kind = ToastKind.Info)
    }

    private fun toggleDictionary() {
        if (lookupQuery == null && activeCue != null) {
            lookupQuery = selectedTokens.firstOrNull()?.surface ?: activeCue!!.text.take(60)
        }
        dictionaryOpen = !dictionaryOpen
    }

    /** Palette-friendly: open the dictionary with the current selection. */
    fun toggleDictionaryFromPalette() {
        if (lookupQuery == null && activeCue != null) {
            lookupQuery = selectedTokens.firstOrNull()?.surface ?: activeCue!!.text.take(60)
        }
        dictionaryOpen = true
    }

    private fun handleEscape() {
        when {
            settingsOpen -> settingsOpen = false
            dictionaryOpen -> clearLookup()
            fullscreenActive -> toggleFullscreen()
            else -> {
                transcriptOpen = false
                libraryOpen = false
            }
        }
    }

    /** Toggle fullscreen intent — the UI applies it to the host window. */
    fun toggleFullscreen() {
        fullscreenActive = !fullscreenActive
    }

    // ------------------------------------------------------------
    // Shutdown (called when the app closes)
    // ------------------------------------------------------------

    fun shutdown() {
        flushWatchTime()
        stopIntegrations()
        backends.shutdownAll()
        activeBackend = null
        tray.stop()
        trayShown = false
    }

    // ------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------

    private fun load() {
        if (!stateFile.exists()) return
        runCatching {
            val dto = json.decodeFromString<MediaStateDto>(stateFile.readText())
            bookmarks.clear(); bookmarks.addAll(dto.bookmarks)
            audioClips.clear(); audioClips.addAll(dto.clips)
            recentFiles.clear(); recentFiles.addAll(dto.recentFiles)
            miningEvents.clear(); miningEvents.addAll(dto.miningEvents)
        }
    }

    private fun save() {
        runCatching {
            stateFile.writeText(
                json.encodeToString(
                    MediaStateDto(
                        bookmarks.toList(),
                        audioClips.toList(),
                        recentFiles.toList(),
                        miningEvents.toList()
                    )
                )
            )
        }
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    fun formatDuration(ms: Long): String {
        val totalMinutes = (ms / 60000).coerceAtLeast(0)
        return if (totalMinutes >= 60) {
            "${totalMinutes / 60}h ${totalMinutes % 60}m"
        } else "${totalMinutes}m"
    }

    /** Backend track lists for the UI dropdowns. */
    val videoTracks: List<MediaTrackInfo> get() = activeBackend?.availableTracks()?.filter { it.kind == TrackKind.Video } ?: emptyList()
    val audioTracks: List<MediaTrackInfo> get() = activeBackend?.availableTracks()?.filter { it.kind == TrackKind.Audio } ?: emptyList()
    val subtitleTracks: List<MediaTrackInfo> get() = activeBackend?.availableTracks()?.filter { it.kind == TrackKind.Subtitle } ?: emptyList()

    fun can(flag: PlaybackCapability): Boolean = activeBackend?.capabilities?.contains(flag) == true

    companion object {
        fun formatTime(ms: Long): String {
            val total = ms.coerceAtLeast(0)
            val h = total / 3600000
            val m = (total % 3600000) / 60000
            val s = (total % 60000) / 1000
            return if (h > 0) "%d:%02d:%02d".format(h, m, s)
            else "%02d:%02d".format(m, s)
        }

        fun durationForRate(ms: Long, rate: Float): Long =
            (ms / rate.coerceAtLeast(0.1f)).roundToInt().toLong()

        /**
         * Canonical screenshot filename: Kaiteyo_<media>_<HH-MM-SS>.<ext>.
         * The media name is sanitized to letters/numbers/space/_/-, and
         * "jpeg" normalizes to the .jpg extension.
         */
        fun screenshotFileName(mediaName: String, positionMs: Long, format: String): String {
            val ext = when (format.lowercase()) {
                "jpg", "jpeg" -> "jpg"
                "png" -> "png"
                else -> "png"
            }
            val name = mediaName.substringBeforeLast('.')
                .replace(Regex("[^\\p{L}\\p{N} _-]"), "")
                .trim()
                .ifBlank { "media" }
            val stamp = formatTime(positionMs).replace(":", "-")
            return "Kaiteyo_${name}_$stamp.$ext"
        }
    }
}
