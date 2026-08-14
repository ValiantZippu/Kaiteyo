package io.kaiteyo.kjd.db

/**
 * The canonical SQLite schema for generated KJD databases.
 *
 * Design goals:
 *   - portable & deterministic
 *   - indexed for read efficiency (kanji lookup, prefix search, reading
 *     search, meaning search)
 *   - versioned (schema_version table) so migrations are possible
 *   - provenance columns on every fact table
 *   - separation from user learning data (never stored in this DB)
 */
object Schema {

    /**
     * Current schema version. Bumped on breaking schema changes; the
     * [SchemaMigrator] upgrades older databases in place (additive only —
     * user learning data is never stored here, and migrations never destroy
     * language records).
     */
    const val SCHEMA_VERSION = 2

    val CREATE_TABLES: List<String> = listOf(
        """
        CREATE TABLE IF NOT EXISTS meta (
            key TEXT PRIMARY KEY,
            value TEXT NOT NULL
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS source (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            homepage TEXT NOT NULL,
            license_id TEXT NOT NULL,
            license_name TEXT NOT NULL,
            license_url TEXT NOT NULL DEFAULT '',
            allows_redistribution INTEGER NOT NULL DEFAULT 0,
            attribution_required INTEGER NOT NULL DEFAULT 0,
            version TEXT NOT NULL,
            retrieved_at TEXT NOT NULL,
            attribution TEXT NOT NULL,
            redistribution_notes TEXT NOT NULL DEFAULT '',
            modification_notes TEXT NOT NULL DEFAULT '',
            source_url TEXT NOT NULL DEFAULT ''
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS kanji (
            id TEXT PRIMARY KEY,
            literal TEXT NOT NULL UNIQUE,
            codepoint INTEGER NOT NULL,
            normalized TEXT NOT NULL,
            stroke_count INTEGER,
            grade INTEGER,
            radical TEXT,
            on_readings TEXT NOT NULL DEFAULT '',
            kun_readings TEXT NOT NULL DEFAULT '',
            meanings TEXT NOT NULL DEFAULT '',
            jlpt TEXT NOT NULL DEFAULT '',
            frequency TEXT NOT NULL DEFAULT ''
        )
        """.trimIndent(),
        """
        CREATE INDEX IF NOT EXISTS idx_kanji_literal ON kanji (literal)
        """.trimIndent(),
        """
        CREATE INDEX IF NOT EXISTS idx_kanji_reading ON kanji (on_readings)
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS kanji_stroke (
            id TEXT PRIMARY KEY,
            kanji_id TEXT NOT NULL REFERENCES kanji(id) ON DELETE CASCADE,
            stroke_index INTEGER NOT NULL,
            path TEXT NOT NULL,
            path_type TEXT NOT NULL DEFAULT 'KanjiVg',
            direction TEXT,
            min_x REAL,
            min_y REAL,
            max_x REAL,
            max_y REAL
        )
        """.trimIndent(),
        """
        CREATE INDEX IF NOT EXISTS idx_stroke_kanji ON kanji_stroke (kanji_id)
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS kana (
            id TEXT PRIMARY KEY,
            literal TEXT NOT NULL UNIQUE,
            codepoint INTEGER NOT NULL,
            syllabary TEXT NOT NULL,
            stroke_count INTEGER,
            romaji TEXT
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS radical (
            id TEXT PRIMARY KEY,
            number INTEGER,
            character TEXT,
            name TEXT,
            meanings TEXT NOT NULL DEFAULT '',
            stroke_count INTEGER,
            unicode_codepoint INTEGER
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS vocab (
            id TEXT PRIMARY KEY,
            expression TEXT NOT NULL,
            reading TEXT NOT NULL DEFAULT '',
            readings_json TEXT NOT NULL DEFAULT '[]',
            kanji_ids TEXT NOT NULL DEFAULT '[]',
            furigana_json TEXT NOT NULL DEFAULT '[]',
            jlpt TEXT NOT NULL DEFAULT '',
            frequency TEXT NOT NULL DEFAULT '',
            pos TEXT NOT NULL DEFAULT '[]'
        )
        """.trimIndent(),
        """
        CREATE INDEX IF NOT EXISTS idx_vocab_expression ON vocab (expression)
        """.trimIndent(),
        """
        CREATE INDEX IF NOT EXISTS idx_vocab_reading ON vocab (reading)
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS sense (
            id TEXT PRIMARY KEY,
            vocab_id TEXT NOT NULL REFERENCES vocab(id) ON DELETE CASCADE,
            sense_index INTEGER NOT NULL,
            glosses_json TEXT NOT NULL DEFAULT '[]',
            pos_json TEXT NOT NULL DEFAULT '[]',
            fields_json TEXT NOT NULL DEFAULT '[]',
            misc_json TEXT NOT NULL DEFAULT '[]',
            restrictions_json TEXT NOT NULL DEFAULT '[]'
        )
        """.trimIndent(),
        """
        CREATE INDEX IF NOT EXISTS idx_sense_vocab ON sense (vocab_id)
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS vocab_furigana (
            vocab_id TEXT PRIMARY KEY REFERENCES vocab(id) ON DELETE CASCADE,
            segments_json TEXT NOT NULL
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS relationship (
            id TEXT PRIMARY KEY,
            from_id TEXT NOT NULL,
            to_id TEXT NOT NULL,
            relation_type TEXT NOT NULL,
            source TEXT NOT NULL DEFAULT ''
        )
        """.trimIndent(),
        """
        CREATE INDEX IF NOT EXISTS idx_relationship_from ON relationship (from_id)
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS component (
            id TEXT PRIMARY KEY,
            kanji_id TEXT NOT NULL REFERENCES kanji(id) ON DELETE CASCADE,
            character TEXT NOT NULL,
            role TEXT NOT NULL DEFAULT 'graphical',
            stroke_count INTEGER
        )
        """.trimIndent(),
        """
        CREATE INDEX IF NOT EXISTS idx_component_kanji ON component (kanji_id)
        """.trimIndent(),
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
        """.trimIndent(),
        """
        CREATE INDEX IF NOT EXISTS idx_search_index_key ON search_index (search_key)
        """.trimIndent(),
        """
        CREATE INDEX IF NOT EXISTS idx_search_index_entity ON search_index (entity_type, entity_id)
        """.trimIndent(),
        """
        CREATE VIRTUAL TABLE IF NOT EXISTS kanji_fts USING fts5(
            entity_id UNINDEXED, literal, on_readings, kun_readings, meanings,
            tokenize = 'unicode61 remove_diacritics 2'
        )
        """.trimIndent(),
        """
        CREATE VIRTUAL TABLE IF NOT EXISTS vocab_fts USING fts5(
            entity_id UNINDEXED, expression, reading, glosses,
            tokenize = 'unicode61 remove_diacritics 2'
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS example_sentence (
            id TEXT PRIMARY KEY,
            text TEXT NOT NULL,
            translation TEXT,
            language TEXT NOT NULL DEFAULT 'ja',
            source TEXT NOT NULL DEFAULT ''
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS source_record (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            source_id TEXT NOT NULL,
            entity_id TEXT NOT NULL,
            source_record_id TEXT,
            transformation TEXT NOT NULL DEFAULT 'parsed',
            is_canonical INTEGER NOT NULL DEFAULT 0,
            UNIQUE (source_id, entity_id, source_record_id)
        )
        """.trimIndent(),
        """
        CREATE INDEX IF NOT EXISTS idx_source_record_entity ON source_record (entity_id)
        """.trimIndent()
    )

    val DROP_ALL: String = """
        DROP TABLE IF EXISTS source_record;
        DROP TABLE IF EXISTS example_sentence;
        DROP TABLE IF EXISTS relationship;
        DROP TABLE IF EXISTS vocab_furigana;
        DROP TABLE IF EXISTS sense;
        DROP TABLE IF EXISTS vocab;
        DROP TABLE IF EXISTS radical;
        DROP TABLE IF EXISTS kana;
        DROP TABLE IF EXISTS kanji_stroke;
        DROP TABLE IF EXISTS component;
        DROP TABLE IF EXISTS search_index;
        DROP TABLE IF EXISTS kanji_fts;
        DROP TABLE IF EXISTS vocab_fts;
        DROP TABLE IF EXISTS kanji;
        DROP TABLE IF EXISTS source;
        DROP TABLE IF EXISTS meta;
    """.trimIndent()

    /** Metadata keys written on generation. */
    object Meta {
        const val SCHEMA_VERSION_KEY = "schema_version"
        const val GENERATOR_VERSION = "generator_version"
        const val GENERATED_AT = "generated_at"
        const val SOURCES_JSON = "sources_json"
        /** Deterministic content-state hash used by the patch system. */
        const val STATE_FINGERPRINT = "state_fingerprint"
    }
}
