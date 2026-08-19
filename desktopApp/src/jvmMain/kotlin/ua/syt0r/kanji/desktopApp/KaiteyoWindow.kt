package ua.syt0r.kanji.desktopApp

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import java.awt.Cursor
import java.awt.Dimension
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.ui.workspace.LocalCaptureState
import ua.syt0r.kanji.desktop.ui.workspace.LocalWindowControls
import ua.syt0r.kanji.desktop.ui.workspace.WindowActionsMenu
import ua.syt0r.kanji.desktop.ui.workspace.WindowControls
import ua.syt0r.kanji.presentation.common.nav.DesktopWindowPlacement
import ua.syt0r.kanji.presentation.common.nav.LocalWindowPlacement
import ua.syt0r.kanji.presentation.common.nav.LocalWindowResizing
import ua.syt0r.kanji.presentation.common.theme.LocalAnimationConfig
import ua.syt0r.kanji.presentation.common.resources.brand.BrandMark
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoPalette
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoSearch

/** How often the floating-window work-area safety check runs (ms). */
private const val WorkAreaWatchIntervalMs = 2_000L

// ============================================
// KAITEYO WINDOW
// Custom borderless window shell.
//
// - A real 44dp title bar: app title (draggable,
//   double-click toggles maximize/restore) plus
//   native-style window controls on the right.
// - The shell is composed INSIDE the KaiteyoApp
//   theme root, so the title bar, divider and
//   window surface all use the live theme tokens
//   (light / dark / OLED / Theme Studio presets).
// - Rounded corners: Windows 11 hands the actual
//   rounding to DWM (square while maximized,
//   matching the OS); everywhere else the rounded
//   app surface sits on the theme background, so
//   corner cutouts never reveal OS black.
// - The window is constrained to the OS work area
//   (taskbar on any edge, macOS menu bar + dock):
//   startup geometry is corrected before show,
//   resize drags clamp to the work area, and a
//   periodic watch recovers the window if the
//   display topology changes underneath it.
// ============================================

@Composable
fun FrameWindowScope.KaiteyoWindow(
    windowState: WindowState,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
    rememberWindowBounds: Boolean = true,
    captureState: String? = null,
    // Content-derived minimum size for the main app; the dev suite passes a
    // smaller bound so its compact tab-bar tier (720dp) is reachable. Kept as
    // a parameter instead of a global so the two entry points never fight.
    minSize: DpSize = DpSize(
        WindowConstraints.MinWidth,
        WindowConstraints.MinHeight
    )
) {
    val surfaceColors = LocalSurfaceColors.current
    val density = LocalDensity.current
    var isMaximized by remember { mutableStateOf(false) }
    isMaximized = windowState.placement == WindowPlacement.Maximized

    // True while a resize drag is active, so layout elements that normally
    // animate (dock width, etc.) can follow the window instantly instead of
    // chasing a moving target on every frame.
    var windowResizing by remember { mutableStateOf(false) }

    // Where the custom system menu opens (right-click on the title bar,
    // Alt+Space, or the context-menu key). Null = closed.
    var systemMenuPosition by remember { mutableStateOf<IntOffset?>(null) }

    fun toggleMaximize() {
        windowState.placement = if (isMaximized) {
            WindowPlacement.Floating
        } else {
            WindowPlacement.Maximized
        }
    }

    // Persist the floating window's size/position (throttled to ~4 writes/s)
    // so it reopens where the user left it. Maximized geometry is never saved;
    // instead the last floating bounds are kept and the maximize state is
    // remembered so the window reopens maximized. Minimized/fullscreen are
    // skipped entirely.
    if (rememberWindowBounds) {
        LaunchedEffect(Unit) {
            var lastSavedAt = 0L
            var lastFloating = SavedWindowBounds(width = 0, height = 0, x = 0, y = 0)
            snapshotFlow {
                Triple(windowState.placement, windowState.size, windowState.position)
            }
                .distinctUntilChanged()
                .collect { (placement, size, position) ->
                    // Minimized geometry is garbage and fullscreen is never
                    // persisted; only floating (size/position) and maximized
                    // (the flag) are remembered.
                    if (placement == WindowPlacement.Fullscreen) return@collect
                    if (position == WindowPosition.PlatformDefault) return@collect
                    val now = System.currentTimeMillis()
                    if (now - lastSavedAt < 250) return@collect
                    lastSavedAt = now
                    val bounds = with(density) {
                        SavedWindowBounds(
                            width = size.width.roundToPx(),
                            height = size.height.roundToPx(),
                            x = position.x.roundToPx(),
                            y = position.y.roundToPx()
                        )
                    }
                    if (placement == WindowPlacement.Floating) lastFloating = bounds
                    if (placement == WindowPlacement.Maximized) {
                        if (lastFloating.isUsable) {
                            WindowStateStore.save(lastFloating.copy(maximized = true))
                        }
                    } else if (placement == WindowPlacement.Floating) {
                        WindowStateStore.save(bounds)
                    }
                }
        }
    }

    // Safety net for display-topology changes while the app is running
    // (monitor unplugged, taskbar/dock moved, display rotated). Only corrects
    // a floating window that is fully outside its work area or larger than it
    // — it never fights a normal drag or a legitimate partial overlap.
    LaunchedEffect(Unit) {
        while (true) {
            delay(WorkAreaWatchIntervalMs)
            if (windowState.placement != WindowPlacement.Floating) continue
            if (windowState.isMinimized) continue
            val bounds = runCatching { window.bounds }.getOrNull() ?: continue
            if (bounds.width <= 0 || bounds.height <= 0) continue
            val workArea = WindowWorkAreas.forBounds(bounds)
            val needsFix = workArea.intersectionArea(bounds) == 0 ||
                bounds.width > workArea.width ||
                bounds.height > workArea.height
            if (!needsFix) continue
            val clamped = workArea.clampRect(bounds)
            with(density) {
                windowState.position = WindowPosition(clamped.x.toDp(), clamped.y.toDp())
                windowState.size = DpSize(clamped.width.toDp(), clamped.height.toDp())
            }
        }
    }

    val windowControls = WindowControls(
        isMaximized = isMaximized,
        onMinimize = { windowState.isMinimized = true },
        onToggleMaximize = { toggleMaximize() },
        onClose = onClose
    )

    // The window's corner radius comes from the shared radius tokens, so the
    // Theme Studio's radius setting shapes the window chrome too. Maximized
    // windows are square, matching the OS. During resize we keep the rounding
    // stable to avoid the flash-to-rectangle glitch.
    val cornerRadius = if (isMaximized) 0.dp else DsRadius.Xl
    val surfaceShape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    // Windows 11: hand the OS the rounding and a theme-colored hairline
    // border. Retried until the frame is realized (the HWND peer only exists
    // once the window is shown), then re-applied whenever the maximized state
    // or the theme border changes. Best effort — the rounded app surface
    // below remains the universal fallback on Windows 10 / Linux / macOS.
    LaunchedEffect(isMaximized, surfaceColors.border) {
        repeat(5) {
            if (NativeWindowChrome.update(
                    frame = window,
                    isMaximized = isMaximized,
                    borderColorArgb = surfaceColors.border.toArgb()
                )
            ) {
                return@LaunchedEffect
            }
            delay(100L)
        }
    }

    // Enforce the minimum size at the AWT level as well, so every resize path
    // (native + custom handles) respects it.
    val minimumSize = remember(density, minSize) {
        with(density) {
            Dimension(
                minSize.width.roundToPx(),
                minSize.height.roundToPx()
            )
        }
    }
    SideEffect {
        runCatching { window.minimumSize = minimumSize }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Fill the entire window (including what sits under the rounded
            // app surface) with the theme surface — never white or OS black.
            // Using surface (not background) prevents the white flash during
            // resize because surface is the same color as the app content.
            .background(surfaceColors.surface)
            .onPreviewKeyEvent { keyEvent ->
                val palette = KaiteyoPalette.controller
                val search = KaiteyoSearch.controller
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when {
                        search.isOpen && keyEvent.key == Key.Escape -> {
                            search.close()
                            true
                        }
                        search.isOpen && keyEvent.key == Key.DirectionUp -> {
                            search.moveSelection(-1)
                            true
                        }
                        search.isOpen && keyEvent.key == Key.DirectionDown -> {
                            search.moveSelection(1)
                            true
                        }
                        search.isOpen && keyEvent.key == Key.Enter -> {
                            search.executeSelected()
                            true
                        }
                        keyEvent.key == Key.F && keyEvent.isCtrlPressed && keyEvent.isShiftPressed -> {
                            search.toggle()
                            true
                        }
                        palette.isOpen && keyEvent.key == Key.Escape -> {
                            palette.close()
                            true
                        }
                        palette.isOpen && keyEvent.key == Key.DirectionUp -> {
                            palette.selectPrevious()
                            true
                        }
                        palette.isOpen && keyEvent.key == Key.DirectionDown -> {
                            palette.selectNext()
                            true
                        }
                        palette.isOpen && keyEvent.key == Key.Enter -> {
                            palette.executeSelected()
                            true
                        }
                        keyEvent.key == Key.K && keyEvent.isCtrlPressed -> {
                            palette.toggle()
                            true
                        }
                        // Window controls — F11 maximize, Cmd/Ctrl+W close,
                        // Alt+Space or the context-menu key open the system menu.
                        keyEvent.key == Key.Escape && systemMenuPosition != null -> {
                            systemMenuPosition = null
                            true
                        }
                        keyEvent.key == Key.F11 -> {
                            toggleMaximize()
                            true
                        }
                        keyEvent.key == Key.W && (if (isMacOS) keyEvent.isMetaPressed else keyEvent.isCtrlPressed) -> {
                            onClose()
                            true
                        }
                        keyEvent.key == Key.Menu || (keyEvent.key == Key.Spacebar && keyEvent.isAltPressed) -> {
                            systemMenuPosition = IntOffset(12, 12)
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(surfaceShape)
                .background(surfaceColors.surface)
                .border(
                    width = 1.dp,
                    color = if (isMaximized) Color.Transparent
                    else surfaceColors.border.copy(alpha = 0.35f),
                    shape = surfaceShape
                )
        ) {
            CompositionLocalProvider(
                LocalWindowPlacement provides
                    if (isMaximized) DesktopWindowPlacement.Maximized
                    else DesktopWindowPlacement.Floating,
                LocalWindowControls provides windowControls,
                LocalWindowResizing provides windowResizing,
                LocalCaptureState provides captureState
            ) {
                Column(Modifier.fillMaxSize()) {
                    KaiteyoTitleBar(
                        isMaximized = isMaximized,
                        onMinimize = { windowState.isMinimized = true },
                        onToggleMaximize = { toggleMaximize() },
                        onClose = onClose,
                        onOpenSystemMenu = { systemMenuPosition = it }
                    )
                    // Hairline divider under the title bar.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(surfaceColors.border.copy(alpha = 0.18f))
                    )
                    WindowContentFade(
                        modifier = Modifier.weight(1f)
                    ) {
                        content()
                    }
                }
            }

            // Invisible edge/corner resize zones (only while floating — a
            // maximized window never resizes). These are the resize handles
            // for undecorated windows on macOS/Linux, and work alongside the
            // OS border on Windows. Every drag is clamped to the work area of
            // the display the window is on, so resizing can never push the
            // window under the taskbar or off the usable desktop.
            if (!isMaximized) {
                WindowResizeHandles(
                    windowState = windowState,
                    frame = window,
                    minSize = minSize,
                    onResizeActive = { windowResizing = it }
                )
            }
        }

        // Custom system menu, anchored where the user opened it (right-click
        // position, or the title-bar corner for Alt+Space / Menu key).
        systemMenuPosition?.let { offset ->
            Popup(
                onDismissRequest = { systemMenuPosition = null },
                offset = offset,
                properties = PopupProperties(focusable = true)
            ) {
                WindowActionsMenu(
                    controls = windowControls,
                    onDismiss = { systemMenuPosition = null }
                )
            }
        }
    }
}

// ============================================
// TITLE BAR — app title + window controls
// ============================================

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FrameWindowScope.KaiteyoTitleBar(
    isMaximized: Boolean,
    onMinimize: () -> Unit,
    onToggleMaximize: () -> Unit,
    onClose: () -> Unit,
    onOpenSystemMenu: (IntOffset) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title region: drags the window, double-click maximizes/restores.
        // The drag modifier is a sibling behind the controls, so the window
        // control buttons never start a drag.
        //
        // On Windows/Linux a press hands the drag straight to the OS (Windows:
        // WM_NCLBUTTONDOWN/HTCAPTION drag loop, Linux: EWMH _NET_WM_MOVERESIZE)
        // for 1:1 native tracking. Windows also gives native double-click
        // maximize/restore as part of the drag loop; Linux keeps the manual
        // double-tap handler as a fallback for WMs that cannot take over. The
        // Compose draggable area stays underneath as the universal fallback.
        // Window icon — native-style: a click opens the system menu, a
        // double-click closes the window. It sits OUTSIDE the draggable area
        // so a press never starts a window drag (on Windows the native drag
        // takes over on any press inside it).
        WindowTitleLogo(
            onClick = { onOpenSystemMenu(IntOffset(12, 12)) },
            onDoubleClick = onClose,
            modifier = Modifier.padding(start = 16.dp)
        )
        WindowDraggableArea(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(
                    if (isNativeDragAvailable) {
                        Modifier.pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown()
                                startNativeWindowDrag(window)
                            }
                        }
                    } else {
                        Modifier
                    }
                )
                .then(
                    if (!isWindows) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = { onToggleMaximize() })
                        }
                    } else {
                        Modifier
                    }
                )
                // Right-click anywhere on the title bar opens the system menu,
                // like a native title bar.
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.button == PointerButton.Secondary) {
                                val pos = event.changes.first().position
                                onOpenSystemMenu(IntOffset(pos.x.roundToInt(), pos.y.roundToInt()))
                            }
                        }
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kaiteyo",
                    color = LocalSurfaceColors.current.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        WindowControlButtons(
            isMaximized = isMaximized,
            onMinimize = onMinimize,
            onToggleMaximize = onToggleMaximize,
            onClose = onClose,
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}

// ============================================
// Window Icon — the Kaiteyo mark. Native-style: a
// click opens the system menu; it never drags.
// ============================================

@Composable
private fun WindowTitleLogo(
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(RoundedCornerShape(5.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    // The tap fires after the double-tap window elapses, so a
                    // quick second click cancels the menu and closes instead.
                    onTap = { onClick() },
                    onDoubleTap = { onDoubleClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // The real Kaiteyo app mark — centralized brand asset, not a "K".
        BrandMark(modifier = Modifier.fillMaxSize(), contentDescription = null)
    }
}

// ============================================
// WINDOW CONTROLS — minimize · maximize · close
// Native-style: transparent until hover; the close
// button turns red on hover like a standard title
// bar. Idle/hover glyph colors come from the theme
// so the controls adapt to light/dark/custom
// presets; hover transitions honor reduced motion.
// ============================================

@Composable
private fun WindowControlButtons(
    isMaximized: Boolean,
    onMinimize: () -> Unit,
    onToggleMaximize: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WindowControlButton(
            icon = "\u2500",
            onClick = onMinimize,
            glowColor = Color.Transparent,
            contentDescription = "Minimize"
        )
        WindowControlButton(
            icon = if (isMaximized) "\u2750" else "\u25A1",
            onClick = onToggleMaximize,
            glowColor = Color.Transparent,
            contentDescription = if (isMaximized) "Restore" else "Maximize"
        )
        WindowControlButton(
            icon = "\u2715",
            onClick = onClose,
            glowColor = LocalKaiteyoAccent.current.primary.copy(alpha = 0.25f),
            hoverTextColor = LocalKaiteyoAccent.current.primary,
            contentDescription = "Close"
        )
    }
}

@Composable
private fun WindowControlButton(
    icon: String,
    onClick: () -> Unit,
    glowColor: Color,
    hoverTextColor: Color = Color.White,
    contentDescription: String,
    size: Dp = 40.dp
) {
    val sc = LocalSurfaceColors.current
    val reducedMotion = LocalAnimationConfig.current.reducedMotion
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor by animateColorAsState(
        targetValue = if (isHovered) glowColor.copy(alpha = 0.9f)
        else Color.Transparent,
        animationSpec = tween(if (reducedMotion) 0 else 120),
        label = "windowControlBg"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            isHovered && glowColor != Color.Transparent -> hoverTextColor
            isHovered -> sc.textPrimary
            else -> sc.textMuted
        },
        animationSpec = tween(if (reducedMotion) 0 else 120),
        label = "windowControlColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.08f else 1f,
        animationSpec = if (reducedMotion) tween(0) else spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "windowControlScale"
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center
        )
    }
}

// ============================================
// STARTUP FADE-IN
// The app surface fades in gently on first show,
// giving the window a deliberate, premium open.
// Skipped under reduced motion.
// ============================================

@Composable
private fun WindowContentFade(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val reducedMotion = LocalAnimationConfig.current.reducedMotion
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (reducedMotion) 0 else 320),
        label = "windowFadeIn"
    )
    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
    ) {
        content()
    }
}

// ============================================
// WINDOW RESIZE HANDLES
// Invisible edge/corner zones that resize the
// undecorated window on every platform. On
// Windows the OS border already resizes; these
// are what make edge/corner resizing work on
// macOS and Linux, and they coexist with the
// native border on Windows.
//
// Every drag is clamped to the work area of the
// display the window is on (captured at drag
// start), so resizing can never place the window
// under the taskbar or beyond the usable desktop.
// ============================================

private enum class ResizeZone(
    val cursor: PointerIcon,
    val west: Boolean = false,
    val east: Boolean = false,
    val north: Boolean = false,
    val south: Boolean = false
) {
    NorthWest(PointerIcon(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR)), west = true, north = true),
    North(PointerIcon(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)), north = true),
    NorthEast(PointerIcon(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR)), east = true, north = true),
    East(PointerIcon(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)), east = true),
    SouthEast(PointerIcon(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR)), east = true, south = true),
    South(PointerIcon(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR)), south = true),
    SouthWest(PointerIcon(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR)), west = true, south = true),
    West(PointerIcon(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)), west = true)
}

private data class ResizeStart(
    val size: DpSize,
    val position: WindowPosition,
    val workArea: WorkArea
)

/**
 * Resize-drag modifier: captures the window's size/position and work area on
 * drag start and applies the standard 8-zone resize math on every drag delta.
 * The window stays in place on the anchored side while the dragged edge
 * follows the pointer; the resulting bounds are clamped to the minimum size
 * and the captured work area.
 */
@Composable
private fun Modifier.windowResizeZone(
    zone: ResizeZone,
    windowState: WindowState,
    frame: java.awt.Frame,
    minWidthPx: Int,
    minHeightPx: Int,
    density: Density,
    onResizeActive: (Boolean) -> Unit
): Modifier {
    var start by remember { mutableStateOf<ResizeStart?>(null) }

    return pointerHoverIcon(zone.cursor, overrideDescendants = true)
        .pointerInput(zone) {
            detectDragGestures(
                onDragStart = {
                    start = ResizeStart(
                        size = windowState.size,
                        position = windowState.position,
                        workArea = WindowWorkAreas.forBounds(runCatching { frame.bounds }.getOrNull()
                            ?: java.awt.Rectangle(0, 0, 0, 0))
                    )
                    onResizeActive(true)
                },
                onDragEnd = {
                    start = null
                    onResizeActive(false)
                },
                onDragCancel = {
                    start = null
                    onResizeActive(false)
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val s = start ?: return@detectDragGestures
                    val wa = s.workArea
                    val startW = with(density) { s.size.width.roundToPx() }
                    val startH = with(density) { s.size.height.roundToPx() }
                    val startX = with(density) { s.position.x.roundToPx() }
                    val startY = with(density) { s.position.y.roundToPx() }
                    val dx = dragAmount.x.roundToInt()
                    val dy = dragAmount.y.roundToInt()

                    var x = startX
                    var y = startY
                    var w = startW
                    var h = startH

                    when {
                        zone.west -> {
                            // Right edge anchored; left edge follows the
                            // pointer but never leaves the work area.
                            w = startW - dx
                            x = startX + (startW - w)
                            x = x.coerceAtLeast(wa.x)
                            val maxW = wa.right - x
                            w = if (maxW < minWidthPx) maxW else w.coerceIn(minWidthPx, maxW)
                        }
                        zone.east -> {
                            w = startW + dx
                            val maxW = wa.right - x
                            w = if (maxW < minWidthPx) maxW else w.coerceIn(minWidthPx, maxW)
                        }
                        zone.north -> {
                            // Bottom edge anchored; top edge never leaves the
                            // work area (a top taskbar stays uncovered).
                            h = startH - dy
                            y = startY + (startH - h)
                            y = y.coerceAtLeast(wa.y)
                            val maxH = wa.bottom - y
                            h = if (maxH < minHeightPx) maxH else h.coerceIn(minHeightPx, maxH)
                        }
                        zone.south -> {
                            h = startH + dy
                            val maxH = wa.bottom - y
                            h = if (maxH < minHeightPx) maxH else h.coerceIn(minHeightPx, maxH)
                        }
                    }

                    windowState.position = WindowPosition(
                        with(density) { x.toDp() },
                        with(density) { y.toDp() }
                    )
                    windowState.size = DpSize(
                        with(density) { w.toDp() },
                        with(density) { h.toDp() }
                    )
                }
            )
        }
}

@Composable
private fun WindowResizeHandles(
    windowState: WindowState,
    frame: java.awt.Frame,
    minSize: DpSize,
    onResizeActive: (Boolean) -> Unit
) {
    val density = LocalDensity.current
    val edge = 5.dp
    val corner = 10.dp
    val minWidthPx = with(density) { minSize.width.roundToPx() }
    val minHeightPx = with(density) { minSize.height.roundToPx() }

    Box(Modifier.fillMaxSize()) {
        // Corner zones (10×10dp) cover the strip ends; the edge zones are
        // padded away from them, so the zones never overlap and the cursor
        // always matches the nearest resize direction.
        Box(Modifier.align(Alignment.TopStart).size(corner).windowResizeZone(ResizeZone.NorthWest, windowState, frame, minWidthPx, minHeightPx, density, onResizeActive))
        Box(Modifier.align(Alignment.TopEnd).size(corner).windowResizeZone(ResizeZone.NorthEast, windowState, frame, minWidthPx, minHeightPx, density, onResizeActive))
        Box(Modifier.align(Alignment.BottomStart).size(corner).windowResizeZone(ResizeZone.SouthWest, windowState, frame, minWidthPx, minHeightPx, density, onResizeActive))
        Box(Modifier.align(Alignment.BottomEnd).size(corner).windowResizeZone(ResizeZone.SouthEast, windowState, frame, minWidthPx, minHeightPx, density, onResizeActive))
        // Edges — inset from the corners so the corner zones win at the joints.
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(edge).padding(horizontal = corner).windowResizeZone(ResizeZone.North, windowState, frame, minWidthPx, minHeightPx, density, onResizeActive))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(edge).padding(horizontal = corner).windowResizeZone(ResizeZone.South, windowState, frame, minWidthPx, minHeightPx, density, onResizeActive))
        Box(Modifier.align(Alignment.CenterStart).fillMaxHeight().width(edge).padding(vertical = corner).windowResizeZone(ResizeZone.West, windowState, frame, minWidthPx, minHeightPx, density, onResizeActive))
        Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(edge).padding(vertical = corner).windowResizeZone(ResizeZone.East, windowState, frame, minWidthPx, minHeightPx, density, onResizeActive))
    }
}
