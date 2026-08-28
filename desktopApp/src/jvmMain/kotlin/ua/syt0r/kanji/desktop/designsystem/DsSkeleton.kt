package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — SKELETON
// Loading placeholders that match content shape.
// ============================================

@Composable
fun DsSkeleton(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(DsRadius.Sm))
            .background(sc.surfaceVariant)
    )
}

@Composable
fun DsSkeletonCard(
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(sc.surface)
            .padding(DsSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        DsSkeleton(width = 180.dp, height = 16.dp)
        DsSkeleton(width = 260.dp, height = 12.dp)
        DsSkeleton(width = 200.dp, height = 12.dp)
    }
}
