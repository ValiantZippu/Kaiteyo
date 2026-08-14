package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — TEXT AREA
// Monospace-free multiline editor for JSON/text
// payloads (theme import, transfer previews).
// Shares the focus treatment with DsTextField:
// the bottom hairline glows with the accent
// while focused.
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
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (focused) ac.primary else sc.border,
        animationSpec = tween(180),
        label = "textAreaBorder"
    )
    val shape = RoundedCornerShape(DsRadius.Md)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(sc.surfaceElevated)
            .border(1.dp, borderColor.copy(alpha = if (focused) 1f else 0.4f), shape)
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
                .onFocusChanged { focused = it.isFocused }
                .verticalScroll(rememberScrollState())
        )
    }
}
