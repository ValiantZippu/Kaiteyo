package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

// ============================================
// KAITEYO DESIGN SYSTEM — MISC COMPONENTS
// DsChip, DsContextMenuHost, DsFavoriteToggle, DsFlagBadge
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DsContextMenuHost(
    enabled: Boolean,
    menuItems: List<DsMenuItem>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var menuPos by remember { mutableStateOf(IntOffset.Zero) }

    Box(
        modifier = modifier.onPointerEvent(PointerEventType.SecondaryPressDown) { event ->
            if (enabled && menuItems.isNotEmpty()) {
                val position = event.changes.firstOrNull()?.position
                if (position != null) {
                    menuPos = IntOffset(position.x.toInt(), position.y.toInt())
                    showMenu = true
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
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 16
) {
    val color = if (isFavorite) favoriteColor else surfaceColors().textMuted

    Text(
        text = if (isFavorite) "\u2605" else "\u2606",
        color = color,
        fontSize = size.sp,
        modifier = modifier
            .clip(RoundedCornerShape(DsRadius.Sm))
            .clickable { onToggle() }
            .padding(2.dp)
    )
}

// --- DsFlagBadge ---

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
