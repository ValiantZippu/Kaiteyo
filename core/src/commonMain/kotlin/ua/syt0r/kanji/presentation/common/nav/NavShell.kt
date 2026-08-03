package ua.syt0r.kanji.presentation.common.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow as materialShadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.runBlocking
import org.koin.compose.koinInject
import ua.syt0r.kanji.PlatformFeature
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.core.user_data.preferences.PreferencesDefaultHomeTab
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LayoutConfig
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoThemeState
import ua.syt0r.kanji.presentation.common.theme.LocalRadiusConfig
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.NavAutoHide
import ua.syt0r.kanji.presentation.common.theme.SidebarMode
import ua.syt0r.kanji.presentation.common.theme.SidebarPosition
import ua.syt0r.kanji.presentation.common.ui.LocalOrientation
import ua.syt0r.kanji.presentation.common.ui.Orientation
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.HomeNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.HomeScreenTab
import ua.syt0r.kanji.presentation.screen.main.screen.home.rememberHomeNavigationState
import kotlin.math.roundToInt

// ============================================
// NAVIGATION MODEL
// ============================================

class NavEntry(
    val id: String,
    val label: @Composable () -> String,
    val icon: ImageVector?,
    val iconContent: (@Composable () -> Unit)? = null,
    val selected: Boolean,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

class NavSection(
    val title: (@Composable () -> String)?,
    val entries: List<NavEntry>
)

// ============================================
// NAVIGATION TOKENS — centralized design tokens
// ============================================

object NavTokens {
    val StripElevation = 6.dp
    val FloatingElevation = 24.dp
    val DockElevation = 16.dp
    val StripRadius = Dimens.RadiusLg
    val FloatingRadius = Dimens.Radius2xl
    val DockRadius = Dimens.RadiusXl
    val EdgeMargin = 8.dp
    val StripPadding = 8.dp
    val ItemHeight = 40.dp
    val ItemIconSize = 20.dp
    val CompactStripWidth = Dimens.SidebarCompactWidth
    val CompactItemSize = 36.dp
    val DockItemSize = 42.dp
    val ResizeStripWidth = 5.dp
    val CollapseButtonSize = 22.dp
}

// ============================================
// COMPOSITION LOCALS
// ============================================

val LocalHomeNavigationState = compositionLocalOf<HomeNavigationState?> { null }

enum class DesktopWindowPlacement { Floating, Maximized }

val LocalWindowPlacement = compositionLocalOf<DesktopWindowPlacement?> { null }

// ============================================
// NAV SHELL — unified desktop navigation system
// ============================================

@Composable
fun NavShell(
    navigationState: MainNavigationState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {

    if (LocalOrientation.current == Orientation.Portrait) {
        Box(modifier) { content() }
        return
    }

    val appPreferences = koinInject<PreferencesContract.AppPreferences>()
    val navManager = rememberNavLayoutManager(appPreferences)
    val themeState = LocalKaiteyoThemeState.current
    val defaultTab = remember { defaultHomeTab(appPreferences) }
    val homeNavState = rememberHomeNavigationState(defaultTab)

    var revealed by remember { mutableStateOf(true) }

    // Load persisted layout into the theme state
    LaunchedEffect(
        navManager.sidebarMode.value, navManager.sidebarPosition.value,
        navManager.autoHide.value, navManager.collapsed.value,
        navManager.panelWidth.value, navManager.panelHeight.value,
        navManager.floatingOffset.value, navManager.accentIndex.value
    ) {
        themeState.layoutConfig = themeState.layoutConfig.copy(
            sidebarMode = navManager.sidebarMode.value,
            sidebarPosition = navManager.sidebarPosition.value,
            autoHide = navManager.autoHide.value,
            collapsed = navManager.collapsed.value,
            panelWidth = navManager.panelWidth.value,
            panelHeight = navManager.panelHeight.value,
            floatingOffset = navManager.floatingOffset.value,
            accentIndex = navManager.accentIndex.value
        )
    }

    // Persist every layout change (Appearance Studio, drag, resize, ...)
    LaunchedEffect(themeState.layoutConfig) {
        navManager.syncFrom(themeState.layoutConfig)
    }

    CompositionLocalProvider(LocalHomeNavigationState provides homeNavState) {
        val layout = themeState.layoutConfig
        val isOverlay = layout.sidebarMode == SidebarMode.FloatingIsland ||
            layout.sidebarMode == SidebarMode.Docked

        val shouldHide = !isOverlay && when {
            layout.sidebarMode == SidebarMode.AutoHide -> !revealed
            layout.autoHide == NavAutoHide.Never -> false
            layout.autoHide == NavAutoHide.Always -> !revealed
            layout.autoHide == NavAutoHide.FullscreenOnly ->
                LocalWindowPlacement.current == DesktopWindowPlacement.Maximized && !revealed
            else -> !revealed // Smart
        }

        val vertical = layout.sidebarPosition == SidebarPosition.Left ||
            layout.sidebarPosition == SidebarPosition.Right
        val stripSize = if (vertical) layout.panelWidth else layout.panelHeight
        val animatedStripSize by animateDpAsState(
            targetValue = if (shouldHide) 0.dp else stripSize,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 260f),
            label = "navStripSize"
        )

        Box(
            modifier = modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        event.isCtrlPressed && event.key == Key.B
                    ) {
                        revealed = !revealed
                        true
                    } else {
                        false
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .let { base ->
                        when {
                            isOverlay || shouldHide -> base
                            layout.sidebarPosition == SidebarPosition.Left -> base.padding(start = animatedStripSize)
                            layout.sidebarPosition == SidebarPosition.Right -> base.padding(end = animatedStripSize)
                            layout.sidebarPosition == SidebarPosition.Top -> base.padding(top = animatedStripSize)
                            else -> base.padding(bottom = animatedStripSize)
                        }
                    }
            ) {
                content()
            }

            NavPanel(
                navigationState = navigationState,
                homeNavState = homeNavState,
                revealed = revealed,
                shouldHide = shouldHide,
                onRevealChange = { revealed = it },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

}

// ============================================
// NAV PANEL — mode dispatcher
// ============================================

@Composable
private fun NavPanel(
    navigationState: MainNavigationState,
    homeNavState: HomeNavigationState,
    revealed: Boolean,
    shouldHide: Boolean,
    onRevealChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    val themeState = LocalKaiteyoThemeState.current
    val layout = themeState.layoutConfig
    val position = layout.sidebarPosition

    if (shouldHide) {
        RevealStrip(
            position = position,
            onReveal = { onRevealChange(true) },
            modifier = modifier
        )
        return
    }

    val sections = buildNavSections(navigationState, homeNavState)

    val enter: EnterTransition
    val exit: ExitTransition
    when (position) {
        SidebarPosition.Left -> {
            enter = slideInHorizontally { -it } + fadeIn()
            exit = slideOutHorizontally { -it } + fadeOut()
        }
        SidebarPosition.Right -> {
            enter = slideInHorizontally { it } + fadeIn()
            exit = slideOutHorizontally { it } + fadeOut()
        }
        SidebarPosition.Top -> {
            enter = slideInVertically { -it } + fadeIn()
            exit = slideOutVertically { -it } + fadeOut()
        }
        SidebarPosition.Bottom -> {
            enter = slideInVertically { it } + fadeIn()
            exit = slideOutVertically { it } + fadeOut()
        }
    }

    AnimatedVisibility(
        visible = !shouldHide,
        enter = enter,
        exit = exit,
        modifier = modifier
    ) {
        Box(Modifier.fillMaxSize()) {
            when (layout.sidebarMode) {
                SidebarMode.FloatingIsland -> FloatingNavPanel(
                    sections = sections,
                    layout = layout,
                    onHoverChange = onRevealChange
                )
                SidebarMode.Docked -> DockNavPanel(
                    sections = sections,
                    layout = layout,
                    onHoverChange = onRevealChange
                )
                SidebarMode.AutoHide,
                SidebarMode.Expanded,
                SidebarMode.Compact,
                SidebarMode.IconsOnly -> DockedNavStrip(
                    sections = sections,
                    layout = layout,
                    onHoverChange = onRevealChange
                )
            }
        }
    }

}

@Composable
private fun NavHoverTracker(onHoverChange: (Boolean) -> Unit): MutableInteractionSource {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var wasHovering by remember { mutableStateOf(false) }
    LaunchedEffect(isHovered) {
        if (isHovered) {
            wasHovering = true
            onHoverChange(true)
        } else if (wasHovering) {
            wasHovering = false
            onHoverChange(false)
        }
    }
    return interactionSource
}

// ============================================
// DOCKED STRIP — Expanded / Compact / Icons Only
// ============================================

@Composable
private fun DockedNavStrip(
    sections: List<NavSection>,
    layout: LayoutConfig,
    onHoverChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val position = layout.sidebarPosition
    val vertical = position == SidebarPosition.Left || position == SidebarPosition.Right
    val labelsVisible = layout.sidebarMode == SidebarMode.Expanded && !layout.collapsed
    val radius = scaledRadius(NavTokens.StripRadius)
    val densityMultiplier = layout.density.spacingMultiplier
    val hoverSource = NavHoverTracker(onHoverChange)

    val shape = RoundedCornerShape(radius)
    val stripWidth = if (labelsVisible) layout.panelWidth else NavTokens.CompactStripWidth
    val stripHeight = if (labelsVisible) layout.panelHeight else 48.dp

    Box(modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .align(stripAlignment(position))
                .then(
                    if (vertical) Modifier.fillMaxHeight().width(stripWidth)
                    else Modifier.fillMaxWidth().height(stripHeight)
                )
                .hoverable(hoverSource)
                .shadow(NavTokens.StripElevation, shape),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (vertical) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = (NavTokens.StripPadding * densityMultiplier),
                            vertical = (NavTokens.StripPadding * densityMultiplier)
                        ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    sections.forEach { section ->
                        section.title?.let { NavSectionHeader(it(), layout) }
                        section.entries.forEach { entry ->
                            if (labelsVisible) {
                                ExpandedNavItem(entry, layout, position)
                            } else {
                                CompactNavItem(entry, layout, position)
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState())
                        .padding(
                            horizontal = (NavTokens.StripPadding * densityMultiplier),
                            vertical = 4.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    sections.forEach { section ->
                        section.title?.let { NavSectionHeader(it(), layout) }
                        section.entries.forEach { entry ->
                            if (labelsVisible) {
                                ExpandedNavItem(entry, layout, position)
                            } else {
                                CompactNavItem(entry, layout, position)
                            }
                        }
                    }
                }
            }
        }

        if (vertical && labelsVisible) {
            ResizeStrip(position = position, layout = layout)
        }
    }

}

@Composable
private fun NavSectionHeader(label: String, layout: LayoutConfig, modifier: Modifier = Modifier) {
    val surfaceColors = LocalSurfaceColors.current
    val multiplier = layout.density.spacingMultiplier
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = surfaceColors.textMuted,
        fontWeight = FontWeight.Medium,
        modifier = modifier.padding(
            start = (Dimens.Space3 * multiplier),
            top = (Dimens.Space3 * multiplier),
            bottom = (Dimens.Space1 * multiplier)
        )
    )
}

// ============================================
// NAV ITEMS
// ============================================

@Composable
private fun ExpandedNavItem(
    entry: NavEntry,
    layout: LayoutConfig,
    position: SidebarPosition,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val multiplier = layout.density.spacingMultiplier
    val radius = scaledRadius(Dimens.RadiusMd)

    val backgroundColor = when {
        entry.selected -> accent.primary.copy(alpha = 0.12f)
        isHovered -> surfaceColors.surfaceInteractive
        else -> Color.Transparent
    }
    val contentColor = when {
        entry.selected -> accent.primary
        isHovered -> surfaceColors.textPrimary
        else -> surfaceColors.textSecondary
    }

    val content: @Composable RowScope.() -> Unit = {
        NavEntryIcon(entry, tint = contentColor)
        Spacer(Modifier.width(Dimens.Space3 * multiplier))
        Text(
            text = entry.label(),
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            fontWeight = if (entry.selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }

    Box(
        modifier = modifier
            .then(
                if (position == SidebarPosition.Left || position == SidebarPosition.Right)
                    Modifier.fillMaxWidth()
                else Modifier.width(IntrinsicSize.Max)
            )
            .clip(RoundedCornerShape(radius))
            .background(backgroundColor)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) accent.primary.copy(alpha = 0.8f) else Color.Transparent,
                shape = RoundedCornerShape(radius)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = entry.enabled,
                onClick = entry.onClick
            )
            .hoverable(interactionSource)
            .padding(
                horizontal = Dimens.Space3 * multiplier,
                vertical = Dimens.Space2 * multiplier
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

@Composable
private fun CompactNavItem(
    entry: NavEntry,
    layout: LayoutConfig,
    position: SidebarPosition,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val radius = scaledRadius(Dimens.RadiusMd)
    var bounds by remember { mutableStateOf<Rect?>(null) }

    val backgroundColor = when {
        entry.selected -> accent.primary.copy(alpha = 0.12f)
        isHovered -> surfaceColors.surfaceInteractive
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .size(NavTokens.CompactItemSize)
            .clip(RoundedCornerShape(radius))
            .background(backgroundColor)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) accent.primary.copy(alpha = 0.8f) else Color.Transparent,
                shape = RoundedCornerShape(radius)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = entry.enabled,
                onClick = entry.onClick
            )
            .hoverable(interactionSource),
        contentAlignment = Alignment.Center
    ) {
        NavEntryIcon(entry, tint = if (entry.selected) accent.primary else surfaceColors.textMuted, label = entry.label())
    }

    if (isHovered && bounds != null) {
        NavTooltip(label = entry.label(), anchor = bounds!!, position = position)
    }
}

@Composable
private fun NavEntryIcon(entry: NavEntry, tint: Color, size: Dp = NavTokens.ItemIconSize, label: String? = null) {
    val icon = entry.icon
    if (icon != null) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(size), tint = tint)
    } else {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            entry.iconContent?.invoke()
        }
    }
}

// ============================================
// TOOLTIP (compact mode)
// ============================================

@Composable
private fun NavTooltip(
    label: String,
    anchor: Rect,
    position: SidebarPosition
) {
    val density = LocalDensity.current
    var tooltipSize by remember { mutableStateOf(IntSize.Zero) }

    val (anchorX, anchorY) = with(density) {
        when (position) {
            SidebarPosition.Left -> IntOffset(
                (anchor.right + 12.dp.roundToPx()).roundToInt(),
                (anchor.top - 4.dp.roundToPx()).roundToInt()
            )
            SidebarPosition.Right -> IntOffset(
                (anchor.left - 12.dp.roundToPx()).roundToInt(),
                (anchor.top - 4.dp.roundToPx()).roundToInt()
            )
            SidebarPosition.Top -> IntOffset(
                anchor.left.roundToInt(),
                (anchor.bottom + 12.dp.roundToPx()).roundToInt()
            )
            SidebarPosition.Bottom -> IntOffset(
                anchor.left.roundToInt(),
                (anchor.top - 12.dp.roundToPx()).roundToInt()
            )
        }
    }

    val translateX = if (position == SidebarPosition.Right) (-tooltipSize.width).dp else 0.dp
    val translateY = if (position == SidebarPosition.Bottom) (-tooltipSize.height).dp else 0.dp

    Popup(
        offset = IntOffset(anchorX, anchorY),
        properties = PopupProperties(dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier.offset(x = translateX, y = translateY)
        ) {
            Surface(
                modifier = Modifier.onSizeChanged { tooltipSize = it },
                shape = RoundedCornerShape(scaledRadius(Dimens.RadiusSm)),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = NavTokens.DockElevation
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalSurfaceColors.current.textPrimary,
                    modifier = Modifier.padding(horizontal = Dimens.Space3, vertical = Dimens.Space2)
                )
            }
        }
    }
}

// ============================================
// REVEAL STRIP — edge hover target when hidden
// ============================================

@Composable
private fun RevealStrip(
    position: SidebarPosition,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    LaunchedEffect(isHovered) { if (isHovered) onReveal() }

    val vertical = position == SidebarPosition.Left || position == SidebarPosition.Right

    Box(modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(stripAlignment(position))
                .then(
                    if (vertical) Modifier.fillMaxHeight().width(8.dp)
                    else Modifier.fillMaxWidth().height(8.dp)
                )
                .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusSm)))
                .background(
                    if (isHovered) surfaceColors.surfaceInteractive
                    else surfaceColors.surfaceElevated
                )
                .hoverable(interactionSource)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onReveal)
        )
    }
}

// ============================================
// RESIZE STRIP — docked side panels
// ============================================

@Composable
private fun ResizeStrip(
    position: SidebarPosition,
    layout: LayoutConfig,
    modifier: Modifier = Modifier
) {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val currentLayout by rememberUpdatedState(layout)
    var resizing by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(
                    when (position) {
                        SidebarPosition.Left -> Alignment.CenterEnd
                        SidebarPosition.Right -> Alignment.CenterStart
                        else -> Alignment.Center
                    }
                )
                .width(NavTokens.ResizeStripWidth)
                .fillMaxHeight()
                .background(
                    when {
                        resizing -> LocalKaiteyoAccent.current.primary.copy(alpha = 0.25f)
                        isHovered -> surfaceColors.border
                        else -> Color.Transparent
                    }
                )
                .hoverable(interactionSource)
                .pointerInput(position) {
                    detectHorizontalDragGestures(
                        onDragStart = { resizing = true },
                        onDragEnd = { resizing = false },
                        onDragCancel = { resizing = false },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val direction = if (position == SidebarPosition.Right) -1f else 1f
                            val newWidth = (currentLayout.panelWidth + (dragAmount * direction).toDp())
                                .coerceIn(180.dp, 480.dp)
                            themeState.layoutConfig = themeState.layoutConfig.copy(
                                panelWidth = newWidth
                            )
                        }
                    )
                }
        )
    }
}

// ============================================
// FLOATING PANEL — Floating Island
// ============================================

@Composable
private fun FloatingNavPanel(
    sections: List<NavSection>,
    layout: LayoutConfig,
    onHoverChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    val windowWidth = with(density) { windowSize.width.toDp() }
    val windowHeight = with(density) { windowSize.height.toDp() }
    val hoverSource = NavHoverTracker(onHoverChange)

    var dragOffset by remember { mutableStateOf(layout.floatingOffset) }
    var panelSize by remember { mutableStateOf(IntSize.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    val shape = RoundedCornerShape(scaledRadius(NavTokens.FloatingRadius))
    val effectiveHeight = if (layout.panelHeight < 64.dp) 240.dp else layout.panelHeight

    Box(
        modifier = modifier
            .offset { IntOffset(dragOffset.x.roundToPx(), dragOffset.y.roundToPx()) }
            .width(layout.panelWidth)
            .height(effectiveHeight)
            .hoverable(hoverSource)
            .onSizeChanged { panelSize = it }
            .shadow(
                elevation = NavTokens.FloatingElevation,
                shape = shape,
                ambientColor = accent.primary.copy(alpha = 0.08f),
                spotColor = accent.primary.copy(alpha = 0.16f)
            )
            .clip(shape)
            .background(
                if (layout.transparencyEnabled)
                    surfaceColors.surfaceElevated.copy(alpha = layout.glassOpacity)
                else surfaceColors.surfaceElevated
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        themeState.layoutConfig = themeState.layoutConfig.copy(
                            floatingOffset = dragOffset
                        )
                    },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset = clampFloatingOffset(
                            offset = dragOffset,
                            dragAmount = DpOffset(dragAmount.x.toDp(), dragAmount.y.toDp()),
                            windowWidth = windowWidth,
                            windowHeight = windowHeight,
                            panelSize = panelSize,
                            density = density
                        )
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.Space3),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sections.forEach { section ->
                section.title?.let { NavSectionHeader(it(), layout) }
                section.entries.forEach { entry ->
                    ExpandedNavItem(entry, layout, SidebarPosition.Left)
                }
            }
        }

        if (layout.collapsed.not()) {
            FloatingResizeHandle(
                layout = layout,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun FloatingResizeHandle(
    layout: LayoutConfig,
    modifier: Modifier = Modifier
) {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current
    val currentLayout by rememberUpdatedState(layout)
    var resizing by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .padding(Dimens.Space2)
            .size(18.dp)
            .clip(RoundedCornerShape(Dimens.RadiusXs))
            .background(if (resizing) LocalKaiteyoAccent.current.primary.copy(alpha = 0.3f) else surfaceColors.border.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { resizing = true },
                    onDragEnd = { resizing = false },
                    onDragCancel = { resizing = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        themeState.layoutConfig = themeState.layoutConfig.copy(
                            panelWidth = (currentLayout.panelWidth + dragAmount.x.toDp()).coerceIn(180.dp, 480.dp),
                            panelHeight = (currentLayout.panelHeight + dragAmount.y.toDp()).coerceIn(160.dp, 800.dp)
                        )
                    }
                )
            }
    )
}

// ============================================
// DOCK PANEL — macOS-style dock
// ============================================

@Composable
private fun DockNavPanel(
    sections: List<NavSection>,
    layout: LayoutConfig,
    onHoverChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val position = layout.sidebarPosition
    val vertical = position == SidebarPosition.Left || position == SidebarPosition.Right
    val radius = scaledRadius(NavTokens.DockRadius)
    val hoverSource = NavHoverTracker(onHoverChange)

    val entries = sections.flatMap { it.entries }

    Box(modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(
                    when (position) {
                        SidebarPosition.Left -> Alignment.CenterStart
                        SidebarPosition.Right -> Alignment.CenterEnd
                        SidebarPosition.Top -> Alignment.TopCenter
                        SidebarPosition.Bottom -> Alignment.BottomCenter
                    }
                )
                .padding(
                    start = if (position == SidebarPosition.Left) NavTokens.EdgeMargin else 0.dp,
                    end = if (position == SidebarPosition.Right) NavTokens.EdgeMargin else 0.dp,
                    top = if (position == SidebarPosition.Top) NavTokens.EdgeMargin else 0.dp,
                    bottom = if (position == SidebarPosition.Bottom) NavTokens.EdgeMargin else 0.dp
                )
        ) {
            Surface(
                modifier = Modifier.hoverable(hoverSource),
                shape = RoundedCornerShape(radius),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = NavTokens.DockElevation
            ) {
                if (vertical) {
                    Column(
                        modifier = Modifier.padding(Dimens.Space2),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        entries.forEach { DockNavItem(it, layout) }
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(Dimens.Space2),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        entries.forEach { DockNavItem(it, layout) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DockNavItem(
    entry: NavEntry,
    layout: LayoutConfig,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.25f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 320f),
        label = "dockScale"
    )

    val backgroundColor = when {
        entry.selected -> accent.primary.copy(alpha = 0.15f)
        isHovered -> surfaceColors.surfaceInteractive
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .size(NavTokens.DockItemSize)
            .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
            .background(backgroundColor)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) accent.primary.copy(alpha = 0.8f) else Color.Transparent,
                shape = RoundedCornerShape(scaledRadius(Dimens.RadiusMd))
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = entry.enabled,
                onClick = entry.onClick
            )
            .hoverable(interactionSource)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        NavEntryIcon(entry, tint = if (entry.selected) accent.primary else surfaceColors.textMuted, label = entry.label())
    }
}

// ============================================
// NAV SECTIONS BUILDER
// ============================================

@Composable
private fun buildNavSections(
    navigationState: MainNavigationState,
    homeNavState: HomeNavigationState
): List<NavSection> {
    val currentDestination = navigationState.currentDestination.value
    val onHome = currentDestination is MainDestination.Home
    val selectedTab = homeNavState.selectedTab.value

    val homeEntries = HomeScreenTab.VisibleTabs.map { tab ->
        NavEntry(
            id = "home_${tab.name}",
            label = { resolveString(tab.titleResolver) },
            icon = null,
            iconContent = tab.iconContent,
            selected = onHome && selectedTab == tab,
            onClick = {
                if (!onHome) navigationState.navigateToTop(MainDestination.Home)
                homeNavState.navigate(tab)
            }
        )
    }

    val featureEntries = listOf(
        Triple<MainDestination, @Composable () -> String, ImageVector>(
            MainDestination.DeckBrowser,
            { resolveString { nav.decksLabel } },
            Icons.Outlined.CollectionsBookmark
        ),
        Triple<MainDestination, @Composable () -> String, ImageVector>(
            MainDestination.TextAnalysis,
            { resolveString { nav.textAnalysisLabel } },
            Icons.Outlined.Translate
        ),
        Triple<MainDestination, @Composable () -> String, ImageVector>(
            MainDestination.StatisticsDashboard,
            { resolveString { home.statsTabLabel } },
            Icons.Outlined.BarChart
        )
    ).map { (destination, label, icon) ->
        destinationEntry(destination, label, icon, currentDestination, navigationState)
    }

    val systemEntries = buildList {
        add(destinationEntry(
            MainDestination.AppearanceStudio,
            { resolveString { nav.appearanceLabel } },
            Icons.Outlined.Palette,
            currentDestination,
            navigationState
        ))
        add(destinationEntry(
            MainDestination.Backup,
            { resolveString { nav.backupLabel } },
            Icons.Outlined.CloudUpload,
            currentDestination,
            navigationState
        ))
        add(destinationEntry(
            MainDestination.Sync,
            { resolveString { nav.syncLabel } },
            Icons.Outlined.Sync,
            currentDestination,
            navigationState
        ))
        if (PlatformFeature.supported) {
            add(destinationEntry(
                MainDestination.Sponsor,
                { resolveString { nav.sponsorLabel } },
                Icons.Outlined.Handshake,
                currentDestination,
                navigationState
            ))
        }
        add(destinationEntry(
            MainDestination.About,
            { resolveString { nav.aboutLabel } },
            Icons.Outlined.Info,
            currentDestination,
            navigationState
        ))
    }

    return listOf(
        NavSection(title = { resolveString { nav.homeSection } }, entries = homeEntries),
        NavSection(title = { resolveString { nav.featuresSection } }, entries = featureEntries),
        NavSection(title = { resolveString { nav.systemSection } }, entries = systemEntries)
    )
}

private fun destinationEntry(
    destination: MainDestination,
    label: @Composable () -> String,
    icon: ImageVector,
    currentDestination: MainDestination?,
    navigationState: MainNavigationState
): NavEntry {
    return NavEntry(
        id = "dest_${destination.analyticsName ?: destination::class.simpleName}",
        label = label,
        icon = icon,
        selected = currentDestination == destination,
        onClick = { navigationState.navigate(destination) }
    )
}

// ============================================
// HELPERS
// ============================================

private fun defaultHomeTab(appPreferences: PreferencesContract.AppPreferences): HomeScreenTab {
    return when (runBlocking { appPreferences.defaultHomeTab.get() }) {
        PreferencesDefaultHomeTab.GeneralDashboard -> HomeScreenTab.GeneralDashboard
        PreferencesDefaultHomeTab.Letters -> HomeScreenTab.Library
        PreferencesDefaultHomeTab.Vocab -> HomeScreenTab.Library
    }
}

private fun stripAlignment(position: SidebarPosition): Alignment {
    return when (position) {
        SidebarPosition.Left,
        SidebarPosition.Top -> Alignment.TopStart
        SidebarPosition.Right -> Alignment.TopEnd
        SidebarPosition.Bottom -> Alignment.BottomStart
    }
}

private fun clampFloatingOffset(
    offset: DpOffset,
    dragAmount: DpOffset,
    windowWidth: Dp,
    windowHeight: Dp,
    panelSize: IntSize,
    density: androidx.compose.ui.unit.Density
): DpOffset {
    val panelWidth = with(density) { panelSize.width.toDp() }.coerceAtLeast(180.dp)
    val panelHeight = with(density) { panelSize.height.toDp() }
    val maxX = (windowWidth - panelWidth - NavTokens.EdgeMargin * 2).coerceAtLeast(0.dp)
    val maxY = (windowHeight - panelHeight.coerceAtMost(400.dp) - NavTokens.EdgeMargin * 2).coerceAtLeast(0.dp)
    return DpOffset(
        x = (offset.x + dragAmount.x).coerceIn(0.dp, maxX),
        y = (offset.y + dragAmount.y).coerceIn(0.dp, maxY)
    )
}

@Composable
private fun scaledRadius(base: Dp): Dp {
    val multiplier = LocalRadiusConfig.current.style.globalMultiplier
    return base * multiplier
}

private fun Modifier.shadow(
    elevation: Dp,
    shape: androidx.compose.ui.graphics.Shape,
    ambientColor: Color = Color.Black.copy(alpha = 0.3f),
    spotColor: Color = Color.Black.copy(alpha = 0.3f)
): Modifier = Modifier.materialShadow(
    elevation = elevation,
    shape = shape,
    ambientColor = ambientColor,
    spotColor = spotColor
)

