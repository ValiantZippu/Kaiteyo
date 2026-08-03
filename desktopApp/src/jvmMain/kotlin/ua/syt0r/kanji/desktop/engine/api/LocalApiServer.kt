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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ua.syt0r.kanji.desktop.engine.mining.MiningEngine
import ua.syt0r.kanji.desktop.engine.mining.MiningPayload

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

class LocalApiServer(
    private val mining: MiningEngine,
    private val port: Int = 48201
) {

    private val json = Json { ignoreUnknownKeys = true }

    var running by mutableStateOf(false)
    var lastError by mutableStateOf<String?>(null)
    var lastRequest by mutableStateOf<IntegrationCardRequest?>(null)

    @Volatile
    private var server: io.ktor.server.engine.ApplicationEngine? = null

    fun start() {
        if (running) return
        try {
            server = embeddedServer(Netty, port = port, host = "127.0.0.1", module = {
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
                            json.encodeToString(ApiMineResponse(true, card.id, "Card \"${req.word}\" created")).toByteArray(),
                            ContentType.Application.Json
                        )
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

    val portInfo: String get() = "http://127.0.0.1:$port/api/mine"
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