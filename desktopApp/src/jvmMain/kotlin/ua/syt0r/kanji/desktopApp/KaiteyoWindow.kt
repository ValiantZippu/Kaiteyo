package ua.syt0r.kanji.desktopApp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import ua.syt0r.kanji.presentation.common.nav.DesktopWindowPlacement
import ua.syt0r.kanji.presentation.common.nav.LocalWindowPlacement
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoThemeState
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SidebarPosition
import ua.syt0r.kanji.presentation.common.theme.surfaceForBaseMode
import ua.syt0r.kanji.presentation.screen.main.features.KaiteyoPalette

// ============================================
// KAITEYO WINDOW
// Custom borderless window with floating controls
// Drag region is ONLY the top 44dp — NOT the entire app
// ============================================

@Composable
fun FrameWindowScope.KaiteyoWindow(
    windowState: WindowState,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    var isMaximized by remember { mutableStateOf(false) }
    isMaximized = windowState.placement == WindowPlacement.Maximized

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColors.background)
            .clip(RoundedCornerShape(if (isMaximized) 0.dp else 20.dp))
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
                        else -> false
                    }
                } else false
            }
    ) {
        CompositionLocalProvider(
            LocalWindowPlacement provides
                if (isMaximized) DesktopWindowPlacement.Maximized
                else DesktopWindowPlacement.Floating
        ) {
            // Main application content — padded to avoid overlap with floating window controls
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 44.dp, end = 100.dp)
            ) {
                content()
            }
        }

        // Top-left logo + title
        WindowTitleRegion(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 6.dp)
        )

        // DRAG REGION — ONLY top 44dp, placed above content
        WindowDraggableArea(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            )
        }

        // FLOATING WINDOW CONTROLS — top-right
        KaiteyoFloatingControls(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-12).dp, y = 8.dp),
            isMaximized = isMaximized,
            onMinimize = { windowState.isMinimized = true },
            onMaximize = {
                windowState.placement = if (isMaximized) {
                    WindowPlacement.Floating
                } else {
                    WindowPlacement.Maximized
                }
            },
            onClose = onClose
        )
    }
}

// ============================================
// Window Title Region — top-left logo + KAITEYO
// ============================================

@Composable
private fun WindowTitleRegion(modifier: Modifier = Modifier) {
    val accent = LocalKaiteyoAccent.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(accent.primary.copy(alpha = 0.2f)),
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
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "KAITEYO",
            color = surfaceColors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp
        )
    }
}

private val surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
    @Composable
    get() = LocalSurfaceColors.current

// ============================================
// Floating Window Controls
// No background strip — buttons float above UI
// ============================================

@Composable
private fun FrameWindowScope.KaiteyoFloatingControls(
    modifier: Modifier = Modifier,
    isMaximized: Boolean,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit
) {
    val accent = LocalKaiteyoAccent.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FloatingControlButton(
            icon = "─",
            onClick = onMinimize,
            glowColor = accent.primary.copy(alpha = 0.2f)
        )
        FloatingControlButton(
            icon = if (isMaximized) "❐" else "□",
            onClick = onMaximize,
            glowColor = accent.primary.copy(alpha = 0.2f)
        )
        FloatingControlButton(
            icon = "×",
            onClick = onClose,
            glowColor = Color(0xFFFF6B6B).copy(alpha = 0.25f)
        )
    }
}

@Composable
private fun FloatingControlButton(
    icon: String,
    onClick: () -> Unit,
    glowColor: Color,
    size: Dp = 32.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor by animateColorAsState(
        targetValue = if (isHovered) glowColor else Color.Transparent,
        animationSpec = tween(200),
        label = "ctrlBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isHovered) Color.White else Color(0xFF808080),
        animationSpec = tween(200),
        label = "ctrlColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "ctrlScale"
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
                scaleY = scale,
                shadowElevation = if (isHovered) 4f else 0f,
                shape = RoundedCornerShape(8.dp),
                clip = false
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Light
        )
    }
}