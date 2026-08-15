package ua.syt0r.kanji.presentation.screen.main.screen.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import kotlin.math.roundToInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import ua.syt0r.kanji.presentation.common.theme.AnimationConfig
import ua.syt0r.kanji.presentation.common.theme.AnimationSpeed
import ua.syt0r.kanji.presentation.common.theme.BaseMode
import ua.syt0r.kanji.presentation.common.theme.CornerRadiusStyle
import ua.syt0r.kanji.presentation.common.theme.GlowConfig
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.KaiteyoThemeState
import ua.syt0r.kanji.presentation.common.theme.LayoutConfig
import ua.syt0r.kanji.presentation.common.theme.NavAutoHide
import ua.syt0r.kanji.presentation.common.theme.PageTransitionType
import ua.syt0r.kanji.presentation.common.theme.RadiusConfig
import ua.syt0r.kanji.presentation.common.theme.SidebarMode
import ua.syt0r.kanji.presentation.common.theme.SidebarPosition
import ua.syt0r.kanji.presentation.common.theme.UIDensity
import ua.syt0r.kanji.presentation.common.theme.parseColorHex
import ua.syt0r.kanji.presentation.common.theme.toHexString

// ============================================
// THEME EXPORT / IMPORT CODEC
// Versioned "kaiteyo-theme" JSON payload that
// round-trips the entire Theme Studio config.
// Exchanged through the clipboard so it works
// on every platform without a file picker.
// ============================================

private const val THEME_FORMAT = "kaiteyo-theme"
private const val THEME_VERSION = 2

/** Compact single-line JSON for clipboard sharing. */
internal fun themeStateToJson(state: KaiteyoThemeState): String =
    themeStateToJsonObject(state).toString()

/** Pretty-printed JSON for on-screen display. */
internal fun themeStateToPrettyJson(state: KaiteyoThemeState): String =
    prettyJson.encodeToString(JsonElement.serializer(), themeStateToJsonObject(state))

private val prettyJson = Json { prettyPrint = true; ignoreUnknownKeys = true }

private fun themeStateToJsonObject(state: KaiteyoThemeState) = buildJsonObject {
    put("format", THEME_FORMAT)
    put("version", THEME_VERSION)
    put("baseMode", state.baseMode.name)
    val accent = state.accentScheme
    put("accentName", accent.name)
    put("primary", accent.primary.toHexString(includeAlpha = true))
    put("primaryDark", accent.primaryDark.toHexString(includeAlpha = true))
    put("secondary", accent.secondary.toHexString(includeAlpha = true))
    put("secondaryDark", accent.secondaryDark.toHexString(includeAlpha = true))
    put("onPrimary", accent.onPrimary.toHexString(includeAlpha = true))
    put("onSecondary", accent.onSecondary.toHexString(includeAlpha = true))
    accent.tertiary?.let { put("tertiary", it.toHexString(includeAlpha = true)) }
    accent.gradientStart?.let { put("gradientStart", it.toHexString(includeAlpha = true)) }
    accent.gradientEnd?.let { put("gradientEnd", it.toHexString(includeAlpha = true)) }
    putJsonArray("previewColors") { accent.previewColors.forEach { add(it.toHexString(includeAlpha = true)) } }
    put("animationSpeed", state.animationConfig.speed.name)
    put("reducedMotion", state.animationConfig.reducedMotion)
    put("springDamping", state.animationConfig.springDamping)
    put("springStiffness", state.animationConfig.springStiffness)
    put("defaultDuration", state.animationConfig.defaultDuration)
    put("pageTransition", state.animationConfig.pageTransition.name)
    put("themeTransition", state.animationConfig.themeTransitionEnabled)
    put("radiusStyle", state.radiusConfig.style.name)
    state.radiusConfig.customRadius?.let { put("customRadius", it) }
    state.radiusConfig.buttonRadius?.let { put("buttonRadius", it) }
    put("glowIntensity", state.glowConfig.intensity)
    put("glowRadius", state.glowConfig.radius)
    put("glowOpacity", state.glowConfig.opacity)
    val layout = state.layoutConfig
    put("density", layout.density.name)
    put("sidebarMode", layout.sidebarMode.name)
    put("sidebarPosition", layout.sidebarPosition.name)
    put("autoHide", layout.autoHide.name)
    put("collapsed", layout.collapsed)
    put("panelWidth", layout.panelWidth.value)
    put("panelHeight", layout.panelHeight.value)
    put("floatingX", layout.floatingOffset.x.value)
    put("floatingY", layout.floatingOffset.y.value)
    put("accentIndex", layout.accentIndex)
    put("transparency", layout.transparencyEnabled)
    put("blur", layout.blurEnabled)
    put("glassOpacity", layout.glassOpacity)
    put("displayScale", layout.displayScale)
    put("buttonScale", layout.buttonScale)
    put("iconScale", layout.iconScale)
    put("bubbleScale", layout.bubbleScale)
    put("toolbarScale", layout.toolbarHeightScale)
    put("windowPaddingScale", layout.windowPaddingScale)
}

/** Parses a "kaiteyo-theme" payload into a fresh state, or null on garbage. */
internal fun themeJsonToState(jsonText: String): KaiteyoThemeState? = runCatching {
    val root = Json.parseToJsonElement(jsonText).jsonObject
    if (root["format"]?.jsonPrimitive?.content != THEME_FORMAT) return null

    fun str(key: String): String? = root[key]?.jsonPrimitive?.content
    fun bool(key: String): Boolean? = root[key]?.jsonPrimitive?.content?.toBooleanStrictOrNull()
    fun float(key: String): Float? = root[key]?.jsonPrimitive?.content?.toFloatOrNull()
    fun int(key: String): Int? = root[key]?.jsonPrimitive?.content?.toIntOrNull()
    fun color(key: String): Color? = str(key)?.let { parseColorHex(it) }
    fun <T : Enum<T>> enumOf(key: String, fallback: T, values: List<T>): T =
        str(key)?.let { name -> values.firstOrNull { it.name == name } } ?: fallback

    val previewColors = root["previewColors"]?.jsonArray?.mapNotNull {
        parseColorHex(it.jsonPrimitive.content)
    } ?: emptyList()

    val accent = KaiteyoAccentScheme(
        name = str("accentName") ?: "Imported",
        primary = color("primary") ?: Color(0xFF7BC8FF),
        primaryDark = color("primaryDark") ?: Color(0xFF7BC8FF),
        secondary = color("secondary") ?: Color(0xFFFEAB57),
        secondaryDark = color("secondaryDark") ?: Color(0xFFFEAB57),
        onPrimary = color("onPrimary") ?: Color.White,
        onSecondary = color("onSecondary") ?: Color.Black,
        tertiary = color("tertiary"),
        previewColors = previewColors.ifEmpty { listOf(Color(0xFF7BC8FF), Color(0xFFFEAB57)) },
        gradientStart = color("gradientStart"),
        gradientEnd = color("gradientEnd")
    )

    KaiteyoThemeState(
        initialBaseMode = enumOf("baseMode", BaseMode.Oled, BaseMode.entries),
        initialAccentScheme = accent,
        initialAnimationConfig = AnimationConfig(
            speed = enumOf("animationSpeed", AnimationSpeed.Normal, AnimationSpeed.entries),
            reducedMotion = bool("reducedMotion") ?: false,
            springDamping = float("springDamping") ?: 0.6f,
            springStiffness = float("springStiffness") ?: 300f,
            defaultDuration = int("defaultDuration") ?: 300,
            pageTransition = enumOf("pageTransition", PageTransitionType.FadeThrough, PageTransitionType.entries),
            themeTransitionEnabled = bool("themeTransition") ?: true
        ),
        initialRadiusConfig = RadiusConfig(
            style = enumOf("radiusStyle", CornerRadiusStyle.Rounded, CornerRadiusStyle.entries),
            customRadius = float("customRadius"),
            buttonRadius = float("buttonRadius")
        ),
        initialGlowConfig = GlowConfig(
            intensity = float("glowIntensity") ?: 1f,
            radius = float("glowRadius") ?: 1f,
            opacity = float("glowOpacity") ?: 1f
        ),
        initialLayoutConfig = LayoutConfig(
            density = enumOf("density", UIDensity.Comfortable, UIDensity.entries),
            sidebarMode = enumOf("sidebarMode", SidebarMode.Expanded, SidebarMode.entries),
            sidebarPosition = enumOf("sidebarPosition", SidebarPosition.Left, SidebarPosition.entries),
            autoHide = enumOf("autoHide", NavAutoHide.Never, NavAutoHide.entries),
            collapsed = bool("collapsed") ?: false,
            panelWidth = Dp(float("panelWidth") ?: 260f),
            panelHeight = Dp(float("panelHeight") ?: 56f),
            floatingOffset = DpOffset(Dp(float("floatingX") ?: 0f), Dp(float("floatingY") ?: 0f)),
            accentIndex = int("accentIndex") ?: -1,
            transparencyEnabled = bool("transparency") ?: false,
            blurEnabled = bool("blur") ?: false,
            glassOpacity = float("glassOpacity") ?: 0.8f,
            displayScale = float("displayScale") ?: 1f,
            buttonScale = float("buttonScale") ?: 1f,
            iconScale = float("iconScale") ?: 1f,
            bubbleScale = float("bubbleScale") ?: 1f,
            toolbarHeightScale = float("toolbarScale") ?: 1f,
            windowPaddingScale = float("windowPaddingScale") ?: 1f
        )
    )
}.getOrNull()

/** KMP-safe number formatting (no String.format on JVM-only). */
internal fun formatFloat(value: Float, decimals: Int): String {
    val factor = when (decimals) {
        0 -> 1
        1 -> 10
        2 -> 100
        else -> 1000
    }
    val rounded = (value * factor).roundToInt()
    val intPart = rounded / factor
    val decPart = (rounded % factor).let { if (it < 0) -it else it }
    return if (decimals > 0) {
        "$intPart.${decPart.toString().padStart(decimals, '0')}"
    } else {
        "$intPart"
    }
}
