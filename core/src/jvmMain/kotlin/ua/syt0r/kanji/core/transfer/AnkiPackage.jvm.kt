package ua.syt0r.kanji.core.transfer

import ua.syt0r.kanji.presentation.screen.main.screen.decks.CardStatus
import ua.syt0r.kanji.presentation.screen.main.screen.decks.KaiteyoCard
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.sql.Connection
import java.sql.DriverManager
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.Result
import kotlin.runCatching

actual class AnkiPackage {

    actual companion object {
        actual val EXTENSION = "apkg"
        private const val COLLECTION_ENTRY = "collection.anki2"
        private const val SCHEMA_VERSION = 11
    }

    // ------------------------------------------------------------
    // Export
    // ------------------------------------------------------------

    actual fun write(cards: List<KaiteyoCard>, deckName: String): Result<ByteArray> = runCatching {
        val cardsToWrite = cards.filter { it.character.isNotBlank() }
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
        val deckId = 1L

        conn.prepareStatement(
            """
            INSERT INTO col (id, crt, mod, scm, ver, dty, usn, ls, conf, models, decks, dconf, tags)
            VALUES (1, ?, ?, ?, $SCHEMA_VERSION, 0, 0, 0, '{}', ?, ?, ?, '{}')
            """.trimIndent()
        ).use { ps ->
            ps.setLong(1, seconds)
            ps.setLong(2, seconds)
            ps.setLong(3, seconds)
            ps.setString(4, modelsJson(modelId, deckId))
            ps.setString(5, decksJson(deckId, deckName, seconds))
            ps.setString(6, "{}")
            ps.executeUpdate()
        }

        insertNotesAndCards(conn, cards, deckId, modelId, nowMs)
    }

    private fun insertNotesAndCards(conn: Connection, cards: List<KaiteyoCard>, deckId: Long, modelId: Long, nowMs: Long) {
        val seconds = nowMs / 1000
        var nextId = nowMs * 1000L

        cards.forEach { card ->
            val noteId = ++nextId
            val cardId = ++nextId
            val front = card.character
            val back = "${card.meaning}\n${card.reading}"
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
                ps.setLong(7, checksum(front))
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
    }

    // ------------------------------------------------------------
    // Archive helpers
    // ------------------------------------------------------------

    private fun zipDatabase(file: File): ByteArray = ByteArrayOutputStream().use { baos ->
        ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(ZipEntry(COLLECTION_ENTRY))
            Files.newInputStream(file.toPath()).use { input -> input.copyTo(zip) }
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("media"))
            zip.write("{}".toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
        }
        baos.toByteArray()
    }

    private fun extractDatabase(bytes: ByteArray, target: java.nio.file.Path) {
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
            conn.createStatement().use { st ->
                val rs = st.executeQuery(
                    """
                    SELECT n.id AS nid, n.guid, n.tags, n.flds,
                           c.type, c.queue, c.ivl, c.factor, c.reps, c.lapses, c.did
                    FROM notes n
                    LEFT JOIN cards c ON c.nid = n.id
                    GROUP BY n.id
                    ORDER BY n.id
                    """.trimIndent()
                )
                while (rs.next()) {
                    val fields = (rs.getString("flds") ?: "").split("\u001F")
                    val front = fields.getOrElse(0) { "" }.trim()
                    val back = fields.getOrElse(1) { "" }.trim()
                    if (front.isBlank()) continue
                    val guid = rs.getString("guid") ?: rs.getLong("nid").toString(16)
                    val tags = (rs.getString("tags") ?: "").split(" ").filter { it.isNotBlank() }
                    val type = rs.getInt("type")
                    val queue = rs.getInt("queue")
                    val ivl = rs.getInt("ivl")
                    val factor = rs.getInt("factor")
                    val reps = rs.getInt("reps")
                    val lapses = rs.getInt("lapses")

                    // Parse meaning and reading from back field
                    val backParts = back.split('\n')
                    val meaning = backParts.firstOrNull() ?: ""
                    val reading = backParts.getOrNull(1) ?: ""

                    result.add(
                        KaiteyoCard(
                            id = "anki-${guid.ifBlank { rs.getLong("nid").toString(16) }}",
                            character = front,
                            meaning = meaning,
                            reading = reading,
                            tagNames = tags.toMutableList(),
                            status = statusFromAnki(type, queue),
                            interval = ivl.coerceAtLeast(0),
                            reviewCount = reps.coerceAtLeast(0),
                            lapses = lapses.coerceAtLeast(0),
                            ease = (factor.coerceAtLeast(1000) / 1000.0).toFloat(),
                            deckId = rs.getLong("did")
                        )
                    )
                }
            }
        }
        return result
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
        // In core library, we don't have dueAt, so use interval
        return card.interval.toLong()
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

    private fun modelsJson(modelId: Long, deckId: Long): String = """
        {
          "$modelId": {
            "id": $modelId,
            "name": "Kaiteyo - Basic",
            "type": 0,
            "mod": 0,
            "usn": -1,
            "sortf": 0,
            "did": $deckId,
            "tmpls": [
              {
                "name": "Card 1",
                "ord": 0,
                "qfmt": "{{Front}}",
                "afmt": "{{FrontSide}}\n\n<hr id=answer>\n{{Back}}"
              }
            ],
            "flds": [
              { "name": "Front", "ord": 0 },
              { "name": "Back", "ord": 1 }
            ],
            "css": ".card { font-family: arial; font-size: 20px; text-align: center; color: black; background-color: white; }"
          }
        }
    """.trimIndent()

    private fun decksJson(deckId: Long, deckName: String, mod: Long): String = """
        {
          "$deckId": {
            "id": $deckId,
            "name": "$deckName",
            "mod": $mod,
            "usn": -1,
            "conf": 1,
            "desc": "",
            "dyn": 0,
            "collapsed": false,
            "extendNew": 0,
            "extendRev": 0,
            "newBury": true,
            "revBury": true,
            "lrnBury": true
          }
        }
    """.trimIndent()
}