package ua.syt0r.kanji.presentation.common.nav

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.staticCompositionLocalOf
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
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
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
import ua.syt0r.kanji.presentation.common.debug.DebugPanel
import ua.syt0r.kanji.presentation.common.debug.DebugSettingsDialog
import ua.syt0r.kanji.presentation.common.debug.DebugSettingsState
import ua.syt0r.kanji.presentation.common.debug.LocalDebugSettings
import ua.syt0r.kanji.presentation.common.debug.rememberDebugSettingsState
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.ui.PageIdentity
import ua.syt0r.kanji.presentation.common.ui.PageRegistry
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoThemeState
import ua.syt0r.kanji.presentation.common.theme.LocalRadiusConfig
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SidebarPosition
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoPalette
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoSearch
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
 * True while the desktop window shell is actively running a resize drag.
 * Layouts that normally animate (sidebar width, content reserve) read this
 * and snap to their targets instead of chasing a window size that changes
 * on every frame. Provided by the desktop shell; defaults to false on
 * mobile and in previews.
 */
val LocalWindowResizing = staticCompositionLocalOf { false }

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
    val debugSettings = rememberDebugSettingsState(appPreferences)
    val defaultTab = remember { defaultHomeTab(appPreferences) }
    val homeNavState = rememberHomeNavigationState(defaultTab)

    CompositionLocalProvider(
        LocalNavigationSettings provides navSettings,
        LocalDebugSettings provides debugSettings,
        LocalHomeNavigationState provides homeNavState
    ) {
        AdaptiveNavigation(
            navigationState = navigationState,
            homeNavState = homeNavState,
            navSettings = navSettings,
            debugSettings = debugSettings,
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
    debugSettings: DebugSettingsState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val formFactor = rememberFormFactor()
    val settings = navSettings.settings
    val mode = settings.mode
    val edge = settings.edgeFor(formFactor)
    val vertical = edge == SidebarPosition.Left || edge == SidebarPosition.Right
    val animations = settings.animationsEnabled && !settings.accessibility.reducedMotion &&
        !debugSettings.settings.disableAnimations
    val expanded = settings.expansionFor(formFactor) == SidebarExpansion.Expanded

    // Publish the space the bottom bar / bottom-anchored bubble occupies so
    // overlays (the root SnackbarHost) can clear it.
    val density = LocalDensity.current
    val containerWidthDp = with(density) {
        LocalWindowInfo.current.containerSize.width.toDp()
    }
    // The docked region size: width for a vertical sidebar, height for a
    // horizontal bar. Both the content reservation and the bar surface derive
    // from this single value so they can never disagree.
    val dockedSize = NavGeometry.dockedBarSize(settings, formFactor, expanded, vertical, containerWidthDp)
    val bottomInset = if (edge == SidebarPosition.Bottom) horizontalBarInsetDp(edge) else 0.dp
    val bubbleBottomSpace = if (mode == NavigationMode.Floating) {
        val snap = settings.snapPointFor(formFactor)
        if (snap.name.startsWith("Bottom") || snap.name.endsWith("Bottom")) {
            settings.accessibility.scaledHitbox(settings.bubble.size).dp + settings.bubble.safeMargin.dp
        } else 0.dp
    } else 0.dp
    val navBarBottomSpace = LocalNavBarBottomSpace.current
    SideEffect {
        navBarBottomSpace.value = when {
            mode == NavigationMode.Sidebar && edge == SidebarPosition.Bottom -> dockedSize + bottomInset
            mode == NavigationMode.Floating -> bubbleBottomSpace
            else -> 0.dp
        }
    }

    val sections = buildPrimaryNavSections(navigationState, homeNavState)
    var debugSettingsOpen by remember { mutableStateOf(false) }

    // During a live resize drag the dock follows the window instantly — a
    // spring toward a size that changes every frame only adds lag and jitter.
    // Provided by the desktop window shell (see LocalWindowResizing).
    val resizing = LocalWindowResizing.current
    val dockAnimSpec = if (resizing) snap() else navAnimSpec(animations)

    // The dock occupies REAL layout space next to the content — never a
    // padding hack and never a full-window overlay stacked on top of it.
    // Each edge slot animates its own size, so switching modes grows the dock
    // out of its edge (and shrinks it back), switching edges slides it across,
    // and the content — a weighted sibling — always keeps the remaining
    // space. The content can never be covered, zero-sized or negatively
    // padded, and the old "padding must be non-negative" crash is impossible
    // because there is no padding.
    val isSidebar = mode == NavigationMode.Sidebar
    val dockLeft by animateDpAsState(
        targetValue = if (isSidebar && edge == SidebarPosition.Left) dockedSize else 0.dp,
        animationSpec = dockAnimSpec,
        label = "dockLeft"
    )
    val dockRight by animateDpAsState(
        targetValue = if (isSidebar && edge == SidebarPosition.Right) dockedSize else 0.dp,
        animationSpec = dockAnimSpec,
        label = "dockRight"
    )
    val dockTop by animateDpAsState(
        targetValue = if (isSidebar && edge == SidebarPosition.Top) dockedSize else 0.dp,
        animationSpec = dockAnimSpec,
        label = "dockTop"
    )
    val dockBottom by animateDpAsState(
        targetValue = if (isSidebar && edge == SidebarPosition.Bottom) dockedSize else 0.dp,
        animationSpec = dockAnimSpec,
        label = "dockBottom"
    )

    val transitionMs = settings.effectiveDurationMs(animations)
    val fadeInSpec: androidx.compose.animation.core.FiniteAnimationSpec<Float> =
        if (animations) tween(transitionMs) else snap()
    val fadeOutSpec: androidx.compose.animation.core.FiniteAnimationSpec<Float> =
        if (animations) tween(transitionMs) else snap()

    // The floating layer simply fades in place over the full-width content;
    // the dock slides in/out by animating its own width/height alongside the
    // content (no scale-from-corner sweep that read as "the sidebar covers
    // the whole screen").
    val floatingEnter: EnterTransition = fadeIn(fadeInSpec)
    val floatingExit: ExitTransition = fadeOut(fadeOutSpec)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }
                when {
                    event.isCtrlPressed && event.key == Key.B -> {
                        // Ctrl+B toggles the presentation mode both ways:
                        // Floating ↔ Sidebar. (Expansion is controlled from
                        // the sidebar header, not this shortcut.)
                        navSettings.update { current ->
                            current.copy(
                                mode = if (current.mode == NavigationMode.Floating)
                                    NavigationMode.Sidebar
                                else NavigationMode.Floating
                            )
                        }
                        true
                    }
                    event.isCtrlPressed && event.isShiftPressed && event.key == Key.F -> {
                        // Global universal search (spec §15) — the shortcut was
                        // advertised by the overlay and palette but never wired.
                        KaiteyoSearch.controller.toggle()
                        true
                    }
                    event.isCtrlPressed && event.key == Key.K -> {
                        // Global command palette (KT-SEARCH-008, spec §58) —
                        // advertised as "Ctrl+K to open" but never bound.
                        KaiteyoPalette.controller.toggle()
                        true
                    }
                    else -> false
                }
            }
    ) {
        // The dock takes real layout space on its edge; the content is a
        // weighted sibling that always receives the remaining space. The
        // sidebar physically cannot swallow the window or blank the content.
        if (vertical) {
            Row(Modifier.fillMaxSize()) {
                if (dockLeft > 0.dp) {
                    DockedSidebar(
                        sections = sections,
                        navigationState = navigationState,
                        homeNavState = homeNavState,
                        navSettings = navSettings,
                        formFactor = formFactor,
                        edge = SidebarPosition.Left,
                        vertical = true,
                        dockSize = dockLeft,
                        modifier = Modifier.fillMaxHeight()
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    content()
                }
                if (dockRight > 0.dp) {
                    DockedSidebar(
                        sections = sections,
                        navigationState = navigationState,
                        homeNavState = homeNavState,
                        navSettings = navSettings,
                        formFactor = formFactor,
                        edge = SidebarPosition.Right,
                        vertical = true,
                        dockSize = dockRight,
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                if (dockTop > 0.dp) {
                    DockedSidebar(
                        sections = sections,
                        navigationState = navigationState,
                        homeNavState = homeNavState,
                        navSettings = navSettings,
                        formFactor = formFactor,
                        edge = SidebarPosition.Top,
                        vertical = false,
                        dockSize = dockTop,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    content()
                }
                if (dockBottom > 0.dp) {
                    DockedSidebar(
                        sections = sections,
                        navigationState = navigationState,
                        homeNavState = homeNavState,
                        navSettings = navSettings,
                        formFactor = formFactor,
                        edge = SidebarPosition.Bottom,
                        vertical = false,
                        dockSize = dockBottom,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Floating mode chrome — the draggable launcher bubble. It overlays
        // the content (the bubble itself is the only chrome in this mode).
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

        // Page name indicator — a small top-right pill naming the current
        // screen (plus its analytics code). Off by default; enabled in
        // Navigation settings so bug reports can name the exact page.
        // Page name indicator — a small pill naming the current screen.
        // Positioned opposite the sidebar so it never hides behind the dock.
        if (debugSettings.settings.showPageInfo) {
            val sidebarOnLeft = settings.desktopEdge == SidebarPosition.Left
            PageNameIndicator(
                navigationState = navigationState,
                homeNavState = homeNavState,
                modifier = Modifier
                    .align(
                        if (sidebarOnLeft || mode == NavigationMode.Floating) Alignment.TopEnd
                        else Alignment.TopStart
                    )
                    .padding(
                        top = 10.dp,
                        start = if (sidebarOnLeft || mode == NavigationMode.Floating) 0.dp else 12.dp,
                        end = if (sidebarOnLeft || mode == NavigationMode.Floating) 12.dp else 0.dp
                    )
            )
        }

        // Debug overlay — the bottom-corner developer surface: page identity
        // (Page / Route / Panel) with a one-tap "copy debug info" action,
        // optional live FPS + viewport readouts, and a shortcut into Debug
        // settings. Shown when any debug-overlay toggle is enabled.
        if (debugSettings.settings.anyEnabled) {
            val sidebarOnLeft = settings.desktopEdge == SidebarPosition.Left
            DebugPanel(
                page = currentPageIdentity(navigationState.currentDestination.value, homeNavState.selectedTab.value),
                navigationMode = mode.name,
                themeLabel = themeDebugLabel(),
                windowState = LocalWindowPlacement.current?.name.orEmpty(),
                showFps = debugSettings.settings.showFps,
                showViewport = debugSettings.settings.showViewport,
                onOpenSettings = { debugSettingsOpen = true },
                modifier = Modifier
                    .align(
                        if (sidebarOnLeft || mode == NavigationMode.Floating) Alignment.BottomStart
                        else Alignment.BottomEnd
                    )
                    .padding(
                        bottom = 12.dp,
                        start = if (sidebarOnLeft || mode == NavigationMode.Floating) 12.dp else 0.dp,
                        end = if (sidebarOnLeft || mode == NavigationMode.Floating) 0.dp else 12.dp
                    )
            )
        }
        if (debugSettingsOpen) {
            DebugSettingsDialog(
                debugSettings = debugSettings,
                navSettings = navSettings,
                onDismiss = { debugSettingsOpen = false }
            )
        }
    }
}

// ============================================
// PAGE NAME INDICATOR — screen name for bug reports
// ============================================

@Composable
private fun PageNameIndicator(
    navigationState: MainNavigationState,
    homeNavState: HomeNavigationState,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val (name, code) = currentPageLabel(
        navigationState.currentDestination.value,
        homeNavState.selectedTab.value
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
            .background(surfaceColors.surfaceElevated.copy(alpha = 0.92f))
            .border(
                width = 1.dp,
                color = surfaceColors.border.copy(alpha = 0.5f),
                shape = RoundedCornerShape(scaledRadius(Dimens.RadiusMd))
            )
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(accent.primary)
        )
        Text(
            text = "PAGE",
            style = MaterialTheme.typography.labelSmall,
            color = surfaceColors.textMuted,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = surfaceColors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        if (code.isNotEmpty()) {
            Text(
                text = code,
                style = MaterialTheme.typography.labelSmall,
                color = surfaceColors.textMuted
            )
        }
    }
}

/**
 * Builds the debug-overlay identity for the current destination. The route
 * derives from the destination's analytics code; the panel names the active
 * sub-surface (e.g. the Home tab). Screens that know more detail can
 * override via [ua.syt0r.kanji.presentation.common.ui.ProvidePageIdentity].
 */
@Composable
private fun currentPageIdentity(
    destination: MainDestination?,
    homeTab: HomeScreenTab
): PageIdentity {
    val (name, code) = currentPageLabel(destination, homeTab)
    val route = PageRegistry.routeFor(code.ifBlank { destination?.analyticsName })
    val panel = if (destination is MainDestination.Home) {
        homeTab.analyticsName
    } else {
        null
    }
    return PageIdentity(
        id = code.ifBlank { "home" },
        name = name,
        route = route,
        panel = panel
    )
}

/** Human-readable screen name plus its analytics code, for bug reports. */
@Composable
private fun currentPageLabel(
    destination: MainDestination?,
    homeTab: HomeScreenTab
): Pair<String, String> {
    val homeTabName = resolveString(homeTab.titleResolver)
    val (name, code) = when (destination) {
        is MainDestination.Home -> homeTabName to homeTab.analyticsName
        is MainDestination.DeckDetails -> "Deck details" to (destination.analyticsName ?: "")
        is MainDestination.LetterPractice -> "Letter practice" to (destination.analyticsName ?: "")
        is MainDestination.VocabPractice -> "Vocab practice" to (destination.analyticsName ?: "")
        is MainDestination.DeckEdit -> "Deck editor" to (destination.analyticsName ?: "")
        is MainDestination.DeckPicker -> "Deck picker" to (destination.analyticsName ?: "")
        is MainDestination.CardBrowser -> "Card browser" to (destination.analyticsName ?: "")
        is MainDestination.DeckBrowser -> "Deck browser" to (destination.analyticsName ?: "")
        is MainDestination.Info -> "Details" to (destination.analyticsName ?: "")
        is MainDestination.VocabCard -> "Word card" to (destination.analyticsName ?: "")
        is MainDestination.StatisticsDashboard -> "Statistics" to (destination.analyticsName ?: "")
        is MainDestination.DayPractice -> "Day practice" to (destination.analyticsName ?: "")
        is MainDestination.KanjiBrowser -> "Kanji browser" to (destination.analyticsName ?: "")
        is MainDestination.KanjiEntry -> "Kanji entry" to (destination.analyticsName ?: "")
        is MainDestination.WordEntry -> "Word entry" to (destination.analyticsName ?: "")
        is MainDestination.KnowledgeGraph -> "Knowledge graph" to (destination.analyticsName ?: "")
        is MainDestination.RadicalExplorer -> "Radical explorer" to (destination.analyticsName ?: "")
        is MainDestination.SentenceEntry -> "Sentence" to (destination.analyticsName ?: "")
        is MainDestination.SentenceExplorer -> "Sentence explorer" to (destination.analyticsName ?: "")
        is MainDestination.LearnerProfile -> "Learner profile" to (destination.analyticsName ?: "")
        is MainDestination.ComponentExplorer -> "Component explorer" to (destination.analyticsName ?: "")
        is MainDestination.BrowseHub -> "Browse" to (destination.analyticsName ?: "")
        is MainDestination.CollectionDetail -> "Collection" to (destination.analyticsName ?: "")
        is MainDestination.Collections -> "Collections" to (destination.analyticsName ?: "")
        is MainDestination.Media -> "Media" to (destination.analyticsName ?: "")
        is MainDestination.Game -> "World" to (destination.analyticsName ?: "")
        is MainDestination.World -> "World 3D" to (destination.analyticsName ?: "")
        is MainDestination.CardSettings -> "Card layouts" to (destination.analyticsName ?: "")
        is MainDestination.SearchEngine -> "Search" to (destination.analyticsName ?: "")
        is MainDestination.StudyHistory -> "Study history" to (destination.analyticsName ?: "")
        is MainDestination.Backup -> "Backup" to (destination.analyticsName ?: "")
        is MainDestination.Feedback -> "Feedback" to (destination.analyticsName ?: "")
        is MainDestination.KeyboardShortcuts -> "Shortcuts" to (destination.analyticsName ?: "")
        else -> {
            val raw = destination?.analyticsName ?: "home"
            val human = raw.replace('_', ' ').replaceFirstChar { it.uppercaseChar() }
            human to raw
        }
    }
    return name to code
}

/** The current theme's short label for the debug overlay copy payload. */
@Composable
private fun themeDebugLabel(): String =
    LocalKaiteyoThemeState.current.baseMode.displayName

@Composable
private fun DockedSidebar(
    sections: List<NavSection>,
    navigationState: MainNavigationState,
    homeNavState: HomeNavigationState,
    navSettings: NavigationSettingsState,
    formFactor: FormFactor,
    edge: SidebarPosition,
    vertical: Boolean,
    dockSize: Dp,
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

    // Render-time guard on top of the NavGeometry clamp: even if upstream
    // state ever regresses, the dock surface can never exceed half the window
    // — the sidebar physically cannot swallow the screen at the drawing node.
    val density = LocalDensity.current
    val containerWidthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val safeDockSize = dockSize.coerceAtMost(containerWidthDp * 0.5f)

    // The horizontal bar must be exactly as tall as the reserved content
    // space (dockSize + system inset) so it never overlaps the content.
    val barInsetDp = if (vertical) 0.dp else horizontalBarInsetDp(edge)

    Box(modifier) {
        Surface(
            modifier = Modifier
                .align(sidebarAlignment(edge))
                .then(
                    when {
                        // Vertical sidebar: an explicit width (≈20% of the window)
                        // — never the full window width. Without this the inner
                        // fillMaxSize column measures against the whole window
                        // and the "sidebar" swallows 100% of the screen.
                        vertical -> Modifier.fillMaxHeight().width(safeDockSize)
                            .padding(vertical = margin)
                        else -> Modifier.fillMaxWidth().height(dockSize + barInsetDp)
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
            color = surfaceColors.surface
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
                    // Phones get a clean navigation bar: the mode/compact/placement
                    // controls live in Navigation settings instead, keeping the bar
                    // from overflowing narrow widths.
                    if (!formFactor.isPhone) {
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
                    }
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
                    if (!formFactor.isPhone) {
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
        if (expanded) {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space1 * densityMultiplier)
            ) {
                ModeSegmentedControl(
                    navSettings = navSettings,
                    showLabels = true,
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
            // Compact rail: the control cluster stacks vertically so it fits
            // the narrow rail without overflowing or compressing the buttons.
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space1 * densityMultiplier),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NavCompactToggle(navSettings, formFactor, size = NavTokens.CompactItemSize)
                NavPlacementButton(navSettings, formFactor, size = NavTokens.CompactItemSize)
                NavSettingsButton(size = NavTokens.CompactItemSize)
            }
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
                modifier = if (showLabels) Modifier.weight(1f) else Modifier
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
                // Shadow before clip/background so it renders behind the surface.
                .shadow(16.dp, RoundedCornerShape(scaledRadius(Dimens.RadiusLg)))
                .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusLg)))
                .background(surfaceColors.surfaceElevated)
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
// PRIMARY NAV SECTIONS — the single navigation
// model shared by Floating (launchpad) and
// Sidebar modes. One curated set of primary
// destinations — Home · Library · Study · Browse
// · Stats · Media · Settings. Media is a real
// MainDestination with a desktop implementation
// (and an honest placeholder elsewhere), so it
// is a genuine primary destination everywhere.
// ============================================

@Composable
private fun buildPrimaryNavSections(
    navigationState: MainNavigationState,
    homeNavState: HomeNavigationState
): List<NavSection> {
    val currentDestination = navigationState.currentDestination.value
    val onHome = currentDestination is MainDestination.Home
    val selectedTab = homeNavState.selectedTab.value

    fun homeEntry(
        tab: HomeScreenTab,
        label: @Composable () -> String,
        extraSelected: Boolean = false
    ): NavEntry = NavEntry(
        id = "primary_${tab.name}",
        label = label,
        icon = null,
        iconContent = tab.iconContent,
        selected = onHome && selectedTab == tab || extraSelected,
        onClick = {
            if (!onHome) navigationState.navigateToTop(MainDestination.Home)
            homeNavState.navigate(tab)
        }
    )

    return buildList {
        add(
            NavSection(
                title = null,
            entries = listOf(
                homeEntry(HomeScreenTab.GeneralDashboard, { resolveString { nav.homeLabel } }),
                // Library — the primary learning workspace. The unified
                // Library screen hosts decks, the study entry points (continue
                // studying, due/new counts), kanji + vocabulary browsing and
                // the manage tools, so the separate Study destination is folded
                // in here. Stays highlighted for the whole deck feature family
                // (deck browser, card browser, study history) so the user
                // always knows where they are inside it.
                homeEntry(
                    HomeScreenTab.Library,
                    { resolveString { nav.libraryLabel } },
                    extraSelected = currentDestination == MainDestination.DeckBrowser ||
                        currentDestination is MainDestination.CardBrowser ||
                        currentDestination == MainDestination.StudyHistory
                ),
                homeEntry(
                    HomeScreenTab.Search,
                    { resolveString { nav.browseLabel } },
                    extraSelected = currentDestination == MainDestination.SearchEngine
                ),
                homeEntry(
                    HomeScreenTab.Stats,
                    { resolveString { nav.statisticsLabel } },
                    extraSelected = currentDestination == MainDestination.StatisticsDashboard
                ),
                // Media — the immersion workspace (desktop) / info screen (elsewhere).
                NavEntry(
                    id = "primary_media",
                    label = { resolveString { nav.mediaLabel } },
                    icon = Icons.Default.VideoLibrary,
                    iconContent = null,
                    selected = currentDestination == MainDestination.Media,
                    onClick = { navigationState.navigate(MainDestination.Media) }
                ),
                // Kaiteyo World — the node-based curriculum over real study state.
                NavEntry(
                    id = "primary_game",
                    label = { "World" },
                    icon = Icons.Default.Face,
                    iconContent = null,
                    selected = currentDestination == MainDestination.Game,
                    onClick = { navigationState.navigate(MainDestination.Game) }
                ),
                // Kaiteyo World 3D — the streamable Kamakura runtime.
                NavEntry(
                    id = "primary_world",
                    label = { "World 3D" },
                    icon = Icons.Default.Public,
                    iconContent = null,
                    selected = currentDestination == MainDestination.World,
                    onClick = { navigationState.navigate(MainDestination.World) }
                ),
                homeEntry(HomeScreenTab.Settings, { resolveString { home.settingsTabLabel } })
            )
        )
        )
    }
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
    // Critically damped (dampingRatio = 1.0): the content reserve settles
    // without overshoot. An underdamped spring dipping below zero while
    // animating the reserve DOWN (Sidebar → Floating) is what previously fed
    // negative Dp into Modifier.padding and crashed with "Padding must be
    // non-negative".
    return if (animations) spring(dampingRatio = 1f, stiffness = 380f)
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
): Modifier = this.materialShadow(
    elevation = elevation,
    shape = shape,
    ambientColor = Color.Black.copy(alpha = 0.25f),
    spotColor = Color.Black.copy(alpha = 0.35f)
)
