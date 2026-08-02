package ua.syt0r.kanji.presentation.common.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.snap
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.presentation.common.resources.string.LocalStrings
import ua.syt0r.kanji.presentation.common.resources.string.getStrings
import ua.syt0r.kanji.presentation.common.ui.LocalOrientation
import ua.syt0r.kanji.presentation.common.ui.Orientation

// ============================================
// KAITEYO v1.2.0 — Theme Engine
// Premium animation system, gradient & glow support
// ============================================

// --- Local Composition Providers for Kaiteyo theme ---

val LocalKaiteyoAccent = compositionLocalOf { AllAccentSchemes.first() }
val LocalBaseMode = compositionLocalOf { BaseMode.Oled }
val LocalSurfaceColors = compositionLocalOf { surfaceForBaseMode(BaseMode.Oled) }

// ============================================
// ANIMATION CONFIGURATION
// ============================================

enum class PageTransitionType(val displayName: String) {
    Crossfade("Crossfade"),
    Slide("Slide"),
    FadeThrough("Fade Through"),
    Scale("Scale")
}

enum class AnimationSpeed(val displayName: String, val multiplier: Float) {
    Slow("Slow", 1.5f),
    Normal("Normal", 1.0f),
    Fast("Fast", 0.6f),
    Instant("Off", 0.0f)
}

data class AnimationConfig(
    val speed: AnimationSpeed = AnimationSpeed.Normal,
    val reducedMotion: Boolean = false,
    val springDamping: Float = 0.6f,
    val springStiffness: Float = 300f,
    val defaultDuration: Int = 300,
    val pageTransition: PageTransitionType = PageTransitionType.FadeThrough
)

val LocalAnimationConfig = compositionLocalOf { AnimationConfig() }

// ============================================
// CORNER RADIUS CONFIGURATION
// ============================================

enum class CornerRadiusStyle(val displayName: String, val globalMultiplier: Float) {
    Square("Square", 0.5f),
    Rounded("Rounded", 1.0f),
    VeryRounded("Very Rounded", 1.5f),
    Soft("Soft", 2.0f)
}

data class RadiusConfig(
    val style: CornerRadiusStyle = CornerRadiusStyle.Rounded,
    val customRadius: Float? = null
)

val LocalRadiusConfig = compositionLocalOf { RadiusConfig() }

// ============================================
// GLOW CONFIGURATION
// ============================================

data class GlowConfig(
    val intensity: Float = 1.0f,
    val radius: Float = 1.0f,
    val opacity: Float = 1.0f
)

val LocalGlowConfig = compositionLocalOf { GlowConfig() }

// ============================================
// DENSITY & LAYOUT CONFIGURATION
// ============================================

enum class UIDensity(val displayName: String, val spacingMultiplier: Float) {
    Compact("Compact", 0.7f),
    Comfortable("Comfortable", 1.0f),
    Spacious("Spacious", 1.3f)
}

enum class SidebarMode(val displayName: String) {
    Expanded("Expanded"),
    Compact("Compact"),
    IconsOnly("Icons Only"),
    FloatingIsland("Floating Island"),
    Docked("Docked"),
    AutoHide("Auto Hide")
}

enum class SidebarPosition(val displayName: String) {
    Left("Left"),
    Right("Right"),
    Top("Top"),
    Bottom("Bottom")
}

enum class NavAutoHide(val displayName: String) {
    Never("Never"),
    Always("Always"),
    FullscreenOnly("Fullscreen Only"),
    Smart("Smart")
}

data class LayoutConfig(
    val density: UIDensity = UIDensity.Comfortable,
    val sidebarMode: SidebarMode = SidebarMode.Expanded,
    val sidebarPosition: SidebarPosition = SidebarPosition.Left,
    val autoHide: NavAutoHide = NavAutoHide.Never,
    val collapsed: Boolean = false,
    val panelWidth: Dp = 260.dp,
    val panelHeight: Dp = 56.dp,
    val floatingOffset: DpOffset = DpOffset.Zero,
    val accentIndex: Int = -1,
    val transparencyEnabled: Boolean = false,
    val blurEnabled: Boolean = false,
    val glassOpacity: Float = 0.8f
)

val LocalLayoutConfig = compositionLocalOf { LayoutConfig() }

// ============================================
// THEME STATE
// ============================================

class KaiteyoThemeState(
    initialBaseMode: BaseMode = BaseMode.Oled,
    initialAccentScheme: KaiteyoAccentScheme = AllAccentSchemes.first(),
    initialAnimationConfig: AnimationConfig = AnimationConfig(),
    initialRadiusConfig: RadiusConfig = RadiusConfig(),
    initialGlowConfig: GlowConfig = GlowConfig(),
    initialLayoutConfig: LayoutConfig = LayoutConfig()
) {
    var baseMode by mutableStateOf(initialBaseMode)
    var accentScheme by mutableStateOf(initialAccentScheme)
    var animationConfig by mutableStateOf(initialAnimationConfig)
    var radiusConfig by mutableStateOf(initialRadiusConfig)
    var glowConfig by mutableStateOf(initialGlowConfig)
    var layoutConfig by mutableStateOf(initialLayoutConfig)
}

val LocalKaiteyoThemeState = compositionLocalOf { KaiteyoThemeState() }

// --- Material Color Scheme Generators ---

private fun createDarkColorScheme(
    accent: KaiteyoAccentScheme,
    surface: SurfaceColors
) = darkColorScheme(
    primary = accent.primary,
    onPrimary = accent.onPrimary,
    primaryContainer = accent.primary.copy(alpha = 0.15f),
    onPrimaryContainer = accent.primary,
    secondary = accent.secondary,
    onSecondary = accent.onSecondary,
    secondaryContainer = accent.secondary.copy(alpha = 0.15f),
    onSecondaryContainer = accent.secondary,
    tertiary = accent.tertiary ?: accent.secondary,
    onTertiary = accent.onSecondary,
    tertiaryContainer = (accent.tertiary ?: accent.secondary).copy(alpha = 0.15f),
    onTertiaryContainer = accent.tertiary ?: accent.secondary,
    error = semanticError,
    onError = textInverse,
    errorContainer = semanticError.copy(alpha = 0.15f),
    onErrorContainer = semanticError,
    background = surface.background,
    onBackground = surface.textPrimary,
    surface = surface.surface,
    onSurface = surface.textPrimary,
    surfaceVariant = surface.surfaceElevated,
    onSurfaceVariant = surface.textSecondary,
    surfaceContainerHigh = surface.surface,
    surfaceContainerHighest = surface.surfaceElevated,
    surfaceDim = surface.background,
    outline = surface.border,
    outlineVariant = surface.border.copy(alpha = 0.5f),
    inverseOnSurface = surface.textInverse,
    inverseSurface = surface.textPrimary,
    inversePrimary = accent.onPrimary,
)

private fun createLightColorScheme(
    accent: KaiteyoAccentScheme,
    surface: SurfaceColors
) = lightColorScheme(
    primary = accent.primaryDark,
    onPrimary = accent.onPrimary,
    primaryContainer = accent.primary.copy(alpha = 0.2f),
    onPrimaryContainer = accent.primaryDark,
    secondary = accent.secondaryDark,
    onSecondary = accent.onSecondary,
    secondaryContainer = accent.secondary.copy(alpha = 0.2f),
    onSecondaryContainer = accent.secondaryDark,
    tertiary = accent.tertiary ?: accent.secondaryDark,
    onTertiary = accent.onSecondary,
    tertiaryContainer = (accent.tertiary ?: accent.secondary).copy(alpha = 0.2f),
    onTertiaryContainer = accent.tertiary ?: accent.secondaryDark,
    error = semanticError,
    onError = textInverseLight,
    errorContainer = semanticError.copy(alpha = 0.15f),
    onErrorContainer = semanticError,
    background = surface.background,
    onBackground = surface.textPrimary,
    surface = surface.surface,
    onSurface = surface.textPrimary,
    surfaceVariant = surface.surfaceElevated,
    onSurfaceVariant = surface.textSecondary,
    surfaceContainerHigh = surface.surface,
    surfaceContainerHighest = surface.surfaceElevated,
    surfaceDim = surface.background,
    outline = surface.border,
    outlineVariant = surface.border.copy(alpha = 0.5f),
    inverseOnSurface = surface.textInverse,
    inverseSurface = surface.textPrimary,
    inversePrimary = accent.onPrimary,
)

// --- Extra colors scheme (backward compatible) ---

class ExtraColorsScheme(
    val link: Color,
    val success: Color,
    val pending: Color,
    val due: Color,
    val new: Color
)

val LightExtraColorScheme = ExtraColorsScheme(
    link = semanticInfo,
    success = semanticSuccess,
    pending = textMutedLight,
    due = semanticWarning,
    new = semanticNew
)

val DarkExtraColorScheme = ExtraColorsScheme(
    link = semanticInfo,
    success = semanticSuccess,
    pending = textMuted,
    due = semanticWarning,
    new = semanticNew
)

val LocalExtraColors = compositionLocalOf { LightExtraColorScheme }

val MaterialTheme.extraColorScheme: ExtraColorsScheme
    @Composable
    get() = LocalExtraColors.current

// --- Convenience accessors for Kaiteyo theme ---

val MaterialTheme.kaiteyoAccent: KaiteyoAccentScheme
    @Composable
    get() = LocalKaiteyoAccent.current

val MaterialTheme.baseMode: BaseMode
    @Composable
    get() = LocalBaseMode.current

val MaterialTheme.surfaceColors: SurfaceColors
    @Composable
    get() = LocalSurfaceColors.current

val MaterialTheme.kaiteyoThemeState: KaiteyoThemeState
    @Composable
    get() = LocalKaiteyoThemeState.current

val MaterialTheme.animationConfig: AnimationConfig
    @Composable
    get() = LocalAnimationConfig.current

val MaterialTheme.glowConfig: GlowConfig
    @Composable
    get() = LocalGlowConfig.current

val MaterialTheme.radiusConfig: RadiusConfig
    @Composable
    get() = LocalRadiusConfig.current

val MaterialTheme.layoutConfig: LayoutConfig
    @Composable
    get() = LocalLayoutConfig.current

// ============================================
// Main Kaiteyo AppTheme Composable
// ============================================

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    useAmoledTheme: Boolean = false,
    orientation: Orientation = Orientation.Portrait,
    baseMode: BaseMode = if (useAmoledTheme) BaseMode.Oled
        else if (!useDarkTheme) BaseMode.Light
        else BaseMode.Dark,
    accentScheme: KaiteyoAccentScheme = AllAccentSchemes.first(),
    animationConfig: AnimationConfig = AnimationConfig(),
    radiusConfig: RadiusConfig = RadiusConfig(),
    glowConfig: GlowConfig = GlowConfig(),
    layoutConfig: LayoutConfig = LayoutConfig(),
    content: @Composable () -> Unit
) {
    val surface = surfaceForBaseMode(baseMode)
    val isDark = baseMode != BaseMode.Light

    val colors = if (isDark) {
        createDarkColorScheme(accentScheme, surface)
    } else {
        createLightColorScheme(accentScheme, surface)
    }

    val extraColors = if (isDark) DarkExtraColorScheme else LightExtraColorScheme

    CompositionLocalProvider(
        LocalKaiteyoAccent provides accentScheme,
        LocalBaseMode provides baseMode,
        LocalSurfaceColors provides surface,
        LocalAnimationConfig provides animationConfig,
        LocalRadiusConfig provides radiusConfig,
        LocalGlowConfig provides glowConfig,
        LocalLayoutConfig provides layoutConfig
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = AppTypography,
            content = {
                CompositionLocalProvider(
                    LocalExtraColors provides extraColors,
                    LocalOrientation provides orientation,
                    LocalStrings provides getStrings(),
                    LocalTextSelectionColors provides neutralTextSelectionColors()
                ) {
                    content()
                }
            }
        )
    }
}

@Composable
private fun neutralTextSelectionColors() = TextSelectionColors(
    handleColor = MaterialTheme.colorScheme.onSurface,
    backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
)

@Composable
fun ButtonDefaults.neutralButtonColors(): ButtonColors {
    return MaterialTheme.colorScheme.run {
        buttonColors(
            containerColor = surfaceVariant,
            contentColor = onSurfaceVariant
        )
    }
}

@Composable
fun ButtonDefaults.neutralTextButtonColors(): ButtonColors {
    return MaterialTheme.colorScheme.run {
        textButtonColors(
            contentColor = onSurface
        )
    }
}

@Composable
fun TextFieldDefaults.neutralColors(): TextFieldColors = MaterialTheme.colorScheme.run {
    val labelColor = onSurface.copy(alpha = 0.4f)
    colors(
        unfocusedIndicatorColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        unfocusedLabelColor = labelColor,
        focusedLabelColor = labelColor,
        disabledLabelColor = labelColor,
        cursorColor = onSurface
    )
}

@Composable
fun ListItemDefaults.errorColors(): ListItemColors {
    return colors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        headlineColor = MaterialTheme.colorScheme.onErrorContainer,
        supportingColor = MaterialTheme.colorScheme.onErrorContainer,
        leadingIconColor = MaterialTheme.colorScheme.onErrorContainer,
        trailingIconColor = MaterialTheme.colorScheme.onErrorContainer
    )
}

// ============================================
// Animation Helpers
// ============================================

/**
 * Get spring animation spec based on animation config
 */
fun springAnim(
    config: AnimationConfig = AnimationConfig(),
    dampingRatio: Float = config.springDamping,
    stiffness: Float = config.springStiffness
): FiniteAnimationSpec<Float> = spring(dampingRatio = dampingRatio, stiffness = stiffness)

/**
 * Get tween duration based on animation speed
 */
fun tweenDuration(
    config: AnimationConfig = AnimationConfig(),
    baseDuration: Int = config.defaultDuration
): Int = if (config.reducedMotion) 0
    else (baseDuration * config.speed.multiplier).toInt()

/**
 * Page transition specifications
 * Returns a fade-through transition by default
 */
fun <S> pageTransitionSpec(
    animationConfig: AnimationConfig = AnimationConfig()
): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
    val duration = tweenDuration(animationConfig, 350)
    fadeIn(animationSpec = tween(duration / 2)) togetherWith
        fadeOut(animationSpec = tween(duration / 2))
}

fun snapSizeTransform(): SizeTransform = SizeTransform() { _, _ -> snap() }

fun snapToBiggerSizeTransform(
    snapToSmallerContainerDelay: Int = AnimationConstants.DefaultDurationMillis
): SizeTransform = SizeTransform { initial, target ->
    if (target.width > initial.width || target.height > initial.height) snap()
    else snap(snapToSmallerContainerDelay)
}

fun <S> snapToBiggerContainerCrossfadeTransitionSpec(
    snapToSmallerContainerDelay: Int = AnimationConstants.DefaultDurationMillis
): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
    ContentTransform(
        targetContentEnter = fadeIn(),
        initialContentExit = fadeOut(),
        sizeTransform = snapToBiggerSizeTransform(snapToSmallerContainerDelay)
    )
}
