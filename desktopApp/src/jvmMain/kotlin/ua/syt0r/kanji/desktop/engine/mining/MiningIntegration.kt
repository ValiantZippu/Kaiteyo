package ua.syt0r.kanji.desktop.engine.mining

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

// ============================================
// KAITEYO MINING INTEGRATION LAYER
// Where mined cards can go. Kaiteyo's own card
// pool is always the primary destination; an
// optional external transport (GameSentenceMiner)
// can receive the same payload. The transport is
// a clean interface so future exporters (Anki,
// custom APIs) plug in without touching the rest
// of the mining pipeline.
// ============================================

/** Where mined cards are sent. */
enum class MiningMode(val label: String) {
    Kaiteyo("Kaiteyo only"),
    Forward("Forward to GameSentenceMiner"),
    Both("Kaiteyo + GameSentenceMiner")
}

/**
 * Per-mine card destination. Kaiteyo is always available; Anki destinations
 * are only valid while the AnkiConnect integration is enabled. `Anki` means
 * the card lives in Anki only (Kaiteyo keeps a mine record, and falls back to
 * a native card if Anki is unreachable so a word is never lost).
 */
enum class CardDestination(val label: String) {
    Kaiteyo("Kaiteyo"),
    Anki("Anki"),
    Both("Kaiteyo + Anki")
}

/** A destination for mined cards, e.g. GameSentenceMiner. */
interface MiningTransport {
    val name: String
    val configured: Boolean
    val connected: Boolean
    fun send(payload: MiningPayload): Result<String>
    fun testConnection(): Result<String>
}

/**
 * GameSentenceMiner transport. GSM's local web server accepts JSON card
 * submissions; host/port/path are all configurable (defaults target a
 * local GSM install). Kaiteyo mining always works — this is optional.
 */
class GsmTransport(
    private val host: String = "127.0.0.1",
    private val port: Int = 9000,
    private val path: String = "/api/save",
    private val token: String = ""
) : MiningTransport {

    override val name: String = "GameSentenceMiner"

    override var connected by mutableStateOf(false)
        private set
    var lastError by mutableStateOf<String?>(null)
        private set

    override val configured: Boolean get() = host.isNotBlank() && port in 1..65535

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    private fun endpoint(): String = "http://$host:$port$path"

    override fun send(payload: MiningPayload): Result<String> = runCatching {
        if (!configured) error("GameSentenceMiner is not configured")
        val body = buildJsonPayload(payload)
        val request = HttpRequest.newBuilder(URI(endpoint()))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .header("User-Agent", "Kaiteyo/1.0")
            .let { req ->
                if (token.isNotBlank()) req.header("Authorization", "Bearer $token") else req
            }
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..399) {
            error("GSM responded HTTP ${response.statusCode()}: ${response.body().take(160)}")
        }
        connected = true
        lastError = null
        response.body().take(200)
    }.onFailure {
        connected = false
        lastError = it.message
    }

    override fun testConnection(): Result<String> = runCatching {
        if (!configured) error("Host or port missing — configure GameSentenceMiner in Settings → Media")
        val request = HttpRequest.newBuilder(URI(endpoint()))
            .timeout(Duration.ofSeconds(5))
            .header("User-Agent", "Kaiteyo/1.0")
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        connected = response.statusCode() in 200..399
        if (connected) "Connected to ${endpoint()} (HTTP ${response.statusCode()})"
        else error("HTTP ${response.statusCode()} from ${endpoint()}")
    }.onFailure {
        connected = false
        lastError = it.message
    }

    private fun buildJsonPayload(p: MiningPayload): String = buildString {
        append('{')
        fun field(key: String, value: String, last: Boolean = false) {
            append('"').append(key).append("\":")
            append('"').append(value.replace("\\", "\\\\").replace("\"", "\\\"")).append('"')
            if (!last) append(',')
        }
        field("word", p.headword)
        field("reading", p.reading)
        field("definition", p.definition)
        field("sentence", p.sentence)
        field("source", p.source)
        field("sourceDetail", p.sourceDetail)
        field("deck", p.deckId)
        field("notes", p.notes)
        if (p.screenshotPath != null) field("screenshotPath", p.screenshotPath)
        if (p.audioPath != null) field("audioPath", p.audioPath)
        if (p.timestamp != null) field("timestamp", p.timestamp.toString())
        field("tags", p.tags.joinToString(","), last = true)
        append('}')
    }
}

/** Owns the active transport + mode, used by MiningEngine and the UI. */
class MiningIntegrationManager(
    private val settings: ua.syt0r.kanji.desktop.engine.settings.SettingsEngine
) {
    val gsm: GsmTransport = GsmTransport(
        host = settings.getString("media.gsm.host", "127.0.0.1"),
        port = settings.getInt("media.gsm.port", 9000),
        path = settings.getString("media.gsm.path", "/api/save"),
        token = settings.getString("media.gsm.token")
    )

    val mode: MiningMode
        get() = when (settings.getString("media.mining-mode", "kaiteyo")) {
            "forward" -> MiningMode.Forward
            "both" -> MiningMode.Both
            else -> MiningMode.Kaiteyo
        }

    /**
     * The AnkiConnect transport. Connection parameters are read from the
     * settings engine at call time, so host/port/key changes apply without
     * rebuilding the manager.
     */
    val anki: AnkiConnectTransport = AnkiConnectTransport(
        config = {
            AnkiConfig(
                host = settings.getString("media.anki.host", "127.0.0.1"),
                port = settings.getInt("media.anki.port", 8765),
                apiKey = settings.getString("media.anki.key"),
                deckOverride = settings.getString("media.anki.deck")
            )
        }
    )

    val transports: List<MiningTransport>
        get() = buildList {
            add(gsm)
            if (settings.getBool("media.anki.enabled")) add(anki)
        }

    /**
     * Push a completed mine to the external transports for the given
     * destination. GameSentenceMiner follows the mining-mode setting; the
     * AnkiConnect transport is used only for `Anki`/`Both` destinations and
     * only while the integration is enabled. Each result is attributed to its
     * transport so callers can queue retries without duplicating anything.
     */
    fun forward(payload: MiningPayload, destination: CardDestination): List<Pair<MiningTransport, Result<String>>> {
        val results = mutableListOf<Pair<MiningTransport, Result<String>>>()
        when (mode) {
            MiningMode.Kaiteyo -> Unit
            MiningMode.Forward, MiningMode.Both -> results.add(gsm to gsm.send(payload))
        }
        if (settings.getBool("media.anki.enabled") && destination != CardDestination.Kaiteyo) {
            results.add(anki to anki.send(payload))
        }
        return results
    }
}
