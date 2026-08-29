package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — BUTTON
// ============================================

enum class DsButtonKind { Primary, Secondary, Ghost, Danger, AccentTint }

@Composable
fun DsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: DsButtonKind = DsButtonKind.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    compact: Boolean = false,
    tint: Color = Color.Unspecified
) {
    val sc = surfaceColors()
    val ac = accent()
    val shape = RoundedCornerShape(DsRadius.Sm)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val containerColor = when (kind) {
        DsButtonKind.Primary -> sc.accent
        DsButtonKind.Secondary -> sc.surface
        DsButtonKind.Ghost -> Color.Transparent
        DsButtonKind.Danger -> errorColor
        DsButtonKind.AccentTint -> sc.accent.copy(alpha = 0.12f)
    }
    val contentColor = when (kind) {
        DsButtonKind.Primary -> sc.textOnAccent
        DsButtonKind.Secondary -> sc.textPrimary
        DsButtonKind.Ghost -> sc.textSecondary
        DsButtonKind.Danger -> Color.White
        DsButtonKind.AccentTint -> sc.accent
    }
    val hoverTint = when (kind) {
        DsButtonKind.Primary -> sc.accent.copy(alpha = 0.85f)
        DsButtonKind.Secondary -> sc.hoverOverlay
        DsButtonKind.Ghost -> sc.hoverOverlay
        DsButtonKind.Danger -> errorColor.copy(alpha = 0.85f)
        DsButtonKind.AccentTint -> sc.accent.copy(alpha = 0.18f)
    }

    val horizontalPad = if (compact) DsSpacing.Md else DsSpacing.Lg
    val verticalPad = if (compact) DsSpacing.Xs else DsSpacing.Sm

    Row(
        modifier = modifier
            .clip(shape)
            .background(if (tint != Color.Unspecified) tint else if (isHovered && enabled) hoverTint else containerColor)
            .then(
                if (kind == DsButtonKind.Secondary) {
                    Modifier.border(1.dp, sc.border, shape)
                } else {
                    Modifier
                }
            )
            .hoverable(interactionSource)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = horizontalPad, vertical = verticalPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
                modifier = Modifier.size(if (compact) 14.dp else 16.dp)
            )
        }
        Text(
            text = text,
            color = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
            fontSize = DsType.Body,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DsTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val sc = surfaceColors()
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(DsRadius.Sm))
            .background(if (isHovered && enabled) sc.hoverOverlay else Color.Transparent)
            .hoverable(interactionSource)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) sc.accent else sc.textDisabled,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = text,
            color = if (enabled) sc.accent else sc.textDisabled,
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
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    size: Dp = 32.dp
) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Sm)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(if (isHovered && enabled) sc.hoverOverlay else Color.Transparent)
            .hoverable(interactionSource)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                tint != Color.Unspecified -> tint
                enabled -> sc.textSecondary
                else -> sc.textDisabled
            }
        )
    }
}
