package ua.syt0r.kanji.presentation.common.nav

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.HomeNavigationState

// ============================================
// BUBBLE LAUNCHER — Floating mode
// A draggable launcher bubble that magnetizes to
// the nearest of the 12 snap points when released.
//   · Click / Tap         → launchpad (expands from the bubble)
//   · Hold / right-click  → mode panel (Floating ↔ Sidebar, settings)
//   · Drag                → free movement, spring snap on release
// Gestures are disambiguated in a single pointer
// handler: a move beyond the drag slop becomes a
// drag (never a click), a press held past the
// hold duration becomes the hold panel (never a
// click), and a clean release stays a click.
// Position + snap point persist across restarts.
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
    val density = LocalDensity.current
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val accessibility = navSettings.settings.accessibility
    val bubbleSettings = navSettings.settings.bubble

    // The launchpad shows the same primary destinations as the sidebar —
    // no separate curated quick-access grid.
    val launchpadSections = sections
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
    var settingsOpen by remember { mutableStateOf(false) }
    var faded by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var bubbleCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // The bubble starts at Offset.Zero (top-left corner) until the first
    // layout pass places it at its snap anchor — keep it invisible until then
    // so the launch never flashes a bubble in the corner.
    var positioned by remember { mutableStateOf(false) }

    // While dragging, the position is tracked in plain state (no per-move
    // coroutine launches — that was a source of lag/jitter). The Animatable
    // owns the idle position and the snap animation on release.
    var dragOffset by remember { mutableStateOf<Offset?>(null) }

    // ── Physics: velocity tracking, magnetic pull, fling snap ──
    var velocity by remember { mutableStateOf(Offset.Zero) }
    val magneticRadius = 100f * density.density
    val flingProjectionFactor = 0.35f
    var edgeProximity by remember { mutableStateOf(0f) }
    var grabProgress by remember { mutableStateOf(0f) }
    val animatedGrabProgress by animateFloatAsState(
        targetValue = grabProgress,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "grabProgress"
    )
    val grabScale = 1f + animatedGrabProgress * 0.1f

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
    // Every snap anchor and drag clamp derives from the configured safe margin
    // so the bubble can never end up clipped, off-screen or under system areas.
    val safeMarginPx = with(density) { bubbleSettings.safeMargin.coerceIn(4, 48).dp.roundToPx() }
    val topMarginPx = max(safeMarginPx, topInsetPx)
    val bottomMarginPx = max(safeMarginPx, bottomInsetPx)

    val snapPositionFor: (BubbleSnapPoint) -> Offset = { snap ->
        val size = bubbleDiameterPx.toFloat()
        val w = containerSize.width.toFloat()
        val h = containerSize.height.toFloat()
        val top = topMarginPx.toFloat()
        val bottom = bottomMarginPx.toFloat()
        val side = safeMarginPx.toFloat()
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

    fun nearestSnap(bubbleTopLeft: Offset): BubbleSnapPoint =
        BubbleSnapPoint.entries.minByOrNull { snap ->
            val pos = snapPositionFor(snap)
            val dx = bubbleTopLeft.x - pos.x
            val dy = bubbleTopLeft.y - pos.y
            dx * dx + dy * dy
        } ?: snapPoint

    // Snap ring trail — fading ghost copies of previous anchor positions
    // that linger briefly as the ring moves between snap points.
    val trailEntries = remember { mutableStateListOf<Offset>() }
    var lastTrailSnap by remember { mutableStateOf<BubbleSnapPoint?>(null) }
    val currentDragPos = dragOffset ?: bubbleOffset.value
    LaunchedEffect(dragging, currentDragPos) {
        if (!dragging) {
            trailEntries.clear()
            lastTrailSnap = null
            return@LaunchedEffect
        }
        val currentSnap = nearestSnap(currentDragPos)
        if (lastTrailSnap != null && currentSnap != lastTrailSnap) {
            val oldAnchor = snapPositionFor(lastTrailSnap!!)
            trailEntries.add(oldAnchor)
            delay(400)
            if (trailEntries.isNotEmpty()) trailEntries.removeAt(0)
        }
        lastTrailSnap = currentSnap
    }

    // Position the bubble at its persisted snap point (no animation on setup).
    // Every stored coordinate is validated against the current window: if the
    // window/device changed so the persisted spot would be off-screen or
    // inside the safe margin, fall back to the clean snap anchor instead of
    // rendering (or persisting) an invalid position.
    //
    // While the user is dragging (dragOffset != null) this effect must not
    // run: snapTo() would fight the drag. The effect only re-triggers when a
    // persisted snap point/offset actually changes (after a release), at
    // which point the drag is already over.
    LaunchedEffect(containerSize, snapPoint, persistedOffsetX, persistedOffsetY, bubbleDiameterPx, safeMarginPx) {
        if (containerSize == IntSize.Zero) return@LaunchedEffect
        if (dragOffset != null) return@LaunchedEffect
        val anchorPos = snapPositionFor(snapPoint)
        val drift = Offset(
            with(density) { persistedOffsetX.dp.roundToPx() }.toFloat(),
            with(density) { persistedOffsetY.dp.roundToPx() }.toFloat()
        )
        val target = anchorPos + drift
        val minX = safeMarginPx.toFloat()
        val minY = topMarginPx.toFloat()
        val maxX = (containerSize.width - bubbleDiameterPx - safeMarginPx).coerceAtLeast(0).toFloat()
        val maxY = (containerSize.height - bubbleDiameterPx - bottomMarginPx).coerceAtLeast(0).toFloat()
        val inBounds = target.x >= minX && target.x <= maxX && target.y >= minY && target.y <= maxY
        bubbleOffset.snapTo(if (inBounds) target else anchorPos)
        positioned = true
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
        // Velocity-aware snap: project the release position forward in the
        // fling direction so the bubble lands on the edge the user was
        // heading toward.
        val vel = velocity
        val speed = vel.getDistance()
        val minX = safeMarginPx.toFloat()
        val minY = topMarginPx.toFloat()
        val maxX = (containerSize.width - bubbleDiameterPx - safeMarginPx).coerceAtLeast(0).toFloat()
        val maxY = (containerSize.height - bubbleDiameterPx - bottomMarginPx).coerceAtLeast(0).toFloat()
        val projectedPos = if (speed > 120f) {
            Offset(
                (bubbleTopLeft.x + vel.x * flingProjectionFactor).coerceIn(minX, maxX),
                (bubbleTopLeft.y + vel.y * flingProjectionFactor).coerceIn(minY, maxY)
            )
        } else bubbleTopLeft
        val nearest = nearestSnap(projectedPos)
        val anchorPos = snapPositionFor(nearest)
        velocity = Offset.Zero
        edgeProximity = 0f
        // Always magnetize on release (WhatsApp-style): the bubble returns to
        // the nearest edge anchor with a quick spring — it never stays floating
        // mid-screen. Slightly underdamped for a satisfying overshoot.
        if (animations) {
            scope.launch {
                bubbleOffset.animateTo(
                    anchorPos,
                    spring(dampingRatio = 0.48f, stiffness = 520f * speed)
                )
                persistPosition(nearest, anchorPos)
            }
        } else {
            scope.launch {
                bubbleOffset.snapTo(anchorPos)
                persistPosition(nearest, anchorPos)
            }
        }
    }

    // Auto-fade when idle (paused while any surface is open or hovered).
    // Hover reveal: hovering the (faded) bubble brings it back instantly.
    val idleTimeout = bubbleSettings.effectiveIdleTimeoutMs()
    LaunchedEffect(isBubbleHovered, launchpadOpen, modePanelOpen, dragging, idleTimeout, bubbleSettings.hoverReveal) {
        val timeout = idleTimeout
        if (timeout == null || isBubbleHovered || launchpadOpen || modePanelOpen || dragging) {
            faded = false
        } else {
            delay(timeout)
            faded = true
        }
    }
    LaunchedEffect(isBubbleHovered, faded, bubbleSettings.hoverReveal) {
        if (faded && isBubbleHovered && bubbleSettings.hoverReveal) {
            faded = false
        }
    }

    val targetAlpha = when {
        !positioned -> 0f
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
    val draggingNow = dragOffset != null
    val hoverScale by animateFloatAsState(
        // While dragging, the pointer can cross the hitbox edge and flip the
        // hover state — ignore hover entirely mid-drag so the bubble never
        // flashes between sizes while being moved.
        targetValue = if (draggingNow || launchpadOpen || modePanelOpen) 1f
        else if (bubbleHovered) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 420f),
        label = "bubbleHoverScale"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (draggingNow) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "bubblePressScale"
    )

    // Bubble geometry helper for the mode panel popup.
    fun bubbleWindowRect(): IntOffset? {
        val coords = bubbleCoords ?: return null
        val pos = coords.positionInWindow()
        return IntOffset(pos.x.roundToInt(), pos.y.roundToInt())
    }

    val position = dragOffset ?: bubbleOffset.value

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
    ) {
        Box(
            modifier = Modifier
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
                .pointerInput(
                    bubbleDiameterPx,
                    bubbleSettings.snapSensitivity,
                    bubbleSettings.holdDurationMs,
                    bubbleSettings.safeMargin
                ) {
                    val minX = safeMarginPx.toFloat()
                    val minY = topMarginPx.toFloat()
                    val maxX = (containerSize.width - bubbleDiameterPx - safeMarginPx).coerceAtLeast(0).toFloat()
                    val maxY = (containerSize.height - bubbleDiameterPx - bottomMarginPx).coerceAtLeast(0).toFloat()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // Claim the press so it can never click through to the
                        // content underneath the floating bubble.
                        down.consume()
                        val isSecondary = currentEvent.buttons.isSecondaryPressed
                        var dragged = false
                        var previous = down.position
                        var longPressFired = false

                        // A press held past the hold duration opens the mode
                        // panel. A click must never reach the hold panel, and a
                        // hold must never double-fire the launchpad afterwards.
                        val longPressJob = scope.launch {
                            delay(bubbleSettings.holdDurationMs.coerceAtLeast(150L))
                            longPressFired = true
                            modePanelOpen = true
                            launchpadOpen = false
                        }

                        try {
                            if (isSecondary) {
                                // Right-click → launchpad (quick access to navigation).
                                longPressJob.cancel()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (event.type == PointerEventType.Release) {
                                        if (!dragged) {
                                            launchpadOpen = true
                                            modePanelOpen = false
                                        }
                                        break
                                    }
                                    if (event.type == PointerEventType.Move &&
                                        (change.position - down.position).getDistance() > viewConfiguration.touchSlop * 2f
                                    ) {
                                        dragged = true
                                    }
                                }
                            } else {
                                var finished = false
                                var lastMoveMark = kotlin.time.TimeSource.Monotonic.markNow()
                                while (!finished) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    when (event.type) {
                                        PointerEventType.Move -> {
                                            if (!dragged) {
                                                val slop = viewConfiguration.touchSlop * 0.45f *
                                                    (bubbleSettings.snapSensitivity / 100f)
                                                if ((change.position - down.position).getDistance() > slop) {
                                                    dragged = true
                                                    longPressJob.cancel()
                                                    dragging = true
                                                    grabProgress = 1f
                                                    scope.launch { bubbleOffset.stop() }
                                                    dragOffset = bubbleOffset.value + Offset(
                                                        change.position.x - down.position.x,
                                                        change.position.y - down.position.y
                                                    )
                                                    launchpadOpen = false
                                                    modePanelOpen = false
                                                }
                                            }
                                            if (dragged) {
                                                change.consume()
                                                val current = dragOffset ?: bubbleOffset.value
                                                var next = Offset(
                                                    (current.x + change.position.x - previous.x).coerceIn(minX, maxX),
                                                    (current.y + change.position.y - previous.y).coerceIn(minY, maxY)
                                                )

                                                // Velocity tracking (exponential moving average)
                                                val now = kotlin.time.TimeSource.Monotonic.markNow()
                                                val dtMs = (now - lastMoveMark).inWholeMilliseconds.toFloat().coerceAtLeast(1f)
                                                lastMoveMark = now
                                                val dx = change.position.x - previous.x
                                                val dy = change.position.y - previous.y
                                                val instantVel = Offset(dx / dtMs * 16f, dy / dtMs * 16f)
                                                velocity = Offset(
                                                    velocity.x * 0.6f + instantVel.x * 0.4f,
                                                    velocity.y * 0.6f + instantVel.y * 0.4f
                                                )

                                                // Magnetic pull toward nearest snap anchor
                                                var bestDist = Float.MAX_VALUE
                                                var bestAnchor = Offset.Zero
                                                BubbleSnapPoint.entries.forEach { snap ->
                                                    val anchor = snapPositionFor(snap)
                                                    val d = Offset(next.x - anchor.x, next.y - anchor.y).getDistance()
                                                    if (d < bestDist) { bestDist = d; bestAnchor = anchor }
                                                }
                                                if (bestDist < magneticRadius && bestDist > 1f) {
                                                    val pull = (1f - bestDist / magneticRadius) * 0.18f
                                                    next = Offset(
                                                        next.x + (bestAnchor.x - next.x) * pull,
                                                        next.y + (bestAnchor.y - next.y) * pull
                                                    )
                                                }
                                                edgeProximity = (1f - (bestDist / magneticRadius).coerceIn(0f, 1f))

                                                dragOffset = next
                                                previous = change.position
                                            }
                                        }
                                        PointerEventType.Release -> {
                                            longPressJob.cancel()
                                            if (dragged) {
                                                dragging = false
                                                grabProgress = 0f
                                                val finalPos = dragOffset ?: bubbleOffset.value
                                                dragOffset = null
                                                snapAndPersist(finalPos)
                                            } else if (!longPressFired) {
                                                // Clean click: no drag, no hold → toggle the launchpad.
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
                            dragOffset = null
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // The bubble glyph — premium squircle, theme-aware, hover lift and
            // press dip, subtle depth. All gestures (click / hold / drag) are
            // handled by the single pointerInput above; there is deliberately
            // no separate clickable here that could double-fire.
            Box(
                modifier = Modifier
                    .size(bubbleSize)
                    .graphicsLayer {
                        alpha = opacity
                        // Velocity-proportional squash/stretch + grab scale.
                        val speed = if (draggingNow) velocity.getDistance() else 0f
                        val squashFactor = (speed / 2000f).coerceIn(0f, 1f) * 0.06f
                        scaleX = hoverScale * pressScale * grabScale * (1f + squashFactor)
                        scaleY = hoverScale * pressScale * grabScale * (1f - squashFactor)
                    }
                    .hoverable(bubbleInteraction)
                    // Shadow first so it renders behind the bubble — ordered
                    // after background it painted a dark shape on top of the
                    // glyph, flattening the depth instead of adding it.
                    .materialShadow(bubbleSettings.elevation.dp, bubbleShape(bubbleSize))
                    .clip(bubbleShape(bubbleSize))
                    .background(accent.primary),
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
                // Edge proximity glow — subtle brightening near snap anchors.
                if (edgeProximity > 0.01f) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(bubbleShape(bubbleSize))
                            .background(Color.White.copy(alpha = edgeProximity * 0.15f))
                    )
                }
                Icon(
                    currentIcon,
                    contentDescription = resolveString { nav.modeFloatingLabel },
                    tint = surfaceColors.surface,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        // Snap target preview while dragging: the nearest anchor is always
        // shown so the release destination is telegraphed. Unconditional — the
        // ring moves between anchors instead of appearing/disappearing, so it
        // can never blink mid-drag. Its size follows the snap-distance setting.
        val previewAnchor = if (draggingNow) {
            val pos = dragOffset!!
            snapPositionFor(nearestSnap(pos))
        } else null
        if (previewAnchor != null) {
            val ringSize = bubbleSettings.snapDistance.coerceIn(40, 120).dp
            val ringPx = with(density) { ringSize.roundToPx() }
            val previewShape = bubbleShape(bubbleSize + hitboxPadding * 2)
            // Gentle pulse: scale 0.92 → 1.08 and alpha 0.35 → 0.65 over 1.8 s.
            val infiniteTransition = rememberInfiniteTransition()
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.92f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "snapRingPulseScale"
            )
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.35f,
                targetValue = 0.65f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "snapRingPulseAlpha"
            )

            // Fading trail — ghost copies of previous anchor positions.
            val trailCount = trailEntries.size
            trailEntries.forEachIndexed { i, trailPos ->
                val progress = (i + 1).toFloat() / (trailCount + 1).coerceAtLeast(1)
                val trailAlpha = 0.08f + progress * 0.17f
                val trailScale = 0.7f + progress * 0.2f
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (trailPos.x - ringPx / 2f).roundToInt(),
                                (trailPos.y - ringPx / 2f).roundToInt()
                            )
                        }
                        .size(ringSize)
                        .graphicsLayer {
                            scaleX = trailScale
                            scaleY = trailScale
                            alpha = trailAlpha
                        }
                        .border(1.5.dp, accent.primary.copy(alpha = trailAlpha + 0.1f), previewShape)
                )
            }

            // Main pulsing ring at the current nearest anchor.
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (previewAnchor.x - ringPx / 2f).roundToInt(),
                            (previewAnchor.y - ringPx / 2f).roundToInt()
                        )
                    }
                    .size(ringSize)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .border(2.dp, accent.primary.copy(alpha = 0.7f), previewShape)
            )
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
                onOpenSettings = {
                    modePanelOpen = false
                    launchpadOpen = false
                    settingsOpen = true
                },
                onDismiss = { modePanelOpen = false }
            )
        }
    }

    // Navigation settings, opened from the hold panel ("change how navigation
    // works"). The overlay is a Dialog so it floats above everything.
    if (settingsOpen) {
        NavigationSettingsOverlay(
            navSettings = navSettings,
            formFactor = formFactor,
            onDismiss = { settingsOpen = false }
        )
    }

    // Always composed so the launchpad can play its exit animation; the
    // full-screen scrim only receives input while visible.
    Launchpad(
        sections = launchpadSections,
        navigationState = navigationState,
        homeNavState = homeNavState,
        visible = launchpadOpen,
        bubbleCenter = Offset(
            position.x + bubbleDiameterPx / 2f,
            position.y + bubbleDiameterPx / 2f
        ),
        onClose = {
            launchpadOpen = false
            bubbleFocusRequester.requestFocus()
        },
        launchpadSettings = navSettings.settings.launchpad
    )
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
    onOpenSettings: () -> Unit,
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

    val panelW = with(density) { 236.dp.roundToPx() }
    val panelH = with(density) { 232.dp.roundToPx() }
    val gap = with(density) { 12.dp.roundToPx() }

    val openLeft = bubbleWindowPos.x + bubbleSizePx / 2 > windowSize.width / 2
    val rawX = if (openLeft) bubbleWindowPos.x - panelW - gap else bubbleWindowPos.x + bubbleSizePx + gap
    val rawY = (bubbleWindowPos.y + bubbleSizePx / 2 - panelH / 2)
        .coerceIn(0, (windowSize.height - panelH).coerceAtLeast(0))
    val offsetX = rawX.coerceIn(0, (windowSize.width - panelW).coerceAtLeast(0))

    // Keyboard: mode rows (0,1) + settings row (2).
    val focusRequesters = remember { listOf(FocusRequester(), FocusRequester(), FocusRequester()) }
    var selectedIndex by remember {
        mutableStateOf(if (current == NavigationMode.Floating) 0 else 1)
    }
    var firstFocus by remember { mutableStateOf(true) }
    LaunchedEffect(selectedIndex) {
        if (firstFocus) {
            firstFocus = false
            delay(120)
        }
        focusRequesters.getOrNull(selectedIndex)?.requestFocus()
    }

    val modes = listOf(
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
                .width(236.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    transformOrigin = TransformOrigin(
                        if (openLeft) 1f else 0f, 0.5f
                    )
                }
                .materialShadow(20.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(surfaceColors.surfaceElevated.copy(alpha = 0.98f))
                .border(1.dp, surfaceColors.border.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
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
            modes.forEachIndexed { index, option ->
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
                                Key.DirectionDown -> {
                                    selectedIndex = (index + 1) % 3
                                    true
                                }
                                Key.DirectionUp -> {
                                    selectedIndex = (index + 2) % 3
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
            // Navigation settings — the second reason to hold the bubble:
            // \"I want to change how navigation works\".
            ModeOptionRow(
                icon = Icons.Default.Settings,
                label = resolveString { nav.settingsLabel },
                selected = false,
                focused = selectedIndex == 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequesters[2])
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                        when (keyEvent.key) {
                            Key.DirectionDown -> {
                                selectedIndex = 0
                                true
                            }
                            Key.DirectionUp -> {
                                selectedIndex = 1
                                true
                            }
                            Key.Enter, Key.Spacebar -> {
                                onOpenSettings()
                                true
                            }
                            else -> false
                        }
                    },
                onClick = onOpenSettings
            )
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
