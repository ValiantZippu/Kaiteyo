package ua.syt0r.kanji.desktop.engine.transfer

import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ua.syt0r.kanji.desktop.model.DesktopCard
import ua.syt0r.kanji.desktop.model.SrsStatus
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

// ============================================
// ANKI PACKAGE (.apkg)
// Reads and writes the Anki 2.1 package format:
// a ZIP containing collection.anki2 (SQLite), a
// media manifest and the media files themselves.
//
// Compatibility layer — what survives a round
// trip (and what does not):
//   ✔ decks + hierarchy (Japanese::N5::Kanji)
//   ✔ notes → cards (one card per Anki card, ord
//     preserved), fields, tags
//   ✔ scheduling state (type/queue → Kaiteyo SRS,
//     interval, ease, reps, lapses, due)
//   ✔ media files (extracted + references repaired)
//   ✔ common {{Field}} / {{cloze:..}} templates
//   ✖ exact template styling (rendered to plain
//     text), typing-mode cards, cram scheduling
// The SQLite JDBC driver is loaded reflectively
// because it arrives transitively from the core
// module rather than as a direct dependency.
// ============================================

object AnkiPackage {

    const val EXTENSION = "apkg"
    private const val COLLECTION_ENTRY = "collection.anki2"
    private const val MEDIA_ENTRY = "media"
    private const val SCHEMA_VERSION = 11

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /** Everything the importer learned about a package, for preview UI. */
    data class AnkiImportResult(
        val cards: List<DesktopCard>,
        val warnings: List<String>,
        val mediaFiles: List<String>,
        val decks: List<String>
    )

    // ------------------------------------------------------------
    // Export
    // ------------------------------------------------------------

    fun write(cards: List<DesktopCard>, deckName: String = "Kaiteyo"): Result<ByteArray> = runCatching {
        val cardsToWrite = cards.filter { it.character.isNotBlank() }
        if (cardsToWrite.isEmpty()) error("Nothing to export — the card pool is empty")
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

    fun read(bytes: ByteArray): Result<List<DesktopCard>> =
        readDetailed(bytes).map { it.cards }

    fun readDetailed(bytes: ByteArray): Result<AnkiImportResult> = runCatching {
        val tmp = Files.createTempFile("kaiteyo-apkg", ".anki2")
        val mediaDir = mediaDirFor(bytes)
        try {
            val media = extractDatabaseAndMedia(bytes, tmp, mediaDir)
            readDatabase(tmp.toFile(), mediaDir, media)
        } finally {
            Files.deleteIfExists(tmp)
        }
    }

    // ------------------------------------------------------------
    // Database creation
    // ------------------------------------------------------------

    private fun createDatabase(file: File, cards: List<DesktopCard>, deckName: String) {
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

    private fun insertCollection(conn: Connection, cards: List<DesktopCard>, deckName: String) {
        val nowMs = System.currentTimeMillis()
        val seconds = nowMs / 1000
        val modelId = 1L

        // One Anki deck per distinct Kaiteyo deckId, named after the deck.
        val deckGroups = cards.groupBy { it.deckId.ifBlank { "default" } }
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
            val sourceDeck = card.deckId.ifBlank { "default" }
            insertNotesAndCards(conn, card, deckById[sourceDeck]?.id ?: defaultDeckId, modelId, nowMs)
        }
    }

    private fun insertNotesAndCards(conn: Connection, card: DesktopCard, deckId: Long, modelId: Long, nowMs: Long) {
        val seconds = nowMs / 1000
        val nextId = java.util.concurrent.atomic.AtomicLong(nowMs * 1000L)
        val noteId = nextId.incrementAndGet()
        val cardId = nextId.incrementAndGet()
        val front = card.character
        val reading = card.readings.joinToString("、")
        val back = listOfNotNull(card.meaning, reading.ifBlank { null }).joinToString("\n")
        val flds = "$front\u001F$back"
        val tags = card.tags.joinToString(" ")

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
            // sfld holds the sort field text (Anki stores it as a string in
            // an INTEGER column); csum is its checksum — not the other way round.
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
            ps.setInt(8, card.intervalDays.toInt().coerceAtLeast(0))
            ps.setInt(9, (card.ease * 1000).toInt().coerceAtLeast(1000))
            ps.setInt(10, card.reps)
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

    /** Where imported media lands: ~/.kaiteyo/anki-media/<package-hash>/ */
    private fun mediaDirFor(bytes: ByteArray): File {
        val home = System.getProperty("user.home") ?: "."
        val hash = abs(bytes.contentHashCode()).toString(16).take(12)
        return File(File(home, ".kaiteyo/anki-media"), hash).apply { mkdirs() }
    }

    /**
     * Extracts collection.anki2 and (when present) the media map + files.
     * Returns the parsed media manifest (real filename → zip entry name).
     */
    private fun extractDatabaseAndMedia(
        bytes: ByteArray,
        target: Path,
        mediaDir: File
    ): Map<String, String> {
        if (bytes.size < 4 || bytes[0] != 'P'.code.toByte() || bytes[1] != 'K'.code.toByte()) {
            error("Not an Anki package — the file is not a ZIP archive (.apkg expected)")
        }
        var found = false
        var mediaMap = emptyMap<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when {
                    entry.name == COLLECTION_ENTRY || entry.name.endsWith(".anki2") -> {
                        Files.newOutputStream(target).use { out -> zip.copyTo(out) }
                        found = true
                    }
                    entry.name == MEDIA_ENTRY -> {
                        val text = zip.readBytes().toString(Charsets.UTF_8)
                        mediaMap = parseMediaMap(text)
                    }
                    mediaMap.values.contains(entry.name) -> {
                        // A numbered media entry referenced by the manifest.
                        val realName = mediaMap.entries.firstOrNull { it.value == entry.name }?.key
                        if (realName != null && realName.isNotBlank()) {
                            runCatching {
                                val safeName = File(realName).name // never traverse outside the dir
                                File(mediaDir, safeName).writeBytes(zip.readBytes())
                            }
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        if (!found) error("Not an Anki package — no collection.anki2 found")
        return mediaMap
    }

    private fun parseMediaMap(text: String): Map<String, String> = runCatching {
        // Anki's manifest maps "1" → "filename.ext".
        json.parseToJsonElement(text).jsonObject
            .mapNotNull { (index, name) -> name.jsonPrimitive.contentOrNull?.let { it to index } }
            .toMap()
    }.getOrDefault(emptyMap())

    // ------------------------------------------------------------
    // Database reading
    // ------------------------------------------------------------

    private fun readDatabase(file: File, mediaDir: File, media: Map<String, String>): AnkiImportResult {
        loadDriver()
        val warnings = mutableListOf<String>()
        val result = mutableListOf<DesktopCard>()
        var deckNames: Map<Long, String> = emptyMap()
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { conn ->
            deckNames = readDeckNames(conn)
            val models = readModels(conn)
            conn.createStatement().use { st ->
                val rs = st.executeQuery(
                    """
                    SELECT c.id AS cid, c.ord, c.type, c.queue, c.due, c.ivl, c.factor,
                           c.reps, c.lapses, c.did,
                           n.id AS nid, n.guid, n.mid, n.tags, n.flds
                    FROM cards c
                    JOIN notes n ON n.id = c.nid
                    ORDER BY n.id, c.ord
                    """
                )
                while (rs.next()) {
                    val fields = (rs.getString("flds") ?: "").split("\u001F")
                    val mid = rs.getLong("mid")
                    val model = models[mid]
                    val fieldNames = model?.flds ?: emptyList()
                    val byName = fieldNames.mapIndexed { index, name -> name to (fields.getOrElse(index) { "" }) }.toMap()

                    val frontHtml = renderTemplate(
                        template = model?.frontTemplate ?: "",
                        fields = byName,
                        ord = rs.getInt("ord"),
                        frontSide = ""
                    )
                    val backHtml = renderTemplate(
                        template = model?.backTemplate ?: "",
                        fields = byName,
                        ord = rs.getInt("ord"),
                        frontSide = frontHtml
                    )
                    if (frontHtml.isBlank()) continue

                    val guid = rs.getString("guid") ?: rs.getLong("nid").toString(16)
                    val ord = rs.getInt("ord")
                    val tags = (rs.getString("tags") ?: "").split(" ").filter { it.isNotBlank() }
                    val type = rs.getInt("type")
                    val queue = rs.getInt("queue")
                    val due = rs.getLong("due")
                    val deckName = deckNames[rs.getLong("did")] ?: "Imported"
                    if (model == null) warnings.add("Note ${rs.getLong("nid")}: unknown note type, fields imported in order")
                    val deckId = "anki-deck-${abs(deckName.hashCode()).toString(16)}"

                    val (meaningText, repairedBack) = sanitizeCardHtml(backHtml, mediaDir, media)
                    val (frontText, _) = sanitizeCardHtml(frontHtml, mediaDir, media)

                    result.add(
                        DesktopCard(
                            id = "anki-${guid.ifBlank { rs.getLong("nid").toString(16) }}-$ord",
                            character = frontText.ifBlank { "…" },
                            meaning = meaningText,
                            onReadings = emptyList(),
                            kunReadings = emptyList(),
                            tags = tags,
                            note = repairedBack,
                            status = statusFromAnki(type, queue),
                            intervalDays = rs.getInt("ivl").toDouble().coerceAtLeast(0.0),
                            dueAt = ankiDueInstant(type, queue, due, conn),
                            reps = rs.getInt("reps").coerceAtLeast(0),
                            lapses = rs.getInt("lapses").coerceAtLeast(0),
                            ease = (rs.getInt("factor").coerceAtLeast(1000) / 1000.0),
                            deckId = deckId,
                            createdAt = Instant.fromEpochMilliseconds(rs.getLong("nid"))
                        )
                    )
                }
            }
        }
        return AnkiImportResult(
            cards = result,
            warnings = warnings,
            mediaFiles = media.keys.toList(),
            decks = deckNames.values.distinct().sorted()
        )
    }

    /** did → human deck name (hierarchy preserved, e.g. "Japanese::N5::Kanji"). */
    private fun readDeckNames(conn: Connection): Map<Long, String> {
        val names = mutableMapOf<Long, String>()
        runCatching {
            conn.createStatement().use { st ->
                val rs = st.executeQuery("SELECT decks FROM col LIMIT 1")
                if (rs.next()) {
                    val payload = rs.getString(1) ?: return@runCatching
                    val root = json.parseToJsonElement(payload).jsonObject
                    root.forEach { (id, deckObj) ->
                        val name = deckObj.jsonObject["name"]?.jsonPrimitive?.contentOrNull
                        val idLong = id.toLongOrNull()
                        if (name != null && idLong != null) names[idLong] = name
                    }
                }
            }
        }
        return names
    }

    private data class AnkiModel(
        val flds: List<String>,
        val frontTemplate: String,
        val backTemplate: String
    )

    private fun readModels(conn: Connection): Map<Long, AnkiModel> {
        val models = mutableMapOf<Long, AnkiModel>()
        runCatching {
            conn.createStatement().use { st ->
                val rs = st.executeQuery("SELECT models FROM col LIMIT 1")
                if (rs.next()) {
                    val payload = rs.getString(1) ?: return@runCatching
                    val root = json.parseToJsonElement(payload).jsonObject
                    root.forEach { (id, modelObj) ->
                        val idLong = id.toLongOrNull() ?: return@forEach
                        val model = modelObj.jsonObject
                        val flds = model["flds"]?.jsonArray
                            ?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull }
                            ?: emptyList()
                        val tmpl = model["tmpls"]?.jsonArray?.firstOrNull()
                        val front = tmpl?.jsonObject?.get("qfmt")?.jsonPrimitive?.contentOrNull ?: ""
                        val back = tmpl?.jsonObject?.get("afmt")?.jsonPrimitive?.contentOrNull ?: ""
                        models[idLong] = AnkiModel(flds, front, back)
                    }
                }
            }
        }
        return models
    }

    /**
     * Renders an Anki template to a safe approximation. Supports {{Field}},
     * {{text:Field}}, {{cloze:n:Field}}, {{FrontSide}} and {{hint:..}}; any
     * leftover braces are dropped rather than leaked into the card.
     */
    private fun renderTemplate(template: String, fields: Map<String, String>, ord: Int, frontSide: String): String {
        if (template.isBlank()) return ""
        var out = template

        // {{cloze:N:Text}} and {{cloze:N:Text::hint}} — only the card's own
        // deletion stays visible; the rest collapse to an ellipsis.
        val clozePattern = Regex("\\{\\{c(\\d+)::(.*?)(?:::.*?)?\\}\\}", RegexOption.DOT_MATCHES_ALL)
        out = clozePattern.replace(out) { m ->
            val number = m.groupValues[1].toIntOrNull() ?: return@replace m.value
            if (number == ord + 1) sanitizeInlineHtml(m.groupValues[2]) else "…"
        }

        // {{hint:Field}} — interactive hints are not renderable here.
        out = Regex("\\{\\{hint:[^}]*\\}\\}").replace(out, "")

        // {{Field}}, {{text:Field}}, {{type:Field}} — field substitution.
        val fieldPattern = Regex("\\{\\{(?:text:|type:)?([^:}\\|]+?)(?:::[^}]*)?\\}\\}")
        out = fieldPattern.replace(out) { m ->
            val name = m.groupValues[1].trim()
            when {
                name == "FrontSide" -> frontSide
                fields.containsKey(name) -> fields[name].orEmpty()
                name == "Tags" -> fields.values.firstOrNull() ?: "" // not a real field; never resolves
                else -> ""
            }
        }

        // Any remaining {{...}} is an unsupported directive — drop it.
        return Regex("\\{\\{.*?\\}\\}", RegexOption.DOT_MATCHES_ALL).replace(out, "")
    }

    /** Keep inline formatting, strip anything that could execute. */
    private fun sanitizeInlineHtml(text: String): String {
        var out = text
        out = Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL).replace(out, "")
        out = Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL).replace(out, "")
        out = Regex("</?\\s*(iframe|object|embed|form|input|button|link|meta)[^>]*>", RegexOption.DOT_MATCHES_ALL).replace(out, "")
        out = Regex("\\s+on\\w+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]*)", RegexOption.DOT_MATCHES_ALL).replace(out, "")
        out = Regex("(href|src|action)\\s*=\\s*(\"|')?\\s*javascript:[^\\s>\"']*", RegexOption.DOT_MATCHES_ALL).replace(out) { m ->
            m.value.substringBefore("javascript:").substringBeforeLast("=").trimEnd() + "=\"\""
        }
        return out.trim()
    }

    /**
     * Sanitizes card HTML, repairs media references and produces a plain-text
     * meaning. Returns (plainText, sanitizedHtmlWithRepairedMedia).
     */
    private fun sanitizeCardHtml(html: String, mediaDir: File, media: Map<String, String>): Pair<String, String> {
        var repaired = html
        // [sound:file.mp3] → local extracted path so audio survives the import.
        repaired = Regex("\\[sound:([^\\]]+)\\]").replace(repaired) { m ->
            val name = m.groupValues[1].trim()
            val local = repairedMediaPath(name, mediaDir, media)
            if (local != null) "[sound:$local]" else ""
        }
        // <img src="file.png"> → local extracted path (kept as HTML in the note).
        repaired = Regex("(<img[^>]*\\bsrc\\s*=\\s*[\"'])([^\"']+)([\"'][^>]*>)", RegexOption.DOT_MATCHES_ALL)
            .replace(repaired) { m ->
                val name = m.groupValues[2].trim()
                val local = repairedMediaPath(name, mediaDir, media)
                m.groupValues[1] + (local ?: "") + m.groupValues[3]
            }
        val safe = sanitizeInlineHtml(repaired)
        // Plain-text meaning: block elements become line breaks, tags stripped.
        val text = safe
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
        return text to safe
    }

    private fun repairedMediaPath(name: String, mediaDir: File, media: Map<String, String>): String? {
        val index = media[name] ?: return null
        val local = File(mediaDir, File(name).name)
        return if (local.exists()) local.absolutePath else null
    }

    /**
     * Anki review cards store `due` as days relative to the collection
     * creation time (col.crt). Convert to a real due instant; new/learning
     * cards have no meaningful due here and are left for Kaiteyo's scheduler.
     */
    private fun ankiDueInstant(type: Int, queue: Int, due: Long, conn: Connection): Instant? {
        if (type != 2 || queue !in 1..3) return null
        val crt = runCatching {
            conn.createStatement().use { st ->
                val rs = st.executeQuery("SELECT crt FROM col LIMIT 1")
                if (rs.next()) rs.getLong(1) else 0L
            }
        }.getOrDefault(0L)
        if (crt <= 0) return null
        return Instant.fromEpochMilliseconds((crt + due.coerceAtLeast(0)) * 1000L)
    }

    // ------------------------------------------------------------
    // Mappers / helpers
    // ------------------------------------------------------------

    private fun loadDriver() {
        try {
            Class.forName("org.sqlite.JDBC")
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException("The SQLite JDBC driver is unavailable on this build", e)
        }
    }

    private fun ankiType(status: SrsStatus): Int = when (status) {
        SrsStatus.New -> 0
        SrsStatus.Learning, SrsStatus.Relearning -> 1
        SrsStatus.Review, SrsStatus.Suspended, SrsStatus.Buried -> 2
    }

    private fun ankiQueue(status: SrsStatus): Int = when (status) {
        SrsStatus.New -> 0
        SrsStatus.Learning -> 1
        SrsStatus.Review -> 2
        SrsStatus.Relearning -> 3
        SrsStatus.Suspended -> -1
        SrsStatus.Buried -> -3
    }

    private fun statusFromAnki(type: Int, queue: Int): SrsStatus = when {
        queue == -1 -> SrsStatus.Suspended
        queue <= -2 -> SrsStatus.Buried
        type == 0 || queue == 0 -> SrsStatus.New
        type == 1 || queue == 1 -> SrsStatus.Learning
        queue == 3 -> SrsStatus.Relearning
        else -> SrsStatus.Review
    }

    /** Anki stores review-card due as whole days relative to the collection clock. */
    private fun ankiDue(card: DesktopCard, nowMs: Long): Long {
        if (card.status == SrsStatus.New || card.dueAt == null) return 0L
        return (card.dueAt.toEpochMilliseconds() - nowMs) / 86_400_000L
    }

    /** Deterministic 10-char Anki-style GUID derived from the card id. */
    private fun cardGuid(card: DesktopCard): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        var value = card.id.hashCode().toLong() and 0x7fffffffL
        val sb = StringBuilder()
        repeat(10) {
            sb.append(alphabet[(value % alphabet.length).toInt()])
            value /= alphabet.length
        }
        return sb.toString()
    }

    /** Anki's duplicate-detection checksum: sum of front bytes, 32-bit masked. */
    fun checksum(text: String): Long {
        var sum = 0L
        text.toByteArray(Charsets.UTF_8).forEach { sum += it.toByte().toLong() and 0xff }
        return sum and 0xffffffffL
    }

    // ------------------------------------------------------------
    // col.decks / col.models JSON (built with kotlinx.serialization so
    // user-supplied deck names can never break out of the JSON).
    // ------------------------------------------------------------

    private fun decksJson(decks: Collection<AnkiDeck>): String {
        val obj = JsonObject(
            decks.associate { deck ->
                deck.id.toString() to JsonObject(
                    mapOf(
                        "id" to JsonPrimitive(deck.id),
                        "name" to JsonPrimitive(deck.name),
                        "mod" to JsonPrimitive(deck.mod),
                        "usn" to JsonPrimitive(-1),
                        "lrnToday" to kotlinx.serialization.json.buildJsonArray { add(JsonPrimitive(0)); add(JsonPrimitive(0)) },
                        "revToday" to kotlinx.serialization.json.buildJsonArray { add(JsonPrimitive(0)); add(JsonPrimitive(0)) },
                        "newToday" to kotlinx.serialization.json.buildJsonArray { add(JsonPrimitive(0)); add(JsonPrimitive(0)) },
                        "timeToday" to kotlinx.serialization.json.buildJsonArray { add(JsonPrimitive(0)); add(JsonPrimitive(0)) },
                        "collapsed" to JsonPrimitive(false),
                        "browserCollapsed" to JsonPrimitive(false),
                        "desc" to JsonPrimitive(""),
                        "dyn" to JsonPrimitive(0),
                        "conf" to JsonPrimitive(1),
                        "extendNew" to JsonPrimitive(10),
                        "extendRev" to JsonPrimitive(50)
                    )
                )
            }
        )
        return obj.toString()
    }

    private fun modelsJson(modelId: Long, deckId: Long): String {
        val obj = JsonObject(
            mapOf(
                modelId.toString() to JsonObject(
                    mapOf(
                        "id" to JsonPrimitive(modelId),
                        "name" to JsonPrimitive("Kaiteyo - Basic"),
                        "type" to JsonPrimitive(0),
                        "mod" to JsonPrimitive(0),
                        "usn" to JsonPrimitive(-1),
                        "sortf" to JsonPrimitive(0),
                        "did" to JsonPrimitive(deckId),
                        "tmpls" to kotlinx.serialization.json.buildJsonArray {
                            add(
                                JsonObject(
                                    mapOf(
                                        "name" to JsonPrimitive("Card 1"),
                                        "ord" to JsonPrimitive(0),
                                        "qfmt" to JsonPrimitive("{{Front}}"),
                                        "afmt" to JsonPrimitive("{{FrontSide}}<hr id=answer>{{Back}}"),
                                        "bqfmt" to JsonPrimitive(""),
                                        "bafmt" to JsonPrimitive(""),
                                        "did" to JsonNull,
                                        "bfont" to JsonPrimitive(""),
                                        "bqid" to JsonPrimitive(0),
                                        "baid" to JsonPrimitive(0)
                                    )
                                )
                            )
                        },
                        "flds" to kotlinx.serialization.json.buildJsonArray {
                            add(fieldJson("Front"))
                            add(fieldJson("Back"))
                        },
                        "css" to JsonPrimitive(".card { font-family: arial; font-size: 20px; text-align: center; color: black; background-color: white; }"),
                        "latexPre" to JsonPrimitive(""),
                        "latexPost" to JsonPrimitive("")
                    )
                )
            )
        )
        return obj.toString()
    }

    private fun fieldJson(name: String): JsonObject = JsonObject(
        mapOf(
            "name" to JsonPrimitive(name),
            "ord" to JsonPrimitive(0),
            "sticky" to JsonPrimitive(false),
            "rtl" to JsonPrimitive(false),
            "font" to JsonPrimitive("Arial"),
            "size" to JsonPrimitive(20),
            "media" to kotlinx.serialization.json.buildJsonArray { }
        )
    )

    private data class AnkiDeck(val id: Long, val source: String, val name: String, val mod: Long)

    private fun ankiDeck(id: Long, source: String, name: String, modSeconds: Long): AnkiDeck =
        AnkiDeck(
            id = id,
            source = source,
            name = name.replace("\\", " ").replace(":", " - ").replace("\n", " ").trim().ifBlank { "Kaiteyo" },
            mod = modSeconds
        )
}
