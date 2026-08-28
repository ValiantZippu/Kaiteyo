package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — BADGE
// Small labeled chip for status/tags.
// ============================================

@Composable
fun DsBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(DsRadius.Full)
    Text(
        text = text,
        color = Color.White,
        fontSize = DsType.Overline,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(shape)
            .background(color)
            .padding(horizontal = DsSpacing.Sm, vertical = 2.dp)
    )
}
