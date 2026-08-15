package ua.syt0r.kanji.presentation.common.nav

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow as materialShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SidebarPosition

// ============================================
// NAVIGATION LIVE PREVIEW
// Miniature application mockup that mirrors the
// real navigation settings in real time: mode,
// edge placement, widths, bubble snap points.
// Every change animates immediately.
// ============================================

private val PreviewWidth = 300.dp
private val PreviewHeight = 190.dp

@Composable
fun NavigationPreview(
    settings: NavigationSettings,
    formFactor: FormFactor,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val animations = settings.animationsEnabled && !settings.accessibility.reducedMotion
    val edge = settings.edgeFor(formFactor)
    val mode = settings.mode

    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(PreviewWidth, PreviewHeight)
                // Shadow before clip/background so it renders behind the mock.
                .materialShadow(10.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(surfaceColors.surface)
                .border(
                    width = 1.dp,
                    color = surfaceColors.border.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            if (mode == NavigationMode.Floating) {
                PreviewContent(edge = null)
                PreviewBubble(
                    settings = settings,
                    formFactor = formFactor,
                    animations = animations,
                    accent = accent.primary,
                    surfaceColors = surfaceColors
                )
            } else {
                val expanded = settings.expansionFor(formFactor) == SidebarExpansion.Expanded
                val vertical = edge == SidebarPosition.Left || edge == SidebarPosition.Right
                PreviewContent(edge = edge)
                PreviewDockedNavigation(
                    edge = edge,
                    vertical = vertical,
                    expanded = expanded,
                    expandedWidth = settings.sidebar.expandedWidth,
                    iconSize = settings.sidebar.iconSize,
                    animations = animations,
                    surfaceColors = surfaceColors,
                    accent = accent.primary
                )
            }
        }
    }
}

// ============================================
// FAKE CONTENT
// ============================================

@Composable
private fun PreviewContent(edge: SidebarPosition?) {
    val surfaceColors = LocalSurfaceColors.current
    val contentColor = surfaceColors.surfaceInteractive.copy(alpha = 0.55f)

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            // Fake top bar line
            Box(
                Modifier
                    .fillMaxWidth(0.45f)
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(contentColor)
            )
            Spacer(Modifier.height(4.dp))
            // Fake stat cards
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(3) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(26.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(surfaceColors.surfaceInteractive.copy(alpha = 0.35f))
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            // Fake list rows
            repeat(4) { index ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(surfaceColors.border.copy(alpha = 0.5f))
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(if (index % 2 == 0) 0.72f else 0.58f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(contentColor)
                    )
                }
            }
        }
        // Active item indicator in the content area
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(LocalKaiteyoAccent.current.primary.copy(alpha = 0.8f))
        )
    }
}

// ============================================
// DOCKED NAVIGATION (expanded / compact)
// ============================================

@Composable
private fun PreviewDockedNavigation(
    edge: SidebarPosition,
    vertical: Boolean,
    expanded: Boolean,
    expandedWidth: Int,
    iconSize: Int,
    animations: Boolean,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    accent: Color
) {
    val railWidth = if (expanded) (expandedWidth / 5).dp else 40.dp
    val railHeight = if (expanded) 44.dp else 30.dp

    val animatedWidth by animateDpAsState(
        targetValue = railWidth,
        animationSpec = if (animations) spring(dampingRatio = 0.75f, stiffness = 320f) else tween(0),
        label = "previewRailWidth"
    )
    val animatedHeight by animateDpAsState(
        targetValue = railHeight,
        animationSpec = if (animations) spring(dampingRatio = 0.75f, stiffness = 320f) else tween(0),
        label = "previewRailHeight"
    )

    val shape = RoundedCornerShape(if (vertical) 10.dp else 8.dp)
    val navBackground = surfaceColors.surfaceElevated.copy(alpha = 0.9f)

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(
                    when (edge) {
                        SidebarPosition.Left -> Alignment.CenterStart
                        SidebarPosition.Right -> Alignment.CenterEnd
                        SidebarPosition.Top -> Alignment.TopCenter
                        else -> Alignment.BottomCenter
                    }
                )
                .then(
                    if (vertical) {
                        Modifier
                            .fillMaxHeight(0.82f)
                            .width(animatedWidth)
                    } else {
                        Modifier
                            .fillMaxWidth(0.9f)
                            .height(animatedHeight)
                    }
                )
                .padding(if (vertical) 4.dp else 2.dp)
                .clip(shape)
                .background(navBackground)
                .border(1.dp, surfaceColors.border.copy(alpha = 0.35f), shape)
        ) {
            if (vertical) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(5) { index ->
                        val selected = index == 0
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                Modifier
                                    .size((iconSize / 2.5f).dp)
                                    .clip(CircleShape)
                                    .background(if (selected) accent.copy(alpha = 0.85f) else surfaceColors.border.copy(alpha = 0.6f))
                            )
                            if (expanded) {
                                Box(
                                    Modifier
                                        .width(if (index == 0) 22.dp else 16.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            if (selected) accent.copy(alpha = 0.7f)
                                            else surfaceColors.border.copy(alpha = 0.45f)
                                        )
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) { index ->
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == 0) accent.copy(alpha = 0.85f)
                                    else surfaceColors.border.copy(alpha = 0.6f)
                                )
                        )
                    }
                    if (expanded) {
                        Spacer(Modifier.width(2.dp))
                        Box(
                            Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(surfaceColors.border.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// BUBBLE + SNAP POINTS
// ============================================

@Composable
private fun PreviewBubble(
    settings: NavigationSettings,
    formFactor: FormFactor,
    animations: Boolean,
    accent: Color,
    surfaceColors: ua.syt0r.kanji.presentation.common.theme.SurfaceColors
) {
    val snap = settings.snapPointFor(formFactor)
    val bubbleSize = 30.dp
    val bubbleRadius = bubbleSize / 2

    val target = anchorPositionInPreview(snap, bubbleRadius)

    val animatedX by animateDpAsState(
        targetValue = target.first,
        animationSpec = if (animations) spring(dampingRatio = 0.6f, stiffness = 240f) else tween(0),
        label = "bubbleX"
    )
    val animatedY by animateDpAsState(
        targetValue = target.second,
        animationSpec = if (animations) spring(dampingRatio = 0.6f, stiffness = 240f) else tween(0),
        label = "bubbleY"
    )

    Box(Modifier.fillMaxSize()) {
        // All 12 snap point markers, arranged as the screen edges.
        BubbleSnapPoint.PickerOrder.forEach { entry ->
            val pos = anchorPositionInPreview(entry, bubbleRadius)
            val active = entry == snap
            val markerColor by animateColorAsState(
                targetValue = if (active) accent.copy(alpha = 0.85f)
                else surfaceColors.border.copy(alpha = 0.5f),
                animationSpec = tween(if (animations) 180 else 0),
                label = "marker"
            )
            Box(
                modifier = Modifier
                    .offset(x = pos.first - 3.dp, y = pos.second - 3.dp)
                    .size(if (active) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(markerColor)
            )
        }

        // The bubble itself — animated position demonstrates live snapping.
        Box(
            modifier = Modifier
                .offset(x = animatedX - bubbleRadius, y = animatedY - bubbleRadius)
                .size(bubbleSize)
                .materialShadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(accent)
                .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
        )
    }
}

private fun anchorPositionInPreview(snap: BubbleSnapPoint, radius: Dp): Pair<Dp, Dp> {
    val margin = 12.dp
    val cx = PreviewWidth / 2
    val cy = PreviewHeight / 2
    val top = margin + radius
    val bottom = PreviewHeight - margin - radius
    val left = margin + radius
    val right = PreviewWidth - margin - radius
    return when (snap) {
        BubbleSnapPoint.TopLeft, BubbleSnapPoint.LeftTop -> left to top
        BubbleSnapPoint.TopCenter -> cx to top
        BubbleSnapPoint.TopRight, BubbleSnapPoint.RightTop -> right to top
        BubbleSnapPoint.BottomLeft, BubbleSnapPoint.LeftBottom -> left to bottom
        BubbleSnapPoint.BottomCenter -> cx to bottom
        BubbleSnapPoint.BottomRight, BubbleSnapPoint.RightBottom -> right to bottom
        BubbleSnapPoint.LeftCenter -> left to cy
        BubbleSnapPoint.RightCenter -> right to cy
    }
}
