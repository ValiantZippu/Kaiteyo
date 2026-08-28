package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — INPUTS
// DsTextField, DsNumericField
// ============================================

@Composable
fun DsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String = "",
    singleLine: Boolean = true
) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Sm)

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .clip(shape)
            .border(1.dp, sc.border, shape),
        placeholder = if (placeholder.isNotBlank()) {
            { Text(placeholder, color = sc.textMuted, fontSize = DsType.Body) }
        } else null,
        label = if (label.isNotBlank()) {
            { Text(label, color = sc.textSecondary, fontSize = DsType.Caption) }
        } else null,
        singleLine = singleLine,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = sc.surface,
            unfocusedContainerColor = sc.surface,
            focusedTextColor = sc.textPrimary,
            unfocusedTextColor = sc.textPrimary,
            cursorColor = sc.accent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = DsType.Body)
    )
}

@Composable
fun DsNumericField(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String = ""
) {
    DsTextField(
        value = if (value == 0) "" else value.toString(),
        onValueChange = { onValueChange(it.toIntOrNull() ?: 0) },
        modifier = modifier,
        placeholder = placeholder,
        label = label,
        singleLine = true
    )
}
