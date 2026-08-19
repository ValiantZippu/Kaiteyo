package ua.syt0r.kanji.presentation.common.ui.kaiteyo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.roundToInt
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors

// ============================================================
// KANJIVERSE FREQUENCY HEATMAP
//
// A kanjiheatmap.com-style visualization that shows kanji
// frequency with brightness coding (more common = brighter).
// Supports switching between 7+ frequency datasets.
// ============================================================

/** Available frequency data sources for the heatmap. */
enum class FrequencySource(
    val displayName: String,
    val shortName: String,
    val methodology: String
) {
    Netflix("Netflix Subtitles", "Netflix", "Netflix Japanese subtitle frequency"),
    Kempson("Kempson Subtitles", "Kempson", "Chris Kempson subtitle frequency"),
    Kandrac("Kandrac 2242", "Kandrac", "Patrick Kandrac 2242 kanji frequency"),
    Nukemarine("Nukemarine RTK", "RTK", "Nukemarine RTK frequency groups"),
    Yatskov("Wikipedia", "Wikipedia", "Alex Yatskov Wikipedia frequency"),
    Girardi("Girardi", "Girardi", "Alexandre Girardi word frequency"),
    Leeds("Leeds Corpus", "Leeds", "Leeds Internet corpus frequency"),
    Kanjidic("KANJIDIC", "KANJIDIC", "KANJIDIC built-in frequency")
}

/** Frequency data for a single kanji from a single source. */
data class KanjiFrequencyData(
    val character: String,
    val rank: Int?,      // 1 = most common, null = not in this dataset
    val count: Int?,     // raw frequency count if available
    val percentile: Float? // 0.0-1.0, how common relative to dataset
)

/**
 * Main frequency heatmap component.
 *
 * Shows a grid of kanji colored by frequency — brighter cells
 * mean more common kanji. Users can switch between frequency
 * sources, filter by JLPT/grade/stroke count, and see details.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KanjiFrequencyHeatmap(
    kanjiData: List<KanjiFrequencyData>,
    allKanji: List<String>,
    selectedSource: FrequencySource,
    onSourceSelected: (FrequencySource) -> Unit,
    onKanjiSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxRank: Int = 3000
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Column(modifier = modifier) {
        // Header with source selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Frequency Heatmap",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedSource.methodology,
                    style = MaterialTheme.typography.bodySmall,
                    color = surfaceColors.textMuted
                )
            }
            FrequencySourceDropdown(
                selectedSource = selectedSource,
                onSourceSelected = onSourceSelected
            )
        }

        Spacer(Modifier.height(12.dp))

        // Legend
        FrequencyLegend(accent.primary)

        Spacer(Modifier.height(12.dp))

        // Kanji grid — 20 columns
        val columns = 20
        val rows = (allKanji.size + columns - 1) / columns

        (0 until rows).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    if (index < allKanji.size) {
                        val kanji = allKanji[index]
                        val freqData = kanjiData.find { it.character == kanji }
                        val rank = freqData?.rank

                        KanjiCell(
                            kanji = kanji,
                            rank = rank,
                            maxRank = maxRank,
                            accent = accent.primary,
                            surfaceColors = surfaceColors,
                            onClick = { onKanjiSelected(kanji) }
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun KanjiCell(
    kanji: String,
    rank: Int?,
    maxRank: Int,
    accent: Color,
    surfaceColors: SurfaceColors,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val background = when {
        rank == null -> surfaceColors.surfaceInteractive.copy(alpha = 0.3f)
        rank <= 100 -> accent
        rank <= 500 -> accent.copy(alpha = 0.85f)
        rank <= 1000 -> accent.copy(alpha = 0.7f)
        rank <= 2000 -> accent.copy(alpha = 0.5f)
        rank <= 3000 -> accent.copy(alpha = 0.35f)
        else -> accent.copy(alpha = 0.2f)
    }

    val textColor = when {
        rank == null -> surfaceColors.textMuted
        rank <= 200 -> surfaceColors.textInverse
        else -> surfaceColors.textPrimary
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .border(
                width = if (isHovered) 1.5.dp else 0.dp,
                color = if (isHovered) accent else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = kanji,
            fontSize = 11.sp,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun FrequencyLegend(accent: Color) {
    val surfaceColors = LocalSurfaceColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Less common",
            fontSize = 9.sp,
            color = surfaceColors.textMuted
        )
        listOf(0.2f, 0.35f, 0.5f, 0.7f, 0.85f, 1f).forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent.copy(alpha = alpha))
            )
        }
        Text(
            text = "Most common",
            fontSize = 9.sp,
            color = surfaceColors.textMuted
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "Not ranked",
            fontSize = 9.sp,
            color = surfaceColors.textMuted
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(surfaceColors.surfaceInteractive.copy(alpha = 0.3f))
        )
    }
}

@Composable
private fun FrequencySourceDropdown(
    selectedSource: FrequencySource,
    onSourceSelected: (FrequencySource) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = selectedSource.shortName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            FrequencySource.entries.forEach { source ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = source.displayName,
                                fontWeight = if (source == selectedSource) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = source.methodology,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSourceSelected(source)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Cumulative use graph — canvas-drawn curve showing how many
 * kanji cover what percentage of text. Inspired by kanjiheatmap.com.
 * The curve plots: X = number of kanji studied, Y = % of text covered.
 * Milestone markers at 50, 100, 200, 500, 1000, 2000 kanji.
 */
@Composable
fun CumulativeUseGraph(
    frequencyRanks: List<Int>,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val textMeasurer = rememberTextMeasurer()

    val sorted = remember(frequencyRanks) { frequencyRanks.sorted() }
    val total = sorted.size.coerceAtLeast(1)

    // Build the cumulative data points: (kanjiCount, percentCovered)
    val dataPoints = remember(sorted, total) {
        val maxKanji = sorted.lastOrNull()?.coerceAtLeast(2136) ?: 2136
        (0..2200 step 50).map { n ->
            val covered = sorted.count { it <= n }
            n to (covered.toFloat() / total * 100f)
        }
    }

    val milestones = listOf(50, 100, 200, 500, 1000, 2000)

    Column(modifier = modifier) {
        Text(
            text = "Cumulative Coverage",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "How many kanji cover X% of written Japanese",
            fontSize = 10.sp,
            color = surfaceColors.textMuted
        )

        Spacer(Modifier.height(12.dp))

        // Canvas chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(surfaceColors.surfaceInteractive.copy(alpha = 0.3f))
                .padding(horizontal = 40.dp, vertical = 12.dp)
        ) {
            val chartWidth = size.width
            val chartHeight = size.height
            val maxX = 2200f
            val maxY = 100f

            fun toCanvasX(kanjiCount: Int) = (kanjiCount / maxX) * chartWidth
            fun toCanvasY(percent: Float) = chartHeight - (percent / maxY) * chartHeight

            // Grid lines
            listOf(0f, 25f, 50f, 75f, 100f).forEach { y ->
                val cy = toCanvasY(y)
                drawLine(
                    color = surfaceColors.textMuted.copy(alpha = 0.15f),
                    start = Offset(0f, cy),
                    end = Offset(chartWidth, cy),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Filled area under the curve
            if (dataPoints.size >= 2) {
                val fillPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(toCanvasX(dataPoints.first().first), chartHeight)
                    dataPoints.forEach { (n, pct) ->
                        lineTo(toCanvasX(n), toCanvasY(pct))
                    }
                    lineTo(toCanvasX(dataPoints.last().first), chartHeight)
                    close()
                }
                drawPath(
                    path = fillPath,
                    color = accent.primary.copy(alpha = 0.15f)
                )

                // The curve line
                val linePath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(toCanvasX(dataPoints.first().first), toCanvasY(dataPoints.first().second))
                    dataPoints.drop(1).forEach { (n, pct) ->
                        lineTo(toCanvasX(n), toCanvasY(pct))
                    }
                }
                drawPath(
                    path = linePath,
                    color = accent.primary,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // Milestone markers
            milestones.forEach { m ->
                val covered = sorted.count { it <= m }
                val pct = covered.toFloat() / total * 100f
                val cx = toCanvasX(m)
                val cy = toCanvasY(pct)

                // Vertical dashed line
                drawLine(
                    color = surfaceColors.textMuted.copy(alpha = 0.25f),
                    start = Offset(cx, 0f),
                    end = Offset(cx, chartHeight),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                )

                // Dot
                drawCircle(
                    color = accent.primary,
                    radius = 4.dp.toPx(),
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = surfaceColors.surface,
                    radius = 2.dp.toPx(),
                    center = Offset(cx, cy)
                )

                // Label below
                val labelStyle = TextStyle(
                    fontSize = 8.sp,
                    color = surfaceColors.textMuted
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = "${m}",
                    topLeft = Offset(cx - 8.dp.toPx(), chartHeight + 2.dp.toPx()),
                    style = labelStyle
                )
            }

            // Y-axis labels
            listOf(0f, 50f, 100f).forEach { y ->
                val labelStyle = TextStyle(
                    fontSize = 8.sp,
                    color = surfaceColors.textMuted
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = "${y.roundToInt()}%",
                    topLeft = Offset(-36.dp.toPx(), toCanvasY(y) - 5.dp.toPx()),
                    style = labelStyle
                )
            }
        }

        // Summary row
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            milestones.take(4).forEach { m ->
                val covered = sorted.count { it <= m }
                val pct = (covered.toFloat() / total * 100).roundToInt()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$m",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent.primary
                    )
                    Text(
                        text = "$pct%",
                        fontSize = 9.sp,
                        color = surfaceColors.textMuted
                    )
                }
            }
        }
    }
}

/**
 * Frequency detail panel — shows a kanji's frequency across all sources.
 */
@Composable
fun FrequencyDetailPanel(
    kanji: String,
    frequencyBySource: Map<FrequencySource, KanjiFrequencyData>,
    modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = kanji,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent.primary
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Frequency across sources",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            FrequencySource.entries.forEach { source ->
                val data = frequencyBySource[source]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = source.shortName,
                        fontSize = 10.sp,
                        color = surfaceColors.textMuted,
                        modifier = Modifier.width(60.dp)
                    )
                    if (data?.rank != null) {
                        val barFraction = (1f - data.rank / 3000f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(surfaceColors.surfaceInteractive)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(barFraction)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(accent.primary)
                            )
                        }
                        Text(
                            text = "#${data.rank}",
                            fontSize = 10.sp,
                            color = accent.primary,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.End
                        )
                    } else {
                        Text(
                            text = "—",
                            fontSize = 10.sp,
                            color = surfaceColors.textMuted,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
