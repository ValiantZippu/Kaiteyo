package ua.syt0r.kanji.desktop.engine.playback

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.Channels
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

// ============================================
// MPV BACKEND
// Real mpv integration: Kaiteyo launches mpv as
// a child process with --input-ipc-server and
// drives it over JSON-RPC (Unix socket on
// Linux/macOS, named pipe on Windows). mpv owns
// its own rendering window; every transport
// control (play/pause/seek/speed/volume/tracks/
// screenshots/chapters) goes through the IPC
// interface — never stdout scraping.
// ============================================

/**
 * Thin JSON-RPC line client for mpv's IPC interface. One request/response
 * per line, plus asynchronous events pushed to [onEvent].
 */
internal class MpvIpcClient(
    private val ipcPath: String,
    private val onEvent: (JsonObject) -> Unit
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val requestId = AtomicLong(0)
    private val pending = ConcurrentHashMap<Long, MutableList<String>>()

    private var socket: SocketChannel? = null
    private var writer: BufferedWriter? = null
    private var readerThread: Thread? = null
    private var closed = false

    fun connect(): Result<Unit> = runCatching {
        if (isWindows()) {
            connectWindowsNamedPipe()
        } else {
            socket = SocketChannel.open(StandardProtocolFamily.UNIX).also { ch ->
                ch.connect(UnixDomainSocketAddress.of(Path.of(ipcPath)))
            }
            writer = BufferedWriter(OutputStreamWriter(Channels.newOutputStream(socket!!), StandardCharsets.UTF_8))
            startReader(Channels.newInputStream(socket!!))
        }
    }

    private fun connectWindowsNamedPipe() {
        val handle = WindowsNamedPipe.open(ipcPath) ?: error("Could not open mpv named pipe at $ipcPath")
        WindowsNamedPipe.setupStreams(handle) { stream ->
            startReader(stream)
        }
    }

    private fun startReader(input: java.io.InputStream) {
        val reader = BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8))
        readerThread = Thread({
            try {
                while (!closed) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) continue
                    val obj = runCatching { json.parseToJsonElement(line).jsonObject }.getOrNull() ?: continue
                    val id = obj["request_id"]?.jsonPrimitive?.longOrNull
                    if (id != null) {
                        pending[id]?.add(line)
                    } else if (obj["event"] != null) {
                        onEvent(obj)
                    }
                }
            } catch (_: Exception) {
                // connection closed
            }
        }, "kaiteyo-mpv-ipc").apply { isDaemon = true; start() }
    }

    /** Send a command and block for its response. */
    @Synchronized
    fun command(vararg args: Any): JsonObject? {
        val id = requestId.incrementAndGet()
        val request = buildString {
            append("{\"command\":[")
            append(args.joinToString(",") { a -> quote(a) })
            append("],\"request_id\":").append(id).append("}\n")
        }
        pending[id] = mutableListOf()
        val w = writer ?: return null
        try {
            w.write(request)
            w.flush()
        } catch (_: Exception) {
            pending.remove(id)
            return null
        }
        // Block until the response line arrives.
        val deadline = System.currentTimeMillis() + 8000
        while (System.currentTimeMillis() < deadline) {
            val lines = pending[id]
            if (!lines.isNullOrEmpty()) {
                pending.remove(id)
                return runCatching { json.parseToJsonElement(lines[0]).jsonObject }.getOrNull()
            }
            Thread.sleep(5)
        }
        pending.remove(id)
        return null
    }

    fun setProperty(name: String, value: Any) {
        command("set_property", name, value)
    }

    fun observeProperty(id: Int, name: String) {
        command("observe_property", id, name)
    }

    fun close() {
        closed = true
        runCatching { writer?.close() }
        runCatching { socket?.close() }
        runCatching { readerThread?.interrupt() }
        WindowsNamedPipe.closeAll()
    }

    private fun quote(value: Any): String = when (value) {
        is String -> "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        is Boolean -> value.toString()
        is Int -> value.toString()
        is Long -> value.toString()
        is Float -> value.toString()
        is Double -> value.toString()
        else -> "\"$value\""
    }

    companion object {
        fun isWindows(): Boolean = System.getProperty("os.name", "").lowercase().contains("win")
    }
}

/**
 * Windows named-pipe transport using JNA (Kernel32). Provides blocking
 * read via a dedicated thread so the IPC client behaves like a socket.
 */
internal object WindowsNamedPipe {
    private val streams = mutableListOf<Pair<Any, java.io.InputStream>>()
    private val lock = Any()

    fun open(pipePath: String): Any? = runCatching {
        val kernel32 = com.sun.jna.platform.win32.Kernel32.INSTANCE
        val handle = kernel32.CreateFile(
            pipePath,
            com.sun.jna.platform.win32.WinNT.GENERIC_READ or com.sun.jna.platform.win32.WinNT.GENERIC_WRITE,
            0, null, com.sun.jna.platform.win32.WinNT.OPEN_EXISTING, 0, null
        )
        if (com.sun.jna.platform.win32.WinNT.INVALID_HANDLE_VALUE.equals(handle)) null else handle
    }.getOrNull()

    fun setupStreams(handle: Any, onInput: (java.io.InputStream) -> Unit) {
        val h = handle as com.sun.jna.platform.win32.WinNT.HANDLE
        synchronized(lock) {
            val stream = PipeInputStream(h)
            streams.add(handle to stream)
            onInput(stream)
        }
    }

    fun closeAll() {
        synchronized(lock) {
            streams.forEach { (h, _) -> runCatching { (h as com.sun.jna.platform.win32.WinNT.HANDLE).setPointer(com.sun.jna.Pointer.NULL) } }
            streams.clear()
        }
    }

    /** Blocking InputStream over Kernel32 ReadFile. */
    private class PipeInputStream(private val handle: com.sun.jna.platform.win32.WinNT.HANDLE) : java.io.InputStream() {
        private val kernel32 = com.sun.jna.platform.win32.Kernel32.INSTANCE
        private val buffer = ByteArray(8192)
        private val readPtr = com.sun.jna.ptr.IntByReference()
        private var pos = 0
        private var len = 0

        override fun read(): Int {
            if (pos >= len) {
                if (!fill()) return -1
            }
            return buffer[pos++].toInt() and 0xFF
        }

        override fun read(b: ByteArray, off: Int, l: Int): Int {
            if (l == 0) return 0
            if (pos >= len && !fill()) return -1
            val n = minOf(l, len - pos)
            System.arraycopy(buffer, pos, b, off, n)
            pos += n
            return n
        }

        private fun fill(): Boolean {
            if (!kernel32.ReadFile(handle, buffer, buffer.size, readPtr, null)) return false
            val n = readPtr.value
            if (n <= 0) return false
            pos = 0
            len = n
            return true
        }

        override fun close() = Unit
    }
}

// ============================================
// MPV process + backend
// ============================================

class MpvBackend(
    private val mpvExecutable: String,
    private val ipcPathOverride: String? = null
) : PlaybackBackend {

    override val kind: BackendKind = BackendKind.Mpv
    override val capabilities: Set<PlaybackCapability> = setOf(
        PlaybackCapability.CanSeek,
        PlaybackCapability.CanFrameAccurateSeek,
        PlaybackCapability.CanChangeSpeed,
        PlaybackCapability.CanSelectSubtitle,
        PlaybackCapability.CanSelectAudio,
        PlaybackCapability.CanVolume,
        PlaybackCapability.CanMute,
        PlaybackCapability.CanScreenshot,
        PlaybackCapability.CanChapters,
        PlaybackCapability.CanLoop,
        PlaybackCapability.CanAspectRatio,
        PlaybackCapability.CanDisplayMode,
        PlaybackCapability.CanVideoAdjustments,
        PlaybackCapability.CanDeinterlace,
        PlaybackCapability.CanAudioDelay,
        PlaybackCapability.CanAudioChannel
        // Note: mpv has no native equalizer — CanEqualizer is deliberately
        // absent so the UI shows the panel disabled with an explanation
        // instead of pretending it works.
    )
    override var listener: ((PlaybackEvent) -> Unit)? = null
    override val isAvailable: Boolean get() = File(mpvExecutable).canExecute()
    override val diagnosticName: String get() = "mpv ($mpvExecutable)"

    private var process: Process? = null
    private var client: MpvIpcClient? = null
    private val json = Json { ignoreUnknownKeys = true }

    private var loadedPath: String? = null
    private var tracks: List<MediaTrackInfo> = emptyList()
    private var chapterList: List<PlaybackChapter> = emptyList()
    private var eofReached = false

    /**
     * Optional GLSL shader file (e.g. an Anime4K pipeline) passed to mpv as
     * --glsl-shaders. Set before open() for the shader to be applied.
     */
    @Volatile
    var shaderPath: String? = null

    // Mirrored mpv state (updated from property events; used for instant UI).
    @Volatile private var positionMs: Long = 0
    @Volatile private var durationMs: Long = 0
    @Volatile private var playing: Boolean = false
    @Volatile private var buffering: Boolean = false
    @Volatile private var volume: Int = 100
    @Volatile private var muted: Boolean = false
    @Volatile private var speed: Float = 1f

    private fun ipcPath(): String {
        ipcPathOverride?.let { return it }
        return if (MpvIpcClient.isWindows()) {
            "\\\\.\\pipe\\kaiteyo-mpv-${ProcessHandle.current().pid()}"
        } else {
            "${System.getProperty("java.io.tmpdir")}/kaiteyo-mpv-${ProcessHandle.current().pid()}.sock"
        }
    }

    override fun open(source: String): Result<Unit> = runCatching {
        close()
        val path = ipcPath()
        File(path).toPath().let { runCatching { Files.deleteIfExists(it) } }
        val command = mutableListOf(
            mpvExecutable,
            "--input-ipc-server=$path",
            "--no-terminal",
            "--really-quiet",
            "--keep-open=no",
            "--force-window=yes",
            "--save-position-on-quit=no",
            "--volume=$volume",
            "--speed=$speed"
        )
        shaderPath?.takeIf { File(it).exists() }?.let { shader ->
            command.add("--glsl-shaders=$shader")
        }
        if (source.startsWith("http://") || source.startsWith("https://")) {
            command.add(source)
        } else {
            command.add("--")
            command.add(source)
        }
        val pb = ProcessBuilder(command)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        process = pb.start()
        Thread.sleep(1200) // wait for the IPC socket to appear

        val c = MpvIpcClient(path) { ev -> handleEvent(ev) }
        c.connect().getOrElse { throw it }
        client = c
        loadedPath = source
        c.observeProperty(1, "time-pos")
        c.observeProperty(2, "duration")
        c.observeProperty(3, "pause")
        c.observeProperty(4, "speed")
        c.observeProperty(5, "volume")
        c.observeProperty(6, "mute")
        c.observeProperty(7, "track-list")
        c.observeProperty(8, "chapter")
        c.observeProperty(9, "eof-reached")
        c.observeProperty(10, "core-idle")
        c.observeProperty(11, "video-params")
        listener?.invoke(PlaybackEvent(PlaybackEventType.MediaLoaded))
    }

    private fun handleEvent(event: JsonObject) {
        val name = event["name"]?.jsonPrimitive?.content ?: return
        val data = event["data"]
        when (name) {
            "time-pos" -> {
                val sec = data?.jsonPrimitive?.doubleOrNull ?: return
                positionMs = (sec * 1000).toLong()
                listener?.invoke(PlaybackEvent(PlaybackEventType.PositionChanged, positionMs = positionMs))
            }
            "duration" -> durationMs = ((data?.jsonPrimitive?.doubleOrNull ?: 0.0) * 1000).toLong()
            "pause" -> {
                playing = data?.jsonPrimitive?.booleanOrNull != true
                listener?.invoke(
                    if (playing) PlaybackEvent(PlaybackEventType.Started)
                    else PlaybackEvent(PlaybackEventType.Paused, positionMs = positionMs)
                )
            }
            "speed" -> speed = data?.jsonPrimitive?.floatOrNull ?: 1f
            "volume" -> volume = data?.jsonPrimitive?.intOrNull ?: 100
            "mute" -> muted = data?.jsonPrimitive?.booleanOrNull ?: false
            "track-list" -> tracks = parseTracks(data?.jsonArray)
            "chapter" -> listener?.invoke(PlaybackEvent(PlaybackEventType.ChapterChanged, positionMs = positionMs))
            "core-idle" -> {
                buffering = data?.jsonPrimitive?.booleanOrNull == true && loadedPath != null && !eofReached
                listener?.invoke(
                    if (buffering) PlaybackEvent(PlaybackEventType.Buffering)
                    else PlaybackEvent(PlaybackEventType.BufferingEnded)
                )
            }
            "eof-reached" -> {
                eofReached = data?.jsonPrimitive?.booleanOrNull == true
                if (eofReached) listener?.invoke(PlaybackEvent(PlaybackEventType.Completed, positionMs = durationMs))
            }
        }
        when (event["event"]?.jsonPrimitive?.content) {
            "file-loaded" -> {
                eofReached = false
                val tracksResp = client?.command("get_property", "track-list")
                tracks = parseTracks(tracksResp?.get("data")?.jsonArray)
                val chaptersResp = client?.command("get_property", "chapter-list")
                chapterList = parseChapters(chaptersResp?.get("data")?.jsonArray)
                val dur = client?.command("get_property", "duration")
                durationMs = ((dur?.get("data")?.jsonPrimitive?.doubleOrNull ?: 0.0) * 1000).toLong()
            }
            "end-file" -> {
                val reason = event["reason"]?.jsonPrimitive?.content
                if (reason == "eof") {
                    listener?.invoke(PlaybackEvent(PlaybackEventType.Completed, positionMs = durationMs))
                }
            }
            "seek" -> listener?.invoke(PlaybackEvent(PlaybackEventType.Seeked, positionMs = positionMs))
        }
    }

    private fun parseTracks(array: JsonArray?): List<MediaTrackInfo> = array.orEmpty().mapNotNull { el ->
        val obj = el.jsonObject
        val id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
        val type = obj["type"]?.jsonPrimitive?.content
        val kind = when (type) {
            "video" -> TrackKind.Video
            "audio" -> TrackKind.Audio
            "sub" -> TrackKind.Subtitle
            else -> null
        } ?: return@mapNotNull null
        MediaTrackInfo(
            id = id.toString(),
            kind = kind,
            title = obj["title"]?.jsonPrimitive?.content ?: "",
            language = obj["lang"]?.jsonPrimitive?.content ?: ""
        )
    }

    private fun parseChapters(array: JsonArray?): List<PlaybackChapter> = array.orEmpty().mapNotNull { el ->
        val obj = el.jsonObject
        val title = obj["title"]?.jsonPrimitive?.content ?: "Chapter"
        val time = obj["time"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
        PlaybackChapter("ch-${(time * 1000).toLong()}", title, (time * 1000).toLong())
    }

    override fun play() {
        client?.setProperty("pause", false)
    }

    override fun pause() {
        client?.setProperty("pause", true)
    }

    override fun stop() {
        client?.command("stop")
        listener?.invoke(PlaybackEvent(PlaybackEventType.Stopped))
    }

    override fun seekTo(ms: Long) {
        client?.setProperty("time-pos", ms / 1000.0)
    }

    override fun setSpeed(rate: Float) {
        client?.setProperty("speed", rate.coerceIn(0.25f, 2f))
    }

    override fun setVolume(percent: Int) {
        client?.setProperty("volume", percent.coerceIn(0, 100))
    }

    override fun setMuted(muted: Boolean) {
        client?.setProperty("mute", muted)
    }

    override fun setLoop(loop: Boolean) {
        client?.setProperty("loop-file", if (loop) "inf" else "no")
    }

    /** Map the user's performance profile onto mpv's decode/vsync options. */
    override fun setPerformanceProfile(profile: String) {
        when (profile) {
            "battery" -> {
                client?.setProperty("hwdec", "no")
                client?.setProperty("video-sync", "display-resample")
            }
            "quality" -> {
                client?.setProperty("hwdec", "auto")
                client?.setProperty("video-sync", "audio")
            }
            else -> {
                client?.setProperty("hwdec", "auto-safe")
                client?.setProperty("video-sync", "audio")
            }
        }
    }

    override fun availableTracks(): List<MediaTrackInfo> = tracks

    override fun selectTrack(trackId: String?) {
        if (trackId == null) {
            client?.setProperty("sid", "no")
            client?.setProperty("aid", "no")
            return
        }
        val track = tracks.firstOrNull { it.id == trackId } ?: return
        when (track.kind) {
            TrackKind.Video -> client?.setProperty("vid", trackId)
            TrackKind.Subtitle -> client?.setProperty("sid", trackId)
            TrackKind.Audio -> client?.setProperty("aid", trackId)
            else -> Unit
        }
    }

    override fun setSubtitleDelay(delayMs: Long) {
        client?.setProperty("sub-delay", delayMs / 1000.0)
    }

    // ------------------------------------------------------------
    // Video rendering (display mode / aspect / adjustments)
    // ------------------------------------------------------------

    override fun setDisplayMode(mode: VideoDisplayMode) {
        when (mode) {
            VideoDisplayMode.Fit -> {
                client?.setProperty("video-unscaled", "no")
                client?.setProperty("panscan", 0.0)
                client?.setProperty("video-aspect-override", "no")
                client?.setProperty("video-zoom", 0.0)
            }
            VideoDisplayMode.Fill -> {
                client?.setProperty("video-unscaled", "no")
                client?.setProperty("panscan", 1.0)
                client?.setProperty("video-aspect-override", "no")
                client?.setProperty("video-zoom", 0.0)
            }
            VideoDisplayMode.Crop -> {
                // Crops overflow to the surface — panscan 1.0 fills and clips.
                client?.setProperty("video-unscaled", "no")
                client?.setProperty("panscan", 1.0)
                client?.setProperty("video-aspect-override", "no")
            }
            VideoDisplayMode.Original -> {
                client?.setProperty("video-unscaled", "yes")
                client?.setProperty("panscan", 0.0)
                client?.setProperty("video-zoom", 0.0)
            }
            VideoDisplayMode.Stretch -> {
                client?.setProperty("video-unscaled", "no")
                client?.setProperty("panscan", 0.0)
                client?.setProperty("video-aspect-override", "16:9")
            }
        }
    }

    override fun setAspectRatio(preset: AspectRatioPreset) {
        client?.setProperty("video-aspect-override", preset.value ?: "no")
    }

    override fun setVideoAdjustments(adjustments: VideoAdjustments) {
        // mpv ranges are -100..100 with 0 = neutral; ours are 0..200/100.
        client?.setProperty("brightness", adjustments.brightness - 100f)
        client?.setProperty("contrast", adjustments.contrast - 100f)
        client?.setProperty("saturation", adjustments.saturation - 100f)
        client?.setProperty("gamma", adjustments.gamma - 100f)
        client?.setProperty("hue", (adjustments.hue / 180f * 100f))
        setDeinterlace(adjustments.deinterlace)
    }

    override fun setDeinterlace(enabled: Boolean) {
        client?.setProperty("deinterlace", if (enabled) "yes" else "no")
    }

    // ------------------------------------------------------------
    // Audio extras (delay / channel — output & EQ unsupported)
    // ------------------------------------------------------------

    override fun setAudioDelay(delayMs: Long) {
        client?.setProperty("audio-delay", delayMs / 1000.0)
    }

    override fun setAudioChannel(channel: AudioChannelPreset) {
        val value = when (channel) {
            AudioChannelPreset.Stereo -> "stereo"
            AudioChannelPreset.ReverseStereo -> "stereo"
            AudioChannelPreset.Left -> "left"
            AudioChannelPreset.Right -> "right"
            AudioChannelPreset.Mono -> "mono"
            AudioChannelPreset.Headphones -> "stereo"
        }
        client?.setProperty("audio-channels", value)
    }

    override fun bufferedPositionMs(): Long {
        // mpv demuxer-cache-time is seconds ahead of the playhead; the
        // observed buffer extends from the current position.
        val resp = client?.command("get_property", "demuxer-cache-time")
        val sec = resp?.get("data")?.jsonPrimitive?.doubleOrNull ?: return super.bufferedPositionMs()
        if (sec <= 0) return super.bufferedPositionMs()
        return (positionMs + (sec * 1000).toLong()).coerceAtMost(durationMs.coerceAtLeast(positionMs))
    }

    override fun frameStepForward(): Boolean {
        client?.command("frame-step")
        return true
    }

    override fun frameStepBackward(): Boolean {
        client?.command("frame-back-step")
        return true
    }

    override fun snapshot(target: File): Result<String> = runCatching {
        target.parentFile?.mkdirs()
        val resp = client?.command("screenshot-to-file", target.absolutePath, "video")
        if (resp == null) error("mpv IPC unavailable")
        target.absolutePath
    }

    override fun chapters(): List<PlaybackChapter> = chapterList

    override fun currentPositionMs(): Long = positionMs

    override fun durationMs(): Long = durationMs

    override val isPlaying: Boolean get() = playing

    override val isBuffering: Boolean get() = buffering

    override fun close() {
        runCatching { client?.close() }
        client = null
        runCatching { process?.destroy() }
        runCatching { process?.waitFor(1500, java.util.concurrent.TimeUnit.MILLISECONDS) }
        runCatching { process?.destroyForcibly() }
        process = null
        loadedPath = null
        tracks = emptyList()
        chapterList = emptyList()
        listener?.invoke(PlaybackEvent(PlaybackEventType.MediaUnloaded))
    }
}
