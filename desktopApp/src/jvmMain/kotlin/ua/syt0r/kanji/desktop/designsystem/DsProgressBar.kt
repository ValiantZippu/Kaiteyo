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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — PROGRESS BAR
// ============================================

@Composable
fun DsProgressBar(
    modifier: Modifier = Modifier,
    value: Float = 0f,
    color: Color = Color.Unspecified,
    fraction: Float = value,
    height: Dp = 6.dp
) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Full)
    val resolvedColor = if (color == Color.Unspecified) sc.accent else color

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(sc.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = fraction.coerceIn(0f, 1f))
                .height(height)
                .clip(shape)
                .background(resolvedColor)
        )
    }
}
