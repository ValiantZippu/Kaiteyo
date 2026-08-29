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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — TAG CHIP & TOGGLE
// ============================================

@Composable
fun DsTagChip(
    text: String,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null
) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Full)

    Row(
        modifier = modifier
            .clip(shape)
            .background(sc.accentSoft)
            .border(1.dp, sc.accent.copy(alpha = 0.3f), shape)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
    ) {
        Text(
            text = text,
            color = sc.accent,
            fontSize = DsType.Caption
        )
        if (onRemove != null) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = sc.accent,
                modifier = Modifier
                    .size(12.dp)
                    .clickable { onRemove() }
            )
        }
    }
}

/**
 * Tag chip with explicit label and color — used by ReviewView and BrowserView.
 */
@Composable
fun DsTagChip(
    label: String,
    colorHex: String = "",
    modifier: Modifier = Modifier,
    removable: Boolean = false,
    onRemove: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val tagColor = if (colorHex.isNotEmpty()) parseHexColor(colorHex) else surfaceColors().accent
    val shape = RoundedCornerShape(DsRadius.Full)
    val clickableModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else if (onRemove != null && removable) {
        Modifier.clickable { onRemove() }
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .clip(shape)
            .background(tagColor.copy(alpha = 0.15f))
            .border(1.dp, tagColor.copy(alpha = 0.3f), shape)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
    ) {
        Text(
            text = label,
            color = tagColor,
            fontSize = DsType.Caption
        )
        if (removable && onRemove != null) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = tagColor,
                modifier = Modifier
                    .size(12.dp)
                    .clickable { onRemove() }
            )
        }
    }
}

// --- DsToggle ---

@Composable
fun DsToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = "",
    enabled: Boolean = true
) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Full)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .clip(shape)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .hoverable(interactionSource)
            .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp, 20.dp)
                .clip(shape)
                .background(
                    if (checked) sc.accent
                    else if (isHovered) sc.surfaceInteractive
                    else sc.surfaceVariant
                )
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(shape)
                    .background(Color.White)
                    .then(
                        if (checked) Modifier.padding(start = 16.dp) else Modifier
                    )
            )
        }
        if (!label.isNullOrEmpty()) {
            Text(
                text = label,
                color = if (enabled) sc.textSecondary else sc.textDisabled,
                fontSize = DsType.Body
            )
        }
    }
}
