package ua.syt0r.kanji.presentation.common.nav

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
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
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
 * Covers the docked bottom navigation bar, and in floating mode a
 * bottom-anchored launcher bubble (so bottom snackbars never cover it).
 * Zero when nothing sits at the bottom edge: side/top placement, or floating
 * mode with a non-bottom bubble snap.
 */
val LocalNavBarBottomSpace = compositionLocalOf<MutableState<Dp>> { mutableStateOf(0.dp) }

// ============================================
// NAV SHELL — unified adaptive navigation
// Two modes — Floating and Sidebar — across
// desktop, tablet and phone.
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
    val expanded = settings.expansionFor(formFactor) == SidebarExpansion.Expanded

    // Publish the space the bottom bar / bottom-anchored bubble occupies so
    // overlays (the root SnackbarHost) can clear it.
    val sidebarSize = dockedBarSize(settings, formFactor, expanded, vertical)
    val bottomInset = if (edge == SidebarPosition.Bottom) horizontalBarInsetDp(edge) else 0.dp
    val bubbleBottomSpace = if (mode == NavigationMode.Floating) {
        val snap = settings.snapPointFor(formFactor)
        if (snap.name.startsWith("Bottom") || snap.name.endsWith("Bottom")) {
            settings.accessibility.scaledHitbox(settings.bubble.size).dp + BubbleEdgeMargin
        } else 0.dp
    } else 0.dp
    val navBarBottomSpace = LocalNavBarBottomSpace.current
    SideEffect {
        navBarBottomSpace.value = when {
            mode == NavigationMode.Sidebar && edge == SidebarPosition.Bottom -> sidebarSize + bottomInset
            mode == NavigationMode.Floating -> bubbleBottomSpace
            else -> 0.dp
        }
    }

    val sections = buildNavSections(navigationState, homeNavState)

    // The docked bar reserves space in sidebar mode only; in floating mode the
    // content owns the whole surface. The reserve animates across the switch
    // so the mode transition feels like one continuous layout.
    val horizontalInset = if (vertical) 0.dp else horizontalBarInsetDp(edge)
    val dockedReserve = if (mode == NavigationMode.Sidebar) sidebarSize + horizontalInset else 0.dp
    val animatedReserve by animateDpAsState(
        targetValue = dockedReserve,
        animationSpec = navAnimSpec(animations),
        label = "contentReserve"
    )

    val transitionMs = settings.effectiveDurationMs(animations)
    val enterSpring: androidx.compose.animation.core.FiniteAnimationSpec<Float> =
        spring(dampingRatio = 0.7f, stiffness = 300f)
    val fadeInSpec: androidx.compose.animation.core.FiniteAnimationSpec<Float> =
        if (animations) tween(transitionMs) else snap()
    val fadeOutSpec: androidx.compose.animation.core.FiniteAnimationSpec<Float> =
        if (animations) tween(transitionMs) else snap()

    // Where the floating bubble sits (from the persisted snap point) and where
    // the sidebar sits (from the current edge) — these drive the transform
    // origins so one mode visibly grows out of the other.
    val snapOrigin = snapPointTransformOrigin(settings.snapPointFor(formFactor))
    val edgeOrigin = edgeTransformOrigin(edge)

    val floatingEnter: EnterTransition = fadeIn(fadeInSpec) + scaleIn(
        initialScale = 0.86f,
        transformOrigin = edgeOrigin,
        animationSpec = enterSpring
    )
    val floatingExit: ExitTransition = fadeOut(fadeOutSpec) + scaleOut(
        targetScale = 0.86f,
        transformOrigin = edgeOrigin,
        animationSpec = if (animations) tween(transitionMs) else snap()
    )
    val sidebarEnter: EnterTransition = fadeIn(fadeInSpec) + scaleIn(
        initialScale = 0.9f,
        transformOrigin = snapOrigin,
        animationSpec = enterSpring
    )
    val sidebarExit: ExitTransition = fadeOut(fadeOutSpec) + scaleOut(
        targetScale = 0.9f,
        transformOrigin = snapOrigin,
        animationSpec = if (animations) tween(transitionMs) else snap()
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.key == Key.B) {
                    navSettings.update { current ->
                        if (current.mode == NavigationMode.Floating) current.copy(mode = NavigationMode.Sidebar)
                        else current.copy(
                            sidebarExpansion = if (current.sidebarExpansion == SidebarExpansion.Expanded)
                                SidebarExpansion.Compact
                            else SidebarExpansion.Expanded
                        )
                    }
                    true
                } else {
                    false
                }
            }
    ) {
        // Content — reserved space follows the active mode.
        Box(
            Modifier
                .fillMaxSize()
                .then(
                    when (edge) {
                        SidebarPosition.Left -> Modifier.padding(start = animatedReserve)
                        SidebarPosition.Right -> Modifier.padding(end = animatedReserve)
                        SidebarPosition.Top -> Modifier.padding(top = animatedReserve)
                        else -> Modifier.padding(bottom = animatedReserve)
                    }
                )
        ) {
            content()
        }

        // Floating mode chrome — the draggable launcher bubble.
        AnimatedVisibility(
            visible = mode == NavigationMode.Floating,
            modifier = Modifier.fillMaxSize(),
            enter = floatingEnter,
            exit = floatingExit
        ) {
            BubbleLauncher(
                navigationState = navigationState,
                homeNavState = homeNavState,
                navSettings = navSettings,
                formFactor = formFactor,
                sections = sections,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Sidebar mode chrome — the docked navigation on the selected edge.
        AnimatedVisibility(
            visible = mode == NavigationMode.Sidebar,
            modifier = Modifier.fillMaxSize(),
            enter = sidebarEnter,
            exit = sidebarExit
        ) {
            DockedNavigation(
                sections = sections,
                navigationState = navigationState,
                homeNavState = homeNavState,
                navSettings = navSettings,
                formFactor = formFactor,
                edge = edge,
                vertical = vertical,
                animations = animations
            )
        }
    }
}

// ============================================
// DOCKED NAVIGATION — Sidebar mode
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
    animations: Boolean
) {
    // Switching edges reflows the sidebar — the old dock slides away while the
    // new one enters from its edge, so the sidebar never teleports.
    AnimatedContent(
        targetState = edge,
        transitionSpec = {
            val forward = SidebarPosition.entries.indexOf(targetState) > SidebarPosition.entries.indexOf(initialState)
            // Slides animate in pixel space; a Dp spec is meaningless here.
            val spec: androidx.compose.animation.core.FiniteAnimationSpec<IntOffset> =
                if (animations) spring(dampingRatio = 0.7f, stiffness = 300f)
                else androidx.compose.animation.core.snap()
            val enterSlide = when (targetState) {
                SidebarPosition.Left -> slideInHorizontally(spec) { -it }
                SidebarPosition.Right -> slideInHorizontally(spec) { it }
                SidebarPosition.Top -> slideInVertically(spec) { -it }
                else -> slideInVertically(spec) { it }
            }
            val exitSlide = when (initialState) {
                SidebarPosition.Left -> slideOutHorizontally(spec) { -it / 3 }
                SidebarPosition.Right -> slideOutHorizontally(spec) { it / 3 }
                SidebarPosition.Top -> slideOutVertically(spec) { -it / 3 }
                else -> slideOutVertically(spec) { it / 3 }
            }
            val fade: androidx.compose.animation.core.FiniteAnimationSpec<Float> =
                if (animations) tween(180) else snap()
            (enterSlide + fadeIn(fade)) togetherWith (exitSlide + fadeOut(fade))
        },
        label = "navEdge"
    ) { targetEdge ->
        val targetVertical = targetEdge == SidebarPosition.Left || targetEdge == SidebarPosition.Right
        DockedSidebar(
            sections = sections,
            navigationState = navigationState,
            homeNavState = homeNavState,
            navSettings = navSettings,
            formFactor = formFactor,
            edge = targetEdge,
            vertical = targetVertical,
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
    modifier: Modifier = Modifier
) {
    val settings = navSettings.settings
    val expanded = settings.expansionFor(formFactor) == SidebarExpansion.Expanded
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
    val barInsetDp = if (vertical) 0.dp else horizontalBarInsetDp(edge)
    val barSize = dockedBarSize(settings, formFactor, expanded, vertical)

    Box(modifier) {
        Surface(
            modifier = Modifier
                .align(sidebarAlignment(edge))
                .then(
                    when {
                        vertical -> Modifier.fillMaxHeight().widthIn(min = margin)
                            .padding(vertical = margin)
                        else -> Modifier.fillMaxWidth().height(barSize + barInsetDp)
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
                    SidebarHeaderControls(
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
                    if (expanded) {
                        Spacer(Modifier.height(Dimens.Space2 * densityMultiplier))
                        SidebarFooter(
                            accent = accent,
                            surfaceColors = surfaceColors,
                            modifier = Modifier.padding(
                                horizontal = Dimens.Space3 * densityMultiplier,
                                vertical = Dimens.Space2 * densityMultiplier
                            )
                        )
                    }
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
                    SidebarHeaderControls(
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
                    SidebarHeaderTrailing(
                        navSettings = navSettings,
                        formFactor = formFactor,
                        modifier = Modifier.padding(vertical = Dimens.Space1)
                    )
                }
            }
        }
    }
}

// ============================================
// SIDEBAR HEADER — mode control, compact toggle,
// position picker and settings
// ============================================

@Composable
private fun SidebarHeaderControls(
    navSettings: NavigationSettingsState,
    vertical: Boolean,
    formFactor: FormFactor,
    modifier: Modifier = Modifier,
    controlSize: Dp = NavTokens.ModeControlSize
) {
    val surfaceColors = LocalSurfaceColors.current
    val themeState = LocalKaiteyoThemeState.current
    val densityMultiplier = themeState.layoutConfig.density.spacingMultiplier
    val expanded = navSettings.settings.expansionFor(formFactor) == SidebarExpansion.Expanded

    val iconRow: @Composable (Modifier) -> Unit = { rowModifier ->
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavCompactToggle(navSettings, formFactor, size = controlSize)
            NavPlacementButton(navSettings, formFactor, size = controlSize)
            NavSettingsButton(size = controlSize)
        }
    }

    if (vertical) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space1 * densityMultiplier)
        ) {
            ModeSegmentedControl(
                navSettings = navSettings,
                showLabels = expanded,
                controlSize = controlSize,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
                    .background(surfaceColors.surfaceInteractive.copy(alpha = 0.5f))
                    .padding(4.dp)
            )
            iconRow(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
                    .background(surfaceColors.surfaceInteractive.copy(alpha = 0.5f))
                    .padding(4.dp)
            )
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModeSegmentedControl(
                navSettings = navSettings,
                showLabels = false,
                controlSize = controlSize
            )
            Box(
                Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(surfaceColors.border.copy(alpha = 0.5f))
            )
            iconRow(Modifier)
        }
    }
}

/** Trailing cluster for horizontal bars (position + settings). */
@Composable
private fun SidebarHeaderTrailing(
    navSettings: NavigationSettingsState,
    formFactor: FormFactor,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavCompactToggle(navSettings, formFactor, size = NavTokens.ModeControlSize)
        NavPlacementButton(navSettings, formFactor, size = NavTokens.ModeControlSize)
        NavSettingsButton(size = NavTokens.ModeControlSize)
    }
}

/**
 * Segmented two-mode control: Sidebar | Floating. In the sidebar this is the
 * primary way back to Floating mode.
 */
@Composable
private fun ModeSegmentedControl(
    navSettings: NavigationSettingsState,
    showLabels: Boolean,
    controlSize: Dp,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val current = navSettings.settings.mode

    val options = listOf(
        Triple(NavigationMode.Sidebar, Icons.AutoMirrored.Filled.ViewSidebar, resolveString { nav.modeSidebarLabel }),
        Triple(NavigationMode.Floating, Icons.Default.Apps, resolveString { nav.modeFloatingLabel })
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (mode, icon, label) ->
            val selected = current == mode
            ModeControlButton(
                icon = icon,
                label = label,
                selected = selected,
                onClick = { if (!selected) navSettings.setMode(mode) },
                size = controlSize,
                showLabel = showLabels,
                modifier = Modifier.weight(if (showLabels) 1f else 0f)
            )
        }
    }
}

/** Expanded ↔ Compact switch for the sidebar layout. */
@Composable
private fun NavCompactToggle(
    navSettings: NavigationSettingsState,
    formFactor: FormFactor,
    size: Dp = NavTokens.ModeControlSize
) {
    val expanded = navSettings.settings.expansionFor(formFactor) == SidebarExpansion.Expanded
    ModeControlButton(
        icon = if (expanded) Icons.Default.ViewModule else Icons.AutoMirrored.Filled.ViewSidebar,
        label = resolveString { if (expanded) nav.collapseTooltip else nav.expandTooltip },
        selected = false,
        onClick = {
            navSettings.update { current ->
                current.copy(
                    sidebarExpansion = if (current.sidebarExpansion == SidebarExpansion.Expanded)
                        SidebarExpansion.Compact
                    else SidebarExpansion.Expanded
                )
            }
        },
        size = size
    )
}

@Composable
private fun ModeControlButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    size: Dp = NavTokens.ModeControlSize,
    showLabel: Boolean = false,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .then(if (showLabel) Modifier.height(size + 8.dp) else Modifier.size(size))
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (showLabel) Arrangement.spacedBy(6.dp) else Arrangement.Center,
        content = {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) accent.primary else surfaceColors.textMuted,
                modifier = Modifier.size(18.dp)
            )
            if (showLabel) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) accent.primary else surfaceColors.textSecondary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    )
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
// SIDEBAR FOOTER (expanded)
// ============================================

@Composable
private fun SidebarFooter(
    accent: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Kaiteyo",
            style = MaterialTheme.typography.labelMedium,
            color = surfaceColors.textMuted,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.Apps,
            contentDescription = null,
            tint = accent.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun NavSettingsButton(size: Dp = NavTokens.BottomControlSize) {
    val accent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var settingsOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusSm)))
            .background(if (isHovered) surfaceColors.surfaceInteractive else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null) { settingsOpen = true }
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

    val navSettings = LocalNavigationSettings.current ?: return
    if (settingsOpen) {
        val formFactor = rememberFormFactor()
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
// PLACEMENT PICKER — smoothly expanding visual
// position control with a live mini preview
// ============================================

@Composable
private fun NavPlacementButton(
    navSettings: NavigationSettingsState,
    formFactor: FormFactor,
    size: Dp = NavTokens.BottomControlSize
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
                .size(size)
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
                    open = false
                },
                onDismiss = { open = false }
            )
        }
    }
}

@Composable
private fun NavigationPlacementSelector(
    current: SidebarPosition,
    formFactor: FormFactor,
    onSelect: (SidebarPosition) -> Unit,
    onDismiss: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    val edges = if (formFactor.isPhone) {
        listOf(SidebarPosition.Top, SidebarPosition.Bottom)
    } else {
        SidebarPosition.entries
    }

    // Smooth expand: the picker scales out from its anchor with a soft fade.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.82f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 380f),
        label = "placementScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(160),
        label = "placementAlpha"
    )

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 200.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusLg)))
                .background(surfaceColors.surfaceElevated)
                .shadow(16.dp, RoundedCornerShape(scaledRadius(Dimens.RadiusLg)))
                .padding(Dimens.Space3),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            Text(
                text = resolveString { nav.placementLabel },
                style = MaterialTheme.typography.labelMedium,
                color = surfaceColors.textMuted,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = Dimens.Space1)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)) {
                edges.forEach { edge ->
                    val selected = current == edge
                    val interactionSource = remember { MutableInteractionSource() }
                    val isHovered by interactionSource.collectIsHoveredAsState()
                    Column(
                        modifier = Modifier
                            .weight(1f)
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
                            .clickable(interactionSource = interactionSource, indication = null) { onSelect(edge) }
                            .hoverable(interactionSource)
                            .padding(vertical = Dimens.Space2),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.Space1)
                    ) {
                        Icon(
                            placementEdgeIcon(edge),
                            contentDescription = edge.displayName,
                            tint = if (selected) accent.primary else surfaceColors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        // Miniature window preview showing the sidebar on the
                        // selected edge — position meaning is instant.
                        MiniEdgePreview(
                            edge = edge,
                            selected = selected,
                            accent = accent.primary,
                            surfaceColors = surfaceColors
                        )
                    }
                }
            }
        }
    }
}

/** Tiny window mock with the dock drawn on [edge]. */
@Composable
private fun MiniEdgePreview(
    edge: SidebarPosition,
    selected: Boolean,
    accent: Color,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 26.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(surfaceColors.surface.copy(alpha = 0.6f))
            .border(
                1.dp,
                if (selected) accent.copy(alpha = 0.6f) else surfaceColors.border.copy(alpha = 0.4f),
                RoundedCornerShape(4.dp)
            )
    ) {
        val barColor = if (selected) accent.copy(alpha = 0.7f) else surfaceColors.border.copy(alpha = 0.55f)
        when (edge) {
            SidebarPosition.Left -> Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(8.dp)
                    .background(barColor)
            )
            SidebarPosition.Right -> Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(8.dp)
                    .background(barColor)
            )
            SidebarPosition.Top -> Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(7.dp)
                    .background(barColor)
            )
            SidebarPosition.Bottom -> Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(7.dp)
                    .background(barColor)
            )
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
 * Size of a docked navigation bar for the current sidebar layout and form
 * factor. Shared by the content reservation, the bar surface and the
 * published bottom-bar space so they always agree.
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

/** Where the floating bubble sits, as a fraction for scale origins. */
private fun snapPointTransformOrigin(snap: BubbleSnapPoint): TransformOrigin = when (snap) {
    BubbleSnapPoint.TopLeft, BubbleSnapPoint.LeftTop -> TransformOrigin(0f, 0f)
    BubbleSnapPoint.TopCenter -> TransformOrigin(0.5f, 0f)
    BubbleSnapPoint.TopRight, BubbleSnapPoint.RightTop -> TransformOrigin(1f, 0f)
    BubbleSnapPoint.BottomLeft, BubbleSnapPoint.LeftBottom -> TransformOrigin(0f, 1f)
    BubbleSnapPoint.BottomCenter -> TransformOrigin(0.5f, 1f)
    BubbleSnapPoint.BottomRight, BubbleSnapPoint.RightBottom -> TransformOrigin(1f, 1f)
    BubbleSnapPoint.LeftCenter -> TransformOrigin(0f, 0.5f)
    BubbleSnapPoint.RightCenter -> TransformOrigin(1f, 0.5f)
}

/** Where the sidebar edge sits, as a fraction for scale origins. */
private fun edgeTransformOrigin(edge: SidebarPosition): TransformOrigin = when (edge) {
    SidebarPosition.Left -> TransformOrigin(0f, 0.5f)
    SidebarPosition.Right -> TransformOrigin(1f, 0.5f)
    SidebarPosition.Top -> TransformOrigin(0.5f, 0f)
    SidebarPosition.Bottom -> TransformOrigin(0.5f, 1f)
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
