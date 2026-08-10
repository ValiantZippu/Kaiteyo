package ua.syt0r.kanji.desktop.engine.updates

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ============================================
// UPDATE MANIFEST — feed format v1
// Mirrors installer/common/update-manifest.schema.json.
// ============================================

@Serializable
data class UpdateManifest(
    @SerialName("schema_version") val schemaVersion: Int = 1,
    val channel: String = UpdateChannel.Stable.name.lowercase(),
    @SerialName("published_at") val publishedAt: String = "",
    val latest: LatestVersion = LatestVersion(),
    val artifacts: Map<String, UpdateArtifact> = emptyMap()
) {
    @Serializable
    data class LatestVersion(
        val version: String = "",
        @SerialName("version_code") val versionCode: Int = 0,
        @SerialName("release_notes_url") val releaseNotesUrl: String = "",
        @SerialName("min_app_version") val minAppVersion: String = ""
    )

    @Serializable
    data class UpdateArtifact(
        val url: String = "",
        val sha256: String = "",
        @SerialName("size_bytes") val sizeBytes: Long = 0,
        val arch: String = ""
    )

    /** The artifact key for this OS + architecture, per the canonical keys
     *  defined in the schema (windows-exe, macos-arm, linux-appimage, …). */
    fun artifactFor(os: String, arch: String): UpdateArtifact? {
        val candidates = when (os) {
            "windows" -> listOf("windows-exe", "windows-msi", "windows-portable")
            "macos" -> listOf("macos-$arch", "macos-arm", "macos-intel")
            "linux" -> listOf("linux-appimage", "linux-deb", "linux-rpm")
            else -> emptyList()
        }
        return candidates.firstNotNullOfOrNull { artifacts[it] }
    }
}

object UpdateManifestParser {

    private val json = Json { ignoreUnknownKeys = true }

    /** Parses and validates the feed. Throws on structurally invalid input. */
    fun parse(raw: String): UpdateManifest {
        val manifest = json.decodeFromString<UpdateManifest>(raw)
        require(manifest.schemaVersion == 1) { "Unsupported manifest schema ${manifest.schemaVersion}" }
        require(manifest.latest.versionCode > 0) { "Manifest missing version code" }
        return manifest
    }
}
