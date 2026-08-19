package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.categories

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector
import ua.syt0r.kanji.core.knowledge.LearnerProfile
import ua.syt0r.kanji.core.knowledge.LearnerProfileStore
import ua.syt0r.kanji.core.knowledge.level.DisplayOverridesStore
import ua.syt0r.kanji.core.theme_manager.ThemeManager
import ua.syt0r.kanji.core.user_data.preferences.PreferencesTheme
import ua.syt0r.kanji.presentation.common.resources.string.getStrings
import ua.syt0r.kanji.presentation.common.theme.AllAccentSchemes
import ua.syt0r.kanji.presentation.common.theme.AnimationSpeed
import ua.syt0r.kanji.presentation.common.theme.CornerRadiusStyle
import ua.syt0r.kanji.presentation.common.theme.ThemeSettingsState
import ua.syt0r.kanji.presentation.common.theme.UIDensity
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.AppearancePreview
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.LocalSettingsNavigation
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingDescriptor
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingsScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.ColorSetting
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.LinkSetting
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.SegmentedSetting
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.SettingGroup
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.SliderSetting
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.ToggleSetting

class AppearanceSettingsCategory(
    private val themeManager: ThemeManager,
    private val themeSettingsState: ThemeSettingsState,
    private val learnerProfileStore: LearnerProfileStore,
    private val displayOverridesStore: DisplayOverridesStore
) : SettingsScreenContract.Category {

    private val strings = getStrings()
    private val s = strings.center

    override val id: String = "appearance"
    override val title: String = s.categoryAppearance
    override val subtitle: String = s.categoryAppearanceSubtitle
    override val keywords: List<String> =
        listOf("theme", "color", "accent", "font", "text", "density", "radius", "corners", "dark", "light", "oled")
    override val icon: ImageVector? = Icons.Default.Palette

    override val reset: (suspend () -> Unit)? = {
        themeSettingsState.reset()
    }

    private fun themeLabel(theme: PreferencesTheme): String = when (theme) {
        PreferencesTheme.System -> strings.settings.themeSystem
        PreferencesTheme.Light -> strings.settings.themeLight
        PreferencesTheme.Dark -> strings.settings.themeDark
        PreferencesTheme.Amoled -> strings.settings.themeAmoled
        PreferencesTheme.Sepia -> strings.settings.themeSepia
        PreferencesTheme.Cream -> strings.settings.themeCream
        PreferencesTheme.Paper -> strings.settings.themePaper
        PreferencesTheme.Midnight -> strings.settings.themeMidnight
    }

    private fun radiusLabel(style: CornerRadiusStyle): String = when (style) {
        CornerRadiusStyle.Square -> s.radiusSquare
        CornerRadiusStyle.Rounded -> s.radiusRounded
        CornerRadiusStyle.VeryRounded -> s.radiusVeryRounded
        CornerRadiusStyle.Soft -> s.radiusSoft
    }

    private fun densityLabel(density: UIDensity): String = when (density) {
        UIDensity.Compact -> s.densityCompact
        UIDensity.Comfortable -> s.densityComfortable
        UIDensity.Spacious -> s.densitySpacious
    }

    private fun speedLabel(speed: AnimationSpeed): String = when (speed) {
        AnimationSpeed.Instant -> s.speedOff
        AnimationSpeed.Fast -> s.speedFast
        AnimationSpeed.Normal -> s.speedNormal
        AnimationSpeed.Slow -> s.speedSlow
    }

    private val accentSwatches: List<Pair<String, Color>> =
        AllAccentSchemes.map { scheme -> scheme.name to scheme.previewColors.first() }

    override val descriptors: List<SettingDescriptor> = listOf(
        SettingDescriptor(
            id = "learner_profile",
            title = "Learner profile",
            description = "Level-adaptive presentation: what dictionary pages show, sentence difficulty, graph depth and card presets.",
            keywords = listOf("level", "profile", "beginner", "intermediate", "advanced", "adaptive", "native", "research", "custom"),
            render = { LearnerProfileSetting() }
        ),
        SettingDescriptor(
            id = "card_settings",
            title = "Card layouts",
            description = "Show, hide and reorder the cards on kanji, word, sentence, grammar and collection pages. Presets per learner profile.",
            keywords = listOf("cards", "layout", "customize", "preset", "kanji page", "word page", "reorder", "visibility"),
            render = { CardSettingsLink() }
        ),
        SettingDescriptor(
            id = "romaji_override",
            title = "Show romaji on dictionary pages",
            description = "Romanized readings under the kana on kanji and word pages. Off = follow the learner profile. This never changes the stored data — just what is displayed (spec §24).",
            keywords = listOf("romaji", "romanization", "reading", "kana", "dictionary", "display", "override"),
            render = { RomajiOverrideSetting() }
        ),
        SettingDescriptor(
            id = "theme_mode",
            title = s.themeMode,
            description = s.themeModeDescription,
            keywords = listOf("light", "dark", "system", "amoled", "oled", "black"),
            render = { ThemeModeSetting() }
        ),
        SettingDescriptor(
            id = "accent_color",
            title = s.accentColor,
            description = s.accentColorDescription,
            keywords = listOf("color", "primary", "palette", "signature", "ocean", "forest"),
            render = { AccentColorSetting() }
        ),
        SettingDescriptor(
            id = "corner_radius",
            title = s.cornerRadius,
            description = s.cornerRadiusDescription,
            keywords = listOf("corners", "rounding", "square", "shape", "rounded"),
            render = { CornerRadiusSetting() }
        ),
        SettingDescriptor(
            id = "density",
            title = s.density,
            description = s.densityDescription,
            keywords = listOf("compact", "spacious", "comfortable", "spacing", "padding", "size"),
            render = { DensitySetting() }
        ),
        SettingDescriptor(
            id = "animation_speed",
            title = s.animationSpeed,
            description = s.animationSpeedDescription,
            keywords = listOf("motion", "transition", "fast", "slow", "animation"),
            render = { AnimationSpeedSetting() }
        ),
        SettingDescriptor(
            id = "reduced_motion",
            title = s.reducedMotion,
            description = s.reducedMotionDescription,
            keywords = listOf("animation", "accessibility", "motion", "flicker"),
            render = { ReducedMotionSetting() }
        ),
        SettingDescriptor(
            id = "font_scale",
            title = s.fontScale,
            description = s.fontScaleDescription,
            keywords = listOf("font", "text", "typography", "size", "scale", "readable"),
            render = { FontScaleSetting() }
        ),
        SettingDescriptor(
            id = "title_scale",
            title = s.titleScale,
            description = s.titleScaleDescription,
            keywords = listOf("heading", "title", "headline", "typography", "size"),
            render = { TitleScaleSetting() }
        ),
        SettingDescriptor(
            id = "line_height",
            title = s.lineHeight,
            description = s.lineHeightDescription,
            keywords = listOf("line", "height", "leading", "spacing", "typography"),
            render = { LineHeightSetting() }
        ),
        SettingDescriptor(
            id = "letter_spacing",
            title = s.letterSpacing,
            description = s.letterSpacingDescription,
            keywords = listOf("letter", "tracking", "spacing", "typography"),
            render = { LetterSpacingSetting() }
        ),
        SettingDescriptor(
            id = "page_transition",
            title = s.pageTransition,
            description = s.pageTransitionDescription,
            keywords = listOf("page", "transition", "fade", "slide", "scale", "crossfade"),
            render = { PageTransitionSetting() }
        ),
        SettingDescriptor(
            id = "theme_transition",
            title = s.themeTransition,
            description = s.themeTransitionDescription,
            keywords = listOf("crossfade", "color", "transition", "smooth"),
            render = { ThemeTransitionSetting() }
        ),
        SettingDescriptor(
            id = "display_scale",
            title = s.displayScale,
            description = s.displayScaleDescription,
            keywords = listOf("ui", "scale", "zoom", "size", "interface"),
            render = { DisplayScaleSetting() }
        ),
        SettingDescriptor(
            id = "button_scale",
            title = s.buttonScale,
            description = s.buttonScaleDescription,
            keywords = listOf("button", "control", "size", "height", "touch"),
            render = { ButtonScaleSetting() }
        ),
        SettingDescriptor(
            id = "icon_scale",
            title = s.iconScale,
            description = s.iconScaleDescription,
            keywords = listOf("icon", "size", "scale", "glyph"),
            render = { IconScaleSetting() }
        ),
        SettingDescriptor(
            id = "appearance_preview",
            title = s.livePreviewLabel,
            description = s.changesApplyInstantly,
            keywords = listOf("preview", "live", "mockup"),
            render = { AppearancePreview() }
        ),
        SettingDescriptor(
            id = "theme_studio",
            title = s.openThemeStudio,
            description = s.openThemeStudioDescription,
            keywords = listOf("studio", "editor", "gradient", "custom", "advanced"),
            render = { ThemeStudioLink() }
        )
    )

    @Composable
    override fun content(mainNavigationState: MainNavigationState) {
        SettingGroup(
            title = s.groupTheme,
            children = listOf(
                { LearnerProfileSetting() },
                { RomajiOverrideSetting() },
                { ThemeModeSetting() },
                { AccentColorSetting() },
                { CornerRadiusSetting() },
                { DensitySetting() }
            )
        )
        SettingGroup(
            title = s.groupTypography,
            children = listOf(
                { FontScaleSetting() },
                { TitleScaleSetting() },
                { LineHeightSetting() },
                { LetterSpacingSetting() }
            )
        )
        SettingGroup(
            title = s.groupLayout,
            children = listOf(
                { DisplayScaleSetting() },
                { ButtonScaleSetting() },
                { IconScaleSetting() }
            )
        )
        SettingGroup(
            title = s.groupMotion,
            children = listOf(
                { AnimationSpeedSetting() },
                { ReducedMotionSetting() },
                { PageTransitionSetting() },
                { ThemeTransitionSetting() }
            )
        )
        SettingGroup(
            title = null,
            children = listOf(
                { AppearancePreview() }
            )
        )
        SettingGroup(
            title = s.groupRelated,
            children = listOf(
                { ThemeStudioLink() }
            )
        )
    }

    // ============================================
    // SETTINGS
    // ============================================

    @Composable
    private fun LearnerProfileSetting() {
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        var selected by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(LearnerProfile.Intermediate) }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            selected = learnerProfileStore.load().profile
        }
        SegmentedSetting(
            title = "Learner profile",
            description = "Adapts dictionary pages, example difficulty and graph depth to your level. Switch any time — nothing is hidden permanently.",
            options = LearnerProfile.entries,
            labelOf = { it.displayName },
            selected = selected,
            onSelected = { profile ->
                selected = profile
                scope.launch { learnerProfileStore.saveProfile(profile) }
            }
        )
    }

    @Composable
    private fun RomajiOverrideSetting() {
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        var romaji by androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf(false)
        }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            romaji = displayOverridesStore.load().override == true
        }
        ToggleSetting(
            title = "Show romaji on dictionary pages",
            description = "Romanized readings under the kana on kanji and word pages. Off = follow the learner profile; the underlying data is never changed.",
            checked = romaji,
            onChanged = { enabled ->
                romaji = enabled
                scope.launch {
                    if (enabled) displayOverridesStore.setRomaji(true)
                    else displayOverridesStore.clearRomajiOverride()
                }
            }
        )
    }

    @Composable
    private fun ThemeModeSetting() {
        SegmentedSetting(
            title = s.themeMode,
            description = s.themeModeDescription,
            options = PreferencesTheme.entries,
            labelOf = ::themeLabel,
            selected = themeManager.currentTheme.value,
            onSelected = { themeManager.changeTheme(it) }
        )
    }

    @Composable
    private fun AccentColorSetting() {
        ColorSetting(
            title = s.accentColor,
            description = s.accentColorDescription,
            swatches = accentSwatches,
            selectedName = themeSettingsState.settings.accentName,
            onSelect = { name ->
                themeSettingsState.update { it.copy(accentName = name) }
            }
        )
    }

    @Composable
    private fun CornerRadiusSetting() {
        SegmentedSetting(
            title = s.cornerRadius,
            description = s.cornerRadiusDescription,
            options = CornerRadiusStyle.entries,
            labelOf = ::radiusLabel,
            selected = themeSettingsState.settings.radiusStyle,
            onSelected = { style ->
                themeSettingsState.update { it.copy(radiusStyle = style) }
            }
        )
    }

    @Composable
    private fun DensitySetting() {
        SegmentedSetting(
            title = s.density,
            description = s.densityDescription,
            options = UIDensity.entries,
            labelOf = ::densityLabel,
            selected = themeSettingsState.settings.density,
            onSelected = { density ->
                themeSettingsState.update { it.copy(density = density) }
            }
        )
    }

    @Composable
    private fun AnimationSpeedSetting() {
        SegmentedSetting(
            title = s.animationSpeed,
            description = s.animationSpeedDescription,
            options = AnimationSpeed.entries,
            labelOf = ::speedLabel,
            selected = themeSettingsState.settings.animationSpeed,
            onSelected = { speed ->
                themeSettingsState.update { it.copy(animationSpeed = speed) }
            }
        )
    }

    @Composable
    private fun ReducedMotionSetting() {
        ToggleSetting(
            title = s.reducedMotion,
            description = s.reducedMotionDescription,
            checked = themeSettingsState.settings.animationReducedMotion,
            onChanged = { enabled ->
                themeSettingsState.update { it.copy(animationReducedMotion = enabled) }
            }
        )
    }

    @Composable
    private fun FontScaleSetting() {
        SliderSetting(
            title = s.fontScale,
            description = s.fontScaleDescription,
            value = themeSettingsState.settings.fontScale,
            range = 0.8f..1.4f,
            valueLabel = { "${(it * 100).toInt()}%" },
            onChanged = { scale ->
                themeSettingsState.update { it.copy(fontScale = scale) }
            }
        )
    }

    @Composable
    private fun TitleScaleSetting() {
        SliderSetting(
            title = s.titleScale,
            description = s.titleScaleDescription,
            value = themeSettingsState.settings.titleScale,
            range = 0.8f..1.4f,
            valueLabel = { "${(it * 100).toInt()}%" },
            onChanged = { scale ->
                themeSettingsState.update { it.copy(titleScale = scale) }
            }
        )
    }

    @Composable
    private fun LineHeightSetting() {
        SliderSetting(
            title = s.lineHeight,
            description = s.lineHeightDescription,
            value = themeSettingsState.settings.lineHeight,
            range = 0.8f..1.4f,
            valueLabel = { "${(it * 100).toInt()}%" },
            onChanged = { scale ->
                themeSettingsState.update { it.copy(lineHeight = scale) }
            }
        )
    }

    @Composable
    private fun LetterSpacingSetting() {
        SliderSetting(
            title = s.letterSpacing,
            description = s.letterSpacingDescription,
            value = themeSettingsState.settings.letterSpacing,
            range = 0f..1f,
            valueLabel = { "${it}sp" },
            onChanged = { spacing ->
                themeSettingsState.update { it.copy(letterSpacing = spacing) }
            }
        )
    }

    @Composable
    private fun PageTransitionSetting() {
        SegmentedSetting(
            title = s.pageTransition,
            description = s.pageTransitionDescription,
            options = ua.syt0r.kanji.presentation.common.theme.PageTransitionType.entries,
            labelOf = { it.displayName },
            selected = themeSettingsState.settings.pageTransition,
            onSelected = { transition ->
                themeSettingsState.update { it.copy(pageTransition = transition) }
            }
        )
    }

    @Composable
    private fun ThemeTransitionSetting() {
        ToggleSetting(
            title = s.themeTransition,
            description = s.themeTransitionDescription,
            checked = themeSettingsState.settings.themeTransitionEnabled,
            onChanged = { enabled ->
                themeSettingsState.update { it.copy(themeTransitionEnabled = enabled) }
            }
        )
    }

    @Composable
    private fun DisplayScaleSetting() {
        SliderSetting(
            title = s.displayScale,
            description = s.displayScaleDescription,
            value = themeSettingsState.settings.displayScale,
            range = 0.85f..1.3f,
            valueLabel = { "${(it * 100).toInt()}%" },
            onChanged = { scale ->
                themeSettingsState.update { it.copy(displayScale = scale) }
            }
        )
    }

    @Composable
    private fun ButtonScaleSetting() {
        SliderSetting(
            title = s.buttonScale,
            description = s.buttonScaleDescription,
            value = themeSettingsState.settings.buttonScale,
            range = 0.85f..1.3f,
            valueLabel = { "${(it * 100).toInt()}%" },
            onChanged = { scale ->
                themeSettingsState.update { it.copy(buttonScale = scale) }
            }
        )
    }

    @Composable
    private fun IconScaleSetting() {
        SliderSetting(
            title = s.iconScale,
            description = s.iconScaleDescription,
            value = themeSettingsState.settings.iconScale,
            range = 0.85f..1.3f,
            valueLabel = { "${(it * 100).toInt()}%" },
            onChanged = { scale ->
                themeSettingsState.update { it.copy(iconScale = scale) }
            }
        )
    }

    @Composable
    private fun CardSettingsLink() {
        val navigationState = LocalSettingsNavigation.current ?: return
        LinkSetting(
            title = "Customize card layouts",
            description = "Show/hide/reorder cards for each dictionary page type, with presets.",
            icon = androidx.compose.material.icons.Icons.Default.ViewModule,
            onClick = { navigationState.navigate(MainDestination.CardSettings()) }
        )
    }

    @Composable
    private fun ThemeStudioLink() {
        val navigationState = LocalSettingsNavigation.current ?: return
        LinkSetting(
            title = s.openThemeStudio,
            description = s.openThemeStudioDescription,
            icon = Icons.Default.Palette,
            onClick = { navigationState.navigate(MainDestination.AppearanceStudio) }
        )
    }

}
