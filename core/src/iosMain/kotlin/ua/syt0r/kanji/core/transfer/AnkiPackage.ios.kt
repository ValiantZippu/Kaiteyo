package ua.syt0r.kanji.core.transfer

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ua.syt0r.kanji.core.getPrivateAppDataDirPath
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardStatus
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KaiteyoCard
import kotlin.math.abs
import kotlin.random.Random
import kotlin.Result
import kotlin.runCatching

// ============================================================
// ANKI PACKAGE (.apkg) — iOS actual.
//
// The collection.anki2 database is opened with SQLDelight's
// NativeSqliteDriver (sqlite3 is linked via `linkSqlite = true`),
// and the .apkg ZIP container is handled by the dependency-free
// codec in IosZip / IosInflate — Kotlin/Native has no java.util.zip.
// Behaviour mirrors AnkiPackage.jvm / .android.
// ============================================================

actual class AnkiPackage {

    actual companion object {
        actual val EXTENSION = "apkg"

        private const val COLLECTION_ENTRY = "collection.anki2"
        private const val MEDIA_ENTRY = "media"
        private const val SCHEMA_VERSION = 11

        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        /**
         * Version 0 disables sqliter's create/upgrade bookkeeping entirely:
         * opening an existing collection.anki2 never touches its schema, and
         * a brand-new file (export) is created empty for our own CREATE TABLEs.
         */
        private val openSchema = object : SqlSchema<QueryResult.Value<Unit>> {
            override val version: Long = 0
            override fun create(driver: SqlDriver): QueryResult.Value<Unit> = QueryResult.Value(Unit)
            override fun migrate(driver: SqlDriver, oldVersion: Long, newVersion: Long): QueryResult.Value<Unit> =
                QueryResult.Value(Unit)
        }
    }

    // ------------------------------------------------------------
    // Export
    // ------------------------------------------------------------

    actual fun write(cards: List<KaiteyoCard>, deckName: String): Result<ByteArray> = runCatching {
        val cardsToWrite = cards.filter { it.character.isNotBlank() }
        if (cardsToWrite.isEmpty()) error("Nothing to export — no cards with content")
        val tmp = tempDatabasePath()
        try {
            createDatabase(tmp, cardsToWrite, deckName)
            buildZip(
                listOf(
                    ZipDataEntry(COLLECTION_ENTRY, readFileBytes(tmp)),
                    ZipDataEntry(MEDIA_ENTRY, "{}".toByteArray(Charsets.UTF_8))
                )
            )
        } finally {
            deleteQuietly(tmp)
        }
    }

    // ------------------------------------------------------------
    // Import
    // ------------------------------------------------------------

    actual fun read(bytes: ByteArray): Result<List<KaiteyoCard>> = runCatching {
        val tmp = tempDatabasePath()
        try {
            extractDatabase(bytes, tmp)
            readDatabase(tmp)
        } finally {
            deleteQuietly(tmp)
        }
    }

    private fun extractDatabase(bytes: ByteArray, target: Path) {
        if (bytes.size < 4 || bytes[0] != 'P'.code.toByte() || bytes[1] != 'K'.code.toByte()) {
            error("Not an Anki package — the file is not a ZIP archive (.apkg expected)")
        }
        val entries = readZip(bytes)
        val collection = entries.firstOrNull { it.name == COLLECTION_ENTRY || it.name.endsWith(".anki2") }
            ?: error("Not an Anki package — no collection.anki2 found")
        writeFileBytes(target, collection.content)
    }

    // ------------------------------------------------------------
    // Temp files
    // ------------------------------------------------------------

    private fun tempDatabasePath(): Path {
        val dir = Path(getPrivateAppDataDirPath(), "anki_tmp")
        if (!SystemFileSystem.exists(dir)) {
            SystemFileSystem.createDirectories(dir)
        }
        return Path(dir, "kaiteyo-anki-${Random.nextInt()}.anki2")
    }

    private fun openDriver(path: Path): SqlDriver {
        val parent = path.parent?.toString() ?: error("Cannot resolve temp directory")
        val name = path.name ?: error("Cannot resolve temp file name")
        return NativeSqliteDriver(
            schema = openSchema,
            name = name,
            onConfiguration = { config ->
                config.copy(extendedConfig = DatabaseConfiguration.Extended(basePath = parent))
            }
        )
    }

    private fun readFileBytes(path: Path): ByteArray {
        val size = SystemFileSystem.metadataOrNull(path)?.size
            ?: error("Missing temporary database file: $path")
        if (size > Int.MAX_VALUE) error("Temporary database file too large: $size bytes")
        val bytes = ByteArray(size.toInt())
        var offset = 0
        SystemFileSystem.source(path).buffered().use { source ->
            while (offset < bytes.size) {
                val read = source.readAtMostTo(bytes, offset, bytes.size - offset)
                if (read <= 0) break
                offset += read
            }
        }
        if (offset != bytes.size) error("Failed to read temporary database file completely")
        return bytes
    }

    private fun writeFileBytes(path: Path, bytes: ByteArray) {
        SystemFileSystem.sink(path).buffered().use { sink ->
            sink.write(bytes, 0, bytes.size)
        }
    }

    private fun deleteQuietly(path: Path) {
        runCatching {
            if (SystemFileSystem.exists(path)) SystemFileSystem.delete(path)
        }
    }

    // ------------------------------------------------------------
    // Database creation (Anki 2.1 schema v11)
    // ------------------------------------------------------------

    private fun createDatabase(path: Path, cards: List<KaiteyoCard>, deckName: String) {
        if (SystemFileSystem.exists(path)) SystemFileSystem.delete(path)
        val driver = openDriver(path)
        try {
            driver.execute(
                null,
                "CREATE TABLE col (id integer NOT NULL PRIMARY KEY, crt integer NOT NULL, " +
                    "mod integer NOT NULL, scm integer NOT NULL, ver integer NOT NULL, " +
                    "dty integer NOT NULL, usn integer NOT NULL, ls integer NOT NULL, " +
                    "conf text NOT NULL, models text NOT NULL, decks text NOT NULL, " +
                    "dconf text NOT NULL, tags text NOT NULL)",
                0
            )
            driver.execute(
                null,
                "CREATE TABLE notes (id integer NOT NULL PRIMARY KEY, guid text NOT NULL, " +
                    "mid integer NOT NULL, mod integer NOT NULL, usn integer NOT NULL, " +
                    "tags text NOT NULL, flds text NOT NULL, sfld integer NOT NULL, " +
                    "csum integer NOT NULL, flags integer NOT NULL, data text NOT NULL)",
                0
            )
            driver.execute(
                null,
                "CREATE TABLE cards (id integer NOT NULL PRIMARY KEY, nid integer NOT NULL, " +
                    "did integer NOT NULL, ord integer NOT NULL, mod integer NOT NULL, " +
                    "usn integer NOT NULL, type integer NOT NULL, queue integer NOT NULL, " +
                    "due integer NOT NULL, ivl integer NOT NULL, factor integer NOT NULL, " +
                    "reps integer NOT NULL, lapses integer NOT NULL, left integer NOT NULL, " +
                    "odue integer NOT NULL, odid integer NOT NULL, flags integer NOT NULL, " +
                    "data text NOT NULL)",
                0
            )
            driver.execute(
                null,
                "CREATE TABLE revlog (id integer NOT NULL PRIMARY KEY, cid integer NOT NULL, " +
                    "usn integer NOT NULL, ease integer NOT NULL, ivl integer NOT NULL, " +
                    "lastIvl integer NOT NULL, factor integer NOT NULL, time integer NOT NULL, " +
                    "type integer NOT NULL)",
                0
            )
            driver.execute(null, "CREATE TABLE graves (usn integer NOT NULL, oid integer NOT NULL, type integer NOT NULL)", 0)
            driver.execute(null, "CREATE INDEX ix_notes_mid ON notes (mid)", 0)
            driver.execute(null, "CREATE INDEX ix_cards_nid ON cards (nid)", 0)
            insertCollection(driver, cards, deckName)
        } finally {
            driver.close()
        }
    }

    private fun insertCollection(driver: SqlDriver, cards: List<KaiteyoCard>, deckName: String) {
        val seconds = System.currentTimeMillis() / 1000
        val modelId = 1L

        val deckGroups = cards.groupBy { it.deck.ifBlank { "default" } }
        val deckById = deckGroups.keys.mapIndexed { index, source ->
            val safeName = if (source == "default" && deckName.isNotBlank()) deckName else source
            ankiDeck(id = index.toLong() + 1, source = source, name = safeName, modSeconds = seconds)
        }.associateBy { it.source }
        val defaultDeckId = deckById.values.firstOrNull()?.id ?: 1L

        driver.execute(
            null,
            "INSERT INTO col (id, crt, mod, scm, ver, dty, usn, ls, conf, models, decks, dconf, tags) " +
                "VALUES (1, ?, ?, ?, $SCHEMA_VERSION, 0, 0, 0, '{}', ?, ?, ?, '{}')",
            6
        ) {
            bindLong(0, seconds)
            bindLong(1, seconds)
            bindLong(2, seconds)
            bindString(3, modelsJson(modelId, defaultDeckId))
            bindString(4, decksJson(deckById.values))
            bindString(5, "{}")
        }

        var idCounter = System.currentTimeMillis() * 1000L
        cards.forEach { card ->
            val sourceDeck = card.deck.ifBlank { "default" }
            insertNotesAndCard(
                driver = driver,
                card = card,
                deckId = deckById[sourceDeck]?.id ?: defaultDeckId,
                modelId = modelId,
                noteId = ++idCounter,
                cardId = ++idCounter
            )
        }
    }

    private fun insertNotesAndCard(
        driver: SqlDriver,
        card: KaiteyoCard,
        deckId: Long,
        modelId: Long,
        noteId: Long,
        cardId: Long
    ) {
        val seconds = System.currentTimeMillis() / 1000
        val front = card.character
        val back = "${card.meaning}\n${card.reading}".trimEnd('\n')
        val fields = "$front\u001F$back"
        val tags = card.tagNames.joinToString(" ")

        driver.execute(
            null,
            "INSERT INTO notes (id, guid, mid, mod, usn, tags, flds, sfld, csum, flags, data) " +
                "VALUES (?, ?, ?, ?, 0, ?, ?, ?, ?, 0, '')",
            8
        ) {
            bindLong(0, noteId)
            bindString(1, cardGuid(card))
            bindLong(2, modelId)
            bindLong(3, seconds)
            bindString(4, tags)
            bindString(5, fields)
            bindString(6, front)
            bindLong(7, checksum(front))
        }

        driver.execute(
            null,
            "INSERT INTO cards (id, nid, did, ord, mod, usn, type, queue, due, ivl, factor, reps, lapses, left, odue, odid, flags, data) " +
                "VALUES (?, ?, ?, 0, ?, 0, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, 0, '')",
            11
        ) {
            bindLong(0, cardId)
            bindLong(1, noteId)
            bindLong(2, deckId)
            bindLong(3, seconds)
            bindLong(4, ankiType(card.status).toLong())
            bindLong(5, ankiQueue(card.status).toLong())
            bindLong(6, ankiDue(card, System.currentTimeMillis()))
            bindLong(7, card.interval.toLong().coerceAtLeast(0))
            bindLong(8, (card.ease * 1000).toLong().coerceAtLeast(1000))
            bindLong(9, card.reviewCount.toLong())
            bindLong(10, card.lapses.toLong())
        }
    }

    // ------------------------------------------------------------
    // Database reading
    // ------------------------------------------------------------

    private fun readDatabase(path: Path): List<KaiteyoCard> {
        val driver = openDriver(path)
        return try {
            val deckNames = readDeckNames(driver)
            val fieldNamesByModel = readModelFieldNames(driver)
            driver.executeQuery(
                identifier = null,
                sql = "SELECT c.ord, c.type, c.queue, c.ivl, c.factor, c.reps, c.lapses, c.did, " +
                    "n.id AS nid, n.guid, n.mid, n.tags, n.flds " +
                    "FROM cards c JOIN notes n ON n.id = c.nid ORDER BY n.id, c.ord",
                mapper = { cursor ->
                    val result = mutableListOf<KaiteyoCard>()
                    while (cursor.next().value) {
                        val fields = (cursor.getString(12) ?: "").split("\u001F")
                        val mid = cursor.getLong(10) ?: 0L
                        val fieldNames = fieldNamesByModel[mid] ?: emptyList()
                        val byName = fieldNames
                            .mapIndexed { index, name -> name to fields.getOrElse(index) { "" } }
                            .toMap()
                        val frontHtml = renderTemplate(byName["Front"] ?: fields.getOrElse(0) { "" })
                        val backHtml = renderTemplate(byName["Back"] ?: fields.getOrElse(1) { "" })
                        if (frontHtml.isBlank()) continue

                        val nid = cursor.getLong(8) ?: 0L
                        val guid = (cursor.getString(9) ?: "").ifBlank { nid.toString(16) }
                        val ord = cursor.getLong(0)?.toInt() ?: 0
                        val tags = (cursor.getString(11) ?: "").split(" ").filter { it.isNotBlank() }
                        val deckName = deckNames[cursor.getLong(7) ?: 0L] ?: "Imported"

                        val front = sanitizeToPlainText(frontHtml)
                        val backLines = sanitizeToPlainText(backHtml)
                            .split('\n')
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                        val meaning = backLines.firstOrNull() ?: ""
                        val reading = backLines.getOrNull(1) ?: ""

                        result.add(
                            KaiteyoCard(
                                id = "anki-$guid-$ord",
                                character = front.ifBlank { "…" },
                                meaning = meaning,
                                reading = reading,
                                deck = deckName,
                                deckId = abs(deckName.hashCode()).toLong(),
                                tagNames = tags.toMutableList(),
                                notes = sanitizeHtml(backHtml),
                                status = statusFromAnki(
                                    cursor.getLong(1)?.toInt() ?: 2,
                                    cursor.getLong(2)?.toInt() ?: 2
                                ),
                                interval = (cursor.getLong(3) ?: 0L).toInt().coerceAtLeast(0),
                                reviewCount = (cursor.getLong(5) ?: 0L).toInt().coerceAtLeast(0),
                                lapses = (cursor.getLong(6) ?: 0L).toInt().coerceAtLeast(0),
                                ease = ((cursor.getLong(4) ?: 1000L).toInt().coerceAtLeast(1000) / 1000.0).toFloat()
                            )
                        )
                    }
                    QueryResult.Value(result)
                },
                parameters = 0
            ).value
        } finally {
            driver.close()
        }
    }

    /** did → deck name (hierarchy preserved, e.g. "Japanese::N5::Kanji"). */
    private fun readDeckNames(driver: SqlDriver): Map<Long, String> {
        val names = mutableMapOf<Long, String>()
        runCatching {
            val payload = driver.executeQuery(
                identifier = null,
                sql = "SELECT decks FROM col LIMIT 1",
                mapper = { cursor ->
                    QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null)
                },
                parameters = 0
            ).value
            if (payload != null) {
                json.parseToJsonElement(payload).jsonObject.forEach { (id, deckObj) ->
                    val name = deckObj.jsonObject["name"]?.jsonPrimitive?.contentOrNull
                    val idLong = id.toLongOrNull()
                    if (name != null && idLong != null) names[idLong] = name
                }
            }
        }
        return names
    }

    private fun readModelFieldNames(driver: SqlDriver): Map<Long, List<String>> {
        val names = mutableMapOf<Long, List<String>>()
        runCatching {
            val payload = driver.executeQuery(
                identifier = null,
                sql = "SELECT models FROM col LIMIT 1",
                mapper = { cursor ->
                    QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null)
                },
                parameters = 0
            ).value
            if (payload != null) {
                json.parseToJsonElement(payload).jsonObject.forEach { (id, modelObj) ->
                    val idLong = id.toLongOrNull() ?: return@forEach
                    val fields = modelObj.jsonObject["flds"]?.jsonArray
                        ?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull }
                        ?: emptyList()
                    names[idLong] = fields
                }
            }
        }
        return names
    }

    /** Fallback rendering for a single field; unsupported templates are dropped safely. */
    private fun renderTemplate(field: String): String =
        sanitizeHtml(field).replace(Regex("<br\\s*/?>", RegexOption.DOT_MATCHES_ALL), "\n")

    private fun sanitizeHtml(html: String): String {
        var out = html
        out = Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL).replace(out, "")
        out = Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL).replace(out, "")
        out = Regex("</?\\s*(iframe|object|embed|form|input|button|link|meta)[^>]*>", RegexOption.DOT_MATCHES_ALL).replace(out, "")
        out = Regex("\\s+on\\w+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]*)", RegexOption.DOT_MATCHES_ALL).replace(out, "")
        out = Regex("(href|src|action)\\s*=\\s*(\"|')?\\s*javascript:[^\\s>\"']*", RegexOption.DOT_MATCHES_ALL).replace(out) { m ->
            m.value.substringBefore("javascript:").substringBeforeLast("=").trimEnd() + "=\"\""
        }
        return out.trim()
    }

    private fun sanitizeToPlainText(html: String): String {
        val safe = sanitizeHtml(html)
        return safe
            .replace(Regex("<br\\s*/?>", RegexOption.DOT_MATCHES_ALL), "\n")
            .replace(Regex("</(p|div|li|h[1-6]|tr)>", RegexOption.DOT_MATCHES_ALL), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("\\[sound:[^\\]]*]"), "")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&quot;"), "\"")
            .replace(Regex("&#39;"), "'")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    // ------------------------------------------------------------
    // Mappers / helpers (actual implementations of expect functions)
    // ------------------------------------------------------------

    actual fun ankiType(status: CardStatus): Int = when (status) {
        CardStatus.New -> 0
        CardStatus.Learning, CardStatus.Relearning -> 1
        else -> 2
    }

    actual fun ankiQueue(status: CardStatus): Int = when (status) {
        CardStatus.New -> 0
        CardStatus.Learning -> 1
        CardStatus.Young, CardStatus.Mature -> 2
        CardStatus.Relearning -> 3
        CardStatus.Suspended -> -1
        CardStatus.Buried -> -3
        else -> 2
    }

    actual fun statusFromAnki(type: Int, queue: Int): CardStatus = when {
        queue == -1 -> CardStatus.Suspended
        queue <= -2 -> CardStatus.Buried
        type == 0 || queue == 0 -> CardStatus.New
        type == 1 || queue == 1 -> CardStatus.Learning
        queue == 3 -> CardStatus.Relearning
        else -> CardStatus.Young
    }

    actual fun cardGuid(card: KaiteyoCard): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        var value = card.id.hashCode().toLong() and 0x7fffffffL
        val sb = StringBuilder()
        repeat(10) {
            sb.append(alphabet[(value % alphabet.length).toInt()])
            value /= alphabet.length
        }
        return sb.toString()
    }

    actual fun checksum(text: String): Long {
        var sum = 0L
        text.toByteArray(Charsets.UTF_8).forEach { sum += it.toLong() and 0xff }
        return sum and 0xffffffffL
    }

    actual fun ankiDue(card: KaiteyoCard, nowMs: Long): Long {
        if (card.status == CardStatus.New) return 0L
        // The exported collection clock starts at export time, so due == the
        // review interval in days is the correct Anki "days since crt" value.
        return card.interval.toLong().coerceAtLeast(0)
    }

    // ------------------------------------------------------------
    // col.decks / col.models JSON — built with kotlinx.serialization
    // so user-supplied deck names can never break out of the JSON.
    // ------------------------------------------------------------

    private data class AnkiDeck(val id: Long, val source: String, val name: String, val mod: Long)

    private fun ankiDeck(id: Long, source: String, name: String, modSeconds: Long): AnkiDeck =
        AnkiDeck(
            id = id,
            source = source,
            name = name.replace("\\", " ").replace(":", " - ").replace("\n", " ").trim().ifBlank { "Kaiteyo" },
            mod = modSeconds
        )

    private fun decksJson(decks: Collection<AnkiDeck>): String {
        val obj = buildJsonObject {
            decks.forEach { deck ->
                put(
                    deck.id.toString(),
                    buildJsonObject {
                        put("id", JsonPrimitive(deck.id))
                        put("name", JsonPrimitive(deck.name))
                        put("mod", JsonPrimitive(deck.mod))
                        put("usn", JsonPrimitive(-1))
                        put("lrnToday", buildJsonArray { add(JsonPrimitive(0)); add(JsonPrimitive(0)) })
                        put("revToday", buildJsonArray { add(JsonPrimitive(0)); add(JsonPrimitive(0)) })
                        put("newToday", buildJsonArray { add(JsonPrimitive(0)); add(JsonPrimitive(0)) })
                        put("timeToday", buildJsonArray { add(JsonPrimitive(0)); add(JsonPrimitive(0)) })
                        put("collapsed", JsonPrimitive(false))
                        put("browserCollapsed", JsonPrimitive(false))
                        put("desc", JsonPrimitive(""))
                        put("dyn", JsonPrimitive(0))
                        put("conf", JsonPrimitive(1))
                        put("extendNew", JsonPrimitive(10))
                        put("extendRev", JsonPrimitive(50))
                    }
                )
            }
        }
        return obj.toString()
    }

    private fun modelsJson(modelId: Long, deckId: Long): String {
        val obj = buildJsonObject {
            put(
                modelId.toString(),
                buildJsonObject {
                    put("id", JsonPrimitive(modelId))
                    put("name", JsonPrimitive("Kaiteyo - Basic"))
                    put("type", JsonPrimitive(0))
                    put("mod", JsonPrimitive(0))
                    put("usn", JsonPrimitive(-1))
                    put("sortf", JsonPrimitive(0))
                    put("did", JsonPrimitive(deckId))
                    put(
                        "tmpls",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("name", JsonPrimitive("Card 1"))
                                    put("ord", JsonPrimitive(0))
                                    put("qfmt", JsonPrimitive("{{Front}}"))
                                    put("afmt", JsonPrimitive("{{FrontSide}}<hr id=answer>{{Back}}"))
                                    put("bqfmt", JsonPrimitive(""))
                                    put("bafmt", JsonPrimitive(""))
                                    put("did", JsonNull)
                                    put("bfont", JsonPrimitive(""))
                                    put("bqid", JsonPrimitive(0))
                                    put("baid", JsonPrimitive(0))
                                }
                            )
                        }
                    )
                    put(
                        "flds",
                        buildJsonArray {
                            add(fieldJson("Front", 0))
                            add(fieldJson("Back", 1))
                        }
                    )
                    put("css", JsonPrimitive(".card { font-family: arial; font-size: 20px; text-align: center; color: black; background-color: white; }"))
                    put("latexPre", JsonPrimitive(""))
                    put("latexPost", JsonPrimitive(""))
                }
            )
        }
        return obj.toString()
    }

    private fun fieldJson(name: String, ord: Int): JsonObject = JsonObject(
        mapOf(
            "name" to JsonPrimitive(name),
            "ord" to JsonPrimitive(ord),
            "sticky" to JsonPrimitive(false),
            "rtl" to JsonPrimitive(false),
            "font" to JsonPrimitive("Arial"),
            "size" to JsonPrimitive(20),
            "media" to buildJsonArray { }
        )
    )
}
