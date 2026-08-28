package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — SELECT
// Simple dropdown selector.
// ============================================

@Composable
fun DsSelect(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Sm)
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .clip(shape)
                .border(1.dp, sc.border, shape)
                .background(sc.surface)
                .clickable { expanded = true }
                .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Xs)
        ) {
            Text(
                text = options.getOrElse(selected) { "" },
                color = sc.textPrimary,
                fontSize = DsType.Body,
                fontWeight = FontWeight.Medium
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            fontSize = DsType.Body,
                            fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}
