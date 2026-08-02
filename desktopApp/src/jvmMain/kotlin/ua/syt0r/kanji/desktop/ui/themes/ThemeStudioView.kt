package ua.syt0r.kanji.desktop.ui.themes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsButton
import ua.syt0r.kanji.desktop.designsystem.DsButtonKind
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsEmptyState
import ua.syt0r.kanji.desktop.designsystem.DsSectionHeader
import ua.syt0r.kanji.desktop.designsystem.DsSelect
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsSplitPane
import ua.syt0r.kanji.desktop.designsystem.DsTextArea
import ua.syt0r.kanji.desktop.designsystem.DsTextField
import ua.syt0r.kanji.desktop.designsystem.DsToggle
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.history.ActivityCategory
import ua.syt0r.kanji.desktop.engine.theming.KaiteyoTheme
import ua.syt0r.kanji.desktop.engine.theming.ThemePresets
import ua.syt0r.kanji.desktop.engine.theming.ThemeSerializer
import ua.syt0r.kanji.desktop.model.ToastKind

// ============================================
// THEME STUDIO
// Browse presets, live-edit colors/effects,
// and export/import full theme definitions as
// lossless JSON. Applies instantly to the suite.
// ============================================

@Composable
fun ThemeStudioView(state: AppState) {
    val sc = surfaceColors()
    var selectedId by remember { mutableStateOf(state.activeThemeId) }
    var draft by remember(selectedId) {
        mutableStateOf(
            ThemePresets.all.firstOrNull { it.id == selectedId } ?: ThemePresets.default
        )
    }
    var json by remember(selectedId) { mutableStateOf(ThemeSerializer.export(draft)) }
    var showJson by remember { mutableStateOf(false) }

    DsSplitPane(
        modifier = Modifier.fillMaxSize().padding(DsSpacing.Lg),
        vertical = false,
        initialFraction = 0.24f,
        first = {
            Column(verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                Text("Presets", color = sc.textMuted, fontSize = DsType.Caption, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = DsSpacing.Xs))
                ThemePresets.all.forEach { theme ->
                    val selected = theme.id == draft.id
                    DsCard(
                        onClick = {
                            selectedId = theme.id
                            draft = theme
                            json = ThemeSerializer.export(theme)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(DsSpacing.Md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
                        ) {
                            ThemeSwatch(theme)
                            Column {
                                Text(
                                    text = theme.name,
                                    color = sc.textPrimary,
                                    fontSize = DsType.Body,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                Text(
                                    text = theme.baseMode.uppercase(),
                                    color = sc.textMuted,
                                    fontSize = DsType.Caption
                                )
                            }
                        }
                    }
                }
            }
        },
        second = {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
            ) {
                // Editor
                DsCard {
                    Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                        DsSectionHeader(
                            title = draft.name,
                            subtitle = draft.description,
                            action = {
                                DsButton(
                                    text = "Apply theme",
                                    onClick = {
                                        state.applyTheme(draft.id)
                                        state.activityLog.record(ActivityCategory.Theme, "Applied theme '${draft.name}'")
                                        state.toastHost.show("Theme '${draft.name}' applied", kind = ToastKind.Success)
                                    }
                                )
                            }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md), verticalAlignment = Alignment.CenterVertically) {
                            DsSelect(
                                selected = draft.baseMode,
                                options = listOf("oled", "dark", "light", "sepia"),
                                onSelected = { draft = draft.copy(baseMode = it); json = ThemeSerializer.export(draft) },
                                labelOf = { it },
                                modifier = Modifier.width(160.dp)
                            )
                            DsToggle(
                                checked = draft.effects.oled,
                                onCheckedChange = { draft = draft.copy(effects = draft.effects.copy(oled = it)); json = ThemeSerializer.export(draft) },
                                label = "OLED"
                            )
                            DsToggle(
                                checked = draft.effects.material,
                                onCheckedChange = { draft = draft.copy(effects = draft.effects.copy(material = it)); json = ThemeSerializer.export(draft) },
                                label = "Material"
                            )
                        }
                    }
                }

                // Sliders
                DsCard {
                    Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                        Text("Adjust", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                        SliderRow("Font scale", draft.typography.fontScale, 0.8f..1.6f) { v ->
                            draft = draft.copy(typography = draft.typography.copy(fontScale = v))
                            json = ThemeSerializer.export(draft)
                        }
                        SliderRow("Corner radius", draft.corners.radiusMultiplier, 0.5f..2.0f) { v ->
                            draft = draft.copy(corners = draft.corners.copy(radiusMultiplier = v))
                            json = ThemeSerializer.export(draft)
                        }
                        SliderRow("Glass opacity", draft.effects.glassOpacity, 0f..1f) { v ->
                            draft = draft.copy(effects = draft.effects.copy(glassOpacity = v))
                            json = ThemeSerializer.export(draft)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                            DsToggle(
                                checked = draft.effects.blur,
                                onCheckedChange = { draft = draft.copy(effects = draft.effects.copy(blur = it)); json = ThemeSerializer.export(draft) },
                                label = "Blur"
                            )
                            DsToggle(
                                checked = draft.effects.transparency,
                                onCheckedChange = { draft = draft.copy(effects = draft.effects.copy(transparency = it)); json = ThemeSerializer.export(draft) },
                                label = "Transparency"
                            )
                            DsToggle(
                                checked = draft.animation.reducedMotion,
                                onCheckedChange = { draft = draft.copy(animation = draft.animation.copy(reducedMotion = it)); json = ThemeSerializer.export(draft) },
                                label = "Reduced motion"
                            )
                        }
                    }
                }

                // Colors
                DsCard {
                    Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                        Text("Colors", color = sc.textPrimary, fontSize = DsType.BodyLarge, fontWeight = FontWeight.SemiBold)
                        val colors = draft.colors
                        ColorRow("Background", colors.background) { v ->
                            draft = draft.copy(colors = colors.copy(background = v))
                            json = ThemeSerializer.export(draft)
                        }
                        ColorRow("Surface", colors.surface) { v ->
                            draft = draft.copy(colors = colors.copy(surface = v))
                            json = ThemeSerializer.export(draft)
                        }
                        ColorRow("Surface elevated", colors.surfaceElevated) { v ->
                            draft = draft.copy(colors = colors.copy(surfaceElevated = v))
                            json = ThemeSerializer.export(draft)
                        }
                        ColorRow("Border", colors.border) { v ->
                            draft = draft.copy(colors = colors.copy(border = v))
                            json = ThemeSerializer.export(draft)
                        }
                        ColorRow("Text primary", colors.textPrimary) { v ->
                            draft = draft.copy(colors = colors.copy(textPrimary = v))
                            json = ThemeSerializer.export(draft)
                        }
                        ColorRow("Text secondary", colors.textSecondary) { v ->
                            draft = draft.copy(colors = colors.copy(textSecondary = v))
                            json = ThemeSerializer.export(draft)
                        }
                        ColorRow("Primary (accent)", colors.primary) { v ->
                            draft = draft.copy(colors = colors.copy(primary = v))
                            json = ThemeSerializer.export(draft)
                        }
                        ColorRow("Secondary", colors.secondary) { v ->
                            draft = draft.copy(colors = colors.copy(secondary = v))
                            json = ThemeSerializer.export(draft)
                        }
                        ColorRow("Error", colors.error) { v ->
                            draft = draft.copy(colors = colors.copy(error = v))
                            json = ThemeSerializer.export(draft)
                        }
                    }
                }

                // JSON import / export
                DsCard {
                    Column(Modifier.padding(DsSpacing.Xl), verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
                        DsSectionHeader(
                            title = "Theme JSON",
                            subtitle = "Lossless serialization for sharing",
                            action = {
                                DsButton(
                                    text = if (showJson) "Hide" else "Edit JSON",
                                    kind = DsButtonKind.Ghost,
                                    onClick = { showJson = !showJson },
                                    compact = true
                                )
                            }
                        )
                        if (showJson) {
                            DsTextArea(
                                value = json,
                                onValueChange = { json = it },
                                height = 300.dp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
                                DsButton(
                                    text = "Validate & apply",
                                    onClick = {
                                        if (state.importThemeJson(json)) {
                                            selectedId = state.activeThemeId
                                        }
                                    }
                                )
                                DsButton(
                                    text = "Reset draft",
                                    kind = DsButtonKind.Ghost,
                                    onClick = {
                                        draft = ThemePresets.all.firstOrNull { it.id == selectedId } ?: ThemePresets.default
                                        json = ThemeSerializer.export(draft)
                                    }
                                )
                            }
                        } else {
                            DsEmptyState(
                                title = "Export your theme",
                                message = "Every theme exports as a portable JSON definition.",
                                icon = Icons.Default.Star
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChanged: (Float) -> Unit
) {
    val sc = surfaceColors()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.weight(1f))
            Text("${"%.2f".format(value)}", color = sc.textMuted, fontSize = DsType.Caption)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChanged,
            valueRange = range
        )
    }
}

@Composable
private fun ColorRow(label: String, value: String, onChange: (String) -> Unit) {
    val sc = surfaceColors()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
        val swatchColor = parseHexColor(value)
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(swatchColor)
        )
        Text(label, color = sc.textSecondary, fontSize = DsType.Body, modifier = Modifier.width(150.dp))
        val text = remember(label) { mutableStateOf(value) }
        DsTextField(
            value = text.value,
            onValueChange = { raw ->
                val clean = raw.uppercase().replace("[^0-9A-F#]".toRegex(), "").take(9)
                text.value = clean
                if (clean.matches(Regex("^#[0-9A-F]{6}$"))) onChange(clean)
            },
            singleLine = true
        )
    }
}

@Composable
private fun ThemeSwatch(theme: KaiteyoTheme) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf(theme.colors.background, theme.colors.surface, theme.colors.primary, theme.colors.secondary).forEach { hex ->
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(parseHexColor(hex))
            )
        }
    }
}

private fun parseHexColor(hex: String): Color = try {
    val raw = hex.removePrefix("#")
    if (raw.length == 6) Color(
        raw.substring(0, 2).toInt(16),
        raw.substring(2, 4).toInt(16),
        raw.substring(4, 6).toInt(16),
        255
    ) else Color(0xFF888888)
} catch (_: Exception) {
    Color(0xFF888888)
}
