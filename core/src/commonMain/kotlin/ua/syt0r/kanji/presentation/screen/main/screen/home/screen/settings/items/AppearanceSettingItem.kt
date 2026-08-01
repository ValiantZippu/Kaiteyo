package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.items

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.theme_manager.ThemeManager
import ua.syt0r.kanji.core.user_data.preferences.PreferencesTheme
import ua.syt0r.kanji.presentation.common.MultiplatformDialog
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.AllAccentSchemes
import ua.syt0r.kanji.presentation.common.theme.BaseMode
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoThemeState
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.surfaceForBaseMode
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingsScreenContract

// ============================================
// KAITEYO v1.1.0 — Appearance Settings
// Visual theme selector with cards
// ============================================

class AppearanceSettingItem(
    private val themeManager: ThemeManager
) : SettingsScreenContract.ListItem {

    @Composable
    override fun content(mainNavigationState: MainNavigationState) {
        val coroutineScope = rememberCoroutineScope()
        val themeState = LocalKaiteyoThemeState.current
        val currentAccent = LocalKaiteyoAccent.current
        val surfaceColors = LocalSurfaceColors.current

        // Appearance section header
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
            color = surfaceColors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                start = 16.dp,
                top = 24.dp,
                bottom = 12.dp
            )
        )

        // Base Mode Selector
        Text(
            text = "Theme Mode",
            style = MaterialTheme.typography.bodyMedium,
            color = surfaceColors.textSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BaseMode.entries.forEach { mode ->
                val isSelected = themeState.baseMode == mode
                BaseModeCard(
                    mode = mode,
                    isSelected = isSelected,
                    onClick = {
                        coroutineScope.launch {
                            themeState.baseMode = mode
                            // Map BaseMode to PreferencesTheme for persistence
                            val prefTheme = when (mode) {
                                BaseMode.Oled -> PreferencesTheme.Amoled
                                BaseMode.Dark -> PreferencesTheme.Dark
                                BaseMode.Light -> PreferencesTheme.Light
                                BaseMode.Sepia -> PreferencesTheme.Light
                            }
                            themeManager.changeTheme(prefTheme)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Accent Scheme Selector
        Text(
            text = "Color Scheme",
            style = MaterialTheme.typography.bodyMedium,
            color = surfaceColors.textSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AllAccentSchemes.forEachIndexed { index, scheme ->
                val isSelected = currentAccent.name == scheme.name
                val isDefault = index == 0
                AccentSchemeCard(
                    scheme = scheme,
                    isSelected = isSelected,
                    isDefault = isDefault,
                    onClick = {
                        coroutineScope.launch {
                            themeState.accentScheme = scheme
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Open full Appearance Studio
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Button(onClick = { mainNavigationState.navigate(MainDestination.AppearanceStudio) }) {
                Text(text = "Open Appearance Studio")
            }
        }
    }
}

@Composable
private fun BaseModeCard(
    mode: BaseMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val surface = surfaceForBaseMode(mode)

    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected -> surface.surfaceElevated
            isHovered -> surface.surface.copy(alpha = 0.8f)
            else -> surface.surface
        },
        animationSpec = tween(200),
        label = "cardBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) LocalKaiteyoAccent.current.primary
            else surface.border.copy(alpha = 0.3f),
        animationSpec = tween(200),
        label = "cardBorder"
    )

    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(200),
        label = "cardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(0.32f)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .hoverable(interactionSource)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Preview box showing the mode's background
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(surface.background)
                    .border(
                        width = 1.dp,
                        color = surface.border.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = mode.displayName,
                color = surface.textPrimary,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun AccentSchemeCard(
    scheme: KaiteyoAccentScheme,
    isSelected: Boolean,
    isDefault: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected -> surfaceColors.surfaceElevated
            isHovered -> surfaceColors.surface.copy(alpha = 0.8f)
            else -> surfaceColors.surface
        },
        animationSpec = tween(200),
        label = "schemeBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accent.primary
            else surfaceColors.border.copy(alpha = 0.3f),
        animationSpec = tween(200),
        label = "schemeBorder"
    )

    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.01f else 1f,
        animationSpec = tween(200),
        label = "schemeScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .hoverable(interactionSource)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color preview dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            scheme.previewColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Scheme name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = scheme.name,
                color = surfaceColors.textPrimary,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
            if (isDefault) {
                Text(
                    text = "Default",
                    color = accent.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Selected indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent.primary)
            )
        }
    }
}