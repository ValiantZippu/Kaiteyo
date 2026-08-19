package ua.syt0r.kanji.presentation.screen.main.screen.practice_common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import kotlin.math.roundToInt

@Composable
fun BrushSelector(
    brushSettings: BrushSettings,
    onBrushSettingsChange: (BrushSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val accent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColors.surface.copy(alpha = 0.85f))
            .border(1.dp, accent.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
    ) {
        // Main brush toolbar row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Brush type pills
            BrushType.entries.forEach { type ->
                val isSelected = brushSettings.brushType == type
                val bgAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = spring(),
                    label = "brushBgAlpha"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    accent.primary.copy(alpha = bgAlpha * 0.25f),
                                    accent.secondary.copy(alpha = bgAlpha * 0.15f)
                                )
                            )
                        )
                        .then(
                            if (isSelected) Modifier.border(
                                1.dp, accent.primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp)
                            ) else Modifier
                        )
                        .clickable {
                            onBrushSettingsChange(brushSettings.copy(brushType = type))
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mini brush preview
                        BrushPreviewDot(type = type, color = accent.primary, size = 10f)
                        Text(
                            text = type.displayNameJa,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) accent.primary else surfaceColors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Expand toggle
            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = accent.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Expanded settings panel with gradient background
        val panelHeight by animateFloatAsState(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.8f),
            label = "panelHeight"
        )

        if (panelHeight > 0.01f) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((160 * panelHeight).dp)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Thickness slider
                SliderRow(
                    label = "太さ",
                    value = brushSettings.thickness,
                    onValueChange = { onBrushSettingsChange(brushSettings.copy(thickness = it)) },
                    valueRange = 0.3f..3.0f,
                    steps = 26,
                    accent = accent.primary
                )

                // Softness slider
                SliderRow(
                    label = "透明度",
                    value = brushSettings.softness,
                    onValueChange = { onBrushSettingsChange(brushSettings.copy(softness = it)) },
                    valueRange = 0.1f..1.0f,
                    steps = 8,
                    accent = accent.primary
                )

                // Smoothing slider
                SliderRow(
                    label = "滑らか",
                    value = brushSettings.smoothingFactor,
                    onValueChange = { onBrushSettingsChange(brushSettings.copy(smoothingFactor = it)) },
                    valueRange = 0f..1f,
                    steps = 9,
                    accent = accent.primary,
                    valueLabel = "${(brushSettings.smoothingFactor * 100).toInt()}%"
                )

                // Pressure sensitivity toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.primary.copy(alpha = 0.06f))
                        .clickable {
                            onBrushSettingsChange(
                                brushSettings.copy(pressureEnabled = !brushSettings.pressureEnabled)
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "圧力感応",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = surfaceColors.textSecondary
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp, 20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (brushSettings.pressureEnabled) accent.primary
                                else surfaceColors.surfaceInteractive
                            )
                            .clickable {
                                onBrushSettingsChange(
                                    brushSettings.copy(pressureEnabled = !brushSettings.pressureEnabled)
                                )
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .offset(x = if (brushSettings.pressureEnabled) 18.dp else 2.dp, y = 0.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }

                // Live brush preview
                LiveBrushPreview(brushSettings = brushSettings, color = accent.primary)
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    accent: Color,
    valueLabel: String = formatFloat(value)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = accent.copy(alpha = 0.15f)
            )
        )
        Text(
            text = valueLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            modifier = Modifier.width(32.dp)
        )
    }
}

@Composable
private fun LiveBrushPreview(
    brushSettings: BrushSettings,
    color: Color
) {
    val surfaceColors = LocalSurfaceColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(surfaceColors.surfaceInteractive.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        ) {
            val path = Path().apply {
                moveTo(size.width * 0.08f, size.height * 0.65f)
                cubicTo(
                    size.width * 0.25f, size.height * 0.15f,
                    size.width * 0.55f, size.height * 0.85f,
                    size.width * 0.92f, size.height * 0.25f
                )
            }
            val strokeWidth = brushSettings.resolveStrokeWidth(2f)
            drawPath(
                path = path,
                color = color,
                alpha = brushSettings.resolveAlpha(),
                style = Stroke(
                    width = strokeWidth,
                    cap = brushSettings.resolveStrokeCap(),
                    join = brushSettings.resolveStrokeJoin()
                )
            )
        }
    }
}

@Composable
private fun BrushPreviewDot(type: BrushType, color: Color, size: Float) {
    Canvas(modifier = Modifier.size(size.dp)) {
        val r = this.size.minDimension / 2f
        when (type) {
            BrushType.Pen -> drawCircle(color, radius = r)
            BrushType.Calligraphy -> drawCircle(color, radius = r * 0.9f)
            BrushType.Pencil -> drawCircle(color, radius = r * 0.7f, alpha = 0.6f)
            BrushType.Side -> {
                drawRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, r * 0.3f),
                    size = androidx.compose.ui.geometry.Size(this.size.width, this.size.height * 0.4f)
                )
            }
        }
    }
}

private fun formatFloat(value: Float): String {
    val intPart = value.toInt()
    val decimalPart = ((value - intPart) * 10).roundToInt()
    return "$intPart.$decimalPart"
}
