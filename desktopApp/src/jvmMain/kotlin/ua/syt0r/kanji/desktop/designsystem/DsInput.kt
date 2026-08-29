package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
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
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Sm)
    androidx.compose.foundation.layout.Column(modifier) {
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
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(sc.surfaceVariant)
                .border(1.dp, sc.border, shape)
                .then(
                    if (leadingIcon != null || trailingIcon != null) {
                        Modifier.padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm)
                    } else {
                        Modifier.padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm)
                    }
                ),
            decorationBox = { innerTextField ->
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    if (leadingIcon != null) {
                        androidx.compose.material3.Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = sc.textMuted,
                            modifier = Modifier.padding(end = DsSpacing.Sm)
                        )
                    }
                    androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                text = placeholder,
                                color = sc.textDisabled,
                                fontSize = DsType.Body
                            )
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
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null
) {
    DsTextField(
        value = value,
        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' || c == '-' }) onValueChange(it) },
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
            androidx.compose.foundation.layout.Box {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        color = sc.textDisabled,
                        fontSize = DsType.Body
                    )
                }
                innerTextField()
            }
        }
    )
}
