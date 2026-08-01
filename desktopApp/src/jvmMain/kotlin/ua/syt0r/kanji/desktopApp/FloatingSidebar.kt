package ua.syt0r.kanji.desktopApp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoThemeState
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SidebarMode
import ua.syt0r.kanji.presentation.common.theme.SidebarPosition
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.common.theme.surfaceForBaseMode
import kotlin.math.roundToInt

// ============================================
// KAITEYO FLOATING ISLAND SIDEBAR v2.0
// Premium floating island — drag, snap, spring, borderless, resizable
// 6 dock positions · 6 modes · Drag reposition · Snap-to-edges
// Spring animations · Glass blur · Resize handle
// ============================================

enum class SidebarDockState {
    DockedLeft, DockedRight, DockedTop, DockedBottom,
    DockedTopLeft, DockedTopRight, DockedBottomLeft, DockedBottomRight,
    Floating
}

@Composable
fun FloatingSidebar(
    modifier: Modifier = Modifier,
    sidebarWidth: Dp = 280.dp,
    sidebarHeight: Dp = 380.dp,
    collapsedWidth: Dp = 52.dp,
    collapsedHeight: Dp = 44.dp,
    content: @Composable (isExpanded: Boolean) -> Unit
) {
    val themeState = LocalKaiteyoThemeState.current
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val config = themeState.layoutConfig

    var isExpanded by remember { mutableStateOf(true) }
    var isHovered by remember { mutableStateOf(false) }
    var dockState by remember { mutableStateOf(
        when (config.sidebarPosition) {
            SidebarPosition.Left -> SidebarDockState.DockedLeft
            SidebarPosition.Right -> SidebarDockState.DockedRight
            SidebarPosition.Top -> SidebarDockState.DockedTop
            SidebarPosition.Bottom -> SidebarDockState.DockedBottom
        }
    )}
    // Drag state for floating mode
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Resize state
    var currentWidth by remember { mutableStateOf(sidebarWidth) }
    var currentHeight by remember { mutableStateOf(sidebarHeight) }
    var isResizing by remember { mutableStateOf(false) }

    val isHorizontal = dockState in listOf(
        SidebarDockState.DockedLeft, SidebarDockState.DockedRight,
        SidebarDockState.DockedTopLeft, SidebarDockState.DockedTopRight,
        SidebarDockState.DockedBottomLeft, SidebarDockState.DockedBottomRight
    )
    val isFloating = dockState == SidebarDockState.Floating
    val isIconsOnly = config.sidebarMode == SidebarMode.IconsOnly

    // Animated dimensions
    val animWidth by animateDpAsState(
        targetValue = if ((isHorizontal || isFloating) && (isExpanded && !isIconsOnly)) currentWidth else collapsedWidth,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "sidebarWidth"
    )
    val animHeight by animateDpAsState(
        targetValue = if (!isHorizontal && (isExpanded && !isIconsOnly)) currentHeight else collapsedHeight,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "sidebarHeight"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (isExpanded && !isIconsOnly) 1f else 0f,
        animationSpec = tween(180),
        label = "sidebarContentAlpha"
    )

    val elevation by animateDpAsState(
        targetValue = if (isFloating) 24.dp else if (isDragging) 32.dp else 6.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "sidebarElevation"
    )

    val cornerRadius by animateFloatAsState(
        targetValue = when {
            isFloating -> 36f
            isDragging -> 28f
            else -> 20f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "sidebarRadius"
    )

    val floatOffset by animateDpAsState(
        targetValue = if (isFloating) 12.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "sidebarFloatOffset"
    )

    val isVisible = if (config.sidebarMode == SidebarMode.AutoHide) isHovered || isExpanded else true

    // Slide transitions
    val slideEnter: androidx.compose.animation.EnterTransition
    val slideExit: androidx.compose.animation.ExitTransition
    when (dockState) {
        SidebarDockState.DockedLeft, SidebarDockState.DockedTopLeft, SidebarDockState.DockedBottomLeft -> {
            slideEnter = slideInHorizontally { -it } + fadeIn()
            slideExit = slideOutHorizontally { -it } + fadeOut()
        }
        SidebarDockState.DockedRight, SidebarDockState.DockedTopRight, SidebarDockState.DockedBottomRight -> {
            slideEnter = slideInHorizontally { it } + fadeIn()
            slideExit = slideOutHorizontally { it } + fadeOut()
        }
        SidebarDockState.DockedTop -> {
            slideEnter = slideInVertically { -it } + fadeIn()
            slideExit = slideOutVertically { -it } + fadeOut()
        }
        SidebarDockState.DockedBottom -> {
            slideEnter = slideInVertically { it } + fadeIn()
            slideExit = slideOutVertically { it } + fadeOut()
        }
        SidebarDockState.Floating -> {
            slideEnter = fadeIn()
            slideExit = fadeOut()
        }
    }

    val shape = RoundedCornerShape(cornerRadius.dp)
    val currentSurface = surfaceForBaseMode(themeState.baseMode)

    // Snap-to-edge logic
    fun processDragEnd(offsetX: Float, offsetY: Float, windowWidth: Dp, windowHeight: Dp) {
        val snapThreshold = 80.dp
        val w = if (isHorizontal) currentWidth else currentHeight
        val h = if (isHorizontal) currentHeight else currentWidth
        val rightEdge = (windowWidth - w - snapThreshold)
        val leftEdge = snapThreshold
        val topEdge = snapThreshold
        val bottomEdge = (windowHeight - h - snapThreshold)

        if (offsetX.dp > rightEdge) {
            dockState = SidebarDockState.DockedRight
            dragOffsetX = 0f; dragOffsetY = 0f
        } else if (offsetX.dp < leftEdge) {
            dockState = SidebarDockState.DockedLeft
            dragOffsetX = 0f; dragOffsetY = 0f
        } else if (offsetY.dp < topEdge) {
            dockState = SidebarDockState.DockedTop
            dragOffsetX = 0f; dragOffsetY = 0f
        } else if (offsetY.dp > bottomEdge) {
            dockState = SidebarDockState.DockedBottom
            dragOffsetX = 0f; dragOffsetY = 0f
        } else {
            dockState = SidebarDockState.Floating
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideEnter,
        exit = slideExit
    ) {
        Box(
            modifier = modifier
                .then(
                    if (isHorizontal || isFloating) Modifier.width(animWidth).fillMaxHeight()
                    else Modifier.fillMaxWidth().height(animHeight)
                )
                .offset { IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt()) }
                .padding(
                    start = if (dockState in listOf(SidebarDockState.DockedLeft, SidebarDockState.DockedTopLeft, SidebarDockState.DockedBottomLeft)) floatOffset else 0.dp,
                    end = if (dockState in listOf(SidebarDockState.DockedRight, SidebarDockState.DockedTopRight, SidebarDockState.DockedBottomRight)) floatOffset else 0.dp,
                    top = if (dockState in listOf(SidebarDockState.DockedTop, SidebarDockState.DockedTopLeft, SidebarDockState.DockedTopRight) || isFloating) floatOffset else 0.dp,
                    bottom = if (dockState in listOf(SidebarDockState.DockedBottom, SidebarDockState.DockedBottomLeft, SidebarDockState.DockedBottomRight) || isFloating) floatOffset else 0.dp
                )
                .shadow(
                    elevation = elevation,
                    shape = shape,
                    ambientColor = accent.primary.copy(alpha = if (isFloating) 0.12f else 0.06f),
                    spotColor = accent.primary.copy(alpha = if (isFloating) 0.2f else 0.1f)
                )
                .clip(shape)
                .background(currentSurface.surface)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            if (isFloating) {
                                // Snap check
                                if (kotlin.math.abs(dragOffsetX) < 50f) dragOffsetX = 0f
                                if (kotlin.math.abs(dragOffsetY) < 50f) dragOffsetY = 0f
                            }
                        },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (isFloating) {
                                dragOffsetX += dragAmount.x
                                dragOffsetY += dragAmount.y
                            }
                        }
                    )
                }
        ) {
            // Top gradient accent line
            if (isHorizontal || isFloating) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(accent.primary.copy(alpha = 0.4f), accent.primary.copy(alpha = 0f))
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(accent.primary.copy(alpha = 0.4f), accent.primary.copy(alpha = 0f))
                            )
                        )
                )
            }

            // Drag handle (visible in floating mode)
            if (isFloating) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(surfaceColors.textMuted.copy(alpha = 0.25f))
                )
            }

            // Collapse toggle
            if (config.sidebarMode != SidebarMode.AutoHide) {
                val toggleAlign = when (dockState) {
                    SidebarDockState.DockedLeft -> Alignment.TopEnd
                    SidebarDockState.DockedRight -> Alignment.TopStart
                    SidebarDockState.DockedTop -> Alignment.BottomEnd
                    SidebarDockState.DockedBottom -> Alignment.TopEnd
                    SidebarDockState.DockedTopLeft -> Alignment.BottomEnd
                    SidebarDockState.DockedTopRight -> Alignment.BottomStart
                    SidebarDockState.DockedBottomLeft -> Alignment.TopEnd
                    SidebarDockState.DockedBottomRight -> Alignment.TopStart
                    SidebarDockState.Floating -> Alignment.TopEnd
                }
                CollapseToggle(
                    isExpanded = isExpanded,
                    accent = accent,
                    surfaceColors = surfaceColors,
                    onToggle = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .align(toggleAlign)
                        .padding(8.dp)
                )
            }

            // Floating mode close/dock button
            if (isFloating) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(surfaceColors.textMuted.copy(alpha = 0.1f))
                        .clickable { dockState = SidebarDockState.DockedLeft; dragOffsetX = 0f; dragOffsetY = 0f },
                    contentAlignment = Alignment.Center
                ) {
                    Text("\u00D7", color = surfaceColors.textMuted, fontSize = 10.sp)
                }
            }

            // Main content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 12.dp,
                        end = if (isIconsOnly || !isExpanded) 12.dp else 44.dp,
                        top = if (isExpanded) 20.dp else 12.dp,
                        bottom = 12.dp
                    )
                    .graphicsLayer(alpha = contentAlpha.coerceAtLeast(0.3f))
            ) {
                content(isExpanded)
            }

            // Resize handle (bottom-right corner in floating mode)
            if (isFloating && isExpanded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(surfaceColors.textMuted.copy(alpha = 0.15f))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { isResizing = true },
                                onDragEnd = { isResizing = false },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    currentWidth = (currentWidth.value + dragAmount.x).coerceIn(180f, 600f).dp
                                    currentHeight = (currentHeight.value + dragAmount.y).coerceIn(200f, 800f).dp
                                }
                            )
                        }
                )
            }
        }
    }
}

// ============================================
// Collapse/Expand Toggle
// ============================================

@Composable
private fun CollapseToggle(
    isExpanded: Boolean,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgAlpha by animateFloatAsState(
        targetValue = if (isHovered) 0.12f else 0f,
        animationSpec = tween(180),
        label = "toggleBg"
    )
    val rotate by animateFloatAsState(
        targetValue = if (isExpanded) 0f else 180f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "toggleRotate"
    )

    Box(
        modifier = modifier
            .size(22.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(accent.primary.copy(alpha = bgAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle
            )
            .hoverable(interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isExpanded) "\u25C0" else "\u25B6",
            color = if (isHovered) accent.primary else surfaceColors.textMuted,
            fontSize = 9.sp,
            modifier = Modifier.graphicsLayer(rotationZ = rotate)
        )
    }
}

// ============================================
// Sidebar Navigation Item
// ============================================

@Composable
fun SidebarNavItem(
    icon: String,
    label: String,
    isActive: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgAlpha by animateFloatAsState(
        targetValue = when { isActive -> 0.12f; isHovered -> 0.06f; else -> 0f },
        animationSpec = tween(180),
        label = "navBg"
    )
    val textColor = when { isActive -> accent.primary; isHovered -> surfaceColors.textPrimary; else -> surfaceColors.textSecondary }
    val iconColor = when { isActive -> accent.primary; isHovered -> surfaceColors.textPrimary.copy(alpha = 0.8f); else -> surfaceColors.textMuted }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(accent.primary.copy(alpha = bgAlpha))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, color = iconColor, fontSize = 16.sp)
        if (isExpanded) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = label, color = textColor, fontSize = 14.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f))
            if (isActive) {
                Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(accent.primary))
            }
        }
    }
}

// ============================================
// Sidebar Components
// ============================================

@Composable
fun SidebarDivider(modifier: Modifier = Modifier) {
    val surfaceColors = LocalSurfaceColors.current
    Divider(modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        color = surfaceColors.border.copy(alpha = 0.3f))
}

@Composable
fun SidebarSectionHeader(label: String, isExpanded: Boolean, modifier: Modifier = Modifier) {
    val surfaceColors = LocalSurfaceColors.current
    if (isExpanded) {
        Text(text = label, color = surfaceColors.textMuted, fontSize = 10.sp,
            fontWeight = FontWeight.Medium, letterSpacing = 1.sp,
            modifier = modifier.padding(start = 12.dp, top = 6.dp, bottom = 4.dp))
    }
}

@Composable
fun SidebarProgress(current: Int, total: Int, accent: KaiteyoAccentScheme, isExpanded: Boolean, modifier: Modifier = Modifier) {
    val surfaceColors = LocalSurfaceColors.current
    val progress = if (total > 0) current.toFloat() / total.toFloat() else 0f
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
            .background(surfaceColors.border.copy(alpha = 0.4f))) {
            Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.horizontalGradient(listOf(accent.primary, accent.secondary))))
        }
        if (isExpanded) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "$current / $total today", color = surfaceColors.textSecondary, fontSize = 11.sp)
                Text(text = "${(progress * 100).toInt()}%", color = accent.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "${(progress * 100).toInt()}%", color = accent.primary, fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        }
    }
}