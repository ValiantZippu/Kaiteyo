package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import kotlin.math.min

// ============================================
// STATISTICS & ANALYTICS DASHBOARD
// Charts, graphs, per-deck stats, per-card stats,
// retention rates, forecasts, review timing
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsDashboard(
    cards: List<KaiteyoCard> = emptyList(),
    onClose: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var selectedTimeRange by remember { mutableStateOf("30d") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    IconButton(onClick = { }) { Icon(Icons.Default.BarChart, "Export Stats") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Time range selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("1d", "7d", "30d", "90d", "1y", "All").forEach { range ->
                    FilterChip(
                        selected = selectedTimeRange == range,
                        onClick = { selectedTimeRange = range },
                        label = { Text(range) }
                    )
                }
            }

            // Overview cards
            Text("Overview", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard2("Reviews Today", "47", Icons.Default.Today, Modifier.weight(1f))
                StatCard2("Cards Studied", "89", Icons.Default.MenuBook, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard2("Time Studied", "32m", Icons.Default.Timer, Modifier.weight(1f))
                StatCard2("Accuracy", "87%", Icons.Default.CheckCircle, Modifier.weight(1f))
            }

            // Retention over time chart
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Retention Rate", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    RetentionChart(cards = cards)
                }
            }

            // Card distribution pie chart
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Card Distribution", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    CardDistributionChart(cards = cards)
                }
            }

            // Review history chart
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Review History", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    ReviewHistoryChart()
                }
            }

            // Forecast chart
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Forecast: Upcoming Reviews", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    ForecastChart()
                }
            }

            // Per-deck breakdown
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Per-Deck Breakdown", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    listOf("N5 Kanji" to "120 cards • 85% mature • 92% retention",
                        "N4 Kanji" to "180 cards • 72% mature • 88% retention",
                        "Core 2000" to "500 cards • 45% mature • 82% retention").forEach { (deck, stats) ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(deck, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text(stats, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.TrendingUp, null, Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Time of day histogram
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Study Time Distribution", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    TimeDistributionChart()
                }
            }

            // Cumulative review count
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Cumulative Reviews", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    CumulativeChart()
                }
            }
        }
    }
}

@Composable
private fun StatCard2(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ============================================
// RETENTION CHART (Line chart)
// ============================================

@Composable
private fun RetentionChart(cards: List<KaiteyoCard>) {
    val accent = LocalKaiteyoAccent.current
    val surfaceColors = LocalSurfaceColors.current
    val dataPoints = remember(cards) {
        listOf(0.91f, 0.88f, 0.85f, 0.89f, 0.92f, 0.87f, 0.84f, 0.86f, 0.90f, 0.93f, 0.89f, 0.91f)
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        val chartWidth = size.width - 40f
        val chartHeight = size.height - 40f
        val stepX = chartWidth / (dataPoints.size - 1).coerceAtLeast(1)

        // Grid lines
        listOf(0.2f, 0.4f, 0.6f, 0.8f).forEach { fraction ->
            val y = 20f + chartHeight * (1f - fraction)
            drawLine(Color.LightGray.copy(alpha = 0.3f), Offset(30f, y), Offset(30f + chartWidth, y))
        }

        // Data line
        val path = Path()
        dataPoints.forEachIndexed { i, value ->
            val x = 30f + i * stepX
            val y = 20f + chartHeight * (1f - value)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, accent.primary, style = Stroke(width = 3f))

        // Fill under line
        val fillPath = Path()
        fillPath.addPath(path)
        fillPath.lineTo(30f + (dataPoints.size - 1) * stepX, 20f + chartHeight)
        fillPath.lineTo(30f, 20f + chartHeight)
        fillPath.close()
        drawPath(fillPath, accent.primary.copy(alpha = 0.1f))

        // Data points
        dataPoints.forEachIndexed { i, value ->
            val x = 30f + i * stepX
            val y = 20f + chartHeight * (1f - value)
            drawCircle(accent.primary, 4f, Offset(x, y))
            drawCircle(Color.White, 2f, Offset(x, y))
        }
    }
}

// ============================================
// CARD DISTRIBUTION (Pie chart)
// ============================================

@Composable
private fun CardDistributionChart(cards: List<KaiteyoCard>) {
    val accent = LocalKaiteyoAccent.current
    val newCount = cards.count { it.status == CardStatus.New }.coerceAtLeast(1)
    val learningCount = cards.count { it.status == CardStatus.Learning || it.status == CardStatus.Relearning }.coerceAtLeast(1)
    val youngCount = cards.count { it.status == CardStatus.Young }.coerceAtLeast(1)
    val matureCount = cards.count { it.status == CardStatus.Mature }.coerceAtLeast(1)
    val suspendedCount = cards.count { it.isSuspended }.coerceAtLeast(1)
    val total = (newCount + learningCount + youngCount + matureCount + suspendedCount).coerceAtLeast(1)

    data class Segment(val name: String, val fraction: Float, val color: Color)

    val segments = listOf(
        Segment("New", newCount.toFloat() / total, Color(0xFF7BC8FF)),
        Segment("Learning", learningCount.toFloat() / total, Color(0xFFFF6B6B)),
        Segment("Young", youngCount.toFloat() / total, Color(0xFFFEAB57)),
        Segment("Mature", matureCount.toFloat() / total, Color(0xFFC2FC8B)),
        Segment("Suspended", suspendedCount.toFloat() / total, Color(0xFFB0B0B0))
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pie chart
        Canvas(modifier = Modifier.size(140.dp)) {
            val radius = min(size.width, size.height) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            var startAngle = -90f

            segments.forEach { segment ->
                val sweepAngle = segment.fraction * 360f
                val topLeft = Offset(center.x - radius, center.y - radius)
                drawArc(
                    color = segment.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = topLeft,
                    size = Size(radius * 2, radius * 2)
                )
                startAngle += sweepAngle
            }
            drawCircle(Color.White, radius * 0.6f, center)
        }

        Spacer(Modifier.width(16.dp))

        // Legend
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            segments.forEach { segment ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(segment.color))
                    Spacer(Modifier.width(6.dp))
                    Text("${segment.name}: ${(segment.fraction * 100).toInt()}%", fontSize = 12.sp)
                }
            }
        }
    }
}

// ============================================
// REVIEW HISTORY (Bar chart)
// ============================================

@Composable
private fun ReviewHistoryChart() {
    val accent = LocalKaiteyoAccent.current
    val data = remember { listOf(12, 18, 8, 25, 15, 30, 22, 17, 9, 20, 28, 14, 19, 11) }

    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        val barWidth = (size.width - 40f) / data.size * 0.7f
        val gap = (size.width - 40f) / data.size * 0.3f
        val maxValue = data.max().coerceAtLeast(1)
        val chartHeight = size.height - 40f

        data.forEachIndexed { i, value ->
            val barHeight = (value.toFloat() / maxValue) * chartHeight
            val x = 20f + i * (barWidth + gap)
            val y = size.height - 20f - barHeight
            drawRect(accent.primary.copy(alpha = 0.7f), Offset(x, y), Size(barWidth, barHeight))
        }

        // Y-axis
        drawLine(Color.LightGray.copy(alpha = 0.3f), Offset(20f, 20f), Offset(20f, size.height - 20f))
    }
}

// ============================================
// FORECAST CHART
// ============================================

@Composable
private fun ForecastChart() {
    val accent = LocalKaiteyoAccent.current
    val forecast = remember { listOf(23, 18, 15, 30, 12, 8, 20, 25, 10, 5, 15, 18, 22, 28) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Next 14 days forecast: ${forecast.sum()} reviews due",
                style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))

            Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                val maxVal = forecast.max().coerceAtLeast(1)
                val barW = (size.width - 20f) / forecast.size * 0.75f
                val gap = (size.width - 20f) / forecast.size * 0.25f

                forecast.forEachIndexed { i, v ->
                    val h = (v.toFloat() / maxVal) * (size.height - 10f)
                    drawRect(accent.primary, Offset(10f + i * (barW + gap), size.height - h),
                        Size(barW, h))
                }
            }
        }
    }
}

// ============================================
// TIME DISTRIBUTION CHART
// ============================================

@Composable
private fun TimeDistributionChart() {
    val accent = LocalKaiteyoAccent.current
    // Simulated hourly distribution (24 hours)
    val hourlyData = remember {
        listOf(0, 0, 0, 0, 1, 3, 8, 15, 22, 18, 12, 8, 5, 3, 4, 6, 10, 15, 20, 25, 18, 10, 5, 1)
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val maxVal = hourlyData.max().coerceAtLeast(1)
        val barW = (size.width - 40f) / 24f * 0.8f
        val gap = (size.width - 40f) / 24f * 0.2f

        hourlyData.forEachIndexed { hour, count ->
            val h = (count.toFloat() / maxVal) * (size.height - 10f)
            val color = when (hour) {
                in 6..11 -> Color(0xFFFFD93D).copy(alpha = 0.7f) // Morning
                in 12..17 -> Color(0xFF7BC8FF).copy(alpha = 0.7f) // Afternoon
                in 18..23 -> Color(0xFFA78BFA).copy(alpha = 0.7f) // Evening
                else -> Color(0xFFB0B0B0).copy(alpha = 0.7f) // Night
            }
            drawRect(color, Offset(20f + hour * (barW + gap), size.height - h), Size(barW, h))
        }
    }
}

// ============================================
// CUMULATIVE CHART
// ============================================

@Composable
private fun CumulativeChart() {
    val accent = LocalKaiteyoAccent.current
    val cumulativeData = remember {
        val base = 100
        (1..30).map { base + it * (10..30).random() }
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val maxVal = cumulativeData.max().coerceAtLeast(1)
        val stepX = (size.width - 40f) / (cumulativeData.size - 1).coerceAtLeast(1)

        val path = Path()
        cumulativeData.forEachIndexed { i, v ->
            val x = 20f + i * stepX
            val y = size.height - 10f - (v.toFloat() / maxVal) * (size.height - 20f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Color(0xFFC2FC8B), style = Stroke(width = 2.5f))

        // Fill
        val fillPath = Path()
        fillPath.addPath(path)
        fillPath.lineTo(20f + (cumulativeData.size - 1) * stepX, size.height - 10f)
        fillPath.lineTo(20f, size.height - 10f)
        fillPath.close()
        drawPath(fillPath, Color(0xFFC2FC8B).copy(alpha = 0.15f))
    }
}
