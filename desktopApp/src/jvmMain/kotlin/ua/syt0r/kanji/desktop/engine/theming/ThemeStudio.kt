package ua.syt0r.kanji.desktop.engine.theming

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.presentation.common.theme.AnimationConfig
import ua.syt0r.kanji.presentation.common.theme.AnimationSpeed
import ua.syt0r.kanji.presentation.common.theme.AppTypography
import ua.syt0r.kanji.presentation.common.theme.BaseMode
import ua.syt0r.kanji.presentation.common.theme.CornerRadiusStyle
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LayoutConfig
import ua.syt0r.kanji.presentation.common.theme.RadiusConfig
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.common.theme.TypeScale
import ua.syt0r.kanji.presentation.common.theme.UIDensity
import kotlin.math.roundToInt

// ============================================
// THEME STUDIO ENGINE
// Full theme definitions: every major UI color,
// typography, display scaling, component sizing,
// spacing, animation and effects. Lossless JSON
// import/export and live editing of the active
// theme through ThemeManager.
// ============================================

@Serializable
data class ThemeColors(
    // --- Surfaces ---
    val background: String = "#050505",
    val surface: String = "#0D0D0D",
    val surfaceElevated: String = "#101010",
    val surfaceInteractive: String = "#1A1A1A",
    val border: String = "#2A2A2A",
    // --- Text ---
    val textPrimary: String = "#F0F0F0",
    val textSecondary: String = "#A0A0A0",
    val textMuted: String = "#606060",
    val textInverse: String = "#050505",
    // --- Brand ---
    val primary: String = "#C2FC8B",
    val primaryDark: String = "#9CE85E",
    val secondary: String = "#FEAB57",
    val secondaryDark: String = "#FD8A2E",
    val tertiary: String = "#7BC8FF",
    val onPrimary: String = "#050505",
    val onSecondary: String = "#050505",
    // --- Semantic ---
    val error: String = "#FF6B6B",
    val success: String = "#C2FC8B",
    val warning: String = "#FEAB57",
    val info: String = "#7BC8FF",
    val link: String = "#7BC8FF",
    // --- Interaction ---
    val hover: String = "#1A1A1A",
    val selection: String = "#2E3B22",
    // --- Component surfaces ---
    val sidebar: String = "#0A0A0A",
    val navigation: String = "#0A0A0A",
    val window: String = "#050505",
    val dialog: String = "#101010",
    val popup: String = "#141414",
    val launchpad: String = "#0A0A0A",
    val bubble: String = "#1A1A1A",
    // --- Shadows & data viz ---
    val shadow: String = "#000000",
    val charts: String = "#7BC8FF",
    val heatmap: String = "#C2FC8B",
    val statistics: String = "#FEAB57"
)

@Serializable
data class ThemeTypography(
    val fontFamily: String = "system",
    val fontSize: Float = 1.0f,
    val fontScale: Float = 1.0f,
    val headingWeight: Int = 700,
    val bodyWeight: Int = 400,
    val lineHeight: Float = 1.0f,
    val letterSpacing: Float = 0.0f,
    val uiScale: Float = 1.0f,
    val titleScale: Float = 1.0f
)

@Serializable
data class ThemeSpacing(
    val scale: Float = 1.0f,
    val padding: Float = 1.0f
)

@Serializable
data class ThemeScaling(
    val displayScale: Float = 1.0f,
    val buttonSize: Float = 1.0f,
    val iconSize: Float = 1.0f,
    val bubbleSize: Float = 1.0f,
    val toolbarHeight: Float = 1.0f,
    val windowPadding: Float = 1.0f,
    val sidebarWidth: Float = 1.0f
)

@Serializable
data class ThemeAnimation(
    val durationMs: Int = 300,
    val speed: Float = 1.0f,
    val reducedMotion: Boolean = false,
    val blurStrength: Float = 1.0f,
    val shadowStrength: Float = 1.0f,
    val hoverEnabled: Boolean = true,
    val launchpadEnabled: Boolean = true,
    val sidebarEnabled: Boolean = true,
    val bubbleEnabled: Boolean = true,
    val themeTransitionEnabled: Boolean = true
)

@Serializable
data class ThemeCorners(val radiusMultiplier: Float = 1.0f, val style: String = "rounded")

@Serializable
data class ThemeEffects(
    val blur: Boolean = false,
    val transparency: Boolean = false,
    val glassOpacity: Float = 0.8f,
    val oled: Boolean = false,
    val material: Boolean = false
)

@Serializable
data class ThemeGradientStop(
    val color: String = "#C2FC8B",
    val position: Float = 0f
)

@Serializable
data class ThemeGradient(
    val enabled: Boolean = true,
    val type: String = "linear", // linear | radial | sweep
    val angle: Float = 0f, // degrees, CSS-style (0 = toward the top, 90 = toward the right)
    val stops: List<ThemeGradientStop> = listOf(
        ThemeGradientStop("#C2FC8B", 0f),
        ThemeGradientStop("#FEAB57", 1f)
    )
) {
    /** First stop — drives the accent scheme's gradientStart. */
    val start: String get() = stops.firstOrNull()?.color ?: "#C2FC8B"

    /** Last stop — drives the accent scheme's gradientEnd. */
    val end: String get() = stops.lastOrNull()?.color ?: "#FEAB57"
}

@Serializable
data class KaiteyoTheme(
    val id: String,
    val name: String,
    val description: String = "",
    val author: String = "Kaiteyo",
    val version: Int = 1,
    val baseMode: String = "oled",
    val colors: ThemeColors = ThemeColors(),
    val gradient: ThemeGradient = ThemeGradient(),
    val typography: ThemeTypography = ThemeTypography(),
    val spacing: ThemeSpacing = ThemeSpacing(),
    val scaling: ThemeScaling = ThemeScaling(),
    val animation: ThemeAnimation = ThemeAnimation(),
    val corners: ThemeCorners = ThemeCorners(),
    val effects: ThemeEffects = ThemeEffects(),
    val tags: List<String> = emptyList(),
    val favorite: Boolean = false,
    val source: String = "preset",
    val createdAt: String = "",
    val updatedAt: String = ""
)

object ThemeSerializer {

    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }

    fun export(theme: KaiteyoTheme): String = json.encodeToString(theme)

    fun import(text: String): Result<KaiteyoTheme> = runCatching {
        json.decodeFromString<KaiteyoTheme>(text)
    }

    fun validate(text: String): Result<KaiteyoTheme> = import(text).map { theme ->
        require(theme.id.isNotBlank()) { "Theme id must not be blank" }
        require(theme.name.isNotBlank()) { "Theme name must not be blank" }
        require(isValidHex(theme.colors.primary)) { "Invalid primary color hex" }
        theme
    }

    private fun isValidHex(hex: String): Boolean =
        hex.matches(Regex("^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$"))
}

// ============================================
// COLOR MATH — hex / RGB / HSV / HSL helpers
// ============================================

fun hexToColor(hex: String): Color = try {
    val raw = hex.removePrefix("#")
    when (raw.length) {
        6 -> Color(
            raw.substring(0, 2).toInt(16),
            raw.substring(2, 4).toInt(16),
            raw.substring(4, 6).toInt(16),
            255
        )
        8 -> Color(
            raw.substring(2, 4).toInt(16),
            raw.substring(4, 6).toInt(16),
            raw.substring(6, 8).toInt(16),
            raw.substring(0, 2).toInt(16)
        )
        else -> Color.White
    }
} catch (_: Exception) {
    Color.White
}

fun colorToHex(color: Color): String = buildString {
    append('#')
    append(hexByte(color.alpha))
    append(hexByte(color.red))
    append(hexByte(color.green))
    append(hexByte(color.blue))
}

fun colorToRgbHex(color: Color): String = buildString {
    append('#')
    append(hexByte(color.red))
    append(hexByte(color.green))
    append(hexByte(color.blue))
}

private fun hexByte(value: Float): String {
    val int = (value.coerceIn(0f, 1f) * 255).toInt()
    return int.toString(16).padStart(2, '0').uppercase()
}

/** RGB channels as 0..255 ints. */
fun rgbChannels(color: Color): Triple<Int, Int, Int> = Triple(
    (color.red.coerceIn(0f, 1f) * 255).roundToInt(),
    (color.green.coerceIn(0f, 1f) * 255).roundToInt(),
    (color.blue.coerceIn(0f, 1f) * 255).roundToInt()
)

/** HSV: hue 0..360, saturation 0..1, value 0..1. */
fun rgbToHsv(color: Color): Triple<Float, Float, Float> {
    val r = color.red.coerceIn(0f, 1f)
    val g = color.green.coerceIn(0f, 1f)
    val b = color.blue.coerceIn(0f, 1f)
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val d = max - min
    val h = when {
        d == 0f -> 0f
        max == r -> 60f * (((g - b) / d) % 6)
        max == g -> 60f * (((b - r) / d) + 2)
        else -> 60f * (((r - g) / d) + 4)
    }
    val hue = if (h < 0) h + 360 else h
    val s = if (max == 0f) 0f else d / max
    return Triple(hue, s, max)
}

fun hsvToColor(hue: Float, saturation: Float, value: Float, alpha: Float = 1f): Color {
    val h = ((hue % 360f) + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val v = value.coerceIn(0f, 1f)
    val c = v * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = v - c
    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m, alpha)
}

/** HSL: hue 0..360, saturation 0..1, lightness 0..1. */
fun rgbToHsl(color: Color): Triple<Float, Float, Float> {
    val r = color.red.coerceIn(0f, 1f)
    val g = color.green.coerceIn(0f, 1f)
    val b = color.blue.coerceIn(0f, 1f)
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    val d = max - min
    val h = when {
        d == 0f -> 0f
        max == r -> 60f * (((g - b) / d) % 6)
        max == g -> 60f * (((b - r) / d) + 2)
        else -> 60f * (((r - g) / d) + 4)
    }
    val hue = if (h < 0) h + 360 else h
    val s = if (d == 0f) 0f else d / (1f - kotlin.math.abs(2f * l - 1f))
    return Triple(hue, s, l)
}

fun hslToColor(hue: Float, saturation: Float, lightness: Float, alpha: Float = 1f): Color {
    val h = ((hue % 360f) + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val l = lightness.coerceIn(0f, 1f)
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m, alpha)
}

fun relativeLuminance(color: Color): Float {
    fun channel(v: Float): Float {
        val c = v.coerceIn(0f, 1f)
        return if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    }
    return 0.2126f * channel(color.red) + 0.7152f * channel(color.green) + 0.0722f * channel(color.blue)
}

private fun Float.pow(exp: Float): Float = Math.pow(this.toDouble(), exp.toDouble()).toFloat()

/** Contrast ratio between two colors (WCAG). */
fun contrastRatio(a: Color, b: Color): Float {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    val lighter = maxOf(la, lb)
    val darker = minOf(la, lb)
    return (lighter + 0.05f) / (darker + 0.05f)
}

/** Contrast-aware text color for a given background. */
fun readableOn(background: Color): String =
    if (contrastRatio(background, Color.White) >= contrastRatio(background, Color.Black)) "#FFFFFF" else "#0A0A0A"

// ============================================
// THEME → COMPOSE MAPPING
// ============================================

object ThemeMapper {

    fun baseMode(theme: KaiteyoTheme): BaseMode = when (theme.baseMode.lowercase()) {
        "light" -> BaseMode.Light
        "dark" -> BaseMode.Dark
        "sepia" -> BaseMode.Sepia
        "cream" -> BaseMode.Cream
        "paper" -> BaseMode.Paper
        "midnight" -> BaseMode.Midnight
        else -> BaseMode.Oled
    }

    fun color(hex: String): Color = hexToColor(hex)

    fun toHex(color: Color): String = colorToHex(color)

    fun surfaceColors(theme: KaiteyoTheme): SurfaceColors = SurfaceColors(
        background = hexToColor(theme.colors.background),
        surface = hexToColor(theme.colors.surface),
        surfaceElevated = hexToColor(theme.colors.surfaceElevated),
        surfaceInteractive = hexToColor(theme.colors.surfaceInteractive),
        border = hexToColor(theme.colors.border),
        textPrimary = hexToColor(theme.colors.textPrimary),
        textSecondary = hexToColor(theme.colors.textSecondary),
        textMuted = hexToColor(theme.colors.textMuted),
        textInverse = hexToColor(theme.colors.textInverse)
    )

    fun accentScheme(theme: KaiteyoTheme): KaiteyoAccentScheme {
        val primary = hexToColor(theme.colors.primary)
        val secondary = hexToColor(theme.colors.secondary)
        val tertiary = hexToColor(theme.colors.tertiary)
        // When the custom gradient is enabled its first/last stops drive the
        // accent gradient; when disabled the app falls back to Primary/Secondary
        // (the old behavior), so toggling off is always safe.
        return KaiteyoAccentScheme(
            name = theme.name,
            primary = primary,
            primaryDark = hexToColor(theme.colors.primaryDark),
            secondary = secondary,
            secondaryDark = hexToColor(theme.colors.secondaryDark),
            onPrimary = hexToColor(theme.colors.onPrimary),
            onSecondary = hexToColor(theme.colors.onSecondary),
            tertiary = tertiary,
            previewColors = listOf(primary, secondary, tertiary),
            gradientStart = if (theme.gradient.enabled) hexToColor(theme.gradient.start) else null,
            gradientEnd = if (theme.gradient.enabled) hexToColor(theme.gradient.end) else null
        )
    }

    fun layoutConfig(theme: KaiteyoTheme): LayoutConfig = LayoutConfig(
        density = when {
            theme.spacing.scale <= 0.75f -> UIDensity.Compact
            theme.spacing.scale >= 1.25f -> UIDensity.Spacious
            else -> UIDensity.Comfortable
        },
        panelWidth = 232.dp * theme.scaling.sidebarWidth.coerceIn(0.5f, 2f),
        transparencyEnabled = theme.effects.transparency,
        blurEnabled = theme.effects.blur,
        glassOpacity = theme.effects.glassOpacity.coerceIn(0f, 1f),
        displayScale = theme.scaling.displayScale.coerceIn(0.5f, 2f)
    )

    fun radiusConfig(theme: KaiteyoTheme): RadiusConfig = RadiusConfig(
        style = CornerRadiusStyle.entries.firstOrNull { it.name.equals(theme.corners.style, ignoreCase = true) }
            ?: CornerRadiusStyle.Rounded,
        customRadius = if (theme.corners.radiusMultiplier != 1f) theme.corners.radiusMultiplier.coerceIn(0.25f, 3f) else null
    )

    fun animationConfig(theme: KaiteyoTheme): AnimationConfig = AnimationConfig(
        speed = when {
            theme.animation.speed <= 0.05f -> AnimationSpeed.Instant
            theme.animation.speed < 0.8f -> AnimationSpeed.Fast
            theme.animation.speed > 1.2f -> AnimationSpeed.Slow
            else -> AnimationSpeed.Normal
        },
        reducedMotion = theme.animation.reducedMotion,
        defaultDuration = theme.animation.durationMs.coerceIn(0, 2000),
        themeTransitionEnabled = theme.animation.themeTransitionEnabled
    )

    fun typeScale(theme: KaiteyoTheme): TypeScale = TypeScale(
        fontScale = theme.typography.fontScale.coerceIn(0.6f, 2f),
        titleScale = theme.typography.titleScale.coerceIn(0.6f, 2f),
        lineHeight = theme.typography.lineHeight.coerceIn(0.8f, 2f),
        letterSpacing = theme.typography.letterSpacing.coerceIn(-2f, 4f)
    )

    fun fontFamily(theme: KaiteyoTheme): FontFamily = when (theme.typography.fontFamily.lowercase()) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        "cursive" -> FontFamily.Cursive
        "sans-serif" -> FontFamily.SansSerif
        else -> FontFamily.Default
    }

    /** Material Typography scaled by the theme's base font size and weights. */
    fun typography(theme: KaiteyoTheme): Typography {
        val family = fontFamily(theme)
        val size = theme.typography.fontSize.coerceIn(0.6f, 2f)
        val heading = FontWeight(theme.typography.headingWeight.coerceIn(100, 900))
        val body = FontWeight(theme.typography.bodyWeight.coerceIn(100, 900))
        val lineHeight = theme.typography.lineHeight.coerceIn(0.8f, 2f)
        val tracking = theme.typography.letterSpacing.coerceIn(-2f, 4f)

        fun scale(style: TextStyle, weight: FontWeight, title: Boolean = false): TextStyle {
            val factor = size * (if (title) theme.typography.titleScale.coerceIn(0.6f, 2f) else 1f)
            return style.copy(
                fontFamily = family,
                fontWeight = weight,
                fontSize = style.fontSize * factor,
                lineHeight = style.lineHeight * lineHeight,
                letterSpacing = (style.letterSpacing.value + tracking).sp
            )
        }

        return Typography(
            displayLarge = scale(AppTypography.displayLarge, heading, title = true),
            displayMedium = scale(AppTypography.displayMedium, heading, title = true),
            displaySmall = scale(AppTypography.displaySmall, heading, title = true),
            headlineLarge = scale(AppTypography.headlineLarge, heading, title = true),
            headlineMedium = scale(AppTypography.headlineMedium, heading, title = true),
            headlineSmall = scale(AppTypography.headlineSmall, heading, title = true),
            titleLarge = scale(AppTypography.titleLarge, heading, title = true),
            titleMedium = scale(AppTypography.titleMedium, heading, title = true),
            titleSmall = scale(AppTypography.titleSmall, heading, title = true),
            bodyLarge = scale(AppTypography.bodyLarge, body),
            bodyMedium = scale(AppTypography.bodyMedium, body),
            bodySmall = scale(AppTypography.bodySmall, body),
            labelLarge = scale(AppTypography.labelLarge, body),
            labelMedium = scale(AppTypography.labelMedium, body),
            labelSmall = scale(AppTypography.labelSmall, body)
        )
    }
}
