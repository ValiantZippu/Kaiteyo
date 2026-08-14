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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import java.awt.Cursor
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
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.WindowPosition
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import ua.syt0r.kanji.presentation.common.nav.DesktopWindowPlacement
import ua.syt0r.kanji.presentation.common.nav.LocalWindowPlacement
import ua.syt0r.kanji.desktop.ui.workspace.LocalCaptureState
import ua.syt0r.kanji.desktop.ui.workspace.LocalWindowControls
import ua.syt0r.kanji.desktop.ui.workspace.WindowActionsMenu
import ua.syt0r.kanji.desktop.ui.workspace.WindowControls
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoPalette

// ============================================
// KAITEYO WINDOW
// Custom borderless window shell.
//
// - A real 44dp title bar: app title (draggable,
//   double-click toggles maximize/restore) plus
//   native-style window controls on the right.
// - A full-window background fill underneath the
//   rounded app surface so the corner cutouts
//   never reveal the OS window's black backdrop.
// - Rounded corners only while floating; square
//   edges while maximized, matching the OS.
// ============================================

@Composable
fun FrameWindowScope.KaiteyoWindow(
    windowState: WindowState,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
    rememberWindowBounds: Boolean = true,
    captureState: String? = null
) {
    val surfaceColors = LocalSurfaceColors.current
    val density = LocalDensity.current
    var isMaximized by remember { mutableStateOf(false) }
    isMaximized = windowState.placement == WindowPlacement.Maximized

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
    // so it reopens where the user left it. Maximized/minimized geometry is
    // never saved.
    if (rememberWindowBounds) {
        LaunchedEffect(Unit) {
            var lastSavedAt = 0L
            snapshotFlow {
                Triple(windowState.placement, windowState.size, windowState.position)
            }
                .distinctUntilChanged()
                .collect { (placement, size, position) ->
                    if (placement != WindowPlacement.Floating) return@collect
                    if (position == WindowPosition.PlatformDefault) return@collect
                    val now = System.currentTimeMillis()
                    if (now - lastSavedAt < 250) return@collect
                    lastSavedAt = now
                    with(density) {
                        WindowStateStore.save(
                            SavedWindowBounds(
                                width = size.width.roundToPx(),
                                height = size.height.roundToPx(),
                                x = position.x.roundToPx(),
                                y = position.y.roundToPx()
                            )
                        )
                    }
                }
        }
    }

    val windowControls = WindowControls(
        isMaximized = isMaximized,
        onMinimize = { windowState.isMinimized = true },
        onToggleMaximize = { toggleMaximize() },
        onClose = onClose
    )

    val cornerRadius = if (isMaximized) 0.dp else 20.dp
    val surfaceShape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Fill the entire window (including what sits under the rounded
            // app surface) with the theme background — never the OS black.
            .background(surfaceColors.background)
            .onPreviewKeyEvent { keyEvent ->
                val palette = KaiteyoPalette.controller
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when {
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
            // OS border on Windows.
            if (!isMaximized) {
                WindowResizeHandles(windowState = windowState)
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
// Window Icon — the "K" mark. Native-style: a
// click opens the system menu; it never drags.
// ============================================

@Composable
private fun WindowTitleLogo(
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalKaiteyoAccent.current
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(accent.primary.copy(alpha = 0.2f))
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
        Text(
            text = "K",
            color = accent.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

// ============================================
// WINDOW CONTROLS — minimize · maximize · close
// Native-style: transparent until hover; the close
// button turns red on hover like a standard title bar.
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
            glowColor = Color(0xFFE81123),
            hoverTextColor = Color.White,
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
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor by animateColorAsState(
        targetValue = if (isHovered) glowColor.copy(alpha = 0.9f)
        else Color.Transparent,
        animationSpec = tween(120),
        label = "windowControlBg"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            isHovered && glowColor != Color.Transparent -> hoverTextColor
            isHovered -> Color(0xFFE0E0E0)
            else -> Color(0xFF8A8A8A)
        },
        animationSpec = tween(120),
        label = "windowControlColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
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
// ============================================

@Composable
private fun WindowContentFade(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(320),
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

private data class ResizeStart(val size: DpSize, val position: WindowPosition)

/**
 * Resize-drag modifier: captures the window's size/position on drag start and
 * applies the standard 8-zone resize math on every drag delta, clamped to the
 * minimum window size. The window stays in place on the anchored side while
 * the dragged edge follows the pointer.
 */
@Composable
private fun Modifier.windowResizeZone(
    zone: ResizeZone,
    windowState: WindowState,
    minWidthPx: Int,
    minHeightPx: Int,
    density: Density
): Modifier {
    var start by remember { mutableStateOf<ResizeStart?>(null) }

    return pointerHoverIcon(zone.cursor, overrideDescendants = true)
        .pointerInput(zone) {
            detectDragGestures(
                onDragStart = {
                    start = ResizeStart(windowState.size, windowState.position)
                },
                onDragEnd = { start = null },
                onDragCancel = { start = null },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val s = start ?: return@detectDragGestures
                    val startW = with(density) { s.size.width.roundToPx() }
                    val startH = with(density) { s.size.height.roundToPx() }
                    val dx = dragAmount.x.roundToInt()
                    val dy = dragAmount.y.roundToInt()

                    var x = s.position.x
                    var y = s.position.y
                    var w = startW
                    var h = startH

                    if (zone.west) {
                        w = (startW - dx).coerceAtLeast(minWidthPx)
                        x = s.position.x + (startW - w).dp
                    }
                    if (zone.east) {
                        w = (startW + dx).coerceAtLeast(minWidthPx)
                    }
                    if (zone.north) {
                        h = (startH - dy).coerceAtLeast(minHeightPx)
                        y = s.position.y + (startH - h).dp
                    }
                    if (zone.south) {
                        h = (startH + dy).coerceAtLeast(minHeightPx)
                    }

                    windowState.position = WindowPosition(x, y)
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
    minWidth: Dp = 860.dp,
    minHeight: Dp = 600.dp
) {
    val density = LocalDensity.current
    val edge = 5.dp
    val corner = 10.dp
    val minWidthPx = with(density) { minWidth.roundToPx() }
    val minHeightPx = with(density) { minHeight.roundToPx() }

    Box(Modifier.fillMaxSize()) {
        // Corner zones (10×10dp) cover the strip ends; the edge zones are
        // padded away from them, so the zones never overlap and the cursor
        // always matches the nearest resize direction.
        Box(Modifier.align(Alignment.TopStart).size(corner).windowResizeZone(ResizeZone.NorthWest, windowState, minWidthPx, minHeightPx, density))
        Box(Modifier.align(Alignment.TopEnd).size(corner).windowResizeZone(ResizeZone.NorthEast, windowState, minWidthPx, minHeightPx, density))
        Box(Modifier.align(Alignment.BottomStart).size(corner).windowResizeZone(ResizeZone.SouthWest, windowState, minWidthPx, minHeightPx, density))
        Box(Modifier.align(Alignment.BottomEnd).size(corner).windowResizeZone(ResizeZone.SouthEast, windowState, minWidthPx, minHeightPx, density))
        // Edges — inset from the corners so the corner zones win at the joints.
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(edge).padding(horizontal = corner).windowResizeZone(ResizeZone.North, windowState, minWidthPx, minHeightPx, density))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(edge).padding(horizontal = corner).windowResizeZone(ResizeZone.South, windowState, minWidthPx, minHeightPx, density))
        Box(Modifier.align(Alignment.CenterStart).fillMaxHeight().width(edge).padding(vertical = corner).windowResizeZone(ResizeZone.West, windowState, minWidthPx, minHeightPx, density))
        Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(edge).padding(vertical = corner).windowResizeZone(ResizeZone.East, windowState, minWidthPx, minHeightPx, density))
    }
}
