package ua.syt0r.kanji.presentation.screen.main.screen.practice_common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Brush selector toolbar shown above the kanji drawing board.
 * Allows users to choose brush type, thickness, softness, and smoothing.
 */
@Composable
fun BrushSelector(
    brushSettings: BrushSettings,
    onBrushSettingsChange: (BrushSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Main brush toolbar row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Brush type selector
            BrushType.values().forEach { type ->
                val isSelected = brushSettings.brushType == type
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        Color.Transparent,
                    animationSpec = tween(200),
                    label = "brushBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "brushText"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .clickable {
                            onBrushSettingsChange(brushSettings.copy(brushType = type))
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Thickness/settings toggle
            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.size(32.dp)
            ) {
                Text(
                    text = if (expanded) "✕" else "⚙",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Expanded settings panel
        if (expanded) {
            BrushSettingsPanel(
                brushSettings = brushSettings,
                onBrushSettingsChange = onBrushSettingsChange
            )
        }
    }
}

@Composable
private fun BrushSettingsPanel(
    brushSettings: BrushSettings,
    onBrushSettingsChange: (BrushSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Thickness slider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Thickness",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(64.dp)
            )
            Slider(
                value = brushSettings.thickness,
                onValueChange = { onBrushSettingsChange(brushSettings.copy(thickness = it)) },
                valueRange = 0.3f..3.0f,
                steps = 26,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = formatFloat(brushSettings.thickness),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp)
            )
        }

        // Softness slider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Softness",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(64.dp)
            )
            Slider(
                value = brushSettings.softness,
                onValueChange = { onBrushSettingsChange(brushSettings.copy(softness = it)) },
                valueRange = 0.1f..1.0f,
                steps = 8,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = formatFloat(brushSettings.softness),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp)
            )
        }

        // Smoothing delay slider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Smooth",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(64.dp)
            )
            Slider(
                value = brushSettings.smoothingFactor,
                onValueChange = { onBrushSettingsChange(brushSettings.copy(smoothingFactor = it)) },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = "${(brushSettings.smoothingFactor * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp)
            )
        }

        // Preview stroke
        val previewColor = MaterialTheme.colorScheme.primary
        val previewStrokeWidth = brushSettings.resolveStrokeWidth(3f)
        val previewCap = brushSettings.resolveStrokeCap()
        val previewJoin = brushSettings.resolveStrokeJoin()
        val previewAlpha = brushSettings.resolveAlpha()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(top = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            ) {
                // Draw a preview swoosh line
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.1f, size.height * 0.8f)
                    cubicTo(
                        size.width * 0.3f, size.height * 0.1f,
                        size.width * 0.7f, size.height * 0.9f,
                        size.width * 0.9f, size.height * 0.2f
                    )
                }
                drawPath(
                    path = path,
                    color = previewColor,
                    alpha = previewAlpha,
                    style = Stroke(
                        width = previewStrokeWidth,
                        cap = previewCap,
                        join = previewJoin
                    )
                )
            }
        }
    }
}

/**
 * Format a float to one decimal place for display in the brush settings panel.
 * Multiplatform-compatible alternative to String.format.
 */
private fun formatFloat(value: Float): String {
    val intPart = value.toInt()
    val decimalPart = ((value - intPart) * 10).roundToInt()
    return "$intPart.$decimalPart"
}
