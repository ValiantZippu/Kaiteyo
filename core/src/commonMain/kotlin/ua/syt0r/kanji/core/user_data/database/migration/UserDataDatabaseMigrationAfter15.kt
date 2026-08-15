package ua.syt0r.kanji.core.user_data.database.migration

import app.cash.sqldelight.db.SqlDriver
import ua.syt0r.kanji.core.user_data.database.UserDataDatabaseContract

/**
 * Reconciliation migration v14 -> v15.
 *
 * Existing databases that were already at version 13 (or 14) before the
 * After13/After14 migrations were finalized never replay those versions:
 * SQLDelight only runs `AfterVersion` callbacks whose version falls inside
 * `oldVersion until newVersion`. The schema version has therefore been
 * bumped to 16 so every pre-existing database is migrated again — and this
 * migration replays BOTH the After13 column additions (e.g. `is_archived`
 * on `letter_deck`/`vocab_deck`, the `fsrs_card` columns) and the After14
 * statistics/examination table DDL.
 *
 * Every statement is idempotent (`addColumnIfNotExists` /
 * `CREATE TABLE IF NOT EXISTS` / `CREATE INDEX IF NOT EXISTS`), so replaying
 * is safe no matter which version the database actually holds. Fresh
 * installations never run this: `Schema.create` already contains the full
 * final schema.
 */
class UserDataDatabaseMigrationAfter15 : UserDataDatabaseContract.Migration {

    override val version: Long = 15

    override suspend fun execute(driver: SqlDriver) {
        UserDataDatabaseMigrationAfter13().execute(driver)
        UserDataDatabaseMigrationAfter14().execute(driver)
    }
}
