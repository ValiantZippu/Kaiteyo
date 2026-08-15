package ua.syt0r.kanji.desktop.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
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
import ua.syt0r.kanji.presentation.common.resources.brand.BrandMark
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.media.MediaCapture
import ua.syt0r.kanji.desktop.engine.media.MediaEngine
import ua.syt0r.kanji.desktop.engine.playback.BackendKind
import ua.syt0r.kanji.desktop.engine.settings.SettingCategory
import ua.syt0r.kanji.desktop.engine.settings.SettingDef
import ua.syt0r.kanji.desktop.engine.settings.SettingType
import ua.syt0r.kanji.desktop.ui.themes.ThemeStudioView
import ua.syt0r.kanji.desktop.ui.tutorial.TutorialChapter
import ua.syt0r.kanji.desktop.ui.tutorial.TutorialReplayTarget
import ua.syt0r.kanji.desktop.ui.tutorial.tutorialChapters
import ua.syt0r.kanji.desktop.engine.updates.UpdateChannel
import ua.syt0r.kanji.desktop.engine.updates.UpdateService
import ua.syt0r.kanji.desktop.engine.updates.UpdateState
import ua.syt0r.kanji.desktop.engine.updates.currentAppVersion
import ua.syt0r.kanji.desktop.designsystem.errorColor
import ua.syt0r.kanji.desktop.designsystem.successColor
import ua.syt0r.kanji.desktop.designsystem.warningColor
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
    SettingCategory.Media,
    SettingCategory.Statistics,
    SettingCategory.History,
    SettingCategory.Activity,
    SettingCategory.ImportExport,
    SettingCategory.Sync,
    SettingCategory.Updates,
    SettingCategory.Plugins,
    SettingCategory.Accessibility,
    SettingCategory.Advanced,
    SettingCategory.About
)

private fun iconForCategory(category: SettingCategory): ImageVector = when (category) {
    SettingCategory.General -> Icons.Default.Settings
    SettingCategory.Navigation -> Icons.Default.Tune
    SettingCategory.Appearance -> Icons.Default.Palette
    SettingCategory.Review -> Icons.Default.School
    SettingCategory.Browser -> Icons.Default.Language
    SettingCategory.Media -> Icons.Default.Movie
    SettingCategory.Statistics -> Icons.Default.Insights
    SettingCategory.History -> Icons.Default.History
    SettingCategory.Activity -> Icons.Default.Schedule
    SettingCategory.ImportExport -> Icons.Default.FileUpload
    SettingCategory.Sync -> Icons.Default.Sync
    SettingCategory.Updates -> Icons.Default.SystemUpdate
    SettingCategory.Plugins -> Icons.Default.Extension
    SettingCategory.Accessibility -> Icons.Default.Visibility
    SettingCategory.Advanced -> Icons.Default.AutoAwesome
    SettingCategory.About -> Icons.Default.Info
}

private fun describeCategory(category: SettingCategory): String = when (category) {
    SettingCategory.General -> "App-wide behavior and startup"
    SettingCategory.Navigation -> "Sidebar, compact dock and bubble launcher"
    SettingCategory.Appearance -> "Colors, base mode and theme"
    SettingCategory.Review -> "Session behavior, limits and grading"
    SettingCategory.Browser -> "Card browsing preferences"
    SettingCategory.Media -> "Playback backends, subtitles, mining and the media library"
    SettingCategory.Statistics -> "Dashboard range and goals"
    SettingCategory.History -> "Activity log retention"
    SettingCategory.Activity -> "Study-time tracking, AFK detection and the AFK rain"
    SettingCategory.ImportExport -> "Transfers and conflict policy"
    SettingCategory.Sync -> "Synchronization schedule"
    SettingCategory.Updates -> "Release channel and update checks"
    SettingCategory.Plugins -> "Extensions and automation"
    SettingCategory.Accessibility -> "Assistive options"
    SettingCategory.Advanced -> "Developer and diagnostics"
    SettingCategory.About -> "Version, credits and licenses"
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
        // About is a static section (no settings defs) but must stay in the rail.
        category == SettingCategory.About ||
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

    // The Theme Studio IS the Appearance category — a full live editor, not
    // a link that navigates away. It owns its own scrolling, so it renders
    // in place of the card grid (and still honors settings search below).
    if (!searching && selected == SettingCategory.Appearance) {
        Box(modifier.fillMaxWidth().fillMaxHeight()) {
            ThemeStudioView(state)
        }
        return
    }

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
        } else if (selected == SettingCategory.About) {
            AboutSettingsSection(state)
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

            if (selected == SettingCategory.General) {
                OnboardingLink(state)
                TutorialLink(state)
            }

            if (selected == SettingCategory.Media) {
                MediaSettingsCard(state)
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

/** Backend diagnostics + test actions for the Media settings category. */
@Composable
private fun MediaSettingsCard(state: AppState) {
    val sc = surfaceColors()
    val ac = accent()
    val probes = remember { state.media.probeBackends() }
    val ffmpegAvailable = remember { MediaCapture.ffmpegAvailable }

    DsCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(DsSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(DsRadius.Md))
                        .background(ac.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = ac.primary, modifier = Modifier.size(24.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Playback backends",
                        color = sc.textPrimary,
                        fontSize = DsType.BodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Kaiteyo uses VLC, mpv or the built-in audio engine to play media. Install a backend to unlock video playback inside the workspace.",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }

            probes.forEach { probe ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = probe.kind.name,
                            color = sc.textPrimary,
                            fontSize = DsType.Body,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = probe.version.ifBlank { probe.message },
                            color = sc.textMuted,
                            fontSize = DsType.Caption,
                            maxLines = 1
                        )
                    }
                    DsBadge(
                        text = probe.statusLabel,
                        tint = if (probe.available) successColor() else errorColor()
                    )
                    Spacer(Modifier.width(DsSpacing.Sm))
                    DsButton(
                        text = "Re-test",
                        kind = DsButtonKind.Ghost,
                        compact = true,
                        onClick = {
                            when (probe.kind) {
                                BackendKind.Vlc -> { state.media.backends.refreshVlc() }
                                BackendKind.Mpv -> { state.media.backends.refreshMpv() }
                                else -> Unit
                            }
                            state.toastHost.show("Re-probed ${probe.kind.name}", kind = ToastKind.Info)
                        }
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Audio capture (ffmpeg)",
                        color = sc.textPrimary,
                        fontSize = DsType.Body,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (ffmpegAvailable) "Installed — video audio clips can be extracted"
                        else "Not found — audio clips still work for WAV/AIFF sources",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
                DsBadge(text = if (ffmpegAvailable) "Installed" else "Not installed", tint = if (ffmpegAvailable) successColor() else warningColor())
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Media cache",
                        color = sc.textPrimary,
                        fontSize = DsType.Body,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${MediaCapture.cacheFileCount()} files · ${MediaEngine.formatTime(MediaCapture.cacheSizeBytes() / 1000)} of screenshots and audio clips",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
                DsButton(
                    text = "Clear cache",
                    kind = DsButtonKind.Ghost,
                    compact = true,
                    onClick = {
                        val removed = MediaCapture.clearCache()
                        state.toastHost.show("Cleared $removed cached media files", kind = ToastKind.Info)
                    }
                )
            }
        }
    }
}

/**
 * Product tutorial entry point: start the full tour, or replay a single
 * chapter. Completion per chapter persists (Settings → tutorial.completed).
 */
@Composable
private fun TutorialLink(state: AppState) {
    val sc = surfaceColors()
    val ac = accent()
    DsCard(
        onClick = { state.requestTutorial() },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
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
                        text = "Product tutorial",
                        color = sc.textPrimary,
                        fontSize = DsType.BodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "A guided tour of navigation, study, writing, browse, media, mining and stats — with live previews.",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
                DsButton(
                    text = "Start tutorial",
                    icon = Icons.Default.ArrowForward,
                    onClick = { state.requestTutorial() }
                )
            }
            // Per-chapter replay — completed chapters carry a check.
            Text(
                text = "REPLAY A CHAPTER",
                color = sc.textMuted,
                fontSize = DsType.Caption,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            tutorialChapters
                .filter { it.id != "welcome" && it.id != "done" }
                .chunked(2)
                .forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                        pair.forEach { chapter: TutorialChapter ->
                            ChapterReplayRow(state, chapter, Modifier.weight(1f))
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
        }
    }
}

@Composable
private fun ChapterReplayRow(state: AppState, chapter: TutorialChapter, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val done = state.tutorialChapterComplete(chapter.id)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(
                when {
                    hovered -> sc.surfaceInteractive
                    else -> sc.surface
                }
            )
            .clickable(interactionSource = interaction, indication = null) {
                state.tutorialRequested = true
                // Replay jumps straight to the requested chapter.
                TutorialReplayTarget.value = chapter.id
            }
            .hoverable(interaction)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Icon(
            chapter.icon,
            contentDescription = null,
            tint = if (done) ac.primary else sc.textSecondary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = chapter.title,
            color = sc.textPrimary,
            fontSize = DsType.Body,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        if (done) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = ac.primary,
                modifier = Modifier.size(14.dp)
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

// ============================================
// ABOUT — version, credits, acknowledgements
// and licenses, revamped for the settings page.
// ============================================

private const val GITHUB_URL = "https://github.com/ValiantZippu/Kaiteyo"
private const val DOCS_URL = "https://github.com/ValiantZippu/Kaiteyo#readme"
private const val ISSUES_URL = "https://github.com/ValiantZippu/Kaiteyo/issues"

private fun openUrl(url: String) {
    runCatching { java.awt.Desktop.getDesktop().browse(java.net.URI(url)) }
}

private data class AboutCredit(val name: String, val detail: String, val license: String)

private val aboutDataSources = listOf(
    AboutCredit("KanjiVG", "Stroke order data", "CC-BY-SA-3.0"),
    AboutCredit("KanjiDic2", "Kanji dictionary data", "CC-BY-SA-3.0"),
    AboutCredit("JMdict / JMnedict", "Vocabulary dictionary data", "CC-BY-SA-4.0"),
    AboutCredit("KANJIDIC", "Readings and meanings", "CC-BY-SA-3.0")
)

private val aboutThirdParty = listOf(
    AboutCredit("Kotlin", "Language", "Apache-2.0"),
    AboutCredit("Compose Multiplatform", "UI framework", "Apache-2.0"),
    AboutCredit("Ktor", "HTTP server (local API)", "Apache-2.0"),
    AboutCredit("kotlinx.coroutines", "Structured concurrency", "Apache-2.0"),
    AboutCredit("kotlinx.serialization", "Type-safe persistence", "Apache-2.0"),
    AboutCredit("kotlinx.datetime", "Date & time handling", "Apache-2.0"),
    AboutCredit("SQLDelight", "Type-safe SQL", "Apache-2.0"),
    AboutCredit("sqlite-jdbc", "Embedded database driver", "Apache-2.0"),
    AboutCredit("Material Icons", "Iconography", "Apache-2.0")
)

@Composable
private fun AboutSettingsSection(state: AppState) {
    val sc = surfaceColors()
    val ac = accent()
    val appVersion = remember { currentAppVersion() }

    Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
        // App identity + quick resources
        DsCard(elevated = true) {
            Column(
                Modifier.padding(DsSpacing.Xl).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
            ) {
                // The real Kaiteyo mark — centralized brand asset, not a "K".
                BrandMark(modifier = Modifier.size(72.dp))
                Text("Kaiteyo", color = sc.textPrimary, fontSize = DsType.Display, fontWeight = FontWeight.Bold)
                Text("Kanji study, rethought.", color = sc.textMuted, fontSize = DsType.BodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DsBadge(text = "v${appVersion.versionName}", tint = sc.textPrimary)
                    DsBadge(text = "MIT License", tint = Color(0xFFC2FC8B))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    DsButton(text = "GitHub", icon = Icons.Default.ArrowForward, kind = DsButtonKind.Ghost, compact = true, onClick = { openUrl(GITHUB_URL) })
                    DsButton(text = "Documentation", kind = DsButtonKind.Ghost, compact = true, onClick = { openUrl(DOCS_URL) })
                    DsButton(text = "Report an issue", kind = DsButtonKind.Ghost, compact = true, onClick = { openUrl(ISSUES_URL) })
                }
            }
        }

        // Live workspace stats
        DsCard {
            Row(
                Modifier.padding(DsSpacing.Xl).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
            ) {
                AboutStat("Cards", state.cards.size.toString(), Modifier.weight(1f))
                AboutStat("Decks", state.library.decks.size.toString(), Modifier.weight(1f))
                AboutStat("Study time", state.formatDuration(state.totalStudyTime()), Modifier.weight(1f))
                AboutStat("Reviews", state.totalReviews().toString(), Modifier.weight(1f))
            }
        }

        // Contributors
        AboutCreditCard(
            title = "Contributors",
            subtitle = "The people behind Kaiteyo",
            rows = listOf(
                Triple("syt0r", "Founder & Lead Developer", "Architecture, core engine, desktop UI"),
                Triple("Community", "Contributors & Testers", "Bug reports, feature ideas, translations")
            )
        )

        // Language data acknowledgements
        AboutCreditCard(
            title = "Language data",
            subtitle = "Open datasets that power the dictionaries and stroke data",
            rows = aboutDataSources.map { Triple(it.name, it.detail, it.license) }
        )

        // Third-party software
        AboutCreditCard(
            title = "Third-party software",
            subtitle = "Open-source libraries Kaiteyo is built on",
            rows = aboutThirdParty.map { Triple(it.name, it.detail, it.license) }
        )

        // License summary
        DsCard {
            Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsSectionHeader(
                    title = "License",
                    subtitle = "Kaiteyo is free and open-source software"
                )
                Text(
                    text = "Copyright (c) 2024-2026 syt0r. Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files, to deal in the Software without restriction. See the full MIT license text on GitHub.",
                    color = sc.textSecondary,
                    fontSize = DsType.Body
                )
                Text(
                    text = "Dictionary data (KanjiVG, KanjiDic2, JMdict, KANJIDIC) is provided under Creative Commons licenses as noted above; all such content remains the property of its respective authors.",
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }
    }
}

@Composable
private fun AboutStat(label: String, value: String, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = sc.textPrimary, fontSize = DsType.Title, fontWeight = FontWeight.Bold)
        Text(label, color = sc.textMuted, fontSize = DsType.Caption)
    }
}

@Composable
private fun AboutCreditCard(
    title: String,
    subtitle: String,
    rows: List<Triple<String, String, String>>
) {
    val sc = surfaceColors()
    DsCard {
        Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            DsSectionHeader(title = title, subtitle = subtitle)
            rows.forEach { (name, detail, license) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = DsSpacing.Xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(name, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium)
                        Text(detail, color = sc.textMuted, fontSize = DsType.Caption)
                    }
                    DsBadge(text = license, tint = sc.textMuted)
                }
            }
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
