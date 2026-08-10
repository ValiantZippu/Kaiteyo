package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

// ============================================
// KAITEYO DESIGN SYSTEM — CONTEXT MENUS
// Rich right-click menus with sections, icons,
// checkmarks and keyboard hint labels.
// ============================================

data class DsMenuItem(
    val label: String,
    val icon: ImageVector? = null,
    val shortcutLabel: String? = null,
    val checked: Boolean? = null,
    val danger: Boolean = false,
    val enabled: Boolean = true,
    val onAction: () -> Unit
)

@Composable
fun DsContextMenuHost(
    enabled: Boolean = true,
    menuItems: List<DsMenuItem>,
    content: @Composable () -> Unit
) {
    var menuPosition by remember { mutableStateOf<androidx.compose.ui.unit.IntOffset?>(null) }
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    Box(
        modifier = androidx.compose.ui.Modifier
            .then(if (enabled) androidx.compose.ui.Modifier
                .pointerInput(menuItems) {
                    detectTapGestures(
                        onPress = { offset ->
                            menuPosition = androidx.compose.ui.unit.IntOffset(offset.x.toInt(), offset.y.toInt())
                        }
                    )
                }
            else androidx.compose.ui.Modifier)
    ) {
        content()
    }

    menuPosition?.let { offset ->
        Popup(
            offset = offset,
            properties = PopupProperties(focusable = true)
        ) {
            DsMenuPanel(menuItems, onDismiss = {
                menuPosition = null
            })
        }
    }
}

/**
 * A menu panel with full keyboard access: grabs focus on open, ↑/↓ navigate
 * (wrapping, skipping disabled items), Enter/Space activates, Esc dismisses.
 * The selected item is highlighted with the accent tint.
 */
@Composable
fun DsMenuPanel(
    menuItems: List<DsMenuItem>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val ac = accent()
    val focusRequester = remember { FocusRequester() }

    // Start on the first enabled item.
    val initialIndex = menuItems.indexOfFirst { it.enabled }.coerceAtLeast(0)
    var selectedIndex by remember { mutableStateOf(initialIndex) }

    fun moveSelection(delta: Int) {
        var next = selectedIndex
        repeat(menuItems.size) {
            next = (next + delta + menuItems.size) % menuItems.size
            if (menuItems[next].enabled) {
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
                        val item = menuItems[selectedIndex]
                        if (item.enabled) {
                            onDismiss()
                            item.onAction()
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
            modifier = modifier
                .width(240.dp)
                .clip(RoundedCornerShape(DsRadius.Md))
                .background(sc.surfaceInteractive)
                .padding(DsSpacing.Xs)
        ) {
            menuItems.forEachIndexed { index, item ->
                DsMenuItemRow(
                    item = item,
                    onClick = {
                        onDismiss()
                        item.onAction()
                    },
                    modifier = if (index == selectedIndex) {
                        Modifier
                            .clip(RoundedCornerShape(DsRadius.Sm))
                            .background(ac.primary.copy(alpha = 0.14f))
                    } else {
                        Modifier
                    }
                )
            }
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
fun DsMenuItemRow(item: DsMenuItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = when {
            item.danger && hovered -> Color(0xFFFF5D5D).copy(alpha = 0.16f)
            item.danger -> Color(0xFFFF5D5D).copy(alpha = 0.08f)
            hovered && item.enabled -> sc.surfaceInteractive
            else -> Color.Transparent
        },
        animationSpec = tween(140),
        label = "menuItemBg"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Sm))
            .background(bg)
            .then(
                androidx.compose.ui.Modifier
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = item.enabled,
                        onClick = onClick
                    )
                    .hoverable(interaction)
                    .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Sm)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.icon != null) {
            androidx.compose.material3.Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (item.danger) Color(0xFFFF6B6B) else sc.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(DsSpacing.Sm))
        }
        Text(
            text = item.label,
            color = if (item.danger) Color(0xFFFF6B6B) else if (item.enabled) sc.textPrimary else sc.textMuted,
            fontSize = DsType.Body,
            modifier = Modifier.weight(1f)
        )
        if (item.checked != null) {
            Text(
                text = if (item.checked) "✓" else "",
                color = ac.primary,
                fontSize = DsType.Label,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(DsSpacing.Sm))
        }
        if (item.shortcutLabel != null) {
            Text(
                text = item.shortcutLabel,
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
    }
}

@Composable
fun DsMenuDivider(modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = DsSpacing.Md)
            .background(sc.border.copy(alpha = 0.4f))
    )
}
