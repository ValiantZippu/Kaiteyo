package io.kaiteyo.kjd.api

import io.kaiteyo.kjd.db.SchemaMigrator
import io.kaiteyo.kjd.model.Component
import io.kaiteyo.kjd.model.EntityType
import io.kaiteyo.kjd.model.FuriganaSegment
import io.kaiteyo.kjd.model.Kanji
import io.kaiteyo.kjd.model.KanaCharacter
import io.kaiteyo.kjd.model.Meaning
import io.kaiteyo.kjd.model.Radical
import io.kaiteyo.kjd.model.Reading
import io.kaiteyo.kjd.model.Sense
import io.kaiteyo.kjd.model.Stroke
import io.kaiteyo.kjd.model.VocabularyEntry
import io.kaiteyo.kjd.search.FtsSearchEngine
import io.kaiteyo.kjd.search.SearchResult
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import kotlinx.serialization.decodeFromString

/**
 * The public KJD API — the stable boundary applications consume.
 *
 * This class is intentionally free of UI types (no Compose) and does not
 * expose SQLite/raw-database internals. All lookups are thread-safe for
 * concurrent reads (each call opens its own statement on the shared
 * read-only connection).
 *
 * Typical usage:
 * ```
 * val db = JapaneseDatabase.open(File("kjd-japanese.db"))
 * val entry = db.lookupVocabulary("食べる")   // VocabularyEntry?
 * val kanji = db.lookupKanji("食")            // Kanji?
 * val results = db.search("たべる")            // List<SearchResult>
 * ```
 */
class JapaneseDatabase private constructor(
    private val connection: Connection
) : AutoCloseable {

    companion object {

        val jsonCodec = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

        /**
         * Open an existing generated KJD database (read-mostly).
         *
         * If the database was produced by an older generator, it is upgraded
         * in place first (additive schema migrations only — no data loss).
         */
        fun open(file: File): JapaneseDatabase {
            if (!file.exists()) {
                throw IllegalArgumentException("KJD database not found: ${file.absolutePath}")
            }
            val connection = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
            SchemaMigrator.migrate(connection)
            connection.createStatement().use { stmt ->
                stmt.execute("PRAGMA query_only = ON")
            }
            return JapaneseDatabase(connection)
        }

        /** Explicitly upgrade a database to the current schema without opening it. */
        fun migrate(file: File, logger: (String) -> Unit = {}) {
            if (!file.exists()) {
                throw IllegalArgumentException("KJD database not found: ${file.absolutePath}")
            }
            DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { connection ->
                SchemaMigrator.migrate(connection, logger)
            }
        }

        /** Open using an existing connection (advanced). */
        fun open(connection: Connection): JapaneseDatabase = JapaneseDatabase(connection)

        /** Convenience: open the database located next to a classpath resource. */
        fun openResource(name: String): JapaneseDatabase {
            val resource = javaClass.classLoader.getResource(name)
                ?: throw IllegalArgumentException("Resource not found: $name")
            return open(File(resource.toURI()))
        }
    }

    // ===============================================================
    // Schema / metadata
    // ===============================================================

    /** The schema version recorded inside this database. */
    fun schemaVersion(): Int = queryMeta("schema_version")?.toIntOrNull() ?: -1

    /** The generator version that produced this database. */
    fun generatorVersion(): String? = queryMeta("generator_version")

    /** Raw metadata value. */
    fun queryMeta(key: String): String? =
        connection.prepareStatement("SELECT value FROM meta WHERE key = ?").use { stmt ->
            stmt.setString(1, key)
            stmt.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
        }

    // ===============================================================
    // Kanji
    // ===============================================================

    /** Lookup a kanji by its literal character. */
    fun lookupKanji(literal: String): Kanji? =
        connection.prepareStatement("SELECT id FROM kanji WHERE literal = ?").use { stmt ->
            stmt.setString(1, literal)
            stmt.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
        }?.let { kanjiById(it) }

    /** Lookup a kanji by canonical id ("kanji:食"). */
    fun kanjiById(id: String): Kanji? =
        connection.prepareStatement(
            """
            SELECT id, literal, codepoint, normalized, stroke_count, grade, radical,
                   on_readings, kun_readings, meanings, jlpt, frequency
            FROM kanji WHERE id = ?
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, id)
            stmt.executeQuery().use { rs -> if (rs.next()) rs.toKanji() else null }
        }

    /** All kanji (paged for large datasets). */
    fun allKanji(limit: Int = 1000, offset: Int = 0): List<Kanji> =
        connection.prepareStatement(
            """
            SELECT id, literal, codepoint, normalized, stroke_count, grade, radical,
                   on_readings, kun_readings, meanings, jlpt, frequency
            FROM kanji ORDER BY literal LIMIT ? OFFSET ?
            """.trimIndent()
        ).use { stmt ->
            stmt.setInt(1, limit)
            stmt.setInt(2, offset)
            stmt.executeQuery().use { rs ->
                buildList { while (rs.next()) add(rs.toKanji()) }
            }
        }

    fun kanjiCount(): Int =
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT COUNT(*) FROM kanji").use { rs ->
                if (rs.next()) rs.getInt(1) else 0
            }
        }

    // ===============================================================
    // Strokes
    // ===============================================================

    /** Stroke order for a kanji or kana character, ordered by stroke index. */
    fun strokesFor(literal: String): List<Stroke> {
        val characterId = connection.prepareStatement(
            "SELECT id FROM kanji WHERE literal = ? UNION SELECT id FROM kana WHERE literal = ?"
        ).use { stmt ->
            stmt.setString(1, literal)
            stmt.setString(2, literal)
            stmt.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
        } ?: return emptyList()
        return connection.prepareStatement(
            """
            SELECT id, kanji_id, stroke_index, path, path_type, direction,
                   min_x, min_y, max_x, max_y
            FROM kanji_stroke WHERE kanji_id = ? ORDER BY stroke_index
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, characterId)
            stmt.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        add(
                            Stroke(
                                id = io.kaiteyo.kjd.model.EntityId(rs.getString(1)),
                                index = rs.getInt(3),
                                characterId = io.kaiteyo.kjd.model.EntityId(rs.getString(2)),
                                path = rs.getString(4),
                                pathType = rs.getString(5).let {
                                    io.kaiteyo.kjd.model.StrokePathType.entries.firstOrNull { e -> e.name == it }
                                        ?: io.kaiteyo.kjd.model.StrokePathType.KanjiVg
                                },
                                direction = rs.getString(6)?.let {
                                    io.kaiteyo.kjd.model.StrokeDirection.entries.firstOrNull { e -> e.name == it }
                                },
                                boundingBox = (rs.getObject(7) != null).let {
                                    if (!it) null else io.kaiteyo.kjd.model.BoundingBox(
                                        minX = rs.getFloat(7), minY = rs.getFloat(8),
                                        maxX = rs.getFloat(9), maxY = rs.getFloat(10)
                                    )
                                }
                            )
                        )
                    }
                }
            }
        }
    }

    /** Structural components of a kanji (radical and/or KanjiVG parts). */
    fun componentsFor(literal: String): List<Component> {
        val kanjiId = connection.prepareStatement("SELECT id FROM kanji WHERE literal = ?").use { stmt ->
            stmt.setString(1, literal)
            stmt.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
        } ?: return emptyList()
        return connection.prepareStatement(
            "SELECT id, kanji_id, character, role, stroke_count FROM component " +
                "WHERE kanji_id = ? ORDER BY character"
        ).use { stmt ->
            stmt.setString(1, kanjiId)
            stmt.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        add(
                            Component(
                                id = io.kaiteyo.kjd.model.EntityId(rs.getString(1)),
                                character = rs.getString(3),
                                role = rs.getString(4),
                                strokeCount = (rs.getObject(5) as? Int)
                                    ?: (rs.getObject(5)?.toString()?.toIntOrNull())
                            )
                        )
                    }
                }
            }
        }
    }

    // ===============================================================
    // Vocabulary
    // ===============================================================

    /** Lookup a vocabulary entry by expression or reading (first match). */
    fun lookupVocabulary(query: String): VocabularyEntry? {
        val ids = mutableListOf<String>()
        connection.prepareStatement("SELECT id FROM vocab WHERE expression = ?").use { stmt ->
            stmt.setString(1, query)
            stmt.executeQuery().use { rs -> while (rs.next()) ids.add(rs.getString(1)) }
        }
        if (ids.isEmpty()) {
            connection.prepareStatement("SELECT id FROM vocab WHERE reading = ?").use { stmt ->
                stmt.setString(1, query)
                stmt.executeQuery().use { rs -> while (rs.next()) ids.add(rs.getString(1)) }
            }
        }
        return ids.firstOrNull()?.let { vocabularyById(it) }
    }

    /** Lookup a vocabulary entry by canonical id. */
    fun vocabularyById(id: String): VocabularyEntry? =
        connection.prepareStatement(
            """
            SELECT id, expression, reading, readings_json, kanji_ids, furigana_json,
                   jlpt, frequency, pos
            FROM vocab WHERE id = ?
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, id)
            stmt.executeQuery().use { rs -> if (rs.next()) rs.toVocabulary() else null }
        }

    /** Senses for a vocabulary entry. */
    fun sensesFor(vocabId: String): List<Sense> =
        connection.prepareStatement(
            """
            SELECT id, vocab_id, sense_index, glosses_json, pos_json, fields_json,
                   misc_json, restrictions_json
            FROM sense WHERE vocab_id = ? ORDER BY sense_index
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, vocabId)
            stmt.executeQuery().use { rs ->
                buildList { while (rs.next()) add(rs.toSense()) }
            }
        }

    /** Furigana segments for a vocabulary entry. */
    fun furiganaFor(vocabId: String): List<FuriganaSegment> =
        connection.prepareStatement(
            "SELECT segments_json FROM vocab_furigana WHERE vocab_id = ?"
        ).use { stmt ->
            stmt.setString(1, vocabId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) decodeList(rs.getString(1)) else emptyList()
            }
        }

    fun vocabularyCount(): Int =
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT COUNT(*) FROM vocab").use { rs ->
                if (rs.next()) rs.getInt(1) else 0
            }
        }

    fun kanaCount(): Int = tableCount("kana")
    fun senseCount(): Int = tableCount("sense")
    fun radicalCount(): Int = tableCount("radical")
    fun componentCount(): Int = tableCount("component")
    fun relationshipCount(): Int = tableCount("relationship")

    private fun tableCount(table: String): Int =
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT COUNT(*) FROM $table").use { rs ->
                if (rs.next()) rs.getInt(1) else 0
            }
        }

    // ===============================================================
    // Readings / meanings / radicals
    // ===============================================================

    fun readingsFor(literal: String): List<Reading> =
        lookupKanji(literal)?.let { kanji ->
            (kanji.onReadings + kanji.kunReadings)
        } ?: emptyList()

    fun meaningsFor(literal: String): List<Meaning> =
        lookupKanji(literal)?.meanings ?: emptyList()

    fun radicalFor(literal: String): Radical? {
        val radicalText = lookupKanji(literal)?.radical ?: return null
        return connection.prepareStatement(
            "SELECT id, number, character, name, meanings, stroke_count, unicode_codepoint FROM radical WHERE name = ? OR character = ? OR number = ?"
        ).use { stmt ->
            stmt.setString(1, radicalText)
            stmt.setString(2, radicalText)
            stmt.setInt(3, radicalText.toIntOrNull() ?: -1)
            stmt.executeQuery().use { rs -> if (rs.next()) rs.toRadical() else null }
        }
    }

    fun radicals(): List<Radical> =
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT id, number, character, name, meanings, stroke_count, unicode_codepoint FROM radical ORDER BY number")
                .use { rs -> buildList { while (rs.next()) add(rs.toRadical()) } }
        }

    // ===============================================================
    // Kana
    // ===============================================================

    fun lookupKana(literal: String): KanaCharacter? =
        connection.prepareStatement(
            "SELECT id, literal, codepoint, syllabary, stroke_count, romaji FROM kana WHERE literal = ?"
        ).use { stmt ->
            stmt.setString(1, literal)
            stmt.executeQuery().use { rs -> if (rs.next()) rs.toKana() else null }
        }

    // ===============================================================
    // Search (database-backed: relational index + SQLite FTS5)
    // ===============================================================

    private val searchEngine = FtsSearchEngine(connection)

    /**
     * Search across kanji + vocabulary. Queries hit on-disk indexes only —
     * the dictionary is never materialized in memory (exact → prefix → FTS5
     * meaning search, deterministic ranking).
     */
    fun search(query: String, limit: Int = 50): List<SearchResult> =
        searchEngine.search(query, limit)

    /** Search restricted to one entity type (kanji or vocabulary). */
    fun searchByType(type: EntityType, query: String, limit: Int = 50): List<SearchResult> =
        searchEngine.search(query, limit, type)

    /** Fast suggestion lookup for autocomplete UI. */
    fun autocomplete(query: String, limit: Int = 20): List<SearchResult> =
        searchEngine.autocomplete(query, limit)

    override fun close() {
        runCatching { connection.close() }
    }

    // ===============================================================
    // Row mappers
    // ===============================================================

    private fun java.sql.ResultSet.toKanji(): Kanji {
        val literal = getString(2)
        return Kanji(
            id = io.kaiteyo.kjd.model.EntityId(getString(1)),
            character = io.kaiteyo.kjd.model.Character(
                id = io.kaiteyo.kjd.model.EntityId(getString(1)),
                literal = literal,
                codepoint = getInt(3),
                normalized = getString(4),
                characterType = io.kaiteyo.kjd.model.CharacterType.Kanji,
                strokeCount = (getObject(5) as? Int) ?: (getObject(5)?.toString()?.toIntOrNull()),
                grade = (getObject(6) as? Int) ?: (getObject(6)?.toString()?.toIntOrNull()),
                radical = getString(7),
                readings = parseReadings(getString(8)).map { Reading(it, "on", emptyList()) } +
                    parseReadings(getString(9)).map { Reading(it, "kun", emptyList()) },
                meanings = getString(10).split("; ").filter { it.isNotBlank() }
                    .map { Meaning(it, "en", emptyList()) },
                jlpt = parseJlpt(getString(11)),
                frequency = emptyList()
            ),
            onReadings = parseReadings(getString(8)).map { Reading(it, "on", emptyList()) },
            kunReadings = parseReadings(getString(9)).map { Reading(it, "kun", emptyList()) },
            meanings = getString(10).split("; ").filter { it.isNotBlank() }
                .map { Meaning(it, "en", emptyList()) },
            grade = (getObject(6) as? Int) ?: (getObject(6)?.toString()?.toIntOrNull()),
            jlpt = parseJlpt(getString(11)),
            strokeCount = (getObject(5) as? Int) ?: (getObject(5)?.toString()?.toIntOrNull()),
            radical = getString(7),
            strokes = strokesFor(literal)
        )
    }

    private fun java.sql.ResultSet.toVocabulary(): VocabularyEntry =
        VocabularyEntry(
            id = io.kaiteyo.kjd.model.EntityId(getString(1)),
            expression = getString(2),
            readings = decodeList<String>(getString(4)).map {
                io.kaiteyo.kjd.model.VocabularyReading(value = it, isKanaOnly = io.kaiteyo.kjd.normalize.JapaneseNormalizer.isKanaOnly(it))
            },
            kanjiIds = decodeList<String>(getString(5)).map { io.kaiteyo.kjd.model.EntityId(it) },
            furigana = furiganaFor(getString(1)),
            jlpt = parseJlpt(getString(7)),
            senses = sensesFor(getString(1)),
            partsOfSpeech = decodeList<String>(getString(9)).map { io.kaiteyo.kjd.model.PartOfSpeech(it, emptyList()) }
        )

    private fun java.sql.ResultSet.toSense(): Sense =
        Sense(
            id = io.kaiteyo.kjd.model.EntityId(getString(1)),
            vocabularyId = io.kaiteyo.kjd.model.EntityId(getString(2)),
            index = getInt(3),
            glosses = decodeList<kotlin.Pair<String, String>>(getString(4)).map { (value, lang) ->
                Meaning(value, lang, emptyList())
            },
            partsOfSpeech = decodeList<String>(getString(5)).map { io.kaiteyo.kjd.model.PartOfSpeech(it, emptyList()) },
            fields = decodeList(getString(6)),
            misc = decodeList(getString(7)),
            restrictions = decodeList(getString(8))
        )

    private fun java.sql.ResultSet.toRadical(): Radical =
        Radical(
            id = io.kaiteyo.kjd.model.EntityId(getString(1)),
            number = (getObject(2) as? Int) ?: (getObject(2)?.toString()?.toIntOrNull()),
            character = getString(3),
            name = getString(4),
            meanings = getString(5).split("; ").filter { it.isNotBlank() }
                .map { Meaning(it, "en", emptyList()) },
            strokeCount = (getObject(6) as? Int) ?: (getObject(6)?.toString()?.toIntOrNull()),
            unicodeCodepoint = (getObject(7) as? Int) ?: (getObject(7)?.toString()?.toIntOrNull())
        )

    private fun java.sql.ResultSet.toKana(): KanaCharacter {
        val literal = getString(2)
        val id = io.kaiteyo.kjd.model.EntityId(getString(1))
        return KanaCharacter(
            id = id,
            character = io.kaiteyo.kjd.model.Character(
                id = id, literal = literal, codepoint = getInt(3),
                normalized = io.kaiteyo.kjd.normalize.JapaneseNormalizer.toNfc(literal),
                characterType = io.kaiteyo.kjd.model.CharacterType.Kana
            ),
            syllabary = io.kaiteyo.kjd.model.Syllabary.valueOf(getString(4)),
            strokeCount = (getObject(5) as? Int) ?: (getObject(5)?.toString()?.toIntOrNull()),
            romaji = getString(6)
        )
    }

    private fun parseReadings(value: String): List<String> =
        value.split(" ").filter { it.isNotBlank() }

    private fun parseJlpt(value: String): List<io.kaiteyo.kjd.model.JlptClassification> =
        value.split(",").filter { it.isNotBlank() }.mapNotNull { tag ->
            Regex("n([1-5])", RegexOption.IGNORE_CASE).find(tag)?.groupValues?.get(1)?.toIntOrNull()
                ?.let { level ->
                    io.kaiteyo.kjd.model.JlptClassification(
                        level = level,
                        source = io.kaiteyo.kjd.model.SourceRef(sourceId = "tanos-jlpt", isCanonical = true)
                    )
                }
        }

    private inline fun <reified T> decodeList(json: String): List<T> =
        runCatching {
            jsonCodec.decodeFromString<List<T>>(json)
        }.getOrDefault(emptyList())
}

/** Thin convenience API class for consumers that prefer named accessors. */
class JapaneseDataApi(private val database: JapaneseDatabase) {

    fun getKanji(literal: String) = database.lookupKanji(literal)
    fun getVocabulary(expression: String) = database.lookupVocabulary(expression)
    fun searchKanji(query: String, limit: Int = 50) =
        database.searchByType(EntityType.Kanji, query, limit)
    fun searchVocabulary(query: String, limit: Int = 50) =
        database.searchByType(EntityType.Vocabulary, query, limit)
    fun lookup(query: String) = database.lookupVocabulary(query) ?: database.lookupKanji(query)
    fun getStrokeData(literal: String) = database.strokesFor(literal)
    fun getRadical(literal: String) = database.radicalFor(literal)
    fun getComponents(literal: String) = database.componentsFor(literal)
    fun getReadings(literal: String) = database.readingsFor(literal)
    fun getMeanings(literal: String) = database.meaningsFor(literal)
    fun getJlpt(literal: String) = database.lookupKanji(literal)?.jlpt ?: emptyList()
    fun getFrequency(literal: String) = database.lookupKanji(literal)?.frequency ?: emptyList()
    fun getFurigana(expression: String) = database.lookupVocabulary(expression)?.let {
        database.furiganaFor(it.id.value)
    } ?: emptyList()
    fun close() = database.close()
}
