package ua.syt0r.kanji.presentation.common.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract

// ============================================
// THEME SETTINGS STATE
// Single source of truth for the persisted
// appearance configuration (accent, radius,
// density, typography, motion). Mirrors the
// NavigationSettingsState pattern: one JSON
// blob in preferences, applied live to
// KaiteyoThemeState so the Theme Studio and
// the Settings Center share the same tokens.
// ============================================

@Serializable
data class ThemeSettings(
    val accentName: String = AllAccentSchemes.first().name,
    // Custom accent colors (hex). Filled when the Theme Studio applies a
    // color that no preset defines, so custom themes survive restarts.
    val customPrimary: String? = null,
    val customSecondary: String? = null,
    val customTertiary: String? = null,
    val customGradientStart: String? = null,
    val customGradientEnd: String? = null,
    val radiusStyle: CornerRadiusStyle = CornerRadiusStyle.Rounded,
    val radiusCustom: Float? = null,
    val radiusButton: Float? = null,
    val density: UIDensity = UIDensity.Comfortable,
    val displayScale: Float = 1f,
    val buttonScale: Float = 1f,
    val iconScale: Float = 1f,
    val bubbleScale: Float = 1f,
    val toolbarHeightScale: Float = 1f,
    val windowPaddingScale: Float = 1f,
    val transparencyEnabled: Boolean = false,
    val blurEnabled: Boolean = false,
    val glassOpacity: Float = 0.8f,
    val animationSpeed: AnimationSpeed = AnimationSpeed.Normal,
    val animationReducedMotion: Boolean = false,
    val springDamping: Float = 0.6f,
    val springStiffness: Float = 300f,
    val defaultDuration: Int = 300,
    val pageTransition: PageTransitionType = PageTransitionType.FadeThrough,
    val themeTransitionEnabled: Boolean = true,
    val glowIntensity: Float = 1f,
    val glowRadius: Float = 1f,
    val glowOpacity: Float = 1f,
    val fontScale: Float = 1f,
    val titleScale: Float = 1f,
    val lineHeight: Float = 1f,
    val letterSpacing: Float = 0f
) {

    fun accentScheme(): KaiteyoAccentScheme {
        AllAccentSchemes.firstOrNull { it.name == accentName }?.let { return it }
        val customPrimaryColor = customPrimary?.let { parseColorHex(it) }
            ?: return AllAccentSchemes.first()
        val customSecondaryColor = customSecondary?.let { parseColorHex(it) } ?: customPrimaryColor
        return KaiteyoAccentScheme(
            name = accentName,
            primary = customPrimaryColor,
            primaryDark = customPrimaryColor,
            secondary = customSecondaryColor,
            secondaryDark = customSecondaryColor,
            onPrimary = Color(0xFF050505),
            onSecondary = Color(0xFF050505),
            tertiary = customTertiary?.let { parseColorHex(it) },
            previewColors = listOf(customPrimaryColor, customSecondaryColor),
            gradientStart = customGradientStart?.let { parseColorHex(it) },
            gradientEnd = customGradientEnd?.let { parseColorHex(it) }
        )
    }

    fun toRadiusConfig() = RadiusConfig(
        style = radiusStyle,
        customRadius = radiusCustom,
        buttonRadius = radiusButton
    )

    fun toGlowConfig() = GlowConfig(
        intensity = glowIntensity,
        radius = glowRadius,
        opacity = glowOpacity
    )

    fun toLayoutConfig() = LayoutConfig(
        density = density,
        displayScale = displayScale,
        buttonScale = buttonScale,
        iconScale = iconScale,
        bubbleScale = bubbleScale,
        toolbarHeightScale = toolbarHeightScale,
        windowPaddingScale = windowPaddingScale,
        transparencyEnabled = transparencyEnabled,
        blurEnabled = blurEnabled,
        glassOpacity = glassOpacity
    )

    fun toAnimationConfig() = AnimationConfig(
        speed = animationSpeed,
        reducedMotion = animationReducedMotion,
        springDamping = springDamping,
        springStiffness = springStiffness,
        defaultDuration = defaultDuration,
        pageTransition = pageTransition,
        themeTransitionEnabled = themeTransitionEnabled
    )

    fun toTypeScale() = TypeScale(
        fontScale = fontScale,
        titleScale = titleScale,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing
    )

    companion object {

        fun from(themeState: KaiteyoThemeState): ThemeSettings {
            val layout = themeState.layoutConfig
            val animation = themeState.animationConfig
            val radius = themeState.radiusConfig
            val glow = themeState.glowConfig
            val typeScale = themeState.typeScale
            val accent = themeState.accentScheme
            val preset = AllAccentSchemes.firstOrNull { it.name == accent.name }
            val isCustom = preset == null ||
                preset.primary != accent.primary ||
                preset.primaryDark != accent.primaryDark ||
                preset.secondary != accent.secondary ||
                preset.secondaryDark != accent.secondaryDark ||
                preset.tertiary != accent.tertiary ||
                preset.gradientStart != accent.gradientStart ||
                preset.gradientEnd != accent.gradientEnd
            return ThemeSettings(
                accentName = accent.name,
                customPrimary = if (isCustom) accent.primary.toHexString() else null,
                customSecondary = if (isCustom) accent.secondary.toHexString() else null,
                customTertiary = if (isCustom) accent.tertiary?.toHexString() else null,
                customGradientStart = if (isCustom) accent.gradientStart?.toHexString() else null,
                customGradientEnd = if (isCustom) accent.gradientEnd?.toHexString() else null,
                radiusStyle = radius.style,
                radiusCustom = radius.customRadius,
                radiusButton = radius.buttonRadius,
                density = layout.density,
                displayScale = layout.displayScale,
                buttonScale = layout.buttonScale,
                iconScale = layout.iconScale,
                bubbleScale = layout.bubbleScale,
                toolbarHeightScale = layout.toolbarHeightScale,
                windowPaddingScale = layout.windowPaddingScale,
                transparencyEnabled = layout.transparencyEnabled,
                blurEnabled = layout.blurEnabled,
                glassOpacity = layout.glassOpacity,
                animationSpeed = animation.speed,
                animationReducedMotion = animation.reducedMotion,
                springDamping = animation.springDamping,
                springStiffness = animation.springStiffness,
                defaultDuration = animation.defaultDuration,
                pageTransition = animation.pageTransition,
                themeTransitionEnabled = animation.themeTransitionEnabled,
                glowIntensity = glow.intensity,
                glowRadius = glow.radius,
                glowOpacity = glow.opacity,
                fontScale = typeScale.fontScale,
                titleScale = typeScale.titleScale,
                lineHeight = typeScale.lineHeight,
                letterSpacing = typeScale.letterSpacing
            )
        }

    }

}

class ThemeSettingsState(
    private val appPreferences: PreferencesContract.AppPreferences,
    dispatcher: CoroutineDispatcher = Dispatchers.Unconfined
) {

    private val scope = CoroutineScope(dispatcher)

    var settings by mutableStateOf(load())
        private set

    /** Apply a change immediately (live) and schedule a persistence write. */
    fun update(transform: (ThemeSettings) -> ThemeSettings) {
        settings = transform(settings)
        persist()
    }

    /**
     * Persist a value that originated elsewhere (e.g. the Theme Studio mutated
     * KaiteyoThemeState directly). Keeps this state in sync without bouncing.
     */
    fun accept(settings: ThemeSettings) {
        if (settings == this.settings) return
        this.settings = settings
        persist()
    }

    fun reset() {
        settings = ThemeSettings()
        persist()
    }

    /** Apply the persisted settings onto a live [KaiteyoThemeState]. */
    fun applyTo(themeState: KaiteyoThemeState) {
        themeState.accentScheme = settings.accentScheme()
        themeState.radiusConfig = settings.toRadiusConfig()
        themeState.glowConfig = settings.toGlowConfig()
        themeState.layoutConfig = settings.toLayoutConfig()
        themeState.animationConfig = settings.toAnimationConfig()
        themeState.typeScale = settings.toTypeScale()
    }

    private fun persist() {
        scope.launch {
            runCatching {
                appPreferences.themeSettingsJson.set(json.encodeToString(ThemeSettings.serializer(), settings))
            }
        }
    }

    private fun load(): ThemeSettings {
        val stored = runBlocking { appPreferences.themeSettingsJson.get() }
        if (!stored.isNullOrBlank()) {
            runCatching {
                val decoded = json.decodeFromString(ThemeSettings.serializer(), stored)
                return decoded
            }
        }
        return ThemeSettings()
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

}

val LocalThemeSettingsState = compositionLocalOf<ThemeSettingsState?> { null }

@Composable
fun rememberThemeSettingsState(
    appPreferences: PreferencesContract.AppPreferences
): ThemeSettingsState {
    return remember { ThemeSettingsState(appPreferences) }
}
