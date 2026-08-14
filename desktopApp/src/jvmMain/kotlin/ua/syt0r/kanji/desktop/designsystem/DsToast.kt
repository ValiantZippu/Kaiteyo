package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.model.ToastKind
import ua.syt0r.kanji.desktop.model.ToastMessage
import ua.syt0r.kanji.presentation.common.theme.AnimationSpeed
import ua.syt0r.kanji.presentation.common.theme.LocalAnimationConfig
import ua.syt0r.kanji.presentation.common.theme.tweenDuration

// ============================================
// KAITEYO DESIGN SYSTEM — TOASTS
// ============================================

/** State holder for the toast host; shared via CompositionLocal. */
class DsToastHost {
    val messages = androidx.compose.runtime.mutableStateListOf<ToastMessage>()

    fun show(text: String, kind: ToastKind = ToastKind.Info, durationMs: Long = 3500) {
        val msg = ToastMessage(text, kind, durationMs)
        messages.add(msg)
        // Auto-dismiss handled by DsToastHostView.
    }

    fun dismiss(message: ToastMessage) {
        messages.remove(message)
    }
}

@Composable
fun DsToastHostView(
    host: DsToastHost,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
        // Toast motion honors the animation speed / reduced-motion config and
        // is identical for every message, so it is computed once per host.
        val duration = tweenDuration(LocalAnimationConfig.current, 240)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = DsSpacing.Xl)
        ) {
            host.messages.forEach { msg ->
                var visible by remember(msg) { mutableStateOf(true) }
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(msg.durationMs)
                    visible = false
                    // Wait for the exit animation to finish before removing
                    // the message, so the fade/slide never gets clipped.
                    kotlinx.coroutines.delay(duration.toLong() + 50)
                    host.dismiss(msg)
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(tween<IntOffset>(duration), initialOffsetY = { it / 4 }) +
                        fadeIn(tween(duration)),
                    exit = slideOutVertically(tween<IntOffset>(duration), targetOffsetY = { it / 4 }) +
                        fadeOut(tween(duration))
                ) {
                    DsToastItem(msg, onDismiss = { host.dismiss(msg) })
                }
                Spacer(Modifier.height(DsSpacing.Sm))
            }
        }
    }
}

@Composable
private fun DsToastItem(message: ToastMessage, onDismiss: () -> Unit) {
    val sc = surfaceColors()
    val (bg, icon, tint) = when (message.kind) {
        ToastKind.Success -> Triple(Color(0xFF1E3A24), Icons.Default.CheckCircle, Color(0xFFC2FC8B))
        ToastKind.Warning -> Triple(Color(0xFF3A2C1E), Icons.Default.Warning, Color(0xFFFEAB57))
        ToastKind.Error -> Triple(Color(0xFF3A1E1E), Icons.Default.Error, Color(0xFFFF6B6B))
        ToastKind.Info -> Triple(sc.surfaceInteractive, Icons.Default.Info, sc.textSecondary)
    }

    // Gentle spring pop on the icon as the toast appears, layered over the
    // slide-up entrance. Skipped under reduced motion / instant speed.
    val config = LocalAnimationConfig.current
    val iconScale = remember(message) { Animatable(0.5f) }
    LaunchedEffect(message) {
        if (config.reducedMotion || config.speed == AnimationSpeed.Instant) {
            iconScale.snapTo(1f)
        } else {
            iconScale.animateTo(1.08f, spring(dampingRatio = 0.45f, stiffness = 480f))
            iconScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 900f))
        }
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(bg)
            .padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer {
                    scaleX = iconScale.value
                    scaleY = iconScale.value
                }
        )
        Text(
            text = message.text,
            color = sc.textPrimary,
            fontSize = DsType.Body,
            fontWeight = FontWeight.Medium
        )
    }
}

val LocalToastHost = androidx.compose.runtime.staticCompositionLocalOf { DsToastHost() }
