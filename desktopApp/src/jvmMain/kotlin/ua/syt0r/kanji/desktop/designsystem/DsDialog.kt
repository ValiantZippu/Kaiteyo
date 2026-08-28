package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

// ============================================
// KAITEYO DESIGN SYSTEM — DIALOG
// Consistent modal dialog.
// ============================================

@Composable
fun DsPromptDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel"
) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Lg)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .clip(shape)
                .background(sc.surface)
                .padding(DsSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
        ) {
            Text(
                text = title,
                color = sc.textPrimary,
                fontSize = DsType.Title,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                color = sc.textSecondary,
                fontSize = DsType.Body
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, androidx.compose.ui.Alignment.End)
            ) {
                DsButton(
                    text = dismissText,
                    onClick = onDismiss,
                    kind = DsButtonKind.Ghost
                )
                DsButton(
                    text = confirmText,
                    onClick = onConfirm,
                    kind = DsButtonKind.Primary
                )
            }
        }
    }
}
