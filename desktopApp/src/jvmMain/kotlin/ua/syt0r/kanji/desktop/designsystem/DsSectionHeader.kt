package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

// ============================================
// KAITEYO DESIGN SYSTEM — SECTION HEADER
// ============================================

@Composable
fun DsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {}
) {
    val sc = surfaceColors()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = sc.textPrimary,
            fontSize = DsType.BodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        action()
    }
}
