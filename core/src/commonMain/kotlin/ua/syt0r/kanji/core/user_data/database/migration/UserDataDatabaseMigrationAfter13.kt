package ua.syt0r.kanji.core.user_data.database.migration

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import ua.syt0r.kanji.core.user_data.database.UserDataDatabaseContract

/**
 * Migration v12 -> v13: Add new columns to existing tables (fsrs_card, letter_deck, vocab_deck)
 * and create new tables for tags, flags, notes, history, shortcuts, backups, filtered decks, plugins.
 *
 * New tables are created via UserData_enhancements.sq (SQLDelight handles CREATE TABLE IF NOT EXISTS).
 * This migration handles the ALTER TABLE statements that SQLDelight cannot parse.
 */
class UserDataDatabaseMigrationAfter13 : UserDataDatabaseContract.Migration {

    override val version: Long = 13

    override suspend fun execute(driver: SqlDriver) {
        // --- Add new columns to fsrs_card ---
        addColumnIfNotExists(driver, "fsrs_card", "card_status", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfNotExists(driver, "fsrs_card", "created_at", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfNotExists(driver, "fsrs_card", "modified_at", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfNotExists(driver, "fsrs_card", "deck_id", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfNotExists(driver, "fsrs_card", "ease", "REAL NOT NULL DEFAULT 2.5")
        addColumnIfNotExists(driver, "fsrs_card", "accuracy", "REAL NOT NULL DEFAULT 0.0")
        addColumnIfNotExists(driver, "fsrs_card", "total_time_studied", "INTEGER NOT NULL DEFAULT 0")

        // --- Add new columns to letter_deck ---
        addColumnIfNotExists(driver, "letter_deck", "is_favorite", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfNotExists(driver, "letter_deck", "is_archived", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfNotExists(driver, "letter_deck", "parent_deck_id", "INTEGER")
        addColumnIfNotExists(driver, "letter_deck", "description", "TEXT NOT NULL DEFAULT ''")
        addColumnIfNotExists(driver, "letter_deck", "color", "TEXT NOT NULL DEFAULT '#808080'")

        // --- Add new columns to vocab_deck ---
        addColumnIfNotExists(driver, "vocab_deck", "is_favorite", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfNotExists(driver, "vocab_deck", "is_archived", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfNotExists(driver, "vocab_deck", "parent_deck_id", "INTEGER")
        addColumnIfNotExists(driver, "vocab_deck", "description", "TEXT NOT NULL DEFAULT ''")
        addColumnIfNotExists(driver, "vocab_deck", "color", "TEXT NOT NULL DEFAULT '#808080'")

        // --- Add index for deck_id on fsrs_card for faster lookups ---
        driver.execute(
            identifier = null,
            sql = "CREATE INDEX IF NOT EXISTS fsrs_card_deck_idx ON fsrs_card(deck_id);",
            parameters = 0
        )

        // --- Add index for card_status on fsrs_card ---
        driver.execute(
            identifier = null,
            sql = "CREATE INDEX IF NOT EXISTS fsrs_card_status_idx ON fsrs_card(card_status);",
            parameters = 0
        )
    }

    /**
     * Safely adds a column to a table if it doesn't already exist.
     * SQLite ignores IF NOT EXISTS for ALTER TABLE ADD COLUMN,
     * so we check the table info first.
     */
    private fun addColumnIfNotExists(driver: SqlDriver, tableName: String, columnName: String, columnDef: String) {
        val columns = driver.executeQuery(
            identifier = null,
            sql = "PRAGMA table_info($tableName);",
            mapper = {
                val list = mutableListOf<String>()
                while (it.next().value) {
                    list.add(it.getString(1)!!) // column name is at index 1
                }
                QueryResult.Value(list)
            },
            parameters = 0
        ).value

        if (columnName !in columns) {
            driver.execute(
                identifier = null,
                sql = "ALTER TABLE $tableName ADD COLUMN $columnName $columnDef;",
                parameters = 0
            )
        }
    }
}
