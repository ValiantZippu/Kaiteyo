package ua.syt0r.kanji.desktop.engine.plugin

import kotlinx.serialization.Serializable

// ============================================
// KAITEYO PLUGIN SYSTEM — SANDBOXED MANIFEST
// The plugin.json schema for sandboxed loading:
// identity, version, entrypoints and declared
// capabilities. A plugin is not loadable until
// its manifest validates AND the sandbox grants
// its capabilities (see PluginSandbox).
//
// Named SandboxedPluginManifest to avoid colliding
// with the legacy PluginManifest used by the
// manifest-driven registry (PluginRegistry.kt).
// ============================================

@Serializable
data class SandboxedPluginManifest(
    val id: String,
    val name: String,
    val version: String = "1.0.0",
    val minApiVersion: Int = 1,
    val author: String = "",
    val description: String = "",
    /** Declared capabilities — the only things the sandbox may grant. */
    val capabilities: Set<PluginCapability> = emptySet(),
    /** Entry points: e.g. ["dictionary", "ocr", "subtitle"] or empty for pure-data. */
    val entrypoints: List<String> = emptyList(),
    val homepage: String = ""
)

object PluginManifestValidator {

    data class ValidationResult(
        val valid: Boolean,
        val errors: List<String>
    ) {
        companion object {
            fun ok() = ValidationResult(true, emptyList())
            fun fail(vararg errors: String) = ValidationResult(false, errors.toList())
        }
    }

    private val idPattern = Regex("^[a-z0-9][a-z0-9-]{1,63}$")
    private val versionPattern = Regex("^\\d+\\.\\d+\\.\\d+$")
    private val apiMax = 1

    fun validate(manifest: SandboxedPluginManifest): ValidationResult {
        val errors = mutableListOf<String>()

        if (!idPattern.matches(manifest.id)) {
            errors += "id must be 2-64 chars of lowercase letters, digits and dashes"
        }
        if (!versionPattern.matches(manifest.version)) {
            errors += "version must be semver-like (x.y.z)"
        }
        if (manifest.minApiVersion < 1 || manifest.minApiVersion > apiMax) {
            errors += "minApiVersion must be in 1..$apiMax (current platform API)"
        }
        if (manifest.entrypoints.distinct().size != manifest.entrypoints.size) {
            errors += "entrypoints must be unique"
        }
        if (manifest.entrypoints.any { it.isBlank() }) {
            errors += "entrypoints must be non-blank"
        }
        if (manifest.name.isBlank()) {
            errors += "name is required"
        }
        return if (errors.isEmpty()) ValidationResult.ok() else ValidationResult(false, errors)
    }
}
