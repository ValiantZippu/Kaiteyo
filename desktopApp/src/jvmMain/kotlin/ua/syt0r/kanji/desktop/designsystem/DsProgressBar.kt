package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — PROGRESS BAR
// ============================================

@Composable
fun DsProgressBar(
    value: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color? = null
) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Full)
    val resolvedTrack = trackColor ?: sc.surfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(shape)
            .background(resolvedTrack)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = value.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(shape)
                .background(color)
        )
    }
}
