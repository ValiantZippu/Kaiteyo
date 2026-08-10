package ua.syt0r.kanji.desktop.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.desktop.engine.settings.SettingCategory

// ============================================
// KAITEYO DESIGN SYSTEM — SELECT / DROPDOWN
// ============================================

@Composable
fun <T> DsSelect(
    selected: T,
    options: List<T>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelOf: (T) -> String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var expanded by remember { mutableStateOf(false) }
    // The chevron rotates smoothly when the menu opens/closes.
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "selectChevron"
    )

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DsRadius.Sm))
                .background(if (hovered) sc.surfaceInteractive else sc.surfaceElevated)
                .border(1.dp, if (expanded) ac.primary.copy(alpha = 0.6f) else sc.border.copy(alpha = 0.4f), RoundedCornerShape(DsRadius.Sm))
                .clickable(interactionSource = interaction, indication = null) { expanded = true }
                .hoverable(interaction)
                .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = ac.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(DsSpacing.Sm))
            }
            Text(
                text = labelOf(selected),
                color = sc.textPrimary,
                fontSize = DsType.Body,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = if (expanded) ac.primary else sc.textMuted,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = chevronRotation }
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(sc.surfaceElevated)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            labelOf(option),
                            color = if (option == selected) ac.primary else sc.textPrimary,
                            fontSize = DsType.Body
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    }
                )
            }
        }
    }
}

/** Reusable tab bar with animated indicator. */
@Composable
fun DsTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    val ac = accent()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(sc.surfaceElevated)
            .padding(DsSpacing.Xs),
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
    ) {
        tabs.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(DsRadius.Sm))
                    .background(if (selected) ac.primary.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = DsSpacing.Sm),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) ac.primary else sc.textSecondary,
                    fontSize = DsType.Label,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

/** Pills/chips used for filter toggles and preset selection. */
@Composable
fun DsChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null
) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(DsRadius.Full))
            .background(
                when {
                    selected -> ac.primary.copy(alpha = 0.2f)
                    hovered -> sc.surfaceInteractive
                    else -> sc.surfaceElevated
                }
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
    ) {
        Text(
            text = text,
            color = if (selected) ac.primary else sc.textSecondary,
            fontSize = DsType.Label,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (trailing != null) {
            Text(
                text = trailing,
                color = if (selected) ac.primary.copy(alpha = 0.7f) else sc.textMuted,
                fontSize = DsType.Caption
            )
        }
    }
}

/** Category badge for settings grouping. */
@Composable
fun DsCategoryBadge(category: SettingCategory, modifier: Modifier = Modifier) {
    val ac = accent()
    val sc = surfaceColors()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(DsRadius.Sm))
            .background(ac.primary.copy(alpha = 0.12f))
            .padding(horizontal = DsSpacing.Sm, vertical = DsSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category.name,
            color = ac.primary,
            fontSize = DsType.Caption,
            fontWeight = FontWeight.Medium
        )
    }
}
