package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

// ============================================
// KAITEYO DESIGN SYSTEM — MISC COMPONENTS
// ============================================

// --- DsChip ---

@Composable
fun DsChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Full)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor = when {
        selected -> sc.accentSoft
        isHovered -> sc.hoverOverlay
        else -> Color.Transparent
    }
    val borderColor = when {
        selected -> sc.accent
        else -> sc.border
    }
    val textColor = when {
        !enabled -> sc.textDisabled
        selected -> sc.accent
        else -> sc.textSecondary
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .hoverable(interactionSource)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Xs)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = DsType.Caption,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// --- DsContextMenuHost ---

@Composable
fun DsContextMenuHost(
    enabled: Boolean = true,
    menuItems: List<DsMenuItem>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var menuPos by remember { mutableStateOf(IntOffset.Zero) }

    Box(
        modifier = modifier.pointerInput(enabled, menuItems.size) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Secondary) {
                        val pos = event.changes.first().position
                        if (enabled && menuItems.isNotEmpty()) {
                            menuPos = IntOffset(pos.x.toInt(), pos.y.toInt())
                            showMenu = true
                        }
                    }
                }
            }
        }
    ) {
        content()

        if (showMenu && enabled && menuItems.isNotEmpty()) {
            Popup(
                alignment = androidx.compose.ui.Alignment.TopStart,
                offset = menuPos,
                onDismissRequest = { showMenu = false },
                properties = PopupProperties(focusable = true)
            ) {
                DsMenuPanel(
                    menuItems = menuItems,
                    onDismiss = { showMenu = false }
                )
            }
        }
    }
}

// --- DsFavoriteToggle ---

@Composable
fun DsFavoriteToggle(
    isFavorite: Boolean = false,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 16,
    favorite: Boolean = isFavorite
) {
    val shown = isFavorite || favorite
    val color = if (shown) favoriteColor else surfaceColors().textMuted

    Text(
        text = if (shown) "\u2605" else "\u2606",
        color = color,
        fontSize = size.sp,
        modifier = modifier
            .clip(RoundedCornerShape(DsRadius.Sm))
            .clickable { onToggle() }
            .padding(2.dp)
    )
}

// --- DsFlagBadge ---
// Supports both (flag) and (label, colorHex) call signatures

@Composable
fun DsFlagBadge(
    flag: String,
    modifier: Modifier = Modifier
) {
    val color = when (flag.lowercase()) {
        "red" -> Color(0xFFEF5350)
        "orange" -> Color(0xFFFFA726)
        "yellow" -> Color(0xFFFFEE58)
        "green" -> Color(0xFF66BB6A)
        "blue" -> Color(0xFF42A5F5)
        "purple" -> Color(0xFFAB47BC)
        else -> Color(0xFF9E9E9E)
    }

    Box(
        modifier = modifier
            .size(10.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}

@Composable
fun DsFlagBadge(
    label: String,
    colorHex: String,
    modifier: Modifier = Modifier
) {
    val color = parseHexColor(colorHex)

    Box(
        modifier = modifier
            .size(10.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}
