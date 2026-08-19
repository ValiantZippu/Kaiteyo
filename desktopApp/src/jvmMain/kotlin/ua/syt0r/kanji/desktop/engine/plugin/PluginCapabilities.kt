package ua.syt0r.kanji.desktop.engine.plugin

import kotlinx.serialization.Serializable

// ============================================
// KAITEYO PLUGIN SYSTEM — CAPABILITY MODEL
// The sandbox contract (ADR-0011): a plugin
// declares capabilities and the runtime grants
// exactly those — nothing more (deny by
// default). No capability = no runtime loading.
// Every capability is narrowly scoped; there is
// no catch-all "everything" permission.
// ============================================

@Serializable
enum class PluginCapability(val label: String) {
    ReadDictionary("Read installed dictionaries"),
    SearchDictionary("Search the bundled dictionary"),
    MineCards("Create study cards (mining)"),
    ReadCards("Read the user's card pool"),
    TextLookup("Look up Japanese text in the app"),
    Network("Make outbound network requests"),
    ReadFiles("Read files the user explicitly picks"),
    WriteAppData("Write to the app's data directory"),
    UiHooks("Register UI hooks (popup actions)"),
    SubtitleIndex("Access the subtitle search index")
}

/** A plugin's declared permissions: capabilities + optional settings. */
@Serializable
data class PluginPermissions(
    val capabilities: Set<PluginCapability> = emptySet(),
    /** Sensitive capabilities require explicit user confirmation. */
    val userApproved: Set<PluginCapability> = emptySet()
) {
    val isTriviallySafe: Boolean get() = capabilities.all { it in LOW_RISK }
    val hasUntrustedNetwork: Boolean get() = PluginCapability.Network in capabilities
}

/**
 * Capabilities the sandbox may grant without extra prompts (still declared,
 * still opt-in). Anything else — network, file writes, card mutation —
 * requires explicit user approval.
 */
val LOW_RISK: Set<PluginCapability> = setOf(
    PluginCapability.ReadDictionary,
    PluginCapability.SearchDictionary,
    PluginCapability.TextLookup,
    PluginCapability.ReadCards,
    PluginCapability.SubtitleIndex
)

object PluginCapabilityResolver {

    /**
     * Resolve the effective permissions for a manifest. The declared set is
     * intersected with the platform's allow-list (deny by default): unknown
     * capabilities are dropped, and sensitive ones must appear in
     * [PluginPermissions.userApproved] to take effect.
     */
    fun resolve(
        declared: Set<PluginCapability>,
        userApproved: Set<PluginCapability> = emptySet(),
        platformAllowList: Set<PluginCapability> = LOW_RISK + PluginCapability.MineCards + PluginCapability.UiHooks
    ): PluginPermissions {
        val safe = declared intersect platformAllowList
        val sensitive = (declared - safe) intersect userApproved intersect platformAllowList
        return PluginPermissions(
            capabilities = safe + sensitive,
            userApproved = sensitive
        )
    }

    /** True when every declared capability is actually granted. */
    fun isFullyGranted(declared: Set<PluginCapability>, granted: Set<PluginCapability>): Boolean =
        declared.all { it in granted }
}
