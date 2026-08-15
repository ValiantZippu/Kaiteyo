package ua.syt0r.kanji.presentation.common.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

// ============================================
// KMP-SAFE COLOR CODECS
// Used by the persisted ThemeSettings (custom
// accent colors survive restarts) and the Theme
// Studio export/import (clipboard JSON). No
// java.* / JVM-only APIs — safe on all targets.
// ============================================

/** `#RRGGBB` (optionally `#RRGGBBAA`) — KMP-safe, no String.format. */
fun Color.toHexString(includeAlpha: Boolean = false): String {
    fun byte(value: Float): String =
        (value * 255).roundToInt().coerceIn(0, 255).toString(16).padStart(2, '0').uppercase()
    val base = "#${byte(red)}${byte(green)}${byte(blue)}"
    return if (includeAlpha) "$base${byte(alpha)}" else base
}

/** Parses `#RRGGBB` / `#RRGGBBAA` back into a [Color], or null on garbage. */
fun parseColorHex(hex: String): Color? {
    val h = hex.removePrefix("#")
    val value = h.toLongOrNull(16) ?: return null
    return when (h.length) {
        6 -> Color(
            ((value shr 16) and 0xFF) / 255f,
            ((value shr 8) and 0xFF) / 255f,
            (value and 0xFF) / 255f
        )
        8 -> Color(
            ((value shr 24) and 0xFF) / 255f,
            ((value shr 16) and 0xFF) / 255f,
            ((value shr 8) and 0xFF) / 255f,
            (value and 0xFF) / 255f
        )
        else -> null
    }
}


// ============================================
// KAITEYO v1.2.0 — Color System
// "A quiet futuristic studio for mastering Japanese"
// 6 Base Themes · 7 Accent Themes · Custom Creator
// ============================================

// --- Base Mode Backgrounds ---

// OLED Black (default)
val backgroundOledBlack = Color(0xFF050505)
val surfaceOledDark = Color(0xFF0D0D0D)
val surfaceOledMedium = Color(0xFF101010)
val surfaceOledLight = Color(0xFF1A1A1A)

// Dark Gray
val backgroundDarkGray = Color(0xFF121212)
val surfaceDarkGrayDark = Color(0xFF1A1A1A)
val surfaceDarkGrayMedium = Color(0xFF242424)
val surfaceDarkGrayLight = Color(0xFF2E2E2E)

// Light Mode
val backgroundLight = Color(0xFFF5F5F5)
val surfaceLightDark = Color(0xFFEEEEEE)
val surfaceLightMedium = Color(0xFFE8E8E8)
val surfaceLightLight = Color(0xFFFCFCFC)

// Sepia (Reading Mode)
val backgroundSepia = Color(0xFFF5F0E8)
val surfaceSepiaDark = Color(0xFFEDE5D8)
val surfaceSepiaMedium = Color(0xFFE5DCC8)
val surfaceSepiaLight = Color(0xFFF8F4EE)
val textSepiaPrimary = Color(0xFF3D3028)
val textSepiaSecondary = Color(0xFF7A6B5D)
val textSepiaMuted = Color(0xFFA89888)
val borderSepia = Color(0xFFD4C8B8)

// --- Shared Surface Colors ---

val surfaceBorder = Color(0xFF2A2A2A)
val surfaceBorderLight = Color(0xFFD0D0D0)
val surfaceHover = Color(0xFF1A1A1A)
val surfaceActive = Color(0xFF222222)
val overlayColor = Color(0x99000000)
val surfaceBorderSubtle = Color(0x33FFFFFF)

// ============================================
// ACCENT THEMES
// ============================================

// --- KAITEYO SIGNATURE (Default) ---
val kaiteyoPrimary = Color(0xFFC2FC8B)
val kaiteyoPrimaryDark = Color(0xFF9CE85E)
val kaiteyoSecondary = Color(0xFFFEAB57)
val kaiteyoSecondaryDark = Color(0xFFFD8A2E)
val kaiteyoTertiary = Color(0xFF7BC8FF)
val kaiteyoOnPrimary = Color(0xFF050505)
val kaiteyoOnSecondary = Color(0xFF050505)

// --- COTTON CANDY ---
val cottonCandyPrimary = Color(0xFFD4A5F0)
val cottonCandyPrimaryDark = Color(0xFFC084E8)
val cottonCandySecondary = Color(0xFFFFB5C5)
val cottonCandySecondaryDark = Color(0xFFFF8FA5)
val cottonCandyTertiary = Color(0xFFA0D2FF)
val cottonCandyOnPrimary = Color(0xFF1A1A2E)
val cottonCandyOnSecondary = Color(0xFF1A1A2E)

// --- OCEAN ---
val oceanPrimary = Color(0xFF00D4AA)
val oceanPrimaryDark = Color(0xFF00B894)
val oceanSecondary = Color(0xFF00A8FF)
val oceanSecondaryDark = Color(0xFF0088CC)
val oceanTertiary = Color(0xFF0D47A1)
val oceanOnPrimary = Color(0xFF050505)
val oceanOnSecondary = Color(0xFF050505)

// --- FOREST ---
val forestPrimary = Color(0xFF81C784)
val forestPrimaryDark = Color(0xFF66BB6A)
val forestSecondary = Color(0xFFA5D6A7)
val forestSecondaryDark = Color(0xFF81C784)
val forestTertiary = Color(0xFF5D4037)
val forestOnPrimary = Color(0xFF1A2E1A)
val forestOnSecondary = Color(0xFF1A2E1A)

// --- SUNSET ---
val sunsetPrimary = Color(0xFFFF6B6B)
val sunsetPrimaryDark = Color(0xFFE05555)
val sunsetSecondary = Color(0xFFFFB347)
val sunsetSecondaryDark = Color(0xFFE09D3A)
val sunsetTertiary = Color(0xFFFF8C69)
val sunsetOnPrimary = Color(0xFF1A0A0A)
val sunsetOnSecondary = Color(0xFF1A0A0A)

// --- LAVENDER ---
val lavenderPrimary = Color(0xFFB39DDB)
val lavenderPrimaryDark = Color(0xFF9575CD)
val lavenderSecondary = Color(0xFFCE93D8)
val lavenderSecondaryDark = Color(0xFFBA68C8)
val lavenderTertiary = Color(0xFF80CBC4)
val lavenderOnPrimary = Color(0xFF1A1A2E)
val lavenderOnSecondary = Color(0xFF1A1A2E)

// --- MONOCHROME ---
val monoPrimary = Color(0xFFE0E0E0)
val monoPrimaryDark = Color(0xFFBDBDBD)
val monoSecondary = Color(0xFF9E9E9E)
val monoSecondaryDark = Color(0xFF757575)
val monoTertiary = Color(0xFF616161)
val monoOnPrimary = Color(0xFF121212)
val monoOnSecondary = Color(0xFF121212)

// ============================================
// Accent Scheme Definition
// ============================================

data class KaiteyoAccentScheme(
    val name: String,
    val primary: Color,
    val primaryDark: Color,
    val secondary: Color,
    val secondaryDark: Color,
    val onPrimary: Color,
    val onSecondary: Color,
    val tertiary: Color? = null,
    val previewColors: List<Color>,
    val gradientStart: Color? = null,
    val gradientEnd: Color? = null
)

val AllAccentSchemes = listOf(
    KaiteyoAccentScheme(
        name = "Signature Pineapple",
        primary = kaiteyoPrimary, primaryDark = kaiteyoPrimaryDark,
        secondary = kaiteyoSecondary, secondaryDark = kaiteyoSecondaryDark,
        onPrimary = kaiteyoOnPrimary, onSecondary = kaiteyoOnSecondary,
        tertiary = kaiteyoTertiary,
        previewColors = listOf(kaiteyoPrimary, kaiteyoSecondary),
        gradientStart = kaiteyoPrimary, gradientEnd = kaiteyoSecondary
    ),
    KaiteyoAccentScheme(
        name = "Cotton Candy",
        primary = cottonCandyPrimary, primaryDark = cottonCandyPrimaryDark,
        secondary = cottonCandySecondary, secondaryDark = cottonCandySecondaryDark,
        onPrimary = cottonCandyOnPrimary, onSecondary = cottonCandyOnSecondary,
        tertiary = cottonCandyTertiary,
        previewColors = listOf(cottonCandyPrimary, cottonCandySecondary, cottonCandyTertiary),
        gradientStart = cottonCandyPrimary, gradientEnd = cottonCandySecondary
    ),
    KaiteyoAccentScheme(
        name = "Ocean",
        primary = oceanPrimary, primaryDark = oceanPrimaryDark,
        secondary = oceanSecondary, secondaryDark = oceanSecondaryDark,
        onPrimary = oceanOnPrimary, onSecondary = oceanOnSecondary,
        tertiary = oceanTertiary,
        previewColors = listOf(oceanTertiary, oceanSecondary, oceanPrimary),
        gradientStart = oceanTertiary, gradientEnd = oceanPrimary
    ),
    KaiteyoAccentScheme(
        name = "Forest",
        primary = forestPrimary, primaryDark = forestPrimaryDark,
        secondary = forestSecondary, secondaryDark = forestSecondaryDark,
        onPrimary = forestOnPrimary, onSecondary = forestOnSecondary,
        tertiary = forestTertiary,
        previewColors = listOf(forestTertiary, forestSecondary, forestPrimary),
        gradientStart = forestTertiary, gradientEnd = forestPrimary
    ),
    KaiteyoAccentScheme(
        name = "Sunset",
        primary = sunsetPrimary, primaryDark = sunsetPrimaryDark,
        secondary = sunsetSecondary, secondaryDark = sunsetSecondaryDark,
        onPrimary = sunsetOnPrimary, onSecondary = sunsetOnSecondary,
        tertiary = sunsetTertiary,
        previewColors = listOf(sunsetPrimary, sunsetSecondary, sunsetTertiary),
        gradientStart = sunsetPrimary, gradientEnd = sunsetSecondary
    ),
    KaiteyoAccentScheme(
        name = "Lavender",
        primary = lavenderPrimary, primaryDark = lavenderPrimaryDark,
        secondary = lavenderSecondary, secondaryDark = lavenderSecondaryDark,
        onPrimary = lavenderOnPrimary, onSecondary = lavenderOnSecondary,
        tertiary = lavenderTertiary,
        previewColors = listOf(lavenderPrimary, lavenderSecondary, lavenderTertiary),
        gradientStart = lavenderPrimary, gradientEnd = lavenderSecondary
    ),
    KaiteyoAccentScheme(
        name = "Monochrome",
        primary = monoPrimary, primaryDark = monoPrimaryDark,
        secondary = monoSecondary, secondaryDark = monoSecondaryDark,
        onPrimary = monoOnPrimary, onSecondary = monoOnSecondary,
        tertiary = monoTertiary,
        previewColors = listOf(monoPrimary, monoSecondary, monoTertiary),
        gradientStart = monoPrimary, gradientEnd = monoSecondary
    )
)

// ============================================
// Text Colors
// ============================================

val textPrimary = Color(0xFFF0F0F0)
val textSecondary = Color(0xFFA0A0A0)
val textMuted = Color(0xFF606060)
val textInverse = Color(0xFF050505)

val textPrimaryLight = Color(0xFF1A1A1A)
val textSecondaryLight = Color(0xFF606060)
val textMutedLight = Color(0xFFA0A0A0)
val textInverseLight = Color(0xFFF0F0F0)

// ============================================
// Semantic Colors
// ============================================

val semanticSuccess = Color(0xFFC2FC8B)
val semanticWarning = Color(0xFFFEAB57)
val semanticError = Color(0xFFFF6B6B)
val semanticInfo = Color(0xFF7BC8FF)
val semanticNew = Color(0xFFA78BFA)
val favoriteYellow = Color(0xFFFFD93D)
val dueOrange = Color(0xFFFF9F43)

// ============================================
// GRADIENT SYSTEM
// ============================================

data class KaiteyoGradient(
    val start: Color,
    val end: Color,
    val angle: Float = 0f,
    val intensity: Float = 1f
)

fun gradientForAccent(accent: KaiteyoAccentScheme): KaiteyoGradient {
    return KaiteyoGradient(
        start = accent.gradientStart ?: accent.primary,
        end = accent.gradientEnd ?: accent.secondary,
        angle = 45f,
        intensity = 1f
    )
}

// ============================================
// GLOW SYSTEM
// ============================================

data class KaiteyoGlow(
    val color: Color,
    val radius: Float = 0.15f,
    val opacity: Float = 0.15f,
    val intensity: Float = 1f
)

fun primaryGlow(accent: KaiteyoAccentScheme, intensity: Float = 1f): KaiteyoGlow = KaiteyoGlow(
    color = accent.primary, radius = 0.15f, opacity = 0.15f * intensity, intensity = intensity
)

fun secondaryGlow(accent: KaiteyoAccentScheme, intensity: Float = 1f): KaiteyoGlow = KaiteyoGlow(
    color = accent.secondary, radius = 0.12f, opacity = 0.12f * intensity, intensity = intensity
)

// ============================================
// Base Mode
// ============================================

enum class BaseMode(val displayName: String) {
    Oled("OLED Black"),
    Dark("Dark Gray"),
    Light("Light"),
    Sepia("Sepia")
}

fun surfaceForBaseMode(mode: BaseMode): SurfaceColors = when (mode) {
    BaseMode.Oled -> SurfaceColors(
        background = backgroundOledBlack, surface = surfaceOledDark,
        surfaceElevated = surfaceOledMedium, surfaceInteractive = surfaceOledLight,
        border = surfaceBorder, textPrimary = textPrimary,
        textSecondary = textSecondary, textMuted = textMuted, textInverse = textInverse
    )
    BaseMode.Dark -> SurfaceColors(
        background = backgroundDarkGray, surface = surfaceDarkGrayDark,
        surfaceElevated = surfaceDarkGrayMedium, surfaceInteractive = surfaceDarkGrayLight,
        border = surfaceBorder, textPrimary = textPrimary,
        textSecondary = textSecondary, textMuted = textMuted, textInverse = textInverse
    )
    BaseMode.Light -> SurfaceColors(
        background = backgroundLight, surface = surfaceLightDark,
        surfaceElevated = surfaceLightMedium, surfaceInteractive = surfaceLightLight,
        border = surfaceBorderLight, textPrimary = textPrimaryLight,
        textSecondary = textSecondaryLight, textMuted = textMutedLight, textInverse = textInverseLight
    )
    BaseMode.Sepia -> SurfaceColors(
        background = backgroundSepia, surface = surfaceSepiaDark,
        surfaceElevated = surfaceSepiaMedium, surfaceInteractive = surfaceSepiaLight,
        border = borderSepia, textPrimary = textSepiaPrimary,
        textSecondary = textSepiaSecondary, textMuted = textSepiaMuted, textInverse = textSepiaPrimary
    )
}

data class SurfaceColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceInteractive: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textInverse: Color
)