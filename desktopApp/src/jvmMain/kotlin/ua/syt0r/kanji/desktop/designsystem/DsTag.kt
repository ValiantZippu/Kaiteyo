package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — TAGS & FLAGS
// ============================================

/** Parse "#RRGGBB" / "#RRGGBBAA" into a Color. */
fun parseHexColor(hex: String?): Color {
    if (hex.isNullOrBlank()) return Color.Gray
    return try {
        val raw = hex.removePrefix("#")
        when (raw.length) {
            6 -> Color(
                raw.substring(0, 2).toInt(16),
                raw.substring(2, 4).toInt(16),
                raw.substring(4, 6).toInt(16),
                255
            )
            8 -> Color(
                raw.substring(2, 4).toInt(16),
                raw.substring(4, 6).toInt(16),
                raw.substring(6, 8).toInt(16),
                raw.substring(0, 2).toInt(16)
            )
            else -> Color.Gray
        }
    } catch (_: Exception) {
        Color.Gray
    }
}

@Composable
fun DsTagChip(
    label: String,
    colorHex: String = "#808080",
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    removable: Boolean = false,
    onRemove: (() -> Unit)? = null
) {
    val color = remember(colorHex) { parseHexColor(colorHex) }
    val sc = surfaceColors()
    val onColor = remember(color) {
        if (color.luminance() > 0.5f) Color(0xFF1A1A1A) else Color(0xFFF0F0F0)
    }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = if (selected) color.copy(alpha = 0.35f)
        else if (hovered) color.copy(alpha = 0.25f)
        else color.copy(alpha = 0.16f),
        animationSpec = tween(160),
        label = "tagBg"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(DsRadius.Sm))
            .background(bg)
            .then(
                if (onClick != null) Modifier
                    .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                    .hoverable(interaction)
                else Modifier
            )
            .padding(horizontal = DsSpacing.Sm, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(
            text = label,
            color = onColor,
            fontSize = DsType.Caption,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (removable && onRemove != null) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                tint = onColor.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onRemove)
            )
        }
    }
}

/** Flag badge — colored dot with label, used for color/priority flags. */
@Composable
fun DsFlagBadge(
    label: String,
    colorHex: String,
    modifier: Modifier = Modifier,
    showDot: Boolean = true
) {
    val color = remember(colorHex) { parseHexColor(colorHex) }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
        Text(
            text = label,
            color = surfaceColors().textSecondary,
            fontSize = DsType.Caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Priority flag: colored diamond + "P1" label. */
@Composable
fun DsPriorityFlag(priority: Int, colorHex: String, modifier: Modifier = Modifier) {
    val color = remember(colorHex) { parseHexColor(colorHex) }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "P$priority",
            color = color,
            fontSize = DsType.Caption,
            fontWeight = FontWeight.Bold
        )
    }
}
