package io.kaiteyo.kjd.db

import io.kaiteyo.kjd.normalize.JapaneseNormalizer
import java.sql.Connection

/**
 * Rebuilds the derived indexes (`search_index`, `kanji_fts`, `vocab_fts`)
 * from the content tables.
 *
 * The derived tables are never diffed or shipped in patches — they are pure
 * functions of the content rows, so any code path that changes content
 * (fresh generation, schema migration, incremental patch apply) calls
 * [rebuild] and the indexes converge to exactly the same state.
 */
object IndexRebuilder {

    fun rebuild(connection: Connection) {
        rebuildSearchIndex(connection)
        rebuildFts(connection)
    }

    /** Rebuilds the relational search index from existing kanji/vocab rows. */
    private fun rebuildSearchIndex(connection: Connection) {
        connection.prepareStatement("DELETE FROM search_index").use { it.executeUpdate() }

        val insert = connection.prepareStatement(
            """
            INSERT OR IGNORE INTO search_index
                (entity_type, entity_id, search_key, display_text, reading, gloss, frequency)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        )
        try {
            // Kanji: literal, each reading, first meaning.
            connection.createStatement().use { stmt ->
                stmt.executeQuery(
                    "SELECT id, literal, on_readings, kun_readings, meanings FROM kanji"
                ).use { rs ->
                    while (rs.next()) {
                        val id = rs.getString(1)
                        val literal = rs.getString(2)
                        val readings = (rs.getString(3) + " " + rs.getString(4)).split(" ")
                            .filter { it.isNotBlank() }
                        val gloss = rs.getString(5)
                        val keys = (listOf(literal) + readings).map { indexKey(it) }
                            .plus(if (gloss.isNotBlank()) listOf(indexKey(gloss)) else emptyList())
                        keys.distinct().forEach { key ->
                            insert.setString(1, "kanji")
                            insert.setString(2, id)
                            insert.setString(3, key)
                            insert.setString(4, literal)
                            insert.setString(5, readings.firstOrNull().orEmpty())
                            insert.setString(6, gloss.take(120))
                            insert.setInt(7, 0)
                            insert.addBatch()
                        }
                    }
                }
            }
            // Vocabulary: expression, readings, first gloss.
            connection.createStatement().use { stmt ->
                stmt.executeQuery(
                    "SELECT id, expression, reading, readings_json, frequency FROM vocab"
                ).use { rs ->
                    while (rs.next()) {
                        val id = rs.getString(1)
                        val expression = rs.getString(2)
                        val primaryReading = rs.getString(3)
                        val readings = decodeStrings(rs.getString(4)) + primaryReading
                        val keys = (listOf(expression) + readings).map { indexKey(it) }
                        val frequency = rs.getString(5)
                            .split(",").firstNotNullOfOrNull { it.trim().toIntOrNull() } ?: 0
                        keys.distinct().forEach { key ->
                            insert.setString(1, "vocab")
                            insert.setString(2, id)
                            insert.setString(3, key)
                            insert.setString(4, expression)
                            insert.setString(5, primaryReading)
                            insert.setString(6, "")
                            insert.setInt(7, frequency)
                            insert.addBatch()
                        }
                    }
                }
            }
            insert.executeBatch()
        } finally {
            insert.close()
        }
    }

    /** Rebuilds the FTS5 indexes from existing kanji/vocab rows. */
    private fun rebuildFts(connection: Connection) {
        connection.prepareStatement("DELETE FROM kanji_fts").use { it.executeUpdate() }
        connection.prepareStatement("DELETE FROM vocab_fts").use { it.executeUpdate() }

        connection.prepareStatement(
            "INSERT INTO kanji_fts (entity_id, literal, on_readings, kun_readings, meanings) VALUES (?, ?, ?, ?, ?)"
        ).use { stmt ->
            connection.createStatement().use { s ->
                s.executeQuery("SELECT id, literal, on_readings, kun_readings, meanings FROM kanji").use { rs ->
                    while (rs.next()) {
                        stmt.setString(1, rs.getString(1))
                        stmt.setString(2, rs.getString(2))
                        stmt.setString(3, rs.getString(3))
                        stmt.setString(4, rs.getString(4))
                        stmt.setString(5, rs.getString(5))
                        stmt.addBatch()
                    }
                }
            }
            stmt.executeBatch()
        }

        connection.prepareStatement(
            "INSERT INTO vocab_fts (entity_id, expression, reading, glosses) VALUES (?, ?, ?, ?)"
        ).use { stmt ->
            connection.createStatement().use { s ->
                s.executeQuery("SELECT id, expression, reading FROM vocab").use { rs ->
                    while (rs.next()) {
                        val vocabId = rs.getString(1)
                        stmt.setString(1, vocabId)
                        stmt.setString(2, rs.getString(2))
                        stmt.setString(3, rs.getString(3))
                        stmt.setString(4, vocabGlosses(connection, vocabId))
                        stmt.addBatch()
                    }
                }
            }
            stmt.executeBatch()
        }
    }

    /** Decodes the glosses of a vocabulary entry for the FTS5 index. */
    private fun vocabGlosses(connection: Connection, vocabId: String): String {
        val glosses = mutableListOf<String>()
        connection.prepareStatement("SELECT glosses_json FROM sense WHERE vocab_id = ?").use { stmt ->
            stmt.setString(1, vocabId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val json = rs.getString(1) ?: continue
                    runCatching {
                        kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                            .decodeFromString<List<kotlin.Pair<String, String>>>(json)
                            .forEach { glosses.add(it.first) }
                    }
                }
            }
        }
        return glosses.joinToString(" ")
    }

    /** NFC + case-fold + punctuation-strip + katakana→hiragana index key. */
    private fun indexKey(input: String): String =
        JapaneseNormalizer.katakanaToHiragana(JapaneseNormalizer.searchKey(input))

    private fun decodeStrings(json: String): List<String> =
        runCatching {
            kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString<List<String>>(json)
        }.getOrDefault(emptyList())
}
