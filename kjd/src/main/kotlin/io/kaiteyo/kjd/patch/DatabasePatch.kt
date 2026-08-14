package io.kaiteyo.kjd.patch

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * An incremental database patch: the delta between two generated KJD
 * releases. Only the *content* tables are diffed — the derived tables
 * (`search_index`, `kanji_fts`, `vocab_fts`) are pure functions of the
 * content and are rebuilt on apply.
 *
 * Safety model (non-destructive apply):
 *
 *   - every generated database records a deterministic [DatabaseFingerprint]
 *     of its content tables in `meta.state_fingerprint`;
 *   - a patch embeds the fingerprint of its source release
 *     ([fromFingerprint]) and of its target release ([toFingerprint]);
 *   - the patcher refuses to apply unless the target database's fingerprint
 *     matches [fromFingerprint], applies everything inside one transaction,
 *     rebuilds the derived indexes, re-verifies that the result hashes to
 *     [toFingerprint] and only then commits.
 *
 * A failed or rejected apply leaves the target database byte-for-byte
 * unchanged. Patching never touches user learning data — KJD databases
 * contain no user data by design.
 */
@Serializable
data class DatabasePatch(
    /** Bump when the patch file format changes. */
    val patchFormatVersion: Int = 1,
    val fromDatabaseVersion: String,
    val fromSchemaVersion: Int,
    val toDatabaseVersion: String,
    val toSchemaVersion: Int,
    val fromFingerprint: String,
    val toFingerprint: String,
    val generatedAt: String,
    /** meta table values that must be carried over (e.g. sources_json). */
    val metaUpdates: Map<String, String> = emptyMap(),
    val tables: Map<String, TablePatch>,
    val summary: PatchSummary
)

/** Row-level changes for one content table. */
@Serializable
data class TablePatch(
    /** Columns that identify a row (diff key, usually the primary key). */
    val keyColumns: List<String>,
    /** Rows present in the new release only — full row data. */
    val inserts: List<Map<String, JsonElement>> = emptyList(),
    /** Rows whose content changed — full new row data (keys unchanged). */
    val updates: List<Map<String, JsonElement>> = emptyList(),
    /** Rows removed in the new release — key values only. */
    val deletes: List<List<JsonElement>> = emptyList()
) {
    val isEmpty: Boolean get() = inserts.isEmpty() && updates.isEmpty() && deletes.isEmpty()
}

@Serializable
data class PatchSummary(
    val inserted: Long = 0,
    val updated: Long = 0,
    val deleted: Long = 0
) {
    val total: Long get() = inserted + updated + deleted
    val isEmpty: Boolean get() = total == 0L
}

/** Result of applying a patch. */
sealed interface PatchResult {
    /** The patch was applied and verified. */
    data class Applied(val summary: PatchSummary, val targetFingerprint: String) : PatchResult

    /** The target is already at the patch's target state — nothing to do. */
    data object AlreadyApplied : PatchResult
}

/**
 * Registry of content tables participating in diffs and fingerprints.
 *
 * [CONTENT_TABLES] is ordered parent-first so inserts can be applied in
 * list order (children reference parents) and deletes in reverse order
 * (children removed before parents).
 */
object PatchTables {

    data class TableSpec(
        val name: String,
        /** Columns forming the stable identity of a row. */
        val keyColumns: List<String>
    )

    val CONTENT_TABLES: List<TableSpec> = listOf(
        TableSpec("source", listOf("id")),
        TableSpec("radical", listOf("id")),
        TableSpec("kana", listOf("id")),
        TableSpec("kanji", listOf("id")),
        TableSpec("component", listOf("id")),
        TableSpec("kanji_stroke", listOf("id")),
        TableSpec("vocab", listOf("id")),
        TableSpec("sense", listOf("id")),
        TableSpec("vocab_furigana", listOf("vocab_id")),
        TableSpec("example_sentence", listOf("id")),
        TableSpec("relationship", listOf("id")),
        // source_record uses its natural unique key (its INTEGER id is
        // autoincrement and would differ between regenerations).
        TableSpec("source_record", listOf("source_id", "entity_id", "source_record_id"))
    )

    /** Tables that are always rebuilt, never diffed or fingerprinted. */
    val DERIVED_TABLES: List<String> = listOf("search_index", "kanji_fts", "vocab_fts")

    /** meta keys written by the platform itself, not carried by patches. */
    val EXCLUDED_META_KEYS: Set<String> = setOf(
        "schema_version",
        "state_fingerprint",
        "generated_at",
        "patched_from"
    )
}
