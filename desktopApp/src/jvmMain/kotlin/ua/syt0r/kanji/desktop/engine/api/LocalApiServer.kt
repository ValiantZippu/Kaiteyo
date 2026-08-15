package ua.syt0r.kanji.desktop.engine.api

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.request.receiveText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ua.syt0r.kanji.desktop.engine.media.MediaEngine
import ua.syt0r.kanji.desktop.engine.mining.MiningEngine
import ua.syt0r.kanji.desktop.engine.mining.MiningPayload
import ua.syt0r.kanji.desktop.engine.settings.SettingsEngine
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.path

// ============================================
// KAITEYO LOCAL INTEGRATION API
// A local HTTP API (default http://127.0.0.1:48201)
// that lets external apps �?" most importantly
// GameSentenceMiner �?" send word/reading/definition/
// sentence/screenshot/audio data straight into the
// card creation workflow. Uses only the already-
// available Ktor netty server + kotlinx.serialization.
// ============================================

/** Request payload accepted by the card-creation endpoint. */
@Serializable
data class IntegrationCardRequest(
    val word: String,
    val reading: String = "",
    val definition: String = "",
    val sentence: String = "",
    val screenshot: String? = null,
    val screenshotPath: String? = null,
    val audio: String? = null,
    val audioPath: String? = null,
    val timestamp: Double? = null,
    val source: String = "api",
    val tags: List<String> = emptyList(),
    val flags: List<String> = emptyList(),
    val notes: String = "",
    val deckId: String = ""
)

@Serializable
data class ApiStatusResponse(
    val app: String,
    val version: String,
    val endpoint: String,
    val reachable: Boolean
)

@Serializable
data class ApiMineResponse(
    val ok: Boolean,
    val cardId: String? = null,
    val message: String = ""
)

/** Full live player snapshot served by /api/player/state. */
@Serializable
data class PlayerStateResponse(
    val path: String,
    val name: String,
    val positionMs: Long,
    val durationMs: Long,
    val playing: Boolean,
    val buffering: Boolean,
    val speed: Float,
    val volume: Int,
    val backend: String,
    val subtitle: String,
    val subtitleStartMs: Long,
    val subtitleEndMs: Long,
    val selectedToken: String,
    val minedCount: Int
)

class LocalApiServer(
    private val mining: MiningEngine,
    private val media: MediaEngine? = null,
    private val settings: SettingsEngine = SettingsEngine(),
    private val port: Int = 48201
) {

    private val json = Json { ignoreUnknownKeys = true }

    var running by mutableStateOf(false)
    var lastError by mutableStateOf<String?>(null)
    var lastRequest by mutableStateOf<IntegrationCardRequest?>(null)

    /** Port actually bound on the last [start] (honors the media.api.port setting). */
    private var activePort: Int = port

    /**
     * Bearer token protecting every endpoint except /api/health. Generated once
     * on first use and persisted through settings, so external tools keep
     * working across restarts without re-authorization.
     */
    val token: String
        get() {
            val existing = settings.getString("media.api.token")
            if (existing.isNotBlank()) return existing
            val generated = java.util.UUID.randomUUID().toString().replace("-", "").take(24)
            settings.set("media.api.token", generated)
            return generated
        }

    @Volatile
    private var server: io.ktor.server.engine.EmbeddedServer<*, *>? = null

    fun start() {
        if (running) return
        try {
            activePort = settings.getInt("media.api.port", port)
            server = embeddedServer(Netty, port = activePort, host = "127.0.0.1", module = {
                // Security: everything except the plain /api/health liveness probe
                // requires the bearer token. External tools authenticate with
                // "Authorization: Bearer <token>" (shown in the Integrations hub).
                intercept(ApplicationCallPipeline.Call) {
                    val path = call.request.path()
                    if (path != "/api/health" && call.request.headers["Authorization"] != "Bearer $token") {
                        call.respondBytes(
                            json.encodeToString(
                                ApiMineResponse(false, message = "Unauthorized — send Authorization: Bearer <token>")
                            ).toByteArray(),
                            ContentType.Application.Json
                        )
                        finish()
                    }
                }
                routing {
                    get("/api/status") {
                        call.respondBytes(
                            json.encodeToString(
                                ApiStatusResponse(
                                    app = "Kaiteyo",
                                    version = "2.2.1",
                                    endpoint = "/api/mine",
                                    reachable = true
                                )
                            ).toByteArray(),
                            ContentType.Application.Json
                        )
                    }
                    get("/api/health") {
                        call.respondText("ok", ContentType.Text.Plain)
                    }
                    post("/api/mine") {
                        val body = call.receiveText()
                        val req = runCatching { json.decodeFromString<IntegrationCardRequest>(body) }.getOrNull()
                        if (req == null) {
                            call.respondBytes(
                                json.encodeToString(ApiMineResponse(false, message = "Invalid JSON payload")).toByteArray(),
                                ContentType.Application.Json
                            )
                            return@post
                        }
                        lastRequest = req
                        val card = mining.mine(req.toPayload())
                        call.respondBytes(
                            json.encodeToString(
                                ApiMineResponse(
                                    true,
                                    card?.id ?: "",
                                    if (card != null) "Card \"${req.word}\" created" else "Card \"${req.word}\" sent to Anki"
                                )
                            ).toByteArray(),
                            ContentType.Application.Json
                        )
                    }

                    // ---- Media / player control endpoints ------------------
                    get("/api/media/current") {
                        val m = media
                        val json = jsonResponse(
                            mapOf(
                                "path" to (m?.currentItem?.path ?: ""),
                                "name" to (m?.currentItem?.name ?: ""),
                                "kind" to (m?.currentItem?.kind?.name ?: ""),
                                "positionMs" to (m?.positionMs ?: 0),
                                "durationMs" to (m?.durationMs ?: 0),
                                "playing" to (m?.isPlaying ?: false),
                                "backend" to (m?.backendKind?.name ?: "None")
                            )
                        )
                        call.respondBytes(json, ContentType.Application.Json)
                    }

                    get("/api/media/subtitle") {
                        val m = media
                        val cue = m?.activeCue
                        call.respondBytes(
                            jsonResponse(
                                mapOf(
                                    "text" to (cue?.text ?: ""),
                                    "startMs" to (cue?.startMs ?: 0),
                                    "endMs" to (cue?.endMs ?: 0),
                                    "positionMs" to (m?.positionMs ?: 0)
                                )
                            ),
                            ContentType.Application.Json
                        )
                    }

                    post("/api/player/control") {
                        val body = call.receiveText()
                        val m = media ?: return@post call.respondBytes(
                            json.encodeToString(ApiMineResponse(false, message = "Media engine unavailable")).toByteArray(),
                            ContentType.Application.Json
                        )
                        val cmd = runCatching {
                            json.parseToJsonElement(body).jsonObject["command"]?.jsonPrimitive?.content
                        }.getOrNull()
                        when (cmd) {
                            "play" -> m.play()
                            "pause" -> m.pause()
                            "toggle" -> m.togglePlay()
                            "stop" -> m.stop()
                            "screenshot" -> m.captureScreenshot()
                            else -> Unit
                        }
                        call.respondBytes(
                            json.encodeToString(ApiMineResponse(true, message = "ok")).toByteArray(),
                            ContentType.Application.Json
                        )
                    }

                    post("/api/player/seek") {
                        val body = call.receiveText()
                        val m = media ?: return@post call.respondBytes(
                            json.encodeToString(ApiMineResponse(false, message = "Media engine unavailable")).toByteArray(),
                            ContentType.Application.Json
                        )
                        val params = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
                        val positionMs = params?.get("positionMs")?.jsonPrimitive?.longOrNull
                        val deltaMs = params?.get("deltaMs")?.jsonPrimitive?.longOrNull
                        if (positionMs != null) m.seekTo(positionMs)
                        else if (deltaMs != null) m.seekBy(deltaMs)
                        call.respondBytes(
                            json.encodeToString(ApiMineResponse(true, message = "ok")).toByteArray(),
                            ContentType.Application.Json
                        )
                    }

                    // ---- Player state + media overview ------------------
                    get("/api/player/state") {
                        val m = media
                        call.respondBytes(
                            json.encodeToString(
                                PlayerStateResponse(
                                    path = m?.currentItem?.path.orEmpty(),
                                    name = m?.currentItem?.name.orEmpty(),
                                    positionMs = m?.positionMs ?: 0,
                                    durationMs = m?.durationMs ?: 0,
                                    playing = m?.isPlaying ?: false,
                                    buffering = m?.buffering ?: false,
                                    speed = m?.speed ?: 1f,
                                    volume = m?.volume ?: 100,
                                    backend = m?.backendKind?.name ?: "None",
                                    subtitle = m?.activeCue?.text.orEmpty(),
                                    subtitleStartMs = m?.activeCue?.startMs ?: 0,
                                    subtitleEndMs = m?.activeCue?.endMs ?: 0,
                                    selectedToken = m?.selectedTokens?.firstOrNull()?.surface.orEmpty(),
                                    minedCount = m?.currentMinedCount ?: 0
                                )
                            ).toByteArray(),
                            ContentType.Application.Json
                        )
                    }

                    get("/api/media/library") {
                        val m = media
                        val query = call.request.queryParameters["query"].orEmpty()
                        val items = m?.library?.search(query)?.take(200) ?: emptyList()
                        call.respondBytes(json.encodeToString(items).toByteArray(), ContentType.Application.Json)
                    }

                    get("/api/media/history") {
                        val m = media
                        val entries = m?.library?.history?.toList() ?: emptyList()
                        call.respondBytes(json.encodeToString(entries).toByteArray(), ContentType.Application.Json)
                    }

                    get("/api/media/queue") {
                        val m = media
                        val idx = m?.queueIndex ?: -1
                        call.respondBytes(
                            jsonResponse(
                                mapOf(
                                    "size" to (m?.playQueue?.size ?: 0),
                                    "currentIndex" to idx,
                                    "currentId" to (m?.playQueue?.getOrNull(idx)?.id ?: ""),
                                    "nextId" to (m?.playQueue?.getOrNull(idx + 1)?.id ?: "")
                                )
                            ),
                            ContentType.Application.Json
                        )
                    }

                    get("/api/media/stats") {
                        val m = media
                        val s = m?.statistics
                        call.respondBytes(
                            jsonResponse(
                                mapOf(
                                    "totalWatchMs" to (s?.totalWatchMs ?: 0L),
                                    "totalStudyMs" to (s?.totalStudyMs ?: 0L),
                                    "mediaHours" to (s?.mediaHours ?: 0f),
                                    "studyHours" to (s?.studyHours ?: 0f),
                                    "totalLookups" to (s?.totalLookups ?: 0),
                                    "totalMined" to (s?.totalMined ?: 0),
                                    "totalSessions" to (s?.totalSessions ?: 0),
                                    "distinctDays" to (s?.days?.size ?: 0),
                                    "watchedMedia" to (m?.library?.watchedMediaCount() ?: 0),
                                    "watchHistoryEntries" to (m?.library?.history?.size ?: 0)
                                )
                            ),
                            ContentType.Application.Json
                        )
                    }

                    get("/api/mining/history") {
                        val records = mining.minedRecords.toList()
                        call.respondBytes(json.encodeToString(records).toByteArray(), ContentType.Application.Json)
                    }
                }
            })
            server?.start(wait = false)
            running = true
            lastError = null
        } catch (e: Exception) {
            lastError = e.message
            running = false
        }
    }

    fun stop() {
        server?.stop(200, 300)
        server = null
        running = false
    }

    val portInfo: String get() = "http://127.0.0.1:$activePort/api/mine"

    /**
     * Verify the running server end to end: the unauthenticated liveness probe
     * must answer, and a token-authenticated call must be accepted. Returns a
     * human-readable summary for the Integrations hub's "Test connection".
     */
    fun selfTest(): Result<String> = runCatching {
        if (!running) error("Server is not running")
        val client = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(3))
            .build()
        fun get(path: String, authorized: Boolean): Int {
            val builder = java.net.http.HttpRequest.newBuilder(java.net.URI("http://127.0.0.1:$activePort/$path"))
            if (authorized) builder.header("Authorization", "Bearer $token")
            val resp = client.send(builder.GET().build(), java.net.http.HttpResponse.BodyHandlers.ofString())
            return resp.statusCode()
        }
        val health = get("api/health", authorized = false)
        if (health != 200) error("Liveness check failed (HTTP $health)")
        val status = get("api/status", authorized = true)
        if (status != 200) error("Token auth check failed (HTTP $status)")
        "Local API responding on port $activePort · auth verified"
    }
}

/** Convert a raw API request into a mining payload. */
fun IntegrationCardRequest.toPayload(): MiningPayload = MiningPayload(
    headword = word,
    reading = reading,
    definition = definition,
    sentence = sentence,
    screenshotPath = screenshotPath ?: screenshot,
    audioPath = audioPath ?: audio,
    timestamp = timestamp,
    source = source.ifBlank { "api" },
    tags = tags,
    flags = flags,
    notes = notes,
    deckId = deckId
)

/** Build a compact JSON response from a map of primitives. */
internal fun jsonResponse(fields: Map<String, Any>): ByteArray {
    val builder = StringBuilder("{")
    fields.entries.forEachIndexed { i, (k, v) ->
        if (i > 0) builder.append(',')
        builder.append('"').append(k).append("\":")
        when (v) {
            is String -> builder.append('"').append(v.replace("\\", "\\\\").replace("\"", "\\\"")).append('"')
            is Boolean -> builder.append(v)
            is Number -> builder.append(v)
            else -> builder.append("\"").append(v).append('"')
        }
    }
    builder.append('}')
    return builder.toString().toByteArray()
}

/** GameSentenceMiner-shaped payload helper. */
fun gameSentenceMinerPayload(
    word: String,
    reading: String,
    definition: String,
    sentence: String,
    deckId: String,
    timestamp: Double? = null,
    screenshotPath: String? = null
): IntegrationCardRequest = IntegrationCardRequest(
    word = word,
    reading = reading,
    definition = definition,
    sentence = sentence,
    timestamp = timestamp,
    screenshotPath = screenshotPath,
    source = "gamesentenceminer",
    deckId = deckId
)