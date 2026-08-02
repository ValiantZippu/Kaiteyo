package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================
// KAITEYO DESIGN SYSTEM — INPUTS
// ============================================

@Composable
fun DsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    singleLine: Boolean = true
) {
    val sc = surfaceColors()
    val ac = accent()
    var focused by remember { mutableStateOf(false) }

    val borderColor = when {
        focused -> ac.primary
        else -> sc.border
    }

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                color = sc.textSecondary,
                fontSize = DsType.Label,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = DsSpacing.Xs)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DsRadius.Sm))
                .background(sc.surfaceElevated)
                .then(
                    Modifier
                        .focusable()
                        .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
                )
                .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = if (focused) ac.primary else sc.textMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(DsSpacing.Sm))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = sc.textPrimary, fontSize = DsType.Body),
                cursorBrush = SolidColor(ac.primary),
                singleLine = singleLine,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(DsSpacing.Xs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(borderColor.copy(alpha = if (focused) 1f else 0.25f))
        )
    }
}

/** Search field with clear button — the canonical browser/global input. */
@Composable
fun DsSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search…",
    autoFocus: Boolean = false
) {
    val sc = surfaceColors()
    val ac = accent()
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    androidx.compose.runtime.LaunchedEffect(autoFocus) {
        if (autoFocus) focusRequester.requestFocus()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(sc.surfaceInteractive)
            .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = sc.textMuted,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(DsSpacing.Sm))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = sc.textPrimary, fontSize = DsType.BodyLarge),
            cursorBrush = SolidColor(ac.primary),
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
        )
        if (value.isNotEmpty()) {
            androidx.compose.material3.IconButton(onClick = { onValueChange("") }) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Clear",
                    tint = sc.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun DsNumericField(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    DsTextField(
        value = text,
        onValueChange = { raw ->
            val filtered = raw.filter { it.isDigit() }.take(6)
            text = filtered
            onValueChange(filtered.toIntOrNull() ?: 0)
        },
        modifier = modifier,
        label = label,
        singleLine = true
    )
}
