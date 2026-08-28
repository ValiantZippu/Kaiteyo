package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — DIVIDER
// ============================================

@Composable
fun DsToolbarDivider(modifier: Modifier = Modifier) {
    val sc = surfaceColors()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(sc.borderSubtle)
    )
}
