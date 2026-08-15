package ua.syt0r.kanji.core.user_data.database.migration

import app.cash.sqldelight.db.SqlDriver
import ua.syt0r.kanji.core.user_data.database.UserDataDatabaseContract

/**
 * Migration that adds the statistics & examination tables
 * (study sessions, writing attempts, exams, exam questions,
 * learning mistakes and precomputed daily rollups).
 *
 * The same DDL lives in `UserData_statistics.sq` (which SQLDelight
 * uses to build the schema for fresh installs); this migration is what
 * brings existing databases up to date. Every statement is idempotent
 * (IF NOT EXISTS / IF NOT NULL guarded) so it is safe to run once or
 * repeatedly regardless of the exact database version it encounters.
 */
class UserDataDatabaseMigrationAfter14 : UserDataDatabaseContract.Migration {

    override val version: Long = 14

    override suspend fun execute(driver: SqlDriver) {
        executeSql(driver, STATISTICS_TABLE_DDL)
    }

    private fun executeSql(driver: SqlDriver, statements: List<String>) {
        statements.forEach { sql ->
            driver.execute(
                identifier = null,
                sql = sql,
                parameters = 0
            )
        }
    }

    companion object {

        /** Kept in sync with UserData_statistics.sq. */
        val STATISTICS_TABLE_DDL: List<String> = listOf(
            """
            CREATE TABLE IF NOT EXISTS study_session (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                start_time INTEGER NOT NULL,
                end_time INTEGER,
                duration_ms INTEGER NOT NULL DEFAULT 0,
                practice_type INTEGER NOT NULL DEFAULT -1,
                mode TEXT NOT NULL DEFAULT '',
                deck_id INTEGER NOT NULL DEFAULT 0,
                deck_name TEXT NOT NULL DEFAULT '',
                items_studied INTEGER NOT NULL DEFAULT 0,
                new_items INTEGER NOT NULL DEFAULT 0,
                review_items INTEGER NOT NULL DEFAULT 0,
                correct INTEGER NOT NULL DEFAULT 0,
                incorrect INTEGER NOT NULL DEFAULT 0,
                is_complete INTEGER NOT NULL DEFAULT 0
            );
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS study_session_start_idx ON study_session(start_time);",
            """
            CREATE TABLE IF NOT EXISTS writing_attempt (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                character TEXT NOT NULL,
                practice_type INTEGER NOT NULL DEFAULT 0,
                timestamp INTEGER NOT NULL,
                deck_id INTEGER NOT NULL DEFAULT 0,
                session_id INTEGER,
                stroke_count INTEGER NOT NULL DEFAULT 0,
                mistakes INTEGER NOT NULL DEFAULT 0,
                wrong_order INTEGER NOT NULL DEFAULT 0,
                almost INTEGER NOT NULL DEFAULT 0,
                accuracy REAL
            );
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS writing_attempt_ts_idx ON writing_attempt(timestamp);",
            "CREATE INDEX IF NOT EXISTS writing_attempt_char_idx ON writing_attempt(character);",
            """
            CREATE TABLE IF NOT EXISTS exam (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                exam_type TEXT NOT NULL,
                scope_json TEXT NOT NULL DEFAULT '',
                question_count INTEGER NOT NULL DEFAULT 0,
                time_limit_ms INTEGER,
                seed INTEGER NOT NULL DEFAULT 0,
                started_at INTEGER NOT NULL,
                finished_at INTEGER,
                status INTEGER NOT NULL DEFAULT 0,
                score INTEGER NOT NULL DEFAULT 0,
                accuracy REAL NOT NULL DEFAULT 0,
                total_time_ms INTEGER NOT NULL DEFAULT 0
            );
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS exam_started_idx ON exam(started_at);",
            """
            CREATE TABLE IF NOT EXISTS exam_question (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exam_id INTEGER NOT NULL,
                question_index INTEGER NOT NULL,
                question_type TEXT NOT NULL,
                prompt TEXT NOT NULL,
                answer TEXT NOT NULL,
                options_json TEXT,
                user_answer TEXT,
                is_correct INTEGER,
                time_ms INTEGER NOT NULL DEFAULT 0,
                entity_key TEXT NOT NULL DEFAULT '',
                skill TEXT NOT NULL DEFAULT '',
                jlpt_level INTEGER,
                mistake_category TEXT,
                FOREIGN KEY(exam_id) REFERENCES exam(id) ON DELETE CASCADE
            );
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS exam_question_exam_idx ON exam_question(exam_id);",
            """
            CREATE TABLE IF NOT EXISTS learning_mistake (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp INTEGER NOT NULL,
                entity_key TEXT NOT NULL DEFAULT '',
                content_type TEXT NOT NULL DEFAULT '',
                mode TEXT NOT NULL DEFAULT '',
                question_type TEXT NOT NULL DEFAULT '',
                expected TEXT NOT NULL DEFAULT '',
                actual TEXT NOT NULL DEFAULT '',
                category TEXT NOT NULL DEFAULT 'unknown',
                severity INTEGER NOT NULL DEFAULT 1,
                session_id INTEGER,
                exam_id INTEGER,
                deck_id INTEGER NOT NULL DEFAULT 0
            );
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS learning_mistake_ts_idx ON learning_mistake(timestamp);",
            "CREATE INDEX IF NOT EXISTS learning_mistake_entity_idx ON learning_mistake(entity_key);",
            """
            CREATE TABLE IF NOT EXISTS daily_stats (
                date TEXT NOT NULL PRIMARY KEY,
                reviews INTEGER NOT NULL DEFAULT 0,
                new_cards INTEGER NOT NULL DEFAULT 0,
                review_cards INTEGER NOT NULL DEFAULT 0,
                correct INTEGER NOT NULL DEFAULT 0,
                incorrect INTEGER NOT NULL DEFAULT 0,
                lapses INTEGER NOT NULL DEFAULT 0,
                study_time_ms INTEGER NOT NULL DEFAULT 0,
                writing_attempts INTEGER NOT NULL DEFAULT 0,
                writing_correct INTEGER NOT NULL DEFAULT 0,
                exams_taken INTEGER NOT NULL DEFAULT 0,
                exam_score_sum INTEGER NOT NULL DEFAULT 0,
                exam_score_count INTEGER NOT NULL DEFAULT 0,
                sessions INTEGER NOT NULL DEFAULT 0
            );
            """.trimIndent()
        )
    }
}
