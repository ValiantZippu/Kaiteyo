package ua.syt0r.kanji.presentation.common.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
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
val LocalKaiteyoSemanticColors = compositionLocalOf { KaiteyoSemanticColorsDark }

// ============================================
// ANIMATION CONFIGURATION
// ============================================

@Serializable
enum class PageTransitionType(val displayName: String) {
    Crossfade("Crossfade"),
    Slide("Slide"),
    FadeThrough("Fade Through"),
    Scale("Scale")
}

@Serializable
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
    val pageTransition: PageTransitionType = PageTransitionType.FadeThrough,
    /** Whether accent / base-mode changes crossfade the whole UI. */
    val themeTransitionEnabled: Boolean = true
)

val LocalAnimationConfig = compositionLocalOf { AnimationConfig() }

/** Base duration (ms) of the whole-app theme color crossfade, before the
 *  user's animation-speed multiplier is applied. */
const val ThemeTransitionMillis = 450

// ============================================
// TYPE SCALE (live typography adjustments)
// ============================================

data class TypeScale(
    val fontScale: Float = 1f,
    val titleScale: Float = 1f,
    val lineHeight: Float = 1f,
    val letterSpacing: Float = 0f
)

val LocalTypeScale = compositionLocalOf { TypeScale() }

// ============================================
// CORNER RADIUS CONFIGURATION
// ============================================

@Serializable
enum class CornerRadiusStyle(val displayName: String, val globalMultiplier: Float) {
    Square("Square", 0.5f),
    Rounded("Rounded", 1.0f),
    VeryRounded("Very Rounded", 1.5f),
    Soft("Soft", 2.0f)
}

data class RadiusConfig(
    val style: CornerRadiusStyle = CornerRadiusStyle.Rounded,
    val customRadius: Float? = null,
    val buttonRadius: Float? = null
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

@Serializable
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
    val glassOpacity: Float = 0.8f,
    val displayScale: Float = 1f,
    val buttonScale: Float = 1f,
    val iconScale: Float = 1f,
    val bubbleScale: Float = 1f,
    val toolbarHeightScale: Float = 1f,
    val windowPaddingScale: Float = 1f
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
    initialLayoutConfig: LayoutConfig = LayoutConfig(),
    initialTypeScale: TypeScale = TypeScale()
) {
    var baseMode by mutableStateOf(initialBaseMode)
    var accentScheme by mutableStateOf(initialAccentScheme)
    var animationConfig by mutableStateOf(initialAnimationConfig)
    var radiusConfig by mutableStateOf(initialRadiusConfig)
    var glowConfig by mutableStateOf(initialGlowConfig)
    var layoutConfig by mutableStateOf(initialLayoutConfig)
    var typeScale by mutableStateOf(initialTypeScale)
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

// --- Semantic color accessors (theme-aware, always adapt) ---

val MaterialTheme.successColor: Color
    @Composable
    get() = LocalExtraColors.current.success

val MaterialTheme.warningColor: Color
    @Composable
    get() = LocalExtraColors.current.due

val MaterialTheme.infoColor: Color
    @Composable
    get() = LocalExtraColors.current.link

val MaterialTheme.newColor: Color
    @Composable
    get() = LocalExtraColors.current.new

val MaterialTheme.dangerColor: Color
    @Composable
    get() = semanticError

val MaterialTheme.favoriteColor: Color
    @Composable
    get() = Color(0xFFFFD93D)

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
    customSurface: SurfaceColors? = null,
    typography: Typography = AppTypography,
    typeScale: TypeScale = TypeScale(),
    content: @Composable () -> Unit
) {
    val surface = customSurface ?: surfaceForBaseMode(baseMode)
    // Every base mode (incl. Cream/Paper/Midnight) declares its own darkness.
    val isDark = baseMode.isDarkMode

    // Material components (buttons, fields, chips, dialogs, menus…) inherit
    // the Kaiteyo corner-radius system instead of Material defaults, so every
    // rounded corner in the app comes from the same tokens and follows the
    // user's radius configuration.
    val radiusMultiplier =
        radiusConfig.style.globalMultiplier * (radiusConfig.customRadius ?: 1f)
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(Dimens.RadiusXs * radiusMultiplier),
        small = RoundedCornerShape(Dimens.RadiusSm * radiusMultiplier),
        medium = RoundedCornerShape(Dimens.RadiusMd * radiusMultiplier),
        large = RoundedCornerShape(Dimens.RadiusLg * radiusMultiplier),
        extraLarge = RoundedCornerShape(Dimens.RadiusXl * radiusMultiplier)
    )

    // A theme switch (accent, base mode, or preset) morphs every color in
    // place through a single smooth crossfade instead of an abrupt jump.
    // Gated by the user's "Theme transition" toggle, the animation speed
    // setting and reduced-motion preference. State is fully preserved
    // because the UI tree itself never leaves composition — only the
    // colors animate toward their new targets.
    val themeFadeDuration = tweenDuration(animationConfig, ThemeTransitionMillis)
    val animateThemeTransition =
        animationConfig.themeTransitionEnabled && themeFadeDuration > 0

    val colors = (if (isDark) {
        createDarkColorScheme(accentScheme, surface)
    } else {
        createLightColorScheme(accentScheme, surface)
    }).withThemeTransition(animateThemeTransition, themeFadeDuration)

    // Live typography adjustments: the user-controlled type scale multiplies
    // font sizes and line heights so settings changes are visible immediately.
    val effectiveTypography = typography.scaledBy(typeScale)

    val extraColors = (if (isDark) DarkExtraColorScheme else LightExtraColorScheme)
        .withThemeTransition(animateThemeTransition, themeFadeDuration)

    // Push the same morphing back through the Kaiteyo locals so anything
    // reading LocalSurfaceColors or the accent (e.g. the desktop design
    // system's DsTokens) fades in lockstep with the Material scheme
    // instead of snapping to the new values mid-transition.
    //
    // For light base modes the bright accent primary clashes with warm
    // paper backgrounds (Sepia/Cream/Paper). Swap to the darker variant
    // so green/pink/orange accents stay readable on light surfaces.
    val adaptedAccent = if (isDark) accentScheme
    else accentScheme.copy(
        primary = accentScheme.primaryDark,
        secondary = accentScheme.secondaryDark
    )
    val animatedAccent =
        adaptedAccent.withThemeTransition(animateThemeTransition, themeFadeDuration)
    val animatedSurface =
        surface.withThemeTransition(animateThemeTransition, themeFadeDuration)

    val semanticColors = (if (isDark) KaiteyoSemanticColorsDark else KaiteyoSemanticColorsLight)
        .withThemeTransition(animateThemeTransition, themeFadeDuration)

    CompositionLocalProvider(
        LocalKaiteyoAccent provides animatedAccent,
        LocalBaseMode provides baseMode,
        LocalSurfaceColors provides animatedSurface,
        LocalKaiteyoSemanticColors provides semanticColors,
        LocalAnimationConfig provides animationConfig,
        LocalRadiusConfig provides radiusConfig,
        LocalGlowConfig provides glowConfig,
        LocalLayoutConfig provides layoutConfig,
        LocalTypeScale provides typeScale
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = effectiveTypography,
            shapes = shapes,
            content = {
                CompositionLocalProvider(
                    LocalExtraColors provides extraColors,
                    LocalOrientation provides orientation,
                    LocalStrings provides getStrings(),
                    LocalTextSelectionColors provides neutralTextSelectionColors()
                ) {
                    // Apply the user's displayScale as a root-level transform
                    // so the entire UI scales proportionally when the slider moves.
                    val rootScale = layoutConfig.displayScale
                    androidx.compose.foundation.layout.Box(
                        modifier = if (rootScale != 1f) {
                            Modifier.graphicsLayer {
                                scaleX = rootScale
                                scaleY = rootScale
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                            }
                        } else Modifier
                    ) {
                        content()
                    }
                }
            }
        )
    }
}

// ============================================
// Theme transition — whole-app color crossfade
// ============================================

// ============================================
// LIVE TYPOGRAPHY SCALING
// ============================================

/**
 * Applies the user's [TypeScale] to a Material [Typography]: title-level
 * styles (display/headline/title) use [TypeScale.titleScale], everything
 * else uses [TypeScale.fontScale]; line heights follow [TypeScale.lineHeight]
 * and [TypeScale.letterSpacing] is added to every style.
 */
private fun Typography.scaledBy(scale: TypeScale): Typography {
    if (scale == TypeScale()) return this
    return copy(
        displayLarge = displayLarge.scaled(scale, useTitleScale = true),
        displayMedium = displayMedium.scaled(scale, useTitleScale = true),
        displaySmall = displaySmall.scaled(scale, useTitleScale = true),
        headlineLarge = headlineLarge.scaled(scale, useTitleScale = true),
        headlineMedium = headlineMedium.scaled(scale, useTitleScale = true),
        headlineSmall = headlineSmall.scaled(scale, useTitleScale = true),
        titleLarge = titleLarge.scaled(scale, useTitleScale = true),
        titleMedium = titleMedium.scaled(scale, useTitleScale = true),
        titleSmall = titleSmall.scaled(scale, useTitleScale = true),
        bodyLarge = bodyLarge.scaled(scale),
        bodyMedium = bodyMedium.scaled(scale),
        bodySmall = bodySmall.scaled(scale),
        labelLarge = labelLarge.scaled(scale),
        labelMedium = labelMedium.scaled(scale),
        labelSmall = labelSmall.scaled(scale)
    )
}

private fun TextStyle.scaled(scale: TypeScale, useTitleScale: Boolean = false): TextStyle {
    val sizeFactor = if (useTitleScale) scale.titleScale else scale.fontScale
    return copy(
        fontSize = fontSize.multiplyBy(sizeFactor),
        lineHeight = lineHeight.multiplyBy(scale.lineHeight),
        letterSpacing = letterSpacing.plusSp(scale.letterSpacing)
    )
}

private fun TextUnit.multiplyBy(factor: Float): TextUnit =
    if (factor == 1f) this else (value * factor).sp

private fun TextUnit.plusSp(amount: Float): TextUnit =
    if (amount == 0f) this else (value + amount).sp

/**
 * Animates every color in the [ColorScheme] toward its target value so a
 * theme change (accent, base mode, preset) dissolves smoothly across the
 * entire UI. When disabled the scheme is returned untouched, which keeps
 * the switch instant.
 */
@Composable
private fun ColorScheme.withThemeTransition(enabled: Boolean, duration: Int): ColorScheme {
    if (!enabled) return this
    val spec = tween<Color>(duration)
    return copy(
        primary = animateColorAsState(primary, animationSpec = spec, label = "themePrimary").value,
        onPrimary = animateColorAsState(onPrimary, animationSpec = spec, label = "themeOnPrimary").value,
        primaryContainer = animateColorAsState(primaryContainer, animationSpec = spec, label = "themePrimaryContainer").value,
        onPrimaryContainer = animateColorAsState(onPrimaryContainer, animationSpec = spec, label = "themeOnPrimaryContainer").value,
        secondary = animateColorAsState(secondary, animationSpec = spec, label = "themeSecondary").value,
        onSecondary = animateColorAsState(onSecondary, animationSpec = spec, label = "themeOnSecondary").value,
        secondaryContainer = animateColorAsState(secondaryContainer, animationSpec = spec, label = "themeSecondaryContainer").value,
        onSecondaryContainer = animateColorAsState(onSecondaryContainer, animationSpec = spec, label = "themeOnSecondaryContainer").value,
        tertiary = animateColorAsState(tertiary, animationSpec = spec, label = "themeTertiary").value,
        onTertiary = animateColorAsState(onTertiary, animationSpec = spec, label = "themeOnTertiary").value,
        tertiaryContainer = animateColorAsState(tertiaryContainer, animationSpec = spec, label = "themeTertiaryContainer").value,
        onTertiaryContainer = animateColorAsState(onTertiaryContainer, animationSpec = spec, label = "themeOnTertiaryContainer").value,
        error = animateColorAsState(error, animationSpec = spec, label = "themeError").value,
        onError = animateColorAsState(onError, animationSpec = spec, label = "themeOnError").value,
        errorContainer = animateColorAsState(errorContainer, animationSpec = spec, label = "themeErrorContainer").value,
        onErrorContainer = animateColorAsState(onErrorContainer, animationSpec = spec, label = "themeOnErrorContainer").value,
        background = animateColorAsState(background, animationSpec = spec, label = "themeBackground").value,
        onBackground = animateColorAsState(onBackground, animationSpec = spec, label = "themeOnBackground").value,
        surface = animateColorAsState(surface, animationSpec = spec, label = "themeSurface").value,
        onSurface = animateColorAsState(onSurface, animationSpec = spec, label = "themeOnSurface").value,
        surfaceVariant = animateColorAsState(surfaceVariant, animationSpec = spec, label = "themeSurfaceVariant").value,
        onSurfaceVariant = animateColorAsState(onSurfaceVariant, animationSpec = spec, label = "themeOnSurfaceVariant").value,
        surfaceContainerHigh = animateColorAsState(surfaceContainerHigh, animationSpec = spec, label = "themeSurfaceContainerHigh").value,
        surfaceContainerHighest = animateColorAsState(surfaceContainerHighest, animationSpec = spec, label = "themeSurfaceContainerHighest").value,
        surfaceDim = animateColorAsState(surfaceDim, animationSpec = spec, label = "themeSurfaceDim").value,
        outline = animateColorAsState(outline, animationSpec = spec, label = "themeOutline").value,
        outlineVariant = animateColorAsState(outlineVariant, animationSpec = spec, label = "themeOutlineVariant").value,
        inverseOnSurface = animateColorAsState(inverseOnSurface, animationSpec = spec, label = "themeInverseOnSurface").value,
        inverseSurface = animateColorAsState(inverseSurface, animationSpec = spec, label = "themeInverseSurface").value,
        inversePrimary = animateColorAsState(inversePrimary, animationSpec = spec, label = "themeInversePrimary").value
    )
}

/** Same morphing treatment for the semantic (extra) color scheme. */
@Composable
private fun ExtraColorsScheme.withThemeTransition(enabled: Boolean, duration: Int): ExtraColorsScheme {
    if (!enabled) return this
    val spec = tween<Color>(duration)
    return ExtraColorsScheme(
        link = animateColorAsState(link, animationSpec = spec, label = "themeExtraLink").value,
        success = animateColorAsState(success, animationSpec = spec, label = "themeExtraSuccess").value,
        pending = animateColorAsState(pending, animationSpec = spec, label = "themeExtraPending").value,
        due = animateColorAsState(due, animationSpec = spec, label = "themeExtraDue").value,
        new = animateColorAsState(new, animationSpec = spec, label = "themeExtraNew").value
    )
}

/** Same morphing treatment for the shared surface tokens. */
@Composable
private fun SurfaceColors.withThemeTransition(enabled: Boolean, duration: Int): SurfaceColors {
    if (!enabled) return this
    val spec = tween<Color>(duration)
    return copy(
        background = animateColorAsState(background, animationSpec = spec, label = "surfaceBackground").value,
        surface = animateColorAsState(surface, animationSpec = spec, label = "surfaceSurface").value,
        surfaceElevated = animateColorAsState(surfaceElevated, animationSpec = spec, label = "surfaceElevated").value,
        surfaceInteractive = animateColorAsState(surfaceInteractive, animationSpec = spec, label = "surfaceInteractive").value,
        border = animateColorAsState(border, animationSpec = spec, label = "surfaceBorder").value,
        textPrimary = animateColorAsState(textPrimary, animationSpec = spec, label = "surfaceTextPrimary").value,
        textSecondary = animateColorAsState(textSecondary, animationSpec = spec, label = "surfaceTextSecondary").value,
        textMuted = animateColorAsState(textMuted, animationSpec = spec, label = "surfaceTextMuted").value,
        textInverse = animateColorAsState(textInverse, animationSpec = spec, label = "surfaceTextInverse").value,
        kanjiKnown = animateColorAsState(kanjiKnown, animationSpec = spec, label = "surfaceKanjiKnown").value,
        kanjiLearning = animateColorAsState(kanjiLearning, animationSpec = spec, label = "surfaceKanjiLearning").value,
        kanjiNew = animateColorAsState(kanjiNew, animationSpec = spec, label = "surfaceKanjiNew").value,
        kanjiDue = animateColorAsState(kanjiDue, animationSpec = spec, label = "surfaceKanjiDue").value,
        kanjiMastered = animateColorAsState(kanjiMastered, animationSpec = spec, label = "surfaceKanjiMastered").value,
        frequencyTiers = frequencyTiers.map { c ->
            animateColorAsState(c, animationSpec = spec, label = "surfaceFrequencyTier").value
        },
        kanjiSuspended = animateColorAsState(kanjiSuspended, animationSpec = spec, label = "surfaceKanjiSuspended").value
    )
}

/** Same morphing treatment for semantic color tokens. */
@Composable
private fun KaiteyoSemanticColors.withThemeTransition(enabled: Boolean, duration: Int): KaiteyoSemanticColors {
    if (!enabled) return this
    val spec = tween<Color>(duration)
    @Composable fun anim(c: Color, label: String) = animateColorAsState(c, animationSpec = spec, label = label).value
    return KaiteyoSemanticColors(
        reviewAgain = anim(reviewAgain, "semReviewAgain"),
        reviewHard = anim(reviewHard, "semReviewHard"),
        reviewGood = anim(reviewGood, "semReviewGood"),
        reviewEasy = anim(reviewEasy, "semReviewEasy"),
        cardNew = anim(cardNew, "semCardNew"),
        cardLearning = anim(cardLearning, "semCardLearning"),
        cardYoung = anim(cardYoung, "semCardYoung"),
        cardMature = anim(cardMature, "semCardMature"),
        cardRelearning = anim(cardRelearning, "semCardRelearning"),
        cardSuspended = anim(cardSuspended, "semCardSuspended"),
        cardBuried = anim(cardBuried, "semCardBuried"),
        cardArchived = anim(cardArchived, "semCardArchived"),
        success = anim(success, "semSuccess"),
        warning = anim(warning, "semWarning"),
        error = anim(error, "semError"),
        info = anim(info, "semInfo"),
        favorite = anim(favorite, "semFavorite"),
        due = anim(due, "semDue"),
        new = anim(new, "semNew"),
        suspended = anim(suspended, "semSuspended"),
        muted = anim(muted, "semMuted"),
        flagRed = anim(flagRed, "semFlagRed"),
        flagOrange = anim(flagOrange, "semFlagOrange"),
        flagYellow = anim(flagYellow, "semFlagYellow"),
        flagGreen = anim(flagGreen, "semFlagGreen"),
        flagBlue = anim(flagBlue, "semFlagBlue"),
        flagPurple = anim(flagPurple, "semFlagPurple"),
        activityReview = anim(activityReview, "semActReview"),
        activityReviewFailed = anim(activityReviewFailed, "semActReviewFailed"),
        activityEdit = anim(activityEdit, "semActEdit"),
        activityImport = anim(activityImport, "semActImport"),
        activityExport = anim(activityExport, "semActExport"),
        activityTag = anim(activityTag, "semActTag"),
        activityFlag = anim(activityFlag, "semActFlag"),
        activityNote = anim(activityNote, "semActNote"),
        activityStudy = anim(activityStudy, "semActStudy"),
        activitySystem = anim(activitySystem, "semActSystem"),
        difficultyEasy = anim(difficultyEasy, "semDiffEasy"),
        difficultyMedium = anim(difficultyMedium, "semDiffMedium"),
        difficultyHard = anim(difficultyHard, "semDiffHard"),
        dayColor = anim(dayColor, "semDayColor"),
        nightColor = anim(nightColor, "semNightColor"),
        automaticBackup = anim(automaticBackup, "semAutoBackup"),
        favoriteStar = anim(favoriteStar, "semFavStar")
    )
}

/** Convenience accessor for semantic color tokens from composables. */
val kaiteyoSemantic: KaiteyoSemanticColors
    @Composable get() = LocalKaiteyoSemanticColors.current

/**
 * Luminance-adaptive study-state color accessor. Returns the kanji study-state
 * color for the supplied [state] under the current surface tokens, so dictionary,
 * graph and list surfaces read study state from theme tokens instead of
 * hardcoding colors that break under Sepia/Cream/Paper/Midnight.
 */
@Composable
fun MaterialTheme.studyColorFor(state: ua.syt0r.kanji.core.knowledge.StudyState): Color {
    val surfaces = LocalSurfaceColors.current
    return when (state) {
        ua.syt0r.kanji.core.knowledge.StudyState.Known -> surfaces.kanjiKnown
        ua.syt0r.kanji.core.knowledge.StudyState.Learning -> surfaces.kanjiLearning
        ua.syt0r.kanji.core.knowledge.StudyState.Due -> surfaces.kanjiDue
        ua.syt0r.kanji.core.knowledge.StudyState.Mastered -> surfaces.kanjiMastered
        ua.syt0r.kanji.core.knowledge.StudyState.Suspended -> surfaces.kanjiSuspended
        ua.syt0r.kanji.core.knowledge.StudyState.New,
        ua.syt0r.kanji.core.knowledge.StudyState.Relearning -> surfaces.kanjiNew
    }
}

/** Frequency band → theme-aware color via the live SurfaceColors tokens. */
@Composable
fun MaterialTheme.frequencyColorFor(band: ua.syt0r.kanji.core.knowledge.FrequencyBand?): Color {
    val isDark = LocalBaseMode.current.isDarkMode
    return frequencyColorForBand(band, isDark)
}

/**
 * Single-source frequency-band color. Used by the composable
 * [frequencyColorFor] and directly by tests so the green→red ramp is defined
 * in exactly one place and adapts to every base mode.
 */
fun frequencyColorForBand(
    band: ua.syt0r.kanji.core.knowledge.FrequencyBand?,
    isDark: Boolean
): Color = if (band == null) FrequencyColorFallback
    else frequencyBandColors(isDark).getOrElse(band.ordinal) { FrequencyColorFallback }

private val FrequencyColorFallback = Color(0xFF888888)

/** Same morphing treatment for the accent scheme (name/preview stay static). */
@Composable
private fun KaiteyoAccentScheme.withThemeTransition(enabled: Boolean, duration: Int): KaiteyoAccentScheme {
    if (!enabled) return this
    val spec = tween<Color>(duration)
    return copy(
        primary = animateColorAsState(primary, animationSpec = spec, label = "accentPrimary").value,
        primaryDark = animateColorAsState(primaryDark, animationSpec = spec, label = "accentPrimaryDark").value,
        secondary = animateColorAsState(secondary, animationSpec = spec, label = "accentSecondary").value,
        secondaryDark = animateColorAsState(secondaryDark, animationSpec = spec, label = "accentSecondaryDark").value,
        onPrimary = animateColorAsState(onPrimary, animationSpec = spec, label = "accentOnPrimary").value,
        onSecondary = animateColorAsState(onSecondary, animationSpec = spec, label = "accentOnSecondary").value,
        tertiary = tertiary?.let {
            animateColorAsState(it, animationSpec = spec, label = "accentTertiary").value
        },
        gradientStart = gradientStart?.let {
            animateColorAsState(it, animationSpec = spec, label = "accentGradientStart").value
        },
        gradientEnd = gradientEnd?.let {
            animateColorAsState(it, animationSpec = spec, label = "accentGradientEnd").value
        }
    )
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
