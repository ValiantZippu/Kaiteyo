package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================
// KAITEYO DESIGN SYSTEM — TOKENS
// Single source of truth for colors, spacing,
// and typography. Every component must consume
// these — never hardcode values.
// ============================================

// --- Surface Colors ---

@Immutable
data class SurfaceColors(
    val background: Color,
    val backgroundSecondary: Color,
    val backgroundTertiary: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
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

@Composable
fun ProvideSurfaceColors(
    colors: SurfaceColors = DarkColors,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalSurfaceColors provides colors) {
        content()
    }
}

// --- Semantic / Status Colors ---

val accent: Color
    get() = LocalSurfaceColors.current.accent

val successColor: Color = Color(0xFF4CAF50)
val warningColor: Color = Color(0xFFFFA726)
val errorColor: Color = Color(0xFFEF5350)
val infoColor: Color = Color(0xFF42A5F5)

val newColor: Color = Color(0xFF66BB6A)
val learningColor: Color = Color(0xFFFFA726)
val reviewColor: Color = Color(0xFF42A5F5)
val relearningColor: Color = Color(0xFFEF5350)
val dueColor: Color = Color(0xFFFFA726)

// --- Spacing (4dp grid) ---

object DsSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val Xxl = 32.dp
    val Xxxl = 48.dp
}

// --- Typography ---

object DsType {
    val DisplayLarge: TextUnit = 36.sp
    val DisplayMedium: TextUnit = 28.sp
    val Title: TextUnit = 20.sp
    val TitleLarge: TextUnit = 24.sp
    val BodyLarge: TextUnit = 16.sp
    val Body: TextUnit = 14.sp
    val BodySmall: TextUnit = 13.sp
    val Caption: TextUnit = 12.sp
    val Overline: TextUnit = 11.sp
    val Tiny: TextUnit = 10.sp
}

// --- Border Radius ---

object DsRadius {
    val Sm = 6.dp
    val Md = 10.dp
    val Lg = 14.dp
    val Xl = 20.dp
    val Full = 999.dp
}

// --- Shadows / Elevation ---

object DsElevation {
    val None = 0.dp
    val Sm = 2.dp
    val Md = 4.dp
    val Lg = 8.dp
    val Xl = 12.dp
}
