package ua.syt0r.kanji.presentation.common.ui.kaiteyo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors

// ============================================================
// CUMULATIVE USE GRAPH
//
// Shows how many kanji you need to cover what percentage of text.
// X-axis: number of kanji (0 → 2200)
// Y-axis: percentage of common text (0% → 100%)
// Milestone markers at 50, 100, 200, 500, 1000, 2000
// ============================================================

data class CoverageDataPoint(
    val kanjiCount: Int,
    val coveragePercent: Float // 0.0 - 100.0
)

// Default cumulative coverage curve based on real frequency data.
// Approximation: top 50 ≈ 25%, 100 ≈ 40%, 200 ≈ 55%, 500 ≈ 75%, 1000 ≈ 90%, 2000 ≈ 98%
private val defaultCoverageData = listOf(
    CoverageDataPoint(0, 0f),
    CoverageDataPoint(10, 8f),
    CoverageDataPoint(25, 16f),
    CoverageDataPoint(50, 25f),
    CoverageDataPoint(100, 40f),
    CoverageDataPoint(200, 55f),
    CoverageDataPoint(300, 63f),
    CoverageDataPoint(500, 75f),
    CoverageDataPoint(750, 84f),
    CoverageDataPoint(1000, 90f),
    CoverageDataPoint(1500, 95f),
    CoverageDataPoint(2000, 98f),
    CoverageDataPoint(2200, 99f)
)

private val milestoneMarkers = listOf(
    50 to "25%",
    100 to "40%",
    200 to "55%",
    500 to "75%",
    1000 to "90%",
    2000 to "98%"
)

@Composable
fun KaiteyoCumulativeUseGraph(
    userKanjiCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val textMeasurer = rememberTextMeasurer()

    val maxKanji = 2200
    val maxPercent = 100f

    KaiteyoCard(
        modifier = modifier,
        header = "Cumulative Coverage",
        subtitle = "How many kanji cover what % of common text"
    ) {
        // The chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(surfaceColors.surfaceInteractive.copy(alpha = 0.2f))
                .padding(start = 40.dp, end = 16.dp, top = 12.dp, bottom = 28.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            fun xForKanji(count: Int) = (count.toFloat() / maxKanji) * canvasWidth
            fun yForPercent(pct: Float) = canvasHeight - (pct / maxPercent) * canvasHeight

            // Grid lines
            listOf(0f, 25f, 50f, 75f, 100f).forEach { pct ->
                val y = yForPercent(pct)
                drawLine(
                    color = surfaceColors.textMuted.copy(alpha = 0.12f),
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Milestone vertical lines
            milestoneMarkers.forEach { (kanjiCount, _) ->
                val x = xForKanji(kanjiCount)
                drawLine(
                    color = accent.primary.copy(alpha = 0.15f),
                    start = Offset(x, 0f),
                    end = Offset(x, canvasHeight),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                )
            }

            // Filled area under curve
            val fillPath = Path().apply {
                moveTo(xForKanji(0), yForPercent(0f))
                defaultCoverageData.forEach { point ->
                    lineTo(xForKanji(point.kanjiCount), yForPercent(point.coveragePercent))
                }
                lineTo(xForKanji(maxKanji), yForPercent(0f))
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accent.primary.copy(alpha = 0.35f),
                        accent.primary.copy(alpha = 0.05f)
                    )
                )
            )

            // The curve line
            val linePath = Path().apply {
                defaultCoverageData.forEachIndexed { index, point ->
                    val x = xForKanji(point.kanjiCount)
                    val y = yForPercent(point.coveragePercent)
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(
                path = linePath,
                color = accent.primary,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Milestone dots
            milestoneMarkers.forEach { (kanjiCount, label) ->
                // Find the coverage at this kanji count
                val coverage = defaultCoverageData
                    .lastOrNull { it.kanjiCount <= kanjiCount }?.coveragePercent ?: 0f
                val x = xForKanji(kanjiCount)
                val y = yForPercent(coverage)

                drawCircle(
                    color = accent.primary,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            // User position marker
            if (userKanjiCount > 0) {
                val userCoverage = defaultCoverageData
                    .lastOrNull { it.kanjiCount <= userKanjiCount }?.coveragePercent ?: 0f
                val x = xForKanji(userKanjiCount.coerceAtMost(maxKanji))
                val y = yForPercent(userCoverage)

                drawCircle(
                    color = accent.secondary,
                    radius = 6.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        // Milestone summary row
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            milestoneMarkers.take(6).forEach { (kanjiCount, coverage) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = coverage,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent.primary
                    )
                    Text(
                        text = "$kanjiCount",
                        fontSize = 9.sp,
                        color = surfaceColors.textMuted
                    )
                }
            }
        }

        if (userKanjiCount > 0) {
            val userCoverage = defaultCoverageData
                .lastOrNull { it.kanjiCount <= userKanjiCount }?.coveragePercent ?: 0f
            Spacer(Modifier.height(6.dp))
            Text(
                text = "You know ~$userKanjiCount kanji — that covers roughly ${userCoverage.toInt()}% of common text.",
                fontSize = 11.sp,
                color = surfaceColors.textSecondary
            )
        }
    }
}
