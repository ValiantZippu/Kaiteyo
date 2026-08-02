package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — TOOLBAR / HEADER
// ============================================

@Composable
fun DsToolbar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable () -> Unit = {},
    backIcon: ImageVector? = null,
    onBack: (() -> Unit)? = null
) {
    val sc = surfaceColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        if (backIcon != null && onBack != null) {
            DsIconButton(icon = backIcon, onClick = onBack, contentDescription = "Back")
        }
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = sc.textPrimary,
                fontSize = DsType.Title,
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
        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
            actions()
        }
    }
}

@Composable
fun DsToolbarDivider(modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(sc.border.copy(alpha = 0.4f))
    )
}

// ============================================
// KAITEYO DESIGN SYSTEM — SPLIT PANES
// Resizable horizontal/vertical split with a
// draggable divider. Fraction persisted via
// onFractionChanged.
// ============================================

@Composable
fun DsSplitPane(
    modifier: Modifier = Modifier,
    vertical: Boolean = true,
    initialFraction: Float = 0.5f,
    dividerWidth: androidx.compose.ui.unit.Dp = 6.dp,
    onFractionChanged: (Float) -> Unit = {},
    first: @Composable () -> Unit,
    second: @Composable () -> Unit
) {
    var fraction by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initialFraction) }
    val sc = surfaceColors()
    val ac = accent()

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
        val maxSize = if (vertical) maxHeight else maxWidth
        val mainSize = maxSize - dividerWidth
        val firstSize = mainSize * fraction.coerceIn(0.1f, 0.9f)

        if (vertical) {
            androidx.compose.foundation.layout.Column {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(firstSize)
                ) { first() }
                SplitDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dividerWidth),
                    vertical = false,
                    sc = sc,
                    ac = ac
                ) { delta ->
                    fraction = (fraction + delta).coerceIn(0.1f, 0.9f)
                    onFractionChanged(fraction)
                }
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { second() }
            }
        } else {
            androidx.compose.foundation.layout.Row {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(firstSize)
                ) { first() }
                SplitDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(dividerWidth),
                    vertical = true,
                    sc = sc,
                    ac = ac
                ) { delta ->
                    fraction = (fraction + delta).coerceIn(0.1f, 0.9f)
                    onFractionChanged(fraction)
                }
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) { second() }
            }
        }
    }
}

@Composable
private fun SplitDivider(
    modifier: Modifier,
    vertical: Boolean,
    sc: ua.syt0r.kanji.presentation.common.theme.SurfaceColors,
    ac: ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme,
    onDelta: (Float) -> Unit
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by androidx.compose.animation.animateColorAsState(
        targetValue = if (hovered) ac.primary.copy(alpha = 0.5f) else sc.border.copy(alpha = 0.4f),
        animationSpec = androidx.compose.animation.core.tween(160),
        label = "splitBg"
    )
    Box(
        modifier = modifier
            .background(bg)
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    val normalized = if (vertical) drag.x / 1000f else drag.y / 1000f
                    onDelta(normalized)
                }
            }
            .hoverable(interaction)
    )
}
