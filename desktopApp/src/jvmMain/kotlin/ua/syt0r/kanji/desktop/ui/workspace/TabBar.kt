package ua.syt0r.kanji.desktop.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.appstate.WorkspaceTab
import ua.syt0r.kanji.desktop.appstate.WorkspaceView
import ua.syt0r.kanji.desktop.designsystem.DsMenuItem
import ua.syt0r.kanji.desktop.designsystem.DsMenuPanel
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import kotlin.math.roundToInt

// ============================================
// WORKSPACE TAB BAR — browser-style sessions
// Every open view is a tab with its own state.
// Click to switch · middle-click closes · drag
// reorders · right/long-press opens the menu ·
// the + button opens a fresh tab of the view
// you are currently in.
// ============================================

@Composable
fun DsWorkspaceTabBar(state: AppState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
    ) {
        state.tabs.forEach { tab ->
            WorkspaceTabChip(state, tab, isActive = tab.id == state.activeTabId)
        }
        NewTabButton(state)
    }
}

@Composable
private fun WorkspaceTabChip(state: AppState, tab: WorkspaceTab, isActive: Boolean) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var menuOpen by remember { mutableStateOf(false) }
    var menuAnchor by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val chipScope = rememberCoroutineScope()

    val chipShape = RoundedCornerShape(DsRadius.Md)

    Row(
        modifier = Modifier
            .onGloballyPositioned { if (menuAnchor != it) menuAnchor = it }
            .clip(chipShape)
            .background(
                when {
                    isActive -> ac.primary.copy(alpha = 0.16f)
                    hovered -> sc.surfaceInteractive
                    else -> sc.surface.copy(alpha = 0.55f)
                }
            )
            .border(
                1.dp,
                when {
                    isActive -> ac.primary.copy(alpha = 0.4f)
                    hovered -> sc.border.copy(alpha = 0.6f)
                    else -> sc.border.copy(alpha = 0.3f)
                },
                chipShape
            )
            .pointerInput(tab.id) {
                // One gesture drives the whole chip: tap activates, middle-click
                // closes, drag reorders, long-press opens the tab menu.
                val scope = chipScope
                awaitEachGesture {
                    // The first event of the gesture carries the down change and
                    // the full button state (Compose 1.8 moved buttons to the
                    // event level, so we read them from here, not the change).
                    val firstEvent = awaitPointerEvent()
                    val down = firstEvent.changes.firstOrNull { it.pressed } ?: return@awaitEachGesture
                    val startIndex = state.tabIndexOf(tab.id)
                    var dragged = false
                    var accumulated = 0f
                    var longPressFired = false
                    val longPressJob = scope.launch {
                        delay(480)
                        if (!dragged) {
                            longPressFired = true
                            menuOpen = true
                        }
                    }
                    try {
                        if (firstEvent.buttons.isTertiaryPressed) {
                            longPressJob.cancel()
                            state.closeTab(tab.id)
                            return@awaitEachGesture
                        }
                        // Right-click opens the tab menu (browser behavior).
                        if (firstEvent.buttons.isSecondaryPressed) {
                            longPressJob.cancel()
                            down.consume()
                            menuOpen = true
                            return@awaitEachGesture
                        }
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            when (event.type) {
                                PointerEventType.Move -> {
                                    if (!dragged &&
                                        (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                                    ) {
                                        dragged = true
                                        longPressJob.cancel()
                                    }
                                    if (dragged) {
                                        accumulated += change.position.x - down.position.x
                                        val step = 28.dp.toPx()
                                        val moved = (accumulated / step).roundToInt()
                                        if (moved != 0) {
                                            val current = state.tabIndexOf(tab.id)
                                            val target = (startIndex + moved).coerceIn(0, state.tabs.lastIndex)
                                            if (target != current) state.moveTab(current, target)
                                        }
                                    }
                                }
                                PointerEventType.Release -> {
                                    longPressJob.cancel()
                                    if (!dragged && !longPressFired && !isActive) {
                                        state.activateTab(tab.id)
                                    }
                                    return@awaitEachGesture
                                }
                                else -> {}
                            }
                        }
                    } finally {
                        longPressJob.cancel()
                    }
                }
            }
            .hoverable(interaction)
            .padding(start = DsSpacing.Md, end = DsSpacing.Xs, top = DsSpacing.Sm, bottom = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
    ) {
        Icon(
            imageVector = tabIcon(tab.view),
            contentDescription = null,
            tint = if (isActive) ac.primary else sc.textSecondary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = tab.title,
            color = if (isActive) sc.textPrimary else sc.textSecondary,
            fontSize = DsType.Body,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
        // Close affordance — always visible on the active tab, on hover otherwise.
        if (isActive || hovered) {
            val closeInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (hovered) sc.surfaceInteractive.copy(alpha = 0.7f) else Color.Transparent
                    )
                    .clickable(interactionSource = closeInteraction, indication = null) {
                        state.closeTab(tab.id)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close tab",
                    tint = sc.textMuted,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }

    // Tab context menu anchored below the chip.
    val coords = menuAnchor
    if (menuOpen && coords != null) {
        val pos = coords.positionInWindow()
        Popup(
            onDismissRequest = { menuOpen = false },
            offset = IntOffset(pos.x.roundToInt(), pos.y.roundToInt() + coords.size.height),
            properties = PopupProperties(focusable = true)
        ) {
            DsMenuPanel(
                menuItems = listOf(
                    DsMenuItem(
                        label = "New tab",
                        icon = Icons.Default.Add,
                        shortcutLabel = "Ctrl T",
                        onAction = {
                            menuOpen = false
                            state.openTab(tab.view, activate = true)
                        }
                    ),
                    DsMenuItem(
                        label = "Duplicate tab",
                        icon = Icons.Default.ContentCopy,
                        onAction = {
                            menuOpen = false
                            state.duplicateTab(tab.id)
                        }
                    ),
                    DsMenuItem(
                        label = "Close tab",
                        icon = Icons.Default.Close,
                        shortcutLabel = "Ctrl W",
                        danger = true,
                        onAction = {
                            menuOpen = false
                            state.closeTab(tab.id)
                        }
                    ),
                    DsMenuItem(
                        label = "Close other tabs",
                        icon = Icons.Default.FilterNone,
                        onAction = {
                            menuOpen = false
                            state.closeOtherTabs(tab.id)
                        }
                    ),
                    DsMenuItem(
                        label = "Close tabs to the right",
                        icon = Icons.Default.KeyboardArrowRight,
                        onAction = {
                            menuOpen = false
                            state.closeTabsAfter(tab.id)
                        }
                    )
                ),
                onDismiss = { menuOpen = false }
            )
        }
    }
}

/** The + button — a fresh tab of the view you are currently in. */
@Composable
private fun NewTabButton(state: AppState) {
    val sc = surfaceColors()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(if (hovered) sc.surfaceInteractive else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null) {
                state.openTab(state.currentView, activate = true)
            }
            .hoverable(interaction),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "New tab",
            tint = sc.textSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

/** The dock icon for a view (same lookup as the nav rail). */
private fun tabIcon(view: WorkspaceView): ImageVector =
    allNavItems.firstOrNull { it.first == view }?.second ?: Icons.Default.Apps
