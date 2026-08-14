package io.kaiteyo.kjd.db

import java.sql.Connection
import java.sql.Statement

/**
 * Incremental schema migrations for KJD databases.
 *
 * Generated databases record their version in `PRAGMA user_version` (and in
 * the `meta.schema_version` row). [SchemaMigrator] upgrades an older database
 * in place by applying only the missing steps. All steps are:
 *
 *   - additive (never DROP or rewrite existing tables),
 *   - idempotent (safe to run on a database that already has the change),
 *   - wrapped in a transaction (a failed migration leaves the database
 *     untouched and the user's data intact).
 *
 * The update workflow is: `backup → migrate → validate`. Language records,
 * relationships and provenance survive every migration.
 */
object SchemaMigrator {

    /**
     * Migration steps indexed by the version they upgrade FROM.
     * `migrations[n]` upgrades version `n` → `n + 1`.
     */
    private val migrations: List<(Connection, Statement) -> Unit> = listOf(
        // 0 → 1: no-op guard. A database created before versioning exists with
        // the base tables already present; nothing structural was added in v1.
        { _, _ -> },

        // 1 → 2: v2 adds the component table, the relational search index and
        // the FTS5 meaning/gloss indexes. Existing data is backfilled.
        ::migrate1To2
    )

    val CURRENT_VERSION: Int = Schema.SCHEMA_VERSION

    /** Applies every missing migration up to [Schema.SCHEMA_VERSION]. */
    fun migrate(connection: Connection, logger: (String) -> Unit = {}) {
        val from = userVersion(connection)
        if (from >= CURRENT_VERSION) return

        connection.autoCommit = false
        try {
            var version = from
            connection.createStatement().use { statement ->
                for (next in (from + 1)..CURRENT_VERSION) {
                    val step = migrations[next - 1]
                    step(connection, statement)
                    statement.execute("PRAGMA user_version = $next")
                    version = next
                    logger("migrated schema $from -> $next")
                }
            }
            // Keep the human-readable meta row in sync.
            upsertMetaVersion(connection, CURRENT_VERSION)
            connection.commit()
        } catch (t: Throwable) {
            connection.rollback()
            throw IllegalStateException(
                "Schema migration ${userVersion(connection)} -> $CURRENT_VERSION failed; " +
                    "database was left unchanged. ${t.message}",
                t
            )
        }
    }

    private fun migrate1To2(connection: Connection, statement: Statement) {
        // Component table.
        statement.execute(
            """
            CREATE TABLE IF NOT EXISTS component (
                id TEXT PRIMARY KEY,
                kanji_id TEXT NOT NULL REFERENCES kanji(id) ON DELETE CASCADE,
                character TEXT NOT NULL,
                role TEXT NOT NULL DEFAULT 'graphical',
                stroke_count INTEGER
            )
            """.trimIndent()
        )
        statement.execute("CREATE INDEX IF NOT EXISTS idx_component_kanji ON component (kanji_id)")

        // Relational search index (normalized keys — prefix & exact lookups).
        statement.execute(
            """
            CREATE TABLE IF NOT EXISTS search_index (
                entity_type TEXT NOT NULL,
                entity_id TEXT NOT NULL,
                search_key TEXT NOT NULL,
                display_text TEXT NOT NULL,
                reading TEXT NOT NULL DEFAULT '',
                gloss TEXT NOT NULL DEFAULT '',
                frequency INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (entity_type, entity_id, search_key)
            )
            """.trimIndent()
        )
        statement.execute("CREATE INDEX IF NOT EXISTS idx_search_index_key ON search_index (search_key)")
        statement.execute("CREATE INDEX IF NOT EXISTS idx_search_index_entity ON search_index (entity_type, entity_id)")

        // FTS5 indexes for meaning / gloss full-text search.
        statement.execute(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS kanji_fts USING fts5(
                entity_id UNINDEXED, literal, on_readings, kun_readings, meanings,
                tokenize = 'unicode61 remove_diacritics 2'
            )
            """.trimIndent()
        )
        statement.execute(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS vocab_fts USING fts5(
                entity_id UNINDEXED, expression, reading, glosses,
                tokenize = 'unicode61 remove_diacritics 2'
            )
            """.trimIndent()
        )

        // Derived tables are rebuilt from content — same code path the
        // patcher uses, so migration and patch converge to identical state.
        IndexRebuilder.rebuild(connection)
    }

    private fun userVersion(connection: Connection): Int =
        connection.createStatement().use { stmt ->
            stmt.executeQuery("PRAGMA user_version").use { rs ->
                if (rs.next()) rs.getInt(1) else 0
            }
        }

    private fun upsertMetaVersion(connection: Connection, version: Int) {
        connection.prepareStatement(
            "INSERT INTO meta (key, value) VALUES (?, ?) " +
                "ON CONFLICT(key) DO UPDATE SET value = excluded.value"
        ).use { stmt ->
            stmt.setString(1, Schema.Meta.SCHEMA_VERSION_KEY)
            stmt.setString(2, version.toString())
            stmt.executeUpdate()
        }
    }
}
