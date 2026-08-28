package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ============================================
// KAITEYO DESIGN SYSTEM — TAB ROW
// Pill-style tab selector.
// ============================================

@Composable
fun DsTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val shape = RoundedCornerShape(DsRadius.Sm)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(sc.surfaceVariant)
            .padding(DsSpacing.Xs),
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
    ) {
        tabs.forEachIndexed { index, label ->
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            val isSelected = index == selectedIndex

            Text(
                text = label,
                color = if (isSelected) sc.textOnAccent else sc.textSecondary,
                fontSize = DsType.Body,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(DsRadius.Sm))
                    .background(
                        when {
                            isSelected -> sc.accent
                            isHovered -> sc.hoverOverlay
                            else -> androidx.compose.ui.graphics.Color.Transparent
                        }
                    )
                    .hoverable(interactionSource)
                    .clickable { onSelect(index) }
                    .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm)
            )
        }
    }
}
