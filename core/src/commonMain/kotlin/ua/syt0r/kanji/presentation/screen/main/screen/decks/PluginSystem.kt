package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.core.transfer.ImportExportContract.ExportConfig
import ua.syt0r.kanji.core.transfer.ImportPreview
import ua.syt0r.kanji.core.transfer.ImportResult
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.ui.KaiteyoAlertDialog

// ============================================
// PLUGIN FOUNDATION SYSTEM
// Extension points for import/export, dictionaries,
// stats, study modes, themes, and more
// ============================================

/** Plugin manifest — describes what a plugin does */
data class PluginManifest(
    val id: String,
    val name: String,
    val version: String = "1.0.0",
    val author: String = "Unknown",
    val description: String = "",
    val extensionPoint: PluginExtensionPoint = PluginExtensionPoint.Dictionary,
    val minAppVersion: String = "1.0.0",
    val permissions: List<String> = emptyList(),
    val icon: String = "🧩",
    val homepage: String = "",
    val license: String = "MIT"
)

/** Plugin state management */
class PluginRuntime(
    val manifest: PluginManifest,
    var enabled: Boolean = true,
    var config: MutableMap<String, String> = mutableMapOf(),
    var loadTime: Long = 0L,
    var errorCount: Int = 0,
    var lastError: String? = null
)

/** Plugin manager — loads, enables, disables plugins */
class PluginManager {
    private val plugins = mutableListOf<PluginRuntime>()

    fun register(manifest: PluginManifest): PluginRuntime {
        val existing = plugins.find { it.manifest.id == manifest.id }
        if (existing != null) return existing
        val runtime = PluginRuntime(manifest)
        plugins.add(runtime)
        return runtime
    }

    fun getPlugin(id: String): PluginRuntime? = plugins.find { it.manifest.id == id }
    fun getAllPlugins(): List<PluginRuntime> = plugins.toList()
    fun getEnabledPlugins(): List<PluginRuntime> = plugins.filter { it.enabled }
    fun getPluginsByExtension(point: PluginExtensionPoint): List<PluginRuntime> =
        plugins.filter { it.manifest.extensionPoint == point }

    fun enable(id: String) { getPlugin(id)?.enabled = true }
    fun disable(id: String) { getPlugin(id)?.enabled = false }
    fun remove(id: String) { plugins.removeAll { it.manifest.id == id } }

    fun updateConfig(id: String, key: String, value: String) {
        getPlugin(id)?.config?.put(key, value)
    }

    /** Built-in plugin examples */
    fun registerBuiltins() {
        register(PluginManifest(
            id = "builtin.apkg-import",
            name = "Anki APKG Importer",
            version = "2.0.0",
            author = "Kaiteyo",
            description = "Import Anki deck packages (.apkg files)",
            extensionPoint = PluginExtensionPoint.ImportFormat,
            icon = "📦"
        ))
        register(PluginManifest(
            id = "builtin.jmdict",
            name = "JMdict Dictionary",
            version = "1.0.0",
            author = "Kaiteyo",
            description = "Japanese dictionary lookup from JMdict",
            extensionPoint = PluginExtensionPoint.Dictionary,
            icon = "📖"
        ))
        register(PluginManifest(
            id = "builtin.tts",
            name = "Text-to-Speech",
            version = "1.0.0",
            author = "Kaiteyo",
            description = "Japanese pronunciation via TTS engine",
            extensionPoint = PluginExtensionPoint.AudioSource,
            icon = "🔊"
        ))
        register(PluginManifest(
            id = "builtin.ocr",
            name = "Handwriting OCR",
            version = "1.0.0",
            author = "Kaiteyo",
            description = "Recognize handwritten kanji characters",
            extensionPoint = PluginExtensionPoint.MediaProvider,
            icon = "✍️"
        ))
    }
}

// ============================================
// PLUGIN UI
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginManagerScreen(
    pluginManager: PluginManager = remember { PluginManager().also { it.registerBuiltins() } },
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Installed") }
    var showDetail by remember { mutableStateOf<PluginRuntime?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plugin Manager") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
                },
                actions = {
                    // Re-register the built-in plugin set, restoring any that
                    // were removed, and reset per-plugin error state.
                    IconButton(onClick = {
                        pluginManager.registerBuiltins()
                        pluginManager.getAllPlugins().forEach { it.errorCount = 0; it.lastError = null }
                    }) { Icon(Icons.Default.Refresh, "Refresh") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Tabs
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Installed", "Browse", "Updates", "Settings").forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab, fontSize = 12.sp) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            when (selectedTab) {
                "Installed" -> InstalledPluginsList(
                    plugins = pluginManager.getAllPlugins(),
                    onToggle = { plugin ->
                        if (plugin.enabled) pluginManager.disable(plugin.manifest.id)
                        else pluginManager.enable(plugin.manifest.id)
                    },
                    onDetail = { showDetail = it },
                    onRemove = { pluginManager.remove(it.manifest.id) }
                )
                "Browse" -> BrowsePluginsTab()
                "Updates" -> Text("No updates available", modifier = Modifier.padding(16.dp))
                "Settings" -> PluginSettingsTab()
            }
        }
    }

    showDetail?.let { plugin ->
        PluginDetailDialog(
            plugin = plugin,
            onToggle = {
                if (plugin.enabled) pluginManager.disable(plugin.manifest.id)
                else pluginManager.enable(plugin.manifest.id)
                showDetail = null
            },
            onApplyConfig = { key, value ->
                pluginManager.updateConfig(plugin.manifest.id, key, value)
                plugin.lastError = null
                plugin.errorCount = 0
            },
            onDismiss = { showDetail = null }
        )
    }
}

@Composable
private fun InstalledPluginsList(
    plugins: List<PluginRuntime>,
    onToggle: (PluginRuntime) -> Unit,
    onDetail: (PluginRuntime) -> Unit,
    onRemove: (PluginRuntime) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(plugins, key = { it.manifest.id }) { plugin ->
            PluginListItem(plugin = plugin, onToggle = { onToggle(plugin) }, onDetail = { onDetail(plugin) }, onRemove = { onRemove(plugin) })
        }
    }
}

@Composable
private fun PluginListItem(
    plugin: PluginRuntime,
    onToggle: () -> Unit,
    onDetail: () -> Unit,
    onRemove: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onDetail() },
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier.size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(plugin.manifest.icon, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))

            // Info
            Column(Modifier.weight(1f)) {
                Text(plugin.manifest.name, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(plugin.manifest.version + " by " + plugin.manifest.author,
                    fontSize = 11.sp, color = surfaceColors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(plugin.manifest.extensionPoint.displayName,
                    fontSize = 10.sp, color = accent.primary.copy(alpha = 0.7f))
                if (plugin.lastError != null) {
                    Text("⚠ Error: ${plugin.lastError}", fontSize = 10.sp, color = MaterialTheme.colorScheme.error,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // Toggle + menu
            Switch(checked = plugin.enabled, onCheckedChange = { onToggle() })
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, "Options", Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Configure") }, onClick = { showMenu = false; onDetail() })
                    DropdownMenuItem(text = { Text("Disable") }, onClick = { showMenu = false; onToggle() })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onRemove() })
                }
            }
        }
    }
}

@Composable
private fun PluginDetailDialog(
    plugin: PluginRuntime,
    onToggle: () -> Unit,
    onApplyConfig: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var configKey by remember { mutableStateOf("") }
    var configValue by remember { mutableStateOf("") }

    KaiteyoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(plugin.manifest.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(plugin.manifest.icon, fontSize = 32.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("v${plugin.manifest.version}", style = MaterialTheme.typography.labelMedium)
                        Text("by ${plugin.manifest.author}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Text(plugin.manifest.description, style = MaterialTheme.typography.bodyMedium)

                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enabled", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = plugin.enabled, onCheckedChange = { onToggle() })
                }

                if (plugin.manifest.permissions.isNotEmpty()) {
                    Text("Permissions:", style = MaterialTheme.typography.labelMedium)
                    plugin.manifest.permissions.forEach { perm ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, null, Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text(perm, fontSize = 12.sp)
                        }
                    }
                }

                // Config — key/value pairs are stored on the plugin runtime.
                Text("Configuration", style = MaterialTheme.typography.labelMedium)
                if (plugin.config.isNotEmpty()) {
                    plugin.config.forEach { (key, value) ->
                        Text("$key = $value", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(value = configKey, onValueChange = { configKey = it },
                        placeholder = { Text("Key") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = configValue, onValueChange = { configValue = it },
                        placeholder = { Text("Value") }, modifier = Modifier.weight(1f), singleLine = true)
                }

                Text("Extension Point: ${plugin.manifest.extensionPoint.displayName}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(
                enabled = configKey.isNotBlank() && configValue.isNotBlank(),
                onClick = {
                    onApplyConfig(configKey.trim(), configValue.trim())
                    configKey = ""
                    configValue = ""
                }
            ) { Text("Apply") }
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun BrowsePluginsTab() {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Extension, null, Modifier.size(64.dp),
            tint = surfaceColors.textMuted.copy(alpha = 0.3f))
        Spacer(Modifier.height(12.dp))
        Text("Plugin Marketplace", style = MaterialTheme.typography.titleMedium)
        Text("Coming in a future update", style = MaterialTheme.typography.bodyMedium,
            color = surfaceColors.textMuted)
        Spacer(Modifier.height(4.dp))
        Text("Plugin SDK and marketplace will allow community extensions.",
            style = MaterialTheme.typography.bodySmall, color = surfaceColors.textMuted)
    }
}

@Composable
private fun PluginSettingsTab() {
    val surfaceColors = LocalSurfaceColors.current
    var sandboxMode by remember { mutableStateOf(true) }
    var autoUpdate by remember { mutableStateOf(true) }
    var devMode by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Plugin Settings", style = MaterialTheme.typography.titleSmall)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = sandboxMode, onCheckedChange = { sandboxMode = it })
            Spacer(Modifier.width(8.dp))
            Column { Text("Sandbox Mode"); Text("Run plugins in isolated environment", fontSize = 12.sp, color = surfaceColors.textMuted) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = autoUpdate, onCheckedChange = { autoUpdate = it })
            Spacer(Modifier.width(8.dp))
            Column { Text("Auto-Update"); Text("Automatically update plugins", fontSize = 12.sp, color = surfaceColors.textMuted) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = devMode, onCheckedChange = { devMode = it })
            Spacer(Modifier.width(8.dp))
            Column { Text("Developer Mode"); Text("Load plugins from local folders", fontSize = 12.sp, color = surfaceColors.textMuted) }
        }
    }
}

// ============================================
// PLUGIN EXTENSION POINTS (Interfaces)
// ============================================

/** Interface for import plugins */
interface ImportPlugin {
    fun canHandle(filePath: String): Boolean
    fun import(filePath: String, onProgress: (Float) -> Unit): ImportResult
    fun preview(filePath: String): ImportPreview
}

/** Interface for export plugins */
interface ExportPlugin {
    fun canHandle(format: String): Boolean
    fun export(cards: List<KaiteyoCard>, config: ExportConfig): ByteArray
}

/** Interface for dictionary lookup plugins */
interface DictionaryPlugin {
    fun lookup(query: String): List<DictionaryEntry>
    fun supportedLanguages(): List<String>
}

data class DictionaryEntry(
    val word: String,
    val reading: String,
    val meanings: List<String>,
    val partOfSpeech: String = "",
    val frequency: Int = 0,
    val jlptLevel: String = "",
    val source: String = ""
)

/** Interface for stats visualization plugins */
interface StatsPlugin {
    fun renderDashboard(cards: List<KaiteyoCard>): @Composable (@Composable () -> Unit) -> Unit
    fun supportedChartTypes(): List<String>
}

/** Interface for study mode plugins */
interface StudyModePlugin {
    fun name(): String
    fun description(): String
    fun startSession(cards: List<KaiteyoCard>)
    fun evaluateCard(card: KaiteyoCard, answer: String): StudyAction
}
