package ua.syt0r.kanji.presentation.screen.main.screen.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.theme_manager.ThemeManager
import ua.syt0r.kanji.core.user_data.preferences.PreferencesTheme
import ua.syt0r.kanji.presentation.common.nav.LocalNavigationSettings
import ua.syt0r.kanji.presentation.common.nav.NavigationSettingsOverlay
import ua.syt0r.kanji.presentation.common.nav.rememberFormFactor
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.AllAccentSchemes
import ua.syt0r.kanji.presentation.common.theme.AnimationSpeed
import ua.syt0r.kanji.presentation.common.theme.BaseMode
import ua.syt0r.kanji.presentation.common.theme.CornerRadiusStyle
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.KaiteyoThemeState
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoThemeState
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.PageTransitionType
import ua.syt0r.kanji.presentation.common.theme.UIDensity
import ua.syt0r.kanji.presentation.common.theme.gradientForAccent
import ua.syt0r.kanji.presentation.common.theme.surfaceForBaseMode
import ua.syt0r.kanji.presentation.common.theme.toHexString

// ============================================
// KAITEYO APPEARANCE STUDIO
// Every control writes straight into the live
// KaiteyoThemeState (base mode is routed through
// the persisted preference), so all six tabs take
// effect app-wide instantly and survive restarts.
// ============================================

private enum class StudioTab(val displayName: String) {
    Themes("Themes"),
    Colors("Colors"),
    Gradient("Gradient"),
    Motion("Motion"),
    Layout("Layout"),
    Export("Export")
}

private val TwoPaneMinWidth = 720.dp

@Composable
fun AppearanceStudio() {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current
    val themeManager = koinInject<ThemeManager>()
    var selectedTab by remember { mutableStateOf(StudioTab.Themes) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Capture the constraint in a local before entering the Row/Column
        // lambdas, where the scope receiver would shadow it.
        val availableWidth = maxWidth
        val wide = availableWidth >= TwoPaneMinWidth

        Row(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(
                modifier = Modifier
                    .width(if (wide) 440.dp else availableWidth - 24.dp)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "Appearance Studio",
                    style = MaterialTheme.typography.titleLarge,
                    color = surfaceColors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                StudioTabBar(
                    selected = selectedTab,
                    onSelect = { selectedTab = it }
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = surfaceColors.border.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTab) {
                        StudioTab.Themes -> ThemePresetsTab(themeManager)
                        StudioTab.Colors -> ColorEditorTab()
                        StudioTab.Gradient -> GradientEditorTab()
                        StudioTab.Motion -> MotionStudioTab()
                        StudioTab.Layout -> LayoutStudioTab()
                        StudioTab.Export -> ThemeExportTab(themeManager)
                    }
                }
            }

            if (wide) {
                Spacer(Modifier.width(12.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxHeight().width(1.dp),
                    color = surfaceColors.border.copy(alpha = 0.2f)
                )
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    LivePreviewPanel()
                }
            }
        }
    }
}

@Composable
private fun StudioTabBar(
    selected: StudioTab,
    onSelect: (StudioTab) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val currentAccent = LocalKaiteyoAccent.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        StudioTab.entries.forEach { tab ->
            val isSelected = selected == tab
            val tabBg by animateColorAsState(
                targetValue = if (isSelected) currentAccent.primary.copy(alpha = 0.15f)
                else Color.Transparent,
                animationSpec = tween(200),
                label = "studioTabBg"
            )
            val tabText by animateColorAsState(
                targetValue = if (isSelected) currentAccent.primary
                else surfaceColors.textSecondary,
                animationSpec = tween(200),
                label = "studioTabText"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tabBg)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.displayName,
                    color = tabText,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ============================================
// BASE MODE → PERSISTED PREFERENCE
// Sepia has no preference equivalent, so it is
// applied to the live state only (session).
// ============================================

private fun applyBaseMode(
    themeState: KaiteyoThemeState,
    themeManager: ThemeManager,
    mode: BaseMode
) {
    when (mode) {
        BaseMode.Oled -> themeManager.changeTheme(PreferencesTheme.Amoled)
        BaseMode.Dark -> themeManager.changeTheme(PreferencesTheme.Dark)
        BaseMode.Light -> themeManager.changeTheme(PreferencesTheme.Light)
        BaseMode.Sepia -> themeManager.changeTheme(PreferencesTheme.Sepia)
        BaseMode.Cream -> themeManager.changeTheme(PreferencesTheme.Cream)
        BaseMode.Paper -> themeManager.changeTheme(PreferencesTheme.Paper)
        BaseMode.Midnight -> themeManager.changeTheme(PreferencesTheme.Midnight)
    }
}

// ============================================
// TAB 1: THEMES
// ============================================

@Composable
private fun ThemePresetsTab(themeManager: ThemeManager) {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current
    val currentAccent = LocalKaiteyoAccent.current

    Text(
        text = "Theme Presets",
        style = MaterialTheme.typography.titleMedium,
        color = surfaceColors.textPrimary,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Choose a base theme to customize",
        style = MaterialTheme.typography.bodySmall,
        color = surfaceColors.textMuted
    )
    Spacer(Modifier.height(12.dp))

    Text(
        text = "Base Mode",
        style = MaterialTheme.typography.bodyMedium,
        color = surfaceColors.textSecondary,
        fontWeight = FontWeight.Medium
    )
    Spacer(Modifier.height(6.dp))
    BaseMode.entries.forEach { mode ->
        val isSelected = themeState.baseMode == mode
        val cardBg by animateColorAsState(
            targetValue = if (isSelected) currentAccent.primary.copy(alpha = 0.12f)
            else surfaceColors.surface,
            animationSpec = tween(200),
            label = "baseModeCardBg"
        )
        val cardBorder by animateColorAsState(
            targetValue = if (isSelected) currentAccent.primary
            else surfaceColors.border.copy(alpha = 0.2f),
            animationSpec = tween(200),
            label = "baseModeCardBorder"
        )
        val surf = surfaceForBaseMode(mode)
        val isSepia = mode == BaseMode.Sepia

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cardBg)
                .border(1.5.dp, cardBorder, RoundedCornerShape(12.dp))
                .clickable { applyBaseMode(themeState, themeManager, mode) }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(surf.background)
                    .border(0.5.dp, surf.border.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = mode.displayName,
                    color = if (isSelected) currentAccent.primary else surfaceColors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
                Text(
                    text = if (isSepia) {
                        "Warm paper reading mode · session-only"
                    } else {
                        "Saved to preferences · ${surf.background.toHexString()} bg"
                    },
                    color = surfaceColors.textMuted,
                    fontSize = 11.sp
                )
            }
            if (isSelected) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(currentAccent.primary))
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    Text(
        text = "Color Schemes",
        style = MaterialTheme.typography.bodyMedium,
        color = surfaceColors.textSecondary,
        fontWeight = FontWeight.Medium
    )
    Spacer(Modifier.height(8.dp))

    AllAccentSchemes.chunked(2).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            row.forEach { scheme ->
                val isSelected = currentAccent.name == scheme.name &&
                    themeState.accentScheme.primary == scheme.primary
                val cardBg by animateColorAsState(
                    targetValue = if (isSelected) currentAccent.primary.copy(alpha = 0.12f)
                    else surfaceColors.surface,
                    animationSpec = tween(200),
                    label = "schemeCardBg"
                )
                val cardBorder by animateColorAsState(
                    targetValue = if (isSelected) currentAccent.primary
                    else surfaceColors.border.copy(alpha = 0.2f),
                    animationSpec = tween(200),
                    label = "schemeCardBorder"
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                        .clickable { themeState.accentScheme = scheme }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        scheme.previewColors.forEach { color ->
                            Box(Modifier.size(14.dp).clip(CircleShape).background(color))
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = scheme.name,
                        color = if (isSelected) currentAccent.primary else surfaceColors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

// ============================================
// TAB 2: COLORS
// ============================================

private enum class ColorTarget(val displayName: String) {
    Primary("Primary"),
    Secondary("Secondary"),
    Tertiary("Tertiary"),
    GradientStart("Gradient Start"),
    GradientEnd("Gradient End")
}

private fun KaiteyoAccentScheme.colorFor(target: ColorTarget): Color = when (target) {
    ColorTarget.Primary -> primary
    ColorTarget.Secondary -> secondary
    ColorTarget.Tertiary -> tertiary ?: secondary
    ColorTarget.GradientStart -> gradientStart ?: primary
    ColorTarget.GradientEnd -> gradientEnd ?: secondary
}

private fun KaiteyoAccentScheme.withColor(target: ColorTarget, color: Color): KaiteyoAccentScheme =
    when (target) {
        ColorTarget.Primary -> copy(
            name = "Custom", primary = color, primaryDark = color,
            previewColors = listOf(color, secondary)
        )
        ColorTarget.Secondary -> copy(
            name = "Custom", secondary = color, secondaryDark = color,
            previewColors = listOf(primary, color)
        )
        ColorTarget.Tertiary -> copy(name = "Custom", tertiary = color)
        ColorTarget.GradientStart -> copy(name = "Custom", gradientStart = color)
        ColorTarget.GradientEnd -> copy(name = "Custom", gradientEnd = color)
    }

@Composable
private fun ColorEditorTab() {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current
    val currentAccent = LocalKaiteyoAccent.current

    Text(
        text = "Color Editor",
        style = MaterialTheme.typography.titleMedium,
        color = surfaceColors.textPrimary,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Fine-tune every color in your theme — changes apply instantly",
        style = MaterialTheme.typography.bodySmall,
        color = surfaceColors.textMuted
    )
    Spacer(Modifier.height(12.dp))

    var selectedTarget by remember { mutableStateOf(ColorTarget.Primary) }
    val accent = themeState.accentScheme
    val targetColor = accent.colorFor(selectedTarget)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ColorTarget.entries.forEach { target ->
            val isSelected = selectedTarget == target
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) currentAccent.primary.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .clickable { selectedTarget = target }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = target.displayName,
                    color = if (isSelected) currentAccent.primary else surfaceColors.textSecondary,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(targetColor)
                .border(1.dp, surfaceColors.border.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = selectedTarget.displayName,
                color = surfaceColors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = targetColor.toHexString(),
                color = surfaceColors.textMuted,
                fontSize = 12.sp
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    // Live RGB draft — applied to the theme as the user drags.
    var red by remember(targetColor, selectedTarget) {
        mutableFloatStateOf(targetColor.red * 255f)
    }
    var green by remember(targetColor, selectedTarget) {
        mutableFloatStateOf(targetColor.green * 255f)
    }
    var blue by remember(targetColor, selectedTarget) {
        mutableFloatStateOf(targetColor.blue * 255f)
    }

    LaunchedEffect(red, green, blue) {
        val draft = Color(red / 255f, green / 255f, blue / 255f)
        // Skip the initial composition (and slider resyncs) — only apply a
        // genuine change so opening the tab never rewrites the current scheme.
        if (draft.toArgb() == targetColor.toArgb()) return@LaunchedEffect
        themeState.accentScheme = accent.withColor(selectedTarget, draft)
    }

    ColorSlider("R", red, Color.Red.copy(alpha = 0.3f)) {
        red = it * 255f
    }
    ColorSlider("G", green, Color.Green.copy(alpha = 0.3f)) {
        green = it * 255f
    }
    ColorSlider("B", blue, Color.Blue.copy(alpha = 0.3f)) {
        blue = it * 255f
    }

    Spacer(Modifier.height(12.dp))

    Text(
        text = "HEX",
        style = MaterialTheme.typography.bodyMedium,
        color = surfaceColors.textSecondary,
        fontWeight = FontWeight.Medium
    )
    Spacer(Modifier.height(4.dp))

    var hexValue by remember(targetColor, selectedTarget) {
        mutableStateOf(targetColor.toHexString().removePrefix("#"))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "#",
            color = surfaceColors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(surfaceColors.surfaceInteractive)
                .border(1.dp, surfaceColors.border.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            BasicTextField(
                value = TextFieldValue(hexValue),
                onValueChange = { newValue ->
                    val sanitized = newValue.text
                        .take(6)
                        .filter { it.isDigit() || it.uppercase() in "ABCDEF" }
                        .uppercase()
                    hexValue = sanitized
                    if (sanitized.length == 6) {
                        val r = sanitized.substring(0, 2).toIntOrNull(16) ?: 0
                        val g = sanitized.substring(2, 4).toIntOrNull(16) ?: 0
                        val b = sanitized.substring(4, 6).toIntOrNull(16) ?: 0
                        themeState.accentScheme = accent.withColor(
                            selectedTarget,
                            Color(r / 255f, g / 255f, b / 255f)
                        )
                    }
                },
                textStyle = TextStyle(
                    color = surfaceColors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                singleLine = true,
                modifier = Modifier.width(100.dp)
            )
        }
    }
}

// ============================================
// TAB 3: GRADIENT
// ============================================

@Composable
private fun GradientEditorTab() {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current
    val accent = themeState.accentScheme

    Text(
        text = "Gradient Editor",
        style = MaterialTheme.typography.titleMedium,
        color = surfaceColors.textPrimary,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Pick start and end colors — the gradient updates app-wide instantly",
        style = MaterialTheme.typography.bodySmall,
        color = surfaceColors.textMuted
    )
    Spacer(Modifier.height(12.dp))

    val gradient = gradientForAccent(accent)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(gradient.start, gradient.end)))
            .border(1.dp, surfaceColors.border.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
    )

    Spacer(Modifier.height(16.dp))

    GradientStopEditor(
        label = "Start color",
        color = gradient.start,
        onPick = { color ->
            themeState.accentScheme = accent.copy(name = "Custom", gradientStart = color)
        }
    )
    Spacer(Modifier.height(12.dp))
    GradientStopEditor(
        label = "End color",
        color = gradient.end,
        onPick = { color ->
            themeState.accentScheme = accent.copy(name = "Custom", gradientEnd = color)
        }
    )

    Spacer(Modifier.height(16.dp))

    Text(
        text = "Presets",
        style = MaterialTheme.typography.bodyMedium,
        color = surfaceColors.textSecondary,
        fontWeight = FontWeight.Medium
    )
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AllAccentSchemes.forEach { scheme ->
            val start = scheme.gradientStart ?: scheme.primary
            val end = scheme.gradientEnd ?: scheme.secondary
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.linearGradient(listOf(start, end)))
                    .border(0.5.dp, surfaceColors.border.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .clickable {
                        themeState.accentScheme = accent.copy(
                            name = "Custom",
                            gradientStart = start,
                            gradientEnd = end
                        )
                    }
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    Button(
        onClick = {
            themeState.accentScheme = accent.copy(gradientStart = null, gradientEnd = null)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = surfaceColors.surface,
            contentColor = surfaceColors.textPrimary
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Reset to scheme default", fontSize = 11.sp)
    }
}

@Composable
private fun GradientStopEditor(
    label: String,
    color: Color,
    onPick: (Color) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val currentAccent = LocalKaiteyoAccent.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, surfaceColors.border.copy(alpha = 0.3f), CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$label  ${color.toHexString()}",
            color = surfaceColors.textPrimary,
            fontSize = 13.sp
        )
    }
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val palette = AllAccentSchemes.flatMap { scheme ->
            listOf(
                scheme.gradientStart ?: scheme.primary,
                scheme.gradientEnd ?: scheme.secondary
            )
        }.distinctBy { it.toArgb() }
        palette.forEach { swatch ->
            val isSelected = swatch == color
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(swatch)
                    .border(
                        2.dp,
                        if (isSelected) currentAccent.primary else surfaceColors.border.copy(alpha = 0.3f),
                        CircleShape
                    )
                    .clickable { onPick(swatch) }
            )
        }
    }
}

private fun Color.toArgb(): Int {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return (255 shl 24) or (r shl 16) or (g shl 8) or b
}

// ============================================
// TAB 4: MOTION
// ============================================

private data class MotionPreset(
    val name: String,
    val speed: AnimationSpeed,
    val damping: Float,
    val stiffness: Float,
    val durationMs: Int
)

private val motionPresets = listOf(
    MotionPreset("Off", AnimationSpeed.Instant, 0.6f, 300f, 300),
    MotionPreset("Minimal", AnimationSpeed.Fast, 0.8f, 400f, 200),
    MotionPreset("Standard", AnimationSpeed.Normal, 0.6f, 300f, 300),
    MotionPreset("Smooth", AnimationSpeed.Slow, 0.5f, 200f, 450),
    MotionPreset("Bouncy", AnimationSpeed.Slow, 0.25f, 140f, 350)
)

@Composable
private fun MotionStudioTab() {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current
    val currentAccent = LocalKaiteyoAccent.current
    val config = themeState.animationConfig

    Text(
        text = "Motion Studio",
        style = MaterialTheme.typography.titleMedium,
        color = surfaceColors.textPrimary,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Control the feel of every interaction",
        style = MaterialTheme.typography.bodySmall,
        color = surfaceColors.textMuted
    )
    Spacer(Modifier.height(12.dp))

    Text(
        text = "Animation Preset",
        style = MaterialTheme.typography.bodyMedium,
        color = surfaceColors.textSecondary,
        fontWeight = FontWeight.Medium
    )
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        motionPresets.forEach { preset ->
            val isSelected = config.speed == preset.speed &&
                config.springDamping == preset.damping &&
                config.springStiffness == preset.stiffness &&
                config.defaultDuration == preset.durationMs
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) currentAccent.primary.copy(alpha = 0.15f)
                        else surfaceColors.surface
                    )
                    .border(
                        1.dp,
                        if (isSelected) currentAccent.primary else surfaceColors.border.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        themeState.animationConfig = config.copy(
                            speed = preset.speed,
                            springDamping = preset.damping,
                            springStiffness = preset.stiffness,
                            defaultDuration = preset.durationMs
                        )
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = preset.name,
                    color = if (isSelected) currentAccent.primary else surfaceColors.textSecondary,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    Text(
        text = "Spring Physics",
        style = MaterialTheme.typography.bodyMedium,
        color = surfaceColors.textSecondary,
        fontWeight = FontWeight.Medium
    )
    Spacer(Modifier.height(6.dp))
    SliderWithLabel(
        label = "Damping",
        value = config.springDamping,
        range = 0.1f..2f,
        suffix = "",
        onValueChange = { v ->
            themeState.animationConfig = config.copy(springDamping = v)
        }
    )
    SliderWithLabel(
        label = "Stiffness",
        value = config.springStiffness,
        range = 100f..1000f,
        suffix = "",
        onValueChange = { v ->
            themeState.animationConfig = config.copy(springStiffness = v)
        }
    )

    Spacer(Modifier.height(12.dp))

    Text(
        text = "Page Transition",
        style = MaterialTheme.typography.bodyMedium,
        color = surfaceColors.textSecondary,
        fontWeight = FontWeight.Medium
    )
    Spacer(Modifier.height(6.dp))
    PageTransitionType.entries.forEach { transition ->
        val isSelected = config.pageTransition == transition
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isSelected) currentAccent.primary.copy(alpha = 0.1f)
                    else Color.Transparent
                )
                .clickable {
                    themeState.animationConfig = config.copy(pageTransition = transition)
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) currentAccent.primary else surfaceColors.border)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = transition.displayName,
                color = if (isSelected) currentAccent.primary else surfaceColors.textPrimary,
                fontSize = 13.sp
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    SliderWithLabel(
        label = "Duration",
        value = config.defaultDuration.toFloat(),
        range = 50f..800f,
        suffix = "ms",
        format = { it.toInt().toString() },
        onValueChange = { v ->
            themeState.animationConfig = config.copy(defaultDuration = v.toInt())
        }
    )

    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                themeState.animationConfig = config.copy(
                    reducedMotion = !config.reducedMotion
                )
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (config.reducedMotion) currentAccent.primary else surfaceColors.border),
            contentAlignment = Alignment.Center
        ) {
            if (config.reducedMotion) {
                Text("\u2713", color = currentAccent.onPrimary, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text("Reduced Motion", color = surfaceColors.textPrimary, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        if (config.reducedMotion) {
            Text("On", color = currentAccent.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ============================================
// TAB 5: LAYOUT
// ============================================

@Composable
private fun LayoutStudioTab() {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current
    val currentAccent = LocalKaiteyoAccent.current
    var navSettingsOpen by remember { mutableStateOf(false) }

    Text(
        text = "Layout Studio",
        style = MaterialTheme.typography.titleMedium,
        color = surfaceColors.textPrimary,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Customize the spatial experience",
        style = MaterialTheme.typography.bodySmall,
        color = surfaceColors.textMuted
    )
    Spacer(Modifier.height(12.dp))

    Text(
        text = "UI Density",
        style = MaterialTheme.typography.bodyMedium,
        color = surfaceColors.textSecondary,
        fontWeight = FontWeight.Medium
    )
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        UIDensity.entries.forEach { density ->
            val isSelected = themeState.layoutConfig.density == density
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) currentAccent.primary.copy(alpha = 0.15f)
                        else surfaceColors.surface
                    )
                    .border(
                        1.dp,
                        if (isSelected) currentAccent.primary else surfaceColors.border.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        themeState.layoutConfig = themeState.layoutConfig.copy(density = density)
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = density.displayName,
                    color = if (isSelected) currentAccent.primary else surfaceColors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    Text(
        text = "Corner Radius Style",
        style = MaterialTheme.typography.bodyMedium,
        color = surfaceColors.textSecondary,
        fontWeight = FontWeight.Medium
    )
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        CornerRadiusStyle.entries.forEach { style ->
            val isSelected = themeState.radiusConfig.style == style
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) currentAccent.primary.copy(alpha = 0.15f)
                        else surfaceColors.surface
                    )
                    .border(
                        1.dp,
                        if (isSelected) currentAccent.primary else surfaceColors.border.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        themeState.radiusConfig = themeState.radiusConfig.copy(style = style)
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = style.displayName,
                    color = if (isSelected) currentAccent.primary else surfaceColors.textSecondary,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    SliderWithLabel(
        label = "Custom Radius",
        value = themeState.radiusConfig.customRadius ?: 12f,
        range = 0f..48f,
        suffix = "dp",
        format = { it.toInt().toString() },
        onValueChange = { v ->
            themeState.radiusConfig = themeState.radiusConfig.copy(customRadius = v)
        }
    )

    Spacer(Modifier.height(16.dp))

    Text(
        text = "Navigation",
        style = MaterialTheme.typography.bodyMedium,
        color = surfaceColors.textSecondary,
        fontWeight = FontWeight.Medium
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = "Mode, placement, floating launcher and phone layout are configured in the adaptive navigation settings.",
        color = surfaceColors.textMuted,
        fontSize = 12.sp
    )
    Spacer(Modifier.height(8.dp))
    val navSettings = LocalNavigationSettings.current
    if (navSettings != null) {
        Button(
            onClick = { navSettingsOpen = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = currentAccent.primary,
                contentColor = currentAccent.onPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(resolveString { nav.openNavigationSettingsLabel }, fontSize = 11.sp)
        }
    }

    Spacer(Modifier.height(16.dp))

    Text(
        text = "Glow Effects",
        style = MaterialTheme.typography.bodyMedium,
        color = surfaceColors.textSecondary,
        fontWeight = FontWeight.Medium
    )
    Spacer(Modifier.height(6.dp))
    SliderWithLabel(
        label = "Intensity",
        value = themeState.glowConfig.intensity,
        range = 0f..2f,
        suffix = "x",
        onValueChange = { v ->
            themeState.glowConfig = themeState.glowConfig.copy(intensity = v)
        }
    )
    SliderWithLabel(
        label = "Radius",
        value = themeState.glowConfig.radius,
        range = 0f..2f,
        suffix = "x",
        onValueChange = { v ->
            themeState.glowConfig = themeState.glowConfig.copy(radius = v)
        }
    )
    SliderWithLabel(
        label = "Opacity",
        value = themeState.glowConfig.opacity,
        range = 0f..1f,
        suffix = "%",
        format = { "${(it * 100).toInt()}" },
        onValueChange = { v ->
            themeState.glowConfig = themeState.glowConfig.copy(opacity = v)
        }
    )

    Spacer(Modifier.height(12.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                themeState.layoutConfig = themeState.layoutConfig.copy(
                    transparencyEnabled = !themeState.layoutConfig.transparencyEnabled
                )
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (themeState.layoutConfig.transparencyEnabled) currentAccent.primary
                    else surfaceColors.border
                ),
            contentAlignment = Alignment.Center
        ) {
            if (themeState.layoutConfig.transparencyEnabled) {
                Text("\u2713", color = currentAccent.onPrimary, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text("Enable Transparency", color = surfaceColors.textPrimary, fontSize = 13.sp)
    }
    if (themeState.layoutConfig.transparencyEnabled) {
        Spacer(Modifier.height(4.dp))
        SliderWithLabel(
            label = "Glass Opacity",
            value = themeState.layoutConfig.glassOpacity,
            range = 0.1f..1f,
            suffix = "%",
            format = { "${(it * 100).toInt()}" },
            onValueChange = { v ->
                themeState.layoutConfig = themeState.layoutConfig.copy(glassOpacity = v)
            }
        )
    }

    if (navSettingsOpen) {
        val navSettingsState = LocalNavigationSettings.current
        if (navSettingsState != null) {
            NavigationSettingsOverlay(
                navSettings = navSettingsState,
                formFactor = rememberFormFactor(),
                onDismiss = { navSettingsOpen = false }
            )
        }
    }
}

// ============================================
// TAB 6: EXPORT / IMPORT
// ============================================

@Composable
private fun ThemeExportTab(themeManager: ThemeManager) {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current
    val currentAccent = LocalKaiteyoAccent.current
    val clipboard = LocalClipboardManager.current
    var message by remember { mutableStateOf<String?>(null) }

    val themeJson = remember(themeState) { themeStateToPrettyJson(themeState) }

    Text(
        text = "Theme Export / Import",
        style = MaterialTheme.typography.titleMedium,
        color = surfaceColors.textPrimary,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Share your custom themes as JSON via the clipboard",
        style = MaterialTheme.typography.bodySmall,
        color = surfaceColors.textMuted
    )
    Spacer(Modifier.height(16.dp))

    Text(
        text = "Current Theme",
        style = MaterialTheme.typography.bodyMedium,
        color = surfaceColors.textSecondary,
        fontWeight = FontWeight.Medium
    )
    Spacer(Modifier.height(8.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(surfaceColors.surfaceInteractive)
            .border(1.dp, surfaceColors.border.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = themeJson,
                color = surfaceColors.textSecondary,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(
            onClick = {
                clipboard.setText(AnnotatedString(themeStateToJson(themeState)))
                message = "Theme copied to clipboard — paste it anywhere to share"
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = currentAccent.primary,
                contentColor = currentAccent.onPrimary
            ),
            modifier = Modifier.weight(1f)
        ) {
            Text("Copy Theme JSON", fontSize = 11.sp)
        }
        Button(
            onClick = {
                val imported = clipboard.getText()?.text?.let { themeJsonToState(it) }
                if (imported != null) {
                    applyBaseMode(themeState, themeManager, imported.baseMode)
                    themeState.accentScheme = imported.accentScheme
                    themeState.animationConfig = imported.animationConfig
                    themeState.radiusConfig = imported.radiusConfig
                    themeState.glowConfig = imported.glowConfig
                    themeState.layoutConfig = imported.layoutConfig
                    message = "Theme imported from clipboard"
                } else {
                    message = "Clipboard does not contain a valid Kaiteyo theme"
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = currentAccent.primary,
                contentColor = currentAccent.onPrimary
            ),
            modifier = Modifier.weight(1f)
        ) {
            Text("Import from Clipboard", fontSize = 11.sp)
        }
    }

    message?.let {
        Spacer(Modifier.height(6.dp))
        Text(it, color = surfaceColors.textMuted, fontSize = 11.sp)
    }

    Spacer(Modifier.height(12.dp))
    Button(
        onClick = {
            applyBaseMode(themeState, themeManager, BaseMode.Oled)
            themeState.accentScheme = AllAccentSchemes.first()
            themeState.animationConfig = themeState.animationConfig.copy(
                speed = AnimationSpeed.Normal,
                springDamping = 0.6f,
                springStiffness = 300f,
                defaultDuration = 300,
                pageTransition = PageTransitionType.FadeThrough,
                reducedMotion = false,
                themeTransitionEnabled = true
            )
            themeState.radiusConfig = themeState.radiusConfig.copy(
                style = CornerRadiusStyle.Rounded,
                customRadius = null
            )
            themeState.glowConfig = themeState.glowConfig.copy(
                intensity = 1f, radius = 1f, opacity = 1f
            )
            themeState.layoutConfig = themeState.layoutConfig.copy(
                density = UIDensity.Comfortable,
                transparencyEnabled = false,
                glassOpacity = 0.8f
            )
            message = "Theme reset to Kaiteyo defaults"
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Reset All to Defaults", fontSize = 11.sp)
    }
}

// ============================================
// LIVE PREVIEW PANEL
// ============================================

@Composable
private fun LivePreviewPanel() {
    val themeState = LocalKaiteyoThemeState.current
    val currentAccent = LocalKaiteyoAccent.current
    val previewSurface = surfaceForBaseMode(themeState.baseMode)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(previewSurface.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Live Preview",
            style = MaterialTheme.typography.titleMedium,
            color = previewSurface.textPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Changes apply in real-time",
            style = MaterialTheme.typography.bodySmall,
            color = previewSurface.textMuted
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(previewSurface.surface)
                .padding(8.dp)
        ) {
            // Mini sidebar
            Column(
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(previewSurface.surfaceElevated)
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(currentAccent.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("K", color = currentAccent.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                listOf("\u25C6", "\u25C7", "\u25C7", "\u25C7").forEachIndexed { i, icon ->
                    val isActive = i == 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isActive) currentAccent.primary.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 4.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            icon,
                            color = if (isActive) currentAccent.primary else previewSurface.textMuted,
                            fontSize = 8.sp
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (i == 0) "Home" else "Item",
                            color = if (isActive) currentAccent.primary else previewSurface.textMuted,
                            fontSize = 7.sp
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(previewSurface.border)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(currentAccent.primary)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            // Content area
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Dashboard",
                            color = previewSurface.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Study overview", color = previewSurface.textMuted, fontSize = 8.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(previewSurface.surfaceElevated),
                            contentAlignment = Alignment.Center
                        ) { Text("\u2699", fontSize = 7.sp, color = previewSurface.textMuted) }
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(previewSurface.surfaceElevated),
                            contentAlignment = Alignment.Center
                        ) { Text("\u2605", fontSize = 7.sp, color = previewSurface.textMuted) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("23", "156", "89").forEachIndexed { i, value ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(previewSurface.surfaceElevated)
                                .padding(6.dp)
                        ) {
                            Text(
                                value,
                                color = currentAccent.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                listOf("Learning", "Review", "Mastered")[i],
                                color = previewSurface.textMuted,
                                fontSize = 7.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(previewSurface.surfaceElevated)
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            "Continue Learning",
                            color = previewSurface.textPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        listOf(0.45f, 0.25f, 0.15f).forEach { progress ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = listOf("N5", "N4", "Vocab")[
                                        listOf(0.45f, 0.25f, 0.15f).indexOf(progress)
                                    ],
                                    color = previewSurface.textMuted,
                                    fontSize = 7.sp,
                                    modifier = Modifier.width(28.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(previewSurface.border)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progress)
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(1.5.dp))
                                            .background(currentAccent.primary)
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "${(progress * 100).toInt()}%",
                                    color = previewSurface.textMuted,
                                    fontSize = 7.sp
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(5.dp))
                                .background(currentAccent.primary)
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Start Review",
                                color = currentAccent.onPrimary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// SHARED CONTROLS
// ============================================

@Composable
private fun SliderWithLabel(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    suffix: String,
    onValueChange: (Float) -> Unit,
    format: (Float) -> String = { formatFloat(it, 1) }
) {
    val surfaceColors = LocalSurfaceColors.current
    Text(
        text = "$label: ${format(value)}$suffix",
        color = surfaceColors.textPrimary,
        fontSize = 13.sp
    )
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = range,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    trackColor: Color,
    onValueChange: (Float) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = surfaceColors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(20.dp)
        )
        Slider(
            value = value.coerceIn(0f, 255f) / 255f,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.toInt().toString(),
            color = surfaceColors.textMuted,
            fontSize = 11.sp,
            modifier = Modifier.width(30.dp),
            textAlign = TextAlign.End
        )
    }
}
