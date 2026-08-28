package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — EMPTY STATE
// Consistent empty/error state placeholder.
// ============================================

@Composable
fun DsEmptyState(
    title: String,
    message: String = "",
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    action: @Composable () -> Unit = {}
) {
    val sc = surfaceColors()
    Column(
        modifier = modifier.padding(DsSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
    ) {
        if (icon != null) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = sc.textMuted,
                modifier = Modifier.padding(bottom = DsSpacing.Xs)
            )
        }
        Text(
            text = title,
            color = sc.textPrimary,
            fontSize = DsType.Title,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        if (message.isNotBlank()) {
            Text(
                text = message,
                color = sc.textMuted,
                fontSize = DsType.Body,
                textAlign = TextAlign.Center
            )
        }
        action()
    }
}
