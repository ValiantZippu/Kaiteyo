package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.categories

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import ua.syt0r.kanji.presentation.common.nav.AccessibilitySettings
import ua.syt0r.kanji.presentation.common.nav.LocalNavigationSettings
import ua.syt0r.kanji.presentation.common.nav.NavigationSettingsState
import ua.syt0r.kanji.presentation.common.resources.string.getStrings
import ua.syt0r.kanji.presentation.common.theme.ThemeSettingsState
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.LocalSettingsNavigation
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingDescriptor
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingsScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.InfoSetting
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.LinkSetting
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.SettingGroup
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.SliderSetting
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.ToggleSetting

/**
 * Accessibility center — text scaling, navigation contrast, motion and
 * touch-target sizing. Everything here mutates the same persisted state the
 * nav shell and the theme system read, so changes take effect immediately.
 */
class AccessibilitySettingsCategory(
    private val themeSettingsState: ThemeSettingsState
) : SettingsScreenContract.Category {

    private val strings = getStrings()
    private val s = strings.center

    override val id: String = "accessibility"
    override val title: String = s.categoryAccessibility
    override val subtitle: String = s.categoryAccessibilitySubtitle
    override val keywords: List<String> = listOf(
        "accessibility", "a11y", "contrast", "scale", "text", "font", "motion",
        "animation", "hitbox", "touch", "target", "icon", "keyboard", "focus",
        "high contrast", "reduced motion", "text size"
    )
    override val icon: ImageVector? = Icons.Default.Accessibility

    // Captured from composition so the reset lambda can run outside it.
    private var navSettingsRef: NavigationSettingsState? = null

    override val reset: (suspend () -> Unit)? = {
        // Only the fields this category manages: theme text scale and the
        // navigation accessibility block. The rest of the theme and nav
        // settings are untouched.
        themeSettingsState.update { it.copy(fontScale = 1f) }
        navSettingsRef?.update { it.copy(accessibility = AccessibilitySettings()) }
    }

    override val descriptors: List<SettingDescriptor> = listOf(
        SettingDescriptor(
            id = "a11y_text_scale",
            title = s.a11yTextScale,
            description = s.a11yTextScaleDescription,
            keywords = listOf("font", "text", "size", "scale", "large", "readable"),
            render = { TextScaleSetting() }
        ),
        SettingDescriptor(
            id = "a11y_large_icons",
            title = s.a11yLargeIcons,
            description = s.a11yLargeIconsDescription,
            keywords = listOf("icon", "sidebar", "bubble", "launcher", "large", "size"),
            render = { LargeIconsSetting() }
        ),
        SettingDescriptor(
            id = "a11y_large_hitboxes",
            title = s.a11yLargeHitboxes,
            description = s.a11yLargeHitboxesDescription,
            keywords = listOf("touch", "target", "hitbox", "tap", "click", "area"),
            render = { LargeHitboxesSetting() }
        ),
        SettingDescriptor(
            id = "a11y_high_contrast",
            title = s.a11yHighContrast,
            description = s.a11yHighContrastDescription,
            keywords = listOf("contrast", "visibility", "readable", "high"),
            render = { HighContrastSetting() }
        ),
        SettingDescriptor(
            id = "a11y_reduce_motion",
            title = s.a11yReduceMotion,
            description = s.a11yReduceMotionDescription,
            keywords = listOf("motion", "animation", "reduced", "seizure", "smooth"),
            render = { ReduceMotionSetting() }
        ),
        SettingDescriptor(
            id = "a11y_keyboard_nav",
            title = s.a11yKeyboardNav,
            description = s.a11yKeyboardNavDescription,
            keywords = listOf("keyboard", "focus", "arrow", "tab", "navigation"),
            render = { KeyboardNavSetting() }
        ),
        SettingDescriptor(
            id = "a11y_shortcuts",
            title = s.shortcutsLink,
            description = s.shortcutsLinkDescription,
            keywords = listOf("shortcuts", "hotkeys", "keys", "bindings"),
            render = { ShortcutsLinkSetting() }
        )
    )

    @Composable
    override fun content(mainNavigationState: MainNavigationState) {
        val navSettings = LocalNavigationSettings.current ?: return
        navSettingsRef = navSettings

        SettingGroup(
            title = s.groupDisplay,
            children = listOf(
                { TextScaleSetting() },
                { HighContrastSetting() }
            )
        )
        SettingGroup(
            title = s.groupInteraction,
            children = listOf(
                { LargeIconsSetting() },
                { LargeHitboxesSetting() },
                { ReduceMotionSetting() },
                { KeyboardNavSetting() }
            )
        )
        SettingGroup(
            title = s.groupRelated,
            children = listOf(
                { ShortcutsLinkSetting() }
            )
        )
    }

    // ============================================
    // SETTINGS
    // ============================================

    @Composable
    private fun TextScaleSetting() {
        SliderSetting(
            title = s.a11yTextScale,
            description = s.a11yTextScaleDescription,
            value = themeSettingsState.settings.fontScale,
            range = 0.8f..1.4f,
            valueLabel = { "${(it * 100).toInt()}%" },
            onChanged = { scale ->
                themeSettingsState.update { it.copy(fontScale = scale) }
            }
        )
    }

    @Composable
    private fun LargeIconsSetting() {
        val navSettings = LocalNavigationSettings.current ?: return
        ToggleSetting(
            title = s.a11yLargeIcons,
            description = s.a11yLargeIconsDescription,
            checked = navSettings.settings.accessibility.largerIcons,
            onChanged = { enabled ->
                navSettings.update { settings ->
                    settings.copy(
                        accessibility = settings.accessibility.copy(largerIcons = enabled)
                    )
                }
            }
        )
    }

    @Composable
    private fun LargeHitboxesSetting() {
        val navSettings = LocalNavigationSettings.current ?: return
        ToggleSetting(
            title = s.a11yLargeHitboxes,
            description = s.a11yLargeHitboxesDescription,
            checked = navSettings.settings.accessibility.largerHitboxes,
            onChanged = { enabled ->
                navSettings.update { settings ->
                    settings.copy(
                        accessibility = settings.accessibility.copy(largerHitboxes = enabled)
                    )
                }
            }
        )
    }

    @Composable
    private fun HighContrastSetting() {
        val navSettings = LocalNavigationSettings.current ?: return
        ToggleSetting(
            title = s.a11yHighContrast,
            description = s.a11yHighContrastDescription,
            checked = navSettings.settings.accessibility.highContrast,
            onChanged = { enabled ->
                navSettings.update { settings ->
                    settings.copy(
                        accessibility = settings.accessibility.copy(highContrast = enabled)
                    )
                }
            }
        )
    }

    @Composable
    private fun ReduceMotionSetting() {
        val navSettings = LocalNavigationSettings.current ?: return
        ToggleSetting(
            title = s.a11yReduceMotion,
            description = s.a11yReduceMotionDescription,
            checked = navSettings.settings.accessibility.reducedMotion,
            onChanged = { enabled ->
                navSettings.update { settings ->
                    settings.copy(
                        accessibility = settings.accessibility.copy(reducedMotion = enabled)
                    )
                }
            }
        )
    }

    @Composable
    private fun KeyboardNavSetting() {
        val navSettings = LocalNavigationSettings.current ?: return
        InfoSetting(
            title = s.a11yKeyboardNav,
            description = s.a11yKeyboardNavDescription,
            value = if (navSettings.settings.animationsEnabled) "Arrows + Tab" else "Enabled"
        )
    }

    @Composable
    private fun ShortcutsLinkSetting() {
        val navigationState = LocalSettingsNavigation.current ?: return
        LinkSetting(
            title = s.shortcutsLink,
            description = s.shortcutsLinkDescription,
            onClick = { navigationState.navigate(MainDestination.KeyboardShortcuts) }
        )
    }

}
