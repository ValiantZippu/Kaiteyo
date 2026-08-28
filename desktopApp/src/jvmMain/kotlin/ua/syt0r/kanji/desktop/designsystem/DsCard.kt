package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — CARD
// Consistent card surface for grouping content.
// ============================================

@Composable
fun DsCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Md)

    Box(
        modifier = modifier
            .let {
                if (elevated) it.shadow(DsElevation.Md, shape) else it
            }
            .clip(shape)
            .background(sc.surface)
            .then(
                if (!elevated) Modifier.border(1.dp, sc.borderSubtle, shape) else Modifier
            )
            .padding(DsSpacing.Lg)
    ) {
        content()
    }
}
