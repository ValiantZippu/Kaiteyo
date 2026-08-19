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

// --- Cream (warm paper) ---
val backgroundCream = Color(0xFFF7F3E8)
val surfaceCreamDark = Color(0xFFEDE6D4)
val surfaceCreamMedium = Color(0xFFE5DCC0)
val surfaceCreamLight = Color(0xFFFAF7F0)
val textCreamPrimary = Color(0xFF3A2F22)
val textCreamSecondary = Color(0xFF6B5B47)
val textCreamMuted = Color(0xFF988A75)
val borderCream = Color(0xFFDED1BC)

// --- Paper (clean off-white) ---
val backgroundPaper = Color(0xFFFCFAF5)
val surfacePaperDark = Color(0xFFF5F2E8)
val surfacePaperMedium = Color(0xFFEDE9DE)
val surfacePaperLight = Color(0xFFFFFFFF)
val textPaperPrimary = Color(0xFF2A2A2A)
val textPaperSecondary = Color(0xFF575757)
val textPaperMuted = Color(0xFF888888)
val borderPaper = Color(0xFFE6E0D4)

// --- Midnight (deep dark blue) ---
val backgroundMidnight = Color(0xFF0A0D1A)
val surfaceMidnightDark = Color(0xFF121622)
val surfaceMidnightMedium = Color(0xFF1A1F30)
val surfaceMidnightLight = Color(0xFF232940)
val textMidnightPrimary = Color(0xFFEAEAFF)
val textMidnightSecondary = Color(0xFFB8B8D0)
val textMidnightMuted = Color(0xFF808098)
val borderMidnight = Color(0xFF2A324A)

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
// SEMANTIC COLOR TOKENS
// Theme-aware replacements for hardcoded Color(0xFF...) values.
// Follows the heatmap philosophy: accent-derived, surface-layered,
// consistent across every screen.
// ============================================

data class KaiteyoSemanticColors(
    // --- Review actions (again/hard/good/easy) ---
    val reviewAgain: Color,
    val reviewHard: Color,
    val reviewGood: Color,
    val reviewEasy: Color,
    // --- Card status ---
    val cardNew: Color,
    val cardLearning: Color,
    val cardYoung: Color,
    val cardMature: Color,
    val cardRelearning: Color,
    val cardSuspended: Color,
    val cardBuried: Color,
    val cardArchived: Color,
    // --- Semantic indicators ---
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
    val favorite: Color,
    val due: Color,
    val new: Color,
    val suspended: Color,
    val muted: Color,
    // --- Card flags ---
    val flagRed: Color,
    val flagOrange: Color,
    val flagYellow: Color,
    val flagGreen: Color,
    val flagBlue: Color,
    val flagPurple: Color,
    // --- Activity / history entry types ---
    val activityReview: Color,
    val activityReviewFailed: Color,
    val activityEdit: Color,
    val activityImport: Color,
    val activityExport: Color,
    val activityTag: Color,
    val activityFlag: Color,
    val activityNote: Color,
    val activityStudy: Color,
    val activitySystem: Color,
    // --- Difficulty tiers ---
    val difficultyEasy: Color,
    val difficultyMedium: Color,
    val difficultyHard: Color,
    // --- Day/Night indicator ---
    val dayColor: Color,
    val nightColor: Color,
    // --- Status badges ---
    val automaticBackup: Color,
    val favoriteStar: Color
)

val KaiteyoSemanticColorsDark = KaiteyoSemanticColors(
    reviewAgain = Color(0xFFFF6B6B),
    reviewHard = Color(0xFFFEAB57),
    reviewGood = Color(0xFFC2FC8B),
    reviewEasy = Color(0xFF7BC8FF),
    cardNew = Color(0xFF7BC8FF),
    cardLearning = Color(0xFFFEAB57),
    cardYoung = Color(0xFFC2FC8B),
    cardMature = Color(0xFF4CAF50),
    cardRelearning = Color(0xFFFF6B6B),
    cardSuspended = Color(0xFFB0B0B0),
    cardBuried = Color(0xFF9B59B6),
    cardArchived = Color(0xFF7F8C8D),
    success = semanticSuccess,
    warning = semanticWarning,
    error = semanticError,
    info = semanticInfo,
    favorite = Color(0xFFFFD93D),
    due = dueOrange,
    new = semanticNew,
    suspended = Color(0xFFB0B0B0),
    muted = textMuted,
    flagRed = Color(0xFFFF6B6B),
    flagOrange = Color(0xFFFEAB57),
    flagYellow = Color(0xFFFFD93D),
    flagGreen = Color(0xFFC2FC8B),
    flagBlue = Color(0xFF7BC8FF),
    flagPurple = Color(0xFFA78BFA),
    activityReview = Color(0xFF4CAF50),
    activityReviewFailed = Color(0xFFF44336),
    activityEdit = Color(0xFF2196F3),
    activityImport = Color(0xFF9C27B0),
    activityExport = Color(0xFF009688),
    activityTag = Color(0xFFFF9800),
    activityFlag = Color(0xFFFF5722),
    activityNote = Color(0xFF3F51B5),
    activityStudy = Color(0xFF00BCD4),
    activitySystem = Color(0xFF9E9E9E),
    difficultyEasy = Color(0xFFC2FC8B),
    difficultyMedium = Color(0xFFFEAB57),
    difficultyHard = Color(0xFFFF6B6B),
    dayColor = Color(0xFFFFD93D),
    nightColor = Color(0xFF7BC8FF),
    automaticBackup = Color(0xFFFEAB57),
    favoriteStar = Color(0xFFFFD93D)
)

val KaiteyoSemanticColorsLight = KaiteyoSemanticColors(
    reviewAgain = Color(0xFFE53935),
    reviewHard = Color(0xFFEF6C00),
    reviewGood = Color(0xFF2E7D32),
    reviewEasy = Color(0xFF1565C0),
    cardNew = Color(0xFF1565C0),
    cardLearning = Color(0xFFEF6C00),
    cardYoung = Color(0xFF2E7D32),
    cardMature = Color(0xFF1B5E20),
    cardRelearning = Color(0xFFE53935),
    cardSuspended = Color(0xFF757575),
    cardBuried = Color(0xFF7B1FA2),
    cardArchived = Color(0xFF616161),
    success = Color(0xFF2E7D32),
    warning = Color(0xFFEF6C00),
    error = Color(0xFFE53935),
    info = Color(0xFF1565C0),
    favorite = Color(0xFFF9A825),
    due = Color(0xFFEF6C00),
    new = Color(0xFF7B1FA2),
    suspended = Color(0xFF757575),
    muted = textMutedLight,
    flagRed = Color(0xFFE53935),
    flagOrange = Color(0xFFEF6C00),
    flagYellow = Color(0xFFF9A825),
    flagGreen = Color(0xFF2E7D32),
    flagBlue = Color(0xFF1565C0),
    flagPurple = Color(0xFF7B1FA2),
    activityReview = Color(0xFF2E7D32),
    activityReviewFailed = Color(0xFFE53935),
    activityEdit = Color(0xFF1565C0),
    activityImport = Color(0xFF7B1FA2),
    activityExport = Color(0xFF00796B),
    activityTag = Color(0xFFEF6C00),
    activityFlag = Color(0xFFE64A19),
    activityNote = Color(0xFF303F9F),
    activityStudy = Color(0xFF00838F),
    activitySystem = Color(0xFF757575),
    difficultyEasy = Color(0xFF2E7D32),
    difficultyMedium = Color(0xFFEF6C00),
    difficultyHard = Color(0xFFE53935),
    dayColor = Color(0xFFF9A825),
    nightColor = Color(0xFF1565C0),
    automaticBackup = Color(0xFFEF6C00),
    favoriteStar = Color(0xFFF9A825)
)

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
    Sepia("Sepia"),
    Cream("Cream"),
    Paper("Paper"),
    Midnight("Midnight")
}

/** True for base modes whose surface is dark (needs light text / dark Material scheme). */
val BaseMode.isDarkMode: Boolean get() = this == BaseMode.Oled || this == BaseMode.Dark || this == BaseMode.Midnight

fun surfaceForBaseMode(mode: BaseMode): SurfaceColors = when (mode) {
    BaseMode.Oled -> SurfaceColors(
        background = backgroundOledBlack, surface = surfaceOledDark,
        surfaceElevated = surfaceOledMedium, surfaceInteractive = surfaceOledLight,
        border = surfaceBorder, textPrimary = textPrimary,
        textSecondary = textSecondary, textMuted = textMuted, textInverse = textInverse,
        kanjiKnown = frequencyBandColors(true)[0],
        kanjiLearning = frequencyBandColors(true)[1],
        kanjiNew = textMuted,
        kanjiDue = semanticWarning,
        kanjiMastered = semanticSuccess,
        frequencyTiers = frequencyBandColors(true)
    )
    BaseMode.Dark -> SurfaceColors(
        background = backgroundDarkGray, surface = surfaceDarkGrayDark,
        surfaceElevated = surfaceDarkGrayMedium, surfaceInteractive = surfaceDarkGrayLight,
        border = surfaceBorder, textPrimary = textPrimary,
        textSecondary = textSecondary, textMuted = textMuted, textInverse = textInverse,
        kanjiKnown = frequencyBandColors(true)[0],
        kanjiLearning = frequencyBandColors(true)[1],
        kanjiNew = textMuted,
        kanjiDue = semanticWarning,
        kanjiMastered = semanticSuccess,
        frequencyTiers = frequencyBandColors(true)
    )
    BaseMode.Light -> SurfaceColors(
        background = backgroundLight, surface = surfaceLightDark,
        surfaceElevated = surfaceLightMedium, surfaceInteractive = surfaceLightLight,
        border = surfaceBorderLight, textPrimary = textPrimaryLight,
        textSecondary = textSecondaryLight, textMuted = textMutedLight, textInverse = textInverseLight,
        kanjiKnown = frequencyBandColors(false)[0],
        kanjiLearning = frequencyBandColors(false)[1],
        kanjiNew = textMutedLight,
        kanjiDue = semanticWarning,
        kanjiMastered = semanticSuccess,
        frequencyTiers = frequencyBandColors(false)
    )
    BaseMode.Sepia -> SurfaceColors(
        background = backgroundSepia, surface = surfaceSepiaDark,
        surfaceElevated = surfaceSepiaMedium, surfaceInteractive = surfaceSepiaLight,
        border = borderSepia, textPrimary = textSepiaPrimary,
        textSecondary = textSepiaSecondary, textMuted = textSepiaMuted, textInverse = textSepiaPrimary,
        kanjiKnown = frequencyBandColors(false)[0],
        kanjiLearning = frequencyBandColors(false)[1],
        kanjiNew = textSepiaMuted,
        kanjiDue = semanticWarning,
        kanjiMastered = semanticSuccess,
        frequencyTiers = frequencyBandColors(false)
    )
    BaseMode.Cream -> SurfaceColors(
        background = backgroundCream, surface = surfaceCreamDark,
        surfaceElevated = surfaceCreamMedium, surfaceInteractive = surfaceCreamLight,
        border = borderCream, textPrimary = textCreamPrimary,
        textSecondary = textCreamSecondary, textMuted = textCreamMuted, textInverse = textCreamPrimary,
        kanjiKnown = frequencyBandColors(false)[0],
        kanjiLearning = frequencyBandColors(false)[1],
        kanjiNew = textCreamMuted,
        kanjiDue = semanticWarning,
        kanjiMastered = semanticSuccess,
        frequencyTiers = frequencyBandColors(false)
    )
    BaseMode.Paper -> SurfaceColors(
        background = backgroundPaper, surface = surfacePaperDark,
        surfaceElevated = surfacePaperMedium, surfaceInteractive = surfacePaperLight,
        border = borderPaper, textPrimary = textPaperPrimary,
        textSecondary = textPaperSecondary, textMuted = textPaperMuted, textInverse = textPaperPrimary,
        kanjiKnown = frequencyBandColors(false)[0],
        kanjiLearning = frequencyBandColors(false)[1],
        kanjiNew = textPaperMuted,
        kanjiDue = semanticWarning,
        kanjiMastered = semanticSuccess,
        frequencyTiers = frequencyBandColors(false)
    )
    BaseMode.Midnight -> SurfaceColors(
        background = backgroundMidnight, surface = surfaceMidnightDark,
        surfaceElevated = surfaceMidnightMedium, surfaceInteractive = surfaceMidnightLight,
        border = borderMidnight, textPrimary = textMidnightPrimary,
        textSecondary = textMidnightSecondary, textMuted = textMidnightMuted, textInverse = textMidnightPrimary,
        kanjiKnown = frequencyBandColors(true)[0],
        kanjiLearning = frequencyBandColors(true)[1],
        kanjiNew = textMidnightMuted,
        kanjiDue = semanticWarning,
        kanjiMastered = semanticSuccess,
        frequencyTiers = frequencyBandColors(true)
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
    val textInverse: Color,
    /** Per-base-mode study-state tokens (theme-aware, not hardcoded). */
    val kanjiKnown: Color = semanticSuccess,
    val kanjiLearning: Color = semanticInfo,
    val kanjiNew: Color = textMuted,
    val kanjiDue: Color = semanticWarning,
    val kanjiMastered: Color = semanticSuccess,
    /** Five frequency bands, VeryCommon → Rare (Kaiteyo green→red language). */
    val frequencyTiers: List<Color> = frequencyBandColors(false),
    /** Study-state for suspended items; defaults to muted. */
    val kanjiSuspended: Color = textMuted
)

/**
 * Frequency band → color, luminance-adaptive so the green→red ramp reads on
 * both light and dark themes. [true] = dark base mode (brighter ramp);
 * [false] = light base mode (saturated ramp).
 * Order: VeryCommon, Common, Moderate, Uncommon, Rare.
 */
fun frequencyBandColors(isDark: Boolean): List<Color> = if (isDark) {
    listOf(
        Color(0xFF86FF86), // Very common — green
        Color(0xFF7FBDFE), // Common — blue
        Color(0xFFFFE684), // Moderate — amber
        Color(0xFFFFBB6B), // Uncommon — orange
        Color(0xFFFF7B7B)  // Rare — red
    )
} else {
    listOf(
        Color(0xFF2E7D32), // Very common — green
        Color(0xFF1565C0), // Common — blue
        Color(0xFFEF6C00), // Moderate — amber
        Color(0xFFFFA726), // Uncommon — orange
        Color(0xFFFF3D3D)  // Rare — red
    )
}