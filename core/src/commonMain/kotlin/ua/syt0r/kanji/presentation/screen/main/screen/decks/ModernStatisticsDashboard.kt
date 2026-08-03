package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors

// ============================================================
// KAITEYO MODERN STATISTICS DASHBOARD
// Fully data-backed analytics replacing the legacy
// Kanji.Dojo statistics page. All numbers come from the real
// StatsOverviewV2 + per-day HeatmapDataV2 exposed by
// DeckFeaturesController.loadStats / loadHeatmap.
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsDashboardV2(
    stats: StatsOverviewV2,
    heatmap: HeatmapDataV2,
    onClose: () -> Unit
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var selectedDay by remember { mutableStateOf<HeatmapDayV2?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics Dashboard") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColors.surface,
                    titleContentColor = surfaceColors.textPrimary
                )
            )
        }
    ) { padding ->
        if (selectedDay != null) {
            DayDrillDown(day = selectedDay!!, onBack = { selectedDay = null })
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // Overview cards
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OverviewStatCard("Reviews Today", stats.todayReviews.toString(), "${stats.todayCardsStudied} cards",
                        Icons.Default.Today, accent.primary, Modifier.weight(1f))
                    OverviewStatCard("Studied Today", stats.todayTimeStudied.formatDuration(),
                        "${stats.todayNewCards} new · ${stats.todayLapses} lapses",
                        Icons.Default.Timer, Color(0xFF7BC8FF), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OverviewStatCard("Accuracy Today", "${(stats.todayAccuracy * 100).roundToInt()}%",
                        "week ${(stats.weekAccuracy * 100).roundToInt()}%",
                        Icons.Default.CheckCircle, Color(0xFFC2FC8B), Modifier.weight(1f))
                    OverviewStatCard("Due Cards", stats.cardsDue.toString(), "${stats.cardsNew} new",
                        Icons.Default.Schedule, Color(0xFFFFD93D), Modifier.weight(1f))
                }

                StreakStrip(current = stats.currentStreak, longest = stats.longestStreak,
                    totalReviews = stats.totalReviews, totalTime = stats.totalTimeStudied)

                SectionCard("Review Activity", Icons.Default.BarChart) {
                    AnalyticsBars(
                        history = heatmap.activeDays(WEEKLY_WINDOW),
                        title = "${stats.weekReviews} reviews this week · ${(stats.weekAccuracy * 100).roundToInt()}% accuracy"
                    )
                }

                SectionCard("Retention & Learning Curve", Icons.Default.TrendingUp) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RetentionDonut(stats.retentionRate, "Retention", Color(0xFFC2FC8B), Modifier.weight(1f))
                        RetentionDonut(stats.predictedRetention, "Predicted", accent.primary, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    AnalyticsLine(history = heatmap.activeDays(ACCURACY_WINDOW))
                }

                SectionCard("Library Distribution", Icons.Default.CalendarMonth) {
                    DistributionList(stats)
                }

                if (stats.forecastNextDays.isNotEmpty()) {
                    SectionCard("Review Forecast (next ${stats.forecastNextDays.size} days)", Icons.Default.BarChart) {
                        ForecastBars(stats.forecastNextDays)
                    }
                }

                SectionCard("Study Heatmap", Icons.Default.LocalFireDepartment) {
                    AnalyticsHeatmap(heatmap = heatmap, onDayClick = { selectedDay = it })
                }
            }
        }
    }
}

private const val WEEKLY_WINDOW = 21
private const val ACCURACY_WINDOW = 14

// ============================================================
// BUILDING BLOCKS
// ============================================================

@Composable
private fun OverviewStatCard(
    title: String, value: String, subtitle: String,
    icon: ImageVector, color: Color, modifier: Modifier = Modifier
) {
    val surfaceColors = LocalSurfaceColors.current
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = surfaceColors.surface)) {
        Column(Modifier.fillMaxWidth().padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(18.dp), tint = color)
                Spacer(Modifier.width(6.dp))
                Text(title, fontSize = 11.sp, color = surfaceColors.textMuted)
            }
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 11.sp, color = surfaceColors.textMuted)
            }
        }
    }
}

@Composable
private fun StreakStrip(current: Int, longest: Int, totalReviews: Int, totalTime: Long) {
    val surfaceColors = LocalSurfaceColors.current
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(surfaceColors.surfaceElevated).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StripLabel("Current", "$current d", Modifier.weight(1f))
        StripLabel("Longest", "$longest d", Modifier.weight(1f))
        StripLabel("Reviews", totalReviews.toString(), Modifier.weight(1f))
        StripLabel("Time", totalTime.formatDuration(), Modifier.weight(1f))
    }
}

@Composable
private fun StripLabel(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LocalKaiteyoAccent.current.primary,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, fontSize = 10.sp, color = LocalSurfaceColors.current.textMuted)
    }
}

@Composable
private fun SectionCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = surfaceColors.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(16.dp), tint = surfaceColors.textMuted)
                Spacer(Modifier.width(6.dp))
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = surfaceColors.textPrimary)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

// ============================================================
// CHARTS
// ============================================================

@Composable
private fun AnalyticsBars(history: List<HeatmapDayV2>, title: String) {
    val surfaceColors = LocalSurfaceColors.current
    if (history.isEmpty()) { EmptyStats(); return }
    val maxValue = (history.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
    Text(title, fontSize = 11.sp, color = surfaceColors.textMuted)
    Spacer(Modifier.height(10.dp))
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        val chartWidth = size.width - 40f
        val bottom = size.height - 20f
        val barW = chartWidth / history.size
        history.forEachIndexed { index, day ->
            val h = (day.count.toFloat() / maxValue) * (bottom - 20f)
            drawRect(Color(0xFF7BC8FF).copy(alpha = 0.75f),
                Offset(20f + index * barW, bottom - h), Size(barW * 0.8f, h))
        }
    }
}

@Composable
private fun AnalyticsLine(history: List<HeatmapDayV2>) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    if (history.isEmpty()) { EmptyStats(); return }
    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val chartWidth = size.width - 40f
        val bottom = size.height - 14f
        val stepX = chartWidth / (history.size - 1).coerceAtLeast(1)
        fun x(i: Int) = 20f + i * stepX
        fun y(acc: Float) = 12f + (bottom - 12f) * (1f - acc.coerceIn(0f, 1f))
        val path = Path()
        history.forEachIndexed { i, d ->
            val px = x(i); val py = y(d.accuracy)
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(path, accent.primary, style = Stroke(width = 2.5f))
        val fill = Path()
        fill.addPath(path)
        fill.lineTo(x(history.size - 1), bottom)
        fill.lineTo(x(0), bottom)
        fill.close()
        drawPath(fill, accent.primary.copy(alpha = 0.1f))
    }
    Text("Daily accuracy (last ${history.size} active days)", fontSize = 10.sp, color = surfaceColors.textMuted)
}

@Composable
private fun RetentionDonut(percentage: Float, label: String, color: Color, modifier: Modifier = Modifier) {
    val surfaceColors = LocalSurfaceColors.current
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = surfaceColors.surfaceElevated)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(60.dp)) {
                    val stroke = Stroke(width = 8f)
                    drawArc(color.copy(alpha = 0.15f), 0f, 360f, false, style = stroke)
                    drawArc(color, -90f, percentage.coerceIn(0f, 1f) * 360f, false, style = stroke)
                }
                Text("${(percentage * 100).roundToInt()}%", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = surfaceColors.textPrimary)
            }
            Spacer(Modifier.height(6.dp))
            Text(label, fontSize = 11.sp, color = surfaceColors.textMuted)
        }
    }
}

@Composable
private fun DistributionList(stats: StatsOverviewV2) {
    val surfaceColors = LocalSurfaceColors.current
    data class Row(val label: String, val value: Int, val color: Color)
    val rows = listOf(
        Row("Due", stats.cardsDue, Color(0xFFFFD93D)),
        Row("New", stats.cardsNew, Color(0xFFC2FC8B)),
        Row("Learning", stats.cardsLearning, Color(0xFF7BC8FF)),
        Row("Young", stats.cardsYoung, Color(0xFFA78BFA)),
        Row("Mature", stats.cardsMature, Color(0xFFFEAB57)),
        Row("Relearning", stats.cardsRelearning, Color(0xFFFF6B6B)),
        Row("Suspended", stats.cardsSuspended, Color(0xFFB0B0B0)),
        Row("Buried", stats.cardsBuried, Color(0xFFB0B0B0)),
        Row("Archived", stats.cardsArchived, Color(0xFF808080))
    )
    val max = rows.maxOf { it.value }.coerceAtLeast(1)
    rows.forEach { r ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(r.label, fontSize = 12.sp, color = surfaceColors.textSecondary, modifier = Modifier.width(90.dp))
            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(r.color.copy(alpha = 0.35f))) {
                Box(Modifier.fillMaxWidth((r.value / max.toFloat()).coerceIn(0.02f, 1f)).height(8.dp)
                    .clip(RoundedCornerShape(4.dp)).background(r.color))
            }
            Spacer(Modifier.width(8.dp))
            Text("${r.value}", fontSize = 12.sp, color = surfaceColors.textPrimary, modifier = Modifier.width(28.dp))
        }
    }
}

@Composable
private fun ForecastBars(forecast: List<Int>) {
    val surfaceColors = LocalSurfaceColors.current
    val max = forecast.maxOrNull()?.coerceAtLeast(1) ?: 1
    Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        forecast.forEachIndexed { index, count ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$count", fontSize = 9.sp, color = surfaceColors.textMuted)
                Spacer(Modifier.height(2.dp))
                Box(Modifier.fillMaxWidth().height((56 * count.toFloat() / max).coerceAtLeast(4f).dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (index == 0) LocalKaiteyoAccent.current.primary
                        else LocalKaiteyoAccent.current.primary.copy(alpha = 0.45f)))
            }
        }
    }
    Text("days ahead →", fontSize = 10.sp, color = surfaceColors.textMuted)
}

// ============================================================
// INTERACTIVE HEATMAP + DRILL DOWN
// ============================================================

@Composable
private fun AnalyticsHeatmap(heatmap: HeatmapDataV2, onDayClick: (HeatmapDayV2) -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val weeks = remember(heatmap) { buildWeeks(today) }

    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text("${heatmap.year} activity", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = surfaceColors.textSecondary)
        Text("${heatmap.totalReviews} reviews", fontSize = 11.sp, color = surfaceColors.textMuted)
    }

    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.Top) {
        Column(Modifier.width(26.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf("", "Mon", "", "Wed", "", "Fri", "").forEach {
                Text(it, fontSize = 9.sp, color = surfaceColors.textMuted, modifier = Modifier.height(13.dp))
            }
        }
        Spacer(Modifier.width(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            weeks.forEach { weekStart ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    (0 until 7).forEach { dow ->
                        val date = weekStart.plus(dow, DateTimeUnit.DAY)
                        val day = heatmap.days[date]
                        val count = day?.count ?: 0
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(accent.primary.copy(alpha = countToAlpha(count)))
                                .then(if (date == today) Modifier.background(accent.secondary.copy(alpha = 0.4f)) else Modifier)
                                .clickable(enabled = count > 0) { day?.let(onDayClick) }
                        )
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(10.dp))
    HeatmapLegend(surfaceColors)
    Spacer(Modifier.height(4.dp))
    Text("Tap a highlighted day for details.", fontSize = 10.sp, color = surfaceColors.textMuted)
}

private fun countToAlpha(count: Int) = when {
    count >= 20 -> 0.95f
    count >= 10 -> 0.78f
    count >= 5 -> 0.58f
    count >= 1 -> 0.35f
    else -> 0.05f
}

private fun buildWeeks(today: LocalDate): List<LocalDate> {
    val start = today.minus(70, DateTimeUnit.DAY)
    val aligned = start.minus(start.dayOfWeek.value % 7, DateTimeUnit.DAY)
    return (0 until 11).map { aligned.plus(it * 7, DateTimeUnit.DAY) }
}

@Composable
private fun HeatmapLegend(surfaceColors: SurfaceColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Less", fontSize = 9.sp, color = surfaceColors.textMuted)
        Spacer(Modifier.width(4.dp))
        listOf(0.05f, 0.35f, 0.58f, 0.78f, 0.95f).forEach { alpha ->
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = alpha)))
            Spacer(Modifier.width(2.dp))
        }
        Spacer(Modifier.width(4.dp))
        Text("More", fontSize = 9.sp, color = surfaceColors.textMuted)
    }
}

@Composable
private fun DayDrillDown(day: HeatmapDayV2, onBack: () -> Unit) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = surfaceColors.textPrimary) }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(day.date.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = surfaceColors.textPrimary)
                Text(day.date.todayLabel(), fontSize = 11.sp, color = surfaceColors.textMuted)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DayStat(day.count.toString(), "Reviews", Color(0xFF7BC8FF), Modifier.weight(1f))
            DayStat(day.cardsStudied.toString(), "Cards", LocalKaiteyoAccent.current.primary, Modifier.weight(1f))
            DayStat(day.newCards.toString(), "New", Color(0xFFC2FC8B), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DayStat("${(day.accuracy * 100).roundToInt()}%", "Accuracy", Color(0xFFA78BFA), Modifier.weight(1f))
            DayStat("${day.mistakes}", "Mistakes", Color(0xFFFF6B6B), Modifier.weight(1f))
            DayStat(day.timeStudied.formatDuration(), "Time", Color(0xFFFFD93D), Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        Text("Breakdown", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = surfaceColors.textPrimary)
        Spacer(Modifier.height(8.dp))
        BreakdownBar("Review Cards", day.reviewCards.toFloat(), day.count.toFloat(), accent.primary)
        BreakdownBar("New Cards", day.newCards.toFloat(), day.count.toFloat(), Color(0xFF7BC8FF))
        BreakdownBar("Accuracy", day.accuracy, 1f, Color(0xFFC2FC8B))
        BreakdownBar("Mistake Rate", 1f - day.accuracy, 1f, Color(0xFFFF6B6B))
    }
}

private fun LocalDate.todayLabel(): String =
    if (this == Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date) "Today"
    else dayOfWeek.name.take(3).let { it.lowercase().replaceFirstChar { c -> c.uppercase() } }

@Composable
private fun DayStat(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    val surfaceColors = LocalSurfaceColors.current
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = surfaceColors.surfaceElevated)) {
        Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, fontSize = 10.sp, color = surfaceColors.textMuted)
        }
    }
}

@Composable
private fun BreakdownBar(label: String, value: Float, maxValue: Float, color: Color) {
    val surfaceColors = LocalSurfaceColors.current
    val fraction = if (maxValue > 0) (value / maxValue).coerceIn(0f, 1f) else 0f
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, color = surfaceColors.textPrimary)
            Text("${(fraction * 100).roundToInt()}%", fontSize = 12.sp, color = surfaceColors.textMuted)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = surfaceColors.surfaceInteractive
        )
    }
}

@Composable
private fun EmptyStats() {
    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
        Text("Not enough data yet.", fontSize = 12.sp, color = LocalSurfaceColors.current.textMuted)
    }
}

// ============================================================
// DATA HELPERS
// ============================================================

private fun HeatmapDataV2.activeDays(limit: Int): List<HeatmapDayV2> =
    days.values.filter { it.count > 0 }.sortedBy { it.date }.takeLast(limit)

private fun Long.formatDuration(): String {
    val minutes = this / 60_000
    val hours = minutes / 60
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m"
        else -> "${this / 1000}s"
    }
}