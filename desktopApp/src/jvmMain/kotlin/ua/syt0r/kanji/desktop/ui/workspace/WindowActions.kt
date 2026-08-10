package ua.syt0r.kanji.desktop.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import ua.syt0r.kanji.desktop.designsystem.DsIconButton
import ua.syt0r.kanji.desktop.designsystem.DsMenuDivider
import ua.syt0r.kanji.desktop.designsystem.DsMenuItem
import ua.syt0r.kanji.desktop.designsystem.DsMenuItemRow
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors

// ============================================
// WINDOW ACTIONS
// Window controls exposed to the workspace UI
// by the window shell, plus the shared
// Restore · Minimize · Maximize · Close menu
// used by both the title bar and the dock.
// ============================================

data class WindowControls(
    val isMaximized: Boolean,
    val onMinimize: () -> Unit,
    val onToggleMaximize: () -> Unit,
    val onClose: () -> Unit
)

/** Provided by the window shell; null when no window is available (e.g. previews). */
val LocalWindowControls = staticCompositionLocalOf<WindowControls?> { null }

/**
 * The Restore/Minimize/Maximize/Close panel. Fully keyboard-accessible:
 * grabs focus on open, arrows move the selection (wrapping, skipping disabled
 * actions), Enter/Space activates, Esc dismisses.
 */
@Composable
fun WindowActionsMenu(
    controls: WindowControls,
    onDismiss: () -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()
    val focusRequester = remember { FocusRequester() }

    val actions: List<Pair<DsMenuItem, () -> Unit>> = listOf(
        DsMenuItem(
            label = "Restore",
            icon = Icons.Default.Restore,
            enabled = controls.isMaximized,
            onAction = controls.onToggleMaximize
        ) to controls.onToggleMaximize,
        DsMenuItem(
            label = "Minimize",
            icon = Icons.Default.Remove,
            onAction = controls.onMinimize
        ) to controls.onMinimize,
        DsMenuItem(
            label = "Maximize",
            icon = Icons.Default.OpenInFull,
            enabled = !controls.isMaximized,
            onAction = controls.onToggleMaximize
        ) to controls.onToggleMaximize,
        DsMenuItem(
            label = "Close",
            icon = Icons.Default.Close,
            danger = true,
            onAction = controls.onClose
        ) to controls.onClose
    )

    // Start on the first enabled action (Minimize while floating, Restore while maximized).
    val initialIndex = actions.indexOfFirst { it.first.enabled }.coerceAtLeast(0)
    var selectedIndex by remember { mutableStateOf(initialIndex) }

    fun moveSelection(delta: Int) {
        var next = selectedIndex
        repeat(actions.size) {
            next = (next + delta + actions.size) % actions.size
            if (actions[next].first.enabled) {
                selectedIndex = next
                return
            }
        }
    }

    Box(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (keyEvent.key) {
                    Key.DirectionDown -> {
                        moveSelection(1)
                        true
                    }
                    Key.DirectionUp -> {
                        moveSelection(-1)
                        true
                    }
                    Key.Enter, Key.Spacebar -> {
                        val (item, action) = actions[selectedIndex]
                        if (item.enabled) {
                            onDismiss()
                            action()
                        }
                        true
                    }
                    Key.Escape -> {
                        onDismiss()
                        true
                    }
                    else -> false
                }
            }
    ) {
        Column(
            modifier = Modifier
                .width(220.dp)
                .clip(RoundedCornerShape(DsRadius.Md))
                .background(sc.surfaceElevated)
                .border(1.dp, sc.border.copy(alpha = 0.45f), RoundedCornerShape(DsRadius.Md))
                .padding(DsSpacing.Xs)
        ) {
            actions.forEachIndexed { index, (item, action) ->
                val selected = index == selectedIndex
                DsMenuItemRow(
                    item = item,
                    onClick = {
                        onDismiss()
                        action()
                    },
                    modifier = if (selected) {
                        Modifier
                            .clip(RoundedCornerShape(DsRadius.Sm))
                            .background(ac.primary.copy(alpha = 0.14f))
                    } else {
                        Modifier
                    }
                )
                if (index == actions.lastIndex - 1) {
                    DsMenuDivider()
                }
            }
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

/**
 * Dock button that opens the window-actions menu below itself. Renders nothing
 * when no window shell is present (LocalWindowControls is null), so the dock
 * stays usable in contexts without a window.
 */
@Composable
fun DsWindowMenuButton(modifier: Modifier = Modifier) {
    val controls = LocalWindowControls.current
    if (controls == null) return

    var open by remember { mutableStateOf(false) }
    var anchor by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = modifier.onGloballyPositioned {
            if (anchor != it) anchor = it
        }
    ) {
        DsIconButton(
            icon = Icons.Default.OpenInFull,
            onClick = { open = true },
            contentDescription = "Window controls"
        )
    }

    val coords = anchor
    if (open && coords != null) {
        val pos = coords.positionInWindow()
        val density = LocalDensity.current
        Popup(
            onDismissRequest = { open = false },
            offset = IntOffset(
                pos.x.roundToInt(),
                pos.y.roundToInt() + coords.size.height + with(density) { 4.dp.roundToPx() }
            ),
            properties = PopupProperties(focusable = true)
        ) {
            WindowActionsMenu(controls = controls, onDismiss = { open = false })
        }
    }
}
