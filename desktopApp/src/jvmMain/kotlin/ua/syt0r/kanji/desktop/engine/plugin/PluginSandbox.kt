package ua.syt0r.kanji.desktop.engine.plugin

import kotlinx.serialization.json.Json

// ============================================
// KAITEYO PLUGIN SYSTEM — SANDBOX
// The gate before ANY plugin runtime loading
// (ADR-0011 / KT-SEC-002): validate the
// manifest, resolve capabilities against the
// platform allow-list (deny by default), and
// refuse to load unless the declaration is
// fully granted. No sandbox approval = no load.
// ============================================

class PluginSandbox(
    private val platformAllowList: Set<PluginCapability> = LOW_RISK +
        PluginCapability.MineCards + PluginCapability.UiHooks
) {

    data class SandboxDecision(
        val manifest: SandboxedPluginManifest? = null,
        val permissions: PluginPermissions = PluginPermissions(),
        val reason: String = ""
    ) {
        val approved: Boolean get() = manifest != null

        companion object {
            fun rejected(reason: String) = SandboxDecision(reason = reason)
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Inspect a raw manifest JSON. Returns a decision that either carries a
     * fully-granted manifest + permissions or a rejection reason. This is an
     * inspection gate — it never executes plugin code.
     */
    fun inspect(rawJson: String, userApproved: Set<PluginCapability> = emptySet()): SandboxDecision {
        val manifest = runCatching {
            json.decodeFromString<SandboxedPluginManifest>(rawJson)
        }.getOrElse {
            return SandboxDecision.rejected("manifest is not valid JSON: ${it.message}")
        }

        val validation = PluginManifestValidator.validate(manifest)
        if (!validation.valid) {
            return SandboxDecision.rejected("manifest invalid: ${validation.errors.joinToString("; ")}")
        }

        val permissions = PluginCapabilityResolver.resolve(
            declared = manifest.capabilities,
            userApproved = userApproved,
            platformAllowList = platformAllowList
        )

        if (!PluginCapabilityResolver.isFullyGranted(manifest.capabilities, permissions.capabilities)) {
            val missing = manifest.capabilities - permissions.capabilities
            return SandboxDecision.rejected(
                "capabilities not granted: ${missing.joinToString(", ")} (deny by default)"
            )
        }

        return SandboxDecision(
            manifest = manifest,
            permissions = permissions,
            reason = "approved"
        )
    }
}
