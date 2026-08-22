package ua.syt0r.kanji.desktop.ui.workspace

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.LauncherIconSize
import ua.syt0r.kanji.desktop.appstate.LauncherSize
import ua.syt0r.kanji.desktop.appstate.LauncherSnapPoint
import ua.syt0r.kanji.desktop.engine.navigation.LauncherSnapMath
import ua.syt0r.kanji.desktop.appstate.NavLayout
import ua.syt0r.kanji.desktop.appstate.WorkspaceView
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors

// ============================================
// FLOATING LAUNCHER — Floating mode
// A draggable launcher bubble that magnetizes to
// the nearest of the 12 snap points when released.
//   · Tap / Enter        → launchpad (expands from the bubble)
//   · Hold / right-click → mode switch panel (Floating ↔ Sidebar)
//   · Drag               → free movement, spring snap on release
// Auto-fades when idle; position + snap point persist.
// ============================================

/** How long a press must be held before the mode panel opens (ms). */
private const val LongPressTimeoutMs = 480L

/**
 * Dev-only capture mode. When the desktop app is launched with
 * `--capture-state=<shell|menu|launchpad|strip>` this local is set and the
 * launcher pre-opens the matching state so `scripts/capture-window-shell.sh`
 * can screenshot it deterministically. Null in normal runs.
 */
val LocalCaptureState = compositionLocalOf<String?> { null }

@Composable
fun DsFloatingLauncher(state: AppState) {
    val sc = surfaceColors()
    val ac = accent()
    val density = LocalDensity.current
    val captureState = LocalCaptureState.current
    val scope = rememberCoroutineScope()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        val bubbleSize = when (state.launcherSize) {
            LauncherSize.Small -> 44.dp
            LauncherSize.Medium -> 52.dp
            LauncherSize.Large -> 62.dp
        }
        val iconSize = when (state.launcherIconSize) {
            LauncherIconSize.Small -> 18.dp
            LauncherIconSize.Medium -> 22.dp
            LauncherIconSize.Large -> 26.dp
        }
        // Premium squircle — consistent corner radius for every bubble size.
        val bubbleShape = RoundedCornerShape(bubbleSize * 0.32f)
        val hitboxExtra = if (state.navigationLargerHitbox) 18.dp else 8.dp
        // Phones/compact windows keep the bubble clear of the tab bars.
        val compactWindow = w < Breakpoints.CompactWindowWidth
        val edgeInset = if (compactWindow) 72.dp else 0.dp
        val snapPoint = state.launcherSnapPoint

        // Position as fractions of the window. Compact windows remember a
        // separate spot so the bubble never fights the tab bar / gesture zones.
        // Restore validates against the current window (sizes change between
        // sessions) and clamps/re-snaps instead of ever crashing or going
        // off-screen — see LauncherSnapMath.restorePosition.
        // 8dp safe margin so the bubble never sits flush against the window
        // edge (premium feel: always a small breathing room).
        val snapLayout = LauncherSnapMath.SnapLayout(
            windowWidth = w.value,
            windowHeight = h.value,
            bubbleSize = bubbleSize.value,
            edgeInset = edgeInset.value,
            safeMargin = 8f
        )
        var dragPos by remember(w, h, compactWindow) {
            mutableStateOf(
                LauncherSnapMath.restorePosition(
                    storedX = if (compactWindow) state.launcherPosXPhone else state.launcherPosX,
                    storedY = if (compactWindow) state.launcherPosYPhone else state.launcherPosY,
                    layout = snapLayout
                )
            )
        }
        var target by remember(w, h) { mutableStateOf(dragPos) }
        // When the window crosses the phone/desktop boundary, dragPos re-reads
        // the form-factor-specific remembered spot — glide the bubble there.
        LaunchedEffect(compactWindow) {
            target = dragPos
        }
        val animatedPos by animateOffsetAsState(
            targetValue = target,
            animationSpec = if (state.navigationAnimations && !state.navReducedMotion) {
                // Slightly underdamped spring for a satisfying overshoot on
                // snap — the bubble lands past the target then settles back,
                // like a real object with mass. Higher animation speed = snappier.
                spring(
                    dampingRatio = 0.48f,
                    stiffness = 280f / state.launcherAnimationSpeed.coerceAtLeast(0.25f)
                )
            } else {
                tween(0)
            },
            label = "launcherPos"
        )
        // ── State declared early so finishDrag() and displayPos can reference them ──
        var dragging by remember { mutableStateOf(false) }
        var velocity by remember { mutableStateOf(Offset.Zero) }
        val flingProjectionFactor = 0.35f

        // During drag the bubble follows the pointer directly (no animation)
        // so it never lags behind or flashes. The animated offset only kicks
        // in for the snap-back on release and the settings-triggered reposition.
        val displayPos = if (dragging) dragPos else animatedPos

        val bubbleDiameterPx = with(density) { bubbleSize.roundToPx() }
        val hitboxPaddingPx = with(density) { hitboxExtra.roundToPx() }

        fun snapPositionFor(snap: LauncherSnapPoint): Offset =
            LauncherSnapMath.anchorPosition(snap, snapLayout)

        // Changing the snap point in settings moves the bubble live (and is
        // persisted as the new remembered spot). Skipped on first launch so
        // the remembered position always wins.
        var initialized by remember { mutableStateOf(false) }
        LaunchedEffect(state.launcherSnapPoint) {
            if (!initialized) {
                initialized = true
                return@LaunchedEffect
            }
            if (w.value <= 0f || h.value <= 0f) return@LaunchedEffect
            val anchor = snapPositionFor(state.launcherSnapPoint)
            dragPos = anchor
            target = anchor
            state.setLauncherPos(anchor.x / w.value, anchor.y / h.value, compact = compactWindow)
        }

        fun finishDrag() {
            // Velocity-aware snap: project the release position forward in the
            // fling direction so the bubble lands on the edge the user was
            // heading toward, not just the geometrically closest one.
            val vel = velocity
            val speed = vel.getDistance()
            val projectedPos = if (speed > 120f) {
                Offset(
                    (dragPos.x + vel.x * flingProjectionFactor).coerceIn(0f, w.value - bubbleSize.value),
                    (dragPos.y + vel.y * flingProjectionFactor).coerceIn(0f, h.value - bubbleSize.value)
                )
            } else {
                dragPos
            }
            val settled = LauncherSnapMath.settle(projectedPos, snapLayout)
            state.updateLauncherSnapPoint(settled.snap)
            dragPos = settled.anchor
            target = settled.anchor
            velocity = Offset.Zero
            state.setLauncherPos(settled.posX, settled.posY, compact = compactWindow)
        }

        var faded by remember { mutableStateOf(false) }
        var lastActive by remember { mutableStateOf(System.currentTimeMillis()) }
        // Capture mode pre-opens the requested state so screenshots are
        // deterministic (see LocalCaptureState).
        var menuOpen by remember { mutableStateOf(captureState == "launchpad" || captureState == "strip") }
        var modePanelOpen by remember { mutableStateOf(captureState == "menu") }
        var bubbleAnchor by remember { mutableStateOf<LayoutCoordinates?>(null) }
        var openingPanelViaKeyboard by remember { mutableStateOf(false) }
        val bubbleFocusRequester = remember { FocusRequester() }

        // ── Physics: magnetic pull ──
        // Magnetic pull radius: within this distance the bubble is gently
        // attracted to the nearest snap anchor. Uses dp-space to match
        // the snapLayout coordinates.
        val magneticRadius = 100f
        // Grab/release scale: the bubble grows when grabbed and shrinks
        // back on release for a tactile "picked up" feel.
        var grabProgress by remember { mutableStateOf(0f) }
        val animatedGrabProgress by animateFloatAsState(
            targetValue = grabProgress,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
            label = "grabProgress"
        )
        val grabScale = 1f + animatedGrabProgress * 0.1f
        // Edge proximity glow: 0 = far from any edge, 1 = snapped/touching.
        var edgeProximity by remember { mutableStateOf(0f) }

        // Snap ring trail — fading ghost copies of previous anchor positions
        // that linger briefly as the ring moves between snap points.
        val trailEntries = remember { mutableStateListOf<Offset>() }
        var lastTrailSnap by remember { mutableStateOf<LauncherSnapPoint?>(null) }
        LaunchedEffect(dragging, dragPos) {
            if (!dragging) {
                trailEntries.clear()
                lastTrailSnap = null
                return@LaunchedEffect
            }
            val currentSnap = LauncherSnapMath.nearestSnap(dragPos, snapLayout)
            if (lastTrailSnap != null && currentSnap != lastTrailSnap) {
                // The ring moved — push the old anchor as a fading ghost.
                val oldAnchor = snapPositionFor(lastTrailSnap!!)
                trailEntries.add(oldAnchor)
                // Prune after 400 ms so old ghosts don't linger.
                delay(400)
                if (trailEntries.isNotEmpty()) trailEntries.removeAt(0)
            }
            lastTrailSnap = currentSnap
        }

        // Turning auto-fade off restores full visibility immediately.
        LaunchedEffect(state.launcherAutoFade) {
            if (!state.launcherAutoFade) faded = false
        }

        // Auto-fade after inactivity. Event-driven: the effect restarts on any
        // activity or fade-setting change, sleeps exactly the fade delay, then
        // fades once. No perpetual polling while the window is idle — the app
        // can rest between interactions, keeping drag and hover smooth.
        LaunchedEffect(state.launcherAutoFade, state.launcherFadeDelayMs, menuOpen, modePanelOpen, lastActive, captureState) {
            if (!state.launcherAutoFade || menuOpen || modePanelOpen || captureState != null) return@LaunchedEffect
            delay(state.launcherFadeDelayMs.toLong())
            if (System.currentTimeMillis() - lastActive >= state.launcherFadeDelayMs) {
                faded = true
            }
        }

        val opacity by animateFloatAsState(
            targetValue = if (faded) state.launcherFadeOpacity else 1f,
            animationSpec = tween(
                if (state.navigationAnimations && !state.navReducedMotion) state.launcherFadeDurationMs else 0
            ),
            label = "launcherOpacity"
        )

        // Hovering the (larger) invisible hitbox restores it immediately.
        val hitboxInteraction = remember { MutableInteractionSource() }
        val hitboxHovered by hitboxInteraction.collectIsHoveredAsState()
        LaunchedEffect(hitboxHovered, menuOpen, modePanelOpen) {
            if (hitboxHovered || menuOpen || modePanelOpen) {
                faded = false
                lastActive = System.currentTimeMillis()
            }
        }

        // Hover / press response on the bubble glyph itself.
        val bubbleInteraction = remember { MutableInteractionSource() }
        val bubbleHovered by bubbleInteraction.collectIsHoveredAsState()
        val hoverScale by animateFloatAsState(
            targetValue = if (menuOpen || modePanelOpen) 1f else if (bubbleHovered) 1.06f else 1f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 420f),
            label = "bubbleHoverScale"
        )
        val pressScale by animateFloatAsState(
            targetValue = if (dragging) 0.94f else 1f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
            label = "bubblePressScale"
        )

        val hitboxSize = bubbleSize + hitboxExtra * 2

        // The launcher bubble, positioned at the animated offset.
        Box(
            modifier = Modifier.offset {
                IntOffset(displayPos.x.roundToInt(), displayPos.y.roundToInt())
            }
        ) {
            Box(
                modifier = Modifier
                    .size(hitboxSize)
                    .hoverable(hitboxInteraction)
                    .onGloballyPositioned { if (bubbleAnchor != it) bubbleAnchor = it },
                contentAlignment = Alignment.Center
            ) {
                // Bubble glyph — premium squircle, theme-aware, hover lift and
                // press dip, subtle depth. Unified gesture handling below:
                // tap opens the launchpad, hold/right-click open the mode
                // panel, dragging moves the bubble freely.
                Box(
                    modifier = Modifier
                        .size(bubbleSize)
                        .graphicsLayer {
                            alpha = opacity
                            // Velocity-proportional squash/stretch: the faster
                            // the drag the more the bubble deforms along the
                            // movement axis — reads as organic, not rigid.
                            val speed = if (dragging) velocity.getDistance() else 0f
                            val squashFactor = (speed / 2000f).coerceIn(0f, 1f) * 0.06f
                            val grabS = grabScale
                            scaleX = hoverScale * pressScale * grabS * (1f + squashFactor)
                            scaleY = hoverScale * pressScale * grabS * (1f - squashFactor)
                        }
                        .focusRequester(bubbleFocusRequester)
                        .focusable()
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                            when {
                                keyEvent.key == Key.Enter || keyEvent.key == Key.Spacebar -> {
                                    menuOpen = true
                                    true
                                }
                                keyEvent.key == Key.Menu ||
                                    (keyEvent.key == Key.F10 && keyEvent.isShiftPressed) -> {
                                    modePanelOpen = true
                                    openingPanelViaKeyboard = true
                                    true
                                }
                                else -> false
                            }
                        }
                        .hoverable(bubbleInteraction)
                        .pointerInput(bubbleDiameterPx) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val isSecondary = currentEvent.buttons.isSecondaryPressed
                                var dragged = false
                                var previous = down.position
                                var longPressFired = false

                                // Right-click opens the launchpad (navigation destinations).
                                // Long-press opens the mode switch panel (Floating ↔ Sidebar).
                                fun openLaunchpad() {
                                    menuOpen = true
                                    modePanelOpen = false
                                    lastActive = System.currentTimeMillis()
                                }
                                fun openModePanel() {
                                    modePanelOpen = true
                                    menuOpen = false
                                    lastActive = System.currentTimeMillis()
                                }

                                val longPressJob = scope.launch {
                                    delay(LongPressTimeoutMs)
                                    longPressFired = true
                                    openModePanel()
                                }

                                try {
                                    if (isSecondary) {
                                        // Right-click → launchpad (navigation destinations).
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            when (event.type) {
                                                PointerEventType.Release, PointerEventType.Exit -> {
                                                    longPressJob.cancel()
                                                    if (!longPressFired) openLaunchpad()
                                                    return@awaitEachGesture
                                                }
                                                else -> {}
                                            }
                                        }
                                    } else {
                                        var finished = false
                                        var lastMoveTime = System.nanoTime()
                                        var prevMovePos = down.position
                                        while (!finished) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            when (event.type) {
                                                PointerEventType.Move -> {
                                                    val pos = change.position
                                                    if (!dragged && pos.x.isFinite() && pos.y.isFinite()) {
                                                        val slop = viewConfiguration.touchSlop
                                                        if ((pos - down.position).getDistance() > slop) {
                                                            dragged = true
                                                            longPressJob.cancel()
                                                            dragging = true
                                                            grabProgress = 1f
                                                            menuOpen = false
                                                            modePanelOpen = false
                                                        }
                                                    }
                                                    if (dragged && previous.x.isFinite() && previous.y.isFinite() && pos.x.isFinite() && pos.y.isFinite()) {
                                                        change.consume()
                                                        val delta = pos - previous

                                                        // ── Velocity tracking (exponential moving average) ──
                                                        val now = System.nanoTime()
                                                        val dtMs = ((now - lastMoveTime) / 1_000_000f).coerceAtLeast(1f)
                                                        val instantVel = Offset(delta.x / dtMs * 16f, delta.y / dtMs * 16f)
                                                        velocity = Offset(
                                                            velocity.x * 0.6f + instantVel.x * 0.4f,
                                                            velocity.y * 0.6f + instantVel.y * 0.4f
                                                        )
                                                        lastMoveTime = now
                                                        prevMovePos = pos

                                                        // ── Magnetic pull ──
                                                        // If the bubble is within the magnetic radius of any
                                                        // snap anchor, gently pull it closer — the closer it
                                                        // gets, the stronger the pull (like a real magnet).
                                                        var newPos = dragPos + delta
                                                        var bestDist = Float.MAX_VALUE
                                                        var bestAnchor = Offset.Zero
                                                        LauncherSnapPoint.entries.forEach { snap ->
                                                            val anchor = snapPositionFor(snap)
                                                            val d = Offset(newPos.x - anchor.x, newPos.y - anchor.y).getDistance()
                                                            if (d < bestDist) {
                                                                bestDist = d
                                                                bestAnchor = anchor
                                                            }
                                                        }
                                                        if (bestDist < magneticRadius && bestDist > 1f) {
                                                            // Pull strength: 0 at radius edge → 0.18 at center.
                                                            val pull = (1f - bestDist / magneticRadius) * 0.18f
                                                            newPos = Offset(
                                                                newPos.x + (bestAnchor.x - newPos.x) * pull,
                                                                newPos.y + (bestAnchor.y - newPos.y) * pull
                                                            )
                                                        }
                                                        dragPos = newPos
                                                        previous = pos

                                                        // ── Edge proximity glow ──
                                                        edgeProximity = (1f - (bestDist / magneticRadius).coerceIn(0f, 1f))
                                                    }
                                                }
                                                PointerEventType.Release -> {
                                                    longPressJob.cancel()
                                                    if (dragged) {
                                                        dragging = false
                                                        grabProgress = 0f
                                                        edgeProximity = 0f
                                                        finishDrag()
                                                    } else if (!longPressFired) {
                                                        menuOpen = !menuOpen
                                                    }
                                                    finished = true
                                                }
                                                PointerEventType.Exit -> {
                                                    longPressJob.cancel()
                                                    if (dragged) {
                                                        dragging = false
                                                        grabProgress = 0f
                                                        edgeProximity = 0f
                                                        finishDrag()
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
                        }
                        // Shadow first so it sits behind the glyph — ordered
                        // after the background it painted a dark shape on top
                        // of the bubble, flattening the depth.
                        .shadow(if (faded) 2.dp else 10.dp, bubbleShape)
                        .clip(bubbleShape)
                        .background(ac.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(bubbleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(ac.primary, ac.primary.copy(alpha = 0.78f))
                                )
                            )
                    )
                    // Edge proximity glow: a subtle white overlay that brightens
                    // the bubble as it nears a snap anchor, like a magnetic field
                    // charging up before connection.
                    if (edgeProximity > 0.01f) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(bubbleShape)
                                .background(Color.White.copy(alpha = edgeProximity * 0.15f))
                        )
                    }
                    Icon(
                        imageVector = launcherIconFor(state.currentView),
                        contentDescription = "Open launcher",
                        tint = ac.onPrimary,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }

        // Snap target preview while dragging: the nearest anchor is always
        // shown so the release destination is telegraphed. Unconditional — the
        // ring moves between anchors instead of appearing/disappearing, so it
        // can never blink mid-drag. A subtle scale + alpha pulse gives it a
        // premium "magnetic" feel.
        if (dragging) {
            val infiniteTransition = rememberInfiniteTransition()
            // Gentle 1.8 s cycle: scale 0.92 → 1.08, alpha 0.35 → 0.65.
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
            val nearestAnchor = snapPositionFor(
                LauncherSnapMath.nearestSnap(dragPos, snapLayout)
            )
            val ringSize = 72f
            val ringShape = RoundedCornerShape(bubbleSize * 0.32f)

            // Fading trail — ghost copies of previous anchor positions that
            // linger briefly as the ring moves between snap points.
            val trailCount = trailEntries.size
            trailEntries.forEachIndexed { i, trailPos ->
                // Oldest → most recent: alpha 0.08 → 0.25, scale 0.7 → 0.9.
                val progress = (i + 1).toFloat() / (trailCount + 1).coerceAtLeast(1)
                val trailAlpha = 0.08f + progress * 0.17f
                val trailScale = 0.7f + progress * 0.2f
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (trailPos.x + (bubbleSize.value - ringSize) / 2f).roundToInt(),
                                (trailPos.y + (bubbleSize.value - ringSize) / 2f).roundToInt()
                            )
                        }
                        .graphicsLayer {
                            scaleX = trailScale
                            scaleY = trailScale
                            alpha = trailAlpha
                        }
                        .border(1.5.dp, ac.primary.copy(alpha = trailAlpha + 0.1f), ringShape)
                )
            }

            // Main pulsing ring at the current nearest anchor.
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (nearestAnchor.x + (bubbleSize.value - ringSize) / 2f).roundToInt(),
                            (nearestAnchor.y + (bubbleSize.value - ringSize) / 2f).roundToInt()
                        )
                    }
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .border(2.dp, ac.primary.copy(alpha = 0.7f), ringShape)
            )
        }

        // Bubble tooltip while hovering.
        val tipCoords = bubbleAnchor
        if (hitboxHovered && !menuOpen && !modePanelOpen && !dragging && !faded && tipCoords != null) {
            val pos = tipCoords.positionInWindow()
            val tip = "${state.currentView.label} — click/right-click for launchpad · hold for modes · drag to move"
            val estW = with(density) { (tip.length * 5.4f + 24).dp.toPx() }
            val estH = with(density) { 30.dp.toPx() }
            val showAbove = dragPos.y > h.value / 2f
            val tipOffset = if (showAbove) {
                IntOffset(
                    (pos.x + tipCoords.size.width / 2 - estW / 2).roundToInt(),
                    (pos.y - estH - 8).roundToInt()
                )
            } else {
                IntOffset(
                    (pos.x + tipCoords.size.width / 2 - estW / 2).roundToInt(),
                    (pos.y + tipCoords.size.height + 8).roundToInt()
                )
            }
            Popup(offset = tipOffset, properties = PopupProperties(focusable = false)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(DsRadius.Sm))
                        .background(sc.surfaceInteractive)
                        .border(1.dp, sc.border.copy(alpha = 0.4f), RoundedCornerShape(DsRadius.Sm))
                        .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Xs)
                ) {
                    Text(
                        text = tip,
                        color = sc.textPrimary,
                        fontSize = DsType.Caption,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Mode switch panel — expands from the bubble (hold / right-click).
        val panelAnchor = bubbleAnchor
        if (modePanelOpen && panelAnchor != null) {
            val pos = panelAnchor.positionInWindow()
            BubbleModePanel(
                current = state.navLayout,
                bubbleWindowPos = IntOffset(pos.x.roundToInt(), pos.y.roundToInt()),
                bubbleSizePx = bubbleDiameterPx + hitboxPaddingPx * 2,
                focusable = openingPanelViaKeyboard,
                onSelect = { mode ->
                    modePanelOpen = false
                    if (mode == NavLayout.Sidebar) state.updateNavLayout(NavLayout.Sidebar)
                    else state.updateNavLayout(NavLayout.Floating)
                    lastActive = System.currentTimeMillis()
                },
                onDismiss = {
                    modePanelOpen = false
                    if (openingPanelViaKeyboard) {
                        openingPanelViaKeyboard = false
                        bubbleFocusRequester.requestFocus()
                    }
                }
            )
        }

        // Full launchpad — a scrimmed overlay that scales out from the bubble
        // with smooth open and close animations.
        if (menuOpen) {
            LaunchpadOverlay(
                state = state,
                bubbleCenter = Offset(
                    dragPos.x + bubbleSize.value / 2f,
                    dragPos.y + bubbleSize.value / 2f
                ),
                onDismiss = {
                    menuOpen = false
                    lastActive = System.currentTimeMillis()
                }
            )
        }
    }
}

// ============================================
// MODE SWITCH PANEL — expands from the bubble
// ============================================

@Composable
private fun BubbleModePanel(
    current: NavLayout,
    bubbleWindowPos: IntOffset,
    bubbleSizePx: Int,
    focusable: Boolean,
    onSelect: (NavLayout) -> Unit,
    onDismiss: () -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()
    val density = LocalDensity.current
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

    val panelW = with(density) { 232.dp.roundToPx() }
    val panelH = with(density) { 152.dp.roundToPx() }
    val gap = with(density) { 12.dp.roundToPx() }

    val openLeft = bubbleWindowPos.x + bubbleSizePx / 2 > windowSize.width / 2
    val rawX = if (openLeft) bubbleWindowPos.x - panelW - gap else bubbleWindowPos.x + bubbleSizePx + gap
    val rawY = (bubbleWindowPos.y + bubbleSizePx / 2 - panelH / 2)
        .coerceIn(0, (windowSize.height - panelH).coerceAtLeast(0))
    val offsetX = rawX.coerceIn(0, (windowSize.width - panelW).coerceAtLeast(0))

    val focusRequesters = remember { listOf(FocusRequester(), FocusRequester()) }
    var selectedIndex by remember { mutableStateOf(if (current == NavLayout.Sidebar) 0 else 1) }
    var firstFocus by remember { mutableStateOf(true) }
    LaunchedEffect(selectedIndex) {
        if (!focusable) return@LaunchedEffect
        if (firstFocus) {
            firstFocus = false
            delay(120)
        }
        focusRequesters[selectedIndex].requestFocus()
    }

    val options = listOf(
        Pair(NavLayout.Sidebar, Icons.Default.ViewSidebar) to "Sidebar",
        Pair(NavLayout.Floating, Icons.Default.ChatBubble) to "Floating"
    )

    Popup(
        onDismissRequest = onDismiss,
        offset = IntOffset(offsetX, rawY),
        properties = PopupProperties(focusable = focusable)
    ) {
        Column(
            modifier = Modifier
                .width(232.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    transformOrigin = TransformOrigin(if (openLeft) 1f else 0f, 0.5f)
                }
                .shadow(20.dp, RoundedCornerShape(DsRadius.Xl))
                .clip(RoundedCornerShape(DsRadius.Xl))
                .background(sc.surfaceElevated.copy(alpha = 0.98f))
                .border(1.dp, sc.border.copy(alpha = 0.35f), RoundedCornerShape(DsRadius.Xl))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Navigation mode",
                color = sc.textMuted,
                fontSize = DsType.Caption,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 2.dp)
            )
            options.forEachIndexed { index, option ->
                val (modeIcon, label) = option
                val (mode, icon) = modeIcon
                val selected = mode == current
                BubbleModeOptionRow(
                    icon = icon,
                    label = label,
                    selected = selected,
                    focused = focusable && selectedIndex == index,
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
private fun BubbleModeOptionRow(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    focused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(DsRadius.Lg)

    Row(
        modifier = modifier
            .clip(shape)
            .background(
                when {
                    selected -> ac.primary.copy(alpha = 0.16f)
                    hovered -> sc.surfaceInteractive
                    else -> Color.Transparent
                }
            )
            .then(
                if (focused || selected) Modifier.border(1.5.dp, ac.primary.copy(alpha = 0.55f), shape)
                else Modifier
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) ac.primary.copy(alpha = 0.2f) else sc.surfaceInteractive),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) ac.primary else sc.textSecondary,
                modifier = Modifier.size(19.dp)
            )
        }
        Text(
            text = label,
            color = if (selected) ac.primary else sc.textPrimary,
            fontSize = DsType.Body,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (selected) {
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ac.primary)
            )
        }
    }
}

// ============================================
// LAUNCHPAD OVERLAY
// A scrimmed launchpad with scale/fade open and
// close animations, scaling from the bubble —
// macOS-launchpad style.
// ============================================

@Composable
private fun LaunchpadOverlay(state: AppState, bubbleCenter: Offset, onDismiss: () -> Unit) {
    val sc = surfaceColors()
    var leaving by remember { mutableStateOf(false) }
    val exitMs = if (state.navigationAnimations && !state.navReducedMotion) 180 else 0
    var panelCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    LaunchedEffect(leaving) {
        if (leaving) {
            delay(exitMs.toLong() + 40)
            onDismiss()
        }
    }
    val close = { leaving = true }

    val scale by animateFloatAsState(
        targetValue = if (leaving) 0.92f else 1f,
        // Open with a spring so the panel settles softly from the bubble;
        // closing stays a quick tween for a decisive dismiss.
        animationSpec = if (leaving) tween(exitMs) else spring(dampingRatio = 0.72f, stiffness = 340f),
        label = "launchpadScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (leaving) 0f else 1f,
        animationSpec = tween(if (leaving) exitMs else 180),
        label = "launchpadAlpha"
    )

    Popup(
        onDismissRequest = { close() },
        properties = PopupProperties(focusable = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f * alpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { close() }
                )
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { panelCoords = it }
                        .graphicsLayer {
                            val pc = panelCoords
                            val ox = if (pc != null) {
                                ((bubbleCenter.x - pc.positionInWindow().x) / pc.size.width).coerceIn(0.05f, 0.95f)
                            } else 0.5f
                            val oy = if (pc != null) {
                                ((bubbleCenter.y - pc.positionInWindow().y) / pc.size.height).coerceIn(0.05f, 0.95f)
                            } else 0.5f
                            transformOrigin = TransformOrigin(ox, oy)
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                ) {
                    LaunchpadPanel(state, onDismiss = close)
                }
            }
        }
    }
}

@Composable
private fun LaunchpadPanel(state: AppState, onDismiss: () -> Unit) {
    val sc = surfaceColors()

    // Responsive: keep a comfortable width on large windows, shrink on phones.
    BoxWithConstraints {
        val panelWidth = minOf(520.dp, (maxWidth - 32.dp).coerceAtLeast(320.dp))
        val navListHeight = (maxHeight - 96.dp).coerceAtLeast(260.dp)
        Column(
            modifier = Modifier
                .width(panelWidth)
                .shadow(40.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(sc.surfaceElevated.copy(alpha = 0.96f))
                .border(1.dp, sc.border.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                .padding(DsSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            Text(
                text = "NAVIGATION",
                color = sc.textMuted,
                fontSize = DsType.Caption,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
            // Full navigation — the same grouped destinations as the dock,
            // never a curated quick-access subset. The list is height-capped
            // so it scrolls instead of overflowing short windows.
            Box(
                Modifier.heightIn(max = navListHeight)
            ) {
                LaunchpadNavList(state, onDismiss)
            }
        }
    }
}

/**
 * Full navigation list for the launchpad: every destination the dock offers,
 * grouped the same way. ↑/↓ move through all items (wrapping), Enter/Space
 * activates the focused row, and focus follows the current view on open.
 */
@Composable
private fun LaunchpadNavList(state: AppState, onDismiss: () -> Unit) {
    val sc = surfaceColors()
    val groups = navGroupsForLaunchpad
    val all = groups.flatMap { (_, items) -> items }
    val focusRequesters = remember { List(all.size) { FocusRequester() } }
    var index by remember {
        mutableStateOf(all.indexOfFirst { it.first == state.currentView }.coerceAtLeast(0))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .onKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (keyEvent.key) {
                    Key.DirectionDown -> {
                        index = (index + 1) % all.size
                        true
                    }
                    Key.DirectionUp -> {
                        index = (index - 1 + all.size) % all.size
                        true
                    }
                    Key.Enter, Key.Spacebar -> {
                        val (view, _) = all[index]
                        state.currentView = view
                        onDismiss()
                        true
                    }
                    else -> false
                }
            },
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        var flat = 0
        groups.forEachIndexed { groupIndex, (groupLabel, items) ->
            // The curated primary set (Home · Library · Browse · Stats · Media
            // · Settings) leads without a header — it sits directly under the
            // launchpad title; secondary groups keep their labels. Study is an
            // action from the Library, not a destination.
            if (groupIndex > 0) {
                Text(
                    text = groupLabel.uppercase(),
                    color = sc.textMuted,
                    fontSize = DsType.Caption,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = DsSpacing.Sm, top = DsSpacing.Sm, bottom = 2.dp)
                )
            }
            items.forEach { (view, icon) ->
                val current = flat
                flat++
                LaunchpadNavRow(
                    view = view,
                    icon = icon,
                    selected = state.currentView == view,
                    focused = index == current,
                    onClick = {
                        state.currentView = view
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesters[current])
                )
            }
        }
    }

    // Let the popup finish grabbing focus on first open so real keyboard
    // focus lands on the current view — arrows work immediately.
    var firstFocus by remember { mutableStateOf(true) }
    LaunchedEffect(index) {
        if (firstFocus) {
            firstFocus = false
            delay(120)
        }
        focusRequesters[index].requestFocus()
    }
}

@Composable
private fun LaunchpadNavRow(
    view: WorkspaceView,
    icon: ImageVector,
    selected: Boolean,
    focused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val focusedSelf by interaction.collectIsFocusedAsState()
    val showFocus = focused || focusedSelf
    val shape = RoundedCornerShape(DsRadius.Lg)

    Row(
        modifier = modifier
            .clip(shape)
            .background(
                when {
                    selected -> ac.primary.copy(alpha = 0.16f)
                    hovered -> sc.surfaceInteractive
                    else -> Color.Transparent
                }
            )
            .then(
                if (showFocus) Modifier.border(1.5.dp, ac.primary.copy(alpha = 0.55f), shape)
                else Modifier
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) ac.primary.copy(alpha = 0.2f) else sc.surfaceInteractive),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) ac.primary else sc.textSecondary,
                modifier = Modifier.size(19.dp)
            )
        }
        Text(
            text = view.label,
            color = if (selected) ac.primary else sc.textPrimary,
            fontSize = DsType.Body,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
        Spacer(Modifier.weight(1f))
        if (selected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ac.primary)
            )
        }
    }
}

/** The current view's icon, falling back to the apps glyph. */
private fun launcherIconFor(view: WorkspaceView): ImageVector =
    allNavItems.firstOrNull { it.first == view }?.second ?: Icons.Default.Apps
