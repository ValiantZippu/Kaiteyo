package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ua.syt0r.kanji.presentation.common.theme.LocalAnimationConfig

// ============================================
// KAITEYO DESIGN SYSTEM — DIALOGS
// ============================================

/**
 * Entrance animation shared by every dialog: the panel scales up from
 * 0.94 and fades in with a spring for a deliberate, premium feel.
 */
@Composable
private fun dialogEntranceLayer(): GraphicsLayerScope.() -> Unit {
    val reducedMotion = LocalAnimationConfig.current.reducedMotion
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.94f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "dialogScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (reducedMotion) 0 else 180),
        label = "dialogAlpha"
    )
    return {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }
}

@Composable
fun DsDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    // Compact dialogs (confirms/prompts) stay readable; rich dialogs spread
    // on wide windows. Measured against the window the dialog opens in, so a
    // maximized 2560px window gets a generously sized panel instead of a
    // 480dp box floating in the middle.
    compact: Boolean = false,
    content: @Composable () -> Unit
) {
    val sc = surfaceColors()
    val layer = dialogEntranceLayer()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().padding(DsSpacing.Lg)
        ) {
            val width = adaptiveDialogWidth(maxWidth, compact = compact)
            Column(
                modifier = modifier
                    .width(width)
                    .clip(RoundedCornerShape(DsRadius.Xl))
                    .background(sc.surfaceElevated)
                    .graphicsLayer(layer)
                    .padding(DsSpacing.Xl)
            ) {
                Text(
                    text = title,
                    color = sc.textPrimary,
                    fontSize = DsType.Title,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(DsSpacing.Lg))
                content()
            }
        }
    }
}

@Composable
fun DsConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false
) {
    val sc = surfaceColors()
    val ac = accent()
    DsDialog(title = title, onDismiss = onDismiss, modifier = modifier, compact = true) {
        Text(
            text = message,
            color = sc.textSecondary,
            fontSize = DsType.Body
        )
        Spacer(Modifier.height(DsSpacing.Xl))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
        ) {
            DsTextButton(text = "Cancel", onClick = onDismiss)
            DsButton(
                text = confirmText,
                onClick = {
                    onDismiss()
                    onConfirm()
                },
                kind = if (danger) DsButtonKind.Danger else DsButtonKind.Primary
            )
        }
    }
}

@Composable
fun DsPromptDialog(
    title: String,
    placeholder: String,
    initialValue: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var value by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initialValue) }
    DsDialog(title = title, onDismiss = onDismiss, modifier = modifier, compact = true) {
        DsTextField(
            value = value,
            onValueChange = { value = it },
            placeholder = placeholder,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(DsSpacing.Xl))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
        ) {
            DsTextButton(text = "Cancel", onClick = onDismiss)
            DsButton(
                text = "Save",
                onClick = {
                    onDismiss()
                    onConfirm(value.trim())
                },
                enabled = value.isNotBlank()
            )
        }
    }
}

@Composable
fun DsProgressDialog(
    title: String,
    message: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val ac = accent()
    DsDialog(title = title, onDismiss = {}, modifier = modifier, compact = true) {
        Text(
            text = message,
            color = sc.textSecondary,
            fontSize = DsType.Body
        )
        Spacer(Modifier.height(DsSpacing.Lg))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(sc.surfaceInteractive)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(ac.primary)
            )
        }
        Spacer(Modifier.height(DsSpacing.Sm))
        Text(
            text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
            color = sc.textMuted,
            fontSize = DsType.Caption,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
