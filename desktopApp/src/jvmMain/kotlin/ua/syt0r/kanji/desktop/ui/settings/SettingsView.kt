package ua.syt0r.kanji.desktop.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.koin.compose.koinInject
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.WorkspaceView
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsNumericField
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSearchField
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSelect
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsTextField
import ua.syt0r.kanji.desktop.designsystem.DsToggle
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.settings.SettingCategory
import ua.syt0r.kanji.desktop.engine.settings.SettingDef
import ua.syt0r.kanji.desktop.engine.settings.SettingType
import ua.syt0r.kanji.desktop.engine.updates.UpdateChannel
import ua.syt0r.kanji.desktop.engine.updates.UpdateService
import ua.syt0r.kanji.desktop.engine.updates.UpdateState
import ua.syt0r.kanji.desktop.engine.updates.currentAppVersion
import ua.syt0r.kanji.desktop.model.ToastKind

// ============================================
// SETTINGS
// Desktop-first control center: a category rail
// with icons on the left, instant search, and a
// card grid on the right. On compact windows the
// rail collapses to a horizontal chip strip. Every
// value reads from the typed SettingsEngine and
// persists live.
// ============================================

private val CATEGORY_ORDER = listOf(
    SettingCategory.General,
    SettingCategory.Navigation,
    SettingCategory.Appearance,
    SettingCategory.Review,
    SettingCategory.Browser,
    SettingCategory.Statistics,
    SettingCategory.History,
    SettingCategory.ImportExport,
    SettingCategory.Sync,
    SettingCategory.Updates,
    SettingCategory.Plugins,
    SettingCategory.Accessibility,
    SettingCategory.Advanced
)

private fun iconForCategory(category: SettingCategory): ImageVector = when (category) {
    SettingCategory.General -> Icons.Default.Settings
    SettingCategory.Navigation -> Icons.Default.Tune
    SettingCategory.Appearance -> Icons.Default.Palette
    SettingCategory.Review -> Icons.Default.School
    SettingCategory.Browser -> Icons.Default.Language
    SettingCategory.Statistics -> Icons.Default.Insights
    SettingCategory.History -> Icons.Default.History
    SettingCategory.ImportExport -> Icons.Default.FileUpload
    SettingCategory.Sync -> Icons.Default.Sync
    SettingCategory.Updates -> Icons.Default.SystemUpdate
    SettingCategory.Plugins -> Icons.Default.Extension
    SettingCategory.Accessibility -> Icons.Default.Visibility
    SettingCategory.Advanced -> Icons.Default.AutoAwesome
}

private fun describeCategory(category: SettingCategory): String = when (category) {
    SettingCategory.General -> "App-wide behavior and startup"
    SettingCategory.Navigation -> "Sidebar, compact dock and bubble launcher"
    SettingCategory.Appearance -> "Colors, base mode and theme"
    SettingCategory.Review -> "Session behavior, limits and grading"
    SettingCategory.Browser -> "Card browsing preferences"
    SettingCategory.Statistics -> "Dashboard range and goals"
    SettingCategory.History -> "Activity log retention"
    SettingCategory.ImportExport -> "Transfers and conflict policy"
    SettingCategory.Sync -> "Synchronization schedule"
    SettingCategory.Updates -> "Release channel and update checks"
    SettingCategory.Plugins -> "Extensions and automation"
    SettingCategory.Accessibility -> "Assistive options"
    SettingCategory.Advanced -> "Developer and diagnostics"
}

@Composable
fun SettingsView(state: AppState) {
    var query by remember { mutableStateOf("") }
    var version by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf(SettingCategory.General) }

    val snapshot = remember(version) { state.settings.snapshot() }
    val searching = query.isNotBlank()
    val matches = remember(version, query) { if (searching) state.settings.search(query) else emptyList() }

    BoxWithConstraints(Modifier.fillMaxSize().padding(DsSpacing.Lg)) {
        val desktop = maxWidth >= 860.dp
        if (desktop) {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
            ) {
                SettingsRail(
                    state = state,
                    query = query,
                    onQuery = { query = it },
                    selected = selected,
                    onSelect = {
                        selected = it
                        query = ""
                    },
                    modifier = Modifier.width(252.dp).fillMaxHeight()
                )
                SettingsContent(
                    state = state,
                    snapshot = snapshot,
                    version = version,
                    selected = selected,
                    searching = searching,
                    matches = matches,
                    wide = true,
                    onChanged = { state.settings.set(it.first, it.second); version++ },
                    onResetCategory = {
                        state.settings.resetCategory(it)
                        version++
                        state.activityLog.record(ActivityCategory.Settings, "Reset ${it.name} settings")
                    },
                    onResetAll = {
                        state.settings.resetAll()
                        version++
                        state.activityLog.record(ActivityCategory.Settings, "Reset all settings")
                        state.toastHost.show("All settings restored to defaults", kind = ToastKind.Info)
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                DsSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search settings…"
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
                ) {
                    populatedCategories(state).forEach { category ->
                        CategoryChip(
                            category = category,
                            selected = category == selected && !searching,
                            onClick = {
                                selected = category
                                query = ""
                            }
                        )
                    }
                }
                SettingsContent(
                    state = state,
                    snapshot = snapshot,
                    version = version,
                    selected = selected,
                    searching = searching,
                    matches = matches,
                    wide = false,
                    onChanged = { state.settings.set(it.first, it.second); version++ },
                    onResetCategory = {
                        state.settings.resetCategory(it)
                        version++
                        state.activityLog.record(ActivityCategory.Settings, "Reset ${it.name} settings")
                    },
                    onResetAll = {
                        state.settings.resetAll()
                        version++
                        state.activityLog.record(ActivityCategory.Settings, "Reset all settings")
                        state.toastHost.show("All settings restored to defaults", kind = ToastKind.Info)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun populatedCategories(state: AppState): List<SettingCategory> =
    CATEGORY_ORDER.filter { category ->
        state.settings.defs.any { it.category == category && it.searchable }
    }

@Composable
private fun SettingsRail(
    state: AppState,
    query: String,
    onQuery: (String) -> Unit,
    selected: SettingCategory,
    onSelect: (SettingCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
        Text(
            text = "Settings",
            color = sc.textPrimary,
            fontSize = DsType.Title,
            fontWeight = FontWeight.SemiBold
        )
        DsSearchField(
            value = query,
            onValueChange = onQuery,
            placeholder = "Search settings…"
        )
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            populatedCategories(state).forEach { category ->
                CategoryRow(
                    category = category,
                    selected = category == selected && query.isBlank(),
                    onClick = { onSelect(category) }
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(category: SettingCategory, selected: Boolean, onClick: () -> Unit) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(
                when {
                    selected -> ac.primary.copy(alpha = 0.12f)
                    hovered -> sc.surfaceInteractive
                    else -> Color.Transparent
                }
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Icon(
            imageVector = iconForCategory(category),
            contentDescription = null,
            tint = if (selected) ac.primary else sc.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = category.name,
            color = if (selected) sc.textPrimary else sc.textSecondary,
            fontSize = DsType.Body,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(ac.primary)
            )
        }
    }
}

@Composable
private fun CategoryChip(category: SettingCategory, selected: Boolean, onClick: () -> Unit) {
    val sc = surfaceColors()
    val ac = accent()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) ac.primary.copy(alpha = 0.16f) else sc.surfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = iconForCategory(category),
            contentDescription = null,
            tint = if (selected) ac.primary else sc.textSecondary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = category.name,
            color = if (selected) ac.primary else sc.textSecondary,
            fontSize = DsType.Label,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun SettingsContent(
    state: AppState,
    snapshot: Map<String, String>,
    version: Int,
    selected: SettingCategory,
    searching: Boolean,
    matches: List<SettingDef>,
    wide: Boolean,
    onChanged: (Pair<String, Any>) -> Unit,
    onResetCategory: (SettingCategory) -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        if (searching) {
            if (matches.isEmpty()) {
                DsCard {
                    Column(Modifier.padding(DsSpacing.Xl)) {
                        Text("No settings match", color = sc.textSecondary, fontSize = DsType.Body)
                        Text("Try a different keyword or clear the search.", color = sc.textMuted, fontSize = DsType.Caption)
                    }
                }
            } else {
                matches.groupBy { it.category }.toList()
                    .sortedBy { (category, _) -> CATEGORY_ORDER.indexOf(category) }
                    .forEach { (category, defs) ->
                        CategoryGroupCard(
                            state = state,
                            category = category,
                            defs = defs,
                            snapshot = snapshot,
                            version = version,
                            wide = wide,
                            onChanged = onChanged,
                            onReset = { onResetCategory(category) }
                        )
                    }
            }
        } else {
            val defs = state.settings.defs.filter { it.category == selected && it.searchable }
            DsCard {
                Row(
                    Modifier.padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                ) {
                    Icon(
                        imageVector = iconForCategory(selected),
                        contentDescription = null,
                        tint = accent().primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = selected.name,
                            color = sc.textPrimary,
                            fontSize = DsType.BodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = describeCategory(selected),
                            color = sc.textMuted,
                            fontSize = DsType.Caption
                        )
                    }
                    DsButton(
                        text = "Reset",
                        kind = DsButtonKind.Ghost,
                        onClick = { onResetCategory(selected) },
                        compact = true
                    )
                }
            }

            if (selected == SettingCategory.Navigation) {
                NavigationPreviewCard(state)
            }

            if (selected == SettingCategory.Appearance) {
                ThemeStudioLink(state)
            }

            if (selected == SettingCategory.General) {
                OnboardingLink(state)
            }

            if (selected == SettingCategory.Updates) {
                UpdatesCard(state)
            }

            DefsGrid(
                defs = defs,
                snapshot = snapshot,
                version = version,
                wide = wide,
                onChanged = onChanged
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            DsButton(
                text = "Reset all settings",
                icon = Icons.Default.RestartAlt,
                kind = DsButtonKind.Ghost,
                onClick = onResetAll
            )
        }
        Spacer(Modifier.height(DsSpacing.Sm))
    }
}

@Composable
private fun CategoryGroupCard(
    state: AppState,
    category: SettingCategory,
    defs: List<SettingDef>,
    snapshot: Map<String, String>,
    version: Int,
    wide: Boolean,
    onChanged: (Pair<String, Any>) -> Unit,
    onReset: () -> Unit
) {
    val sc = surfaceColors()
    DsCard {
        Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            DsSectionHeader(
                title = category.name,
                subtitle = "${defs.size} setting${if (defs.size == 1) "" else "s"}",
                action = {
                    DsButton(text = "Reset", kind = DsButtonKind.Ghost, onClick = onReset, compact = true)
                }
            )
            if (category == SettingCategory.Navigation) {
                NavigationPreviewCard(state)
            }
            DefsGrid(
                defs = defs,
                snapshot = snapshot,
                version = version,
                wide = wide,
                onChanged = onChanged
            )
        }
    }
}

/** Re-open the first-run wizard on demand — the only way onboarding reappears. */
@Composable
private fun OnboardingLink(state: AppState) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    DsCard(
        onClick = { state.requestOnboarding() },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(DsSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(DsRadius.Md))
                    .background(ac.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ac.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Show onboarding again",
                    color = sc.textPrimary,
                    fontSize = DsType.BodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Re-run the first-launch setup wizard — theme, accent, scaling and navigation",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = if (hovered) ac.primary else sc.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ThemeStudioLink(state: AppState) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    DsCard(
        onClick = { state.currentView = WorkspaceView.ThemeStudio },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(DsSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(DsRadius.Md))
                    .background(ac.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = null,
                    tint = ac.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Open Theme Studio",
                    color = sc.textPrimary,
                    fontSize = DsType.BodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Live theme editor — every color, font, scale and animation, applied instantly",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = if (hovered) ac.primary else sc.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Auto-update section: channel selection, manual check, download/install. */
@Composable
private fun UpdatesCard(state: AppState) {
    val sc = surfaceColors()
    val ac = accent()
    val uriHandler = LocalUriHandler.current
    val updateService = koinInject<UpdateService>()
    val updateState by updateService.state.collectAsState()
    val appVersion = remember { currentAppVersion() }
    // Derived from settings (not local state) so the category-level Reset
    // button keeps the select in sync with the persisted value.
    val channel = UpdateChannel.fromName(
        state.settings.getString("updates.channel", "stable")
    )

    // Keep the service in sync with the persisted channel whenever this
    // section is shown or the channel changes (incl. via Reset).
    LaunchedEffect(channel) {
        updateService.setChannel(channel)
    }

    DsCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(DsSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            // Header: current version + check action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(DsRadius.Md))
                        .background(ac.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = ac.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Updates",
                        color = sc.textPrimary,
                        fontSize = DsType.BodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Kaiteyo ${appVersion.versionName} — check for new releases",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
                val busy = updateState is UpdateState.Checking ||
                    updateState is UpdateState.Downloading ||
                    updateState is UpdateState.Applying
                DsButton(
                    text = if (busy) "Working…" else "Check for updates",
                    onClick = { updateService.check() },
                    enabled = !busy,
                    compact = true
                )
            }

            // Channel selection (persisted via settings key updates.channel)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Update channel",
                        color = sc.textPrimary,
                        fontSize = DsType.Body,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Stable is recommended. Beta and nightly are for testing.",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
                DsSelect(
                    selected = channel,
                    options = UpdateChannel.entries,
                    onSelected = { value ->
                        state.settings.setString("updates.channel", value.name.lowercase())
                        updateService.setChannel(value)
                    },
                    labelOf = { it.displayName },
                    modifier = Modifier.width(160.dp)
                )
            }

            // Live status + actions
            when (val s = updateState) {
                is UpdateState.Idle ->
                    StatusLine("No check performed yet — press “Check for updates”.")

                is UpdateState.Checking ->
                    StatusLine("Checking the ${s.channel.displayName.lowercase()} channel…")

                is UpdateState.UpToDate ->
                    StatusLine("You're up to date on the ${s.channel.displayName.lowercase()} channel.")

                is UpdateState.Available -> {
                    val artifact = s.artifact
                    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                        StatusLine(
                            if (artifact == null)
                                "Version ${s.manifest.latest.version} is available — no package for this platform yet."
                            else
                                "Version ${s.manifest.latest.version} is available for download."
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                            if (artifact != null) {
                                DsButton(
                                    text = "Download & install",
                                    onClick = { updateService.download() }
                                )
                            }
                            if (s.manifest.latest.releaseNotesUrl.isNotBlank()) {
                                DsButton(
                                    text = "Release notes",
                                    kind = DsButtonKind.Ghost,
                                    onClick = { uriHandler.openUri(s.manifest.latest.releaseNotesUrl) },
                                    compact = true
                                )
                            }
                        }
                    }
                }

                is UpdateState.Downloading -> {
                    val total = s.totalBytes
                    StatusLine(
                        if (total != null && total > 0)
                            "Downloading… ${s.downloadedBytes / 1024} KB of ${total / 1024} KB"
                        else "Downloading…"
                    )
                }

                is UpdateState.ReadyToApply ->
                    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                        StatusLine("Download complete — the update is verified and ready.")
                        DsButton(
                            text = "Install & restart",
                            onClick = { updateService.apply() }
                        )
                    }

                is UpdateState.Applying ->
                    StatusLine("Installing the update…")

                is UpdateState.Error ->
                    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                        StatusLine(s.reason)
                        if (s.retryable) {
                            DsButton(
                                text = "Retry",
                                kind = DsButtonKind.Ghost,
                                onClick = { updateService.check() },
                                compact = true
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun StatusLine(text: String) {
    Text(
        text = text,
        color = surfaceColors().textSecondary,
        fontSize = DsType.Caption
    )
}

@Composable
private fun DefsGrid(
    defs: List<SettingDef>,
    snapshot: Map<String, String>,
    version: Int,
    wide: Boolean,
    onChanged: (Pair<String, Any>) -> Unit
) {
    if (wide) {
        defs.chunked(2).forEach { pair ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
            ) {
                pair.forEach { def ->
                    DefCard(
                        def = def,
                        snapshot = snapshot,
                        version = version,
                        onChanged = onChanged,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    } else {
        defs.forEach { def ->
            DefCard(
                def = def,
                snapshot = snapshot,
                version = version,
                onChanged = onChanged,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DefCard(
    def: SettingDef,
    snapshot: Map<String, String>,
    version: Int,
    onChanged: (Pair<String, Any>) -> Unit,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    DsCard(modifier = modifier) {
        Column(Modifier.padding(DsSpacing.Md), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (def.group.isNotBlank()) {
                Text(
                    text = def.group.uppercase(),
                    color = sc.textMuted,
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.SemiBold
                )
            }
            SettingRow(def, snapshot, version, onChanged)
        }
    }
}

@Composable
private fun SettingRow(
    def: SettingDef,
    snapshot: Map<String, String>,
    version: Int,
    onChanged: (Pair<String, Any>) -> Unit
) {
    val sc = surfaceColors()
    val current = snapshot[def.key] ?: def.normalizedDefault

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = def.name,
                color = sc.textPrimary,
                fontSize = DsType.Body,
                fontWeight = FontWeight.Medium
            )
            if (def.description.isNotBlank()) {
                Text(
                    text = def.description,
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }
        Spacer(Modifier.width(DsSpacing.Md))
        when (def.type) {
            SettingType.Boolean -> DsToggle(
                checked = current.toBooleanStrictOrNull() ?: false,
                onCheckedChange = { onChanged(def.key to it) }
            )
            SettingType.Enum -> DsSelect(
                selected = current,
                options = def.options.ifEmpty { listOf(current) },
                onSelected = { onChanged(def.key to it) },
                labelOf = { it },
                modifier = Modifier.width(180.dp)
            )
            SettingType.Int -> DsNumericField(
                value = current.toIntOrNull() ?: 0,
                onValueChange = { onChanged(def.key to it) },
                label = null,
                modifier = Modifier.width(110.dp)
            )
            SettingType.Float -> {
                val text = remember(def.key, version) { mutableStateOf(current) }
                DsTextField(
                    value = text.value,
                    onValueChange = { raw ->
                        val filtered = raw.filter { it.isDigit() || it == '.' }.take(6)
                        text.value = filtered
                        onChanged(def.key to (filtered.toFloatOrNull() ?: 0f))
                    },
                    singleLine = true,
                    modifier = Modifier.width(110.dp)
                )
            }
            SettingType.String, SettingType.List -> {
                val text = remember(def.key, version) { mutableStateOf(current) }
                DsTextField(
                    value = text.value,
                    onValueChange = { text.value = it; onChanged(def.key to it) },
                    singleLine = true,
                    modifier = Modifier.width(180.dp)
                )
            }
        }
    }
}
