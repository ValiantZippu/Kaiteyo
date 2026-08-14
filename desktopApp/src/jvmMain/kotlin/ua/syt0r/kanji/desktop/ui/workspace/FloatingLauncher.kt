package ua.syt0r.kanji.desktop.ui.workspace

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
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

/** The views surfaced by the launcher menu — the everyday destinations. */
private val launcherTargets: List<WorkspaceView> = listOf(
    WorkspaceView.Dashboard,
    WorkspaceView.Library,
    WorkspaceView.Review,
    WorkspaceView.Dictionary,
    WorkspaceView.Media,
    WorkspaceView.Ocr,
    WorkspaceView.Mining,
    WorkspaceView.Statistics,
    WorkspaceView.Settings
)

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
        val compactWindow = w < 720.dp
        val edgeInset = if (compactWindow) 72.dp else 0.dp
        val snapPoint = state.launcherSnapPoint

        // Position as fractions of the window. Compact windows remember a
        // separate spot so the bubble never fights the tab bar / gesture zones.
        var dragPos by remember(w, h, compactWindow) {
            mutableStateOf(
                Offset(
                    w.value * (if (compactWindow) state.launcherPosXPhone else state.launcherPosX),
                    h.value * (if (compactWindow) state.launcherPosYPhone else state.launcherPosY)
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
                // Higher animation speed = snappier spring (stiffness scales inversely).
                spring(
                    dampingRatio = 0.55f,
                    stiffness = 320f / state.launcherAnimationSpeed.coerceAtLeast(0.25f)
                )
            } else {
                tween(0)
            },
            label = "launcherPos"
        )

        val bubbleDiameterPx = with(density) { bubbleSize.roundToPx() }
        val hitboxPaddingPx = with(density) { hitboxExtra.roundToPx() }

        val snapPositionFor: (LauncherSnapPoint) -> Offset = { snap ->
            val size = bubbleSize.value
            when (snap) {
                LauncherSnapPoint.TopLeft, LauncherSnapPoint.LeftTop -> Offset(0f, edgeInset.value)
                LauncherSnapPoint.TopCenter -> Offset((w.value - size) / 2f, edgeInset.value)
                LauncherSnapPoint.TopRight, LauncherSnapPoint.RightTop -> Offset(w.value - size, edgeInset.value)
                LauncherSnapPoint.BottomLeft, LauncherSnapPoint.LeftBottom -> Offset(0f, h.value - edgeInset.value - size)
                LauncherSnapPoint.BottomCenter -> Offset((w.value - size) / 2f, h.value - edgeInset.value - size)
                LauncherSnapPoint.BottomRight, LauncherSnapPoint.RightBottom -> Offset(w.value - size, h.value - edgeInset.value - size)
                LauncherSnapPoint.LeftCenter -> Offset(0f, (h.value - size) / 2f)
                LauncherSnapPoint.RightCenter -> Offset(w.value - size, (h.value - size) / 2f)
            }
        }

        fun nearestSnap(pos: Offset): LauncherSnapPoint =
            LauncherSnapPoint.entries.minByOrNull { snap ->
                val anchor = snapPositionFor(snap)
                val dx = pos.x - anchor.x
                val dy = pos.y - anchor.y
                dx * dx + dy * dy
            } ?: LauncherSnapPoint.BottomRight

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
            val nearest = nearestSnap(dragPos)
            val anchor = snapPositionFor(nearest)
            state.updateLauncherSnapPoint(nearest)
            dragPos = anchor
            target = anchor
            state.setLauncherPos(anchor.x / w.value, anchor.y / h.value, compact = compactWindow)
        }

        var faded by remember { mutableStateOf(false) }
        var lastActive by remember { mutableStateOf(System.currentTimeMillis()) }
        // Capture mode pre-opens the requested state so screenshots are
        // deterministic (see LocalCaptureState).
        var menuOpen by remember { mutableStateOf(captureState == "launchpad" || captureState == "strip") }
        var modePanelOpen by remember { mutableStateOf(captureState == "menu") }
        var bubbleAnchor by remember { mutableStateOf<LayoutCoordinates?>(null) }
        var dragging by remember { mutableStateOf(false) }
        var openingPanelViaKeyboard by remember { mutableStateOf(false) }
        val bubbleFocusRequester = remember { FocusRequester() }

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
                IntOffset(animatedPos.x.roundToInt(), animatedPos.y.roundToInt())
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
                            scaleX = hoverScale * pressScale
                            scaleY = hoverScale * pressScale
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

                                val longPressJob = scope.launch {
                                    delay(LongPressTimeoutMs)
                                    longPressFired = true
                                    modePanelOpen = true
                                    menuOpen = false
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
                                                    menuOpen = false
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
                                                        val slop = viewConfiguration.touchSlop
                                                        if ((change.position - down.position).getDistance() > slop) {
                                                            dragged = true
                                                            longPressJob.cancel()
                                                            dragging = true
                                                            menuOpen = false
                                                            modePanelOpen = false
                                                        }
                                                    }
                                                    if (dragged) {
                                                        change.consume()
                                                        dragPos += change.position - previous
                                                        target = dragPos
                                                        previous = change.position
                                                    }
                                                }
                                                PointerEventType.Release -> {
                                                    longPressJob.cancel()
                                                    if (dragged) {
                                                        dragging = false
                                                        finishDrag()
                                                    } else if (!longPressFired) {
                                                        menuOpen = !menuOpen
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
                        .clip(bubbleShape)
                        .background(ac.primary)
                        .shadow(if (faded) 2.dp else 10.dp, bubbleShape),
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
                    Icon(
                        imageVector = launcherIconFor(state.currentView),
                        contentDescription = "Open launcher",
                        tint = ac.onPrimary,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }

        // Bubble tooltip while hovering.
        val tipCoords = bubbleAnchor
        if (hitboxHovered && !menuOpen && !modePanelOpen && !dragging && !faded && tipCoords != null) {
            val pos = tipCoords.positionInWindow()
            val tip = "${state.currentView.label} — click for launchpad · right-click or hold for modes · drag to move"
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
        animationSpec = tween(if (leaving) exitMs else 200),
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
        val panelWidth = minOf(540.dp, (maxWidth - 32.dp).coerceAtLeast(320.dp))
        Column(
            modifier = Modifier
                .width(panelWidth)
                .shadow(40.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(sc.surfaceElevated.copy(alpha = 0.92f))
                .border(1.dp, sc.border.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                .padding(DsSpacing.Xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
        ) {
        Text(
            text = "LAUNCHPAD",
            color = sc.textMuted,
            fontSize = DsType.Caption,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp
        )
        // 5×2 grid of large icon tiles — arrow-key navigable (see below).
        LaunchpadTileGrid(state, onDismiss)
        // Window controls — Restore/Minimize/Maximize/Close, so the launchpad
        // doubles as a window menu without needing the title bar.
        val controls = LocalWindowControls.current
        if (controls != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(sc.border.copy(alpha = 0.25f))
            )
            LaunchpadWindowControls(controls)
        }
        }
    }
}

/**
 * Keyboard-accessible tile grid: ←/→ move within a row, ↑/↓ jump rows (both
 * wrapping, with the short last row clamped), Enter/Space activates the
 * focused tile through its native clickable, and the focused tile shows an
 * accent ring. Focus starts on the current view, if it's a launcher target.
 */
@Composable
private fun LaunchpadTileGrid(state: AppState, onDismiss: () -> Unit) {
    val targets = launcherTargets
    val cols = 5
    val rows = targets.chunked(cols)
    val focusRequesters = remember { List(targets.size) { FocusRequester() } }
    var index by remember {
        mutableStateOf(targets.indexOfFirst { it == state.currentView }.coerceAtLeast(0))
    }

    fun move(dx: Int, dy: Int) {
        val rowCount = rows.size
        val row = index / cols
        val col = index % cols
        val newRow = ((row + dy) % rowCount + rowCount) % rowCount
        val newCol = ((col + dx) % cols + cols) % cols
        val rowSize = minOf(cols, targets.size - newRow * cols)
        index = newRow * cols + minOf(newCol, rowSize - 1)
    }

    Column(
        modifier = Modifier.onKeyEvent { keyEvent ->
            if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
            when (keyEvent.key) {
                Key.DirectionRight -> {
                    move(1, 0); true
                }
                Key.DirectionLeft -> {
                    move(-1, 0); true
                }
                Key.DirectionDown -> {
                    move(0, 1); true
                }
                Key.DirectionUp -> {
                    move(0, -1); true
                }
                else -> false
            }
        },
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        rows.forEachIndexed { row, rowTargets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
            ) {
                rowTargets.forEachIndexed { col, view ->
                    val flat = row * cols + col
                    LaunchpadTileBig(
                        view = view,
                        selected = state.currentView == view,
                        focused = index == flat,
                        onClick = {
                            state.currentView = view
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequesters[flat])
                    )
                }
                repeat(cols - rowTargets.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    // Let the popup finish grabbing focus on first open so real keyboard
    // focus lands on the selected tile — arrows work immediately, and
    // capture mode screenshots the genuine focus ring.
    var firstFocus by remember { mutableStateOf(true) }
    LaunchedEffect(index) {
        if (firstFocus) {
            firstFocus = false
            delay(120)
        }
        focusRequesters[index].requestFocus()
    }
}

/**
 * Keyboard-accessible window-control strip: ←/→ (and ↑/↓) move focus across
 * the four buttons (wrapping, skipping disabled ones), Tab reaches them, and
 * each button shows a focus ring while selected or focused.
 */
@Composable
private fun LaunchpadWindowControls(controls: WindowControls) {
    val focusRequesters = remember { List(4) { FocusRequester() } }
    val buttons = listOf(
        Triple(Icons.Default.Restore, "Restore", controls.isMaximized) to controls.onToggleMaximize,
        Triple(Icons.Default.Remove, "Minimize", true) to controls.onMinimize,
        Triple(Icons.Default.OpenInFull, "Maximize", !controls.isMaximized) to controls.onToggleMaximize,
        Triple(Icons.Default.Close, "Close", true) to controls.onClose
    )

    // Start on the first enabled action (Minimize while floating, Restore while maximized).
    var selectedIndex by remember {
        mutableStateOf(buttons.indexOfFirst { it.first.third }.coerceAtLeast(0))
    }

    fun move(delta: Int) {
        var next = selectedIndex
        repeat(buttons.size) {
            next = (next + delta + buttons.size) % buttons.size
            if (buttons[next].first.third) {
                selectedIndex = next
                return
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (keyEvent.key) {
                    Key.DirectionRight, Key.DirectionDown -> {
                        move(1)
                        true
                    }
                    Key.DirectionLeft, Key.DirectionUp -> {
                        move(-1)
                        true
                    }
                    else -> false
                }
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        buttons.forEachIndexed { index, (def, action) ->
            LaunchpadWindowButton(
                icon = def.first,
                contentDescription = def.second,
                enabled = def.third,
                onClick = action,
                focused = index == selectedIndex,
                modifier = Modifier.focusRequester(focusRequesters[index])
            )
        }
    }
    // Same settle-on-open as the tile grid: the popup's own focus grab must
    // not win the race, or arrows would go nowhere on first open.
    var firstFocus by remember { mutableStateOf(true) }
    LaunchedEffect(selectedIndex) {
        if (firstFocus) {
            firstFocus = false
            delay(120)
        }
        focusRequesters[selectedIndex].requestFocus()
    }
}

@Composable
private fun LaunchpadWindowButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    danger: Boolean = false,
    focused: Boolean = false,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val focusedSelf by interaction.collectIsFocusedAsState()
    val showFocus = focused || focusedSelf
    Box(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    !enabled -> Color.Transparent
                    hovered && danger -> Color(0xFFE81123).copy(alpha = 0.18f)
                    hovered -> sc.surfaceInteractive
                    else -> Color.Transparent
                }
            )
            .then(
                if (showFocus) Modifier.border(1.5.dp, ac.primary, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .hoverable(interaction),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> sc.textMuted.copy(alpha = 0.4f)
                danger -> Color(0xFFFF6B6B)
                else -> sc.textSecondary
            },
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun LaunchpadTileBig(
    view: WorkspaceView,
    selected: Boolean,
    onClick: () -> Unit,
    focused: Boolean = false,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val focusedSelf by interaction.collectIsFocusedAsState()
    val showFocus = focused || focusedSelf

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                when {
                    selected -> ac.primary.copy(alpha = 0.16f)
                    hovered -> sc.surfaceInteractive.copy(alpha = 0.8f)
                    else -> Color.Transparent
                }
            )
            .then(
                if (showFocus) Modifier.border(1.5.dp, ac.primary, RoundedCornerShape(20.dp))
                else Modifier
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(vertical = DsSpacing.Sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (selected || hovered) ac.primary.copy(alpha = 0.2f)
                    else sc.surfaceInteractive.copy(alpha = 0.7f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = launcherIconFor(view),
                contentDescription = null,
                tint = if (selected) ac.primary else sc.textSecondary,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = view.label,
            color = if (selected) ac.primary else sc.textPrimary,
            fontSize = DsType.Caption,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

/** The current view's icon, falling back to the apps glyph. */
private fun launcherIconFor(view: WorkspaceView): ImageVector =
    allNavItems.firstOrNull { it.first == view }?.second ?: Icons.Default.Apps
