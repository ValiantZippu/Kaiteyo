package ua.syt0r.kanji.core.transfer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardStatus
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KaiteyoCard
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.abs
import kotlin.Result
import kotlin.runCatching

actual class AnkiPackage {

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
        val tmp = Files.createTempFile("kaiteyo-anki", ".anki2")
        try {
            createDatabase(tmp.toFile(), cardsToWrite, deckName)
            zipDatabase(tmp.toFile())
        } finally {
            Files.deleteIfExists(tmp)
        }
    }

    // ------------------------------------------------------------
    // Import
    // ------------------------------------------------------------

    actual fun read(bytes: ByteArray): Result<List<KaiteyoCard>> = runCatching {
        val tmp = Files.createTempFile("kaiteyo-apkg", ".anki2")
        try {
            extractDatabase(bytes, tmp)
            readDatabase(tmp.toFile())
        } finally {
            Files.deleteIfExists(tmp)
        }
    }

    // ------------------------------------------------------------
    // Database creation
    // ------------------------------------------------------------

    private fun createDatabase(file: File, cards: List<KaiteyoCard>, deckName: String) {
        loadDriver()
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { conn ->
            conn.createStatement().use { st ->
                st.executeUpdate(
                    """
                    CREATE TABLE col (
                      id integer NOT NULL PRIMARY KEY,
                      crt integer NOT NULL, mod integer NOT NULL, scm integer NOT NULL,
                      ver integer NOT NULL, dty integer NOT NULL, usn integer NOT NULL,
                      ls integer NOT NULL, conf text NOT NULL, models text NOT NULL,
                      decks text NOT NULL, dconf text NOT NULL, tags text NOT NULL
                    )
                    """.trimIndent()
                )
                st.executeUpdate(
                    """
                    CREATE TABLE notes (
                      id integer NOT NULL PRIMARY KEY, guid text NOT NULL, mid integer NOT NULL,
                      mod integer NOT NULL, usn integer NOT NULL, tags text NOT NULL,
                      flds text NOT NULL, sfld integer NOT NULL, csum integer NOT NULL,
                      flags integer NOT NULL, data text NOT NULL
                    )
                    """.trimIndent()
                )
                st.executeUpdate(
                    """
                    CREATE TABLE cards (
                      id integer NOT NULL PRIMARY KEY, nid integer NOT NULL, did integer NOT NULL,
                      ord integer NOT NULL, mod integer NOT NULL, usn integer NOT NULL,
                      type integer NOT NULL, queue integer NOT NULL, due integer NOT NULL,
                      ivl integer NOT NULL, factor integer NOT NULL, reps integer NOT NULL,
                      lapses integer NOT NULL, left integer NOT NULL, odue integer NOT NULL,
                      odid integer NOT NULL, flags integer NOT NULL, data text NOT NULL
                    )
                    """.trimIndent()
                )
                st.executeUpdate(
                    """
                    CREATE TABLE revlog (
                      id integer NOT NULL PRIMARY KEY, cid integer NOT NULL, usn integer NOT NULL,
                      ease integer NOT NULL, ivl integer NOT NULL, lastIvl integer NOT NULL,
                      factor integer NOT NULL, time integer NOT NULL, type integer NOT NULL
                    )
                    """.trimIndent()
                )
                st.executeUpdate("CREATE TABLE graves ( usn integer NOT NULL, oid integer NOT NULL, type integer NOT NULL )")
                st.executeUpdate("CREATE INDEX ix_notes_mid ON notes (mid)")
                st.executeUpdate("CREATE INDEX ix_cards_nid ON cards (nid)")
            }
            insertCollection(conn, cards, deckName)
        }
    }

    private fun insertCollection(conn: Connection, cards: List<KaiteyoCard>, deckName: String) {
        val nowMs = System.currentTimeMillis()
        val seconds = nowMs / 1000
        val modelId = 1L

        val deckGroups = cards.groupBy { it.deck.ifBlank { "default" } }
        val deckById = deckGroups.keys.mapIndexed { index, name ->
            val safeName = if (name == "default" && deckName.isNotBlank()) deckName else name
            ankiDeck(id = index.toLong() + 1, source = name, name = safeName, modSeconds = seconds)
        }.associateBy { it.source }
        val defaultDeckId = deckById.values.firstOrNull()?.id ?: 1L

        conn.prepareStatement(
            """
            INSERT INTO col (id, crt, mod, scm, ver, dty, usn, ls, conf, models, decks, dconf, tags)
            VALUES (1, ?, ?, ?, $SCHEMA_VERSION, 0, 0, 0, '{}', ?, ?, ?, '{}')
            """.trimIndent()
        ).use { ps ->
            ps.setLong(1, seconds)
            ps.setLong(2, seconds)
            ps.setLong(3, seconds)
            ps.setString(4, modelsJson(modelId, defaultDeckId))
            ps.setString(5, decksJson(deckById.values))
            ps.setString(6, "{}")
            ps.executeUpdate()
        }

        cards.forEach { card ->
            val sourceDeck = card.deck.ifBlank { "default" }
            insertNotesAndCards(conn, card, deckById[sourceDeck]?.id ?: defaultDeckId, modelId, nowMs)
        }
    }

    private fun insertNotesAndCards(conn: Connection, card: KaiteyoCard, deckId: Long, modelId: Long, nowMs: Long) {
        val seconds = nowMs / 1000
        val counter = java.util.concurrent.atomic.AtomicLong(nowMs * 1000L)
        val noteId = counter.incrementAndGet()
        val cardId = counter.incrementAndGet()
        val front = card.character
        val back = "${card.meaning}\n${card.reading}".trimEnd('\n')
        val flds = "$front\u001F$back"
        val tags = card.tagNames.joinToString(" ")

        conn.prepareStatement(
            """
            INSERT INTO notes (id, guid, mid, mod, usn, tags, flds, sfld, csum, flags, data)
            VALUES (?, ?, ?, ?, 0, ?, ?, ?, ?, 0, '')
            """.trimIndent()
        ).use { ps ->
            ps.setLong(1, noteId)
            ps.setString(2, cardGuid(card))
            ps.setLong(3, modelId)
            ps.setLong(4, seconds)
            ps.setString(5, tags)
            ps.setString(6, flds)
            // sfld holds the sort-field TEXT; csum holds its checksum.
            ps.setString(7, front)
            ps.setLong(8, checksum(front))
            ps.executeUpdate()
        }

        conn.prepareStatement(
            """
            INSERT INTO cards (id, nid, did, ord, mod, usn, type, queue, due, ivl, factor, reps, lapses, left, odue, odid, flags, data)
            VALUES (?, ?, ?, 0, ?, 0, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, 0, '')
            """.trimIndent()
        ).use { ps ->
            ps.setLong(1, cardId)
            ps.setLong(2, noteId)
            ps.setLong(3, deckId)
            ps.setLong(4, seconds)
            ps.setInt(5, ankiType(card.status))
            ps.setInt(6, ankiQueue(card.status))
            ps.setLong(7, ankiDue(card, nowMs))
            ps.setInt(8, card.interval.coerceAtLeast(0))
            ps.setInt(9, (card.ease * 1000).toInt().coerceAtLeast(1000))
            ps.setInt(10, card.reviewCount)
            ps.setInt(11, card.lapses)
            ps.executeUpdate()
        }
    }

    // ------------------------------------------------------------
    // Archive helpers
    // ------------------------------------------------------------

    private fun zipDatabase(file: File): ByteArray = ByteArrayOutputStream().use { baos ->
        ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(ZipEntry(COLLECTION_ENTRY))
            Files.newInputStream(file.toPath()).use { input -> input.copyTo(zip) }
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(MEDIA_ENTRY))
            zip.write("{}".toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
        }
        baos.toByteArray()
    }

    private fun extractDatabase(bytes: ByteArray, target: Path) {
        if (bytes.size < 4 || bytes[0] != 'P'.code.toByte() || bytes[1] != 'K'.code.toByte()) {
            error("Not an Anki package — the file is not a ZIP archive (.apkg expected)")
        }
        var found = false
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == COLLECTION_ENTRY || entry.name.endsWith(".anki2")) {
                    Files.newOutputStream(target).use { out -> zip.copyTo(out) }
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
        loadDriver()
        val result = mutableListOf<KaiteyoCard>()
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { conn ->
            val deckNames = readDeckNames(conn)
            val fieldNamesByModel = readModelFieldNames(conn)
            conn.createStatement().use { st ->
                val rs = st.executeQuery(
                    """
                    SELECT c.ord, c.type, c.queue, c.ivl, c.factor, c.reps, c.lapses, c.did,
                           n.id AS nid, n.guid, n.mid, n.tags, n.flds
                    FROM cards c
                    JOIN notes n ON n.id = c.nid
                    ORDER BY n.id, c.ord
                    """
                )
                while (rs.next()) {
                    val fields = (rs.getString("flds") ?: "").split("\u001F")
                    val fieldNames = fieldNamesByModel[rs.getLong("mid")] ?: emptyList()
                    val byName = fieldNames.mapIndexed { index, name -> name to (fields.getOrElse(index) { "" }) }.toMap()
                    val frontHtml = renderTemplate(byName["Front"] ?: fields.getOrElse(0) { "" })
                    val backHtml = renderTemplate(byName["Back"] ?: fields.getOrElse(1) { "" })
                    if (frontHtml.isBlank()) continue

                    val guid = rs.getString("guid") ?: rs.getLong("nid").toString(16)
                    val ord = rs.getInt("ord")
                    val tags = (rs.getString("tags") ?: "").split(" ").filter { it.isNotBlank() }
                    val deckName = deckNames[rs.getLong("did")] ?: "Imported"

                    val front = sanitizeToPlainText(frontHtml)
                    val backLines = sanitizeToPlainText(backHtml).split('\n').map { it.trim() }.filter { it.isNotBlank() }
                    val meaning = backLines.firstOrNull() ?: ""
                    val reading = backLines.getOrNull(1) ?: ""

                    result.add(
                        KaiteyoCard(
                            id = "anki-${guid.ifBlank { rs.getLong("nid").toString(16) }}-$ord",
                            character = front.ifBlank { "…" },
                            meaning = meaning,
                            reading = reading,
                            deck = deckName,
                            deckId = abs(deckName.hashCode()).toLong(),
                            tagNames = tags.toMutableList(),
                            notes = sanitizeHtml(backHtml),
                            status = statusFromAnki(rs.getInt("type"), rs.getInt("queue")),
                            interval = rs.getInt("ivl").coerceAtLeast(0),
                            reviewCount = rs.getInt("reps").coerceAtLeast(0),
                            lapses = rs.getInt("lapses").coerceAtLeast(0),
                            ease = (rs.getInt("factor").coerceAtLeast(1000) / 1000.0).toFloat()
                        )
                    )
                }
            }
        }
        return result
    }

    /** did → deck name (hierarchy preserved, e.g. "Japanese::N5::Kanji"). */
    private fun readDeckNames(conn: Connection): Map<Long, String> {
        val names = mutableMapOf<Long, String>()
        runCatching {
            conn.createStatement().use { st ->
                val rs = st.executeQuery("SELECT decks FROM col LIMIT 1")
                if (rs.next()) {
                    val payload = rs.getString(1) ?: return@runCatching
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

    private fun readModelFieldNames(conn: Connection): Map<Long, List<String>> {
        val names = mutableMapOf<Long, List<String>>()
        runCatching {
            conn.createStatement().use { st ->
                val rs = st.executeQuery("SELECT models FROM col LIMIT 1")
                if (rs.next()) {
                    val payload = rs.getString(1) ?: return@runCatching
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

    /** Fallback rendering for a single field; other templates are dropped safely. */
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
        else -> CardStatus.Young // Anki review maps to Young (or Mature based on interval)
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
        text.toByteArray(StandardCharsets.UTF_8).forEach { sum += it.toLong() and 0xff }
        return sum and 0xffffffffL
    }

    actual fun ankiDue(card: KaiteyoCard, nowMs: Long): Long {
        if (card.status == CardStatus.New) return 0L
        // The exported collection clock starts at export time, so due == the
        // review interval in days is the correct Anki "days since crt" value.
        return card.interval.toLong().coerceAtLeast(0)
    }

    // ------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------

    private fun loadDriver() {
        try {
            Class.forName("org.sqlite.JDBC")
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException("The SQLite JDBC driver is unavailable on this build", e)
        }
    }

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
                        put("id", kotlinx.serialization.json.JsonPrimitive(deck.id))
                        put("name", kotlinx.serialization.json.JsonPrimitive(deck.name))
                        put("mod", kotlinx.serialization.json.JsonPrimitive(deck.mod))
                        put("usn", kotlinx.serialization.json.JsonPrimitive(-1))
                        put("lrnToday", kotlinx.serialization.json.buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive(0)); add(kotlinx.serialization.json.JsonPrimitive(0)) })
                        put("revToday", kotlinx.serialization.json.buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive(0)); add(kotlinx.serialization.json.JsonPrimitive(0)) })
                        put("newToday", kotlinx.serialization.json.buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive(0)); add(kotlinx.serialization.json.JsonPrimitive(0)) })
                        put("timeToday", kotlinx.serialization.json.buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive(0)); add(kotlinx.serialization.json.JsonPrimitive(0)) })
                        put("collapsed", kotlinx.serialization.json.JsonPrimitive(false))
                        put("browserCollapsed", kotlinx.serialization.json.JsonPrimitive(false))
                        put("desc", kotlinx.serialization.json.JsonPrimitive(""))
                        put("dyn", kotlinx.serialization.json.JsonPrimitive(0))
                        put("conf", kotlinx.serialization.json.JsonPrimitive(1))
                        put("extendNew", kotlinx.serialization.json.JsonPrimitive(10))
                        put("extendRev", kotlinx.serialization.json.JsonPrimitive(50))
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
                    put("id", kotlinx.serialization.json.JsonPrimitive(modelId))
                    put("name", kotlinx.serialization.json.JsonPrimitive("Kaiteyo - Basic"))
                    put("type", kotlinx.serialization.json.JsonPrimitive(0))
                    put("mod", kotlinx.serialization.json.JsonPrimitive(0))
                    put("usn", kotlinx.serialization.json.JsonPrimitive(-1))
                    put("sortf", kotlinx.serialization.json.JsonPrimitive(0))
                    put("did", kotlinx.serialization.json.JsonPrimitive(deckId))
                    put(
                        "tmpls",
                        kotlinx.serialization.json.buildJsonArray {
                            add(
                                kotlinx.serialization.json.buildJsonObject {
                                    put("name", kotlinx.serialization.json.JsonPrimitive("Card 1"))
                                    put("ord", kotlinx.serialization.json.JsonPrimitive(0))
                                    put("qfmt", kotlinx.serialization.json.JsonPrimitive("{{Front}}"))
                                    put("afmt", kotlinx.serialization.json.JsonPrimitive("{{FrontSide}}<hr id=answer>{{Back}}"))
                                    put("bqfmt", kotlinx.serialization.json.JsonPrimitive(""))
                                    put("bafmt", kotlinx.serialization.json.JsonPrimitive(""))
                                    put("did", kotlinx.serialization.json.JsonNull)
                                    put("bfont", kotlinx.serialization.json.JsonPrimitive(""))
                                    put("bqid", kotlinx.serialization.json.JsonPrimitive(0))
                                    put("baid", kotlinx.serialization.json.JsonPrimitive(0))
                                }
                            )
                        }
                    )
                    put(
                        "flds",
                        kotlinx.serialization.json.buildJsonArray {
                            add(
                                kotlinx.serialization.json.buildJsonObject {
                                    put("name", kotlinx.serialization.json.JsonPrimitive("Front"))
                                    put("ord", kotlinx.serialization.json.JsonPrimitive(0))
                                    put("sticky", kotlinx.serialization.json.JsonPrimitive(false))
                                    put("rtl", kotlinx.serialization.json.JsonPrimitive(false))
                                    put("font", kotlinx.serialization.json.JsonPrimitive("Arial"))
                                    put("size", kotlinx.serialization.json.JsonPrimitive(20))
                                    put("media", kotlinx.serialization.json.buildJsonArray { })
                                }
                            )
                            add(
                                kotlinx.serialization.json.buildJsonObject {
                                    put("name", kotlinx.serialization.json.JsonPrimitive("Back"))
                                    put("ord", kotlinx.serialization.json.JsonPrimitive(1))
                                    put("sticky", kotlinx.serialization.json.JsonPrimitive(false))
                                    put("rtl", kotlinx.serialization.json.JsonPrimitive(false))
                                    put("font", kotlinx.serialization.json.JsonPrimitive("Arial"))
                                    put("size", kotlinx.serialization.json.JsonPrimitive(20))
                                    put("media", kotlinx.serialization.json.buildJsonArray { })
                                }
                            )
                        }
                    )
                    put("css", kotlinx.serialization.json.JsonPrimitive(".card { font-family: arial; font-size: 20px; text-align: center; color: black; background-color: white; }"))
                    put("latexPre", kotlinx.serialization.json.JsonPrimitive(""))
                    put("latexPost", kotlinx.serialization.json.JsonPrimitive(""))
                }
            )
        }
        return obj.toString()
    }
}
