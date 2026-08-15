package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.categories

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import ua.syt0r.kanji.presentation.common.nav.LocalNavigationSettings
import ua.syt0r.kanji.presentation.common.nav.NavigationSettingsPage
import ua.syt0r.kanji.presentation.common.nav.NavigationSettingsState
import ua.syt0r.kanji.presentation.common.nav.snapPointFor
import ua.syt0r.kanji.presentation.common.nav.rememberFormFactor
import ua.syt0r.kanji.presentation.common.resources.string.getStrings
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingDescriptor
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.SettingsScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.InfoSetting

class NavigationSettingsCategory : SettingsScreenContract.Category {

    private val strings = getStrings()
    private val s = strings.center

    override val id: String = "navigation"
    override val title: String = s.categoryNavigation
    override val subtitle: String = s.categoryNavigationSubtitle
    override val keywords: List<String> = listOf(
        "sidebar", "bubble", "launcher", "launchpad", "floating", "snap", "width",
        "compact", "expanded", "dock", "position", "edge", "phone", "tooltip",
        "mode", "animation", "placement"
    )
    override val icon: ImageVector? = Icons.Default.Apps

    // Captured from composition (the nav shell provides LocalNavigationSettings)
    // so the reset lambda can run outside composition.
    private var navSettingsRef: NavigationSettingsState? = null

    override val reset: (suspend () -> Unit)? = {
        navSettingsRef?.reset()
    }

    private fun navDescriptor(what: String): SettingDescriptor? =
        descriptors.firstOrNull { it.id == "nav_$what" }

    override val descriptors: List<SettingDescriptor> = listOf(
        SettingDescriptor(
            id = "nav_mode",
            title = "Navigation mode",
            description = "Full sidebar, compact rail or floating launcher",
            keywords = listOf("mode", "expanded", "compact", "bubble", "sidebar", "floating"),
            render = { NavigationInfo("mode") }
        ),
        SettingDescriptor(
            id = "nav_default_mode",
            title = "Default mode",
            description = "Mode used when the app launches",
            keywords = listOf("startup", "default", "mode", "launch"),
            render = { NavigationInfo("default_mode") }
        ),
        SettingDescriptor(
            id = "nav_remember_mode",
            title = "Remember previous mode",
            description = "Restore the last used mode on startup",
            keywords = listOf("remember", "previous", "restore", "startup"),
            render = { NavigationInfo("remember_mode") }
        ),
        SettingDescriptor(
            id = "nav_placement",
            title = "Sidebar position",
            description = "Left, right, top or bottom edge placement",
            keywords = listOf("position", "edge", "left", "right", "top", "bottom", "placement"),
            render = { NavigationInfo("placement") }
        ),
        SettingDescriptor(
            id = "nav_expanded_width",
            title = "Sidebar width",
            description = "Predefined expanded widths — no free resizing",
            keywords = listOf("width", "size", "expanded", "sidebar", "narrow", "wide"),
            render = { NavigationInfo("expanded_width") }
        ),
        SettingDescriptor(
            id = "nav_icon_size",
            title = "Sidebar icon size",
            description = "Icon scale in the dock",
            keywords = listOf("icon", "size", "sidebar", "scale"),
            render = { NavigationInfo("icon_size") }
        ),
        SettingDescriptor(
            id = "nav_compact_spacing",
            title = "Compact item spacing",
            description = "Vertical rhythm between dock items",
            keywords = listOf("spacing", "compact", "gap", "padding"),
            render = { NavigationInfo("compact_spacing") }
        ),
        SettingDescriptor(
            id = "nav_labels",
            title = "Labels",
            description = "Always, on hover or never",
            keywords = listOf("labels", "text", "tooltip", "show"),
            render = { NavigationInfo("labels") }
        ),
        SettingDescriptor(
            id = "nav_bubble_size",
            title = "Bubble size",
            description = "Floating launcher diameter",
            keywords = listOf("bubble", "floating", "launcher", "size", "diameter"),
            render = { NavigationInfo("bubble_size") }
        ),
        SettingDescriptor(
            id = "nav_bubble_icon",
            title = "Bubble icon size",
            description = "Icon inside the floating launcher",
            keywords = listOf("bubble", "icon", "size", "launcher"),
            render = { NavigationInfo("bubble_icon") }
        ),
        SettingDescriptor(
            id = "nav_bubble_fade",
            title = "Auto fade",
            description = "Fade the launcher after inactivity",
            keywords = listOf("fade", "idle", "opacity", "transparent", "auto"),
            render = { NavigationInfo("bubble_fade") }
        ),
        SettingDescriptor(
            id = "nav_idle_opacity",
            title = "Idle opacity",
            description = "Transparency of the launcher while faded",
            keywords = listOf("opacity", "fade", "idle", "alpha"),
            render = { NavigationInfo("idle_opacity") }
        ),
        SettingDescriptor(
            id = "nav_snap_position",
            title = "Snap position",
            description = "Where the launcher snaps — visual picker",
            keywords = listOf("snap", "position", "anchor", "corner", "edge", "dock"),
            render = { NavigationInfo("snap_position") }
        ),
        SettingDescriptor(
            id = "nav_snap_sensitivity",
            title = "Snap sensitivity",
            description = "How close before the launcher snaps",
            keywords = listOf("snap", "sensitivity", "distance", "magnet"),
            render = { NavigationInfo("snap_sensitivity") }
        ),
        SettingDescriptor(
            id = "nav_bubble_animation",
            title = "Launcher animation speed",
            description = "Movement and launchpad animation speed",
            keywords = listOf("animation", "speed", "bubble", "launchpad", "motion"),
            render = { NavigationInfo("bubble_animation") }
        ),
        SettingDescriptor(
            id = "nav_phone_position",
            title = "Phone navigation position",
            description = "Top or bottom bar on phones",
            keywords = listOf("phone", "position", "top", "bottom", "bar"),
            render = { NavigationInfo("phone_position") }
        ),
        SettingDescriptor(
            id = "nav_phone_launcher",
            title = "Phone launcher position",
            description = "Floating launcher corner on phones",
            keywords = listOf("phone", "launcher", "corner", "bubble"),
            render = { NavigationInfo("phone_launcher") }
        ),
        SettingDescriptor(
            id = "nav_animations",
            title = "Navigation animations",
            description = "Enable and tune layout transitions",
            keywords = listOf("animation", "transition", "duration", "motion"),
            render = { NavigationInfo("animations") }
        ),
        SettingDescriptor(
            id = "nav_reduced_motion",
            title = "Reduced motion",
            description = "Disable navigation animations",
            keywords = listOf("motion", "accessibility", "animation"),
            render = { NavigationInfo("reduced_motion") }
        ),
        SettingDescriptor(
            id = "nav_accessibility",
            title = "Larger icons and hitboxes",
            description = "Navigation accessibility options",
            keywords = listOf("accessibility", "icons", "hitbox", "large", "contrast"),
            render = { NavigationInfo("accessibility") }
        ),
        SettingDescriptor(
            id = "nav_preview",
            title = "Live preview",
            description = "Miniature app preview — updates instantly",
            keywords = listOf("preview", "live", "mockup", "snap"),
            render = { NavigationInfo("preview") }
        )
    )

    @Composable
    override fun content(mainNavigationState: MainNavigationState) {
        val navSettings = LocalNavigationSettings.current ?: return
        navSettingsRef = navSettings
        NavigationSettingsPage(
            navSettings = navSettings,
            formFactor = rememberFormFactor()
        )
    }

    // ============================================
    // SEARCH-ONLY INFO ROWS
    // The interactive controls live in the page
    // above; search results show current values.
    // ============================================

    @Composable
    private fun NavigationInfo(what: String) {
        val descriptor = navDescriptor(what) ?: return
        val navSettings = LocalNavigationSettings.current ?: return
        val settings = navSettings.settings
        val value = when (what) {
            "mode" -> settings.mode.name
            "default_mode" -> settings.defaultMode.name
            "remember_mode" -> if (settings.rememberPreviousMode) "On" else "Off"
            "placement" -> settings.desktopEdge.displayName
            "expanded_width" -> "${settings.sidebar.expandedWidth} dp"
            "icon_size" -> "${settings.sidebar.iconSize} dp"
            "compact_spacing" -> "${settings.sidebar.compactSpacing} dp"
            "labels" -> settings.sidebar.labelVisibility.name
            "bubble_size" -> "${settings.bubble.size} dp"
            "bubble_icon" -> "${settings.bubble.iconSize} dp"
            "bubble_fade" -> if (settings.bubble.autoFade) "On" else "Off"
            "idle_opacity" -> "${(settings.bubble.fadeOpacity * 100).toInt()}%"
            "snap_position" -> settings.snapPointFor(rememberFormFactor()).name
            "snap_sensitivity" -> settings.bubble.snapSensitivity.toString()
            "bubble_animation" -> "${settings.bubble.animationSpeed}x"
            "phone_position" -> settings.phone.edge.displayName
            "phone_launcher" -> settings.phone.snapPoint.name
            "animations" ->
                if (settings.animationsEnabled) "${settings.animationDurationMs} ms" else "Off"
            "reduced_motion" ->
                if (settings.accessibility.reducedMotion) "On" else "Off"
            "accessibility" -> {
                val a = settings.accessibility
                listOf(
                    a.largerIcons to "Icons",
                    a.largerHitboxes to "Hitboxes",
                    a.highContrast to "Contrast"
                ).filter { it.first }.joinToString { it.second }.ifBlank { "Off" }
            }
            else -> "Live"
        }
        InfoSetting(
            title = descriptor.title,
            description = descriptor.description,
            value = value
        )
    }

}
