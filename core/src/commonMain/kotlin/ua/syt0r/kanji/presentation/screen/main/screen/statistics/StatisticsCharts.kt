package ua.syt0r.kanji.presentation.screen.main.screen.statistics

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors

// ============================================================
// SHARED STATISTICS CHART COMPONENTS
// ============================================================

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String = "",
    color: Color? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val valueColor = color ?: accent.primary
    val baseModifier = modifier
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    Card(
        modifier = baseModifier,
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(13.dp)) {
            Text(
                title, fontSize = 11.sp, color = surfaceColors.textMuted,
                fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(7.dp))
            Text(
                value, fontSize = 21.sp, fontWeight = FontWeight.Bold,
                color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, fontSize = 11.sp, color = surfaceColors.textMuted, maxLines = 1)
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    subtitle: String = "",
    content: @Composable () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = surfaceColors.textPrimary)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 11.sp, color = surfaceColors.textMuted)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun LabeledBarRow(
    label: String,
    value: Int,
    max: Int,
    color: Color,
    valueSuffix: String = ""
) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = surfaceColors.textSecondary, modifier = Modifier.width(86.dp))
        Box(
            Modifier.weight(1f).height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.3f))
        ) {
            val fraction = if (max > 0) (value.toFloat() / max).coerceIn(0.01f, 1f) else 0.01f
            Box(
                Modifier.fillMaxWidth(fraction).height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "$value$valueSuffix", fontSize = 12.sp, color = surfaceColors.textPrimary,
            modifier = Modifier.width(40.dp), maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ProgressRow(
    label: String,
    fraction: Float,
    detail: String,
    color: Color? = null
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val barColor = color ?: accent.primary
    Column(Modifier.padding(vertical = 5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, color = surfaceColors.textPrimary)
            Text(detail, fontSize = 12.sp, color = surfaceColors.textMuted)
        }
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier.fillMaxWidth().height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(surfaceColors.surfaceInteractive)
        ) {
            Box(
                Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun BarsChart(
    values: List<Float>,
    labels: List<String> = emptyList(),
    color: Color,
    heightDp: Int = 130,
    valueFormatter: (Float) -> String = { it.roundToInt().toString() }
) {
    val surfaceColors = LocalSurfaceColors.current
    if (values.isEmpty()) {
        EmptyChart()
        return
    }
    val max = values.maxOrNull()?.takeIf { it > 0 } ?: 1f
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(heightDp.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            values.forEach { value ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(valueFormatter(value), fontSize = 9.sp, color = surfaceColors.textMuted)
                    Spacer(Modifier.height(2.dp))
                    Box(
                        Modifier.fillMaxWidth()
                            .height((value / max * (heightDp - 24)).coerceAtLeast(3f).dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                }
            }
        }
        if (labels.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                labels.forEach { label ->
                    Text(
                        label, fontSize = 9.sp, color = surfaceColors.textMuted,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun LineChart(
    points: List<Pair<String, Float>>,
    color: Color,
    heightDp: Int = 110,
    valueFormatter: (Float) -> String = { it.roundToInt().toString() }
) {
    val surfaceColors = LocalSurfaceColors.current
    if (points.isEmpty() || points.all { it.second <= 0f }) {
        EmptyChart("No reviews in this window yet.")
        return
    }
    val maxValue = points.maxOf { it.second }
    Canvas(Modifier.fillMaxWidth().height(heightDp.dp)) {
        val chartWidth = size.width - 30f
        val bottom = size.height - 14f
        val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)
        fun x(i: Int) = 15f + i * stepX
        // Normalize by the real max so a 5-review day and a 50-review day
        // both scale correctly (previous version clamped to 0..1).
        fun y(value: Float) = 10f + (bottom - 10f) * (1f - value / maxValue)
        val path = Path()
        points.forEachIndexed { i, (_, value) ->
            val px = x(i); val py = y(value)
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(path, color, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
        val fill = Path()
        fill.addPath(path)
        fill.lineTo(x(points.size - 1), bottom)
        fill.lineTo(x(0), bottom)
        fill.close()
        drawPath(fill, color.copy(alpha = 0.14f))
        points.forEachIndexed { i, (_, value) ->
            if (value > 0f) {
                drawCircle(color, radius = 2.5f, center = Offset(x(i), y(value)))
            }
        }
        // Baseline grid: a faint horizontal line at the bottom axis.
        drawLine(
            color = surfaceColors.textMuted.copy(alpha = 0.25f),
            start = Offset(15f, bottom),
            end = Offset(size.width - 15f, bottom),
            strokeWidth = 1f
        )
    }
    // Date labels: first, middle, last — keeps the axis readable.
    Row(Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp, top = 4.dp)) {
        val indices = listOf(0, points.size / 2, points.lastIndex)
        indices.distinct().forEach { i ->
            val label = points[i].first
            Text(
                label, fontSize = 9.sp, color = surfaceColors.textMuted,
                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================
// MULTI-SLICE DONUT — proportions with a legend
// ============================================================

data class DonutSliceData(
    val label: String,
    val value: Float,
    val color: Color
)

@Composable
fun MultiDonutChart(
    slices: List<DonutSliceData>,
    modifier: Modifier = Modifier,
    sizeDp: Int = 120,
    strokeDp: Int = 16,
    centerTop: String = "",
    centerBottom: String = ""
) {
    val surfaceColors = LocalSurfaceColors.current
    val total = slices.fold(0f) { acc, slice -> acc + slice.value }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(sizeDp.dp)) {
                val stroke = Stroke(width = strokeDp.dp.toPx(), cap = StrokeCap.Butt)
                val active = slices.filter { it.value > 0f }
                if (active.isEmpty() || total <= 0f) {
                    drawArc(surfaceColors.surfaceInteractive, 0f, 360f, false, style = stroke)
                } else {
                    var start = -90f
                    active.forEach { slice ->
                        val sweep = slice.value / total * 360f
                        drawArc(slice.color, start, sweep, false, style = stroke)
                        start += sweep
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (centerTop.isNotEmpty()) {
                    Text(
                        centerTop, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = surfaceColors.textPrimary
                    )
                }
                if (centerBottom.isNotEmpty()) {
                    Text(centerBottom, fontSize = 9.sp, color = surfaceColors.textMuted)
                }
            }
        }
    }
}

@Composable
fun ChartLegend(
    items: List<DonutSliceData>,
    modifier: Modifier = Modifier,
    valueFormatter: (Float) -> String = { it.roundToInt().toString() },
    percentOfTotal: Boolean = false
) {
    val surfaceColors = LocalSurfaceColors.current
    val total = items.fold(0f) { acc, item -> acc + item.value }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(item.color))
                Spacer(Modifier.width(7.dp))
                Text(
                    item.label, fontSize = 11.sp, color = surfaceColors.textSecondary,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (percentOfTotal && total > 0f)
                        "${((item.value / total) * 100).roundToInt()}%"
                    else valueFormatter(item.value),
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = surfaceColors.textPrimary
                )
            }
        }
    }
}

/** Thin stacked proportion bar — great for "studied vs remaining" splits. */
@Composable
fun StackedBar(
    items: List<DonutSliceData>,
    modifier: Modifier = Modifier,
    heightDp: Int = 10
) {
    val total = items.fold(0f) { acc, item -> acc + item.value }
    Row(
        modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape((heightDp / 2).dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEach { item ->
            if (item.value > 0f && total > 0f) {
                Box(
                    Modifier
                        .weight(item.value / total)
                        .height(heightDp.dp)
                        .background(item.color)
                )
            }
        }
    }
}

@Composable
fun DonutChart(
    percentage: Float,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = surfaceColors.surfaceElevated)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(58.dp)) {
                    val stroke = Stroke(width = 8f)
                    drawArc(color.copy(alpha = 0.15f), 0f, 360f, false, style = stroke)
                    drawArc(color, -90f, percentage.coerceIn(0f, 1f) * 360f, false, style = stroke)
                }
                Text(
                    "${(percentage * 100).roundToInt()}%",
                    fontWeight = FontWeight.Bold, fontSize = 13.sp, color = surfaceColors.textPrimary
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(label, fontSize = 11.sp, color = surfaceColors.textMuted)
        }
    }
}

@Composable
fun EmptyChart(message: String = "Not enough data yet.") {
    val surfaceColors = LocalSurfaceColors.current
    Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
        Text(message, fontSize = 12.sp, color = surfaceColors.textMuted)
    }
}

@Composable
fun EmptyState(title: String, message: String) {
    val surfaceColors = LocalSurfaceColors.current
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📊", fontSize = 30.sp)
        Spacer(Modifier.height(10.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = surfaceColors.textPrimary)
        Spacer(Modifier.height(4.dp))
        Text(
            message, fontSize = 12.sp, color = surfaceColors.textMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
