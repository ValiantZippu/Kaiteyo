package ua.syt0r.kanji.presentation.common.nav

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalRadiusConfig
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.HomeNavigationState

// ============================================
// BUBBLE LAUNCHER — Floating mode
// A draggable launcher bubble that magnetizes to
// the nearest of the 12 snap points when released.
//   · Tap / Enter        → launchpad (expands from the bubble)
//   · Hold / right-click → mode switch panel (Floating ↔ Sidebar)
//   · Drag               → free movement, spring snap on release
// Auto-fades when idle; position + snap point persist.
// ============================================

/** How long a press must be held before the mode panel opens (ms). */
private const val LongPressTimeoutMs = 480L

@Composable
fun BubbleLauncher(
    navigationState: MainNavigationState,
    homeNavState: HomeNavigationState,
    navSettings: NavigationSettingsState,
    formFactor: FormFactor,
    sections: List<NavSection>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val accessibility = navSettings.settings.accessibility
    val bubbleSettings = navSettings.settings.bubble

    // The launchpad leads with a curated quick-access grid followed by the
    // full section tree (home tabs, features, system).
    val launchpadSections = listOf(buildQuickAccessSection(navigationState, homeNavState)) + sections
    val currentIcon = launchpadSections
        .flatMap { it.entries }
        .firstOrNull { it.selected }
        ?.icon ?: Icons.Default.GridView

    val bubbleSize = accessibility.scaledHitbox(bubbleSettings.size).dp
    val iconSize = accessibility.scaledIconSize(bubbleSettings.iconSize).dp
    val bubbleDiameterPx = with(density) { bubbleSize.roundToPx() }
    val hitboxPadding = if (accessibility.largerHitboxes) 16.dp else 10.dp
    val hitboxPaddingPx = with(density) { hitboxPadding.roundToPx() }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var launchpadOpen by remember { mutableStateOf(false) }
    var modePanelOpen by remember { mutableStateOf(false) }
    var faded by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var bubbleCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val interactionSource = remember { MutableInteractionSource() }
    val isBubbleHovered by interactionSource.collectIsHoveredAsState()
    val bubbleOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()
    val bubbleFocusRequester = remember { FocusRequester() }

    val snapPoint = navSettings.settings.snapPointFor(formFactor)
    val (persistedOffsetX, persistedOffsetY) = navSettings.settings.snapOffsetFor(formFactor)
    val animations = navSettings.settings.animationsEnabled && !accessibility.reducedMotion
    val speed = bubbleSettings.animationSpeed.coerceAtLeast(0.25f)

    // Phones: keep the bubble clear of status bar / notch, system gesture
    // areas and the soft keyboard. Desktop/tablet report zero insets.
    val topInsetPx = if (formFactor.isPhone) WindowInsets.statusBars.getTop(density) else 0
    val bottomInsetPx = if (formFactor.isPhone) {
        max(WindowInsets.navigationBars.getBottom(density), WindowInsets.ime.getBottom(density))
    } else 0
    val edgeMarginPx = with(density) { BubbleEdgeMargin.roundToPx() }
    val topMarginPx = max(edgeMarginPx, topInsetPx)
    val bottomMarginPx = max(edgeMarginPx, bottomInsetPx)

    val snapPositionFor: (BubbleSnapPoint) -> Offset = { snap ->
        val size = bubbleDiameterPx.toFloat()
        val w = containerSize.width.toFloat()
        val h = containerSize.height.toFloat()
        val top = topMarginPx.toFloat()
        val bottom = bottomMarginPx.toFloat()
        val side = edgeMarginPx.toFloat()
        when (snap) {
            BubbleSnapPoint.TopLeft, BubbleSnapPoint.LeftTop -> Offset(side, top)
            BubbleSnapPoint.TopCenter -> Offset((w - size) / 2f, top)
            BubbleSnapPoint.TopRight, BubbleSnapPoint.RightTop -> Offset(w - size - side, top)
            BubbleSnapPoint.BottomLeft, BubbleSnapPoint.LeftBottom -> Offset(side, h - size - bottom)
            BubbleSnapPoint.BottomCenter -> Offset((w - size) / 2f, h - size - bottom)
            BubbleSnapPoint.BottomRight, BubbleSnapPoint.RightBottom -> Offset(w - size - side, h - size - bottom)
            BubbleSnapPoint.LeftCenter -> Offset(side, (h - size) / 2f)
            BubbleSnapPoint.RightCenter -> Offset(w - size - side, (h - size) / 2f)
        }
    }

    // Position the bubble at its persisted snap point (no animation on setup).
    // If the window shrank so the persisted spot would be off-screen, fall
    // back to the clean snap anchor.
    LaunchedEffect(containerSize, snapPoint, persistedOffsetX, persistedOffsetY, bubbleDiameterPx) {
        if (containerSize == IntSize.Zero) return@LaunchedEffect
        val anchorPos = snapPositionFor(snapPoint)
        val drift = Offset(
            with(density) { persistedOffsetX.dp.roundToPx() }.toFloat(),
            with(density) { persistedOffsetY.dp.roundToPx() }.toFloat()
        )
        val target = anchorPos + drift
        val inBounds = target.x >= 0f && target.x <= (containerSize.width - bubbleDiameterPx).toFloat() &&
            target.y >= 0f && target.y <= (containerSize.height - bubbleDiameterPx).toFloat()
        bubbleOffset.snapTo(if (inBounds) target else anchorPos)
    }

    fun nearestSnap(bubbleTopLeft: Offset): BubbleSnapPoint =
        BubbleSnapPoint.entries.minByOrNull { snap ->
            val pos = snapPositionFor(snap)
            val dx = bubbleTopLeft.x - pos.x
            val dy = bubbleTopLeft.y - pos.y
            dx * dx + dy * dy
        } ?: snapPoint

    /**
     * True when the bubble is close enough to [anchor] to magnetize on
     * release — honors the user's snap-distance preference.
     */
    fun withinSnapDistance(bubbleTopLeft: Offset, anchor: Offset): Boolean {
        val dx = bubbleTopLeft.x - anchor.x
        val dy = bubbleTopLeft.y - anchor.y
        val distancePx = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        val maxDistancePx = with(density) { bubbleSettings.snapDistance.dp.roundToPx().toFloat() }
        return distancePx <= maxDistancePx
    }

    fun persistPosition(snap: BubbleSnapPoint, bubbleTopLeft: Offset) {
        val anchor = snapPositionFor(snap)
        val offsetX = with(density) { ((bubbleTopLeft.x - anchor.x) / density.density).toInt() }
        val offsetY = with(density) { ((bubbleTopLeft.y - anchor.y) / density.density).toInt() }
        navSettings.update { current ->
            if (formFactor.isPhone) {
                current.copy(
                    phone = current.phone.copy(
                        snapPoint = snap,
                        snapOffsetX = offsetX,
                        snapOffsetY = offsetY
                    )
                )
            } else {
                current.copy(snapPoint = snap, snapOffsetX = offsetX, snapOffsetY = offsetY)
            }
        }
    }

    fun snapAndPersist(bubbleTopLeft: Offset) {
        val nearest = nearestSnap(bubbleTopLeft)
        val anchorPos = snapPositionFor(nearest)
        if (withinSnapDistance(bubbleTopLeft, anchorPos)) {
            // Magnetize to the anchor and remember the snap point.
            persistPosition(nearest, bubbleTopLeft)
            if (animations) {
                scope.launch {
                    bubbleOffset.animateTo(anchorPos, spring(dampingRatio = 0.62f, stiffness = 340f * speed))
                }
            } else {
                scope.launch { bubbleOffset.snapTo(anchorPos) }
            }
        } else {
            // Beyond snap distance: keep the free drop position (persisted as
            // a micro-offset from the nearest anchor so it survives restarts).
            persistPosition(nearest, bubbleTopLeft)
        }
    }

    // Auto-fade when idle (paused while any surface is open or hovered).
    // Hover reveal: hovering the (faded) bubble brings it back instantly.
    LaunchedEffect(isBubbleHovered, launchpadOpen, modePanelOpen, bubbleSettings.autoFade, bubbleSettings.idleTimeoutMs, bubbleSettings.hoverReveal) {
        if (!bubbleSettings.autoFade || isBubbleHovered || launchpadOpen || modePanelOpen) {
            faded = false
        } else {
            delay(bubbleSettings.idleTimeoutMs)
            faded = true
        }
    }
    // When hover-reveal is enabled and the bubble has faded, hovering the
    // (invisible) area instantly restores full opacity.
    LaunchedEffect(isBubbleHovered, faded, bubbleSettings.hoverReveal) {
        if (faded && isBubbleHovered && bubbleSettings.hoverReveal) {
            faded = false
        }
    }

    val targetAlpha = when {
        launchpadOpen || modePanelOpen -> 0f
        faded -> bubbleSettings.fadeOpacity
        else -> 1f
    }
    val opacity by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = if (animations) tween(180) else snap(),
        label = "bubbleAlpha"
    )

    // Hover / press response on the bubble glyph itself.
    val bubbleInteraction = remember { MutableInteractionSource() }
    val bubbleHovered by bubbleInteraction.collectIsHoveredAsState()
    val hoverScale by animateFloatAsState(
        targetValue = if (launchpadOpen || modePanelOpen) 1f else if (bubbleHovered) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 420f),
        label = "bubbleHoverScale"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (dragging) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "bubblePressScale"
    )

    // Bubble geometry helper for the mode panel popup.
    fun bubbleWindowRect(): IntOffset? {
        val coords = bubbleCoords ?: return null
        val pos = coords.positionInWindow()
        return IntOffset(pos.x.roundToInt(), pos.y.roundToInt())
    }

    val bubbleTopLeft = bubbleOffset.value
    val position = bubbleOffset.value

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
                .hoverable(interactionSource)
                .onGloballyPositioned { if (bubbleCoords != it) bubbleCoords = it }
                .focusRequester(bubbleFocusRequester)
                .focusable()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when {
                        keyEvent.key == Key.Enter || keyEvent.key == Key.Spacebar -> {
                            launchpadOpen = true
                            true
                        }
                        keyEvent.key == Key.Menu ||
                            (keyEvent.key == Key.F10 && keyEvent.isShiftPressed) -> {
                            modePanelOpen = true
                            true
                        }
                        else -> false
                    }
                }
                .pointerInput(bubbleDiameterPx, bubbleSettings.snapSensitivity) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val isSecondary = currentEvent.buttons.isSecondaryPressed
                        var dragged = false
                        var previous = down.position
                        var longPressFired = false

                        val longPressJob = scope.launch {
                            delay(LongPressTimeoutMs)
                            longPressFired = true
                            modePanelOpen = true
                            launchpadOpen = false
                        }

                        try {
                            if (isSecondary) {
                                // Right-click → mode switch panel.
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (event.type == PointerEventType.Release) {
                                        longPressJob.cancel()
                                        if (!longPressFired) {
                                            modePanelOpen = true
                                            launchpadOpen = false
                                        }
                                        break
                                    }
                                }
                            } else {
                                var finished = false
                                while (!finished) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    when (event.type) {
                                        PointerEventType.Move -> {
                                            if (!dragged) {
                                                // Drag sensitivity: the snap-sensitivity slider scales
                                                // how far the pointer must travel before the bubble
                                                // starts following it (larger = sturdier / less twitchy).
                                                val slop = viewConfiguration.touchSlop *
                                                    (bubbleSettings.snapSensitivity / 80f)
                                                if ((change.position - down.position).getDistance() > slop) {
                                                    dragged = true
                                                    longPressJob.cancel()
                                                    dragging = true
                                                    scope.launch { bubbleOffset.stop() }
                                                    launchpadOpen = false
                                                    modePanelOpen = false
                                                }
                                            }
                                            if (dragged) {
                                                change.consume()
                                                val current = bubbleOffset.value
                                                val next = Offset(
                                                    (current.x + change.position.x - previous.x)
                                                        .coerceIn(0f, (containerSize.width - bubbleDiameterPx).coerceAtLeast(0).toFloat()),
                                                    (current.y + change.position.y - previous.y)
                                                        .coerceIn(0f, (containerSize.height - bubbleDiameterPx).coerceAtLeast(0).toFloat())
                                                )
                                                scope.launch { bubbleOffset.snapTo(next) }
                                                previous = change.position
                                            }
                                        }
                                        PointerEventType.Release -> {
                                            longPressJob.cancel()
                                            if (dragged) {
                                                dragging = false
                                                snapAndPersist(bubbleOffset.value)
                                            } else if (!longPressFired) {
                                                launchpadOpen = !launchpadOpen
                                            }
                                            finished = true
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        } finally {
                            longPressJob.cancel()
                            dragging = false
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // The bubble glyph — premium squircle, theme-aware, hover lift and
            // press dip, subtle depth.
            Box(
                modifier = Modifier
                    .size(bubbleSize)
                    .graphicsLayer {
                        alpha = opacity
                        scaleX = hoverScale * pressScale
                        scaleY = hoverScale * pressScale
                    }
                    .hoverable(bubbleInteraction)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { launchpadOpen = true }
                    )
                    .clip(bubbleShape(bubbleSize))
                    .background(accent.primary)
                    .materialShadow(bubbleSettings.elevation.dp, bubbleShape(bubbleSize)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(bubbleShape(bubbleSize))
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(accent.primary, accent.primary.copy(alpha = 0.78f))
                            )
                        )
                )
                Icon(
                    currentIcon,
                    contentDescription = resolveString { nav.modeFloatingLabel },
                    tint = surfaceColors.surface,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        // Mode switch panel — expands from the bubble (hold / right-click).
        val windowPos = bubbleWindowRect()
        if (modePanelOpen && windowPos != null) {
            BubbleModeSwitchPanel(
                current = navSettings.settings.mode,
                bubbleWindowPos = windowPos,
                bubbleSizePx = bubbleDiameterPx + hitboxPaddingPx * 2,
                onSelect = { mode ->
                    modePanelOpen = false
                    navSettings.setMode(mode)
                },
                onDismiss = { modePanelOpen = false }
            )
        }
    }

    if (launchpadOpen) {
        Launchpad(
            sections = launchpadSections,
            navigationState = navigationState,
            homeNavState = homeNavState,
            visible = launchpadOpen,
            bubbleCenter = Offset(
                bubbleTopLeft.x + bubbleDiameterPx / 2f,
                bubbleTopLeft.y + bubbleDiameterPx / 2f
            ),
            onClose = {
                launchpadOpen = false
                bubbleFocusRequester.requestFocus()
            },
            launchpadSettings = navSettings.settings.launchpad
        )
    }
}

private fun bubbleShape(size: androidx.compose.ui.unit.Dp) =
    RoundedCornerShape(size * 0.32f)

// ============================================
// MODE SWITCH PANEL — expands from the bubble
// ============================================

@Composable
private fun BubbleModeSwitchPanel(
    current: NavigationMode,
    bubbleWindowPos: IntOffset,
    bubbleSizePx: Int,
    onSelect: (NavigationMode) -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val windowSize = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize

    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.82f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 420f),
        label = "modePanelScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(150),
        label = "modePanelAlpha"
    )

    val panelW = with(density) { 228.dp.roundToPx() }
    val panelH = with(density) { 148.dp.roundToPx() }
    val gap = with(density) { 12.dp.roundToPx() }

    val openLeft = bubbleWindowPos.x + bubbleSizePx / 2 > windowSize.width / 2
    val rawX = if (openLeft) bubbleWindowPos.x - panelW - gap else bubbleWindowPos.x + bubbleSizePx + gap
    val rawY = (bubbleWindowPos.y + bubbleSizePx / 2 - panelH / 2)
        .coerceIn(0, (windowSize.height - panelH).coerceAtLeast(0))
    val offsetX = rawX.coerceIn(0, (windowSize.width - panelW).coerceAtLeast(0))

    val focusRequesters = remember { listOf(FocusRequester(), FocusRequester()) }
    var selectedIndex by remember { mutableStateOf(if (current == NavigationMode.Floating) 0 else 1) }
    var firstFocus by remember { mutableStateOf(true) }
    LaunchedEffect(selectedIndex) {
        if (firstFocus) {
            firstFocus = false
            delay(120)
        }
        focusRequesters[selectedIndex].requestFocus()
    }

    val options = listOf(
        Pair(NavigationMode.Floating, Icons.Default.Apps) to resolveString { nav.modeFloatingLabel },
        Pair(NavigationMode.Sidebar, Icons.AutoMirrored.Filled.ViewSidebar) to resolveString { nav.modeSidebarLabel }
    )

    Popup(
        onDismissRequest = onDismiss,
        offset = IntOffset(offsetX, rawY),
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            modifier = Modifier
                .width(228.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    transformOrigin = TransformOrigin(
                        if (openLeft) 1f else 0f, 0.5f
                    )
                }
                .clip(RoundedCornerShape(24.dp))
                .background(surfaceColors.surfaceElevated.copy(alpha = 0.98f))
                .border(1.dp, surfaceColors.border.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .materialShadow(20.dp, RoundedCornerShape(24.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = resolveString { nav.modeSwitchTitle },
                style = MaterialTheme.typography.labelMedium,
                color = surfaceColors.textMuted,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 2.dp)
            )
            options.forEachIndexed { index, option ->
                val (modeAndIcon, label) = option
                val (mode, icon) = modeAndIcon
                val selected = mode == current
                ModeOptionRow(
                    icon = icon,
                    label = label,
                    selected = selected,
                    focused = selectedIndex == index,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesters[index])
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                            when (keyEvent.key) {
                                Key.DirectionDown, Key.DirectionUp -> {
                                    selectedIndex = 1 - index
                                    true
                                }
                                Key.Enter, Key.Spacebar -> {
                                    onSelect(mode)
                                    true
                                }
                                else -> false
                            }
                        },
                    onClick = { onSelect(mode) }
                )
            }
        }
    }
}

@Composable
private fun ModeOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    focused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = modifier
            .clip(shape)
            .background(
                when {
                    selected -> accent.primary.copy(alpha = 0.16f)
                    hovered -> surfaceColors.surfaceInteractive
                    else -> Color.Transparent
                }
            )
            .then(
                if (focused || selected) Modifier.border(1.5.dp, accent.primary.copy(alpha = 0.55f), shape)
                else Modifier
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) accent.primary.copy(alpha = 0.2f) else surfaceColors.surfaceInteractive),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) accent.primary else surfaceColors.textSecondary,
                modifier = Modifier.size(19.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) accent.primary else surfaceColors.textPrimary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (selected) {
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent.primary)
            )
        }
    }
}


