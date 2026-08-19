package ua.syt0r.kanji.presentation.common.ui.kanji

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

private val MajorSegmentLength = 8.dp
private val MajorSegmentWidth = 1.5.dp
private val MinorSegmentLength = 6.dp
private val MinorSegmentWidth = 0.5.dp

/**
 * Premium writing grid background.
 * Uses the current theme accent at very low opacity for a subtle, cohesive feel.
 * Diagonal division lines + quarter lines = full hanjie-style practice grid.
 */
@Composable
fun KanjiBackground(
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.outline
) {
    Canvas(modifier) {

        // Subtle radial gradient wash from the center — like premium paper
        val washColors = listOf(
            Color.Transparent,
            lineColor.copy(alpha = 0.02f),
            Color.Transparent
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = washColors,
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.maxDimension * 0.55f
            )
        )

        val majorLenPx = MajorSegmentLength.toPx()
        val majorW = MajorSegmentWidth.toPx()
        val minorLenPx = MinorSegmentLength.toPx()
        val minorW = MinorSegmentWidth.toPx()

        // Major center cross
        val majorColor = lineColor.copy(alpha = 0.35f)
        val minorColor = lineColor.copy(alpha = 0.18f)

        // Horizontal center (major)
        drawDashedLine(
            Orientation.Horizontal,
            ceil(size.maxDimension / majorLenPx / 2).toInt(),
            Size(majorLenPx, majorW),
            Offset(0f, size.height / 2f - majorW / 2f),
            majorColor
        )

        // Vertical center (major)
        drawDashedLine(
            Orientation.Vertical,
            ceil(size.maxDimension / majorLenPx / 2).toInt(),
            Size(majorW, majorLenPx),
            Offset(size.width / 2f - majorW / 2f, 0f),
            majorColor
        )

        // Quarter lines (minor) — vertical
        drawDashedLine(
            Orientation.Vertical,
            ceil(size.maxDimension / minorLenPx / 2).toInt(),
            Size(minorW, minorLenPx),
            Offset(size.width / 4f - minorW / 2f, 0f),
            minorColor
        )
        drawDashedLine(
            Orientation.Vertical,
            ceil(size.maxDimension / minorLenPx / 2).toInt(),
            Size(minorW, minorLenPx),
            Offset(size.width * 3f / 4f - minorW / 2f, 0f),
            minorColor
        )

        // Quarter lines (minor) — horizontal
        drawDashedLine(
            Orientation.Horizontal,
            ceil(size.maxDimension / minorLenPx / 2).toInt(),
            Size(minorLenPx, minorW),
            Offset(0f, size.height / 4f - minorW / 2f),
            minorColor
        )
        drawDashedLine(
            Orientation.Horizontal,
            ceil(size.maxDimension / minorLenPx / 2).toInt(),
            Size(minorLenPx, minorW),
            Offset(0f, size.height * 3f / 4f - minorW / 2f),
            minorColor
        )

        // Diagonal lines (very subtle) — top-left to bottom-right
        val diagAlpha = 0.08f
        val diagColor = lineColor.copy(alpha = diagAlpha)
        drawLine(
            color = diagColor,
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height),
            strokeWidth = 0.5.dp.toPx()
        )
        // top-right to bottom-left
        drawLine(
            color = diagColor,
            start = Offset(size.width, 0f),
            end = Offset(0f, size.height),
            strokeWidth = 0.5.dp.toPx()
        )

        // Outer border — thin accent line
        drawRect(
            color = lineColor.copy(alpha = 0.25f),
            topLeft = Offset.Zero,
            size = size,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )
    }
}

private fun DrawScope.drawDashedLine(
    orientation: Orientation,
    segmentsCount: Int,
    lineSegmentSize: Size,
    startOffset: Offset,
    color: Color,
) {
    for (i in 0 until segmentsCount) {
        val offset = when (orientation) {
            Orientation.Vertical -> startOffset + Offset(0f, lineSegmentSize.height * 2 * i)
            Orientation.Horizontal -> startOffset + Offset(lineSegmentSize.width * 2 * i, 0f)
        }
        drawRect(color, offset, lineSegmentSize)
    }
}
