package ua.syt0r.kanji.desktop.ui.themes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsDialog
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsProgressBar
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSelect
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsStatTile
import ua.syt0r.kanji.desktop.designsystem.DsTabRow
import ua.syt0r.kanji.desktop.designsystem.DsTextArea
import ua.syt0r.kanji.desktop.designsystem.DsTextButton
import ua.syt0r.kanji.desktop.designsystem.DsTextField
import ua.syt0r.kanji.desktop.designsystem.DsToggle
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.successColor
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.designsystem.warningColor
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.theming.KaiteyoTheme
import ua.syt0r.kanji.desktop.engine.theming.ThemeAnimation
import ua.syt0r.kanji.desktop.engine.theming.ThemeColors
import ua.syt0r.kanji.desktop.engine.theming.ThemeGradient
import ua.syt0r.kanji.desktop.engine.theming.ThemeGradientStop
import ua.syt0r.kanji.desktop.engine.theming.ThemeManager
import ua.syt0r.kanji.desktop.engine.theming.ThemeSerializer
import ua.syt0r.kanji.desktop.engine.theming.colorToHex
import ua.syt0r.kanji.desktop.engine.theming.colorToRgbHex
import ua.syt0r.kanji.desktop.engine.theming.hexToColor
import ua.syt0r.kanji.desktop.engine.theming.hsvToColor
import ua.syt0r.kanji.desktop.engine.theming.relativeLuminance
import ua.syt0r.kanji.desktop.engine.theming.rgbChannels
import ua.syt0r.kanji.desktop.engine.theming.rgbToHsv
import ua.syt0r.kanji.desktop.engine.transfer.TransferFilePicker
import ua.syt0r.kanji.desktop.model.ToastKind
import ua.syt0r.kanji.presentation.common.resources.brand.BrandMark
import kotlin.math.roundToInt

// ============================================
// THEME STUDIO
// Kaiteyo's flagship customization surface.
// Every control writes straight into the active
// theme through ThemeManager, so the whole app
// live-updates as you drag — no Apply step, no
// dead sliders. Includes a full theme library
// (presets + custom), HSV/RGB/hex color picker,
// typography, display scaling presets, component
// sizing, animation and effects, accessibility
// shortcuts, and portable JSON import/export.
// ============================================

private val STUDIO_TABS = listOf("Colors", "Typography", "Scaling", "Animation", "Effects", "Accessibility", "Preview")

@Composable
fun ThemeStudioView(state: AppState) {
    val manager = state.themeManager
    var selectedTab by remember { mutableStateOf(0) }
    var showJson by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize().padding(DsSpacing.Lg)) {
        val wide = maxWidth >= 760.dp
        if (wide) {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
            ) {
                ThemeLibraryRail(state, modifier = Modifier.width(272.dp).fillMaxHeight())
                ThemeEditor(
                    state = state,
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    showJson = showJson,
                    onToggleJson = { showJson = !showJson },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                ThemeLibraryStrip(state)
                ThemeEditor(
                    state = state,
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    showJson = showJson,
                    onToggleJson = { showJson = !showJson },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ------------------------------------------------------------
// Library rail
// ------------------------------------------------------------

@Composable
private fun ThemeLibraryRail(state: AppState, modifier: Modifier = Modifier) {
    val manager = state.themeManager
    val activeId = manager.activeThemeId
    val sc = surfaceColors()

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Theme Library",
                color = sc.textPrimary,
                fontSize = DsType.BodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                DsIconButton(
                    icon = Icons.Default.FileUpload,
                    onClick = { importThemeFile(state) },
                    contentDescription = "Import theme"
                )
                DsIconButton(
                    icon = Icons.Default.FileDownload,
                    onClick = { exportThemeFile(state) },
                    contentDescription = "Export theme"
                )
            }
        }

        RailLabel("Presets")
        manager.presets.forEach { theme ->
            ThemeLibraryRow(
                theme = theme,
                active = theme.id == activeId,
                onClick = { state.applyTheme(theme.id) }
            )
        }

        if (manager.customThemes.isNotEmpty()) {
            RailLabel("Custom")
            manager.customThemes.forEach { theme ->
                ThemeLibraryRow(
                    theme = theme,
                    active = theme.id == activeId,
                    onClick = { state.applyTheme(theme.id) },
                    actions = {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            DsIconButton(
                                icon = Icons.Default.Star,
                                onClick = { manager.toggleFavorite(theme.id) },
                                contentDescription = "Favorite",
                                tint = if (theme.favorite) Color(0xFFFFD93D) else sc.textMuted,
                                size = 28.dp
                            )
                            DsIconButton(
                                icon = Icons.Default.ContentCopy,
                                onClick = {
                                    val copyId = manager.duplicate(theme.id)
                                    state.applyTheme(copyId)
                                    state.activityLog.record(ActivityCategory.Theme, "Duplicated theme '${theme.name}'")
                                    state.toastHost.show("Theme duplicated", kind = ToastKind.Success)
                                },
                                contentDescription = "Duplicate",
                                size = 28.dp
                            )
                            DsIconButton(
                                icon = Icons.Default.Delete,
                                onClick = { manager.deleteTheme(theme.id) },
                                contentDescription = "Delete",
                                tint = Color(0xFFFF5D5D),
                                size = 28.dp
                            )
                        }
                    }
                )
            }
        }
        Spacer(Modifier.height(DsSpacing.Sm))
    }
}

@Composable
private fun RailLabel(text: String) {
    val sc = surfaceColors()
    Text(
        text = text.uppercase(),
        color = sc.textMuted,
        fontSize = DsType.Caption,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = DsSpacing.Xs)
    )
}

@Composable
private fun ThemeLibraryRow(
    theme: KaiteyoTheme,
    active: Boolean,
    onClick: () -> Unit,
    actions: @Composable () -> Unit = {}
) {
    val sc = surfaceColors()
    val ac = accent()
    DsCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (active) Modifier.border(1.5.dp, ac.primary, RoundedCornerShape(16.dp)) else Modifier
            )
    ) {
        Column(Modifier.padding(DsSpacing.Sm), verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
            ) {
                ThemeSwatches(theme)
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = theme.name,
                            color = sc.textPrimary,
                            fontSize = DsType.Body,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1
                        )
                        if (theme.favorite) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Favorite",
                                tint = Color(0xFFFFD93D),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Text(
                        text = "${theme.author} · v${theme.version}",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
                if (active) {
                    Icon(Icons.Default.Check, contentDescription = "Active", tint = ac.primary, modifier = Modifier.size(16.dp))
                }
            }
            actions()
        }
    }
}

@Composable
private fun ThemeSwatches(theme: KaiteyoTheme) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf(theme.colors.background, theme.colors.surface, theme.colors.primary, theme.colors.secondary).forEach { hex ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(hexToColor(hex))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun ThemeLibraryStrip(state: AppState) {
    val manager = state.themeManager
    val activeId = manager.activeThemeId
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        manager.allThemes.forEach { theme ->
            val active = theme.id == activeId
            val sc = surfaceColors()
            val ac = accent()
            DsCard(onClick = { state.applyTheme(theme.id) }) {
                Row(
                    Modifier.padding(DsSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                ) {
                    ThemeSwatches(theme)
                    Text(
                        text = theme.name,
                        color = if (active) ac.primary else sc.textSecondary,
                        fontSize = DsType.Label,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------
// Editor
// ------------------------------------------------------------

@Composable
private fun ThemeEditor(
    state: AppState,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    showJson: Boolean,
    onToggleJson: () -> Unit,
    modifier: Modifier = Modifier
) {
    val manager = state.themeManager
    val theme = manager.activeTheme

    Column(modifier, verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
        ThemeEditorHeader(state, showJson = showJson, onToggleJson = onToggleJson)
        if (showJson) {
            ThemeJsonCard(manager)
        }
        DsTabRow(
            tabs = STUDIO_TABS,
            selectedIndex = selectedTab,
            onSelect = onSelectTab,
            modifier = Modifier.fillMaxWidth()
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> ThemeColorsEditor(theme, manager)
                1 -> ThemeTypographyEditor(theme, manager)
                2 -> ThemeScalingEditor(theme, manager)
                3 -> ThemeAnimationEditor(state, theme, manager)
                4 -> ThemeEffectsEditor(theme, manager)
                5 -> ThemeAccessibilityEditor(state, theme, manager)
                else -> ThemePreviewTab(theme, manager)
            }
        }
    }
}

@Composable
private fun ThemeEditorHeader(state: AppState, showJson: Boolean, onToggleJson: () -> Unit) {
    val manager = state.themeManager
    val theme = manager.activeTheme
    val custom = !manager.isPreset(theme.id)
    val sc = surfaceColors()
    val ac = accent()
    var renameOpen by remember { mutableStateOf(false) }

    DsCard {
        Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
                ThemeSwatches(theme)
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                        Text(
                            text = theme.name,
                            color = sc.textPrimary,
                            fontSize = DsType.Title,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (theme.id == manager.activeThemeId) {
                            Text(
                                text = "ACTIVE",
                                color = ac.primary,
                                fontSize = DsType.Caption,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ac.primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (custom) {
                            Text(
                                text = "CUSTOM",
                                color = sc.textSecondary,
                                fontSize = DsType.Caption,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(sc.surfaceInteractive)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = theme.description.ifBlank { "No description" },
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                    Text(
                        text = "by ${theme.author} · version ${theme.version} · ${theme.baseMode.uppercase()}",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DsIconButton(
                    icon = Icons.Default.Star,
                    onClick = { manager.toggleFavorite(theme.id) },
                    contentDescription = "Favorite",
                    tint = if (theme.favorite) Color(0xFFFFD93D) else sc.textMuted
                )
                DsIconButton(
                    icon = Icons.Default.Edit,
                    onClick = { renameOpen = true },
                    contentDescription = "Rename theme",
                    enabled = custom
                )
                DsIconButton(
                    icon = Icons.Default.ContentCopy,
                    onClick = {
                        val copyId = manager.duplicate(theme.id)
                        state.applyTheme(copyId)
                        state.activityLog.record(ActivityCategory.Theme, "Duplicated theme '${theme.name}'")
                        state.toastHost.show("Theme duplicated", kind = ToastKind.Success)
                    },
                    contentDescription = "Duplicate theme"
                )
                DsIconButton(
                    icon = Icons.Default.RestartAlt,
                    onClick = {
                        manager.resetTheme(theme.id)
                        state.activityLog.record(ActivityCategory.Theme, "Reset theme '${theme.name}'")
                        state.toastHost.show("Theme reset to its source values", kind = ToastKind.Info)
                    },
                    contentDescription = "Reset theme"
                )
                DsIconButton(
                    icon = Icons.Default.FileDownload,
                    onClick = { exportThemeFile(state) },
                    contentDescription = "Export theme to file"
                )
                DsIconButton(
                    icon = Icons.Default.FileUpload,
                    onClick = { importThemeFile(state) },
                    contentDescription = "Import theme from file"
                )
                Spacer(Modifier.weight(1f))
                DsTextButton(text = if (showJson) "Hide JSON" else "JSON", onClick = onToggleJson)
            }
        }
    }
    if (renameOpen) {
        RenameThemeDialog(state, onDismiss = { renameOpen = false })
    }
}

@Composable
private fun ThemeJsonCard(manager: ThemeManager) {
    val theme = manager.activeTheme
    var jsonText by remember(theme.id) { mutableStateOf(ThemeSerializer.export(theme)) }
    DsCard {
        Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            DsSectionHeader(
                title = "Theme JSON",
                subtitle = "Lossless portable definition — paste or share anywhere"
            )
            DsTextArea(value = jsonText, onValueChange = { jsonText = it }, height = 220.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                DsButton(
                    text = "Import this JSON",
                    onClick = {
                        if (manager.importJson(jsonText)) {
                            jsonText = ThemeSerializer.export(manager.activeTheme)
                        }
                    }
                )
                DsTextButton(text = "Refresh from theme", onClick = { jsonText = ThemeSerializer.export(theme) })
            }
        }
    }
}

// ------------------------------------------------------------
// Colors
// ------------------------------------------------------------

@Composable
private fun ThemeColorsEditor(theme: KaiteyoTheme, manager: ThemeManager) {
    val c = theme.colors
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        GradientSection(theme, manager)
        ColorGroup("Surfaces") {
            ColorField("Background", c.background) { hex -> manager.updateActiveColors { it.copy(background = hex) } }
            ColorField("Surface", c.surface) { hex -> manager.updateActiveColors { it.copy(surface = hex) } }
            ColorField("Surface elevated", c.surfaceElevated) { hex -> manager.updateActiveColors { it.copy(surfaceElevated = hex) } }
            ColorField("Surface interactive", c.surfaceInteractive) { hex -> manager.updateActiveColors { it.copy(surfaceInteractive = hex) } }
            ColorField("Border", c.border) { hex -> manager.updateActiveColors { it.copy(border = hex) } }
            ColorField("Window", c.window) { hex -> manager.updateActiveColors { it.copy(window = hex) } }
            ColorField("Sidebar", c.sidebar) { hex -> manager.updateActiveColors { it.copy(sidebar = hex) } }
            ColorField("Navigation", c.navigation) { hex -> manager.updateActiveColors { it.copy(navigation = hex) } }
        }
        ColorGroup("Text") {
            ColorField("Text primary", c.textPrimary) { hex -> manager.updateActiveColors { it.copy(textPrimary = hex) } }
            ColorField("Text secondary", c.textSecondary) { hex -> manager.updateActiveColors { it.copy(textSecondary = hex) } }
            ColorField("Text muted", c.textMuted) { hex -> manager.updateActiveColors { it.copy(textMuted = hex) } }
            ColorField("Text inverse", c.textInverse) { hex -> manager.updateActiveColors { it.copy(textInverse = hex) } }
            ColorField("Links", c.link) { hex -> manager.updateActiveColors { it.copy(link = hex) } }
        }
        ColorGroup("Brand") {
            ColorField("Primary", c.primary) { hex -> manager.updateActiveColors { it.copy(primary = hex) } }
            ColorField("Primary dark", c.primaryDark) { hex -> manager.updateActiveColors { it.copy(primaryDark = hex) } }
            ColorField("Secondary", c.secondary) { hex -> manager.updateActiveColors { it.copy(secondary = hex) } }
            ColorField("Secondary dark", c.secondaryDark) { hex -> manager.updateActiveColors { it.copy(secondaryDark = hex) } }
            ColorField("Tertiary", c.tertiary) { hex -> manager.updateActiveColors { it.copy(tertiary = hex) } }
            ColorField("On primary", c.onPrimary) { hex -> manager.updateActiveColors { it.copy(onPrimary = hex) } }
            ColorField("On secondary", c.onSecondary) { hex -> manager.updateActiveColors { it.copy(onSecondary = hex) } }
        }
        ColorGroup("Semantic") {
            ColorField("Error", c.error) { hex -> manager.updateActiveColors { it.copy(error = hex) } }
            ColorField("Success", c.success) { hex -> manager.updateActiveColors { it.copy(success = hex) } }
            ColorField("Warning", c.warning) { hex -> manager.updateActiveColors { it.copy(warning = hex) } }
            ColorField("Info", c.info) { hex -> manager.updateActiveColors { it.copy(info = hex) } }
        }
        ColorGroup("Interaction") {
            ColorField("Hover", c.hover) { hex -> manager.updateActiveColors { it.copy(hover = hex) } }
            ColorField("Selection", c.selection) { hex -> manager.updateActiveColors { it.copy(selection = hex) } }
        }
        ColorGroup("Components") {
            ColorField("Dialog", c.dialog) { hex -> manager.updateActiveColors { it.copy(dialog = hex) } }
            ColorField("Popup", c.popup) { hex -> manager.updateActiveColors { it.copy(popup = hex) } }
            ColorField("Launchpad", c.launchpad) { hex -> manager.updateActiveColors { it.copy(launchpad = hex) } }
            ColorField("Bubble", c.bubble) { hex -> manager.updateActiveColors { it.copy(bubble = hex) } }
        }
        ColorGroup("Data & shadows") {
            ColorField("Charts", c.charts) { hex -> manager.updateActiveColors { it.copy(charts = hex) } }
            ColorField("Heatmap", c.heatmap) { hex -> manager.updateActiveColors { it.copy(heatmap = hex) } }
            ColorField("Statistics", c.statistics) { hex -> manager.updateActiveColors { it.copy(statistics = hex) } }
            ColorField("Shadow", c.shadow) { hex -> manager.updateActiveColors { it.copy(shadow = hex) } }
        }
    }
}

@Composable
private fun ColorGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val sc = surfaceColors()
    DsCard {
        Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Text(
                text = title.uppercase(),
                color = sc.textMuted,
                fontSize = DsType.Caption,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun ColorField(label: String, value: String, onChange: (String) -> Unit) {
    val sc = surfaceColors()
    var pickerOpen by remember { mutableStateOf(false) }
    var text by remember(value) { mutableStateOf(value) }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(hexToColor(value))
                .border(1.dp, sc.border, RoundedCornerShape(7.dp))
        )
        Text(
            text = label,
            color = sc.textSecondary,
            fontSize = DsType.Body,
            modifier = Modifier.width(150.dp)
        )
        DsTextField(
            value = text,
            onValueChange = { raw ->
                val clean = raw.uppercase().replace("[^0-9A-F#]".toRegex(), "").take(9)
                text = clean
                if (clean.matches(Regex("^#[0-9A-F]{6}$")) || clean.matches(Regex("^#[0-9A-F]{8}$"))) {
                    onChange(clean)
                }
            },
            singleLine = true,
            modifier = Modifier.width(140.dp)
        )
        DsIconButton(
            icon = Icons.Default.Palette,
            onClick = { pickerOpen = true },
            contentDescription = "Pick color",
            size = 32.dp
        )
    }
    if (pickerOpen) {
        ColorPickerDialog(
            initialHex = value,
            onDismiss = { pickerOpen = false },
            onPick = {
                text = it
                onChange(it)
                pickerOpen = false
            }
        )
    }
}

@Composable
private fun ColorPickerDialog(initialHex: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val sc = surfaceColors()
    var color by remember(initialHex) { mutableStateOf(hexToColor(initialHex)) }
    var hexText by remember(initialHex) { mutableStateOf(hexForDisplay(color)) }
    val (h, s, v) = rgbToHsv(color)
    val (r, g, b) = rgbChannels(color)

    fun apply(updated: Color) {
        color = updated
        hexText = hexForDisplay(updated)
    }

    DsDialog(title = "Pick color", onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(DsSpacing.Md))
                    .background(color)
                    .border(1.dp, sc.border, RoundedCornerShape(DsSpacing.Md))
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Current", color = sc.textMuted, fontSize = DsType.Caption)
                Text(initialHex.uppercase(), color = sc.textSecondary, fontSize = DsType.Label)
                Spacer(Modifier.height(4.dp))
                Text("New", color = sc.textMuted, fontSize = DsType.Caption)
                Text(hexText, color = sc.textPrimary, fontSize = DsType.Label, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(DsSpacing.Lg))

        PickerSlider("Hue", h, 0f..360f, "%.0f") { apply(hsvToColor(it, s, v)) }
        PickerSlider("Saturation", s, 0f..1f, "%.2f") { apply(hsvToColor(h, it, v)) }
        PickerSlider("Value", v, 0f..1f, "%.2f") { apply(hsvToColor(h, s, it)) }
        PickerSlider("Opacity", color.alpha, 0f..1f, "%.2f") { a -> apply(color.copy(alpha = a)) }
        Spacer(Modifier.height(DsSpacing.Md))

        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
            RgbField("R", r) { apply(Color(it / 255f, color.green, color.blue, 1f)) }
            RgbField("G", g) { apply(Color(color.red, it / 255f, color.blue, 1f)) }
            RgbField("B", b) { apply(Color(color.red, color.green, it / 255f, 1f)) }
            DsTextField(
                value = hexText,
                onValueChange = { raw ->
                    val clean = raw.uppercase().replace("[^0-9A-F#]".toRegex(), "").take(9)
                    hexText = clean
                    when {
                        clean.matches(Regex("^#[0-9A-F]{6}$")) -> color = hexToColor(clean)
                        clean.matches(Regex("^#[0-9A-F]{8}$")) -> color = hexToColor(clean)
                    }
                },
                singleLine = true,
                modifier = Modifier.width(150.dp)
            )
        }
        Spacer(Modifier.height(DsSpacing.Xl))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
        ) {
            DsTextButton(text = "Cancel", onClick = onDismiss)
            DsButton(text = "Apply", onClick = { onPick(hexForDisplay(color)) })
        }
    }
}

@Composable
private fun PickerSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, format: String, onChanged: (Float) -> Unit) {
    val sc = surfaceColors()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
        Text(label, color = sc.textSecondary, fontSize = DsType.Label, modifier = Modifier.width(80.dp))
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChanged,
            valueRange = range,
            modifier = Modifier.weight(1f)
        )
        Text(String.format(format, value), color = sc.textMuted, fontSize = DsType.Caption, modifier = Modifier.width(44.dp))
    }
}

@Composable
private fun RgbField(label: String, value: Int, onChange: (Int) -> Unit) {
    val sc = surfaceColors()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = sc.textMuted, fontSize = DsType.Caption, fontWeight = FontWeight.SemiBold)
        var text by remember(value) { mutableStateOf(value.toString()) }
        DsTextField(
            value = text,
            onValueChange = { raw ->
                val filtered = raw.filter { it.isDigit() }.take(3)
                text = filtered
                onChange(filtered.toIntOrNull()?.coerceIn(0, 255) ?: 0)
            },
            singleLine = true,
            modifier = Modifier.width(56.dp)
        )
    }
}

// ------------------------------------------------------------
// Gradient editor
// ------------------------------------------------------------

@Composable
private fun GradientSection(theme: KaiteyoTheme, manager: ThemeManager) {
    val g = theme.gradient
    val sc = surfaceColors()
    EditorCard("Gradient", "Brand gradients across navigation, progress bars, hero surfaces and previews") {
        SettingToggle(
            label = "Gradient enabled",
            description = "Off → gradients follow Primary/Secondary automatically",
            checked = g.enabled,
            onCheckedChange = { on -> manager.updateActiveGradient { it.copy(enabled = on) } }
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
            Text("Type", color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.width(80.dp))
            DsSelect(
                selected = g.type,
                options = listOf("linear", "radial", "sweep"),
                onSelected = { type -> manager.updateActiveGradient { it.copy(type = type) } },
                labelOf = { it.replaceFirstChar { c -> c.uppercaseChar() } },
                modifier = Modifier.width(160.dp)
            )
        }
        if (g.type == "linear") {
            StudioSlider("Angle", g.angle, 0f..360f, "%.0f°") { a -> manager.updateActiveGradient { it.copy(angle = a) } }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Stops", color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
            Text("${g.stops.size}", color = sc.textMuted, fontSize = DsType.Caption, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = g.stops.size.toFloat(),
            onValueChange = { count ->
                val n = count.roundToInt().coerceIn(2, 8)
                manager.updateActiveGradient { grad ->
                    val current = grad.stops
                    val resized = if (n < current.size) current.take(n)
                    else current + List(n - current.size) { current.lastOrNull() ?: ThemeGradientStop() }
                    grad.copy(
                        stops = resized.mapIndexed { i, s -> s.copy(position = if (n <= 1) 0f else i / (n - 1f)) }
                    )
                }
            },
            valueRange = 2f..8f,
            steps = 5
        )
        g.stops.forEachIndexed { index, stop ->
            ColorField("Stop ${index + 1}", stop.color) { hex ->
                manager.updateActiveGradient { grad ->
                    grad.copy(stops = grad.stops.mapIndexed { i, s -> if (i == index) s.copy(color = hex) else s })
                }
            }
            StudioSlider("Position", stop.position, 0f..1f, "%.2f") { p ->
                manager.updateActiveGradient { grad ->
                    grad.copy(stops = grad.stops.mapIndexed { i, s -> if (i == index) s.copy(position = p) else s })
                }
            }
        }
        Text("Live swatch", color = sc.textSecondary, fontSize = DsType.Body)
        ThemeGradientBar(
            g,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(DsRadius.Md))
                .border(1.dp, sc.border, RoundedCornerShape(DsRadius.Md))
        )
        DsTextButton(
            text = "Reset to brand colors",
            onClick = {
                manager.updateActiveGradient {
                    it.copy(
                        type = "linear",
                        angle = 0f,
                        stops = listOf(
                            ThemeGradientStop(theme.colors.primary, 0f),
                            ThemeGradientStop(theme.colors.secondary, 1f)
                        )
                    )
                }
            }
        )
    }
}

/** Renders a theme gradient honoring type, angle and stop positions. */
@Composable
private fun ThemeGradientBar(gradient: ThemeGradient, modifier: Modifier = Modifier) {
    // (position, color) pairs — the Compose 1.8 gradient API takes stops as
    // pairs rather than a separate colors list + positions array.
    val stops = gradient.stops.sortedBy { it.position }.map { stop ->
        stop.position.coerceIn(0f, 1f) to hexToColor(stop.color)
    }
    val angleRad = gradient.angle * kotlin.math.PI.toFloat() / 180f
    Box(
        modifier = modifier.drawBehind {
            val dx = kotlin.math.sin(angleRad) * size.width / 2f
            val dy = -kotlin.math.cos(angleRad) * size.height / 2f
            val brush = when (gradient.type) {
                "radial" -> Brush.radialGradient(
                    colorStops = stops.toTypedArray(),
                    center = center,
                    radius = size.minDimension / 2f
                )
                "sweep" -> Brush.sweepGradient(colorStops = stops.toTypedArray(), center = center)
                else -> Brush.linearGradient(
                    colorStops = stops.toTypedArray(),
                    start = center - Offset(dx, dy),
                    end = center + Offset(dx, dy)
                )
            }
            drawRect(brush)
        }
    )
}

/** Picker output keeps 6-digit hex for opaque colors and 8-digit ARGB otherwise. */
private fun hexForDisplay(color: Color): String =
    if (color.alpha >= 0.999f) colorToRgbHex(color) else colorToHex(color)

// ------------------------------------------------------------
// Typography
// ------------------------------------------------------------

@Composable
private fun ThemeTypographyEditor(theme: KaiteyoTheme, manager: ThemeManager) {
    val t = theme.typography
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        EditorCard("Font family", "Applies across the whole application") {
            DsSelect(
                selected = t.fontFamily,
                options = listOf("system", "sans-serif", "serif", "monospace", "cursive"),
                onSelected = { family -> manager.updateActiveTypography { it.copy(fontFamily = family) } },
                labelOf = { it },
                modifier = Modifier.width(200.dp)
            )
        }
        EditorCard("Base size") {
            StudioSlider("Font size", t.fontSize, 0.6f..2f, "%.2f×") { v -> manager.updateActiveTypography { it.copy(fontSize = v) } }
            StudioSlider("Font scale", t.fontScale, 0.6f..2f, "%.2f×") { v -> manager.updateActiveTypography { it.copy(fontScale = v) } }
            StudioSlider("UI scale", t.uiScale, 0.6f..2f, "%.2f×") { v -> manager.updateActiveTypography { it.copy(uiScale = v) } }
            StudioSlider("Title scale", t.titleScale, 0.6f..2f, "%.2f×") { v -> manager.updateActiveTypography { it.copy(titleScale = v) } }
        }
        EditorCard("Style") {
            StudioSlider("Heading weight", t.headingWeight.toFloat(), 100f..900f, "%.0f") { v ->
                manager.updateActiveTypography { it.copy(headingWeight = (v / 100f).roundToInt() * 100) }
            }
            StudioSlider("Body weight", t.bodyWeight.toFloat(), 100f..900f, "%.0f") { v ->
                manager.updateActiveTypography { it.copy(bodyWeight = (v / 100f).roundToInt() * 100) }
            }
            StudioSlider("Line height", t.lineHeight, 0.8f..2f, "%.2f") { v -> manager.updateActiveTypography { it.copy(lineHeight = v) } }
            StudioSlider("Letter spacing", t.letterSpacing, -2f..4f, "%.1f sp") { v -> manager.updateActiveTypography { it.copy(letterSpacing = v) } }
        }
    }
}

// ------------------------------------------------------------
// Scaling & sizing
// ------------------------------------------------------------

@Composable
private fun ThemeScalingEditor(theme: KaiteyoTheme, manager: ThemeManager) {
    val s = theme.scaling
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        EditorCard("Display scale", "Browser-style zoom applied to the entire app") {
            val pct = (s.displayScale * 100).roundToInt()
            val presets = listOf(50, 75, 90, 100, 110, 125, 150, 175, 200)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
            ) {
                presets.forEach { preset ->
                    val selected = pct == preset
                    val ac = accent()
                    DsButton(
                        text = "$preset%",
                        onClick = { manager.updateActiveScaling { it.copy(displayScale = preset / 100f) } },
                        kind = if (selected) DsButtonKind.AccentTint else DsButtonKind.Ghost,
                        compact = true
                    )
                }
            }
            var custom by remember(pct) { mutableStateOf(pct.toString()) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("Custom", color = surfaceColors().textSecondary, fontSize = DsType.Body)
                DsTextField(
                    value = custom,
                    onValueChange = { raw ->
                        val filtered = raw.filter { it.isDigit() }.take(3)
                        custom = filtered
                        val value = filtered.toIntOrNull()
                        if (value != null && value in 25..400) {
                            manager.updateActiveScaling { it.copy(displayScale = value / 100f) }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.width(90.dp)
                )
                Text("%", color = surfaceColors().textMuted, fontSize = DsType.Body)
                Text(
                    "Current: $pct%",
                    color = if (pct == 100) accent().primary else surfaceColors().textSecondary,
                    fontSize = DsType.Caption
                )
            }
        }
        EditorCard("Component sizing") {
            StudioSlider("Button size", s.buttonSize, 0.5f..2f, "%.2f×") { v -> manager.updateActiveScaling { it.copy(buttonSize = v) } }
            StudioSlider("Icon size", s.iconSize, 0.5f..2f, "%.2f×") { v -> manager.updateActiveScaling { it.copy(iconSize = v) } }
            StudioSlider("Bubble size", s.bubbleSize, 0.5f..2f, "%.2f×") { v -> manager.updateActiveScaling { it.copy(bubbleSize = v) } }
            StudioSlider("Toolbar height", s.toolbarHeight, 0.5f..2f, "%.2f×") { v -> manager.updateActiveScaling { it.copy(toolbarHeight = v) } }
            StudioSlider("Window padding", s.windowPadding, 0.5f..2f, "%.2f×") { v -> manager.updateActiveScaling { it.copy(windowPadding = v) } }
            StudioSlider("Sidebar width", s.sidebarWidth, 0.5f..2f, "%.2f×") { v -> manager.updateActiveScaling { it.copy(sidebarWidth = v) } }
        }
        EditorCard("Spacing density") {
            StudioSlider("Spacing scale", theme.spacing.scale, 0.5f..2f, "%.2f×") { v -> manager.updateActiveSpacing { it.copy(scale = v) } }
            StudioSlider("Padding density", theme.spacing.padding, 0.5f..2f, "%.2f×") { v -> manager.updateActiveSpacing { it.copy(padding = v) } }
        }
        EditorCard("Corners") {
            StudioSlider("Radius multiplier", theme.corners.radiusMultiplier, 0.25f..3f, "%.2f×") { v -> manager.updateActiveCorners { it.copy(radiusMultiplier = v) } }
            DsSelect(
                selected = theme.corners.style,
                options = listOf("square", "rounded", "soft"),
                onSelected = { style -> manager.updateActiveCorners { it.copy(style = style) } },
                labelOf = { it },
                modifier = Modifier.width(200.dp)
            )
        }
    }
}

// ------------------------------------------------------------
// Animation
// ------------------------------------------------------------

@Composable
private fun ThemeAnimationEditor(state: AppState, theme: KaiteyoTheme, manager: ThemeManager) {
    val a = theme.animation
    var masterOn by remember(theme.id) {
        mutableStateOf(a.hoverEnabled || a.launchpadEnabled || a.sidebarEnabled || a.bubbleEnabled || a.themeTransitionEnabled)
    }
    var prevFlags by remember(theme.id) { mutableStateOf<ThemeAnimation?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        EditorCard("Master") {
            SettingToggle(
                label = "Animations enabled",
                description = "Toggles every animation category at once",
                checked = masterOn,
                onCheckedChange = { on ->
                    if (on) {
                        val restored = prevFlags
                        if (restored != null) {
                            manager.updateActiveAnimation {
                                it.copy(
                                    hoverEnabled = restored.hoverEnabled,
                                    launchpadEnabled = restored.launchpadEnabled,
                                    sidebarEnabled = restored.sidebarEnabled,
                                    bubbleEnabled = restored.bubbleEnabled,
                                    themeTransitionEnabled = restored.themeTransitionEnabled
                                )
                            }
                        }
                    } else {
                        prevFlags = a
                        manager.updateActiveAnimation {
                            it.copy(
                                hoverEnabled = false,
                                launchpadEnabled = false,
                                sidebarEnabled = false,
                                bubbleEnabled = false,
                                themeTransitionEnabled = false
                            )
                        }
                    }
                    masterOn = on
                }
            )
            StudioSlider("Animation speed", a.speed, 0.1f..3f, "%.2f×") { v -> manager.updateActiveAnimation { it.copy(speed = v) } }
            StudioSlider("Default duration", a.durationMs.toFloat(), 0f..2000f, "%.0f ms") { v -> manager.updateActiveAnimation { it.copy(durationMs = v.roundToInt()) } }
            SettingToggle("Reduced motion", "Disables all navigation and launcher motion", a.reducedMotion) { on ->
                manager.updateActiveAnimation { it.copy(reducedMotion = on) }
                // The settings key mirrors the theme so AppState's nav mirror stays in sync.
                state.settings.setBool("appearance.reduced-motion", on)
            }
        }
        EditorCard("Categories") {
            SettingToggle("Hover animation", "Buttons, cards and dock hover feedback", a.hoverEnabled) { on ->
                manager.updateActiveAnimation { it.copy(hoverEnabled = on) }
            }
            SettingToggle("Launchpad animation", "Scale-from-bubble open and close", a.launchpadEnabled) { on ->
                manager.updateActiveAnimation { it.copy(launchpadEnabled = on) }
            }
            SettingToggle("Sidebar animation", "Dock expand / collapse transitions", a.sidebarEnabled) { on ->
                manager.updateActiveAnimation { it.copy(sidebarEnabled = on) }
            }
            SettingToggle("Bubble animation", "Snapping springs and hover expansion", a.bubbleEnabled) { on ->
                manager.updateActiveAnimation { it.copy(bubbleEnabled = on) }
            }
            SettingToggle("Theme transition", "Animated color changes between themes", a.themeTransitionEnabled) { on ->
                manager.updateActiveAnimation { it.copy(themeTransitionEnabled = on) }
            }
        }
        EditorCard("Intensity") {
            StudioSlider("Blur strength", a.blurStrength, 0f..3f, "%.1f×") { v -> manager.updateActiveAnimation { it.copy(blurStrength = v) } }
            StudioSlider("Shadow strength", a.shadowStrength, 0f..3f, "%.1f×") { v -> manager.updateActiveAnimation { it.copy(shadowStrength = v) } }
        }
    }
}

// ------------------------------------------------------------
// Effects
// ------------------------------------------------------------

@Composable
private fun ThemeEffectsEditor(theme: KaiteyoTheme, manager: ThemeManager) {
    val e = theme.effects
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        EditorCard("Surfaces") {
            SettingToggle("Transparency", "Translucent panels and docks", e.transparency) { on ->
                manager.updateActiveEffects { it.copy(transparency = on) }
            }
            SettingToggle("Blur", "Backdrop blur behind floating surfaces", e.blur) { on ->
                manager.updateActiveEffects { it.copy(blur = on) }
            }
            SettingToggle("OLED", "Pure-black backgrounds", e.oled) { on ->
                manager.updateActiveEffects { it.copy(oled = on) }
            }
            SettingToggle("Material", "Material-style surface elevation", e.material) { on ->
                manager.updateActiveEffects { it.copy(material = on) }
            }
        }
        EditorCard("Glass") {
            StudioSlider("Glass opacity", e.glassOpacity, 0.3f..1f, "%.2f") { v -> manager.updateActiveEffects { it.copy(glassOpacity = v) } }
        }
    }
}

// ------------------------------------------------------------
// Accessibility
// ------------------------------------------------------------

@Composable
private fun ThemeAccessibilityEditor(state: AppState, theme: KaiteyoTheme, manager: ThemeManager) {
    val a = theme.animation
    val t = theme.typography
    val s = theme.scaling

    var highContrast by remember(theme.id) { mutableStateOf(false) }
    var hcPrev by remember(theme.id) { mutableStateOf<ThemeColors?>(null) }
    var largeFonts by remember(theme.id) { mutableStateOf(t.fontScale >= 1.2f) }
    var fontPrev by remember(theme.id) { mutableStateOf(t.fontScale) }
    var largeIcons by remember(theme.id) { mutableStateOf(s.iconSize >= 1.2f) }
    var iconPrev by remember(theme.id) { mutableStateOf(s.iconSize) }
    var bigBubble by remember(theme.id) { mutableStateOf(s.bubbleSize >= 1.15f) }
    var bubblePrev by remember(theme.id) { mutableStateOf(s.bubbleSize) }

    val bg = hexToColor(theme.colors.background)
    val dark = relativeLuminance(bg) < 0.5f

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        EditorCard("Contrast") {
            SettingToggle(
                label = "High contrast",
                description = if (dark) "Brightens borders and secondary text" else "Darkens borders and secondary text",
                checked = highContrast,
                onCheckedChange = { on ->
                    if (on) {
                        hcPrev = theme.colors
                        manager.updateActiveColors {
                            it.copy(
                                border = if (dark) "#4A4A4A" else "#909090",
                                textSecondary = if (dark) "#D4D4D4" else "#3A3A3A",
                                textMuted = if (dark) "#A8A8A8" else "#555555"
                            )
                        }
                    } else {
                        hcPrev?.let { prev -> manager.updateActiveColors { prev } }
                    }
                    highContrast = on
                }
            )
            SettingToggle("Reduced motion", "Kills all navigation and launcher animation", a.reducedMotion) { on ->
                manager.updateActiveAnimation { it.copy(reducedMotion = on) }
                state.settings.setBool("appearance.reduced-motion", on)
            }
        }
        EditorCard("Scale") {
            SettingToggle(
                label = "Large fonts",
                description = "Boosts the typography scale by 25%",
                checked = largeFonts,
                onCheckedChange = { on ->
                    if (on) {
                        fontPrev = t.fontScale
                        manager.updateActiveTypography { it.copy(fontScale = it.fontScale.coerceAtMost(1.6f) * 1.25f) }
                    } else {
                        manager.updateActiveTypography { it.copy(fontScale = fontPrev) }
                    }
                    largeFonts = on
                }
            )
            SettingToggle(
                label = "Large icons",
                description = "Boosts icon sizing by 25%",
                checked = largeIcons,
                onCheckedChange = { on ->
                    if (on) {
                        iconPrev = s.iconSize
                        manager.updateActiveScaling { it.copy(iconSize = it.iconSize.coerceAtMost(1.6f) * 1.25f) }
                    } else {
                        manager.updateActiveScaling { it.copy(iconSize = iconPrev) }
                    }
                    largeIcons = on
                }
            )
            SettingToggle(
                label = "Larger hitboxes",
                description = "Enlarges the floating bubble and its hover region",
                checked = bigBubble,
                onCheckedChange = { on ->
                    if (on) {
                        bubblePrev = s.bubbleSize
                        manager.updateActiveScaling { it.copy(bubbleSize = it.bubbleSize.coerceAtMost(1.7f) * 1.15f) }
                    } else {
                        manager.updateActiveScaling { it.copy(bubbleSize = bubblePrev) }
                    }
                    bigBubble = on
                }
            )
        }
    }
}

// ------------------------------------------------------------
// Preview tab — component anatomy rendered with the live theme
// ------------------------------------------------------------

@Composable
private fun ThemePreviewTab(theme: KaiteyoTheme, _manager: ThemeManager) {
    val sc = surfaceColors()
    val ac = accent()
    val g = theme.gradient
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        EditorCard("App anatomy", "Sidebar, top bar, cards, lists and controls — every change applies live") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .clip(RoundedCornerShape(DsRadius.Xl))
                    .background(sc.background)
                    .border(1.dp, sc.border.copy(alpha = 0.6f), RoundedCornerShape(DsRadius.Xl))
            ) {
                Row(Modifier.fillMaxSize()) {
                    // Mini sidebar
                    Column(
                        Modifier
                            .width(168.dp)
                            .fillMaxHeight()
                            .background(sc.surface)
                            .padding(DsSpacing.Md),
                        verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
                    ) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(DsRadius.Md)),
                            contentAlignment = Alignment.Center
                        ) {
                            ThemeGradientBar(g, Modifier.fillMaxSize())
                            // The real Kaiteyo mark in the preview mock —
                            // centralized brand asset, not a "K".
                            BrandMark(
                                modifier = Modifier.size(26.dp),
                                contentDescription = null
                            )
                        }
                        Spacer(Modifier.height(DsSpacing.Sm))
                        PreviewNavItem("Dashboard", active = true, g = g)
                        PreviewNavItem("Browse", active = false, g = g)
                        PreviewNavItem("Review", active = false, g = g)
                        PreviewNavItem("Collections", active = false, g = g)
                        PreviewNavItem("Statistics", active = false, g = g)
                        Spacer(Modifier.weight(1f))
                        Text("Weekly goal", color = sc.textMuted, fontSize = DsType.Caption)
                        DsProgressBar(fraction = 0.62f, height = 5.dp)
                    }
                    // Main content
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(DsSpacing.Lg),
                        verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Dashboard", color = sc.textPrimary, fontSize = DsType.Title, fontWeight = FontWeight.SemiBold)
                                Text("Live theme preview", color = sc.textMuted, fontSize = DsType.Caption)
                            }
                            DsButton(text = "New review", onClick = {}, compact = true)
                            DsIconButton(icon = Icons.Default.Palette, onClick = {}, contentDescription = "Theme")
                            DsIconButton(icon = Icons.Default.Star, onClick = {}, contentDescription = "Favorite")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                            DsStatTile("Due", "128", modifier = Modifier.weight(1f), delta = "+12 today")
                            DsStatTile("Learning", "24", modifier = Modifier.weight(1f))
                            DsStatTile("Mastered", "1,024", modifier = Modifier.weight(1f))
                        }
                        Row(
                            Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                        ) {
                            DsCard(modifier = Modifier.weight(1f)) {
                                Column(
                                    Modifier.padding(DsSpacing.Sm),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        "Continue learning",
                                        color = sc.textPrimary,
                                        fontSize = DsType.BodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(DsSpacing.Sm)
                                    )
                                    PreviewListItem("N5 · Kanji", "23 cards due", leading = "日", g = g)
                                    PreviewListItem("N4 · Vocab", "12 cards due", leading = "語", g = g)
                                    PreviewListItem("JLPT · Grammar", "8 cards due", leading = "文", g = g)
                                }
                            }
                            DsCard(modifier = Modifier.weight(1f)) {
                                Column(
                                    Modifier.padding(DsSpacing.Lg),
                                    verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                                ) {
                                    Text("Controls", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                                        DsButton("Primary", onClick = {}, compact = true)
                                        DsButton("Ghost", onClick = {}, kind = DsButtonKind.Ghost, compact = true)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                                        DsButton("Secondary", onClick = {}, kind = DsButtonKind.Secondary, compact = true)
                                        DsButton("Danger", onClick = {}, kind = DsButtonKind.Danger, compact = true)
                                    }
                                    DsToggle(checked = true, onCheckedChange = {}, label = "Auto-hide sidebar")
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        DsBadge("12 due", tint = ac.primary)
                                        DsBadge("NEW", tint = successColor())
                                        DsBadge("FLAG", tint = warningColor())
                                    }
                                    Text("Progress", color = sc.textSecondary, fontSize = DsType.Caption)
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(sc.surfaceInteractive)
                                    ) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth(0.7f)
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        ) {
                                            ThemeGradientBar(g, Modifier.fillMaxSize())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        EditorCard("Dialog anatomy", "Floating surfaces, fields and actions") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DsRadius.Xl))
                    .background(sc.surfaceElevated)
                    .border(1.dp, sc.border, RoundedCornerShape(DsRadius.Xl))
            ) {
                Column(
                    Modifier.padding(DsSpacing.Xl),
                    verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                ) {
                    Text("Edit card", color = sc.textPrimary, fontSize = DsType.Title, fontWeight = FontWeight.SemiBold)
                    Text("A dialog keeps focus on the current action.", color = sc.textMuted, fontSize = DsType.Body)
                    DsTextField(value = "進む", onValueChange = {}, singleLine = true)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
                    ) {
                        DsTextButton(text = "Cancel", onClick = {})
                        DsButton(text = "Save", onClick = {})
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewNavItem(label: String, active: Boolean, g: ThemeGradient) {
    val sc = surfaceColors()
    val ac = accent()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .then(if (active) Modifier.background(ac.primary.copy(alpha = 0.14f)) else Modifier)
            .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp))) {
            if (active) {
                ThemeGradientBar(g, Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize().background(sc.border.copy(alpha = 0.5f)))
            }
        }
        Text(
            text = label,
            color = if (active) sc.textPrimary else sc.textMuted,
            fontSize = DsType.Label,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun PreviewListItem(title: String, subtitle: String, leading: String, g: ThemeGradient) {
    val sc = surfaceColors()
    val ac = accent()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(sc.surfaceInteractive.copy(alpha = 0.4f))
            .padding(DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(DsRadius.Sm))
                .background(sc.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Text(leading, color = ac.primary, fontSize = DsType.Body, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium)
            Text(subtitle, color = sc.textMuted, fontSize = DsType.Caption)
        }
        Text("→", color = sc.textMuted, fontSize = DsType.Body)
    }
}

// ------------------------------------------------------------
// Shared studio controls
// ------------------------------------------------------------

@Composable
private fun EditorCard(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    DsCard {
        Column(Modifier.padding(DsSpacing.Lg), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
            DsSectionHeader(title = title, subtitle = subtitle)
            content()
        }
    }
}

@Composable
private fun StudioSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: String,
    onChanged: (Float) -> Unit
) {
    val sc = surfaceColors()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
            Text(String.format(format, value), color = sc.textMuted, fontSize = DsType.Caption)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChanged,
            valueRange = range
        )
    }
}

@Composable
private fun SettingToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val sc = surfaceColors()
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = sc.textPrimary, fontSize = DsType.Body, fontWeight = FontWeight.Medium)
            if (description.isNotBlank()) {
                Text(description, color = sc.textMuted, fontSize = DsType.Caption)
            }
        }
        DsToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun RenameThemeDialog(state: AppState, onDismiss: () -> Unit) {
    val manager = state.themeManager
    val theme = manager.activeTheme
    var name by remember(theme.id) { mutableStateOf(theme.name) }
    DsDialog(title = "Rename theme", onDismiss = onDismiss) {
        DsTextField(value = name, onValueChange = { name = it }, singleLine = true)
        Spacer(Modifier.height(DsSpacing.Lg))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
        ) {
            DsTextButton(
                text = "Cancel",
                onClick = {
                    onDismiss()
                }
            )
            DsButton(
                text = "Save",
                onClick = {
                    manager.rename(theme.id, name)
                    onDismiss()
                    state.activityLog.record(ActivityCategory.Theme, "Renamed theme to '$name'")
                    state.toastHost.show("Theme renamed", kind = ToastKind.Success)
                }
            )
        }
    }
}

private fun exportThemeFile(state: AppState) {
    val manager = state.themeManager
    val theme = manager.activeTheme
    val ok = TransferFilePicker.save(
        bytes = manager.exportJson(theme.id).toByteArray(),
        fileName = "kaiteyo-${theme.id}.json",
        description = "Kaiteyo theme",
        "json"
    )
    state.activityLog.record(ActivityCategory.Theme, if (ok) "Exported theme '${theme.name}'" else "Export of '${theme.name}' cancelled")
    state.toastHost.show(if (ok) "Theme exported" else "Export cancelled", kind = if (ok) ToastKind.Success else ToastKind.Info)
}

private fun importThemeFile(state: AppState) {
    val bytes = TransferFilePicker.open("Kaiteyo theme", "json") ?: return
    val text = bytes.toString(Charsets.UTF_8)
    val ok = state.themeManager.importJson(text)
    state.activityLog.record(ActivityCategory.Theme, if (ok) "Imported theme" else "Theme import failed")
    if (!ok) state.toastHost.show("Invalid theme file", kind = ToastKind.Error)
}
