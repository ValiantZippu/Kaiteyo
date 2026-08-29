package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ua.syt0r.kanji.desktop.model.ToastKind

// ============================================
// KAITEYO DESIGN SYSTEM — TOAST
// ============================================

/**
 * Mutable toast host that queues toast messages.
 * Owned by [AppState.toastHost].
 */
class DsToastHost {
    private var _message = mutableStateOf("")
    private var _kind = mutableStateOf(ToastKind.Info)
    private var _visible = mutableStateOf(false)
    private val scope = CoroutineScope(Dispatchers.Default)

    val message: String get() = _message.value
    val kind: ToastKind get() = _kind.value
    val visible: Boolean get() = _visible.value

    fun show(message: String, kind: ToastKind = ToastKind.Info, durationMs: Long = 3000) {
        _message.value = message
        _kind.value = kind
        _visible.value = true
        scope.launch {
            delay(durationMs)
            _visible.value = false
        }
    }

    fun dismiss() {
        _visible.value = false
    }
}

/**
 * Toast host view — wraps content and shows toast notifications at the bottom-center.
 */
@Composable
fun DsToastHostView(
    host: DsToastHost,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()

        // Toast overlay
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).padding(DsSpacing.Lg)
        ) {
            AnimatedVisibility(
                visible = host.visible,
                enter = slideInVertically(tween(200)) { it / 2 } + fadeIn(tween(200)),
                exit = slideOutVertically(tween(200)) { it / 2 } + fadeOut(tween(200))
            ) {
                val sc = surfaceColors()
                val shape = RoundedCornerShape(DsRadius.Lg)
                val (icon, tint) = when (host.kind) {
                    ToastKind.Success -> Icons.Default.CheckCircle to successColor
                    ToastKind.Warning -> Icons.Default.Warning to warningColor
                    ToastKind.Error -> Icons.Default.Error to errorColor
                    ToastKind.Info -> Icons.Default.Info to infoColor
                }

                Row(
                    modifier = Modifier
                        .clip(shape)
                        .background(sc.surfaceElevated)
                        .border(1.dp, sc.border.copy(alpha = 0.3f), shape)
                        .padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Md)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = host.message,
                        color = sc.textPrimary,
                        fontSize = DsType.Body,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
