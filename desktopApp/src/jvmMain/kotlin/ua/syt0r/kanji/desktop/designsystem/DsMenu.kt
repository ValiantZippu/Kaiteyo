package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — MENU
// Data class for menu items, panel, row, divider.
// ============================================

/** Immutable menu item descriptor — NOT a composable. */
data class DsMenuItem(
    val label: String,
    val icon: ImageVector? = null,
    val onAction: () -> Unit = {},
    val danger: Boolean = false,
    val enabled: Boolean = true,
    val shortcutLabel: String? = null,
    val checked: Boolean = false
)

// --- DsMenuPanel (renders list of items) ---

@Composable
fun DsMenuPanel(
    menuItems: List<DsMenuItem>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Md)
    Column(
        modifier = modifier
            .clip(shape)
            .background(sc.surfaceElevated)
            .padding(DsSpacing.Xs)
    ) {
        menuItems.forEach { item ->
            DsMenuItemRow(
                item = item,
                onClick = {
                    if (item.enabled) {
                        item.onAction()
                        onDismiss()
                    }
                }
            )
        }
        content?.invoke(this)
    }
}

// --- DsMenuItemRow (single row) ---

@Composable
fun DsMenuItemRow(
    item: DsMenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    val sc = surfaceColors()
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Sm))
            .background(
                if (isHovered && item.enabled) sc.hoverOverlay else Color.Transparent
            )
            .hoverable(interactionSource)
            .clickable(enabled = item.enabled) { onClick() }
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        if (item.icon != null) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = when {
                    !item.enabled -> sc.textDisabled
                    item.danger -> errorColor
                    else -> sc.textSecondary
                },
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = item.label,
            color = when {
                !item.enabled -> sc.textDisabled
                item.danger -> errorColor
                else -> sc.textPrimary
            },
            fontSize = DsType.Body,
            modifier = Modifier.weight(1f)
        )
        if (item.shortcutLabel != null) {
            Text(
                text = item.shortcutLabel,
                color = sc.textMuted,
                fontSize = DsType.Caption
            )
        }
        if (trailingContent != null) {
            trailingContent()
        }
    }
}

// --- DsMenuDivider ---

@Composable
fun DsMenuDivider(modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    HorizontalDivider(
        modifier = modifier.padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Xs),
        color = sc.borderSubtle,
        thickness = 1.dp
    )
}

// --- Legacy composable DsMenuItem (kept for any callers that use it as @Composable) ---

@Composable
fun DsMenuItemRow(
    label: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
    shortcutLabel: String? = null,
    tint: Color = Color.Unspecified
) {
    DsMenuItemRow(
        item = DsMenuItem(
            label = label,
            icon = icon,
            onAction = onClick,
            danger = danger,
            enabled = enabled,
            shortcutLabel = shortcutLabel
        ),
        onClick = onClick,
        modifier = modifier
    )
}
