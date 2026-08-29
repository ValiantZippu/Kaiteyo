package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

// ============================================
// KAITEYO DESIGN SYSTEM — TOOLBAR
// Standard toolbar with title + optional subtitle
// and trailing action slot. Used by views that
// need a page-level header but don't use the
// full DsPageShell wrapper.
// ============================================

@Composable
fun DsToolbar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable () -> Unit = {}
) {
    val sc = surfaceColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(sc.background)
            .padding(horizontal = DsSpacing.Lg, vertical = DsSpacing.Md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = sc.textPrimary,
                fontSize = DsType.Title,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = sc.textMuted,
                    fontSize = DsType.Caption
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)) {
            actions()
        }
    }
}
