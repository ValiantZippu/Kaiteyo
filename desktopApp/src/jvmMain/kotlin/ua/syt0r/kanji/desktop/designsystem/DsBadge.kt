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
// ============================================

@Composable
fun DsBadge(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    label: String = text,
    colorHex: String? = null
) {
    val sc = surfaceColors()
    val resolvedColor = when {
        colorHex != null -> parseHexColor(colorHex)
        tint != Color.Unspecified -> tint
        else -> sc.textSecondary
    }
    val bgColor = resolvedColor.copy(alpha = 0.15f)

    Text(
        text = label,
        color = resolvedColor,
        fontSize = DsType.Caption,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(RoundedCornerShape(DsRadius.Full))
            .background(bgColor)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Xs)
    )
}
