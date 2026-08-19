package ua.syt0r.kanji.presentation.common.nav

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.SidebarPosition

// ============================================
// KAITEYO NAVIGATION MODEL
// Exactly two modes — Floating and Sidebar —
// with adaptive form factors and persistent
// settings for desktop/tablet and phone.
// ============================================

/**
 * The two navigation modes. There are exactly two — no more.
 *
 * - [Floating]: a movable launcher bubble with a full launchpad. The bubble
 *   is freely draggable and magnetizes to the nearest of the 12 snap points
 *   when released.
 * - [Sidebar]: a structured dock on one of the four screen edges. Its
 *   layout is selected through [SidebarExpansion] (Expanded ↔ Compact).
 */
@Serializable
enum class NavigationMode {
    Floating,
    Sidebar
}

/** The two predefined sidebar layouts. No free resizing — exactly these two. */
@Serializable
enum class SidebarExpansion {
    Expanded,
    Compact
}

/**
 * Snap points the floating bubble can magnetize to. Desktop, tablet and phone
 * all use the full set of 12 — three per screen edge. Corner positions appear
 * twice (once per adjacent edge) at identical coordinates.
 */
@Serializable
enum class BubbleSnapPoint {
    // Top edge
    TopLeft,
    TopCenter,
    TopRight,
    // Bottom edge
    BottomLeft,
    BottomCenter,
    BottomRight,
    // Left edge
    LeftTop,
    LeftCenter,
    LeftBottom,
    // Right edge
    RightTop,
    RightCenter,
    RightBottom;

    companion object {
        /** The four visual corners, deduplicated across adjacent edges. */
        val Corners: List<BubbleSnapPoint> =
            listOf(TopLeft, TopRight, BottomLeft, BottomRight)

        /** All snap points, ordered for the visual 3-column picker grid. */
        val PickerOrder: List<BubbleSnapPoint> =
            listOf(
                TopLeft, TopCenter, TopRight,
                LeftTop, RightTop,
                LeftCenter, RightCenter,
                LeftBottom, RightBottom,
                BottomLeft, BottomCenter, BottomRight
            )
    }
}

/** Whether this snap point sits on the top or bottom edge (horizontal bar zone). */
val BubbleSnapPoint.isHorizontalEdge: Boolean
    get() = this in listOf(
        BubbleSnapPoint.TopLeft, BubbleSnapPoint.TopCenter, BubbleSnapPoint.TopRight,
        BubbleSnapPoint.BottomLeft, BubbleSnapPoint.BottomCenter, BubbleSnapPoint.BottomRight
    )

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

/**
 * Edge margin the floating launcher keeps from the screen edges. Shared with
 * the snackbar clearance so the launcher position and the published bottom
 * space can never drift apart.
 */
val BubbleEdgeMargin: Dp = Dimens.Space3

// ============================================
// BUBBLE SETTINGS
// ============================================

/**
 * Auto-hide (fade) presets. [Custom] uses [BubbleSettings.idleTimeoutMs].
 * The bubble never fully disappears — its hitbox stays interactive and the
 * glyph fades back in on hover (see [BubbleSettings.hoverReveal]).
 */
@Serializable
enum class AutoHidePreset(val timeoutMs: Long?) {
    Never(null),
    TenSeconds(10_000L),
    TwentySeconds(20_000L),
    ThirtySeconds(30_000L),
    OneMinute(60_000L),
    Custom(null)
}

@Serializable
data class BubbleSettings(
    val size: Int = 56,
    val iconSize: Int = 26,
    val snapSensitivity: Int = 80,
    /**
     * Size (dp) of the drag target preview ring shown while dragging. The
     * bubble always snaps to the nearest edge anchor on release; this only
     * controls how big the target telegraph is.
     */
    val snapDistance: Int = 140,
    /**
     * Minimum clearance (dp) the bubble keeps from the window edges, system
     * insets and the title bar area. Every snap anchor and every drag clamp
     * is derived from this value — positions are always validated against it.
     */
    val safeMargin: Int = 12,
    /**
     * How long a press must be held before the hold panel (mode controls)
     * opens, in milliseconds. Shorter presses are interpreted as clicks.
     */
    val holdDurationMs: Long = 480,
    val autoFade: Boolean = true,
    /** Auto-hide preset; [AutoHidePreset.Custom] uses [idleTimeoutMs]. */
    val autoHide: AutoHidePreset = AutoHidePreset.TwentySeconds,
    val fadeDelayMs: Long = 4000,
    val fadeOpacity: Float = 0.35f,
    /**
     * Idle timeout (ms) used when [autoHide] is [AutoHidePreset.Custom],
     * before the bubble starts fading.
     */
    val idleTimeoutMs: Long = 6000,
    /**
     * Hover reveal: when the bubble has faded, hovering over its area reveals
     * it again instantly. Off = only drag/tap brings it back.
     */
    val hoverReveal: Boolean = true,
    val animationSpeed: Float = 1.0f,
    /** Shadow elevation of the bubble glyph, in dp. */
    val elevation: Int = 12
) {
    /** Effective fade timeout for the current preset; null = never hide. */
    fun effectiveIdleTimeoutMs(): Long? = when {
        !autoFade || autoHide == AutoHidePreset.Never -> null
        autoHide == AutoHidePreset.Custom -> idleTimeoutMs
        else -> autoHide.timeoutMs
    }
}

// ============================================
// LAUNCHPAD SETTINGS
// ============================================

/** Direction the launchpad expands toward when it opens. */
@Serializable
enum class LaunchpadDirection {
    /** Expand toward the bubble's nearest edge (auto). */
    Auto,
    /** Always expand upward from a bottom-anchored bubble. */
    Up,
    /** Always expand downward from a top-anchored bubble. */
    Down
}

@Serializable
data class LaunchpadSettings(
    /** Panel width scale relative to the default (0.7..1.2). */
    val scale: Float = 1f,
    /** Tile / row spacing multiplier (0.7..1.5). */
    val spacing: Float = 1f,
    /** Direction the panel visually grows from the bubble. */
    val direction: LaunchpadDirection = LaunchpadDirection.Auto,
    /** Panel background opacity (0.6..1.0). */
    val opacity: Float = 0.96f,
    /** Staggered cascade reveal of tiles. */
    val staggeredReveal: Boolean = true
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
    val snapPoint: BubbleSnapPoint = BubbleSnapPoint.BottomRight,
    val snapOffsetX: Int = 0,
    val snapOffsetY: Int = 0
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
    val mode: NavigationMode = NavigationMode.Sidebar,
    val rememberPreviousMode: Boolean = true,
    /** When [rememberPreviousMode] is off, the app always starts in this mode. */
    val defaultMode: NavigationMode = NavigationMode.Sidebar,
    /** Last used mode — restored when [rememberPreviousMode] is enabled. */
    val lastMode: NavigationMode? = null,
    val animationsEnabled: Boolean = true,
    /** Base duration for all navigation transitions, in milliseconds. */
    val animationDurationMs: Int = 260,
    val desktopEdge: SidebarPosition = SidebarPosition.Left,
    /** Sidebar sub-layout: Expanded (icons + labels) or Compact (icons only). */
    val sidebarExpansion: SidebarExpansion = SidebarExpansion.Expanded,
    /** Desktop/tablet bubble snap point. */
    val snapPoint: BubbleSnapPoint = BubbleSnapPoint.BottomRight,
    /** Fine-grained drift from the exact snap anchor (dp, desktop/tablet). */
    val snapOffsetX: Int = 0,
    val snapOffsetY: Int = 0,
    val bubble: BubbleSettings = BubbleSettings(),
    val launchpad: LaunchpadSettings = LaunchpadSettings(),
    val phone: PhoneNavigationSettings = PhoneNavigationSettings(),
    val sidebar: SidebarSettings = SidebarSettings(),
    val accessibility: AccessibilitySettings = AccessibilitySettings(),
    /**
     * Show a small overlay with the current screen name (and its analytics
     * code) in the top-right corner. Off by default; enabled from Navigation
     * settings when reporting bugs so the reporter can name the exact screen.
     */
    val showPageName: Boolean = false
) {
    /** Duration honoring the animations toggle and reduced-motion accessibility. */
    fun effectiveDurationMs(animations: Boolean): Int =
        if (animations) animationDurationMs else 0
}

/** Convenience projection used across the nav system. */
val NavigationSettings.effectiveEdge: SidebarPosition
    get() = desktopEdge

/** The active bubble snap point for the current form factor. */
fun NavigationSettings.snapPointFor(formFactor: FormFactor): BubbleSnapPoint =
    if (formFactor.isPhone) phone.snapPoint else snapPoint

/** The active bubble micro-offset for the current form factor. */
fun NavigationSettings.snapOffsetFor(formFactor: FormFactor): Pair<Int, Int> =
    if (formFactor.isPhone) phone.snapOffsetX to phone.snapOffsetY
    else snapOffsetX to snapOffsetY

fun NavigationSettings.bubbleSettingsFor(formFactor: FormFactor): BubbleSettings = bubble

/** The active sidebar layout for the current form factor (shared across all). */
fun NavigationSettings.expansionFor(formFactor: FormFactor): SidebarExpansion =
    sidebarExpansion

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

// ============================================
// LEGACY MIGRATION HELPERS
// The previous model had three modes
// (Expanded / Compact / Bubble) and six bubble
// anchors. These map old values onto the new
// two-mode model.
// ============================================

/** Map a legacy persisted mode name onto the two-mode model. */
fun legacyModeToNavigationMode(name: String?): NavigationMode = when (name) {
    "FloatingIsland", "Docked", "Bubble" -> NavigationMode.Floating
    else -> NavigationMode.Sidebar
}

/** Map a legacy mode name onto the sidebar layout. */
fun legacyModeToSidebarExpansion(name: String?): SidebarExpansion = when (name) {
    "IconsOnly", "AutoHide", "Compact" -> SidebarExpansion.Compact
    else -> SidebarExpansion.Expanded
}

/** Map a legacy bubble anchor onto the new 12-point snap set. */
fun legacyAnchorToSnapPoint(anchor: String?): BubbleSnapPoint = when (anchor) {
    "Left" -> BubbleSnapPoint.LeftCenter
    "Right" -> BubbleSnapPoint.RightCenter
    "TopLeft" -> BubbleSnapPoint.TopLeft
    "TopRight" -> BubbleSnapPoint.TopRight
    "BottomLeft" -> BubbleSnapPoint.BottomLeft
    "BottomRight" -> BubbleSnapPoint.BottomRight
    else -> BubbleSnapPoint.BottomRight
}

// ============================================
// NAV GEOMETRY — pure layout math
// The docked navigation size, the adaptive
// sidebar width and the content reservation are
// derived from one model shared by the content
// padding, the bar surface and the published
// bottom-bar space — so they can never disagree
// and the layout can never produce negative
// geometry (the old padding crash). Pure and
// unit-tested; NavShell only renders these.
// ============================================

object NavGeometry {

    /** Expanded-sidebar width ratios by [SidebarSettings.expandedWidthIndex]. */
    val ExpandedWidthRatios = listOf(0.16f, 0.18f, 0.20f, 0.22f)

    /** Hard bounds for an expanded vertical sidebar, in dp. */
    val MinSidebarWidth = 208.dp
    val MaxSidebarWidth = 384.dp

    /**
     * Adaptive sidebar width: roughly 20% of the available window width so the
     * content always keeps ~80%, clamped to sensible bounds so the sidebar stays
     * usable on small windows and never becomes enormous on very wide ones. The
     * configured expanded-width preference picks the target ratio (0.16–0.22).
     */
    fun adaptiveSidebarWidth(
        availableWidth: Dp,
        settings: NavigationSettings
    ): Dp {
        val ratio = ExpandedWidthRatios.getOrElse(settings.sidebar.expandedWidthIndex) { 0.20f }
        return (availableWidth * ratio).coerceIn(MinSidebarWidth, MaxSidebarWidth)
    }

    /**
     * Size of the docked navigation region for the current sidebar layout and
     * form factor: width for a vertical sidebar, height for a horizontal bar.
     */
    fun dockedBarSize(
        settings: NavigationSettings,
        formFactor: FormFactor,
        expanded: Boolean,
        vertical: Boolean,
        containerWidthDp: Dp
    ): Dp {
        val size = when {
            vertical && expanded -> adaptiveSidebarWidth(containerWidthDp, settings)
            vertical -> NavTokens.CompactRailWidth
            // Phone bars use a fixed comfortable height for touch; expanded vs
            // compact differs only by whether labels are shown.
            formFactor.isPhone -> NavTokens.PhoneBarHeight
            expanded -> NavTokens.HorizontalBarHeight
            else -> NavTokens.HorizontalBarCompactHeight
        }
        // Hard safety at the layout boundary: the docked region can never
        // consume more than half the window in its docked dimension, so the
        // sidebar can never become the entire screen no matter what settings
        // or persisted state produced the value upstream.
        val maxDocked = if (vertical) containerWidthDp * 0.5f else containerWidthDp * 0.4f
        return size.coerceAtMost(maxDocked)
    }

    /**
     * Content reservation for the active mode: the docked region (plus system
     * inset) in Sidebar mode, zero in Floating mode where the bubble overlays
     * the whole surface. Always non-negative — the transition animates between
     * two valid geometries and can never produce invalid padding.
     */
    fun contentReserve(
        mode: NavigationMode,
        dockedSize: Dp,
        horizontalInset: Dp
    ): Dp = when (mode) {
        NavigationMode.Sidebar -> dockedSize + horizontalInset
        NavigationMode.Floating -> 0.dp
    }
}
