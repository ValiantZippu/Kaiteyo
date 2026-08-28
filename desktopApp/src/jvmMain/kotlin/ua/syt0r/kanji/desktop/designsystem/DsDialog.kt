package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

// ============================================
// KAITEYO DESIGN SYSTEM — DIALOG
// ============================================

/**
 * General-purpose dialog with title, dismiss, and a content slot.
 */
@Composable
fun DsDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
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
            content()
        }
    }
}

/**
 * Prompt dialog with a text input field.
 */
@Composable
fun DsPromptDialog(
    title: String,
    placeholder: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialValue: String = "",
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    message: String = ""
) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Lg)
    var textValue by remember { mutableStateOf(initialValue) }

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
            if (message.isNotBlank()) {
                Text(
                    text = message,
                    color = sc.textSecondary,
                    fontSize = DsType.Body
                )
            }
            DsTextField(
                value = textValue,
                onValueChange = { textValue = it },
                placeholder = placeholder,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(DsSpacing.Xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
            ) {
                DsButton(
                    text = dismissText,
                    onClick = onDismiss,
                    kind = DsButtonKind.Ghost
                )
                DsButton(
                    text = confirmText,
                    onClick = { onConfirm(textValue) },
                    kind = DsButtonKind.Primary
                )
            }
        }
    }
}

/**
 * Simple confirm / cancel dialog.
 */
@Composable
fun DsConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    danger: Boolean = false
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
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm, Alignment.End)
            ) {
                DsButton(
                    text = dismissText,
                    onClick = onDismiss,
                    kind = DsButtonKind.Ghost
                )
                DsButton(
                    text = confirmText,
                    onClick = onConfirm,
                    kind = if (danger) DsButtonKind.Danger else DsButtonKind.Primary
                )
            }
        }
    }
}
