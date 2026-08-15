package ua.syt0r.kanji.desktop.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.LauncherSnapPoint
import ua.syt0r.kanji.desktop.appstate.NavExpansion
import ua.syt0r.kanji.desktop.appstate.NavLayout
import ua.syt0r.kanji.desktop.appstate.NavPosition
import ua.syt0r.kanji.desktop.designsystem.DsCard
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.presentation.common.resources.brand.BrandMark
import kotlin.math.roundToInt

// ============================================
// NAVIGATION PREVIEW
// A live miniature of the workspace shown at the
// top of the Navigation settings. Every setting
// mirrors into the mock instantly — placement,
// mode, width, spacing, and bubble snapping.
// ============================================

@Composable
fun NavigationPreviewCard(state: AppState) {
    val sc = surfaceColors()
    var phonePreview by remember { mutableStateOf(false) }
    var bubbleSpot by remember { mutableStateOf(LauncherSnapPoint.BottomRight) }

    DsCard {
        Column(
            Modifier.padding(DsSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Navigation Preview",
                        color = sc.textPrimary,
                        fontSize = DsType.Title,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Live preview — every change applies instantly",
                        color = sc.textMuted,
                        fontSize = DsType.Caption
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
                    PreviewChip("Desktop", selected = !phonePreview) { phonePreview = false }
                    PreviewChip("Phone", selected = phonePreview) { phonePreview = true }
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .clip(RoundedCornerShape(DsRadius.Lg))
                    .background(sc.background)
                    .border(1.dp, sc.border.copy(alpha = 0.5f), RoundedCornerShape(DsRadius.Lg))
            ) {
                val mockW = maxWidth.value
                val mockH = maxHeight.value
                if (phonePreview) {
                    PhoneMock(state, bubbleSpot, mockW, mockH) { bubbleSpot = it }
                } else {
                    DesktopMock(state, bubbleSpot, mockW, mockH) { bubbleSpot = it }
                }
            }

            Text(
                text = configLine(state, phonePreview),
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
    }
}

@Composable
private fun PreviewChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val sc = surfaceColors()
    val ac = accent()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(if (selected) ac.primary.copy(alpha = 0.16f) else sc.surfaceInteractive)
            .clickable(onClick = onClick)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Xs)
    ) {
        Text(
            text = label,
            color = if (selected) ac.primary else sc.textSecondary,
            fontSize = DsType.Caption,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

private fun configLine(state: AppState, phone: Boolean): String {
    if (state.navLayout == NavLayout.Floating) {
        val where = if (phone) "phone layout" else "desktop"
        return "Floating mode · bubble in $where · snap to ${state.launcherSnapPoint.label.lowercase()} · tap the mock to test snapping"
    }
    val expansion = if (state.navExpansion == NavExpansion.Expanded) "expanded" else "compact"
    val edge = state.navPosition.label.lowercase()
    return if (phone) {
        "Phone · sidebar · $expansion · tab bar at ${state.compactNavPosition.label.lowercase()}"
    } else {
        "Sidebar · $expansion on the $edge · ${state.sidebarWidth.label.lowercase()} width"
    }
}

// ============================================
// DESKTOP MOCK — dock on any of the four edges
// ============================================

@Composable
private fun BoxScope.DesktopMock(
    state: AppState,
    bubbleSpot: LauncherSnapPoint,
    mockW: Float,
    mockH: Float,
    onBubble: (LauncherSnapPoint) -> Unit
) {
    if (state.navLayout == NavLayout.Floating) {
        BubbleMock(bubbleSpot, onBubble, topInset = 0f, bottomInset = 0f, mockW = mockW, mockH = mockH)
        return
    }
    val rail = state.navPosition == NavPosition.Left || state.navPosition == NavPosition.Right
    val expanded = state.navExpansion == NavExpansion.Expanded
    val dock = @Composable { MiniDock(vertical = rail, expanded = expanded) }
    if (rail) {
        Row(Modifier.fillMaxSize()) {
            if (state.navPosition == NavPosition.Left) {
                dock(); MiniContent()
            } else {
                MiniContent(); dock()
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            if (state.navPosition == NavPosition.Top) {
                dock(); MiniContent()
            } else {
                MiniContent(); dock()
            }
        }
    }
}

// ============================================
// PHONE MOCK — tab bar top/bottom, no desktop leak
// ============================================

@Composable
private fun BoxScope.PhoneMock(
    state: AppState,
    bubbleSpot: LauncherSnapPoint,
    mockW: Float,
    mockH: Float,
    onBubble: (LauncherSnapPoint) -> Unit
) {
    if (state.navLayout == NavLayout.Floating) {
        BubbleMock(bubbleSpot, onBubble, topInset = 34f, bottomInset = 34f, mockW = mockW, mockH = mockH)
        return
    }
    Column(Modifier.fillMaxSize()) {
        if (state.compactNavPosition == NavPosition.Top) MiniTabBar()
        MiniContent()
        if (state.compactNavPosition != NavPosition.Top) MiniTabBar()
    }
}

// ============================================
// BUBBLE MOCK — snap anchors + click-to-snap
// ============================================

@Composable
private fun BoxScope.BubbleMock(
    bubbleSpot: LauncherSnapPoint,
    onBubble: (LauncherSnapPoint) -> Unit,
    topInset: Float,
    bottomInset: Float,
    mockW: Float,
    mockH: Float
) {
    val sc = surfaceColors()
    val ac = accent()

    // Click anywhere → the bubble snaps to the nearest anchor (like the real launcher).
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(bubbleSpot) {
                detectTapGestures { tap ->
                    onBubble(nearestPreviewAnchor(tap, mockW, mockH, topInset, bottomInset))
                }
            }
    ) {
        // Snap anchor dots — all 12, three per screen edge.
        LauncherSnapPoint.entries.forEach { anchor ->
            val p = previewAnchor(anchor, mockW, mockH, topInset, bottomInset, 18f)
            Box(
                Modifier
                    .offset { IntOffset(p.x.roundToInt(), p.y.roundToInt()) }
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(sc.border.copy(alpha = 0.7f))
            )
        }
        // The bubble itself.
        val pos = previewAnchor(bubbleSpot, mockW, mockH, topInset, bottomInset, 18f)
        Box(
            Modifier
                .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                .size(18.dp)
                .clip(CircleShape)
                .background(ac.primary)
                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // The launcher bubble carries the real Kaiteyo mark —
            // centralized brand asset, not a "K".
            BrandMark(modifier = Modifier.size(16.dp), contentDescription = null)
        }
    }
}

/** Top-left position (px) of a bubble of [sizePx] for an anchor inside a [w]×[h] mock. */
private fun previewAnchor(
    anchor: LauncherSnapPoint,
    w: Float,
    h: Float,
    topInset: Float,
    bottomInset: Float,
    sizePx: Float
): Offset {
    return when (anchor) {
        LauncherSnapPoint.TopLeft, LauncherSnapPoint.LeftTop -> Offset(0f, topInset)
        LauncherSnapPoint.TopCenter -> Offset((w - sizePx) / 2f, topInset)
        LauncherSnapPoint.TopRight, LauncherSnapPoint.RightTop -> Offset(w - sizePx, topInset)
        LauncherSnapPoint.BottomLeft, LauncherSnapPoint.LeftBottom -> Offset(0f, h - bottomInset - sizePx)
        LauncherSnapPoint.BottomCenter -> Offset((w - sizePx) / 2f, h - bottomInset - sizePx)
        LauncherSnapPoint.BottomRight, LauncherSnapPoint.RightBottom -> Offset(w - sizePx, h - bottomInset - sizePx)
        LauncherSnapPoint.LeftCenter -> Offset(0f, (h - sizePx) / 2f)
        LauncherSnapPoint.RightCenter -> Offset(w - sizePx, (h - sizePx) / 2f)
    }
}

/** Nearest anchor to a tap point — mirrors the real launcher's snapping. */
private fun nearestPreviewAnchor(
    tap: Offset,
    w: Float,
    h: Float,
    topInset: Float,
    bottomInset: Float
): LauncherSnapPoint {
    return LauncherSnapPoint.entries.minByOrNull { anchor ->
        val c = previewAnchor(anchor, w, h, topInset, bottomInset, 0f)
        val dx = tap.x - c.x
        val dy = tap.y - c.y
        dx * dx + dy * dy
    } ?: LauncherSnapPoint.BottomRight
}

// ============================================
// MINI COMPONENTS
// ============================================

@Composable
private fun MiniDock(vertical: Boolean, expanded: Boolean) {
    val sc = surfaceColors()
    if (vertical) {
        Column(
            modifier = Modifier
                .width(if (expanded) 92.dp else 40.dp)
                .fillMaxHeight()
                .background(sc.surface)
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniDot()
            MiniDot()
            MiniItem(expanded)
            MiniItem(expanded)
            MiniItem(expanded)
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (expanded) 42.dp else 32.dp)
                .background(sc.surface)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniDot()
            MiniDot()
            MiniItem(expanded)
            MiniItem(expanded)
            MiniItem(expanded)
        }
    }
}

@Composable
private fun MiniItem(expanded: Boolean) {
    val sc = surfaceColors()
    val ac = accent()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(ac.primary.copy(alpha = 0.75f))
        )
        if (expanded) {
            Box(
                Modifier
                    .width(42.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(sc.surfaceInteractive)
            )
        }
    }
}

@Composable
private fun MiniDot() {
    Box(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(surfaceColors().surfaceInteractive)
    )
}

@Composable
private fun MiniTabBar() {
    val sc = surfaceColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(sc.surface)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        repeat(4) { MiniDot() }
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(sc.surfaceInteractive)
        )
    }
}

@Composable
private fun MiniContent() {
    val sc = surfaceColors()
    Column(
        Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.35f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(sc.surfaceInteractive)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(sc.surfaceElevated)
        )
        Box(
            Modifier
                .fillMaxWidth(0.7f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(sc.surfaceInteractive)
        )
        Box(
            Modifier
                .fillMaxWidth(0.5f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(sc.surfaceInteractive)
        )
    }
}
