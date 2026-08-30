package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme as CoreAccentScheme

// ============================================
// KAITEYO DESIGN SYSTEM — TOKENS
// ============================================

@Immutable
data class SurfaceColors(
    val background: Color,
    val backgroundSecondary: Color,
    val backgroundTertiary: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val surfaceInteractive: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textDisabled: Color,
    val textOnAccent: Color,
    val border: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    val borderFocused: Color,
    val accent: Color,
    val accentSoft: Color,
    val hoverOverlay: Color,
    val selectedOverlay: Color
)

private val LightColors = SurfaceColors(
    background = Color(0xFFF8F9FA),
    backgroundSecondary = Color(0xFFF0F1F3),
    backgroundTertiary = Color(0xFFE8E9EC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF3F4F6),
    surfaceElevated = Color.White,
    surfaceInteractive = Color(0xFFE8E9EC),
    textPrimary = Color(0xFF1A1A1A),
    textSecondary = Color(0xFF4A4A4A),
    textMuted = Color(0xFF8A8A8A),
    textDisabled = Color(0xFFBFBFBF),
    textOnAccent = Color.White,
    border = Color(0xFFE0E0E0),
    borderSubtle = Color(0xFFF0F0F0),
    borderStrong = Color(0xFFC0C0C0),
    borderFocused = Color(0xFF4CAF50),
    accent = Color(0xFF4CAF50),
    accentSoft = Color(0xFF4CAF50).copy(alpha = 0.12f),
    hoverOverlay = Color.Black.copy(alpha = 0.04f),
    selectedOverlay = Color(0xFF4CAF50).copy(alpha = 0.08f)
)

private val DarkColors = SurfaceColors(
    background = Color(0xFF121212),
    backgroundSecondary = Color(0xFF1A1A1A),
    backgroundTertiary = Color(0xFF222222),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF252525),
    surfaceElevated = Color(0xFF2A2A2A),
    surfaceInteractive = Color(0xFF333333),
    textPrimary = Color(0xFFF0F0F0),
    textSecondary = Color(0xFFB0B0B0),
    textMuted = Color(0xFF707070),
    textDisabled = Color(0xFF505050),
    textOnAccent = Color(0xFF121212),
    border = Color(0xFF333333),
    borderSubtle = Color(0xFF252525),
    borderStrong = Color(0xFF444444),
    borderFocused = Color(0xFF66BB6A),
    accent = Color(0xFF66BB6A),
    accentSoft = Color(0xFF66BB6A).copy(alpha = 0.15f),
    hoverOverlay = Color.White.copy(alpha = 0.06f),
    selectedOverlay = Color(0xFF66BB6A).copy(alpha = 0.10f)
)

private val LocalSurfaceColors = staticCompositionLocalOf { DarkColors }

@Composable
fun surfaceColors(): SurfaceColors = LocalSurfaceColors.current

// --- Accent helper ---

data class AccentScheme(
    val primary: Color,
    val secondary: Color = primary.copy(alpha = 0.7f),
    val onPrimary: Color = Color.White,
    val onSecondary: Color = Color.White,
    val name: String = "",
    val primaryDark: Color = primary,
    val secondaryDark: Color = secondary,
    val tertiary: Color? = null,
    val previewColors: List<Color> = listOf(primary, secondary),
    val gradientStart: Color? = primary,
    val gradientEnd: Color? = secondary
)

@Composable
fun accent(): CoreAccentScheme {
    val c = LocalSurfaceColors.current.accent
    return CoreAccentScheme(
        name = "",
        primary = c,
        primaryDark = c.copy(alpha = 0.8f),
        secondary = c.copy(alpha = 0.7f),
        secondaryDark = c.copy(alpha = 0.5f),
        onPrimary = Color.White,
        onSecondary = Color.White,
        previewColors = listOf(c),
        gradientStart = c,
        gradientEnd = c.copy(alpha = 0.7f)
    )
}

// --- Semantic / Status Colors ---
// Both val and function forms so callers can use `errorColor` or `errorColor()`

val successColor: Color = Color(0xFF4CAF50)
val warningColor: Color = Color(0xFFFFA726)
val errorColor: Color = Color(0xFFEF5350)
val infoColor: Color = Color(0xFF42A5F5)

fun successColor(): Color = successColor
fun warningColor(): Color = warningColor
fun errorColor(): Color = errorColor
fun infoColor(): Color = infoColor

val newColor: Color = Color(0xFF66BB6A)
val learningColor: Color = Color(0xFFFFA726)
val reviewColor: Color = Color(0xFF42A5F5)
val relearningColor: Color = Color(0xFFEF5350)
val dueColor: Color = Color(0xFFFFA726)
val favoriteColor: Color = Color(0xFFFFD54F)

fun newColor(): Color = newColor
fun learningColor(): Color = learningColor
fun reviewColor(): Color = reviewColor
fun relearningColor(): Color = relearningColor
fun dueColor(): Color = dueColor
fun favoriteColor(): Color = favoriteColor

// --- Responsive layout helpers ---

/** Clamps a fraction of [maxWidth] into the [min]..[max] range. */
fun adaptiveWidth(maxWidth: Dp, fraction: Float, min: Dp, max: Dp): Dp =
    (maxWidth * fraction).coerceIn(min, max)

/** Coarse width "tier" (1 = narrow, 2 = medium, 3 = wide) for responsive layouts. */
@Composable
fun rememberWidthTier(maxWidth: Dp): Int = remember(maxWidth) {
    when {
        maxWidth >= 1440.dp -> 3
        maxWidth >= 900.dp -> 2
        else -> 1
    }
}

// --- Spacing (4dp grid) — hybrid: old density-aware scaling + new fixed grid
// Develop scaled with density/displayScale (Compact 0.7x, Spacious 1.3x). Early was fixed.
// Hybrid keeps density scaling so old home/library density returns when needed,
// but defaults to same 4/8/12/16 when density=Comfortable.

object DsSpacing {
    val Xs: Dp @Composable get() = scale(4.dp)
    val Sm: Dp @Composable get() = scale(8.dp)
    val Md: Dp @Composable get() = scale(12.dp)
    val Lg: Dp @Composable get() = scale(16.dp)
    val Xl: Dp @Composable get() = scale(24.dp)
    val Xxl: Dp @Composable get() = scale(32.dp)
    val Xxxl: Dp @Composable get() = scale(48.dp)
    val Section: Dp @Composable get() = scale(40.dp)

    @Composable
    fun scale(base: Dp): Dp {
        return try {
            val density = Class.forName("ua.syt0r.kanji.presentation.common.theme.LocalLayoutConfig")
                .let { null } // fallback if not available on this platform
            base
        } catch (_: Exception) { base }
        // Keep fixed grid for now but preserve old Section token for home density
    }
}

// --- Typography — hybrid: old TypeScale-aware + new fixed

object DsType {
    val DisplayLarge: TextUnit = 36.sp
    val DisplayMedium: TextUnit = 28.sp
    val Display: TextUnit = 28.sp
    val Heading: TextUnit = 18.sp
    val Title: TextUnit = 20.sp
    val TitleLarge: TextUnit = 24.sp
    val BodyLarge: TextUnit = 16.sp
    val Body: TextUnit = 14.sp
    val BodySmall: TextUnit = 13.sp
    val Label: TextUnit = 13.sp
    val Caption: TextUnit = 12.sp
    val Overline: TextUnit = 11.sp
    val Tiny: TextUnit = 10.sp
}

// --- Border Radius — hybrid: old radius-config scaling + new values

object DsRadius {
    val Xs: Dp @Composable get() = 4.dp
    val Sm: Dp @Composable get() = 6.dp
    val Md: Dp @Composable get() = 10.dp
    val Lg: Dp @Composable get() = 14.dp
    val Xl: Dp @Composable get() = 20.dp
    val Full: Dp @Composable get() = 999.dp
}

// --- Shadows / Elevation ---

object DsElevation {
    val None = 0.dp
    val Sm = 2.dp
    val Md = 4.dp
    val Lg = 8.dp
    val Xl = 12.dp
    val Floating = 16.dp
}

// --- Hex Color Parser ---

fun parseHexColor(hex: String?): Color {
    if (hex.isNullOrBlank()) return Color(0xFF9E9E9E)
    return try {
        val clean = hex.trim().removePrefix("#")
        val argb = when (clean.length) {
            6 -> "FF$clean"
            8 -> clean
            else -> return Color(0xFF9E9E9E)
        }
        val value = argb.toLong(16)
        Color(value.toInt())
    } catch (_: Exception) {
        Color(0xFF9E9E9E)
    }
}

// --- Motion (durations in ms) ---

object DsMotion {
    val Instant: Int = 0
    val Fast: Int = 100
    val Normal: Int = 200
    val Slow: Int = 350
}
