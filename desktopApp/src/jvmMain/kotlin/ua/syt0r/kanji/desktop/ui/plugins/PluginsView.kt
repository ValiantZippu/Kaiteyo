package ua.syt0r.kanji.desktop.ui.plugins

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsConfirmDialog
import ua.syt0r.kanji.desktop.designsystem.DsEmptyState
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTabRow
import ua.syt0r.kanji.desktop.designsystem.DsToggle
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.l10n.resolveSuiteString
import ua.syt0r.kanji.desktop.engine.plugin.MarketplaceIndex
import ua.syt0r.kanji.desktop.engine.plugin.MarketplacePlugin
import ua.syt0r.kanji.desktop.engine.plugin.PluginMarketplace
import ua.syt0r.kanji.desktop.engine.plugin.persistPlugins
import ua.syt0r.kanji.desktop.model.ToastKind

// ============================================
// PLUGINS
// Two tabs: Installed (manage enable / disable /
// uninstall) and Marketplace (browse + install a
// curated GitHub-hosted index, with an offline
// fallback catalog so the tab is never empty).
// ============================================

@Composable
fun PluginsView(state: AppState) {
    val sc = surfaceColors()
    var tab by remember { mutableStateOf(0) }
    val registry = state.pluginRegistry

    Column(
        Modifier.fillMaxSize().padding(DsSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        DsTabRow(
            tabs = listOf(
                "${resolveSuiteString { pluginsInstalledTab }} (${registry.installed.size})",
                resolveSuiteString { pluginsMarketplaceTab }
            ),
            selectedIndex = tab,
            onSelect = { tab = it }
        )
        when (tab) {
            0 -> InstalledPanel(state)
            1 -> MarketplacePanel(state)
        }
    }
}

// ============================================
// INSTALLED
// ============================================

@Composable
private fun InstalledPanel(state: AppState) {
    val sc = surfaceColors()
    var version by remember { mutableStateOf(0) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    val registry = state.pluginRegistry

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        Text(
            text = "${registry.installed.size} ${resolveSuiteString { pluginsCountSubtitle }} · ${registry.installed.count { it.manifest.enabled }} ${resolveSuiteString { enabledBadge }}",
            color = sc.textMuted,
            fontSize = DsType.Caption
        )

        if (registry.installed.isEmpty()) {
            DsCard {
                DsEmptyState(
                    title = resolveSuiteString { noPluginsTitle },
                    message = resolveSuiteString { noPluginsMessage },
                    icon = Icons.Default.Extension
                )
            }
        } else {
            registry.installed.forEach { install ->
                val plugin = install.manifest
                DsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(DsSpacing.Xl),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                                Text(plugin.name, color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                                DsBadge(text = "v${plugin.version}", tint = sc.textMuted)
                                if (plugin.enabled) DsBadge(text = resolveSuiteString { enabledBadge }, tint = Color(0xFFC2FC8B))
                                if (!plugin.enabled) DsBadge(text = resolveSuiteString { disabledBadge }, tint = Color(0xFFFEAB57))
                            }
                            Text(
                                text = plugin.description.ifBlank { resolveSuiteString { noDescription } },
                                color = sc.textMuted,
                                fontSize = DsType.Body
                            )
                            Text(
                                text = "By ${plugin.author.ifBlank { resolveSuiteString { unknownAuthor } }} · ${install.source} · ${install.installedAt.toString().take(19)}",
                                color = sc.textMuted,
                                fontSize = DsType.Caption
                            )
                        }
                        DsToggle(
                            checked = plugin.enabled,
                            onCheckedChange = { enabled ->
                                if (enabled) registry.enable(plugin.id) else registry.disable(plugin.id)
                                state.persistPlugins()
                                version++
                            }
                        )
                        Spacer(Modifier.width(DsSpacing.Sm))
                        DsIconButton(
                            icon = Icons.Default.Delete,
                            onClick = { deleteTarget = plugin.id },
                            contentDescription = resolveSuiteString { uninstallActionDesc }.format(plugin.name),
                            size = 30.dp
                        )
                    }
                }
            }
        }
    }

    deleteTarget?.let { id ->
        val plugin = registry.installed.firstOrNull { it.manifest.id == id }?.manifest
        if (plugin != null) {
            DsConfirmDialog(
                title = resolveSuiteString { uninstallConfirmTitle },
                message = resolveSuiteString { uninstallConfirmMessage }.format(plugin.name),
                confirmText = resolveSuiteString { uninstallButton },
                danger = true,
                onConfirm = {
                    registry.uninstall(id)
                    state.persistPlugins()
                    version++
                    state.activityLog.record(ActivityCategory.Plugin, "Uninstalled '${plugin.name}'")
                    deleteTarget = null
                },
                onDismiss = { deleteTarget = null }
            )
        }
    }
}

// ============================================
// MARKETPLACE
// ============================================

@Composable
private fun MarketplacePanel(state: AppState) {
    val sc = surfaceColors()
    val scope = rememberCoroutineScope()
    val registry = state.pluginRegistry
    var index by remember { mutableStateOf<MarketplaceIndex?>(null) }
    var loading by remember { mutableStateOf(true) }
    var offline by remember { mutableStateOf(false) }
    var installingId by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        loading = true
        offline = false
        scope.launch {
            val result = withContext(Dispatchers.IO) { PluginMarketplace.load() }
            result.onSuccess {
                index = it
                offline = it.plugins == PluginMarketplace.demoCatalog()
            }
            result.onFailure { e ->
                index = MarketplaceIndex(plugins = PluginMarketplace.demoCatalog())
                offline = true
                state.toastHost.show(resolveSuiteString { marketplaceOfflineToast }, kind = ToastKind.Warning)
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    fun install(plugin: MarketplacePlugin) {
        installingId = plugin.id
        scope.launch {
            val manifest = withContext(Dispatchers.IO) {
                PluginMarketplace.fetchManifest(plugin.manifestUrl).getOrElse { PluginMarketplace.localManifest(plugin) }
            }
            val wasUpdate = registry.isInstalled(plugin.id)
            registry.install(manifest, source = "marketplace")
            state.persistPlugins()
            installingId = null
            state.activityLog.record(ActivityCategory.Plugin, if (wasUpdate) "Updated '${plugin.name}' to ${plugin.version}" else "Installed '${plugin.name}' from marketplace")
            state.toastHost.show("'${plugin.name}' ${if (wasUpdate) "updated to" else "installed from marketplace"} v${plugin.version}", kind = ToastKind.Success)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(resolveSuiteString { communityPluginsTitle }, color = sc.textPrimary, fontSize = DsType.Heading, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (offline) resolveSuiteString { marketplaceOfflineSubtitle } else resolveSuiteString { marketplaceOnlineSubtitle },
                    color = sc.textMuted,
                    fontSize = DsType.Body
                )
            }
            DsButton(
                text = resolveSuiteString { refreshLabel },
                icon = Icons.Default.Refresh,
                kind = DsButtonKind.Ghost,
                onClick = { refresh() },
                compact = true
            )
        }

        if (loading) {
            DsCard {
                DsEmptyState(
                    title = resolveSuiteString { loadingMarketplace },
                    message = resolveSuiteString { marketplaceFetchingMessage },
                    icon = Icons.Default.Download
                )
            }
        } else {
            val plugins = index?.plugins ?: emptyList()
            if (plugins.isEmpty()) {
                DsCard {
                    DsEmptyState(
                        title = resolveSuiteString { marketplaceEmpty },
                        message = resolveSuiteString { marketplaceEmptyMessage },
                        icon = Icons.Default.Extension
                    )
                }
            }
            plugins.forEach { plugin ->
                val installed = registry.isInstalled(plugin.id)
                val installedVersion = registry.installedVersion(plugin.id)
                val updateAvailable = installed && installedVersion != plugin.version

                DsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(DsSpacing.Xl),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                                Text(plugin.name, color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                                DsBadge(text = "v${plugin.version}", tint = sc.textMuted)
                                DsBadge(text = plugin.category, tint = Color(0xFF7BC8FF))
                                DsBadge(text = plugin.license, tint = Color(0xFFA78BFA))
                            }
                            Text(plugin.description, color = sc.textMuted, fontSize = DsType.Body)
                            Text(
                                text = buildString {
                                    append("By ${plugin.author.ifBlank { resolveSuiteString { unknownAuthor } }}")
                                    if (plugin.downloads > 0) append(" · ${plugin.downloads} ${resolveSuiteString { downloadsSuffix }}")
                                    if (plugin.stars > 0) append(" · ${plugin.stars} ${resolveSuiteString { starsSuffix }}")
                                    if (updateAvailable) append(" · ${resolveSuiteString { installedVersionSuffix }}$installedVersion")
                                },
                                color = sc.textMuted,
                                fontSize = DsType.Caption
                            )
                        }
                        when {
                            updateAvailable -> DsButton(
                                text = resolveSuiteString { updateButton },
                                icon = Icons.Default.Download,
                                compact = true,
                                enabled = installingId != plugin.id,
                                onClick = { install(plugin) }
                            )
                            installed -> DsBadge(text = resolveSuiteString { installedBadge }, tint = Color(0xFFC2FC8B))
                            else -> DsButton(
                                text = if (installingId == plugin.id) resolveSuiteString { installingLabel } else resolveSuiteString { installButton },
                                icon = Icons.Default.Download,
                                compact = true,
                                enabled = installingId != plugin.id,
                                onClick = { install(plugin) }
                            )
                        }
                    }
                }
            }
        }
    }
}
