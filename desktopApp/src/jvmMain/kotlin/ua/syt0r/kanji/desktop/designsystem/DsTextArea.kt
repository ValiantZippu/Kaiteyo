package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — TEXT AREA
// Monospace-free multiline editor for JSON/text
// payloads (theme import, transfer previews).
// ============================================

@Composable
fun DsTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    readOnly: Boolean = false
) {
    val sc = surfaceColors()
    val ac = accent()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(sc.surfaceElevated)
            .padding(DsSpacing.Md)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = sc.textPrimary, fontSize = DsType.Body),
            cursorBrush = SolidColor(ac.primary),
            readOnly = readOnly,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        )
    }
}
