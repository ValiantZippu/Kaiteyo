package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
// Dropdown and context menu primitives.
// ============================================

@Composable
fun DsMenuPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Md)
    Column(
        modifier = modifier
            .clip(shape)
            .background(sc.surfaceElevated)
            .padding(DsSpacing.Xs),
        content = content
    )
}

@Composable
fun DsMenuItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified
) {
    val sc = surfaceColors()
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Sm))
            .background(if (isHovered && enabled) sc.hoverOverlay else Color.Transparent)
            .hoverable(interactionSource)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (tint != Color.Unspecified) tint else sc.textSecondary
            )
        }
        Text(
            text = label,
            color = if (enabled) sc.textPrimary else sc.textDisabled,
            fontSize = DsType.Body,
            modifier = Modifier.weight(1f)
        )
    }
}
