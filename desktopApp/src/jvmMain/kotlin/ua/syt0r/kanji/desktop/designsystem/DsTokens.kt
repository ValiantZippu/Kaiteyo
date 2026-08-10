package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.LocalAnimationConfig
import ua.syt0r.kanji.presentation.common.theme.LocalLayoutConfig
import ua.syt0r.kanji.presentation.common.theme.LocalRadiusConfig
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.LocalTypeScale
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.common.theme.UIDensity
import ua.syt0r.kanji.presentation.common.theme.dueOrange
import ua.syt0r.kanji.presentation.common.theme.favoriteYellow
import ua.syt0r.kanji.presentation.common.theme.semanticError
import ua.syt0r.kanji.presentation.common.theme.semanticInfo
import ua.syt0r.kanji.presentation.common.theme.semanticNew
import ua.syt0r.kanji.presentation.common.theme.semanticSuccess
import ua.syt0r.kanji.presentation.common.theme.semanticWarning

// ============================================
// KAITEYO DESIGN SYSTEM — TOKENS
// Single source of truth for spacing, radii,
// durations and type scale. Nothing in the UI
// hardcodes values; everything reads tokens that
// scale with density / radius / theme config.
// ============================================

/** Spacing scale, scaled by the active density multiplier and display zoom. */
object DsSpacing {
    val Xs: Dp @Composable get() = scale(4.dp)
    val Sm: Dp @Composable get() = scale(8.dp)
    val Md: Dp @Composable get() = scale(12.dp)
    val Lg: Dp @Composable get() = scale(16.dp)
    val Xl: Dp @Composable get() = scale(24.dp)
    val Xxl: Dp @Composable get() = scale(32.dp)
    val Section: Dp @Composable get() = scale(40.dp)

    @Composable
    fun scale(base: Dp): Dp {
        val multiplier = densityMultiplier()
        val zoom = LocalLayoutConfig.current.displayScale
        return base * multiplier * zoom
    }

    @Composable
    fun densityMultiplier(): Float = when (LocalLayoutConfig.current.density) {
        UIDensity.Compact -> 0.7f
        UIDensity.Comfortable -> 1.0f
        UIDensity.Spacious -> 1.3f
    }
}

/** Corner radius tokens, scaled by radius config. */
object DsRadius {
    val Xs: Dp @Composable get() = radius(4.dp)
    val Sm: Dp @Composable get() = radius(8.dp)
    val Md: Dp @Composable get() = radius(12.dp)
    val Lg: Dp @Composable get() = radius(16.dp)
    val Xl: Dp @Composable get() = radius(24.dp)
    val Full: Dp @Composable get() = radius(999.dp)

    @Composable
    fun radius(base: Dp): Dp {
        val config = LocalRadiusConfig.current
        val multiplier = config.style.globalMultiplier * (config.customRadius ?: 1f)
        return base * multiplier
    }
}

/** Animation durations, honoring reduced-motion and speed config. */
object DsMotion {
    const val Fast = 120
    const val Normal = 240
    const val Slow = 380

    @Composable
    fun duration(base: Int): Int {
        val config = LocalAnimationConfig.current
        if (config.reducedMotion) return 0
        return (base * config.speed.multiplier).toInt()
    }
}

/** Type scale, honoring font scale, display zoom and title scale. */
object DsType {
    val Caption: TextUnit @Composable get() = scaled(11.sp, title = false)
    val Label: TextUnit @Composable get() = scaled(12.sp, title = false)
    val Body: TextUnit @Composable get() = scaled(14.sp, title = false)
    val BodyLarge: TextUnit @Composable get() = scaled(16.sp, title = false)
    val Title: TextUnit @Composable get() = scaled(18.sp, title = true)
    val Heading: TextUnit @Composable get() = scaled(22.sp, title = true)
    val Display: TextUnit @Composable get() = scaled(28.sp, title = true)

    @Composable
    fun scaled(base: TextUnit, title: Boolean = false): TextUnit {
        val ts = LocalTypeScale.current
        val zoom = LocalLayoutConfig.current.displayScale
        val scale = ts.fontScale * zoom * (if (title) ts.titleScale else 1f)
        return base * scale
    }
}

/** Elevation tokens. */
object DsElevation {
    val Flat = 0.dp
    val Raised = 2.dp
    val Floating = 8.dp
    val Overlay = 16.dp
}

/** Semantic colors resolved from the active theme. */
object DsColors {
    val surface: SurfaceColors
        @Composable get() = LocalSurfaceColors.current
}

/**
 * Semantic palette used by data viz, badges and status colors across the
 * suite. Reads the shared Kaiteyo tokens so every screen reports the same
 * success / warning / info / danger tones and follows theme changes.
 */
object DsSemantic {
    val Success: Color
        @Composable get() = semanticSuccess
    val Warning: Color
        @Composable get() = semanticWarning
    val Error: Color
        @Composable get() = semanticError
    val Info: Color
        @Composable get() = semanticInfo
    val New: Color
        @Composable get() = semanticNew
    val Due: Color
        @Composable get() = dueOrange
    val Favorite: Color
        @Composable get() = favoriteYellow
}

/** Convenience accessors so views stay token-driven. */
@Composable
fun successColor(): Color = DsSemantic.Success

@Composable
fun warningColor(): Color = DsSemantic.Warning

@Composable
fun errorColor(): Color = DsSemantic.Error

@Composable
fun infoColor(): Color = DsSemantic.Info

@Composable
fun newColor(): Color = DsSemantic.New

@Composable
fun dueColor(): Color = DsSemantic.Due

@Composable
fun favoriteColor(): Color = DsSemantic.Favorite

/** Typed access to accent colors for components. */
@Composable
fun accent() = ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent.current

@Composable
fun surfaceColors(): SurfaceColors = LocalSurfaceColors.current
