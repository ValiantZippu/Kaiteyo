package ua.syt0r.kanji.core.transfer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardStatus
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KaiteyoCard
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.abs
import kotlin.Result
import kotlin.runCatching

actual class AnkiPackage(private val context: Context) {

    actual companion object {
        actual val EXTENSION = "apkg"
        private const val COLLECTION_ENTRY = "collection.anki2"
        private const val MEDIA_ENTRY = "media"
        private const val SCHEMA_VERSION = 11
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    }

    // ------------------------------------------------------------
    // Export
    // ------------------------------------------------------------

    actual fun write(cards: List<KaiteyoCard>, deckName: String): Result<ByteArray> = runCatching {
        val cardsToWrite = cards.filter { it.character.isNotBlank() }
        if (cardsToWrite.isEmpty()) error("Nothing to export — no cards with content")
        val tmp = tempDatabaseFile()
        try {
            createDatabase(tmp, cardsToWrite, deckName)
            zipDatabase(tmp)
        } finally {
            tmp.delete()
        }
    }

    // ------------------------------------------------------------
    // Import
    // ------------------------------------------------------------

    actual fun read(bytes: ByteArray): Result<List<KaiteyoCard>> = runCatching {
        val tmp = tempDatabaseFile()
        try {
            extractDatabase(bytes, tmp)
            readDatabase(tmp)
        } finally {
            tmp.delete()
        }
    }

    private fun tempDatabaseFile(): File =
        File(context.cacheDir, "kaiteyo-anki-${System.currentTimeMillis()}.anki2")

    // ------------------------------------------------------------
    // Database creation (Anki 2.1 schema v11)
    // ------------------------------------------------------------

    private fun createDatabase(file: File, cards: List<KaiteyoCard>, deckName: String) {
        file.delete()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                "CREATE TABLE col (id integer NOT NULL PRIMARY KEY, crt integer NOT NULL, " +
                    "mod integer NOT NULL, scm integer NOT NULL, ver integer NOT NULL, " +
                    "dty integer NOT NULL, usn integer NOT NULL, ls integer NOT NULL, " +
                    "conf text NOT NULL, models text NOT NULL, decks text NOT NULL, " +
                    "dconf text NOT NULL, tags text NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE notes (id integer NOT NULL PRIMARY KEY, guid text NOT NULL, " +
                    "mid integer NOT NULL, mod integer NOT NULL, usn integer NOT NULL, " +
                    "tags text NOT NULL, flds text NOT NULL, sfld integer NOT NULL, " +
                    "csum integer NOT NULL, flags integer NOT NULL, data text NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE cards (id integer NOT NULL PRIMARY KEY, nid integer NOT NULL, " +
                    "did integer NOT NULL, ord integer NOT NULL, mod integer NOT NULL, " +
                    "usn integer NOT NULL, type integer NOT NULL, queue integer NOT NULL, " +
                    "due integer NOT NULL, ivl integer NOT NULL, factor integer NOT NULL, " +
                    "reps integer NOT NULL, lapses integer NOT NULL, left integer NOT NULL, " +
                    "odue integer NOT NULL, odid integer NOT NULL, flags integer NOT NULL, " +
                    "data text NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE revlog (id integer NOT NULL PRIMARY KEY, cid integer NOT NULL, " +
                    "usn integer NOT NULL, ease integer NOT NULL, ivl integer NOT NULL, " +
                    "lastIvl integer NOT NULL, factor integer NOT NULL, time integer NOT NULL, " +
                    "type integer NOT NULL)"
            )
            db.execSQL("CREATE TABLE graves (usn integer NOT NULL, oid integer NOT NULL, type integer NOT NULL)")
            db.execSQL("CREATE INDEX ix_notes_mid ON notes (mid)")
            db.execSQL("CREATE INDEX ix_cards_nid ON cards (nid)")

            insertCollection(db, cards, deckName)
        }
    }

    private fun insertCollection(db: SQLiteDatabase, cards: List<KaiteyoCard>, deckName: String) {
        val nowMs = System.currentTimeMillis()
        val seconds = nowMs / 1000
        val modelId = 1L

        val deckGroups = cards.groupBy { it.deck.ifBlank { "default" } }
        val deckById = deckGroups.keys.mapIndexed { index, source ->
            val safeName = if (source == "default" && deckName.isNotBlank()) deckName else source
            ankiDeck(id = index.toLong() + 1, source = source, name = safeName, modSeconds = seconds)
        }.associateBy { it.source }
        val defaultDeckId = deckById.values.firstOrNull()?.id ?: 1L

        db.execSQL(
            "INSERT INTO col (id, crt, mod, scm, ver, dty, usn, ls, conf, models, decks, dconf, tags) " +
                "VALUES (1, ?, ?, ?, $SCHEMA_VERSION, 0, 0, 0, '{}', ?, ?, ?, '{}')",
            arrayOf(seconds, seconds, seconds, modelsJson(modelId, defaultDeckId), decksJson(deckById.values), "{}")
        )

        val counter = java.util.concurrent.atomic.AtomicLong(nowMs * 1000L)
        cards.forEach { card ->
            val sourceDeck = card.deck.ifBlank { "default" }
            insertNotesAndCards(db, card, deckById[sourceDeck]?.id ?: defaultDeckId, modelId, counter)
        }
    }

    private fun insertNotesAndCards(
        db: SQLiteDatabase,
        card: KaiteyoCard,
        deckId: Long,
        modelId: Long,
        counter: java.util.concurrent.atomic.AtomicLong
    ) {
        val seconds = System.currentTimeMillis() / 1000
        val noteId = counter.incrementAndGet()
        val cardId = counter.incrementAndGet()
        val front = card.character
        val back = "${card.meaning}\n${card.reading}".trimEnd('\n')
        val flds = "$front\u001F$back"
        val tags = card.tagNames.joinToString(" ")

        db.execSQL(
            "INSERT INTO notes (id, guid, mid, mod, usn, tags, flds, sfld, csum, flags, data) " +
                "VALUES (?, ?, ?, ?, 0, ?, ?, ?, ?, 0, '')",
            arrayOf(noteId, cardGuid(card), modelId, seconds, tags, flds, front, checksum(front))
        )

        db.execSQL(
            "INSERT INTO cards (id, nid, did, ord, mod, usn, type, queue, due, ivl, factor, reps, lapses, left, odue, odid, flags, data) " +
                "VALUES (?, ?, ?, 0, ?, 0, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, 0, '')",
            arrayOf(
                cardId, noteId, deckId, seconds,
                ankiType(card.status), ankiQueue(card.status), ankiDue(card, System.currentTimeMillis()),
                card.interval.coerceAtLeast(0),
                (card.ease * 1000).toInt().coerceAtLeast(1000),
                card.reviewCount, card.lapses
            )
        )
    }

    // ------------------------------------------------------------
    // Archive helpers
    // ------------------------------------------------------------

    private fun zipDatabase(file: File): ByteArray = ByteArrayOutputStream().use { baos ->
        ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(ZipEntry(COLLECTION_ENTRY))
            file.inputStream().use { input -> input.copyTo(zip) }
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(MEDIA_ENTRY))
            zip.write("{}".toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        baos.toByteArray()
    }

    private fun extractDatabase(bytes: ByteArray, target: File) {
        if (bytes.size < 4 || bytes[0] != 'P'.code.toByte() || bytes[1] != 'K'.code.toByte()) {
            error("Not an Anki package — the file is not a ZIP archive (.apkg expected)")
        }
        var found = false
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == COLLECTION_ENTRY || entry.name.endsWith(".anki2")) {
                    target.outputStream().use { out -> zip.copyTo(out) }
                    found = true
                    break
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        if (!found) error("Not an Anki package — no collection.anki2 found")
    }

    // ------------------------------------------------------------
    // Database reading
    // ------------------------------------------------------------

    private fun readDatabase(file: File): List<KaiteyoCard> {
        val result = mutableListOf<KaiteyoCard>()
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            val deckNames = readDeckNames(db)
            val fieldNamesByModel = readModelFieldNames(db)
            db.rawQuery(
                "SELECT c.ord, c.type, c.queue, c.ivl, c.factor, c.reps, c.lapses, c.did, " +
                    "n.id AS nid, n.guid, n.mid, n.tags, n.flds " +
                    "FROM cards c JOIN notes n ON n.id = c.nid ORDER BY n.id, c.ord",
                null
            ).use { rs ->
                while (rs.moveToNext()) {
                    val fields = (rs.getString(rs.getColumnIndexOrThrow("flds")) ?: "").split("\u001F")
                    val mid = rs.getLong(rs.getColumnIndexOrThrow("mid"))
                    val fieldNames = fieldNamesByModel[mid] ?: emptyList()
                    val byName = fieldNames.mapIndexed { index, name -> name to (fields.getOrElse(index) { "" }) }.toMap()
                    val frontHtml = renderTemplate(byName["Front"] ?: fields.getOrElse(0) { "" })
                    val backHtml = renderTemplate(byName["Back"] ?: fields.getOrElse(1) { "" })
                    if (frontHtml.isBlank()) continue

                    val guid = rs.getString(rs.getColumnIndexOrThrow("guid")) ?: rs.getLong(rs.getColumnIndexOrThrow("nid")).toString(16)
                    val ord = rs.getInt(rs.getColumnIndexOrThrow("ord"))
                    val tags = (rs.getString(rs.getColumnIndexOrThrow("tags")) ?: "").split(" ").filter { it.isNotBlank() }
                    val deckName = deckNames[rs.getLong(rs.getColumnIndexOrThrow("did"))] ?: "Imported"

                    val front = sanitizeToPlainText(frontHtml)
                    val backLines = sanitizeToPlainText(backHtml).split('\n').map { it.trim() }.filter { it.isNotBlank() }
                    val meaning = backLines.firstOrNull() ?: ""
                    val reading = backLines.getOrNull(1) ?: ""

                    result.add(
                        KaiteyoCard(
                            id = "anki-${guid.ifBlank { rs.getLong(rs.getColumnIndexOrThrow("nid")).toString(16) }}-$ord",
                            character = front.ifBlank { "…" },
                            meaning = meaning,
                            reading = reading,
                            deck = deckName,
                            deckId = abs(deckName.hashCode()).toLong(),
                            tagNames = tags.toMutableList(),
                            notes = sanitizeHtml(backHtml),
                            status = statusFromAnki(
                                rs.getInt(rs.getColumnIndexOrThrow("type")),
                                rs.getInt(rs.getColumnIndexOrThrow("queue"))
                            ),
                            interval = rs.getInt(rs.getColumnIndexOrThrow("ivl")).coerceAtLeast(0),
                            reviewCount = rs.getInt(rs.getColumnIndexOrThrow("reps")).coerceAtLeast(0),
                            lapses = rs.getInt(rs.getColumnIndexOrThrow("lapses")).coerceAtLeast(0),
                            ease = (rs.getInt(rs.getColumnIndexOrThrow("factor")).coerceAtLeast(1000) / 1000.0).toFloat()
                        )
                    )
                }
            }
        }
        return result
    }

    /** did → deck name (hierarchy preserved, e.g. "Japanese::N5::Kanji"). */
    private fun readDeckNames(db: SQLiteDatabase): Map<Long, String> {
        val names = mutableMapOf<Long, String>()
        runCatching {
            db.rawQuery("SELECT decks FROM col LIMIT 1", null).use { rs ->
                if (rs.moveToNext()) {
                    val payload = rs.getString(0) ?: return@runCatching
                    json.parseToJsonElement(payload).jsonObject.forEach { (id, deckObj) ->
                        val name = deckObj.jsonObject["name"]?.jsonPrimitive?.contentOrNull
                        val idLong = id.toLongOrNull()
                        if (name != null && idLong != null) names[idLong] = name
                    }
                }
            }
        }
        return names
    }

    private fun readModelFieldNames(db: SQLiteDatabase): Map<Long, List<String>> {
        val names = mutableMapOf<Long, List<String>>()
        runCatching {
            db.rawQuery("SELECT models FROM col LIMIT 1", null).use { rs ->
                if (rs.moveToNext()) {
                    val payload = rs.getString(0) ?: return@runCatching
                    json.parseToJsonElement(payload).jsonObject.forEach { (id, modelObj) ->
                        val idLong = id.toLongOrNull() ?: return@forEach
                        val flds = modelObj.jsonObject["flds"]?.jsonArray
                            ?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull }
                            ?: emptyList()
                        names[idLong] = flds
                    }
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
            .replace(Regex("\\[sound:[^\\]]*\\]"), "")
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
        CardStatus.Young, CardStatus.Mature, CardStatus.Suspended, CardStatus.Buried -> 2
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
        val obj = kotlinx.serialization.json.buildJsonObject {
            decks.forEach { deck ->
                put(
                    deck.id.toString(),
                    kotlinx.serialization.json.buildJsonObject {
                        put("id", JsonPrimitive(deck.id))
                        put("name", JsonPrimitive(deck.name))
                        put("mod", JsonPrimitive(deck.mod))
                        put("usn", JsonPrimitive(-1))
                        put("lrnToday", kotlinx.serialization.json.buildJsonArray { add(JsonPrimitive(0)); add(JsonPrimitive(0)) })
                        put("revToday", kotlinx.serialization.json.buildJsonArray { add(JsonPrimitive(0)); add(JsonPrimitive(0)) })
                        put("newToday", kotlinx.serialization.json.buildJsonArray { add(JsonPrimitive(0)); add(JsonPrimitive(0)) })
                        put("timeToday", kotlinx.serialization.json.buildJsonArray { add(JsonPrimitive(0)); add(JsonPrimitive(0)) })
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
        val obj = kotlinx.serialization.json.buildJsonObject {
            put(
                modelId.toString(),
                kotlinx.serialization.json.buildJsonObject {
                    put("id", JsonPrimitive(modelId))
                    put("name", JsonPrimitive("Kaiteyo - Basic"))
                    put("type", JsonPrimitive(0))
                    put("mod", JsonPrimitive(0))
                    put("usn", JsonPrimitive(-1))
                    put("sortf", JsonPrimitive(0))
                    put("did", JsonPrimitive(deckId))
                    put(
                        "tmpls",
                        kotlinx.serialization.json.buildJsonArray {
                            add(
                                kotlinx.serialization.json.buildJsonObject {
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
                        kotlinx.serialization.json.buildJsonArray {
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
            "media" to kotlinx.serialization.json.buildJsonArray { }
        )
    )
}
