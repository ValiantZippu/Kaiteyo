package ua.syt0r.kanji.desktop.engine.plugin

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.desktop.appstate.AppState

// ============================================
// PLUGIN ARCHITECTURE
// Manifest-driven plugin model + registry. The
// extension points are open interfaces so plugins
// can contribute commands, providers and views.
// ============================================

@Serializable
data class PluginManifest(
    val id: String,
    val name: String,
    val version: String = "1.0.0",
    val author: String = "",
    val description: String = "",
    val entryPoint: String = "",
    val permissions: List<String> = emptyList(),
    val minAppVersion: String = "1.0.0",
    val enabled: Boolean = true
)

@Serializable
data class PluginInstall(
    val manifest: PluginManifest,
    val installedAt: Instant = Clock.System.now(),
    val source: String = "local"
)

/** A loaded plugin instance. Implementations are JVM/service-loaded in production. */
interface KaiteyoPlugin {
    val manifest: PluginManifest

    fun onLoad() = Unit
    fun onUnload() = Unit
    fun onEnable() = Unit
    fun onDisable() = Unit

    /** Command-palette entries contributed by the plugin. */
    fun commands(): List<PluginCommand> = emptyList()

    /** Additional settings pages contributed by the plugin. */
    fun settingsPages(): List<PluginSettingsPage> = emptyList()
}

data class PluginCommand(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val keywords: String = "",
    val category: String = "Plugins",
    val execute: () -> Unit = {}
)

data class PluginSettingsPage(
    val id: String,
    val title: String,
    val content: @androidx.compose.runtime.Composable () -> Unit
)

/** Registry holds manifests, install state and enabled flags. */
class PluginRegistry {

    private val _installed = mutableListOf<PluginInstall>()
    val installed: List<PluginInstall> get() = _installed.toList()

    private val _instances = mutableMapOf<String, KaiteyoPlugin>()
    val instances: Map<String, KaiteyoPlugin> get() = _instances.toMap()

    private val _messages = mutableListOf<String>()

    fun install(manifest: PluginManifest, source: String = "local"): Result<Unit> = runCatching {
        // Sandbox gate (ADR-0011, KT-SEC-002): deny by default. Every declared
        // permission must be a known capability name or a known legacy tag
        // (the marketplace's tag vocabulary, used as pre-capability-model
        // permissions) — anything else is rejected before the plugin is stored.
        val knownCapabilities = PluginCapability.entries.map { it.name }.toSet()
        val knownLegacyTags = setOf(
            "audio", "reading", "pitch", "accent", "pronunciation", "radicals",
            "study", "theme", "appearance", "anki", "export", "sync", "import",
            "stats", "analytics", "insights", "dictionary", "search", "mine",
            "cards", "network", "files", "ui", "subtitle", "lookup"
        )
        val unknown = manifest.permissions - (knownCapabilities + knownLegacyTags)
        if (unknown.isNotEmpty()) {
            error("install rejected by sandbox — unknown permissions: ${unknown.joinToString(", ")} (deny by default)")
        }
        val existing = _installed.firstOrNull { it.manifest.id == manifest.id }
        if (existing != null) {
            _installed.remove(existing)
        }
        _installed.add(PluginInstall(manifest, source = source))
        _messages.add("Installed ${manifest.name} ${manifest.version}")
    }

    fun uninstall(id: String) {
        _instances.remove(id)?.onUnload()
        _installed.removeAll { it.manifest.id == id }
        _messages.add("Uninstalled plugin $id")
    }

    fun enable(id: String) {
        val idx = _installed.indexOfFirst { it.manifest.id == id }
        if (idx == -1) return
        _installed[idx] = _installed[idx].copy(manifest = _installed[idx].manifest.copy(enabled = true))
        _instances[id]?.onEnable()
    }

    fun disable(id: String) {
        val idx = _installed.indexOfFirst { it.manifest.id == id }
        if (idx == -1) return
        _installed[idx] = _installed[idx].copy(manifest = _installed[idx].manifest.copy(enabled = false))
        _instances[id]?.onDisable()
    }

    fun registerInstance(plugin: KaiteyoPlugin) {
        _instances[plugin.manifest.id] = plugin
    }

    fun enabled(): List<PluginInstall> = _installed.filter { it.manifest.enabled }

    fun commands(): List<PluginCommand> =
        enabled().mapNotNull { _instances[it.manifest.id] }.flatMap { it.commands() }

    fun allCommands(): List<PluginCommand> = commands()

    fun messages(): List<String> = _messages.toList()

    fun clearMessages() = _messages.clear()

    fun isInstalled(id: String): Boolean = _installed.any { it.manifest.id == id }

    fun installedVersion(id: String): String? = _installed.firstOrNull { it.manifest.id == id }?.manifest?.version

    // ------------------------------------------------------------
    // Persistence (serialized into the settings engine)
    // ------------------------------------------------------------

    @Serializable
    private data class RegistrySnapshot(val plugins: List<PluginManifest>)

    fun toSnapshot(): String {
        val manifests = _installed.map { it.manifest }
        return Json { encodeDefaults = true }.encodeToString(RegistrySnapshot(manifests))
    }

    fun restoreSnapshot(raw: String) {
        if (raw.isBlank()) return
        runCatching {
            val snapshot = Json { ignoreUnknownKeys = true }.decodeFromString<RegistrySnapshot>(raw)
            _installed.clear()
            snapshot.plugins.forEach { manifest ->
                _installed.add(PluginInstall(manifest, source = "restored"))
            }
        }
    }
}

/** Persist the current plugin registry into the settings engine. */
fun AppState.persistPlugins() {
    settings.set("plugins.installed", pluginRegistry.toSnapshot())
}
