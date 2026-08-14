package io.kaiteyo.kjd.search

import io.kaiteyo.kjd.model.EntityId
import io.kaiteyo.kjd.model.EntityType
import io.kaiteyo.kjd.normalize.JapaneseNormalizer
import java.sql.Connection

/**
 * Database-backed search for KJD SQLite databases.
 *
 * Unlike an in-memory index, this engine never materializes the dictionary in
 * RAM: it queries three on-disk structures built at generation time:
 *
 *   1. `search_index`  — normalized relational keys (NFC + case-fold +
 *      punctuation-strip + katakana→hiragana). Handles exact and prefix
 *      lookup of kanji literals, expressions, readings and kana-equivalent
 *      forms using indexed range scans (not table scans).
 *   2. `kanji_fts` / `vocab_fts` — SQLite FTS5 indexes over meanings/glosses
 *      for full-text meaning search (ranked with bm25).
 *
 * Ranking is deterministic: exact match > prefix match > FTS bm25, with
 * frequency as a secondary tie-break for vocabulary.
 */
class FtsSearchEngine(private val connection: Connection) {

    private var ftsAvailable: Boolean? = null

    private fun hasFts(): Boolean {
        ftsAvailable?.let { return it }
        val available = connection.createStatement().use { stmt ->
            stmt.executeQuery(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name IN ('kanji_fts', 'vocab_fts')"
            ).use { rs -> rs.next() && rs.getInt(1) == 2 }
        }
        ftsAvailable = available
        return available
    }

    /**
     * Search across kanji + vocabulary (optionally restricted to one entity
     * type). [query] may be kanji, kana, reading or Latin meaning text.
     */
    fun search(query: String, limit: Int = 50, entityType: EntityType? = null): List<SearchResult> {
        val key = indexKey(query)
        if (key.isBlank()) return emptyList()
        val typeClause = entityType?.let { if (it == EntityType.Kanji) "AND entity_type = 'kanji'" else if (it == EntityType.Vocabulary) "AND entity_type = 'vocab'" else "" }
            ?: ""

        val results = mutableListOf<SearchResult>()

        // ---- 1. Exact key match (indexed equality). ----
        connection.prepareStatement(
            "SELECT entity_type, entity_id, display_text, reading, gloss FROM search_index " +
                "WHERE search_key = ? $typeClause ORDER BY frequency DESC"
        ).use { stmt ->
            stmt.setString(1, key)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    results.add(rs.toResult(1.0f))
                }
            }
        }

        // ---- 2. Prefix match (indexed range scan). ----
        if (results.size < limit) {
            val prefix = escapeLike(key) + "%"
            connection.prepareStatement(
                "SELECT entity_type, entity_id, display_text, reading, gloss FROM search_index " +
                    "WHERE search_key LIKE ? ESCAPE '\\' $typeClause ORDER BY length(search_key), frequency DESC LIMIT ?"
            ).use { stmt ->
                stmt.setString(1, prefix)
                stmt.setInt(2, limit * 2)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        // Exact duplicates already added; keep the best rank per entity.
                        val id = rs.getString(2)
                        if (results.none { it.entityId.value == id }) {
                            results.add(rs.toResult(0.8f))
                        }
                    }
                }
            }
        }

        // ---- 3. FTS5 meaning/gloss full-text search. ----
        if (results.size < limit && hasFts()) {
            val ftsQuery = buildFtsQuery(query)
            if (ftsQuery.isNotBlank()) {
                runCatching { searchFts(ftsQuery, entityType, limit).forEach { results.add(it) } }
            }
        }

        return results
            .distinctBy { it.entityId.value }
            .sortedWith(compareByDescending<SearchResult> { it.relevance }.thenBy { it.entityId.value })
            .take(limit)
    }

    /** Fast suggestion lookup: exact + prefix only (no FTS). */
    fun autocomplete(query: String, limit: Int = 20): List<SearchResult> {
        val key = indexKey(query)
        if (key.isBlank()) return emptyList()
        val prefix = escapeLike(key) + "%"
        val results = mutableListOf<SearchResult>()
        connection.prepareStatement(
            "SELECT entity_type, entity_id, display_text, reading, gloss FROM search_index " +
                "WHERE search_key LIKE ? ESCAPE '\\' " +
                "ORDER BY (search_key = ?) DESC, length(search_key), frequency DESC LIMIT ?"
        ).use { stmt ->
            stmt.setString(1, prefix)
            stmt.setString(2, key)
            stmt.setInt(3, limit)
            stmt.executeQuery().use { rs ->
                while (rs.next()) results.add(rs.toResult(0.9f))
            }
        }
        return results.distinctBy { it.entityId.value }.take(limit)
    }

    private fun searchFts(
        ftsQuery: String,
        entityType: EntityType?,
        limit: Int
    ): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        if (entityType == null || entityType == EntityType.Kanji) {
            results += ftsHits("kanji_fts", ftsQuery, "kanji", limit)
        }
        if (entityType == null || entityType == EntityType.Vocabulary) {
            results += ftsHits("vocab_fts", ftsQuery, "vocab", limit)
        }
        return results
    }

    private fun ftsHits(table: String, ftsQuery: String, typeName: String, limit: Int): List<SearchResult> {
        val hits = mutableListOf<SearchResult>()
        connection.prepareStatement(
            "SELECT entity_id, bm25($table) AS rank FROM $table WHERE $table MATCH ? ORDER BY rank LIMIT ?"
        ).use { stmt ->
            stmt.setString(1, ftsQuery)
            stmt.setInt(2, limit)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val entityId = rs.getString(1)
                    val rank = rs.getFloat(2)
                    val display = lookupDisplay(typeName, entityId)
                    if (display != null) {
                        hits.add(
                            SearchResult(
                                entityType = if (typeName == "kanji") EntityType.Kanji else EntityType.Vocabulary,
                                entityId = EntityId(entityId),
                                displayText = display.first,
                                reading = display.second,
                                gloss = display.third,
                                // FTS5 bm25 is negative; more negative = better match.
                                // Map it so relevance increases with quality while
                                // staying below exact (1.0) and prefix (0.8) hits.
                                relevance = (0.6f - rank / 50f).coerceIn(0.3f, 0.75f),
                                kanaKey = indexKey(display.first)
                            )
                        )
                    }
                }
            }
        }
        return hits
    }

    private fun lookupDisplay(typeName: String, entityId: String): Triple<String, String?, String?>? {
        val table = if (typeName == "kanji") "kanji" else "vocab"
        return connection.prepareStatement(
            if (typeName == "kanji") {
                "SELECT literal, on_readings || ' ' || kun_readings, meanings FROM kanji WHERE id = ?"
            } else {
                "SELECT expression, reading, '' FROM vocab WHERE id = ?"
            }
        ).use { stmt ->
            stmt.setString(1, entityId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    Triple(
                        rs.getString(1),
                        rs.getString(2)?.takeIf { it.isNotBlank() },
                        rs.getString(3)?.takeIf { it.isNotBlank() }
                    )
                } else null
            }
        }
    }

    private fun java.sql.ResultSet.toResult(relevance: Float): SearchResult =
        SearchResult(
            entityType = if (getString(1) == "kanji") EntityType.Kanji else EntityType.Vocabulary,
            entityId = EntityId(getString(2)),
            displayText = getString(3),
            reading = getString(4)?.takeIf { it.isNotBlank() },
            gloss = getString(5)?.takeIf { it.isNotBlank() },
            relevance = relevance,
            kanaKey = indexKey(getString(3))
        )

    companion object {
        /** NFC + case-fold + punctuation-strip + katakana→hiragana. */
        fun indexKey(input: String): String =
            JapaneseNormalizer.katakanaToHiragana(JapaneseNormalizer.searchKey(input))

        private fun escapeLike(input: String): String =
            input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

        /** "to eat" → `to* eat*` (FTS5 prefix per token). */
        private fun buildFtsQuery(query: String): String =
            JapaneseNormalizer.searchKey(query)
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }
                .joinToString(" ") { "$it*" }
    }
}
