package ua.syt0r.kanji.presentation.common.nav

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.SidebarPosition

// ============================================
// KAITEYO NAVIGATION MODEL
// Exactly three modes · adaptive form factors ·
// persistent settings for desktop/tablet and phone.
// ============================================

/** The three navigation modes. There are exactly three — no more. */
@Serializable
enum class NavigationMode {
    Expanded,
    Compact,
    Bubble
}

/**
 * Edge margin the floating launcher keeps from the screen edges. Shared with
 * the snackbar clearance so the launcher position and the published bottom
 * space can never drift apart.
 */
val BubbleEdgeMargin: Dp = Dimens.Space3

/**
 * Anchor points the floating launcher can snap to.
 * Desktop & tablet use the full set; phone restricts to the four corners.
 */
@Serializable
enum class BubbleAnchor {
    Left,
    Right,
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight
}

/** How labels are shown in the sidebar. */
@Serializable
enum class NavLabelVisibility {
    Always,
    Hover,
    Never
}

/**
 * Adaptive layout tiers. Each tier is designed independently instead of
 * simply shrinking the desktop UI.
 */
enum class FormFactor {
    Phone,
    SmallTablet,
    LargeTablet,
    CompactWindow,
    Desktop
}

/** Whether the current platform is touch-first (phone). */
val FormFactor.isPhone: Boolean get() = this == FormFactor.Phone

/** Whether the platform can host navigation on any of the four screen edges. */
val FormFactor.supportsFourEdges: Boolean get() = !isPhone

// ============================================
// BUBBLE SETTINGS
// ============================================

@Serializable
data class BubbleSettings(
    val size: Int = 56,
    val iconSize: Int = 26,
    val snapSensitivity: Int = 80,
    val autoFade: Boolean = true,
    val fadeDelayMs: Long = 4000,
    val fadeOpacity: Float = 0.35f,
    val defaultAnchor: BubbleAnchor = BubbleAnchor.BottomRight,
    val animationSpeed: Float = 1.0f
)

// ============================================
// SIDEBAR SETTINGS
// ============================================

/** Predefined expanded widths (dp) — fixed, no free dragging. */
val ExpandedWidthOptions = listOf(220, 260, 300, 340)

@Serializable
data class SidebarSettings(
    val expandedWidthIndex: Int = 1,
    val iconSize: Int = 22,
    val compactSpacing: Int = 8,
    val labelVisibility: NavLabelVisibility = NavLabelVisibility.Always
) {
    val expandedWidth: Int
        get() = ExpandedWidthOptions.getOrElse(expandedWidthIndex) { ExpandedWidthOptions[1] }
}

// ============================================
// PHONE SETTINGS (stored separately from desktop)
// ============================================

@Serializable
data class PhoneNavigationSettings(
    val edge: SidebarPosition = SidebarPosition.Bottom,
    val bubbleAnchor: BubbleAnchor = BubbleAnchor.BottomRight,
    val bubbleOffsetX: Int = 0,
    val bubbleOffsetY: Int = 0
)

// ============================================
// ACCESSIBILITY SETTINGS
// ============================================

@Serializable
data class AccessibilitySettings(
    val reducedMotion: Boolean = false,
    val largerHitboxes: Boolean = false,
    val largerIcons: Boolean = false,
    val highContrast: Boolean = false
)

// ============================================
// TOP-LEVEL NAVIGATION SETTINGS
// ============================================

@Serializable
data class NavigationSettings(
    val mode: NavigationMode = NavigationMode.Expanded,
    val rememberPreviousMode: Boolean = true,
    /** When [rememberPreviousMode] is off, the app always starts in this mode. */
    val defaultMode: NavigationMode = NavigationMode.Expanded,
    /** Last used mode — restored when [rememberPreviousMode] is enabled. */
    val lastMode: NavigationMode? = null,
    val animationsEnabled: Boolean = true,
    /** Base duration for all navigation transitions, in milliseconds. */
    val animationDurationMs: Int = 260,
    val desktopEdge: SidebarPosition = SidebarPosition.Left,
    val bubbleAnchor: BubbleAnchor = BubbleAnchor.BottomRight,
    val bubbleOffsetX: Int = 0,
    val bubbleOffsetY: Int = 0,
    val bubble: BubbleSettings = BubbleSettings(),
    val phone: PhoneNavigationSettings = PhoneNavigationSettings(),
    val sidebar: SidebarSettings = SidebarSettings(),
    val accessibility: AccessibilitySettings = AccessibilitySettings()
) {
    /** Duration honoring the animations toggle and reduced-motion accessibility. */
    fun effectiveDurationMs(animations: Boolean): Int =
        if (animations) animationDurationMs else 0
}

/** Convenience projections used across the nav system. */
val NavigationSettings.effectiveEdge: SidebarPosition
    get() = desktopEdge

/** Desktop/tablet share one bubble anchor; phone keeps its own. */
fun NavigationSettings.bubbleAnchorFor(formFactor: FormFactor): BubbleAnchor =
    if (formFactor.isPhone) phone.bubbleAnchor else bubbleAnchor

fun NavigationSettings.bubbleOffsetFor(formFactor: FormFactor): Pair<Int, Int> =
    if (formFactor.isPhone) phone.bubbleOffsetX to phone.bubbleOffsetY
    else bubbleOffsetX to bubbleOffsetY

fun NavigationSettings.bubbleSettingsFor(formFactor: FormFactor): BubbleSettings = bubble

/** Pick the edge for the current form factor. Phone only supports Top/Bottom. */
fun NavigationSettings.edgeFor(formFactor: FormFactor): SidebarPosition {
    if (!formFactor.isPhone) return desktopEdge
    return when (phone.edge) {
        SidebarPosition.Top -> SidebarPosition.Top
        else -> SidebarPosition.Bottom
    }
}

/** Icons are scaled up when the larger-icons accessibility flag is on. */
fun AccessibilitySettings.scaledIconSize(base: Int): Int =
    if (largerIcons) (base * 1.2f).toInt() else base

/** Hitboxes are enlarged when the larger-hitboxes accessibility flag is on. */
fun AccessibilitySettings.scaledHitbox(base: Int): Int =
    if (largerHitboxes) (base * 1.25f).toInt() else base

/** Map a legacy persisted [SidebarMode] name onto the new three-mode model. */
fun legacyModeToNavigationMode(name: String?): NavigationMode = when (name) {
    "FloatingIsland", "Docked" -> NavigationMode.Bubble
    "IconsOnly", "AutoHide", "Compact" -> NavigationMode.Compact
    "Expanded", null -> NavigationMode.Expanded
    else -> NavigationMode.Expanded
}
