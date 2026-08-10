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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme

// ============================================
// KAITEYO DESIGN SYSTEM — BUTTONS
// ============================================

sealed interface DsButtonKind {
    data object Primary : DsButtonKind
    data object Secondary : DsButtonKind
    data object Ghost : DsButtonKind
    data object Danger : DsButtonKind
    data object AccentTint : DsButtonKind
}

@Composable
fun DsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: DsButtonKind = DsButtonKind.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    compact: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val sc = surfaceColors()
    val ac = accent()

    val (rest, hover, pressedColor) = colorsFor(kind, sc, ac)
    val bg by animateColorAsState(
        targetValue = when {
            pressed -> pressedColor
            hovered -> hover
            else -> rest
        },
        animationSpec = tween(160),
        label = "btnBg"
    )
    val fg = when (kind) {
        DsButtonKind.Primary -> ac.onPrimary
        DsButtonKind.Danger -> Color.White
        else -> sc.textPrimary
    }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else if (hovered) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "btnScale"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(bg)
            .then(
                if (enabled) Modifier
                    .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                    .hoverable(interaction)
                else Modifier
            )
            .padding(
                horizontal = DsSpacing.Md,
                vertical = if (compact) DsSpacing.Xs else DsSpacing.Sm
            ),
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        }
        Text(
            text = text,
            color = if (enabled) fg else sc.textMuted,
            fontSize = if (compact) DsType.Label else DsType.Body,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun colorsFor(
    kind: DsButtonKind,
    sc: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    ac: KaiteyoAccentScheme
): Triple<Color, Color, Color> = when (kind) {
    DsButtonKind.Primary -> Triple(
        ac.primary,
        ac.primary.copy(alpha = 0.85f),
        ac.primary.copy(alpha = 0.72f)
    )
    DsButtonKind.Secondary -> Triple(
        sc.surfaceElevated,
        sc.surfaceInteractive,
        sc.surfaceInteractive.copy(alpha = 0.8f)
    )
    DsButtonKind.Ghost -> Triple(
        Color.Transparent,
        sc.surfaceInteractive,
        sc.surfaceInteractive.copy(alpha = 0.8f)
    )
    DsButtonKind.Danger -> Triple(
        errorColor(),
        errorColor().copy(alpha = 0.85f),
        errorColor().copy(alpha = 0.72f)
    )
    DsButtonKind.AccentTint -> Triple(
        ac.primary.copy(alpha = 0.16f),
        ac.primary.copy(alpha = 0.26f),
        ac.primary.copy(alpha = 0.34f)
    )
}

@Composable
fun DsIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color? = null,
    size: Dp = 34.dp,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val sc = surfaceColors()
    val ac = accent()
    val bg by animateColorAsState(
        targetValue = if (hovered) sc.surfaceInteractive else Color.Transparent,
        animationSpec = tween(160),
        label = "iconBtnBg"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(DsRadius.Sm))
            .background(bg)
            .then(
                if (enabled) Modifier
                    .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                    .hoverable(interaction)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: if (hovered) ac.primary else sc.textSecondary,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
fun DsTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val sc = surfaceColors()
    val ac = accent()

    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(DsRadius.Sm))
            .background(if (hovered) ac.primary.copy(alpha = 0.1f) else Color.Transparent)
            .then(
                if (enabled) Modifier
                    .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                    .hoverable(interaction)
                else Modifier
            )
            .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Xs),
        color = if (enabled) ac.primary else sc.textMuted,
        fontSize = DsType.Label,
        fontWeight = FontWeight.Medium
    )
}

/** Group of buttons laid out evenly (used by review grading). */
@Composable
fun DsButtonRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        content()
    }
}
