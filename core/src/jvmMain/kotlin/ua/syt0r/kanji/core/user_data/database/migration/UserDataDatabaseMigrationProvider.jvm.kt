package ua.syt0r.kanji.core.user_data.database.migration

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.runBlocking

/**
 * Runs a single migration's statements.
 *
 * Every migration in the provider is idempotent by construction
 * (`addColumnIfNotExists`, `CREATE TABLE/INDEX IF NOT EXISTS`,
 * `INSERT OR IGNORE`), so statements are executed in autocommit mode
 * instead of inside a hand-rolled BEGIN/COMMIT. The previous manual
 * `newTransaction()` + `COMMIT` combination was fragile: `newTransaction()`
 * only opens a transaction when none is already active, so on connections
 * that reused an existing transaction state the trailing `COMMIT` threw
 * "cannot commit - no transaction is active". Because the statements are
 * idempotent, a partially applied migration is harmless: the next launch
 * replays it, and the version-15 reconciliation migration heals any gap.
 */
actual fun SqlDriver.migrationScope(block: suspend () -> Unit) {
    runBlocking { block() }
}
