package ua.syt0r.kanji.desktop.engine.theming

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.presentation.common.theme.BaseMode

// ============================================
// THEME STUDIO ENGINE
// Full theme definitions (colors, typography,
// spacing, animation, corners, icons, blur,
// transparency, glass, OLED/material presets),
// JSON import/export and live editing.
// ============================================

@Serializable
data class ThemeColors(
    val background: String = "#050505",
    val surface: String = "#0D0D0D",
    val surfaceElevated: String = "#101010",
    val surfaceInteractive: String = "#1A1A1A",
    val border: String = "#2A2A2A",
    val textPrimary: String = "#F0F0F0",
    val textSecondary: String = "#A0A0A0",
    val textMuted: String = "#606060",
    val primary: String = "#C2FC8B",
    val primaryDark: String = "#9CE85E",
    val secondary: String = "#FEAB57",
    val secondaryDark: String = "#FD8A2E",
    val tertiary: String = "#7BC8FF",
    val onPrimary: String = "#050505",
    val error: String = "#FF6B6B",
    val success: String = "#C2FC8B",
    val warning: String = "#FEAB57",
    val info: String = "#7BC8FF"
)

@Serializable
data class ThemeTypography(
    val fontScale: Float = 1.0f,
    val headingWeight: Int = 700,
    val bodyWeight: Int = 400
)

@Serializable
data class ThemeSpacing(val scale: Float = 1.0f)

@Serializable
data class ThemeAnimation(
    val durationMs: Int = 300,
    val speed: Float = 1.0f,
    val reducedMotion: Boolean = false
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
data class KaiteyoTheme(
    val id: String,
    val name: String,
    val description: String = "",
    val author: String = "Kaiteyo",
    val version: Int = 1,
    val baseMode: String = "oled",
    val colors: ThemeColors = ThemeColors(),
    val typography: ThemeTypography = ThemeTypography(),
    val spacing: ThemeSpacing = ThemeSpacing(),
    val animation: ThemeAnimation = ThemeAnimation(),
    val corners: ThemeCorners = ThemeCorners(),
    val effects: ThemeEffects = ThemeEffects(),
    val tags: List<String> = emptyList()
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

/** Converts a theme definition into live Compose theme values. */
object ThemeMapper {

    fun baseMode(theme: KaiteyoTheme): BaseMode = when (theme.baseMode.lowercase()) {
        "light" -> BaseMode.Light
        "dark" -> BaseMode.Dark
        "sepia" -> BaseMode.Sepia
        else -> BaseMode.Oled
    }

    fun color(hex: String): Color = try {
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

    fun toHex(color: Color): String = buildString {
        append('#')
        append(hexByte(color.alpha))
        append(hexByte(color.red))
        append(hexByte(color.green))
        append(hexByte(color.blue))
    }

    private fun hexByte(value: Float): String {
        val int = (value.coerceIn(0f, 1f) * 255).toInt()
        return int.toString(16).padStart(2, '0').uppercase()
    }
}

/** The built-in theme presets (matches the 8 app themes). */
object ThemePresets {
    val Signature = KaiteyoTheme(
        id = "signature", name = "Signature", description = "Lime + Orange studio",
        tags = listOf("dark", "default")
    )
    val Oled = KaiteyoTheme(
        id = "oled", name = "OLED Black", description = "True black",
        effects = ThemeEffects(oled = true),
        tags = listOf("dark", "oled")
    )
    val DarkGray = KaiteyoTheme(
        id = "dark-gray", name = "Dark Gray", description = "Softer dark",
        baseMode = "dark",
        colors = ThemeColors(
            background = "#121212", surface = "#1A1A1A",
            surfaceElevated = "#242424", surfaceInteractive = "#2E2E2E", border = "#2A2A2A"
        ),
        tags = listOf("dark")
    )
    val Light = KaiteyoTheme(
        id = "light", name = "Light", description = "Clean light",
        baseMode = "light",
        colors = ThemeColors(
            background = "#F5F5F5", surface = "#EEEEEE",
            surfaceElevated = "#E8E8E8", surfaceInteractive = "#FCFCFC", border = "#D0D0D0",
            textPrimary = "#1A1A1A", textSecondary = "#606060", textMuted = "#A0A0A0",
            primary = "#9CE85E", primaryDark = "#7BC848", onPrimary = "#050505"
        ),
        tags = listOf("light")
    )
    val Reading = KaiteyoTheme(
        id = "reading", name = "Reading", description = "Warm paper tones",
        baseMode = "sepia",
        colors = ThemeColors(
            background = "#F5F0E8", surface = "#EDE5D8",
            surfaceElevated = "#E5DCC8", surfaceInteractive = "#F8F4EE", border = "#D4C8B8",
            textPrimary = "#3D3028", textSecondary = "#7A6B5D", textMuted = "#A89888",
            primary = "#B4894A", secondary = "#8A6A3A", onPrimary = "#F8F4EE"
        ),
        tags = listOf("light", "reading")
    )
    val CottonCandy = KaiteyoTheme(
        id = "cotton", name = "Cotton Candy", description = "Pastel",
        colors = ThemeColors(
            primary = "#D4A5F0", secondary = "#FFB5C5", tertiary = "#A0D2FF"
        ),
        tags = listOf("pastel")
    )
    val Ocean = KaiteyoTheme(
        id = "ocean", name = "Ocean", description = "Cool blue",
        colors = ThemeColors(
            primary = "#00D4AA", secondary = "#00A8FF", tertiary = "#0D47A1"
        ),
        tags = listOf("cool")
    )
    val Forest = KaiteyoTheme(
        id = "forest", name = "Forest", description = "Earthy green",
        colors = ThemeColors(
            primary = "#81C784", secondary = "#A5D6A7", tertiary = "#5D4037"
        ),
        tags = listOf("nature")
    )
    val Material = KaiteyoTheme(
        id = "material", name = "Material", description = "Material You feel",
        effects = ThemeEffects(material = true),
        tags = listOf("material")
    )

    val all: List<KaiteyoTheme> = listOf(Signature, Oled, DarkGray, Light, Reading, CottonCandy, Ocean, Forest, Material)
    val default: KaiteyoTheme = Signature
}
