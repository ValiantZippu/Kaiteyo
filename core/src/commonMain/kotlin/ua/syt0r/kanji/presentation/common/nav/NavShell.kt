package ua.syt0r.kanji.presentation.common.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow as materialShadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.core.user_data.preferences.PreferencesDefaultHomeTab
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoThemeState
import ua.syt0r.kanji.presentation.common.theme.LocalRadiusConfig
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SidebarPosition
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.HomeNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.HomeScreenTab
import ua.syt0r.kanji.presentation.screen.main.screen.home.rememberHomeNavigationState

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
    val SidebarElevation = 10.dp
    val SidebarRadius = Dimens.RadiusXl
    val SidebarMargin = 8.dp
    val ItemIconSize = 20.dp
    val CompactItemSize = 40.dp
    /** Touch-friendly target for phone top/bottom bars (Material minimum). */
    val PhoneCompactItemSize = 48.dp
    val CompactRailWidth = 64.dp
    val HorizontalBarHeight = 56.dp
    val HorizontalBarCompactHeight = 48.dp
    /** Phone top/bottom bar height — comfortably fits 48dp touch targets. */
    val PhoneBarHeight = 52.dp
    val ModeControlSize = 32.dp
    val BottomControlSize = 36.dp
    val ItemHeight = 40.dp
}

// ============================================
// COMPOSITION LOCALS
// ============================================

val LocalHomeNavigationState = compositionLocalOf<HomeNavigationState?> { null }

enum class DesktopWindowPlacement { Floating, Maximized }

val LocalWindowPlacement = compositionLocalOf<DesktopWindowPlacement?> { null }

/**
 * Space (in dp) that bottom-docked UI currently occupies, including any system
 * inset it clears. [NavShell] publishes this so overlays placed on top — most
 * importantly the root SnackbarHost in MainScreen — can pad themselves and
 * always render in front of it.
 *
 * Covers the docked bottom navigation bar, and in bubble mode a
 * bottom-anchored floating launcher (so bottom snackbars never cover it).
 * Zero when nothing sits at the bottom edge: side/top placement, bubble mode
 * with a non-bottom launcher anchor, or any non-phone layout without a
 * bottom-docked bar.
 */
val LocalNavBarBottomSpace = compositionLocalOf<MutableState<Dp>> { mutableStateOf(0.dp) }

// ============================================
// NAV SHELL — unified adaptive navigation
// Expanded · Compact · Bubble across desktop,
// tablet and phone.
// ============================================

@Composable
fun NavShell(
    navigationState: MainNavigationState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {

    val appPreferences = koinInject<PreferencesContract.AppPreferences>()
    val navSettings = rememberNavigationSettingsState(appPreferences)
    val defaultTab = remember { defaultHomeTab(appPreferences) }
    val homeNavState = rememberHomeNavigationState(defaultTab)

    CompositionLocalProvider(
        LocalNavigationSettings provides navSettings,
        LocalHomeNavigationState provides homeNavState
    ) {
        AdaptiveNavigation(
            navigationState = navigationState,
            homeNavState = homeNavState,
            navSettings = navSettings,
            modifier = modifier,
            content = content
        )
    }
}

@Composable
private fun AdaptiveNavigation(
    navigationState: MainNavigationState,
    homeNavState: HomeNavigationState,
    navSettings: NavigationSettingsState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val formFactor = rememberFormFactor()
    val settings = navSettings.settings
    val mode = settings.mode
    val edge = settings.edgeFor(formFactor)
    val vertical = edge == SidebarPosition.Left || edge == SidebarPosition.Right
    val animations = settings.animationsEnabled && !settings.accessibility.reducedMotion

    // Publish the space the bottom bar occupies so overlays (the root
    // SnackbarHost) can clear it. The bar surface is always at its full target
    // size, so the target — not the animated spring value — is the right amount.
    val expanded = settings.mode == NavigationMode.Expanded
    val sidebarSize = dockedBarSize(settings, formFactor, expanded, vertical)
    val bottomInset = if (edge == SidebarPosition.Bottom) horizontalBarInsetDp(edge) else 0.dp
    // Bubble mode has no docked bar, but a bottom-anchored launcher floats at a
    // corner where bottom-aligned snackbars could overlap it. Publish its
    // clearance too (scaled bubble size + the shared BubbleEdgeMargin the
    // launcher snaps to), so snackbars always stay clear. This tracks the
    // snap-anchor position; a bubble deliberately dragged well off-anchor is
    // not fully tracked (acceptable — it re-snaps on the next interaction).
    val bubbleBottomSpace = if (mode == NavigationMode.Bubble) {
        val anchor = settings.bubbleAnchorFor(formFactor)
        if (anchor == BubbleAnchor.BottomLeft || anchor == BubbleAnchor.BottomRight) {
            settings.accessibility.scaledHitbox(settings.bubble.size).dp + BubbleEdgeMargin
        } else 0.dp
    } else 0.dp
    val navBarBottomSpace = LocalNavBarBottomSpace.current
    SideEffect {
        navBarBottomSpace.value = when {
            mode != NavigationMode.Bubble && edge == SidebarPosition.Bottom -> sidebarSize + bottomInset
            mode == NavigationMode.Bubble -> bubbleBottomSpace
            else -> 0.dp
        }
    }

    val sections = buildNavSections(navigationState, homeNavState)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    event.isCtrlPressed && event.key == Key.B
                ) {
                    navSettings.update { current ->
                        current.copy(mode = if (current.mode == NavigationMode.Expanded) NavigationMode.Compact else NavigationMode.Expanded)
                    }
                    true
                } else {
                    false
                }
            }
    ) {
        if (mode == NavigationMode.Bubble) {
            // Content gets the whole window; navigation floats on top.
            Box(Modifier.fillMaxSize()) { content() }
            BubbleLauncher(
                navigationState = navigationState,
                homeNavState = homeNavState,
                navSettings = navSettings,
                formFactor = formFactor,
                sections = sections,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            DockedNavigation(
                sections = sections,
                navigationState = navigationState,
                homeNavState = homeNavState,
                navSettings = navSettings,
                formFactor = formFactor,
                edge = edge,
                vertical = vertical,
                animations = animations,
                modifier = Modifier.fillMaxSize(),
                content = content
            )
        }
    }
}

// ============================================
// DOCKED NAVIGATION — Expanded / Compact
// ============================================

@Composable
private fun DockedNavigation(
    sections: List<NavSection>,
    navigationState: MainNavigationState,
    homeNavState: HomeNavigationState,
    navSettings: NavigationSettingsState,
    formFactor: FormFactor,
    edge: SidebarPosition,
    vertical: Boolean,
    animations: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val settings = navSettings.settings
    val expanded = settings.mode == NavigationMode.Expanded
    // Shared with AdaptiveNavigation so the published bottom-bar space always
    // matches the reserved content space — they can't drift.
    val sidebarSize = dockedBarSize(settings, formFactor, expanded, vertical)

    val animatedSize by animateDpAsState(
        targetValue = sidebarSize,
        animationSpec = navAnimSpec(animations),
        label = "navSize"
    )

    // Phone top/bottom bars must clear the system UI: the status bar above a
    // top bar and the system navigation bar / gesture pill below a bottom bar.
    // Desktop and tablet report zero insets, so this is a no-op there.
    val horizontalInsetDp = horizontalBarInsetDp(edge)

    Box(modifier) {
        Box(
            Modifier
                .fillMaxSize()
                .let { base ->
                    when (edge) {
                        SidebarPosition.Left -> base.padding(start = animatedSize)
                        SidebarPosition.Right -> base.padding(end = animatedSize)
                        SidebarPosition.Top -> base.padding(top = animatedSize + horizontalInsetDp)
                        else -> base.padding(bottom = animatedSize + horizontalInsetDp)
                    }
                }
        ) {
            content()
        }

        DockedSidebar(
            sections = sections,
            navigationState = navigationState,
            homeNavState = homeNavState,
            navSettings = navSettings,
            formFactor = formFactor,
            edge = edge,
            vertical = vertical,
            sidebarSize = sidebarSize,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun DockedSidebar(
    sections: List<NavSection>,
    navigationState: MainNavigationState,
    homeNavState: HomeNavigationState,
    navSettings: NavigationSettingsState,
    formFactor: FormFactor,
    edge: SidebarPosition,
    vertical: Boolean,
    sidebarSize: Dp,
    modifier: Modifier = Modifier
) {
    val settings = navSettings.settings
    val expanded = settings.mode == NavigationMode.Expanded
    val themeState = LocalKaiteyoThemeState.current
    val densityMultiplier = themeState.layoutConfig.density.spacingMultiplier
    val radius = scaledRadius(NavTokens.SidebarRadius)
    val shape = RoundedCornerShape(radius)
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    val margin = if (formFactor.isPhone) 0.dp else NavTokens.SidebarMargin
    val itemSpacing = settings.sidebar.compactSpacing.dp

    // The horizontal bar must be exactly as tall as the reserved content
    // space (sidebarSize + system inset) so it never overlaps the content.
    // Same inset as DockedNavigation via the shared helper — they can't drift.
    val barInsetDp = if (vertical) 0.dp else horizontalBarInsetDp(edge)

    Box(modifier) {
        Surface(
            modifier = Modifier
                .align(sidebarAlignment(edge))
                .then(
                    when {
                        vertical -> Modifier.fillMaxHeight().widthIn(min = margin)
                            .padding(vertical = margin)
                        else -> Modifier.fillMaxWidth().height(sidebarSize + barInsetDp)
                            .padding(horizontal = margin)
                    }
                )
                .then(
                    when {
                        vertical && edge == SidebarPosition.Left -> Modifier.padding(start = margin)
                        vertical && edge == SidebarPosition.Right -> Modifier.padding(end = margin)
                        !vertical && edge == SidebarPosition.Top -> Modifier.padding(top = margin)
                        else -> Modifier.padding(bottom = margin)
                    }
                )
                .shadow(NavTokens.SidebarElevation, shape),
            shape = shape,
            color = if (settings.accessibility.highContrast) surfaceColors.surface
            else MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (vertical) {
                Column(Modifier.fillMaxSize()) {
                    SidebarModeControls(
                        navSettings = navSettings,
                        vertical = true,
                        formFactor = formFactor,
                        modifier = Modifier.padding(
                            start = (Dimens.Space2 * densityMultiplier),
                            end = (Dimens.Space2 * densityMultiplier),
                            top = (Dimens.Space2 * densityMultiplier)
                        )
                    )
                    Spacer(Modifier.height(Dimens.Space2 * densityMultiplier))
                    NavSectionsColumn(
                        sections = sections,
                        settings = settings,
                        expanded = expanded,
                        vertical = true,
                        formFactor = formFactor,
                        itemSpacing = itemSpacing,
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    )
                    SidebarBottomControls(
                        navSettings = navSettings,
                        vertical = true,
                        formFactor = formFactor,
                        accent = accent,
                        surfaceColors = surfaceColors,
                        modifier = Modifier.padding(
                            horizontal = Dimens.Space2 * densityMultiplier,
                            vertical = Dimens.Space2 * densityMultiplier
                        )
                    )
                }
            } else {
                // Keep the bar clear of the system status bar / gesture area.
                val barInsets = when (edge) {
                    SidebarPosition.Top -> WindowInsets.statusBars
                    SidebarPosition.Bottom -> WindowInsets.systemBars
                    else -> WindowInsets(0, 0, 0, 0)
                }
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(barInsets)
                        .padding(horizontal = Dimens.Space1),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space1)
                ) {
                    SidebarModeControls(
                        navSettings = navSettings,
                        vertical = false,
                        formFactor = formFactor,
                        controlSize = if (formFactor.isPhone) 28.dp else NavTokens.ModeControlSize,
                        modifier = Modifier.padding(vertical = (Dimens.Space1 * densityMultiplier))
                    )
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(surfaceColors.border.copy(alpha = 0.5f))
                    )
                    NavSectionsColumn(
                        sections = sections,
                        settings = settings,
                        expanded = expanded,
                        vertical = false,
                        formFactor = formFactor,
                        itemSpacing = itemSpacing,
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState())
                    )
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(surfaceColors.border.copy(alpha = 0.5f))
                    )
                    if (formFactor.isPhone) {
                        // Phone bars get a compact settings gear instead of the
                        // desktop placement/settings cluster.
                        PhoneBarSettingsButton(
                            navSettings = navSettings,
                            formFactor = formFactor
                        )
                    } else {
                        SidebarBottomControls(
                            navSettings = navSettings,
                            vertical = false,
                            formFactor = formFactor,
                            accent = accent,
                            surfaceColors = surfaceColors,
                            modifier = Modifier.padding(vertical = Dimens.Space1)
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// MODE CONTROLS — Expanded · Compact · Bubble
// ============================================

@Composable
private fun SidebarModeControls(
    navSettings: NavigationSettingsState,
    vertical: Boolean,
    formFactor: FormFactor,
    modifier: Modifier = Modifier,
    controlSize: Dp = NavTokens.ModeControlSize
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val current = navSettings.settings.mode

    val items = listOf(
        Triple(NavigationMode.Expanded, Icons.AutoMirrored.Filled.ViewSidebar, resolveString { nav.modeExpandedTooltip }),
        Triple(NavigationMode.Compact, Icons.Default.ViewModule, resolveString { nav.modeCompactTooltip }),
        Triple(NavigationMode.Bubble, Icons.Default.Apps, resolveString { nav.modeBubbleTooltip })
    )

    val content: @Composable (Modifier) -> Unit = { rowModifier ->
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (mode, icon, label) ->
                val selected = current == mode
                ModeControlButton(
                    icon = icon,
                    label = label,
                    selected = selected,
                    size = controlSize,
                    onClick = { navSettings.update { it.copy(mode = mode) } }
                )
            }
        }
    }

    if (vertical) {
        content(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
                .background(surfaceColors.surfaceInteractive.copy(alpha = 0.5f))
                .padding(4.dp)
        )
    } else {
        content(Modifier)
    }
}

@Composable
private fun ModeControlButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    size: Dp = NavTokens.ModeControlSize
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusSm)))
            .background(
                when {
                    selected -> accent.primary.copy(alpha = 0.18f)
                    isHovered -> surfaceColors.surfaceInteractive
                    else -> Color.Transparent
                }
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) accent.primary else surfaceColors.textMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ============================================
// SIDEBAR SECTIONS
// ============================================

@Composable
private fun NavSectionsColumn(
    sections: List<NavSection>,
    settings: NavigationSettings,
    expanded: Boolean,
    vertical: Boolean,
    formFactor: FormFactor,
    itemSpacing: Dp,
    modifier: Modifier = Modifier
) {
    val densityMultiplier = LocalKaiteyoThemeState.current.layoutConfig.density.spacingMultiplier
    val padding = (Dimens.Space2 * densityMultiplier)
    val compactHitbox = if (formFactor.isPhone) NavTokens.PhoneCompactItemSize
    else NavTokens.CompactItemSize
    val expandedItemHeight = if (formFactor.isPhone && !vertical) NavTokens.PhoneCompactItemSize
    else NavTokens.ItemHeight

    if (vertical) {
        Column(
            modifier = modifier.padding(horizontal = padding),
            verticalArrangement = Arrangement.spacedBy(itemSpacing / 2)
        ) {
            sections.forEach { section ->
                section.title?.let { NavSectionHeader(it(), settings) }
                section.entries.forEach { entry ->
                    if (expanded) ExpandedNavItem(entry, settings, itemHeight = expandedItemHeight)
                    else CompactNavItem(entry, settings, SidebarPosition.Left, hitboxSize = compactHitbox)
                }
            }
        }
    } else {
        // No vertical padding here — the fixed-height bar centers items.
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(itemSpacing / 2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            sections.forEach { section ->
                section.title?.let { title ->
                    if (expanded && settings.sidebar.labelVisibility == NavLabelVisibility.Always) {
                        NavSectionHeader(title(), settings, Modifier.padding(start = Dimens.Space2))
                    }
                }
                section.entries.forEach { entry ->
                    if (expanded) ExpandedNavItem(entry, settings, itemHeight = expandedItemHeight)
                    else CompactNavItem(entry, settings, SidebarPosition.Top, hitboxSize = compactHitbox)
                }
            }
        }
    }
}

@Composable
private fun NavSectionHeader(label: String, settings: NavigationSettings, modifier: Modifier = Modifier) {
    val surfaceColors = LocalSurfaceColors.current
    if (settings.sidebar.labelVisibility == NavLabelVisibility.Never) return
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = surfaceColors.textMuted,
        fontWeight = FontWeight.Medium,
        modifier = modifier.padding(
            start = Dimens.Space2,
            top = Dimens.Space3,
            bottom = Dimens.Space1
        )
    )
}

// ============================================
// NAV ITEMS
// ============================================

@Composable
private fun ExpandedNavItem(
    entry: NavEntry,
    settings: NavigationSettings,
    modifier: Modifier = Modifier,
    itemHeight: Dp = NavTokens.ItemHeight
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val radius = scaledRadius(Dimens.RadiusMd)
    val iconSize = settings.accessibility.scaledIconSize(settings.sidebar.iconSize).dp

    val backgroundColor = when {
        entry.selected -> accent.primary.copy(alpha = 0.14f)
        isHovered -> surfaceColors.surfaceInteractive
        else -> Color.Transparent
    }
    val contentColor = when {
        entry.selected -> accent.primary
        isHovered -> surfaceColors.textPrimary
        else -> surfaceColors.textSecondary
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(itemHeight)
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
            .padding(horizontal = Dimens.Space3),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NavEntryIcon(entry, tint = contentColor, size = iconSize, label = entry.label())
            Spacer(Modifier.width(Dimens.Space3))
            Text(
                text = entry.label(),
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = if (entry.selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (entry.selected) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(accent.primary)
                )
            }
        }
    }
}

@Composable
private fun CompactNavItem(
    entry: NavEntry,
    settings: NavigationSettings,
    position: SidebarPosition,
    modifier: Modifier = Modifier,
    hitboxSize: Dp = NavTokens.CompactItemSize
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val radius = scaledRadius(Dimens.RadiusMd)
    val hitbox = settings.accessibility.scaledHitbox(hitboxSize.value.toInt()).dp
    val iconSize = settings.accessibility.scaledIconSize(settings.sidebar.iconSize - 2).dp
    var bounds by remember { mutableStateOf<Rect?>(null) }

    val backgroundColor = when {
        entry.selected -> accent.primary.copy(alpha = 0.14f)
        isHovered -> surfaceColors.surfaceInteractive
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .size(hitbox)
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
        NavEntryIcon(entry, tint = if (entry.selected) accent.primary else surfaceColors.textMuted, size = iconSize, label = entry.label())
    }

    if (isHovered && bounds != null && settings.sidebar.labelVisibility != NavLabelVisibility.Never) {
        NavTooltip(label = entry.label(), anchor = bounds!!, position = position)
    }
}

@Composable
private fun NavEntryIcon(entry: NavEntry, tint: Color, size: Dp, label: String? = null) {
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

    val anchorOffset = with(density) {
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
    val anchorX = anchorOffset.x
    val anchorY = anchorOffset.y

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
                shadowElevation = 12.dp
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
// SIDEBAR BOTTOM CONTROLS — placement + settings
// ============================================

@Composable
private fun SidebarBottomControls(
    navSettings: NavigationSettingsState,
    vertical: Boolean,
    formFactor: FormFactor,
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    modifier: Modifier = Modifier
) {
    var settingsOpen by remember { mutableStateOf(false) }

    val content: @Composable (Modifier) -> Unit = { rowModifier ->
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NavPlacementButton(navSettings, formFactor)
            NavSettingsButton(onClick = { settingsOpen = true })
        }
    }

    if (vertical) {
        Box(modifier.fillMaxWidth()) {
            content(
                Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
                    .background(surfaceColors.surfaceInteractive.copy(alpha = 0.5f))
                    .padding(4.dp)
            )
        }
    } else {
        content(modifier)
    }

    if (settingsOpen) {
        NavigationSettingsOverlay(
            navSettings = navSettings,
            formFactor = formFactor,
            onDismiss = { settingsOpen = false }
        )
    }
}

@Composable
private fun NavPlacementButton(
    navSettings: NavigationSettingsState,
    formFactor: FormFactor
) {
    val accent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current
    var open by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val currentEdge = navSettings.settings.edgeFor(formFactor)

    Box {
        Box(
            modifier = Modifier
                .size(NavTokens.BottomControlSize)
                .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusSm)))
                .background(if (isHovered) surfaceColors.surfaceInteractive else Color.Transparent)
                .clickable(interactionSource = interactionSource, indication = null) { open = true }
                .hoverable(interactionSource),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                placementEdgeIcon(currentEdge),
                contentDescription = resolveString { nav.placementLabel },
                tint = accent.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        if (open) {
            NavigationPlacementSelector(
                current = currentEdge,
                formFactor = formFactor,
                onSelect = { edge ->
                    navSettings.update { current ->
                        if (formFactor.isPhone) current.copy(phone = current.phone.copy(edge = edge))
                        else current.copy(desktopEdge = edge)
                    }
                },
                onDismiss = { open = false },
                anchor = IntOffset.Zero
            )
        }
    }
}

@Composable
private fun NavSettingsButton(onClick: () -> Unit) {
    val accent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .size(NavTokens.BottomControlSize)
            .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusSm)))
            .background(if (isHovered) surfaceColors.surfaceInteractive else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Settings,
            contentDescription = resolveString { nav.settingsLabel },
            tint = surfaceColors.textMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ============================================
// PHONE BAR SETTINGS BUTTON
// Top/bottom bars keep a single gear that opens
// the navigation settings dialog (mode, placement,
// bubble and phone layout all live there).
// ============================================

@Composable
private fun PhoneBarSettingsButton(
    navSettings: NavigationSettingsState,
    formFactor: FormFactor
) {
    var settingsOpen by remember { mutableStateOf(false) }

    NavSettingsButton(onClick = { settingsOpen = true })

    if (settingsOpen) {
        NavigationSettingsOverlay(
            navSettings = navSettings,
            formFactor = formFactor,
            onDismiss = { settingsOpen = false }
        )
    }
}

private fun placementEdgeIcon(edge: SidebarPosition): ImageVector = when (edge) {
    SidebarPosition.Left -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
    SidebarPosition.Right -> Icons.AutoMirrored.Filled.KeyboardArrowRight
    SidebarPosition.Top -> Icons.Default.KeyboardArrowUp
    SidebarPosition.Bottom -> Icons.Default.KeyboardArrowDown
}

// ============================================
// PLACEMENT SELECTOR — visual icon buttons
// ============================================

@Composable
private fun NavigationPlacementSelector(
    current: SidebarPosition,
    formFactor: FormFactor,
    onSelect: (SidebarPosition) -> Unit,
    onDismiss: () -> Unit,
    anchor: IntOffset
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    val edges = if (formFactor.isPhone) {
        listOf(SidebarPosition.Top, SidebarPosition.Bottom)
    } else {
        SidebarPosition.entries
    }

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 172.dp)
                .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusLg)))
                .background(surfaceColors.surfaceElevated)
                .shadow(16.dp, RoundedCornerShape(scaledRadius(Dimens.RadiusLg)))
                .padding(Dimens.Space3)
        ) {
            Text(
                text = resolveString { nav.placementLabel },
                style = MaterialTheme.typography.labelMedium,
                color = surfaceColors.textMuted,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = Dimens.Space1, bottom = Dimens.Space2)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)) {
                edges.forEach { edge ->
                    val selected = current == edge
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .size(44.dp)
                            .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
                            .background(
                                if (selected) accent.primary.copy(alpha = 0.16f)
                                else surfaceColors.surfaceInteractive.copy(alpha = 0.4f)
                            )
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) accent.primary else surfaceColors.border.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(scaledRadius(Dimens.RadiusMd))
                            )
                            .clickable { onSelect(edge) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            placementEdgeIcon(edge),
                            contentDescription = edge.displayName,
                            tint = if (selected) accent.primary else surfaceColors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
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

    // Note: Statistics intentionally lives in the Home section as the Stats tab —
    // it is NOT duplicated here in Features.
    val featureEntries = listOf(
        Triple<MainDestination, @Composable () -> String, ImageVector>(
            MainDestination.DeckBrowser,
            { resolveString { nav.decksLabel } },
            Icons.Default.CollectionsBookmark
        ),
        Triple<MainDestination, @Composable () -> String, ImageVector>(
            MainDestination.TextAnalysis,
            { resolveString { nav.textAnalysisLabel } },
            Icons.Default.GridView
        )
    ).map { (destination, label, icon) ->
        destinationEntry(destination, label, icon, currentDestination, navigationState)
    }

    val systemEntries = buildList {
        add(destinationEntry(
            MainDestination.KanjiBrowser(),
            { resolveString { nav.kanjiBrowserLabel } },
            Icons.Default.Search,
            currentDestination,
            navigationState
        ))
        add(destinationEntry(
            MainDestination.AppearanceStudio,
            { resolveString { nav.appearanceLabel } },
            Icons.Default.Palette,
            currentDestination,
            navigationState
        ))
        add(destinationEntry(
            MainDestination.Backup,
            { resolveString { nav.backupLabel } },
            Icons.AutoMirrored.Filled.ArrowBack,
            currentDestination,
            navigationState
        ))
        add(destinationEntry(
            MainDestination.Account(),
            { resolveString { nav.accountLabel } },
            Icons.Default.Person,
            currentDestination,
            navigationState
        ))
        add(destinationEntry(
            MainDestination.Credits,
            { resolveString { nav.creditsLabel } },
            Icons.Default.Info,
            currentDestination,
            navigationState
        ))
        add(destinationEntry(
            MainDestination.About,
            { resolveString { nav.aboutLabel } },
            Icons.Default.Info,
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

// ============================================
// LAUNCHPAD QUICK ACCESS — curated destinations
// Note: Browser / Media / OCR / Mining live only in the desktop suite and
// have no core MainDestination, so they are intentionally not listed here —
// wiring them would create dead navigation.
// ============================================

@Composable
internal fun buildQuickAccessSection(
    navigationState: MainNavigationState,
    homeNavState: HomeNavigationState
): NavSection {
    val currentDestination = navigationState.currentDestination.value
    val onHome = currentDestination is MainDestination.Home
    val selectedTab = homeNavState.selectedTab.value

    fun homeEntry(
        tab: HomeScreenTab,
        label: @Composable () -> String,
        icon: ImageVector?
    ): NavEntry = NavEntry(
        id = "quick_${tab.name}",
        label = label,
        icon = icon,
        iconContent = tab.iconContent,
        selected = onHome && selectedTab == tab,
        onClick = {
            if (!onHome) navigationState.navigateToTop(MainDestination.Home)
            homeNavState.navigate(tab)
        }
    )

    return NavSection(
        title = { resolveString { nav.quickAccessLabel } },
        entries = listOf(
            homeEntry(
                HomeScreenTab.GeneralDashboard,
                { resolveString { nav.homeLabel } },
                Icons.Default.Home
            ),
            homeEntry(
                HomeScreenTab.Library,
                { resolveString { nav.libraryLabel } },
                null
            ),
            destinationEntry(
                MainDestination.DeckBrowser,
                { resolveString { nav.studyLabel } },
                Icons.Default.ViewModule,
                currentDestination,
                navigationState
            ),
            homeEntry(
                HomeScreenTab.Search,
                { resolveString { nav.dictionaryLabel } },
                Icons.Default.Search
            ),
            homeEntry(
                HomeScreenTab.Stats,
                { resolveString { nav.statisticsLabel } },
                Icons.Default.BarChart
            ),
            destinationEntry(
                MainDestination.Collections,
                { resolveString { nav.collectionsLabel } },
                Icons.Default.CollectionsBookmark,
                currentDestination,
                navigationState
            ),
            destinationEntry(
                MainDestination.KanjiBrowser(),
                { resolveString { nav.kanjiBrowserLabel } },
                Icons.Default.Tune,
                currentDestination,
                navigationState
            ),
            homeEntry(
                HomeScreenTab.Settings,
                { resolveString { home.settingsTabLabel } },
                Icons.Default.Settings
            )
        )
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

/**
 * Size of a docked navigation bar for the current mode and form factor.
 * Shared by the content reservation, the bar surface and the published
 * bottom-bar space so they always agree.
 */
private fun dockedBarSize(
    settings: NavigationSettings,
    formFactor: FormFactor,
    expanded: Boolean,
    vertical: Boolean
): Dp = when {
    vertical && expanded -> settings.sidebar.expandedWidth.dp
    vertical -> NavTokens.CompactRailWidth
    // Phone bars use a fixed comfortable height for touch; expanded vs
    // compact differs only by whether labels are shown.
    formFactor.isPhone -> NavTokens.PhoneBarHeight
    expanded -> NavTokens.HorizontalBarHeight
    else -> NavTokens.HorizontalBarCompactHeight
}

/**
 * System inset a horizontal (top/bottom) bar must clear. Shared by the
 * content-padding calculation and the bar sizing so they always agree.
 */
@Composable
private fun horizontalBarInsetDp(edge: SidebarPosition): Dp {
    val density = LocalDensity.current
    val insetPx = when (edge) {
        SidebarPosition.Top -> WindowInsets.statusBars.getTop(density)
        SidebarPosition.Bottom -> WindowInsets.systemBars.getBottom(density)
        else -> 0
    }
    return with(density) { insetPx.toDp() }
}

private fun sidebarAlignment(position: SidebarPosition): Alignment {
    return when (position) {
        SidebarPosition.Left,
        SidebarPosition.Top -> Alignment.TopStart
        SidebarPosition.Right -> Alignment.TopEnd
        SidebarPosition.Bottom -> Alignment.BottomStart
    }
}

private fun navAnimSpec(animations: Boolean): androidx.compose.animation.core.FiniteAnimationSpec<Dp> {
    return if (animations) spring(dampingRatio = 0.7f, stiffness = 280f)
    else androidx.compose.animation.core.snap()
}

@Composable
private fun scaledRadius(base: Dp): Dp {
    val multiplier = LocalRadiusConfig.current.style.globalMultiplier
    return base * multiplier
}

private fun Modifier.shadow(
    elevation: Dp,
    shape: androidx.compose.ui.graphics.Shape
): Modifier = Modifier.materialShadow(
    elevation = elevation,
    shape = shape,
    ambientColor = Color.Black.copy(alpha = 0.25f),
    spotColor = Color.Black.copy(alpha = 0.35f)
)
