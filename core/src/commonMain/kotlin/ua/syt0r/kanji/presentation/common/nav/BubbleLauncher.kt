package ua.syt0r.kanji.presentation.common.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow as materialShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalRadiusConfig
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.HomeNavigationState

// ============================================
// BUBBLE LAUNCHER
// Floating launcher for Bubble mode. Draggable
// with spring snapping to edges/corners.
// On hover it smoothly expands a quick-control
// rail: [Full Sidebar] [Compact Sidebar]
// [Launchpad]. Auto-fades when idle.
// ============================================

@Composable
fun BubbleLauncher(
    navigationState: MainNavigationState,
    homeNavState: HomeNavigationState,
    navSettings: NavigationSettingsState,
    formFactor: FormFactor,
    sections: List<NavSection>,
    modifier: Modifier = Modifier
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val accessibility = navSettings.settings.accessibility
    val bubbleSettings = navSettings.settings.bubble

    // The launchpad leads with a curated quick-access grid followed by the
    // full section tree (home tabs, features, system).
    val launchpadSections = listOf(buildQuickAccessSection(navigationState, homeNavState)) + sections

    val bubbleSize = accessibility.scaledHitbox(bubbleSettings.size).dp
    val iconSize = accessibility.scaledIconSize(bubbleSettings.iconSize).dp
    val bubbleDiameterPx = with(density) { bubbleSize.roundToPx() }
    val hitboxPadding = if (accessibility.largerHitboxes) 16.dp else 10.dp

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var launchpadOpen by remember { mutableStateOf(false) }
    var faded by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isBubbleHovered by interactionSource.collectIsHoveredAsState()
    val bubbleOffset = remember {
        Animatable(Offset.Zero, Offset.VectorConverter)
    }
    val scope = rememberCoroutineScope()

    val anchor = navSettings.settings.bubbleAnchorFor(formFactor)
    val (persistedOffsetX, persistedOffsetY) = navSettings.settings.bubbleOffsetFor(formFactor)
    val animations = navSettings.settings.animationsEnabled && !accessibility.reducedMotion
    val speed = bubbleSettings.animationSpeed.coerceAtLeast(0.25f)

    val anchorPositionFor: (BubbleAnchor) -> Offset = { entry ->
        with(density) {
            val size = bubbleDiameterPx
            val margin = BubbleEdgeMargin.roundToPx()
            when (entry) {
                BubbleAnchor.Left -> Offset(margin.toFloat(), (containerSize.height - size) / 2f)
                BubbleAnchor.Right -> Offset((containerSize.width - size - margin).toFloat(), (containerSize.height - size) / 2f)
                BubbleAnchor.TopLeft -> Offset(margin.toFloat(), margin.toFloat())
                BubbleAnchor.TopRight -> Offset((containerSize.width - size - margin).toFloat(), margin.toFloat())
                BubbleAnchor.BottomLeft -> Offset(margin.toFloat(), (containerSize.height - size - margin).toFloat())
                BubbleAnchor.BottomRight -> Offset((containerSize.width - size - margin).toFloat(), (containerSize.height - size - margin).toFloat())
            }
        }
    }

    val allowedAnchors = remember(formFactor) {
        if (formFactor.isPhone) {
            listOf(BubbleAnchor.TopLeft, BubbleAnchor.TopRight, BubbleAnchor.BottomLeft, BubbleAnchor.BottomRight)
        } else {
            BubbleAnchor.entries
        }
    }

    // Position the bubble at its persisted anchor/offset (no animation on setup).
    LaunchedEffect(containerSize, anchor, persistedOffsetX, persistedOffsetY, bubbleDiameterPx) {
        if (containerSize == IntSize.Zero) return@LaunchedEffect
        val p = anchorPositionFor(anchor)
        bubbleOffset.snapTo(
            Offset(
                p.x + with(density) { persistedOffsetX.dp.roundToPx() },
                p.y + with(density) { persistedOffsetY.dp.roundToPx() }
            )
        )
    }

    fun persistPosition(entry: BubbleAnchor, bubbleTopLeft: Offset) {
        val anchored = anchorPositionFor(entry)
        val newOffsetX = with(density) { ((bubbleTopLeft.x - anchored.x) / density.density).toInt() }
        val newOffsetY = with(density) { ((bubbleTopLeft.y - anchored.y) / density.density).toInt() }
        navSettings.update { current ->
            if (formFactor.isPhone) {
                current.copy(
                    phone = current.phone.copy(
                        bubbleAnchor = entry,
                        bubbleOffsetX = newOffsetX,
                        bubbleOffsetY = newOffsetY
                    )
                )
            } else {
                current.copy(
                    bubbleAnchor = entry,
                    bubbleOffsetX = newOffsetX,
                    bubbleOffsetY = newOffsetY
                )
            }
        }
    }

    fun snapAndPersist(bubbleTopLeft: Offset) {
        val anchorPos = anchorPositionFor(anchor)
        val driftX = bubbleTopLeft.x - anchorPos.x
        val driftY = bubbleTopLeft.y - anchorPos.y
        val nearest = allowedAnchors.minByOrNull { entry ->
            val pos = anchorPositionFor(entry)
            val dx = bubbleTopLeft.x - pos.x
            val dy = bubbleTopLeft.y - pos.y
            dx * dx + dy * dy
        } ?: anchor
        val nearestPos = anchorPositionFor(nearest)
        val snapThresholdPx = with(density) { bubbleSettings.snapSensitivity.dp.roundToPx() }
        val dx = bubbleTopLeft.x - nearestPos.x
        val dy = bubbleTopLeft.y - nearestPos.y
        val withinThreshold = (dx * dx + dy * dy) <= (snapThresholdPx * snapThresholdPx)

        if (withinThreshold) {
            // Magnetize exactly onto the anchor for a clean snap.
            persistPosition(nearest, nearestPos)
            if (animations) {
                scope.launch {
                    bubbleOffset.animateTo(nearestPos, spring(dampingRatio = 0.62f, stiffness = 320f * speed))
                }
            } else {
                scope.launch { bubbleOffset.snapTo(nearestPos) }
            }
        } else {
            // Keep the bubble where dropped; anchor it to the nearest snap point.
            val target = bubbleTopLeft
            persistPosition(nearest, target)
            if (animations) {
                scope.launch {
                    bubbleOffset.animateTo(target, spring(dampingRatio = 0.7f, stiffness = 220f * speed))
                }
            } else {
                scope.launch { bubbleOffset.snapTo(target) }
            }
        }
    }

    LaunchedEffect(isBubbleHovered, launchpadOpen, bubbleSettings.autoFade) {
        if (!bubbleSettings.autoFade || isBubbleHovered || launchpadOpen) {
            faded = false
        } else {
            delay(bubbleSettings.fadeDelayMs)
            faded = true
        }
    }

    // Fade the launcher out entirely while the launchpad is open so it never
    // peeks through the translucent glass panel behind the dim scrim — most
    // noticeable when the bubble is anchored at a bottom corner.
    val targetAlpha = when {
        launchpadOpen -> 0f
        faded -> bubbleSettings.fadeOpacity
        else -> 1f
    }
    val opacity by animateFloatAsState(
        targetValue = targetAlpha,
        // Honor the animations / reduced-motion setting like the rest of the
        // nav system: snap instead of tween when animations are disabled.
        animationSpec = if (animations) tween(180) else snap(),
        label = "bubbleAlpha"
    )
    val position = bubbleOffset.value
    val hitboxPaddingPx = with(density) { hitboxPadding.roundToPx() }

    // Determine which side the quick-control rail expands toward.
    val expandToRight = position.x + bubbleDiameterPx < containerSize.width / 2f

    Box(modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .onSizeChanged { containerSize = it }
                .offset {
                    IntOffset(
                        (position.x - hitboxPaddingPx).roundToInt(),
                        (position.y - hitboxPaddingPx).roundToInt()
                    )
                }
                .size(bubbleSize + hitboxPadding * 2)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { launchpadOpen = !launchpadOpen }
                )
                .hoverable(interactionSource)
                .pointerInput(bubbleDiameterPx, bubbleSettings.snapSensitivity) {
                    detectDragGestures(
                        onDragStart = {
                            scope.launch { bubbleOffset.stop() }
                            launchpadOpen = false
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            val current = bubbleOffset.value
                            val next = Offset(
                                (current.x + amount.x).coerceIn(0f, (containerSize.width - bubbleDiameterPx).toFloat()),
                                (current.y + amount.y).coerceIn(0f, (containerSize.height - bubbleDiameterPx).toFloat())
                            )
                            scope.launch { bubbleOffset.snapTo(next) }
                        },
                        onDragEnd = { snapAndPersist(bubbleOffset.value) }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .size(bubbleSize)
                    .align(Alignment.Center)
                    .alpha(opacity)
                    .clip(CircleShape)
                    .background(accent.primary)
                    .materialShadow(12.dp, CircleShape)
                    .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.GridView,
                    contentDescription = resolveString { nav.modeBubbleTooltip },
                    tint = surfaceColors.surface,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        // Quick-control rail revealed on hover.
        val pillPx = with(density) { 44.dp.roundToPx() }
        AnimatedVisibility(
            visible = isBubbleHovered && !launchpadOpen,
            modifier = Modifier.offset {
                val centerY = position.y + bubbleDiameterPx / 2f
                IntOffset(
                    if (expandToRight) {
                        (position.x + bubbleDiameterPx + hitboxPaddingPx * 2).roundToInt()
                    } else {
                        (position.x - 160 - hitboxPaddingPx).roundToInt()
                    },
                    (centerY - pillPx / 2).roundToInt()
                )
            },
            // Same fade treatment as the bubble: synced duration, and snapped
            // when animations are disabled (reduced motion).
            enter = fadeIn(if (animations) tween(180) else snap()) + expandHorizontally(
                expandFrom = if (expandToRight) Alignment.Start else Alignment.End,
                animationSpec = if (animations) tween(180) else snap()
            ),
            exit = fadeOut(if (animations) tween(180) else snap()) + shrinkHorizontally(
                shrinkTowards = if (expandToRight) Alignment.Start else Alignment.End,
                animationSpec = if (animations) tween(180) else snap()
            )
        ) {
            BubbleQuickControlRail(
                current = navSettings.settings.mode,
                onSetMode = { mode ->
                    navSettings.setMode(mode)
                    launchpadOpen = false
                },
                onOpenLaunchpad = { launchpadOpen = true }
            )
        }
    }

    if (launchpadOpen) {
        Launchpad(
            sections = launchpadSections,
            navigationState = navigationState,
            homeNavState = homeNavState,
            visible = launchpadOpen,
            onClose = { launchpadOpen = false }
        )
    }
}

// ============================================
// QUICK-CONTROL RAIL
// ============================================

@Composable
private fun BubbleQuickControlRail(
    current: NavigationMode,
    onSetMode: (NavigationMode) -> Unit,
    onOpenLaunchpad: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val shape = RoundedCornerShape(scaledRadius(Dimens.RadiusLg))

    Row(
        modifier = Modifier
            .size(width = 160.dp, height = 44.dp)
            .clip(shape)
            .background(surfaceColors.surfaceElevated.copy(alpha = 0.96f))
            .border(1.dp, surfaceColors.border.copy(alpha = 0.4f), shape)
            .materialShadow(14.dp, shape)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RailButton(
            icon = Icons.AutoMirrored.Filled.ViewSidebar,
            label = resolveString { nav.modeExpandedTooltip },
            selected = current == NavigationMode.Expanded,
            onClick = { onSetMode(NavigationMode.Expanded) }
        )
        RailButton(
            icon = Icons.Default.ViewModule,
            label = resolveString { nav.modeCompactTooltip },
            selected = current == NavigationMode.Compact,
            onClick = { onSetMode(NavigationMode.Compact) }
        )
        Box(Modifier.size(1.dp, 20.dp).background(surfaceColors.border.copy(alpha = 0.35f)))
        RailButton(
            icon = Icons.Default.Apps,
            label = "Launchpad",
            selected = false,
            accentTint = accent.primary,
            onClick = onOpenLaunchpad
        )
    }
}

@Composable
private fun RailButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentTint: Color? = null
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val tint = accentTint ?: if (selected) accent.primary else surfaceColors.textSecondary

    val bgColor by animateColorAsState(
        targetValue = when {
            selected -> accent.primary.copy(alpha = 0.16f)
            isHovered -> surfaceColors.surfaceInteractive
            else -> Color.Transparent
        },
        animationSpec = spring(dampingRatio = 0.8f),
        label = "railBg"
    )

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusSm)))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun scaledRadius(base: Dp): Dp {
    val multiplier = LocalRadiusConfig.current.style.globalMultiplier
    return base * multiplier
}