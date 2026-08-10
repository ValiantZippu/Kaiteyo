package ua.syt0r.kanji.desktop.ui.workspace

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import kotlin.math.roundToInt
import kotlin.math.sqrt
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.LauncherAnchor
import ua.syt0r.kanji.desktop.appstate.LauncherIconSize
import ua.syt0r.kanji.desktop.appstate.LauncherSize
import ua.syt0r.kanji.desktop.appstate.NavLayout
import ua.syt0r.kanji.desktop.appstate.WorkspaceView
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors

// ============================================
// FLOATING LAUNCHER
// An Android-chat-bubble style quick launcher.
// Draggable, snaps to edges/corners with a
// spring, auto-fades after inactivity, and
// opens a compact quick-nav popup. Its position
// is remembered across sessions.
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
        val hitboxExtra = if (state.navigationLargerHitbox) 18.dp else 8.dp
        // Phones/compact windows keep the bubble clear of the tab bars.
        val compactWindow = w < 720.dp
        val edgeInset = if (compactWindow) 72.dp else 0.dp

        // Position as fractions of the window. Phones remember a separate
        // spot so the bubble never fights the tab bar or gesture zones.
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

        var faded by remember { mutableStateOf(false) }
        var lastActive by remember { mutableStateOf(System.currentTimeMillis()) }
        // Capture mode pre-opens the requested state so screenshots are
        // deterministic (see LocalCaptureState).
        var menuOpen by remember { mutableStateOf(captureState == "launchpad" || captureState == "strip") }
        var expanded by remember { mutableStateOf(captureState == "menu") }
        var chipHovered by remember { mutableStateOf(captureState == "menu") }
        var bubbleAnchor by remember { mutableStateOf<LayoutCoordinates?>(null) }
        var initialized by remember { mutableStateOf(false) }
        var dragging by remember { mutableStateOf(false) }
        // True when the chips popup was opened with the keyboard (Enter/Space on
        // the bubble) — makes the popup focusable so arrows work inside it.
        var chipsViaKeyboard by remember { mutableStateOf(false) }
        val bubbleFocusRequester = remember { FocusRequester() }

        // Turning auto-fade off restores full visibility immediately.
        LaunchedEffect(state.launcherAutoFade) {
            if (!state.launcherAutoFade) faded = false
        }

        // Auto-fade after inactivity. Event-driven: the effect restarts on any
        // activity or fade-setting change, sleeps exactly the fade delay, then
        // fades once. No perpetual polling while the window is idle — the app
        // can rest between interactions, keeping drag and hover smooth.
        LaunchedEffect(state.launcherAutoFade, state.launcherFadeDelayMs, menuOpen, lastActive, captureState) {
            if (!state.launcherAutoFade || menuOpen || captureState != null) return@LaunchedEffect
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
        LaunchedEffect(hitboxHovered, menuOpen) {
            if (hitboxHovered || menuOpen) {
                faded = false
                lastActive = System.currentTimeMillis()
            }
        }

        // Bubble hover expansion: hovering the bubble, its hitbox, or the
        // quick-control chips expands the bubble; leaving collapses it after a
        // short grace so the pointer can travel onto the chips.
        LaunchedEffect(hitboxHovered, chipHovered, menuOpen) {
            if (hitboxHovered || chipHovered || menuOpen) {
                expanded = true
            } else {
                delay(350)
                expanded = false
            }
        }
        LaunchedEffect(menuOpen) {
            if (menuOpen) expanded = false
        }

        // Changing "Default position" in settings moves it live (and is
        // persisted as the new remembered spot). Skipped on first launch
        // so the remembered position always wins.
        LaunchedEffect(state.launcherDefaultPosition) {
            if (!initialized) {
                initialized = true
                return@LaunchedEffect
            }
            if (w.value <= 0f || h.value <= 0f) return@LaunchedEffect
            val anchor = anchorFor(state.launcherDefaultPosition, bubbleSize, w, h, edgeInset, edgeInset)
            dragPos = anchor
            target = anchor
            state.setLauncherPos(anchor.x / w.value, anchor.y / h.value, compact = compactWindow)
        }

        fun finishDrag() {
            val snapped = if (state.launcherSnapEnabled) {
                nearestAnchor(dragPos, bubbleSize, w, h, state.launcherSnapSensitivity, edgeInset, edgeInset)
            } else {
                Offset(
                    dragPos.x.coerceIn(0f, w.value - bubbleSize.value),
                    dragPos.y.coerceIn(edgeInset.value, h.value - edgeInset.value - bubbleSize.value)
                )
            }
            dragPos = snapped
            target = snapped
            state.setLauncherPos(snapped.x / w.value, snapped.y / h.value, compact = compactWindow)
        }

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
                // Draggable bubble. The auto-fade opacity is applied here so a
                // faded launcher truly becomes translucent (still fully clickable).
                Box(
                    modifier = Modifier
                        .size(bubbleSize)
                        .graphicsLayer { alpha = opacity }
                        .shadow(if (faded) 2.dp else 10.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(ac.primary, ac.primary.copy(alpha = 0.72f))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    dragging = true
                                    faded = false
                                    lastActive = System.currentTimeMillis()
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    if (menuOpen) menuOpen = false
                                    lastActive = System.currentTimeMillis()
                                    dragPos += amount
                                    target = dragPos
                                },
                                onDragEnd = {
                                    dragging = false
                                    finishDrag()
                                },
                                onDragCancel = {
                                    dragging = false
                                    finishDrag()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(bubbleFocusRequester)
                            .onPreviewKeyEvent { keyEvent ->
                                // Keyboard activation (Enter/Space) makes the
                                // chips popup focusable so arrows work inside it.
                                if (keyEvent.type == KeyEventType.KeyDown &&
                                    (keyEvent.key == Key.Enter || keyEvent.key == Key.Spacebar)
                                ) {
                                    chipsViaKeyboard = true
                                }
                                false // let the clickable handle the activation
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (menuOpen) {
                                        menuOpen = false
                                    } else if (expanded) {
                                        expanded = false
                                        chipsViaKeyboard = false
                                    } else {
                                        expanded = true
                                    }
                                    faded = false
                                    lastActive = System.currentTimeMillis()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = launcherIconFor(state.currentView),
                            contentDescription = "Open launcher",
                            tint = ac.onPrimary,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
            }
        }

        // Bubble tooltip while hovering (hidden while the quick controls show).
        val tipCoords = bubbleAnchor
        if (hitboxHovered && !menuOpen && !dragging && !faded && !expanded && tipCoords != null) {
            val pos = tipCoords.positionInWindow()
            val tip = "${state.currentView.label} — click for quick actions, drag to move"
            val estW = with(density) { (tip.length * 6 + 24).dp.toPx() }
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

        // Quick controls beside the bubble while it is hover-expanded.
        val chipAnchor = bubbleAnchor
        if (expanded && !menuOpen && chipAnchor != null) {
            val pos = chipAnchor.positionInWindow()
            val chipsOnLeft = dragPos.x > w.value / 2f
            val chipW = with(density) { 252.dp.toPx() }
            val chipH = with(density) { 46.dp.toPx() }
            val chipsX = if (chipsOnLeft) pos.x - chipW else pos.x + chipAnchor.size.width
            val chipsY = pos.y + chipAnchor.size.height / 2f - chipH / 2f
            Popup(
                offset = IntOffset(chipsX.roundToInt(), chipsY.roundToInt()),
                onDismissRequest = {
                    if (chipsViaKeyboard) {
                        chipsViaKeyboard = false
                        expanded = false
                        bubbleFocusRequester.requestFocus()
                    }
                },
                properties = PopupProperties(focusable = chipsViaKeyboard)
            ) {
                BubbleQuickControls(
                    state = state,
                    focusable = chipsViaKeyboard,
                    onAct = {
                        expanded = false
                        if (chipsViaKeyboard) {
                            chipsViaKeyboard = false
                            bubbleFocusRequester.requestFocus()
                        }
                    },
                    onLaunchpad = {
                        menuOpen = true
                        expanded = false
                        lastActive = System.currentTimeMillis()
                    },
                    onHoverChange = { chipHovered = it }
                )
            }
        }

        // Full launchpad — a centered, scrimmed overlay that scales out from
        // the bubble with smooth open and close animations.
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
// BUBBLE QUICK CONTROLS (hover expansion)
// A slim chip row beside the bubble with the
// three navigation actions — Full Sidebar,
// Compact Sidebar, Launchpad.
// ============================================

@Composable
private fun BubbleQuickControls(
    state: AppState,
    focusable: Boolean,
    onAct: () -> Unit,
    onLaunchpad: () -> Unit,
    onHoverChange: (Boolean) -> Unit
) {
    val sc = surfaceColors()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    LaunchedEffect(hovered) { onHoverChange(hovered) }

    // Keyboard navigation across the three chips (only when the popup was
    // opened with the keyboard): ←/→ wrap, Enter/Space activates the focused
    // chip via its native clickable, focus shows an accent ring.
    val chips = listOf(
        Triple(Icons.Default.ViewSidebar, "Sidebar") to {
            state.updateNavLayout(NavLayout.Expanded)
            onAct()
        },
        Triple(Icons.Default.Apps, "Compact") to {
            state.updateNavLayout(NavLayout.Compact)
            onAct()
        },
        Triple(Icons.Default.GridView, "Launchpad") to onLaunchpad
    )
    val focusRequesters = remember { List(chips.size) { FocusRequester() } }
    var selectedIndex by remember { mutableStateOf(0) }
    var firstFocus by remember { mutableStateOf(true) }

    // Same settle-on-open as the tile grid: let the popup's focus grab land
    // first, then move real focus to the selected chip.
    LaunchedEffect(selectedIndex) {
        if (!focusable) return@LaunchedEffect
        if (firstFocus) {
            firstFocus = false
            delay(120)
        }
        focusRequesters[selectedIndex].requestFocus()
    }

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.85f,
        animationSpec = if (state.navigationAnimations && !state.navReducedMotion) {
            spring(dampingRatio = 0.6f, stiffness = 460f)
        } else {
            tween(0)
        },
        label = "chipScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(if (state.navigationAnimations && !state.navReducedMotion) 160 else 0),
        label = "chipAlpha"
    )

    Row(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = alpha }
            .shadow(18.dp, RoundedCornerShape(DsRadius.Xl))
            .clip(RoundedCornerShape(DsRadius.Xl))
            .background(sc.surfaceElevated.copy(alpha = 0.96f))
            .border(1.dp, sc.border.copy(alpha = 0.4f), RoundedCornerShape(DsRadius.Xl))
            .padding(4.dp)
            .hoverable(interaction)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (keyEvent.key) {
                    Key.DirectionRight -> {
                        selectedIndex = (selectedIndex + 1) % chips.size
                        true
                    }
                    Key.DirectionLeft -> {
                        selectedIndex = (selectedIndex - 1 + chips.size) % chips.size
                        true
                    }
                    else -> false
                }
            },
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        chips.forEachIndexed { index, (def, action) ->
            QuickChip(
                icon = def.first,
                label = def.second,
                onClick = action,
                focused = focusable && index == selectedIndex,
                modifier = Modifier.focusRequester(focusRequesters[index])
            )
        }
    }
}

@Composable
private fun QuickChip(
    icon: ImageVector,
    label: String,
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

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(if (hovered) ac.primary.copy(alpha = 0.14f) else Color.Transparent)
            .then(
                if (showFocus) Modifier.border(1.5.dp, ac.primary, RoundedCornerShape(DsRadius.Md))
                else Modifier
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (hovered) ac.primary else sc.textSecondary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            color = sc.textPrimary,
            fontSize = DsType.Caption,
            fontWeight = FontWeight.Medium
        )
    }
}

// ============================================
// LAUNCHPAD OVERLAY
// A centered, scrimmed launchpad with scale/fade
// open and close animations, scaling from the
// bubble toward center — macOS-launchpad style.
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
                            alpha = alpha
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

/**
 * Compute the anchor point (bubble top-left) for a named snap target.
 * [topInset]/[bottomInset] keep the bubble clear of phone tab bars.
 */
private fun anchorFor(
    anchor: LauncherAnchor,
    bubble: androidx.compose.ui.unit.Dp,
    w: androidx.compose.ui.unit.Dp,
    h: androidx.compose.ui.unit.Dp,
    topInset: androidx.compose.ui.unit.Dp = 0.dp,
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp
): Offset {
    val usableH = (h.value - topInset.value - bottomInset.value).coerceAtLeast(0f)
    return when (anchor) {
        LauncherAnchor.Left -> Offset(0f, topInset.value + usableH / 2f - bubble.value / 2f)
        LauncherAnchor.Right -> Offset(w.value - bubble.value, topInset.value + usableH / 2f - bubble.value / 2f)
        LauncherAnchor.TopLeft -> Offset(0f, topInset.value)
        LauncherAnchor.TopRight -> Offset(w.value - bubble.value, topInset.value)
        LauncherAnchor.BottomLeft -> Offset(0f, h.value - bottomInset.value - bubble.value)
        LauncherAnchor.BottomRight -> Offset(w.value - bubble.value, h.value - bottomInset.value - bubble.value)
    }
}

/**
 * Snap to the closest edge/corner given the bubble's top-left position.
 * [sensitivity] scales the snap radius (how far the bubble can be dropped
 * from an anchor and still snap to it): 0 ≈ free placement, 1 = default,
 * 2 = snaps from anywhere. Insets keep the bubble off phone tab bars.
 */
private fun nearestAnchor(
    pos: Offset,
    bubble: androidx.compose.ui.unit.Dp,
    w: androidx.compose.ui.unit.Dp,
    h: androidx.compose.ui.unit.Dp,
    sensitivity: Float = 1f,
    topInset: androidx.compose.ui.unit.Dp = 0.dp,
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp
): Offset {
    val center = Offset(pos.x + bubble.value / 2f, pos.y + bubble.value / 2f)
    val usableH = (h.value - topInset.value - bottomInset.value).coerceAtLeast(0f)
    val candidates = mapOf(
        LauncherAnchor.Left to Offset(0f, topInset.value + usableH / 2f),
        LauncherAnchor.Right to Offset(w.value, topInset.value + usableH / 2f),
        LauncherAnchor.TopLeft to Offset(0f, topInset.value),
        LauncherAnchor.TopRight to Offset(w.value, topInset.value),
        LauncherAnchor.BottomLeft to Offset(0f, h.value - bottomInset.value),
        LauncherAnchor.BottomRight to Offset(w.value, h.value - bottomInset.value)
    )
    val best = candidates.minByOrNull { (_, p) ->
        val dx = center.x - p.x
        val dy = center.y - p.y
        dx * dx + dy * dy
    }!!
    val anchorCenter = candidates.getValue(best.key)
    val dx = center.x - anchorCenter.x
    val dy = center.y - anchorCenter.y
    val distance = sqrt(dx * dx + dy * dy)
    val snapRadius = minOf(w.value, h.value) * 0.22f * sensitivity.coerceAtLeast(0.05f)
    if (distance > snapRadius) {
        // Too far from any anchor — keep the free position, clamped in-window.
        return Offset(
            pos.x.coerceIn(0f, w.value - bubble.value),
            pos.y.coerceIn(topInset.value, h.value - bottomInset.value - bubble.value)
        )
    }
    return anchorFor(best.key, bubble, w, h, topInset, bottomInset)
}
