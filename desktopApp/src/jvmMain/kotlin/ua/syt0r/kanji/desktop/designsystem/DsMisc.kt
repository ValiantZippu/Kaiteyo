package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — MISC PRIMITIVES
// Badges, stat tiles, progress, tooltip, toggle.
// ============================================

/** Small numeric badge (e.g. review counts). */
@Composable
fun DsBadge(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val sc = surfaceColors()
    val ac = accent()
    val color = if (tint != Color.Unspecified) tint else ac.primary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(DsRadius.Full))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = DsSpacing.Sm, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = DsType.Caption,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Stat tile: label + big value + optional delta. */
@Composable
fun DsStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    delta: String? = null,
    deltaPositive: Boolean = true
) {
    val sc = surfaceColors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Lg))
            .background(sc.surface)
            .padding(DsSpacing.Lg)
    ) {
        Text(
            text = label.uppercase(),
            color = sc.textMuted,
            fontSize = DsType.Caption,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(DsSpacing.Xs))
        Text(
            text = value,
            color = sc.textPrimary,
            fontSize = DsType.Heading,
            fontWeight = FontWeight.Bold
        )
        if (delta != null) {
            Spacer(Modifier.height(DsSpacing.Xs))
            Text(
                text = delta,
                color = if (deltaPositive) successColor() else errorColor(),
                fontSize = DsType.Caption
            )
        }
    }
}

/** Inline progress bar. */
@Composable
fun DsProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 6.dp,
    color: Color = Color.Unspecified
) {
    val sc = surfaceColors()
    val ac = accent()
    val fill = if (color != Color.Unspecified) color else ac.primary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(sc.surfaceInteractive)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(fill)
        )
    }
}

/** Kaiteyo-native toggle switch — no Material3 dependency. */
@Composable
fun DsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 20f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "switchThumb"
    )
    val trackColor by animateColorAsState(
        targetValue = when {
            checked -> ac.primary
            hovered -> sc.surfaceInteractive
            else -> sc.surfaceInteractive.copy(alpha = 0.6f)
        },
        animationSpec = tween(160),
        label = "switchTrack"
    )
    val thumbColor = if (checked) ac.onPrimary else sc.textSecondary

    Box(
        modifier = modifier
            .size(width = 40.dp, height = 22.dp)
            .clip(RoundedCornerShape(DsRadius.Full))
            .background(trackColor)
            .then(
                if (enabled) Modifier
                    .clickable(interactionSource = interaction, indication = null) { onCheckedChange(!checked) }
                    .hoverable(interaction)
                else Modifier
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = 2.dp)
                .offset(x = thumbOffset.dp)
                .size(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(thumbColor)
        )
    }
}

/** Simple labeled toggle switch. */
@Composable
fun DsToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true
) {
    val sc = surfaceColors()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (label != null) {
            Text(
                text = label,
                color = if (enabled) sc.textPrimary else sc.textMuted,
                fontSize = DsType.Body,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(DsSpacing.Md))
        }
        DsSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

/** Link-style action row (e.g. "Open browser →"). */
@Composable
fun DsLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ac = accent()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(DsRadius.Sm))
            .clickable(onClick = onClick)
            .padding(vertical = DsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text,
            color = ac.primary,
            fontSize = DsType.Label,
            fontWeight = FontWeight.Medium
        )
        Icon(
            Icons.Default.ArrowForward,
            contentDescription = null,
            tint = ac.primary,
            modifier = Modifier.size(12.dp)
        )
    }
}

/** Section header with optional right-side content. */
@Composable
fun DsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: @Composable () -> Unit = {}
) {
    val sc = surfaceColors()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = sc.textPrimary,
                fontSize = DsType.BodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }
        action()
    }
}

/** Numeric label for grid density controls. */
@Composable
fun DsNumberLabel(value: Int, modifier: Modifier = Modifier) {
    Text(
        text = value.toString(),
        color = surfaceColors().textPrimary,
        fontSize = DsType.Body,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}
