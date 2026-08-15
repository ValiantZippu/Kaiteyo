package ua.syt0r.kanji.desktop.engine.mining

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import ua.syt0r.kanji.desktop.model.DesktopCard
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64

// ============================================
// KAITEYO ANKICONNECT TRANSPORT
// An optional bridge to Anki (desktop) through
// the AnkiConnect plugin. Kaiteyo's own card pool
// is always the primary destination; when enabled,
// a completed mine is also pushed to AnkiConnect
// as a Basic note with tags and (when present)
// screenshot/audio attached as media.
//
// This is an interoperability layer, never the
// database underneath Kaiteyo. Duplicate notes are
// detected via canAddNotes and skipped instead of
// silently duplicating a collection.
// ============================================

/** Live connection parameters read from the settings engine at call time. */
data class AnkiConfig(
    val host: String = "127.0.0.1",
    val port: Int = 8765,
    val apiKey: String = "",
    /** When set, mined cards land in this Anki deck instead of `Kaiteyo::<deck>`. */
    val deckOverride: String = ""
)

/** Builds AnkiConnect JSON-RPC-style requests and parses responses. */
class AnkiConnectTransport(
    private val config: () -> AnkiConfig
) : MiningTransport {

    override val name: String = "AnkiConnect"

    override var connected by mutableStateOf(false)
        private set
    var lastError by mutableStateOf<String?>(null)
        private set

    private val host: String get() = config().host
    private val port: Int get() = config().port
    private val apiKey: String get() = config().apiKey
    private val deckOverride: String get() = config().deckOverride

    override val configured: Boolean get() = host.isNotBlank() && port in 1..65535

    private val json = Json { ignoreUnknownKeys = true }
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    private val endpoint: String get() = "http://$host:$port"

    private fun call(action: String, params: JsonObject? = null): Result<JsonElement> = runCatching {
        if (!configured) error("AnkiConnect is not configured")
        val payload = buildJsonObject {
            put("action", action)
            put("version", 6)
            if (params != null) put("params", params)
            if (apiKey.isNotBlank()) put("key", apiKey)
        }
        val request = HttpRequest.newBuilder(URI(endpoint))
            .timeout(Duration.ofSeconds(8))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..399) error("AnkiConnect HTTP ${response.statusCode()}")
        val body = json.parseToJsonElement(response.body()).jsonObject
        val err = body["error"]?.jsonPrimitive?.contentOrNull
        if (!err.isNullOrBlank()) error("AnkiConnect: $err")
        body["result"] ?: JsonNull
    }

    override fun testConnection(): Result<String> = runCatching {
        val version = call("version").getOrThrow().jsonPrimitive.content
        val decks = call("deckNames").getOrThrow().jsonArray.size
        connected = true
        lastError = null
        "AnkiConnect v$version reachable — $decks deck(s)"
    }.onFailure {
        connected = false
        lastError = it.message
    }

    // ------------------------------------------------------------
    // Read actions (Anki → Kaiteyo import direction)
    // ------------------------------------------------------------

    /** All deck names → Anki deck ids (AnkiConnect `deckNamesAndIds`). */
    fun deckNamesAndIds(): Result<Map<String, Long>> = runCatching {
        call("deckNamesAndIds").getOrThrow().jsonObject.entries
            .mapNotNull { (name, value) ->
                val id = value.jsonPrimitive.longOrNull ?: return@mapNotNull null
                name to id
            }
            .toMap()
    }.onFailure {
        connected = false
        lastError = it.message
    }

    /** Note ids matching an Anki search query, e.g. `deck:"Japanese::N5"`. */
    fun findNotes(query: String): Result<List<Long>> = runCatching {
        call("findNotes", buildJsonObject { put("query", query) }).getOrThrow()
            .jsonArray.mapNotNull { it.jsonPrimitive.longOrNull }
    }.onFailure {
        connected = false
        lastError = it.message
    }

    /** Full note payloads (fields, tags, model, cards) for the given note ids. */
    fun notesInfo(noteIds: List<Long>): Result<List<JsonObject>> = runCatching {
        if (noteIds.isEmpty()) return@runCatching emptyList()
        call("notesInfo", buildJsonObject {
            put("notes", JsonArray(noteIds.map { JsonPrimitive(it) }))
        }).getOrThrow().jsonArray.mapNotNull { it as? JsonObject }
    }.onFailure {
        connected = false
        lastError = it.message
    }

    /** Card ids matching an Anki search query. */
    fun findCards(query: String): Result<List<Long>> = runCatching {
        call("findCards", buildJsonObject { put("query", query) }).getOrThrow()
            .jsonArray.mapNotNull { it.jsonPrimitive.longOrNull }
    }.onFailure {
        connected = false
        lastError = it.message
    }

    /** Card scheduling payloads (deck, due, interval, queue, reps, lapses, ease). */
    fun cardsInfo(cardIds: List<Long>): Result<List<JsonObject>> = runCatching {
        if (cardIds.isEmpty()) return@runCatching emptyList()
        call("cardsInfo", buildJsonObject {
            put("cards", JsonArray(cardIds.map { JsonPrimitive(it) }))
        }).getOrThrow().jsonArray.mapNotNull { it as? JsonObject }
    }.onFailure {
        connected = false
        lastError = it.message
    }

    /** Base64 content of an Anki media file, or null when it does not exist. */
    fun retrieveMediaFile(filename: String): Result<String?> = runCatching {
        call("retrieveMediaFile", buildJsonObject { put("filename", filename) }).getOrThrow()
            .jsonPrimitive.contentOrNull
    }.onFailure {
        connected = false
        lastError = it.message
    }

    /**
     * The note fields as a plain name → value map (HTML preserved). Also
     * returns the note guid, model name and tags for import mapping.
     */
    fun noteFields(note: JsonObject): Map<String, String> =
        note["fields"]?.jsonObject?.entries.orEmpty()
            .associate { (name, field) ->
                val value = field.jsonObject["value"]?.jsonPrimitive?.contentOrNull ?: ""
                name to value
            }

    override fun send(payload: MiningPayload): Result<String> = runCatching {
        if (!configured) error("AnkiConnect is not configured")
        val deck = deckOverride.ifBlank { "Kaiteyo::${payload.deckId.ifBlank { DesktopCard.DEFAULT_DECK_ID }}" }
        ensureDeck(deck)

        val note = buildJsonObject {
            put("deckName", deck)
            put("modelName", "Basic")
            put("fields", buildJsonObject {
                put("Front", payload.headword)
                put("Back", buildBackField(payload))
            })
            put("tags", JsonArray(payload.tags.map { JsonPrimitive(it) }))
            put("options", buildJsonObject { put("allowDuplicate", false) })
        }

        // Duplicate detection BEFORE adding: AnkiConnect returns null for
        // disallowed duplicates, but canAddNotes gives us the answer up front.
        val canAdd = call("canAddNotes", buildJsonObject {
            put("notes", JsonArray(listOf(note)))
        }).getOrThrow().jsonArray.firstOrNull()?.jsonPrimitive?.booleanOrNull ?: true

        val noteWithMedia = buildJsonObject {
            put("deckName", deck)
            put("modelName", "Basic")
            put("fields", note["fields"] ?: JsonNull)
            put("tags", note["tags"] ?: JsonNull)
            put("options", note["options"] ?: JsonNull)
            payload.audioPath?.let { path -> audioMedia(path)?.let { put("audio", JsonArray(listOf(it))) } }
            payload.screenshotPath?.let { path -> pictureMedia(path)?.let { put("picture", JsonArray(listOf(it))) } }
        }

        if (!canAdd) {
            connected = true
            lastError = null
            return@runCatching "Duplicate — not added to Anki (\"${payload.headword}\" already exists)"
        }
        val noteId = call("addNote", buildJsonObject { put("note", noteWithMedia) }).getOrThrow()
        connected = true
        lastError = null
        "Added to Anki deck \"$deck\" (note $noteId)"
    }.onFailure {
        connected = false
        lastError = it.message
    }

    /** Create the target deck once (idempotent — checked before creating). */
    private fun ensureDeck(deck: String) {
        val existing = call("deckNames").getOrThrow().jsonArray
            .map { it.jsonPrimitive.content }
            .toSet()
        if (deck !in existing) {
            call("createDeck", buildJsonObject { put("deck", deck) }).getOrThrow()
        }
    }

    /** Back field with reading, definition, sentence and source — safe HTML only. */
    private fun buildBackField(p: MiningPayload): String = buildString {
        if (p.reading.isNotBlank()) append(p.reading).append("<br>")
        if (p.definition.isNotBlank()) append(p.definition).append("<br>")
        if (p.sentence.isNotBlank()) append("Sentence: ").append(p.sentence).append("<br>")
        if (p.sourceDetail.isNotBlank()) append("Source: ").append(p.sourceDetail)
    }.trim()

    /** Inline audio for the Back field (base64 — works for local + remote collections). */
    private fun audioMedia(path: String): JsonObject? {
        val file = File(path)
        if (!file.exists() || !file.isFile) return null
        return runCatching {
            buildJsonObject {
                put("data", Base64.getEncoder().encodeToString(file.readBytes()))
                put("filename", "kaiteyo-${System.currentTimeMillis()}.${file.extension.ifBlank { "mp3" }}")
                put("fields", JsonArray(listOf(JsonPrimitive("Back"))))
            }
        }.getOrNull()
    }

    /** Inline screenshot for the Back field (base64). */
    private fun pictureMedia(path: String): JsonObject? {
        val file = File(path)
        if (!file.exists() || !file.isFile) return null
        return runCatching {
            buildJsonObject {
                put("data", Base64.getEncoder().encodeToString(file.readBytes()))
                put("filename", "kaiteyo-${System.currentTimeMillis()}.${file.extension.ifBlank { "png" }}")
                put("fields", JsonArray(listOf(JsonPrimitive("Back"))))
            }
        }.getOrNull()
    }
}
