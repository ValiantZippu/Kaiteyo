package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.LocalLayoutConfig
import ua.syt0r.kanji.presentation.common.theme.LocalRadiusConfig
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.common.theme.UIDensity

// ============================================
// KAITEYO DESIGN SYSTEM — TOKENS
// Single source of truth for spacing, radii,
// durations and type scale. Nothing in the UI
// hardcodes values; everything reads tokens that
// scale with density / radius / theme config.
// ============================================

/** Spacing scale, scaled by the active density multiplier. */
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
        return base * multiplier
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
        val multiplier = LocalRadiusConfig.current.style.globalMultiplier
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
        val config = androidx.compose.runtime.remember { ua.syt0r.kanji.presentation.common.theme.AnimationConfig() }
        if (config.reducedMotion) return 0
        return (base * config.speed.multiplier).toInt()
    }
}

/** Type scale. */
object DsType {
    val Caption = 11.sp
    val Label = 12.sp
    val Body = 14.sp
    val BodyLarge = 16.sp
    val Title = 18.sp
    val Heading = 22.sp
    val Display = 28.sp
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

/** Typed access to accent colors for components. */
@Composable
fun accent() = ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent.current

@Composable
fun surfaceColors(): SurfaceColors = LocalSurfaceColors.current
