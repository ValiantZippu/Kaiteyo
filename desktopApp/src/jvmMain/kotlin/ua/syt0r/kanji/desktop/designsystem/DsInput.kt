package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================
// KAITEYO DESIGN SYSTEM — INPUTS
// DsTextField, DsNumericField, DsTextArea
// ============================================

@Composable
fun DsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    autoFocus: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Sm)
    Column(modifier) {
        if (label != null) {
            Text(
                text = label,
                color = sc.textSecondary,
                fontSize = DsType.Caption,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = DsSpacing.Xs)
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = TextStyle(
                color = if (enabled) sc.textPrimary else sc.textDisabled,
                fontSize = DsType.Body
            ),
            cursorBrush = SolidColor(sc.accent),
            singleLine = singleLine,
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(sc.surfaceVariant)
                .border(1.dp, sc.border, shape)
                .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
            decorationBox = { innerTextField ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = sc.textMuted,
                            modifier = Modifier.padding(end = DsSpacing.Sm)
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(text = placeholder, color = sc.textDisabled, fontSize = DsType.Body)
                        }
                        innerTextField()
                    }
                    if (trailingIcon != null) {
                        trailingIcon()
                    }
                }
            }
        )
    }
}

@Composable
fun DsNumericField(
    value: Any,
    onValueChange: (Any) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null
) {
    val strValue = value.toString()
    DsTextField(
        value = strValue,
        onValueChange = { newText ->
            if (newText.isEmpty() || newText.all { c -> c.isDigit() || c == '.' || c == '-' }) {
                val intValue = newText.toIntOrNull()
                if (intValue != null) onValueChange(intValue)
                else onValueChange(newText)
            }
        },
        modifier = modifier,
        placeholder = placeholder,
        label = label
    )
}

/**
 * Multi-line text area with fixed height.
 */
@Composable
fun DsTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    readOnly: Boolean = false,
    placeholder: String = ""
) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Sm)
    BasicTextField(
        value = value,
        onValueChange = { if (!readOnly) onValueChange(it) },
        readOnly = readOnly,
        textStyle = TextStyle(
            color = if (readOnly) sc.textMuted else sc.textPrimary,
            fontSize = DsType.Body,
            lineHeight = 20.sp
        ),
        cursorBrush = SolidColor(sc.accent),
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(sc.surfaceVariant)
            .border(1.dp, sc.border, shape)
            .padding(DsSpacing.Md),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(text = placeholder, color = sc.textDisabled, fontSize = DsType.Body)
                }
                innerTextField()
            }
        }
    )
}
