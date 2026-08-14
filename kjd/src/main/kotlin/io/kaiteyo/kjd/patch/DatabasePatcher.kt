package io.kaiteyo.kjd.patch

import io.kaiteyo.kjd.db.IndexRebuilder
import io.kaiteyo.kjd.db.openKjdDatabase
import java.io.File
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Types
import java.time.Instant
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * Applies an incremental [DatabasePatch] to an existing KJD database.
 *
 * Non-destructive guarantees:
 *
 *   - **Verification first.** The target's schema version and content
 *     fingerprint must match the patch's `from*` values; a mismatch aborts
 *     before anything is written (pass [force] to override the fingerprint
 *     check only — never the schema check).
 *   - **One transaction.** Inserts, updates and deletes run inside a single
 *     transaction. Any failure rolls everything back — the target database
 *     is left byte-for-byte unchanged.
 *   - **Derived indexes rebuilt.** `search_index` + FTS5 tables are rebuilt
 *     from content after the apply.
 *   - **Post-apply verification.** The result must hash to the patch's
 *     `toFingerprint` before the transaction commits.
 *   - **Idempotent.** Applying to a database already at the target state is
 *     a no-op ([PatchResult.AlreadyApplied]).
 *   - **Optional backup.** [backupDir] copies the target before applying.
 *
 * KJD databases contain only immutable language data — user learning data
 * lives in a separate database and is never touched by patching.
 */
class DatabasePatcher {

    fun apply(
        target: File,
        patch: DatabasePatch,
        force: Boolean = false,
        backupDir: File? = null
    ): PatchResult {
        require(target.isFile) { "Target database not found: $target" }
        require(patch.patchFormatVersion == PATCH_FORMAT_VERSION) {
            "Unsupported patch format v${patch.patchFormatVersion} " +
                "(this patcher understands v$PATCH_FORMAT_VERSION only)."
        }

        return openKjdDatabase(target).use { connection ->
            connection.autoCommit = false
            try {
                val currentSchema = userVersion(connection)
                require(currentSchema == patch.fromSchemaVersion) {
                    "Target schema version $currentSchema does not match the patch's from-schema " +
                        "${patch.fromSchemaVersion}. Migrate the database first."
                }

                val currentFingerprint = DatabaseFingerprint.compute(connection, currentSchema)
                if (currentFingerprint == patch.toFingerprint) {
                    connection.rollback()
                    return@use PatchResult.AlreadyApplied
                }
                if (!force && currentFingerprint != patch.fromFingerprint) {
                    connection.rollback()
                    throw IllegalStateException(
                        "Target fingerprint does not match the patch's source release; " +
                            "applying could corrupt the database. Nothing was changed.\n" +
                            "patch expects: ${patch.fromFingerprint}\n" +
                            "target has:   $currentFingerprint\n" +
                            "Use --force only if you have verified the mismatch is benign."
                    )
                }

                // All safety checks passed — snapshot the target before mutating.
                backupDir?.let { dir ->
                    dir.mkdirs()
                    val backup = File(dir, "kjd-backup-${System.currentTimeMillis()}.db")
                    target.copyTo(backup, overwrite = true)
                }

                applyDeletes(connection, patch)
                applyUpdates(connection, patch)
                applyInserts(connection, patch)

                // Derived indexes are pure functions of content — rebuild.
                IndexRebuilder.rebuild(connection)

                // Carry over metadata, then record the new state.
                patch.metaUpdates.forEach { (key, value) -> upsertMeta(connection, key, value) }
                upsertMeta(connection, STATE_FINGERPRINT_KEY, patch.toFingerprint)
                upsertMeta(connection, "generated_at", Instant.now().toString())
                upsertMeta(connection, "patched_from", patch.fromFingerprint)

                val verified = DatabaseFingerprint.compute(connection, currentSchema)
                if (verified != patch.toFingerprint) {
                    connection.rollback()
                    throw IllegalStateException(
                        "Post-apply verification failed: resulting fingerprint $verified " +
                            "!= expected ${patch.toFingerprint}. Nothing was committed."
                    )
                }

                connection.commit()
                PatchResult.Applied(patch.summary, verified)
            } catch (t: Throwable) {
                connection.rollback()
                throw t
            }
        }
    }

    // ---------------------------------------------------------------
    // Apply phases. Deletes run child-first (reverse registry order),
    // inserts parent-first (registry order), so foreign keys never dangle.
    // ---------------------------------------------------------------

    private fun applyDeletes(connection: Connection, patch: DatabasePatch) {
        for (spec in PatchTables.CONTENT_TABLES.asReversed()) {
            val table = patch.tables[spec.name] ?: continue
            if (table.deletes.isEmpty()) continue
            val where = spec.keyColumns.joinToString(" AND ") { "$it = ?" }
            connection.prepareStatement("DELETE FROM ${spec.name} WHERE $where").use { stmt ->
                table.deletes.forEach { keyValues ->
                    keyValues.forEachIndexed { index, value -> bind(stmt, index + 1, value) }
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        }
    }

    private fun applyUpdates(connection: Connection, patch: DatabasePatch) {
        for (spec in PatchTables.CONTENT_TABLES) {
            val table = patch.tables[spec.name] ?: continue
            if (table.updates.isEmpty()) continue
            val row = table.updates.first()
            val setColumns = row.keys.filter { it !in spec.keyColumns }
            if (setColumns.isEmpty()) continue
            val setClause = setColumns.joinToString(", ") { "$it = ?" }
            val where = spec.keyColumns.joinToString(" AND ") { "$it = ?" }
            connection.prepareStatement("UPDATE ${spec.name} SET $setClause WHERE $where").use { stmt ->
                table.updates.forEach { updateRow ->
                    var index = 1
                    setColumns.forEach { column -> bind(stmt, index++, updateRow[column] ?: JsonNull) }
                    spec.keyColumns.forEach { column -> bind(stmt, index++, updateRow[column] ?: JsonNull) }
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        }
    }

    private fun applyInserts(connection: Connection, patch: DatabasePatch) {
        for (spec in PatchTables.CONTENT_TABLES) {
            val table = patch.tables[spec.name] ?: continue
            if (table.inserts.isEmpty()) continue
            val columns = table.inserts.first().keys.toList()
            val placeholders = columns.joinToString(", ") { "?" }
            connection.prepareStatement(
                "INSERT OR REPLACE INTO ${spec.name} (${columns.joinToString(", ")}) VALUES ($placeholders)"
            ).use { stmt ->
                table.inserts.forEach { insertRow ->
                    columns.forEachIndexed { index, column -> bind(stmt, index + 1, insertRow[column] ?: JsonNull) }
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        }
    }

    private fun bind(stmt: PreparedStatement, index: Int, element: JsonElement) {
        when (element) {
            is JsonNull -> stmt.setNull(index, Types.NULL)
            is JsonPrimitive -> {
                val content = element.content
                when {
                    element.isString -> stmt.setString(index, content)
                    content.toLongOrNull() != null -> stmt.setLong(index, content.toLong())
                    content.toDoubleOrNull() != null -> stmt.setDouble(index, content.toDouble())
                    content.toBooleanStrictOrNull() != null -> stmt.setBoolean(index, content.toBooleanStrict())
                    else -> stmt.setString(index, content)
                }
            }
            else -> throw IllegalArgumentException("Unexpected JSON element in patch row: $element")
        }
    }

    private fun upsertMeta(connection: Connection, key: String, value: String) {
        connection.prepareStatement(
            "INSERT INTO meta (key, value) VALUES (?, ?) " +
                "ON CONFLICT(key) DO UPDATE SET value = excluded.value"
        ).use { stmt ->
            stmt.setString(1, key)
            stmt.setString(2, value)
            stmt.executeUpdate()
        }
    }

    private companion object {
        const val STATE_FINGERPRINT_KEY = "state_fingerprint"
        const val PATCH_FORMAT_VERSION = 1
    }
}
