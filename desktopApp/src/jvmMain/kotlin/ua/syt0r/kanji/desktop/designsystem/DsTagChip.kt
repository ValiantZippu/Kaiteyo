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
                Icons.Default.Close,
                contentDescription = "Remove",
                tint = sc.accent,
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onRemove() }
                    .padding(1.dp)
            )
        }
    }
}

@Composable
fun DsToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val sc = surfaceColors()
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val thumbColor = if (checked) sc.accent else sc.textMuted
    val trackColor = when {
        !enabled -> sc.surfaceVariant
        checked -> sc.accentSoft
        isHovered -> sc.hoverOverlay
        else -> sc.surfaceInteractive
    }

    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = androidx.compose.material3.SwitchDefaults.colors(
            checkedThumbColor = thumbColor,
            checkedTrackColor = trackColor,
            uncheckedThumbColor = thumbColor,
            uncheckedTrackColor = trackColor
        )
    )
}
