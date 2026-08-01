package ua.syt0r.kanji.presentation.screen.main.screen.decks

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import kotlinx.datetime.*

// ============================================
// KAITEYO v1.2 — HEATMAP SCREEN
// Interactive contribution-style study heatmap
// with daily drill-down, statistics, and streaks
// ============================================

data class HeatmapDayV2(
    val date: LocalDate,
    val count: Int,
    val cardsStudied: Int = 0,
    val newCards: Int = 0,
    val reviewCards: Int = 0,
    val accuracy: Float = 0f,
    val timeStudied: Long = 0L,
    val mistakes: Int = 0
)

data class HeatmapDataV2(
    val year: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year,
    val days: Map<LocalDate, HeatmapDayV2> = emptyMap(),
    val totalReviews: Int = 0,
    val totalCardsStudied: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val averageAccuracy: Float = 0f,
    val totalStudyTime: Long = 0L
)

private fun generateMockHeatmapData2(): HeatmapDataV2 {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val days = mutableMapOf<LocalDate, HeatmapDayV2>()
    var totalReviews = 0
    var totalCardsStudied = 0
    var currentStreak = 0
    var longestStreak = 0
    var streak = 0

    // Generate 365 days of data
    for (i in 364 downTo 0) {
        val date = today.minus(i, DateTimeUnit.DAY)
        val hasActivity = Math.random() > 0.4 // 60% chance of activity
        if (hasActivity) {
            val count = (Math.random() * 30 + 1).toInt()
            val cardsStudied = (count * (0.7 + Math.random() * 0.3)).toInt()
            val newCards = (count * (0.1 + Math.random() * 0.2)).toInt()
            val accuracy = 0.6f + Math.random().toFloat() * 0.4f
            val timeStudied = (count * (15 + Math.random().toLong() * 45)) * 1000L // ms
            days[date] = HeatmapDayV2(
                date = date,
                count = count,
                cardsStudied = cardsStudied,
                newCards = newCards,
                reviewCards = count - newCards,
                accuracy = accuracy,
                timeStudied = timeStudied,
                mistakes = (count * (1f - accuracy)).toInt()
            )
            totalReviews += count
            totalCardsStudied += cardsStudied
            streak++
            currentStreak = streak
            if (streak > longestStreak) longestStreak = streak
        } else {
            streak = 0
        }
    }

    // Recalculate streak from today backward
    var streakCount = 0
    var d = today
    while (days.containsKey(d) && days[d]!!.count > 0) {
        streakCount++
        d = d.minus(1, DateTimeUnit.DAY)
    }
    currentStreak = streakCount

    return HeatmapDataV2(
        days = days,
        totalReviews = totalReviews,
        totalCardsStudied = totalCardsStudied,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        averageAccuracy = if (totalReviews > 0) days.values.sumOf { it.accuracy.toDouble() }.toFloat() / days.size else 0f,
        totalStudyTime = days.values.sumOf { it.timeStudied }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapV2FullScreen(
    data: HeatmapDataV2 = generateMockHeatmapData2(),
    cards: List<KaiteyoCard> = emptyList(),
    onClose: () -> Unit = {}
) {
    val surfaceColors = LocalSurfaceColors.current
    val accent = LocalKaiteyoAccent.current
    var selectedDay by remember { mutableStateOf<HeatmapDayV2?>(null) }
    var viewYear by remember { mutableStateOf(data.year) }
    var showStats by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Heatmap") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } },
                actions = {
                    IconButton(onClick = { showStats = !showStats }) {
                        Icon(if (showStats) Icons.Default.BarChart else Icons.Default.VisibilityOff, "Toggle Stats")
                    }
                    IconButton(onClick = { viewYear-- }) { Icon(Icons.Default.ChevronLeft, "Previous Year") }
                    Text("$viewYear", color = surfaceColors.textPrimary)
                    IconButton(onClick = { viewYear++ }) { Icon(Icons.Default.ChevronRight, "Next Year") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColors.surface,
                    titleContentColor = surfaceColors.textPrimary
                )
            )
        }
    ) { padding ->
        if (selectedDay != null) {
            DayDetailPanel(
                day = selectedDay!!,
                surfaceColors = surfaceColors,
                accent = accent,
                onBack = { selectedDay = null }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Streak and stats header
                StreakHeader(data, surfaceColors, accent)

                if (showStats) {
                    Spacer(Modifier.height(8.dp))
                    StatsSummaryRow(data, surfaceColors, accent)
                }

                Spacer(Modifier.height(16.dp))

                // Main heatmap
                HeatmapGrid(
                    data = data,
                    viewYear = viewYear,
                    surfaceColors = surfaceColors,
                    accent = accent,
                    onDayClick = { selectedDay = it }
                )

                Spacer(Modifier.height(16.dp))

                // Legend
                HeatmapLegend(surfaceColors = surfaceColors)

                Spacer(Modifier.height(16.dp))

                // Monthly breakdown
                MonthlyBreakdown(data, viewYear, surfaceColors, accent)

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StreakHeader(
    data: HeatmapDataV2,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColors.surfaceElevated)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StreakBadge("🔥", "${data.currentStreak}", "Day Streak", surfaceColors)
        StreakBadge("🏆", "${data.longestStreak}", "Best Streak", surfaceColors)
        StreakBadge("📊", "${data.totalReviews}", "Total Reviews", surfaceColors)
        StreakBadge("⏱️", formatStudyTime(data.totalStudyTime), "Study Time", surfaceColors)
    }
}

@Composable
private fun StreakBadge(
    emoji: String,
    value: String,
    label: String,
    surfaceColors: SurfaceColors
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 24.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = surfaceColors.textPrimary)
        Text(label, fontSize = 11.sp, color = surfaceColors.textMuted)
    }
}

@Composable
private fun StatsSummaryRow(
    data: HeatmapDataV2,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColors.surfaceElevated)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatChip("Accuracy", "${(data.averageAccuracy * 100).toInt()}%", Color(0xFFC2FC8B), surfaceColors)
        StatChip("Cards Studied", "${data.totalCardsStudied}", accent.primary, surfaceColors)
        StatChip("Avg/Day", "${if (data.days.isNotEmpty()) data.totalReviews / maxOf(data.days.size, 1) else 0}", Color(0xFFA78BFA), surfaceColors)
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    color: Color,
    surfaceColors: SurfaceColors
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
        Text(label, fontSize = 10.sp, color = surfaceColors.textMuted)
    }
}

@Composable
private fun HeatmapGrid(
    data: HeatmapDataV2,
    viewYear: Int,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme,
    onDayClick: (HeatmapDayV2) -> Unit
) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val dayLabels = listOf("", "Mon", "", "Wed", "", "Fri", "", "Sun")
    val monthLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    // Calculate the start date (Jan 1 of viewYear)
    val startDate = LocalDate(viewYear, 1, 1)
    val endDate = LocalDate(viewYear, 12, 31)
    val startDayOfWeek = startDate.dayOfWeek.value % 7 // 0 = Monday

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Activity ($viewYear)",
            style = MaterialTheme.typography.titleSmall,
            color = surfaceColors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Month labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            monthLabels.forEachIndexed { index, label ->
                // Only show months that have at least some days visible
                val monthStart = LocalDate(viewYear, index + 1, 1)
                if (monthStart >= startDate && monthStart <= endDate) {
                    Text(label, fontSize = 9.sp, color = surfaceColors.textMuted,
                        modifier = Modifier.width(28.dp))
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Heatmap grid
        val cellSize = 14.dp
        val gap = 3.dp

        Row(modifier = Modifier.fillMaxWidth()) {
            // Day labels column
            Column(
                verticalArrangement = Arrangement.spacedBy(gap),
                modifier = Modifier.width(28.dp)
            ) {
                dayLabels.forEach { label ->
                    Text(label, fontSize = 9.sp, color = surfaceColors.textMuted,
                        modifier = Modifier.height(cellSize))
                }
            }

            Spacer(Modifier.width(4.dp))

            // Grid of weeks
            val weeksInYear = 53
            Row(
                horizontalArrangement = Arrangement.spacedBy(gap),
                modifier = Modifier.fillMaxWidth()
            ) {
                (0 until weeksInYear).forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        (0 until 7).forEach { dayOfWeek ->
                            val dayOfYear = week * 7 + dayOfWeek - startDayOfWeek
                            val date = try {
                                startDate.plus(dayOfYear, DateTimeUnit.DAY)
                            } catch (e: Exception) { null }

                            if (date != null && date.year == viewYear && date <= endDate) {
                                val dayData = data.days[date]
                                val count = dayData?.count ?: 0
                                val isToday = date == today
                                val isSelected = false // Track separately

                                val intensity = when {
                                    count >= 20 -> 0.95f
                                    count >= 15 -> 0.75f
                                    count >= 10 -> 0.55f
                                    count >= 5 -> 0.35f
                                    count >= 1 -> 0.15f
                                    else -> 0.04f
                                }

                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(accent.primary.copy(alpha = intensity))
                                        .then(
                                            if (isToday) Modifier.border(
                                                1.5.dp, accent.secondary, RoundedCornerShape(3.dp)
                                            ) else Modifier
                                        )
                                        .clickable(enabled = count > 0) {
                                            dayData?.let { onDayClick(it) }
                                        }
                                )
                            } else {
                                Spacer(Modifier.size(cellSize))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapLegend(surfaceColors: SurfaceColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text("Less", fontSize = 10.sp, color = surfaceColors.textMuted)
        Spacer(Modifier.width(4.dp))
        listOf(0.04f, 0.15f, 0.35f, 0.55f, 0.75f, 0.95f).forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = alpha))
            )
            Spacer(Modifier.width(2.dp))
        }
        Text("More", fontSize = 10.sp, color = surfaceColors.textMuted)
    }
}

@Composable
private fun DayDetailPanel(
    day: HeatmapDayV2,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Back button and date
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = surfaceColors.textPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                day.date.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = surfaceColors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))

        // Day summary cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DayStatCard("Reviews", "${day.count}", accent.primary, surfaceColors, Modifier.weight(1f))
            DayStatCard("Cards", "${day.cardsStudied}", Color(0xFFC2FC8B), surfaceColors, Modifier.weight(1f))
            DayStatCard("New", "${day.newCards}", Color(0xFF7BC8FF), surfaceColors, Modifier.weight(1f))
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DayStatCard("Accuracy", "${(day.accuracy * 100).toInt()}%", Color(0xFFA78BFA), surfaceColors, Modifier.weight(1f))
            DayStatCard("Mistakes", "${day.mistakes}", Color(0xFFFF6B6B), surfaceColors, Modifier.weight(1f))
            DayStatCard("Time", formatStudyTime(day.timeStudied), Color(0xFFFFD93D), surfaceColors, Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))

        // Detailed breakdown
        Text(
            "Detailed Breakdown",
            style = MaterialTheme.typography.titleSmall,
            color = surfaceColors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))

        BreakdownBar("Review Cards", day.reviewCards.toFloat(), day.count.toFloat(), accent.primary, surfaceColors)
        BreakdownBar("New Cards", day.newCards.toFloat(), day.count.toFloat(), Color(0xFF7BC8FF), surfaceColors)
        BreakdownBar("Accuracy Rate", day.accuracy, 1f, Color(0xFFC2FC8B), surfaceColors)
        BreakdownBar("Mistake Rate", 1f - day.accuracy, 1f, Color(0xFFFF6B6B), surfaceColors)

        Spacer(Modifier.height(20.dp))

        // Decks studied (mock)
        Text(
            "Decks Studied",
            style = MaterialTheme.typography.titleSmall,
            color = surfaceColors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))

        val mockDecks = listOf(
            "JLPT N5 Kanji" to (5..20).random(),
            "JLPT N5 Vocabulary" to (3..15).random(),
            "Common Phrases" to (2..10).random(),
            "Radicals" to (1..8).random()
        )
        mockDecks.filter { it.second > 0 }.forEach { (name, reviews) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(name, fontSize = 13.sp, color = surfaceColors.textPrimary)
                Text("$reviews reviews", fontSize = 13.sp, color = surfaceColors.textMuted)
            }
        }
    }
}

@Composable
private fun DayStatCard(
    label: String,
    value: String,
    color: Color,
    surfaceColors: SurfaceColors,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = surfaceColors.surfaceElevated),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color)
            Text(label, fontSize = 11.sp, color = surfaceColors.textMuted)
        }
    }
}

@Composable
private fun BreakdownBar(
    label: String,
    value: Float,
    maxValue: Float,
    color: Color,
    surfaceColors: SurfaceColors
) {
    val fraction = if (maxValue > 0) (value / maxValue).coerceIn(0f, 1f) else 0f
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = surfaceColors.textPrimary)
            Text("${(value * 100 / maxOf(maxValue, 1f)).toInt()}%", fontSize = 12.sp, color = surfaceColors.textMuted)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = surfaceColors.surfaceInteractive,
        )
    }
}

@Composable
private fun MonthlyBreakdown(
    data: HeatmapDataV2,
    year: Int,
    surfaceColors: SurfaceColors,
    accent: KaiteyoAccentScheme
) {
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Monthly Breakdown",
            style = MaterialTheme.typography.titleSmall,
            color = surfaceColors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        monthNames.forEachIndexed { index, name ->
            val month = index + 1
            val monthDays = data.days.filter { it.key.monthNumber == month && it.key.year == year }
            val totalReviews = monthDays.values.sumOf { it.count }
            val activeDays = monthDays.count { it.value.count > 0 }
            val avgAccuracy = if (monthDays.isNotEmpty())
                monthDays.values.sumOf { it.accuracy.toDouble() }.toFloat() / monthDays.size else 0f
            val totalTime = monthDays.values.sumOf { it.timeStudied }

            if (totalReviews > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(surfaceColors.surfaceElevated.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = surfaceColors.textPrimary)
                        Text(
                            "$activeDays active days",
                            fontSize = 11.sp, color = surfaceColors.textMuted
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("$totalReviews reviews", fontSize = 13.sp, color = accent.primary)
                        Text("${(avgAccuracy * 100).toInt()}% accuracy", fontSize = 11.sp, color = surfaceColors.textMuted)
                    }
                }
            }
        }
    }
}

private fun formatStudyTime(millis: Long): String {
    val hours = millis / 3_600_000
    val minutes = (millis % 3_600_000) / 60_000
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
