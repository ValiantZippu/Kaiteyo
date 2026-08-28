package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — BUTTON
// ============================================

enum class DsButtonKind { Primary, Secondary, Ghost }

@Composable
fun DsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: DsButtonKind = DsButtonKind.Primary,
    enabled: Boolean = true
) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Sm)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val containerColor = when (kind) {
        DsButtonKind.Primary -> sc.accent
        DsButtonKind.Secondary -> sc.surface
        DsButtonKind.Ghost -> Color.Transparent
    }
    val contentColor = when (kind) {
        DsButtonKind.Primary -> sc.textOnAccent
        DsButtonKind.Secondary -> sc.textPrimary
        DsButtonKind.Ghost -> sc.textSecondary
    }
    val hoverTint = when (kind) {
        DsButtonKind.Primary -> sc.accent.copy(alpha = 0.85f)
        DsButtonKind.Secondary -> sc.hoverOverlay
        DsButtonKind.Ghost -> sc.hoverOverlay
    }

    Row(
        modifier = modifier
            .clip(shape)
            .background(if (isHovered && enabled) hoverTint else containerColor)
            .then(
                if (kind == DsButtonKind.Secondary) {
                    Modifier.border(1.dp, sc.border, shape)
                } else {
                    Modifier
                }
            )
            .hoverable(interactionSource)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Text(
            text = text,
            color = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
            fontSize = DsType.Body,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DsIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String? = null,
    enabled: Boolean = true
) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Sm)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(32.dp)
            .clip(shape)
            .background(if (isHovered && enabled) sc.hoverOverlay else Color.Transparent)
            .hoverable(interactionSource)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) sc.textSecondary else sc.textDisabled
        )
    }
}
