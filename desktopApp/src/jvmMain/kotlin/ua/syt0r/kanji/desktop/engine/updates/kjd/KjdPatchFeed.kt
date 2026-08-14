package ua.syt0r.kanji.desktop.engine.updates.kjd

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ============================================
// KJD PATCH FEED — feed format v1
//
// Mirrors the release pipeline's kjd patch
// feed (kjd-update-{channel}.json). Each entry
// points at a serialized io.kaiteyo.kjd.patch
// .DatabasePatch artifact. The checker selects
// the entry whose fromFingerprint matches the
// bundled database's current fingerprint, so
// only directly applicable patches are offered.
// ============================================

@Serializable
data class KjdPatchFeed(
    @SerialName("schema_version") val schemaVersion: Int = 1,
    val channel: String = "stable",
    @SerialName("published_at") val publishedAt: String = "",
    @SerialName("database_version") val databaseVersion: String = "",
    val patches: List<PatchEntry> = emptyList()
) {
    @Serializable
    data class PatchEntry(
        @SerialName("from_database_version") val fromDatabaseVersion: String = "",
        @SerialName("from_fingerprint") val fromFingerprint: String = "",
        @SerialName("to_database_version") val toDatabaseVersion: String = "",
        @SerialName("to_fingerprint") val toFingerprint: String = "",
        val url: String = "",
        val sha256: String = "",
        @SerialName("size_bytes") val sizeBytes: Long = 0
    )
}

/** Parses and validates the KJD patch feed. Throws on structurally invalid input. */
object KjdPatchFeedParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(raw: String): KjdPatchFeed {
        val feed = json.decodeFromString<KjdPatchFeed>(raw)
        require(feed.schemaVersion == 1) { "Unsupported KJD feed schema ${feed.schemaVersion}" }
        require(feed.patches.isNotEmpty()) { "KJD feed contains no patches" }
        feed.patches.forEach { entry ->
            require(entry.sha256.length == 64) { "Patch missing sha256: ${entry.toDatabaseVersion}" }
            require(entry.url.startsWith("https://")) { "Patch URL must be HTTPS: ${entry.url}" }
        }
        return feed
    }
}
