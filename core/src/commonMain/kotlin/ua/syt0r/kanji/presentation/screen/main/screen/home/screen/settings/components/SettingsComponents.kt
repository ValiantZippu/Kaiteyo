package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import ua.syt0r.kanji.presentation.common.CommonTimeFormat
import ua.syt0r.kanji.presentation.common.MultiplatformDialog
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.Dimens
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalRadiusConfig
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors

// ============================================
// KAITEYO SETTINGS COMPONENT SYSTEM
// Reusable building blocks for the Settings
// Center — every category page and search
// result is composed from these primitives so
// the whole system stays visually consistent.
// ============================================

@Composable
internal fun scaledRadius(base: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp {
    val multiplier = LocalRadiusConfig.current.style.globalMultiplier
    return base * multiplier
}

// ============================================
// GROUP CARD
// ============================================

@Composable
fun SettingGroup(
    title: String?,
    children: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier
) {
    if (children.isEmpty()) return
    val surfaceColors = LocalSurfaceColors.current
    Column(modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = surfaceColors.textMuted,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = Dimens.Space1, bottom = Dimens.Space2)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusLg)))
                .background(surfaceColors.surfaceElevated.copy(alpha = 0.6f))
                .border(
                    width = 1.dp,
                    color = surfaceColors.border.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(scaledRadius(Dimens.RadiusLg))
                )
        ) {
            children.forEachIndexed { index, child ->
                if (index > 0) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.Space4)
                            .height(1.dp)
                            .background(surfaceColors.border.copy(alpha = 0.25f))
                    )
                }
                child()
            }
        }
    }
}

// ============================================
// BASE ROW
// ============================================

@Composable
fun SettingRow(
    title: String,
    description: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val baseModifier = modifier
        .fillMaxWidth()
        .then(
            if (onClick != null) {
                Modifier
                    .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
                    .background(if (isHovered) surfaceColors.surfaceInteractive.copy(alpha = 0.5f) else Color.Transparent)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        onClick = onClick
                    )
                    .hoverable(interactionSource)
            } else {
                Modifier
            }
        )
        .padding(horizontal = Dimens.Space4, vertical = Dimens.Space3)

    Row(
        modifier = baseModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) surfaceColors.textPrimary else surfaceColors.textMuted,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (description != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = surfaceColors.textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

// ============================================
// TOGGLE
// ============================================

@Composable
fun ToggleSetting(
    title: String,
    description: String? = null,
    checked: Boolean,
    onChanged: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    SettingRow(
        title = title,
        description = description,
        enabled = enabled,
        trailing = {
            Switch(checked = checked, onCheckedChange = onChanged, enabled = enabled)
        }
    )
}

// ============================================
// SLIDER
// ============================================

@Composable
fun SliderSetting(
    title: String,
    description: String? = null,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: (Float) -> String,
    onChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    Column(modifier.fillMaxWidth().padding(horizontal = Dimens.Space4, vertical = Dimens.Space3)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = surfaceColors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = surfaceColors.textMuted,
                        maxLines = 2
                    )
                }
            }
            Text(
                text = valueLabel(value),
                style = MaterialTheme.typography.labelSmall,
                color = surfaceColors.textSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onChanged,
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ============================================
// NUMBER (stepper)
// ============================================

@Composable
fun NumberSetting(
    title: String,
    description: String? = null,
    value: Int,
    range: IntRange,
    onChanged: (Int) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    SettingRow(
        title = title,
        description = description,
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StepperButton(Icons.Default.Remove, enabled = value > range.first) {
                    onChanged((value - 1).coerceIn(range))
                }
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = surfaceColors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                StepperButton(Icons.Default.Add, enabled = value < range.last) {
                    onChanged((value + 1).coerceIn(range))
                }
            }
        }
    )
}

@Composable
private fun StepperButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusSm)))
            .background(if (enabled) surfaceColors.surfaceInteractive else surfaceColors.surface)
            .border(
                1.dp,
                surfaceColors.border.copy(alpha = 0.4f),
                RoundedCornerShape(scaledRadius(Dimens.RadiusSm))
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) accent.primary else surfaceColors.border,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ============================================
// SEGMENTED CHOICE
// ============================================

@Composable
fun <T> SegmentedSetting(
    title: String,
    description: String? = null,
    options: List<T>,
    labelOf: (T) -> String,
    selected: T,
    onSelected: (T) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Column(Modifier.fillMaxWidth().padding(horizontal = Dimens.Space4, vertical = Dimens.Space3)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = surfaceColors.textPrimary,
            fontWeight = FontWeight.Medium
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = surfaceColors.textMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(Modifier.height(Dimens.Space2))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
                        .background(
                            when {
                                isSelected -> accent.primary.copy(alpha = 0.16f)
                                isHovered -> surfaceColors.surfaceInteractive
                                else -> surfaceColors.surface
                            }
                        )
                        .border(
                            width = if (isSelected) 1.5f.dp else 1.dp,
                            color = if (isSelected) accent.primary
                            else surfaceColors.border.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(scaledRadius(Dimens.RadiusMd))
                        )
                        .clickable(interactionSource = interactionSource, indication = null) { onSelected(option) }
                        .hoverable(interactionSource)
                        .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2)
                ) {
                    Text(
                        text = labelOf(option),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) accent.primary else surfaceColors.textSecondary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ============================================
// DROPDOWN (row → picker dialog)
// ============================================

@Composable
fun <T> DropdownSetting(
    title: String,
    description: String? = null,
    options: List<T>,
    labelOf: (T) -> String,
    selected: T,
    onSelected: (T) -> Unit,
    enabled: Boolean = true
) {
    val surfaceColors = LocalSurfaceColors.current
    var showPicker by remember { mutableStateOf(false) }
    SettingRow(
        title = title,
        description = description,
        enabled = enabled,
        onClick = { if (enabled) showPicker = true },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = labelOf(selected),
                    style = MaterialTheme.typography.labelLarge,
                    color = surfaceColors.textSecondary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = surfaceColors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    )

    if (showPicker) {
        MultiplatformDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(title) },
            content = {
                Column {
                    options.forEach { option ->
                        val isSelected = option == selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
                                .background(
                                    if (isSelected) LocalKaiteyoAccent.current.primary.copy(alpha = 0.14f)
                                    else Color.Transparent
                                )
                                .clickable { onSelected(option); showPicker = false }
                                .padding(horizontal = Dimens.Space3, vertical = Dimens.Space3),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = labelOf(option),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) LocalKaiteyoAccent.current.primary
                                else surfaceColors.textPrimary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(LocalKaiteyoAccent.current.primary)
                                )
                            }
                        }
                    }
                }
            },
            buttons = {
                TextButton({ showPicker = false }) {
                    Text(resolveString { settings.pickerDialogCancel })
                }
            }
        )
    }
}

// ============================================
// COLOR SWATCHES
// ============================================

@Composable
fun ColorSetting(
    title: String,
    description: String? = null,
    swatches: List<Pair<String, Color>>,
    selectedName: String,
    onSelect: (String) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Column(Modifier.fillMaxWidth().padding(horizontal = Dimens.Space4, vertical = Dimens.Space3)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = surfaceColors.textPrimary,
            fontWeight = FontWeight.Medium
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = surfaceColors.textMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(Modifier.height(Dimens.Space2))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            swatches.forEach { (name, color) ->
                val isSelected = name == selectedName
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.5.dp,
                            color = if (isSelected) accent.primary else surfaceColors.border.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable(interactionSource = interactionSource, indication = null) { onSelect(name) }
                        .hoverable(interactionSource)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected || isHovered) {
                        Box(
                            Modifier
                                .size(if (isSelected) 10.dp else 12.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) accent.primary
                                    else Color.White.copy(alpha = 0.9f)
                                )
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// TIME PICKER ROW
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSetting(
    title: String,
    description: String? = null,
    value: LocalTime,
    onChanged: (LocalTime) -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    var showPicker by remember { mutableStateOf(false) }
    SettingRow(
        title = title,
        description = description,
        onClick = { showPicker = true },
        trailing = {
            Text(
                text = value.format(CommonTimeFormat),
                style = MaterialTheme.typography.labelLarge,
                color = surfaceColors.textSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    )

    if (showPicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = value.hour,
            initialMinute = value.minute,
            is24Hour = true
        )
        MultiplatformDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(title) },
            content = {
                TimeInput(
                    state = timePickerState,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            },
            buttons = {
                TextButton({ showPicker = false }) {
                    Text(resolveString { settings.pickerDialogCancel })
                }
                TextButton({
                    onChanged(LocalTime(timePickerState.hour, timePickerState.minute))
                    showPicker = false
                }) {
                    Text(resolveString { settings.pickerDialogApply })
                }
            }
        )
    }
}

// ============================================
// LINK ROW
// ============================================

@Composable
fun LinkSetting(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
            .background(if (isHovered) surfaceColors.surfaceInteractive.copy(alpha = 0.5f) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(horizontal = Dimens.Space4, vertical = Dimens.Space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
                    .background(accent.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = surfaceColors.textPrimary,
                fontWeight = FontWeight.Medium
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = surfaceColors.textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (isHovered) accent.primary else surfaceColors.textMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ============================================
// DANGER ACTION ROW
// ============================================

@Composable
fun DangerActionSetting(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val danger = MaterialTheme.colorScheme.error
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
            .background(
                if (isHovered) danger.copy(alpha = 0.1f) else Color.Transparent
            )
            .border(
                width = 1.dp,
                color = if (isHovered) danger.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(scaledRadius(Dimens.RadiusMd))
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(horizontal = Dimens.Space4, vertical = Dimens.Space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
                    .background(danger.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = danger,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = danger,
                fontWeight = FontWeight.SemiBold
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = surfaceColors.textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ============================================
// INFO ROW (read-only)
// ============================================

@Composable
fun InfoSetting(
    title: String,
    description: String? = null,
    value: String
) {
    val surfaceColors = LocalSurfaceColors.current
    SettingRow(
        title = title,
        description = description,
        trailing = {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = surfaceColors.textSecondary,
                maxLines = 1
            )
        }
    )
}

// ============================================
// PAGE HEADER
// ============================================

@Composable
fun SettingHeader(
    icon: ImageVector?,
    title: String,
    subtitle: String,
    onReset: (() -> Unit)? = null
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusLg)))
            .background(surfaceColors.surfaceElevated.copy(alpha = 0.6f))
            .border(
                1.dp,
                surfaceColors.border.copy(alpha = 0.35f),
                RoundedCornerShape(scaledRadius(Dimens.RadiusLg))
            )
            .padding(horizontal = Dimens.Space4, vertical = Dimens.Space4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
                    .background(accent.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = surfaceColors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = surfaceColors.textMuted
            )
        }
        if (onReset != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
                    .background(surfaceColors.surfaceInteractive)
                    .border(
                        1.dp,
                        surfaceColors.border.copy(alpha = 0.4f),
                        RoundedCornerShape(scaledRadius(Dimens.RadiusMd))
                    )
                    .clickable(onClick = onReset)
                    .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = surfaceColors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = resolveString { center.resetToDefaults },
                        style = MaterialTheme.typography.labelMedium,
                        color = surfaceColors.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ============================================
// SEARCH FIELD
// ============================================

@Composable
fun SettingsSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(scaledRadius(Dimens.RadiusMd)))
            .background(surfaceColors.surfaceInteractive.copy(alpha = 0.6f))
            .border(
                1.dp,
                surfaceColors.border.copy(alpha = 0.4f),
                RoundedCornerShape(scaledRadius(Dimens.RadiusMd))
            )
            .padding(horizontal = Dimens.Space3, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = if (value.isNotBlank()) accent.primary else surfaceColors.textMuted,
            modifier = Modifier.size(18.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = surfaceColors.textPrimary,
                fontSize = 14.sp
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = surfaceColors.textMuted
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (value.isNotBlank()) {
            IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(22.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = surfaceColors.textMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
