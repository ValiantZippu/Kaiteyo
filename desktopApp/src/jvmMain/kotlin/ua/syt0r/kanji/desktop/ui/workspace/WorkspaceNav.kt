package ua.syt0r.kanji.desktop.ui.workspace

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.CompactIconSize
import ua.syt0r.kanji.desktop.appstate.NavExpansion
import ua.syt0r.kanji.desktop.appstate.NavIconSize
import ua.syt0r.kanji.desktop.appstate.NavLabelMode
import ua.syt0r.kanji.desktop.appstate.NavLayout
import ua.syt0r.kanji.desktop.appstate.NavPosition
import ua.syt0r.kanji.desktop.appstate.NavSpacing
import ua.syt0r.kanji.desktop.appstate.WorkspaceView
import ua.syt0r.kanji.desktop.designsystem.DsBadge
import ua.syt0r.kanji.desktop.designsystem.DsElevation
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsMenuItem
import ua.syt0r.kanji.desktop.designsystem.DsMenuPanel
import ua.syt0r.kanji.desktop.designsystem.DsMotion
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.presentation.common.nav.LocalWindowResizing
import ua.syt0r.kanji.presentation.common.resources.brand.BrandMark
import kotlin.math.roundToInt

// ============================================
// KAITEYO NAVIGATION — adaptive workspace dock
// One component family for every edge:
//   * Left / Right  → vertical rail
//   * Top / Bottom  → horizontal bar
// Three predefined layout states only:
//   * Expanded (labels) · Compact (icons) · Hidden
// Width/height animate between states; there is
// NO free resizing and NO drag handle.
// Compact windows get a dedicated tab bar —
// desktop docks never shrink into it.
// ============================================

private data class NavItem(
    val view: WorkspaceView,
    val icon: ImageVector
)

// ============================================
// CURATED PRIMARY DESTINATIONS — the single
// navigation model shared by the dock, the
// compact tab bar and the floating launchpad.
// The Library is the learning hub: studying is
// an action performed from a deck, never a
// top-level destination. Primary set:
// Home · Library · Browse · Stats · Media ·
// Settings — Review (the active study session)
// stays reachable from decks and the "Study
// tools" overflow, not from the main dock.
// ============================================

private val primaryNavItems: List<NavItem> = listOf(
    // Home
    NavItem(WorkspaceView.Dashboard, Icons.Default.SpaceDashboard),
    // Library — the hub (collections → decks → study)
    NavItem(WorkspaceView.Library, Icons.Default.LibraryBooks),
    // Browse
    NavItem(WorkspaceView.Dictionary, Icons.Default.MenuBook),
    // Stats
    NavItem(WorkspaceView.Statistics, Icons.Default.BarChart),
    // Media
    NavItem(WorkspaceView.Media, Icons.Default.VideoLibrary),
    // Settings
    NavItem(WorkspaceView.Settings, Icons.Default.Settings)
)

/**
 * Secondary workspaces, grouped for the dock's "All views" overflow and the
 * launchpad's secondary section. The suite's power tools (exams, writing,
 * mining, OCR, integrations…) stay one click away — never orphaned. Review
 * lives here: starting a study session from a deck still opens it, but the
 * Review view is an action surface, not a navigation destination.
 */
private val secondaryNavGroups: List<Pair<String, List<NavItem>>> = listOf(
    "Study tools" to listOf(
        NavItem(WorkspaceView.Review, Icons.Default.PlayArrow),
        NavItem(WorkspaceView.Exams, Icons.Default.School),
        NavItem(WorkspaceView.Writing, Icons.Default.Create),
        NavItem(WorkspaceView.Grammar, Icons.Default.Lightbulb),
        NavItem(WorkspaceView.Collections, Icons.Default.Bookmarks)
    ),
    // The game world — an optional second space inside Kaiteyo.
    "World" to listOf(
        NavItem(WorkspaceView.Game, Icons.Default.VideogameAsset)
    ),
    "Materials" to listOf(
        NavItem(WorkspaceView.LearningBrowser, Icons.Default.TextSnippet),
        NavItem(WorkspaceView.Ocr, Icons.Default.Camera),
        NavItem(WorkspaceView.Mining, Icons.Default.Usb)
    ),
    "Organize" to listOf(
        NavItem(WorkspaceView.Tags, Icons.Default.Sell),
        NavItem(WorkspaceView.Mistakes, Icons.Default.Warning),
        NavItem(WorkspaceView.History, Icons.Default.History)
    ),
    "System" to listOf(
        NavItem(WorkspaceView.Integrations, Icons.Default.Tune),
        NavItem(WorkspaceView.Transfer, Icons.Default.ImportExport),
        NavItem(WorkspaceView.Sync, Icons.Default.Sync),
        NavItem(WorkspaceView.Shortcuts, Icons.Default.Keyboard),
        NavItem(WorkspaceView.Plugins, Icons.Default.Extension),
        NavItem(WorkspaceView.Account, Icons.Default.Person),
        NavItem(WorkspaceView.Contributions, Icons.Default.Favorite)
    )
)

/** All nav items flattened (primary + secondary) for the palette and icon lookups. */
val allNavItems: List<Pair<WorkspaceView, ImageVector>> =
    (primaryNavItems + secondaryNavGroups.flatMap { it.second }).map { it.view to it.icon }

/** All secondary views, flattened for the dock "All views" overflow. */
private val secondaryNavItems: List<Pair<WorkspaceView, ImageVector>> =
    secondaryNavGroups.flatMap { (_, items) -> items.map { it.view to it.icon } }

/**
 * Launchpad groups: the curated primary set first, then every secondary
 * workspace — the launchpad stays the comprehensive launcher while the
 * primary destinations lead it, exactly like the dock.
 */
val navGroupsForLaunchpad: List<Pair<String, List<Pair<WorkspaceView, ImageVector>>>> =
    listOf("Primary" to primaryNavItems.map { it.view to it.icon }) +
        secondaryNavGroups.map { (label, items) -> label to items.map { it.view to it.icon } }

/** The views surfaced as primary tabs in compact windows (Home · Library · Browse · Stats). */
private val compactPrimaryViews = listOf(
    WorkspaceView.Dashboard,
    WorkspaceView.Library,
    WorkspaceView.Dictionary,
    WorkspaceView.Statistics
)

/**
 * Dock-item press behavior: a plain click navigates the active tab, while
 * middle-click or Ctrl+click opens the destination in a new workspace tab
 * (browser-style multi-instance). Consumption keeps the two exclusive.
 */
private fun Modifier.dockItemGesture(state: AppState, view: WorkspaceView): Modifier = this
    .pointerInput(view) {
        awaitEachGesture {
            // Buttons live on the event in Compose 1.8, not the change.
            val firstEvent = awaitPointerEvent()
            val down = firstEvent.changes.firstOrNull { it.pressed } ?: return@awaitEachGesture
            if (firstEvent.buttons.isTertiaryPressed || currentEvent.keyboardModifiers.isCtrlPressed) {
                down.consume()
                state.openTab(view, activate = true)
                return@awaitEachGesture
            }
            var dragged = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                when (event.type) {
                    PointerEventType.Move -> {
                        if (!dragged &&
                            (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                        ) {
                            dragged = true
                        }
                    }
                    PointerEventType.Release -> {
                        if (!dragged) state.currentView = view
                        return@awaitEachGesture
                    }
                    else -> {}
                }
            }
        }
    }

@Composable
private fun navBadge(state: AppState, view: WorkspaceView): String? = when (view) {
    // The Library is the hub — its badge is the total due workload across
    // every deck, so the dock still answers "what needs studying today?".
    WorkspaceView.Library -> state.dueCount().takeIf { it > 0 }?.toString()
    else -> null
}

// ============================================
// LOGO MARK
// ============================================

@Composable
private fun DsLogoMark(modifier: Modifier = Modifier) {
    // The real Kaiteyo mark — resolved through the centralized brand asset
    // API instead of a letterform placeholder.
    BrandMark(modifier = modifier.size(30.dp), contentDescription = null)
}

/** Motion duration for dock size changes, honoring the animation toggle + speed + global reduced motion. */
private fun dockDurationMs(state: AppState): Int =
    if (!state.navigationAnimations || state.navReducedMotion) 0
    else (DsMotion.Normal * state.navigationAnimationSpeed).toInt()

/** Dock icon size from the Sidebar setting, bumped further by the accessibility toggle. */
@Composable
private fun navIconSize(state: AppState): androidx.compose.ui.unit.Dp {
    val base = when (state.navIconSize) {
        NavIconSize.Small -> 16.dp
        NavIconSize.Medium -> 18.dp
        NavIconSize.Large -> 22.dp
    }
    return base + if (state.navigationLargerIcons) 3.dp else 0.dp
}

/** Vertical rhythm between dock items, honoring the compact-spacing setting. */
private fun navItemSpacing(state: AppState): Dp = when (state.navSpacing) {
    NavSpacing.Tight -> 2.dp
    NavSpacing.Comfortable -> 8.dp
    NavSpacing.Spacious -> 14.dp
}

/** Gap inside the mode switcher, honoring the compact-spacing setting. */
private fun navSwitchSpacing(state: AppState): Dp = when (state.navSpacing) {
    NavSpacing.Tight -> 4.dp
    NavSpacing.Comfortable -> 8.dp
    NavSpacing.Spacious -> 12.dp
}

/** Whether item labels render in the expanded dock, per the label-visibility setting. */
private fun showNavLabel(state: AppState, expanded: Boolean, hovered: Boolean): Boolean =
    expanded && when (state.navLabelMode) {
        NavLabelMode.Always -> true
        NavLabelMode.OnHover -> hovered
        NavLabelMode.Hidden -> false
    }

// ============================================
// NAV TOOLTIP (compact icon-only mode)
// ============================================

private enum class TooltipPlacement { Left, Right, Above, Below }

@Composable
private fun NavTooltipHost(
    label: String,
    placement: TooltipPlacement,
    enabled: Boolean = true,
    delayMs: Int = 450,
    content: @Composable () -> Unit
) {
    val sc = surfaceColors()
    val density = LocalDensity.current
    var anchor by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var tipVisible by remember { mutableStateOf(false) }

    LaunchedEffect(hovered, enabled, delayMs) {
        if (hovered && enabled && delayMs > 0) {
            delay(delayMs.toLong())
            tipVisible = true
        } else {
            tipVisible = false
        }
    }

    Box(
        modifier = Modifier
            .onGloballyPositioned { if (anchor != it) anchor = it }
            .hoverable(interaction)
    ) {
        content()
    }

    val coords = anchor
    if (enabled && tipVisible && coords != null) {
        val pos = coords.positionInWindow()
        val estW = with(density) { (label.length * 7 + 24).dp.toPx() }
        val estH = with(density) { 30.dp.toPx() }
        val offset = when (placement) {
            TooltipPlacement.Right -> IntOffset(
                pos.x.roundToInt() + coords.size.width + 8,
                pos.y.roundToInt() + coords.size.height / 2 - (estH / 2).roundToInt()
            )
            TooltipPlacement.Left -> IntOffset(
                pos.x.roundToInt() - estW.roundToInt() - 8,
                pos.y.roundToInt() + coords.size.height / 2 - (estH / 2).roundToInt()
            )
            TooltipPlacement.Below -> IntOffset(
                pos.x.roundToInt() + coords.size.width / 2 - (estW / 2).roundToInt(),
                pos.y.roundToInt() + coords.size.height + 8
            )
            TooltipPlacement.Above -> IntOffset(
                pos.x.roundToInt() + coords.size.width / 2 - (estW / 2).roundToInt(),
                pos.y.roundToInt() - estH.roundToInt() - 8
            )
        }
        Popup(offset = offset, properties = PopupProperties(focusable = false)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(DsRadius.Sm))
                    .background(sc.surfaceInteractive)
                    .border(1.dp, sc.border.copy(alpha = 0.4f), RoundedCornerShape(DsRadius.Sm))
                    .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Xs)
            ) {
                Text(
                    text = label,
                    color = sc.textPrimary,
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ============================================
// VERTICAL RAIL (left / right edge)
// ============================================

@Composable
fun DsNavRail(
    state: AppState,
    onOpenPalette: () -> Unit
) {
    val sc = surfaceColors()
    val expanded = state.navExpansion == NavExpansion.Expanded
    val itemSpacing = navItemSpacing(state)

    // The rail reads the window width so the expanded size stays within
    // predefined widths and is capped on narrow (tablet) windows. While the
    // window is being resized the width follows instantly — animating toward
    // a target that changes every frame only adds lag and jitter.
    BoxWithConstraints(Modifier.fillMaxHeight()) {
        val resizing = LocalWindowResizing.current
        val railWidth by animateDpAsState(
            targetValue = if (expanded) state.effectiveExpandedWidth(maxWidth.value) else 64.dp,
            animationSpec = tween(if (resizing) 0 else dockDurationMs(state), easing = FastOutSlowInEasing),
            label = "navRailWidth"
        )

        // Floating island: rounded, elevated and softly bordered so the
        // dock reads as a surface floating above the window background
        // (the shell wraps it in an 8dp ring — see DsDockIsland).
        val dockShape = RoundedCornerShape(DsRadius.Lg)
        Column(
            modifier = Modifier
                .width(railWidth)
                .fillMaxHeight()
                .shadow(
                    elevation = DsElevation.Floating,
                    shape = dockShape,
                    ambientColor = accent().primary.copy(alpha = 0.22f),
                    spotColor = accent().primary.copy(alpha = 0.22f)
                )
                .clip(dockShape)
                .background(sc.surfaceElevated)
                .border(
                    1.dp,
                    sc.border.copy(alpha = if (state.navHighContrast) 0.8f else 0.3f),
                    dockShape
                )
                .padding(vertical = DsSpacing.Lg)
        ) {
        // Exactly three mode buttons at the top of the sidebar.
        DsNavModeSwitcher(
            state,
            vertical = !expanded,
            modifier = if (expanded) Modifier.fillMaxWidth().padding(horizontal = DsSpacing.Md) else Modifier
        )
        Spacer(Modifier.height(DsSpacing.Md))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DsSpacing.Md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DsLogoMark()
            if (expanded) {
                Spacer(Modifier.width(DsSpacing.Sm))
                Column(Modifier.weight(1f)) {
                    Text("Kaiteyo", color = sc.textPrimary, fontSize = DsType.Title, fontWeight = FontWeight.Bold)
                    Text("Desktop", color = accent().primary, fontSize = DsType.Caption, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(DsSpacing.Lg))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            // The curated primary destinations — Home · Library · Browse ·
            // Stats · Media · Settings — with the Library as the learning hub
            // (single section, no headers). Study is an action from a deck.
            primaryNavItems.forEach { item -> DsNavItem(item, state, expanded) }
            Spacer(Modifier.height(DsSpacing.Xs))
            // Every other workspace behind one overflow entry — the dock
            // stays a curated primary navigation and nothing is orphaned.
            DsMoreViewsButton(state, expanded, vertical = true)
        }

        Spacer(Modifier.height(DsSpacing.Md))

        if (expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DsSpacing.Md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = state.dueCount().toString(),
                        color = sc.textPrimary,
                        fontSize = DsType.BodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text("cards due", color = sc.textMuted, fontSize = DsType.Caption)
                }
                DsNavPositionButton(state)
                // Window controls (Restore/Minimize/Maximize/Close) reachable
                // without the title bar.
                DsWindowMenuButton()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
            ) {
                DsNavPositionButton(state)
                DsIconButton(
                    icon = Icons.Default.MenuOpen,
                    onClick = { state.updateNavExpansion(NavExpansion.Expanded) },
                    contentDescription = "Expand navigation"
                )
                DsWindowMenuButton()
            }
        }
        }
    }
}

@Composable
private fun DsNavItem(item: NavItem, state: AppState, expanded: Boolean) {
    val sc = surfaceColors()
    val ac = accent()
    val selected = state.currentView == item.view
    val badge = navBadge(state, item.view)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg = when {
        selected -> ac.primary.copy(alpha = 0.16f)
        hovered -> sc.surfaceInteractive.copy(alpha = 0.6f)
        else -> Color.Transparent
    }
    val showLabel = showNavLabel(state, expanded, hovered)

    NavTooltipHost(
        label = item.view.label,
        enabled = !expanded,
        placement = if (state.navPosition == NavPosition.Right) TooltipPlacement.Left else TooltipPlacement.Right,
        delayMs = state.navigationTooltipDelayMs
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DsRadius.Md))
                .background(bg)
                .dockItemGesture(state, item.view)
                .hoverable(interaction)
                .padding(horizontal = if (expanded) DsSpacing.Md else 0.dp, vertical = DsSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
        ) {
            if (expanded) {
                Spacer(
                    Modifier
                        .width(3.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (selected) ac.primary else Color.Transparent)
                )
                Spacer(Modifier.width(DsSpacing.Sm))
            }
            Icon(
                item.icon,
                contentDescription = item.view.label,
                tint = if (selected) ac.primary else sc.textSecondary,
                modifier = Modifier.size(navIconSize(state))
            )
            if (showLabel) {
                Spacer(Modifier.width(DsSpacing.Md))
                Text(
                    text = item.view.label,
                    color = if (selected) ac.primary else sc.textSecondary,
                    fontSize = DsType.Body,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (badge != null) {
                    DsBadge(text = badge, tint = if (selected) ac.primary else sc.textMuted)
                }
            }
        }
    }
}

// ============================================
// HORIZONTAL BAR (top / bottom edge)
// ============================================

@Composable
fun DsNavBar(
    state: AppState,
    onOpenPalette: () -> Unit
) {
    val sc = surfaceColors()
    val expanded = state.navExpansion == NavExpansion.Expanded
    val resizing = LocalWindowResizing.current
    val barHeight by animateDpAsState(
        targetValue = if (expanded) 64.dp else 52.dp,
        animationSpec = tween(if (resizing) 0 else dockDurationMs(state), easing = FastOutSlowInEasing),
        label = "navBarHeight"
    )

    // Floating island — same treatment as the rail (see DsDockIsland).
    val dockShape = RoundedCornerShape(DsRadius.Lg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
            .shadow(
                elevation = DsElevation.Floating,
                shape = dockShape,
                ambientColor = accent().primary.copy(alpha = 0.22f),
                spotColor = accent().primary.copy(alpha = 0.22f)
            )
            .clip(dockShape)
            .background(sc.surfaceElevated)
            .border(
                1.dp,
                sc.border.copy(alpha = if (state.navHighContrast) 0.8f else 0.3f),
                dockShape
            )
            .padding(horizontal = DsSpacing.Md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Exactly three mode buttons at the start of the bar.
        DsNavModeSwitcher(state, vertical = false, modifier = Modifier.padding(end = DsSpacing.Lg))
        DsLogoMark()
        if (expanded) {
            Spacer(Modifier.width(DsSpacing.Sm))
            Text("Kaiteyo", color = sc.textPrimary, fontSize = DsType.Title, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(DsSpacing.Lg))
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The curated primary destinations first, then the overflow for
            // every secondary workspace.
            primaryNavItems.forEach { item -> DsNavPill(item, state, expanded) }
            NavGroupSeparator()
            DsMoreViewsButton(state, expanded, vertical = false)
        }
        Spacer(Modifier.width(DsSpacing.Sm))
        DsNavPositionButton(state)
    }
}

@Composable
private fun DsNavPill(item: NavItem, state: AppState, expanded: Boolean) {
    val sc = surfaceColors()
    val ac = accent()
    val selected = state.currentView == item.view
    val badge = navBadge(state, item.view)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg = when {
        selected -> ac.primary.copy(alpha = 0.16f)
        hovered -> sc.surfaceInteractive.copy(alpha = 0.6f)
        else -> Color.Transparent
    }
    val showLabel = showNavLabel(state, expanded, hovered)

    NavTooltipHost(
        label = item.view.label,
        enabled = !expanded,
        placement = if (state.navPosition == NavPosition.Bottom) TooltipPlacement.Above else TooltipPlacement.Below,
        delayMs = state.navigationTooltipDelayMs
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(DsRadius.Md))
                .background(bg)
                .dockItemGesture(state, item.view)
                .hoverable(interaction)
                .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
        ) {
            Icon(
                item.icon,
                contentDescription = item.view.label,
                tint = if (selected) ac.primary else sc.textSecondary,
                modifier = Modifier.size(navIconSize(state))
            )
            if (showLabel) {
                Text(
                    text = item.view.label,
                    color = if (selected) ac.primary else sc.textSecondary,
                    fontSize = DsType.Body,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            if (badge != null && showLabel) {
                DsBadge(text = badge, tint = if (selected) ac.primary else sc.textMuted)
            }
        }
    }
}

@Composable
private fun NavGroupSeparator() {
    val sc = surfaceColors()
    Box(
        Modifier
            .padding(horizontal = DsSpacing.Sm)
            .width(1.dp)
            .height(20.dp)
            .background(sc.border.copy(alpha = 0.5f))
    )
}

// ============================================
// ALL VIEWS OVERFLOW
// Every secondary workspace sits behind one
// "More" entry, so the dock stays a curated
// primary navigation (Home · Library · Browse ·
// Stats · Media · Settings) without orphaning
// anything. Same grouped menu used by the compact
// tab bar, with full keyboard access.
// ============================================

@Composable
private fun DsMoreViewsButton(
    state: AppState,
    expanded: Boolean,
    vertical: Boolean,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    var open by remember { mutableStateOf(false) }
    var anchor by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg = if (hovered) sc.surfaceInteractive.copy(alpha = 0.6f) else Color.Transparent

    NavTooltipHost(
        label = "All views",
        enabled = !expanded,
        placement = when {
            vertical -> if (state.navPosition == NavPosition.Right) TooltipPlacement.Left else TooltipPlacement.Right
            state.navPosition == NavPosition.Bottom -> TooltipPlacement.Above
            else -> TooltipPlacement.Below
        },
        delayMs = state.navigationTooltipDelayMs
    ) {
        Row(
            modifier = modifier
                .then(if (vertical) Modifier.fillMaxWidth() else Modifier)
                .onGloballyPositioned { if (anchor != it) anchor = it }
                .clip(RoundedCornerShape(DsRadius.Md))
                .background(bg)
                .hoverable(interaction)
                .clickable(interactionSource = interaction, indication = null) { open = true }
                .padding(horizontal = if (expanded) DsSpacing.Md else 0.dp, vertical = DsSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "All views",
                tint = if (hovered) sc.textPrimary else sc.textSecondary,
                modifier = Modifier.size(navIconSize(state))
            )
            if (expanded) {
                Spacer(Modifier.width(DsSpacing.Md))
                Text(
                    text = "More",
                    color = sc.textSecondary,
                    fontSize = DsType.Body
                )
            }
        }
    }

    val coords = anchor
    if (open && coords != null) {
        val pos = coords.positionInWindow()
        val density = LocalDensity.current
        val windowSize = LocalWindowInfo.current.containerSize
        val menuW = with(density) { 244.dp.toPx() }
        val menuH = with(density) { minOf(430f, secondaryNavItems.size * 36f + 16f).dp.toPx() }
        // Open toward the window interior and flip upward when the menu would
        // clip below the window.
        val openUp = state.navPosition == NavPosition.Bottom ||
            pos.y + coords.size.height + menuH > windowSize.height
        val openLeft = state.navPosition == NavPosition.Right
        val popupX = when {
            openLeft -> pos.x - menuW - 8
            vertical -> pos.x + coords.size.width + 8
            else -> pos.x
        }
        val popupY = if (openUp) pos.y - menuH else pos.y + coords.size.height
        Popup(
            onDismissRequest = { open = false },
            offset = IntOffset(popupX.roundToInt(), popupY.roundToInt()),
            properties = PopupProperties(focusable = true)
        ) {
            Column(
                Modifier
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                DsMenuPanel(
                    menuItems = secondaryNavItems
                        .map { (view, icon) ->
                            DsMenuItem(
                                label = view.label,
                                icon = icon,
                                checked = state.currentView == view,
                                onAction = {
                                    state.currentView = view
                                    open = false
                                }
                            )
                        },
                    onDismiss = { open = false }
                )
            }
        }
    }
}

// ============================================
// COMPACT TAB BAR (compact windows only)
// A real compact navigation: icon + label tabs
// plus an overflow menu. Top or bottom edge.
// ============================================

@Composable
fun DsCompactNavBar(state: AppState) {
    val sc = surfaceColors()
    val ac = accent()
    val compactIcon = when (state.compactIconSize) {
        CompactIconSize.Small -> 16.dp
        CompactIconSize.Medium -> 20.dp
        CompactIconSize.Large -> 24.dp
    } + if (state.navigationLargerIcons) 3.dp else 0.dp

    // Floating island — same treatment as the desktop dock (see DsDockIsland).
    val dockShape = RoundedCornerShape(DsRadius.Lg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .shadow(
                elevation = DsElevation.Floating,
                shape = dockShape,
                ambientColor = accent().primary.copy(alpha = 0.22f),
                spotColor = accent().primary.copy(alpha = 0.22f)
            )
            .clip(dockShape)
            .background(sc.surfaceElevated)
            .border(
                1.dp,
                sc.border.copy(alpha = if (state.navHighContrast) 0.8f else 0.3f),
                dockShape
            )
            .padding(horizontal = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        compactPrimaryViews.forEach { view ->
            val selected = state.currentView == view
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(DsRadius.Md))
                    .dockItemGesture(state, view)
                    .padding(vertical = DsSpacing.Xs),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(Modifier.height(2.dp)) {
                    if (selected) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(ac.primary)
                        )
                    }
                }
                Icon(
                    imageVector = iconForView(view),
                    contentDescription = view.label,
                    tint = if (selected) ac.primary else sc.textSecondary,
                    modifier = Modifier.size(compactIcon)
                )
                Text(
                    text = view.label,
                    color = if (selected) ac.primary else sc.textSecondary,
                    fontSize = DsType.Caption,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
        var overflowOpen by remember { mutableStateOf(false) }
        var overflowAnchor by remember { mutableStateOf<LayoutCoordinates?>(null) }
        // Compact tab-bar position toggle — moves the bar to the opposite
        // edge. Only Top/Bottom are valid in compact windows (AppState
        // coerces anything else), and only reachable in the suite's compact
        // layout, where this bar is visible.
        DsIconButton(
            icon = if (state.compactNavPosition == NavPosition.Top) {
                Icons.Default.KeyboardArrowDown
            } else {
                Icons.Default.KeyboardArrowUp
            },
            onClick = {
                state.updateCompactNavPosition(
                    if (state.compactNavPosition == NavPosition.Top) NavPosition.Bottom
                    else NavPosition.Top
                )
            },
            contentDescription = if (state.compactNavPosition == NavPosition.Top) {
                "Move tab bar to bottom"
            } else {
                "Move tab bar to top"
            },
            size = 32.dp
        )
        Box(
            modifier = Modifier
                .onGloballyPositioned { if (overflowAnchor != it) overflowAnchor = it }
                .padding(2.dp)
        ) {
            DsIconButton(
                icon = Icons.Default.MoreVert,
                onClick = { overflowOpen = true },
                contentDescription = "More views"
            )
        }
        val coords = overflowAnchor
        if (overflowOpen && coords != null) {
            val pos = coords.positionInWindow()
            val density = LocalDensity.current
            val overflowItems = allNavItems.filterNot { (view, _) -> view in compactPrimaryViews }
            // A bottom tab bar opens the overflow upward so it never clips off-screen.
            val openUp = state.compactNavPosition == NavPosition.Bottom
            val menuH = with(density) { minOf(440f, overflowItems.size * 36f + 16f).dp.toPx() }
            Popup(
                onDismissRequest = { overflowOpen = false },
                offset = IntOffset(
                    pos.x.roundToInt(),
                    if (openUp) (pos.y - menuH).roundToInt() else pos.y.roundToInt() + coords.size.height
                ),
                properties = PopupProperties(focusable = true)
            ) {
                Column(
                    Modifier
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    DsMenuPanel(
                        menuItems = overflowItems
                            .map { (view, icon) ->
                                DsMenuItem(
                                    label = view.label,
                                    icon = icon,
                                    checked = state.currentView == view,
                                    onAction = { state.currentView = view }
                                )
                            },
                        onDismiss = { overflowOpen = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun iconForView(view: WorkspaceView): ImageVector =
    allNavItems.firstOrNull { it.first == view }?.second ?: Icons.Default.Apps

// ============================================
// NAV MODE SWITCHER (top of the sidebar)
// Two modes — Sidebar and Floating — plus the
// sidebar layout toggle (Expanded ↔ Compact).
// ============================================

@Composable
private fun DsNavModeSwitcher(state: AppState, vertical: Boolean, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    val ac = accent()
    val expanded = state.navExpansion == NavExpansion.Expanded
    val spacing = navSwitchSpacing(state)
    val modes = listOf(
        NavLayout.Sidebar to Icons.Default.ViewSidebar,
        NavLayout.Floating to Icons.Default.ChatBubble
    )

    val modeButtons: @Composable () -> Unit = {
        modes.forEach { (mode, icon) ->
            val selected = state.navLayout == mode
            val interaction = remember { MutableInteractionSource() }
            val hovered by interaction.collectIsHoveredAsState()
            Box(
                modifier = Modifier
                    .size(if (vertical) 40.dp else 36.dp)
                    .clip(RoundedCornerShape(DsRadius.Md))
                    .background(
                        when {
                            selected -> ac.primary.copy(alpha = if (state.navHighContrast) 0.28f else 0.18f)
                            hovered -> sc.surfaceInteractive
                            else -> Color.Transparent
                        }
                    )
                    .clickable(interactionSource = interaction, indication = null) {
                        if (!selected) state.updateNavLayout(mode)
                    }
                    .hoverable(interaction),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = mode.label,
                    tint = if (selected) ac.primary else sc.textSecondary,
                    modifier = Modifier.size(navIconSize(state))
                )
            }
        }
    }

    // Expanded ↔ Compact switch for the sidebar layout.
    val expansionToggle: @Composable () -> Unit = {
        val interaction = remember { MutableInteractionSource() }
        val hovered by interaction.collectIsHoveredAsState()
        Box(
            modifier = Modifier
                .size(if (vertical) 40.dp else 36.dp)
                .clip(RoundedCornerShape(DsRadius.Md))
                .background(if (hovered) sc.surfaceInteractive else Color.Transparent)
                .clickable(interactionSource = interaction, indication = null) {
                    state.updateNavExpansion(
                        if (expanded) NavExpansion.Compact else NavExpansion.Expanded
                    )
                }
                .hoverable(interaction),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ViewSidebar,
                contentDescription = if (expanded) "Collapse sidebar" else "Expand sidebar",
                tint = if (expanded) ac.primary else sc.textSecondary,
                modifier = Modifier.size(navIconSize(state))
            )
        }
    }

    if (vertical) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            modeButtons()
            expansionToggle()
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            modeButtons()
            Box(
                Modifier
                    .padding(horizontal = DsSpacing.Sm)
                    .width(1.dp)
                    .height(20.dp)
                    .background(sc.border.copy(alpha = 0.5f))
            )
            expansionToggle()
        }
    }
}

// ============================================
// NAV POSITION POPUP (dock edge picker)
// A single clean popup with icons for the four
// dock edges. Mode switching lives in the
// three-button switcher at the top of the dock.
// ============================================

@Composable
private fun DsNavPositionButton(state: AppState) {
    var open by remember { mutableStateOf(false) }
    var anchor by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = Modifier
            .padding(2.dp)
            .onGloballyPositioned { if (anchor != it) anchor = it }
    ) {
        DsIconButton(
            icon = Icons.Default.Tune,
            onClick = { open = true },
            contentDescription = "Sidebar position",
            tint = accent().primary
        )
    }

    val coords = anchor
    if (open && coords != null) {
        val windowPos = coords.positionInWindow()
        val density = LocalDensity.current
        val menuW = with(density) { 196.dp.toPx() }
        val menuH = with(density) { 210.dp.toPx() }
        // The button hugs the dock edge — open the popup toward the
        // window interior so a bottom or right dock never clips it off-screen.
        val openUp = state.navPosition != NavPosition.Top
        val openLeft = state.navPosition == NavPosition.Right
        val popupX = if (openLeft) windowPos.x + coords.size.width - menuW else windowPos.x
        val popupY = if (openUp) windowPos.y - menuH else windowPos.y + coords.size.height
        Popup(
            onDismissRequest = { open = false },
            offset = IntOffset(popupX.roundToInt(), popupY.roundToInt()),
            properties = PopupProperties(focusable = true)
        ) {
            // Smooth expand: the picker scales out from the button with a fade.
            var shown by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { shown = true }
            val scale by animateFloatAsState(
                targetValue = if (shown) 1f else 0.82f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                label = "positionScale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (shown) 1f else 0f,
                animationSpec = tween(150),
                label = "positionAlpha"
            )
            val sc = surfaceColors()
            Column(
                modifier = Modifier
                    .width(196.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .shadow(18.dp, RoundedCornerShape(DsRadius.Md))
                    .clip(RoundedCornerShape(DsRadius.Md))
                    .background(sc.surfaceInteractive)
                    .padding(DsSpacing.Sm),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
            ) {
                Text(
                    text = "Position",
                    color = sc.textMuted,
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = DsSpacing.Xs)
                )
                // Visual 2×2 icon grid with a mini window preview on each
                // option — one-click switching, no text list.
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    PositionGridButton(state, NavPosition.Left, Icons.Default.KeyboardArrowLeft, Modifier.weight(1f)) { open = false }
                    PositionGridButton(state, NavPosition.Right, Icons.Default.KeyboardArrowRight, Modifier.weight(1f)) { open = false }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)) {
                    PositionGridButton(state, NavPosition.Top, Icons.Default.KeyboardArrowUp, Modifier.weight(1f)) { open = false }
                    PositionGridButton(state, NavPosition.Bottom, Icons.Default.KeyboardArrowDown, Modifier.weight(1f)) { open = false }
                }
                // Miniature window with the dock drawn on the selected edge.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = DsSpacing.Xs),
                    contentAlignment = Alignment.Center
                ) {
                    MiniPositionPreview(state)
                }
            }
        }
    }
}

/** Tiny window mock with the dock drawn on the current edge. */
@Composable
private fun MiniPositionPreview(state: AppState) {
    val sc = surfaceColors()
    val ac = accent()
    val edge = state.navPosition
    val barColor = ac.primary.copy(alpha = 0.7f)
    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(sc.surface.copy(alpha = 0.6f))
            .border(1.dp, sc.border.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
    ) {
        when (edge) {
            NavPosition.Left -> Box(
                Modifier.align(Alignment.CenterStart).fillMaxHeight().width(12.dp).background(barColor)
            )
            NavPosition.Right -> Box(
                Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(12.dp).background(barColor)
            )
            NavPosition.Top -> Box(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().height(10.dp).background(barColor)
            )
            NavPosition.Bottom -> Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(10.dp).background(barColor)
            )
        }
    }
}

@Composable
private fun PositionGridButton(
    state: AppState,
    position: NavPosition,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onPicked: () -> Unit = {}
) {
    val sc = surfaceColors()
    val ac = accent()
    val selected = state.navPosition == position
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(
                when {
                    selected -> ac.primary.copy(alpha = 0.2f)
                    hovered -> sc.surfaceInteractive.copy(alpha = 0.8f)
                    else -> Color.Transparent
                }
            )
            .clickable(interactionSource = interaction, indication = null) {
                state.updateNavPosition(position)
                onPicked()
            }
            .hoverable(interaction),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = position.label,
            tint = if (selected) ac.primary else sc.textSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}
