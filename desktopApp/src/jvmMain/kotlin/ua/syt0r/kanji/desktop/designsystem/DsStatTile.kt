package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — STAT TILE
// Key performance indicator card.
// ============================================

@Composable
fun DsStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    delta: String? = null,
    deltaPositive: Boolean = true
) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Md)

    Column(
        modifier = modifier
            .clip(shape)
            .background(sc.surface)
            .padding(DsSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
    ) {
        Text(
            text = label,
            color = sc.textMuted,
            fontSize = DsType.Caption
        )
        Text(
            text = value,
            color = sc.textPrimary,
            fontSize = DsType.Title,
            fontWeight = FontWeight.Bold
        )
        if (delta != null) {
            Text(
                text = delta,
                color = if (deltaPositive) successColor else errorColor,
                fontSize = DsType.Caption,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
