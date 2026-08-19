package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.categories

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import ua.syt0r.kanji.presentation.common.debug.DebugSettingsState
import ua.syt0r.kanji.presentation.common.nav.AccessibilitySettings
import ua.syt0r.kanji.presentation.common.nav.LocalNavigationSettings
import ua.syt0r.kanji.presentation.common.nav.NavigationMode
import ua.syt0r.kanji.presentation.common.nav.NavigationSettingsState
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingDescriptor
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingsScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.SettingGroup
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.ToggleSetting

/**
 * Debug settings category — developer-facing controls for overlays, motion
 * overrides, and forced modes. Every toggle writes to real persisted state
 * that the nav shell and the debug panel read live.
 *
 * Positioned at the bottom of the settings list (after About) so it is
 * discoverable but never in the way of normal users.
 */
class DebugSettingsCategory(
    private val debugSettingsState: DebugSettingsState,
    private val navSettingsState: NavigationSettingsState
) : SettingsScreenContract.Category {

    override val id: String = "debug"
    override val title: String = "Debug"
    override val subtitle: String = "Developer tools, overlays, and motion controls"
    override val keywords: List<String> = listOf(
        "debug", "developer", "fps", "viewport", "page", "overlay", "info",
        "animation", "motion", "reduced", "contrast", "force", "navigation",
        "mode", "theme", "panel", "screen", "bug", "report"
    )
    override val icon: ImageVector? = Icons.Default.BugReport

    override val reset: (suspend () -> Unit)? = {
        debugSettingsState.reset()
        navSettingsState.update { it.copy(
            animationsEnabled = true,
            accessibility = AccessibilitySettings()
        ) }
    }

    override val descriptors: List<SettingDescriptor> = listOf(
        SettingDescriptor(
            id = "debug_page_info",
            title = "Show panel name",
            description = "Displays the current panel/screen name in the corner for bug reports",
            keywords = listOf("page", "panel", "screen", "name", "label", "corner", "bug", "report"),
            render = { PageInfoToggle() }
        ),
        SettingDescriptor(
            id = "debug_fps",
            title = "Show FPS",
            description = "Live frames-per-second readout in the debug overlay",
            keywords = listOf("fps", "frame", "rate", "performance", "smooth"),
            render = { FpsToggle() }
        ),
        SettingDescriptor(
            id = "debug_viewport",
            title = "Show viewport size",
            description = "Current window/viewport dimensions in the debug overlay",
            keywords = listOf("viewport", "size", "window", "dimension", "width", "height", "resolution"),
            render = { ViewportToggle() }
        ),
        SettingDescriptor(
            id = "debug_disable_anim",
            title = "Disable animations",
            description = "Force-skip all navigation transitions (for debugging motion issues)",
            keywords = listOf("animation", "disable", "transition", "motion", "skip"),
            render = { DisableAnimationsToggle() }
        ),
        SettingDescriptor(
            id = "debug_reduce_motion",
            title = "Reduce motion",
            description = "Honors the OS reduced-motion preference for all animations",
            keywords = listOf("reduced", "motion", "accessibility", "animation", "seizure"),
            render = { ReduceMotionToggle() }
        ),
        SettingDescriptor(
            id = "debug_high_contrast",
            title = "High contrast",
            description = "Increase border and text contrast for better visibility",
            keywords = listOf("contrast", "visibility", "readable", "high", "accessibility"),
            render = { HighContrastToggle() }
        ),
        SettingDescriptor(
            id = "debug_force_nav_mode",
            title = "Force navigation mode",
            description = "Override the current navigation mode (Sidebar / Floating)",
            keywords = listOf("navigation", "mode", "sidebar", "floating", "force", "override"),
            render = { ForceNavModeSetting() }
        )
    )

    @Composable
    override fun content(mainNavigationState: MainNavigationState) {
        SettingGroup(
            title = "Overlays",
            children = listOf(
                { PageInfoToggle() },
                { FpsToggle() },
                { ViewportToggle() }
            )
        )
        SettingGroup(
            title = "Motion & Contrast",
            children = listOf(
                { DisableAnimationsToggle() },
                { ReduceMotionToggle() },
                { HighContrastToggle() }
            )
        )
        SettingGroup(
            title = "Force",
            children = listOf(
                { ForceNavModeSetting() }
            )
        )
    }

    // ============================================
    // OVERLAYS
    // ============================================

    @Composable
    private fun PageInfoToggle() {
        ToggleSetting(
            title = "Show panel name",
            description = "Displays the current panel/screen name in the corner for bug reports",
            checked = debugSettingsState.settings.showPageInfo,
            onChanged = { enabled ->
                debugSettingsState.update { it.copy(showPageInfo = enabled) }
            }
        )
    }

    @Composable
    private fun FpsToggle() {
        ToggleSetting(
            title = "Show FPS",
            description = "Live frames-per-second readout in the debug overlay",
            checked = debugSettingsState.settings.showFps,
            onChanged = { enabled ->
                debugSettingsState.update { it.copy(showFps = enabled) }
            }
        )
    }

    @Composable
    private fun ViewportToggle() {
        ToggleSetting(
            title = "Show viewport size",
            description = "Current window/viewport dimensions in the debug overlay",
            checked = debugSettingsState.settings.showViewport,
            onChanged = { enabled ->
                debugSettingsState.update { it.copy(showViewport = enabled) }
            }
        )
    }

    // ============================================
    // MOTION & CONTRAST
    // ============================================

    @Composable
    private fun DisableAnimationsToggle() {
        ToggleSetting(
            title = "Disable animations",
            description = "Force-skip all navigation transitions",
            checked = debugSettingsState.settings.disableAnimations,
            onChanged = { enabled ->
                debugSettingsState.update { it.copy(disableAnimations = enabled) }
            }
        )
    }

    @Composable
    private fun ReduceMotionToggle() {
        val navSettings = LocalNavigationSettings.current ?: return
        ToggleSetting(
            title = "Reduce motion",
            description = "Honors the OS reduced-motion preference for all animations",
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
    private fun HighContrastToggle() {
        val navSettings = LocalNavigationSettings.current ?: return
        ToggleSetting(
            title = "High contrast",
            description = "Increase border and text contrast for better visibility",
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

    // ============================================
    // FORCE NAVIGATION
    // ============================================

    @Composable
    private fun ForceNavModeSetting() {
        val navSettings = LocalNavigationSettings.current ?: return
        val currentMode = navSettings.settings.mode

        ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.DropdownSetting(
            title = "Navigation mode",
            description = "Override the current navigation mode",
            options = NavigationMode.entries,
            labelOf = { it.name },
            selected = currentMode,
            onSelected = { mode -> navSettings.setMode(mode) }
        )
    }

}
