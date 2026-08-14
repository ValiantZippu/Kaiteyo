package ua.syt0r.kanji.desktop.engine.jdata.schema

// ============================================================
// CANONICAL SQLITE SCHEMA (DDL EMISSION)
// The platform's relational schema: normalized tables with foreign
// keys, indexes and an FTS5 gloss index. The DDL is emitted as
// plain SQL so any SQLite consumer (Android, desktop, third-party
// apps) can apply it with its own driver. This module produces
// strings only — it never touches a connection.
// ============================================================

object SchemaSql {

    const val SchemaVersion = 1
    const val DatabaseVersion = "1"

    private val tables = listOf(
        """
        CREATE TABLE IF NOT EXISTS source (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            version TEXT NOT NULL DEFAULT '',
            homepage TEXT NOT NULL DEFAULT '',
            license_name TEXT NOT NULL DEFAULT '',
            license_url TEXT NOT NULL DEFAULT '',
            retrieval_date TEXT NOT NULL DEFAULT '',
            format TEXT NOT NULL DEFAULT '',
            priority INTEGER NOT NULL DEFAULT 0,
            tags TEXT NOT NULL DEFAULT '[]'
        );
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS kanji (
            id TEXT PRIMARY KEY,
            character TEXT NOT NULL UNIQUE,
            stroke_count INTEGER,
            radical_id TEXT REFERENCES radical(id) ON DELETE SET NULL,
            jlpt INTEGER,
            grade INTEGER,
            frequency_rank INTEGER,
            meanings_json TEXT NOT NULL DEFAULT '[]',
            on_readings_json TEXT NOT NULL DEFAULT '[]',
            kun_readings_json TEXT NOT NULL DEFAULT '[]',
            sources_json TEXT NOT NULL DEFAULT '[]'
        );
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS kana (
            id TEXT PRIMARY KEY,
            character TEXT NOT NULL UNIQUE,
            script TEXT NOT NULL,
            reading TEXT NOT NULL,
            stroke_count INTEGER,
            sources_json TEXT NOT NULL DEFAULT '[]'
        );
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS radical (
            id TEXT PRIMARY KEY,
            character TEXT NOT NULL UNIQUE,
            meaning TEXT,
            stroke_count INTEGER,
            sources_json TEXT NOT NULL DEFAULT '[]'
        );
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS component (
            id TEXT PRIMARY KEY,
            character TEXT NOT NULL UNIQUE,
            kind TEXT NOT NULL,
            sources_json TEXT NOT NULL DEFAULT '[]'
        );
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS vocabulary (
            id TEXT PRIMARY KEY,
            expression TEXT NOT NULL,
            reading TEXT NOT NULL DEFAULT '',
            jlpt INTEGER,
            furigana_json TEXT NOT NULL DEFAULT '[]',
            frequencies_json TEXT NOT NULL DEFAULT '[]',
            sources_json TEXT NOT NULL DEFAULT '[]'
        );
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS vocab_reading (
            id TEXT PRIMARY KEY,
            vocab_id TEXT NOT NULL REFERENCES vocabulary(id) ON DELETE CASCADE,
            kana TEXT NOT NULL,
            restrictions_json TEXT NOT NULL DEFAULT '[]',
            pitch_json TEXT NOT NULL DEFAULT '[]'
        );
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS sense (
            id TEXT PRIMARY KEY,
            vocab_id TEXT NOT NULL REFERENCES vocabulary(id) ON DELETE CASCADE,
            sense_index INTEGER NOT NULL,
            language TEXT NOT NULL DEFAULT 'en',
            glosses_json TEXT NOT NULL DEFAULT '[]',
            part_of_speech_json TEXT NOT NULL DEFAULT '[]',
            field_json TEXT NOT NULL DEFAULT '[]',
            misc_json TEXT NOT NULL DEFAULT '[]',
            restrictions_json TEXT NOT NULL DEFAULT '[]',
            sources_json TEXT NOT NULL DEFAULT '[]'
        );
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS stroke_set (
            character TEXT PRIMARY KEY,
            stroke_count INTEGER NOT NULL,
            source_json TEXT
        );
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS stroke (
            character TEXT NOT NULL REFERENCES stroke_set(character) ON DELETE CASCADE,
            stroke_index INTEGER NOT NULL,
            path TEXT,
            min_x REAL, min_y REAL, max_x REAL, max_y REAL,
            PRIMARY KEY (character, stroke_index)
        );
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS relation (
            id TEXT PRIMARY KEY,
            from_type TEXT NOT NULL,
            from_id TEXT NOT NULL,
            to_type TEXT NOT NULL,
            to_id TEXT NOT NULL,
            kind TEXT NOT NULL
        );
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS jlpt (
            id TEXT PRIMARY KEY,
            level INTEGER NOT NULL UNIQUE
        );
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS extension_slot (
            namespace TEXT NOT NULL,
            owner_id TEXT NOT NULL,
            kind TEXT NOT NULL,
            payload_json TEXT NOT NULL,
            PRIMARY KEY (namespace, owner_id, kind)
        );
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS provenance (
            entity_type TEXT NOT NULL,
            entity_id TEXT NOT NULL,
            source_id TEXT NOT NULL REFERENCES source(id),
            record_key TEXT NOT NULL DEFAULT '',
            retrieved_at TEXT NOT NULL DEFAULT '',
            PRIMARY KEY (entity_type, entity_id, source_id)
        );
        """.trimIndent()
    )

    private val indexes = listOf(
        "CREATE INDEX IF NOT EXISTS idx_kanji_jlpt ON kanji(jlpt);",
        "CREATE INDEX IF NOT EXISTS idx_kanji_freq ON kanji(frequency_rank);",
        "CREATE INDEX IF NOT EXISTS idx_vocab_reading ON vocabulary(reading);",
        "CREATE INDEX IF NOT EXISTS idx_vocab_jlpt ON vocabulary(jlpt);",
        "CREATE INDEX IF NOT EXISTS idx_reading_vocab ON vocab_reading(vocab_id);",
        "CREATE INDEX IF NOT EXISTS idx_sense_vocab ON sense(vocab_id);",
        "CREATE INDEX IF NOT EXISTS idx_relation_from ON relation(from_id);",
        "CREATE INDEX IF NOT EXISTS idx_relation_to ON relation(to_id);",
        "CREATE INDEX IF NOT EXISTS idx_provenance_entity ON provenance(entity_type, entity_id);"
    )

    /** FTS5 gloss index over vocabulary senses (meaning search must not scan). */
    private val fts = """
        CREATE VIRTUAL TABLE IF NOT EXISTS vocab_fts USING fts5(
            vocab_id UNINDEXED,
            expression,
            reading,
            glosses,
            content=''
        );
        CREATE INDEX IF NOT EXISTS idx_vocab_fts ON vocab_fts(vocab_id);
    """.trimIndent()

    /** All DDL, in dependency order. */
    fun createAll(): String = buildString {
        appendLine("PRAGMA foreign_keys = ON;")
        appendLine("PRAGMA journal_mode = WAL;")
        tables.forEach { appendLine(it); appendLine() }
        indexes.forEach { appendLine(it); appendLine() }
        appendLine(fts)
    }

    /**
     * Migration map: schema version → list of ALTER/CREATE statements.
     * v2 adds a canonical pitch table (moved out of JSON) and a kanji
     * radical count column. Append here as the schema evolves; never edit
     * an already-shipped version entry.
     */
    val Migrations: Map<Int, List<String>> = mapOf(
        2 to listOf(
            "CREATE TABLE IF NOT EXISTS pitch (id TEXT PRIMARY KEY, reading_id TEXT NOT NULL REFERENCES vocab_reading(id) ON DELETE CASCADE, position INTEGER NOT NULL, downstep INTEGER);",
            "ALTER TABLE kanji ADD COLUMN radical_count INTEGER;"
        )
    )

    fun migrationStatements(fromVersion: Int, toVersion: Int): List<String> {
        val statements = mutableListOf<String>()
        for (version in (fromVersion + 1)..toVersion) {
            Migrations[version]?.let { statements += it }
        }
        return statements
    }
}
