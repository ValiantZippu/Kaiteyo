package io.kaiteyo.kjd.db

import io.kaiteyo.kjd.KjdVersion
import io.kaiteyo.kjd.model.CanonicalDatabase
import io.kaiteyo.kjd.normalize.JapaneseNormalizer
import io.kaiteyo.kjd.patch.DatabaseFingerprint
import io.kaiteyo.kjd.source.AttributionManifest
import io.kaiteyo.kjd.source.SourceMetadata
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException
import java.time.Instant

/**
 * Writes a canonical database snapshot into a portable SQLite file.
 *
 * The generator is deterministic for identical input (stable ordering, no
 * timestamps in data rows). Generation is wrapped in a single transaction so
 * a failure never leaves a half-written database.
 */
class DatabaseWriter(
    private val json: Json = Json { encodeDefaults = true }
) {

    fun write(database: CanonicalDatabase, target: File, sources: List<SourceMetadata>) {
        target.parentFile?.mkdirs()
        val url = "jdbc:sqlite:${target.absolutePath}"
        DriverManager.getConnection(url).use { connection ->
            connection.autoCommit = false
            try {
                connection.createStatement().use { it.execute(Schema.DROP_ALL) }
                Schema.CREATE_TABLES.forEach { ddl ->
                    connection.createStatement().use { it.execute(ddl) }
                }
                writeMeta(connection, sources)
                writeSources(connection, sources)
                writeKanji(connection, database)
                writeKana(connection, database)
                writeVocabulary(connection, database)
                writeSenses(connection, database)
                writeComponents(connection, database)
                writeRelationships(connection, database)
                writeSearchIndex(connection, database)
                writeFtsIndexes(connection, database)
                // The migration framework keys off PRAGMA user_version.
                connection.createStatement().use { it.execute("PRAGMA user_version = ${Schema.SCHEMA_VERSION}") }
                // Deterministic content fingerprint — the incremental update
                // system verifies against this before applying any patch.
                val fingerprint = DatabaseFingerprint.compute(connection)
                connection.prepareStatement(
                    "INSERT OR REPLACE INTO meta (key, value) VALUES (?, ?)"
                ).use { stmt ->
                    stmt.setString(1, Schema.Meta.STATE_FINGERPRINT)
                    stmt.setString(2, fingerprint)
                    stmt.executeUpdate()
                }
                connection.commit()
            } catch (t: Throwable) {
                connection.rollback()
                throw t
            }
        }
    }

    private fun writeMeta(connection: Connection, sources: List<SourceMetadata>) {
        val manifest = AttributionManifest(
            platform = KjdVersion.PLATFORM_NAME,
            generatedBy = KjdVersion.GENERATOR_VERSION,
            generatedAt = Instant.now().toString(),
            schemaVersion = Schema.SCHEMA_VERSION,
            sources = sources
        )
        val rows = listOf(
            Schema.Meta.SCHEMA_VERSION_KEY to Schema.SCHEMA_VERSION.toString(),
            Schema.Meta.GENERATOR_VERSION to KjdVersion.GENERATOR_VERSION,
            Schema.Meta.GENERATED_AT to manifest.generatedAt,
            Schema.Meta.SOURCES_JSON to json.encodeToString(manifest)
        )
        connection.prepareStatement("INSERT INTO meta (key, value) VALUES (?, ?)").use { stmt ->
            rows.forEach { (k, v) ->
                stmt.setString(1, k)
                stmt.setString(2, v)
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    private fun writeSources(connection: Connection, sources: List<SourceMetadata>) {
        connection.prepareStatement(
            """
            INSERT INTO source (id, name, homepage, license_id, license_name, license_url,
                allows_redistribution, attribution_required, version, retrieved_at, attribution,
                redistribution_notes, modification_notes, source_url)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """.trimIndent()
        ).use { stmt ->
            for (source in sources) {
                stmt.setString(1, source.id)
                stmt.setString(2, source.name)
                stmt.setString(3, source.homepage)
                stmt.setString(4, source.license.id)
                stmt.setString(5, source.license.name)
                stmt.setString(6, source.license.url)
                stmt.setInt(7, if (source.license.allowsRedistribution) 1 else 0)
                stmt.setInt(8, if (source.license.attributionRequired) 1 else 0)
                stmt.setString(9, source.version)
                stmt.setString(10, source.retrievedAt)
                stmt.setString(11, source.attribution)
                stmt.setString(12, source.redistributionNotes)
                stmt.setString(13, source.modificationNotes)
                stmt.setString(14, source.sourceUrl)
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    private fun writeKanji(connection: Connection, database: CanonicalDatabase) {
        connection.prepareStatement(
            """
            INSERT INTO kanji (id, literal, codepoint, normalized, stroke_count, grade, radical,
                on_readings, kun_readings, meanings, jlpt, frequency)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
            """.trimIndent()
        ).use { stmt ->
            for (kanji in database.kanji) {
                stmt.setString(1, kanji.id.value)
                stmt.setString(2, kanji.character.literal)
                stmt.setInt(3, kanji.character.codepoint)
                stmt.setString(4, kanji.character.normalized)
                stmt.setIntOrNull(5, kanji.strokeCount)
                stmt.setIntOrNull(6, kanji.grade)
                stmt.setString(7, kanji.radical)
                stmt.setString(8, kanji.onReadings.joinToString(" ") { it.value })
                stmt.setString(9, kanji.kunReadings.joinToString(" ") { it.value })
                stmt.setString(10, kanji.meanings.joinToString("; ") { it.value })
                stmt.setString(11, kanji.jlpt.joinToString(",") { "n${it.level}" })
                stmt.setString(12, kanji.frequency.joinToString(",") { "${it.value}" })
                stmt.addBatch()
            }
            stmt.executeBatch()
        }

        // Strokes.
        connection.prepareStatement(
            """
            INSERT INTO kanji_stroke (id, kanji_id, stroke_index, path, path_type, direction,
                min_x, min_y, max_x, max_y)
            VALUES (?,?,?,?,?,?,?,?,?,?)
            """.trimIndent()
        ).use { stmt ->
            for (kanji in database.kanji) {
                for (stroke in kanji.strokes) {
                    stmt.setString(1, stroke.id.value)
                    stmt.setString(2, kanji.id.value)
                    stmt.setInt(3, stroke.index)
                    stmt.setString(4, stroke.path)
                    stmt.setString(5, stroke.pathType.name)
                    stmt.setString(6, stroke.direction?.name)
                    stmt.setFloatOrNull(7, stroke.boundingBox?.minX)
                    stmt.setFloatOrNull(8, stroke.boundingBox?.minY)
                    stmt.setFloatOrNull(9, stroke.boundingBox?.maxX)
                    stmt.setFloatOrNull(10, stroke.boundingBox?.maxY)
                    stmt.addBatch()
                }
            }
            stmt.executeBatch()
        }
    }

    private fun writeKana(connection: Connection, database: CanonicalDatabase) {
        connection.prepareStatement(
            """
            INSERT INTO kana (id, literal, codepoint, syllabary, stroke_count, romaji)
            VALUES (?,?,?,?,?,?)
            """.trimIndent()
        ).use { stmt ->
            for (kana in database.kana) {
                stmt.setString(1, kana.id.value)
                stmt.setString(2, kana.character.literal)
                stmt.setInt(3, kana.character.codepoint)
                stmt.setString(4, kana.syllabary.name)
                stmt.setIntOrNull(5, kana.strokeCount)
                stmt.setString(6, kana.romaji)
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    private fun writeVocabulary(connection: Connection, database: CanonicalDatabase) {
        connection.prepareStatement(
            """
            INSERT INTO vocab (id, expression, reading, readings_json, kanji_ids, furigana_json,
                jlpt, frequency, pos)
            VALUES (?,?,?,?,?,?,?,?,?)
            """.trimIndent()
        ).use { stmt ->
            for (entry in database.vocabulary) {
                stmt.setString(1, entry.id.value)
                stmt.setString(2, entry.expression)
                stmt.setString(3, entry.readings.firstOrNull()?.value ?: "")
                stmt.setString(4, json.encodeToString(entry.readings.map { it.value }))
                stmt.setString(5, json.encodeToString(entry.kanjiIds.map { it.value }))
                stmt.setString(6, json.encodeToString(entry.furigana))
                stmt.setString(7, entry.jlpt.joinToString(",") { "n${it.level}" })
                stmt.setString(8, entry.frequency.joinToString(",") { "${it.value}" })
                stmt.setString(9, json.encodeToString(entry.partsOfSpeech.map { it.value }))
                stmt.addBatch()
            }
            stmt.executeBatch()
        }

        // Furigana table.
        connection.prepareStatement(
            "INSERT OR REPLACE INTO vocab_furigana (vocab_id, segments_json) VALUES (?, ?)"
        ).use { stmt ->
            for (entry in database.vocabulary) {
                if (entry.furigana.isEmpty()) continue
                stmt.setString(1, entry.id.value)
                stmt.setString(2, json.encodeToString(entry.furigana))
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    private fun writeSenses(connection: Connection, database: CanonicalDatabase) {
        connection.prepareStatement(
            """
            INSERT INTO sense (id, vocab_id, sense_index, glosses_json, pos_json, fields_json,
                misc_json, restrictions_json)
            VALUES (?,?,?,?,?,?,?,?)
            """.trimIndent()
        ).use { stmt ->
            for (sense in database.senses) {
                stmt.setString(1, sense.id.value)
                stmt.setString(2, sense.vocabularyId.value)
                stmt.setInt(3, sense.index)
                stmt.setString(4, json.encodeToString(sense.glosses.map { it.value to it.language }))
                stmt.setString(5, json.encodeToString(sense.partsOfSpeech.map { it.value }))
                stmt.setString(6, json.encodeToString(sense.fields))
                stmt.setString(7, json.encodeToString(sense.misc))
                stmt.setString(8, json.encodeToString(sense.restrictions))
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    private fun writeComponents(connection: Connection, database: CanonicalDatabase) {
        connection.prepareStatement(
            "INSERT INTO component (id, kanji_id, character, role, stroke_count) VALUES (?,?,?,?,?)"
        ).use { stmt ->
            for (kanji in database.kanji) {
                for (component in kanji.components) {
                    stmt.setString(1, component.id.value)
                    stmt.setString(2, kanji.id.value)
                    stmt.setString(3, component.character)
                    stmt.setString(4, component.role)
                    stmt.setIntOrNull(5, component.strokeCount)
                    stmt.addBatch()
                }
            }
            stmt.executeBatch()
        }
    }

    /**
     * Normalized relational search index: one row per searchable key of each
     * kanji/vocab entity. Keys are NFC-folded, punctuation-stripped and
     * katakana→hiragana folded so kana-equivalent queries match.
     */
    private fun writeSearchIndex(connection: Connection, database: CanonicalDatabase) {
        connection.prepareStatement(
            """
            INSERT INTO search_index
                (entity_type, entity_id, search_key, display_text, reading, gloss, frequency)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { stmt ->
            for (kanji in database.kanji) {
                val readings = (kanji.onReadings + kanji.kunReadings).map { it.value }
                val meaning = kanji.meanings.firstOrNull()?.value.orEmpty()
                val keys = (listOf(kanji.character.literal) + readings).map { indexKey(it) } +
                    if (meaning.isNotBlank()) listOf(indexKey(meaning)) else emptyList()
                keys.distinct().forEach { key ->
                    stmt.setString(1, "kanji")
                    stmt.setString(2, kanji.id.value)
                    stmt.setString(3, key)
                    stmt.setString(4, kanji.character.literal)
                    stmt.setString(5, readings.firstOrNull().orEmpty())
                    stmt.setString(6, meaning.take(120))
                    stmt.setInt(7, 0)
                    stmt.addBatch()
                }
            }
            for (entry in database.vocabulary) {
                val readings = entry.readings.map { it.value }
                val gloss = entry.senses.firstOrNull()?.glosses?.firstOrNull()?.value.orEmpty()
                val keys = (listOf(entry.expression) + readings).map { indexKey(it) }
                val frequency = entry.frequency.minOfOrNull { it.value } ?: 0
                keys.distinct().forEach { key ->
                    stmt.setString(1, "vocab")
                    stmt.setString(2, entry.id.value)
                    stmt.setString(3, key)
                    stmt.setString(4, entry.expression)
                    stmt.setString(5, readings.firstOrNull().orEmpty())
                    stmt.setString(6, gloss.take(120))
                    stmt.setInt(7, frequency)
                    stmt.addBatch()
                }
            }
            stmt.executeBatch()
        }
    }

    /** FTS5 meaning/gloss indexes (English and other-language full text). */
    private fun writeFtsIndexes(connection: Connection, database: CanonicalDatabase) {
        connection.prepareStatement(
            "INSERT INTO kanji_fts (entity_id, literal, on_readings, kun_readings, meanings) VALUES (?,?,?,?,?)"
        ).use { stmt ->
            for (kanji in database.kanji) {
                stmt.setString(1, kanji.id.value)
                stmt.setString(2, kanji.character.literal)
                stmt.setString(3, kanji.onReadings.joinToString(" ") { it.value })
                stmt.setString(4, kanji.kunReadings.joinToString(" ") { it.value })
                stmt.setString(5, kanji.meanings.joinToString(" ") { it.value })
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
        connection.prepareStatement(
            "INSERT INTO vocab_fts (entity_id, expression, reading, glosses) VALUES (?,?,?,?)"
        ).use { stmt ->
            for (entry in database.vocabulary) {
                stmt.setString(1, entry.id.value)
                stmt.setString(2, entry.expression)
                stmt.setString(3, entry.readings.joinToString(" ") { it.value })
                stmt.setString(4, entry.senses.flatMap { it.glosses }.joinToString(" ") { it.value })
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    /** NFC + case-fold + punctuation-strip + katakana→hiragana index key. */
    private fun indexKey(input: String): String =
        JapaneseNormalizer.katakanaToHiragana(JapaneseNormalizer.searchKey(input))

    private fun writeRelationships(connection: Connection, database: CanonicalDatabase) {
        connection.prepareStatement(
            "INSERT INTO relationship (id, from_id, to_id, relation_type, source) VALUES (?,?,?,?,?)"
        ).use { stmt ->
            for (rel in database.relationships) {
                stmt.setString(1, rel.id.value)
                stmt.setString(2, rel.from.value)
                stmt.setString(3, rel.to.value)
                stmt.setString(4, rel.relationType)
                stmt.setString(5, rel.source.joinToString(",") { it.sourceId })
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    private fun PreparedStatement.setIntOrNull(index: Int, value: Int?) {
        if (value == null) setNull(index, java.sql.Types.INTEGER) else setInt(index, value)
    }

    private fun PreparedStatement.setFloatOrNull(index: Int, value: Float?) {
        if (value == null) setNull(index, java.sql.Types.REAL) else setFloat(index, value)
    }
}

/** Opens an existing generated database and returns a JDBC connection. */
fun openKjdDatabase(file: File): Connection {
    if (!file.exists()) throw SQLException("Database not found: ${file.absolutePath}")
    return DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
}
