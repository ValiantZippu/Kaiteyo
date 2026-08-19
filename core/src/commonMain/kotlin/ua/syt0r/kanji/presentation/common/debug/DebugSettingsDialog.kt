package ua.syt0r.kanji.presentation.common.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow as materialShadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.theme_manager.ThemeManager
import ua.syt0r.kanji.core.user_data.preferences.PreferencesTheme
import ua.syt0r.kanji.presentation.common.nav.NavigationMode
import ua.syt0r.kanji.presentation.common.nav.NavigationSettingsState
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors

// ============================================================
// DEBUG SETTINGS
// ------------------------------------------------------------
// Developer-facing settings for the Debug Panel. Every control
// is wired to real state: overlay visibility toggles persist
// through DebugSettingsState, motion/contrast toggles drive the
// actual accessibility settings, "force" actions call the real
// navigation/theme managers, and reset restores defaults.
// Intentionally absent: any control that cannot work yet (see
// the honest note at the bottom) — a dead control would be a
// ghost control.
// ============================================================

@Composable
fun DebugSettingsDialog(
    debugSettings: DebugSettingsState,
    navSettings: NavigationSettingsState,
    onDismiss: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .safeDrawingPadding()
                .widthIn(max = 560.dp)
                .materialShadow(24.dp, RoundedCornerShape(Dimens.RadiusXl))
                .clip(RoundedCornerShape(Dimens.RadiusXl)),
            shape = RoundedCornerShape(Dimens.RadiusXl),
            color = surfaceColors.surfaceElevated
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.Space3),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Debug settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = surfaceColors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = surfaceColors.textSecondary
                        )
                    }
                }

                DebugSection("Overlay") {
                    ToggleRow(
                        label = "Show page info (bottom corner)",
                        checked = debugSettings.settings.showPageInfo,
                        onChange = { value ->
                            debugSettings.update { current -> current.copy(showPageInfo = value) }
                        }
                    )
                    ToggleRow(
                        label = "Show FPS",
                        checked = debugSettings.settings.showFps,
                        onChange = { value ->
                            debugSettings.update { current -> current.copy(showFps = value) }
                        }
                    )
                    ToggleRow(
                        label = "Show viewport size",
                        checked = debugSettings.settings.showViewport,
                        onChange = { value ->
                            debugSettings.update { current -> current.copy(showViewport = value) }
                        }
                    )
                }

                DebugSection("Motion") {
                    ToggleRow(
                        label = "Disable animations",
                        checked = debugSettings.settings.disableAnimations,
                        onChange = { value ->
                            debugSettings.update { current -> current.copy(disableAnimations = value) }
                        }
                    )
                    ToggleRow(
                        label = "Reduce motion",
                        checked = navSettings.settings.accessibility.reducedMotion,
                        onChange = { value ->
                            navSettings.update { current ->
                                current.copy(accessibility = current.accessibility.copy(reducedMotion = value))
                            }
                        }
                    )
                    ToggleRow(
                        label = "High contrast",
                        checked = navSettings.settings.accessibility.highContrast,
                        onChange = { value ->
                            navSettings.update { current ->
                                current.copy(accessibility = current.accessibility.copy(highContrast = value))
                            }
                        }
                    )
                }

                DebugSection("Force") {
                    Text(
                        text = "Navigation mode",
                        style = MaterialTheme.typography.bodyMedium,
                        color = surfaceColors.textPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space1)) {
                        listOf(NavigationMode.Floating, NavigationMode.Sidebar).forEach { mode ->
                            ChoiceChip(
                                label = mode.name,
                                selected = navSettings.settings.mode == mode,
                                onClick = { navSettings.setMode(mode) }
                            )
                        }
                    }
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.bodyMedium,
                        color = surfaceColors.textPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space1)) {
                        val themeManager = koinInject<ThemeManager>()
                        PreferencesTheme.entries.forEach { theme ->
                            ChoiceChip(
                                label = theme.name,
                                selected = themeManager.currentTheme.value == theme,
                                onClick = { themeManager.changeTheme(theme) }
                            )
                        }
                    }
                }

                DebugSection("Data") {
                    ResetRow(label = "Reset debug settings") { debugSettings.reset() }
                }

                Text(
                    text = "\"Show layout bounds / hitboxes\" is intentionally absent: a control that does not actually work would be a ghost control. It will be added together with a real bounds overlay.",
                    style = MaterialTheme.typography.bodySmall,
                    color = surfaceColors.textMuted
                )
            }
        }
    }
}

@Composable
private fun DebugSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = surfaceColors.textMuted,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = Dimens.Space1)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.RadiusLg))
                .background(surfaceColors.surfaceInteractive.copy(alpha = 0.5f))
                .padding(Dimens.Space3),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2),
            content = content
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = surfaceColors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(
                when {
                    selected -> accent.primary.copy(alpha = 0.16f)
                    isHovered -> surfaceColors.surfaceInteractive
                    else -> surfaceColors.surface
                }
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent.primary else surfaceColors.border.copy(alpha = 0.3f),
                shape = RoundedCornerShape(Dimens.RadiusMd)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) accent.primary else surfaceColors.textSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun ResetRow(label: String, onReset: () -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(if (isHovered) surfaceColors.surfaceInteractive else surfaceColors.surface)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onReset)
            .hoverable(interactionSource)
            .padding(vertical = Dimens.Space2),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = accent.primary,
            fontWeight = FontWeight.Medium
        )
    }
}
