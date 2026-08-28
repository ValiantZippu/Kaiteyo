package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — SEARCH FIELD
// ============================================

@Composable
fun DsSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search…"
) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Sm)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, sc.border, shape)
            .background(sc.surface)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        textStyle = androidx.compose.ui.text.TextStyle(
            color = sc.textPrimary,
            fontSize = DsType.Body
        ),
        singleLine = true,
        cursorBrush = SolidColor(sc.accent),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = sc.textMuted,
                    fontSize = DsType.Body
                )
            }
            inner()
        }
    )
}
