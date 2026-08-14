package io.kaiteyo.kjd.patch

import io.kaiteyo.kjd.db.Schema
import io.kaiteyo.kjd.db.openKjdDatabase
import java.io.File
import java.security.MessageDigest
import java.sql.Connection
import java.sql.ResultSet
import java.time.Instant
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * Deterministic hash of a database's content state.
 *
 * Computed over every content table (in registry order, rows ordered by
 * their diff key) plus the schema version prefix. Two databases produced
 * from the same canonical content always produce the same fingerprint;
 * any content difference changes it. Derived tables are excluded by
 * design — they are rebuilt, not stored.
 */
object DatabaseFingerprint {

    fun compute(connection: Connection, schemaVersion: Int = Schema.SCHEMA_VERSION): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update("schema:$schemaVersion\n".toByteArray(Charsets.UTF_8))

        for (spec in PatchTables.CONTENT_TABLES) {
            connection.createStatement().use { stmt ->
                val sql = "SELECT * FROM ${spec.name} ORDER BY ${spec.keyColumns.joinToString(", ")}"
                stmt.executeQuery(sql).use { rs ->
                    val columns = rs.columns()
                    val keyIndexes = spec.keyColumns.map { columns.indexOf(it) }
                    val nonKeyIndexes = columns.indices.filter { it !in keyIndexes }
                    while (rs.next()) {
                        val pkKey = keyIndexes.joinToString("\u0001") { canonical(rs, it) }
                        val canonicalPart = nonKeyIndexes
                            .map { columns[it] }
                            .sorted()
                            .joinToString(";") { col -> "$col=${canonical(rs, columns.indexOf(col))}" }
                        digest.update("${spec.name}\t$pkKey\t$canonicalPart\n".toByteArray(Charsets.UTF_8))
                    }
                }
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Convenience for CLI/diagnostics: compute from a database file. */
    fun compute(file: File, schemaVersion: Int = Schema.SCHEMA_VERSION): String =
        openKjdDatabase(file).use { compute(it, schemaVersion) }

    private fun canonical(rs: ResultSet, columnIndex: Int): String =
        rs.getObject(columnIndex + 1)?.toString() ?: "null"
}

/**
 * Generates an incremental [DatabasePatch] between two release databases.
 *
 * Requires both databases to share the same schema version — schema upgrades
 * are handled by [io.kaiteyo.kjd.db.SchemaMigrator], not by content patches.
 */
class DatabaseDiffGenerator {

    fun generate(
        from: File,
        to: File,
        generatedAt: String = Instant.now().toString()
    ): DatabasePatch {
        require(from.isFile) { "Source database not found: $from" }
        require(to.isFile) { "Target database not found: $to" }

        openKjdDatabase(from).use { fromConn ->
            openKjdDatabase(to).use { toConn ->
                val fromSchema = userVersion(fromConn)
                val toSchema = userVersion(toConn)
                require(fromSchema == toSchema) {
                    "Schema versions differ (from=$fromSchema, to=$toSchema). " +
                        "Content patches cannot cross schema versions — regenerate or migrate first."
                }

                val tables = PatchTables.CONTENT_TABLES.associate { spec ->
                    spec.name to diffTable(spec, fromConn, toConn)
                }
                val summary = PatchSummary(
                    inserted = tables.values.sumOf { it.inserts.size.toLong() },
                    updated = tables.values.sumOf { it.updates.size.toLong() },
                    deleted = tables.values.sumOf { it.deletes.size.toLong() }
                )

                return DatabasePatch(
                    patchFormatVersion = 1,
                    fromDatabaseVersion = metaValue(fromConn, "generator_version") ?: "unknown",
                    fromSchemaVersion = fromSchema,
                    toDatabaseVersion = metaValue(toConn, "generator_version") ?: "unknown",
                    toSchemaVersion = toSchema,
                    fromFingerprint = DatabaseFingerprint.compute(fromConn, fromSchema),
                    toFingerprint = DatabaseFingerprint.compute(toConn, toSchema),
                    generatedAt = generatedAt,
                    metaUpdates = readMetaUpdates(toConn),
                    tables = tables,
                    summary = summary
                )
            }
        }
    }

    private fun diffTable(spec: PatchTables.TableSpec, from: Connection, to: Connection): TablePatch {
        val oldRows = readRows(from, spec)
        val newRows = readRows(to, spec)
        val inserts = mutableListOf<Map<String, JsonElement>>()
        val updates = mutableListOf<Map<String, JsonElement>>()
        val deletes = mutableListOf<List<JsonElement>>()

        newRows.forEach { (key, row) ->
            when {
                key !in oldRows -> inserts.add(row.json)
                oldRows[key]!!.canonical != row.canonical -> updates.add(row.json)
            }
        }
        oldRows.keys.forEach { key ->
            if (key !in newRows) deletes.add(oldRows[key]!!.keyValues)
        }
        return TablePatch(
            keyColumns = spec.keyColumns,
            inserts = inserts,
            updates = updates,
            deletes = deletes
        )
    }

    private class Row(
        val keyValues: List<JsonElement>,
        val canonical: String,
        val json: Map<String, JsonElement>
    )

    private fun readRows(connection: Connection, spec: PatchTables.TableSpec): Map<String, Row> {
        val rows = LinkedHashMap<String, Row>()
        connection.createStatement().use { stmt ->
            val sql = "SELECT * FROM ${spec.name} ORDER BY ${spec.keyColumns.joinToString(", ")}"
            stmt.executeQuery(sql).use { rs ->
                val columns = rs.columns()
                val keyIndexes = spec.keyColumns.map { columns.indexOf(it) }
                val nonKeyIndexes = columns.indices.filter { it !in keyIndexes }
                while (rs.next()) {
                    val json = columns.associateWith { col -> toJson(rs, columns.indexOf(col)) }
                    val keyValues = keyIndexes.map { json[columns[it]]!! }
                    val key = keyValues.joinToString("\u0001") { it.toString() }
                    val canonical = nonKeyIndexes
                        .map { columns[it] }
                        .sorted()
                        .joinToString(";") { col -> "$col=${rs.getObject(columns.indexOf(col) + 1)?.toString() ?: "null"}" }
                    rows[key] = Row(keyValues, canonical, json)
                }
            }
        }
        return rows
    }

    private fun toJson(rs: ResultSet, columnIndex: Int): JsonElement {
        val value = rs.getObject(columnIndex + 1) ?: return JsonNull
        return when (value) {
            is Long -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value.toLong())
            is Double -> JsonPrimitive(value)
            is Float -> JsonPrimitive(value.toDouble())
            is Boolean -> JsonPrimitive(value)
            else -> JsonPrimitive(value.toString())
        }
    }

    private fun readMetaUpdates(connection: Connection): Map<String, String> {
        val updates = LinkedHashMap<String, String>()
        connection.prepareStatement("SELECT key, value FROM meta").use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val key = rs.getString(1)
                    if (key in PatchTables.EXCLUDED_META_KEYS) continue
                    updates[key] = rs.getString(2)
                }
            }
        }
        return updates
    }

    private fun metaValue(connection: Connection, key: String): String? =
        connection.prepareStatement("SELECT value FROM meta WHERE key = ?").use { stmt ->
            stmt.setString(1, key)
            stmt.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
        }
}

internal fun userVersion(connection: Connection): Int =
    connection.createStatement().use { stmt ->
        stmt.executeQuery("PRAGMA user_version").use { rs ->
            if (rs.next()) rs.getInt(1) else 0
        }
    }

internal fun ResultSet.columns(): List<String> {
    val meta = metaData
    return (1..meta.columnCount).map { meta.getColumnName(it) }
}
